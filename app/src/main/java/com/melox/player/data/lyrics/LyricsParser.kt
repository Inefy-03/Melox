package com.melox.player.data.lyrics

import com.melox.player.model.LyricsDocument
import com.melox.player.model.LyricsFormat
import com.melox.player.model.LyricsSource
import com.melox.player.model.TimedLyricLine
import com.melox.player.model.TimedWord
import java.io.StringReader
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

internal object LyricsParser {
    private data class RawLine(val timeMs: Long, val text: String)

    private val lrcTimestamp = Regex(
        """\[(\d{1,3}):([0-5]?\d)(?:[.:](\d{1,3}))?]""",
    )
    private val lrcOffset = Regex(
        """(?i)^\s*\[offset:\s*([+-]?\d+)\s*]\s*$""",
    )
    private val enhancedTimestamp = Regex(
        """<\d{1,3}:[0-5]?\d(?:[.:]\d{1,3})?>""",
    )

    fun parse(
        raw: String,
        source: LyricsSource,
        preferredFormat: LyricsFormat? = null,
    ): LyricsDocument? {
        val bounded = raw
            .replace("\u0000", "")
            .take(MAX_LYRICS_CHARS)
            .trim()
        if (bounded.isEmpty()) return null
        val looksLikeTtml = preferredFormat == LyricsFormat.TTML ||
            bounded.startsWith("<?xml", ignoreCase = true) ||
            Regex("""(?is)<(?:\w+:)?tt(?:\s|>)""").containsMatchIn(bounded)
        return if (looksLikeTtml) {
            parseTtml(bounded, source) ?: parseLrc(bounded, source)
        } else {
            parseLrc(bounded, source) ?: parseTtml(bounded, source)
        }
    }

    private val enhancedWordTag = Regex(
        """<(\d{1,3}):([0-5]?\d)(?:[.:](\d{1,3}))?>\s*([^<]*)""",
    )

    private fun parseLrc(
        raw: String,
        source: LyricsSource,
    ): LyricsDocument? {
        var offsetMs = 0L
        val rawLines = mutableListOf<RawLine>()
        raw.lineSequence()
            .take(MAX_LYRIC_LINES)
            .forEach { line ->
                lrcOffset.matchEntire(line)?.let { match ->
                    offsetMs = match.groupValues[1].toLongOrNull()?.coerceIn(
                        -MAX_OFFSET_MS,
                        MAX_OFFSET_MS,
                    ) ?: 0L
                    return@forEach
                }
                val matches = lrcTimestamp.findAll(line).toList()
                if (matches.isEmpty()) return@forEach
                val text = line
                    .substring(matches.last().range.last + 1)
                    .trim()
                if (text.isEmpty()) return@forEach
                matches.forEach { match ->
                    val minutes = match.groupValues[1].toLongOrNull() ?: return@forEach
                    val seconds = match.groupValues[2].toLongOrNull() ?: return@forEach
                    val fraction = fractionToMilliseconds(match.groupValues[3])
                    rawLines += RawLine(
                        timeMs = minutes * 60_000L + seconds * 1_000L + fraction,
                        text = text,
                    )
                }
            }
        val normalized = rawLines.map { line ->
            line.copy(timeMs = (line.timeMs + offsetMs).coerceAtLeast(0L))
        }
        return buildDocument(normalized, LyricsFormat.LRC, source)
    }

