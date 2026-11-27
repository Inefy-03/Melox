package com.melox.player.data.library

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.id3.ApicFrame
import androidx.media3.extractor.metadata.id3.Id3Decoder
import com.kyant.taglib.Picture
import com.kyant.taglib.TagLib
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream

private val embeddedCoverTypes = listOf(
    MediaMetadata.PICTURE_TYPE_FRONT_COVER to "Front Cover",
    MediaMetadata.PICTURE_TYPE_BACK_COVER to "Back Cover",
    MediaMetadata.PICTURE_TYPE_FILE_ICON_OTHER to "Other File Icon",
    MediaMetadata.PICTURE_TYPE_OTHER to "Other",
    MediaMetadata.PICTURE_TYPE_ILLUSTRATION to "Illustration",
    MediaMetadata.PICTURE_TYPE_LEAFLET_PAGE to "Leaflet Page",
    MediaMetadata.PICTURE_TYPE_MEDIA to "Media",
    MediaMetadata.PICTURE_TYPE_FILE_ICON to "File Icon",
)
private val embeddedCoverNames = embeddedCoverTypes.map { (_, name) -> name }
private val embeddedArtistNames = listOf("Artist")

/** Opens a fresh image stream for each decode pass. Prefer typed pictures over platform artwork. */
@OptIn(UnstableApi::class)
internal fun <T : Any> readEmbeddedArtwork(
    context: Context,
    contentUri: String,
    artistImage: Boolean = false,
    decode: (() -> InputStream) -> T?,
): T? {
    val uri = contentUri.toUri()
    val resolver = context.contentResolver
    fun openAudio() = resolver.openInputStream(uri) ?: throw IOException("Unable to open audio")

    var readFailure: Exception? = null
    fun <R> attempt(read: () -> R): R? = try {
        read()
    } catch (exception: Exception) {
        if (readFailure == null) readFailure = exception
        null
    }

    fun decodeImage(open: () -> InputStream): T? = attempt {
        decode(open) ?: throw IOException("Unable to decode embedded artwork")
    }

    var id3Pictures = emptyList<ApicFrame>()
    val flac = attempt {
        openAudio().buffered().use { stream ->
            stream.mark(4)
            readFlacPictureMetadata(stream, artistImage) ?: run {
                stream.reset()
                id3Pictures = readId3Pictures(stream)
                null
            }
        }
    }
    flac?.cover?.let { cover ->
        decodeImage { cover.openStream(resolver, uri) }?.let { return it }
        val bytes = attempt { openAudio().use(cover::readFrom) }
        bytes?.let { decodeImage { it.inputStream() } }?.let { return it }
    }
    if (artistImage && flac != null && flac.cover == null) return null
    val id3Candidates = id3Pictures
        .mapNotNull { frame -> embeddedPicturePriority(frame.pictureType, artistImage)?.let { it to frame.pictureData } }
        .sortedBy { (priority, _) -> priority }
    id3Candidates
        .firstNotNullOfOrNull { (_, bytes) -> decodeImage { bytes.inputStream() } }
        ?.let { return it }
    if (artistImage && id3Pictures.isNotEmpty() && id3Candidates.isEmpty()) return null

    val pictures = attempt {
        val descriptor = resolver.openFileDescriptor(uri, "r") ?: throw IOException("Unable to open audio")
        descriptor.use {
            TagLib.getPictures(descriptor.dup().detachFd())
        }
    }
    selectEmbeddedCover(pictures.orEmpty(), artistImage) { bytes -> decodeImage { bytes.inputStream() } }
        ?.let { return it }

    val wav = if (flac == null && id3Pictures.isEmpty()) {
        attempt { openAudio().use { readWavMetadata(it, artistImage) } }
    } else null
    wav?.artwork?.let { bytes -> decodeImage { bytes.inputStream() } }?.let { return it }
    if (!artistImage && flac?.hasPictureBlocks != true && id3Pictures.isEmpty() &&
        pictures.isNullOrEmpty() && wav?.hasPictures != true
    ) {
        val platformArtwork = attempt {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                retriever.embeddedPicture?.takeIf(ByteArray::isNotEmpty)
            } finally {
                runCatching(retriever::release)
            }
        }
        platformArtwork?.let { bytes -> decodeImage { bytes.inputStream() } }?.let { return it }
    }

    // A failed read or decode must be retried instead of becoming a cached miss.
    readFailure?.let { throw IOException("Unable to load embedded artwork", it) }
    return null
}

@OptIn(UnstableApi::class)
internal fun readId3Pictures(stream: InputStream): List<ApicFrame> {
    val input = DataInputStream(stream)
    if (input.read() != 'I'.code || input.read() != 'D'.code || input.read() != '3'.code) return emptyList()
    val header = "ID3".toByteArray(Charsets.US_ASCII).copyOf(10)
    input.readFully(header, 3, 7)
    var size = 0
    for (index in 6..9) {
        val byte = header[index].toInt()
        if (byte < 0) throw IOException("Invalid ID3 tag size")
        size = (size shl 7) or byte
    }
    if (size > 16 * 1024 * 1024) throw IOException("ID3 tag is too large to read artwork")
    val tag = header.copyOf(header.size + size)
    input.readFully(tag, header.size, size)
    val metadata = Id3Decoder { version, a, b, c, d ->
        if (version == 2) a == 'P'.code && b == 'I'.code && c == 'C'.code
        else a == 'A'.code && b == 'P'.code && c == 'I'.code && d == 'C'.code
    }.decode(tag, tag.size) ?: throw IOException("Unable to read ID3 artwork")
    return (0 until metadata.length()).mapNotNull { metadata[it] as? ApicFrame }
}

internal fun <T : Any> selectEmbeddedCover(
    pictures: Array<out Picture>,
    artistImage: Boolean = false,
    decode: (ByteArray) -> T?,
): T? {
    val allowedTypes = if (artistImage) embeddedArtistNames else embeddedCoverNames
    return allowedTypes.firstNotNullOfOrNull { type ->
        pictures.firstNotNullOfOrNull { picture ->
            picture.data.takeIf { picture.pictureType == type && it.isNotEmpty() }?.let(decode)
        }
    }
}

internal fun embeddedPicturePriority(type: Int, artistImage: Boolean): Int? =
    if (artistImage) 0.takeIf { type == MediaMetadata.PICTURE_TYPE_ARTIST_PERFORMER } else coverPicturePriority(type)

internal fun coverPicturePriority(type: Int): Int? = when (type) {
    // Some tags store an all-ones value instead of an explicit picture type.
    -1, 0xff -> embeddedCoverTypes.size
    else -> embeddedCoverTypes.indexOfFirst { (pictureType, _) -> pictureType == type }.takeIf { it >= 0 }
}
