package com.melox.player

import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.drawToBitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.melox.player.ui.component.TopBarBackdrop
import com.melox.player.ui.component.PageScaffold
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import com.melox.player.ui.component.SmallTopAppBar
import com.melox.player.ui.component.TopAppBar
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

@RunWith(AndroidJUnit4::class)
class TopAppBarStatusBarSpaceTest {
    @Test
    fun largeBarRetainsItsBackgroundAndBoundsWhenStatusBarHides() = checkBar(small = false)

    @Test
    fun smallBarRetainsItsBackgroundAndBoundsWhenStatusBarHides() = checkBar(small = true)

    @Test
    fun progressiveGlassBarMeasuresWithStaleCollapseOffset() {
        assumeTrue(isRuntimeShaderSupported())
        val measured = CountDownLatch(1)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContentView(ComposeView(activity).apply {
                    setContent {
                        MiuixTheme(colors = lightColorScheme()) {
                            GlassTopAppBar(
                                title = "Top app bar",
                                modifier = Modifier
                                    .requiredWidth(with(LocalDensity.current) { 1220.toDp() })
                                    .onSizeChanged { if (it.width == 1220 && it.height > 0) measured.countDown() },
                                backdrop = rememberLayerBackdrop(),
                                progressiveBlur = true,
                                scrollBehavior = MiuixScrollBehavior(
                                    state = rememberTopAppBarState(
                                        initialHeightOffsetLimit = -1f,
                                        initialHeightOffset = -Float.MAX_VALUE,
                                    ),
                                ),
                            )
                        }
                    }
                })
            }
            assertTrue("The progressive Glass bar did not measure", measured.await(5, TimeUnit.SECONDS))
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        }
    }

    private fun checkBar(small: Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val ready = CountDownLatch(1)
        val barBounds = AtomicReference(Rect.Zero)
        val bodyBounds = AtomicReference(Rect.Zero)
        val barColor = Color(0xFF1457AD)
        val statusBars = WindowInsetsCompat.Type.statusBars()
        lateinit var view: ComposeView
        lateinit var controller: WindowInsetsControllerCompat
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                WindowCompat.setDecorFitsSystemWindows(activity.window, false)
                controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                view = ComposeView(activity).apply {
                    setContent {
                        MiuixTheme(colors = lightColorScheme(surface = barColor)) {
                            PageScaffold(
                                topBar = {
                                    TopBarBackdrop(backdrop = null) {
                                        val modifier = Modifier
                                            .onGloballyPositioned { barBounds.set(it.boundsInRoot()) }
                                        if (small) {
                                            SmallTopAppBar(
                                                title = "Status bar space",
                                                modifier = modifier,
                                                color = barColor,
                                            )
                                        } else {
                                            TopAppBar(
                                                title = "Status bar space",
                                                modifier = modifier,
                                                color = barColor,
                                            )
                                        }
                                    }
                                },
                            ) { padding ->
                                Box(Modifier.fillMaxSize().padding(padding)) {
                                    Box(Modifier.fillMaxSize().onGloballyPositioned {
                                        bodyBounds.set(it.boundsInRoot())
                                        ready.countDown()
                                    })
                                }
                            }
                        }
                    }
                }
                activity.setContentView(view)
            }
            assertTrue("Top bar did not measure", ready.await(5, TimeUnit.SECONDS))
            instrumentation.waitForIdleSync()
            val originalVisible = ViewCompat.getRootWindowInsets(view)?.isVisible(statusBars) ?: true
            fun setVisible(visible: Boolean) {
                instrumentation.runOnMainSync {
                    if (visible) controller.show(statusBars) else controller.hide(statusBars)
                }
                val deadline = SystemClock.uptimeMillis() + 5_000
                while (ViewCompat.getRootWindowInsets(view)?.isVisible(statusBars) != visible &&
                    SystemClock.uptimeMillis() < deadline
                ) {
                    SystemClock.sleep(20)
                }
                assertEquals(visible, ViewCompat.getRootWindowInsets(view)?.isVisible(statusBars))
                SystemClock.sleep(250)
                instrumentation.waitForIdleSync()
            }
            fun assertTopRegionColor() {
                val topInset = ViewCompat.getRootWindowInsets(view)
                    ?.getInsetsIgnoringVisibility(statusBars)?.top ?: 0
                assumeTrue("A top status-bar region is required", topInset > 0)
                instrumentation.runOnMainSync {
                    val bitmap = view.drawToBitmap()
                    try {
                        assertEquals(barColor.toArgb(), bitmap.getPixel(bitmap.width / 2, topInset / 2))
                    } finally {
                        bitmap.recycle()
                    }
                }
            }
            try {
                setVisible(true)
                val visibleBounds = barBounds.get() to bodyBounds.get()
                assertTrue(visibleBounds.first.height > 0)
                assertTopRegionColor()
                setVisible(false)
                assertEquals(visibleBounds, barBounds.get() to bodyBounds.get())
                assertTopRegionColor()
                setVisible(true)
                assertEquals(visibleBounds, barBounds.get() to bodyBounds.get())
                assertTopRegionColor()
            } finally {
                instrumentation.runOnMainSync {
                    if (originalVisible) controller.show(statusBars) else controller.hide(statusBars)
                }
            }
        }
    }
}
