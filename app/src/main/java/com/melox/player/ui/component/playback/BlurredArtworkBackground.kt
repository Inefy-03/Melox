package com.melox.player.ui.component.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.melox.player.ui.component.library.createArtworkCacheKey
import com.melox.player.ui.component.library.loadArtworkBitmap
import com.melox.player.ui.component.library.loadCachedArtworkDerivative
import java.util.LinkedHashMap
import java.util.WeakHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

private const val PLAYBACK_BACKGROUND_BLUR_SIZE_PX = 128
private const val PLAYBACK_BACKGROUND_BLUR_RADIUS = 25
private const val PLAYBACK_BACKGROUND_TRANSITION_DURATION_MILLIS = 640
private const val KEN_BURNS_TRANSITION_DURATION_MILLIS = 12_000
private const val BLURRED_ARTWORK_CACHE_SCHEMA_VERSION = 2
private const val BLURRED_ARTWORK_LAYER_MEMORY_CACHE_MAX_ENTRIES = 3
private const val BLURRED_ARTWORK_BACKGROUND_SATURATION = 1.5f
private const val BLURRED_ARTWORK_BACKGROUND_OVERLAY_COLOR = 0x4D000000
private val MissingArtworkBackgroundColor = Color(0xFF242424)
private val blurredArtworkMemoryCache = WeakHashMap<Bitmap, Bitmap>()
private val blurredArtworkLayerMemoryCache =
    object : LinkedHashMap<String, BlurredArtworkLayer>(8, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, BlurredArtworkLayer>?,
        ): Boolean = size > BLURRED_ARTWORK_LAYER_MEMORY_CACHE_MAX_ENTRIES
    }

private data class BlurredArtworkLayer(
    val key: String,
    val blurredArtwork: Bitmap,
)

private data class BlurredArtworkLayerBlend(
    val previousLayer: BlurredArtworkLayer?,
    val currentLayer: BlurredArtworkLayer?,
    val progress: Float,
    val hasPreviousLayer: Boolean,
)

internal data class KenBurnsFrame(
    val scale: Float,
    val horizontalBias: Float,
    val verticalBias: Float,
)

