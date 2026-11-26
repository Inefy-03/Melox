package com.melox.player.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import com.melox.player.ui.LocalLandscapeNavigation
import com.melox.player.ui.LocalSecondaryTopBar
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.upstream.glass.glassSearchBarTransition
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.*
import com.melox.player.ui.glass.*

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
    expandedContent: (@Composable () -> Unit)? = null,
    showTitleWhileExpanded: Boolean = false,
) {
    val backdrop = LocalGlassTopBackdrop.current
    val landscapeNavigation = LocalLandscapeNavigation.current
    val secondary = LocalSecondaryTopBar.current
    val navigation = landscapeNavigation.takeUnless { secondary }
    val titleStartProgress: () -> Float = remember(navigation, inlineTitleEnabled) {
        { if (inlineTitleEnabled && navigation != null) 1f - navigation.titleAlpha() else 0f }
    }
    val indexTopBarHeight = LocalAlphabetIndexTopBarHeight.current
    val searchExpanded = LocalGlassSearchPage.current?.isExpanded == true
    GlassTopAppBar(
        title = title,
        modifier = if (indexTopBarHeight != null && expandedContent == null) {
            modifier.onSizeChanged { indexTopBarHeight.intValue = it.height }
        } else modifier,
        subtitle = subtitle,
        compact = true,
        expandedContent = expandedContent,
        showTitleWhileExpanded = showTitleWhileExpanded,
        contentModifier = Modifier
            .glassSearchBarTransition(searchExpanded, LocalTopBarBlurSettings.current.blurEnabled) { 1f }
            .stableTopAppBarContentInsets(),
        backgroundAlpha = glassBackgroundAlpha,
        backdrop = backdrop, scrollBehavior = scrollBehavior,
        isContentScrolled = topBarContentScrolled(scrollBehavior),
        progressiveBlur = LocalTopBarBlurSettings.current.blurEnabled,
        defaultWindowInsetsPadding = defaultWindowInsetsPadding,
        compactTitleStartProgress = titleStartProgress,
        compactTitleObstructionStart = landscapeNavigation?.centerObstructionStart?.takeIf { secondary },
        titleAlpha = {
            val navigationProgress = navigation?.titleAlpha?.invoke()
            val navigationAlpha = if (inlineTitleEnabled) 1f else navigationProgress ?: 1f
            val inlineTitle = shouldShowInlineTopBarTitle(title, titleColor.alpha, navigationProgress, inlineTitleEnabled)
            (glassTitleAlpha?.invoke() ?: if (inlineTitle) 1f else titleColor.alpha) * navigationAlpha
        },
        navigationIcon = { TopBarButtonHaptics { navigationIcon() } },
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { TopBarButtonHaptics { actions() } }
        }, bottomContent = bottomContent,
    )
}

internal fun shouldShowInlineTopBarTitle(
    title: String,
    titleAlpha: Float,
    navigationProgress: Float?,
    enabled: Boolean,
): Boolean = enabled && title.isNotEmpty() && navigationProgress != null &&
    (navigationProgress <= 0f || titleAlpha <= 0f)
