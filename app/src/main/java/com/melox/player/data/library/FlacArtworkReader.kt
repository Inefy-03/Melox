package com.melox.player.data.library

import java.io.DataInputStream
import java.io.EOFException
import java.io.InputStream

internal data class FlacPictureMetadata(
    val cover: FlacPictureLocation?,
    val hasPictureBlocks: Boolean,
)

internal data class FlacPictureLocation(val offset: Long, val length: Int) {
    fun readFrom(input: InputStream): ByteArray {
        input.skipFully(offset)
        return ByteArray(length).also { DataInputStream(input).readFully(it) }
    }
}

/** Scans FLAC metadata and records only the selected picture's byte range. */
internal fun readFlacPictureMetadata(stream: InputStream): FlacPictureMetadata? {
    val input = DataInputStream(stream)
    try {
        if (input.readInt() != 0x664C6143) return null // fLaC
    } catch (_: EOFException) {
        return null
    }

    var bestPriority = Int.MAX_VALUE
    var cover: FlacPictureLocation? = null
    var nextBlockOffset = 4L
    var hasPictureBlocks = false
    try {
        do {
            val header = input.readInt()
            val blockLength = header and 0xFFFFFF
            val blockOffset = nextBlockOffset + 4
            nextBlockOffset = blockOffset + blockLength
            val isPicture = (header ushr 24) and 0x7F == 6
            hasPictureBlocks = hasPictureBlocks || isPicture
            if (!isPicture || blockLength < 32) {
                input.skipFully(blockLength.toLong())
                continue
            }

            var remaining = blockLength - 4
            val priority = coverPicturePriority(input.readInt())
            if (priority != null && priority < bestPriority) {
                val mimeLength = input.readInt()
                remaining -= 4
                if (mimeLength in 0..(remaining - 24)) {
                    input.skipFully(mimeLength.toLong())
                    val descriptionLength = input.readInt()
                    remaining -= mimeLength + 4
                    if (descriptionLength in 0..(remaining - 20)) {
                        input.skipFully(descriptionLength.toLong() + 16) // Dimensions and color fields
                        val pictureLength = input.readInt()
                        remaining -= descriptionLength + 20
                        if (pictureLength in 1..remaining) {
                            cover = FlacPictureLocation(nextBlockOffset - remaining, pictureLength)
                            bestPriority = priority
                            if (bestPriority == 0) return FlacPictureMetadata(cover, hasPictureBlocks)
                        }
                    }
                }
            }
            input.skipFully(remaining.toLong())
        } while (header >= 0)
    } catch (_: EOFException) {
        // Keep a valid cover already found before a truncated block.
    }
    return FlacPictureMetadata(cover, hasPictureBlocks)
}
