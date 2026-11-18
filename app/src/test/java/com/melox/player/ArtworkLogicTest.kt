package com.melox.player

import com.melox.player.ui.component.library.artworkCacheFileStem
import com.melox.player.ui.component.library.createArtworkCacheKey
import com.melox.player.ui.component.library.fitArtworkDimensions
import com.melox.player.ui.component.library.fullPlayerArtworkTargetSizePx
import com.melox.player.ui.component.library.snapshotArtworkDiskCacheEntries
import com.melox.player.ui.component.playback.DYNAMIC_FLOW_ARTWORK_SATURATION
import com.melox.player.ui.component.playback.DYNAMIC_FLOW_BACKGROUND_DARKEN_AMOUNT
import com.melox.player.ui.component.playback.KenBurnsFrame
import com.melox.player.ui.component.playback.advanceDynamicFlowClockMillis
import com.melox.player.ui.component.playback.artworkCrossfadeDurationMillis
import com.melox.player.ui.component.playback.createRenderScriptBlurBoxSizes
import com.melox.player.ui.component.playback.dynamicFlowDownsampleFactor
import com.melox.player.ui.component.playback.interpolateKenBurnsFrame
import com.melox.player.ui.component.playback.scaledDynamicFlowTimeMs
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkLogicTest {
    @Test
    fun dynamicFlowUsesConfiguredVisualValuesAndAdaptiveDownsampling() {
        assertEquals(2.5f, DYNAMIC_FLOW_ARTWORK_SATURATION, 0f)
        assertEquals(0.25f, DYNAMIC_FLOW_BACKGROUND_DARKEN_AMOUNT, 0f)
        assertEquals(16f, dynamicFlowDownsampleFactor(419), 0f)
        assertEquals(20f, dynamicFlowDownsampleFactor(420), 0f)
        assertEquals(10_000L, scaledDynamicFlowTimeMs(10_000L, 10))
    }

    @Test
    fun dynamicFlowClockStartsAndResumesWithoutJumpingToAbsoluteFrameTime() {
        val absoluteFrameNanos = 987_654_321_000_000L
        val initial = advanceDynamicFlowClockMillis(
            elapsedMillis = 0L,
            previousFrameNanos = null,
            frameNanos = absoluteFrameNanos,
        )
        val advanced = advanceDynamicFlowClockMillis(
            elapsedMillis = initial,
            previousFrameNanos = absoluteFrameNanos,
            frameNanos = absoluteFrameNanos + 42_000_000L,
        )
        val resumed = advanceDynamicFlowClockMillis(
            elapsedMillis = advanced,
            previousFrameNanos = null,
            frameNanos = absoluteFrameNanos + 5_000_000_000L,
        )

        assertEquals(0L, initial)
        assertEquals(42L, advanced)
        assertEquals(42L, resumed)
    }

    @Test
    fun playbackBlurUsesThreeOddRenderScriptCalibratedBoxes() {
        val boxSizes = createRenderScriptBlurBoxSizes(radius = 25)

        assertEquals(3, boxSizes.size)
        assertTrue(boxSizes.all { size -> size > 0 && size % 2 == 1 })
        assertArrayEquals(intArrayOf(21, 21, 21), boxSizes)
    }

    @Test
    fun fullPlayerArtworkResolutionFollowsDisplayNeedAndMemoryBudget() {
        val largeHeap = 3L * 1024L * 1024L * 1024L

        assertEquals(1800, fullPlayerArtworkTargetSizePx(1800, largeHeap))
        assertEquals(3999, fullPlayerArtworkTargetSizePx(3999, largeHeap))
        assertEquals(4000, fullPlayerArtworkTargetSizePx(4000, largeHeap))
        assertEquals(4001, fullPlayerArtworkTargetSizePx(4001, largeHeap))
        assertEquals(8000, fullPlayerArtworkTargetSizePx(9000, largeHeap))
        assertEquals(
            4096,
            fullPlayerArtworkTargetSizePx(
                displayedSizePx = 8000,
                maxMemoryBytes = 512L * 1024L * 1024L,
            ),
        )
    }

    @Test
    fun fullPlayerArtworkKeepsTrackTransitionAndShortensResolutionUpgrade() {
        assertEquals(
            500,
            artworkCrossfadeDurationMillis("track-a", "track-b", 500, 100),
        )
        assertEquals(
            100,
            artworkCrossfadeDurationMillis("track-a", "track-a", 500, 100),
        )
    }

    @Test
    fun kenBurnsFrameInterpolationPreservesEndpoints() {
        val start = KenBurnsFrame(
            scale = 1.08f,
            horizontalBias = -1f,
            verticalBias = 0.5f,
        )
        val end = KenBurnsFrame(
            scale = 1.2f,
            horizontalBias = 1f,
            verticalBias = -0.5f,
        )

        assertEquals(start, interpolateKenBurnsFrame(start, end, 0f))
        assertEquals(end, interpolateKenBurnsFrame(start, end, 1f))
        val midpoint = interpolateKenBurnsFrame(start, end, 0.5f)
        assertEquals(1.14f, midpoint.scale, 0.000001f)
        assertEquals(0f, midpoint.horizontalBias, 0.000001f)
        assertEquals(0f, midpoint.verticalBias, 0.000001f)
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
