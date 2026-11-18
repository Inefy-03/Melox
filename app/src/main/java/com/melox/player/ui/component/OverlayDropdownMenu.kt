package com.melox.player.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.DropdownEntry

@Composable
internal fun OverlayDropdownMenu(
    entry: DropdownEntry,
    title: String,
    modifier: Modifier = Modifier,
    startAction: (@Composable () -> Unit)? = null,
) {
    OverlayDropdownPreference(
        entry = entry, title = title, modifier = modifier,
        startAction = startAction, showValue = false,
    )
}
