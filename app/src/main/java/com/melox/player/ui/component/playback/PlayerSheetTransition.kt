package com.melox.player.ui.component.playback

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
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
import kotlin.math.roundToInt
import kotlin.math.sign
import androidx.compose.runtime.saveable.Saver
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.basic.DividerDefaults
import top.yukonga.miuix.kmp.utils.getRoundedCorner

// Duration of the artwork crossfade when the current track changes.
internal const val PLAYER_TRACK_ARTWORK_CROSSFADE_DURATION_MILLIS = 400
// A same-track resolution upgrade should finish near the start of the sheet motion.
internal const val PLAYER_ARTWORK_RESOLUTION_CROSSFADE_DURATION_MILLIS = 100
// Backgrounds fade from the current visible layer, including the first fallback color.
internal const val PLAYBACK_BACKGROUND_TRANSITION_DURATION_MILLIS = 400
// Progress at which the mini-player layers have fully handed off to the full player.
internal const val PLAYER_LAYER_HANDOFF_END_PROGRESS = 0.2f
// Progress at which the mini-player's recorded content finishes fading out.
internal const val PLAYER_MINI_CONTENT_FADE_END_PROGRESS = 0.3f
// Progress at which full-player content begins to appear.
internal const val PLAYER_CONTENT_APPEAR_START_PROGRESS = 0.1f
// Progress at which full-player content is fully visible.
internal const val PLAYER_CONTENT_APPEAR_END_PROGRESS = 0.6f
// Linear-motion share used to soften the artwork's vertical travel curve.
internal const val PLAYER_ARTWORK_VERTICAL_LINEAR_WEIGHT = 0.4f

// Requested square size for the full-player artwork bitmap.
internal val PLAYER_FULL_ARTWORK_REQUEST_SIZE = 420.dp
// Corner radius of the full-player artwork frame.
internal val PLAYER_FULL_ARTWORK_CORNER_RADIUS = 12.dp
// Corner-radius reduction when the mini player uses rectangular artwork.
internal val MINI_PLAYER_RECTANGULAR_ARTWORK_CORNER_REDUCTION = 2.dp
internal val PLAYER_TRACK_ARTWORK_CROSSFADE_EASING = androidx.compose.animation.core.FastOutSlowInEasing

/**
 * Owns the one progress value shared by the mini player, full player, and
 * artwork overlay. Its endpoint-driven spring allows a close gesture to
 * reverse from the current frame.
 */
internal enum class PlayerInputOwner { MINI, FULL, NONE }

@Stable
internal class PlayerSheetTransitionState(initialProgress: Float = 0f) {
    private val restoredProgress = initialProgress.coerceIn(0f, 1f)
    private val progressAnimation = Animatable(restoredProgress, visibilityThreshold = 0.001f)
    private var renderedProgress by mutableFloatStateOf(restoredProgress)
    private var dragStartProgress = 0f
    private var dragDistanceY = 0f
    var gestureDensity = 1f
    private var dragOriginOpen = false
    private var dragStartedFromMiniPlayer = false
    private var miniPlayerExpansionStarted by mutableStateOf(false)
    private var requestedInitialVelocity = 0f
    private var hasBeenShown by mutableStateOf(restoredProgress > 0f)

    companion object {
        val Saver: Saver<PlayerSheetTransitionState, List<Any>> = Saver(
            save = { state ->
                listOf(
                    if (state.targetOpen) 1f else 0f,
                    state.fullPlayerArtworkPageSelected,
                    state.hasBeenShown,
                )
            },
            restore = { savedState ->
                PlayerSheetTransitionState(savedState[0] as Float).apply {
                    hasBeenShown = savedState.getOrNull(2) as? Boolean
                        ?: ((savedState[0] as Float) > 0f)
                    updateFullPlayerArtworkPageSelected(savedState[1] as Boolean)
                }
            },
        )
    }

    var targetOpen by mutableStateOf(restoredProgress > 0.5f)
        private set

    var isDragging by mutableStateOf(false)
        private set

    var animationRequest by mutableIntStateOf(0)
        private set

    var miniPlayerBounds by mutableStateOf(Rect.Zero)
        private set

    var miniPlayerContentBounds by mutableStateOf(Rect.Zero)
        private set

