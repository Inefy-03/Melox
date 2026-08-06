package com.melox.player.ui.component.liquid

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun Modifier.miniPlayerSurface(
    shape: Shape,
    backdrop: LayerBackdrop?,
    blurActive: Boolean,
    liquidGlassActive: Boolean,
    isDark: Boolean,
    followsNavigationBar: Boolean,
    floatingHighlight: Highlight? = null,
    highlightAlpha: Float = 1f,
): Modifier {
    val surfaceColor = if (followsNavigationBar) {
        MiuixTheme.colorScheme.surface
    } else {
        MiuixTheme.colorScheme.surfaceContainer
    }
    val usableBackdrop = backdrop.takeIf { blurActive || liquidGlassActive }
    val resolvedHighlightAlpha = highlightAlpha.coerceIn(0f, 1f)
    return then(
        when {
            usableBackdrop != null && liquidGlassActive -> Modifier.drawBackdrop(
                backdrop = usableBackdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(4.dp.toPx(), 4.dp.toPx())
                    lens(
                        refractionHeight = 24.dp.toPx(),
                        refractionAmount = 24.dp.toPx(),
                    )
                },
                highlight = {
                    val highlight = floatingHighlight?.copy(alpha = 0.75f) ?: if (isDark) {
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
                shape = shape,
                blurRadius = 25f,
                colors = BlurDefaults.blurColors(
                    blendColors = listOf(
                        BlendColorEntry(
                            surfaceColor.copy(alpha = if (followsNavigationBar) 0.8f else 0.6f),
                        ),
                    ),
                ),
                highlight = (if (followsNavigationBar) {
                    null
                } else if (floatingHighlight != null) {
                    floatingHighlight
                } else if (isDark) {
                    Highlight.GlassStrokeMiddleDark
                } else {
                    Highlight.GlassStrokeMiddleLight
                })?.let { highlight ->
                    highlight.copy(alpha = highlight.alpha * resolvedHighlightAlpha)
                },
            )

            else -> Modifier.background(surfaceColor, shape)
        },
    )
}

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
