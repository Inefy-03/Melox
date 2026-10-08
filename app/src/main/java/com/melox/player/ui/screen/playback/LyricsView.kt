// SPDX-License-Identifier: Apache-2.0
package com.melox.player.ui.screen.playback

import android.graphics.Typeface
import android.os.SystemClock
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyLayoutScrollScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.LayerOutsets
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melox.player.model.LyricAnimationMode
import com.melox.player.model.LyricLine
import com.melox.player.model.LyricsDocument
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val LYRIC_EDGE_FADE_DP = 100f
private const val LYRIC_ANIMATION_BLEED_DP = 8f
internal const val LYRIC_INACTIVE_TEXT_ALPHA = 0.4f
internal const val LYRICS_MANUAL_FOLLOW_RESUME_DELAY_MS = 5_000L
private const val LYRIC_FOCUS_SCALE_IN_DURATION_MS = 600
private const val LYRIC_FOCUS_SCALE_OUT_DURATION_MS = 300
private const val LYRIC_FOCUS_ALPHA_ANIMATION_DURATION_MS = 180
private const val LYRIC_BLUR_ANIMATION_DURATION_MS = 300
internal const val LYRIC_PRIMARY_FONT_SIZE_SP = 24f
internal const val LYRIC_PRIMARY_LINE_HEIGHT_SP = 28f
internal const val LYRIC_TRANSLATION_FONT_SIZE_SP = 16f
internal const val LYRIC_TRANSLATION_LINE_HEIGHT_SP = 22f
internal const val LYRIC_TRANSLATION_GAP_DP = 2f
private val LyricLayerPaint = Paint()

@Composable
internal fun rememberLyricFontFamily(weight: Int): FontFamily = remember(weight) {
    FontFamily(Typeface.create(Typeface.DEFAULT, weight.coerceIn(1, 1000), false))
}

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

internal fun lyricTranslationAlpha(lineAlpha: Float): Float = (
    LYRIC_INACTIVE_TEXT_ALPHA / lineAlpha.coerceAtLeast(LYRIC_INACTIVE_TEXT_ALPHA)
).coerceIn(0f, 1f)

internal fun lyricLineLayerAlpha(
    lineAlpha: Float,
    usesWordProgress: Boolean,
): Float = if (usesWordProgress) 1f else lineAlpha

internal fun lyricWordProgressActiveAlpha(
    lineAlpha: Float,
    usesWordProgress: Boolean,
): Float = if (usesWordProgress) lineAlpha else 1f

internal fun lyricSeekPositionIsApplied(
    currentPositionMs: Long,
    seekPositionMs: Long,
): Boolean = abs(currentPositionMs - seekPositionMs) <= 250L

internal fun lyricSeekRequestIsAcknowledged(
    requestKey: Int,
    positionUpdateAnchorElapsedRealtimeMs: Long,
    positionUpdateElapsedRealtimeMs: Long,
    currentPositionMs: Long,
    seekPositionMs: Long,
): Boolean {
    if (requestKey == 0) return false
    val hasFreshPosition =
        positionUpdateElapsedRealtimeMs > positionUpdateAnchorElapsedRealtimeMs
    return hasFreshPosition && lyricSeekPositionIsApplied(currentPositionMs, seekPositionMs)
}

internal fun lyricPlaybackPositionMs(
    positionMs: Long,
    positionUpdateElapsedRealtimeMs: Long,
    nowElapsedRealtimeMs: Long,
    isPlaying: Boolean,
    playbackSpeed: Float,
): Long {
    val basePositionMs = positionMs.coerceAtLeast(0L)
    if (!isPlaying || positionUpdateElapsedRealtimeMs <= 0L) return basePositionMs
    val elapsedMs = (nowElapsedRealtimeMs - positionUpdateElapsedRealtimeMs).coerceAtLeast(0L)
    val validSpeed = playbackSpeed.takeIf { it.isFinite() && it > 0f } ?: 1f
    return (basePositionMs + elapsedMs * validSpeed)
        .roundToLong()
        .coerceAtLeast(0L)
}

internal fun stabilizedLyricPlaybackPositionMs(
    previousPositionMs: Double,
    sampledPositionMs: Long,
    frameAdvanceNanos: Long,
    playbackSpeed: Float,
): Double {
    // A suspended UI clock is not evidence that playback ran throughout the gap.
    // Re-anchor to the controller, which may have paused or sought in the background.
    if (frameAdvanceNanos > 250_000_000L) return sampledPositionMs.coerceAtLeast(0L).toDouble()
    val validSpeed = playbackSpeed.takeIf { it.isFinite() && it > 0f } ?: 1f
    val frameAdvancedPositionMs = previousPositionMs.coerceAtLeast(0.0) +
        frameAdvanceNanos.coerceAtLeast(0L) / 1_000_000.0 * validSpeed
    return maxOf(
        sampledPositionMs.coerceAtLeast(0L).toDouble(),
        frameAdvancedPositionMs,
    )
}

internal fun lyricSeekUsesAnimatedCentering(
    hasPositionedInitialFocus: Boolean,
    centerOffsetUnchanged: Boolean,
    isPreviewing: Boolean = false,
): Boolean = hasPositionedInitialFocus && centerOffsetUnchanged && !isPreviewing

internal fun lyricLineRenderPositionMs(
    lineIndex: Int,
    displayedPositionMs: Long,
    outgoingSeekLineIndex: Int,
    outgoingSeekPositionMs: Long,
): Long = if (lineIndex == outgoingSeekLineIndex) {
    outgoingSeekPositionMs
} else {
    displayedPositionMs
}

internal fun lyricOutgoingSeekCanClear(
    lineIndex: Int,
    outgoingSeekLineIndex: Int,
    currentLineIndex: Int,
    settledAlpha: Float,
): Boolean = lineIndex == outgoingSeekLineIndex &&
    currentLineIndex != lineIndex &&
    settledAlpha == LYRIC_INACTIVE_TEXT_ALPHA

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

internal fun lyricRetainedTravelPx(
    translationStartPx: Float,
    velocityPxPerSecond: Float,
    stiffness: Float,
): Float = abs(translationStartPx) + abs(velocityPxPerSecond) / sqrt(stiffness.coerceAtLeast(1f))

internal fun lyricRetentionIsMeasured(
    viewportHeightPx: Int,
    reservePx: Int,
    measuredHeightPx: Int,
    beforePaddingPx: Int,
    afterPaddingPx: Int,
): Boolean = measuredHeightPx == viewportHeightPx + reservePx * 2 &&
    beforePaddingPx == viewportHeightPx / 2 + reservePx &&
    afterPaddingPx == beforePaddingPx

internal fun lyricActualPlacementTranslation(
    previousTranslationPx: Float,
    previousTargetOffsetPx: Int?,
    placedTargetOffsetPx: Int?,
    offscreenTranslationPx: Float,
): Float = if (previousTargetOffsetPx != null && placedTargetOffsetPx != null) {
    previousTranslationPx + previousTargetOffsetPx - placedTargetOffsetPx
} else {
    offscreenTranslationPx
}

internal fun lyricGlyphOutsetPx(textHeightPx: Float): Float =
    maxOf(textHeightPx * 0.72f,
        textHeightPx * 0.12f + 12f * 3f * 1.12f + LYRIC_FLOAT_MAX_OFFSET_PX)

internal fun lyricCharacterPivot(position: Offset, width: Float, height: Float): Offset =
    Offset(position.x + width / 2f, position.y + height)

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

@Suppress("UNUSED_PARAMETER")
internal fun lyricLineVerticalPaddingDp(
    hasTimedWords: Boolean,
    hasTranslation: Boolean,
    showLyricsTranslation: Boolean,
    lyricFontScale: Float = 1f,
): Float {
    val translationVisible = hasTranslation && showLyricsTranslation
    val spacingScale = 1f + (lyricFontScale - 1f) * 0.25f
    return (if (translationVisible) 10f else 12f) * spacingScale
}

