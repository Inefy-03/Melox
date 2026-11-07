package com.melox.player.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.melox.player.model.CoverShape
import com.melox.player.model.LyricsTranslationLanguage
import com.melox.player.model.LyricAnimationMode
import com.melox.player.data.library.AlbumGroup
import com.melox.player.data.library.AlbumSortConfig
import com.melox.player.data.library.AlbumSortField
import com.melox.player.data.library.ArtistGroup
import com.melox.player.data.library.ArtistSortConfig
import com.melox.player.data.library.ArtistSortField
import com.melox.player.data.library.FolderGroup
import com.melox.player.data.library.InternalStorageHierarchy
import com.melox.player.data.library.buildInternalStorageHierarchy
import com.melox.player.data.library.FolderSortConfig
import com.melox.player.data.library.FolderSortField
import com.melox.player.data.library.MusicSortConfig
import com.melox.player.data.library.MusicSortField
import com.melox.player.data.library.albumSectionKey
import com.melox.player.data.library.buildAlbumGroups
import com.melox.player.data.library.buildArtistGroups
import com.melox.player.data.library.buildFolderGroups
import com.melox.player.data.library.createMusicSortKeys
import com.melox.player.data.library.filterAlbums
import com.melox.player.data.library.filterArtists
import com.melox.player.data.library.filterFolders
import com.melox.player.data.library.filterMusicTracks
import com.melox.player.data.library.artistSectionKey
import com.melox.player.data.library.folderSectionKey
import com.melox.player.data.library.sortAlbums
import com.melox.player.data.library.sortArtists
import com.melox.player.data.library.sortFolders
import com.melox.player.data.library.sortMusicTracks
import com.melox.player.data.library.hiddenFolderDisplayPaths
import com.melox.player.data.library.isMusicFolderHidden
import com.melox.player.data.library.normalizeHiddenFolderPath
import com.melox.player.data.library.normalizedHiddenFolderPaths
import com.melox.player.data.library.visibleMusicTracks
import com.melox.player.data.repository.MusicRepository
import com.melox.player.data.repository.CustomBackgroundRepository
import com.melox.player.data.repository.LyricsRepository
import com.melox.player.data.repository.LyricsRequest
import com.melox.player.data.repository.LyricsReadException
import com.melox.player.data.repository.PlaylistRepository
import com.melox.player.data.repository.SettingsRepository
import com.melox.player.data.library.MusicLibraryUpdates
import com.melox.player.data.library.MusicLibrarySaveQueue
import com.melox.player.data.playlist.addTracksToPlaylist
import com.melox.player.data.playlist.PlaylistSortConfig
import com.melox.player.data.playlist.PlaylistSaveQueue
import com.melox.player.data.playlist.removePlaylistEntries
import com.melox.player.data.playlist.reorderPlaylistEntries
import com.melox.player.data.playlist.reorderPlaylists
import com.melox.player.model.AppSettings
import com.melox.player.model.TransientAudioFocusBehavior
import com.melox.player.model.BottomBarStyle
import com.melox.player.model.DefaultHomePage
import com.melox.player.model.DynamicColorSource
import com.melox.player.model.LocalPlaylist
import com.melox.player.model.LyricsDocument
import com.melox.player.model.LyricsFormatPriority
import com.melox.player.model.LyricsSourcePriority
import com.melox.player.model.MusicTrack
import com.melox.player.model.PlaybackMode
import com.melox.player.model.NavigationTransitionStyle
import com.melox.player.model.LyricsUiState
import com.melox.player.model.PlaybackUiState
import com.melox.player.model.PlaybackBackgroundStyle
import com.melox.player.model.DynamicFlowStyle
import com.melox.player.model.ScanStatus
import com.melox.player.model.ThemeMode
import com.melox.player.model.withTrackMetadata
import com.melox.player.model.PlaybackMetadataProjection
import com.melox.player.playback.PlaybackController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import java.io.IOException

/** Immutable screen state assembled from persisted settings and the current scan session. */
data class AppUiState(
    val settings: AppSettings,
    val settingsLoaded: Boolean,
    val tracks: List<MusicTrack>,
    val albums: List<AlbumGroup>,
    val artists: List<ArtistGroup>,
    val folders: List<FolderGroup>,
    val scanStatus: ScanStatus,
    val scanGeneration: Long,
)

data class MusicPresentationState(
    val items: List<MusicTrack> = emptyList(),
    val queueItems: List<MusicTrack> = emptyList(),
    val sectionIndexMap: Map<String, Int> = emptyMap(),
    val query: String = "",
    val sortConfig: MusicSortConfig = MusicSortConfig(),
)

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

internal fun LyricsRequest.hasSameLyricsContentTarget(other: LyricsRequest): Boolean =
    mediaId == other.mediaId && contentUri == other.contentUri

internal fun shouldShowLyricsLoading(
    previousRequest: LyricsRequest?,
    request: LyricsRequest,
): Boolean = previousRequest?.hasSameLyricsContentTarget(request) != true

internal fun shouldPublishLyricsResolution(
    previousRequest: LyricsRequest?,
    request: LyricsRequest,
    previousDocument: LyricsDocument?,
    document: LyricsDocument?,
): Boolean = shouldShowLyricsLoading(previousRequest, request) || previousDocument != document

