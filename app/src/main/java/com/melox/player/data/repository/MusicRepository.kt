package com.melox.player.data.repository

import android.content.ContentUris
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.DocumentsContract
import android.util.AtomicFile
import android.util.Log
import com.melox.player.data.library.AudioPropertiesReader
import com.melox.player.data.library.MusicLibrarySnapshotCodec
import com.melox.player.data.library.createMusicSortKeys
import com.melox.player.data.library.hasReusableAudioProperties
import com.melox.player.data.library.isWavSource
import com.melox.player.data.library.normalizeMusicFolderPath
import com.melox.player.model.MusicTrack
import java.io.IOException
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

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
            val cached = snapshotFile.openRead().use(MusicLibrarySnapshotCodec::read)
            val refreshed = cached.map { track ->
                if (!track.audioPropertiesScanned && isWavSource(track.fileName, track.mimeType)) {
                    enrichTrack(track)
                } else track
            }
            if (refreshed != cached) cacheMusic(refreshed)
            refreshed
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
        customFolderUris: List<String> = emptyList(),
        skipShortAudio: Boolean = false,
        onInitialTracks: suspend (List<MusicTrack>) -> Unit = {},
    ): List<MusicTrack> = withContext(Dispatchers.IO) {
        val previousTracksByUri = previousTracks.associateBy(MusicTrack::contentUri)
        val customFolderScopes = customFolderUris.mapNotNull(::customFolderScope)
        val collections = externalAudioCollections()
        var indexedTracks = queryIndexedTracks(
            collections = collections,
            previousTracksByUri = previousTracksByUri,
            refreshAudioProperties = refreshAudioProperties,
            onlyTrackId = onlyTrackId,
            customFolderUrisPresent = customFolderUris.isNotEmpty(),
            customFolderScopes = customFolderScopes,
            skipShortAudio = skipShortAudio,
        )

        if (onlyTrackId == null) {
            onInitialTracks(indexedTracks.sortedForLibrary())
        }

        val customDocuments = if (onlyTrackId == null) {
            val discovery = CustomFolderAudioDiscovery(contentResolver)
            customFolderUris.flatMap(discovery::enumerate)
        } else {
            emptyList()
        }
        if (customDocuments.isNotEmpty()) {
            val indexedKeys = indexedTracks.mapTo(HashSet(), MusicTrack::audioFileIdentity)
            val missingDocuments = customDocuments.filterNot { document ->
                document.audioFileIdentity() in indexedKeys
            }
            requestMediaStoreIndexing(
                missingDocuments.mapNotNull(CustomFolderAudioDocument::scannerPath),
            )
            if (missingDocuments.any { it.scannerPath != null }) {
                indexedTracks = queryIndexedTracks(
                    collections = collections,
                    previousTracksByUri = previousTracksByUri,
                    refreshAudioProperties = refreshAudioProperties,
                    onlyTrackId = onlyTrackId,
                    customFolderUrisPresent = true,
                    customFolderScopes = customFolderScopes,
                    skipShortAudio = skipShortAudio,
                )
            }
        }

        val enrichedIndexedTracks = indexedTracks.map(::enrichTrack)
        val indexedKeys = enrichedIndexedTracks.mapTo(HashSet(), MusicTrack::audioFileIdentity)
        val directDocumentTracks = customDocuments
            .asSequence()
            .filterNot { it.audioFileIdentity() in indexedKeys }
            .map(::createDocumentTrack)
            .filter { track -> !skipShortAudio || track.durationMs >= MIN_AUDIO_DURATION_MS }
            .toList()
        (enrichedIndexedTracks + directDocumentTracks)
            .distinctBy(MusicTrack::contentUri)
            .sortedForLibrary()
    }

    private fun queryIndexedTracks(
        collections: List<MediaStoreCollection>,
        previousTracksByUri: Map<String, MusicTrack>,
        refreshAudioProperties: Boolean,
        onlyTrackId: Long?,
        customFolderUrisPresent: Boolean,
        customFolderScopes: List<CustomFolderScope>,
        skipShortAudio: Boolean,
    ): List<MusicTrack> {
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
        val selection = buildList {
            if (skipShortAudio) add("${MediaStore.Audio.Media.DURATION} >= ?")
            if (onlyTrackId != null) add("${MediaStore.Audio.Media._ID} = ?")
        }.takeIf(List<String>::isNotEmpty)?.joinToString(" AND ")
        val selectionArgs = buildList {
            if (skipShortAudio) add(MIN_AUDIO_DURATION_MS.toString())
            onlyTrackId?.let { add(it.toString()) }
        }.takeIf(List<String>::isNotEmpty)?.toTypedArray()

        val tracks = mutableListOf<MusicTrack>()
        val seenContentUris = HashSet<String>()
        collections.forEach { mediaCollection ->
            val cursor = runCatching {
                contentResolver.query(
                    mediaCollection.uri,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                )
            }.onFailure { exception ->
                Log.w(TAG, "Unable to query audio volume ${mediaCollection.volumeName}", exception)
            }.getOrNull() ?: return@forEach
            cursor.use {
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

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val trackId = stableMediaStoreTrackId(mediaCollection.volumeName, id)
                    val dateModifiedEpochSeconds = cursor
                        .getLong(dateModifiedColumn)
                        .coerceAtLeast(0L)
                    val fileSizeBytes = cursor.getLong(fileSizeColumn).coerceAtLeast(0L)
                    val contentUri = ContentUris.withAppendedId(mediaCollection.uri, id).toString()
                    if (!seenContentUris.add(contentUri)) continue
                    val rawFolderPath = cursor.getString(folderColumnIndex)
                    if (customFolderUrisPresent &&
                        customFolderScopes.none { scope ->
                            customFolderScopeMatches(
                                scope = scope,
                                volumeName = mediaCollection.volumeName,
                                rawPath = rawFolderPath,
                                includesFileName = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q,
                                sdkInt = Build.VERSION.SDK_INT,
                            )
                        }
                    ) continue
                    val normalizedFolderPath = normalizeMusicFolderPath(
                        rawPath = rawFolderPath,
                        includesFileName = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q,
                    )
                    val reusableTrack = previousTracksByUri[contentUri]?.takeIf { previousTrack ->
                        !refreshAudioProperties &&
                            previousTrack.hasReusableAudioProperties(
                                id = trackId,
                                contentUri = contentUri,
                                dateModifiedEpochSeconds = dateModifiedEpochSeconds,
                                fileSizeBytes = fileSizeBytes,
                            )
                    }
                    val mediaStoreTrack = cursor.getInt(trackColumn).takeIf { it > 0 }
                    val title = reusableTrack?.title
                        ?: cursor.getString(titleColumn).metadataOrNull()
                    val titleSortKeys = createMusicSortKeys(title)
                    tracks.add(
                        MusicTrack(
                            id = trackId,
                            title = title,
                            artist = reusableTrack?.artist
                                ?: cursor.getString(artistColumn).metadataOrNull(),
                            album = reusableTrack?.album
                                ?: cursor.getString(albumColumn).metadataOrNull(),
                            albumId = cursor.getLong(albumIdColumn).takeIf { it > 0L },
                            mediaStoreId = id,
                            albumArtist = reusableTrack?.albumArtist
                                ?: albumArtistColumnIndex
                                    ?.let(cursor::getString)
                                    .metadataOrNull(),
                            year = reusableTrack?.year
                                ?: cursor.getInt(yearColumn).takeIf { it > 0 },
                            trackNumber = reusableTrack?.trackNumber
                                ?: mediaStoreTrack?.rem(MEDIASTORE_DISC_FACTOR)
                                    ?.takeIf { it > 0 },
                            discNumber = reusableTrack?.discNumber
                                ?: mediaStoreTrack?.div(MEDIASTORE_DISC_FACTOR)
                                    ?.takeIf { it > 0 },
                            durationMs = reusableTrack?.durationMs
                                ?: cursor.getLong(durationColumn).coerceAtLeast(0L),
                            dateAddedEpochSeconds = cursor.getLong(dateAddedColumn).coerceAtLeast(0L),
                            dateModifiedEpochSeconds = dateModifiedEpochSeconds,
                            fileName = cursor.getString(fileNameColumn).metadataOrNull(),
                            folderPath = normalizedFolderPath,
                            fileSizeBytes = fileSizeBytes,
                            contentUri = contentUri,
                            titleSectionKey = titleSortKeys.section,
                            titleSortKey = titleSortKeys.value,
                            mimeType = cursor.getString(mimeTypeColumn).metadataOrNull(),
                            bitrateBitsPerSecond = reusableTrack?.bitrateBitsPerSecond,
                            sampleRateHz = reusableTrack?.sampleRateHz,
                            channelCount = reusableTrack?.channelCount,
                            bitDepth = reusableTrack?.bitDepth,
                            audioPropertiesScanned = reusableTrack != null,
                        ),
                    )
                }
            }
        }
        return tracks
    }

    private fun enrichTrack(track: MusicTrack): MusicTrack {
        if (track.audioPropertiesScanned) return track
        val audioProperties = audioPropertiesReader.read(track.contentUri)
        val title = audioProperties?.title ?: track.title
        val titleSortKeys = createMusicSortKeys(title)
        return track.copy(
            title = title,
            artist = audioProperties?.artist ?: track.artist,
            album = audioProperties?.album ?: track.album,
            albumArtist = audioProperties?.albumArtist ?: track.albumArtist,
            year = audioProperties?.year ?: track.year,
            trackNumber = audioProperties?.trackNumber ?: track.trackNumber,
            discNumber = audioProperties?.discNumber ?: track.discNumber,
            durationMs = audioProperties?.durationMs ?: track.durationMs,
            titleSectionKey = titleSortKeys.section,
            titleSortKey = titleSortKeys.value,
            bitrateBitsPerSecond = audioProperties?.bitrateBitsPerSecond,
            sampleRateHz = audioProperties?.sampleRateHz,
            channelCount = audioProperties?.channelCount,
            bitDepth = audioProperties?.bitDepth,
            audioPropertiesScanned = audioProperties != null,
        )
    }

    private fun createDocumentTrack(document: CustomFolderAudioDocument): MusicTrack {
        val uriString = document.uri.toString()
        val fileTitle = document.displayName.substringBeforeLast('.', document.displayName)
            .metadataOrNull()
        val titleSortKeys = createMusicSortKeys(fileTitle)
        return enrichTrack(
            MusicTrack(
                id = stableDocumentTrackId(uriString),
                title = fileTitle,
                artist = null,
                album = null,
                durationMs = 0L,
                dateAddedEpochSeconds = document.dateModifiedEpochSeconds,
                dateModifiedEpochSeconds = document.dateModifiedEpochSeconds,
                fileName = document.displayName,
                fileSizeBytes = document.fileSizeBytes,
                contentUri = uriString,
                titleSectionKey = titleSortKeys.section,
                titleSortKey = titleSortKeys.value,
                folderPath = document.folderPath,
                mimeType = document.mimeType,
            ),
        )
    }

    private fun externalAudioCollections(): List<MediaStoreCollection> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return listOf(
                MediaStoreCollection(
                    volumeName = LEGACY_EXTERNAL_VOLUME,
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                ),
            )
        }
        val volumeNames = runCatching {
            MediaStore.getExternalVolumeNames(applicationContext)
        }.onFailure { exception ->
            Log.w(TAG, "Unable to enumerate external media volumes", exception)
        }.getOrDefault(emptySet())
        return volumeNames
            .ifEmpty { setOf(MediaStore.VOLUME_EXTERNAL_PRIMARY) }
            .sorted()
            .map { volumeName ->
                MediaStoreCollection(
                    volumeName = volumeName,
                    uri = MediaStore.Audio.Media.getContentUri(volumeName),
                )
            }
    }

    private suspend fun requestMediaStoreIndexing(paths: List<String>) {
        withTimeoutOrNull(MEDIA_SCANNER_TIMEOUT_MS) {
            paths.distinct().chunked(MEDIA_SCANNER_BATCH_SIZE).forEach { batch ->
                suspendCancellableCoroutine { continuation ->
                    val remaining = AtomicInteger(batch.size)
                    MediaScannerConnection.scanFile(
                        applicationContext,
                        batch.toTypedArray(),
                        null,
                    ) { _, _ ->
                        if (remaining.decrementAndGet() == 0 && continuation.isActive) {
                            continuation.resume(Unit)
                        }
                    }
                }
            }
        }
    }

    /** Re-reads one MediaStore row and its embedded tags without scanning the full library. */
    suspend fun refreshTrack(track: MusicTrack): MusicTrack? {
        val uri = runCatching { Uri.parse(track.contentUri) }.getOrNull()
        if (uri != null && DocumentsContract.isDocumentUri(applicationContext, uri)) {
            return withContext(Dispatchers.IO) {
                enrichTrack(track.copy(audioPropertiesScanned = false))
            }
        }
        return scanMusic(
            previousTracks = listOf(track),
            refreshAudioProperties = true,
            onlyTrackId = track.mediaStoreId
                ?: Uri.parse(track.contentUri).lastPathSegment?.toLongOrNull()
                ?: track.id,
        ).firstOrNull { refreshedTrack -> refreshedTrack.contentUri == track.contentUri }
    }

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

    suspend fun clearCachedMusic() = withContext(Dispatchers.IO) {
        snapshotFile.delete()
    }

    private companion object {
        private const val TAG = "MusicRepository"
        private const val SNAPSHOT_FILE_NAME = "music_library_snapshot.bin"
        private const val MAX_SNAPSHOT_BYTES = 64L * 1024L * 1024L
        private const val MEDIASTORE_DISC_FACTOR = 1_000
        private const val MEDIA_SCANNER_BATCH_SIZE = 32
        private const val MEDIA_SCANNER_TIMEOUT_MS = 15_000L
        private const val MIN_AUDIO_DURATION_MS = 60_000L
        private const val LEGACY_EXTERNAL_VOLUME = "external"
    }
}

