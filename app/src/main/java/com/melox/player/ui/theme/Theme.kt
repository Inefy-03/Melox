package com.melox.player.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.melox.player.model.AppSettings
import com.melox.player.model.DynamicColorSource
import com.melox.player.model.ThemeMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun MeloxTheme(
    settings: AppSettings,
    playbackArtworkColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val colorSchemeMode = settings.toColorSchemeMode(
        dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    )
    val dynamicKeyColor = resolveDynamicColorSeed(
        dynamicColorEnabled = settings.dynamicColorEnabled,
        source = settings.dynamicColorSource,
        playbackArtworkColor = playbackArtworkColor,
    )
    val isDark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    // Recreate the controller when the effective Miuix color source changes.
    val controller = remember(colorSchemeMode, dynamicKeyColor, isDark) {
        ThemeController(
            colorSchemeMode = colorSchemeMode,
            keyColor = dynamicKeyColor,
            isDark = isDark,
        )
    }
    MiuixTheme(
        controller = controller,
        content = content,
    )
}

/**
 * Desktop wallpaper dynamic colors are resolved by Miuix from the platform Monet palette.
 * Artwork mode supplies a seed only after a cover has been loaded; the platform palette remains
 * the fallback.
 */
internal fun resolveDynamicColorSeed(
    dynamicColorEnabled: Boolean,
    source: DynamicColorSource,
    playbackArtworkColor: Color?,
): Color? = if (!dynamicColorEnabled) {
    null
} else {
    when (source) {
        DynamicColorSource.DESKTOP -> null
        DynamicColorSource.PLAYBACK_ARTWORK -> playbackArtworkColor
    }
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
