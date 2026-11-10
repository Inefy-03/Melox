package com.melox.player.ui.screen.playback

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.withoutVisualEffect
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Rect
import com.melox.player.model.LyricsTranslationLanguage
import com.melox.player.model.LyricAnimationMode
import com.melox.player.R
import com.melox.player.data.library.ArtistGroup
import com.melox.player.data.library.displayArtistName
import com.melox.player.model.MusicTrack
import com.melox.player.model.LyricsUiState
import com.melox.player.model.PlaybackMode
import com.melox.player.model.PlaybackBackgroundStyle
import com.melox.player.model.PlaybackQueueItem
import com.melox.player.model.PlaybackUiState
import com.melox.player.model.withTrackMetadata
import com.melox.player.ui.isMiuixWideLayout
import com.melox.player.ui.component.liquid.rememberCombinedBackdrop
import com.melox.player.ui.component.library.PlaybackArtworkFrame
import com.melox.player.ui.component.library.PLAYBACK_ARTWORK_SHADOW_BLUR_RADIUS
import com.melox.player.ui.component.library.TrackActionsOverlay
import com.melox.player.ui.component.library.formatDuration
import com.melox.player.ui.component.playback.BlurredArtworkBackground
import com.melox.player.ui.component.playback.DynamicFlowBackground
import com.melox.player.ui.component.playback.DynamicFlowBackgroundState
import com.melox.player.ui.component.playback.PLAYER_FULL_ARTWORK_CORNER_RADIUS
import com.melox.player.ui.component.playback.PLAYER_FULL_ARTWORK_REQUEST_SIZE
import com.melox.player.ui.component.playback.PLAYER_TRACK_ARTWORK_CROSSFADE_DURATION_MILLIS
import com.melox.player.ui.component.playback.PLAYER_TRACK_ARTWORK_CROSSFADE_EASING
import com.melox.player.ui.component.playback.rememberPlaybackArtworkResource
import com.melox.player.ui.component.playback.playerControlIconTransition
import com.melox.player.ui.component.playback.recordPlayerLayer
import com.melox.player.ui.component.playback.WeightedCrossfadeFrame
import com.melox.player.ui.component.playback.weightedCrossfadeFrames
import com.melox.player.ui.component.playback.rememberPlayerSheetVerticalDragModifier
import com.melox.player.ui.component.playback.artworkInsetRect
import com.melox.player.ui.component.playback.fittedArtworkRect
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.BasicComponent
import com.melox.player.ui.component.os4.Icon
import com.melox.player.ui.component.os4.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Playlist
import top.yukonga.miuix.kmp.icon.extended.Tune
import com.melox.player.ui.component.os4.OverlayBottomSheet
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.melox.player.ui.component.overScrollVertical

