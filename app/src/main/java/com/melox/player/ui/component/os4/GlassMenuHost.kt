package com.melox.player.ui.component.os4

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.glass.*
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.PopupLayout
import com.melox.player.ui.component.LocalBottomSheetBlurBackdrop

@Composable
internal fun GlassMenuHost(
    entries: List<DropdownEntry>,
    modifier: Modifier,
    enabled: Boolean,
    root: Boolean,
    collapseOnSelection: Boolean,
    onExpandedChange: ((Boolean) -> Unit)?,
    content: @Composable () -> Unit,
    maxHeight: Dp? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val mounted = remember { mutableStateOf(false) }
    val anchor = rememberGlassPopupAnchor()
    val backdrop = LocalBottomSheetBlurBackdrop.current
    fun setExpanded(value: Boolean) {
        expanded = value
        if (value) mounted.value = true
        onExpandedChange?.invoke(value)
    }
    IconButton(
        onClick = { setExpanded(!expanded) },
        enabled = enabled,
        minHeight = GlassTopAppBarDefaults.ButtonSize,
        minWidth = GlassTopAppBarDefaults.ButtonSize,
        modifier = modifier.glassPopupAnchor(anchor, 22.dp),
        content = { Box(Modifier.glassPopupAnchorContent(anchor)) { content() } },
    )
    if (!mounted.value) return
    PopupLayout(
        visible = mounted, enableWindowDim = false, enableBackHandler = false,
        enterTransition = EnterTransition.None, exitTransition = ExitTransition.None,
        renderInRootScaffold = root,
    ) {
        // PopupLayout mounts asynchronously: initialize the morph only after its content exists.
        var menuShown by remember { mutableStateOf(false) }
        LaunchedEffect(expanded) { menuShown = expanded }
        var hostOrigin by remember { mutableStateOf(Offset.Zero) }
        Box(Modifier.fillMaxSize().onGloballyPositioned { hostOrigin = it.positionInRoot() }) {
            GlassIconDropdownMenu(
                show = menuShown, onDismissRequest = { setExpanded(false) },
                anchor = anchor, anchorContent = content, entries = entries,
                backdrop = backdrop, collapseOnSelection = collapseOnSelection,
                coordinateOrigin = hostOrigin,
                sizing = GlassPopupSizing(maxHeight = maxHeight ?: GlassPopupDefaults.MaxHeight),
                onDismissFinished = { mounted.value = false },
            )
        }
    }
}
