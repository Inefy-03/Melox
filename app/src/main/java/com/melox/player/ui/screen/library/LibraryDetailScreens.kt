package com.melox.player.ui.screen.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melox.player.R
import com.melox.player.data.library.AlbumGroup
import com.melox.player.data.library.AlbumDiscSection
import com.melox.player.data.library.AlbumGridStyle
import com.melox.player.data.library.ArtistGroup
import com.melox.player.data.library.buildAlbumDiscSections
import com.melox.player.data.library.buildAlbumGroups
import com.melox.player.data.library.displayArtistName
import com.melox.player.model.MusicTrack
import com.melox.player.ui.component.BlurredBar
import com.melox.player.ui.component.LocalCustomPageBackground
import com.melox.player.ui.component.LocalTopBarBlurSettings
import com.melox.player.ui.component.topBarContainerColor
import com.melox.player.ui.component.tabSelectedContainerColor
import com.melox.player.ui.component.miuixBarColor
import com.melox.player.ui.component.rememberBlurBackdrop
import com.melox.player.ui.component.library.ArtistArtwork
import com.melox.player.ui.component.library.ArtistListItem
import com.melox.player.ui.component.library.MusicTrackDescriptionMode
import com.melox.player.ui.component.library.MusicTrackRow
import com.melox.player.ui.component.library.PlaybackArtwork
import com.melox.player.ui.component.library.SelectionActionsAnimatedContent
import com.melox.player.ui.component.library.SelectionNavigationIconAnimatedContent
import com.melox.player.ui.component.library.TrackActionsOverlay
import com.melox.player.ui.component.library.TrackSelectionActions
import com.melox.player.ui.component.library.formatDuration
import com.melox.player.ui.component.library.participatingArtistGroups
import com.melox.player.ui.component.library.selectedItemsInDisplayedOrder
import com.melox.player.ui.component.library.toggleAllTrackSelection
import com.melox.player.ui.component.library.toggleTrackSelection
import kotlinx.coroutines.launch
import com.melox.player.ui.component.PageCard as Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import com.melox.player.ui.component.PageScaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.TabRowDefaults
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
    bottomContentPadding: Dp,
    onBack: () -> Unit,
    selectionExitRequest: Int,
    onSelectionModeChange: (Boolean) -> Unit,
    onTrackClick: (List<MusicTrack>, Int) -> Unit,
    onPlayNext: (MusicTrack) -> Unit,
    onAppendToQueue: (MusicTrack) -> Unit,
    onAddToPlaylist: (MusicTrack) -> Unit,
    onAddAllToPlaylist: (List<MusicTrack>, () -> Unit) -> Unit,
    onGoToAlbum: (MusicTrack) -> Unit,
    onGoToArtist: (ArtistGroup) -> Unit,
    onExternalEditReturned: (Long) -> Unit,
    showMusicTagEditor: Boolean,
    showLyricoEditor: Boolean,
    showLunaBeatEditor: Boolean,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val layoutDirection = LocalLayoutDirection.current
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val hasWallpaper = LocalCustomPageBackground.current != null
    val tabRowBackgroundColor = topBarContainerColor(hasWallpaper, backdrop != null, MiuixTheme.colorScheme.surface)
    val tabSelectedColor = tabSelectedContainerColor(
        hasWallpaper = hasWallpaper,
        progressiveBlurActive = backdrop != null && LocalTopBarBlurSettings.current.progressiveEnabled,
        fallbackColor = MiuixTheme.colorScheme.surfaceContainer,
    )
    val trackSections = remember(album.tracks) { buildAlbumDiscSections(album.tracks) }
    val orderedTracks = remember(trackSections) { trackSections.flatMap(AlbumDiscSection::tracks) }
    val orderedTrackIndices = remember(orderedTracks) {
        orderedTracks.mapIndexed { index, track -> track.id to index }.toMap()
    }
    val participatingArtists = remember(album.tracks, artistGroups) {
        participatingArtistGroups(album.tracks, artistGroups)
    }
    var selectedTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var selectedTrackUris by remember(album.key) {
        mutableStateOf<Set<String>>(emptySet())
    }
    var selectionMode by remember(album.key) { mutableStateOf(false) }
    val displayedKeys = orderedTracks.map(MusicTrack::contentUri)
    BackHandler(enabled = selectionMode) {
        selectedTrackUris = emptySet()
        selectionMode = false
    }
    LaunchedEffect(selectionMode) {
        onSelectionModeChange(selectionMode)
    }
    LaunchedEffect(selectionExitRequest) {
        if (selectionMode) {
            selectedTrackUris = emptySet()
            selectionMode = false
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        if (selectionMode) {
            selectedTrackUris = emptySet()
            selectionMode = false
        }
    }
    PageScaffold(
            topBar = {
            BlurredBar(
                backdrop = backdrop,
                blurEnabled = backdrop != null,
                scrollBehavior = scrollBehavior,
            ) {
                SmallTopAppBar(
                    title = if (selectionMode) {
                        if (selectedTrackUris.isEmpty()) {
                            stringResource(R.string.selection_choose_songs)
                        } else {
                            stringResource(
                                R.string.selection_selected_count,
                                selectedTrackUris.size,
                            )
                        }
                    } else {
                        ""
                    },
                    color = backdrop.miuixBarColor(),
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        SelectionNavigationIconAnimatedContent(
                            selectionMode = selectionMode,
                            onCloseSelection = {
                                selectedTrackUris = emptySet()
                                selectionMode = false
                            },
                            defaultNavigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        imageVector = MiuixIcons.Back,
                                        contentDescription = stringResource(R.string.back),
                                    )
                                }
                            },
                        )
                    },
                    actions = {
                        SelectionActionsAnimatedContent(
                            selectionMode = selectionMode,
                            selectionActions = {
                                TrackSelectionActions(
                                    allSelected = displayedKeys.isNotEmpty() &&
                                        selectedTrackUris.containsAll(displayedKeys),
                                    actionEnabled = selectedTrackUris.isNotEmpty(),
                                    onToggleAll = {
                                        selectedTrackUris = toggleAllTrackSelection(
                                            selectedTrackUris,
                                            displayedKeys,
                                        )
                                    },
                                    onAction = {
                                        val tracks = selectedItemsInDisplayedOrder(
                                            orderedTracks,
                                            selectedTrackUris,
                                            MusicTrack::contentUri,
                                        )
                                        if (tracks.isNotEmpty()) {
                                            onAddAllToPlaylist(tracks) {
                                                selectedTrackUris = emptySet()
                                                selectionMode = false
                                            }
                                        }
                                    },
                                )
                            },
                            defaultActions = {},
                        )
                    },
                    bottomContent = {
                        Column {
                            AlbumDetailHeader(album)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                                insideMargin = PaddingValues(0.dp),
                                colors = CardDefaults.defaultColors(
                                    color = tabRowBackgroundColor,
                                    contentColor = MiuixTheme.colorScheme.onSurface,
                                ),
                            ) {
                                TabRowWithContour(
                                    tabs = listOf(
                                        stringResource(R.string.artist_songs),
                                        stringResource(R.string.participating_artists),
                                    ),
                                    selectedTabIndex = pagerState.currentPage,
                                    onTabSelected = { page ->
                                        scope.launch { pagerState.animateScrollToPage(page) }
                                    },
                                    colors = TabRowDefaults.tabRowColors(
                                        backgroundColor = tabRowBackgroundColor,
                                        selectedBackgroundColor = tabSelectedColor,
                                    ),
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
                modifier = Modifier
                    .fillMaxSize(),
                userScrollEnabled = true,
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
                            start = padding.calculateStartPadding(layoutDirection),
                            top = padding.calculateTopPadding() + 4.dp,
                            end = padding.calculateEndPadding(layoutDirection),
                            bottom = maxOf(
                                padding.calculateBottomPadding(),
                                bottomContentPadding,
                            ) + 16.dp,
                        ),
                        overscrollEffect = null,
                    ) {
                        trackSections.forEach { section ->
                            section.discNumber?.let { discNumber ->
                                item(key = "disc:$discNumber") {
                                    AlbumDiscSectionHeader(section)
                                }
                            }
                            items(
                                items = section.tracks,
                                key = MusicTrack::id,
                            ) { track ->
                                MusicTrackRow(
                                    track = track,
                                    isCurrent = track.id == currentTrackId,
                                    onClick = {
                                        if (selectionMode) {
                                            selectedTrackUris = toggleTrackSelection(
                                                selectedTrackUris,
                                                track.contentUri,
                                            )
                                        } else {
                                            onTrackClick(
                                                orderedTracks,
                                                orderedTrackIndices.getValue(track.id),
                                            )
                                        }
                                    },
                                    onMoreClick = { selectedTrack = track },
                                    selectionMode = selectionMode,
                                    selected = track.contentUri in selectedTrackUris,
                                    onLongClick = {
                                        selectedTrack = null
                                        selectionMode = true
                                        selectedTrackUris += track.contentUri
                                    },
                                    descriptionMode = MusicTrackDescriptionMode.Artist,
                                    artworkOverlayText = track.trackNumber
                                        ?.takeIf { it > 0 }
                                        ?.toString()
                                        ?: stringResource(R.string.music_track_number_missing),
                                )
                            }
                        }
                    }
                } else if (participatingArtists.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.artist_empty),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .scrollEndHaptic()
                            .overScrollVertical()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = PaddingValues(
                            start = padding.calculateStartPadding(layoutDirection),
                            top = padding.calculateTopPadding(),
                            end = padding.calculateEndPadding(layoutDirection),
                            bottom = maxOf(
                                padding.calculateBottomPadding(),
                                bottomContentPadding,
                            ) + 16.dp,
                        ),
                        overscrollEffect = null,
                    ) {
                        items(
                            items = participatingArtists,
                            key = ArtistGroup::key,
                        ) { artist ->
                            ArtistListItem(
                                artist = artist,
                                onClick = { onGoToArtist(artist) },
                                artworkTextSpacing = 12.dp,
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
            onAddToPlaylist = onAddToPlaylist,
            onGoToAlbum = onGoToAlbum,
            artistGroups = artistGroups,
            onGoToArtist = onGoToArtist,
            onExternalEditReturned = onExternalEditReturned,
            showMusicTagEditor = showMusicTagEditor,
            showLyricoEditor = showLyricoEditor,
            showLunaBeatEditor = showLunaBeatEditor,
        )
    }
}

