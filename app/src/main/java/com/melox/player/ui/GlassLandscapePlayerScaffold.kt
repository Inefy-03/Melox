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
import com.melox.player.ui.component.Icon
import com.melox.player.ui.component.GlassTopBarIcons
import com.melox.player.ui.component.SettingsOutline
import com.melox.player.ui.component.glassIcon
import com.melox.player.ui.component.glassSidebarIcon
import com.melox.player.ui.component.WideGlassSearchHost
import com.melox.player.ui.component.LocalWideGlassSearchHost
import com.melox.player.ui.component.dismissWideSearchOnOutsideTap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.upstream.glass.GlassNavigationBarDefaults as UpstreamGlassNavigationBarDefaults
import top.yukonga.miuix.kmp.upstream.glass.GlassIconButton
import top.yukonga.miuix.kmp.upstream.glass.glassPanel
import top.yukonga.miuix.kmp.upstream.material.GlassShape
import top.yukonga.miuix.kmp.upstream.material.GlassShadows
import top.yukonga.miuix.kmp.upstream.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.upstream.blur.layerBackdrop as glassLayerBackdrop
import top.yukonga.miuix.kmp.upstream.blur.rememberLayerBackdrop as rememberGlassLayerBackdrop
import top.yukonga.miuix.kmp.glass.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val SidebarItemIconSize = 28.dp
private val SidebarToggleIconSize = 24.dp

@Composable
private fun SidebarItemIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector,
        null,
        modifier.size(SidebarItemIconSize),
        tint = MiuixTheme.colorScheme.onSurface,
    )
}

