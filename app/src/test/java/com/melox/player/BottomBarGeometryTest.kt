package com.melox.player

import androidx.compose.ui.unit.dp
import com.melox.player.ui.floatingBottomBarAvailableWidth
import com.melox.player.ui.floatingBottomBarBottomPadding
import com.melox.player.ui.floatingMiniPlayerBottomPaddingWhenNavigationIsHidden
import com.melox.player.ui.normalMiniPlayerBottomPadding
import com.melox.player.ui.component.liquid.floatingNavigationBarBottomPadding
import org.junit.Assert.assertEquals
import com.melox.player.ui.MiniPlayerChrome
import com.melox.player.model.BottomBarStyle
import org.junit.Test

class BottomBarGeometryTest {
    @Test
    fun progressiveBandFollowsMeasuredTabsAndSearchHeight() {
        for (scale in listOf(1f, 2f, 3f)) {
            val density = androidx.compose.ui.unit.Density(scale)
            val topInset = (24 * scale).toInt()
            val bar = (76 * scale).toInt()
            val tabs = (48 * scale).toInt()
            val search = (56 * scale).toInt()
            val base = top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults.progressiveBandHeight(bar, topInset, density)
            assertEquals(base + tabs, top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults.progressiveBandHeight(bar + tabs, topInset, density))
            assertEquals(base + tabs + search, top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults.progressiveBandHeight(bar + tabs + search, topInset, density))
        }
    }

    @Test
    fun os4PlaybackGeometryUsesTheFixedHeight() {
        val chrome = MiniPlayerChrome(
            style = BottomBarStyle.OS4,
            backdrop = null,
            blurActive = false,
            liquidGlassActive = false,
            isDark = false,
            smallPlayerBar = true,
        )
        assertEquals(52.dp, com.melox.player.ui.component.playback.miniPlayerHeight(BottomBarStyle.OS4, false))
        assertEquals(52.dp, chrome.height)
        assertEquals(26.dp, chrome.cornerRadius)
        assertEquals(36.dp, chrome.artworkSize)
        assertEquals(16.dp, chrome.os4ArtworkStartPadding)
        assertEquals(6.dp, chrome.os4ArtworkCornerRadius)
        assertEquals(0.25f, chrome.contentFraction)
        assertEquals(52.dp, chrome.copy(smallPlayerBar = false).height)
        assertEquals(64.dp, chrome.copy(style = BottomBarStyle.LIQUID_GLASS, smallPlayerBar = false).height)
    }

    @Test
    fun floatingPortraitWidthReservesSixteenDpOnEachSide() {
        assertEquals(358.dp, floatingBottomBarAvailableWidth(390.dp, 844.dp, 378.dp, 390.dp))
        assertEquals(796.dp, floatingBottomBarAvailableWidth(844.dp, 390.dp, 796.dp, 390.dp))
        assertEquals(1152.dp, floatingBottomBarAvailableWidth(1200.dp, 800.dp, 1152.dp, 800.dp))
    }

    @Test
    fun floatingPlayerAndNavigationShareTheSuppliedVisibleBottomDistances() {
        for (inset in listOf(0.dp, 24.dp, 48.dp)) {
            val portraitDistance = if (inset > 0.dp) inset + 8.dp else 24.dp
            val landscapeDistance = if (inset > 0.dp) inset + 8.dp else 16.dp
            assertEquals(portraitDistance, floatingNavigationBarBottomPadding(inset))
            assertEquals(landscapeDistance, floatingBottomBarBottomPadding(inset))
            assertEquals(portraitDistance, floatingMiniPlayerBottomPaddingWhenNavigationIsHidden(inset, true) + 8.dp)
            assertEquals(landscapeDistance, floatingMiniPlayerBottomPaddingWhenNavigationIsHidden(inset, false) + 8.dp)
        }
    }

    @Test
    fun normalPortraitRetainsSixDpAboveInsetsAndUsesTwentyFourWithoutInsets() {
        assertEquals(24.dp, normalMiniPlayerBottomPadding(390.dp, 844.dp, 0.dp) + 6.dp)
        assertEquals(30.dp, normalMiniPlayerBottomPadding(390.dp, 844.dp, 24.dp) + 6.dp)
    }

    @Test
    fun rotatedPhoneAndWideWindowShareSixteenDpZeroInsetFallback() {
        assertEquals(16.dp, normalMiniPlayerBottomPadding(844.dp, 390.dp, 0.dp) + 6.dp)
        assertEquals(16.dp, normalMiniPlayerBottomPadding(1200.dp, 800.dp, 0.dp) + 6.dp)
        assertEquals(16.dp, normalMiniPlayerBottomPadding(900.dp, 1600.dp, 0.dp) + 6.dp)
        for ((width, height) in listOf(844.dp to 390.dp, 1200.dp to 800.dp)) {
            assertEquals(24.dp, normalMiniPlayerBottomPadding(width, height, 24.dp) + 6.dp)
            assertEquals(48.dp, normalMiniPlayerBottomPadding(width, height, 48.dp) + 6.dp)
        }
    }
}
