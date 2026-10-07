package com.melox.player.ui.component.playback

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Shader
import android.graphics.Color as AndroidColor
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import top.yukonga.miuix.kmp.shader.isRuntimeShaderSupported

private const val DYNAMIC_FLOW_FRAME_INTERVAL_NANOS = 1_000_000_000L / 30
internal const val DYNAMIC_FLOW_DEFAULT_SPEED_TENTHS = 10
private const val DYNAMIC_FLOW_DEFAULT_BLUR = 60f
private const val DYNAMIC_FLOW_FRAME_BUFFER_COUNT = 3
private const val DYNAMIC_FLOW_MESH_COLUMNS = 5
private const val DYNAMIC_FLOW_MESH_ROWS = 5
private const val DYNAMIC_FLOW_MESH_TEMPLATE_COUNT = 6
private const val DYNAMIC_FLOW_MESH_INTERIOR_OFFSET = 0.20f
private const val DYNAMIC_FLOW_MESH_EDGE_OFFSET = 0.06f
private const val DYNAMIC_FLOW_MESH_CORNER_OFFSET = 0.04f
private const val DYNAMIC_FLOW_MESH_DRIFT = 0.63f
private const val DYNAMIC_FLOW_PREVIOUS_FRAME_ALPHA = 64
private const val DYNAMIC_FLOW_HIGH_PRECISION_CHANNEL_SHIFT = 8
private const val DYNAMIC_FLOW_HIGH_PRECISION_CHANNEL_MASK = 0xffffL
internal const val DYNAMIC_FLOW_ARTWORK_SATURATION = 2.5f
internal const val DYNAMIC_FLOW_BACKGROUND_DARKEN_AMOUNT = 0.25f
private val DynamicFlowFallbackColor = Color(0xFF242424)

@Stable
internal class DynamicFlowBackgroundState {
    internal var displayedFrame: Bitmap? = null
        private set
    private var currentPaint: Paint? = null
    private var previousPaint: Paint? = null
    private var previousFrame: Bitmap? = null
    private val shaderMatrix = Matrix()
    internal var elapsedMillis: Long = 0L
    internal var frameRevision by mutableIntStateOf(0)
        private set

    internal fun publishFrame(frame: Bitmap) {
        displayedFrame = frame
        currentPaint = shaderPaint(frame)
        previousPaint = null
        previousFrame = null
        frameRevision += 1
    }

    internal fun beginTransition(frame: Bitmap) {
        freezeTransition()
        val previous = displayedFrame ?: Bitmap.createBitmap(
            1, 1, Bitmap.Config.ARGB_8888,
        ).apply { eraseColor(DynamicFlowFallbackColor.toArgb()) }
        previousFrame = previous
        previousPaint = currentPaint ?: shaderPaint(previous)
        displayedFrame = frame
        currentPaint = shaderPaint(frame).apply { alpha = 0 }
        frameRevision += 1
    }

    internal fun updateTransition(progress: Float) {
        currentPaint?.alpha = (progress.coerceIn(0f, 1f) * 255f).roundToInt()
        frameRevision += 1
    }

    // A cancelled fade is flattened once, before its source buffers can be reused.
    internal fun freezeTransition() {
        val previous = previousFrame ?: return
        val current = displayedFrame ?: return
        val snapshot = Bitmap.createBitmap(
            max(previous.width, current.width),
            max(previous.height, current.height),
            Bitmap.Config.ARGB_8888,
        )
        Canvas(snapshot).apply {
            drawColor(DynamicFlowFallbackColor.toArgb())
            drawTo(this, snapshot.width.toFloat(), snapshot.height.toFloat())
        }
        publishFrame(snapshot)
    }