@Composable
private fun AlbumDiscSectionHeader(section: AlbumDiscSection) {
    val discTitle = stringResource(
        R.string.album_disc_title,
        requireNotNull(section.discNumber),
    )
    val discDetails = stringResource(
        R.string.album_disc_details,
        section.tracks.size,
        formatDuration(section.totalDurationMs),
    )
    val titleColor = MiuixTheme.colorScheme.onSurface
    val detailsColor = MiuixTheme.colorScheme.onSurfaceVariantSummary
    Text(
        modifier = Modifier.padding(
            start = 28.dp,
            top = 20.dp,
            end = 28.dp,
            bottom = 8.dp,
        ),
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = titleColor)) { append(discTitle) }
            append(' ')
            withStyle(
                SpanStyle(
                    color = detailsColor,
                    fontSize = MiuixTheme.textStyles.footnote1.fontSize,
                ),
            ) { append(discDetails) }
        },
        style = MiuixTheme.textStyles.subtitle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun ArtistDetailScreen(
    artist: ArtistGroup,
    artistGroups: List<ArtistGroup>,
    currentTrackId: Long?,
    bottomContentPadding: Dp,
    albumGridStyle: AlbumGridStyle,
    onBack: () -> Unit,
    selectionExitRequest: Int,
    onSelectionModeChange: (Boolean) -> Unit,
    onAlbumClick: (AlbumGroup) -> Unit,
    onTrackClick: (List<MusicTrack>, Int) -> Unit,
    onPlayNext: (MusicTrack) -> Unit,
    onAppendToQueue: (MusicTrack) -> Unit,
    onAddToPlaylist: (MusicTrack) -> Unit,
    onAddAllToPlaylist: (List<MusicTrack>, () -> Unit) -> Unit,
    onGoToAlbum: (MusicTrack) -> Unit,
    onGoToArtist: (ArtistGroup) -> Unit,
    onExternalEditReturned: (Long) -> Unit,
    showMusicTagEditor: Boolean,
    showLyricoEditor: Boolean,
    showLunaBeatEditor: Boolean,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val layoutDirection = LocalLayoutDirection.current
    val albums = remember(artist.tracks) { buildAlbumGroups(artist.tracks) }
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val hasWallpaper = LocalCustomPageBackground.current != null
    val tabRowBackgroundColor = topBarContainerColor(hasWallpaper, backdrop != null, MiuixTheme.colorScheme.surface)
    val tabSelectedColor = tabSelectedContainerColor(
        hasWallpaper = hasWallpaper,
        progressiveBlurActive = backdrop != null && LocalTopBarBlurSettings.current.progressiveEnabled,
        fallbackColor = MiuixTheme.colorScheme.surfaceContainer,
    )
    var selectedTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var selectedTrackUris by remember(artist.key) {
        mutableStateOf<Set<String>>(emptySet())
    }
    var selectionMode by remember(artist.key) { mutableStateOf(false) }
    val displayedKeys = artist.tracks.map(MusicTrack::contentUri)
    BackHandler(enabled = selectionMode) {
        selectedTrackUris = emptySet()
        selectionMode = false
    }
    LaunchedEffect(selectionMode) {
        onSelectionModeChange(selectionMode)
    }
    LaunchedEffect(selectionExitRequest) {
        if (selectionMode) {
            selectedTrackUris = emptySet()
            selectionMode = false
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        if (selectionMode) {
            selectedTrackUris = emptySet()
            selectionMode = false
        }
    }
    PageScaffold(
            topBar = {
            BlurredBar(
                backdrop = backdrop,
                blurEnabled = backdrop != null,
                scrollBehavior = scrollBehavior,
            ) {
                SmallTopAppBar(
                    title = if (selectionMode) {
                        if (selectedTrackUris.isEmpty()) {
                            stringResource(R.string.selection_choose_songs)
                        } else {
                            stringResource(
                                R.string.selection_selected_count,
                                selectedTrackUris.size,
                            )
                        }
                    } else {
                        ""
                    },
                    color = backdrop.miuixBarColor(),
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        SelectionNavigationIconAnimatedContent(
                            selectionMode = selectionMode,
                            onCloseSelection = {
                                selectedTrackUris = emptySet()
                                selectionMode = false
                            },
                            defaultNavigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        imageVector = MiuixIcons.Back,
                                        contentDescription = stringResource(R.string.back),
                                    )
                                }
                            },
                        )
                    },
                    actions = {
                        SelectionActionsAnimatedContent(
                            selectionMode = selectionMode,
                            selectionActions = {
                                TrackSelectionActions(
                                    allSelected = displayedKeys.isNotEmpty() &&
                                        selectedTrackUris.containsAll(displayedKeys),
                                    actionEnabled = selectedTrackUris.isNotEmpty(),
                                    onToggleAll = {
                                        selectedTrackUris = toggleAllTrackSelection(
                                            selectedTrackUris,
                                            displayedKeys,
                                        )
                                    },
                                    onAction = {
                                        val tracks = selectedItemsInDisplayedOrder(
                                            artist.tracks,
                                            selectedTrackUris,
                                            MusicTrack::contentUri,
                                        )
                                        if (tracks.isNotEmpty()) {
                                            onAddAllToPlaylist(tracks) {
                                                selectedTrackUris = emptySet()
                                                selectionMode = false
                                            }
                                        }
                                    },
                                )
                            },
                            defaultActions = {},
                        )
                    },
                    bottomContent = {
                        Column {
                            ArtistDetailHeader(artist)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                                insideMargin = PaddingValues(0.dp),
                                colors = CardDefaults.defaultColors(
                                    color = tabRowBackgroundColor,
                                    contentColor = MiuixTheme.colorScheme.onSurface,
                                ),
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
                                    colors = TabRowDefaults.tabRowColors(
                                        backgroundColor = tabRowBackgroundColor,
                                        selectedBackgroundColor = tabSelectedColor,
                                    ),
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
                userScrollEnabled = true,
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
                            start = padding.calculateStartPadding(layoutDirection),
                            top = padding.calculateTopPadding(),
                            end = padding.calculateEndPadding(layoutDirection),
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
                                onClick = {
                                    if (selectionMode) {
                                        selectedTrackUris = toggleTrackSelection(
                                            selectedTrackUris,
                                            track.contentUri,
                                        )
                                    } else {
                                        onTrackClick(artist.tracks, index)
                                    }
                                },
                                onMoreClick = { selectedTrack = track },
                                selectionMode = selectionMode,
                                selected = track.contentUri in selectedTrackUris,
                                onLongClick = {
                                    selectedTrack = null
                                    selectionMode = true
                                    selectedTrackUris += track.contentUri
                                },
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
                            start = padding.calculateStartPadding(layoutDirection) + 16.dp,
                            top = padding.calculateTopPadding() + 8.dp,
                            end = padding.calculateEndPadding(layoutDirection) + 16.dp,
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
            onAddToPlaylist = onAddToPlaylist,
            onGoToAlbum = onGoToAlbum,
            artistGroups = artistGroups,
            onGoToArtist = onGoToArtist,
            onExternalEditReturned = onExternalEditReturned,
            showMusicTagEditor = showMusicTagEditor,
            showLyricoEditor = showLyricoEditor,
            showLunaBeatEditor = showLunaBeatEditor,
        )
    }
}

@Composable
private fun AlbumDetailHeader(album: AlbumGroup) {
    val cover = album.coverTrack
    val albumArtist = displayArtistName(album.albumArtist)
        ?: stringResource(R.string.album_artist_unknown)
    val totalDurationMs = album.tracks.sumOf { it.durationMs.coerceAtLeast(0L) }
    Box(modifier = Modifier.fillMaxWidth()) {
        val coverSize = albumDetailHeaderCoverSize()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaybackArtwork(
                contentUri = cover?.contentUri.orEmpty(),
                dateModifiedEpochSeconds = cover?.dateModifiedEpochSeconds ?: 0L,
                fileSizeBytes = cover?.fileSizeBytes ?: 0L,
                size = coverSize,
                cornerRadius = 14.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = album.name ?: stringResource(R.string.album_unknown),
                    style = MiuixTheme.textStyles.title3,
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
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.album_song_count,
                            album.tracks.size,
                            album.tracks.size,
                        ),
                        style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                    album.year?.takeIf { it > 0 }?.let { year ->
                        Text(
                            text = stringResource(R.string.album_detail_year, year),
                            style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                }
                Text(
                    text = formatDuration(totalDurationMs),
                    style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
                    color = MiuixTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

internal fun albumDetailHeaderCoverSize(): Dp = 96.dp

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
            size = 72.dp,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = artist.name ?: stringResource(R.string.artist_unknown),
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.artist_song_count,
                    artist.tracks.size,
                    artist.tracks.size,
                ),
                style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.artist_album_count,
                    artist.albumCount,
                    artist.albumCount,
                ),
                style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
                color = MiuixTheme.colorScheme.onSurface,
            )
        }
    }
}
