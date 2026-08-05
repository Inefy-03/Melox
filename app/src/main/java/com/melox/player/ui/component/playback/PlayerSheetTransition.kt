package com.melox.player.ui.component.playback

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.melox.player.model.PlaybackUiState
import com.melox.player.ui.component.library.PlaybackArtworkFrame
import com.melox.player.ui.component.library.playbackArtworkCornerRadius
import com.melox.player.ui.component.library.rememberArtworkBitmap
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.utils.getRoundedCorner

internal const val PLAYER_TRACK_ARTWORK_CROSSFADE_DURATION_MILLIS = 320
internal const val PLAYER_LAYER_HANDOFF_END_PROGRESS = 0.4f
internal const val PLAYER_SCREEN_CORNER_EXPANSION_DURATION_MILLIS = 140
private const val PLAYER_ARTWORK_VERTICAL_LINEAR_WEIGHT = 0.4f
private const val PLAYER_MINI_PLAYER_INPUT_ALPHA_THRESHOLD = 0.8f

internal val PLAYER_FULL_ARTWORK_REQUEST_SIZE = 420.dp
internal val MINI_PLAYER_RECTANGULAR_ARTWORK_CORNER_REDUCTION = 1.dp
internal val PLAYER_TRACK_ARTWORK_CROSSFADE_EASING = androidx.compose.animation.core.FastOutSlowInEasing

/**
 * Owns the one progress value shared by the mini player, full player, and
 * artwork overlay. The spring mirrors VMusic's endpoint-driven container
 * motion while allowing a close gesture to reverse from its current frame.
 */
@Stable
internal class PlayerSheetTransitionState {
    private val progressAnimation = Animatable(0f, visibilityThreshold = 0.001f)
    private var dragProgress by mutableFloatStateOf(0f)
    private var dragStartProgress = 0f
    private var dragDistanceY = 0f
    private var lastDragAmountY = 0f
    private var dragOriginOpen = false
    private var requestedInitialVelocity = 0f
    private var cornerExpansionProgress by mutableFloatStateOf(0f)

    var targetOpen by mutableStateOf(false)
        private set

    var isDragging by mutableStateOf(false)
        private set

    var animationRequest by mutableIntStateOf(0)
        private set

    var miniPlayerBounds by mutableStateOf(Rect.Zero)
        private set

    var fullPlayerBounds by mutableStateOf(Rect.Zero)
        private set

    var miniArtworkBounds by mutableStateOf(Rect.Zero)
        private set

    var fullArtworkBounds by mutableStateOf(Rect.Zero)
        private set

    val progress: Float
        get() = if (isDragging) dragProgress else progressAnimation.value.coerceIn(0f, 1f)

    val hasArtworkBounds: Boolean
        get() = miniArtworkBounds.isUsable() && fullArtworkBounds.isUsable()

    val hasContainerBounds: Boolean
        get() = miniPlayerBounds.isUsable() && fullPlayerBounds.isUsable()

    val isReady: Boolean
        get() = hasContainerBounds && hasArtworkBounds

    val isMounted: Boolean
        get() = isDragging || targetOpen || progress > 0f

    val isTransitionActive: Boolean
        get() = isDragging || if (targetOpen) {
            progress < 1f || cornerExpansionProgress < 1f
        } else {
            progress > 0f || cornerExpansionProgress > 0f
        }

    val miniPlayerAcceptsInput: Boolean
        get() = playerSheetMiniPlayerAcceptsInput(
            targetOpen = targetOpen,
            isDragging = isDragging,
            dragOriginOpen = dragOriginOpen,
            progress = progress,
        )

    val screenCornerExpansionProgress: Float
        get() = cornerExpansionProgress

    fun open() {
        requestSettle(open = true)
    }

    fun close() {
        requestSettle(open = false)
    }

    fun beginDrag() {
        if (isDragging) return
        dragStartProgress = progress
        dragProgress = dragStartProgress
        dragDistanceY = 0f
        lastDragAmountY = 0f
        dragOriginOpen = targetOpen
        cornerExpansionProgress = 0f
        isDragging = true
        animationRequest += 1
    }

    fun dragBy(dragAmountY: Float) {
        if (!isDragging) return
        dragDistanceY += dragAmountY
        if (dragAmountY != 0f) lastDragAmountY = dragAmountY
        updateDragProgress()
    }

