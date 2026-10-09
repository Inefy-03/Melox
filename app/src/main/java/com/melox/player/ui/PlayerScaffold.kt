package com.melox.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.melox.player.R
import com.melox.player.model.BottomBarStyle
import com.melox.player.ui.component.GaussianBlurredBar
import com.melox.player.ui.component.miuixBarColor
import com.melox.player.ui.component.playback.miniPlayerHeight
import com.melox.player.ui.component.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.NavigationRailValue
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.DividerDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.basic.rememberNavigationRailState

internal data class MiniPlayerChrome(
    val style: BottomBarStyle,
    val backdrop: LayerBackdrop?,
    val blurActive: Boolean,
    val liquidGlassActive: Boolean,
    val isDark: Boolean,
    val floatingHighlight: Highlight? = null,
    val smallPlayerBar: Boolean = false,
)

internal const val NORMAL_BAR_STROKE_ALPHA = 0.42f
private const val NAVIGATION_ENTER_DURATION_MILLIS = 280
private const val NAVIGATION_EXIT_DURATION_MILLIS = 240
private const val NAVIGATION_FADE_IN_DURATION_MILLIS = 180
private const val NAVIGATION_FADE_OUT_DURATION_MILLIS = 140

/** Holds the persistent bottom navigation while pages render inside the shared Pager. */
@Composable
internal fun PlayerScaffold(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    showNavigation: Boolean,
    playerNavigationOffset: () -> Float,
    showNavigationRailOnSecondary: Boolean = false,
    persistedNavigationRailExpanded: Boolean = true,
    onNavigationRailExpandedChange: (Boolean) -> Unit = {},
    bottomBarStyle: BottomBarStyle,
    smallPlayerBar: Boolean,
    liquidGlassSupported: Boolean,
    isDark: Boolean,
    blurEnabled: Boolean,
    backdropRefreshKey: Any,
    backdropPagingSignal: () -> Float,
    miniPlayer: @Composable (MiniPlayerChrome) -> Unit,
    content: @Composable (PaddingValues, navigationRailExpanded: Boolean) -> Unit,
) {
    val homeLabel = stringResource(R.string.navigation_home)
    val songsLabel = stringResource(R.string.navigation_music)
    val libraryLabel = stringResource(R.string.navigation_library)
    val settingsLabel = stringResource(R.string.navigation_settings)
    val navigationItems = remember(
        homeLabel,
        songsLabel,
        libraryLabel,
        settingsLabel,
    ) {
        listOf(
            NavigationItem(label = homeLabel, icon = MiuixIcons.Home),
            NavigationItem(label = songsLabel, icon = MiuixIcons.Music),
            NavigationItem(label = libraryLabel, icon = MiuixIcons.Album),
            NavigationItem(label = settingsLabel, icon = MiuixIcons.Settings),
        )
    }
    val effectiveStyle = resolveBottomBarStyle(bottomBarStyle, liquidGlassSupported)
    val windowSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val windowWidth = with(density) { windowSize.width.toDp() }
    val windowHeight = with(density) { windowSize.height.toDp() }
    val useNavigationRail = shouldUseNavigationRail(
        windowWidth = windowWidth,
        windowHeight = windowHeight,
        effectiveStyle = effectiveStyle,
    )
    val landscape = isMiuixWideLayout(windowWidth, windowHeight)
    val expandNavigationRail = landscape && windowWidth >= 1200.dp
    val navigationRailState = rememberNavigationRailState(
        initialValue = if (expandNavigationRail && persistedNavigationRailExpanded) {
            NavigationRailValue.Expanded
        } else {
            NavigationRailValue.Collapsed
        },
    )
    val routeBackdropRefresh = remember { Animatable(1f) }
    val currentBackdropPagingSignal by rememberUpdatedState(backdropPagingSignal)
    LaunchedEffect(backdropRefreshKey) {
        routeBackdropRefresh.snapTo(0f)
        routeBackdropRefresh.animateTo(1f, tween(650))
    }
    LaunchedEffect(expandNavigationRail) {
        if (!expandNavigationRail) {
            navigationRailState.collapse()
        } else if (persistedNavigationRailExpanded) {
            navigationRailState.expand()
        }
    }
    LaunchedEffect(
        navigationRailState.currentValue,
        useNavigationRail,
        effectiveStyle,
        windowWidth,
        windowHeight,
    ) {
        if (useNavigationRail && effectiveStyle == BottomBarStyle.NORMAL &&
            landscape
        ) {
            onNavigationRailExpandedChange(navigationRailState.isExpanded)
        }
    }
    val backdropRefreshSignal = {
        routeBackdropRefresh.value + currentBackdropPagingSignal()
    }

    if (useNavigationRail && landscape) {
        val miniPlayerBackdrop = rememberBlurBackdrop()
        val navigationRailVisible = showNavigation || showNavigationRailOnSecondary
        val navigationBarBottomInset = WindowInsets.navigationBars
            .only(WindowInsetsSides.Bottom)
            .asPaddingValues()
            .calculateBottomPadding()
        val miniPlayerBottomPadding = normalMiniPlayerBottomPadding(
            windowWidth, windowHeight, navigationBarBottomInset,
        )
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        @Suppress("UNUSED_VARIABLE")
                        val refreshFrame = backdropRefreshSignal()
                        drawContent()
                    }
                    .then(
                        miniPlayerBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier,
                    ),
            ) {
                AnimatedVisibility(
                    visible = navigationRailVisible,
                    enter = fadeIn(tween(NAVIGATION_FADE_IN_DURATION_MILLIS)) +
                        expandHorizontally(tween(NAVIGATION_ENTER_DURATION_MILLIS)),
                    exit = fadeOut(tween(NAVIGATION_FADE_OUT_DURATION_MILLIS)) +
                        shrinkHorizontally(tween(NAVIGATION_EXIT_DURATION_MILLIS)),
                ) {
                    NavigationRail(
                        state = navigationRailState,
                        expandContentDescription = stringResource(R.string.navigation_expand),
                        collapseContentDescription = stringResource(R.string.navigation_collapse),
                    ) {
                        navigationItems.forEachIndexed { index, item ->
                            NavigationRailItem(
                                selected = selectedTab == index,
                                onClick = { onTabSelected(index) },
                                icon = item.icon,
                                label = item.label,
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clipToBounds()
                        .consumeNavigationRailStartInsets(navigationRailVisible),
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent,
                        bottomBar = {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(
                                        miniPlayerHeight(BottomBarStyle.NORMAL, smallPlayerBar) +
                                            miniPlayerBottomPadding,
                                    ),
                            )
                        },
                    ) { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            content(
                                innerPadding,
                                navigationRailVisible && navigationRailState.isExpanded,
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = miniPlayerBottomPadding),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .offset(y = if (navigationBarBottomInset > 0.dp) {
                            (PLAYER_NORMAL_MINI_PLAYER_INTERNAL_PADDING -
                                navigationBarBottomInset).coerceAtLeast(0.dp)
                        } else {
                            0.dp
                        })
                        .widthIn(
                            max = BottomSheetDefaults.maxWidth +
                                PLAYER_NORMAL_MINI_PLAYER_INTERNAL_PADDING * 2,
                        )
                        .fillMaxWidth(),
                ) {
                    miniPlayer(
                        MiniPlayerChrome(
                            style = BottomBarStyle.NORMAL,
                            smallPlayerBar = smallPlayerBar,
                            backdrop = miniPlayerBackdrop,
                            blurActive = miniPlayerBackdrop != null,
                            liquidGlassActive = false,
                            isDark = isDark,
                        ),
                    )
                }
            }
        }
        return
    }

    if (useNavigationRail) {
        val navigationRailVisible = showNavigation || showNavigationRailOnSecondary
        Row(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = navigationRailVisible,
                enter = fadeIn(tween(NAVIGATION_FADE_IN_DURATION_MILLIS)) +
                    expandHorizontally(tween(NAVIGATION_ENTER_DURATION_MILLIS)),
                exit = fadeOut(tween(NAVIGATION_FADE_OUT_DURATION_MILLIS)) +
                    shrinkHorizontally(tween(NAVIGATION_EXIT_DURATION_MILLIS)),
            ) {
                NavigationRail {
                    navigationItems.forEachIndexed { index, item ->
                        NavigationRailItem(
                            selected = selectedTab == index,
                            onClick = { onTabSelected(index) },
                            icon = item.icon,
                            label = item.label,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clipToBounds()
                    .consumeNavigationRailStartInsets(navigationRailVisible),
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    bottomBar = {
                        Column {
                            miniPlayer(
                                MiniPlayerChrome(
                                    style = BottomBarStyle.NORMAL,
                                    smallPlayerBar = smallPlayerBar,
                                    backdrop = null,
                                    blurActive = false,
                                    liquidGlassActive = false,
                                    isDark = isDark,
                                ),
                            )
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding(),
                            )
                        }
                    },
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        content(innerPadding, false)
                    }
                }
            }
        }
        return
    }

    if (effectiveStyle != BottomBarStyle.NORMAL) {
        IosLikeFloatingPlayerScaffold(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            navigationItems = navigationItems,
            blurEnabled = blurEnabled,
            liquidGlassEnabled = bottomBarStyle == BottomBarStyle.LIQUID_GLASS,
            smallPlayerBar = smallPlayerBar,
            effectsSupported = liquidGlassSupported,
            isDark = isDark,
            showNavigation = showNavigation,
            playerNavigationOffset = playerNavigationOffset,
            miniPlayer = miniPlayer,
            backdropRefreshSignal = backdropRefreshSignal,
            content = { padding -> content(padding, false) },
        )
    } else {
        BasePlayerScaffold(
            smallPlayerBar = smallPlayerBar,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            navigationItems = navigationItems,
            blurEnabled = blurEnabled,
            isDark = isDark,
            showNavigation = showNavigation,
            playerNavigationOffset = playerNavigationOffset,
            miniPlayer = miniPlayer,
            backdropRefreshSignal = backdropRefreshSignal,
            content = { padding -> content(padding, false) },
        )
    }
}

