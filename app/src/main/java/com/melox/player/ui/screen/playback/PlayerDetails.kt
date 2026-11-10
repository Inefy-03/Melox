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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
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
import com.melox.player.ui.component.playback.PlayerTrackSkipButton
import com.melox.player.ui.component.playback.rememberPlayerControlPressFeedback
import com.melox.player.ui.component.playback.PlayerControlIconButton
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
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.BasicComponent
import com.melox.player.ui.component.os4.Icon
import com.melox.player.ui.component.os4.IconButton
import top.yukonga.miuix.kmp.basic.IconButtonDefaults
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Translate
import top.yukonga.miuix.kmp.icon.extended.Playlist
import com.melox.player.ui.component.os4.OverlayBottomSheet
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable
import com.melox.player.ui.component.overScrollVertical

@Composable
internal fun PlayerDetails(
    contentWidth: Dp,
    playback: PlaybackUiState,
    emphasisControlColor: Color,
    onTogglePlayPause: () -> Unit,
    immersiveMode: Boolean,
    onToggleImmersiveMode: () -> Unit,
    onPrevious: () -> Boolean,
    onNext: () -> Boolean,
    onSeek: (Long) -> Unit,
    onPreviewPositionChange: (Long?) -> Unit,
    onCyclePlaybackMode: () -> Unit,
    onOpenAudioSettings: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenTrackActions: () -> Unit,
    modifier: Modifier = Modifier,
    primaryControlAlpha: Float? = null,
    primaryControlsBackdrop: Backdrop? = null,
    primaryControlsBackdropAlpha: () -> Float = { 1f },
    primaryControlsWidth: Dp = contentWidth,
    secondaryControlsWidth: Dp = contentWidth,
    spacing: PlayerVerticalSpacing = PlayerVerticalSpacing(),
) {
    val primaryControlColor = emphasisControlColor.copy(
        alpha = primaryControlAlpha ?: emphasisControlColor.alpha,
    )
    val progressIndicatorColor = emphasisControlColor.copy(alpha = 0.6f)
    val progressLabelColor = emphasisControlColor.copy(alpha = 0.6f)
    val secondaryControlColor = emphasisControlColor.copy(alpha = 0.6f)
    Box(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .width(contentWidth)
                    .zIndex(1f)
                    .playerChromeVisibility(!immersiveMode),
            ) {
                PlayerProgress(
                    positionMs = playback.positionMs,
                    durationMs = playback.durationMs,
                    enabled = playback.currentItem != null,
                    interactive = !immersiveMode,
                    indicatorColor = progressIndicatorColor,
                    labelColor = progressLabelColor,
                    primaryControlSpacing = spacing.progressToPrimary,
                    onSeek = onSeek,
                    onPreviewPositionChange = onPreviewPositionChange,
                )
            }
            BoxWithConstraints(
                modifier = Modifier
                    .width(primaryControlsWidth)
                    .height(PLAYER_PLAY_PAUSE_TOUCH_SIZE),
            ) {
                val layout = playerPrimaryControlLayout(maxWidth)
                PlayerPrimaryControlSurface(
                    touchSize = PLAYER_SIDE_CONTROL_TOUCH_SIZE,
                    backdrop = primaryControlsBackdrop,
                    backdropAlpha = primaryControlsBackdropAlpha,
                    modifier = Modifier.align(Alignment.CenterStart).offset(x = layout.previousTouchOffset),
                ) {
                    PlayerTrackSkipButton(
                        direction = -1,
                        tint = primaryControlColor,
                        iconSize = PLAYER_SIDE_CONTROL_ICON_SIZE,
                        touchSize = PLAYER_SIDE_CONTROL_TOUCH_SIZE,
                        onClick = { playback.currentItem != null && onPrevious() },
                    )
                }
                PlayerPrimaryControlSurface(
                    touchSize = PLAYER_PLAY_PAUSE_TOUCH_SIZE,
                    backdrop = primaryControlsBackdrop,
                    backdropAlpha = primaryControlsBackdropAlpha,
                    modifier = Modifier.align(Alignment.CenterStart).offset(x = layout.playPauseTouchOffset),
                ) {
                    AnimatedPlayPauseButton(
                        playWhenReady = playback.playWhenReady,
                        tint = primaryControlColor,
                        onClick = {
                            if (playback.currentItem != null) onTogglePlayPause()
                        },
                        onLongClick = onToggleImmersiveMode,
                        onLongClickLabel = stringResource(
                            if (immersiveMode) R.string.player_immersive_mode_disable
                            else R.string.player_immersive_mode_enable,
                        ),
                    )
                }
                PlayerPrimaryControlSurface(
                    touchSize = PLAYER_SIDE_CONTROL_TOUCH_SIZE,
                    backdrop = primaryControlsBackdrop,
                    backdropAlpha = primaryControlsBackdropAlpha,
                    modifier = Modifier.align(Alignment.CenterStart).offset(x = layout.nextTouchOffset),
                ) {
                    PlayerTrackSkipButton(
                        direction = 1,
                        tint = primaryControlColor,
                        iconSize = PLAYER_SIDE_CONTROL_ICON_SIZE,
                        touchSize = PLAYER_SIDE_CONTROL_TOUCH_SIZE,
                        onClick = { playback.currentItem != null && onNext() },
                    )
                }
            }
            Spacer(Modifier.height(spacing.controlGroup))
            Row(
                modifier = Modifier
                    .width(secondaryControlsWidth)
                    .playerChromeVisibility(!immersiveMode),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlaybackModeButton(
                    mode = playback.playbackMode,
                    tint = secondaryControlColor,
                    enabled = !immersiveMode,
                    onClick = onCyclePlaybackMode,
                )
                PlayerFunctionButton(
                    onClick = onOpenSleepTimer,
                    enabled = !immersiveMode,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_player_sleep_timer),
                        contentDescription = stringResource(R.string.playback_sleep_timer),
                        modifier = Modifier.size(24.dp),
                        tint = secondaryControlColor,
                    )
                }
                PlayerFunctionButton(
                    onClick = onOpenAudioSettings,
                    enabled = !immersiveMode,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_player_audio_settings),
                        contentDescription = stringResource(R.string.playback_audio_settings),
                        modifier = Modifier.size(24.dp),
                        tint = secondaryControlColor,
                    )
                }
                PlayerIconButton(
                    icon = MiuixIcons.Playlist,
                    description = stringResource(R.string.open_queue),
                    size = 24.dp,
                    tint = secondaryControlColor,
                    enabled = !immersiveMode,
                    onClick = onOpenQueue,
                )
                PlayerIconButton(
                    icon = MiuixIcons.More,
                    description = stringResource(R.string.track_actions),
                    size = 23.dp,
                    tint = secondaryControlColor,
                    enabled = !immersiveMode,
                    onClick = onOpenTrackActions,
                )
            }
        }
    }
}

