package com.melox.player.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
                        enumValueOrDefault(storedValue, DynamicColorSource.DESKTOP)
                    }
                    ?: DynamicColorSource.DESKTOP,
                blurEnabled = preferences[Keys.BlurEnabled] ?: true,
                floatingBottomBar = preferences[Keys.FloatingBottomBar]
                    ?: (preferences[Keys.BottomBarStyle] != null &&
                        preferences[Keys.BottomBarStyle] != BottomBarStyle.NORMAL.name),
                liquidGlass = preferences[Keys.LiquidGlass]
                    ?: (preferences[Keys.BottomBarStyle] == BottomBarStyle.LIQUID_GLASS.name),
                predictiveBackEnabled = preferences[Keys.PredictiveBackEnabled] ?: true,
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
            preferences[Keys.LiquidGlass] = enabled
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
        val BottomBarStyle = stringPreferencesKey("bottom_bar_style")
        val BlurEnabled = booleanPreferencesKey("blur_enabled")
        val FloatingBottomBar = booleanPreferencesKey("floating_bottom_bar")
        val LiquidGlass = booleanPreferencesKey("liquid_glass")
        val PredictiveBackEnabled = booleanPreferencesKey("predictive_back_enabled")
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

private const val LIBRARY_TAB_COUNT = 3
private const val LEGACY_ALBUM_GRID_THREE_ORDINAL = 2
