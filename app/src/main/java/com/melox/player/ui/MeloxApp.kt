package com.melox.player.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateTo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
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
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.selection.selectable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.semantics.clearAndSetSemantics
import top.yukonga.miuix.kmp.glass.popupShadowOutsets
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.melox.player.model.LyricsTranslationLanguage
import com.melox.player.model.LyricAnimationMode
import com.melox.player.model.isDuetForMedia
import com.melox.player.R
import com.melox.player.ui.component.os4.*
import com.melox.player.ui.component.library.PLACEHOLDER_ARTWORK_RES_ID
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
import com.melox.player.data.library.INTERNAL_STORAGE_ROOT
import com.melox.player.data.library.internalStorageDirectoryTrail
import com.melox.player.ui.component.library.offsetSectionIndices
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.basic.BreadcrumbItem
import com.melox.player.data.library.FolderSortField
import com.melox.player.ui.viewmodel.withHiddenFolders
import com.melox.player.model.BottomBarStyle
import com.melox.player.model.normalizeCustomBackgroundDimPercent
import com.melox.player.model.DefaultHomePage
import com.melox.player.model.DynamicColorSource
import com.melox.player.model.PlaybackBackgroundStyle
import com.melox.player.model.usesDarkPlaybackBackground
import com.melox.player.model.PlaybackQueueItem
import com.melox.player.model.PlaybackUiState
import com.melox.player.model.ScanStatus
import com.melox.player.model.MusicTrack
import com.melox.player.ui.component.BlurredBar
import com.melox.player.ui.component.animation.InteractiveHighlight
import com.melox.player.ui.component.playback.playerSheetBarAlpha
import com.melox.player.ui.component.LocalBottomSheetBlurBackdrop
import com.melox.player.ui.component.LocalTopBarBlurSettings
import com.melox.player.ui.component.LocalCustomPageBackground
import com.melox.player.ui.component.LocalCustomBackgroundDimAlpha
import com.melox.player.ui.component.LocalPageCardBlurRadius
import com.melox.player.ui.component.pageCardBlurRadius
import com.melox.player.ui.component.LocalPageCardSurfaceAlpha
import com.melox.player.ui.component.pageCardSurfaceAlpha
import com.melox.player.ui.component.topBarContainerColor
import com.melox.player.ui.component.tabSelectedContainerColor
import com.melox.player.ui.component.FixedPageBackgroundHost
import com.melox.player.ui.component.trackFixedWallpaperMotion
import com.melox.player.ui.component.refreshFixedWallpaperSample
import com.melox.player.ui.component.rememberCustomPageBackground
import com.melox.player.ui.component.customPageBackground
import com.melox.player.ui.component.customPageContainerColor
import com.melox.player.ui.component.PageScaffold
import com.melox.player.ui.component.expandedTopBarBottomContentGap
import com.melox.player.ui.component.TopBarBlurSettings
import com.melox.player.ui.component.miuixBarColor
import com.melox.player.ui.component.stableTopAppBarInsets
import com.melox.player.ui.component.rememberBlurBackdrop
import com.melox.player.ui.component.library.LocalAlphabetIndexBottomPadding
import com.melox.player.ui.component.library.fixedAlphabetIndexTopPadding
import com.melox.player.ui.component.library.MusicSortButton
import com.melox.player.ui.component.library.SelectionActionsAnimatedContent
import com.melox.player.ui.component.library.SelectionNavigationIconAnimatedContent
import com.melox.player.ui.component.library.ShufflePlayButton
import com.melox.player.ui.component.library.rememberSearchTopBarScrollBehavior
import com.melox.player.ui.component.library.TrackSelectionActions
import com.melox.player.ui.component.library.extractArtworkColor
import com.melox.player.ui.component.library.rememberArtworkBitmap
import com.melox.player.ui.component.library.normalizeArtworkTargetSize
import com.melox.player.model.CoverShape
import com.melox.player.model.withTrackMetadata
import com.melox.player.ui.component.playback.ArtworkTransitionState
import com.melox.player.ui.component.playback.rememberPlaybackArtworkTransition
import com.melox.player.ui.component.library.AlphabetSections
import com.melox.player.ui.component.library.AlphabetSideBar
import com.melox.player.ui.component.library.AlbumSortButton
import com.melox.player.ui.component.library.ArtistSortButton
import com.melox.player.ui.component.library.FolderSortButton
import com.melox.player.ui.component.library.selectedItemsInDisplayedOrder
import com.melox.player.ui.component.library.handleSongListTrackClick
import com.melox.player.ui.component.library.toggleAllTrackSelection
import com.melox.player.ui.component.playlist.PlaylistNameDialog
import com.melox.player.ui.component.playlist.PlaylistPickerOverlay
import com.melox.player.ui.component.playback.playerNavigationOffset
import com.melox.player.ui.component.playback.MiniPlayer
import com.melox.player.ui.component.playback.miniPlayerHeight
import com.melox.player.ui.component.playback.miniPlayerArtworkCornerRadius
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
import com.melox.player.ui.navigation.ImmediateNavigationTransition
import com.melox.player.ui.screen.library.Os4LibraryOverviewScreen
import com.melox.player.data.library.sortAlbums
import com.melox.player.data.library.sortArtists
import com.melox.player.data.library.sortFolders
import com.melox.player.ui.navigation.secondaryPageEnterMotion
import com.melox.player.ui.navigation.LocalWindowBackDispatcherOwner
import com.melox.player.ui.navigation.LocalOrdinaryBackDispatcherOwner
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
import com.melox.player.ui.screen.playback.MiniPlayerLyrics
import com.melox.player.ui.screen.playback.compactLyricsDocument
import com.melox.player.ui.screen.playback.FullPlayerScreen
import com.melox.player.ui.screen.playback.playerLayout
import com.melox.player.ui.screen.playback.playerHidesSystemNavigation
import com.melox.player.ui.screen.playback.QueueSheet
import com.melox.player.ui.screen.playlist.PlaylistDetailScreen
import com.melox.player.ui.screen.playlist.PlaylistLibraryScreen
import com.melox.player.ui.screen.settings.SettingsScreen
import com.melox.player.ui.screen.settings.ScanMusicScreen
import com.melox.player.ui.screen.settings.MusicStatisticsScreen
import com.melox.player.ui.screen.settings.AboutScreen
import com.melox.player.ui.screen.settings.PlaybackBehaviorScreen
import com.melox.player.ui.screen.settings.AudioOutputScreen
import com.melox.player.ui.screen.settings.SponsorScreen
import com.melox.player.ui.screen.settings.InterfaceStyleSettingsScreen
import com.melox.player.ui.screen.settings.ThemeSettingsScreen
import com.melox.player.ui.screen.settings.MainBackgroundScreen
import com.melox.player.ui.screen.settings.LyricsSettingsScreen
import com.melox.player.ui.screen.settings.LyricsInterfaceSettingsScreen
import com.melox.player.ui.screen.settings.BlockedFoldersScreen
import com.melox.player.ui.viewmodel.MeloxViewModel
import com.melox.player.ui.theme.MeloxTheme
import com.melox.player.ui.theme.rememberPlaybackContentColor
import com.melox.player.ui.theme.isInDarkTheme
import com.melox.player.ui.component.library.LocalSongListArtworkVisible
import com.melox.player.ui.component.library.LocalTrackMutationActions
import com.melox.player.ui.component.library.TrackMutationActions
import com.melox.player.ui.component.library.rememberTrackSourceDeletionHandler
import com.melox.player.ui.screen.settings.HiddenSongsScreen
import com.melox.player.ui.component.library.LocalSongListFileNameVisible
import com.melox.player.ui.component.library.LocalSongListDurationVisible
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import com.melox.player.ui.component.os4.SmallTopAppBar
import com.melox.player.ui.component.os4.Icon
import com.melox.player.ui.component.os4.IconButton
import top.yukonga.miuix.kmp.basic.NavigationRailDefaults
import top.yukonga.miuix.kmp.basic.Text
import com.melox.player.ui.component.os4.TopAppBar
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.basic.SearchCleanup
import top.yukonga.miuix.kmp.icon.extended.Search
import androidx.compose.runtime.snapshots.Snapshot
import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.LocalNavTransitionScope
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.RecordingTape
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.MiuixPopupHost
import top.yukonga.miuix.kmp.utils.overScrollHorizontal
import kotlin.math.abs
import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
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

internal fun shouldTriggerTabSwitchHaptic(
    enabled: Boolean,
    selectedPage: Int,
    targetPage: Int,
): Boolean = enabled && selectedPage != targetPage

@Serializable
internal enum class AppRoute {
    ROOT,
    SCAN_SETTINGS,
    MUSIC_STATISTICS,
    PLAYBACK_BEHAVIOR,
    AUDIO_OUTPUT,
    THEME_SETTINGS,
    INTERFACE_STYLE,
    MAIN_BACKGROUND,
    LYRICS_SETTINGS,
    LYRICS_INTERFACE,
    ABOUT,
    ALBUM_DETAIL,
    ARTIST_DETAIL,
    FOLDER_DETAIL,
    INTERNAL_STORAGE,
    PLAYLISTS,
    ALBUMS,
    ARTISTS,
    FOLDERS,
    PLAYLIST_DETAIL,
    BLOCKED_FOLDERS,
    HIDDEN_SONGS,
    SPONSOR,
}

@Serializable
internal data class AppNavDestination(
    val route: AppRoute,
    val contentKey: String? = null,
    val depth: Int,
) : NavKey

// Keep miuix-nav identity tied to the stack slot so a return can swap the displayed
// detail object without changing a pop into a forward replacement transition.
private fun AppNavDestination.entryContentKey(): String =
    if (route == AppRoute.INTERNAL_STORAGE) "melox:${route.name}:$depth:$contentKey"
    else "melox:${route.name}:$depth"

internal fun selectedSidebarShortcut(
    destinations: List<AppNavDestination>,
    sidebarRoutes: List<AppRoute>,
): Int? = destinations.asReversed()
    .firstNotNullOfOrNull { destination -> sidebarRoutes.indexOf(destination.route).takeIf { it >= 0 } }

