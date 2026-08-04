package com.melox.player.data.playback

import android.content.Context
import android.util.AtomicFile
import androidx.core.net.toUri
import androidx.media3.common.Player
import com.melox.player.model.PlaybackQueueItem
import com.melox.player.model.PlaybackSnapshot
import com.melox.player.model.PlaybackMode
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.CRC32
import java.util.zip.CheckedInputStream
import java.util.zip.CheckedOutputStream

/** Compact, checksummed encoding for playback restoration. */
internal object PlaybackSnapshotCodec {
    private const val MAGIC = 0x4D454C50
    private const val VERSION = 2
    private const val MIN_SUPPORTED_VERSION = 1
    private const val MAX_QUEUE_SIZE = 100_000
    private const val MAX_STRING_BYTES = 1_048_576

    fun write(outputStream: OutputStream, snapshot: PlaybackSnapshot) {
        require(snapshot.queue.size <= MAX_QUEUE_SIZE)
        val checksum = CRC32()
        val output = DataOutputStream(
            CheckedOutputStream(BufferedOutputStream(outputStream), checksum),
        )
        output.writeInt(MAGIC)
        output.writeInt(VERSION)
        output.writeInt(snapshot.queue.size)
        snapshot.queue.forEach { item ->
            output.writeSizedString(item.mediaId)
            output.writeNullableLong(item.trackId)
            output.writeSizedString(item.contentUri)
            output.writeSizedString(item.title)
            output.writeNullableString(item.artist)
            output.writeNullableString(item.album)
            output.writeLong(item.durationMs)
            output.writeLong(item.dateModifiedEpochSeconds)
            output.writeLong(item.fileSizeBytes)
            output.writeDouble(item.sourceOrder)
            output.writeInt(item.playbackMode.ordinal)
        }
        output.writeInt(snapshot.currentIndex)
        output.writeLong(snapshot.positionMs)
        output.writeInt(snapshot.playbackMode.ordinal)
        output.writeLong(checksum.value)
        output.flush()
    }

    fun read(inputStream: InputStream): PlaybackSnapshot {
        val checksum = CRC32()
        val input = DataInputStream(
            CheckedInputStream(BufferedInputStream(inputStream), checksum),
        )
        if (input.readInt() != MAGIC) throw IOException("Unrecognized playback snapshot")
        val version = input.readInt()
        if (version !in MIN_SUPPORTED_VERSION..VERSION) {
            throw IOException("Unsupported playback snapshot version: $version")
        }
        val itemCount = input.readInt()
        if (itemCount !in 0..MAX_QUEUE_SIZE) {
            throw IOException("Invalid playback queue size: $itemCount")
        }
        val encodedQueue = List(itemCount) { index ->
            PlaybackQueueItem(
                mediaId = input.readSizedString(),
                trackId = input.readNullableLong(),
                contentUri = input.readSizedString(),
                title = input.readSizedString(),
                artist = input.readNullableString(),
                album = input.readNullableString(),
                durationMs = input.readLong(),
                dateModifiedEpochSeconds = input.readLong(),
                fileSizeBytes = input.readLong(),
                sourceOrder = if (version >= 2) input.readDouble() else index.toDouble(),
                playbackMode = if (version >= 2) {
                    input.readPlaybackMode()
                } else {
                    PlaybackMode.ORDER
                },
            )
        }
        val currentIndex = input.readInt()
        val positionMs = input.readLong()
        val playbackMode = if (version >= 2) {
            input.readPlaybackMode()
        } else {
            val shuffleEnabled = input.readBoolean()
            val repeatMode = input.readInt()
            if (
                repeatMode != Player.REPEAT_MODE_OFF &&
                repeatMode != Player.REPEAT_MODE_ONE &&
                repeatMode != Player.REPEAT_MODE_ALL
            ) {
                throw IOException("Invalid playback repeat mode: $repeatMode")
            }
            when {
                shuffleEnabled -> PlaybackMode.RANDOM
                repeatMode == Player.REPEAT_MODE_ONE -> PlaybackMode.REPEAT_ONE
                else -> PlaybackMode.ORDER
            }
        }
        val actualChecksum = checksum.value
        val expectedChecksum = input.readLong()
        if (actualChecksum != expectedChecksum) {
            throw IOException("Playback snapshot checksum mismatch")
        }
        if (
            encodedQueue.isEmpty() && currentIndex != -1 ||
            encodedQueue.isNotEmpty() && currentIndex !in encodedQueue.indices
        ) {
            throw IOException("Invalid playback queue index: $currentIndex")
        }
        val queue = encodedQueue.map { it.copy(playbackMode = playbackMode) }
        return PlaybackSnapshot(
            queue = queue,
            currentIndex = currentIndex,
            positionMs = positionMs.coerceAtLeast(0L),
            playbackMode = playbackMode,
        )
    }

