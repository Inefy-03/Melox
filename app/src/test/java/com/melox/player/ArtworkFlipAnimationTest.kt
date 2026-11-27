package com.melox.player

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.MotionDurationScale
import com.melox.player.model.AppSettings
import com.melox.player.ui.component.playback.ArtworkFlipState
import com.melox.player.ui.component.playback.ArtworkFlipFrame
import com.melox.player.ui.component.playback.artworkFlipFrame
import com.melox.player.ui.component.playback.artworkFlipCameraDistance
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos

class ArtworkFlipAnimationTest {
    private val clock = BroadcastFrameClock()
    private var time = 0L

    private suspend fun frame() {
        repeat(4) { yield() }
        time += 16_666_667L
        clock.sendFrame(time)
        repeat(4) { yield() }
    }

    private suspend fun finish(job: Job) {
        repeat(100) {
            if (!job.isActive) return
            frame()
        }
        assertTrue("Flip did not settle", job.isCompleted)
    }

    @Test
    fun cameraDistanceIsPositiveBeforeLayoutAndAtAnyArtworkSize() {
        for ((width, height) in listOf(0f to 0f, 32f to 32f, 1080f to 1080f, 600f to 1200f)) {
            assertTrue(artworkFlipCameraDistance(width, height) > 0f)
            assertEquals(maxOf(width, height, 1f) * 3f,
                artworkFlipCameraDistance(width, height) * 72f, 0.001f)
        }
    }

    @Test
    fun settingDefaultsToEnabled() {
        assertTrue(AppSettings().artworkFlipAnimation)
    }

    @Test
    fun theTwoFacesHaveOneContinuousProjectedWidthAndOppositeDirections() {
        for (p in listOf(0f, 0.1f, 0.49f, 0.5f, 0.51f, 0.8f, 1f)) {
            val next = artworkFlipFrame(p, 1)
            val previous = artworkFlipFrame(p, -1)
            assertEquals(-next.rotationY, previous.rotationY, 0f)
            assertEquals(p >= 0.5f, next.showBack)
            val width = abs(cos(Math.toRadians(next.rotationY.toDouble())))
            if (p <= 0.5f || p == 1f) {
                assertEquals(abs(cos(Math.PI * p)), width, 0.00001)
            }
        }
        assertEquals(0f, artworkFlipFrame(1f, 1).rotationY, 0f)
    }

    @Test
    fun oldFaceBlursTowardTheEdgeAndNewFaceSharpensAfterIt() {
        val fractions = listOf(0f, 0.25f, 0.5f, 0.6f, 1f).map { artworkFlipFrame(it, 1).blurFraction }
        assertEquals(0f, fractions.first(), 0f)
        assertEquals(0f, fractions.last(), 0f)
        assertEquals(1f, fractions[2], 0f)
        assertTrue(fractions[1] > fractions[0] && fractions[2] > fractions[1])
        assertTrue(fractions[3] < fractions[2] && fractions[4] < fractions[3])
    }

    @Test
    fun incomingFaceOvershootsOnceThenReturnsSharpInEitherDirection() = runBlocking(clock) {
        for (direction in listOf(-1, 1)) {
            val state = ArtworkFlipState("a", "a")
            val job = launch { state.transitionTo("b", "b", direction, 400) }
            val frames = mutableListOf<ArtworkFlipFrame>()
            repeat(100) {
                if (job.isActive) {
                    frame()
                    if (state.frame.showBack) frames += state.frame
                }
            }
            assertTrue(job.isCompleted)
            frames += artworkFlipFrame(1f, direction)
            val angles = frames.map { it.rotationY * direction }
            val peakIndex = angles.indices.maxBy { angles[it] }
            assertTrue(angles[peakIndex] in 7f..7.3f)
            assertTrue(angles.take(peakIndex + 1).zipWithNext().all { (a, b) -> b >= a })
            assertTrue(angles.drop(peakIndex).zipWithNext().all { (a, b) -> b <= a })
            assertTrue(frames.all { it.showBack })
            assertTrue(frames.zip(angles).filter { it.second >= 0f }.all { it.first.blurFraction == 0f })
            assertEquals(0f, angles.last(), 0f)
        }
    }

