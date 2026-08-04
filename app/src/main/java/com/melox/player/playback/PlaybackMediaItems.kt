package com.melox.player.playback

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.melox.player.R
import com.melox.player.model.MusicTrack
import com.melox.player.model.PlaybackQueueItem
import com.melox.player.model.PlaybackMode

private const val EXTRA_TRACK_ID = "com.melox.player.TRACK_ID"
private const val EXTRA_CONTENT_URI = "com.melox.player.CONTENT_URI"
private const val EXTRA_DURATION = "com.melox.player.DURATION"
private const val EXTRA_DATE_MODIFIED = "com.melox.player.DATE_MODIFIED"
private const val EXTRA_FILE_SIZE = "com.melox.player.FILE_SIZE"
private const val EXTRA_SOURCE_ORDER = "com.melox.player.SOURCE_ORDER"
private const val EXTRA_PLAYBACK_MODE = "com.melox.player.PLAYBACK_MODE"

internal fun MusicTrack.toPlaybackQueueItem(
    context: Context,
    sourceOrder: Double = 0.0,
    playbackMode: PlaybackMode = PlaybackMode.ORDER,
): PlaybackQueueItem =
    PlaybackQueueItem(
        mediaId = id.toString(),
        trackId = id,
        contentUri = contentUri,
        title = title ?: context.getString(R.string.music_unknown_title),
        artist = artist,
        album = album,
        durationMs = durationMs,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileSizeBytes = fileSizeBytes,
        sourceOrder = sourceOrder,
        playbackMode = playbackMode,
    )

internal fun Uri.toExternalPlaybackQueueItem(context: Context): PlaybackQueueItem {
    val metadata = context.queryExternalAudioMetadata(this)
    val fallbackName = lastPathSegment
        ?.substringAfterLast('/')
        ?.takeIf(String::isNotBlank)
    return PlaybackQueueItem(
        mediaId = "external:$this",
        trackId = null,
        contentUri = toString(),
        title = metadata?.title
            ?: metadata?.displayName?.substringBeforeLast('.', metadata.displayName)
            ?: fallbackName
            ?: context.getString(R.string.music_unknown_title),
        artist = metadata?.artist,
        album = metadata?.album,
        durationMs = metadata?.durationMs ?: 0L,
        dateModifiedEpochSeconds = metadata?.dateModifiedEpochSeconds ?: 0L,
        fileSizeBytes = metadata?.fileSizeBytes ?: 0L,
        sourceOrder = 0.0,
        playbackMode = PlaybackMode.ORDER,
    )
}

private data class ExternalAudioMetadata(
    val displayName: String?,
    val title: String?,
    val artist: String?,
    val album: String?,
    val durationMs: Long,
    val dateModifiedEpochSeconds: Long,
    val fileSizeBytes: Long,
)

private fun Context.queryExternalAudioMetadata(uri: Uri): ExternalAudioMetadata? {
    val richProjection = arrayOf(
        OpenableColumns.DISPLAY_NAME,
        OpenableColumns.SIZE,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATE_MODIFIED,
    )
    runCatching {
        contentResolver.query(uri, richProjection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            ExternalAudioMetadata(
                displayName = cursor.stringOrNull(OpenableColumns.DISPLAY_NAME),
                title = cursor.stringOrNull(MediaStore.Audio.Media.TITLE),
                artist = cursor.stringOrNull(MediaStore.Audio.Media.ARTIST),
                album = cursor.stringOrNull(MediaStore.Audio.Media.ALBUM),
                durationMs = cursor.longOrZero(MediaStore.Audio.Media.DURATION),
                dateModifiedEpochSeconds = cursor.longOrZero(MediaStore.Audio.Media.DATE_MODIFIED),
                fileSizeBytes = cursor.longOrZero(OpenableColumns.SIZE),
            )
        }
    }.getOrNull()?.let { return it }

    return runCatching {
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            ExternalAudioMetadata(
                displayName = cursor.stringOrNull(OpenableColumns.DISPLAY_NAME),
                title = null,
                artist = null,
                album = null,
                durationMs = 0L,
                dateModifiedEpochSeconds = 0L,
                fileSizeBytes = cursor.longOrZero(OpenableColumns.SIZE),
            )
        }
    }.getOrNull()
}

private fun android.database.Cursor.stringOrNull(columnName: String): String? {
    val columnIndex = getColumnIndex(columnName)
    return if (columnIndex >= 0 && !isNull(columnIndex)) {
        getString(columnIndex)
            ?.trim()
            ?.takeUnless { it.isEmpty() || it == MediaStore.UNKNOWN_STRING }
    } else {
        null
    }
}

private fun android.database.Cursor.longOrZero(columnName: String): Long {
    val columnIndex = getColumnIndex(columnName)
    return if (columnIndex >= 0 && !isNull(columnIndex)) {
        getLong(columnIndex).coerceAtLeast(0L)
    } else {
        0L
    }
}

internal fun PlaybackQueueItem.toMediaItem(): MediaItem {
    val extras = Bundle().apply {
        trackId?.let { putLong(EXTRA_TRACK_ID, it) }
        putString(EXTRA_CONTENT_URI, contentUri)
        putLong(EXTRA_DURATION, durationMs)
        putLong(EXTRA_DATE_MODIFIED, dateModifiedEpochSeconds)
        putLong(EXTRA_FILE_SIZE, fileSizeBytes)
        putDouble(EXTRA_SOURCE_ORDER, sourceOrder)
        putInt(EXTRA_PLAYBACK_MODE, playbackMode.ordinal)
    }
    return MediaItem.Builder()
        .setMediaId(mediaId)
        .setUri(contentUri.toUri())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setExtras(extras)
                .build(),
        )
        .build()
}

internal fun MediaItem.toPlaybackQueueItem(): PlaybackQueueItem {
    val extras = mediaMetadata.extras
    val uri = extras?.getString(EXTRA_CONTENT_URI)
        ?: localConfiguration?.uri?.toString()
        .orEmpty()
    return PlaybackQueueItem(
        mediaId = mediaId,
        trackId = extras?.takeIf { it.containsKey(EXTRA_TRACK_ID) }?.getLong(EXTRA_TRACK_ID),
        contentUri = uri,
        title = mediaMetadata.title?.toString().orEmpty(),
        artist = mediaMetadata.artist?.toString(),
        album = mediaMetadata.albumTitle?.toString(),
        durationMs = extras?.getLong(EXTRA_DURATION) ?: 0L,
        dateModifiedEpochSeconds = extras?.getLong(EXTRA_DATE_MODIFIED) ?: 0L,
        fileSizeBytes = extras?.getLong(EXTRA_FILE_SIZE) ?: 0L,
        sourceOrder = extras?.getDouble(EXTRA_SOURCE_ORDER) ?: 0.0,
        playbackMode = PlaybackMode.entries.getOrElse(
            extras?.getInt(EXTRA_PLAYBACK_MODE) ?: PlaybackMode.ORDER.ordinal,
        ) {
            PlaybackMode.ORDER
        },
    )
}
