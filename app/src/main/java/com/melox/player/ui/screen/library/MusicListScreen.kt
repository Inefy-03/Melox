package com.melox.player.ui.screen.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
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
    onGoToAlbum: (MusicTrack) -> Unit,
    artistGroups: List<ArtistGroup>,
    onGoToArtist: (ArtistGroup) -> Unit,
    onExternalEditReturned: (Long) -> Unit,
    scrollBehavior: ScrollBehavior,
    indexTopPadding: Dp,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    showIndex: Boolean = true,
    indexBottomSpacing: Dp = 12.dp,
) {
    val layoutDirection = LocalLayoutDirection.current
    var selectedTrack by remember { mutableStateOf<MusicTrack?>(null) }
    val listContentPadding = PaddingValues(
        start = contentPadding.calculateStartPadding(layoutDirection),
        top = contentPadding.calculateTopPadding() + 12.dp,
        end = contentPadding.calculateEndPadding(layoutDirection),
        bottom = contentPadding.calculateBottomPadding(),
    )
    val showScrollTop by remember {
        derivedStateOf { scrollBehavior.state.collapsedFraction > 0.01f }
    }
    val sections = remember(sortConfig.descending) {
        if (sortConfig.descending) AlphabetSections.asReversed() else AlphabetSections
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
                            resolveMusicPlaybackSelection(
                                displayedTracks = displayedTracks,
                                queueTracks = queueTracks,
                                query = query,
                                selectedIndex = index,
                            )?.let { (playbackTracks, playbackIndex) ->
                                onTrackClick(playbackTracks, playbackIndex)
                            }
                        },
                        onMoreClick = { selectedTrack = track },
                    )
                }
            }
        }

        if (
            showIndex &&
            displayedTracks.isNotEmpty() &&
            (
                sortConfig.field == MusicSortField.TITLE ||
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
                showScrollTop = showScrollTop,
                onTargetIndexChanged = { targetIndex, restoreLargeTitle ->
                    val topBarState = scrollBehavior.state
                    if (restoreLargeTitle) {
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
                        bottom = contentPadding.calculateBottomPadding() +
                            indexBottomSpacing,
                    )
                    .fillMaxHeight()
            )
        }
    }

    TrackActionsOverlay(
        track = selectedTrack,
        onDismiss = { selectedTrack = null },
        onPlayNext = onPlayNext,
        onAppendToQueue = onAppendToQueue,
        onGoToAlbum = onGoToAlbum,
        artistGroups = artistGroups,
        onGoToArtist = onGoToArtist,
        onExternalEditReturned = onExternalEditReturned,
    )
}

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
            -> Text(
                text = when {
                    query.isNotBlank() -> stringResource(R.string.music_no_search_results)
                    scanStatus is ScanStatus.Error -> stringResource(R.string.music_scan_failed)
                    else -> stringResource(R.string.music_empty_after_scan)
                },
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
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
