package com.melox.player

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.melox.player.model.AppSettings
import com.melox.player.model.DynamicFlowStyle
import com.melox.player.model.PlaybackBackgroundStyle
import com.melox.player.model.usesDarkPlaybackBackground
import com.melox.player.ui.component.playback.dynamicFlowPrimaryWashColor
import com.melox.player.ui.component.playback.dynamicFlowSecondaryWashColor
import com.melox.player.ui.component.playback.dynamicFlowBackgroundColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicFlowStyleTest {
    @Test
    fun defaultFlowUsesDarkAppearance() {
        val style = AppSettings().dynamicFlowStyle
        assertEquals(DynamicFlowStyle.DARK, style)
        assertTrue(style.isDark(systemDark = false))
        assertTrue(style.isDark(systemDark = true))
    }

    @Test
    fun explicitFlowStylesIgnoreSystemChanges() {
        for (systemDark in listOf(false, true)) {
            assertFalse(DynamicFlowStyle.LIGHT.isDark(systemDark))
            assertTrue(DynamicFlowStyle.DARK.isDark(systemDark))
        }
    }

    @Test
    fun unavailableArtworkUsesTheMatchingFlowFallback() {
        assertEquals(Color(0xFF242424), dynamicFlowBackgroundColor(null, dark = true))
        assertEquals(Color(0xFFF2F2F2), dynamicFlowBackgroundColor(null, dark = false))
    }

    @Test
    fun flowAppearanceAlsoControlsTheEmptyPlayerIndependentlyOfAppTheme() {
        for (appDark in listOf(false, true)) {
            for (hasItem in listOf(false, true)) {
                for (dynamicFlowDark in listOf(false, true)) {
                    assertEquals(
                        dynamicFlowDark,
                        usesDarkPlaybackBackground(
                            PlaybackBackgroundStyle.DYNAMIC_FLOW, dynamicFlowDark, appDark, hasItem,
                        ),
                    )
                }
            }
        }
    }

    @Test
    fun emptyBlurredPlayerFollowsAppThemeWhileSelectedArtworkStaysDark() {
        val backgroundStyle = PlaybackBackgroundStyle.BLURRED_ARTWORK
        for (appDark in listOf(false, true)) {
            for (dynamicFlowDark in listOf(false, true)) {
                assertEquals(
                    appDark,
                    usesDarkPlaybackBackground(backgroundStyle, dynamicFlowDark, appDark, hasItem = false),
                )
                assertTrue(usesDarkPlaybackBackground(backgroundStyle, dynamicFlowDark, appDark, hasItem = true))
            }
        }
    }

    @Test
    fun darkFlowPreservesExistingWashColorsAndOpacity() {
        val background = Color(0xFF6080A0)
        val expectedPrimary = Color(
            red = background.red * 0.76f,
            green = background.green * 0.76f,
            blue = background.blue * 0.76f,
        ).copy(alpha = 0.24f)
        assertEquals(expectedPrimary, dynamicFlowPrimaryWashColor(background, dark = true))
        assertEquals(
            Color(0xFF121316).copy(alpha = 0.16f),
            dynamicFlowSecondaryWashColor(dark = true),
        )
    }

    @Test
    fun lightFlowUsesRequestedWhiteWashesAndBrightensDarkArtwork() {
        for (background in listOf(Color.Black, Color(0xFF6080A0), Color.White)) {
            val primary = dynamicFlowPrimaryWashColor(background, dark = false)
            val secondary = dynamicFlowSecondaryWashColor(dark = false)
            assertEquals(Color.White.copy(alpha = 0.48f), primary)
            assertEquals(Color.White.copy(alpha = 0.16f), secondary)
            val result = secondary.compositeOver(primary.compositeOver(background))
            assertTrue(result.red >= background.red)
            assertTrue(result.green >= background.green)
            assertTrue(result.blue >= background.blue)
            assertTrue(result.red > 0.42f && result.green > 0.42f && result.blue > 0.42f)
            assertEquals(1f, result.alpha, 0f)
        }
    }
}
