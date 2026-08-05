package com.melox.player.ui.component.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.AtomicFile
import android.util.LruCache
import android.util.Size
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val ArtworkSize = 48.dp
private val ArtworkCornerRadius = 6.dp

/** Displays a bounded cached thumbnail without blocking the Compose thread. */
@Composable
fun TrackArtwork(
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
    modifier: Modifier = Modifier,
    size: Dp = ArtworkSize,
    cornerRadius: Dp = ArtworkCornerRadius,
) {
    val bitmap = rememberArtworkBitmap(
        contentUri = contentUri,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileSizeBytes = fileSizeBytes,
        size = size,
    )

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier
                .size(size)
                .squircleClip(cornerRadius),
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.High,
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .squircleBackground(
                    color = MiuixTheme.colorScheme.secondaryContainer,
                    cornerRadius = cornerRadius,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MiuixIcons.Music,
                contentDescription = null,
                modifier = Modifier.size(size / 2f),
                tint = MiuixTheme.colorScheme.onSecondaryContainer
                    .copy(alpha = 0.3f),
            )
        }
    }
}

/** Displays artwork at a caller-selected size while sharing the bounded memory/disk cache. */
@Composable
fun PlaybackArtwork(
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
    size: Dp,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    requestSize: Dp = size,
    contentScale: ContentScale = ContentScale.Crop,
    bitmapCrossfadeDurationMillis: Int = 0,
    bitmapCrossfadeEasing: Easing = LinearEasing,
    rectangularCornerRadiusReduction: Dp = 0.dp,
) {
    val bitmap = rememberArtworkBitmap(
        contentUri = contentUri,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileSizeBytes = fileSizeBytes,
        size = requestSize,
    )
    if (bitmapCrossfadeDurationMillis > 0) {
        PlaybackArtworkStackedFade(
            targetBitmap = bitmap,
            size = size,
            cornerRadius = cornerRadius,
            durationMillis = bitmapCrossfadeDurationMillis,
            easing = bitmapCrossfadeEasing,
            modifier = modifier,
            contentScale = contentScale,
            rectangularCornerRadiusReduction = rectangularCornerRadiusReduction,
        )
    } else {
        PlaybackArtworkFrame(
            bitmap = bitmap,
            size = size,
            cornerRadius = cornerRadius,
            modifier = modifier,
            contentScale = contentScale,
            rectangularCornerRadiusReduction = rectangularCornerRadiusReduction,
        )
    }
}

@Composable
private fun PlaybackArtworkStackedFade(
    targetBitmap: Bitmap?,
    size: Dp,
    cornerRadius: Dp,
    durationMillis: Int,
    easing: Easing,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    rectangularCornerRadiusReduction: Dp = 0.dp,
) {
    var outgoingBitmap by remember { mutableStateOf(targetBitmap) }
    var incomingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var hasIncomingLayer by remember { mutableStateOf(false) }
    val outgoingAlpha = remember { Animatable(1f) }
    val incomingAlpha = remember { Animatable(1f) }

    LaunchedEffect(targetBitmap) {
        if (targetBitmap === outgoingBitmap && !hasIncomingLayer) return@LaunchedEffect

        if (hasIncomingLayer) {
            outgoingBitmap = incomingBitmap
        }
        incomingAlpha.snapTo(0f)
        outgoingAlpha.snapTo(1f)
        incomingBitmap = targetBitmap
        hasIncomingLayer = true
        coroutineScope {
            launch {
                outgoingAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = durationMillis,
                        easing = easing,
                    ),
                )
            }
            launch {
                incomingAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = durationMillis,
                        easing = easing,
                    ),
                )
            }
        }
        outgoingBitmap = targetBitmap
        hasIncomingLayer = false
        incomingBitmap = null
        outgoingAlpha.snapTo(1f)
    }

    Box(modifier = modifier.size(size)) {
        if (hasIncomingLayer) {
            // The two layers crossfade together so a rectangular incoming image
            // cannot reveal an unchanged outgoing image in its letterbox area.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = outgoingAlpha.value },
            ) {
                PlaybackArtworkFrame(
                    bitmap = outgoingBitmap,
                    size = size,
                    cornerRadius = cornerRadius,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                    rectangularCornerRadiusReduction = rectangularCornerRadiusReduction,
                )
            }
            PlaybackArtworkFrame(
                bitmap = incomingBitmap,
                size = size,
                cornerRadius = cornerRadius,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = incomingAlpha.value },
                contentScale = contentScale,
                rectangularCornerRadiusReduction = rectangularCornerRadiusReduction,
            )
        } else {
            PlaybackArtworkFrame(
                bitmap = outgoingBitmap,
                size = size,
                cornerRadius = cornerRadius,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                rectangularCornerRadiusReduction = rectangularCornerRadiusReduction,
            )
        }
    }
}

