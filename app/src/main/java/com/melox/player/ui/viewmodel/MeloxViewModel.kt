package com.melox.player.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.melox.player.model.LyricAnimationMode
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
import com.melox.player.data.repository.PlaylistRepository
import com.melox.player.data.repository.SettingsRepository
import com.melox.player.data.playlist.addTracksToPlaylist
import com.melox.player.data.playlist.PlaylistSortConfig
import com.melox.player.data.playlist.removePlaylistEntries
import com.melox.player.data.playlist.reorderPlaylistEntries
import com.melox.player.data.playlist.reorderPlaylists
import com.melox.player.model.AppSettings
import com.melox.player.model.BottomBarStyle
import com.melox.player.model.DefaultHomePage
import com.melox.player.model.DynamicColorSource
import com.melox.player.model.LocalPlaylist
import com.melox.player.model.LyricsDocument
import com.melox.player.model.LyricsFormat
import com.melox.player.model.LyricsSidecarFormatPriority
import com.melox.player.model.LyricsSource
import com.melox.player.model.LyricsSourcePriority
import com.melox.player.model.MusicTrack
import com.melox.player.model.PlaybackMode
import com.melox.player.model.NavigationTransitionStyle
import com.melox.player.model.LyricsUiState
import com.melox.player.model.PlaybackUiState
import com.melox.player.model.PlaybackBackgroundStyle
import com.melox.player.model.ScanStatus
import com.melox.player.model.ThemeMode
import com.melox.player.model.withTrackMetadata
import com.melox.player.playback.PlaybackController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
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

private data class LyricsResolutionKey(
    val source: LyricsSource,
    val sidecarFormat: LyricsFormat?,
)

private fun LyricsDocument?.resolutionKey(): LyricsResolutionKey? = this?.let { document ->
    LyricsResolutionKey(
        source = document.source,
        sidecarFormat = document.format.takeIf { document.source == LyricsSource.SIDECAR },
    )
}

internal fun LyricsRequest.hasSameLyricsContentTarget(other: LyricsRequest): Boolean =
    mediaId == other.mediaId &&
        contentUri == other.contentUri &&
        fileName == other.fileName &&
        folderPath == other.folderPath &&
        durationMs == other.durationMs &&
        refreshRevision == other.refreshRevision

internal fun shouldShowLyricsLoading(
    previousRequest: LyricsRequest?,
    request: LyricsRequest,
): Boolean = previousRequest?.hasSameLyricsContentTarget(request) != true