@Composable
private fun rememberSmoothLyricTimeProvider(
    document: LyricsDocument,
    positionMs: Long,
    positionUpdateElapsedRealtimeMs: Long,
    playbackIteration: Long,
    playbackSpeed: Float,
    isPlaying: Boolean,
    seekRequestKey: Int,
    seekPositionMs: Long,
): () -> Long {
    val latestPositionMs by rememberUpdatedState(positionMs)
    val latestPositionUpdateElapsedRealtimeMs by rememberUpdatedState(
        positionUpdateElapsedRealtimeMs,
    )
    val latestPlaybackSpeed by rememberUpdatedState(playbackSpeed)
    val latestSeekRequestKey by rememberUpdatedState(seekRequestKey)
    val latestSeekPositionMs by rememberUpdatedState(seekPositionMs)
    val clock = remember(document) { LyricPlaybackClock(positionMs, playbackIteration) }
    val smoothPositionMs = clock.positionMs
    val latestPlaybackIteration by rememberUpdatedState(playbackIteration)

    fun currentPlaybackPositionMs(): Long = lyricPlaybackPositionMs(
        positionMs = latestPositionMs,
        positionUpdateElapsedRealtimeMs = latestPositionUpdateElapsedRealtimeMs,
        nowElapsedRealtimeMs = SystemClock.elapsedRealtime(),
        isPlaying = isPlaying,
        playbackSpeed = latestPlaybackSpeed,
    )

    LaunchedEffect(
        document,
        seekRequestKey,
        seekPositionMs,
        playbackIteration,
    ) {
        clock.resetForIteration(playbackIteration, currentPlaybackPositionMs())
        if (seekRequestKey != 0) {
            smoothPositionMs.doubleValue = seekPositionMs.coerceAtLeast(0L).toDouble()
        }
    }
    LaunchedEffect(document, isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        var previousFrameTimeNanos = 0L
        while (isActive) {
            val frameTimeNanos = withFrameNanos { it }
            val frameAdvanceNanos = if (previousFrameTimeNanos > 0L) {
                (frameTimeNanos - previousFrameTimeNanos).coerceAtLeast(0L)
            } else {
                0L
            }
            previousFrameTimeNanos = frameTimeNanos
            val iterationReset = clock.resetForIteration(
                latestPlaybackIteration, currentPlaybackPositionMs(),
            )
            smoothPositionMs.doubleValue = if (latestSeekRequestKey != 0) {
                latestSeekPositionMs.coerceAtLeast(0L).toDouble()
            } else if (iterationReset) {
                smoothPositionMs.doubleValue
            } else {
                stabilizedLyricPlaybackPositionMs(
                    previousPositionMs = smoothPositionMs.doubleValue,
                    sampledPositionMs = currentPlaybackPositionMs(),
                    frameAdvanceNanos = frameAdvanceNanos,
                    playbackSpeed = latestPlaybackSpeed,
                )
            }
        }
    }

    return remember(smoothPositionMs) { { smoothPositionMs.doubleValue.roundToLong() } }
}

