package com.melox.player.ui.screen.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.melox.player.R
import com.melox.player.ui.component.library.responsiveGridColumnCount
import com.melox.player.data.library.AlbumGroup
import com.melox.player.data.library.AlbumGridStyle
import com.melox.player.data.library.ArtistGroup
import com.melox.player.data.library.FolderGroup
import com.melox.player.ui.component.LazyColumn
import com.melox.player.ui.component.PageCard
import com.melox.player.ui.component.library.ArtistArtwork
import com.melox.player.ui.component.overScrollVertical
import com.melox.player.ui.screen.home.HomeEmptyCardContent
import com.melox.player.ui.screen.home.HomeSectionTitle
import com.melox.player.ui.screen.home.HomeSectionSpacing
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.melox.player.ui.screen.home.HomeEmptyActionRow
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
internal fun Os4LibraryOverviewScreen(
    albums: List<AlbumGroup>,
    artists: List<ArtistGroup>,
    folders: List<FolderGroup>,
    gridStyle: AlbumGridStyle,
    listState: LazyListState,
    scrollBehavior: ScrollBehavior,
    onOpenSection: (Int) -> Unit,
    onAlbumClick: (AlbumGroup) -> Unit,
    onArtistClick: (ArtistGroup) -> Unit,
    onFolderClick: (FolderGroup) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val window = LocalWindowInfo.current.containerSize
    val landscape = window.width > window.height
    val direction = LocalLayoutDirection.current
    BoxWithConstraints(modifier.fillMaxSize()) {
        val availableWidth = maxWidth - contentPadding.calculateStartPadding(direction) -
            contentPadding.calculateEndPadding(direction)
        val columns = albumGridColumnCount(
            gridStyle, landscape, false,
            maxWidth - contentPadding.calculateStartPadding(direction) - contentPadding.calculateEndPadding(direction),
            os4 = true, horizontalPadding = 32.dp,
        )
        val albumRows = if (gridStyle == AlbumGridStyle.TWO_SMALL) 3 else 2
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = contentPadding.calculateStartPadding(direction),
                end = contentPadding.calculateEndPadding(direction),
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 12.dp,
            ),
            modifier = Modifier.fillMaxSize().scrollEndHaptic().overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            overscrollEffect = null,
        ) {
            item(key = "albums_title") {
                HomeSectionTitle(R.string.navigation_albums, onClick = { onOpenSection(0) }, endPadding = 28.dp)
            }
            if (albums.isEmpty()) {
                item(key = "albums_empty") { LibraryPreviewEmptyCard(0) }
            } else {
                items(albums.take(columns * albumRows).chunked(columns), key = { it.first().key }) { row ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = LibraryPreviewBottomPadding), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { album ->
                            AlbumGridItem(album, gridStyle, { onAlbumClick(album) }, Modifier.weight(1f))
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            item(key = "artists_title") {
                HomeSectionTitle(R.string.navigation_artists, onClick = { onOpenSection(1) }, topPadding = HomeSectionSpacing - LibraryPreviewBottomPadding, endPadding = 28.dp)
            }
            item(key = "artists") {
                if (artists.isEmpty()) LibraryPreviewEmptyCard(1) else LibraryPreviewCards(artists.size, landscape, availableWidth) { index ->
                    val artist = artists[index]
                    LibraryPreviewRow(
                        artist.name ?: stringResource(R.string.artist_unknown), artist.tracks.size,
                        { onArtistClick(artist) },
                    ) {
                        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                            ArtistArtwork(artist.coverTrack, Modifier.requiredSize(28.dp), size = 28.dp)
                        }
                    }
                }
            }
            item(key = "folders_title") {
                HomeSectionTitle(R.string.navigation_folders, onClick = { onOpenSection(2) }, topPadding = HomeSectionSpacing - LibraryPreviewBottomPadding, endPadding = 28.dp)
            }
            item(key = "folders") {
                if (folders.isEmpty()) LibraryPreviewEmptyCard(2) else LibraryPreviewCards(folders.size, landscape, availableWidth) { index ->
                    val folder = folders[index]
                    LibraryPreviewRow(
                        folder.name ?: stringResource(R.string.folder_unknown), folder.tracks.size,
                        { onFolderClick(folder) },
                    ) {
                        Image(painterResource(R.drawable.ic_file), null, Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryPreviewEmptyCard(section: Int, modifier: Modifier = Modifier) {
    val title = listOf(R.string.album_empty, R.string.artist_empty, R.string.folder_empty)[section]
    val icon = listOf(MiuixIcons.Album, MiuixIcons.Contacts, MiuixIcons.Folder)[section]
    PageCard(modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = LibraryPreviewBottomPadding), cornerRadius = 20.dp) {
        HomeEmptyCardContent(icon, stringResource(title), stringResource(R.string.library_preview_empty_description))
        HomeEmptyActionRow(
            stringResource(R.string.playlist_create_title), null,
            Modifier.graphicsLayer { alpha = 0f }.clearAndSetSemantics { },
        )
    }
}

@Composable
private fun LibraryPreviewCards(
    count: Int,
    landscape: Boolean,
    availableWidth: Dp,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit,
) {
    val groups = libraryPreviewColumns(count, landscape, availableWidth)
    val columns = libraryPreviewColumnCount(landscape, availableWidth)
    val contentWidth = (availableWidth - 32.dp).coerceAtLeast(0.dp)
    val rowWidth = if (landscape) contentWidth.coerceAtLeast(492.dp) else contentWidth
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = LibraryPreviewBottomPadding)
            .horizontalScroll(rememberScrollState()).width(rowWidth),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        groups.forEach { indices ->
            PageCard(Modifier.weight(1f), insideMargin = PaddingValues(0.dp)) {
                indices.forEach { content(it) }
            }
        }
        repeat(columns - groups.size) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun LibraryPreviewRow(
    title: String,
    songCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artwork: @Composable () -> Unit,
) {
    val direction = LocalLayoutDirection.current
    BasicComponent(
        modifier = modifier,
        startAction = artwork,
        endActions = {
            Row(Modifier.padding(end = 8.dp).align(Alignment.CenterVertically).weight(1f, fill = false)) {
                Text(songCount.toString(), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            Image(
                imageVector = MiuixIcons.Basic.ArrowRight,
                contentDescription = null,
                modifier = Modifier.size(10.dp, 16.dp).graphicsLayer {
                    scaleX = if (direction == LayoutDirection.Rtl) -1f else 1f
                }.align(Alignment.CenterVertically),
                colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurfaceVariantActions),
            )
        },
        onClick = onClick,
    ) {
        Text(title, style = MiuixTheme.textStyles.body1, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

internal fun libraryPreviewColumnCount(landscape: Boolean, availableWidth: Dp): Int =
    if (landscape) responsiveGridColumnCount(
        availableWidth = availableWidth,
        horizontalPadding = 32.dp,
        minimumCellWidth = 240.dp,
        minimumColumns = 2,
        maximumColumns = 4,
    ) else 1

internal fun libraryPreviewColumns(
    count: Int,
    landscape: Boolean,
    availableWidth: Dp,
): List<List<Int>> {
    val columns = libraryPreviewColumnCount(landscape, availableWidth)
    val rows = if (landscape) 5 else 4
    return (0 until minOf(count, rows * columns)).groupBy { it % columns }.values.toList()
}

private val LibraryPreviewBottomPadding = 12.dp