private data class MediaStoreCollection(
    val volumeName: String,
    val uri: Uri,
)

private fun List<MusicTrack>.sortedForLibrary(): List<MusicTrack> = sortedWith(
    compareBy<MusicTrack>(MusicTrack::titleSortKey)
        .thenBy(MusicTrack::contentUri),
)

private fun MusicTrack.audioFileIdentity(): String {
    val volumeName = Uri.parse(contentUri).pathSegments.firstOrNull().orEmpty()
    return audioFileIdentity(volumeName, folderPath, fileName)
}

private fun CustomFolderAudioDocument.audioFileIdentity(): String =
    audioFileIdentity(volumeName.orEmpty(), folderPath, displayName)

private fun audioFileIdentity(
    volumeName: String,
    folderPath: String?,
    fileName: String?,
): String = listOf(volumeName, folderPath.orEmpty(), fileName.orEmpty())
    .joinToString("|") { value -> value.lowercase(Locale.ROOT) }

internal fun stableMediaStoreTrackId(volumeName: String, mediaStoreId: Long): Long {
    if (volumeName.equals(MediaStore.VOLUME_EXTERNAL_PRIMARY, ignoreCase = true) ||
        volumeName.equals(LEGACY_EXTERNAL_VOLUME_NAME, ignoreCase = true)
    ) return mediaStoreId
    val hash = stableDocumentTrackId("$volumeName:$mediaStoreId") and SECONDARY_ID_HASH_MASK
    return SECONDARY_ID_MARKER or hash
}

