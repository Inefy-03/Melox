package com.melox.player.ui.screen.playback

import java.text.BreakIterator
import java.util.Locale

/** Splits only oversized spans, preserving natural line breaks and grapheme boundaries. */
internal fun lyricTextChunks(
    text: String,
    availableWidthPx: Float,
    measureWidth: (String) -> Float,
): List<IntRange> {
    if (text.isEmpty()) return emptyList()
    if ('\n' in text) {
        val result = mutableListOf<IntRange>()
        var start = 0
        text.forEachIndexed { index, character ->
            if (character == '\n') {
                result += lyricTextChunks(text.substring(start, index), availableWidthPx, measureWidth)
                    .map { (it.first + start)..(it.last + start) }
                result += index..index
                start = index + 1
            }
        }
        result += lyricTextChunks(text.substring(start), availableWidthPx, measureWidth)
            .map { (it.first + start)..(it.last + start) }
        return result
    }
    val width = availableWidthPx.coerceAtLeast(1f)
    if (measureWidth(text) <= width) return listOf(text.indices)
    val characters = listOf(0) + lyricGraphemeRanges(text).map { it.last + 1 }
    val lineBreaks = textBoundaries(text, BreakIterator.getLineInstance(Locale.ROOT)).toSet()
    val result = mutableListOf<IntRange>()
    var startIndex = 0
    while (startIndex < characters.lastIndex) {
        var endIndex = startIndex + 1
        var naturalEndIndex = -1
        while (endIndex < characters.lastIndex) {
            if (characters[endIndex] in lineBreaks) naturalEndIndex = endIndex
            if (measureWidth(text.substring(characters[startIndex], characters[endIndex + 1])) > width) {
                break
            }
            endIndex++
        }
        if (endIndex < characters.lastIndex && naturalEndIndex > startIndex) {
            endIndex = naturalEndIndex
        }
        // Trailing whitespace has no ink and is trimmed at the visual line end.
        // Keep it with the preceding text rather than creating an empty wrapped row.
        while (
            endIndex < characters.lastIndex &&
                text.substring(characters[endIndex], characters[endIndex + 1]).isBlank()
        ) {
            endIndex++
        }
        result += characters[startIndex] until characters[endIndex]
        startIndex = endIndex
    }
    return result
}

/** Fills each row before wrapping; overlong groups can break at measured span boundaries. */
internal fun lyricWrapRanges(
    contents: List<String>,
    widths: List<Float>,
    availableWidthPx: Float,
    lineEndWidths: List<Float> = widths,
): List<IntRange> {
    require(contents.size == widths.size && contents.size == lineEndWidths.size)
    if (contents.isEmpty()) return emptyList()
    val width = availableWidthPx.coerceAtLeast(1f)
    val boundaries = textBoundaries(
        contents.joinToString(""),
        BreakIterator.getLineInstance(Locale.ROOT),
    ).toSet()
    val allowed = BooleanArray(contents.size + 1)
    allowed[0] = true
    allowed[contents.size] = true
    var offset = 0
    contents.forEachIndexed { index, content ->
        offset += content.length
        if (offset in boundaries || content.lastOrNull()?.isWhitespace() == true) {
            allowed[index + 1] = true
        }
    }
    var groupStart = 0
    var groupWidth = 0f
    widths.forEachIndexed { index, itemWidth ->
        groupWidth += itemWidth
        if (allowed[index + 1]) {
            if (groupWidth - itemWidth + lineEndWidths[index] > width) {
                for (boundary in groupStart + 1..index) allowed[boundary] = true
            }
            groupStart = index + 1
            groupWidth = 0f
        }
    }
    val result = mutableListOf<IntRange>()
    var start = 0
    while (start < contents.size) {
        var advanceWidth = 0f
        var fittingEnd = start
        for (end in start + 1..contents.size) {
            val index = end - 1
            val visibleWidth = advanceWidth + lineEndWidths[index]
            if (visibleWidth > width) break
            advanceWidth += widths[index]
            if (allowed[end]) fittingEnd = end
            if ('\n' in contents[index]) {
                fittingEnd = end
                break
            }
        }
        // A single oversized grapheme still occupies one row and makes progress.
        if (fittingEnd == start) fittingEnd = start + 1
        result += start until fittingEnd
        start = fittingEnd
    }
    return result
}

private fun textBoundaries(text: String, iterator: BreakIterator): List<Int> {
    iterator.setText(text)
    return buildList {
        var boundary = iterator.first()
        while (boundary != BreakIterator.DONE) {
            add(boundary)
            boundary = iterator.next()
        }
    }
}

/** Extend platform character boundaries for emoji sequences on older Android/JVM runtimes. */
internal fun lyricGraphemeRanges(text: String): List<IntRange> {
    if (text.isEmpty()) return emptyList()
    val boundaries = textBoundaries(text, BreakIterator.getCharacterInstance(Locale.ROOT))
    val retained = mutableListOf(0)
    for (boundary in boundaries.drop(1).dropLast(1)) {
        val previous = text.codePointBefore(boundary)
        val next = text.codePointAt(boundary)
        val nextType = Character.getType(next)
        var regionalCount = 0
        var regionalOffset = boundary
        while (regionalOffset > 0 && text.codePointBefore(regionalOffset) in 0x1F1E6..0x1F1FF) {
            regionalCount++
            regionalOffset -= Character.charCount(text.codePointBefore(regionalOffset))
        }
        val joins = previous == 0x200D || next == 0x200D ||
            next in 0xFE00..0xFE0F || next in 0xE0100..0xE01EF ||
            next in 0x1F3FB..0x1F3FF || next in 0xE0020..0xE007F ||
            nextType == Character.NON_SPACING_MARK.toInt() ||
            nextType == Character.COMBINING_SPACING_MARK.toInt() ||
            (previous in 0x1F1E6..0x1F1FF && next in 0x1F1E6..0x1F1FF && regionalCount % 2 == 1)
        if (!joins) retained += boundary
    }
    retained += text.length
    return retained.zipWithNext { start, end -> start until end }
}