    var miniPlayerControlsBounds by mutableStateOf(Rect.Zero)
        private set

    var fullPlayerBounds by mutableStateOf(Rect.Zero)
        private set

    var miniArtworkBounds by mutableStateOf(Rect.Zero)
        private set

    var fullArtworkBounds by mutableStateOf(Rect.Zero)
        private set

    var fullPlayerArtworkPageSelected by mutableStateOf(true)
        private set

    private var layoutWindowSize by mutableStateOf(IntSize.Zero)
    private var miniFrameReady by mutableStateOf(false)
    private var fullBackgroundFrameReady by mutableStateOf(false)
    private var fullContentFrameReady by mutableStateOf(false)
    private var frameRecordingGeneration by mutableIntStateOf(0)
    private var frozenArtworkBounds: Pair<Rect, Rect>? = null
    private var openingMiniPlayerScale by mutableFloatStateOf(1f)
    private var openingMiniPlayerOffsetX by mutableFloatStateOf(0f)
    private var closingStartProgress = 1f

    val capturedMiniPlayerPressScale: Float?
        get() = openingMiniPlayerScale.takeIf {
            isDragging && dragStartedFromMiniPlayer && miniPlayerExpansionStarted
        }

    val overlayMiniPlayerScale: Float
        get() = when {
            !isTransitionActive && progress >= 1f -> 1f
            !targetOpen && !isDragging -> lerp(
                1f,
                openingMiniPlayerScale,
                if (closingStartProgress > 0f) progress / closingStartProgress else 0f,
            )
            else -> openingMiniPlayerScale
        }

    val overlayMiniPlayerOffsetX: Float
        get() = when {
            !isTransitionActive && progress >= 1f -> 0f
            !targetOpen && !isDragging -> lerp(
                0f,
                openingMiniPlayerOffsetX,
                if (closingStartProgress > 0f) progress / closingStartProgress else 0f,
            )
            else -> openingMiniPlayerOffsetX
        }

    val capturedMiniPlayerOffsetX: Float?
        get() = overlayMiniPlayerOffsetX.takeIf { isMounted }

    val overlayMiniPlayerBounds: Rect
        get() = transformedMiniPlayerBounds(miniPlayerBounds)
    val overlayMiniPlayerContentBounds: Rect
        get() = transformedMiniPlayerBounds(miniPlayerContentBounds)
    val overlayMiniPlayerControlsBounds: Rect
        get() = transformedMiniPlayerBounds(miniPlayerControlsBounds)

    private fun transformedMiniPlayerBounds(bounds: Rect): Rect =
        scaledMiniPlayerBounds(bounds, miniPlayerBounds, overlayMiniPlayerScale)
            .translate(Offset(overlayMiniPlayerOffsetX, 0f))

    val currentFrameRecordingGeneration: Int
        get() = frameRecordingGeneration

    val overlayMiniArtworkBounds: Rect
        get() = transformedMiniPlayerBounds(
            frozenArtworkBounds?.first ?: miniArtworkBounds,
        )
    val overlayFullArtworkBounds: Rect
        get() = frozenArtworkBounds?.second ?: fullArtworkBounds

    fun updateWindowSize(size: IntSize) {
        if (size == layoutWindowSize || size.width <= 0 || size.height <= 0) return
        if (isTransitionActive) {
            isDragging = false
            requestedInitialVelocity = 0f
            animationRequest += 1
        }
        layoutWindowSize = size
        miniPlayerBounds = Rect.Zero
        miniPlayerContentBounds = Rect.Zero
        miniPlayerControlsBounds = Rect.Zero
        fullPlayerBounds = Rect.Zero
        miniArtworkBounds = Rect.Zero
        fullArtworkBounds = Rect.Zero
        miniFrameReady = false
        fullBackgroundFrameReady = false
        fullContentFrameReady = false
        frameRecordingGeneration += 1
        frozenArtworkBounds = null
        openingMiniPlayerScale = 1f
        openingMiniPlayerOffsetX = 0f
        miniPlayerExpansionStarted = false
    }

    fun markMiniFrameRecorded(
        windowSize: IntSize,
        generation: Int,
        size: IntSize,
    ) {
        if (acceptsRecordedFrame(windowSize, generation, size, miniPlayerBounds)) {
            miniFrameReady = true
        }
    }

