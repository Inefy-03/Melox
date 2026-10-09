package com.melox.player.ui.component.playback

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.RectangleShape
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
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
import com.melox.player.ui.component.library.rememberFullPlayerArtworkBitmapPixels
import com.melox.player.ui.component.library.rememberPlaceholderArtworkBitmap
import com.melox.player.ui.component.liquid.miuixFloatingBarShadow
import com.melox.player.ui.component.liquid.miniPlayerSurface
import com.melox.player.ui.screen.playback.rememberArtworkBlend
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.runtime.saveable.Saver
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.basic.DividerDefaults
import top.yukonga.miuix.kmp.utils.getRoundedCorner

internal fun playerSheetBarAlpha(progress: Float): Float {
    val handoff = (progress.coerceIn(0f, 1f) / PLAYER_LAYER_HANDOFF_END_PROGRESS)
        .coerceIn(0f, 1f)
    return 1f - easeOutCubic(handoff)
}
internal fun playerSheetMiniContentAlpha(progress: Float): Float {
    val fade = (progress.coerceIn(0f, 1f) / PLAYER_MINI_CONTENT_FADE_END_PROGRESS)
        .coerceIn(0f, 1f)
    return 1f - easeOutCubic(fade)
}

internal fun playerSheetPageAlpha(progress: Float): Float {
    return playerSheetContentAlpha(progress)
}

internal fun playerSheetBackgroundAlpha(progress: Float): Float {
    val handoff = (progress.coerceIn(0f, 1f) / PLAYER_LAYER_HANDOFF_END_PROGRESS)
        .coerceIn(0f, 1f)
    return easeInCubic(handoff)
}

internal fun playerSheetContentAlpha(progress: Float): Float {
    val fraction = (
        (progress.coerceIn(0f, 1f) - PLAYER_CONTENT_APPEAR_START_PROGRESS) /
            (PLAYER_CONTENT_APPEAR_END_PROGRESS - PLAYER_CONTENT_APPEAR_START_PROGRESS)
        ).coerceIn(0f, 1f)
    return easeInCubic(fraction)
}

internal fun playerSheetGlassVisible(progress: Float): Boolean =
    progress.coerceIn(0f, 1f) < PLAYER_LAYER_HANDOFF_END_PROGRESS

internal fun playerSheetUsesFullPlayerStatusBar(
    progress: Float,
): Boolean = progress > PLAYER_LAYER_HANDOFF_END_PROGRESS
internal fun Modifier.recordPlayerLayer(
    layer: GraphicsLayer,
    drawInPlace: Boolean,
    recordingGeneration: Int = 0,
    onRecorded: (generation: Int, size: IntSize) -> Unit = { _, _ -> },
): Modifier = drawWithContent {
    if (drawInPlace) {
        // Draw descendants into one target per frame across the live handoff.
        this@drawWithContent.drawContent()
    } else {
        layer.record {
            this@drawWithContent.drawContent()
        }
        onRecorded(recordingGeneration, layer.size)
    }
}

internal fun Modifier.recordMiniPlayerLayer(
    layer: GraphicsLayer,
    drawInPlace: Boolean,
    recordingGeneration: Int,
    onRecorded: (generation: Int, size: IntSize) -> Unit,
): Modifier = drawWithContent {
    // Mini readiness must be reported even while the mini player is visible.
    // Replay this private, untransformed source instead of drawing children twice.
    layer.record { this@drawWithContent.drawContent() }
    onRecorded(recordingGeneration, layer.size)
    if (drawInPlace) drawLayer(layer)
}

internal fun Modifier.recordPlayerContentLayer(
    layer: GraphicsLayer,
): Modifier = drawWithContent {
    layer.record {
        this@drawWithContent.drawContent()
    }
    this@drawWithContent.drawContent()
}

@Composable
internal fun PlayerSheetDragInputOverlay(
    transition: PlayerSheetTransitionState,
    hasItem: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!transition.sharedLayersReady || !transition.isTransitionActive) return
    val bounds = sharedContainerRect(
        transition.miniPlayerBounds,
        transition.fullPlayerBounds,
        transition.progress,
    )
    val density = LocalDensity.current
    val dragModifier = rememberPlayerSheetVerticalDragModifier(
        enabled = true,
        hasItem = hasItem,
        onDragStart = transition::beginFullPlayerDrag,
        onDrag = transition::dragBy,
        onDragEnd = transition::endDrag,
        onDragCancel = transition::cancelDrag,
    )
    Box(
        modifier = modifier
            .offset { IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt()) }
            .size(
                width = with(density) { bounds.width.toDp() },
                height = with(density) { bounds.height.toDp() },
            )
            .playerSheetHostLayer(
                hostBounds = bounds,
                inputBounds = bounds,
                miniPlayerBounds = transition.miniPlayerBounds.takeIf {
                    transition.miniPlayerAcceptsInput
                },
            )
            .then(dragModifier)
            .pointerInput(Unit) {
                // Transition snapshots are not live button targets.
                detectTapGestures { }
            },
    )
}