@Composable
private fun PlayerPrimaryControlSurface(
    touchSize: Dp,
    backdrop: Backdrop?,
    backdropAlpha: () -> Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.size(touchSize), contentAlignment = Alignment.Center) {
        if (backdrop != null) {
            PlayerRadialControlBackdrop(
                backdrop = backdrop,
                modifier = Modifier.matchParentSize()
                    .requiredSize(touchSize + 48.dp)
                    .graphicsLayer { alpha = backdropAlpha() },
            )
        }
        content()
    }
}

@Composable
private fun PlayerRadialControlBackdrop(backdrop: Backdrop, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithCache {
                val mask = Brush.radialGradient(
                    0f to Color.White,
                    0.45f to Color.White,
                    1f to Color.Transparent,
                    radius = size.minDimension / 2f,
                )
                onDrawWithContent {
                    drawContent()
                    drawRect(mask, blendMode = BlendMode.DstIn)
                }
            }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RectangleShape },
                effects = { blur(12.dp.toPx()) },
            ),
    )
}
@Composable
private fun AnimatedPlayPauseButton(
    playWhenReady: Boolean,
    tint: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onLongClickLabel: String,
    modifier: Modifier = Modifier,
) {
    val press = rememberPlayerControlPressFeedback()
    PlayerControlIconButton(
        onClick = onClick,
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        modifier = modifier.then(press.modifier),
        minWidth = PLAYER_PLAY_PAUSE_TOUCH_SIZE,
        minHeight = PLAYER_PLAY_PAUSE_TOUCH_SIZE,
    ) {
        AnimatedContent(
            modifier = Modifier.graphicsLayer {
                scaleX = press.scale.value
                scaleY = press.scale.value
            },
            targetState = playWhenReady,
            transitionSpec = { playerControlIconTransition() },
            label = "playPauseIcon",
        ) { playing ->
            Icon(
                painter = painterResource(
                    if (playing) R.drawable.ic_player_pause else R.drawable.ic_player_play,
                ),
                contentDescription = stringResource(if (playing) R.string.pause else R.string.play),
                modifier = Modifier.size(PLAYER_PLAY_PAUSE_ICON_SIZE),
                tint = tint,
            )
        }
    }
}

