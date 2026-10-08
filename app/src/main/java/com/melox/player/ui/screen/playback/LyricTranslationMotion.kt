// SPDX-License-Identifier: Apache-2.0
package com.melox.player.ui.screen.playback

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Shared by every row, so lazy-item creation never replays a translation entrance. */
internal class LyricTranslationMotion(initiallyVisible: Boolean) {
    val expansion = Animatable(if (initiallyVisible) 1f else 0f)
    val opacity = Animatable(if (initiallyVisible) 1f else 0f)
    private var expansionVelocity = 0f

    suspend fun transitionTo(visible: Boolean, onGeometrySettled: () -> Unit = {}) = coroutineScope {
        val target = if (visible) 1f else 0f
        launch { fadeTo(target) }
        launch {
            if (expansion.value != target) {
                expansion.animateTo(
                    targetValue = target,
                    animationSpec = spring(dampingRatio = 1f, stiffness = 300f),
                    initialVelocity = expansionVelocity,
                ) { expansionVelocity = velocity }
                expansionVelocity = 0f
                onGeometrySettled()
            }
        }
    }

    private suspend fun fadeTo(target: Float) {
        if (opacity.value == target) return
        opacity.animateTo(
            targetValue = target,
            animationSpec = tween(180, easing = LinearOutSlowInEasing),
        )
    }
}

internal fun lyricTranslationPaddingDp(
    hasTranslation: Boolean,
    expansion: Float,
    lyricFontScale: Float = 1f,
): Float {
    val hidden = lyricLineVerticalPaddingDp(false, hasTranslation, false, lyricFontScale)
    val shown = lyricLineVerticalPaddingDp(false, hasTranslation, true, lyricFontScale)
    return hidden + (shown - hidden) * expansion.coerceIn(0f, 1f)
}
