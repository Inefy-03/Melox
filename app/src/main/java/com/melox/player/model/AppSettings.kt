package com.melox.player.model

/** Controls whether the app follows the system appearance or forces a light/dark theme. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/** Selects the seed used when dynamic colors are enabled. */
enum class DynamicColorSource {
    DESKTOP,
    PLAYBACK_ARTWORK,
}

/** Selects the artwork-derived background rendered behind the full player. */
enum class PlaybackBackgroundStyle {
    BLURRED_ARTWORK,
    FLOWING_COLORS,
}

/**
 * Selects the navigation bar presentation.
 *
 * [LIQUID_GLASS] is persisted like the other values; runtime support is gated by the UI layer.
 */
enum class BottomBarStyle {
    NORMAL,
    FLOATING,
    LIQUID_GLASS,
}

/** Selects the root destination shown after settings finish loading at app startup. */
enum class DefaultHomePage {
    HOME,
    SONGS,
    LIBRARY,
}

/** User-controlled preferences stored by the settings repository. */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = false,
    val dynamicColorSource: DynamicColorSource = DynamicColorSource.PLAYBACK_ARTWORK,
    val playbackBackgroundStyle: PlaybackBackgroundStyle =
        PlaybackBackgroundStyle.BLURRED_ARTWORK,
    val lyricFontScale: Float = 1f,
    val lyricFontWeight: Int = 400,
    val forceWordByWordLyrics: Boolean = false,
    val lyricBlurEnabled: Boolean = false,
    val centerLyrics: Boolean = false,
    val hideControlsOnLyrics: Boolean = false,
    val showLyricsTranslation: Boolean = true,
    val blurEnabled: Boolean = true,
    val floatingBottomBar: Boolean = false,
    val liquidGlass: Boolean = false,
    val predictiveBackEnabled: Boolean = true,
    val refreshLibraryOnStart: Boolean = false,
    val skipShortAudio: Boolean = false,
    val customFolderUris: List<String> = emptyList(),
    val libraryTabIndex: Int = 0,
    val musicSortFieldOrdinal: Int = 0,
    val musicSortDescending: Boolean = false,
    val albumSortFieldOrdinal: Int = 0,
    val albumSortDescending: Boolean = false,
    val albumGridStyleOrdinal: Int = 0,
    val artistSortFieldOrdinal: Int = 0,
    val artistSortDescending: Boolean = false,
    val folderSortFieldOrdinal: Int = 0,
    val folderSortDescending: Boolean = false,
    val defaultHomePage: DefaultHomePage = DefaultHomePage.HOME,
) {
    val bottomBarStyle: BottomBarStyle
        get() = when {
            !floatingBottomBar -> BottomBarStyle.NORMAL
            liquidGlass -> BottomBarStyle.LIQUID_GLASS
            else -> BottomBarStyle.FLOATING
        }
}