internal fun Flow<LyricsRequest?>.resolveLyricsStates(
    loadDocument: suspend (LyricsRequest) -> LyricsDocument?,
): Flow<LyricsUiState> = channelFlow {
    var previousRequest: LyricsRequest? = null
    var previousDocument: LyricsDocument? = null
    collectLatest { request ->
        if (request == null) {
            send(LyricsUiState.Unavailable)
            previousRequest = null
            previousDocument = null
            return@collectLatest
        }

        val showLoading = shouldShowLyricsLoading(previousRequest, request)
        if (showLoading) send(LyricsUiState.Loading)

        val document = try {
            loadDocument(request)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            val previousSourceConfirmedAbsent = exception is LyricsReadException &&
                previousDocument?.source?.let { it !in exception.failedSources } == true
            if (showLoading || previousSourceConfirmedAbsent) {
                send(LyricsUiState.Unavailable)
                previousDocument = null
            }
            previousRequest = request
            return@collectLatest
        }
        currentCoroutineContext().ensureActive()
        if (
            shouldPublishLyricsResolution(
                previousRequest = previousRequest,
                request = request,
                previousDocument = previousDocument,
                document = document,
            )
        ) {
            send(document?.let { LyricsUiState.Available(it, request.mediaId) }
                ?: LyricsUiState.Unavailable)
        }
        previousRequest = request
        previousDocument = document
    }
}

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

