package com.melox.player.ui.component.playback

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.materialkolor.hct.Hct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.WeakHashMap
import kotlin.math.pow
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

private const val ARTWORK_COLOR_FIELD_SIZE = 8
private const val ARTWORK_COLOR_FIELD_MAX_CHROMA = 20.0
private const val ARTWORK_COLOR_FIELD_LIGHT_TONE = 64.0
private const val ARTWORK_COLOR_FIELD_DARK_TONE = 32.0
private const val ARTWORK_BACKGROUND_ROTATION_DURATION_MILLIS = 18_000
private const val ARTWORK_COLOR_ORBIT_DURATION_MILLIS = 42_000
private const val ARTWORK_BACKGROUND_COLOR_TRANSITION_DURATION_MILLIS = 640
private const val ARTWORK_COLOR_ORBIT_LONG_LAP_MILLIS = 24_000f
private const val ARTWORK_COLOR_ORBIT_SHORT_LAP_MILLIS = 18_000f
private const val ARTWORK_BACKGROUND_SIZE = 4
private const val ARTWORK_BACKGROUND_QUADRANT_SIZE = ARTWORK_BACKGROUND_SIZE / 2
private const val ARTWORK_COLOR_FIELD_CENTER_OFFSET =
    (ARTWORK_COLOR_FIELD_SIZE - ARTWORK_BACKGROUND_SIZE) / 2
private const val MISSING_ARTWORK_BACKGROUND_COLOR_ARGB = 0xFF242424.toInt()
private const val STATUS_BAR_DARK_BACKGROUND_LUMINANCE_THRESHOLD = 0.179f
private val MissingArtworkBackgroundColor = Color(0xFF242424)

private class ArtworkColorFieldCacheEntry {
    var sourcePixels: IntArray? = null
    var lightFieldPixels: IntArray? = null
    var darkFieldPixels: IntArray? = null
}

private val artworkColorFieldCache = WeakHashMap<Bitmap, ArtworkColorFieldCacheEntry>()