    private fun DataOutputStream.writeNullableLong(value: Long?) {
        writeBoolean(value != null)
        if (value != null) writeLong(value)
    }

    private fun DataOutputStream.writeNullableString(value: String?) {
        writeBoolean(value != null)
        if (value != null) writeSizedString(value)
    }

    private fun DataOutputStream.writeSizedString(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_STRING_BYTES)
        writeInt(bytes.size)
        write(bytes)
    }

    private fun DataInputStream.readNullableLong(): Long? =
        if (readBoolean()) readLong() else null

    private fun DataInputStream.readNullableString(): String? =
        if (readBoolean()) readSizedString() else null

    private fun DataInputStream.readSizedString(): String {
        val byteCount = readInt()
        if (byteCount !in 0..MAX_STRING_BYTES) {
            throw IOException("Invalid playback snapshot string size: $byteCount")
        }
        return ByteArray(byteCount).also(::readFully).toString(Charsets.UTF_8)
    }

    private fun DataInputStream.readPlaybackMode(): PlaybackMode {
        val ordinal = readInt()
        return PlaybackMode.entries.getOrNull(ordinal)
            ?: throw IOException("Invalid playback mode: $ordinal")
    }
}

/** Owns atomic snapshot I/O and rejects items that can no longer be opened. */
internal class PlaybackSnapshotStore(context: Context) {
    private val applicationContext = context.applicationContext
    private val atomicFile = AtomicFile(File(applicationContext.noBackupFilesDir, FILE_NAME))

    fun load(): PlaybackSnapshot? {
        val snapshot = runCatching {
            atomicFile.openRead().use(PlaybackSnapshotCodec::read)
        }.getOrElse {
            atomicFile.delete()
            return null
        }
        return snapshot.retainReadableItems(::canOpen)
    }

    fun save(snapshot: PlaybackSnapshot) {
        if (snapshot.queue.isEmpty()) {
            atomicFile.delete()
            return
        }
        val output = atomicFile.startWrite()
        try {
            PlaybackSnapshotCodec.write(output, snapshot)
            atomicFile.finishWrite(output)
        } catch (exception: Exception) {
            atomicFile.failWrite(output)
            throw exception
        }
    }

    private fun canOpen(item: PlaybackQueueItem): Boolean = runCatching {
        applicationContext.contentResolver
            .openFileDescriptor(item.contentUri.toUri(), "r")
            ?.use { true }
            ?: false
    }.getOrDefault(false)

    private companion object {
        const val FILE_NAME = "playback_snapshot.bin"
    }
}

/**
 * Stores only the current queue item so the mini player can restore its identity before the
 * MediaController connection and full queue validation finish.
 */
internal class MiniPlaybackSnapshotStore(context: Context) {
    private val atomicFile = AtomicFile(
        File(context.applicationContext.noBackupFilesDir, FILE_NAME),
    )

    fun load(): PlaybackSnapshot? = runCatching {
        atomicFile.openRead().use(PlaybackSnapshotCodec::read)
    }.getOrElse {
        atomicFile.delete()
        null
    }

    fun save(snapshot: PlaybackSnapshot) {
        val currentItem = snapshot.queue.getOrNull(snapshot.currentIndex)
        if (currentItem == null) {
            atomicFile.delete()
            return
        }
        val summary = snapshot.copy(
            queue = listOf(currentItem),
            currentIndex = 0,
        )
        val output = atomicFile.startWrite()
        try {
            PlaybackSnapshotCodec.write(output, summary)
            atomicFile.finishWrite(output)
        } catch (exception: Exception) {
            atomicFile.failWrite(output)
            throw exception
        }
    }

    fun clear() {
        atomicFile.delete()
    }

    private companion object {
        const val FILE_NAME = "playback_mini_snapshot.bin"
    }
}

internal fun PlaybackSnapshot.retainReadableItems(
    isReadable: (PlaybackQueueItem) -> Boolean,
): PlaybackSnapshot? {
    val readableQueue = queue.filter(isReadable)
    if (readableQueue.isEmpty()) return null
    val currentMediaId = queue.getOrNull(currentIndex)?.mediaId
    val retainedCurrentIndex = readableQueue.indexOfFirst { it.mediaId == currentMediaId }
    val currentItemWasRetained = retainedCurrentIndex >= 0
    return copy(
        queue = readableQueue,
        currentIndex = retainedCurrentIndex.takeIf { currentItemWasRetained } ?: 0,
        positionMs = if (currentItemWasRetained) positionMs else 0L,
    )
}