    internal fun drawTo(canvas: Canvas, width: Float, height: Float) {
        fun draw(frame: Bitmap?, paint: Paint?) {
            if (frame == null || paint == null) return
            shaderMatrix.setScale(width / frame.width, height / frame.height)
            paint.shader.setLocalMatrix(shaderMatrix)
            canvas.drawRect(0f, 0f, width, height, paint)
        }
        draw(previousFrame, previousPaint)
        draw(displayedFrame, currentPaint)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    internal fun drawDitheredTo(
        canvas: Canvas,
        width: Float,
        height: Float,
        painter: DynamicFlowDitherPainter,
    ): Boolean {
        val current = displayedFrame ?: return false
        val currentShader = currentPaint?.shader as? BitmapShader ?: return false
        // RuntimeShader inputs do not inherit Paint's bitmap filtering flag.
        currentShader.setFilterMode(BitmapShader.FILTER_MODE_LINEAR)
        shaderMatrix.setScale(width / current.width, height / current.height)
        currentShader.setLocalMatrix(shaderMatrix)
        val previous = previousFrame
        val previousShader = previousPaint?.shader as? BitmapShader
        if (previous != null && previousShader != null) {
            previousShader.setFilterMode(BitmapShader.FILTER_MODE_LINEAR)
            shaderMatrix.setScale(width / previous.width, height / previous.height)
            previousShader.setLocalMatrix(shaderMatrix)
        }
        painter.draw(
            canvas, width, height, currentShader, previousShader ?: currentShader,
            (currentPaint?.alpha ?: 255) / 255f,
        )
        return true
    }

    private fun shaderPaint(frame: Bitmap) = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        shader = BitmapShader(frame, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }
}

@Composable
internal fun rememberDynamicFlowBackgroundState(): DynamicFlowBackgroundState =
    remember { DynamicFlowBackgroundState() }

/** Multi-layer cover background. */
@Composable
internal fun DynamicFlowBackground(
    state: DynamicFlowBackgroundState,
    artwork: Bitmap?,
    animate: Boolean,
    modifier: Modifier = Modifier,
    onStatusBarBackgroundDarkChanged: (Boolean) -> Unit = {},
    artworkLoading: Boolean = false,
    backgroundColor: Color? = null,
    onFrameReady: () -> Unit = {},
) {
    val densityDpi = LocalContext.current.resources.displayMetrics.densityDpi
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val animationEnabled by rememberUpdatedState(
        animate && lifecycleState.isAtLeast(Lifecycle.State.RESUMED),
    )
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val currentDensityDpi by rememberUpdatedState(densityDpi)
    val frameBufferPool = remember { DynamicFlowFrameBufferPool() }
    val ditherPainter = remember {
        if (isRuntimeShaderSupported()) DynamicFlowDitherPainter() else null
    }
    val scrim = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.18f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.30f),
            ),
        )
    }

    LaunchedEffect(artwork, artworkLoading, backgroundColor) {
        if (artworkLoading) return@LaunchedEffect
        state.freezeTransition()
        frameBufferPool.resetHistory()
        snapshotFlow { viewportSize }.first { it.width > 0 && it.height > 0 }
        val cover = withContext(Dispatchers.Default) { artwork?.scaledForDynamicFlowSource() }
        val meshSeed = withContext(Dispatchers.Default) { cover?.dynamicFlowMeshSeed() ?: 0 }
        val resolvedBackgroundColor = withContext(Dispatchers.Default) {
            backgroundColor ?: dynamicFlowBackgroundColor(artwork)
        }
        var renderedSize = IntSize.Zero
        var renderedDensity = 0
        suspend fun renderFrame(): Bitmap {
            val size = viewportSize
            val density = currentDensityDpi
            val visibleFrame = state.displayedFrame
            val frame = withContext(Dispatchers.Default) {
                if (cover == null) {
                    Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply {
                        eraseColor(resolvedBackgroundColor.toArgb())
                    }
                } else {
                    createDynamicFlowFrameBitmap(
                        frameBufferPool = frameBufferPool,
                        displayedFrame = visibleFrame,
                        cover = cover,
                        meshSeed = meshSeed,
                        viewportWidth = size.width,
                        viewportHeight = size.height,
                        timeMillis = scaledDynamicFlowTimeMs(
                            state.elapsedMillis, DYNAMIC_FLOW_DEFAULT_SPEED_TENTHS,
                        ),
                        densityDpi = density,
                        blur = DYNAMIC_FLOW_DEFAULT_BLUR,
                        washPrimaryArgb = blendDynamicFlowColors(
                            resolvedBackgroundColor, Color.Black, 0.24f,
                        ).copy(alpha = 0.24f).toArgb(),
                        washSecondaryArgb = Color(0xFF121316).copy(alpha = 0.16f).toArgb(),
                        backgroundArgb = resolvedBackgroundColor.toArgb(),
                    )
                }
            }
            renderedSize = size
            renderedDensity = density
            return frame
        }

        val target = renderFrame()
        state.beginTransition(target)
        Animatable(0f).animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = PLAYBACK_BACKGROUND_TRANSITION_DURATION_MILLIS,
                easing = PLAYER_TRACK_ARTWORK_CROSSFADE_EASING,
            ),
        ) {
            state.updateTransition(value)
        }
        state.publishFrame(if (
            renderedSize != viewportSize || renderedDensity != currentDensityDpi
        ) {
            renderFrame()
        } else {
            target
        })
        onFrameReady()
        if (cover == null) return@LaunchedEffect

        var previousFrameNanos: Long? = null
        while (true) {
            if (animationEnabled) {
                val frameNanos = withFrameNanos { it }
                if (!animationEnabled) continue
                if (!shouldRenderDynamicFlowFrame(previousFrameNanos, frameNanos)) continue
                state.elapsedMillis = advanceDynamicFlowClockMillis(
                    state.elapsedMillis, previousFrameNanos, frameNanos,
                )
                previousFrameNanos = frameNanos
                state.publishFrame(renderFrame())
            } else {
                previousFrameNanos = null
                if (viewportSize.width > 0 && viewportSize.height > 0 &&
                    (renderedSize != viewportSize || renderedDensity != currentDensityDpi)
                ) {
                    state.publishFrame(renderFrame())
                }
                snapshotFlow { Triple(animationEnabled, viewportSize, currentDensityDpi) }
                    .first { (enabled, size, density) ->
                        enabled || (size.width > 0 && size.height > 0 &&
                            (size != renderedSize || density != renderedDensity))
                    }
            }
        }
    }

    LaunchedEffect(artwork) {
        onStatusBarBackgroundDarkChanged(true)
    }

    Box(
        modifier = modifier
            .background(DynamicFlowFallbackColor)
            .clipToBounds()
            .onSizeChanged { viewportSize = it },
    ) {
        ComposeCanvas(modifier = Modifier.fillMaxSize()) {
            // Snapshot observation stays in the draw phase, so frame publication
            // invalidates only this background node.
            state.frameRevision
            val canvas = drawContext.canvas.nativeCanvas
            val dithered = isRuntimeShaderSupported() && ditherPainter != null && canvas.isHardwareAccelerated &&
                state.drawDitheredTo(canvas, size.width, size.height, ditherPainter)
            if (!dithered) {
                state.drawTo(canvas, size.width, size.height)
                drawRect(scrim)
            }
        }
    }
}

