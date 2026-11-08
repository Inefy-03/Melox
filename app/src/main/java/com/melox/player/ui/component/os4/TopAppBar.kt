package com.melox.player.ui.component.os4

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import com.melox.player.ui.component.stableTopAppBarContentInsets
import com.melox.player.ui.component.rememberTopBarOverlapDistance
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.glass.*
import androidx.compose.ui.graphics.SolidColor
import com.melox.player.ui.theme.LocalOs4Style
import com.melox.player.ui.component.LocalGlassTopBackdrop
import com.melox.player.ui.component.LocalTopBarBlurSettings
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar

@Composable
internal fun TopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.surface,
    titleColor: Color = MiuixTheme.colorScheme.onSurface,
    largeTitle: String = title,
    largeTitleColor: Color = MiuixTheme.colorScheme.onSurface,
    subtitle: String = "",
    subtitleColor: Color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: ScrollBehavior? = null,
    defaultWindowInsetsPadding: Boolean = true,
    titlePadding: Dp = TopAppBarDefaults.TitlePadding,
    navigationIconPadding: Dp = TopAppBarDefaults.NavigationIconPadding,
    actionIconPadding: Dp = TopAppBarDefaults.ActionIconPadding,
    bottomContent: @Composable () -> Unit = {},
) {
    val backdrop = LocalGlassTopBackdrop.current
    if (LocalOs4Style.current) {
        GlassTopAppBar(
            title = title, modifier = modifier, subtitle = subtitle,
            contentModifier = Modifier.stableTopAppBarContentInsets(),
            backdrop = backdrop, scrollBehavior = scrollBehavior,
            overlapDistance = rememberTopBarOverlapDistance(scrollBehavior),
            progressiveBlur = LocalTopBarBlurSettings.current.progressiveEnabled && LocalTopBarBlurSettings.current.blurEnabled,
            defaultWindowInsetsPadding = defaultWindowInsetsPadding,
            titleAlpha = { titleColor.alpha },
            bandOverhang = if (LocalTopBarBlurSettings.current.blurEnabled) GlassTopAppBarDefaults.BandOverhang else 0.dp,
            bandBrush = if (backdrop != null) GlassTopAppBarDefaults.bandBrush(MiuixTheme.colorScheme.surface) else SolidColor(color),
            navigationIcon = navigationIcon,
            actions = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { actions() }
            }, bottomContent = bottomContent,
        )
    } else {
        MiuixTopAppBar(title = title, modifier = modifier, color = color, titleColor = titleColor, largeTitle = largeTitle, largeTitleColor = largeTitleColor, subtitle = subtitle, subtitleColor = subtitleColor, navigationIcon = navigationIcon, actions = actions, scrollBehavior = scrollBehavior, defaultWindowInsetsPadding = defaultWindowInsetsPadding, titlePadding = titlePadding, navigationIconPadding = navigationIconPadding, actionIconPadding = actionIconPadding, bottomContent = bottomContent)
    }
}
