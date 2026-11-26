package com.melox.player.data.library

import android.content.ContentResolver
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.id3.ApicFrame
import androidx.media3.extractor.metadata.id3.Id3Decoder
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import java.io.DataInputStream
import java.io.EOFException
import java.io.InputStream

internal data class WavMetadata(
    val tags: LocalAudioTags,
    val artwork: ByteArray?,
)

internal fun ContentResolver.readWavMetadata(contentUri: String): WavMetadata? = runCatching {
    openInputStream(contentUri.toUri())?.use(::readWavMetadata)
}.getOrNull()

/** RIFF chunk sizes let us skip PCM without buffering or scanning audio bytes. */
@androidx.annotation.OptIn(UnstableApi::class)
internal fun readWavMetadata(stream: InputStream): WavMetadata? {
    val input = DataInputStream(stream)
    val header = ByteArray(12)
    try {
        input.readFully(header)
        if (String(header, 0, 4, Charsets.US_ASCII) != "RIFF" ||
            String(header, 8, 4, Charsets.US_ASCII) != "WAVE"
        ) return null
        var remaining = header.uint32le(4) - 4
        val chunk = ByteArray(8)
        var count = 0
        while (remaining >= 8 && count++ < 4096) {
            input.readFully(chunk)
            val size = chunk.uint32le(4)
            val paddedSize = size + (size and 1)
            if (paddedSize > remaining - 8) return null
            if (String(chunk, 0, 4, Charsets.US_ASCII).equals("id3 ", ignoreCase = true) &&
                size in 10..16L * 1024 * 1024
            ) {
                val bytes = ByteArray(size.toInt())
                input.readFully(bytes)
                val metadata = Id3Decoder().decode(bytes, bytes.size)
                if (metadata != null) {
                    val properties = mutableMapOf<String, Array<String>>()
                    var artwork: ByteArray? = null
                    var artworkPriority = Int.MAX_VALUE
                    for (index in 0 until metadata.length()) {
                        when (val frame = metadata[index]) {
                            is TextInformationFrame -> properties[frame.id] = frame.values.toTypedArray()
                            is ApicFrame -> {
                                val priority = coverPicturePriority(frame.pictureType)
                                if (priority != null && priority < artworkPriority && frame.pictureData.isNotEmpty()) {
                                    artwork = frame.pictureData
                                    artworkPriority = priority
                                }
                            }
                        }
                    }
                    return WavMetadata(parseAudioTagProperties(properties), artwork)
                }
                input.skipFully(size and 1)
            } else {
                input.skipFully(paddedSize)
            }
            remaining -= 8 + paddedSize
        }
    } catch (_: EOFException) {
        return null
    }
    return null
}

private fun ByteArray.uint32le(offset: Int): Long =
    (0..3).fold(0L) { value, index ->
        value or ((this[offset + index].toLong() and 0xff) shl (index * 8))
    }

internal fun InputStream.skipFully(length: Long) {
    var remaining = length
    while (remaining > 0) {
        val skipped = skip(remaining)
        if (skipped > 0) remaining -= skipped
        else if (read() < 0) throw EOFException()
        else remaining--
    }
}
