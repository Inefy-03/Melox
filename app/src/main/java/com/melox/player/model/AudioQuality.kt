package com.melox.player.model

import java.util.Locale

enum class AudioQuality {
    HR,
    SQ,
    HQ,
}

internal fun MusicTrack.resolveAudioQuality(): AudioQuality? {
    val extension = fileName
        ?.substringAfterLast('.', missingDelimiterValue = "")
        ?.lowercase(Locale.ROOT)
        .orEmpty()
    val normalizedMimeType = mimeType?.lowercase(Locale.ROOT).orEmpty()
    val isLossless = normalizedMimeType in LOSSLESS_MIME_TYPES ||
        extension in LOSSLESS_FILE_EXTENSIONS
    val bitrate = bitrateBitsPerSecond
        ?.toLong()
        ?.takeIf { it > 0L }

    return when {
        isLossless && (
            sampleRateHz?.let { it >= HIGH_RESOLUTION_SAMPLE_RATE_HZ } == true ||
                bitrate?.let { it >= HIGH_RESOLUTION_BITRATE_BITS_PER_SECOND } == true
            ) -> AudioQuality.HR
        isLossless -> AudioQuality.SQ
        bitrate?.let { it >= HIGH_QUALITY_BITRATE_BITS_PER_SECOND } == true -> AudioQuality.HQ
        else -> null
    }
}

private const val HIGH_RESOLUTION_SAMPLE_RATE_HZ = 88_200
private const val HIGH_RESOLUTION_BITRATE_BITS_PER_SECOND = 1_500_000L
private const val HIGH_QUALITY_BITRATE_BITS_PER_SECOND = 256_000L

private val LOSSLESS_MIME_TYPES = setOf(
    "audio/aiff",
    "audio/alac",
    "audio/ape",
    "audio/flac",
    "audio/wav",
    "audio/x-aiff",
    "audio/x-alac",
    "audio/x-ape",
    "audio/x-flac",
    "audio/x-wav",
)

private val LOSSLESS_FILE_EXTENSIONS = setOf(
    "aif",
    "aiff",
    "ape",
    "flac",
    "wav",
    "wave",
)
