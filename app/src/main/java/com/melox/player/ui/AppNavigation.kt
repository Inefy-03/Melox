package com.melox.player.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.melox.player.data.library.AlbumGroup
import com.melox.player.data.library.ArtistGroup
import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavKey

internal const val HOME_TAB_INDEX = 0
internal const val SONGS_TAB_INDEX = 1
internal const val LIBRARY_TAB_INDEX = 2
internal const val SETTINGS_TAB_INDEX = 3
internal const val ROOT_TAB_COUNT = 4

internal fun rootPagerUserScrollEnabled(
    selectedPage: Int,
    homeRecommendationGestureActive: Boolean,
): Boolean = selectedPage != HOME_TAB_INDEX ||
    !homeRecommendationGestureActive

internal fun shouldTriggerTabSwitchHaptic(
    enabled: Boolean,
    selectedPage: Int,
    targetPage: Int,
): Boolean = enabled && selectedPage != targetPage

@Serializable
internal enum class AppRoute {
    ROOT,
    SCAN_SETTINGS,
    MUSIC_STATISTICS,
    PLAYBACK_BEHAVIOR,
    AUDIO_OUTPUT,
    THEME_SETTINGS,
    INTERFACE_STYLE,
    MAIN_BACKGROUND,
    LYRICS_SETTINGS,
    LYRICS_INTERFACE,
    ABOUT,
    ALBUM_DETAIL,
    ARTIST_DETAIL,
    FOLDER_DETAIL,
    INTERNAL_STORAGE,
    PLAYLISTS,
    ALBUMS,
    ARTISTS,
    FOLDERS,
    PLAYLIST_DETAIL,
    BLOCKED_FOLDERS,
    HIDDEN_SONGS,
    SPONSOR,
}

@Serializable
internal data class AppNavDestination(
    val route: AppRoute,
    val contentKey: String? = null,
    val depth: Int,
) : NavKey

// Keep miuix-nav identity tied to the stack slot so a return can swap the displayed
// detail object without changing a pop into a forward replacement transition.
internal fun AppNavDestination.entryContentKey(): String =
    if (route == AppRoute.INTERNAL_STORAGE) "melox:${route.name}:$depth:$contentKey"
    else "melox:${route.name}:$depth"

internal fun List<AppNavDestination>.missingLibraryDetailCount(
    albums: List<AlbumGroup>,
    artists: List<ArtistGroup>,
): Int = lastIndex - indexOfLast { destination ->
    when (destination.route) {
        AppRoute.ALBUM_DETAIL -> albums.any { it.key == destination.contentKey }
        AppRoute.ARTIST_DETAIL -> artists.any { it.key == destination.contentKey }
        else -> true
    }
}

/** Keep drawing the last available detail until NavDisplay removes its entry. */
@Composable
internal fun <T : Any> rememberLibraryDetailForExit(contentKey: String?, detail: T?): T? {
    var lastDetail by remember(contentKey) { mutableStateOf<T?>(null) }
    if (detail != null) {
        SideEffect { lastDetail = detail }
    }
    return detail ?: lastDetail
}

internal fun selectedSidebarShortcut(
    destinations: List<AppNavDestination>,
    sidebarRoutes: List<AppRoute>,
): Int? = destinations.asReversed()
    .firstNotNullOfOrNull { destination -> sidebarRoutes.indexOf(destination.route).takeIf { it >= 0 } }

internal fun sidebarShortcutPrimaryPage(route: AppRoute?): Int? = when (route) {
    AppRoute.PLAYLISTS -> HOME_TAB_INDEX
    AppRoute.ALBUMS, AppRoute.ARTISTS, AppRoute.FOLDERS -> LIBRARY_TAB_INDEX
    else -> null
}

internal fun synchronizeAppNavBackStack(
    backStack: NavBackStack,
    destinations: List<AppNavDestination>,
) {
    var sharedCount = 0
    while (sharedCount < backStack.size && sharedCount < destinations.size) {
        val current = backStack[sharedCount] as? AppNavDestination ?: break
        val target = destinations[sharedCount]
        if (current.entryContentKey() != target.entryContentKey()) break
        if (current != target) backStack[sharedCount] = target
        sharedCount += 1
    }
    while (backStack.size > sharedCount) backStack.removeAt(backStack.lastIndex)
    for (index in sharedCount until destinations.size) backStack.add(destinations[index])
}