@Composable
internal fun PlayerSheetMiniControlsInputOverlay(
    transition: PlayerSheetTransitionState,
    hasItem: Boolean,
    normalChrome: Boolean,
    smallPlayerBar: Boolean,
    onTogglePlayPause: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!transition.sharedLayersReady || !transition.isTransitionActive ||
        transition.progress >= PLAYER_MINI_CONTENT_FADE_END_PROGRESS
    ) return
    val source = transition.miniPlayerBounds
    val content = transition.miniPlayerContentBounds
    val controls = transition.miniPlayerControlsBounds
    if (!source.isUsable() || !content.isUsable() || !controls.isUsable()) return

    val bounds = sharedContainerRect(source, transition.fullPlayerBounds, transition.progress)
    val renderedControls = sharedMiniPlayerControlsRenderRect(
        sourcePlayerBounds = source,
        animatedPlayerBounds = bounds,
        contentBounds = content,
        controlsBounds = controls,
    )
    val density = LocalDensity.current
    val playOffset = with(density) { (if (smallPlayerBar) 0.dp else (-6).dp).toPx() }
    val queueOffset = with(density) {
        (if (normalChrome || smallPlayerBar) 0.dp else (-2).dp).toPx()
    }
    val controlSizeDp = miniPlayerControlSize(normalChrome, smallPlayerBar)
    val controlSize = with(density) { controlSizeDp.toPx() }
    val currentOnTogglePlayPause by rememberUpdatedState(onTogglePlayPause)
    val currentOnOpenQueue by rememberUpdatedState(onOpenQueue)

    Box(
        modifier = modifier
            .offset { IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt()) }
            .size(
                width = with(density) { bounds.width.toDp() },
                height = with(density) { bounds.height.toDp() },
            )
            .graphicsLayer {
                clip = true
                shape = RectangleShape
            },
    ) {
        if (hasItem) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (renderedControls.left - bounds.left + playOffset).roundToInt(),
                            (renderedControls.top - bounds.top).roundToInt(),
                        )
                    }
                    .size(controlSizeDp)
                    .pointerInput(Unit) {
                        detectTapGestures { currentOnTogglePlayPause() }
                    },
            )
        }
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (renderedControls.left - bounds.left + controlSize + queueOffset)
                            .roundToInt(),
                        (renderedControls.top - bounds.top).roundToInt(),
                    )
                }
                .size(controlSizeDp)
                .pointerInput(Unit) {
                    detectTapGestures { currentOnOpenQueue() }
                },
        )
    }
}

/**
 * Shared container overlay. Mini-player and full-player content are recorded
 * independently, then drawn inside one expanding squircle instead of fading a
 * fixed bar under a fixed screen.
 */
