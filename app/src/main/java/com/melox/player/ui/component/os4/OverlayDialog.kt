package com.melox.player.ui.component.os4

import androidx.compose.runtime.CompositionLocalProvider
import com.melox.player.ui.component.LocalTopBarScrollContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.layout.DialogSurfaceScope
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.melox.player.ui.theme.LocalOs4Style
import com.melox.player.ui.component.LocalBottomSheetBlurBackdrop
import top.yukonga.miuix.kmp.overlay.OverlayDialog as MiuixOverlayDialog

@Composable
internal fun OverlayDialog(
    show: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    titleColor: Color = DialogDefaults.titleColor(),
    summary: String? = null,
    summaryColor: Color = DialogDefaults.summaryColor(),
    backgroundColor: Color = DialogDefaults.backgroundColor(),
    enableWindowDim: Boolean = true,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    outsideMargin: DpSize = DialogDefaults.outsideMargin,
    insideMargin: DpSize = DialogDefaults.insideMargin,
    defaultWindowInsetsPadding: Boolean = true,
    renderInRootScaffold: Boolean = true,
    maxWidth: Dp = DialogDefaults.MaxWidth,
    largeScreen: Boolean? = null,
    cornerRadius: Dp? = null,
    dimColor: Color = MiuixTheme.colorScheme.windowDimming,
    surface: (@Composable DialogSurfaceScope.() -> Modifier)? = null,
    content: @Composable () -> Unit,
) {
    MiuixOverlayDialog(
        show = show,
        modifier = modifier,
        title = title,
        titleColor = titleColor,
        summary = summary,
        summaryColor = summaryColor,
        backgroundColor = backgroundColor,
        enableWindowDim = enableWindowDim,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        outsideMargin = outsideMargin,
        insideMargin = insideMargin,
        defaultWindowInsetsPadding = defaultWindowInsetsPadding,
        renderInRootScaffold = renderInRootScaffold,
        maxWidth = maxWidth,
        largeScreen = largeScreen,
        cornerRadius = cornerRadius,
        dimColor = dimColor,
        surface = surface ?: if (LocalOs4Style.current) top.yukonga.miuix.kmp.glass.glassDialogSurface(LocalBottomSheetBlurBackdrop.current) else null,
        content = { CompositionLocalProvider(LocalTopBarScrollContent provides null, content = content) },
    )
}
