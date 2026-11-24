package com.melox.player.ui.component.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.melox.player.R
import com.melox.player.ui.component.LocalTopBarBlurSettings
import com.melox.player.ui.icon.GlassIcons
import com.melox.player.ui.icon.Download
import com.melox.player.ui.icon.Share
import com.melox.player.ui.glass.GlassTopAppBar
import com.melox.player.ui.glass.GlassTopAppBarDefaults
import com.melox.player.ui.glass.GlassIconButton
import com.melox.player.model.MusicTrack
import com.melox.player.ui.component.rememberBlurBackdrop
import com.melox.player.ui.component.playback.sourceOverAlphas
import com.melox.player.ui.screen.playback.rememberArtworkBlend
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import top.yukonga.miuix.kmp.anim.DecelerateEasing
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import com.melox.player.ui.component.Icon
import com.melox.player.ui.component.navigationBackIcon
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.squircle.squircleClip
import kotlin.math.min
import kotlin.math.roundToInt

internal data class ArtworkPreviewRequest(
    val contentUri: String,
    val title: String?,
    val sourceBounds: () -> Rect,
    val thumbnail: Bitmap,
    val sourceCornerRadius: Dp = 8.dp,
    val circularSource: Boolean = false,
)

@Composable
internal fun PreviewableTrackArtwork(
    track: MusicTrack,
    size: Dp,
    cornerRadius: Dp,
    hidden: Boolean,
    onPreview: (ArtworkPreviewRequest) -> Unit,
) {
    val bitmap = rememberArtworkBitmap(track.contentUri, track.dateModifiedEpochSeconds,
        track.fileSizeBytes, size)
    var bounds by remember { mutableStateOf(Rect.Zero) }
    if (bitmap == null) {
        TrackArtwork(track.contentUri, track.dateModifiedEpochSeconds, track.fileSizeBytes,
            size = size, cornerRadius = cornerRadius)
    } else {
        Card(
            modifier = Modifier
                .size(size)
                .onGloballyPositioned { bounds = it.boundsInWindow() }
                .graphicsLayer { alpha = if (hidden) 0f else 1f },
            cornerRadius = cornerRadius,
            insideMargin = PaddingValues(0.dp),
            colors = CardDefaults.defaultColors(color = Color.Transparent),
            pressFeedbackType = PressFeedbackType.Sink,
            onClick = if (hidden) null else {
                {
                    if (!bounds.isEmpty) {
                        onPreview(ArtworkPreviewRequest(track.contentUri, track.title, { bounds }, bitmap))
                    }
                }
            },
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.artwork_preview),
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High,
                modifier = Modifier
                    .fillMaxSize()
                    .squircleClip(cornerRadius),
            )
        }
    }
}

/** Uses the same window coordinates for the sheet thumbnail and the root popup. */
internal fun artworkPreviewBounds(
    width: Float,
    height: Float,
    imageWidth: Int,
    imageHeight: Int,
): Rect {
    val availableWidth = width.coerceAtLeast(1f)
    val availableHeight = height.coerceAtLeast(1f)
    val scale = min(availableWidth / imageWidth.coerceAtLeast(1),
        availableHeight / imageHeight.coerceAtLeast(1))
    val fittedWidth = imageWidth.coerceAtLeast(1) * scale
    val fittedHeight = imageHeight.coerceAtLeast(1) * scale
    return Rect((width - fittedWidth) / 2f, (height - fittedHeight) / 2f,
        (width + fittedWidth) / 2f, (height + fittedHeight) / 2f)
}

internal fun clampArtworkPan(offset: Offset, image: Rect, zoom: Float, viewport: Rect): Offset {
    val limitX = ((image.width * zoom - viewport.width) / 2f).coerceAtLeast(0f)
    val limitY = ((image.height * zoom - viewport.height) / 2f).coerceAtLeast(0f)
    return Offset(
        if (limitX == 0f) 0f else offset.x.coerceIn(-limitX, limitX),
        if (limitY == 0f) 0f else offset.y.coerceIn(-limitY, limitY),
    )
}

