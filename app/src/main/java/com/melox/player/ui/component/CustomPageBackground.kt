package com.melox.player.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Box
import com.melox.player.ui.component.os4.*
import com.melox.player.ui.LocalFloatingNavigationRailInset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onLayoutRectChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.melox.player.data.repository.CustomBackgroundRepository
import com.melox.player.data.repository.LoadedCustomBackground
import com.melox.player.model.AppSettings
import com.melox.player.model.normalizeCustomBackgroundBlurPercent
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val LocalCustomPageBackground = compositionLocalOf<ImageBitmap?> { null }
internal val LocalCustomBackgroundDimAlpha = compositionLocalOf { 0f }
internal val LocalTopBarWallpaperVisible = compositionLocalOf { false }
internal val LocalFixedPageBackground = compositionLocalOf<FixedPageBackground?> { null }

internal class FixedPageBackground(
    val backdrop: LayerBackdrop,
    refreshSignal: () -> Float,
) {
    var motionPosition by mutableStateOf(IntOffset.Zero)
    val refreshSignal: () -> Float = {
        refreshSignal() + motionPosition.x + motionPosition.y
    }
}

@Composable
internal fun Modifier.trackFixedWallpaperMotion(): Modifier {
    val background = LocalFixedPageBackground.current ?: return this
    return onLayoutRectChanged(throttleMillis = 0, debounceMillis = 0) {
        background.motionPosition = it.positionInRoot
    }
}

@Composable
internal fun FixedPageBackgroundHost(
    modifier: Modifier = Modifier,
    refreshSignal: () -> Float,
    content: @Composable BoxScope.() -> Unit,
) {
    val parentBackground = LocalFixedPageBackground.current
    val image = LocalCustomPageBackground.current
    val surfaceColor = MiuixTheme.colorScheme.surface
    val dimAlpha = LocalCustomBackgroundDimAlpha.current
    val currentRefreshSignal by rememberUpdatedState(refreshSignal)
    val backdrop = parentBackground?.backdrop ?: rememberLayerBackdrop {
        drawRect(surfaceColor)
        image?.let { drawCustomPageBackground(it, dimAlpha) }
    }
    val fixedBackground = remember(backdrop, parentBackground) {
        FixedPageBackground(backdrop) {
            currentRefreshSignal() + (parentBackground?.refreshSignal?.invoke() ?: 0f)
        }
    }
    // Keep the pager mounted when an image is selected, removed or still decoding.
    Box(if (parentBackground == null) modifier.customPageBackground() else modifier) {
        if (image != null && parentBackground == null) {
            Box(Modifier.matchParentSize().layerBackdrop(backdrop))
        }
        CompositionLocalProvider(LocalFixedPageBackground provides fixedBackground.takeIf { image != null }) {
            content()
        }
    }
}

@Composable
private fun Modifier.fixedWallpaperRegion(background: FixedPageBackground): Modifier {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var positionInRoot by remember { mutableStateOf(IntOffset.Zero) }
    return onGloballyPositioned { coordinates = it }.onLayoutRectChanged(throttleMillis = 0, debounceMillis = 0) {
        positionInRoot = it.positionInRoot
    }.drawWithCache {
        onDrawBehind {
            // Read inside this drawing layer so pager motion invalidates the sample.
            background.refreshSignal()
            @Suppress("UNUSED_VARIABLE")
            val regionPosition = positionInRoot
            val currentCoordinates = coordinates?.takeIf { it.isAttached }
            if (currentCoordinates != null) {
                clipRect {
                    with(background.backdrop) {
                        drawBackdrop(density = this@onDrawBehind, coordinates = currentCoordinates)
                    }
                }
            }
        }
    }
}

