package com.melox.player.ui.component.liquid

import top.yukonga.miuix.kmp.material.EdgeStroke
import top.yukonga.miuix.kmp.material.drawEdgeStroke
import top.yukonga.miuix.kmp.material.GlassEdgeStrokes
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.melox.player.model.BottomBarStyle
import com.melox.player.ui.glass.GlassShape
import top.yukonga.miuix.kmp.upstream.glass.GlassNavigationBarDefaults
import top.yukonga.miuix.kmp.upstream.glass.glassPanel
import top.yukonga.miuix.kmp.upstream.material.GlassDefaults
import top.yukonga.miuix.kmp.upstream.material.GlassShape as UpstreamGlassShape
import top.yukonga.miuix.kmp.upstream.blur.LayerBackdrop as GlassLayerBackdrop
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.effect
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
    floatingHighlight: State<EdgeStroke>? = null,
    highlightAlpha: Float = 1f,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    style: BottomBarStyle = BottomBarStyle.FLOATING,
    glassShape: GlassShape = GlassShape(cornerRadius),
    showGlassStroke: Boolean = true,
    glassBackdrop: GlassLayerBackdrop? = null,
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
    return then(
        when {
            style == BottomBarStyle.GLASS -> {
                val surfaceShape = UpstreamGlassShape(
                    topStart = glassShape.topStart,
                    topEnd = glassShape.topEnd,
                    bottomEnd = glassShape.bottomEnd,
                    bottomStart = glassShape.bottomStart,
                    smoothing = glassShape.smoothing,
                )
                Modifier.glassPanel(
                    backdrop = glassBackdrop.takeIf { blurActive },
                    shape = surfaceShape,
                    style = GlassDefaults.Style,
                    alpha = resolvedHighlightAlpha,
                    material = GlassNavigationBarDefaults.material(),
                    stroke = if (showGlassStroke) GlassNavigationBarDefaults.stroke() else null,
                    shadow = null,
                    shading = false,
                    fallback = Modifier.clip(surfaceShape)
                        .background(MiuixTheme.colorScheme.surfaceContainer),
                )
            }
            usableBackdrop != null && liquidGlassActive -> {
                val blurRadiusPx = with(LocalDensity.current) { 4.dp.toPx() }
                val containerBlur = remember(blurRadiusPx) {
                    // Match the original full-resolution blur sigma on Android.
                    val radius = (blurRadiusPx * 0.45f - 0.5f) / 0.57735f
                    BlurEffect(radius, radius)
                }
                Modifier.drawBackdrop(
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
                    // The lens uses symmetric padding for its origin. An identity transform also
                    // keeps Miuix from clamping the recorded padding per edge during expansion.
                    layerBlock = layerBlock ?: {},
                    onDrawFront = {
                        val stroke = floatingHighlight?.value ?: GlassEdgeStrokes.forTheme(isDark)
                        val alpha = if (floatingHighlight != null) 0.75f else 1f
                        drawEdgeStroke(samplingShape, stroke, alpha * resolvedHighlightAlpha)
                    },
                    onDrawSurface = {
                        drawRect(surfaceColor.copy(alpha = 0.4f))
                    },
                )
            }

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