    suspend fun endDrag(velocityY: Float) {
        if (!isDragging) return
        settleDrag(
            open = playerSheetDragTarget(
                velocityY = velocityY,
                lastDragAmountY = lastDragAmountY,
                originOpen = dragOriginOpen,
            ),
            velocityY = velocityY,
        )
    }

    suspend fun cancelDrag() {
        if (!isDragging) return
        settleDrag(open = dragOriginOpen, velocityY = 0f)
    }

    fun updateMiniPlayerBounds(bounds: Rect) {
        if (bounds.isUsable()) miniPlayerBounds = bounds
    }

    fun updateFullPlayerBounds(bounds: Rect) {
        if (bounds.isUsable()) {
            fullPlayerBounds = bounds
            updateDragProgress()
        }
    }

    fun updateMiniArtworkBounds(bounds: Rect) {
        if (bounds.isUsable()) miniArtworkBounds = bounds
    }

    fun updateFullArtworkBounds(bounds: Rect) {
        if (bounds.isUsable()) fullArtworkBounds = bounds
    }

    internal suspend fun animateToTarget() {
        if (progress < 1f) {
            cornerExpansionProgress = 0f
        }
        val visibilityThreshold = 0.5f / fullPlayerBounds.height.coerceAtLeast(1f)
        if (!targetOpen) {
            animateScreenCornersTo(0f)
            withFrameNanos { }
        }
        progressAnimation.animateTo(
            targetValue = if (targetOpen) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = 300f,
                visibilityThreshold = visibilityThreshold,
            ),
            initialVelocity = requestedInitialVelocity,
        )
        if (targetOpen) {
            withFrameNanos { }
            animateScreenCornersTo(1f)
        } else {
            cornerExpansionProgress = 0f
            fullPlayerBounds = Rect.Zero
            fullArtworkBounds = Rect.Zero
        }
    }

    private fun updateDragProgress() {
        if (!isDragging || !fullPlayerBounds.isUsable()) return
        dragProgress = playerSheetDragProgress(
            startProgress = dragStartProgress,
            dragDistanceY = dragDistanceY,
            containerHeight = fullPlayerBounds.height,
        )
    }

    private suspend fun settleDrag(open: Boolean, velocityY: Float) {
        val containerHeight = fullPlayerBounds.height.coerceAtLeast(1f)
        progressAnimation.snapTo(dragProgress)
        isDragging = false
        requestSettle(
            open = open,
            initialVelocity = -velocityY / containerHeight,
        )
    }

    private fun requestSettle(open: Boolean, initialVelocity: Float = 0f) {
        if (progress < 1f) {
            cornerExpansionProgress = 0f
        }
        targetOpen = open
        requestedInitialVelocity = initialVelocity
        animationRequest += 1
    }

    private suspend fun animateScreenCornersTo(target: Float) {
        if (cornerExpansionProgress == target) return
        val animation = Animatable(cornerExpansionProgress)
        animation.animateTo(
            targetValue = target,
            animationSpec = tween(
                durationMillis = PLAYER_SCREEN_CORNER_EXPANSION_DURATION_MILLIS,
                easing = EaseOut,
            ),
        ) {
            cornerExpansionProgress = value
        }
        cornerExpansionProgress = target
    }
}

@Composable
internal fun rememberPlayerSheetTransitionState(): PlayerSheetTransitionState = remember {
    PlayerSheetTransitionState()
}

internal fun playerSheetBarAlpha(progress: Float): Float {
    val handoff = (progress.coerceIn(0f, 1f) / PLAYER_LAYER_HANDOFF_END_PROGRESS)
        .coerceIn(0f, 1f)
    return 1f - easeOutCubic(handoff)
}

internal fun playerSheetPageAlpha(progress: Float): Float {
    val handoff = (progress.coerceIn(0f, 1f) / PLAYER_LAYER_HANDOFF_END_PROGRESS)
        .coerceIn(0f, 1f)
    return easeInCubic(handoff)
}

internal fun playerSheetMiniPlayerAcceptsInput(
    targetOpen: Boolean,
    isDragging: Boolean,
    dragOriginOpen: Boolean,
    progress: Float,
): Boolean = !targetOpen && if (isDragging) {
    !dragOriginOpen
} else {
    playerSheetBarAlpha(progress) >= PLAYER_MINI_PLAYER_INPUT_ALPHA_THRESHOLD
}