@Composable
internal fun rememberCustomPageBackground(
    settings: AppSettings,
    prepared: LoadedCustomBackground?,
    blurPercentOverride: Int? = null,
): ImageBitmap? {
    val id = settings.customBackgroundId ?: return null
    val percent = normalizeCustomBackgroundBlurPercent(blurPercentOverride ?: settings.customBackgroundBlurPercent)
    val preparedImage = remember(prepared?.rendered) { prepared?.rendered?.asImageBitmap() }
    // A prepared startup image is usable immediately, without another composition cycle.
    if (prepared?.id == id && prepared.blurPercent == percent) return preparedImage
    val context = LocalContext.current.applicationContext
    val repository = remember(context) { CustomBackgroundRepository(context) }
    return produceState<ImageBitmap?>(preparedImage, prepared, percent, id) {
        if (prepared == null) {
            value = null
        } else if (prepared.id == id) {
            value = if (prepared.blurPercent == percent) {
                preparedImage
            } else {
                repository.blurImage(prepared.source, percent)?.asImageBitmap()
            }
        }
    }.value
}

@Composable
internal fun Modifier.customPageBackground(): Modifier {
    val image = LocalCustomPageBackground.current ?: return this
    LocalFixedPageBackground.current?.let { return fixedWallpaperRegion(it) }
    val surfaceColor = MiuixTheme.colorScheme.surface
    val dimAlpha = LocalCustomBackgroundDimAlpha.current
    return drawWithCache {
        onDrawBehind {
            drawRect(surfaceColor)
            drawCustomPageBackground(image, dimAlpha)
        }
    }
}

@Composable
internal fun customPageContainerColor(): Color =
    if (LocalCustomPageBackground.current == null) MiuixTheme.colorScheme.surface else Color.Transparent

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun PageScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val floatingRailInset = LocalFloatingNavigationRailInset.current
    val wideOs4 = com.melox.player.ui.LocalOs4LandscapeNavigation.current != null
    var wideCaptureCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val wideTopBackdrop = if (wideOs4) rememberBlurBackdrop(captureCoordinates = { wideCaptureCoordinates }) else null
    val scrollContent = remember { TopBarScrollContent() }
    val searchPage = remember { GlassSearchPage() }
    val wideSearchHost = LocalWideGlassSearchHost.current
    androidx.compose.runtime.DisposableEffect(wideSearchHost, searchPage) {
        wideSearchHost?.pages?.add(searchPage)
        onDispose { wideSearchHost?.pages?.remove(searchPage) }
    }
    val searchActive = searchPage.isExpanded || searchPage.transitionActive
    val showSearchResults = !wideOs4 && showGlassSearchResults(
        searchPage.isExpanded, searchPage.session?.query?.value.orEmpty(),
    )
    val hidePageContent = !wideOs4 && searchPage.isExpanded && !showSearchResults
    val blockPageContent = searchActive && !wideOs4 && !showSearchResults
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val fixedBackground = LocalFixedPageBackground.current
    val surfaceBackdrop = rememberPageSurfaceBackdrop()
    val image = LocalCustomPageBackground.current
    val dimAlpha = LocalCustomBackgroundDimAlpha.current
    val surfaceColor = MiuixTheme.colorScheme.surface
    val statusBarSpace = WindowInsets.statusBarsIgnoringVisibility.only(WindowInsetsSides.Top)
    BoxWithConstraints(modifier = modifier.onGloballyPositioned { scrollContent.bounds = it.boundsInRoot() }) {
        val viewportSize = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        CompositionLocalProvider(LocalPageSurfaceBackdrop provides surfaceBackdrop, LocalGlassSearchPage provides searchPage, LocalTopBarScrollContent provides scrollContent) {
            Scaffold(
                // Each route must cover retained pages with its matching wallpaper region.
                modifier = (if (fixedBackground == null) {
                    Modifier.customPageBackground()
                } else {
                    Modifier.fixedWallpaperRegion(fixedBackground)
                }),
                containerColor = customPageContainerColor(),
                contentWindowInsets = WindowInsets.systemBars
                    .union(WindowInsets.displayCutout)
                    .union(statusBarSpace),
                topBar = {
                    CompositionLocalProvider(LocalTopBarWallpaperVisible provides (image != null), LocalWideTopBackdrop provides wideTopBackdrop) {
                        Box(
                            Modifier.padding(start = if (wideOs4) 0.dp else floatingRailInset).then(if (fixedBackground != null) {
                                Modifier.fixedWallpaperRegion(fixedBackground)
                            } else Modifier.drawWithCache {
                                onDrawBehind {
                                    // Reuse the page crop at Miuix's (0, 0) bar origin.
                                    // Keep the wallpaper base mounted through effect handoff.
                                    if (image != null) {
                                        drawRect(surfaceColor)
                                        drawCustomPageBackground(image, dimAlpha, viewportSize)
                                    }
                                }
                            }),
                        ) {
                            topBar()
                        }
                    }
                },
            ) { padding ->
                val pagePadding = if (showSearchResults) PaddingValues(
                    start = padding.calculateStartPadding(layoutDirection),
                    top = with(density) { statusBarSpace.getTop(this).toDp() } + 52.dp,
                    end = padding.calculateEndPadding(layoutDirection),
                    bottom = padding.calculateBottomPadding(),
                ) else padding
                // Keep the same filtered list and state underneath the search header.
                Box(Modifier.fillMaxSize()
                    .then(if (showSearchResults) Modifier.imePadding() else Modifier)
                    .onGloballyPositioned { wideCaptureCoordinates = it }
                    .then(wideTopBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
                    .padding(start = floatingRailInset).graphicsLayer { alpha = if (hidePageContent) 0f else 1f }
                    .then(if (blockPageContent) Modifier.clearAndSetSemantics { }.pointerInput(Unit) {
                        awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } }
                    } else Modifier)) {
                if (surfaceBackdrop == null || fixedBackground != null) {
                    content(pagePadding)
                } else {
                    Box(Modifier.fillMaxSize()) {
                        // Capture only the wallpaper, never the Cards that consume this layer.
                        Box(Modifier.matchParentSize().layerBackdrop(surfaceBackdrop))
                        content(pagePadding)
                    }
                }
                }
            }
            searchPage.session?.let { session ->
                CompositionLocalProvider(LocalWideTopBackdrop provides wideTopBackdrop) {
                    GlassSearchHeader(session, searchPage)
                }
            }
        }
    }
}

