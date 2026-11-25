package com.melox.player.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Size
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.melox.player.R
import com.melox.player.data.library.readEmbeddedArtworkData
import com.melox.player.model.PlaybackQueueItem
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

private const val LOCK_SCREEN_ARTWORK_SIZE_PX = 512
private const val LOCK_SCREEN_ARTWORK_JPEG_QUALITY = 90

internal data class PlaybackArtworkKey(
    val contentUri: String,
    val dateModifiedEpochSeconds: Long,
    val fileSizeBytes: Long,
)

internal fun PlaybackQueueItem.toPlaybackArtworkKey(): PlaybackArtworkKey =
    PlaybackArtworkKey(
        contentUri = contentUri,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileSizeBytes = fileSizeBytes,
    )

internal fun MediaItem.withArtworkData(artworkData: ByteArray): MediaItem =
    buildUpon()
        .setMediaMetadata(
            mediaMetadata.buildUpon()
                .setArtworkData(
                    artworkData,
                    MediaMetadata.PICTURE_TYPE_FRONT_COVER,
                )
                .build(),
        )
        .build()

internal fun Context.loadPlaybackArtworkData(contentUri: String): ByteArray? {
    val uri = contentUri.toUri()
    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        runCatching {
            contentResolver.loadThumbnail(
                uri,
                Size(LOCK_SCREEN_ARTWORK_SIZE_PX, LOCK_SCREEN_ARTWORK_SIZE_PX),
                null,
            )
        }.getOrNull()
    } else {
        null
    } ?: run {
        val retriever = MediaMetadataRetriever()
        try {
            runCatching {
                retriever.setDataSource(this, uri)
                retriever.embeddedPicture
            }.getOrNull()
                ?.let(::decodePlaybackArtwork)
                ?: readEmbeddedArtworkData(this, contentUri)
                    ?.let(::decodePlaybackArtwork)
        } catch (_: Exception) {
            readEmbeddedArtworkData(this, contentUri)?.let(::decodePlaybackArtwork)
        } finally {
            runCatching(retriever::release)
        }
    }
    return bitmap?.let { runCatching { it.toArtworkData() }.getOrNull() }
        ?: runCatching { decodePlaceholderArtworkData() }.getOrNull()
}

private fun Context.decodePlaceholderArtworkData(): ByteArray? {
    return BitmapFactory.decodeResource(resources, R.drawable.ic_album_placeholder)
        ?.toArtworkData()
}

private fun decodePlaybackArtwork(data: ByteArray): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (
        maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >=
        LOCK_SCREEN_ARTWORK_SIZE_PX
    ) {
        sampleSize *= 2
    }
    return BitmapFactory.decodeByteArray(
        data,
        0,
        data.size,
        BitmapFactory.Options().apply { inSampleSize = sampleSize },
    )
}

private fun Bitmap.toArtworkData(): ByteArray? {
    val boundedBitmap = toBoundedArtwork()
    return try {
        ByteArrayOutputStream().use { output ->
            if (
                boundedBitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    LOCK_SCREEN_ARTWORK_JPEG_QUALITY,
                    output,
                )
            ) {
                output.toByteArray()
            } else {
                null
            }
        }
    } finally {
        boundedBitmap.recycle()
        if (boundedBitmap !== this) recycle()
    }
}

private fun Bitmap.toBoundedArtwork(): Bitmap {
    val maxDimension = maxOf(width, height)
    if (maxDimension <= LOCK_SCREEN_ARTWORK_SIZE_PX) return this

    val scale = LOCK_SCREEN_ARTWORK_SIZE_PX.toFloat() / maxDimension
    return Bitmap.createScaledBitmap(
        this,
        (width * scale).roundToInt().coerceAtLeast(1),
        (height * scale).roundToInt().coerceAtLeast(1),
        true,
    )
}
