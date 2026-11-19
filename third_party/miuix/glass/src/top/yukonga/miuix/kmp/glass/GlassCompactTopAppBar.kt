// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.anim.folmeSpring
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
    expandedContent: (@Composable () -> Unit)? = null,
    showTitleWhileExpanded: Boolean = false,
    titleBlurRadius: Dp = GlassTopAppBarDefaults.TitleBlurRadius,
) {
    val collapsible = expandedContent != null
    val scrollState = scrollBehavior?.state
    DisposableEffect(scrollBehavior, collapsible) {
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
        if (!collapsible) scrollState?.let {
            it.heightOffsetLimit = 0f
            it.heightOffset = 0f
        }
    }
    val titleFade = { ((scrollState?.collapsedFraction ?: 0f) * 3f).coerceIn(0f, 1f) }
    val smallTitleVisible by remember(scrollState, collapsible, showTitleWhileExpanded) {
        derivedStateOf { !collapsible || showTitleWhileExpanded || titleFade() >= 1f }
    }
    val smallTitleAlpha = remember { Animatable(if (smallTitleVisible) 1f else 0f) }
    val smallTitleTranslationY = remember { Animatable(if (smallTitleVisible) 0f else 20f) }
    if (collapsible) {
        LaunchedEffect(smallTitleVisible) {
            val spec = folmeSpring<Float>(damping = 1f, response = if (smallTitleVisible) 0.3f else 0.15f)
            launch { smallTitleAlpha.animateTo(if (smallTitleVisible) 1f else 0f, spec) }
            launch { smallTitleTranslationY.animateTo(if (smallTitleVisible) 0f else 20f, spec) }
        }
    }
    val titleStartProgress = LocalGlassCompactTitleStartProgress.current.coerceIn(0f, 1f)
    var compactHeaderHeight by remember { mutableIntStateOf(0) }
    Layout(
        modifier = modifier.then(if (defaultWindowInsetsPadding) Modifier.windowInsetsPadding(
            WindowInsets.displayCutout.union(WindowInsets.navigationBars).only(WindowInsetsSides.Horizontal),
        ) else Modifier)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)),
        content = {
            Box(Modifier.padding(start = GlassTopAppBarDefaults.HorizontalPadding)) { navigationIcon() }
            Row(Modifier.padding(end = GlassTopAppBarDefaults.HorizontalPadding), content = actions)
            Text(
                title,
                modifier = if (collapsible) Modifier.graphicsLayer {
                    val currentAlpha = smallTitleAlpha.value
                    alpha = currentAlpha
                    translationY = smallTitleTranslationY.value
                    val radius = titleBlurRadius.toPx() * 0.5f * (1f - currentAlpha).coerceIn(0f, 1f)
                    renderEffect = if (radius > 0.1f) BlurEffect(radius, radius, TileMode.Decal) else null
                } else Modifier,
                color = titleColor,
                style = MiuixTheme.textStyles.title4,
                maxLines = 1,
            )
            Text(subtitle, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, style = MiuixTheme.textStyles.subtitle, maxLines = 1)
            Box { bottomContent() }
            if (expandedContent != null) {
                Box(
                    Modifier.drawWithContent {
                        // Use the bar's top edge, not the moving title slot, as the clip boundary.
                        clipRect(top = -compactHeaderHeight.toFloat()) {
                            this@drawWithContent.drawContent()
                        }
                    }.layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val offset = scrollState?.heightOffset ?: 0f
                        val height = (placeable.height + offset).toInt().coerceIn(0, placeable.height)
                        layout(placeable.width, height) {
                            if (height > 0) placeable.placeRelative(0, height - placeable.height)
                        }
                    },
                ) {
                    Box(Modifier.onSizeChanged { size ->
                        if (scrollState != null) {
                            val limit = -size.height.toFloat()
                            if (scrollState.heightOffsetLimit != limit) {
                                val fraction = scrollState.collapsedFraction.coerceIn(0f, 1f)
                                scrollState.heightOffsetLimit = limit
                                scrollState.heightOffset = limit * fraction
                            }
                        }
                    }.graphicsLayer {
                        val fade = titleFade()
                        alpha = 1f - fade
                        val radius = titleBlurRadius.toPx() * fade
                        renderEffect = if (radius > 0.1f && fade < 1f) {
                            BlurEffect(radius, radius, TileMode.Decal)
                        } else null
                    }) {
                        expandedContent()
                    }
                }
            }
        },
    ) { children, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val leading = children[0].measure(loose)
        val trailing = children[1].measure(loose)
        val titleConstraints = loose.copy(maxWidth = ((constraints.maxWidth - leading.width - trailing.width).coerceAtLeast(0) * 0.9f).toInt())
        val heading = children[2].measure(titleConstraints)
        val summary = children[3].measure(titleConstraints)
        val bottom = children[4].measure(loose)
        val expanded = children.getOrNull(5)?.measure(loose)
        val expandedHeight = expanded?.height ?: 0
        val center = TopAppBarDefaults.SmallTopAppBarCenterHeight.roundToPx() / 2 + 3.dp.roundToPx()
        val contentTop = maxOf((TopAppBarDefaults.CollapsedHeight + 6.dp).roundToPx(),
            if (subtitle.isEmpty()) 0 else center + heading.height / 2 + summary.height + TopAppBarDefaults.SubtitleBottomPadding.roundToPx())
        if (collapsible) compactHeaderHeight = contentTop
        layout(constraints.maxWidth, contentTop + expandedHeight + bottom.height) {
            expanded?.placeRelative(0, contentTop, zIndex = -1f)
            leading.placeRelative(0, center - leading.height / 2)
            trailing.placeRelative(constraints.maxWidth - trailing.width, center - trailing.height / 2)
            val titleX = ((constraints.maxWidth - heading.width) / 2).coerceIn(leading.width, (constraints.maxWidth - trailing.width - heading.width).coerceAtLeast(leading.width))
            val startX = (leading.width + 12.dp.roundToPx()).coerceAtMost((constraints.maxWidth - trailing.width - heading.width).coerceAtLeast(leading.width))
            heading.placeRelative((titleX + (startX - titleX) * titleStartProgress).toInt(), center - heading.height / 2)
            if (subtitle.isNotEmpty()) summary.placeRelative((constraints.maxWidth - summary.width) / 2, center + heading.height / 2)
            bottom.placeRelative(0, contentTop + expandedHeight)
        }
    }
}