/** Draws one already-resolved playback artwork frame without starting another cache request. */
@Composable
internal fun PlaybackArtworkFrame(
    bitmap: Bitmap?,
    size: Dp,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shadowElevation: Dp = 0.dp,
    ambientShadowColor: Color = Color.Transparent,
    spotShadowColor: Color = Color.Transparent,
    onContentBoundsChanged: ((Rect) -> Unit)? = null,
    rectangularCornerRadiusReduction: Dp = 0.dp,
) {
    if (bitmap != null) {
        val resolvedCornerRadius = playbackArtworkCornerRadius(
            cornerRadius = cornerRadius,
            bitmapWidth = bitmap.width,
            bitmapHeight = bitmap.height,
            rectangularReduction = rectangularCornerRadiusReduction,
        )
        val imageSize = remember(bitmap.width, bitmap.height, size, contentScale) {
            if (contentScale == ContentScale.Fit) {
                fittedArtworkSize(bitmap = bitmap, bound = size)
            } else {
                DpSize(size, size)
            }
        }
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(imageSize)
                    .onGloballyPositioned { coordinates ->
                        onContentBoundsChanged?.invoke(coordinates.boundsInParent())
                    }
                    .graphicsLayer {
                        this.shadowElevation = shadowElevation.toPx()
                        this.ambientShadowColor = ambientShadowColor
                        this.spotShadowColor = spotShadowColor
                        shape = RoundedCornerShape(resolvedCornerRadius)
                        clip = true
                    },
                contentScale = contentScale,
                filterQuality = FilterQuality.High,
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(size)
                .squircleBackground(
                    color = MiuixTheme.colorScheme.secondaryContainer,
                    cornerRadius = cornerRadius,
                ),
        )
    }
}

internal fun playbackArtworkCornerRadius(
    cornerRadius: Dp,
    bitmapWidth: Int,
    bitmapHeight: Int,
    rectangularReduction: Dp,
): Dp = if (bitmapWidth != bitmapHeight) {
    maxOf(0.dp, cornerRadius - rectangularReduction)
} else {
    cornerRadius
}

private fun fittedArtworkSize(bitmap: Bitmap, bound: Dp): DpSize {
    val width = bitmap.width.coerceAtLeast(1)
    val height = bitmap.height.coerceAtLeast(1)
    return if (width >= height) {
        DpSize(width = bound, height = bound * height.toFloat() / width.toFloat())
    } else {
        DpSize(width = bound * width.toFloat() / height.toFloat(), height = bound)
    }
}

/** Loads a cached artwork bitmap for surfaces that need their own rendering treatment. */
@Composable
internal fun rememberArtworkBitmap(
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
    size: Dp,
): Bitmap? {
    val context = LocalContext.current.applicationContext
    val targetSizePx = normalizeArtworkTargetSize(
        with(LocalDensity.current) { size.roundToPx() },
    )
    val cacheKey = createArtworkCacheKey(
        contentUri = contentUri,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileSizeBytes = fileSizeBytes,
        targetSizePx = targetSizePx,
    )
    var retainedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val initialResult = remember(cacheKey) {
        ArtworkCache.getCached(
            context = context,
            key = cacheKey,
            includeDisk = false,
        )
    }
    val result = produceState<ArtworkResult?>(
            initialValue = initialResult ?: retainedBitmap
                ?.takeIf { contentUri.isNotBlank() }
                ?.let(ArtworkResult::Loaded),
            key1 = cacheKey,
        ) {
            val cached = ArtworkCache.get(cacheKey)
            if (cached != null) {
                value = cached
            } else {
                value = ArtworkCache.getOrLoad(context, cacheKey) {
                    loadArtworkThumbnail(
                        context = context,
                        contentUri = contentUri,
                        targetSizePx = targetSizePx,
                    )
                }
            }
        }.value
    LaunchedEffect(result, contentUri) {
        retainedBitmap = when {
            contentUri.isBlank() -> null
            result is ArtworkResult.Loaded -> result.bitmap
            result == ArtworkResult.Missing -> null
            else -> retainedBitmap
        }
    }
    return when (result) {
        is ArtworkResult.Loaded -> result.bitmap
        ArtworkResult.Missing,
        null,
        -> null
    }
}

