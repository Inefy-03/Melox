package com.melox.player.ui

import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
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
import top.yukonga.miuix.kmp.glass.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Sidebar
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val Os4NavigationReboundMotion = spring<Dp>(stiffness = 200f, dampingRatio = 0.8f)
internal val Os4NavigationSidebarMotion = tween<Dp>(
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
    // All geometry shares this reversible timeline; text and icons retain their own scale.
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
    // Intrinsic scale from the 3048 px / 400 dpi reference, independent of window resizing.
    val referenceScale = 1.1288889f
    val sidebarStart = 10.dp * referenceScale
    val sidebarWidth = 222.dp * referenceScale
    val railInset = if (showNavigation) (sidebarStart + sidebarWidth) * progress else 0.dp
    val textStyle = MiuixTheme.textStyles.body1
    val primaryItems = navigationItems.take(3)
    // Reserve the original indicator's full width in each independent top slot.
    val destinationPadding = 24.dp + GlassNavigationBarDefaults.IndicatorOverhang * 2
    val tabWidths = primaryItems.map { item ->
        with(density) { textMeasurer.measure(item.label, textStyle).size.width.toDp() } + destinationPadding
    }
    val capsuleWidth = IconButtonDefaults.MinHeight + 11.dp + tabWidths.fold(0.dp) { sum, width -> sum + width }
    val interactions = remember(navigationItems.size) { List(navigationItems.size) { MutableInteractionSource() } }
    FixedPageBackgroundHost(Modifier.fillMaxSize(), refreshSignal = refreshSignal) {
        BoxWithConstraints(Modifier.fillMaxSize()
            .onGloballyPositioned { searchCoordinates = it }
            .dismissWideSearchOnOutsideTap({ searchHost.expandedPage }) { searchCoordinates }
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
            val viewportWidth = maxWidth
            val viewportHeight = maxHeight
            val collapsedWidth = capsuleWidth.coerceAtMost((viewportWidth - 144.dp * referenceScale).coerceAtLeast(170.dp * referenceScale))
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
                    val statusInset = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
                    val panelWidth = collapsedWidth + (sidebarWidth - collapsedWidth) * progress
                    val collapsedHeight = IconButtonDefaults.MinHeight
                    val panelTop = statusInset + (TopAppBarDefaults.SmallTopAppBarCenterHeight - collapsedHeight) / 2 + 3.dp * (1f - progress)
                    val expandedHeight = (viewportHeight - panelTop - 10.dp * referenceScale).coerceAtLeast(collapsedHeight)
                    val panelHeight by animateDpAsState(
                        targetValue = if (expanded) expandedHeight else collapsedHeight,
                        animationSpec = if (expanded) Os4NavigationReboundMotion else Os4NavigationSidebarMotion,
                        label = "os4SidebarBottomSettle",
                    )
                    // Position continues settling after the height/width morph reaches its endpoint.
                    val panelStart by animateDpAsState(
                        targetValue = if (expanded) sidebarStart else collapsedStart,
                        animationSpec = if (expanded) Os4NavigationSidebarMotion else Os4NavigationReboundMotion,
                        label = "os4NavigationHorizontalSettle",
                    )
                    val radius = 20.dp + (18.dp * referenceScale - 20.dp) * progress
                    val widthRatio = (collapsedWidth - IconButtonDefaults.MinHeight - 11.dp) / (capsuleWidth - IconButtonDefaults.MinHeight - 11.dp)
                    val measuredTabWidths = tabWidths.map { it * widthRatio }
                    if (progress == 0f) {
                        GlassNavigationBar(
                            items = primaryItems.map { GlassNavigationItem(it.icon, it.label) },
                            selectionVisible = selectedShortcut == null && selectedTab in primaryItems.indices,
                            selectedIndex = selectedTab,
                            onSelect = onTabSelected,
                            backdrop = backdrop.takeIf { blurEnabled },
                            modifier = Modifier.offset(x = panelStart, y = panelTop).width(collapsedWidth),
                            defaultWindowInsetsPadding = false,
                            height = IconButtonDefaults.MinHeight,
                            labelsOnly = true,
                            labelStyle = textStyle,
                            destinationWidths = measuredTabWidths,
                            indicatorOverhang = 0.dp,
                            contentPaddingEnd = 3.dp,
                            leadingActionWidth = IconButtonDefaults.MinHeight,
                            leadingAction = {
                                GlassIconButton(onClick = { expanded = true }, size = 34.dp, surfaceAlpha = 0f, shadow = null) {
                                    Icon(MiuixIcons.Sidebar, stringResource(R.string.navigation_expand), Modifier.size(24.dp))
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
                        val selectedStart = widths.take(leading).fold(48.dp) { sum, width -> sum + width } + widths[leading] * fraction
                        val selectedWidth = widths[leading]
                        if (selectedTab in primaryItems.indices && selectedShortcut == null) GlassNavigationIndicator(
                            pressed = false,
                            scaleOnPress = false,
                            height = 34.dp + 8.dp * progress,
                            modifier = Modifier.offset(
                                x = selectedStart + (10.dp * referenceScale - selectedStart) * progress,
                                y = 3.dp + (54.dp + 41.dp * slot) * referenceScale * progress,
                            ).size(selectedWidth + (202.dp * referenceScale - selectedWidth) * progress, 34.dp + 8.dp * progress),
                        )
                        Box(Modifier.offset(x = 4.dp + (10.dp * referenceScale + 2.dp) * progress, y = 12.dp * progress)) {
                            IconButton(onClick = { expanded = !expanded }, glassEnabled = false, minWidth = IconButtonDefaults.MinHeight, minHeight = IconButtonDefaults.MinHeight) {
                                Icon(MiuixIcons.Sidebar, stringResource(if (expanded) R.string.navigation_collapse else R.string.navigation_expand), Modifier.size(24.dp))
                            }
                        }
                        Box(Modifier.offset(x = sidebarWidth - (10.dp * referenceScale + 6.dp) - 40.dp, y = 12.dp).graphicsLayer { alpha = progress }) {
                            IconButton(onClick = { onTabSelected(3) }, glassEnabled = false) {
                                Icon(navigationItems[3].icon, navigationItems[3].label, Modifier.size(24.dp))
                            }
                        }
                        val lastPrimaryRowBottom = 3.dp +
                            (54.dp + 41.dp * primaryItems.lastIndex) * referenceScale + 42.dp
                        val dividerGap = 12.dp
                        val dividerY = lastPrimaryRowBottom + dividerGap
                        val shortcutsTop = dividerY + DividerDefaults.Thickness + dividerGap
                        HorizontalDivider(Modifier.offset(x = 21.dp, y = dividerY).width(sidebarWidth - 42.dp).graphicsLayer { alpha = progress })
                        Column(
                            Modifier.offset(x = 10.dp * referenceScale, y = shortcutsTop)
                                .width(202.dp * referenceScale)
                                .height((panelHeight - shortcutsTop).coerceAtLeast(0.dp))
                                .graphicsLayer { alpha = progress }
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(41.dp * referenceScale - 42.dp),
                        ) {
                            shortcutItems.forEachIndexed { index, item ->
                                Row(
                                    Modifier.fillMaxWidth().height(42.dp)
                                        .clip(GlassShape(21.dp))
                                        .background(if (selectedShortcut == index) GlassNavigationBarDefaults.indicatorColor() else Color.Transparent)
                                        .selectable(selectedShortcut == index, role = Role.Tab, onClick = { onShortcutSelected(index) })
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(item.icon, null, Modifier.size(24.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(item.label, style = textStyle, maxLines = 1)
                                }
                            }
                        }
                        var tabStart = 48.dp
                        primaryItems.forEachIndexed { index, item ->
                            val collapsedTabWidth = measuredTabWidths[index]
                            val x = tabStart + (10.dp * referenceScale - tabStart) * progress
                            val y = 3.dp + (54.dp + 41.dp * index) * referenceScale * progress
                            val width = collapsedTabWidth + (202.dp * referenceScale - collapsedTabWidth) * progress
                            val labelWidth = tabWidths[index] - destinationPadding
                            // Match the native slot's centered text before switching renderers.
                            val collapsedTextInset = ((collapsedTabWidth - labelWidth) / 2).coerceAtLeast(0.dp)
                            val textStartInset = collapsedTextInset + (14.dp - collapsedTextInset) * progress
                            val textEndInset = collapsedTextInset + (16.dp * referenceScale - collapsedTextInset) * progress
                            Row(
                                Modifier.offset(x = x, y = y).width(width).height(34.dp + 8.dp * progress)
                                    .clip(GlassShape(21.dp))
                                    .selectable(selected = selectedShortcut == null && selectedTab == index, role = Role.Tab, interactionSource = interactions[index], indication = LocalIndication.current, onClick = { onTabSelected(index) })
                                    .padding(start = textStartInset, end = textEndInset),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.width(36.dp * progress).graphicsLayer { alpha = progress }) {
                                    Icon(item.icon, null, Modifier.size(24.dp), tint = MiuixTheme.colorScheme.onSurface)
                                }
                                Text(item.label, color = MiuixTheme.colorScheme.onSurface, style = textStyle, maxLines = 1)
                            }
                            tabStart += collapsedTabWidth
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
