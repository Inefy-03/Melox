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
 * Reads stream properties through a duplicated descriptor, following Lyrico's TagLib scan path.
 */
internal class AudioPropertiesReader(
    private val contentResolver: ContentResolver,
) {
    fun read(contentUri: String): LocalAudioProperties? = try {
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
        null
    } catch (error: LinkageError) {
        Log.e(TAG, "TagLib is unavailable for $contentUri", error)
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
)

internal fun parseAudioTagProperties(
    properties: Map<String, Array<String>>,
): LocalAudioTags {
    val normalizedProperties = properties.entries.associate { (key, values) ->
        key.uppercase(Locale.ROOT) to values
    }

    fun joinedValue(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
        normalizedProperties[key]
            ?.asSequence()
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            ?.toList()
            ?.takeIf(List<String>::isNotEmpty)
            ?.joinToString("/")
    }

    fun indexedValue(vararg keys: String): Int? = joinedValue(*keys)
        ?.substringBefore('/')
        ?.trim()
        ?.toIntOrNull()
        ?.takeIf { it > 0 }

    val rawDate = joinedValue("DATE", "YEAR")
    val year = rawDate
        ?.let(YEAR_PATTERN::find)
        ?.value
        ?.toIntOrNull()
        ?.takeIf { it > 0 }

    return LocalAudioTags(
        title = joinedValue("TITLE"),
        artist = joinedValue("ARTIST"),
        album = joinedValue("ALBUM"),
        albumArtist = joinedValue(
            "ALBUMARTIST",
            "ALBUM ARTIST",
            "TPE2",
            "AART",
            "ALBUMARTISTSORT",
        ),
        year = year,
        trackNumber = indexedValue("TRACKNUMBER", "TRACK", "TRCK"),
        discNumber = indexedValue("DISCNUMBER", "DISC", "TPOS", "DISKNUMBER"),
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
