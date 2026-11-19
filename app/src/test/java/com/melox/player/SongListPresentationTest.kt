package com.melox.player

import com.melox.player.model.MusicTrack
import com.melox.player.data.library.songListTitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SongListPresentationTest {
    @Test
    fun filenameModePreservesTheCompleteNameIncludingExtension() {
        for (fileName in listOf("01. Song.live.FLAC", "歌曲.m4a", "track.with.dots.mp3")) {
            assertEquals(fileName, track(fileName = fileName).songListTitle(true))
        }
    }

    @Test
    fun metadataModeKeepsTheOriginalTitleEvenWhenAFilenameIsAvailable() {
        assertEquals("Metadata title", track(fileName = "File name.flac").songListTitle(false))
        assertNull(track(fileName = "File name.flac", title = null).songListTitle(false))
    }

    @Test
    fun missingOrBlankFilenameFallsBackToMetadataAndLeavesUnknownTitlesLocalizable() {
        for (fileName in listOf(null, "", "  ")) {
            assertEquals("Metadata title", track(fileName = fileName).songListTitle(true))
            assertNull(track(fileName = fileName, title = null).songListTitle(true))
        }
    }

    private fun track(fileName: String?, title: String? = "Metadata title") = MusicTrack(
        id = 1L,
        title = title,
        artist = "Artist",
        album = "Album",
        durationMs = 1_000L,
        dateAddedEpochSeconds = 0L,
        dateModifiedEpochSeconds = 0L,
        fileName = fileName,
        fileSizeBytes = 1L,
        contentUri = "content://media/external/audio/media/1",
        titleSectionKey = "M",
        titleSortKey = "METADATA TITLE",
    )
}