@Composable
internal fun LyricsView(
    document: LyricsDocument,
    positionMs: Long,
    positionUpdateElapsedRealtimeMs: Long,
    playbackIteration: Long,
    playbackSpeed: Float,
    previewPositionMs: Long?,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    contentWidth: Dp,
    lyricFontScale: Float,
    lyricFontWeight: Int,
    lyricAnimationMode: LyricAnimationMode,
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
    val documentHasTimedWords = remember(document) {
        document.lines.any { it.words.isNotEmpty() }
    }
    val listState = rememberLazyListState()
    val currentTimeProvider = rememberSmoothLyricTimeProvider(
        document = document,
        positionMs = positionMs,
        positionUpdateElapsedRealtimeMs = positionUpdateElapsedRealtimeMs,
        playbackIteration = playbackIteration,
        playbackSpeed = playbackSpeed,
        isPlaying = isPlaying,
        seekRequestKey = seekRequestKey,
        seekPositionMs = seekPositionMs,
    )
    val latestSeekRequestKey = rememberUpdatedState(seekRequestKey)
    val latestSeekPositionMs = rememberUpdatedState(seekPositionMs)
    val latestPreviewPositionMs = rememberUpdatedState(previewPositionMs)
    val lyricTimeProvider = remember(currentTimeProvider) {
        {
            lyricDisplayedPositionMs(
                previewPositionMs = latestPreviewPositionMs.value,
                seekRequestPending = latestSeekRequestKey.value != 0,
                seekPositionMs = latestSeekPositionMs.value,
                smoothPositionMs = currentTimeProvider(),
            )
        }
    }
    val density = LocalDensity.current
    val alignmentProgress = animateFloatAsState(
        targetValue = if (centerLyrics) 1f else 0f,
        animationSpec = playerTextAlignmentSpec(),
        label = "lyricsAlignment",
    )
    val viewConfiguration = LocalViewConfiguration.current
    val centerOffsetPx = with(density) { centerOffsetY.toPx() }
    val keepAliveZone = 100.dp
    val currentLineIndex by remember(document) {
        derivedStateOf { document.currentLineIndex(lyricTimeProvider()) }
    }
    val focusLineIndex by remember(document) {
        derivedStateOf { document.focusLineIndex(lyricTimeProvider()) }
    }
    var isUserBrowsingLyrics by remember(document) { mutableStateOf(false) }
    var outgoingSeekLineIndex by remember(document) { mutableIntStateOf(-1) }
    var outgoingSeekPositionMs by remember(document) { mutableLongStateOf(0L) }
    var pendingSeekCenteringTargetIndex by remember(document) { mutableIntStateOf(-1) }
    var pendingSeekCenteringDeltaPx by remember(document) { mutableStateOf<Float?>(null) }
    val isPreviewing = previewPositionMs != null
    val rowMotion = remember(document) { LyricRowMotion() }
    val translationMotion = remember(document) { LyricTranslationMotion(showLyricsTranslation) }
    var translationRevision by remember(document) { mutableIntStateOf(0) }
    var lyricTapRevision by remember(document) { mutableIntStateOf(0) }
    var retainedTravelPx by remember(document) { mutableFloatStateOf(0f) }
    var viewportHeightPx by remember(document) { mutableIntStateOf(0) }
    var viewportWidthPx by remember(document) { mutableIntStateOf(0) }
    var lastCenteringGeometry by remember(document) { mutableStateOf<List<Any>?>(null) }
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
    val centeringGeometry = listOf(
        centerOffsetY, viewportHeightPx, viewportWidthPx, contentWidth,
        lyricFontScale, lyricFontWeight,
        density.density, density.fontScale,
    )
    val visualFocusRenderIndex = focusLineIndex
    val lyricFontFamily = rememberLyricFontFamily(lyricFontWeight)
    val normalTextStyle = MiuixTheme.textStyles.title3.copy(
        fontSize = (LYRIC_PRIMARY_FONT_SIZE_SP * lyricFontScale).sp,
        lineHeight = (LYRIC_PRIMARY_LINE_HEIGHT_SP * lyricFontScale).sp,
        fontFamily = lyricFontFamily,
        fontWeight = FontWeight(lyricFontWeight.coerceIn(1, 1000)),
        fontSynthesis = FontSynthesis.None,
        textDirection = TextDirection.Content,
        textMotion = TextMotion.Animated,
    )
    val translationTextStyle = MiuixTheme.textStyles.body1.copy(
        fontSize = (LYRIC_TRANSLATION_FONT_SIZE_SP * lyricFontScale).sp,
        lineHeight = (LYRIC_TRANSLATION_LINE_HEIGHT_SP * lyricFontScale).sp,
        fontFamily = lyricFontFamily,
        fontWeight = FontWeight(lyricFontWeight.coerceIn(1, 1000)),
        fontSynthesis = FontSynthesis.None,
        textDirection = TextDirection.Content,
        textMotion = TextMotion.Animated,
    )
    val textMeasurer = rememberTextMeasurer()
    val layoutDirection = LocalLayoutDirection.current
    val fontResolver = LocalFontFamilyResolver.current
    val lineCache = remember(document, normalTextStyle, emphasisColor, density, layoutDirection, fontResolver) {
        PreparedLyricLineCache()
    }

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
    LaunchedEffect(playbackIteration) {
        isUserBrowsingLyrics = false
        outgoingSeekLineIndex = -1
        pendingSeekCenteringTargetIndex = -1
        pendingSeekCenteringDeltaPx = null
    }
    LaunchedEffect(resumeFollowRequestKey) {
        isUserBrowsingLyrics = false
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
    LaunchedEffect(document, showLyricsTranslation) {
        if (document.lines.none { it.translation != null }) return@LaunchedEffect
        translationMotion.transitionTo(showLyricsTranslation) {
            translationRevision++
        }
    }
    val centeringEvent = LyricCenteringEvent(
        focusIndex = visualFocusRenderIndex,
        requestedPositionMs = seekPositionMs,
        tapRevision = lyricTapRevision,
        browsing = isUserBrowsingLyrics,
        previewing = isPreviewing,
        geometry = centeringGeometry,
        translationRevision = translationRevision,
    )
    LaunchedEffect(document, centeringEvent) {
        if (viewportHeightPx == 0) return@LaunchedEffect
        if (visualFocusRenderIndex < 0) {
            hasPositionedInitialFocus = true
            return@LaunchedEffect
        }
        if (isUserBrowsingLyrics) {
            // Transfer the common visual displacement into native scrolling without a jump.
            val consumed = listState.dispatchRawDelta(-rowMotion.offset(lastVisualFocusRenderIndex))
            rowMotion.absorbScroll(consumed)
            retainedTravelPx = maxOf(retainedTravelPx, rowMotion.retainedTravel(LYRIC_CENTERING_BASE_STIFFNESS))
            snapshotFlow { isManualScrolling }.collectLatest { scrolling ->
                if (scrolling) {
                    rowMotion.absorbScroll(0f)
                } else {
                    // Give the gesture that enabled browsing a frame to acquire the list.
                    withFrameNanos { }
                    if (!isManualScrolling) {
                        rowMotion.settle(LYRIC_CENTERING_BASE_STIFFNESS)
                        retainedTravelPx = 0f
                    }
                }
            }
            return@LaunchedEffect
        }
        var animateCentering = false
        var centeringSpringStiffness = LYRIC_CENTERING_BASE_STIFFNESS
        var visibleRowOrder = emptyList<Int>()
        var placementCommitted = false
        try {
            scrollInCode.value = true
            listState.scroll {
                // Own native scrolling before capturing positions or awaiting retention layout.
                val layoutInfo = listState.layoutInfo
                val targetItem = layoutInfo.visibleItemsInfo.firstOrNull {
                    it.index == visualFocusRenderIndex
                }
                animateCentering = lyricSeekUsesAnimatedCentering(
                    hasPositionedInitialFocus = hasPositionedInitialFocus,
                    centerOffsetUnchanged = lastCenteringGeometry == centeringGeometry,
                    isPreviewing = isPreviewing,
                )
                val centeringLine = document.lines.getOrNull(visualFocusRenderIndex)
                val nextTimestampGapMs = centeringLine?.let { line ->
                    document.lines
                        .getOrNull(visualFocusRenderIndex + 1)
                        ?.startTimeMs
                        ?.minus(line.startTimeMs)
                }
                centeringSpringStiffness = lyricCenteringSpringStiffness(nextTimestampGapMs)
                val capturedSeekCenteringDelta = pendingSeekCenteringDeltaPx.takeIf {
                    pendingSeekCenteringTargetIndex == visualFocusRenderIndex
                }
                val measuredScrollDelta = capturedSeekCenteringDelta ?: targetItem?.let { visibleTarget ->
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
                    viewportStartOffset = 0,
                    viewportEndOffset = viewportHeightPx + with(density) { (keepAliveZone * 2).roundToPx() },
                    itemSize = targetItem?.size ?: estimatedItemSize,
                )
                val previousRenderIndex = lastVisualFocusRenderIndex
                    .takeIf { it >= 0 }
                    ?: visualFocusRenderIndex
                val currentTranslationVelocity = rowMotion.velocity(visualFocusRenderIndex)
                lastCenteringGeometry = centeringGeometry
                val previousTranslation = rowMotion.offset(visualFocusRenderIndex)
                val requestedTranslation = if (animateCentering) {
                    val translationStart = lyricProgrammaticTranslationStart(
                        currentTranslationY = previousTranslation,
                        measuredScrollDelta = measuredScrollDelta,
                        targetRenderIndex = visualFocusRenderIndex,
                        previousRenderIndex = previousRenderIndex,
                        offscreenTravelPx = offscreenCenteringTravelPx,
                    )
                    retainedTravelPx = maxOf(
                        retainedTravelPx,
                        rowMotion.retainedTravel(centeringSpringStiffness) + abs(translationStart - previousTranslation),
                        lyricRetainedTravelPx(
                            translationStartPx = translationStart,
                            velocityPxPerSecond = currentTranslationVelocity,
                            stiffness = centeringSpringStiffness,
                        ),
                    )
                    translationStart
                } else {
                    retainedTravelPx = 0f
                    0f
                }
                val reservePx = with(density) { keepAliveZone.roundToPx() } + retainedTravelPx.roundToInt()
                // snapToItem can synchronously remeasure the child with its old constraints.
                // Wait until the outer viewport and both paddings have changed together first.
                snapshotFlow { listState.layoutInfo }.first { measured ->
                    lyricRetentionIsMeasured(
                        viewportHeightPx = viewportHeightPx,
                        reservePx = reservePx,
                        measuredHeightPx = measured.viewportSize.height,
                        beforePaddingPx = measured.beforeContentPadding,
                        afterPaddingPx = measured.afterContentPadding,
                    )
                }
                val placement = LazyLayoutScrollScope(listState, this)
                // Capture and commit in one frame, after retention has finished remeasuring.
                // A pre-wait snapshot may describe an obsolete native scroll anchor.
                placementCommitted = withFrameNanos {
                    if (document.focusLineIndex(lyricTimeProvider()) != visualFocusRenderIndex) {
                        return@withFrameNanos false
                    }
                    val readyLayout = listState.layoutInfo
                    val readyTarget = readyLayout.visibleItemsInfo.firstOrNull {
                        it.index == visualFocusRenderIndex
                    }
                    val previousVisualOffsets = readyLayout.visibleItemsInfo.associate { item ->
                        item.index to item.offset + rowMotion.offset(item.index)
                    }
                    val viewportStart = readyLayout.viewportStartOffset + reservePx
                    visibleRowOrder = lyricVisibleRowOrder(
                        rows = readyLayout.visibleItemsInfo.map { item ->
                            LyricVisualRow(item.index, previousVisualOffsets.getValue(item.index), item.size)
                        },
                        viewportStart = viewportStart.toFloat(),
                        viewportEnd = viewportStart + viewportHeightPx.toFloat(),
                    )
                    val placementTranslation = rowMotion.offset(visualFocusRenderIndex)
                    val placementVelocity = rowMotion.velocity(visualFocusRenderIndex)
                    placement.snapToItem(
                        index = visualFocusRenderIndex,
                        offset = lyricTargetScrollOffset(
                            itemSize = readyTarget?.size ?: targetItem?.size ?: estimatedItemSize,
                            viewportStartOffset = readyLayout.viewportStartOffset,
                            viewportEndOffset = readyLayout.viewportEndOffset,
                            centerOffsetPx = centerOffsetPx,
                        ),
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
                        // A delta may defer placement; snap with the measured size before compensation.
                        if (abs(correction) > 0.5f) {
                            placement.snapToItem(
                                index = visualFocusRenderIndex,
                                offset = lyricTargetScrollOffset(
                                    itemSize = centeredItem.size,
                                    viewportStartOffset = centeredLayoutInfo.viewportStartOffset,
                                    viewportEndOffset = centeredLayoutInfo.viewportEndOffset,
                                    centerOffsetPx = centerOffsetPx,
                                ),
                            )
                        }
                    }
                    val placedTargetOffset = listState.layoutInfo.visibleItemsInfo.firstOrNull {
                        it.index == visualFocusRenderIndex
                    }?.offset
                    rowMotion.place(
                        previousVisualOffsets = previousVisualOffsets,
                        placedOffsets = listState.layoutInfo.visibleItemsInfo.associate { it.index to it.offset },
                        fallbackTranslation = lyricActualPlacementTranslation(
                            previousTranslationPx = placementTranslation,
                            previousTargetOffsetPx = readyTarget?.offset,
                            placedTargetOffsetPx = placedTargetOffset,
                            offscreenTranslationPx = requestedTranslation,
                        ),
                        fallbackVelocity = placementVelocity,
                        animate = animateCentering,
                    )
                    true
                }
                if (placementCommitted) hasPositionedInitialFocus = true
            }
        } finally {
            scrollInCode.value = false
        }
        if (!placementCommitted) return@LaunchedEffect
        if (pendingSeekCenteringTargetIndex == visualFocusRenderIndex) {
            pendingSeekCenteringTargetIndex = -1
            pendingSeekCenteringDeltaPx = null
        }
        lastVisualFocusRenderIndex = visualFocusRenderIndex
        if (animateCentering) {
            rowMotion.settle(
                stiffness = centeringSpringStiffness,
                visibleOrder = visibleRowOrder,
                cascade = true,
            )
            retainedTravelPx = 0f
        }
    }

    BoxWithConstraints(modifier = modifier.onSizeChanged {
        viewportHeightPx = it.height
        viewportWidthPx = it.width
    }) {
        val animationBleed = LYRIC_ANIMATION_BLEED_DP.dp
        val horizontalPadding = (
            (maxWidth - contentWidth) / 2f - animationBleed
            ).coerceAtLeast(0.dp)
        val textWidthPx = with(density) {
            (maxWidth.roundToPx() - horizontalPadding.roundToPx() * 2 -
                animationBleed.roundToPx() * 2).coerceAtLeast(1).toFloat()
        }
        LaunchedEffect(lineCache, focusLineIndex, textWidthPx, lyricAnimationMode) {
            withContext(Dispatchers.Default) {
                val measurer = TextMeasurer(fontResolver, density, layoutDirection)
                val indices = listOf(focusLineIndex + 1, focusLineIndex + 2, focusLineIndex + 3, focusLineIndex - 1)
                for (index in indices) {
                    if (!isActive) break
                    val line = document.lines.getOrNull(index) ?: continue
                    if (!lyricAnimationMode.usesWordAnimation(line.words.isNotEmpty(), documentHasTimedWords)) continue
                    lineCache.prepare(line, textWidthPx,
                        measurer, normalTextStyle, emphasisColor, density, layoutDirection)
                }
            }
        }
        val viewportHeight = maxHeight
        // Read retention during measurement so padding and viewport grow together before centering.
        val listContentPadding = remember(document, horizontalPadding, viewportHeight, density) {
            object : PaddingValues {
                override fun calculateLeftPadding(layoutDirection: LayoutDirection) = horizontalPadding
                override fun calculateRightPadding(layoutDirection: LayoutDirection) = horizontalPadding
                override fun calculateTopPadding() = with(density) {
                    (viewportHeight.roundToPx() / 2 + keepAliveZone.roundToPx() +
                        retainedTravelPx.roundToInt()).toDp()
                }
                override fun calculateBottomPadding() = calculateTopPadding()
            }
        }
        val glyphOutset = with(density) {
            lyricGlyphOutsetPx(normalTextStyle.lineHeight.toPx()).toDp()
        }
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
                    .drawWithCache {
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
                        onDrawWithContent {
                            drawContent()
                            drawRect(
                                brush = edgeMask,
                                blendMode = BlendMode.DstIn,
                            )
                        }
                    },
            ) {
                LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                .graphicsLayer {
                    alpha = if (hasPositionedInitialFocus) 1f else 0f
                }
                .layout { measurable, constraints ->
                        val keepAlivePx = keepAliveZone.roundToPx() + retainedTravelPx.roundToInt()
                        val extraHeightPx = keepAlivePx * 2
                        val placeable = measurable.measure(
                            constraints.copy(
                                maxHeight = constraints.maxHeight + extraHeightPx,
                            ),
                        )
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeable.place(0, -keepAlivePx)
                        }
                    },
                contentPadding = listContentPadding,
            ) {
                itemsIndexed(
                    items = document.lines,
                    key = { index, line ->
                        "line-${line.startTimeMs}-${line.endTimeMs}-$index"
                    },
                ) { index, line ->
                    val distanceFromFocus = if (focusLineIndex >= 0) {
                        kotlin.math.abs(index - focusLineIndex)
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
                        animationSpec = tween(LYRIC_BLUR_ANIMATION_DURATION_MS),
                        label = "lyricBlurRadius",
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                translationY = rowMotion.offset(index)
                            },
                    ) {
                        val isCurrentLine = index == currentLineIndex
                        val preservesOutgoingSeekProgress = index == outgoingSeekLineIndex
                        val usesWordProgress = lyricAnimationMode.usesWordAnimation(
                            lineHasTimedWords = line.words.isNotEmpty(),
                            documentHasTimedWords = documentHasTimedWords,
                        )
                        val prepared = if (usesWordProgress) remember(lineCache, line, textWidthPx) {
                            lineCache.prepare(line, textWidthPx, textMeasurer, normalTextStyle,
                                emphasisColor, density, layoutDirection)
                        } else null
                        val visualOutsets = remember(prepared, textWidthPx, density) {
                            prepared?.visualOutsets(textWidthPx, density) ?: LayerOutsets()
                        }
                        val lineTimeProvider = {
                            lyricLineRenderPositionMs(
                                lineIndex = index,
                                displayedPositionMs = lyricTimeProvider(),
                                outgoingSeekLineIndex = outgoingSeekLineIndex,
                                outgoingSeekPositionMs = outgoingSeekPositionMs,
                            )
                        }
                        LyricsLineItem(
                            isFocused = isCurrentLine,
                            usesWordProgress = usesWordProgress,
                            alignmentProgress = alignmentProgress,
                            blurRadius = blurRadius,
                            glyphOutset = glyphOutset,
                            visualOutsets = visualOutsets,
                            onAlphaSettled = { settledAlpha ->
                                if (
                                    lyricOutgoingSeekCanClear(
                                        lineIndex = index,
                                        outgoingSeekLineIndex = outgoingSeekLineIndex,
                                        currentLineIndex = currentLineIndex,
                                        settledAlpha = settledAlpha,
                                    )
                                ) {
                                    outgoingSeekLineIndex = -1
                                }
                            },
                            onClick = {
                                val clickedItem = listState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { it.index == index }
                                lyricTapRevision += 1
                                pendingSeekCenteringTargetIndex = index
                                pendingSeekCenteringDeltaPx = clickedItem?.let { visibleItem ->
                                    val layoutInfo = listState.layoutInfo
                                    lyricCenterScrollDelta(
                                        itemOffset = visibleItem.offset,
                                        itemSize = visibleItem.size,
                                        viewportStartOffset = layoutInfo.viewportStartOffset,
                                        viewportEndOffset = layoutInfo.viewportEndOffset,
                                        centerOffsetPx = centerOffsetPx,
                                    )
                                }
                                if (currentLineIndex >= 0 && currentLineIndex != index) {
                                    outgoingSeekLineIndex = currentLineIndex
                                    outgoingSeekPositionMs = lyricTimeProvider()
                                }
                                isUserBrowsingLyrics = false
                                onSeek(line.startTimeMs)
                            },
                        ) { wordProgressActiveAlpha, translationAlpha ->
                            if (prepared != null) {
                                TimedLyricLine(
                                    line = line,
                                    prepared = prepared,
                                    usePlaybackProgress =
                                        isCurrentLine || preservesOutgoingSeekProgress,
                                    translationAlpha = translationAlpha,
                                    activeAlpha = wordProgressActiveAlpha,
                                    currentTimeProvider = lineTimeProvider,
                                    normalTextStyle = normalTextStyle,
                                    translationTextStyle = translationTextStyle,
                                    emphasisColor = emphasisColor,
                                    alignmentProgress = alignmentProgress,
                                    translationMotion = translationMotion,
                                )
                            } else {
                                SyncedLyricLine(
                                    line = line,
                                    translationAlpha = translationAlpha,
                                    normalTextStyle = normalTextStyle,
                                    translationTextStyle = translationTextStyle,
                                    emphasisColor = emphasisColor,
                                    alignmentProgress = alignmentProgress,
                                    translationMotion = translationMotion,
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
    usesWordProgress: Boolean,
    alignmentProgress: State<Float>,
    blurRadius: Float,
    glyphOutset: Dp,
    visualOutsets: LayerOutsets,
    onAlphaSettled: (Float) -> Unit,
    onClick: () -> Unit,
    content: @Composable (
        wordProgressActiveAlpha: Float,
        translationAlpha: Float,
    ) -> Unit,
) {
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.98f,
        animationSpec = if (isFocused) {
            tween(
                durationMillis = LYRIC_FOCUS_SCALE_IN_DURATION_MS,
                easing = LinearOutSlowInEasing,
            )
        } else {
            tween(
                durationMillis = LYRIC_FOCUS_SCALE_OUT_DURATION_MS,
                easing = EaseInOut,
            )
        },
        label = "lyricFocusScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else LYRIC_INACTIVE_TEXT_ALPHA,
        animationSpec = tween(
            durationMillis = LYRIC_FOCUS_ALPHA_ANIMATION_DURATION_MS,
            easing = LinearOutSlowInEasing,
        ),
        label = "lyricFocusAlpha",
        finishedListener = { settledAlpha ->
            onAlphaSettled(settledAlpha)
        },
    )
    val layerAlpha = lyricLineLayerAlpha(
        lineAlpha = alpha,
        usesWordProgress = usesWordProgress,
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = layerAlpha
                transformOrigin = TransformOrigin(
                    pivotFractionX = alignmentProgress.value * 0.5f,
                    pivotFractionY = 1f,
                )
                compositingStrategy = CompositingStrategy.Offscreen
                val blurOutset = with(density) { (blurRadius * 3f).toDp() }
                outsets = LayerOutsets(
                    left = maxOf(glyphOutset, visualOutsets.left) + blurOutset,
                    right = maxOf(glyphOutset, visualOutsets.right) + blurOutset,
                    top = maxOf(glyphOutset, visualOutsets.top) + blurOutset,
                    bottom = maxOf(glyphOutset, visualOutsets.bottom) + blurOutset,
                )
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LYRIC_ANIMATION_BLEED_DP.dp),
        ) {
            content(
                lyricWordProgressActiveAlpha(
                    lineAlpha = alpha,
                    usesWordProgress = usesWordProgress,
                ),
                lyricTranslationAlpha(layerAlpha),
            )
        }
    }
}

@Composable
private fun TranslationText(
    text: String,
    style: TextStyle,
    color: Color,
    alignmentProgress: State<Float>,
    motion: LyricTranslationMotion,
    lyricFontScale: Float,
    modifier: Modifier = Modifier,
) {
    var textLayout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = text,
        style = style,
        color = color,
        textAlign = TextAlign.Start,
        onTextLayout = { textLayout = it },
        modifier = modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minHeight = 0))
                val topPadding = (LYRIC_TRANSLATION_GAP_DP * lyricFontScale).dp.roundToPx()
                val height = ((placeable.height + topPadding) *
                    motion.expansion.value.coerceIn(0f, 1f)).roundToInt()
                layout(placeable.width, constraints.constrainHeight(height)) {
                    placeable.place(0, topPadding)
                }
            }
            .animatedPlayerTextAlignment(textLayout, alignmentProgress)
            .graphicsLayer { alpha = motion.opacity.value }
            .then(if (motion.opacity.value == 0f) Modifier.clearAndSetSemantics { } else Modifier),
    )
}

