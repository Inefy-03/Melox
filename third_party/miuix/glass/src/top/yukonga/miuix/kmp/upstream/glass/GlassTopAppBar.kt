// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.upstream.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.upstream.blur.Backdrop
import top.yukonga.miuix.kmp.upstream.material.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

object GlassTopAppBarDefaults {

    /** Start/end inset of the glass controls, aligned with the page's cards and tabs. */
    val HorizontalPadding: Dp = 12.dp

/** Scroll threshold for the retained shadow ramp. */
    val RampStart: Dp = 0.dp

/** Scroll distance for the retained shadow ramp. Material transitions use their own timing. */
    val RampDistance: Dp = 32.dp

    /** Maximum blur radius applied to titles during collapse/expand transitions. */
    val TitleBlurRadius: Dp = 8.dp

    /** Diameter of the glass pill behind a bar button. */
    val ButtonSize: Dp = 44.dp

    /** Maximum radius of the progressive background band. */
    val ProgressiveBlurRadius: Dp = 80.dp

    /** Scroll distance over which the progressive band fades in. */
    val ProgressiveBlurRampDistance: Dp = 48.dp

    /** Icon opacity while an action-bar button is held. */
    val PressedContentAlpha: Float = 0.6f

/** Fallback button fill when backdrop material is unavailable. */
    @Composable
    fun buttonFill(): Color = if (isDarkTheme()) Color(0xFF2C2C2C) else Color.White

/** Default material for action-bar buttons. */
    @Composable
    fun buttonMaterial(): GlassMaterial = GlassMaterials.puredThinGlass(isDarkTheme())

/** Material applied beneath child buttons and tabs. */
    @Composable
    internal fun actionBarUnderlayMaterial(): GlassMaterial = GlassMaterials.actionBarMask(isDarkTheme())

    /** The small bloom stroke paired with [buttonMaterial]. */
    @Composable
    fun buttonStroke(): EdgeStroke = GlassEdgeStrokes.forTheme(
        isDark = isDarkTheme(),
        light = GlassEdgeStrokes.SmallLight,
        dark = GlassEdgeStrokes.SmallDark,
    )

    /** The state overlay painted behind a held action-bar icon. */
    @Composable
    fun buttonPressedOverlay(): Color = if (isDarkTheme()) {
        Color.White.copy(alpha = 0.14f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }

    @Composable
    private fun isDarkTheme(): Boolean = MiuixTheme.colorScheme.background.luminance() < 0.5f
}

internal data class GlassTopAppBarContext(
    val backdrop: Backdrop?,
    val buttonBackdrop: Backdrop?,
    val material: GlassMaterial,
    val underlayMaterial: GlassMaterial?,
    val buttonUnderlayMaterial: GlassMaterial?,
    val floating: Boolean,
    val alpha: Float,
    val ramp: State<Float>,
    val materialProgress: State<Float>,
    val keepMaterial: Boolean,
    val style: GlassStyle,
    val size: Dp,
    val shape: GlassShape,
    val fill: Color,
    val stroke: EdgeStroke?,
    val shadow: GlassShadow?,
)

internal val LocalGlassTopAppBarContext = staticCompositionLocalOf<GlassTopAppBarContext?> { null }

