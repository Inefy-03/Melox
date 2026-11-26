package com.melox.player.ui.component.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.os.SystemClock
import android.util.AtomicFile
import android.util.LruCache
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativePaint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.melox.player.data.library.PlaceholderArtworkPalette
import com.melox.player.data.library.readEmbeddedArtwork
import com.melox.player.data.library.renderPlaceholderArtwork
import com.melox.player.ui.component.playback.ArtworkFlipLayer
import com.melox.player.ui.screen.playback.rememberArtworkBlend
import com.melox.player.ui.screen.playback.ArtworkBlend
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.squircle.addSquircleRect
import top.yukonga.miuix.kmp.squircle.isSquircleEnabled
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val ArtworkSize = 48.dp
private val ArtworkCornerRadius = 6.dp
internal val PLACEHOLDER_ARTWORK_REQUEST_SIZE = 420.dp

/** Displays a bounded cached thumbnail without blocking the Compose thread. */
@Composable
fun TrackArtwork(
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
    modifier: Modifier = Modifier,
    size: Dp = ArtworkSize,
    cornerRadius: Dp = ArtworkCornerRadius,
    showLoadingBackground: Boolean = false,
) {
    val artwork = rememberArtworkBitmapLoadState(
        contentUri = contentUri,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileSizeBytes = fileSizeBytes,
        size = size,
    )
    val bitmap = rememberRetainedArtworkBitmap(artwork)

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier
                .size(size)
                .squircleClip(cornerRadius),
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.High,
        )
    } else if (showLoadingBackground && artwork.isLoading) {
        Box(
            modifier = modifier
                .size(size)
                .squircleBackground(MiuixTheme.colorScheme.secondaryContainer, cornerRadius),
        )
    } else {
        PlaceholderArtwork(
            modifier = modifier
                .size(size)
                .squircleClip(cornerRadius)
                .graphicsLayer { alpha = 0.3f },
        )
    }
}

/** Displays artwork at a caller-selected size while sharing the bounded memory/disk cache. */
@Composable
fun PlaybackArtwork(
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
    size: Dp,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    requestSize: Dp = size,
    contentScale: ContentScale = ContentScale.Crop,
    bitmapCrossfadeDurationMillis: Int = 0,
    bitmapCrossfadeEasing: Easing = LinearEasing,
    rectangularCornerRadiusReduction: Dp = 0.dp,
    circular: Boolean = false,
    flipAnimationEnabled: Boolean = false,
    transitionDirection: Int = 1,
    foreground: Boolean = false,
) {
    val artwork = rememberArtworkBitmapLoadState(
        contentUri = contentUri,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileSizeBytes = fileSizeBytes,
        size = requestSize,
        foreground = foreground,
    )
    val placeholder = if (artwork.bitmap == null) {
        rememberPlaceholderArtworkBitmapLoadState(PLACEHOLDER_ARTWORK_REQUEST_SIZE)
    } else null
    val bitmap = artwork.bitmap ?: placeholder?.bitmap
    val targetReady = !artwork.isLoading &&
        (artwork.bitmap != null || placeholder?.isLoading == false)
    if (bitmapCrossfadeDurationMillis > 0) {
        PlaybackArtworkStackedFade(
            targetBitmap = bitmap,
            size = size,
            cornerRadius = cornerRadius,
            durationMillis = bitmapCrossfadeDurationMillis,
            easing = bitmapCrossfadeEasing,
            modifier = modifier,
            contentScale = contentScale,
            rectangularCornerRadiusReduction = rectangularCornerRadiusReduction,
            circular = circular,
            flipAnimationEnabled = flipAnimationEnabled,
            transitionDirection = transitionDirection,
            contentKey = contentUri,
            targetReady = targetReady,
        )
    } else {
        PlaybackArtworkFrame(
            bitmap = bitmap,
            size = size,
            cornerRadius = cornerRadius,
            modifier = modifier,
            contentScale = contentScale,
            rectangularCornerRadiusReduction = rectangularCornerRadiusReduction,
            circular = circular,
        )
    }
}

@Composable
private fun PlaybackArtworkStackedFade(
    targetBitmap: Bitmap?,
    size: Dp,
    cornerRadius: Dp,
    durationMillis: Int,
    easing: Easing,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    rectangularCornerRadiusReduction: Dp = 0.dp,
    circular: Boolean = false,
    flipAnimationEnabled: Boolean,
    transitionDirection: Int,
    contentKey: Any?,
    targetReady: Boolean,
) {
    val blend = rememberArtworkBlend(
        targetBitmap = targetBitmap,
        animate = true,
        durationMillis = durationMillis,
        contentKey = contentKey,
        sameContentDurationMillis = 100,
        transitionDirection = transitionDirection,
        flipAnimationEnabled = flipAnimationEnabled,
        targetReady = targetReady,
        easing = easing,
    )
    PlaybackArtworkBlend(blend, size, cornerRadius, modifier, contentScale,
        rectangularCornerRadiusReduction, circular)
}

