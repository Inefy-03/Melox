// SPDX-License-Identifier: Apache-2.0
// Rendering and placement behavior adapted from accompanist-lyrics-ui.
package com.melox.player.ui.screen.playback

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melox.player.model.LyricLine
import com.melox.player.model.LyricTransition
import com.melox.player.model.LyricsRenderItem
import com.melox.player.model.LyricsDocument
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val LYRIC_EDGE_FADE_DP = 100f
internal const val LYRIC_INACTIVE_TEXT_ALPHA = 0.4f
internal const val LYRICS_MANUAL_FOLLOW_RESUME_DELAY_MS = 5_000L
private const val LYRIC_FOCUS_ALPHA_ANIMATION_DURATION_MS = 180
internal const val LYRIC_PRIMARY_FONT_SIZE_SP = 24f
internal const val LYRIC_PRIMARY_LINE_HEIGHT_SP = 28f
internal const val LYRIC_TRANSLATION_FONT_SIZE_SP = 16f
internal const val LYRIC_TRANSLATION_LINE_HEIGHT_SP = 22f

internal fun lyricEdgeFadeHeights(showBottomFade: Boolean): Pair<Float, Float> =
    LYRIC_EDGE_FADE_DP to if (showBottomFade) LYRIC_EDGE_FADE_DP else 0f

internal fun lyricCenterScrollDelta(
    itemOffset: Int,
    itemSize: Int,
    viewportStartOffset: Int,
    viewportEndOffset: Int,
    centerOffsetPx: Float = 0f,
): Float = itemOffset + itemSize / 2f -
    ((viewportStartOffset + viewportEndOffset) / 2f + centerOffsetPx)

internal fun lyricTargetScrollOffset(
    itemSize: Int,
    viewportStartOffset: Int,
    viewportEndOffset: Int,
    centerOffsetPx: Float = 0f,
): Int = -(
    (viewportStartOffset + viewportEndOffset) / 2f +
        centerOffsetPx -
        itemSize / 2f
    ).roundToInt()

internal fun lyricBlurRadiusTarget(
    lyricBlurEnabled: Boolean,
    distanceFromFocus: Int,
    isUserBrowsingLyrics: Boolean,
): Float = if (
    lyricBlurEnabled && distanceFromFocus > 0 && !isUserBrowsingLyrics
) {
    distanceFromFocus * 3f
} else {
    0f
}

internal fun shouldAcceptPublishedLyricPosition(
    publishedPositionMs: Long,
    seekPositionMs: Long,
    positionAtSeekRequestMs: Long,
): Boolean = abs(publishedPositionMs - seekPositionMs) <=
    abs(publishedPositionMs - positionAtSeekRequestMs)

internal fun lyricSeekPositionIsApplied(
    currentPositionMs: Long,
    seekPositionMs: Long,
): Boolean = abs(currentPositionMs - seekPositionMs) <= 250L

internal fun lyricClockStartPosition(
    currentSmoothPositionMs: Long,
    publishedPositionMs: Long,
    seekPositionMs: Long,
    seekRequestKey: Int,
    seekRequestChanged: Boolean,
    isPlaying: Boolean,
): Long = when {
    seekRequestChanged && seekRequestKey > 0 -> seekPositionMs.coerceAtLeast(0L)
    seekRequestChanged -> publishedPositionMs.coerceAtLeast(0L)
    !isPlaying -> currentSmoothPositionMs.coerceAtLeast(0L)
    else -> publishedPositionMs.coerceAtLeast(0L)
}

internal fun lyricTransitionVerticalPaddingDp(afterLineIndex: Int): Float =
    if (afterLineIndex >= 0) 10f else 5f

internal fun lyricSeekUsesAnimatedCentering(
    hasPositionedInitialFocus: Boolean,
    centerOffsetUnchanged: Boolean,
    isPreviewing: Boolean = false,
): Boolean = hasPositionedInitialFocus && centerOffsetUnchanged && !isPreviewing

internal fun lyricDisplayedPositionMs(
    previewPositionMs: Long?,
    seekRequestPending: Boolean,
    seekPositionMs: Long,
    smoothPositionMs: Long,
): Long = previewPositionMs?.coerceAtLeast(0L) ?: if (seekRequestPending) {
    seekPositionMs.coerceAtLeast(0L)
} else {
    smoothPositionMs.coerceAtLeast(0L)
}

internal fun lyricScrollIsManual(
    listIsScrolling: Boolean,
    scrollInCode: Boolean,
): Boolean = listIsScrolling && !scrollInCode

internal fun lyricVerticalDragExceedsTouchSlop(
    horizontalDeltaPx: Float,
    verticalDeltaPx: Float,
    touchSlopPx: Float,
): Boolean = abs(verticalDeltaPx) > touchSlopPx &&
    abs(verticalDeltaPx) > abs(horizontalDeltaPx)

internal fun lyricProgrammaticTranslationStart(
    currentTranslationY: Float,
    measuredScrollDelta: Float?,
    targetRenderIndex: Int,
    previousRenderIndex: Int,
    offscreenTravelPx: Float,
): Float = currentTranslationY + when {
    measuredScrollDelta != null -> measuredScrollDelta
    targetRenderIndex > previousRenderIndex -> offscreenTravelPx
    targetRenderIndex < previousRenderIndex -> -offscreenTravelPx
    else -> 0f
}

internal data class LyricsRenderIndexMap(
    val lineRenderIndices: IntArray,
    val transitionRenderIndices: IntArray,
)

internal fun buildLyricsRenderIndexMap(
    renderItems: List<LyricsRenderItem>,
    lineCount: Int,
    transitionCount: Int,
): LyricsRenderIndexMap {
    val lineRenderIndices = IntArray(lineCount) { -1 }
    val transitionRenderIndices = IntArray(transitionCount) { -1 }
    renderItems.forEachIndexed { renderIndex, item ->
        when (item) {
            is LyricsRenderItem.Line -> {
                if (item.lineIndex in lineRenderIndices.indices) {
                    lineRenderIndices[item.lineIndex] = renderIndex
                }
            }

            is LyricsRenderItem.Transition -> {
                if (item.transitionIndex in transitionRenderIndices.indices) {
                    transitionRenderIndices[item.transitionIndex] = renderIndex
                }
            }
        }
    }
    return LyricsRenderIndexMap(
        lineRenderIndices = lineRenderIndices,
        transitionRenderIndices = transitionRenderIndices,
    )
}

internal fun lyricOffscreenTranslationDistance(
    viewportStartOffset: Int,
    viewportEndOffset: Int,
    itemSize: Int,
): Float =
    (viewportEndOffset - viewportStartOffset).coerceAtLeast(0) / 2f +
        itemSize.coerceAtLeast(0) / 2f

internal fun lyricBlurShouldDisableForBrowsing(
    isUserBrowsingLyrics: Boolean,
    @Suppress("UNUSED_PARAMETER") isManualScrolling: Boolean,
): Boolean = isUserBrowsingLyrics

internal const val LYRIC_CENTERING_BASE_STIFFNESS = 80f
internal const val LYRIC_CENTERING_REFERENCE_INTERVAL_MS = 1_000L
internal const val LYRIC_CENTERING_MAX_STIFFNESS = 1_200f

internal fun lyricCenteringSpringStiffness(nextTimestampGapMs: Long?): Float {
    val gapMs = nextTimestampGapMs?.takeIf { it > 0L }
        ?: return LYRIC_CENTERING_BASE_STIFFNESS
    val speedRatio = (
        LYRIC_CENTERING_REFERENCE_INTERVAL_MS.toFloat() / gapMs.toFloat()
    ).coerceAtLeast(1f)
    return (
        LYRIC_CENTERING_BASE_STIFFNESS * speedRatio * speedRatio
    ).coerceAtMost(LYRIC_CENTERING_MAX_STIFFNESS)
}

