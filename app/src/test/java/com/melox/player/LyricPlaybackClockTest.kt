package com.melox.player

import androidx.compose.ui.unit.dp
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.melox.player.model.LyricLine
import com.melox.player.model.LyricAuxiliaryText
import com.melox.player.model.LyricWord
import com.melox.player.model.LyricRuby
import com.melox.player.data.lyrics.LyricsParser
import com.melox.player.model.LyricsDocument
import com.melox.player.model.LyricsFormat
import com.melox.player.model.LyricsSource
import com.melox.player.model.LyricsUiState
import com.melox.player.ui.screen.playback.LyricPlaybackClock
import com.melox.player.ui.screen.playback.compactLyricsDocument
import com.melox.player.ui.screen.playback.compactPrimaryLines
import com.melox.player.ui.screen.playback.compactLyricAuxiliaryText
import com.melox.player.ui.screen.playback.compactLyricSingleLine
import com.melox.player.ui.screen.playback.lyricPhonetics
import com.melox.player.ui.screen.playback.compactLyricRowCenters
import com.melox.player.ui.screen.playback.compactLyricIndexAtPosition
import com.melox.player.ui.screen.playback.compactLyricScrollTarget
import com.melox.player.ui.screen.playback.CompactLyricScroll
import com.melox.player.ui.screen.playback.compactArtworkLayout
import com.melox.player.ui.screen.playback.PLAYER_ARTWORK_CONTAINER_EXPANSION
import com.melox.player.ui.screen.playback.PLAYER_ARTWORK_PLAYING_PADDING
import com.melox.player.ui.screen.playback.PLAYER_ARTWORK_VERTICAL_FOOTPRINT_EXPANSION
import com.melox.player.ui.screen.playback.lyricPlaybackPositionMs
import com.melox.player.ui.screen.playback.stabilizedLyricPlaybackPositionMs
import com.melox.player.ui.screen.playback.rememberRetainedLyricDocument
import com.melox.player.ui.screen.playback.lyricPagePresentation
import com.melox.player.ui.screen.playback.rememberLyricContentFrame
import com.melox.player.ui.screen.playback.LyricContentFrame
import com.melox.player.ui.screen.playback.rememberCompactArtworkLyricsPresence
import com.melox.player.ui.screen.playback.fitPlayerArtworkSize
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LyricPlaybackClockTest {
    @Test
    fun lyricEmptyStateHasItsOwnFrameAndRetainsOutgoingContentAcrossLoads() = runBlocking {
        val available = LyricsUiState.Available(
            LyricsDocument(listOf(LyricLine("main", 0, 1_000, "Lyrics", emptyList(), null)),
                LyricsFormat.LRC, LyricsSource.SIDECAR), "song")
        val input = mutableStateOf<LyricsUiState>(LyricsUiState.Loading)
        var rendered: LyricContentFrame? = null
        withArtworkComposition(content = {
            val retained = rememberRetainedLyricDocument(input.value)
            val target = rememberLyricContentFrame(lyricPagePresentation(retained), false)
            SideEffect { rendered = target }
        }) {
            frame(0)
            val initialEmpty = requireNotNull(rendered)
            input.value = LyricsUiState.Unavailable
            frame(16)
            val empty = requireNotNull(rendered)
            assertSame(initialEmpty, empty)
            assertEquals(LyricsUiState.Unavailable, empty.lyrics)
            input.value = available
            frame(32)
            val ready = requireNotNull(rendered)
            input.value = LyricsUiState.Loading
            frame(48)
            assertSame(ready, rendered)
            input.value = LyricsUiState.Unavailable
            frame(64)
            assertEquals(LyricsUiState.Unavailable, requireNotNull(rendered).lyrics)
            assertNotSame(empty, rendered)
            // An outgoing empty frame must not depend on the latest loading state.
            input.value = LyricsUiState.Loading
            frame(80)
            assertEquals(LyricsUiState.Unavailable, requireNotNull(rendered).lyrics)
            assertEquals(LyricsUiState.Unavailable, empty.lyrics)
        }
    }

    @Test
    fun lyricPageTreatsEmptyBlankAndBackgroundOnlyDocumentsAsUnavailable() {
        val line = LyricLine("main", 0, 1_000, "Lyrics", emptyList(), null)
        val available = LyricsUiState.Available(
            LyricsDocument(listOf(line), LyricsFormat.LRC, LyricsSource.SIDECAR), "song")
        assertSame(available, lyricPagePresentation(available))
        assertEquals(LyricsUiState.Unavailable, lyricPagePresentation(null))
        for (lines in listOf(emptyList(), listOf(line.copy(text = " ")),
            listOf(line.copy(isBackground = true)))) {
            assertEquals(LyricsUiState.Unavailable,
                lyricPagePresentation(available.copy(document = available.document.copy(lines = lines))))
        }
    }

    @Test
    fun artworkLyricsKeepTheResolvedLayoutDuringLoadingAndReactToConfirmedResults() = runBlocking {
        val line = LyricLine("main", 0, 1_000, "Lyrics", emptyList(), null)
        val available = LyricsUiState.Available(
            LyricsDocument(listOf(line), LyricsFormat.LRC, LyricsSource.SIDECAR), "song")
        val input = mutableStateOf<LyricsUiState>(LyricsUiState.Loading)
        var present = false
        withArtworkComposition(content = {
            val current = rememberCompactArtworkLyricsPresence(input.value)
            SideEffect { present = current }
        }) {
            frame(0)
            assertTrue(present)
            input.value = available
            frame(16)
            assertTrue(present)
            input.value = LyricsUiState.Loading
            frame(32)
            assertTrue(present)
            input.value = available.copy(mediaId = "next")
            frame(48)
            assertTrue(present)

            input.value = LyricsUiState.Unavailable
            frame(64)
            assertFalse(present)
            input.value = LyricsUiState.Loading
            frame(80)
            assertFalse(present)
            input.value = available.copy(document = available.document.copy(lines = emptyList()))
            frame(96)
            assertFalse(present)
            input.value = available.copy(document = available.document.copy(lines = listOf(
                line.copy(isBackground = true), line.copy(text = " "))))
            frame(112)
            assertFalse(present)
            input.value = LyricsUiState.Loading
            frame(128)
            assertFalse(present)
            input.value = available
            frame(144)
            assertTrue(present)
        }
    }

    @Test
    fun lyricLoadingRetainsTheOldDocumentUntilItsReplacementIsActuallyAvailable() = runBlocking {
        val first = LyricsUiState.Available(
            LyricsDocument(listOf(LyricLine("main", 0, 1_000, "First", emptyList(), null)),
                LyricsFormat.LRC, LyricsSource.SIDECAR), "first")
        val second = first.copy(mediaId = "second")
        val input = mutableStateOf<LyricsUiState>(first)
        var displayed: LyricsUiState.Available? = null
        withArtworkComposition(content = {
            val retained = rememberRetainedLyricDocument(input.value)
            SideEffect { displayed = retained }
        }) {
            frame(0)
            assertEquals(first, displayed)
            input.value = LyricsUiState.Loading
            frame(16)
            frame(500)
            assertEquals(first, displayed)
            input.value = second
            frame(516)
            assertEquals(second, displayed)
            input.value = LyricsUiState.Unavailable
            frame(532)
            assertNull(displayed)
            input.value = LyricsUiState.Loading
            frame(548)
            assertNull(displayed)
        }
    }

    @Test
    fun coalescedLyricLoadsPresentTheLatestConfirmedDocumentWithoutAnEmptyTarget() = runBlocking {
        val first = LyricsUiState.Available(LyricsDocument(emptyList(), LyricsFormat.LRC, LyricsSource.SIDECAR), "first")
        val input = mutableStateOf<LyricsUiState>(first)
        val observed = mutableListOf<String?>()
        withArtworkComposition(content = {
            val retained = rememberRetainedLyricDocument(input.value)
            SideEffect { observed += retained?.mediaId }
        }) {
            frame(0)
            input.value = LyricsUiState.Loading
            frame(16)
            input.value = first.copy(mediaId = "skipped")
            input.value = first.copy(mediaId = "latest")
            frame(32)
            assertEquals("latest", observed.last())
            assertFalse("skipped" in observed)
            assertFalse(null in observed)
        }
    }

    @Test
    fun compactLyricsFollowPrimaryVocalsAndKeepCompletedLinesThroughGaps() {
        val first = LyricLine("lead", 1_000, 2_000, "First", emptyList(), "Translation")
        val second = first.copy(startTimeMs = 5_000, endTimeMs = 6_000, text = "Second")
        val document = LyricsDocument(
            listOf(
                first,
                first.copy(startTimeMs = 1_500, text = "Backing", isBackground = true),
                first.copy(startTimeMs = 3_000, text = " "),
                second,
            ), LyricsFormat.TTML, LyricsSource.EMBEDDED,
        )
        val lines = compactPrimaryLines(document)
        assertEquals(listOf(first, second), lines)
        assertEquals(-1, compactLyricIndexAtPosition(lines, 999))
        assertEquals(0, compactLyricIndexAtPosition(lines, 1_000))
        assertEquals(0, compactLyricIndexAtPosition(lines, 4_999))
        assertEquals(1, compactLyricIndexAtPosition(lines, 5_000))
        assertEquals(1, compactLyricIndexAtPosition(lines, 9_000))
        // Backward seeks and repeat restarts select directly, without replaying intermediate lines.
        assertEquals(0, compactLyricIndexAtPosition(lines, 1_500))
        assertEquals(-1, compactLyricIndexAtPosition(lines, 0))
        assertEquals(-1, compactLyricIndexAtPosition(emptyList(), 10_000))
    }

    @Test
    fun compactLyricsRejectDocumentsFromThePreviousTrack() {
        val document = LyricsDocument(emptyList(), LyricsFormat.LRC, LyricsSource.SIDECAR)
        val state = LyricsUiState.Available(document, "first")
        assertSame(document, compactLyricsDocument(state, "first"))
        assertNull(compactLyricsDocument(state, "second"))
        assertNull(compactLyricsDocument(state, null))
        assertNull(compactLyricsDocument(LyricsUiState.Loading, "first"))
        assertNull(compactLyricsDocument(LyricsUiState.Unavailable, "first"))
    }

    @Test
    fun compactLyricsPackOnlyPresentAuxiliaryTextWithConsistentEntryGaps() {
        val primary = LyricLine("main", 0, 1_000, "Original", emptyList(), null)
        val translated = primary.copy(translation = "Translation")
        val romanized = primary.copy(romanizations = listOf(LyricAuxiliaryText("Romanization")))
        val both = romanized.copy(translation = "Translation")
        val blank = primary.copy(translation = " \n ", romanizations = listOf(LyricAuxiliaryText(" ")))
        val auxiliary = listOf(primary, translated, romanized, both, blank).map {
            compactLyricAuxiliaryText(it, showTranslation = true, showRomanization = true)
        }
        assertEquals(listOf(emptyList<String>(), listOf("Translation"), listOf("Romanization"),
            listOf("Romanization", "Translation"), emptyList<String>()), auxiliary)
        val multilingual = romanized.copy(translations = listOf(
            LyricAuxiliaryText("English", "en"), LyricAuxiliaryText("Chinese", "zh-Hans")))
        assertEquals(listOf("Romanization", "Chinese"), compactLyricAuxiliaryText(multilingual,
            true, true, com.melox.player.model.LyricsTranslationLanguage.AUTO, "zh-CN"))
        assertEquals(listOf("English"), compactLyricAuxiliaryText(multilingual,
            true, false, com.melox.player.model.LyricsTranslationLanguage.ENGLISH, "zh-CN"))
        assertEquals(listOf("Romanization", "English", "Chinese"), compactLyricAuxiliaryText(multilingual,
            true, true, com.melox.player.model.LyricsTranslationLanguage.ALL, "zh-CN"))
        assertEquals(listOf("reading one", "reading two", "Translation", "second line"), compactLyricAuxiliaryText(
            both.copy(translation = "Translation\r\nsecond line", romanizations = listOf(
                LyricAuxiliaryText("reading one"), LyricAuxiliaryText("reading two"))), true, true))
        assertEquals(listOf("Translation"), compactLyricAuxiliaryText(both, true, false))
        assertEquals(listOf("Romanization"), compactLyricAuxiliaryText(both, false, true))
        assertEquals(emptyList<String>(), compactLyricAuxiliaryText(both, false, false))

        val heights = auxiliary.map { 20f + 16f * it.size }
        val centers = compactLyricRowCenters(heights, 6f)
        assertArrayEquals(floatArrayOf(10f, 44f, 86f, 136f, 178f), centers, 0f)
        for (index in 1..heights.lastIndex) {
            val previousBottom = centers[index - 1] + heights[index - 1] / 2f
            val nextTop = centers[index] - heights[index] / 2f
            assertEquals(6f, nextTop - previousBottom, 0f)
        }
    }

    @Test
    fun compactLyricsKeepMappedPronunciationAboveAndUnmappedCaptionsBelow() {
        val document = LyricsParser.parse("""
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:i="http://music.apple.com/lyric-ttml-internal">
            <head><metadata><i:transliterations><i:transliteration xml:lang="ja-Latn">
            <i:text for="L10"><span begin="1s" end="2s">mai</span><span begin="2s" end="3s">a</span><span begin="3s" end="4s">ge,</span></i:text>
            </i:transliteration></i:transliterations></metadata></head>
            <body><div><p begin="1s" end="4s" i:key="L10"><span begin="1s" end="2s">舞い</span><span begin="2s" end="3s">上</span><span begin="3s" end="4s">げ、</span></p></div></body></tt>
        """.trimIndent(), LyricsSource.SIDECAR)!!
        val line = document.lines.single().copy(translation = "Translation")
        assertEquals(listOf("mai", "a", "ge,"), lyricPhonetics(compactLyricSingleLine(line)).words)
        assertEquals(listOf("Translation"), compactLyricAuxiliaryText(line, true, true))
        val mixed = line.copy(romanizations = line.romanizations + LyricAuxiliaryText("mai a ge"))
        assertEquals(listOf("mai a ge", "Translation"), compactLyricAuxiliaryText(mixed, true, true))
        assertEquals(listOf("Translation"), compactLyricAuxiliaryText(mixed, true, false))
        val untimed = line.copy(romanizations = line.romanizations.map { it.copy(words = emptyList()) })
        assertFalse(lyricPhonetics(untimed).hasAnnotations)
        assertEquals(listOf("maiage,", "Translation"), compactLyricAuxiliaryText(untimed, true, true))
        val ruby = line.copy(romanizations = emptyList(), words = listOf(
            LyricWord(1_000, 4_000, "今日", false, listOf(LyricRuby("kyou\n", 1_000, 4_000)))))
        val flattened = compactLyricSingleLine(ruby)
        assertEquals(listOf("kyou "), lyricPhonetics(flattened).words)
        assertEquals(listOf("Translation"), compactLyricAuxiliaryText(flattened, true, true))
        assertEquals(ruby.words.single().ruby.single().startTimeMs, flattened.words.single().ruby.single().startTimeMs)
    }

    @Test
    fun compactOriginalHardBreaksUseOneDisplayRowWithoutChangingWordTimes() {
        for (separator in listOf("\r\n", "\r", "\n")) {
            val words = listOf(LyricWord(1_000, 2_000, "Original${separator}second", false))
            val line = LyricLine("main", 1_000, 2_000, null, words, null)
            val single = compactLyricSingleLine(line)
            assertEquals("Original second", single.displayText)
            assertEquals(listOf(1_000L to 2_000L), single.words.map { it.startTimeMs to it.endTimeMs })
            assertEquals(words, line.words)
            assertEquals("Original second", compactLyricSingleLine(line.copy(text = line.displayText, words = emptyList())).displayText)
        }
    }

    @Test
    fun compactVisibleWindowPreservesVariableHeightCentersDuringFractionalScrolls() {
        val heights = listOf(20f, 52f, 36f, 68f, 20f, 44f, 84f, 36f, 20f)
        val global = compactLyricRowCenters(heights, 6f)
        fun centerAt(centers: FloatArray, position: Float, firstRow: Int): Float {
            val lower = (position.toInt() - firstRow).coerceIn(centers.indices)
            val upper = (lower + 1).coerceAtMost(centers.lastIndex)
            return centers[lower] + (centers[upper] - centers[lower]) * (position - lower - firstRow)
        }
        for (focus in heights.indices) {
            val visible = (focus - 2).coerceAtLeast(0)..(focus + 2).coerceAtMost(heights.lastIndex)
            val local = compactLyricRowCenters(heights.slice(visible), 6f)
            for (delta in listOf(-1.5f, -0.5f, 0f, 0.5f, 1.5f)) {
                val position = (focus + delta).coerceIn(0f, heights.lastIndex.toFloat())
                for (row in visible) {
                    assertEquals(global[row] - centerAt(global, position, 0),
                        local[row - visible.first] - centerAt(local, position, visible.first), 0.001f)
                }
            }
        }
    }

    @Test
    fun compactLongLinesFollowTheCursorAndStopAtTextEdges() {
        assertEquals(0f, compactLyricScrollTarget(100f, 400f, 0f, false), 0f)
        assertEquals(-150f, compactLyricScrollTarget(100f, 400f, 200f, false), 0f)
        assertEquals(-300f, compactLyricScrollTarget(100f, 400f, 400f, false), 0f)
        assertEquals(20f, compactLyricScrollTarget(100f, 60f, 60f, true), 0f)
        assertEquals(0f, compactLyricScrollTarget(100f, 60f, 60f, false), 0f)
        // The scrolling end stops before the artwork viewport's horizontal fade.
        assertEquals(-306f, compactLyricScrollTarget(100f, 400f, 400f, false, 6f), 0f)
        assertEquals(-306f, compactLyricScrollTarget(100f, 400f, 400f, true, 6f), 0f)
        assertEquals(0f, compactLyricScrollTarget(100f, 400f, 0f, false, 6f), 0f)
        assertEquals(-150f, compactLyricScrollTarget(100f, 400f, 200f, false, 6f), 0f)
        assertEquals(20f, compactLyricScrollTarget(100f, 60f, 60f, true, 6f), 0f)
        assertEquals(0f, compactLyricScrollTarget(100f, 60f, 60f, false, 6f), 0f)
        val scroll = CompactLyricScroll()
        assertEquals(0f, scroll.update(0f, 1_000, 100f, 400f), 0f)
        val next = scroll.update(-100f, 1_250, 100f, 400f)
        assertEquals(-63.212f, next, 0.001f)
        repeat(3) { assertEquals(next, scroll.update(-100f, 1_250, 100f, 400f), 0f) }
        // A backward seek and a resize reanchor without scrolling across obsolete text.
        assertEquals(-200f, scroll.update(-200f, 500, 100f, 400f), 0f)
        assertEquals(-20f, scroll.update(-20f, 516, 200f, 400f), 0f)
        assertEquals(-40f, scroll.update(-40f, 532, 200f, 400f, smooth = false), 0f)
    }

    @Test
    fun portraitLyricsUseSaltSpaceBudgetAndPlayingArtworkBounds() {
        val geometry = compactArtworkLayout(600.dp, 330.dp, 28.dp)
        val playingInset = (PLAYER_ARTWORK_VERTICAL_FOOTPRINT_EXPANSION -
            PLAYER_ARTWORK_CONTAINER_EXPANSION) / 2f + PLAYER_ARTWORK_PLAYING_PADDING
        val artworkBottom = geometry.artworkTop + geometry.artworkHeight - playingInset
        val lyricCenter = geometry.lyricsTop + geometry.lyricsHeight / 2f
        assertEquals((artworkBottom.value + 628f) / 2f, lyricCenter.value, 0.001f)
        assertEquals(68.dp, geometry.lyricsHeight)
        assertEquals(330.dp + PLAYER_ARTWORK_VERTICAL_FOOTPRINT_EXPANSION, geometry.artworkHeight)
        assertTrue(geometry.artworkTop < (600.dp - geometry.artworkHeight) / 2f)
        assertTrue(geometry.lyricsTop >= artworkBottom)
        assertTrue(geometry.lyricsTop + geometry.lyricsHeight <= 600.dp)

        val withoutLyrics = compactArtworkLayout(600.dp, 330.dp, 28.dp, reserveLyrics = false)
        assertEquals(300.dp, withoutLyrics.artworkTop + withoutLyrics.artworkHeight / 2f)
        assertTrue(withoutLyrics.artworkTop > geometry.artworkTop)
        assertEquals(0.dp, withoutLyrics.lyricsHeight)
        assertEquals(fitPlayerArtworkSize(330.dp, 600.dp),
            withoutLyrics.artworkHeight - PLAYER_ARTWORK_VERTICAL_FOOTPRINT_EXPANSION)

        assertEquals(24.dp, compactArtworkLayout(416.dp, 330.dp, 28.dp).lyricsHeight)
        assertEquals(68.dp, compactArtworkLayout(417.dp, 330.dp, 28.dp).lyricsHeight)

        val small = compactArtworkLayout(300.dp, 330.dp, 28.dp)
        assertEquals(24.dp, small.lyricsHeight)
        assertTrue(small.artworkHeight < geometry.artworkHeight)
        assertTrue(small.lyricsTop + small.lyricsHeight <= 300.dp)
        val smallWithoutLyrics = compactArtworkLayout(300.dp, 330.dp, 28.dp, reserveLyrics = false)
        assertEquals(150.dp, smallWithoutLyrics.artworkTop + smallWithoutLyrics.artworkHeight / 2f)
        assertEquals(fitPlayerArtworkSize(330.dp, 300.dp),
            smallWithoutLyrics.artworkHeight - PLAYER_ARTWORK_VERTICAL_FOOTPRINT_EXPANSION)
        val empty = compactArtworkLayout(0.dp, 330.dp, 28.dp)
        assertEquals(0.dp, empty.artworkHeight)
        assertEquals(0.dp, empty.lyricsHeight)
        assertEquals(0.dp, empty.lyricsTop)
    }

    @Test
    fun hiddenPlaybackSamplesKeepTheClockReadyWithoutFrames() {
        val clock = LyricPlaybackClock(1_000, 0)
        val sample = lyricPlaybackPositionMs(60_000, 10_000, 10_500, true, 2f)
        clock.updateFromPlayback(0, sample, active = false)
        assertEquals(61_000.0, clock.positionMs.doubleValue, 0.0)

        clock.updateFromPlayback(0, sample, active = true)
        // A slightly older active sample must not replace the smooth clock after activation.
        clock.updateFromPlayback(0, 60_900, active = true)
        assertEquals(61_000.0, clock.positionMs.doubleValue, 0.0)
    }

    @Test
    fun activeSystemSeekResetsOnceAndOrdinaryBackwardSamplesStayFiltered() {
        val clock = LyricPlaybackClock(180_000, 0)
        clock.updateFromPlayback(0, 180_000, active = true)
        assertTrue(clock.updateFromPlayback(0, 30_000, active = true, positionDiscontinuityRevision = 1))
        assertEquals(30_000.0, clock.positionMs.doubleValue, 0.0)
        assertFalse(clock.updateFromPlayback(0, 29_990, active = true, positionDiscontinuityRevision = 1))
        assertEquals(30_000.0, clock.positionMs.doubleValue, 0.0)
    }

    @Test
    fun reopeningReanchorsEvenWithoutASeekEventOrRunningFrames() {
        val clock = LyricPlaybackClock(180_000, 0)
        clock.updateFromPlayback(0, 180_000, active = true)
        clock.updateFromPlayback(0, 180_000, active = false)
        assertTrue(clock.updateFromPlayback(0, 30_000, active = true))
        assertEquals(30_000.0, clock.positionMs.doubleValue, 0.0)
    }

    @Test
    fun hiddenSystemSeeksInBothDirectionsReplaceTheOldPosition() {
        val clock = LyricPlaybackClock(1_000, 0)
        for (target in listOf(180_000L, 30_000L)) {
            clock.updateFromPlayback(0, target, active = false)
            assertEquals(target.toDouble(), clock.positionMs.doubleValue, 0.0)
            clock.updateFromPlayback(0, target, active = true)
            val firstFrame = stabilizedLyricPlaybackPositionMs(
                clock.positionMs.doubleValue, target, 0L, 1f,
            )
            assertEquals(target.toDouble(), firstFrame, 0.0)
        }
    }

    @Test
    fun pausedHiddenSeekUsesTheControllerPositionWithoutExtrapolation() {
        val clock = LyricPlaybackClock(180_000, 0)
        val sample = lyricPlaybackPositionMs(30_000, 10_000, 30_000, false, 1f)
        clock.updateFromPlayback(0, sample, active = false)
        clock.updateFromPlayback(0, sample, active = true)
        assertEquals(30_000.0, clock.positionMs.doubleValue, 0.0)
    }

    @Test
    fun consecutiveRepeatsResetTheSameDocumentAndItsProgress() {
        val lyrics = LyricsDocument(
            listOf(
                LyricLine("", 0, 5_000, "First", emptyList(), null),
                LyricLine("", 5_000, 10_000, "Last", emptyList(), null),
            ), LyricsFormat.LRC, LyricsSource.EMBEDDED,
        )
        val clock = LyricPlaybackClock(9_900, 0)
        for (iteration in 1L..3L) {
            clock.positionMs.doubleValue = 9_900.0
            assertEquals(1, lyrics.focusLineIndex(clock.positionMs.doubleValue.toLong()))
            assertTrue(clock.resetForIteration(iteration, 20))
            assertEquals(0, lyrics.currentLineIndex(clock.positionMs.doubleValue.toLong()))
            assertTrue(lyrics.lines.first().revealProgress(20, true) < 0.01f)
            assertEquals(20.0, clock.positionMs.doubleValue, 0.0)
        }
    }

    @Test
    fun conflatedRoundTripToTheSameTrackStartsANewClockInterval() {
        val clock = LyricPlaybackClock(90_000, 10)
        // Two media transitions occurred before the UI collected the latest snapshot.
        assertTrue(clock.resetForIteration(12, 0))
        assertEquals(0.0, clock.positionMs.doubleValue, 0.0)
        assertFalse(clock.resetForIteration(12, 0))
        clock.positionMs.doubleValue = stabilizedLyricPlaybackPositionMs(
            clock.positionMs.doubleValue, 17, 16_666_667, 1f,
        )
        assertEquals(17.0, clock.positionMs.doubleValue, 0.0)
    }

    @Test
    fun ordinaryBackwardSamplesNeverResetTheClock() {
        val clock = LyricPlaybackClock(9_900, 2)
        assertFalse(clock.resetForIteration(2, 20))
        clock.positionMs.doubleValue = stabilizedLyricPlaybackPositionMs(
            clock.positionMs.doubleValue, 20, 16_666_667, 1f,
        )
        assertTrue(clock.positionMs.doubleValue > 9_900)
    }

    @Test
    fun suspendedFramesReanchorToPlaybackInsteadOfAddingBackgroundTime() {
        assertEquals(12_000.0, stabilizedLyricPlaybackPositionMs(
            10_000.0, 12_000, 30_000_000_000L, 1f,
        ), 0.0)
        assertEquals(2_000.0, stabilizedLyricPlaybackPositionMs(
            10_000.0, 2_000, 30_000_000_000L, 1f,
        ), 0.0)
        assertEquals(32_000.0, stabilizedLyricPlaybackPositionMs(
            10_000.0, 32_000, 30_000_000_000L, 1f,
        ), 0.0)
    }

    @Test
    fun lateRepeatSampleResetsToItsRealPositionAndIsConsumedOnlyOnce() {
        val clock = LyricPlaybackClock(240_000, 0)
        assertTrue(clock.resetForIteration(1, 650))
        clock.positionMs.doubleValue = stabilizedLyricPlaybackPositionMs(
            clock.positionMs.doubleValue, 650, 16_666_667, 1f,
        )
        val advanced = clock.positionMs.doubleValue
        assertFalse(clock.resetForIteration(1, 650))
        assertEquals(advanced, clock.positionMs.doubleValue, 0.0)
        assertTrue(advanced > 650 && advanced < 700)
    }
}