@Composable
internal fun PlaybackArtworkBlend(
    blend: ArtworkBlend,
    size: Dp,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    rectangularCornerRadiusReduction: Dp = 0.dp,
    circular: Boolean = false,
    placeholderAlpha: Float = 1f,
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        val flip = blend.flipState
        if (flip != null) {
            ArtworkFlipLayer(flip, Modifier.fillMaxSize(),
                contentScale = if (circular) ContentScale.Crop else contentScale) { face, blur ->
                PlaybackArtworkFrame(
                    bitmap = face,
                    size = size,
                    cornerRadius = cornerRadius,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                    rectangularCornerRadiusReduction = rectangularCornerRadiusReduction,
                    circular = circular,
                    artworkAlpha = if (flip.visibleIsPlaceholder) placeholderAlpha else 1f,
                    artworkEffect = blur,
                )
            }
        } else if (blend.frames().isEmpty()) {
            PlaybackArtworkFrame(
                bitmap = null,
                size = size,
                cornerRadius = cornerRadius,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                rectangularCornerRadiusReduction = rectangularCornerRadiusReduction,
                circular = circular,
                artworkAlpha = if (blend.isPlaceholder) placeholderAlpha else 1f,
            )
        } else {
            blend.frames().forEach { frame ->
                PlaybackArtworkFrame(
                    bitmap = frame.value,
                    size = size,
                    cornerRadius = cornerRadius,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        alpha = frame.alpha * if (frame.isPlaceholder) {
                            placeholderAlpha.coerceIn(0f, 1f)
                        } else {
                            1f
                        }
                    },
                    contentScale = contentScale,
                    rectangularCornerRadiusReduction = rectangularCornerRadiusReduction,
                    circular = circular,
                )
            }
        }
    }
}

/** Draws one already-resolved playback artwork frame without starting another cache request. */
@Composable
internal fun PlaybackArtworkFrame(
    bitmap: Bitmap?,
    size: Dp,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    useSquircleClip: Boolean = false,
    drawArtworkShadow: Boolean = false,
    artworkAlpha: Float = 1f,
    artworkShadowAlpha: Float = artworkAlpha,
    onContentBoundsChanged: ((Rect) -> Unit)? = null,
    rectangularCornerRadiusReduction: Dp = 0.dp,
    circular: Boolean = false,
    artworkEffect: Modifier = Modifier,
) {
    val resolvedContentScale = if (circular) ContentScale.Crop else contentScale
    if (bitmap != null) {
        val resolvedCornerRadius = playbackArtworkCornerRadius(
            cornerRadius = cornerRadius,
            bitmapWidth = bitmap.width,
            bitmapHeight = bitmap.height,
            rectangularReduction = rectangularCornerRadiusReduction,
        )
        val imageSize = remember(bitmap.width, bitmap.height, size, resolvedContentScale) {
            if (resolvedContentScale == ContentScale.Fit) {
                fittedArtworkSize(bitmap = bitmap, bound = size)
            } else {
                DpSize(size, size)
            }
        }
        val imageModifier = Modifier
            .size(imageSize)
            .onGloballyPositioned { coordinates ->
                onContentBoundsChanged?.invoke(coordinates.boundsInParent())
            }
            .let { baseModifier ->
                if (useSquircleClip || circular) {
                    val shadowModifier = if (drawArtworkShadow) {
                        baseModifier.playbackArtworkShadow(
                            cornerRadius = resolvedCornerRadius,
                            alpha = artworkShadowAlpha,
                            circular = circular,
                        )
                    } else {
                        baseModifier
                    }
                    shadowModifier
                        .graphicsLayer { alpha = artworkAlpha.coerceIn(0f, 1f) }
                        .playbackArtworkClip(resolvedCornerRadius, circular)
                        .then(artworkEffect)
                } else {
                    baseModifier
                        .graphicsLayer { alpha = artworkAlpha.coerceIn(0f, 1f) }
                        .playbackArtworkClip(resolvedCornerRadius, circular)
                        .then(artworkEffect)
                }
            }
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = imageModifier,
                contentScale = resolvedContentScale,
                filterQuality = FilterQuality.High,
            )
        }
    } else {
        val placeholderModifier = if (drawArtworkShadow) {
            modifier.size(size)
                .playbackArtworkShadow(
                    cornerRadius = cornerRadius,
                    alpha = artworkShadowAlpha,
                    circular = circular,
                )
        } else {
            modifier.size(size)
        }
        val fadedPlaceholderModifier = if (artworkAlpha < 1f) {
            placeholderModifier.graphicsLayer {
                alpha = artworkAlpha.coerceIn(0f, 1f)
            }
        } else {
            placeholderModifier
        }
        PlaceholderArtwork(fadedPlaceholderModifier.playbackArtworkClip(cornerRadius, circular))
    }
}