internal fun Modifier.recordPlayerLayer(
    layer: GraphicsLayer,
    drawInPlace: Boolean,
): Modifier = drawWithContent {
    layer.record {
        this@drawWithContent.drawContent()
    }
    if (drawInPlace) {
        layer.alpha = 1f
        drawLayer(layer)
    }
}

/**
 * Shared container overlay. Mini-player and full-player content are recorded
 * independently, then drawn inside one expanding squircle. This mirrors
 * VMusic's layer handoff instead of fading a fixed bar under a fixed screen.
 */
@Composable
internal fun PlayerSheetContentOverlay(
    transition: PlayerSheetTransitionState,
    miniPlayerLayer: GraphicsLayer,
    fullPlayerLayer: GraphicsLayer,
    collapsedCornerRadius: Dp,
    floatingMiniPlayer: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!transition.isReady || !transition.isTransitionActive) return

    val progress = transition.progress
    val bounds = sharedContainerRect(
        source = transition.miniPlayerBounds,
        target = transition.fullPlayerBounds,
        progress = progress,
    )
    val density = LocalDensity.current
    val deviceCornerRadius = getRoundedCorner()
    val expandedCornerRadius = if (rememberPlayerWindowUsesPhysicalScreenCorners()) {
        deviceCornerRadius
    } else {
        0.dp
    }
    val cornerRadius = sharedContainerCornerRadius(
        collapsedCornerRadius = collapsedCornerRadius.value,
        expandedCornerRadius = expandedCornerRadius.value,
        progress = progress,
        screenCornerExpansionProgress = transition.screenCornerExpansionProgress,
    ).dp
    val miniShadowAlpha = if (floatingMiniPlayer) {
        val baseAlpha = if (isDark) 0.2f else 0.1f
        baseAlpha * playerSheetBarAlpha(progress)
    } else {
        0f
    }

    Canvas(
        modifier = modifier
            .offset {
                IntOffset(
                    bounds.left.roundToInt(),
                    bounds.top.roundToInt(),
                )
            }
            .size(
                width = with(density) { bounds.width.coerceAtLeast(1f).toDp() },
                height = with(density) { bounds.height.coerceAtLeast(1f).toDp() },
            )
            .then(
                if (miniShadowAlpha > 0f) {
                    Modifier.dropShadow(
                        shape = RoundedCornerShape(cornerRadius),
                        shadow = Shadow(
                            radius = 10.dp,
                            color = Color.Black,
                            alpha = miniShadowAlpha,
                        ),
                    )
                } else {
                    Modifier
                },
            )
            .squircleClip(cornerRadius),
    ) {
        if (miniPlayerLayer.size.width > 0 && progress < PLAYER_LAYER_HANDOFF_END_PROGRESS) {
            val scale = size.width / miniPlayerLayer.size.width
            miniPlayerLayer.alpha = playerSheetBarAlpha(progress)
            withTransform({
                scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
            }) {
                drawLayer(miniPlayerLayer)
            }
        }
        if (fullPlayerLayer.size.width > 0 && progress > 0f) {
            val scale = size.width / fullPlayerLayer.size.width
            fullPlayerLayer.alpha = playerSheetPageAlpha(progress)
            withTransform({
                scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
            }) {
                drawLayer(fullPlayerLayer)
            }
        }
    }
}

/**
 * Shared cover overlay. The layout is anchored at the mini-player bounds and
 * uses uniform scale plus translation to reach the full-player bounds. The
 * center path follows VMusic's staged feel: horizontal travel is front-loaded,
 * while the cover rises throughout and vertical travel dominates after halfway.
 */
