package com.melox.player.playback

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.melox.player.data.library.coverPicturePriority
import com.melox.player.data.library.readEmbeddedArtwork
import com.melox.player.data.library.renderPlaceholderArtwork
import com.melox.player.model.PlaybackQueueItem
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

private const val LOCK_SCREEN_ARTWORK_SIZE_PX = 512
private const val LOCK_SCREEN_ARTWORK_JPEG_QUALITY = 90
private const val EXTRA_ARTWORK_PLACEHOLDER_DARK = "com.melox.player.ARTWORK_PLACEHOLDER_DARK"

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

internal fun MediaItem.withArtworkData(
    artworkData: ByteArray,
    placeholderDark: Boolean? = null,
): MediaItem {
    val metadata = mediaMetadata.buildUpon()
        .setArtworkData(artworkData, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
    if (placeholderDark != null || mediaMetadata.playbackPlaceholderDark != null) {
        val extras = mediaMetadata.extras?.let(::Bundle) ?: Bundle()
        if (placeholderDark == null) extras.remove(EXTRA_ARTWORK_PLACEHOLDER_DARK)
        else extras.putBoolean(EXTRA_ARTWORK_PLACEHOLDER_DARK, placeholderDark)
        metadata.setExtras(extras)
    }
    return buildUpon().setMediaMetadata(metadata.build()).build()
}

internal val MediaMetadata.playbackPlaceholderDark: Boolean?
    get() = extras?.takeIf { it.containsKey(EXTRA_ARTWORK_PLACEHOLDER_DARK) }
        ?.getBoolean(EXTRA_ARTWORK_PLACEHOLDER_DARK)

internal fun MediaMetadata.hasCoverArtwork(): Boolean =
    artworkUri != null || (
        artworkData != null && (artworkDataType?.let { coverPicturePriority(it) != null } ?: true)
    )

internal fun needsPlaybackArtwork(
    placeholderDark: Boolean?,
    currentDark: Boolean,
    hasCoverArtwork: Boolean,
): Boolean = placeholderDark?.let { it != currentDark } ?: !hasCoverArtwork

internal fun Context.loadPlaybackArtworkData(contentUri: String): ByteArray? =
    readEmbeddedArtwork(this, contentUri, ::decodePlaybackArtwork)?.toArtworkData()

internal fun Context.loadPlaybackPlaceholderData(dark: Boolean): ByteArray? {
    val configuration = Configuration(resources.configuration)
    configuration.uiMode = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
        if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
    return renderPlaceholderArtwork(createConfigurationContext(configuration), LOCK_SCREEN_ARTWORK_SIZE_PX)
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