internal fun lyricLineVerticalPaddingDp(
    hasTimedWords: Boolean,
    hasTranslation: Boolean,
    showLyricsTranslation: Boolean,
): Float {
    val basePadding = if (hasTimedWords) 6f else 10f
    val translationVisible = hasTranslation && showLyricsTranslation
    return if (translationVisible) basePadding else basePadding + 4f
}

internal fun correctedLyricClockMs(
    predictedPositionMs: Double,
    publishedPositionMs: Long,
): Double {
    val correctionMs = publishedPositionMs - predictedPositionMs
    return if (abs(correctionMs) >= 80.0) {
        publishedPositionMs.toDouble()
    } else {
        predictedPositionMs + correctionMs * 0.75
    }
}

@Composable
private fun rememberSmoothLyricTimeProvider(
    document: LyricsDocument,
    positionMs: Long,
    isPlaying: Boolean,
    seekRequestKey: Int,
    seekPositionMs: Long,
): () -> Long {
    val latestPositionMs by rememberUpdatedState(positionMs)
    val latestSeekPositionMs by rememberUpdatedState(seekPositionMs)
    val smoothPositionMs = remember(document) { mutableLongStateOf(positionMs) }
    var handledSeekRequestKey by remember(document) {
        mutableIntStateOf(seekRequestKey)
    }

    LaunchedEffect(document, isPlaying, seekRequestKey, seekPositionMs) {
        val seekRequestChanged = seekRequestKey != handledSeekRequestKey
        val initialPositionMs = lyricClockStartPosition(
            currentSmoothPositionMs = smoothPositionMs.longValue,
            publishedPositionMs = latestPositionMs,
            seekPositionMs = latestSeekPositionMs,
            seekRequestKey = seekRequestKey,
            seekRequestChanged = seekRequestChanged,
            isPlaying = isPlaying,
        )
        handledSeekRequestKey = seekRequestKey
        val publishedPositionAtStartMs = latestPositionMs.coerceAtLeast(0L)
        if (!isPlaying) {
            smoothPositionMs.longValue = initialPositionMs
            return@LaunchedEffect
        }
        var precisePositionMs = initialPositionMs.toDouble()
        var lastPublishedPositionMs = publishedPositionAtStartMs
        var awaitingSeekConfirmation = isPlaying &&
            seekRequestChanged &&
            seekRequestKey > 0 &&
            initialPositionMs != publishedPositionAtStartMs
        var previousFrameNanos = withFrameNanos { it }
        smoothPositionMs.longValue = initialPositionMs
        while (isActive) {
            val frameNanos = withFrameNanos { it }
            val frameDeltaMs = ((frameNanos - previousFrameNanos) / 1_000_000.0)
                .coerceIn(0.0, 100.0)
            precisePositionMs += frameDeltaMs
            previousFrameNanos = frameNanos

            val publishedPositionMs = latestPositionMs
            if (publishedPositionMs != lastPublishedPositionMs) {
                val acceptsPublishedPosition = !awaitingSeekConfirmation ||
                    shouldAcceptPublishedLyricPosition(
                        publishedPositionMs = publishedPositionMs,
                        seekPositionMs = initialPositionMs,
                        positionAtSeekRequestMs = publishedPositionAtStartMs,
                    )
                if (acceptsPublishedPosition) {
                    awaitingSeekConfirmation = false
                    // The controller publishes a position every 500 ms. A slow correction
                    // leaves the lyric clock permanently behind the playing audio, so large
                    // publication gaps must be adopted immediately.
                    precisePositionMs = correctedLyricClockMs(
                        predictedPositionMs = precisePositionMs,
                        publishedPositionMs = publishedPositionMs,
                    )
                }
                lastPublishedPositionMs = publishedPositionMs
            }
            smoothPositionMs.longValue = precisePositionMs.roundToLong().coerceAtLeast(0L)
        }
    }

    return remember(smoothPositionMs) { { smoothPositionMs.longValue } }
}