    fun markFullBackgroundFrameRecorded(
        windowSize: IntSize,
        generation: Int,
        size: IntSize,
    ) {
        if (acceptsRecordedFrame(windowSize, generation, size, fullPlayerBounds)) {
            fullBackgroundFrameReady = true
        }
    }

    fun markFullContentFrameRecorded(
        windowSize: IntSize,
        generation: Int,
        size: IntSize,
    ) {
        if (acceptsRecordedFrame(windowSize, generation, size, fullPlayerBounds)) {
            fullContentFrameReady = true
        }
    }

    fun markFullFrameRecorded(windowSize: IntSize, generation: Int, size: IntSize) {
        markFullBackgroundFrameRecorded(windowSize, generation, size)
        markFullContentFrameRecorded(windowSize, generation, size)
    }

    private fun acceptsRecordedFrame(
        windowSize: IntSize,
        generation: Int,
        size: IntSize,
        bounds: Rect,
    ): Boolean = windowSize == layoutWindowSize &&
        generation == frameRecordingGeneration &&
        size.matches(bounds)

    private fun IntSize.matches(bounds: Rect): Boolean = bounds.isUsable() &&
        abs(width - bounds.width.roundToInt()) <= 1 &&
        abs(height - bounds.height.roundToInt()) <= 1

    private fun freezeArtworkBounds() {
        if (frozenArtworkBounds == null && hasArtworkBounds) {
            frozenArtworkBounds = miniArtworkBounds to fullArtworkBounds
        }
    }

    val progress: Float
        get() = renderedProgress.coerceIn(0f, 1f)

    val hasArtworkBounds: Boolean
        get() = miniArtworkBounds.isUsable() && fullArtworkBounds.isUsable()

    val hasContainerBounds: Boolean
        get() = miniPlayerBounds.isUsable() && fullPlayerBounds.isUsable()

    val sharedLayersReady: Boolean
        get() = hasContainerBounds &&
            (layoutWindowSize == IntSize.Zero ||
                (miniFrameReady && fullBackgroundFrameReady && fullContentFrameReady))

    val fullPlayerFramesNeedRecording: Boolean
        get() = layoutWindowSize != IntSize.Zero &&
            (!fullBackgroundFrameReady || !fullContentFrameReady)

    val separateArtworkOverlayReady: Boolean
        get() = sharedLayersReady && sharedArtworkEnabled && hasArtworkBounds

    val canSettle: Boolean
        get() = hasContainerBounds

    private val isCollapsedMiniPlayerDrag: Boolean
        get() = isDragging && dragStartedFromMiniPlayer && !miniPlayerExpansionStarted &&
            !targetOpen && progress <= 0f

    val isMounted: Boolean
        get() = !isCollapsedMiniPlayerDrag && (isDragging || targetOpen || progress > 0f)

    val isInProgress: Boolean
        get() = isDragging || progress > 0f && progress < 1f

    val isFullyExpanded: Boolean
        get() = !isDragging && progress >= 1f

    val isTransitionActive: Boolean
        get() = !isCollapsedMiniPlayerDrag &&
            (isDragging || if (targetOpen) progress < 1f else progress > 0f)

    val sharedArtworkEnabled: Boolean
        get() = fullPlayerArtworkPageSelected

    val inputOwner: PlayerInputOwner
        get() = playerSheetInputOwner(
            targetOpen = targetOpen,
            isDragging = isDragging,
            dragStartedFromMiniPlayer = dragStartedFromMiniPlayer,
            progress = progress,
            sharedLayersReady = sharedLayersReady,
        )

    val miniPlayerAcceptsInput: Boolean
        get() = inputOwner == PlayerInputOwner.MINI

    val fullPlayerHostMounted: Boolean
        get() = hasBeenShown || isDragging

    val fullPlayerDrawsInPlace: Boolean
        get() = if (sharedLayersReady) {
            targetOpen && !isTransitionActive
        } else {
            progress > PLAYER_LAYER_HANDOFF_END_PROGRESS
        }

    val fullPlayerAcceptsInput: Boolean
        get() = inputOwner == PlayerInputOwner.FULL

    val blocksUnderlyingInput: Boolean
        get() = inputOwner == PlayerInputOwner.NONE && fullPlayerDrawsAboveRoot

