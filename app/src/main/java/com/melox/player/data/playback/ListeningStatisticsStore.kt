package com.melox.player.data.playback

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.melox.player.model.PlaybackQueueItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs
import kotlin.math.roundToLong

private const val MINUTE_MS = 60_000L
private const val BUCKET_MS = 15 * 60 * 1000L

internal data class ListeningPeriod(val start: LocalDate, val endExclusive: LocalDate)

internal fun List<Long>.recordedDayAverage(): Double {
    val recordedDays = count { it > 0L }
    return if (recordedDays == 0) 0.0 else sum().toDouble() / recordedDays
}

internal enum class ListeningRange {
    DAY, WEEK, MONTH;

    val rollingDays: Int
        get() = when (this) {
            DAY -> 1
            WEEK -> 7
            MONTH -> 30
        }

    fun start(date: LocalDate): LocalDate = when (this) {
        DAY -> date
        WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        MONTH -> date.withDayOfMonth(1)
    }

    fun shift(start: LocalDate, amount: Long): LocalDate = when (this) {
        DAY -> start.plusDays(amount)
        WEEK -> start.plusWeeks(amount)
        MONTH -> start.plusMonths(amount)
    }

    fun period(today: LocalDate, offset: Int): ListeningPeriod {
        if (offset == 0 && this != DAY) {
            return ListeningPeriod(today.minusDays(rollingDays.toLong() - 1L), today.plusDays(1))
        }
        val calendarOffset = if (this != DAY && offset > 0) offset - 1 else offset
        val start = shift(start(today), calendarOffset.toLong())
        return ListeningPeriod(start, shift(start, 1))
    }

    fun size(period: ListeningPeriod): Int =
        if (this == DAY) 24 else ChronoUnit.DAYS.between(period.start, period.endExclusive).toInt()

    fun previousComparisonPeriod(period: ListeningPeriod, offset: Int): ListeningPeriod {
        val previousStart = if (this != DAY && offset == 0) period.start.minusDays(rollingDays.toLong())
        else shift(period.start, -1)
        return ListeningPeriod(previousStart, period.start)
    }

    fun headlineValue(values: List<Long>): Double =
        if (this == DAY) values.sum().toDouble() else values.recordedDayAverage()

    fun canGoPrevious(today: LocalDate, offset: Int, historyStart: LocalDate?): Boolean =
        (this != DAY && offset > 0) ||
            (historyStart != null && period(today, offset).start > start(historyStart))

    fun index(start: LocalDate, bucket: Instant, zone: ZoneId): Int {
        val local = bucket.atZone(zone)
        return if (this == DAY) local.hour else ChronoUnit.DAYS.between(start, local.toLocalDate()).toInt()
    }
}

internal data class ListeningAmount(val count: Long = 0, val durationMs: Long = 0) {
    operator fun plus(other: ListeningAmount) = ListeningAmount(count + other.count, durationMs + other.durationMs)
}

internal data class ListenedSong(
    val uri: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val amount: ListeningAmount,
)

internal data class ListeningStatistics(
    val buckets: List<ListeningAmount>,
    val songs: List<ListenedSong>,
    val previousBuckets: List<ListeningAmount>? = null,
)

internal data class ListeningDelta(
    val song: PlaybackQueueItem,
    val wallTimeMs: Long,
    val amount: ListeningAmount,
) {
    fun split(): List<Pair<Long, ListeningAmount>> = buildList {
        var cursor = wallTimeMs
        var remaining = amount.durationMs
        var count = amount.count
        while (remaining > 0 || count > 0) {
            val start = Math.floorDiv(cursor, BUCKET_MS) * BUCKET_MS
            val duration = minOf(remaining, start + BUCKET_MS - cursor)
            add(start to ListeningAmount(count, duration))
            count = 0
            cursor += duration
            remaining -= duration
        }
    }
}

