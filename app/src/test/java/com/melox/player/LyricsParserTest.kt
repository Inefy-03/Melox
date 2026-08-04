package com.melox.player

import com.melox.player.data.lyrics.LyricsParser
import com.melox.player.model.LyricsFormat
import com.melox.player.model.LyricsSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LyricsParserTest {
    @Test
    fun lrcSupportsOffsetsAndMultipleTimestamps() {
        val document = LyricsParser.parse(
            raw = """
                [offset:+120]
                [00:01.20][00:02.300]First line
                [00:04.5]Second line
            """.trimIndent(),
            source = LyricsSource.SIDECAR,
        )

        requireNotNull(document)
        assertEquals(LyricsFormat.LRC, document.format)
        assertEquals(listOf(1_320L, 2_420L, 4_620L), document.lines.map { it.startTimeMs })
        assertEquals(2_420L, document.lines.first().endTimeMs)
        assertEquals("First line", document.lines[1].text)
    }

    @Test
    fun enhancedLrcUsesLineTimingAndRemovesWordMarkers() {
        val document = LyricsParser.parse(
            raw = "[00:03.00]<00:03.00>Hello <00:03.50>world",
            source = LyricsSource.EMBEDDED,
        )

        requireNotNull(document)
        assertEquals(3_000L, document.lines.single().startTimeMs)
        assertEquals("Hello world", document.lines.single().text)
    }

    @Test
    fun ttmlSupportsClockOffsetAndDurationTimes() {
        val document = LyricsParser.parse(
            raw = """
                <?xml version="1.0" encoding="UTF-8"?>
                <tt xmlns="http://www.w3.org/ns/ttml">
                  <body><div>
                    <p begin="00:00:01.250" end="00:00:02.500">First line</p>
                    <p begin="3s" dur="1.5s"><span>Second</span> line</p>
                  </div></body>
                </tt>
            """.trimIndent(),
            source = LyricsSource.SIDECAR,
            preferredFormat = LyricsFormat.TTML,
        )

        requireNotNull(document)
        assertEquals(LyricsFormat.TTML, document.format)
        assertEquals(listOf(1_250L, 3_000L), document.lines.map { it.startTimeMs })
        assertEquals(2_500L, document.lines[0].endTimeMs)
        assertEquals(4_500L, document.lines[1].endTimeMs)
        assertEquals("Second line", document.lines[1].text)
    }

    @Test
    fun untimedOrMalformedLyricsAreIgnored() {
        assertNull(
            LyricsParser.parse(
                raw = "plain lyrics without timestamps",
                source = LyricsSource.EMBEDDED,
            ),
        )
        assertNull(
            LyricsParser.parse(
                raw = "<tt><body><p begin=\"bad\">line</p>",
                source = LyricsSource.SIDECAR,
            ),
        )
    }

    @Test
    fun currentLineLookupTracksPlaybackTime() {
        val document = requireNotNull(
            LyricsParser.parse(
                raw = "[00:01]One\n[00:02]Two\n[00:03]Three",
                source = LyricsSource.SIDECAR,
            ),
        )

        assertEquals(-1, document.currentLineIndex(999L))
        assertEquals(0, document.currentLineIndex(1_500L))
        assertEquals(1, document.currentLineIndex(2_999L))
        assertEquals(2, document.currentLineIndex(30_000L))
    }
}
