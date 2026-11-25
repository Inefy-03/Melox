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
    internal fun match(track: MusicTrack, query: String): SearchMatch = maxOf(
        searchMatch(track.songListTitle(showFileName) ?: unknownTitle, query),
        searchMatch(track.artist, query),
        searchMatch(track.album, query),
    )
}

internal fun filterMusicTracks(
    tracks: List<MusicTrack>,
    query: String,
    fields: SongSearchFields = SongSearchFields(),
): List<MusicTrack> = tracks.rankSearchResults(query, fields::match)

internal enum class SearchMatch {
    NONE,
    SUBSTRING,
    WORD_START,
    PREFIX,
    EXACT,
}

internal fun searchMatch(text: String?, query: String): SearchMatch {
    if (text == null) return SearchMatch.NONE
    var index = text.indexOf(query, ignoreCase = true)
    if (index < 0) return SearchMatch.NONE
    if (text.length == query.length) return SearchMatch.EXACT
    if (index == 0) return SearchMatch.PREFIX
    while (index > 0) {
        if (text[index].isUpperCase() || !text[index - 1].isLetterOrDigit()) {
            return SearchMatch.WORD_START
        }
        index = text.indexOf(query, startIndex = index + 1, ignoreCase = true)
    }
    return SearchMatch.SUBSTRING
}

internal inline fun <T> List<T>.rankSearchResults(
    query: String,
    match: (T, String) -> SearchMatch,
): List<T> {
    val keyword = query.trim()
    if (keyword.isEmpty()) return this
    val results = List(SearchMatch.entries.size) { mutableListOf<T>() }
    for (item in this) {
        val rank = match(item, keyword)
        if (rank != SearchMatch.NONE) results[rank.ordinal].add(item)
    }
    return buildList {
        for (rank in SearchMatch.EXACT.ordinal downTo SearchMatch.SUBSTRING.ordinal) {
            addAll(results[rank])
        }
    }
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