    @Test
    fun skippingDuringReboundKeepsTheIncomingFaceAndAngleUntilTheNextTurn() = runBlocking(clock) {
        for (direction in listOf(-1, 1)) {
            val state = ArtworkFlipState("a", "a")
            val first = launch { state.transitionTo("b", "b", direction, 400) }
            repeat(27) { frame() }
            val outwardAngle = state.frame.rotationY * direction
            assertTrue(outwardAngle > 0f)
            repeat(3) { frame() }
            val angle = state.frame.rotationY
            assertTrue(angle * direction in 0f..outwardAngle)
            assertEquals(0f, state.frame.blurFraction, 0f)
            first.cancelAndJoin()
            val second = launch { state.transitionTo("c", "c", -direction, 400) }
            repeat(4) { yield() }
            assertEquals("b", state.visibleValue)
            assertEquals(angle, state.frame.rotationY, 0f)
            finish(second)
            assertEquals("c", state.visibleValue)
            assertFalse(state.isRunning)
        }
    }

    @Test
    fun oldFaceRemainsUntilEdgeOnThenNewFaceIsVisible() = runBlocking(clock) {
        val state = ArtworkFlipState("old", "old-key")
        val job = launch { state.transitionTo("new", "new-key", 1, 400) }
        repeat(5) { frame() }
        assertFalse(state.frame.showBack)
        assertEquals("old", state.visibleValue)
        assertEquals("new", state.back)
        repeat(10) { frame() }
        assertTrue(state.frame.showBack)
        assertEquals("new", state.visibleValue)
        assertEquals("old", state.front)
        repeat(12) { frame() }
        val reboundAngle = state.frame.rotationY
        repeat(6) { frame() }
        assertTrue(state.isRunning)
        assertEquals("new", state.visibleValue)
        assertTrue(state.frame.rotationY > 0f && state.frame.rotationY < reboundAngle)
        finish(job)
        assertEquals("new", state.visibleValue)
        assertFalse(state.isRunning)
        assertEquals(0f, state.frame.rotationY, 0f)
        assertTrue(time in 900_000_000L..1_200_000_000L)
    }

    @Test
    fun completingEitherDirectionNeverExposesTheOldFaceAfterTheNewFaceIsVisible() = runBlocking(clock) {
        for (direction in listOf(-1, 1)) {
            val state = ArtworkFlipState("old", "old-key")
            val job = launch { state.transitionTo("new", "new-key", direction, 400) }
            repeat(14) { frame() }
            assertEquals("new", state.visibleValue)
            val observed = mutableListOf<String?>()
            Snapshot.sendApplyNotifications()
            val observer = Snapshot.registerGlobalWriteObserver { observed += state.visibleValue }
            try {
                repeat(100) {
                    if (job.isActive) {
                        Snapshot.sendApplyNotifications()
                        frame()
                    }
                }
                assertTrue(job.isCompleted)
                assertTrue(observed.isNotEmpty())
                assertTrue("Previous face was exposed during completion: $observed",
                    observed.all { it == "new" })
                assertFalse(state.isRunning)
                assertEquals("new", state.visibleValue)
            } finally {
                observer.dispose()
                job.cancelAndJoin()
            }
        }
    }

    @Test
    fun interruptionAtTheEndpointKeepsTheIncomingFaceForTheFollowingFlip() = runBlocking(clock) {
        val state = ArtworkFlipState("a", "a")
        val first = launch { state.transitionTo("b", "b", 1, 400) }
        repeat(14) { frame() }
        // Simulate cancellation after the final animation value, before front promotion.
        state.progress.snapTo(1f)
        first.join()
        assertEquals("a", state.front)
        assertEquals("b", state.back)
        assertEquals("b", state.visibleValue)
        assertEquals(0f, state.frame.rotationY, 0f)
        assertEquals(0f, state.frame.blurFraction, 0f)
        val next = launch { state.transitionTo("c", "c", -1, 400) }
        repeat(4) { frame() }
        assertEquals("b", state.front)
        assertEquals("b", state.visibleValue)
        assertEquals("c", state.back)
        finish(next)
        assertEquals("c", state.visibleValue)
        assertFalse(state.isRunning)
    }

    @Test
    fun preparingEitherDirectionKeepsTheCurrentFaceUntilTheFirstAnimationFrame() = runBlocking(clock) {
        for (direction in listOf(-1, 1)) {
            val state = ArtworkFlipState("old", "old-key")
            val observed = mutableListOf<String?>()
            Snapshot.sendApplyNotifications()
            val observer = Snapshot.registerGlobalWriteObserver { observed += state.visibleValue }
            val job = launch { state.transitionTo("new", "new-key", direction, 400) }
            try {
                repeat(4) { yield() }
                assertTrue(observed.isNotEmpty())
                assertTrue("Incoming face appeared before the flip started: $observed",
                    observed.all { it == "old" })
                assertEquals(0f, state.progress.value, 0f)
                assertEquals("old", state.visibleValue)
            } finally {
                observer.dispose()
                job.cancelAndJoin()
            }
        }
    }

