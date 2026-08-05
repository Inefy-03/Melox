package com.melox.player.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.selection.selectable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import com.melox.player.R
import com.melox.player.data.library.MusicSortConfig
import com.melox.player.data.library.MusicSortField
import com.melox.player.data.library.AlbumSortConfig
import com.melox.player.data.library.AlbumSortField
import com.melox.player.data.library.AlbumGridStyle
import com.melox.player.data.library.ArtistGroup
import com.melox.player.data.library.ArtistSortConfig
import com.melox.player.data.library.ArtistSortField
import com.melox.player.data.library.FolderSortConfig
import com.melox.player.data.library.FolderSortField
import com.melox.player.model.BottomBarStyle
import com.melox.player.model.DefaultHomePage
import com.melox.player.model.ScanStatus
import com.melox.player.model.ThemeMode
import com.melox.player.model.MusicTrack
import com.melox.player.ui.component.MiuixBlurredBar
import com.melox.player.ui.component.miuixBarColor
import com.melox.player.ui.component.rememberMiuixBlurBackdrop
import com.melox.player.ui.component.library.MusicSortButton
import com.melox.player.ui.component.library.prefetchArtwork
import com.melox.player.ui.component.library.AlphabetSections
import com.melox.player.ui.component.library.AlphabetSideBar
import com.melox.player.ui.component.library.AlbumSortButton
import com.melox.player.ui.component.library.ArtistSortButton
import com.melox.player.ui.component.library.FolderSortButton
import com.melox.player.ui.component.playback.MiniPlayer
import com.melox.player.ui.component.playback.PLAYER_FULL_ARTWORK_REQUEST_SIZE
import com.melox.player.ui.component.playback.PlayerSheetArtworkOverlay
import com.melox.player.ui.component.playback.PlayerSheetContentOverlay
import com.melox.player.ui.component.playback.rememberPlayerSheetTransitionState
import com.melox.player.ui.navigation.PredictiveNavDisplay
import com.melox.player.ui.screen.library.MusicListScreen
import com.melox.player.ui.screen.library.AlbumDetailScreen
import com.melox.player.ui.screen.library.AlbumLibraryScreen
import com.melox.player.ui.screen.library.ArtistDetailScreen
import com.melox.player.ui.screen.library.ArtistLibraryScreen
import com.melox.player.ui.screen.library.FolderDetailScreen
import com.melox.player.ui.screen.library.FolderLibraryScreen
import com.melox.player.ui.screen.home.HomeScreen
import com.melox.player.ui.screen.home.rememberHomeRecommendations
import com.melox.player.ui.screen.playback.FullPlayerScreen
import com.melox.player.ui.screen.playback.QueueSheet
import com.melox.player.ui.screen.settings.SettingsScreen
import com.melox.player.ui.screen.settings.AboutScreen
import com.melox.player.ui.screen.settings.ThemeSettingsScreen
import com.melox.player.ui.viewmodel.MeloxViewModel
import com.melox.player.ui.theme.MeloxTheme
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.SearchCleanup
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.MiuixPopupHost
import top.yukonga.miuix.kmp.utils.overScrollHorizontal
import kotlin.math.abs
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val HOME_TAB_INDEX = 0
private const val SONGS_TAB_INDEX = 1
private const val LIBRARY_TAB_INDEX = 2
private const val SETTINGS_TAB_INDEX = 3
private const val ROOT_TAB_COUNT = 4

private const val LIBRARY_ALBUMS_TAB_INDEX = 0
private const val LIBRARY_ARTISTS_TAB_INDEX = 1
private const val LIBRARY_FOLDERS_TAB_INDEX = 2
private const val LIBRARY_TAB_COUNT = 3

internal fun rootPagerUserScrollEnabled(
    selectedPage: Int,
    homeRecommendationPage: Int,
): Boolean = selectedPage != HOME_TAB_INDEX || homeRecommendationPage == 0

private enum class AppRoute {
    ROOT,
    THEME_SETTINGS,
    ABOUT,
    ALBUM_DETAIL,
    ARTIST_DETAIL,
    FOLDER_DETAIL,
}

private enum class PermissionRequestSource {
    STARTUP,
    MANUAL_SCAN,
}

