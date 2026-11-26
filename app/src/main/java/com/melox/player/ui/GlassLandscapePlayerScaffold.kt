package com.melox.player.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.melox.player.R
import com.melox.player.model.BottomBarStyle
import com.melox.player.ui.component.FixedPageBackgroundHost
import com.melox.player.ui.component.glassSidebarIcon
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.upstream.blur.layerBackdrop
import top.yukonga.miuix.kmp.upstream.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.upstream.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.upstream.glass.GlassNavigationRail
import top.yukonga.miuix.kmp.upstream.glass.GlassNavigationRailDefaults
import top.yukonga.miuix.kmp.upstream.glass.rememberGlassNavigationRailState
import top.yukonga.miuix.kmp.upstream.material.isRuntimeShaderSupported

internal class LandscapeNavigation(
    val labels: List<String>,
    val overlayInsets: WindowInsets,
    val titleAlpha: () -> Float,
    val searchTopPadding: () -> Dp,
    val centerObstructionStart: () -> Dp,
)

internal val LocalLandscapeNavigation = staticCompositionLocalOf<LandscapeNavigation?> { null }
internal val LocalSecondaryTopBar = staticCompositionLocalOf { false }

internal fun landscapeRailInsets(start: () -> Dp): WindowInsets = object : WindowInsets {
    override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int =
        if (layoutDirection == LayoutDirection.Ltr) with(density) { start().roundToPx() } else 0

    override fun getRight(density: Density, layoutDirection: LayoutDirection): Int =
        if (layoutDirection == LayoutDirection.Rtl) with(density) { start().roundToPx() } else 0

    override fun getTop(density: Density): Int = 0

    override fun getBottom(density: Density): Int = 0
}

@Composable
internal fun GlassLandscapePlayerScaffold(
    navigationItems: List<NavigationItem>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    showNavigation: Boolean,
    blurEnabled: Boolean,
    isDark: Boolean,
    miniPlayer: @Composable (MiniPlayerChrome) -> Unit,
    refreshSignal: () -> Float,
    shortcutItems: List<NavigationItem>,
    selectedShortcut: Int?,
    onShortcutSelected: (Int) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val railState = rememberGlassNavigationRailState()
    val showRail = showNavigation
    val navigation = remember(navigationItems, showRail, railState) {
        val railInset = {
            if (showRail) {
                (GlassNavigationRailDefaults.Margin + GlassNavigationRailDefaults.ExpandedWidth) * railState.progress
            } else {
                0.dp
            }
        }
        LandscapeNavigation(
            labels = navigationItems.map { it.label },
            overlayInsets = landscapeRailInsets(railInset),
            titleAlpha = { if (showRail) railState.progress else 1f },
            centerObstructionStart = {
                if (showRail && !railState.expanded && railState.collapsedWidth > 0.dp) {
                    (railState.collapsedStart - railInset()).coerceAtLeast(0.dp)
                } else {
                    Dp.Unspecified
                }
            },
            searchTopPadding = {
                if (showRail) {
                    (GlassNavigationRailDefaults.CollapsedTop +
                        GlassNavigationRailDefaults.CollapsedHeight + 4.dp) * (1f - railState.progress)
                } else {
                    0.dp
                }
            },
        )
    }
    val blurActive = blurEnabled && isRuntimeShaderSupported()
    val surfaceColor = MiuixTheme.colorScheme.surface
    val glassBackdrop = if (blurActive) rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    } else null
    val primaryItems = remember(navigationItems) {
        navigationItems.map { GlassNavigationItem(it.icon, it.label) }
    }
    val secondaryItems = remember(shortcutItems) {
        shortcutItems.map { GlassNavigationItem(glassSidebarIcon(it.icon), it.label) }
    }
    val secondaryOffset = primaryItems.size
    val railSelection = selectedShortcut?.let { secondaryOffset + it } ?: selectedTab

    FixedPageBackgroundHost(Modifier.fillMaxSize(), refreshSignal = refreshSignal) {
        CompositionLocalProvider(LocalLandscapeNavigation provides navigation) {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    Box(
                        Modifier.fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                            .padding(navigation.overlayInsets.asPaddingValues())
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                            miniPlayer(
                                MiniPlayerChrome(
                                    style = BottomBarStyle.GLASS,
                                    backdrop = null,
                                    blurActive = blurActive,
                                    liquidGlassActive = false,
                                    isDark = isDark,
                                    useIosLikeLayout = true,
                                    glassBackdrop = glassBackdrop,
                                ),
                            )
                        }
                    }
                },
            ) { padding ->
                Box(Modifier.fillMaxSize().then(
                    glassBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier,
                )) { content(padding) }
            }
            if (showRail) {
                GlassNavigationRail(
                    items = primaryItems,
                    secondaryItems = secondaryItems,
                    selectedIndex = railSelection,
                    secondaryParentIndex = selectedTab,
                    onSelect = { index ->
                        if (index < secondaryOffset) {
                            onTabSelected(index)
                        } else {
                            onShortcutSelected(index - secondaryOffset)
                        }
                    },
                    backdrop = glassBackdrop,
                    state = railState,
                    expandContentDescription = stringResource(R.string.navigation_expand),
                    collapseContentDescription = stringResource(R.string.navigation_collapse),
                )
            }
        }
    }
}
