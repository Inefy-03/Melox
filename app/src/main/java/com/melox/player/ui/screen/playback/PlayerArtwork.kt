package com.melox.player.ui.screen.playback

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Rect
import com.melox.player.R
import com.melox.player.data.library.ArtistGroup
import com.melox.player.data.library.displayArtistName
import com.melox.player.model.MusicTrack
import com.melox.player.model.LyricsUiState
import com.melox.player.model.LyricsDocument
import com.melox.player.model.PlaybackMode
import com.melox.player.model.PlaybackBackgroundStyle
import com.melox.player.model.PlaybackQueueItem
import com.melox.player.model.PlaybackUiState
import com.melox.player.model.withTrackMetadata
import com.melox.player.ui.isMiuixWideLayout
import com.melox.player.ui.component.library.PlaybackArtworkFrame
import com.melox.player.ui.component.playback.playbackArtworkBounds
import com.melox.player.ui.component.library.PLAYBACK_ARTWORK_SHADOW_BLUR_RADIUS
import com.melox.player.ui.component.library.TrackActionsOverlay
import com.melox.player.ui.component.library.formatDuration
import com.melox.player.ui.component.playback.BlurredArtworkBackground
import com.melox.player.ui.component.playback.DynamicFlowBackground
import com.melox.player.ui.component.playback.DynamicFlowBackgroundState
import com.melox.player.ui.component.playback.PLAYER_FULL_ARTWORK_CORNER_RADIUS
import com.melox.player.ui.component.playback.PLAYER_FULL_ARTWORK_REQUEST_SIZE
import com.melox.player.ui.component.playback.PLAYER_TRACK_ARTWORK_CROSSFADE_DURATION_MILLIS
import com.melox.player.ui.component.playback.PLAYER_TRACK_ARTWORK_CROSSFADE_EASING
import com.melox.player.ui.component.playback.ArtworkFlipState
import com.melox.player.ui.component.playback.ArtworkTransitionState
import com.melox.player.ui.component.playback.ArtworkFlipLayer
import com.melox.player.ui.component.playback.rememberPlaybackArtworkResource
import com.melox.player.ui.component.playback.playerControlIconTransition
import com.melox.player.ui.component.playback.recordPlayerLayer
import com.melox.player.ui.component.playback.WeightedCrossfadeFrame
import com.melox.player.ui.component.playback.rememberReadyArtworkTarget
import com.melox.player.ui.component.playback.rememberArtworkCrossfadeState
import com.melox.player.ui.component.playback.rememberArtworkFlipState
import com.melox.player.ui.component.playback.rememberPlayerSheetVerticalDragModifier
import com.melox.player.ui.component.playback.playerSheetPlaceholderArtworkAlpha
import com.melox.player.ui.component.playback.artworkInsetRect
import com.melox.player.ui.component.playback.fittedArtworkRect
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.BasicComponent
import com.melox.player.ui.component.os4.Icon
import com.melox.player.ui.component.os4.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Playlist
import top.yukonga.miuix.kmp.icon.extended.Tune
import com.melox.player.ui.component.os4.OverlayBottomSheet
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.melox.player.ui.component.overScrollVertical

internal data class ArtworkBlend(
    // Observe changing fade weights only inside artwork consumers, not the player host.
    val frames: () -> List<WeightedCrossfadeFrame<Bitmap>>,
    val currentBitmap: Bitmap?,
    val flipState: ArtworkFlipState<Bitmap>? = null,
    val isPlaceholder: Boolean = false,
)

internal fun ArtworkTransitionState<Bitmap>.toArtworkBlend(): ArtworkBlend = ArtworkBlend(
    frames = { frames },
    currentBitmap = currentValue,
    flipState = flipState,
    isPlaceholder = isPlaceholderArtwork,
)

// This stateless comparison must not occupy a Compose remembered-lambda slot.
private val sameArtworkBitmap: (Bitmap, Bitmap) -> Boolean = { first, second -> first === second }

@Composable
internal fun rememberArtworkBlend(
    targetBitmap: Bitmap?,
    animate: Boolean,
    durationMillis: Int = PLAYER_TRACK_ARTWORK_CROSSFADE_DURATION_MILLIS,
    contentKey: Any? = targetBitmap,
    sameContentDurationMillis: Int? = null,
    transitionDirection: Int = 1,
    flipAnimationEnabled: Boolean = false,
    targetReady: Boolean = true,
    easing: Easing = PLAYER_TRACK_ARTWORK_CROSSFADE_EASING,
): ArtworkBlend {
    val target = rememberReadyArtworkTarget(targetBitmap, contentKey, targetReady)
    if (flipAnimationEnabled) {
        val flip = rememberArtworkFlipState(
            target = target,
            requestedContentKey = contentKey,
            targetReady = targetReady,
            animate = animate,
            direction = transitionDirection,
            durationMillis = durationMillis,
        )
        val visible = flip.visibleValue
        return ArtworkBlend(
            frames = {
                if (flip.isRunning) emptyList()
                else listOfNotNull(visible?.let { WeightedCrossfadeFrame(it, 1f) })
            },
            currentBitmap = visible,
            flipState = flip.takeIf { it.isRunning },
        )
    }
    val fade = rememberArtworkCrossfadeState(
        target = target,
        animate = animate,
        durationMillis = durationMillis,
        sameContentDurationMillis = sameContentDurationMillis,
        easing = easing,
        sameValue = sameArtworkBitmap,
    )
    return ArtworkBlend(frames = { fade.frames }, currentBitmap = fade.currentValue)
}

