package com.melox.player.data.library

import com.melox.player.model.MusicTrack
import java.net.URI
import java.util.Locale

data class FolderGroup(
    val key: String,
    val name: String?,
    val path: String?,
    val displayPath: String,
    val tracks: List<MusicTrack>,
) {
    val coverTrack: MusicTrack?
        get() = tracks.firstOrNull()
}

enum class FolderSortField {
    NAME,
    SONG_COUNT,
}

data class FolderSortConfig(
    val field: FolderSortField = FolderSortField.NAME,
    val descending: Boolean = false,
)

internal fun normalizeMusicFolderPath(
    rawPath: String?,
    includesFileName: Boolean,
): String? {
    val normalized = rawPath
        ?.trim()
        ?.replace('\\', '/')
        ?.split('/')
        ?.filter(String::isNotBlank)
        ?.joinToString(separator = "/", prefix = "/")
        ?.takeIf { it != "/" }
        ?: return null
    if (!includesFileName) return normalized
    return normalized.substringBeforeLast('/', missingDelimiterValue = "")
        .takeIf(String::isNotEmpty)
}

internal fun musicFolderIdentity(track: MusicTrack): Pair<String?, String> =
    track.folderPath to track.contentUri.substringBefore("/audio/").substringBefore("/document/")

internal fun fullMusicFolderPath(track: MusicTrack): String? {
    val path = track.folderPath ?: return null
    if (path.startsWith("/storage/") || path.startsWith("/mnt/") ||
        path == "/sdcard" || path.startsWith("/sdcard/")
    ) return path
    val uri = runCatching { URI(track.contentUri) }.getOrNull() ?: return path
    val segments = uri.path.orEmpty().trim('/').split('/')
    val storageRoot = when {
        uri.host == "media" && segments.size >= 4 && segments[1] == "audio" -> {
            when (val volume = segments[0]) {
                "external_primary", "external" -> "/storage/emulated/0"
                "internal" -> return path
                else -> "/storage/${volume.uppercase(Locale.ROOT)}"
            }
        }
        uri.host == "com.android.externalstorage.documents" -> {
            val documentId = uri.path.orEmpty().substringAfterLast("/document/", "")
            val volume = documentId.substringBefore(':')
            if (volume.isEmpty() || ':' !in documentId) return path
            if (volume.equals("primary", ignoreCase = true)) "/storage/emulated/0" else "/storage/$volume"
        }
        else -> return path
    }
    return storageRoot + path.trimEnd('/').takeUnless { it == "/" }.orEmpty()
}

internal fun folderDisplayPath(path: String?): String {
    val normalized = normalizeMusicFolderPath(path, includesFileName = false) ?: return "/"
    val sharedStorageRoots = listOf(
        "/storage/emulated/0",
        "/storage/self/primary",
        "/mnt/sdcard",
        "/sdcard",
    )
    val root = sharedStorageRoots.firstOrNull { candidate ->
        normalized.equals(candidate, ignoreCase = true) ||
            normalized.startsWith("$candidate/", ignoreCase = true)
    } ?: return normalized
    return normalized.substring(root.length).ifEmpty { "/" }
}

internal fun buildFolderGroups(tracks: List<MusicTrack>): List<FolderGroup> {
    val folderPaths = HashMap<Pair<String?, String>, String?>()
    fun resolvedPath(track: MusicTrack): String? =
        folderPaths.getOrPut(musicFolderIdentity(track)) { fullMusicFolderPath(track) }
    return tracks
        .groupBy { track -> resolvedPath(track)?.lowercase(Locale.ROOT).orEmpty() }
        .map { (key, folderTracks) ->
            val path = folderTracks.firstNotNullOfOrNull(::resolvedPath)
            val displayPath = folderDisplayPath(path)
            FolderGroup(
                key = key,
                name = displayPath
                    .substringAfterLast('/')
                    .takeIf(String::isNotEmpty),
                path = path,
                displayPath = displayPath,
                tracks = folderTracks,
            )
        }
}

internal fun filterFolders(
    folders: List<FolderGroup>,
    query: String,
): List<FolderGroup> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return folders
    return folders.filter { folder ->
        folder.name?.contains(normalizedQuery, ignoreCase = true) == true
    }
}

internal fun sortFolders(
    folders: List<FolderGroup>,
    config: FolderSortConfig,
): List<FolderGroup> {
    val nameComparator = compareBy<FolderGroup> {
        createMusicSortKeys(it.name).value
    }
        .thenBy { it.displayPath.lowercase(Locale.ROOT) }
        .thenBy(FolderGroup::key)
    val comparator = when (config.field) {
        FolderSortField.NAME -> nameComparator
        FolderSortField.SONG_COUNT -> compareBy<FolderGroup> { it.tracks.size }
            .then(nameComparator)
    }
    val effectiveComparator = if (config.descending) {
        Comparator<FolderGroup> { first, second -> comparator.compare(second, first) }
    } else {
        comparator
    }
    return folders.sortedWith(effectiveComparator)
}

internal fun folderSectionKey(folder: FolderGroup): String =
    createMusicSortKeys(folder.name).section
