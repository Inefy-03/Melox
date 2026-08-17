package com.melox.player

import com.melox.player.data.repository.customFolderPrefixForDocumentId
import com.melox.player.data.repository.exactLyricsSidecarCandidates
import com.melox.player.data.repository.folderMatchesPrefix
import com.melox.player.model.LyricsFormat
import com.melox.player.ui.screen.playback.correctedLyricClockMs
import com.melox.player.ui.viewmodel.shouldEmitScanNoChanges
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicScanLogicTest {
    @Test
    fun primaryDocumentTreeMapsToMediaStoreRelativePrefix() {
        assertEquals(
            "/Music/Albums",
            customFolderPrefixForDocumentId("primary:Music/Albums", sdkInt = 29),
        )
        assertEquals(
            "/storage/emulated/0/Music/Albums",
            customFolderPrefixForDocumentId("primary:Music/Albums", sdkInt = 28),
        )
        assertNull(customFolderPrefixForDocumentId("1234-5678:Music", sdkInt = 29))
    }

    @Test
    fun customFolderMatchesOnlyTheFolderAndItsDescendants() {
        assertTrue(
            folderMatchesPrefix(
                rawPath = "Music/Albums/",
                includesFileName = false,
                prefix = "/Music",
            ),
        )
        assertTrue(
            folderMatchesPrefix(
                rawPath = "/storage/emulated/0/Music/song.flac",
                includesFileName = true,
                prefix = "/storage/emulated/0/Music",
            ),
        )
        assertFalse(
            folderMatchesPrefix(
                rawPath = "Podcasts/",
                includesFileName = false,
                prefix = "/Music",
            ),
        )
    }

    @Test
    fun lyricClockAdoptsControllerUpdatesWithoutKeepingAVisibleLag() {
        assertEquals(10_500.0, correctedLyricClockMs(10_000.0, 10_500L), 0.0)
        assertEquals(10_037.5, correctedLyricClockMs(10_000.0, 10_050L), 0.0)
    }

    @Test
    fun onlyExplicitUnchangedScansRequestNoChangesFeedback() {
        assertTrue(
            shouldEmitScanNoChanges(
                libraryChanged = false,
                notifyIfUnchanged = true,
            ),
        )
        assertFalse(
            shouldEmitScanNoChanges(
                libraryChanged = true,
                notifyIfUnchanged = true,
            ),
        )
        assertFalse(
            shouldEmitScanNoChanges(
                libraryChanged = false,
                notifyIfUnchanged = false,
            ),
        )
    }

    @Test
    fun sidecarCandidatesRequireTheExactAudioFileStem() {
        assertEquals(
            listOf(
                "Song Name.ttml" to LyricsFormat.TTML,
                "Song Name.lrc" to LyricsFormat.LRC,
            ),
            exactLyricsSidecarCandidates("Song Name.flac"),
        )
        assertFalse(
            exactLyricsSidecarCandidates("Song Name.flac")
                .any { (name, _) -> name == "Song Name (1).lrc" },
        )
    }
}
