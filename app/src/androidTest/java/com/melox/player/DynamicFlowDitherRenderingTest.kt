package com.melox.player

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.melox.player.ui.component.playback.DynamicFlowBackgroundState
import com.melox.player.ui.component.playback.DynamicFlowDitherPainter
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 33)
class DynamicFlowDitherRenderingTest {
    @Test
    fun finalNoiseIsStableAndPreservesColorAndTransitionEndpoints() {
        val state = DynamicFlowBackgroundState()
        val frame = Bitmap.createBitmap(32, 64, Bitmap.Config.ARGB_8888)
        for (y in 0 until frame.height) {
            for (x in 0 until frame.width) {
                val variation = (3f * sin(x / 8f) * cos(y / 13f)).roundToInt()
                frame.setPixel(x, y, Color.rgb(79 + variation, 74 + variation, 80 + variation))
            }
        }
        state.publishFrame(frame)
        lateinit var view: View
        var dither = false
        val painter = DynamicFlowDitherPainter()
        val firstDraw = CountDownLatch(1)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                view = object : View(activity) {
                    override fun onDraw(canvas: Canvas) {
                        assertTrue(canvas.isHardwareAccelerated)
                        if (dither) {
                            assertTrue(state.drawDitheredTo(canvas, width.toFloat(), height.toFloat(), painter))
                        } else {
                            state.drawTo(canvas, width.toFloat(), height.toFloat())
                            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply {
                                shader = LinearGradient(
                                    0f, 0f, 0f, height.toFloat(),
                                    intArrayOf(0x2e000000, Color.TRANSPARENT, 0x4d000000),
                                    null, Shader.TileMode.CLAMP,
                                )
                            })
                        }
                        firstDraw.countDown()
                    }
                }
                activity.setContentView(view)
            }
            assertTrue(firstDraw.await(10, TimeUnit.SECONDS))
            val before = capture(scenario, view)
            scenario.onActivity { dither = true; view.invalidate() }
            val after = capture(scenario, view)
            scenario.onActivity { view.invalidate() }
            assertArrayEquals("A static redraw must not shimmer", pixels(after), pixels(capture(scenario, view)))
            val original = pixels(before)
            val updated = pixels(after)
            var totalDifference = 0L
            var changed = 0
            var beforePlateaus = 0
            var afterPlateaus = 0
            for (index in original.indices) {
                assertTrue(Color.alpha(updated[index]) == 255)
                val difference = Color.red(updated[index]) - Color.red(original[index])
                assertTrue("Noise changed the background by more than one level", abs(difference) <= 1)
                totalDifference += difference
                if (original[index] != updated[index]) changed++
                if (index % before.width != 0) {
                    if (Color.red(original[index]) == Color.red(original[index - 1])) beforePlateaus++
                    if (Color.red(updated[index]) == Color.red(updated[index - 1])) afterPlateaus++
                }
            }
            assertTrue("Noise introduced a visible brightness bias", abs(totalDifference.toDouble() / original.size) < 0.2)
            assertTrue("Output dithering did not reach physical pixels", changed > original.size / 20)
            assertTrue("Large flat color runs were not reduced", afterPlateaus < beforePlateaus * 0.9)
            save(before, "dynamic-flow-before.png")
            save(after, "dynamic-flow-after.png")
            scenario.onActivity { state.beginTransition(frame); state.updateTransition(0.5f); view.invalidate() }
            assertArrayEquals("Identical-frame fading must not alter output", updated, pixels(capture(scenario, view)))
            scenario.onActivity { state.updateTransition(1f); state.publishFrame(frame); view.invalidate() }
            assertArrayEquals("Settling must preserve the dither pattern", updated, pixels(capture(scenario, view)))
            val nextFrame = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.RED)
            }
            scenario.onActivity { state.beginTransition(nextFrame); view.invalidate() }
            assertArrayEquals("A new fade must begin at the displayed output", updated, pixels(capture(scenario, view)))
            scenario.onActivity { state.updateTransition(1f); view.invalidate() }
            val endpoint = pixels(capture(scenario, view))
            assertTrue(endpoint.all { Color.alpha(it) == 255 && Color.red(it) >= 177 &&
                Color.green(it) <= 1 && Color.blue(it) <= 1 })
            scenario.onActivity { state.publishFrame(nextFrame); view.invalidate() }
            assertArrayEquals("Releasing the previous texture must not change the endpoint",
                endpoint, pixels(capture(scenario, view)))
        }
    }

    private fun capture(scenario: ActivityScenario<MainActivity>, view: View): Bitmap {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        // PixelCopy may see the previous submitted frame immediately after invalidate().
        Thread.sleep(150)
        val done = CountDownLatch(1)
        lateinit var output: Bitmap
        var result = -1
        scenario.onActivity { activity ->
            val location = IntArray(2)
            view.getLocationInWindow(location)
            output = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            PixelCopy.request(
                activity.window,
                Rect(location[0], location[1], location[0] + view.width, location[1] + view.height),
                output, { result = it; done.countDown() }, Handler(Looper.getMainLooper()),
            )
        }
        assertTrue(done.await(10, TimeUnit.SECONDS))
        assertTrue("PixelCopy failed: $result", result == PixelCopy.SUCCESS)
        return output
    }

    private fun pixels(bitmap: Bitmap) = IntArray(bitmap.width * bitmap.height).also {
        bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    }

    private fun save(bitmap: Bitmap, name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.cacheDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