    private fun parseTtml(
        raw: String,
        source: LyricsSource,
    ): LyricsDocument? {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isExpandEntityReferences = false
            setFeatureSafely("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeatureSafely("http://xml.org/sax/features/external-general-entities", false)
            setFeatureSafely("http://xml.org/sax/features/external-parameter-entities", false)
            setAttributeSafely("http://javax.xml.XMLConstants/property/accessExternalDTD", "")
            setAttributeSafely("http://javax.xml.XMLConstants/property/accessExternalSchema", "")
        }
        val document = runCatching {
            factory.newDocumentBuilder().parse(InputSource(StringReader(raw)))
        }.getOrNull() ?: return null
        val paragraphs = document.getElementsByTagNameNS("*", "p")
        val lines = buildList {
            for (index in 0 until minOf(paragraphs.length, MAX_LYRIC_LINES)) {
                val paragraph = paragraphs.item(index)
                val attributes = paragraph.attributes ?: continue
                val begin = attributes.getNamedItem("begin")?.nodeValue
                    ?.let(::parseTtmlTimeMs)
                    ?: continue
                val explicitEnd = attributes.getNamedItem("end")?.nodeValue
                    ?.let(::parseTtmlTimeMs)
                val duration = attributes.getNamedItem("dur")?.nodeValue
                    ?.let(::parseTtmlTimeMs)
                val end = explicitEnd ?: duration?.let { begin + it }
                val text = paragraph.textContent
                    .orEmpty()
                    .replace(Regex("""\s+"""), " ")
                    .trim()
                if (text.isNotEmpty()) {
                    add(Triple(begin.coerceAtLeast(0L), end, text))
                }
            }
        }
        if (lines.isEmpty()) return null
        val grouped = lines
            .sortedBy { it.first }
            .groupBy { it.first }
            .map { (start, entries) ->
                Triple(
                    start,
                    entries.mapNotNull { it.second }.maxOrNull(),
                    entries.map { it.third }.distinct().joinToString("\n"),
                )
            }
        val timedLines = grouped.mapIndexed { index, (start, explicitEnd, text) ->
            val nextStart = grouped.getOrNull(index + 1)?.first ?: Long.MAX_VALUE
            TimedLyricLine(
                startTimeMs = start,
                endTimeMs = explicitEnd
                    ?.coerceAtLeast(start)
                    ?.coerceAtMost(nextStart)
                    ?: nextStart,
                text = text,
            )
        }
        return LyricsDocument(timedLines, LyricsFormat.TTML, source)
    }

    private fun buildDocument(
        rawLines: List<RawLine>,
        format: LyricsFormat,
        source: LyricsSource,
    ): LyricsDocument? {
        if (rawLines.isEmpty()) return null
        data class GroupedLine(
            val timeMs: Long,
            val text: String,
            val words: List<TimedWord>?,
        )
        val grouped = rawLines
            .sortedBy(RawLine::timeMs)
            .groupBy(RawLine::timeMs)
            .map { (start, entries) ->
                val wordTiming = if (entries.size == 1) {
                    parseWordTiming(entries.first())
                } else {
                    null
                }
                val text = if (wordTiming != null) {
                    wordTiming.plainText
                } else {
                    entries.map(RawLine::text)
                        .distinct()
                        .joinToString("\n")
                }
                GroupedLine(start, text, wordTiming?.words)
            }
        val lines = grouped.mapIndexed { index, line ->
            TimedLyricLine(
                startTimeMs = line.timeMs,
                endTimeMs = grouped.getOrNull(index + 1)?.timeMs ?: Long.MAX_VALUE,
                text = line.text,
                words = line.words,
            )
        }
        return LyricsDocument(lines, format, source)
    }

    private data class WordTimingResult(
        val words: List<TimedWord>,
        val plainText: String,
    )

