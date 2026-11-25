package com.melox.player

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.layout.PaddingValues
import com.melox.player.ui.component.library.gridViewportWidth
import com.melox.player.data.library.AlbumGridStyle
import com.melox.player.data.library.AlbumSortConfig
import com.melox.player.data.library.AlbumSortField
import com.melox.player.data.library.ArtistSortConfig
import com.melox.player.data.library.ArtistSortField
import com.melox.player.data.library.FolderSortConfig
import com.melox.player.data.library.FolderSortField
import com.melox.player.data.library.LocalAudioProperties
import com.melox.player.data.library.MusicLibrarySnapshotCodec
import com.melox.player.data.library.MusicSortConfig
import com.melox.player.data.library.MusicSortField
import com.melox.player.data.library.buildAlbumDiscSections
import com.melox.player.data.library.buildAlbumGroups
import com.melox.player.data.library.buildArtistGroups
import com.melox.player.data.library.buildFolderGroups
import com.melox.player.data.library.createMusicSortKeys
import com.melox.player.data.library.displayArtistName
import com.melox.player.data.library.filterAlbums
import com.melox.player.data.library.filterArtists
import com.melox.player.data.library.SongSearchFields
import com.melox.player.data.library.filterMusicTracks
import com.melox.player.data.library.folderDisplayPath
import com.melox.player.data.library.hasReusableAudioProperties
import com.melox.player.data.library.japaneseKanaToRomaji
import com.melox.player.data.library.normalizeAudioProperties
import com.melox.player.data.library.normalizeMusicFolderPath
import com.melox.player.data.library.parseAudioTagProperties
import com.melox.player.data.library.sortAlbums
import com.melox.player.data.library.sortArtists
import com.melox.player.data.library.sortFolders
import com.melox.player.data.library.sortMusicTracks
import com.melox.player.data.library.splitArtistNames
import com.melox.player.data.repository.resolveAlbumGridStyleOrdinal
import com.melox.player.model.AppSettings
import com.melox.player.model.AudioQuality
import com.melox.player.model.MusicTrack
import com.melox.player.model.ScanStatus
import com.melox.player.model.resolveAudioQuality
import com.melox.player.ui.component.library.AlphabetSections
import com.melox.player.ui.component.library.audioFormatLabel
import com.melox.player.ui.component.library.displayFileLocation
import com.melox.player.ui.component.library.findAlphabetTargetIndex
import com.melox.player.ui.component.library.alphabetItemIndexAt
import com.melox.player.ui.component.library.alphabetSectionRanges
import com.melox.player.ui.component.library.formatDuration
import com.melox.player.ui.component.library.participatingArtistGroups
import com.melox.player.ui.component.library.responsiveGridColumnCount
import com.melox.player.ui.component.playlist.PlaylistArtworkLayout
import com.melox.player.ui.component.playlist.playlistArtworkLayout
import com.melox.player.ui.screen.home.homeInitialRecommendationCount
import com.melox.player.ui.component.library.playlistGridColumnCount
import com.melox.player.ui.screen.library.MusicLibraryPlaceholder
import com.melox.player.ui.screen.library.albumDetailHeaderCoverSize
import com.melox.player.ui.screen.library.albumGridColumnCount
import com.melox.player.ui.screen.library.resolveMusicPlaybackSelection
import com.melox.player.ui.screen.library.toMusicLibraryPlaceholder
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.zip.CRC32
import java.util.zip.CheckedOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryLogicTest {
    @Test
    fun playlistGridUsesViewportAfterSidebarAndSafeInsets() {
        val padding = PaddingValues(start = 240.dp, end = 12.dp)
        for (direction in LayoutDirection.entries) {
            val viewport = padding.gridViewportWidth(840.dp, direction)
            assertEquals(588.dp, viewport)
            assertEquals(3, playlistGridColumnCount(landscape = true, availableWidth = viewport))
        }
    }

    @Test
    fun libraryPreviewColumnsUse360DpWidthWithinFiveToSevenColumns() {
        val cases = mapOf(320 to 5, 1879 to 5, 1880 to 5, 2251 to 5, 2252 to 6, 2623 to 6, 2624 to 7, 3000 to 7)
        cases.forEach { (width, columns) ->
            assertEquals(columns, com.melox.player.ui.screen.library.libraryPreviewColumnCount(true, width.dp))
        }
        assertEquals(3, com.melox.player.ui.screen.library.libraryPreviewColumnCount(false, 1200.dp))
    }

    @Test
    fun libraryOverviewRowsRespectOrientationLimitsAndReadingOrder() {
        assertEquals(
            List(5) { column -> List(5) { row -> row * 5 + column } },
            com.melox.player.ui.screen.library.libraryPreviewColumns(40, true, 1000.dp),
        )
        assertEquals(emptyList<List<Int>>(), com.melox.player.ui.screen.library.libraryPreviewColumns(0, true, 1028.dp))
        assertEquals(listOf(listOf(0)), com.melox.player.ui.screen.library.libraryPreviewColumns(1, true, 1028.dp))
        assertEquals(
            listOf(listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5)),
            com.melox.player.ui.screen.library.libraryPreviewColumns(8, false, 400.dp),
        )
        assertEquals(
            listOf(listOf(0, 3, 6, 9), listOf(1, 4, 7, 10), listOf(2, 5, 8, 11)),
            com.melox.player.ui.screen.library.libraryPreviewColumns(20, false, 400.dp),
        )
        assertEquals(
            List(7) { column -> List(5) { row -> row * 7 + column } },
            com.melox.player.ui.screen.library.libraryPreviewColumns(40, true, 2700.dp),
        )
    }

    @Test
    fun durationFormattingSupportsHoursAndInvalidValues() {
        assertEquals("0:00", formatDuration(-1L))
        assertEquals("3:05", formatDuration(185_000L))
        assertEquals("1:02:03", formatDuration(3_723_000L))
    }

    @Test
    fun titleSortKeysCoverDigitsAsciiPinyinAndFallback() {
        assertEquals("0", createMusicSortKeys("1989").section)
        assertEquals("A", createMusicSortKeys("afterglow").section)
        assertEquals("Z", createMusicSortKeys("周杰伦").section)
        assertEquals("#", createMusicSortKeys("♪ intro").section)
        assertEquals("#", createMusicSortKeys(null).section)
    }

    @Test
    fun japaneseKanaUsesRomajiForTitleSortingAndSections() {
        assertEquals("sakura", japaneseKanaToRomaji("さくら"))
        assertEquals("katakana", japaneseKanaToRomaji("カタカナ"))
        assertEquals("ccha", japaneseKanaToRomaji("っちゃ"))
        assertEquals("S", createMusicSortKeys("さくら").section)
        assertEquals("1_SAKURA", createMusicSortKeys("さくら").value)
        assertEquals("T", createMusicSortKeys("とうきょう").section)
    }

    @Test
    fun compressedAlphabetKeepsEndpointsAndDistributesVisibleLetters() {
        val count = AlphabetSections.size
        for (slots in 0..count) {
            val ranges = alphabetSectionRanges(count, slots)
            assertEquals((1..count).toList(), ranges.flatMap { it.toList() })
            assertEquals(listOf(1..1, 2..2), ranges.take(2))
            assertEquals(listOf((count - 1)..(count - 1), count..count), ranges.takeLast(2))
            assertTrue(ranges.size <= slots.coerceAtLeast(5))
        }
        assertEquals(
            listOf(1..1, 2..2, 3..14, 15..15, 16..26, 27..27, 28..28),
            alphabetSectionRanges(count, slots = 7),
        )
    }

    @Test
    fun dragAcrossCompressedAlphabetReachesEveryHiddenLetter() {
        val ranges = listOf(0..0) + alphabetSectionRanges(AlphabetSections.size, slots = 5)
        val height = 90
        assertEquals(0, alphabetItemIndexAt(-1f, height, ranges))
        assertEquals(1, alphabetItemIndexAt(15f, height, ranges))
        assertEquals(2, alphabetItemIndexAt(30f, height, ranges))
        for (offset in 0..23) {
            val y = 45f + (offset + 0.5f) * (15f / 24)
            assertEquals(3 + offset, alphabetItemIndexAt(y, height, ranges))
        }
        assertEquals(27, alphabetItemIndexAt(60f, height, ranges))
        assertEquals(28, alphabetItemIndexAt(90f, height, ranges))
    }

    @Test
    fun missingAlphabetSectionsResolveInDisplayOrder() {
        val sectionIndexMap = mapOf(
            "0" to 0,
            "B" to 3,
            "M" to 8,
            "#" to 12,
        )

        assertEquals(3, findAlphabetTargetIndex("A", sectionIndexMap))
        assertEquals(8, findAlphabetTargetIndex("C", sectionIndexMap))
        assertEquals(12, findAlphabetTargetIndex("Z", sectionIndexMap))
        assertEquals(12, findAlphabetTargetIndex("#", sectionIndexMap))
        assertEquals(0, findAlphabetTargetIndex("?", sectionIndexMap))
    }

    @Test
    fun missingAlphabetSectionsResolveInDescendingDisplayOrder() {
        val sectionIndexMap = mapOf(
            "#" to 0,
            "M" to 3,
            "B" to 8,
            "0" to 12,
        )
        val descendingSections = AlphabetSections.asReversed()

        assertEquals(3, findAlphabetTargetIndex("Z", sectionIndexMap, descendingSections))
        assertEquals(8, findAlphabetTargetIndex("C", sectionIndexMap, descendingSections))
        assertEquals(12, findAlphabetTargetIndex("A", sectionIndexMap, descendingSections))
    }

    @Test
    fun musicSortingCoversEveryFieldAndDescendingOrder() {
        val tracks = listOf(
            musicTrack(
                id = 1L,
                title = "Bravo",
                artist = "Zed",
                dateAddedEpochSeconds = 30L,
                fileName = "c.mp3",
                fileSizeBytes = 300L,
                durationMs = 200L,
            ),
            musicTrack(
                id = 2L,
                title = "Alpha",
                artist = "Alpha",
                dateAddedEpochSeconds = 10L,
                fileName = "b.mp3",
                fileSizeBytes = 100L,
                durationMs = 300L,
            ),
            musicTrack(
                id = 3L,
                title = "Charlie",
                artist = "Mia",
                dateAddedEpochSeconds = 20L,
                fileName = "a.mp3",
                fileSizeBytes = 200L,
                durationMs = 100L,
            ),
        )

        assertEquals(listOf(2L, 1L, 3L), tracks.sortedIds(MusicSortField.TITLE))
        assertEquals(listOf(2L, 3L, 1L), tracks.sortedIds(MusicSortField.ARTIST))
        assertEquals(listOf(2L, 3L, 1L), tracks.sortedIds(MusicSortField.DATE_ADDED))
        assertEquals(listOf(3L, 2L, 1L), tracks.sortedIds(MusicSortField.FILE_NAME))
        assertEquals(listOf(2L, 3L, 1L), tracks.sortedIds(MusicSortField.FILE_SIZE))
        assertEquals(listOf(3L, 1L, 2L), tracks.sortedIds(MusicSortField.DURATION))
        assertEquals(
            listOf(3L, 1L, 2L),
            tracks.sortedIds(MusicSortField.TITLE, descending = true),
        )
        assertEquals("A", createMusicSortKeys(tracks[2].fileName).section)
    }

    @Test
    fun singleSongSearchFallsBackToTheSortedSongsPageQueue() {
        val queue = listOf(
            musicTrack(1L, "Alpha", 1L, "alpha.mp3", 1L, 1L),
            musicTrack(2L, "Bravo", 2L, "bravo.mp3", 2L, 2L),
            musicTrack(3L, "Charlie", 3L, "charlie.mp3", 3L, 3L),
        )
        val displayed = listOf(queue[1])

        val selection = resolveMusicPlaybackSelection(
            displayedTracks = displayed,
            queueTracks = queue,
            query = "brav",
            selectedIndex = 0,
        )

        assertEquals(queue, selection?.first)
        assertEquals(1, selection?.second)
    }

    @Test
    fun multipleSongSearchUsesTheCompleteSongsPageQueue() {
        val displayed = listOf(
            musicTrack(1L, "Alpha", 1L, "alpha.mp3", 1L, 1L),
            musicTrack(2L, "Alpine", 2L, "alpine.mp3", 2L, 2L),
        )
        val fullQueue = displayed + musicTrack(3L, "Bravo", 3L, "bravo.mp3", 3L, 3L)

        val selection = resolveMusicPlaybackSelection(
            displayedTracks = displayed,
            queueTracks = fullQueue,
            query = "al",
            selectedIndex = 1,
        )

        assertEquals(fullQueue, selection?.first)
        assertEquals(1, selection?.second)
    }

    @Test
    fun albumAndArtistGroupsExposeRequestedSortCounts() {
        val tracks = listOf(
            musicTrack(1L, "One", 1L, "one.mp3", 1L, 1L).copy(
                artist = "Artist B",
                album = "Album B",
                albumArtist = "Artist B",
                year = 2024,
            ),
            musicTrack(2L, "Two", 2L, "two.mp3", 2L, 2L).copy(
                artist = "Artist A",
                album = "Album A",
                albumArtist = "Artist A",
                year = 2020,
            ),
            musicTrack(3L, "Three", 3L, "three.mp3", 3L, 3L).copy(
                artist = "Artist A",
                album = "Album A",
                albumArtist = "Artist A",
                year = 2020,
            ),
        )

        val albums = buildAlbumGroups(tracks)
        assertEquals(
            listOf("Album A", "Album B"),
            sortAlbums(
                albums,
                AlbumSortConfig(field = AlbumSortField.SONG_COUNT, descending = true),
            ).map { it.name },
        )
        assertEquals(
            listOf("Album A", "Album B"),
            sortAlbums(
                albums,
                AlbumSortConfig(field = AlbumSortField.YEAR),
            ).map { it.name },
        )

        val artists = buildArtistGroups(tracks)
        assertEquals(
            listOf("Artist A", "Artist B"),
            sortArtists(
                artists,
                ArtistSortConfig(field = ArtistSortField.SONG_COUNT, descending = true),
            ).map { it.name },
        )
        assertEquals(2, artists.first { it.name == "Artist A" }.tracks.size)
        assertEquals(1, artists.first { it.name == "Artist A" }.albumCount)
    }

    @Test
    fun albumDiscSectionsSortMissingMetadataBeforeNumberedTracks() {
        val tracks = listOf(
            musicTrack(7L, "Disc One Track Two", 7L, "7.mp3", 7L, 7_000L)
                .copy(discNumber = 1, trackNumber = 2),
            musicTrack(1L, "Unnumbered B", 1L, "1.mp3", 1L, 1_000L),
            musicTrack(9L, "Disc Two Track Three", 9L, "9.mp3", 9L, 9_000L)
                .copy(discNumber = 2, trackNumber = 3),
            musicTrack(5L, "Disc One Missing B", 5L, "5.mp3", 5L, 5_000L)
                .copy(discNumber = 1),
            musicTrack(3L, "Numbered Two", 3L, "3.mp3", 3L, 3_000L)
                .copy(trackNumber = 2),
            musicTrack(8L, "Disc One Track One", 8L, "8.mp3", 8L, 8_000L)
                .copy(discNumber = 1, trackNumber = 1),
            musicTrack(2L, "Unnumbered A", 2L, "2.mp3", 2L, 2_000L),
            musicTrack(6L, "Disc One Missing A", 6L, "6.mp3", 6L, 6_000L)
                .copy(discNumber = 1),
            musicTrack(4L, "Numbered One", 4L, "4.mp3", 4L, 4_000L)
                .copy(trackNumber = 1),
        )

        val sections = buildAlbumDiscSections(tracks)

        assertEquals(listOf(null, 1, 2), sections.map { it.discNumber })
        assertEquals(listOf(2L, 1L, 4L, 3L), sections[0].tracks.map(MusicTrack::id))
        assertEquals(listOf(6L, 5L, 8L, 7L), sections[1].tracks.map(MusicTrack::id))
        assertEquals(listOf(9L), sections[2].tracks.map(MusicTrack::id))
        assertEquals(listOf(10_000L, 26_000L, 9_000L), sections.map { it.totalDurationMs })
    }

    @Test
    fun artistGroupsSplitDelimitedArtistNames() {
        val tracks = listOf(
            musicTrack(1L, "One", 1L, "one.mp3", 1L, 1L).copy(
                artist = "Artist A，Artist B, Artist C、Artist D/Artist E & Artist G",
                album = "Album One",
                albumArtist = "Artist A",
            ),
            musicTrack(2L, "Two", 2L, "two.mp3", 2L, 2L).copy(
                artist = "Artist B / Artist F",
                album = "Album Two",
                albumArtist = "Artist B",
            ),
        )

        val artistsByName = buildArtistGroups(tracks).associateBy { it.name }

        assertEquals(
            setOf(
                "Artist A",
                "Artist B",
                "Artist C",
                "Artist D",
                "Artist E",
                "Artist F",
                "Artist G",
            ),
            artistsByName.keys,
        )
        assertEquals(listOf(1L, 2L), artistsByName.getValue("Artist B").tracks.map { it.id })
        assertEquals(2, artistsByName.getValue("Artist B").albumCount)
        assertEquals(
            "Artist A / Artist B / Artist C / Artist D / Artist E / Artist G",
            displayArtistName(
                "Artist A，Artist B, Artist C、Artist D/Artist E & Artist G",
            ),
        )
        assertEquals(
            listOf("Artist A", "Artist B"),
            splitArtistNames("Artist A & Artist B & artist a"),
        )
    }

    @Test
    fun participatingArtistsResolveRealGroupsInTrackOrder() {
        val tracks = listOf(
            musicTrack(1L, "Duet", 1L, "duet.flac", 1L, 1L).copy(
                artist = "Artist B & Artist A",
                album = "Album",
            ),
            musicTrack(2L, "Solo", 2L, "solo.flac", 2L, 2L).copy(
                artist = "Artist A & Artist C",
                album = "Album",
            ),
            musicTrack(3L, "Guest", 3L, "guest.flac", 3L, 3L).copy(
                artist = "Artist C / Artist D",
                album = "Album",
            ),
        )
        val groups = buildArtistGroups(tracks)

        assertEquals(
            listOf("Artist B", "Artist A"),
            participatingArtistGroups(tracks.first(), groups).map { it.name },
        )
        assertEquals(
            listOf("Artist B", "Artist A", "Artist C", "Artist D"),
            participatingArtistGroups(tracks, groups).map { it.name },
        )
    }

    @Test
    fun songInfoFormatsAudioTypeAndPrimaryStorageLocation() {
        val track = musicTrack(
            id = 3L,
            title = "Song",
            dateAddedEpochSeconds = 1L,
            fileName = "song.flac",
            fileSizeBytes = 1L,
            durationMs = 1L,
        ).copy(
            folderPath = "/storage/self/primary/Music/Album",
            mimeType = "audio/flac",
        )

        assertEquals("FLAC", track.audioFormatLabel())
        assertEquals(
            "/storage/emulated/0/Music/Album/song.flac",
            track.displayFileLocation(),
        )
        assertEquals(
            "MPEG",
            track.copy(fileName = "song", mimeType = "audio/mpeg").audioFormatLabel(),
        )
    }

    @Test
    fun albumGridStylesKeepLegacySelectionsAndExpectedColumnCounts() {
        assertEquals(AlbumGridStyle.THREE, AlbumSortConfig().gridStyle)
        assertEquals(AlbumGridStyle.THREE.ordinal, AppSettings().albumGridStyleOrdinal)
        assertEquals(AlbumGridStyle.THREE.ordinal, resolveAlbumGridStyleOrdinal(null, null))
        assertEquals(AlbumGridStyle.THREE.ordinal, resolveAlbumGridStyleOrdinal(-1, null))
        assertEquals(
            listOf(2, 3),
            AlbumGridStyle.entries.map(AlbumGridStyle::columns),
        )
        assertEquals(
            AlbumGridStyle.TWO_SMALL.ordinal,
            resolveAlbumGridStyleOrdinal(storedStyleOrdinal = null, legacyColumns = 2),
        )
        assertEquals(
            AlbumGridStyle.THREE.ordinal,
            resolveAlbumGridStyleOrdinal(storedStyleOrdinal = null, legacyColumns = 3),
        )
        assertEquals(
            AlbumGridStyle.TWO_SMALL.ordinal,
            resolveAlbumGridStyleOrdinal(
                storedStyleOrdinal = AlbumGridStyle.TWO_SMALL.ordinal,
                legacyColumns = 3,
            ),
        )
        assertEquals(
            AlbumGridStyle.TWO_SMALL.ordinal,
            resolveAlbumGridStyleOrdinal(
                storedStyleOrdinal = 1,
                legacyColumns = 2,
            ),
        )
        assertEquals(
            AlbumGridStyle.THREE.ordinal,
            resolveAlbumGridStyleOrdinal(
                storedStyleOrdinal = 2,
                legacyColumns = 3,
            ),
        )
        assertEquals(2, albumGridColumnCount(AlbumGridStyle.TWO_SMALL, false, false, 400.dp))
        assertEquals(3, albumGridColumnCount(AlbumGridStyle.TWO_SMALL, true, false, 520.dp))
        assertEquals(4, albumGridColumnCount(AlbumGridStyle.TWO_SMALL, true, true, 680.dp))
        assertEquals(5, albumGridColumnCount(AlbumGridStyle.TWO_SMALL, true, false, 840.dp))
        assertEquals(3, albumGridColumnCount(AlbumGridStyle.THREE, false, true, 400.dp))
        assertEquals(3, albumGridColumnCount(AlbumGridStyle.THREE, true, false, 400.dp))
        assertEquals(4, albumGridColumnCount(AlbumGridStyle.THREE, true, false, 560.dp))
        assertEquals(5, albumGridColumnCount(AlbumGridStyle.THREE, true, true, 720.dp))
        assertEquals(6, albumGridColumnCount(AlbumGridStyle.THREE, true, false, 840.dp))
        assertTrue(com.melox.player.ui.component.library.isCompactLandscapeGrid(1000.dp, 599.dp))
        assertFalse(com.melox.player.ui.component.library.isCompactLandscapeGrid(1000.dp, 600.dp))
        assertFalse(com.melox.player.ui.component.library.isCompactLandscapeGrid(500.dp, 900.dp))
        assertEquals(2, albumGridColumnCount(AlbumGridStyle.TWO_SMALL, true, false, 451.dp, spaciousCells = true))
        assertEquals(2, albumGridColumnCount(AlbumGridStyle.TWO_SMALL, true, false, 452.dp, spaciousCells = true))
        assertEquals(3, albumGridColumnCount(AlbumGridStyle.THREE, true, false, 371.dp, spaciousCells = true))
        assertEquals(3, albumGridColumnCount(AlbumGridStyle.THREE, true, false, 372.dp, spaciousCells = true))
        assertEquals(3, albumGridColumnCount(AlbumGridStyle.TWO_SMALL, true, true, 840.dp, spaciousCells = true))
        assertEquals(4, albumGridColumnCount(AlbumGridStyle.THREE, true, true, 840.dp, spaciousCells = true))
        assertEquals(3, albumGridColumnCount(AlbumGridStyle.THREE, false, false, 400.dp, spaciousCells = true))
        assertEquals(2, albumGridColumnCount(AlbumGridStyle.TWO_SMALL, true, false, 663.dp, spaciousCells = true))
        assertEquals(3, albumGridColumnCount(AlbumGridStyle.TWO_SMALL, true, false, 664.dp, spaciousCells = true))
        assertEquals(3, albumGridColumnCount(AlbumGridStyle.THREE, true, false, 715.dp, spaciousCells = true))
        assertEquals(4, albumGridColumnCount(AlbumGridStyle.THREE, true, false, 716.dp, spaciousCells = true))
        assertEquals(5, albumGridColumnCount(AlbumGridStyle.TWO_SMALL, true, false, 2000.dp, spaciousCells = true))
        assertEquals(6, albumGridColumnCount(AlbumGridStyle.THREE, true, false, 2000.dp, spaciousCells = true))
        assertEquals(2, albumGridColumnCount(AlbumGridStyle.TWO_SMALL, true, false, 451.dp, spaciousCells = true, compactLandscape = true))
        assertEquals(2, albumGridColumnCount(AlbumGridStyle.TWO_SMALL, true, false, 452.dp, spaciousCells = true, compactLandscape = true))
        assertEquals(3, albumGridColumnCount(AlbumGridStyle.THREE, true, false, 371.dp, spaciousCells = true, compactLandscape = true))
        assertEquals(3, albumGridColumnCount(AlbumGridStyle.THREE, true, false, 372.dp, spaciousCells = true, compactLandscape = true))
        assertEquals(4, albumGridColumnCount(AlbumGridStyle.TWO_SMALL, true, true, 840.dp, spaciousCells = true, compactLandscape = true))
        assertEquals(6, albumGridColumnCount(AlbumGridStyle.THREE, true, true, 840.dp, spaciousCells = true, compactLandscape = true))
        assertEquals(3, albumGridColumnCount(AlbumGridStyle.THREE, false, false, 400.dp, spaciousCells = true, compactLandscape = true))
        assertEquals(2, albumGridColumnCount(AlbumGridStyle.TWO_SMALL, true, false, 543.dp, spaciousCells = true, compactLandscape = true))
        assertEquals(3, albumGridColumnCount(AlbumGridStyle.TWO_SMALL, true, false, 544.dp, spaciousCells = true, compactLandscape = true))
        assertEquals(3, albumGridColumnCount(AlbumGridStyle.THREE, true, false, 555.dp, spaciousCells = true, compactLandscape = true))
        assertEquals(4, albumGridColumnCount(AlbumGridStyle.THREE, true, false, 556.dp, spaciousCells = true, compactLandscape = true))
        for ((style, width, expected) in listOf(
            Triple(AlbumGridStyle.TWO_SMALL, 535.dp, 2),
            Triple(AlbumGridStyle.TWO_SMALL, 536.dp, 3),
            Triple(AlbumGridStyle.THREE, 547.dp, 3),
            Triple(AlbumGridStyle.THREE, 548.dp, 4),
            Triple(AlbumGridStyle.THREE, 840.dp - 262.dp, 4),
        )) {
            assertEquals(expected, albumGridColumnCount(
                style, true, false, width, spaciousCells = true,
                horizontalPadding = 32.dp, compactLandscape = true,
            ))
        }
        assertEquals(5, albumGridColumnCount(AlbumGridStyle.TWO_SMALL, true, false, 2000.dp, spaciousCells = true, compactLandscape = true))
        assertEquals(6, albumGridColumnCount(AlbumGridStyle.THREE, true, false, 2000.dp, spaciousCells = true, compactLandscape = true))
        assertEquals(2, playlistGridColumnCount(landscape = false, availableWidth = 400.dp))
        assertEquals(2, playlistGridColumnCount(landscape = false, availableWidth = 535.dp))
        assertEquals(3, playlistGridColumnCount(landscape = false, availableWidth = 536.dp))
        assertEquals(3, playlistGridColumnCount(landscape = false, availableWidth = 1200.dp))
        assertEquals(2, playlistGridColumnCount(landscape = true, availableWidth = 400.dp))
        assertEquals(3, playlistGridColumnCount(landscape = true, availableWidth = 600.dp))
        assertEquals(4, playlistGridColumnCount(landscape = true, availableWidth = 840.dp))
        assertEquals(3, responsiveGridColumnCount(600.dp, 32.dp, 140.dp, 2, 6))
        assertEquals(4, homeInitialRecommendationCount(400.dp))
        assertEquals(8, homeInitialRecommendationCount(1280.dp))
        assertEquals(9, homeInitialRecommendationCount(1413.dp))
        assertEquals(8, homeInitialRecommendationCount(1413.dp, landscape = true))
    }

    @Test
    fun playlistGridColumnsUse160DpThresholdWithinTwoToSixClamp() {
        val cases = mapOf(
            320 to 2, 364 to 2, 535 to 2, 536 to 3,
            707 to 3, 708 to 4, 879 to 4, 880 to 5,
            1051 to 5, 1052 to 6, 2000 to 6,
        )
        cases.forEach { (width, expectedColumns) ->
            assertEquals(
                "Playlist columns at ${width}dp",
                expectedColumns,
                playlistGridColumnCount(landscape = true, availableWidth = width.dp),
            )
        }
    }

    @Test
    fun playlistArtworkUsesTheExpectedLayoutForEachEntryCount() {
        assertEquals(PlaylistArtworkLayout.EMPTY, playlistArtworkLayout(0))
        assertEquals(PlaylistArtworkLayout.SINGLE, playlistArtworkLayout(1))
        assertEquals(PlaylistArtworkLayout.DOUBLE, playlistArtworkLayout(2))
        assertEquals(PlaylistArtworkLayout.COLLAGE, playlistArtworkLayout(3))
        assertEquals(PlaylistArtworkLayout.COLLAGE, playlistArtworkLayout(12))
    }

    @Test
    fun albumDetailHeaderCoverUsesFixedSizeInEveryOrientation() {
        assertEquals(96.dp, albumDetailHeaderCoverSize())
    }

    @Test
    fun albumGroupsUseNormalizedAlbumAndAlbumArtistInsteadOfMediaStoreId() {
        val tracks = listOf(
            musicTrack(1L, "Solo", 1L, "solo.mp3", 1L, 1L).copy(
                artist = "Artist A",
                album = "Compilation",
                albumArtist = "Various Artists",
                albumId = 100L,
                year = 2024,
            ),
            musicTrack(2L, "Duet", 2L, "duet.mp3", 2L, 2L).copy(
                artist = "Artist B",
                album = "  Compilation  ",
                albumArtist = "Various   Artists",
                albumId = 200L,
            ),
            musicTrack(3L, "Other", 3L, "other.mp3", 3L, 3L).copy(
                artist = "Artist C",
                album = "Compilation",
                albumArtist = "Artist C",
                albumId = 100L,
            ),
            musicTrack(4L, "Fourth", 4L, "fourth.mp3", 4L, 4L).copy(
                artist = "Artist D",
                album = "No Album Artist",
                albumId = 300L,
            ),
            musicTrack(5L, "Fifth", 5L, "fifth.mp3", 5L, 5L).copy(
                artist = "Artist E",
                album = "No Album Artist",
                albumId = 400L,
            ),
        )

        val albums = buildAlbumGroups(tracks)

        assertEquals(3, albums.size)
        assertEquals(
            listOf(1L, 2L),
            albums.first { it.albumArtist == "Various Artists" }.tracks.map(MusicTrack::id),
        )
        assertEquals(2024, albums.first { it.albumArtist == "Various Artists" }.year)
        assertEquals(
            listOf(4L, 5L),
            albums.first { it.name == "No Album Artist" }.tracks.map(MusicTrack::id),
        )
        assertNull(albums.first { it.name == "No Album Artist" }.albumArtist)
        assertNull(albums.first { it.name == "No Album Artist" }.year)
    }

    @Test
    fun folderPathsNormalizeAndHideThePrimaryStorageRoot() {
        assertEquals(
            "/Music/Rock",
            normalizeMusicFolderPath("Music/Rock/", includesFileName = false),
        )
        assertEquals(
            "/storage/emulated/0/Music",
            normalizeMusicFolderPath(
                "/storage/emulated/0/Music/song.flac",
                includesFileName = true,
            ),
        )
        assertEquals(
            "/Music",
            folderDisplayPath("/storage/emulated/0/Music/"),
        )
        assertEquals(
            "/Podcasts/Music",
            folderDisplayPath("/storage/self/primary/Podcasts/Music"),
        )
        assertNull(normalizeMusicFolderPath(null, includesFileName = false))
    }

    @Test
    fun folderGroupsKeepEqualNamesAtDifferentPathsAndSortDeterministically() {
        val tracks = listOf(
            musicTrack(1L, "One", 1L, "one.mp3", 1L, 1L)
                .copy(folderPath = "/storage/emulated/0/Music"),
            musicTrack(2L, "Two", 2L, "two.mp3", 2L, 2L)
                .copy(folderPath = "/storage/emulated/0/Music"),
            musicTrack(3L, "Three", 3L, "three.mp3", 3L, 3L)
                .copy(folderPath = "/storage/emulated/0/Podcasts/Music"),
            musicTrack(4L, "Four", 4L, "four.mp3", 4L, 4L)
                .copy(folderPath = "/storage/emulated/0/Download"),
        )

        val folders = buildFolderGroups(tracks)
        assertEquals(3, folders.size)
        assertEquals(2, folders.count { it.name == "Music" })
        assertEquals(
            listOf(1L, 2L),
            folders.single { it.displayPath == "/Music" }.tracks.map(MusicTrack::id),
        )
        assertEquals(
            "/Music",
            sortFolders(
                folders,
                FolderSortConfig(
                    field = FolderSortField.SONG_COUNT,
                    descending = true,
                ),
            ).first().displayPath,
        )
        assertEquals(
            listOf("/Download", "/Music", "/Podcasts/Music"),
            sortFolders(
                folders,
                FolderSortConfig(field = FolderSortField.NAME),
            ).map { it.displayPath },
        )
    }

    @Test
    fun musicSearchMatchesVisibleMetadataAndRanksResults() {
        val tracks = listOf(
            musicTrack(1L, "Blue Hour", 1L, "blue.mp3", 1L, 1L)
                .copy(artist = "TXT", album = "Minisode"),
            musicTrack(2L, "夜曲", 2L, "nocturne.flac", 2L, 2L)
                .copy(artist = "周杰伦", album = "十一月的萧邦"),
        )

        assertEquals(listOf(1L), filterMusicTracks(tracks, "txt").map(MusicTrack::id))
        assertEquals(listOf(2L), filterMusicTracks(tracks, "萧邦").map(MusicTrack::id))
        val fileTitles = SongSearchFields(showFileName = true)
        assertTrue(filterMusicTracks(tracks, "FLAC").isEmpty())
        assertEquals(listOf(2L), filterMusicTracks(tracks, "FLAC", fileTitles).map(MusicTrack::id))
        assertTrue(filterMusicTracks(tracks, "夜曲", fileTitles).isEmpty())
        assertTrue(filterMusicTracks(tracks, "TXT - Minisode").isEmpty())
        assertTrue(filterMusicTracks(tracks, "-").isEmpty())
        val hyphenatedAlbum = tracks.first().copy(album = "Mini-Sode")
        assertEquals(listOf(hyphenatedAlbum), filterMusicTracks(listOf(hyphenatedAlbum), "-"))
        assertEquals(listOf(1L, 2L), filterMusicTracks(tracks, "  ").map(MusicTrack::id))

        val rankedTracks = listOf(
            tracks[0].copy(id = 10L, title = "Midnight"),
            tracks[0].copy(id = 11L, title = "Midnight Night"),
            tracks[0].copy(id = 12L, title = "Night Sky"),
            tracks[0].copy(id = 13L, title = "Nightfall"),
            tracks[0].copy(id = 14L, title = "Night"),
            tracks[0].copy(id = 15L, title = "Another Song", artist = "Night"),
        )
        assertEquals(
            listOf(14L, 15L, 12L, 13L, 11L, 10L),
            filterMusicTracks(rankedTracks, " night ").map(MusicTrack::id),
        )
    }

    @Test
    fun libraryGroupSearchMatchesOnlyPageItemTitles() {
        val tracks = listOf(
            musicTrack(1L, "Blue Hour", 1L, "blue.mp3", 1L, 1L)
                .copy(
                    artist = "TXT",
                    album = "Minisode",
                    albumArtist = "Big Hit",
                    folderPath = "/storage/emulated/0/Collections/Pop",
                ),
            musicTrack(2L, "Night Drive", 2L, "night.flac", 2L, 2L)
                .copy(
                    artist = "Moon",
                    album = "After Dark",
                    albumArtist = "Night Label",
                    folderPath = "/storage/emulated/0/Archive/Jazz",
                ),
        )
        val albums = buildAlbumGroups(tracks)
        val artists = buildArtistGroups(tracks)

        assertEquals(
            listOf("Minisode"),
            filterAlbums(albums, "mini").map { it.name },
        )
        assertEquals(
            listOf("Moon"),
            filterArtists(artists, "moon").map { it.name },
        )
        assertEquals(1, filterAlbums(listOf(albums.first().copy(name = null)), "未知专辑", "未知专辑").size)
        assertEquals(1, filterArtists(listOf(artists.first().copy(name = null)), "未知艺术家", "未知艺术家").size)
        assertTrue(filterAlbums(albums, "blue").isEmpty())
        assertTrue(filterAlbums(albums, "big hit").isEmpty())
        assertTrue(filterArtists(artists, "after dark").isEmpty())
        assertTrue(filterArtists(artists, "night.flac").isEmpty())
    }

    @Test
    fun unresolvedMusicLibraryNeverMapsToEmptyContent() {
        assertEquals(
            MusicLibraryPlaceholder.Empty,
            ScanStatus.Idle.toMusicLibraryPlaceholder(),
        )
        assertEquals(
            MusicLibraryPlaceholder.Loading,
            ScanStatus.Scanning.toMusicLibraryPlaceholder(),
        )
        assertEquals(
            MusicLibraryPlaceholder.Empty,
            ScanStatus.PermissionRequired.toMusicLibraryPlaceholder(),
        )
        assertEquals(
            MusicLibraryPlaceholder.Empty,
            ScanStatus.Success(0).toMusicLibraryPlaceholder(),
        )
        assertEquals(
            MusicLibraryPlaceholder.Error,
            ScanStatus.Error("failed").toMusicLibraryPlaceholder(),
        )
    }

    @Test
    fun musicLibrarySnapshotRoundTripsTrackMetadata() {
        val tracks = listOf(
            musicTrack(
                id = 7L,
                title = "周杰伦",
                dateAddedEpochSeconds = 123L,
                fileName = "track.flac",
                fileSizeBytes = 456L,
                durationMs = 789L,
                dateModifiedEpochSeconds = 321L,
            ).copy(
                artist = "Artist",
                album = "Album",
                albumId = 42L,
                folderPath = "/storage/emulated/0/Music",
                mimeType = "audio/flac",
                bitrateBitsPerSecond = 1_800_000,
                sampleRateHz = 96_000,
                channelCount = 2,
                trackNumber = 3,
                discNumber = 2,
                bitDepth = 24,
                audioPropertiesScanned = true,
            ),
        )
        val encoded = ByteArrayOutputStream().also { output ->
            MusicLibrarySnapshotCodec.write(output, tracks)
        }.toByteArray()

        assertEquals(
            tracks,
            MusicLibrarySnapshotCodec.read(ByteArrayInputStream(encoded)),
        )
    }

    @Test
    fun audioQualityUsesFormatAndAvailableTechnicalMetadata() {
        val baseTrack = musicTrack(
            id = 9L,
            title = "Quality",
            dateAddedEpochSeconds = 1L,
            fileName = "quality.mp3",
            fileSizeBytes = 0L,
            durationMs = 180_000L,
        )

        assertEquals(
            AudioQuality.RAW,
            baseTrack.copy(
                fileName = "quality.flac",
                mimeType = "audio/flac",
                sampleRateHz = 192_000,
                bitDepth = 32,
            ).resolveAudioQuality(),
        )
        assertEquals(
            AudioQuality.HI_RES,
            baseTrack.copy(
                fileName = "quality.flac",
                mimeType = "audio/flac",
                sampleRateHz = 192_000,
                bitDepth = 24,
            ).resolveAudioQuality(),
        )
        assertEquals(
            AudioQuality.HI_RES,
            baseTrack.copy(
                fileName = "quality.flac",
                mimeType = "audio/flac",
                sampleRateHz = 44_100,
                bitDepth = 24,
            ).resolveAudioQuality(),
        )
        assertEquals(
            AudioQuality.SQ,
            baseTrack.copy(
                fileName = "quality.flac",
                mimeType = "audio/flac",
                sampleRateHz = 192_000,
                bitDepth = 16,
            ).resolveAudioQuality(),
        )
        assertEquals(
            AudioQuality.SQ,
            baseTrack.copy(
                fileName = "quality.flac",
                mimeType = "audio/flac",
                sampleRateHz = 44_099,
                bitDepth = 32,
            ).resolveAudioQuality(),
        )
        assertEquals(
            AudioQuality.HQ,
            baseTrack.copy(bitrateBitsPerSecond = 320_000).resolveAudioQuality(),
        )
        assertNull(
            baseTrack.copy(bitrateBitsPerSecond = 319_999).resolveAudioQuality(),
        )
        assertNull(baseTrack.resolveAudioQuality())
    }

    @Test
    fun audioQualityDoesNotGuessFromFileSize() {
        val track = musicTrack(
            id = 10L,
            title = "Estimated",
            dateAddedEpochSeconds = 1L,
            fileName = "estimated.m4a",
            fileSizeBytes = 7_200_000L,
            durationMs = 180_000L,
        )

        assertNull(track.resolveAudioQuality())
    }

    @Test
    fun tagLibAudioPropertiesNormalizeToStoredUnits() {
        assertEquals(
            LocalAudioProperties(
                durationMs = 180_000L,
                bitrateBitsPerSecond = 1_411_000,
                sampleRateHz = 96_000,
                channelCount = 2,
            ),
            normalizeAudioProperties(
                durationMs = 180_000,
                bitrateKbps = 1_411,
                sampleRateHz = 96_000,
                channelCount = 2,
            ),
        )
        assertEquals(
            LocalAudioProperties(
                durationMs = null,
                bitrateBitsPerSecond = null,
                sampleRateHz = null,
                channelCount = null,
            ),
            normalizeAudioProperties(
                durationMs = 0,
                bitrateKbps = -1,
                sampleRateHz = 0,
                channelCount = 0,
            ),
        )
    }

    @Test
    fun tagLibPropertyMapResolvesLibraryFields() {
        assertEquals(
            com.melox.player.data.library.LocalAudioTags(
                title = "Title",
                artist = "Artist A/Artist B",
                album = "Album",
                albumArtist = "Album Artist",
                year = 2024,
                trackNumber = 3,
                discNumber = 2,
            ),
            parseAudioTagProperties(
                mapOf(
                    "TITLE" to arrayOf(" Title "),
                    "ARTIST" to arrayOf("Artist A", "Artist B"),
                    "ALBUM" to arrayOf("Album"),
                    "ALBUM ARTIST" to arrayOf("Album Artist"),
                    "DATE" to arrayOf("2024-08-04"),
                    "TRACKNUMBER" to arrayOf("3/12"),
                    "DISCNUMBER" to arrayOf("2/2"),
                ),
            ),
        )
    }

    @Test
    fun tagLibPropertyMapResolvesWavAndId3Aliases() {
        assertEquals(
            com.melox.player.data.library.LocalAudioTags(
                title = "WAV title",
                artist = "WAV artist",
                album = "WAV album",
                albumArtist = "Album artist",
                year = 2025,
                trackNumber = 4,
                discNumber = 1,
            ),
            parseAudioTagProperties(
                mapOf(
                    "INAM" to arrayOf("WAV title"),
                    "IART" to arrayOf("WAV artist"),
                    "IPRD" to arrayOf("WAV album"),
                    "TPE2" to arrayOf("Album artist"),
                    "DATE" to arrayOf("2025"),
                    "ITRK" to arrayOf("4/10"),
                    "DISKNUMBER" to arrayOf("1/1"),
                ),
            ),
        )
        assertEquals(
            "ID3 title",
            parseAudioTagProperties(mapOf("TIT2" to arrayOf("ID3 title"))).title,
        )
    }

    @Test
    fun audioPropertiesReuseRequiresAnUnchangedCompletedSourceRead() {
        val track = musicTrack(
            id = 11L,
            title = "Cached",
            dateAddedEpochSeconds = 1L,
            fileName = "cached.flac",
            fileSizeBytes = 4_000L,
            durationMs = 5_000L,
            dateModifiedEpochSeconds = 3L,
        ).copy(audioPropertiesScanned = true)

        assertEquals(
            true,
            track.hasReusableAudioProperties(
                id = 11L,
                contentUri = "content://music/11",
                dateModifiedEpochSeconds = 3L,
                fileSizeBytes = 4_000L,
            ),
        )
        assertEquals(
            false,
            track.copy(audioPropertiesScanned = false).hasReusableAudioProperties(
                id = 11L,
                contentUri = "content://music/11",
                dateModifiedEpochSeconds = 3L,
                fileSizeBytes = 4_000L,
            ),
        )
        assertEquals(
            false,
            track.hasReusableAudioProperties(
                id = 11L,
                contentUri = "content://music/11",
                dateModifiedEpochSeconds = 4L,
                fileSizeBytes = 4_000L,
            ),
        )
    }

    @Test
    fun musicLibrarySnapshotReadsVersionOneWithSafeArtworkFallback() {
        val original = musicTrack(
            id = 8L,
            title = "Legacy",
            dateAddedEpochSeconds = 12L,
            fileName = "legacy.mp3",
            fileSizeBytes = 34L,
            durationMs = 56L,
        ).copy(
            artist = "Artist",
            album = "Album",
        )

        assertEquals(
            listOf(original),
            MusicLibrarySnapshotCodec.read(
                ByteArrayInputStream(encodeVersionOneSnapshot(original)),
            ),
        )
    }

    @Test
    fun musicLibrarySnapshotVersionSixForcesCompleteMetadataRefresh() {
        val original = musicTrack(
            id = 12L,
            title = "Legacy properties",
            dateAddedEpochSeconds = 13L,
            fileName = "legacy.flac",
            fileSizeBytes = 14L,
            durationMs = 15L,
            dateModifiedEpochSeconds = 16L,
        ).copy(
            artist = "Artist",
            album = "Album",
            albumArtist = "Album Artist",
            year = 2020,
            mimeType = "audio/flac",
            bitrateBitsPerSecond = 1_411_000,
            sampleRateHz = 96_000,
            channelCount = 2,
            audioPropertiesScanned = true,
        )

        val restored = MusicLibrarySnapshotCodec.read(
            ByteArrayInputStream(encodeVersionSixSnapshot(original)),
        ).single()

        assertFalse(restored.audioPropertiesScanned)
        assertNull(restored.trackNumber)
        assertNull(restored.discNumber)
        assertNull(restored.bitDepth)
    }

    @Test
    fun musicLibrarySnapshotRejectsTruncatedAndCorruptData() {
        val encoded = ByteArrayOutputStream().also { output ->
            MusicLibrarySnapshotCodec.write(
                output,
                listOf(
                    musicTrack(
                        id = 1L,
                        title = "Track",
                        dateAddedEpochSeconds = 2L,
                        fileName = "track.mp3",
                        fileSizeBytes = 3L,
                        durationMs = 4L,
                    ),
                ),
            )
        }.toByteArray()

        assertThrows(IOException::class.java) {
            MusicLibrarySnapshotCodec.read(
                ByteArrayInputStream(encoded.copyOf(encoded.size - 1)),
            )
        }

        val corrupted = encoded.copyOf().also { bytes ->
            bytes[bytes.lastIndex - Long.SIZE_BYTES] =
                (bytes[bytes.lastIndex - Long.SIZE_BYTES].toInt() xor 1).toByte()
        }
        assertThrows(IOException::class.java) {
            MusicLibrarySnapshotCodec.read(ByteArrayInputStream(corrupted))
        }
    }
}

