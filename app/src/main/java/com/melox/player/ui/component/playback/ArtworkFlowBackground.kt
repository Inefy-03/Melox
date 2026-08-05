package com.melox.player.ui.component.playback

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

private const val ARTWORK_COLOR_FIELD_SIZE = 8
private const val ARTWORK_COLOR_FIELD_MAX_CHROMA = 32.0
private const val ARTWORK_COLOR_FIELD_LIGHT_TONE = 48.0
private const val ARTWORK_COLOR_FIELD_DARK_TONE = 24.0
private const val ARTWORK_BACKGROUND_ROTATION_DURATION_MILLIS = 18_000
private const val ARTWORK_COLOR_ORBIT_DURATION_MILLIS = 42_000
private const val ARTWORK_COLOR_ORBIT_LONG_LAP_MILLIS = 24_000f
private const val ARTWORK_COLOR_ORBIT_SHORT_LAP_MILLIS = 18_000f
private const val ARTWORK_BACKGROUND_SIZE = 4
private const val ARTWORK_BACKGROUND_QUADRANT_SIZE = ARTWORK_BACKGROUND_SIZE / 2
private const val ARTWORK_COLOR_FIELD_CENTER_OFFSET =
    (ARTWORK_COLOR_FIELD_SIZE - ARTWORK_BACKGROUND_SIZE) / 2
private val MissingArtworkBackgroundColor = Color(0xFF242424)

@Composable
internal fun ArtworkFlowBackground(
    artwork: Bitmap?,
    isDark: Boolean,
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    val fieldPixels = rememberArtworkColorFieldPixels(artwork, isDark)
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
    val backgroundPixels = remember { IntArray(ARTWORK_BACKGROUND_SIZE * ARTWORK_BACKGROUND_SIZE) }
    val colorRotationPhase = remember { Animatable(0f) }
    val colorOrbitPhase = remember { Animatable(0f) }
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val animationEnabled = animate && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)

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
        if (fieldPixels != null) {
            Image(
                painter = backgroundPainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        onDrawWithContent {
                            resolveArtworkOrbitColors(
                                fieldPixels = fieldPixels,
                                cycleProgress = colorOrbitPhase.value,
                                outputPixels = orbitPixels,
                            )
                            resolveArtworkBackgroundColorRotation(
                                sourcePixels = orbitPixels,
                                rotationProgress = colorRotationPhase.value,
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

    val sourcePixels by produceState<IntArray?>(
        initialValue = null,
        artwork,
    ) {
        value = withContext(Dispatchers.Default) {
            createArtworkColorSamples(artwork)
        }
    }
    val colorFieldPixels by produceState<IntArray?>(
        initialValue = null,
        sourcePixels,
        isDark,
    ) {
        val samples = sourcePixels ?: return@produceState
        value = withContext(Dispatchers.Default) {
            createArtworkColorFieldPixels(samples, isDark)
        }
    }
    return colorFieldPixels
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
