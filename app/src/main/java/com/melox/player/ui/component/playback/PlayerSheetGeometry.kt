package com.melox.player.ui.component.playback

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import com.melox.player.model.PlaybackUiState
import com.melox.player.model.BottomBarStyle
import com.melox.player.ui.MiniPlayerChrome
import com.melox.player.ui.NORMAL_BAR_STROKE_ALPHA
import com.melox.player.ui.component.library.PlaybackArtworkFrame
import com.melox.player.ui.component.library.playbackArtworkShadow
import com.melox.player.ui.component.library.playbackArtworkCornerRadius
import com.melox.player.ui.component.library.rememberArtworkBitmap
import com.melox.player.ui.component.liquid.miuixFloatingBarShadow
import com.melox.player.ui.component.liquid.miniPlayerSurface
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import androidx.compose.runtime.saveable.Saver
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.squircle.addSquircleRect
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.basic.DividerDefaults
import top.yukonga.miuix.kmp.utils.getRoundedCorner

internal fun scaledMiniPlayerBounds(bounds: Rect, playerBounds: Rect, scale: Float): Rect {
    if (!bounds.isUsable() || !playerBounds.isUsable()) return bounds
    val center = playerBounds.center
    return Rect(
        left = center.x + (bounds.left - center.x) * scale,
        top = center.y + (bounds.top - center.y) * scale,
        right = center.x + (bounds.right - center.x) * scale,
        bottom = center.y + (bounds.bottom - center.y) * scale,
    )
}

internal fun sharedContainerRect(
    source: Rect,
    target: Rect,
    progress: Float,
): Rect {
    val fraction = progress.coerceIn(0f, 1f)
    return Rect(
        left = lerp(source.left, target.left, fraction),
        top = lerp(source.top, target.top, fraction),
        right = lerp(source.right, target.right, fraction),
        bottom = lerp(source.bottom, target.bottom, fraction),
    )
}

internal fun playbackArtworkBounds(
    bounds: Rect,
    bitmapWidth: Int,
    bitmapHeight: Int,
    circular: Boolean,
): Rect = if (circular) bounds else fittedArtworkRect(bounds, bitmapWidth, bitmapHeight)

internal fun playerSheetUsesFullPlayerStatusBar(
    progress: Float,
): Boolean = progress >= 0.96f

internal fun Modifier.playerSheetHostLayer(
    hostBounds: Rect,
    inputBounds: Rect?,
    miniPlayerBounds: Rect?,
    residentTranslationY: Float = 0f,
    inputCornerRadiusPx: Float = 0f,
    squircleEnabled: Boolean = false,
): Modifier = graphicsLayer {
    // Keep recording coordinates stable; only the separate input surface moves.
    translationY = residentTranslationY
    clip = true
    shape = if (inputBounds != null) {
        PlayerSheetInputShape(
            bounds = inputBounds.translate(-hostBounds.topLeft),
            excludedBounds = miniPlayerBounds?.translate(-hostBounds.topLeft),
            cornerRadiusPx = inputCornerRadiusPx,
            squircleEnabled = squircleEnabled,
        )
    } else {
        RectangleShape
    }
}

internal class PlayerSheetInputShape(
    private val bounds: Rect,
    private val excludedBounds: Rect?,
    private val cornerRadiusPx: Float = 0f,
    private val squircleEnabled: Boolean = false,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        if (!bounds.isUsable()) return Outline.Rectangle(Rect.Zero)
        val radius = cornerRadiusPx.coerceIn(0f, minOf(bounds.width, bounds.height) / 2f)
        val roundedBounds = RoundRect(bounds, CornerRadius(radius))
        if (!squircleEnabled && excludedBounds == null) {
            return if (radius > 0f) Outline.Rounded(roundedBounds) else Outline.Rectangle(bounds)
        }
        val outer = Path().apply {
            if (squircleEnabled) {
                addSquircleRect(bounds.width, bounds.height, radius)
                translate(bounds.topLeft)
            } else {
                addRoundRect(roundedBounds)
            }
        }
        val excluded = excludedBounds ?: return Outline.Generic(outer)
        val inner = Path().apply { addRect(excluded) }
        return Outline.Generic(Path.combine(PathOperation.Difference, outer, inner))
    }
}

