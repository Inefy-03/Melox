package com.melox.player.ui.component.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.flaviofaria.kenburnsview.KenBurnsView
import com.flaviofaria.kenburnsview.RandomTransitionGenerator

private const val KEN_BURNS_CYCLE_MILLIS = 6_000

private const val DIM_ANIM_MILLIS = 300

/**
 * Full-player backdrop that mirrors the FlamingoSank floating-light effect.
 *
 * - The processed artwork (128 px square crop -> saturation x3 -> tonal stack ->
 *   pure-Kotlin 25 px Gaussian blur) fills the whole backdrop.
 * - KenBurnsView owns the random viewport sweep through
 *   RandomTransitionGenerator(6000, AccelerateDecelerateInterpolator()). While
 *   [animateDrift] is false the sweep pauses in place, resuming when it becomes
 *   true again.
 * - A second overlay dims the backdrop with 0x33000000 OVERLAY tint. Its
 *   theme-dependent alpha animates with a 300 ms FastOutSlowIn tween.
 *
 * The displayed artwork switches instantly on track change;
 * [fallbackBitmap] is only used while the current backdrop is still being
 * processed so the screen does not flash black.
 */
@Composable
internal fun ArtworkKenBurnsBackdrop(
    bitmap: Bitmap?,
    fallbackBitmap: Bitmap?,
    animateDrift: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    // FlamingoSank gates the drift with lifecycle >= RESUMED in addition to
    // isPlaying; mirror that so the backdrop freezes in the background.
    val lifecycleState = LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val lifecycleActive =
        lifecycleState.value.isAtLeast(Lifecycle.State.RESUMED)
    val driftEnabled = animateDrift && lifecycleActive

    val dimTargetAlpha = if (isDark) 0.7f else 0.1f
    val dimAlpha by animateFloatAsState(
        targetValue = dimTargetAlpha,
        animationSpec = tween(
            durationMillis = DIM_ANIM_MILLIS,
            easing = FastOutSlowInEasing,
        ),
        label = "kenBurnsDimAlpha",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) Color.Black else Color.White)
            .clipToBounds(),
    ) {
        val displayedBitmap = bitmap ?: fallbackBitmap
        if (displayedBitmap != null) {
            AndroidView(
                factory = { context ->
                    StableKenBurnsView(context).apply {
                        setTransitionGenerator(
                            RandomTransitionGenerator(
                                KEN_BURNS_CYCLE_MILLIS.toLong(),
                                AccelerateDecelerateInterpolator(),
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
            ) { view ->
                view.updateArtwork(displayedBitmap, driftEnabled)
            }
            // Dim overlay: 0x33000000 with BlendMode.Overlay, exactly the
            // FlamingoSank stack. Alpha is animated so light/dark theme toggles
            // blend smoothly.
            Image(
                bitmap = displayedBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Offscreen buffer so the Overlay tint blends exactly,
                        // same as FlamingoSank's foreground AsyncImage.
                        compositingStrategy = CompositingStrategy.Offscreen
                        alpha = dimAlpha
                    },
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.tint(
                    Color(0x33000000.toInt()),
                    BlendMode.Overlay,
                ),
            )
        }
    }
}

/**
 * KenBurnsView begins with an identity Matrix. A paused image replacement must
 * draw once so its Matrix fits the measured viewport before the animation stops.
 */
private class StableKenBurnsView(context: Context) : KenBurnsView(context) {
    private var artwork: Bitmap? = null
    private var pauseAfterFirstFrame = false

    fun updateArtwork(
        bitmap: Bitmap,
        animateDrift: Boolean,
    ) {
        if (artwork !== bitmap) {
            artwork = bitmap
            setImageBitmap(bitmap)
            if (width > 0 && height > 0) {
                restart()
            }
            pauseAfterFirstFrame = !animateDrift
        }

        when {
            animateDrift -> {
                pauseAfterFirstFrame = false
                resume()
            }
            pauseAfterFirstFrame -> resume()
            else -> pause()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (pauseAfterFirstFrame) {
            pauseAfterFirstFrame = false
            pause()
        }
    }
}
