// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.isSpecified
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.upstream.blur.BackdropPipeline
import top.yukonga.miuix.kmp.upstream.blur.BackdropEffectScope
import top.yukonga.miuix.kmp.upstream.blur.BlurDefaults
import top.yukonga.miuix.kmp.upstream.blur.blur
import top.yukonga.miuix.kmp.upstream.blur.drawBackdrop
import top.yukonga.miuix.kmp.upstream.blur.noiseDither
import top.yukonga.miuix.kmp.glass.internal.asCurrentBackdrop
import top.yukonga.miuix.kmp.glass.internal.GLASS_COLOR_BLEND_SHADER_KEY
import top.yukonga.miuix.kmp.glass.internal.colorBlendEffect
import top.yukonga.miuix.kmp.glass.internal.drawGlassMask
import top.yukonga.miuix.kmp.glass.internal.drawGlassRim
import top.yukonga.miuix.kmp.glass.internal.drawGlassShadow
import top.yukonga.miuix.kmp.glass.internal.drawGlassStroke
import top.yukonga.miuix.kmp.glass.internal.glassEffect
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Applies a glass surface over [backdrop].
 *
 * @param backdrop Content behind the glass. This modifier must be outside the subtree recorded by
 *   the backdrop; placing it inside creates a sampling cycle.
 * @param shape Surface silhouette used for clipping, [stroke], and rendering geometry.
 * @param style Glass appearance. Use a predefined style from [GlassStyles] or [GlassDefaults.Style].
 * @param alpha Opacity multiplier for colour layers, shading, and stroke.
 * @param tint Overrides [GlassInner.tint]. Its alpha replaces the style tint strength.
 *   [Color.Unspecified] retains the style tint.
 * @param material Surface body: blur radius and colour layers. Its blur radius overrides [style].
 *   `null` uses the blurred backdrop as the body.
 * @param noiseCoefficient Noise dithering coefficient for backdrop blur. 0 disables dithering.
 * @param stroke Optional bloom stroke along the silhouette. `null` disables it.
 * @param contentBlendMode Blend mode for content drawn over the glass.
 * @param enabled Whether the glass effect is active. When false, content draws without it.
 * @param shading Whether to refract and shade the backdrop. Use `false` for bars and menus whose
 *   body is supplied by [material].
 */
fun Modifier.glass(
    backdrop: Backdrop,
    shape: GlassShape,
    style: GlassStyle = GlassDefaults.Style,
    alpha: Float = 1f,
    tint: Color = Color.Unspecified,
    material: GlassMaterial? = null,
    noiseCoefficient: Float = GlassDefaults.NoiseCoefficient,
    stroke: GlassStroke? = null,
    contentBlendMode: BlendMode = BlendMode.SrcOver,
    enabled: Boolean = true,
    shading: Boolean = true,
): Modifier = glassImpl(
    backdrop = backdrop,
    shape = shape,
    style = style,
    alpha = alpha,
    tint = tint,
    material = material,
    underlayMaterial = null,
    noiseCoefficient = noiseCoefficient,
    stroke = stroke,
    contentBlendMode = contentBlendMode,
    enabled = enabled,
    shading = shading,
)

/** Applies a child material over an action bar's blurred and colour-treated surface. */
internal fun Modifier.glassOnActionBar(
    backdrop: Backdrop,
    shape: GlassShape,
    style: GlassStyle,
    alpha: Float,
    material: GlassMaterial?,
    underlayMaterial: GlassMaterial,
    stroke: GlassStroke?,
    enabled: Boolean = true,
): Modifier = glassImpl(
    backdrop = backdrop,
    shape = shape,
    style = style,
    alpha = alpha,
    tint = Color.Unspecified,
    material = material,
    underlayMaterial = underlayMaterial,
    noiseCoefficient = GlassDefaults.NoiseCoefficient,
    stroke = stroke,
    contentBlendMode = BlendMode.SrcOver,
    enabled = enabled,
    shading = false,
)

private fun Modifier.glassImpl(
    backdrop: Backdrop,
    shape: GlassShape,
    style: GlassStyle,
    alpha: Float,
    tint: Color,
    material: GlassMaterial?,
    underlayMaterial: GlassMaterial?,
    noiseCoefficient: Float,
    stroke: GlassStroke?,
    contentBlendMode: BlendMode,
    enabled: Boolean,
    shading: Boolean,
): Modifier = this.drawBackdrop(
    backdrop = backdrop.asCurrentBackdrop(),
    pipeline = GlassPipeline(shape, style, alpha, tint, material, underlayMaterial, noiseCoefficient, stroke, shading),
    contentBlendMode = contentBlendMode,
    enabled = enabled,
)

/** Equal material inputs retain the current backdrop node across recompositions. */
private data class GlassPipeline(
    private val glassShape: GlassShape,
    private val style: GlassStyle,
    private val alpha: Float,
    private val tint: Color,
    private val material: GlassMaterial?,
    private val underlayMaterial: GlassMaterial?,
    private val noiseCoefficient: Float,
    private val stroke: GlassStroke?,
    private val shading: Boolean,
) : BackdropPipeline {
    override val shape: Shape get() = glassShape

    override fun configureEffects(scope: BackdropEffectScope) {
        with(scope) {
            noiseDither(noiseCoefficient)
            val radius = material?.blurRadius?.value ?: (style.blur.small / GlassDefaults.SourceDensity)
            blur(radius.coerceIn(0f, BlurDefaults.MaxBlurRadius) * density)
            if (underlayMaterial != null) {
                colorBlendEffect(underlayMaterial, 1f, "${GLASS_COLOR_BLEND_SHADER_KEY}ActionBar")
            }
            if (material != null) colorBlendEffect(material, alpha)
            if (shading) glassEffect(style, glassShape, alpha, tint)
        }
    }

    override fun drawFront(scope: DrawScope) {
        with(scope) {
            if (stroke != null) drawGlassStroke(glassShape, layoutDirection, stroke, alpha)
            drawGlassMask(glassShape, layoutDirection)
            if (shading) {
                drawGlassRim(glassShape, layoutDirection, style, if (tint.isSpecified) tint else style.inner.tint, alpha)
            }
        }
    }
}

/**
 * Draws [shadow] beneath this composable using [shape].
 *
 * @param shape Shadow silhouette. Use the same shape as the corresponding [glass] surface.
 * @param shadow Shadow configuration. Use a value from [GlassShadows], or `null` to disable it.
 * @param alpha Opacity multiplier for the shadow.
 */
fun Modifier.glassShadow(
    shape: GlassShape,
    shadow: GlassShadow? = GlassShadows.Regular,
    alpha: Float = 1f,
): Modifier = glassShadow(shape, shadow) { alpha }

internal fun Modifier.glassShadow(
    shape: GlassShape,
    shadow: GlassShadow?,
    alpha: () -> Float,
): Modifier = if (shadow == null) {
    this
} else {
    this.drawBehind { drawGlassShadow(shape, layoutDirection, shadow, alpha()) }
}

/** Tints the glass material with a container surface when appropriate. */
@Composable
internal fun dynamicGlassMaterial(
    base: GlassMaterial,
    tint: Color = MiuixTheme.colorScheme.surfaceContainerHigh,
    alpha: Float = 0.6f,
): GlassMaterial = if (MiuixTheme.isDynamicColor) {
    remember(base, tint, alpha) {
        base.copy(third = GlassColorLayer(tint.copy(alpha = alpha), GlassColorBlendMode.SrcOver))
    }
} else {
    base
}