internal fun shouldPublishLyricsResolution(
    previousRequest: LyricsRequest?,
    request: LyricsRequest,
    previousDocument: LyricsDocument?,
    document: LyricsDocument?,
): Boolean = shouldShowLyricsLoading(previousRequest, request) ||
    previousDocument.resolutionKey() != document.resolutionKey()

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

        val document = loadDocument(request)
        if (
            shouldPublishLyricsResolution(
                previousRequest = previousRequest,
                request = request,
                previousDocument = previousDocument,
                document = document,
            )
        ) {
            send(document?.let(LyricsUiState::Available) ?: LyricsUiState.Unavailable)
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
): Flow<LibraryProjection> = combine(tracks, hiddenPaths) { allTracks, paths ->
    allTracks to paths
}.mapLatest { (allTracks, paths) ->
    withContext(Dispatchers.Default) {
        val visibleTracks = visibleMusicTracks(allTracks, paths)
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

private data class MusicPresentationRequest(
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
    private val library = visibleLibraryProjections(allLibraryTracks, hiddenFolderPaths)
        .stateIn(viewModelScope, SharingStarted.Eagerly, LibraryProjection())
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
    private val playlistSaveMutex = Mutex()
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
    val compactPlaybackState: StateFlow<PlaybackUiState> = combine(
        compactControllerPlaybackState,
        allLibraryTracks,
    ) { playback, tracks ->
        playback.withTrackMetadata(tracks)
    }
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
    private val lyricsRequests = combine(
        playbackState,
        allLibraryTracks,
        lyricsRefreshRevision,
        loadedSettings,
    ) { playback, tracks, refreshRevision, loadedSettings ->
        val item = playback.currentItem ?: return@combine null
        val track = item.trackId?.let { trackId ->
            tracks.firstOrNull { it.id == trackId }
        }
        LyricsRequest(
            mediaId = item.mediaId,
            contentUri = item.contentUri,
            fileName = track?.fileName,
            folderPath = track?.folderPath,
            durationMs = playback.durationMs,
            refreshRevision = refreshRevision,
            sourcePriority = loadedSettings.value.lyricsSourcePriority,
            sidecarFormatPriority = loadedSettings.value.lyricsSidecarFormatPriority,
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

    val musicPresentation: StateFlow<MusicPresentationState> = combine(
        library,
        musicPresentationRequest,
    ) { projection, request ->
        val queueItems = if (request.sortConfig == MusicSortConfig()) {
            projection.tracks
        } else {
            sortMusicTracks(projection.tracks, request.sortConfig)
        }
        val items = filterMusicTracks(queueItems, request.query)
        val sectionIndexMap = if (
            request.query.isBlank() &&
            (
                request.sortConfig.field == MusicSortField.TITLE ||
                    request.sortConfig.field == MusicSortField.ARTIST ||
                    request.sortConfig.field == MusicSortField.FILE_NAME
            )
        ) {
            buildMap {
                items.forEachIndexed { index, track ->
                    val key = when (request.sortConfig.field) {
                        MusicSortField.ARTIST -> createMusicSortKeys(track.artist).section
                        MusicSortField.FILE_NAME -> createMusicSortKeys(track.fileName).section
                        else -> track.titleSectionKey
                    }
                    putIfAbsent(key, index)
                }
            }
        } else {
            emptyMap()
        }
        MusicPresentationState(
            items = items,
            queueItems = queueItems,
            sectionIndexMap = sectionIndexMap,
            query = request.query,
            sortConfig = request.sortConfig,
        )
    }
        .flowOn(Dispatchers.Default)
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

    fun setShowLyricsTranslation(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowLyricsTranslation(enabled)
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

    fun setLyricsSidecarFormatPriority(priority: LyricsSidecarFormatPriority) {
        viewModelScope.launch {
            settingsRepository.setLyricsSidecarFormatPriority(priority)
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
            musicRepository.clearCachedMusic()
        }
    }

    fun removeCustomFolderUri(uriString: String) {
        viewModelScope.launch {
            settingsRepository.removeCustomFolderUri(uriString)
            musicRepository.clearCachedMusic()
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
        viewModelScope.launch {
            withContext(Dispatchers.IO + NonCancellable) {
                playlistSaveMutex.withLock {
                    runCatching { playlistRepository.save(playlists.value) }
                }
            }
        }
    }

    fun playTracks(
        tracks: List<MusicTrack>,
        startIndex: Int,
    ) {
        playbackController.playQueue(tracks, startIndex)
    }

    fun shuffleTracks(tracks: List<MusicTrack>) {
        if (tracks.isEmpty()) return
        playbackController.playQueue(
            tracks = tracks,
            startIndex = tracks.indices.random(),
            requestedMode = PlaybackMode.RANDOM,
        )
    }

    fun refreshTrackAfterExternalEdit(trackId: Long) {
        viewModelScope.launch {
            val track = allLibraryTracks.value.firstOrNull { it.id == trackId }
            try {
                val refreshedTrack = track?.let { musicRepository.refreshTrack(it) }
                if (refreshedTrack != null) {
                    val currentTracks = allLibraryTracks.value
                    if (currentTracks.any { it.id == trackId }) {
                        val updatedTracks = currentTracks.map { currentTrack ->
                            if (currentTrack.id == trackId) refreshedTrack else currentTrack
                        }
                        allLibraryTracks.value = updatedTracks
                        musicRepository.cacheMusic(updatedTracks)
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // Keep the last valid library metadata when the edited file cannot be read.
            } finally {
                lyricsRefreshRevision.value += 1L
            }
        }
    }

    fun playHomeRecommendation(
        selectedTrack: MusicTrack,
        loadedRecommendations: List<MusicTrack>,
    ) {
        playbackController.playHomeRecommendation(
            selectedTrack = selectedTrack,
            loadedRecommendations = loadedRecommendations,
            allTracks = library.value.tracks,
        )
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
        if (playbackController.state.value.floatOutputActive && speed != 1f) return
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

    fun playNext(track: MusicTrack) = playbackController.playNext(track)

    fun appendToQueue(track: MusicTrack) = playbackController.append(track)

    fun jumpToQueueItem(index: Int) = playbackController.jumpTo(index)

    fun moveQueueItem(fromIndex: Int, toIndex: Int) =
        playbackController.move(fromIndex, toIndex)

    fun removeQueueItem(index: Int) = playbackController.remove(index)

    fun clearQueue() = playbackController.clear()

    fun clearMusicLibrary() {
        if (scanJob?.isActive == true) return

        viewModelScope.launch {
            musicRepository.clearCachedMusic()
            // A scan that began after confirmation is newer than this clear request.
            if (scanJob?.isActive == true) return@launch
            playbackController.clear()
            allLibraryTracks.value = emptyList()
            scanStatus.value = ScanStatus.Idle
            scanGeneration.value += 1L
        }
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
                allLibraryTracks.value = cachedTracks
                if (!refreshAfterRestore) {
                    scanStatus.value = ScanStatus.Success(cachedTracks.size)
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
                        allLibraryTracks.value = initialTracks
                    },
                )
                val libraryChanged = scannedTracks != previousTracks
                allLibraryTracks.value = scannedTracks
                if (libraryChanged) {
                    musicRepository.cacheMusic(scannedTracks)
                }
                scanStatus.value = ScanStatus.Success(scannedTracks.size)
                scanGeneration.value += 1L
                if (shouldEmitScanCompletion(libraryChanged, notifyUser)) {
                    val visibleCount = withContext(Dispatchers.Default) {
                        visibleMusicTracks(scannedTracks, hiddenFolderPaths.value).size
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
                allLibraryTracks.value = previousTracks
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