@Composable
private fun TimedLyricLine(
    line: LyricLine,
    prepared: PreparedLyricLine,
    usePlaybackProgress: Boolean,
    translationAlpha: Float,
    activeAlpha: Float,
    currentTimeProvider: () -> Long,
    normalTextStyle: TextStyle,
    translationTextStyle: TextStyle,
    emphasisColor: Color,
    alignmentProgress: State<Float>,
    translationMotion: LyricTranslationMotion,
) {
    val verticalPadding = lyricTranslationPaddingDp(
        hasTranslation = line.translation != null,
        expansion = translationMotion.expansion.value,
        lyricFontScale = normalTextStyle.fontSize.value / LYRIC_PRIMARY_FONT_SIZE_SP,
    ).dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = verticalPadding),
        horizontalAlignment = Alignment.Start,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val density = LocalDensity.current
            val rowRenderData = prepared.rows
            val effectScale = with(density) { lyricEffectGeometryScale(normalTextStyle.fontSize.toPx()) }
            val fadeWidth = with(density) { lyricForegroundFadeWidthPx(normalTextStyle.fontSize.toPx()) }
            val totalHeight = prepared.heightPx
            var motionDurationScale by remember { mutableStateOf<MotionDurationScale?>(null) }
            LaunchedEffect(Unit) { motionDurationScale = coroutineContext[MotionDurationScale] }

            val latestTimeProvider = rememberUpdatedState(currentTimeProvider)
            val latestActiveAlpha = rememberUpdatedState(activeAlpha)
            val staticCompleted by remember(line, latestTimeProvider) {
                derivedStateOf { latestTimeProvider.value() >= line.endTimeMs }
            }
            val drawTimedLyrics: CacheDrawScope.() -> DrawResult = remember(
                rowRenderData, emphasisColor, usePlaybackProgress, staticCompleted,
            ) {
                {
                    onDrawBehind {
                        drawLyricsLine(
                            rows = rowRenderData,
                            canvasWidth = size.width,
                            alignmentProgress = alignmentProgress,
                            positionMs = if (usePlaybackProgress) {
                                latestTimeProvider.value()
                            } else if (staticCompleted) {
                                line.endTimeMs
                            } else {
                                line.startTimeMs
                            },
                            color = emphasisColor,
                            activeAlpha = latestActiveAlpha.value,
                            effectScale = effectScale,
                            fadeWidthPx = fadeWidth,
                            glyphMotionEnabled = motionDurationScale?.scaleFactor != 0f,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(
                        width = maxWidth,
                        height = with(density) {
                            totalHeight.toDp()
                        },
                    )
                    .drawWithCache(drawTimedLyrics),
            )
        }
        line.translation?.let { translation ->
            TranslationText(
                text = translation,
                style = translationTextStyle,
                color = emphasisColor.copy(alpha = translationAlpha),
                alignmentProgress = alignmentProgress,
                motion = translationMotion,
                lyricFontScale = normalTextStyle.fontSize.value / LYRIC_PRIMARY_FONT_SIZE_SP,
            )
        }
    }
}