    val fullPlayerDrawsAboveRoot: Boolean
        get() = isMounted &&
            (sharedLayersReady || progress > PLAYER_LAYER_HANDOFF_END_PROGRESS)

    val visibleBackgroundBounds: Rect
        get() = when {
            !isMounted -> Rect.Zero
            sharedLayersReady && isTransitionActive -> sharedContainerRenderRect(
                source = overlayMiniPlayerBounds,
                target = fullPlayerBounds,
                progress = progress,
            )
            fullPlayerDrawsInPlace -> fullPlayerBounds
            else -> Rect.Zero
        }

    fun open(
        miniPlayerScale: Float = 1f,
        requireFreshMiniFrame: Boolean = false,
        miniPlayerOffsetX: Float = 0f,
    ) {
        val needsFreshFrames = !sharedLayersReady || requireFreshMiniFrame && !isMounted
        if (!isMounted) {
            openingMiniPlayerScale = miniPlayerScale.coerceAtLeast(1f)
            openingMiniPlayerOffsetX = miniPlayerOffsetX
        }
        hasBeenShown = true
        releaseDragForProgrammaticSettle()
        if (needsFreshFrames) requestFreshFrameRecording()
        requestSettle(open = true)
    }

    fun close() {
        releaseDragForProgrammaticSettle()
        requestSettle(open = false)
        if (!sharedLayersReady && progress <= 0f) {
            renderedProgress = 0f
            frozenArtworkBounds = null
        }
    }

    fun beginMiniPlayerDrag(
        miniPlayerScale: Float = 1f,
        requireFreshMiniFrame: Boolean = false,
        miniPlayerOffsetX: Float = 0f,
    ) {
        if (requireFreshMiniFrame && !isMounted && !isDragging) {
            requestFreshFrameRecording()
        }
        if (!isMounted) {
            openingMiniPlayerScale = miniPlayerScale.coerceAtLeast(1f)
            openingMiniPlayerOffsetX = miniPlayerOffsetX
        }
        beginDrag(startedFromMiniPlayer = true)
    }

    fun beginFullPlayerDrag() {
        beginDrag(startedFromMiniPlayer = false)
    }

    private fun beginDrag(startedFromMiniPlayer: Boolean) {
        if (isDragging) return
        if (isMounted) {
            openingMiniPlayerScale = overlayMiniPlayerScale
            openingMiniPlayerOffsetX = overlayMiniPlayerOffsetX
        }
        // A first upward drag is also the first presentation of the full player.
        // Keep its host mounted after the pointer is released, just like click-open.
        hasBeenShown = true
        if (!isTransitionActive) frozenArtworkBounds = null
        freezeArtworkBounds()
        val currentProgress = progress
        dragStartProgress = currentProgress
        renderedProgress = dragStartProgress
        dragDistanceY = 0f
        dragOriginOpen = targetOpen
        dragStartedFromMiniPlayer = startedFromMiniPlayer
        miniPlayerExpansionStarted = startedFromMiniPlayer && currentProgress > 0f
        isDragging = true
        animationRequest += 1
    }

    fun dragMiniPlayerBy(dragAmountY: Float, miniPlayerScale: Float) {
        // Capture the live press only until this gesture first starts expanding.
        if (isCollapsedMiniPlayerDrag) {
            openingMiniPlayerScale = miniPlayerScale.coerceAtLeast(1f)
        }
        dragBy(dragAmountY)
    }

    fun dragBy(dragAmountY: Float) {
        if (!isDragging) return
        dragDistanceY += dragAmountY
        updateDragProgress()
    }

    fun endMiniPlayerDrag(velocityY: Float, miniPlayerScale: Float) {
        if (isCollapsedMiniPlayerDrag) {
            openingMiniPlayerScale = miniPlayerScale.coerceAtLeast(1f)
        }
        endDrag(velocityY)
    }

    fun endDrag(velocityY: Float) {
        if (!isDragging) return
        val verticalTravel = playerSheetVerticalTravel(
            source = miniPlayerBounds,
            target = fullPlayerBounds,
        )
        val open = playerSheetDragTarget(
            velocityY = velocityY,
            progress = progress,
            travelDistance = verticalTravel,
            originOpen = dragOriginOpen,
            density = gestureDensity,
            startProgress = dragStartProgress,
        )
        closingStartProgress = progress
        isDragging = false
        miniPlayerExpansionStarted = false
        requestSettle(
            open = open,
            initialVelocity = -velocityY / verticalTravel,
        )
    }

