package com.melox.player.ui.screen.library

import com.melox.player.ui.component.library.LocalAlphabetIndexBottomPadding
import com.melox.player.ui.component.library.PreserveSortScrollPosition

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
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
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TopAppBarState
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.Job

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
    scrollBehavior: ScrollBehavior,
    indexTopPadding: Dp,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    showIndex: Boolean = true,
    selectionMode: Boolean = false,
    selectedTrackUris: Set<String> = emptySet(),
    onSelectionChange: ((Set<String>) -> Unit)? = null,
    onSelectionModeChange: ((Boolean) -> Unit)? = null,
    showLocateAction: Boolean = false,
    currentTrackContentUri: String? = null,
    miniPlayerBounds: Rect = Rect.Zero,
    playerContentBounds: Rect = Rect.Zero,
) {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var locateJob by remember { mutableStateOf<Job?>(null) }
    val locateActionSize = ButtonDefaults.MinHeight
    val locateActionVisible = showLocateAction && !selectionMode &&
        miniPlayerBounds.width > 0f && playerContentBounds.width > 0f
    val currentIndex = displayedTracks.indexOfFirst { it.contentUri == currentTrackContentUri }
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
        itemCount = displayedTracks.size,
        scrollState = listState,
    ) {
        listState.requestScrollToItem(
            listState.firstVisibleItemIndex,
            listState.firstVisibleItemScrollOffset,
        )
    }
    var selectedTrack by remember { mutableStateOf<MusicTrack?>(null) }
    val listContentPadding = PaddingValues(
        start = contentPadding.calculateStartPadding(layoutDirection),
        top = contentPadding.calculateTopPadding() + 12.dp,
        end = contentPadding.calculateEndPadding(layoutDirection),
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

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            state = listState,
            contentPadding = listContentPadding,
            overscrollEffect = null,
        ) {
            if (displayedTracks.isEmpty()) {
                item(key = "empty_music") {
                    EmptyMusicState(
                        scanStatus = scanStatus,
                        query = query,
                        modifier = Modifier.fillParentMaxSize(),
                    )
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
                            if (selectionMode) {
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
                        selectionMode = selectionMode,
                        selected = track.contentUri in selectedTrackUris,
                        onLongClick = onSelectionChange?.let { selectionChange ->
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

        if (
            showIndex &&
            displayedTracks.isNotEmpty() &&
            (
                sortConfig.field == MusicSortField.TITLE ||
                    sortConfig.field == MusicSortField.ARTIST ||
                    sortConfig.field == MusicSortField.FILE_NAME
            ) &&
            query.isBlank()
        ) {
            AlphabetSideBar(
                sectionIndexMap = sectionIndexMap,
                itemCount = displayedTracks.size,
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
                    .padding(
                        top = indexTopPadding + 4.dp,
                        end = contentPadding.calculateEndPadding(layoutDirection),
                        bottom = LocalAlphabetIndexBottomPadding.current,
                    )
                    .fillMaxHeight()
            )
        }
        if (locateActionVisible) {
            FloatingActionButton(
                onClick = {
                    if (currentIndex in displayedTracks.indices) {
                        locateJob?.cancel()
                        locateJob = scope.launch {
                            val paddingBeforeCollapse = listState.layoutInfo.beforeContentPadding
                            val collapsedPadding = paddingBeforeCollapse +
                                collapseMusicLocationTopBar(scrollBehavior.state).roundToInt()
                            // Programmatic list scrolling does not dispatch nested scroll to the bar.
                            snapshotFlow { listState.layoutInfo.beforeContentPadding }
                                .first { it <= collapsedPadding + 1 }
                            listState.animateScrollToItem(
                                currentIndex,
                                scrollOffset = with(density) { 12.dp.roundToPx() },
                            )
                        }
                    }
                },
                containerColor = CardDefaults.defaultColors().color.copy(alpha = 0.8f),
                shadowElevation = 0.dp,
                minWidth = locateActionSize,
                minHeight = locateActionSize,
                modifier = Modifier
                    .offset {
                        with(density) {
                            musicLocationOffset(
                                miniPlayerBounds,
                                playerContentBounds,
                                locateActionSize.toPx(),
                                12.dp.toPx(),
                                (contentPadding.calculateEndPadding(layoutDirection) +
                                    TopAppBarDefaults.ActionIconPadding).toPx(),
                            )
                        }
                    }
                    .semantics { if (currentIndex < 0) disabled() },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_locate_current_track),
                    contentDescription = stringResource(R.string.locate_current_track),
                    modifier = Modifier.size(24.dp),
                    tint = MiuixTheme.colorScheme.onSurface.copy(
                        alpha = if (currentIndex >= 0) 1f else 0.38f,
                    ),
                )
            }
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
    )
}

internal fun collapseMusicLocationTopBar(state: TopAppBarState): Float {
    if (state.heightOffsetLimit == -Float.MAX_VALUE) return 0f
    val heightChange = state.heightOffsetLimit - state.heightOffset
    state.heightOffset = state.heightOffsetLimit
    state.contentOffset = state.heightOffsetLimit
    return heightChange
}

internal fun musicLocationOffset(
    miniPlayerBounds: Rect,
    playerContentBounds: Rect,
    actionSizePx: Float,
    gapPx: Float,
    trailingPaddingPx: Float,
): IntOffset = IntOffset(
    (playerContentBounds.width - trailingPaddingPx - actionSizePx)
        .roundToInt().coerceAtLeast(0),
    (miniPlayerBounds.top - playerContentBounds.top - gapPx - actionSizePx)
        .roundToInt().coerceAtLeast(0),
)

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
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
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
