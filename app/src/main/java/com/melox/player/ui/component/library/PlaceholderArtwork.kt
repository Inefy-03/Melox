package com.melox.player.ui.component.library

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.melox.player.data.library.PlaceholderArtworkPalette
import com.melox.player.data.library.loadPlaceholderArtworkDrawable
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun currentPlaceholderArtworkPalette(): PlaceholderArtworkPalette {
    val colors = MiuixTheme.colorScheme
    return PlaceholderArtworkPalette(
        background = colors.surfaceContainer.toArgb(),
        icon = colors.onSurfaceVariantSummary.toArgb(),
    )
}

@Composable
internal fun PlaceholderArtwork(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val palette = currentPlaceholderArtworkPalette()
    Box(
        modifier.drawWithCache {
            val artwork = context.loadPlaceholderArtworkDrawable(palette)
            artwork?.setBounds(0, 0, size.width.roundToInt(), size.height.roundToInt())
            onDrawBehind {
                drawIntoCanvas { canvas -> artwork?.draw(canvas.nativeCanvas) }
            }
        },
    )
}
