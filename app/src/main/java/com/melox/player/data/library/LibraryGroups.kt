package com.melox.player.data.library

import com.melox.player.model.MusicTrack
import java.util.Locale

data class AlbumGroup(
    val key: String,
    val name: String?,
    val albumArtist: String?,
    val year: Int?,
    val tracks: List<MusicTrack>,
) {
    val coverTrack: MusicTrack?
        get() = tracks.firstOrNull()
}

data class ArtistGroup(
    val key: String,
    val name: String?,
    val tracks: List<MusicTrack>,
    val albumCount: Int,
) {
    val coverTrack: MusicTrack?
        get() = tracks.firstOrNull()
}

enum class AlbumSortField {
    ALBUM,
    ALBUM_ARTIST,
    SONG_COUNT,
    YEAR,
}

enum class AlbumGridStyle(val columns: Int) {
    TWO_SMALL(columns = 2),
    THREE(columns = 3),
}

data class AlbumSortConfig(
    val field: AlbumSortField = AlbumSortField.ALBUM,
    val descending: Boolean = false,
    val gridStyle: AlbumGridStyle = AlbumGridStyle.TWO_SMALL,
)

enum class ArtistSortField {
    NAME,
    SONG_COUNT,
    ALBUM_COUNT,
}

data class ArtistSortConfig(
    val field: ArtistSortField = ArtistSortField.NAME,
    val descending: Boolean = false,
)

internal fun buildAlbumGroups(tracks: List<MusicTrack>): List<AlbumGroup> =
    tracks
        .groupBy { track ->
            albumGroupKey(
                album = track.album,
                albumArtist = track.albumArtist,
            )
        }
        .map { (key, albumTracks) ->
            AlbumGroup(
                key = key,
                name = albumTracks.firstNotNullOfOrNull(MusicTrack::album),
                albumArtist = albumTracks.firstNotNullOfOrNull(MusicTrack::albumArtist),
                year = albumTracks.firstNotNullOfOrNull { it.year?.takeIf { value -> value > 0 } },
                tracks = albumTracks,
            )
        }

internal fun buildArtistGroups(tracks: List<MusicTrack>): List<ArtistGroup> =
    tracks
        .flatMap { track ->
            val artistNames = splitArtistNames(track.artist)
            if (artistNames.isEmpty()) {
                listOf(ArtistTrackEntry(key = artistGroupKey(null), name = null, track = track))
            } else {
                artistNames.map { name ->
                    ArtistTrackEntry(
                        key = artistGroupKey(name),
                        name = name,
                        track = track,
                    )
                }
            }
        }
        .groupBy(ArtistTrackEntry::key)
        .map { (key, artistEntries) ->
            val artistTracks = artistEntries
                .map(ArtistTrackEntry::track)
                .distinctBy(MusicTrack::id)
            ArtistGroup(
                key = key,
                name = artistEntries.firstNotNullOfOrNull { it.name },
                tracks = artistTracks,
                albumCount = artistTracks
                    .map { track ->
                        albumGroupKey(
                            album = track.album,
                            albumArtist = track.albumArtist,
                        )
                    }
                    .distinct()
                    .size,
            )
        }

internal fun filterAlbums(
    albums: List<AlbumGroup>,
    query: String,
): List<AlbumGroup> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return albums
    return albums.filter { album ->
        album.name?.contains(normalizedQuery, ignoreCase = true) == true
    }
}

internal fun sortAlbums(
    albums: List<AlbumGroup>,
    config: AlbumSortConfig,
): List<AlbumGroup> {
    val albumNameComparator = compareBy<AlbumGroup> {
        createMusicSortKeys(it.name).value
    }.thenBy(AlbumGroup::key)
    val comparator = when (config.field) {
        AlbumSortField.ALBUM -> albumNameComparator
        AlbumSortField.ALBUM_ARTIST -> compareBy<AlbumGroup> {
            createMusicSortKeys(it.albumArtist).value
        }.then(albumNameComparator)
        AlbumSortField.SONG_COUNT -> compareBy<AlbumGroup> { it.tracks.size }
            .then(albumNameComparator)
        AlbumSortField.YEAR -> compareBy<AlbumGroup> { it.year ?: 0 }
            .then(albumNameComparator)
    }
    return albums.sortedWith(comparator.withDirection(config.descending))
}

internal fun filterArtists(
    artists: List<ArtistGroup>,
    query: String,
): List<ArtistGroup> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return artists
    return artists.filter { artist ->
        artist.name?.contains(normalizedQuery, ignoreCase = true) == true
    }
}

internal fun sortArtists(
    artists: List<ArtistGroup>,
    config: ArtistSortConfig,
): List<ArtistGroup> {
    val nameComparator = compareBy<ArtistGroup> {
        createMusicSortKeys(it.name).value
    }.thenBy(ArtistGroup::key)
    val comparator = when (config.field) {
        ArtistSortField.NAME -> nameComparator
        ArtistSortField.SONG_COUNT -> compareBy<ArtistGroup> { it.tracks.size }
            .then(nameComparator)
        ArtistSortField.ALBUM_COUNT -> compareBy<ArtistGroup>(ArtistGroup::albumCount)
            .then(nameComparator)
    }
    return artists.sortedWith(comparator.withDirection(config.descending))
}

internal fun albumSectionKey(
    album: AlbumGroup,
    field: AlbumSortField,
): String = createMusicSortKeys(
    when (field) {
        AlbumSortField.ALBUM -> album.name
        AlbumSortField.ALBUM_ARTIST -> album.albumArtist
        AlbumSortField.SONG_COUNT,
        AlbumSortField.YEAR,
        -> null
    },
).section

internal fun artistSectionKey(artist: ArtistGroup): String =
    createMusicSortKeys(artist.name).section

internal fun albumGroupKey(
    album: String?,
    albumArtist: String?,
): String = "${album.normalizedGroupValue()}\u0000${albumArtist.normalizedGroupValue()}"

internal fun artistGroupKey(artist: String?): String = artist.normalizedGroupValue()

internal fun displayArtistName(artist: String?): String? =
    splitArtistNames(artist).takeIf { it.isNotEmpty() }?.joinToString(" / ")

private data class ArtistTrackEntry(
    val key: String,
    val name: String?,
    val track: MusicTrack,
)

private val ArtistNameSeparatorRegex = Regex("""[，,、/&]+""")
private val WhitespaceRegex = Regex("""\s+""")

internal fun splitArtistNames(artist: String?): List<String> = artist
    ?.split(ArtistNameSeparatorRegex)
    ?.map { it.trim().replace(WhitespaceRegex, " ") }
    ?.filter(String::isNotEmpty)
    ?.distinctBy { it.normalizedGroupValue() }
    .orEmpty()

private fun String?.normalizedGroupValue(): String = this
    ?.trim()
    ?.replace(WhitespaceRegex, " ")
    ?.lowercase(Locale.ROOT)
    .orEmpty()

private fun <T> Comparator<T>.withDirection(descending: Boolean): Comparator<T> =
    if (descending) Comparator { first, second -> compare(second, first) } else this
