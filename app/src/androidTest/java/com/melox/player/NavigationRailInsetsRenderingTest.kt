package com.melox.player

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.core.view.drawToBitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.rememberNavigationRailState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.theme.MiuixTheme

@RunWith(AndroidJUnit4::class)
class NavigationRailInsetsRenderingTest {
    @Test
    fun explicitTopInsetMovesContentWhileNativeSurfaceAndDividerKeepFullHeight() = withRail { rail ->
        val originalBounds = rail.bounds.get()
        val originalItem = rail.itemBounds.get()
        val topInset = (40 * rail.view.resources.displayMetrics.density).roundToInt()
        rail.update { rail.topInsets.value = WindowInsets(top = topInset) }

        assertEquals(originalBounds, rail.bounds.get())
        assertEquals(0f, originalBounds.top, 0.5f)
        assertEquals(rail.view.height.toFloat(), originalBounds.bottom, 0.5f)
        assertEquals(originalItem.top + topInset, rail.itemBounds.get().top, 0.5f)
        rail.onMain {
            val bitmap = rail.view.drawToBitmap()
            try {
                val centerX = originalBounds.center.x.roundToInt()
                val dividerX = originalBounds.right.roundToInt() - 1
                val topY = topInset / 2
                val middleY = bitmap.height / 2
                assertEquals(RAIL_COLOR.toArgb(), bitmap.getPixel(centerX, topY))
                assertEquals(bitmap.getPixel(dividerX, middleY), bitmap.getPixel(dividerX, topY))
                assertNotEquals(RAIL_COLOR.toArgb(), bitmap.getPixel(dividerX, topY))
            } finally {
                bitmap.recycle()
            }
        }
        rail.update { rail.topInsets.value = WindowInsets(0, 0, 0, 0) }
        assertEquals(originalItem, rail.itemBounds.get())
    }

    @Test
    fun explicitVisibleInsetsPreserveLegacyOverloadDrawing() = withRail { rail ->
        rail.update { rail.legacy.value = true }
        val originalBounds = rail.bounds.get() to rail.itemBounds.get()
        val originalPixels = rail.pixels()
        rail.update {
            rail.topInsets.value = null
            rail.legacy.value = false
        }
        assertEquals(originalBounds, rail.bounds.get() to rail.itemBounds.get())
        assertArrayEquals(originalPixels, rail.pixels())
    }

    @Test
    fun explicitTopInsetUsesTheStockBottomScrollLayout() = withRail { rail ->
        rail.update {
            rail.legacy.value = true
            rail.itemCount.value = 20
        }
        rail.onMain { runBlocking { rail.scrollState.scrollTo(rail.scrollState.maxValue) } }
        rail.update {}

        val stockViewport = rail.scrollState.viewportSize
        val stockScrollRange = rail.scrollState.maxValue
        val stockLastItem = rail.lastItemBounds.get()
        assertTrue(stockScrollRange > 0)
        assertTrue(stockLastItem.height > 0)

        rail.update {
            rail.topInsets.value = null
            rail.legacy.value = false
        }
        rail.onMain { runBlocking { rail.scrollState.scrollTo(rail.scrollState.maxValue) } }
        rail.update {}

        assertEquals(stockViewport, rail.scrollState.viewportSize)
        assertEquals(stockScrollRange, rail.scrollState.maxValue)
        assertEquals(stockLastItem, rail.lastItemBounds.get())
    }

    private fun withRail(check: (RailFixture) -> Unit) {
        val ready = CountDownLatch(1)
        val fixture = RailFixture()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                WindowCompat.setDecorFitsSystemWindows(activity.window, false)
                fixture.view = ComposeView(activity).apply {
                    setContent {
                        MiuixTheme {
                            val state = rememberNavigationRailState()
                            val modifier = Modifier.fillMaxHeight().onGloballyPositioned {
                                fixture.bounds.set(it.boundsInRoot())
                                ready.countDown()
                            }
                            val content: @Composable ColumnScope.() -> Unit = {
                                repeat(fixture.itemCount.value) { index ->
                                    NavigationRailItem(
                                        selected = index == 0,
                                        onClick = {},
                                        icon = MiuixIcons.Home,
                                        label = "Home $index",
                                        modifier = Modifier.onGloballyPositioned {
                                            if (index == 0) fixture.itemBounds.set(it.boundsInRoot())
                                            if (index == fixture.itemCount.value - 1) {
                                                fixture.lastItemBounds.set(it.boundsInRoot())
                                            }
                                        },
                                    )
                                }
                            }
                            if (fixture.legacy.value) {
                                NavigationRail(
                                    state = state,
                                    modifier = modifier,
                                    color = RAIL_COLOR,
                                    scrollState = fixture.scrollState,
                                    content = content,
                                )
                            } else {
                                NavigationRail(
                                    state = state,
                                    statusBarInsets = fixture.topInsets.value ?: WindowInsets.statusBars,
                                    modifier = modifier,
                                    color = RAIL_COLOR,
                                    scrollState = fixture.scrollState,
                                    content = content,
                                )
                            }
                        }
                    }
                }
                activity.setContentView(fixture.view)
            }
            assertTrue("Rail did not measure", ready.await(5, TimeUnit.SECONDS))
            fixture.update {}
            check(fixture)
        }
    }

    private class RailFixture {
        lateinit var view: ComposeView
        val bounds = AtomicReference(Rect.Zero)
        val itemBounds = AtomicReference(Rect.Zero)
        val lastItemBounds = AtomicReference(Rect.Zero)
        val legacy = mutableStateOf(false)
        val itemCount = mutableStateOf(1)
        val scrollState = ScrollState(0)
        val topInsets: MutableState<WindowInsets?> = mutableStateOf(WindowInsets(0, 0, 0, 0))

        fun onMain(action: () -> Unit) =
            InstrumentationRegistry.getInstrumentation().runOnMainSync(action)

        fun update(action: () -> Unit) {
            val ready = CountDownLatch(1)
            onMain {
                action()
                view.postOnAnimation { view.postOnAnimation { ready.countDown() } }
            }
            assertTrue("Rail did not finish layout", ready.await(5, TimeUnit.SECONDS))
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        }

        fun pixels(): IntArray {
            lateinit var pixels: IntArray
            onMain {
                val bitmap = view.drawToBitmap()
                try {
                    pixels = IntArray(bitmap.width * bitmap.height)
                    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                } finally {
                    bitmap.recycle()
                }
            }
            return pixels
        }
    }

    private companion object {
        val RAIL_COLOR = Color(0xFF1457AD)
    }
}
