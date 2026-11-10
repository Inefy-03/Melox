package com.melox.player.ui.component.playback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.melox.player.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.melox.player.ui.component.os4.Icon

@Composable
internal fun PlayerTrackSkipButton(
    direction: Int,
    tint: Color,
    iconSize: Dp,
    touchSize: Dp,
    onClick: () -> Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val motion = remember { TrackSkipIconMotion() }
    val scope = rememberCoroutineScope()
    var animationJob by remember { mutableStateOf<Job?>(null) }
    val press = rememberPlayerControlPressFeedback(enabled)
    val next = direction > 0
    PlayerControlIconButton(
        onClick = {
            if (onClick()) {
                animationJob?.cancel()
                animationJob = scope.launch { motion.play() }
            }
        },
        modifier = modifier.then(press.modifier),
        enabled = enabled,
        minWidth = touchSize,
        minHeight = touchSize,
    ) {
        Box(
            modifier = Modifier.size(iconSize).graphicsLayer {
                val scale = motion.scale.value * press.scale.value
                scaleX = scale
                scaleY = scale
            },
        ) {
            Icon(
                painter = painterResource(
                    if (next) R.drawable.ic_player_next_triangle else R.drawable.ic_player_previous_triangle,
                ),
                contentDescription = stringResource(if (next) R.string.next_track else R.string.previous_track),
                tint = tint,
                modifier = Modifier.size(iconSize).graphicsLayer {
                    translationX = direction * TRACK_SKIP_TRIANGLE_TRAVEL * size.width / 24f *
                        motion.triangle.value
                },
            )
            Icon(
                painter = painterResource(
                    if (next) R.drawable.ic_player_next_bar else R.drawable.ic_player_previous_bar,
                ),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(iconSize).graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, (0.75f + 23.3972f) / 48f)
                    scaleY = motion.barHeight.value
                    translationX = direction * TRACK_SKIP_BAR_TRAVEL * size.width / 24f *
                        motion.bar.value
                },
            )
        }
    }
}
