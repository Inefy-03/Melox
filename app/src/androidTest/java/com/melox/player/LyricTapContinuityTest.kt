package com.melox.player

import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.melox.player.model.AppSettings
import com.melox.player.model.LyricAnimationMode
import com.melox.player.model.LyricLine
import com.melox.player.model.LyricsDocument
import com.melox.player.model.LyricsFormat
import com.melox.player.model.LyricsSource
import com.melox.player.ui.screen.playback.LyricsView
import com.melox.player.ui.theme.MeloxTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LyricTapContinuityTest {
    private data class Playback(
        val position: Long = 0,
        val revision: Int = 1,
        val iteration: Long = 0,
        val playing: Boolean = false,
    )
    private var sampledFrames = 15
    private val cachedRows = mutableMapOf<Int, AccessibilityNodeInfo>()
    private var centerY = 0
    private var focusedIndex = 0
    private val clock = BroadcastFrameClock()
    private var frameNanos = 0L
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var texts = List(12) { index ->
        "ROW $index " + if (index % 3 == 0) "wrapped lyric words ".repeat(8) else "single lyric line"
    }

    @Test
    fun plainLyricTapsKeepIntermediatePositionsContinuous() = checkTapContinuity(LyricAnimationMode.NEVER)

    @Test
    fun wordLyricTapsKeepIntermediatePositionsContinuous() = checkTapContinuity(LyricAnimationMode.ALWAYS)

    @Test
    fun shortWordLinePlaybackAndAcknowledgedTapsKeepPlacementContinuous() =
        checkTapContinuity(LyricAnimationMode.ALWAYS, shortLine = true)

    @Test
    fun acknowledgedSeekRemainsContinuousThroughCompleteAutomaticHandoffs() {
        sampledFrames = 150
        checkTapContinuity(LyricAnimationMode.ALWAYS, shortLine = true)
    }

    private fun checkTapContinuity(mode: LyricAnimationMode, shortLine: Boolean = false) {
        if (shortLine) {
            texts = listOf("总不舍得分开 (yay yay yay yay)", "不知道", "该用哪种方式回应你的温柔",
                "该怎么", "将我所有模糊的心动说出口") + texts.drop(5)
        }
        instrumentation.uiAutomation.serviceInfo = instrumentation.uiAutomation.serviceInfo.apply {
            flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        val playback = mutableStateOf(Playback())
        val starts = List(12) { index ->
            if (shortLine && index >= 2) 10_688L + (index - 2) * (if (sampledFrames > 15) 100_000L else 10_000L) else index * 10_000L
        }
        val document = LyricsDocument(texts.mapIndexed { index, text ->
            val end = if (shortLine && index == 1) 10_464L else starts.getOrElse(index + 1) { starts[index] + 10_000L }
            LyricLine("", starts[index], end, text, emptyList(), null)
        }, LyricsFormat.LRC, LyricsSource.SIDECAR)
        lateinit var recomposer: Recomposer
        lateinit var recomposition: Job
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                recomposer = Recomposer(Dispatchers.Main + clock)
                recomposition = CoroutineScope(Dispatchers.Main + clock).launch { recomposer.runRecomposeAndApplyChanges() }
                activity.setContentView(ComposeView(activity).apply {
                    setParentCompositionContext(recomposer)
                    setContent {
                        MeloxTheme(AppSettings()) {
                            Box(Modifier.fillMaxSize().background(Color.Black)) {
                                val state = playback.value
                                LyricsView(document = document, positionMs = state.position,
                                    positionUpdateElapsedRealtimeMs = SystemClock.elapsedRealtime(),
                                    playbackIteration = state.iteration, playbackSpeed = 1f,
                                    previewPositionMs = null, isPlaying = state.playing,
                                    onSeek = { position -> playback.value = state.copy(position = position, revision = state.revision + 1) },
                                    contentWidth = 240.dp, lyricFontScale = 1f, lyricFontWeight = 400,
                                    lyricAnimationMode = mode, lyricBlurEnabled = false,
                                    centerLyrics = false, showLyricsTranslation = false, showBottomFade = false,
                                    resumeFollowRequestKey = 0, seekRequestKey = state.revision,
                                    seekPositionMs = state.position, emphasisColor = Color.White,
                                    modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                })
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val content = activity.findViewById<android.view.View>(android.R.id.content)
                val location = IntArray(2)
                content.getLocationOnScreen(location)
                centerY = location[1] + content.height / 2
            }
            repeat(90) { frame() }
            assertCentered(0)
            val gestureBounds = Rect().also { row(0).getBoundsInScreen(it) }
            val downTime = SystemClock.uptimeMillis()
            fun pointer(action: Int, y: Float) {
                val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action,
                    gestureBounds.centerX().toFloat(), y, 0)
                try {
                    instrumentation.sendPointerSync(event)
                } finally {
                    event.recycle()
                }
                frame()
            }
            pointer(MotionEvent.ACTION_DOWN, centerY + 100f)
            repeat(if (sampledFrames > 15) 1 else 4) { step -> pointer(MotionEvent.ACTION_MOVE, centerY + 100f - (step + 1) * 50f) }
            pointer(MotionEvent.ACTION_UP, centerY - 100f)
            tapAndSample(1)
            tapAndSample(0)
            // Sample the actual accessibility bounds throughout motion, rather than only its endpoint.
            repeat(if (sampledFrames > 15) 1 else 4) {
                tapAndSample(1)
                tapAndSample(0)
            }
            scenario.onActivity { playback.value = Playback(iteration = 1, revision = 0, playing = true) }
            repeat(90) { frame() }
            focusedIndex = 0
            // Keep automatic first-to-second-line playback continuous after the repeat reset.
            transitionAndSample(1) {
                scenario.onActivity { playback.value = playback.value.copy(position = 10_000) }
            }
            if (shortLine) {
                repeat(3) { transitionAndSample(2) {} }
            }
            repeat(90) { frame() }
            assertCentered(if (shortLine) 2 else 1)
            tapAndSample(1)
            if (shortLine) {
                // Match the video: a short line resumes playback after seek acknowledgement.
                transitionAndSample(1) {
                    scenario.onActivity { playback.value = playback.value.copy(revision = 0) }
                }
                repeat(3) { transitionAndSample(2) {} }
            }
            tapAndSample(0)
            if (sampledFrames > 15) {
                // The UI stopped receiving frames while playback paused or sought in the background.
                scenario.onActivity {
                    playback.value = playback.value.copy(position = 12_000, revision = 0)
                }
                frameNanos += 120_000_000_000L
                repeat(150) { frame() }
                assertCentered(2)
                focusedIndex = 2
                transitionAndSample(3) {
                    scenario.onActivity { playback.value = playback.value.copy(position = starts[3]) }
                }
                assertCentered(3)
            }
            scenario.onActivity { recomposer.cancel(); recomposition.cancel() }
        }
    }

    private fun row(index: Int): AccessibilityNodeInfo {
        cachedRows[index]?.let { cached ->
            if (cached.refresh() && cached.text?.toString() == texts[index]) return cached
        }
        val root = instrumentation.uiAutomation.rootInActiveWindow
            ?: instrumentation.uiAutomation.windows.firstNotNullOfOrNull { it.root }
            ?: error("No accessibility window")
        val clickableRows = mutableListOf<AccessibilityNodeInfo>()
        fun find(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            node.refresh()
            if (node.text?.toString() == texts[index]) return node
            if (node.isClickable) clickableRows += node
            for (child in 0 until node.childCount) {
                node.getChild(child)?.let { find(it)?.let { found -> return found } }
            }
            return null
        }
        return (find(root) ?: clickableRows.getOrNull(index) ?: error("Missing lyric $index"))
            .also { cachedRows[index] = it }
    }

    private fun top(index: Int): Int = Rect().also { row(index).getBoundsInScreen(it) }.top

    private fun assertCentered(index: Int) {
        val bounds = Rect().also { row(index).getBoundsInScreen(it) }
        assertTrue("Lyric $index did not reach the center: ${bounds.centerY()} != $centerY",
            abs(bounds.centerY() - centerY) <= 3)
    }

    private fun frame() {
        instrumentation.runOnMainSync {
            Snapshot.sendApplyNotifications()
            frameNanos += 16_666_667L
            clock.sendFrame(frameNanos)
        }
        instrumentation.waitForIdleSync()
    }

    private fun tapAndSample(index: Int) = transitionAndSample(index) {
        var target = row(index)
        while (!target.isClickable && target.parent != null) target = target.parent
        assertTrue("Lyric click was rejected", target.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun transitionAndSample(index: Int, change: () -> Unit) {
        val velocitySample = top(index)
        frame()
        val initialBounds = Rect().also { row(index).getBoundsInScreen(it) }
        val initial = initialBounds.top
        val travel = abs(initialBounds.centerY() - centerY)
        val oldBounds = Rect().also { row(focusedIndex).getBoundsInScreen(it) }
        val oldTravel = abs(oldBounds.centerY() - centerY)
        val inheritedSpeed = abs(initial - velocitySample) * 60.0
        change()
        focusedIndex = index
        var previous = initial
        repeat(sampledFrames) {
            frame()
            val current = top(index)
            // Include the interrupted trajectory as well as the newly requested travel.
            val maxStep = maxOf(12.0, (inheritedSpeed + (travel + oldTravel) * 10.0) / 60.0 + 12.0)
            assertTrue("Lyric $index jumped: $previous -> $current in one frame",
                abs(current - previous) <= maxStep)
            previous = current
        }
    }
}
