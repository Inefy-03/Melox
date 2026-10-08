package com.melox.player

import com.melox.player.model.LyricLine
import com.melox.player.model.LyricsDocument
import com.melox.player.model.LyricsFormat
import com.melox.player.model.LyricsSource
import com.melox.player.ui.screen.playback.LyricPlaybackClock
import com.melox.player.ui.screen.playback.stabilizedLyricPlaybackPositionMs
import org.junit.Assert.*
import org.junit.Test

class LyricPlaybackClockTest {
    @Test
    fun consecutiveRepeatsResetTheSameDocumentAndItsProgress() {
        val lyrics = LyricsDocument(
            listOf(
                LyricLine("", 0, 5_000, "First", emptyList(), null),
                LyricLine("", 5_000, 10_000, "Last", emptyList(), null),
            ), LyricsFormat.LRC, LyricsSource.EMBEDDED,
        )
        val clock = LyricPlaybackClock(9_900, 0)
        for (iteration in 1L..3L) {
            clock.positionMs.doubleValue = 9_900.0
            assertEquals(1, lyrics.focusLineIndex(clock.positionMs.doubleValue.toLong()))
            assertTrue(clock.resetForIteration(iteration, 20))
            assertEquals(0, lyrics.currentLineIndex(clock.positionMs.doubleValue.toLong()))
            assertTrue(lyrics.lines.first().revealProgress(20, true) < 0.01f)
            assertEquals(20.0, clock.positionMs.doubleValue, 0.0)
        }
    }

    @Test
    fun conflatedRoundTripToTheSameTrackStartsANewClockInterval() {
        val clock = LyricPlaybackClock(90_000, 10)
        // Two media transitions occurred before the UI collected the latest snapshot.
        assertTrue(clock.resetForIteration(12, 0))
        assertEquals(0.0, clock.positionMs.doubleValue, 0.0)
        assertFalse(clock.resetForIteration(12, 0))
        clock.positionMs.doubleValue = stabilizedLyricPlaybackPositionMs(
            clock.positionMs.doubleValue, 17, 16_666_667, 1f,
        )
        assertEquals(17.0, clock.positionMs.doubleValue, 0.0)
    }

    @Test
    fun ordinaryBackwardSamplesNeverResetTheClock() {
        val clock = LyricPlaybackClock(9_900, 2)
        assertFalse(clock.resetForIteration(2, 20))
        clock.positionMs.doubleValue = stabilizedLyricPlaybackPositionMs(
            clock.positionMs.doubleValue, 20, 16_666_667, 1f,
        )
        assertTrue(clock.positionMs.doubleValue > 9_900)
    }

    @Test
    fun suspendedFramesReanchorToPlaybackInsteadOfAddingBackgroundTime() {
        assertEquals(12_000.0, stabilizedLyricPlaybackPositionMs(
            10_000.0, 12_000, 30_000_000_000L, 1f,
        ), 0.0)
        assertEquals(2_000.0, stabilizedLyricPlaybackPositionMs(
            10_000.0, 2_000, 30_000_000_000L, 1f,
        ), 0.0)
        assertEquals(32_000.0, stabilizedLyricPlaybackPositionMs(
            10_000.0, 32_000, 30_000_000_000L, 1f,
        ), 0.0)
    }

    @Test
    fun lateRepeatSampleResetsToItsRealPositionAndIsConsumedOnlyOnce() {
        val clock = LyricPlaybackClock(240_000, 0)
        assertTrue(clock.resetForIteration(1, 650))
        clock.positionMs.doubleValue = stabilizedLyricPlaybackPositionMs(
            clock.positionMs.doubleValue, 650, 16_666_667, 1f,
        )
        val advanced = clock.positionMs.doubleValue
        assertFalse(clock.resetForIteration(1, 650))
        assertEquals(advanced, clock.positionMs.doubleValue, 0.0)
        assertTrue(advanced > 650 && advanced < 700)
    }
}
