package com.melox.player

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.PixelCopy
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.melox.player.model.AppSettings
import com.melox.player.model.LyricAnimationMode
import com.melox.player.model.LyricLine
import com.melox.player.model.LyricWord
import com.melox.player.model.LyricsDocument
import com.melox.player.model.LyricsFormat
import com.melox.player.model.LyricsSource
import com.melox.player.ui.screen.playback.LyricsView
import com.melox.player.ui.screen.playback.prepareLyricTextRaster
import com.melox.player.ui.theme.MeloxTheme
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LyricViewRenderingTest {
    private data class Fixture(
        val position: Long = 2_000,
        val mode: LyricAnimationMode = LyricAnimationMode.ALWAYS,
        val scale: Float = 1f,
        val centered: Boolean = false,
        val translated: Boolean = true,
        val seekRevision: Int = 1,
    )

    @Test
    fun sentenceAndWordModesUseTheSamePrimaryTranslationGapAtEveryFontSize() {
        val fontScale = mutableStateOf(1f)
        val position = mutableStateOf(0L)
        val document = LyricsDocument(listOf(
            LyricLine("", 0L, 5_000L, null, listOf(LyricWord(0L, 5_000L, "田田", false)), "田"),
        ), LyricsFormat.TTML, LyricsSource.SIDECAR)
        lateinit var view: ComposeView
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                view = ComposeView(activity).apply {
                    setContent {
                        MeloxTheme(AppSettings()) {
                            Row(Modifier.fillMaxSize().background(Color.Black)) {
                                for (mode in listOf(LyricAnimationMode.NEVER, LyricAnimationMode.ALWAYS)) {
                                    Box(Modifier.weight(1f)) {
                                        LyricsView(
                                            document = document, positionMs = position.value,
                                            positionUpdateElapsedRealtimeMs = SystemClock.elapsedRealtime(),
                                            playbackIteration = 0L, playbackSpeed = 1f,
                                            previewPositionMs = null, isPlaying = false, onSeek = {},
                                            contentWidth = 160.dp, lyricFontScale = fontScale.value,
                                            lyricFontWeight = 400, lyricAnimationMode = mode,
                                            lyricBlurEnabled = false, centerLyrics = false,
                                            showLyricsTranslation = true, showBottomFade = false,
                                            resumeFollowRequestKey = 0, seekRequestKey = position.value.toInt(),
                                            seekPositionMs = position.value, emphasisColor = Color.White,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                activity.setContentView(view)
            }
            for (scale in listOf(2f / 3f, 1f, 2f)) {
                scenario.onActivity { fontScale.value = scale; position.value = 0L }
                val image = capture(scenario, view)
                val sentenceBands = inkBands(image, 0)
                val wordBands = inkBands(image, 1)
                assertEquals("Sentence ink bands at scale=$scale", 2, sentenceBands.size)
                assertEquals("Word ink bands at scale=$scale", 2, wordBands.size)
                val sentenceGap = sentenceBands[1].first - sentenceBands[0].last - 1
                val wordGap = wordBands[1].first - wordBands[0].last - 1
                assertTrue("Translation gap differs at scale=$scale: $sentenceGap vs $wordGap",
                    kotlin.math.abs(sentenceGap - wordGap) <= 1)
                saveImage(image, "lyric-gap-$scale.png")
                val translationBottom = wordBands[1].last
                image.recycle()
                scenario.onActivity { position.value = 1_700L }
                val movingImage = capture(scenario, view)
                try {
                    assertTrue("Word renderer did not advance its highlight at scale=$scale",
                        (movingImage.width / 2 until movingImage.width).sumOf { x ->
                            (0 until movingImage.height).count { y ->
                                (movingImage.getPixel(x, y) shr 16 and 255) > 150
                            }
                        } > 10)
                    assertEquals("Lift changed translation position at scale=$scale",
                        translationBottom, inkBands(movingImage, 1).last().last)
                    saveImage(movingImage, "lyric-bounds-$scale.png")
                } finally { movingImage.recycle() }
            }
        }
    }

    private fun inkBands(image: Bitmap, side: Int): List<IntRange> {
        val startX = side * image.width / 2 + 8
        val endX = (side + 1) * image.width / 2 - 8
        val result = mutableListOf<IntRange>()
        var startY = -1
        for (y in 0..image.height) {
            val ink = y < image.height && (startX until endX).count { x ->
                (image.getPixel(x, y) shr 16 and 255) > 30
            } >= 2
            if (ink && startY < 0) startY = y
            if (!ink && startY >= 0) {
                result += startY until y
                startY = -1
            }
        }
        return result
    }

    private fun saveImage(image: Bitmap, name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.cacheDir, name).outputStream().use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun hardwareImageFilteringPreservesFractionalRise() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val density = Density(1f)
        val layout = TextMeasurer(createFontFamilyResolver(context), density, LayoutDirection.Ltr)
            .measure("啊", TextStyle(fontSize = 40.sp))
        val raster = requireNotNull(prepareLyricTextRaster(layout, Color.White, density, LayoutDirection.Ltr, 1_000_000))
        val shift = mutableStateOf(0f)
        lateinit var view: ComposeView
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                view = ComposeView(activity).apply {
                    setContent {
                        Canvas(Modifier.fillMaxSize().background(Color.Black)) {
                            raster.draw(this, Offset(100f, 200f - shift.value), -100f, 100f)
                        }
                    }
                }
                activity.setContentView(view)
            }
            var previous = Double.NaN
            for (step in 0..8) {
                scenario.onActivity { shift.value = step / 8f }
                val image = capture(scenario, view, 150)
                try {
                    var weight = 0L
                    var weightedY = 0L
                    for (y in 0 until image.height) for (x in 0 until image.width) {
                        val coverage = image.getPixel(x, y) shr 16 and 255
                        weight += coverage
                        weightedY += coverage.toLong() * y
                    }
                    assertTrue(weight > 0)
                    val next = weightedY.toDouble() / weight
                    if (step > 0) assertTrue("Hardware glyph stalled at $step: $previous -> $next",
                        previous - next in 0.07..0.18)
                    previous = next
                } finally { image.recycle() }
            }
        }
    }

    @Test
    fun nativeWordRendererSurvivesPlaybackPrewarmingAndDocumentReplacement() {
        val fixture = mutableStateOf(Fixture(position = 0, scale = 2f))
        val generation = mutableStateOf(0)
        val texts = listOf("星の光を追いかけて", "歌词加载与播放", "office مرحبا 👨‍👩‍👧‍👦", "\u200D \n ")
        val documents = List(4) { revision ->
            LyricsDocument(List(60) { index ->
                val interval = if (revision == 3) 900L else 6_000L
                val start = index * interval
                LyricLine("", start, start + interval, null,
                    listOf(LyricWord(start, start + interval / 2, texts[(index + revision) % texts.size], true),
                        LyricWord(start + interval / 2, start + interval, "啊かな", false)),
                    "Translation $revision / $index")
            }, LyricsFormat.TTML, LyricsSource.SIDECAR)
        }
        lateinit var view: ComposeView
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                view = ComposeView(activity).apply {
                    setContent {
                        MeloxTheme(AppSettings()) {
                            val state = fixture.value
                            Box(Modifier.fillMaxSize().background(Color(0xFF16161A))) {
                                LyricsView(
                                    document = documents[generation.value],
                                    positionMs = state.position,
                                    positionUpdateElapsedRealtimeMs = SystemClock.elapsedRealtime(),
                                    playbackIteration = 0,
                                    playbackSpeed = 1f,
                                    previewPositionMs = null,
                                    isPlaying = true,
                                    onSeek = {},
                                    contentWidth = 260.dp,
                                    lyricFontScale = state.scale,
                                    lyricFontWeight = 600,
                                    lyricAnimationMode = state.mode,
                                    lyricBlurEnabled = true,
                                    centerLyrics = state.centered,
                                    showLyricsTranslation = state.translated,
                                    showBottomFade = true,
                                    resumeFollowRequestKey = 0,
                                    seekRequestKey = state.seekRevision,
                                    seekPositionMs = state.position,
                                    emphasisColor = Color.White,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
                activity.setContentView(view)
            }
            repeat(24) { index ->
                scenario.onActivity {
                    if (index % 8 == 0) generation.value = index / 8
                    fixture.value = Fixture(position = (index % 8) * 6_000L + 1_700,
                        scale = if (index % 3 == 0) 2f else 1f,
                        centered = index % 2 == 0, translated = index % 4 != 0,
                        seekRevision = index + 2)
                }
                val image = capture(scenario, view, delayMs = 450)
                try {
                    val pixels = IntArray(image.width * image.height)
                    image.getPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
                    assertTrue("Missing rendered lyrics at transition $index",
                        pixels.count { (it shr 16 and 255) > 100 } > 100)
                } finally { image.recycle() }
            }
            // Automatic row replacement also needs coverage; acknowledged seeks alone missed the crash.
            scenario.onActivity {
                generation.value = 3
                fixture.value = Fixture(position = 0, scale = 1.5f, seekRevision = 0)
            }
            val image = capture(scenario, view, delayMs = 10_000)
            try {
                val pixels = IntArray(image.width * image.height)
                image.getPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
                assertTrue("Lyrics disappeared during automatic row replacement",
                    pixels.count { (it shr 16 and 255) > 100 } > 100)
            } finally { image.recycle() }
        }
    }

    @Test
    fun realRendererHandlesModesUnicodeWrappingFontChangesAndReverseSeek() {
        val fixture = mutableStateOf(Fixture())
        val lines = listOf(
            LyricLine("", 0, 6_000, null,
                listOf(LyricWord(0, 4_000, "啊", true), LyricWord(4_000, 6_000, "中文长音", false)),
                "A sustained Chinese note"),
            LyricLine("", 6_000, 12_000, "Swimming through a holy soil — fg jpqy e\u0301 👨‍👩‍👧‍👦", emptyList(), "Generated timing and wrapping"),
            LyricLine("", 12_000, 18_000, null,
                listOf(LyricWord(12_000, 15_000, "office", true), LyricWord(15_000, 18_000, "مرحبا", false)), "Shaped text"),
        )
        val document = LyricsDocument(lines, LyricsFormat.TTML, LyricsSource.SIDECAR)
        lateinit var view: ComposeView
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                view = ComposeView(activity).apply {
                    setContent {
                        MeloxTheme(AppSettings()) {
                            Box(Modifier.fillMaxSize().background(Color(0xFF16161A))) {
                                val state = fixture.value
                                LyricsView(
                                    document = document,
                                    positionMs = state.position,
                                    positionUpdateElapsedRealtimeMs = SystemClock.elapsedRealtime(),
                                    playbackIteration = 0,
                                    playbackSpeed = 1f,
                                    previewPositionMs = null,
                                    isPlaying = false,
                                    onSeek = {},
                                    contentWidth = 260.dp,
                                    lyricFontScale = state.scale,
                                    lyricFontWeight = 600,
                                    lyricAnimationMode = state.mode,
                                    lyricBlurEnabled = false,
                                    centerLyrics = state.centered,
                                    showLyricsTranslation = state.translated,
                                    showBottomFade = true,
                                    resumeFollowRequestKey = 0,
                                    seekRequestKey = state.seekRevision,
                                    seekPositionMs = state.position,
                                    emphasisColor = Color.White,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
                activity.setContentView(view)
            }
            val states = listOf(
                Fixture(),
                Fixture(position = 9_000, centered = true, scale = 1.5f, seekRevision = 2),
                Fixture(position = 15_500, centered = true, translated = false, seekRevision = 3),
                Fixture(position = 1_500, seekRevision = 4),
            ) + LyricAnimationMode.entries.mapIndexed { index, mode ->
                Fixture(position = 9_000, mode = mode, seekRevision = index + 5)
            }
            states.forEachIndexed { index, state ->
                scenario.onActivity { fixture.value = state }
                val image = capture(scenario, view)
                try {
                    val pixels = IntArray(image.width * image.height)
                    image.getPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
                    assertTrue("No visible lyrics for fixture $index", pixels.count { (it shr 16 and 255) > 100 } > 100)
                    val context = InstrumentationRegistry.getInstrumentation().targetContext
                    File(context.cacheDir, "lyric-render-$index.png").outputStream().use {
                        image.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                } finally { image.recycle() }
            }
        }
    }

    private fun capture(scenario: ActivityScenario<MainActivity>, view: ComposeView, delayMs: Long = 900): Bitmap {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(delayMs)
        val complete = CountDownLatch(1)
        var result = -1
        lateinit var image: Bitmap
        scenario.onActivity { activity ->
            val location = IntArray(2)
            view.getLocationInWindow(location)
            image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            PixelCopy.request(activity.window,
                Rect(location[0], location[1], location[0] + view.width, location[1] + view.height),
                image, { result = it; complete.countDown() }, Handler(Looper.getMainLooper()))
        }
        assertTrue(complete.await(10, TimeUnit.SECONDS))
        assertTrue("PixelCopy failed: $result", result == PixelCopy.SUCCESS)
        return image
    }
}