@Composable
internal fun Modifier.playbackArtworkClip(cornerRadius: Dp, circular: Boolean): Modifier =
    if (circular) clip(CircleShape) else squircleClip(cornerRadius)

@Composable
internal fun Modifier.playbackArtworkShadow(
    cornerRadius: Dp,
    alpha: Float,
    circular: Boolean = false,
): Modifier {
    val resolvedAlpha = alpha.coerceIn(0f, 1f)
    if (resolvedAlpha == 0f) return this
    val squircleEnabled = isSquircleEnabled()
    return drawWithCache {
        val shadowBounds = playbackArtworkShadowBounds(
            width = size.width,
            height = size.height,
        )
        val shadowPath = Path().apply {
            if (circular) {
                addOval(Rect(0f, 0f, shadowBounds.width, shadowBounds.height))
            } else addSquircleRect(
                width = shadowBounds.width,
                height = shadowBounds.height,
                cornerRadius = cornerRadius.toPx(),
                squircleEnabled = squircleEnabled,
            )
        }
        val shadowPaint = Paint().apply {
            color = Color.Black.copy(alpha = PLAYBACK_ARTWORK_SHADOW_ALPHA * resolvedAlpha)
            blendMode = BlendMode.Overlay
            nativePaint.maskFilter = BlurMaskFilter(
                PLAYBACK_ARTWORK_SHADOW_BLUR_RADIUS.toPx(),
                BlurMaskFilter.Blur.NORMAL,
            )
        }
        onDrawBehind {
            drawIntoCanvas { canvas ->
                canvas.save()
                canvas.translate(shadowBounds.left, shadowBounds.top)
                canvas.drawPath(shadowPath, shadowPaint)
                canvas.restore()
            }
        }
    }
}

internal fun playbackArtworkShadowBounds(
    width: Float,
    height: Float,
): Rect {
    val resolvedWidth = width.coerceAtLeast(0f)
    val resolvedHeight = height.coerceAtLeast(0f)
    val left = resolvedWidth * PLAYBACK_ARTWORK_SHADOW_HORIZONTAL_OFFSET_FRACTION
    val top = resolvedHeight * PLAYBACK_ARTWORK_SHADOW_VERTICAL_OFFSET_FRACTION
    return Rect(
        left = left,
        top = top,
        right = left + resolvedWidth * PLAYBACK_ARTWORK_SHADOW_SIZE_FRACTION,
        bottom = top + resolvedHeight * PLAYBACK_ARTWORK_SHADOW_SIZE_FRACTION,
    )
}

internal fun playbackArtworkCornerRadius(
    cornerRadius: Dp,
    bitmapWidth: Int,
    bitmapHeight: Int,
    rectangularReduction: Dp,
): Dp = if (bitmapWidth != bitmapHeight) {
    maxOf(0.dp, cornerRadius - rectangularReduction)
} else {
    cornerRadius
}

private fun fittedArtworkSize(bitmap: Bitmap, bound: Dp): DpSize {
    val width = bitmap.width.coerceAtLeast(1)
    val height = bitmap.height.coerceAtLeast(1)
    return if (width >= height) {
        DpSize(width = bound, height = bound * height.toFloat() / width.toFloat())
    } else {
        DpSize(width = bound * width.toFloat() / height.toFloat(), height = bound)
    }
}

private const val PLAYBACK_ARTWORK_SHADOW_ALPHA = 0.36f
private const val PLAYBACK_ARTWORK_SHADOW_SIZE_FRACTION = 0.98f
private const val PLAYBACK_ARTWORK_SHADOW_HORIZONTAL_OFFSET_FRACTION = 0.01f
private const val PLAYBACK_ARTWORK_SHADOW_VERTICAL_OFFSET_FRACTION = 0.01f
internal val PLAYBACK_ARTWORK_SHADOW_BLUR_RADIUS = 16.dp

internal data class ArtworkBitmapLoadState(val bitmap: Bitmap?, val isLoading: Boolean)

@Composable
private fun rememberRetainedArtworkBitmap(artwork: ArtworkBitmapLoadState): Bitmap? {
    return rememberRetainedArtworkValue(
        result = if (artwork.isLoading) null else ArtworkLoadResult(artwork.bitmap),
    )
}

