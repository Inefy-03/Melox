package com.melox.player.data.library

import com.melox.player.model.MusicTrack
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.CRC32
import java.util.zip.CheckedInputStream
import java.util.zip.CheckedOutputStream

/** Compact, versioned encoding for the last successful local-library snapshot. */
internal object MusicLibrarySnapshotCodec {
    private const val MAGIC = 0x5549584D
    private const val VERSION = 7
    private const val MIN_SUPPORTED_VERSION = 1
    private const val MAX_TRACK_COUNT = 100_000
    private const val MAX_STRING_BYTES = 1_048_576

    fun write(
        outputStream: OutputStream,
        tracks: List<MusicTrack>,
    ) {
        require(tracks.size <= MAX_TRACK_COUNT) {
            "Music snapshot contains too many tracks: ${tracks.size}"
        }

        val checksum = CRC32()
        val output = DataOutputStream(
            CheckedOutputStream(BufferedOutputStream(outputStream), checksum),
        )
        output.writeInt(MAGIC)
        output.writeInt(VERSION)
        output.writeInt(tracks.size)
        tracks.forEach { track ->
            output.writeLong(track.id)
            output.writeNullableString(track.title)
            output.writeNullableString(track.artist)
            output.writeNullableString(track.album)
            output.writeNullableString(track.albumArtist)
            output.writeInt(track.year ?: 0)
            output.writeLong(track.durationMs)
            output.writeLong(track.dateAddedEpochSeconds)
            output.writeLong(track.dateModifiedEpochSeconds)
            output.writeNullableString(track.fileName)
            output.writeNullableString(track.folderPath)
            output.writeLong(track.albumId ?: 0L)
            output.writeLong(track.fileSizeBytes)
            output.writeSizedString(track.contentUri)
            output.writeSizedString(track.titleSectionKey)
            output.writeSizedString(track.titleSortKey)
            output.writeNullableString(track.mimeType)
            output.writeInt(track.bitrateBitsPerSecond ?: 0)
            output.writeInt(track.sampleRateHz ?: 0)
            output.writeInt(track.channelCount ?: 0)
            output.writeBoolean(track.audioPropertiesScanned)
            output.writeInt(track.trackNumber ?: 0)
            output.writeInt(track.discNumber ?: 0)
            output.writeInt(track.bitDepth ?: 0)
        }
        output.writeLong(checksum.value)
        output.flush()
    }

    fun read(inputStream: InputStream): List<MusicTrack> {
        val checksum = CRC32()
        val input = DataInputStream(
            CheckedInputStream(BufferedInputStream(inputStream), checksum),
        )
        if (input.readInt() != MAGIC) {
            throw IOException("Unrecognized music snapshot")
        }
        val version = input.readInt()
        if (version !in MIN_SUPPORTED_VERSION..VERSION) {
            throw IOException("Unsupported music snapshot version: $version")
        }
        val trackCount = input.readInt()
        if (trackCount !in 0..MAX_TRACK_COUNT) {
            throw IOException("Invalid music snapshot track count: $trackCount")
        }

        val tracks = List(trackCount) {
            val id = input.readLong()
            val title = input.readNullableString()
            val artist = input.readNullableString()
            val album = input.readNullableString()
            val albumArtist = if (version >= 3) input.readNullableString() else null
            val year = if (version >= 3) input.readInt().takeIf { it > 0 } else null
            val durationMs = input.readLong()
            val dateAddedEpochSeconds = input.readLong()
            val dateModifiedEpochSeconds = if (version >= 2) input.readLong() else 0L
            val fileName = input.readNullableString()
            val folderPath = if (version >= 4) input.readNullableString() else null
            val albumId = if (version >= 5) input.readLong().takeIf { it > 0L } else null
            val fileSizeBytes = input.readLong()
            val contentUri = input.readSizedString()
            val titleSectionKey = input.readSizedString()
            val titleSortKey = input.readSizedString()
            val mimeType = if (version >= 6) input.readNullableString() else null
            val bitrateBitsPerSecond =
                if (version >= 6) input.readInt().takeIf { it > 0 } else null
            val sampleRateHz = if (version >= 6) input.readInt().takeIf { it > 0 } else null
            val channelCount = if (version >= 6) input.readInt().takeIf { it > 0 } else null
            val completedLegacyRead = version >= 6 && input.readBoolean()
            val trackNumber = if (version >= 7) input.readInt().takeIf { it > 0 } else null
            val discNumber = if (version >= 7) input.readInt().takeIf { it > 0 } else null
            val bitDepth = if (version >= 7) input.readInt().takeIf { it > 0 } else null
            MusicTrack(
                id = id,
                title = title,
                artist = artist,
                album = album,
                albumArtist = albumArtist,
                year = year,
                trackNumber = trackNumber,
                discNumber = discNumber,
                durationMs = durationMs,
                dateAddedEpochSeconds = dateAddedEpochSeconds,
                dateModifiedEpochSeconds = dateModifiedEpochSeconds,
                fileName = fileName,
                folderPath = folderPath,
                albumId = albumId,
                fileSizeBytes = fileSizeBytes,
                contentUri = contentUri,
                titleSectionKey = titleSectionKey,
                titleSortKey = titleSortKey,
                mimeType = mimeType,
                bitrateBitsPerSecond = bitrateBitsPerSecond,
                sampleRateHz = sampleRateHz,
                channelCount = channelCount,
                bitDepth = bitDepth,
                audioPropertiesScanned = version >= 7 && completedLegacyRead,
            )
        }
        val actualChecksum = checksum.value
        val expectedChecksum = input.readLong()
        if (actualChecksum != expectedChecksum) {
            throw IOException("Music snapshot checksum mismatch")
        }
        return tracks
    }

    private fun DataOutputStream.writeNullableString(value: String?) {
        writeBoolean(value != null)
        if (value != null) {
            writeSizedString(value)
        }
    }

    private fun DataOutputStream.writeSizedString(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_STRING_BYTES) {
            "Music snapshot string is too large: ${bytes.size} bytes"
        }
        writeInt(bytes.size)
        write(bytes)
    }

    private fun DataInputStream.readNullableString(): String? =
        if (readBoolean()) readSizedString() else null

    private fun DataInputStream.readSizedString(): String {
        val byteCount = readInt()
        if (byteCount !in 0..MAX_STRING_BYTES) {
            throw IOException("Invalid music snapshot string size: $byteCount")
        }
        return ByteArray(byteCount)
            .also(::readFully)
            .toString(Charsets.UTF_8)
    }
}
