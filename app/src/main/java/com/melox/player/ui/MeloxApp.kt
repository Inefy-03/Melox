package com.melox.player.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.melox.player.R
import com.melox.player.data.library.AlbumGroup
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
import com.melox.player.model.normalizeCustomBackgroundDimPercent
import com.melox.player.model.DefaultHomePage
import com.melox.player.model.DynamicColorSource
import com.melox.player.model.PlaybackBackgroundStyle
import com.melox.player.model.ScanStatus
import com.melox.player.model.ThemeMode
import com.melox.player.model.MusicTrack
import com.melox.player.ui.component.BlurredBar
import com.melox.player.ui.component.LocalBottomSheetBlurBackdrop
import com.melox.player.ui.component.LocalTopBarBlurSettings
import com.melox.player.ui.component.LocalCustomPageBackground
import com.melox.player.ui.component.LocalCustomBackgroundDimAlpha
import com.melox.player.ui.component.LocalPageSurfaceBackdrop
import com.melox.player.ui.component.LocalPageCardBlurRadius
import com.melox.player.ui.component.pageCardBlurRadius
import com.melox.player.ui.component.LocalPageCardSurfaceAlpha
import com.melox.player.ui.component.pageCardSurfaceAlpha
import com.melox.player.ui.component.topBarContainerColor
import com.melox.player.ui.component.tabSelectedContainerColor
import com.melox.player.ui.component.FixedPageBackgroundHost
import com.melox.player.ui.component.pageTextureBlur
import com.melox.player.ui.component.refreshFixedWallpaperSample
import com.melox.player.ui.component.pageSurfaceBlur
import com.melox.player.ui.component.rememberCustomPageBackground
import com.melox.player.ui.component.customPageBackground
import com.melox.player.ui.component.customPageContainerColor
import com.melox.player.ui.component.PageScaffold
import com.melox.player.ui.component.TopBarBlurSettings
import com.melox.player.ui.component.miuixBarColor
import com.melox.player.ui.component.rememberBlurBackdrop
import com.melox.player.ui.component.library.MusicSortButton
import com.melox.player.ui.component.library.SelectionActionsAnimatedContent
import com.melox.player.ui.component.library.SelectionNavigationIconAnimatedContent
import com.melox.player.ui.component.library.TrackSelectionActions
import com.melox.player.ui.component.library.extractArtworkColor
import com.melox.player.ui.component.library.rememberArtworkBitmap
import com.melox.player.ui.component.library.AlphabetSections
import com.melox.player.ui.component.library.AlphabetSideBar
import com.melox.player.ui.component.library.AlbumSortButton
import com.melox.player.ui.component.library.ArtistSortButton
import com.melox.player.ui.component.library.FolderSortButton
import com.melox.player.ui.component.library.selectedItemsInDisplayedOrder
import com.melox.player.ui.component.library.toggleAllTrackSelection
import com.melox.player.ui.component.playlist.PlaylistNameDialog
import com.melox.player.ui.component.playlist.PlaylistPickerOverlay
import com.melox.player.ui.component.playback.playerNavigationOffset
import com.melox.player.ui.component.playback.MiniPlayer
import com.melox.player.ui.component.playback.DynamicFlowBackgroundState
import com.melox.player.ui.component.playback.PLAYER_FULL_ARTWORK_REQUEST_SIZE
import com.melox.player.ui.component.playback.PlayerSheetArtworkOverlay
import com.melox.player.ui.component.playback.PlayerSheetContentOverlay
import com.melox.player.ui.component.playback.PlayerSheetDragInputOverlay
import com.melox.player.ui.component.playback.PlayerSheetMiniControlsInputOverlay
import com.melox.player.ui.component.playback.sharedArtworkTargetIsOnscreen
import com.melox.player.ui.component.playback.playerSheetUsesFullPlayerStatusBar
import com.melox.player.ui.component.playback.playerSheetResidentHostTranslationY
import com.melox.player.ui.component.playback.prefetchPlaybackArtworkResource
import com.melox.player.ui.component.playback.prefetchPlaybackBackground
import com.melox.player.ui.component.playback.rememberDynamicFlowBackgroundState
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
import com.melox.player.ui.screen.home.homeInitialRecommendationCount
import com.melox.player.ui.screen.home.rememberHomeRecommendations
import com.melox.player.ui.screen.playback.FullPlayerScreen
import com.melox.player.ui.screen.playback.QueueSheet
import com.melox.player.ui.screen.playlist.PlaylistDetailScreen
import com.melox.player.ui.screen.playlist.PlaylistLibraryScreen
import com.melox.player.ui.screen.settings.SettingsScreen
import com.melox.player.ui.screen.settings.ScanMusicScreen
import com.melox.player.ui.screen.settings.MusicStatisticsScreen
import com.melox.player.ui.screen.settings.AboutScreen
import com.melox.player.ui.screen.settings.SponsorScreen
import com.melox.player.ui.screen.settings.ThemeSettingsScreen
import com.melox.player.ui.screen.settings.MainBackgroundScreen
import com.melox.player.ui.screen.settings.LyricsSettingsScreen
import com.melox.player.ui.screen.settings.LyricsInterfaceSettingsScreen
import com.melox.player.ui.screen.settings.BlockedFoldersScreen
import com.melox.player.ui.viewmodel.MeloxViewModel
import com.melox.player.ui.theme.MeloxTheme
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.NavigationRailDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.SearchCleanup
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.MiuixPopupHost
import top.yukonga.miuix.kmp.utils.overScrollHorizontal
import kotlin.math.abs
import kotlinx.serialization.Serializable
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
    homeRecommendationPageCount: Int,
    homeRecommendationGestureActive: Boolean,
): Boolean = selectedPage != HOME_TAB_INDEX ||
    !homeRecommendationGestureActive ||
    homeRecommendationPage <= 0 ||
    homeRecommendationPage >= homeRecommendationPageCount - 1

@Serializable
private enum class AppRoute {
    ROOT,
    SCAN_SETTINGS,
    MUSIC_STATISTICS,
    THEME_SETTINGS,
    MAIN_BACKGROUND,
    LYRICS_SETTINGS,
    LYRICS_INTERFACE,
    ABOUT,
    ALBUM_DETAIL,
    ARTIST_DETAIL,
    FOLDER_DETAIL,
    PLAYLISTS,
    PLAYLIST_DETAIL,
    BLOCKED_FOLDERS,
    SPONSOR,
}

@Serializable
private data class AppNavDestination(
    val route: AppRoute,
    val contentKey: String? = null,
    val depth: Int,
) : NavKey

// Keep miuix-nav identity tied to the stack slot so a return can swap the displayed
// detail object without changing a pop into a forward replacement transition.
private fun AppNavDestination.entryContentKey(): String =
    "melox:${route.name}:$depth"

private fun synchronizeAppNavBackStack(
    backStack: NavBackStack,
    destinations: List<AppNavDestination>,
) {
    var sharedCount = 0
    while (sharedCount < backStack.size && sharedCount < destinations.size) {
        val current = backStack[sharedCount] as? AppNavDestination ?: break
        val target = destinations[sharedCount]
        if (current.entryContentKey() != target.entryContentKey()) break
        if (current != target) backStack[sharedCount] = target
        sharedCount += 1
    }
    while (backStack.size > sharedCount) backStack.removeAt(backStack.lastIndex)
    for (index in sharedCount until destinations.size) backStack.add(destinations[index])
}

private enum class PermissionRequestSource {
    STARTUP,
    MANUAL_SCAN,
}

