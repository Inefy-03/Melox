package com.melox.player.ui.component.liquid

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
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
): Modifier {
    val surfaceColor = if (followsNavigationBar) {
        MiuixTheme.colorScheme.surface
    } else {
        MiuixTheme.colorScheme.surfaceContainer
    }
    val usableBackdrop = backdrop.takeIf { blurActive || liquidGlassActive }
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
                    floatingHighlight?.copy(alpha = 0.75f) ?: if (isDark) {
                        Highlight.GlassStrokeMiddleDark
                    } else {
                        Highlight.GlassStrokeMiddleLight
                    }
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
                highlight = if (followsNavigationBar) {
                    null
                } else if (floatingHighlight != null) {
                    floatingHighlight
                } else if (isDark) {
                    Highlight.GlassStrokeMiddleDark
                } else {
                    Highlight.GlassStrokeMiddleLight
                },
            )

            else -> Modifier.background(surfaceColor, shape)
        },
    )
}
