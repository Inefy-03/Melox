package com.melox.player.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.melox.player.R
import com.melox.player.model.BottomBarStyle
import com.melox.player.ui.component.FixedPageBackgroundHost
import com.melox.player.ui.component.rememberBlurBackdrop
import com.melox.player.ui.component.os4.Icon
import com.melox.player.ui.component.os4.GlassSearchHeader
import com.melox.player.ui.component.os4.GlassSearchPage
import com.melox.player.ui.component.os4.GlassSearchSession
import com.melox.player.ui.component.os4.WideGlassSearchHost
import com.melox.player.ui.component.os4.LocalWideGlassSearchHost
import com.melox.player.ui.component.os4.dismissWideSearchOnOutsideTap
import com.melox.player.ui.component.LocalWideTopBackdrop
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import com.melox.player.ui.component.os4.IconButton
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.blur.layerBackdrop
private val Os4TopNavigationIcon = ImageVector.Builder(
    name = "TopNavigation", defaultWidth = 24.dp, defaultHeight = 24.dp,
    viewportWidth = 24f, viewportHeight = 24f,
).apply {
    path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
        moveTo(6f, 3f)
        lineTo(18f, 3f)
        curveTo(21f, 3f, 22f, 4f, 22f, 7f)
        lineTo(22f, 17f)
        curveTo(22f, 20f, 21f, 21f, 18f, 21f)
        lineTo(6f, 21f)
        curveTo(3f, 21f, 2f, 20f, 2f, 17f)
        lineTo(2f, 7f)
        curveTo(2f, 4f, 3f, 3f, 6f, 3f)
        close()
    }
    path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2.8f, strokeLineCap = StrokeCap.Round) {
        moveTo(9f, 7f)
        lineTo(15f, 7f)
    }
}.build()

private val Os4SidebarIcon = ImageVector.Builder(
    name = "Os4SidebarToggle", defaultWidth = 24.dp, defaultHeight = 24.dp,
    viewportWidth = 24f, viewportHeight = 24f,
).apply {
    path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
        moveTo(6f, 3f)
        lineTo(18f, 3f)
        curveTo(21f, 3f, 22f, 4f, 22f, 7f)
        lineTo(22f, 17f)
        curveTo(22f, 20f, 21f, 21f, 18f, 21f)
        lineTo(6f, 21f)
        curveTo(3f, 21f, 2f, 20f, 2f, 17f)
        lineTo(2f, 7f)
        curveTo(2f, 4f, 3f, 3f, 6f, 3f)
        close()
    }
    path(fill = SolidColor(Color.Black)) {
        moveTo(4f, 6.6f)
        curveTo(4f, 6.03995f, 4f, 5.75992f, 4.10899f, 5.54601f)
        curveTo(4.20487f, 5.35785f, 4.35785f, 5.20487f, 4.54601f, 5.10899f)
        curveTo(4.75992f, 5f, 5.03995f, 5f, 5.6f, 5f)
        lineTo(8.4f, 5f)
        curveTo(8.96005f, 5f, 9.24008f, 5f, 9.45399f, 5.10899f)
        curveTo(9.64215f, 5.20487f, 9.79513f, 5.35785f, 9.89101f, 5.54601f)
        curveTo(10f, 5.75992f, 10f, 6.03995f, 10f, 6.6f)
        lineTo(10f, 17.4f)
        curveTo(10f, 17.9601f, 10f, 18.2401f, 9.89101f, 18.454f)
        curveTo(9.79513f, 18.6422f, 9.64215f, 18.7951f, 9.45399f, 18.891f)
        curveTo(9.24008f, 19f, 8.96005f, 19f, 8.4f, 19f)
        lineTo(5.6f, 19f)
        curveTo(5.03995f, 19f, 4.75992f, 19f, 4.54601f, 18.891f)
        curveTo(4.35785f, 18.7951f, 4.20487f, 18.6422f, 4.10899f, 18.454f)
        curveTo(4f, 18.2401f, 4f, 17.9601f, 4f, 17.4f)
        lineTo(4f, 6.6f)
        close()
    }
}.build()

