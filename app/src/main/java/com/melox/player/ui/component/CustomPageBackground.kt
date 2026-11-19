package com.melox.player.ui.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.layout.ContentScale
import com.melox.player.ui.LocalFloatingNavigationRailInset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.res.stringResource
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.melox.player.R
import com.melox.player.ui.navigation.LocalWindowBackDispatcherOwner
import top.yukonga.miuix.kmp.upstream.glass.GlassSearchOverlay
import top.yukonga.miuix.kmp.upstream.glass.GlassSearchOverlayDefaults
import top.yukonga.miuix.kmp.upstream.glass.rememberGlassSearchBarAlpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
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
    if (!settings.customBackgroundEnabled) return null
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
    val landscapeNavigation = com.melox.player.ui.LocalLandscapeNavigation.current
    val pageBackdrop = rememberLayerBackdrop()
    val wallpaperBackdrop = rememberLayerBackdrop()
    val blurSettings = LocalTopBarBlurSettings.current
    val effectsActive = blurSettings.blurEnabled && isRuntimeShaderSupported()
    val topBackdrop = pageBackdrop.takeIf { effectsActive }
    val scrollContent = remember { TopBarScrollContent() }
    val searchPage = remember { GlassSearchPage() }
    val searchExpanded = searchPage.isExpanded
    val searchAlpha = rememberGlassSearchBarAlpha(searchExpanded)
    val query = searchPage.session?.query?.value.orEmpty()
    val renderedQuery = searchPage.session?.resultsQuery?.value.orEmpty()
    val resultsVisible = searchExpanded && query.isNotBlank() && renderedQuery.isNotBlank()
    val resultsReady = showGlassSearchResults(searchExpanded, query, renderedQuery)
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val image = LocalCustomPageBackground.current
    val dimAlpha = LocalCustomBackgroundDimAlpha.current
    val surfaceColor = MiuixTheme.colorScheme.surface
    val fixedWallpaper = LocalFixedPageBackground.current.takeIf { LocalRootPageWallpaperFixed.current }
    val surfaceBackdrop = wallpaperBackdrop.takeIf { image != null && effectsActive }
    val safeInsets = WindowInsets.systemBars.union(WindowInsets.captionBar).union(WindowInsets.displayCutout)
    val statusBarSpace = WindowInsets.statusBarsIgnoringVisibility.only(WindowInsetsSides.Top)
    val searchTopInsets = safeInsets.only(WindowInsetsSides.Top)
    val searchTopPadding = landscapeNavigation?.searchTopPadding ?: { 0.dp }
    val searchStartInset = WindowInsets(
        left = if (layoutDirection == LayoutDirection.Ltr) floatingRailInset else 0.dp,
        right = if (layoutDirection == LayoutDirection.Rtl) floatingRailInset else 0.dp,
    )
    val searchHorizontalInsets = safeInsets.only(WindowInsetsSides.Horizontal).add(searchStartInset)
    Box(modifier = modifier.onGloballyPositioned { scrollContent.bounds = it.boundsInRoot() }) {
        CompositionLocalProvider(LocalPageSurfaceBackdrop provides surfaceBackdrop, LocalGlassSearchPage provides searchPage, LocalTopBarScrollContent provides scrollContent) {
            Scaffold(
                modifier = Modifier.fillMaxSize()
                    .graphicsLayer { alpha = searchAlpha.value }
                    .then(if (searchExpanded) Modifier.clearAndSetSemantics { } else Modifier),
                containerColor = surfaceColor,
                contentWindowInsets = safeInsets.union(statusBarSpace),
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
                val pagePadding = PaddingValues(
                    start = padding.calculateStartPadding(layoutDirection) + floatingRailInset,
                    top = padding.calculateTopPadding(),
                    end = padding.calculateEndPadding(layoutDirection),
                    bottom = padding.calculateBottomPadding(),
                )
                Box(Modifier.fillMaxSize().layerBackdrop(pageBackdrop)) {
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
                    // The result overlay and the page share the screen content. Only one may own
                    // the list while search results are being displayed.
                    if (!resultsVisible) {
                        content(pagePadding)
                    }
                }
            }
            searchPage.session?.let { session ->
                val searchScrollContent = remember { TopBarScrollContent() }
                val backOwner = LocalWindowBackDispatcherOwner.current ?: LocalNavigationEventDispatcherOwner.current
                DisposableEffect(searchPage) {
                    onDispose {
                        searchPage.transitionActive = false
                    }
                }
                val backOwnerLocals = if (backOwner != null) {
                    arrayOf(LocalNavigationEventDispatcherOwner provides backOwner)
                } else emptyArray()
                CompositionLocalProvider(*backOwnerLocals) {
                    GlassSearchOverlay(
                        expanded = session.expanded.value,
                        scrollMaterialThreshold = GlassSearchOverlayDefaults.VerticalPadding * 2,
                        onDismissRequest = session.onDismiss.value,
                        label = session.label.value,
                        horizontalInsetsModifier = Modifier.windowInsetsPadding(searchHorizontalInsets),
                        topInsets = searchTopInsets,
                        searchTopPadding = searchTopPadding,
                        scrollOffset = { -searchScrollContent.visualContentOffset(0f) },
                        blurEnabled = effectsActive,
                        progressiveBlur = blurSettings.progressiveEnabled,
                        searchIcon = glassSearchIcon,
                        closeIcon = glassCloseIcon,
                        query = session.query.value,
                        onQueryChange = session.onQueryChange.value,
                        onFocusedChange = session.onFocusedChange.value,
                        onActiveChange = { searchPage.transitionActive = it },
                        clearContentDescription = stringResource(R.string.search_clear),
                        cancelContentDescription = stringResource(R.string.search_cancel),
                    ) {
                        if (resultsVisible) {
                            Box(
                                Modifier.fillMaxSize()
                                    .windowInsetsPadding(searchHorizontalInsets)
                                    .then(if (resultsReady) Modifier else Modifier.clearAndSetSemantics { }.pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                                            }
                                        }
                                    })
                                    .onGloballyPositioned { searchScrollContent.bounds = it.boundsInRoot() },
                            ) {
                                CompositionLocalProvider(
                                    LocalPageSurfaceBackdrop provides null,
                                    LocalCustomPageBackground provides null,
                                    LocalTopBarScrollContent provides searchScrollContent,
                                    LocalGlassSearchResults provides true,
                                ) {
                                    content(
                                        PaddingValues(
                                            top = with(density) { searchTopInsets.getTop(this).toDp() } +
                                                searchTopPadding() + GlassSearchOverlayDefaults.BarHeight +
                                                GlassSearchOverlayDefaults.VerticalPadding * 2,
                                            bottom = safeInsets.asPaddingValues().calculateBottomPadding(),
                                        ),
                                    )
                                }
                            }
                        }
                    }
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