@Composable
private fun SyncedLyricLine(
    line: LyricLine,
    translationAlpha: Float,
    normalTextStyle: TextStyle,
    translationTextStyle: TextStyle,
    emphasisColor: Color,
    alignmentProgress: State<Float>,
    translationMotion: LyricTranslationMotion,
) {
    var textLayout by remember(line) { mutableStateOf<TextLayoutResult?>(null) }
    val verticalPadding = lyricTranslationPaddingDp(
        hasTranslation = line.translation != null,
        expansion = translationMotion.expansion.value,
        lyricFontScale = normalTextStyle.fontSize.value / LYRIC_PRIMARY_FONT_SIZE_SP,
    ).dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = verticalPadding),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = line.displayText,
            modifier = Modifier.fillMaxWidth()
                .animatedPlayerTextAlignment(textLayout, alignmentProgress),
            style = normalTextStyle,
            color = emphasisColor,
            textAlign = TextAlign.Start,
            onTextLayout = { textLayout = it },
        )
        line.translation?.let { translation ->
            TranslationText(
                text = translation,
                style = translationTextStyle,
                color = emphasisColor.copy(alpha = translationAlpha),
                alignmentProgress = alignmentProgress,
                motion = translationMotion,
                lyricFontScale = normalTextStyle.fontSize.value / LYRIC_PRIMARY_FONT_SIZE_SP,
            )
        }
    }
}

