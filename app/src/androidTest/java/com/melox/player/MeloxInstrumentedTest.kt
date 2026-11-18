package com.melox.player

import android.Manifest
import android.content.ComponentName
import android.content.pm.ActivityInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.melox.player.data.repository.SettingsRepository
import com.melox.player.model.BottomBarStyle
import com.melox.player.model.CoverShape
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
            repository.setCustomBackgroundEnabled(false)
            repository.setShowSongListArtwork(false)
            repository.setCircularArtwork(true)
            repository.setShowSongListFileName(true)
            repository.setBlurEnabled(false)
            repository.setProgressiveTopBarBlurEnabled(true)
            repository.setHideBottomBar(true)
            repository.setBottomBarStyle(BottomBarStyle.LIQUID_GLASS)
            repository.setPredictiveBackEnabled(true)
            repository.setNavigationTransitionStyle(NavigationTransitionStyle.AOSP)

            val restored = repository.loadSettings()
            assertEquals(ThemeMode.DARK, restored.themeMode)
            assertEquals(true, restored.dynamicColorEnabled)
            assertEquals(false, restored.customBackgroundEnabled)
            assertEquals(original.customBackgroundId, restored.customBackgroundId)
            assertEquals(original.customBackgroundBlurPercent, restored.customBackgroundBlurPercent)
            assertEquals(original.customBackgroundDimPercent, restored.customBackgroundDimPercent)
            assertEquals(original.customBackgroundCardBlurPercent, restored.customBackgroundCardBlurPercent)
            assertEquals(original.customBackgroundCardOpacityPercent, restored.customBackgroundCardOpacityPercent)
            assertEquals(false, restored.showSongListArtwork)
            assertEquals(true, restored.circularArtwork)
            assertEquals(true, restored.showSongListFileName)
            assertEquals(false, restored.blurEnabled)
            assertEquals(true, restored.progressiveTopBarBlurEnabled)
            assertEquals(true, restored.hideBottomBar)
            assertEquals(BottomBarStyle.LIQUID_GLASS, restored.bottomBarStyle)
            assertEquals(true, restored.predictiveBackEnabled)
            assertEquals(
                NavigationTransitionStyle.AOSP,
                restored.navigationTransitionStyle,
            )
            for (shape in CoverShape.entries) {
                repository.setCoverShape(shape)
                val shaped = repository.loadSettings()
                assertEquals(shape, shaped.coverShape)
                assertEquals(shape == CoverShape.CIRCLE, shaped.circularArtwork)
            }
        } finally {
            repository.setThemeMode(original.themeMode)
            repository.setDynamicColorEnabled(original.dynamicColorEnabled)
            repository.setCustomBackgroundEnabled(original.customBackgroundEnabled)
            repository.setShowSongListArtwork(original.showSongListArtwork)
            repository.setCoverShape(original.coverShape)
            repository.setShowSongListFileName(original.showSongListFileName)
            repository.setBlurEnabled(original.blurEnabled)
            repository.setProgressiveTopBarBlurEnabled(
                original.progressiveTopBarBlurEnabled,
            )
            repository.setHideBottomBar(original.hideBottomBar)
            repository.setBottomBarStyle(original.bottomBarStyle)
            repository.setPredictiveBackEnabled(original.predictiveBackEnabled)
            repository.setNavigationTransitionStyle(original.navigationTransitionStyle)
        }
    }

    @Test
    fun audioFocusPreferencesPersistIndependently() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = SettingsRepository(context)
        val original = repository.loadSettings()
        try {
            for (enabled in listOf(false, true)) {
                for (behavior in com.melox.player.model.TransientAudioFocusBehavior.entries) {
                    repository.setAudioFocusEnabled(enabled)
                    repository.setTransientAudioFocusBehavior(behavior)
                    val restored = SettingsRepository(context).loadSettings()
                    assertEquals(enabled, restored.audioFocusEnabled)
                    assertEquals(behavior, restored.transientAudioFocusBehavior)
                    assertEquals(original.highPrecisionOutput, restored.highPrecisionOutput)
                }
            }
        } finally {
            repository.setAudioFocusEnabled(original.audioFocusEnabled)
            repository.setTransientAudioFocusBehavior(original.transientAudioFocusBehavior)
        }
    }

    @Test
    fun playbackBehaviorPreferencesPersistIndependently() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = SettingsRepository(context)
        val original = repository.loadSettings()
        try {
            for (autoPlay in listOf(false, true)) {
                for (terminate in listOf(false, true)) {
                    repository.setAutoPlayOnStart(autoPlay)
                    repository.setTerminateOnTaskRemoved(terminate)
                    val restored = SettingsRepository(context).loadSettings()
                    assertEquals(autoPlay, restored.autoPlayOnStart)
                    assertEquals(terminate, restored.terminateOnTaskRemoved)
                    assertEquals(original.playbackPauseFade, restored.playbackPauseFade)
                }
            }
        } finally {
            repository.setAutoPlayOnStart(original.autoPlayOnStart)
            repository.setTerminateOnTaskRemoved(original.terminateOnTaskRemoved)
        }
    }

    @Test
    fun immersiveAndStatusBarPreferencesPersistIndependently() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = SettingsRepository(context)
        val original = repository.loadSettings()
        try {
            repository.setHideControlsOnLyrics(true)
            repository.setHideStatusBar(true)
            repository.setImmersiveMode(true)
            repository.setImmersiveMode(false)
            val afterImmersion = SettingsRepository(context).loadSettings()
            assertEquals(false, afterImmersion.immersiveMode)
            assertEquals(true, afterImmersion.hideStatusBar)
            assertEquals(true, afterImmersion.statusBarHidden)
            assertEquals(true, afterImmersion.hideControlsOnLyrics)

            repository.setImmersiveMode(true)
            repository.setHideStatusBar(false)
            val afterStatusBar = SettingsRepository(context).loadSettings()
            assertEquals(true, afterStatusBar.immersiveMode)
            assertEquals(false, afterStatusBar.hideStatusBar)
            assertEquals(true, afterStatusBar.statusBarHidden)

            repository.setImmersiveMode(false)
            assertEquals(false, SettingsRepository(context).loadSettings().statusBarHidden)
        } finally {
            repository.setImmersiveMode(original.immersiveMode)
            repository.setHideStatusBar(original.hideStatusBar)
            repository.setHideControlsOnLyrics(original.hideControlsOnLyrics)
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
    fun translationLanguagePersistsIndependentlyOfCaptionVisibility() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = SettingsRepository(context)
        val original = repository.loadSettings()
        try {
            for (language in com.melox.player.model.LyricsTranslationLanguage.entries) {
                repository.setLyricsTranslationLanguage(language)
                val restored = SettingsRepository(context).loadSettings()
                assertEquals(language, restored.lyricsTranslationLanguage)
                assertEquals(original.showLyricsTranslation, restored.showLyricsTranslation)
                assertEquals(original.showLyricsRomanization, restored.showLyricsRomanization)
            }
        } finally {
            repository.setLyricsTranslationLanguage(original.lyricsTranslationLanguage)
        }
    }

    @Test
    fun romanizationVisibilityPersistsIndependentlyOfTranslation() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = SettingsRepository(context)
        val original = repository.loadSettings()
        try {
            for (romanization in listOf(true, false)) {
                for (translation in listOf(true, false)) {
                    repository.setShowLyricsRomanization(romanization)
                    repository.setShowLyricsTranslation(translation)
                    val restored = SettingsRepository(context).loadSettings()
                    assertEquals(romanization, restored.showLyricsRomanization)
                    assertEquals(translation, restored.showLyricsTranslation)
                }
            }
        } finally {
            repository.setShowLyricsRomanization(original.showLyricsRomanization)
            repository.setShowLyricsTranslation(original.showLyricsTranslation)
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
