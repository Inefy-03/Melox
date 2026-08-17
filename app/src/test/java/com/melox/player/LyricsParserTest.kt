package com.melox.player

import com.melox.player.data.lyrics.LyricsParser
import com.melox.player.model.LyricsFormat
import com.melox.player.model.LyricsRenderItem
import com.melox.player.model.LyricsSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsParserTest {
    @Test
    fun enhancedLrcRetainsWordTimingSpacingAndTranslation() {
        val document = LyricsParser.parse(
            raw = """
                [00:10.00]<00:10.00>Hello <00:10.50>world
                [00:10.00]你好世界
                [00:12.00]Next line
            """.trimIndent(),
            source = LyricsSource.SIDECAR,
            durationMs = 15_000L,
        )

        assertNotNull(document)
        assertEquals(LyricsFormat.LRC, document?.format)
        assertEquals(2, document?.lines?.size)
        val first = document!!.lines.first()
        assertEquals("Hello world", first.displayText)
        assertEquals("你好世界", first.translation)
        assertEquals(2, first.words.size)
        assertTrue(first.words.first().hasTrailingSpace)
        assertEquals(10_500L, first.words.first().endTimeMs)
        assertEquals(11_000L, first.words.last().endTimeMs)
    }

    @Test
    fun ttmlRetainsAgentTranslationAndTimedSpans() {
        val document = LyricsParser.parse(
            raw = """
                <?xml version="1.0" encoding="utf-8"?>
                <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                  <body><div>
                    <p begin="00:00:10.000" end="00:00:12.000" ttm:agent="v1">
                      <span begin="00:00:10.000" end="00:00:10.500">Hello</span>
                      <span begin="00:00:10.500" end="00:00:12.000">world</span>
                      <span ttm:role="x-translation">你好世界</span>
                    </p>
                  </div></body>
                </tt>
            """.trimIndent(),
            source = LyricsSource.EMBEDDED,
        )

        val line = document!!.lines.single()
        assertEquals(LyricsFormat.TTML, document.format)
        assertEquals("v1", line.agent)
        assertEquals("Hello world", line.displayText)
        assertEquals("你好世界", line.translation)
        assertEquals(2, line.words.size)
    }

    @Test
    fun ttmlRetainsExplicitWordEndBeforeTheNextWordStarts() {
        val document = LyricsParser.parse(
            raw = """
                <tt xmlns="http://www.w3.org/ns/ttml">
                  <body><div>
                    <p begin="10s" end="12s">
                      <span begin="10s" end="10.2s">Short</span>
                      <span begin="11s" end="12s">gap</span>
                    </p>
                  </div></body>
                </tt>
            """.trimIndent(),
            source = LyricsSource.EMBEDDED,
        )!!

        assertEquals(10_200L, document.lines.single().words.first().endTimeMs)
        assertEquals(11_000L, document.lines.single().words.last().startTimeMs)
    }

    @Test
    fun forcedRevealUsesLineDurationButPlainModeRevealsImmediately() {
        val line = LyricsParser.parse(
            raw = "[00:10.00]Hello world\n[00:12.00]Next",
            source = LyricsSource.SIDECAR,
        )!!.lines.first()

        assertEquals(0.5f, line.revealProgress(11_000L, forceWordByWord = true), 0.001f)
        assertEquals(1f, line.revealProgress(10_001L, forceWordByWord = false), 0.001f)
    }

    @Test
    fun currentLineRespectsTimedGaps() {
        val document = LyricsParser.parse(
            raw = """
                <tt xmlns="http://www.w3.org/ns/ttml">
                  <body><div>
                    <p begin="1s" end="2s">First</p>
                    <p begin="4s" end="5s">Second</p>
                  </div></body>
                </tt>
            """.trimIndent(),
            source = LyricsSource.EMBEDDED,
        )!!

        assertEquals(0, document.currentLineIndex(1_500L))
        assertEquals(-1, document.currentLineIndex(3_000L))
        assertEquals(1, document.currentLineIndex(4_500L))
        assertEquals(0, document.focusLineIndex(0L))
        assertEquals(1, document.focusLineIndex(3_000L))
        assertEquals(1, document.focusLineIndex(9_000L))
    }

    @Test
    fun denseLrcLinesRemainIndividuallyCurrent() {
        val document = LyricsParser.parse(
            raw = """
                [00:11.100]Guitar
                [00:11.640]Guitar:
                [00:11.850]Bass:
                [00:12.270]Drums:
                [00:12.510]Lyrics
            """.trimIndent(),
            source = LyricsSource.SIDECAR,
            durationMs = 20_000L,
        )!!

        assertEquals(0, document.currentLineIndex(11_100L))
        assertEquals(1, document.currentLineIndex(11_640L))
        assertEquals(2, document.currentLineIndex(11_850L))
        assertEquals(3, document.currentLineIndex(12_270L))
        assertEquals(4, document.currentLineIndex(12_510L))
    }

    @Test
    fun longTimedGapPrecomputesStableThreeDotTransition() {
        val document = LyricsParser.parse(
            raw = """
                [00:01.00]<00:01.00>First
                [00:06.50]Second
            """.trimIndent(),
            source = LyricsSource.SIDECAR,
        )!!

        assertEquals(1, document.transitions.size)
        val transition = document.transitions.single()
        assertEquals(0, transition.afterLineIndex)
        assertEquals(1_500L, transition.startTimeMs)
        assertEquals(6_500L, transition.endTimeMs)
        assertTrue(transition.isActive(4_000L))
        assertEquals(-1, document.currentLineIndex(4_000L))
        assertEquals(1, document.focusLineIndex(4_000L))
        assertEquals(0, document.transitionIndex(4_000L))

        val renderItems = document.renderItems()
        assertEquals(3, renderItems.size)
        assertTrue(renderItems[0] is LyricsRenderItem.Line)
        assertTrue(renderItems[1] is LyricsRenderItem.Transition)
        assertTrue(renderItems[2] is LyricsRenderItem.Line)
    }

    @Test
    fun gapShorterThanFiveSecondsDoesNotCreateTransition() {
        val document = LyricsParser.parse(
            raw = """
                [00:01.00]<00:01.00>First
                [00:06.49]Second
            """.trimIndent(),
            source = LyricsSource.SIDECAR,
        )!!

        assertTrue(document.transitions.isEmpty())
    }

    @Test
    fun untimedOrMalformedLyricsAreIgnored() {
        assertNull(
            LyricsParser.parse(
                raw = "plain lyrics without timestamps",
                source = LyricsSource.EMBEDDED,
            ),
        )
        assertFalse(
            LyricsParser.parse(
                raw = "[00:01.00]Timed",
                source = LyricsSource.SIDECAR,
            )!!.lines.isEmpty(),
        )
    }
}
