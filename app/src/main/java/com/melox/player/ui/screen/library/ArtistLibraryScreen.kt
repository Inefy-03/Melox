package com.melox.player.ui.screen.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.melox.player.R
import com.melox.player.data.library.ArtistGroup
import com.melox.player.data.library.ArtistSortConfig
import com.melox.player.data.library.ArtistSortField
import com.melox.player.ui.component.library.AlphabetSections
import com.melox.player.ui.component.library.AlphabetSideBar
import com.melox.player.ui.component.library.ArtistListItem
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun ArtistLibraryScreen(
    displayedArtists: List<ArtistGroup>,
    sectionIndexMap: Map<String, Int>,
    query: String,
    sortConfig: ArtistSortConfig,
    onArtistClick: (ArtistGroup) -> Unit,
    scrollBehavior: ScrollBehavior,
    indexTopPadding: Dp,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    showIndex: Boolean = true,
    indexBottomSpacing: Dp = 12.dp,
) {
    val layoutDirection = LocalLayoutDirection.current
    val sections = remember(sortConfig.descending) {
        if (sortConfig.descending) AlphabetSections.asReversed() else AlphabetSections
    }
    val showScrollTop by remember {
        derivedStateOf { scrollBehavior.state.collapsedFraction > 0.01f }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (displayedArtists.isEmpty()) {
            Text(
                text = stringResource(
                    if (query.isBlank()) R.string.artist_empty else R.string.artist_no_search_results,
                ),
                modifier = Modifier.align(Alignment.Center),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                state = listState,
                contentPadding = PaddingValues(
                    top = contentPadding.calculateTopPadding() + 12.dp,
                    bottom = contentPadding.calculateBottomPadding() + 12.dp,
                ),
                overscrollEffect = null,
            ) {
                items(
                    items = displayedArtists,
                    key = ArtistGroup::key,
                ) { artist ->
                    ArtistListItem(
                        artist = artist,
                        onClick = { onArtistClick(artist) },
                        artworkTextSpacing = 12.dp,
                    )
                }
            }
        }

        if (
            showIndex &&
            displayedArtists.isNotEmpty() &&
            sortConfig.field == ArtistSortField.NAME &&
            query.isBlank()
        ) {
            AlphabetSideBar(
                sectionIndexMap = sectionIndexMap,
                itemCount = displayedArtists.size,
                scrollStateKey = listState,
                isAtTarget = { targetIndex ->
                    listState.firstVisibleItemIndex == targetIndex &&
                        listState.firstVisibleItemScrollOffset == 0
                },
                scrollToItem = listState::scrollToItem,
                sections = sections,
                showScrollTop = showScrollTop,
                onTargetIndexChanged = { _, restoreLargeTitle ->
                    val state = scrollBehavior.state
                    if (restoreLargeTitle) {
                        state.heightOffset = 0f
                        state.contentOffset = 0f
                    } else if (state.heightOffsetLimit != -Float.MAX_VALUE) {
                        state.heightOffset = state.heightOffsetLimit
                        state.contentOffset = state.heightOffsetLimit
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
                    .fillMaxHeight(),
            )
        }
    }
}
