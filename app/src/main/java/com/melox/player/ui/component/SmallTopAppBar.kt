package com.melox.player.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import com.melox.player.ui.LocalLandscapeNavigation
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.glass.*

@Composable
internal fun SmallTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.surface,
    titleColor: Color = MiuixTheme.colorScheme.onSurface,
    subtitle: String = "",
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: ScrollBehavior? = null,
    defaultWindowInsetsPadding: Boolean = true,
    inlineTitleEnabled: Boolean = true,
    bottomContent: @Composable () -> Unit = {},
    glassBackgroundAlpha: () -> Float = { 1f },
    glassTitleAlpha: (() -> Float)? = null,
) {
    val backdrop = LocalGlassTopBackdrop.current
    val navigation = LocalLandscapeNavigation.current
    val inlineTitle = shouldShowInlineTopBarTitle(
        title, titleColor.alpha, navigation?.titleAlpha?.invoke(), inlineTitleEnabled,
    )
    val searchExpanded = com.melox.player.ui.component.LocalGlassSearchPage.current?.isExpanded == true &&
        com.melox.player.ui.LocalLandscapeNavigation.current == null
    val searchAlpha = com.melox.player.ui.component.rememberGlassSearchBarAlpha(searchExpanded)
    val searchTitleAlpha = com.melox.player.ui.component.rememberGlassSearchTitleAlpha(
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
            backgroundAlpha = { searchAlpha.value * glassBackgroundAlpha() },
            backdrop = backdrop, scrollBehavior = scrollBehavior,
            isContentScrolled = topBarContentScrolled(scrollBehavior),
            visualContentOffset = rememberTopBarVisualContentOffset(scrollBehavior),
            progressiveBlur = LocalTopBarBlurSettings.current.progressiveEnabled && LocalTopBarBlurSettings.current.blurEnabled,
            defaultWindowInsetsPadding = defaultWindowInsetsPadding,
            titleAlpha = {
                val navigationAlpha = if (inlineTitleEnabled) 1f else navigation?.titleAlpha?.invoke() ?: 1f
                (glassTitleAlpha?.invoke() ?: if (inlineTitle) 1f else titleColor.alpha) * searchTitleAlpha() * navigationAlpha
            },
            navigationIcon = { TopBarButtonHaptics { navigationIcon() } },
            actions = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { TopBarButtonHaptics { actions() } }
            }, bottomContent = bottomContent,
        )
    }
}

internal fun shouldShowInlineTopBarTitle(
    title: String,
    titleAlpha: Float,
    navigationProgress: Float?,
    enabled: Boolean,
): Boolean = enabled && title.isNotEmpty() && navigationProgress != null &&
    (navigationProgress <= 0f || titleAlpha <= 0f)
