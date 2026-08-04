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
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.melox.player.R
import com.melox.player.model.BottomBarStyle
import com.melox.player.ui.component.MiuixBlurredBar
import com.melox.player.ui.component.miuixBarColor
import com.melox.player.ui.component.rememberMiuixBlurBackdrop
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
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

internal data class MiniPlayerChrome(
    val style: BottomBarStyle,
    val backdrop: LayerBackdrop?,
    val blurActive: Boolean,
    val liquidGlassActive: Boolean,
    val isDark: Boolean,
    val floatingHighlight: Highlight? = null,
)

internal const val NORMAL_BAR_STROKE_ALPHA = 0.42f

/** Holds the persistent bottom navigation while pages render inside the shared Pager. */
@Composable
internal fun PlayerScaffold(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    showNavigation: Boolean,
    bottomBarStyle: BottomBarStyle,
    liquidGlassSupported: Boolean,
    isDark: Boolean,
    blurEnabled: Boolean,
    backdropRefreshKey: Any,
    backdropPagingSignal: () -> Float,
    miniPlayer: @Composable (MiniPlayerChrome) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
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
    val windowWidth = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.width.toDp()
    }
    val useNavigationRail = windowWidth >= 600.dp
    val routeBackdropRefresh = remember { Animatable(1f) }
    val currentBackdropPagingSignal by rememberUpdatedState(backdropPagingSignal)
    LaunchedEffect(backdropRefreshKey) {
        routeBackdropRefresh.snapTo(0f)
        routeBackdropRefresh.animateTo(1f, tween(650))
    }
    val backdropRefreshSignal = {
        routeBackdropRefresh.value + currentBackdropPagingSignal()
    }

    if (useNavigationRail) {
        Row(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = showNavigation,
                enter = fadeIn(tween(180)) + expandHorizontally(tween(260)),
                exit = fadeOut(tween(140)) + shrinkHorizontally(tween(220)),
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
            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = Color.Transparent,
                bottomBar = {
                    Column {
                        miniPlayer(
                            MiniPlayerChrome(
                                style = BottomBarStyle.NORMAL,
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
                    content(innerPadding)
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
            effectsSupported = liquidGlassSupported,
            isDark = isDark,
            showNavigation = showNavigation,
            miniPlayer = miniPlayer,
            backdropRefreshSignal = backdropRefreshSignal,
            content = content,
        )
    } else {
        BasePlayerScaffold(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            navigationItems = navigationItems,
            blurEnabled = blurEnabled,
            isDark = isDark,
            showNavigation = showNavigation,
            miniPlayer = miniPlayer,
            backdropRefreshSignal = backdropRefreshSignal,
            content = content,
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

@Composable
private fun BasePlayerScaffold(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    navigationItems: List<NavigationItem>,
    blurEnabled: Boolean,
    isDark: Boolean,
    showNavigation: Boolean,
    miniPlayer: @Composable (MiniPlayerChrome) -> Unit,
    backdropRefreshSignal: () -> Float,
    content: @Composable (PaddingValues) -> Unit,
) {
    val bottomBarBackdrop = rememberMiuixBlurBackdrop(
        enabled = blurEnabled,
    )
    val normalBarColor = bottomBarBackdrop.miuixBarColor()

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Column {
                miniPlayer(
                    MiniPlayerChrome(
                        style = BottomBarStyle.NORMAL,
                        backdrop = bottomBarBackdrop,
                        blurActive = bottomBarBackdrop != null,
                        liquidGlassActive = false,
                        isDark = isDark,
                    ),
                )
                AnimatedVisibility(
                    visible = showNavigation,
                    enter = slideInVertically(
                        animationSpec = tween(280),
                        initialOffsetY = { it },
                    ) + expandVertically(tween(280)) + fadeIn(tween(180)),
                    exit = slideOutVertically(
                        animationSpec = tween(240),
                        targetOffsetY = { it },
                    ) + shrinkVertically(tween(240)) + fadeOut(tween(140)),
                ) {
                    MiuixBlurredBar(
                        backdrop = bottomBarBackdrop,
                        modifier = Modifier.background(normalBarColor),
                    ) {
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
                AnimatedNavigationInset(visible = !showNavigation)
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

@Composable
internal fun AnimatedNavigationInset(
    visible: Boolean,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(tween(240)),
        exit = shrinkVertically(tween(280)),
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        )
    }
}
