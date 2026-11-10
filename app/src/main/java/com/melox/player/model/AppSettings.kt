package com.melox.player.model

import com.melox.player.data.playlist.PlaylistSortConfig
import com.melox.player.data.library.MusicSortConfig

/** Selects which auxiliary translation language is shown beside lyrics. */
enum class LyricsTranslationLanguage {
    AUTO,
    CHINESE,
    ENGLISH,
    ALL,
}

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
    DYNAMIC_FLOW,
}

/** Selects the clip shape used for playback artwork. */
enum class CoverShape {
    DEFAULT,
    ROUNDED_RECT,
    CIRCLE,
}

/** Selects the dynamic-flow appearance independently of the app theme. */
enum class DynamicFlowStyle {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }
}

fun usesDarkPlaybackBackground(
    backgroundStyle: PlaybackBackgroundStyle,
    dynamicFlowDark: Boolean,
    appDark: Boolean,
    hasItem: Boolean,
): Boolean = when {
    backgroundStyle == PlaybackBackgroundStyle.DYNAMIC_FLOW -> dynamicFlowDark
    hasItem -> true
    else -> appDark
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

/** Selects the Miuix navigation host transition preset. */
enum class NavigationTransitionStyle {
    MIUIX,
    AOSP,
}

/** Selects the root destination shown after settings finish loading at app startup. */
enum class DefaultHomePage {
    HOME,
    SONGS,
    LIBRARY,
}

/** Selects whether embedded or sidecar lyrics are attempted first. */
enum class LyricsSourcePriority {
    EMBEDDED,
    SIDECAR,
}

/** Selects the preferred parsed lyric format within each source. */
enum class LyricsFormatPriority(val format: LyricsFormat) {
    LRC(LyricsFormat.LRC),
    TTML(LyricsFormat.TTML),
}

enum class LyricAnimationMode {
    CURRENT_LINE,
    EXTEND_TO_ALL,
    ALWAYS,
    NEVER,
    ;

    fun usesWordAnimation(lineHasTimedWords: Boolean, documentHasTimedWords: Boolean): Boolean =
        when (this) {
            CURRENT_LINE -> lineHasTimedWords
            EXTEND_TO_ALL -> documentHasTimedWords
            ALWAYS -> true
            NEVER -> false
        }
}

fun resolveLyricAnimationMode(storedValue: String?, legacyForceWordByWord: Boolean?): LyricAnimationMode =
    if (storedValue != null) {
        LyricAnimationMode.entries.firstOrNull { it.name == storedValue }
            ?: LyricAnimationMode.CURRENT_LINE
    } else if (legacyForceWordByWord == true) {
        LyricAnimationMode.ALWAYS
    } else {
        LyricAnimationMode.CURRENT_LINE
    }

val PLAYBACK_SPEED_VALUES = listOf(
    0.25f, 0.50f, 0.75f, 0.90f, 0.95f, 1.00f, 1.05f,
    1.10f, 1.25f, 1.50f, 1.75f, 2.00f, 2.50f, 3.00f,
)

fun normalizePlaybackSpeed(speed: Float): Float =
    speed.takeIf(PLAYBACK_SPEED_VALUES::contains) ?: 1f

const val DEFAULT_CUSTOM_BACKGROUND_BLUR_PERCENT = 0
const val DEFAULT_CUSTOM_BACKGROUND_CARD_BLUR_PERCENT = 50
const val DEFAULT_CUSTOM_BACKGROUND_CARD_OPACITY_PERCENT = 80
const val MIN_CUSTOM_BACKGROUND_CARD_OPACITY_PERCENT = 10
const val MAX_CUSTOM_BACKGROUND_CARD_OPACITY_PERCENT = 80
const val MAX_CUSTOM_BACKGROUND_DIM_PERCENT = 90

fun normalizeCustomBackgroundBlurPercent(percent: Int): Int = percent.coerceIn(0, 100)

internal fun resolveCustomBackgroundBlurPercent(storedPercent: Int?, legacyEnabled: Boolean?): Int =
    when (legacyEnabled) {
        false -> 0
        true -> normalizeCustomBackgroundBlurPercent(storedPercent ?: 50)
        null -> normalizeCustomBackgroundBlurPercent(storedPercent ?: DEFAULT_CUSTOM_BACKGROUND_BLUR_PERCENT)
    }

fun normalizeCustomBackgroundDimPercent(percent: Int): Int = percent.coerceIn(0, MAX_CUSTOM_BACKGROUND_DIM_PERCENT)

fun normalizeCustomBackgroundCardBlurPercent(percent: Int): Int = percent.coerceIn(0, 100)

fun normalizeCustomBackgroundCardOpacityPercent(percent: Int): Int =
    percent.coerceIn(MIN_CUSTOM_BACKGROUND_CARD_OPACITY_PERCENT, MAX_CUSTOM_BACKGROUND_CARD_OPACITY_PERCENT)

enum class TransientAudioFocusBehavior {
    KEEP_VOLUME,
    DUCK,
    PAUSE,
}

/** User-controlled preferences stored by the settings repository. */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = false,
    val dynamicColorSource: DynamicColorSource = DynamicColorSource.PLAYBACK_ARTWORK,
    val customBackgroundId: String? = null,
    val customBackgroundBlurPercent: Int = DEFAULT_CUSTOM_BACKGROUND_BLUR_PERCENT,
    val customBackgroundDimPercent: Int = 0,
    val customBackgroundCardBlurPercent: Int = DEFAULT_CUSTOM_BACKGROUND_CARD_BLUR_PERCENT,
    val customBackgroundCardOpacityPercent: Int = DEFAULT_CUSTOM_BACKGROUND_CARD_OPACITY_PERCENT,
    val playbackBackgroundStyle: PlaybackBackgroundStyle =
        PlaybackBackgroundStyle.BLURRED_ARTWORK,
    val dynamicFlowStyle: DynamicFlowStyle = DynamicFlowStyle.SYSTEM,
    val showSongListArtwork: Boolean = true,
    val coverShape: CoverShape = CoverShape.DEFAULT,
    val artworkFlipAnimation: Boolean = false,
    val showSongListFileName: Boolean = false,
    val showSongListDuration: Boolean = true,
    val playbackSpeed: Float = 1f,
    val highPrecisionOutput: Boolean = false,
    val sleepTimerSeconds: Int = 600,
    val autoExtendSleepTimer: Boolean = false,
    val playbackPauseFade: Boolean = false,
    val audioFocusEnabled: Boolean = true,
    val transientAudioFocusBehavior: TransientAudioFocusBehavior = TransientAudioFocusBehavior.DUCK,
    val autoPlayOnStart: Boolean = false,
    val terminateOnTaskRemoved: Boolean = false,
    val lyricFontScale: Float = 1f,
    val lyricFontWeight: Int = 400,
    val lyricAnimationMode: LyricAnimationMode = LyricAnimationMode.CURRENT_LINE,
    val lyricBlurEnabled: Boolean = false,
    val centerLyrics: Boolean = false,
    val leftAlignPlayerTitle: Boolean = false,
    val hideControlsOnLyrics: Boolean = false,
    val showLyricsTranslation: Boolean = true,
    val showLyricsRomanization: Boolean = true,
    val lyricsTranslationLanguage: LyricsTranslationLanguage = LyricsTranslationLanguage.AUTO,
    val showMiniPlayerLyrics: Boolean = true,
    val showArtworkLyrics: Boolean = false,
    val showMusicTagEditor: Boolean = true,
    val showLyricoEditor: Boolean = true,
    val showLunaBeatEditor: Boolean = true,
    val lyricsSourcePriority: LyricsSourcePriority = LyricsSourcePriority.EMBEDDED,
    val lyricsFormatPriority: LyricsFormatPriority =
        LyricsFormatPriority.LRC,
    val blurEnabled: Boolean = true,
    val progressiveTopBarBlurEnabled: Boolean = false,
    val smallPlayerBar: Boolean = false,
    val hideBottomBar: Boolean = false,
    val immersiveMode: Boolean = false,
    val hideStatusBar: Boolean = false,
    val floatingBottomBar: Boolean = false,
    val navigationRailExpanded: Boolean = true,
    val liquidGlass: Boolean = false,
    val predictiveBackEnabled: Boolean = true,
    val navigationTransitionStyle: NavigationTransitionStyle =
        NavigationTransitionStyle.MIUIX,
    val tabSwitchHapticsEnabled: Boolean = false,
    val songListPullToRefresh: Boolean = true,
    val refreshLibraryOnStart: Boolean = false,
    val skipShortAudio: Boolean = false,
    val customFolderUris: List<String> = emptyList(),
    val blockedFolderPaths: List<String> = emptyList(),
    val hiddenTrackUris: Set<String> = emptySet(),
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
    val folderTrackSortConfigs: Map<String, MusicSortConfig> = emptyMap(),
    val playlistSortConfigs: Map<String, PlaylistSortConfig> = emptyMap(),
    val defaultHomePage: DefaultHomePage = DefaultHomePage.HOME,
) {
    /** Backwards-compatible derived flag; only the circular shape crops playback artwork. */
    val circularArtwork: Boolean
        get() = coverShape == CoverShape.CIRCLE

    val statusBarHidden: Boolean
        get() = immersiveMode || hideStatusBar

    val bottomBarStyle: BottomBarStyle
        get() = when {
            !floatingBottomBar -> BottomBarStyle.NORMAL
            liquidGlass -> BottomBarStyle.LIQUID_GLASS
            else -> BottomBarStyle.FLOATING
        }
}