internal fun sharedContainerRenderRect(
    source: Rect,
    target: Rect,
    progress: Float,
    endpointOverscanPx: Float = 0f,
): Rect {
    val bounds = sharedContainerRect(source, target, progress)
    if (progress.coerceIn(0f, 1f) < 1f) return bounds

    val overscan = endpointOverscanPx.coerceAtLeast(0f)
    return Rect(
        left = minOf(bounds.left, target.left) - overscan,
        top = minOf(bounds.top, target.top) - overscan,
        right = maxOf(bounds.right, target.right) + overscan,
        bottom = maxOf(bounds.bottom, target.bottom) + overscan,
    )
}

internal fun sharedContainerContentOffset(
    renderBounds: Rect,
    contentBounds: Rect,
): Offset = Offset(
    x = contentBounds.left - renderBounds.left,
    y = contentBounds.top - renderBounds.top,
)

internal data class SharedContainerCornerRadii(
    val topStart: Float,
    val topEnd: Float,
    val bottomEnd: Float,
    val bottomStart: Float,
) {
    companion object {
        fun uniform(cornerRadius: Float): SharedContainerCornerRadii =
            SharedContainerCornerRadii(
                topStart = cornerRadius,
                topEnd = cornerRadius,
                bottomEnd = cornerRadius,
                bottomStart = cornerRadius,
            )
    }
}

internal fun sharedContainerSourceCornerRadius(
    nominalRadius: Float,
    sourceWidth: Float,
    sourceHeight: Float,
): Float = minOf(nominalRadius, sourceWidth / 2f, sourceHeight / 2f).coerceAtLeast(0f)

internal fun sharedContainerCornerRadii(
    source: SharedContainerCornerRadii,
    target: SharedContainerCornerRadii,
    progress: Float,
): SharedContainerCornerRadii {
    val fraction = progress.coerceIn(0f, 1f)
    fun interpolate(sourceRadius: Float, targetRadius: Float): Float = if (fraction < 1f) {
        lerp(sourceRadius, targetRadius, fraction)
    } else {
        0f
    }
    return SharedContainerCornerRadii(
        topStart = interpolate(source.topStart, target.topStart),
        topEnd = interpolate(source.topEnd, target.topEnd),
        bottomEnd = interpolate(source.bottomEnd, target.bottomEnd),
        bottomStart = interpolate(source.bottomStart, target.bottomStart),
    )
}

internal fun sharedContainerCornerRadius(
    collapsedCornerRadius: Float,
    expandedCornerRadius: Float,
    progress: Float,
): Float = sharedContainerCornerRadii(
    source = SharedContainerCornerRadii.uniform(collapsedCornerRadius),
    target = SharedContainerCornerRadii.uniform(expandedCornerRadius),
    progress = progress,
).topStart

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

internal fun sharedArtworkTargetIsOnscreen(
    artworkBounds: Rect,
    viewportBounds: Rect,
): Boolean = !artworkBounds.isUsable() ||
    !viewportBounds.isUsable() ||
    artworkBounds.center.x in viewportBounds.left..viewportBounds.right

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

internal fun artworkInsetRect(
    bounds: Rect,
    inset: Float,
): Rect {
    val horizontalInset = inset.coerceIn(0f, bounds.width / 2f)
    val verticalInset = inset.coerceIn(0f, bounds.height / 2f)
    return Rect(
        left = bounds.left + horizontalInset,
        top = bounds.top + verticalInset,
        right = bounds.right - horizontalInset,
        bottom = bounds.bottom - verticalInset,
    )
}

internal fun playerSheetDragProgress(
    startProgress: Float,
    dragDistanceY: Float,
    travelDistance: Float,
): Float {
    if (travelDistance <= 0f) return startProgress.coerceIn(0f, 1f)
    return (startProgress - dragDistanceY / travelDistance).coerceIn(0f, 1f)
}

