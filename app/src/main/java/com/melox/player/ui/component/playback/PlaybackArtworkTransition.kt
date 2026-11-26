package com.melox.player.ui.component.playback

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import com.melox.player.R
import com.melox.player.model.PlaybackQueueItem
import com.melox.player.ui.component.library.currentPlaceholderArtworkPalette
import com.melox.player.ui.component.library.rememberArtworkBitmapLoadState
import com.melox.player.ui.component.library.rememberFullPlayerArtworkBitmapPixels
import com.melox.player.ui.component.library.rememberPlaceholderArtworkBitmapLoadState

private val samePlaybackArtwork: (Bitmap, Bitmap) -> Boolean = { first, second -> first === second }

internal fun <T> playbackArtworkTarget(
    contentUri: String?,
    artwork: T?,
    placeholder: T?,
    placeholderArtworkResId: Int,
): ArtworkTarget<T> {
    val value = if (contentUri == null) placeholder else artwork ?: placeholder
    val key = when {
        contentUri == null -> "empty|placeholder-$placeholderArtworkResId"
        artwork == null && placeholder != null -> "$contentUri|placeholder-$placeholderArtworkResId"
        else -> contentUri
    }
    return ArtworkTarget(value, key)
}

@Composable
internal fun rememberPlaybackArtworkTransition(
    item: PlaybackQueueItem?,
    direction: Int,
    flipAnimationEnabled: Boolean,
    fullPlayerTargetSizePx: Int,
    fullPlayerMounted: Boolean,
): ArtworkTransitionState<Bitmap> {
    val artwork = rememberArtworkBitmapLoadState(
        contentUri = item?.contentUri.orEmpty(),
        dateModifiedEpochSeconds = item?.dateModifiedEpochSeconds ?: 0L,
        fileSizeBytes = item?.fileSizeBytes ?: 0L,
        size = PLAYER_FULL_ARTWORK_REQUEST_SIZE,
        foreground = true,
    )
    val fullPlayerArtwork = rememberFullPlayerArtworkBitmapPixels(
        contentUri = item?.contentUri.orEmpty(),
        dateModifiedEpochSeconds = item?.dateModifiedEpochSeconds ?: 0L,
        fileSizeBytes = item?.fileSizeBytes ?: 0L,
        targetSizePx = fullPlayerTargetSizePx,
        enabled = item != null && fullPlayerMounted && fullPlayerTargetSizePx > 0,
    )
    val placeholderPalette = currentPlaceholderArtworkPalette()
    val placeholder = if (item == null || artwork.bitmap == null) {
        rememberPlaceholderArtworkBitmapLoadState(
            PLAYER_FULL_ARTWORK_REQUEST_SIZE,
            palette = placeholderPalette,
        )
    } else null
    val usesPlaceholderArtwork = item == null ||
        fullPlayerArtwork == null && artwork.bitmap == null && placeholder?.bitmap != null
    val ready = if (item == null) {
        placeholder?.isLoading == false
    } else {
        fullPlayerArtwork != null ||
            (!artwork.isLoading && (artwork.bitmap != null || placeholder?.isLoading == false))
    }
    val target = playbackArtworkTarget(
        contentUri = item?.contentUri,
        artwork = fullPlayerArtwork ?: artwork.bitmap,
        placeholder = placeholder?.bitmap,
        placeholderArtworkResId = R.drawable.ic_album_placeholder,
    )
    return rememberArtworkTransitionState(
        target = rememberReadyArtworkTarget(target.value, target.contentKey, ready),
        requestedContentKey = target.contentKey,
        targetReady = ready,
        targetIsPlaceholder = item == null || ready && usesPlaceholderArtwork,
        animate = true,
        flipAnimationEnabled = flipAnimationEnabled,
        direction = direction,
        durationMillis = PLAYER_TRACK_ARTWORK_CROSSFADE_DURATION_MILLIS,
        sameContentDurationMillis = PLAYER_ARTWORK_RESOLUTION_CROSSFADE_DURATION_MILLIS,
        easing = PLAYER_TRACK_ARTWORK_CROSSFADE_EASING,
        sameValue = samePlaybackArtwork,
    )
}