@Composable
internal fun BlurredArtworkBackground(
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
    animate: Boolean,
    animateArtworkTransition: Boolean = true,
    onStatusBarBackgroundDarkChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val targetLayer = rememberBlurredArtworkLayer(
        contentUri = contentUri,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileSizeBytes = fileSizeBytes,
    )
    val layerBlend = rememberBlurredArtworkLayerBlend(
        targetLayer = targetLayer,
        animateTransition = animateArtworkTransition,
    )
    val currentOnStatusBarBackgroundDarkChanged by rememberUpdatedState(
        onStatusBarBackgroundDarkChanged,
    )

    LaunchedEffect(Unit) {
        currentOnStatusBarBackgroundDarkChanged(true)
    }

    Box(
        modifier = modifier
            .background(MissingArtworkBackgroundColor)
            .clipToBounds(),
    ) {
        MovingBlurredArtworkLayer(
            layerBlend = layerBlend,
            animate = animate,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun MovingBlurredArtworkLayer(
    layerBlend: BlurredArtworkLayerBlend,
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    val animationKey = layerBlend.currentLayer?.key ?: layerBlend.previousLayer?.key
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val animationEnabled = animate &&
        animationKey != null &&
        lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val phase = remember(animationKey) { Animatable(0f) }
    var segmentIndex by remember(animationKey) { mutableIntStateOf(0) }

    LaunchedEffect(animationEnabled, animationKey) {
        if (!animationEnabled) return@LaunchedEffect
        while (true) {
            val remainingDuration =
                (KEN_BURNS_TRANSITION_DURATION_MILLIS * (1f - phase.value))
                    .roundToInt()
                    .coerceAtLeast(1)
            phase.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = remainingDuration,
                    easing = LinearEasing,
                ),
            )
            segmentIndex += 1
            phase.snapTo(0f)
        }
    }

    val startFrame = remember(animationKey, segmentIndex) {
        createKenBurnsFrame(animationKey.orEmpty(), segmentIndex)
    }
    val endFrame = remember(animationKey, segmentIndex) {
        createKenBurnsFrame(animationKey.orEmpty(), segmentIndex + 1)
    }
    val frame = interpolateKenBurnsFrame(
        start = startFrame,
        end = endFrame,
        progress = accelerateDecelerate(phase.value),
    )

    Box(modifier = modifier) {
        if (layerBlend.hasPreviousLayer) {
            layerBlend.previousLayer?.let { layer ->
                MovingArtworkImage(
                    bitmap = layer.blurredArtwork,
                    frame = frame,
                    alpha = 1f - layerBlend.progress,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        layerBlend.currentLayer?.let { layer ->
            MovingArtworkImage(
                bitmap = layer.blurredArtwork,
                frame = frame,
                alpha = layerBlend.progress,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun MovingArtworkImage(
    bitmap: Bitmap,
    frame: KenBurnsFrame,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    val painter = remember(bitmap) {
        BitmapPainter(
            image = bitmap.asImageBitmap(),
            filterQuality = FilterQuality.Low,
        )
    }
    Image(
        painter = painter,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.graphicsLayer {
            scaleX = frame.scale
            scaleY = frame.scale
            translationX = frame.horizontalBias * size.width * (frame.scale - 1f) * 0.5f
            translationY = frame.verticalBias * size.height * (frame.scale - 1f) * 0.5f
            this.alpha = alpha.coerceIn(0f, 1f)
        },
    )
}

@Composable
private fun rememberBlurredArtworkLayer(
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
): BlurredArtworkLayer? {
    val context = LocalContext.current.applicationContext
    var layer by remember { mutableStateOf<BlurredArtworkLayer?>(null) }
    val layerKey = remember(contentUri, dateModifiedEpochSeconds, fileSizeBytes) {
        createBlurredArtworkLayerKey(
            contentUri = contentUri,
            dateModifiedEpochSeconds = dateModifiedEpochSeconds,
            fileSizeBytes = fileSizeBytes,
        )
    }
    val cachedLayer = remember(layerKey) {
        getCachedBlurredArtworkLayer(layerKey)
    }

    LaunchedEffect(layerKey) {
        layer = cachedLayer ?: loadBlurredArtworkLayer(
            context = context,
            contentUri = contentUri,
            dateModifiedEpochSeconds = dateModifiedEpochSeconds,
            fileSizeBytes = fileSizeBytes,
        )
    }

    return cachedLayer ?: layer
}

internal suspend fun prefetchBlurredArtworkBackground(
    context: Context,
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
) {
    loadBlurredArtworkLayer(
        context = context.applicationContext,
        contentUri = contentUri,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileSizeBytes = fileSizeBytes,
    )
}

private suspend fun loadBlurredArtworkLayer(
    context: Context,
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
): BlurredArtworkLayer? {
    if (contentUri.isBlank()) return null
    val layerKey = createBlurredArtworkLayerKey(
        contentUri = contentUri,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileSizeBytes = fileSizeBytes,
    )
    getCachedBlurredArtworkLayer(layerKey)?.let { return it }
    val blurSource = loadArtworkBitmap(
        context = context,
        contentUri = contentUri,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileSizeBytes = fileSizeBytes,
        targetSizePx = PLAYBACK_BACKGROUND_BLUR_SIZE_PX,
    ) ?: return null
    val blurredArtwork = loadCachedArtworkDerivative(
        context = context,
        cacheKey = layerKey,
    ) {
        createBlurredArtwork(blurSource)
    } ?: return null
    return BlurredArtworkLayer(
        key = layerKey,
        blurredArtwork = blurredArtwork,
    ).also(::cacheBlurredArtworkLayer)
}

private fun createBlurredArtworkLayerKey(
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
): String {
    val artworkKey = createArtworkCacheKey(
        contentUri = contentUri,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileSizeBytes = fileSizeBytes,
        targetSizePx = PLAYBACK_BACKGROUND_BLUR_SIZE_PX,
    )
    return buildString {
        append(artworkKey)
        append("|playback-blur|")
        append(BLURRED_ARTWORK_CACHE_SCHEMA_VERSION)
        append('|')
        append(PLAYBACK_BACKGROUND_BLUR_SIZE_PX)
        append('|')
        append(PLAYBACK_BACKGROUND_BLUR_RADIUS)
    }
}

private fun getCachedBlurredArtworkLayer(key: String): BlurredArtworkLayer? =
    synchronized(blurredArtworkLayerMemoryCache) {
        blurredArtworkLayerMemoryCache[key]
    }

private fun cacheBlurredArtworkLayer(layer: BlurredArtworkLayer) {
    synchronized(blurredArtworkLayerMemoryCache) {
        blurredArtworkLayerMemoryCache[layer.key] = layer
    }
}

@Composable
private fun rememberBlurredArtworkLayerBlend(
    targetLayer: BlurredArtworkLayer?,
    animateTransition: Boolean,
): BlurredArtworkLayerBlend {
    var previousLayer by remember { mutableStateOf<BlurredArtworkLayer?>(null) }
    var currentLayer by remember { mutableStateOf(targetLayer) }
    var hasPreviousLayer by remember { mutableStateOf(false) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(targetLayer?.key, animateTransition) {
        if (targetLayer?.key == currentLayer?.key && !hasPreviousLayer) {
            return@LaunchedEffect
        }
        if (!animateTransition || currentLayer == null && !hasPreviousLayer) {
            previousLayer = null
            currentLayer = targetLayer
            hasPreviousLayer = false
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        previousLayer = if (progress.value < 0.5f && hasPreviousLayer) {
            previousLayer
        } else {
            currentLayer
        }
        currentLayer = targetLayer
        hasPreviousLayer = true
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = PLAYBACK_BACKGROUND_TRANSITION_DURATION_MILLIS,
                easing = PLAYER_TRACK_ARTWORK_CROSSFADE_EASING,
            ),
        )
        hasPreviousLayer = false
        previousLayer = null
    }

    return BlurredArtworkLayerBlend(
        previousLayer = previousLayer,
        currentLayer = currentLayer,
        progress = progress.value,
        hasPreviousLayer = hasPreviousLayer,
    )
}

private fun createBlurredArtwork(source: Bitmap): Bitmap {
    synchronized(blurredArtworkMemoryCache) {
        blurredArtworkMemoryCache[source]
    }?.let { return it }

    val sampled = Bitmap.createBitmap(
        PLAYBACK_BACKGROUND_BLUR_SIZE_PX,
        PLAYBACK_BACKGROUND_BLUR_SIZE_PX,
        Bitmap.Config.ARGB_8888,
    )
    val sourceSize = minOf(source.width, source.height)
    val sourceLeft = (source.width - sourceSize) / 2
    val sourceTop = (source.height - sourceSize) / 2
    val artworkPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(
            ColorMatrix().apply {
                setSaturation(BLURRED_ARTWORK_BACKGROUND_SATURATION)
            },
        )
    }
    Canvas(sampled).apply {
        drawBitmap(
            source,
            Rect(sourceLeft, sourceTop, sourceLeft + sourceSize, sourceTop + sourceSize),
            RectF(
                0f,
                0f,
                PLAYBACK_BACKGROUND_BLUR_SIZE_PX.toFloat(),
                PLAYBACK_BACKGROUND_BLUR_SIZE_PX.toFloat(),
            ),
            artworkPaint,
        )
        drawColor(BLURRED_ARTWORK_BACKGROUND_OVERLAY_COLOR, PorterDuff.Mode.OVERLAY)
        drawColor(BLURRED_ARTWORK_BACKGROUND_OVERLAY_COLOR)
    }
    val blurred = blurArtworkLikeRenderScript(
        source = sampled,
        radius = PLAYBACK_BACKGROUND_BLUR_RADIUS,
    )
    sampled.recycle()
    synchronized(blurredArtworkMemoryCache) {
        blurredArtworkMemoryCache[source] = blurred
    }
    return blurred
}

private fun blurArtworkLikeRenderScript(source: Bitmap, radius: Int): Bitmap {
    val width = source.width
    val height = source.height
    var pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)
    val horizontalPixels = IntArray(pixels.size)

    createRenderScriptBlurBoxSizes(radius).forEach { boxSize ->
        val outputPixels = IntArray(pixels.size)
        val boxRadius = boxSize / 2
        boxBlurHorizontal(
            source = pixels,
            destination = horizontalPixels,
            width = width,
            height = height,
            radius = boxRadius,
        )
        boxBlurVertical(
            source = horizontalPixels,
            destination = outputPixels,
            width = width,
            height = height,
            radius = boxRadius,
        )
        pixels = outputPixels
    }

    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
}

internal fun createRenderScriptBlurBoxSizes(radius: Int): IntArray {
    val boundedRadius = radius.coerceIn(1, 25)
    val sigma = 0.4f * boundedRadius + 0.6f
    val passCount = 3
    val idealWidth = sqrt((12f * sigma * sigma / passCount) + 1f)
    var lowerWidth = floor(idealWidth).toInt()
    if (lowerWidth % 2 == 0) lowerWidth -= 1
    val upperWidth = lowerWidth + 2
    val lowerPassCount = round(
        (12f * sigma * sigma -
            passCount * lowerWidth * lowerWidth -
            4f * passCount * lowerWidth -
            3f * passCount) /
            (-4f * lowerWidth - 4f),
    ).toInt().coerceIn(0, passCount)
    return IntArray(passCount) { index ->
        if (index < lowerPassCount) lowerWidth else upperWidth
    }
}

private fun boxBlurHorizontal(
    source: IntArray,
    destination: IntArray,
    width: Int,
    height: Int,
    radius: Int,
) {
    val windowSize = radius * 2 + 1
    for (y in 0 until height) {
        val rowOffset = y * width
        var alpha = 0
        var red = 0
        var green = 0
        var blue = 0
        for (offset in -radius..radius) {
            val color = source[rowOffset + offset.coerceIn(0, width - 1)]
            alpha += color ushr 24 and 0xff
            red += color ushr 16 and 0xff
            green += color ushr 8 and 0xff
            blue += color and 0xff
        }
        for (x in 0 until width) {
            destination[rowOffset + x] = packAveragedArgb(
                alpha = alpha,
                red = red,
                green = green,
                blue = blue,
                divisor = windowSize,
            )
            val removed = source[rowOffset + (x - radius).coerceIn(0, width - 1)]
            val added = source[rowOffset + (x + radius + 1).coerceIn(0, width - 1)]
            alpha += (added ushr 24 and 0xff) - (removed ushr 24 and 0xff)
            red += (added ushr 16 and 0xff) - (removed ushr 16 and 0xff)
            green += (added ushr 8 and 0xff) - (removed ushr 8 and 0xff)
            blue += (added and 0xff) - (removed and 0xff)
        }
    }
}

private fun boxBlurVertical(
    source: IntArray,
    destination: IntArray,
    width: Int,
    height: Int,
    radius: Int,
) {
    val windowSize = radius * 2 + 1
    for (x in 0 until width) {
        var alpha = 0
        var red = 0
        var green = 0
        var blue = 0
        for (offset in -radius..radius) {
            val color = source[offset.coerceIn(0, height - 1) * width + x]
            alpha += color ushr 24 and 0xff
            red += color ushr 16 and 0xff
            green += color ushr 8 and 0xff
            blue += color and 0xff
        }
        for (y in 0 until height) {
            destination[y * width + x] = packAveragedArgb(
                alpha = alpha,
                red = red,
                green = green,
                blue = blue,
                divisor = windowSize,
            )
            val removed = source[(y - radius).coerceIn(0, height - 1) * width + x]
            val added = source[(y + radius + 1).coerceIn(0, height - 1) * width + x]
            alpha += (added ushr 24 and 0xff) - (removed ushr 24 and 0xff)
            red += (added ushr 16 and 0xff) - (removed ushr 16 and 0xff)
            green += (added ushr 8 and 0xff) - (removed ushr 8 and 0xff)
            blue += (added and 0xff) - (removed and 0xff)
        }
    }
}

private fun packAveragedArgb(
    alpha: Int,
    red: Int,
    green: Int,
    blue: Int,
    divisor: Int,
): Int =
    ((alpha / divisor).coerceIn(0, 0xff) shl 24) or
        ((red / divisor).coerceIn(0, 0xff) shl 16) or
        ((green / divisor).coerceIn(0, 0xff) shl 8) or
        (blue / divisor).coerceIn(0, 0xff)

private fun createKenBurnsFrame(key: String, segmentIndex: Int): KenBurnsFrame {
    val seed = key.hashCode().toLong() xor
        (segmentIndex.toLong() * -7046029254386353131L)
    val random = Random(seed)
    return KenBurnsFrame(
        scale = 1.08f + random.nextFloat() * 0.12f,
        horizontalBias = random.nextFloat() * 2f - 1f,
        verticalBias = random.nextFloat() * 2f - 1f,
    )
}

internal fun interpolateKenBurnsFrame(
    start: KenBurnsFrame,
    end: KenBurnsFrame,
    progress: Float,
): KenBurnsFrame {
    val amount = progress.coerceIn(0f, 1f)
    return KenBurnsFrame(
        scale = start.scale + (end.scale - start.scale) * amount,
        horizontalBias = start.horizontalBias +
            (end.horizontalBias - start.horizontalBias) * amount,
        verticalBias = start.verticalBias +
            (end.verticalBias - start.verticalBias) * amount,
    )
}

private fun accelerateDecelerate(progress: Float): Float =
    (cos((progress.coerceIn(0f, 1f) + 1f) * PI) / 2.0 + 0.5).toFloat()