internal fun artworkZoomOffset(
    offset: Offset,
    centroid: Offset,
    center: Offset,
    pan: Offset,
    ratio: Float,
): Offset = offset * ratio + (centroid - center) * (1f - ratio) + pan

internal fun artworkDoubleTapZoom(image: Rect, viewport: Rect): Float = maxOf(
    2f,
    viewport.width / image.width.coerceAtLeast(1f),
    viewport.height / image.height.coerceAtLeast(1f),
).coerceAtMost(8f)

internal fun artworkPreviewImagePoint(position: Offset, image: Rect): Offset = Offset(
    position.x.coerceIn(image.left, image.right),
    position.y.coerceIn(image.top, image.bottom),
)

internal fun artworkPreviewTransitionBounds(source: Rect, target: Rect, progress: Float): Rect {
    return androidx.compose.ui.geometry.lerp(source, target, progress.coerceIn(0f, 1f))
}

internal fun artworkPreviewUsesLightChrome(surfaceColor: Color): Boolean =
    surfaceColor.luminance() >= 0.5f

internal fun artworkPreviewBackgroundColor(
    usesLightChrome: Boolean,
    toolbarVisible: Boolean,
): Color = if (usesLightChrome && toolbarVisible) Color.White else Color.Black

internal fun artworkPreviewChromeAlpha(zoom: Float): Float =
    ((zoom - ARTWORK_PREVIEW_DISMISS_ZOOM) / (1f - ARTWORK_PREVIEW_DISMISS_ZOOM)).coerceIn(0f, 1f)

