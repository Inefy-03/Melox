// SPDX-License-Identifier: Apache-2.0
package com.melox.player.ui.screen.playback

import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.MotionDurationScale
import kotlinx.coroutines.currentCoroutineContext
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.math.sqrt

internal const val LYRIC_ROW_DAMPING_RATIO = 1.1f

/** Acknowledgement changes pending state, but not this centering identity. */
internal data class LyricCenteringEvent(
    val focusIndex: Int,
    val requestedPositionMs: Long,
    val tapRevision: Int,
    val browsing: Boolean,
    val previewing: Boolean,
    val geometry: List<Any>,
    val translationRevision: Int = 0,
)

internal data class LyricVisualRow(val index: Int, val top: Float, val height: Int)

internal fun lyricVisibleRowOrder(
    rows: List<LyricVisualRow>,
    viewportStart: Float,
    viewportEnd: Float,
): List<Int> = rows.filter { it.top + it.height > viewportStart && it.top < viewportEnd }
    .sortedWith(compareBy<LyricVisualRow> { it.top }.thenBy { it.index })
    .map { it.index }

internal fun lyricCascadeDelays(visibleOrder: List<Int>): Map<Int, Long> {
    val stepMs = minOf(50.0, 200.0 / (visibleOrder.size - 1).coerceAtLeast(1))
    return visibleOrder.mapIndexed { rank, index -> index to (rank * stepMs).roundToLong() }.toMap()
}

/** Document-owned values survive lazy-item disposal; one clock samples every row. */
internal class LyricRowMotion {
    private class Row {
        val translation = mutableFloatStateOf(0f)
        var velocity = 0f
        var target = 0f

        fun place(value: Float, preservedVelocity: Float, animate: Boolean) {
            target = if (animate) target + value - translation.floatValue else 0f
            translation.floatValue = if (animate) value else 0f
            velocity = if (animate) preservedVelocity else 0f
        }

        fun springTo(stiffness: Float, destination: Float) = TargetBasedAnimation(
            animationSpec = spring<Float>(
                dampingRatio = LYRIC_ROW_DAMPING_RATIO,
                stiffness = stiffness,
                visibilityThreshold = 0.01f,
            ),
            typeConverter = Float.VectorConverter,
            initialValue = translation.floatValue,
            targetValue = destination,
            initialVelocityVector = AnimationVector1D(velocity),
        )
    }

    private class RunningRow(val row: Row, stiffness: Float, val waitNanos: Long) {
        private var animation = row.springTo(stiffness, row.target)
        private var switched = false
        private val stiffness = stiffness

        fun sample(elapsedNanos: Long): Boolean {
            if (!switched && elapsedNanos >= waitNanos) {
                // Sample the old trajectory at the exact deadline, even after a slow frame.
                row.translation.floatValue = animation.getValueFromNanos(waitNanos)
                row.velocity = animation.getVelocityVectorFromNanos(waitNanos).value
                row.target = 0f
                animation = row.springTo(stiffness, 0f)
                switched = true
            }
            val playTime = if (switched) elapsedNanos - waitNanos else elapsedNanos
            row.translation.floatValue = animation.getValueFromNanos(playTime)
            row.velocity = animation.getVelocityVectorFromNanos(playTime).value
            return switched && animation.isFinishedFromNanos(playTime)
        }
    }

    private val reference = Row()
    private val rows = mutableStateMapOf<Int, Row>()

    fun offset(index: Int): Float = (rows[index] ?: reference).translation.floatValue
    fun velocity(index: Int): Float = (rows[index] ?: reference).velocity

    fun retainedTravel(stiffness: Float): Float = (rows.values + reference).maxOf { row ->
        // Include the old target followed during the delay, and its remaining kinetic energy.
        abs(row.target) + abs(row.translation.floatValue - row.target) +
            abs(row.velocity) / sqrt(stiffness.coerceAtLeast(1f))
    }

    fun place(
        previousVisualOffsets: Map<Int, Float>,
        placedOffsets: Map<Int, Int>,
        fallbackTranslation: Float,
        fallbackVelocity: Float,
        animate: Boolean,
    ) {
        for ((index, offset) in placedOffsets) {
            val oldVelocity = rows[index]?.velocity ?: fallbackVelocity
            val translation = previousVisualOffsets[index]?.minus(offset) ?: fallbackTranslation
            rows.getOrPut(index) { Row() }.place(translation, oldVelocity, animate)
        }
        rows.keys.retainAll(placedOffsets.keys)
        reference.place(fallbackTranslation, fallbackVelocity, animate)
    }

    /** Positive consumed scroll moves layout upwards; compensate by the same amount. */
    fun absorbScroll(consumedPx: Float) {
        for (row in rows.values + reference) {
            row.translation.floatValue += consumedPx
            row.velocity = 0f
            row.target = row.translation.floatValue
        }
    }

    suspend fun settle(
        stiffness: Float,
        visibleOrder: List<Int> = emptyList(),
        cascade: Boolean = false,
    ) {
        // Distant seeks can dispose the old visible rows before the spring starts.
        val retainedVisibleOrder = visibleOrder.filter { it in rows }
        val delays = if (cascade) lyricCascadeDelays(retainedVisibleOrder) else emptyMap()
        val lastVisibleIndex = retainedVisibleOrder.maxOrNull()
        val running = (rows.map { (index, row) ->
            val delay = delays[index] ?: if (
                cascade && lastVisibleIndex != null && index > lastVisibleIndex
            ) delays.values.maxOrNull() ?: 0L else 0L
            RunningRow(row, stiffness, delay * 1_000_000L)
        } + RunningRow(reference, stiffness, 0L))
        val context = currentCoroutineContext()
        var previousFrame = withFrameNanos { it }
        var elapsedNanos = 0L
        while (true) {
            val scale = context[MotionDurationScale]?.scaleFactor ?: 1f
            if (scale <= 0f) {
                for (row in rows.values + reference) row.place(0f, 0f, false)
                return
            }
            // Do not short-circuit: every row must be sampled at the same timestamp.
            var finished = true
            for (row in running) {
                if (!row.sample(elapsedNanos)) finished = false
            }
            if (finished) {
                for (row in rows.values + reference) row.place(0f, 0f, false)
                return
            }
            val frame = withFrameNanos { it }
            elapsedNanos += ((frame - previousFrame).coerceAtLeast(0L) / scale).roundToLong()
            previousFrame = frame
        }
    }
}
