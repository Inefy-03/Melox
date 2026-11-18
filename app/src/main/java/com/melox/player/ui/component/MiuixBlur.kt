package com.melox.player.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.LayoutCoordinates
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.upstream.blur.drawBackdrop
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.glass.internal.currentTextureBlur as textureBlur
import top.yukonga.miuix.kmp.glass.internal.asCurrentBackdrop
import top.yukonga.miuix.kmp.glass.internal.asCurrentBlurColors
import top.yukonga.miuix.kmp.upstream.blur.textureBlurEffect
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.melox.player.model.DEFAULT_CUSTOM_BACKGROUND_CARD_BLUR_PERCENT
import com.melox.player.model.normalizeCustomBackgroundCardBlurPercent
import com.melox.player.model.normalizeCustomBackgroundCardOpacityPercent

internal data class TopBarBlurSettings(
    val blurEnabled: Boolean,
    val progressiveEnabled: Boolean,
)

internal val LocalTopBarBlurSettings = compositionLocalOf<TopBarBlurSettings> {
    error("No TopBarBlurSettings provided")
}

internal val LocalBottomSheetBlurBackdrop = compositionLocalOf<LayerBackdrop?> { null }
internal val LocalPageSurfaceBackdrop = compositionLocalOf<LayerBackdrop?> { null }
internal val LocalPageCardBlurRadius = compositionLocalOf { NormalBarBlurRadius }
internal val LocalPageCardSurfaceAlpha = compositionLocalOf { NormalBarSurfaceAlpha }

internal fun pageCardSurfaceAlpha(opacityPercent: Int): Float =
    normalizeCustomBackgroundCardOpacityPercent(opacityPercent) / 100f

internal fun pageCardBlurRadius(percent: Int): Float =
    NormalBarBlurRadius * normalizeCustomBackgroundCardBlurPercent(percent) / DEFAULT_CUSTOM_BACKGROUND_CARD_BLUR_PERCENT

@Composable
internal fun rememberPageSurfaceBackdrop(): LayerBackdrop? {
    val image = LocalCustomPageBackground.current
    if (!pageSurfaceBlurEnabled(
            hasBackground = image != null,
            blurEnabled = LocalTopBarBlurSettings.current.blurEnabled,
            runtimeSupported = isRuntimeShaderSupported(),
        )
    ) return null
    LocalFixedPageBackground.current?.let { return it.backdrop }
    return null
}

internal fun pageSurfaceBlurEnabled(
    hasBackground: Boolean,
    blurEnabled: Boolean,
    runtimeSupported: Boolean,
): Boolean = hasBackground && blurEnabled && runtimeSupported

@Composable
internal fun rememberGlassBlurBackdrop(
    captureCoordinates: () -> LayoutCoordinates?,
): top.yukonga.miuix.kmp.upstream.blur.LayerBackdrop? {
    if (!LocalTopBarBlurSettings.current.blurEnabled ||
        !top.yukonga.miuix.kmp.upstream.material.isRuntimeShaderSupported()
    ) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    val fixedBackground = LocalFixedPageBackground.current
    return top.yukonga.miuix.kmp.upstream.blur.rememberLayerBackdrop {
        drawRect(surfaceColor)
        if (fixedBackground != null) {
            fixedBackground.refreshSignal()
            with(fixedBackground.backdrop) {
                drawBackdrop(
                    density = this@rememberLayerBackdrop,
                    coordinates = captureCoordinates()?.takeIf { it.isAttached },
                )
            }
        }
        drawContent()
    }
}

@Composable
internal fun rememberBlurBackdrop(
    includeCustomBackground: Boolean = true,
    captureCoordinates: () -> LayoutCoordinates? = { null },
): LayerBackdrop? {
    val currentSettings = LocalTopBarBlurSettings.current
    if (!currentSettings.blurEnabled || !isRuntimeShaderSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    val fixedBackground = LocalFixedPageBackground.current.takeIf { includeCustomBackground }
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        if (fixedBackground != null) {
            fixedBackground.refreshSignal()
            with(fixedBackground.backdrop) {
                drawBackdrop(
                    density = this@rememberLayerBackdrop,
                    coordinates = captureCoordinates()?.takeIf { it.isAttached },
                )
            }
        }
        drawContent()
    }
}