@Composable
internal fun LyricsView(
    document: LyricsDocument,
    positionMs: Long,
    previewPositionMs: Long?,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    contentWidth: Dp,
    lyricFontScale: Float,
    lyricFontWeight: Int,
    forceWordByWordLyrics: Boolean,
    lyricBlurEnabled: Boolean,
    centerLyrics: Boolean,
    centerOffsetY: Dp = 0.dp,
    showLyricsTranslation: Boolean,
    showBottomFade: Boolean,
    resumeFollowRequestKey: Int,
    seekRequestKey: Int,
    seekPositionMs: Long,
    emphasisColor: Color,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val currentTimeProvider = rememberSmoothLyricTimeProvider(
        document = document,
        positionMs = positionMs,
        isPlaying = isPlaying,
        seekRequestKey = seekRequestKey,
        seekPositionMs = seekPositionMs,
    )
    var appliedSeekRequestKey by remember(document) {
        mutableIntStateOf(seekRequestKey)
    }
    LaunchedEffect(seekRequestKey, seekPositionMs) {
        if (seekRequestKey == appliedSeekRequestKey) return@LaunchedEffect
        if (seekRequestKey == 0) {
            appliedSeekRequestKey = 0
            return@LaunchedEffect
        }
        withFrameNanos { }
        while (
            isActive &&
                !lyricSeekPositionIsApplied(
                    currentPositionMs = currentTimeProvider(),
                    seekPositionMs = seekPositionMs,
                )
        ) {
            withFrameNanos { }
        }
        if (isActive) {
            appliedSeekRequestKey = seekRequestKey
        }
    }
    val latestSeekRequestKey = rememberUpdatedState(seekRequestKey)
    val latestAppliedSeekRequestKey = rememberUpdatedState(appliedSeekRequestKey)
    val latestSeekPositionMs = rememberUpdatedState(seekPositionMs)
    val latestPreviewPositionMs = rememberUpdatedState(previewPositionMs)
    val lyricTimeProvider = remember(currentTimeProvider) {
        {
            lyricDisplayedPositionMs(
                previewPositionMs = latestPreviewPositionMs.value,
                seekRequestPending =
                    latestSeekRequestKey.value != latestAppliedSeekRequestKey.value,
                seekPositionMs = latestSeekPositionMs.value,
                smoothPositionMs = currentTimeProvider(),
            )
        }
    }
    val density = LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current
    val centerOffsetPx = with(density) { centerOffsetY.toPx() }
    val keepAliveZone = 100.dp
    val renderItems = remember(document) { document.renderItems() }
    val renderIndexMap = remember(
        renderItems,
        document.lines.size,
        document.transitions.size,
    ) {
        buildLyricsRenderIndexMap(
            renderItems = renderItems,
            lineCount = document.lines.size,
            transitionCount = document.transitions.size,
        )
    }
    val lineRenderIndices = renderIndexMap.lineRenderIndices
    val transitionRenderIndices = renderIndexMap.transitionRenderIndices
    val visualCurrentLineIndex by remember(document) {
        derivedStateOf { document.visualLineIndex(lyricTimeProvider()) }
    }
    val focusLineIndex by remember(document) {
        derivedStateOf { document.visualFocusLineIndex(lyricTimeProvider()) }
    }
    var isUserBrowsingLyrics by remember(document) { mutableStateOf(false) }
    val isPreviewing = previewPositionMs != null
    val programmaticTranslationY = remember(document) { Animatable(0f) }
    var lastVisualFocusRenderIndex by remember(document) { mutableIntStateOf(-1) }
    val scrollInCode = remember { mutableStateOf(false) }
    val isManualScrolling by remember {
        derivedStateOf {
            lyricScrollIsManual(
                listIsScrolling = listState.isScrollInProgress,
                scrollInCode = scrollInCode.value,
            )
        }
    }
    var hasPositionedInitialFocus by remember(document) { mutableStateOf(false) }
    var lastCenterOffsetY by remember(document) { mutableStateOf(centerOffsetY) }
    val activeTransitionIndex by remember(document) {
        derivedStateOf { document.transitionIndex(lyricTimeProvider()) }
    }
    val playbackFocusRenderIndex = when {
        activeTransitionIndex >= 0 -> transitionRenderIndices
            .getOrNull(activeTransitionIndex)
            ?: -1
        else -> lineRenderIndices.getOrNull(focusLineIndex) ?: -1
    }
    val visualFocusRenderIndex = playbackFocusRenderIndex
    val normalTextStyle = MiuixTheme.textStyles.title3.copy(
        fontSize = (LYRIC_PRIMARY_FONT_SIZE_SP * lyricFontScale).sp,
        lineHeight = (LYRIC_PRIMARY_LINE_HEIGHT_SP * lyricFontScale).sp,
        fontWeight = FontWeight(lyricFontWeight.coerceIn(1, 1000)),
        fontSynthesis = FontSynthesis.None,
        textDirection = TextDirection.Content,
        textMotion = TextMotion.Animated,
    )
    val translationTextStyle = MiuixTheme.textStyles.body1.copy(
        fontSize = (LYRIC_TRANSLATION_FONT_SIZE_SP * lyricFontScale).sp,
        lineHeight = (LYRIC_TRANSLATION_LINE_HEIGHT_SP * lyricFontScale).sp,
        fontWeight = FontWeight(lyricFontWeight.coerceIn(1, 1000)),
        fontSynthesis = FontSynthesis.None,
        textDirection = TextDirection.Content,
        textMotion = TextMotion.Animated,
    )

    val lyricBrowsingModifier = Modifier.pointerInput(document) {
        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Final,
            )
            var browsingStarted = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                val change = event.changes.firstOrNull { it.id == down.id }
                    ?: break
                if (change.changedToUpIgnoreConsumed()) break
                if (
                    !browsingStarted &&
                        lyricVerticalDragExceedsTouchSlop(
                            horizontalDeltaPx = change.position.x - down.position.x,
                            verticalDeltaPx = change.position.y - down.position.y,
                            touchSlopPx = viewConfiguration.touchSlop,
                        )
                ) {
                    browsingStarted = true
                    isUserBrowsingLyrics = true
                }
            }
        }
    }
    LaunchedEffect(
        isManualScrolling,
        isUserBrowsingLyrics,
        isPlaying,
    ) {
        if (
            !isPlaying ||
                isManualScrolling ||
                !isUserBrowsingLyrics
        ) {
            return@LaunchedEffect
        }
        delay(LYRICS_MANUAL_FOLLOW_RESUME_DELAY_MS)
        if (isPlaying && !isManualScrolling && isUserBrowsingLyrics) {
            isUserBrowsingLyrics = false
        }
    }
    LaunchedEffect(resumeFollowRequestKey) {
        isUserBrowsingLyrics = false
    }
    LaunchedEffect(isUserBrowsingLyrics) {
        if (isUserBrowsingLyrics) {
            programmaticTranslationY.snapTo(0f)
        }
    }
    LaunchedEffect(isPreviewing) {
        if (isPreviewing) {
            isUserBrowsingLyrics = false
        }
    }
    LaunchedEffect(seekRequestKey, seekPositionMs) {
        if (seekRequestKey > 0) {
            isUserBrowsingLyrics = false
        }
    }

    LaunchedEffect(
        document,
        visualFocusRenderIndex,
        isUserBrowsingLyrics,
        centerOffsetY,
    ) {
        if (visualFocusRenderIndex < 0) {
            hasPositionedInitialFocus = true
            return@LaunchedEffect
        }
        if (isUserBrowsingLyrics) return@LaunchedEffect
        val layoutInfo = listState.layoutInfo
        val targetItem = layoutInfo.visibleItemsInfo.firstOrNull {
            it.index == visualFocusRenderIndex
        }
        val animateCentering = lyricSeekUsesAnimatedCentering(
            hasPositionedInitialFocus = hasPositionedInitialFocus,
            centerOffsetUnchanged = lastCenterOffsetY == centerOffsetY,
            isPreviewing = isPreviewing,
        )
        val centeringLineIndex = focusLineIndex.takeIf { activeTransitionIndex < 0 } ?: -1
        val centeringLine = document.lines.getOrNull(centeringLineIndex)
        val nextTimestampGapMs = centeringLine?.let { line ->
            document.lines
                .getOrNull(centeringLineIndex + 1)
                ?.startTimeMs
                ?.minus(line.startTimeMs)
        }
        val centeringSpringStiffness = lyricCenteringSpringStiffness(nextTimestampGapMs)
        val measuredScrollDelta = targetItem?.let { visibleTarget ->
            lyricCenterScrollDelta(
                itemOffset = visibleTarget.offset,
                itemSize = visibleTarget.size,
                viewportStartOffset = layoutInfo.viewportStartOffset,
                viewportEndOffset = layoutInfo.viewportEndOffset,
                centerOffsetPx = centerOffsetPx,
            )
        }
        val estimatedItemSize = layoutInfo.visibleItemsInfo
            .map { it.size }
            .average()
            .takeIf { !it.isNaN() }
            ?.roundToInt()
            ?: with(density) { normalTextStyle.lineHeight.toDp().roundToPx() }
        val offscreenCenteringTravelPx = lyricOffscreenTranslationDistance(
            viewportStartOffset = layoutInfo.viewportStartOffset,
            viewportEndOffset = layoutInfo.viewportEndOffset,
            itemSize = targetItem?.size ?: estimatedItemSize,
        )
        val previousRenderIndex = lastVisualFocusRenderIndex
            .takeIf { it >= 0 }
            ?: visualFocusRenderIndex
        val currentTranslationVelocity = programmaticTranslationY.velocity
        lastCenterOffsetY = centerOffsetY
        if (animateCentering) {
            programmaticTranslationY.snapTo(
                lyricProgrammaticTranslationStart(
                    currentTranslationY = programmaticTranslationY.value,
                    measuredScrollDelta = measuredScrollDelta,
                    targetRenderIndex = visualFocusRenderIndex,
                    previousRenderIndex = previousRenderIndex,
                    offscreenTravelPx = offscreenCenteringTravelPx,
                ),
            )
        } else {
            programmaticTranslationY.snapTo(0f)
        }
        try {
            scrollInCode.value = true
            if (targetItem != null) {
                listState.scrollBy(measuredScrollDelta ?: 0f)
            } else {
                val targetScrollOffset = lyricTargetScrollOffset(
                    itemSize = estimatedItemSize,
                    viewportStartOffset = layoutInfo.viewportStartOffset,
                    viewportEndOffset = layoutInfo.viewportEndOffset,
                    centerOffsetPx = centerOffsetPx,
                )
                listState.scrollToItem(
                    index = visualFocusRenderIndex,
                    scrollOffset = targetScrollOffset,
                )
                listState.layoutInfo.visibleItemsInfo.firstOrNull {
                    it.index == visualFocusRenderIndex
                }?.let { centeredItem ->
                    val centeredLayoutInfo = listState.layoutInfo
                    val correction = lyricCenterScrollDelta(
                        itemOffset = centeredItem.offset,
                        itemSize = centeredItem.size,
                        viewportStartOffset = centeredLayoutInfo.viewportStartOffset,
                        viewportEndOffset = centeredLayoutInfo.viewportEndOffset,
                        centerOffsetPx = centerOffsetPx,
                    )
                    if (abs(correction) >= 0.5f) {
                        listState.scrollBy(correction)
                    }
                }
            }
            hasPositionedInitialFocus = true
        } finally {
            scrollInCode.value = false
        }
        lastVisualFocusRenderIndex = visualFocusRenderIndex
        if (animateCentering) {
            programmaticTranslationY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 1f,
                    stiffness = centeringSpringStiffness,
                ),
                initialVelocity = currentTranslationVelocity,
            )
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val horizontalPadding = ((maxWidth - contentWidth) / 2f).coerceAtLeast(0.dp)
        val centerContentPadding = maxHeight / 2f + keepAliveZone
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .then(lyricBrowsingModifier),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        val (topFadeDp, bottomFadeDp) = lyricEdgeFadeHeights(showBottomFade)
                        val topFade = (topFadeDp.dp.toPx() / size.height.coerceAtLeast(1f))
                            .coerceAtMost(0.5f)
                        val bottomFade =
                            (bottomFadeDp.dp.toPx() / size.height.coerceAtLeast(1f))
                                .coerceAtMost(0.5f)
                        val edgeMask = if (bottomFade > 0f) {
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                topFade to Color.Black,
                                1f - bottomFade to Color.Black,
                                1f to Color.Transparent,
                            )
                        } else {
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                topFade to Color.Black,
                                1f to Color.Black,
                            )
                        }
                        drawContent()
                        drawRect(
                            brush = edgeMask,
                            blendMode = BlendMode.DstIn,
                        )
                    },
            ) {
                LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                .graphicsLayer {
                    alpha = if (hasPositionedInitialFocus) 1f else 0f
                    translationY = programmaticTranslationY.value
                }
                .layout { measurable, constraints ->
                        val extraHeightPx = (keepAliveZone * 2).roundToPx()
                        val placeable = measurable.measure(
                            constraints.copy(
                                maxHeight = constraints.maxHeight + extraHeightPx,
                            ),
                        )
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeable.place(0, -keepAliveZone.roundToPx())
                        }
                    },
                contentPadding = PaddingValues(
                    start = horizontalPadding,
                    top = centerContentPadding,
                    end = horizontalPadding,
                    bottom = centerContentPadding,
                ),
            ) {
                itemsIndexed(
                    items = renderItems,
                    key = { index, item ->
                        when (item) {
                            is LyricsRenderItem.Line ->
                                "line-${item.line.startTimeMs}-${item.line.endTimeMs}-${item.lineIndex}"
                            is LyricsRenderItem.Transition ->
                                "transition-${item.transition.startTimeMs}-${item.transition.endTimeMs}-${item.transitionIndex}"
                        }
                    },
                ) { index, item ->
                    val distanceFromFocus = if (visualFocusRenderIndex >= 0) {
                        kotlin.math.abs(index - visualFocusRenderIndex)
                    } else {
                        0
                    }
                    val blurRadius by animateFloatAsState(
                        targetValue = lyricBlurRadiusTarget(
                            lyricBlurEnabled = lyricBlurEnabled,
                            distanceFromFocus = distanceFromFocus,
                            isUserBrowsingLyrics = lyricBlurShouldDisableForBrowsing(
                                isUserBrowsingLyrics = isUserBrowsingLyrics,
                                isManualScrolling = isManualScrolling,
                            ),
                        ),
                        animationSpec = tween(300),
                        label = "lyricBlurRadius",
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        when (item) {
                            is LyricsRenderItem.Line -> {
                                val line = item.line
                                val isFocused = item.lineIndex == visualCurrentLineIndex
                                LyricsLineItem(
                                    isFocused = isFocused,
                                    centerLyrics = centerLyrics,
                                    blurRadius = blurRadius,
                                    onClick = {
                                        isUserBrowsingLyrics = false
                                        onSeek(line.startTimeMs)
                                    },
                                ) {
                                    if (line.words.isNotEmpty()) {
                                        TimedLyricLine(
                                            line = line,
                                            currentTimeProvider = lyricTimeProvider,
                                            normalTextStyle = normalTextStyle,
                                            translationTextStyle = translationTextStyle,
                                            emphasisColor = emphasisColor,
                                            centerLyrics = centerLyrics,
                                            showLyricsTranslation = showLyricsTranslation,
                                        )
                                    } else {
                                        SyncedLyricLine(
                                            line = line,
                                            isFocused = isFocused,
                                            currentTimeProvider = lyricTimeProvider,
                                            forceWordByWordLyrics = forceWordByWordLyrics,
                                            normalTextStyle = normalTextStyle,
                                            translationTextStyle = translationTextStyle,
                                            emphasisColor = emphasisColor,
                                            centerLyrics = centerLyrics,
                                            showLyricsTranslation = showLyricsTranslation,
                                        )
                                    }
                                }
                            }
                            is LyricsRenderItem.Transition -> {
                                LyricTransitionItem(
                                    transition = item.transition,
                                    positionMs = lyricTimeProvider(),
                                    lyricFontScale = lyricFontScale,
                                    emphasisColor = emphasisColor,
                                    centerLyrics = centerLyrics,
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun LyricsLineItem(
    isFocused: Boolean,
    centerLyrics: Boolean,
    blurRadius: Float,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.98f,
        animationSpec = if (isFocused) {
            tween(durationMillis = 600, easing = LinearOutSlowInEasing)
        } else {
            tween(durationMillis = 300, easing = EaseInOut)
        },
        label = "lyricFocusScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.4f,
        animationSpec = tween(
            durationMillis = LYRIC_FOCUS_ALPHA_ANIMATION_DURATION_MS,
            easing = LinearOutSlowInEasing,
        ),
        label = "lyricFocusAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
                transformOrigin = TransformOrigin(
                    pivotFractionX = if (centerLyrics) 0.5f else 0f,
                    pivotFractionY = 1f,
                )
                compositingStrategy = CompositingStrategy.Offscreen
                if (blurRadius > 0f) {
                    renderEffect = BlurEffect(
                        radiusX = blurRadius,
                        radiusY = blurRadius,
                        edgeTreatment = TileMode.Clamp,
                    )
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        content()
    }
}

@Composable
private fun LyricTransitionItem(
    transition: LyricTransition,
    positionMs: Long,
    lyricFontScale: Float,
    emphasisColor: Color,
    centerLyrics: Boolean,
) {
    val scale = lyricFontScale.coerceIn(0.7f, 1.3f)
    val dotSize = 11.dp * scale
    val dotSpacing = 7.dp * scale
    val verticalPadding = lyricTransitionVerticalPaddingDp(
        afterLineIndex = transition.afterLineIndex,
    ).dp * scale
    val isActive = transition.isActive(positionMs)
    val visibilityAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(180),
        label = "lyricTransitionVisibility",
    )
    val progress = transition.progress(positionMs)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = visibilityAlpha },
        horizontalArrangement = if (centerLyrics) {
            Arrangement.Center
        } else {
            Arrangement.Start
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { dotIndex ->
            val dotProgress = ((progress - dotIndex / 3f) / (1f / 3f))
                .coerceIn(0f, 1f)
            Canvas(
                modifier = Modifier
                    .padding(vertical = verticalPadding)
                    .size(dotSize),
            ) {
                drawCircle(
                    color = emphasisColor.copy(alpha = 0.2f + 0.8f * dotProgress),
                )
            }
            if (dotIndex < 2) Spacer(Modifier.size(width = dotSpacing, height = 1.dp))
        }
    }
}

@Composable
private fun TimedLyricLine(
    line: LyricLine,
    currentTimeProvider: () -> Long,
    normalTextStyle: TextStyle,
    translationTextStyle: TextStyle,
    emphasisColor: Color,
    centerLyrics: Boolean,
    showLyricsTranslation: Boolean,
) {
    val textMeasurer = rememberTextMeasurer()
    val verticalPadding = lyricLineVerticalPaddingDp(
        hasTimedWords = true,
        hasTranslation = line.translation != null,
        showLyricsTranslation = showLyricsTranslation,
    ).dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = verticalPadding),
        horizontalAlignment = if (centerLyrics) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val density = LocalDensity.current
            val availableWidthPx = with(density) { maxWidth.toPx() }
            val syllables = remember(line.words) {
                line.words.map { word ->
                    Syllable(
                        startTimeMs = word.startTimeMs,
                        endTimeMs = word.endTimeMs,
                        content = word.text + if (word.hasTrailingSpace) " " else "",
                    )
                }
            }
            val spaceWidth = remember(textMeasurer, normalTextStyle) {
                textMeasurer.measure(" ", normalTextStyle).size.width.toFloat()
            }
            val measuredSyllables = remember(
                syllables,
                textMeasurer,
                normalTextStyle,
                spaceWidth,
            ) {
                measureSyllables(
                    syllables = syllables,
                    textMeasurer = textMeasurer,
                    style = normalTextStyle,
                    spaceWidth = spaceWidth,
                )
            }
            val wrappedLines = remember(
                measuredSyllables,
                availableWidthPx,
                textMeasurer,
                normalTextStyle,
            ) {
                calculateBalancedLines(
                    syllableLayouts = measuredSyllables,
                    availableWidthPx = availableWidthPx,
                    textMeasurer = textMeasurer,
                    style = normalTextStyle,
                )
            }
            val lineHeight = remember(textMeasurer, normalTextStyle) {
                textMeasurer.measure("M", normalTextStyle).size.height.toFloat()
            }
            val positionedLines = remember(
                wrappedLines,
                availableWidthPx,
                lineHeight,
                centerLyrics,
            ) {
                calculateStaticLineLayout(
                    wrappedLines = wrappedLines,
                    centerLyrics = centerLyrics,
                    canvasWidth = availableWidthPx,
                    lineHeight = lineHeight,
                )
            }
            val rowRenderData = remember(positionedLines, density) {
                calculateRowRenderData(
                    lineLayouts = positionedLines,
                    density = density.density,
                )
            }
            val totalHeight = (lineHeight * wrappedLines.size).roundToInt() + 8

            Canvas(
                modifier = Modifier.size(
                    width = maxWidth,
                    height = with(density) { totalHeight.toDp() },
                ),
            ) {
                drawLyricsLine(
                    rows = rowRenderData,
                    positionMs = currentTimeProvider(),
                    color = emphasisColor,
                )
            }
        }
        if (showLyricsTranslation) {
            line.translation?.let { translation ->
                Text(
                    text = translation,
                    modifier = Modifier.fillMaxWidth(),
                    style = translationTextStyle,
                    color = emphasisColor.copy(alpha = 0.4f),
                    textAlign = if (centerLyrics) TextAlign.Center else TextAlign.Start,
                )
            }
        }
    }
}

@Composable
private fun SyncedLyricLine(
    line: LyricLine,
    isFocused: Boolean,
    currentTimeProvider: () -> Long,
    forceWordByWordLyrics: Boolean,
    normalTextStyle: TextStyle,
    translationTextStyle: TextStyle,
    emphasisColor: Color,
    centerLyrics: Boolean,
    showLyricsTranslation: Boolean,
) {
    val textAlign = if (centerLyrics) TextAlign.Center else TextAlign.Start
    val verticalPadding = lyricLineVerticalPaddingDp(
        hasTimedWords = false,
        hasTranslation = line.translation != null,
        showLyricsTranslation = showLyricsTranslation,
    ).dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = verticalPadding),
        horizontalAlignment = if (centerLyrics) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        if (isFocused && forceWordByWordLyrics) {
            ForcedLyricLineText(
                text = line.displayText,
                startTimeMs = line.startTimeMs,
                endTimeMs = line.endTimeMs,
                currentTimeProvider = currentTimeProvider,
                textStyle = normalTextStyle,
                color = emphasisColor,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text(
                text = line.displayText,
                modifier = Modifier.fillMaxWidth(),
                style = normalTextStyle,
                color = emphasisColor,
                textAlign = textAlign,
            )
        }
        if (showLyricsTranslation) {
            line.translation?.let { translation ->
                Text(
                    text = translation,
                    modifier = Modifier.fillMaxWidth(),
                    style = translationTextStyle,
                    color = emphasisColor.copy(alpha = 0.6f),
                    textAlign = textAlign,
                )
            }
        }
    }
}

