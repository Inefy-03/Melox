package com.melox.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.melox.player.ui.component.liquid.LiquidGlassNavigationBar
import com.melox.player.ui.component.liquid.floatingNavigationBarBottomPadding
import com.melox.player.ui.component.liquid.rememberFloatingBarHighlight
import com.melox.player.ui.component.liquid.rememberIosLikeNavigationBarWidth
import com.melox.player.ui.component.playback.FLOATING_MINI_PLAYER_BOTTOM_PADDING
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
    smallPlayerBar: Boolean,
    effectsSupported: Boolean,
    isDark: Boolean,
    showNavigation: Boolean,
    playerNavigationOffset: () -> Float,
    miniPlayer: @Composable (MiniPlayerChrome) -> Unit,
    backdropRefreshSignal: () -> Float,
    content: @Composable (PaddingValues) -> Unit,
) {
    val surfaceColor = MiuixTheme.colorScheme.surface
    val density = LocalDensity.current
    val windowSize = LocalWindowInfo.current.containerSize
    val windowWidth = with(density) { windowSize.width.toDp() }
    val windowHeight = with(density) { windowSize.height.toDp() }
    val navigationBarBottomInset = WindowInsets.navigationBars
        .only(WindowInsetsSides.Bottom)
        .asPaddingValues()
        .calculateBottomPadding()
    val portraitReferenceWidth = with(density) {
        minOf(windowSize.width, windowSize.height).toDp()
    }
    val ordinaryPortrait = windowHeight >= windowWidth &&
        !isMiuixWideLayout(windowWidth, windowHeight)
    val navigationBottomPadding = if (ordinaryPortrait) {
        floatingNavigationBarBottomPadding(navigationBarBottomInset)
    } else {
        floatingBottomBarBottomPadding(navigationBarBottomInset)
    }
    val hiddenNavigationBottomPadding = floatingMiniPlayerBottomPaddingWhenNavigationIsHidden(
        navigationBarBottomInset = navigationBarBottomInset,
        isPortrait = ordinaryPortrait,
    )
    val miniPlayerNavigationGap by animateDpAsState(
        targetValue = if (showNavigation) 0.dp else {
            hiddenNavigationBottomPadding
        },
        animationSpec = tween(if (showNavigation) 280 else 240),
        label = "floatingMiniPlayerNavigationGap",
    )
    val miniPlayerNavigationShadowOffset by animateDpAsState(
        targetValue = if (showNavigation) FLOATING_NAVIGATION_BAR_SHADOW_PADDING else {
            (FLOATING_MINI_PLAYER_BOTTOM_PADDING - navigationBottomPadding).coerceAtLeast(0.dp)
        },
        animationSpec = tween(if (showNavigation) 280 else 240),
        label = "floatingMiniPlayerNavigationShadowOffset",
    )
    val effectsActive = effectsSupported && (blurEnabled || liquidGlassEnabled)
    val floatingHighlight = rememberFloatingBarHighlight(
        active = effectsSupported && liquidGlassEnabled,
    )
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
                    .padding(
                        horizontal = (if (ordinaryPortrait) 16.dp else 24.dp) -
                            FLOATING_NAVIGATION_BAR_SHADOW_PADDING,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val availableBarWidth = floatingBottomBarAvailableWidth(
                    windowWidth = windowWidth,
                    windowHeight = windowHeight,
                    contentMaxWidth = maxWidth,
                    portraitReferenceWidth = portraitReferenceWidth,
                )
                val barWidth = rememberIosLikeNavigationBarWidth(
                    items = navigationItems,
                    maxWidth = availableBarWidth,
                )
                Column(
                    modifier = Modifier.width(
                        barWidth + FLOATING_NAVIGATION_BAR_SHADOW_PADDING * 2,
                    ),
                ) {
                    Box(
                        modifier = Modifier
                            .padding(
                                start = FLOATING_NAVIGATION_BAR_SHADOW_PADDING,
                                end = FLOATING_NAVIGATION_BAR_SHADOW_PADDING,
                                bottom = miniPlayerNavigationGap,
                            )
                            .offset(y = miniPlayerNavigationShadowOffset),
                    ) {
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
                                smallPlayerBar = smallPlayerBar,
                            ),
                        )
                    }
                    AnimatedVisibility(
                        modifier = Modifier.graphicsLayer { translationY = playerNavigationOffset() },
                        visible = showNavigation,
                        enter = slideInVertically(
                            animationSpec = tween(280),
                            initialOffsetY = { it },
                        ) + expandVertically(
                            animationSpec = tween(280),
                            clip = false,
                        ) + fadeIn(tween(180)),
                        exit = slideOutVertically(
                            animationSpec = tween(240),
                            targetOffsetY = { it },
                        ) + shrinkVertically(
                            animationSpec = tween(240),
                            clip = false,
                        ) + fadeOut(tween(140)),
                    ) {
                        Box(
                            modifier = Modifier.padding(
                                start = FLOATING_NAVIGATION_BAR_SHADOW_PADDING,
                                top = FLOATING_NAVIGATION_BAR_SHADOW_PADDING,
                                end = FLOATING_NAVIGATION_BAR_SHADOW_PADDING,
                                bottom = minOf(
                                    FLOATING_NAVIGATION_BAR_SHADOW_PADDING,
                                    navigationBottomPadding,
                                ),
                            ),
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
                                bottomPadding = (
                                    navigationBottomPadding - FLOATING_NAVIGATION_BAR_SHADOW_PADDING
                                ).coerceAtLeast(0.dp),
                            )
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
                    backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier,
                ),
        ) {
            content(innerPadding)
        }
    }
}

internal fun floatingMiniPlayerBottomPaddingWhenNavigationIsHidden(
    navigationBarBottomInset: Dp,
    isPortrait: Boolean,
): Dp = ((if (isPortrait) {
    floatingNavigationBarBottomPadding(navigationBarBottomInset)
} else {
    floatingBottomBarBottomPadding(navigationBarBottomInset)
}) - FLOATING_MINI_PLAYER_BOTTOM_PADDING).coerceAtLeast(0.dp)

internal fun floatingBottomBarAvailableWidth(
    windowWidth: Dp,
    windowHeight: Dp,
    contentMaxWidth: Dp,
    portraitReferenceWidth: Dp,
): Dp = if (windowWidth > windowHeight || isMiuixWideLayout(windowWidth, windowHeight)) {
    contentMaxWidth
} else {
    (portraitReferenceWidth - 32.dp).coerceAtLeast(0.dp)
}

internal fun floatingBottomBarBottomPadding(
    navigationBarBottomInset: Dp,
): Dp = if (navigationBarBottomInset > 0.dp) {
    navigationBarBottomInset
} else {
    16.dp
}

private val FLOATING_NAVIGATION_BAR_SHADOW_PADDING = 10.dp