/** Loads a cached artwork bitmap for surfaces that need their own rendering treatment. */
@Composable
internal fun rememberArtworkBitmap(
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
    size: Dp,
    foreground: Boolean = false,
    onLoadCompleted: (Boolean) -> Unit = {},
): Bitmap? = rememberRetainedArtworkBitmap(rememberArtworkBitmapLoadState(
    contentUri = contentUri,
    dateModifiedEpochSeconds = dateModifiedEpochSeconds,
    fileSizeBytes = fileSizeBytes,
    size = size,
    foreground = foreground,
    onLoadCompleted = onLoadCompleted,
))

@Composable
internal fun rememberArtworkBitmapLoadState(
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
    size: Dp,
    foreground: Boolean = false,
    onLoadCompleted: (Boolean) -> Unit = {},
): ArtworkBitmapLoadState {
    val targetSizePx = normalizeArtworkTargetSize(
        with(LocalDensity.current) { size.roundToPx() },
    )
    return rememberArtworkBitmapForTargetSize(
        contentUri = contentUri,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileSizeBytes = fileSizeBytes,
        targetSizePx = targetSizePx,
        onLoadCompleted = onLoadCompleted,
        foreground = foreground,
    )
}

/** Loads a cached artwork bitmap at a fixed pixel bucket instead of a density-derived size. */
@Composable
internal fun rememberArtworkBitmapPixels(
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
    targetSizePx: Int,
): Bitmap? = rememberRetainedArtworkBitmap(rememberArtworkBitmapForTargetSize(
    contentUri = contentUri,
    dateModifiedEpochSeconds = dateModifiedEpochSeconds,
    fileSizeBytes = fileSizeBytes,
    targetSizePx = normalizeArtworkTargetSize(targetSizePx),
))

@Composable
internal fun rememberFullPlayerArtworkBitmapPixels(
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
    targetSizePx: Int,
    enabled: Boolean,
): Bitmap? {
    val context = LocalContext.current.applicationContext
    val normalizedTargetSizePx = fullPlayerArtworkTargetSizePx(targetSizePx)
    val cacheKey = remember(
        contentUri,
        dateModifiedEpochSeconds,
        fileSizeBytes,
        normalizedTargetSizePx,
    ) {
        "full-player|" + createArtworkCacheKey(
            contentUri = contentUri,
            dateModifiedEpochSeconds = dateModifiedEpochSeconds,
            fileSizeBytes = fileSizeBytes,
            targetSizePx = normalizedTargetSizePx,
        )
    }
    val result = rememberArtworkLoad(cacheKey to enabled) {
        if (enabled) {
            loadFullPlayerArtworkBitmap(
                context = context,
                contentUri = contentUri,
                dateModifiedEpochSeconds = dateModifiedEpochSeconds,
                fileSizeBytes = fileSizeBytes,
                targetSizePx = normalizedTargetSizePx,
            )
        } else {
            null
        }
    }
    return rememberRetainedArtworkValue(result, contentKey = contentUri, enabled = enabled)
}

@Composable
private fun rememberArtworkBitmapForTargetSize(
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
    targetSizePx: Int,
    foreground: Boolean = false,
    onLoadCompleted: (Boolean) -> Unit = {},
): ArtworkBitmapLoadState {
    val context = LocalContext.current.applicationContext
    val cacheKey = createArtworkCacheKey(
        contentUri = contentUri,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileSizeBytes = fileSizeBytes,
        targetSizePx = targetSizePx,
    )
    val initialResult = remember(cacheKey) {
        ArtworkCache.getCached(
            context = context,
            key = cacheKey,
            includeDisk = false,
        )?.let { ArtworkLoadResult(it) }
    }
    val result = rememberArtworkLoad(cacheKey, initialResult) {
        ArtworkCache.getOrLoad(context, cacheKey, foreground = foreground) {
            loadArtworkThumbnail(
                context = context,
                contentUri = contentUri,
                targetSizePx = targetSizePx,
            )
        }
    }?.value
    LaunchedEffect(result, contentUri) { onLoadCompleted(result != null) }
    return ArtworkBitmapLoadState(
        bitmap = (result as? ArtworkResult.Loaded)?.bitmap,
        isLoading = result == null,
    )
}

internal suspend fun prefetchArtwork(
    context: Context,
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
    targetSizePx: Int,
): Bitmap? = loadArtworkBitmap(
        context = context,
        contentUri = contentUri,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileSizeBytes = fileSizeBytes,
        targetSizePx = targetSizePx,
    )

