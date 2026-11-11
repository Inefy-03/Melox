package com.melox.player.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import com.melox.player.ui.component.rememberTopBarOverlapDistance
import androidx.compose.runtime.Composable
import com.melox.player.ui.component.os4.glassSearchBarTransition
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.melox.player.ui.usesMiuixSmallTopAppBar
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import com.melox.player.ui.component.os4.TopBarButtonHaptics
import com.melox.player.ui.component.os4.SmallTopAppBar
import com.melox.player.ui.component.os4.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun AdaptiveTopAppBar(
    title: String,
    scrollBehavior: ScrollBehavior,
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.surface,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
) {
    val barModifier = modifier.stableTopAppBarInsets(color)
    if (com.melox.player.ui.theme.LocalOs4Style.current && usesMiuixSmallTopAppBar()) {
        SmallTopAppBar(
            title = title,
            modifier = barModifier,
            color = color,
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior,
            bottomContent = bottomContent,
        )
    } else if (com.melox.player.ui.theme.LocalOs4Style.current) {
        val searchExpanded = com.melox.player.ui.component.os4.LocalGlassSearchPage.current?.isExpanded == true &&
            com.melox.player.ui.LocalOs4LandscapeNavigation.current == null
        val searchAlpha = com.melox.player.ui.component.os4.rememberGlassSearchBarAlpha(searchExpanded)
        val searchTitleAlpha = com.melox.player.ui.component.os4.rememberGlassSearchTitleAlpha(
            searchExpanded,
            scrollBehavior.state.heightOffset == scrollBehavior.state.heightOffsetLimit,
        )
        top.yukonga.miuix.kmp.glass.GlassTopAppBar(
            title = title, modifier = barModifier,
            compact = usesMiuixSmallTopAppBar(),
            contentModifier = Modifier.stableTopAppBarContentInsets()
                .glassSearchBarTransition(searchExpanded) { searchAlpha.value },
            backgroundAlpha = { searchAlpha.value },
            titleAlpha = searchTitleAlpha,
            scrollBehavior = scrollBehavior,
            backdrop = LocalGlassTopBackdrop.current,
            overlapDistance = rememberTopBarOverlapDistance(scrollBehavior),
            progressiveBlur = LocalTopBarBlurSettings.current.progressiveEnabled && LocalTopBarBlurSettings.current.blurEnabled,
            navigationIcon = { TopBarButtonHaptics { navigationIcon() } },
            actions = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { TopBarButtonHaptics { actions() } }
            }, bottomContent = bottomContent,
        )
    } else if (usesMiuixSmallTopAppBar()) {
        SmallTopAppBar(
            title = title,
            modifier = barModifier,
            color = color,
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior,
            bottomContent = bottomContent,
        )
    } else {
        TopAppBar(
            title = title,
            modifier = barModifier,
            color = color,
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior,
            bottomContent = bottomContent,
        )
    }
}

@Composable
internal fun expandedTopBarBottomContentGap(
    scrollBehavior: ScrollBehavior,
    expandedGap: Dp,
): Dp = expandedGap * (1f - scrollBehavior.state.collapsedFraction.coerceIn(0f, 1f))
