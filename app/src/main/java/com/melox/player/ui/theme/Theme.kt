package com.melox.player.ui.theme

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.melox.player.model.AppSettings
import com.melox.player.model.DynamicColorSource
import com.melox.player.model.ThemeMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

private const val PLAYBACK_ARTWORK_COLOR_TRANSITION_DURATION_MILLIS = 600

private data class ThemePaletteTarget(
    val colorSchemeMode: ColorSchemeMode,
    val keyColor: Color?,
    val isDark: Boolean,
    val animateArtworkChanges: Boolean,
)

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
    val paletteTarget = ThemePaletteTarget(
        colorSchemeMode = colorSchemeMode,
        keyColor = dynamicKeyColor,
        isDark = isDark,
        animateArtworkChanges =
            settings.dynamicColorEnabled &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                settings.dynamicColorSource == DynamicColorSource.PLAYBACK_ARTWORK,
    )
    // Recreate the controller when the effective Miuix color source changes.
    val controller = remember(colorSchemeMode, dynamicKeyColor, isDark) {
        ThemeController(
            colorSchemeMode = colorSchemeMode,
            keyColor = dynamicKeyColor,
            isDark = isDark,
        )
    }
    MiuixTheme(controller = controller) {
        val targetColors = MiuixTheme.colorScheme
        val targetColorsSnapshot = remember(paletteTarget) { targetColors.copy() }
        val animatedColors = rememberAnimatedThemeColors(
            targetColors = targetColorsSnapshot,
            target = paletteTarget,
        )
        // Keep the outer Monet mode in the composition while overriding its colors with the
        // interpolated palette. Miuix components therefore retain their dynamic-color styling.
        MiuixTheme(
            colors = animatedColors,
            content = content,
        )
    }
}

@Composable
private fun rememberAnimatedThemeColors(
    targetColors: Colors,
    target: ThemePaletteTarget,
): Colors {
    var displayedColors by remember { mutableStateOf(targetColors) }
    var previousTarget by remember { mutableStateOf(target) }

    LaunchedEffect(target) {
        val startColors = displayedColors
        val shouldAnimate = target.shouldAnimateFrom(previousTarget)
        previousTarget = target
        if (!shouldAnimate) {
            displayedColors = targetColors
            return@LaunchedEffect
        }
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = PLAYBACK_ARTWORK_COLOR_TRANSITION_DURATION_MILLIS,
                easing = FastOutSlowInEasing,
            ),
        ) { progress, _ ->
            displayedColors = lerpThemeColors(startColors, targetColors, progress)
        }
        displayedColors = targetColors
    }
    return displayedColors
}

private fun ThemePaletteTarget.shouldAnimateFrom(previous: ThemePaletteTarget): Boolean =
    animateArtworkChanges &&
        previous.animateArtworkChanges &&
        colorSchemeMode == previous.colorSchemeMode &&
        isDark == previous.isDark &&
        keyColor != previous.keyColor

