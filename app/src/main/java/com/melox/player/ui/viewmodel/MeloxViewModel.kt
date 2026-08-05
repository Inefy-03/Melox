package com.melox.player.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import com.melox.player.data.repository.MusicRepository
import com.melox.player.data.repository.LyricsRepository
import com.melox.player.data.repository.LyricsRequest
import com.melox.player.data.repository.SettingsRepository
import com.melox.player.model.AppSettings
import com.melox.player.model.BottomBarStyle
import com.melox.player.model.DefaultHomePage
import com.melox.player.model.MusicTrack
import com.melox.player.model.LyricsUiState
import com.melox.player.model.PlaybackUiState
import com.melox.player.model.ScanStatus
import com.melox.player.model.ThemeMode
import com.melox.player.playback.PlaybackController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/** Immutable screen state assembled from persisted settings and the current scan session. */
data class AppUiState(
    val settings: AppSettings,
    val settingsLoaded: Boolean,
    val tracks: List<MusicTrack>,
    val recentlyAddedTrackIds: Set<Long>,
    val albums: List<AlbumGroup>,
    val artists: List<ArtistGroup>,
    val folders: List<FolderGroup>,
    val scanStatus: ScanStatus,
)

data class MusicPresentationState(
    val items: List<MusicTrack> = emptyList(),
    val queueItems: List<MusicTrack> = emptyList(),
    val sectionIndexMap: Map<String, Int> = emptyMap(),
)

data class AlbumPresentationState(
    val items: List<AlbumGroup> = emptyList(),
    val sectionIndexMap: Map<String, Int> = emptyMap(),
)

data class ArtistPresentationState(
    val items: List<ArtistGroup> = emptyList(),
    val sectionIndexMap: Map<String, Int> = emptyMap(),
)

data class FolderPresentationState(
    val items: List<FolderGroup> = emptyList(),
    val sectionIndexMap: Map<String, Int> = emptyMap(),
)

