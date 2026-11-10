package com.melox.player.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import com.melox.player.ui.component.rememberTopBarOverlapDistance
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.melox.player.ui.usesMiuixSmallTopAppBar
import top.yukonga.miuix.kmp.basic.ScrollBehavior
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
        top.yukonga.miuix.kmp.glass.GlassTopAppBar(
            title = title, modifier = barModifier,
            compact = usesMiuixSmallTopAppBar(),
            contentModifier = Modifier.stableTopAppBarContentInsets(),
            scrollBehavior = scrollBehavior,
            backdrop = LocalGlassTopBackdrop.current,
            overlapDistance = rememberTopBarOverlapDistance(scrollBehavior),
            progressiveBlur = LocalTopBarBlurSettings.current.progressiveEnabled && LocalTopBarBlurSettings.current.blurEnabled,
            bandBrush = if (LocalGlassTopBackdrop.current != null) top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults.bandBrush(MiuixTheme.colorScheme.surface)
                else androidx.compose.ui.graphics.SolidColor(color),
            bandOverhang = if (LocalTopBarBlurSettings.current.blurEnabled) top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults.BandOverhang else 0.dp,
            navigationIcon = navigationIcon,
            actions = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { actions() }
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