import top.yukonga.miuix.kmp.glass.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val Os4NavigationReboundMotion = spring<Dp>(stiffness = 200f, dampingRatio = 0.8f)
internal val Os4NavigationSidebarMotion = tween<Dp>(
internal val Os4SidebarHeightExpandMotion = spring<Dp>(stiffness = 220f, dampingRatio = 0.83f)
internal val Os4SidebarHeightCollapseMotion = spring<Dp>(stiffness = 462.25f, dampingRatio = 0.97f)
internal fun os4SidebarHeightMotion(expanded: Boolean, distance: Dp): SpringSpec<Dp> {
    val response = if (expanded) Os4SidebarHeightExpandMotion else Os4SidebarHeightCollapseMotion
    return spring(
        stiffness = response.stiffness,
        dampingRatio = response.dampingRatio,
        visibilityThreshold = (distance * 0.001f).coerceAtLeast(0.1.dp),
    )
}

internal val Os4NavigationIconEnterMotion = tween<Float>(200, easing = Os4NavigationSidebarMotion.easing)
internal val Os4NavigationIconExitMotion = tween<Float>(120, easing = Os4NavigationSidebarMotion.easing)
internal val Os4NavigationLabelEnterMotion = tween<Float>(160, delayMillis = 64, easing = Os4NavigationSidebarMotion.easing)
internal val Os4NavigationLabelExitMotion = tween<Float>(100, delayMillis = 64, easing = LinearEasing)

internal fun os4NavigationItemMotion(index: Int, expanded: Boolean): SpringSpec<Float> {
    // Fitted to the recording's row centers, with one response for both axes.
    val frequency = (if (expanded) 26.75f else 21.5f) / (1f + 0.03f * index)
    return spring(stiffness = frequency * frequency, dampingRatio = 0.97f, visibilityThreshold = 0.001f)
}

    durationMillis = 300,
    easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f),
)

