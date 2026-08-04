package com.melox.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.melox.player.ui.component.liquid.LiquidGlassNavigationBar
import com.melox.player.ui.component.liquid.rememberFloatingBarHighlight
import com.melox.player.ui.component.liquid.rememberIosLikeNavigationBarWidth
import com.melox.player.model.BottomBarStyle
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun IosLikeFloatingPlayerScaffold(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    navigationItems: List<NavigationItem>,
    blurEnabled: Boolean,
    liquidGlassEnabled: Boolean,
    effectsSupported: Boolean,
    isDark: Boolean,
    showNavigation: Boolean,
    miniPlayer: @Composable (MiniPlayerChrome) -> Unit,
    backdropRefreshSignal: () -> Float,
    content: @Composable (PaddingValues) -> Unit,
) {
    val surfaceColor = MiuixTheme.colorScheme.surface
    val effectsActive = effectsSupported && (blurEnabled || liquidGlassEnabled)
    val floatingHighlight = rememberFloatingBarHighlight(active = effectsActive)
    val backdrop = if (effectsActive) {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    } else {
        null
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                val barWidth = rememberIosLikeNavigationBarWidth(
                    items = navigationItems,
                    maxWidth = maxWidth,
                )
                Column(modifier = Modifier.width(barWidth)) {
                    miniPlayer(
                        MiniPlayerChrome(
                            style = if (liquidGlassEnabled) {
                                BottomBarStyle.LIQUID_GLASS
                            } else {
                                BottomBarStyle.FLOATING
                            },
                            backdrop = backdrop,
                            blurActive = blurEnabled && effectsSupported,
                            liquidGlassActive = liquidGlassEnabled && effectsSupported,
                            isDark = isDark,
                            floatingHighlight = floatingHighlight,
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
                        LiquidGlassNavigationBar(
                            items = navigationItems,
                            selectedIndex = selectedTab,
                            onItemClick = onTabSelected,
                            backdrop = backdrop,
                            isBlurActive = blurEnabled && effectsSupported,
                            isLiquidGlassActive = liquidGlassEnabled && effectsSupported,
                            isDark = isDark,
                            containerHighlight = floatingHighlight,
                        )
                    }
                    AnimatedNavigationInset(visible = !showNavigation)
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
                    backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier,
                ),
        ) {
            content(innerPadding)
        }
    }
}
