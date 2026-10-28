package com.melox.player.ui.screen.playback

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.melox.player.model.LyricsTranslationLanguage
import com.melox.player.model.LyricAnimationMode
import com.melox.player.model.LyricLine
import com.melox.player.model.LyricWord
import com.melox.player.model.LyricsDocument
import com.melox.player.model.LyricsUiState
import com.melox.player.model.PlaybackUiState
import kotlin.math.abs
import kotlin.math.exp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal const val COMPACT_LYRIC_HANDOFF_MS = 500
internal const val COMPACT_LYRIC_SCROLL_MS = 600
internal const val COMPACT_LYRIC_INACTIVE_ALPHA = 0.45f
private val CompactLyricScrollEasing = Easing { 1f - (1f - it) * (1f - it) }
private val CompactArtworkMotion = spring<Dp>(dampingRatio = 1f, stiffness = 350f)
private val CompactLyricHorizontalEdgeFadeWidth = 6.dp

internal fun compactLyricsDocument(lyrics: LyricsUiState, mediaId: String?): LyricsDocument? {
    val available = lyrics as? LyricsUiState.Available ?: return null
    return available.document.takeIf { mediaId != null && available.mediaId == mediaId }
}

internal fun compactPrimaryLines(document: LyricsDocument?): List<LyricLine> =
    document?.lines.orEmpty().filter { !it.isBackground && it.displayText.isNotBlank() }

@Composable
internal fun rememberCompactArtworkLyricsPresence(lyrics: LyricsUiState): Boolean {
    var retainedPresence by remember { mutableStateOf(true) }
    val present = when (lyrics) {
        LyricsUiState.Loading -> retainedPresence
        LyricsUiState.Unavailable -> false
        is LyricsUiState.Available -> remember(lyrics.document) {
            compactPrimaryLines(lyrics.document).isNotEmpty()
        }
    }
    SideEffect { retainedPresence = present }
    return present
}

internal fun compactLyricAuxiliaryText(
    line: LyricLine,
    showTranslation: Boolean,
    showRomanization: Boolean,
    translationLanguage: LyricsTranslationLanguage = LyricsTranslationLanguage.AUTO,
    appLanguageTag: String = "en",
): List<String> = listOfNotNull(
    lyricPhonetics(line).standaloneRomanization?.takeIf { showRomanization && it.isNotBlank() },
    line.translationFor(translationLanguage, appLanguageTag)?.takeIf { showTranslation && it.isNotBlank() },
).flatMap { caption -> caption.lines().map(String::trim).filter(String::isNotEmpty) }

internal fun compactLyricSingleLine(line: LyricLine): LyricLine {
    fun String.singleLine() = replace("\r\n", " ").replace('\r', ' ').replace('\n', ' ')
    fun LyricWord.singleLine() = copy(
        text = text.singleLine(),
        ruby = ruby.map { it.copy(text = it.text.singleLine()) },
    )
    return line.copy(
        text = line.text?.singleLine(),
        words = line.words.map { it.singleLine() },
        romanizations = line.romanizations.map { track ->
            track.copy(text = track.text.singleLine(), words = track.words.map { it.singleLine() })
        },
    )
}

/** SALT spaces adjacent entries by their actual half-heights plus a shared gap. */
internal fun compactLyricRowCenters(heights: List<Float>, gap: Float): FloatArray {
    var top = 0f
    return FloatArray(heights.size) { index ->
        val center = top + heights[index] / 2f
        top += heights[index] + gap
        center
    }
}

/** Keep the completed primary line through instrumental gaps, without showing future lyrics. */
internal fun compactLyricIndexAtPosition(lines: List<LyricLine>, positionMs: Long): Int {
    var low = 0
    var high = lines.lastIndex
    var index = -1
    while (low <= high) {
        val middle = (low + high).ushr(1)
        if (lines[middle].startTimeMs <= positionMs) {
            index = middle
            low = middle + 1
        } else {
            high = middle - 1
        }
    }
    return index
}

/** SALT keeps the singing cursor near the viewport center, clamped at either text edge. */
internal fun compactLyricScrollTarget(
    viewportWidth: Float,
    textWidth: Float,
    cursorX: Float,
    centered: Boolean,
    endPaddingPx: Float = 0f,
): Float = if (textWidth > viewportWidth) {
    (viewportWidth / 2f - cursorX).coerceIn(viewportWidth - textWidth - endPaddingPx.coerceAtLeast(0f), 0f)
} else if (centered) {
    (viewportWidth - textWidth) / 2f
} else {
    0f
}

