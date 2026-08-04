package com.melox.player.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melox.player.R
import com.melox.player.data.library.displayArtistName
import com.melox.player.model.MusicTrack
import com.melox.player.model.ScanStatus
import com.melox.player.ui.component.library.PlaybackArtwork
import com.melox.player.ui.component.library.PlaybackArtworkFrame
import com.melox.player.ui.component.library.extractArtworkColor
import com.melox.player.ui.component.library.loadArtworkBitmap
import com.melox.player.ui.component.library.rememberArtworkBitmap
import com.melox.player.ui.screen.library.MusicLibraryPlaceholder
import com.melox.player.ui.screen.library.toMusicLibraryPlaceholder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlin.random.Random

data class HomeRecommendations(
    val tracks: List<MusicTrack>,
    val requestMore: () -> Unit,
)

@Composable
fun HomeScreen(
    tracks: List<MusicTrack>,
    recommendations: HomeRecommendations?,
    recentlyAddedTrackIds: Set<Long>,
    scanStatus: ScanStatus,
    onTrackClick: (List<MusicTrack>, Int) -> Unit,
    onRecommendationClick: (MusicTrack, List<MusicTrack>) -> Unit,
    onRecommendationPageChanged: (Int) -> Unit,
    scrollBehavior: ScrollBehavior,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val recommendationTracks = recommendations?.tracks.orEmpty()
    val recentTracks = remember(tracks, recentlyAddedTrackIds) {
        buildHomeRecentlyAddedTracks(tracks, recentlyAddedTrackIds)
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 12.dp,
        ),
        overscrollEffect = null,
    ) {
        if (tracks.isEmpty()) {
            item(key = "home_empty") {
                HomeEmptyState(
                    scanStatus = scanStatus,
                    modifier = Modifier.fillParentMaxSize(),
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
                                .height(HomeRecommendationArtworkSize + HomeRecommendationInfoHeight)
                                .padding(bottom = 14.dp),
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
                            onDispose { onRecommendationPageChanged(0) }
                        }
                        HorizontalPager(
                            state = pagerState,
                            pageSize = PageSize.Fixed(HomeRecommendationArtworkSize),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            pageSpacing = 12.dp,
                            beyondViewportPageCount = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(HomeRecommendationArtworkSize + HomeRecommendationInfoHeight)
                                .padding(bottom = 14.dp),
                            key = { page -> "$page-${recommendationTracks[page].id}" },
                        ) { page ->
                            val track = recommendationTracks[page]
                            HomeRecommendationCard(
                                track = track,
                                artworkSize = HomeRecommendationArtworkSize,
                                onClick = {
                                    onRecommendationClick(track, recommendationTracks)
                                },
                            )
                        }
                    }
                }
            }

            if (recentTracks.isNotEmpty()) {
                item(key = "home_recently_added_title") {
                    HomeSectionTitle(R.string.home_recently_added_title)
                }
                items(
                    items = recentTracks.chunked(HomeRecentGridColumns),
                    key = { row -> row.first().id },
                ) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { track ->
                            HomeRecentTrackCard(
                                track = track,
                                onClick = {
                                    val startIndex = tracks.indexOfFirst { allTrack ->
                                        allTrack.id == track.id
                                    }
                                    if (startIndex >= 0) {
                                        onTrackClick(tracks, startIndex)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size < HomeRecentGridColumns) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun rememberHomeRecommendations(
    tracks: List<MusicTrack>,
    active: Boolean,
): HomeRecommendations? {
    val recommendationSeed = rememberSaveable { Random.nextInt() }
    var loadRequested by rememberSaveable { mutableStateOf(false) }
    var selectionComplete by rememberSaveable { mutableStateOf(false) }
    var initialRecommendationExpansionStarted by rememberSaveable { mutableStateOf(false) }
    var requestedRecommendationCount by rememberSaveable {
        mutableIntStateOf(HomePriorityRecommendationCount)
    }
    var selectedTrackIds by rememberSaveable { mutableStateOf(LongArray(0)) }
    val requestMore = remember {
        { requestedRecommendationCount += 1 }
    }
    val context = LocalContext.current.applicationContext
    val artworkTargetSizePx = with(LocalDensity.current) {
        HomeRecommendationArtworkSize.roundToPx()
    }
    val selectedTracks = remember(tracks, selectedTrackIds) {
        val tracksById = tracks.associateBy(MusicTrack::id)
        selectedTrackIds.map { trackId -> tracksById[trackId] }.filterNotNull()
    }

    LaunchedEffect(active) {
        if (active) loadRequested = true
    }
    LaunchedEffect(selectionComplete, initialRecommendationExpansionStarted) {
        if (selectionComplete && !initialRecommendationExpansionStarted) {
            initialRecommendationExpansionStarted = true
            requestedRecommendationCount = HomeInitialRecommendationCount
        }
    }
    LaunchedEffect(
        loadRequested,
        tracks,
        recommendationSeed,
        artworkTargetSizePx,
        requestedRecommendationCount,
    ) {
        if (!loadRequested || tracks.isEmpty()) return@LaunchedEffect
        val selected = withContext(Dispatchers.Default) {
            selectHomeRecommendations(
                tracks = tracks,
                seed = recommendationSeed,
                count = requestedRecommendationCount,
                knownArtworkTrackIds = selectedTrackIds.toSet(),
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
                    targetSizePx = artworkTargetSizePx,
                ) != null
            }
        }
        selectedTrackIds = selected.map(MusicTrack::id).toLongArray()
        selectionComplete = true
    }

    return when {
        tracks.isEmpty() -> HomeRecommendations(emptyList(), requestMore)
        !selectionComplete -> null
        else -> HomeRecommendations(selectedTracks, requestMore)
    }
}

internal fun buildHomeRecentlyAddedTracks(
    tracks: List<MusicTrack>,
    recentlyAddedTrackIds: Set<Long>,
): List<MusicTrack> {
    val newestFirst = tracks.sortedWith(
        compareByDescending<MusicTrack>(MusicTrack::dateModifiedEpochSeconds)
            .thenByDescending(MusicTrack::dateAddedEpochSeconds)
            .thenByDescending(MusicTrack::id),
    )
    val newlyAdded = newestFirst.filter { track -> track.id in recentlyAddedTrackIds }
    if (newlyAdded.isEmpty()) return newestFirst.take(HomeRecentTrackCount)

    return newlyAdded + newestFirst
        .asSequence()
        .filterNot { track -> track.id in recentlyAddedTrackIds }
        .take((HomeRecentTrackCount - newlyAdded.size).coerceAtLeast(0))
        .toList()
}

@Composable
private fun HomeSectionTitle(stringRes: Int) {
    Text(
        text = stringResource(stringRes),
        modifier = Modifier.padding(start = 28.dp, top = 2.dp, end = 16.dp, bottom = 10.dp),
        style = MiuixTheme.textStyles.title4,
    )
}

@Composable
private fun HomeRecentTrackCard(
    track: MusicTrack,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = track.title ?: stringResource(R.string.music_unknown_title)
    val artist = displayArtistName(track.artist) ?: stringResource(R.string.music_unknown_artist)
    Card(
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
                    contentUri = track.contentUri,
                    dateModifiedEpochSeconds = track.dateModifiedEpochSeconds,
                    fileSizeBytes = track.fileSizeBytes,
                    size = maxWidth,
                    cornerRadius = 14.dp,
                )
            }
            Text(
                text = title,
                modifier = Modifier.padding(start = 6.dp, top = 6.dp, end = 6.dp),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artist,
                modifier = Modifier.padding(start = 6.dp, top = 2.dp, end = 6.dp),
                style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
): List<MusicTrack> {
    if (count <= 0 || probeBatchSize <= 0) return emptyList()
    val recommendations = ArrayList<MusicTrack>(count.coerceAtMost(tracks.size))
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
                if (recommendations.size == count) return recommendations
            }
        }
    }
    return recommendations
}

