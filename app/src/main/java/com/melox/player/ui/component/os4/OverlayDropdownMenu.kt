package com.melox.player.ui.component.os4

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.menu.OverlayDropdownMenu as MiuixDropdownMenu
import com.melox.player.ui.theme.LocalOs4Style

@Composable
internal fun OverlayDropdownMenu(
    entry: DropdownEntry,
    title: String,
    modifier: Modifier = Modifier,
    startAction: (@Composable () -> Unit)? = null,
) {
    if (LocalOs4Style.current) {
        OverlayDropdownPreference(
            entry = entry, title = title, modifier = modifier,
            startAction = startAction, showValue = false,
        )
    } else {
        MiuixDropdownMenu(entry = entry, title = title, modifier = modifier, startAction = startAction)
    }
}
