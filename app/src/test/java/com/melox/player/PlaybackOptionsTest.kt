package com.melox.player

import androidx.compose.runtime.MonotonicFrameClock
import com.melox.player.playback.remainingExtensionSeconds
import com.melox.player.playback.nextItemIndexAfterExtendedTimer
import com.melox.player.playback.sleepTimerTick
import com.melox.player.model.AppSettings
import com.melox.player.model.normalizePlaybackSpeed
import com.melox.player.ui.screen.playback.formatTimerDuration
import com.melox.player.ui.screen.playback.formatTimerExtension
import com.melox.player.ui.screen.playback.TimerNumberPickerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.math.roundToInt

class PlaybackOptionsTest {
    @Test
    fun savedPlaybackOptionsUseRequestedDefaultsAndValidSpeeds() {
        val settings = AppSettings()
        assertEquals(1f, settings.playbackSpeed)
        assertEquals(true, settings.highPrecisionOutput)
        assertEquals(600, settings.sleepTimerSeconds)
        assertEquals(false, settings.autoExtendSleepTimer)
        assertEquals(true, settings.playbackPauseFade)
        assertEquals(true, settings.audioFocusEnabled)
        assertEquals(com.melox.player.model.TransientAudioFocusBehavior.DUCK, settings.transientAudioFocusBehavior)
        assertEquals(false, settings.autoPlayOnStart)
        assertEquals(false, settings.terminateOnTaskRemoved)
        assertEquals(0.95f, normalizePlaybackSpeed(0.95f))
        assertEquals(0.30f, normalizePlaybackSpeed(0.30f))
        assertEquals(2.95f, normalizePlaybackSpeed(2.95f))
        assertEquals(3f, normalizePlaybackSpeed(3f))
        assertEquals(1f, normalizePlaybackSpeed(1.37f))
    }

    @Test
    fun timerDisplaysFullRangeAndZero() {
        assertEquals("00:00:00", formatTimerDuration(0))
        assertEquals("00:10:00", formatTimerDuration(600))
        assertEquals("23:59:59", formatTimerDuration(86_399))
    }

    @Test
    fun repeatedTimerStartsKeepTicksOnTheNewDeadlinesSecondBoundaries() {
        assertEquals(10, sleepTimerTick(10_000L, 0L)?.remainingSeconds)
        assertEquals(1_000L, sleepTimerTick(10_000L, 0L)?.delayUntilNextSecondMs)
        assertEquals(9, sleepTimerTick(10_000L, 1_175L)?.remainingSeconds)
        assertEquals(825L, sleepTimerTick(10_000L, 1_175L)?.delayUntilNextSecondMs)
        assertEquals(8, sleepTimerTick(10_000L, 2_000L)?.remainingSeconds)
        assertEquals(10, sleepTimerTick(12_150L, 2_150L)?.remainingSeconds)
        assertEquals(1_000L, sleepTimerTick(12_150L, 2_150L)?.delayUntilNextSecondMs)
        assertEquals(9, sleepTimerTick(12_150L, 3_150L)?.remainingSeconds)
        assertNull(sleepTimerTick(12_150L, 12_150L))
    }

    @Test
    fun extensionUsesRealTimeAtPlaybackSpeed() {
        assertEquals(320, remainingExtensionSeconds(640_000L, 0L, 2f))
        assertEquals("+ 00:05:20", formatTimerExtension(320))
        assertEquals(0, remainingExtensionSeconds(640_000L, 640_000L, 1f))
        assertEquals(0, remainingExtensionSeconds(640_000L, 0L, 0f))
    }

    @Test
    fun extendedTimerAdvancesOnceAndWrapsQueue() {
        assertEquals(2, nextItemIndexAfterExtendedTimer(1, 1, 3))
        assertEquals(0, nextItemIndexAfterExtendedTimer(2, 2, 3))
        assertEquals(2, nextItemIndexAfterExtendedTimer(2, 1, 3))
        assertEquals(0, nextItemIndexAfterExtendedTimer(0, 0, 1))
    }

    @Test
    fun timerStartCapturesVisibleValuesBeforeScrollCommits() {
        val hours = TimerNumberPickerState(0, 0..23)
        val minutes = TimerNumberPickerState(10, 0..59)
        val seconds = TimerNumberPickerState(0, 0..59)
        minutes.beginDrag()
        minutes.dragBy(-75f, 45)
        seconds.beginDrag()
        seconds.dragBy(-30f, 45)
        val duration = hours.captureForStart() * 3600 + minutes.captureForStart() * 60 +
            seconds.captureForStart()
        assertEquals(721, duration)
        minutes.dragBy(-450f, 45)
        assertEquals(12, minutes.value)
    }

    @Test
    fun timerWheelsWrapAndCanReachZeroDuringDrag() {
        val hours = TimerNumberPickerState(23, 0..23)
        hours.dragBy(-45f, 45)
        assertEquals(0, hours.captureForStart())
        hours.unlock()
        hours.dragBy(90f, 45)
        assertEquals(22, hours.value)
        val minutes = TimerNumberPickerState(1, 0..59)
        minutes.dragBy(30f, 45)
        assertEquals(0, minutes.value)
        assertEquals(0, minutes.captureForStart())
    }

    @Test
    fun timerStartCancelsFlingAndAlignsCapturedNumber() = runBlocking(
        object : MonotonicFrameClock {
            private var timeNanos = 0L
            override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
                yield()
                timeNanos += 16_666_667L
                return onFrame(timeNanos)
            }
        },
    ) {
        val picker = TimerNumberPickerState(10, 0..59)
        var staleCommit = false
        picker.beginDrag()
        picker.dragBy(-75f, 45)
        picker.fling(this, 12f) { staleCommit = true }
        repeat(5) { yield() }
        val positionBeforeCapture = picker.position
        val targetPosition = positionBeforeCapture.roundToInt().toFloat()
        val captured = picker.captureForStart()
        picker.fling(this, 50f) { staleCommit = true }
        assertEquals(captured, picker.value)
        assertEquals(positionBeforeCapture, picker.position, 0f)
        assertEquals(captured, picker.alignForStart())
        assertEquals(targetPosition, picker.position, 0f)
        assertEquals(false, staleCommit)
        assertEquals(false, picker.isScrolling)
    }

}
