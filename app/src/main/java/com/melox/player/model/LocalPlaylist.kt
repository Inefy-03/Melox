package com.melox.player.model

/** A user-owned local playlist whose [entries] order is the persisted custom order. */
data class LocalPlaylist(
    val id: String,
    val name: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val entries: List<PlaylistTrackEntry> = emptyList(),
    // The image is persisted separately. This revision invalidates active artwork requests.
    internal val customCoverRevision: Long = 0,
)
/** One stable playlist slot with enough metadata to remain visible when its source disappears. */
data class PlaylistTrackEntry(
    val id: String,
    val addedAtEpochMillis: Long,
    val trackSnapshot: MusicTrack,
)

/** A playlist slot resolved against the current library and a read-access check. */
data class ResolvedPlaylistTrack(
    val entry: PlaylistTrackEntry,
    val track: MusicTrack,
    val available: Boolean,
)