@Composable
fun MeloxApp(
    viewModel: MeloxViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val compactPlayback by viewModel.compactPlaybackState.collectAsStateWithLifecycle()
    val currentTrackId by viewModel.currentTrackId.collectAsStateWithLifecycle()
    val hasCurrentItem by viewModel.hasCurrentItem.collectAsStateWithLifecycle()
    val musicPresentation by viewModel.musicPresentation.collectAsStateWithLifecycle()
    val albumPresentation by viewModel.albumPresentation.collectAsStateWithLifecycle()
    val artistPresentation by viewModel.artistPresentation.collectAsStateWithLifecycle()
    val folderPresentation by viewModel.folderPresentation.collectAsStateWithLifecycle()
    val settings = uiState.settings
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val systemDark = isSystemInDarkTheme()
    val isDark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    PlaybackArtworkPrefetchEffect(viewModel)
    // API level alone is insufficient: liquid glass also needs RuntimeShader support at runtime.
    val liquidGlassSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        isRuntimeShaderSupported()
    val initialRootPage = remember {
        when (settings.defaultHomePage) {
            DefaultHomePage.HOME -> HOME_TAB_INDEX
            DefaultHomePage.SONGS -> SONGS_TAB_INDEX
            DefaultHomePage.LIBRARY -> LIBRARY_TAB_INDEX
        }
    }
    val pagerState = rememberPagerState(
        initialPage = initialRootPage,
        pageCount = { ROOT_TAB_COUNT },
    )
    var homeRecommendationPage by remember { mutableIntStateOf(0) }
    val libraryPagerState = rememberPagerState(pageCount = { LIBRARY_TAB_COUNT })
    val playerPagerState = rememberPlayerPagerState(pagerState)
    val homeRecommendations = rememberHomeRecommendations(
        tracks = uiState.tracks,
        active = playerPagerState.selectedPage == HOME_TAB_INDEX,
    )
    var permissionRequestSource by rememberSaveable {
        mutableStateOf<PermissionRequestSource?>(null)
    }
    var startupPermissionRequested by rememberSaveable { mutableStateOf(false) }
    var musicSortFieldOrdinal by rememberSaveable {
        mutableIntStateOf(MusicSortField.TITLE.ordinal)
    }
    var musicSortDescending by rememberSaveable { mutableStateOf(false) }
    var songSearchQuery by rememberSaveable { mutableStateOf("") }
    var songSearchVisible by rememberSaveable { mutableStateOf(false) }
    var songSearchFocused by remember { mutableStateOf(false) }
    var librarySearchQuery by rememberSaveable { mutableStateOf("") }
    var librarySearchVisible by rememberSaveable { mutableStateOf(false) }
    var librarySearchFocused by remember { mutableStateOf(false) }
    var albumSortFieldOrdinal by rememberSaveable {
        mutableIntStateOf(AlbumSortField.ALBUM.ordinal)
    }
    var albumSortDescending by rememberSaveable { mutableStateOf(false) }
    var albumGridStyleOrdinal by rememberSaveable {
        mutableIntStateOf(AlbumGridStyle.TWO_SMALL.ordinal)
    }
    var artistSortFieldOrdinal by rememberSaveable {
        mutableIntStateOf(ArtistSortField.NAME.ordinal)
    }
    var artistSortDescending by rememberSaveable { mutableStateOf(false) }
    var folderSortFieldOrdinal by rememberSaveable {
        mutableIntStateOf(FolderSortField.NAME.ordinal)
    }
    var folderSortDescending by rememberSaveable { mutableStateOf(false) }
    var showQueue by rememberSaveable { mutableStateOf(false) }
    val playerTransition = rememberPlayerSheetTransitionState()
    val miniPlayerLayer = rememberGraphicsLayer()
    val fullPlayerLayer = rememberGraphicsLayer()
    var currentRoute by rememberSaveable { mutableStateOf(AppRoute.ROOT) }
    var selectedAlbumKey by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedArtistKey by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFolderKey by rememberSaveable { mutableStateOf<String?>(null) }
    var albumParentRoute by rememberSaveable { mutableStateOf(AppRoute.ROOT) }
    var libraryPreferencesHydrated by remember { mutableStateOf(false) }
    val musicSortConfig = MusicSortConfig(
        field = MusicSortField.entries.getOrElse(musicSortFieldOrdinal) {
            MusicSortField.TITLE
        },
        descending = musicSortDescending,
    )
    val albumSortConfig = AlbumSortConfig(
        field = AlbumSortField.entries.getOrElse(albumSortFieldOrdinal) {
            AlbumSortField.ALBUM
        },
        descending = albumSortDescending,
        gridStyle = AlbumGridStyle.entries.getOrElse(albumGridStyleOrdinal) {
            AlbumGridStyle.TWO_SMALL
        },
    )
    val artistSortConfig = ArtistSortConfig(
        field = ArtistSortField.entries.getOrElse(artistSortFieldOrdinal) {
            ArtistSortField.NAME
        },
        descending = artistSortDescending,
    )
    val folderSortConfig = FolderSortConfig(
        field = FolderSortField.entries.getOrElse(folderSortFieldOrdinal) {
            FolderSortField.NAME
        },
        descending = folderSortDescending,
    )
    val audioPermission = remember { requiredAudioPermission() }
    val homeTitle = stringResource(R.string.navigation_home)
    val musicTitle = stringResource(R.string.navigation_music)
    val libraryTitle = stringResource(R.string.navigation_library)
    val albumsTitle = stringResource(R.string.navigation_albums)
    val artistsTitle = stringResource(R.string.navigation_artists)
    val foldersTitle = stringResource(R.string.navigation_folders)
    val settingsTitle = stringResource(R.string.navigation_settings)
    val libraryTabs = remember(albumsTitle, artistsTitle, foldersTitle) {
        listOf(albumsTitle, artistsTitle, foldersTitle)
    }
    val openPlayer = { playerTransition.open() }
    val closePlayer = { playerTransition.close() }

    LaunchedEffect(uiState.settingsLoaded) {
        if (uiState.settingsLoaded && !libraryPreferencesHydrated) {
            musicSortFieldOrdinal = settings.musicSortFieldOrdinal
            musicSortDescending = settings.musicSortDescending
            albumSortFieldOrdinal = settings.albumSortFieldOrdinal
            albumSortDescending = settings.albumSortDescending
            albumGridStyleOrdinal = settings.albumGridStyleOrdinal
                .coerceIn(AlbumGridStyle.entries.indices)
            artistSortFieldOrdinal = settings.artistSortFieldOrdinal
            artistSortDescending = settings.artistSortDescending
            folderSortFieldOrdinal = settings.folderSortFieldOrdinal
            folderSortDescending = settings.folderSortDescending
            libraryPagerState.scrollToPage(
                settings.libraryTabIndex.coerceIn(0, LIBRARY_TAB_COUNT - 1),
            )
            libraryPreferencesHydrated = true
        }
    }
    LaunchedEffect(libraryPreferencesHydrated, libraryPagerState) {
        if (!libraryPreferencesHydrated) return@LaunchedEffect
        snapshotFlow { libraryPagerState.settledPage }
            .distinctUntilChanged()
            .collectLatest(viewModel::setLibraryTabIndex)
    }
    LaunchedEffect(songSearchQuery, musicSortConfig) {
        viewModel.updateMusicPresentation(songSearchQuery, musicSortConfig)
    }
    LaunchedEffect(librarySearchQuery, albumSortConfig) {
        viewModel.updateAlbumPresentation(librarySearchQuery, albumSortConfig)
    }
    LaunchedEffect(librarySearchQuery, artistSortConfig) {
        viewModel.updateArtistPresentation(librarySearchQuery, artistSortConfig)
    }
    LaunchedEffect(librarySearchQuery, folderSortConfig) {
        viewModel.updateFolderPresentation(librarySearchQuery, folderSortConfig)
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collectLatest(playerPagerState::syncPage)
    }
    LaunchedEffect(playerTransition.animationRequest, playerTransition.isReady) {
        if (!playerTransition.isReady || playerTransition.isDragging) return@LaunchedEffect
        playerTransition.animateToTarget()
    }
    LaunchedEffect(hasCurrentItem) {
        if (!hasCurrentItem && playerTransition.targetOpen) {
            closePlayer()
        }
    }
    val focusManager = LocalFocusManager.current
    val softwareKeyboardController = LocalSoftwareKeyboardController.current
    val dismissLibrarySearchFocus = {
        librarySearchFocused = false
        focusManager.clearFocus(force = true)
        softwareKeyboardController?.hide()
    }
    var previousRootPage by remember { mutableIntStateOf(pagerState.currentPage) }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collectLatest { currentPage ->
                if (currentPage == previousRootPage) return@collectLatest
                songSearchFocused = false
                librarySearchFocused = false
                previousRootPage = currentPage
                focusManager.clearFocus(force = true)
                softwareKeyboardController?.hide()
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            if (permissionRequestSource == PermissionRequestSource.MANUAL_SCAN) {
                viewModel.scanMusic()
            } else {
                viewModel.markPermissionGrantedWithoutScan()
            }
        } else {
            viewModel.markPermissionRequired()
        }
        permissionRequestSource = null
    }

    val scanMusic: () -> Unit = {
        when {
            context.hasPermission(audioPermission) -> viewModel.scanMusic()
            else -> {
                permissionRequestSource = PermissionRequestSource.MANUAL_SCAN
                permissionLauncher.launch(audioPermission)
            }
        }
    }

    LaunchedEffect(audioPermission) {
        if (
            !startupPermissionRequested &&
            !context.hasPermission(audioPermission)
        ) {
            startupPermissionRequested = true
            permissionRequestSource = PermissionRequestSource.STARTUP
            permissionLauncher.launch(audioPermission)
        }
    }

    MeloxTheme(settings = settings) {
        DisposableEffect(activity, isDark) {
            val componentActivity = activity as? ComponentActivity
            val transparentStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
            ) { isDark }
            componentActivity?.enableEdgeToEdge(
                statusBarStyle = transparentStyle,
                navigationBarStyle = transparentStyle,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                activity?.window?.isNavigationBarContrastEnforced = false
            }
            onDispose {}
        }
        val rootScope = rememberCoroutineScope()
        val homeListState = rememberLazyListState()
        val songsListState = rememberLazyListState()
        val albumsGridState = rememberLazyGridState()
        val artistsListState = rememberLazyListState()
        val foldersListState = rememberLazyListState()
        val windowWidth = with(LocalDensity.current) {
            LocalWindowInfo.current.containerSize.width.toDp()
        }
        var renderedBottomBarStyle by remember {
            mutableStateOf(settings.bottomBarStyle)
        }
        LaunchedEffect(settings.bottomBarStyle, currentRoute) {
            if (
                currentRoute == AppRoute.THEME_SETTINGS &&
                renderedBottomBarStyle != settings.bottomBarStyle
            ) {
                delay(280)
            }
            renderedBottomBarStyle = settings.bottomBarStyle
        }
        val miniPlayerUsesNormalChrome =
            windowWidth >= 600.dp ||
                resolveBottomBarStyle(
                    renderedBottomBarStyle,
                    liquidGlassSupported,
                ) == BottomBarStyle.NORMAL
        val rootIndexBottomSpacing = if (miniPlayerUsesNormalChrome) 12.dp else 6.dp
        val homeScrollBehavior = MiuixScrollBehavior()
        val songsScrollBehavior = MiuixScrollBehavior()
        val libraryScrollBehavior = MiuixScrollBehavior()
        val settingsScrollBehavior = MiuixScrollBehavior()
        val openTrackAlbum: (MusicTrack) -> Unit = { track ->
            uiState.albums.firstOrNull { album ->
                album.tracks.any { it.id == track.id }
            }?.let { album ->
                if (currentRoute != AppRoute.ALBUM_DETAIL || selectedAlbumKey != album.key) {
                    albumParentRoute = if (currentRoute == AppRoute.ARTIST_DETAIL) {
                        AppRoute.ARTIST_DETAIL
                    } else {
                        AppRoute.ROOT
                    }
                }
                selectedAlbumKey = album.key
                currentRoute = AppRoute.ALBUM_DETAIL
                if (playerTransition.targetOpen) closePlayer()
            }
        }
        val openTrackArtist: (ArtistGroup) -> Unit = { artist ->
            selectedArtistKey = artist.key
            currentRoute = AppRoute.ARTIST_DETAIL
            if (playerTransition.targetOpen) closePlayer()
        }
        val content: @Composable (PaddingValues) -> Unit = { outerPadding ->
            // Each page owns its top bar so the title moves with the Pager, like the Miuix demo.
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .overScrollHorizontal(),
                userScrollEnabled = rootPagerUserScrollEnabled(
                    selectedPage = playerPagerState.selectedPage,
                    homeRecommendationPage = homeRecommendationPage,
                ),
                verticalAlignment = Alignment.Top,
                overscrollEffect = null,
                // Precomposing a blurred page records another full-screen backdrop.
                beyondViewportPageCount = if (settings.blurEnabled) 0 else 1,
                key = { it },
            ) { page ->
                when (page) {
                    HOME_TAB_INDEX -> PlayerPage(
                        title = homeTitle,
                        outerPadding = outerPadding,
                        blurEnabled = settings.blurEnabled,
                        scrollBehavior = homeScrollBehavior,
                    ) { contentPadding, scrollBehavior, _ ->
                        HomeScreen(
                            tracks = uiState.tracks,
                            recommendations = homeRecommendations,
                            recentlyAddedTrackIds = uiState.recentlyAddedTrackIds,
                            scanStatus = uiState.scanStatus,
                            onTrackClick = viewModel::playTracks,
                            onRecommendationClick = viewModel::playHomeRecommendation,
                            onRecommendationPageChanged = { page -> homeRecommendationPage = page },
                            scrollBehavior = scrollBehavior,
                            listState = homeListState,
                            contentPadding = contentPadding,
                        )
                    }

                    SONGS_TAB_INDEX -> PlayerPage(
                        title = musicTitle,
                        outerPadding = outerPadding,
                        blurEnabled = settings.blurEnabled,
                        scrollBehavior = songsScrollBehavior,
                        actions = {
                            LibrarySearchButton(
                                visible = songSearchVisible,
                                onClick = {
                                    if (songSearchVisible) {
                                        songSearchVisible = false
                                        songSearchFocused = false
                                        songSearchQuery = ""
                                    } else {
                                        songSearchVisible = true
                                        songSearchFocused = true
                                    }
                                },
                            )
                            MusicSortButton(
                                config = musicSortConfig,
                                onConfigChange = { config ->
                                    val changed = config != musicSortConfig
                                    musicSortFieldOrdinal = config.field.ordinal
                                    musicSortDescending = config.descending
                                    viewModel.setMusicSortConfig(config)
                                    if (changed) {
                                        rootScope.launch {
                                            songsListState.scrollToItem(0)
                                            songsScrollBehavior.state.heightOffset = 0f
                                            songsScrollBehavior.state.contentOffset = 0f
                                        }
                                    }
                                },
                            )
                        },
                        bottomContent = {
                            LibrarySearchBar(
                                visible = songSearchVisible,
                                focused = songSearchFocused,
                                query = songSearchQuery,
                                label = stringResource(R.string.search_hint),
                                topPadding = expandedTopBarBottomContentGap(
                                    songsScrollBehavior,
                                    12.dp,
                                ),
                                onQueryChange = { songSearchQuery = it },
                                onFocusedChange = { songSearchFocused = it },
                                onVisibleChange = { visible ->
                                    songSearchVisible = visible
                                    if (!visible) {
                                        songSearchFocused = false
                                        songSearchQuery = ""
                                    }
                                },
                            )
                        },
                    ) { contentPadding, scrollBehavior, indexTopPadding ->
                        MusicListScreen(
                            onTrackClick = viewModel::playTracks,
                            displayedTracks = musicPresentation.items,
                            queueTracks = musicPresentation.queueItems,
                            sectionIndexMap = musicPresentation.sectionIndexMap,
                            scanStatus = uiState.scanStatus,
                            currentTrackId = currentTrackId,
                            query = songSearchQuery,
                            sortConfig = musicSortConfig,
                            onPlayNext = viewModel::playNext,
                            onAppendToQueue = viewModel::appendToQueue,
                            onGoToAlbum = openTrackAlbum,
                            artistGroups = uiState.artists,
                            onGoToArtist = openTrackArtist,
                            scrollBehavior = scrollBehavior,
                            indexTopPadding = indexTopPadding,
                            listState = songsListState,
                            contentPadding = contentPadding,
                            indexBottomSpacing = rootIndexBottomSpacing,
                        )
                    }

                    LIBRARY_TAB_INDEX -> PlayerPage(
                        title = libraryTitle,
                        outerPadding = outerPadding,
                        blurEnabled = settings.blurEnabled,
                        scrollBehavior = libraryScrollBehavior,
                        actions = {
                            LibrarySearchButton(
                                visible = librarySearchVisible,
                                contentDescription = when (libraryPagerState.currentPage) {
                                    LIBRARY_ARTISTS_TAB_INDEX ->
                                        stringResource(R.string.artist_search_hint)
                                    LIBRARY_FOLDERS_TAB_INDEX ->
                                        stringResource(R.string.folder_search_hint)
                                    else -> stringResource(R.string.album_search_hint)
                                },
                                onClick = {
                                    if (librarySearchVisible) {
                                        librarySearchVisible = false
                                        librarySearchFocused = false
                                        librarySearchQuery = ""
                                    } else {
                                        librarySearchVisible = true
                                        librarySearchFocused = true
                                    }
                                },
                            )
                            when (libraryPagerState.currentPage) {
                                LIBRARY_ARTISTS_TAB_INDEX -> ArtistSortButton(
                                    config = artistSortConfig,
                                    onConfigChange = { config ->
                                        val changed = config != artistSortConfig
                                        artistSortFieldOrdinal = config.field.ordinal
                                        artistSortDescending = config.descending
                                        viewModel.setArtistSortConfig(config)
                                        if (changed) {
                                            rootScope.launch {
                                                artistsListState.scrollToItem(0)
                                                libraryScrollBehavior.state.heightOffset = 0f
                                                libraryScrollBehavior.state.contentOffset = 0f
                                            }
                                        }
                                    },
                                )

                                LIBRARY_FOLDERS_TAB_INDEX -> FolderSortButton(
                                    config = folderSortConfig,
                                    onConfigChange = { config ->
                                        val changed = config != folderSortConfig
                                        folderSortFieldOrdinal = config.field.ordinal
                                        folderSortDescending = config.descending
                                        viewModel.setFolderSortConfig(config)
                                        if (changed) {
                                            rootScope.launch {
                                                foldersListState.scrollToItem(0)
                                                libraryScrollBehavior.state.heightOffset = 0f
                                                libraryScrollBehavior.state.contentOffset = 0f
                                            }
                                        }
                                    },
                                )

                                else -> AlbumSortButton(
                                    config = albumSortConfig,
                                    onConfigChange = { config ->
                                        val changed = config != albumSortConfig
                                        albumSortFieldOrdinal = config.field.ordinal
                                        albumSortDescending = config.descending
                                        albumGridStyleOrdinal = config.gridStyle.ordinal
                                        viewModel.setAlbumSortConfig(config)
                                        if (changed) {
                                            rootScope.launch {
                                                albumsGridState.scrollToItem(0)
                                                libraryScrollBehavior.state.heightOffset = 0f
                                                libraryScrollBehavior.state.contentOffset = 0f
                                            }
                                        }
                                    },
                                )
                            }
                        },
                        bottomContent = {
                            Column {
                                Box(
                                    modifier = Modifier.height(
                                        expandedTopBarBottomContentGap(
                                            libraryScrollBehavior,
                                            12.dp,
                                        ),
                                    ),
                                )
                                LibraryTabRow(
                                    tabs = libraryTabs,
                                    selectedTabIndex = libraryPagerState.currentPage,
                                    onTabSelected = { selectedTab ->
                                        rootScope.launch {
                                            libraryPagerState.scrollToPage(selectedTab)
                                        }
                                        viewModel.setLibraryTabIndex(selectedTab)
                                    },
                                    blurred = settings.blurEnabled,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp),
                                )
                                Box(modifier = Modifier.height(6.dp))
                                LibrarySearchBar(
                                    visible = librarySearchVisible,
                                    focused = librarySearchFocused,
                                    query = librarySearchQuery,
                                    label = stringResource(R.string.search_hint),
                                    topPadding = 6.dp,
                                    onQueryChange = { query -> librarySearchQuery = query },
                                    onFocusedChange = { librarySearchFocused = it },
                                    onVisibleChange = { visible ->
                                        librarySearchVisible = visible
                                        if (!visible) {
                                            librarySearchFocused = false
                                            librarySearchQuery = ""
                                        }
                                    },
                                )
                            }
                        },
                    ) { contentPadding, _, indexTopPadding ->
                        val layoutDirection = LocalLayoutDirection.current
                        val showLibraryScrollTop by remember {
                            derivedStateOf {
                                libraryScrollBehavior.state.collapsedFraction > 0.01f
                            }
                        }
                        val onLibraryIndexTargetChanged: (Int, Boolean) -> Unit =
                            { _, restoreLargeTitle ->
                                val state = libraryScrollBehavior.state
                                if (restoreLargeTitle) {
                                    state.heightOffset = 0f
                                    state.contentOffset = 0f
                                } else if (state.heightOffsetLimit != -Float.MAX_VALUE) {
                                    state.heightOffset = state.heightOffsetLimit
                                    state.contentOffset = state.heightOffsetLimit
                                }
                            }
                        Box(modifier = Modifier.fillMaxSize()) {
                            val libraryIndexModifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(
                                    top = indexTopPadding + 4.dp,
                                    end = contentPadding.calculateEndPadding(layoutDirection),
                                    bottom = contentPadding.calculateBottomPadding() +
                                        rootIndexBottomSpacing,
                                )
                                .fillMaxHeight()
                            HorizontalPager(
                                state = libraryPagerState,
                                modifier = Modifier.fillMaxSize(),
                                userScrollEnabled = false,
                                verticalAlignment = Alignment.Top,
                                beyondViewportPageCount = if (settings.blurEnabled) 0 else 1,
                                key = { it },
                            ) { libraryPage ->
                                when (libraryPage) {
                                    LIBRARY_ALBUMS_TAB_INDEX -> AlbumLibraryScreen(
                                        displayedAlbums = albumPresentation.items,
                                        sectionIndexMap = albumPresentation.sectionIndexMap,
                                        query = librarySearchQuery,
                                        sortConfig = albumSortConfig,
                                        onAlbumClick = { album ->
                                            dismissLibrarySearchFocus()
                                            selectedAlbumKey = album.key
                                            albumParentRoute = AppRoute.ROOT
                                            currentRoute = AppRoute.ALBUM_DETAIL
                                        },
                                        scrollBehavior = libraryScrollBehavior,
                                        indexTopPadding = indexTopPadding,
                                        gridState = albumsGridState,
                                        contentPadding = contentPadding,
                                        showIndex = false,
                                    )

                                    LIBRARY_ARTISTS_TAB_INDEX -> ArtistLibraryScreen(
                                        displayedArtists = artistPresentation.items,
                                        sectionIndexMap = artistPresentation.sectionIndexMap,
                                        query = librarySearchQuery,
                                        sortConfig = artistSortConfig,
                                        onArtistClick = { artist ->
                                            dismissLibrarySearchFocus()
                                            selectedArtistKey = artist.key
                                            currentRoute = AppRoute.ARTIST_DETAIL
                                        },
                                        scrollBehavior = libraryScrollBehavior,
                                        indexTopPadding = indexTopPadding,
                                        listState = artistsListState,
                                        contentPadding = contentPadding,
                                        showIndex = false,
                                    )

                                    LIBRARY_FOLDERS_TAB_INDEX -> FolderLibraryScreen(
                                        displayedFolders = folderPresentation.items,
                                        sectionIndexMap = folderPresentation.sectionIndexMap,
                                        query = librarySearchQuery,
                                        sortConfig = folderSortConfig,
                                        onFolderClick = { folder ->
                                            dismissLibrarySearchFocus()
                                            selectedFolderKey = folder.key
                                            currentRoute = AppRoute.FOLDER_DETAIL
                                        },
                                        scrollBehavior = libraryScrollBehavior,
                                        indexTopPadding = indexTopPadding,
                                        listState = foldersListState,
                                        contentPadding = contentPadding,
                                        showIndex = false,
                                    )
                                }
                            }

                            when (libraryPagerState.currentPage) {
                                LIBRARY_ALBUMS_TAB_INDEX -> if (
                                    albumPresentation.items.isNotEmpty() &&
                                    librarySearchQuery.isBlank() &&
                                    (
                                        albumSortConfig.field == AlbumSortField.ALBUM ||
                                            albumSortConfig.field ==
                                            AlbumSortField.ALBUM_ARTIST
                                    )
                                ) {
                                    AlphabetSideBar(
                                        sectionIndexMap = albumPresentation.sectionIndexMap,
                                        itemCount = albumPresentation.items.size,
                                        scrollStateKey = albumsGridState,
                                        isAtTarget = { index ->
                                            albumsGridState.firstVisibleItemIndex == index &&
                                                albumsGridState.firstVisibleItemScrollOffset == 0
                                        },
                                        scrollToItem = albumsGridState::scrollToItem,
                                        sections = if (albumSortConfig.descending) {
                                            AlphabetSections.asReversed()
                                        } else {
                                            AlphabetSections
                                        },
                                        showScrollTop = showLibraryScrollTop,
                                        onTargetIndexChanged = onLibraryIndexTargetChanged,
                                        modifier = libraryIndexModifier,
                                    )
                                }

                                LIBRARY_ARTISTS_TAB_INDEX -> if (
                                    artistPresentation.items.isNotEmpty() &&
                                    librarySearchQuery.isBlank() &&
                                    artistSortConfig.field == ArtistSortField.NAME
                                ) {
                                    AlphabetSideBar(
                                        sectionIndexMap = artistPresentation.sectionIndexMap,
                                        itemCount = artistPresentation.items.size,
                                        scrollStateKey = artistsListState,
                                        isAtTarget = { index ->
                                            artistsListState.firstVisibleItemIndex == index &&
                                                artistsListState.firstVisibleItemScrollOffset == 0
                                        },
                                        scrollToItem = artistsListState::scrollToItem,
                                        sections = if (artistSortConfig.descending) {
                                            AlphabetSections.asReversed()
                                        } else {
                                            AlphabetSections
                                        },
                                        showScrollTop = showLibraryScrollTop,
                                        onTargetIndexChanged = onLibraryIndexTargetChanged,
                                        modifier = libraryIndexModifier,
                                    )
                                }

                                LIBRARY_FOLDERS_TAB_INDEX -> if (
                                    folderPresentation.items.isNotEmpty() &&
                                    librarySearchQuery.isBlank() &&
                                    folderSortConfig.field == FolderSortField.NAME
                                ) {
                                    AlphabetSideBar(
                                        sectionIndexMap = folderPresentation.sectionIndexMap,
                                        itemCount = folderPresentation.items.size,
                                        scrollStateKey = foldersListState,
                                        isAtTarget = { index ->
                                            foldersListState.firstVisibleItemIndex == index &&
                                                foldersListState.firstVisibleItemScrollOffset == 0
                                        },
                                        scrollToItem = foldersListState::scrollToItem,
                                        sections = if (folderSortConfig.descending) {
                                            AlphabetSections.asReversed()
                                        } else {
                                            AlphabetSections
                                        },
                                        showScrollTop = showLibraryScrollTop,
                                        onTargetIndexChanged = onLibraryIndexTargetChanged,
                                        modifier = libraryIndexModifier,
                                    )
                                }
                            }
                        }
                    }

                    SETTINGS_TAB_INDEX -> PlayerPage(
                        title = settingsTitle,
                        outerPadding = outerPadding,
                        blurEnabled = settings.blurEnabled,
                        scrollBehavior = settingsScrollBehavior,
                    ) { contentPadding, scrollBehavior, _ ->
                        SettingsScreen(
                            scanStatus = uiState.scanStatus,
                            defaultHomePage = settings.defaultHomePage,
                            onDefaultHomePageChange = viewModel::setDefaultHomePage,
                            onOpenThemeSettings = {
                                currentRoute = AppRoute.THEME_SETTINGS
                            },
                            onOpenAbout = { currentRoute = AppRoute.ABOUT },
                            onScanClick = scanMusic,
                            scrollBehavior = scrollBehavior,
                            contentPadding = contentPadding,
                        )
                    }

                    else -> Unit
                }
            }
        }

        val navBackStack = remember(currentRoute, albumParentRoute) {
            when {
                currentRoute == AppRoute.ROOT -> listOf(AppRoute.ROOT)
                currentRoute == AppRoute.ALBUM_DETAIL &&
                    albumParentRoute == AppRoute.ARTIST_DETAIL -> listOf(
                    AppRoute.ROOT,
                    AppRoute.ARTIST_DETAIL,
                    AppRoute.ALBUM_DETAIL,
                )
                else -> listOf(AppRoute.ROOT, currentRoute)
            }
        }
        val navigateBack = {
            if (showQueue) {
                showQueue = false
            } else if (
                currentRoute == AppRoute.ALBUM_DETAIL &&
                albumParentRoute == AppRoute.ARTIST_DETAIL
            ) {
                currentRoute = AppRoute.ARTIST_DETAIL
            } else {
                currentRoute = AppRoute.ROOT
            }
        }
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            popupHost = {
                MiuixPopupHost()
            },
        ) { _ ->
            Box(modifier = Modifier.fillMaxSize()) {
                PlayerScaffold(
                            selectedTab = playerPagerState.selectedPage,
                            onTabSelected = playerPagerState::animateToPage,
                            showNavigation = currentRoute == AppRoute.ROOT,
                            bottomBarStyle = renderedBottomBarStyle,
                            liquidGlassSupported = liquidGlassSupported,
                            isDark = isDark,
                            blurEnabled = settings.blurEnabled,
                            backdropRefreshKey = currentRoute,
                            backdropPagingSignal = {
                                pagerState.currentPage +
                                    pagerState.currentPageOffsetFraction +
                                    libraryPagerState.currentPage +
                                    libraryPagerState.currentPageOffsetFraction
                            },
                            miniPlayer = { chrome ->
                                MiniPlayerHost(
                                    viewModel = viewModel,
                                    chrome = chrome,
                                    onOpen = openPlayer,
                                    onOpenQueue = { showQueue = true },
                                    onPlayerDragStart = playerTransition::beginDrag,
                                    onPlayerDrag = playerTransition::dragBy,
                                    onPlayerDragEnd = { velocityY ->
                                        rootScope.launch {
                                            playerTransition.endDrag(velocityY)
                                        }
                                    },
                                    onPlayerDragCancel = {
                                        rootScope.launch {
                                            playerTransition.cancelDrag()
                                        }
                                    },
                                    playerLayer = miniPlayerLayer,
                                    drawInPlace = !playerTransition.isMounted || !playerTransition.isReady,
                                    sharedArtworkVisible =
                                        !playerTransition.isReady ||
                                            !playerTransition.isTransitionActive,
                                    onPlayerBoundsChanged = playerTransition::updateMiniPlayerBounds,
                                    onArtworkBoundsChanged = playerTransition::updateMiniArtworkBounds,
                                )
                            },
                        ) { outerPadding ->
                            val layoutDirection = LocalLayoutDirection.current
                            val currentOuterPadding by rememberUpdatedState(outerPadding)
                            var retainedRootBottomPadding by remember(
                                miniPlayerUsesNormalChrome,
                            ) {
                                mutableStateOf(outerPadding.calculateBottomPadding())
                            }
                            if (currentRoute == AppRoute.ROOT) {
                                SideEffect {
                                    retainedRootBottomPadding = maxOf(
                                        retainedRootBottomPadding,
                                        outerPadding.calculateBottomPadding(),
                                    )
                                }
                            }
                            val rootPadding = PaddingValues(
                                start = outerPadding.calculateStartPadding(layoutDirection),
                                top = outerPadding.calculateTopPadding(),
                                end = outerPadding.calculateEndPadding(layoutDirection),
                                bottom = retainedRootBottomPadding,
                            )
                            val currentRootPadding by rememberUpdatedState(rootPadding)
                            Box(modifier = Modifier.fillMaxSize()) {
                                PredictiveNavDisplay(
                                    backStack = navBackStack,
                                    predictiveBackEnabled =
                                        settings.predictiveBackEnabled && !showQueue,
                                    onBack = navigateBack,
                                    modifier = Modifier.fillMaxSize(),
                                    entryProvider = { route ->
                                        NavEntry(route) {
                                            if (route == AppRoute.ROOT) {
                                                content(currentRootPadding)
                                            } else {
                                                val routeBottomPadding =
                                                    currentOuterPadding
                                                        .calculateBottomPadding()
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                ) {
                                                    when (route) {
                                                    AppRoute.THEME_SETTINGS ->
                                                        ThemeSettingsScreen(
                                                            settings = settings,
                                                            bottomContentPadding =
                                                                routeBottomPadding,
                                                            liquidGlassSupported =
                                                                liquidGlassSupported,
                                                            onBack = navigateBack,
                                                            onThemeModeChange =
                                                                viewModel::setThemeMode,
                                                            onDynamicColorChange =
                                                                viewModel::setDynamicColorEnabled,
                                                            onBlurChange =
                                                                viewModel::setBlurEnabled,
                                                            onFloatingBottomBarChange =
                                                                viewModel::setFloatingBottomBar,
                                                            onLiquidGlassChange =
                                                                viewModel::setLiquidGlass,
                                                            onPredictiveBackChange =
                                                                viewModel::setPredictiveBackEnabled,
                                                        )

                                                    AppRoute.ABOUT -> AboutScreen(
                                                        blurEnabled = settings.blurEnabled,
                                                        bottomContentPadding = routeBottomPadding,
                                                        onBack = navigateBack,
                                                    )

                                                    AppRoute.ALBUM_DETAIL -> {
                                                        val album = remember(
                                                            uiState.albums,
                                                            selectedAlbumKey,
                                                        ) {
                                                            uiState.albums.firstOrNull {
                                                                it.key == selectedAlbumKey
                                                            }
                                                        }
                                                        album?.let {
                                                            AlbumDetailScreen(
                                                                album = it,
                                                                artistGroups = uiState.artists,
                                                                currentTrackId = currentTrackId,
                                                                blurEnabled = settings.blurEnabled,
                                                                bottomContentPadding =
                                                                    routeBottomPadding,
                                                                onBack = navigateBack,
                                                                onTrackClick =
                                                                    viewModel::playTracks,
                                                                onPlayNext =
                                                                    viewModel::playNext,
                                                                onAppendToQueue =
                                                                    viewModel::appendToQueue,
                                                                onGoToAlbum = openTrackAlbum,
                                                                onGoToArtist = openTrackArtist,
                                                            )
                                                        }
                                                    }

                                                    AppRoute.ARTIST_DETAIL -> {
                                                        val artist = remember(
                                                            uiState.artists,
                                                            selectedArtistKey,
                                                        ) {
                                                            uiState.artists.firstOrNull {
                                                                it.key == selectedArtistKey
                                                            }
                                                        }
                                                        artist?.let {
                                                            ArtistDetailScreen(
                                                                artist = it,
                                                                artistGroups = uiState.artists,
                                                                currentTrackId = currentTrackId,
                                                                blurEnabled = settings.blurEnabled,
                                                                bottomContentPadding =
                                                                    routeBottomPadding,
                                                                albumGridStyle =
                                                                    albumSortConfig.gridStyle,
                                                                onBack = navigateBack,
                                                                onAlbumClick = { album ->
                                                                    selectedAlbumKey = album.key
                                                                    albumParentRoute =
                                                                        AppRoute.ARTIST_DETAIL
                                                                    currentRoute =
                                                                        AppRoute.ALBUM_DETAIL
                                                                },
                                                                onTrackClick =
                                                                    viewModel::playTracks,
                                                                onPlayNext =
                                                                    viewModel::playNext,
                                                                onAppendToQueue =
                                                                    viewModel::appendToQueue,
                                                                onGoToAlbum = openTrackAlbum,
                                                                onGoToArtist = openTrackArtist,
                                                            )
                                                        }
                                                    }

                                                    AppRoute.FOLDER_DETAIL -> {
                                                        val folder = remember(
                                                            uiState.folders,
                                                            selectedFolderKey,
                                                        ) {
                                                            uiState.folders.firstOrNull {
                                                                it.key == selectedFolderKey
                                                            }
                                                        }
                                                        folder?.let {
                                                            FolderDetailScreen(
                                                                folder = it,
                                                                artistGroups = uiState.artists,
                                                                currentTrackId = currentTrackId,
                                                                blurEnabled = settings.blurEnabled,
                                                                bottomContentPadding =
                                                                    routeBottomPadding,
                                                                onBack = navigateBack,
                                                                onTrackClick =
                                                                    viewModel::playTracks,
                                                                onPlayNext =
                                                                    viewModel::playNext,
                                                                onAppendToQueue =
                                                                    viewModel::appendToQueue,
                                                                onGoToAlbum = openTrackAlbum,
                                                                onGoToArtist = openTrackArtist,
                                                            )
                                                        }
                                                    }

                                                            AppRoute.ROOT -> Unit
                                                        }
                                                }
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    if (playerTransition.isMounted) {
                        FullPlayerHost(
                            viewModel = viewModel,
                            tracks = uiState.tracks,
                            artistGroups = uiState.artists,
                            isDark = isDark,
                            onDismiss = closePlayer,
                            onOpenQueue = { showQueue = true },
                            onGoToAlbum = openTrackAlbum,
                            onGoToArtist = openTrackArtist,
                            playerLayer = fullPlayerLayer,
                            drawInPlace =
                                playerTransition.targetOpen &&
                                    !playerTransition.isTransitionActive,
                            sharedArtworkVisible =
                                !playerTransition.isReady ||
                                    !playerTransition.isTransitionActive,
                            onPlayerDragStart = playerTransition::beginDrag,
                            onPlayerDrag = playerTransition::dragBy,
                            onPlayerDragEnd = { velocityY ->
                                rootScope.launch {
                                    playerTransition.endDrag(velocityY)
                                }
                            },
                            onPlayerDragCancel = {
                                rootScope.launch {
                                    playerTransition.cancelDrag()
                                }
                            },
                            onPlayerBoundsChanged = playerTransition::updateFullPlayerBounds,
                            onArtworkBoundsChanged = playerTransition::updateFullArtworkBounds,
                        )
                    }
                    PlayerSheetContentOverlay(
                        transition = playerTransition,
                        miniPlayerLayer = miniPlayerLayer,
                        fullPlayerLayer = fullPlayerLayer,
                        collapsedCornerRadius =
                            if (miniPlayerUsesNormalChrome) 18.dp else 32.dp,
                        floatingMiniPlayer = !miniPlayerUsesNormalChrome,
                        isDark = isDark,
                    )
                    PlayerSheetArtworkOverlay(
                        playback = compactPlayback,
                        transition = playerTransition,
                        collapsedCornerRadius = if (miniPlayerUsesNormalChrome) 7.dp else 8.dp,
                    )
                QueueSheetHost(
                    viewModel = viewModel,
                    show = showQueue,
                    onDismiss = { showQueue = false },
                )
            }
        }
    }
}

@Composable
private fun FullPlayerHost(
    viewModel: MeloxViewModel,
    tracks: List<MusicTrack>,
    artistGroups: List<ArtistGroup>,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onOpenQueue: () -> Unit,
    onGoToAlbum: (MusicTrack) -> Unit,
    onGoToArtist: (ArtistGroup) -> Unit,
    playerLayer: GraphicsLayer,
    drawInPlace: Boolean,
    sharedArtworkVisible: Boolean,
    onPlayerDragStart: () -> Unit,
    onPlayerDrag: (Float) -> Unit,
    onPlayerDragEnd: (Float) -> Unit,
    onPlayerDragCancel: () -> Unit,
    onPlayerBoundsChanged: (androidx.compose.ui.geometry.Rect) -> Unit,
    onArtworkBoundsChanged: (androidx.compose.ui.geometry.Rect) -> Unit,
) {
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()
    val lyrics by viewModel.lyricsState.collectAsStateWithLifecycle()
    val currentTrack = remember(tracks, playback.currentItem?.trackId) {
        val trackId = playback.currentItem?.trackId
        tracks.firstOrNull { it.id == trackId }
    }
    FullPlayerScreen(
        playback = playback,
        currentTrack = currentTrack,
        lyrics = lyrics,
        isDark = isDark,
        onDismiss = onDismiss,
        onTogglePlayPause = viewModel::togglePlayPause,
        onPrevious = viewModel::previous,
        onNext = viewModel::next,
        onSeek = viewModel::seekTo,
        onCyclePlaybackMode = viewModel::cyclePlaybackMode,
        onOpenQueue = onOpenQueue,
        onPlayNext = viewModel::playNext,
        onAppendToQueue = viewModel::appendToQueue,
        onGoToAlbum = onGoToAlbum,
        artistGroups = artistGroups,
        onGoToArtist = onGoToArtist,
        playerLayer = playerLayer,
        drawInPlace = drawInPlace,
        sharedArtworkVisible = sharedArtworkVisible,
        onPlayerDragStart = onPlayerDragStart,
        onPlayerDrag = onPlayerDrag,
        onPlayerDragEnd = onPlayerDragEnd,
        onPlayerDragCancel = onPlayerDragCancel,
        onPlayerBoundsChanged = onPlayerBoundsChanged,
        onArtworkBoundsChanged = onArtworkBoundsChanged,
    )
}

@Composable
private fun MiniPlayerHost(
    viewModel: MeloxViewModel,
    chrome: MiniPlayerChrome,
    onOpen: () -> Unit,
    onOpenQueue: () -> Unit,
    onPlayerDragStart: () -> Unit,
    onPlayerDrag: (Float) -> Unit,
    onPlayerDragEnd: (Float) -> Unit,
    onPlayerDragCancel: () -> Unit,
    playerLayer: GraphicsLayer,
    drawInPlace: Boolean,
    sharedArtworkVisible: Boolean,
    onPlayerBoundsChanged: (androidx.compose.ui.geometry.Rect) -> Unit,
    onArtworkBoundsChanged: (androidx.compose.ui.geometry.Rect) -> Unit,
) {
    val playback by viewModel.compactPlaybackState.collectAsStateWithLifecycle()
    MiniPlayer(
        playback = playback,
        chrome = chrome,
        onOpen = {
            if (playback.currentItem != null) onOpen()
        },
        onTogglePlayPause = viewModel::togglePlayPause,
        onPrevious = viewModel::previous,
        onNext = viewModel::next,
        onOpenQueue = onOpenQueue,
        onPlayerDragStart = onPlayerDragStart,
        onPlayerDrag = onPlayerDrag,
        onPlayerDragEnd = onPlayerDragEnd,
        onPlayerDragCancel = onPlayerDragCancel,
        playerLayer = playerLayer,
        drawInPlace = drawInPlace,
        sharedArtworkVisible = sharedArtworkVisible,
        onPlayerBoundsChanged = onPlayerBoundsChanged,
        onArtworkBoundsChanged = onArtworkBoundsChanged,
    )
}

@Composable
private fun PlaybackArtworkPrefetchEffect(
    viewModel: MeloxViewModel,
) {
    val playback by viewModel.compactPlaybackState.collectAsStateWithLifecycle()
    val applicationContext = LocalContext.current.applicationContext
    val artworkPrefetchSizePx = with(LocalDensity.current) {
        PLAYER_FULL_ARTWORK_REQUEST_SIZE.roundToPx()
    }
    LaunchedEffect(playback.currentIndex, playback.queue, artworkPrefetchSizePx) {
        if (playback.queue.isEmpty() || playback.currentIndex !in playback.queue.indices) {
            return@LaunchedEffect
        }
        listOf(
            (playback.currentIndex - 1 + playback.queue.size) % playback.queue.size,
            playback.currentIndex,
            (playback.currentIndex + 1) % playback.queue.size,
        ).distinct().forEach { index ->
            val item = playback.queue[index]
            prefetchArtwork(
                context = applicationContext,
                contentUri = item.contentUri,
                dateModifiedEpochSeconds = item.dateModifiedEpochSeconds,
                fileSizeBytes = item.fileSizeBytes,
                targetSizePx = artworkPrefetchSizePx,
            )
        }
    }
}

@Composable
private fun QueueSheetHost(
    viewModel: MeloxViewModel,
    show: Boolean,
    onDismiss: () -> Unit,
) {
    val playback by viewModel.compactPlaybackState.collectAsStateWithLifecycle()
    QueueSheet(
        show = show,
        playback = playback,
        onDismiss = onDismiss,
        onJumpTo = viewModel::jumpToQueueItem,
        onRemove = viewModel::removeQueueItem,
        onClear = viewModel::clearQueue,
    )
}

@Composable
internal fun LibrarySearchButton(
    visible: Boolean,
    contentDescription: String = stringResource(R.string.music_search_hint),
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        holdDownState = visible,
    ) {
        Icon(
            imageVector = MiuixIcons.Search,
            contentDescription = contentDescription,
        )
    }
}

@Composable
private fun LibraryTabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    blurred: Boolean = false,
) {
    Row(
        modifier = modifier
            .height(38.dp)
            .background(
                if (blurred) {
                    androidx.compose.ui.graphics.Color.Transparent
                } else {
                    MiuixTheme.colorScheme.surface
                },
            )
            .semantics {
                collectionInfo = CollectionInfo(rowCount = 1, columnCount = tabs.size)
            },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = selectedTabIndex == index
            val interactionSource = remember { MutableInteractionSource() }
            val outlineColor = MiuixTheme.colorScheme.outline
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (selected) {
                            Modifier.squircleBackground(
                                color = MiuixTheme.colorScheme.surfaceContainer,
                                cornerRadius = 12.dp,
                            )
                        } else {
                            Modifier.squircleBorder(
                                width = { 1.dp },
                                color = { outlineColor },
                                cornerRadius = 12.dp,
                            )
                        },
                    )
                    .selectable(
                        selected = selected,
                        onClick = {
                            if (!selected) onTabSelected(index)
                        },
                        role = Role.Tab,
                        interactionSource = interactionSource,
                        indication = null,
                    )
                    .semantics {
                        collectionItemInfo = CollectionItemInfo(
                            rowIndex = 0,
                            rowSpan = 1,
                            columnIndex = index,
                            columnSpan = 1,
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tab,
                    color = if (selected) {
                        MiuixTheme.colorScheme.onBackground
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                    },
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LibrarySearchBar(
    visible: Boolean,
    focused: Boolean,
    query: String,
    label: String,
    topPadding: Dp = 0.dp,
    onQueryChange: (String) -> Unit,
    onFocusedChange: (Boolean) -> Unit,
    onVisibleChange: (Boolean) -> Unit,
) {
    val clearSearchContentDescription = stringResource(R.string.search_clear)
    val imeVisible = WindowInsets.isImeVisible
    var imeWasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(focused, imeVisible) {
        if (!focused) {
            imeWasVisible = false
        } else if (shouldClearSearchFocusAfterImeDismissed(
                searchFocused = focused,
                imeVisible = imeVisible,
                imeWasVisible = imeWasVisible,
            )
        ) {
            imeWasVisible = false
            onFocusedChange(false)
        } else if (imeVisible) {
            imeWasVisible = true
        }
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(200)) +
            expandVertically(animationSpec = tween(250)),
        exit = fadeOut(animationSpec = tween(150)) +
            shrinkVertically(animationSpec = tween(200)),
    ) {
        SearchBar(
            inputField = {
                InputField(
                    query = query,
                    onQueryChange = { newQuery ->
                        // Miuix clears its query when `expanded` becomes false.
                        // A visible search field may deliberately remain unfocused
                        // while another root page is selected, so retain its query.
                        if (focused || newQuery.isNotEmpty() || query.isEmpty()) {
                            onQueryChange(newQuery)
                        }
                    },
                    onSearch = onQueryChange,
                    expanded = focused,
                    onExpandedChange = onFocusedChange,
                    label = label,
                    trailingIcon = {
                        AnimatedVisibility(
                            visible = query.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Box(
                                modifier = Modifier.padding(start = 8.dp, end = 16.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { onQueryChange("") },
                                    imageVector = MiuixIcons.Basic.SearchCleanup,
                                    tint = MiuixTheme.colorScheme.onSurfaceContainerHighest,
                                    contentDescription = clearSearchContentDescription,
                                )
                            }
                        }
                    },
                )
            },
            onExpandedChange = onFocusedChange,
            expanded = focused,
            insideMargin = DpSize(16.dp, 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding, bottom = 6.dp),
        ) {}
    }
}

internal fun shouldClearSearchFocusAfterImeDismissed(
    searchFocused: Boolean,
    imeVisible: Boolean,
    imeWasVisible: Boolean,
): Boolean = searchFocused && imeWasVisible && !imeVisible

@Composable
private fun expandedTopBarBottomContentGap(
    scrollBehavior: ScrollBehavior,
    expandedGap: Dp,
): Dp = expandedGap * (1f - scrollBehavior.state.collapsedFraction.coerceIn(0f, 1f))

@Composable
private fun PlayerPage(
    title: String,
    outerPadding: PaddingValues,
    blurEnabled: Boolean,
    scrollBehavior: ScrollBehavior,
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
    content: @Composable (PaddingValues, ScrollBehavior, indexTopPadding: androidx.compose.ui.unit.Dp) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val windowSize = LocalWindowInfo.current.containerSize
    val topBarBackdrop = rememberMiuixBlurBackdrop(enabled = blurEnabled)
    var bottomContentHeightPx by remember { mutableIntStateOf(0) }
    var bottomContentMeasured by remember { mutableStateOf(false) }
    var fixedExpandedBarPadding by remember(scrollBehavior, density, windowSize) {
        mutableStateOf<Dp?>(null)
    }

    Scaffold(
        topBar = {
            MiuixBlurredBar(topBarBackdrop) {
                TopAppBar(
                    title = title,
                    color = topBarBackdrop.miuixBarColor(),
                    actions = actions,
                    scrollBehavior = scrollBehavior,
                    bottomContent = {
                        Box(
                            modifier = Modifier.onSizeChanged {
                                bottomContentHeightPx = it.height
                                bottomContentMeasured = true
                            },
                        ) {
                            bottomContent()
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        val bottomContentHeight = with(density) { bottomContentHeightPx.toDp() }
        val currentBarPadding =
            (innerPadding.calculateTopPadding() - bottomContentHeight).coerceAtLeast(0.dp)
        val heightOffset = with(density) {
            scrollBehavior.state.heightOffset.toDp()
        }
        val measuredExpandedBarPadding =
            (currentBarPadding - heightOffset).coerceAtLeast(currentBarPadding)
        SideEffect {
            if (
                bottomContentMeasured &&
                fixedExpandedBarPadding == null &&
                scrollBehavior.state.heightOffsetLimit != -Float.MAX_VALUE
            ) {
                fixedExpandedBarPadding = measuredExpandedBarPadding
            }
        }
        val indexTopPadding =
            (fixedExpandedBarPadding ?: measuredExpandedBarPadding) + bottomContentHeight
        // The page bar owns top/system insets; the outer scaffold owns bottom-bar clearance.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(topBarBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
        ) {
            content(
                PaddingValues(
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    top = innerPadding.calculateTopPadding(),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                    bottom = outerPadding.calculateBottomPadding(),
                ),
                scrollBehavior,
                indexTopPadding,
            )
        }
    }
}

@SuppressLint("InlinedApi")
internal fun requiredAudioPermission(sdkInt: Int = Build.VERSION.SDK_INT): String =
    if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