internal suspend fun loadArtworkBitmap(
    context: Context,
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
    targetSizePx: Int,
    foreground: Boolean = false,
): Bitmap? {
    if (contentUri.isBlank()) return null
    val normalizedTargetSizePx = normalizeArtworkTargetSize(targetSizePx)
    val cacheKey = createArtworkCacheKey(
        contentUri = contentUri,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileSizeBytes = fileSizeBytes,
        targetSizePx = normalizedTargetSizePx,
    )
    return when (val result = ArtworkCache.getOrLoad(
        context.applicationContext,
        cacheKey,
        foreground = foreground,
    ) {
        loadArtworkThumbnail(
            context = context.applicationContext,
            contentUri = contentUri,
            targetSizePx = normalizedTargetSizePx,
        )
    }) {
        is ArtworkResult.Loaded -> result.bitmap
        ArtworkResult.Missing -> null
    }
}

internal fun getArtworkBitmapFromMemoryCache(cacheKey: String): Bitmap? =
    (ArtworkCache.get(cacheKey) as? ArtworkResult.Loaded)?.bitmap

internal fun getArtworkLoadResultFromMemoryCache(cacheKey: String): ArtworkLoadResult<Bitmap?>? =
    when (val result = ArtworkCache.get(cacheKey)) {
        is ArtworkResult.Loaded -> ArtworkLoadResult(result.bitmap)
        ArtworkResult.Missing -> ArtworkLoadResult(null)
        null -> null
    }

internal suspend fun loadArtworkBitmapFromCache(
    context: Context,
    cacheKey: String,
): Bitmap? = withContext(Dispatchers.IO) {
    (ArtworkCache.getCached(
        context = context.applicationContext,
        key = cacheKey,
        includeDisk = true,
    ) as? ArtworkResult.Loaded)?.bitmap
}

internal suspend fun loadFullPlayerArtworkBitmap(
    context: Context,
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
    targetSizePx: Int,
): Bitmap? {
    if (contentUri.isBlank()) return null
    val normalizedTargetSizePx = fullPlayerArtworkTargetSizePx(targetSizePx)
    val cacheKey = "full-player|" + createArtworkCacheKey(
        contentUri = contentUri,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileSizeBytes = fileSizeBytes,
        targetSizePx = normalizedTargetSizePx,
    )
    return when (val result = ArtworkCache.getOrLoad(
        context = context.applicationContext,
        key = cacheKey,
        useDiskCache = false,
        foreground = true,
    ) {
        loadArtworkThumbnail(
            context = context.applicationContext,
            contentUri = contentUri,
            targetSizePx = normalizedTargetSizePx,
        )
    }) {
        is ArtworkResult.Loaded -> result.bitmap
        ArtworkResult.Missing -> null
    }
}

/** Stores a derived artwork bitmap in the same bounded cache under a caller-owned key. */
internal suspend fun loadCachedArtworkDerivative(
    context: Context,
    cacheKey: String,
    createBitmap: () -> Bitmap?,
): Bitmap? = runCatching {
    when (val result = ArtworkCache.getOrLoad(context.applicationContext, cacheKey) {
        createBitmap()
            ?.let(ArtworkExtractionResult::Loaded)
            ?: ArtworkExtractionResult.Failed
    }) {
        is ArtworkResult.Loaded -> result.bitmap
        ArtworkResult.Missing -> null
    }
}.getOrNull()

@Composable
internal fun rememberPlaceholderArtworkBitmap(size: Dp): Bitmap? =
    rememberRetainedArtworkBitmap(rememberPlaceholderArtworkBitmapLoadState(size))

@Composable
internal fun rememberPlaceholderArtworkBitmapLoadState(
    size: Dp,
    palette: PlaceholderArtworkPalette = currentPlaceholderArtworkPalette(),
): ArtworkBitmapLoadState {
    val context = LocalContext.current.applicationContext
    val targetSizePx = normalizeArtworkTargetSize(
        with(LocalDensity.current) { size.roundToPx() },
    )
    val cacheKey = "${palette.cacheKey}:$targetSizePx"
    val initialResult = remember(cacheKey) {
        ArtworkCache.getCached(
            context = context,
            key = cacheKey,
            includeDisk = false,
        )?.let { ArtworkLoadResult(it) }
    }
    val result = rememberArtworkLoad(cacheKey, initialResult) {
        loadPlaceholderArtworkBitmap(
            context = context,
            palette = palette,
            targetSizePx = targetSizePx,
        )
            ?.let { ArtworkResult.Loaded(it) } ?: ArtworkResult.Missing
    }?.value
    return ArtworkBitmapLoadState(
        bitmap = (result as? ArtworkResult.Loaded)?.bitmap,
        isLoading = result == null,
    )
}

