package com.melox.player

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.ui.MotionDurationScale
import com.melox.player.ui.screen.playback.LyricRowMotion
import com.melox.player.ui.screen.playback.lyricCascadeDelays
import com.melox.player.ui.screen.playback.lyricVisibleRowOrder
import com.melox.player.ui.screen.playback.LyricVisualRow
import com.melox.player.ui.screen.playback.LyricCenteringEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LyricRowMotionTest {
    private val clock = BroadcastFrameClock()
    private var frameNanos = 0L

    private suspend fun frame(deltaNanos: Long = 16_666_667L) {
        yield()
        frameNanos += deltaNanos
        clock.sendFrame(frameNanos)
        yield()
    }

    private suspend fun finish(job: Job) {
        repeat(600) {
            if (!job.isActive) return
            frame()
        }
        assertTrue("Spring did not settle", job.isCompleted)
    }

    @Test
    fun tappedForwardAndBackwardTargetsCascadeFromTheirVisiblePositions() = runBlocking(clock) {
        for ((previous, target) in listOf(0 to 5, 9 to 5)) {
            val motion = LyricRowMotion()
            val shift = if (target > previous) 160f else -160f
            val before = mapOf(4 to shift, 5 to 80f + shift, 6 to 160f + shift)
            val after = mapOf(4 to 0, 5 to 80, 6 to 160)
            motion.place(before, after, shift, 0f, true)
            for ((index, y) in before) {
                assertEquals(y, after.getValue(index) + motion.offset(index), 0.001f)
            }
            val animation = launch { motion.settle(220f, listOf(4, 5, 6), cascade = true) }
            repeat(3) { frame() }
            assertTrue(abs(motion.offset(4)) < abs(shift))
            assertEquals("Tapped row must retain its cascade wait", shift, motion.offset(5), 0f)
            finish(animation)
            for (index in before.keys) assertEquals(0f, motion.offset(index), 0f)
        }
    }

    @Test
    fun distantSeeksStartWithoutWaitingForDisposedVisibleRowsInEitherDirection() = runBlocking(clock) {
        for ((indices, shift) in listOf((90..92) to 800f, (0..2) to -800f)) {
            val motion = LyricRowMotion()
            motion.place(emptyMap(), indices.associateWith { it * 80 }, shift, 0f, true)
            val animation = launch { motion.settle(220f, listOf(40, 41, 42), cascade = true) }
            repeat(3) { frame() }
            for (index in indices) {
                assertTrue("Seek stalled at row $index", abs(motion.offset(index)) < abs(shift))
            }
            finish(animation)
            for (index in indices) assertEquals(0f, motion.offset(index), 0f)
        }
    }

    @Test
    fun partialRetentionStartsAtTheFirstSurvivingVisibleRow() = runBlocking(clock) {
        val motion = LyricRowMotion()
        motion.place(emptyMap(), mapOf(42 to 0, 43 to 80, 44 to 160), 160f, 0f, true)
        val animation = launch { motion.settle(220f, listOf(40, 41, 42, 43), cascade = true) }
        repeat(3) { frame() }
        assertTrue("Disposed upper rows delayed the surviving row", motion.offset(42) < 160f)
        assertEquals("Visible lower row lost its cascade", 160f, motion.offset(43), 0f)
        assertEquals("Incoming lower row lost its cascade", 160f, motion.offset(44), 0f)
        finish(animation)
    }

    @Test
    fun centeringEventDistinguishesNewTapsFromPlaybackAcknowledgements() {
        val tap = LyricCenteringEvent(5, 5_000L, 1, false, false, listOf(600, 800))
        // The pending request flag is absent from the production key.
        assertEquals(tap, tap.copy())
        assertTrue(tap != tap.copy(tapRevision = 2))
        assertTrue(tap != tap.copy(focusIndex = 6))
        assertTrue(tap != tap.copy(previewing = true))
        assertTrue(tap != tap.copy(browsing = true))
        assertTrue(tap != tap.copy(requestedPositionMs = 6_000L))
    }

    @Test
    fun unequalHeightRowsPreserveTheirActualPositionsAcrossForwardAndBackwardSeeks() = runBlocking(clock) {
        val motion = LyricRowMotion()
        for (shift in listOf(95, -210, 730, -980)) {
            val before = mapOf(3 to -35f, 4 to 20f, 5 to 190f)
            val after = mapOf(3 to -35 - shift, 4 to 20 - shift, 5 to 190 - shift)
            motion.place(before, after, shift.toFloat(), 0f, true)
            for ((index, y) in before) {
                assertEquals(y, after.getValue(index) + motion.offset(index), 0.001f)
            }
            val animation = launch { motion.settle(220f) }
            finish(animation)
            for (index in before.keys) assertEquals(0f, motion.offset(index), 0f)
        }
    }

    @Test
    fun interruptedSpringKeepsEachRowsPositionAndVelocityWhenRetargeted() = runBlocking(clock) {
        val motion = LyricRowMotion()
        motion.place(emptyMap(), mapOf(0 to 0, 1 to 80), 120f, 0f, true)
        val animation = launch { motion.settle(170f) }
        repeat(10) { frame() }
        val velocity = motion.velocity(0)
        assertTrue(velocity < -1f)
        animation.cancelAndJoin()
        assertEquals(velocity, motion.velocity(0), 0f)
        val before = mapOf(0 to motion.offset(0), 1 to 80f + motion.offset(1))
        val after = mapOf(0 to -80, 1 to 0)
        motion.place(before, after, 200f, velocity, true)
        assertEquals(before.getValue(0), after.getValue(0) + motion.offset(0), 0.001f)
        assertEquals(velocity, motion.velocity(0), 0f)
        val resumed = launch { motion.settle(170f, listOf(0, 1), cascade = true) }
        repeat(3) { frame() }
        assertTrue("Retargeted row stalled for a cascade delay", motion.offset(0) < before.getValue(0) + 80)
        finish(resumed)
    }

    @Test
    fun cascadeSeparatesRowsAndCancellationRemovesPendingWaits() = runBlocking(clock) {
        val motion = LyricRowMotion()
        motion.place(emptyMap(), mapOf(0 to 0, 1 to 80), 100f, 0f, true)
        val animation = launch { motion.settle(220f, listOf(0, 1), cascade = true) }
        repeat(3) { frame() }
        assertTrue(motion.offset(0) < 100f)
        assertEquals(100f, motion.offset(1), 0f)
        animation.cancelAndJoin()
        val held = motion.offset(1)
        repeat(20) { frame() }
        assertEquals("Cancelled wait restarted a row", held, motion.offset(1), 0f)
        val noWait = launch { motion.settle(220f) }
        repeat(3) { frame() }
        assertTrue(motion.offset(1) < held)
        finish(noWait)
    }

    @Test
    fun browsingHandoffPreservesPositionsEvenWhenScrollIsClampedAtAnEdge() = runBlocking(clock) {
        for (consumed in listOf(-100f, -45f, 0f, 30f)) {
            val motion = LyricRowMotion()
            motion.place(mapOf(0 to 100f, 1 to 230f), mapOf(0 to 0, 1 to 80), 100f, -350f, true)
            val before = mapOf(0 to motion.offset(0), 1 to 80f + motion.offset(1))
            motion.absorbScroll(consumed)
            assertEquals(before.getValue(0), -consumed + motion.offset(0), 0.001f)
            assertEquals(before.getValue(1), 80 - consumed + motion.offset(1), 0.001f)
            assertEquals(0f, motion.velocity(0), 0f)
            val animation = launch { motion.settle(80f) }
            finish(animation)
        }
    }

    @Test
    fun retentionContainsEverySpringIncludingOpposingInitialVelocity() = runBlocking(clock) {
        for (velocity in listOf(-3_000f, 0f, 3_000f)) {
            val motion = LyricRowMotion()
            motion.place(mapOf(0 to -180f, 1 to 270f), mapOf(0 to 0, 1 to 80), 180f, velocity, true)
            val reserve = motion.retainedTravel(170f)
            val animation = launch { motion.settle(170f) }
            repeat(300) {
                frame()
                for (index in listOf(0, 1, 20)) {
                    assertTrue(abs(motion.offset(index)) <= reserve + 0.01f)
                }
            }
            assertTrue(animation.isCompleted)
        }
    }

    @Test
    fun firstPlacementAndGeometryChangesClearOldMotionIncludingFallbackRows() = runBlocking(clock) {
        val motion = LyricRowMotion()
        motion.place(emptyMap(), mapOf(0 to 0), 100f, 200f, true)
        motion.place(emptyMap(), mapOf(90 to 0), -800f, -300f, false)
        for (index in listOf(0, 90, 200)) {
            assertEquals(0f, motion.offset(index), 0f)
            assertEquals(0f, motion.velocity(index), 0f)
        }
    }

    @Test
    fun disabledSystemAnimationsSkipBothCascadeWaitAndSpring() = runBlocking(clock) {
        val motion = LyricRowMotion()
        motion.place(emptyMap(), mapOf(0 to 0, 8 to 640), 100f, 0f, true)
        val scale = object : MotionDurationScale { override val scaleFactor = 0f }
        val animation = launch(scale) { motion.settle(170f, listOf(0, 8), cascade = true) }
        repeat(5) { frame() }
        assertTrue(animation.isCompleted)
        assertEquals(0f, motion.offset(8), 0f)
    }

    @Test
    fun visibleDeadlinesRemainStrictlyOrderedWithoutDenseLyricClamping() {
        for (count in listOf(2, 5, 10, 30)) {
            val order = (100 until 100 + count).toList()
            val delays = lyricCascadeDelays(order)
            assertEquals(0L, delays.getValue(order.first()))
            assertTrue(delays.values.max() <= 200L)
            assertTrue(order.zipWithNext().all { (a, b) -> delays.getValue(a) < delays.getValue(b) })
        }
    }

    @Test
    fun orderingUsesDisplayedBoundsBeforePlacementAndExcludesHiddenBuffers() {
        val rows = listOf(
            LyricVisualRow(0, -500f, 80),
            LyricVisualRow(1, -40f, 100),
            LyricVisualRow(2, 180f, 70),
            LyricVisualRow(3, 100f, 70),
            LyricVisualRow(4, 400f, 80),
        )
        assertEquals(listOf(1, 3, 2), lyricVisibleRowOrder(rows, 0f, 300f))
        val delays = lyricCascadeDelays(lyricVisibleRowOrder(rows, 0f, 300f))
        assertEquals(mapOf(1 to 0L, 3 to 50L, 2 to 100L), delays)
    }

    @Test
    fun movingRowsKeepOldTrajectoryUntilTheirOwnDeadlineOnRepeatedReverseTaps() = runBlocking(clock) {
        val motion = LyricRowMotion()
        motion.place(emptyMap(), mapOf(0 to 0, 1 to 80, 2 to 160), 150f, 0f, true)
        val first = launch { motion.settle(170f) }
        repeat(12) { frame() }
        first.cancelAndJoin()
        for (shift in listOf(-240, 190, -120)) {
            val before = (0..2).associateWith { it * 80f + motion.offset(it) }
            val oldVelocity = (0..2).associateWith { motion.velocity(it) }
            val after = (0..2).associateWith { it * 80 - shift }
            motion.place(before, after, shift.toFloat(), 0f, true)
            for (index in 0..2) {
                assertEquals(before.getValue(index), after.getValue(index) + motion.offset(index), 0.001f)
                assertEquals(oldVelocity.getValue(index), motion.velocity(index), 0.001f)
            }
            val beforeTop = motion.offset(0)
            val beforeBottom = motion.offset(2)
            val animation = launch { motion.settle(170f, listOf(0, 1, 2), true) }
            repeat(3) { frame() }
            // Equal old trajectories now respond differently because only the upper target changed.
            val topDelta = motion.offset(0) - beforeTop
            val bottomDelta = motion.offset(2) - beforeBottom
            assertTrue("Retarget skipped the lower delay", abs(topDelta - bottomDelta) > 0.01f)
            animation.cancelAndJoin()
        }
        val final = launch { motion.settle(170f, listOf(0, 1, 2), true) }
        finish(final)
        for (index in 0..2) assertEquals(0f, motion.offset(index), 0f)
    }
    @Test
    fun oneClockKeepsTopFirstAtDifferentRefreshRatesAndAcrossLongFrames() = runBlocking(clock) {
        for (rate in listOf(60, 90, 120)) {
            val motion = LyricRowMotion()
            motion.place(emptyMap(), mapOf(0 to 0, 1 to 80, 2 to 160), 180f, 0f, true)
            val job = launch { motion.settle(170f, listOf(0, 1, 2), cascade = true) }
            repeat(5) {
                frame(1_000_000_000L / rate)
                assertTrue(motion.offset(0) <= motion.offset(1))
                assertTrue(motion.offset(1) <= motion.offset(2))
            }
            frame(150_000_000L)
            assertTrue(motion.offset(0) < motion.offset(1))
            assertTrue(motion.offset(1) < motion.offset(2))
            finish(job)
        }
    }

    @Test
    fun delayedMovingRowFollowsTheExactOldSpringUntilTheDeadline() = runBlocking(clock) {
        val subject = LyricRowMotion()
        val control = LyricRowMotion()
        for (motion in listOf(subject, control)) {
            motion.place(emptyMap(), mapOf(0 to 0, 1 to 80), 150f, 0f, true)
        }
        val firstSubject = launch { subject.settle(170f) }
        val firstControl = launch { control.settle(170f) }
        repeat(12) { frame() }
        firstSubject.cancelAndJoin()
        firstControl.cancelAndJoin()
        val before = mapOf(0 to subject.offset(0), 1 to 80f + subject.offset(1))
        subject.place(before, mapOf(0 to 240, 1 to 320), -240f, 0f, true)
        val redirected = launch { subject.settle(170f, listOf(0, 1), true) }
        val unchanged = launch { control.settle(170f) }
        repeat(3) { frame() }
        assertEquals(80f + control.offset(1), 320f + subject.offset(1), 0.001f)
        assertEquals(control.velocity(1), subject.velocity(1), 0.001f)
        assertTrue(abs(control.offset(0) - (240f + subject.offset(0))) > 0.01f)
        finish(redirected)
        finish(unchanged)
    }

}
