package com.melox.player.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.id3.BinaryFrame
import androidx.media3.extractor.metadata.id3.CommentFrame
import androidx.media3.extractor.metadata.id3.InternalFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import androidx.media3.inspector.MetadataRetriever
import com.melox.player.data.lyrics.LyricsParser
import com.melox.player.model.LyricsDocument
import com.melox.player.model.LyricsFormat
import com.melox.player.model.LyricsSource
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.TimeUnit

data class LyricsRequest(
    val mediaId: String,
    val contentUri: String,
    val fileName: String?,
    val folderPath: String?,
    val durationMs: Long,
    val refreshRevision: Long = 0L,
)

/** Reads timestamped lyrics from local audio metadata and sidecars. */
class LyricsRepository(context: Context) {
    private val applicationContext = context.applicationContext
    private val contentResolver = applicationContext.contentResolver

    suspend fun load(request: LyricsRequest): LyricsDocument? {
        readEmbeddedCandidates(request.contentUri)
            .firstNotNullOfOrNull { candidate ->
                LyricsParser.parse(
                    raw = candidate,
                    source = LyricsSource.EMBEDDED,
                    durationMs = request.durationMs,
                )
            }
            ?.let { return it }
        readSidecar(request)?.let { (text, format) ->
            return LyricsParser.parse(
                raw = text,
                source = LyricsSource.SIDECAR,
                preferredFormat = format,
                durationMs = request.durationMs,
            )
        }
        return null
    }