@Composable
internal fun PlayerContentPager(
    pagerState: PagerState,
    lyrics: LyricsUiState,
    positionMs: Long,
    positionUpdateElapsedRealtimeMs: Long,
    playbackIteration: Long,
    playbackSpeed: Float,
    previewPositionMs: Long?,
    isPlaying: Boolean,
    lyricsActive: Boolean,
    onSeek: (Long) -> Unit,
    contentWidth: Dp,
    controlColor: Color,
    emphasisControlColor: Color,
    lyricsPagingEnabled: Boolean,
    lyricFontScale: Float,
    lyricFontWeight: Int,
    lyricAnimationMode: LyricAnimationMode,
    lyricBlurEnabled: Boolean,
    centerLyrics: Boolean,
    lyricCenterOffsetY: Dp,
    showLyricsTranslation: Boolean,
    showBottomFade: Boolean,
    resumeFollowRequestKey: Int,
    seekRequestKey: Int,
    seekPositionMs: Long,
    artworkContent: @Composable (overscrollModifier: Modifier) -> Unit,
    modifier: Modifier = Modifier,
    currentMediaId: String? = (lyrics as? LyricsUiState.Available)?.mediaId,
    activeLyricAlpha: Float = 1f,
    showLyricsRomanization: Boolean = true,
    lyricsTranslationLanguage: LyricsTranslationLanguage = LyricsTranslationLanguage.AUTO,
    playerActive: Boolean = true,
    positionDiscontinuityRevision: Long = 0L,
    lyricsBottomContent: (@Composable (Backdrop?) -> Unit)? = null,
    lyricsBackground: Backdrop? = null,
    lyricsBackdrop: LayerBackdrop? = null,
    artworkAlignment: Alignment = Alignment.Center,
    lyricsViewportTopInset: Dp = 0.dp,
    lyricsViewportBottomInset: Dp = 0.dp,
    immersiveMode: Boolean = false,
) {
    val immersiveAlpha = playerChromeAlpha(immersiveMode)
    val overscrollEffect = rememberOverscrollEffect()
    val pagerOverscroll = remember(overscrollEffect) {
        overscrollEffect?.let(::PlayerPagerOverscroll)
    }
    val eventOverscrollEffect = remember(overscrollEffect) {
        overscrollEffect?.withoutVisualEffect()
    }
    HorizontalPager(
        state = pagerState,
        userScrollEnabled = lyricsPagingEnabled,
        overscrollEffect = eventOverscrollEffect,
        modifier = modifier.clipToBounds(),
        beyondViewportPageCount = 1,
        verticalAlignment = Alignment.CenterVertically,
        key = { it },
    ) { page ->
        val overscrollModifier = pagerOverscroll?.run {
            Modifier.visualModifier(page == pagerState.currentPage)
        } ?: Modifier
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = if (page == 0) artworkAlignment else Alignment.Center,
        ) {
            if (page == 0) {
                artworkContent(overscrollModifier)
            } else {
                val lyricBackdrop = lyricsBackdrop ?: if (lyricsBackground != null && lyricsBottomContent != null) {
                    rememberLayerBackdrop()
                } else null
                val actionBackdrop = if (lyricsBackground != null && lyricBackdrop != null) {
                    rememberCombinedBackdrop(lyricsBackground, lyricBackdrop)
                } else null
                SyncedLyrics(
                    activeLyricAlpha = activeLyricAlpha,
                    lyrics = lyrics,
                    currentMediaId = currentMediaId,
                    positionMs = positionMs,
                    positionUpdateElapsedRealtimeMs = positionUpdateElapsedRealtimeMs,
                    playbackIteration = playbackIteration,
                    playbackSpeed = playbackSpeed,
                    previewPositionMs = previewPositionMs,
                    isPlaying = isPlaying,
                    active = lyricsActive && playerActive,
                    playerActive = playerActive,
                    positionDiscontinuityRevision = positionDiscontinuityRevision,
                    onSeek = onSeek,
                    contentWidth = contentWidth,
                    controlColor = controlColor,
                    emphasisControlColor = emphasisControlColor,
                    lyricFontScale = lyricFontScale,
                    lyricFontWeight = lyricFontWeight,
                    lyricAnimationMode = lyricAnimationMode,
                    lyricBlurEnabled = lyricBlurEnabled,
                    centerLyrics = centerLyrics,
                    lyricCenterOffsetY = lyricCenterOffsetY,
                    showLyricsTranslation = showLyricsTranslation,
                    showLyricsRomanization = showLyricsRomanization,
                    lyricsTranslationLanguage = lyricsTranslationLanguage,
                    showBottomFade = showBottomFade,
                    applyEdgeFade = lyricsViewportTopInset == 0.dp && lyricsViewportBottomInset == 0.dp,
                    resumeFollowRequestKey = resumeFollowRequestKey,
                    seekRequestKey = seekRequestKey,
                    seekPositionMs = seekPositionMs,
                    modifier = Modifier.fillMaxSize().then(overscrollModifier).then(
                        if (lyricBackdrop != null) Modifier.layerBackdrop(lyricBackdrop)
                        else Modifier,
                    ).then(
                        if (lyricsViewportTopInset > 0.dp || lyricsViewportBottomInset > 0.dp) {
                            Modifier.portraitLyricViewport(
                                topInset = lyricsViewportTopInset,
                                bottomInset = lyricsViewportBottomInset,
                                showBottomFade = showBottomFade,
                                immersiveMode = immersiveMode,
                                immersiveAlpha = immersiveAlpha,
                            )
                        } else Modifier,
                    ),
                )
                lyricsBottomContent?.let { content ->
                    Box(
                        modifier = Modifier.align(Alignment.BottomCenter)
                            .fillMaxWidth().padding(bottom = lyricsViewportBottomInset),
                    ) { content(actionBackdrop) }
                }
            }
        }
    }
}