private fun customFolderScope(uriString: String): CustomFolderScope? {
    val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null
    if (uri.authority != EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY) return null
    val documentId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
        ?: return null
    return customFolderScopeForDocumentId(documentId, Build.VERSION.SDK_INT)
}

private const val EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY =
    "com.android.externalstorage.documents"
private const val LEGACY_EXTERNAL_VOLUME_NAME = "external"
private const val SECONDARY_ID_MARKER = 1L shl 62
private const val SECONDARY_ID_HASH_MASK = SECONDARY_ID_MARKER - 1L

private fun customFolderPrefix(uriString: String): String? {
    val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null
    val documentId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
        ?: return null
    return customFolderPrefixForDocumentId(documentId, Build.VERSION.SDK_INT)
}

internal fun customFolderPrefixForDocumentId(
    documentId: String,
    sdkInt: Int,
): String? {
    val separator = documentId.indexOf(':')
    if (separator <= 0) return null
    val volume = documentId.substring(0, separator)
    val path = documentId.substring(separator + 1).trim('/')
    return if (volume.equals("primary", ignoreCase = true)) {
        if (sdkInt >= Build.VERSION_CODES.Q) {
            "/${path.takeIf(String::isNotEmpty).orEmpty()}".trimEnd('/').ifEmpty { "/" }
        } else {
            "/storage/emulated/0/${path.takeIf(String::isNotEmpty).orEmpty()}"
                .trimEnd('/')
                .ifEmpty { "/storage/emulated/0" }
        }
    } else {
        null
    }
}

internal fun folderMatchesPrefix(
    rawPath: String?,
    includesFileName: Boolean,
    prefix: String,
): Boolean {
    val normalized = normalizeMusicFolderPath(rawPath, includesFileName) ?: return false
    val normalizedPrefix = prefix.trimEnd('/').ifEmpty { "/" }
    return normalized == normalizedPrefix ||
        normalized.startsWith("$normalizedPrefix/") ||
        normalizedPrefix == "/"
}

/** Treats MediaStore's canonical `<unknown>` marker like missing metadata for the UI to localize. */
private fun String?.metadataOrNull(): String? =
    this
        ?.trim()
        ?.takeUnless { value ->
            value.isEmpty() || value == MediaStore.UNKNOWN_STRING
        }
