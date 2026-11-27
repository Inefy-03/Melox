package com.melox.player.data.library

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import com.kyant.taglib.TagLib
import com.melox.player.model.MusicTrack
import java.util.Locale

internal data class LocalAudioProperties(
    val durationMs: Long?,
    val bitrateBitsPerSecond: Int?,
    val sampleRateHz: Int?,
    val channelCount: Int?,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumArtist: String? = null,
    val year: Int? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val bitDepth: Int? = null,
)

/**
 * Reads stream properties through a duplicated descriptor so metadata scanning keeps ownership local.
 */
internal class AudioPropertiesReader(
    private val contentResolver: ContentResolver,
) {
    fun readTags(contentUri: String): LocalAudioTags? {
        val tags = try {
            contentResolver.openFileDescriptor(contentUri.toUri(), "r")?.use { descriptor ->
                TagLib.getMetadata(descriptor.dup().detachFd(), false)
                    ?.propertyMap?.let(::parseAudioTagProperties)
            }
        } catch (exception: Exception) {
            Log.w(TAG, "Unable to read audio tags for $contentUri", exception)
            null
        } catch (error: LinkageError) {
            Log.e(TAG, "TagLib is unavailable for $contentUri", error)
            null
        }
        val wav = contentResolver.readWavMetadata(contentUri)?.tags ?: return tags
        return LocalAudioTags(
            title = wav.title ?: tags?.title,
            artist = wav.artist ?: tags?.artist,
            album = wav.album ?: tags?.album,
            albumArtist = wav.albumArtist ?: tags?.albumArtist,
            year = wav.year ?: tags?.year,
            trackNumber = wav.trackNumber ?: tags?.trackNumber,
            discNumber = wav.discNumber ?: tags?.discNumber,
            replayGainDb = wav.replayGainDb ?: tags?.replayGainDb,
        )
    }

    fun read(contentUri: String): LocalAudioProperties? {
        val properties = readNativeProperties(contentUri)
        val wav = contentResolver.readWavMetadata(contentUri) ?: return properties
        return (properties ?: LocalAudioProperties(null, null, null, null)).copy(
            title = wav.tags.title ?: properties?.title,
            artist = wav.tags.artist ?: properties?.artist,
            album = wav.tags.album ?: properties?.album,
            albumArtist = wav.tags.albumArtist ?: properties?.albumArtist,
            year = wav.tags.year ?: properties?.year,
            trackNumber = wav.tags.trackNumber ?: properties?.trackNumber,
            discNumber = wav.tags.discNumber ?: properties?.discNumber,
        )
    }

    private fun readNativeProperties(contentUri: String): LocalAudioProperties? = try {
        contentResolver.openFileDescriptor(contentUri.toUri(), "r")?.use { descriptor ->
            val properties = TagLib.getAudioProperties(descriptor.dup().detachFd())
            val tagProperties = TagLib.getMetadata(
                descriptor.dup().detachFd(),
                false,
            )?.propertyMap
            val tags = parseAudioTagProperties(tagProperties.orEmpty())
            normalizeAudioProperties(
                durationMs = properties?.length ?: 0,
                bitrateKbps = properties?.bitrate ?: 0,
                sampleRateHz = properties?.sampleRate ?: 0,
                channelCount = properties?.channels ?: 0,
            ).copy(
                title = tags.title,
                artist = tags.artist,
                album = tags.album,
                albumArtist = tags.albumArtist,
                year = tags.year,
                trackNumber = tags.trackNumber,
                discNumber = tags.discNumber,
                bitDepth = readBitDepth(descriptor),
            )
        }
    } catch (exception: Exception) {
        Log.w(TAG, "Unable to read audio properties for $contentUri", exception)
        readPlatformProperties(contentUri)
    } catch (error: LinkageError) {
        Log.e(TAG, "TagLib is unavailable for $contentUri", error)
        readPlatformProperties(contentUri)
    }

    private fun readPlatformProperties(contentUri: String): LocalAudioProperties? = try {
        val retriever = MediaMetadataRetriever()
        try {
            contentResolver.openFileDescriptor(contentUri.toUri(), "r")?.use { descriptor ->
                retriever.setDataSource(descriptor.fileDescriptor)
                LocalAudioProperties(
                    durationMs = retriever
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull()
                        ?.takeIf { it > 0L },
                    bitrateBitsPerSecond = retriever
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                        ?.toIntOrNull()
                        ?.takeIf { it > 0 },
                    sampleRateHz = null,
                    channelCount = null,
                )
            }
        } finally {
            retriever.release()
        }
    } catch (exception: Exception) {
        Log.w(TAG, "Unable to read platform audio properties for $contentUri", exception)
        null
    }

    private companion object {
        private const val TAG = "AudioPropertiesReader"
    }
}