@Composable
private fun ForcedLyricLineText(
    text: String,
    startTimeMs: Long,
    endTimeMs: Long,
    currentTimeProvider: () -> Long,
    textStyle: TextStyle,
    color: Color,
    textAlign: TextAlign,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val availableWidthPx = with(density) { maxWidth.roundToPx() }
        val measuredStyle = remember(textStyle, textAlign) {
            textStyle.copy(textAlign = textAlign)
        }
        val textLayoutResult = remember(
            text,
            measuredStyle,
            availableWidthPx,
            textMeasurer,
        ) {
            textMeasurer.measure(
                text = text,
                style = measuredStyle,
                constraints = Constraints(
                    minWidth = availableWidthPx,
                    maxWidth = availableWidthPx,
                ),
            )
        }
        val rowWidths = remember(textLayoutResult) {
            List(textLayoutResult.lineCount.coerceAtLeast(1)) { lineIndex ->
                (
                    textLayoutResult.getLineRight(lineIndex) -
                        textLayoutResult.getLineLeft(lineIndex)
                    ).coerceAtLeast(0f)
            }
        }
        Canvas(
            modifier = Modifier.size(
                width = maxWidth,
                height = with(density) { textLayoutResult.size.height.toDp() },
            ),
        ) {
            val lineProgress = lyricIntervalProgress(
                positionMs = currentTimeProvider(),
                startTimeMs = startTimeMs,
                endTimeMs = endTimeMs,
            )
            drawText(
                textLayoutResult = textLayoutResult,
                color = color.copy(alpha = color.alpha * LYRIC_INACTIVE_TEXT_ALPHA),
            )
            drawForcedLyricRows(
                textLayoutResult = textLayoutResult,
                rowWidths = rowWidths,
                lineProgress = lineProgress,
                color = color,
            )
        }
    }
}