@Composable
internal fun PlayerSheetArtworkOverlay(
    playback: PlaybackUiState,
    transition: PlayerSheetTransitionState,
    collapsedCornerRadius: Dp,
    modifier: Modifier = Modifier,
) {
    val item = playback.currentItem ?: return
    val progress = transition.progress
    if (!transition.isReady || !transition.isTransitionActive) return

    val bitmap = rememberArtworkBitmap(
        contentUri = item.contentUri,
        dateModifiedEpochSeconds = item.dateModifiedEpochSeconds,
        fileSizeBytes = item.fileSizeBytes,
        size = PLAYER_FULL_ARTWORK_REQUEST_SIZE,
        priorityLoad = true,
    )
    val density = LocalDensity.current
    val source = transition.miniArtworkBounds
    val target = transition.fullArtworkBounds
    val sourceArtworkBounds = bitmap?.let {
        fittedArtworkRect(source, it.width, it.height)
    }
    val sourceBounds = sourceArtworkBounds ?: source
    val renderedBounds = sharedArtworkRect(
        source = sourceBounds,
        target = target,
        progress = progress,
    )
    val collapsedArtworkCornerRadius = bitmap?.let {
        playbackArtworkCornerRadius(
            cornerRadius = collapsedCornerRadius,
            bitmapWidth = it.width,
            bitmapHeight = it.height,
            rectangularReduction = MINI_PLAYER_RECTANGULAR_ARTWORK_CORNER_REDUCTION,
        )
    } ?: collapsedCornerRadius
    val artworkCornerRadius = lerp(
        collapsedArtworkCornerRadius.value,
        12f,
        progress,
    )

    if (bitmap == null) {
        val scaleX = renderedBounds.width / source.width.coerceAtLeast(1f)
        val scaleY = renderedBounds.height / source.height.coerceAtLeast(1f)
        val localCornerRadius = (artworkCornerRadius / scaleX.coerceAtLeast(1f)).dp
        Box(
            modifier = modifier
                .offset {
                    IntOffset(
                        source.left.roundToInt(),
                        source.top.roundToInt(),
                    )
                }
                .size(
                    with(density) { source.width.coerceAtLeast(1f).toDp() },
                )
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    this.scaleX = scaleX
                    this.scaleY = scaleY
                    translationX = renderedBounds.left - source.left
                    translationY = renderedBounds.top - source.top
                },
        ) {
            PlaybackArtworkFrame(
                bitmap = null,
                size = with(density) { source.width.coerceAtLeast(1f).toDp() },
                cornerRadius = localCornerRadius,
                modifier = Modifier,
                contentScale = ContentScale.Fit,
            )
        }
    } else {
        val targetBounds = target
        val targetArtworkWidth = targetBounds.width.coerceAtLeast(1f)
        val targetArtworkHeight = targetBounds.height.coerceAtLeast(1f)
        val scaleX = renderedBounds.width / targetArtworkWidth
        val scaleY = renderedBounds.height / targetArtworkHeight
        val localCornerRadius = (artworkCornerRadius / scaleX.coerceAtLeast(0.001f)).dp
        Box(
            modifier = modifier
                .offset {
                    IntOffset(
                        targetBounds.left.roundToInt(),
                        targetBounds.top.roundToInt(),
                    )
                }
                .size(
                    width = with(density) { targetArtworkWidth.toDp() },
                    height = with(density) { targetArtworkHeight.toDp() },
                )
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    this.scaleX = scaleX
                    this.scaleY = scaleY
                    translationX = renderedBounds.left - targetBounds.left
                    translationY = renderedBounds.top - targetBounds.top
                },
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(localCornerRadius)),
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.High,
            )
        }
    }
}

internal fun sharedContainerRect(
    source: Rect,
    target: Rect,
    progress: Float,
): Rect {
    val fraction = progress.coerceIn(0f, 1f)
    val centerX = lerp(source.center.x, target.center.x, fraction)
    val centerY = lerp(source.center.y, target.center.y, fraction)
    val width = lerp(source.width, target.width, easeInCubic(fraction))
    val height = lerp(source.height, target.height, fraction)
    return Rect(
        left = centerX - width / 2f,
        top = centerY - height / 2f,
        right = centerX + width / 2f,
        bottom = centerY + height / 2f,
    )
}

internal fun sharedContainerCornerRadius(
    collapsedCornerRadius: Float,
    expandedCornerRadius: Float,
    progress: Float,
    screenCornerExpansionProgress: Float,
): Float {
    val fraction = progress.coerceIn(0f, 1f)
    val screenRoundedCorner = lerp(collapsedCornerRadius, expandedCornerRadius, fraction)
    val cornerExpansionFraction = if (fraction < 1f) {
        0f
    } else {
        screenCornerExpansionProgress
    }
    return lerp(screenRoundedCorner, 0f, cornerExpansionFraction)
}

internal fun playerWindowUsesPhysicalScreenCorners(
    currentWidth: Int,
    currentHeight: Int,
    maximumWidth: Int,
    maximumHeight: Int,
    isInMultiWindowMode: Boolean,
    isInPictureInPictureMode: Boolean,
): Boolean = !isInMultiWindowMode &&
    !isInPictureInPictureMode &&
    currentWidth >= maximumWidth &&
    currentHeight >= maximumHeight