internal fun resolveBottomBarStyle(
    bottomBarStyle: BottomBarStyle,
    liquidGlassSupported: Boolean,
): BottomBarStyle = if (
    // Older devices cannot create the RuntimeShader-based glass effect.
    bottomBarStyle == BottomBarStyle.LIQUID_GLASS && !liquidGlassSupported
) {
    BottomBarStyle.FLOATING
} else {
    bottomBarStyle
}

internal fun shouldUseNavigationRail(
    windowWidth: Dp,
    windowHeight: Dp,
    effectiveStyle: BottomBarStyle,
): Boolean = isMiuixWideLayout(windowWidth, windowHeight) &&
    effectiveStyle == BottomBarStyle.NORMAL

internal fun isMiuixWideLayout(
    windowWidth: Dp,
    windowHeight: Dp,
): Boolean {
    if (windowWidth <= 0.dp) return false
    val heightToWidthRatio = windowHeight.value / windowWidth.value
    return windowWidth >= 840.dp ||
        (windowWidth >= 600.dp && heightToWidthRatio < 1.2f)
}

@Composable
internal fun usesMiuixSmallTopAppBar(): Boolean {
    val windowSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    return isMiuixWideLayout(
        windowWidth = with(density) { windowSize.width.toDp() },
        windowHeight = with(density) { windowSize.height.toDp() },
    )
}