data class PlaylistUiState(
    val playlists: List<LocalPlaylist> = emptyList(),
    val readableContentUris: Set<String> = emptySet(),
    val loaded: Boolean = false,
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

private data class LoadedSettings(
    val value: AppSettings = AppSettings(),
    val loaded: Boolean = false,
)

internal data class MusicPresentationRequest(
    val query: String = "",
    val sortConfig: MusicSortConfig = MusicSortConfig(),
)

private data class AlbumPresentationRequest(
    val query: String = "",
    val sortConfig: AlbumSortConfig = AlbumSortConfig(),
)

private data class ArtistPresentationRequest(
    val query: String = "",
    val sortConfig: ArtistSortConfig = ArtistSortConfig(),
)

private data class FolderPresentationRequest(
    val query: String = "",
    val sortConfig: FolderSortConfig = FolderSortConfig(),
)

/** Coordinates appearance persistence, runtime-permission state, and local music scans. */
class MeloxViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    // Resolve persisted chrome before Compose can draw an intermediate normal bottom bar.
    private val initialSettings = runBlocking(Dispatchers.IO) {
        settingsRepository.loadSettings()
    }
    // Decode alongside feature initialization, before the first page composition.
    val customBackground = CustomBackgroundRepository(application)
        .observeImages(settingsRepository.settings.onStart { emit(initialSettings) })
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val musicRepository = MusicRepository(application)
    private val playlistRepository = PlaylistRepository(application)
    private val lyricsRepository = LyricsRepository(application)
    private val playbackController = PlaybackController(application)
    private val mutableSleepTimerSelectionSeconds = MutableStateFlow(initialSettings.sleepTimerSeconds)
    val sleepTimerSelectionSeconds: StateFlow<Int> = mutableSleepTimerSelectionSeconds
    private var scanJob: Job? = null
    private val hasInitialAudioPermission = hasAudioPermission()
    private val loadedSettings = settingsRepository.settings
        .map { settings -> LoadedSettings(value = settings, loaded = true) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = LoadedSettings(
                value = initialSettings,
                loaded = true,
            ),
        )

    private val allLibraryTracks = MutableStateFlow<List<MusicTrack>>(emptyList())
    private val mutableHiddenFolderPaths = MutableStateFlow(initialSettings.blockedFolderPaths)
    val hiddenFolderPaths: StateFlow<List<String>> = mutableHiddenFolderPaths.asStateFlow()
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val hiddenFolderAddresses: StateFlow<Map<String, String>> = combine(allLibraryTracks, hiddenFolderPaths) { tracks, paths ->
        tracks to paths
    }.mapLatest { (tracks, paths) ->
        withContext(Dispatchers.Default) { hiddenFolderDisplayPaths(tracks, paths) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    private val hiddenFolderSaveMutex = Mutex()
    private var persistedHiddenFolderPaths = initialSettings.blockedFolderPaths
    private val mutableHiddenFolderSaveFailures = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val hiddenFolderSaveFailures = mutableHiddenFolderSaveFailures.asSharedFlow()
    private val mutableHiddenTrackUris = MutableStateFlow(initialSettings.hiddenTrackUris)
    val hiddenTrackUris: StateFlow<Set<String>> = mutableHiddenTrackUris.asStateFlow()
    val hiddenTracks: StateFlow<List<MusicTrack>> = combine(allLibraryTracks, hiddenTrackUris) { tracks, uris ->
        tracks.filter { it.contentUri in uris }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val hiddenTrackSaveMutex = Mutex()
    private var persistedHiddenTrackUris = initialSettings.hiddenTrackUris
    private val mutableHiddenTrackSaveFailures = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val hiddenTrackSaveFailures = mutableHiddenTrackSaveFailures.asSharedFlow()
    private val mutableLibraryCacheSaveFailures = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val libraryCacheSaveFailures = mutableLibraryCacheSaveFailures.asSharedFlow()
    private val libraryUpdates = MusicLibraryUpdates()
    private val librarySaveQueue = MusicLibrarySaveQueue(
        scope = viewModelScope,
        save = { tracks, invalidate -> musicRepository.cacheMusic(tracks, invalidate) },
        clearCache = musicRepository::clearCachedMusic,
        onFailure = { mutableLibraryCacheSaveFailures.tryEmit(Unit) },
    )
    private val library = visibleLibraryProjections(allLibraryTracks, hiddenFolderPaths, hiddenTrackUris)
        .stateIn(viewModelScope, SharingStarted.Eagerly, LibraryProjection())
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    internal val internalStorageHierarchy: StateFlow<InternalStorageHierarchy> = library
        .map { it.tracks }.distinctUntilChanged()
        .mapLatest { tracks -> withContext(Dispatchers.Default) { buildInternalStorageHierarchy(tracks) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, buildInternalStorageHierarchy(emptyList()))
    private val scanStatus = MutableStateFlow<ScanStatus>(
        if (hasInitialAudioPermission) ScanStatus.Scanning else ScanStatus.PermissionRequired,
    )
    private val scanGeneration = MutableStateFlow(0L)
    private val mutableScanCompletionEvents = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val scanCompletionEvents: SharedFlow<Int> = mutableScanCompletionEvents.asSharedFlow()
    private val mutableScanNoChangesEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val scanNoChangesEvents: SharedFlow<Unit> = mutableScanNoChangesEvents.asSharedFlow()
    private val playlists = MutableStateFlow<List<LocalPlaylist>>(emptyList())
    private val playlistReadableContentUris = MutableStateFlow<Set<String>>(emptySet())
    private val playlistsLoaded = MutableStateFlow(false)
    private val mutablePlaylistSaveFailures = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val playlistSaveFailures = mutablePlaylistSaveFailures.asSharedFlow()
    private val playlistSaveQueue = PlaylistSaveQueue(
        scope = viewModelScope,
        save = playlistRepository::save,
        onFailure = { mutablePlaylistSaveFailures.tryEmit(Unit) },
    )
    private val lyricsRefreshRevision = MutableStateFlow(0L)
    val playlistState: StateFlow<PlaylistUiState> = combine(
        playlists,
        playlistReadableContentUris,
        playlistsLoaded,
    ) { playlists, readableContentUris, loaded ->
        PlaylistUiState(
            playlists = playlists,
            readableContentUris = readableContentUris,
            loaded = loaded,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PlaylistUiState(),
    )
    val playbackState: StateFlow<PlaybackUiState> = playbackController.state
    val sleepTimerState = playbackController.sleepTimerState
    val autoExtendSleepTimer = playbackController.autoExtendSleepTimer
    val playbackPauseFade = playbackController.playbackPauseFade
    val currentTrackId: StateFlow<Long?> = playbackState
        .map { state -> state.currentItem?.trackId }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = playbackState.value.currentItem?.trackId,
        )
    val hasCurrentItem: StateFlow<Boolean> = playbackState
        .map { state -> state.currentItem != null }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = playbackState.value.currentItem != null,
        )
    private val compactControllerPlaybackState = playbackState
        .map { state ->
            state.copy(
                positionMs = 0L,
                positionUpdateElapsedRealtimeMs = 0L,
                bufferedPositionMs = 0L,
            )
        }
        .distinctUntilChanged()
    private val playbackMetadataProjection = PlaybackMetadataProjection()
    val compactPlaybackState: StateFlow<PlaybackUiState> = combine(
        compactControllerPlaybackState,
        allLibraryTracks,
    ) { playback, tracks ->
        playbackMetadataProjection.project(playback, tracks)
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = playbackState.value.copy(
                positionMs = 0L,
                positionUpdateElapsedRealtimeMs = 0L,
                bufferedPositionMs = 0L,
            ).withTrackMetadata(allLibraryTracks.value),
        )
    private val lyricPlaybackTarget = playbackState
        .map { it.currentItem to it.durationMs }
        .distinctUntilChanged()
    private val lyricsRequests = combine(
        lyricPlaybackTarget,
        allLibraryTracks,
        lyricsRefreshRevision,
        loadedSettings,
    ) { target, tracks, refreshRevision, loadedSettings ->
        val item = target.first ?: return@combine null
        val track = item.trackId?.let { trackId ->
            tracks.firstOrNull { it.id == trackId }
        }
        LyricsRequest(
            mediaId = item.mediaId,
            contentUri = item.contentUri,
            fileName = track?.fileName,
            folderPath = track?.folderPath,
            durationMs = target.second,
            refreshRevision = refreshRevision,
            sourcePriority = loadedSettings.value.lyricsSourcePriority,
            formatPriority = loadedSettings.value.lyricsFormatPriority,
        )
    }
        .distinctUntilChanged()

    val lyricsState: StateFlow<LyricsUiState> = lyricsRequests
        .resolveLyricsStates(lyricsRepository::load)
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = LyricsUiState.Unavailable,
        )
    private val musicPresentationRequest = MutableStateFlow(MusicPresentationRequest())
    private val albumPresentationRequest = MutableStateFlow(AlbumPresentationRequest())
    private val artistPresentationRequest = MutableStateFlow(ArtistPresentationRequest())
    private val folderPresentationRequest = MutableStateFlow(FolderPresentationRequest())

    val uiState: StateFlow<AppUiState> = combine(
        loadedSettings,
        library,
        scanStatus,
        scanGeneration,
    ) { loadedSettings, library, scanStatus, scanGeneration ->
        AppUiState(
            settings = loadedSettings.value,
            settingsLoaded = loadedSettings.loaded,
            tracks = library.tracks,
            albums = library.albums,
            artists = library.artists,
            folders = library.folders,
            scanStatus = if (scanStatus is ScanStatus.Success) {
                ScanStatus.Success(library.tracks.size)
            } else scanStatus,
            scanGeneration = scanGeneration,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppUiState(
            settings = loadedSettings.value.value,
            settingsLoaded = loadedSettings.value.loaded,
            tracks = emptyList(),
            albums = emptyList(),
            artists = emptyList(),
            folders = emptyList(),
            scanStatus = scanStatus.value,
            scanGeneration = scanGeneration.value,
        ),
    )

    val musicPresentation: StateFlow<MusicPresentationState> = musicPresentations(
        tracks = library.map { it.tracks }.distinctUntilChanged(),
        requests = musicPresentationRequest,
    )
        .stateIn(viewModelScope, SharingStarted.Eagerly, MusicPresentationState())

    val albumPresentation: StateFlow<AlbumPresentationState> = combine(
        library,
        albumPresentationRequest,
    ) { projection, request ->
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
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, AlbumPresentationState())

    val artistPresentation: StateFlow<ArtistPresentationState> = combine(
        library,
        artistPresentationRequest,
    ) { projection, request ->
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
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, ArtistPresentationState())

    val folderPresentation: StateFlow<FolderPresentationState> = combine(
        library,
        folderPresentationRequest,
    ) { projection, request ->
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
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, FolderPresentationState())

    init {
        playbackController.setHighPrecisionOutput(initialSettings.highPrecisionOutput)
        playbackController.setPlaybackSpeed(initialSettings.playbackSpeed)
        playbackController.setAutoExtendSleepTimer(initialSettings.autoExtendSleepTimer)
        playbackController.setPlaybackPauseFade(initialSettings.playbackPauseFade)
        viewModelScope.launch {
            playlists.value = playlistRepository.load()
            playlistsLoaded.value = true
        }
        viewModelScope.launch {
            combine(
                playlists,
                allLibraryTracks.map { tracks ->
                    tracks.mapTo(HashSet(), MusicTrack::contentUri)
                }.distinctUntilChanged(),
            ) { playlists, libraryContentUris ->
                playlists.asSequence()
                    .flatMap { playlist -> playlist.entries.asSequence() }
                    .map { entry -> entry.trackSnapshot.contentUri }
                    .filter { contentUri -> contentUri in libraryContentUris }
                    .toSet()
            }
                .distinctUntilChanged()
                .collectLatest { candidates ->
                    playlistReadableContentUris.value =
                        playlistRepository.readableContentUris(candidates)
                }
        }
        // Startup scanning never triggers a permission dialog; the Activity owns that UI flow.
        if (hasInitialAudioPermission) {
            startMusicScan(
                restoreCachedTracks = true,
                refreshAfterRestore = initialSettings.refreshLibraryOnStart,
            )
        }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(themeMode)
        }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColorEnabled(enabled)
        }
    }

    fun setDynamicColorSource(source: DynamicColorSource) {
        viewModelScope.launch {
            settingsRepository.setDynamicColorSource(source)
        }
    }

    fun setPlaybackBackgroundStyle(style: PlaybackBackgroundStyle) {
        viewModelScope.launch {
            settingsRepository.setPlaybackBackgroundStyle(style)
        }
    }

    fun setDynamicFlowStyle(style: DynamicFlowStyle) {
        viewModelScope.launch {
            settingsRepository.setDynamicFlowStyle(style)
        }
    }

    fun setShowSongListDuration(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowSongListDuration(enabled)
        }
    }

    fun setShowSongListArtwork(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowSongListArtwork(enabled)
        }
    }

    fun setCircularArtwork(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCircularArtwork(enabled)
        }
    }

    fun setCoverShape(shape: CoverShape) {
        viewModelScope.launch {
            settingsRepository.setCoverShape(shape)
        }
    }

    fun setArtworkFlipAnimation(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setArtworkFlipAnimation(enabled)
        }
    }

    fun setShowSongListFileName(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowSongListFileName(enabled)
        }
    }
    fun setLyricFontScale(scale: Float) {
        viewModelScope.launch {
            settingsRepository.setLyricFontScale(scale)
        }
    }

    fun setLyricFontWeight(weight: Int) {
        viewModelScope.launch {
            settingsRepository.setLyricFontWeight(weight)
        }
    }

    fun setLyricAnimationMode(mode: LyricAnimationMode) {
        viewModelScope.launch {
            settingsRepository.setLyricAnimationMode(mode)
        }
    }

    fun setLyricBlurEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setLyricBlurEnabled(enabled)
        }
    }

    fun setCenterLyrics(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCenterLyrics(enabled)
        }
    }

    fun setLeftAlignPlayerTitle(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setLeftAlignPlayerTitle(enabled)
        }
    }

    fun setHideControlsOnLyrics(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHideControlsOnLyrics(enabled)
        }
    }

    fun setShowMiniPlayerLyrics(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShowMiniPlayerLyrics(enabled) }
    }

    fun setShowArtworkLyrics(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShowArtworkLyrics(enabled) }
    }

    fun setShowLyricsTranslation(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowLyricsTranslation(enabled)
        }
    }

    fun setLyricsTranslationLanguage(language: LyricsTranslationLanguage) {
        viewModelScope.launch { settingsRepository.setLyricsTranslationLanguage(language) }
    }

    fun setShowLyricsRomanization(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowLyricsRomanization(enabled)
        }
    }

    fun setLyricsSourcePriority(priority: LyricsSourcePriority) {
        viewModelScope.launch {
            settingsRepository.setLyricsSourcePriority(priority)
        }
    }

    suspend fun setCustomBackground(uri: Uri): Boolean = settingsRepository.setCustomBackground(uri)

    suspend fun deleteCustomBackground(): Boolean = settingsRepository.deleteCustomBackground()

    fun setCustomBackgroundDimPercent(percent: Int) {
        viewModelScope.launch {
            settingsRepository.setCustomBackgroundDimPercent(percent)
        }
    }

    fun setCustomBackgroundBlurPercent(percent: Int) {
        viewModelScope.launch {
            settingsRepository.setCustomBackgroundBlurPercent(percent)
        }
    }

    fun setCustomBackgroundCardBlurPercent(percent: Int) {
        viewModelScope.launch {
            settingsRepository.setCustomBackgroundCardBlurPercent(percent)
        }
    }

    fun setCustomBackgroundCardOpacityPercent(percent: Int) {
        viewModelScope.launch {
            settingsRepository.setCustomBackgroundCardOpacityPercent(percent)
        }
    }

    fun setShowMusicTagEditor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowMusicTagEditor(enabled)
        }
    }

    fun setShowLyricoEditor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowLyricoEditor(enabled)
        }
    }

    fun setShowLunaBeatEditor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowLunaBeatEditor(enabled)
        }
    }

    fun setLyricsFormatPriority(priority: LyricsFormatPriority) {
        viewModelScope.launch {
            settingsRepository.setLyricsFormatPriority(priority)
        }
    }

    fun setBottomBarStyle(bottomBarStyle: BottomBarStyle) {
        viewModelScope.launch {
            settingsRepository.setBottomBarStyle(bottomBarStyle)
        }
    }

    fun setBlurEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBlurEnabled(enabled)
        }
    }

    fun setProgressiveTopBarBlurEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setProgressiveTopBarBlurEnabled(enabled)
        }
    }

    fun setSmallPlayerBar(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSmallPlayerBar(enabled)
        }
    }

    fun setHideBottomBar(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHideBottomBar(enabled)
        }
    }

    fun setImmersiveMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setImmersiveMode(enabled)
        }
    }

    fun setHideStatusBar(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHideStatusBar(enabled)
        }
    }

    fun setFloatingBottomBar(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setFloatingBottomBar(enabled)
        }
    }

    fun setNavigationRailExpanded(expanded: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNavigationRailExpanded(expanded)
        }
    }

    fun setLiquidGlass(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setLiquidGlass(enabled)
        }
    }

    fun setPredictiveBackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setPredictiveBackEnabled(enabled)
        }
    }

    fun setNavigationTransitionStyle(style: NavigationTransitionStyle) {
        viewModelScope.launch {
            settingsRepository.setNavigationTransitionStyle(style)
        }
    }

    fun setSongListPullToRefresh(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSongListPullToRefresh(enabled)
        }
    }

    fun setRefreshLibraryOnStart(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRefreshLibraryOnStart(enabled)
        }
    }

    fun setSkipShortAudio(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSkipShortAudio(enabled)
        }
    }

    fun addCustomFolderUri(uri: Uri) {
        val uriString = uri.toString()
        viewModelScope.launch {
            settingsRepository.addCustomFolderUri(uriString)
            librarySaveQueue.clear()
        }
    }

    fun removeCustomFolderUri(uriString: String) {
        viewModelScope.launch {
            settingsRepository.removeCustomFolderUri(uriString)
            librarySaveQueue.clear()
        }
    }

    fun addBlockedFolderPath(path: String) {
        updateHiddenFolderPaths(normalizedHiddenFolderPaths(hiddenFolderPaths.value + path))
    }

    fun removeBlockedFolderPath(path: String) {
        val normalized = normalizeHiddenFolderPath(path)
        updateHiddenFolderPaths(hiddenFolderPaths.value.filterNot {
            normalizeHiddenFolderPath(it).equals(normalized, ignoreCase = true)
        })
    }

    private fun updateHiddenFolderPaths(paths: List<String>) {
        if (paths == hiddenFolderPaths.value) return
        mutableHiddenFolderPaths.value = paths
        viewModelScope.launch {
            hiddenFolderSaveMutex.withLock {
                val requestedPaths = hiddenFolderPaths.value
                try {
                    settingsRepository.setBlockedFolderPaths(requestedPaths)
                    persistedHiddenFolderPaths = requestedPaths
                } catch (exception: IOException) {
                    if (hiddenFolderPaths.value === requestedPaths) {
                        mutableHiddenFolderPaths.value = persistedHiddenFolderPaths
                    }
                    mutableHiddenFolderSaveFailures.emit(Unit)
                }
            }
        }
    }

    fun hideTrack(track: MusicTrack) {
        updateHiddenTrackUris(hiddenTrackUris.value + track.contentUri)
    }

    fun restoreTracks(contentUris: Set<String>) {
        updateHiddenTrackUris(hiddenTrackUris.value - contentUris)
    }

    private fun updateHiddenTrackUris(contentUris: Set<String>) {
        if (contentUris == hiddenTrackUris.value) return
        mutableHiddenTrackUris.value = contentUris
        viewModelScope.launch {
            hiddenTrackSaveMutex.withLock {
                val requestedUris = hiddenTrackUris.value
                try {
                    settingsRepository.setHiddenTrackUris(requestedUris)
                    persistedHiddenTrackUris = requestedUris
                } catch (exception: IOException) {
                    if (hiddenTrackUris.value === requestedUris) {
                        mutableHiddenTrackUris.value = persistedHiddenTrackUris
                    }
                    mutableHiddenTrackSaveFailures.emit(Unit)
                }
            }
        }
    }

    /** Called only after the source provider confirms successful deletion. */
    fun onTrackSourceDeleted(contentUri: String) {
        libraryUpdates.delete(contentUri)
        allLibraryTracks.value = allLibraryTracks.value.filterNot { it.contentUri == contentUri }
        playlistReadableContentUris.value = playlistReadableContentUris.value - contentUri
        playbackController.removeContentUris(setOf(contentUri))
        restoreTracks(setOf(contentUri))
        librarySaveQueue.enqueue(allLibraryTracks.value, invalidate = true)
    }

    fun setDefaultHomePage(defaultHomePage: DefaultHomePage) {
        viewModelScope.launch {
            settingsRepository.setDefaultHomePage(defaultHomePage)
        }
    }

    fun setLibraryTabIndex(index: Int) {
        viewModelScope.launch {
            settingsRepository.setLibraryTabIndex(index)
        }
    }

    fun setMusicSortConfig(config: MusicSortConfig) {
        viewModelScope.launch {
            settingsRepository.setMusicSortConfig(config)
        }
    }

    fun setAlbumSortConfig(config: AlbumSortConfig) {
        viewModelScope.launch {
            settingsRepository.setAlbumSortConfig(config)
        }
    }

    fun setArtistSortConfig(config: ArtistSortConfig) {
        viewModelScope.launch {
            settingsRepository.setArtistSortConfig(config)
        }
    }

    fun setFolderSortConfig(config: FolderSortConfig) {
        viewModelScope.launch {
            settingsRepository.setFolderSortConfig(config)
        }
    }

    fun updateMusicPresentation(
        query: String,
        sortConfig: MusicSortConfig,
    ) {
        musicPresentationRequest.value = MusicPresentationRequest(query, sortConfig)
    }

    fun updateAlbumPresentation(
        query: String,
        sortConfig: AlbumSortConfig,
    ) {
        albumPresentationRequest.value = AlbumPresentationRequest(
            query = query,
            sortConfig = sortConfig,
        )
    }

    fun updateArtistPresentation(
        query: String,
        sortConfig: ArtistSortConfig,
    ) {
        artistPresentationRequest.value = ArtistPresentationRequest(query, sortConfig)
    }

    fun updateFolderPresentation(
        query: String,
        sortConfig: FolderSortConfig,
    ) {
        folderPresentationRequest.value = FolderPresentationRequest(query, sortConfig)
    }

    fun createPlaylist(
        name: String,
        initialTracks: List<MusicTrack> = emptyList(),
    ): String? {
        if (!playlistsLoaded.value) return null
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) return null
        val now = System.currentTimeMillis().coerceAtLeast(0L)
        val playlistId = UUID.randomUUID().toString()
        val playlist = addTracksToPlaylist(
            playlist = LocalPlaylist(
                id = playlistId,
                name = normalizedName,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
            ),
            tracks = initialTracks,
            nowEpochMillis = now,
            newEntryId = { UUID.randomUUID().toString() },
        )
        playlists.value = listOf(playlist) + playlists.value
        persistPlaylists()
        return playlistId
    }

    fun renamePlaylist(playlistId: String, name: String): Boolean {
        if (!playlistsLoaded.value) return false
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) return false
        val currentPlaylists = playlists.value
        val index = currentPlaylists.indexOfFirst { it.id == playlistId }
        if (index < 0) return false
        val current = currentPlaylists[index]
        if (current.name == normalizedName) return true
        val updated = current.copy(
            name = normalizedName,
            updatedAtEpochMillis = System.currentTimeMillis().coerceAtLeast(0L),
        )
        playlists.value = currentPlaylists.toMutableList().apply { set(index, updated) }
        persistPlaylists()
        return true
    }

    fun setFolderTrackSortConfig(folderKey: String, config: MusicSortConfig) {
        viewModelScope.launch {
            settingsRepository.setFolderTrackSortConfig(folderKey, config)
        }
    }

    fun setPlaylistSortConfig(playlistId: String, config: PlaylistSortConfig) {
        viewModelScope.launch {
            settingsRepository.setPlaylistSortConfig(playlistId, config)
        }
    }

    fun deletePlaylist(playlistId: String): Boolean {
        if (!playlistsLoaded.value) return false
        val currentPlaylists = playlists.value
        val updated = currentPlaylists.filterNot { it.id == playlistId }
        if (updated.size == currentPlaylists.size) return false
        playlists.value = updated
        persistPlaylists()
        setPlaylistSortConfig(playlistId, PlaylistSortConfig())
        return true
    }

    fun addTracksToPlaylist(
        playlistId: String,
        tracks: List<MusicTrack>,
    ): Boolean {
        if (!playlistsLoaded.value || tracks.isEmpty()) return false
        val currentPlaylists = playlists.value
        val index = currentPlaylists.indexOfFirst { it.id == playlistId }
        if (index < 0) return false
        val current = currentPlaylists[index]
        val updated = addTracksToPlaylist(
            playlist = current,
            tracks = tracks,
            nowEpochMillis = System.currentTimeMillis().coerceAtLeast(0L),
            newEntryId = { UUID.randomUUID().toString() },
        )
        if (updated === current) return true
        playlists.value = currentPlaylists.toMutableList().apply { set(index, updated) }
        persistPlaylists()
        return true
    }

    fun removePlaylistEntries(
        playlistId: String,
        entryIds: Set<String>,
    ): Boolean {
        if (!playlistsLoaded.value) return false
        val currentPlaylists = playlists.value
        val index = currentPlaylists.indexOfFirst { it.id == playlistId }
        if (index < 0) return false
        val current = currentPlaylists[index]
        val updated = removePlaylistEntries(
            playlist = current,
            entryIds = entryIds,
            nowEpochMillis = System.currentTimeMillis().coerceAtLeast(0L),
        )
        if (updated === current) return false
        playlists.value = currentPlaylists.toMutableList().apply { set(index, updated) }
        persistPlaylists()
        return true
    }

    fun movePlaylistEntry(
        playlistId: String,
        orderedEntryIds: List<String>,
    ): Boolean {
        if (!playlistsLoaded.value) return false
        val currentPlaylists = playlists.value
        val index = currentPlaylists.indexOfFirst { it.id == playlistId }
        if (index < 0) return false
        val current = currentPlaylists[index]
        val updated = reorderPlaylistEntries(
            playlist = current,
            orderedEntryIds = orderedEntryIds,
            nowEpochMillis = System.currentTimeMillis().coerceAtLeast(0L),
        ) ?: return false
        if (updated === current) return true
        playlists.value = currentPlaylists.toMutableList().apply { set(index, updated) }
        persistPlaylists()
        return true
    }

    fun movePlaylists(orderedPlaylistIds: List<String>): Boolean {
        if (!playlistsLoaded.value) return false
        val current = playlists.value
        val updated = reorderPlaylists(current, orderedPlaylistIds) ?: return false
        if (updated === current) return true
        playlists.value = updated
        persistPlaylists()
        return true
    }

    private fun persistPlaylists() {
        playlistSaveQueue.enqueue(playlists.value)
    }

    fun playTracks(
        tracks: List<MusicTrack>,
        startIndex: Int,
    ) {
        val visible = visibleMusicTracks(tracks, hiddenFolderPaths.value, hiddenTrackUris.value)
        if (visible.isEmpty()) return
        val retainedUris = visible.mapTo(HashSet(), MusicTrack::contentUri)
        val sourceIndices = tracks.indices.filter { tracks[it].contentUri in retainedUris }
        val nextSourceIndex = sourceIndices.firstOrNull { it >= startIndex } ?: sourceIndices.first()
        playbackController.playQueue(visible, sourceIndices.indexOf(nextSourceIndex))
    }

    fun shuffleTracks(tracks: List<MusicTrack>) {
        val visible = visibleMusicTracks(tracks, hiddenFolderPaths.value, hiddenTrackUris.value)
        if (visible.isEmpty()) return
        playbackController.playQueue(
            tracks = visible,
            startIndex = visible.indices.random(),
            requestedMode = PlaybackMode.RANDOM,
        )
    }

    fun refreshTrackAfterExternalEdit(trackId: Long) {
        val track = allLibraryTracks.value.firstOrNull { it.id == trackId } ?: return
        val request = libraryUpdates.beginTagRefresh(track.contentUri)
        viewModelScope.launch {
            try {
                val refreshedTrack = musicRepository.refreshTrackTags(track)
                val beforeUpdate = libraryUpdates.revision
                if (refreshedTrack != null && libraryUpdates.acceptTagRefresh(refreshedTrack, request)) {
                    val currentTracks = allLibraryTracks.value
                    if (currentTracks.any { it.contentUri == track.contentUri }) {
                        allLibraryTracks.value = libraryUpdates.reconcile(currentTracks, beforeUpdate)
                        librarySaveQueue.enqueue(allLibraryTracks.value)
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // Keep the last valid library metadata when the edited file cannot be read.
            } finally {
                if (playbackState.value.currentItem?.trackId == trackId) {
                    lyricsRefreshRevision.value += 1L
                }
            }
        }
    }

    fun playHomeRecommendation(
        selectedTrack: MusicTrack,
        loadedRecommendations: List<MusicTrack>,
    ) {
        if (visibleMusicTracks(listOf(selectedTrack), hiddenFolderPaths.value, hiddenTrackUris.value).isEmpty()) return
        playbackController.playHomeRecommendation(
            selectedTrack = selectedTrack,
            loadedRecommendations = loadedRecommendations,
            allTracks = library.value.tracks,
        )
    }

    fun applyStartupPlayback(allowAutoPlay: Boolean) {
        playbackController.applyStartupPlayback(allowAutoPlay)
    }

    fun playExternalAudio(uri: Uri) {
        playbackController.playExternal(uri)
    }

    fun togglePlayPause() = playbackController.togglePlayPause()

    fun seekTo(positionMs: Long) = playbackController.seekTo(positionMs)

    fun previous() = playbackController.previous()

    fun next() = playbackController.next()

    fun cyclePlaybackMode() = playbackController.cyclePlaybackMode()

    fun setPlaybackSpeed(speed: Float) {
        playbackController.setPlaybackSpeed(speed)
        viewModelScope.launch { settingsRepository.setPlaybackSpeed(speed) }
    }

    val highPrecisionOutput = playbackController.highPrecisionOutput

    fun setHighPrecisionOutput(enabled: Boolean) {
        playbackController.setHighPrecisionOutput(enabled)
        viewModelScope.launch { settingsRepository.setHighPrecisionOutput(enabled) }
    }

    fun setSleepTimerSelectionSeconds(seconds: Int) {
        val value = seconds.coerceIn(0, 86_399)
        mutableSleepTimerSelectionSeconds.value = value
        viewModelScope.launch { settingsRepository.setSleepTimerSeconds(value) }
    }

    fun startSleepTimer(seconds: Int) = playbackController.startSleepTimer(seconds)

    fun cancelSleepTimer() = playbackController.cancelSleepTimer()

    fun acknowledgeSleepTimerInterruption() = playbackController.acknowledgeSleepTimerInterruption()

    fun setAutoExtendSleepTimer(enabled: Boolean) {
        playbackController.setAutoExtendSleepTimer(enabled)
        viewModelScope.launch { settingsRepository.setAutoExtendSleepTimer(enabled) }
    }

    fun setPlaybackPauseFade(enabled: Boolean) {
        playbackController.setPlaybackPauseFade(enabled)
        viewModelScope.launch { settingsRepository.setPlaybackPauseFade(enabled) }
    }

    fun setAudioFocusEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAudioFocusEnabled(enabled) }
    }

    fun setTransientAudioFocusBehavior(behavior: TransientAudioFocusBehavior) {
        viewModelScope.launch { settingsRepository.setTransientAudioFocusBehavior(behavior) }
    }

    fun setAutoPlayOnStart(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoPlayOnStart(enabled) }
    }

    fun setTerminateOnTaskRemoved(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setTerminateOnTaskRemoved(enabled) }
    }

    fun setShowNotificationPlaybackModeButton(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShowNotificationPlaybackModeButton(enabled) }
    }

    fun setShowNotificationCloseButton(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShowNotificationCloseButton(enabled) }
    }

    fun playNext(track: MusicTrack) {
        if (visibleMusicTracks(listOf(track), hiddenFolderPaths.value, hiddenTrackUris.value).isNotEmpty()) {
            playbackController.playNext(track)
        }
    }

    fun appendToQueue(track: MusicTrack) {
        if (visibleMusicTracks(listOf(track), hiddenFolderPaths.value, hiddenTrackUris.value).isNotEmpty()) {
            playbackController.append(track)
        }
    }

    fun jumpToQueueItem(index: Int) = playbackController.jumpTo(index)

    fun moveQueueItem(fromIndex: Int, toIndex: Int) =
        playbackController.move(fromIndex, toIndex)

    fun removeQueueItem(index: Int) = playbackController.remove(index)

    fun clearQueue() = playbackController.clear()

    fun clearMusicLibrary() {
        if (scanJob?.isActive == true) return
        libraryUpdates.clear()
        playbackController.clear()
        allLibraryTracks.value = emptyList()
        scanStatus.value = ScanStatus.Idle
        scanGeneration.value += 1L
        librarySaveQueue.clear()
    }

    fun scanMusic() {
        if (!hasAudioPermission()) {
            markPermissionRequired()
            return
        }
        startMusicScan(
            restoreCachedTracks = false,
            refreshAfterRestore = true,
            notifyUser = true,
        )
    }

    private fun startMusicScan(
        restoreCachedTracks: Boolean,
        refreshAfterRestore: Boolean,
        notifyUser: Boolean = false,
    ) {
        // The UI invokes commands on the main thread, so this debounces repeated scan taps.
        if (scanJob?.isActive == true) return

        // Publish loading before launch so an empty initial list can never render as confirmed empty.
        scanStatus.value = ScanStatus.Scanning
        scanJob = viewModelScope.launch {
            val scanRevision = libraryUpdates.revision
            val settings = loadedSettings.value.value
            val cachedTracks = if (restoreCachedTracks) {
                try {
                    musicRepository.loadCachedMusic()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }
            if (cachedTracks != null) {
                allLibraryTracks.value = libraryUpdates.reconcile(cachedTracks, scanRevision)
                if (!refreshAfterRestore) {
                    scanStatus.value = ScanStatus.Success(allLibraryTracks.value.size)
                    scanGeneration.value += 1L
                    return@launch
                }
            }
            if (restoreCachedTracks && !refreshAfterRestore) {
                scanStatus.value = ScanStatus.Idle
                return@launch
            }

            val previousTracks = allLibraryTracks.value
            try {
                val scannedTracks = musicRepository.scanMusic(
                    previousTracks = previousTracks,
                    refreshAudioProperties = false,
                    customFolderUris = settings.customFolderUris,
                    skipShortAudio = settings.skipShortAudio,
                    onInitialTracks = { initialTracks ->
                        withContext(Dispatchers.Main.immediate) {
                            allLibraryTracks.value = libraryUpdates.reconcile(initialTracks, scanRevision)
                        }
                    },
                )
                val retainedTracks = libraryUpdates.reconcile(scannedTracks, scanRevision)
                val libraryChanged = retainedTracks != previousTracks
                allLibraryTracks.value = retainedTracks
                if (libraryChanged) {
                    librarySaveQueue.enqueue(retainedTracks)
                }
                scanStatus.value = ScanStatus.Success(retainedTracks.size)
                scanGeneration.value += 1L
                if (shouldEmitScanCompletion(libraryChanged, notifyUser)) {
                    val visibleCount = withContext(Dispatchers.Default) {
                        visibleMusicTracks(retainedTracks, hiddenFolderPaths.value, hiddenTrackUris.value).size
                    }
                    mutableScanCompletionEvents.emit(visibleCount)
                }
                if (shouldEmitScanNoChanges(libraryChanged, notifyUser)) {
                    mutableScanNoChangesEvents.emit(Unit)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                // A failed refresh keeps the last successful list visible.
                allLibraryTracks.value = libraryUpdates.reconcile(previousTracks, scanRevision)
                if (libraryUpdates.revision > scanRevision) {
                    librarySaveQueue.enqueue(allLibraryTracks.value)
                }
                scanStatus.value = ScanStatus.Error(
                    exception.message.orEmpty(),
                )
            }
        }
    }

    fun markPermissionRequired() {
        scanStatus.value = ScanStatus.PermissionRequired
    }

    fun markPermissionGrantedWithoutScan() {
        scanStatus.value = ScanStatus.Idle
    }

    private fun hasAudioPermission(): Boolean {
        // Android 13 split audio access from the legacy shared-storage permission.
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(
            getApplication(),
            permission,
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCleared() {
        playbackController.release()
        super.onCleared()
    }
}

internal fun shouldEmitScanNoChanges(
    libraryChanged: Boolean,
    notifyIfUnchanged: Boolean,
): Boolean = !libraryChanged && notifyIfUnchanged

internal fun shouldEmitScanCompletion(
    libraryChanged: Boolean,
    notifyUser: Boolean,
): Boolean = libraryChanged && notifyUser
