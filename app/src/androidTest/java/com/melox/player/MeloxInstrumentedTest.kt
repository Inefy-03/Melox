package com.melox.player

import android.Manifest
import android.content.ComponentName
import android.content.pm.ActivityInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.melox.player.data.repository.SettingsRepository
import com.melox.player.model.LyricAnimationMode
import com.melox.player.model.NavigationTransitionStyle
import com.melox.player.model.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MeloxInstrumentedTest {
    @Test
    fun packageIsOfflineAndUsesExpectedApplicationId() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            android.content.pm.PackageManager.GET_PERMISSIONS,
        )

        assertEquals("com.melox.player", context.packageName)
        assertFalse(
            packageInfo.requestedPermissions
                .orEmpty()
                .contains(Manifest.permission.INTERNET),
        )
        assertFalse(
            packageInfo.requestedPermissions
                .orEmpty()
                .contains(Manifest.permission.ACCESS_NETWORK_STATE),
        )
    }

    @Test
    fun settingsRoundTripThroughDataStore() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = SettingsRepository(context)
        val original = repository.loadSettings()
        try {
            repository.setThemeMode(ThemeMode.DARK)
            repository.setDynamicColorEnabled(true)
            repository.setBlurEnabled(false)
            repository.setProgressiveTopBarBlurEnabled(true)
            repository.setHideBottomBar(true)
            repository.setFloatingBottomBar(true)
            repository.setLiquidGlass(true)
            repository.setPredictiveBackEnabled(true)
            repository.setNavigationTransitionStyle(NavigationTransitionStyle.AOSP)

            val restored = repository.loadSettings()
            assertEquals(ThemeMode.DARK, restored.themeMode)
            assertEquals(true, restored.dynamicColorEnabled)
            assertEquals(false, restored.blurEnabled)
            assertEquals(true, restored.progressiveTopBarBlurEnabled)
            assertEquals(true, restored.hideBottomBar)
            assertEquals(true, restored.floatingBottomBar)
            assertEquals(true, restored.liquidGlass)
            assertEquals(true, restored.predictiveBackEnabled)
            assertEquals(
                NavigationTransitionStyle.AOSP,
                restored.navigationTransitionStyle,
            )
        } finally {
            repository.setThemeMode(original.themeMode)
            repository.setDynamicColorEnabled(original.dynamicColorEnabled)
            repository.setBlurEnabled(original.blurEnabled)
            repository.setProgressiveTopBarBlurEnabled(
                original.progressiveTopBarBlurEnabled,
            )
            repository.setHideBottomBar(original.hideBottomBar)
            repository.setFloatingBottomBar(original.floatingBottomBar)
            repository.setLiquidGlass(original.liquidGlass)
            repository.setPredictiveBackEnabled(original.predictiveBackEnabled)
            repository.setNavigationTransitionStyle(original.navigationTransitionStyle)
        }
    }

    @Test
    fun lyricAnimationModesPersistAndPreserveOtherLyricSettings() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = SettingsRepository(context)
        val original = repository.loadSettings()
        try {
            for (mode in LyricAnimationMode.entries) {
                repository.setLyricAnimationMode(mode)
                val restored = SettingsRepository(context).loadSettings()
                assertEquals(mode, restored.lyricAnimationMode)
                assertEquals(original.lyricFontScale, restored.lyricFontScale, 0f)
                assertEquals(original.lyricFontWeight, restored.lyricFontWeight)
                assertEquals(original.showLyricsTranslation, restored.showLyricsTranslation)
            }
        } finally {
            repository.setLyricAnimationMode(original.lyricAnimationMode)
        }
    }

    @Test
    fun cardBlurSettingPersistsIndependentlyAndClampsBounds() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = SettingsRepository(context)
        val original = repository.loadSettings()
        try {
            for ((requested, expected) in listOf(-1 to 0, 50 to 50, 101 to 100)) {
                repository.setCustomBackgroundCardBlurPercent(requested)
                val restored = SettingsRepository(context).loadSettings()
                assertEquals(expected, restored.customBackgroundCardBlurPercent)
                assertEquals(original.customBackgroundBlurPercent, restored.customBackgroundBlurPercent)
                assertEquals(original.customBackgroundDimPercent, restored.customBackgroundDimPercent)
                assertEquals(original.blurEnabled, restored.blurEnabled)
            }
        } finally {
            repository.setCustomBackgroundCardBlurPercent(original.customBackgroundCardBlurPercent)
        }
    }

    @Test
    fun wallpaperDimmingSettingStopsAtNinetyPercentAndPreservesOtherSettings() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = SettingsRepository(context)
        val original = repository.loadSettings()
        try {
            for ((requested, expected) in listOf(-1 to 0, 90 to 90, 91 to 90, 100 to 90)) {
                repository.setCustomBackgroundDimPercent(requested)
                val restored = SettingsRepository(context).loadSettings()
                assertEquals(expected, restored.customBackgroundDimPercent)
                assertEquals(original.customBackgroundBlurPercent, restored.customBackgroundBlurPercent)
                assertEquals(original.customBackgroundCardBlurPercent, restored.customBackgroundCardBlurPercent)
                assertEquals(original.customBackgroundCardOpacityPercent, restored.customBackgroundCardOpacityPercent)
                assertEquals(original.blurEnabled, restored.blurEnabled)
            }
        } finally {
            repository.setCustomBackgroundDimPercent(original.customBackgroundDimPercent)
        }
    }

    @Test
    fun cardOpacitySettingPersistsWithoutChangingBlurOrOtherBackgroundSettings() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = SettingsRepository(context)
        val original = repository.loadSettings()
        try {
            for ((requested, expected) in listOf(-1 to 0, 80 to 80, 81 to 80, 101 to 80)) {
                repository.setCustomBackgroundCardOpacityPercent(requested)
                val restored = SettingsRepository(context).loadSettings()
                assertEquals(expected, restored.customBackgroundCardOpacityPercent)
                assertEquals(original.customBackgroundCardBlurPercent, restored.customBackgroundCardBlurPercent)
                assertEquals(original.customBackgroundBlurPercent, restored.customBackgroundBlurPercent)
                assertEquals(original.customBackgroundDimPercent, restored.customBackgroundDimPercent)
                assertEquals(original.blurEnabled, restored.blurEnabled)
            }
        } finally {
            repository.setCustomBackgroundCardOpacityPercent(original.customBackgroundCardOpacityPercent)
        }
    }

    @Test
    fun mainActivityCreatesWithoutCrashing() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse(activity.isFinishing)
            }
        }
    }

    @Test
    fun mainActivityHandlesUiAndWindowChangesInPlace() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        @Suppress("DEPRECATION")
        val activityInfo = context.packageManager.getActivityInfo(
            ComponentName(context, MainActivity::class.java),
            0,
        )

        assertEquals(
            ActivityInfo.CONFIG_LOCALE,
            activityInfo.configChanges and ActivityInfo.CONFIG_LOCALE,
        )
        assertEquals(
            ActivityInfo.CONFIG_LAYOUT_DIRECTION,
            activityInfo.configChanges and ActivityInfo.CONFIG_LAYOUT_DIRECTION,
        )
        assertEquals(
            ActivityInfo.CONFIG_ORIENTATION,
            activityInfo.configChanges and ActivityInfo.CONFIG_ORIENTATION,
        )
        assertEquals(
            ActivityInfo.CONFIG_SCREEN_SIZE,
            activityInfo.configChanges and ActivityInfo.CONFIG_SCREEN_SIZE,
        )
    }
}
