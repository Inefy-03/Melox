package com.melox.player.ui.component.playback

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.exp

private const val ARTWORK_ATMOSPHERE_SATURATION = 3f
private const val ARTWORK_ATMOSPHERE_BLUR_RADIUS_PX = 25

private const val ARTWORK_ATMOSPHERE_PX = 128

/**
 * Builds the full-player backdrop away from composition work.
 */
@Composable
internal fun rememberArtworkAtmosphere(
    artwork: Bitmap?,
    brighten: Boolean,
): Bitmap? {
    val atmosphere by produceState<Bitmap?>(
        initialValue = null,
        artwork,
        brighten,
    ) {
        value = artwork?.let { bitmap ->
            withContext(Dispatchers.Default) {
                createArtworkAtmosphere(bitmap, brighten)
            }
        }
    }
    return atmosphere
}

/**
 * Uses the FlamingoSank backdrop color pipeline with Melox's retained 128 px
 * working bitmap target:
 *
 * 1. Center-crop to a square, downscale with integer division
 *    (size / 128 => actual result 128..255 px), then force RGB_565 (16-bit).
 * 2. imageResolve(): saturation x3, apply the light/dark tonal stack, then use
 *    a background-thread 25 px Gaussian blur without bundling a native library.
 */
internal fun createArtworkAtmosphere(
    source: Bitmap,
    brighten: Boolean,
): Bitmap {
    val working = source.cropToFlamingoSankWorkingBitmap()
    val output = Bitmap.createBitmap(
        working.width.coerceAtLeast(1),
        working.height.coerceAtLeast(1),
        Bitmap.Config.ARGB_8888,
    )
    val saturationPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        isDither = true
        colorFilter = ColorMatrixColorFilter(
            ColorMatrix().apply { setSaturation(ARTWORK_ATMOSPHERE_SATURATION) },
        )
    }
    Canvas(output).apply {
        drawBitmap(working, 0f, 0f, saturationPaint)
        if (brighten) {
            drawColor(Color.argb(26, 255, 255, 255))
            drawColor(Color.WHITE, PorterDuff.Mode.OVERLAY)
            drawColor(Color.argb(82, 255, 255, 255))
            drawColor(Color.argb(191, 255, 255, 255), PorterDuff.Mode.OVERLAY)
        } else {
            // Exact FlamingoSank imageResolve() darken stack.
            drawColor(Color.argb(51, 0, 0, 0), PorterDuff.Mode.OVERLAY)
            drawColor(Color.argb(64, 0, 0, 0))
        }
    }
    return output.blurArtworkAtmosphere(ARTWORK_ATMOSPHERE_BLUR_RADIUS_PX)
}

/**
 * Uses a 25 px Gaussian kernel with clamped edges, matching FlamingoSank's
 * intended backdrop blur without a native dependency.
 */
private fun Bitmap.blurArtworkAtmosphere(radius: Int): Bitmap {
    val bitmapWidth = width
    val bitmapHeight = height
    if (radius <= 0 || bitmapWidth <= 1 || bitmapHeight <= 1) {
        return copy(Bitmap.Config.ARGB_8888, true)
    }

    val kernelRadius = radius.coerceAtMost(ARTWORK_ATMOSPHERE_BLUR_RADIUS_PX)
    val weights = gaussianWeights(kernelRadius)
    val pixels = IntArray(bitmapWidth * bitmapHeight).also {
        getPixels(it, 0, bitmapWidth, 0, 0, bitmapWidth, bitmapHeight)
    }
    val intermediate = FloatArray(pixels.size * 4)

    for (y in 0 until bitmapHeight) {
        for (x in 0 until bitmapWidth) {
            var alpha = 0f
            var red = 0f
            var green = 0f
            var blue = 0f
            for (index in weights.indices) {
                val sampleY = (y + index - kernelRadius).coerceIn(0, bitmapHeight - 1)
                val color = pixels[sampleY * bitmapWidth + x]
                val weight = weights[index]
                alpha += (color ushr 24) * weight
                red += (color shr 16 and 0xff) * weight
                green += (color shr 8 and 0xff) * weight
                blue += (color and 0xff) * weight
            }
            val offset = (y * bitmapWidth + x) * 4
            intermediate[offset] = alpha
            intermediate[offset + 1] = red
            intermediate[offset + 2] = green
            intermediate[offset + 3] = blue
        }
    }

    val blurredPixels = IntArray(pixels.size)
    for (y in 0 until bitmapHeight) {
        for (x in 0 until bitmapWidth) {
            var alpha = 0f
            var red = 0f
            var green = 0f
            var blue = 0f
            for (index in weights.indices) {
                val sampleX = (x + index - kernelRadius).coerceIn(0, bitmapWidth - 1)
                val offset = (y * bitmapWidth + sampleX) * 4
                val weight = weights[index]
                alpha += intermediate[offset] * weight
                red += intermediate[offset + 1] * weight
                green += intermediate[offset + 2] * weight
                blue += intermediate[offset + 3] * weight
            }
            blurredPixels[y * bitmapWidth + x] =
                (alpha.toInt().coerceIn(0, 0xff) shl 24) or
                    (red.toInt().coerceIn(0, 0xff) shl 16) or
                    (green.toInt().coerceIn(0, 0xff) shl 8) or
                    blue.toInt().coerceIn(0, 0xff)
        }
    }
    return Bitmap.createBitmap(
        blurredPixels,
        bitmapWidth,
        bitmapHeight,
        Bitmap.Config.ARGB_8888,
    )
}

private fun gaussianWeights(radius: Int): FloatArray {
    val kernelRadius = radius
    val sigma = radius * 0.4f + 0.6f
    val exponentScale = -1f / (2f * sigma * sigma)
    val weights = FloatArray(kernelRadius * 2 + 1)
    var totalWeight = 0f
    for (offset in -kernelRadius..kernelRadius) {
        val weight = exp(offset.toFloat() * offset * exponentScale)
        weights[offset + kernelRadius] = weight
        totalWeight += weight
    }
    for (index in weights.indices) {
        weights[index] /= totalWeight
    }
    return weights
}

/**
 * Melox keeps the FlamingoSank crop/downscale shape while using a 128 px target:
 * center-crop to a square, downscale using integer division so the result is
 * in the 128..255 px range, then force RGB_565 (16-bit).
 */
private fun Bitmap.cropToFlamingoSankWorkingBitmap(): Bitmap {
    val size = minOf(width, height)
    val xOffset = (width - size) / 2
    val yOffset = (height - size) / 2
    val square = Bitmap.createBitmap(this, xOffset, yOffset, size, size)
    val resized = if (size > ARTWORK_ATMOSPHERE_PX) {
        val scaleFactor = size / ARTWORK_ATMOSPHERE_PX
        val scaledSize = size / scaleFactor
        Bitmap.createScaledBitmap(square, scaledSize, scaledSize, true)
    } else {
        square.copy(Bitmap.Config.ARGB_8888, true)
    }
    return resized.copy(Bitmap.Config.RGB_565, false) ?: resized
}
