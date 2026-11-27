package com.melox.player.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.melox.player.model.PlaybackMode
import com.melox.player.model.PlaybackQueueItem
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackArtworkMetadataTest {
    @Test
    fun artistPictureDoesNotSatisfyPlaybackCoverFallback() {
        val metadata = MediaMetadata.Builder()
            .setArtworkData(byteArrayOf(1), MediaMetadata.PICTURE_TYPE_ARTIST_PERFORMER)
            .build()
        assertFalse(metadata.hasCoverArtwork())
        assertTrue(
            metadata.buildUpon()
                .setArtworkData(byteArrayOf(2), MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                .build()
                .hasCoverArtwork(),
        )
    }

    @Test
    fun withArtworkData_preservesTrackMetadataAndAddsFrontCover() {
        val original = MediaItem.Builder()
            .setMediaId("track-1")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Title")
                    .setArtist("Artist")
                    .build(),
            )
            .build()
        val artworkData = byteArrayOf(1, 2, 3)

        val updated = original.withArtworkData(artworkData)

        assertEquals("track-1", updated.mediaId)
        assertEquals("Title", updated.mediaMetadata.title)
        assertEquals("Artist", updated.mediaMetadata.artist)
        assertArrayEquals(artworkData, updated.mediaMetadata.artworkData)
        assertEquals(
            MediaMetadata.PICTURE_TYPE_FRONT_COVER,
            updated.mediaMetadata.artworkDataType,
        )
    }

    @Test
    fun onlyMissingArtworkOrAPlaceholderFromAnotherThemeNeedsAnUpdate() {
        for (dark in listOf(false, true)) {
            assertTrue(needsPlaybackArtwork(!dark, dark, hasCoverArtwork = true))
            assertFalse(needsPlaybackArtwork(dark, dark, hasCoverArtwork = true))
            assertTrue(needsPlaybackArtwork(null, dark, hasCoverArtwork = false))
            assertFalse(needsPlaybackArtwork(null, dark, hasCoverArtwork = true))
        }
    }

    @Test
    fun playbackArtworkKey_changesWhenArtworkSourceChanges() {
        val item = PlaybackQueueItem(
            mediaId = "track-1",
            trackId = 1L,
            contentUri = "content://media/1",
            title = "Title",
            artist = null,
            album = null,
            durationMs = 1_000L,
            dateModifiedEpochSeconds = 10L,
            fileSizeBytes = 20L,
            sourceOrder = 0.0,
            playbackMode = PlaybackMode.ORDER,
        )

        assertEquals(
            PlaybackArtworkKey(
                contentUri = "content://media/1",
                dateModifiedEpochSeconds = 10L,
                fileSizeBytes = 20L,
            ),
            item.toPlaybackArtworkKey(),
        )
        assertEquals(
            11L,
            item.copy(dateModifiedEpochSeconds = 11L)
                .toPlaybackArtworkKey()
                .dateModifiedEpochSeconds,
        )
    }
}