private data class PlaylistCreateRequest(
    val tracks: List<MusicTrack>,
    val openAfterCreate: Boolean,
    val detailParentRoute: AppRoute = AppRoute.ROOT,
)

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
    val playlistState by viewModel.playlistState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    val context = LocalContext.current
    val resources = LocalResources.current
    val windowSize = LocalWindowInfo.current.containerSize
    val playbackErrorText = stringResource(R.string.playback_error)
    val scanNoChangesText = stringResource(R.string.scan_no_changes)
    LaunchedEffect(compactPlayback.errorMessage) {
        if (compactPlayback.errorMessage != null) {
            Toast.makeText(
                context,
                playbackErrorText,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
    LaunchedEffect(viewModel, scanNoChangesText) {
        viewModel.scanNoChangesEvents.collectLatest {
            Toast.makeText(
                context,
                scanNoChangesText,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
    LaunchedEffect(viewModel, context) {
        viewModel.scanCompletionEvents.collectLatest { songCount ->
            Toast.makeText(
                context,
                context.resources.getQuantityString(
                    R.plurals.scan_complete,
                    songCount,
                    songCount,
                ),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
    val activity = remember(context) { context.findActivity() }
    val systemDark = isSystemInDarkTheme()
    val isDark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val dynamicFlowBackgroundState = rememberDynamicFlowBackgroundState()
    PlaybackArtworkPrefetchEffect(
        viewModel = viewModel,
        playbackBackgroundStyle = settings.playbackBackgroundStyle,
    )
    // API level alone is insufficient: liquid glass also needs RuntimeShader support at runtime.
    val liquidGlassSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        isRuntimeShaderSupported()
    val density = LocalDensity.current
    val windowWidth = with(density) { windowSize.width.toDp() }
    val windowHeight = with(density) { windowSize.height.toDp() }
    val landscape = isMiuixWideLayout(windowWidth, windowHeight)
    val requestedBottomBarStyle = resolveBottomBarStyle(
        settings.bottomBarStyle,
        liquidGlassSupported,
    )
    val recommendationViewportWidth = if (
        !settings.hideBottomBar &&
            shouldUseNavigationRail(
                windowWidth = windowWidth,
                windowHeight = windowHeight,
                effectiveStyle = requestedBottomBarStyle,
            )
    ) {
        windowWidth - if (windowWidth >= 1200.dp) {
            NavigationRailDefaults.ExpandedWidth
        } else {
            NavigationRailDefaults.MinWidth
        }
    } else {
        windowWidth
    }
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
    var homeRecommendationGestureActive by remember { mutableStateOf(false) }
    val libraryPagerState = rememberPagerState(pageCount = { LIBRARY_TAB_COUNT })
    val playerPagerState = rememberPlayerPagerState(pagerState)
    val homeRecommendations = rememberHomeRecommendations(
        tracks = uiState.tracks,
        active = playerPagerState.selectedPage == HOME_TAB_INDEX,
        initialRecommendationCount = homeInitialRecommendationCount(
            recommendationViewportWidth,
        ),
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
    var selectedSongUris by remember { mutableStateOf<Set<String>>(emptySet()) }
    var songsSelectionMode by remember { mutableStateOf(false) }
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
    var pendingAlbumGridReset by remember { mutableStateOf<AlbumGridStyle?>(null) }
    var showQueue by rememberSaveable { mutableStateOf(false) }
    val playerTransition = rememberPlayerSheetTransitionState()
    val sharedPlayerArtworkEnabled = playerTransition.fullPlayerArtworkPageSelected
    val miniPlayerLayer = rememberGraphicsLayer()
    val miniPlayerContentLayer = rememberGraphicsLayer()
    val fullPlayerBackgroundLayer = rememberGraphicsLayer()
    val fullPlayerContentLayer = rememberGraphicsLayer()
    SideEffect { playerTransition.updateWindowSize(windowSize) }
    val frameRecordingGeneration = playerTransition.currentFrameRecordingGeneration
    var miniPlayerChrome by remember { mutableStateOf<MiniPlayerChrome?>(null) }
    var currentRoute by rememberSaveable { mutableStateOf(AppRoute.ROOT) }
    var detailSelectionActive by remember { mutableStateOf(false) }
    var detailSelectionExitRequest by remember { mutableIntStateOf(0) }
    var selectedAlbumKey by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedArtistKey by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFolderKey by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPlaylistId by rememberSaveable { mutableStateOf<String?>(null) }
    var playlistParentRoute by rememberSaveable { mutableStateOf(AppRoute.ROOT) }
    var playlistPickerTracks by remember { mutableStateOf<List<MusicTrack>?>(null) }
    var playlistPickerOnAdded by remember { mutableStateOf<(() -> Unit)?>(null) }
    var playlistCreateRequest by remember { mutableStateOf<PlaylistCreateRequest?>(null) }
    var albumParentRoute by rememberSaveable { mutableStateOf(AppRoute.ROOT) }
    var albumParentArtistKey by rememberSaveable { mutableStateOf<String?>(null) }
    var artistParentRoute by rememberSaveable { mutableStateOf(AppRoute.ROOT) }
    var artistParentAlbumKey by rememberSaveable { mutableStateOf<String?>(null) }
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
    var playerStatusBarBackgroundIsDark by remember { mutableStateOf(isDark) }
    val playerUsesFullPlayerStatusBar = playerSheetUsesFullPlayerStatusBar(
        progress = playerTransition.progress,
    )
    val statusBarUsesDarkMode = if (playerUsesFullPlayerStatusBar) {
        playerStatusBarBackgroundIsDark
    } else {
        isDark
    }

    LaunchedEffect(currentTrackId, isDark) {
        playerStatusBarBackgroundIsDark = isDark
    }
    val hideStatusBarForPlayer = settings.hideStatusBar &&
        playerSheetUsesFullPlayerStatusBar(progress = playerTransition.progress)
    LaunchedEffect(hideStatusBarForPlayer) {
        val window = activity?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (hideStatusBarForPlayer) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.statusBars())
        } else {
            controller.show(WindowInsetsCompat.Type.statusBars())
        }
    }
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
    LaunchedEffect(pagerState.currentPage, currentRoute) {
        if (pagerState.currentPage != SONGS_TAB_INDEX || currentRoute != AppRoute.ROOT) {
            selectedSongUris = emptySet()
            songsSelectionMode = false
        }
    }
    LaunchedEffect(
        currentRoute,
        selectedAlbumKey,
        selectedArtistKey,
        selectedFolderKey,
        selectedPlaylistId,
    ) {
        detailSelectionActive = false
    }
    BackHandler(
        enabled = songsSelectionMode &&
            pagerState.currentPage == SONGS_TAB_INDEX &&
            currentRoute == AppRoute.ROOT,
    ) {
        selectedSongUris = emptySet()
        songsSelectionMode = false
    }
    LaunchedEffect(
        playerTransition.animationRequest,
        playerTransition.canSettle,
    ) {
        if (!playerTransition.canSettle || playerTransition.isDragging) {
            return@LaunchedEffect
        }
        withFrameNanos { }
        if (!playerTransition.canSettle || playerTransition.isDragging) {
            return@LaunchedEffect
        }
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

    val playbackArtworkColor = if (
        settings.dynamicColorEnabled &&
            settings.dynamicColorSource == DynamicColorSource.PLAYBACK_ARTWORK
    ) {
        compactPlayback.currentItem?.let { item ->
            rememberArtworkBitmap(
                contentUri = item.contentUri,
                dateModifiedEpochSeconds = item.dateModifiedEpochSeconds,
                fileSizeBytes = item.fileSizeBytes,
                size = 48.dp,
            )?.extractArtworkColor()
        }
    } else {
        null
    }

    MeloxTheme(
        settings = settings,
        playbackArtworkColor = playbackArtworkColor,
    ) {
        DisposableEffect(activity, isDark, statusBarUsesDarkMode) {
            val componentActivity = activity as? ComponentActivity
            val statusBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
            ) { statusBarUsesDarkMode }
            val navigationBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
            ) { isDark }
            componentActivity?.enableEdgeToEdge(
                statusBarStyle = statusBarStyle,
                navigationBarStyle = navigationBarStyle,
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
        val themeSettingsListState = rememberLazyListState()
        val mainBackgroundListState = rememberLazyListState()
        val lyricsSettingsListState = rememberLazyListState()
        val lyricsInterfaceListState = rememberLazyListState()
        var renderedBottomBarStyle by remember {
            mutableStateOf(settings.bottomBarStyle)
        }
        LaunchedEffect(settings.bottomBarStyle, currentRoute) {
            if (
                renderedBottomBarStyle != settings.bottomBarStyle &&
                (
                    currentRoute == AppRoute.THEME_SETTINGS ||
                        resolveBottomBarStyle(
                            renderedBottomBarStyle,
                            liquidGlassSupported,
                        ) == BottomBarStyle.NORMAL &&
                        requestedBottomBarStyle != BottomBarStyle.NORMAL ||
                        landscape &&
                            requestedBottomBarStyle == BottomBarStyle.NORMAL
                    )
            ) {
                delay(280)
            }
            renderedBottomBarStyle = settings.bottomBarStyle
        }
        val miniPlayerUsesNormalChrome = usesNormalMiniPlayerChrome(
            renderedBottomBarStyle = renderedBottomBarStyle,
            liquidGlassSupported = liquidGlassSupported,
        )
        val useSmallTopAppBar = landscape
        val rootIndexBottomSpacing = if (miniPlayerUsesNormalChrome) 12.dp else 6.dp
        val homeScrollBehavior = MiuixScrollBehavior()
        val songsScrollBehavior = MiuixScrollBehavior()
        val libraryScrollBehavior = MiuixScrollBehavior()
        val settingsScrollBehavior = MiuixScrollBehavior()
        val themeSettingsScrollBehavior = MiuixScrollBehavior()
        val mainBackgroundScrollBehavior = MiuixScrollBehavior()
        val lyricsSettingsScrollBehavior = MiuixScrollBehavior()
        val lyricsInterfaceScrollBehavior = MiuixScrollBehavior()
        LaunchedEffect(
            pendingAlbumGridReset,
            albumSortConfig.gridStyle,
        ) {
            val pendingGridStyle = pendingAlbumGridReset ?: return@LaunchedEffect
            if (albumSortConfig.gridStyle != pendingGridStyle) return@LaunchedEffect
            withFrameNanos { }
            albumsGridState.scrollToItem(0)
            libraryScrollBehavior.state.heightOffset = 0f
            libraryScrollBehavior.state.contentOffset = 0f
            pendingAlbumGridReset = null
        }
        val openPlaylistPicker: (List<MusicTrack>) -> Unit = { tracks ->
            if (playlistState.loaded && tracks.isNotEmpty()) {
                playlistPickerOnAdded = null
                playlistPickerTracks = tracks
            }
        }
        val openPlaylistPickerForSelection: (List<MusicTrack>, () -> Unit) -> Unit =
            { tracks, onAdded ->
                if (playlistState.loaded && tracks.isNotEmpty()) {
                    playlistPickerOnAdded = onAdded
                    playlistPickerTracks = tracks
                }
            }
        val openPlaylist: (String, AppRoute) -> Unit = { playlistId, parentRoute ->
            selectedPlaylistId = playlistId
            playlistParentRoute = parentRoute
            currentRoute = AppRoute.PLAYLIST_DETAIL
        }
        val returnToArtistParentAlbum: (String?) -> Unit = { targetAlbumKey ->
            artistParentAlbumKey?.let { parentAlbumKey ->
                selectedAlbumKey = targetAlbumKey ?: parentAlbumKey
                artistParentRoute = AppRoute.ROOT
                artistParentAlbumKey = null
                currentRoute = AppRoute.ALBUM_DETAIL
            }
        }
        val returnToAlbumParentArtist: (String?) -> Unit = { targetArtistKey ->
            albumParentArtistKey?.let { parentArtistKey ->
                selectedArtistKey = targetArtistKey ?: parentArtistKey
                albumParentRoute = AppRoute.ROOT
                albumParentArtistKey = null
                currentRoute = AppRoute.ARTIST_DETAIL
            }
        }
        val openAlbumFromArtist: (AlbumGroup) -> Unit = { album ->
            if (
                artistParentRoute == AppRoute.ALBUM_DETAIL &&
                    artistParentAlbumKey != null
            ) {
                returnToArtistParentAlbum(album.key)
            } else {
                selectedAlbumKey = album.key
                albumParentRoute = AppRoute.ARTIST_DETAIL
                albumParentArtistKey = selectedArtistKey
                currentRoute = AppRoute.ALBUM_DETAIL
            }
        }
        val openTrackAlbum: (MusicTrack) -> Unit = { track ->
            uiState.albums.firstOrNull { album ->
                album.tracks.any { it.id == track.id }
            }?.let { album ->
                when {
                    currentRoute == AppRoute.ARTIST_DETAIL -> openAlbumFromArtist(album)
                    currentRoute == AppRoute.ALBUM_DETAIL && selectedAlbumKey == album.key -> Unit
                    else -> {
                        selectedAlbumKey = album.key
                        albumParentRoute = AppRoute.ROOT
                        albumParentArtistKey = null
                        currentRoute = AppRoute.ALBUM_DETAIL
                    }
                }
                if (playerTransition.targetOpen) closePlayer()
            }
        }
        val openTrackArtist: (ArtistGroup) -> Unit = { artist ->
            if (
                currentRoute == AppRoute.ALBUM_DETAIL &&
                    albumParentRoute == AppRoute.ARTIST_DETAIL &&
                    albumParentArtistKey != null
            ) {
                returnToAlbumParentArtist(artist.key)
            } else {
                if (currentRoute != AppRoute.ARTIST_DETAIL || selectedArtistKey != artist.key) {
                    if (currentRoute == AppRoute.ALBUM_DETAIL) {
                        artistParentRoute = AppRoute.ALBUM_DETAIL
                        artistParentAlbumKey = selectedAlbumKey
                    } else {
                        artistParentRoute = AppRoute.ROOT
                        artistParentAlbumKey = null
                        albumParentRoute = AppRoute.ROOT
                        albumParentArtistKey = null
                    }
                }
                selectedArtistKey = artist.key
                currentRoute = AppRoute.ARTIST_DETAIL
            }
            if (playerTransition.targetOpen) closePlayer()
        }
        val content: @Composable (PaddingValues, Boolean) -> Unit =
            { outerPadding, navigationRailExpanded ->
            FixedPageBackgroundHost(
                modifier = Modifier.fillMaxSize(),
                refreshSignal = {
                    pagerState.currentPage + pagerState.currentPageOffsetFraction +
                        libraryPagerState.currentPage + libraryPagerState.currentPageOffsetFraction
                },
            ) {
            // Each page owns its top bar so the title moves with the Pager, like the Miuix demo.
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .overScrollHorizontal(),
                userScrollEnabled = rootPagerUserScrollEnabled(
                    selectedPage = playerPagerState.selectedPage,
                    homeRecommendationPage = homeRecommendationPage,
                    homeRecommendationPageCount = homeRecommendations?.tracks?.size ?: 0,
                    homeRecommendationGestureActive = homeRecommendationGestureActive,
                ),
                verticalAlignment = Alignment.Top,
                overscrollEffect = null,
                key = { it },
            ) { page ->
                when (page) {
                    HOME_TAB_INDEX -> PlayerPage(
                        title = homeTitle,
                        outerPadding = outerPadding,
                        scrollBehavior = homeScrollBehavior,
                        useSmallTopAppBar = useSmallTopAppBar,
                    ) { contentPadding, scrollBehavior, _ ->
                        HomeScreen(
                            tracks = uiState.tracks,
                            recommendations = homeRecommendations,
                            playlists = playlistState.playlists,
                            playlistsLoaded = playlistState.loaded,
                            scanStatus = uiState.scanStatus,
                            blurEnabled = settings.blurEnabled,
                            onOpenPlaylists = {
                                currentRoute = AppRoute.PLAYLISTS
                            },
                            onOpenScanSettings = {
                                currentRoute = AppRoute.SCAN_SETTINGS
                            },
                            onCreatePlaylist = {
                                if (playlistState.loaded) {
                                    playlistCreateRequest = PlaylistCreateRequest(
                                        tracks = emptyList(),
                                        openAfterCreate = true,
                                        detailParentRoute = AppRoute.ROOT,
                                    )
                                }
                            },
                            onPlaylistClick = { playlist ->
                                openPlaylist(playlist.id, AppRoute.ROOT)
                            },
                            onRecommendationClick = viewModel::playHomeRecommendation,
                            onRecommendationPageChanged = { page -> homeRecommendationPage = page },
                            onRecommendationGestureActiveChanged = { active ->
                                homeRecommendationGestureActive = active
                            },
                            scrollBehavior = scrollBehavior,
                            listState = homeListState,
                            landscape = landscape,
                            contentPadding = contentPadding,
                        )
                    }

                    SONGS_TAB_INDEX -> PlayerPage(
                        title = if (songsSelectionMode) {
                            if (selectedSongUris.isEmpty()) {
                                stringResource(R.string.selection_choose_songs)
                            } else {
                                stringResource(
                                    R.string.selection_selected_count,
                                    selectedSongUris.size,
                                )
                            }
                        } else {
                            musicTitle
                        },
                        outerPadding = outerPadding,
                        scrollBehavior = songsScrollBehavior,
                        useSmallTopAppBar = useSmallTopAppBar,
                        navigationIcon = {
                            SelectionNavigationIconAnimatedContent(
                                selectionMode = songsSelectionMode,
                                onCloseSelection = {
                                    selectedSongUris = emptySet()
                                    songsSelectionMode = false
                                },
                            )
                        },
                        actions = {
                            SelectionActionsAnimatedContent(
                                selectionMode = songsSelectionMode,
                                selectionActions = {
                                    val displayedKeys = musicPresentation.items.map(
                                        MusicTrack::contentUri,
                                    )
                                    TrackSelectionActions(
                                        allSelected = displayedKeys.isNotEmpty() &&
                                            selectedSongUris.containsAll(displayedKeys),
                                        actionEnabled = selectedSongUris.isNotEmpty(),
                                        onToggleAll = {
                                            selectedSongUris = toggleAllTrackSelection(
                                                selectedSongUris,
                                                displayedKeys,
                                            )
                                        },
                                        onAction = {
                                            val tracks = selectedItemsInDisplayedOrder(
                                                musicPresentation.items,
                                                selectedSongUris,
                                                MusicTrack::contentUri,
                                            )
                                            if (tracks.isNotEmpty()) {
                                                openPlaylistPickerForSelection(tracks) {
                                                    selectedSongUris = emptySet()
                                                    songsSelectionMode = false
                                                }
                                            }
                                        },
                                    )
                                },
                                defaultActions = {
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
                                        musicSortFieldOrdinal = config.field.ordinal
                                        musicSortDescending = config.descending
                                        viewModel.setMusicSortConfig(config)
                                    },
                                    )
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
                            query = musicPresentation.query,
                            sortConfig = musicPresentation.sortConfig,
                            onPlayNext = viewModel::playNext,
                            onAppendToQueue = viewModel::appendToQueue,
                            onAddToPlaylist = { track ->
                                openPlaylistPicker(listOf(track))
                            },
                            onGoToAlbum = openTrackAlbum,
                            artistGroups = uiState.artists,
                            onGoToArtist = openTrackArtist,
                            onExternalEditReturned =
                                viewModel::refreshTrackAfterExternalEdit,
                            showMusicTagEditor = settings.showMusicTagEditor,
                            showLyricoEditor = settings.showLyricoEditor,
                            scrollBehavior = scrollBehavior,
                            indexTopPadding = indexTopPadding,
                            listState = songsListState,
                            contentPadding = contentPadding,
                            indexBottomSpacing = rootIndexBottomSpacing,
                            selectionMode = songsSelectionMode,
                            selectedTrackUris = selectedSongUris,
                            onSelectionChange = { selectedSongUris = it },
                            onSelectionModeChange = { songsSelectionMode = it },
                        )
                    }

                    LIBRARY_TAB_INDEX -> PlayerPage(
                        title = libraryTitle,
                        outerPadding = outerPadding,
                        scrollBehavior = libraryScrollBehavior,
                        useSmallTopAppBar = useSmallTopAppBar,
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
                                        artistSortFieldOrdinal = config.field.ordinal
                                        artistSortDescending = config.descending
                                        viewModel.setArtistSortConfig(config)
                                    },
                                )

                                LIBRARY_FOLDERS_TAB_INDEX -> FolderSortButton(
                                    config = folderSortConfig,
                                    onConfigChange = { config ->
                                        folderSortFieldOrdinal = config.field.ordinal
                                        folderSortDescending = config.descending
                                        viewModel.setFolderSortConfig(config)
                                    },
                                )

                                else -> AlbumSortButton(
                                    config = albumSortConfig,
                                    onConfigChange = { config ->
                                        val gridStyleChanged =
                                            config.gridStyle != albumSortConfig.gridStyle
                                        albumSortFieldOrdinal = config.field.ordinal
                                        albumSortDescending = config.descending
                                        albumGridStyleOrdinal = config.gridStyle.ordinal
                                        viewModel.setAlbumSortConfig(config)
                                        if (gridStyleChanged) {
                                            pendingAlbumGridReset = config.gridStyle
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
                                    selectedTabIndex = libraryPagerState.targetPage,
                                    onTabSelected = { selectedTab ->
                                        rootScope.launch {
                                            libraryPagerState.animateScrollToPage(selectedTab)
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
                                key = { it },
                            ) { libraryPage ->
                                when (libraryPage) {
                                    LIBRARY_ALBUMS_TAB_INDEX -> AlbumLibraryScreen(
                                        displayedAlbums = albumPresentation.items,
                                        sectionIndexMap = albumPresentation.sectionIndexMap,
                                        query = albumPresentation.query,
                                        scanStatus = uiState.scanStatus,
                                        sortConfig = albumPresentation.sortConfig.copy(
                                            gridStyle = albumSortConfig.gridStyle,
                                        ),
                                        onAlbumClick = { album ->
                                            dismissLibrarySearchFocus()
                                            selectedAlbumKey = album.key
                                            albumParentRoute = AppRoute.ROOT
                                            albumParentArtistKey = null
                                            currentRoute = AppRoute.ALBUM_DETAIL
                                        },
                                        scrollBehavior = libraryScrollBehavior,
                                        indexTopPadding = indexTopPadding,
                                        gridState = albumsGridState,
                                        contentPadding = contentPadding,
                                        showIndex = false,
                                        landscape = landscape,
                                        navigationRailExpanded = navigationRailExpanded,
                                    )

                                    LIBRARY_ARTISTS_TAB_INDEX -> ArtistLibraryScreen(
                                        displayedArtists = artistPresentation.items,
                                        sectionIndexMap = artistPresentation.sectionIndexMap,
                                        query = artistPresentation.query,
                                        scanStatus = uiState.scanStatus,
                                        sortConfig = artistPresentation.sortConfig,
                                        onArtistClick = { artist ->
                                            dismissLibrarySearchFocus()
                                            openTrackArtist(artist)
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
                                        query = folderPresentation.query,
                                        scanStatus = uiState.scanStatus,
                                        sortConfig = folderPresentation.sortConfig,
                                        onFolderClick = { folder ->
                                            dismissLibrarySearchFocus()
                                            selectedFolderKey = folder.key
                                            currentRoute = AppRoute.FOLDER_DETAIL
                                        },
                                        onBlockFolder = { path ->
                                            viewModel.addBlockedFolderPath(path)
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
                        scrollBehavior = settingsScrollBehavior,
                        useSmallTopAppBar = useSmallTopAppBar,
                    ) { contentPadding, scrollBehavior, _ ->
                        SettingsScreen(
                            defaultHomePage = settings.defaultHomePage,
                            trackCount = uiState.tracks.size,
                            onDefaultHomePageChange = viewModel::setDefaultHomePage,
                            onOpenThemeSettings = {
                                themeSettingsListState.requestScrollToItem(0)
                                themeSettingsScrollBehavior.state.heightOffset = 0f
                                themeSettingsScrollBehavior.state.contentOffset = 0f
                                currentRoute = AppRoute.THEME_SETTINGS
                            },
                            onOpenLyricsSettings = {
                                currentRoute = AppRoute.LYRICS_SETTINGS
                            },
                            onOpenAbout = { currentRoute = AppRoute.ABOUT },
                            onOpenScanSettings = {
                                currentRoute = AppRoute.SCAN_SETTINGS
                            },
                            onOpenStatistics = {
                                currentRoute = AppRoute.MUSIC_STATISTICS
                            },
                            scrollBehavior = scrollBehavior,
                            contentPadding = contentPadding,
                        )
                    }

                    else -> Unit
                }
            }
            }
        }

        val currentRouteContentKey = when (currentRoute) {
            AppRoute.ALBUM_DETAIL -> selectedAlbumKey
            AppRoute.ARTIST_DETAIL -> selectedArtistKey
            AppRoute.FOLDER_DETAIL -> selectedFolderKey
            AppRoute.PLAYLIST_DETAIL -> selectedPlaylistId
            else -> null
        }
        val desiredNavBackStack = remember(
            currentRoute,
            currentRouteContentKey,
            albumParentRoute,
            albumParentArtistKey,
            artistParentRoute,
            artistParentAlbumKey,
            playlistParentRoute,
        ) {
            val root = AppNavDestination(AppRoute.ROOT, depth = 0)
            when {
                currentRoute == AppRoute.ROOT -> listOf(root)
                currentRoute == AppRoute.ALBUM_DETAIL &&
                    albumParentRoute == AppRoute.ARTIST_DETAIL &&
                    albumParentArtistKey != null -> listOf(
                    root,
                    AppNavDestination(
                        route = AppRoute.ARTIST_DETAIL,
                        contentKey = albumParentArtistKey,
                        depth = 1,
                    ),
                    AppNavDestination(
                        route = AppRoute.ALBUM_DETAIL,
                        contentKey = selectedAlbumKey,
                        depth = 2,
                    ),
                )
                currentRoute == AppRoute.ARTIST_DETAIL &&
                    artistParentRoute == AppRoute.ALBUM_DETAIL &&
                    artistParentAlbumKey != null -> buildList {
                    add(root)
                    if (
                        albumParentRoute == AppRoute.ARTIST_DETAIL &&
                            albumParentArtistKey != null
                    ) {
                        add(
                            AppNavDestination(
                                route = AppRoute.ARTIST_DETAIL,
                                contentKey = albumParentArtistKey,
                                depth = 1,
                            ),
                        )
                    }
                    add(
                        AppNavDestination(
                            route = AppRoute.ALBUM_DETAIL,
                            contentKey = artistParentAlbumKey,
                            depth = size,
                        ),
                    )
                    add(
                        AppNavDestination(
                            route = AppRoute.ARTIST_DETAIL,
                            contentKey = selectedArtistKey,
                            depth = size,
                        ),
                    )
                }
                currentRoute == AppRoute.BLOCKED_FOLDERS -> listOf(
                    root,
                    AppNavDestination(
                        route = AppRoute.SCAN_SETTINGS,
                        depth = 1,
                    ),
                    AppNavDestination(
                        route = AppRoute.BLOCKED_FOLDERS,
                        depth = 2,
                    ),
                )
                currentRoute == AppRoute.MAIN_BACKGROUND -> listOf(
                    root,
                    AppNavDestination(AppRoute.THEME_SETTINGS, depth = 1),
                    AppNavDestination(AppRoute.MAIN_BACKGROUND, depth = 2),
                )
                currentRoute == AppRoute.LYRICS_INTERFACE -> listOf(
                    root,
                    AppNavDestination(AppRoute.LYRICS_SETTINGS, depth = 1),
                    AppNavDestination(AppRoute.LYRICS_INTERFACE, depth = 2),
                )
                currentRoute == AppRoute.SPONSOR -> listOf(
                    root,
                    AppNavDestination(AppRoute.ABOUT, depth = 1),
                    AppNavDestination(AppRoute.SPONSOR, depth = 2),
                )
                currentRoute == AppRoute.PLAYLIST_DETAIL &&
                    playlistParentRoute == AppRoute.PLAYLISTS -> listOf(
                    root,
                    AppNavDestination(
                        route = AppRoute.PLAYLISTS,
                        depth = 1,
                    ),
                    AppNavDestination(
                        route = AppRoute.PLAYLIST_DETAIL,
                        contentKey = selectedPlaylistId,
                        depth = 2,
                    ),
                )
                else -> listOf(
                    root,
                    AppNavDestination(
                        route = currentRoute,
                        contentKey = currentRouteContentKey,
                        depth = 1,
                    ),
                )
            }
        }
        val navBackStack = rememberNavBackStack<AppNavDestination>(
            *desiredNavBackStack.toTypedArray(),
        )
        LaunchedEffect(desiredNavBackStack) {
            synchronizeAppNavBackStack(navBackStack, desiredNavBackStack)
        }
        val navigateBack = {
            if (detailSelectionActive) {
                detailSelectionExitRequest += 1
            } else if (showQueue) {
                showQueue = false
            } else if (
                currentRoute == AppRoute.ALBUM_DETAIL &&
                albumParentRoute == AppRoute.ARTIST_DETAIL &&
                albumParentArtistKey != null
            ) {
                returnToAlbumParentArtist(null)
            } else if (
                currentRoute == AppRoute.ARTIST_DETAIL &&
                artistParentRoute == AppRoute.ALBUM_DETAIL &&
                artistParentAlbumKey != null
            ) {
                returnToArtistParentAlbum(null)
            } else if (currentRoute == AppRoute.BLOCKED_FOLDERS) {
                currentRoute = AppRoute.SCAN_SETTINGS
            } else if (currentRoute == AppRoute.LYRICS_INTERFACE) {
                currentRoute = AppRoute.LYRICS_SETTINGS
            } else if (currentRoute == AppRoute.MAIN_BACKGROUND) {
                currentRoute = AppRoute.THEME_SETTINGS
            } else if (currentRoute == AppRoute.SPONSOR) {
                currentRoute = AppRoute.ABOUT
            } else if (
                currentRoute == AppRoute.PLAYLIST_DETAIL &&
                playlistParentRoute == AppRoute.PLAYLISTS
            ) {
                currentRoute = AppRoute.PLAYLISTS
            } else {
                currentRoute = AppRoute.ROOT
            }
        }
        val onNavigationTabSelected: (Int) -> Unit = { selectedTab ->
            while (navBackStack.size > 1) {
                navBackStack.removeAt(navBackStack.lastIndex)
            }
            currentRoute = AppRoute.ROOT
            playerPagerState.animateToPage(selectedTab)
        }
        var customBackgroundBlurPreview by remember(settings.customBackgroundId) {
            mutableStateOf<Int?>(null)
        }
        LaunchedEffect(settings.customBackgroundBlurPercent, customBackgroundBlurPreview) {
            if (customBackgroundBlurPreview == settings.customBackgroundBlurPercent) {
                customBackgroundBlurPreview = null
            }
        }
        val customPageBackground = rememberCustomPageBackground(settings, customBackgroundBlurPreview)
        var customBackgroundDimPreview by remember(settings.customBackgroundId) { mutableStateOf<Int?>(null) }
        LaunchedEffect(settings.customBackgroundDimPercent, customBackgroundDimPreview) {
            if (customBackgroundDimPreview == settings.customBackgroundDimPercent) customBackgroundDimPreview = null
        }
        var customBackgroundCardBlurPreview by remember(settings.customBackgroundId) { mutableStateOf<Int?>(null) }
        LaunchedEffect(settings.customBackgroundCardBlurPercent, customBackgroundCardBlurPreview) {
            if (customBackgroundCardBlurPreview == settings.customBackgroundCardBlurPercent) customBackgroundCardBlurPreview = null
        }
        var customBackgroundCardOpacityPreview by remember(settings.customBackgroundId) { mutableStateOf<Int?>(null) }
        LaunchedEffect(settings.customBackgroundCardOpacityPercent, customBackgroundCardOpacityPreview) {
            if (customBackgroundCardOpacityPreview == settings.customBackgroundCardOpacityPercent) customBackgroundCardOpacityPreview = null
        }
        CompositionLocalProvider(
            LocalCustomPageBackground provides customPageBackground,
            LocalCustomBackgroundDimAlpha provides
                (normalizeCustomBackgroundDimPercent(customBackgroundDimPreview ?: settings.customBackgroundDimPercent) / 100f),
            LocalPageCardBlurRadius provides pageCardBlurRadius(
                customBackgroundCardBlurPreview ?: settings.customBackgroundCardBlurPercent,
            ),
            LocalPageCardSurfaceAlpha provides pageCardSurfaceAlpha(
                customBackgroundCardOpacityPreview ?: settings.customBackgroundCardOpacityPercent,
            ),
            LocalTopBarBlurSettings provides TopBarBlurSettings(
                blurEnabled = settings.blurEnabled,
                progressiveEnabled = settings.progressiveTopBarBlurEnabled,
            ),
        ) {
            val bottomSheetBackdrop = rememberBlurBackdrop()
            // Overlay content is composed by the root popup host.
            CompositionLocalProvider(
                LocalBottomSheetBlurBackdrop provides bottomSheetBackdrop,
            ) {
                Scaffold(
                    modifier = Modifier.customPageBackground(),
                    containerColor = customPageContainerColor(),
                    popupHost = { MiuixPopupHost() },
                ) { _ ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(bottomSheetBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
                ) {
                    PlayerScaffold(
                            playerNavigationOffset = {
                                playerNavigationOffset(
                                    source = playerTransition.miniPlayerBounds,
                                    target = playerTransition.fullPlayerBounds,
                                    progress = playerTransition.progress,
                                )
                            },
                            selectedTab = playerPagerState.selectedPage,
                            onTabSelected = onNavigationTabSelected,
                            showNavigation = shouldShowNavigation(
                                currentRouteIsRoot = currentRoute == AppRoute.ROOT,
                                hideBottomBar = settings.hideBottomBar,
                                landscape = landscape,
                                requestedBottomBarStyle = requestedBottomBarStyle,
                                renderedBottomBarStyle = resolveBottomBarStyle(
                                    renderedBottomBarStyle,
                                    liquidGlassSupported,
                                ),
                            ),
                            showNavigationRailOnSecondary =
                                !settings.hideBottomBar &&
                                    landscape &&
                                    requestedBottomBarStyle == BottomBarStyle.NORMAL,
                            persistedNavigationRailExpanded = settings.navigationRailExpanded,
                            onNavigationRailExpandedChange = viewModel::setNavigationRailExpanded,
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
                                    onChromeChanged = { miniPlayerChrome = it },
                                    onOpen = openPlayer,
                                    onOpenQueue = { showQueue = true },
                                    onPlayerDragStart = playerTransition::beginMiniPlayerDrag,
                                    onPlayerDrag = playerTransition::dragBy,
                                    onPlayerDragEnd = playerTransition::endDrag,
                                    onPlayerDragCancel = playerTransition::cancelDrag,
                                    playerLayer = miniPlayerLayer,
                                    playerContentLayer = miniPlayerContentLayer,
                                    frameRecordingGeneration = frameRecordingGeneration,
                                    drawInPlace =
                                        !playerTransition.isMounted || !playerTransition.sharedLayersReady,
                                    surfaceVisible =
                                        !playerTransition.isMounted ||
                                            !playerTransition.sharedLayersReady,
                                    sharedArtworkVisible =
                                        !playerTransition.separateArtworkOverlayReady ||
                                            !playerTransition.isTransitionActive ||
                                            !sharedPlayerArtworkEnabled ||
                                            playerSheetArtworkIsOffscreen(playerTransition),
                                    onLayerRecorded = { generation, size ->
                                        playerTransition.markMiniFrameRecorded(
                                            windowSize,
                                            generation,
                                            size,
                                        )
                                    },
                                    onPlayerBoundsChanged = { playerTransition.updateMiniPlayerBounds(it, windowSize) },
                                    onPlayerContentBoundsChanged =
                                        { playerTransition.updateMiniPlayerContentBounds(it, windowSize) },
                                    onPlayerControlsBoundsChanged =
                                        { playerTransition.updateMiniPlayerControlsBounds(it, windowSize) },
                                    onArtworkBoundsChanged = { playerTransition.updateMiniArtworkBounds(it, windowSize) },
                                )
                            },
                    ) { outerPadding, navigationRailExpanded ->
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
                                    backEnabled = !playerTransition.isMounted,
                                    transitionStyle = settings.navigationTransitionStyle,
                                    isDark = isDark,
                                    onBack = navigateBack,
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    entry<AppNavDestination>(
                                        contentKey = AppNavDestination::entryContentKey,
                                    ) { route ->
                                            if (route.route == AppRoute.ROOT) {
                                                content(
                                                    currentRootPadding,
                                                    navigationRailExpanded,
                                                )
                                            } else {
                                                val routeBottomPadding =
                                                    currentOuterPadding
                                                        .calculateBottomPadding()
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                ) {
                                                    when (route.route) {
                                                    AppRoute.THEME_SETTINGS ->
                                                        ThemeSettingsScreen(
                                                            settings = settings,
                                                            bottomContentPadding =
                                                                routeBottomPadding,
                                                            liquidGlassSupported =
                                                                liquidGlassSupported,
                                                            listState = themeSettingsListState,
                                                            scrollBehavior =
                                                                themeSettingsScrollBehavior,
                                                            onBack = navigateBack,
                                                            onThemeModeChange =
                                                                viewModel::setThemeMode,
                                                            onDynamicColorChange =
                                                                viewModel::setDynamicColorEnabled,
                                                            onDynamicColorSourceChange =
                                                                viewModel::setDynamicColorSource,
                                                            onPlaybackBackgroundStyleChange =
                                                                viewModel::setPlaybackBackgroundStyle,
                                                            onBlurChange =
                                                                viewModel::setBlurEnabled,
                                                            onProgressiveTopBarBlurChange =
                                                                viewModel::setProgressiveTopBarBlurEnabled,
                                                            onHideBottomBarChange =
                                                                viewModel::setHideBottomBar,
                                                            onFloatingBottomBarChange =
                                                                viewModel::setFloatingBottomBar,
                                                            onLiquidGlassChange =
                                                                viewModel::setLiquidGlass,
                                                            onPredictiveBackChange =
                                                                viewModel::setPredictiveBackEnabled,
                                                            onNavigationTransitionStyleChange =
                                                                viewModel::setNavigationTransitionStyle,
                                                            onOpenMainBackground = {
                                                                currentRoute = AppRoute.MAIN_BACKGROUND
                                                            },
                                                        )

                                                    AppRoute.MAIN_BACKGROUND ->
                                                        MainBackgroundScreen(
                                                            settings = settings,
                                                            bottomContentPadding = routeBottomPadding,
                                                            listState = mainBackgroundListState,
                                                            scrollBehavior = mainBackgroundScrollBehavior,
                                                            onBack = navigateBack,
                                                            onImageChange = viewModel::setCustomBackground,
                                                            onDeleteImage = viewModel::deleteCustomBackground,
                                                            onDimPercentChange = {
                                                                customBackgroundDimPreview = it
                                                                viewModel.setCustomBackgroundDimPercent(it)
                                                            },
                                                            onBlurPercentChange = {
                                                                customBackgroundBlurPreview = it
                                                                viewModel.setCustomBackgroundBlurPercent(it)
                                                            },
                                                            onCardBlurPercentChange = {
                                                                customBackgroundCardBlurPreview = it
                                                                viewModel.setCustomBackgroundCardBlurPercent(it)
                                                            },
                                                            onCardOpacityPercentChange = {
                                                                customBackgroundCardOpacityPreview = it
                                                                viewModel.setCustomBackgroundCardOpacityPercent(it)
                                                            },
                                                        )

                                                    AppRoute.LYRICS_SETTINGS ->
                                                        LyricsSettingsScreen(
                                                            settings = settings,
                                                            bottomContentPadding =
                                                                routeBottomPadding,
                                                            listState = lyricsSettingsListState,
                                                            scrollBehavior =
                                                                lyricsSettingsScrollBehavior,
                                                            onBack = navigateBack,
                                                            onLyricsSourcePriorityChange =
                                                                viewModel::setLyricsSourcePriority,
                                                            onLyricsSidecarFormatPriorityChange =
                                                                viewModel::setLyricsSidecarFormatPriority,
                                                            onShowMusicTagEditorChange =
                                                                viewModel::setShowMusicTagEditor,
                                                            onShowLyricoEditorChange =
                                                                viewModel::setShowLyricoEditor,
                                                            onOpenLyricsInterface = {
                                                                currentRoute = AppRoute.LYRICS_INTERFACE
                                                            },
                                                        )

                                                    AppRoute.LYRICS_INTERFACE ->
                                                        LyricsInterfaceSettingsScreen(
                                                            settings = settings,
                                                            bottomContentPadding =
                                                                routeBottomPadding,
                                                            listState = lyricsInterfaceListState,
                                                            scrollBehavior =
                                                                lyricsInterfaceScrollBehavior,
                                                            onBack = navigateBack,
                                                            onLeftAlignPlayerTitleChange =
                                                                viewModel::setLeftAlignPlayerTitle,
                                                            onCenterLyricsChange =
                                                                viewModel::setCenterLyrics,
                                                            onShowLyricsTranslationChange =
                                                                viewModel::setShowLyricsTranslation,
                                                            onLyricFontScaleChange =
                                                                viewModel::setLyricFontScale,
                                                            onLyricFontWeightChange =
                                                                viewModel::setLyricFontWeight,
                                                            onLyricBlurEnabledChange =
                                                                viewModel::setLyricBlurEnabled,
                                                            onForceWordByWordLyricsChange =
                                                                viewModel::setForceWordByWordLyrics,
                                                            onHideControlsOnLyricsChange =
                                                                viewModel::setHideControlsOnLyrics,
                                                        )

                                                    AppRoute.SCAN_SETTINGS ->
                                                        ScanMusicScreen(
                                                            settings = settings,
                                                            scanStatus = uiState.scanStatus,
                                                            bottomContentPadding =
                                                                routeBottomPadding,
                                                            onBack = navigateBack,
                                                            onRefreshOnStartChange =
                                                                viewModel::setRefreshLibraryOnStart,
                                                            onSkipShortAudioChange =
                                                                viewModel::setSkipShortAudio,
                                                            onAddCustomFolder =
                                                                viewModel::addCustomFolderUri,
                                                            onRemoveCustomFolder =
                                                                viewModel::removeCustomFolderUri,
                                                            onOpenBlockedFolders = {
                                                                currentRoute = AppRoute.BLOCKED_FOLDERS
                                                            },
                                                            onStartScan = scanMusic,
                                                            onClearMusicLibrary =
                                                                viewModel::clearMusicLibrary,
                                                        )

                                                    AppRoute.BLOCKED_FOLDERS ->
                                                        BlockedFoldersScreen(
                                                            paths = settings.blockedFolderPaths,
                                                            bottomContentPadding = routeBottomPadding,
                                                            onBack = navigateBack,
                                                            onUnblock = viewModel::removeBlockedFolderPath,
                                                        )

                                                    AppRoute.MUSIC_STATISTICS ->
                                                        MusicStatisticsScreen(
                                                            tracks = uiState.tracks,
                                                            bottomContentPadding =
                                                                routeBottomPadding,
                                                            onBack = navigateBack,
                                                        )

                                                    AppRoute.ABOUT -> AboutScreen(
                                                        bottomContentPadding = routeBottomPadding,
                                                        onBack = navigateBack,
                                                        onOpenSponsor = {
                                                            currentRoute = AppRoute.SPONSOR
                                                        },
                                                    )

                                                    AppRoute.SPONSOR -> SponsorScreen(
                                                        bottomContentPadding = routeBottomPadding,
                                                        onBack = navigateBack,
                                                    )

                                                    AppRoute.ALBUM_DETAIL -> {
                                                        val album = remember(
                                                            uiState.albums,
                                                            route.contentKey,
                                                        ) {
                                                            uiState.albums.firstOrNull {
                                                                it.key == route.contentKey
                                                            }
                                                        }
                                                        album?.let {
                                                            AlbumDetailScreen(
                                                                album = it,
                                                                artistGroups = uiState.artists,
                                                                currentTrackId = currentTrackId,
                                                                bottomContentPadding =
                                                                    routeBottomPadding,
                                                                onBack = navigateBack,
                                                                selectionExitRequest =
                                                                    detailSelectionExitRequest,
                                                                onSelectionModeChange = { active ->
                                                                    if (
                                                                        currentRoute ==
                                                                        AppRoute.ALBUM_DETAIL &&
                                                                        selectedAlbumKey ==
                                                                        route.contentKey
                                                                    ) {
                                                                        detailSelectionActive = active
                                                                    }
                                                                },
                                                                onTrackClick =
                                                                    viewModel::playTracks,
                                                                onPlayNext =
                                                                    viewModel::playNext,
                                                                onAppendToQueue =
                                                                    viewModel::appendToQueue,
                                                                onAddToPlaylist = { track ->
                                                                    openPlaylistPicker(listOf(track))
                                                                },
                                                                onAddAllToPlaylist =
                                                                    openPlaylistPickerForSelection,
                                                                onGoToAlbum = openTrackAlbum,
                                                                onGoToArtist = openTrackArtist,
                                                                onExternalEditReturned =
                                                                    viewModel::refreshTrackAfterExternalEdit,
                                                                showMusicTagEditor = settings.showMusicTagEditor,
                                                                showLyricoEditor = settings.showLyricoEditor,
                                                            )
                                                        }
                                                    }

                                                    AppRoute.ARTIST_DETAIL -> {
                                                        val artist = remember(
                                                            uiState.artists,
                                                            route.contentKey,
                                                        ) {
                                                            uiState.artists.firstOrNull {
                                                                it.key == route.contentKey
                                                            }
                                                        }
                                                        artist?.let {
                                                            ArtistDetailScreen(
                                                                artist = it,
                                                                artistGroups = uiState.artists,
                                                                currentTrackId = currentTrackId,
                                                                bottomContentPadding =
                                                                    routeBottomPadding,
                                                                albumGridStyle =
                                                                    albumSortConfig.gridStyle,
                                                                onBack = navigateBack,
                                                                selectionExitRequest =
                                                                    detailSelectionExitRequest,
                                                                onSelectionModeChange = { active ->
                                                                    if (
                                                                        currentRoute ==
                                                                        AppRoute.ARTIST_DETAIL &&
                                                                        selectedArtistKey ==
                                                                        route.contentKey
                                                                    ) {
                                                                        detailSelectionActive = active
                                                                    }
                                                                },
                                                                onAlbumClick = openAlbumFromArtist,
                                                                onTrackClick =
                                                                    viewModel::playTracks,
                                                                onPlayNext =
                                                                    viewModel::playNext,
                                                                onAppendToQueue =
                                                                    viewModel::appendToQueue,
                                                                onAddToPlaylist = { track ->
                                                                    openPlaylistPicker(listOf(track))
                                                                },
                                                                onAddAllToPlaylist =
                                                                    openPlaylistPickerForSelection,
                                                                onGoToAlbum = openTrackAlbum,
                                                                onGoToArtist = openTrackArtist,
                                                                onExternalEditReturned =
                                                                    viewModel::refreshTrackAfterExternalEdit,
                                                                showMusicTagEditor = settings.showMusicTagEditor,
                                                                showLyricoEditor = settings.showLyricoEditor,
                                                            )
                                                        }
                                                    }

                                                    AppRoute.FOLDER_DETAIL -> {
                                                        val folder = remember(
                                                            uiState.folders,
                                                            route.contentKey,
                                                        ) {
                                                            uiState.folders.firstOrNull {
                                                                it.key == route.contentKey
                                                            }
                                                        }
                                                        folder?.let {
                                                            FolderDetailScreen(
                                                                folder = it,
                                                                artistGroups = uiState.artists,
                                                                currentTrackId = currentTrackId,
                                                                bottomContentPadding =
                                                                    routeBottomPadding,
                                                                onBack = navigateBack,
                                                                selectionExitRequest =
                                                                    detailSelectionExitRequest,
                                                                onSelectionModeChange = { active ->
                                                                    if (
                                                                        currentRoute ==
                                                                        AppRoute.FOLDER_DETAIL &&
                                                                        selectedFolderKey ==
                                                                        route.contentKey
                                                                    ) {
                                                                        detailSelectionActive = active
                                                                    }
                                                                },
                                                                onTrackClick =
                                                                    viewModel::playTracks,
                                                                onPlayNext =
                                                                    viewModel::playNext,
                                                                onAppendToQueue =
                                                                    viewModel::appendToQueue,
                                                                onAddToPlaylist = { track ->
                                                                    openPlaylistPicker(listOf(track))
                                                                },
                                                                onAddTracksToPlaylist =
                                                                    openPlaylistPickerForSelection,
                                                                onGoToAlbum = openTrackAlbum,
                                                                onGoToArtist = openTrackArtist,
                                                                onExternalEditReturned =
                                                                    viewModel::refreshTrackAfterExternalEdit,
                                                                showMusicTagEditor = settings.showMusicTagEditor,
                                                                showLyricoEditor = settings.showLyricoEditor,
                                                            )
                                                        }
                                                    }

                                                    AppRoute.PLAYLISTS ->
                                                        PlaylistLibraryScreen(
                                                            playlists = playlistState.playlists,
                                                            loaded = playlistState.loaded,
                                                            onMovePlaylists = viewModel::movePlaylists,
                                                            landscape = landscape,
                                                            bottomContentPadding =
                                                                routeBottomPadding,
                                                            onBack = navigateBack,
                                                            onCreatePlaylist = {
                                                                if (playlistState.loaded) {
                                                                    playlistCreateRequest =
                                                                        PlaylistCreateRequest(
                                                                            tracks = emptyList(),
                                                                            openAfterCreate = true,
                                                                            detailParentRoute =
                                                                                AppRoute.PLAYLISTS,
                                                                        )
                                                                }
                                                            },
                                                            onPlaylistClick = { playlist ->
                                                                openPlaylist(
                                                                    playlist.id,
                                                                    AppRoute.PLAYLISTS,
                                                                )
                                                            },
                                                        )

                                                    AppRoute.PLAYLIST_DETAIL -> {
                                                        val playlist = remember(
                                                            playlistState.playlists,
                                                            route.contentKey,
                                                        ) {
                                                            playlistState.playlists.firstOrNull {
                                                                it.id == route.contentKey
                                                            }
                                                        }
                                                        playlist?.let {
                                                            PlaylistDetailScreen(
                                                                playlist = it,
                                                                libraryTracks = uiState.tracks,
                                                                readableContentUris =
                                                                    playlistState.readableContentUris,
                                                                currentTrackId = currentTrackId,
                                                                artistGroups = uiState.artists,
                                                                bottomContentPadding =
                                                                    routeBottomPadding,
                                                                onBack = navigateBack,
                                                                selectionExitRequest =
                                                                    detailSelectionExitRequest,
                                                                onSelectionModeChange = { active ->
                                                                    if (
                                                                        currentRoute ==
                                                                        AppRoute.PLAYLIST_DETAIL &&
                                                                        selectedPlaylistId ==
                                                                        route.contentKey
                                                                    ) {
                                                                        detailSelectionActive = active
                                                                    }
                                                                },
                                                                onTrackClick = viewModel::playTracks,
                                                                onPlayNext = viewModel::playNext,
                                                                onAppendToQueue =
                                                                    viewModel::appendToQueue,
                                                                onAddToPlaylist = { track ->
                                                                    openPlaylistPicker(listOf(track))
                                                                },
                                                                onGoToAlbum = openTrackAlbum,
                                                                onGoToArtist = openTrackArtist,
                                                                onExternalEditReturned =
                                                                    viewModel::refreshTrackAfterExternalEdit,
                                                                showMusicTagEditor = settings.showMusicTagEditor,
                                                                showLyricoEditor = settings.showLyricoEditor,
                                                                onRename = { name ->
                                                                    viewModel.renamePlaylist(
                                                                        it.id,
                                                                        name,
                                                                    )
                                                                },
                                                                onDelete = {
                                                                    currentRoute =
                                                                        playlistParentRoute
                                                                    selectedPlaylistId = null
                                                                    viewModel.deletePlaylist(it.id)
                                                                },
                                                                onRemoveEntries = { entryIds ->
                                                                    viewModel.removePlaylistEntries(
                                                                        it.id,
                                                                        entryIds,
                                                                    )
                                                                },
                                                                onMoveEntry = { orderedEntryIds ->
                                                                    viewModel.movePlaylistEntry(
                                                                        it.id,
                                                                        orderedEntryIds,
                                                                    )
                                                                },
                                                            )
                                                        }
                                                    }

                                                            AppRoute.ROOT -> Unit
                                                        }
                                                }
                                            }
                                        }
                                    }
                                }
                        }
                    BackHandler(enabled = playerTransition.isMounted) {
                        closePlayer()
                    }
                    if (playerTransition.fullPlayerHostMounted) {
                        val fullPlayerRecordingActive =
                            playerTransition.sharedLayersReady &&
                            playerTransition.isTransitionActive
                        val fullPlayerHostTranslationY = playerSheetResidentHostTranslationY(
                            miniPlayerAcceptsInput = playerTransition.miniPlayerAcceptsInput,
                            windowHeight = windowSize.height,
                        ).takeUnless { fullPlayerRecordingActive } ?: 0f
                        FullPlayerHost(
                            viewModel = viewModel,
                            tracks = uiState.tracks,
                            artistGroups = uiState.artists,
                            playbackBackgroundStyle =
                                settings.playbackBackgroundStyle,
                            dynamicFlowBackgroundState = dynamicFlowBackgroundState,
                            lyricFontScale = settings.lyricFontScale,
                            lyricFontWeight = settings.lyricFontWeight,
                            forceWordByWordLyrics = settings.forceWordByWordLyrics,
                            lyricBlurEnabled = settings.lyricBlurEnabled,
                            centerLyrics = settings.centerLyrics,
                            leftAlignPlayerTitle = settings.leftAlignPlayerTitle,
                            hideControlsOnLyrics = settings.hideControlsOnLyrics,
                            hideStatusBar = settings.hideStatusBar,
                            showLyricsTranslation = settings.showLyricsTranslation,
                            showMusicTagEditor = settings.showMusicTagEditor,
                            showLyricoEditor = settings.showLyricoEditor,
                            onDismiss = closePlayer,
                            onOpenQueue = { showQueue = true },
                            onAddToPlaylist = { track ->
                                openPlaylistPicker(listOf(track))
                            },
                            onGoToAlbum = openTrackAlbum,
                            onGoToArtist = openTrackArtist,
                            backgroundLayer = fullPlayerBackgroundLayer,
                            contentLayer = fullPlayerContentLayer,
                            frameRecordingGeneration = frameRecordingGeneration,
                            interactionEnabled = playerTransition.fullPlayerAcceptsInput &&
                                playerTransition.progress > 0f,
                            blockUnderlyingInput = playerTransition.blocksUnderlyingInput,
                            lyricsPagingEnabled = playerTransition.isFullyExpanded,
                            drawInPlace = playerTransition.fullPlayerDrawsInPlace,
                            sharedArtworkVisible =
                                !playerTransition.separateArtworkOverlayReady ||
                                    !playerTransition.isTransitionActive ||
                                    !sharedPlayerArtworkEnabled,
                            initialArtworkPageSelected =
                                playerTransition.fullPlayerArtworkPageSelected,
                            onPlayerDragStart = playerTransition::beginFullPlayerDrag,
                            onPlayerDrag = playerTransition::dragBy,
                            onPlayerDragEnd = playerTransition::endDrag,
                            onPlayerDragCancel = playerTransition::cancelDrag,
                            onBackgroundLayerRecorded = { generation, size ->
                                playerTransition.markFullBackgroundFrameRecorded(
                                    windowSize,
                                    generation,
                                    size,
                                )
                            },
                            onContentLayerRecorded = { generation, size ->
                                playerTransition.markFullContentFrameRecorded(
                                    windowSize,
                                    generation,
                                    size,
                                )
                            },
                            onPlayerBoundsChanged = { bounds ->
                                playerTransition.updateFullPlayerBounds(
                                    androidx.compose.ui.geometry.Rect(
                                        left = bounds.left,
                                        top = bounds.top - fullPlayerHostTranslationY,
                                        right = bounds.right,
                                        bottom = bounds.bottom - fullPlayerHostTranslationY,
                                    ),
                                    windowSize,
                                )
                            },
                            onArtworkBoundsChanged = { bounds ->
                                playerTransition.updateFullArtworkBounds(
                                    androidx.compose.ui.geometry.Rect(
                                        left = bounds.left,
                                        top = bounds.top - fullPlayerHostTranslationY,
                                        right = bounds.right,
                                        bottom = bounds.bottom - fullPlayerHostTranslationY,
                                    ),
                                    windowSize,
                                )
                            },
                            onArtworkPageSelectedChanged =
                                playerTransition::updateFullPlayerArtworkPageSelected,
                            onStatusBarBackgroundDarkChanged = {
                                playerStatusBarBackgroundIsDark = it
                            },
                            modifier = Modifier
                                .zIndex(
                                    if (playerTransition.fullPlayerDrawsInPlace) 1f else -1f,
                                )
                                .graphicsLayer {
                                    // Hidden recording sources must not inherit the animated input clip.
                                    translationY = fullPlayerHostTranslationY
                                },
                        )
                    }
                    PlayerSheetDragInputOverlay(
                        transition = playerTransition,
                        hasItem = compactPlayback.currentItem != null,
                        modifier = Modifier.zIndex(2f),
                    )
                    PlayerSheetMiniControlsInputOverlay(
                        transition = playerTransition,
                        hasItem = compactPlayback.currentItem != null,
                        normalChrome = miniPlayerUsesNormalChrome,
                        onTogglePlayPause = viewModel::togglePlayPause,
                        onOpenQueue = { showQueue = true },
                        modifier = Modifier.zIndex(3f),
                    )
                    PlayerSheetContentOverlay(
                        transition = playerTransition,
                        miniPlayerContentLayer = miniPlayerContentLayer,
                        fullPlayerBackgroundLayer = fullPlayerBackgroundLayer,
                        fullPlayerContentLayer = fullPlayerContentLayer,
                        miniPlayerChrome = miniPlayerChrome,
                        collapsedCornerRadius =
                            if (miniPlayerUsesNormalChrome) 18.dp else 32.dp,
                        floatingMiniPlayer = !miniPlayerUsesNormalChrome,
                        isDark = isDark,
                    )
                    PlayerSheetArtworkOverlay(
                        playback = compactPlayback,
                        transition = playerTransition,
                        collapsedCornerRadius = if (miniPlayerUsesNormalChrome) 7.dp else 8.dp,
                        enabled = sharedPlayerArtworkEnabled,
                    )
                QueueSheetHost(
                    viewModel = viewModel,
                    show = showQueue,
                    onDismiss = { showQueue = false },
                )
                PlaylistPickerOverlay(
                    tracks = playlistPickerTracks,
                    playlists = playlistState.playlists,
                    onDismiss = {
                        playlistPickerTracks = null
                        playlistPickerOnAdded = null
                    },
                    onCreateRequestDismiss = {
                        playlistPickerTracks = null
                    },
                    onCreatePlaylist = { tracks ->
                        playlistCreateRequest = PlaylistCreateRequest(
                            tracks = tracks,
                            openAfterCreate = false,
                        )
                    },
                    onPlaylistSelected = { playlistId, tracks ->
                        val playlist = playlistState.playlists.firstOrNull { candidate ->
                            candidate.id == playlistId
                        }
                        if (
                            playlist != null &&
                            viewModel.addTracksToPlaylist(playlistId, tracks)
                        ) {
                            Toast.makeText(
                                context,
                                resources.getString(R.string.playlist_added_to, playlist.name),
                                Toast.LENGTH_SHORT,
                            ).show()
                            playlistPickerTracks = null
                            val onAdded = playlistPickerOnAdded
                            playlistPickerOnAdded = null
                            onAdded?.invoke()
                        }
                    },
                )
                val createRequest = playlistCreateRequest
                PlaylistNameDialog(
                    show = createRequest != null,
                    title = stringResource(R.string.playlist_create_title),
                    requestFocusOnShow = true,
                    onDismiss = {
                        val request = playlistCreateRequest
                        playlistCreateRequest = null
                        if (request != null && !request.openAfterCreate) {
                            playlistPickerTracks = request.tracks
                        }
                    },
                    onConfirm = createPlaylist@{ name ->
                        val request = playlistCreateRequest ?: return@createPlaylist
                        val playlistId = viewModel.createPlaylist(
                            name = name,
                            initialTracks = if (request.openAfterCreate) {
                                request.tracks
                            } else {
                                emptyList()
                            },
                        )
                        if (playlistId != null) {
                            playlistCreateRequest = null
                            if (request.openAfterCreate) {
                                openPlaylist(playlistId, request.detailParentRoute)
                            } else {
                                playlistPickerTracks = request.tracks
                            }
                        }
                    },
                )
            }
            }
        }
    }
}
}

@Composable
private fun FullPlayerHost(
    viewModel: MeloxViewModel,
    tracks: List<MusicTrack>,
    artistGroups: List<ArtistGroup>,
    playbackBackgroundStyle: PlaybackBackgroundStyle,
    dynamicFlowBackgroundState: DynamicFlowBackgroundState,
    lyricFontScale: Float,
    lyricFontWeight: Int,
    forceWordByWordLyrics: Boolean,
    lyricBlurEnabled: Boolean,
    centerLyrics: Boolean,
    leftAlignPlayerTitle: Boolean,
    hideControlsOnLyrics: Boolean,
    hideStatusBar: Boolean,
    showLyricsTranslation: Boolean,
    showMusicTagEditor: Boolean,
    showLyricoEditor: Boolean,
    onDismiss: () -> Unit,
    onOpenQueue: () -> Unit,
    onAddToPlaylist: (MusicTrack) -> Unit,
    onGoToAlbum: (MusicTrack) -> Unit,
    onGoToArtist: (ArtistGroup) -> Unit,
    backgroundLayer: GraphicsLayer,
    contentLayer: GraphicsLayer,
    frameRecordingGeneration: Int,
    interactionEnabled: Boolean,
    blockUnderlyingInput: Boolean,
    lyricsPagingEnabled: Boolean,
    drawInPlace: Boolean,
    sharedArtworkVisible: Boolean,
    initialArtworkPageSelected: Boolean,
    onPlayerDragStart: () -> Unit,
    onPlayerDrag: (Float) -> Unit,
    onPlayerDragEnd: (Float) -> Unit,
    onPlayerDragCancel: () -> Unit,
    onBackgroundLayerRecorded: (generation: Int, size: IntSize) -> Unit,
    onContentLayerRecorded: (generation: Int, size: IntSize) -> Unit,
    onPlayerBoundsChanged: (androidx.compose.ui.geometry.Rect) -> Unit,
    onArtworkBoundsChanged: (androidx.compose.ui.geometry.Rect) -> Unit,
    onArtworkPageSelectedChanged: (Boolean) -> Unit,
    onStatusBarBackgroundDarkChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()
    val lyrics by viewModel.lyricsState.collectAsStateWithLifecycle()
    val sleepTimerState by viewModel.sleepTimerState.collectAsStateWithLifecycle()
    val sleepTimerSelectionSeconds by viewModel.sleepTimerSelectionSeconds.collectAsStateWithLifecycle()
    val autoExtendSleepTimer by viewModel.autoExtendSleepTimer.collectAsStateWithLifecycle()
    val playbackPauseFade by viewModel.playbackPauseFade.collectAsStateWithLifecycle()
    val highPrecisionOutput by viewModel.highPrecisionOutput.collectAsStateWithLifecycle()
    val currentTrack = remember(tracks, playback.currentItem?.trackId) {
        val trackId = playback.currentItem?.trackId
        tracks.firstOrNull { it.id == trackId }
    }
    FullPlayerScreen(
        playback = playback,
        currentTrack = currentTrack,
        lyrics = lyrics,
        playbackBackgroundStyle = playbackBackgroundStyle,
        dynamicFlowBackgroundState = dynamicFlowBackgroundState,
        lyricFontScale = lyricFontScale,
        lyricFontWeight = lyricFontWeight,
        forceWordByWordLyrics = forceWordByWordLyrics,
        lyricBlurEnabled = lyricBlurEnabled,
        centerLyrics = centerLyrics,
        leftAlignPlayerTitle = leftAlignPlayerTitle,
        hideControlsOnLyrics = hideControlsOnLyrics,
        hideStatusBar = hideStatusBar,
        showLyricsTranslation = showLyricsTranslation,
        onLyricFontScaleChange = viewModel::setLyricFontScale,
        onLyricFontWeightChange = viewModel::setLyricFontWeight,
        onForceWordByWordLyricsChange = viewModel::setForceWordByWordLyrics,
        onLyricBlurEnabledChange = viewModel::setLyricBlurEnabled,
        onCenterLyricsChange = viewModel::setCenterLyrics,
        onLeftAlignPlayerTitleChange = viewModel::setLeftAlignPlayerTitle,
        onHideControlsOnLyricsChange = viewModel::setHideControlsOnLyrics,
        onHideStatusBarChange = viewModel::setHideStatusBar,
        onShowLyricsTranslationChange = viewModel::setShowLyricsTranslation,
        onDismiss = onDismiss,
        onTogglePlayPause = viewModel::togglePlayPause,
        onPrevious = viewModel::previous,
        onNext = viewModel::next,
        onSeek = viewModel::seekTo,
        onCyclePlaybackMode = viewModel::cyclePlaybackMode,
        onPlaybackSpeedChange = viewModel::setPlaybackSpeed,
        highPrecisionOutput = highPrecisionOutput,
        onHighPrecisionOutputChange = viewModel::setHighPrecisionOutput,
        sleepTimerState = sleepTimerState,
        sleepTimerSeconds = sleepTimerSelectionSeconds,
        onSleepTimerSecondsChange = viewModel::setSleepTimerSelectionSeconds,
        autoExtendSleepTimer = autoExtendSleepTimer,
        onAutoExtendSleepTimerChange = viewModel::setAutoExtendSleepTimer,
        playbackPauseFade = playbackPauseFade,
        onPlaybackPauseFadeChange = viewModel::setPlaybackPauseFade,
        onStartSleepTimer = viewModel::startSleepTimer,
        onCancelSleepTimer = viewModel::cancelSleepTimer,
        onAcknowledgeSleepTimerInterruption = viewModel::acknowledgeSleepTimerInterruption,
        onOpenQueue = onOpenQueue,
        onPlayNext = viewModel::playNext,
        onAppendToQueue = viewModel::appendToQueue,
        onAddToPlaylist = onAddToPlaylist,
        onGoToAlbum = onGoToAlbum,
        artistGroups = artistGroups,
        onGoToArtist = onGoToArtist,
        onExternalEditReturned = viewModel::refreshTrackAfterExternalEdit,
        showMusicTagEditor = showMusicTagEditor,
        showLyricoEditor = showLyricoEditor,
        backgroundLayer = backgroundLayer,
        contentLayer = contentLayer,
        frameRecordingGeneration = frameRecordingGeneration,
        interactionEnabled = interactionEnabled,
        blockUnderlyingInput = blockUnderlyingInput,
        lyricsPagingEnabled = lyricsPagingEnabled,
        drawInPlace = drawInPlace,
        sharedArtworkVisible = sharedArtworkVisible,
        initialArtworkPageSelected = initialArtworkPageSelected,
        onPlayerDragStart = onPlayerDragStart,
        onPlayerDrag = onPlayerDrag,
        onPlayerDragEnd = onPlayerDragEnd,
        onPlayerDragCancel = onPlayerDragCancel,
        onBackgroundLayerRecorded = onBackgroundLayerRecorded,
        onContentLayerRecorded = onContentLayerRecorded,
        onPlayerBoundsChanged = onPlayerBoundsChanged,
        onArtworkBoundsChanged = onArtworkBoundsChanged,
        onArtworkPageSelectedChanged = onArtworkPageSelectedChanged,
        onStatusBarBackgroundDarkChanged = onStatusBarBackgroundDarkChanged,
        modifier = modifier,
    )
}

@Composable
private fun MiniPlayerHost(
    viewModel: MeloxViewModel,
    chrome: MiniPlayerChrome,
    onChromeChanged: (MiniPlayerChrome) -> Unit,
    onOpen: () -> Unit,
    onOpenQueue: () -> Unit,
    onPlayerDragStart: () -> Unit,
    onPlayerDrag: (Float) -> Unit,
    onPlayerDragEnd: (Float) -> Unit,
    onPlayerDragCancel: () -> Unit,
    playerLayer: GraphicsLayer,
    playerContentLayer: GraphicsLayer,
    frameRecordingGeneration: Int,
    drawInPlace: Boolean,
    surfaceVisible: Boolean,
    sharedArtworkVisible: Boolean,
    onLayerRecorded: (generation: Int, size: IntSize) -> Unit,
    onPlayerBoundsChanged: (androidx.compose.ui.geometry.Rect) -> Unit,
    onPlayerContentBoundsChanged: (androidx.compose.ui.geometry.Rect) -> Unit,
    onPlayerControlsBoundsChanged: (androidx.compose.ui.geometry.Rect) -> Unit,
    onArtworkBoundsChanged: (androidx.compose.ui.geometry.Rect) -> Unit,
) {
    val playback by viewModel.compactPlaybackState.collectAsStateWithLifecycle()
    SideEffect {
        onChromeChanged(chrome)
    }
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
        playerContentLayer = playerContentLayer,
        frameRecordingGeneration = frameRecordingGeneration,
        drawInPlace = drawInPlace,
        surfaceVisible = surfaceVisible,
        sharedArtworkVisible = sharedArtworkVisible,
        onLayerRecorded = onLayerRecorded,
        onPlayerBoundsChanged = onPlayerBoundsChanged,
        onPlayerContentBoundsChanged = onPlayerContentBoundsChanged,
        onPlayerControlsBoundsChanged = onPlayerControlsBoundsChanged,
        onArtworkBoundsChanged = onArtworkBoundsChanged,
    )
}

private fun playerSheetArtworkIsOffscreen(
    transition: com.melox.player.ui.component.playback.PlayerSheetTransitionState,
): Boolean = transition.separateArtworkOverlayReady &&
    !sharedArtworkTargetIsOnscreen(
        artworkBounds = transition.fullArtworkBounds,
        viewportBounds = transition.fullPlayerBounds,
    )

@Composable
private fun PlaybackArtworkPrefetchEffect(
    viewModel: MeloxViewModel,
    playbackBackgroundStyle: PlaybackBackgroundStyle,
) {
    val playback by viewModel.compactPlaybackState.collectAsStateWithLifecycle()
    val applicationContext = LocalContext.current.applicationContext
    val artworkPrefetchSizePx = with(LocalDensity.current) {
        PLAYER_FULL_ARTWORK_REQUEST_SIZE.roundToPx()
    }
    val placeholderArtworkResId = if (MiuixTheme.colorScheme.surface.luminance() < 0.5f) {
        R.drawable.ic_album_placeholder_dark
    } else {
        R.drawable.ic_album_placeholder_light
    }
    LaunchedEffect(
        playback.currentIndex,
        playback.queue,
        artworkPrefetchSizePx,
        placeholderArtworkResId,
        playbackBackgroundStyle,
    ) {
        if (playback.queue.isEmpty() || playback.currentIndex !in playback.queue.indices) {
            return@LaunchedEffect
        }
        listOf(
            playback.currentIndex,
            (playback.currentIndex + 1) % playback.queue.size,
            (playback.currentIndex - 1 + playback.queue.size) % playback.queue.size,
        ).distinct().forEach { index ->
            val item = playback.queue[index]
            if (index == playback.currentIndex &&
                playbackBackgroundStyle == PlaybackBackgroundStyle.BLURRED_ARTWORK
            ) {
                prefetchPlaybackBackground(
                    context = applicationContext,
                    contentUri = item.contentUri,
                    dateModifiedEpochSeconds = item.dateModifiedEpochSeconds,
                    fileSizeBytes = item.fileSizeBytes,
                    placeholderArtworkResId = placeholderArtworkResId,
                )
            }
            prefetchPlaybackArtworkResource(
                context = applicationContext,
                contentUri = item.contentUri,
                dateModifiedEpochSeconds = item.dateModifiedEpochSeconds,
                fileSizeBytes = item.fileSizeBytes,
                targetSizePx = artworkPrefetchSizePx,
                placeholderArtworkResId = placeholderArtworkResId,
                includeBlurredArtwork =
                    playbackBackgroundStyle == PlaybackBackgroundStyle.BLURRED_ARTWORK,
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
        onMove = viewModel::moveQueueItem,
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
    val pageSurfaceBlurActive = LocalPageSurfaceBackdrop.current != null
    val hasWallpaper = LocalCustomPageBackground.current != null
    val progressiveBlurActive = blurred &&
        LocalTopBarBlurSettings.current.progressiveEnabled &&
        isRuntimeShaderSupported()
    Row(
        modifier = modifier
            .height(38.dp)
            .background(
                topBarContainerColor(hasWallpaper, blurred, MiuixTheme.colorScheme.surface),
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
                            Modifier.pageSurfaceBlur(12.dp).squircleBackground(
                                color = if (pageSurfaceBlurActive) androidx.compose.ui.graphics.Color.Transparent else tabSelectedContainerColor(
                                    hasWallpaper, progressiveBlurActive, MiuixTheme.colorScheme.surfaceContainer,
                                ),
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
    val pageSurfaceBackdrop = LocalPageSurfaceBackdrop.current
    val topBarBlurSettings = LocalTopBarBlurSettings.current
    val progressiveBlurActive = topBarBlurSettings.blurEnabled &&
        topBarBlurSettings.progressiveEnabled &&
        isRuntimeShaderSupported()
    val clearSearchContentDescription = stringResource(R.string.search_clear)
    val imeVisible = WindowInsets.isImeVisible
    val searchFocusManager = LocalFocusManager.current
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
            searchFocusManager.clearFocus(force = true)
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
        val handleExpandedChange: (Boolean) -> Unit = { expanded ->
            if (expanded) {
                onFocusedChange(true)
            } else {
                onVisibleChange(false)
            }
        }
        SearchBar(
            inputField = {
                InputField(
                    modifier = pageSurfaceBackdrop?.let {
                        Modifier.pageTextureBlur(
                            backdrop = it,
                            shape = RoundedCornerShape(percent = 50),
                        )
                    } ?: Modifier,
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
                    expanded = visible,
                    onExpandedChange = handleExpandedChange,
                    label = label,
                    color = if (pageSurfaceBackdrop != null) androidx.compose.ui.graphics.Color.Transparent else MiuixTheme.colorScheme.surfaceContainerHigh.copy(
                        alpha = if (progressiveBlurActive) 0.8f else 1f,
                    ),
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
            onExpandedChange = handleExpandedChange,
            expanded = visible,
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
    scrollBehavior: ScrollBehavior,
    useSmallTopAppBar: Boolean,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
    content: @Composable (PaddingValues, ScrollBehavior, indexTopPadding: androidx.compose.ui.unit.Dp) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val windowSize = LocalWindowInfo.current.containerSize
    var topBarCaptureCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val topBarBackdrop = rememberBlurBackdrop(captureCoordinates = { topBarCaptureCoordinates })
    var bottomContentHeightPx by remember { mutableIntStateOf(0) }
    var bottomContentMeasured by remember { mutableStateOf(false) }
    var fixedExpandedBarPadding by remember(scrollBehavior, density, windowSize, useSmallTopAppBar) {
        mutableStateOf<Dp?>(null)
    }

    PageScaffold(
            topBar = {
            BlurredBar(
                backdrop = topBarBackdrop,
                blurEnabled = topBarBackdrop != null,
                scrollBehavior = scrollBehavior,
            ) {
                val measuredBottomContent: @Composable () -> Unit = {
                    Box(
                        modifier = Modifier.onSizeChanged {
                            bottomContentHeightPx = it.height
                            bottomContentMeasured = true
                        },
                    ) {
                        bottomContent()
                    }
                }
                if (useSmallTopAppBar) {
                    SmallTopAppBar(
                        title = title,
                        color = topBarBackdrop.miuixBarColor(),
                        navigationIcon = navigationIcon,
                        actions = actions,
                        scrollBehavior = scrollBehavior,
                        bottomContent = measuredBottomContent,
                    )
                } else {
                    TopAppBar(
                        title = title,
                        color = topBarBackdrop.miuixBarColor(),
                        navigationIcon = navigationIcon,
                        actions = actions,
                        scrollBehavior = scrollBehavior,
                        bottomContent = measuredBottomContent,
                    )
                }
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
                .onGloballyPositioned { topBarCaptureCoordinates = it }
                .refreshFixedWallpaperSample()
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