internal fun Modifier.portraitLyricViewport(
    topInset: Dp,
    bottomInset: Dp,
    showBottomFade: Boolean,
    immersiveMode: Boolean,
    immersiveAlpha: State<Float>,
): Modifier = graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithCache {
        val top = topInset.toPx().coerceIn(0f, size.height)
        val bottom = (size.height - bottomInset.toPx()).coerceIn(top, size.height)
        val fade = lyricEdgeFadeHeights(showBottomFade).first.dp.toPx()
        fun edgeAlpha(y: Float, start: Float, end: Float): Float {
            val extent = (end - start).coerceAtLeast(0f)
            if (extent == 0f || y < start || y > end) return 0f
            val band = minOf(fade, extent / 2f).coerceAtLeast(1f)
            val enter = ((y - start) / band).coerceIn(0f, 1f)
            val exit = if (showBottomFade) ((end - y) / band).coerceIn(0f, 1f) else 1f
            return minOf(enter, exit)
        }
        val normalBand = minOf(fade, (bottom - top) / 2f)
        val fullBand = minOf(fade, size.height / 2f)
        val stops = listOf(0f, top, top + normalBand, bottom - normalBand, bottom,
            fullBand, size.height - fullBand, size.height).distinct().sorted()
        val normal = stops.map { edgeAlpha(it, top, bottom) }
        val full = stops.map { edgeAlpha(it, 0f, size.height) }
        onDrawWithContent {
            drawContent()
            val alpha = immersiveAlpha.value
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = stops.mapIndexed { index, y ->
                        y / size.height.coerceAtLeast(1f) to
                            Color.Black.copy(alpha = normal[index] * (1f - alpha) + full[index] * alpha)
                    }.toTypedArray(),
                ),
                blendMode = BlendMode.DstIn,
            )
        }
    }
    .pointerInput(topInset, bottomInset, immersiveMode) {
        if (immersiveMode) return@pointerInput
        val top = topInset.toPx()
        val bottom = size.height - bottomInset.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (down.position.y < top || down.position.y >= bottom) {
                down.consume()
                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    event.changes.forEach { it.consume() }
                } while (event.changes.any { it.pressed })
            }
        }
    }

@Composable
internal fun rememberRetainedLyricDocument(lyrics: LyricsUiState): LyricsUiState.Available? {
    var retained by remember { mutableStateOf<LyricsUiState.Available?>(null) }
    val available = when (lyrics) {
        is LyricsUiState.Available -> lyrics
        LyricsUiState.Loading -> retained
        LyricsUiState.Unavailable -> null
    }
    SideEffect { retained = available }
    return available
}

/** Empty lyric documents use the same placeholder as an absent document. */
internal fun lyricPagePresentation(lyrics: LyricsUiState.Available?): LyricsUiState =
    lyrics?.takeIf { available ->
        available.document.lines.any { !it.isBackground && it.displayText.isNotBlank() }
    } ?: LyricsUiState.Unavailable

// Identity intentionally distinguishes a fresh entrance after a rapid reversal.
internal class LyricContentFrame(val lyrics: LyricsUiState, val annotationsVisible: Boolean)

@Composable
internal fun rememberLyricContentFrame(lyrics: LyricsUiState, annotationsVisible: Boolean): LyricContentFrame =
    remember(lyrics, annotationsVisible) { LyricContentFrame(lyrics, annotationsVisible) }

@Composable
internal fun LyricContentTransition(
    lyrics: LyricsUiState,
    modifier: Modifier = Modifier,
    annotationsVisible: Boolean = false,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable (LyricsUiState, Boolean, Boolean) -> Unit,
) {
    val target = rememberLyricContentFrame(lyrics, annotationsVisible)
    AnimatedContent(
        targetState = target,
        modifier = modifier,
        contentAlignment = contentAlignment,
        transitionSpec = {
            val enter = lyricContentEnterSpec(fromEmpty = initialState.lyrics == LyricsUiState.Loading)
            ((fadeIn(enter) + scaleIn(enter, LYRIC_CONTENT_MIN_SCALE))
                togetherWith (fadeOut(lyricContentExitSpec()) +
                    scaleOut(lyricContentExitSpec(), LYRIC_CONTENT_MIN_SCALE))).using(null)
        },
        label = "lyricContent",
    ) { frame -> content(frame.lyrics, frame.annotationsVisible, frame === target) }
}