internal data class LocalAudioTags(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumArtist: String? = null,
    val year: Int? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val replayGainDb: Double? = null,
)

internal fun parseAudioTagProperties(
    properties: Map<String, Array<String>>,
): LocalAudioTags {
    val normalizedProperties = buildMap<String, Array<String>> {
        properties.forEach { (key, values) ->
            val normalizedKey = key.uppercase(Locale.ROOT)
            val shortKey = normalizedKey.substringAfterLast(':', missingDelimiterValue = normalizedKey)
            listOf(normalizedKey, shortKey).distinct().forEach { candidate ->
                put(candidate, (get(candidate)?.toList().orEmpty() + values.toList()).toTypedArray())
            }
        }
    }

    fun joinedValue(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
        normalizedProperties[key]
            ?.asSequence()
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            ?.filterNot { it.contains('�') }
            ?.distinct()
            ?.toList()
            ?.takeIf(List<String>::isNotEmpty)
            ?.joinToString("/")
    }

    fun indexedValue(vararg keys: String): Int? = joinedValue(*keys)
        ?.substringBefore('/')
        ?.trim()
        ?.toIntOrNull()
        ?.takeIf { it > 0 }

    val rawDate = joinedValue("DATE", "YEAR", "TDRC", "TYER", "ICRD")
    val year = rawDate
        ?.let(YEAR_PATTERN::find)
        ?.value
        ?.toIntOrNull()
        ?.takeIf { it > 0 }

    return LocalAudioTags(
        title = joinedValue("TITLE", "TIT2", "\u00A9NAM", "INAM", "NAME"),
        artist = joinedValue("ARTIST", "TPE1", "\u00A9ART", "IART"),
        album = joinedValue("ALBUM", "TALB", "\u00A9ALB", "IPRD", "PRODUCT"),
        albumArtist = joinedValue(
            "ALBUMARTIST",
            "ALBUM ARTIST",
            "TPE2",
            "AART",
            "ALBUMARTISTSORT",
        ),
        year = year,
        trackNumber = indexedValue("TRACKNUMBER", "TRACK", "TRCK", "ITRK"),
        discNumber = indexedValue("DISCNUMBER", "DISC", "TPOS", "DISKNUMBER"),
        replayGainDb = sequenceOf("REPLAYGAIN_TRACK_GAIN", "REPLAYGAIN_ALBUM_GAIN")
            .flatMap { normalizedProperties[it].orEmpty().asSequence() }
            .mapNotNull { value ->
                value.trim().replace(REPLAY_GAIN_UNIT, "").toDoubleOrNull()?.takeIf(Double::isFinite)
            }.firstOrNull(),
    )
}

@SuppressLint("InlinedApi")
private fun readBitDepth(descriptor: android.os.ParcelFileDescriptor): Int? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val retriever = MediaMetadataRetriever()
    return try {
        descriptor.dup().use { duplicate ->
            retriever.setDataSource(duplicate.fileDescriptor)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)
                ?.toIntOrNull()
                ?.takeIf { it > 0 }
        }
    } catch (exception: RuntimeException) {
        Log.w("AudioPropertiesReader", "Unable to read audio bit depth", exception)
        null
    } finally {
        retriever.release()
    }
}

internal fun normalizeAudioProperties(
    durationMs: Int,
    bitrateKbps: Int,
    sampleRateHz: Int,
    channelCount: Int,
): LocalAudioProperties = LocalAudioProperties(
    durationMs = durationMs.takeIf { it > 0 }?.toLong(),
    bitrateBitsPerSecond = bitrateKbps
        .takeIf { it > 0 }
        ?.toLong()
        ?.times(BITS_PER_KILOBIT)
        ?.coerceAtMost(Int.MAX_VALUE.toLong())
        ?.toInt(),
    sampleRateHz = sampleRateHz.takeIf { it > 0 },
    channelCount = channelCount.takeIf { it > 0 },
)

internal fun MusicTrack.hasReusableAudioProperties(
    id: Long,
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
): Boolean = audioPropertiesScanned &&
    this.id == id &&
    this.contentUri == contentUri &&
    this.dateModifiedEpochSeconds == dateModifiedEpochSeconds &&
    this.fileSizeBytes == fileSizeBytes

private const val BITS_PER_KILOBIT = 1_000L
private val YEAR_PATTERN = Regex("""\b(?:1\d{3}|2\d{3})\b""")

private val REPLAY_GAIN_UNIT = Regex("""\s*dB$""", RegexOption.IGNORE_CASE)
