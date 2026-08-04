package com.melox.player.ui.screen.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.melox.player.R
import com.melox.player.data.library.AlbumGroup
import com.melox.player.data.library.AlbumGridStyle
import com.melox.player.data.library.AlbumSortConfig
import com.melox.player.data.library.AlbumSortField
import com.melox.player.ui.component.library.AlphabetSections
import com.melox.player.ui.component.library.AlphabetSideBar
import com.melox.player.ui.component.library.PlaybackArtwork
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun AlbumLibraryScreen(
    displayedAlbums: List<AlbumGroup>,
    sectionIndexMap: Map<String, Int>,
    query: String,
    sortConfig: AlbumSortConfig,
    onAlbumClick: (AlbumGroup) -> Unit,
    scrollBehavior: ScrollBehavior,
    indexTopPadding: Dp,
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    showIndex: Boolean = true,
    indexBottomSpacing: Dp = 12.dp,
) {
    val layoutDirection = LocalLayoutDirection.current
    val supportsIndex = sortConfig.field == AlbumSortField.ALBUM ||
        sortConfig.field == AlbumSortField.ALBUM_ARTIST
    val sections = remember(sortConfig.descending) {
        if (sortConfig.descending) AlphabetSections.asReversed() else AlphabetSections
    }
    val showScrollTop by remember {
        derivedStateOf { scrollBehavior.state.collapsedFraction > 0.01f }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (displayedAlbums.isEmpty()) {
            Text(
                text = stringResource(
                    if (query.isBlank()) R.string.album_empty else R.string.album_no_search_results,
                ),
                modifier = Modifier.align(Alignment.Center),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(sortConfig.gridStyle.columns),
                modifier = Modifier
                    .fillMaxSize()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                state = gridState,
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = contentPadding.calculateTopPadding() + 12.dp,
                    end = 20.dp,
                    bottom = contentPadding.calculateBottomPadding() + 12.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                overscrollEffect = null,
            ) {
                items(
                    items = displayedAlbums,
                    key = AlbumGroup::key,
                ) { album ->
                    AlbumGridItem(
                        album = album,
                        gridStyle = sortConfig.gridStyle,
                        onClick = { onAlbumClick(album) },
                    )
                }
            }
        }

        if (showIndex && displayedAlbums.isNotEmpty() && supportsIndex && query.isBlank()) {
            AlphabetSideBar(
                sectionIndexMap = sectionIndexMap,
                itemCount = displayedAlbums.size,
                scrollStateKey = gridState,
                isAtTarget = { targetIndex ->
                    gridState.firstVisibleItemIndex == targetIndex &&
                        gridState.firstVisibleItemScrollOffset == 0
                },
                scrollToItem = gridState::scrollToItem,
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

@Composable
internal fun AlbumGridItem(
    album: AlbumGroup,
    gridStyle: AlbumGridStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cover = album.coverTrack
    when (gridStyle) {
        AlbumGridStyle.TWO_SMALL -> Card(
            modifier = modifier.fillMaxWidth(),
            cornerRadius = 14.dp,
            insideMargin = PaddingValues(
                start = 6.dp,
                top = 6.dp,
                end = 12.dp,
                bottom = 6.dp,
            ),
            pressFeedbackType = PressFeedbackType.Sink,
            onClick = onClick,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlaybackArtwork(
                    contentUri = cover?.contentUri.orEmpty(),
                    dateModifiedEpochSeconds = cover?.dateModifiedEpochSeconds ?: 0L,
                    fileSizeBytes = cover?.fileSizeBytes ?: 0L,
                    size = 54.dp,
                    cornerRadius = 8.dp,
                )
                AlbumGridLabels(
                    album = album,
                    gridStyle = gridStyle,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        AlbumGridStyle.THREE -> Card(
            modifier = modifier.fillMaxWidth(),
            cornerRadius = 0.dp,
            insideMargin = PaddingValues(0.dp),
            colors = CardDefaults.defaultColors(
                color = Color.Transparent,
                contentColor = MiuixTheme.colorScheme.onSurface,
            ),
            pressFeedbackType = PressFeedbackType.Sink,
            onClick = onClick,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    PlaybackArtwork(
                        contentUri = cover?.contentUri.orEmpty(),
                        dateModifiedEpochSeconds = cover?.dateModifiedEpochSeconds ?: 0L,
                        fileSizeBytes = cover?.fileSizeBytes ?: 0L,
                        size = maxWidth,
                        cornerRadius = 14.dp,
                    )
                }
                AlbumGridLabels(
                    album = album,
                    gridStyle = gridStyle,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun AlbumGridLabels(
    album: AlbumGroup,
    gridStyle: AlbumGridStyle,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = album.name ?: stringResource(R.string.album_unknown),
            modifier = if (gridStyle == AlbumGridStyle.THREE) {
                Modifier.padding(horizontal = 6.dp)
            } else {
                Modifier
            },
            style = MiuixTheme.textStyles.body2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = pluralStringResource(
                R.plurals.album_song_count,
                album.tracks.size,
                album.tracks.size,
            ),
            modifier = if (gridStyle == AlbumGridStyle.THREE) {
                Modifier.padding(start = 6.dp, top = 2.dp)
            } else {
                Modifier.padding(top = 2.dp)
            },
            style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
