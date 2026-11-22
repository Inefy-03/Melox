package com.melox.player

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.melox.player.ui.screen.playback.PlaybackSpeedSelector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import com.melox.player.ui.glass.GlassSegmentedTabRow
import com.melox.player.ui.glass.GlassNavigationBar
import top.yukonga.miuix.kmp.upstream.glass.GlassNavigationBar as OfficialGlassNavigationBar
import top.yukonga.miuix.kmp.upstream.glass.GlassNavigationItem as OfficialGlassNavigationItem
import com.melox.player.ui.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.theme.MiuixTheme

@RunWith(AndroidJUnit4::class)
class NavigationBarInteractionTest {
    @Test
    fun framedSpeedThumbScrollsAtEdgeAndCommitsOnlyOnRelease() {
        val ready = CountDownLatch(1)
        val bounds = AtomicReference(Rect.Zero)
        val selections = mutableListOf<Int>()
        val selected = mutableIntStateOf(0)
        lateinit var view: ComposeView
        var density = 1f
        fun onMain(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
        fun event(action: Int, x: Float, downTime: Long) = onMain {
            val rect = bounds.get()
            val motion = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, rect.center.y, 0)
            try { view.dispatchTouchEvent(motion) } finally { motion.recycle() }
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                density = activity.resources.displayMetrics.density
                view = ComposeView(activity).apply {
                    setContent {
                        MiuixTheme {
                            Box(Modifier.fillMaxSize()) {
                                GlassSegmentedTabRow(
                                    tabs = List(14) { it.toString() },
                                    selectedIndex = selected.intValue,
                                    onSelect = { selections += it; selected.intValue = it },
                                    backdrop = null,
                                    tabWidth = 64.dp,
                                    modifier = Modifier.width(240.dp).onGloballyPositioned {
                                        bounds.set(it.boundsInRoot())
                                        ready.countDown()
                                    },
                                )
                            }
                        }
                    }
                }
                activity.setContentView(view)
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            val initialBounds = bounds.get()
            val downTime = SystemClock.uptimeMillis()
            event(MotionEvent.ACTION_DOWN, initialBounds.left + 35 * density, downTime)
            event(MotionEvent.ACTION_MOVE, initialBounds.right - 8 * density, downTime)
            SystemClock.sleep(1400)
            onMain { assertTrue(selections.isEmpty()) }
            event(MotionEvent.ACTION_UP, initialBounds.right - 8 * density, downTime)
            onMain {
                assertEquals(1, selections.size)
                assertTrue("The held thumb must advance beyond the initially visible tabs", selections.single() > 3)
                assertEquals(initialBounds, bounds.get())
                selected.intValue = 0
            }
            SystemClock.sleep(500)
            val cancelTime = SystemClock.uptimeMillis()
            event(MotionEvent.ACTION_DOWN, initialBounds.left + 35 * density, cancelTime)
            event(MotionEvent.ACTION_MOVE, initialBounds.right - 8 * density, cancelTime)
            SystemClock.sleep(200)
            event(MotionEvent.ACTION_CANCEL, initialBounds.right - 8 * density, cancelTime)
            onMain { assertEquals(1, selections.size) }
        }
    }