    private fun parseWordTiming(line: RawLine): WordTimingResult? {
        val matches = enhancedWordTag.findAll(line.text).toList()
        if (matches.isEmpty()) return null
        val plainText = line.text.replace(enhancedTimestamp, "").trim()
        if (plainText.isEmpty()) return null
        val words = buildList {
            var lastEnd = line.timeMs
            matches.forEachIndexed { index, match ->
                val minutes = match.groupValues[1].toLongOrNull() ?: return@forEachIndexed
                val seconds = match.groupValues[2].toLongOrNull() ?: return@forEachIndexed
                val fraction = fractionToMilliseconds(match.groupValues[3])
                val wordTimeMs = minutes * 60_000L + seconds * 1_000L + fraction
                val wordText = match.groupValues[4].trim()
                if (wordText.isEmpty()) return@forEachIndexed
                if (index == 0 && wordTimeMs < lastEnd) {
                    // Allow the first word tag to shift the start earlier (grace handling)
                }
                val wordStart = wordTimeMs.coerceAtLeast(lastEnd)
                val isLast = index == matches.lastIndex
                val nextWordTime = if (!isLast) {
                    val nextMatch = matches[index + 1]
                    val nM = nextMatch.groupValues[1].toLongOrNull() ?: return@forEachIndexed
                    val nS = nextMatch.groupValues[2].toLongOrNull() ?: return@forEachIndexed
                    val nF = fractionToMilliseconds(nextMatch.groupValues[3])
                    nM * 60_000L + nS * 1_000L + nF
                } else {
                    line.timeMs + 3000L
                }
                val wordEnd = minOf(nextWordTime, line.timeMs + 8000L)
                    .coerceAtLeast(wordStart + 50L)
                add(TimedWord(startTimeMs = wordStart, endTimeMs = wordEnd, text = wordText))
                lastEnd = wordEnd
            }
        }
        return if (words.isNotEmpty()) WordTimingResult(words, plainText) else null
    }

    private fun fractionToMilliseconds(value: String): Long = when (value.length) {
        0 -> 0L
        1 -> value.toLongOrNull()?.times(100L) ?: 0L
        2 -> value.toLongOrNull()?.times(10L) ?: 0L
        else -> value.take(3).toLongOrNull() ?: 0L
    }

    private fun parseTtmlTimeMs(raw: String): Long? {
        val value = raw.trim().lowercase(Locale.ROOT)
        Regex("""^(\d+(?:\.\d+)?)(ms|s|m|h)$""")
            .matchEntire(value)
            ?.let { match ->
                val amount = match.groupValues[1].toDoubleOrNull() ?: return null
                val multiplier = when (match.groupValues[2]) {
                    "ms" -> 1.0
                    "s" -> 1_000.0
                    "m" -> 60_000.0
                    "h" -> 3_600_000.0
                    else -> return null
                }
                return (amount * multiplier).toLong().coerceAtLeast(0L)
            }
        val parts = value.split(':')
        if (parts.size !in 2..4) return null
        val hours: Long
        val minutes: Long
        val seconds: Double
        when (parts.size) {
            2 -> {
                hours = 0L
                minutes = parts[0].toLongOrNull() ?: return null
                seconds = parts[1].toDoubleOrNull() ?: return null
            }
            3 -> {
                hours = parts[0].toLongOrNull() ?: return null
                minutes = parts[1].toLongOrNull() ?: return null
                seconds = parts[2].toDoubleOrNull() ?: return null
            }
            else -> {
                hours = parts[0].toLongOrNull() ?: return null
                minutes = parts[1].toLongOrNull() ?: return null
                val wholeSeconds = parts[2].toLongOrNull() ?: return null
                val frames = parts[3].toLongOrNull() ?: return null
                seconds = wholeSeconds + frames / DEFAULT_FRAME_RATE
            }
        }
        if (minutes < 0L || seconds < 0.0) return null
        return (hours * 3_600_000L + minutes * 60_000L + seconds * 1_000.0)
            .toLong()
            .coerceAtLeast(0L)
    }

    private fun DocumentBuilderFactory.setFeatureSafely(
        name: String,
        value: Boolean,
    ) {
        runCatching { setFeature(name, value) }
    }

    private fun DocumentBuilderFactory.setAttributeSafely(
        name: String,
        value: String,
    ) {
        runCatching { setAttribute(name, value) }
    }

    private const val MAX_LYRICS_CHARS = 2 * 1024 * 1024
    private const val MAX_LYRIC_LINES = 10_000
    private const val MAX_OFFSET_MS = 10 * 60 * 1_000L
    private const val DEFAULT_FRAME_RATE = 30.0
}
