package com.melox.player.ui.component.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import com.melox.player.ui.theme.LocalOs4Style
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.melox.player.R
import com.melox.player.ui.component.os4.Icon
import com.melox.player.ui.component.os4.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.SelectAll
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SelectionNavigationIconAnimatedContent(
    selectionMode: Boolean,
    modifier: Modifier = Modifier,
    onCloseSelection: () -> Unit,
    defaultNavigationIcon: @Composable () -> Unit = {},
) {
    AnimatedContent(
        targetState = selectionMode,
        modifier = modifier,
        contentAlignment = Alignment.CenterStart,
        label = "TopBarSelectionNavigationIcon",
    ) { selectionActive ->
        if (selectionActive) {
            IconButton(onClick = onCloseSelection) {
                Icon(
                    imageVector = MiuixIcons.Close,
                    contentDescription = stringResource(R.string.close),
                )
            }
        } else {
            defaultNavigationIcon()
        }
    }
}

@Composable
fun SelectionActionsAnimatedContent(
    selectionMode: Boolean,
    modifier: Modifier = Modifier,
    selectionActions: @Composable RowScope.() -> Unit,
    defaultActions: @Composable RowScope.() -> Unit,
) {
    AnimatedContent(
        targetState = selectionMode,
        modifier = modifier,
        contentAlignment = Alignment.CenterEnd,
        label = "TopBarSelectionActions",
    ) { selectionActive ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (LocalOs4Style.current) 8.dp else 0.dp)) {
            if (selectionActive) selectionActions() else defaultActions()
        }
    }
}

@Composable
fun TrackSelectionActions(
    modifier: Modifier = Modifier,
    allSelected: Boolean,
    actionEnabled: Boolean,
    enabled: Boolean = true,
    removeAction: Boolean = false,
    onToggleAll: () -> Unit,
    onAction: () -> Unit,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(if (LocalOs4Style.current) 8.dp else 0.dp)) {
        IconButton(
            onClick = onToggleAll,
            enabled = enabled,
        ) {
            Icon(
                imageVector = MiuixIcons.SelectAll,
                contentDescription = stringResource(
                    if (allSelected) {
                        R.string.selection_deselect_all
                    } else {
                        R.string.selection_select_all
                    },
                ),
                modifier = Modifier.size(24.dp),
                tint = when {
                    !enabled -> MiuixTheme.colorScheme.disabledOnSurface
                    allSelected -> MiuixTheme.colorScheme.primary
                    else -> LocalContentColor.current
                },
            )
        }
        IconButton(
            onClick = onAction,
            enabled = enabled && actionEnabled,
        ) {
            val contentDescription = stringResource(
                if (removeAction) {
                    R.string.selection_remove_from_playlist
                } else {
                    R.string.selection_add_to_playlist
                },
            )
            if (removeAction) {
                Icon(
                    imageVector = MiuixIcons.Delete,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(24.dp),
                    tint = if (enabled && actionEnabled) LocalContentColor.current
                    else MiuixTheme.colorScheme.disabledOnSurface,
                )
            } else {
                Icon(
                    imageVector = MiuixIcons.AddCircle,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(24.dp),
                    tint = if (enabled && actionEnabled) LocalContentColor.current
                    else MiuixTheme.colorScheme.disabledOnSurface,
                )
            }
        }
    }
}

internal fun toggleTrackSelection(
    selectedKeys: Set<String>,
    key: String,
): Set<String> = if (key in selectedKeys) selectedKeys - key else selectedKeys + key

internal fun toggleAllTrackSelection(
    selectedKeys: Set<String>,
    displayedKeys: List<String>,
): Set<String> {
    val availableKeys = displayedKeys.toSet()
    if (availableKeys.isEmpty()) return emptySet()
    return if (selectedKeys.containsAll(availableKeys)) emptySet() else availableKeys
}

internal fun <T> selectedItemsInDisplayedOrder(
    items: List<T>,
    selectedKeys: Set<String>,
    keyOf: (T) -> String,
): List<T> = items.filter { item -> keyOf(item) in selectedKeys }
