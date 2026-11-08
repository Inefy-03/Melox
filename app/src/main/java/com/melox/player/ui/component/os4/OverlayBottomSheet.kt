package com.melox.player.ui.component.os4

import com.melox.player.ui.component.LocalTopBarScrollContent
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.glass.*
import com.melox.player.ui.component.LocalGlassTopBackdrop
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.layout.BottomSheetSurfaceScope
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.melox.player.ui.theme.LocalOs4Style
import com.melox.player.ui.component.LocalBottomSheetBlurBackdrop
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet as MiuixOverlayBottomSheet

@Composable
internal fun OverlayBottomSheet(
    show: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    startAction: @Composable (() -> Unit)? = null,
    endAction: @Composable (() -> Unit)? = null,
    backgroundColor: Color = BottomSheetDefaults.backgroundColor(),
    enableWindowDim: Boolean = true,
    cornerRadius: Dp = BottomSheetDefaults.cornerRadius,
    sheetMaxWidth: Dp = BottomSheetDefaults.maxWidth,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    outsideMargin: DpSize = BottomSheetDefaults.outsideMargin,
    insideMargin: DpSize = BottomSheetDefaults.insideMargin,
    defaultWindowInsetsPadding: Boolean = true,
    dragHandleColor: Color = BottomSheetDefaults.dragHandleColor(),
    allowDismiss: Boolean = true,
    enableNestedScroll: Boolean = true,
    renderInRootScaffold: Boolean = true,
    dimColor: Color = MiuixTheme.colorScheme.windowDimming,
    surface: (@Composable BottomSheetSurfaceScope.() -> Modifier)? = null,
    headerScrolled: (() -> Boolean)? = null,
    content: @Composable () -> Unit,
) {
    val os4 = LocalOs4Style.current
    var scrollOffset by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(show) { if (!show) scrollOffset = 0f }
    val scrollObserver = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                scrollOffset = (scrollOffset - consumed.y).coerceAtLeast(0f)
                return Offset.Zero
            }
        }
    }
    val scrolled = headerScrolled?.invoke() ?: (scrollOffset > 0f)
    MiuixOverlayBottomSheet(
        show = show,
        modifier = modifier,
        title = title,
        startAction = startAction?.let { action ->
            { if (os4) SheetHeaderButtons(scrolled, action) else action() }
        },
        endAction = endAction?.let { action ->
            { if (os4) SheetHeaderButtons(scrolled, action) else action() }
        },
        backgroundColor = backgroundColor,
        enableWindowDim = enableWindowDim,
        cornerRadius = cornerRadius,
        sheetMaxWidth = sheetMaxWidth,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        outsideMargin = outsideMargin,
        insideMargin = insideMargin,
        defaultWindowInsetsPadding = defaultWindowInsetsPadding,
        dragHandleColor = dragHandleColor,
        allowDismiss = allowDismiss,
        enableNestedScroll = enableNestedScroll,
        renderInRootScaffold = renderInRootScaffold,
        dimColor = dimColor,
        surface = surface ?: if (LocalOs4Style.current) top.yukonga.miuix.kmp.glass.glassBottomSheetSurface(LocalBottomSheetBlurBackdrop.current) else null,
        content = {
            CompositionLocalProvider(LocalTopBarScrollContent provides null) {
                if (os4) Column(Modifier.nestedScroll(scrollObserver)) { content() } else content()
            }
        },
    )
}

@Composable
private fun SheetHeaderButtons(scrolled: Boolean, content: @Composable () -> Unit) {
    val backdrop = LocalBottomSheetBlurBackdrop.current
    val progress = animateFloatAsState(
        targetValue = if (scrolled) 1f else 0f,
        animationSpec = GlassMotion.topBarButtonFloat(),
        label = "sheetHeaderMaterial",
    )
    val keepMaterial = scrolled || progress.value > 0f
    val underlay = GlassTopAppBarDefaults.actionBarUnderlayMaterial().takeIf { keepMaterial && backdrop != null }
    CompositionLocalProvider(
        LocalGlassTopBackdrop provides backdrop,
        LocalGlassTopAppBarContext provides GlassTopAppBarContext(
            backdrop = backdrop, buttonBackdrop = backdrop,
            material = GlassTopAppBarDefaults.buttonMaterial().withUnderlayBlur(underlay),
            underlayMaterial = underlay, buttonUnderlayMaterial = underlay,
            floating = scrolled, alpha = 1f, ramp = progress, materialProgress = progress,
            keepMaterial = keepMaterial, style = GlassDefaults.Style,
            size = GlassTopAppBarDefaults.ButtonSize,
            shape = GlassShape(GlassTopAppBarDefaults.ButtonSize / 2),
            fill = GlassTopAppBarDefaults.buttonFill(), stroke = GlassTopAppBarDefaults.buttonStroke(),
            shadow = GlassShadows.Regular,
        ),
        content = content,
    )
}
