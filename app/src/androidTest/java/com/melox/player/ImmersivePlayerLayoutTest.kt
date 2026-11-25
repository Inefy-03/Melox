package com.melox.player

import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.melox.player.model.PlaybackQueueItem
import com.melox.player.model.LyricLine
import com.melox.player.model.LyricsDocument
import com.melox.player.model.LyricsFormat
import com.melox.player.model.LyricsSource
import com.melox.player.model.LyricAnimationMode
import com.melox.player.ui.screen.playback.LyricsView
import com.melox.player.ui.screen.playback.playerChromeAlpha
import com.melox.player.ui.screen.playback.portraitLyricViewport
import com.melox.player.model.PlaybackUiState
import com.melox.player.ui.screen.playback.PLAYER_PROGRESS_IDLE_HEIGHT
import com.melox.player.ui.screen.playback.PLAYER_PROGRESS_TOUCH_HEIGHT
import com.melox.player.ui.screen.playback.PLAYER_PROGRESS_TIME_SPACING
import com.melox.player.ui.screen.playback.PlayerLyricActionsRow
import com.melox.player.ui.screen.playback.CompactLandscapePlayerLayout
import com.melox.player.ui.screen.playback.PlayerDetails
import com.melox.player.ui.screen.playback.PlayerVerticalSpacing
import com.melox.player.ui.screen.playback.PortraitPlayerLayout
import com.melox.player.ui.screen.playback.playerChromeVisibility
import com.melox.player.ui.screen.playback.rememberPlayerHeaderGestureModifier
import com.melox.player.ui.component.playback.rememberPlayerSheetVerticalDragModifier
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import top.yukonga.miuix.kmp.theme.MiuixTheme

