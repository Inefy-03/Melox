// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.upstream.glass

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LayerOutsets
import androidx.compose.ui.unit.Density
import top.yukonga.miuix.kmp.upstream.material.*

@Immutable
data class GlassPopupVisuals(
    val style: GlassStyle,
    val alpha: Float,
    val stroke: EdgeStroke?,
    val shadow: GlassShadow?,
    val material: GlassMaterial?,
    val containerColor: Color,
)

internal fun Density.popupShadowOutsets(shadow: GlassShadow?): LayerOutsets {
    if (shadow == null) return LayerOutsets.Zero
    val sourceScale = density / GlassDefaults.SourceDensity
    val reach = (shadow.radius * sourceScale).coerceAtLeast(1f)
    val x = shadow.offsetX * sourceScale
    val y = shadow.offsetY * sourceScale
    return LayerOutsets(
        left = (reach - x).coerceAtLeast(0f).toDp(),
        top = (reach - y).coerceAtLeast(0f).toDp(),
        right = (reach + x).coerceAtLeast(0f).toDp(),
        bottom = (reach + y).coerceAtLeast(0f).toDp(),
    )
}

