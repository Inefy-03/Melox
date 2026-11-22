package com.melox.player.ui.component

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.*
import androidx.compose.foundation.layout.*
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.glass.GlassDropdownPreference
import top.yukonga.miuix.kmp.glass.GlassPopupDefaults
import top.yukonga.miuix.kmp.glass.GlassPopupSizing
import top.yukonga.miuix.kmp.utils.*


@Composable
internal fun OverlayDropdownPreference(
    items: List<String>,
    selectedIndex: Int,
    title: String,
    modifier: Modifier = Modifier,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    dropdownColors: DropdownColors = DropdownDefaults.dropdownColors(),
    startAction: @Composable (() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    maxHeight: Dp? = null,
    enabled: Boolean = true,
    showValue: Boolean = true,
    renderInRootScaffold: Boolean = true,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    onSelectedIndexChange: ((Int) -> Unit)? = null,
) {
    GlassDropdownPreference(
        title = title, modifier = modifier, backdrop = LocalBottomSheetBlurBackdrop.current,
        enabled = enabled, showValue = showValue, renderInRootScaffold = renderInRootScaffold,
        titleColor = titleColor, summary = summary, summaryColor = summaryColor,
        startAction = startAction, bottomAction = bottomAction, insideMargin = insideMargin,
        onExpandedChange = onExpandedChange,
        sizing = GlassPopupSizing(maxHeight = maxHeight ?: GlassPopupDefaults.MaxHeight),
        items = items, selectedIndex = selectedIndex, onSelectedIndexChange = { onSelectedIndexChange?.invoke(it) },
    )
}

@Composable
internal fun OverlayDropdownPreference(
    entry: DropdownEntry,
    title: String,
    modifier: Modifier = Modifier,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    dropdownColors: DropdownColors = DropdownDefaults.dropdownColors(),
    startAction: @Composable (() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    maxHeight: Dp? = null,
    enabled: Boolean = true,
    showValue: Boolean = true,
    renderInRootScaffold: Boolean = true,
    collapseOnSelection: Boolean = true,
    onExpandedChange: ((Boolean) -> Unit)? = null,
) {
    val availableItems = entry.items.filter { it.enabled || it.selected }
    GlassDropdownPreference(
        title = title, modifier = modifier, backdrop = LocalBottomSheetBlurBackdrop.current,
        enabled = enabled && entry.enabled, showValue = showValue, renderInRootScaffold = renderInRootScaffold,
        titleColor = titleColor, summary = summary, summaryColor = summaryColor,
        startAction = startAction, bottomAction = bottomAction, insideMargin = insideMargin,
        onExpandedChange = onExpandedChange,
        sizing = GlassPopupSizing(maxHeight = maxHeight ?: GlassPopupDefaults.MaxHeight),
        items = availableItems.map { it.text },
        selectedIndex = availableItems.indexOfFirst { it.selected },
        onSelectedIndexChange = { availableItems[it].takeIf { item -> item.enabled }?.onClick?.invoke() },
    )
}

