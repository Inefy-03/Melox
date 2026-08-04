package com.melox.player.data.library

import com.melox.player.model.MusicTrack
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

internal fun buildFolderGroups(tracks: List<MusicTrack>): List<FolderGroup> =
    tracks
        .groupBy { track ->
            track.folderPath
                ?.lowercase(Locale.ROOT)
                .orEmpty()
        }
        .map { (key, folderTracks) ->
            val path = folderTracks.firstNotNullOfOrNull(MusicTrack::folderPath)
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
