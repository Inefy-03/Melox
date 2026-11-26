package com.melox.player.ui.component.library

import android.content.Context
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.melox.player.data.library.readEmbeddedArtwork
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal data class ArtworkPreviewFile(
    val file: File,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val displayName: String,
)

internal suspend fun readArtworkPreviewFile(
    context: Context,
    contentUri: String,
    title: String?,
): ArtworkPreviewFile? = withContext(Dispatchers.IO) {
    try {
        val (bytes, bounds) = readEmbeddedArtwork(context, contentUri) { data ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(data, 0, data.size, options)
            (data to options).takeIf { options.outWidth > 0 && options.outHeight > 0 }
        } ?: return@withContext null
        val mime = bounds.outMimeType ?: return@withContext null
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "img"
        val directory = File(context.cacheDir, "shared_artwork").apply { mkdirs() }
        // Share recipients may read after the preview closes. Retain files for one day.
        val cutoff = System.currentTimeMillis() - 24L * 60L * 60L * 1000L
        directory.listFiles()?.filter { it.isFile && it.lastModified() < cutoff }
            ?.forEach { it.delete() }
        val file = File.createTempFile("cover-", ".$extension", directory)
        try {
            file.writeBytes(bytes)
        } catch (error: Exception) {
            file.delete()
            throw error
        }
        ArtworkPreviewFile(
            file = file,
            width = bounds.outWidth,
            height = bounds.outHeight,
            mimeType = mime,
            displayName = artworkExportName(title, extension),
        )
    } catch (_: Exception) {
        null
    } catch (_: OutOfMemoryError) {
        null
    }
}

internal fun artworkExportName(title: String?, extension: String): String {
    val base = title.orEmpty().replace(Regex("[\\p{Cntrl}/\\\\:*?\"<>|]"), "_")
        .trim().trim('.').take(80).ifBlank { "Artwork" }
    return "$base.$extension"
}

internal suspend fun decodeArtworkPreview(
    artwork: ArtworkPreviewFile,
    displayedSizePx: Int,
): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val target = fullPlayerArtworkTargetSizePx(displayedSizePx)
        val (width, height) = fitArtworkDimensions(artwork.width, artwork.height, target)
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(artwork.file)) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetSize(width, height)
        }
    } catch (_: Exception) {
        null
    } catch (_: OutOfMemoryError) {
        null
    }
}

internal suspend fun saveArtworkToGallery(
    context: Context,
    artwork: ArtworkPreviewFile,
): Boolean = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, artwork.displayName)
            put(MediaStore.Images.Media.MIME_TYPE, artwork.mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Melox")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = runCatching {
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        }.getOrNull() ?: return@withContext false
        try {
            requireNotNull(resolver.openOutputStream(uri, "w")).use { output ->
                artwork.file.inputStream().use { it.copyTo(output) }
            }
            val published = resolver.update(uri, ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }, null, null)
            check(published > 0)
            true
        } catch (_: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            false
        }
    } else {
        saveArtworkToLegacyGallery(context, artwork)
    }
}

@Suppress("DEPRECATION")
private suspend fun saveArtworkToLegacyGallery(
    context: Context,
    artwork: ArtworkPreviewFile,
): Boolean {
    var destination: File? = null
    return try {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "Melox",
        )
        check(directory.isDirectory || directory.mkdirs())
        val file = File.createTempFile(
            artwork.displayName.substringBeforeLast('.').padEnd(3, '_') + "-",
            ".${artwork.displayName.substringAfterLast('.')}",
            directory,
        )
        destination = file
        artwork.file.inputStream().use { input -> file.outputStream().use { input.copyTo(it) } }
        val scanned = suspendCancellableCoroutine { continuation ->
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath),
                arrayOf(artwork.mimeType)) { _, uri ->
                if (continuation.isActive) continuation.resume(uri != null)
            }
        }
        if (!scanned) file.delete()
        scanned
    } catch (_: Exception) {
        destination?.delete()
        false
    }
}