@Composable
internal fun ArtworkFlowBackground(
    artwork: Bitmap?,
    isDark: Boolean,
    animate: Boolean,
    animateColorTransition: Boolean = true,
    onStatusBarBackgroundDarkChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val targetFieldPixels = rememberArtworkColorFieldPixels(artwork, isDark)
    val fieldBlend = rememberArtworkBackgroundFieldBlend(
        artwork = artwork,
        targetFieldPixels = targetFieldPixels,
        animateTransition = animateColorTransition,
    )
    val backgroundBitmap = remember {
        Bitmap.createBitmap(
            ARTWORK_BACKGROUND_SIZE,
            ARTWORK_BACKGROUND_SIZE,
            Bitmap.Config.ARGB_8888,
        )
    }
    val backgroundPainter = remember(backgroundBitmap) {
        BitmapPainter(
            image = backgroundBitmap.asImageBitmap(),
            filterQuality = FilterQuality.Low,
        )
    }
    val orbitPixels = remember { IntArray(ARTWORK_BACKGROUND_SIZE * ARTWORK_BACKGROUND_SIZE) }
    val previousOrbitPixels = remember {
        IntArray(ARTWORK_BACKGROUND_SIZE * ARTWORK_BACKGROUND_SIZE)
    }
    val backgroundPixels = remember { IntArray(ARTWORK_BACKGROUND_SIZE * ARTWORK_BACKGROUND_SIZE) }
    val previousBackgroundPixels = remember {
        IntArray(ARTWORK_BACKGROUND_SIZE * ARTWORK_BACKGROUND_SIZE)
    }
    val colorRotationPhase = remember { Animatable(0f) }
    val colorOrbitPhase = remember { Animatable(0f) }
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val animationEnabled = animate && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val currentOnStatusBarBackgroundDarkChanged by rememberUpdatedState(
        onStatusBarBackgroundDarkChanged,
    )
    val currentFieldBlend by rememberUpdatedState(fieldBlend)

    DisposableEffect(backgroundBitmap) {
        onDispose {
            backgroundBitmap.recycle()
        }
    }

    LaunchedEffect(animationEnabled) {
        if (!animationEnabled) return@LaunchedEffect
        while (true) {
            val remainingDuration =
                (ARTWORK_BACKGROUND_ROTATION_DURATION_MILLIS * (1f - colorRotationPhase.value))
                    .roundToInt()
                    .coerceAtLeast(1)
            colorRotationPhase.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = remainingDuration,
                    easing = LinearEasing,
                ),
            )
            colorRotationPhase.snapTo(0f)
        }
    }

    LaunchedEffect(fieldBlend.fromFieldPixels, fieldBlend.toFieldPixels, artwork, animationEnabled) {
        if (fieldBlend.fromFieldPixels == null && fieldBlend.toFieldPixels == null) {
            currentOnStatusBarBackgroundDarkChanged(artwork == null || isDark)
            return@LaunchedEffect
        }
        val targetOrbitPixels = IntArray(ARTWORK_BACKGROUND_SIZE * ARTWORK_BACKGROUND_SIZE)
        val targetBackgroundPixels =
            IntArray(ARTWORK_BACKGROUND_SIZE * ARTWORK_BACKGROUND_SIZE)
        val sourceOrbitPixels = IntArray(ARTWORK_BACKGROUND_SIZE * ARTWORK_BACKGROUND_SIZE)
        val sourceBackgroundPixels =
            IntArray(ARTWORK_BACKGROUND_SIZE * ARTWORK_BACKGROUND_SIZE)
        snapshotFlow {
            Triple(
                colorRotationPhase.value,
                colorOrbitPhase.value,
                currentFieldBlend.progress,
            )
        }
            .map { (rotationProgress, orbitProgress, transitionProgress) ->
                val blend = currentFieldBlend
                resolveArtworkBackgroundPixels(
                    fieldPixels = blend.toFieldPixels,
                    orbitProgress = orbitProgress,
                    rotationProgress = rotationProgress,
                    orbitPixels = targetOrbitPixels,
                    outputPixels = targetBackgroundPixels,
                )
                resolveArtworkBackgroundPixels(
                    fieldPixels = blend.fromFieldPixels,
                    orbitProgress = orbitProgress,
                    rotationProgress = rotationProgress,
                    orbitPixels = sourceOrbitPixels,
                    outputPixels = sourceBackgroundPixels,
                )
                interpolateArtworkBackgroundPixels(
                    startPixels = sourceBackgroundPixels,
                    endPixels = targetBackgroundPixels,
                    fraction = transitionProgress,
                    outputPixels = targetBackgroundPixels,
                )
                artworkBackgroundUsesLightStatusBarIcons(targetBackgroundPixels)
            }
            .distinctUntilChanged()
            .collect(currentOnStatusBarBackgroundDarkChanged)
    }

    LaunchedEffect(animationEnabled) {
        if (!animationEnabled) return@LaunchedEffect
        while (true) {
            val remainingDuration =
                (ARTWORK_COLOR_ORBIT_DURATION_MILLIS * (1f - colorOrbitPhase.value))
                    .roundToInt()
                    .coerceAtLeast(1)
            colorOrbitPhase.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = remainingDuration,
                    easing = LinearEasing,
                ),
            )
            colorOrbitPhase.snapTo(0f)
        }
    }

    Box(
        modifier = modifier
            .background(
                if (artwork == null) {
                    MissingArtworkBackgroundColor
                } else {
                    MiuixTheme.colorScheme.surfaceContainer
                },
            )
            .clipToBounds(),
    ) {
        val shouldDrawBackground = fieldBlend.toFieldPixels != null ||
            (fieldBlend.fromFieldPixels != null && fieldBlend.progress < 1f)
        if (shouldDrawBackground) {
            Image(
                painter = backgroundPainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        onDrawWithContent {
                            resolveArtworkBackgroundPixels(
                                fieldPixels = fieldBlend.toFieldPixels,
                                orbitProgress = colorOrbitPhase.value,
                                rotationProgress = colorRotationPhase.value,
                                orbitPixels = orbitPixels,
                                outputPixels = backgroundPixels,
                            )
                            resolveArtworkBackgroundPixels(
                                fieldPixels = fieldBlend.fromFieldPixels,
                                orbitProgress = colorOrbitPhase.value,
                                rotationProgress = colorRotationPhase.value,
                                orbitPixels = previousOrbitPixels,
                                outputPixels = previousBackgroundPixels,
                            )
                            interpolateArtworkBackgroundPixels(
                                startPixels = previousBackgroundPixels,
                                endPixels = backgroundPixels,
                                fraction = fieldBlend.progress,
                                outputPixels = backgroundPixels,
                            )
                            backgroundBitmap.setPixels(
                                backgroundPixels,
                                0,
                                ARTWORK_BACKGROUND_SIZE,
                                0,
                                0,
                                ARTWORK_BACKGROUND_SIZE,
                                ARTWORK_BACKGROUND_SIZE,
                            )
                            drawContent()
                        }
                    },
            )
        }
    }
}

