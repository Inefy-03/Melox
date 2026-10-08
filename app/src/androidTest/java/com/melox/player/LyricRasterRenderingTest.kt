package com.melox.player

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.melox.player.ui.screen.playback.drawLyricForeground
import com.melox.player.ui.screen.playback.drawLyricRasterOrText
import com.melox.player.ui.screen.playback.prepareLyricGlyphGlow
import com.melox.player.ui.screen.playback.prepareLyricTextRaster
import com.melox.player.ui.screen.playback.lyricFadeBounds
import com.melox.player.ui.screen.playback.LYRIC_INACTIVE_TEXT_ALPHA
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class LyricRasterRenderingTest {
    private val density = Density(1f)
    private fun measure(text: String) = TextMeasurer(
        createFontFamilyResolver(InstrumentationRegistry.getInstrumentation().targetContext),
        density, LayoutDirection.Ltr,
    ).measure(text, TextStyle(fontSize = 32.sp))

    @Test
    fun completedGlyphSlicesMatchWholeParagraphAtFractionalPositions() {
        val layout = TextMeasurer(
            createFontFamilyResolver(InstrumentationRegistry.getInstrumentation().targetContext),
            density, LayoutDirection.Ltr,
        ).measure("hanser", TextStyle(fontSize = 64.sp))
        val raster = requireNotNull(prepareLyricTextRaster(layout, Color.White, density, LayoutDirection.Ltr, 1_000_000))
        for (fraction in listOf(0f, 0.25f, 0.5f, 0.75f)) {
            val position = Offset(20f + fraction, 30f - fraction)
            val expected = render {
                scale(0.985f, 0.985f, Offset(20f, 120f)) {
                    raster.draw(this, position, -100f, layout.size.width + 100f)
                }
            }
            val actual = render {
                scale(0.985f, 0.985f, Offset(20f, 120f)) {
                    for (index in "hanser".indices) {
                        val left = if (index == 0) -100f else layout.getBoundingBox(index).left
                        val right = if (index == 5) layout.size.width + 100f else layout.getBoundingBox(index + 1).left
                        raster.draw(this, position, left, right)
                    }
                }
            }
            expected.indices.forEach { index ->
                assertTrue("Word completion changes coverage at fraction=$fraction pixel=$index",
                    kotlin.math.abs((expected[index] ushr 24) - (actual[index] ushr 24)) <= 1)
            }
        }
    }

    @Test
    fun translatedNarrowMaskMatchesOriginalClippedStopsNearNativeWordEnd() {
        val rowStart = 40f
        val rowWidth = 60f
        val fadeWidth = 60f
        for (progress in listOf(0.1f, 0.5f, 0.9f, 0.99f)) {
            val previousCenter = -0.5f + 2f * progress
            val previousStart = (previousCenter - 0.5f).coerceIn(0f, 1f)
            val previousEnd = (previousCenter + 0.5f).coerceIn(0f, 1f)
            val expected = render {
                drawRect(Brush.horizontalGradient(
                    0f to Color.White, previousStart to Color.White,
                    previousEnd to Color.White.copy(alpha = LYRIC_INACTIVE_TEXT_ALPHA),
                    1f to Color.White.copy(alpha = LYRIC_INACTIVE_TEXT_ALPHA),
                    startX = rowStart, endX = rowStart + rowWidth,
                ))
            }
            val fade = lyricFadeBounds(rowStart, rowStart + rowWidth, rowStart + rowWidth * progress, fadeWidth)
            val center = (fade.startX + fade.endX) / 2f
            val width = fade.endX - fade.startX
            val actual = render {
                translate(left = center) {
                    drawRect(Brush.horizontalGradient(
                        listOf(Color.White, Color.White.copy(alpha = LYRIC_INACTIVE_TEXT_ALPHA)),
                        startX = -width / 2f, endX = width / 2f,
                    ), topLeft = Offset(-center, 0f), size = size)
                }
            }
            expected.indices.forEach { index ->
                assertTrue("Mask advances early at progress=$progress pixel=$index",
                    kotlin.math.abs((expected[index] ushr 24) - (actual[index] ushr 24)) <= 1)
            }
        }
    }

    @Test
    fun cachedParagraphPreservesShapingDescendersAndAlpha() {
        for (text in listOf("fgjpqy", "office", "e\u0301", "中文", "مرحبا", "👨‍👩‍👧‍👦")) {
            val layout = measure(text)
            val raster = requireNotNull(prepareLyricTextRaster(layout, Color.White, density, LayoutDirection.Ltr, 1_000_000))
            val bounds = Rect(-raster.padding.toFloat(), -raster.padding.toFloat(),
                layout.size.width + raster.padding.toFloat(), layout.size.height + raster.padding.toFloat())
            for (alpha in listOf(1f, 0.4f)) {
                val expected = render {
                    // Lyrics applied opacity to a completed row, after drawing shaped ink at full alpha.
                    drawIntoCanvas { canvas ->
                        canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint())
                        drawLyricForeground(layout, Color.White, Offset(60f, 40f))
                        drawRect(Color.White.copy(alpha = alpha), blendMode = BlendMode.DstIn)
                        canvas.restore()
                    }
                }
                val actual = render { drawLyricRasterOrText(layout, raster, Color.White, Offset(60f, 40f), bounds, alpha) }
                if (alpha == 1f) {
                    assertArrayEquals("Cached shaping changed for $text", expected, actual)
                } else {
                    // Coverage rounds once; stored/unpremultiplied color can round twice.
                    expected.indices.forEach { index ->
                        val expectedAlpha = expected[index] ushr 24
                        val actualAlpha = actual[index] ushr 24
                        assertTrue("Coverage changed for $text at pixel $index",
                            kotlin.math.abs(expectedAlpha - actualAlpha) <= 1)
                        for (shift in listOf(0, 8, 16)) {
                            val expectedChannel = ((expected[index] shr shift and 255) * expectedAlpha / 255f).roundToInt()
                            val actualChannel = ((actual[index] shr shift and 255) * actualAlpha / 255f).roundToInt()
                            assertTrue("Shaped color changed for $text at pixel $index: $expectedChannel -> $actualChannel",
                                kotlin.math.abs(expectedChannel - actualChannel) <= 2)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun fractionalRiseChangesCoverageWithoutWholePixelStalls() {
        val layout = measure("啊")
        val raster = requireNotNull(prepareLyricTextRaster(layout, Color.White, density, LayoutDirection.Ltr, 1_000_000))
        var previous = centroid(render { raster.draw(this, Offset(60f, 60f), -100f, 100f) })
        for (step in 1..16) {
            val next = centroid(render { raster.draw(this, Offset(60f, 60f - step / 8f), -100f, 100f) })
            assertTrue("Coverage stalled at step=$step: $previous -> $next", previous - next in 0.07..0.18)
            previous = next
        }
    }

    @Test
    fun isolatedGlowHasCoverageBeyondTheGlyphWithoutChangingForeground() {
        val layout = measure("啊")
        val raster = requireNotNull(prepareLyricTextRaster(layout, Color.White, density, LayoutDirection.Ltr, 1_000_000))
        val before = render { raster.draw(this, Offset(60f, 40f), -100f, 100f) }
        val glow = prepareLyricGlyphGlow(raster, -100f, 100f, 8f, Color.White, 1_000_000)
        assertNotNull(glow)
        val halo = render { requireNotNull(glow).draw(this, Offset(60f, 40f), 1f) }
        assertTrue(before.indices.any { before[it] ushr 24 == 0 && halo[it] ushr 24 > 0 })
        val after = render { raster.draw(this, Offset(60f, 40f), -100f, 100f) }
        assertArrayEquals(before, after)
    }

    private fun centroid(pixels: IntArray): Double {
        var weight = 0L
        var weightedY = 0L
        pixels.forEachIndexed { index, color ->
            val alpha = color ushr 24
            weight += alpha
            weightedY += alpha.toLong() * (index / 256)
        }
        check(weight > 0)
        return weightedY.toDouble() / weight
    }

    private fun render(draw: DrawScope.() -> Unit): IntArray {
        val bitmap = Bitmap.createBitmap(256, 160, Bitmap.Config.ARGB_8888)
        CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(bitmap.asImageBitmap()), Size(256f, 160f), draw)
        return IntArray(256 * 160).also {
            bitmap.getPixels(it, 0, 256, 0, 0, 256, 160)
            bitmap.recycle()
        }
    }
}
