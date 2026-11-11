package com.melox.player.data.repository

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.melox.player.model.LyricsTranslationLanguage
import com.melox.player.model.LyricAnimationMode
import com.melox.player.model.resolveLyricAnimationMode
import com.melox.player.data.library.AlbumSortConfig
import com.melox.player.data.library.AlbumSortField
import com.melox.player.data.library.AlbumGridStyle
import com.melox.player.data.library.ArtistSortConfig
import com.melox.player.data.library.ArtistSortField
import com.melox.player.data.library.FolderSortConfig
import com.melox.player.data.library.FolderSortField
import com.melox.player.data.library.MusicSortConfig
import com.melox.player.data.library.MusicSortField
import com.melox.player.model.AppSettings
import com.melox.player.model.TransientAudioFocusBehavior
import com.melox.player.data.playlist.PlaylistSortConfig
import com.melox.player.data.playlist.PlaylistSortField
import com.melox.player.model.normalizeCustomBackgroundBlurPercent
import com.melox.player.model.resolveCustomBackgroundBlurPercent
import com.melox.player.model.normalizeCustomBackgroundDimPercent
import com.melox.player.model.normalizeCustomBackgroundCardBlurPercent
import com.melox.player.model.DEFAULT_CUSTOM_BACKGROUND_CARD_BLUR_PERCENT
import com.melox.player.model.DEFAULT_CUSTOM_BACKGROUND_CARD_OPACITY_PERCENT
import com.melox.player.model.normalizeCustomBackgroundCardOpacityPercent
import com.melox.player.model.resolveStoredNavigationStyle
import android.os.Build
import com.melox.player.model.availableBottomBarStyle
import com.melox.player.model.ThemeStyle
import com.melox.player.model.BottomBarStyle
import com.melox.player.model.DefaultHomePage
import com.melox.player.model.DynamicColorSource
import com.melox.player.model.CoverShape
import com.melox.player.model.DynamicFlowStyle
import com.melox.player.model.LyricsFormatPriority
import com.melox.player.model.LyricsSourcePriority
import com.melox.player.model.NavigationTransitionStyle
import com.melox.player.model.PlaybackBackgroundStyle
import com.melox.player.model.normalizePlaybackSpeed
import com.melox.player.model.ThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "melox_settings",
)

/**
 * Persists appearance choices for the whole application.
 *
 * Enum names are the on-disk representation, so renaming an enum constant requires a migration.
 * Unknown values deliberately fall back to defaults to remain compatible with older app versions.
 */