private data class ArtworkBackgroundFieldBlend(
    val fromFieldPixels: IntArray?,
    val toFieldPixels: IntArray?,
    val progress: Float,
)

@Composable
private fun rememberArtworkBackgroundFieldBlend(
    artwork: Bitmap?,
    targetFieldPixels: IntArray?,
    animateTransition: Boolean,
): ArtworkBackgroundFieldBlend {
    var fromFieldPixels by remember { mutableStateOf<IntArray?>(null) }
    var toFieldPixels by remember { mutableStateOf<IntArray?>(null) }
    var currentArtwork by remember { mutableStateOf<Bitmap?>(null) }
    var initialized by remember { mutableStateOf(false) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(artwork, targetFieldPixels, animateTransition) {
        if (artwork != null && targetFieldPixels == null) return@LaunchedEffect

        if (!initialized) {
            fromFieldPixels = null
            toFieldPixels = targetFieldPixels
            currentArtwork = artwork
            initialized = true
            progress.snapTo(1f)
            return@LaunchedEffect
        }

        if (!animateTransition) {
            fromFieldPixels = null
            toFieldPixels = targetFieldPixels
            currentArtwork = artwork
            progress.snapTo(1f)
            return@LaunchedEffect
        }

        if (artwork === currentArtwork) {
            fromFieldPixels = null
            toFieldPixels = targetFieldPixels
            progress.snapTo(1f)
            return@LaunchedEffect
        }

        val currentFieldPixels = interpolateArtworkBackgroundField(
            startFieldPixels = fromFieldPixels,
            endFieldPixels = toFieldPixels,
            fraction = progress.value,
        )
        fromFieldPixels = currentFieldPixels
        toFieldPixels = targetFieldPixels
        currentArtwork = artwork
        progress.snapTo(0f)

        if (currentFieldPixels == null && targetFieldPixels == null) {
            progress.snapTo(1f)
        } else {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = ARTWORK_BACKGROUND_COLOR_TRANSITION_DURATION_MILLIS,
                    easing = FastOutSlowInEasing,
                ),
            )
            fromFieldPixels = null
        }
    }

    return ArtworkBackgroundFieldBlend(
        fromFieldPixels = fromFieldPixels,
        toFieldPixels = toFieldPixels,
        progress = progress.value,
    )
}

internal fun interpolateArtworkBackgroundField(
    startFieldPixels: IntArray?,
    endFieldPixels: IntArray?,
    fraction: Float,
): IntArray? {
    if (startFieldPixels == null && endFieldPixels == null) return null

    val pixelCount = ARTWORK_COLOR_FIELD_SIZE * ARTWORK_COLOR_FIELD_SIZE
    val startPixels = startFieldPixels ?: IntArray(pixelCount) {
        MISSING_ARTWORK_BACKGROUND_COLOR_ARGB
    }
    val endPixels = endFieldPixels ?: IntArray(pixelCount) {
        MISSING_ARTWORK_BACKGROUND_COLOR_ARGB
    }
    return IntArray(pixelCount) { index ->
        interpolateArgb(
            startColor = startPixels[index],
            endColor = endPixels[index],
            fraction = fraction,
        )
    }
}

private fun resolveArtworkBackgroundPixels(
    fieldPixels: IntArray?,
    orbitProgress: Float,
    rotationProgress: Float,
    orbitPixels: IntArray,
    outputPixels: IntArray,
) {
    if (fieldPixels == null) {
        outputPixels.fill(MISSING_ARTWORK_BACKGROUND_COLOR_ARGB)
        return
    }
    resolveArtworkOrbitColors(
        fieldPixels = fieldPixels,
        cycleProgress = orbitProgress,
        outputPixels = orbitPixels,
    )
    resolveArtworkBackgroundColorRotation(
        sourcePixels = orbitPixels,
        rotationProgress = rotationProgress,
        outputPixels = outputPixels,
    )
}