internal fun usesNormalMiniPlayerChrome(
    renderedBottomBarStyle: BottomBarStyle,
    liquidGlassSupported: Boolean,
): Boolean {
    val resolvedStyle = resolveBottomBarStyle(renderedBottomBarStyle, liquidGlassSupported)
    return resolvedStyle == BottomBarStyle.NORMAL
}

internal fun shouldShowNavigation(
    currentRouteIsRoot: Boolean,
    hideBottomBar: Boolean,
    landscape: Boolean,
    requestedBottomBarStyle: BottomBarStyle,
    renderedBottomBarStyle: BottomBarStyle = requestedBottomBarStyle,
): Boolean = currentRouteIsRoot && !hideBottomBar &&
    (!landscape || requestedBottomBarStyle == BottomBarStyle.NORMAL ||
        renderedBottomBarStyle != BottomBarStyle.NORMAL)

private val PLAYER_NORMAL_MINI_PLAYER_INTERNAL_PADDING = 6.dp

internal fun normalMiniPlayerBottomPadding(
    windowWidth: Dp,
    windowHeight: Dp,
    navigationBarBottomInset: Dp,
): Dp {
    val wideLayout = isMiuixWideLayout(windowWidth, windowHeight)
    val visibleBottomPadding = if (navigationBarBottomInset > 0.dp) {
        navigationBarBottomInset + if (wideLayout) 0.dp else 6.dp
    } else if (wideLayout) {
        16.dp
    } else {
        24.dp
    }
    return (visibleBottomPadding - PLAYER_NORMAL_MINI_PLAYER_INTERNAL_PADDING)
        .coerceAtLeast(0.dp)
}