internal suspend fun loadPlaceholderArtworkBitmap(
    context: Context,
    palette: PlaceholderArtworkPalette,
    targetSizePx: Int,
): Bitmap? {
    val normalizedSize = normalizeArtworkTargetSize(targetSizePx)
    return loadCachedArtworkDerivative(
        context = context,
        cacheKey = "${palette.cacheKey}:$normalizedSize",
    ) {
        renderPlaceholderArtwork(context, normalizedSize, palette)
    }
}

internal fun normalizeArtworkTargetSize(targetSizePx: Int): Int {
    val requested = targetSizePx.coerceAtLeast(1)
    return ARTWORK_SIZE_BUCKETS.firstOrNull { it >= requested }
        ?: ARTWORK_SIZE_BUCKETS.last()
}

internal fun fullPlayerArtworkTargetSizePx(
    displayedSizePx: Int,
    maxMemoryBytes: Long = Runtime.getRuntime().maxMemory(),
): Int {
    val requested = displayedSizePx.coerceAtLeast(1)
    val bitmapBudgetBytes = (maxMemoryBytes / FULL_PLAYER_ARTWORK_HEAP_FRACTION)
        .coerceAtLeast(4L)
    val memoryLimitedEdge = sqrt(bitmapBudgetBytes / 4.0).toInt().coerceAtLeast(1)
    val maximumEdge = minOf(FULL_PLAYER_ARTWORK_MAX_SIZE_PX, memoryLimitedEdge)
    return minOf(requested, maximumEdge)
}

private fun loadArtworkThumbnail(
    context: Context,
    contentUri: String,
    targetSizePx: Int,
): ArtworkExtractionResult {
    return try {
        readEmbeddedArtwork(context, contentUri) { data ->
            decodeSampledBitmap(data, targetSizePx.coerceAtLeast(1))
        }?.let(ArtworkExtractionResult::Loaded)
            ?: ArtworkExtractionResult.Missing
    } catch (_: Exception) {
        ArtworkExtractionResult.Failed
    } catch (_: OutOfMemoryError) {
        ArtworkExtractionResult.Failed
    }
}

private fun Bitmap.toBoundedThumbnail(targetSizePx: Int): Bitmap {
    val (targetWidth, targetHeight) = fitArtworkDimensions(
        width = width,
        height = height,
        targetSizePx = targetSizePx,
    )
    if (width == targetWidth && height == targetHeight) return this

    return Bitmap.createScaledBitmap(
        this,
        targetWidth,
        targetHeight,
        true,
    ).also { scaled ->
        if (scaled !== this) recycle()
    }
}

internal fun fitArtworkDimensions(
    width: Int,
    height: Int,
    targetSizePx: Int,
): Pair<Int, Int> {
    val boundedTarget = targetSizePx.coerceAtLeast(1)
    val maxDimension = maxOf(width, height)
    if (maxDimension <= boundedTarget) return width to height

    val scale = boundedTarget.toFloat() / maxDimension
    return (width * scale).roundToInt().coerceAtLeast(1) to
        (height * scale).roundToInt().coerceAtLeast(1)
}

private fun decodeSampledBitmap(data: ByteArray, targetSizePx: Int): Bitmap? {
    val thumbnailSizePx = targetSizePx.coerceAtLeast(1)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(data, 0, data.size, bounds)

    var sampleSize = 1
    while (
        maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >=
        thumbnailSizePx
    ) {
        sampleSize *= 2
    }

    val decoded = BitmapFactory.decodeByteArray(
        data,
        0,
        data.size,
        BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inScaled = false
        },
    ) ?: return null

    return decoded.toBoundedThumbnail(thumbnailSizePx)
}

private sealed interface ArtworkResult {
    data class Loaded(val bitmap: Bitmap) : ArtworkResult

    data object Missing : ArtworkResult
}

private sealed interface ArtworkExtractionResult {
    data class Loaded(val bitmap: Bitmap) : ArtworkExtractionResult

    data object Missing : ArtworkExtractionResult

    data object Failed : ArtworkExtractionResult
}

internal fun createArtworkCacheKey(
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
    targetSizePx: Int,
): String = buildString {
    append(ARTWORK_CACHE_SCHEMA_VERSION)
    append('|')
    append(targetSizePx.coerceAtLeast(1))
    append('|')
    append(dateModifiedEpochSeconds.coerceAtLeast(0L))
    append('|')
    append(fileSizeBytes.coerceAtLeast(0L))
    append('|')
    append(contentUri)
}

internal fun artworkCacheFileStem(cacheKey: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(cacheKey.toByteArray(Charsets.UTF_8))
    return buildString(digest.size * 2) {
        digest.forEach { byte ->
            val value = byte.toInt() and 0xff
            append(HEX_DIGITS[value ushr 4])
            append(HEX_DIGITS[value and 0x0f])
        }
    }
}