private fun DrawScope.drawForcedLyricRows(
    textLayoutResult: TextLayoutResult,
    rowWidths: List<Float>,
    lineProgress: Float,
    color: Color,
) {
    val lineCount = textLayoutResult.lineCount.coerceAtLeast(1)
    repeat(lineCount) { lineIndex ->
        val rowProgress = forcedLyricRowProgress(
            lineProgress = lineProgress,
            rowIndex = lineIndex,
            rowWidths = rowWidths,
        )
        if (rowProgress <= 0f) return@repeat

        val left = textLayoutResult.getLineLeft(lineIndex)
        val right = textLayoutResult.getLineRight(lineIndex)
        val top = textLayoutResult.getLineTop(lineIndex)
        val bottom = textLayoutResult.getLineBottom(lineIndex)
        val rowWidth = right - left
        if (rowWidth <= 0f || bottom <= top) return@repeat
        val rowBounds = Rect(left, top, right, bottom)

        drawIntoCanvas { canvas ->
            canvas.saveLayer(rowBounds, Paint())
            drawText(
                textLayoutResult = textLayoutResult,
                color = color,
            )
            if (rowProgress < 1f) {
                val fadeRange = (100f / rowWidth).coerceAtMost(1f)
                val fadeCenter = -fadeRange / 2f + (1f + fadeRange) * rowProgress
                val fadeStart = (fadeCenter - fadeRange / 2f).coerceIn(0f, 1f)
                val fadeEnd = (fadeCenter + fadeRange / 2f).coerceIn(0f, 1f)
                drawRect(
                    brush = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0f to Color.White,
                            fadeStart to Color.White,
                            fadeEnd to Color.Transparent,
                            1f to Color.Transparent,
                        ),
                        startX = left,
                        endX = right,
                    ),
                    topLeft = rowBounds.topLeft,
                    size = rowBounds.size,
                    blendMode = BlendMode.DstIn,
                )
            }
            canvas.restore()
        }
    }
}

internal fun forcedLyricRowProgress(
    lineProgress: Float,
    rowIndex: Int,
    rowWidths: List<Float>,
): Float {
    if (rowIndex !in rowWidths.indices) return 0f
    var totalWidth = 0f
    var widthBeforeRow = 0f
    rowWidths.forEachIndexed { index, width ->
        val normalizedWidth = width.coerceAtLeast(0f)
        totalWidth += normalizedWidth
        if (index < rowIndex) widthBeforeRow += normalizedWidth
    }
    if (totalWidth <= 0f) return 0f
    val completedWidth = lineProgress.coerceIn(0f, 1f) * totalWidth
    val rowWidth = rowWidths[rowIndex].coerceAtLeast(0f)
    if (rowWidth <= 0f) return if (completedWidth > widthBeforeRow) 1f else 0f
    return ((completedWidth - widthBeforeRow) / rowWidth).coerceIn(0f, 1f)
}

