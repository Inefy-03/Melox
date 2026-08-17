package com.melox.player.data.lyrics

import com.melox.player.model.LyricLine
import com.melox.player.model.LyricTransition
import com.melox.player.model.LyricWord
import com.melox.player.model.LyricsDocument
import com.melox.player.model.LyricsFormat
import com.melox.player.model.LyricsSource
import java.io.StringReader
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource

internal object LyricsParser {
    private data class RawWord(
        val startTimeMs: Long,
        val explicitEndTimeMs: Long?,
        val text: String,
        val hasTrailingSpace: Boolean,
    )

    private data class RawLine(
        val agent: String,
        val startTimeMs: Long,
        val explicitEndTimeMs: Long?,
        val text: String?,
        val words: List<RawWord>,
        val translation: String?,
    )

    private val lrcTimestamp = Regex(
        """\[(\d{1,3}):([0-5]?\d)(?:[.:](\d{1,3}))?]""",
    )
    private val lrcOffset = Regex(
        """(?i)^\s*\[offset:\s*([+-]?\d+)\s*]\s*$""",
    )
    private val enhancedWordTimestamp = Regex(
        """<(\d{1,3}):([0-5]?\d)(?:[.:](\d{1,3}))?>""",
    )

    fun parse(
        raw: String,
        source: LyricsSource,
        preferredFormat: LyricsFormat? = null,
        durationMs: Long = 0L,
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
            parseTtml(bounded, source, durationMs) ?: parseLrc(bounded, source, durationMs)
        } else {
            parseLrc(bounded, source, durationMs) ?: parseTtml(bounded, source, durationMs)
        }
    }

    private fun parseLrc(
        raw: String,
        source: LyricsSource,
        durationMs: Long,
    ): LyricsDocument? {
        var offsetMs = 0L
        val entries = mutableListOf<RawLine>()
        raw.lineSequence()
            .take(MAX_LYRIC_LINES)
            .forEach { line ->
                lrcOffset.matchEntire(line)?.let { match ->
                    offsetMs = match.groupValues[1].toLongOrNull()
                        ?.coerceIn(-MAX_OFFSET_MS, MAX_OFFSET_MS)
                        ?: 0L
                    return@forEach
                }
                val timestamps = lrcTimestamp.findAll(line).toList()
                if (timestamps.isEmpty()) return@forEach
                val payload = line.substring(timestamps.last().range.last + 1)
                if (payload.isBlank()) return@forEach
                timestamps.forEach { timestamp ->
                    val startTimeMs = timestamp.toTimeMs() ?: return@forEach
                    val words = parseEnhancedWords(payload, startTimeMs)
                    entries += RawLine(
                        agent = DEFAULT_AGENT,
                        startTimeMs = (startTimeMs + offsetMs).coerceAtLeast(0L),
                        explicitEndTimeMs = null,
                        text = payload
                            .replace(enhancedWordTimestamp, "")
                            .trim()
                            .takeIf(String::isNotEmpty),
                        words = words.map { word ->
                            word.copy(
                                startTimeMs = (word.startTimeMs + offsetMs).coerceAtLeast(0L),
                            )
                        },
                        translation = null,
                    )
                }
            }
        if (entries.isEmpty()) return null

        val grouped = entries
            .sortedBy(RawLine::startTimeMs)
            .groupBy(RawLine::startTimeMs)
            .map { (_, sameTimeLines) ->
                val primary = sameTimeLines.first()
                primary.copy(
                    translation = sameTimeLines
                        .drop(1)
                        .mapNotNull(RawLine::text)
                        .distinct()
                        .joinToString("\n")
                        .takeIf(String::isNotBlank),
                )
            }
        return buildDocument(grouped, LyricsFormat.LRC, source, durationMs)
    }

    private fun parseEnhancedWords(payload: String, lineStartTimeMs: Long): List<RawWord> {
        val timestamps = enhancedWordTimestamp.findAll(payload).toList()
        if (timestamps.isEmpty()) return emptyList()
        return buildList {
            val prefix = payload.substring(0, timestamps.first().range.first)
            prefix.toRawWord(lineStartTimeMs)?.let(::add)
            timestamps.forEachIndexed { index, timestamp ->
                val wordStart = timestamp.toTimeMs() ?: return@forEachIndexed
                val textStart = timestamp.range.last + 1
                val textEnd = timestamps.getOrNull(index + 1)?.range?.first ?: payload.length
                payload.substring(textStart, textEnd).toRawWord(wordStart)?.let(::add)
            }
        }
    }

    private fun String.toRawWord(startTimeMs: Long): RawWord? {
        val visible = trim()
        if (visible.isEmpty()) return null
        return RawWord(
            startTimeMs = startTimeMs,
            explicitEndTimeMs = null,
            text = visible,
            hasTrailingSpace = lastOrNull()?.isWhitespace() == true,
        )
    }

    private fun parseTtml(
        raw: String,
        source: LyricsSource,
        durationMs: Long,
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
        val entries = buildList {
            for (index in 0 until minOf(paragraphs.length, MAX_LYRIC_LINES)) {
                val paragraph = paragraphs.item(index) as? Element ?: continue
                parseTtmlParagraph(paragraph)?.let(::add)
            }
        }.sortedBy(RawLine::startTimeMs)
        return buildDocument(entries, LyricsFormat.TTML, source, durationMs)
    }

    private fun parseTtmlParagraph(paragraph: Element): RawLine? {
        val startTimeMs = paragraph.attributeValue("begin")
            ?.let(::parseTtmlTimeMs)
            ?: return null
        val endTimeMs = paragraph.attributeValue("end")?.let(::parseTtmlTimeMs)
            ?: paragraph.attributeValue("dur")?.let(::parseTtmlTimeMs)?.let(startTimeMs::plus)
        val agent = paragraph.attributeValue("agent")
            ?.takeIf(String::isNotBlank)
            ?: DEFAULT_AGENT
        val spans = paragraph.getElementsByTagNameNS("*", "span")
        var translation: String? = null
        val rawWords = buildList {
            for (index in 0 until spans.length) {
                val span = spans.item(index) as? Element ?: continue
                val role = span.attributeValue("role")?.lowercase(Locale.ROOT)
                when (role) {
                    "x-translation" -> {
                        translation = span.textContent.normalizeVisibleText()
                            .takeIf(String::isNotEmpty)
                    }
                    "x-bg", "x-roman" -> Unit
                    else -> {
                        val wordStart = span.attributeValue("begin")
                            ?.let(::parseTtmlTimeMs)
                            ?: continue
                        val wordEnd = span.attributeValue("end")
                            ?.let(::parseTtmlTimeMs)
                            ?: span.attributeValue("dur")
                                ?.let(::parseTtmlTimeMs)
                                ?.let(wordStart::plus)
                        val visible = span.textContent.normalizeVisibleText()
                        if (visible.isNotEmpty()) {
                            add(
                                RawWord(
                                    startTimeMs = wordStart,
                                    explicitEndTimeMs = wordEnd,
                                    text = visible,
                                    hasTrailingSpace = span.nextSibling
                                        ?.takeIf { it.nodeType == Node.TEXT_NODE }
                                        ?.nodeValue
                                        ?.any(Char::isWhitespace) == true,
                                ),
                            )
                        }
                    }
                }
            }
        }
        val text = if (rawWords.isEmpty()) {
            paragraph.textContent
                .normalizeVisibleText()
                .removeSuffix(translation.orEmpty())
                .trim()
                .takeIf(String::isNotEmpty)
        } else {
            null
        }
        if (text == null && rawWords.isEmpty()) return null
        return RawLine(
            agent = agent,
            startTimeMs = startTimeMs.coerceAtLeast(0L),
            explicitEndTimeMs = endTimeMs?.coerceAtLeast(startTimeMs),
            text = text,
            words = rawWords,
            translation = translation,
        )
    }

    private fun buildDocument(
        rawLines: List<RawLine>,
        format: LyricsFormat,
        source: LyricsSource,
        durationMs: Long,
    ): LyricsDocument? {
        if (rawLines.isEmpty()) return null
        val sorted = rawLines.sortedBy(RawLine::startTimeMs)
        val lines = sorted.mapIndexed { index, rawLine ->
            val nextStartTimeMs = sorted.getOrNull(index + 1)?.startTimeMs
            val timedWordEndTimeMs = rawLine.words
                .mapIndexed { wordIndex, word ->
                    val nextWordStartTimeMs = rawLine.words
                        .getOrNull(wordIndex + 1)
                        ?.startTimeMs
                    word.explicitEndTimeMs
                        ?.takeIf { it > word.startTimeMs }
                        ?: nextWordStartTimeMs
                        ?: word.startTimeMs + DEFAULT_WORD_DURATION_MS
                }
                .maxOrNull()
            val fallbackEndTimeMs = when {
                timedWordEndTimeMs != null && timedWordEndTimeMs > rawLine.startTimeMs -> {
                    timedWordEndTimeMs
                }
                nextStartTimeMs != null && nextStartTimeMs > rawLine.startTimeMs -> nextStartTimeMs
                durationMs > rawLine.startTimeMs -> durationMs
                else -> rawLine.startTimeMs + DEFAULT_LINE_DURATION_MS
            }
            val endTimeMs = rawLine.explicitEndTimeMs
                ?.coerceAtMost(nextStartTimeMs ?: Long.MAX_VALUE)
                ?.takeIf { it > rawLine.startTimeMs }
                ?: fallbackEndTimeMs
            val words = rawLine.words.mapIndexed { wordIndex, word ->
                val nextWordStart = rawLine.words.getOrNull(wordIndex + 1)?.startTimeMs
                val fallbackWordEndTimeMs = (nextWordStart ?: endTimeMs)
                    .coerceAtMost(endTimeMs)
                    .coerceAtLeast(word.startTimeMs + MIN_WORD_DURATION_MS)
                val wordEndTimeMs = word.explicitEndTimeMs
                    ?.coerceAtMost(nextWordStart ?: endTimeMs)
                    ?.coerceAtMost(endTimeMs)
                    ?.takeIf { it > word.startTimeMs }
                    ?: fallbackWordEndTimeMs
                LyricWord(
                    startTimeMs = word.startTimeMs.coerceAtLeast(rawLine.startTimeMs),
                    endTimeMs = wordEndTimeMs,
                    text = word.text,
                    hasTrailingSpace = word.hasTrailingSpace,
                )
            }
            LyricLine(
                agent = rawLine.agent,
                startTimeMs = rawLine.startTimeMs,
                endTimeMs = endTimeMs,
                text = rawLine.text,
                words = words,
                translation = rawLine.translation,
            )
        }.filter { line -> line.displayText.isNotBlank() }
        if (lines.isEmpty()) return null
        val transitions = buildList {
            lines.firstOrNull()?.let { firstLine ->
                if (firstLine.startTimeMs >= COUNTDOWN_GAP_THRESHOLD_MS) {
                    add(
                        LyricTransition(
                            afterLineIndex = -1,
                            startTimeMs = 0L,
                            endTimeMs = firstLine.startTimeMs,
                        ),
                    )
                }
            }
            lines.zipWithNext().forEachIndexed { lineIndex, (line, nextLine) ->
                val gapStartTimeMs = line.endTimeMs.coerceAtMost(nextLine.startTimeMs)
                if (nextLine.startTimeMs - gapStartTimeMs >= COUNTDOWN_GAP_THRESHOLD_MS) {
                    add(
                        LyricTransition(
                            afterLineIndex = lineIndex,
                            startTimeMs = gapStartTimeMs,
                            endTimeMs = nextLine.startTimeMs,
                        ),
                    )
                }
            }
        }
        return LyricsDocument(
            lines = lines,
            format = format,
            source = source,
            transitions = transitions,
        )
    }

    private fun MatchResult.toTimeMs(): Long? {
        val minutes = groupValues[1].toLongOrNull() ?: return null
        val seconds = groupValues[2].toLongOrNull() ?: return null
        val fraction = fractionToMilliseconds(groupValues[3])
        return minutes * 60_000L + seconds * 1_000L + fraction
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
        val hours = if (parts.size >= 3) parts[0].toLongOrNull() ?: return null else 0L
        val minutesIndex = if (parts.size >= 3) 1 else 0
        val minutes = parts[minutesIndex].toLongOrNull() ?: return null
        val secondsIndex = minutesIndex + 1
        val seconds = parts[secondsIndex].toDoubleOrNull() ?: return null
        val frames = parts.getOrNull(secondsIndex + 1)?.toLongOrNull() ?: 0L
        if (minutes < 0L || seconds < 0.0 || frames < 0L) return null
        return (
            hours * 3_600_000L +
                minutes * 60_000L +
                seconds * 1_000.0 +
                frames * (1_000.0 / DEFAULT_FRAME_RATE)
            ).toLong().coerceAtLeast(0L)
    }

    private fun Element.attributeValue(localName: String): String? {
        val attributes = attributes ?: return null
        for (index in 0 until attributes.length) {
            val attribute = attributes.item(index)
            val name = attribute.localName ?: attribute.nodeName.substringAfter(':')
            if (name.equals(localName, ignoreCase = true)) return attribute.nodeValue
        }
        return null
    }

    private fun String.normalizeVisibleText(): String = replace(Regex("""\s+"""), " ").trim()

    private fun DocumentBuilderFactory.setFeatureSafely(name: String, value: Boolean) {
        runCatching { setFeature(name, value) }
    }

    private fun DocumentBuilderFactory.setAttributeSafely(name: String, value: String) {
        runCatching { setAttribute(name, value) }
    }

    private const val DEFAULT_AGENT = "main"
    private const val MAX_LYRICS_CHARS = 2 * 1024 * 1024
    private const val MAX_LYRIC_LINES = 10_000
    private const val MAX_OFFSET_MS = 10 * 60 * 1_000L
    private const val DEFAULT_LINE_DURATION_MS = 5_000L
    private const val DEFAULT_WORD_DURATION_MS = 500L
    private const val MIN_WORD_DURATION_MS = 50L
    private const val DEFAULT_FRAME_RATE = 30.0
    private const val COUNTDOWN_GAP_THRESHOLD_MS = 5_000L
}