@Composable
private fun PlaybackModeButton(
    mode: PlaybackMode,
    tint: Color,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    PlayerFunctionButton(
        onClick = onClick,
        enabled = enabled,
    ) {
        val (icon, description) = when (mode) {
            PlaybackMode.ORDER -> R.drawable.ic_player_repeat_all to
                R.string.playback_mode_order
            PlaybackMode.REPEAT_ONE -> R.drawable.ic_player_repeat_one to
                R.string.playback_mode_repeat_one
            PlaybackMode.RANDOM -> R.drawable.ic_player_shuffle to
                R.string.playback_mode_random
        }
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(description),
            modifier = Modifier.size(24.dp),
            tint = tint,
        )
    }
}

@Composable
private fun PlayerProgress(
    positionMs: Long,
    durationMs: Long,
    enabled: Boolean,
    interactive: Boolean,
    indicatorColor: Color,
    labelColor: Color,
    primaryControlSpacing: Dp,
    onSeek: (Long) -> Unit,
    onPreviewPositionChange: (Long?) -> Unit,
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    val interactionEnabled = enabled && interactive && durationMs > 0L
    var seekPositionMs by remember { mutableFloatStateOf(positionMs.toFloat()) }
    var isSeeking by remember { mutableStateOf(false) }
    val currentOnSeek by rememberUpdatedState(onSeek)
    val currentOnPreviewPositionChange by rememberUpdatedState(onPreviewPositionChange)
    LaunchedEffect(positionMs, durationMs, isSeeking) {
        if (!isSeeking) {
            seekPositionMs = positionMs.coerceIn(0L, safeDuration).toFloat()
        }
    }
    val displayedPosition = if (isSeeking) {
        seekPositionMs
    } else {
        positionMs.coerceIn(0L, safeDuration).toFloat()
    }
    val progress = (displayedPosition / safeDuration.toFloat()).coerceIn(0f, 1f)
    val unavailableTime = stringResource(R.string.player_time_unavailable)
    val indicatorHeight by animateDpAsState(
        targetValue = if (isSeeking) 12.dp else PLAYER_PROGRESS_IDLE_HEIGHT,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 550f),
        label = "playerProgressPressedHeight",
    )
    val indicatorWidthExpansion by animateDpAsState(
        targetValue = if (isSeeking) 8.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 550f),
        label = "playerProgressPressedWidth",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(playerProgressLayoutHeight(primaryControlSpacing)),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(PLAYER_PROGRESS_TOUCH_HEIGHT)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                    if (interactionEnabled) {
                        setProgress { targetProgress ->
                            seekPositionMs =
                                targetProgress.coerceIn(0f, 1f) * safeDuration.toFloat()
                            currentOnSeek(seekPositionMs.roundToLong())
                            true
                        }
                    }
                }
                .pointerInput(interactionEnabled, safeDuration) {
                    if (!interactionEnabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        fun updatePosition(horizontalPosition: Float): Long {
                            val fraction = if (size.width == 0) {
                                0f
                            } else {
                                (horizontalPosition / size.width).coerceIn(0f, 1f)
                            }
                            seekPositionMs = fraction * safeDuration.toFloat()
                            return seekPositionMs.roundToLong()
                        }
                        isSeeking = true
                        updatePosition(down.position.x)
                        down.consume()
                        var completed = false
                        var isDragging = false
                        try {
                            completed = drag(down.id) { change ->
                                val targetPositionMs = updatePosition(change.position.x)
                                if (
                                    !isDragging &&
                                        progressGestureIsDrag(
                                            horizontalDistancePx =
                                                change.position.x - down.position.x,
                                            touchSlopPx = viewConfiguration.touchSlop,
                                        )
                                ) {
                                    isDragging = true
                                }
                                if (isDragging) {
                                    currentOnPreviewPositionChange(targetPositionMs)
                                }
                                change.consume()
                            }
                            if (completed) currentOnSeek(seekPositionMs.roundToLong())
                        } finally {
                            if (!completed || !isDragging) {
                                currentOnPreviewPositionChange(null)
                            }
                            isSeeking = false
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            LinearProgressIndicator(
                progress = progress,
                colors = ProgressIndicatorDefaults.progressIndicatorColors(
                    foregroundColor = if (enabled) {
                        indicatorColor
                    } else {
                        indicatorColor.copy(alpha = indicatorColor.alpha * 0.6f)
                    },
                    disabledForegroundColor =
                        indicatorColor.copy(alpha = indicatorColor.alpha * 0.5f),
                    backgroundColor =
                        indicatorColor.copy(alpha = 0.3f),
                ),
                height = indicatorHeight,
                modifier = Modifier
                    .width(maxWidth + indicatorWidthExpansion),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = PLAYER_PROGRESS_LABEL_OFFSET),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (enabled) formatPlayerProgressTime(displayedPosition.roundToLong()) else unavailableTime,
                style = MiuixTheme.textStyles.footnote1,
                color = labelColor,
            )
            Text(
                text = if (enabled) formatPlayerProgressTime(durationMs) else unavailableTime,
                style = MiuixTheme.textStyles.footnote1,
                color = labelColor,
            )
        }
    }
}

