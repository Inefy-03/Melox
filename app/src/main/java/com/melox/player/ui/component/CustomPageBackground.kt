package com.melox.player.ui.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.layout.ContentScale
import com.melox.player.ui.component.*
import com.melox.player.ui.LocalFloatingNavigationRailInset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.captionBar
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onLayoutRectChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import com.melox.player.data.repository.CustomBackgroundRepository
import com.melox.player.data.repository.LoadedCustomBackground
import com.melox.player.model.AppSettings
import com.melox.player.model.normalizeCustomBackgroundBlurPercent
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val LocalCustomPageBackground = compositionLocalOf<ImageBitmap?> { null }
internal val LocalCustomBackgroundDimAlpha = compositionLocalOf { 0f }
internal val LocalTopBarWallpaperVisible = compositionLocalOf { false }
internal val LocalRootPageWallpaperFixed = compositionLocalOf { false }
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
    val backdrop = parentBackground?.backdrop ?: rememberLayerBackdrop()
    val fixedBackground = remember(backdrop, parentBackground) {
        FixedPageBackground(backdrop) {
            currentRefreshSignal() + (parentBackground?.refreshSignal?.invoke() ?: 0f)
        }
    }
    // Keep the pager mounted when an image is selected, removed or still decoding.
    Box(modifier) {
        if (parentBackground == null) {
            // Match GlassPage's Wallpaper: capture the image separately from page controls.
            Box(Modifier.matchParentSize().layerBackdrop(backdrop).background(surfaceColor)) {
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    if (dimAlpha > 0f) {
                        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = dimAlpha.coerceIn(0f, 1f))))
                    }
                }
            }
        }
        if (image != null && parentBackground != null) {
            // Fill exposed pager edges even when an intervening navigation host is opaque.
            Box(Modifier.matchParentSize().fixedWallpaperRegion(parentBackground))
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
    val background = LocalFixedPageBackground.current ?: return this
    return fixedWallpaperRegion(background)
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
    val clippedRailInset = com.melox.player.ui.LocalClippedNavigationRailInset.current
    val wideSidebar = com.melox.player.ui.LocalLandscapeNavigation.current != null
    val pageBackdrop = rememberLayerBackdrop()
    val wallpaperBackdrop = rememberLayerBackdrop()
    val effectsActive = LocalTopBarBlurSettings.current.blurEnabled && isRuntimeShaderSupported()
    val topBackdrop = pageBackdrop.takeIf { effectsActive }
    val scrollContent = remember { TopBarScrollContent() }
    val searchPage = remember { GlassSearchPage() }
    val wideSearchHost = LocalWideGlassSearchHost.current
    androidx.compose.runtime.DisposableEffect(wideSearchHost, searchPage) {
        wideSearchHost?.pages?.add(searchPage)
        onDispose { wideSearchHost?.pages?.remove(searchPage) }
    }
    val searchActive = searchPage.isExpanded || searchPage.transitionActive
    val hasSearchQuery = showGlassSearchResults(
        searchPage.isExpanded, searchPage.session?.query?.value.orEmpty(),
    )
    val resultsReady = showGlassSearchResults(
        searchPage.isExpanded,
        searchPage.session?.query?.value.orEmpty(),
        searchPage.session?.resultsQuery?.value.orEmpty(),
    )
    val awaitingResults = hasSearchQuery && !resultsReady
    val showSearchResults = !wideSidebar && hasSearchQuery
    val hidePageContent = (!wideSidebar && searchPage.isExpanded && !hasSearchQuery) || awaitingResults
    val blockPageContent = (searchActive && !wideSidebar && !hasSearchQuery) || awaitingResults
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val image = LocalCustomPageBackground.current
    val dimAlpha = LocalCustomBackgroundDimAlpha.current
    val surfaceColor = MiuixTheme.colorScheme.surface
    val fixedWallpaper = LocalFixedPageBackground.current.takeIf { LocalRootPageWallpaperFixed.current }
    val surfaceBackdrop = wallpaperBackdrop.takeIf { image != null && effectsActive }
    val statusBarSpace = WindowInsets.statusBarsIgnoringVisibility
        .union(WindowInsets.captionBar).union(WindowInsets.displayCutout).only(WindowInsetsSides.Top)
    Box(modifier = modifier.onGloballyPositioned { scrollContent.bounds = it.boundsInRoot() }) {
        CompositionLocalProvider(LocalPageSurfaceBackdrop provides surfaceBackdrop, LocalGlassSearchPage provides searchPage, LocalTopBarScrollContent provides scrollContent) {
            Scaffold(
                containerColor = surfaceColor,
                contentWindowInsets = WindowInsets.systemBars
                    .union(WindowInsets.displayCutout)
                    .union(statusBarSpace),
                topBar = {
                    CompositionLocalProvider(LocalTopBarWallpaperVisible provides (image != null), LocalWideTopBackdrop provides topBackdrop) {
                        Box(
                            Modifier.padding(start = clippedRailInset),
                        ) {
                            topBar()
                        }
                    }
                },
            ) { padding ->
                val pagePadding = if (showSearchResults) PaddingValues(
                    start = padding.calculateStartPadding(layoutDirection) + floatingRailInset,
                    top = with(density) { statusBarSpace.getTop(this).toDp() } + 52.dp,
                    end = padding.calculateEndPadding(layoutDirection),
                    bottom = padding.calculateBottomPadding(),
                ) else PaddingValues(
                    start = padding.calculateStartPadding(layoutDirection) + floatingRailInset,
                    top = padding.calculateTopPadding(),
                    end = padding.calculateEndPadding(layoutDirection),
                    bottom = padding.calculateBottomPadding(),
                )
                // Keep the same filtered list and state underneath the search header.
                Box(Modifier.fillMaxSize()
                    .then(if (showSearchResults) Modifier.imePadding() else Modifier)
                    .layerBackdrop(pageBackdrop)
                    .then(if (blockPageContent) Modifier.clearAndSetSemantics { }.pointerInput(Unit) {
                        awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } }
                    } else Modifier)) {
                    // Root pages sample the stationary window image; secondary pages crop locally.
                    Box(Modifier.matchParentSize().layerBackdrop(wallpaperBackdrop).background(surfaceColor)
                        .then(fixedWallpaper?.let { Modifier.fixedWallpaperRegion(it) } ?: Modifier)) {
                        if (image != null && fixedWallpaper == null) {
                            Image(bitmap = image, contentDescription = null,
                                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            if (dimAlpha > 0f) Box(Modifier.matchParentSize()
                                .background(Color.Black.copy(alpha = dimAlpha.coerceIn(0f, 1f))))
                        }
                    }
                    Box(Modifier.fillMaxSize()
                        .graphicsLayer { alpha = if (hidePageContent) 0f else 1f }) {
                        content(pagePadding)
                    }
                }
            }
            searchPage.session?.let { session ->
                CompositionLocalProvider(LocalWideTopBackdrop provides topBackdrop) {
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
