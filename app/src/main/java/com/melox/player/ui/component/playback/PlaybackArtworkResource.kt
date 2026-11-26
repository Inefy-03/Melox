package com.melox.player.ui.component.playback

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.melox.player.data.library.PlaceholderArtworkPalette
import com.melox.player.ui.component.library.createArtworkCacheKey
import com.melox.player.ui.component.library.currentPlaceholderArtworkPalette
import com.melox.player.ui.component.library.getArtworkBitmapFromMemoryCache
import com.melox.player.ui.component.library.loadArtworkBitmap
import com.melox.player.ui.component.library.loadArtworkBitmapFromCache
import com.melox.player.ui.component.library.loadCachedArtworkDerivative
import com.melox.player.ui.component.library.loadPlaceholderArtworkBitmap
import com.melox.player.ui.component.library.normalizeArtworkTargetSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal data class PlaybackArtworkResource(
    val cacheKey: String,
    val blurredCacheKey: String,
    val artwork: Bitmap?,
    val blurredArtwork: Bitmap?,
    val backgroundColor: Color,
    val isLoading: Boolean,
    val isArtworkReady: Boolean = !isLoading,
)

@Composable
internal fun rememberPlaybackArtworkResource(
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
    requestSize: Dp,
    prioritizeBlurredBackground: Boolean,
): PlaybackArtworkResource {
    val context = LocalContext.current.applicationContext
    val targetSizePx = normalizeArtworkTargetSize(
        with(LocalDensity.current) { requestSize.roundToPx() },
    )
    val placeholderPalette = currentPlaceholderArtworkPalette()
    val cacheKey = remember(contentUri, dateModifiedEpochSeconds, fileSizeBytes, targetSizePx) {
        createArtworkCacheKey(
            contentUri = contentUri,
            dateModifiedEpochSeconds = dateModifiedEpochSeconds,
            fileSizeBytes = fileSizeBytes,
            targetSizePx = targetSizePx,
        )
    }
    val blurredCacheKey = remember(contentUri, dateModifiedEpochSeconds, fileSizeBytes) {
        createBlurredArtworkLayerKey(
            contentUri = contentUri,
            dateModifiedEpochSeconds = dateModifiedEpochSeconds,
            fileSizeBytes = fileSizeBytes,
        )
    }
    val cachedArtwork = remember(cacheKey) {
        getArtworkBitmapFromMemoryCache(cacheKey)
    }
    val cachedBlurredArtwork = remember(
        blurredCacheKey,
        placeholderPalette,
        prioritizeBlurredBackground,
    ) {
        if (!prioritizeBlurredBackground) {
            null
        } else {
            getArtworkBitmapFromMemoryCache(blurredCacheKey)
                ?.let { blurredCacheKey to it }
                ?: "$blurredCacheKey-${placeholderPalette.cacheKey}".let { placeholderKey ->
                    getArtworkBitmapFromMemoryCache(placeholderKey)?.let { placeholderKey to it }
                }
        }
    }
    val request = remember(cacheKey, blurredCacheKey, placeholderPalette, prioritizeBlurredBackground) {
        Any()
    }
    val currentRequest by rememberUpdatedState(request)
    var resource by remember {
        mutableStateOf(
            PlaybackArtworkResource(
                cacheKey = cacheKey,
                blurredCacheKey = cachedBlurredArtwork?.first ?: blurredCacheKey,
                artwork = cachedArtwork,
                blurredArtwork = cachedBlurredArtwork?.second,
                backgroundColor = PlaybackArtworkFallbackColor,
                isLoading = contentUri.isNotBlank(),
            ),
        )
    }

    LaunchedEffect(request) {
        resource = resource.copy(isLoading = true)
        if (prioritizeBlurredBackground &&
            resource.cacheKey == cacheKey && resource.blurredArtwork == null
        ) {
            loadFirstPlaybackBackground(
                context = context,
                contentUri = contentUri,
                dateModifiedEpochSeconds = dateModifiedEpochSeconds,
                fileSizeBytes = fileSizeBytes,
                placeholderPalette = placeholderPalette,
                cachedArtwork = resource.artwork,
            )?.let { (layerKey, bitmap) ->
                currentCoroutineContext().ensureActive()
                if (currentRequest !== request) return@LaunchedEffect
                resource = resource.copy(
                    blurredCacheKey = layerKey,
                    blurredArtwork = bitmap,
                )
            }
        }
        val loaded = loadPlaybackArtworkResource(
            context = context,
            contentUri = contentUri,
            dateModifiedEpochSeconds = dateModifiedEpochSeconds,
            fileSizeBytes = fileSizeBytes,
            targetSizePx = targetSizePx,
            placeholderPalette = placeholderPalette,
            includeBlurredArtwork = prioritizeBlurredBackground,
        )
        currentCoroutineContext().ensureActive()
        if (currentRequest === request) resource = loaded
    }

    return resource.copy(
        isLoading = resource.isLoading || resource.cacheKey != cacheKey,
        isArtworkReady = resource.cacheKey == cacheKey &&
            (resource.artwork != null || !resource.isLoading),
    )
}

