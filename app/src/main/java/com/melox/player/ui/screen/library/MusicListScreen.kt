package com.melox.player.ui.screen.library

import com.melox.player.ui.component.rememberGlassBlurBackdrop
import top.yukonga.miuix.kmp.upstream.blur.layerBackdrop
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned

import com.melox.player.ui.component.LocalGlassSearchResults
import com.melox.player.ui.component.LocalPageAlphabetIndexPadding
import com.melox.player.ui.component.library.MusicLocationAction
import com.melox.player.ui.component.library.PreserveSortScrollPosition

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.melox.player.ui.component.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.melox.player.R
import com.melox.player.data.library.ArtistGroup
import com.melox.player.data.library.MusicSortConfig
import com.melox.player.data.library.MusicSortField
import com.melox.player.model.MusicTrack
import com.melox.player.model.ScanStatus
import com.melox.player.ui.component.library.AlphabetSections
import com.melox.player.ui.component.library.AlphabetSideBar
import com.melox.player.ui.component.library.MusicTrackRow
import com.melox.player.ui.component.library.TrackActionsOverlay
import com.melox.player.ui.component.library.toggleTrackSelection
import com.melox.player.ui.component.library.offsetSectionIndices
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import com.melox.player.ui.component.Icon
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.RefreshState
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.melox.player.ui.component.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MusicListScreen(
    onTrackClick: (List<MusicTrack>, Int) -> Unit,
    displayedTracks: List<MusicTrack>,
    queueTracks: List<MusicTrack> = displayedTracks,
    sectionIndexMap: Map<String, Int>,
    scanStatus: ScanStatus,
    currentTrackId: Long?,
    query: String,
    sortConfig: MusicSortConfig,
    onPlayNext: (MusicTrack) -> Unit,
    onAppendToQueue: (MusicTrack) -> Unit,
    onAddToPlaylist: (MusicTrack) -> Unit,
    onGoToAlbum: (MusicTrack) -> Unit,
    artistGroups: List<ArtistGroup>,
    onGoToArtist: (ArtistGroup) -> Unit,
    onExternalEditReturned: (Long) -> Unit,
    showMusicTagEditor: Boolean,
    showLyricoEditor: Boolean,
    showLunaBeatEditor: Boolean,
    showSaltAudioEditor: Boolean,
    showSaltAudioAnalyzer: Boolean,
    scrollBehavior: ScrollBehavior,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    topContentSpacing: Dp = 12.dp,
    showIndex: Boolean = true,
    selectionMode: Boolean = false,
    selectedTrackUris: Set<String> = emptySet(),
    onSelectionChange: ((Set<String>) -> Unit)? = null,
    onSelectionModeChange: ((Boolean) -> Unit)? = null,
    showLocateAction: Boolean = false,
    currentTrackContentUri: String? = null,
    miniPlayerBounds: Rect = Rect.Zero,
    playerContentBounds: Rect = Rect.Zero,
    leadingItemCount: Int = 0,
    leadingContent: LazyListScope.() -> Unit = {},
    emptyContent: (@Composable (Modifier) -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
) {
    val layoutDirection = LocalLayoutDirection.current
    val selectionAllowed = !LocalGlassSearchResults.current
    val showingSearchResults = LocalGlassSearchResults.current
    val selectingTracks = selectionAllowed && selectionMode
    val density = LocalDensity.current
    val locateActionSize = ButtonDefaults.MinHeight
    val trackIndex = displayedTracks.indexOfFirst { it.contentUri == currentTrackContentUri }
    val currentIndex = if (trackIndex < 0) -1 else trackIndex + leadingItemCount
    val listSectionIndexMap = remember(sectionIndexMap, leadingItemCount) {
        offsetSectionIndices(sectionIndexMap, leadingItemCount)
    }
    val showAlphabetIndex = showIndex &&
        displayedTracks.isNotEmpty() &&
        (
            sortConfig.field == MusicSortField.TITLE ||
                sortConfig.field == MusicSortField.ARTIST ||
                sortConfig.field == MusicSortField.FILE_NAME
        ) &&
        query.isBlank()
    val locateActionVisible = showLocateAction && currentIndex >= 0 && !selectingTracks &&
        miniPlayerBounds.width > 0f && playerContentBounds.width > 0f
    val locateBottomPadding = if (locateActionVisible) {
        with(density) {
            (playerContentBounds.bottom - miniPlayerBounds.top).coerceAtLeast(0f).toDp()
        } + 12.dp + locateActionSize
    } else {
        0.dp
    }
    PreserveSortScrollPosition(
        sortKey = sortConfig,
        query = query,
        itemCount = displayedTracks.size + leadingItemCount,
        scrollState = listState,
    ) {
        listState.requestScrollToItem(
            listState.firstVisibleItemIndex,
            listState.firstVisibleItemScrollOffset,
        )
    }
    var selectedTrack by remember { mutableStateOf<MusicTrack?>(null) }
    val startPadding = contentPadding.calculateStartPadding(layoutDirection)
    val endPadding = contentPadding.calculateEndPadding(layoutDirection)
    val listContentPadding = PaddingValues(
        start = if (onRefresh == null) startPadding else 0.dp,
        top = contentPadding.calculateTopPadding() + topContentSpacing,
        end = if (onRefresh == null) endPadding else 0.dp,
        bottom = maxOf(contentPadding.calculateBottomPadding(), locateBottomPadding) + 12.dp,
    )
    val sections = remember(sortConfig.descending) {
        if (sortConfig.descending) AlphabetSections.asReversed() else AlphabetSections
    }
    val displayedTrackUris = remember(displayedTracks) {
        displayedTracks.map(MusicTrack::contentUri).toSet()
    }
    LaunchedEffect(displayedTrackUris, selectedTrackUris) {
        if (selectedTrackUris.isNotEmpty()) {
            val retainedSelection = selectedTrackUris intersect displayedTrackUris
            if (retainedSelection != selectedTrackUris) {
                onSelectionChange?.invoke(retainedSelection)
            }
        }
    }

    var locationCaptureCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val locationBackdrop = if (showLocateAction) {
        rememberGlassBlurBackdrop(captureCoordinates = { locationCaptureCoordinates })
    } else null
    val listContent: @Composable () -> Unit = {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(locationBackdrop?.let {
                    Modifier.onGloballyPositioned { coordinates -> locationCaptureCoordinates = coordinates }.layerBackdrop(it)
                } ?: Modifier)
                .scrollEndHaptic()
                .overScrollVertical(scrollBehavior = scrollBehavior)
                .then(
                    if (onRefresh == null) {
                        Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                    } else {
                        Modifier
                    },
                ),
            state = listState,
            contentPadding = listContentPadding,
            overscrollEffect = null,
        ) {
            leadingContent()
            if (displayedTracks.isEmpty() && leadingItemCount == 0 && !showingSearchResults) {
                item(key = "empty_music") {
                    val emptyModifier = Modifier.fillParentMaxSize()
                    if (emptyContent != null) {
                        emptyContent(emptyModifier)
                    } else {
                        EmptyMusicState(
                            scanStatus = scanStatus,
                            query = query,
                            modifier = emptyModifier,
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = displayedTracks,
                    key = { _, track -> track.id },
                ) { index, track ->
                    MusicTrackRow(
                        track = track,
                        isCurrent = track.id == currentTrackId,
                        onClick = {
                            if (selectingTracks) {
                                onSelectionChange?.invoke(
                                    toggleTrackSelection(selectedTrackUris, track.contentUri),
                                )
                            } else {
                                resolveMusicPlaybackSelection(
                                    displayedTracks = displayedTracks,
                                    queueTracks = queueTracks,
                                    query = query,
                                    selectedIndex = index,
                                )?.let { (playbackTracks, playbackIndex) ->
                                    onTrackClick(playbackTracks, playbackIndex)
                                }
                            }
                        },
                        onMoreClick = { selectedTrack = track },
                        selectionMode = selectingTracks,
                        selected = track.contentUri in selectedTrackUris,
                        onLongClick = onSelectionChange?.takeIf { selectionAllowed }?.let { selectionChange ->
                            {
                                selectedTrack = null
                                onSelectionModeChange?.invoke(true)
                                selectionChange(selectedTrackUris + track.contentUri)
                            }
                        },
                    )
                }
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        if (onRefresh != null) {
            val pullToRefreshState = rememberPullToRefreshState()
            PullToRefresh(
                isRefreshing = shouldKeepMusicPullRefreshing(scanStatus, pullToRefreshState.refreshState),
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize().padding(start = startPadding, end = endPadding),
                pullToRefreshState = pullToRefreshState,
                contentPadding = listContentPadding,
                topAppBarScrollBehavior = scrollBehavior,
                refreshTexts = listOf(
                    stringResource(R.string.music_pull_to_refresh),
                    stringResource(R.string.music_release_to_refresh),
                    stringResource(R.string.music_refreshing),
                    stringResource(R.string.music_refresh_complete),
                ),
                content = listContent,
            )
        } else {
            listContent()
        }

        if (showAlphabetIndex) {
            AlphabetSideBar(
                contentPadding = LocalPageAlphabetIndexPadding.current,
                sectionIndexMap = listSectionIndexMap,
                itemCount = displayedTracks.size + leadingItemCount,
                scrollStateKey = listState,
                isAtTarget = { targetIndex ->
                    listState.firstVisibleItemIndex == targetIndex &&
                        listState.firstVisibleItemScrollOffset == 0
                },
                scrollToItem = listState::scrollToItem,
                sections = sections,
                onTargetIndexChanged = { targetIndex, restoreLargeTitle ->
                    val topBarState = scrollBehavior.state
                    if (restoreLargeTitle && !scrollBehavior.isPinned) {
                        topBarState.heightOffset = 0f
                        topBarState.contentOffset = 0f
                    } else if (topBarState.heightOffsetLimit != -Float.MAX_VALUE) {
                        topBarState.heightOffset = topBarState.heightOffsetLimit
                        topBarState.contentOffset = topBarState.heightOffsetLimit
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
            )
        }
        if (showLocateAction) {
            MusicLocationAction(
                visible = locateActionVisible,
                currentIndex = currentIndex,
                listState = listState,
                scrollBehavior = scrollBehavior,
                miniPlayerBounds = miniPlayerBounds,
                contentPadding = contentPadding,
                backdrop = locationBackdrop,
                alphabetIndexVisible = showAlphabetIndex,
            )
        }
    }

    TrackActionsOverlay(
        track = selectedTrack,
        onDismiss = { selectedTrack = null },
        onPlayNext = onPlayNext,
        onAppendToQueue = onAppendToQueue,
        onAddToPlaylist = onAddToPlaylist,
        onGoToAlbum = onGoToAlbum,
        artistGroups = artistGroups,
        onGoToArtist = onGoToArtist,
        onExternalEditReturned = onExternalEditReturned,
        showMusicTagEditor = showMusicTagEditor,
        showLyricoEditor = showLyricoEditor,
        showLunaBeatEditor = showLunaBeatEditor,
        showSaltAudioEditor = showSaltAudioEditor,
        showSaltAudioAnalyzer = showSaltAudioAnalyzer,
    )
}

// Only a real pull may open the native indicator; startup scans stay silent.
internal fun shouldKeepMusicPullRefreshing(
    scanStatus: ScanStatus,
    refreshState: RefreshState,
): Boolean = scanStatus is ScanStatus.Scanning && refreshState != RefreshState.Idle

internal fun resolveMusicPlaybackSelection(
    displayedTracks: List<MusicTrack>,
    queueTracks: List<MusicTrack>,
    query: String,
    selectedIndex: Int,
): Pair<List<MusicTrack>, Int>? {
    val selectedTrack = displayedTracks.getOrNull(selectedIndex) ?: return null
    val playbackTracks = if (query.isNotBlank() && queueTracks.isNotEmpty()) {
        queueTracks
    } else {
        displayedTracks
    }
    val playbackIndex = playbackTracks.indexOfFirst { it.id == selectedTrack.id }
    return playbackIndex.takeIf { it >= 0 }?.let { playbackTracks to it }
}

@Composable
private fun EmptyMusicState(
    scanStatus: ScanStatus,
    modifier: Modifier = Modifier,
    query: String = "",
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        when (scanStatus.toMusicLibraryPlaceholder()) {
            MusicLibraryPlaceholder.Loading -> InfiniteProgressIndicator(
                color = MiuixTheme.colorScheme.onSurface,
            )

            MusicLibraryPlaceholder.Error,
            MusicLibraryPlaceholder.Empty,
            -> MusicLibraryEmptyMessage(
                icon = MiuixIcons.Music,
                text = when {
                    query.isNotBlank() -> stringResource(R.string.music_no_search_results)
                    scanStatus is ScanStatus.Error -> stringResource(R.string.music_scan_failed)
                    else -> stringResource(R.string.music_empty_after_scan)
                },
            )
        }
    }
}

@Composable
internal fun MusicLibraryEmptyState(
    scanStatus: ScanStatus,
    query: String,
    emptyMessageRes: Int,
    noSearchResultsRes: Int,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        when (scanStatus.toMusicLibraryPlaceholder()) {
            MusicLibraryPlaceholder.Loading -> InfiniteProgressIndicator(
                color = MiuixTheme.colorScheme.onSurface,
            )

            MusicLibraryPlaceholder.Error,
            MusicLibraryPlaceholder.Empty,
            -> MusicLibraryEmptyMessage(
                icon = icon,
                text = stringResource(
                    if (query.isBlank()) emptyMessageRes else noSearchResultsRes,
                ),
            )
        }
    }
}

@Composable
internal fun MusicLibraryEmptyMessage(
    icon: ImageVector,
    text: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.disabledOnSecondaryVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = text,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

internal enum class MusicLibraryPlaceholder {
    Loading,
    Empty,
    Error,
}

internal fun ScanStatus.toMusicLibraryPlaceholder(): MusicLibraryPlaceholder = when (this) {
    ScanStatus.Idle -> MusicLibraryPlaceholder.Empty
    ScanStatus.Scanning -> MusicLibraryPlaceholder.Loading

    ScanStatus.PermissionRequired -> MusicLibraryPlaceholder.Empty
    is ScanStatus.Success -> MusicLibraryPlaceholder.Empty
    is ScanStatus.Error -> MusicLibraryPlaceholder.Error
}
