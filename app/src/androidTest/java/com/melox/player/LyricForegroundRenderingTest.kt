package com.melox.player

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.melox.player.ui.screen.playback.drawLyricForeground
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LyricForegroundRenderingTest {
    @Test
    fun foregroundClearsCachedGlowPaintBeforeUnstartedAndCompletedGlyphs() {
        val density = Density(1f)
        val measurer = TextMeasurer(
            defaultFontFamilyResolver = createFontFamilyResolver(
                InstrumentationRegistry.getInstrumentation().targetContext,
            ),
            defaultDensity = density,
            defaultLayoutDirection = LayoutDirection.Ltr,
        )
        for (text in listOf("W", "fgjpqy")) {
            val glyph = measurer.measure(text, TextStyle(fontSize = 40.sp))
            val position = Offset(64f, 48f)
            for (alpha in listOf(0.4f, 0.7f, 1f)) {
                val color = Color.White.copy(alpha = alpha)
                val expected = render(density) {
                    drawLyricForeground(glyph, color, position)
                }
                // Recording a glow changes the same paragraph paint reused by foreground draws.
                render(density) {
                    drawText(
                        glyph,
                        color = Color.White,
                        topLeft = position,
                        shadow = Shadow(Color.White, Offset.Zero, 12f),
                    )
                }
                val actual = render(density) {
                    drawLyricForeground(glyph, color, position)
                }
                assertArrayEquals("Foreground retained a glow for $text at alpha=$alpha", expected, actual)
            }
        }
    }

    private fun render(density: Density, draw: DrawScope.() -> Unit): IntArray {
        val bitmap = Bitmap.createBitmap(256, 160, Bitmap.Config.ARGB_8888)
        CanvasDrawScope().draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = Canvas(bitmap.asImageBitmap()),
            size = Size(256f, 160f),
            block = draw,
        )
        return IntArray(256 * 160).also {
            bitmap.getPixels(it, 0, 256, 0, 0, 256, 160)
            bitmap.recycle()
        }
    }
}
