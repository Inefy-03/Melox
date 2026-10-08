package com.melox.player

import com.melox.player.data.lyrics.LyricsParser
import com.melox.player.model.LyricsSource
import com.melox.player.ui.screen.playback.lyricTextChunks
import com.melox.player.ui.screen.playback.lyricWrapRanges
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricTextLayoutTest {
    @Test
    fun explicitLineBreaksRemainSeparateWhenTheWholeSpanFits() {
        val text = "Swimming\nthrough a\nholy soil"
        val chunks = lyricTextChunks(text, 1_000f) { it.length.toFloat() }
            .map { text.substring(it) }
        assertEquals(listOf("Swimming", "\n", "through a", "\n", "holy soil"), chunks)
        assertEquals(text, chunks.joinToString(""))
        assertEquals(listOf(0..1, 2..3, 4..4), lyricWrapRanges(chunks, List(chunks.size) { 10f }, 1_000f))
    }

    @Test
    fun repeatedHardBreaksPreserveBlankRowsAndGraphemeChunks() {
        val text = "e\u0301😀\n\nfgjpqy"
        val chunks = lyricTextChunks(text, 20f) { it.codePointCount(0, it.length) * 10f }
            .map { text.substring(it) }
        assertEquals(text, chunks.joinToString(""))
        assertTrue(chunks.none { it.first().isLowSurrogate() || it.first() == '\u0301' })
        val ranges = lyricWrapRanges(chunks, List(chunks.size) { 10f }, 1_000f)
        assertEquals(listOf("e\u0301😀\n", "\n", "fgjpqy"), ranges.map { chunks.slice(it).joinToString("") })
    }

    @Test
    fun wrappingFillsTheCurrentLineInsteadOfBalancingTheParagraph() {
        val parts = listOf("one ", "two ", "three ", "four ")
        assertEquals(listOf(0..2, 3..3), lyricWrapRanges(parts, List(4) { 30f }, 100f))
    }

    @Test
    fun spareWidthAcceptsAnySpacedSymbolBeforeWrappingTheFollowingWord() {
        for (symbol in listOf("-", "—", "/", "+", "&")) {
            val parts = listOf("prefix ", "word ", "$symbol ", "tail")
            assertEquals(
                "Symbol $symbol must not pull an already fitting word onto the next row",
                listOf(0..2, 3..3),
                lyricWrapRanges(parts, listOf(40f, 40f, 20f, 60f), 100f),
            )
        }
    }

    @Test
    fun trailingWhitespaceDoesNotRejectAWordWhoseInkStillFits() {
        assertEquals(
            listOf(0..2, 3..3),
            lyricWrapRanges(
                contents = listOf("one ", "two ", "three ", "four"),
                widths = listOf(36f, 36f, 36f, 30f),
                availableWidthPx = 102f,
                lineEndWidths = listOf(30f, 30f, 30f, 30f),
            ),
        )
    }

    @Test
    fun princeLyricKeepsTimedWordsAndAllowsBreaksAroundTheSpacedHyphen() {
        val raw = "[00:00.000] <00:00.000>I<00:01.262> <00:02.524>Wanna<00:03.786> <00:05.048>Be<00:06.310> <00:07.572>Your<00:08.834> <00:10.096>Lover<00:11.358> <00:12.620>-<00:13.882> <00:15.144>Prince<00:16.406>"
        val line = LyricsParser.parse(raw, LyricsSource.SIDECAR)!!.lines.single()
        assertEquals(listOf("I", "Wanna", "Be", "Your", "Lover", "-", "Prince"), line.words.map { it.text })
        assertEquals(listOf(0L, 2_524L, 5_048L, 7_572L, 10_096L, 12_620L, 15_144L), line.words.map { it.startTimeMs })
        val contents = line.words.takeLast(3).map { it.text + if (it.hasTrailingSpace) " " else "" }
        assertEquals(listOf("Lover ", "- ", "Prince"), contents)
        assertEquals(listOf(0..0, 1..1, 2..2), lyricWrapRanges(contents, listOf(60f, 20f, 60f), 70f))
        assertEquals(listOf(0..1, 2..2), lyricWrapRanges(contents, listOf(60f, 20f, 60f), 100f))
    }

    @Test
    fun longChineseLineWrapsWithoutSpaces() {
        val parts = "这是一句没有空格的中文歌词".map(Char::toString)
        val lines = lyricWrapRanges(parts, List(parts.size) { 30f }, 100f)
        assertTrue(lines.size > 1)
        assertTrue(lines.all { it.count() * 30 <= 100 })
        assertEquals(parts.indices.toList(), lines.flatMap { it.toList() })
    }

    @Test
    fun ordinaryEnglishSyllablesStayTogetherWhenTheWordFits() {
        val parts = listOf("Hel", "lo ", "world")
        val lines = lyricWrapRanges(parts, listOf(30f, 30f, 50f), 70f)
        assertEquals(listOf(0..1, 2..2), lines)
    }

    @Test
    fun oversizedWordBreaksBetweenTimedSyllables() {
        val lines = lyricWrapRanges(List(10) { "a" }, List(10) { 30f }, 100f)
        assertTrue(lines.size > 1)
        assertTrue(lines.all { it.count() * 30 <= 100 })
        assertEquals((0..9).toList(), lines.flatMap { it.toList() })
    }

    @Test
    fun oversizedSingleSpanSplitsWithoutLosingTextOrGraphemes() {
        for (text in listOf("averylongenglishword", "一整句很长的中文歌词", "e\u0301e\u0301e\u0301", "😀😀😀😀")) {
            val chunks = lyricTextChunks(text, 30f) { it.codePointCount(0, it.length) * 10f }
                .map { text.substring(it) }
            assertEquals(text, chunks.joinToString(""))
            assertTrue(chunks.all { it.codePointCount(0, it.length) * 10 <= 30 })
            assertTrue(chunks.none { it.first().isLowSurrogate() || it.first() == '\u0301' })
            assertTrue(chunks.none { it.last().isHighSurrogate() })
        }
    }

    @Test
    fun oversizedPhrasePrefersWordBoundaries() {
        val text = "Hello world again"
        val chunks = lyricTextChunks(text, 8f) { it.length.toFloat() }.map { text.substring(it) }
        assertEquals(listOf("Hello ", "world ", "again"), chunks)
    }

    @Test
    fun widthBoundaryWhitespaceDoesNotCreateBlankRows() {
        val text = "abc def ghi"
        val chunks = lyricTextChunks(text, 3f) { it.length.toFloat() }.map { text.substring(it) }
        assertEquals(listOf("abc ", "def ", "ghi"), chunks)
        assertTrue(chunks.all { it.trimEnd().length <= 3 })
        assertTrue(chunks.none(String::isBlank))
        assertEquals(text, chunks.joinToString(""))
    }

    @Test
    fun emptyOrTinyViewportStillTerminatesWithoutDroppingText() {
        assertEquals(emptyList<IntRange>(), lyricWrapRanges(emptyList(), emptyList(), 0f))
        assertEquals(listOf(0..0, 1..1), lyricTextChunks("ab", 0f) { it.length * 20f })
        assertEquals(listOf(0..0, 1..1), lyricWrapRanges(listOf("a", "b"), listOf(20f, 20f), 0f))
    }
}