@Composable
private fun HomeRecommendationCard(
    track: MusicTrack,
    artworkSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val artwork = rememberArtworkBitmap(
        contentUri = track.contentUri,
        dateModifiedEpochSeconds = track.dateModifiedEpochSeconds,
        fileSizeBytes = track.fileSizeBytes,
        size = artworkSize,
    )
    val informationColor = remember(artwork) {
        artwork
            ?.extractArtworkColor()
            ?.let { color -> lerp(color, Color.Black, 0.48f) }
            ?: Color.Black
    }
    Card(
        modifier = modifier
            .width(artworkSize)
            .height(artworkSize + HomeRecommendationInfoHeight),
        insideMargin = PaddingValues(0.dp),
        colors = CardDefaults.defaultColors(
            color = informationColor,
            contentColor = Color.White,
        ),
        pressFeedbackType = PressFeedbackType.Sink,
        onClick = onClick,
    ) {
        PlaybackArtworkFrame(
            bitmap = artwork,
            size = artworkSize,
            cornerRadius = 0.dp,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
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
}

@Composable
private fun HomeEmptyState(
    scanStatus: ScanStatus,
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
            -> Text(
                text = if (scanStatus is ScanStatus.Error) {
                    stringResource(R.string.music_scan_failed)
                } else {
                    stringResource(R.string.music_empty_after_scan)
                },
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

private const val HomePriorityRecommendationCount = 2
private const val HomeInitialRecommendationCount = 5
private const val HomePriorityRecommendationProbeBatchSize = 2
private const val HomeRecommendationProbeBatchSize = 8
private const val HomeRecentTrackCount = 20
private val HomeRecommendationArtworkSize = 256.dp
private const val HomeRecentGridColumns = 2
private val HomeRecommendationInfoHeight = 84.dp