internal fun scaledDynamicFlowTimeMs(elapsedMillis: Long, speedTenths: Int): Long =
    elapsedMillis.coerceAtLeast(0L) * speedTenths.coerceIn(5, 60) / 10L

internal fun advanceDynamicFlowClockMillis(
    elapsedMillis: Long,
    previousFrameNanos: Long?,
    frameNanos: Long,
): Long {
    if (previousFrameNanos == null) return elapsedMillis.coerceAtLeast(0L)
    val deltaMillis = ((frameNanos - previousFrameNanos).coerceAtLeast(0L)) / 1_000_000L
    return elapsedMillis.coerceAtLeast(0L) + deltaMillis
}

private fun Bitmap.scaledForDynamicFlowSource(maxDimension: Int = 256): Bitmap {
    val longest = max(width, height)
    if (longest <= maxDimension || longest <= 0) return this
    val scale = maxDimension.toFloat() / longest
    return Bitmap.createScaledBitmap(
        this,
        (width * scale).roundToInt().coerceAtLeast(1),
        (height * scale).roundToInt().coerceAtLeast(1),
        true,
    )
}

internal fun dynamicFlowDownsampleFactor(densityDpi: Int): Float =
    if (densityDpi >= 420) 20f else 16f

internal fun dynamicFlowBlurRadius(blur: Float, densityDpi: Int): Int {
    val baseRadius = (
        ((blur.coerceIn(30f, 100f) - 30f) / 70f) * 17f + 8f
    ).roundToInt().coerceIn(8, 25)
    return if (densityDpi >= 420) {
        (baseRadius * 24f / dynamicFlowDownsampleFactor(densityDpi))
            .roundToInt()
            .coerceIn(8, 25)
    } else {
        baseRadius
    }
}

private fun createDynamicFlowFrameBitmap(
    frameBufferPool: DynamicFlowFrameBufferPool,
    displayedFrame: Bitmap?,
    cover: Bitmap,
    meshSeed: Int,
    viewportWidth: Int,
    viewportHeight: Int,
    timeMillis: Long,
    densityDpi: Int,
    blur: Float,
    washPrimaryArgb: Int,
    washSecondaryArgb: Int,
    backgroundArgb: Int,
): Bitmap {
    val downsample = dynamicFlowDownsampleFactor(densityDpi)
    val width = ((viewportWidth * 1.3f) / downsample).roundToInt().coerceAtLeast(1)
    val height = ((viewportHeight * 1.3f) / downsample).roundToInt().coerceAtLeast(1)
    return frameBufferPool.render(
        displayedFrame = displayedFrame,
        cover = cover,
        meshSeed = meshSeed,
        width = width,
        height = height,
        timeMillis = timeMillis,
        blurRadius = dynamicFlowBlurRadius(blur, densityDpi),
        washPrimaryArgb = washPrimaryArgb,
        washSecondaryArgb = washSecondaryArgb,
        backgroundArgb = backgroundArgb,
    )
}

internal class DynamicFlowFrameBufferPool {
    private val buffers = List(DYNAMIC_FLOW_FRAME_BUFFER_COUNT) { DynamicFlowFrameBuffer() }
    private var nextBufferIndex = 0
    private var previousCompleteFrame: Bitmap? = null

    fun resetHistory() {
        previousCompleteFrame = null
    }

    fun render(
        displayedFrame: Bitmap?,
        cover: Bitmap,
        meshSeed: Int,
        width: Int,
        height: Int,
        timeMillis: Long,
        blurRadius: Int,
        washPrimaryArgb: Int,
        washSecondaryArgb: Int,
        backgroundArgb: Int,
    ): Bitmap {
        var bufferIndex = nextBufferIndex
        for (offset in buffers.indices) {
            val candidateIndex = (nextBufferIndex + offset) % buffers.size
            val candidate = buffers[candidateIndex].outputBitmap
            if (candidate !== displayedFrame && candidate !== previousCompleteFrame) {
                bufferIndex = candidateIndex
                break
            }
        }
        nextBufferIndex = (bufferIndex + 1) % buffers.size
        val previousFrame = previousCompleteFrame
            ?.takeIf { it !== buffers[bufferIndex].outputBitmap }
        return buffers[bufferIndex].render(
            cover = cover,
            meshSeed = meshSeed,
            previousFrame = previousFrame,
            width = width,
            height = height,
            timeMillis = timeMillis,
            blurRadius = blurRadius,
            washPrimaryArgb = washPrimaryArgb,
            washSecondaryArgb = washSecondaryArgb,
            backgroundArgb = backgroundArgb,
        ).also { previousCompleteFrame = it }
    }
}