@Composable
internal fun Modifier.pageTextureBlur(
    backdrop: LayerBackdrop,
    shape: Shape = RectangleShape,
    blurRadius: Float = NormalBarBlurRadius,
    surfaceAlpha: Float = NormalBarSurfaceAlpha,
): Modifier {
    val refreshSignal = LocalFixedPageBackground.current?.refreshSignal
    val colors = barBlurColors(surfaceAlpha = surfaceAlpha)
    if (refreshSignal == null) return textureBlur(backdrop, shape, blurRadius, colors = colors)
    return drawBackdrop(
        backdrop = backdrop.asCurrentBackdrop(),
        shape = { shape },
        effects = { textureBlurEffect(blurRadiusX = blurRadius, colors = colors.asCurrentBlurColors()) },
        onDrawBehind = { refreshSignal() },
    )
}

@Composable
internal fun Modifier.refreshFixedWallpaperSample(): Modifier {
    val refreshSignal = LocalFixedPageBackground.current?.refreshSignal ?: return this
    return drawWithContent {
        refreshSignal()
        drawContent()
    }
}

@Composable
internal fun LayerBackdrop?.miuixBarColor(): Color =
    topBarContainerColor(
        hasWallpaper = LocalTopBarWallpaperVisible.current,
        hasBackdrop = this != null,
        fallbackColor = MiuixTheme.colorScheme.surface,
    )

internal fun topBarContainerColor(hasWallpaper: Boolean, hasBackdrop: Boolean, fallbackColor: Color): Color =
    if (hasWallpaper || hasBackdrop) Color.Transparent else fallbackColor

@Composable
internal fun bottomSheetMaterialColor(): Color =
    if (LocalBottomSheetBlurBackdrop.current == null) {
        BottomSheetDefaults.backgroundColor()
    } else {
        Color.Transparent
    }

@Composable
internal fun bottomSheetCardColor(): Color {
    if (LocalBottomSheetBlurBackdrop.current == null) {
        return MiuixTheme.colorScheme.secondaryContainer
    }
    val isDark = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    return MiuixTheme.colorScheme.onSurface.copy(
        alpha = if (isDark) 0x26 / 255f else 0x0F / 255f,
    )
}

@Composable
internal fun bottomSheetDirectContentColor(): Color =
    if (LocalBottomSheetBlurBackdrop.current == null) {
        MiuixTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }

internal val LocalWideTopBackdrop = compositionLocalOf<LayerBackdrop?> { null }

internal val LocalGlassTopBackdrop = compositionLocalOf<LayerBackdrop?> { null }

@Composable
internal fun TopBarBackdrop(
    backdrop: LayerBackdrop?,
    content: @Composable () -> Unit,
) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalGlassTopBackdrop provides (LocalWideTopBackdrop.current ?: backdrop),
        content = content,
    )
}

@Composable
internal fun GaussianBlurredBar(
    backdrop: LayerBackdrop?,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = if (backdrop != null) {
            Modifier.textureBlur(
                backdrop = backdrop,
                shape = RectangleShape,
                blurRadius = NormalBarBlurRadius,
                colors = barBlurColors(),
            )
        } else {
            Modifier
        },
    ) {
        content()
    }
}

@Composable
internal fun barBlurColors(
    progressive: Boolean = false,
    surfaceColor: Color = MiuixTheme.colorScheme.surface,
    surfaceAlpha: Float = if (progressive) 0.3f else NormalBarSurfaceAlpha,
): BlurColors = BlurDefaults.blurColors(
    blendColors = listOf(
        BlendColorEntry(color = surfaceColor.copy(alpha = surfaceAlpha.coerceIn(0f, 1f))),
    ),
)

internal const val NormalBarBlurRadius = 25f
private const val NormalBarSurfaceAlpha = 0.8f
