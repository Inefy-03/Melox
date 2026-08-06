package com.melox.player.ui.component.playback

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melox.player.R
import com.melox.player.model.BottomBarStyle
import com.melox.player.model.PlaybackQueueItem
import com.melox.player.model.PlaybackUiState
import com.melox.player.ui.MiniPlayerChrome
import com.melox.player.ui.NORMAL_BAR_STROKE_ALPHA
import com.melox.player.ui.component.library.PlaybackArtwork
import com.melox.player.ui.component.liquid.miuixFloatingBarShadow
import com.melox.player.ui.component.liquid.miniPlayerSurface
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.DividerDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Playlist
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MiniPlayer(
    playback: PlaybackUiState,
    chrome: MiniPlayerChrome,
    onOpen: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenQueue: () -> Unit,
    onPlayerDragStart: () -> Unit,
    onPlayerDrag: (Float) -> Unit,
    onPlayerDragEnd: (Float) -> Unit,
    onPlayerDragCancel: () -> Unit,
    playerLayer: GraphicsLayer,
    drawInPlace: Boolean,
    surfaceVisible: Boolean,
    sharedArtworkVisible: Boolean,
    onPlayerBoundsChanged: (Rect) -> Unit,
    onArtworkBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = playback.currentItem
    val hasItem = item != null
    val isNormal = chrome.style == BottomBarStyle.NORMAL
    val surfaceCornerRadius = if (isNormal) 18.dp else 32.dp
    val artworkCornerRadius = if (isNormal) 7.dp else 8.dp
    val shape = RoundedCornerShape(surfaceCornerRadius)
    val normalOutlineColor = DividerDefaults.DividerColor.copy(
        alpha = NORMAL_BAR_STROKE_ALPHA,
    )
    val artworkSize = if (isNormal) 48.dp else 44.dp
    val metadataSpacing = if (isNormal) 6.dp else 8.dp
    val controlSize = 40.dp
    val playPauseIconSize = 22.dp
    val controlIconSize = 24.dp
    val artworkShape = RoundedCornerShape(artworkCornerRadius)
    val expansionGestureModifier = rememberPlayerSheetVerticalDragModifier(
        enabled = true,
        hasItem = hasItem,
        onDragStart = onPlayerDragStart,
        onDrag = onPlayerDrag,
        onDragEnd = onPlayerDragEnd,
        onDragCancel = onPlayerDragCancel,
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (isNormal) 6.dp else 0.dp)
            .padding(bottom = if (isNormal) 6.dp else 8.dp)
            .height(if (isNormal) 68.dp else 64.dp)
            .onGloballyPositioned { coordinates ->
                onPlayerBoundsChanged(coordinates.boundsInRoot())
            }
            .recordPlayerLayer(
                layer = playerLayer,
                drawInPlace = drawInPlace,
            )
            .then(expansionGestureModifier)
            .clickable(
                enabled = hasItem,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpen,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (surfaceVisible) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(
                        if (isNormal) {
                            Modifier
                        } else {
                            Modifier.miuixFloatingBarShadow(
                                shape = shape,
                                isDark = chrome.isDark,
                            )
                        },
                    )
                    .clip(shape)
                    .miniPlayerSurface(
                        shape = shape,
                        backdrop = chrome.backdrop,
                        blurActive = chrome.blurActive,
                        liquidGlassActive = chrome.liquidGlassActive,
                        isDark = chrome.isDark,
                        followsNavigationBar = isNormal,
                        floatingHighlight = chrome.floatingHighlight,
                    )
                    .then(
                        if (isNormal) {
                            Modifier.border(
                                width = DividerDefaults.Thickness,
                                color = normalOutlineColor,
                                shape = shape,
                            )
                        } else {
                            Modifier
                        },
                    ),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = if (isNormal) 10.dp else 16.dp,
                    end = 10.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(artworkSize)
                    .onGloballyPositioned { coordinates ->
                        onArtworkBoundsChanged(coordinates.boundsInRoot())
                    }
                    .graphicsLayer {
                        alpha = if (sharedArtworkVisible) 1f else 0f
                        this.shape = artworkShape
                        clip = false
                    },
            ) {
                PlaybackArtwork(
                    contentUri = item?.contentUri.orEmpty(),
                    dateModifiedEpochSeconds = item?.dateModifiedEpochSeconds ?: 0L,
                    fileSizeBytes = item?.fileSizeBytes ?: 0L,
                    size = artworkSize,
                    cornerRadius = artworkCornerRadius,
                    modifier = Modifier.fillMaxSize(),
                    requestSize = artworkSize,
                    contentScale = ContentScale.Fit,
                    bitmapCrossfadeDurationMillis =
                        PLAYER_TRACK_ARTWORK_CROSSFADE_DURATION_MILLIS,
                    bitmapCrossfadeEasing = PLAYER_TRACK_ARTWORK_CROSSFADE_EASING,
                    rectangularCornerRadiusReduction =
                        MINI_PLAYER_RECTANGULAR_ARTWORK_CORNER_REDUCTION,
                )
            }
            Spacer(modifier = Modifier.width(metadataSpacing))
            SwipeableMetadata(
                playback = playback,
                onPrevious = onPrevious,
                onNext = onNext,
                modifier = Modifier.weight(1f),
                contentStartPadding = 4.dp,
            )
            Spacer(modifier = Modifier.width(if (isNormal) 6.dp else 2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    modifier = Modifier.offset(x = (-6).dp),
                    enabled = hasItem,
                    onClick = onTogglePlayPause,
                    minWidth = controlSize,
                    minHeight = controlSize,
                ) {
                    AnimatedContent(
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
                IconButton(
                    modifier = Modifier.offset(x = if (isNormal) 0.dp else (-2).dp),
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

@Composable
private fun SwipeableMetadata(
    playback: PlaybackUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    contentStartPadding: Dp = 0.dp,
) {
    val item = playback.currentItem
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
        if (expectedSwipeMediaId == liveMetadata.mediaId) {
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
                detectHorizontalDragGestures(
                    onDragStart = {
                        settleJob?.cancel()
                        thresholdHapticDirection = 0
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
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
                .graphicsLayer { translationX = horizontalOffset }
                .padding(start = contentStartPadding),
        ) {
            outgoingMetadata?.let { metadata ->
                MiniMetadataColumn(
                    metadata = metadata,
                    titleColor = titleColor,
                    artistColor = artistColor,
                    alpha = 1f - crossfadeProgress,
                )
            }
            MiniMetadataColumn(
                metadata = visibleMetadata,
                titleColor = titleColor,
                artistColor = artistColor,
                alpha = if (outgoingMetadata == null) 1f else crossfadeProgress,
            )
        }
    }
}

private val MiniMetadataEdgeMaskWidth = 4.dp
private val MiniMetadataLabelSpacing = 12.dp
private const val MiniMetadataReturnDurationMillis = 160

private data class MiniPlayerMetadata(
    val mediaId: String?,
    val title: String,
    val artist: String,
)

@Composable
private fun MiniMetadataColumn(
    metadata: MiniPlayerMetadata,
    titleColor: Color,
    artistColor: Color,
    alpha: Float,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = metadata.title,
            modifier = Modifier,
            style = MiuixTheme.textStyles.body1.copy(fontSize = 15.sp),
            color = titleColor,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = metadata.artist,
            modifier = Modifier,
            style = MiuixTheme.textStyles.footnote1,
            color = artistColor,
            textAlign = TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