private data class Syllable(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val content: String,
)

private data class WordAnimationInfo(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val content: String,
) {
    val durationMs: Long = endTimeMs - startTimeMs
}

private data class SyllableLayout(
    val syllable: Syllable,
    val textLayoutResult: TextLayoutResult,
    val wordId: Int,
    val useWordAnimation: Boolean,
    val width: Float = textLayoutResult.size.width.toFloat(),
    val position: Offset = Offset.Zero,
    val wordPivot: Offset = Offset.Zero,
    val wordAnimationInfo: WordAnimationInfo? = null,
    val characterOffsetInWord: Int = 0,
    val characterLayouts: List<TextLayoutResult>? = null,
    val characterOriginalBounds: List<Rect>? = null,
    val firstBaseline: Float = textLayoutResult.firstBaseline,
)

private data class WrappedLine(
    val syllables: List<SyllableLayout>,
    val totalWidth: Float,
)

private data class RowRenderData(
    val layouts: List<SyllableLayout>,
    val minX: Float,
    val maxX: Float,
    val width: Float,
    val firstStartTimeMs: Long,
    val lastEndTimeMs: Long,
    val layerBounds: Rect,
)

private fun measureSyllables(
    syllables: List<Syllable>,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    spaceWidth: Float,
): List<SyllableLayout> {
    val words = groupSyllablesIntoWords(syllables)
    return words.flatMapIndexed { wordIndex, word ->
        val wordContent = word.joinToString(separator = "") { it.content }
        val wordDurationMs = if (word.isEmpty()) {
            0L
        } else {
            word.last().endTimeMs - word.first().startTimeMs
        }
        val useWordAnimation = shouldUseWordAnimation(
            content = wordContent,
            durationMs = wordDurationMs,
        )
        word.map { syllable ->
            val layoutResult = textMeasurer.measure(syllable.content, style)
            var layoutWidth = layoutResult.size.width.toFloat()
            if (syllable.content.endsWith(" ")) {
                val trimmedContent = syllable.content.trimEnd()
                val trimmedWidth = textMeasurer.measure(trimmedContent, style)
                    .size.width.toFloat()
                if (layoutWidth <= trimmedWidth) {
                    val spaceCount = syllable.content.length - trimmedContent.length
                    layoutWidth = trimmedWidth + spaceWidth * spaceCount
                }
            }
            val characterLayouts = if (useWordAnimation) {
                syllable.content.map { character ->
                    textMeasurer.measure(character.toString(), style)
                }
            } else {
                null
            }
            val characterBounds = if (useWordAnimation) {
                syllable.content.indices.map(layoutResult::getBoundingBox)
            } else {
                null
            }
            SyllableLayout(
                syllable = syllable,
                textLayoutResult = layoutResult,
                wordId = wordIndex,
                useWordAnimation = useWordAnimation,
                width = layoutWidth,
                characterLayouts = characterLayouts,
                characterOriginalBounds = characterBounds,
            )
        }
    }
}

private fun groupSyllablesIntoWords(
    syllables: List<Syllable>,
): List<List<Syllable>> {
    if (syllables.isEmpty()) return emptyList()
    val words = mutableListOf<List<Syllable>>()
    var currentWord = mutableListOf<Syllable>()
    syllables.forEach { syllable ->
        currentWord += syllable
        if (syllable.content.trimEnd().length < syllable.content.length) {
            words += currentWord.toList()
            currentWord = mutableListOf()
        }
    }
    if (currentWord.isNotEmpty()) words += currentWord.toList()
    return words
}

internal fun shouldUseWordAnimation(
    content: String,
    durationMs: Long,
): Boolean {
    if (content.isEmpty() || durationMs < 1_000L) return false
    val perCharacterDurationMs = durationMs.toFloat() / content.length
    return perCharacterDurationMs > 200f && !content.shouldUseSimpleAnimation()
}

private fun String.shouldUseSimpleAnimation(): Boolean {
    val visibleCharacters = filterNot { character ->
        character.isWhitespace() || character.isPunctuation()
    }
    if (visibleCharacters.isEmpty()) return false
    return visibleCharacters.all(Char::isCjk) ||
        visibleCharacters.any { it.isArabic() || it.isDevanagari() }
}

private fun Char.isCjk(): Boolean = code in 0x3400..0x4DBF ||
    code in 0x4E00..0x9FFF ||
    code in 0xF900..0xFAFF

private fun Char.isArabic(): Boolean = code in 0x0600..0x06FF ||
    code in 0x0750..0x077F ||
    code in 0x08A0..0x08FF

private fun Char.isDevanagari(): Boolean = code in 0x0900..0x097F

private fun Char.isPunctuation(): Boolean = when (Character.getType(this)) {
    Character.CONNECTOR_PUNCTUATION.toInt(),
    Character.DASH_PUNCTUATION.toInt(),
    Character.START_PUNCTUATION.toInt(),
    Character.END_PUNCTUATION.toInt(),
    Character.INITIAL_QUOTE_PUNCTUATION.toInt(),
    Character.FINAL_QUOTE_PUNCTUATION.toInt(),
    Character.OTHER_PUNCTUATION.toInt(),
    -> true
    else -> false
}

private fun calculateBalancedLines(
    syllableLayouts: List<SyllableLayout>,
    availableWidthPx: Float,
    textMeasurer: TextMeasurer,
    style: TextStyle,
): List<WrappedLine> {
    if (syllableLayouts.isEmpty()) return emptyList()
    val itemCount = syllableLayouts.size
    val costs = DoubleArray(itemCount + 1) { Double.POSITIVE_INFINITY }
    val breaks = IntArray(itemCount + 1)
    costs[0] = 0.0

    for (endIndex in 1..itemCount) {
        var currentLineWidth = 0f
        for (startIndex in endIndex downTo 1) {
            if (
                startIndex > 1 &&
                syllableLayouts[startIndex - 2].wordId ==
                syllableLayouts[startIndex - 1].wordId
            ) {
                currentLineWidth += syllableLayouts[startIndex - 1].width
                if (currentLineWidth > availableWidthPx) break
                continue
            }
            currentLineWidth += syllableLayouts[startIndex - 1].width
            if (currentLineWidth > availableWidthPx) break
            val badness = (availableWidthPx - currentLineWidth).pow(2).toDouble()
            if (
                costs[startIndex - 1] != Double.POSITIVE_INFINITY &&
                costs[startIndex - 1] + badness < costs[endIndex]
            ) {
                costs[endIndex] = costs[startIndex - 1] + badness
                breaks[endIndex] = startIndex - 1
            }
        }
    }

    if (costs[itemCount] == Double.POSITIVE_INFINITY) {
        return calculateGreedyLines(
            syllableLayouts = syllableLayouts,
            availableWidthPx = availableWidthPx,
            textMeasurer = textMeasurer,
            style = style,
        )
    }

    val lines = mutableListOf<WrappedLine>()
    var currentIndex = itemCount
    while (currentIndex > 0) {
        val startIndex = breaks[currentIndex]
        lines.add(
            index = 0,
            element = trimLineTrailingSpaces(
                layouts = syllableLayouts.subList(startIndex, currentIndex),
                textMeasurer = textMeasurer,
                style = style,
            ),
        )
        currentIndex = startIndex
    }
    return lines
}

