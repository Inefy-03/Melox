package com.melox.player.data.library

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.core.net.toUri
import androidx.media3.common.MediaMetadata
import com.kyant.taglib.Picture
import com.kyant.taglib.TagLib
import java.io.IOException

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

/** Tries typed pictures first so the platform's untyped fallback cannot select an artist portrait. */
internal fun <T : Any> readEmbeddedArtwork(
    context: Context,
    contentUri: String,
    decode: (ByteArray) -> T?,
): T? {
    val uri = contentUri.toUri()
    val resolver = context.contentResolver
    val flac = runCatching {
        resolver.openInputStream(uri)?.use(::readFlacPictureMetadata)
    }.getOrNull()
    val flacBytes = try {
        flac?.cover?.let { cover -> resolver.openInputStream(uri)?.use(cover::readFrom) }
    } catch (_: IOException) {
        null
    }
    flacBytes?.let(decode)?.let { return it }

    val pictures = runCatching {
        resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            TagLib.getPictures(descriptor.dup().detachFd())
        }
    }.getOrNull()
    if (!pictures.isNullOrEmpty()) {
        selectEmbeddedCover(pictures, decode)?.let { return it }
    }
    val wavArtwork = resolver.readWavMetadata(contentUri)?.artwork
    wavArtwork?.let(decode)?.let { return it }
    if (flac?.cover != null || wavArtwork != null || pictures.orEmpty().any { picture ->
            picture.data.isNotEmpty() && embeddedCoverTypes.any { (_, type) -> type == picture.pictureType }
        }
    ) {
        // An unreadable cover must not become a cached "no artwork" result.
        throw IOException("Unable to decode embedded artwork")
    }
    if (flac?.hasPictureBlocks == true || !pictures.isNullOrEmpty()) return null

    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        retriever.embeddedPicture?.takeIf(ByteArray::isNotEmpty)?.let { bytes ->
            decode(bytes) ?: throw IOException("Unable to decode embedded artwork")
        }
    } finally {
        runCatching(retriever::release)
    }
}

internal fun <T : Any> selectEmbeddedCover(pictures: Array<Picture>, decode: (ByteArray) -> T?): T? =
    embeddedCoverTypes.firstNotNullOfOrNull { (_, type) ->
        pictures.firstNotNullOfOrNull { picture ->
            picture.data.takeIf { picture.pictureType == type && it.isNotEmpty() }?.let(decode)
        }
    }

internal fun coverPicturePriority(type: Int): Int? =
    embeddedCoverTypes.indexOfFirst { (pictureType, _) -> pictureType == type }.takeIf { it >= 0 }
