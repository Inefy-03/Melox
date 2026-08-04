package com.melox.player

import android.Manifest
import android.content.ComponentName
import android.content.pm.ActivityInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.melox.player.data.repository.SettingsRepository
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
            repository.setFloatingBottomBar(true)
            repository.setLiquidGlass(true)
            repository.setPredictiveBackEnabled(true)

            val restored = repository.loadSettings()
            assertEquals(ThemeMode.DARK, restored.themeMode)
            assertEquals(true, restored.dynamicColorEnabled)
            assertEquals(false, restored.blurEnabled)
            assertEquals(true, restored.floatingBottomBar)
            assertEquals(true, restored.liquidGlass)
            assertEquals(true, restored.predictiveBackEnabled)
        } finally {
            repository.setThemeMode(original.themeMode)
            repository.setDynamicColorEnabled(original.dynamicColorEnabled)
            repository.setBlurEnabled(original.blurEnabled)
            repository.setFloatingBottomBar(original.floatingBottomBar)
            repository.setLiquidGlass(original.liquidGlass)
            repository.setPredictiveBackEnabled(original.predictiveBackEnabled)
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
    fun mainActivityHandlesLocaleChangesInPlace() {
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
    }
}