@Composable
internal fun PlayerSheetContentOverlay(
    transition: PlayerSheetTransitionState,
    miniPlayerContentLayer: GraphicsLayer,
    fullPlayerBackgroundLayer: GraphicsLayer,
    fullPlayerContentLayer: GraphicsLayer,
    miniPlayerChrome: MiniPlayerChrome?,
    collapsedCornerRadius: Dp,
    floatingMiniPlayer: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!transition.sharedLayersReady || !transition.isTransitionActive) return

    val progress = transition.progress
    val density = LocalDensity.current
    val contentBounds = sharedContainerRect(
        source = transition.miniPlayerBounds,
        target = transition.fullPlayerBounds,
        progress = progress,
    )
    val bounds = sharedContainerRenderRect(
        source = transition.miniPlayerBounds,
        target = transition.fullPlayerBounds,
        progress = progress,
    )
    val deviceCornerRadius = getRoundedCorner()
    val expandedCornerRadius = if (rememberPlayerWindowUsesPhysicalScreenCorners()) {
        deviceCornerRadius
    } else {
        0.dp
    }
    val cornerRadii = sharedContainerCornerRadii(
        source = SharedContainerCornerRadii.uniform(collapsedCornerRadius.value),
        target = SharedContainerCornerRadii.uniform(expandedCornerRadius.value),
        progress = progress,
    )
    val cornerRadius = cornerRadii.topStart.dp
    val miniChromeAlpha = playerSheetBarAlpha(progress)

    val glassChrome = miniPlayerChrome?.takeIf {
        playerSheetGlassVisible(progress)
    }
    val glassSurfaceModifier = if (glassChrome != null) {
        Modifier
            .miniPlayerSurface(
                cornerRadius = cornerRadius,
                backdrop = glassChrome.backdrop,
                blurActive = glassChrome.blurActive,
                liquidGlassActive = glassChrome.liquidGlassActive,
                isDark = glassChrome.isDark,
                followsNavigationBar = glassChrome.style == BottomBarStyle.NORMAL,
                floatingHighlight = glassChrome.floatingHighlight,
                highlightAlpha = miniChromeAlpha,
            )
            .then(
                if (glassChrome.style == BottomBarStyle.NORMAL) {
                    Modifier.squircleBorder(
                        width = DividerDefaults.Thickness,
                        color = DividerDefaults.DividerColor.copy(
                            alpha = NORMAL_BAR_STROKE_ALPHA,
                        ),
                        cornerRadius = cornerRadius,
                    )
                } else {
                    Modifier
                },
            )
    } else {
        Modifier
    }
    Box(
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
                if (floatingMiniPlayer) {
                    Modifier.miuixFloatingBarShadow(
                        cornerRadius = cornerRadius,
                        isDark = isDark,
                        alpha = miniChromeAlpha,
                    )
                } else {
                    Modifier
                },
            )
            .squircleClip(
                topStart = cornerRadii.topStart.dp,
                topEnd = cornerRadii.topEnd.dp,
                bottomEnd = cornerRadii.bottomEnd.dp,
                bottomStart = cornerRadii.bottomStart.dp,
            )
            .then(glassSurfaceModifier),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (
                miniPlayerContentLayer.size.width > 0 &&
                    progress < PLAYER_MINI_CONTENT_FADE_END_PROGRESS
            ) {
                    miniPlayerContentLayer.alpha = playerSheetMiniContentAlpha(progress)
                    val sourceBounds = transition.miniPlayerBounds
                    val contentLayerBounds = transition.miniPlayerContentBounds
                    val contentOffset = if (contentLayerBounds.isUsable()) {
                        sharedMiniPlayerContentOffset(
                            sourcePlayerBounds = sourceBounds,
                            animatedPlayerBounds = contentBounds,
                            contentBounds = contentLayerBounds,
                        )
                    } else {
                        Offset.Zero
                    }
                    val controlsBounds = transition.miniPlayerControlsBounds
                    if (sourceBounds.isUsable() && controlsBounds.isUsable()) {
                        val controlsLeft = controlsBounds.left - sourceBounds.left
                        val controlsTranslationX = sharedMiniPlayerControlsTranslationX(
                            sourcePlayerBounds = sourceBounds,
                            animatedPlayerBounds = contentBounds,
                            controlsBounds = controlsBounds,
                        )
                        clipRect(right = controlsLeft) {
                            withTransform({
                                translate(
                                    left = contentOffset.x,
                                    top = contentOffset.y,
                                )
                            }) {
                                drawLayer(miniPlayerContentLayer)
                            }
                        }
                        clipRect(left = controlsLeft + controlsTranslationX) {
                            withTransform({
                                translate(
                                    left = contentOffset.x + controlsTranslationX,
                                    top = contentOffset.y,
                                )
                            }) {
                                drawLayer(miniPlayerContentLayer)
                            }
                        }
                    } else {
                        withTransform({
                            translate(
                                left = contentOffset.x,
                                top = contentOffset.y,
                            )
                        }) {
                            drawLayer(miniPlayerContentLayer)
                        }
                    }
            }
            if (fullPlayerBackgroundLayer.size.width > 0 && progress > 0f) {
                val contentOffset = sharedContainerContentOffset(
                    renderBounds = bounds,
                    contentBounds = contentBounds,
                )
                val scale = contentBounds.width / fullPlayerBackgroundLayer.size.width
                fullPlayerBackgroundLayer.alpha = playerSheetBackgroundAlpha(progress)
                withTransform({
                    translate(left = contentOffset.x, top = contentOffset.y)
                    scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
                }) {
                    drawLayer(fullPlayerBackgroundLayer)
                }
            }
            if (fullPlayerContentLayer.size.width > 0 && progress > PLAYER_CONTENT_APPEAR_START_PROGRESS) {
                val contentOffset = sharedContainerContentOffset(
                    renderBounds = bounds,
                    contentBounds = contentBounds,
                )
                val scale = contentBounds.width / fullPlayerContentLayer.size.width
                fullPlayerContentLayer.alpha = playerSheetContentAlpha(progress)
                withTransform({
                    translate(left = contentOffset.x, top = contentOffset.y)
                    scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
                }) {
                    drawLayer(fullPlayerContentLayer)
                }
            }
        }
    }
}

/**
 * Shared cover overlay. Landscape uses direct edge interpolation so horizontal
 * and vertical movement follow their measured start-to-end distances; portrait
 * keeps the established eased center path.
 */
