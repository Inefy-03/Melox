package com.melox.player

import androidx.compose.ui.unit.dp
import com.melox.player.ui.floatingBottomBarAvailableWidth
import com.melox.player.ui.floatingBottomBarBottomPadding
import com.melox.player.ui.floatingMiniPlayerBottomPaddingWhenNavigationIsHidden
import com.melox.player.ui.normalMiniPlayerBottomPadding
import com.melox.player.ui.component.liquid.floatingNavigationBarBottomPadding
import org.junit.Assert.assertEquals
import org.junit.Test

class BottomBarGeometryTest {
    @Test
    fun floatingPortraitWidthReservesSixteenDpOnEachSide() {
        assertEquals(358.dp, floatingBottomBarAvailableWidth(390.dp, 844.dp, 378.dp, 390.dp))
        assertEquals(796.dp, floatingBottomBarAvailableWidth(844.dp, 390.dp, 796.dp, 390.dp))
        assertEquals(1152.dp, floatingBottomBarAvailableWidth(1200.dp, 800.dp, 1152.dp, 800.dp))
    }

    @Test
    fun floatingPlayerAndNavigationShareTheSuppliedVisibleBottomDistances() {
        for (inset in listOf(0.dp, 24.dp, 48.dp)) {
            val portraitDistance = if (inset > 0.dp) inset else 24.dp
            val landscapeDistance = if (inset > 0.dp) inset else 16.dp
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
