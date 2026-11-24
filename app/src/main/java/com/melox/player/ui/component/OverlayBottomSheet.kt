package com.melox.player.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.glass.GlassBottomSheet
import top.yukonga.miuix.kmp.glass.GlassBottomSheetDefaults
import top.yukonga.miuix.kmp.glass.GlassIconButton

@Composable
internal fun OverlayBottomSheet(
    show: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    startAction: @Composable (() -> Unit)? = null,
    endAction: @Composable (() -> Unit)? = null,
    backgroundColor: Color = GlassBottomSheetDefaults.containerColor(),
    enableWindowDim: Boolean = true,
    cornerRadius: Dp = BottomSheetDefaults.cornerRadius,
    sheetMaxWidth: Dp = BottomSheetDefaults.maxWidth,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    outsideMargin: DpSize = BottomSheetDefaults.outsideMargin,
    insideMargin: DpSize = GlassBottomSheetDefaults.InsideMargin,
    defaultWindowInsetsPadding: Boolean = true,
    dragHandleColor: Color = BottomSheetDefaults.dragHandleColor(),
    allowDismiss: Boolean = true,
    enableNestedScroll: Boolean = true,
    renderInRootScaffold: Boolean = true,
    dimColor: Color = GlassBottomSheetDefaults.ScrimColor,
    headerBottomContent: @Composable () -> Unit = {},
    scrollOffset: (() -> Float)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    GlassBottomSheet(
        show = show,
        backdrop = LocalBottomSheetBlurBackdrop.current,
        modifier = modifier,
        title = title,
        startAction = startAction,
        endAction = endAction,
        headerBottomContent = headerBottomContent,
        containerColor = backgroundColor,
        enableWindowDim = enableWindowDim,
        cornerRadius = cornerRadius,
        sheetMaxWidth = sheetMaxWidth,
        onDismissRequest = onDismissRequest ?: {},
        onDismissFinished = onDismissFinished,
        outsideMargin = outsideMargin,
        insideMargin = insideMargin,
        defaultWindowInsetsPadding = defaultWindowInsetsPadding,
        dragHandleColor = dragHandleColor,
        allowDismiss = allowDismiss,
        enableNestedScroll = enableNestedScroll,
        renderInRootScaffold = renderInRootScaffold,
        dimColor = dimColor,
        scrollOffset = scrollOffset,
        content = { headerPadding ->
            CompositionLocalProvider(LocalTopBarScrollContent provides null) {
                content(headerPadding)
            }
        },
    )
}

@Composable
internal fun BottomSheetHeaderButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (enabled) {
        GlassIconButton(onClick = onClick, modifier = modifier, content = content)
    } else {
        IconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = false,
            minWidth = GlassBottomSheetDefaults.ButtonSize,
            minHeight = GlassBottomSheetDefaults.ButtonSize,
            content = content,
        )
    }
}