private fun encodeVersionOneSnapshot(track: MusicTrack): ByteArray {
    val encoded = ByteArrayOutputStream()
    val checksum = CRC32()
    val output = DataOutputStream(
        CheckedOutputStream(BufferedOutputStream(encoded), checksum),
    )
    output.writeInt(0x5549584D)
    output.writeInt(1)
    output.writeInt(1)
    output.writeLong(track.id)
    output.writeNullableString(track.title)
    output.writeNullableString(track.artist)
    output.writeNullableString(track.album)
    output.writeLong(track.durationMs)
    output.writeLong(track.dateAddedEpochSeconds)
    output.writeNullableString(track.fileName)
    output.writeLong(track.fileSizeBytes)
    output.writeSizedString(track.contentUri)
    output.writeSizedString(track.titleSectionKey)
    output.writeSizedString(track.titleSortKey)
    output.writeLong(checksum.value)
    output.flush()
    return encoded.toByteArray()
}

private fun encodeVersionSixSnapshot(track: MusicTrack): ByteArray {
    val encoded = ByteArrayOutputStream()
    val checksum = CRC32()
    val output = DataOutputStream(
        CheckedOutputStream(BufferedOutputStream(encoded), checksum),
    )
    output.writeInt(0x5549584D)
    output.writeInt(6)
    output.writeInt(1)
    output.writeLong(track.id)
    output.writeNullableString(track.title)
    output.writeNullableString(track.artist)
    output.writeNullableString(track.album)
    output.writeNullableString(track.albumArtist)
    output.writeInt(track.year ?: 0)
    output.writeLong(track.durationMs)
    output.writeLong(track.dateAddedEpochSeconds)
    output.writeLong(track.dateModifiedEpochSeconds)
    output.writeNullableString(track.fileName)
    output.writeNullableString(track.folderPath)
    output.writeLong(track.albumId ?: 0L)
    output.writeLong(track.fileSizeBytes)
    output.writeSizedString(track.contentUri)
    output.writeSizedString(track.titleSectionKey)
    output.writeSizedString(track.titleSortKey)
    output.writeNullableString(track.mimeType)
    output.writeInt(track.bitrateBitsPerSecond ?: 0)
    output.writeInt(track.sampleRateHz ?: 0)
    output.writeInt(track.channelCount ?: 0)
    output.writeBoolean(track.audioPropertiesScanned)
    output.writeLong(checksum.value)
    output.flush()
    return encoded.toByteArray()
}

private fun List<MusicTrack>.sortedIds(
    field: MusicSortField,
    descending: Boolean = false,
): List<Long> = sortMusicTracks(
    tracks = this,
    config = MusicSortConfig(field = field, descending = descending),
).map(MusicTrack::id)
