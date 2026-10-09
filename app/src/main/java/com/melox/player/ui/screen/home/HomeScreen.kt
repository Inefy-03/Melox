package com.melox.player.ui.screen.home

import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melox.player.R
import com.melox.player.model.LocalPlaylist
import com.melox.player.model.MusicTrack
import com.melox.player.model.ScanStatus
import com.melox.player.ui.component.home.HOME_RECOMMENDATION_ARTWORK_SIZE_PX
import com.melox.player.ui.component.home.rememberHomeRecommendationReflection
import com.melox.player.ui.component.library.PlaybackArtwork
import com.melox.player.ui.component.library.PlaybackArtworkFrame
import com.melox.player.ui.component.library.extractArtworkColor
import com.melox.player.ui.component.library.loadArtworkBitmap
import com.melox.player.ui.component.library.rememberArtworkBitmapPixels
import com.melox.player.ui.component.library.rememberPlaceholderArtworkBitmap
import com.melox.player.ui.component.library.currentPlaceholderArtworkResId
import com.melox.player.ui.component.library.responsiveGridColumnCount
import com.melox.player.ui.component.playlist.PlaylistGridItem
import com.melox.player.ui.screen.library.MusicLibraryPlaceholder
import com.melox.player.ui.screen.library.toMusicLibraryPlaceholder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import com.melox.player.ui.component.PageCard
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Create
import top.yukonga.miuix.kmp.icon.extended.RecordingTape
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlin.math.ceil
import kotlin.math.max
import kotlin.random.Random

data class HomeRecommendations(
    val tracks: List<MusicTrack>,
    val requestMore: () -> Unit,
    val artworkTrackIds: Set<Long>,
)

internal data class HomeRecommendationSelection(
    val tracks: List<MusicTrack>,
    val artworkTrackIds: Set<Long>,
)