private data class Syllable(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val content: String,
)

private data class LyricGlyph(
    val range: IntRange,
    val bounds: Rect,
    val sourceIndex: Int,
    val sourceCount: Int,
    val visible: Boolean,
    val motionBounds: Rect = bounds,
    val glow: LyricGlyphGlow? = null,
)

private data class SyllableLayout(
    val syllable: Syllable,
    val textLayoutResult: TextLayoutResult,
    val effects: LyricWordEffects,
    val animationSource: Syllable = syllable,
    val sourceOffset: Int = 0,
    val width: Float = textLayoutResult.size.width.toFloat(),
    val lineEndWidth: Float = width,
    val position: Offset = Offset.Zero,
    val glyphs: List<LyricGlyph>,
    val raster: LyricTextRaster? = null,
    val firstBaseline: Float = textLayoutResult.firstBaseline,
) {
    val glyphStartX = glyphs.minOfOrNull { it.motionBounds.left } ?: 0f
    val glyphEndX = glyphs.maxOfOrNull { it.motionBounds.right } ?: width
}

private data class WrappedLine(val syllables: List<SyllableLayout>, val totalWidth: Float)

private class RowRenderData(
    val layouts: List<SyllableLayout>,
    val minX: Float,
    val maxX: Float,
    val width: Float,
    val firstStartTimeMs: Long,
    val lastEndTimeMs: Long,
    val layerBounds: Rect,
) {
    val cursor = LyricTimingCursor(
        layouts.map { it.syllable.startTimeMs }.toLongArray(),
        layouts.map { it.syllable.endTimeMs }.toLongArray(),
    )
    private var maskAlpha = Float.NaN
    private var maskWidth = Float.NaN
    private var mask: Brush? = null

    fun gradient(activeAlpha: Float, fadeWidth: Float): Brush {
        if (activeAlpha != maskAlpha || fadeWidth != maskWidth) {
            maskAlpha = activeAlpha
            maskWidth = fadeWidth
            mask = Brush.horizontalGradient(
                listOf(Color.White.copy(alpha = activeAlpha),
                    Color.White.copy(alpha = LYRIC_INACTIVE_TEXT_ALPHA)),
                startX = -fadeWidth / 2f, endX = fadeWidth / 2f,
            )
        }
        return requireNotNull(mask)
    }
}

private data class PreparedLyricLine(val rows: List<RowRenderData>, val heightPx: Int, val rasterBytes: Long)

/** Drawing coverage is prepared data; reading descendant alignment lines can place lazy items during measurement. */
private fun PreparedLyricLine.visualOutsets(
    widthPx: Float,
    density: androidx.compose.ui.unit.Density,
): LayerOutsets {
    val bounds = rows.fold(Rect(0f, 0f, widthPx, heightPx.toFloat())) { bounds, row ->
        val centerOffset = playerTextAlignmentOffset(row.minX, row.maxX, widthPx, 1f)
        bounds.lyricUnion(row.layerBounds)
            .lyricUnion(row.layerBounds.translate(Offset(centerOffset, 0f)))
    }
    return with(density) {
        LayerOutsets(
            left = (-bounds.left).coerceAtLeast(0f).toDp(),
            right = (bounds.right - widthPx).coerceAtLeast(0f).toDp(),
            top = (-bounds.top).coerceAtLeast(0f).toDp(),
            bottom = (bounds.bottom - heightPx).coerceAtLeast(0f).toDp(),
        )
    }
}

/** Retain preparation across lazy-item disposal, bounded to a small part of the document. */
private class PreparedLyricLineCache {
    private data class Key(val line: LyricLine, val width: Float)
    private val entries = LinkedHashMap<Key, PreparedLyricLine>(32, 0.75f, true)
    private var rasterBytes = 0L

    fun prepare(
        line: LyricLine, width: Float, measurer: TextMeasurer, style: TextStyle,
        color: Color, density: androidx.compose.ui.unit.Density, direction: LayoutDirection,
    ): PreparedLyricLine {
        val key = Key(line, width)
        synchronized(entries) { entries[key]?.let { return it } }
        val syllables = line.animationWords().map {
            Syllable(it.startTimeMs, it.endTimeMs, it.text + if (it.hasTrailingSpace) " " else "")
        }
        val spaceWidth = measurer.measure(" ", style).size.width.toFloat()
        val measured = measureSyllables(syllables, measurer, style, spaceWidth, width,
            hasNativeWordTiming = line.words.isNotEmpty())
        val wrapped = calculateWrappedLines(measured, width, measurer, style)
        val height = measurer.measure("M", style).size.height.toFloat()
        val effectScale = with(density) { lyricEffectGeometryScale(style.fontSize.toPx()) }
        var remainingBytes = 4L * 1024 * 1024
        val positioned = calculateStaticLineLayout(wrapped, height).map { row ->
            row.map { layout ->
                val raster = prepareLyricTextRaster(layout.textLayoutResult, color, density,
                    direction, remainingBytes)
                remainingBytes -= raster?.byteCount ?: 0L
                val duration = layout.animationSource.endTimeMs - layout.animationSource.startTimeMs
                val glyphs = layout.glyphs.map { glyph ->
                    val glow = if (raster != null && layout.effects.glow && glyph.visible) {
                        prepareLyricGlyphGlow(raster, glyph.bounds.left, glyph.bounds.right,
                            wordMotion(0.5f, duration).glowRadius * effectScale, color, remainingBytes)
                    } else null
                    remainingBytes -= glow?.byteCount ?: 0L
                    glyph.copy(glow = glow)
                }
                layout.copy(raster = raster, glyphs = glyphs)
            }
        }
        val preparedHeight = positioned.flatten().maxOfOrNull {
            it.position.y + it.textLayoutResult.size.height
        } ?: 0f
        val prepared = PreparedLyricLine(calculateRowRenderData(positioned, effectScale),
            ceil(preparedHeight).toInt(), 4L * 1024 * 1024 - remainingBytes)
        synchronized(entries) {
            entries[key]?.let { return it }
            entries[key] = prepared
            rasterBytes += prepared.rasterBytes
            while (entries.size > 32 || rasterBytes > 12L * 1024 * 1024) {
                val iterator = entries.entries.iterator()
                rasterBytes -= iterator.next().value.rasterBytes
                iterator.remove()
            }
        }
        return prepared
    }
}

private fun measureSyllables(
    syllables: List<Syllable>,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    spaceWidth: Float,
    availableWidthPx: Float,
    hasNativeWordTiming: Boolean,
): List<SyllableLayout> = syllables.flatMap { source ->
    val sourceRanges = lyricGraphemeRanges(source.content)
    val duration = source.endTimeMs - source.startTimeMs
    val effects = lyricWordEffects(source.content, duration, hasNativeWordTiming)
    val chunks = lyricTextChunks(source.content, availableWidthPx) {
        textMeasurer.measure(it, style).size.width.toFloat()
    }
    chunks.map { range ->
        val syllable = Syllable(
            startTimeMs = lyricChunkTimeMs(source.startTimeMs, source.endTimeMs,
                source.content.length, range.first),
            endTimeMs = lyricChunkTimeMs(source.startTimeMs, source.endTimeMs,
                source.content.length, range.last + 1),
            content = source.content.substring(range),
        )
        val layoutResult = textMeasurer.measure(syllable.content, style)
        var layoutWidth = layoutResult.size.width.toFloat()
        if (syllable.content.endsWith(" ")) {
            val trimmedContent = syllable.content.trimEnd()
            val trimmedWidth = textMeasurer.measure(trimmedContent, style).size.width.toFloat()
            val spaceCount = syllable.content.length - trimmedContent.length
            layoutWidth = maxOf(layoutWidth, trimmedWidth + spaceWidth * spaceCount)
        }
        val glyphs = shapedLyricGlyphs(layoutResult, syllable.content, range.first,
            sourceRanges, source.content.needsJoinedText())
        SyllableLayout(
            syllable = syllable,
            textLayoutResult = layoutResult,
            effects = effects,
            animationSource = source,
            sourceOffset = range.first,
            width = layoutWidth,
            lineEndWidth = textMeasurer.measure(syllable.content.trimEnd(), style).size.width.toFloat(),
            glyphs = glyphs,
        )
    }
}