private fun lerpThemeColors(
    start: Colors,
    stop: Colors,
    fraction: Float,
): Colors = start.copy(
    primary = lerp(start.primary, stop.primary, fraction),
    onPrimary = lerp(start.onPrimary, stop.onPrimary, fraction),
    primaryVariant = lerp(start.primaryVariant, stop.primaryVariant, fraction),
    onPrimaryVariant = lerp(start.onPrimaryVariant, stop.onPrimaryVariant, fraction),
    error = lerp(start.error, stop.error, fraction),
    onError = lerp(start.onError, stop.onError, fraction),
    errorContainer = lerp(start.errorContainer, stop.errorContainer, fraction),
    onErrorContainer = lerp(start.onErrorContainer, stop.onErrorContainer, fraction),
    disabledPrimary = lerp(start.disabledPrimary, stop.disabledPrimary, fraction),
    disabledOnPrimary = lerp(start.disabledOnPrimary, stop.disabledOnPrimary, fraction),
    disabledPrimaryButton = lerp(start.disabledPrimaryButton, stop.disabledPrimaryButton, fraction),
    disabledOnPrimaryButton = lerp(start.disabledOnPrimaryButton, stop.disabledOnPrimaryButton, fraction),
    disabledPrimarySlider = lerp(start.disabledPrimarySlider, stop.disabledPrimarySlider, fraction),
    primaryContainer = lerp(start.primaryContainer, stop.primaryContainer, fraction),
    onPrimaryContainer = lerp(start.onPrimaryContainer, stop.onPrimaryContainer, fraction),
    secondary = lerp(start.secondary, stop.secondary, fraction),
    onSecondary = lerp(start.onSecondary, stop.onSecondary, fraction),
    secondaryVariant = lerp(start.secondaryVariant, stop.secondaryVariant, fraction),
    onSecondaryVariant = lerp(start.onSecondaryVariant, stop.onSecondaryVariant, fraction),
    disabledSecondary = lerp(start.disabledSecondary, stop.disabledSecondary, fraction),
    disabledOnSecondary = lerp(start.disabledOnSecondary, stop.disabledOnSecondary, fraction),
    disabledSecondaryVariant = lerp(start.disabledSecondaryVariant, stop.disabledSecondaryVariant, fraction),
    disabledOnSecondaryVariant = lerp(start.disabledOnSecondaryVariant, stop.disabledOnSecondaryVariant, fraction),
    secondaryContainer = lerp(start.secondaryContainer, stop.secondaryContainer, fraction),
    onSecondaryContainer = lerp(start.onSecondaryContainer, stop.onSecondaryContainer, fraction),
    secondaryContainerVariant = lerp(start.secondaryContainerVariant, stop.secondaryContainerVariant, fraction),
    onSecondaryContainerVariant = lerp(start.onSecondaryContainerVariant, stop.onSecondaryContainerVariant, fraction),
    tertiaryContainer = lerp(start.tertiaryContainer, stop.tertiaryContainer, fraction),
    onTertiaryContainer = lerp(start.onTertiaryContainer, stop.onTertiaryContainer, fraction),
    tertiaryContainerVariant = lerp(start.tertiaryContainerVariant, stop.tertiaryContainerVariant, fraction),
    background = lerp(start.background, stop.background, fraction),
    onBackground = lerp(start.onBackground, stop.onBackground, fraction),
    onBackgroundVariant = lerp(start.onBackgroundVariant, stop.onBackgroundVariant, fraction),
    surface = lerp(start.surface, stop.surface, fraction),
    onSurface = lerp(start.onSurface, stop.onSurface, fraction),
    surfaceVariant = lerp(start.surfaceVariant, stop.surfaceVariant, fraction),
    onSurfaceSecondary = lerp(start.onSurfaceSecondary, stop.onSurfaceSecondary, fraction),
    onSurfaceVariantSummary = lerp(start.onSurfaceVariantSummary, stop.onSurfaceVariantSummary, fraction),
    onSurfaceVariantActions = lerp(start.onSurfaceVariantActions, stop.onSurfaceVariantActions, fraction),
    disabledOnSurface = lerp(start.disabledOnSurface, stop.disabledOnSurface, fraction),
    surfaceContainer = lerp(start.surfaceContainer, stop.surfaceContainer, fraction),
    onSurfaceContainer = lerp(start.onSurfaceContainer, stop.onSurfaceContainer, fraction),
    onSurfaceContainerVariant = lerp(start.onSurfaceContainerVariant, stop.onSurfaceContainerVariant, fraction),
    surfaceContainerHigh = lerp(start.surfaceContainerHigh, stop.surfaceContainerHigh, fraction),
    onSurfaceContainerHigh = lerp(start.onSurfaceContainerHigh, stop.onSurfaceContainerHigh, fraction),
    surfaceContainerHighest = lerp(start.surfaceContainerHighest, stop.surfaceContainerHighest, fraction),
    onSurfaceContainerHighest = lerp(start.onSurfaceContainerHighest, stop.onSurfaceContainerHighest, fraction),
    outline = lerp(start.outline, stop.outline, fraction),
    dividerLine = lerp(start.dividerLine, stop.dividerLine, fraction),
    windowDimming = lerp(start.windowDimming, stop.windowDimming, fraction),
    sliderKeyPoint = lerp(start.sliderKeyPoint, stop.sliderKeyPoint, fraction),
    sliderKeyPointForeground = lerp(start.sliderKeyPointForeground, stop.sliderKeyPointForeground, fraction),
    sliderBackground = lerp(start.sliderBackground, stop.sliderBackground, fraction),
)

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