    fun cancelDrag() {
        if (!isDragging) return
        closingStartProgress = progress
        isDragging = false
        miniPlayerExpansionStarted = false
        requestSettle(open = dragOriginOpen)
    }

    fun updateMiniPlayerBounds(bounds: Rect, windowSize: IntSize = layoutWindowSize) {
        if (windowSize != layoutWindowSize) return
        if (bounds.isUsable()) {
            if (miniPlayerBounds.size != bounds.size) miniFrameReady = false
            miniPlayerBounds = bounds
            updateDragProgress()
        }
    }

    fun updateMiniPlayerContentBounds(bounds: Rect, windowSize: IntSize = layoutWindowSize) {
        if (windowSize != layoutWindowSize) return
        if (bounds.isUsable()) miniPlayerContentBounds = bounds
    }

    fun updateMiniPlayerControlsBounds(bounds: Rect, windowSize: IntSize = layoutWindowSize) {
        if (windowSize != layoutWindowSize) return
        if (bounds.isUsable()) miniPlayerControlsBounds = bounds
    }

    fun updateFullPlayerBounds(bounds: Rect, windowSize: IntSize = layoutWindowSize) {
        if (windowSize != layoutWindowSize) return
        if (bounds.isUsable()) {
            if (fullPlayerBounds.size != bounds.size) {
                fullBackgroundFrameReady = false
                fullContentFrameReady = false
            }
            fullPlayerBounds = bounds
            updateDragProgress()
        }
    }

    fun updateMiniArtworkBounds(bounds: Rect, windowSize: IntSize = layoutWindowSize) {
        if (windowSize != layoutWindowSize) return
        if (bounds.isUsable()) {
            miniArtworkBounds = bounds
            frozenArtworkBounds = frozenArtworkBounds?.let { (_, target) ->
                bounds to target
            }
        }
    }

    fun updateFullArtworkBounds(bounds: Rect, windowSize: IntSize = layoutWindowSize) {
        if (windowSize != layoutWindowSize) return
        if (bounds.isUsable()) {
            fullArtworkBounds = bounds
            frozenArtworkBounds = frozenArtworkBounds?.let { (source, _) ->
                source to bounds
            }
        }
    }

    fun updateFullPlayerArtworkPageSelected(selected: Boolean) {
        fullPlayerArtworkPageSelected = selected
    }

    internal suspend fun animateToTarget() {
        freezeArtworkBounds()
        progressAnimation.snapTo(renderedProgress)
        val visibilityThreshold = 0.5f / fullPlayerBounds.height.coerceAtLeast(1f)
        progressAnimation.animateTo(
            targetValue = if (targetOpen) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = 360f,
                visibilityThreshold = visibilityThreshold,
            ),
            initialVelocity = requestedInitialVelocity,
        ) {
            if (!isDragging) renderedProgress = value
        }
    }

    private fun updateDragProgress() {
        if (!isDragging || !fullPlayerBounds.isUsable()) return
        renderedProgress = playerSheetDragProgress(
            startProgress = dragStartProgress,
            dragDistanceY = dragDistanceY,
            travelDistance = playerSheetVerticalTravel(
                source = miniPlayerBounds,
                target = fullPlayerBounds,
            ),
        )
        if (dragStartedFromMiniPlayer && renderedProgress > 0f) {
            miniPlayerExpansionStarted = true
        }
    }

    private fun releaseDragForProgrammaticSettle() {
        if (!isDragging) return
        closingStartProgress = progress
        isDragging = false
        miniPlayerExpansionStarted = false
    }

    private fun requestSettle(open: Boolean, initialVelocity: Float = 0f) {
        if (!isTransitionActive && progress >= 1f) {
            openingMiniPlayerScale = 1f
            openingMiniPlayerOffsetX = 0f
        }
        if (isMounted) {
            openingMiniPlayerScale = overlayMiniPlayerScale
            openingMiniPlayerOffsetX = overlayMiniPlayerOffsetX
        }
        if (!open) closingStartProgress = progress
        if (!isTransitionActive) frozenArtworkBounds = null
        freezeArtworkBounds()
        targetOpen = open
        requestedInitialVelocity = initialVelocity
        animationRequest += 1
    }

    private fun requestFreshFrameRecording() {
        miniFrameReady = false
        fullBackgroundFrameReady = false
        fullContentFrameReady = false
        frameRecordingGeneration += 1
    }

}

