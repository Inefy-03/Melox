package com.melox.player

import androidx.compose.runtime.mutableStateOf
import com.melox.player.model.PlaybackQueueItem
import com.melox.player.model.PlaybackUiState
import com.melox.player.ui.screen.playback.playbackArtworkPrefetchItems
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackArtworkPrefetchTest {
    @Test
    fun emptyQueuesAndInvalidIndicesProduceNoRequests() {
        for (index in listOf(Int.MIN_VALUE, -1, 0, 15, Int.MAX_VALUE)) {
            assertTrue(playbackArtworkPrefetchItems(PlaybackUiState(currentIndex = index)).isEmpty())
        }
        for (index in listOf(Int.MIN_VALUE, -1, 3, Int.MAX_VALUE)) {
            assertTrue(playbackArtworkPrefetchItems(playback(3, index)).isEmpty())
        }
    }

    @Test
    fun currentItemComesBeforeNextAndPrevious() {
        assertEquals(listOf("1", "2", "0"), targets(playback(4, 1)))
    }

    @Test
    fun neighborsWrapAtBothQueueBoundaries() {
        assertEquals(listOf("0", "1", "3"), targets(playback(4, 0)))
        assertEquals(listOf("3", "0", "2"), targets(playback(4, 3)))
    }

    @Test
    fun shortQueuesDoNotRequestTheSamePositionTwice() {
        assertEquals(listOf("0"), targets(playback(1, 0)))
        assertEquals(listOf("0", "1"), targets(playback(2, 0)))
        assertEquals(listOf("1", "0"), targets(playback(2, 1)))
    }

    @Test
    fun clearingTheLiveQueueDuringAWaveKeepsCapturedItems() = runBlocking {
        val state = mutableStateOf(playback(16, 15))
        val resume = CompletableDeferred<Unit>()
        val observed = mutableListOf<String>()
        val captured = state.value
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            playbackArtworkPrefetchItems(captured).forEachIndexed { index, item ->
                observed += item.mediaId
                if (index == 0) resume.await()
            }
        }
        assertEquals(listOf("15"), observed)
        state.value = PlaybackUiState()
        resume.complete(Unit)
        job.join()

        assertEquals(listOf("15", "0", "14"), observed)
        assertTrue(playbackArtworkPrefetchItems(state.value).isEmpty())
    }

    @Test
    fun replacementDuringAWaveDoesNotMixSongsOrCurrentIndices() = runBlocking {
        val state = mutableStateOf(playback(16, 15))
        val resume = CompletableDeferred<Unit>()
        val observed = mutableListOf<String>()
        val captured = state.value
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            playbackArtworkPrefetchItems(captured).forEachIndexed { index, item ->
                observed += item.mediaId
                if (index == 0) resume.await()
            }
        }
        state.value = PlaybackUiState(listOf(item(100), item(101)), currentIndex = 1)
        resume.complete(Unit)
        job.join()

        assertEquals(listOf("15", "0", "14"), observed)
        assertEquals(listOf("101", "100"), targets(state.value))
    }

    @Test
    fun aPreparedWaveDoesNotKeepIndexingItsBackingList() = runBlocking {
        val queue = (0 until 16).map(::item).toMutableList()
        val prepared = playbackArtworkPrefetchItems(PlaybackUiState(queue, currentIndex = 15))
        val resume = CompletableDeferred<Unit>()
        val observed = mutableListOf<String>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            prepared.forEachIndexed { index, item ->
                observed += item.mediaId
                if (index == 0) resume.await()
            }
        }
        queue.clear()
        resume.complete(Unit)
        job.join()

        assertEquals(listOf("15", "0", "14"), observed)
    }

    private fun targets(playback: PlaybackUiState): List<String> =
        playbackArtworkPrefetchItems(playback).map(PlaybackQueueItem::mediaId)

    private fun playback(size: Int, index: Int) =
        PlaybackUiState(queue = (0 until size).map(::item), currentIndex = index)

    private fun item(id: Int) = PlaybackQueueItem(
        mediaId = id.toString(),
        trackId = id.toLong(),
        contentUri = "content://music/$id",
        title = "Track $id",
        artist = null,
        album = null,
        durationMs = 60_000,
        dateModifiedEpochSeconds = 0,
        fileSizeBytes = 1,
    )
}
