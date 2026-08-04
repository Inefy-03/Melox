package com.melox.player

import android.Manifest
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
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
import com.melox.player.data.library.buildAlbumGroups
import com.melox.player.data.library.buildArtistGroups
import com.melox.player.data.library.buildFolderGroups
import com.melox.player.data.library.createMusicSortKeys
import com.melox.player.data.library.displayArtistName
import com.melox.player.data.library.filterAlbums
import com.melox.player.data.library.filterArtists
import com.melox.player.data.library.filterFolders
import com.melox.player.data.library.filterMusicTracks
import com.melox.player.data.library.folderDisplayPath
import com.melox.player.data.library.hasReusableAudioProperties
import com.melox.player.data.library.normalizeAudioProperties
import com.melox.player.data.library.parseAudioTagProperties
import com.melox.player.data.library.normalizeMusicFolderPath
import com.melox.player.data.library.sortAlbums
import com.melox.player.data.library.sortArtists
import com.melox.player.data.library.sortFolders
import com.melox.player.data.library.sortMusicTracks
import com.melox.player.data.library.splitArtistNames
import com.melox.player.data.playback.PlaybackSnapshotCodec
import com.melox.player.data.playback.retainReadableItems
import com.melox.player.data.repository.resolveAlbumGridStyleOrdinal
import com.melox.player.model.AppSettings
import com.melox.player.model.AudioQuality
import com.melox.player.model.BottomBarStyle
import com.melox.player.model.MusicTrack
import com.melox.player.model.PlaybackMode
import com.melox.player.model.PlaybackQueueItem
import com.melox.player.model.PlaybackSnapshot
import com.melox.player.model.PlaybackUiState
import com.melox.player.model.ScanStatus
import com.melox.player.model.ThemeMode
import com.melox.player.model.resolveAudioQuality
import com.melox.player.playback.isValidQueueIndex
import com.melox.player.playback.buildHomeRecommendationPlaybackQueue
import com.melox.player.playback.nextPlaybackMode
import com.melox.player.playback.nextQueueInsertionIndex
import com.melox.player.playback.playbackQueueReplacement
import com.melox.player.playback.reorderQueueForPlaybackMode
import com.melox.player.playback.sourceOrderForPlayNext
import com.melox.player.playback.toInitialPlaybackState
import com.melox.player.ui.component.library.findAlphabetTargetIndex
import com.melox.player.ui.component.library.fitArtworkDimensions
import com.melox.player.ui.component.library.formatDuration
import com.melox.player.ui.component.library.artworkCacheFileStem
import com.melox.player.ui.component.library.createArtworkCacheKey
import com.melox.player.ui.component.library.AlphabetSections
import com.melox.player.ui.component.library.audioFormatLabel
import com.melox.player.ui.component.library.displayFileLocation
import com.melox.player.ui.component.library.participatingArtistGroups
import com.melox.player.ui.component.library.snapshotArtworkDiskCacheEntries
import com.melox.player.ui.component.playback.hasDifferentMetadataSwipeTarget
import com.melox.player.ui.component.playback.PLAYER_LAYER_HANDOFF_END_PROGRESS
import com.melox.player.ui.component.playback.playerSheetBarAlpha
import com.melox.player.ui.component.playback.playerSheetDragProgress
import com.melox.player.ui.component.playback.playerSheetDragTarget
import com.melox.player.ui.component.playback.playerSheetPageAlpha
import com.melox.player.ui.component.playback.scaledRectAroundCenter
import com.melox.player.ui.component.playback.sharedArtworkRect
import com.melox.player.ui.component.playback.sharedContainerRect
import com.melox.player.ui.navigation.predictiveBackHandlerEnabled
import com.melox.player.ui.requiredAudioPermission
import com.melox.player.ui.resolveBottomBarStyle
import com.melox.player.ui.rootPagerUserScrollEnabled
import com.melox.player.ui.screen.home.buildHomeRecentlyAddedTracks
import com.melox.player.ui.screen.home.selectHomeRecommendations
import com.melox.player.ui.screen.library.MusicLibraryPlaceholder
import com.melox.player.ui.screen.library.toMusicLibraryPlaceholder
import com.melox.player.ui.screen.playback.queueCardHeight
import com.melox.player.ui.theme.toColorSchemeMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.CRC32
import java.util.zip.CheckedOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import kotlin.random.Random

class UiLogicTest {
    @Test
    fun rootPagerYieldsHorizontalGestureToRecommendationsAfterFirstCard() {
        assertTrue(rootPagerUserScrollEnabled(selectedPage = 0, homeRecommendationPage = 0))
        assertFalse(rootPagerUserScrollEnabled(selectedPage = 0, homeRecommendationPage = 1))
        assertTrue(rootPagerUserScrollEnabled(selectedPage = 1, homeRecommendationPage = 1))
    }

