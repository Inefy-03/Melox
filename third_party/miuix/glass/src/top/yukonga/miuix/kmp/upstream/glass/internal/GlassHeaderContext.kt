// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.upstream.glass.internal

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import top.yukonga.miuix.kmp.upstream.blur.Backdrop
import top.yukonga.miuix.kmp.upstream.material.EdgeStroke
import top.yukonga.miuix.kmp.upstream.material.GlassMaterial
import top.yukonga.miuix.kmp.upstream.material.GlassShadow
import top.yukonga.miuix.kmp.upstream.material.GlassShape
import top.yukonga.miuix.kmp.upstream.material.GlassStyle

@Stable
internal data class GlassHeaderContext(
    val backdrop: Backdrop?,
    val buttonBackdrop: Backdrop?,
    val material: GlassMaterial,
    val underlayMaterial: GlassMaterial?,
    val buttonUnderlayMaterial: GlassMaterial?,
    val floating: Boolean,
    val alpha: Float,
    val materialProgress: State<Float>,
    val keepMaterial: Boolean,
    val style: GlassStyle,
    val size: Dp,
    val shape: GlassShape,
    val fill: Color,
    val stroke: EdgeStroke?,
    val shadow: GlassShadow?,
    val onButtonBackdropConsumerChange: ((Int) -> Unit)? = null,
)

internal val LocalGlassHeaderContext = compositionLocalOf<GlassHeaderContext?> { null }