private fun interpolateArtworkBackgroundPixels(
    startPixels: IntArray,
    endPixels: IntArray,
    fraction: Float,
    outputPixels: IntArray,
) {
    if (fraction >= 1f) return
    for (index in outputPixels.indices) {
        outputPixels[index] = interpolateArgb(
            startColor = startPixels[index],
            endColor = endPixels[index],
            fraction = fraction,
        )
    }
}

internal fun resolveArtworkBackgroundColorRotation(
    sourcePixels: IntArray,
    rotationProgress: Float,
    outputPixels: IntArray,
) {
    val quarterTurnCount = 4
    val rotationPosition = rotationProgress.coerceIn(0f, 1f) * quarterTurnCount
    val completedSteps = rotationPosition.toInt()
    val step = completedSteps % quarterTurnCount
    val nextStep = (step + 1) % quarterTurnCount
    val fraction = rotationPosition - completedSteps

    for (destinationY in 0 until ARTWORK_BACKGROUND_SIZE) {
        for (destinationX in 0 until ARTWORK_BACKGROUND_SIZE) {
            val destinationIndex = destinationY * ARTWORK_BACKGROUND_SIZE + destinationX
            val sourceIndex = rotatedArtworkPixelIndex(
                destinationX = destinationX,
                destinationY = destinationY,
                clockwiseQuarterTurns = step,
            )
            val nextSourceIndex = rotatedArtworkPixelIndex(
                destinationX = destinationX,
                destinationY = destinationY,
                clockwiseQuarterTurns = nextStep,
            )
            outputPixels[destinationIndex] = interpolateArgb(
                startColor = sourcePixels[sourceIndex],
                endColor = sourcePixels[nextSourceIndex],
                fraction = fraction,
            )
        }
    }
}

private fun rotatedArtworkPixelIndex(
    destinationX: Int,
    destinationY: Int,
    clockwiseQuarterTurns: Int,
): Int {
    val lastIndex = ARTWORK_BACKGROUND_SIZE - 1
    val sourceX: Int
    val sourceY: Int
    when (clockwiseQuarterTurns % 4) {
        1 -> {
            sourceX = destinationY
            sourceY = lastIndex - destinationX
        }

        2 -> {
            sourceX = lastIndex - destinationX
            sourceY = lastIndex - destinationY
        }

        3 -> {
            sourceX = lastIndex - destinationY
            sourceY = destinationX
        }

        else -> {
            sourceX = destinationX
            sourceY = destinationY
        }
    }
    return sourceY * ARTWORK_BACKGROUND_SIZE + sourceX
}

internal fun resolveArtworkOrbitColors(
    fieldPixels: IntArray,
    cycleProgress: Float,
    outputPixels: IntArray,
) {
    val elapsedMillis =
        cycleProgress.coerceIn(0f, 1f) * ARTWORK_COLOR_ORBIT_DURATION_MILLIS
    val clockwiseProgress = resolveArtworkOrbitProgress(
        elapsedMillis = elapsedMillis,
        firstLapDurationMillis = ARTWORK_COLOR_ORBIT_LONG_LAP_MILLIS,
        secondLapDurationMillis = ARTWORK_COLOR_ORBIT_SHORT_LAP_MILLIS,
    )
    val counterClockwiseProgress = resolveArtworkOrbitProgress(
        elapsedMillis = elapsedMillis,
        firstLapDurationMillis = ARTWORK_COLOR_ORBIT_SHORT_LAP_MILLIS,
        secondLapDurationMillis = ARTWORK_COLOR_ORBIT_LONG_LAP_MILLIS,
    )

    for (outputY in 0 until ARTWORK_BACKGROUND_SIZE) {
        for (outputX in 0 until ARTWORK_BACKGROUND_SIZE) {
            val isLeft = outputX < ARTWORK_BACKGROUND_QUADRANT_SIZE
            val isTop = outputY < ARTWORK_BACKGROUND_QUADRANT_SIZE
            val orbitProgress = if (isLeft == isTop) {
                clockwiseProgress
            } else {
                counterClockwiseProgress
            }
            outputPixels[outputY * ARTWORK_BACKGROUND_SIZE + outputX] =
                interpolateArtworkOrbit(
                    fieldPixels = fieldPixels,
                    baseX = outputX + ARTWORK_COLOR_FIELD_CENTER_OFFSET,
                    baseY = outputY + ARTWORK_COLOR_FIELD_CENTER_OFFSET,
                    horizontalOffset = if (isLeft) {
                        -ARTWORK_BACKGROUND_QUADRANT_SIZE
                    } else {
                        ARTWORK_BACKGROUND_QUADRANT_SIZE
                    },
                    verticalOffset = if (isTop) {
                        -ARTWORK_BACKGROUND_QUADRANT_SIZE
                    } else {
                        ARTWORK_BACKGROUND_QUADRANT_SIZE
                    },
                    progress = orbitProgress,
                )
        }
    }
}