@Composable
internal fun SyncedLyrics(
    lyrics: LyricsUiState,
    positionMs: Long,
    positionUpdateElapsedRealtimeMs: Long,
    playbackIteration: Long,
    playbackSpeed: Float,
    previewPositionMs: Long?,
    isPlaying: Boolean,
    active: Boolean,
    onSeek: (Long) -> Unit,
    contentWidth: Dp,
    controlColor: Color,
    emphasisControlColor: Color,
    lyricFontScale: Float,
    lyricFontWeight: Int,
    lyricAnimationMode: LyricAnimationMode,
    lyricBlurEnabled: Boolean,
    centerLyrics: Boolean,
    lyricCenterOffsetY: Dp,
    showLyricsTranslation: Boolean,
    showBottomFade: Boolean,
    resumeFollowRequestKey: Int,
    seekRequestKey: Int,
    seekPositionMs: Long,
    modifier: Modifier = Modifier,
    currentMediaId: String? = (lyrics as? LyricsUiState.Available)?.mediaId,
    activeLyricAlpha: Float = 1f,
    showLyricsRomanization: Boolean = true,
    lyricsTranslationLanguage: LyricsTranslationLanguage = LyricsTranslationLanguage.AUTO,
    playerActive: Boolean = true,
    positionDiscontinuityRevision: Long = 0L,
    applyEdgeFade: Boolean = true,
) {
    val displayedLyrics = rememberRetainedLyricDocument(lyrics)
    val hasAnnotations = remember(displayedLyrics?.document) {
        displayedLyrics?.document?.lines?.any { lyricPhonetics(it).hasAnnotations } == true
    }
    var browseAnchor by remember(displayedLyrics?.document) { mutableStateOf<LyricBrowseAnchor?>(null) }

    var unavailableTextLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val alignmentProgress = animateFloatAsState(
        targetValue = if (centerLyrics) 1f else 0f,
        animationSpec = playerTextAlignmentSpec(),
        label = "unavailableLyricsAlignment",
    )

    LyricContentTransition(lyricPagePresentation(displayedLyrics), modifier, hasAnnotations && showLyricsRomanization,
        contentAlignment = Alignment.Center) { frame, annotated, target ->
        val available = frame as? LyricsUiState.Available
        val document = available?.document
        val current = target && frame == lyrics && currentMediaId != null && available?.mediaId == currentMediaId
        var retainedPosition by remember(frame) { mutableLongStateOf(positionMs) }
        // Seed each entering layout once; ongoing drag samples must not recompose the viewport.
        val initialBrowseAnchor = remember(frame) { browseAnchor }
        if (current) SideEffect { retainedPosition = positionMs }
        if (document != null) {
            document.let { document ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LyricsView(
                        activeLyricAlpha = activeLyricAlpha,
                        document = document,
                        annotationLayout = annotated,
                        annotationFrameTarget = target,
                        initialBrowseAnchor = initialBrowseAnchor,
                        onBrowseAnchorChange = { if (target) browseAnchor = it },
                        positionMs = if (current) positionMs else retainedPosition,
                        positionUpdateElapsedRealtimeMs = positionUpdateElapsedRealtimeMs,
                        playbackIteration = playbackIteration,
                        playbackSpeed = playbackSpeed,
                        previewPositionMs = previewPositionMs,
                        isPlaying = isPlaying && current,
                        active = active && current,
                        playerActive = playerActive && current,
                        positionDiscontinuityRevision = positionDiscontinuityRevision,
                        onSeek = onSeek,
                        contentWidth = contentWidth,
                        lyricFontScale = lyricFontScale,
                        lyricFontWeight = lyricFontWeight,
                        lyricAnimationMode = lyricAnimationMode,
                        lyricBlurEnabled = lyricBlurEnabled,
                        centerLyrics = centerLyrics,
                        centerOffsetY = lyricCenterOffsetY,
                        showLyricsTranslation = showLyricsTranslation,
                        showLyricsRomanization = if (!target && frame == displayedLyrics) annotated && showLyricsRomanization
                            else showLyricsRomanization,
                        lyricsTranslationLanguage = lyricsTranslationLanguage,
                        showBottomFade = showBottomFade,
                        applyEdgeFade = applyEdgeFade,
                        resumeFollowRequestKey = if (current) resumeFollowRequestKey else 0,
                        seekRequestKey = if (current) seekRequestKey else 0,
                        seekPositionMs = seekPositionMs,
                        emphasisColor = emphasisControlColor,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        } else if (frame is LyricsUiState.Unavailable) {
            val lyricFontFamily = rememberLyricFontFamily(lyricFontWeight)
            // Consume viewport constraints here so Text keeps its natural line height.
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.lyrics_unavailable),
                    modifier = Modifier.width(contentWidth)
                        .offset(y = lyricCenterOffsetY)
                        .animatedPlayerTextAlignment(unavailableTextLayout, alignmentProgress),
                    style = MiuixTheme.textStyles.title3.copy(
                        fontSize = (LYRIC_PRIMARY_FONT_SIZE_SP * lyricFontScale).sp,
                        lineHeight = (LYRIC_PRIMARY_LINE_HEIGHT_SP * lyricFontScale).sp,
                        fontFamily = lyricFontFamily,
                        fontWeight = FontWeight(lyricFontWeight.coerceIn(1, 1000)),
                        fontSynthesis = FontSynthesis.None,
                    ),
                    color = controlColor.copy(alpha = 0.72f),
                    textAlign = TextAlign.Start,
                    onTextLayout = { unavailableTextLayout = it },
                )
            }
        }
    }
}
