package com.melox.player.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.upstream.glass.glassSearchBarTransition
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.glass.*

@Composable
internal fun TopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.surface,
    titleColor: Color = MiuixTheme.colorScheme.onSurface,
    subtitle: String = "",
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: ScrollBehavior? = null,
    defaultWindowInsetsPadding: Boolean = true,
    bottomContent: @Composable () -> Unit = {},
    glassTitleAlpha: (() -> Float)? = null,
    expandable: Boolean = true,
) {
    val backdrop = LocalGlassTopBackdrop.current
    if (com.melox.player.ui.usesMiuixSmallTopAppBar()) {
        SmallTopAppBar(
            title = title,
            modifier = modifier,
            color = color,
            titleColor = titleColor,
            subtitle = subtitle,
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior,
            defaultWindowInsetsPadding = defaultWindowInsetsPadding,
            bottomContent = bottomContent,
            glassTitleAlpha = glassTitleAlpha,
        )
    } else {
        val searchExpanded = LocalGlassSearchPage.current?.isExpanded == true
        GlassTopAppBar(
            title = title, modifier = modifier, subtitle = subtitle,
            compact = com.melox.player.ui.usesMiuixSmallTopAppBar(),
            expandable = expandable,
            contentModifier = Modifier
                .glassSearchBarTransition(searchExpanded, LocalTopBarBlurSettings.current.blurEnabled) { 1f }
                .stableTopAppBarContentInsets(),
            backdrop = backdrop, scrollBehavior = scrollBehavior,
            isContentScrolled = topBarContentScrolled(scrollBehavior),
            visualContentOffset = rememberTopBarVisualContentOffset(scrollBehavior),
            progressiveBlur = LocalTopBarBlurSettings.current.progressiveEnabled && LocalTopBarBlurSettings.current.blurEnabled,
            defaultWindowInsetsPadding = defaultWindowInsetsPadding,
            titleAlpha = { (glassTitleAlpha?.invoke() ?: titleColor.alpha) },
            navigationIcon = { TopBarButtonHaptics { navigationIcon() } },
            actions = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { TopBarButtonHaptics { actions() } }
            }, bottomContent = bottomContent,
        )
    }
}

@Composable
internal fun expandedTopBarBottomContentGap(
    scrollBehavior: ScrollBehavior,
    expandedGap: Dp,
): Dp = expandedGap * (1f - scrollBehavior.state.collapsedFraction.coerceIn(0f, 1f))
