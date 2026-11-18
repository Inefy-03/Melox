package com.melox.player

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.glass.internal.asCurrentBlurColors
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor
import top.yukonga.miuix.kmp.glass.isTransformPopupInteractive
import top.yukonga.miuix.kmp.glass.placeGlassDropdownPopup
import top.yukonga.miuix.kmp.glass.placeGlassPopup
import top.yukonga.miuix.kmp.glass.placeGlassSecondaryPopup
import top.yukonga.miuix.kmp.glass.transformPanelAlpha

class GlassPopupTest {
    private val window = Rect(80f, 24f, 480f, 824f)
    private val panel = Size(200f, 300f)

    @Test
    fun currentBlurRetainsOrderedColorLayersAndAdjustments() {
        val colors = BlurColors(
            blendColors = listOf(
                BlendColorEntry(Color.Red, BlurBlendMode.Multiply),
                BlendColorEntry(Color.Blue.copy(alpha = 0.4f), BlurBlendMode.Overlay),
            ),
            brightness = 0.1f,
            contrast = 1.2f,
            saturation = 0.8f,
        )
        val converted = colors.asCurrentBlurColors()
        assertEquals(colors.blendColors.map { it.color }, converted.blendColors.map { it.color })
        assertEquals(colors.blendColors.map { it.mode.value }, converted.blendColors.map { it.mode.value })
        assertEquals(colors.brightness, converted.brightness)
        assertEquals(colors.contrast, converted.contrast)
        assertEquals(colors.saturation, converted.saturation)
    }

    @Test
    fun toolbarMenuUsesWindowCoordinatesAndKeepsTheAnchorInsideTheTopMargin() {
        val anchor = Rect(400f, 28f, 444f, 72f)
        assertEquals(
            Rect(244f, 28f, 444f, 328f),
            placeGlassPopup(anchor, panel, 12f, window, LayoutDirection.Ltr).rect,
        )
        assertEquals(
            Rect(268f, 28f, 468f, 328f),
            placeGlassPopup(anchor, panel, 12f, window, LayoutDirection.Rtl).rect,
        )
        val aboveSafeArea = Rect(400f, 0f, 444f, 44f)
        assertEquals(24f, placeGlassPopup(aboveSafeArea, panel, 12f, window, LayoutDirection.Ltr).rect.top, 0f)
    }

    @Test
    fun menuOpensUpwardWhenTheBottomSafeAreaHasInsufficientRoom() {
        val anchor = Rect(400f, 740f, 444f, 784f)
        val placement = placeGlassPopup(anchor, panel, 12f, window, LayoutDirection.Ltr)
        assertFalse(placement.alignTop)
        assertEquals(Rect(244f, 484f, 444f, 784f), placement.rect)
    }

    @Test
    fun submenuUsesItsRowAndClampsOversizedPanelsToTheSafeArea() {
        val row = Rect(380f, 740f, 460f, 800f)
        assertEquals(Rect(268f, 512f, 468f, 812f), placeGlassSecondaryPopup(row, panel, 12f, window, LayoutDirection.Ltr))
        assertEquals(Rect(260f, 512f, 460f, 812f), placeGlassSecondaryPopup(row, panel, 12f, window, LayoutDirection.Rtl))
        assertEquals(Rect(92f, 36f, 468f, 812f), placeGlassSecondaryPopup(row, Size(900f, 900f), 12f, window, LayoutDirection.Ltr))
    }

    @Test
    fun preferenceValueRemainsAlignedAfterSharedAnchorsMoveToWindowCoordinates() {
        val row = Rect(92f, 200f, 468f, 260f)
        val value = Rect(370f, 214f, 452f, 246f)
        assertEquals(Rect(252f, 214f, 452f, 514f), placeGlassDropdownPopup(row, value, panel, 12f, window).rect)
        for (direction in LayoutDirection.entries) {
            val original = placeGlassPopup(row, panel, 12f, window, direction)
            val dropdown = placeGlassDropdownPopup(row, null, panel, 12f, window, direction)
            assertEquals(original.rect, dropdown.rect)
            assertEquals(original.alignTop, dropdown.alignTop)
        }
    }

    @Test
    fun popupVisibilityFollowsTheLiveAnchorAnimationIndependentlyOfMaterialOpacity() {
        val progress = mutableFloatStateOf(1f)
        val anchor = GlassPopupAnchor().apply {
            surfaceProgress = progress
            surfaceMaterialAlpha = 0.4f
        }
        for (visibility in listOf(1f, 0.5f, 0f)) {
            progress.floatValue = visibility
            assertEquals(visibility, anchor.surfaceVisibility, 0f)
            assertEquals(visibility, transformPanelAlpha(true, anchor.surfaceVisibility, 0f, 0f), 0f)
            assertEquals(1f, transformPanelAlpha(true, anchor.surfaceVisibility, 1f, 1f), 0f)
        }
    }

    @Test
    fun closingOrStackedMenusCannotActivateRows() {
        assertTrue(isTransformPopupInteractive(show = true, stacked = false))
        assertFalse(isTransformPopupInteractive(show = false, stacked = false))
        assertFalse(isTransformPopupInteractive(show = true, stacked = true))
    }
}