internal fun playerSheetVerticalTravel(
    source: Rect,
    target: Rect,
): Float = maxOf(
    abs(target.top - source.top),
    abs(target.bottom - source.bottom),
).coerceAtLeast(1f)

internal enum class PlayerSheetDragAxis { PENDING, HORIZONTAL, VERTICAL }

/** Reject a horizontal start even when no other component accepts the gesture. */
internal fun playerSheetDragAxis(delta: Offset, touchSlop: Float): PlayerSheetDragAxis = when {
    delta.getDistance() <= touchSlop -> PlayerSheetDragAxis.PENDING
    atan2(abs(delta.y).toDouble(), abs(delta.x).toDouble()) <= Math.PI / 6.0 ->
        PlayerSheetDragAxis.HORIZONTAL
    abs(delta.y) <= touchSlop -> PlayerSheetDragAxis.PENDING
    else -> PlayerSheetDragAxis.VERTICAL
}

internal fun playerSheetDragTarget(
    velocityY: Float,
    progress: Float,
    travelDistance: Float,
    originOpen: Boolean,
    density: Float,
    startProgress: Float = if (originOpen) 1f else 0f,
): Boolean {
    val velocityThreshold = 125f * density
    // Preserve an intentional fling even after the drag returns to a clamped endpoint.
    if (velocityY <= -velocityThreshold) return true
    if (velocityY >= velocityThreshold) return false
    val movement = (progress.coerceIn(0f, 1f) - startProgress.coerceIn(0f, 1f)) * travelDistance
    if (movement == 0f) return originOpen
    val distance = if (originOpen) -movement else movement
    val positionThreshold = minOf(56f * density, travelDistance)
    return if (distance >= positionThreshold) !originOpen else originOpen
}

internal fun sharedMiniPlayerControlsTranslationX(
    sourcePlayerBounds: Rect,
    animatedPlayerBounds: Rect,
    controlsBounds: Rect,
): Float {
    val sourceRightInset = sourcePlayerBounds.right - controlsBounds.right
    val targetControlsLeft = animatedPlayerBounds.right - sourceRightInset - controlsBounds.width
    return targetControlsLeft - controlsBounds.left
}

internal fun sharedMiniPlayerContentOffset(
    sourcePlayerBounds: Rect,
    animatedPlayerBounds: Rect,
    contentBounds: Rect,
): Offset = Offset(
    x = contentBounds.left - animatedPlayerBounds.left,
    y = contentBounds.top - sourcePlayerBounds.top,
)

internal fun sharedMiniPlayerControlsRenderRect(
    sourcePlayerBounds: Rect,
    animatedPlayerBounds: Rect,
    contentBounds: Rect,
    controlsBounds: Rect,
): Rect {
    val offset = sharedMiniPlayerContentOffset(
        sourcePlayerBounds = sourcePlayerBounds,
        animatedPlayerBounds = animatedPlayerBounds,
        contentBounds = contentBounds,
    )
    val controlsX = sharedMiniPlayerControlsTranslationX(
        sourcePlayerBounds = sourcePlayerBounds,
        animatedPlayerBounds = animatedPlayerBounds,
        controlsBounds = controlsBounds,
    )
    val left = animatedPlayerBounds.left + offset.x +
        controlsBounds.left - contentBounds.left + controlsX
    val top = animatedPlayerBounds.top + offset.y +
        controlsBounds.top - contentBounds.top
    return Rect(left, top, left + controlsBounds.width, top + controlsBounds.height)
}

internal fun Rect.isUsable(): Boolean = width > 0f && height > 0f

internal fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction.coerceIn(0f, 1f)

internal fun easeInCubic(value: Float): Float {
    val clamped = value.coerceIn(0f, 1f)
    return clamped * clamped * clamped
}

internal fun easeOutCubic(value: Float): Float {
    val clamped = value.coerceIn(0f, 1f)
    val inverse = 1f - clamped
    return 1f - inverse * inverse * inverse
}

internal fun playerNavigationOffset(source: Rect, target: Rect, progress: Float): Float {
    if (source.isEmpty || target.isEmpty) return 0f
    return sharedContainerRect(source, target, progress).bottom - source.bottom
}