private fun calculateGreedyLines(
    syllableLayouts: List<SyllableLayout>,
    availableWidthPx: Float,
    textMeasurer: TextMeasurer,
    style: TextStyle,
): List<WrappedLine> {
    val lines = mutableListOf<WrappedLine>()
    val currentLine = mutableListOf<SyllableLayout>()
    var currentLineWidth = 0f
    val wordGroups = syllableLayouts.groupBy(SyllableLayout::wordId).values

    wordGroups.forEach { wordLayouts ->
        val wordWidth = wordLayouts.sumOf { it.width.toDouble() }.toFloat()
        if (currentLineWidth + wordWidth <= availableWidthPx) {
            if (wordWidth <= availableWidthPx) {
                currentLine += wordLayouts
                currentLineWidth += wordWidth
            } else {
                wordLayouts.forEach { syllable ->
                    if (
                        currentLineWidth + syllable.width > availableWidthPx &&
                        currentLine.isNotEmpty()
                    ) {
                        lines += trimLineTrailingSpaces(
                            currentLine,
                            textMeasurer,
                            style,
                        )
                        currentLine.clear()
                        currentLineWidth = 0f
                    }
                    currentLine += syllable
                    currentLineWidth += syllable.width
                }
            }
        } else {
            if (currentLine.isNotEmpty()) {
                lines += trimLineTrailingSpaces(
                    currentLine,
                    textMeasurer,
                    style,
                )
                currentLine.clear()
                currentLineWidth = 0f
            }
            currentLine += wordLayouts
            currentLineWidth += wordWidth
        }
    }
    if (currentLine.isNotEmpty()) {
        lines += trimLineTrailingSpaces(currentLine, textMeasurer, style)
    }
    return lines
}

private fun trimLineTrailingSpaces(
    layouts: List<SyllableLayout>,
    textMeasurer: TextMeasurer,
    style: TextStyle,
): WrappedLine {
    if (layouts.isEmpty()) return WrappedLine(emptyList(), 0f)
    val processed = layouts.toMutableList()
    while (processed.lastOrNull()?.syllable?.content?.isBlank() == true) {
        processed.removeAt(processed.lastIndex)
    }
    if (processed.isEmpty()) return WrappedLine(emptyList(), 0f)

    val lastLayout = processed.last()
    val trimmedContent = lastLayout.syllable.content.trimEnd()
    if (trimmedContent.length < lastLayout.syllable.content.length) {
        if (trimmedContent.isEmpty()) {
            processed.removeAt(processed.lastIndex)
        } else {
            val trimmedResult = textMeasurer.measure(trimmedContent, style)
            processed[processed.lastIndex] = lastLayout.copy(
                syllable = lastLayout.syllable.copy(content = trimmedContent),
                textLayoutResult = trimmedResult,
                width = trimmedResult.size.width.toFloat(),
            )
        }
    }
    return WrappedLine(
        syllables = processed,
        totalWidth = processed.sumOf { it.width.toDouble() }.toFloat(),
    )
}

private fun calculateStaticLineLayout(
    wrappedLines: List<WrappedLine>,
    centerLyrics: Boolean,
    canvasWidth: Float,
    lineHeight: Float,
): List<List<SyllableLayout>> {
    val layoutsByWord = mutableMapOf<Int, MutableList<SyllableLayout>>()
    val positionedLines = wrappedLines.mapIndexed { lineIndex, wrappedLine ->
        val maxBaseline = wrappedLine.syllables.maxOfOrNull { it.firstBaseline } ?: 0f
        val startX = if (centerLyrics) {
            (canvasWidth - wrappedLine.totalWidth) / 2f
        } else {
            0f
        }
        var currentX = startX
        wrappedLine.syllables.map { initialLayout ->
            val positionedLayout = initialLayout.copy(
                position = Offset(
                    x = currentX,
                    y = lineIndex * lineHeight + maxBaseline - initialLayout.firstBaseline,
                ),
            )
            layoutsByWord.getOrPut(positionedLayout.wordId) { mutableListOf() }
                .add(positionedLayout)
            currentX += positionedLayout.width
            positionedLayout
        }
    }

    val animationInfoByWord = mutableMapOf<Int, WordAnimationInfo>()
    val characterOffsets = mutableMapOf<SyllableLayout, Int>()
    layoutsByWord.forEach { (wordId, layouts) ->
        if (layouts.first().useWordAnimation) {
            animationInfoByWord[wordId] = WordAnimationInfo(
                startTimeMs = layouts.minOf { it.syllable.startTimeMs },
                endTimeMs = layouts.maxOf { it.syllable.endTimeMs },
                content = layouts.joinToString(separator = "") { it.syllable.content },
            )
            var characterOffset = 0
            layouts.forEach { layout ->
                characterOffsets[layout] = characterOffset
                characterOffset += layout.syllable.content.length
            }
        }
    }

    return positionedLines.map { line ->
        line.map { positionedLayout ->
            val wordLayouts = layoutsByWord.getValue(positionedLayout.wordId)
            val minX = wordLayouts.minOf { it.position.x }
            val maxX = wordLayouts.maxOf { it.position.x + it.width }
            val bottomY = wordLayouts.maxOf {
                it.position.y + it.textLayoutResult.size.height
            }
            positionedLayout.copy(
                wordPivot = Offset((minX + maxX) / 2f, bottomY),
                wordAnimationInfo = animationInfoByWord[positionedLayout.wordId],
                characterOffsetInWord = characterOffsets[positionedLayout] ?: 0,
            )
        }
    }
}

private fun calculateRowRenderData(
    lineLayouts: List<List<SyllableLayout>>,
    density: Float,
): List<RowRenderData> = lineLayouts.mapNotNull { layouts ->
    if (layouts.isEmpty()) return@mapNotNull null
    val minX = layouts.minOf { it.position.x }
    val maxX = layouts.maxOf { it.position.x + it.width }
    val width = maxX - minX
    val minY = layouts.minOf { it.position.y }
    val height = layouts.maxOf { it.textLayoutResult.size.height }.toFloat()
    val verticalPadding = height * 0.1f * density
    val horizontalPadding = width * 0.2f * density
    val edgePadding = 8f * density
    RowRenderData(
        layouts = layouts,
        minX = minX,
        maxX = maxX,
        width = width,
        firstStartTimeMs = layouts.first().syllable.startTimeMs,
        lastEndTimeMs = layouts.last().syllable.endTimeMs,
        layerBounds = Rect(
            left = minX - horizontalPadding,
            top = minY - verticalPadding - edgePadding,
            right = maxX + horizontalPadding,
            bottom = minY + height + verticalPadding + edgePadding,
        ),
    )
}

private fun DrawScope.drawLyricsLine(
    rows: List<RowRenderData>,
    positionMs: Long,
    color: Color,
) {
    rows.forEach { row ->
        if (positionMs >= row.lastEndTimeMs) {
            drawRowText(row.layouts, color, positionMs)
            return@forEach
        }
        drawIntoCanvas { canvas ->
            canvas.saveLayer(row.layerBounds, Paint())
            drawRowText(row.layouts, color, positionMs)
            drawRect(
                brush = createLineGradient(row, positionMs),
                topLeft = row.layerBounds.topLeft,
                size = row.layerBounds.size,
                blendMode = BlendMode.DstIn,
            )
            canvas.restore()
        }
    }
}