internal fun synchronizeAppNavBackStack(
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

private data class PlaylistCreateRequest(
    val tracks: List<MusicTrack>,
    val openAfterCreate: Boolean,
    val detailParentRoute: AppRoute = AppRoute.ROOT,
)

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun MeloxApp(
    viewModel: MeloxViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val loadedCustomBackground by viewModel.customBackground.collectAsStateWithLifecycle()
    val compactPlayback by viewModel.compactPlaybackState.collectAsStateWithLifecycle()
    val currentTrackId by viewModel.currentTrackId.collectAsStateWithLifecycle()
    val hasCurrentItem by viewModel.hasCurrentItem.collectAsStateWithLifecycle()
    val musicPresentation by viewModel.musicPresentation.collectAsStateWithLifecycle()
    val albumPresentation by viewModel.albumPresentation.collectAsStateWithLifecycle()
    val artistPresentation by viewModel.artistPresentation.collectAsStateWithLifecycle()
    val preparedFolderPresentation by viewModel.folderPresentation.collectAsStateWithLifecycle()
    val internalStorageHierarchy by viewModel.internalStorageHierarchy.collectAsStateWithLifecycle()
    val hiddenFolderPaths by viewModel.hiddenFolderPaths.collectAsStateWithLifecycle()
    val hiddenFolderAddresses by viewModel.hiddenFolderAddresses.collectAsStateWithLifecycle()
    val hiddenTracks by viewModel.hiddenTracks.collectAsStateWithLifecycle()
    val hiddenTrackUris by viewModel.hiddenTrackUris.collectAsStateWithLifecycle()
    val folderPresentation = remember(preparedFolderPresentation, hiddenFolderPaths) {
        preparedFolderPresentation.withHiddenFolders(hiddenFolderPaths)
    }
    val folderLibrarySectionIndexMap = remember(folderPresentation.sectionIndexMap) {
        offsetSectionIndices(folderPresentation.sectionIndexMap, 1)
    }
    val playlistState by viewModel.playlistState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    val context = LocalContext.current
    val resources = LocalResources.current
    val windowSize = LocalWindowInfo.current.containerSize
    val playbackErrorText = stringResource(R.string.playback_error)
    val scanNoChangesText = stringResource(R.string.scan_no_changes)
    val hiddenFolderSaveErrorText = stringResource(R.string.hidden_folder_save_error)
    val hiddenTrackSaveErrorText = stringResource(R.string.hidden_song_save_error)
    val libraryCacheSaveErrorText = stringResource(R.string.music_cache_save_error)
    val deleteTrackSource = rememberTrackSourceDeletionHandler(viewModel::onTrackSourceDeleted)
    val playlistSaveErrorText = stringResource(R.string.playlist_save_error)
    LaunchedEffect(viewModel, playlistSaveErrorText) {
        viewModel.playlistSaveFailures.collectLatest {
            Toast.makeText(context, playlistSaveErrorText, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(viewModel, hiddenFolderSaveErrorText) {
        viewModel.hiddenFolderSaveFailures.collectLatest {
            Toast.makeText(context, hiddenFolderSaveErrorText, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(viewModel, hiddenTrackSaveErrorText) {
        viewModel.hiddenTrackSaveFailures.collectLatest {
            Toast.makeText(context, hiddenTrackSaveErrorText, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(viewModel, libraryCacheSaveErrorText) {
        viewModel.libraryCacheSaveFailures.collectLatest {
            Toast.makeText(context, libraryCacheSaveErrorText, Toast.LENGTH_LONG).show()
        }
    }
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
    val isDark = isInDarkTheme(settings.themeMode)
    val dynamicFlowDark = settings.dynamicFlowStyle.isDark(systemDark)
    val playerBackgroundDark = usesDarkPlaybackBackground(
        backgroundStyle = settings.playbackBackgroundStyle,
        dynamicFlowDark = dynamicFlowDark,
        appDark = isDark,
        hasItem = compactPlayback.currentItem != null,
    )
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
    } else if (!settings.hideBottomBar && landscape && requestedBottomBarStyle == BottomBarStyle.LIQUID_GLASS) {
        windowWidth - FloatingNavigationRailInset
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
    // A tab tap owns presentation until its scroll settles; swipes follow the visible page.
    val navigationPresentationPage = if (playerPagerState.isNavigating) playerPagerState.selectedPage else pagerState.currentPage
    val homeRecommendations = rememberHomeRecommendations(
        tracks = uiState.tracks,
        active = playerPagerState.selectedPage == HOME_TAB_INDEX,
        blurEnabled = settings.blurEnabled,
        initialRecommendationCount = homeInitialRecommendationCount(
            recommendationViewportWidth,
            landscape = windowWidth > windowHeight,
        ),
    )
    var musicSortFieldOrdinal by rememberSaveable {
        mutableIntStateOf(MusicSortField.TITLE.ordinal)
    }
    var musicSortDescending by rememberSaveable { mutableStateOf(false) }
    var songSearchQuery by rememberSaveable { mutableStateOf("") }
    var songSearchVisible by rememberSaveable { mutableStateOf(false) }
    var songSearchFocused by remember { mutableStateOf(false) }
    var selectedSongUris by remember { mutableStateOf<Set<String>>(emptySet()) }
    var songsSelectionMode by remember { mutableStateOf(false) }
    val librarySearchQueryState = rememberSaveable { mutableStateOf("") }
    var librarySearchQuery by librarySearchQueryState
    val secondarySearchQuery = List(LIBRARY_TAB_COUNT) { rememberSaveable { mutableStateOf("") } }
    val librarySearchVisibleState = rememberSaveable { mutableStateOf(false) }
    var librarySearchVisible by librarySearchVisibleState
    val secondarySearchVisible = List(LIBRARY_TAB_COUNT) { rememberSaveable { mutableStateOf(false) } }
    val librarySearchFocusedState = remember { mutableStateOf(false) }
    var librarySearchFocused by librarySearchFocusedState
    val secondarySearchFocused = List(LIBRARY_TAB_COUNT) { remember { mutableStateOf(false) } }
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
    var artworkDisplaySizePx by remember(density) {
        mutableIntStateOf(normalizeArtworkTargetSize(
            with(density) { PLAYER_FULL_ARTWORK_REQUEST_SIZE.roundToPx() },
        ))
    }
    val artworkItem = remember(compactPlayback.currentItem, uiState.tracks) {
        compactPlayback.currentItem?.let { item ->
            uiState.tracks.firstOrNull { it.id == item.trackId }?.let(item::withTrackMetadata) ?: item
        }
    }
    val artworkState = rememberPlaybackArtworkTransition(
        item = artworkItem,
        direction = compactPlayback.trackChangeDirection,
        flipAnimationEnabled = settings.artworkFlipAnimation,
        fullPlayerTargetSizePx = artworkDisplaySizePx,
        fullPlayerMounted = playerTransition.fullPlayerHostMounted,
    )
    var playerContentBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    val sharedPlayerArtworkEnabled = compactPlayback.currentItem != null &&
        playerTransition.fullPlayerArtworkPageSelected
    val miniPlayerLayer = rememberGraphicsLayer()
    val miniPlayerContentLayer = rememberGraphicsLayer()
    val miniPlayerHighlightScope = rememberCoroutineScope()
    val miniPlayerInteractiveHighlight = remember(miniPlayerHighlightScope) {
        InteractiveHighlight(
            miniPlayerHighlightScope,
            highlightAlpha = { playerSheetBarAlpha(playerTransition.progress) },
            spotRadius = miniPlayerHeight(BottomBarStyle.LIQUID_GLASS, smallPlayerBar = false) * 1.2f,
        )
    }
    val fullPlayerBackgroundLayer = rememberGraphicsLayer()
    val fullPlayerContentLayer = rememberGraphicsLayer()
    SideEffect { playerTransition.updateWindowSize(windowSize) }
    val frameRecordingGeneration = playerTransition.currentFrameRecordingGeneration
    var miniPlayerChrome by remember { mutableStateOf<MiniPlayerChrome?>(null) }
    var currentRoute by rememberSaveable { mutableStateOf(AppRoute.ROOT) }
    var standaloneLibraryParent by rememberSaveable { mutableStateOf(AppRoute.ROOT) }
    var sidebarEntryRoute by rememberSaveable { mutableStateOf<AppRoute?>(null) }
    val bottomSearchActions = remember { mutableStateMapOf<Any, BottomSearchAction>() }
    var detailSelectionActive by remember { mutableStateOf(false) }
    var detailSelectionExitRequest by remember { mutableIntStateOf(0) }
    var selectedAlbumKey by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedArtistKey by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFolderKey by rememberSaveable { mutableStateOf<String?>(null) }
    var internalStoragePath by rememberSaveable { mutableStateOf(INTERNAL_STORAGE_ROOT) }
    LaunchedEffect(internalStorageHierarchy, internalStoragePath, currentRoute) {
        if (currentRoute == AppRoute.INTERNAL_STORAGE) {
            internalStoragePath = internalStorageHierarchy.closestDirectoryKey(internalStoragePath)
        }
    }
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
    val closePlayer = { playerTransition.close() }
    val playerUsesFullPlayerStatusBar = playerSheetUsesFullPlayerStatusBar(
        progress = playerTransition.progress,
    )
    val playerLayoutDirection = LocalLayoutDirection.current
    val stablePlayerInsets = WindowInsets.systemBarsIgnoringVisibility
        .union(WindowInsets.displayCutout).asPaddingValues()
    val systemChromePlayerLayout = playerLayout(
        windowWidth = (windowWidth - stablePlayerInsets.calculateStartPadding(playerLayoutDirection) -
            stablePlayerInsets.calculateEndPadding(playerLayoutDirection)).coerceAtLeast(0.dp),
        windowHeight = (windowHeight - stablePlayerInsets.calculateTopPadding() -
            stablePlayerInsets.calculateBottomPadding()).coerceAtLeast(0.dp),
    )
    StatusBarVisibilityEffect(
        activity = activity,
        hidden = settings.statusBarHidden,
        navigationBarHidden = playerHidesSystemNavigation(
            layout = systemChromePlayerLayout,
            fullPlayerVisible = playerTransition.isMounted && playerUsesFullPlayerStatusBar,
        ),
    )
    val statusBarUsesDarkMode = if (playerUsesFullPlayerStatusBar) playerBackgroundDark else isDark
    val navigationBarUsesDarkMode = if (
        playerUsesFullPlayerStatusBar &&
            (settings.playbackBackgroundStyle == PlaybackBackgroundStyle.DYNAMIC_FLOW ||
                compactPlayback.currentItem == null)
    ) playerBackgroundDark else isDark
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
    val independentLibrarySearch = settings.themeStyle == com.melox.player.model.ThemeStyle.OS4 &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val albumSearchQuery = if (independentLibrarySearch) secondarySearchQuery[LIBRARY_ALBUMS_TAB_INDEX].value else librarySearchQuery
    val artistSearchQuery = if (independentLibrarySearch) secondarySearchQuery[LIBRARY_ARTISTS_TAB_INDEX].value else librarySearchQuery
    val folderSearchQuery = if (independentLibrarySearch) secondarySearchQuery[LIBRARY_FOLDERS_TAB_INDEX].value else librarySearchQuery
    LaunchedEffect(albumSearchQuery, albumSortConfig) {
        viewModel.updateAlbumPresentation(albumSearchQuery, albumSortConfig)
    }
    LaunchedEffect(artistSearchQuery, artistSortConfig) {
        viewModel.updateArtistPresentation(artistSearchQuery, artistSortConfig)
    }
    LaunchedEffect(folderSearchQuery, folderSortConfig) {
        viewModel.updateFolderPresentation(folderSearchQuery, folderSortConfig)
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
        internalStoragePath,
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
            viewModel.scanMusic()
        } else {
            viewModel.markPermissionRequired()
        }
    }

    val startManualScan: () -> Unit = {
        if (context.hasPermission(audioPermission)) {
            viewModel.scanMusic()
        } else {
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
                foreground = true,
            )?.extractArtworkColor()
        }
    } else {
        null
    }

    MeloxTheme(
        settings = settings,
        playbackArtworkColor = playbackArtworkColor,
    ) {
        DisposableEffect(activity, navigationBarUsesDarkMode, statusBarUsesDarkMode) {
            val componentActivity = activity as? ComponentActivity
            val statusBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
            ) { statusBarUsesDarkMode }
            val navigationBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
            ) { navigationBarUsesDarkMode }
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
        val secondaryAlbumsGridState = rememberLazyGridState()
        val secondaryArtistsListState = rememberLazyListState()
        val secondaryFoldersListState = rememberLazyListState()
        val secondaryLibraryScrollBehaviors = List(LIBRARY_TAB_COUNT) { index ->
            rememberSearchTopBarScrollBehavior(
                if (independentLibrarySearch) secondarySearchVisible[index].value else librarySearchVisible,
            )
        }
        val themeSettingsListState = rememberLazyListState()
        val interfaceStyleListState = rememberLazyListState()
        val mainBackgroundListState = rememberLazyListState()
        val lyricsSettingsListState = rememberLazyListState()
        val lyricsInterfaceListState = rememberLazyListState()
        val playbackBehaviorListState = rememberLazyListState()
        val audioOutputListState = rememberLazyListState()
        var renderedBottomBarStyle by remember {
            mutableStateOf(settings.bottomBarStyle)
        }
        LaunchedEffect(settings.bottomBarStyle, currentRoute) {
            if (
                renderedBottomBarStyle != settings.bottomBarStyle &&
                (
                    currentRoute == AppRoute.INTERFACE_STYLE ||
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
        val renderedSmallPlayerBar = settings.smallPlayerBar && !landscape &&
            resolveBottomBarStyle(renderedBottomBarStyle, liquidGlassSupported).supportsSmallPlayerBar
        val miniPlayerUsesNormalChrome = usesNormalMiniPlayerChrome(
            renderedBottomBarStyle = renderedBottomBarStyle,
            liquidGlassSupported = liquidGlassSupported,
        )
        val useSmallTopAppBar = landscape
        val rootIndexBottomSpacing = if (miniPlayerUsesNormalChrome) 12.dp else 6.dp
        val homeScrollBehavior = MiuixScrollBehavior()
        val songsScrollBehavior = rememberSearchTopBarScrollBehavior(songSearchVisible)
        val libraryScrollBehavior = rememberSearchTopBarScrollBehavior(librarySearchVisible)
        val settingsScrollBehavior = MiuixScrollBehavior()
        val themeSettingsScrollBehavior = MiuixScrollBehavior()
        val interfaceStyleScrollBehavior = MiuixScrollBehavior()
        val mainBackgroundScrollBehavior = MiuixScrollBehavior()
        val lyricsSettingsScrollBehavior = MiuixScrollBehavior()
        val lyricsInterfaceScrollBehavior = MiuixScrollBehavior()
        val playbackBehaviorScrollBehavior = MiuixScrollBehavior()
        val audioOutputScrollBehavior = MiuixScrollBehavior()
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
            if (currentRoute !in listOf(
                    AppRoute.ALBUMS, AppRoute.ARTISTS, AppRoute.FOLDERS,
                    AppRoute.ALBUM_DETAIL, AppRoute.ARTIST_DETAIL,
                    AppRoute.FOLDER_DETAIL, AppRoute.INTERNAL_STORAGE,
                )
            ) {
                standaloneLibraryParent = AppRoute.ROOT
            }
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
            if (currentRoute !in listOf(
                    AppRoute.ALBUMS, AppRoute.ARTISTS, AppRoute.FOLDERS,
                    AppRoute.ALBUM_DETAIL, AppRoute.ARTIST_DETAIL,
                    AppRoute.FOLDER_DETAIL, AppRoute.INTERNAL_STORAGE,
                )
            ) {
                standaloneLibraryParent = AppRoute.ROOT
            }
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
        val openCurrentPlayer: () -> Unit = {
            playerTransition.open(
                requireFreshMiniFrame = miniPlayerChrome?.let { chrome ->
                    chrome.liquidGlassActive && chrome.backdrop != null
                } == true,
            )
        }
        val onSongListTrackClick: (List<MusicTrack>, Int) -> Unit = { tracks, index ->
            tracks.getOrNull(index)?.let { track ->
                handleSongListTrackClick(
                    selectedTrack = track,
                    currentTrackContentUri = viewModel.playbackState.value.currentItem?.contentUri,
                    onOpenPlayer = openCurrentPlayer,
                    onPlayTrack = { viewModel.playTracks(tracks, index) },
                )
            }
        }
        val libraryRoutes = listOf(AppRoute.ALBUMS, AppRoute.ARTISTS, AppRoute.FOLDERS)
        val sidebarRoutes = listOf(AppRoute.PLAYLISTS) + libraryRoutes
        val libraryOverviewListState = rememberLazyListState()
        val libraryContent: @Composable (PaddingValues, Boolean, Int?, () -> Unit) -> Unit =
            { outerPadding, navigationRailExpanded, section, onBack ->
                val overview = section == null && com.melox.player.ui.theme.LocalOs4Style.current
                val independentSection = section.takeIf { independentLibrarySearch }
                var librarySearchQuery by (independentSection?.let { secondarySearchQuery[it] } ?: librarySearchQueryState)
                var librarySearchVisible by (independentSection?.let { secondarySearchVisible[it] } ?: librarySearchVisibleState)
                var librarySearchFocused by (independentSection?.let { secondarySearchFocused[it] } ?: librarySearchFocusedState)
                val pageLibraryScrollBehavior = if (section == null) {
                    libraryScrollBehavior
                } else {
                    secondaryLibraryScrollBehaviors[section]
                }
                val pageFoldersListState = if (section == null) foldersListState else secondaryFoldersListState
                val pageArtistsListState = if (section == null) artistsListState else secondaryArtistsListState
                val pageAlbumsGridState = if (section == null) albumsGridState else secondaryAlbumsGridState
                LaunchedEffect(pendingAlbumGridReset, albumSortConfig.gridStyle, currentRoute) {
                    val pageRoute = section?.let { libraryRoutes[it] } ?: AppRoute.ROOT
                    val pendingGridStyle = pendingAlbumGridReset ?: return@LaunchedEffect
                    if (currentRoute != pageRoute || albumSortConfig.gridStyle != pendingGridStyle) {
                        return@LaunchedEffect
                    }
                    withFrameNanos { }
                    pageAlbumsGridState.scrollToItem(0)
                    pageLibraryScrollBehavior.state.heightOffset = 0f
                    pageLibraryScrollBehavior.state.contentOffset = 0f
                    pendingAlbumGridReset = null
                }
                PlayerPage(
                    title = section?.let { libraryTabs[it] } ?: libraryTitle,
                    navigationIcon = {
                        if (section != null) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    com.melox.player.ui.component.os4.navigationBackIcon(),
                                    stringResource(R.string.back),
                                )
                            }
                        }
                    },
                    outerPadding = outerPadding,
                    scrollBehavior = pageLibraryScrollBehavior,
                    useSmallTopAppBar = useSmallTopAppBar,
                    isRootPage = section == null,
                    actions = {
                        if (!overview) {
                            LibrarySearchButton(
                                bottomActionActive = section != null || navigationPresentationPage == LIBRARY_TAB_INDEX,
                                visible = librarySearchVisible,
                                scrollBehavior = pageLibraryScrollBehavior,
                                contentDescription = when (section ?: libraryPagerState.currentPage) {
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
                            when (section ?: libraryPagerState.currentPage) {
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
                        }
                    },
                    bottomContent = {
                        if (!overview) {
                            Column {
                                if (section == null) {
                                    Box(
                                        modifier = Modifier.height(
                                            expandedTopBarBottomContentGap(
                                                pageLibraryScrollBehavior,
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
                                }
                                LibrarySearchBar(
                                    visible = librarySearchVisible,
                                    focused = librarySearchFocused,
                                    backEnabled = if (section != null) currentRoute == libraryRoutes[section]
                                    else currentRoute == AppRoute.ROOT && pagerState.currentPage == LIBRARY_TAB_INDEX,
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
                        }
                    },
                ) { contentPadding, _, indexTopPadding ->
                    if (overview) {
                        Os4LibraryOverviewScreen(
                            albums = remember(uiState.albums, albumSortConfig) { sortAlbums(uiState.albums, albumSortConfig) },
                            artists = remember(uiState.artists, artistSortConfig) { sortArtists(uiState.artists, artistSortConfig) },
                            folders = remember(uiState.folders, folderSortConfig) { sortFolders(uiState.folders, folderSortConfig) },
                            gridStyle = albumSortConfig.gridStyle,
                            listState = libraryOverviewListState,
                            scrollBehavior = pageLibraryScrollBehavior,
                            contentPadding = contentPadding,
                            onOpenSection = { index ->
                                librarySearchVisible = false
                                librarySearchFocused = false
                                librarySearchQuery = ""
                                currentRoute = libraryRoutes[index]
                            },
                            onAlbumClick = { album ->
                                standaloneLibraryParent = AppRoute.ROOT
                                selectedAlbumKey = album.key
                                albumParentRoute = AppRoute.ROOT
                                albumParentArtistKey = null
                                currentRoute = AppRoute.ALBUM_DETAIL
                            },
                            onArtistClick = openTrackArtist,
                            onFolderClick = { folder ->
                                standaloneLibraryParent = AppRoute.ROOT
                                selectedFolderKey = folder.key
                                currentRoute = AppRoute.FOLDER_DETAIL
                            },
                        )
                    } else {
                        val layoutDirection = LocalLayoutDirection.current
                        val onLibraryIndexTargetChanged: (Int, Boolean) -> Unit =
                            { _, restoreLargeTitle ->
                                val state = pageLibraryScrollBehavior.state
                                if (restoreLargeTitle && !pageLibraryScrollBehavior.isPinned) {
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
                                    bottom = LocalAlphabetIndexBottomPadding.current,
                                )
                                .fillMaxHeight()
                            val renderLibrarySection: @Composable (Int) -> Unit = { libraryPage ->
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
                                            standaloneLibraryParent = section?.let { libraryRoutes[it] } ?: AppRoute.ROOT
                                            selectedAlbumKey = album.key
                                            albumParentRoute = AppRoute.ROOT
                                            albumParentArtistKey = null
                                            currentRoute = AppRoute.ALBUM_DETAIL
                                        },
                                        scrollBehavior = pageLibraryScrollBehavior,
                                        indexTopPadding = indexTopPadding,
                                        gridState = pageAlbumsGridState,
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
                                            standaloneLibraryParent = section?.let { libraryRoutes[it] } ?: AppRoute.ROOT
                                            openTrackArtist(artist)
                                        },
                                        scrollBehavior = pageLibraryScrollBehavior,
                                        indexTopPadding = indexTopPadding,
                                        listState = pageArtistsListState,
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
                                            standaloneLibraryParent = section?.let { libraryRoutes[it] } ?: AppRoute.ROOT
                                            selectedFolderKey = folder.key
                                            currentRoute = AppRoute.FOLDER_DETAIL
                                        },
                                        onOpenInternalStorage = {
                                            dismissLibrarySearchFocus()
                                            standaloneLibraryParent = section?.let { libraryRoutes[it] } ?: AppRoute.ROOT
                                            internalStoragePath = INTERNAL_STORAGE_ROOT
                                            currentRoute = AppRoute.INTERNAL_STORAGE
                                        },
                                        onBlockFolder = { path ->
                                            viewModel.addBlockedFolderPath(path)
                                        },
                                        scrollBehavior = pageLibraryScrollBehavior,
                                        indexTopPadding = indexTopPadding,
                                        listState = pageFoldersListState,
                                        contentPadding = contentPadding,
                                        showIndex = false,
                                    )
                                }
                            }
                            if (section == null) {
                                HorizontalPager(
                                    state = libraryPagerState,
                                    modifier = Modifier.fillMaxSize(),
                                    userScrollEnabled = false,
                                    key = { it },
                                ) { renderLibrarySection(it) }
                            } else {
                                renderLibrarySection(section)
                            }

                            when (section ?: libraryPagerState.currentPage) {
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
                                        topOffset = 108.dp,
                                        itemCount = albumPresentation.items.size,
                                        scrollStateKey = pageAlbumsGridState,
                                        isAtTarget = { index ->
                                            pageAlbumsGridState.firstVisibleItemIndex == index &&
                                                pageAlbumsGridState.firstVisibleItemScrollOffset == 0
                                        },
                                        scrollToItem = pageAlbumsGridState::scrollToItem,
                                        sections = if (albumSortConfig.descending) {
                                            AlphabetSections.asReversed()
                                        } else {
                                            AlphabetSections
                                        },
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
                                        topOffset = 108.dp,
                                        itemCount = artistPresentation.items.size,
                                        scrollStateKey = pageArtistsListState,
                                        isAtTarget = { index ->
                                            pageArtistsListState.firstVisibleItemIndex == index &&
                                                pageArtistsListState.firstVisibleItemScrollOffset == 0
                                        },
                                        scrollToItem = pageArtistsListState::scrollToItem,
                                        sections = if (artistSortConfig.descending) {
                                            AlphabetSections.asReversed()
                                        } else {
                                            AlphabetSections
                                        },
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
                                        sectionIndexMap = folderLibrarySectionIndexMap,
                                        topOffset = 108.dp,
                                        itemCount = folderPresentation.items.size + 1,
                                        scrollStateKey = pageFoldersListState,
                                        isAtTarget = { index ->
                                            pageFoldersListState.firstVisibleItemIndex == index &&
                                                pageFoldersListState.firstVisibleItemScrollOffset == 0
                                        },
                                        scrollToItem = pageFoldersListState::scrollToItem,
                                        sections = if (folderSortConfig.descending) {
                                            AlphabetSections.asReversed()
                                        } else {
                                            AlphabetSections
                                        },
                                        onTargetIndexChanged = onLibraryIndexTargetChanged,
                                        modifier = libraryIndexModifier,
                                    )
                                }
                            }
                        }
                    }
                }
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
                    .overScrollHorizontal()
                    .trackFixedWallpaperMotion(),
                userScrollEnabled = !(landscape && requestedBottomBarStyle == BottomBarStyle.OS4) && rootPagerUserScrollEnabled(
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
                                sidebarEntryRoute = null
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
                            onRecommendationClick = { track, recommendations ->
                                handleSongListTrackClick(
                                    selectedTrack = track,
                                    currentTrackContentUri =
                                        viewModel.playbackState.value.currentItem?.contentUri,
                                    onOpenPlayer = openCurrentPlayer,
                                    onPlayTrack = {
                                        viewModel.playHomeRecommendation(track, recommendations)
                                    },
                                )
                            },
                            onRecommendationPageChanged = { page -> homeRecommendationPage = page },
                            onRecommendationGestureActiveChanged = { active ->
                                homeRecommendationGestureActive = active
                            },
                            scrollBehavior = scrollBehavior,
                            listState = homeListState,
                            landscape = windowWidth > windowHeight,
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
                                defaultNavigationIcon = {
                                    ShufflePlayButton(
                                        enabled = musicPresentation.items.isNotEmpty(),
                                        onClick = { viewModel.shuffleTracks(musicPresentation.items) },
                                    )
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
                                    bottomActionActive = navigationPresentationPage == SONGS_TAB_INDEX,
                                    visible = songSearchVisible,
                                    scrollBehavior = songsScrollBehavior,
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
                                backEnabled = pagerState.currentPage == SONGS_TAB_INDEX,
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
                            showLocateAction = true,
                            currentTrackContentUri = compactPlayback.currentItem?.contentUri,
                            miniPlayerBounds = playerTransition.miniPlayerBounds,
                            playerContentBounds = playerContentBounds,
                            onTrackClick = onSongListTrackClick,
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
                            showLunaBeatEditor = settings.showLunaBeatEditor,
                            scrollBehavior = scrollBehavior,
                            indexTopPadding = indexTopPadding,
                            listState = songsListState,
                            onRefresh = if (settings.songListPullToRefresh) viewModel::scanMusic else null,
                            contentPadding = contentPadding,
                            selectionMode = songsSelectionMode,
                            selectedTrackUris = selectedSongUris,
                            onSelectionChange = { selectedSongUris = it },
                            onSelectionModeChange = { songsSelectionMode = it },
                        )
                    }

                    LIBRARY_TAB_INDEX -> libraryContent(outerPadding, navigationRailExpanded, null, {})

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
                            onOpenPlaybackBehavior = { currentRoute = AppRoute.PLAYBACK_BEHAVIOR },
                            onOpenAudioOutput = { currentRoute = AppRoute.AUDIO_OUTPUT },
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
            AppRoute.INTERNAL_STORAGE -> INTERNAL_STORAGE_ROOT
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
            standaloneLibraryParent,
        ) {
            val root = AppNavDestination(AppRoute.ROOT, depth = 0)
            when {
                currentRoute == AppRoute.ROOT -> listOf(root)
                currentRoute == AppRoute.INTERNAL_STORAGE -> buildList {
                    add(root)
                    if (standaloneLibraryParent != AppRoute.ROOT) {
                        add(AppNavDestination(standaloneLibraryParent, depth = size))
                    }
                    add(AppNavDestination(AppRoute.INTERNAL_STORAGE, contentKey = INTERNAL_STORAGE_ROOT, depth = size))
                }
                currentRoute == AppRoute.ALBUM_DETAIL &&
                    albumParentRoute == AppRoute.ARTIST_DETAIL &&
                    albumParentArtistKey != null -> buildList {
                    add(root)
                    if (standaloneLibraryParent != AppRoute.ROOT) {
                        add(AppNavDestination(standaloneLibraryParent, depth = size))
                    }
                    add(
                        AppNavDestination(
                            route = AppRoute.ARTIST_DETAIL,
                            contentKey = albumParentArtistKey,
                            depth = size,
                        ),
                    )
                    add(
                        AppNavDestination(
                            route = AppRoute.ALBUM_DETAIL,
                            contentKey = selectedAlbumKey,
                            depth = size,
                        ),
                    )
                }
                currentRoute == AppRoute.ARTIST_DETAIL &&
                    artistParentRoute == AppRoute.ALBUM_DETAIL &&
                    artistParentAlbumKey != null -> buildList {
                    add(root)
                    if (standaloneLibraryParent != AppRoute.ROOT) {
                        add(AppNavDestination(standaloneLibraryParent, depth = size))
                    }
                    if (
                        albumParentRoute == AppRoute.ARTIST_DETAIL &&
                            albumParentArtistKey != null
                    ) {
                        add(
                            AppNavDestination(
                                route = AppRoute.ARTIST_DETAIL,
                                contentKey = albumParentArtistKey,
                                depth = size,
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
                currentRoute == AppRoute.BLOCKED_FOLDERS ||
                    currentRoute == AppRoute.HIDDEN_SONGS -> listOf(
                    root,
                    AppNavDestination(
                        route = AppRoute.SCAN_SETTINGS,
                        depth = 1,
                    ),
                    AppNavDestination(
                        route = currentRoute,
                        depth = 2,
                    ),
                )
                currentRoute == AppRoute.MAIN_BACKGROUND ||
                    currentRoute == AppRoute.INTERFACE_STYLE -> listOf(
                    root,
                    AppNavDestination(AppRoute.THEME_SETTINGS, depth = 1),
                    AppNavDestination(currentRoute, depth = 2),
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
                currentRoute in listOf(AppRoute.ALBUM_DETAIL, AppRoute.ARTIST_DETAIL, AppRoute.FOLDER_DETAIL) &&
                    standaloneLibraryParent != AppRoute.ROOT -> listOf(
                    root,
                    AppNavDestination(standaloneLibraryParent, depth = 1),
                    AppNavDestination(currentRoute, contentKey = currentRouteContentKey, depth = 2),
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
        // Reconcile before composing entries so no frame renders the new route with the old stack.
        Snapshot.withMutableSnapshot {
            synchronizeAppNavBackStack(navBackStack, desiredNavBackStack)
        }
        val navigateBack = {
            Snapshot.withMutableSnapshot {
                val previousRoute = currentRoute
                if (detailSelectionActive) {
                    detailSelectionExitRequest += 1
                } else if (showQueue) {
                    showQueue = false
                } else if (currentRoute == AppRoute.INTERNAL_STORAGE) {
                    val trail = internalStorageDirectoryTrail(internalStoragePath)
                    val parent = trail.getOrNull(trail.lastIndex - 1)
                    if (parent == null) currentRoute = standaloneLibraryParent else internalStoragePath = parent
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
                } else if (
                    currentRoute == AppRoute.BLOCKED_FOLDERS ||
                    currentRoute == AppRoute.HIDDEN_SONGS
                ) {
                    currentRoute = AppRoute.SCAN_SETTINGS
                } else if (currentRoute == AppRoute.LYRICS_INTERFACE) {
                    currentRoute = AppRoute.LYRICS_SETTINGS
                } else if (
                    currentRoute == AppRoute.MAIN_BACKGROUND ||
                    currentRoute == AppRoute.INTERFACE_STYLE
                ) {
                    currentRoute = AppRoute.THEME_SETTINGS
                } else if (currentRoute == AppRoute.SPONSOR) {
                    currentRoute = AppRoute.ABOUT
                } else if (
                    currentRoute == AppRoute.PLAYLIST_DETAIL &&
                    playlistParentRoute == AppRoute.PLAYLISTS
                ) {
                    currentRoute = AppRoute.PLAYLISTS
                } else {
                    currentRoute = when (currentRoute) {
                        AppRoute.ALBUM_DETAIL, AppRoute.ARTIST_DETAIL, AppRoute.FOLDER_DETAIL -> standaloneLibraryParent
                        else -> AppRoute.ROOT
                    }
                }
                // Miuix consumes the pop synchronously before releasing predictive gesture ownership.
                if (currentRoute != previousRoute && navBackStack.size > 1) {
                    navBackStack.removeAt(navBackStack.lastIndex)
                }
            }
        }
        val hapticFeedback = LocalHapticFeedback.current
        val onNavigationTabSelected: (Int) -> Unit = { selectedTab ->
            val shouldHaptic = shouldTriggerTabSwitchHaptic(
                enabled = settings.hapticFeedbackEnabled,
                selectedPage = playerPagerState.selectedPage,
                targetPage = selectedTab,
            )
            if (shouldHaptic) hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
            val wideOs4 = landscape && requestedBottomBarStyle == BottomBarStyle.OS4
            Snapshot.withMutableSnapshot {
                if (wideOs4 && currentRoute != AppRoute.ROOT) sidebarEntryRoute = AppRoute.ROOT
                if (navBackStack.size > 1) navBackStack.subList(1, navBackStack.size).clear()
                currentRoute = AppRoute.ROOT
            }
            if (wideOs4) playerPagerState.snapToPage(selectedTab)
            else playerPagerState.animateToPage(selectedTab)
        }
        var customBackgroundBlurPreview by remember(settings.customBackgroundId) {
            mutableStateOf<Int?>(null)
        }
        LaunchedEffect(settings.customBackgroundBlurPercent, customBackgroundBlurPreview) {
            if (customBackgroundBlurPreview == settings.customBackgroundBlurPercent) {
                customBackgroundBlurPreview = null
            }
        }
        val customPageBackground = rememberCustomPageBackground(
            settings, loadedCustomBackground, customBackgroundBlurPreview,
        )
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
            LocalTrackMutationActions provides TrackMutationActions(
                hiddenContentUris = hiddenTrackUris,
                onHide = viewModel::hideTrack,
                onRestore = viewModel::restoreTracks,
                onDelete = deleteTrackSource,
            ),
            LocalCustomPageBackground provides customPageBackground,
            LocalSongListArtworkVisible provides settings.showSongListArtwork,
            LocalSongListFileNameVisible provides settings.showSongListFileName,
            LocalSongListDurationVisible provides settings.showSongListDuration,
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
                LocalBottomSearchActions provides bottomSearchActions.takeIf { requestedBottomBarStyle == BottomBarStyle.OS4 && !landscape },
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
                    val searchDestination = desiredNavBackStack.lastOrNull()?.entryContentKey()
                    val currentSearchAction = bottomSearchActions.values.firstOrNull {
                        it.owner == searchDestination && it.active.value
                    }
                    val searchDestinationKey = searchDestination to navigationPresentationPage.takeIf { currentRoute == AppRoute.ROOT }
                    var preparedSearchDestination by remember { mutableStateOf<Pair<String?, Int?>?>(null) }
                    val destinationHasSearch = when (currentRoute) {
                        AppRoute.ROOT -> navigationPresentationPage == SONGS_TAB_INDEX ||
                            (navigationPresentationPage == LIBRARY_TAB_INDEX && !com.melox.player.ui.theme.LocalOs4Style.current)
                        AppRoute.ALBUMS, AppRoute.ARTISTS, AppRoute.FOLDERS,
                        AppRoute.FOLDER_DETAIL, AppRoute.INTERNAL_STORAGE, AppRoute.PLAYLIST_DETAIL -> true
                        else -> false
                    }
                    if (currentSearchAction != null || !destinationHasSearch) {
                        SideEffect { preparedSearchDestination = searchDestinationKey }
                    }
                    val pageNavigationHideMotion = if (currentRoute == AppRoute.ROOT) null
                        else secondaryPageEnterMotion(settings.navigationTransitionStyle)
                    val pageScaffoldContent: @Composable () -> Unit = {
                    PlayerScaffold(
                            playerNavigationOffset = {
                                playerNavigationOffset(
                                    source = playerTransition.overlayMiniPlayerBounds,
                                    target = playerTransition.fullPlayerBounds,
                                    progress = playerTransition.progress,
                                )
                            },
                            bottomSearchAction = currentSearchAction,
                            showBottomSearch = currentSearchAction != null ||
                                (destinationHasSearch && preparedSearchDestination != searchDestinationKey),
                            searchExpanded = currentSearchAction?.expanded?.value == true,
                            searchOpen = currentSearchAction?.searchOpen?.value == true,
                            selectedTab = playerPagerState.selectedPage,
                            onTabSelected = onNavigationTabSelected,
                            sidebarShortcutItems = listOf(
                                NavigationItem(stringResource(R.string.home_playlists_title), MiuixIcons.RecordingTape),
                                NavigationItem(albumsTitle, MiuixIcons.Album),
                                NavigationItem(artistsTitle, MiuixIcons.Contacts),
                                NavigationItem(foldersTitle, MiuixIcons.Folder),
                            ),
                            selectedSidebarShortcut = selectedSidebarShortcut(desiredNavBackStack, sidebarRoutes),
                            onSidebarShortcutSelected = { index ->
                                val target = sidebarRoutes[index]
                                if (currentRoute != target) {
                                    sidebarEntryRoute = target
                                    currentRoute = target
                                }
                            },
                            navigationHideMotion = pageNavigationHideMotion,
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
                                    (requestedBottomBarStyle == BottomBarStyle.NORMAL || requestedBottomBarStyle == BottomBarStyle.LIQUID_GLASS),
                            persistedNavigationRailExpanded = settings.navigationRailExpanded,
                            onNavigationRailExpandedChange = viewModel::setNavigationRailExpanded,
                            bottomBarStyle = renderedBottomBarStyle,
                            smallPlayerBar = renderedSmallPlayerBar,
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
                                    artworkState = artworkState,
                                    circularArtwork = settings.circularArtwork,
                                    squareArtwork = settings.coverShape != CoverShape.DEFAULT,
                                    showMiniPlayerLyrics = settings.showMiniPlayerLyrics,
                                    lyricAnimationMode = settings.lyricAnimationMode,
                                    chrome = chrome,
                                    onChromeChanged = { miniPlayerChrome = it },
                                    interactiveHighlight = miniPlayerInteractiveHighlight,
                                    onOpen = { scale, offsetX ->
                                        playerTransition.open(
                                            miniPlayerScale = scale,
                                            miniPlayerOffsetX = offsetX,
                                            requireFreshMiniFrame =
                                                chrome.liquidGlassActive && chrome.backdrop != null,
                                        )
                                    },
                                    onOpenQueue = { showQueue = true },
                                    onPlayerDragStart = { scale, offsetX ->
                                        playerTransition.beginMiniPlayerDrag(
                                            miniPlayerScale = scale,
                                            miniPlayerOffsetX = offsetX,
                                            requireFreshMiniFrame =
                                                chrome.liquidGlassActive && chrome.backdrop != null,
                                        )
                                    },
                                    onPlayerDrag = playerTransition::dragMiniPlayerBy,
                                    onPlayerDragEnd = playerTransition::endMiniPlayerDrag,
                                    onPlayerDragCancel = playerTransition::cancelDrag,
                                    capturedPressScale = playerTransition.capturedMiniPlayerPressScale,
                                    capturedSwipeTranslationX = playerTransition.capturedMiniPlayerOffsetX,
                                    playerLayer = miniPlayerLayer,
                                    playerContentLayer = miniPlayerContentLayer,
                                    frameRecordingGeneration = frameRecordingGeneration,
                                    recordForTransition = playerTransition.isMounted,
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
                                    playerSheetProgress = { playerTransition.progress },
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
                            Box(
                                modifier = Modifier.fillMaxSize().onGloballyPositioned {
                                    playerContentBounds = it.boundsInRoot()
                                },
                            ) {
                                val songsIndexBottomPadding = maxOf(
                                    retainedRootBottomPadding + rootIndexBottomSpacing,
                                    if (playerTransition.miniPlayerBounds.width > 0f && playerContentBounds.width > 0f) {
                                        with(density) {
                                            (playerContentBounds.bottom - playerTransition.miniPlayerBounds.top)
                                                .coerceAtLeast(0f).toDp()
                                        } + 24.dp + top.yukonga.miuix.kmp.basic.ButtonDefaults.MinHeight
                                    } else {
                                        0.dp
                                    },
                                )
                                var retainedSongsIndexBottomPadding by remember(
                                    windowSize, density, miniPlayerUsesNormalChrome,
                                ) { mutableStateOf(songsIndexBottomPadding) }
                                if (currentRoute == AppRoute.ROOT) {
                                    SideEffect {
                                        retainedSongsIndexBottomPadding = songsIndexBottomPadding
                                    }
                                }
                                CompositionLocalProvider(
                                    LocalAlphabetIndexBottomPadding provides if (currentRoute == AppRoute.ROOT) {
                                        songsIndexBottomPadding
                                    } else {
                                        retainedSongsIndexBottomPadding
                                    },
                                ) {
                                PredictiveNavDisplay(
                                    backStack = navBackStack,
                                    predictiveBackEnabled =
                                        settings.predictiveBackEnabled && !showQueue,
                                    backEnabled = !playerTransition.isMounted,
                                    withinPageBackEnabled = currentSearchAction?.searchOpen?.value == true ||
                                        (currentRoute == AppRoute.INTERNAL_STORAGE &&
                                        internalStoragePath != INTERNAL_STORAGE_ROOT && !showQueue),
                                    transitionStyle = settings.navigationTransitionStyle,
                                    isDark = isDark,
                                    onBack = {
                                        if (currentSearchAction?.searchOpen?.value == true) currentSearchAction.onClick.value()
                                        else navigateBack()
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    entry<AppNavDestination>(
                                        contentKey = AppNavDestination::entryContentKey,
                                        transition = if (
                                            landscape && shouldUseImmediateOs4Navigation(windowWidth, windowHeight, requestedBottomBarStyle) &&
                                            sidebarEntryRoute != null
                                        ) ImmediateNavigationTransition else null,
                                        swipeDismiss = if (currentRoute == AppRoute.INTERNAL_STORAGE &&
                                            internalStoragePath != INTERNAL_STORAGE_ROOT
                                        ) NavSwipeDirection.None else null,
                                    ) { route ->
                                        val transitionScope = LocalNavTransitionScope.current
                                        LaunchedEffect(route.route, sidebarEntryRoute, transitionScope) {
                                            if (route.route != sidebarEntryRoute) return@LaunchedEffect
                                            snapshotFlow {
                                                !transitionScope.isRunning && transitionScope.relativeDepth == 0f
                                            }.first { it }
                                            // End the immediate sidebar switch; subsequent pushes and pops use the normal transition.
                                            if (sidebarEntryRoute == route.route) sidebarEntryRoute = null
                                        }
                                        CompositionLocalProvider(LocalBottomSearchOwner provides route.entryContentKey()) {
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
                                                            listState = themeSettingsListState,
                                                            scrollBehavior =
                                                                themeSettingsScrollBehavior,
                                                            onBack = navigateBack,
                                                            onThemeStyleChange = viewModel::setThemeStyle,
                                                            onThemeModeChange =
                                                                viewModel::setThemeMode,
                                                            onImmersiveModeChange =
                                                                viewModel::setImmersiveMode,
                                                            onHideStatusBarChange =
                                                                viewModel::setHideStatusBar,
                                                            onDynamicColorChange =
                                                                viewModel::setDynamicColorEnabled,
                                                            onDynamicColorSourceChange =
                                                                viewModel::setDynamicColorSource,
                                                            onPlaybackBackgroundStyleChange =
                                                                viewModel::setPlaybackBackgroundStyle,
                                                            onDynamicFlowStyleChange =
                                                                viewModel::setDynamicFlowStyle,
                                                            onCoverShapeChange =
                                                                viewModel::setCoverShape,
                                                            onArtworkFlipAnimationChange =
                                                                viewModel::setArtworkFlipAnimation,
                                                            onShowSongListArtworkChange =
                                                                viewModel::setShowSongListArtwork,
                                                            onShowSongListFileNameChange =
                                                                viewModel::setShowSongListFileName,
                                                            onShowSongListDurationChange =
                                                                viewModel::setShowSongListDuration,
                                                            onPredictiveBackChange =
                                                                viewModel::setPredictiveBackEnabled,
                                                            onNavigationTransitionStyleChange =
                                                                viewModel::setNavigationTransitionStyle,
                                                            onHapticFeedbackEnabledChange =
                                                                viewModel::setHapticFeedbackEnabled,
                                                            onOpenInterfaceStyle = {
                                                                interfaceStyleListState.requestScrollToItem(0)
                                                                interfaceStyleScrollBehavior.state.heightOffset = 0f
                                                                interfaceStyleScrollBehavior.state.contentOffset = 0f
                                                                currentRoute = AppRoute.INTERFACE_STYLE
                                                            },
                                                            onOpenMainBackground = {
                                                                currentRoute = AppRoute.MAIN_BACKGROUND
                                                            },
                                                        )

                                                    AppRoute.INTERFACE_STYLE ->
                                                        InterfaceStyleSettingsScreen(
                                                            settings = settings,
                                                            bottomContentPadding = routeBottomPadding,
                                                            liquidGlassSupported = liquidGlassSupported,
                                                            listState = interfaceStyleListState,
                                                            scrollBehavior = interfaceStyleScrollBehavior,
                                                            onBack = navigateBack,
                                                            onBlurChange =
                                                                viewModel::setBlurEnabled,
                                                            onProgressiveTopBarBlurChange =
                                                                viewModel::setProgressiveTopBarBlurEnabled,
                                                            onSmallPlayerBarChange =
                                                                viewModel::setSmallPlayerBar,
                                                            onHideBottomBarChange =
                                                                viewModel::setHideBottomBar,
                                                            onBottomBarStyleChange = viewModel::setBottomBarStyle,
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
                                                            onLyricsFormatPriorityChange =
                                                                viewModel::setLyricsFormatPriority,
                                                            onShowMiniPlayerLyricsChange =
                                                                viewModel::setShowMiniPlayerLyrics,
                                                            onShowArtworkLyricsChange =
                                                                viewModel::setShowArtworkLyrics,
                                                            onShowMusicTagEditorChange =
                                                                viewModel::setShowMusicTagEditor,
                                                            onShowLyricoEditorChange =
                                                                viewModel::setShowLyricoEditor,
                                                            onShowLunaBeatEditorChange =
                                                                viewModel::setShowLunaBeatEditor,
                                                            onOpenLyricsInterface = {
                                                                currentRoute = AppRoute.LYRICS_INTERFACE
                                                            },
                                                        )

                                                    AppRoute.LYRICS_INTERFACE -> {
                                                        val currentLyrics by viewModel.lyricsState.collectAsStateWithLifecycle()
                                                        LyricsInterfaceSettingsScreen(
                                                            settings = settings,
                                                            centerLyricsEnabled = !currentLyrics.isDuetForMedia(compactPlayback.currentItem?.mediaId),
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
                                                            onShowLyricsRomanizationChange =
                                                                viewModel::setShowLyricsRomanization,
                                                            onLyricsTranslationLanguageChange =
                                                                viewModel::setLyricsTranslationLanguage,
                                                            onLyricFontScaleChange =
                                                                viewModel::setLyricFontScale,
                                                            onLyricFontWeightChange =
                                                                viewModel::setLyricFontWeight,
                                                            onLyricBlurEnabledChange =
                                                                viewModel::setLyricBlurEnabled,
                                                            onLyricAnimationModeChange =
                                                                viewModel::setLyricAnimationMode,
                                                            onHideControlsOnLyricsChange =
                                                                viewModel::setHideControlsOnLyrics,
                                                        )

                                                    }

                                                    AppRoute.SCAN_SETTINGS ->
                                                        ScanMusicScreen(
                                                            settings = settings,
                                                            scanStatus = uiState.scanStatus,
                                                            bottomContentPadding =
                                                                routeBottomPadding,
                                                            onBack = navigateBack,
                                                            onSongListPullToRefreshChange =
                                                                viewModel::setSongListPullToRefresh,
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
                                                            onOpenHiddenSongs = {
                                                                currentRoute = AppRoute.HIDDEN_SONGS
                                                            },
                                                            onStartScan = startManualScan,
                                                            onClearMusicLibrary =
                                                                viewModel::clearMusicLibrary,
                                                        )

                                                    AppRoute.BLOCKED_FOLDERS ->
                                                        BlockedFoldersScreen(
                                                            paths = hiddenFolderPaths,
                                                            displayPaths = hiddenFolderAddresses,
                                                            bottomContentPadding = routeBottomPadding,
                                                            onBack = navigateBack,
                                                            onUnblock = viewModel::removeBlockedFolderPath,
                                                        )

                                                    AppRoute.HIDDEN_SONGS ->
                                                        HiddenSongsScreen(
                                                            tracks = hiddenTracks,
                                                            settings = settings,
                                                            bottomContentPadding = routeBottomPadding,
                                                            selectionExitRequest = detailSelectionExitRequest,
                                                            onSelectionModeChange = { active ->
                                                                if (currentRoute == AppRoute.HIDDEN_SONGS) {
                                                                    detailSelectionActive = active
                                                                }
                                                            },
                                                            onBack = navigateBack,
                                                            onRestore = viewModel::restoreTracks,
                                                            onRestoreAll = {
                                                                viewModel.restoreTracks(hiddenTrackUris)
                                                            },
                                                            onPlayNext = viewModel::playNext,
                                                            onAppendToQueue = viewModel::appendToQueue,
                                                            onAddToPlaylist = { track ->
                                                                openPlaylistPicker(listOf(track))
                                                            },
                                                            onGoToAlbum = openTrackAlbum,
                                                            artistGroups = uiState.artists,
                                                            onGoToArtist = openTrackArtist,
                                                            onExternalEditReturned = viewModel::refreshTrackAfterExternalEdit,
                                                        )

                                                    AppRoute.MUSIC_STATISTICS ->
                                                        MusicStatisticsScreen(
                                                            tracks = uiState.tracks,
                                                            bottomContentPadding =
                                                                routeBottomPadding,
                                                            onBack = navigateBack,
                                                        )

                                                    AppRoute.AUDIO_OUTPUT -> {
                                                        val playback by viewModel.playbackState.collectAsStateWithLifecycle()
                                                        val highPrecision by viewModel.highPrecisionOutput.collectAsStateWithLifecycle()
                                                        val pauseFade by viewModel.playbackPauseFade.collectAsStateWithLifecycle()
                                                        AudioOutputScreen(
                                                            settings = settings,
                                                            bottomContentPadding = routeBottomPadding,
                                                            listState = audioOutputListState,
                                                            scrollBehavior = audioOutputScrollBehavior,
                                                            onBack = navigateBack,
                                                            playbackSpeed = playback.playbackSpeed,
                                                            audioSessionId = playback.audioSessionId,
                                                            highPrecisionOutput = highPrecision,
                                                            playbackPauseFade = pauseFade,
                                                            onAudioFocusEnabledChange = viewModel::setAudioFocusEnabled,
                                                            onTransientAudioFocusBehaviorChange = viewModel::setTransientAudioFocusBehavior,
                                                            onPlaybackSpeedChange = viewModel::setPlaybackSpeed,
                                                            onHighPrecisionOutputChange = viewModel::setHighPrecisionOutput,
                                                            onPlaybackPauseFadeChange = viewModel::setPlaybackPauseFade,
                                                        )
                                                    }

                                                    AppRoute.PLAYBACK_BEHAVIOR -> PlaybackBehaviorScreen(
                                                        settings = settings,
                                                        bottomContentPadding = routeBottomPadding,
                                                        listState = playbackBehaviorListState,
                                                        scrollBehavior = playbackBehaviorScrollBehavior,
                                                        onBack = navigateBack,
                                                        onAutoPlayOnStartChange = viewModel::setAutoPlayOnStart,
                                                        onTerminateOnTaskRemovedChange = viewModel::setTerminateOnTaskRemoved,
                                                        onShowNotificationPlaybackModeButtonChange =
                                                            viewModel::setShowNotificationPlaybackModeButton,
                                                        onShowNotificationCloseButtonChange =
                                                            viewModel::setShowNotificationCloseButton,
                                                    )

                                                    AppRoute.ABOUT -> AboutScreen(
                                                        bottomContentPadding = routeBottomPadding,
                                                        customBackgroundSelected = settings.customBackgroundId != null,
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
                                                                    onSongListTrackClick,
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
                                                                showLunaBeatEditor = settings.showLunaBeatEditor,
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
                                                                    onSongListTrackClick,
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
                                                                showLunaBeatEditor = settings.showLunaBeatEditor,
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
                                                                currentTrackContentUri = compactPlayback.currentItem?.contentUri,
                                                                miniPlayerBounds = playerTransition.miniPlayerBounds,
                                                                playerContentBounds = playerContentBounds,
                                                                onShufflePlay = viewModel::shuffleTracks,
                                                                folder = it,
                                                                sortConfig = settings.folderTrackSortConfigs[it.key]
                                                                    ?: MusicSortConfig(),
                                                                onSortConfigChange = { config ->
                                                                    viewModel.setFolderTrackSortConfig(it.key, config)
                                                                },
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
                                                                    onSongListTrackClick,
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
                                                                showLunaBeatEditor = settings.showLunaBeatEditor,
                                                            )
                                                        }
                                                    }

                                                    AppRoute.INTERNAL_STORAGE -> {
                                                        val key = internalStoragePath
                                                        val directory = internalStorageHierarchy.directories[key]
                                                            ?: internalStorageHierarchy.directories.getValue(
                                                                internalStorageHierarchy.closestDirectoryKey(key),
                                                            )
                                                        val songSortConfig = settings.folderTrackSortConfigs[directory.key]
                                                            ?: MusicSortConfig()
                                                        val children = remember(internalStorageHierarchy, directory.key) {
                                                            internalStorageHierarchy.childDirectories(directory.key)
                                                        }
                                                        val trail = internalStorageDirectoryTrail(directory.key)
                                                        val storageTitle = stringResource(R.string.folder_internal_storage)
                                                        val breadcrumbs = remember(trail, internalStorageHierarchy, storageTitle) {
                                                            trail.map { ancestor ->
                                                                val node = internalStorageHierarchy.directories.getValue(ancestor)
                                                                BreadcrumbItem(
                                                                    path = if (ancestor == INTERNAL_STORAGE_ROOT) INTERNAL_STORAGE_ROOT else node.path.substringAfterLast('/'),
                                                                    text = if (ancestor == INTERNAL_STORAGE_ROOT) storageTitle else node.name,
                                                                )
                                                            }
                                                        }
                                                        val folder = remember(directory, storageTitle) {
                                                            directory.asFolderGroup(
                                                                if (directory.key == INTERNAL_STORAGE_ROOT) storageTitle else directory.name,
                                                            )
                                                        }
                                                        folder.let {
                                                            FolderDetailScreen(
                                                                currentTrackContentUri = compactPlayback.currentItem?.contentUri,
                                                                miniPlayerBounds = playerTransition.miniPlayerBounds,
                                                                playerContentBounds = playerContentBounds,
                                                                onShufflePlay = viewModel::shuffleTracks,
                                                                folder = it,
                                                                sortConfig = songSortConfig,
                                                                onSortConfigChange = { config ->
                                                                    viewModel.setFolderTrackSortConfig(it.key, config)
                                                                },
                                                                artistGroups = uiState.artists,
                                                                currentTrackId = currentTrackId,
                                                                bottomContentPadding =
                                                                    routeBottomPadding,
                                                                childFolders = remember(children) { children.map { it.asFolderGroup() } },
                                                                scanStatus = uiState.scanStatus,
                                                                childSongCounts = remember(children) { children.associate { it.key to it.songCount } },
                                                                breadcrumbItems = breadcrumbs,
                                                                titleOverride = storageTitle,
                                                                onDirectoryClick = { internalStoragePath = it },
                                                                onBreadcrumbClick = { index ->
                                                                    trail.getOrNull(index)?.let { internalStoragePath = it }
                                                                },
                                                                onBlockFolder = viewModel::addBlockedFolderPath,
                                                                onBack = navigateBack,
                                                                selectionExitRequest =
                                                                    detailSelectionExitRequest,
                                                                onSelectionModeChange = { active ->
                                                                    if (
                                                                        currentRoute ==
                                                                        AppRoute.INTERNAL_STORAGE
                                                                    ) {
                                                                        detailSelectionActive = active
                                                                    }
                                                                },
                                                                onTrackClick =
                                                                    onSongListTrackClick,
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
                                                                showLunaBeatEditor = settings.showLunaBeatEditor,
                                                            )
                                                        }
                                                    }

                                                    AppRoute.ALBUMS, AppRoute.ARTISTS, AppRoute.FOLDERS ->
                                                        libraryContent(outerPadding, navigationRailExpanded, libraryRoutes.indexOf(route.route), navigateBack)
                                                    AppRoute.PLAYLISTS ->
                                                        PlaylistLibraryScreen(
                                                            playlists = playlistState.playlists,
                                                            loaded = playlistState.loaded,
                                                            onMovePlaylists = viewModel::movePlaylists,
                                                            landscape = windowWidth > windowHeight,
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
                                                                currentTrackContentUri = compactPlayback.currentItem?.contentUri,
                                                                miniPlayerBounds = playerTransition.miniPlayerBounds,
                                                                playerContentBounds = playerContentBounds,
                                                                onShufflePlay = viewModel::shuffleTracks,
                                                                playlist = it,
                                                                sortConfig = settings.playlistSortConfigs[it.id]
                                                                    ?: com.melox.player.data.playlist.PlaylistSortConfig(),
                                                                onSortConfigChange = { config ->
                                                                    viewModel.setPlaylistSortConfig(it.id, config)
                                                                },
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
                                                                onTrackClick = onSongListTrackClick,
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
                                                                showLunaBeatEditor = settings.showLunaBeatEditor,
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
                                }
                        }
                    }
                    val retainedScaffoldContent = com.melox.player.ui.navigation.rememberNavigationHostContent(pageScaffoldContent)
                    retainedScaffoldContent()
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
                            artworkState = artworkState,
                            onArtworkDisplaySizeChanged = { artworkDisplaySizePx = it },
                            circularArtwork = settings.circularArtwork,
                            squareArtwork = settings.coverShape != CoverShape.DEFAULT,
                            tracks = uiState.tracks,
                            artistGroups = uiState.artists,
                            playbackBackgroundStyle =
                                settings.playbackBackgroundStyle,
                            dynamicFlowBackgroundState = dynamicFlowBackgroundState,
                            playbackBackgroundDark = playerBackgroundDark,
                            dynamicColorEnabled = settings.dynamicColorEnabled,
                            playbackContentColor = rememberPlaybackContentColor(
                                settings = settings,
                                playbackArtworkColor = playbackArtworkColor,
                                backgroundDark = playerBackgroundDark,
                                hasItem = compactPlayback.currentItem != null,
                            ),
                            lyricFontScale = settings.lyricFontScale,
                            lyricFontWeight = settings.lyricFontWeight,
                            lyricAnimationMode = settings.lyricAnimationMode,
                            lyricBlurEnabled = settings.lyricBlurEnabled,
                            centerLyrics = settings.centerLyrics,
                            leftAlignPlayerTitle = settings.leftAlignPlayerTitle,
                            hideControlsOnLyrics = settings.hideControlsOnLyrics,
                            immersiveMode = settings.immersiveMode,
                            showLyricsTranslation = settings.showLyricsTranslation,
                            showLyricsRomanization = settings.showLyricsRomanization,
                            lyricsTranslationLanguage = settings.lyricsTranslationLanguage,
                            showArtworkLyrics = settings.showArtworkLyrics,
                            showMusicTagEditor = settings.showMusicTagEditor,
                            showLyricoEditor = settings.showLyricoEditor,
                            showLunaBeatEditor = settings.showLunaBeatEditor,
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
                            playerActive = playerTransition.isMounted,
                            blockUnderlyingInput = playerTransition.blocksUnderlyingInput,
                            lyricsPagingEnabled = playerTransition.isFullyExpanded,
                            drawInPlace = playerTransition.fullPlayerDrawsInPlace,
                            recordInPlace = playerTransition.fullPlayerFramesNeedRecording,
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
                        hasItem = true,
                        modifier = Modifier.zIndex(2f),
                        collapsedCornerRadius =
                            miniPlayerChrome?.cornerRadius ?: if (miniPlayerUsesNormalChrome) 18.dp else 32.dp,
                        liquidGlassActive =
                            miniPlayerChrome?.let { it.liquidGlassActive && it.backdrop != null } == true,
                    )
                    PlayerSheetMiniControlsInputOverlay(
                        transition = playerTransition,
                        hasItem = compactPlayback.currentItem != null,
                        normalChrome = miniPlayerUsesNormalChrome,
                        smallPlayerBar = renderedSmallPlayerBar,
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
                        miniPlayerInteractiveHighlight = miniPlayerInteractiveHighlight,
                        collapsedCornerRadius =
                            miniPlayerChrome?.cornerRadius ?: if (miniPlayerUsesNormalChrome) 18.dp else 32.dp,
                        floatingMiniPlayer = !miniPlayerUsesNormalChrome,
                        isDark = isDark,
                    )
                    PlayerSheetArtworkOverlay(
                        playback = compactPlayback,
                        artworkState = artworkState,
                        transition = playerTransition,
                        collapsedCornerRadius = if (miniPlayerChrome?.style == BottomBarStyle.OS4) {
                            if (settings.circularArtwork) miniPlayerChrome!!.artworkSize / 2
                            else miniPlayerChrome!!.os4ArtworkCornerRadius
                        } else miniPlayerArtworkCornerRadius(
                            normalChrome = miniPlayerUsesNormalChrome,
                            smallPlayerBar = renderedSmallPlayerBar,
                            circular = settings.circularArtwork,
                        ),
                        enabled = sharedPlayerArtworkEnabled,
                        circularArtwork = settings.circularArtwork,
                        squareArtwork = settings.coverShape != CoverShape.DEFAULT,
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
    artworkState: ArtworkTransitionState<Bitmap>,
    onArtworkDisplaySizeChanged: (Int) -> Unit,
    circularArtwork: Boolean,
    squareArtwork: Boolean,
    tracks: List<MusicTrack>,
    artistGroups: List<ArtistGroup>,
    playbackBackgroundStyle: PlaybackBackgroundStyle,
    dynamicFlowBackgroundState: DynamicFlowBackgroundState,
    playbackBackgroundDark: Boolean,
    dynamicColorEnabled: Boolean,
    playbackContentColor: androidx.compose.ui.graphics.Color,
    lyricFontScale: Float,
    lyricFontWeight: Int,
    lyricAnimationMode: LyricAnimationMode,
    lyricBlurEnabled: Boolean,
    centerLyrics: Boolean,
    leftAlignPlayerTitle: Boolean,
    hideControlsOnLyrics: Boolean,
    immersiveMode: Boolean,
    showLyricsTranslation: Boolean,
    showLyricsRomanization: Boolean,
    lyricsTranslationLanguage: LyricsTranslationLanguage,
    showArtworkLyrics: Boolean,
    showMusicTagEditor: Boolean,
    showLyricoEditor: Boolean,
    showLunaBeatEditor: Boolean,
    onDismiss: () -> Unit,
    onOpenQueue: () -> Unit,
    onAddToPlaylist: (MusicTrack) -> Unit,
    onGoToAlbum: (MusicTrack) -> Unit,
    onGoToArtist: (ArtistGroup) -> Unit,
    backgroundLayer: GraphicsLayer,
    contentLayer: GraphicsLayer,
    frameRecordingGeneration: Int,
    interactionEnabled: Boolean,
    playerActive: Boolean,
    blockUnderlyingInput: Boolean,
    lyricsPagingEnabled: Boolean,
    drawInPlace: Boolean,
    recordInPlace: Boolean,
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
    modifier: Modifier = Modifier,
) {
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()
    val lyrics by viewModel.lyricsState.collectAsStateWithLifecycle()
    val sleepTimerState by viewModel.sleepTimerState.collectAsStateWithLifecycle()
    val sleepTimerSelectionSeconds by viewModel.sleepTimerSelectionSeconds.collectAsStateWithLifecycle()
    val autoExtendSleepTimer by viewModel.autoExtendSleepTimer.collectAsStateWithLifecycle()
    val playbackPauseFade by viewModel.playbackPauseFade.collectAsStateWithLifecycle()
    val currentTrack = remember(tracks, playback.currentItem?.trackId) {
        val trackId = playback.currentItem?.trackId
        tracks.firstOrNull { it.id == trackId }
    }
    FullPlayerScreen(
        playback = playback,
        artworkState = artworkState,
        onArtworkDisplaySizeChanged = onArtworkDisplaySizeChanged,
        circularArtwork = circularArtwork,
        squareArtwork = squareArtwork,
        currentTrack = currentTrack,
        lyrics = lyrics,
        playbackBackgroundStyle = playbackBackgroundStyle,
        dynamicFlowBackgroundState = dynamicFlowBackgroundState,
        playbackBackgroundDark = playbackBackgroundDark,
        dynamicColorEnabled = dynamicColorEnabled,
        playbackContentColor = playbackContentColor,
        lyricFontScale = lyricFontScale,
        lyricFontWeight = lyricFontWeight,
        lyricAnimationMode = lyricAnimationMode,
        lyricBlurEnabled = lyricBlurEnabled,
        centerLyrics = centerLyrics,
        leftAlignPlayerTitle = leftAlignPlayerTitle,
        hideControlsOnLyrics = hideControlsOnLyrics,
        immersiveMode = immersiveMode,
        onImmersiveModeChange = viewModel::setImmersiveMode,
        showLyricsTranslation = showLyricsTranslation,
        showLyricsRomanization = showLyricsRomanization,
        lyricsTranslationLanguage = lyricsTranslationLanguage,
        showArtworkLyrics = showArtworkLyrics,
        onLyricFontScaleChange = viewModel::setLyricFontScale,
        onLyricFontWeightChange = viewModel::setLyricFontWeight,
        onLyricAnimationModeChange = viewModel::setLyricAnimationMode,
        onLyricBlurEnabledChange = viewModel::setLyricBlurEnabled,
        onCenterLyricsChange = viewModel::setCenterLyrics,
        onLeftAlignPlayerTitleChange = viewModel::setLeftAlignPlayerTitle,
        onHideControlsOnLyricsChange = viewModel::setHideControlsOnLyrics,
        onShowLyricsTranslationChange = viewModel::setShowLyricsTranslation,
        onShowLyricsRomanizationChange = viewModel::setShowLyricsRomanization,
        onLyricsTranslationLanguageChange = viewModel::setLyricsTranslationLanguage,
        onDismiss = onDismiss,
        onTogglePlayPause = viewModel::togglePlayPause,
        onPrevious = viewModel::previous,
        onNext = viewModel::next,
        onSeek = viewModel::seekTo,
        onCyclePlaybackMode = viewModel::cyclePlaybackMode,
        onPlaybackSpeedChange = viewModel::setPlaybackSpeed,
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
        showLunaBeatEditor = showLunaBeatEditor,
        backgroundLayer = backgroundLayer,
        contentLayer = contentLayer,
        frameRecordingGeneration = frameRecordingGeneration,
        interactionEnabled = interactionEnabled,
        playerActive = playerActive,
        blockUnderlyingInput = blockUnderlyingInput,
        lyricsPagingEnabled = lyricsPagingEnabled,
        drawInPlace = drawInPlace,
        recordInPlace = recordInPlace,
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
        modifier = modifier,
    )
}

@Composable
private fun MiniPlayerHost(
    viewModel: MeloxViewModel,
    artworkState: ArtworkTransitionState<Bitmap>,
    circularArtwork: Boolean,
    squareArtwork: Boolean,
    showMiniPlayerLyrics: Boolean,
    lyricAnimationMode: LyricAnimationMode,
    chrome: MiniPlayerChrome,
    onChromeChanged: (MiniPlayerChrome) -> Unit,
    interactiveHighlight: InteractiveHighlight,
    onOpen: (Float, Float) -> Unit,
    onOpenQueue: () -> Unit,
    onPlayerDragStart: (Float, Float) -> Unit,
    onPlayerDrag: (Float, Float) -> Unit,
    onPlayerDragEnd: (Float, Float) -> Unit,
    onPlayerDragCancel: () -> Unit,
    playerLayer: GraphicsLayer,
    playerContentLayer: GraphicsLayer,
    frameRecordingGeneration: Int,
    recordForTransition: Boolean,
    drawInPlace: Boolean,
    surfaceVisible: Boolean,
    sharedArtworkVisible: Boolean,
    playerSheetProgress: () -> Float,
    capturedPressScale: Float?,
    capturedSwipeTranslationX: Float?,
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
        artworkState = artworkState,
        circularArtwork = circularArtwork,
        squareArtwork = squareArtwork,
        chrome = chrome,
        interactiveHighlight = interactiveHighlight,
        onOpen = { scale, offsetX ->
            if (playback.currentItem != null) onOpen(scale, offsetX)
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
        recordForTransition = recordForTransition,
        drawInPlace = drawInPlace,
        surfaceVisible = surfaceVisible,
        sharedArtworkVisible = sharedArtworkVisible,
        playerSheetProgress = playerSheetProgress,
        capturedPressScale = capturedPressScale,
        capturedSwipeTranslationX = capturedSwipeTranslationX,
        onLayerRecorded = onLayerRecorded,
        onPlayerBoundsChanged = onPlayerBoundsChanged,
        onPlayerContentBoundsChanged = onPlayerContentBoundsChanged,
        onPlayerControlsBoundsChanged = onPlayerControlsBoundsChanged,
        onArtworkBoundsChanged = onArtworkBoundsChanged,
        subtitleContent = { mediaId, artist, style, color, textStartPadding ->
            MiniPlayerSubtitleHost(
                viewModel = viewModel,
                mediaId = mediaId,
                artist = artist,
                style = style,
                color = color,
                textStartPadding = textStartPadding,
                enabled = showMiniPlayerLyrics,
                active = drawInPlace && surfaceVisible && !recordForTransition,
                animationMode = lyricAnimationMode,
            )
        },
    )
}

@Composable
private fun MiniPlayerSubtitleHost(
    viewModel: MeloxViewModel,
    mediaId: String?,
    artist: String,
    style: androidx.compose.ui.text.TextStyle,
    color: androidx.compose.ui.graphics.Color,
    textStartPadding: Dp,
    enabled: Boolean,
    active: Boolean,
    animationMode: LyricAnimationMode,
) {
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()
    val lyrics by viewModel.lyricsState.collectAsStateWithLifecycle()
    val document = if (enabled) {
        compactLyricsDocument(
            lyrics, mediaId.takeIf { it == playback.currentItem?.mediaId },
        )
    } else {
        null
    }
    key(mediaId, playback.currentItem?.mediaId) {
        MiniPlayerLyrics(
            document = document,
            playback = playback,
            artist = artist,
            style = style,
            color = color,
            active = active,
            animationMode = animationMode,
            modifier = Modifier.fillMaxWidth(),
            textStartPadding = textStartPadding,
        )
    }
}

private fun playerSheetArtworkIsOffscreen(
    transition: com.melox.player.ui.component.playback.PlayerSheetTransitionState,
): Boolean = transition.separateArtworkOverlayReady &&
    !sharedArtworkTargetIsOnscreen(
        artworkBounds = transition.fullArtworkBounds,
        viewportBounds = transition.fullPlayerBounds,
    )

internal fun playbackArtworkPrefetchItems(playback: PlaybackUiState): List<PlaybackQueueItem> {
    val queue = playback.queue
    val currentIndex = playback.currentIndex
    if (currentIndex !in queue.indices) return emptyList()
    return listOf(
        currentIndex,
        (currentIndex + 1) % queue.size,
        (currentIndex - 1 + queue.size) % queue.size,
    ).distinct().map(queue::get)
}

@Composable
private fun PlaybackArtworkPrefetchEffect(
    viewModel: MeloxViewModel,
    playbackBackgroundStyle: PlaybackBackgroundStyle,
) {
    val playback = viewModel.compactPlaybackState.collectAsStateWithLifecycle().value
    val applicationContext = LocalContext.current.applicationContext
    val artworkPrefetchSizePx = with(LocalDensity.current) {
        PLAYER_FULL_ARTWORK_REQUEST_SIZE.roundToPx()
    }
    val placeholderArtworkResId = PLACEHOLDER_ARTWORK_RES_ID
    LaunchedEffect(
        playback.currentIndex,
        playback.queue,
        artworkPrefetchSizePx,
        placeholderArtworkResId,
        playbackBackgroundStyle,
    ) {
        playbackArtworkPrefetchItems(playback).forEachIndexed { index, item ->
            if (index == 0 &&
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
                foreground = index == 0,
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
    scrollBehavior: ScrollBehavior,
    contentDescription: String = stringResource(R.string.music_search_hint),
    onClick: () -> Unit,
    bottomActionActive: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val currentOnClick by rememberUpdatedState(onClick)
    var collapsing by remember { mutableStateOf(false) }
    val onSearchClick: () -> Unit = {
            collapsing = true
            scope.launch {
                try {
                    val state = scrollBehavior.state
                    val limit = state.heightOffsetLimit
                    if (limit != -Float.MAX_VALUE && limit < 0f) {
                        if (state.heightOffset > limit) {
                            AnimationState(initialValue = state.heightOffset).animateTo(
                                targetValue = limit,
                                animationSpec = scrollBehavior.snapAnimationSpec ?: tween(200),
                            ) {
                                state.heightOffset = value
                            }
                        }
                        state.heightOffset = limit
                        state.contentOffset = limit
                    }
                    currentOnClick()
                } finally {
                    collapsing = false
                }
            }
        }
    val effectiveOnSearchClick: () -> Unit = if (com.melox.player.ui.theme.LocalOs4Style.current) {
        { currentOnClick() }
    } else onSearchClick
    if (publishBottomSearchAction(bottomActionActive, visible || LocalGlassSearchPage.current?.transitionActive == true, visible, contentDescription, !collapsing, effectiveOnSearchClick)) return
    if (LocalOs4LandscapeNavigation.current != null) {
        val searchPage = LocalGlassSearchPage.current
        val materialProgress = top.yukonga.miuix.kmp.glass.LocalGlassTopAppBarContext.current?.materialProgress
        val searchHapticFeedback = LocalHapticFeedback.current
        SideEffect { searchPage?.materialProgress = materialProgress }
        val buttonAlpha = androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (visible) 0f else 1f,
            animationSpec = tween(400, easing = androidx.compose.animation.core.LinearEasing),
            label = "landscapeSearchButtonAlpha",
        )
        val density = LocalDensity.current
        val shadowOutsets = remember(density) {
            with(density) { popupShadowOutsets(top.yukonga.miuix.kmp.glass.GlassShadows.Regular) }
        }
        Box(Modifier.graphicsLayer {
            alpha = buttonAlpha.value
            outsets = shadowOutsets
        }) {
            top.yukonga.miuix.kmp.glass.GlassIconButton(
                onClick = {
                    searchHapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    effectiveOnSearchClick()
                },
                enabled = !visible && !collapsing,
                size = top.yukonga.miuix.kmp.basic.IconButtonDefaults.MinHeight,
                modifier = if (visible) Modifier.clearAndSetSemantics { } else Modifier,
            ) {
                Icon(imageVector = MiuixIcons.Search, contentDescription = contentDescription)
            }
        }
        return
    }
    IconButton(
        onClick = effectiveOnSearchClick,
        enabled = !collapsing,
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
                            Modifier.squircleBackground(
                                color = tabSelectedContainerColor(
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
    backEnabled: Boolean = true,
    onBackRequest: (() -> Unit)? = null,
) {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val ordinaryBackOwner = LocalOrdinaryBackDispatcherOwner.current
    val searchBackOwner = LocalWindowBackDispatcherOwner.current.takeIf { onBackRequest != null }
        ?: ordinaryBackOwner
        ?: checkNotNull(LocalNavigationEventDispatcherOwner.current)
    val ordinaryBackEnabled = onBackRequest == null && visible && backEnabled &&
        ordinaryBackOwner != null &&
        lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    // Activity 1.13 otherwise registers BackHandler in the disabled entry dispatcher.
    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides searchBackOwner) {
        BackHandler(enabled = ordinaryBackEnabled) {
            onVisibleChange(false)
        }
    }
    if (registerGlassSearchSession(visible && backEnabled, query, label, onQueryChange, onFocusedChange) { onVisibleChange(false) }) {
        return
    }
    val hasWallpaper = LocalCustomPageBackground.current != null
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
    val contentBackOwner = if (onBackRequest != null) searchBackOwner
        else checkNotNull(LocalNavigationEventDispatcherOwner.current)
    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides contentBackOwner) {
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
                        query = query,
                        onQueryChange = { newQuery ->
                            // Miuix clears its query when input focus is dismissed.
                            // A visible search field may deliberately remain unfocused
                            // while another root page is selected, so retain its query.
                            if (focused || newQuery.isNotEmpty() || query.isEmpty()) {
                                onQueryChange(newQuery)
                            }
                        },
                        onSearch = onQueryChange,
                        expanded = visible && focused,
                        onExpandedChange = onFocusedChange,
                        label = label,
                        color = MiuixTheme.colorScheme.surfaceContainerHigh.copy(
                            alpha = if (hasWallpaper || progressiveBlurActive) 0.8f else 1f,
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
        if (onBackRequest != null) {
            androidx.navigationevent.compose.NavigationBackHandler(
                state = androidx.navigationevent.compose.rememberNavigationEventState(
                    androidx.navigationevent.NavigationEventInfo.None,
                ),
                isBackEnabled = visible && backEnabled &&
                    lifecycleState.isAtLeast(Lifecycle.State.RESUMED),
                onBackCompleted = onBackRequest,
            )
        }
    }
}

internal fun shouldClearSearchFocusAfterImeDismissed(
    searchFocused: Boolean,
    imeVisible: Boolean,
    imeWasVisible: Boolean,
): Boolean = searchFocused && imeWasVisible && !imeVisible

@Composable
private fun PlayerPage(
    title: String,
    outerPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
    useSmallTopAppBar: Boolean,
    isRootPage: Boolean = true,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
    content: @Composable (PaddingValues, ScrollBehavior, indexTopPadding: androidx.compose.ui.unit.Dp) -> Unit,
) {
    val landscapeNavigation = LocalOs4LandscapeNavigation.current.takeIf { isRootPage }
    val replaceTitle = landscapeNavigation?.labels?.contains(title) == true
    val layoutDirection = LocalLayoutDirection.current
    var topBarCaptureCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val topBarBackdrop = rememberBlurBackdrop(captureCoordinates = { topBarCaptureCoordinates })

    PageScaffold(
            topBar = {
            BlurredBar(
                backdrop = topBarBackdrop,
                blurEnabled = topBarBackdrop != null,
                scrollBehavior = scrollBehavior,
            ) {
                val topBarColor = topBarBackdrop.miuixBarColor()
                if (!isRootPage) {
                    com.melox.player.ui.component.AdaptiveTopAppBar(
                        title = title,
                        scrollBehavior = scrollBehavior,
                        color = topBarColor,
                        navigationIcon = navigationIcon,
                        actions = actions,
                        bottomContent = bottomContent,
                    )
                } else if (useSmallTopAppBar) {
                    SmallTopAppBar(
                        title = title,
                        inlineTitleEnabled = !replaceTitle,
                        modifier = Modifier.stableTopAppBarInsets(topBarColor),
                        color = topBarColor,
                        navigationIcon = navigationIcon,
                        actions = actions,
                        scrollBehavior = scrollBehavior,
                        bottomContent = bottomContent,
                    )
                } else {
                    TopAppBar(
                        title = title,
                        modifier = Modifier.stableTopAppBarInsets(topBarColor),
                        color = topBarColor,
                        navigationIcon = navigationIcon,
                        actions = actions,
                        scrollBehavior = scrollBehavior,
                        bottomContent = bottomContent,
                    )
                }
            }
        },
        ) { innerPadding ->
        val indexTopPadding = fixedAlphabetIndexTopPadding()
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
                    bottom = if (LocalGlassSearchPage.current?.isExpanded == true) innerPadding.calculateBottomPadding()
                        else outerPadding.calculateBottomPadding(),
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
