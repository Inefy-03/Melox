package com.melox.player.ui.component.playback

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith

internal fun AnimatedContentTransitionScope<*>.playerControlIconTransition() =
    (
        fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.72f)
    ).togetherWith(
        fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.72f),
    ).using(SizeTransform(clip = false))
