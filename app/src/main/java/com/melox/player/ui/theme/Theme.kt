package com.melox.player.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import android.os.Build
import com.melox.player.model.AppSettings
import com.melox.player.model.ThemeMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun MeloxTheme(
    settings: AppSettings,
    content: @Composable () -> Unit,
) {
    val colorSchemeMode = settings.toColorSchemeMode(
        dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    )
    // Recreate the controller only when the effective Miuix color mode changes.
    val controller = remember(colorSchemeMode) {
        ThemeController(colorSchemeMode = colorSchemeMode)
    }
    MiuixTheme(
        controller = controller,
        content = content,
    )
}

internal fun AppSettings.toColorSchemeMode(
    dynamicColorSupported: Boolean = true,
): ColorSchemeMode = when (themeMode) {
    ThemeMode.SYSTEM -> if (dynamicColorEnabled && dynamicColorSupported) {
        ColorSchemeMode.MonetSystem
    } else {
        ColorSchemeMode.System
    }

    ThemeMode.LIGHT -> if (dynamicColorEnabled && dynamicColorSupported) {
        ColorSchemeMode.MonetLight
    } else {
        ColorSchemeMode.Light
    }

    ThemeMode.DARK -> if (dynamicColorEnabled && dynamicColorSupported) {
        ColorSchemeMode.MonetDark
    } else {
        ColorSchemeMode.Dark
    }
}
