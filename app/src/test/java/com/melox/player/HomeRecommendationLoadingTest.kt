package com.melox.player

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import com.melox.player.model.MusicTrack
import com.melox.player.ui.screen.home.HomeRecommendationLoader
import com.melox.player.ui.screen.home.HomeRecommendations
import com.melox.player.ui.screen.home.homeRecommendationSources
import com.melox.player.ui.screen.home.rememberHomeRecommendationSelection
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class HomeRecommendationLoadingTest {
    @Test
    fun metadataAndLibraryOrderChangesReuseTheSeedAndAllConfirmedProbes() = runBlocking {
        val loader = HomeRecommendationLoader(42)
        val tracks = tracks(6)
        val probes = AtomicInteger()
        val first = loader.select(tracks, 3, 2) { probes.incrementAndGet(); it.id % 2L == 0L }
        val count = probes.get()
        val updated = tracks.reversed().map { it.copy(title = "Updated ${it.id}", bitrateBitsPerSecond = 900_000) }
        assertEquals(homeRecommendationSources(tracks), homeRecommendationSources(updated))
        val second = loader.select(updated, 3, 2) { fail("Metadata-only update reprobed artwork"); null }
        assertEquals(first.tracks.map(MusicTrack::id), second.tracks.map(MusicTrack::id))
        assertTrue(second.tracks.all { it.title == "Updated ${it.id}" })
        assertEquals(count, probes.get())
    }

    @Test
    fun replacingOrRemovingOneFileDoesNotInvalidateOtherArtworkChecks() = runBlocking {
        val loader = HomeRecommendationLoader(42)
        val tracks = tracks(5)
        loader.select(tracks, 5, 2) { false }
        val changed = tracks.map { if (it.id == 3L) it.copy(dateModifiedEpochSeconds = 2L) else it }
        val checked = mutableListOf<Long>()
        val selected = loader.select(changed, 5, 1) { checked += it.id; true }
        assertEquals(listOf(3L), checked)
        assertEquals(setOf(3L), selected.artworkTrackIds)
        val removed = loader.select(changed.filterNot { it.id == 3L }, 5, 1) {
            fail("Unchanged remaining file was reprobed"); null
        }
        assertFalse(removed.tracks.any { it.id == 3L })
        assertTrue(removed.artworkTrackIds.isEmpty())
    }

    @Test
    fun fileSizeAndUriChangesInvalidateArtworkEvenWithTheSameTrackId() = runBlocking {
        val loader = HomeRecommendationLoader(42)
        var current = tracks(1).single()
        loader.select(listOf(current), 1, 1) { true }
        current = current.copy(fileSizeBytes = current.fileSizeBytes + 1)
        var probes = 0
        assertTrue(loader.select(listOf(current), 1, 1) { probes++; false }.artworkTrackIds.isEmpty())
        current = current.copy(contentUri = "content://media/replaced")
        assertEquals(setOf(current.id), loader.select(listOf(current), 1, 1) { probes++; true }.artworkTrackIds)
        assertEquals(2, probes)
    }

    @Test
    fun expansionKeepsThePreparedPrefixWithoutRepeatingConfirmedChecks() = runBlocking {
        val loader = HomeRecommendationLoader(42)
        val probes = ConcurrentHashMap<Long, AtomicInteger>()
        val first = loader.select(tracks(8), 2, 2) {
            probes.computeIfAbsent(it.id) { AtomicInteger() }.incrementAndGet(); true
        }
        val expanded = loader.select(tracks(8), 6, 2) {
            probes.computeIfAbsent(it.id) { AtomicInteger() }.incrementAndGet(); true
        }
        assertEquals(first.tracks, expanded.tracks.take(2))
        assertEquals(6, probes.size)
        assertTrue(probes.values.all { it.get() == 1 })
    }

    @Test
    fun transientProbeFailuresAreRetriedInsteadOfBecomingPermanentMissingCovers() = runBlocking {
        val loader = HomeRecommendationLoader(42)
        assertTrue(loader.select(tracks(1), 1, 1) { null }.artworkTrackIds.isEmpty())
        assertEquals(setOf(1L), loader.select(tracks(1), 1, 1) { true }.artworkTrackIds)
    }

    @Test
    fun aCancelledProbeCannotCacheItsLateAnswer() = runBlocking {
        val loader = HomeRecommendationLoader(42)
        val entered = CompletableDeferred<Unit>()
        val answer = CompletableDeferred<Boolean>()
        val old = async {
            loader.select(tracks(1), 1, 1) {
                withContext(NonCancellable) { entered.complete(Unit); answer.await() }
            }
        }
        entered.await()
        old.cancel()
        answer.complete(true)
        old.join()
        var probes = 0
        val current = loader.select(tracks(1), 1, 1) { probes++; false }
        assertEquals(1, probes)
        assertTrue(current.artworkTrackIds.isEmpty())
    }

    @Test
    fun startupSelectsThirtyCardsAndKeepsThemThroughHomeEntryAndLibraryChanges() = runBlocking {
        val library = mutableStateOf(emptyList<MusicTrack>())
        val active = mutableStateOf(false)
        val firstResources = CompletableDeferred<Unit>()
        val preparing = Channel<Long>(Channel.UNLIMITED)
        val probes = ConcurrentHashMap<Long, AtomicInteger>()
        var rendered: HomeRecommendations? = null
        withArtworkComposition(content = {
            val value = rememberHomeRecommendationSelection(
                tracks = library.value, active = active.value, initialRecommendationCount = 4,
                preparationKey = "blur-on", probeArtwork = {
                    probes.computeIfAbsent(it.id) { AtomicInteger() }.incrementAndGet(); true
                }, prepareTrack = { track, hasArtwork ->
                    assertTrue(hasArtwork)
                    preparing.send(track.id)
                    firstResources.await()
                },
            )
            SideEffect { rendered = value }
        }) {
            frame(0)
            library.value = tracks(40)
            frame(16)
            withTimeout(5_000) { repeat(2) { preparing.receive() } }
            assertNull(rendered)
            firstResources.complete(Unit)
            until { rendered?.tracks?.size == 30 }
            val prefix = rendered!!.tracks
            repeat(3) { frame(64L + it * 16) }
            assertEquals(32, probes.size)
            active.value = true
            library.value = tracks(60).reversed()
            repeat(6) { frame(128L + it * 16) }
            assertEquals(prefix, rendered!!.tracks)
            assertEquals(32, probes.size)
            assertTrue(probes.values.all { it.get() == 1 })
        }
    }

    @Test
    fun aSmallLibraryStopsAtItsActualSize() = runBlocking {
        var rendered: HomeRecommendations? = null
        withArtworkComposition(content = {
            val value = rememberHomeRecommendationSelection(
                tracks(12), active = false, initialRecommendationCount = 8,
                preparationKey = Unit, probeArtwork = { true }, prepareTrack = { _, _ -> },
            )
            SideEffect { rendered = value }
        }) {
            until { rendered?.tracks?.size == 12 }
            repeat(3) { frame(100L + it * 16) }
            assertEquals(12, rendered!!.tracks.size)
        }
    }

    @Test
    fun scanMetadataUpdatesDoNotCancelInProgressPreparationOrRestartCompletedWork() = runBlocking {
        val library = mutableStateOf(tracks(2))
        val resources = CompletableDeferred<Unit>()
        val started = Channel<Unit>(Channel.UNLIMITED)
        val preparations = AtomicInteger()
        var rendered: HomeRecommendations? = null
        withArtworkComposition(content = {
            val value = rememberHomeRecommendationSelection(
                library.value, active = false, initialRecommendationCount = 4,
                preparationKey = Unit, probeArtwork = { true }, prepareTrack = { _, _ ->
                    preparations.incrementAndGet(); started.send(Unit); resources.await()
                },
            )
            SideEffect { rendered = value }
        }) {
            frame(0)
            withTimeout(5_000) { repeat(2) { started.receive() } }
            library.value = library.value.reversed().map { it.copy(title = "New ${it.id}") }
            frame(16)
            resources.complete(Unit)
            until { rendered?.tracks?.size == 2 }
            assertTrue(rendered!!.tracks.all { it.title == "New ${it.id}" })
            library.value = library.value.map { it.copy(artist = "Updated artist") }
            frame(64)
            assertEquals(2, preparations.get())
            assertTrue(rendered!!.tracks.all { it.artist == "Updated artist" })
        }
    }

    @Test
    fun appearanceChangesReprepareTheSameSongsWithoutRepeatingSelectionProbes() = runBlocking {
        val appearance = mutableStateOf("light-blur")
        val prepared = ConcurrentHashMap.newKeySet<String>()
        val probes = AtomicInteger()
        var rendered: HomeRecommendations? = null
        withArtworkComposition(content = {
            val requestedAppearance = appearance.value
            val value = rememberHomeRecommendationSelection(
                tracks(2), active = false, initialRecommendationCount = 4,
                preparationKey = requestedAppearance, probeArtwork = { probes.incrementAndGet(); false },
                prepareTrack = { track, hasArtwork ->
                    assertFalse(hasArtwork)
                    prepared += "$requestedAppearance-${track.id}"
                },
            )
            SideEffect { rendered = value }
        }) {
            until { rendered?.tracks?.size == 2 }
            val ids = rendered!!.tracks.map(MusicTrack::id)
            appearance.value = "dark-no-blur"
            until { prepared.size == 4 }
            assertEquals(ids, rendered!!.tracks.map(MusicTrack::id))
            assertEquals(2, probes.get())
        }
    }

    @Test
    fun aLateObsoletePreparationCannotPublishRemovedSongs() = runBlocking {
        val library = mutableStateOf(tracks(1))
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val published = mutableListOf<List<Long>>()
        var rendered: HomeRecommendations? = null
        withArtworkComposition(content = {
            val value = rememberHomeRecommendationSelection(
                library.value, active = false, initialRecommendationCount = 4,
                preparationKey = Unit, probeArtwork = { true }, prepareTrack = { track, _ ->
                    if (track.id == 1L) withContext(NonCancellable) { entered.complete(Unit); release.await() }
                },
            )
            SideEffect {
                rendered = value
                if (value != null) published += value.tracks.map(MusicTrack::id)
            }
        }) {
            frame(0)
            withTimeout(5_000) { entered.await() }
            library.value = listOf(tracks(2).last())
            frame(16)
            release.complete(Unit)
            until { rendered?.tracks?.map(MusicTrack::id) == listOf(2L) }
            assertTrue(published.none { 1L in it })
        }
    }

    private suspend fun ArtworkTestComposition.until(condition: () -> Boolean) {
        withTimeout(5_000) {
            var time = 0L
            while (!condition()) { frame(time); time += 16 }
        }
    }

    private fun tracks(count: Int): List<MusicTrack> = (1L..count.toLong()).map { id ->
        MusicTrack(
            id = id, title = "Song $id", artist = null, album = null,
            durationMs = 100_000, dateAddedEpochSeconds = 1, dateModifiedEpochSeconds = 1,
            fileName = "$id.flac", fileSizeBytes = 100, contentUri = "content://media/audio/$id",
            titleSectionKey = "S", titleSortKey = "song $id",
        )
    }
}
