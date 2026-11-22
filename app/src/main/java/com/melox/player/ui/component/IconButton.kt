package com.melox.player.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import top.yukonga.miuix.kmp.basic.IconButtonDefaults
import androidx.compose.foundation.LocalIndication
import top.yukonga.miuix.kmp.utils.MiuixIndication
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.glass.GlassIconButton as MiuixGlassIconButton

internal val LocalTopBarButtonHaptics = staticCompositionLocalOf { false }

@Composable
internal fun TopBarButtonHaptics(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTopBarButtonHaptics provides true, content = content)
}

@Composable
internal fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    holdDownState: Boolean = false,
    backgroundColor: Color = Color.Unspecified,
    cornerRadius: Dp = IconButtonDefaults.CornerRadius,
    minHeight: Dp = IconButtonDefaults.MinHeight,
    minWidth: Dp = IconButtonDefaults.MinWidth,
    glassEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val topBarButtonHaptics = LocalTopBarButtonHaptics.current
    val onClickWithHaptic = {
        if (topBarButtonHaptics) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
        }
        onClick()
    }
    // Custom/no indication and display-only wrappers retain their original interaction owner.
    if (glassEnabled && !holdDownState && LocalIndication.current is MiuixIndication &&
        (enabled || !topBarButtonHaptics)
    ) {
        if (topBarButtonHaptics && enabled) {
            MiuixGlassIconButton(onClick = onClickWithHaptic, modifier = modifier, content = content)
            return
        }
        com.melox.player.ui.glass.GlassIconButton(
            onClick = onClickWithHaptic,
            modifier = modifier,
            enabled = enabled,
            content = content,
            size = maxOf(minHeight, minWidth),
            // The root scene includes this button; only the separate wallpaper layer is safe here.
            backdrop = if (com.melox.player.ui.component.LocalGlassTopBackdrop.current == null) LocalPageSurfaceBackdrop.current else null,
        )
        return
    }
    val topBarButtonSize = if (topBarButtonHaptics && glassEnabled && !enabled) {
        top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults.ButtonSize
    } else null
    MiuixIconButton(
        onClick = onClickWithHaptic,
        modifier = modifier,
        enabled = enabled,
        holdDownState = holdDownState,
        backgroundColor = backgroundColor,
        cornerRadius = cornerRadius,
        minHeight = topBarButtonSize ?: minHeight,
        minWidth = topBarButtonSize ?: minWidth,
        content = content,
    )
}