private val TopNavigationIcon = ImageVector.Builder(
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

private val SidebarIcon = ImageVector.Builder(
    name = "SidebarToggle", defaultWidth = 24.dp, defaultHeight = 24.dp,
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

internal val SidebarNavigationReboundMotion = spring<Dp>(stiffness = 200f, dampingRatio = 0.8f)
internal val SidebarNavigationSidebarMotion = tween<Dp>(
    durationMillis = 300,
    easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f),
)

internal val SidebarHeightExpandMotion = spring<Dp>(stiffness = 220f, dampingRatio = 0.83f)
internal val SidebarHeightCollapseMotion = spring<Dp>(stiffness = 462.25f, dampingRatio = 0.97f)
internal fun sidebarHeightMotion(expanded: Boolean, distance: Dp): SpringSpec<Dp> {
    val response = if (expanded) SidebarHeightExpandMotion else SidebarHeightCollapseMotion
    return spring(
        stiffness = response.stiffness,
        dampingRatio = response.dampingRatio,
        visibilityThreshold = (distance * 0.001f).coerceAtLeast(0.1.dp),
    )
}

internal val SidebarNavigationIconEnterMotion = tween<Float>(200, easing = SidebarNavigationSidebarMotion.easing)
internal val SidebarNavigationIconExitMotion = tween<Float>(120, easing = SidebarNavigationSidebarMotion.easing)
internal val SidebarNavigationLabelEnterMotion = tween<Float>(160, delayMillis = 64, easing = SidebarNavigationSidebarMotion.easing)
internal val SidebarNavigationLabelExitMotion = tween<Float>(100, delayMillis = 64, easing = LinearEasing)

internal fun sidebarItemMotion(index: Int, expanded: Boolean): SpringSpec<Float> {
    // Fitted to the recording's row centers, with one response for both axes.
    val frequency = (if (expanded) 26.75f else 21.5f) / (1f + 0.03f * index)
    return spring(stiffness = frequency * frequency, dampingRatio = 0.97f, visibilityThreshold = 0.001f)
}

internal class LandscapeNavigation(
    val labels: List<String>,
    val titleAlpha: () -> Float,
    val leadingContentMaxWidth: Dp,
)
internal val LocalLandscapeNavigation = staticCompositionLocalOf<LandscapeNavigation?> { null }

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GlassLandscapePlayerScaffold(
    navigationItems: List<NavigationItem>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    showNavigation: Boolean,
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
        label = "sidebarNavigationMorph",
    )
    val blurActive = blurEnabled && isRuntimeShaderSupported()
    val surfaceColor = MiuixTheme.colorScheme.surface
    val glassBackdrop = rememberGlassLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val searchHost = remember { WideGlassSearchHost() }
    var searchCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // Align 28 dp row icons with the 44 dp toggle centered at x = 38 dp.
    val itemContentPadding = 12.dp
    val itemIconSlotWidth = SidebarItemIconSize + 12.dp
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
        animationSpec = if (expanded) SidebarNavigationIconEnterMotion else SidebarNavigationIconExitMotion,
        label = "sidebarNavigationIconOpacity",
    )
    val shortcutLabelAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = if (expanded) SidebarNavigationLabelEnterMotion else SidebarNavigationLabelExitMotion,
        label = "sidebarNavigationLabelOpacity",
    )
    val itemProgress = List(sourceWidths.size) { index ->
        animateFloatAsState(
            targetValue = if (expanded) 1f else 0f,
            animationSpec = sidebarItemMotion(index, expanded),
            label = "sidebarNavigationItem$index",
        ).value.coerceIn(0f, 1f)
    }
    val sidebarScrollState = rememberScrollState()
    val leadingActionWidth = GlassTopAppBarDefaults.ButtonSize + 4.dp
    val capsuleWidth = leadingActionWidth + 11.dp + tabWidths.fold(0.dp) { sum, width -> sum + width }
    val interactions = remember(navigationItems.size) { List(navigationItems.size) { MutableInteractionSource() } }
    FixedPageBackgroundHost(Modifier.fillMaxSize(), refreshSignal = refreshSignal) {
        BoxWithConstraints(Modifier.fillMaxSize()
            .onGloballyPositioned { searchCoordinates = it }
            .dismissWideSearchOnOutsideTap({ searchHost.expandedPage }) { searchCoordinates }) {
            val horizontalInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()
            val direction = androidx.compose.ui.platform.LocalLayoutDirection.current
            val viewportWidth = maxWidth - horizontalInsets.calculateStartPadding(direction) -
                horizontalInsets.calculateEndPadding(direction)
            val viewportHeight = maxHeight
            val collapsedWidth = capsuleWidth.coerceAtMost((viewportWidth - 162.dp).coerceAtLeast(192.dp))
            val collapsedStart = (viewportWidth - collapsedWidth) / 2
            CompositionLocalProvider(
                LocalWideGlassSearchHost provides searchHost,
                LocalFloatingNavigationRailInset provides railInset,
                LocalLandscapeNavigation provides LandscapeNavigation(
                    navigationItems.map { it.label }, titleAlpha = { progress },
                    leadingContentMaxWidth = (collapsedStart - GlassTopAppBarDefaults.HorizontalPadding * 2).coerceAtLeast(0.dp),
                ),
            ) {
                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        Box(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)).padding(start = railInset).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Box(Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                                miniPlayer(
                                    MiniPlayerChrome(
                                        style = BottomBarStyle.GLASS,
                                        backdrop = null,
                                        blurActive = blurActive,
                                        liquidGlassActive = false,
                                        isDark = isDark,
                                        useIosLikeLayout = true,
                                        glassBackdrop = glassBackdrop,
                                    ),
                                )
                            }
                        }
                    },
                ) { padding ->
                    // Keep the shared navigation surface and player outside their own capture.
                    Box(
                        Modifier.fillMaxSize()
                            .glassLayerBackdrop(glassBackdrop),
                    ) { content(padding) }
                }
                Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
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
                            animationSpec = sidebarHeightMotion(expanded, expandedHeight - collapsedHeight),
                            label = "sidebarBottomSettle",
                        )
                        // Position continues settling after the height/width morph reaches its endpoint.
                        val panelStart by animateDpAsState(
                            targetValue = if (expanded) sidebarStart else collapsedStart,
                            animationSpec = if (expanded) SidebarNavigationSidebarMotion else SidebarNavigationReboundMotion,
                            label = "sidebarNavigationHorizontalSettle",
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
                                backdrop = glassBackdrop.takeIf { blurActive },
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
                                        Icon(SidebarIcon, stringResource(R.string.navigation_expand), Modifier.size(SidebarToggleIconSize))
                                    }
                                },
                            )
                        } else Box(
                            Modifier.offset(x = panelStart, y = panelTop)
                                .size(panelWidth, panelHeight)
                                .glassPanel(
                                    backdrop = glassBackdrop.takeIf { blurActive }, shape = GlassShape(radius),
                                    material = UpstreamGlassNavigationBarDefaults.material(), stroke = UpstreamGlassNavigationBarDefaults.stroke(),
                                    shadow = GlassShadows.Float, shading = false,
                                    fallback = Modifier.clip(GlassShape(radius)).background(MiuixTheme.colorScheme.surfaceContainer),
                                ).clip(GlassShape(radius)).selectableGroup(),
                        ) {
                            val lastPrimaryRowBottom = 3.dp +
                                (61.dp + 46.dp * primaryItems.lastIndex) + 42.dp
                            val dividerGap = 12.dp
                            val dividerY = lastPrimaryRowBottom + dividerGap
                            val shortcutsTop = dividerY + DividerDefaults.Thickness + dividerGap
                            val sidebarSettled = expanded && itemProgress.all { it == 1f }
                            val listTop = if (sidebarSettled) 64.dp else 0.dp
                            val listBottom = maxOf(lastPrimaryRowBottom,
                                shortcutsTop + 46.dp * shortcutItems.size - 4.dp) + 12.dp
                            Box(
                                Modifier.offset(y = listTop).width(panelWidth)
                                    .height((panelHeight - listTop).coerceAtLeast(0.dp))
                                    .then(if (sidebarSettled) Modifier.verticalScroll(sidebarScrollState) else Modifier),
                            ) {
                                Box(
                                    Modifier.width(panelWidth)
                                        .height(if (sidebarSettled) listBottom - listTop else panelHeight)
                                        .offset(y = -listTop)
                                        .graphicsLayer {
                                            translationY = if (sidebarSettled) 0f else -sidebarScrollState.value * progress
                                        },
                                ) {
                                    val slot = selectedTab.coerceIn(0, primaryItems.lastIndex).toFloat()
                                    val leading = slot.toInt()
                                    val fraction = slot - leading
                                    val widths = measuredTabWidths
                                    val selectedStart = widths.take(leading).fold(leadingActionWidth + 8.dp) { sum, width -> sum + width } + widths[leading] * fraction
                                    val selectedWidth = widths[leading]
                                    val selectionProgress = itemProgress[selectedShortcut?.let { primaryItems.size + it } ?: leading]
                                    val selectionTargetY = selectedShortcut?.let { shortcutsTop + 46.dp * it }
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
                                    Column(
                                        Modifier.offset(x = itemHorizontalMargin, y = shortcutsTop)
                                            .width(itemWidth)
                                            .wrapContentHeight(Alignment.Top, unbounded = true),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        shortcutItems.forEachIndexed { index, item ->
                                            val globalIndex = primaryItems.size + index
                                            val rowProgress = itemProgress[globalIndex]
                                            val labelWidth = sourceWidths[globalIndex] - destinationPadding
                                            val textInset = ((sourceWidths[globalIndex] * widthRatio - labelWidth) / 2).coerceAtLeast(0.dp)
                                            val sourceX = sourceStarts[globalIndex] + textInset - itemContentPadding - itemIconSlotWidth
                                            val rowX = sourceX + (itemHorizontalMargin - sourceX) * rowProgress
                                            val targetY = shortcutsTop + 46.dp * index
                                            val rowY = 3.dp + (targetY - 3.dp) * rowProgress
                                            Row(
                                                Modifier.fillMaxWidth().height(42.dp)
                                                    .graphicsLayer {
                                                        translationX = (rowX - itemHorizontalMargin).toPx()
                                                        translationY = if (sidebarSettled) 0f else
                                                            (rowY - shortcutsTop - 46.dp * index).toPx()
                                                    }
                                                    .clip(GlassShape(21.dp))
                                                    .selectable(expanded && selectedShortcut == index, role = Role.Tab, onClick = { onShortcutSelected(index) })
                                                    .padding(horizontal = itemContentPadding),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                SidebarItemIcon(glassSidebarIcon(item.icon), Modifier.graphicsLayer { alpha = iconAlpha })
                                                Spacer(Modifier.width(12.dp))
                                                Text(item.label, modifier = Modifier.graphicsLayer { alpha = shortcutLabelAlpha }, style = textStyle, maxLines = 1)
                                            }
                                        }
                                    }
                                    primaryItems.forEachIndexed { index, item ->
                                        val rowProgress = itemProgress[index]
                                        val tabStart = sourceStarts[index]
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
                                                SidebarItemIcon(glassIcon(item.icon))
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
                            Box(Modifier.offset(x = 8.dp + 8.dp * progress, y = 12.dp * progress).size(GlassTopAppBarDefaults.ButtonSize), contentAlignment = Alignment.Center) {
                                GlassIconButton(onClick = { expanded = !expanded }, size = 38.dp + 6.dp * progress, surfaceAlpha = 0f, shadow = null) {
                                    val description = stringResource(if (expanded) R.string.navigation_collapse else R.string.navigation_expand)
                                    Box(Modifier.size(SidebarToggleIconSize).semantics { contentDescription = description }) {
                                        Icon(TopNavigationIcon, null, Modifier.fillMaxSize().graphicsLayer { alpha = progress })
                                        Icon(SidebarIcon, null, Modifier.fillMaxSize().graphicsLayer { alpha = 1f - progress })
                                    }
                                }
                            }
                            Box(Modifier.offset(x = panelWidth - 16.dp - GlassTopAppBarDefaults.ButtonSize, y = 12.dp).graphicsLayer { alpha = shortcutLabelAlpha }) {
                                GlassIconButton(onClick = { onTabSelected(3) }, surfaceAlpha = 0f, shadow = null) {
                                    Icon(GlassTopBarIcons.SettingsOutline, navigationItems[3].label, Modifier.size(28.dp))
                                }
                            }
                        }
                    }
                }

            }
        }
    }
}
