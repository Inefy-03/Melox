package com.melox.player.playback

import android.util.LruCache
import com.melox.player.model.PlaybackQueueItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.pow

/** Resolves file tags off the playback thread and latches the setting at a playback boundary. */
internal class PlaybackReplayGain(
    private val scope: CoroutineScope,
    private val readGainDb: (String) -> Double?,
) {
    private data class Key(val uri: String, val modifiedAt: Long, val size: Long)

    private fun PlaybackQueueItem.toKey() = Key(contentUri, dateModifiedEpochSeconds, fileSizeBytes)

    private val cache = LruCache<Key, Float>(64)
    private var currentKey: Key? = null
    private var enabledForCurrentItem = false
    private var loadJob: Job? = null
    private var prefetchJob: Job? = null
    private var prefetchKey: Key? = null

    var enabled = false
        set(value) {
            field = value
            if (!value) {
                prefetchJob?.cancel()
                prefetchKey = null
            }
        }

    @Volatile
    var multiplier = 1f
        private set

    fun select(item: PlaybackQueueItem?, restart: Boolean = false) {
        val key = item?.toKey()
        if (currentKey == key && (!restart || enabled == enabledForCurrentItem)) return
        currentKey = key
        enabledForCurrentItem = enabled
        loadJob?.cancel()
        multiplier = 1f
        if (!enabled || key == null) return
        cache.get(key)?.let {
            multiplier = it
            return
        }
        loadJob = scope.launch {
            multiplier = load(key)
        }
    }

    fun prefetch(item: PlaybackQueueItem?) {
        val key = item?.toKey().takeIf { enabled }
        if (prefetchKey == key) return
        prefetchKey = key
        prefetchJob?.cancel()
        if (key != null && cache.get(key) == null) {
            prefetchJob = scope.launch { load(key) }
        }
    }

    private suspend fun load(key: Key): Float {
        val value = withContext(Dispatchers.IO) { replayGainMultiplier(readGainDb(key.uri)) }
        cache.put(key, value)
        return value
    }
}

internal fun replayGainMultiplier(decibels: Double?): Float =
    decibels
        ?.let { 10.0.pow(it / 20.0).toFloat() }
        ?.takeIf { it.isFinite() && it > 0f }
        ?: 1f
