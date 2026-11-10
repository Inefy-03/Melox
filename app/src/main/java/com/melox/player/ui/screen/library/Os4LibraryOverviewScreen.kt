package com.melox.player.ui.screen.library

import androidx.compose.foundation.Image
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.melox.player.R
import com.melox.player.ui.LocalFloatingNavigationRailInset
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.melox.player.ui.screen.home.HomeEmptyActionRow
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
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
    val sidebarVisible = LocalFloatingNavigationRailInset.current > 0.dp
    val direction = LocalLayoutDirection.current
    BoxWithConstraints(modifier.fillMaxSize()) {
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
                if (artists.isEmpty()) LibraryPreviewEmptyCard(1) else LibraryPreviewCards(artists.size, landscape, sidebarVisible) { index ->
                    val artist = artists[index]
                    LibraryPreviewRow(
                        artist.name ?: stringResource(R.string.artist_unknown), artist.tracks.size,
                        { onArtistClick(artist) },
                    ) { ArtistArtwork(artist.coverTrack, size = 24.dp) }
                }
            }
            item(key = "folders_title") {
                HomeSectionTitle(R.string.navigation_folders, onClick = { onOpenSection(2) }, topPadding = HomeSectionSpacing - LibraryPreviewBottomPadding, endPadding = 28.dp)
            }
            item(key = "folders") {
                if (folders.isEmpty()) LibraryPreviewEmptyCard(2) else LibraryPreviewCards(folders.size, landscape, sidebarVisible) { index ->
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
    sidebarVisible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit,
) {
    val groups = libraryPreviewColumns(count, landscape, sidebarVisible)
    val columns = libraryPreviewColumnCount(landscape, sidebarVisible)
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = LibraryPreviewBottomPadding), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
    BasicComponent(
        modifier = modifier,
        startAction = artwork,
        endActions = {
            Text(songCount.toString(), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            Spacer(Modifier.width(8.dp))
            Icon(MiuixIcons.Basic.ArrowRight, null, Modifier.size(width = 10.dp, height = 16.dp))
        },
        onClick = onClick,
    ) {
        Text(title, style = MiuixTheme.textStyles.body1, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

internal fun libraryPreviewColumnCount(landscape: Boolean, sidebarVisible: Boolean): Int = when {
    !landscape -> 1
    sidebarVisible -> 3
    else -> 4
}

internal fun libraryPreviewColumns(
    count: Int,
    landscape: Boolean,
    sidebarVisible: Boolean = false,
): List<List<Int>> {
    val columns = libraryPreviewColumnCount(landscape, sidebarVisible)
    val rows = if (landscape) 5 else 4
    return (0 until minOf(count, rows * columns)).groupBy { it % columns }.values.toList()
}

private val LibraryPreviewBottomPadding = 12.dp