private class DynamicFlowFrameBuffer {
    private var sourceBitmap: Bitmap? = null
    private var warpedBitmap: Bitmap? = null
    private var blurredBitmap: Bitmap? = null
    var outputBitmap: Bitmap? = null
        private set
    private var pixels = IntArray(0)
    private var highPrecisionPixels = LongArray(0)
    private var highPrecisionTemporaryPixels = LongArray(0)
    private var meshVertices = FloatArray(0)
    private var blurBoxRadius = -1
    private var blurBoxSizes = IntArray(0)
    private val sourceCanvas = Canvas()
    private val warpedCanvas = Canvas()
    private val outputCanvas = Canvas()
    private val artworkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
        colorFilter = ColorMatrixColorFilter(
            ColorMatrix().apply { setSaturation(DYNAMIC_FLOW_ARTWORK_SATURATION) },
        )
    }
    private val meshPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    private val previousFramePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        alpha = DYNAMIC_FLOW_PREVIOUS_FRAME_ALPHA
        isFilterBitmap = true
    }
    private val firstLayerMatrix = Matrix()
    private val secondLayerMatrix = Matrix()
    private val thirdLayerMatrix = Matrix()

    fun render(
        cover: Bitmap,
        meshSeed: Int,
        previousFrame: Bitmap?,
        width: Int,
        height: Int,
        timeMillis: Long,
        blurRadius: Int,
        washPrimaryArgb: Int,
        washSecondaryArgb: Int,
        backgroundArgb: Int,
    ): Bitmap {
        ensureCapacity(width, height)
        val source = requireNotNull(sourceBitmap)
        val warped = requireNotNull(warpedBitmap)
        val blurred = requireNotNull(blurredBitmap)
        val output = requireNotNull(outputBitmap)
        sourceCanvas.drawColor(backgroundArgb)

        val diagonal = (max(width, height) * 1.3f).roundToInt().coerceAtLeast(1).toFloat()
        val coverScale = diagonal / max(cover.height, 1)
        val translateX = -(diagonal - width) / 2f
        val translateY = -(diagonal - height) / 2f
        val rotatePivot = diagonal / 2f
        drawDynamicFlowLayer(
            canvas = sourceCanvas,
            cover = cover,
            paint = artworkPaint,
            matrix = firstLayerMatrix,
            scale = coverScale,
            rotatePivot = rotatePivot,
            translateX = translateX,
            translateY = translateY,
            viewWidth = width.toFloat(),
            viewHeight = height.toFloat(),
            rotation = (timeMillis % 120_000L) / 120_000f * -360f,
            offsetXFactor = dynamicFlowLayerMotion(timeMillis, 0, 0) * 0.10f,
            offsetYFactor = dynamicFlowLayerMotion(timeMillis, 0, 1) * 0.10f,
        )
        drawDynamicFlowLayer(
            canvas = sourceCanvas,
            cover = cover,
            paint = artworkPaint,
            matrix = secondLayerMatrix,
            scale = coverScale,
            rotatePivot = rotatePivot,
            translateX = translateX,
            translateY = translateY,
            viewWidth = width.toFloat(),
            viewHeight = height.toFloat(),
            rotation = (timeMillis % 90_000L) / 90_000f * 360f,
            offsetXFactor = -0.95f + dynamicFlowLayerMotion(timeMillis, 1, 0) * 0.28f,
            offsetYFactor = -0.7f + dynamicFlowLayerMotion(timeMillis, 1, 1) * 0.28f,
        )
        drawDynamicFlowLayer(
            canvas = sourceCanvas,
            cover = cover,
            paint = artworkPaint,
            matrix = thirdLayerMatrix,
            scale = coverScale,
            rotatePivot = rotatePivot,
            translateX = translateX,
            translateY = translateY,
            viewWidth = width.toFloat(),
            viewHeight = height.toFloat(),
            rotation = (timeMillis % 70_000L) / 70_000f * 360f,
            offsetXFactor = -0.5f + dynamicFlowLayerMotion(timeMillis, 2, 0) * 0.32f,
            offsetYFactor = 0.7f + dynamicFlowLayerMotion(timeMillis, 2, 1) * 0.32f,
            extraRotation = (timeMillis % 70_000L) / 70_000f * 360f,
        )

        fillDynamicFlowMeshVertices(meshVertices, width, height, timeMillis, meshSeed)
        // Keep mesh edges opaque so history blending cannot change their coverage.
        warped.eraseColor(backgroundArgb or 0xff000000.toInt())
        warpedCanvas.drawBitmapMesh(
            source,
            DYNAMIC_FLOW_MESH_COLUMNS,
            DYNAMIC_FLOW_MESH_ROWS,
            meshVertices,
            0,
            null,
            0,
            meshPaint,
        )
        warpedCanvas.drawColor(washPrimaryArgb)
        warpedCanvas.drawColor(washSecondaryArgb)
        if (blurBoxRadius != blurRadius) {
            blurBoxRadius = blurRadius
            blurBoxSizes = dynamicFlowGaussianBoxSizes(blurRadius)
        }
        blurDynamicFlowBitmap(
            source = warped,
            target = blurred,
            pixels = pixels,
            highPrecisionPixels = highPrecisionPixels,
            highPrecisionTemporaryPixels = highPrecisionTemporaryPixels,
            boxSizes = blurBoxSizes,
        )
        outputCanvas.drawBitmap(
            blurred,
            -((blurred.width - output.width) / 2).toFloat(),
            -((blurred.height - output.height) / 2).toFloat(),
            null,
        )
        if (previousFrame?.width == output.width && previousFrame.height == output.height) {
            outputCanvas.drawBitmap(previousFrame, 0f, 0f, previousFramePaint)
        }
        return output
    }

    private fun ensureCapacity(width: Int, height: Int) {
        val cropWidth = (width / 1.3f).roundToInt().coerceIn(1, width)
        val cropHeight = (height / 1.3f).roundToInt().coerceIn(1, height)
        if (sourceBitmap?.width == width && sourceBitmap?.height == height &&
            outputBitmap?.width == cropWidth && outputBitmap?.height == cropHeight
        ) return
        sourceBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        warpedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        blurredBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        outputBitmap = Bitmap.createBitmap(cropWidth, cropHeight, Bitmap.Config.ARGB_8888)
        pixels = IntArray(width * height)
        highPrecisionPixels = LongArray(width * height)
        highPrecisionTemporaryPixels = LongArray(width * height)
        meshVertices = FloatArray((DYNAMIC_FLOW_MESH_COLUMNS + 1) * (DYNAMIC_FLOW_MESH_ROWS + 1) * 2)
        sourceCanvas.setBitmap(sourceBitmap)
        warpedCanvas.setBitmap(warpedBitmap)
        outputCanvas.setBitmap(outputBitmap)
    }
}

