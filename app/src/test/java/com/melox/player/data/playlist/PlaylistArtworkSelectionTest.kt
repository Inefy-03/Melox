package com.melox.player.data.playlist

import com.melox.player.model.LocalPlaylist
import com.melox.player.model.MusicTrack
import com.melox.player.model.PlaylistTrackEntry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistArtworkSelectionTest {
    @Test
    fun aCustomCoverReplacesTheEntireCollageAndWorksForAnEmptyPlaylist() = runBlocking {
        for (tracks in listOf(emptyList(), (1L..4L).map { track(it) })) {
            val result = loadPlaylistArtwork(
                playlist = playlist(tracks),
                loadArtwork = { error("A custom cover should skip track artwork loading") },
                sameArtwork = String::equals,
                loadCustomArtwork = { "custom" },
            )
            assertEquals(listOf("custom"), result)
        }
    }

    @Test
    fun missingImagesAreReplacedByLaterImagesInCustomOrder() = runBlocking {
        val requested = mutableListOf<Long>()
        val result = select(
            tracks = (1L..6L).map { track(it) },
            images = mapOf(2L to "red", 4L to "blue", 5L to "green", 6L to "yellow"),
            requested = requested,
        )
        assertEquals(listOf("red", "blue", "green"), result)
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), requested)
    }

    @Test
    fun aRepresentedAlbumIsSkippedEvenWhenItsNextTrackHasDifferentArtwork() = runBlocking {
        val requested = mutableListOf<Long>()
        val result = select(
            tracks = listOf(track(1, "A"), track(2, "A"), track(3, "B"), track(4, "C")),
            images = mapOf(1L to "red", 2L to "yellow", 3L to "blue", 4L to "green"),
            requested = requested,
        )
        assertEquals(listOf("red", "blue", "green"), result)
        assertEquals(listOf(1L, 3L, 4L), requested)
    }

    @Test
    fun aMissingImageDoesNotReserveItsAlbum() = runBlocking {
        assertEquals(
            listOf("red", "blue", "green"),
            select(
                listOf(track(1, "A"), track(2, "A"), track(3, "B"), track(4, "C")),
                mapOf(2L to "red", 3L to "blue", 4L to "green"),
            ),
        )
    }

    @Test
    fun identicalImagesFromDifferentAlbumsAreReplacedByLaterDistinctImages() = runBlocking {
        assertEquals(
            listOf("red", "blue", "green"),
            select(
                listOf(track(1, "A"), track(2, "B"), track(3, "C"), track(4, "D")),
                mapOf(1L to "red", 2L to "red", 3L to "blue", 4L to "green"),
            ),
        )
    }

    @Test
    fun aDuplicateImageDoesNotHideALaterDistinctImageFromTheSameAlbum() = runBlocking {
        assertEquals(
            listOf("red", "blue", "green"),
            select(
                listOf(track(1, "A"), track(2, "B"), track(3, "B"), track(4, "C")),
                mapOf(1L to "red", 2L to "red", 3L to "blue", 4L to "green"),
            ),
        )
    }

    @Test
    fun missingAlbumNamesDoNotGroupUnrelatedImagesTogether() = runBlocking {
        assertEquals(
            listOf("red", "blue", "green"),
            select(
                listOf(track(1), track(2, ""), track(3, "  ")),
                mapOf(1L to "red", 2L to "blue", 3L to "green"),
            ),
        )
    }

    @Test
    fun equalImagesAreStillDeduplicatedWhenAlbumNamesAreMissing() = runBlocking {
        assertEquals(
            listOf("red", "blue"),
            select(
                listOf(track(1), track(2), track(3)),
                mapOf(1L to "red", 2L to "red", 3L to "blue"),
            ),
        )
    }

    @Test
    fun albumIdentityMatchesNormalizedLibraryAlbumAndAlbumArtistGrouping() = runBlocking {
        val requested = mutableListOf<Long>()
        assertEquals(
            listOf("red", "blue", "green"),
            select(
                listOf(
                    track(1, "  Album   One ", " Band   One "),
                    track(2, "album one", "band one"),
                    track(3, "Album One", "Another Band"),
                    track(4, "Other Album", "Band One"),
                ),
                mapOf(1L to "red", 2L to "yellow", 3L to "blue", 4L to "green"),
                requested,
            ),
        )
        assertEquals(listOf(1L, 3L, 4L), requested)
    }

    @Test
    fun fewerThanThreeUsableDistinctImagesAreReturnedWithoutSyntheticTiles() = runBlocking {
        assertEquals(
            listOf("red"),
            select(
                listOf(track(1, "A"), track(2, "A"), track(3, "B"), track(4, "C")),
                mapOf(1L to "red", 2L to "blue", 3L to "red"),
            ),
        )
    }

    @Test
    fun emptyAndAllMissingPlaylistsProduceNoImage() = runBlocking {
        val requested = mutableListOf<Long>()
        assertTrue(select(emptyList(), emptyMap(), requested).isEmpty())
        assertTrue(requested.isEmpty())
        assertTrue(select(listOf(track(1), track(2)), emptyMap(), requested).isEmpty())
        assertEquals(listOf(1L, 2L), requested)
    }

    @Test
    fun pendingSelectionKeepsItsOriginalEntrySnapshot() = runBlocking {
        val entries = playlist((1L..4L).map { track(it) }).entries.toMutableList()
        val pending = playlist(emptyList()).copy(entries = entries)
        val resume = CompletableDeferred<Unit>()
        var result: List<Long>? = null
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            result = loadPlaylistArtwork(
                pending,
                loadArtwork = {
                    if (it.id == 1L) resume.await()
                    it.id
                },
                sameArtwork = { first, second -> first == second },
            )
        }
        entries.clear()
        resume.complete(Unit)
        job.join()
        assertEquals(listOf(1L, 2L, 3L), result)
    }

    @Test
    fun cancelledSelectionDoesNotPublishAfterALateNonCancellableLoad() = runBlocking {
        val resume = CompletableDeferred<Unit>()
        val requested = mutableListOf<Long>()
        var published: List<Long>? = null
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            published = loadPlaylistArtwork(
                playlist((1L..4L).map { track(it) }),
                loadArtwork = {
                    requested += it.id
                    withContext(NonCancellable) { resume.await() }
                    it.id
                },
                sameArtwork = { first, second -> first == second },
            )
        }
        job.cancel()
        resume.complete(Unit)
        job.join()
        assertTrue(job.isCancelled)
        assertNull(published)
        assertEquals(listOf(1L), requested)
    }

    private suspend fun select(
        tracks: List<MusicTrack>,
        images: Map<Long, String>,
        requested: MutableList<Long> = mutableListOf(),
    ): List<String> = loadPlaylistArtwork(
        playlist(tracks),
        loadArtwork = {
            requested += it.id
            images[it.id]
        },
        sameArtwork = { first, second -> first == second },
    )

    private fun playlist(tracks: List<MusicTrack>) = LocalPlaylist(
        id = "playlist",
        name = "Playlist",
        createdAtEpochMillis = 0,
        updatedAtEpochMillis = 0,
        entries = tracks.map { PlaylistTrackEntry("entry-${it.id}", 0, it) },
    )

    private fun track(id: Long, album: String? = null, albumArtist: String? = null) = MusicTrack(
        id = id,
        title = "Track $id",
        artist = "Artist",
        album = album,
        albumArtist = albumArtist,
        durationMs = 60_000,
        dateAddedEpochSeconds = 0,
        dateModifiedEpochSeconds = 0,
        fileName = "$id.mp3",
        fileSizeBytes = 1,
        contentUri = "content://music/$id",
        titleSectionKey = "T",
        titleSortKey = "track $id",
    )
}
