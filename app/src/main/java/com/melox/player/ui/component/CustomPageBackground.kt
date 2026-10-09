package com.melox.player.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onLayoutRectChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.melox.player.data.repository.CustomBackgroundRepository
import com.melox.player.data.repository.LoadedCustomBackground
import com.melox.player.model.AppSettings
import com.melox.player.model.normalizeCustomBackgroundBlurPercent
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val LocalCustomPageBackground = compositionLocalOf<ImageBitmap?> { null }
internal val LocalCustomBackgroundDimAlpha = compositionLocalOf { 0f }
internal val LocalTopBarWallpaperVisible = compositionLocalOf { false }
internal val LocalFixedPageBackground = compositionLocalOf<FixedPageBackground?> { null }

internal class FixedPageBackground(
    val backdrop: LayerBackdrop,
    refreshSignal: () -> Float,
) {
    var motionPosition by mutableStateOf(IntOffset.Zero)
    val refreshSignal: () -> Float = {
        refreshSignal() + motionPosition.x + motionPosition.y
    }
}

@Composable
internal fun Modifier.trackFixedWallpaperMotion(): Modifier {
    val background = LocalFixedPageBackground.current ?: return this
    return onLayoutRectChanged(throttleMillis = 0, debounceMillis = 0) {
        background.motionPosition = it.positionInRoot
    }
}

@Composable
internal fun FixedPageBackgroundHost(
    modifier: Modifier = Modifier,
    refreshSignal: () -> Float,
    content: @Composable () -> Unit,
) {
    val image = LocalCustomPageBackground.current
    val surfaceColor = MiuixTheme.colorScheme.surface
    val dimAlpha = LocalCustomBackgroundDimAlpha.current
    val currentRefreshSignal by rememberUpdatedState(refreshSignal)
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        image?.let { drawCustomPageBackground(it, dimAlpha) }
    }
    val fixedBackground = remember(backdrop) { FixedPageBackground(backdrop) { currentRefreshSignal() } }
    // Keep the pager mounted when an image is selected, removed or still decoding.
    Box(modifier.customPageBackground()) {
        if (image != null) {
            Box(Modifier.matchParentSize().layerBackdrop(backdrop))
        }
        CompositionLocalProvider(LocalFixedPageBackground provides fixedBackground.takeIf { image != null }) {
            content()
        }
    }
}

@Composable
private fun Modifier.fixedWallpaperRegion(background: FixedPageBackground): Modifier {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    return onGloballyPositioned { coordinates = it }.drawWithCache {
        onDrawBehind {
            // Read inside this drawing layer so pager motion invalidates the sample.
            background.refreshSignal()
            val currentCoordinates = coordinates?.takeIf { it.isAttached }
            if (currentCoordinates != null) {
                clipRect {
                    with(background.backdrop) {
                        drawBackdrop(density = this@onDrawBehind, coordinates = currentCoordinates)
                    }
                }
            }
        }
    }
}

@Composable
internal fun rememberCustomPageBackground(
    settings: AppSettings,
    prepared: LoadedCustomBackground?,
    blurPercentOverride: Int? = null,
): ImageBitmap? {
    val id = settings.customBackgroundId ?: return null
    val percent = normalizeCustomBackgroundBlurPercent(blurPercentOverride ?: settings.customBackgroundBlurPercent)
    val preparedImage = remember(prepared?.rendered) { prepared?.rendered?.asImageBitmap() }
    // A prepared startup image is usable immediately, without another composition cycle.
    if (prepared?.id == id && prepared.blurPercent == percent) return preparedImage
    val context = LocalContext.current.applicationContext
    val repository = remember(context) { CustomBackgroundRepository(context) }
    return produceState<ImageBitmap?>(preparedImage, prepared, percent, id) {
        if (prepared == null) {
            value = null
        } else if (prepared.id == id) {
            value = if (prepared.blurPercent == percent) {
                preparedImage
            } else {
                repository.blurImage(prepared.source, percent)?.asImageBitmap()
            }
        }
    }.value
}

