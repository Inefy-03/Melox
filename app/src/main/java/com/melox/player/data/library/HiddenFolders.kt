package com.melox.player.data.library

import com.melox.player.model.MusicTrack
import java.util.Locale

internal fun normalizeHiddenFolderPath(path: String): String =
    path.trim().replace('\\', '/').trimEnd('/').ifEmpty { "/" }

internal fun normalizedHiddenFolderPaths(paths: List<String>): List<String> =
    paths.map(::normalizeHiddenFolderPath).distinctBy { it.lowercase(Locale.ROOT) }

internal fun isMusicFolderHidden(path: String?, hiddenPaths: List<String>): Boolean {
    val normalized = path?.let(::normalizeHiddenFolderPath) ?: return false
    return hiddenPaths.any { prefix ->
        prefix == "/" || normalized.equals(prefix, ignoreCase = true) ||
            normalized.startsWith("$prefix/", ignoreCase = true)
    }
}

internal fun visibleMusicTracks(
    tracks: List<MusicTrack>,
    hiddenPaths: List<String>,
): List<MusicTrack> {
    if (hiddenPaths.isEmpty()) return tracks
    val prefixes = normalizedHiddenFolderPaths(hiddenPaths)
    val hiddenByFolder = HashMap<Pair<String?, String>, Boolean>()
    return tracks.filterNot { track ->
        hiddenByFolder.getOrPut(musicFolderIdentity(track)) {
            isMusicFolderHidden(fullMusicFolderPath(track), prefixes) ||
                isMusicFolderHidden(track.folderPath, prefixes)
        }
    }
}

internal fun hiddenFolderDisplayPaths(
    tracks: List<MusicTrack>,
    paths: List<String>,
): Map<String, String> {
    if (paths.isEmpty()) return emptyMap()
    val folders = tracks.distinctBy(::musicFolderIdentity)
    return paths.associateWith { path ->
        if (path.startsWith("/storage/") || path.startsWith("/mnt/") ||
            path == "/sdcard" || path.startsWith("/sdcard/")
        ) {
            path
        } else {
            folders.mapNotNull { track ->
                val relativePath = track.folderPath ?: return@mapNotNull null
                if (!isMusicFolderHidden(relativePath, listOf(path))) return@mapNotNull null
                val fullPath = fullMusicFolderPath(track) ?: return@mapNotNull null
                if (fullPath == relativePath) return@mapNotNull null
                val root = fullPath.removeSuffix(relativePath.trimEnd('/'))
                root + path.trimEnd('/').takeUnless { it == "/" }.orEmpty()
            }.distinct().sorted().joinToString("\n").ifEmpty {
                "/storage/emulated/0" + path.trimEnd('/').takeUnless { it == "/" }.orEmpty()
            }
        }
    }
}