private fun shapedLyricGlyphs(
    layout: TextLayoutResult, content: String, sourceOffset: Int,
    sourceRanges: List<IntRange>, joined: Boolean,
): List<LyricGlyph> {
    if (content.isEmpty()) return emptyList()
    val ranges = if (joined) listOf(content.indices) else lyricGraphemeRanges(content)
    val padding = lyricGlyphOutsetPx(layout.size.height.toFloat())
    val result = mutableListOf<LyricGlyph>()
    for (range in ranges) {
        val boxes = range.map(layout::getBoundingBox)
        val bounds = Rect(boxes.minOf { it.left }, -padding,
            boxes.maxOf { it.right }, layout.size.height + padding)
        val sourceIndex = if (joined) 0 else sourceRanges.indexOfFirst { sourceOffset + range.first in it }.coerceAtLeast(0)
        val glyph = LyricGlyph(range, bounds, sourceIndex, if (joined) 1 else sourceRanges.size,
            visible = range.any { !content[it].isWhitespace() && !content[it].isPunctuation() })
        val previous = result.lastOrNull()
        // Shared ligature bounds must move together instead of slicing a shaped glyph in half.
        if (previous != null && (bounds.width <= 0f || bounds.left < previous.bounds.right - 0.5f)) {
            result[result.lastIndex] = previous.copy(range = previous.range.first..range.last,
                visible = previous.visible || glyph.visible,
                bounds = Rect(minOf(previous.bounds.left, bounds.left), -padding,
                    maxOf(previous.bounds.right, bounds.right), layout.size.height + padding),
                motionBounds = Rect(minOf(previous.motionBounds.left, bounds.left), 0f,
                    maxOf(previous.motionBounds.right, bounds.right), layout.size.height.toFloat()))
        } else result += glyph
    }
    return result.mapIndexed { index, glyph ->
        glyph.copy(bounds = Rect(
            if (index == 0) -padding else glyph.bounds.left,
            -padding,
            if (index == result.lastIndex) layout.size.width + padding else result[index + 1].bounds.left,
            layout.size.height + padding,
        ))
    }
}

internal fun shouldUseWordAnimation(content: String, durationMs: Long): Boolean =
    lyricWordEffects(content, durationMs).scale

internal const val LYRIC_FLOAT_MAX_OFFSET_PX = 4f

private fun String.needsJoinedText(): Boolean = lyricUsesJoinedGlyphs(this)

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

private fun calculateWrappedLines(
    syllableLayouts: List<SyllableLayout>,
    availableWidthPx: Float,
    textMeasurer: TextMeasurer,
    style: TextStyle,
): List<WrappedLine> = lyricWrapRanges(
    contents = syllableLayouts.map { it.syllable.content },
    widths = syllableLayouts.map { it.width },
    availableWidthPx = availableWidthPx,
    lineEndWidths = syllableLayouts.map { it.lineEndWidth },
).map { range ->
    trimLineTrailingSpaces(syllableLayouts.subList(range.first, range.last + 1), textMeasurer, style)
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
                glyphs = shapedLyricGlyphs(trimmedResult, trimmedContent, lastLayout.sourceOffset,
                    lyricGraphemeRanges(lastLayout.animationSource.content),
                    lastLayout.animationSource.content.needsJoinedText()),
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
    lineHeight: Float,
): List<List<SyllableLayout>> {
    var currentY = 0f
    return wrappedLines.map { wrappedLine ->
        val maxBaseline = wrappedLine.syllables.maxOfOrNull { it.firstBaseline } ?: 0f
        var currentX = 0f
        val row = wrappedLine.syllables.map { initialLayout ->
            val positioned = initialLayout.copy(
                position = Offset(
                    x = currentX,
                    y = currentY + maxBaseline - initialLayout.firstBaseline,
                ),
            )
            currentX += initialLayout.width
            positioned
        }
        currentY += maxOf(lineHeight, row.maxOfOrNull {
            maxBaseline + it.textLayoutResult.size.height - it.firstBaseline
        } ?: lineHeight)
        row
    }
}

private fun calculateRowRenderData(
    lineLayouts: List<List<SyllableLayout>>,
    effectScale: Float,
): List<RowRenderData> = lineLayouts.mapNotNull { layouts ->
    if (layouts.isEmpty()) return@mapNotNull null
    val minX = layouts.minOf { it.position.x }
    val maxX = layouts.maxOf { it.position.x + it.width }
    val width = maxX - minX
    val minY = layouts.minOf { it.position.y }
    val height = layouts.maxOf { it.textLayoutResult.size.height }.toFloat()
    val visualBounds = layouts.fold(Rect(minX, minY, maxX, minY + height)) { rowBounds, layout ->
        layout.glyphs.fold(rowBounds) { bounds, glyph ->
            val rasterPadding = layout.raster?.padding?.toFloat()
            val ink = if (rasterPadding != null) Rect(
                maxOf(glyph.bounds.left, -rasterPadding), -rasterPadding,
                minOf(glyph.bounds.right, layout.textLayoutResult.size.width + rasterPadding),
                layout.textLayoutResult.size.height + rasterPadding,
            ) else glyph.bounds
            val coverage = glyph.glow?.let { ink.lyricUnion(it.bounds) } ?: ink
            val maxScale = if (layout.effects.scale && glyph.visible) wordMotion(0.5f,
                layout.animationSource.endTimeMs - layout.animationSource.startTimeMs).scale else 1f
            bounds.lyricUnion(lyricGlyphVisualBounds(coverage,
                lyricCharacterPivot(Offset(glyph.motionBounds.left, 0f), glyph.motionBounds.width,
                    layout.textLayoutResult.size.height.toFloat()),
                maxScale, LYRIC_FLOAT_MAX_OFFSET_PX * effectScale).translate(layout.position))
        }
    }
    RowRenderData(
        layouts = layouts,
        minX = minX,
        maxX = maxX,
        width = width,
        firstStartTimeMs = layouts.minOf { it.animationSource.startTimeMs },
        lastEndTimeMs = layouts.last().syllable.endTimeMs,
        layerBounds = visualBounds,
    )
}

private fun DrawScope.drawLyricsLine(
    rows: List<RowRenderData>,
    canvasWidth: Float,
    alignmentProgress: State<Float>,
    positionMs: Long,
    color: Color,
    activeAlpha: Float,
    effectScale: Float,
    fadeWidthPx: Float,
    glyphMotionEnabled: Boolean,
) {
    val motionStrength = lyricLineMotionStrength(activeAlpha)
    rows.forEach { row ->
        val width = minOf(fadeWidthPx, row.width).coerceAtLeast(1f)
        val fade = lyricFadeBounds(row.minX, row.maxX, linePixelPosition(row, positionMs), width)
        val offset = playerTextAlignmentOffset(row.minX, row.maxX, canvasWidth, alignmentProgress.value)
        val uniformAlpha = when {
            positionMs <= row.firstStartTimeMs -> LYRIC_INACTIVE_TEXT_ALPHA
            positionMs >= row.lastEndTimeMs -> activeAlpha
            activeAlpha == LYRIC_INACTIVE_TEXT_ALPHA -> LYRIC_INACTIVE_TEXT_ALPHA
            else -> null
        }
        withTransform({ translate(left = offset) }) {
            if (uniformAlpha != null) {
                drawRowText(row.layouts, color, positionMs, motionStrength, effectScale, uniformAlpha,
                    fade, glyphMotionEnabled)
            } else {
                drawIntoCanvas { canvas ->
                    canvas.saveLayer(row.layerBounds, LyricLayerPaint)
                    drawRowText(row.layouts, color, positionMs, motionStrength, effectScale, 1f,
                        fade, glyphMotionEnabled)
                    val center = (fade.startX + fade.endX) / 2f
                    val clippedWidth = (fade.endX - fade.startX).coerceAtLeast(0.001f)
                    translate(left = center) {
                        drawRect(
                            brush = row.gradient(activeAlpha, clippedWidth),
                            topLeft = row.layerBounds.topLeft - Offset(center, 0f),
                            size = row.layerBounds.size,
                            blendMode = BlendMode.DstIn,
                        )
                    }
                    canvas.restore()
                }
            }
        }
    }
}