@Composable
internal fun Modifier.customPageBackground(): Modifier {
    val image = LocalCustomPageBackground.current ?: return this
    val surfaceColor = MiuixTheme.colorScheme.surface
    val dimAlpha = LocalCustomBackgroundDimAlpha.current
    return drawWithCache {
        onDrawBehind {
            drawRect(surfaceColor)
            drawCustomPageBackground(image, dimAlpha)
        }
    }
}

@Composable
internal fun customPageContainerColor(): Color =
    if (LocalCustomPageBackground.current == null) MiuixTheme.colorScheme.surface else Color.Transparent

@Composable
internal fun PageScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val fixedBackground = LocalFixedPageBackground.current
    val surfaceBackdrop = rememberPageSurfaceBackdrop()
    val image = LocalCustomPageBackground.current
    val dimAlpha = LocalCustomBackgroundDimAlpha.current
    val surfaceColor = MiuixTheme.colorScheme.surface
    BoxWithConstraints(modifier = modifier) {
        val viewportSize = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        CompositionLocalProvider(LocalPageSurfaceBackdrop provides surfaceBackdrop) {
            Scaffold(
                modifier = if (fixedBackground == null) Modifier.customPageBackground() else Modifier,
                containerColor = customPageContainerColor(),
                topBar = {
                    CompositionLocalProvider(LocalTopBarWallpaperVisible provides (image != null)) {
                        Box(
                            if (fixedBackground != null) {
                                Modifier.fixedWallpaperRegion(fixedBackground)
                            } else Modifier.drawWithCache {
                                onDrawBehind {
                                    // Reuse the page crop at Miuix's (0, 0) bar origin.
                                    // Keep the wallpaper base mounted through effect handoff.
                                    if (image != null) {
                                        drawRect(surfaceColor)
                                        drawCustomPageBackground(image, dimAlpha, viewportSize)
                                    }
                                }
                            },
                        ) { topBar() }
                    }
                },
            ) { padding ->
                if (surfaceBackdrop == null || fixedBackground != null) {
                    content(padding)
                } else {
                    Box(Modifier.fillMaxSize()) {
                        // Capture only the wallpaper, never the Cards that consume this layer.
                        Box(Modifier.matchParentSize().layerBackdrop(surfaceBackdrop))
                        content(padding)
                    }
                }
            }
        }
    }
}

internal fun wallpaperTopBarVisible(
    hasBackground: Boolean,
    blurEnabled: Boolean,
    progressiveEnabled: Boolean,
    runtimeSupported: Boolean,
): Boolean = hasBackground && !(blurEnabled && progressiveEnabled && runtimeSupported)

internal fun DrawScope.drawCustomPageBackground(
    image: ImageBitmap,
    dimAlpha: Float = 0f,
    viewportSize: Size = size,
) {
    val destination = customBackgroundCropSize(image.width, image.height, viewportSize.width, viewportSize.height)
    if (destination == IntSize.Zero) return
    clipRect {
        drawImage(
            image = image,
            dstOffset = customBackgroundCropOffset(destination, viewportSize),
            dstSize = destination,
            filterQuality = FilterQuality.Medium,
        )
        if (dimAlpha > 0f) drawRect(Color.Black.copy(alpha = dimAlpha.coerceIn(0f, 1f)))
    }
}

internal fun customBackgroundCropOffset(destination: IntSize, viewportSize: Size): IntOffset = IntOffset(
    ((viewportSize.width - destination.width) / 2f).roundToInt(),
    ((viewportSize.height - destination.height) / 2f).roundToInt(),
)

internal fun customBackgroundCropSize(
    imageWidth: Int,
    imageHeight: Int,
    pageWidth: Float,
    pageHeight: Float,
): IntSize {
    if (imageWidth <= 0 || imageHeight <= 0 || pageWidth <= 0f || pageHeight <= 0f) return IntSize.Zero
    val scale = maxOf(pageWidth / imageWidth, pageHeight / imageHeight)
    return IntSize((imageWidth * scale).roundToInt(), (imageHeight * scale).roundToInt())
}
