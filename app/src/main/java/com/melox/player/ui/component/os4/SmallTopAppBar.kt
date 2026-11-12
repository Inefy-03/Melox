package com.melox.player.ui.component.os4

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import com.melox.player.ui.LocalOs4LandscapeNavigation
import androidx.compose.ui.Alignment
import com.melox.player.ui.component.stableTopAppBarContentInsets
import com.melox.player.ui.component.rememberTopBarOverlapDistance
import androidx.compose.runtime.Composable
import com.melox.player.ui.component.os4.glassSearchBarTransition
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.glass.*
import com.melox.player.ui.theme.LocalOs4Style
import com.melox.player.ui.component.LocalGlassTopBackdrop
import com.melox.player.ui.component.LocalTopBarBlurSettings
import top.yukonga.miuix.kmp.basic.SmallTopAppBar as MiuixSmallTopAppBar

@Composable
internal fun SmallTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.surface,
    titleColor: Color = MiuixTheme.colorScheme.onSurface,
    subtitle: String = "",
    subtitleColor: Color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: ScrollBehavior? = null,
    defaultWindowInsetsPadding: Boolean = true,
    titlePadding: Dp = TopAppBarDefaults.TitlePadding,
    navigationIconPadding: Dp = TopAppBarDefaults.NavigationIconPadding,
    actionIconPadding: Dp = TopAppBarDefaults.ActionIconPadding,
    inlineTitleEnabled: Boolean = true,
    bottomContent: @Composable () -> Unit = {},
) {
    val backdrop = LocalGlassTopBackdrop.current
    if (LocalOs4Style.current) {
        val navigation = LocalOs4LandscapeNavigation.current
        val inlineTitle = shouldShowInlineTopBarTitle(
            title, titleColor.alpha, navigation?.titleAlpha?.invoke(), inlineTitleEnabled,
        )
        val searchExpanded = com.melox.player.ui.component.os4.LocalGlassSearchPage.current?.isExpanded == true &&
            com.melox.player.ui.LocalOs4LandscapeNavigation.current == null
        val searchAlpha = com.melox.player.ui.component.os4.rememberGlassSearchBarAlpha(searchExpanded)
        val searchTitleAlpha = com.melox.player.ui.component.os4.rememberGlassSearchTitleAlpha(
            searchExpanded,
            scrollBehavior?.state?.let { it.heightOffset == it.heightOffsetLimit } == true,
        )
        androidx.compose.runtime.CompositionLocalProvider(
            LocalGlassCompactTitleStartProgress provides (if (inlineTitleEnabled) navigation?.titleAlpha?.let { 1f - it() } ?: 0f else 0f),
        ) {
            GlassTopAppBar(
                title = title, modifier = modifier, subtitle = subtitle,
                compact = true,
                contentModifier = Modifier.stableTopAppBarContentInsets()
                    .glassSearchBarTransition(searchExpanded) { searchAlpha.value },
                backgroundAlpha = { searchAlpha.value },
                backdrop = backdrop, scrollBehavior = scrollBehavior,
                overlapDistance = rememberTopBarOverlapDistance(scrollBehavior),
                progressiveBlur = LocalTopBarBlurSettings.current.progressiveEnabled && LocalTopBarBlurSettings.current.blurEnabled,
                defaultWindowInsetsPadding = defaultWindowInsetsPadding,
                titleAlpha = { (if (inlineTitle) 1f else titleColor.alpha) * searchTitleAlpha() },
                navigationIcon = { TopBarButtonHaptics { navigationIcon() } },
                actions = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { TopBarButtonHaptics { actions() } }
                }, bottomContent = bottomContent,
            )
        }
    } else {
        MiuixSmallTopAppBar(title = title, modifier = modifier, color = color, titleColor = titleColor, subtitle = subtitle, subtitleColor = subtitleColor, navigationIcon = { TopBarButtonHaptics { navigationIcon() } }, actions = { TopBarButtonHaptics { actions() } }, scrollBehavior = scrollBehavior, defaultWindowInsetsPadding = defaultWindowInsetsPadding, titlePadding = titlePadding, navigationIconPadding = navigationIconPadding, actionIconPadding = actionIconPadding, bottomContent = bottomContent)
    }
}

internal fun shouldShowInlineTopBarTitle(
    title: String,
    titleAlpha: Float,
    navigationProgress: Float?,
    enabled: Boolean,
): Boolean = enabled && title.isNotEmpty() && navigationProgress != null &&
    (navigationProgress <= 0f || titleAlpha <= 0f)
