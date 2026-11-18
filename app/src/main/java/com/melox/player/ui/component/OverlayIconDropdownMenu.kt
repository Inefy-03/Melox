package com.melox.player.ui.component

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import androidx.compose.foundation.layout.*
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.glass.*
import top.yukonga.miuix.kmp.utils.*


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
    GlassMenuHost(listOf(entry), modifier, enabled, renderInRootScaffold, collapseOnSelection, onExpandedChange, content, maxHeight = maxHeight)
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
    GlassMenuHost(entries, modifier, enabled, renderInRootScaffold, collapseOnSelection, onExpandedChange, content, maxHeight = maxHeight)
}

