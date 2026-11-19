package com.melox.player.optimization

import com.melox.player.data.library.MusicSortConfig
import com.melox.player.data.library.MusicSortField
import com.melox.player.model.MusicTrack
import com.melox.player.ui.viewmodel.MusicPresentationRequest
import com.melox.player.ui.viewmodel.MusicPresentationState
import com.melox.player.ui.viewmodel.musicPresentations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class SearchPresentationOptimizationTest {
    @Test
    fun queriesReuseTheQueueAndIndexWhileMatchingAllExistingFields() = runBlocking {
        val fixture = Fixture(this, MusicSortConfig(MusicSortField.ARTIST))
        try {
            val initial = fixture.next()
            assertEquals(listOf(2L, 1L, 3L), initial.queueItems.map { it.id })
            for ((query, ids) in listOf(
                "ALPHA" to listOf(1L), "zeta" to listOf(1L, 3L),
                "compilation" to listOf(2L), "z.flac" to emptyList(),
                "missing" to emptyList(),
            )) {
                fixture.requests.value = fixture.requests.value.copy(query = query)
                val result = fixture.next()
                assertSame(initial.queueItems, result.queueItems)
                assertEquals(ids, result.items.map { it.id })
                assertSame(initial.queueSectionIndexMap, result.queueSectionIndexMap)
            }
            fixture.requests.value = fixture.requests.value.copy(
                query = "z.flac",
                searchFields = com.melox.player.data.library.SongSearchFields(showFileName = true),
            )
            val fileTitleResult = fixture.next()
            assertEquals(listOf(1L), fileTitleResult.items.map { it.id })
            assertSame(initial.queueItems, fileTitleResult.queueItems)
            fixture.requests.value = fixture.requests.value.copy(query = "  ")
            val cleared = fixture.next()
            assertSame(initial.queueItems, cleared.items)
            assertSame(initial.queueSectionIndexMap, cleared.queueSectionIndexMap)
            assertEquals(mapOf("A" to 0, "Z" to 1), cleared.queueSectionIndexMap)
        } finally { fixture.close() }
    }

    @Test
    fun sortChangesPreserveBothDirectionsAndDeterministicTieOrdering() = runBlocking {
        val fixture = Fixture(this)
        try {
            fixture.next()
            val expected = mapOf(
                MusicSortField.TITLE to listOf(1L, 2L, 3L),
                MusicSortField.ARTIST to listOf(2L, 1L, 3L),
                MusicSortField.FILE_NAME to listOf(2L, 3L, 1L),
                MusicSortField.FILE_SIZE to listOf(2L, 3L, 1L),
                MusicSortField.DATE_ADDED to listOf(1L, 3L, 2L),
                MusicSortField.DURATION to listOf(2L, 3L, 1L),
            )
            for ((field, ids) in expected) {
                for (descending in listOf(true, false)) {
                    val config = MusicSortConfig(field, descending)
                    fixture.requests.value = MusicPresentationRequest(sortConfig = config)
                    val result = fixture.next()
                    assertEquals(config, result.sortConfig)
                    assertEquals(if (descending) ids.reversed() else ids, result.items.map { it.id })
                }
            }
        } finally { fixture.close() }
    }

    @Test
    fun tagRefreshAndFolderRemovalInvalidateTheCachedQueue() = runBlocking {
        val fixture = Fixture(this, MusicSortConfig(MusicSortField.ARTIST))
        try {
            val initial = fixture.next()
            fixture.tracks.value = fixture.tracks.value.map {
                if (it.id == 3L) it.copy(artist = "0 artist", title = "Changed") else it
            }
            val refreshed = fixture.next()
            assertNotSame(initial.queueItems, refreshed.queueItems)
            assertEquals(listOf(3L, 2L, 1L), refreshed.queueItems.map { it.id })
            assertEquals("Changed", refreshed.items.first().title)
            fixture.tracks.value = fixture.tracks.value.filterNot { it.id == 2L }
            val hidden = fixture.next()
            assertEquals(listOf(3L, 1L), hidden.queueItems.map { it.id })
            assertFalse(hidden.queueSectionIndexMap.containsKey("A"))
        } finally { fixture.close() }
    }

    @Test
    fun rapidQueryAndSortChangesConvergeToOneConsistentLatestPresentation() = runBlocking {
        val fixture = Fixture(this)
        try {
            fixture.next()
            repeat(40) {
                fixture.requests.value = MusicPresentationRequest("missing-$it",
                    MusicSortConfig(MusicSortField.ARTIST, it % 2 == 0))
            }
            val final = MusicPresentationRequest("gamma", MusicSortConfig(MusicSortField.DURATION, true))
            fixture.requests.value = final
            val result = withTimeout(5_000L) {
                var state = fixture.next()
                while (state.query != final.query) state = fixture.next()
                state
            }
            assertEquals(final.sortConfig, result.sortConfig)
            assertEquals(listOf(1L, 3L, 2L), result.queueItems.map { it.id })
            assertEquals(listOf(3L), result.items.map { it.id })
        } finally { fixture.close() }
    }

    private class Fixture(scope: CoroutineScope, config: MusicSortConfig = MusicSortConfig()) {
        val tracks = MutableStateFlow(listOf(
            track(1L, "Alpha", "Zeta", "Album", "z.flac", 10L, 30L, 300L),
            track(2L, "Beta", "Artist", "Compilation", "a.flac", 30L, 10L, 100L),
            track(3L, "Gamma", "Zeta", null, "m.flac", 20L, 20L, 200L),
        ))
        val requests = MutableStateFlow(MusicPresentationRequest(sortConfig = config))
        private val states = Channel<MusicPresentationState>(Channel.UNLIMITED)
        private val job = scope.launch { musicPresentations(tracks, requests).collect(states::send) }
        suspend fun next(): MusicPresentationState = withTimeout(5_000L) { states.receive() }
        suspend fun close() = job.cancelAndJoin()
    }

    companion object {
        private fun track(id: Long, title: String, artist: String, album: String?, name: String,
            added: Long, size: Long, duration: Long) = MusicTrack(
            id = id, title = title, artist = artist, album = album, durationMs = duration,
            dateAddedEpochSeconds = added, dateModifiedEpochSeconds = 1L,
            fileName = name, fileSizeBytes = size, contentUri = "content://track/$id",
            titleSectionKey = title.take(1).uppercase(), titleSortKey = title.uppercase(),
        )
    }
}
