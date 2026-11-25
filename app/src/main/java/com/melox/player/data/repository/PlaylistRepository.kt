package com.melox.player.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.AtomicFile
import android.util.Log
import com.melox.player.data.playlist.PlaylistSnapshotCodec
import com.melox.player.model.LocalPlaylist
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Owns local-playlist persistence and read-access checks, independently from playback state. */
internal class PlaylistRepository(context: Context) {
    private val applicationContext = context.applicationContext
    private val atomicFile = AtomicFile(
        File(applicationContext.noBackupFilesDir, FILE_NAME),
    )
    private val coverMutex = Mutex()

    suspend fun load(): List<LocalPlaylist> = withContext(Dispatchers.IO) {
        if (!atomicFile.baseFile.exists()) return@withContext emptyList()
        if (atomicFile.baseFile.length() > MAX_SNAPSHOT_BYTES) {
            Log.w(TAG, "Ignoring oversized playlist snapshot")
            atomicFile.delete()
            return@withContext emptyList()
        }
        try {
            atomicFile.openRead().use(PlaylistSnapshotCodec::read)
        } catch (exception: IOException) {
            Log.w(TAG, "Ignoring unreadable playlist snapshot", exception)
            atomicFile.delete()
            emptyList()
        }
    }

    suspend fun save(playlists: List<LocalPlaylist>) = withContext(Dispatchers.IO) {
        if (playlists.isEmpty()) {
            atomicFile.delete()
            return@withContext
        }
        val output = atomicFile.startWrite()
        try {
            PlaylistSnapshotCodec.write(output, playlists)
            atomicFile.finishWrite(output)
        } catch (exception: Exception) {
            atomicFile.failWrite(output)
            throw exception
        }
    }

    suspend fun setCustomCover(playlistId: String, uri: Uri): Boolean = coverMutex.withLock {
        withContext(Dispatchers.IO) {
            val destination = playlistCoverFile(applicationContext, playlistId)
            val file = AtomicFile(destination)
            try {
                val source = ImageDecoder.createSource(applicationContext.contentResolver, uri)
                val bitmap = decodePlaylistCover(source, PLAYLIST_COVER_MAX_EDGE_PX)
                try {
                    ensureActive()
                    val directory = requireNotNull(destination.parentFile)
                    check(directory.isDirectory || directory.mkdirs())
                    val output = file.startWrite()
                    try {
                        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                            throw IOException("Could not encode playlist cover")
                        }
                        ensureActive()
                        file.finishWrite(output)
                    } catch (failure: Throwable) {
                        file.failWrite(output)
                        throw failure
                    }
                } finally {
                    bitmap.recycle()
                }
                true
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                false
            } catch (_: OutOfMemoryError) {
                false
            }
        }
    }

    suspend fun deleteCustomCover(playlistId: String) = coverMutex.withLock {
        withContext(Dispatchers.IO) {
            AtomicFile(playlistCoverFile(applicationContext, playlistId)).delete()
        }
    }

    suspend fun readableContentUris(contentUris: Collection<String>): Set<String> =
        withContext(Dispatchers.IO) {
            contentUris.asSequence()
                .distinct()
                .filterTo(mutableSetOf()) { contentUri ->
                    runCatching {
                        applicationContext.contentResolver
                            .openFileDescriptor(Uri.parse(contentUri), "r")
                            ?.use { true }
                            ?: false
                    }.getOrDefault(false)
                }
        }

    private companion object {
        const val TAG = "PlaylistRepository"
        const val FILE_NAME = "local_playlists.bin"
        const val MAX_SNAPSHOT_BYTES = 64L * 1024L * 1024L
    }
}

private const val PLAYLIST_COVER_MAX_EDGE_PX = 1024

private fun playlistCoverFile(context: Context, playlistId: String): File {
    val fileId = UUID.nameUUIDFromBytes(playlistId.toByteArray(Charsets.UTF_8))
    return File(context.noBackupFilesDir, "playlist_covers/$fileId.png")
}

internal fun loadPlaylistCustomCover(context: Context, playlistId: String, targetEdgePx: Int): Bitmap? {
    val file = playlistCoverFile(context, playlistId)
    if (!file.isFile) return null
    return try {
        decodePlaylistCover(ImageDecoder.createSource(file), targetEdgePx)
    } catch (_: IOException) {
        null
    } catch (_: OutOfMemoryError) {
        null
    }
}

private fun decodePlaylistCover(source: ImageDecoder.Source, targetEdgePx: Int): Bitmap =
    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        val width = info.size.width
        val height = info.size.height
        val scale = minOf(1.0, targetEdgePx.toDouble() / maxOf(width, height))
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.setTargetSize(
            (width * scale).roundToInt().coerceAtLeast(1),
            (height * scale).roundToInt().coerceAtLeast(1),
        )
    }