@Volatile
private var artworkCacheResumeTimeMillis = 0L

internal fun canRetainArtworkInMemory(): Boolean =
    SystemClock.uptimeMillis() >= artworkCacheResumeTimeMillis

internal fun trimArtworkMemoryCache() {
    artworkCacheResumeTimeMillis = SystemClock.uptimeMillis() + 30_000L
    ArtworkCache.trim()
}

private object ArtworkCache {
    private val maxSizeKilobytes = (Runtime.getRuntime().maxMemory() / 1024L / 4L)
        .coerceIn(
            minimumValue = 8L * 1024L,
            maximumValue = 64L * 1024L,
        ).toInt()

    private val cache = object : LruCache<String, Bitmap>(maxSizeKilobytes) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            (value.byteCount / 1024).coerceAtLeast(1)
    }
    private val missingKeys = LruCache<String, Boolean>(MAX_MEMORY_MISSING_ENTRIES)
    private class Request(
        val priority: ArtworkDecodeLimiter.Request,
        val result: Deferred<ArtworkResult>,
    )
    private val inFlight = ConcurrentHashMap<String, Request>()
    private val loaderScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val decodeLimiter = ArtworkDecodeLimiter()

    @Synchronized
    fun trim() {
        // Visible consumers may still own these bitmaps; never recycle them here.
        cache.evictAll()
        missingKeys.evictAll()
    }

    fun get(key: String): ArtworkResult? =
        cache.get(key)?.let(ArtworkResult::Loaded)
            ?: if (missingKeys.get(key) == true) ArtworkResult.Missing else null

    fun getCached(
        context: Context,
        key: String,
        includeDisk: Boolean,
    ): ArtworkResult? {
        get(key)?.let { return it }
        if (!includeDisk) return null
        return ArtworkDiskCache.get(context, key)?.also { result ->
            remember(key, result)
        }
    }

    suspend fun getOrLoad(
        context: Context,
        key: String,
        useDiskCache: Boolean = true,
        foreground: Boolean = false,
        loader: () -> ArtworkExtractionResult,
    ): ArtworkResult {
        get(key)?.let { return it }

        val priority = decodeLimiter.request(foreground)
        val newRequest = Request(priority, loaderScope.async(start = CoroutineStart.LAZY) {
            try {
                if (useDiskCache) {
                    ArtworkDiskCache.get(context, key)?.let { cachedResult ->
                        remember(key, cachedResult)
                        return@async cachedResult
                    }
                }

                when (val extracted = decodeLimiter.decode(priority) { loader() }) {
                    is ArtworkExtractionResult.Loaded -> {
                        val result = ArtworkResult.Loaded(extracted.bitmap)
                        remember(key, result)
                        if (useDiskCache) ArtworkDiskCache.put(context, key, result)
                        result
                    }

                    ArtworkExtractionResult.Missing -> {
                        val result = ArtworkResult.Missing
                        remember(key, result)
                        if (useDiskCache) ArtworkDiskCache.put(context, key, result)
                        result
                    }

                    ArtworkExtractionResult.Failed -> ArtworkResult.Missing
                }
            } finally {
                inFlight.remove(key)
            }
        })
        val request = inFlight.putIfAbsent(key, newRequest) ?: newRequest
        if (request !== newRequest) {
            newRequest.result.cancel()
        }
        if (foreground) request.priority.promote()
        request.result.start()
        return request.result.await()
    }

    @Synchronized
    private fun remember(key: String, result: ArtworkResult) {
        if (!canRetainArtworkInMemory()) return
        when (result) {
            is ArtworkResult.Loaded -> cache.put(key, result.bitmap)
            ArtworkResult.Missing -> missingKeys.put(key, true)
        }
    }
}

internal data class ArtworkDiskCacheEntry(
    val file: File,
    val lastModifiedEpochMillis: Long,
    val sizeBytes: Long,
)

internal fun snapshotArtworkDiskCacheEntries(files: Array<File>): List<ArtworkDiskCacheEntry> =
    files
        .asSequence()
        .filter(File::isFile)
        .map { file ->
            ArtworkDiskCacheEntry(
                file = file,
                lastModifiedEpochMillis = file.lastModified(),
                sizeBytes = file.length(),
            )
        }
        .sortedWith(
            compareBy<ArtworkDiskCacheEntry>(ArtworkDiskCacheEntry::lastModifiedEpochMillis)
                .thenBy { entry -> entry.file.name },
        )
        .toList()

private object ArtworkDiskCache {
    private val initialized = AtomicBoolean(false)
    private val writesSinceMaintenance = AtomicInteger(0)
    private val maintenanceLock = Any()

