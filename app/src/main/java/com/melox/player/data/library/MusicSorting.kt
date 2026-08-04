package com.melox.player.data.library

import com.melox.player.model.MusicTrack
import java.util.Locale

enum class MusicSortField {
    TITLE,
    DATE_ADDED,
    FILE_NAME,
    FILE_SIZE,
    DURATION,
}

data class MusicSortConfig(
    val field: MusicSortField = MusicSortField.TITLE,
    val descending: Boolean = false,
)

internal fun filterMusicTracks(
    tracks: List<MusicTrack>,
    query: String,
): List<MusicTrack> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return tracks
    return tracks.filter { track ->
        sequenceOf(track.title, track.artist, track.album, track.fileName)
            .filterNotNull()
            .any { value -> value.contains(normalizedQuery, ignoreCase = true) }
    }
}

internal fun sortMusicTracks(
    tracks: List<MusicTrack>,
    config: MusicSortConfig,
): List<MusicTrack> {
    val comparator = when (config.field) {
        MusicSortField.TITLE -> compareBy<MusicTrack>(MusicTrack::titleSortKey)
            .thenBy(MusicTrack::id)

        MusicSortField.DATE_ADDED -> compareBy<MusicTrack>(MusicTrack::dateAddedEpochSeconds)
            .thenBy(MusicTrack::titleSortKey)
            .thenBy(MusicTrack::id)

        MusicSortField.FILE_NAME -> compareBy<MusicTrack> {
            it.fileName?.uppercase(Locale.ROOT).orEmpty()
        }
            .thenBy(MusicTrack::titleSortKey)
            .thenBy(MusicTrack::id)

        MusicSortField.FILE_SIZE -> compareBy<MusicTrack>(MusicTrack::fileSizeBytes)
            .thenBy(MusicTrack::titleSortKey)
            .thenBy(MusicTrack::id)

        MusicSortField.DURATION -> compareBy<MusicTrack>(MusicTrack::durationMs)
            .thenBy(MusicTrack::titleSortKey)
            .thenBy(MusicTrack::id)
    }

    val effectiveComparator = if (config.descending) {
        Comparator<MusicTrack> { first, second -> comparator.compare(second, first) }
    } else {
        comparator
    }
    return tracks.sortedWith(effectiveComparator)
}
