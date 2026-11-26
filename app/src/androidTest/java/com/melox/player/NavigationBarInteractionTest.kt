package com.melox.player

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import com.melox.player.ui.glass.GlassSegmentedTabRow
import com.melox.player.ui.glass.GlassNavigationBar
import top.yukonga.miuix.kmp.upstream.glass.GlassNavigationBar as OfficialGlassNavigationBar
import top.yukonga.miuix.kmp.upstream.glass.GlassNavigationItem as OfficialGlassNavigationItem
import com.melox.player.ui.glass.GlassNavigationItem
import com.melox.player.model.NavigationTransitionStyle
import com.melox.player.ui.component.GlassMenuEntry
import com.melox.player.ui.component.GlassMenuHost
import com.melox.player.ui.component.GlassMenuLayer
import com.melox.player.ui.component.LocalGlassMenus
import com.melox.player.ui.navigation.PredictiveNavDisplay
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.nav.core.LocalNavTransitionScope
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.navBackStackOf

@RunWith(AndroidJUnit4::class)
class NavigationBarInteractionTest {
    private data class MenuRoute(val detail: Boolean) : NavKey

    @Test
    fun iconDropdownOwnsPredictiveBackUntilItsPanelExits() {
        val input = DirectNavigationEventInput()
        val stack = navBackStackOf(MenuRoute(false), MenuRoute(true))
        val anchorBounds = AtomicReference(Rect.Zero)
        var ownsBack: () -> Boolean = { false }
        var expanded = false
        var depth = 0f
        lateinit var view: ComposeView
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        fun await(message: String, condition: () -> Boolean) {
            val deadline = SystemClock.elapsedRealtime() + 5_000L
            while (true) {
                var reached = false
                instrumentation.runOnMainSync { reached = condition() }
                if (reached) return
                check(SystemClock.elapsedRealtime() < deadline) { message }
                SystemClock.sleep(16)
            }
        }

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val owner = object : NavigationEventDispatcherOwner {
                    override val navigationEventDispatcher = NavigationEventDispatcher().also { it.addInput(input) }
                }
                view = ComposeView(activity).apply {
                    setContent {
                        val menus = remember { mutableStateMapOf<Any, GlassMenuEntry>() }
                        SideEffect { ownsBack = { menus.values.any { it.ownsBack() } } }
                        CompositionLocalProvider(
                            LocalNavigationEventDispatcherOwner provides owner,
                            LocalGlassMenus provides menus,
                        ) {
                            MiuixTheme {
                                Scaffold(popupHost = { GlassMenuLayer(menus) }) {
                                    PredictiveNavDisplay(
                                        backStack = stack,
                                        predictiveBackEnabled = true,
                                        transitionStyle = NavigationTransitionStyle.MIUIX,
                                        isDark = false,
                                        menuOwnsBack = { menus.values.any { it.ownsBack() } },
                                        onBack = { stack.removeLast() },
                                    ) {
                                        entry<MenuRoute> { route ->
                                            if (route.detail) {
                                                val transition = LocalNavTransitionScope.current
                                                val relativeDepth = transition.relativeDepth
                                                SideEffect { depth = relativeDepth }
                                                GlassMenuHost(
                                                    entries = listOf(DropdownEntry(listOf(DropdownItem(text = "Sort")))),
                                                    modifier = Modifier.onGloballyPositioned { anchorBounds.set(it.boundsInRoot()) },
                                                    enabled = true,
                                                    collapseOnSelection = true,
                                                    onExpandedChange = { expanded = it },
                                                    content = { Text("Menu") },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                activity.setContentView(view)
            }
            await("The detail toolbar did not lay out") { !anchorBounds.get().isEmpty }
            scenario.onActivity {
                val center = anchorBounds.get().center
                val downTime = SystemClock.uptimeMillis()
                for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                    val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, center.x, center.y, 0)
                    try { view.dispatchTouchEvent(event) } finally { event.recycle() }
                }
            }
            await("The dropdown did not open") { expanded && ownsBack() }
            val popupFrame = CountDownLatch(1)
            scenario.onActivity { view.postOnAnimation { view.postOnAnimation { popupFrame.countDown() } } }
            assertTrue(popupFrame.await(5, TimeUnit.SECONDS))
            scenario.onActivity {
                input.backStarted(NavigationEvent(progress = 0f))
                input.backProgressed(NavigationEvent(progress = 0.5f))
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity {
                assertEquals(0f, depth, 0.001f)
                input.backCancelled()
                assertTrue(expanded)
                assertEquals(2, stack.size)
                input.backStarted(NavigationEvent(progress = 0f))
                input.backProgressed(NavigationEvent(progress = 0.5f))
                input.backCompleted()
                assertFalse(expanded)
                assertTrue(ownsBack())
                input.backCompleted()
                assertEquals(2, stack.size)
            }
            await("The closing dropdown never released Back") { !ownsBack() }
            scenario.onActivity { input.backCompleted() }
            await("Back did not return to the page stack") { stack.size == 1 }
        }
    }

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
                                        Box(
                                            Modifier.requiredWidth(width).onGloballyPositioned {
                                                assertTrue(it.size.width > 0 && it.size.height > 0)
                                                ready.countDown()
                                            },
                                        ) {
                                            PlaybackSpeedSelector(
                                                playbackSpeed = 1f,
                                                onPlaybackSpeedChange = {},
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
