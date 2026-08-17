package com.melox.player.model

enum class LyricsFormat {
    LRC,
    TTML,
}

enum class LyricsSource {
    EMBEDDED,
    SIDECAR,
}

data class LyricWord(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
    val hasTrailingSpace: Boolean,
)

data class LyricLine(
    val agent: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String?,
    val words: List<LyricWord>,
    val translation: String?,
) {
    val displayText: String
        get() = text ?: words.joinToString(separator = "") { word ->
            word.text + if (word.hasTrailingSpace) " " else ""
        }.trimEnd()

    fun revealProgress(positionMs: Long, forceWordByWord: Boolean): Float {
        if (positionMs < startTimeMs) return 0f
        if (words.isEmpty()) {
            if (!forceWordByWord) return if (positionMs >= startTimeMs) 1f else 0f
            return intervalProgress(positionMs, startTimeMs, endTimeMs)
        }

        val totalCharacters = words.sumOf { word ->
            word.text.length + if (word.hasTrailingSpace) 1 else 0
        }.coerceAtLeast(1)
        var revealedCharacters = 0f
        for (word in words) {
            val wordCharacters = word.text.length + if (word.hasTrailingSpace) 1 else 0
            when {
                positionMs >= word.endTimeMs -> revealedCharacters += wordCharacters
                positionMs > word.startTimeMs -> {
                    revealedCharacters += wordCharacters * intervalProgress(
                        positionMs = positionMs,
                        startTimeMs = word.startTimeMs,
                        endTimeMs = word.endTimeMs,
                    )
                    break
                }
                else -> break
            }
        }
        return (revealedCharacters / totalCharacters).coerceIn(0f, 1f)
    }
}

data class LyricTransition(
    val afterLineIndex: Int,
    val startTimeMs: Long,
    val endTimeMs: Long,
) {
    val durationMs: Long
        get() = (endTimeMs - startTimeMs).coerceAtLeast(0L)

    fun progress(positionMs: Long): Float {
        val duration = durationMs
        if (duration <= 0L) return if (positionMs >= endTimeMs) 1f else 0f
        return ((positionMs - startTimeMs).toFloat() / duration).coerceIn(0f, 1f)
    }

    fun isActive(positionMs: Long): Boolean =
        positionMs >= startTimeMs && positionMs < endTimeMs
}

sealed interface LyricsRenderItem {
    data class Line(
        val lineIndex: Int,
        val line: LyricLine,
    ) : LyricsRenderItem

    data class Transition(
        val transitionIndex: Int,
        val transition: LyricTransition,
    ) : LyricsRenderItem
}

data class LyricsDocument(
    val lines: List<LyricLine>,
    val format: LyricsFormat,
    val source: LyricsSource,
    val transitions: List<LyricTransition> = emptyList(),
) {
    fun renderItems(): List<LyricsRenderItem> {
        if (transitions.isEmpty()) {
            return lines.mapIndexed(LyricsRenderItem::Line)
        }
        val transitionsByAfterLine = transitions
            .withIndex()
            .associateBy { indexedTransition -> indexedTransition.value.afterLineIndex }
        return buildList(lines.size + transitions.size) {
            transitionsByAfterLine[-1]?.let { indexedTransition ->
                add(
                    LyricsRenderItem.Transition(
                        transitionIndex = indexedTransition.index,
                        transition = indexedTransition.value,
                    ),
                )
            }
            lines.forEachIndexed { lineIndex, line ->
                add(LyricsRenderItem.Line(lineIndex, line))
                transitionsByAfterLine[lineIndex]?.let { indexedTransition ->
                    add(
                        LyricsRenderItem.Transition(
                            transitionIndex = indexedTransition.index,
                            transition = indexedTransition.value,
                        ),
                    )
                }
            }
        }
    }

    fun transitionIndex(positionMs: Long): Int {
        var low = 0
        var high = transitions.lastIndex
        while (low <= high) {
            val middle = (low + high).ushr(1)
            if (transitions[middle].startTimeMs <= positionMs) {
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        return high.takeIf { candidate ->
            candidate >= 0 && transitions[candidate].isActive(positionMs)
        } ?: -1
    }

    fun currentLineIndex(positionMs: Long): Int {
        if (lines.isEmpty() || positionMs < lines.first().startTimeMs) return -1
        var low = 0
        var high = lines.lastIndex
        while (low <= high) {
            val middle = (low + high).ushr(1)
            if (lines[middle].startTimeMs <= positionMs) {
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        val candidate = high
        if (candidate < 0) return -1
        val line = lines[candidate]
        return candidate.takeIf {
            positionMs < line.endTimeMs ||
                (candidate == lines.lastIndex && line.endTimeMs <= line.startTimeMs)
        } ?: -1
    }

    fun focusLineIndex(positionMs: Long): Int {
        currentLineIndex(positionMs).takeIf { it >= 0 }?.let { return it }
        if (lines.isEmpty()) return -1

        var low = 0
        var high = lines.lastIndex
        while (low <= high) {
            val middle = (low + high).ushr(1)
            if (lines[middle].startTimeMs <= positionMs) {
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        return low.coerceAtMost(lines.lastIndex)
    }
}

sealed interface LyricsUiState {
    data object Loading : LyricsUiState

    data object Unavailable : LyricsUiState

    data class Available(
        val document: LyricsDocument,
    ) : LyricsUiState
}

private fun intervalProgress(
    positionMs: Long,
    startTimeMs: Long,
    endTimeMs: Long,
): Float {
    val durationMs = endTimeMs - startTimeMs
    if (durationMs <= 0L) return if (positionMs >= startTimeMs) 1f else 0f
    return ((positionMs - startTimeMs).toFloat() / durationMs).coerceIn(0f, 1f)
}
