package com.melox.player.ui.screen.playback

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
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
import com.melox.player.model.LyricAnimationMode
import com.melox.player.R
import com.melox.player.data.library.ArtistGroup
import com.melox.player.data.library.displayArtistName
import com.melox.player.model.MusicTrack
import com.melox.player.model.LyricsUiState
import com.melox.player.model.LyricsDocument
import com.melox.player.model.PlaybackMode
import com.melox.player.model.PlaybackBackgroundStyle
import com.melox.player.model.PlaybackQueueItem
import com.melox.player.model.PlaybackUiState
import com.melox.player.model.withTrackMetadata
import com.melox.player.ui.isMiuixWideLayout
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
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Playlist
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

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
    artworkContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    HorizontalPager(
        state = pagerState,
        userScrollEnabled = lyricsPagingEnabled,
        modifier = modifier.clipToBounds(),
        beyondViewportPageCount = 1,
        verticalAlignment = Alignment.CenterVertically,
        key = { it },
    ) { page ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (page == 0) {
                artworkContent()
            } else {
                SyncedLyrics(
                    lyrics = lyrics,
                    positionMs = positionMs,
                    positionUpdateElapsedRealtimeMs = positionUpdateElapsedRealtimeMs,
                    playbackIteration = playbackIteration,
                    playbackSpeed = playbackSpeed,
                    previewPositionMs = previewPositionMs,
                    isPlaying = isPlaying,
                    active = lyricsActive,
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
                    showBottomFade = showBottomFade,
                    resumeFollowRequestKey = resumeFollowRequestKey,
                    seekRequestKey = seekRequestKey,
                    seekPositionMs = seekPositionMs,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
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
) {
    var displayedDocument by remember { mutableStateOf<LyricsDocument?>(null) }
    val lyricsAlpha = remember { Animatable(0f) }
    var unavailableTextLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val alignmentProgress = animateFloatAsState(
        targetValue = if (centerLyrics) 1f else 0f,
        animationSpec = playerTextAlignmentSpec(),
        label = "unavailableLyricsAlignment",
    )

    LaunchedEffect(lyrics) {
        when (lyrics) {
            LyricsUiState.Loading -> {
                if (displayedDocument != null) {
                    lyricsAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(180),
                    )
                }
            }

            LyricsUiState.Unavailable -> {
                if (displayedDocument != null) {
                    lyricsAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(180),
                    )
                    displayedDocument = null
                }
            }

            is LyricsUiState.Available -> {
                val incomingDocument = lyrics.document
                if (displayedDocument != incomingDocument) {
                    if (displayedDocument != null) {
                        lyricsAlpha.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(180),
                        )
                    }
                    displayedDocument = incomingDocument
                    lyricsAlpha.snapTo(0f)
                }
                lyricsAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(240),
                )
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (displayedDocument != null) {
            displayedDocument?.let { document ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = lyricsAlpha.value },
                ) {
                    LyricsView(
                        document = document,
                        positionMs = positionMs,
                        positionUpdateElapsedRealtimeMs = positionUpdateElapsedRealtimeMs,
                        playbackIteration = playbackIteration,
                        playbackSpeed = playbackSpeed,
                        previewPositionMs = previewPositionMs,
                        isPlaying = isPlaying && active,
                        onSeek = onSeek,
                        contentWidth = contentWidth,
                        lyricFontScale = lyricFontScale,
                        lyricFontWeight = lyricFontWeight,
                        lyricAnimationMode = lyricAnimationMode,
                        lyricBlurEnabled = lyricBlurEnabled,
                        centerLyrics = centerLyrics,
                        centerOffsetY = lyricCenterOffsetY,
                        showLyricsTranslation = showLyricsTranslation,
                        showBottomFade = showBottomFade,
                        resumeFollowRequestKey = resumeFollowRequestKey,
                        seekRequestKey = seekRequestKey,
                        seekPositionMs = seekPositionMs,
                        emphasisColor = emphasisControlColor,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        } else if (lyrics is LyricsUiState.Unavailable) {
            val lyricFontFamily = rememberLyricFontFamily(lyricFontWeight)
            Text(
                text = stringResource(R.string.lyrics_unavailable),
                modifier = Modifier.width(contentWidth)
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
