package com.melox.player.ui.glass.internal

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.upstream.blur.Backdrop as CurrentBackdrop
import top.yukonga.miuix.kmp.upstream.blur.BackdropEffectScope as CurrentBackdropEffectScope
import top.yukonga.miuix.kmp.upstream.blur.BlendColorEntry as CurrentBlendColorEntry
import top.yukonga.miuix.kmp.upstream.blur.BlurBlendMode as CurrentBlurBlendMode
import top.yukonga.miuix.kmp.upstream.blur.BlurColors as CurrentBlurColors
import top.yukonga.miuix.kmp.upstream.blur.ProgressiveBlur as CurrentProgressiveBlur
import top.yukonga.miuix.kmp.upstream.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.upstream.blur.textureBlur

/** Retain existing capture ownership while rendering through the current blur pipeline. */
private data class BackdropAdapter(val source: Backdrop) : CurrentBackdrop {
    override val isCoordinatesDependent: Boolean get() = source.isCoordinatesDependent
    override val offsetResidualX: Float get() = source.offsetResidualX
    override val offsetResidualY: Float get() = source.offsetResidualY

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        downscaleFactor: Float,
    ) {
        val recordingSize = drawContext.size
        // The official capture reads DrawScope.size when passed the relocated effect scope.
        drawContext.size = (density as? CurrentBackdropEffectScope)?.size ?: recordingSize
        try {
            with(source) { drawBackdrop(density, coordinates, layerBlock, downscaleFactor) }
        } finally {
            drawContext.size = recordingSize
        }
    }
}

internal fun Backdrop.asCurrentBackdrop(): CurrentBackdrop = BackdropAdapter(this)

internal fun BlurColors.asCurrentBlurColors(): CurrentBlurColors = CurrentBlurColors(
    blendColors = blendColors.map { CurrentBlendColorEntry(it.color, CurrentBlurBlendMode(it.mode.value)) },
    brightness = brightness,
    contrast = contrast,
    saturation = saturation,
)

internal fun Modifier.currentTextureBlur(
    backdrop: Backdrop,
    shape: Shape,
    blurRadius: Float,
    colors: BlurColors,
): Modifier = textureBlur(
    backdrop = backdrop.asCurrentBackdrop(),
    shape = shape,
    blurRadius = blurRadius,
    colors = colors.asCurrentBlurColors(),
)

internal fun Modifier.currentProgressiveTextureBlur(
    backdrop: Backdrop,
    shape: Shape,
    gradient: ProgressiveBlur,
    blurRadius: Float,
    colors: BlurColors,
    enabled: Boolean = true,
): Modifier = progressiveTextureBlur(
    backdrop = backdrop.asCurrentBackdrop(),
    shape = shape,
    gradient = CurrentProgressiveBlur(
        gradient.angle, gradient.startFraction, gradient.endFraction, gradient.curve,
    ),
    blurRadius = blurRadius,
    colors = colors.asCurrentBlurColors(),
    enabled = enabled,
)
