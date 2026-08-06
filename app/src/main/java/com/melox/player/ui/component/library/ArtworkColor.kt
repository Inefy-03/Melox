package com.melox.player.ui.component.library

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.get

internal fun Bitmap.extractArtworkColor(): Color? {
    if (width <= 0 || height <= 0) return null
    val stepX = (width / 32).coerceAtLeast(1)
    val stepY = (height / 32).coerceAtLeast(1)
    var redTotal = 0.0
    var greenTotal = 0.0
    var blueTotal = 0.0
    var weightTotal = 0.0
    var opaqueRedTotal = 0.0
    var opaqueGreenTotal = 0.0
    var opaqueBlueTotal = 0.0
    var opaquePixelCount = 0
    for (y in stepY / 2 until height step stepY) {
        for (x in stepX / 2 until width step stepX) {
            val pixel = this[x, y]
            if (android.graphics.Color.alpha(pixel) < 128) continue
            val red = android.graphics.Color.red(pixel) / 255.0
            val green = android.graphics.Color.green(pixel) / 255.0
            val blue = android.graphics.Color.blue(pixel) / 255.0
            opaqueRedTotal += red
            opaqueGreenTotal += green
            opaqueBlueTotal += blue
            opaquePixelCount += 1
            val maximum = maxOf(red, green, blue)
            val minimum = minOf(red, green, blue)
            val luminance = red * 0.2126 + green * 0.7152 + blue * 0.0722
            if (luminance !in 0.05..0.95) continue
            val saturation = if (maximum == 0.0) 0.0 else (maximum - minimum) / maximum
            val weight = 0.25 + saturation * 1.75
            redTotal += red * weight
            greenTotal += green * weight
            blueTotal += blue * weight
            weightTotal += weight
        }
    }
    if (weightTotal == 0.0) {
        if (opaquePixelCount == 0) return null
        return Color(
            red = (opaqueRedTotal / opaquePixelCount).toFloat().coerceIn(0f, 1f),
            green = (opaqueGreenTotal / opaquePixelCount).toFloat().coerceIn(0f, 1f),
            blue = (opaqueBlueTotal / opaquePixelCount).toFloat().coerceIn(0f, 1f),
        )
    }
    return Color(
        red = (redTotal / weightTotal).toFloat().coerceIn(0f, 1f),
        green = (greenTotal / weightTotal).toFloat().coerceIn(0f, 1f),
        blue = (blueTotal / weightTotal).toFloat().coerceIn(0f, 1f),
    )
}
