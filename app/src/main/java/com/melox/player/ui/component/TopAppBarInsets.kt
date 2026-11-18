package com.melox.player.ui.component

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.melox.player.ui.LocalFloatingNavigationRailInset
import com.melox.player.ui.LocalClippedNavigationRailInset

// Keep Glass backgrounds outside the reserved inset while moving only foreground controls.
@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun Modifier.stableTopAppBarContentInsets(): Modifier =
    windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility.only(WindowInsetsSides.Top))
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        .padding(start = (LocalFloatingNavigationRailInset.current - LocalClippedNavigationRailInset.current).coerceAtLeast(0.dp))
