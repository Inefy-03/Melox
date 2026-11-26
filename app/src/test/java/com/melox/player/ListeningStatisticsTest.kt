package com.melox.player

import com.melox.player.data.playback.ListenedSong
import com.melox.player.data.playback.ListeningAmount
import com.melox.player.data.playback.ListeningDelta
import com.melox.player.data.playback.ListeningRange
import com.melox.player.data.playback.ListeningRecorder
import com.melox.player.data.playback.applyMinimumListeningDuration
import com.melox.player.data.playback.playCountThresholdMs
import com.melox.player.data.playback.recordedDayAverage
import com.melox.player.model.PlaybackQueueItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ListeningStatisticsTest {
    @Test
    fun playCountRequiresReachingEitherLimit() {
        assertEquals(24_000L, playCountThresholdMs(30_000))
        assertEquals(24_001L, playCountThresholdMs(30_001))
        assertEquals(48_000L, playCountThresholdMs(60_000))
        assertEquals(56_000L, playCountThresholdMs(70_000))
        assertEquals(60_000L, playCountThresholdMs(75_000))
        assertEquals(60_000L, playCountThresholdMs(200_000))
        assertEquals(60_000L, playCountThresholdMs(0))
    }

    @Test
    fun qualifyingPlayCountsOnceAcrossPausesAndAgainOnRepeat() {
        val song = song(30_000L)
        val deltas = mutableListOf<ListeningDelta>()
        val recorder = ListeningRecorder { deltas.addAll(it) }
        recorder.update(song, true, 0, 0)
        recorder.update(song, false, 15_000, 15_000)
        recorder.update(song, true, 100_000, 100_000)
        recorder.update(song, true, 108_999, 108_999)
        assertEquals(emptyList<ListeningDelta>(), deltas)
        recorder.update(song, true, 109_000, 109_000)
        assertEquals(1L, deltas.sumOf { it.amount.count })
        recorder.update(song, true, 115_000, 115_000, newPlay = true)
        assertEquals(1L, deltas.sumOf { it.amount.count })
        recorder.update(song, false, 142_000, 142_000)
        assertEquals(2L, deltas.sumOf { it.amount.count })
        assertEquals(57_000L, deltas.sumOf { it.amount.durationMs })
        assertEquals(listOf(109_000L, 139_000L), deltas.filter { it.amount.count > 0 }.map { it.wallTimeMs })
    }

    @Test
    fun skippedPlayContributesNeitherCountNorDuration() {
        val song = song(30_000L)
        val deltas = mutableListOf<ListeningDelta>()
        val recorder = ListeningRecorder { deltas.addAll(it) }
        recorder.update(song, true, 0, 0)
        recorder.update(song, true, 15_000, 15_000)
        recorder.update(song, true, 23_999, 23_999, newPlay = true)
        assertEquals(emptyList<ListeningDelta>(), deltas)

        recorder.update(null, false, 47_999, 47_999)
        assertEquals(1L, deltas.sumOf { it.amount.count })
        assertEquals(24_000L, deltas.sumOf { it.amount.durationMs })
        assertEquals(23_999L, deltas.first().wallTimeMs)
    }

    @Test
    fun rawDurationCrossesMidnightAndUsesLocalCalendarBuckets() {
        val midnight = Instant.parse("2026-11-26T16:00:00Z").toEpochMilli()
        val segments = ListeningDelta(song(200_000L), midnight - 1_500L, ListeningAmount(0, 4_000)).split()
        val zone = ZoneId.of("Asia/Shanghai")
        val day = LocalDate.of(2026, 11, 27)
        val weekStart = ListeningRange.WEEK.start(day)
        assertEquals(LocalDate.of(2026, 11, 23), weekStart)
        assertEquals(listOf(23, 0), segments.map { ListeningRange.DAY.index(day, Instant.ofEpochMilli(it.first), zone) })
        assertEquals(listOf(3, 4), segments.map { ListeningRange.WEEK.index(weekStart, Instant.ofEpochMilli(it.first), zone) })
        assertEquals(listOf(1_500L, 2_500L), segments.map { it.second.durationMs })
    }

    @Test
    fun rollingPeriodsAndCalendarHistoryUseDifferentBoundaries() {
        val today = LocalDate.of(2028, 3, 2)
        val week = ListeningRange.WEEK
        val month = ListeningRange.MONTH
        assertEquals(today.minusDays(6), week.period(today, 0).start)
        assertEquals(today.plusDays(1), week.period(today, 0).endExclusive)
        assertEquals(week.start(today).minusWeeks(1), week.period(today, -1).start)
        assertEquals(week.start(today), week.period(today, 1).start)
        assertEquals(today.minusDays(29), month.period(today, 0).start)
        assertEquals(30, month.size(month.period(today, 0)))
        assertEquals(LocalDate.of(2028, 2, 1), month.period(today, -1).start)
        assertEquals(29, month.size(month.period(today, -1)))
        assertEquals(LocalDate.of(2028, 3, 1), month.period(today, 1).start)
    }

    @Test
    fun historyNavigationComparesCurrentPeriodWithTheHistoryBoundary() {
        val today = LocalDate.of(2026, 11, 26)
        val historyStart = today.minusDays(4)
        assertFalse(ListeningRange.DAY.canGoPrevious(today, 0, today))
        assertTrue(ListeningRange.DAY.canGoPrevious(today, -3, historyStart))
        assertFalse(ListeningRange.DAY.canGoPrevious(today, -4, historyStart))
        assertTrue(ListeningRange.WEEK.canGoPrevious(today, 0, historyStart))
        assertFalse(ListeningRange.WEEK.canGoPrevious(today, -1, historyStart))
        assertFalse(ListeningRange.MONTH.canGoPrevious(today, 0, historyStart))
        assertTrue(ListeningRange.MONTH.canGoPrevious(today, 1, today))
        assertFalse(ListeningRange.DAY.canGoPrevious(today, 0, null))
        assertFalse(ListeningRange.WEEK.canGoPrevious(today, 0, null))
        assertTrue(ListeningRange.WEEK.canGoPrevious(today, 1, null))

        val oldestRecord = today.minusDays(120)
        assertTrue(ListeningRange.DAY.canGoPrevious(today, -119, oldestRecord))
        assertFalse(ListeningRange.DAY.canGoPrevious(today, -120, oldestRecord))
        assertTrue(ListeningRange.MONTH.canGoPrevious(today, -3, oldestRecord))
        assertFalse(ListeningRange.MONTH.canGoPrevious(today, -4, oldestRecord))
    }

    @Test
    fun dayBucketsFollowLocalHoursAcrossDaylightSavingChanges() {
        val zone = ZoneId.of("America/New_York")
        val fallBack = LocalDate.of(2026, 11, 1)
        val springForward = LocalDate.of(2026, 3, 8)
        val day = ListeningRange.DAY
        assertEquals(24, day.size(day.period(fallBack, 0)))
        assertEquals(1, day.index(fallBack, Instant.parse("2026-11-01T05:30:00Z"), zone))
        assertEquals(1, day.index(fallBack, Instant.parse("2026-11-01T06:30:00Z"), zone))
        assertEquals(3, day.index(springForward, Instant.parse("2026-03-08T07:30:00Z"), zone))
    }

    @Test
    fun dailyAverageUsesOnlyDaysWithData() {
        assertEquals(0.0, listOf(0L, 0L).recordedDayAverage(), 0.0)
        assertEquals(1.5, listOf(0L, 1L, 0L, 2L).recordedDayAverage(), 0.0)
        assertEquals(90_000.0, listOf(60_000L, 0L, 120_000L).recordedDayAverage(), 0.0)
    }

    @Test
    fun headlineComparisonUsesAdjacentPeriodsAndTheSameMetricOnBothSides() {
        val today = LocalDate.of(2028, 3, 2)
        val day = ListeningRange.DAY
        val week = ListeningRange.WEEK
        val month = ListeningRange.MONTH
        assertEquals(day.period(today, -1), day.previousComparisonPeriod(day.period(today, 0), 0))
        assertEquals(week.period(today, -1), week.previousComparisonPeriod(week.period(today, 1), 1))
        val rollingMonth = month.period(today, 0)
        val priorMonth = month.previousComparisonPeriod(rollingMonth, 0)
        assertEquals(rollingMonth.start, priorMonth.endExclusive)
        assertEquals(30, month.size(priorMonth))
        assertEquals(month.period(today, -1), month.previousComparisonPeriod(month.period(today, 1), 1))
        assertEquals(3.0, day.headlineValue(listOf(0L, 1L, 2L)), 0.0)
        assertEquals(1.5, week.headlineValue(listOf(0L, 1L, 2L)), 0.0)
    }

    @Test
    fun shortSongGetsOneMinuteOnceAcrossAllItsBuckets() {
        val short = ListenedSong("short", "Short", null, null, ListeningAmount(1, 25_000))
        val long = ListenedSong("long", "Long", null, null, ListeningAmount(1, 61_000))
        val buckets = mutableListOf(ListeningAmount(0, 10_000), ListeningAmount(2, 76_000))
        val view = applyMinimumListeningDuration(buckets, listOf(short, long), mapOf("short" to 0, "long" to 1))
        assertEquals(25_000L, short.amount.durationMs)
        assertEquals(listOf(25_000L, 61_000L), view.songs.map { it.amount.durationMs })
        assertEquals(121_000L, view.buckets.sumOf { it.durationMs })
        assertEquals(2L, view.buckets.sumOf { it.count })
    }

    private fun song(durationMs: Long) = PlaybackQueueItem(
        mediaId = "1",
        trackId = 1L,
        contentUri = "content://test/1",
        title = "Song",
        artist = null,
        album = null,
        durationMs = durationMs,
        dateModifiedEpochSeconds = 0L,
        fileSizeBytes = 0L,
    )
}