internal fun artworkPreviewShouldDismiss(zoom: Float): Boolean =
    zoom <= ARTWORK_PREVIEW_DISMISS_ZOOM

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ArtworkPreviewOverlay(
    request: ArtworkPreviewRequest,
    onPresented: (Boolean) -> Unit,
    onClosed: () -> Unit,
) {
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.artwork_share)
    val scope = rememberCoroutineScope()
    val visible = remember { mutableStateOf(true) }
    var closing by remember { mutableStateOf(false) }
    var toolbarVisible by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var zoomAnimation by remember { mutableStateOf<Job?>(null) }
    val progress = remember { Animatable(0f) }
    val themeSurfaceColor = MiuixTheme.colorScheme.surface
    val usesLightChrome = remember(themeSurfaceColor) {
        artworkPreviewUsesLightChrome(themeSurfaceColor)
    }
    val activity = remember(context) { context.findActivity() }
    val systemBarsController = remember(activity) {
        activity?.let { owner ->
            WindowCompat.getInsetsController(owner.window, owner.window.decorView)
        }
    }
    val originalSystemBarAppearance = remember(systemBarsController) {
        systemBarsController?.let { controller ->
            SystemBarAppearance(
                lightStatusBars = controller.isAppearanceLightStatusBars,
                lightNavigationBars = controller.isAppearanceLightNavigationBars,
            )
        }
    }
    DisposableEffect(systemBarsController, originalSystemBarAppearance) {
        onDispose {
            originalSystemBarAppearance?.let { appearance ->
                systemBarsController?.isAppearanceLightStatusBars = appearance.lightStatusBars
                systemBarsController?.isAppearanceLightNavigationBars =
                    appearance.lightNavigationBars
            }
        }
    }
    val pinchChromeAlpha = artworkPreviewChromeAlpha(zoom)
    val toolbarAlpha by animateFloatAsState(
        targetValue = if (toolbarVisible) 1f else 0f,
        animationSpec = tween(ARTWORK_PREVIEW_CHROME_TRANSITION_DURATION_MILLIS),
        label = "artworkPreviewToolbar",
    )
    val toolbarSurfaceColor = artworkPreviewBackgroundColor(usesLightChrome, toolbarVisible = true)
    val previewBackgroundColor = lerp(Color.Black, toolbarSurfaceColor, toolbarAlpha)
    val previewProgress = progress.value.coerceIn(0f, 1f)
    val previewAlpha = previewProgress * pinchChromeAlpha
    val lightSystemBars = previewBackgroundColor.luminance() >= 0.5f
    val systemBarAppearance = if (previewAlpha >= 0.5f) {
        SystemBarAppearance(lightSystemBars, lightSystemBars)
    } else originalSystemBarAppearance
    LaunchedEffect(systemBarsController, systemBarAppearance) {
        systemBarAppearance?.let {
            systemBarsController?.isAppearanceLightStatusBars = it.lightStatusBars
            systemBarsController?.isAppearanceLightNavigationBars = it.lightNavigationBars
        }
    }
    val previewContentColor = if (usesLightChrome) Color.Black else Color.White
    val asset by produceState<ArtworkPreviewFile?>(null, request.contentUri) {
        value = readArtworkPreviewFile(context.applicationContext, request.contentUri, request.title)
        if (value == null) {
            Toast.makeText(context, R.string.artwork_unavailable, Toast.LENGTH_SHORT).show()
        }
    }
    val saveToGallery: () -> Unit = {
        asset?.takeIf { !busy }?.let { original ->
            busy = true
            scope.launch {
                val saved = saveArtworkToGallery(context.applicationContext, original)
                busy = false
                Toast.makeText(context, if (saved) R.string.artwork_saved else
                    R.string.artwork_export_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) saveToGallery() else {
            Toast.makeText(context, R.string.artwork_save_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }
    MiuixPopupUtils.PopupLayout(
        visible = visible,
        enterTransition = EnterTransition.None,
        exitTransition = ExitTransition.None,
        enableWindowDim = false,
        enableBackHandler = false,
    ) {
        DisposableEffect(Unit) {
            onPresented(true)
            onDispose { onPresented(false) }
        }
        NavigationBackHandler(
            state = rememberNavigationEventState(NavigationEventInfo.None),
            isBackEnabled = true,
            onBackCompleted = { if (!busy) closing = true },
        )
        LaunchedEffect(closing) {
            if (closing) zoomAnimation?.cancel()
            progress.animateTo(if (closing) 0f else 1f,
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = 500f,
                    visibilityThreshold = 0.0001f,
                ))
            if (closing) {
                visible.value = false
                onPresented(false)
                onClosed()
            }
        }
        val density = LocalDensity.current
        val topBarBackdrop = rememberBlurBackdrop(includeCustomBackground = false)
        var origin by remember { mutableStateOf(Offset.Zero) }
        BoxWithConstraints(Modifier.fillMaxSize().clipToBounds().onGloballyPositioned {
            origin = it.positionInWindow()
        }) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()
            val target = artworkPreviewBounds(
                width, height,
                request.thumbnail.width, request.thumbnail.height,
            )
            val viewport = Rect(0f, 0f, width, height)
            LaunchedEffect(target) {
                zoomAnimation?.cancel()
                zoom = 1f
                panOffset = Offset.Zero
            }
            var decodeSize by remember(target) {
                mutableIntStateOf(maxOf(target.width, target.height).roundToInt())
            }
            LaunchedEffect(target, asset, closing) {
                if (closing) return@LaunchedEffect
                snapshotFlow {
                    fullPlayerArtworkTargetSizePx((maxOf(target.width, target.height) *
                        zoom.coerceAtLeast(1f))
                        .roundToInt().coerceAtMost(asset?.let { maxOf(it.width, it.height) } ?: Int.MAX_VALUE))
                }.collectLatest { size ->
                    delay(80)
                    decodeSize = size
                }
            }
            val preview by produceState<Bitmap?>(null, asset, decodeSize) {
                asset?.let { decodeArtworkPreview(it, decodeSize) }?.let {
                    value = it
                }
            }
            val artworkBlend = rememberArtworkBlend(
                targetBitmap = if (closing) request.thumbnail else preview ?: request.thumbnail,
                animate = true,
                durationMillis = 100,
            )
            val artworkLayerAlphas = sourceOverAlphas(artworkBlend.frames().map { it.alpha })
            val source = request.sourceBounds().translate(-origin)
            val rect = artworkPreviewTransitionBounds(source, target, previewProgress)
            val artworkScale = 1f + (zoom - 1f) * previewProgress
            val artworkTranslation = panOffset * previewProgress
            val artworkCenter = rect.center + artworkTranslation
            val halfArtworkSize = Offset(rect.width, rect.height) * (artworkScale / 2f)
            val displayedArtwork = Rect(artworkCenter - halfArtworkSize, artworkCenter + halfArtworkSize)
            Box(Modifier.fillMaxSize()
                .then(topBarBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)) {
            Box(Modifier.fillMaxSize()
                .graphicsLayer { alpha = previewAlpha }
                .background(previewBackgroundColor))
            Box(Modifier.fillMaxSize().pointerInput(target, closing) {
                detectTapGestures(onTap = {
                    if (!closing && progress.value >= 0.999f) toolbarVisible = !toolbarVisible
                }, onDoubleTap = { position ->
                    if (!closing && progress.value >= 0.999f) {
                        zoomAnimation?.cancel()
                        val startZoom = zoom
                        val startPan = panOffset
                        val endZoom = if (zoom > 1.01f) 1f else artworkDoubleTapZoom(target, viewport)
                        if (endZoom > startZoom) toolbarVisible = false
                        val endPan = clampArtworkPan(
                            artworkZoomOffset(startPan, artworkPreviewImagePoint(position, target),
                                target.center, Offset.Zero, endZoom / startZoom),
                            target, endZoom, viewport,
                        )
                        zoomAnimation = scope.launch {
                            animate(0f, 1f, animationSpec = tween(220, easing = DecelerateEasing(1.5f))) { fraction, _ ->
                                zoom = startZoom + (endZoom - startZoom) * fraction
                                panOffset = clampArtworkPan(
                                    androidx.compose.ui.geometry.lerp(startPan, endPan, fraction),
                                    target, zoom, viewport,
                                )
                            }
                        }
                    }
                })
            }.pointerInput(target, closing) {
                detectTransformGestures { centroid, pan, gestureZoom, _ ->
                    if (!closing && progress.value >= 0.999f && (zoom > 1f || gestureZoom != 1f)) {
                        zoomAnimation?.cancel()
                        val nextZoom = (zoom * gestureZoom).coerceIn(ARTWORK_PREVIEW_MIN_ZOOM, 8f)
                        if (nextZoom > 1.01f && nextZoom > zoom) toolbarVisible = false
                        panOffset = clampArtworkPan(
                            artworkZoomOffset(panOffset, centroid, target.center, pan, nextZoom / zoom),
                            target, nextZoom, viewport,
                        )
                        zoom = nextZoom
                    }
                }
            }.pointerInput(target, closing) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var hadMultiplePointers = false
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        if (event.changes.count { it.pressed } >= 2) hadMultiplePointers = true
                    } while (event.changes.any { it.pressed })
                    if (hadMultiplePointers && !closing && progress.value >= 0.999f && zoom < 1f) {
                        if (artworkPreviewShouldDismiss(zoom)) {
                            closing = true
                        } else {
                            zoomAnimation?.cancel()
                            val startZoom = zoom
                            zoomAnimation = scope.launch {
                                animate(startZoom, 1f,
                                    animationSpec = tween(220, easing = DecelerateEasing(1.5f))) {
                                    value, _ -> zoom = value
                                }
                                panOffset = Offset.Zero
                            }
                        }
                    }
                }
            }) {
                val sourceAlpha = 1f - previewProgress
                val coverClip = if (request.circularSource) {
                    val radius = with(density) { (minOf(source.width, source.height) / 2f).toDp() }
                    Modifier.clip(RoundedCornerShape(radius * sourceAlpha))
                } else {
                    Modifier.squircleClip(request.sourceCornerRadius * sourceAlpha)
                }
                Box(Modifier
                        .absoluteOffset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
                        .requiredSize(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() })
                        .graphicsLayer {
                            scaleX = artworkScale
                            scaleY = artworkScale
                            translationX = artworkTranslation.x
                            translationY = artworkTranslation.y
                        }
                        .then(coverClip)) {
                    artworkBlend.frames().forEachIndexed { index, frame ->
                        Image(
                            bitmap = frame.value.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = artworkLayerAlphas[index] },
                            contentScale = ContentScale.Crop,
                            filterQuality = FilterQuality.High,
                        )
                    }
                }
            }
            }
            val toolbarInteractive = toolbarVisible && pinchChromeAlpha > 0f && !closing
            Box(
                Modifier
                    .graphicsLayer {
                        alpha = previewAlpha * toolbarAlpha
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                    }
                    .semantics {
                        if (!toolbarInteractive) hideFromAccessibility()
                    },
            ) {
                GlassTopAppBar(
                    title = asset?.let { stringResource(R.string.artwork_dimensions, it.width, it.height) }
                        .orEmpty(),
                    compact = true,
                    isContentScrolled = true,
                    backdrop = topBarBackdrop,
                    progressiveBlur = LocalTopBarBlurSettings.current.blurEnabled,
                    bandBrush = GlassTopAppBarDefaults.bandBrush(toolbarSurfaceColor),
                    backgroundModifier = Modifier.drawWithContent {
                        // The shared backdrop already colors the rest of the screen.
                        val overlap = displayedArtwork.intersect(Rect(Offset.Zero, size))
                        if (!overlap.isEmpty) {
                            clipRect(overlap.left, overlap.top, overlap.right, overlap.bottom) {
                                this@drawWithContent.drawContent()
                            }
                        }
                    },
                    contentModifier = Modifier.windowInsetsPadding(
                        WindowInsets.statusBarsIgnoringVisibility.union(WindowInsets.captionBar)
                            .only(WindowInsetsSides.Top),
                    ),
                    navigationIcon = {
                        GlassIconButton(enabled = toolbarInteractive && !busy,
                            onClick = { closing = true }) {
                            Icon(navigationBackIcon(), stringResource(R.string.back),
                                modifier = Modifier.size(28.dp), tint = previewContentColor)
                        }
                    },
                    actions = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassIconButton(enabled = toolbarInteractive && asset != null && !busy, onClick = {
                                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                                    PackageManager.PERMISSION_GRANTED) {
                                    permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                } else {
                                    saveToGallery()
                                }
                            }) {
                                Icon(GlassIcons.Download, stringResource(R.string.artwork_download),
                                    modifier = Modifier.size(28.dp), tint = previewContentColor)
                            }
                            GlassIconButton(enabled = toolbarInteractive && asset != null && !busy, onClick = {
                                asset?.let { original ->
                                    runCatching {
                                        val uri = FileProvider.getUriForFile(context,
                                            "${context.packageName}.fileprovider", original.file, original.displayName)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = original.mimeType
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            clipData = ClipData.newRawUri(original.displayName, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent,
                                            shareTitle))
                                    }.onFailure {
                                        Toast.makeText(context, R.string.artwork_export_failed, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                                Icon(GlassIcons.Share, stringResource(R.string.artwork_share),
                                    modifier = Modifier.size(28.dp), tint = previewContentColor)
                            }
                        }
                    },
                )
            }
        }
    }
}

private const val ARTWORK_PREVIEW_CHROME_TRANSITION_DURATION_MILLIS = 180
private const val ARTWORK_PREVIEW_MIN_ZOOM = 0.55f
private const val ARTWORK_PREVIEW_DISMISS_ZOOM = 0.72f

private data class SystemBarAppearance(
    val lightStatusBars: Boolean,
    val lightNavigationBars: Boolean,
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
