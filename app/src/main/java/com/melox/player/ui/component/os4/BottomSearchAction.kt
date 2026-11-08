package com.melox.player.ui.component.os4

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.melox.player.ui.theme.LocalOs4Style

internal class BottomSearchAction(
    val owner: Any,
    val active: State<Boolean>,
    val expanded: State<Boolean>,
    val searchOpen: State<Boolean>,
    val label: State<String>,
    val enabled: State<Boolean>,
    val onClick: State<() -> Unit>,
)

internal val LocalBottomSearchActions = staticCompositionLocalOf<SnapshotStateMap<Any, BottomSearchAction>?> { null }
internal val LocalBottomSearchOwner = staticCompositionLocalOf<Any?> { null }

@Composable
internal fun publishBottomSearchAction(active: Boolean, expanded: Boolean, searchOpen: Boolean, label: String, enabled: Boolean, onClick: () -> Unit): Boolean {
    val registry = LocalBottomSearchActions.current
    val owner = LocalBottomSearchOwner.current
    if (!LocalOs4Style.current || registry == null || owner == null) return false
    val key = remember { Any() }
    val activeState = rememberUpdatedState(active)
    val expandedState = rememberUpdatedState(expanded)
    val searchOpenState = rememberUpdatedState(searchOpen)
    val labelState = rememberUpdatedState(label)
    val enabledState = rememberUpdatedState(enabled)
    val onClickState = rememberUpdatedState(onClick)
    val action = remember(owner) { BottomSearchAction(owner, activeState, expandedState, searchOpenState, labelState, enabledState, onClickState) }
    DisposableEffect(registry, key, action) {
        registry[key] = action
        onDispose { registry.remove(key) }
    }
    return true
}