internal fun DrawScope.drawLyricForeground(
    textLayoutResult: TextLayoutResult,
    color: Color,
    topLeft: Offset = Offset.Zero,
) {
    drawText(
        textLayoutResult = textLayoutResult,
        color = color,
        topLeft = topLeft,
        shadow = Shadow.None,
    )
}

internal fun lyricLineMotionStrength(activeAlpha: Float): Float =
    ((activeAlpha - LYRIC_INACTIVE_TEXT_ALPHA) / (1f - LYRIC_INACTIVE_TEXT_ALPHA)).coerceIn(0f, 1f)

internal fun WordMotion.withLineStrength(strength: Float): WordMotion {
    val amount = strength.coerceIn(0f, 1f)
    return copy(
        scale = 1f + (scale - 1f) * amount,
        offsetYPx = offsetYPx * amount,
        glowAlpha = glowAlpha * amount,
    )
}

private fun linePixelPosition(row: RowRenderData, positionMs: Long): Float {
    val index = row.cursor.indexAt(positionMs)
    val layout = row.layouts.getOrNull(index)
    return if (layout != null) {
        layout.position.x + layout.width * lyricIntervalProgress(positionMs,
            layout.syllable.startTimeMs, layout.syllable.endTimeMs)
    } else row.maxX
}

private fun DrawScope.drawRowText(
    layouts: List<SyllableLayout>,
    color: Color,
    positionMs: Long,
    motionStrength: Float,
    effectScale: Float,
    alpha: Float,
    fade: LyricFadeBounds,
    glyphMotionEnabled: Boolean,
) {
    layouts.forEachIndexed { index, layout ->
        val source = layout.animationSource
        val started = positionMs > source.startTimeMs
        val completed = positionMs >= source.endTimeMs
        val effectsActive = (layout.effects.scale || layout.effects.glow) && started && !completed
        val untouched = fade.endX <= layout.position.x + layout.glyphStartX
        val revealed = fade.startX >= layout.position.x + layout.glyphEndX
        if ((!effectsActive && (untouched || revealed)) || motionStrength == 0f) {
            val offset = if (revealed && glyphMotionEnabled)
                -LYRIC_FLOAT_MAX_OFFSET_PX * effectScale * motionStrength else 0f
            val padding = lyricGlyphOutsetPx(layout.textLayoutResult.size.height.toFloat())
            drawLyricRasterOrText(layout.textLayoutResult, layout.raster, color,
                layout.position + Offset(0f, offset),
                Rect(-padding, -padding, layout.textLayoutResult.size.width + padding,
                    layout.textLayoutResult.size.height + padding), alpha)
        } else {
            val punctuationDriver = if (layout.syllable.content.trim().all(Char::isPunctuation)) {
                layouts.subList(0, index).lastOrNull {
                    it.syllable.content.trim().any { char -> !char.isPunctuation() }
                }?.animationSource ?: source
            } else source
            for (glyph in layout.glyphs) {
                val raw = if (layout.effects.glow || layout.effects.scale) {
                    characterMotion(positionMs, punctuationDriver.startTimeMs, punctuationDriver.endTimeMs,
                        glyph.sourceIndex, glyph.sourceCount)
                } else WordMotion(1f, 0f, 0f)
                val glyphScale = if (layout.effects.scale && glyph.visible && glyphMotionEnabled)
                    raw.scale else 1f
                val pivotX = layout.position.x + glyph.motionBounds.center.x
                val revealProgress = lyricGlyphRevealProgress(
                    pivotX + (layout.position.x + glyph.motionBounds.left - pivotX) * glyphScale,
                    pivotX + (layout.position.x + glyph.motionBounds.right - pivotX) * glyphScale, fade)
                val motion = raw.copy(
                    scale = glyphScale,
                    offsetYPx = if (glyphMotionEnabled) lyricRevealFloatOffset(revealProgress) * effectScale else 0f,
                    glowAlpha = if (layout.effects.glow && glyph.visible) raw.glowAlpha else 0f,
                ).withLineStrength(motionStrength)
                val position = layout.position + Offset(0f, motion.offsetYPx)
                withTransform({
                    scale(motion.scale, motion.scale, lyricCharacterPivot(
                        position + Offset(glyph.motionBounds.left, 0f), glyph.motionBounds.width,
                        layout.textLayoutResult.size.height.toFloat()))
                }) {
                    glyph.glow?.let { glow ->
                        if (motion.glowAlpha > 0.001f) glow.draw(this, position, motion.glowAlpha * alpha)
                    }
                    drawLyricRasterOrText(layout.textLayoutResult, layout.raster, color,
                        position, glyph.bounds, alpha)
                }
            }
        }
    }
}

internal data class WordMotion(
    val scale: Float,
    val offsetYPx: Float,
    val glowRadius: Float,
    val glowAlpha: Float = 0f,
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
    val windowFraction = if (characterCount <= 10) 0.68f else 4f / (characterCount + 3)
    val animationDurationMs = wordDurationMs * windowFraction
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

internal fun lyricCharacterFloatOffset(
    positionMs: Long,
    startTimeMs: Long,
    endTimeMs: Long,
    characterIndex: Int,
    characterCount: Int,
): Float {
    val count = characterCount.coerceAtLeast(1)
    val duration = (endTimeMs - startTimeMs).coerceAtLeast(1L).toFloat()
    val window = duration * (2f / (count + 1)).coerceAtMost(1f)
    val ratio = if (count == 1) 0f else characterIndex.coerceIn(0, count - 1).toFloat() / (count - 1)
    val start = startTimeMs + (duration - window) * ratio
    val progress = ((positionMs - start) / window).coerceIn(0f, 1f)
    val eased = (1f - cos(progress * Math.PI).toFloat()) / 2f
    return -LYRIC_FLOAT_MAX_OFFSET_PX * eased
}

internal fun characterMotion(
    positionMs: Long,
    wordStartTimeMs: Long,
    wordEndTimeMs: Long,
    characterIndex: Int,
    characterCount: Int,
): WordMotion {
    val progress = characterProgress(
        positionMs = positionMs,
        wordStartTimeMs = wordStartTimeMs,
        wordEndTimeMs = wordEndTimeMs,
        characterIndex = characterIndex,
        characterCount = characterCount,
    )
    val motion = wordMotion(progress, wordEndTimeMs - wordStartTimeMs)
    return motion.copy(
        offsetYPx = lyricCharacterFloatOffset(
            positionMs, wordStartTimeMs, wordEndTimeMs, characterIndex, characterCount,
        ),
    )
}

internal fun wordMotion(
    progress: Float,
    durationMs: Long,
): WordMotion {
    val boundedProgress = progress.coerceIn(0f, 1f)
    val durationRatio = ((durationMs - 1_000L) / 2_000f).coerceIn(0f, 1f)
    val intensity = durationRatio * durationRatio * (3f - 2f * durationRatio)
    val pulse = sin(boundedProgress * Math.PI).toFloat().let { it * it }
    return WordMotion(
        scale = 1f + (0.06f + 0.06f * intensity) * pulse,
        offsetYPx = -LYRIC_FLOAT_MAX_OFFSET_PX *
            (1f - cos(boundedProgress * Math.PI).toFloat()) / 2f,
        glowRadius = 7f + 5f * intensity,
        glowAlpha = (0.42f + 0.2f * intensity) * pulse,
    )
}