    @Test
    fun startupAppearanceDefaultsCanRenderNavigationImmediately() {
        val settings = AppSettings()

        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertFalse(settings.dynamicColorEnabled)
        assertEquals(true, settings.blurEnabled)
        assertEquals(true, settings.predictiveBackEnabled)
        assertEquals(BottomBarStyle.NORMAL, settings.bottomBarStyle)
    }

    @Test
    fun predictiveBackHandlerRequiresBothSettingAndBackEntry() {
        assertEquals(true, predictiveBackHandlerEnabled(true, true))
        assertFalse(predictiveBackHandlerEnabled(true, false))
        assertFalse(predictiveBackHandlerEnabled(false, true))
    }

    @Test
    fun themeSettingsMapToExpectedMiuixModes() {
        val expected = mapOf(
            AppSettings(ThemeMode.SYSTEM, false) to ColorSchemeMode.System,
            AppSettings(ThemeMode.LIGHT, false) to ColorSchemeMode.Light,
            AppSettings(ThemeMode.DARK, false) to ColorSchemeMode.Dark,
            AppSettings(ThemeMode.SYSTEM, true) to ColorSchemeMode.MonetSystem,
            AppSettings(ThemeMode.LIGHT, true) to ColorSchemeMode.MonetLight,
            AppSettings(ThemeMode.DARK, true) to ColorSchemeMode.MonetDark,
        )

        expected.forEach { (settings, mode) ->
            assertEquals(mode, settings.toColorSchemeMode())
        }
    }

    @Test
    fun unsupportedDynamicColorFallsBackToStaticTheme() {
        assertEquals(
            ColorSchemeMode.System,
            AppSettings(dynamicColorEnabled = true)
                .toColorSchemeMode(dynamicColorSupported = false),
        )
        assertEquals(
            ColorSchemeMode.Dark,
            AppSettings(
                themeMode = ThemeMode.DARK,
                dynamicColorEnabled = true,
            ).toColorSchemeMode(dynamicColorSupported = false),
        )
    }

    @Test
    fun audioPermissionChangesAtAndroid13() {
        assertEquals(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            requiredAudioPermission(sdkInt = 32),
        )
        assertEquals(
            Manifest.permission.READ_MEDIA_AUDIO,
            requiredAudioPermission(sdkInt = 33),
        )
    }

    @Test
    fun unsupportedLiquidGlassFallsBackToFloating() {
        assertEquals(
            BottomBarStyle.FLOATING,
            resolveBottomBarStyle(BottomBarStyle.LIQUID_GLASS, liquidGlassSupported = false),
        )
        assertEquals(
            BottomBarStyle.LIQUID_GLASS,
            resolveBottomBarStyle(BottomBarStyle.LIQUID_GLASS, liquidGlassSupported = true),
        )
        assertEquals(
            BottomBarStyle.NORMAL,
            resolveBottomBarStyle(BottomBarStyle.NORMAL, liquidGlassSupported = false),
        )
    }

    @Test
    fun sharedPlayerContainerReachesBothMeasuredEndpoints() {
        val miniPlayer = Rect(16f, 700f, 384f, 768f)
        val fullPlayer = Rect(0f, 0f, 400f, 800f)

        assertEquals(miniPlayer, sharedContainerRect(miniPlayer, fullPlayer, 0f))
        assertEquals(fullPlayer, sharedContainerRect(miniPlayer, fullPlayer, 1f))

        val midpoint = sharedContainerRect(miniPlayer, fullPlayer, 0.5f)
        assertEquals(372f, midpoint.width, 0.0001f)
        assertEquals(434f, midpoint.height, 0.0001f)
        assertEquals(200f, midpoint.center.x, 0.0001f)
        assertEquals(567f, midpoint.center.y, 0.0001f)
    }

    @Test
    fun sharedArtworkGrowsRightAndUpWithoutChangingAspectRatio() {
        val thumbnail = Rect(16f, 708f, 64f, 756f)
        val albumArt = Rect(28f, 120f, 372f, 464f)
        val samples = listOf(0f, 0.25f, 0.5f, 0.75f, 1f).map { progress ->
            sharedArtworkRect(thumbnail, albumArt, progress)
        }

        assertEquals(thumbnail, samples.first())
        assertEquals(albumArt, samples.last())
        samples.zipWithNext().forEach { (before, after) ->
            assertTrue(after.center.x > before.center.x)
            assertTrue(after.center.y < before.center.y)
            assertTrue(after.width > before.width)
            assertEquals(before.width / before.height, after.width / after.height, 0.0001f)
        }
    }

