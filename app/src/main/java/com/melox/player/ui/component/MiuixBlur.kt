package com.melox.player.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.blur.textureBlurEffect
import top.yukonga.miuix.kmp.blur.progressiveTextureBlurEffect
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
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

internal fun tabSelectedContainerColor(hasWallpaper: Boolean, progressiveBlurActive: Boolean, fallbackColor: Color): Color =
    if (hasWallpaper || progressiveBlurActive) fallbackColor.copy(alpha = fallbackColor.alpha * 0.8f) else fallbackColor

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
    val surfaceColor = MiuixTheme.colorScheme.surface
    val dimAlpha = LocalCustomBackgroundDimAlpha.current
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        image?.let { drawCustomPageBackground(it, dimAlpha) }
    }
}

internal fun pageSurfaceBlurEnabled(
    hasBackground: Boolean,
    blurEnabled: Boolean,
    runtimeSupported: Boolean,
): Boolean = hasBackground && blurEnabled && runtimeSupported

@Composable
internal fun rememberBlurBackdrop(
    includeCustomBackground: Boolean = true,
    captureCoordinates: () -> LayoutCoordinates? = { null },
): LayerBackdrop? {
    val currentSettings = LocalTopBarBlurSettings.current
    if (!currentSettings.blurEnabled || !isRuntimeShaderSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    val customBackground = LocalCustomPageBackground.current.takeIf { includeCustomBackground }
    val dimAlpha = LocalCustomBackgroundDimAlpha.current
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
        } else {
            customBackground?.let { drawCustomPageBackground(it, dimAlpha) }
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
        backdrop = backdrop,
        shape = { shape },
        effects = { textureBlurEffect(blurRadiusX = blurRadius, colors = colors) },
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

@Composable
internal fun bottomSheetGlassModifier(): Modifier {
    val backdrop = LocalBottomSheetBlurBackdrop.current ?: return Modifier
    val elasticExtensionPx =
        (LocalWindowInfo.current.containerSize.height / BottomSheetDragDampingDivisor)
            .coerceAtLeast(1)
    val isDark = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    val highlight = if (isDark) {
        Highlight.GlassStrokeBigDark
    } else {
        val stroke = BloomStroke.GlassStrokeBigLight
        Highlight(
            width = 1.dp,
            style = stroke.copy(
                primaryLight = stroke.primaryLight.copy(intensity = 0f),
                secondaryLight = stroke.secondaryLight.copy(intensity = 0f),
            ),
        )
    }
    return Modifier
        .layout { measurable, constraints ->
            val placeable = measurable.measure(
                constraints.offset(vertical = elasticExtensionPx),
            )
            layout(placeable.width, placeable.height - elasticExtensionPx) {
                placeable.place(0, 0)
            }
        }
        .textureBlur(
            backdrop = backdrop,
            shape = RoundedCornerShape(
                topStart = BottomSheetDefaults.cornerRadius,
                topEnd = BottomSheetDefaults.cornerRadius,
            ),
            blurRadius = 100f,
            colors = BlurDefaults.blurColors(
                blendColors = listOf(
                    BlendColorEntry(
                        color = if (isDark) Color(0x4D000000) else Color(0x99FFFFFF),
                        mode = if (isDark) BlurBlendMode.ColorBurn else BlurBlendMode.Overlay,
                    ),
                    BlendColorEntry(
                        color = MiuixTheme.colorScheme.surface.copy(
                            alpha = if (isDark) 0x80 / 255f else 0xBF / 255f,
                        ),
                    ),
                ),
            ),
            highlight = highlight,
        )
        .layout { measurable, constraints ->
            val placeable = measurable.measure(
                constraints.offset(vertical = -elasticExtensionPx),
            )
            layout(placeable.width, placeable.height + elasticExtensionPx) {
                placeable.place(0, 0)
            }
        }
}

private const val BottomSheetDragDampingDivisor = 10

@Composable
internal fun BlurredBar(
    backdrop: LayerBackdrop?,
    blurEnabled: Boolean,
    scrollBehavior: ScrollBehavior? = null,
    surfaceColor: Color = MiuixTheme.colorScheme.surface,
    content: @Composable () -> Unit,
) {
    val progressive = LocalTopBarBlurSettings.current.progressiveEnabled
    val refreshSignal = LocalFixedPageBackground.current?.refreshSignal
    val progressiveColors = barBlurColors(progressive = true, surfaceColor = surfaceColor)
    val progressiveGradient = ProgressiveBlur.Top.copy(startFraction = 0.2f, endFraction = 1f, curve = 3f)
    val wallpaperOnly = wallpaperTopBarVisible(
        hasBackground = LocalTopBarWallpaperVisible.current,
        blurEnabled = blurEnabled,
        progressiveEnabled = progressive,
        runtimeSupported = backdrop != null,
    )
    val blurActive = blurEnabled && backdrop != null && !wallpaperOnly
    Box(
        modifier = if (blurActive && !progressive) {
            Modifier.textureBlur(
                backdrop = backdrop,
                shape = RectangleShape,
                blurRadius = NormalBarBlurRadius,
                colors = barBlurColors(surfaceColor = surfaceColor),
            )
        } else {
            Modifier
        },
    ) {
        if (blurActive && progressive) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(
                        if (refreshSignal != null) {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { RectangleShape },
                                effects = {
                                    progressiveTextureBlurEffect(
                                        blurRadiusX = 12f,
                                        gradient = progressiveGradient,
                                        noiseCoefficient = BlurDefaults.ProgressiveNoiseCoefficient,
                                        colors = progressiveColors,
                                    )
                                },
                                progressiveGradient = progressiveGradient,
                                onDrawBehind = { refreshSignal() },
                            )
                        } else {
                            Modifier.progressiveTextureBlur(
                                backdrop = backdrop,
                                shape = RectangleShape,
                                gradient = progressiveGradient,
                                blurRadius = 12f,
                                colors = progressiveColors,
                            )
                        },
                    ),
            )
        }
        content()
    }
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
