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

import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu as MiuixMenu

@Composable
internal fun OverlayIconDropdownMenu(
    entry: DropdownEntry,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    maxHeight: Dp? = null,
    dropdownColors: DropdownColors = DropdownDefaults.dropdownColors(),
    renderInRootScaffold: Boolean = true,
    collapseOnSelection: Boolean = true,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    backgroundColor: Color = Color.Unspecified,
    cornerRadius: Dp = IconButtonDefaults.CornerRadius,
    minHeight: Dp = IconButtonDefaults.MinHeight,
    minWidth: Dp = IconButtonDefaults.MinWidth,
    content: @Composable () -> Unit,
) {
    if (LocalOs4Style.current) {
        GlassMenuHost(listOf(entry), modifier, enabled, renderInRootScaffold, collapseOnSelection, onExpandedChange, content, maxHeight = maxHeight)
    } else {
        MiuixMenu(entry = entry, modifier = modifier, enabled = enabled, maxHeight = maxHeight, dropdownColors = dropdownColors, renderInRootScaffold = renderInRootScaffold, collapseOnSelection = collapseOnSelection, onExpandedChange = onExpandedChange, backgroundColor = backgroundColor, cornerRadius = cornerRadius, minHeight = minHeight, minWidth = minWidth, content = content)
    }
}

@Composable
internal fun OverlayIconDropdownMenu(
    entries: List<DropdownEntry>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    maxHeight: Dp? = null,
    dropdownColors: DropdownColors = DropdownDefaults.dropdownColors(),
    renderInRootScaffold: Boolean = true,
    collapseOnSelection: Boolean = entries.size <= 1,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    backgroundColor: Color = Color.Unspecified,
    cornerRadius: Dp = IconButtonDefaults.CornerRadius,
    minHeight: Dp = IconButtonDefaults.MinHeight,
    minWidth: Dp = IconButtonDefaults.MinWidth,
    content: @Composable () -> Unit,
) {
    if (LocalOs4Style.current) {
        GlassMenuHost(entries, modifier, enabled, renderInRootScaffold, collapseOnSelection, onExpandedChange, content, maxHeight = maxHeight)
    } else {
        MiuixMenu(entries = entries, modifier = modifier, enabled = enabled, maxHeight = maxHeight, dropdownColors = dropdownColors, renderInRootScaffold = renderInRootScaffold, collapseOnSelection = collapseOnSelection, onExpandedChange = onExpandedChange, backgroundColor = backgroundColor, cornerRadius = cornerRadius, minHeight = minHeight, minWidth = minWidth, content = content)
    }
}