class SettingsRepository(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore
    private val customBackgroundRepository = CustomBackgroundRepository(context)
    private val backgroundImageMutex = Mutex()

    val settings: Flow<AppSettings> = dataStore.data
        .catch { exception ->
            // An I/O read failure uses defaults; programming and cancellation errors still propagate.
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                themeMode = preferences[Keys.ThemeMode]
                    ?.let { storedValue ->
                        enumValueOrDefault(storedValue, ThemeMode.SYSTEM)
                    }
                    ?: ThemeMode.SYSTEM,
                dynamicColorEnabled = preferences[Keys.DynamicColorEnabled] ?: false,
                customBackgroundId = preferences[Keys.CustomBackgroundId]?.takeIf(::isCustomBackgroundId),
                customBackgroundBlurPercent = resolveCustomBackgroundBlurPercent(
                    preferences[Keys.CustomBackgroundBlurPercent],
                    preferences[Keys.LegacyCustomBackgroundBlurEnabled],
                ),
                customBackgroundDimPercent = preferences[Keys.CustomBackgroundDimPercent]
                    ?.let(::normalizeCustomBackgroundDimPercent) ?: 0,
                customBackgroundCardBlurPercent = preferences[Keys.CustomBackgroundCardBlurPercent]
                    ?.let(::normalizeCustomBackgroundCardBlurPercent) ?: DEFAULT_CUSTOM_BACKGROUND_CARD_BLUR_PERCENT,
                customBackgroundCardOpacityPercent = preferences[Keys.CustomBackgroundCardOpacityPercent]
                    ?.let(::normalizeCustomBackgroundCardOpacityPercent) ?: DEFAULT_CUSTOM_BACKGROUND_CARD_OPACITY_PERCENT,
                dynamicColorSource = preferences[Keys.DynamicColorSource]
                    ?.let { storedValue ->
                        enumValueOrDefault(storedValue, DynamicColorSource.PLAYBACK_ARTWORK)
                    }
                    ?: DynamicColorSource.PLAYBACK_ARTWORK,
                playbackBackgroundStyle = preferences[Keys.PlaybackBackgroundStyle]
                    ?.let { storedValue ->
                        enumValueOrDefault(
                            storedValue,
                            PlaybackBackgroundStyle.BLURRED_ARTWORK,
                        )
                    }
                    ?: PlaybackBackgroundStyle.BLURRED_ARTWORK,
                dynamicFlowStyle = preferences[Keys.DynamicFlowStyle]
                    ?.let { enumValueOrDefault(it, DynamicFlowStyle.SYSTEM) }
                    ?: DynamicFlowStyle.SYSTEM,
                showSongListArtwork = preferences[Keys.ShowSongListArtwork] ?: true,
                coverShape = preferences[Keys.CoverShape]
                    ?.let { enumValueOrDefault(it, CoverShape.DEFAULT) }
                    ?: if (preferences[Keys.CircularArtwork] == true) {
                        CoverShape.CIRCLE
                    } else {
                        CoverShape.DEFAULT
                    },
                artworkFlipAnimation = preferences[Keys.ArtworkFlipAnimation] ?: false,
                showSongListFileName = preferences[Keys.ShowSongListFileName] ?: false,
                showSongListDuration = preferences[Keys.ShowSongListDuration] ?: true,
                playbackSpeed = preferences[Keys.PlaybackSpeed]
                    ?.let(::normalizePlaybackSpeed) ?: 1f,
                highPrecisionOutput = preferences[Keys.HighPrecisionOutput] ?: false,
                sleepTimerSeconds = preferences[Keys.SleepTimerSeconds]
                    ?.coerceIn(0, 86_399) ?: 600,
                autoExtendSleepTimer = preferences[Keys.AutoExtendSleepTimer] ?: false,
                playbackPauseFade = preferences[Keys.PlaybackPauseFade] ?: false,
                audioFocusEnabled = preferences[Keys.AudioFocusEnabled] ?: true,
                transientAudioFocusBehavior = preferences[Keys.TransientAudioFocusBehavior]
                    ?.let { enumValueOrDefault(it, TransientAudioFocusBehavior.DUCK) }
                    ?: TransientAudioFocusBehavior.DUCK,
                autoPlayOnStart = preferences[Keys.AutoPlayOnStart] ?: false,
                terminateOnTaskRemoved = preferences[Keys.TerminateOnTaskRemoved] ?: false,
                showNotificationPlaybackModeButton =
                    preferences[Keys.ShowNotificationPlaybackModeButton] ?: true,
                showNotificationCloseButton = preferences[Keys.ShowNotificationCloseButton] ?: true,
                lyricFontScale = preferences[Keys.LyricFontScale]
                    ?.coerceIn(MIN_LYRIC_FONT_SCALE, MAX_LYRIC_FONT_SCALE)
                    ?: preferences[Keys.LegacyLyricFontScale]
                        ?.let(::migrateLegacyLyricFontScale)
                    ?: DEFAULT_LYRIC_FONT_SCALE,
                lyricFontWeight = preferences[Keys.LyricFontWeight]
                    ?.let(::normalizeLyricFontWeight)
                    ?: DEFAULT_LYRIC_FONT_WEIGHT,
                lyricAnimationMode = resolveLyricAnimationMode(
                    preferences[Keys.LyricAnimationMode],
                    preferences[Keys.LegacyForceWordByWordLyrics],
                ),
                lyricBlurEnabled = preferences[Keys.LyricBlurEnabled] ?: false,
                centerLyrics = preferences[Keys.CenterLyrics] ?: false,
                leftAlignPlayerTitle = preferences[Keys.LeftAlignPlayerTitle] ?: false,
                hideControlsOnLyrics = preferences[Keys.HideControlsOnLyrics] ?: false,
                showLyricsTranslation = preferences[Keys.ShowLyricsTranslation] ?: true,
                showLyricsRomanization = preferences[Keys.ShowLyricsRomanization] ?: true,
                lyricsTranslationLanguage = preferences[Keys.LyricsTranslationLanguage]
                    ?.let { enumValueOrDefault(it, LyricsTranslationLanguage.AUTO) }
                    ?: LyricsTranslationLanguage.AUTO,
                showMiniPlayerLyrics = preferences[Keys.ShowMiniPlayerLyrics] ?: true,
                showArtworkLyrics = preferences[Keys.ShowArtworkLyrics] ?: false,
                showMusicTagEditor = preferences[Keys.ShowMusicTagEditor] ?: true,
                showLyricoEditor = preferences[Keys.ShowLyricoEditor] ?: true,
                showLunaBeatEditor = preferences[Keys.ShowLunaBeatEditor] ?: true,
                lyricsSourcePriority = preferences[Keys.LyricsSourcePriority]
                    ?.let { storedValue ->
                        enumValueOrDefault(storedValue, LyricsSourcePriority.EMBEDDED)
                    }
                    ?: LyricsSourcePriority.EMBEDDED,
                lyricsFormatPriority = preferences[Keys.LyricsFormatPriority]
                    ?.let { storedValue ->
                        enumValueOrDefault(storedValue, LyricsFormatPriority.LRC)
                    }
                    ?: LyricsFormatPriority.LRC,
                themeStyle = preferences[Keys.ThemeStyle]
                    ?.let { enumValueOrDefault(it, if (Build.VERSION.SDK_INT >= 33) ThemeStyle.OS4 else ThemeStyle.OS3) }
                    ?.takeUnless { it == ThemeStyle.OS4 && Build.VERSION.SDK_INT < 33 }
                    ?: if (Build.VERSION.SDK_INT >= 33) ThemeStyle.OS4 else ThemeStyle.OS3,
                selectedBottomBarStyle = if (preferences[Keys.NavigationBarStyle] == null &&
                    preferences[Keys.BottomBarStyle] == null && preferences[Keys.FloatingBottomBar] == null
                ) {
                    if (Build.VERSION.SDK_INT >= 33 && preferences[Keys.ThemeStyle] != ThemeStyle.OS3.name)
                        BottomBarStyle.OS4 else BottomBarStyle.NORMAL
                } else resolveStoredNavigationStyle(
                    stored = preferences[Keys.NavigationBarStyle],
                    legacyStyle = preferences[Keys.BottomBarStyle],
                    floating = preferences[Keys.FloatingBottomBar],
                ).let { availableBottomBarStyle(it, Build.VERSION.SDK_INT >= 33) },
                blurEnabled = preferences[Keys.BlurEnabled] ?: true,
                progressiveTopBarBlurEnabled =
                    preferences[Keys.ProgressiveTopBarBlurEnabled] ?: false,
                smallPlayerBar = preferences[Keys.SmallPlayerBar] ?: false,
                hideBottomBar = preferences[Keys.HideBottomBar] ?: false,
                immersiveMode = preferences[Keys.ImmersiveMode] ?: false,
                hideStatusBar = preferences[Keys.HideStatusBar] ?: false,
                floatingBottomBar = preferences[Keys.FloatingBottomBar]
                    ?: (preferences[Keys.BottomBarStyle] != null &&
                        preferences[Keys.BottomBarStyle] != BottomBarStyle.NORMAL.name),
                navigationRailExpanded = preferences[Keys.NavigationRailExpanded] ?: true,
                liquidGlass = preferences[Keys.LiquidGlass]
                    ?: (preferences[Keys.BottomBarStyle] == BottomBarStyle.LIQUID_GLASS.name),
                predictiveBackEnabled = preferences[Keys.PredictiveBackEnabled] ?: true,
                navigationTransitionStyle = preferences[Keys.NavigationTransitionStyle]
                    ?.let { storedValue ->
                        enumValueOrDefault(storedValue, NavigationTransitionStyle.MIUIX)
                    }
                    ?: NavigationTransitionStyle.MIUIX,
                hapticFeedbackEnabled = preferences[Keys.HapticFeedbackEnabled]
                    ?: preferences[Keys.LegacyTabSwitchHapticsEnabled] ?: true,
                songListPullToRefresh = preferences[Keys.SongListPullToRefresh] ?: true,
                refreshLibraryOnStart = preferences[Keys.RefreshLibraryOnStart] ?: false,
                skipShortAudio = preferences[Keys.SkipShortAudio] ?: false,
                customFolderUris = preferences[Keys.CustomFolderUris]
                    ?.toList()
                    ?.sorted()
                    ?: emptyList(),
                blockedFolderPaths = preferences[Keys.BlockedFolderPaths]
                    ?.toList()
                    ?.mapNotNull { it.trim().takeIf(String::isNotEmpty) }
                    ?.distinctBy { it.lowercase() }
                    ?.sorted()
                    ?: emptyList(),
                hiddenTrackUris = preferences[Keys.HiddenTrackUris].orEmpty(),
                libraryTabIndex = preferences[Keys.LibraryTabIndex]
                    ?.coerceIn(0, LIBRARY_TAB_COUNT - 1)
                    ?: 0,
                musicSortFieldOrdinal = preferences[Keys.MusicSortField]
                    ?.coerceIn(MusicSortField.entries.indices)
                    ?: MusicSortField.TITLE.ordinal,
                musicSortDescending = preferences[Keys.MusicSortDescending] ?: false,
                albumSortFieldOrdinal = preferences[Keys.AlbumSortField]
                    ?.coerceIn(AlbumSortField.entries.indices)
                    ?: AlbumSortField.ALBUM.ordinal,
                albumSortDescending = preferences[Keys.AlbumSortDescending] ?: false,
                albumGridStyleOrdinal = resolveAlbumGridStyleOrdinal(
                    storedStyleOrdinal = preferences[Keys.AlbumGridStyle],
                    legacyColumns = preferences[Keys.AlbumGridColumns],
                ),
                artistSortFieldOrdinal = preferences[Keys.ArtistSortField]
                    ?.coerceIn(ArtistSortField.entries.indices)
                    ?: ArtistSortField.NAME.ordinal,
                artistSortDescending = preferences[Keys.ArtistSortDescending] ?: false,
                folderSortFieldOrdinal = preferences[Keys.FolderSortField]
                    ?.coerceIn(FolderSortField.entries.indices)
                    ?: FolderSortField.NAME.ordinal,
                folderSortDescending = preferences[Keys.FolderSortDescending] ?: false,
                folderTrackSortConfigs = decodeFolderTrackSortConfigs(
                    preferences[Keys.FolderTrackSortConfigs].orEmpty(),
                ),
                playlistSortConfigs = decodePlaylistSortConfigs(
                    preferences[Keys.PlaylistSortConfigs].orEmpty(),
                ),
                defaultHomePage = preferences[Keys.DefaultHomePage]
                    ?.let { storedValue ->
                        enumValueOrDefault(storedValue, DefaultHomePage.HOME)
                    }
                    ?: DefaultHomePage.HOME,
            )
        }

    suspend fun loadSettings(): AppSettings = settings.first()

    suspend fun setCustomBackground(uri: Uri): Boolean = backgroundImageMutex.withLock {
        val id = customBackgroundRepository.importImage(uri) ?: return@withLock false
        withContext(NonCancellable) {
            var previousId: String? = null
            try {
                dataStore.edit { preferences ->
                    previousId = preferences[Keys.CustomBackgroundId]
                    preferences[Keys.CustomBackgroundId] = id
                }
            } catch (error: CancellationException) {
                customBackgroundRepository.deleteImage(id)
                throw error
            } catch (_: IOException) {
                customBackgroundRepository.deleteImage(id)
                return@withContext false
            }
            previousId?.let { customBackgroundRepository.deleteImage(it) }
            true
        }
    }

    suspend fun deleteCustomBackground(): Boolean = backgroundImageMutex.withLock {
        withContext(NonCancellable) {
            var previousId: String? = null
            try {
                dataStore.edit { preferences ->
                    previousId = preferences.remove(Keys.CustomBackgroundId)
                    preferences.remove(Keys.LegacyCustomBackgroundBlurEnabled)
                    preferences.remove(Keys.CustomBackgroundBlurPercent)
                    preferences.remove(Keys.CustomBackgroundDimPercent)
                    preferences.remove(Keys.CustomBackgroundCardBlurPercent)
                    preferences.remove(Keys.CustomBackgroundCardOpacityPercent)
                }
            } catch (_: IOException) {
                return@withContext false
            }
            previousId?.let { customBackgroundRepository.deleteImage(it) }
            true
        }
    }

    suspend fun setCustomBackgroundBlurPercent(percent: Int) {
        dataStore.edit {
            it[Keys.CustomBackgroundBlurPercent] = normalizeCustomBackgroundBlurPercent(percent)
            it.remove(Keys.LegacyCustomBackgroundBlurEnabled)
        }
    }

    suspend fun setCustomBackgroundDimPercent(percent: Int) {
        dataStore.edit { it[Keys.CustomBackgroundDimPercent] = normalizeCustomBackgroundDimPercent(percent) }
    }

    suspend fun setCustomBackgroundCardBlurPercent(percent: Int) {
        dataStore.edit { it[Keys.CustomBackgroundCardBlurPercent] = normalizeCustomBackgroundCardBlurPercent(percent) }
    }

    suspend fun setCustomBackgroundCardOpacityPercent(percent: Int) {
        dataStore.edit { it[Keys.CustomBackgroundCardOpacityPercent] = normalizeCustomBackgroundCardOpacityPercent(percent) }
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[Keys.ThemeMode] = themeMode.name
        }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.DynamicColorEnabled] = enabled
        }
    }

    suspend fun setDynamicColorSource(source: DynamicColorSource) {
        dataStore.edit { preferences ->
            preferences[Keys.DynamicColorSource] = source.name
        }
    }

    suspend fun setPlaybackBackgroundStyle(style: PlaybackBackgroundStyle) {
        dataStore.edit { preferences ->
            preferences[Keys.PlaybackBackgroundStyle] = style.name
        }
    }

    suspend fun setDynamicFlowStyle(style: DynamicFlowStyle) {
        dataStore.edit { preferences ->
            preferences[Keys.DynamicFlowStyle] = style.name
        }
    }

    suspend fun setShowSongListArtwork(enabled: Boolean) {
        dataStore.edit { it[Keys.ShowSongListArtwork] = enabled }
    }

    suspend fun setCircularArtwork(enabled: Boolean) {
        setCoverShape(if (enabled) CoverShape.CIRCLE else CoverShape.DEFAULT)
    }

    suspend fun setCoverShape(shape: CoverShape) {
        dataStore.edit { preferences ->
            preferences[Keys.CoverShape] = shape.name
        }
    }

    suspend fun setArtworkFlipAnimation(enabled: Boolean) {
        dataStore.edit { it[Keys.ArtworkFlipAnimation] = enabled }
    }

    suspend fun setShowSongListFileName(enabled: Boolean) {
        dataStore.edit { it[Keys.ShowSongListFileName] = enabled }
    }

    suspend fun setShowSongListDuration(enabled: Boolean) {
        dataStore.edit { it[Keys.ShowSongListDuration] = enabled }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        dataStore.edit { it[Keys.PlaybackSpeed] = normalizePlaybackSpeed(speed) }
    }

    suspend fun setHighPrecisionOutput(enabled: Boolean) {
        dataStore.edit { it[Keys.HighPrecisionOutput] = enabled }
    }

    suspend fun setSleepTimerSeconds(seconds: Int) {
        dataStore.edit { it[Keys.SleepTimerSeconds] = seconds.coerceIn(0, 86_399) }
    }

    suspend fun setAutoExtendSleepTimer(enabled: Boolean) {
        dataStore.edit { it[Keys.AutoExtendSleepTimer] = enabled }
    }

    suspend fun setPlaybackPauseFade(enabled: Boolean) {
        dataStore.edit { it[Keys.PlaybackPauseFade] = enabled }
    }

    suspend fun setAutoPlayOnStart(enabled: Boolean) {
        dataStore.edit { it[Keys.AutoPlayOnStart] = enabled }
    }

    suspend fun setAudioFocusEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AudioFocusEnabled] = enabled }
    }

    suspend fun setTransientAudioFocusBehavior(behavior: TransientAudioFocusBehavior) {
        dataStore.edit { it[Keys.TransientAudioFocusBehavior] = behavior.name }
    }

    suspend fun setTerminateOnTaskRemoved(enabled: Boolean) {
        dataStore.edit { it[Keys.TerminateOnTaskRemoved] = enabled }
    }

    suspend fun setShowNotificationPlaybackModeButton(enabled: Boolean) {
        dataStore.edit { it[Keys.ShowNotificationPlaybackModeButton] = enabled }
    }

    suspend fun setShowNotificationCloseButton(enabled: Boolean) {
        dataStore.edit { it[Keys.ShowNotificationCloseButton] = enabled }
    }

    suspend fun setLyricFontScale(scale: Float) {
        dataStore.edit { preferences ->
            preferences[Keys.LyricFontScale] = scale.coerceIn(
                MIN_LYRIC_FONT_SCALE,
                MAX_LYRIC_FONT_SCALE,
            )
        }
    }

    suspend fun setLyricFontWeight(weight: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.LyricFontWeight] = normalizeLyricFontWeight(weight)
        }
    }

    suspend fun setLyricAnimationMode(mode: LyricAnimationMode) {
        dataStore.edit { preferences ->
            preferences[Keys.LyricAnimationMode] = mode.name
            preferences.remove(Keys.LegacyForceWordByWordLyrics)
        }
    }

    suspend fun setLyricBlurEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.LyricBlurEnabled] = enabled
        }
    }

    suspend fun setCenterLyrics(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.CenterLyrics] = enabled
        }
    }

    suspend fun setLeftAlignPlayerTitle(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.LeftAlignPlayerTitle] = enabled
        }
    }

    suspend fun setHideControlsOnLyrics(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.HideControlsOnLyrics] = enabled
        }
    }

    suspend fun setShowMiniPlayerLyrics(enabled: Boolean) {
        dataStore.edit { it[Keys.ShowMiniPlayerLyrics] = enabled }
    }

    suspend fun setShowArtworkLyrics(enabled: Boolean) {
        dataStore.edit { it[Keys.ShowArtworkLyrics] = enabled }
    }

    suspend fun setShowLyricsTranslation(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.ShowLyricsTranslation] = enabled
        }
    }

    suspend fun setLyricsTranslationLanguage(language: LyricsTranslationLanguage) {
        dataStore.edit { it[Keys.LyricsTranslationLanguage] = language.name }
    }

    suspend fun setShowLyricsRomanization(enabled: Boolean) {
        dataStore.edit { it[Keys.ShowLyricsRomanization] = enabled }
    }

    suspend fun setLyricsSourcePriority(priority: LyricsSourcePriority) {
        dataStore.edit { preferences ->
            preferences[Keys.LyricsSourcePriority] = priority.name
        }
    }

    suspend fun setShowMusicTagEditor(enabled: Boolean) {
        dataStore.edit { it[Keys.ShowMusicTagEditor] = enabled }
    }

    suspend fun setShowLyricoEditor(enabled: Boolean) {
        dataStore.edit { it[Keys.ShowLyricoEditor] = enabled }
    }

    suspend fun setShowLunaBeatEditor(enabled: Boolean) {
        dataStore.edit { it[Keys.ShowLunaBeatEditor] = enabled }
    }

    suspend fun setLyricsFormatPriority(priority: LyricsFormatPriority) {
        dataStore.edit { preferences ->
            preferences[Keys.LyricsFormatPriority] = priority.name
        }
    }

    suspend fun setThemeStyle(style: ThemeStyle) {
        if (style == ThemeStyle.OS4 && Build.VERSION.SDK_INT < 33) return
        dataStore.edit { preferences ->
            preferences[Keys.ThemeStyle] = style.name
            preferences[Keys.NavigationBarStyle] = style.defaultBottomBarStyle.name
        }
    }

    suspend fun setBottomBarStyle(bottomBarStyle: BottomBarStyle) {
        if (availableBottomBarStyle(bottomBarStyle, Build.VERSION.SDK_INT >= 33) != bottomBarStyle) return
        dataStore.edit { preferences ->
            preferences[Keys.NavigationBarStyle] = bottomBarStyle.name
            preferences[Keys.FloatingBottomBar] = bottomBarStyle != BottomBarStyle.NORMAL
            preferences[Keys.LiquidGlass] = bottomBarStyle == BottomBarStyle.LIQUID_GLASS
        }
    }

    suspend fun setBlurEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.BlurEnabled] = enabled
        }
    }

    suspend fun setProgressiveTopBarBlurEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.ProgressiveTopBarBlurEnabled] = enabled
        }
    }

    suspend fun setSmallPlayerBar(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.SmallPlayerBar] = enabled
        }
    }

    suspend fun setHideBottomBar(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.HideBottomBar] = enabled
        }
    }

    suspend fun setImmersiveMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.ImmersiveMode] = enabled
        }
    }

    suspend fun setHideStatusBar(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.HideStatusBar] = enabled
        }
    }

    suspend fun setNavigationRailExpanded(expanded: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.NavigationRailExpanded] = expanded
        }
    }

    suspend fun setPredictiveBackEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.PredictiveBackEnabled] = enabled
        }
    }

    suspend fun setNavigationTransitionStyle(style: NavigationTransitionStyle) {
        dataStore.edit { preferences ->
            preferences[Keys.NavigationTransitionStyle] = style.name
        }
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.HapticFeedbackEnabled] = enabled
        }
    }

    suspend fun setSongListPullToRefresh(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.SongListPullToRefresh] = enabled
        }
    }

    suspend fun setRefreshLibraryOnStart(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.RefreshLibraryOnStart] = enabled
        }
    }

    suspend fun setSkipShortAudio(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.SkipShortAudio] = enabled
        }
    }

    suspend fun addCustomFolderUri(uri: String) {
        dataStore.edit { preferences ->
            preferences[Keys.CustomFolderUris] =
                preferences[Keys.CustomFolderUris].orEmpty() + uri
        }
    }

    suspend fun removeCustomFolderUri(uri: String) {
        dataStore.edit { preferences ->
            preferences[Keys.CustomFolderUris] =
                preferences[Keys.CustomFolderUris].orEmpty() - uri
        }
    }

    suspend fun setBlockedFolderPaths(paths: List<String>) {
        dataStore.edit { preferences ->
            preferences[Keys.BlockedFolderPaths] = paths.toSet()
        }
    }

    suspend fun setHiddenTrackUris(contentUris: Set<String>) {
        dataStore.edit { preferences ->
            preferences[Keys.HiddenTrackUris] = contentUris
        }
    }

    suspend fun setDefaultHomePage(defaultHomePage: DefaultHomePage) {
        dataStore.edit { preferences ->
            preferences[Keys.DefaultHomePage] = defaultHomePage.name
        }
    }

    suspend fun setLibraryTabIndex(index: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.LibraryTabIndex] = index.coerceIn(0, LIBRARY_TAB_COUNT - 1)
        }
    }

    suspend fun setMusicSortConfig(config: MusicSortConfig) {
        dataStore.edit { preferences ->
            preferences[Keys.MusicSortField] = config.field.ordinal
            preferences[Keys.MusicSortDescending] = config.descending
        }
    }

    suspend fun setAlbumSortConfig(config: AlbumSortConfig) {
        dataStore.edit { preferences ->
            preferences[Keys.AlbumSortField] = config.field.ordinal
            preferences[Keys.AlbumSortDescending] = config.descending
            preferences[Keys.AlbumGridStyle] = config.gridStyle.ordinal
            preferences[Keys.AlbumGridColumns] = config.gridStyle.columns
        }
    }

    suspend fun setArtistSortConfig(config: ArtistSortConfig) {
        dataStore.edit { preferences ->
            preferences[Keys.ArtistSortField] = config.field.ordinal
            preferences[Keys.ArtistSortDescending] = config.descending
        }
    }

    suspend fun setFolderSortConfig(config: FolderSortConfig) {
        dataStore.edit { preferences ->
            preferences[Keys.FolderSortField] = config.field.ordinal
            preferences[Keys.FolderSortDescending] = config.descending
        }
    }

    suspend fun setFolderTrackSortConfig(folderKey: String, config: MusicSortConfig) {
        dataStore.edit { preferences ->
            val configs = decodeFolderTrackSortConfigs(
                preferences[Keys.FolderTrackSortConfigs].orEmpty(),
            ).toMutableMap()
            if (config == MusicSortConfig()) configs.remove(folderKey)
            else configs[folderKey] = config
            preferences[Keys.FolderTrackSortConfigs] = encodeFolderTrackSortConfigs(configs)
        }
    }

    suspend fun setPlaylistSortConfig(playlistId: String, config: PlaylistSortConfig) {
        dataStore.edit { preferences ->
            val configs = decodePlaylistSortConfigs(
                preferences[Keys.PlaylistSortConfigs].orEmpty(),
            ).toMutableMap()
            if (config == PlaylistSortConfig()) configs.remove(playlistId)
            else configs[playlistId] = config
            preferences[Keys.PlaylistSortConfigs] = encodePlaylistSortConfigs(configs)
        }
    }

    private object Keys {
        val ThemeMode = stringPreferencesKey("theme_mode")
        val DynamicColorEnabled = booleanPreferencesKey("dynamic_color_enabled")
        val CustomBackgroundId = stringPreferencesKey("custom_background_id")
        val LegacyCustomBackgroundBlurEnabled = booleanPreferencesKey("custom_background_blur_enabled")
        val CustomBackgroundBlurPercent = intPreferencesKey("custom_background_blur_percent")
        val CustomBackgroundDimPercent = intPreferencesKey("custom_background_dim_percent")
        val CustomBackgroundCardBlurPercent = intPreferencesKey("custom_background_card_blur_percent")
        val CustomBackgroundCardOpacityPercent = intPreferencesKey("custom_background_card_opacity_percent")
        val DynamicColorSource = stringPreferencesKey("dynamic_color_source")
        val PlaybackBackgroundStyle = stringPreferencesKey("playback_background_style")
        val DynamicFlowStyle = stringPreferencesKey("dynamic_flow_style")
        val ShowSongListArtwork = booleanPreferencesKey("show_song_list_artwork")
        val CircularArtwork = booleanPreferencesKey("circular_artwork")
        val CoverShape = stringPreferencesKey("cover_shape")
        val ArtworkFlipAnimation = booleanPreferencesKey("artwork_flip_animation")
        val ShowSongListFileName = booleanPreferencesKey("show_song_list_file_name")
        val ShowSongListDuration = booleanPreferencesKey("show_song_list_duration")
        val PlaybackSpeed = floatPreferencesKey("playback_speed")
        val HighPrecisionOutput = booleanPreferencesKey("high_precision_output")
        val SleepTimerSeconds = intPreferencesKey("sleep_timer_seconds")
        val AutoExtendSleepTimer = booleanPreferencesKey("auto_extend_sleep_timer")
        val PlaybackPauseFade = booleanPreferencesKey("playback_pause_fade")
        val AudioFocusEnabled = booleanPreferencesKey("audio_focus_enabled")
        val TransientAudioFocusBehavior = stringPreferencesKey("transient_audio_focus_behavior")
        val AutoPlayOnStart = booleanPreferencesKey("auto_play_on_start")
        val TerminateOnTaskRemoved = booleanPreferencesKey("terminate_on_task_removed")
        val ShowNotificationPlaybackModeButton =
            booleanPreferencesKey("show_notification_playback_mode_button")
        val ShowNotificationCloseButton =
            booleanPreferencesKey("show_notification_close_button")
        val LyricFontScale = floatPreferencesKey("lyric_font_scale_v2")
        val LegacyLyricFontScale = floatPreferencesKey("lyric_font_scale")
        val LyricFontWeight = intPreferencesKey("lyric_font_weight")
        val LyricAnimationMode = stringPreferencesKey("lyric_animation_mode")
        val LegacyForceWordByWordLyrics = booleanPreferencesKey("force_word_by_word_lyrics")
        val LyricBlurEnabled = booleanPreferencesKey("lyric_blur_enabled")
        val CenterLyrics = booleanPreferencesKey("center_lyrics")
        val LeftAlignPlayerTitle = booleanPreferencesKey("left_align_player_title")
        val HideControlsOnLyrics = booleanPreferencesKey("hide_controls_on_lyrics")
        val ShowLyricsTranslation = booleanPreferencesKey("show_lyrics_translation")
        val ShowLyricsRomanization = booleanPreferencesKey("show_lyrics_romanization")
        val LyricsTranslationLanguage = stringPreferencesKey("lyrics_translation_language")
        val ShowMiniPlayerLyrics = booleanPreferencesKey("show_mini_player_lyrics")
        val ShowArtworkLyrics = booleanPreferencesKey("show_artwork_lyrics")
        val ShowMusicTagEditor = booleanPreferencesKey("show_music_tag_editor")
        val ShowLyricoEditor = booleanPreferencesKey("show_lyrico_editor")
        val ShowLunaBeatEditor = booleanPreferencesKey("show_luna_beat_editor")
        val LyricsSourcePriority = stringPreferencesKey("lyrics_source_priority")
        // Keep the existing key so saved external-format preferences apply to both sources.
        val LyricsFormatPriority = stringPreferencesKey("lyrics_sidecar_format_priority")
        val ThemeStyle = stringPreferencesKey("theme_style")
        val NavigationBarStyle = stringPreferencesKey("navigation_bar_style")
        val BottomBarStyle = stringPreferencesKey("bottom_bar_style")
        val BlurEnabled = booleanPreferencesKey("blur_enabled")
        val ProgressiveTopBarBlurEnabled =
            booleanPreferencesKey("progressive_top_bar_blur_enabled")
        val FloatingBottomBar = booleanPreferencesKey("floating_bottom_bar")
        val NavigationRailExpanded = booleanPreferencesKey("navigation_rail_expanded")
        val SmallPlayerBar = booleanPreferencesKey("small_player_bar")
        val HideBottomBar = booleanPreferencesKey("hide_bottom_bar")
        val ImmersiveMode = booleanPreferencesKey("immersive_mode")
        val HideStatusBar = booleanPreferencesKey("hide_status_bar")
        val LiquidGlass = booleanPreferencesKey("liquid_glass")
        val PredictiveBackEnabled = booleanPreferencesKey("predictive_back_enabled")
        val NavigationTransitionStyle = stringPreferencesKey("navigation_transition_style")
        val HapticFeedbackEnabled = booleanPreferencesKey("haptic_feedback_enabled")
        val LegacyTabSwitchHapticsEnabled = booleanPreferencesKey("tab_switch_haptics_enabled")
        val SongListPullToRefresh = booleanPreferencesKey("song_list_pull_to_refresh")
        val RefreshLibraryOnStart = booleanPreferencesKey("refresh_library_on_start")
        val SkipShortAudio = booleanPreferencesKey("skip_short_audio")
        val CustomFolderUris = stringSetPreferencesKey("custom_folder_uris")
        val BlockedFolderPaths = stringSetPreferencesKey("blocked_folder_paths")
        val HiddenTrackUris = stringSetPreferencesKey("hidden_track_uris")
        val DefaultHomePage = stringPreferencesKey("default_home_page")
        val LibraryTabIndex = intPreferencesKey("library_tab_index")
        val MusicSortField = intPreferencesKey("music_sort_field")
        val MusicSortDescending = booleanPreferencesKey("music_sort_descending")
        val AlbumSortField = intPreferencesKey("album_sort_field")
        val AlbumSortDescending = booleanPreferencesKey("album_sort_descending")
        val AlbumGridStyle = intPreferencesKey("album_grid_style")
        val AlbumGridColumns = intPreferencesKey("album_grid_columns")
        val ArtistSortField = intPreferencesKey("artist_sort_field")
        val ArtistSortDescending = booleanPreferencesKey("artist_sort_descending")
        val FolderSortField = intPreferencesKey("folder_sort_field")
        val FolderSortDescending = booleanPreferencesKey("folder_sort_descending")
        val FolderTrackSortConfigs = stringSetPreferencesKey("folder_track_sort_configs")
        val PlaylistSortConfigs = stringSetPreferencesKey("playlist_sort_configs")
    }
}

