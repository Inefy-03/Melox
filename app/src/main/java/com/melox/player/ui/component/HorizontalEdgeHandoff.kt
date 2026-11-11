package com.melox.player.ui.component

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs
import kotlin.math.sign

/** Records inertial edge impacts, independently of pointer drags. */
internal class HorizontalEdgeHandoff(initialReachedEdges: Int = 0) {
    var reachedEdges = initialReachedEdges
        private set
    var parentDragActive = false
    private var bounceDirection = 0f

    fun shouldHandleDrag(delta: Float, canHandoff: Boolean): Boolean =
        delta != 0f && !canHandoff

    fun recordDragBounce(delta: Float) {
        bounceDirection = delta.sign
    }

    fun isReturningFromBounce(delta: Float): Boolean =
        delta != 0f && bounceDirection != 0f && delta.sign != bounceDirection

    fun shouldBounceInertia(velocity: Float, canHandoff: Boolean): Boolean =
        velocity != 0f && (!canHandoff || (reachedEdges and edgeMask(velocity)) == 0)

    fun recordInertialImpact(velocity: Float) {
        reachedEdges = reachedEdges or edgeMask(velocity)
        bounceDirection = velocity.sign
    }

    private fun edgeMask(direction: Float): Int = if (direction > 0f) 1 else 2
}

internal class HorizontalEdgeFlingBehavior(
    private val scrollState: ScrollableState,
    private val decay: FlingBehavior,
    private val snap: TargetedFlingBehavior,
    private val effect: OverscrollEffect,
    private val handoff: HorizontalEdgeHandoff,
    private val physicalDirection: Float,
    private val canHandoff: Boolean,
) : TargetedFlingBehavior {
    override suspend fun ScrollScope.performFling(
        initialVelocity: Float,
        onRemainingDistanceUpdated: (Float) -> Unit,
    ): Float {
        if (canHandoff && handoff.parentDragActive) return initialVelocity
        var hitBoundary = false
        val localScope = object : ScrollScope {
            override fun scrollBy(pixels: Float): Float {
                // The owning scroll mutation is already active. Keep this fling local to its state.
                val consumed = scrollState.dispatchRawDelta(pixels)
                if (abs(pixels - consumed) > 0.5f) hitBoundary = true
                return consumed
            }
        }
        var remaining = 0f
        var impactVelocity = 0f
        effect.applyToFling(Velocity(initialVelocity * physicalDirection, 0f)) { velocity ->
            remaining = with(decay) { localScope.performFling(velocity.x * physicalDirection) }
            val physicalRemaining = if (hitBoundary) remaining * physicalDirection else 0f
            if (!hitBoundary) {
                remaining = with(snap) { localScope.performFling(0f, onRemainingDistanceUpdated) }
            }
            if (handoff.shouldBounceInertia(physicalRemaining, canHandoff)) {
                impactVelocity = physicalRemaining
                remaining = 0f
                Velocity(velocity.x - physicalRemaining, 0f)
            } else {
                velocity
            }
        }
        if (impactVelocity != 0f) {
            // A low-speed impact can settle below the native effect's visible displacement.
            repeat(3) {
                if (!effect.isInProgress) withFrameNanos { }
            }
            if (effect.isInProgress) handoff.recordInertialImpact(impactVelocity)
        }
        return remaining
    }
}

internal class HorizontalEdgeScroll(
    val modifier: Modifier,
    val flingBehavior: TargetedFlingBehavior,
)

@Composable
internal fun rememberHorizontalEdgeScroll(
    scrollState: ScrollableState,
    snap: TargetedFlingBehavior,
    canHandoff: Boolean = true,
): HorizontalEdgeScroll {
    val effect = rememberOverscrollEffect() ?: return HorizontalEdgeScroll(Modifier, snap)
    val decay = ScrollableDefaults.flingBehavior()
    val physicalDirection = if (LocalLayoutDirection.current == LayoutDirection.Ltr) -1f else 1f
    val handoff = rememberSaveable(saver = Saver(
        save = { it: HorizontalEdgeHandoff -> it.reachedEdges },
        restore = { HorizontalEdgeHandoff(it) },
    )) { HorizontalEdgeHandoff() }
    val dispatcher = remember { NestedScrollDispatcher() }
    val connection = remember(effect, handoff, canHandoff, dispatcher) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (canHandoff && handoff.parentDragActive && source == NestedScrollSource.UserInput) {
                    return dispatcher.dispatchPostScroll(Offset.Zero, available, source)
                }
                if (canHandoff || source != NestedScrollSource.UserInput || !effect.isInProgress ||
                    !handoff.isReturningFromBounce(available.x)
                ) return Offset.Zero
                var remaining = 0f
                effect.applyToScroll(Offset(available.x, 0f), source) {
                    remaining = it.x
                    it
                }
                return Offset(available.x - remaining, 0f)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (canHandoff && source == NestedScrollSource.UserInput && available.x != 0f) {
                    handoff.parentDragActive = true
                }
                if (source != NestedScrollSource.UserInput || !handoff.shouldHandleDrag(available.x, canHandoff)) {
                    return Offset.Zero
                }
                handoff.recordDragBounce(available.x)
                return effect.applyToScroll(Offset(available.x, 0f), source) { Offset.Zero }
            }
        }
    }
    return remember(scrollState, effect, handoff, connection, decay, snap, physicalDirection, canHandoff) {
        HorizontalEdgeScroll(
            modifier = Modifier.overscroll(effect)
                .pointerInput(handoff) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        handoff.parentDragActive = false
                    }
                }
                .nestedScroll(connection, dispatcher),
            flingBehavior = HorizontalEdgeFlingBehavior(scrollState, decay, snap, effect, handoff, physicalDirection, canHandoff),
        )
    }
}