internal fun formatPlayerProgressTime(durationMs: Long): String {
    val safeDuration = durationMs.coerceAtLeast(0L)
    val formatted = formatDuration(safeDuration)
    return if (safeDuration < 600_000L) "0$formatted" else formatted
}

@Composable
internal fun PlayerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    tint: Color = MiuixTheme.colorScheme.onSurface,
    enabled: Boolean = true,
) {
    PlayerFunctionButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(size),
            tint = tint,
        )
    }
}

@Composable
internal fun PlayerLyricActionsRow(
    showLyricsTranslation: Boolean,
    emphasisControlColor: Color,
    onOpenLyricsSettings: () -> Unit,
    onToggleLyricsTranslation: () -> Unit,
    onOpenTranslationSettings: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    blurBackdrop: Backdrop? = null,
) {
    val secondaryControlColor = emphasisControlColor.copy(alpha = 0.6f)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerFunctionButton(
            onClick = onOpenLyricsSettings,
            blurBackdrop = blurBackdrop,
            enabled = enabled,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_player_lyrics_settings),
                contentDescription = stringResource(R.string.player_settings_open),
                modifier = Modifier.size(24.dp),
                tint = secondaryControlColor,
            )
        }
        repeat(3) { Spacer(Modifier.width(IconButtonDefaults.MinWidth)) }
        PlayerFunctionButton(
            onClick = onToggleLyricsTranslation,
            onLongClick = onOpenTranslationSettings,
            onLongClickLabel = stringResource(R.string.lyrics_translation_open),
            enabled = enabled,
            blurBackdrop = blurBackdrop,
            modifier = Modifier.semantics {
                toggleableState = ToggleableState(showLyricsTranslation)
            },
        ) {
            Icon(
                imageVector = MiuixIcons.Translate,
                contentDescription = stringResource(R.string.lyrics_translation),
                tint = if (showLyricsTranslation) secondaryControlColor
                    else emphasisControlColor.copy(alpha = 0.3f),
            )
        }
    }
}

@Composable
private fun PlayerFunctionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    blurBackdrop: Backdrop? = null,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val feedback = remember { SinkFeedback() }
    Box(
        modifier = modifier.then(if (enabled) {
            Modifier.pressable(
                interactionSource = interactionSource,
                indication = feedback,
                delay = null,
            )
            .combinedClickable(
                role = Role.Button,
                interactionSource = null,
                indication = null,
                onLongClick = onLongClick,
                onLongClickLabel = onLongClickLabel,
                onClick = onClick,
            )
        } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (blurBackdrop != null) {
            PlayerRadialControlBackdrop(
                backdrop = blurBackdrop,
                modifier = Modifier.matchParentSize()
                    .requiredSize(64.dp),
            )
        }
        IconButton(onClick = onClick, enabled = false, content = content)
    }
}