private fun drawDynamicFlowLayer(
    canvas: Canvas,
    cover: Bitmap,
    paint: Paint,
    matrix: Matrix,
    scale: Float,
    rotatePivot: Float,
    translateX: Float,
    translateY: Float,
    viewWidth: Float,
    viewHeight: Float,
    rotation: Float,
    offsetXFactor: Float = 0f,
    offsetYFactor: Float = 0f,
    extraRotation: Float? = null,
) {
    matrix.setScale(scale, scale)
    matrix.postRotate(rotation, rotatePivot, rotatePivot)
    matrix.postTranslate(translateX, translateY)
    if (offsetXFactor != 0f || offsetYFactor != 0f) {
        matrix.postTranslate(viewWidth * offsetXFactor, viewHeight * offsetYFactor)
    }
    if (extraRotation != null) {
        matrix.postRotate(extraRotation, viewWidth / 2f, viewHeight / 2f)
    }
    canvas.drawBitmap(cover, matrix, paint)
}

/** Bounded, independent motion without frame-random noise or phase resets. */
internal fun dynamicFlowLayerMotion(timeMillis: Long, layer: Int, axis: Int): Float {
    val seconds = timeMillis.toDouble() / 1000.0
    val phase = layer * 1.73 + axis * 2.31
    val period = 23.0 + layer * 7.0 + axis * 5.0
    return (sin(seconds * 2.0 * PI / period + phase) * 0.65 +
        sin(seconds * 2.0 * PI / (period * 1.61) + phase * 0.73) * 0.35).toFloat()
}

internal fun fillDynamicFlowMeshVertices(
    output: FloatArray,
    width: Int,
    height: Int,
    timeMillis: Long,
    seed: Int,
) {
    require(output.size == (DYNAMIC_FLOW_MESH_COLUMNS + 1) * (DYNAMIC_FLOW_MESH_ROWS + 1) * 2)
    // Keep artwork identity while redistributing the same displacement budget to motion.
    val phase = timeMillis.toDouble() / 30_000.0 * (PI * 2.0)
    val templateIndex =
        ((seed.toLong() and 0x7fffffffL) % DYNAMIC_FLOW_MESH_TEMPLATE_COUNT).toInt()
    val templateSeed = DYNAMIC_FLOW_MESH_TEMPLATE_SEEDS[templateIndex]
    val segment = timeMillis.coerceAtLeast(0L) / 24_000L
    val fraction = (timeMillis.coerceAtLeast(0L) % 24_000L) / 24_000f
    val blend = fraction * fraction * (3f - 2f * fraction)
    val fromSeed = DYNAMIC_FLOW_MESH_TEMPLATE_SEEDS[
        ((templateIndex + segment) % DYNAMIC_FLOW_MESH_TEMPLATE_COUNT).toInt()
    ]
    val toSeed = DYNAMIC_FLOW_MESH_TEMPLATE_SEEDS[
        ((templateIndex + segment + 1) % DYNAMIC_FLOW_MESH_TEMPLATE_COUNT).toInt()
    ]
    fun shapeNoise(column: Int, row: Int, axis: Int): Float {
        val from = dynamicFlowMeshUnitNoise(seed, fromSeed, column, row, axis)
        val to = dynamicFlowMeshUnitNoise(seed, toSeed, column, row, axis)
        return from + (to - from) * blend
    }
    var index = 0
    for (row in 0..DYNAMIC_FLOW_MESH_ROWS) {
        for (column in 0..DYNAMIC_FLOW_MESH_COLUMNS) {
            val x = width * column.toFloat() / DYNAMIC_FLOW_MESH_COLUMNS
            val y = height * row.toFloat() / DYNAMIC_FLOW_MESH_ROWS
            val isEdge = column == 0 || column == DYNAMIC_FLOW_MESH_COLUMNS ||
                row == 0 || row == DYNAMIC_FLOW_MESH_ROWS
            val isCorner = isEdge &&
                (column == 0 || column == DYNAMIC_FLOW_MESH_COLUMNS) &&
                (row == 0 || row == DYNAMIC_FLOW_MESH_ROWS)
            val maxOffset = when {
                isCorner -> DYNAMIC_FLOW_MESH_CORNER_OFFSET
                isEdge -> DYNAMIC_FLOW_MESH_EDGE_OFFSET
                else -> DYNAMIC_FLOW_MESH_INTERIOR_OFFSET
            }

            val localHorizontal = shapeNoise(column, row, axis = 0)
            val coarseHorizontal = shapeNoise(column / 2, row / 2, axis = 2)
            val localVertical = shapeNoise(column, row, axis = 1)
            val coarseVertical = shapeNoise(column / 2, row / 2, axis = 3)
            val staticHorizontal = localHorizontal * 0.68f + coarseHorizontal * 0.32f
            val staticVertical = localVertical * 0.68f + coarseVertical * 0.32f

            val horizontalPhase = dynamicFlowMeshPhase(seed, templateSeed, column, row, axis = 4)
            val verticalPhase = dynamicFlowMeshPhase(seed, templateSeed, column, row, axis = 5)
            val horizontalSpeed = dynamicFlowMeshSpeed(seed, templateSeed, column, row, axis = 6)
            val verticalSpeed = dynamicFlowMeshSpeed(seed, templateSeed, column, row, axis = 7)
            val horizontalDrift = sin(phase * horizontalSpeed + horizontalPhase).toFloat()
            val verticalDrift = cos(phase * verticalSpeed + verticalPhase).toFloat()

            output[index++] = x +
                (staticHorizontal * 0.55f + horizontalDrift * DYNAMIC_FLOW_MESH_DRIFT) * width * maxOffset
            output[index++] = y +
                (staticVertical * 0.55f + verticalDrift * DYNAMIC_FLOW_MESH_DRIFT) * height * maxOffset
        }
    }
}

