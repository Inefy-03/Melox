package com.melox.player.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.melox.player.ui.glass.GlassDropdownPreference
import top.yukonga.miuix.kmp.basic.DropdownEntry

@Composable
internal fun OverlayDropdownMenu(
    entry: DropdownEntry,
    title: String,
    itemIcons: List<ImageVector>,
    modifier: Modifier = Modifier,
    startAction: (@Composable () -> Unit)? = null,
) {
    val availableIndices = entry.items.indices.filter { entry.items[it].enabled || entry.items[it].selected }
    val availableItems = availableIndices.map(entry.items::get)
    GlassDropdownPreference(
        title = title,
        modifier = modifier,
        backdrop = LocalBottomSheetBlurBackdrop.current,
        enabled = entry.enabled,
        showValue = false,
        startAction = startAction,
        items = availableItems.map { it.text },
        itemIcons = availableIndices.map(itemIcons::getOrNull),
        itemEnabled = availableItems.map { it.enabled },
        selectedIndex = availableItems.indexOfFirst { it.selected },
        onSelectedIndexChange = { availableItems[it].onClick?.invoke() },
    )
}