internal class CompactLyricScroll {
    private var offset = Float.NaN
    private var positionMs = 0L
    private var viewportWidth = Float.NaN
    private var textWidth = Float.NaN

    fun update(target: Float, positionMs: Long, viewportWidth: Float, textWidth: Float,
        smooth: Boolean = true,
    ): Float {
        val elapsed = positionMs - this.positionMs
        val sameGeometry = abs(this.viewportWidth - viewportWidth) < 0.5f &&
            abs(this.textWidth - textWidth) < 0.5f
        offset = if (smooth && offset.isFinite() && sameGeometry && elapsed == 0L) {
            offset
        } else if (smooth && offset.isFinite() && sameGeometry && elapsed in 1..1_000) {
            offset + (target - offset) * (1f - exp(-elapsed / 250f))
        } else {
            target
        }
        this.positionMs = positionMs
        this.viewportWidth = viewportWidth
        this.textWidth = textWidth
        return offset
    }
}

internal data class CompactArtworkLayout(
    val artworkTop: Dp,
    val artworkHeight: Dp,
    val lyricsTop: Dp,
    val lyricsHeight: Dp,
)

internal fun compactArtworkLayout(
    height: Dp,
    preferredArtworkSize: Dp,
    progressGap: Dp,
    reserveLyrics: Boolean = true,
): CompactArtworkLayout {
    val available = height.coerceAtLeast(0.dp)
    if (!reserveLyrics) {
        val artworkHeight = (preferredArtworkSize + PLAYER_ARTWORK_VERTICAL_FOOTPRINT_EXPANSION)
            .coerceIn(0.dp, available)
        return CompactArtworkLayout((available - artworkHeight) / 2f, artworkHeight, available, 0.dp)
    }
    val artworkSize = minOf(preferredArtworkSize, available - 44.dp).coerceAtLeast(0.dp)
    val artworkHeight = (artworkSize + PLAYER_ARTWORK_VERTICAL_FOOTPRINT_EXPANSION)
        .coerceAtMost(available)
    // SALT reserves 68 dp, or 24 dp when constrained, and divides spare height into thirds.
    val captionHeight = (if (available - artworkSize > 86.dp) 68.dp else 24.dp)
        .coerceAtMost(available)
    // Anchor to playing artwork bounds so pause scaling cannot move the lyric region.
    val playingInset = (artworkHeight - artworkSize) / 2f
    val artworkTop = ((available - artworkSize - captionHeight) / 3f).coerceAtLeast(0.dp) - playingInset
    val artworkBottom = artworkTop + artworkHeight - playingInset
    val lyricsTop = ((artworkBottom + available + progressGap - captionHeight) / 2f)
        .coerceIn(0.dp, available - captionHeight)
    return CompactArtworkLayout(artworkTop, artworkHeight, lyricsTop, captionHeight)
}

internal fun compactLyricLineHeight(style: TextStyle): TextUnit =
    if (style.lineHeight == TextUnit.Unspecified) style.fontSize * 1.4f else style.lineHeight

@Composable
private fun rememberCompactLyricTime(
    document: LyricsDocument,
    playback: PlaybackUiState,
    active: Boolean,
    previewPositionMs: Long?,
): () -> Long {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val time = key(
        playback.currentItem?.mediaId,
        playback.playbackIteration,
        playback.positionDiscontinuityRevision,
    ) {
        rememberSmoothLyricTimeProvider(
            document = document,
            positionMs = playback.positionMs,
            positionUpdateElapsedRealtimeMs = playback.positionUpdateElapsedRealtimeMs,
            playbackIteration = playback.playbackIteration,
            playbackSpeed = playback.playbackSpeed,
            isPlaying = playback.isPlaying && previewPositionMs == null,
            active = active && lifecycleState.isAtLeast(Lifecycle.State.STARTED),
            positionDiscontinuityRevision = playback.positionDiscontinuityRevision,
            seekRequestKey = if (previewPositionMs != null) 1 else 0,
            seekPositionMs = previewPositionMs ?: playback.positionMs,
        )
    }
    val preview = rememberUpdatedState(previewPositionMs)
    return remember(time) { { preview.value ?: time() } }
}

