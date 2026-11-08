package com.melox.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.melox.player.model.BottomBarStyle
import com.melox.player.ui.component.FixedPageBackgroundHost
import com.melox.player.ui.component.rememberBlurBackdrop
import com.melox.player.ui.component.liquid.LiquidGlassNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import androidx.compose.ui.graphics.Color

// The 64 dp capsule starts 12 dp beyond the system safe drawing region; list padding supplies the trailing gap.
internal val FloatingNavigationRailInset = 76.dp

internal val LocalFloatingNavigationRailInset = staticCompositionLocalOf { 0.dp }

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LandscapeFloatingPlayerScaffold(
    navigationItems: List<NavigationItem>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    showNavigation: Boolean,
    smallPlayerBar: Boolean,
    blurEnabled: Boolean,
    liquidGlassSupported: Boolean,
    isDark: Boolean,
    miniPlayer: @Composable (MiniPlayerChrome) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val backdrop = rememberBlurBackdrop()
    FixedPageBackgroundHost(Modifier.fillMaxSize(), refreshSignal = { 1f }) {
        CompositionLocalProvider(LocalFloatingNavigationRailInset provides if (showNavigation) FloatingNavigationRailInset else 0.dp) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
            Scaffold(
                modifier = Modifier.fillMaxSize(), containerColor = Color.Transparent,
                bottomBar = {
                    Box(Modifier.fillMaxWidth().padding(start = if (showNavigation) FloatingNavigationRailInset else 0.dp).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                            miniPlayer(MiniPlayerChrome(
                                style = BottomBarStyle.LIQUID_GLASS, backdrop = backdrop,
                                blurActive = blurEnabled && liquidGlassSupported,
                                liquidGlassActive = blurEnabled && liquidGlassSupported,
                                isDark = isDark, smallPlayerBar = smallPlayerBar,
                            ))
                        }
                    }
                },
            ) { padding ->
                Box(Modifier.fillMaxSize().then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)) { content(padding) }
            }
            AnimatedVisibility(
                modifier = Modifier.align(Alignment.CenterStart),
                visible = showNavigation,
                enter = fadeIn() + expandHorizontally(clip = false),
                exit = fadeOut() + shrinkHorizontally(clip = false),
            ) {
                Box(Modifier.width(FloatingNavigationRailInset).fillMaxHeight().windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility.union(WindowInsets.navigationBarsIgnoringVisibility).only(WindowInsetsSides.Vertical)), contentAlignment = Alignment.CenterEnd) {
                    LiquidGlassNavigationBar(
                        items = navigationItems, selectedIndex = selectedTab, onItemClick = onTabSelected,
                        backdrop = backdrop, isBlurActive = blurEnabled && liquidGlassSupported,
                        isLiquidGlassActive = blurEnabled && liquidGlassSupported, isDark = isDark,
                        vertical = true, bottomPadding = 0.dp,
                    )
                }
            }
        }
        }
    }
}
