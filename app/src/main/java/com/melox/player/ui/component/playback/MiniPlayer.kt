package com.melox.player.ui.component.playback

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melox.player.R
import com.melox.player.model.BottomBarStyle
import com.melox.player.model.PlaybackQueueItem
import com.melox.player.model.PlaybackUiState
import top.yukonga.miuix.kmp.glass.GlassShape
import top.yukonga.miuix.kmp.glass.GlassShadows
import top.yukonga.miuix.kmp.glass.glassShadow
import com.melox.player.ui.MiniPlayerChrome
import com.melox.player.ui.NORMAL_BAR_STROKE_ALPHA
import com.melox.player.ui.component.animation.InteractiveHighlight
import com.melox.player.ui.component.library.PlaybackArtworkBlend
import com.melox.player.ui.screen.playback.toArtworkBlend
import com.melox.player.ui.component.liquid.miuixFloatingBarShadow
import com.melox.player.ui.component.liquid.miniPlayerSurface
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.melox.player.ui.component.os4.Icon
import top.yukonga.miuix.kmp.basic.IconButtonDefaults
import top.yukonga.miuix.kmp.basic.DividerDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Playlist
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MiniPlayer(
    playback: PlaybackUiState,
    artworkState: ArtworkTransitionState<Bitmap>,
    chrome: MiniPlayerChrome,
    interactiveHighlight: InteractiveHighlight,
    onOpen: (Float, Float) -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenQueue: () -> Unit,
    onPlayerDragStart: (Float, Float) -> Unit,
    onPlayerDrag: (Float, Float) -> Unit,
    onPlayerDragEnd: (Float, Float) -> Unit,
    onPlayerDragCancel: () -> Unit,
    playerLayer: GraphicsLayer,
    playerContentLayer: GraphicsLayer,
    frameRecordingGeneration: Int,
    recordForTransition: Boolean,
    drawInPlace: Boolean,
    surfaceVisible: Boolean,
    sharedArtworkVisible: Boolean,
    playerSheetProgress: () -> Float,
    capturedPressScale: Float?,
    capturedSwipeTranslationX: Float?,
    onLayerRecorded: (generation: Int, size: IntSize) -> Unit,
    onPlayerBoundsChanged: (Rect) -> Unit,
    onPlayerContentBoundsChanged: (Rect) -> Unit,
    onPlayerControlsBoundsChanged: (Rect) -> Unit,
    onArtworkBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier,
    circularArtwork: Boolean = false,
    squareArtwork: Boolean = false,
    subtitleContent: (@Composable (String?, String, TextStyle, Color, Dp) -> Unit)? = null,
) {
    val item = playback.currentItem
    val hasItem = item != null
    val isNormal = chrome.style == BottomBarStyle.NORMAL
    val isSmallFloating = chrome.smallPlayerBar && !isNormal
    val isOs4 = chrome.style == BottomBarStyle.OS4
    val surfaceCornerRadius = chrome.cornerRadius
    val artworkCornerRadius = if (isOs4) {
        if (circularArtwork) chrome.artworkSize / 2 else chrome.os4ArtworkCornerRadius
    } else {
        miniPlayerArtworkCornerRadius(isNormal, chrome.smallPlayerBar, circularArtwork)
    }
    val normalOutlineColor = DividerDefaults.DividerColor.copy(
        alpha = NORMAL_BAR_STROKE_ALPHA,
    )
    val playerHeight = chrome.height
    val artworkSize = chrome.artworkSize
    val metadataSpacing = if (isOs4) 4.dp + 4.dp * chrome.contentFraction
        else miniPlayerArtworkMetadataSpacing(isNormal, chrome.smallPlayerBar)
    val controlSize = miniPlayerControlSize()
    val playPauseIconSize = 22.dp
    val controlIconSize = 24.dp
    val titleStyle = if (isOs4) {
        androidx.compose.ui.text.lerp(
            MiuixTheme.textStyles.subtitle,
            MiuixTheme.textStyles.body1,
            chrome.contentFraction,
        ).copy(fontSize = 14.sp)
    } else if (isSmallFloating) {
        MiuixTheme.textStyles.subtitle
    } else {
        MiuixTheme.textStyles.body1
    }
    val artistStyle = if (isOs4) {
        androidx.compose.ui.text.lerp(
            MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
            MiuixTheme.textStyles.footnote1,
            chrome.contentFraction,
        ).copy(fontSize = 12.sp)
    } else if (isSmallFloating) {
        MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp)
    } else {
        MiuixTheme.textStyles.footnote1
    }
    val pressEnabled = chrome.liquidGlassActive && chrome.backdrop != null
    val pressProgress = remember { Animatable(0f, 0.001f) }
    var pressed by remember { mutableStateOf(false) }
    var playerBounds by remember { mutableStateOf(Rect.Zero) }
    var contentCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val pressGrowth = with(LocalDensity.current) { 16.dp.toPx() }
    val recordLayers = !pressEnabled || recordForTransition
    val swipeState = rememberMiniPlayerSwipeState(pressEnabled, recordForTransition)
    val swipeOffsetLimit = with(LocalDensity.current) { 4.dp.toPx() }
    fun pressScale(): Float = if (pressEnabled) {
        capturedPressScale
            ?: (1f + pressGrowth / playerBounds.width.coerceAtLeast(1f) * pressProgress.value)
    } else {
        1f
    }
    fun swipeTranslationX(): Float = when {
        !pressEnabled -> 0f
        capturedSwipeTranslationX != null -> capturedSwipeTranslationX
        else -> swipeState.translationX(playerBounds.width, swipeOffsetLimit)
    }
    fun layoutBounds(coordinates: LayoutCoordinates): Rect {
        val content = contentCoordinates
        return if (content?.isAttached == true) {
            content.localBoundingBoxOf(coordinates, clipBounds = false)
                .translate(playerBounds.topLeft)
        } else {
            coordinates.boundsInRoot()
        }
    }
    LaunchedEffect(pressEnabled, pressed, capturedPressScale) {
        if (!pressEnabled) {
            pressProgress.snapTo(0f)
        } else if (capturedPressScale != null) {
            // Keep the hidden press spring at the shared container's captured size.
            pressProgress.snapTo(
                ((capturedPressScale - 1f) * playerBounds.width / pressGrowth).coerceIn(0f, 1f),
            )
        } else {
            pressProgress.animateTo(
                if (pressed) 1f else 0f,
                spring(1f, 1000f, 0.001f),
            )
        }
    }
    val pressModifier = if (pressEnabled) {
        Modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                pressed = true
                try {
                    // Observe child controls and existing drags without consuming input.
                    while (awaitPointerEvent(PointerEventPass.Final).changes.any { it.pressed }) {
                        // Keep the press until the pointer is released, including during drags.
                    }
                } finally {
                    pressed = false
                    swipeState.release()
                }
            }
        }
    } else {
        Modifier
    }
    val expansionGestureModifier = rememberPlayerSheetVerticalDragModifier(
        enabled = true,
        hasItem = true,
        onDragStart = { onPlayerDragStart(pressScale(), swipeTranslationX()) },
        onDrag = { onPlayerDrag(it, pressScale()) },
        onDragEnd = { onPlayerDragEnd(it, pressScale()) },
        onDragCancel = onPlayerDragCancel,
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (isNormal) 6.dp else 0.dp)
            .padding(bottom = if (isNormal) 6.dp else FLOATING_MINI_PLAYER_BOTTOM_PADDING)
            .height(playerHeight)
            .onGloballyPositioned { coordinates ->
                playerBounds = coordinates.boundsInRoot()
                onPlayerBoundsChanged(playerBounds)
            }
            .then(
                if (recordLayers) Modifier.recordMiniPlayerLayer(
                    layer = playerLayer,
                    drawInPlace = drawInPlace,
                    drawLiveGlass = pressEnabled,
                    recordingGeneration = frameRecordingGeneration,
                    onRecorded = onLayerRecorded,
                ) else Modifier,
            )
            .then(pressModifier)
            .then(expansionGestureModifier)
            .clickable(
                enabled = hasItem,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onOpen(pressScale(), swipeTranslationX()) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (surfaceVisible && !pressEnabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(
                        if (isOs4) Modifier.glassShadow(GlassShape(surfaceCornerRadius), GlassShadows.Float)
                        else if (isNormal) Modifier else Modifier.miuixFloatingBarShadow(
                            cornerRadius = surfaceCornerRadius,
                            isDark = chrome.isDark,
                        ),
                    )
                    .then(
                        if (chrome.style == BottomBarStyle.OS4) Modifier
                        else Modifier.squircleClip(surfaceCornerRadius),
                    )
                    .miniPlayerSurface(
                        cornerRadius = surfaceCornerRadius,
                        style = chrome.style,
                        backdrop = chrome.backdrop,
                        blurActive = chrome.blurActive,
                        liquidGlassActive = chrome.liquidGlassActive,
                        isDark = chrome.isDark,
                        followsNavigationBar = isNormal,
                        floatingHighlight = chrome.floatingHighlight,
                    )
                    .then(
                        if (isNormal) Modifier.squircleBorder(
                            width = DividerDefaults.Thickness,
                            color = normalOutlineColor,
                            cornerRadius = surfaceCornerRadius,
                        ) else Modifier,
                    ),
            )
        }
        if (pressEnabled && surfaceVisible) {
            // Follow the glass press without changing its inverse backdrop sampling.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        translationX = swipeTranslationX()
                        val scale = pressScale()
                        scaleX = scale
                        scaleY = scale
                    }
                    .miuixFloatingBarShadow(surfaceCornerRadius, chrome.isDark),
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    translationX = swipeTranslationX()
                }
                .then(
                    when {
                        pressEnabled && surfaceVisible -> Modifier
                            .miniPlayerSurface(
                                cornerRadius = surfaceCornerRadius,
                                style = chrome.style,
                                backdrop = chrome.backdrop,
                                blurActive = chrome.blurActive,
                                liquidGlassActive = true,
                                isDark = chrome.isDark,
                                followsNavigationBar = isNormal,
                                floatingHighlight = chrome.floatingHighlight,
                                layerBlock = {
                                    val scale = pressScale()
                                    scaleX = scale
                                    scaleY = scale
                                },
                            )
                        pressEnabled -> Modifier.graphicsLayer {
                            val scale = pressScale()
                            scaleX = scale
                            scaleY = scale
                        }
                        else -> Modifier
                    },
                )
                .then(
                    if (pressEnabled) {
                        interactiveHighlight.modifier.then(interactiveHighlight.trackingGestureModifier)
                    } else Modifier,
                )
                .onGloballyPositioned { contentCoordinates = it },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        onPlayerContentBoundsChanged(layoutBounds(coordinates))
                    }
                    .then(
                        if (recordLayers) Modifier.recordPlayerContentLayer(playerContentLayer)
                        else Modifier,
                    )
                    .padding(
                        start = if (isOs4) chrome.os4ArtworkStartPadding
                        else miniPlayerArtworkStartPadding(isNormal, chrome.smallPlayerBar, circularArtwork),
                        end = when {
                            isOs4 -> 7.dp
                            isSmallFloating -> 8.dp
                            chrome.smallPlayerBar -> (playerHeight - controlSize) / 2
                            else -> 10.dp
                        },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(artworkSize)
                        .onGloballyPositioned { coordinates ->
                            onArtworkBoundsChanged(layoutBounds(coordinates))
                        }
                        .graphicsLayer {
                            alpha = if (sharedArtworkVisible) 1f else 0f
                        },
                ) {
                    MiniPlayerArtwork(
                        state = artworkState,
                        playerSheetProgress = playerSheetProgress,
                        size = artworkSize,
                        cornerRadius = artworkCornerRadius,
                        modifier = Modifier.fillMaxSize(),
                        circular = circularArtwork,
                        square = squareArtwork,
                    )
                }
                Spacer(modifier = Modifier.width(metadataSpacing))
                SwipeableMetadata(
                    playback = playback,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onSwipeStart = swipeState::begin,
                    onSwipeDrag = {
                        if (pressEnabled && !recordForTransition) swipeState.dragBy(it)
                    },
                    onSwipeRelease = swipeState::release,
                    titleStyle = titleStyle,
                    artistStyle = artistStyle,
                    modifier = Modifier.weight(1f),
                    contentStartPadding = 4.dp,
                    subtitleContent = subtitleContent,
                )
                Spacer(modifier = Modifier.width(if (isNormal) 6.dp else 2.dp))
                Row(
                    // Keep the former 54 dp surface's control centers relative to its bottom edge.
                    modifier = Modifier
                        .offset(y = if (isOs4) (-1).dp else 0.dp)
                        .onGloballyPositioned { coordinates ->
                            onPlayerControlsBoundsChanged(layoutBounds(coordinates))
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    val playPausePress = rememberPlayerControlPressFeedback(hasItem)
                    PlayerControlIconButton(
                        modifier = Modifier
                            .offset(x = if (isOs4 || chrome.smallPlayerBar) 0.dp else (-6).dp)
                            .then(playPausePress.modifier),
                        enabled = hasItem,
                        onClick = onTogglePlayPause,
                        minWidth = controlSize,
                        minHeight = controlSize,
                    ) {
                        AnimatedContent(
                            modifier = Modifier.graphicsLayer {
                                scaleX = playPausePress.scale.value
                                scaleY = playPausePress.scale.value
                            },
                            targetState = playback.playWhenReady,
                            transitionSpec = { playerControlIconTransition() },
                            label = "miniPlayPauseIcon",
                        ) { playing ->
                            Icon(
                                painter = painterResource(
                                    if (playing) {
                                        R.drawable.ic_player_pause
                                    } else {
                                        R.drawable.ic_player_play
                                    },
                                ),
                                contentDescription = stringResource(
                                    if (playing) R.string.pause else R.string.play,
                                ),
                                modifier = Modifier.size(playPauseIconSize),
                            )
                        }
                    }
                    PlayerControlIconButton(
                        modifier = Modifier.offset(x = if (isOs4) 0.dp else miniPlayerQueueOffset(isNormal, chrome.smallPlayerBar)),
                        onClick = onOpenQueue,
                        minWidth = controlSize,
                        minHeight = controlSize,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Playlist,
                            contentDescription = stringResource(R.string.open_queue),
                            modifier = Modifier.size(controlIconSize),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerArtwork(
    state: ArtworkTransitionState<Bitmap>,
    playerSheetProgress: () -> Float,
    size: Dp,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    circular: Boolean = false,
    square: Boolean = false,
) {
    val squareClip = circular || square
    PlaybackArtworkBlend(
        blend = state.toArtworkBlend(),
        size = size,
        cornerRadius = cornerRadius,
        modifier = modifier,
        contentScale = if (squareClip) ContentScale.Crop else ContentScale.Fit,
        rectangularCornerRadiusReduction = if (squareClip) {
            0.dp
        } else {
            MINI_PLAYER_RECTANGULAR_ARTWORK_CORNER_REDUCTION
        },
        circular = circular,
        placeholderAlpha = playerSheetPlaceholderArtworkAlpha(playerSheetProgress()),
    )
}

@Composable
private fun SwipeableMetadata(
    playback: PlaybackUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSwipeStart: () -> Unit,
    onSwipeDrag: (Float) -> Unit,
    onSwipeRelease: () -> Unit,
    titleStyle: TextStyle,
    artistStyle: TextStyle,
    modifier: Modifier = Modifier,
    contentStartPadding: Dp = 0.dp,
    subtitleContent: (@Composable (String?, String, TextStyle, Color, Dp) -> Unit)? = null,
) {
    val item = playback.currentItem
    val currentOnSwipeStart by rememberUpdatedState(onSwipeStart)
    val currentOnSwipeDrag by rememberUpdatedState(onSwipeDrag)
    val currentOnSwipeRelease by rememberUpdatedState(onSwipeRelease)
    val emptyTitle = stringResource(R.string.mini_player_title)
    val emptyArtist = stringResource(R.string.mini_player_empty)
    val unknownArtist = stringResource(R.string.music_unknown_artist)
    val liveMetadata = item.toMiniPlayerMetadata(
        emptyTitle = emptyTitle,
        emptyArtist = emptyArtist,
        unknownArtist = unknownArtist,
    )
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var horizontalOffset by remember { mutableFloatStateOf(0f) }
    var thresholdHapticDirection by remember { mutableIntStateOf(0) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    var contentWidth by remember { mutableFloatStateOf(1f) }
    var labelWidth by remember { mutableFloatStateOf(0f) }
    val edgeMaskWidthPx = with(LocalDensity.current) { MiniMetadataEdgeMaskWidth.toPx() }
    val labelSpacingPx = with(LocalDensity.current) { MiniMetadataLabelSpacing.toPx() }
    var visibleMetadata by remember { mutableStateOf(liveMetadata) }
    var outgoingMetadata by remember { mutableStateOf<MiniPlayerMetadata?>(null) }
    var crossfadeProgress by remember { mutableFloatStateOf(1f) }
    var expectedSwipeMediaId by remember { mutableStateOf<String?>(null) }
    val titleColor = MiuixTheme.colorScheme.onBackground
    val artistColor = MiuixTheme.colorScheme.onSurfaceVariantSummary
    val swipeProgress =
        (
            abs(horizontalOffset) /
                miniMetadataSwipeCommitThreshold(
                    contentWidthPx = contentWidth,
                    labelWidthPx = labelWidth,
                    edgeMaskWidthPx = edgeMaskWidthPx,
                    labelSpacingPx = labelSpacingPx,
                )
            ).coerceIn(0f, 1f)
    val swipeLabel = when {
        horizontalOffset > 0f -> stringResource(R.string.previous_track)
        horizontalOffset < 0f -> stringResource(R.string.next_track)
        else -> ""
    }

    LaunchedEffect(liveMetadata) {
        if (hasExpectedMiniMetadataSwipeTarget(expectedSwipeMediaId, liveMetadata.mediaId)) {
            expectedSwipeMediaId = null
            return@LaunchedEffect
        }
        if (liveMetadata == visibleMetadata) return@LaunchedEffect
        settleJob?.cancel()
        horizontalOffset = 0f
        outgoingMetadata = visibleMetadata
        visibleMetadata = liveMetadata
        val animation = Animatable(0f)
        animation.animateTo(1f, tween(MiniMetadataReturnDurationMillis)) {
            crossfadeProgress = value
        }
        crossfadeProgress = 1f
        outgoingMetadata = null
    }

    Box(
        modifier = modifier
            .onSizeChanged {
                contentWidth = it.width.toFloat().coerceAtLeast(1f)
            }
            .clipToBounds()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val edgeFraction = (MiniMetadataEdgeMaskWidth.toPx() / size.width.coerceAtLeast(1f))
                    .coerceIn(0f, 0.18f)
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to Color.Transparent,
                        edgeFraction to Color.Black,
                        1f - edgeFraction to Color.Black,
                        1f to Color.Transparent,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
            .pointerInput(item?.mediaId, contentWidth) {
                if (item == null) return@pointerInput
                var swipeActive = false
                try {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            swipeActive = true
                            currentOnSwipeStart()
                            settleJob?.cancel()
                            thresholdHapticDirection = 0
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            currentOnSwipeDrag(dragAmount)
                            val updatedOffset =
                                (horizontalOffset + dragAmount).coerceIn(-contentWidth, contentWidth)
                            horizontalOffset = updatedOffset
                            val currentThresholdDirection = miniMetadataSwipeThresholdDirection(
                                offsetPx = updatedOffset,
                                commits = miniMetadataSwipeCommits(
                                    offsetPx = updatedOffset,
                                    contentWidthPx = contentWidth,
                                    labelWidthPx = labelWidth,
                                    edgeMaskWidthPx = edgeMaskWidthPx,
                                    labelSpacingPx = labelSpacingPx,
                                ),
                                hasDifferentTarget =
                                    playback.hasDifferentMetadataSwipeTarget(updatedOffset),
                            )
                            if (
                                shouldTriggerMiniMetadataSwipeThresholdHaptic(
                                    previousDirection = thresholdHapticDirection,
                                    currentDirection = currentThresholdDirection,
                                )
                            ) {
                                hapticFeedback.performHapticFeedback(
                                    HapticFeedbackType.GestureThresholdActivate,
                                )
                            }
                            thresholdHapticDirection = currentThresholdDirection
                        },
                        onDragEnd = {
                            swipeActive = false
                            currentOnSwipeRelease()
                            val startOffset = horizontalOffset
                            val targetMetadata = playback.targetMetadataForSwipe(
                                offset = startOffset,
                                emptyTitle = emptyTitle,
                                emptyArtist = emptyArtist,
                                unknownArtist = unknownArtist,
                            )
                            val commits = miniMetadataSwipeCommits(
                                offsetPx = startOffset,
                                contentWidthPx = contentWidth,
                                labelWidthPx = labelWidth,
                                edgeMaskWidthPx = edgeMaskWidthPx,
                                labelSpacingPx = labelSpacingPx,
                            )
                            settleJob = scope.launch {
                                val oldMetadata = visibleMetadata
                                val shouldCrossfade =
                                    commits &&
                                        targetMetadata != null &&
                                        targetMetadata.mediaId != oldMetadata.mediaId
                                if (shouldCrossfade) {
                                    outgoingMetadata = oldMetadata
                                    visibleMetadata = targetMetadata
                                    crossfadeProgress = 0f
                                    expectedSwipeMediaId = targetMetadata.mediaId
                                }
                                if (commits) {
                                    if (startOffset > 0f) onPrevious() else onNext()
                                }
                                val animation = Animatable(startOffset)
                                animation.animateTo(0f, tween(MiniMetadataReturnDurationMillis)) {
                                    horizontalOffset = value
                                    if (shouldCrossfade) {
                                        crossfadeProgress =
                                            (1f - abs(value) / abs(startOffset).coerceAtLeast(1f))
                                                .coerceIn(0f, 1f)
                                    }
                                }
                                horizontalOffset = 0f
                                if (shouldCrossfade) {
                                    crossfadeProgress = 1f
                                    outgoingMetadata = null
                                }
                            }
                            thresholdHapticDirection = 0
                        },
                        onDragCancel = {
                            swipeActive = false
                            currentOnSwipeRelease()
                            thresholdHapticDirection = 0
                            settleJob = scope.launch {
                                Animatable(horizontalOffset).animateTo(
                                    0f,
                                    tween(MiniMetadataReturnDurationMillis),
                                ) {
                                    horizontalOffset = value
                                }
                            }
                        },
                    )
                } finally {
                    if (swipeActive) currentOnSwipeRelease()
                }
            },
    ) {
        if (swipeLabel.isNotEmpty()) {
            Text(
                text = swipeLabel,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                modifier = Modifier
                    .align(
                        if (horizontalOffset > 0f) {
                            Alignment.CenterStart
                        } else {
                            Alignment.CenterEnd
                        },
                    )
                    .onSizeChanged { labelWidth = it.width.toFloat() }
                    .graphicsLayer {
                        alpha = swipeProgress
                        translationX = if (horizontalOffset > 0f) {
                            horizontalOffset - labelWidth - labelSpacingPx
                        } else {
                            horizontalOffset + labelWidth + labelSpacingPx
                        }
                    },
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = horizontalOffset },
        ) {
            outgoingMetadata?.let { metadata ->
                MiniMetadataColumn(
                    metadata = metadata,
                    titleColor = titleColor,
                    artistColor = artistColor,
                    alpha = 1f - crossfadeProgress,
                    titleStyle = titleStyle,
                    artistStyle = artistStyle,
                    contentStartPadding = contentStartPadding,
                )
            }
            MiniMetadataColumn(
                metadata = visibleMetadata,
                titleColor = titleColor,
                artistColor = artistColor,
                alpha = if (outgoingMetadata == null) 1f else crossfadeProgress,
                titleStyle = titleStyle,
                artistStyle = artistStyle,
                contentStartPadding = contentStartPadding,
                subtitleContent = subtitleContent,
            )
        }
    }
}

private val MiniMetadataEdgeMaskWidth = 4.dp
private val MiniMetadataLabelSpacing = 12.dp
private const val MiniMetadataReturnDurationMillis = 160

internal fun miniPlayerHeight(style: BottomBarStyle, smallPlayerBar: Boolean): Dp =
    if (style == BottomBarStyle.OS4) {
        52.dp
    } else if (style == BottomBarStyle.NORMAL) {
        if (smallPlayerBar) 54.dp else 68.dp
    } else {
        if (smallPlayerBar) 48.dp else 64.dp
    }

internal val FLOATING_MINI_PLAYER_BOTTOM_PADDING = 8.dp

internal fun miniPlayerControlSize(): Dp = IconButtonDefaults.MinWidth

internal fun miniPlayerArtworkSize(normalChrome: Boolean, smallPlayerBar: Boolean): Dp = when {
    !normalChrome && smallPlayerBar -> 32.dp
    smallPlayerBar -> 36.dp
    else -> miniPlayerHeight(
        if (normalChrome) BottomBarStyle.NORMAL else BottomBarStyle.FLOATING,
        false,
    ) - 20.dp
}

internal fun miniPlayerArtworkStartPadding(
    normalChrome: Boolean,
    smallPlayerBar: Boolean,
    circular: Boolean,
): Dp =
    when {
        normalChrome -> 10.dp
        circular -> {
            val height = miniPlayerHeight(BottomBarStyle.FLOATING, smallPlayerBar)
            (height - miniPlayerArtworkSize(false, smallPlayerBar)) / 2
        }
        smallPlayerBar -> 14.dp
        else -> 16.dp
    }

internal fun miniPlayerArtworkMetadataSpacing(
    normalChrome: Boolean,
    smallPlayerBar: Boolean,
): Dp = when {
    normalChrome -> 6.dp
    smallPlayerBar -> 4.dp
    else -> 8.dp
}

internal fun miniPlayerQueueOffset(normalChrome: Boolean, smallPlayerBar: Boolean): Dp = when {
    normalChrome -> 0.dp
    smallPlayerBar -> 0.dp
    else -> (-2).dp
}

internal fun miniPlayerArtworkCornerRadius(
    normalChrome: Boolean,
    smallPlayerBar: Boolean,
    circular: Boolean = false,
): Dp =
    when {
        circular -> miniPlayerArtworkSize(normalChrome, smallPlayerBar) / 2
        normalChrome -> 7.dp
        smallPlayerBar -> 6.dp
        else -> 8.dp
    }

private data class MiniPlayerMetadata(
    val mediaId: String?,
    val title: String,
    val artist: String,
)

internal fun hasExpectedMiniMetadataSwipeTarget(
    expectedMediaId: String?,
    displayedMediaId: String?,
): Boolean = expectedMediaId != null && expectedMediaId == displayedMediaId

@Composable
private fun MiniMetadataColumn(
    metadata: MiniPlayerMetadata,
    titleColor: Color,
    artistColor: Color,
    alpha: Float,
    titleStyle: TextStyle,
    artistStyle: TextStyle,
    contentStartPadding: Dp,
    subtitleContent: (@Composable (String?, String, TextStyle, Color, Dp) -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha },
    ) {
        Text(
            text = metadata.title,
            modifier = Modifier.padding(start = contentStartPadding),
            style = titleStyle,
            color = titleColor,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitleContent != null) {
            subtitleContent(metadata.mediaId, metadata.artist, artistStyle, artistColor, contentStartPadding)
        } else {
            Text(
                text = metadata.artist,
                modifier = Modifier.padding(start = contentStartPadding),
                style = artistStyle,
                color = artistColor,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun PlaybackQueueItem?.toMiniPlayerMetadata(
    emptyTitle: String,
    emptyArtist: String,
    unknownArtist: String,
): MiniPlayerMetadata = if (this == null) {
    MiniPlayerMetadata(
        mediaId = null,
        title = emptyTitle,
        artist = emptyArtist,
    )
} else {
    MiniPlayerMetadata(
        mediaId = mediaId,
        title = title,
        artist = artist ?: unknownArtist,
    )
}

private fun PlaybackUiState.targetMetadataForSwipe(
    offset: Float,
    emptyTitle: String,
    emptyArtist: String,
    unknownArtist: String,
): MiniPlayerMetadata? {
    if (offset == 0f || queue.isEmpty() || currentIndex !in queue.indices) return null
    val targetIndex = if (offset > 0f) {
        (currentIndex - 1 + queue.size) % queue.size
    } else {
        (currentIndex + 1) % queue.size
    }
    return queue.getOrNull(targetIndex)?.toMiniPlayerMetadata(
        emptyTitle = emptyTitle,
        emptyArtist = emptyArtist,
        unknownArtist = unknownArtist,
    )
}

internal fun PlaybackUiState.hasDifferentMetadataSwipeTarget(offset: Float): Boolean {
    val currentMediaId = currentItem?.mediaId ?: return false
    if (offset == 0f || queue.isEmpty() || currentIndex !in queue.indices) return false
    val targetIndex = if (offset > 0f) {
        (currentIndex - 1 + queue.size) % queue.size
    } else {
        (currentIndex + 1) % queue.size
    }
    return queue.getOrNull(targetIndex)?.mediaId != currentMediaId
}

internal fun miniMetadataSwipeCommitThreshold(
    contentWidthPx: Float,
    labelWidthPx: Float,
    edgeMaskWidthPx: Float,
    labelSpacingPx: Float,
): Float {
    val visibleLabelWidth = if (labelWidthPx > 0f) {
        labelWidthPx
    } else {
        contentWidthPx * 0.24f
    }
    return max(
        visibleLabelWidth + edgeMaskWidthPx + labelSpacingPx,
        1f,
    ).coerceAtMost(contentWidthPx.coerceAtLeast(1f))
}

internal fun miniMetadataSwipeCommits(
    offsetPx: Float,
    contentWidthPx: Float,
    labelWidthPx: Float,
    edgeMaskWidthPx: Float,
    labelSpacingPx: Float,
): Boolean = abs(offsetPx) >= miniMetadataSwipeCommitThreshold(
    contentWidthPx = contentWidthPx,
    labelWidthPx = labelWidthPx,
    edgeMaskWidthPx = edgeMaskWidthPx,
    labelSpacingPx = labelSpacingPx,
)

internal fun miniMetadataSwipeThresholdDirection(
    offsetPx: Float,
    commits: Boolean,
    hasDifferentTarget: Boolean,
): Int = when {
    !commits || !hasDifferentTarget -> 0
    offsetPx > 0f -> 1
    offsetPx < 0f -> -1
    else -> 0
}

internal fun shouldTriggerMiniMetadataSwipeThresholdHaptic(
    previousDirection: Int,
    currentDirection: Int,
): Boolean = currentDirection != 0 && currentDirection != previousDirection