@RunWith(AndroidJUnit4::class)
class ImmersivePlayerLayoutTest {
    @Test
    fun hiddenLyricActionsLetBothButtonRegionsScrollTheLyrics() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val ready = CountDownLatch(1)
        val hidden = mutableStateOf(false)
        val row = AtomicReference(Rect.Zero)
        val scrollPosition = AtomicInteger()
        val actions = AtomicInteger()
        lateinit var view: ComposeView
        var density = 1f
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                density = activity.resources.displayMetrics.density
                view = ComposeView(activity).apply {
                    setContent {
                        MiuixTheme {
                            val scroll = rememberScrollState()
                            val offset = scroll.value
                            SideEffect { scrollPosition.set(offset) }
                            Box(Modifier.width(320.dp).height(480.dp)) {
                                Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                                    Box(Modifier.fillMaxWidth().height(2000.dp))
                                }
                                PlayerLyricActionsRow(
                                    showLyricsTranslation = true,
                                    emphasisControlColor = Color.White,
                                    onOpenLyricsSettings = { actions.incrementAndGet() },
                                    onToggleLyricsTranslation = { actions.incrementAndGet() },
                                    onOpenTranslationSettings = { actions.incrementAndGet() },
                                    enabled = !hidden.value,
                                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                                        .playerChromeVisibility(!hidden.value)
                                        .onGloballyPositioned { row.set(it.boundsInRoot()); ready.countDown() },
                                )
                            }
                        }
                    }
                }
                activity.setContentView(view)
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            instrumentation.waitForIdleSync()
            val originalRow = row.get()
            val points = listOf(R.string.player_settings_open, R.string.lyrics_translation).map { resource ->
                val node = requireNotNull(findNode(instrumentation.uiAutomation.rootInActiveWindow) {
                    it.contentDescription?.toString() == view.context.getString(resource)
                })
                val bounds = android.graphics.Rect().also(node::getBoundsInScreen)
                val origin = IntArray(2)
                instrumentation.runOnMainSync { view.getLocationOnScreen(origin) }
                Offset(bounds.exactCenterX() - origin[0], bounds.exactCenterY() - origin[1])
            }
            instrumentation.runOnMainSync { hidden.value = true }
            SystemClock.sleep(350)
            instrumentation.waitForIdleSync()
            assertEquals("Fading must preserve the action row's measured size", originalRow, row.get())
            for (point in points) {
                val before = scrollPosition.get()
                val downTime = SystemClock.uptimeMillis()
                fun event(action: Int, fraction: Float) = instrumentation.runOnMainSync {
                    MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action,
                        point.x, point.y - 64f * density * fraction, 0).also {
                        view.dispatchTouchEvent(it)
                        it.recycle()
                    }
                }
                event(MotionEvent.ACTION_DOWN, 0f)
                for (step in 1..6) {
                    SystemClock.sleep(16)
                    event(MotionEvent.ACTION_MOVE, step / 6f)
                }
                event(MotionEvent.ACTION_UP, 1f)
                instrumentation.waitForIdleSync()
                assertTrue("A faded button must not block the scrollable lyrics", scrollPosition.get() > before)
                assertEquals("Hidden buttons must not perform actions", 0, actions.get())
            }
            instrumentation.runOnMainSync { hidden.value = false }
            SystemClock.sleep(350)
            instrumentation.waitForIdleSync()
            assertEquals(originalRow, row.get())
            for (resource in listOf(R.string.player_settings_open, R.string.lyrics_translation)) {
                assertTrue(hasDescription(instrumentation.uiAutomation.rootInActiveWindow,
                    view.context.getString(resource)))
            }
        }
    }

    @Test
    fun normalPortraitChromeDragsDismissWhileTheLyricViewportStillScrolls() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val ready = CountDownLatch(1)
        val page = AtomicReference(Rect.Zero)
        val starts = AtomicInteger()
        val ends = AtomicInteger()
        val scrollPosition = AtomicInteger()
        lateinit var view: ComposeView
        var density = 1f
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                density = activity.resources.displayMetrics.density
                view = ComposeView(activity).apply {
                    setContent {
                        MiuixTheme {
                            val dismiss = rememberPlayerSheetVerticalDragModifier(
                                enabled = true, hasItem = true,
                                onDragStart = { starts.incrementAndGet() }, onDrag = {},
                                onDragEnd = { ends.incrementAndGet() }, onDragCancel = {},
                            )
                            val scroll = rememberScrollState()
                            val scrollOffset = scroll.value
                            val viewportAlpha = playerChromeAlpha(false)
                            SideEffect { scrollPosition.set(scrollOffset) }
                            Box(Modifier.width(320.dp).height(600.dp).then(dismiss)
                                .onGloballyPositioned { page.set(it.boundsInRoot()); ready.countDown() }) {
                                PortraitPlayerLayout(
                                    spacing = PlayerVerticalSpacing(artworkToProgress = 0.dp, panelBottom = 32.dp),
                                    artworkTopPadding = 80.dp,
                                    overlayDetails = true,
                                    detailsModifier = dismiss,
                                    modifier = Modifier.fillMaxSize(),
                                    artworkContent = { artworkHeight ->
                                        Box(Modifier.fillMaxSize().portraitLyricViewport(
                                            topInset = 80.dp,
                                            bottomInset = 600.dp - 80.dp - artworkHeight,
                                            showBottomFade = true, immersiveMode = false,
                                            immersiveAlpha = viewportAlpha,
                                        )) {
                                            Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                                                Box(Modifier.fillMaxWidth().height(2000.dp))
                                            }
                                        }
                                    },
                                    detailsContent = { Box(Modifier.fillMaxWidth().height(160.dp)) },
                                )
                                Box(Modifier.fillMaxWidth().height(80.dp).then(dismiss))
                            }
                        }
                    }
                }
                activity.setContentView(view)
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            instrumentation.waitForIdleSync()
            fun swipe(point: Offset, deltaY: Float) {
                val downTime = SystemClock.uptimeMillis()
                fun event(action: Int, fraction: Float) = instrumentation.runOnMainSync {
                    MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action,
                        point.x, point.y + deltaY * fraction, 0).also {
                        view.dispatchTouchEvent(it)
                        it.recycle()
                    }
                }
                event(MotionEvent.ACTION_DOWN, 0f)
                for (step in 1..6) {
                    SystemClock.sleep(16)
                    event(MotionEvent.ACTION_MOVE, step / 6f)
                }
                event(MotionEvent.ACTION_UP, 1f)
                instrumentation.waitForIdleSync()
            }
            for (y in listOf(8f, 40f, 440f, 580f)) {
                val before = starts.get()
                swipe(Offset(page.get().center.x, page.get().top + y * density), 64f * density)
                assertEquals("Header, panel and footer must retain sheet drag ownership", before + 1, starts.get())
                assertEquals(starts.get(), ends.get())
                assertEquals("Chrome drags must not scroll the masked lyrics", 0, scrollPosition.get())
            }
            val before = starts.get()
            swipe(Offset(page.get().center.x, page.get().top + 280f * density), -64f * density)
            assertEquals("Lyric scrolling must not start a sheet drag", before, starts.get())
            assertTrue("The lyric viewport must still scroll", scrollPosition.get() > 0)
        }
    }

    @Test
    fun portraitImmersionExtendsViewportWithoutMovingContent() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val ready = CountDownLatch(1)
        val immersive = mutableStateOf(false)
        val page = AtomicReference(Rect.Zero)
        val header = AtomicReference(Rect.Zero)
        val lyrics = AtomicReference(Rect.Zero)
        val artwork = AtomicReference(Rect.Zero)
        val details = AtomicReference(Rect.Zero)
        val fadeSamples = java.util.Collections.synchronizedList(mutableListOf<Float>())
        val document = LyricsDocument(
            List(12) { index -> LyricLine("", index * 10_000L, (index + 1) * 10_000L,
                "Stable immersion lyric $index", emptyList(), null) },
            LyricsFormat.LRC, LyricsSource.SIDECAR,
        )
        val topClicks = AtomicInteger()
        val headerHeight = 88.dp
        lateinit var view: ComposeView
        val spacing = PlayerVerticalSpacing(artworkToProgress = 0.dp, panelBottom = 32.dp)
        val overlap = (PLAYER_PROGRESS_TOUCH_HEIGHT - PLAYER_PROGRESS_IDLE_HEIGHT) / 2f
        var density = 1f
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                density = activity.resources.displayMetrics.density
                view = ComposeView(activity).apply {
                    setContent {
                        MiuixTheme {
                            Box(Modifier.width(320.dp).height(600.dp).onGloballyPositioned {
                                page.set(it.boundsInRoot())
                            }) {
                                Box(Modifier.onGloballyPositioned { header.set(it.boundsInRoot()) }
                                    .playerChromeVisibility(!immersive.value)
                                    .padding(top = 24.dp).height(64.dp).fillMaxWidth())
                                BoxWithConstraints(Modifier.fillMaxSize()) {
                                    val pageHeight = maxHeight
                                    PortraitPlayerLayout(
                                        spacing = spacing,
                                        artworkBottomOverlap = overlap,
                                        artworkTopPadding = headerHeight,
                                        overlayDetails = true,
                                        modifier = Modifier.fillMaxSize(),
                                        artworkContent = { artworkHeight ->
                                            val focusOffset = headerHeight + (artworkHeight - pageHeight) / 2f
                                            val alpha = playerChromeAlpha(immersive.value)
                                            Box(Modifier.fillMaxSize()
                                                .onGloballyPositioned { lyrics.set(it.boundsInRoot()) }
                                                .portraitLyricViewport(
                                                    topInset = headerHeight,
                                                    bottomInset = (pageHeight - headerHeight - artworkHeight).coerceAtLeast(0.dp),
                                                    showBottomFade = true,
                                                    immersiveMode = immersive.value,
                                                    immersiveAlpha = alpha,
                                                )
                                                .drawWithContent {
                                                    fadeSamples.add(alpha.value)
                                                    drawContent()
                                                }
                                                .clickable { topClicks.incrementAndGet() }) {
                                                LyricsView(
                                                    document = document, positionMs = 35_000L,
                                                    positionUpdateElapsedRealtimeMs = 0L,
                                                    playbackIteration = 0L, playbackSpeed = 1f,
                                                    previewPositionMs = null, isPlaying = false, onSeek = {},
                                                    contentWidth = 280.dp, lyricFontScale = 1f, lyricFontWeight = 400,
                                                    lyricAnimationMode = LyricAnimationMode.NEVER, lyricBlurEnabled = false,
                                                    centerLyrics = false, centerOffsetY = focusOffset,
                                                    showLyricsTranslation = false, showBottomFade = true,
                                                    resumeFollowRequestKey = 0, seekRequestKey = 0, seekPositionMs = 0L,
                                                    emphasisColor = Color.White, applyEdgeFade = false,
                                                    modifier = Modifier.fillMaxSize(),
                                                )
                                                Box(Modifier
                                                    .padding(top = headerHeight)
                                                    .fillMaxWidth().height(artworkHeight)
                                                    .onGloballyPositioned { artwork.set(it.boundsInRoot()) })
                                            }
                                        },
                                        detailsContent = {
                                            PlayerDetails(
                                                contentWidth = 320.dp,
                                                playback = PlaybackUiState(),
                                                emphasisControlColor = Color.White,
                                                immersiveMode = immersive.value,
                                                onToggleImmersiveMode = {}, onTogglePlayPause = {},
                                                onPrevious = { true }, onNext = { true }, onSeek = {},
                                                onPreviewPositionChange = {}, onCyclePlaybackMode = {},
                                                onOpenAudioSettings = {}, onOpenSleepTimer = {},
                                                onOpenQueue = {}, onOpenTrackActions = {},
                                                spacing = spacing,
                                                modifier = Modifier.fillMaxWidth().onGloballyPositioned {
                                                    details.set(it.boundsInRoot())
                                                    ready.countDown()
                                                },
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                activity.setContentView(view)
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS))
            instrumentation.waitForIdleSync()
            val originalHeader = header.get()
            fun focusedRowBounds(): android.graphics.Rect {
                val node = requireNotNull(findNode(instrumentation.uiAutomation.rootInActiveWindow) {
                    it.text?.toString() == "Stable immersion lyric 3"
                })
                return android.graphics.Rect().also(node::getBoundsInScreen)
            }
            SystemClock.sleep(900)
            instrumentation.waitForIdleSync()
            val originalFocus = focusedRowBounds()
            val originalLyrics = lyrics.get()
            val originalArtwork = artwork.get()
            val originalDetails = details.get()
            val originalFadeSampleCount = fadeSamples.size
            assertEquals(page.get(), originalLyrics)
            assertEquals(88f * density, header.get().height, 1f)
            instrumentation.runOnMainSync { immersive.value = true }
            repeat(16) {
                SystemClock.sleep(20)
                instrumentation.waitForIdleSync()
                assertEquals(originalFocus, focusedRowBounds())
                assertEquals(originalLyrics, lyrics.get())
            }
            assertTrue(fadeSamples.drop(originalFadeSampleCount).any { it > 0f && it < 1f })
            assertEquals(page.get().top, lyrics.get().top, 1f)
            assertEquals(originalHeader, header.get())
            assertEquals(originalFocus, focusedRowBounds())
            assertEquals(page.get().bottom, lyrics.get().bottom, 1f)
            assertEquals(originalDetails, details.get())
            assertEquals(originalArtwork.bottom, artwork.get().bottom, 1f)
            instrumentation.runOnMainSync {
                val time = SystemClock.uptimeMillis()
                val point = Offset(page.get().center.x, page.get().top + 8f * density)
                for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                    MotionEvent.obtain(time, time + action, action, point.x, point.y, 0).also {
                        view.dispatchTouchEvent(it)
                        it.recycle()
                    }
                }
            }
            instrumentation.waitForIdleSync()
            assertEquals(1, topClicks.get())
            val reverseFadeSampleCount = fadeSamples.size
            instrumentation.runOnMainSync { immersive.value = false }
            repeat(16) {
                SystemClock.sleep(20)
                instrumentation.waitForIdleSync()
                assertEquals(originalFocus, focusedRowBounds())
            }
            assertTrue(fadeSamples.drop(reverseFadeSampleCount).any { it > 0f && it < 1f })
            assertEquals(originalLyrics, lyrics.get())
            assertEquals(originalArtwork, artwork.get())
            assertEquals(originalDetails, details.get())
        }
    }

    @Test
    fun compactLandscapeHeaderToggleExpandsLyricsOnOneContinuousPage() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val ready = CountDownLatch(1)
        val cover = AtomicReference(Rect.Zero)
        val header = AtomicReference(Rect.Zero)
        val toggle = AtomicReference(Rect.Zero)
        val pane = AtomicReference(Rect.Zero)
        val controls = AtomicReference(Rect.Zero)
        val lyrics = AtomicReference(Rect.Zero)
        val selectedPage = AtomicInteger()
        val immersive = mutableStateOf(false)
        val controlClicks = AtomicInteger()
        val chromeInset = 24.dp
        var density = 1f
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var view: ComposeView
            scenario.onActivity { activity ->
                density = activity.resources.displayMetrics.density
                view = ComposeView(activity).apply {
                    setContent {
                        MiuixTheme {
                            val pager = rememberPagerState(pageCount = { 2 })
                            SideEffect { selectedPage.set(pager.currentPage) }
                            CompactLandscapePlayerLayout(
                                pagerState = pager,
                                lyricsPagingEnabled = true,
                                immersiveMode = immersive.value,
                                chromeVerticalInset = chromeInset,
                                artworkContent = {
                                    Box(Modifier.size(96.dp).onGloballyPositioned {
                                        cover.set(it.boundsInRoot())
                                        ready.countDown()
                                    })
                                },
                                headerContent = { width ->
                                    Box(Modifier.width(width).height(52.dp).onGloballyPositioned {
                                        header.set(it.boundsInRoot())
                                    })
                                },
                                lyricsToggleContent = { _, enabled, onToggle ->
                                    Box(Modifier.size(40.dp).clickable(enabled = enabled, onClick = onToggle)
                                        .semantics { contentDescription = "Compact lyrics toggle" }
                                        .onGloballyPositioned { toggle.set(it.boundsInRoot()) })
                                },
                                controlsContent = { width, _ ->
                                    Box(Modifier.width(width).height(128.dp)
                                        .onGloballyPositioned { controls.set(Rect(it.positionInRoot(), Size(it.size.width.toFloat(), it.size.height.toFloat()))) }
                                        .clickable { controlClicks.incrementAndGet() }
                                        .semantics { contentDescription = "Compact controls" })
                                },
                                lyricsContent = { width, _, height, _ ->
                                    Box(Modifier.width(width).height(height)
                                        .onGloballyPositioned { lyrics.set(Rect(it.positionInRoot(), Size(it.size.width.toFloat(), it.size.height.toFloat()))) }
                                        .semantics { contentDescription = "Compact lyrics" })
                                },
                                modifier = Modifier.fillMaxWidth().height(300.dp)
                                    .onGloballyPositioned { pane.set(it.boundsInRoot()) },
                            )
                        }
                    }
                }
                activity.setContentView(view)
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            instrumentation.waitForIdleSync()
            val originalHeader = header.get()
            val originalControls = controls.get()
            val originalPageGap = controls.get().top - lyrics.get().bottom
            fun tap(point: Offset) {
                instrumentation.runOnMainSync {
                    val time = SystemClock.uptimeMillis()
                    for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                        MotionEvent.obtain(time, time + action, action, point.x, point.y, 0).also {
                            view.dispatchTouchEvent(it)
                            it.recycle()
                        }
                    }
                }
                SystemClock.sleep(400)
                instrumentation.waitForIdleSync()
            }
            tap(controls.get().center)
            assertEquals(1, controlClicks.get())
            tap(cover.get().center)
            assertEquals(0, selectedPage.get())
            tap(toggle.get().center)
            assertEquals(1, selectedPage.get())
            assertEquals(originalHeader.top - 12f * density, header.get().top, 1f)
            assertEquals(header.get().bottom, lyrics.get().top, 1f)
            assertEquals(pane.get().bottom - (chromeInset.value - 12f) * density, lyrics.get().bottom, 1f)
            assertEquals(originalPageGap, controls.get().top - lyrics.get().bottom, 1f)
            assertEquals(controls.get().width, lyrics.get().width, 1f)
            assertFalse(hasDescription(instrumentation.uiAutomation.rootInActiveWindow, "Compact controls"))
            assertTrue(hasDescription(instrumentation.uiAutomation.rootInActiveWindow, "Compact lyrics"))
            tap(toggle.get().center)
            assertEquals(0, selectedPage.get())
            assertEquals(originalPageGap, controls.get().top - lyrics.get().bottom, 1f)
            assertEquals(originalHeader, header.get())
            assertFalse(hasDescription(instrumentation.uiAutomation.rootInActiveWindow, "Compact lyrics"))
            tap(controls.get().center)
            assertEquals(2, controlClicks.get())
            instrumentation.runOnMainSync { immersive.value = true }
            SystemClock.sleep(250)
            instrumentation.waitForIdleSync()
            assertFalse(hasDescription(instrumentation.uiAutomation.rootInActiveWindow, "Compact lyrics toggle"))
            assertEquals(originalControls, controls.get())
            assertEquals(pane.get().top, lyrics.get().bottom, 1f)
            tap(toggle.get().center)
            assertEquals(0, selectedPage.get())
            tap(cover.get().center)
            assertEquals(1, selectedPage.get())
            assertEquals(pane.get().top, lyrics.get().top, 1f)
            assertEquals(pane.get().bottom, lyrics.get().bottom, 1f)
            assertTrue(controls.get().top > pane.get().bottom)
            tap(cover.get().center)
            assertEquals(0, selectedPage.get())
            assertEquals(originalControls, controls.get())
            instrumentation.runOnMainSync { immersive.value = false }
            SystemClock.sleep(250)
            instrumentation.waitForIdleSync()
            assertTrue(hasDescription(instrumentation.uiAutomation.rootInActiveWindow, "Compact lyrics toggle"))
            tap(cover.get().center)
            assertEquals(0, selectedPage.get())
            tap(toggle.get().center)
            assertEquals(1, selectedPage.get())
        }
    }

    @Test
    fun fadingChromeKeepsBoundsAndDisablesHiddenActionsWhileTransportWorks() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val immersive = mutableStateOf(false)
        val ready = CountDownLatch(1)
        val header = AtomicReference(Rect.Zero)
        val artwork = AtomicReference(Rect.Zero)
        val details = AtomicReference(Rect.Zero)
        val seeks = AtomicInteger()
        val queues = AtomicInteger()
        val headerActions = AtomicInteger()
        val playbackClicks = AtomicInteger()
        val translationToggles = AtomicInteger()
        val translationSheets = AtomicInteger()
        val lyricSettings = AtomicInteger()
        val translation = mutableStateOf(true)
        val lyricActionsVisible = mutableStateOf(true)
        val lyricViewport = AtomicReference(Rect.Zero)
        val spacing = PlayerVerticalSpacing(artworkToProgress = 0.dp)
        val item = PlaybackQueueItem(
            mediaId = "layout-test", trackId = null, contentUri = "content://layout-test",
            title = "Layout test", artist = null, album = null, durationMs = 120_000,
            dateModifiedEpochSeconds = 0, fileSizeBytes = 0,
        )
        lateinit var view: ComposeView
        var density = 1f
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                density = activity.resources.displayMetrics.density
                view = ComposeView(activity).apply {
                    setContent {
                        MiuixTheme {
                            Column(Modifier.fillMaxSize().width(320.dp)) {
                                Box(
                                    Modifier.width(320.dp).height(48.dp)
                                        .playerChromeVisibility(!immersive.value)
                                        .then(rememberPlayerHeaderGestureModifier(
                                            enabled = !immersive.value,
                                            playWhenReady = false,
                                            onTogglePlayPause = { headerActions.incrementAndGet() },
                                            onPrevious = { headerActions.incrementAndGet() },
                                            onNext = { headerActions.incrementAndGet() },
                                        ))
                                        .semantics { contentDescription = HEADER_DESCRIPTION }
                                        .onGloballyPositioned { header.set(it.boundsInRoot()) },
                                )
                                PortraitPlayerLayout(
                                    spacing = spacing,
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    artworkBottomOverlap =
                                        (PLAYER_PROGRESS_TOUCH_HEIGHT - PLAYER_PROGRESS_IDLE_HEIGHT) / 2f,
                                    artworkContent = {
                                        Box(Modifier.fillMaxSize().onGloballyPositioned {
                                            lyricViewport.set(it.boundsInRoot())
                                        }) {
                                            Box(Modifier.align(Alignment.Center).size(96.dp).onGloballyPositioned {
                                                artwork.set(it.boundsInRoot())
                                            })
                                            if (lyricActionsVisible.value) {
                                                PlayerLyricActionsRow(
                                                    showLyricsTranslation = translation.value,
                                                    emphasisControlColor = Color.White,
                                                    onToggleLyricsTranslation = {
                                                        translation.value = !translation.value
                                                        translationToggles.incrementAndGet()
                                                    },
                                                    onOpenTranslationSettings = { translationSheets.incrementAndGet() },
                                                    onOpenLyricsSettings = { lyricSettings.incrementAndGet() },
                                                    enabled = !immersive.value,
                                                    modifier = Modifier.align(Alignment.BottomCenter)
                                                        .width(320.dp).padding(bottom = PLAYER_PROGRESS_TIME_SPACING + 12.dp)
                                                        .playerChromeVisibility(!immersive.value),
                                                )
                                            }
                                        }
                                    },
                                    detailsContent = {
                                        PlayerDetails(
                                            contentWidth = 320.dp,
                                            playback = PlaybackUiState(
                                                queue = listOf(item), currentIndex = 0,
                                                durationMs = item.durationMs,
                                            ),
                                            emphasisControlColor = Color.White,
                                            immersiveMode = immersive.value,
                                            onToggleImmersiveMode = { immersive.value = !immersive.value },
                                            onTogglePlayPause = { playbackClicks.incrementAndGet() },
                                            onPrevious = { true }, onNext = { true },
                                            onSeek = { seeks.incrementAndGet() },
                                            onPreviewPositionChange = {},
                                            onCyclePlaybackMode = {},
                                            onOpenAudioSettings = {}, onOpenSleepTimer = {},
                                            onOpenQueue = { queues.incrementAndGet() },
                                            onOpenTrackActions = {},
                                            modifier = Modifier.width(320.dp).onGloballyPositioned {
                                                details.set(it.boundsInRoot())
                                                ready.countDown()
                                            },
                                            spacing = spacing,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
                activity.setContentView(view)
            }
            assertTrue("Player did not measure", ready.await(5, TimeUnit.SECONDS))
            instrumentation.waitForIdleSync()
            val original = listOf(header.get(), artwork.get(), details.get())
            fun nodeBounds(predicate: (AccessibilityNodeInfo) -> Boolean): Rect {
                val node = requireNotNull(findNode(instrumentation.uiAutomation.rootInActiveWindow, predicate))
                val bounds = android.graphics.Rect()
                node.getBoundsInScreen(bounds)
                val origin = IntArray(2)
                instrumentation.runOnMainSync { view.getLocationOnScreen(origin) }
                return Rect((bounds.left - origin[0]).toFloat(), (bounds.top - origin[1]).toFloat(),
                    (bounds.right - origin[0]).toFloat(), (bounds.bottom - origin[1]).toFloat())
            }
            fun actionBounds(resource: Int) = nodeBounds {
                it.contentDescription?.toString() == view.context.getString(resource)
            }
            val progressBounds = nodeBounds { it.rangeInfo != null }
            val progress = progressBounds.center
            val queue = actionBounds(R.string.open_queue).center
            val transport = actionBounds(R.string.play).center
            val translateBounds = actionBounds(R.string.lyrics_translation)
            val settingsBounds = actionBounds(R.string.player_settings_open)
            val moreBounds = actionBounds(R.string.track_actions)
            val timeBounds = nodeBounds { it.text?.toString() == "00:00" }
            val trackTop = progressBounds.center.y - PLAYER_PROGRESS_IDLE_HEIGHT.value * density / 2f
            val trackBottom = progressBounds.center.y + PLAYER_PROGRESS_IDLE_HEIGHT.value * density / 2f
            assertEquals(moreBounds.center.x, translateBounds.center.x, density)
            assertEquals(settingsBounds.bottom, translateBounds.bottom, density)
            assertEquals(lyricViewport.get().bottom, trackTop, density)
            assertEquals(40f * density, translateBounds.height, density)
            assertEquals(
                12f * density,
                (trackTop - translateBounds.bottom) - (timeBounds.top - trackBottom),
                density,
            )
            fun press(point: Offset, long: Boolean = false) {
                val down = SystemClock.uptimeMillis()
                fun event(action: Int) = instrumentation.runOnMainSync {
                    MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, point.x, point.y, 0).also {
                        view.dispatchTouchEvent(it)
                        it.recycle()
                    }
                }
                event(MotionEvent.ACTION_DOWN)
                SystemClock.sleep(if (long) ViewConfiguration.getLongPressTimeout().toLong() + 100 else 70)
                event(MotionEvent.ACTION_UP)
                instrumentation.waitForIdleSync()
            }
            fun settle() {
                SystemClock.sleep(250)
                instrumentation.waitForIdleSync()
                assertEquals(original, listOf(header.get(), artwork.get(), details.get()))
            }
            instrumentation.runOnMainSync { lyricActionsVisible.value = false }
            SystemClock.sleep(250)
            instrumentation.waitForIdleSync()
            assertEquals(original, listOf(header.get(), artwork.get(), details.get()))
            val artworkPageRoot = instrumentation.uiAutomation.rootInActiveWindow
            assertFalse(hasDescription(artworkPageRoot, view.context.getString(R.string.lyrics_translation)))
            assertFalse(hasDescription(artworkPageRoot, view.context.getString(R.string.player_settings_open)))
            instrumentation.runOnMainSync { lyricActionsVisible.value = true }
            settle()
            press(header.get().center, long = true)
            assertEquals(0, headerActions.get())
            press(translateBounds.center)
            assertFalse(translation.value)
            assertEquals(1, translationToggles.get())
            press(translateBounds.center, long = true)
            assertEquals(1, translationSheets.get())
            assertEquals(1, translationToggles.get())
            press(settingsBounds.center)
            assertEquals(1, lyricSettings.get())
            press(progress)
            press(queue)
            assertEquals(1, seeks.get())
            assertEquals(1, queues.get())
            val visibleRoot = instrumentation.uiAutomation.rootInActiveWindow
            assertTrue(hasDescription(visibleRoot, HEADER_DESCRIPTION))
            assertTrue(hasDescription(visibleRoot, view.context.getString(R.string.open_queue)))

            press(transport, long = true)
            assertTrue(immersive.value)
            assertEquals(0, playbackClicks.get())
            settle()
            val root = instrumentation.uiAutomation.rootInActiveWindow
            assertFalse(hasDescription(root, HEADER_DESCRIPTION))
            assertFalse(hasDescription(root, view.context.getString(R.string.open_queue)))
            assertFalse(hasDescription(root, view.context.getString(R.string.lyrics_translation)))
            press(translateBounds.center, long = true)
            assertEquals(1, translationSheets.get())
            assertEquals(1, translationToggles.get())
            press(progress)
            press(queue)
            press(header.get().center, long = true)
            assertEquals(1, seeks.get())
            assertEquals(1, queues.get())
            assertEquals(0, headerActions.get())

            press(transport)
            assertEquals(1, playbackClicks.get())
            press(transport, long = true)
            assertFalse(immersive.value)
            settle()
            press(queue)
            assertEquals(2, queues.get())
        }
    }

    private fun findNode(
        node: AccessibilityNodeInfo?,
        predicate: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo? {
        if (node == null) return null
        if (predicate(node)) return node
        return (0 until node.childCount).firstNotNullOfOrNull { findNode(node.getChild(it), predicate) }
    }

    private fun hasDescription(node: AccessibilityNodeInfo?, description: String): Boolean =
        findNode(node) { it.contentDescription?.toString() == description } != null

    private companion object {
        const val HEADER_DESCRIPTION = "Immersive layout header"
    }
}