internal suspend fun prefetchArtwork(
    context: Context,
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
    targetSizePx: Int,
): Bitmap? = loadArtworkBitmap(
        context = context,
        contentUri = contentUri,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileSizeBytes = fileSizeBytes,
        targetSizePx = targetSizePx,
    )

internal suspend fun loadArtworkBitmap(
    context: Context,
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
    targetSizePx: Int,
): Bitmap? {
    if (contentUri.isBlank()) return null
    val normalizedTargetSizePx = normalizeArtworkTargetSize(targetSizePx)
    val cacheKey = createArtworkCacheKey(
        contentUri = contentUri,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileSizeBytes = fileSizeBytes,
        targetSizePx = normalizedTargetSizePx,
    )
    return when (val result = ArtworkCache.getOrLoad(context.applicationContext, cacheKey) {
        loadArtworkThumbnail(
            context = context.applicationContext,
            contentUri = contentUri,
            targetSizePx = normalizedTargetSizePx,
        )
    }) {
        is ArtworkResult.Loaded -> result.bitmap
        ArtworkResult.Missing -> null
    }
}

internal fun normalizeArtworkTargetSize(targetSizePx: Int): Int {
    val requested = targetSizePx.coerceAtLeast(1)
    return ARTWORK_SIZE_BUCKETS.firstOrNull { it >= requested }
        ?: ARTWORK_SIZE_BUCKETS.last()
}

private fun loadArtworkThumbnail(
    context: Context,
    contentUri: String,
    targetSizePx: Int,
): ArtworkExtractionResult {
    val thumbnailSizePx = targetSizePx.coerceAtLeast(1)
    val uri = contentUri.toUri()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        runCatching {
            context.contentResolver.loadThumbnail(
                uri,
                Size(thumbnailSizePx, thumbnailSizePx),
                null,
            )
        }.getOrNull()?.let { bitmap ->
            return ArtworkExtractionResult.Loaded(
                bitmap.toBoundedThumbnail(thumbnailSizePx),
            )
        }
    }

    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val artworkData = retriever.embeddedPicture
            ?: return ArtworkExtractionResult.Missing
        decodeSampledBitmap(artworkData, thumbnailSizePx)
            ?.let(ArtworkExtractionResult::Loaded)
            ?: ArtworkExtractionResult.Missing
    } catch (_: Exception) {
        ArtworkExtractionResult.Failed
    } finally {
        runCatching(retriever::release)
    }
}

private fun Bitmap.toBoundedThumbnail(targetSizePx: Int): Bitmap {
    val (targetWidth, targetHeight) = fitArtworkDimensions(
        width = width,
        height = height,
        targetSizePx = targetSizePx,
    )
    if (width == targetWidth && height == targetHeight) return this

    return Bitmap.createScaledBitmap(
        this,
        targetWidth,
        targetHeight,
        true,
    ).also { scaled ->
        if (scaled !== this) recycle()
    }
}

internal fun fitArtworkDimensions(
    width: Int,
    height: Int,
    targetSizePx: Int,
): Pair<Int, Int> {
    val boundedTarget = targetSizePx.coerceAtLeast(1)
    val maxDimension = maxOf(width, height)
    if (maxDimension <= boundedTarget) return width to height

    val scale = boundedTarget.toFloat() / maxDimension
    return (width * scale).roundToInt().coerceAtLeast(1) to
        (height * scale).roundToInt().coerceAtLeast(1)
}

private fun decodeSampledBitmap(data: ByteArray, targetSizePx: Int): Bitmap? {
    val thumbnailSizePx = targetSizePx.coerceAtLeast(1)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(data, 0, data.size, bounds)

    var sampleSize = 1
    while (
        maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >=
        thumbnailSizePx
    ) {
        sampleSize *= 2
    }

    val decoded = BitmapFactory.decodeByteArray(
        data,
        0,
        data.size,
        BitmapFactory.Options().apply { inSampleSize = sampleSize },
    ) ?: return null

    return decoded.toBoundedThumbnail(thumbnailSizePx)
}

private sealed interface ArtworkResult {
    data class Loaded(val bitmap: Bitmap) : ArtworkResult

    data object Missing : ArtworkResult
}

private sealed interface ArtworkExtractionResult {
    data class Loaded(val bitmap: Bitmap) : ArtworkExtractionResult

