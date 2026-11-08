package com.melox.player.ui.component.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.melox.player.model.BottomBarStyle
import top.yukonga.miuix.kmp.glass.GlassNavigationBarDefaults
import top.yukonga.miuix.kmp.glass.GlassDefaults
import top.yukonga.miuix.kmp.glass.GlassShape
import top.yukonga.miuix.kmp.glass.glassPanel
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.effect
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun Modifier.miniPlayerSurface(
    cornerRadius: Dp,
    backdrop: LayerBackdrop?,
    blurActive: Boolean,
    liquidGlassActive: Boolean,
    isDark: Boolean,
    followsNavigationBar: Boolean,
    floatingHighlight: State<Highlight>? = null,
    highlightAlpha: Float = 1f,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    style: BottomBarStyle = BottomBarStyle.FLOATING,
    glassShape: GlassShape = GlassShape(cornerRadius),
    showGlassStroke: Boolean = true,
): Modifier {
    val surfaceColor = if (followsNavigationBar) {
        MiuixTheme.colorScheme.surface
    } else {
        MiuixTheme.colorScheme.surfaceContainer
    }
    val usableBackdrop = backdrop.takeIf { blurActive || liquidGlassActive }
    val resolvedHighlightAlpha = highlightAlpha.coerceIn(0f, 1f)
    // Glass uses the official rounded pill for sampling and clipping. Other surfaces
    // retain their caller's outer Miuix squircle mask.
    val samplingShape = RoundedCornerShape(cornerRadius)
    val containerBlur = rememberGlassContainerBlur()
    return then(
        when {
            style == BottomBarStyle.OS4 -> Modifier.glassPanel(
                backdrop = usableBackdrop,
                shape = glassShape,
                style = GlassDefaults.Style,
                alpha = resolvedHighlightAlpha,
                material = GlassNavigationBarDefaults.material(),
                stroke = if (showGlassStroke) GlassNavigationBarDefaults.stroke() else null,
                shadow = null,
                shading = false,
                fallback = Modifier.clip(glassShape)
                    .background(MiuixTheme.colorScheme.surfaceContainer),
            )
            usableBackdrop != null && liquidGlassActive -> Modifier.drawBackdrop(
                backdrop = usableBackdrop,
                shape = { samplingShape },
                effects = {
                    padding = maxOf(padding, 40.dp.toPx())
                    vibrancy()
                    effect(containerBlur)
                    lens(
                        refractionHeight = 24.dp.toPx(),
                        refractionAmount = 24.dp.toPx(),
                    )
                },
                layerBlock = layerBlock,
                highlight = {
                    val highlight = floatingHighlight?.value?.copy(alpha = 0.75f) ?: if (isDark) {
                        Highlight.GlassStrokeMiddleDark
                    } else {
                        Highlight.GlassStrokeMiddleLight
                    }
                    highlight.copy(alpha = highlight.alpha * resolvedHighlightAlpha)
                },
                onDrawSurface = {
                    drawRect(surfaceColor.copy(alpha = 0.4f))
                },
            )

            usableBackdrop != null && blurActive -> Modifier.textureBlur(
                backdrop = usableBackdrop,
                shape = samplingShape,
                blurRadius = 25f,
                colors = BlurDefaults.blurColors(
                    blendColors = listOf(
                        BlendColorEntry(
                            surfaceColor.copy(alpha = if (followsNavigationBar) 0.8f else 0.6f),
                        ),
                    ),
                ),
                highlight = null,
            )

            else -> Modifier.squircleBackground(
                color = surfaceColor,
                cornerRadius = cornerRadius,
            )
        },
    )
}

internal fun Modifier.miuixFloatingBarShadow(
    cornerRadius: Dp,
    isDark: Boolean,
    alpha: Float = 1f,
): Modifier = miuixFloatingBarShadow(
    shape = RoundedCornerShape(cornerRadius),
    isDark = isDark,
    alpha = alpha,
)

internal fun Modifier.miuixFloatingBarShadow(
    shape: Shape,
    isDark: Boolean,
    alpha: Float = 1f,
): Modifier {
    val resolvedAlpha = alpha.coerceIn(0f, 1f)
    if (resolvedAlpha <= 0f) return this
    return dropShadow(
        shape = shape,
        shadow = Shadow(
            radius = 10.dp,
            color = Color.Black,
            alpha = (if (isDark) 0.2f else 0.1f) * resolvedAlpha,
        ),
    )
}
