package com.melox.player.ui.component.home

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.google.android.renderscript.Toolkit
import com.melox.player.ui.component.library.createArtworkCacheKey
import com.melox.player.ui.component.library.loadCachedArtworkDerivative

@Composable
internal fun rememberHomeRecommendationReflection(
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
    sourceBitmap: Bitmap?,
    enabled: Boolean,
): Bitmap? {
    val context = LocalContext.current.applicationContext
    val cacheKey = createHomeRecommendationReflectionCacheKey(
        contentUri = contentUri,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileSizeBytes = fileSizeBytes,
    )
    return produceState<Bitmap?>(
        initialValue = null,
        key1 = enabled,
        key2 = sourceBitmap,
        key3 = cacheKey,
    ) {
        value = if (enabled && sourceBitmap != null) {
            loadCachedArtworkDerivative(
                context = context,
                cacheKey = cacheKey,
            ) {
                createHomeRecommendationReflectionBitmap(sourceBitmap)
            }
        } else {
            null
        }
    }.value
}

internal fun createHomeRecommendationReflectionCacheKey(
    contentUri: String,
    dateModifiedEpochSeconds: Long,
    fileSizeBytes: Long,
): String = createArtworkCacheKey(
    contentUri = "$HOME_RECOMMENDATION_REFLECTION_CACHE_NAMESPACE|$contentUri",
    dateModifiedEpochSeconds = dateModifiedEpochSeconds,
    fileSizeBytes = fileSizeBytes,
    targetSizePx = HOME_RECOMMENDATION_REFLECTION_SIZE_PX,
)

internal fun createHomeRecommendationReflectionMesh(
    width: Int,
    height: Int,
): FloatArray = FloatArray(HOME_RECOMMENDATION_REFLECTION_NORMALIZED_MESH.size) { index ->
    val scale = if (index % 2 == 0) width.toFloat() else height.toFloat()
    HOME_RECOMMENDATION_REFLECTION_NORMALIZED_MESH[index] * scale
}

private fun createHomeRecommendationReflectionBitmap(sourceBitmap: Bitmap): Bitmap {
    val smallBitmap = sourceBitmap.centerCropAndScale(HOME_RECOMMENDATION_REFLECTION_SIZE_PX)
    val processedBitmap = smallBitmap.copy(Bitmap.Config.ARGB_8888, true)
        ?: Bitmap.createBitmap(
            HOME_RECOMMENDATION_REFLECTION_SIZE_PX,
            HOME_RECOMMENDATION_REFLECTION_SIZE_PX,
            Bitmap.Config.ARGB_8888,
        )
    val paint = Paint(
        Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG,
    ).apply {
        colorFilter = ColorMatrixColorFilter(
            ColorMatrix().apply {
                setSaturation(HOME_RECOMMENDATION_REFLECTION_SATURATION)
            },
        )
    }
    Canvas(processedBitmap).apply {
        drawBitmapMesh(
            smallBitmap,
            HOME_RECOMMENDATION_REFLECTION_MESH_WIDTH,
            HOME_RECOMMENDATION_REFLECTION_MESH_HEIGHT,
            createHomeRecommendationReflectionMesh(
                width = processedBitmap.width,
                height = processedBitmap.height,
            ),
            0,
            null,
            0,
            paint,
        )
        drawColor(HOME_RECOMMENDATION_REFLECTION_OVERLAY_COLOR, PorterDuff.Mode.OVERLAY)
        drawColor(HOME_RECOMMENDATION_REFLECTION_DARKEN_COLOR)
    }
    var blurredBitmap: Bitmap? = null
    try {
        val result = Toolkit.blur(
            processedBitmap,
            HOME_RECOMMENDATION_REFLECTION_BLUR_RADIUS,
        )
        blurredBitmap = result
        return result
    } finally {
        if (blurredBitmap !== processedBitmap) {
            processedBitmap.recycle()
        }
        if (
            smallBitmap !== sourceBitmap &&
            smallBitmap !== processedBitmap &&
            smallBitmap !== blurredBitmap
        ) {
            smallBitmap.recycle()
        }
    }
}

private fun Bitmap.centerCropAndScale(sizePx: Int): Bitmap {
    val cropSize = minOf(width, height)
    val croppedBitmap = Bitmap.createBitmap(
        this,
        (width - cropSize) / 2,
        (height - cropSize) / 2,
        cropSize,
        cropSize,
    )
    val scaledBitmap = Bitmap.createScaledBitmap(
        croppedBitmap,
        sizePx,
        sizePx,
        true,
    )
    if (croppedBitmap !== this && croppedBitmap !== scaledBitmap) {
        croppedBitmap.recycle()
    }
    return scaledBitmap
}

internal const val HOME_RECOMMENDATION_ARTWORK_SIZE_PX = 512
internal const val HOME_RECOMMENDATION_REFLECTION_SIZE_PX = 96
private const val HOME_RECOMMENDATION_REFLECTION_MESH_WIDTH = 5
private const val HOME_RECOMMENDATION_REFLECTION_MESH_HEIGHT = 5
private const val HOME_RECOMMENDATION_REFLECTION_SATURATION = 1.5f
private const val HOME_RECOMMENDATION_REFLECTION_BLUR_RADIUS = 25
private const val HOME_RECOMMENDATION_REFLECTION_OVERLAY_COLOR = 0x4D000000
private const val HOME_RECOMMENDATION_REFLECTION_DARKEN_COLOR = 0x4D000000
private const val HOME_RECOMMENDATION_REFLECTION_CACHE_NAMESPACE =
    "home-recommendation-reflection-v1"

private val HOME_RECOMMENDATION_REFLECTION_NORMALIZED_MESH = floatArrayOf(
    -0.2351f, -0.0967f, 0.2135f, -0.1414f, 0.9221f, -0.0908f,
    0.9221f, -0.0685f, 1.3027f, 0.0253f, 1.2351f, 0.1786f,
    -0.3768f, 0.1851f, 0.2f, 0.2f, 0.6615f, 0.3146f,
    0.9543f, 0f, 0.6969f, 0.1911f, 1f, 0.2f,
    0f, 0.4f, 0.2f, 0.4f, 0.0776f, 0.2318f,
    0.6f, 0.4f, 0.6615f, 0.3851f, 1f, 0.4f,
    0f, 0.6f, 0.1291f, 0.6f, 0.4f, 0.6f,
    0.4f, 0.4304f, 0.4264f, 0.5792f, 1.2029f, 0.8188f,
    -0.1192f, 1f, 0.6f, 0.8f, 0.4264f, 0.8104f,
    0.6f, 0.8f, 0.8f, 0.8f, 1f, 0.8f,
    0f, 1f, 0.0776f, 1.0283f, 0.4f, 1f,
    0.6f, 1f, 0.8f, 1f, 1.1868f, 1.0283f,
)
