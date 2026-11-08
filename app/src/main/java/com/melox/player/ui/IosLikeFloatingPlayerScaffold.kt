package com.melox.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
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
import com.melox.player.ui.component.os4.BottomSearchAction
import com.melox.player.ui.component.os4.glassSearchIcon
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.basic.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.melox.player.ui.component.os4.rememberGlassSearchBarAlpha
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.glass.popupShadowOutsets
import top.yukonga.miuix.kmp.glass.GlassShadows
import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.glass.GlassNavigationBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import top.yukonga.miuix.kmp.glass.GlassNavigationBar
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.nav.transition.NavSettleSpec

@Composable
internal fun IosLikeFloatingPlayerScaffold(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    navigationItems: List<NavigationItem>,
    blurEnabled: Boolean,
    liquidGlassEnabled: Boolean,
    bottomBarStyle: BottomBarStyle,
    smallPlayerBar: Boolean,
    effectsSupported: Boolean,
    isDark: Boolean,
    showNavigation: Boolean,
    navigationHideMotion: NavSettleSpec.Tween?,
    playerNavigationOffset: () -> Float,
    miniPlayer: @Composable (MiniPlayerChrome) -> Unit,
    bottomSearchAction: BottomSearchAction? = null,
    showBottomSearch: Boolean = bottomSearchAction != null,
    searchExpanded: Boolean = false,
    searchOpen: Boolean = false,
    content: @Composable (PaddingValues) -> Unit,
) {
    val surfaceColor = MiuixTheme.colorScheme.surface
    val searchChromeAlpha = rememberGlassSearchBarAlpha(searchOpen)
    val density = LocalDensity.current
    val chromeShadowOutsets = remember(density) { with(density) { popupShadowOutsets(GlassShadows.Regular) } }
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
        animationSpec = navigationBarAnimationSpec(showNavigation, navigationHideMotion),
        label = "floatingMiniPlayerNavigationGap",
    )
    val miniPlayerNavigationShadowOffset by animateDpAsState(
        targetValue = if (showNavigation) FLOATING_NAVIGATION_BAR_SHADOW_PADDING else {
            (FLOATING_MINI_PLAYER_BOTTOM_PADDING - navigationBottomPadding).coerceAtLeast(0.dp)
        },
        animationSpec = navigationBarAnimationSpec(showNavigation, navigationHideMotion),
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
            // Match the Glass example: preserve chrome and its layout through search mode.
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        outsets = chromeShadowOutsets
                        alpha = searchChromeAlpha.value
                    }
                    .then(if (searchExpanded || searchOpen) Modifier.clearAndSetSemantics { }.pointerInput(Unit) {
                        awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } }
                    } else Modifier)
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
                val barWidth = if (bottomBarStyle == BottomBarStyle.OS4) {
                    availableBarWidth
                } else {
                    rememberIosLikeNavigationBarWidth(items = navigationItems, maxWidth = availableBarWidth)
                }
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
                                style = bottomBarStyle,
                                backdrop = backdrop,
                                blurActive = blurEnabled && effectsSupported,
                                liquidGlassActive = liquidGlassEnabled && effectsSupported,
                                isDark = isDark,
                                floatingHighlight = floatingHighlight,
                                smallPlayerBar = smallPlayerBar && bottomBarStyle.supportsSmallPlayerBar,
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
                            animationSpec = navigationBarAnimationSpec(false, navigationHideMotion),
                            targetOffsetY = { it },
                        ) + shrinkVertically(
                            animationSpec = navigationBarAnimationSpec(false, navigationHideMotion),
                            clip = false,
                        ) + if (navigationHideMotion == null) {
                            fadeOut(tween(140))
                        } else {
                            ExitTransition.None
                        },
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
                            val barBottomPadding = (
                                navigationBottomPadding - FLOATING_NAVIGATION_BAR_SHADOW_PADDING
                            ).coerceAtLeast(0.dp)
                            when (bottomBarStyle) {
                                BottomBarStyle.OS4 -> GlassNavigationBar(
                                    items = navigationItems.map {
                                        GlassNavigationItem(icon = it.icon, label = it.label)
                                    },
                                    selectedIndex = selectedTab,
                                    onSelect = onTabSelected,
                                    backdrop = backdrop.takeIf { blurEnabled && effectsSupported },
                                    defaultWindowInsetsPadding = false,
                                    trailingAction = if (showBottomSearch) {
                                        {
                                            GlassIconButton(
                                                onClick = { bottomSearchAction?.onClick?.value?.invoke() },
                                                enabled = bottomSearchAction?.enabled?.value == true,
                                                size = GlassNavigationBarDefaults.Height,
                                                backdrop = backdrop.takeIf { blurEnabled && effectsSupported },
                                            ) {
                                                Icon(glassSearchIcon, bottomSearchAction?.label?.value ?: androidx.compose.ui.res.stringResource(com.melox.player.R.string.search_hint), Modifier.size(24.dp))
                                            }
                                        }
                                    } else null,
                                    modifier = Modifier.padding(bottom = barBottomPadding),
                                )
                                else -> LiquidGlassNavigationBar(
                                    items = navigationItems,
                                    selectedIndex = selectedTab,
                                    onItemClick = onTabSelected,
                                    backdrop = backdrop,
                                    isBlurActive = blurEnabled && effectsSupported,
                                    isLiquidGlassActive = liquidGlassEnabled && effectsSupported,
                                    isDark = isDark,
                                    containerHighlight = floatingHighlight,
                                    bottomPadding = barBottomPadding,
                                )
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
    navigationBarBottomInset + 8.dp
} else {
    16.dp
}

private val FLOATING_NAVIGATION_BAR_SHADOW_PADDING = 10.dp