    @Test
    fun sharedArtworkTargetMatchesVisiblePlaybackScale() {
        val layoutBounds = Rect(28f, 120f, 372f, 464f)

        assertEquals(layoutBounds, scaledRectAroundCenter(layoutBounds, 1f))

        val pausedBounds = scaledRectAroundCenter(layoutBounds, 0.9f)
        assertEquals(45.2f, pausedBounds.left, 0.0001f)
        assertEquals(137.2f, pausedBounds.top, 0.0001f)
        assertEquals(354.8f, pausedBounds.right, 0.0001f)
        assertEquals(446.8f, pausedBounds.bottom, 0.0001f)
        assertEquals(layoutBounds.center.x, pausedBounds.center.x, 0.0001f)
        assertEquals(layoutBounds.center.y, pausedBounds.center.y, 0.0001f)
        assertEquals(layoutBounds.width / layoutBounds.height, pausedBounds.width / pausedBounds.height, 0.0001f)

        val resumePeakBounds = scaledRectAroundCenter(layoutBounds, 1.02f)
        assertEquals(layoutBounds.center.x, resumePeakBounds.center.x, 0.0001f)
        assertEquals(layoutBounds.center.y, resumePeakBounds.center.y, 0.0001f)
        assertEquals(layoutBounds.width * 1.02f, resumePeakBounds.width, 0.0001f)
    }

    @Test
    fun sharedPlayerLayersHandOffOnOneProgress() {
        assertEquals(1f, playerSheetBarAlpha(0f), 0f)
        assertEquals(0f, playerSheetPageAlpha(0f), 0f)
        assertTrue(playerSheetBarAlpha(0.2f) in 0f..1f)
        assertTrue(playerSheetPageAlpha(0.2f) in 0f..1f)
        assertEquals(0f, playerSheetBarAlpha(PLAYER_LAYER_HANDOFF_END_PROGRESS), 0f)
        assertEquals(1f, playerSheetPageAlpha(PLAYER_LAYER_HANDOFF_END_PROGRESS), 0f)
        assertEquals(0f, playerSheetBarAlpha(1f), 0f)
        assertEquals(1f, playerSheetPageAlpha(1f), 0f)
    }

    @Test
    fun sharedPlayerDragMapsDirectlyToOneClampedProgress() {
        assertEquals(
            0.25f,
            playerSheetDragProgress(
                startProgress = 0f,
                dragDistanceY = -200f,
                containerHeight = 800f,
            ),
            0f,
        )
        assertEquals(
            0.75f,
            playerSheetDragProgress(
                startProgress = 1f,
                dragDistanceY = 200f,
                containerHeight = 800f,
            ),
            0f,
        )
        assertEquals(0f, playerSheetDragProgress(0f, 200f, 800f), 0f)
        assertEquals(1f, playerSheetDragProgress(1f, -200f, 800f), 0f)
    }

