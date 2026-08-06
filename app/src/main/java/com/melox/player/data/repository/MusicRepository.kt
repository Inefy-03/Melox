package com.melox.player.data.repository

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.AtomicFile
import android.util.Log
import com.melox.player.data.library.AudioPropertiesReader
import com.melox.player.data.library.LocalAudioProperties
import com.melox.player.data.library.MusicLibrarySnapshotCodec
import com.melox.player.data.library.createMusicSortKeys
import com.melox.player.data.library.hasReusableAudioProperties
import com.melox.player.data.library.normalizeMusicFolderPath
import com.melox.player.model.MusicTrack
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads music indexed in shared storage. The caller must hold the platform audio permission. */
class MusicRepository(context: Context) {
    private val applicationContext = context.applicationContext
    private val contentResolver = applicationContext.contentResolver
    private val audioPropertiesReader = AudioPropertiesReader(contentResolver)
    private val snapshotFile by lazy {
        AtomicFile(
            applicationContext.noBackupFilesDir.resolve(SNAPSHOT_FILE_NAME),
        )
    }

    /** Returns the last successful scan, or null when no compatible snapshot is available. */
    suspend fun loadCachedMusic(): List<MusicTrack>? = withContext(Dispatchers.IO) {
        if (!snapshotFile.baseFile.exists() || snapshotFile.baseFile.length() > MAX_SNAPSHOT_BYTES) {
            return@withContext null
        }
        try {
            snapshotFile.openRead().use(MusicLibrarySnapshotCodec::read)
        } catch (exception: IOException) {
            Log.w(TAG, "Ignoring unreadable music snapshot", exception)
            snapshotFile.delete()
            null
        }
    }

