package com.melox.player.ui.screen.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melox.player.R
import com.melox.player.data.library.AlbumGroup
import com.melox.player.data.library.AlbumGridStyle
import com.melox.player.data.library.ArtistGroup
import com.melox.player.data.library.buildAlbumGroups
import com.melox.player.data.library.displayArtistName
import com.melox.player.model.MusicTrack
import com.melox.player.ui.component.MiuixBlurredBar
import com.melox.player.ui.component.miuixBarColor
import com.melox.player.ui.component.rememberMiuixBlurBackdrop
import com.melox.player.ui.component.library.ArtistArtwork
import com.melox.player.ui.component.library.MusicTrackDescriptionMode
import com.melox.player.ui.component.library.MusicTrackRow
import com.melox.player.ui.component.library.PlaybackArtwork
import com.melox.player.ui.component.library.TrackActionsOverlay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun AlbumDetailScreen(
    album: AlbumGroup,
    artistGroups: List<ArtistGroup>,
    currentTrackId: Long?,
    blurEnabled: Boolean,
    bottomContentPadding: Dp,
    onBack: () -> Unit,
    onTrackClick: (List<MusicTrack>, Int) -> Unit,
    onPlayNext: (MusicTrack) -> Unit,
    onAppendToQueue: (MusicTrack) -> Unit,
    onGoToAlbum: (MusicTrack) -> Unit,
    onGoToArtist: (ArtistGroup) -> Unit,
    onExternalEditReturned: (Long) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    var selectedTrack by remember { mutableStateOf<MusicTrack?>(null) }
    Scaffold(
        topBar = {
            MiuixBlurredBar(
                backdrop = backdrop,
                modifier = Modifier.background(backdrop.miuixBarColor()),
            ) {
                SmallTopAppBar(
                    title = "",
                    color = Color.Transparent,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                    bottomContent = {
                        AlbumDetailHeader(album)
                    },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 4.dp,
                    bottom = maxOf(
                        padding.calculateBottomPadding(),
                        bottomContentPadding,
                    ) + 16.dp,
                ),
                overscrollEffect = null,
            ) {
                itemsIndexed(
                    items = album.tracks,
                    key = { _, track -> track.id },
                ) { index, track ->
                    MusicTrackRow(
                        track = track,
                        isCurrent = track.id == currentTrackId,
                        onClick = { onTrackClick(album.tracks, index) },
                        onMoreClick = { selectedTrack = track },
                        descriptionMode = MusicTrackDescriptionMode.Artist,
                    )
                }
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
}

@Composable
fun ArtistDetailScreen(
    artist: ArtistGroup,
    artistGroups: List<ArtistGroup>,
    currentTrackId: Long?,
    blurEnabled: Boolean,
    bottomContentPadding: Dp,
    albumGridStyle: AlbumGridStyle,
    onBack: () -> Unit,
    onAlbumClick: (AlbumGroup) -> Unit,
    onTrackClick: (List<MusicTrack>, Int) -> Unit,
    onPlayNext: (MusicTrack) -> Unit,
    onAppendToQueue: (MusicTrack) -> Unit,
    onGoToAlbum: (MusicTrack) -> Unit,
    onGoToArtist: (ArtistGroup) -> Unit,
    onExternalEditReturned: (Long) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    val albums = remember(artist.tracks) { buildAlbumGroups(artist.tracks) }
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    var selectedTrack by remember { mutableStateOf<MusicTrack?>(null) }
    Scaffold(
        topBar = {
            MiuixBlurredBar(
                backdrop = backdrop,
                modifier = Modifier.background(backdrop.miuixBarColor()),
            ) {
                SmallTopAppBar(
                    title = "",
                    color = Color.Transparent,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                    bottomContent = {
                        Column {
                            ArtistDetailHeader(artist)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                insideMargin = PaddingValues(0.dp),
                            ) {
                                TabRowWithContour(
                                    tabs = listOf(
                                        stringResource(R.string.artist_songs),
                                        stringResource(R.string.artist_albums),
                                    ),
                                    selectedTabIndex = pagerState.currentPage,
                                    onTabSelected = { page ->
                                        scope.launch { pagerState.animateScrollToPage(page) }
                                    },
                                )
                            }
                        }
                    },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false,
                beyondViewportPageCount = 1,
                key = { it },
            ) { page ->
                if (page == 0) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .scrollEndHaptic()
                            .overScrollVertical()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = PaddingValues(
                            top = padding.calculateTopPadding(),
                            bottom = maxOf(
                                padding.calculateBottomPadding(),
                                bottomContentPadding,
                            ) + 16.dp,
                        ),
                        overscrollEffect = null,
                    ) {
                        itemsIndexed(
                            items = artist.tracks,
                            key = { _, track -> track.id },
                        ) { index, track ->
                            MusicTrackRow(
                                track = track,
                                isCurrent = track.id == currentTrackId,
                                onClick = { onTrackClick(artist.tracks, index) },
                                onMoreClick = { selectedTrack = track },
                                descriptionMode = MusicTrackDescriptionMode.Album,
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(albumGridStyle.columns),
                        modifier = Modifier
                            .fillMaxSize()
                            .scrollEndHaptic()
                            .overScrollVertical()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = padding.calculateTopPadding() + 8.dp,
                            end = 16.dp,
                            bottom = maxOf(
                                padding.calculateBottomPadding(),
                                bottomContentPadding,
                            ) + 16.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        overscrollEffect = null,
                    ) {
                        items(albums, key = AlbumGroup::key) { album ->
                            AlbumGridItem(
                                album = album,
                                gridStyle = albumGridStyle,
                                onClick = { onAlbumClick(album) },
                            )
                        }
                    }
                }
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
}

@Composable
private fun AlbumDetailHeader(album: AlbumGroup) {
    val cover = album.coverTrack
    val albumArtist = displayArtistName(album.albumArtist)
        ?: stringResource(R.string.album_artist_unknown)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaybackArtwork(
            contentUri = cover?.contentUri.orEmpty(),
            dateModifiedEpochSeconds = cover?.dateModifiedEpochSeconds ?: 0L,
            fileSizeBytes = cover?.fileSizeBytes ?: 0L,
            size = 112.dp,
            cornerRadius = 10.dp,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = album.name ?: stringResource(R.string.album_unknown),
                style = MiuixTheme.textStyles.title2,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = albumArtist,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            album.year?.takeIf { it > 0 }?.let { year ->
                Text(
                    text = stringResource(R.string.album_detail_year, year),
                    style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
                    color = MiuixTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = pluralStringResource(
                    R.plurals.album_song_count,
                    album.tracks.size,
                    album.tracks.size,
                ),
                style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
                color = MiuixTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ArtistDetailHeader(artist: ArtistGroup) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtistArtwork(
            track = artist.coverTrack,
            size = 112.dp,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = artist.name ?: stringResource(R.string.artist_unknown),
                style = MiuixTheme.textStyles.title2,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.artist_counts,
                    pluralStringResource(
                        R.plurals.artist_album_count,
                        artist.albumCount,
                        artist.albumCount,
                    ),
                    pluralStringResource(
                        R.plurals.artist_song_count,
                        artist.tracks.size,
                        artist.tracks.size,
                    ),
                ),
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}
