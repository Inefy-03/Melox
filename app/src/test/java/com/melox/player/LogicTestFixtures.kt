package com.melox.player

import com.melox.player.data.library.createMusicSortKeys
import com.melox.player.model.MusicTrack
import com.melox.player.model.PlaybackQueueItem
import java.io.DataOutputStream

internal fun DataOutputStream.writeNullableString(value: String?) {
    writeBoolean(value != null)
    if (value != null) writeSizedString(value)
}

internal fun DataOutputStream.writeSizedString(value: String) {
    val bytes = value.toByteArray(Charsets.UTF_8)
    writeInt(bytes.size)
    write(bytes)
}

internal fun musicTrack(
    id: Long,
    title: String,
    dateAddedEpochSeconds: Long,
    fileName: String,
    fileSizeBytes: Long,
    durationMs: Long,
    dateModifiedEpochSeconds: Long = 0L,
    artist: String? = null,
): MusicTrack {
    val sortKeys = createMusicSortKeys(title)
    return MusicTrack(
        id = id,
        title = title,
        artist = artist,
        album = null,
        durationMs = durationMs,
        dateAddedEpochSeconds = dateAddedEpochSeconds,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileName = fileName,
        fileSizeBytes = fileSizeBytes,
        contentUri = "content://music/$id",
        titleSectionKey = sortKeys.section,
        titleSortKey = sortKeys.value,
    )
}

internal fun playbackQueueItem(mediaId: String): PlaybackQueueItem = PlaybackQueueItem(
    mediaId = mediaId,
    trackId = null,
    contentUri = "content://music/$mediaId",
    title = mediaId,
    artist = null,
    album = null,
    durationMs = 1L,
    dateModifiedEpochSeconds = 1L,
    fileSizeBytes = 1L,
)