@Composable
private fun Modifier.consumeNavigationRailStartInsets(
    navigationRailVisible: Boolean,
): Modifier = if (navigationRailVisible) {
    consumeWindowInsets(
        WindowInsets.displayCutout.only(WindowInsetsSides.Start)
            .union(WindowInsets.navigationBars.only(WindowInsetsSides.Start)),
    )
} else {
    this
}

@Composable
private fun BasePlayerScaffold(
    smallPlayerBar: Boolean,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    navigationItems: List<NavigationItem>,
    blurEnabled: Boolean,
    isDark: Boolean,
    showNavigation: Boolean,
    playerNavigationOffset: () -> Float,
    miniPlayer: @Composable (MiniPlayerChrome) -> Unit,
    backdropRefreshSignal: () -> Float,
    content: @Composable (PaddingValues) -> Unit,
) {
    val bottomBarBackdrop = rememberBlurBackdrop()
    val normalBarColor = bottomBarBackdrop.miuixBarColor()
    val density = LocalDensity.current
    val windowSize = LocalWindowInfo.current.containerSize
    val windowWidth = with(density) { windowSize.width.toDp() }
    val windowHeight = with(density) { windowSize.height.toDp() }
    val navigationBarBottomInset = WindowInsets.navigationBars
        .only(WindowInsetsSides.Bottom)
        .asPaddingValues()
        .calculateBottomPadding()
    val miniPlayerNavigationGap by animateDpAsState(
        targetValue = if (showNavigation) 0.dp else normalMiniPlayerBottomPadding(
            windowWidth, windowHeight, navigationBarBottomInset,
        ),
        animationSpec = tween(
            if (showNavigation) NAVIGATION_ENTER_DURATION_MILLIS else NAVIGATION_EXIT_DURATION_MILLIS,
        ),
        label = "normalMiniPlayerNavigationGap",
    )

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Column {
                Box(modifier = Modifier.padding(bottom = miniPlayerNavigationGap)) {
                    miniPlayer(
                        MiniPlayerChrome(
                            style = BottomBarStyle.NORMAL,
                            smallPlayerBar = smallPlayerBar,
                            backdrop = bottomBarBackdrop,
                            blurActive = bottomBarBackdrop != null,
                            liquidGlassActive = false,
                            isDark = isDark,
                        ),
                    )
                }
                AnimatedVisibility(
                    modifier = Modifier.graphicsLayer { translationY = playerNavigationOffset() },
                    visible = showNavigation,
                    enter = slideInVertically(
                        animationSpec = tween(NAVIGATION_ENTER_DURATION_MILLIS),
                        initialOffsetY = { it },
                    ) + expandVertically(tween(NAVIGATION_ENTER_DURATION_MILLIS)) +
                        fadeIn(tween(NAVIGATION_FADE_IN_DURATION_MILLIS)),
                    exit = slideOutVertically(
                        animationSpec = tween(NAVIGATION_EXIT_DURATION_MILLIS),
                        targetOffsetY = { it },
                    ) + shrinkVertically(tween(NAVIGATION_EXIT_DURATION_MILLIS)) +
                        fadeOut(tween(NAVIGATION_FADE_OUT_DURATION_MILLIS)),
                ) {
                    Box(modifier = Modifier.background(normalBarColor)) {
                        GaussianBlurredBar(backdrop = bottomBarBackdrop) {
                            Column {
                                HorizontalDivider(
                                    thickness = DividerDefaults.Thickness,
                                    color = DividerDefaults.DividerColor.copy(
                                        alpha = NORMAL_BAR_STROKE_ALPHA,
                                    ),
                                )
                                NavigationBar(
                                    color = normalBarColor,
                                    showDivider = false,
                                ) {
                                    navigationItems.forEachIndexed { index, item ->
                                        NavigationBarItem(
                                            selected = selectedTab == index,
                                            onClick = { onTabSelected(index) },
                                            icon = item.icon,
                                            label = item.label,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    @Suppress("UNUSED_VARIABLE")
                    val refreshFrame = backdropRefreshSignal()
                    drawContent()
                }
                .then(
                    bottomBarBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier,
                ),
        ) {
            content(innerPadding)
        }
    }
}
