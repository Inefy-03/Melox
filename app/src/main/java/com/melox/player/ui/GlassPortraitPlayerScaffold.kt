package com.melox.player.ui

import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.constrainWidth
import com.melox.player.R
import com.melox.player.model.BottomBarStyle
import com.melox.player.ui.component.BottomSearchAction
import com.melox.player.ui.component.GlassTopBarIcons
import com.melox.player.ui.component.Search
import com.melox.player.ui.component.rememberGlassSearchBarAlpha
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.upstream.blur.layerBackdrop
import top.yukonga.miuix.kmp.upstream.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.upstream.glass.GlassIconButton
import top.yukonga.miuix.kmp.upstream.glass.GlassNavigationBar
import top.yukonga.miuix.kmp.upstream.glass.GlassNavigationBarDefaults
import top.yukonga.miuix.kmp.upstream.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.upstream.material.GlassShadows

@Composable
internal fun GlassPortraitPlayerScaffold(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    navigationItems: List<NavigationItem>,
    blurEnabled: Boolean,
    effectsSupported: Boolean,
    isDark: Boolean,
    showNavigation: Boolean,
    playerNavigationOffset: () -> Float,
    miniPlayer: @Composable (MiniPlayerChrome) -> Unit,
    bottomSearchAction: BottomSearchAction?,
    showBottomSearch: Boolean,
    searchExpanded: Boolean,
    searchOpen: Boolean,
    content: @Composable (PaddingValues) -> Unit,
) {
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val blurActive = blurEnabled && effectsSupported
    val searchAlpha by rememberGlassSearchBarAlpha(searchOpen)
    val items = remember(navigationItems) {
        navigationItems.map { GlassNavigationItem(icon = it.icon, label = it.label) }
    }
    val navigationPadding = WindowInsets.navigationBars.asPaddingValues()
    val captionPadding = WindowInsets.captionBar.asPaddingValues()
    val cutoutPadding = WindowInsets.displayCutout.asPaddingValues()
    val bottomPadding = maxOf(
        24.dp,
        navigationPadding.calculateBottomPadding() + 8.dp,
        captionPadding.calculateBottomPadding() + 8.dp,
        cutoutPadding.calculateBottomPadding() + 8.dp,
    )

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            // Keep Melox's 16 dp side margins; use GlassPage's sizing and search action.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = searchAlpha }
                    .then(if (searchExpanded || searchOpen) Modifier.clearAndSetSemantics { }.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    } else Modifier)
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp)
                    .layout { measurable, constraints ->
                        val bottom = bottomPadding.roundToPx().coerceIn(0, constraints.maxHeight)
                        val placeable = measurable.measure(constraints.offset(vertical = -bottom))
                        layout(placeable.width, placeable.height + bottom) {
                            placeable.placeRelative(0, 0)
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                miniPlayer(
                    MiniPlayerChrome(
                        style = BottomBarStyle.GLASS,
                        backdrop = null,
                        blurActive = blurActive,
                        liquidGlassActive = false,
                        isDark = isDark,
                        glassBackdrop = backdrop.takeIf { blurActive },
                    ),
                )
                GlassNavigationBar(
                    items = items,
                    selectedIndex = selectedTab,
                    onSelect = onTabSelected,
                    backdrop = backdrop.takeIf { blurActive },
                    visible = showNavigation,
                    defaultWindowInsetsPadding = false,
                    modifier = Modifier
                        .graphicsLayer { translationY = playerNavigationOffset() }
                        .layout { measurable, constraints ->
                            val actionWidth = (GlassNavigationBarDefaults.Height + GlassNavigationBarDefaults.ActionSpacing).roundToPx()
                            val available = constraints.maxWidth - actionWidth + (10.dp * (items.size - 1) - 6.dp).roundToPx()
                            val width = if (constraints.hasBoundedWidth && available > 400.dp.roundToPx()) {
                                constraints.constrainWidth(344.dp.roundToPx() + actionWidth)
                            } else {
                                constraints.maxWidth
                            }
                            val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                            layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
                        },
                    // Reserve the destination action before its page registers the callback.
                    trailingAction = if (showBottomSearch) {
                        {
                            GlassIconButton(
                                onClick = {
                                    bottomSearchAction?.let { action ->
                                        if (action.enabled.value) action.onClick.value()
                                    }
                                },
                                backdrop = backdrop.takeIf { blurActive },
                                size = GlassNavigationBarDefaults.Height,
                                shadow = GlassShadows.Float,
                            ) {
                                Icon(
                                    imageVector = GlassTopBarIcons.Search,
                                    contentDescription = bottomSearchAction?.label?.value.orEmpty().ifEmpty { stringResource(R.string.search_hint) },
                                    modifier = Modifier.size(28.dp),
                                    tint = MiuixTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    } else null,
                )
            }
        },
    ) { innerPadding ->
        Box(
            Modifier.fillMaxSize()
                .layerBackdrop(backdrop),
        ) {
            content(innerPadding)
        }
    }
}
