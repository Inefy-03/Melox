package com.melox.player.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.ColorSpace
import android.net.Uri
import androidx.core.graphics.scale
import com.google.android.renderscript.Toolkit
import com.melox.player.model.AppSettings
import com.melox.player.model.normalizeCustomBackgroundBlurPercent
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

/** Published bitmaps are read-only and remain valid while the UI retains them. */
data class LoadedCustomBackground(
    val id: String,
    val blurPercent: Int,
    val source: Bitmap,
    val rendered: Bitmap,
)

class CustomBackgroundRepository(context: Context) {
    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, "custom_backgrounds")

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeImages(settings: Flow<AppSettings>): Flow<LoadedCustomBackground?> = flow {
        var sourceId: String? = null
        var source: Bitmap? = null
        emitAll(
            settings.map { it.customBackgroundId to normalizeCustomBackgroundBlurPercent(it.customBackgroundBlurPercent) }
                .distinctUntilChanged()
                .mapLatest { (id, percent) ->
                    if (id == null) {
                        sourceId = null
                        source = null
                        return@mapLatest null
                    }
                    if (sourceId != id) {
                        source = loadSourceImage(id)
                        sourceId = id
                    }
                    val currentSource = source ?: return@mapLatest null
                    val rendered = if (percent == 0) currentSource else blurImage(currentSource, percent)
                    currentCoroutineContext().ensureActive()
                    rendered?.let { LoadedCustomBackground(id, percent, currentSource, it) }
                },
        )
    }.flowOn(Dispatchers.Default)

    suspend fun importImage(uri: Uri): String? = withContext(Dispatchers.IO) {
        var bitmap: Bitmap? = null
        var destination: File? = null
        try {
            bitmap = ImageDecoder.decodeBitmap(
                ImageDecoder.createSource(appContext.contentResolver, uri),
            ) { decoder, info, _ ->
                val dimensions = fitCustomBackgroundDimensions(info.size.width, info.size.height)
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
                decoder.setTargetSize(dimensions.first, dimensions.second)
            }
            ensureActive()
            check(directory.isDirectory || directory.mkdirs())
            val id = UUID.randomUUID().toString()
            destination = requireNotNull(imageFile(id))
            destination.outputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
            ensureActive()
            id
        } catch (error: CancellationException) {
            destination?.delete()
            throw error
        } catch (_: Exception) {
            destination?.delete()
            null
        } catch (_: OutOfMemoryError) {
            destination?.delete()
            null
        } finally {
            bitmap?.recycle()
        }
    }

    suspend fun loadImage(id: String, blurPercent: Int?): Bitmap? {
        val source = loadSourceImage(id) ?: return null
        if (blurPercent == null || normalizeCustomBackgroundBlurPercent(blurPercent) == 0) return source
        return try {
            blurImage(source, blurPercent)
        } finally {
            source.recycle()
        }
    }

    internal suspend fun loadSourceImage(id: String): Bitmap? = withContext(Dispatchers.IO) {
        var decoded: Bitmap? = null
        try {
            val file = imageFile(id)?.takeIf(File::isFile) ?: return@withContext null
            decoded = ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
                val dimensions = fitCustomBackgroundDimensions(info.size.width, info.size.height)
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
                decoder.setTargetSize(dimensions.first, dimensions.second)
            }
            ensureActive()
            decoded.also { decoded = null }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        } finally {
            decoded?.recycle()
        }
    }

    internal suspend fun blurImage(source: Bitmap, percent: Int): Bitmap? = withContext(Dispatchers.Default) {
        if (normalizeCustomBackgroundBlurPercent(percent) == 0) return@withContext source
        var scaled: Bitmap? = null
        try {
            val dimensions = fitCustomBackgroundDimensions(
                source.width, source.height, customBackgroundBlurInputMaxEdge(percent),
            )
            val input = if (dimensions.first == source.width && dimensions.second == source.height) {
                source
            } else {
                source.scale(dimensions.first, dimensions.second, filter = true).also { scaled = it }
            }
            ensureActive()
            Toolkit.blur(input, customBackgroundBlurRadius(percent)).also { ensureActive() }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        } finally {
            scaled?.recycle()
        }
    }

    suspend fun deleteImage(id: String) = withContext(Dispatchers.IO) {
        imageFile(id)?.delete()
        Unit
    }

    private fun imageFile(id: String): File? =
        id.takeIf(::isCustomBackgroundId)?.let { File(directory, "$it.png") }
}

internal fun isCustomBackgroundId(id: String): Boolean =
    id.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))

internal fun fitCustomBackgroundDimensions(
    width: Int,
    height: Int,
    maxEdge: Int = 2048,
): Pair<Int, Int> {
    require(width > 0 && height > 0 && maxEdge > 0)
    val scale = minOf(1.0, maxEdge.toDouble() / maxOf(width, height))
    return (width * scale).roundToInt().coerceAtLeast(1) to
        (height * scale).roundToInt().coerceAtLeast(1)
}

internal fun customBackgroundBlurRadius(percent: Int): Int =
    if (normalizeCustomBackgroundBlurPercent(percent) == 0) 0
    else (normalizeCustomBackgroundBlurPercent(percent) * 25f / 100f).roundToInt().coerceIn(1, 25)

internal fun customBackgroundBlurInputMaxEdge(percent: Int): Int {
    val strength = normalizeCustomBackgroundBlurPercent(percent) / 100f
    return (2048f / (1f + 7f * strength * strength)).roundToInt().coerceIn(256, 2048)
}