    @Test
    fun sharedPlayerDragReleaseFollowsVerticalDirection() {
        assertTrue(playerSheetDragTarget(-1f, 1f, originOpen = false))
        assertFalse(playerSheetDragTarget(1f, -1f, originOpen = true))
        assertTrue(playerSheetDragTarget(0f, -1f, originOpen = false))
        assertFalse(playerSheetDragTarget(0f, 1f, originOpen = true))
        assertTrue(playerSheetDragTarget(0f, 0f, originOpen = true))
        assertFalse(playerSheetDragTarget(0f, 0f, originOpen = false))
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
                dateAddedEpochSeconds = 30L,
                fileName = "c.mp3",
                fileSizeBytes = 300L,
                durationMs = 200L,
            ),
            musicTrack(
                id = 2L,
                title = "Alpha",
                dateAddedEpochSeconds = 10L,
                fileName = "b.mp3",
                fileSizeBytes = 100L,
                durationMs = 300L,
            ),
            musicTrack(
                id = 3L,
                title = "Charlie",
                dateAddedEpochSeconds = 20L,
                fileName = "a.mp3",
                fileSizeBytes = 200L,
                durationMs = 100L,
            ),
        )

        assertEquals(listOf(2L, 1L, 3L), tracks.sortedIds(MusicSortField.TITLE))
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
                artist = "Artist A",
                album = "Album",
            ),
        )
        val groups = buildArtistGroups(tracks)

        assertEquals(
            listOf("Artist B", "Artist A"),
            participatingArtistGroups(tracks.first(), groups).map { it.name },
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
    fun metadataSwipeRequiresADifferentQueueTargetForHaptic() {
        val single = PlaybackUiState(
            queue = listOf(playbackQueueItem("one")),
            currentIndex = 0,
        )
        val multiple = PlaybackUiState(
            queue = listOf(playbackQueueItem("one"), playbackQueueItem("two")),
            currentIndex = 0,
        )

        assertFalse(single.hasDifferentMetadataSwipeTarget(-1f))
        assertFalse(single.hasDifferentMetadataSwipeTarget(1f))
        assertEquals(true, multiple.hasDifferentMetadataSwipeTarget(-1f))
        assertEquals(true, multiple.hasDifferentMetadataSwipeTarget(1f))
    }

    @Test
    fun albumGridStylesKeepLegacySelectionsAndExpectedColumnCounts() {
        assertEquals(AlbumGridStyle.TWO_SMALL, AlbumSortConfig().gridStyle)
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
    }

    @Test
    fun homeRecommendationsStopWhenArtworkTracksAreExhausted() = runBlocking {
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

        assertEquals(artworkTrackIds, recommendations.map(MusicTrack::id).toSet())
        assertEquals(artworkTrackIds.size, recommendations.size)
        assertEquals(recommendations.map(MusicTrack::id), repeatedSelection.map(MusicTrack::id))
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
            knownArtworkTrackIds = priorityRecommendations.map(MusicTrack::id).toSet(),
        ) { track ->
            expansionProbeIds += track.id
            true
        }

        assertEquals(2, priorityProbeIds.size)
        assertEquals(priorityRecommendations, expandedRecommendations.take(2))
        assertTrue(expansionProbeIds.none { it in priorityRecommendations.map(MusicTrack::id) })
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

    @Test
    fun homeRecentlyAddedTracksPrioritizeScanAdditionsAndFillWithNewestFiles() {
        val tracks = listOf(
            musicTrack(1L, "Track 1", 1L, "1.mp3", 1L, 1L, dateModifiedEpochSeconds = 10L),
            musicTrack(2L, "Track 2", 2L, "2.mp3", 2L, 2L, dateModifiedEpochSeconds = 40L),
            musicTrack(3L, "Track 3", 3L, "3.mp3", 3L, 3L, dateModifiedEpochSeconds = 30L),
            musicTrack(4L, "Track 4", 4L, "4.mp3", 4L, 4L, dateModifiedEpochSeconds = 20L),
        )

        assertEquals(
            listOf(1L, 2L, 3L, 4L),
            buildHomeRecentlyAddedTracks(tracks, recentlyAddedTrackIds = setOf(1L))
                .map(MusicTrack::id),
        )
    }

    @Test
    fun homeRecentlyAddedTracksKeepEveryNewSongAboveTheDefaultLimit() {
        val tracks = (1L..21L).map { id ->
            musicTrack(
                id = id,
                title = "Track $id",
                dateAddedEpochSeconds = id,
                fileName = "$id.mp3",
                fileSizeBytes = id,
                durationMs = id,
                dateModifiedEpochSeconds = id,
            )
        }

        assertEquals(
            (21L downTo 1L).toList(),
            buildHomeRecentlyAddedTracks(
                tracks = tracks,
                recentlyAddedTrackIds = tracks.map(MusicTrack::id).toSet(),
            ).map(MusicTrack::id),
        )
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
        assertTrue(filterFolders(folders, "podcasts").isEmpty())
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
    fun musicSearchMatchesVisibleMetadataAndPreservesSourceOrder() {
        val tracks = listOf(
            musicTrack(1L, "Blue Hour", 1L, "blue.mp3", 1L, 1L)
                .copy(artist = "TXT", album = "Minisode"),
            musicTrack(2L, "夜曲", 2L, "nocturne.flac", 2L, 2L)
                .copy(artist = "周杰伦", album = "十一月的萧邦"),
        )

        assertEquals(listOf(1L), filterMusicTracks(tracks, "txt").map(MusicTrack::id))
        assertEquals(listOf(2L), filterMusicTracks(tracks, "萧邦").map(MusicTrack::id))
        assertEquals(listOf(2L), filterMusicTracks(tracks, "FLAC").map(MusicTrack::id))
        assertEquals(listOf(1L, 2L), filterMusicTracks(tracks, "  ").map(MusicTrack::id))
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
        val folders = buildFolderGroups(tracks)

        assertEquals(
            listOf("Minisode"),
            filterAlbums(albums, "mini").map { it.name },
        )
        assertEquals(
            listOf("Moon"),
            filterArtists(artists, "moon").map { it.name },
        )
        assertEquals(
            listOf("Pop"),
            filterFolders(folders, "pop").map { it.name },
        )
        assertTrue(filterAlbums(albums, "blue").isEmpty())
        assertTrue(filterAlbums(albums, "big hit").isEmpty())
        assertTrue(filterArtists(artists, "after dark").isEmpty())
        assertTrue(filterArtists(artists, "night.flac").isEmpty())
        assertTrue(filterFolders(folders, "collections").isEmpty())
        assertTrue(filterFolders(folders, "blue hour").isEmpty())
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
            AudioQuality.HR,
            baseTrack.copy(
                fileName = "quality.flac",
                mimeType = "audio/flac",
                bitrateBitsPerSecond = 900_000,
                sampleRateHz = 96_000,
            ).resolveAudioQuality(),
        )
        assertEquals(
            AudioQuality.SQ,
            baseTrack.copy(
                fileName = "quality.flac",
                mimeType = "audio/flac",
                bitrateBitsPerSecond = 900_000,
                sampleRateHz = 44_100,
            ).resolveAudioQuality(),
        )
        assertEquals(
            AudioQuality.HQ,
            baseTrack.copy(bitrateBitsPerSecond = 320_000).resolveAudioQuality(),
        )
        assertNull(
            baseTrack.copy(bitrateBitsPerSecond = 128_000).resolveAudioQuality(),
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
    fun tagLibPropertyMapResolvesLyricoCompatibleLibraryFields() {
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
    fun artworkCacheIdentityChangesWithSourceRevisionAndSize() {
        val base = createArtworkCacheKey(
            contentUri = "content://music/1",
            dateModifiedEpochSeconds = 2L,
            fileSizeBytes = 3L,
            targetSizePx = 48,
        )

        assertEquals(64, artworkCacheFileStem(base).length)
        assertEquals(artworkCacheFileStem(base), artworkCacheFileStem(base))
        assertNotEquals(
            base,
            createArtworkCacheKey("content://music/1", 4L, 3L, 48),
        )
        assertNotEquals(
            base,
            createArtworkCacheKey("content://music/1", 2L, 5L, 48),
        )
        assertNotEquals(
            base,
            createArtworkCacheKey("content://music/1", 2L, 3L, 96),
        )
    }

    @Test
    fun artworkThumbnailDimensionsPreserveAspectRatioWithoutUpscaling() {
        assertEquals(256 to 256, fitArtworkDimensions(512, 512, 256))
        assertEquals(256 to 128, fitArtworkDimensions(1024, 512, 256))
        assertEquals(128 to 256, fitArtworkDimensions(512, 1024, 256))
        assertEquals(120 to 80, fitArtworkDimensions(120, 80, 256))
    }

    @Test
    fun artworkDiskCacheSnapshotsMutableFileMetadataBeforeSorting() {
        val newer = CountingMetadataFile("newer.png", lastModifiedValue = 20L)
        val older = CountingMetadataFile("older.png", lastModifiedValue = 10L)

        val entries = snapshotArtworkDiskCacheEntries(arrayOf(newer, older))

        assertEquals(listOf("older.png", "newer.png"), entries.map { it.file.name })
        assertEquals(1, newer.lastModifiedCalls)
        assertEquals(1, older.lastModifiedCalls)
        assertEquals(1, newer.lengthCalls)
        assertEquals(1, older.lengthCalls)
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

    @Test
    fun playbackSnapshotRoundTripsQueueAndTransportState() {
        val firstItem = PlaybackQueueItem(
            mediaId = "7",
            trackId = 7L,
            contentUri = "content://media/audio/7",
            title = "Track",
            artist = "Artist",
            album = "Album",
            durationMs = 123_000L,
            dateModifiedEpochSeconds = 456L,
            fileSizeBytes = 789L,
            sourceOrder = 2.0,
            playbackMode = PlaybackMode.RANDOM,
        )
        val secondItem = firstItem.copy(
            mediaId = "8",
            trackId = 8L,
            contentUri = "content://media/audio/8",
            title = "Track 2",
            sourceOrder = 3.0,
        )
        val snapshot = PlaybackSnapshot(
            queue = listOf(firstItem, secondItem),
            currentIndex = 1,
            positionMs = 12_000L,
            playbackMode = PlaybackMode.RANDOM,
        )
        val encoded = ByteArrayOutputStream().also { output ->
            PlaybackSnapshotCodec.write(output, snapshot)
        }.toByteArray()

        assertEquals(
            snapshot,
            PlaybackSnapshotCodec.read(ByteArrayInputStream(encoded)),
        )
        assertThrows(IOException::class.java) {
            PlaybackSnapshotCodec.read(
                ByteArrayInputStream(encoded.copyOf(encoded.size - 3)),
            )
        }

        val invalidPlaybackMode = encoded.copyOf().also { bytes ->
            val modeOffset = bytes.size - Long.SIZE_BYTES - Int.SIZE_BYTES
            bytes[modeOffset] = 0
            bytes[modeOffset + 1] = 0
            bytes[modeOffset + 2] = 0
            bytes[modeOffset + 3] = 99
        }
        assertThrows(IOException::class.java) {
            PlaybackSnapshotCodec.read(ByteArrayInputStream(invalidPlaybackMode))
        }
    }

    @Test
    fun playbackRestorePrunesUnreadableItemsWithoutLosingCurrentPosition() {
        val queue = listOf(
            playbackQueueItem("missing-before"),
            playbackQueueItem("current"),
            playbackQueueItem("after"),
        )
        val snapshot = PlaybackSnapshot(
            queue = queue,
            currentIndex = 1,
            positionMs = 12_345L,
            playbackMode = PlaybackMode.ORDER,
        )

        val retainedCurrent = snapshot.retainReadableItems { it.mediaId != "missing-before" }
        assertEquals(listOf("current", "after"), retainedCurrent?.queue?.map { it.mediaId })
        assertEquals(0, retainedCurrent?.currentIndex)
        assertEquals(12_345L, retainedCurrent?.positionMs)

        val missingCurrent = snapshot.retainReadableItems { it.mediaId == "after" }
        assertEquals(0, missingCurrent?.currentIndex)
        assertEquals(0L, missingCurrent?.positionMs)

        assertNull(snapshot.retainReadableItems { false })
    }

    @Test
    fun playbackSnapshotReadsVersionOneShuffleAsRandomQueue() {
        val item = playbackQueueItem("legacy")
        val restored = PlaybackSnapshotCodec.read(
            ByteArrayInputStream(
                encodeVersionOnePlaybackSnapshot(
                    item = item,
                    shuffleEnabled = true,
                    repeatMode = 2,
                ),
            ),
        )

        assertEquals(PlaybackMode.RANDOM, restored.playbackMode)
        assertEquals(PlaybackMode.RANDOM, restored.queue.single().playbackMode)
        assertEquals(0.0, restored.queue.single().sourceOrder, 0.0)
    }

    @Test
    fun playbackModesCycleAndRandomizeQueueOnce() {
        assertEquals(PlaybackMode.REPEAT_ONE, nextPlaybackMode(PlaybackMode.ORDER))
        assertEquals(PlaybackMode.RANDOM, nextPlaybackMode(PlaybackMode.REPEAT_ONE))
        assertEquals(PlaybackMode.ORDER, nextPlaybackMode(PlaybackMode.RANDOM))

        val sourceQueue = listOf("a", "b", "c", "d").mapIndexed { index, mediaId ->
            playbackQueueItem(mediaId).copy(sourceOrder = index.toDouble())
        }
        val randomized = reorderQueueForPlaybackMode(
            queue = sourceQueue,
            currentIndex = 1,
            targetMode = PlaybackMode.RANDOM,
            random = Random(17),
        )
        assertEquals("b", randomized.queue.first().mediaId)
        assertEquals(0, randomized.currentIndex)
        assertEquals(sourceQueue.map { it.mediaId }.toSet(), randomized.queue.map { it.mediaId }.toSet())
        assertEquals(
            setOf(PlaybackMode.RANDOM),
            randomized.queue.map { it.playbackMode }.toSet(),
        )

        val restored = reorderQueueForPlaybackMode(
            queue = randomized.queue,
            currentIndex = randomized.currentIndex,
            targetMode = PlaybackMode.ORDER,
        )
        assertEquals(listOf("a", "b", "c", "d"), restored.queue.map { it.mediaId })
        assertEquals(1, restored.currentIndex)
    }

    @Test
    fun randomToOrderReplacementKeepsCurrentSlotOutsideTwoBulkSpans() {
        val sourceQueue = List(100_000) { index ->
            playbackQueueItem(index.toString()).copy(sourceOrder = index.toDouble())
        }
        val randomized = reorderQueueForPlaybackMode(
            queue = sourceQueue,
            currentIndex = 75_000,
            targetMode = PlaybackMode.RANDOM,
            random = Random(17),
        )
        val restored = reorderQueueForPlaybackMode(
            queue = randomized.queue,
            currentIndex = randomized.currentIndex,
            targetMode = PlaybackMode.ORDER,
        )
        val replacement = playbackQueueReplacement(
            targetQueue = restored.queue,
            currentIndex = randomized.currentIndex,
            targetCurrentIndex = restored.currentIndex,
        )

        assertEquals(75_000, replacement.beforeCurrent.size)
        assertEquals(24_999, replacement.afterCurrent.size)
        assertTrue(replacement.replaceAfterCurrentFirst)
        assertEquals("74999", replacement.beforeCurrent.last().mediaId)
        assertEquals("75001", replacement.afterCurrent.first().mediaId)
        assertFalse(
            (replacement.beforeCurrent + replacement.afterCurrent)
                .any { item ->
                    item.mediaId == restored.queue[restored.currentIndex].mediaId &&
                        item.contentUri == restored.queue[restored.currentIndex].contentUri &&
                        item.sourceOrder == restored.queue[restored.currentIndex].sourceOrder
                },
        )
    }

    @Test
    fun queueReplacementExcludesOnlyTheCurrentRepeatedSlot() {
        val targetQueue = listOf(
            playbackQueueItem("repeat").copy(sourceOrder = 0.0),
            playbackQueueItem("repeat").copy(sourceOrder = 1.0),
            playbackQueueItem("other").copy(sourceOrder = 2.0),
        )

        val replacement = playbackQueueReplacement(
            targetQueue = targetQueue,
            currentIndex = 2,
            targetCurrentIndex = 1,
        )

        assertEquals(listOf(0.0), replacement.beforeCurrent.map(PlaybackQueueItem::sourceOrder))
        assertEquals(listOf(2.0), replacement.afterCurrent.map(PlaybackQueueItem::sourceOrder))
        assertFalse(replacement.replaceAfterCurrentFirst)
    }

    @Test
    fun randomModeKeepsCurrentItemAndModeMetadata() {
        val sourceQueue = listOf("a", "b").mapIndexed { index, mediaId ->
            playbackQueueItem(mediaId).copy(sourceOrder = index.toDouble())
        }

        val randomized = reorderQueueForPlaybackMode(
            queue = sourceQueue,
            currentIndex = 0,
            targetMode = PlaybackMode.RANDOM,
            random = Random(1),
        )

        assertEquals("a", randomized.queue.first().mediaId)
        assertEquals(setOf(PlaybackMode.RANDOM), randomized.queue.map { it.playbackMode }.toSet())
    }

    @Test
    fun randomModeRetainsRepeatedQueueSlots() {
        val sourceQueue = listOf(
            playbackQueueItem("repeat").copy(sourceOrder = 0.0),
            playbackQueueItem("repeat").copy(sourceOrder = 1.0),
            playbackQueueItem("other").copy(sourceOrder = 2.0),
        )

        val randomized = reorderQueueForPlaybackMode(
            queue = sourceQueue,
            currentIndex = 1,
            targetMode = PlaybackMode.RANDOM,
            random = Random(1),
        )

        assertEquals(sourceQueue.size, randomized.queue.size)
        assertEquals(1.0, randomized.queue.first().sourceOrder, 0.0)
        assertEquals(0, randomized.currentIndex)
        assertEquals(
            sourceQueue.map(PlaybackQueueItem::sourceOrder).sorted(),
            randomized.queue.map(PlaybackQueueItem::sourceOrder).sorted(),
        )
    }

    @Test
    fun miniPlaybackSnapshotSeedsLastCurrentItemBeforeServiceConnection() {
        val item = playbackQueueItem("last").copy(
            durationMs = 123_000L,
            playbackMode = PlaybackMode.RANDOM,
        )

        val state = PlaybackSnapshot(
            queue = listOf(item),
            currentIndex = 0,
            positionMs = 4_000L,
            playbackMode = PlaybackMode.RANDOM,
        ).toInitialPlaybackState()

        assertEquals(item, state.currentItem)
        assertEquals(4_000L, state.positionMs)
        assertEquals(PlaybackMode.RANDOM, state.playbackMode)
        assertFalse(state.isPlaying)
    }

    @Test
    fun bottomBarBooleansResolveToExpectedNavigationStyle() {
        assertEquals(BottomBarStyle.NORMAL, AppSettings().bottomBarStyle)
        assertEquals(
            BottomBarStyle.FLOATING,
            AppSettings(floatingBottomBar = true).bottomBarStyle,
        )
        assertEquals(
            BottomBarStyle.LIQUID_GLASS,
            AppSettings(floatingBottomBar = true, liquidGlass = true).bottomBarStyle,
        )
    }

    @Test
    fun queueOperationIndicesAreDeterministicAndBoundsChecked() {
        assertEquals(0, nextQueueInsertionIndex(currentIndex = 0, itemCount = 0))
        assertEquals(1, nextQueueInsertionIndex(currentIndex = 0, itemCount = 3))
        assertEquals(3, nextQueueInsertionIndex(currentIndex = 2, itemCount = 3))
        assertEquals(0, nextQueueInsertionIndex(currentIndex = -1, itemCount = 3))

        assertEquals(true, isValidQueueIndex(index = 0, itemCount = 1))
        assertEquals(true, isValidQueueIndex(index = 2, itemCount = 3))
        assertFalse(isValidQueueIndex(index = -1, itemCount = 3))
        assertFalse(isValidQueueIndex(index = 3, itemCount = 3))
        assertFalse(isValidQueueIndex(index = 0, itemCount = 0))
    }

    @Test
    fun queueCardHeightCapsLargeRestoredQueuesBeforeMultiplication() {
        val rowHeight = 68.dp
        val maxHeight = 600.dp

        assertEquals(0.dp, queueCardHeight(itemCount = 0, rowHeight, maxHeight))
        assertEquals(204.dp, queueCardHeight(itemCount = 3, rowHeight, maxHeight))
        assertEquals(maxHeight, queueCardHeight(itemCount = 100_000, rowHeight, maxHeight))
        assertEquals(maxHeight, queueCardHeight(itemCount = Int.MAX_VALUE, rowHeight, maxHeight))
    }

    @Test
    fun playNextPreservesOrderQueuePositionOutsideRandomMode() {
        val queue = listOf(
            playbackQueueItem("a").copy(sourceOrder = 0.0),
            playbackQueueItem("b").copy(sourceOrder = 1.0),
            playbackQueueItem("c").copy(sourceOrder = 2.0),
        )

        assertEquals(
            0.5,
            sourceOrderForPlayNext(
                queue,
                currentIndex = 0,
                playbackMode = PlaybackMode.ORDER,
            ),
            0.0,
        )
        assertEquals(
            3.0,
            sourceOrderForPlayNext(
                queue,
                currentIndex = 1,
                playbackMode = PlaybackMode.RANDOM,
            ),
            0.0,
        )
    }
}

private class CountingMetadataFile(
    path: String,
    private val lastModifiedValue: Long,
) : File(path) {
    var lastModifiedCalls: Int = 0
        private set
    var lengthCalls: Int = 0
        private set

    override fun isFile(): Boolean = true

    override fun lastModified(): Long {
        lastModifiedCalls += 1
        return lastModifiedValue
    }

    override fun length(): Long {
        lengthCalls += 1
        return 1L
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

private fun encodeVersionOnePlaybackSnapshot(
    item: PlaybackQueueItem,
    shuffleEnabled: Boolean,
    repeatMode: Int,
): ByteArray {
    val encoded = ByteArrayOutputStream()
    val checksum = CRC32()
    val output = DataOutputStream(
        CheckedOutputStream(BufferedOutputStream(encoded), checksum),
    )
    output.writeInt(0x4D454C50)
    output.writeInt(1)
    output.writeInt(1)
    output.writeSizedString(item.mediaId)
    output.writeNullableLong(item.trackId)
    output.writeSizedString(item.contentUri)
    output.writeSizedString(item.title)
    output.writeNullableString(item.artist)
    output.writeNullableString(item.album)
    output.writeLong(item.durationMs)
    output.writeLong(item.dateModifiedEpochSeconds)
    output.writeLong(item.fileSizeBytes)
    output.writeInt(0)
    output.writeLong(12_000L)
    output.writeBoolean(shuffleEnabled)
    output.writeInt(repeatMode)
    output.writeLong(checksum.value)
    output.flush()
    return encoded.toByteArray()
}

private fun DataOutputStream.writeNullableLong(value: Long?) {
    writeBoolean(value != null)
    if (value != null) writeLong(value)
}

private fun DataOutputStream.writeNullableString(value: String?) {
    writeBoolean(value != null)
    if (value != null) writeSizedString(value)
}

private fun DataOutputStream.writeSizedString(value: String) {
    val bytes = value.toByteArray(Charsets.UTF_8)
    writeInt(bytes.size)
    write(bytes)
}

private fun musicTrack(
    id: Long,
    title: String,
    dateAddedEpochSeconds: Long,
    fileName: String,
    fileSizeBytes: Long,
    durationMs: Long,
    dateModifiedEpochSeconds: Long = 0L,
): MusicTrack {
    val sortKeys = createMusicSortKeys(title)
    return MusicTrack(
        id = id,
        title = title,
        artist = null,
        album = null,
        durationMs = durationMs,
        dateAddedEpochSeconds = dateAddedEpochSeconds,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileName = fileName,
        fileSizeBytes = fileSizeBytes,
        contentUri = "content://music/$id",
        titleSectionKey = sortKeys.section,
        titleSortKey = sortKeys.value,
    )
}

private fun playbackQueueItem(mediaId: String): PlaybackQueueItem = PlaybackQueueItem(
    mediaId = mediaId,
    trackId = null,
    contentUri = "content://music/$mediaId",
    title = mediaId,
    artist = null,
    album = null,
    durationMs = 1L,
    dateModifiedEpochSeconds = 1L,
    fileSizeBytes = 1L,
)

private fun List<MusicTrack>.sortedIds(
    field: MusicSortField,
    descending: Boolean = false,
): List<Long> = sortMusicTracks(
    tracks = this,
    config = MusicSortConfig(field = field, descending = descending),
).map(MusicTrack::id)