@Composable
internal fun MiniPlayerLyrics(
    document: LyricsDocument?,
    playback: PlaybackUiState,
    artist: String,
    style: TextStyle,
    color: Color,
    active: Boolean,
    animationMode: LyricAnimationMode,
    modifier: Modifier = Modifier,
    textStartPadding: Dp = 0.dp,
) {
    val measurer = rememberTextMeasurer()
    val artistLayout = measurer.measure(artist, style, softWrap = false, maxLines = 1)
    val density = LocalDensity.current
    val lineHeight = with(density) { artistLayout.size.height.toDp() }
    val textBaseline = with(density) { artistLayout.firstBaseline.toDp() }
    val lines = remember(document) { compactPrimaryLines(document) }
    val time = if (document != null) {
        rememberCompactLyricTime(document, playback, active, null)
    } else {
        val position = rememberUpdatedState(playback.positionMs)
        remember(position) { { position.value } }
    }
    val index by remember(lines, time) { derivedStateOf { compactLyricIndexAtPosition(lines, time()) } }
    val hasTimedWords = remember(lines) { lines.any { it.words.isNotEmpty() } }
    key(playback.currentItem?.mediaId) {
        AnimatedContent(
            targetState = lines.getOrNull(index),
            modifier = modifier.height(lineHeight).clipToBounds(),
            transitionSpec = {
                if (active) {
                    (slideInVertically(tween(COMPACT_LYRIC_HANDOFF_MS)) { it } togetherWith
                        slideOutVertically(tween(COMPACT_LYRIC_HANDOFF_MS)) { -it }).using(null)
                } else {
                    (EnterTransition.None togetherWith ExitTransition.None).using(null)
                }
            },
            label = "miniLyricHandoff",
        ) { line ->
            if (line == null) {
                Text(artist, modifier = Modifier.fillMaxWidth().padding(start = textStartPadding), style = style, color = color,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                CompactTimedLyricLine(line, style, color, time, animationMode, hasTimedWords,
                    modifier = Modifier.fillMaxWidth().height(lineHeight),
                    textStartPadding = textStartPadding,
                    textBaseline = textBaseline,
                    resetKey = playback.playbackIteration to playback.positionDiscontinuityRevision)
            }
        }
    }
}

@Composable
internal fun PortraitArtworkWithLyrics(
    lyrics: LyricsUiState,
    playback: PlaybackUiState,
    enabled: Boolean,
    active: Boolean,
    showTranslation: Boolean,
    showRomanization: Boolean,
    centerLyrics: Boolean,
    animationMode: LyricAnimationMode,
    lyricFontWeight: Int,
    contentWidth: Dp,
    preferredArtworkSize: Dp,
    progressGap: Dp,
    color: Color,
    artworkContent: @Composable (Dp) -> Unit,
    modifier: Modifier = Modifier,
    activeLyricAlpha: Float = 1f,
    lyricsVisible: Boolean = true,
    previewPositionMs: Long? = null,
    translationLanguage: LyricsTranslationLanguage = LyricsTranslationLanguage.AUTO,
) {
    if (!enabled) {
        BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) { artworkContent(maxHeight) }
        return
    }
    val incomingDocument = compactLyricsDocument(lyrics, playback.currentItem?.mediaId)
    val incomingLyrics = when {
        !lyricsVisible -> LyricsUiState.Unavailable
        incomingDocument != null -> lyrics
        lyrics == LyricsUiState.Unavailable -> LyricsUiState.Unavailable
        else -> LyricsUiState.Loading
    }
    val reserveLyrics = rememberCompactArtworkLyricsPresence(incomingLyrics)
    val displayedLyrics = rememberRetainedLyricDocument(incomingLyrics)
    val lyricFontFamily = rememberLyricFontFamily(lyricFontWeight)
    val primaryStyle = MiuixTheme.textStyles.footnote1.copy(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFamily = lyricFontFamily,
        fontWeight = FontWeight(lyricFontWeight.coerceIn(1, 1000)),
        fontSynthesis = FontSynthesis.None,
        lineHeightStyle = LyricLineHeightStyle,
    )
    val auxiliaryStyle = primaryStyle.copy(fontSize = 12.sp, lineHeight = 16.sp)
    BoxWithConstraints(modifier = modifier) {
        val geometry = compactArtworkLayout(maxHeight, preferredArtworkSize, progressGap)
        val artworkTarget = if (reserveLyrics) geometry else {
            compactArtworkLayout(maxHeight, preferredArtworkSize, progressGap, reserveLyrics = false)
        }
        val artworkTop by animateDpAsState(artworkTarget.artworkTop, CompactArtworkMotion,
            label = "compactArtworkTop")
        val artworkHeight by animateDpAsState(artworkTarget.artworkHeight, CompactArtworkMotion,
            label = "compactArtworkHeight")
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().height(artworkHeight)
                .offset(y = artworkTop),
            contentAlignment = Alignment.Center,
        ) { artworkContent(maxHeight) }
        LyricContentTransition(
            // The artwork surface has no empty-state message to transition.
            lyrics = displayedLyrics ?: LyricsUiState.Loading,
            modifier = Modifier.align(Alignment.TopCenter).width(contentWidth)
                .expandLeftForMarquee(CompactLyricHorizontalEdgeFadeWidth)
                .height(geometry.lyricsHeight).offset(y = geometry.lyricsTop),
        ) { frame, _, target ->
            val document = (frame as? LyricsUiState.Available)?.document
            val lines = remember(document) { compactPrimaryLines(document) }
            val currentDocument = target && frame == incomingLyrics
            var retainedPlayback by remember(frame) { mutableStateOf(playback) }
            if (currentDocument) SideEffect { retainedPlayback = playback }
            val renderPlayback = if (currentDocument) playback else retainedPlayback.copy(isPlaying = false)
            val renderActive = active && currentDocument
            if (document != null && lines.isNotEmpty()) {
                val time = rememberCompactLyricTime(document, renderPlayback, renderActive,
                    previewPositionMs.takeIf { currentDocument })
                val frozenTime = remember(frame, currentDocument) {
                    if (currentDocument) null else time()
                }
                val renderTime = remember(time, frozenTime) {
                    if (frozenTime == null) time else { { frozenTime } }
                }
                CompactLyricsViewport(
                    lines, renderPlayback, renderTime, renderActive, currentDocument && previewPositionMs != null,
                    primaryStyle, auxiliaryStyle, showTranslation, showRomanization, centerLyrics, animationMode, color,
                    translationLanguage = translationLanguage,
                    activeLyricAlpha = activeLyricAlpha,
                    modifier = Modifier.fillMaxWidth().height(geometry.lyricsHeight),
                )
            }
        }
    }
}