private data class LibraryProjection(
    val tracks: List<MusicTrack> = emptyList(),
    val albums: List<AlbumGroup> = emptyList(),
    val artists: List<ArtistGroup> = emptyList(),
    val folders: List<FolderGroup> = emptyList(),
)

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
    private val musicRepository = MusicRepository(application)
    private val lyricsRepository = LyricsRepository(application)
    private val playbackController = PlaybackController(application)
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

    private val library = MutableStateFlow(LibraryProjection())
    private val scanStatus = MutableStateFlow<ScanStatus>(
        if (hasInitialAudioPermission) ScanStatus.Scanning else ScanStatus.PermissionRequired,
    )
    private val recentlyAddedTrackIds = MutableStateFlow<Set<Long>>(emptySet())
    val playbackState: StateFlow<PlaybackUiState> = playbackController.state
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
    val compactPlaybackState: StateFlow<PlaybackUiState> = playbackState
        .map { state ->
            state.copy(
                positionMs = 0L,
                bufferedPositionMs = 0L,
            )
        }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = playbackState.value.copy(
                positionMs = 0L,
                bufferedPositionMs = 0L,
            ),
        )
    @OptIn(ExperimentalCoroutinesApi::class)
    val lyricsState: StateFlow<LyricsUiState> = combine(
        playbackState,
        library,
    ) { playback, projection ->
        val item = playback.currentItem ?: return@combine null
        val track = item.trackId?.let { trackId ->
            projection.tracks.firstOrNull { it.id == trackId }
        }
        LyricsRequest(
            mediaId = item.mediaId,
            contentUri = item.contentUri,
            fileName = track?.fileName,
            folderPath = track?.folderPath,
        )
    }
        .distinctUntilChanged()
        .transformLatest { request ->
            if (request == null) {
                emit(LyricsUiState.Unavailable)
            } else {
                emit(LyricsUiState.Loading)
                emit(
                    lyricsRepository.load(request)
                        ?.let(LyricsUiState::Available)
                        ?: LyricsUiState.Unavailable,
                )
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
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
        recentlyAddedTrackIds,
    ) { loadedSettings, library, scanStatus, recentlyAddedTrackIds ->
        AppUiState(
            settings = loadedSettings.value,
            settingsLoaded = loadedSettings.loaded,
            tracks = library.tracks,
            recentlyAddedTrackIds = recentlyAddedTrackIds,
            albums = library.albums,
            artists = library.artists,
            folders = library.folders,
            scanStatus = scanStatus,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppUiState(
            settings = loadedSettings.value.value,
            settingsLoaded = loadedSettings.value.loaded,
            tracks = emptyList(),
            recentlyAddedTrackIds = emptySet(),
            albums = emptyList(),
            artists = emptyList(),
            folders = emptyList(),
            scanStatus = scanStatus.value,
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
                    request.sortConfig.field == MusicSortField.FILE_NAME
            )
        ) {
            buildMap {
                items.forEachIndexed { index, track ->
                    val key = when (request.sortConfig.field) {
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
        AlbumPresentationState(items, sectionIndexMap)
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
        ArtistPresentationState(items, sectionIndexMap)
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
        FolderPresentationState(items, sectionIndexMap)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, FolderPresentationState())

    init {
        // Startup scanning never triggers a permission dialog; the Activity owns that UI flow.
        if (hasInitialAudioPermission) {
            startMusicScan(restoreCachedTracks = true)
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

    fun setFloatingBottomBar(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setFloatingBottomBar(enabled)
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

    fun playTracks(
        tracks: List<MusicTrack>,
        startIndex: Int,
    ) {
        playbackController.playQueue(tracks, startIndex)
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

    fun playNext(track: MusicTrack) = playbackController.playNext(track)

    fun appendToQueue(track: MusicTrack) = playbackController.append(track)

    fun jumpToQueueItem(index: Int) = playbackController.jumpTo(index)

    fun removeQueueItem(index: Int) = playbackController.remove(index)

    fun clearQueue() = playbackController.clear()

    fun scanMusic() {
        if (!hasAudioPermission()) {
            markPermissionRequired()
            return
        }
        startMusicScan(restoreCachedTracks = false)
    }

    private fun startMusicScan(restoreCachedTracks: Boolean) {
        // The UI invokes commands on the main thread, so this debounces repeated scan taps.
        if (scanJob?.isActive == true) return

        // Publish loading before launch so an empty initial list can never render as confirmed empty.
        scanStatus.value = ScanStatus.Scanning
        scanJob = viewModelScope.launch {
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
                // Publish the lightweight root-page data before grouping. Home and Songs
                // become usable while the library tabs are prepared off the UI thread.
                library.value = LibraryProjection(tracks = cachedTracks)
                library.value = createLibraryProjection(cachedTracks)
            }

            try {
                val previousTracks = cachedTracks ?: library.value.tracks
                val previousTrackIds = previousTracks.mapTo(mutableSetOf(), MusicTrack::id)
                val scannedTracks = musicRepository.scanMusic(
                    previousTracks = previousTracks,
                    refreshAudioProperties = !restoreCachedTracks,
                )
                recentlyAddedTrackIds.value = if (previousTracks.isEmpty()) {
                    emptySet()
                } else {
                    scannedTracks
                        .asSequence()
                        .map(MusicTrack::id)
                        .filterNot(previousTrackIds::contains)
                        .toSet()
                }
                if (scannedTracks != previousTracks) {
                    library.value = LibraryProjection(tracks = scannedTracks)
                    library.value = createLibraryProjection(scannedTracks)
                    musicRepository.cacheMusic(scannedTracks)
                }
                scanStatus.value = ScanStatus.Success(scannedTracks.size)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                // A failed refresh keeps the last successful list visible.
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

    private suspend fun createLibraryProjection(
        tracks: List<MusicTrack>,
    ): LibraryProjection = withContext(Dispatchers.Default) {
        LibraryProjection(
            tracks = tracks,
            albums = buildAlbumGroups(tracks),
            artists = buildArtistGroups(tracks),
            folders = buildFolderGroups(tracks),
        )
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
