package com.melox.player.ui.screen.playback

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.ceil
import kotlin.math.roundToInt

/** A shaped paragraph drawn once; fractional translation filters its fixed glyph coverage. */
internal class LyricTextRaster(
    val image: ImageBitmap,
    val padding: Int,
) {
    val byteCount: Long get() = image.width.toLong() * image.height * 4L

    fun draw(scope: DrawScope, position: Offset, left: Float, right: Float, alpha: Float = 1f) {
        val start = (left + padding).roundToInt().coerceIn(0, image.width)
        val end = (right + padding).roundToInt().coerceIn(start, image.width)
        if (end <= start) return
        with(scope) {
            translate(position.x + start - padding, position.y - padding) {
                drawImage(
                    image = image,
                    srcOffset = IntOffset(start, 0),
                    srcSize = IntSize(end - start, image.height),
                    dstSize = IntSize(end - start, image.height),
                    alpha = alpha,
                    filterQuality = FilterQuality.Low,
                )
            }
        }
    }
}

internal fun prepareLyricTextRaster(
    layout: TextLayoutResult,
    color: Color,
    density: Density,
    layoutDirection: LayoutDirection,
    maxBytes: Long,
): LyricTextRaster? {
    // The foreground needs ink overhang only; glow allocates its own expanded coverage.
    val padding = ceil(layout.size.height * 0.12f + 2f).toInt()
    val width = layout.size.width + padding * 2
    val height = layout.size.height + padding * 2
    if (width <= 0 || height <= 0 || width > 4096 || height > 4096 ||
        width.toLong() * height * 4L > maxBytes
    ) return null
    val image = ImageBitmap(width, height)
    CanvasDrawScope().draw(density, layoutDirection, Canvas(image), Size(width.toFloat(), height.toFloat())) {
        drawText(layout, color = color, topLeft = Offset(padding.toFloat(), padding.toFloat()), shadow = Shadow.None)
    }
    return LyricTextRaster(image, padding)
}

internal fun DrawScope.drawLyricRasterOrText(
    layout: TextLayoutResult,
    raster: LyricTextRaster?,
    color: Color,
    position: Offset,
    bounds: Rect,
    alpha: Float = 1f,
) {
    if (raster != null) {
        raster.draw(this, position, bounds.left, bounds.right, alpha)
    } else {
        clipRect(position.x + bounds.left, position.y + bounds.top,
            position.x + bounds.right, position.y + bounds.bottom) {
            drawLyricForeground(layout, color.copy(alpha = color.alpha * alpha), position)
        }
    }
}

internal class LyricGlyphGlow(val image: ImageBitmap, private val offset: Offset, private val color: Color) {
    val byteCount: Long get() = image.width.toLong() * image.height * 4L
    val bounds = Rect(offset, Size(image.width.toFloat(), image.height.toFloat()))

    fun draw(scope: DrawScope, position: Offset, alpha: Float) {
        with(scope) {
            translate(position.x + offset.x, position.y + offset.y) {
                drawImage(image, alpha = alpha, colorFilter = ColorFilter.tint(color), filterQuality = FilterQuality.Low)
            }
        }
    }
}

/** Blur the isolated shaped coverage; this also works on API 28-30 without RenderEffect. */
internal fun prepareLyricGlyphGlow(
    raster: LyricTextRaster,
    left: Float,
    right: Float,
    radiusPx: Float,
    color: Color,
    maxBytes: Long,
): LyricGlyphGlow? {
    if (radiusPx <= 0f) return null
    val start = (left + raster.padding).roundToInt().coerceIn(0, raster.image.width)
    val end = (right + raster.padding).roundToInt().coerceIn(start, raster.image.width)
    val blurOutset = ceil(radiusPx * 3f).toInt()
    if (end <= start || (end - start + blurOutset * 2L) *
        (raster.image.height + blurOutset * 2L) * 4L > maxBytes
    ) return null
    val original = raster.image.asAndroidBitmap()
    val slice = Bitmap.createBitmap(original, start, 0, end - start, original.height)
    val offset = IntArray(2)
    val coverage = slice.extractAlpha(android.graphics.Paint().apply {
        maskFilter = BlurMaskFilter(radiusPx, BlurMaskFilter.Blur.NORMAL)
    }, offset)
    if (slice !== original) slice.recycle()
    return LyricGlyphGlow(coverage.asImageBitmap(),
        Offset((start - raster.padding + offset[0]).toFloat(), (offset[1] - raster.padding).toFloat()), color)
}
