package com.melox.player.ui.viewmodel

import com.melox.player.data.library.AlbumGroup
import com.melox.player.data.library.AlbumSortConfig
import com.melox.player.data.library.AlbumSortField
import com.melox.player.data.library.ArtistGroup
import com.melox.player.data.library.ArtistSortConfig
import com.melox.player.data.library.ArtistSortField
import com.melox.player.data.library.FolderGroup
import com.melox.player.data.library.FolderSortConfig
import com.melox.player.data.library.FolderSortField
import com.melox.player.data.library.MusicSortConfig
import com.melox.player.data.library.MusicSortField
import com.melox.player.data.library.albumSectionKey
import com.melox.player.data.library.artistSectionKey
import com.melox.player.data.library.buildAlbumGroups
import com.melox.player.data.library.buildArtistGroups
import com.melox.player.data.library.buildFolderGroups
import com.melox.player.data.library.createMusicSortKeys
import com.melox.player.data.library.filterAlbums
import com.melox.player.data.library.filterArtists
import com.melox.player.data.library.filterFolders
import com.melox.player.data.library.filterMusicTracks
import com.melox.player.data.library.folderSectionKey
import com.melox.player.data.library.isMusicFolderHidden
import com.melox.player.data.library.normalizedHiddenFolderPaths
import com.melox.player.data.library.sortAlbums
import com.melox.player.data.library.sortArtists
import com.melox.player.data.library.sortFolders
import com.melox.player.data.library.sortMusicTracks
import com.melox.player.data.library.visibleMusicTracks
import com.melox.player.model.MusicTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

data class MusicPresentationState(
    val items: List<MusicTrack> = emptyList(),
    val queueItems: List<MusicTrack> = emptyList(),
    val sectionIndexMap: Map<String, Int> = emptyMap(),
    val query: String = "",
    val sortConfig: MusicSortConfig = MusicSortConfig(),
)

data class AlbumPresentationState(
    val items: List<AlbumGroup> = emptyList(),
    val sectionIndexMap: Map<String, Int> = emptyMap(),
    val query: String = "",
    val sortConfig: AlbumSortConfig = AlbumSortConfig(),
)

data class ArtistPresentationState(
    val items: List<ArtistGroup> = emptyList(),
    val sectionIndexMap: Map<String, Int> = emptyMap(),
    val query: String = "",
    val sortConfig: ArtistSortConfig = ArtistSortConfig(),
)

data class FolderPresentationState(
    val items: List<FolderGroup> = emptyList(),
    val sectionIndexMap: Map<String, Int> = emptyMap(),
    val query: String = "",
    val sortConfig: FolderSortConfig = FolderSortConfig(),
)

internal data class MusicPresentationRequest(
    val query: String = "",
    val sortConfig: MusicSortConfig = MusicSortConfig(),
)

internal data class AlbumPresentationRequest(
    val query: String = "",
    val sortConfig: AlbumSortConfig = AlbumSortConfig(),
)

internal data class ArtistPresentationRequest(
    val query: String = "",
    val sortConfig: ArtistSortConfig = ArtistSortConfig(),
)

internal data class FolderPresentationRequest(
    val query: String = "",
    val sortConfig: FolderSortConfig = FolderSortConfig(),
)