    @Test
    fun confirmedIdenticalArtworkUpdatesItsOwnerWithoutStartingANewFlip() = runBlocking(clock) {
        val shared = Any()
        val higherResolution = Any()
        val state = ArtworkFlipState(shared, "old-key")
        state.transitionTo(shared, "new-key", 1, 400)
        assertFalse(state.isRunning)
        assertTrue(state.visibleValue === shared)
        state.transitionTo(higherResolution, "new-key", 1, 400)
        assertFalse(state.isRunning)
        assertTrue(state.visibleValue === higherResolution)
    }

    @Test
    fun qualityUpgradeDoesNotStartANewTrackFlip() = runBlocking(clock) {
        val state = ArtworkFlipState("low", "track-key")
        state.transitionTo("high", "track-key", 1, 400)
        assertEquals("high", state.visibleValue)
        assertFalse(state.isRunning)
        assertEquals(1f, state.progress.value, 0f)
    }

    @Test
    fun confirmedSharedBackUpdatesItsOwnerBeforeAQualityUpgradeDuringTheFlip() = runBlocking(clock) {
        val shared = Any()
        val highResolution = Any()
        val state = ArtworkFlipState(Any(), "a")
        val first = launch { state.transitionTo(shared, "b", 1, 400) }
        repeat(14) { frame() }
        assertTrue(state.frame.showBack)
        first.cancelAndJoin()
        val nextSong = launch { state.transitionTo(shared, "c", 1, 400) }
        repeat(4) { yield() }
        nextSong.cancelAndJoin()
        val angle = state.frame.rotationY
        val upgrade = launch { state.transitionTo(highResolution, "c", 1, 400) }
        repeat(4) { yield() }
        assertTrue(state.visibleValue === highResolution)
        assertEquals(angle, state.frame.rotationY, 0.001f)
        finish(upgrade)
        assertTrue(state.visibleValue === highResolution)
    }

    @Test
    fun rapidSkipBeforeMidpointKeepsTheOldFaceAndCurrentAngle() = runBlocking(clock) {
        val state = ArtworkFlipState("a", "a")
        val first = launch { state.transitionTo("b", "b", 1, 400) }
        repeat(5) { frame() }
        val angle = state.frame.rotationY
        first.cancelAndJoin()
        val second = launch { state.transitionTo("c", "c", -1, 400) }
        repeat(4) { yield() }
        assertEquals("a", state.visibleValue)
        assertEquals(angle, state.frame.rotationY, 0f)
        assertEquals("c", state.back)
        finish(second)
        assertEquals("c", state.visibleValue)
    }

    @Test
    fun returningToTheOriginalSongBeforeTheMidpointSettlesBackWithoutShowingTheOtherFace() =
        runBlocking(clock) {
            val state = ArtworkFlipState("a", "a")
            val first = launch { state.transitionTo("b", "b", 1, 400) }
            repeat(5) { frame() }
            val progress = state.progress.value
            first.cancelAndJoin()
            val returning = launch { state.transitionTo("a", "a", -1, 400) }
            repeat(3) { frame() }
            assertTrue(state.progress.value < progress)
            while (returning.isActive) {
                assertEquals("a", state.visibleValue)
                frame()
            }
            assertFalse(state.isRunning)
            assertEquals("a", state.visibleValue)
        }

    @Test
    fun rapidSkipAfterMidpointCompletesTheVisibleBackBeforeTurningAgain() = runBlocking(clock) {
        val state = ArtworkFlipState("a", "a")
        val first = launch { state.transitionTo("b", "b", 1, 400) }
        repeat(14) { frame() }
        assertEquals("b", state.visibleValue)
        val angle = state.frame.rotationY
        first.cancelAndJoin()
        val second = launch { state.transitionTo("c", "c", -1, 400) }
        repeat(4) { yield() }
        assertEquals("b", state.visibleValue)
        assertEquals(angle, state.frame.rotationY, 0f)
        finish(second)
        assertEquals("c", state.visibleValue)
        assertEquals(0f, state.frame.rotationY, 0f)
    }

    @Test
    fun disabledSystemAnimationSettlesToTheNewFaceWithoutBlur() {
        val scale = object : MotionDurationScale { override val scaleFactor = 0f }
        runBlocking(clock + scale) {
            val state = ArtworkFlipState("a", "a")
            val job = launch { state.transitionTo("b", "b", 1, 400) }
            finish(job)
            assertEquals("b", state.visibleValue)
            assertFalse(state.isRunning)
            assertEquals(0f, state.frame.blurFraction, 0f)
            assertTrue(time <= 66_666_668L)
        }
    }
}
