// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.glass.internal

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BackdropEffectScope

/** Applies a scrim to the padded backdrop recording before blur. */
@Stable
internal class ScrimBackdrop(
    private val backdrop: Backdrop,
    private val scrimColor: State<Color>,
    private val scale: State<Float>? = null,
) : Backdrop by backdrop {

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        downscaleFactor: Float,
    ) {
        with(backdrop) {
            drawBackdrop(
                density = density,
                coordinates = coordinates,
                layerBlock = {
                    layerBlock?.invoke(this)
                    // The host already applies this scale. Only invert it during sampling.
                    scale?.let {
                        scaleX *= it.value
                        scaleY *= it.value
                    }
                },
                downscaleFactor = downscaleFactor,
            )
        }
        val color = scrimColor.value
        if (color.alpha > 0f) {
            val officialScope = density as? BackdropEffectScope
            val upstreamScope = density as? top.yukonga.miuix.kmp.upstream.blur.BackdropEffectScope
            val consumerSize = officialScope?.size ?: upstreamScope?.size
            val padding = officialScope?.padding ?: upstreamScope?.padding ?: 0f
            val scaledPadding = padding / downscaleFactor
            val coverage = consumerSize?.let {
                Size((it.width + padding * 2f) / downscaleFactor, (it.height + padding * 2f) / downscaleFactor)
            } ?: size
            // Cover recording padding while preserving transparent backdrop pixels.
            drawRect(color, topLeft = Offset(-scaledPadding, -scaledPadding), size = coverage, blendMode = BlendMode.SrcAtop)
        }
    }
}