internal fun sharedArtworkRect(
    source: Rect,
    target: Rect,
    progress: Float,
): Rect {
    val fraction = progress.coerceIn(0f, 1f)
    val centerX = lerp(source.center.x, target.center.x, easeOutCubic(fraction))
    val verticalFraction = lerp(
        easeInCubic(fraction),
        fraction,
        PLAYER_ARTWORK_VERTICAL_LINEAR_WEIGHT,
    )
    val centerY = lerp(source.center.y, target.center.y, verticalFraction)
    val sourceWidth = source.width.coerceAtLeast(1f)
    val targetWidth = target.width.coerceAtLeast(sourceWidth)
    val scale = lerp(1f, targetWidth / sourceWidth, fraction)
    val width = sourceWidth * scale
    val height = source.height.coerceAtLeast(1f) * scale
    return Rect(
        left = centerX - width / 2f,
        top = centerY - height / 2f,
        right = centerX + width / 2f,
        bottom = centerY + height / 2f,
    )
}

internal fun fittedArtworkRect(
    bounds: Rect,
    bitmapWidth: Int,
    bitmapHeight: Int,
): Rect {
    val width = bitmapWidth.coerceAtLeast(1).toFloat()
    val height = bitmapHeight.coerceAtLeast(1).toFloat()
    val scale = minOf(bounds.width / width, bounds.height / height)
    val fittedWidth = width * scale
    val fittedHeight = height * scale
    return Rect(
        left = bounds.center.x - fittedWidth / 2f,
        top = bounds.center.y - fittedHeight / 2f,
        right = bounds.center.x + fittedWidth / 2f,
        bottom = bounds.center.y + fittedHeight / 2f,
    )
}

internal fun scaledRectAroundCenter(
    bounds: Rect,
    scale: Float,
): Rect {
    val halfWidth = bounds.width * scale / 2f
    val halfHeight = bounds.height * scale / 2f
    return Rect(
        left = bounds.center.x - halfWidth,
        top = bounds.center.y - halfHeight,
        right = bounds.center.x + halfWidth,
        bottom = bounds.center.y + halfHeight,
    )
}

internal fun playerSheetDragProgress(
    startProgress: Float,
    dragDistanceY: Float,
    containerHeight: Float,
): Float {
    if (containerHeight <= 0f) return startProgress.coerceIn(0f, 1f)
    return (startProgress - dragDistanceY / containerHeight).coerceIn(0f, 1f)
}

internal fun playerSheetDragTarget(
    velocityY: Float,
    lastDragAmountY: Float,
    originOpen: Boolean,
): Boolean = when {
    velocityY < 0f -> true
    velocityY > 0f -> false
    lastDragAmountY < 0f -> true
    lastDragAmountY > 0f -> false
    else -> originOpen
}

private fun Rect.isUsable(): Boolean = width > 0f && height > 0f

@Composable
private fun rememberPlayerWindowUsesPhysicalScreenCorners(): Boolean {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(
        context,
        configuration.screenWidthDp,
        configuration.screenHeightDp,
    ) {
        context.playerWindowUsesPhysicalScreenCorners()
    }
}

private fun Context.playerWindowUsesPhysicalScreenCorners(): Boolean {
    val activity = findActivity() ?: return false
    if (activity.isInMultiWindowMode || activity.isInPictureInPictureMode) return false
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return true

    val currentBounds = activity.windowManager.currentWindowMetrics.bounds
    val maximumBounds = activity.windowManager.maximumWindowMetrics.bounds
    return playerWindowUsesPhysicalScreenCorners(
        currentWidth = currentBounds.width(),
        currentHeight = currentBounds.height(),
        maximumWidth = maximumBounds.width(),
        maximumHeight = maximumBounds.height(),
        isInMultiWindowMode = false,
        isInPictureInPictureMode = false,
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction.coerceIn(0f, 1f)

private fun easeInCubic(value: Float): Float {
    val clamped = value.coerceIn(0f, 1f)
    return clamped * clamped * clamped
}

private fun easeOutCubic(value: Float): Float {
    val clamped = value.coerceIn(0f, 1f)
    val inverse = 1f - clamped
    return 1f - inverse * inverse * inverse
}
