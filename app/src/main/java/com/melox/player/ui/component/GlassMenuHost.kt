package com.melox.player.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassIconDropdownMenu
import top.yukonga.miuix.kmp.glass.GlassPopupDefaults
import top.yukonga.miuix.kmp.glass.GlassPopupSizing
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.glass.glassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchorContent
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import top.yukonga.miuix.kmp.theme.LocalContentColor

internal val LocalGlassMenus = staticCompositionLocalOf<SnapshotStateMap<Any, @Composable BoxScope.() -> Unit>> {
    error("Glass menus require the root Scaffold host")
}

@Composable
internal fun GlassMenuLayer(menus: Map<Any, @Composable BoxScope.() -> Unit>) {
    if (menus.isEmpty()) return
    Box(Modifier.fillMaxSize()) {
        menus.forEach { (id, menu) -> key(id) { menu() } }
    }
}

@Composable
internal fun GlassMenuHost(
    entries: List<DropdownEntry>,
    modifier: Modifier,
    enabled: Boolean,
    collapseOnSelection: Boolean,
    onExpandedChange: ((Boolean) -> Unit)?,
    content: @Composable () -> Unit,
    maxHeight: Dp? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val anchor = rememberGlassPopupAnchor()
    val backdrop = LocalBottomSheetBlurBackdrop.current
    val menus = LocalGlassMenus.current
    val topBarIcon = LocalTopBarButtonHaptics.current
    val iconColor = LocalContentColor.current
    val haptics = LocalHapticFeedback.current
    fun setExpanded(value: Boolean) {
        expanded = value
        onExpandedChange?.invoke(value)
    }

    val buttonSize = GlassTopAppBarDefaults.ButtonSize
    if (enabled) {
        GlassIconButton(
            onClick = {
                if (topBarIcon) haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                setExpanded(!expanded)
            },
            modifier = modifier.glassPopupAnchor(anchor, cornerRadius = buttonSize / 2, floating = true),
            backdrop = LocalPageSurfaceBackdrop.current.takeUnless { topBarIcon },
        ) {
            Box(Modifier.glassPopupAnchorContent(anchor)) { content() }
        }
    } else {
        IconButton(
            onClick = {},
            modifier = modifier,
            enabled = false,
            minHeight = buttonSize,
            minWidth = buttonSize,
            content = content,
        )
    }

    val latestMenu by rememberUpdatedState<@Composable BoxScope.() -> Unit> {
        GlassIconDropdownMenu(
            show = expanded,
            onDismissRequest = { setExpanded(false) },
            anchor = anchor,
            anchorContent = {
                CompositionLocalProvider(
                    LocalTopBarButtonHaptics provides topBarIcon,
                    LocalContentColor provides iconColor,
                    content = content,
                )
            },
            entries = entries,
            backdrop = backdrop,
            collapseOnSelection = collapseOnSelection,
            sizing = GlassPopupSizing(maxHeight = maxHeight ?: GlassPopupDefaults.MaxHeight),
        )
    }
    DisposableEffect(menus, anchor) {
        menus[anchor] = { latestMenu() }
        onDispose { menus.remove(anchor) }
    }
}
