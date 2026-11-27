package com.melox.player

import com.melox.player.model.ThemeMode
import com.melox.player.model.effectiveThemeMode
import com.melox.player.ui.screen.settings.themeModeFollowingSystem
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeSelectionTest {
    @Test
    fun systemSelectionTracksAppearanceWhileExplicitChoicesStayFixed() {
        for (systemDark in listOf(false, true)) {
            assertEquals(
                if (systemDark) ThemeMode.DARK else ThemeMode.LIGHT,
                effectiveThemeMode(ThemeMode.SYSTEM, systemDark),
            )
            assertEquals(ThemeMode.LIGHT, effectiveThemeMode(ThemeMode.LIGHT, systemDark))
            assertEquals(ThemeMode.DARK, effectiveThemeMode(ThemeMode.DARK, systemDark))
        }
    }

    @Test
    fun disablingFollowSystemPreservesAppearanceAndEnablingRestoresAutomaticMode() {
        for (systemDark in listOf(false, true)) {
            val explicitMode = themeModeFollowingSystem(false, systemDark)
            assertEquals(effectiveThemeMode(ThemeMode.SYSTEM, systemDark), explicitMode)
            assertEquals(explicitMode, effectiveThemeMode(explicitMode, !systemDark))
            assertEquals(ThemeMode.SYSTEM, themeModeFollowingSystem(true, systemDark))
        }
    }
}
