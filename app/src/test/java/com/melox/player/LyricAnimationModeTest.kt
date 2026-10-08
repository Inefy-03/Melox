package com.melox.player

import com.melox.player.model.AppSettings
import com.melox.player.model.LyricAnimationMode
import com.melox.player.model.LyricLine
import com.melox.player.model.LyricWord
import com.melox.player.model.resolveLyricAnimationMode
import com.melox.player.ui.screen.playback.lyricRevealFloatOffset
import com.melox.player.ui.screen.playback.lyricWordEffects
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricAnimationModeTest {
    @Test
    fun mixedDocumentAppliesTheSelectedScopeToEveryOriginalLine() {
        val timedLines = listOf(true, false, true, false)
        val expectations = mapOf(
            LyricAnimationMode.CURRENT_LINE to listOf(true, false, true, false),
            LyricAnimationMode.EXTEND_TO_ALL to listOf(true, true, true, true),
            LyricAnimationMode.ALWAYS to listOf(true, true, true, true),
            LyricAnimationMode.NEVER to listOf(false, false, false, false),
        )
        expectations.forEach { (mode, expected) ->
            assertEquals(expected, timedLines.map { mode.usesWordAnimation(it, timedLines.any { it }) })
        }
    }

    @Test
    fun plainDocumentOnlyAnimatesWordsInAlwaysMode() {
        for (mode in LyricAnimationMode.entries) {
            assertEquals(mode == LyricAnimationMode.ALWAYS, mode.usesWordAnimation(false, false))
        }
    }

    @Test
    fun neverDisablesNativeTimedWordAnimationToo() {
        for (mode in LyricAnimationMode.entries) {
            assertEquals(mode != LyricAnimationMode.NEVER, mode.usesWordAnimation(true, true))
        }
    }

    @Test
    fun migrationPreservesBothLegacyStatesAndTheOriginalDefault() {
        assertEquals(LyricAnimationMode.CURRENT_LINE, AppSettings().lyricAnimationMode)
        assertEquals(LyricAnimationMode.CURRENT_LINE, resolveLyricAnimationMode(null, null))
        assertEquals(LyricAnimationMode.CURRENT_LINE, resolveLyricAnimationMode(null, false))
        assertEquals(LyricAnimationMode.ALWAYS, resolveLyricAnimationMode(null, true))
    }

    @Test
    fun storedModesOverrideLegacyValuesAndUnknownModesFallBackSafely() {
        for (mode in LyricAnimationMode.entries) {
            for (legacy in listOf(null, false, true)) {
                assertEquals(mode, resolveLyricAnimationMode(mode.name, legacy))
            }
        }
        assertEquals(LyricAnimationMode.CURRENT_LINE, resolveLyricAnimationMode("UNKNOWN", true))
    }

    @Test
    fun syntheticSpanPreservesTextAndTheWholeLineIntervalWithoutMutatingSource() {
        for ((start, end) in listOf(1_000L to 5_000L, 1_000L to 1_000L)) {
            for (text in listOf("These petals float", "Swimming\nthrough a holy soil", "中文歌词", "e\u0301 😀", "")) {
                val line = LyricLine("", start, end, text, emptyList(), "translation")
                assertEquals(listOf(LyricWord(start, end, text, false)), line.animationWords())
                assertTrue(line.words.isEmpty())
                assertEquals(text, line.displayText)
                assertEquals("translation", line.translation)
            }
        }
    }

    @Test
    fun nativeWordTimestampsAndSpacingAreKeptExactly() {
        val words = listOf(
            LyricWord(1_200L, 2_000L, "Swimming", true),
            LyricWord(2_100L, 4_500L, "through", false),
        )
        val line = LyricLine("", 1_000L, 5_000L, null, words, null)
        assertSame(words, line.animationWords())
    }

    @Test
    fun identicalAnimationSpansKeepNativeAndGeneratedEffectsDistinct() {
        for (text in listOf("中文", "日本語", "한글", "مرحبا", "hello")) {
            val generated = LyricLine("", 1_000L, 5_000L, text, emptyList(), null)
            val native = generated.copy(words = generated.animationWords())
            assertEquals(native.animationWords(), generated.animationWords())
            assertNotEquals(native, generated)
            for (mode in listOf(LyricAnimationMode.EXTEND_TO_ALL, LyricAnimationMode.ALWAYS)) {
                assertTrue(mode.usesWordAnimation(generated.words.isNotEmpty(), true))
                val generatedEffects = lyricWordEffects(text, 4_000L, generated.words.isNotEmpty())
                assertFalse(text, generatedEffects.scale)
                assertFalse(text, generatedEffects.glow)
                val nativeEffects = lyricWordEffects(text, 4_000L, native.words.isNotEmpty())
                assertTrue(text, nativeEffects.scale)
                assertTrue(text, nativeEffects.glow)
            }
            assertTrue(lyricRevealFloatOffset(0.5f) < 0f)
            assertTrue(generated.words.isEmpty())
        }
    }
}
