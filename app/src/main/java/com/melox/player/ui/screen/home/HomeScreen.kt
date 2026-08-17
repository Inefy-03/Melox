package com.melox.player.ui.screen.home

import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import com.melox.player.ui.component.home.HOME_RECOMMENDATION_ARTWORK_SIZE_PX
import com.melox.player.ui.component.home.rememberHomeRecommendationReflection
import com.melox.player.ui.component.library.PlaybackArtwork
import com.melox.player.ui.component.library.PlaybackArtworkFrame
import com.melox.player.ui.component.library.extractArtworkColor
import com.melox.player.ui.component.library.loadArtworkBitmap
import com.melox.player.ui.component.library.rememberArtworkBitmapPixels
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
import kotlin.math.max
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
    blurEnabled: Boolean,
    onTrackClick: (List<MusicTrack>, Int) -> Unit,
    onRecommendationClick: (MusicTrack, List<MusicTrack>) -> Unit,
    onRecommendationPageChanged: (Int) -> Unit,
    onRecommendationGestureActiveChanged: (Boolean) -> Unit,
    scrollBehavior: ScrollBehavior,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val recommendationTracks = recommendations?.tracks.orEmpty()
    val currentOnRecommendationGestureActiveChanged by rememberUpdatedState(
        onRecommendationGestureActiveChanged,
    )
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
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            pageSpacing = 12.dp,
                            beyondViewportPageCount = 1,
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
                                artworkSize = HomeRecommendationArtworkSize,
                                blurEnabled = blurEnabled,
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
                    targetSizePx = HOME_RECOMMENDATION_ARTWORK_SIZE_PX,
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
    blurEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val artwork = rememberArtworkBitmapPixels(
        contentUri = track.contentUri,
        dateModifiedEpochSeconds = track.dateModifiedEpochSeconds,
        fileSizeBytes = track.fileSizeBytes,
        targetSizePx = HOME_RECOMMENDATION_ARTWORK_SIZE_PX,
    )
    val reflection = rememberHomeRecommendationReflection(
        contentUri = track.contentUri,
        dateModifiedEpochSeconds = track.dateModifiedEpochSeconds,
        fileSizeBytes = track.fileSizeBytes,
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
private val HomeRecommendationArtworkSize = 240.dp
private const val HomeRecentGridColumns = 2
private val HomeRecommendationInfoHeight = 70.dp
private val HomeRecommendationClearArtworkHeight = 240.dp
private val HomeRecommendationMetadataHorizontalPadding = 18.dp
private val HomeRecommendationMetadataBottomPadding = 14.dp
private val HomeRecommendationSectionBottomSpacing = 14.dp