private val DYNAMIC_FLOW_MESH_TEMPLATE_SEEDS = intArrayOf(
    0x13579BDF,
    0x2468ACE1,
    0x5A17C0DE,
    0x6D2B79F5,
    0x1B873593,
    0x9E3779B9.toInt(),
)

private fun dynamicFlowMeshHash(
    seed: Int,
    templateSeed: Int,
    column: Int,
    row: Int,
    axis: Int,
): Int {
    var value = seed xor templateSeed
    value = value xor (column + 1) * 0x45D9F3B
    value = value xor (row + 1) * 0x119DE1F3
    value = value xor (axis + 1) * 0x27D4EB2D
    value = (value xor (value ushr 16)) * 0x7FEB352D
    value = (value xor (value ushr 15)) * 0x846CA68B.toInt()
    return value xor (value ushr 16)
}

private fun dynamicFlowMeshUnitNoise(
    seed: Int,
    templateSeed: Int,
    column: Int,
    row: Int,
    axis: Int,
): Float {
    val value = dynamicFlowMeshHash(seed, templateSeed, column, row, axis)
    return ((value ushr 8) and 0xffff) / 32_767.5f - 1f
}

private fun dynamicFlowMeshPhase(
    seed: Int,
    templateSeed: Int,
    column: Int,
    row: Int,
    axis: Int,
): Float {
    val value = dynamicFlowMeshHash(seed, templateSeed, column, row, axis)
    return ((value ushr 8) and 0xffff) / 65_535f * (PI * 2f).toFloat()
}

private fun dynamicFlowMeshSpeed(
    seed: Int,
    templateSeed: Int,
    column: Int,
    row: Int,
    axis: Int,
): Float {
    val value = dynamicFlowMeshHash(seed, templateSeed, column, row, axis)
    return 0.42f + ((value ushr 8) and 0xffff) / 65_535f * 0.34f
}

private fun Bitmap.dynamicFlowMeshSeed(): Int {
    val positions = intArrayOf(
        0, 0,
        width / 2, height / 2,
        width - 1, height - 1,
    )
    var hash = 17
    for (index in positions.indices step 2) {
        hash = hash * 31 + getPixel(positions[index], positions[index + 1])
    }
    return hash
}

internal fun shouldRenderDynamicFlowFrame(previousFrameNanos: Long?, frameNanos: Long): Boolean =
    previousFrameNanos == null ||
        // Allow display-clock rounding near the 60 fps boundary, not a catch-up queue.
        frameNanos - previousFrameNanos >= DYNAMIC_FLOW_FRAME_INTERVAL_NANOS - 1_000_000L

private fun blurDynamicFlowBitmap(
    source: Bitmap,
    target: Bitmap,
    pixels: IntArray,
    highPrecisionPixels: LongArray,
    highPrecisionTemporaryPixels: LongArray,
    boxSizes: IntArray,
) {
    if (boxSizes.isEmpty()) {
        Canvas(target).drawBitmap(source, 0f, 0f, null)
        return
    }
    val width = source.width
    val height = source.height
    if (width <= 1 || height <= 1) {
        Canvas(target).drawBitmap(source, 0f, 0f, null)
        return
    }

    source.getPixels(pixels, 0, width, 0, 0, width, height)
    for (index in pixels.indices) {
        highPrecisionPixels[index] = highPrecisionDynamicFlowPixel(pixels[index])
    }
    for (boxSize in boxSizes) {
        if (boxSize == 1) continue
        val radius = boxSize / 2
        blurDynamicFlowHorizontal(
            source = highPrecisionPixels,
            target = highPrecisionTemporaryPixels,
            width = width,
            height = height,
            radius = radius,
        )
        blurDynamicFlowVertical(
            source = highPrecisionTemporaryPixels,
            target = highPrecisionPixels,
            width = width,
            height = height,
            radius = radius,
        )
    }
    for (index in pixels.indices) {
        pixels[index] = dynamicFlowArgbPixel(highPrecisionPixels[index])
    }
    target.setPixels(pixels, 0, width, 0, 0, width, height)
}

internal fun dynamicFlowGaussianBoxSizes(radius: Int): IntArray {
    val boundedRadius = radius.coerceIn(1, 25)
    val sourceWidth = boundedRadius * 2 + 1
    val targetVariance = (sourceWidth * sourceWidth - 1) / 12f
    val passCount = 3
    val idealWidth = sqrt((12f * targetVariance / passCount) + 1f)
    var lowerWidth = floor(idealWidth).toInt()
    if (lowerWidth % 2 == 0) lowerWidth -= 1
    val upperWidth = lowerWidth + 2
    val lowerPassCount = round(
        (12f * targetVariance -
            passCount * lowerWidth * lowerWidth -
            4f * passCount * lowerWidth -
            3f * passCount) /
            (-4f * lowerWidth - 4f),
    ).toInt().coerceIn(0, passCount)
    return IntArray(passCount) { index ->
        if (index < lowerPassCount) lowerWidth else upperWidth
    }
}

