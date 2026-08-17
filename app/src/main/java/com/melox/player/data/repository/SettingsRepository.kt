package com.melox.player.data.repository

import android.content.Context
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
import com.melox.player.model.BottomBarStyle
import com.melox.player.model.DefaultHomePage
import com.melox.player.model.DynamicColorSource
import com.melox.player.model.PlaybackBackgroundStyle
import com.melox.player.model.ThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

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
                lyricFontScale = preferences[Keys.LyricFontScale]
                    ?.coerceIn(MIN_LYRIC_FONT_SCALE, MAX_LYRIC_FONT_SCALE)
                    ?: preferences[Keys.LegacyLyricFontScale]
                        ?.let(::migrateLegacyLyricFontScale)
                    ?: DEFAULT_LYRIC_FONT_SCALE,
                lyricFontWeight = preferences[Keys.LyricFontWeight]
                    ?.let(::normalizeLyricFontWeight)
                    ?: DEFAULT_LYRIC_FONT_WEIGHT,
                forceWordByWordLyrics = preferences[Keys.ForceWordByWordLyrics] ?: false,
                lyricBlurEnabled = preferences[Keys.LyricBlurEnabled] ?: false,
                centerLyrics = preferences[Keys.CenterLyrics] ?: false,
                hideControlsOnLyrics = preferences[Keys.HideControlsOnLyrics] ?: false,
                showLyricsTranslation = preferences[Keys.ShowLyricsTranslation] ?: true,
                blurEnabled = preferences[Keys.BlurEnabled] ?: true,
                floatingBottomBar = preferences[Keys.FloatingBottomBar]
                    ?: (preferences[Keys.BottomBarStyle] != null &&
                        preferences[Keys.BottomBarStyle] != BottomBarStyle.NORMAL.name),
                liquidGlass = preferences[Keys.LiquidGlass]
                    ?: (preferences[Keys.BottomBarStyle] == BottomBarStyle.LIQUID_GLASS.name),
                predictiveBackEnabled = preferences[Keys.PredictiveBackEnabled] ?: true,
                refreshLibraryOnStart = preferences[Keys.RefreshLibraryOnStart] ?: false,
                skipShortAudio = preferences[Keys.SkipShortAudio] ?: false,
                customFolderUris = preferences[Keys.CustomFolderUris]
                    ?.toList()
                    ?.sorted()
                    ?: emptyList(),
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
                defaultHomePage = preferences[Keys.DefaultHomePage]
                    ?.let { storedValue ->
                        enumValueOrDefault(storedValue, DefaultHomePage.HOME)
                    }
                    ?: DefaultHomePage.HOME,
            )
        }

    suspend fun loadSettings(): AppSettings = settings.first()

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

    suspend fun setForceWordByWordLyrics(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.ForceWordByWordLyrics] = enabled
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

    suspend fun setHideControlsOnLyrics(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.HideControlsOnLyrics] = enabled
        }
    }

    suspend fun setShowLyricsTranslation(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.ShowLyricsTranslation] = enabled
        }
    }

    suspend fun setBottomBarStyle(bottomBarStyle: BottomBarStyle) {
        dataStore.edit { preferences ->
            preferences[Keys.FloatingBottomBar] = bottomBarStyle != BottomBarStyle.NORMAL
            preferences[Keys.LiquidGlass] = bottomBarStyle == BottomBarStyle.LIQUID_GLASS
        }
    }

    suspend fun setBlurEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.BlurEnabled] = enabled
        }
    }

    suspend fun setFloatingBottomBar(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.FloatingBottomBar] = enabled
            preferences[Keys.LiquidGlass] = false
        }
    }

    suspend fun setLiquidGlass(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.LiquidGlass] = enabled
        }
    }

    suspend fun setPredictiveBackEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.PredictiveBackEnabled] = enabled
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

    private object Keys {
        val ThemeMode = stringPreferencesKey("theme_mode")
        val DynamicColorEnabled = booleanPreferencesKey("dynamic_color_enabled")
        val DynamicColorSource = stringPreferencesKey("dynamic_color_source")
        val PlaybackBackgroundStyle = stringPreferencesKey("playback_background_style")
        val LyricFontScale = floatPreferencesKey("lyric_font_scale_v2")
        val LegacyLyricFontScale = floatPreferencesKey("lyric_font_scale")
        val LyricFontWeight = intPreferencesKey("lyric_font_weight")
        val ForceWordByWordLyrics = booleanPreferencesKey("force_word_by_word_lyrics")
        val LyricBlurEnabled = booleanPreferencesKey("lyric_blur_enabled")
        val CenterLyrics = booleanPreferencesKey("center_lyrics")
        val HideControlsOnLyrics = booleanPreferencesKey("hide_controls_on_lyrics")
        val ShowLyricsTranslation = booleanPreferencesKey("show_lyrics_translation")
        val BottomBarStyle = stringPreferencesKey("bottom_bar_style")
        val BlurEnabled = booleanPreferencesKey("blur_enabled")
        val FloatingBottomBar = booleanPreferencesKey("floating_bottom_bar")
        val LiquidGlass = booleanPreferencesKey("liquid_glass")
        val PredictiveBackEnabled = booleanPreferencesKey("predictive_back_enabled")
        val RefreshLibraryOnStart = booleanPreferencesKey("refresh_library_on_start")
        val SkipShortAudio = booleanPreferencesKey("skip_short_audio")
        val CustomFolderUris = stringSetPreferencesKey("custom_folder_uris")
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
    }
}

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
private const val MIN_LYRIC_FONT_SCALE = 0.7f
private const val MAX_LYRIC_FONT_SCALE = 1.3f
private const val DEFAULT_LYRIC_FONT_SCALE = 1f
private const val MIN_LYRIC_FONT_WEIGHT = 100
private const val MAX_LYRIC_FONT_WEIGHT = 900
private const val DEFAULT_LYRIC_FONT_WEIGHT = 400
private const val LYRICS_FONT_WEIGHT_STEP = 100
