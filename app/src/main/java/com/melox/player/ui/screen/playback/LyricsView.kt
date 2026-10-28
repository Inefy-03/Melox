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
import androidx.compose.foundation.indication
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melox.player.model.LyricsTranslationLanguage
import com.melox.player.model.LyricAnimationMode
import com.melox.player.model.LyricLine
import com.melox.player.model.LyricWord
import com.melox.player.model.LyricAlignment
import com.melox.player.model.LyricAuxiliaryText
import com.melox.player.model.LyricsDocument
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import java.util.Locale
import kotlin.math.cos
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.utils.MiuixIndication

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
internal const val LYRIC_ROMANIZATION_FONT_SIZE_SP = 14f
internal const val LYRIC_ROMANIZATION_LINE_HEIGHT_SP = 16f
internal const val LYRIC_TRANSLATION_FONT_SIZE_SP = 16f
internal const val LYRIC_TRANSLATION_LINE_HEIGHT_SP = 22f
internal const val LYRIC_TRANSLATION_GAP_DP = 2f
private val LyricLayerPaint = Paint()
internal val LyricLineHeightStyle = LineHeightStyle(
    LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None, LineHeightStyle.Mode.Minimum,
)

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

internal fun lyricFocusCenterOffset(
    targetIndex: Int,
    rows: List<LyricVisualRow>,
    focusLineIndices: List<Int>,
    viewportHeightPx: Int,
    centerOffsetPx: Float,
): Float {
    val target = rows.firstOrNull { it.index == targetIndex } ?: return centerOffsetPx
    val focusRows = rows.filter { it.index in focusLineIndices }
    if (focusRows.size <= 1 || focusRows.size != focusLineIndices.size) return centerOffsetPx
    val groupTop = focusRows.minOf { it.top }
    val groupBottom = focusRows.maxOf { it.top + it.height }
    if (groupBottom - groupTop > viewportHeightPx - 2f * abs(centerOffsetPx)) return centerOffsetPx
    return centerOffsetPx + target.top + target.height / 2f - (groupTop + groupBottom) / 2f
}

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
    active: Boolean = true,
): Boolean = hasPositionedInitialFocus && centerOffsetUnchanged && !isPreviewing && active

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
internal fun rememberSmoothLyricTimeProvider(
    document: LyricsDocument,
    positionMs: Long,
    positionUpdateElapsedRealtimeMs: Long,
    playbackIteration: Long,
    playbackSpeed: Float,
    isPlaying: Boolean,
    active: Boolean,
    positionDiscontinuityRevision: Long,
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
    val clock = remember(document) {
        LyricPlaybackClock(positionMs, playbackIteration, positionDiscontinuityRevision)
    }
    val smoothPositionMs = clock.positionMs
    val latestPlaybackIteration by rememberUpdatedState(playbackIteration)
    val latestDiscontinuityRevision by rememberUpdatedState(positionDiscontinuityRevision)

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
        positionMs,
        positionUpdateElapsedRealtimeMs,
        playbackSpeed,
        isPlaying,
        active,
        positionDiscontinuityRevision,
    ) {
        clock.updateFromPlayback(
            playbackIteration, currentPlaybackPositionMs(), active, positionDiscontinuityRevision,
        )
        if (seekRequestKey != 0) {
            smoothPositionMs.doubleValue = seekPositionMs.coerceAtLeast(0L).toDouble()
        }
    }
    LaunchedEffect(document, isPlaying, active) {
        if (!isPlaying || !active) return@LaunchedEffect
        var previousFrameTimeNanos = 0L
        while (isActive) {
            val frameTimeNanos = withFrameNanos { it }
            val frameAdvanceNanos = if (previousFrameTimeNanos > 0L) {
                (frameTimeNanos - previousFrameTimeNanos).coerceAtLeast(0L)
            } else {
                0L
            }
            previousFrameTimeNanos = frameTimeNanos
            val clockReset = clock.updateFromPlayback(
                latestPlaybackIteration, currentPlaybackPositionMs(), active, latestDiscontinuityRevision,
            )
            smoothPositionMs.doubleValue = if (latestSeekRequestKey != 0) {
                latestSeekPositionMs.coerceAtLeast(0L).toDouble()
            } else if (clockReset) {
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

internal data class LyricBrowseAnchor(val index: Int, val offset: Int)

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
    activeLyricAlpha: Float = 1f,
    active: Boolean = true,
    playerActive: Boolean = true,
    positionDiscontinuityRevision: Long = 0L,
    showLyricsRomanization: Boolean = true,
    lyricsTranslationLanguage: LyricsTranslationLanguage = LyricsTranslationLanguage.AUTO,
    applyEdgeFade: Boolean = true,
    annotationLayout: Boolean? = null,
    annotationFrameTarget: Boolean = true,
    initialBrowseAnchor: LyricBrowseAnchor? = null,
    onBrowseAnchorChange: (LyricBrowseAnchor?) -> Unit = {},
) {
    val appLanguageTag = LocalConfiguration.current.locales[0].toLanguageTag()
    val selectedTranslations = remember(document, lyricsTranslationLanguage, appLanguageTag) {
        document.lines.map { it.translationFor(lyricsTranslationLanguage, appLanguageTag) }
    }
    val suppliedPhonetics = remember(document) { document.lines.map(::lyricPhonetics) }
    val documentHasTimedWords = remember(document) {
        document.lines.any { it.words.isNotEmpty() }
    }
    val listState = rememberLazyListState(initialBrowseAnchor?.index ?: 0, initialBrowseAnchor?.offset ?: 0)
    val currentTimeProvider = rememberSmoothLyricTimeProvider(
        document = document,
        positionMs = positionMs,
        positionUpdateElapsedRealtimeMs = positionUpdateElapsedRealtimeMs,
        playbackIteration = playbackIteration,
        playbackSpeed = playbackSpeed,
        isPlaying = isPlaying,
        active = playerActive,
        positionDiscontinuityRevision = positionDiscontinuityRevision,
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
    // Offscreen rows need ready geometry, but do not need per-frame glyph rendering.
    val offscreenRenderPositionMs = lyricDisplayedPositionMs(
        previewPositionMs = previewPositionMs,
        seekRequestPending = seekRequestKey != 0,
        seekPositionMs = seekPositionMs,
        smoothPositionMs = positionMs,
    )
    val density = LocalDensity.current
    val alignmentProgress = animateFloatAsState(
        targetValue = if (centerLyrics && !document.isDuet) 1f else 0f,
        animationSpec = playerTextAlignmentSpec(),
        label = "lyricsAlignment",
    )
    val viewConfiguration = LocalViewConfiguration.current
    val centerOffsetPx = with(density) { centerOffsetY.toPx() }
    val keepAliveZone = 100.dp
    val playbackFrame by remember(document) {
        derivedStateOf { document.playbackFrame(lyricTimeProvider()) }
    }
    val focusLineIndex by remember(document) {
        derivedStateOf { document.playbackFrame(lyricTimeProvider()).anchorIndex }
    }
    val activeLineIndices = playbackFrame.activeLineIndices
    var isUserBrowsingLyrics by remember(document) { mutableStateOf(initialBrowseAnchor != null) }
    var outgoingSeekPositions by remember(document) { mutableStateOf(emptyMap<Int, Long>()) }
    var pendingSeekCenteringTargetIndex by remember(document) { mutableIntStateOf(-1) }
    var pendingSeekCenteringDeltaPx by remember(document) { mutableStateOf<Float?>(null) }
    val isPreviewing = previewPositionMs != null
    val rowMotion = remember(document) { LyricRowMotion() }
    val translationMotion = remember(document) { LyricTranslationMotion(showLyricsTranslation) }
    val romanizationMotion = remember(document) { LyricTranslationMotion(showLyricsRomanization) }
    val hasMappedPronunciation = remember(suppliedPhonetics) { suppliedPhonetics.any { it.hasAnnotations } }
    val prepareInlinePhonetics = hasMappedPronunciation && (annotationLayout ?: showLyricsRomanization)
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
    var hasPositionedInitialFocus by remember(document) { mutableStateOf(initialBrowseAnchor != null) }
    val latestBrowseAnchorChange = rememberUpdatedState(onBrowseAnchorChange)
    LaunchedEffect(document, listState) {
        snapshotFlow {
            if (isUserBrowsingLyrics) LyricBrowseAnchor(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
            else null
        }.collect { latestBrowseAnchorChange.value(it) }
    }
    val centeringGeometry = listOf(
        centerOffsetY, viewportHeightPx, viewportWidthPx, contentWidth,
        lyricFontScale, lyricFontWeight, lyricsTranslationLanguage, appLanguageTag,
        prepareInlinePhonetics,
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
        textAlign = if (document.isDuet) TextAlign.Left else TextAlign.Start,
        lineHeightStyle = LyricLineHeightStyle,
    )
    val translationTextStyle = MiuixTheme.textStyles.body1.copy(
        fontSize = (LYRIC_TRANSLATION_FONT_SIZE_SP * lyricFontScale).sp,
        lineHeight = (LYRIC_TRANSLATION_LINE_HEIGHT_SP * lyricFontScale).sp,
        fontFamily = lyricFontFamily,
        fontWeight = FontWeight(lyricFontWeight.coerceIn(1, 1000)),
        fontSynthesis = FontSynthesis.None,
        textDirection = TextDirection.Content,
        textMotion = TextMotion.Animated,
        lineHeightStyle = LyricLineHeightStyle,
    )
    val romanizationTextStyle = translationTextStyle.copy(
        fontSize = (LYRIC_ROMANIZATION_FONT_SIZE_SP * lyricFontScale).sp,
        lineHeight = (LYRIC_ROMANIZATION_LINE_HEIGHT_SP * lyricFontScale).sp,
    )
    val textMeasurer = rememberTextMeasurer()
    val layoutDirection = LocalLayoutDirection.current
    val fontResolver = LocalFontFamilyResolver.current
    val lineCache = remember(document, normalTextStyle, romanizationTextStyle, emphasisColor,
        density, layoutDirection, fontResolver, prepareInlinePhonetics, appLanguageTag) {
        PreparedLyricLineCache(romanizationTextStyle.takeIf { prepareInlinePhonetics },
            document.language ?: appLanguageTag)
    }

    val lyricBrowsingModifier = Modifier.pointerInput(document) {
        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Final,
            )
            var movement = Offset.Zero
            var browsingStarted = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (change.changedToUpIgnoreConsumed()) break
                // Both positions use this event's coordinates, even when the pager moves us.
                movement += change.position - change.previousPosition
                if (!browsingStarted) {
                    if (
                        abs(movement.x) > viewConfiguration.touchSlop &&
                            abs(movement.x) >= abs(movement.y)
                    ) {
                        // Once horizontal paging wins, this gesture cannot become browsing.
                        break
                    }
                    if (lyricVerticalDragExceedsTouchSlop(
                            horizontalDeltaPx = movement.x,
                            verticalDeltaPx = movement.y,
                            touchSlopPx = viewConfiguration.touchSlop,
                        )
                    ) {
                        browsingStarted = true
                        isUserBrowsingLyrics = true
                    }
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
        outgoingSeekPositions = emptyMap()
        pendingSeekCenteringTargetIndex = -1
        pendingSeekCenteringDeltaPx = null
    }
    LaunchedEffect(activeLineIndices) {
        outgoingSeekPositions = outgoingSeekPositions.filterKeys { it !in activeLineIndices }
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
        translationMotion.transitionTo(showLyricsTranslation)
    }
    LaunchedEffect(document, showLyricsRomanization, annotationFrameTarget) {
        if (hasMappedPronunciation && annotationLayout != null) {
            if (annotationFrameTarget) romanizationMotion.snapTo(showLyricsRomanization)
            else romanizationMotion.transitionTo(showLyricsRomanization, lyricContentExitSpec())
        } else romanizationMotion.transitionTo(showLyricsRomanization)
    }
    val centeringEvent = LyricCenteringEvent(
        focusIndex = visualFocusRenderIndex,
        requestedPositionMs = seekPositionMs,
        tapRevision = lyricTapRevision,
        browsing = isUserBrowsingLyrics,
        previewing = isPreviewing,
        geometry = centeringGeometry,
        active = active,
        focusLineIndices = playbackFrame.anchorLineIndices,
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
        var centeringOffsetPx = centerOffsetPx
        try {
            scrollInCode.value = true
            listState.scroll {
                // Own native scrolling before capturing positions or awaiting retention layout.
                val layoutInfo = listState.layoutInfo
                val targetItem = layoutInfo.visibleItemsInfo.firstOrNull {
                    it.index == visualFocusRenderIndex
                }
                val focusItems = layoutInfo.visibleItemsInfo.filter { it.index in centeringEvent.focusLineIndices }
                centeringOffsetPx = lyricFocusCenterOffset(
                    targetIndex = visualFocusRenderIndex,
                    rows = layoutInfo.visibleItemsInfo.map { LyricVisualRow(it.index, it.offset.toFloat(), it.size) },
                    focusLineIndices = centeringEvent.focusLineIndices,
                    viewportHeightPx = viewportHeightPx,
                    centerOffsetPx = centerOffsetPx,
                )
                animateCentering = lyricSeekUsesAnimatedCentering(
                    hasPositionedInitialFocus = hasPositionedInitialFocus,
                    centerOffsetUnchanged = lastCenteringGeometry == centeringGeometry,
                    isPreviewing = isPreviewing,
                    active = active,
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
                    pendingSeekCenteringTargetIndex == visualFocusRenderIndex && focusItems.size <= 1
                }
                val measuredScrollDelta = capturedSeekCenteringDelta ?: targetItem?.let { visibleTarget ->
                    lyricCenterScrollDelta(
                        itemOffset = visibleTarget.offset,
                        itemSize = visibleTarget.size,
                        viewportStartOffset = layoutInfo.viewportStartOffset,
                        viewportEndOffset = layoutInfo.viewportEndOffset,
                        centerOffsetPx = centeringOffsetPx,
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
                    val currentFrame = document.playbackFrame(lyricTimeProvider())
                    if (currentFrame.anchorIndex != visualFocusRenderIndex ||
                        currentFrame.anchorLineIndices != centeringEvent.focusLineIndices
                    ) {
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
                            centerOffsetPx = centeringOffsetPx,
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
                            centerOffsetPx = centeringOffsetPx,
                        )
                        // A delta may defer placement; snap with the measured size before compensation.
                        if (abs(correction) > 0.5f) {
                            placement.snapToItem(
                                index = visualFocusRenderIndex,
                                offset = lyricTargetScrollOffset(
                                    itemSize = centeredItem.size,
                                    viewportStartOffset = centeredLayoutInfo.viewportStartOffset,
                                    viewportEndOffset = centeredLayoutInfo.viewportEndOffset,
                                    centerOffsetPx = centeringOffsetPx,
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
        fun measuredCenteringDelta(): Float? {
            val measured = listState.layoutInfo
            val target = measured.visibleItemsInfo.firstOrNull {
                it.index == visualFocusRenderIndex
            } ?: return null
            val focusOffset = lyricFocusCenterOffset(
                targetIndex = visualFocusRenderIndex,
                rows = measured.visibleItemsInfo.map {
                    LyricVisualRow(it.index, it.offset.toFloat(), it.size)
                },
                focusLineIndices = centeringEvent.focusLineIndices,
                viewportHeightPx = viewportHeightPx,
                centerOffsetPx = centerOffsetPx,
            )
            return lyricCenterScrollDelta(
                itemOffset = target.offset,
                itemSize = target.size,
                viewportStartOffset = measured.viewportStartOffset,
                viewportEndOffset = measured.viewportEndOffset,
                centerOffsetPx = focusOffset,
            )
        }
        coroutineScope {
            launch {
                if (animateCentering) {
                    rowMotion.settle(
                        stiffness = centeringSpringStiffness,
                        visibleOrder = visibleRowOrder,
                        cascade = true,
                    )
                    retainedTravelPx = 0f
                }
            }
            // Follow measured caption height without restarting the independent row spring.
            // Keep observing the last measurement too, including animation scale zero.
            snapshotFlow {
                if (isUserBrowsingLyrics || isManualScrolling) null else measuredCenteringDelta()
            }.collect { delta ->
                if (delta == null || abs(delta) <= 0.5f) return@collect
                scrollInCode.value = true
                try {
                    listState.scroll {
                        measuredCenteringDelta()?.let { if (abs(it) > 0.5f) scrollBy(it) }
                    }
                } finally {
                    scrollInCode.value = false
                }
            }
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
                    val wordEffects = lyricAnimationMode.usesWordAnimation(line.words.isNotEmpty(), documentHasTimedWords)
                    val hasPhonetics = suppliedPhonetics[index].hasAnnotations
                    if (!wordEffects && !(prepareInlinePhonetics && hasPhonetics)) continue
                    lineCache.prepare(line, textWidthPx,
                        measurer, normalTextStyle, emphasisColor, density, layoutDirection, wordEffects)
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
                        if (!applyEdgeFade) return@drawWithCache onDrawWithContent { drawContent() }
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
                    val distanceFromFocus = activeLineIndices.minOfOrNull { kotlin.math.abs(index - it) }
                        ?: if (focusLineIndex >= 0) kotlin.math.abs(index - focusLineIndex) else 0
                    val lineAlignment = document.lineAlignment(index).takeIf { document.isDuet }
                    val lineAlignmentProgress: State<Float> = if (lineAlignment == null) {
                        alignmentProgress
                    } else remember(lineAlignment) {
                        mutableFloatStateOf(if (lineAlignment == LyricAlignment.CENTER) 1f else 0f)
                    }
                    val endAligned = lineAlignment == LyricAlignment.END
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
                        val isCurrentLine = index in activeLineIndices
                        val preservesOutgoingSeekProgress = !isCurrentLine && index in outgoingSeekPositions
                        val usesWordProgress = lyricAnimationMode.usesWordAnimation(
                            lineHasTimedWords = line.words.isNotEmpty(),
                            documentHasTimedWords = documentHasTimedWords,
                        )
                        val hasPhonetics = suppliedPhonetics[index].hasAnnotations
                        val prepared = if (usesWordProgress || prepareInlinePhonetics && hasPhonetics) remember(lineCache, line, textWidthPx, usesWordProgress) {
                            lineCache.prepare(line, textWidthPx, textMeasurer, normalTextStyle,
                                emphasisColor, density, layoutDirection, wordEffects = usesWordProgress)
                        } else null
                        val visualOutsets = remember(prepared, textWidthPx, density, endAligned) {
                            prepared?.visualOutsets(textWidthPx, density, endAligned) ?: LayerOutsets()
                        }
                        val lineTimeProvider = {
                            lyricLineRenderPositionMs(
                                lineIndex = index,
                                displayedPositionMs = if (active) lyricTimeProvider() else offscreenRenderPositionMs,
                                outgoingSeekLineIndex = if (preservesOutgoingSeekProgress) index else -1,
                                outgoingSeekPositionMs = outgoingSeekPositions[index] ?: 0L,
                            )
                        }
                        LyricsLineItem(
                            activeOpacityLimit = activeLyricAlpha,
                            pressColor = emphasisColor,
                            isFocused = isCurrentLine,
                            usesWordProgress = usesWordProgress,
                            alignmentProgress = lineAlignmentProgress,
                            endAligned = endAligned,
                            blurRadius = blurRadius,
                            glyphOutset = glyphOutset,
                            visualOutsets = visualOutsets,
                            onAlphaSettled = { settledAlpha ->
                                if (
                                    lyricOutgoingSeekCanClear(
                                        lineIndex = index,
                                        outgoingSeekLineIndex = if (index in outgoingSeekPositions) index else -1,
                                        currentLineIndex = if (isCurrentLine) index else -1,
                                        settledAlpha = settledAlpha,
                                    )
                                ) {
                                    outgoingSeekPositions = outgoingSeekPositions - index
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
                                val leavingTime = lyricTimeProvider()
                                outgoingSeekPositions = (outgoingSeekPositions +
                                    activeLineIndices.associateWith { leavingTime }) - index
                                isUserBrowsingLyrics = false
                                onSeek(line.startTimeMs)
                            },
                        ) { wordProgressActiveAlpha, translationAlpha, primaryTextAlpha ->
                            if (prepared != null) {
                                TimedLyricLine(
                                    activeOpacityLimit = activeLyricAlpha,
                                    line = line,
                                    translation = selectedTranslations[index],
                                    prepared = prepared,
                                    usesWordProgress = usesWordProgress,
                                    primaryTextAlpha = primaryTextAlpha,
                                    usePlaybackProgress =
                                        isCurrentLine || preservesOutgoingSeekProgress,
                                    translationAlpha = translationAlpha,
                                    activeAlpha = wordProgressActiveAlpha,
                                    currentTimeProvider = lineTimeProvider,
                                    normalTextStyle = normalTextStyle,
                                    translationTextStyle = translationTextStyle,
                                    romanizationTextStyle = romanizationTextStyle,
                                    emphasisColor = emphasisColor,
                                    alignmentProgress = lineAlignmentProgress,
                                    lineAlignment = lineAlignment,
                                    translationMotion = translationMotion,
                                    romanizationMotion = romanizationMotion,
                                )
                            } else {
                                SyncedLyricLine(
                                    primaryTextAlpha = primaryTextAlpha,
                                    line = line,
                                    translation = selectedTranslations[index],
                                    translationAlpha = translationAlpha,
                                    normalTextStyle = normalTextStyle,
                                    translationTextStyle = translationTextStyle,
                                    romanizationTextStyle = romanizationTextStyle,
                                    emphasisColor = emphasisColor,
                                    alignmentProgress = lineAlignmentProgress,
                                    lineAlignment = lineAlignment,
                                    translationMotion = translationMotion,
                                    romanizationMotion = romanizationMotion,
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
    activeOpacityLimit: Float,
    pressColor: Color,
    isFocused: Boolean,
    usesWordProgress: Boolean,
    alignmentProgress: State<Float>,
    endAligned: Boolean,
    blurRadius: Float,
    glyphOutset: Dp,
    visualOutsets: LayerOutsets,
    onAlphaSettled: (Float) -> Unit,
    onClick: () -> Unit,
    content: @Composable (
        wordProgressActiveAlpha: Float,
        translationAlpha: Float,
        primaryTextAlpha: Float,
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
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier.matchParentSize()
                .graphicsLayer { this.alpha = 0.6f }
                .squircleClip(12.dp)
                .indication(interactionSource, remember(pressColor) { MiuixIndication(pressColor) }),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = layerAlpha
                    transformOrigin = TransformOrigin(
                        pivotFractionX = if (endAligned) 1f else alignmentProgress.value * 0.5f,
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
                .padding(horizontal = LYRIC_ANIMATION_BLEED_DP.dp),
        ) {
            content(
                lyricWordProgressActiveAlpha(
                    lineAlpha = alpha,
                    usesWordProgress = usesWordProgress,
                ),
                lyricTranslationAlpha(layerAlpha),
                minOf(alpha, activeOpacityLimit) / alpha.coerceAtLeast(LYRIC_INACTIVE_TEXT_ALPHA),
            )
        }
    }
}

@Composable
private fun AuxiliaryLyricText(
    text: String,
    style: TextStyle,
    color: Color,
    alignmentProgress: State<Float>,
    lineAlignment: LyricAlignment?,
    motion: LyricTranslationMotion,
    lyricFontScale: Float,
    modifier: Modifier = Modifier,
) {
    var textLayout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    Box(modifier = modifier.fillMaxWidth()) {
        Text(
            text = text,
            style = style,
            color = color,
            textAlign = if (lineAlignment == null) TextAlign.Start else TextAlign.Left,
            onTextLayout = { textLayout = it },
            modifier = Modifier
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
                .animatedPlayerTextAlignment(textLayout, alignmentProgress,
                    endAligned = lineAlignment == LyricAlignment.END)
                .graphicsLayer { alpha = motion.opacity.value }
                .then(if (motion.opacity.value == 0f) Modifier.clearAndSetSemantics { }
                    else Modifier.semantics { this.text = AnnotatedString(text) }),
        )
    }
}

/** Measure the same unbounded caption groups used by the compact renderer. */
internal fun compactLyricAnnotationHeight(
    line: LyricLine,
    style: TextStyle,
    phoneticStyle: TextStyle,
    measurer: TextMeasurer,
    density: androidx.compose.ui.unit.Density,
): Int {
    val annotations = lyricPhonetics(line)
    if (!annotations.hasAnnotations) return 0
    var offset = 0
    val sources = line.animationWords().map { word ->
        val start = offset
        offset += word.text.length + if (word.hasTrailingSpace) 1 else 0
        ParagraphSource(word, start, offset)
    }
    val text = sources.joinToString("") { it.word.text + if (it.word.hasTrailingSpace) " " else "" }
    val layout = measurer.measure(text, style.copy(lineHeightStyle = LyricLineHeightStyle), softWrap = false)
    val captions = paragraphCaptions(paragraphClusters(layout, text), sources, annotations, text,
        measurer, phoneticStyle, Constraints.Infinity)
    val height = captions.maxOfOrNull { it.layout.size.height } ?: return 0
    return ceil(height + with(density) { (4f * style.fontSize.value / LYRIC_PRIMARY_FONT_SIZE_SP).dp.toPx() }).toInt()
}

@Composable
internal fun CompactTimedLyricLine(
    line: LyricLine,
    style: TextStyle,
    color: Color,
    currentTimeProvider: () -> Long,
    animationMode: LyricAnimationMode,
    documentHasTimedWords: Boolean,
    modifier: Modifier = Modifier,
    activeOpacityLimit: Float = 1f,
    centered: Boolean = false,
    focused: Boolean = true,
    resetKey: Any? = null,
    textStartPadding: Dp = 0.dp,
    textBaseline: Dp? = null,
    phoneticStyle: TextStyle? = null,
    textEndPadding: Dp = 0.dp,
) {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val measurer = rememberTextMeasurer()
    val cache = remember(style, phoneticStyle, color, density, direction) { PreparedLyricLineCache(phoneticStyle) }
    val singleLine = remember(line) { compactLyricSingleLine(line) }
    val prepared = remember(cache, singleLine) {
        cache.prepare(singleLine, Float.MAX_VALUE, measurer, style, color, density, direction,
            wordEffects = false)
    }
    val preparedBaseline = remember(prepared) {
        prepared.rows.firstOrNull()?.layouts?.maxOfOrNull { it.position.y + it.firstBaseline } ?: 0f
    }
    val alignment = remember { mutableFloatStateOf(0f) }
    val scroll = remember(singleLine, resetKey) { CompactLyricScroll() }
    val latestTime = rememberUpdatedState(currentTimeProvider)
    val usesWordProgress = animationMode.usesWordAnimation(line.words.isNotEmpty(), documentHasTimedWords)
    val lineHeight = with(density) {
        maxOf(compactLyricLineHeight(style).toPx(), prepared.heightPx.toFloat()).toDp()
    }
    val effectScale = with(density) { lyricEffectGeometryScale(style.fontSize.toPx()) }
    val fadeWidth = with(density) { style.fontSize.toPx() * 0.25f }
    val textStartPx = with(density) { textStartPadding.toPx() }
    val textEndPx = with(density) { textEndPadding.toPx() }
    val textBaselinePx = with(density) { textBaseline?.toPx() }
    var durationScale by remember { mutableStateOf<MotionDurationScale?>(null) }
    LaunchedEffect(Unit) { durationScale = coroutineContext[MotionDurationScale] }
    Box(
        modifier = modifier.height(lineHeight)
            .semantics { text = AnnotatedString(line.displayText) }
            .drawWithCache {
                onDrawBehind {
                    val row = prepared.rows.firstOrNull() ?: return@onDrawBehind
                    val position = latestTime.value()
                    val cursor = if (!focused) row.pieces.firstOrNull()?.startX ?: 0f else if (usesWordProgress) {
                        linePixelPosition(row, position)
                    } else {
                        lyricRevealPosition(row.pieces, row.startDistance +
                            (row.endDistance - row.startDistance) *
                            lyricIntervalProgress(position, line.startTimeMs, line.endTimeMs))
                    }
                    val target = textStartPx + compactLyricScrollTarget(
                        (size.width - textStartPx).coerceAtLeast(0f), row.width, cursor, centered, textEndPx)
                    val offset = scroll.update(target, position, size.width, row.width + textEndPx,
                        smooth = durationScale?.scaleFactor != 0f)
                    clipRect {
                        val annotationHeight = if (row.phonetics.isEmpty()) 0f
                            else row.phoneticHeight + prepared.phoneticGap
                        val textTop = textBaselinePx?.minus(preparedBaseline)
                            ?: ((size.height - prepared.heightPx) / 2f)
                        translate(offset, textTop) {
                            drawLyricPhonetics(row, color.copy(alpha = color.alpha *
                                lyricTranslationAlpha(1f) * activeOpacityLimit),
                                if (usesWordProgress && focused) position else null, row.width)
                        }
                        translate(offset, textTop + annotationHeight) {
                            drawLyricsLine(
                                rows = prepared.rows,
                                canvasWidth = row.width,
                                alignmentProgress = alignment,
                                endAligned = false,
                                positionMs = if (usesWordProgress && focused) position else line.endTimeMs,
                                color = color,
                                activeAlpha = 1f,
                                activeOpacityLimit = activeOpacityLimit,
                                effectScale = effectScale,
                                fadeWidthPx = fadeWidth,
                                glyphMotionEnabled = false,
                                inactiveAlpha = 0.3f,
                            )
                        }
                    }
                }
            },
    )
}

@Composable
private fun TimedLyricLine(
    activeOpacityLimit: Float,
    line: LyricLine,
    translation: String?,
    prepared: PreparedLyricLine,
    usesWordProgress: Boolean,
    primaryTextAlpha: Float,
    usePlaybackProgress: Boolean,
    translationAlpha: Float,
    activeAlpha: Float,
    currentTimeProvider: () -> Long,
    normalTextStyle: TextStyle,
    translationTextStyle: TextStyle,
    romanizationTextStyle: TextStyle,
    emphasisColor: Color,
    alignmentProgress: State<Float>,
    lineAlignment: LyricAlignment?,
    translationMotion: LyricTranslationMotion,
    romanizationMotion: LyricTranslationMotion,
) {
    val latestTimeProvider = rememberUpdatedState(currentTimeProvider)
    val staticCompleted by remember(line, latestTimeProvider) {
        derivedStateOf { latestTimeProvider.value() >= line.endTimeMs }
    }
    val phoneticTimeProvider: () -> Long? = {
        if (!usesWordProgress) null else lyricStableRenderPositionMs(
            usePlaybackProgress, staticCompleted, if (usePlaybackProgress) latestTimeProvider.value() else 0L,
        )
    }
    val hasRomanization = line.romanization != null || prepared.hasInlinePhonetics
    val verticalPadding = lyricTranslationPaddingDp(
        hasTranslation = translation != null || hasRomanization,
        expansion = maxOf(
            if (translation != null) translationMotion.expansion.value else 0f,
            if (hasRomanization) romanizationMotion.expansion.value else 0f,
        ),
        lyricFontScale = normalTextStyle.fontSize.value / LYRIC_PRIMARY_FONT_SIZE_SP,
    ).dp
    val groups = remember(prepared) {
        if (prepared.hasInlinePhonetics) prepared.rows.map { listOf(it) } else listOf(prepared.rows)
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = verticalPadding),
        horizontalAlignment = Alignment.Start,
    ) {
        Column(Modifier.fillMaxWidth().semantics {
            text = AnnotatedString(buildString {
                append(line.displayText)
                if (prepared.hasInlinePhonetics && romanizationMotion.opacity.value > 0f) {
                    for (row in prepared.rows) for (caption in row.phonetics) {
                        append('\n')
                        append(caption.layout.layoutInput.text.text)
                    }
                }
            })
        }) {
            for ((index, rows) in groups.withIndex()) {
                val row = if (prepared.hasInlinePhonetics) rows.single() else null
                if (row != null) {
                    InlineLyricPhonetics(row, prepared.phoneticGap, romanizationMotion,
                        emphasisColor.copy(alpha = translationAlpha), alignmentProgress,
                        lineAlignment == LyricAlignment.END, phoneticTimeProvider)
                }
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val density = LocalDensity.current
                    val effectScale = with(density) { lyricEffectGeometryScale(normalTextStyle.fontSize.toPx()) }
                    val fadeWidth = with(density) { lyricForegroundFadeWidthPx(normalTextStyle.fontSize.toPx()) }
                    val height = row?.height ?: prepared.heightPx.toFloat()
                    var motionDurationScale by remember { mutableStateOf<MotionDurationScale?>(null) }
                    LaunchedEffect(Unit) { motionDurationScale = coroutineContext[MotionDurationScale] }
                    val latestActiveAlpha = rememberUpdatedState(activeAlpha)
                    val latestPrimaryAlpha = rememberUpdatedState(primaryTextAlpha)
                    val latestActiveOpacityLimit = rememberUpdatedState(activeOpacityLimit)
                    val drawTimedLyrics: CacheDrawScope.() -> DrawResult = remember(
                        rows, emphasisColor, usesWordProgress, usePlaybackProgress, staticCompleted, lineAlignment,
                    ) {
                        {
                            onDrawBehind {
                                translate(top = -(row?.top ?: 0f)) {
                                    drawLyricsLine(
                                        rows = rows,
                                        canvasWidth = size.width,
                                        alignmentProgress = alignmentProgress,
                                        endAligned = lineAlignment == LyricAlignment.END,
                                        positionMs = if (usePlaybackProgress) latestTimeProvider.value()
                                            else if (staticCompleted) line.endTimeMs else line.startTimeMs,
                                        color = emphasisColor,
                                        activeAlpha = latestActiveAlpha.value,
                                        activeOpacityLimit = latestActiveOpacityLimit.value,
                                        effectScale = effectScale,
                                        fadeWidthPx = fadeWidth,
                                        glyphMotionEnabled = motionDurationScale?.scaleFactor != 0f,
                                        usesWordProgress = usesWordProgress,
                                        primaryTextAlpha = latestPrimaryAlpha.value,
                                    )
                                }
                            }
                        }
                    }
                    Box(Modifier.size(maxWidth, with(density) { height.toDp() })
                        .drawWithCache(drawTimedLyrics))
                }
                if (row != null && row.phonetics.isNotEmpty() && index < groups.lastIndex) {
                    val spacing = with(LocalDensity.current) { prepared.phoneticRowSpacing.toDp() }
                    Box(Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints.copy(minHeight = 0))
                        val height = (placeable.height * romanizationMotion.expansion.value.coerceIn(0f, 1f)).roundToInt()
                        layout(placeable.width, constraints.constrainHeight(height)) { placeable.place(0, 0) }
                    }.height(spacing))
                }
            }
        }
        prepared.standaloneRomanization?.let { romanization ->
            AuxiliaryLyricText(
                text = romanization,
                style = romanizationTextStyle,
                color = emphasisColor.copy(alpha = translationAlpha),
                alignmentProgress = alignmentProgress,
                lineAlignment = lineAlignment,
                motion = romanizationMotion,
                lyricFontScale = normalTextStyle.fontSize.value / LYRIC_PRIMARY_FONT_SIZE_SP,
            )
        }
        translation?.let { translation ->
            AuxiliaryLyricText(
                text = translation,
                style = translationTextStyle,
                color = emphasisColor.copy(alpha = translationAlpha),
                alignmentProgress = alignmentProgress,
                lineAlignment = lineAlignment,
                motion = translationMotion,
                lyricFontScale = normalTextStyle.fontSize.value / LYRIC_PRIMARY_FONT_SIZE_SP,
            )
        }
    }
}

@Composable
private fun InlineLyricPhonetics(
    row: RowRenderData,
    gap: Float,
    motion: LyricTranslationMotion,
    color: Color,
    alignmentProgress: State<Float>,
    endAligned: Boolean,
    currentTimeProvider: () -> Long?,
) {
    if (row.phonetics.isEmpty()) return
    val density = LocalDensity.current
    val latestTime = rememberUpdatedState(currentTimeProvider)
    Canvas(Modifier.fillMaxWidth().clipToBounds()
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(minHeight = 0))
            val height = (placeable.height * motion.expansion.value.coerceIn(0f, 1f)).roundToInt()
            layout(placeable.width, constraints.constrainHeight(height)) {
                placeable.place(0, 0)
            }
        }
        .height(with(density) { (row.phoneticHeight + gap).toDp() })
        .graphicsLayer { alpha = motion.opacity.value },
    ) {
        val offset = playerTextAlignmentOffset(row.minX, row.maxX, size.width, alignmentProgress.value, endAligned)
        drawLyricPhonetics(row, color, latestTime.value(), size.width, offset)
    }
}

/** Both lyric surfaces share authored pronunciation timing and reveal masks. */
private fun DrawScope.drawLyricPhonetics(
    row: RowRenderData,
    color: Color,
    time: Long?,
    viewportWidth: Float,
    offset: Float = 0f,
) {
    for (caption in row.phonetics) {
        val position = caption.position + Offset(offset, 0f)
        if (time == null) {
            drawText(caption.layout, color, topLeft = position)
        } else {
            // Pronunciation follows its source timing, without inheriting glyph motion.
            val path = caption.revealPath
            path.reset()
            var unstarted = true
            var completed = true
            for (revealRow in caption.revealRows) {
                val progress = revealRow.progressAt(time)
                unstarted = unstarted && progress <= 0f
                completed = completed && progress >= 1f
                if (progress >= 1f) {
                    // Completed tracks retain horizontal ink overhang while other tracks sing.
                    path.addRect(Rect(-viewportWidth, revealRow.top, viewportWidth * 2f, revealRow.bottom))
                } else {
                    for (bounds in lyricRevealClipRects(revealRow.pieces, progress,
                        revealRow.top, revealRow.bottom)) path.addRect(bounds)
                }
            }
            if (unstarted || completed) {
                drawText(caption.layout, if (completed) color
                    else color.copy(alpha = color.alpha * LYRIC_INACTIVE_TEXT_ALPHA), topLeft = position)
                continue
            }
            translate(position.x, position.y) {
                clipPath(path) { drawText(caption.layout, color) }
                clipPath(path, ClipOp.Difference) {
                    drawText(caption.layout, color.copy(alpha = color.alpha * LYRIC_INACTIVE_TEXT_ALPHA))
                }
            }
        }
    }
}

@Composable
private fun SyncedLyricLine(
    primaryTextAlpha: Float,
    line: LyricLine,
    translation: String?,
    translationAlpha: Float,
    normalTextStyle: TextStyle,
    translationTextStyle: TextStyle,
    romanizationTextStyle: TextStyle,
    emphasisColor: Color,
    alignmentProgress: State<Float>,
    lineAlignment: LyricAlignment?,
    translationMotion: LyricTranslationMotion,
    romanizationMotion: LyricTranslationMotion,
) {
    var textLayout by remember(line) { mutableStateOf<TextLayoutResult?>(null) }
    val verticalPadding = lyricTranslationPaddingDp(
        hasTranslation = translation != null || line.romanization != null,
        expansion = maxOf(
            if (translation != null) translationMotion.expansion.value else 0f,
            if (line.romanization != null) romanizationMotion.expansion.value else 0f,
        ),
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
                .animatedPlayerTextAlignment(textLayout, alignmentProgress,
                    endAligned = lineAlignment == LyricAlignment.END),
            style = normalTextStyle,
            color = emphasisColor.copy(alpha = emphasisColor.alpha * primaryTextAlpha),
            textAlign = if (lineAlignment == null) TextAlign.Start else TextAlign.Left,
            onTextLayout = { textLayout = it },
        )
        line.romanization?.let { romanization ->
            AuxiliaryLyricText(
                text = romanization,
                style = romanizationTextStyle,
                color = emphasisColor.copy(alpha = translationAlpha),
                alignmentProgress = alignmentProgress,
                lineAlignment = lineAlignment,
                motion = romanizationMotion,
                lyricFontScale = normalTextStyle.fontSize.value / LYRIC_PRIMARY_FONT_SIZE_SP,
            )
        }
        translation?.let { translation ->
            AuxiliaryLyricText(
                text = translation,
                style = translationTextStyle,
                color = emphasisColor.copy(alpha = translationAlpha),
                alignmentProgress = alignmentProgress,
                lineAlignment = lineAlignment,
                motion = translationMotion,
                lyricFontScale = normalTextStyle.fontSize.value / LYRIC_PRIMARY_FONT_SIZE_SP,
            )
        }
    }
}

private data class Syllable(val startTimeMs: Long, val endTimeMs: Long, val content: String)

private data class LyricGlyph(
    val bounds: Rect,
    val sourceIndex: Int,
    val sourceCount: Int,
    val visible: Boolean,
    val startDistance: Float,
    val endDistance: Float,
    val motionBounds: Rect,
    val glow: LyricGlyphGlow? = null,
)

private data class SyllableLayout(
    val syllable: Syllable,
    val textLayoutResult: TextLayoutResult,
    val effects: LyricWordEffects,
    val animationSource: Syllable,
    val position: Offset,
    val sourceOrigin: Offset,
    val width: Float,
    val height: Float,
    val firstBaseline: Float,
    val glyphs: List<LyricGlyph>,
    val raster: LyricTextRaster?,
)

private data class PreparedPhonetic(
    val layout: TextLayoutResult,
    val position: Offset,
    val tracks: List<LyricAuxiliaryText> = emptyList(),
    val sourceProgress: ((Long) -> Float)? = null,
) {
    val revealPath = Path()
    private val clusters = paragraphClusters(layout, layout.layoutInput.text.text)
    private val advances = paragraphAdvances(clusters, layout.layoutInput.text.length)
    private val clocks = buildMap<Int, LyricParagraphClock?> {
        var offset = 0
        for (track in tracks) {
            val trackOffset = offset
            val timings = lyricPhoneticTimings(track) { advances[trackOffset + it] }
            val clock = timings.takeIf { it.isNotEmpty() }?.let(::LyricParagraphClock)
            if (track.text.isNotEmpty()) {
                for (row in layout.getLineForOffset(offset)..layout.getLineForOffset(offset + track.text.lastIndex)) {
                    put(row, clock)
                }
            }
            offset += track.text.length + 1
        }
    }
    val revealRows = clusters.groupBy { it.row }
        .map { (row, clusters) ->
            PhoneticRevealRow(layout.getLineTop(row), layout.getLineBottom(row),
                clusters.filter { it.endDistance > it.startDistance }.map { cluster ->
                    LyricRevealPiece(
                        if (cluster.rtl) cluster.bounds.right else cluster.bounds.left,
                        if (cluster.rtl) cluster.bounds.left else cluster.bounds.right,
                        cluster.startDistance, cluster.endDistance,
                    )
                }, clocks[row], sourceProgress?.let { progress ->
                    { time -> progress(time) * advances.last() }
                })
        }
}

private data class PhoneticRevealRow(
    val top: Float, val bottom: Float, val pieces: List<LyricRevealPiece>, val clock: LyricParagraphClock?,
    val sourceDistance: ((Long) -> Float)? = null,
) {
    fun progressAt(time: Long): Float {
        val distance = clock?.distanceAt(time) ?: sourceDistance?.invoke(time) ?: return 1f
        val start = pieces.firstOrNull()?.startDistance ?: return 1f
        val end = pieces.lastOrNull()?.endDistance ?: return 1f
        return if (end > start) ((distance - start) / (end - start)).coerceIn(0f, 1f) else 1f
    }
}

private data class RowRenderData(
    val layouts: List<SyllableLayout>,
    val minX: Float,
    val maxX: Float,
    val width: Float,
    val layerBounds: Rect,
    val top: Float,
    val height: Float,
    val startDistance: Float,
    val endDistance: Float,
    val pieces: List<LyricRevealPiece>,
    val clock: LyricParagraphClock,
    val phonetics: List<PreparedPhonetic> = emptyList(),
) {
    val phoneticHeight = phonetics.maxOfOrNull { it.position.y + it.layout.size.height } ?: 0f

    fun gradient(activeAlpha: Float, fade: LyricFadeBounds, inactiveAlpha: Float): Brush {
        val first = pieces.first()
        val direction = if (first.endX > first.startX) 1f else -1f
        if (pieces.all { piece ->
                val expected = first.startX + (piece.startDistance - first.startDistance) * direction
                abs(piece.startX - expected) < 0.01f && (piece.endX - piece.startX) * direction > 0f
            }
        ) {
            return Brush.horizontalGradient(
                listOf(Color.White.copy(alpha = activeAlpha), Color.White.copy(alpha = inactiveAlpha)),
                startX = first.startX + (fade.startX - first.startDistance) * direction,
                endX = first.startX + (fade.endX - first.startDistance) * direction,
            )
        }
        val stops = lyricRevealMaskStops(pieces, fade, activeAlpha, inactiveAlpha)
        val left = stops.first().first
        val right = stops.last().first
        return Brush.horizontalGradient(
            colorStops = stops.map { (x, alpha) ->
                ((x - left) / (right - left)) to Color.White.copy(alpha = alpha)
            }.toTypedArray(),
            startX = left, endX = right,
        )
    }
}

private data class PreparedLyricLine(
    val rows: List<RowRenderData>,
    val heightPx: Int,
    val rasterBytes: Long,
    val phoneticGap: Float = 0f,
    val phoneticRowSpacing: Float = 0f,
    val standaloneRomanization: String? = null,
) {
    val hasInlinePhonetics = rows.any { it.phonetics.isNotEmpty() }
}

private fun PreparedLyricLine.visualOutsets(
    widthPx: Float,
    density: androidx.compose.ui.unit.Density,
    endAligned: Boolean,
): LayerOutsets {
    var addedHeight = 0f
    val bounds = rows.fold(Rect(0f, 0f, widthPx, heightPx.toFloat())) { bounds, row ->
        val centerOffset = playerTextAlignmentOffset(row.minX, row.maxX, widthPx, 1f, endAligned)
        val before = addedHeight
        if (row.phonetics.isNotEmpty()) addedHeight += row.phoneticHeight + phoneticGap
        bounds.lyricUnion(row.layerBounds.translate(Offset(0f, before)))
            .lyricUnion(row.layerBounds.translate(Offset(centerOffset, before)))
            .lyricUnion(row.layerBounds.translate(Offset(0f, addedHeight)))
            .lyricUnion(row.layerBounds.translate(Offset(centerOffset, addedHeight)))
            .also { if (row.phonetics.isNotEmpty()) addedHeight += phoneticRowSpacing }
    }
    return lyricLayerOutsets(bounds, widthPx, heightPx.toFloat(), density)
}

private data class ParagraphSource(val word: LyricWord, val start: Int, val end: Int)

private data class ParagraphCluster(
    val range: IntRange,
    val row: Int,
    val bounds: Rect,
    val rtl: Boolean,
    val startDistance: Float = 0f,
    val endDistance: Float = 0f,
)

private data class ParagraphCaption(
    val start: Int,
    val end: Int,
    val layout: TextLayoutResult,
    val tracks: List<LyricAuxiliaryText>,
)

/** Native shaping sees the complete sentence, never the authored timing boundaries. */
private fun paragraphClusters(
    layout: TextLayoutResult,
    text: String,
    layoutOffsets: IntArray? = null,
): List<ParagraphCluster> {
    val result = mutableListOf<ParagraphCluster>()
    fun visualOffset(offset: Int) = layoutOffsets?.get(offset) ?: offset
    for (range in lyricGraphemeRanges(text)) {
        val row = layout.getLineForOffset(visualOffset(range.first))
        val boxes = range.map { layout.getBoundingBox(visualOffset(it)) }
        val left = boxes.minOf { it.left }
        val right = boxes.maxOf { it.right }
        val visibleEnd = layout.getLineEnd(row, visibleEnd = true)
        val bounds = Rect(left, layout.getLineTop(row),
            if (visualOffset(range.first) >= visibleEnd) left else right, layout.getLineBottom(row))
        val previous = result.lastOrNull()
        val content = text.substring(range)
        val rtl = layout.getBidiRunDirection(visualOffset(range.first)) == ResolvedTextDirection.Rtl
        val joined = previous != null &&
            previous.rtl == rtl &&
            !text.substring(previous.range).any(Char::isWhitespace) && !content.any(Char::isWhitespace) &&
            lyricUsesJoinedGlyphs(content) && lyricUsesJoinedGlyphs(text.substring(previous.range))
        val overlaps = previous != null && bounds.width > 0f && previous.bounds.width > 0f &&
            minOf(previous.bounds.right, bounds.right) - maxOf(previous.bounds.left, bounds.left) > 0.5f
        if (previous != null && previous.row == row && previous.rtl == rtl &&
            (joined || overlaps || bounds.width <= 0f && !content.isBlank())) {
            result[result.lastIndex] = previous.copy(
                range = previous.range.first..range.last,
                bounds = previous.bounds.lyricUnion(bounds),
            )
        } else {
            result += ParagraphCluster(range, row, bounds, rtl)
        }
    }
    var distance = 0f
    return result.map { cluster ->
        val start = distance
        distance += cluster.bounds.width.coerceAtLeast(0f)
        cluster.copy(startDistance = start, endDistance = distance)
    }
}

private fun paragraphAdvances(clusters: List<ParagraphCluster>, textLength: Int): FloatArray =
    FloatArray(textLength + 1).also { advances ->
        for (cluster in clusters) for (offset in cluster.range.first..cluster.range.last + 1) {
            advances[offset] = cluster.startDistance + (cluster.endDistance - cluster.startDistance) *
                (offset - cluster.range.first) / (cluster.range.last + 1 - cluster.range.first)
        }
    }

/** Score safe breaks using shaped original widths and pronunciation occupancy. */
private fun annotatedParagraphText(
    text: String,
    sources: List<ParagraphSource>,
    annotations: LyricPhonetics?,
    measurer: TextMeasurer,
    style: TextStyle,
    phoneticStyle: TextStyle?,
    width: Float,
    languageTag: String?,
): LyricLayoutText {
    if (text.isEmpty()) return lyricLayoutText(text)
    val whole = measurer.measure(text, style, softWrap = false)
    val clusters = paragraphClusters(whole, text)
    val advances = paragraphAdvances(clusters, text.length)
    val captions = paragraphCaptions(clusters, sources, annotations, text, measurer, phoneticStyle)
    val captionRanges = captions.map { clusters[it.start].range.first..clusters[it.end].range.last }
    val separation = measurer.measure(" ", style, softWrap = false).size.width.toFloat()
    val safe = clusters.flatMap { listOf(it.range.first, it.range.last + 1) }.toSet()
    val locale = languageTag?.let(Locale::forLanguageTag) ?: Locale.getDefault()
    val breaks = lyricAnnotationLineBreaks(text, width, locale, safe, captionRanges) { start, end ->
        var required = advances[end] - advances[start]
        for ((index, caption) in captions.withIndex()) {
            val range = captionRanges[index]
            val from = maxOf(start, range.first)
            val to = minOf(end, range.last + 1)
            if (to <= from) continue
            val original = advances[range.last + 1] - advances[range.first]
            val fraction = if (original > 0f) (advances[to] - advances[from]) / original else 1f
            required += (caption.layout.size.width + separation - original).coerceAtLeast(0f) * fraction
        }
        required
    }
    return lyricLayoutText(text, breaks ?: intArrayOf())
}

private fun paragraphCaptions(
    clusters: List<ParagraphCluster>, sources: List<ParagraphSource>, annotations: LyricPhonetics?,
    text: String, measurer: TextMeasurer, style: TextStyle?,
    maxWidth: Int = Constraints.Infinity,
): List<ParagraphCaption> {
    if (annotations == null || style == null) return emptyList()
    data class CaptionRange(val start: Int, val end: Int, val tracks: List<LyricAuxiliaryText>)
    val ranges = sources.flatMapIndexed { index, source ->
        if (annotations.words.getOrNull(index) == null) return@flatMapIndexed emptyList()
        val touched = clusters.indices.filter { clusters[it].range.first < source.end && clusters[it].range.last >= source.start }
        val rows = touched.groupBy { clusters[it].row }.values.toList()
        val captions = annotations.wordAnnotationRows(index, rows.map { row ->
            text.substring(maxOf(source.start, clusters[row.first()].range.first),
                minOf(source.end, clusters[row.last()].range.last + 1))
        })
        rows.mapIndexedNotNull { row, indices ->
            captions[row].takeIf { tracks -> tracks.any { it.text.isNotBlank() } }?.let { tracks ->
                CaptionRange(indices.first(), indices.last(), tracks)
            }
        }
    }.sortedBy { it.start }
    val merged = mutableListOf<CaptionRange>()
    for (range in ranges) {
        val previous = merged.lastOrNull()
        if (previous != null && range.start <= previous.end) {
            merged[merged.lastIndex] = previous.copy(end = maxOf(previous.end, range.end),
                tracks = lyricMergePhoneticTracks(previous.tracks, range.tracks))
        } else merged += range
    }
    return merged.map { caption ->
        val tracks = caption.tracks
        val visibleTracks = tracks.filter { it.text.isNotBlank() }.distinctBy { it.text }
        ParagraphCaption(caption.start, caption.end,
            measurer.measure(visibleTracks.joinToString("\n") { it.text },
                style, softWrap = maxWidth != Constraints.Infinity,
                constraints = Constraints(maxWidth = maxWidth)),
            visibleTracks)
    }
}

/** Retain one paragraph raster and its original timing through lazy-item disposal. */
private class PreparedLyricLineCache(
    private val phoneticStyle: TextStyle? = null,
    private val languageTag: String? = null,
) {
    private data class Key(val line: LyricLine, val width: Float, val wordEffects: Boolean)
    private val entries = LinkedHashMap<Key, PreparedLyricLine>(32, 0.75f, true)
    private var rasterBytes = 0L

    fun prepare(
        line: LyricLine, width: Float, measurer: TextMeasurer, style: TextStyle,
        color: Color, density: androidx.compose.ui.unit.Density, direction: LayoutDirection,
        wordEffects: Boolean = true,
    ): PreparedLyricLine {
        val key = Key(line, width, wordEffects)
        synchronized(entries) { entries[key]?.let { return it } }
        var sourceOffset = 0
        val sources = line.animationWords().map { word ->
            val start = sourceOffset
            sourceOffset += word.text.length + if (word.hasTrailingSpace) 1 else 0
            ParagraphSource(word, start, sourceOffset)
        }
        val text = sources.joinToString("") { it.word.text + if (it.word.hasTrailingSpace) " " else "" }
        val annotations = lyricPhonetics(line)
        val inlineAnnotations = annotations.takeIf { phoneticStyle != null }
        val paragraphStyle = style.copy(lineHeightStyle = LyricLineHeightStyle)
        val bounded = width < 1_000_000f
        val available = width.toInt().coerceAtLeast(1)
        lateinit var layout: TextLayoutResult
        var clusters = emptyList<ParagraphCluster>()
        var captions = emptyList<ParagraphCaption>()
        var layoutText = lyricLayoutText(text)
        val separation = measurer.measure(" ", style).size.width.toFloat()
        // Pronunciation can reserve extra horizontal room, but original glyphs still come
        // from one contextual paragraph layout. Only supplied word annotations participate.
        fun measureParagraph(measureWidth: Int): Float {
            if (bounded && inlineAnnotations?.hasAnnotations == true) {
                layoutText = annotatedParagraphText(text, sources, inlineAnnotations, measurer, paragraphStyle,
                    phoneticStyle, measureWidth.toFloat(), line.language ?: languageTag)
            }
            layout = measurer.measure(layoutText.text, paragraphStyle,
                softWrap = layoutText.text == text,
                constraints = if (bounded) Constraints(maxWidth = measureWidth) else Constraints(),
                overflow = TextOverflow.Visible)
            clusters = paragraphClusters(layout, text, layoutText.layoutOffsets)
            captions = paragraphCaptions(clusters, sources, inlineAnnotations, text, measurer, phoneticStyle,
                if (bounded) available else Constraints.Infinity)
            val maxWidth = (0 until layout.lineCount).maxOfOrNull { row ->
                val extra = captions.filter { clusters[it.start].row == row }.sumOf { caption ->
                    val bounds = clusters.subList(caption.start, caption.end + 1).map { it.bounds }
                    val original = bounds.maxOf { it.right } - bounds.minOf { it.left }
                    (caption.layout.size.width + separation - original).coerceAtLeast(0f).toDouble()
                }.toFloat()
                layout.getLineRight(row) - layout.getLineLeft(row) + extra
            } ?: 0f
            return maxWidth
        }
        val initialRequiredWidth = measureParagraph(available)
        if (bounded && initialRequiredWidth > available + 0.5f) {
            // Measure indivisible glyphs in their paragraph context, not in isolation.
            val minimum = ceil(lyricGraphemeRanges(text).maxOfOrNull { range ->
                val boxes = range.map { layout.getBoundingBox(layoutText.layoutOffsets[it]) }
                boxes.maxOf { it.right } - boxes.minOf { it.left }
            } ?: 1f).toInt().coerceIn(1, available)
            lyricParagraphReflowWidth(available, minimum, ::measureParagraph)
        }
        val advances = paragraphAdvances(clusters, text.length)
        val clock = LyricParagraphClock(sources.map { source ->
            LyricTimedDistance(source.word.startTimeMs, source.word.endTimeMs, advances[source.start], advances[source.end])
        })
        val animationSources = sources.map { source ->
            Syllable(source.word.startTimeMs, source.word.endTimeMs,
                source.word.text + if (source.word.hasTrailingSpace) " " else "")
        }
        val sourceGraphemes = animationSources.map { lyricGraphemeRanges(it.content) }
        val sourceEffects = animationSources.map {
            lyricWordEffects(it.content, it.endTimeMs - it.startTimeMs,
                line.words.isNotEmpty() && wordEffects)
        }
        val effectScale = with(density) { lyricEffectGeometryScale(style.fontSize.toPx()) }
        var remainingBytes = 4L * 1024 * 1024
        val raster = prepareLyricTextRaster(layout, color, density, direction, remainingBytes)
        remainingBytes -= raster?.byteCount ?: 0L
        val padding = raster?.padding?.toFloat() ?: lyricGlyphOutsetPx(style.fontSize.value)
        val gap = with(density) { (4f * style.fontSize.value / LYRIC_PRIMARY_FONT_SIZE_SP).dp.toPx() }
        val rows = (0 until layout.lineCount).map { row ->
            val top = layout.getLineTop(row)
            val height = layout.getLineBottom(row) - top
            val indices = clusters.indices.filter { clusters[it].row == row }
            val rowCaptions = captions.filter { clusters[it.start].row == row }
                .sortedBy { caption -> clusters.subList(caption.start, caption.end + 1).minOf { it.bounds.left } }
            val shifts = mutableMapOf<Int, Float>()
            val preparedCaptions = mutableListOf<PreparedPhonetic>()
            var extra = 0f
            for (caption in rowCaptions) {
                val covered = clusters.subList(caption.start, caption.end + 1)
                val left = covered.minOf { it.bounds.left }
                val right = covered.maxOf { it.bounds.right }
                val addition = (caption.layout.size.width + separation - (right - left)).coerceAtLeast(0f)
                for (index in indices) {
                    if (clusters[index].bounds.left >= right) shifts[index] = (shifts[index] ?: 0f) + addition
                    else if (index in caption.start..caption.end) shifts[index] = (shifts[index] ?: 0f) + addition / 2f
                }
                preparedCaptions += PreparedPhonetic(caption.layout,
                    Offset(left + extra + (right - left + addition - caption.layout.size.width) / 2f, 0f),
                    caption.tracks, sourceProgress = { time ->
                        clock.progressBetween(time, clusters[caption.start].startDistance,
                            clusters[caption.end].endDistance)
                    })
                extra += addition
            }
            val nativeLeft = layout.getLineLeft(row)
            val nativeRight = layout.getLineRight(row)
            val rowWidth = nativeRight - nativeLeft + extra
            val rtl = text.isNotEmpty() && layout.getParagraphDirection(layout.getLineStart(row)) == ResolvedTextDirection.Rtl
            val baseShift = if (rtl && bounded && paragraphStyle.textAlign != TextAlign.Left)
                width - rowWidth - nativeLeft else 0f
            val phonetics = preparedCaptions.map { it.copy(position = it.position + Offset(baseShift, 0f)) }.toMutableList()
            val units = indices.filter { !text.substring(clusters[it].range).isBlank() }.map { index ->
                val cluster = clusters[index]
                val content = text.substring(cluster.range)
                val owners = sources.indices.filter { sources[it].start <= cluster.range.last && sources[it].end > cluster.range.first }
                val owner = owners.first()
                val source = if (owners.size == 1) animationSources[owner] else Syllable(
                    owners.minOf { sources[it].word.startTimeMs }, owners.maxOf { sources[it].word.endTimeMs },
                    owners.joinToString("") { animationSources[it].content })
                val joined = lyricUsesJoinedGlyphs(content)
                val sourceRanges = if (owners.size == 1) sourceGraphemes[owner] else lyricGraphemeRanges(source.content)
                val sourceIndex = sourceRanges.indexOfFirst {
                    cluster.range.first - sources[owner].start in it
                }.coerceAtLeast(0)
                val effects = if (owners.size == 1) sourceEffects[owner] else LyricWordEffects(
                    glow = owners.any { sourceEffects[it].glow },
                    scale = owners.any { sourceEffects[it].scale },
                )
                val left = cluster.bounds.left
                val right = cluster.bounds.right
                val origin = Offset(left, top)
                val bounds = Rect(
                    if (left <= nativeLeft) -padding else 0f,
                    if (row == 0) -padding else 0f,
                    right - left + if (right >= nativeRight) padding else 0f,
                    height + if (row == layout.lineCount - 1) padding else 0f,
                )
                val rawGlow = if (raster != null && effects.glow) {
                    prepareLyricGlyphGlow(raster, bounds.translate(origin),
                        wordMotion(0.5f, source.endTimeMs - source.startTimeMs).glowRadius * effectScale,
                        color, remainingBytes)
                } else null
                remainingBytes -= rawGlow?.byteCount ?: 0L
                val glow = rawGlow?.translated(-origin)
                val glyph = LyricGlyph(bounds, if (joined) 0 else sourceIndex,
                    if (joined) 1 else sourceRanges.size,
                    content.any { !it.isWhitespace() && !it.isPunctuation() },
                    cluster.startDistance, cluster.endDistance, Rect(0f, 0f, right - left, height), glow)
                SyllableLayout(source.copy(content = content), layout, effects, source,
                    Offset(left + baseShift + (shifts[index] ?: 0f), top), origin, right - left,
                    height, layout.getLineBaseline(row) - top, listOf(glyph), raster)
            }
            val pieces = indices.mapNotNull { index ->
                val cluster = clusters[index]
                if (cluster.endDistance <= cluster.startDistance) return@mapNotNull null
                val shift = baseShift + (shifts[index] ?: 0f)
                LyricRevealPiece(
                    (if (cluster.rtl) cluster.bounds.right else cluster.bounds.left) + shift,
                    (if (cluster.rtl) cluster.bounds.left else cluster.bounds.right) + shift,
                    cluster.startDistance, cluster.endDistance)
            }
            val minX = nativeLeft + baseShift
            val maxX = maxOf(minX + rowWidth, phonetics.maxOfOrNull { it.position.x + it.layout.size.width } ?: minX)
            val coverage = units.fold(Rect(minX, top, maxX, top + height)) { bounds, unit ->
                val glyph = unit.glyphs.single()
                val ink = glyph.glow?.let { glyph.bounds.lyricUnion(it.bounds) } ?: glyph.bounds
                val scale = if (unit.effects.scale && glyph.visible)
                    wordMotion(0.5f, unit.animationSource.endTimeMs - unit.animationSource.startTimeMs).scale else 1f
                bounds.lyricUnion(lyricGlyphVisualBounds(ink,
                    lyricCharacterPivot(Offset.Zero, unit.width, height), scale,
                    LYRIC_FLOAT_MAX_OFFSET_PX * effectScale).translate(unit.position))
            }
            val rowStart = advances[layoutText.sourceOffsets[layout.getLineStart(row)]]
            val rowEnd = advances[layoutText.sourceOffsets[layout.getLineEnd(row)]]
            RowRenderData(units, minX, maxX, maxX - minX, coverage, top, height,
                rowStart, rowEnd, pieces, clock, phonetics)
        }
        val annotationHeight = rows.sumOf { row ->
            if (row.phonetics.isEmpty()) 0.0 else (row.phoneticHeight + gap).toDouble()
        }.toFloat()
        val rowSpacing = if (annotationHeight > 0f) gap * 2f else 0f
        val prepared = PreparedLyricLine(rows, ceil(layout.size.height + annotationHeight + rowSpacing *
            rows.dropLast(1).count { it.phonetics.isNotEmpty() }).toInt(),
            4L * 1024 * 1024 - remainingBytes, gap, rowSpacing, annotations.standaloneRomanization)
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

internal fun shouldUseWordAnimation(content: String, durationMs: Long): Boolean =
    lyricWordEffects(content, durationMs).scale

internal const val LYRIC_FLOAT_MAX_OFFSET_PX = 4f

private fun Char.isPunctuation(): Boolean = when (Character.getType(this)) {
    Character.CONNECTOR_PUNCTUATION.toInt(), Character.DASH_PUNCTUATION.toInt(),
    Character.START_PUNCTUATION.toInt(), Character.END_PUNCTUATION.toInt(),
    Character.INITIAL_QUOTE_PUNCTUATION.toInt(), Character.FINAL_QUOTE_PUNCTUATION.toInt(),
    Character.OTHER_PUNCTUATION.toInt() -> true
    else -> false
}

private fun DrawScope.drawLyricsLine(
    rows: List<RowRenderData>,
    canvasWidth: Float,
    alignmentProgress: State<Float>,
    endAligned: Boolean,
    positionMs: Long,
    color: Color,
    activeAlpha: Float,
    effectScale: Float,
    fadeWidthPx: Float,
    glyphMotionEnabled: Boolean,
    inactiveAlpha: Float = LYRIC_INACTIVE_TEXT_ALPHA,
    activeOpacityLimit: Float = 1f,
    usesWordProgress: Boolean = true,
    primaryTextAlpha: Float = 1f,
) {
    val motionStrength = lyricLineMotionStrength(activeAlpha)
    val foregroundAlpha = minOf(activeAlpha, activeOpacityLimit)
    rows.forEach { row ->
        if (row.layouts.isEmpty()) return@forEach
        val width = minOf(fadeWidthPx, row.endDistance - row.startDistance).coerceAtLeast(1f)
        val distance = row.clock.distanceAt(positionMs)
        val fade = lyricFadeBounds(row.startDistance, row.endDistance, distance, width)
        val offset = playerTextAlignmentOffset(row.minX, row.maxX, canvasWidth, alignmentProgress.value, endAligned)
        val uniformAlpha = when {
            distance <= row.startDistance -> inactiveAlpha
            distance >= row.endDistance -> foregroundAlpha
            foregroundAlpha == inactiveAlpha -> inactiveAlpha
            else -> null
        }
        withTransform({ translate(left = offset) }) {
            if (!usesWordProgress) {
                drawRowText(row.layouts, color, positionMs, 0f, effectScale, primaryTextAlpha,
                    fade, glyphMotionEnabled = false)
            } else if (uniformAlpha != null) {
                drawRowText(row.layouts, color, positionMs, motionStrength, effectScale, uniformAlpha,
                    fade, glyphMotionEnabled)
            } else {
                drawIntoCanvas { canvas ->
                    canvas.saveLayer(row.layerBounds, LyricLayerPaint)
                    drawRowText(row.layouts, color, positionMs, motionStrength, effectScale, 1f,
                        fade, glyphMotionEnabled)
                    drawRect(
                        brush = row.gradient(foregroundAlpha, fade, inactiveAlpha),
                        topLeft = row.layerBounds.topLeft,
                        size = row.layerBounds.size,
                        blendMode = BlendMode.DstIn,
                    )
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

internal fun WordMotion.withLineStrength(strength: Float, enabled: Boolean = true): WordMotion {
    val amount = if (enabled) strength.coerceIn(0f, 1f) else 0f
    return copy(
        scale = 1f + (scale - 1f) * amount,
        offsetYPx = offsetYPx * amount,
        glowAlpha = glowAlpha * amount,
    )
}

private fun linePixelPosition(row: RowRenderData, positionMs: Long): Float {
    return lyricRevealPosition(row.pieces, row.clock.distanceAt(positionMs))
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
        val untouched = fade.endX <= layout.glyphs.first().startDistance
        val revealed = fade.startX >= layout.glyphs.last().endDistance
        if ((!effectsActive && (untouched || revealed)) || motionStrength == 0f) {
            val offset = if (revealed && glyphMotionEnabled)
                -LYRIC_FLOAT_MAX_OFFSET_PX * effectScale * motionStrength else 0f
            drawLyricRasterOrText(layout.textLayoutResult, layout.raster, color,
                layout.position - layout.sourceOrigin + Offset(0f, offset),
                layout.glyphs.single().bounds.translate(layout.sourceOrigin), alpha)
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
                val glyphScale = if (!untouched && layout.effects.scale && glyph.visible && glyphMotionEnabled)
                    raw.scale else 1f
                val revealProgress = lyricGlyphRevealProgress(
                    (glyph.startDistance + glyph.endDistance) / 2f -
                        (glyph.endDistance - glyph.startDistance) * glyphScale / 2f,
                    (glyph.startDistance + glyph.endDistance) / 2f +
                        (glyph.endDistance - glyph.startDistance) * glyphScale / 2f, fade)
                val motion = raw.copy(
                    scale = glyphScale,
                    offsetYPx = if (glyphMotionEnabled) lyricRevealFloatOffset(revealProgress) * effectScale else 0f,
                    glowAlpha = if (!untouched && layout.effects.glow && glyph.visible) raw.glowAlpha else 0f,
                ).withLineStrength(motionStrength, glyphMotionEnabled)
                val position = layout.position + Offset(0f, motion.offsetYPx)
                withTransform({
                    scale(motion.scale, motion.scale, lyricCharacterPivot(
                        position + Offset(glyph.motionBounds.left, 0f), glyph.motionBounds.width,
                        layout.height))
                }) {
                    glyph.glow?.let { glow ->
                        if (motion.glowAlpha > 0.001f) glow.draw(this, position, motion.glowAlpha * alpha)
                    }
                    drawLyricRasterOrText(layout.textLayoutResult, layout.raster, color,
                        position - layout.sourceOrigin, glyph.bounds.translate(layout.sourceOrigin), alpha)
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
    if (positionMs <= startTimeMs) return 0f
    if (positionMs >= endTimeMs) return 1f
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
