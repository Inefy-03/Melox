package com.melox.player.model

/**
 * Immutable projection of one MediaStore audio row.
 *
 * Missing metadata remains null so presentation code can resolve a localized fallback at render time.
 * [contentUri] is a string form of the row URI and can be reopened through ContentResolver.
 * [titleSectionKey] and [titleSortKey] keep the displayed order aligned with the fast index.
 * [dateAddedEpochSeconds], [dateModifiedEpochSeconds], [fileName], and [fileSizeBytes] preserve
 * MediaStore values used by sorting and artwork-cache invalidation. [albumId] keeps the stable
 * MediaStore album identity, while [folderPath] is the normalized direct parent directory used by
 * the folder library. Optional audio format fields come from TagLib and support quality badges.
 * [audioPropertiesScanned] distinguishes an unreadable file from an older snapshot that has not
 * attempted the descriptor-based read yet.
 */
data class MusicTrack(
    val id: Long,
    val title: String?,
    val artist: String?,
    val album: String?,
    val albumArtist: String? = null,
    val year: Int? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val durationMs: Long,
    val dateAddedEpochSeconds: Long,
    val dateModifiedEpochSeconds: Long,
    val fileName: String?,
    val fileSizeBytes: Long,
    val contentUri: String,
    val titleSectionKey: String,
    val titleSortKey: String,
    val folderPath: String? = null,
    val albumId: Long? = null,
    val mimeType: String? = null,
    val bitrateBitsPerSecond: Int? = null,
    val sampleRateHz: Int? = null,
    val channelCount: Int? = null,
    val bitDepth: Int? = null,
    val audioPropertiesScanned: Boolean = false,
)