internal class Os4LandscapeNavigation(
    val labels: List<String>,
    val titleAlpha: () -> Float,
    val leadingContentMaxWidth: Dp,
)
internal val LocalOs4LandscapeNavigation = staticCompositionLocalOf<Os4LandscapeNavigation?> { null }

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Os4LandscapePlayerScaffold(
    navigationItems: List<NavigationItem>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    showNavigation: Boolean,
    smallPlayerBar: Boolean,
    blurEnabled: Boolean,
    isDark: Boolean,
    miniPlayer: @Composable (MiniPlayerChrome) -> Unit,
    refreshSignal: () -> Float,
    shortcutItems: List<NavigationItem>,
    selectedShortcut: Int?,
    onShortcutSelected: (Int) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    // The recording shows one surface moving to the top center while shedding its height.
    // The surface has its own settle. Each row shares one response across both axes.
    val progress by animateFloatAsState(
        if (expanded) 1f else 0f,
        tween(300, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)),
        label = "os4NavigationMorph",
    )
    val backdrop = rememberBlurBackdrop()
    val searchHost = remember { WideGlassSearchHost() }
    var searchCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var retainedSearch by remember { mutableStateOf<Pair<GlassSearchPage, GlassSearchSession>?>(null) }
    val expandedSearchPage = searchHost.expandedPage
    LaunchedEffect(expandedSearchPage, expandedSearchPage?.session) {
        expandedSearchPage?.session?.let { retainedSearch = expandedSearchPage to it }
    }
    LaunchedEffect(expandedSearchPage, retainedSearch?.first?.transitionActive) {
        if (expandedSearchPage == null && retainedSearch?.first?.transitionActive == false) {
            retainedSearch = null
        }
    }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val itemContentPadding = 14.dp
    val itemIconSlotWidth = 24.dp + 12.dp
    val sidebarStart = 12.dp
    val sidebarWidth = 250.dp
    val itemHorizontalMargin = 12.dp
    val itemWidth = sidebarWidth - itemHorizontalMargin * 2
    val railInset = if (showNavigation) (sidebarStart + sidebarWidth) * progress else 0.dp
    val textStyle = MiuixTheme.textStyles.body1
    val primaryItems = navigationItems.take(3)
    // Reserve the original indicator's full width in each independent top slot.
    val destinationPadding = 24.dp + GlassNavigationBarDefaults.IndicatorOverhang * 2
    val sourceWidths = (primaryItems + shortcutItems).map { item ->
        with(density) { textMeasurer.measure(item.label, textStyle).size.width.toDp() } + destinationPadding
    }
    val tabWidths = sourceWidths.take(primaryItems.size)
    // Rows and opacity channels retarget on the same input frame.
    val iconAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = if (expanded) Os4NavigationIconEnterMotion else Os4NavigationIconExitMotion,
        label = "os4NavigationIconOpacity",
    )
    val shortcutLabelAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = if (expanded) Os4NavigationLabelEnterMotion else Os4NavigationLabelExitMotion,
        label = "os4NavigationLabelOpacity",
    )
    val itemProgress = List(sourceWidths.size) { index ->
        animateFloatAsState(
            targetValue = if (expanded) 1f else 0f,
            animationSpec = os4NavigationItemMotion(index, expanded),
            label = "os4NavigationItem$index",
        ).value.coerceIn(0f, 1f)
    }
    val shortcutScrollState = rememberScrollState()
    val leadingActionWidth = GlassTopAppBarDefaults.ButtonSize + 4.dp
    val capsuleWidth = leadingActionWidth + 11.dp + tabWidths.fold(0.dp) { sum, width -> sum + width }
    val interactions = remember(navigationItems.size) { List(navigationItems.size) { MutableInteractionSource() } }
    FixedPageBackgroundHost(Modifier.fillMaxSize(), refreshSignal = refreshSignal) {
        BoxWithConstraints(Modifier.fillMaxSize()
            .onGloballyPositioned { searchCoordinates = it }
            .dismissWideSearchOnOutsideTap({ searchHost.expandedPage }) { searchCoordinates }
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
            val viewportWidth = maxWidth
            val viewportHeight = maxHeight
            val collapsedWidth = capsuleWidth.coerceAtMost((viewportWidth - 162.dp).coerceAtLeast(192.dp))
            val collapsedStart = (viewportWidth - collapsedWidth) / 2
            CompositionLocalProvider(
                LocalWideGlassSearchHost provides searchHost,
                LocalFloatingNavigationRailInset provides railInset,
                LocalOs4LandscapeNavigation provides Os4LandscapeNavigation(
                    navigationItems.map { it.label }, titleAlpha = { progress },
                    leadingContentMaxWidth = (collapsedStart - GlassTopAppBarDefaults.HorizontalPadding * 2).coerceAtLeast(0.dp),
                ),
            ) {
                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        Box(Modifier.fillMaxWidth().padding(start = railInset).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Box(Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                                miniPlayer(MiniPlayerChrome(BottomBarStyle.OS4, backdrop, blurEnabled && backdrop != null, false, isDark, smallPlayerBar = smallPlayerBar))
                            }
                        }
                    },
                ) { padding ->
                    // Keep the shared navigation surface and player outside their own capture.
                    Box(Modifier.fillMaxSize().then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)) { content(padding) }
                }
                if (showNavigation) {
                    val topInset = WindowInsets.statusBarsIgnoringVisibility.union(WindowInsets.systemBars)
                        .asPaddingValues().calculateTopPadding()
                    val panelWidth = collapsedWidth + (sidebarWidth - collapsedWidth) * progress
                    val collapsedHeight = GlassTopAppBarDefaults.ButtonSize
                    val collapsedTop = topInset + (TopAppBarDefaults.SmallTopAppBarCenterHeight - collapsedHeight) / 2 + 3.dp
                    val expandedTop = if (topInset == 0.dp) sidebarStart else topInset + (TopAppBarDefaults.SmallTopAppBarCenterHeight - collapsedHeight) / 2
                    val panelTop = collapsedTop + (expandedTop - collapsedTop) * progress
                    val expandedHeight = (viewportHeight - panelTop - sidebarStart).coerceAtLeast(collapsedHeight)
                    val panelHeight by animateDpAsState(
                        targetValue = if (expanded) expandedHeight else collapsedHeight,
                        animationSpec = os4SidebarHeightMotion(expanded, expandedHeight - collapsedHeight),
                        label = "os4SidebarBottomSettle",
                    )
                    // Position continues settling after the height/width morph reaches its endpoint.
                    val panelStart by animateDpAsState(
                        targetValue = if (expanded) sidebarStart else collapsedStart,
                        animationSpec = if (expanded) Os4NavigationSidebarMotion else Os4NavigationReboundMotion,
                        label = "os4NavigationHorizontalSettle",
                    )
                    val radius = 22.dp + (20.dp - 22.dp) * progress
                    val widthRatio = (collapsedWidth - leadingActionWidth - 11.dp) / (capsuleWidth - leadingActionWidth - 11.dp)
                    val measuredTabWidths = tabWidths.map { it * widthRatio }
                    // Hidden destinations continue the top row's slots past its trailing edge.
                    val sourceStarts = sourceWidths.map { it * widthRatio }
                        .runningFold(leadingActionWidth + 8.dp) { start, width -> start + width }
                        .dropLast(1)
                    if (progress == 0f && itemProgress.all { it == 0f } && panelHeight == collapsedHeight) {
                        GlassNavigationBar(
                            items = primaryItems.map { GlassNavigationItem(it.icon, it.label) },
                            selectionVisible = selectedTab in primaryItems.indices,
                            selectedIndex = selectedTab,
                            onSelect = onTabSelected,
                            backdrop = backdrop.takeIf { blurEnabled },
                            modifier = Modifier.offset(x = panelStart, y = panelTop).width(collapsedWidth),
                            defaultWindowInsetsPadding = false,
                            height = GlassTopAppBarDefaults.ButtonSize,
                            labelsOnly = true,
                            labelStyle = textStyle,
                            destinationWidths = measuredTabWidths,
                            indicatorOverhang = 0.dp,
                            contentPaddingEnd = 3.dp,
                            leadingActionWidth = leadingActionWidth,
                            leadingAction = {
                                GlassIconButton(onClick = { expanded = true }, modifier = Modifier.offset(x = 2.dp), size = collapsedHeight - GlassNavigationBarDefaults.IndicatorPaddingVertical * 2, surfaceAlpha = 0f, shadow = null) {
                        val lastPrimaryRowBottom = 3.dp +
                            (61.dp + 46.dp * primaryItems.lastIndex) + 42.dp
                        val dividerGap = 12.dp
                        val dividerY = lastPrimaryRowBottom + dividerGap
                        val shortcutsTop = dividerY + DividerDefaults.Thickness + dividerGap
                        val shortcutScrollOffset = with(density) { shortcutScrollState.value.toDp() }
                                    Icon(Os4SidebarIcon, stringResource(R.string.navigation_expand), Modifier.size(24.dp))
                                }
                            },
                        )
                    } else Box(
                        Modifier.offset(x = panelStart, y = panelTop)
                            .size(panelWidth, panelHeight)
                            .glassPanel(
                                backdrop = backdrop.takeIf { blurEnabled }, shape = GlassShape(radius),
                                material = GlassNavigationBarDefaults.material(), stroke = GlassNavigationBarDefaults.stroke(),
                                shadow = GlassShadows.Float, shading = false,
                                fallback = Modifier.clip(GlassShape(radius)).background(MiuixTheme.colorScheme.surfaceContainer),
                            ).clip(GlassShape(radius)).selectableGroup(),
                    ) {
                        val slot = selectedTab.coerceIn(0, primaryItems.lastIndex).toFloat()
                        val leading = slot.toInt()
                        val fraction = slot - leading
                        val widths = measuredTabWidths
                        val selectedStart = widths.take(leading).fold(leadingActionWidth + 8.dp) { sum, width -> sum + width } + widths[leading] * fraction
                        val selectedWidth = widths[leading]
                        val selectionProgress = itemProgress[selectedShortcut?.let { primaryItems.size + it } ?: leading]
                        val selectionTargetY = selectedShortcut?.let { shortcutsTop + 46.dp * it - shortcutScrollOffset }
                            ?: (64.dp + 46.dp * slot)
                        if (selectedTab in primaryItems.indices) GlassNavigationIndicator(
                            pressed = false,
                            scaleOnPress = false,
                            height = 38.dp + 4.dp * selectionProgress,
                            modifier = Modifier.offset(
                                x = selectedStart + (itemHorizontalMargin - selectedStart) * selectionProgress,
                                y = 3.dp + (selectionTargetY - 3.dp) * selectionProgress,
                            ).size(selectedWidth + (itemWidth - selectedWidth) * selectionProgress, 38.dp + 4.dp * selectionProgress),
                        )
                        Box(Modifier.offset(x = 8.dp + 8.dp * progress, y = 12.dp * progress).size(GlassTopAppBarDefaults.ButtonSize), contentAlignment = Alignment.Center) {
                            GlassIconButton(onClick = { expanded = !expanded }, size = 38.dp + 6.dp * progress, surfaceAlpha = 0f, shadow = null) {
                                val description = stringResource(if (expanded) R.string.navigation_collapse else R.string.navigation_expand)
                                Box(Modifier.size(24.dp).semantics { contentDescription = description }) {
                                    Icon(Os4TopNavigationIcon, null, Modifier.fillMaxSize().graphicsLayer { alpha = progress })
                                    Icon(Os4SidebarIcon, null, Modifier.fillMaxSize().graphicsLayer { alpha = 1f - progress })
                                }
                            }
                        }
                        Box(Modifier.offset(x = panelWidth - 16.dp - GlassTopAppBarDefaults.ButtonSize, y = 12.dp).graphicsLayer { alpha = shortcutLabelAlpha }) {
                            GlassIconButton(onClick = { onTabSelected(3) }, surfaceAlpha = 0f, shadow = null) {
                                Icon(navigationItems[3].icon, navigationItems[3].label, Modifier.size(24.dp))
                            }
                        }
                        val dividerProgress = itemProgress.getOrElse(primaryItems.size) { itemProgress.last() }
                        val dividerSourceSlot = primaryItems.size.coerceAtMost(sourceWidths.lastIndex)
                        val dividerLabelWidth = sourceWidths[dividerSourceSlot] - destinationPadding
                        val dividerTextInset = ((sourceWidths[dividerSourceSlot] * widthRatio - dividerLabelWidth) / 2).coerceAtLeast(0.dp)
                        val dividerSourceX = sourceStarts[dividerSourceSlot] + dividerTextInset - itemContentPadding - itemIconSlotWidth
                        HorizontalDivider(
                            Modifier.offset(
                                x = 21.dp + (dividerSourceX - itemHorizontalMargin) * (1f - dividerProgress),
                                y = 22.dp + (dividerY - 22.dp) * dividerProgress,
                            ).width(sidebarWidth - 42.dp).graphicsLayer { alpha = shortcutLabelAlpha },
                        )
                        val sidebarSettled = expanded && itemProgress.all { it == 1f }
                                val globalIndex = primaryItems.size + index
                                val rowProgress = itemProgress[globalIndex]
                                val labelWidth = sourceWidths[globalIndex] - destinationPadding
                                val textInset = ((sourceWidths[globalIndex] * widthRatio - labelWidth) / 2).coerceAtLeast(0.dp)
                                val sourceX = sourceStarts[globalIndex] + textInset - itemContentPadding - itemIconSlotWidth
                                val rowX = sourceX + (itemHorizontalMargin - sourceX) * rowProgress
                                val targetY = shortcutsTop + 46.dp * index - shortcutScrollOffset
                                val rowY = 3.dp + (targetY - 3.dp) * rowProgress
                        Column(
                            Modifier.offset(x = itemHorizontalMargin, y = shortcutsTop)
                                        .graphicsLayer {
                                            translationX = (rowX - itemHorizontalMargin).toPx()
                                            translationY = if (sidebarSettled) 0f else
                                                (rowY - shortcutsTop - 46.dp * index).toPx()
                                        }
                                .width(itemWidth)
                                .then(
                                    if (sidebarSettled) {
                                        Modifier.height((panelHeight - shortcutsTop).coerceAtLeast(0.dp))
                                            .verticalScroll(shortcutScrollState)
                                    } else {
                                        Modifier.wrapContentHeight(Alignment.Top, unbounded = true)
                                    },
                                ),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            shortcutItems.forEachIndexed { index, item ->
                            val rowProgress = itemProgress[index]
                            val tabStart = sourceStarts[index]
                                Row(
                                    Modifier.fillMaxWidth().height(42.dp)
                                        .clip(GlassShape(21.dp))
                                        .selectable(expanded && selectedShortcut == index, role = Role.Tab, onClick = { onShortcutSelected(index) })
                                        .padding(horizontal = itemContentPadding),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(item.icon, null, Modifier.size(24.dp).graphicsLayer { alpha = iconAlpha })
                                    Spacer(Modifier.width(12.dp))
                                    Text(item.label, modifier = Modifier.graphicsLayer { alpha = shortcutLabelAlpha }, style = textStyle, maxLines = 1)
                                }
                            }
                        }
                        primaryItems.forEachIndexed { index, item ->
                            val collapsedTabWidth = measuredTabWidths[index]
                            val labelWidth = tabWidths[index] - destinationPadding
                            val collapsedTextInset = ((collapsedTabWidth - labelWidth) / 2).coerceAtLeast(0.dp)
                            // Keep the glyph and label together while preserving the native label anchor.
                            val sourceX = tabStart + collapsedTextInset - itemContentPadding - itemIconSlotWidth
                            val x = sourceX + (itemHorizontalMargin - sourceX) * rowProgress
                            val y = 3.dp + (61.dp + 46.dp * index) * rowProgress
                            val sourceWidth = labelWidth + itemContentPadding * 2 + itemIconSlotWidth
                            val width = sourceWidth + (itemWidth - sourceWidth) * rowProgress
                            Row(
                                Modifier.offset(x = x, y = y).width(width).height(38.dp + 4.dp * rowProgress)
                                    .clip(GlassShape(21.dp))
                                    .selectable(selected = (selectedShortcut == null || !expanded) && selectedTab == index, role = Role.Tab, interactionSource = interactions[index], indication = LocalIndication.current, onClick = { onTabSelected(index) })
                                    .padding(horizontal = itemContentPadding),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.width(itemIconSlotWidth).graphicsLayer { alpha = iconAlpha }) {
                                    Icon(item.icon, null, Modifier.size(24.dp), tint = MiuixTheme.colorScheme.onSurface)
                                }
                                Text(
                                    item.label,
                                    modifier = Modifier.wrapContentWidth(Alignment.Start, unbounded = true),
                                    color = MiuixTheme.colorScheme.onSurface,
                                    style = textStyle,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    }
                }
                retainedSearch?.let { (page, session) ->
                    CompositionLocalProvider(LocalWideTopBackdrop provides backdrop) {
                        GlassSearchHeader(
                            session = session,
                            page = page,
                            expanded = expandedSearchPage === page && page.session === session && page.isExpanded,
                        )
                    }
                }
            }
        }
    }
}