/** Measures listening independently of position and counts each qualifying play once. */
internal class ListeningRecorder(private val record: (List<ListeningDelta>) -> Unit) {
    private var song: PlaybackQueueItem? = null
    private val pending = mutableListOf<ListeningDelta>()
    private var counted = false
    private var listenedMs = 0L
    private var active = false
    private var startElapsedMs = 0L
    private var startWallMs = 0L

    fun update(
        current: PlaybackQueueItem?,
        playing: Boolean,
        elapsedMs: Long,
        wallMs: Long,
        newPlay: Boolean = false,
    ) {
        val sameSong = song?.contentUri == current?.contentUri && !newPlay
        val previous = song
        val wasActive = active
        val duration = if (wasActive) (elapsedMs - startElapsedMs).coerceAtLeast(0L) else 0L
        if (previous != null && duration > 0L) {
            val delta = ListeningDelta(previous, startWallMs, ListeningAmount(durationMs = duration))
            if (counted) {
                record(listOf(delta))
            } else {
                pending.add(delta)
                val threshold = playCountThresholdMs(previous.durationMs)
                if (listenedMs + duration >= threshold) {
                    val reachedAt = startWallMs + (threshold - listenedMs).coerceAtLeast(0L)
                    pending.add(ListeningDelta(previous, reachedAt, ListeningAmount(count = 1)))
                    record(pending.toList())
                    pending.clear()
                    counted = true
                }
                listenedMs += duration
            }
        }
        if (!sameSong) {
            pending.clear()
            counted = false
            listenedMs = 0L
        }
        val predictedWallMs = startWallMs + duration
        startWallMs = if (sameSong && wasActive && abs(wallMs - predictedWallMs) < 2_000L) {
            predictedWallMs
        } else wallMs
        startElapsedMs = elapsedMs
        active = current != null && playing
        song = current
    }
}

internal fun playCountThresholdMs(trackDurationMs: Long): Long {
    // At 75 seconds, 80% equals one minute. Longer or unknown tracks use the time limit.
    return if (trackDurationMs in 1L..(MINUTE_MS * 5L / 4L)) {
        (trackDurationMs * 4L + 4L) / 5L
    } else {
        MINUTE_MS
    }
}