    private fun readSidecar(request: LyricsRequest): Pair<String, LyricsFormat>? {
        val candidates = exactLyricsSidecarCandidates(request.fileName)
        if (candidates.isEmpty()) return null
        readDirectSidecar(request.folderPath, candidates)?.let { return it }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val relativePath = request.folderPath.toRelativeMediaStorePath() ?: return null
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val candidateByName = candidates.associateBy { it.first }
        return runCatching {
            contentResolver.query(
                collection,
                arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                ),
                "${MediaStore.Files.FileColumns.RELATIVE_PATH} = ?",
                arrayOf(relativePath),
                null,
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val displayName = cursor.getString(nameColumn).orEmpty()
                    val candidate = candidateByName[displayName] ?: continue
                    val uri = ContentUris.withAppendedId(collection, cursor.getLong(idColumn))
                    readText(uri)?.let { return@use it to candidate.second }
                }
                null
            }
        }.getOrNull()
    }

    @Suppress("DEPRECATION")
    private fun readDirectSidecar(
        folderPath: String?,
        candidates: List<Pair<String, LyricsFormat>>,
    ): Pair<String, LyricsFormat>? {
        val path = folderPath ?: return null
        val directory = when {
            path.startsWith("/storage/") ||
                path.startsWith("/sdcard/") ||
                path.startsWith("/mnt/") -> File(path)
            else -> File(Environment.getExternalStorageDirectory(), path.trimStart('/'))
        }
        candidates.forEach { (name, format) ->
            val file = File(directory, name)
            runCatching {
                if (file.isFile && file.length() in 1..MAX_SIDECAR_BYTES) {
                    file.inputStream().use(::readBounded)?.let { return it to format }
                }
            }
        }
        return null
    }

    private fun readText(uri: Uri): String? = runCatching {
        contentResolver.openInputStream(uri)?.use(::readBounded)
    }.getOrNull()

    @OptIn(UnstableApi::class)
    @Suppress("DEPRECATION")
    private fun readEmbeddedCandidates(contentUri: String): List<String> {
        if (contentUri.isBlank()) return emptyList()
        val groups = runCatching {
            MetadataRetriever.Builder(
                applicationContext,
                MediaItem.fromUri(contentUri),
            ).build().use { retriever ->
                retriever.retrieveTrackGroups().get(METADATA_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            }
        }.getOrNull() ?: return emptyList()
        return buildList {
            for (groupIndex in 0 until groups.length) {
                val group = groups[groupIndex]
                for (formatIndex in 0 until group.length) {
                    val metadata = group.getFormat(formatIndex).metadata ?: continue
                    for (entryIndex in 0 until metadata.length()) {
                        extractLyricsText(metadata[entryIndex])?.let { text ->
                            if (text.isNotBlank() && none { it == text }) {
                                add(text.take(MAX_EMBEDDED_CHARS))
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun extractLyricsText(entry: Any): String? = when (entry) {
        is TextInformationFrame -> when {
            entry.id.equals("USLT", ignoreCase = true) -> entry.values.joinToString("\n")
            entry.id.equals("TXXX", ignoreCase = true) &&
                entry.description.isLyricsKey() -> entry.values.joinToString("\n")
            else -> null
        }
        is VorbisComment -> entry.value.takeIf { entry.key.isLyricsKey() }
        is BinaryFrame -> entry.data.takeIf {
            entry.id.equals("USLT", ignoreCase = true)
        }?.let(::decodeUsltFrame)
        is CommentFrame -> entry.text.takeIf { entry.description.isLyricsKey() }
        is InternalFrame -> entry.text.takeIf { entry.description.isLyricsKey() }
        else -> null
    }

    private fun decodeUsltFrame(data: ByteArray): String? {
        if (data.size < 5 || data.size > MAX_EMBEDDED_CHARS) return null
        val encoding = data[0].toInt() and 0xFF
        val charset = when (encoding) {
            1 -> StandardCharsets.UTF_16
            2 -> StandardCharsets.UTF_16BE
            3 -> StandardCharsets.UTF_8
            else -> StandardCharsets.ISO_8859_1
        }
        val delimiterLength = if (encoding == 1 || encoding == 2) 2 else 1
        var delimiterStart = 4
        while (delimiterStart + delimiterLength <= data.size) {
            val isDelimiter = if (delimiterLength == 1) {
                data[delimiterStart].toInt() == 0
            } else {
                data[delimiterStart].toInt() == 0 && data[delimiterStart + 1].toInt() == 0
            }
            if (isDelimiter) break
            delimiterStart += delimiterLength
        }
        val textStart = (delimiterStart + delimiterLength).coerceAtMost(data.size)
        if (textStart >= data.size) return null
        return String(data, textStart, data.size - textStart, charset)
            .trim('\u0000', '\uFEFF', ' ')
            .takeIf(String::isNotBlank)
    }

    private fun readBounded(input: InputStream): String? {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_SIDECAR_BYTES) return null
            output.write(buffer, 0, read)
        }
        return decodeText(output.toByteArray()).takeIf(String::isNotBlank)
    }

    private fun decodeText(bytes: ByteArray): String {
        if (bytes.startsWith(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))) {
            return String(bytes, 3, bytes.size - 3, StandardCharsets.UTF_8)
        }
        if (bytes.startsWith(byteArrayOf(0xFF.toByte(), 0xFE.toByte()))) {
            return String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16LE)
        }
        if (bytes.startsWith(byteArrayOf(0xFE.toByte(), 0xFF.toByte()))) {
            return String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16BE)
        }
        return try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (_: CharacterCodingException) {
            String(bytes, charset("GB18030"))
        }
    }

    private fun String?.isLyricsKey(): Boolean {
        val normalized = this?.uppercase(Locale.ROOT).orEmpty()
            .replace(" ", "")
            .replace("_", "")
        return normalized in LYRICS_KEYS || normalized.contains("LYRIC")
    }

    private fun String?.toRelativeMediaStorePath(): String? {
        var path = this?.replace('\\', '/')?.trim().orEmpty()
        if (path.isEmpty()) return null
        SHARED_STORAGE_ROOTS.firstOrNull { root ->
            path.equals(root, ignoreCase = true) || path.startsWith("$root/", ignoreCase = true)
        }?.let { root ->
            path = path.substring(root.length)
        }
        return path.trim('/').takeIf(String::isNotEmpty)?.plus("/")
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

    private companion object {
        private const val MAX_SIDECAR_BYTES = 2L * 1024L * 1024L
        private const val MAX_EMBEDDED_CHARS = 2 * 1024 * 1024
        private const val METADATA_TIMEOUT_SECONDS = 5L
        private val LYRICS_KEYS = setOf(
            "LYRICS",
            "SYNCEDLYRICS",
            "UNSYNCEDLYRICS",
            "USLT",
            "LYRIC",
            "LYRICSENG",
        )
        private val SHARED_STORAGE_ROOTS = listOf(
            "/storage/emulated/0",
            "/storage/self/primary",
            "/mnt/sdcard",
            "/sdcard",
        )
    }
}

internal fun exactLyricsSidecarCandidates(
    audioFileName: String?,
): List<Pair<String, LyricsFormat>> {
    val fileName = audioFileName ?: return emptyList()
    val stem = fileName.substringBeforeLast('.', fileName).takeIf(String::isNotBlank)
        ?: return emptyList()
    return listOf(
        "$stem.ttml" to LyricsFormat.TTML,
        "$stem.lrc" to LyricsFormat.LRC,
    )
}