@Composable
fun HomeScreen(
    tracks: List<MusicTrack>,
    recommendations: HomeRecommendations?,
    playlists: List<LocalPlaylist>,
    playlistsLoaded: Boolean,
    scanStatus: ScanStatus,
    blurEnabled: Boolean,
    onOpenPlaylists: () -> Unit,
    onOpenScanSettings: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onPlaylistClick: (LocalPlaylist) -> Unit,
    onRecommendationClick: (MusicTrack, List<MusicTrack>) -> Unit,
    onRecommendationPageChanged: (Int) -> Unit,
    onRecommendationGestureActiveChanged: (Boolean) -> Unit,
    scrollBehavior: ScrollBehavior,
    listState: LazyListState,
    landscape: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val layoutDirection = LocalLayoutDirection.current
    val recommendationTracks = recommendations?.tracks.orEmpty()
    val currentOnRecommendationGestureActiveChanged by rememberUpdatedState(
        onRecommendationGestureActiveChanged,
    )
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val playlistGridColumns = homePlaylistGridColumnCount(
            landscape = landscape,
            availableWidth = maxWidth,
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = contentPadding.calculateStartPadding(layoutDirection),
                top = contentPadding.calculateTopPadding() + 12.dp,
                end = contentPadding.calculateEndPadding(layoutDirection),
                bottom = contentPadding.calculateBottomPadding() + 12.dp,
            ),
            overscrollEffect = null,
        ) {
        if (tracks.isEmpty()) {
            item(key = "home_recommendation_title") {
                HomeSectionTitle(R.string.home_recommendation_title)
            }
            item(key = "home_recommendation_empty") {
                HomeEmptyRecommendationState(
                    scanStatus = scanStatus,
                    hasScannedMusic = false,
                    onOpenScanSettings = onOpenScanSettings,
                )
            }
        } else {
            when {
                recommendations == null -> {
                    item(key = "home_recommendation_title") {
                        HomeSectionTitle(R.string.home_recommendation_title)
                    }
                    item(key = "home_recommendation_loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = HomeRecommendationSectionBottomSpacing)
                                .height(HomeRecommendationArtworkSize + HomeRecommendationInfoHeight),
                            contentAlignment = Alignment.Center,
                        ) {
                            InfiniteProgressIndicator(
                                color = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                recommendationTracks.isNotEmpty() -> {
                    item(key = "home_recommendation_title") {
                        HomeSectionTitle(R.string.home_recommendation_title)
                    }
                    item(key = "home_recommendations") {
                        val pagerState = rememberPagerState(
                            pageCount = { recommendationTracks.size },
                        )
                        var previousPage by remember(pagerState) {
                            mutableIntStateOf(pagerState.currentPage)
                        }
                        LaunchedEffect(pagerState.currentPage) {
                            onRecommendationPageChanged(pagerState.currentPage)
                            if (pagerState.currentPage > previousPage) {
                                recommendations.requestMore()
                            }
                            previousPage = pagerState.currentPage
                        }
                        DisposableEffect(pagerState) {
                            onDispose {
                                onRecommendationPageChanged(0)
                                currentOnRecommendationGestureActiveChanged(false)
                            }
                        }
                        HorizontalPager(
                            state = pagerState,
                            pageSize = PageSize.Fixed(HomeRecommendationArtworkSize),
                            contentPadding = PaddingValues(
                                horizontal = HomeRecommendationHorizontalContentPadding,
                            ),
                            pageSpacing = HomeRecommendationPageSpacing,
                            beyondViewportPageCount = HomeRecommendationPrefetchCount,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = HomeRecommendationSectionBottomSpacing)
                                .height(HomeRecommendationArtworkSize + HomeRecommendationInfoHeight)
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(
                                            requireUnconsumed = false,
                                            pass = PointerEventPass.Initial,
                                        )
                                        currentOnRecommendationGestureActiveChanged(true)
                                        try {
                                            while (true) {
                                                val event = awaitPointerEvent(
                                                    pass = PointerEventPass.Initial,
                                                )
                                                val change = event.changes.firstOrNull {
                                                    it.id == down.id
                                                } ?: break
                                                if (!change.pressed) break
                                            }
                                        } finally {
                                            currentOnRecommendationGestureActiveChanged(false)
                                        }
                                    }
                                },
                            overscrollEffect = null,
                            key = { page -> "$page-${recommendationTracks[page].id}" },
                        ) { page ->
                            val track = recommendationTracks[page]
                            HomeRecommendationCard(
                                track = track,
                                hasArtwork = track.id in recommendations.artworkTrackIds,
                                artworkSize = HomeRecommendationArtworkSize,
                                blurEnabled = blurEnabled,
                                onClick = {
                                    onRecommendationClick(track, recommendationTracks)
                                },
                            )
                        }
                    }
                }

                else -> {
                    item(key = "home_recommendation_title") {
                        HomeSectionTitle(R.string.home_recommendation_title)
                    }
                    item(key = "home_recommendation_empty") {
                        HomeEmptyRecommendationState(
                            scanStatus = scanStatus,
                            hasScannedMusic = true,
                            onOpenScanSettings = onOpenScanSettings,
                        )
                    }
                }
            }

        }

        item(key = "home_playlists_title") {
            HomeSectionTitle(
                stringRes = R.string.home_playlists_title,
                onClick = onOpenPlaylists,
                topPadding = 8.dp,
                endPadding = 28.dp,
            )
        }
        when {
            !playlistsLoaded -> item(key = "home_playlists_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    InfiniteProgressIndicator(color = MiuixTheme.colorScheme.onSurface)
                }
            }

            playlists.isEmpty() -> item(key = "home_playlists_empty") {
                HomeEmptyPlaylistCard(onCreatePlaylist = onCreatePlaylist)
            }

            else -> {
                items(
                    items = playlists.take(playlistGridColumns * 3).chunked(playlistGridColumns),
                    key = { row -> row.first().id },
                ) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { playlist ->
                            PlaylistGridItem(
                                playlist = playlist,
                                onClick = { onPlaylistClick(playlist) },
                                modifier = Modifier.weight(1f),
                                showEmptyArtworkIcon = false,
                            )
                        }
                        repeat(playlistGridColumns - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun HomeEmptyPlaylistCard(
    onCreatePlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PageCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        cornerRadius = 20.dp,
    ) {
        HomeEmptyCardContent(
            imageVector = MiuixIcons.RecordingTape,
            title = stringResource(R.string.home_playlist_empty_title),
            description = stringResource(R.string.home_playlist_empty_description),
        )
        HomeEmptyActionRow(
            label = stringResource(R.string.playlist_create_title),
            onClick = onCreatePlaylist,
        )
    }
}

@Composable
private fun HomeEmptyRecommendationState(
    scanStatus: ScanStatus,
    hasScannedMusic: Boolean,
    onOpenScanSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (scanStatus.toMusicLibraryPlaceholder()) {
        MusicLibraryPlaceholder.Loading -> Box(
            modifier = modifier
                .fillMaxWidth()
                .height(HomeEmptyCardContentHeight),
            contentAlignment = Alignment.Center,
        ) {
            InfiniteProgressIndicator(color = MiuixTheme.colorScheme.onSurface)
        }

        MusicLibraryPlaceholder.Error,
        MusicLibraryPlaceholder.Empty,
        -> PageCard(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = HomeRecommendationSectionBottomSpacing,
                ),
            cornerRadius = 20.dp,
        ) {
            HomeEmptyCardContent(
                imageVector = MiuixIcons.Create,
                title = if (scanStatus is ScanStatus.Error) {
                    stringResource(R.string.music_scan_failed)
                } else if (hasScannedMusic) {
                    stringResource(R.string.home_recommendation_unavailable_title)
                } else {
                    stringResource(R.string.home_recommendation_empty_title)
                },
                description = stringResource(
                    if (hasScannedMusic) R.string.home_recommendation_unavailable_description
                    else R.string.home_recommendation_empty_description,
                ),
            )
            HomeEmptyActionRow(
                label = stringResource(R.string.scan_music_page_title),
                onClick = onOpenScanSettings,
            )
        }
    }
}

@Composable
private fun HomeEmptyActionRow(
    label: String,
    onClick: () -> Unit,
) {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
    BasicComponent(
        insideMargin = PaddingValues(horizontal = 20.dp),
        onClick = onClick,
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.button,
            color = MiuixTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun HomeEmptyCardContent(
    imageVector: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(HomeEmptyCardContentHeight)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.weight(2f))
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
        )
        Spacer(modifier = Modifier.weight(1f))
        Column {
            Text(
                text = title,
                style = MiuixTheme.textStyles.title4,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                modifier = Modifier.padding(top = 4.dp),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        Spacer(modifier = Modifier.weight(4f))
    }
}

internal fun homePlaylistGridColumnCount(
    landscape: Boolean,
    availableWidth: Dp,
): Int = if (landscape) {
    responsiveGridColumnCount(
        availableWidth = availableWidth,
        horizontalPadding = HomePlaylistGridHorizontalPadding,
        minimumCellWidth = HomePlaylistGridMinimumCellWidth,
        minimumColumns = HomePlaylistPortraitColumnCount,
        maximumColumns = HomePlaylistLandscapeMaximumColumnCount,
    )
} else {
    HomePlaylistPortraitColumnCount
}

internal fun homeInitialRecommendationCount(viewportWidth: Dp): Int {
    val pageViewportWidth = (viewportWidth - HomeRecommendationHorizontalContentPadding * 2)
        .coerceAtLeast(0.dp)
    val pageExtent = HomeRecommendationArtworkSize + HomeRecommendationPageSpacing
    return ceil(
        ((pageViewportWidth + HomeRecommendationPageSpacing) / pageExtent).toDouble(),
    ).toInt()
        .coerceAtLeast(HomePriorityRecommendationCount)
        .plus(HomeRecommendationPrefetchCount)
}

@Composable
internal fun rememberHomeRecommendations(
    tracks: List<MusicTrack>,
    active: Boolean,
    initialRecommendationCount: Int,
): HomeRecommendations? {
    val recommendationSeed = rememberSaveable { Random.nextInt() }
    var loadRequested by rememberSaveable { mutableStateOf(false) }
    var selectionComplete by rememberSaveable { mutableStateOf(false) }
    var initialRecommendationExpansionStarted by rememberSaveable { mutableStateOf(false) }
    var requestedRecommendationCount by rememberSaveable {
        mutableIntStateOf(HomePriorityRecommendationCount)
    }
    var selectedTrackIds by rememberSaveable { mutableStateOf(LongArray(0)) }
    var artworkTrackIds by rememberSaveable { mutableStateOf(LongArray(0)) }
    val requestMore = remember {
        { requestedRecommendationCount += 1 }
    }
    val context = LocalContext.current.applicationContext
    val selectedTracks = remember(tracks, selectedTrackIds) {
        val tracksById = tracks.associateBy(MusicTrack::id)
        selectedTrackIds.map { trackId -> tracksById[trackId] }.filterNotNull()
    }

    LaunchedEffect(active) {
        if (active) loadRequested = true
    }
    LaunchedEffect(selectionComplete, initialRecommendationExpansionStarted, initialRecommendationCount) {
        if (selectionComplete) {
            if (!initialRecommendationExpansionStarted) {
                initialRecommendationExpansionStarted = true
            }
            requestedRecommendationCount = maxOf(
                requestedRecommendationCount,
                initialRecommendationCount,
            )
        }
    }
    LaunchedEffect(
        loadRequested,
        tracks,
        recommendationSeed,
        requestedRecommendationCount,
    ) {
        if (!loadRequested || tracks.isEmpty()) return@LaunchedEffect
        val selected = withContext(Dispatchers.Default) {
            selectHomeRecommendations(
                tracks = tracks,
                seed = recommendationSeed,
                count = requestedRecommendationCount,
                knownArtworkTrackIds = artworkTrackIds.toSet(),
                probeBatchSize = if (!initialRecommendationExpansionStarted) {
                    HomePriorityRecommendationProbeBatchSize
                } else {
                    HomeRecommendationProbeBatchSize
                },
            ) { track ->
                loadArtworkBitmap(
                    context = context,
                    contentUri = track.contentUri,
                    dateModifiedEpochSeconds = track.dateModifiedEpochSeconds,
                    fileSizeBytes = track.fileSizeBytes,
                    targetSizePx = HOME_RECOMMENDATION_ARTWORK_SIZE_PX,
                ) != null
            }
        }
        selectedTrackIds = selected.tracks.map(MusicTrack::id).toLongArray()
        artworkTrackIds = selected.artworkTrackIds.toLongArray()
        selectionComplete = true
    }

    return when {
        tracks.isEmpty() -> HomeRecommendations(emptyList(), requestMore, emptySet())
        !selectionComplete -> null
        else -> HomeRecommendations(selectedTracks, requestMore, artworkTrackIds.toSet())
    }
}

@Composable
private fun HomeSectionTitle(
    stringRes: Int,
    onClick: (() -> Unit)? = null,
    topPadding: Dp = 2.dp,
    endPadding: Dp = 16.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(start = 28.dp, top = topPadding, end = endPadding, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(stringRes),
            modifier = Modifier.weight(1f),
            style = MiuixTheme.textStyles.title4,
            fontWeight = FontWeight.Medium,
        )
        if (onClick != null) {
            Icon(
                imageVector = MiuixIcons.Demibold.ChevronForward,
                contentDescription = stringResource(stringRes),
                modifier = Modifier.size(16.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        }
    }
}

internal suspend fun selectHomeRecommendations(
    tracks: List<MusicTrack>,
    seed: Int,
    count: Int,
    knownArtworkTrackIds: Set<Long> = emptySet(),
    probeBatchSize: Int = HomeRecommendationProbeBatchSize,
    hasArtwork: suspend (MusicTrack) -> Boolean,
): HomeRecommendationSelection {
    if (count <= 0 || probeBatchSize <= 0) {
        return HomeRecommendationSelection(emptyList(), emptySet())
    }
    val recommendations = ArrayList<MusicTrack>(count.coerceAtMost(tracks.size))
    val withoutArtwork = ArrayList<MusicTrack>()
    val artworkTrackIds = HashSet<Long>()
    val candidates = tracks.shuffled(Random(seed))
    for (batchStart in candidates.indices step probeBatchSize) {
        val batch = candidates.subList(
            batchStart,
            minOf(batchStart + probeBatchSize, candidates.size),
        )
        val artworkMatches = coroutineScope {
            batch.map { track ->
                async { track.id in knownArtworkTrackIds || hasArtwork(track) }
            }.awaitAll()
        }
        batch.forEachIndexed { index, track ->
            if (artworkMatches[index]) {
                recommendations += track
                artworkTrackIds += track.id
                if (recommendations.size == count) {
                    return HomeRecommendationSelection(recommendations, artworkTrackIds)
                }
            } else {
                if (withoutArtwork.size < count) withoutArtwork += track
            }
        }
    }
    recommendations += withoutArtwork.take(count - recommendations.size)
    return HomeRecommendationSelection(recommendations, artworkTrackIds)
}

@Composable
private fun HomeRecommendationCard(
    track: MusicTrack,
    hasArtwork: Boolean,
    artworkSize: Dp,
    blurEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val artwork = if (hasArtwork) rememberArtworkBitmapPixels(
        contentUri = track.contentUri,
        dateModifiedEpochSeconds = track.dateModifiedEpochSeconds,
        fileSizeBytes = track.fileSizeBytes,
        targetSizePx = HOME_RECOMMENDATION_ARTWORK_SIZE_PX,
    ) else rememberPlaceholderArtworkBitmap(artworkSize)
    val placeholderArtworkResId = if (hasArtwork) null else currentPlaceholderArtworkResId()
    val reflection = rememberHomeRecommendationReflection(
        contentUri = placeholderArtworkResId?.let { "placeholder-artwork:$it" } ?: track.contentUri,
        dateModifiedEpochSeconds = if (hasArtwork) track.dateModifiedEpochSeconds else 0L,
        fileSizeBytes = if (hasArtwork) track.fileSizeBytes else 0L,
        sourceBitmap = artwork,
        enabled = blurEnabled,
    )
    val informationColor = remember(artwork) {
        artwork
            ?.extractArtworkColor()
            ?.let { color -> lerp(color, Color.Black, 0.48f) }
            ?: Color.Black
    }
    val artworkAvailable = artwork != null
    val reflectionAvailable = reflection != null
    if (
        !homeRecommendationCardReady(
            blurEnabled = blurEnabled,
            artworkAvailable = artworkAvailable,
            reflectionAvailable = reflectionAvailable,
        )
    ) {
        Box(
            modifier = modifier
                .width(artworkSize)
                .height(artworkSize + HomeRecommendationInfoHeight),
        )
        return
    }
    Card(
        modifier = modifier
            .width(artworkSize)
            .height(artworkSize + HomeRecommendationInfoHeight),
        cornerRadius = HomeRecommendationCardCornerRadius,
        insideMargin = PaddingValues(0.dp),
        colors = CardDefaults.defaultColors(
            color = informationColor,
            contentColor = Color.White,
        ),
        pressFeedbackType = PressFeedbackType.Sink,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
        ) {
            if (
                homeRecommendationUsesReflection(
                    blurEnabled = blurEnabled,
                    artworkAvailable = artworkAvailable,
                    reflectionAvailable = reflectionAvailable,
                )
            ) {
                HomeRecommendationReflectionArtwork(
                    clearArtwork = checkNotNull(artwork),
                    reflectedArtwork = checkNotNull(reflection),
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                PlaybackArtworkFrame(
                    bitmap = artwork,
                    size = artworkSize,
                    cornerRadius = 0.dp,
                    modifier = Modifier.align(Alignment.TopStart),
                )
            }
            HomeRecommendationMetadata(
                track = track,
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
    }
}

@Composable
private fun HomeRecommendationMetadata(
    track: MusicTrack,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = HomeRecommendationMetadataHorizontalPadding,
                end = HomeRecommendationMetadataHorizontalPadding,
                bottom = HomeRecommendationMetadataBottomPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = track.title ?: stringResource(R.string.music_unknown_title),
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = track.artist ?: stringResource(R.string.music_unknown_artist),
            style = MiuixTheme.textStyles.footnote1,
            color = Color.White.copy(alpha = 0.68f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HomeRecommendationReflectionArtwork(
    clearArtwork: Bitmap,
    reflectedArtwork: Bitmap,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val clearArtworkHeight = HomeRecommendationClearArtworkHeight.toPx()
        val reflectionTop = clearArtworkHeight / 2f
        drawIntoCanvas { composeCanvas ->
            val nativeCanvas = composeCanvas.nativeCanvas
            val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val clearPlacement = homeRecommendationCropPlacement(
                sourceWidth = clearArtwork.width,
                sourceHeight = clearArtwork.height,
                destinationWidth = size.width,
                destinationHeight = clearArtworkHeight,
            )
            nativeCanvas.drawBitmap(
                clearArtwork,
                null,
                clearPlacement.toRectF(),
                bitmapPaint,
            )

            val layerSave = nativeCanvas.saveLayer(
                0f,
                reflectionTop,
                size.width,
                size.height,
                Paint(Paint.ANTI_ALIAS_FLAG),
            )
            val transformSave = nativeCanvas.save()
            nativeCanvas.scale(
                1f,
                -1f,
                size.width / 2f,
                homeRecommendationReflectionPivotY(
                    cardHeight = size.height,
                    clearArtworkHeight = clearArtworkHeight,
                ),
            )
            val reflectionPlacement = homeRecommendationCropPlacement(
                sourceWidth = reflectedArtwork.width,
                sourceHeight = reflectedArtwork.height,
                destinationWidth = size.width,
                destinationHeight = size.height - reflectionTop,
                verticalOffset = reflectionTop / 2f,
            )
            nativeCanvas.drawBitmap(
                reflectedArtwork,
                null,
                reflectionPlacement.toRectF(),
                bitmapPaint,
            )
            nativeCanvas.restoreToCount(transformSave)

            val gradientMaskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f,
                    clearArtworkHeight * 2f / 3f,
                    0f,
                    clearArtworkHeight,
                    HomeRecommendationReflectionGradientColors,
                    null,
                    Shader.TileMode.CLAMP,
                )
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            }
            nativeCanvas.drawRect(0f, 0f, size.width, size.height, gradientMaskPaint)
            nativeCanvas.restoreToCount(layerSave)
        }
    }
}

internal data class HomeRecommendationCropPlacement(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
) {
    fun toRectF(): RectF = RectF(
        left.toInt().toFloat(),
        top.toInt().toFloat(),
        (left + width).toInt().toFloat(),
        (top + height).toInt().toFloat(),
    )
}

internal fun homeRecommendationCropPlacement(
    sourceWidth: Int,
    sourceHeight: Int,
    destinationWidth: Float,
    destinationHeight: Float,
    verticalOffset: Float = 0f,
): HomeRecommendationCropPlacement {
    val scale = max(
        destinationWidth / sourceWidth.coerceAtLeast(1),
        destinationHeight / sourceHeight.coerceAtLeast(1),
    )
    val scaledWidth = sourceWidth * scale
    val scaledHeight = sourceHeight * scale
    return HomeRecommendationCropPlacement(
        left = (destinationWidth - scaledWidth) / 2f,
        top = (destinationHeight - scaledHeight) / 2f + verticalOffset,
        width = scaledWidth,
        height = scaledHeight,
    )
}

internal fun homeRecommendationReflectionPivotY(
    cardHeight: Float,
    clearArtworkHeight: Float,
): Float {
    val reflectionTop = clearArtworkHeight / 2f
    return (cardHeight - reflectionTop) / 2f + reflectionTop
}

internal fun homeRecommendationUsesReflection(
    blurEnabled: Boolean,
    artworkAvailable: Boolean,
    reflectionAvailable: Boolean,
): Boolean = blurEnabled && artworkAvailable && reflectionAvailable

internal fun homeRecommendationCardReady(
    blurEnabled: Boolean,
    artworkAvailable: Boolean,
    reflectionAvailable: Boolean,
): Boolean = artworkAvailable && (!blurEnabled || reflectionAvailable)

private val HomeRecommendationReflectionGradientColors = intArrayOf(
    0x00000000,
    0x4D000000,
    0xCC000000.toInt(),
    0xFF000000.toInt(),
)

private const val HomePriorityRecommendationCount = 2
private const val HomePriorityRecommendationProbeBatchSize = 2
private const val HomeRecommendationProbeBatchSize = 8
private const val HomeRecommendationPrefetchCount = 2
private val HomeRecommendationArtworkSize = 220.dp
private val HomeRecommendationCardCornerRadius = 20.dp
private val HomeRecommendationPageSpacing = 12.dp
private val HomeRecommendationHorizontalContentPadding = 16.dp
private const val HomePlaylistPortraitColumnCount = 2
private const val HomePlaylistLandscapeMaximumColumnCount = 6
private val HomePlaylistGridHorizontalPadding = 32.dp
private val HomePlaylistGridMinimumCellWidth = 140.dp
private val HomeRecommendationInfoHeight = 70.dp
private val HomeRecommendationClearArtworkHeight = 220.dp
private val HomeRecommendationMetadataHorizontalPadding = 18.dp
private val HomeRecommendationMetadataBottomPadding = 14.dp
private val HomeRecommendationSectionBottomSpacing = 14.dp
private val HomeEmptyCardContentHeight = 160.dp