    data object Missing : ArtworkExtractionResult

    data object Failed : ArtworkExtractionResult
}

internal fun createArtworkCacheKey(
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
    targetSizePx: Int,
): String = buildString {
    append(ARTWORK_CACHE_SCHEMA_VERSION)
    append('|')
    append(targetSizePx.coerceAtLeast(1))
    append('|')
    append(dateModifiedEpochSeconds.coerceAtLeast(0L))
    append('|')
    append(fileSizeBytes.coerceAtLeast(0L))
    append('|')
    append(contentUri)
}

internal fun artworkCacheFileStem(cacheKey: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(cacheKey.toByteArray(Charsets.UTF_8))
    return buildString(digest.size * 2) {
        digest.forEach { byte ->
            val value = byte.toInt() and 0xff
            append(HEX_DIGITS[value ushr 4])
            append(HEX_DIGITS[value and 0x0f])
        }
    }
}

private object ArtworkCache {
    private val maxSizeKilobytes = (Runtime.getRuntime().maxMemory() / 1024L / 4L)
        .coerceIn(
            minimumValue = 8L * 1024L,
            maximumValue = 64L * 1024L,
        ).toInt()

    private val cache = object : LruCache<String, Bitmap>(maxSizeKilobytes) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            (value.byteCount / 1024).coerceAtLeast(1)
    }
    private val missingKeys = LruCache<String, Boolean>(MAX_MEMORY_MISSING_ENTRIES)
    private val inFlight = ConcurrentHashMap<String, Deferred<ArtworkResult>>()
    private val loaderScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun get(key: String): ArtworkResult? =
        cache.get(key)?.let(ArtworkResult::Loaded)
            ?: if (missingKeys.get(key) == true) ArtworkResult.Missing else null

    fun getCached(
        context: Context,
        key: String,
        includeDisk: Boolean,
    ): ArtworkResult? {
        get(key)?.let { return it }
        if (!includeDisk) return null
        return ArtworkDiskCache.get(context, key)?.also { result ->
            remember(key, result)
        }
    }

    suspend fun getOrLoad(
        context: Context,
        key: String,
        loader: () -> ArtworkExtractionResult,
    ): ArtworkResult {
        get(key)?.let { return it }

        val newRequest = loaderScope.async(start = CoroutineStart.LAZY) {
            try {
                ArtworkDiskCache.get(context, key)?.let { cachedResult ->
                    remember(key, cachedResult)
                    return@async cachedResult
                }

                when (val extracted = loader()) {
                    is ArtworkExtractionResult.Loaded -> {
                        val result = ArtworkResult.Loaded(extracted.bitmap)
                        remember(key, result)
                        ArtworkDiskCache.put(context, key, result)
                        result
                    }

                    ArtworkExtractionResult.Missing -> {
                        val result = ArtworkResult.Missing
                        remember(key, result)
                        ArtworkDiskCache.put(context, key, result)
                        result
                    }

                    ArtworkExtractionResult.Failed -> ArtworkResult.Missing
                }
            } finally {
                inFlight.remove(key)
            }
        }
        val request = inFlight.putIfAbsent(key, newRequest) ?: newRequest
        if (request !== newRequest) {
            newRequest.cancel()
        }
        request.start()
        return request.await()
    }

    private fun remember(key: String, result: ArtworkResult) {
        when (result) {
            is ArtworkResult.Loaded -> cache.put(key, result.bitmap)
            ArtworkResult.Missing -> missingKeys.put(key, true)
        }
    }
}

internal data class ArtworkDiskCacheEntry(
    val file: File,
    val lastModifiedEpochMillis: Long,
    val sizeBytes: Long,
)

internal fun snapshotArtworkDiskCacheEntries(files: Array<File>): List<ArtworkDiskCacheEntry> =
    files
        .asSequence()
        .filter(File::isFile)
        .map { file ->
            ArtworkDiskCacheEntry(
                file = file,
                lastModifiedEpochMillis = file.lastModified(),
                sizeBytes = file.length(),
            )
        }
        .sortedWith(
            compareBy<ArtworkDiskCacheEntry>(ArtworkDiskCacheEntry::lastModifiedEpochMillis)
                .thenBy { entry -> entry.file.name },
        )
        .toList()

private object ArtworkDiskCache {
    private val initialized = AtomicBoolean(false)
    private val writesSinceMaintenance = AtomicInteger(0)
    private val maintenanceLock = Any()