private fun createLineGradient(
    row: RowRenderData,
    positionMs: Long,
): Brush {
    val activeColor = Color.White
    val inactiveColor = Color.White.copy(alpha = LYRIC_INACTIVE_TEXT_ALPHA)
    if (row.width <= 0f) {
        return Brush.horizontalGradient(
            listOf(
                if (positionMs >= row.lastEndTimeMs) activeColor else inactiveColor,
                if (positionMs >= row.lastEndTimeMs) activeColor else inactiveColor,
            ),
        )
    }
    if (positionMs <= row.firstStartTimeMs) {
        return Brush.horizontalGradient(listOf(inactiveColor, inactiveColor))
    }
    if (positionMs >= row.lastEndTimeMs) {
        return Brush.horizontalGradient(listOf(activeColor, activeColor))
    }

    val activeLayout = row.layouts.firstOrNull { layout ->
        positionMs >= layout.syllable.startTimeMs &&
            positionMs < layout.syllable.endTimeMs
    }
    val currentPixelPosition = if (activeLayout != null) {
        activeLayout.position.x + activeLayout.width * lyricIntervalProgress(
            positionMs = positionMs,
            startTimeMs = activeLayout.syllable.startTimeMs,
            endTimeMs = activeLayout.syllable.endTimeMs,
        )
    } else {
        row.layouts.lastOrNull { positionMs >= it.syllable.endTimeMs }
            ?.let { it.position.x + it.width }
            ?: row.minX
    }
    val lineProgress = ((currentPixelPosition - row.minX) / row.width).coerceIn(0f, 1f)
    val fadeRange = (100f / row.width).coerceAtMost(1f)
    val fadeCenter = -fadeRange / 2f + (1f + fadeRange) * lineProgress
    val fadeStart = (fadeCenter - fadeRange / 2f).coerceIn(0f, 1f)
    val fadeEnd = (fadeCenter + fadeRange / 2f).coerceIn(0f, 1f)
    return Brush.horizontalGradient(
        colorStops = arrayOf(
            0f to activeColor,
            fadeStart to activeColor,
            fadeEnd to inactiveColor,
            1f to inactiveColor,
        ),
        startX = row.minX,
        endX = row.maxX,
    )
}

private fun DrawScope.drawRowText(
    layouts: List<SyllableLayout>,
    color: Color,
    positionMs: Long,
) {
    layouts.forEachIndexed { index, layout ->
        val wordAnimationInfo = layout.wordAnimationInfo
        if (wordAnimationInfo != null) {
            val characterLayouts = layout.characterLayouts.orEmpty()
            val characterBounds = layout.characterOriginalBounds.orEmpty()
            val characterCount = wordAnimationInfo.content.length
            layout.syllable.content.forEachIndexed { characterIndex, _ ->
                val characterLayout = characterLayouts.getOrNull(characterIndex)
                    ?: return@forEachIndexed
                val characterBox = characterBounds.getOrNull(characterIndex)
                    ?: return@forEachIndexed
                val absoluteCharacterIndex = layout.characterOffsetInWord + characterIndex
                val progress = characterProgress(
                    positionMs = positionMs,
                    wordStartTimeMs = wordAnimationInfo.startTimeMs,
                    wordEndTimeMs = wordAnimationInfo.endTimeMs,
                    characterIndex = absoluteCharacterIndex,
                    characterCount = characterCount,
                )
                val motion = wordMotion(
                    progress = progress,
                    durationMs = wordAnimationInfo.durationMs,
                    characterCount = characterCount,
                )
                val centeredOffsetX =
                    (characterBox.width - characterLayout.size.width) / 2f
                val position = Offset(
                    x = layout.position.x + characterBox.left + centeredOffsetX,
                    y = layout.position.y + characterBox.top + motion.offsetYPx,
                )
                withTransform({
                    scale(
                        scaleX = motion.scale,
                        scaleY = motion.scale,
                        pivot = layout.wordPivot,
                    )
                }) {
                    drawText(
                        textLayoutResult = characterLayout,
                        color = color,
                        topLeft = position,
                        shadow = Shadow(
                            color = color.copy(alpha = 0.4f),
                            offset = Offset.Zero,
                            blurRadius = motion.glowRadius,
                        ),
                    )
                }
            }
        } else {
            val driverLayout = if (layout.syllable.content.trim().all {
                    it.isPunctuation()
                }
            ) {
                layouts.subList(0, index).lastOrNull { candidate ->
                    candidate.syllable.content.trim().any { character ->
                        !character.isPunctuation()
                    }
                } ?: layout
            } else {
                layout
            }
            val floatOffset = simpleFloatOffset(
                positionMs = positionMs,
                startTimeMs = driverLayout.syllable.startTimeMs,
            )
            drawText(
                textLayoutResult = layout.textLayoutResult,
                color = color,
                topLeft = layout.position.copy(y = layout.position.y + floatOffset),
            )
        }
    }
}

internal data class WordMotion(
    val scale: Float,
    val offsetYPx: Float,
    val glowRadius: Float,
)

internal fun lyricIntervalProgress(
    positionMs: Long,
    startTimeMs: Long,
    endTimeMs: Long,
): Float {
    val durationMs = endTimeMs - startTimeMs
    if (durationMs <= 0L) return if (positionMs >= startTimeMs) 1f else 0f
    return ((positionMs - startTimeMs).toFloat() / durationMs).coerceIn(0f, 1f)
}

internal fun characterProgress(
    positionMs: Long,
    wordStartTimeMs: Long,
    wordEndTimeMs: Long,
    characterIndex: Int,
    characterCount: Int,
): Float {
    val wordDurationMs = (wordEndTimeMs - wordStartTimeMs).coerceAtLeast(0L)
    val animationDurationMs = wordDurationMs * 0.8f
    if (animationDurationMs <= 0f) {
        return if (positionMs >= wordStartTimeMs) 1f else 0f
    }
    val boundedCharacterCount = characterCount.coerceAtLeast(1)
    val characterRatio = if (boundedCharacterCount > 1) {
        characterIndex.coerceIn(0, boundedCharacterCount - 1).toFloat() /
            (boundedCharacterCount - 1)
    } else {
        0.5f
    }
    val latestStartTimeMs = wordEndTimeMs - animationDurationMs
    val animationStartTimeMs = wordStartTimeMs +
        (latestStartTimeMs - wordStartTimeMs) * characterRatio
    return ((positionMs - animationStartTimeMs) / animationDurationMs).coerceIn(0f, 1f)
}

internal fun characterMotion(
    positionMs: Long,
    wordStartTimeMs: Long,
    wordEndTimeMs: Long,
    characterIndex: Int,
    characterCount: Int,
): WordMotion = wordMotion(
    progress = characterProgress(
        positionMs = positionMs,
        wordStartTimeMs = wordStartTimeMs,
        wordEndTimeMs = wordEndTimeMs,
        characterIndex = characterIndex,
        characterCount = characterCount,
    ),
    durationMs = wordEndTimeMs - wordStartTimeMs,
    characterCount = characterCount,
)

internal fun wordMotion(
    progress: Float,
    durationMs: Long,
    characterCount: Int,
): WordMotion {
    val animationIntensity = (
        (durationMs.coerceAtLeast(0L) - 200f * characterCount.coerceAtLeast(1)) / 1000f
        )
    val dip = (0.5f * animationIntensity).coerceIn(0f, 0.5f)
    val swell = (0.1f * animationIntensity).coerceIn(0f, 0.1f)
    val boundedProgress = progress.coerceIn(0f, 1f)
    return WordMotion(
        scale = 1f + threePointInterpolation(
            fraction = boundedProgress,
            middleX = 0.5f,
            middleY = swell,
            endY = 0f,
        ),
        offsetYPx = 4f * threePointInterpolation(
            fraction = 1f - boundedProgress,
            middleX = 0.5f,
            middleY = -dip,
            endY = 1f,
        ),
        glowRadius = 10f * threePointInterpolation(
            fraction = boundedProgress,
            middleX = 0.7f,
            middleY = 1f,
            endY = 0f,
        ).coerceAtLeast(0f),
    )
}

internal fun simpleFloatOffset(
    positionMs: Long,
    startTimeMs: Long,
): Float {
    val progress = ((positionMs - startTimeMs) / 700f).coerceIn(0f, 1f)
    return 4f * SimpleFloatEasing.transform(1f - progress)
}

private fun threePointInterpolation(
    fraction: Float,
    middleX: Float,
    middleY: Float,
    endY: Float,
): Float {
    val x = fraction.coerceIn(0f, 1f)
    val middleTerm = middleY * x * (x - 1f) / (middleX * (middleX - 1f))
    val endTerm = endY * x * (x - middleX) / (1f - middleX)
    return middleTerm + endTerm
}

private val SimpleFloatEasing = CubicBezierEasing(0f, 0f, 0.2f, 1f)
