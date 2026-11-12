// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.glass

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val LocalGlassCompactTitleStartProgress = staticCompositionLocalOf { 0f }

/** Native small-bar geometry without clipping the Glass controls' shadow halo. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GlassCompactTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    titleColor: Color,
    subtitle: String,
    scrollBehavior: ScrollBehavior?,
    defaultWindowInsetsPadding: Boolean,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    bottomContent: @Composable () -> Unit,
) {
    DisposableEffect(scrollBehavior) {
        val state = scrollBehavior?.state
        val previousLimit = state?.heightOffsetLimit ?: 0f
        onDispose {
            if (state != null && previousLimit < 0f) {
                state.heightOffsetLimit = previousLimit
                state.heightOffset = if (state.contentOffset < 0f) previousLimit else 0f
            }
        }
    }
    SideEffect {
        scrollBehavior?.state?.let {
            it.heightOffsetLimit = 0f
            it.heightOffset = 0f
        }
    }
    val titleStartProgress = LocalGlassCompactTitleStartProgress.current.coerceIn(0f, 1f)
    Layout(
        modifier = modifier.then(if (defaultWindowInsetsPadding) Modifier.windowInsetsPadding(
            WindowInsets.displayCutout.union(WindowInsets.navigationBars).only(WindowInsetsSides.Horizontal),
        ) else Modifier)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)),
        content = {
            Box(Modifier.padding(start = GlassTopAppBarDefaults.HorizontalPadding)) { navigationIcon() }
            Row(Modifier.padding(end = GlassTopAppBarDefaults.HorizontalPadding), content = actions)
            Text(title, color = titleColor, style = MiuixTheme.textStyles.title4, maxLines = 1)
            Text(subtitle, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, style = MiuixTheme.textStyles.subtitle, maxLines = 1)
            Box { bottomContent() }
        },
    ) { children, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val leading = children[0].measure(loose)
        val trailing = children[1].measure(loose)
        val titleConstraints = loose.copy(maxWidth = ((constraints.maxWidth - leading.width - trailing.width).coerceAtLeast(0) * 0.9f).toInt())
        val heading = children[2].measure(titleConstraints)
        val summary = children[3].measure(titleConstraints)
        val bottom = children[4].measure(loose)
        val center = TopAppBarDefaults.SmallTopAppBarCenterHeight.roundToPx() / 2 + 3.dp.roundToPx()
        val contentTop = maxOf((TopAppBarDefaults.CollapsedHeight + 6.dp).roundToPx(),
            if (subtitle.isEmpty()) 0 else center + heading.height / 2 + summary.height + TopAppBarDefaults.SubtitleBottomPadding.roundToPx())
        layout(constraints.maxWidth, contentTop + bottom.height) {
            leading.placeRelative(0, center - leading.height / 2)
            trailing.placeRelative(constraints.maxWidth - trailing.width, center - trailing.height / 2)
            val titleX = ((constraints.maxWidth - heading.width) / 2).coerceIn(leading.width, (constraints.maxWidth - trailing.width - heading.width).coerceAtLeast(leading.width))
            val startX = (leading.width + 12.dp.roundToPx()).coerceAtMost((constraints.maxWidth - trailing.width - heading.width).coerceAtLeast(leading.width))
            heading.placeRelative((titleX + (startX - titleX) * titleStartProgress).toInt(), center - heading.height / 2)
            if (subtitle.isNotEmpty()) summary.placeRelative((constraints.maxWidth - summary.width) / 2, center + heading.height / 2)
            bottom.placeRelative(0, contentTop)
        }
    }
}