internal class ListeningStatisticsStore(context: Context) :
    SQLiteOpenHelper(context, "listening_statistics.db", null, 2) {

    private sealed interface Command {
        data class Write(val deltas: List<ListeningDelta>) : Command
        data class Flush(val done: CompletableDeferred<Unit>) : Command
    }

    private val commands = Channel<Command>(Channel.UNLIMITED)
    private val revision = MutableStateFlow(0L)

    init {
        setWriteAheadLoggingEnabled(true)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            for (command in commands) {
                when (command) {
                    is Command.Write -> try {
                        write(command.deltas)
                        revision.value++
                    } catch (exception: SQLiteException) {
                        Log.e("ListeningStatistics", "Could not write listening history", exception)
                    }
                    is Command.Flush -> command.done.complete(Unit)
                }
            }
        }
    }

    fun record(deltas: List<ListeningDelta>) {
        commands.trySend(Command.Write(deltas))
    }

    suspend fun flush() {
        val done = CompletableDeferred<Unit>()
        commands.send(Command.Flush(done))
        done.await()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE listening (
                bucket INTEGER NOT NULL, uri TEXT NOT NULL, title TEXT NOT NULL,
                artist TEXT, album TEXT, plays INTEGER NOT NULL, duration_seconds REAL NOT NULL,
                qualified_duration_seconds REAL NOT NULL DEFAULT 0,
                PRIMARY KEY (bucket, uri)
            )""".trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // The legacy bucket totals mix qualified and skipped plays. Keep the counts and archive those durations.
            db.execSQL("ALTER TABLE listening ADD COLUMN qualified_duration_seconds REAL NOT NULL DEFAULT 0")
        }
    }

    private fun write(deltas: List<ListeningDelta>) {
        val db = writableDatabase
        db.beginTransactionNonExclusive()
        try {
            for (delta in deltas) {
                val song = delta.song
                for ((bucket, amount) in delta.split()) {
                    db.execSQL(
                        "INSERT OR IGNORE INTO listening (bucket, uri, title, artist, album, plays, duration_seconds) VALUES (?, ?, ?, ?, ?, 0, 0)",
                        arrayOf<Any?>(bucket, song.contentUri, song.title, song.artist, song.album),
                    )
                    db.execSQL(
                        "UPDATE listening SET plays = plays + ?, qualified_duration_seconds = qualified_duration_seconds + ? WHERE bucket = ? AND uri = ?",
                        arrayOf<Any>(amount.count, amount.durationMs / 1_000.0, bucket, song.contentUri),
                    )
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun observeOldestHistoryDate(zone: ZoneId, hiddenUris: Set<String>) = revision.map {
        readableDatabase.rawQuery(
            "SELECT bucket, uri FROM listening WHERE plays > 0 OR qualified_duration_seconds > 0 ORDER BY bucket",
            null,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(1) !in hiddenUris) {
                    return@use Instant.ofEpochMilli(cursor.getLong(0)).atZone(zone).toLocalDate()
                }
            }
            null
        }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)

    fun observe(
        range: ListeningRange,
        period: ListeningPeriod,
        previousPeriod: ListeningPeriod?,
        zone: ZoneId,
        hiddenUris: Set<String>,
    ) = revision.map {
        read(range, period, zone, hiddenUris).copy(
            previousBuckets = previousPeriod?.let { read(range, it, zone, hiddenUris).buckets },
        )
    }.flowOn(Dispatchers.IO)

    private fun read(range: ListeningRange, period: ListeningPeriod, zone: ZoneId, hiddenUris: Set<String>): ListeningStatistics {
        val buckets = MutableList(range.size(period)) { ListeningAmount() }
        val songs = mutableMapOf<String, ListenedSong>()
        val firstDurationBucket = mutableMapOf<String, Int>()
        val from = period.start.atStartOfDay(zone).toInstant().toEpochMilli()
        val until = period.endExclusive.atStartOfDay(zone).toInstant().toEpochMilli()
        readableDatabase.rawQuery(
            "SELECT bucket, uri, title, artist, album, plays, qualified_duration_seconds FROM listening WHERE bucket >= ? AND bucket < ? AND (plays > 0 OR qualified_duration_seconds > 0) ORDER BY bucket",
            arrayOf(from.toString(), until.toString()),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val uri = cursor.getString(1)
                if (uri in hiddenUris) continue
                val index = range.index(period.start, Instant.ofEpochMilli(cursor.getLong(0)), zone)
                if (index !in buckets.indices) continue
                val amount = ListeningAmount(cursor.getLong(5), (cursor.getDouble(6) * 1_000.0).roundToLong())
                if (amount.durationMs > 0) firstDurationBucket.putIfAbsent(uri, index)
                buckets[index] += amount
                songs[uri] = ListenedSong(
                    uri, cursor.getString(2), cursor.getString(3), cursor.getString(4),
                    (songs[uri]?.amount ?: ListeningAmount()) + amount,
                )
            }
        }
        return applyMinimumListeningDuration(buckets, songs.values, firstDurationBucket)
    }
}

/** Applies the minimum to chart totals once per song while keeping each song's actual duration. */
internal fun applyMinimumListeningDuration(
    buckets: MutableList<ListeningAmount>,
    songs: Collection<ListenedSong>,
    firstDurationBucket: Map<String, Int>,
): ListeningStatistics {
    for (song in songs) {
        if (song.amount.durationMs in 1 until MINUTE_MS) {
            val index = firstDurationBucket.getValue(song.uri)
            buckets[index] += ListeningAmount(durationMs = MINUTE_MS - song.amount.durationMs)
        }
    }
    return ListeningStatistics(buckets, songs.toList())
}