internal fun encodeFolderTrackSortConfigs(configs: Map<String, MusicSortConfig>): Set<String> =
    configs.mapTo(mutableSetOf()) { (key, config) ->
        "${config.field.name}|${config.descending}|$key"
    }

internal fun decodeFolderTrackSortConfigs(stored: Set<String>): Map<String, MusicSortConfig> =
    stored.mapNotNull { value ->
        val parts = value.split('|', limit = 3)
        if (parts.size != 3) return@mapNotNull null
        val field = MusicSortField.entries.firstOrNull { it.name == parts[0] }
            ?: return@mapNotNull null
        val descending = parts[1].toBooleanStrictOrNull() ?: return@mapNotNull null
        parts[2] to MusicSortConfig(field, descending)
    }.toMap()

internal fun encodePlaylistSortConfigs(configs: Map<String, PlaylistSortConfig>): Set<String> =
    configs.mapTo(mutableSetOf()) { (id, config) ->
        "${config.field.name}|${config.descending}|$id"
    }

internal fun decodePlaylistSortConfigs(stored: Set<String>): Map<String, PlaylistSortConfig> =
    stored.mapNotNull { value ->
        val parts = value.split('|', limit = 3)
        if (parts.size != 3 || parts[2].isBlank()) return@mapNotNull null
        val field = PlaylistSortField.entries.firstOrNull { it.name == parts[0] }
            ?: return@mapNotNull null
        val descending = parts[1].toBooleanStrictOrNull() ?: return@mapNotNull null
        parts[2] to PlaylistSortConfig(field, descending)
    }.toMap()

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
    enumValues<T>().firstOrNull { enumValue -> enumValue.name == value } ?: default