    /** Returns MediaStore music rows ordered by title without blocking the caller's dispatcher. */
    suspend fun scanMusic(
        previousTracks: List<MusicTrack> = emptyList(),
        refreshAudioProperties: Boolean = false,
        onlyTrackId: Long? = null,
    ): List<MusicTrack> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val previousTracksById = previousTracks.associateBy(MusicTrack::id)
        val albumArtistColumn = MediaStore.Audio.AudioColumns.ALBUM_ARTIST
            .takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.R }
        val folderColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.RELATIVE_PATH
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Audio.Media.DATA
        }
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            albumArtistColumn?.let(::add)
            add(MediaStore.Audio.Media.YEAR)
            add(MediaStore.Audio.Media.TRACK)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.DATE_MODIFIED)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            add(folderColumn)
            add(MediaStore.Audio.Media.SIZE)
            add(MediaStore.Audio.Media.MIME_TYPE)
        }.toTypedArray()
        val selection = buildString {
            append("${MediaStore.Audio.Media.IS_MUSIC} != 0")
            if (onlyTrackId != null) {
                append(" AND ${MediaStore.Audio.Media._ID} = ?")
            }
        }
        val selectionArgs = onlyTrackId?.let { arrayOf(it.toString()) }

        contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val albumArtistColumnIndex = albumArtistColumn?.let(cursor::getColumnIndex)
                ?.takeIf { it >= 0 }
            val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val fileNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val folderColumnIndex = cursor.getColumnIndexOrThrow(folderColumn)
            val fileSizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

            buildList {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val dateModifiedEpochSeconds = cursor
                        .getLong(dateModifiedColumn)
                        .coerceAtLeast(0L)
                    val fileSizeBytes = cursor.getLong(fileSizeColumn).coerceAtLeast(0L)
                    val contentUri = ContentUris.withAppendedId(collection, id).toString()
                    val reusableTrack = previousTracksById[id]?.takeIf { previousTrack ->
                        !refreshAudioProperties &&
                            previousTrack.hasReusableAudioProperties(
                                id = id,
                                contentUri = contentUri,
                                dateModifiedEpochSeconds = dateModifiedEpochSeconds,
                                fileSizeBytes = fileSizeBytes,
                            )
                    }
                    val audioProperties = reusableTrack?.let { previousTrack ->
                        LocalAudioProperties(
                            durationMs = previousTrack.durationMs.takeIf { it > 0L },
                            bitrateBitsPerSecond = previousTrack.bitrateBitsPerSecond,
                            sampleRateHz = previousTrack.sampleRateHz,
                            channelCount = previousTrack.channelCount,
                            title = previousTrack.title,
                            artist = previousTrack.artist,
                            album = previousTrack.album,
                            albumArtist = previousTrack.albumArtist,
                            year = previousTrack.year,
                            trackNumber = previousTrack.trackNumber,
                            discNumber = previousTrack.discNumber,
                            bitDepth = previousTrack.bitDepth,
                        )
                    } ?: audioPropertiesReader.read(contentUri)
                    val mediaStoreTrack = cursor.getInt(trackColumn).takeIf { it > 0 }
                    val title = audioProperties?.title
                        ?: cursor.getString(titleColumn).metadataOrNull()
                    val titleSortKeys = createMusicSortKeys(title)
                    add(
                        MusicTrack(
                            id = id,
                            title = title,
                            artist = audioProperties?.artist
                                ?: cursor.getString(artistColumn).metadataOrNull(),
                            album = audioProperties?.album
                                ?: cursor.getString(albumColumn).metadataOrNull(),
                            albumId = cursor.getLong(albumIdColumn).takeIf { it > 0L },
                            albumArtist = audioProperties?.albumArtist
                                ?: albumArtistColumnIndex
                                    ?.let(cursor::getString)
                                    .metadataOrNull(),
                            year = audioProperties?.year
                                ?: cursor.getInt(yearColumn).takeIf { it > 0 },
                            trackNumber = audioProperties?.trackNumber
                                ?: mediaStoreTrack?.rem(MEDIASTORE_DISC_FACTOR)
                                    ?.takeIf { it > 0 },
                            discNumber = audioProperties?.discNumber
                                ?: mediaStoreTrack?.div(MEDIASTORE_DISC_FACTOR)
                                    ?.takeIf { it > 0 },
                            durationMs = audioProperties?.durationMs
                                ?: cursor.getLong(durationColumn).coerceAtLeast(0L),
                            dateAddedEpochSeconds = cursor.getLong(dateAddedColumn).coerceAtLeast(0L),
                            dateModifiedEpochSeconds = dateModifiedEpochSeconds,
                            fileName = cursor.getString(fileNameColumn).metadataOrNull(),
                            folderPath = normalizeMusicFolderPath(
                                rawPath = cursor.getString(folderColumnIndex),
                                includesFileName =
                                    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q,
                            ),
                            fileSizeBytes = fileSizeBytes,
                            contentUri = contentUri,
                            titleSectionKey = titleSortKeys.section,
                            titleSortKey = titleSortKeys.value,
                            mimeType = cursor.getString(mimeTypeColumn).metadataOrNull(),
                            bitrateBitsPerSecond = audioProperties?.bitrateBitsPerSecond,
                            sampleRateHz = audioProperties?.sampleRateHz,
                            channelCount = audioProperties?.channelCount,
                            bitDepth = audioProperties?.bitDepth,
                            audioPropertiesScanned = true,
                        ),
                    )
                }
            }.sortedWith(
                compareBy<MusicTrack>(MusicTrack::titleSortKey)
                    .thenBy(MusicTrack::id),
            )
        } ?: emptyList()
    }

    /** Re-reads one MediaStore row and its embedded tags without scanning the full library. */
    suspend fun refreshTrack(track: MusicTrack): MusicTrack? = scanMusic(
        previousTracks = listOf(track),
        refreshAudioProperties = true,
        onlyTrackId = track.id,
    ).singleOrNull()

    /** Atomically stores a successful scan without changing the visible scan result on failure. */
    suspend fun cacheMusic(tracks: List<MusicTrack>) = withContext(Dispatchers.IO) {
        val output = try {
            snapshotFile.startWrite()
        } catch (exception: IOException) {
            Log.w(TAG, "Unable to start music snapshot write", exception)
            return@withContext
        }

        try {
            MusicLibrarySnapshotCodec.write(output, tracks)
            snapshotFile.finishWrite(output)
        } catch (exception: IOException) {
            snapshotFile.failWrite(output)
            Log.w(TAG, "Unable to write music snapshot", exception)
        } catch (exception: IllegalArgumentException) {
            snapshotFile.failWrite(output)
            Log.w(TAG, "Unable to encode music snapshot", exception)
        }
    }

    private companion object {
        private const val TAG = "MusicRepository"
        private const val SNAPSHOT_FILE_NAME = "music_library_snapshot.bin"
        private const val MAX_SNAPSHOT_BYTES = 64L * 1024L * 1024L
        private const val MEDIASTORE_DISC_FACTOR = 1_000
    }
}

/** Treats MediaStore's canonical `<unknown>` marker like missing metadata for the UI to localize. */
private fun String?.metadataOrNull(): String? =
    this
        ?.trim()
        ?.takeUnless { value ->
            value.isEmpty() || value == MediaStore.UNKNOWN_STRING
        }
