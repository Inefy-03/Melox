package com.melox.player

import com.melox.player.data.playback.PlaybackSnapshotCodec
import com.melox.player.data.playback.retainReadableItems
import com.melox.player.data.playback.toStartupPlaybackPreview
import com.melox.player.model.PlaybackMode
import com.melox.player.model.PlaybackQueueItem
import com.melox.player.model.PlaybackSnapshot
import com.melox.player.model.PlaybackUiState
import com.melox.player.model.withTrackMetadata
import com.melox.player.playback.displayedPlaybackMode
import com.melox.player.playback.hasSameQueueSlots
import com.melox.player.playback.isValidQueueIndex
import com.melox.player.playback.isValidQueueMove
import com.melox.player.playback.nextPlaybackMode
import com.melox.player.playback.nextQueueInsertionIndex
import com.melox.player.playback.playbackQueueReplacement
import com.melox.player.playback.reconcileValidatedPlaybackSnapshot
import com.melox.player.playback.reorderQueueForPlaybackMode
import com.melox.player.playback.sourceOrderForPlayNext
import com.melox.player.playback.toInitialPlaybackState
import com.melox.player.ui.screen.playback.moveQueueListItem
import com.melox.player.ui.screen.playback.queueMoveTargetAfterCurrent
import com.melox.player.ui.screen.playback.queueOffscreenExitAlpha
import com.melox.player.ui.screen.playback.queueOffscreenExitTranslation
import com.melox.player.ui.screen.playback.queuePlayNextPeerTranslation
import com.melox.player.ui.screen.playback.queueShowsPlayNextAction
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.zip.CRC32
import java.util.zip.CheckedOutputStream
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackQueueLogicTest {
    @Test
    fun refreshedLibraryMetadataUpdatesPlaybackUiWithoutChangingQueueIdentity() {
        val queueItem = playbackQueueItem("track-7").copy(
            trackId = 7L,
            title = "Old title",
            artist = "Old artist",
            album = "Old album",
            durationMs = 1_000L,
            dateModifiedEpochSeconds = 10L,
            fileSizeBytes = 20L,
        )
        val refreshedTrack = musicTrack(
            id = 7L,
            title = "New title",
            dateAddedEpochSeconds = 1L,
            fileName = "new.flac",
            fileSizeBytes = 40L,
            durationMs = 2_000L,
            dateModifiedEpochSeconds = 30L,
        ).copy(
            artist = "New artist",
            album = "New album",
        )

        val updated = PlaybackUiState(
            queue = listOf(queueItem),
            currentIndex = 0,
            positionMs = 500L,
            isPlaying = true,
        ).withTrackMetadata(listOf(refreshedTrack))

        assertEquals(queueItem.mediaId, updated.currentItem?.mediaId)
        assertEquals(queueItem.contentUri, updated.currentItem?.contentUri)
        assertEquals("New title", updated.currentItem?.title)
        assertEquals("New artist", updated.currentItem?.artist)
        assertEquals("New album", updated.currentItem?.album)
        assertEquals(2_000L, updated.currentItem?.durationMs)
        assertEquals(30L, updated.currentItem?.dateModifiedEpochSeconds)
        assertEquals(40L, updated.currentItem?.fileSizeBytes)
        assertEquals(500L, updated.positionMs)
        assertTrue(updated.isPlaying)
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
    fun pendingPlaybackModeKeepsTheNewIconUntilThePlayerConfirmsIt() {
        assertEquals(
            PlaybackMode.RANDOM,
            displayedPlaybackMode(
                playerMode = PlaybackMode.REPEAT_ONE,
                pendingMode = PlaybackMode.RANDOM,
            ),
        )
        assertEquals(
            PlaybackMode.ORDER,
            displayedPlaybackMode(
                playerMode = PlaybackMode.ORDER,
                pendingMode = null,
            ),
        )
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
        val queue = List(24) { index -> playbackQueueItem(index.toString()) }
        val item = queue[12].copy(
            durationMs = 123_000L,
            playbackMode = PlaybackMode.RANDOM,
        )
        val snapshotQueue = queue.toMutableList().also { items -> items[12] = item }

        val preview = PlaybackSnapshot(
            queue = snapshotQueue,
            currentIndex = 12,
            positionMs = 4_000L,
            playbackMode = PlaybackMode.RANDOM,
        ).toStartupPlaybackPreview(maxItemCount = 10)

        requireNotNull(preview)
        val state = preview.toInitialPlaybackState()

        assertEquals(item, state.currentItem)
        assertEquals(10, state.queue.size)
        assertEquals(0, state.currentIndex)
        assertEquals(4_000L, state.positionMs)
        assertEquals(PlaybackMode.RANDOM, state.playbackMode)
        assertFalse(state.isPlaying)
    }

    @Test
    fun startupPlaybackPreviewUsesTheLastFullWindowNearQueueEnd() {
        val queue = List(24) { index -> playbackQueueItem(index.toString()) }

        val preview = PlaybackSnapshot(
            queue = queue,
            currentIndex = 22,
            positionMs = 0L,
            playbackMode = PlaybackMode.ORDER,
        ).toStartupPlaybackPreview(maxItemCount = 10)

        requireNotNull(preview)
        assertEquals(queue.subList(14, 24), preview.queue)
        assertEquals(8, preview.currentIndex)
    }

    @Test
    fun stagedPlaybackValidationPreservesTheActiveQueueSlotAndPosition() {
        val first = playbackQueueItem("duplicate").copy(sourceOrder = 0.0)
        val second = playbackQueueItem("duplicate").copy(sourceOrder = 1.0)
        val third = playbackQueueItem("third").copy(sourceOrder = 2.0)
        val restored = PlaybackSnapshot(
            queue = listOf(first, second, third),
            currentIndex = 0,
            positionMs = 1_000L,
            playbackMode = PlaybackMode.ORDER,
        )
        val validated = restored.copy(queue = listOf(second, third))

        val reconciled = reconcileValidatedPlaybackSnapshot(
            restoredSnapshot = restored,
            validatedSnapshot = validated,
            currentQueue = restored.queue,
            currentIndex = 1,
            positionMs = 8_000L,
            playbackMode = PlaybackMode.REPEAT_ONE,
        )

        requireNotNull(reconciled)
        assertEquals(
            listOf(second, third).map { item ->
                item.copy(playbackMode = PlaybackMode.REPEAT_ONE)
            },
            reconciled.queue,
        )
        assertEquals(0, reconciled.currentIndex)
        assertEquals(8_000L, reconciled.positionMs)
        assertEquals(PlaybackMode.REPEAT_ONE, reconciled.playbackMode)
    }

    @Test
    fun stagedPlaybackValidationDoesNotReplaceAUserMutatedQueue() {
        val restored = PlaybackSnapshot(
            queue = listOf(playbackQueueItem("one"), playbackQueueItem("two")),
            currentIndex = 0,
            positionMs = 0L,
            playbackMode = PlaybackMode.ORDER,
        )

        assertNull(
            reconcileValidatedPlaybackSnapshot(
                restoredSnapshot = restored,
                validatedSnapshot = restored,
                currentQueue = restored.queue.dropLast(1),
                currentIndex = 0,
                positionMs = 0L,
                playbackMode = PlaybackMode.ORDER,
            ),
        )
        assertTrue(hasSameQueueSlots(restored.queue, restored.queue))
        assertFalse(hasSameQueueSlots(restored.queue, restored.queue.reversed()))
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

        assertEquals(true, isValidQueueMove(fromIndex = 0, toIndex = 2, itemCount = 3))
        assertFalse(isValidQueueMove(fromIndex = 1, toIndex = 1, itemCount = 3))
        assertFalse(isValidQueueMove(fromIndex = -1, toIndex = 1, itemCount = 3))
        assertFalse(isValidQueueMove(fromIndex = 1, toIndex = 3, itemCount = 3))
    }

    @Test
    fun queueDraftMoveAndPlayNextTargetPreserveSlotIdentity() {
        val queue = listOf("a", "b", "current", "d", "e")

        assertEquals(
            listOf("b", "current", "a", "d", "e"),
            moveQueueListItem(queue, fromIndex = 0, toIndex = 2),
        )
        assertEquals(
            2,
            queueMoveTargetAfterCurrent(itemIndex = 0, currentIndex = 2, itemCount = 5),
        )
        assertEquals(
            3,
            queueMoveTargetAfterCurrent(itemIndex = 4, currentIndex = 2, itemCount = 5),
        )
        assertNull(
            queueMoveTargetAfterCurrent(itemIndex = 2, currentIndex = 2, itemCount = 5),
        )
        assertEquals("current", moveQueueListItem(queue, 2, 4)[4])
    }

    @Test
    fun queuePlayNextActionSkipsCurrentAndImmediateNextItems() {
        assertFalse(queueShowsPlayNextAction(itemIndex = 2, currentIndex = 2, itemCount = 5))
        assertFalse(queueShowsPlayNextAction(itemIndex = 3, currentIndex = 2, itemCount = 5))
        assertTrue(queueShowsPlayNextAction(itemIndex = 1, currentIndex = 2, itemCount = 5))
        assertTrue(queueShowsPlayNextAction(itemIndex = 4, currentIndex = 2, itemCount = 5))
        assertFalse(queueShowsPlayNextAction(itemIndex = 0, currentIndex = -1, itemCount = 5))
    }

    @Test
    fun queueOffscreenPlayNextAnimationUsesTheRequestedViewportEdge() {
        assertEquals(
            -308f,
            queueOffscreenExitTranslation(
                itemIndex = 4,
                currentIndex = 1,
                itemOffsetPx = 240,
                itemSizePx = 68,
                viewportStartPx = 0,
                viewportEndPx = 600,
            ),
            0f,
        )
        assertEquals(
            480f,
            queueOffscreenExitTranslation(
                itemIndex = 1,
                currentIndex = 4,
                itemOffsetPx = 120,
                itemSizePx = 68,
                viewportStartPx = 0,
                viewportEndPx = 600,
            ),
            0f,
        )
    }

    @Test
    fun queueOffscreenPlayNextFadesOnlyAcrossTheViewportBoundary() {
        assertEquals(
            1f,
            queueOffscreenExitAlpha(
                itemIndex = 4,
                currentIndex = 1,
                itemOffsetPx = 240,
                itemSizePx = 68,
                translationY = -240f,
                viewportStartPx = 0,
                viewportEndPx = 600,
            ),
            0f,
        )
        assertEquals(
            0.5f,
            queueOffscreenExitAlpha(
                itemIndex = 4,
                currentIndex = 1,
                itemOffsetPx = 240,
                itemSizePx = 68,
                translationY = -274f,
                viewportStartPx = 0,
                viewportEndPx = 600,
            ),
            0f,
        )
        assertEquals(
            0f,
            queueOffscreenExitAlpha(
                itemIndex = 1,
                currentIndex = 4,
                itemOffsetPx = 120,
                itemSizePx = 68,
                translationY = 480f,
                viewportStartPx = 0,
                viewportEndPx = 600,
            ),
            0f,
        )
    }

    @Test
    fun queueOffscreenPlayNextMovesCrossedRowsOutOfTheGap() {
        assertEquals(
            34f,
            queuePlayNextPeerTranslation(
                itemIndex = 3,
                movingIndex = 5,
                currentIndex = 1,
                rowHeightPx = 68f,
                progress = 0.5f,
            ),
            0f,
        )
        assertEquals(
            -68f,
            queuePlayNextPeerTranslation(
                itemIndex = 2,
                movingIndex = 1,
                currentIndex = 4,
                rowHeightPx = 68f,
                progress = 1f,
            ),
            0f,
        )
        assertEquals(
            0f,
            queuePlayNextPeerTranslation(
                itemIndex = 5,
                movingIndex = 5,
                currentIndex = 1,
                rowHeightPx = 68f,
                progress = 1f,
            ),
            0f,
        )
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