internal fun resolveAlbumGridStyleOrdinal(
    storedStyleOrdinal: Int?,
    legacyColumns: Int?,
): Int = when {
    storedStyleOrdinal == AlbumGridStyle.TWO_SMALL.ordinal -> AlbumGridStyle.TWO_SMALL.ordinal
    storedStyleOrdinal == LEGACY_ALBUM_GRID_THREE_ORDINAL -> AlbumGridStyle.THREE.ordinal
    legacyColumns == AlbumGridStyle.THREE.columns -> AlbumGridStyle.THREE.ordinal
    else -> AlbumGridStyle.TWO_SMALL.ordinal
}

internal fun migrateLegacyLyricFontScale(storedScale: Float): Float =
    (storedScale / LEGACY_LYRIC_FONT_BASE_SCALE).coerceIn(
        MIN_LYRIC_FONT_SCALE,
        MAX_LYRIC_FONT_SCALE,
    )

internal fun normalizeLyricFontWeight(weight: Int): Int =
    ((weight.coerceIn(MIN_LYRIC_FONT_WEIGHT, MAX_LYRIC_FONT_WEIGHT) +
        LYRICS_FONT_WEIGHT_STEP / 2) / LYRICS_FONT_WEIGHT_STEP) * LYRICS_FONT_WEIGHT_STEP

private const val LIBRARY_TAB_COUNT = 3
private const val LEGACY_ALBUM_GRID_THREE_ORDINAL = 2
private const val LEGACY_LYRIC_FONT_BASE_SCALE = 0.8f
private const val MIN_LYRIC_FONT_SCALE = 0.6666667f
private const val MAX_LYRIC_FONT_SCALE = 2f
private const val DEFAULT_LYRIC_FONT_SCALE = 1f
private const val MIN_LYRIC_FONT_WEIGHT = 100
private const val MAX_LYRIC_FONT_WEIGHT = 900
private const val DEFAULT_LYRIC_FONT_WEIGHT = 400
private const val LYRICS_FONT_WEIGHT_STEP = 100