internal fun blurDynamicFlowHorizontal(
    source: LongArray,
    target: LongArray,
    width: Int,
    height: Int,
    radius: Int,
) {
    val window = radius * 2 + 1
    for (y in 0 until height) {
        val rowStart = y * width
        var alpha = 0
        var red = 0
        var green = 0
        var blue = 0
        for (offset in -radius..radius) {
            val pixel = source[rowStart + offset.coerceIn(0, width - 1)]
            alpha += dynamicFlowHighPrecisionChannel(pixel, 48)
            red += dynamicFlowHighPrecisionChannel(pixel, 32)
            green += dynamicFlowHighPrecisionChannel(pixel, 16)
            blue += dynamicFlowHighPrecisionChannel(pixel, 0)
        }
        for (x in 0 until width) {
            target[rowStart + x] = packDynamicFlowHighPrecisionPixel(
                alpha = (alpha + window / 2) / window,
                red = (red + window / 2) / window,
                green = (green + window / 2) / window,
                blue = (blue + window / 2) / window,
            )
            val outgoing = source[rowStart + (x - radius).coerceIn(0, width - 1)]
            val incoming = source[rowStart + (x + radius + 1).coerceIn(0, width - 1)]
            alpha += dynamicFlowHighPrecisionChannel(incoming, 48) -
                dynamicFlowHighPrecisionChannel(outgoing, 48)
            red += dynamicFlowHighPrecisionChannel(incoming, 32) -
                dynamicFlowHighPrecisionChannel(outgoing, 32)
            green += dynamicFlowHighPrecisionChannel(incoming, 16) -
                dynamicFlowHighPrecisionChannel(outgoing, 16)
            blue += dynamicFlowHighPrecisionChannel(incoming, 0) -
                dynamicFlowHighPrecisionChannel(outgoing, 0)
        }
    }
}

internal fun blurDynamicFlowVertical(
    source: LongArray,
    target: LongArray,
    width: Int,
    height: Int,
    radius: Int,
) {
    val window = radius * 2 + 1
    for (x in 0 until width) {
        var alpha = 0
        var red = 0
        var green = 0
        var blue = 0
        for (offset in -radius..radius) {
            val pixel = source[offset.coerceIn(0, height - 1) * width + x]
            alpha += dynamicFlowHighPrecisionChannel(pixel, 48)
            red += dynamicFlowHighPrecisionChannel(pixel, 32)
            green += dynamicFlowHighPrecisionChannel(pixel, 16)
            blue += dynamicFlowHighPrecisionChannel(pixel, 0)
        }
        for (y in 0 until height) {
            target[y * width + x] = packDynamicFlowHighPrecisionPixel(
                alpha = (alpha + window / 2) / window,
                red = (red + window / 2) / window,
                green = (green + window / 2) / window,
                blue = (blue + window / 2) / window,
            )
            val outgoing = source[(y - radius).coerceIn(0, height - 1) * width + x]
            val incoming = source[(y + radius + 1).coerceIn(0, height - 1) * width + x]
            alpha += dynamicFlowHighPrecisionChannel(incoming, 48) -
                dynamicFlowHighPrecisionChannel(outgoing, 48)
            red += dynamicFlowHighPrecisionChannel(incoming, 32) -
                dynamicFlowHighPrecisionChannel(outgoing, 32)
            green += dynamicFlowHighPrecisionChannel(incoming, 16) -
                dynamicFlowHighPrecisionChannel(outgoing, 16)
            blue += dynamicFlowHighPrecisionChannel(incoming, 0) -
                dynamicFlowHighPrecisionChannel(outgoing, 0)
        }
    }
}

private fun highPrecisionDynamicFlowPixel(pixel: Int): Long = packDynamicFlowHighPrecisionPixel(
    alpha = (pixel ushr 24 and 0xff) shl DYNAMIC_FLOW_HIGH_PRECISION_CHANNEL_SHIFT,
    red = (pixel ushr 16 and 0xff) shl DYNAMIC_FLOW_HIGH_PRECISION_CHANNEL_SHIFT,
    green = (pixel ushr 8 and 0xff) shl DYNAMIC_FLOW_HIGH_PRECISION_CHANNEL_SHIFT,
    blue = (pixel and 0xff) shl DYNAMIC_FLOW_HIGH_PRECISION_CHANNEL_SHIFT,
)

private fun packDynamicFlowHighPrecisionPixel(
    alpha: Int,
    red: Int,
    green: Int,
    blue: Int,
): Long =
    // Inputs are byte channels shifted by eight, or rounded averages of them.
    // Both stay in 0..65280, so per-channel saturation is unnecessary.
    (alpha.toLong() shl 48) or
        (red.toLong() shl 32) or
        (green.toLong() shl 16) or
        blue.toLong()

private fun dynamicFlowHighPrecisionChannel(pixel: Long, shift: Int): Int =
    ((pixel ushr shift) and DYNAMIC_FLOW_HIGH_PRECISION_CHANNEL_MASK).toInt()

