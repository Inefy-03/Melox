package com.melox.player.ui.component.os4

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import androidx.compose.foundation.layout.*
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.glass.*
import top.yukonga.miuix.kmp.utils.*
import com.melox.player.ui.theme.LocalOs4Style
import com.melox.player.ui.component.LocalBottomSheetBlurBackdrop

import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference as MiuixDropdown

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
    if (LocalOs4Style.current) {
        GlassDropdownPreference(
            title = title, modifier = modifier, backdrop = LocalBottomSheetBlurBackdrop.current,
            enabled = enabled, showValue = showValue, renderInRootScaffold = renderInRootScaffold,
            titleColor = titleColor, summary = summary, summaryColor = summaryColor,
            startAction = startAction, bottomAction = bottomAction, insideMargin = insideMargin,
            onExpandedChange = onExpandedChange,
            sizing = GlassPopupSizing(maxHeight = maxHeight ?: GlassPopupDefaults.MaxHeight),
            items = items, selectedIndex = selectedIndex, onSelectedIndexChange = { onSelectedIndexChange?.invoke(it) },
        )
    } else {
        MiuixDropdown(items = items, selectedIndex = selectedIndex, title = title, modifier = modifier, titleColor = titleColor, summary = summary, summaryColor = summaryColor, dropdownColors = dropdownColors, startAction = startAction, bottomAction = bottomAction, insideMargin = insideMargin, maxHeight = maxHeight, enabled = enabled, showValue = showValue, renderInRootScaffold = renderInRootScaffold, onExpandedChange = onExpandedChange, onSelectedIndexChange = onSelectedIndexChange)
    }
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
    if (LocalOs4Style.current) {
        GlassDropdownPreference(
            title = title, modifier = modifier, backdrop = LocalBottomSheetBlurBackdrop.current,
            enabled = enabled, showValue = showValue, renderInRootScaffold = renderInRootScaffold,
            titleColor = titleColor, summary = summary, summaryColor = summaryColor,
            startAction = startAction, bottomAction = bottomAction, insideMargin = insideMargin,
            onExpandedChange = onExpandedChange,
            sizing = GlassPopupSizing(maxHeight = maxHeight ?: GlassPopupDefaults.MaxHeight),
            items = entry.items.map { it.text },
            itemEnabled = entry.items.map { entry.enabled && it.enabled },
            selectedIndex = entry.items.indexOfFirst { it.selected },
            onSelectedIndexChange = { entry.items[it].onClick?.invoke() },
        )
    } else {
        MiuixDropdown(entry = entry, title = title, modifier = modifier, titleColor = titleColor, summary = summary, summaryColor = summaryColor, dropdownColors = dropdownColors, startAction = startAction, bottomAction = bottomAction, insideMargin = insideMargin, maxHeight = maxHeight, enabled = enabled, showValue = showValue, renderInRootScaffold = renderInRootScaffold, collapseOnSelection = collapseOnSelection, onExpandedChange = onExpandedChange)
    }
}

@Composable
internal fun OverlayDropdownPreference(
    entries: List<DropdownEntry>,
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
    collapseOnSelection: Boolean = entries.size <= 1,
    onExpandedChange: ((Boolean) -> Unit)? = null,
) {
    MiuixDropdown(entries = entries, title = title, modifier = modifier, titleColor = titleColor, summary = summary, summaryColor = summaryColor, dropdownColors = dropdownColors, startAction = startAction, bottomAction = bottomAction, insideMargin = insideMargin, maxHeight = maxHeight, enabled = enabled, showValue = showValue, renderInRootScaffold = renderInRootScaffold, collapseOnSelection = collapseOnSelection, onExpandedChange = onExpandedChange)
}

