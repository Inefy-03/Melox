package com.melox.player

import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.melox.player.data.repository.customBackgroundBlurRadius
import com.melox.player.data.repository.customBackgroundBlurInputMaxEdge
import com.melox.player.data.repository.fitCustomBackgroundDimensions
import com.melox.player.data.repository.isCustomBackgroundId
import com.melox.player.model.AppSettings
import com.melox.player.model.MIN_CUSTOM_BACKGROUND_CARD_OPACITY_PERCENT
import com.melox.player.model.MAX_CUSTOM_BACKGROUND_CARD_OPACITY_PERCENT
import com.melox.player.model.MAX_CUSTOM_BACKGROUND_DIM_PERCENT
import com.melox.player.model.normalizeCustomBackgroundBlurPercent
import com.melox.player.model.normalizeCustomBackgroundDimPercent
import com.melox.player.model.normalizeCustomBackgroundCardBlurPercent
import com.melox.player.model.normalizeCustomBackgroundCardOpacityPercent
import com.melox.player.model.resolveCustomBackgroundBlurPercent
import com.melox.player.ui.component.pageSurfaceBlurEnabled
import com.melox.player.ui.component.customBackgroundCropSize
import com.melox.player.ui.component.customBackgroundCropOffset
import com.melox.player.ui.component.wallpaperTopBarVisible
import com.melox.player.ui.component.topBarContainerColor
import com.melox.player.ui.component.tabSelectedContainerColor
import com.melox.player.ui.component.pageCardBlurRadius
import com.melox.player.ui.component.pageCardSurfaceAlpha
import com.melox.player.ui.component.pageCardBackgroundColor
import com.melox.player.ui.component.NormalBarBlurRadius
import com.melox.player.ui.screen.playback.sliderValueAtPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomBackgroundTest {
    @Test
    fun cardOpacityDefaultsToTheExistingMaterialAlpha() {
        assertEquals(80, AppSettings().customBackgroundCardOpacityPercent)
        assertEquals(0.8f, pageCardSurfaceAlpha(AppSettings().customBackgroundCardOpacityPercent), 0f)
    }

    @Test
    fun cardOpacityUsesTheOfficialAlphaDirectionAndClampsBounds() {
        assertEquals(10, normalizeCustomBackgroundCardOpacityPercent(Int.MIN_VALUE))
        assertEquals(10, normalizeCustomBackgroundCardOpacityPercent(0))
        assertEquals(10, normalizeCustomBackgroundCardOpacityPercent(9))
        assertEquals(10, normalizeCustomBackgroundCardOpacityPercent(10))
        assertEquals(80, normalizeCustomBackgroundCardOpacityPercent(Int.MAX_VALUE))
        assertEquals(80, normalizeCustomBackgroundCardOpacityPercent(81))
        assertEquals(80, normalizeCustomBackgroundCardOpacityPercent(100))
        assertEquals(0.1f, pageCardSurfaceAlpha(Int.MIN_VALUE), 0f)
        assertEquals(0.8f, pageCardSurfaceAlpha(Int.MAX_VALUE), 0f)
        for (percent in MIN_CUSTOM_BACKGROUND_CARD_OPACITY_PERCENT..MAX_CUSTOM_BACKGROUND_CARD_OPACITY_PERCENT) {
            assertEquals(percent / 100f, pageCardSurfaceAlpha(percent), 0f)
        }
        val alphas = (MIN_CUSTOM_BACKGROUND_CARD_OPACITY_PERCENT..MAX_CUSTOM_BACKGROUND_CARD_OPACITY_PERCENT).map(::pageCardSurfaceAlpha)
        assertTrue(alphas.zipWithNext().all { (first, next) -> first < next })
    }

    @Test
    fun cardOpacitySliderSupportsTapsAtEveryPercentAndRtlDirection() {
        for (percent in MIN_CUSTOM_BACKGROUND_CARD_OPACITY_PERCENT..MAX_CUSTOM_BACKGROUND_CARD_OPACITY_PERCENT) {
            for (rtl in listOf(false, true)) {
                val minimum = MIN_CUSTOM_BACKGROUND_CARD_OPACITY_PERCENT
                val maximum = MAX_CUSTOM_BACKGROUND_CARD_OPACITY_PERCENT
                val rangeLength = maximum - minimum
                val position = 10f + (if (rtl) maximum - percent else percent - minimum) * 2f
                assertEquals(
                    percent.toFloat(),
                    sliderValueAtPosition(position, rangeLength * 2 + 20, 20, minimum.toFloat()..maximum.toFloat(), rangeLength - 1, null, 0.02f, rtl),
                    0.001f,
                )
            }
        }
    }

    @Test
    fun nativeWallpaperCardsApplyEveryOpacityWithoutABlurBackdrop() {
        val base = Color(0xFF7457AA)
        for (percent in 0..100) {
            val result = pageCardBackgroundColor(base, true, false, pageCardSurfaceAlpha(percent))
            assertEquals(percent.coerceIn(10, 80) / 100f, result.alpha, 0.005f)
            assertEquals(base.red, result.red, 0f)
            assertEquals(base.green, result.green, 0f)
            assertEquals(base.blue, result.blue, 0f)
        }
    }

    @Test
    fun nativeCardFillPreservesOriginalPartialAlphaAndClampsInput() {
        val base = Color(0x807457AA)
        assertEquals(base.alpha * 0.8f, pageCardBackgroundColor(base, true, false, 0.8f).alpha, 0.005f)
        assertEquals(0f, pageCardBackgroundColor(base, true, false, -1f).alpha, 0f)
        assertEquals(base, pageCardBackgroundColor(base, true, false, 2f))
    }

    @Test
    fun cardsWithoutWallpaperKeepTheirOriginalFill() {
        for (base in listOf(Color.White, Color.Black, Color(0x807457AA))) {
            for (alpha in listOf(0f, 0.4f, 0.8f)) {
                assertEquals(base, pageCardBackgroundColor(base, false, false, alpha))
            }
        }
    }

    @Test
    fun originallyTransparentCardsNeverGainAFill() {
        val transparent = Color(0x007457AA)
        for (hasWallpaper in listOf(false, true)) {
            for (hasBackdrop in listOf(false, true)) {
                assertEquals(transparent, pageCardBackgroundColor(transparent, hasWallpaper, hasBackdrop, 0.8f))
            }
        }
    }

    @Test
    fun blurredWallpaperCardsLeaveTheFillToTheOfficialBackdrop() {
        assertEquals(Color.Transparent, pageCardBackgroundColor(Color.White, true, true, 0.8f))
        assertEquals(Color.Transparent, pageCardBackgroundColor(Color.Black, true, true, 0f))
    }

    @Test
    fun wallpaperTabsNeverRestoreTheOrdinaryBarFillWhenBlurIsDisabled() {
        for (hasImage in listOf(false, true)) {
            for (hasBackdrop in listOf(false, true)) {
                val expected = if (hasImage || hasBackdrop) Color.Transparent else Color.White
                assertEquals(expected, topBarContainerColor(hasImage, hasBackdrop, Color.White))
            }
        }
    }

    @Test
    fun wallpaperSelectedTabsKeepTheThemeHueWithProgressiveTabOpacity() {
        val base = Color(0xFF7457AA)
        for (hasImage in listOf(false, true)) {
            for (progressive in listOf(false, true)) {
                val selected = tabSelectedContainerColor(hasImage, progressive, base)
                assertEquals(if (hasImage || progressive) 0.8f else 1f, selected.alpha, 0.005f)
                assertEquals(base.red, selected.red, 0.001f)
                assertEquals(base.green, selected.green, 0.001f)
                assertEquals(base.blue, selected.blue, 0.001f)
            }
        }
    }

    @Test
    fun cardBlurDefaultsToTheExistingRadiusAndHasBoundedMonotonicStrength() {
        assertEquals(50, AppSettings().customBackgroundCardBlurPercent)
        assertEquals(NormalBarBlurRadius, pageCardBlurRadius(AppSettings().customBackgroundCardBlurPercent), 0f)
        assertEquals(0, normalizeCustomBackgroundCardBlurPercent(Int.MIN_VALUE))
        assertEquals(100, normalizeCustomBackgroundCardBlurPercent(Int.MAX_VALUE))
        assertEquals(0f, pageCardBlurRadius(Int.MIN_VALUE), 0f)
        assertEquals(2f * NormalBarBlurRadius, pageCardBlurRadius(Int.MAX_VALUE), 0f)
        val radii = (0..100).map(::pageCardBlurRadius)
        assertTrue(radii.zipWithNext().all { (first, next) -> first < next })
        assertEquals(0, AppSettings().customBackgroundBlurPercent)
    }

    @Test
    fun cardBlurSliderTapsCoverEveryPercentAndKeepRtlDirection() {
        for (percent in 0..100) {
            for (rtl in listOf(false, true)) {
                val position = 10f + (if (rtl) 100 - percent else percent) * 2f
                assertEquals(
                    percent.toFloat(),
                    sliderValueAtPosition(position, 220, 20, 0f..100f, 99, null, 0.02f, rtl),
                    0.001f,
                )
            }
        }
    }

    @Test
    fun pageMaterialRequiresAnImageEnabledBlurAndRuntimeSupport() {
        for (hasImage in listOf(false, true)) {
            for (blurEnabled in listOf(false, true)) {
                for (supported in listOf(false, true)) {
                    assertEquals(hasImage && blurEnabled && supported, pageSurfaceBlurEnabled(hasImage, blurEnabled, supported))
                }
            }
        }
    }

    @Test
    fun normalTopBarsShowWallpaperButActiveProgressiveBlurIsPreserved() {
        for (hasImage in listOf(false, true)) {
            for (blurEnabled in listOf(false, true)) {
                for (progressive in listOf(false, true)) {
                    for (supported in listOf(false, true)) {
                        assertEquals(
                            hasImage && !(blurEnabled && progressive && supported),
                            wallpaperTopBarVisible(hasImage, blurEnabled, progressive, supported),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun wallpaperBarColorStaysTransparentDuringBlurModeAndBackdropHandoff() {
        for (blurEnabled in listOf(true, false, true)) {
            for (progressive in listOf(true, false, true)) {
                for (backdropAvailable in listOf(true, false, true)) {
                    assertEquals(Color.Transparent, topBarContainerColor(true, backdropAvailable, Color.White))
                    assertEquals(
                        !(blurEnabled && progressive && backdropAvailable),
                        wallpaperTopBarVisible(true, blurEnabled, progressive, backdropAvailable),
                    )
                }
            }
        }
    }

    @Test
    fun barsWithoutWallpaperKeepTheirOriginalOpaqueFallback() {
        assertEquals(Color.White, topBarContainerColor(false, false, Color.White))
        assertEquals(Color.Black, topBarContainerColor(false, false, Color.Black))
        assertEquals(Color.Transparent, topBarContainerColor(false, true, Color.White))
    }

    @Test
    fun wallpaperDimmingStartsClearAndStopsAtNinetyPercent() {
        assertEquals(0, AppSettings().customBackgroundDimPercent)
        assertEquals(0, normalizeCustomBackgroundDimPercent(Int.MIN_VALUE))
        assertEquals(50, normalizeCustomBackgroundDimPercent(50))
        assertEquals(90, normalizeCustomBackgroundDimPercent(Int.MAX_VALUE))
        assertEquals(90, normalizeCustomBackgroundDimPercent(91))
        assertEquals(90, normalizeCustomBackgroundDimPercent(100))
    }

    @Test
    fun tappingDimmingSelectsEveryWholePercentIncludingZero() {
        for (percent in 0..MAX_CUSTOM_BACKGROUND_DIM_PERCENT) {
            val maximum = MAX_CUSTOM_BACKGROUND_DIM_PERCENT
            assertEquals(
                percent.toFloat(),
                sliderValueAtPosition(percent * 2f + 10f, maximum * 2 + 20, 20, 0f..maximum.toFloat(), maximum - 1, null, 0.02f, false),
                0.001f,
            )
        }
    }

    @Test
    fun weakBlurRetainsHighResolutionWhileEffectiveStrengthIncreases() {
        val edges = (1..100).map(::customBackgroundBlurInputMaxEdge)
        assertTrue(edges.first() >= 2000)
        assertEquals(256, edges.last())
        assertTrue(edges.all { it in 256..2048 })
        assertTrue(edges.zipWithNext().all { (first, next) -> first >= next })
        val strengths = (1..100).map { customBackgroundBlurRadius(it).toFloat() / customBackgroundBlurInputMaxEdge(it) }
        assertTrue(strengths.zipWithNext().all { (first, next) -> first <= next })
        assertEquals(2048, customBackgroundBlurInputMaxEdge(Int.MIN_VALUE))
        assertEquals(edges.last(), customBackgroundBlurInputMaxEdge(Int.MAX_VALUE))
    }

    @Test
    fun tappingTheBlurSliderSelectsEveryWholePercentIncludingBounds() {
        for (percent in 0..100) {
            val position = 10f + percent * 2f
            assertEquals(
                percent.toFloat(),
                sliderValueAtPosition(
                    positionX = position,
                    width = 220,
                    height = 20,
                    valueRange = 0f..100f,
                    steps = 99,
                    keyPoints = null,
                    magnetThreshold = 0.02f,
                    reverseDirection = false,
                ),
                0.001f,
            )
        }
    }

    @Test
    fun tappingTheBlurSliderHonorsRtlAndClampsOutsideTheTrack() {
        for ((position, percent) in listOf(-100f to 100f, 110f to 50f, 400f to 0f)) {
            assertEquals(
                percent,
                sliderValueAtPosition(
                    positionX = position,
                    width = 220,
                    height = 20,
                    valueRange = 0f..100f,
                    steps = 99,
                    keyPoints = null,
                    magnetThreshold = 0.02f,
                    reverseDirection = true,
                ),
                0.001f,
            )
        }
    }

    @Test
    fun defaultsPreserveTheOriginalBackgroundAndStartAtZeroPercent() {
        val settings = AppSettings()
        assertEquals(null, settings.customBackgroundId)
        assertEquals(0, settings.customBackgroundBlurPercent)
    }

    @Test
    fun blurPercentIsBoundedToZeroThroughOneHundred() {
        assertEquals(0, normalizeCustomBackgroundBlurPercent(Int.MIN_VALUE))
        assertEquals(50, normalizeCustomBackgroundBlurPercent(50))
        assertEquals(100, normalizeCustomBackgroundBlurPercent(Int.MAX_VALUE))
    }

    @Test
    fun legacySwitchRetainsTheEffectiveBlurAndNewPercentagesNeedNoSwitch() {
        assertEquals(0, resolveCustomBackgroundBlurPercent(null, null))
        assertEquals(0, resolveCustomBackgroundBlurPercent(75, false))
        assertEquals(75, resolveCustomBackgroundBlurPercent(75, true))
        assertEquals(50, resolveCustomBackgroundBlurPercent(null, true))
        assertEquals(0, resolveCustomBackgroundBlurPercent(0, null))
        assertEquals(75, resolveCustomBackgroundBlurPercent(75, null))
        assertEquals(100, resolveCustomBackgroundBlurPercent(Int.MAX_VALUE, null))
    }

    @Test
    fun largeImportsPreserveAspectRatioWithinTheDecodeLimit() {
        assertEquals(2048 to 1024, fitCustomBackgroundDimensions(8000, 4000))
        assertEquals(1024 to 2048, fitCustomBackgroundDimensions(4000, 8000))
        assertEquals(2048 to 1, fitCustomBackgroundDimensions(Int.MAX_VALUE, 1))
    }

    @Test
    fun smallImportsAreNotUpscaled() {
        assertEquals(320 to 240, fitCustomBackgroundDimensions(320, 240))
        assertEquals(256 to 192, fitCustomBackgroundDimensions(320, 240, 256))
    }

    @Test
    fun onlyGeneratedImageIdentifiersCanResolveToPrivateFiles() {
        assertTrue(isCustomBackgroundId("8a2d0cbb-b8c0-4508-8e76-01a23ebdd7f6"))
        assertFalse(isCustomBackgroundId("../../outside"))
        assertFalse(isCustomBackgroundId("/tmp/outside.png"))
        assertFalse(isCustomBackgroundId(""))
    }

    @Test
    fun blurStrengthIsMonotonicAndWithinToolkitBounds() {
        val radii = (1..100).map(::customBackgroundBlurRadius)
        assertTrue(radii.all { it in 1..25 })
        assertTrue(radii.zipWithNext().all { (first, next) -> first <= next })
        assertEquals(13, customBackgroundBlurRadius(50))
        assertEquals(25, customBackgroundBlurRadius(100))
        assertEquals(0, customBackgroundBlurRadius(0))
        assertEquals(0, customBackgroundBlurRadius(Int.MIN_VALUE))
    }

    @Test
    fun pageCropFillsPortraitAndLandscapeWithoutStretching() {
        assertEquals(IntSize(1200, 600), customBackgroundCropSize(800, 400, 300f, 600f))
        assertEquals(IntSize(600, 1200), customBackgroundCropSize(400, 800, 600f, 300f))
        assertEquals(IntSize.Zero, customBackgroundCropSize(0, 800, 600f, 300f))
    }

    @Test
    fun topBarUsesThePageCropRatherThanRecenteringIntoTheBarHeight() {
        for ((image, viewport) in listOf(
            IntSize(800, 400) to Size(300f, 600f),
            IntSize(400, 800) to Size(600f, 300f),
            IntSize(400, 800) to Size(300f, 600f),
        )) {
            val destination = customBackgroundCropSize(image.width, image.height, viewport.width, viewport.height)
            val pageOffset = customBackgroundCropOffset(destination, viewport)
            // Both expanded and collapsed bars sample the same full-page origin.
            for (barHeight in listOf(56f, 200f)) {
                val barOffset = customBackgroundCropOffset(destination, Size(viewport.width, barHeight))
                assertTrue(pageOffset != barOffset)
            }
        }
        assertEquals(IntOffset(0, -450), customBackgroundCropOffset(IntSize(600, 1200), Size(600f, 300f)))
        assertEquals(IntOffset(-450, 0), customBackgroundCropOffset(IntSize(1200, 600), Size(300f, 600f)))
    }
}
