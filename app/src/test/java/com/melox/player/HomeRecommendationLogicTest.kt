package com.melox.player

import com.melox.player.model.MusicTrack
import com.melox.player.model.PlaybackMode
import com.melox.player.model.PlaybackQueueItem
import com.melox.player.playback.buildHomeRecommendationPlaybackQueue
import com.melox.player.playback.reorderQueueForPlaybackMode
import com.melox.player.ui.screen.home.selectHomeRecommendations
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRecommendationLogicTest {
    @Test
    fun homeRecommendationsFillMissingArtworkOnlyAfterArtworkTracksAreExhausted() = runBlocking {
        val tracks = (1L..8L).map { id ->
            musicTrack(id, "Track $id", id, "$id.mp3", id, id)
        }
        val artworkTrackIds = setOf(2L, 4L, 7L)

        val recommendations = selectHomeRecommendations(
            tracks = tracks,
            seed = 42,
            count = 5,
        ) { track ->
            track.id in artworkTrackIds
        }
        val repeatedSelection = selectHomeRecommendations(
            tracks = tracks,
            seed = 42,
            count = 5,
        ) { track ->
            track.id in artworkTrackIds
        }

        assertEquals(artworkTrackIds, recommendations.tracks.take(3).map(MusicTrack::id).toSet())
        assertEquals(artworkTrackIds, recommendations.artworkTrackIds)
        assertEquals(5, recommendations.tracks.size)
        assertTrue(recommendations.tracks.drop(3).none { it.id in artworkTrackIds })
        assertEquals(recommendations, repeatedSelection)
    }

    @Test
    fun homeRecommendationPriorityPassPublishesTwoAndReusesTheirArtwork() = runBlocking {
        val tracks = (1L..8L).map { id ->
            musicTrack(id, "Track $id", id, "$id.mp3", id, id)
        }
        val priorityProbeIds = mutableListOf<Long>()
        val priorityRecommendations = selectHomeRecommendations(
            tracks = tracks,
            seed = 42,
            count = 2,
            probeBatchSize = 2,
        ) { track ->
            priorityProbeIds += track.id
            true
        }
        val expansionProbeIds = mutableListOf<Long>()
        val expandedRecommendations = selectHomeRecommendations(
            tracks = tracks,
            seed = 42,
            count = 5,
            knownArtworkTrackIds = priorityRecommendations.artworkTrackIds,
        ) { track ->
            expansionProbeIds += track.id
            true
        }

        assertEquals(2, priorityProbeIds.size)
        assertEquals(priorityRecommendations.tracks, expandedRecommendations.tracks.take(2))
        assertTrue(expansionProbeIds.none { it in priorityRecommendations.artworkTrackIds })
    }

    @Test
    fun homeRecommendationsSkipMissingArtworkWhileCoveredCandidatesRemain() = runBlocking {
        val tracks = (1L..8L).map { id -> musicTrack(id, "Track $id", id, "$id.mp3", id, id) }
        val coveredIds = tracks.shuffled(Random(42)).takeLast(2).map(MusicTrack::id).toSet()
        val probedIds = mutableSetOf<Long>()
        val selected = selectHomeRecommendations(tracks, seed = 42, count = 2, probeBatchSize = 1) {
            probedIds += it.id
            it.id in coveredIds
        }
        assertEquals(coveredIds, selected.tracks.map(MusicTrack::id).toSet())
        assertEquals(8, probedIds.size)
        assertEquals(coveredIds, selected.artworkTrackIds)
    }

    @Test
    fun homeRecommendationsShowArtworkFreeLibrariesInStableSeededOrder() = runBlocking {
        val tracks = (1L..5L).map { id -> musicTrack(id, "Track $id", id, "$id.mp3", id, id) }
        val probedIds = mutableSetOf<Long>()
        val selected = selectHomeRecommendations(tracks, seed = 42, count = 3, probeBatchSize = 1) {
            probedIds += it.id
            false
        }
        assertEquals(tracks.shuffled(Random(42)).take(3), selected.tracks)
        assertTrue(selected.artworkTrackIds.isEmpty())
        assertEquals(5, probedIds.size)
        val expanded = selectHomeRecommendations(
            tracks, seed = 42, count = 10, knownArtworkTrackIds = selected.artworkTrackIds,
        ) { false }
        assertEquals(selected.tracks, expanded.tracks.take(3))
        assertEquals(5, expanded.tracks.size)
        assertEquals(5, expanded.tracks.map(MusicTrack::id).distinct().size)
    }

    @Test
    fun homeRecommendationExpansionNeverPromotesPlaceholderSelectionsToArtworkHits() = runBlocking {
        val tracks = (1L..5L).map { id -> musicTrack(id, "Track $id", id, "$id.mp3", id, id) }
        val coveredId = tracks.shuffled(Random(42)).last().id
        val initial = selectHomeRecommendations(tracks, seed = 42, count = 2) { it.id == coveredId }
        assertEquals(setOf(coveredId), initial.artworkTrackIds)
        val probedIds = mutableSetOf<Long>()
        val expanded = selectHomeRecommendations(
            tracks, seed = 42, count = 4, knownArtworkTrackIds = initial.artworkTrackIds,
            probeBatchSize = 1,
        ) {
            probedIds += it.id
            false
        }
        assertEquals(initial.tracks, expanded.tracks.take(2))
        assertEquals(coveredId, expanded.tracks.first().id)
        assertTrue(initial.tracks[1].id in probedIds)
        assertEquals(setOf(coveredId), expanded.artworkTrackIds)
    }

    @Test
    fun homeRecommendationQueueStartsWithSelectedTrackAndKeepsLoadedPrefixOrder() {
        val tracks = (1L..5L).map { id ->
            musicTrack(id, "Track $id", id, "$id.mp3", id, id)
        }

        val queue = buildHomeRecommendationPlaybackQueue(
            selectedTrackId = 2L,
            recommendations = listOf(tracks[2], tracks[0], tracks[1], tracks[2]),
            allTracks = tracks,
            playbackMode = PlaybackMode.ORDER,
        )

        assertEquals(listOf(2L, 3L, 1L, 4L, 5L), queue.map(MusicTrack::id))
    }

    @Test
    fun homeRecommendationQueueKeepsItsPrefixUntilPlaybackModeChanges() {
        val tracks = (1L..6L).map { id ->
            musicTrack(id, "Track $id", id, "$id.mp3", id, id)
        }
        val randomQueue = buildHomeRecommendationPlaybackQueue(
            selectedTrackId = 2L,
            recommendations = listOf(tracks[2], tracks[0], tracks[1]),
            allTracks = tracks,
            playbackMode = PlaybackMode.RANDOM,
            random = Random(17),
        )

        assertEquals(listOf(2L, 3L, 1L), randomQueue.take(3).map(MusicTrack::id))
        assertEquals(
            setOf(4L, 5L, 6L),
            randomQueue.drop(3).map(MusicTrack::id).toSet(),
        )

        val sourceOrderByTrackId = tracks.mapIndexed { index, track -> track.id to index.toDouble() }.toMap()
        val normalized = reorderQueueForPlaybackMode(
            queue = randomQueue.map { track ->
                playbackQueueItem(track.id.toString()).copy(
                    sourceOrder = sourceOrderByTrackId.getValue(track.id),
                )
            },
            currentIndex = 0,
            targetMode = PlaybackMode.REPEAT_ONE,
        )

        assertEquals(listOf("1", "2", "3", "4", "5", "6"), normalized.queue.map(PlaybackQueueItem::mediaId))
        assertEquals(1, normalized.currentIndex)
        assertEquals(
            setOf(PlaybackMode.REPEAT_ONE),
            normalized.queue.map(PlaybackQueueItem::playbackMode).toSet(),
        )
    }
}
