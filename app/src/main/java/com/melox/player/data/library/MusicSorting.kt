package com.melox.player.data.library

import com.melox.player.model.MusicTrack
import java.util.Locale

enum class MusicSortField {
    TITLE,
    DATE_ADDED,
    FILE_NAME,
    FILE_SIZE,
    DURATION,
    // Keep this appended so persisted ordinal values from older versions remain stable.
    ARTIST,
}

data class MusicSortConfig(
    val field: MusicSortField = MusicSortField.TITLE,
    val descending: Boolean = false,
)

internal fun MusicTrack.songListTitle(showFileName: Boolean): String? =
    if (showFileName) fileName?.takeIf { it.isNotBlank() } ?: title else title

data class SongSearchFields(
    val showFileName: Boolean = false,
    val unknownTitle: String? = null,
) {
    internal fun matches(track: MusicTrack, query: String): Boolean {
        val title = track.songListTitle(showFileName) ?: unknownTitle
        return title?.contains(query, ignoreCase = true) == true ||
            track.artist?.contains(query, ignoreCase = true) == true ||
            track.album?.contains(query, ignoreCase = true) == true
    }
}

internal fun filterMusicTracks(
    tracks: List<MusicTrack>,
    query: String,
    fields: SongSearchFields = SongSearchFields(),
): List<MusicTrack> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return tracks
    return tracks.filter { fields.matches(it, normalizedQuery) }
}

internal fun sortMusicTracks(
    tracks: List<MusicTrack>,
    config: MusicSortConfig,
): List<MusicTrack> = tracks.sortedWith(musicTrackComparator(config))

internal fun musicTrackComparator(config: MusicSortConfig): Comparator<MusicTrack> {
    val comparator = when (config.field) {
        MusicSortField.TITLE -> compareBy<MusicTrack>(MusicTrack::titleSortKey)
            .thenBy(MusicTrack::id)

        MusicSortField.ARTIST -> {
            val keys = HashMap<String?, String>()
            compareBy<MusicTrack> { track ->
                keys.getOrPut(track.artist) { createMusicSortKeys(track.artist).value }
            }
                .thenBy(MusicTrack::titleSortKey)
                .thenBy(MusicTrack::id)
        }

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

    return if (config.descending) {
        Comparator<MusicTrack> { first, second -> comparator.compare(second, first) }
    } else {
        comparator
    }
}
