package com.melox.player.model

enum class LyricsFormat {
    LRC,
    TTML,
}

enum class LyricsSource {
    SIDECAR,
    EMBEDDED,
}

data class TimedWord(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
)

data class TimedLyricLine(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
    val words: List<TimedWord>? = null,
)

data class LyricsDocument(
    val lines: List<TimedLyricLine>,
    val format: LyricsFormat,
    val source: LyricsSource,
) {
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
        return high
    }

    fun currentWordIndex(lineIndex: Int, positionMs: Long): Int {
        val words = lines.getOrNull(lineIndex)?.words ?: return -1
        if (words.isEmpty() || positionMs < words.first().startTimeMs) return -1
        var low = 0
        var high = words.lastIndex
        while (low <= high) {
            val middle = (low + high).ushr(1)
            if (words[middle].startTimeMs <= positionMs) {
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        return high
    }

    fun wordProgress(lineIndex: Int, positionMs: Long): Float {
        val words = lines.getOrNull(lineIndex)?.words ?: return -1f
        val wordIndex = currentWordIndex(lineIndex, positionMs)
        if (wordIndex < 0) return -1f
        val word = words[wordIndex]
        val duration = word.endTimeMs - word.startTimeMs
        if (duration <= 0) return 1f
        val elapsed = positionMs - word.startTimeMs
        val progress = wordIndex.toFloat() + (elapsed.toFloat() / duration).coerceIn(0f, 1f)
        return progress / words.size
    }

    fun lineProgress(positionMs: Long): Float {
        if (lines.isEmpty()) return 0f
        val lineIndex = currentLineIndex(positionMs)
        if (lineIndex < 0) return 0f
        val line = lines[lineIndex]
        val duration = line.endTimeMs - line.startTimeMs
        if (duration <= 0) return 0f
        return ((positionMs - line.startTimeMs).toFloat() / duration).coerceIn(0f, 1f)
    }
}

sealed interface LyricsUiState {
    data object Loading : LyricsUiState

    data object Unavailable : LyricsUiState

    data class Available(
        val document: LyricsDocument,
    ) : LyricsUiState
}