internal fun resolveArtworkOrbitProgress(
    elapsedMillis: Float,
    firstLapDurationMillis: Float,
    secondLapDurationMillis: Float,
): Float = if (elapsedMillis <= firstLapDurationMillis) {
    elapsedMillis / firstLapDurationMillis
} else {
    1f + (elapsedMillis - firstLapDurationMillis) / secondLapDurationMillis
}

private fun interpolateArtworkOrbit(
    fieldPixels: IntArray,
    baseX: Int,
    baseY: Int,
    horizontalOffset: Int,
    verticalOffset: Int,
    progress: Float,
): Int {
    val segmentCount = 4
    val segmentPosition = (progress % 1f) * segmentCount
    val startPosition = segmentPosition.toInt().coerceIn(0, segmentCount - 1)
    val endPosition = (startPosition + 1) % segmentCount
    return interpolateArgb(
        startColor = fieldPixels[artworkOrbitPixelIndex(
            baseX = baseX,
            baseY = baseY,
            horizontalOffset = horizontalOffset,
            verticalOffset = verticalOffset,
            position = startPosition,
        )],
        endColor = fieldPixels[artworkOrbitPixelIndex(
            baseX = baseX,
            baseY = baseY,
            horizontalOffset = horizontalOffset,
            verticalOffset = verticalOffset,
            position = endPosition,
        )],
        fraction = segmentPosition - startPosition,
    )
}

private fun artworkOrbitPixelIndex(
    baseX: Int,
    baseY: Int,
    horizontalOffset: Int,
    verticalOffset: Int,
    position: Int,
): Int {
    val x = baseX + if (position == 1 || position == 2) horizontalOffset else 0
    val y = baseY + if (position == 2 || position == 3) verticalOffset else 0
    return y * ARTWORK_COLOR_FIELD_SIZE + x
}

private fun interpolateArgb(
    startColor: Int,
    endColor: Int,
    fraction: Float,
): Int {
    val amount = fraction.coerceIn(0f, 1f)
    fun channel(shift: Int): Int {
        val start = startColor ushr shift and 0xff
        val end = endColor ushr shift and 0xff
        return (start + (end - start) * amount).roundToInt().coerceIn(0, 0xff)
    }

    return (channel(24) shl 24) or
        (channel(16) shl 16) or
        (channel(8) shl 8) or
        channel(0)
}

@Composable
private fun rememberArtworkColorFieldPixels(
    artwork: Bitmap?,
    isDark: Boolean,
): IntArray? {
    if (artwork == null) return null

    val cachedFieldPixels = remember(artwork, isDark) {
        cachedArtworkColorFieldPixels(artwork, isDark)
    }
    var colorFieldPixels by remember(artwork, isDark) {
        mutableStateOf(cachedFieldPixels)
    }
    LaunchedEffect(artwork, isDark) {
        if (colorFieldPixels == null) {
            colorFieldPixels = loadArtworkColorFieldPixels(artwork, isDark)
        }
    }
    return colorFieldPixels
}

internal suspend fun prefetchArtworkColorField(
    artwork: Bitmap?,
    isDark: Boolean,
) {
    if (artwork == null) return
    loadArtworkColorFieldPixels(artwork, isDark)
}

private fun cachedArtworkColorFieldPixels(
    artwork: Bitmap,
    isDark: Boolean,
): IntArray? = synchronized(artworkColorFieldCache) {
    val entry = artworkColorFieldCache[artwork] ?: return@synchronized null
    if (isDark) entry.darkFieldPixels else entry.lightFieldPixels
}