    fun get(context: Context, key: String): ArtworkResult? {
        val directory = directory(context) ?: return null
        val fileStem = artworkCacheFileStem(key)
        val artworkFile = directory.resolve("$fileStem.png")
        val atomicArtworkFile = AtomicFile(artworkFile)
        val backupFile = directory.resolve("$fileStem.png.bak")
        if (artworkFile.isFile || backupFile.isFile) {
            val bitmap = runCatching {
                atomicArtworkFile.openRead().use(BitmapFactory::decodeStream)
            }.getOrNull()
            if (bitmap != null) {
                artworkFile.touchIfStale()
                return ArtworkResult.Loaded(bitmap)
            }
            atomicArtworkFile.delete()
        }

        val missingFile = directory.resolve("$fileStem.missing")
        return if (missingFile.isFile) {
            missingFile.touchIfStale()
            ArtworkResult.Missing
        } else {
            null
        }
    }

    fun put(context: Context, key: String, result: ArtworkResult) {
        val directory = directory(context) ?: return
        val fileStem = artworkCacheFileStem(key)
        val artworkFile = directory.resolve("$fileStem.png")
        val missingFile = directory.resolve("$fileStem.missing")
        val written = when (result) {
            is ArtworkResult.Loaded -> {
                val atomicFile = AtomicFile(artworkFile)
                val output = try {
                    atomicFile.startWrite()
                } catch (_: IOException) {
                    return
                }
                try {
                    if (!result.bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                        throw IOException("Unable to encode artwork thumbnail")
                    }
                    atomicFile.finishWrite(output)
                    missingFile.delete()
                    true
                } catch (_: Exception) {
                    atomicFile.failWrite(output)
                    false
                }
            }

            ArtworkResult.Missing -> runCatching {
                if (!missingFile.exists()) {
                    missingFile.createNewFile()
                }
                AtomicFile(artworkFile).delete()
                true
            }.getOrDefault(false)
        }
        if (written) {
            maintainAfterWrite(directory)
        }
    }

    private fun directory(context: Context): File? {
        val directory = context.noBackupFilesDir.resolve(ARTWORK_DISK_CACHE_DIRECTORY)
        if ((!directory.exists() && !directory.mkdirs()) || !directory.isDirectory) {
            return null
        }
        if (initialized.compareAndSet(false, true)) {
            synchronized(maintenanceLock) {
                trim(directory)
            }
        }
        return directory
    }

    private fun maintainAfterWrite(directory: File) {
        if (writesSinceMaintenance.incrementAndGet() < DISK_MAINTENANCE_INTERVAL) return
        synchronized(maintenanceLock) {
            if (writesSinceMaintenance.get() >= DISK_MAINTENANCE_INTERVAL) {
                writesSinceMaintenance.set(0)
                trim(directory)
            }
        }
    }

    private fun trim(directory: File) {
        val entries = directory.listFiles()
            ?.let(::snapshotArtworkDiskCacheEntries)
            ?: return
        var totalBytes = entries.sumOf(ArtworkDiskCacheEntry::sizeBytes)
        var fileCount = entries.size
        for (entry in entries) {
            if (
                totalBytes <= MAX_DISK_CACHE_BYTES &&
                fileCount <= MAX_DISK_CACHE_ENTRIES
            ) {
                break
            }
            if (entry.file.delete()) {
                totalBytes -= entry.sizeBytes
                fileCount -= 1
            }
        }
    }

    private fun File.touchIfStale() {
        val now = System.currentTimeMillis()
        if (now - lastModified() >= DISK_ACCESS_TOUCH_INTERVAL_MILLIS) {
            setLastModified(now)
        }
    }
}

private const val ARTWORK_CACHE_SCHEMA_VERSION = 6
private const val ARTWORK_DISK_CACHE_DIRECTORY = "artwork_thumbnails_v2"
private const val MAX_MEMORY_MISSING_ENTRIES = 10_000
private const val MAX_DISK_CACHE_ENTRIES = 20_000
private const val MAX_DISK_CACHE_BYTES = 64L * 1024L * 1024L
private const val DISK_MAINTENANCE_INTERVAL = 64
private const val DISK_ACCESS_TOUCH_INTERVAL_MILLIS = 60L * 60L * 1000L
private const val HEX_DIGITS = "0123456789abcdef"
private val ARTWORK_SIZE_BUCKETS =
    intArrayOf(64, 96, 128, 192, 256, 384, 512, 640, 768, 1024, 1280, 1536, 2048)
private const val FULL_PLAYER_ARTWORK_MAX_SIZE_PX = 8000
private const val FULL_PLAYER_ARTWORK_HEAP_FRACTION = 8L