@Composable
internal fun PlayerSheetArtworkOverlay(
    playback: PlaybackUiState,
    transition: PlayerSheetTransitionState,
    collapsedCornerRadius: Dp,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val item = playback.currentItem ?: return
    val progress = transition.progress
    val source = transition.overlayMiniArtworkBounds
    val target = transition.overlayFullArtworkBounds
    val targetSizePx = maxOf(target.width, target.height).roundToInt()
    val artworkBitmap = rememberArtworkBitmap(
        contentUri = item.contentUri,
        dateModifiedEpochSeconds = item.dateModifiedEpochSeconds,
        fileSizeBytes = item.fileSizeBytes,
        size = PLAYER_FULL_ARTWORK_REQUEST_SIZE,
    )
    val fullPlayerArtwork = rememberFullPlayerArtworkBitmapPixels(
        contentUri = item.contentUri,
        dateModifiedEpochSeconds = item.dateModifiedEpochSeconds,
        fileSizeBytes = item.fileSizeBytes,
        targetSizePx = targetSizePx.coerceAtLeast(1),
        enabled = targetSizePx > 0,
    )
    val fallbackBitmap = artworkBitmap ?: rememberPlaceholderArtworkBitmap(
        PLAYER_FULL_ARTWORK_REQUEST_SIZE,
    )
    val artworkBlend = rememberArtworkBlend(
        targetBitmap = fullPlayerArtwork ?: fallbackBitmap,
        animate = true,
        contentKey = item.contentUri,
        durationMillis = PLAYER_TRACK_ARTWORK_CROSSFADE_DURATION_MILLIS,
        sameContentDurationMillis = PLAYER_ARTWORK_RESOLUTION_CROSSFADE_DURATION_MILLIS,
    )
    val bitmap = artworkBlend.currentBitmap
    val artworkLayerAlphas = sourceOverAlphas(artworkBlend.frames.map { it.alpha })
    val density = LocalDensity.current
    if (!enabled || !transition.separateArtworkOverlayReady || !transition.isTransitionActive) return
    val sourceArtworkBounds = bitmap?.let {
        fittedArtworkRect(source, it.width, it.height)
    }
    val sourceBounds = sourceArtworkBounds ?: source
    if (!sharedArtworkTargetIsOnscreen(
        artworkBounds = target,
        viewportBounds = transition.fullPlayerBounds,
    )) return
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
        PLAYER_FULL_ARTWORK_CORNER_RADIUS.value,
        progress,
    )

    if (bitmap == null) {
        val renderedBounds = sharedArtworkRect(sourceBounds, target, progress)
        val scaleX = renderedBounds.width / source.width.coerceAtLeast(1f)
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
                    val frameProgress = transition.progress
                    val frameBounds = sharedArtworkRect(
                        source = sourceBounds,
                        target = target,
                        progress = frameProgress,
                    )
                    transformOrigin = TransformOrigin(0f, 0f)
                    this.scaleX = frameBounds.width / source.width.coerceAtLeast(1f)
                    this.scaleY = frameBounds.height / source.height.coerceAtLeast(1f)
                    translationX = frameBounds.left - source.left
                    translationY = frameBounds.top - source.top
                },
        ) {
            PlaybackArtworkFrame(
                bitmap = null,
                size = with(density) { source.width.coerceAtLeast(1f).toDp() },
                cornerRadius = localCornerRadius,
                modifier = Modifier,
                contentScale = ContentScale.Fit,
                useSquircleClip = true,
                drawArtworkShadow = true,
                artworkShadowAlpha = progress,
            )
        }
    } else {
        val targetBounds = target
        val targetArtworkWidth = targetBounds.width.coerceAtLeast(1f)
        val targetArtworkHeight = targetBounds.height.coerceAtLeast(1f)
        val renderedBounds = sharedArtworkRect(sourceBounds, target, progress)
        val scaleX = renderedBounds.width / targetArtworkWidth
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
                    val frameProgress = transition.progress
                    val frameBounds = sharedArtworkRect(
                        source = sourceBounds,
                        target = target,
                        progress = frameProgress,
                    )
                    transformOrigin = TransformOrigin(0f, 0f)
                    this.scaleX = frameBounds.width / targetArtworkWidth
                    this.scaleY = frameBounds.height / targetArtworkHeight
                    translationX = frameBounds.left - targetBounds.left
                    translationY = frameBounds.top - targetBounds.top
                },
        ) {
            artworkBlend.frames.forEachIndexed { index, frame ->
                Image(
                    bitmap = frame.value.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .playbackArtworkShadow(
                            cornerRadius = localCornerRadius,
                            alpha = progress,
                        )
                        .squircleClip(localCornerRadius)
                        .graphicsLayer { alpha = artworkLayerAlphas[index] },
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.High,
                )
            }
        }
    }
}

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