private suspend fun loadArtworkColorFieldPixels(
    artwork: Bitmap,
    isDark: Boolean,
): IntArray = withContext(Dispatchers.Default) {
    synchronized(artworkColorFieldCache) {
        val entry = artworkColorFieldCache.getOrPut(artwork) {
            ArtworkColorFieldCacheEntry()
        }
        val cached = if (isDark) entry.darkFieldPixels else entry.lightFieldPixels
        if (cached != null) return@synchronized cached
        val sourcePixels = entry.sourcePixels ?: createArtworkColorSamples(artwork).also {
            entry.sourcePixels = it
        }
        val fieldPixels = createArtworkColorFieldPixels(sourcePixels, isDark)
        if (isDark) {
            entry.darkFieldPixels = fieldPixels
        } else {
            entry.lightFieldPixels = fieldPixels
        }
        fieldPixels
    }
}

private fun createArtworkColorSamples(source: Bitmap): IntArray {
    val scaled = Bitmap.createScaledBitmap(
        source,
        ARTWORK_COLOR_FIELD_SIZE,
        ARTWORK_COLOR_FIELD_SIZE,
        true,
    )
    val pixels = IntArray(ARTWORK_COLOR_FIELD_SIZE * ARTWORK_COLOR_FIELD_SIZE)
    scaled.getPixels(
        pixels,
        0,
        ARTWORK_COLOR_FIELD_SIZE,
        0,
        0,
        ARTWORK_COLOR_FIELD_SIZE,
        ARTWORK_COLOR_FIELD_SIZE,
    )
    if (scaled !== source) scaled.recycle()
    return pixels
}

private fun createArtworkColorFieldPixels(
    sourcePixels: IntArray,
    isDark: Boolean,
): IntArray = IntArray(sourcePixels.size) { index ->
    resolveArtworkColorFieldPixel(sourcePixels[index], isDark)
}

internal fun resolveArtworkColorFieldPixel(
    sourceColor: Int,
    isDark: Boolean,
): Int {
    val sourceHct = Hct.fromInt(sourceColor)
    val targetTone = if (isDark) {
        ARTWORK_COLOR_FIELD_DARK_TONE
    } else {
        ARTWORK_COLOR_FIELD_LIGHT_TONE
    }
    val targetChroma = sourceHct.chroma.coerceAtMost(ARTWORK_COLOR_FIELD_MAX_CHROMA)
    var resolved = Hct.from(
        hue = sourceHct.hue,
        chroma = targetChroma,
        tone = targetTone,
    )
    if (resolved.chroma <= ARTWORK_COLOR_FIELD_MAX_CHROMA) return resolved.toInt()

    var acceptedChroma = 0.0
    var rejectedChroma = targetChroma
    repeat(12) {
        val candidateChroma = (acceptedChroma + rejectedChroma) / 2.0
        val candidate = Hct.from(
            hue = sourceHct.hue,
            chroma = candidateChroma,
            tone = targetTone,
        )
        if (candidate.chroma <= ARTWORK_COLOR_FIELD_MAX_CHROMA) {
            acceptedChroma = candidateChroma
            resolved = candidate
        } else {
            rejectedChroma = candidateChroma
        }
    }
    return resolved.toInt()
}

internal fun artworkBackgroundUsesLightStatusBarIcons(backgroundPixels: IntArray): Boolean {
    if (backgroundPixels.isEmpty()) return true
    val sampleCount = minOf(ARTWORK_BACKGROUND_SIZE, backgroundPixels.size)
    val averageLuminance = (0 until sampleCount).sumOf { index ->
        relativeLuminance(backgroundPixels[index]).toDouble()
    }.toFloat() / sampleCount
    return averageLuminance < STATUS_BAR_DARK_BACKGROUND_LUMINANCE_THRESHOLD
}

private fun relativeLuminance(color: Int): Float {
    fun linearChannel(shift: Int): Float {
        val channel = ((color ushr shift) and 0xff) / 255f
        return if (channel <= 0.04045f) {
            channel / 12.92f
        } else {
            ((channel + 0.055f) / 1.055f).pow(2.4f)
        }
    }

    return 0.2126f * linearChannel(16) +
        0.7152f * linearChannel(8) +
        0.0722f * linearChannel(0)
}
