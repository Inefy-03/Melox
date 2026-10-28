package com.melox.player

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.melox.player.ui.component.playback.PlayerSheetTransitionState
import com.melox.player.ui.component.playback.playerSheetHostLayer
import com.melox.player.ui.component.playback.sharedContainerRect
import com.melox.player.ui.component.playback.rememberPlayerSheetVerticalDragModifier
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class PlayerSheetDragVelocityTest {
    @Test
    fun downwardReleaseClosesFromTheMovingInputSurface() {
        checkRelease(lastMoveFraction = 0.42f, expectedOpen = false)
    }

    @Test
    fun intentionalUpwardReversalStillExpands() {
        checkRelease(lastMoveFraction = 0.28f, expectedOpen = true)
    }

    @Test
    fun horizontalStartStaysRejectedWhenTheFingerLaterMovesDown() {
        checkRelease(lastMoveFraction = 0.7f, expectedOpen = true, horizontalFirst = true)
    }

    private fun checkRelease(lastMoveFraction: Float, expectedOpen: Boolean, horizontalFirst: Boolean = false) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val transition = PlayerSheetTransitionState(initialProgress = 1f)
        val firstDraw = CountDownLatch(1)
        val expectedDraw = AtomicReference(Float.POSITIVE_INFINITY to firstDraw)
        var size = IntSize.Zero
        var velocity = Float.NaN
        var cancelled = false
        lateinit var view: ComposeView

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                view = ComposeView(activity).apply {
                    setContent {
                        Box(
                            Modifier.fillMaxSize().onSizeChanged {
                                size = it
                                transition.updateFullPlayerBounds(
                                    Rect(0f, 0f, it.width.toFloat(), it.height.toFloat()),
                                )
                                transition.updateMiniPlayerBounds(
                                    Rect(
                                        it.width * 0.05f,
                                        it.height * 0.9f,
                                        it.width * 0.95f,
                                        it.height.toFloat(),
                                    ),
                                )
                            },
                        ) {
                            val progress = transition.progress
                            val bounds = sharedContainerRect(
                                transition.miniPlayerBounds,
                                transition.fullPlayerBounds,
                                progress,
                            )
                            val density = LocalDensity.current
                            transition.gestureDensity = density.density
                            val gesture = rememberPlayerSheetVerticalDragModifier(
                                enabled = true,
                                hasItem = true,
                                onDragStart = transition::beginFullPlayerDrag,
                                onDrag = transition::dragBy,
                                onDragEnd = {
                                    velocity = it
                                    transition.endDrag(it)
                                },
                                onDragCancel = {
                                    cancelled = true
                                    transition.cancelDrag()
                                },
                            )
                            Box(
                                Modifier
                                    .offset {
                                        IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt())
                                    }
                                    .size(
                                        with(density) { bounds.width.toDp() },
                                        with(density) { bounds.height.toDp() },
                                    )
                                    .playerSheetHostLayer(
                                        hostBounds = bounds,
                                        inputBounds = bounds,
                                        miniPlayerBounds = null,
                                    )
                                    .then(gesture)
                                    .drawWithContent {
                                        drawContent()
                                        val (previousProgress, latch) = expectedDraw.get()
                                        if (progress < previousProgress &&
                                            progress == transition.progress
                                        ) {
                                            latch.countDown()
                                        }
                                    },
                            )
                        }
                    }
                }
                activity.setContentView(view)
            }
            assertTrue("Player was not drawn", firstDraw.await(5, TimeUnit.SECONDS))
            val downTime = SystemClock.uptimeMillis()
            var eventTime = downTime

            fun send(action: Int, yFraction: Float, xFraction: Float = 0.5f) {
                val event = MotionEvent.obtain(
                    downTime, eventTime, action,
                    size.width * xFraction, size.height * yFraction, 0,
                )
                event.source = InputDevice.SOURCE_TOUCHSCREEN
                try {
                    view.dispatchTouchEvent(event)
                } finally {
                    event.recycle()
                }
                eventTime += 16L
            }

            if (horizontalFirst) {
                scenario.onActivity {
                    send(MotionEvent.ACTION_DOWN, 0.3f, 0.1f)
                    send(MotionEvent.ACTION_MOVE, 0.31f, 0.5f)
                    send(MotionEvent.ACTION_MOVE, 0.32f, 0.8f)
                    send(MotionEvent.ACTION_MOVE, 0.7f, 0.8f)
                    send(MotionEvent.ACTION_UP, 0.7f, 0.8f)
                    assertFalse(transition.isDragging)
                    assertTrue(transition.targetOpen)
                    assertEquals(1f, transition.progress, 0f)
                    assertTrue("Horizontal input must not release a sheet drag", velocity.isNaN())
                    assertFalse(cancelled)
                }
                return
            }

            scenario.onActivity { send(MotionEvent.ACTION_DOWN, 0.15f) }
            for (fraction in listOf(0.25f, 0.35f)) {
                val frame = CountDownLatch(1)
                scenario.onActivity {
                    expectedDraw.set(transition.progress to frame)
                    send(MotionEvent.ACTION_MOVE, fraction)
                }
                assertTrue("Dragged host was not drawn", frame.await(5, TimeUnit.SECONDS))
                instrumentation.waitForIdleSync()
            }
            scenario.onActivity {
                // The last two frames move the host origin; this sample must use
                // its original event position, even when its local Y moves up.
                send(MotionEvent.ACTION_MOVE, lastMoveFraction)
                send(MotionEvent.ACTION_UP, lastMoveFraction)
                assertFalse("Drag was cancelled instead of released", cancelled)
                assertTrue("Release velocity was not reported", velocity.isFinite())
                assertEquals(expectedOpen, velocity < 0f)
                assertEquals(expectedOpen, transition.targetOpen)
            }
        }
    }
}