    @Test
    fun framedPlaybackSpeedMeasuresAtNarrowAndWideWidthsWithLargeText() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            for (width in listOf(240.dp, 640.dp)) {
                for (fontScale in listOf(1f, 1.6f)) {
                    val ready = CountDownLatch(1)
                    scenario.onActivity { activity ->
                        activity.setContentView(ComposeView(activity).apply {
                            setContent {
                                MiuixTheme {
                                    val density = LocalDensity.current
                                    CompositionLocalProvider(
                                        LocalDensity provides Density(density.density, fontScale),
                                    ) {
                                        Box(Modifier.requiredWidth(width)) {
                                            PlaybackSpeedSelector(
                                                playbackSpeed = 1f,
                                                onPlaybackSpeedChange = {},
                                                modifier = Modifier.onGloballyPositioned {
                                                    assertTrue(it.size.width > 0 && it.size.height > 0)
                                                    ready.countDown()
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        })
                    }
                    assertTrue("Speed selector did not measure at $width / $fontScale",
                        ready.await(5, TimeUnit.SECONDS))
                }
            }
        }
    }

    @Test
    fun bottomTapAndDragCommitOnlyOnceOnRelease() = withBar { bar ->
        val tap = bar.down(1)
        assertTrue(bar.selections.isEmpty())
        bar.event(MotionEvent.ACTION_UP, tap, 1)
        assertEquals(listOf(1), bar.selections)

        val drag = bar.down(0)
        bar.event(MotionEvent.ACTION_MOVE, drag, 1)
        bar.event(MotionEvent.ACTION_MOVE, drag, 2)
        assertEquals(listOf(1), bar.selections)
        bar.event(MotionEvent.ACTION_UP, drag, 2)
        assertEquals(listOf(1, 2), bar.selections)
    }

    @Test
    fun cancelledBottomDragDoesNotNavigate() = withBar { bar ->
        val drag = bar.down(0)
        bar.event(MotionEvent.ACTION_MOVE, drag, 2)
        bar.event(MotionEvent.ACTION_CANCEL, drag, 2)
        assertTrue(bar.selections.isEmpty())
        assertEquals(0, bar.selected.intValue)
        val tap = bar.down(1)
        bar.event(MotionEvent.ACTION_UP, tap, 1)
        assertEquals(listOf(1), bar.selections)
    }

    @Test
    fun resizedBottomBarKeepsOriginalHitRegionsInBothDirections() {
        for (direction in LayoutDirection.entries) {
            withBar(direction = direction) { bar ->
                val original = bar.bounds.get()
                val drag = bar.down(0)
                val resized = CountDownLatch(1)
                bar.resized.set(resized)
                bar.onMain { bar.width.value = 320.dp }
                assertTrue("Navigation bar did not resize", resized.await(5, TimeUnit.SECONDS))
                bar.event(MotionEvent.ACTION_MOVE, drag, 1, original)
                assertTrue(bar.selections.isEmpty())
                bar.event(MotionEvent.ACTION_UP, drag, 1, original)
                assertEquals(listOf(1), bar.selections)
            }
        }
    }

    @Test
    fun landscapeNavigationRetainsPressAndDragSelection() = withBar(selectOnRelease = false) { bar ->
        val drag = bar.down(1)
        assertEquals(listOf(1), bar.selections)
        bar.event(MotionEvent.ACTION_MOVE, drag, 2)
        assertEquals(listOf(1, 2), bar.selections)
        bar.event(MotionEvent.ACTION_UP, drag, 2)
        assertEquals(listOf(1, 2), bar.selections)
    }

    private class BarFixture(val direction: LayoutDirection) {
        lateinit var view: ComposeView
        val bounds = AtomicReference(Rect.Zero)
        val width = mutableStateOf(240.dp)
        val selected = mutableIntStateOf(0)
        val selections = mutableListOf<Int>()
        val resized = AtomicReference<CountDownLatch?>()

        fun down(index: Int): Long = SystemClock.uptimeMillis().also {
            event(MotionEvent.ACTION_DOWN, it, index)
        }

        fun onMain(action: () -> Unit) =
            InstrumentationRegistry.getInstrumentation().runOnMainSync(action)

        fun event(action: Int, downTime: Long, index: Int, rect: Rect = bounds.get()) {
            val inset = 8f * view.resources.displayMetrics.density
            val logicalX = inset + (index + 0.5f) * (rect.width - inset * 2f) / 3f
            val x = rect.left + if (direction == LayoutDirection.Rtl) rect.width - logicalX else logicalX
            onMain {
                MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, rect.center.y, 0).also {
                    it.source = InputDevice.SOURCE_TOUCHSCREEN
                    view.dispatchTouchEvent(it)
                    it.recycle()
                }
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        }
    }

    private fun withBar(
        selectOnRelease: Boolean = true,
        direction: LayoutDirection = LayoutDirection.Ltr,
        check: (BarFixture) -> Unit,
    ) {
        val ready = CountDownLatch(1)
        val fixture = BarFixture(direction)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                fixture.view = ComposeView(activity).apply {
                    setContent {
                        MiuixTheme {
                            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                                Box(Modifier.fillMaxSize()) {
                                    val barModifier = Modifier.width(fixture.width.value)
                                        .onGloballyPositioned {
                                            val bounds = it.boundsInRoot()
                                            val previous = fixture.bounds.getAndSet(bounds)
                                            if (previous.width != bounds.width) fixture.resized.get()?.countDown()
                                        }
                                        .drawWithContent {
                                            drawContent()
                                            ready.countDown()
                                        }
                                    if (selectOnRelease) {
                                        OfficialGlassNavigationBar(
                                            items = List(3) { OfficialGlassNavigationItem(MiuixIcons.Home, "Tab $it") },
                                            selectedIndex = fixture.selected.intValue,
                                            onSelect = {
                                                fixture.selections += it
                                                fixture.selected.intValue = it
                                            },
                                            backdrop = null,
                                            defaultWindowInsetsPadding = false,
                                            stroke = null,
                                            shadow = null,
                                            modifier = barModifier,
                                        )
                                    } else {
                                        GlassNavigationBar(
                                            items = List(3) { GlassNavigationItem(MiuixIcons.Home, "Tab $it") },
                                            selectedIndex = fixture.selected.intValue,
                                            onSelect = {
                                                fixture.selections += it
                                                fixture.selected.intValue = it
                                            },
                                            backdrop = null,
                                            defaultWindowInsetsPadding = false,
                                            selectOnRelease = selectOnRelease,
                                            labelsOnly = !selectOnRelease,
                                            stroke = null,
                                            shadow = null,
                                            modifier = barModifier,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                activity.setContentView(fixture.view)
            }
            assertTrue("Navigation bar did not draw", ready.await(5, TimeUnit.SECONDS))
            check(fixture)
        }
    }
}