@Composable
private fun CompactLyricsViewport(
    lines: List<LyricLine>,
    playback: PlaybackUiState,
    time: () -> Long,
    active: Boolean,
    previewing: Boolean,
    primaryStyle: TextStyle,
    auxiliaryStyle: TextStyle,
    showTranslation: Boolean,
    showRomanization: Boolean,
    centered: Boolean,
    animationMode: LyricAnimationMode,
    color: Color,
    modifier: Modifier = Modifier,
    activeLyricAlpha: Float = 1f,
    translationLanguage: LyricsTranslationLanguage = LyricsTranslationLanguage.AUTO,
) {
    val index by remember(lines, time) { derivedStateOf { compactLyricIndexAtPosition(lines, time()) } }
    val focus = index.coerceAtLeast(0)
    val center = remember(lines) { Animatable(focus.toFloat()) }
    var iteration by remember(lines) { mutableLongStateOf(playback.playbackIteration) }
    var discontinuity by remember(lines) { mutableLongStateOf(playback.positionDiscontinuityRevision) }
    LaunchedEffect(focus, active, previewing, playback.playbackIteration, playback.positionDiscontinuityRevision) {
        val reset = iteration != playback.playbackIteration ||
            discontinuity != playback.positionDiscontinuityRevision
        iteration = playback.playbackIteration
        discontinuity = playback.positionDiscontinuityRevision
        if (reset || !active || previewing || abs(center.value - focus) > 1.5f) center.snapTo(focus.toFloat())
        else center.animateTo(focus.toFloat(), tween(COMPACT_LYRIC_SCROLL_MS, easing = CompactLyricScrollEasing))
    }
    val density = LocalDensity.current
    val appLanguageTag = LocalConfiguration.current.locales[0].toLanguageTag()
    val visibleRows = (focus - 2).coerceAtLeast(0)..(focus + 2).coerceAtMost(lines.lastIndex)
    val auxiliary = remember(lines, visibleRows, showTranslation, showRomanization, translationLanguage, appLanguageTag) {
        visibleRows.map { row ->
            compactLyricAuxiliaryText(lines[row], showTranslation, showRomanization, translationLanguage, appLanguageTag)
        }
    }
    val measurer = rememberTextMeasurer()
    val rowHeights = visibleRows.map { row ->
        key(row) {
            val line = lines[row]
            val captions = auxiliary[row - visibleRows.first]
            remember(line, captions, primaryStyle, auxiliaryStyle, showRomanization, density, measurer) {
                val singleLine = compactLyricSingleLine(line)
                val original = singleLine.animationWords().joinToString("") {
                    it.text + if (it.hasTrailingSpace) " " else ""
                }
                val primaryHeight = measurer.measure(original, primaryStyle.copy(lineHeightStyle = LyricLineHeightStyle),
                    softWrap = false).size.height
                val auxiliaryHeight = captions.sumOf { caption ->
                    measurer.measure(caption, auxiliaryStyle.copy(lineHeightStyle = LyricLineHeightStyle),
                        softWrap = false).size.height
                }
                val annotationHeight = if (showRomanization) {
                    compactLyricAnnotationHeight(singleLine, primaryStyle, auxiliaryStyle, measurer, density)
                } else 0
                with(density) { (primaryHeight + annotationHeight + auxiliaryHeight).toDp() }
            }
        }
    }
    val rowCenters = remember(rowHeights, density) {
        with(density) { compactLyricRowCenters(rowHeights.map { it.toPx() }, 6.dp.toPx()) }
    }
    val edgeFade = with(density) { 4.dp.toPx() }
    val horizontalFade = with(density) { CompactLyricHorizontalEdgeFadeWidth.toPx() }
    val auxiliaryColor = color.copy(alpha = lyricTranslationAlpha(1f))
    val hasTimedWords = remember(lines) { lines.any { it.words.isNotEmpty() } }
    BoxWithConstraints(
        modifier = modifier.clipToBounds().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val edge = (edgeFade / size.height.coerceAtLeast(1f)).coerceAtMost(0.5f)
                drawRect(Brush.verticalGradient(0f to Color.Transparent, edge to Color.Black,
                    1f - edge to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
                val horizontalEdge = (horizontalFade / size.width.coerceAtLeast(1f)).coerceIn(0f, 0.18f)
                drawRect(Brush.horizontalGradient(0f to Color.Transparent, horizontalEdge to Color.Black,
                    1f - horizontalEdge to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
            },
    ) {
        val viewportHeight = with(density) { maxHeight.toPx() }
        for (row in visibleRows) {
            key(row) {
                val localRow = row - visibleRows.first
                val line = lines[row]
                val focused = row == index
                val rowColor = color.copy(alpha = color.alpha * if (focused) 1f else COMPACT_LYRIC_INACTIVE_ALPHA)
                Column(
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(Alignment.Top, unbounded = true)
                        .height(rowHeights[localRow])
                        .graphicsLayer {
                            val visibleCenter = if (abs(center.value - focus) > 1.5f) focus.toFloat() else center.value
                            val lower = (visibleCenter.toInt() - visibleRows.first).coerceIn(rowCenters.indices)
                            val upper = (lower + 1).coerceAtMost(rowCenters.lastIndex)
                            val centerPosition = rowCenters[lower] +
                                (rowCenters[upper] - rowCenters[lower]) * (visibleCenter - lower - visibleRows.first)
                            translationY = viewportHeight / 2f + rowCenters[localRow] - centerPosition -
                                with(density) { rowHeights[localRow].toPx() } / 2f
                        },
                    horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
                    verticalArrangement = Arrangement.Top,
                ) {
                    CompactTimedLyricLine(line, primaryStyle, rowColor, time, animationMode, hasTimedWords,
                        modifier = Modifier.fillMaxWidth(), centered = centered, focused = focused,
                        activeOpacityLimit = if (focused) activeLyricAlpha else 1f,
                        textStartPadding = CompactLyricHorizontalEdgeFadeWidth,
                        textEndPadding = CompactLyricHorizontalEdgeFadeWidth,
                        phoneticStyle = auxiliaryStyle.takeIf { showRomanization },
                        resetKey = playback.playbackIteration to playback.positionDiscontinuityRevision)
                    auxiliary[localRow].forEach { text ->
                        CompactAuxiliaryLine(line, text, auxiliaryStyle, auxiliaryColor, time, focused, centered)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactAuxiliaryLine(line: LyricLine, text: String, style: TextStyle,
    color: Color, time: () -> Long, focused: Boolean, centered: Boolean,
) {
    val auxiliary = remember(line, text) { line.copy(text = text, words = emptyList()) }
    CompactTimedLyricLine(auxiliary, style, color, time, LyricAnimationMode.NEVER, false,
        modifier = Modifier.fillMaxWidth(), centered = centered, focused = focused,
        textStartPadding = CompactLyricHorizontalEdgeFadeWidth,
        textEndPadding = CompactLyricHorizontalEdgeFadeWidth)
}