internal suspend fun prefetchPlaybackBackground(
    context: Context,
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
    placeholderPalette: PlaceholderArtworkPalette,
) {
    loadFirstPlaybackBackground(
        context = context.applicationContext,
        contentUri = contentUri,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileSizeBytes = fileSizeBytes,
        placeholderPalette = placeholderPalette,
    )
}

private suspend fun loadFirstPlaybackBackground(
    context: Context,
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
    placeholderPalette: PlaceholderArtworkPalette,
    cachedArtwork: Bitmap? = null,
): Pair<String, Bitmap>? {
    val blurredCacheKey = createBlurredArtworkLayerKey(
        contentUri = contentUri,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileSizeBytes = fileSizeBytes,
    )
    loadArtworkBitmapFromCache(context, blurredCacheKey)?.let {
        return blurredCacheKey to it
    }
    val placeholderKey = "$blurredCacheKey-${placeholderPalette.cacheKey}"
    loadArtworkBitmapFromCache(context, placeholderKey)?.let {
        return placeholderKey to it
    }

    val embeddedArtwork = cachedArtwork ?: loadArtworkBitmap(
        context = context,
        contentUri = contentUri,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileSizeBytes = fileSizeBytes,
        targetSizePx = PLAYBACK_BACKGROUND_BLUR_SIZE_PX,
        foreground = true,
    )
    val artwork = embeddedArtwork ?: loadPlaceholderArtwork(
        context = context,
        palette = placeholderPalette,
        targetSizePx = PLAYBACK_BACKGROUND_BLUR_SIZE_PX,
    )
    val layerKey = if (embeddedArtwork == null) placeholderKey else blurredCacheKey
    val blurredArtwork = artwork?.let { source ->
        loadCachedArtworkDerivative(
            context = context,
            cacheKey = layerKey,
        ) {
            createBlurredArtwork(source)
        }
    }
    return blurredArtwork?.let { layerKey to it }
}

internal suspend fun prefetchPlaybackArtworkResource(
    context: Context,
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
    targetSizePx: Int,
    placeholderPalette: PlaceholderArtworkPalette,
    includeBlurredArtwork: Boolean,
    foreground: Boolean = false,
): PlaybackArtworkResource = loadPlaybackArtworkResource(
    context = context.applicationContext,
    contentUri = contentUri,
    dateModifiedEpochSeconds = dateModifiedEpochSeconds,
    fileSizeBytes = fileSizeBytes,
    targetSizePx = targetSizePx,
    placeholderPalette = placeholderPalette,
    includeBlurredArtwork = includeBlurredArtwork,
    foreground = foreground,
)

private suspend fun loadPlaybackArtworkResource(
    context: Context,
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
    targetSizePx: Int,
    placeholderPalette: PlaceholderArtworkPalette,
    includeBlurredArtwork: Boolean,
    foreground: Boolean = true,
): PlaybackArtworkResource {
    val normalizedTargetSizePx = normalizeArtworkTargetSize(targetSizePx)
    val cacheKey = createArtworkCacheKey(
        contentUri = contentUri,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileSizeBytes = fileSizeBytes,
        targetSizePx = normalizedTargetSizePx,
    )
    val baseBlurredCacheKey = createBlurredArtworkLayerKey(
        contentUri = contentUri,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileSizeBytes = fileSizeBytes,
    )
    val embeddedArtwork = loadArtworkBitmap(
        context = context,
        contentUri = contentUri,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileSizeBytes = fileSizeBytes,
        targetSizePx = normalizedTargetSizePx,
        foreground = foreground,
    )
    val usesPlaceholderArtwork = embeddedArtwork == null
    val artwork = embeddedArtwork ?: loadPlaceholderArtwork(
        context = context,
        palette = placeholderPalette,
        targetSizePx = normalizedTargetSizePx,
    )
    val blurredCacheKey = if (usesPlaceholderArtwork) {
        "$baseBlurredCacheKey-${placeholderPalette.cacheKey}"
    } else {
        baseBlurredCacheKey
    }
    val backgroundColor = withContext(Dispatchers.Default) {
        dynamicFlowBackgroundColor(artwork)
    }
    val blurredArtwork = artwork?.takeIf { includeBlurredArtwork }?.let { source ->
        loadCachedArtworkDerivative(
            context = context,
            cacheKey = blurredCacheKey,
        ) {
            createBlurredArtwork(source)
        }
    }
    return PlaybackArtworkResource(
        cacheKey = cacheKey,
        blurredCacheKey = blurredCacheKey,
        artwork = artwork,
        blurredArtwork = blurredArtwork,
        backgroundColor = backgroundColor,
        isLoading = false,
    )
}

private suspend fun loadPlaceholderArtwork(
    context: Context,
    palette: PlaceholderArtworkPalette,
    targetSizePx: Int,
): Bitmap? = withContext(Dispatchers.IO) {
    loadPlaceholderArtworkBitmap(
        context = context,
        palette = palette,
        targetSizePx = targetSizePx,
    )
}

private val PlaybackArtworkFallbackColor = Color(0xFF242424)