internal data class LibraryProjection(
    val tracks: List<MusicTrack> = emptyList(),
    val albums: List<AlbumGroup> = emptyList(),
    val artists: List<ArtistGroup> = emptyList(),
    val folders: List<FolderGroup> = emptyList(),
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal fun visibleLibraryProjections(
    tracks: Flow<List<MusicTrack>>,
    hiddenPaths: Flow<List<String>>,
    hiddenTrackUris: Flow<Set<String>> = kotlinx.coroutines.flow.flowOf(emptySet()),
): Flow<LibraryProjection> = combine(tracks, hiddenPaths, hiddenTrackUris) { allTracks, paths, uris ->
    Triple(allTracks, paths, uris)
}.mapLatest { (allTracks, paths, uris) ->
    withContext(Dispatchers.Default) {
        val visibleTracks = visibleMusicTracks(allTracks, paths, uris)
        currentCoroutineContext().ensureActive()
        val albums = buildAlbumGroups(visibleTracks)
        currentCoroutineContext().ensureActive()
        val artists = buildArtistGroups(visibleTracks)
        currentCoroutineContext().ensureActive()
        val folders = buildFolderGroups(visibleTracks)
        currentCoroutineContext().ensureActive()
        LibraryProjection(visibleTracks, albums, artists, folders)
    }
}

internal fun FolderPresentationState.withHiddenFolders(paths: List<String>): FolderPresentationState {
    if (paths.isEmpty()) return this
    val prefixes = normalizedHiddenFolderPaths(paths)
    val visibleFolders = items.filterNot { isMusicFolderHidden(it.path, prefixes) }
    if (visibleFolders.size == items.size) return this
    val indices = if (query.isBlank() && sortConfig.field == FolderSortField.NAME) {
        buildMap {
            visibleFolders.forEachIndexed { index, folder ->
                putIfAbsent(folderSectionKey(folder), index)
            }
        }
    } else emptyMap()
    return copy(items = visibleFolders, sectionIndexMap = indices)
}

internal fun musicPresentations(
    tracks: Flow<List<MusicTrack>>,
    requests: Flow<MusicPresentationRequest>,
): Flow<MusicPresentationState> = channelFlow {
    var previousTracks: List<MusicTrack>? = null
    var previousSortConfig: MusicSortConfig? = null
    var queueItems: List<MusicTrack> = emptyList()
    var sectionIndexMap: Map<String, Int> = emptyMap()
    combine(tracks, requests) { library, request -> library to request }
        .collectLatest { (library, request) ->
            withContext(Dispatchers.Default) {
                if (previousTracks !== library || previousSortConfig != request.sortConfig) {
                    val sorted = if (request.sortConfig == MusicSortConfig()) {
                        library
                    } else {
                        sortMusicTracks(library, request.sortConfig)
                    }
                    currentCoroutineContext().ensureActive()
                    val indices = if (request.sortConfig.field in setOf(
                            MusicSortField.TITLE, MusicSortField.ARTIST, MusicSortField.FILE_NAME,
                        )
                    ) {
                        buildMap {
                            sorted.forEachIndexed { index, track ->
                                currentCoroutineContext().ensureActive()
                                val key = when (request.sortConfig.field) {
                                    MusicSortField.ARTIST -> createMusicSortKeys(track.artist).section
                                    MusicSortField.FILE_NAME -> createMusicSortKeys(track.fileName).section
                                    else -> track.titleSectionKey
                                }
                                putIfAbsent(key, index)
                            }
                        }
                    } else emptyMap()
                    previousTracks = library
                    previousSortConfig = request.sortConfig
                    queueItems = sorted
                    sectionIndexMap = indices
                }
                val items = filterMusicTracks(queueItems, request.query)
                currentCoroutineContext().ensureActive()
                send(MusicPresentationState(
                    items = items,
                    queueItems = queueItems,
                    sectionIndexMap = if (request.query.isBlank()) sectionIndexMap else emptyMap(),
                    query = request.query,
                    sortConfig = request.sortConfig,
                ))
            }
        }
}

internal fun albumPresentations(
    library: Flow<LibraryProjection>,
    requests: Flow<AlbumPresentationRequest>,
): Flow<AlbumPresentationState> = combine(library, requests) { projection, request ->
    val items = sortAlbums(
        filterAlbums(projection.albums, request.query),
        request.sortConfig,
    )
    val columns = request.sortConfig.gridStyle.columns
    val sectionIndexMap = if (
        request.query.isBlank() &&
        (
            request.sortConfig.field == AlbumSortField.ALBUM ||
                request.sortConfig.field == AlbumSortField.ALBUM_ARTIST
        )
    ) {
        buildMap {
            items.forEachIndexed { index, album ->
                val key = albumSectionKey(album, request.sortConfig.field)
                putIfAbsent(key, index - index % columns)
            }
        }
    } else {
        emptyMap()
    }
    AlbumPresentationState(
        items = items,
        sectionIndexMap = sectionIndexMap,
        query = request.query,
        sortConfig = request.sortConfig,
    )
}

internal fun artistPresentations(
    library: Flow<LibraryProjection>,
    requests: Flow<ArtistPresentationRequest>,
): Flow<ArtistPresentationState> = combine(library, requests) { projection, request ->
    val items = sortArtists(
        filterArtists(projection.artists, request.query),
        request.sortConfig,
    )
    val sectionIndexMap = if (
        request.query.isBlank() &&
        request.sortConfig.field == ArtistSortField.NAME
    ) {
        buildMap {
            items.forEachIndexed { index, artist ->
                putIfAbsent(artistSectionKey(artist), index)
            }
        }
    } else {
        emptyMap()
    }
    ArtistPresentationState(
        items = items,
        sectionIndexMap = sectionIndexMap,
        query = request.query,
        sortConfig = request.sortConfig,
    )
}

internal fun folderPresentations(
    library: Flow<LibraryProjection>,
    requests: Flow<FolderPresentationRequest>,
): Flow<FolderPresentationState> = combine(library, requests) { projection, request ->
    val items = sortFolders(
        filterFolders(projection.folders, request.query),
        request.sortConfig,
    )
    val sectionIndexMap = if (
        request.query.isBlank() &&
        request.sortConfig.field == FolderSortField.NAME
    ) {
        buildMap {
            items.forEachIndexed { index, folder ->
                putIfAbsent(folderSectionKey(folder), index)
            }
        }
    } else {
        emptyMap()
    }
    FolderPresentationState(
        items = items,
        sectionIndexMap = sectionIndexMap,
        query = request.query,
        sortConfig = request.sortConfig,
    )
}
