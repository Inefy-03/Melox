package com.melox.player.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import top.yukonga.miuix.kmp.glass.GlassBottomSheetDefaults
import top.yukonga.miuix.kmp.glass.GlassDefaults
import top.yukonga.miuix.kmp.glass.GlassMotion
import top.yukonga.miuix.kmp.glass.GlassShadows
import top.yukonga.miuix.kmp.glass.GlassShape
import top.yukonga.miuix.kmp.glass.GlassTopAppBarContext
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.glass.LocalGlassTopAppBarContext
import top.yukonga.miuix.kmp.glass.glassBottomSheetSurface
import top.yukonga.miuix.kmp.glass.withUnderlayBlur
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.layout.BottomSheetSurfaceScope
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
    dimColor: Color = GlassBottomSheetDefaults.ScrimColor,
    surface: (@Composable BottomSheetSurfaceScope.() -> Modifier)? = null,
    headerScrolled: (() -> Boolean)? = null,
    content: @Composable () -> Unit,
) {
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
            { SheetHeaderButtons(scrolled, action) }
        },
        endAction = endAction?.let { action ->
            { SheetHeaderButtons(scrolled, action) }
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
        surface = surface ?: glassBottomSheetSurface(LocalBottomSheetBlurBackdrop.current),
        content = {
            CompositionLocalProvider(LocalTopBarScrollContent provides null) {
                Column(Modifier.fillMaxWidth().nestedScroll(scrollObserver)) { content() }
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