@Composable
internal fun rememberPlayerSheetTransitionState(): PlayerSheetTransitionState {
    val density = LocalDensity.current.density
    val state = rememberSaveable(saver = PlayerSheetTransitionState.Saver) { PlayerSheetTransitionState() }
    SideEffect { state.gestureDensity = density }
    return state
}

@Composable
internal fun rememberPlayerSheetVerticalDragModifier(
    enabled: Boolean,
    hasItem: Boolean,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: (Float) -> Unit,
    onDragCancel: () -> Unit,
): Modifier {
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDragCancel by rememberUpdatedState(onDragCancel)
    if (!enabled || !hasItem) return Modifier

    return Modifier.pointerInput(enabled, hasItem) {
        val velocityTracker = VelocityTracker()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            velocityTracker.resetTracking()
            velocityTracker.addPointerInputChange(down)
            var delta = Offset.Zero
            var started = false
            var released = false
            try {
                while (!started) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                        ?: return@awaitEachGesture
                    if (change.isConsumed || !change.pressed) return@awaitEachGesture
                    velocityTracker.addPointerInputChange(change)
                    delta += change.positionChange()
                    when (playerSheetDragAxis(delta, viewConfiguration.touchSlop)) {
                        PlayerSheetDragAxis.HORIZONTAL -> return@awaitEachGesture
                        PlayerSheetDragAxis.VERTICAL -> {
                            started = true
                            change.consume()
                            currentOnDragStart()
                            currentOnDrag(delta.y - sign(delta.y) * viewConfiguration.touchSlop)
                        }
                        PlayerSheetDragAxis.PENDING -> {
                            // Ancestors may claim this event after our Main pass.
                            if (awaitPointerEvent(PointerEventPass.Final).changes.any {
                                it.id == down.id && it.isConsumed
                            }) return@awaitEachGesture
                        }
                    }
                }
                val completed = drag(down.id) { change ->
                    // Keep original event coordinates while the host moves under the pointer.
                    velocityTracker.addPointerInputChange(change)
                    currentOnDrag(change.positionChange().y)
                    change.consume()
                }
                if (completed) {
                    currentEvent.changes.firstOrNull { !it.pressed && it.previousPressed }
                        ?.let(velocityTracker::addPointerInputChange)
                    released = true
                    currentOnDragEnd(velocityTracker.calculateVelocity().y)
                }
            } finally {
                if (started && !released) currentOnDragCancel()
            }
        }
    }
}

internal fun playerSheetInputOwner(
    targetOpen: Boolean,
    isDragging: Boolean,
    dragStartedFromMiniPlayer: Boolean,
    progress: Float,
    sharedLayersReady: Boolean,
): PlayerInputOwner = when {
    isDragging -> if (dragStartedFromMiniPlayer) PlayerInputOwner.MINI else PlayerInputOwner.FULL
    !targetOpen && progress.coerceIn(0f, 1f) <= PLAYER_LAYER_HANDOFF_END_PROGRESS ->
        PlayerInputOwner.MINI
    sharedLayersReady || targetOpen && progress >= 1f -> PlayerInputOwner.FULL
    else -> PlayerInputOwner.NONE
}

internal fun playerSheetMiniPlayerAcceptsInput(
    targetOpen: Boolean,
    isDragging: Boolean,
    dragStartedFromMiniPlayer: Boolean,
    progress: Float,
): Boolean = playerSheetInputOwner(
    targetOpen = targetOpen,
    isDragging = isDragging,
    dragStartedFromMiniPlayer = dragStartedFromMiniPlayer,
    progress = progress,
    sharedLayersReady = false,
) == PlayerInputOwner.MINI

internal fun playerSheetResidentHostTranslationY(
    miniPlayerAcceptsInput: Boolean,
    windowHeight: Int,
): Float = if (miniPlayerAcceptsInput) {
    windowHeight.coerceAtLeast(0).toFloat()
} else {
    0f
}
