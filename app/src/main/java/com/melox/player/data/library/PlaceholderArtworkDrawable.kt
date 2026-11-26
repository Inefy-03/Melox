package com.melox.player.data.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import androidx.annotation.ColorInt
import androidx.core.graphics.createBitmap
import com.melox.player.R

internal data class PlaceholderArtworkPalette(
    @param:ColorInt val background: Int,
    @param:ColorInt val icon: Int,
) {
    val cacheKey: String
        get() = "album-placeholder-v6:$background:$icon"
}

internal fun Context.loadPlaceholderArtworkDrawable(
    palette: PlaceholderArtworkPalette? = null,
): LayerDrawable? {
    val drawable = getDrawable(R.drawable.ic_album_placeholder)?.mutate() as? LayerDrawable
        ?: return null
    if (palette != null) {
        (drawable.findDrawableByLayerId(R.id.artwork_placeholder_background) as ColorDrawable)
            .color = palette.background
        drawable.findDrawableByLayerId(R.id.artwork_placeholder_icon).setTint(palette.icon)
    }
    return drawable
}

internal fun renderPlaceholderArtwork(
    context: Context,
    sizePx: Int,
    palette: PlaceholderArtworkPalette? = null,
): Bitmap? {
    val drawable = context.loadPlaceholderArtworkDrawable(palette) ?: return null
    return createBitmap(sizePx, sizePx).also { bitmap ->
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(Canvas(bitmap))
    }
}