internal fun wallpaperTopBarVisible(
    hasBackground: Boolean,
    blurEnabled: Boolean,
    progressiveEnabled: Boolean,
    runtimeSupported: Boolean,
): Boolean = hasBackground && !(blurEnabled && progressiveEnabled && runtimeSupported)

internal fun DrawScope.drawCustomPageBackground(
    image: ImageBitmap,
    dimAlpha: Float = 0f,
    viewportSize: Size = size,
) {
    val destination = customBackgroundCropSize(image.width, image.height, viewportSize.width, viewportSize.height)
    if (destination == IntSize.Zero) return
    clipRect {
        drawImage(
            image = image,
            dstOffset = customBackgroundCropOffset(destination, viewportSize),
            dstSize = destination,
            filterQuality = FilterQuality.Medium,
        )
        if (dimAlpha > 0f) drawRect(Color.Black.copy(alpha = dimAlpha.coerceIn(0f, 1f)))
    }
}

internal fun customBackgroundCropOffset(destination: IntSize, viewportSize: Size): IntOffset = IntOffset(
    ((viewportSize.width - destination.width) / 2f).roundToInt(),
    ((viewportSize.height - destination.height) / 2f).roundToInt(),
)

internal fun customBackgroundCropSize(
    imageWidth: Int,
    imageHeight: Int,
    pageWidth: Float,
    pageHeight: Float,
): IntSize {
    if (imageWidth <= 0 || imageHeight <= 0 || pageWidth <= 0f || pageHeight <= 0f) return IntSize.Zero
    val scale = maxOf(pageWidth / imageWidth, pageHeight / imageHeight)
    return IntSize((imageWidth * scale).roundToInt(), (imageHeight * scale).roundToInt())
}