private fun dynamicFlowArgbPixel(pixel: Long): Int {
    fun channel(shift: Int): Int = (
        dynamicFlowHighPrecisionChannel(pixel, shift) +
            (1 shl (DYNAMIC_FLOW_HIGH_PRECISION_CHANNEL_SHIFT - 1))
        ) ushr DYNAMIC_FLOW_HIGH_PRECISION_CHANNEL_SHIFT
    return (channel(48) shl 24) or
        (channel(32) shl 16) or
        (channel(16) shl 8) or
        channel(0)
}

private fun defaultDynamicFlowBackgroundColor(): Color = Color(0xFF0B0B0D)

internal fun dynamicFlowBackgroundColor(bitmap: Bitmap?): Color {
    val representative = representativeDynamicFlowAccent(bitmap)
        ?: return if (bitmap == null) {
            DynamicFlowFallbackColor
        } else {
            defaultDynamicFlowBackgroundColor()
        }
    val accent = representative.toDynamicFlowAccent()
    return accent.darken(DYNAMIC_FLOW_BACKGROUND_DARKEN_AMOUNT)
}

private fun representativeDynamicFlowAccent(bitmap: Bitmap?): Color? {
    if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) return null
    val sampleStep = (min(bitmap.width, bitmap.height) / 36).coerceAtLeast(1)
    val buckets = linkedMapOf<Int, LongArray>()
    val fallback = LongArray(4)
    val hsv = FloatArray(3)
    var sampled = 0
    var brightNeutral = 0
    var eligible = 0

    var y = 0
    while (y < bitmap.height) {
        var x = 0
        while (x < bitmap.width) {
            val pixel = bitmap.getPixel(x, y)
            if (AndroidColor.alpha(pixel) > 24) {
                val red = AndroidColor.red(pixel)
                val green = AndroidColor.green(pixel)
                val blue = AndroidColor.blue(pixel)
                AndroidColor.RGBToHSV(red, green, blue, hsv)
                val saturation = hsv[1]
                val value = hsv[2]

                sampled += 1
                fallback[0] += 1L
                fallback[1] += red.toLong()
                fallback[2] += green.toLong()
                fallback[3] += blue.toLong()
                if (value > 0.78f && saturation < 0.18f) brightNeutral += 1

                if (value > 0.08f && !(value > 0.94f && saturation < 0.20f)) {
                    eligible += 1
                    val key = ((red ushr 4) shl 8) or
                        ((green ushr 4) shl 4) or
                        (blue ushr 4)
                    val bucket = buckets.getOrPut(key) { LongArray(4) }
                    bucket[0] += 1L
                    bucket[1] += red.toLong()
                    bucket[2] += green.toLong()
                    bucket[3] += blue.toLong()
                }
            }
            x += sampleStep
        }
        y += sampleStep
    }
    if (fallback[0] == 0L) return null
    if (
        sampled > 0 &&
        brightNeutral.toFloat() / sampled > 0.56f &&
        eligible.toFloat() / sampled < 0.24f
    ) {
        val count = fallback[0].coerceAtLeast(1L)
        return Color(
            (fallback[1] / count).toInt(),
            (fallback[2] / count).toInt(),
            (fallback[3] / count).toInt(),
        )
    }

    val best = buckets.values.maxByOrNull { bucket ->
        val count = bucket[0].coerceAtLeast(1L)
        val red = (bucket[1] / count).toInt()
        val green = (bucket[2] / count).toInt()
        val blue = (bucket[3] / count).toInt()
        AndroidColor.RGBToHSV(red, green, blue, hsv)
        val luminance = (0.2126f * red + 0.7152f * green + 0.0722f * blue) / 255f
        val balance = 1f - abs(luminance - 0.50f).coerceIn(0f, 0.50f) * 1.25f
        count.toFloat() * (0.55f + hsv[1] * 1.65f) * (0.75f + balance * 0.55f)
    } ?: fallback
    val count = best[0].coerceAtLeast(1L)
    return Color(
        (best[1] / count).toInt(),
        (best[2] / count).toInt(),
        (best[3] / count).toInt(),
    )
}

private fun Color.toDynamicFlowAccent(): Color {
    val hsv = FloatArray(3)
    AndroidColor.RGBToHSV(
        (red * 255f).toInt().coerceIn(0, 255),
        (green * 255f).toInt().coerceIn(0, 255),
        (blue * 255f).toInt().coerceIn(0, 255),
        hsv,
    )
    if (hsv[1] < 0.12f) return Color(0xFF4D72B8)
    hsv[1] = hsv[1].coerceAtLeast(0.34f)
    hsv[2] = hsv[2].coerceIn(0.46f, 0.88f)
    return Color(AndroidColor.HSVToColor(hsv))
}

private fun Color.darken(amount: Float): Color = Color(
    red = red * (1f - amount),
    green = green * (1f - amount),
    blue = blue * (1f - amount),
    alpha = 1f,
)

private fun Color.lighten(amount: Float): Color = Color(
    red = red + (1f - red) * amount,
    green = green + (1f - green) * amount,
    blue = blue + (1f - blue) * amount,
    alpha = alpha,
)

private fun blendDynamicFlowColors(first: Color, second: Color, amount: Float): Color {
    val fraction = amount.coerceIn(0f, 1f)
    return Color(
        red = first.red + (second.red - first.red) * fraction,
        green = first.green + (second.green - first.green) * fraction,
        blue = first.blue + (second.blue - first.blue) * fraction,
        alpha = first.alpha + (second.alpha - first.alpha) * fraction,
    )
}
