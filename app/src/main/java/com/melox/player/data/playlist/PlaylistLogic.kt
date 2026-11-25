package com.melox.player.data.playlist

import com.melox.player.data.library.MusicSortConfig
import com.melox.player.data.library.MusicSortField
import com.melox.player.data.library.albumGroupKey
import com.melox.player.data.library.musicTrackComparator
import com.melox.player.model.LocalPlaylist
import com.melox.player.model.MusicTrack
import com.melox.player.model.PlaylistTrackEntry
import com.melox.player.model.ResolvedPlaylistTrack
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

enum class PlaylistSortField {
    CUSTOM,
    TITLE,
    DATE_ADDED,
    FILE_NAME,
    FILE_SIZE,
    DURATION,
}

data class PlaylistSortConfig(
    val field: PlaylistSortField = PlaylistSortField.CUSTOM,
    val descending: Boolean = false,
)

internal suspend fun <Artwork : Any> loadPlaylistArtwork(
    playlist: LocalPlaylist,
    loadArtwork: suspend (MusicTrack) -> Artwork?,
    sameArtwork: (Artwork, Artwork) -> Boolean,
    loadCustomArtwork: suspend () -> Artwork? = { null },
): List<Artwork> {
    val tracks = playlist.entries.map(PlaylistTrackEntry::trackSnapshot)
    loadCustomArtwork()?.let {
        currentCoroutineContext().ensureActive()
        return listOf(it)
    }
    val selected = ArrayList<Artwork>(3)
    val representedAlbums = HashSet<String>()
    for (track in tracks) {
        currentCoroutineContext().ensureActive()
        val albumKey = track.album?.takeIf(String::isNotBlank)?.let {
            albumGroupKey(it, track.albumArtist)
        }
        if (albumKey != null && albumKey in representedAlbums) continue
        val artwork = loadArtwork(track) ?: continue
        currentCoroutineContext().ensureActive()
        if (selected.any { sameArtwork(it, artwork) }) continue
        selected += artwork
        if (albumKey != null) representedAlbums += albumKey
        if (selected.size == 3) break
    }
    currentCoroutineContext().ensureActive()
    return selected
}

internal fun resolvePlaylistTracks(
    playlist: LocalPlaylist,
    libraryTracks: List<MusicTrack>,
    readableContentUris: Set<String>,
): List<ResolvedPlaylistTrack> {
    val libraryByContentUri = libraryTracks.associateBy(MusicTrack::contentUri)
    return playlist.entries.map { entry ->
        val currentTrack = libraryByContentUri[entry.trackSnapshot.contentUri]
        ResolvedPlaylistTrack(
            entry = entry,
            track = currentTrack ?: entry.trackSnapshot,
            available = currentTrack != null && currentTrack.contentUri in readableContentUris,
        )
    }
}

internal fun sortPlaylistTracks(
    tracks: List<ResolvedPlaylistTrack>,
    config: PlaylistSortConfig,
): List<ResolvedPlaylistTrack> {
    if (config.field == PlaylistSortField.CUSTOM) {
        return if (config.descending) tracks.asReversed() else tracks
    }
    val musicConfig = MusicSortConfig(
        field = when (config.field) {
            PlaylistSortField.TITLE -> MusicSortField.TITLE
            PlaylistSortField.DATE_ADDED -> MusicSortField.DATE_ADDED
            PlaylistSortField.FILE_NAME -> MusicSortField.FILE_NAME
            PlaylistSortField.FILE_SIZE -> MusicSortField.FILE_SIZE
            PlaylistSortField.DURATION -> MusicSortField.DURATION
            PlaylistSortField.CUSTOM -> error("Custom order has no music comparator")
        },
        descending = config.descending,
    )
    val musicComparator = musicTrackComparator(musicConfig)
    return tracks.sortedWith { first, second ->
        val compared = musicComparator.compare(first.track, second.track)
        if (compared != 0) compared else first.entry.id.compareTo(second.entry.id)
    }
}

internal fun resolvePlaylistPlaybackSelection(
    displayedTracks: List<ResolvedPlaylistTrack>,
    selectedEntryId: String,
): Pair<List<MusicTrack>, Int>? {
    val availableTracks = displayedTracks.filter(ResolvedPlaylistTrack::available)
    val selectedIndex = availableTracks.indexOfFirst { it.entry.id == selectedEntryId }
    if (selectedIndex < 0) return null
    return availableTracks.map(ResolvedPlaylistTrack::track) to selectedIndex
}

internal fun addTracksToPlaylist(
    playlist: LocalPlaylist,
    tracks: List<MusicTrack>,
    nowEpochMillis: Long,
    newEntryId: () -> String,
): LocalPlaylist {
    val existingContentUris = playlist.entries
        .mapTo(HashSet()) { entry -> entry.trackSnapshot.contentUri }
    val addedEntries = tracks.mapNotNull { track ->
        if (!existingContentUris.add(track.contentUri)) return@mapNotNull null
        PlaylistTrackEntry(
            id = newEntryId(),
            addedAtEpochMillis = nowEpochMillis,
            trackSnapshot = track,
        )
    }
    if (addedEntries.isEmpty()) return playlist
    return playlist.copy(
        updatedAtEpochMillis = nowEpochMillis,
        entries = addedEntries + playlist.entries,
    )
}

internal fun removePlaylistEntries(
    playlist: LocalPlaylist,
    entryIds: Set<String>,
    nowEpochMillis: Long,
): LocalPlaylist {
    if (entryIds.isEmpty()) return playlist
    val remainingEntries = playlist.entries.filterNot { entry -> entry.id in entryIds }
    if (remainingEntries.size == playlist.entries.size) return playlist
    return playlist.copy(
        updatedAtEpochMillis = nowEpochMillis,
        entries = remainingEntries,
    )
}

internal fun canonicalPlaylistEntryOrder(
    displayedEntryIds: List<String>,
    descending: Boolean,
): List<String> = if (descending) displayedEntryIds.asReversed() else displayedEntryIds

internal fun reorderPlaylists(
    playlists: List<LocalPlaylist>,
    orderedPlaylistIds: List<String>,
): List<LocalPlaylist>? {
    if (orderedPlaylistIds.size != playlists.size) return null
    if (orderedPlaylistIds.toSet().size != orderedPlaylistIds.size) return null
    val playlistsById = playlists.associateBy(LocalPlaylist::id)
    if (playlistsById.size != playlists.size) return null
    if (orderedPlaylistIds.toSet() != playlistsById.keys) return null
    if (orderedPlaylistIds == playlists.map(LocalPlaylist::id)) return playlists
    return orderedPlaylistIds.map(playlistsById::getValue)
}

internal fun reorderPlaylistEntries(
    playlist: LocalPlaylist,
    orderedEntryIds: List<String>,
    nowEpochMillis: Long,
): LocalPlaylist? {
    if (orderedEntryIds.size != playlist.entries.size) return null
    if (orderedEntryIds.toSet().size != orderedEntryIds.size) return null
    val entriesById = playlist.entries.associateBy(PlaylistTrackEntry::id)
    if (entriesById.size != playlist.entries.size) return null
    if (orderedEntryIds.toSet() != entriesById.keys) return null
    if (orderedEntryIds == playlist.entries.map(PlaylistTrackEntry::id)) return playlist
    return playlist.copy(
        updatedAtEpochMillis = nowEpochMillis,
        entries = orderedEntryIds.map(entriesById::getValue),
    )
}