@Composable
internal fun PlayerArtwork(
    size: Dp,
    artworkPadding: Dp,
    artworkBlend: ArtworkBlend,
    cornerRadius: Dp,
    sharedArtworkVisible: Boolean,
    onArtworkBoundsChanged: (Rect) -> Unit,
    onArtworkDisplaySizeChanged: (Int) -> Unit = {},
    circularArtwork: Boolean = false,
    squareArtwork: Boolean = false,
    showArtwork: Boolean = true,
) {
    // 方形裁剪：圆形或"圆角矩形"都裁成正方形；默认形状保留等比边长条。
    val squareClip = circularArtwork || squareArtwork
    val resolvedContentScale = if (squareClip) ContentScale.Crop else ContentScale.Fit
    val artworkContainerSize = size + PLAYER_ARTWORK_CONTAINER_EXPANSION
    val placeholderArtworkAlpha = playerSheetPlaceholderArtworkAlpha(1f)
    val artworkContentSize =
        (artworkContainerSize - artworkPadding * 2f).coerceAtLeast(0.dp)
    // The playing state expands the artwork content back to the measured size.
    // Decode for that maximum visible edge so padding motion never triggers new requests.
    val artworkDisplaySizePx = with(LocalDensity.current) { size.roundToPx() }
    SideEffect {
        onArtworkDisplaySizeChanged(artworkDisplaySizePx)
    }
    val density = LocalDensity.current
    var artworkLayoutBounds by remember { mutableStateOf(Rect.Zero) }
    val artworkWindowSize = LocalWindowInfo.current.containerSize
    var boundsWindowSize by remember { mutableStateOf(artworkWindowSize) }
    fun reportArtworkBounds(bounds: Rect) {
        if (bounds.width > 0f && bounds.height > 0f) {
            val insetArtworkBounds = artworkInsetRect(
                bounds = bounds,
                inset = with(density) { artworkPadding.toPx() },
            )
            val visibleArtworkBounds = artworkBlend.currentBitmap?.let { bitmap ->
                playbackArtworkBounds(
                    bounds = insetArtworkBounds,
                    bitmapWidth = bitmap.width,
                    bitmapHeight = bitmap.height,
                    circular = squareClip,
                )
            } ?: insetArtworkBounds
            onArtworkBoundsChanged(visibleArtworkBounds)
        }
    }
    SideEffect {
        if (boundsWindowSize == artworkWindowSize) reportArtworkBounds(artworkLayoutBounds)
    }
    Box(
        modifier = Modifier
            .size(artworkContainerSize)
            .onGloballyPositioned { coordinates ->
                artworkLayoutBounds = coordinates.findRootCoordinates().localBoundingBoxOf(
                    sourceCoordinates = coordinates,
                    clipBounds = false,
                )
                boundsWindowSize = artworkWindowSize
                reportArtworkBounds(artworkLayoutBounds)
            }
            .graphicsLayer {
                alpha = if (sharedArtworkVisible) 1f else 0f
                clip = false
            },
        contentAlignment = Alignment.Center,
    ) {
        if (!showArtwork) {
            // Keep the measured slot for the player layout, but omit the cover and shadow.
        } else if (artworkBlend.flipState != null) {
            ArtworkFlipLayer(artworkBlend.flipState,
                contentScale = resolvedContentScale) { face, blur ->
                PlaybackArtworkFrame(
                    bitmap = face,
                    size = artworkContentSize,
                    cornerRadius = cornerRadius,
                    contentScale = resolvedContentScale,
                    useSquircleClip = true,
                    circular = circularArtwork,
                    drawArtworkShadow = true,
                    artworkAlpha = if (artworkBlend.flipState.visibleIsPlaceholder) placeholderArtworkAlpha else 1f,
                    artworkEffect = blur,
                )
            }
        } else if (artworkBlend.frames().isEmpty()) {
            PlaybackArtworkFrame(
                bitmap = null,
                size = artworkContentSize,
                cornerRadius = cornerRadius,
                modifier = Modifier,
                contentScale = resolvedContentScale,
                useSquircleClip = true,
                circular = circularArtwork,
                drawArtworkShadow = true,
                artworkAlpha = if (artworkBlend.isPlaceholder) placeholderArtworkAlpha else 1f,
            )
        } else {
            artworkBlend.frames().forEach { frame ->
                PlaybackArtworkFrame(
                    bitmap = frame.value,
                    size = artworkContentSize,
                    cornerRadius = cornerRadius,
                    modifier = Modifier,
                    contentScale = resolvedContentScale,
                    useSquircleClip = true,
                    circular = circularArtwork,
                    drawArtworkShadow = true,
                    artworkAlpha = frame.alpha * if (frame.isPlaceholder) placeholderArtworkAlpha else 1f,
                )
            }
        }
    }
}
