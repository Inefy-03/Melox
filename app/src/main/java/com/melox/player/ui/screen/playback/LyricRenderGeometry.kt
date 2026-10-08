package com.melox.player.ui.screen.playback

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.cos

internal fun lyricForegroundFadeWidthPx(fontSizePx: Float): Float = fontSizePx.coerceIn(1f, 36f)

// Preserve the previous 4 px lift at the common 24 sp / 3x density while scaling typography.
internal fun lyricEffectGeometryScale(fontSizePx: Float): Float = (fontSizePx / 72f).coerceAtLeast(0f)

/** Shaping/eligibility count graphemes; split timestamps retain the original UTF-16 timing units. */
internal fun lyricChunkTimeMs(startMs: Long, endMs: Long, sourceLength: Int, offset: Int): Long =
    if (sourceLength <= 0) startMs else startMs + (endMs - startMs) * offset / sourceLength

internal data class LyricFadeBounds(val startX: Float, val endX: Float)

internal fun Rect.lyricUnion(other: Rect): Rect = Rect(
    minOf(left, other.left), minOf(top, other.top),
    maxOf(right, other.right), maxOf(bottom, other.bottom),
)

/** A unit finishes rising exactly when the opaque edge has passed its shaped bounds. */
internal fun lyricGlyphRevealProgress(left: Float, right: Float, fade: LyricFadeBounds): Float {
    val width = (right - left).coerceAtLeast(0f)
    val fadeWidth = (fade.endX - fade.startX).coerceAtLeast(0f)
    if (width + fadeWidth <= 0f) return if (fade.startX >= right) 1f else 0f
    return ((fade.endX - left) / (width + fadeWidth)).coerceIn(0f, 1f)
}

internal fun lyricRevealFloatOffset(progress: Float): Float =
    -LYRIC_FLOAT_MAX_OFFSET_PX * (1f - cos(progress.coerceIn(0f, 1f) * Math.PI).toFloat()) / 2f

/** Include rest and maximum transformed coverage, independently of list layout spacing. */
internal fun lyricGlyphVisualBounds(bounds: Rect, pivot: Offset, maxScale: Float, liftPx: Float): Rect {
    val scale = maxScale.coerceAtLeast(1f)
    return Rect(
        minOf(bounds.left, pivot.x + (bounds.left - pivot.x) * scale),
        minOf(bounds.top, pivot.y + (bounds.top - pivot.y) * scale) - liftPx.coerceAtLeast(0f),
        maxOf(bounds.right, pivot.x + (bounds.right - pivot.x) * scale),
        maxOf(bounds.bottom, pivot.y + (bounds.bottom - pivot.y) * scale),
    )
}

/** Match the original clipped stops, including the shrinking fade at the row's first/last pixels. */
internal fun lyricFadeBounds(minX: Float, maxX: Float, pixelPosition: Float, fadeWidthPx: Float): LyricFadeBounds {
    val width = maxX - minX
    if (width <= 0f) return LyricFadeBounds(minX, minX)
    val fadeWidth = fadeWidthPx.coerceIn(0f, width)
    val progress = ((pixelPosition - minX) / width).coerceIn(0f, 1f)
    val end = (width + fadeWidth) * progress
    return LyricFadeBounds(minX + (end - fadeWidth).coerceIn(0f, width),
        minX + end.coerceIn(0f, width))
}

internal data class LyricWordEffects(val glow: Boolean, val scale: Boolean)

internal fun lyricWordEffects(
    content: String,
    durationMs: Long,
    hasNativeWordTiming: Boolean = true,
): LyricWordEffects {
    if (!hasNativeWordTiming) return LyricWordEffects(false, false)
    val visible = lyricGraphemeRanges(content).map { content.substring(it) }.filter { grapheme ->
        grapheme.codePoints().anyMatch { code ->
            !Character.isWhitespace(code) && Character.getType(code) !in punctuationTypes
        }
    }
    if (visible.isEmpty() || durationMs < 1_000L) return LyricWordEffects(false, false)
    val cjk = visible.all { grapheme ->
        grapheme.codePoints().allMatch { code ->
            code in 0x3400..0x4DBF || code in 0x4E00..0x9FFF ||
                code in 0xF900..0xFAFF || code in 0x20000..0x323AF ||
                Character.getType(code) == Character.NON_SPACING_MARK.toInt()
        }
    }
    val eligible = cjk || durationMs.toDouble() / visible.size > 200.0
    return LyricWordEffects(glow = eligible, scale = eligible)
}

internal fun lyricUsesJoinedGlyphs(content: String): Boolean = content.codePoints().anyMatch { code ->
    Character.getDirectionality(code) == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
        Character.getDirectionality(code) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC ||
        Character.UnicodeScript.of(code) in joinedScripts
}

private val joinedScripts = setOf(
    Character.UnicodeScript.ARABIC, Character.UnicodeScript.SYRIAC,
    Character.UnicodeScript.DEVANAGARI, Character.UnicodeScript.BENGALI,
    Character.UnicodeScript.GURMUKHI, Character.UnicodeScript.GUJARATI,
    Character.UnicodeScript.ORIYA, Character.UnicodeScript.TAMIL,
    Character.UnicodeScript.TELUGU, Character.UnicodeScript.KANNADA,
    Character.UnicodeScript.MALAYALAM, Character.UnicodeScript.SINHALA,
    Character.UnicodeScript.THAI, Character.UnicodeScript.LAO,
    Character.UnicodeScript.MYANMAR, Character.UnicodeScript.KHMER,
    Character.UnicodeScript.TIBETAN,
)

private val punctuationTypes = setOf(
    Character.CONNECTOR_PUNCTUATION.toInt(), Character.DASH_PUNCTUATION.toInt(),
    Character.START_PUNCTUATION.toInt(), Character.END_PUNCTUATION.toInt(),
    Character.INITIAL_QUOTE_PUNCTUATION.toInt(), Character.FINAL_QUOTE_PUNCTUATION.toInt(),
    Character.OTHER_PUNCTUATION.toInt(),
)

/** Forward playback advances once per word; backward seeks find the first possible live interval. */
internal class LyricTimingCursor(private val starts: LongArray, private val ends: LongArray) {
    private val prefixEnds = LongArray(ends.size)
    private var index = 0
    private var previousPosition = Long.MIN_VALUE

    init {
        require(starts.size == ends.size)
        ends.indices.forEach { prefixEnds[it] = maxOf(ends[it], prefixEnds.getOrElse(it - 1) { Long.MIN_VALUE }) }
    }

    fun indexAt(positionMs: Long): Int {
        if (positionMs < previousPosition) {
            var low = 0
            var high = ends.size
            while (low < high) {
                val middle = (low + high) ushr 1
                if (prefixEnds[middle] <= positionMs) low = middle + 1 else high = middle
            }
            index = low
        }
        while (index < ends.size && prefixEnds[index] <= positionMs) index++
        previousPosition = positionMs
        var active = index
        while (active < ends.size && starts[active] <= positionMs) {
            if (positionMs < ends[active]) return active
            active++
        }
        return index.coerceAtMost(ends.size)
    }
}