    fun get(context: Context, key: String): ArtworkResult? {
        val directory = directory(context) ?: return null
        val fileStem = artworkCacheFileStem(key)
        val artworkFile = directory.resolve("$fileStem.png")
        val atomicArtworkFile = AtomicFile(artworkFile)
        val backupFile = directory.resolve("$fileStem.png.bak")
        if (artworkFile.isFile || backupFile.isFile) {
            val bitmap = runCatching {
                atomicArtworkFile.openRead().use(BitmapFactory::decodeStream)
            }.getOrNull()
            if (bitmap != null) {
                artworkFile.touchIfStale()
                return ArtworkResult.Loaded(bitmap)
            }
            atomicArtworkFile.delete()
        }

        val missingFile = directory.resolve("$fileStem.missing")
        return if (missingFile.isFile) {
            missingFile.touchIfStale()
            ArtworkResult.Missing
        } else {
            null
        }
    }

    fun put(context: Context, key: String, result: ArtworkResult) {
        val directory = directory(context) ?: return
        val fileStem = artworkCacheFileStem(key)
        val artworkFile = directory.resolve("$fileStem.png")
        val missingFile = directory.resolve("$fileStem.missing")
        val written = when (result) {
            is ArtworkResult.Loaded -> {
                val atomicFile = AtomicFile(artworkFile)
                val output = try {
                    atomicFile.startWrite()
                } catch (_: IOException) {
                    return
                }
                try {
                    if (!result.bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                        throw IOException("Unable to encode artwork thumbnail")
                    }
                    atomicFile.finishWrite(output)
                    missingFile.delete()
                    true
                } catch (_: Exception) {
                    atomicFile.failWrite(output)
                    false
                }
            }

            ArtworkResult.Missing -> runCatching {
                if (!missingFile.exists()) {
                    missingFile.createNewFile()
                }
                AtomicFile(artworkFile).delete()
                true
            }.getOrDefault(false)
        }
        if (written) {
            maintainAfterWrite(directory)
        }
    }

    private fun directory(context: Context): File? {
        val directory = context.noBackupFilesDir.resolve(ARTWORK_DISK_CACHE_DIRECTORY)
        if ((!directory.exists() && !directory.mkdirs()) || !directory.isDirectory) {
            return null
        }
        if (initialized.compareAndSet(false, true)) {
            synchronized(maintenanceLock) {
                trim(directory)
            }
        }
        return directory
    }

    private fun maintainAfterWrite(directory: File) {
        if (writesSinceMaintenance.incrementAndGet() < DISK_MAINTENANCE_INTERVAL) return
        synchronized(maintenanceLock) {
            if (writesSinceMaintenance.get() >= DISK_MAINTENANCE_INTERVAL) {
                writesSinceMaintenance.set(0)
                trim(directory)
            }
        }
    }

    private fun trim(directory: File) {
        val entries = directory.listFiles()
            ?.let(::snapshotArtworkDiskCacheEntries)
            ?: return
        var totalBytes = entries.sumOf(ArtworkDiskCacheEntry::sizeBytes)
        var fileCount = entries.size
        for (entry in entries) {
            if (
                totalBytes <= MAX_DISK_CACHE_BYTES &&
                fileCount <= MAX_DISK_CACHE_ENTRIES
            ) {
                break
            }
            if (entry.file.delete()) {
                totalBytes -= entry.sizeBytes
                fileCount -= 1
            }
        }
    }

    private fun File.touchIfStale() {
        val now = System.currentTimeMillis()
        if (now - lastModified() >= DISK_ACCESS_TOUCH_INTERVAL_MILLIS) {
            setLastModified(now)
        }
    }
}

private const val ARTWORK_CACHE_SCHEMA_VERSION = 3
private const val ARTWORK_DISK_CACHE_DIRECTORY = "artwork_thumbnails_v2"
private const val MAX_MEMORY_MISSING_ENTRIES = 10_000
private const val MAX_DISK_CACHE_ENTRIES = 20_000
private const val MAX_DISK_CACHE_BYTES = 64L * 1024L * 1024L
private const val DISK_MAINTENANCE_INTERVAL = 64
private const val DISK_ACCESS_TOUCH_INTERVAL_MILLIS = 60L * 60L * 1000L
private const val HEX_DIGITS = "0123456789abcdef"
private val ARTWORK_SIZE_BUCKETS =
    intArrayOf(64, 96, 128, 192, 256, 384, 512, 640, 768, 1024, 1280, 1536, 2048)
