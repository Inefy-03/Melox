package com.melox.player.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.melox.player.ui.LocalFloatingNavigationRailInset
import com.melox.player.ui.LocalOs4LandscapeNavigation
import com.melox.player.ui.theme.LocalOs4Style

// Include the reserved region in the bar background and its parent's blur bounds.
@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun Modifier.stableTopAppBarInsets(color: Color): Modifier =
    if (LocalOs4Style.current) this else
        background(color).stableTopAppBarContentInsets()

// Keep Glass backgrounds outside the reserved inset while moving only foreground controls.
@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun Modifier.stableTopAppBarContentInsets(): Modifier =
    windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility.only(WindowInsetsSides.Top))
        .padding(start = if (LocalOs4LandscapeNavigation.current != null) LocalFloatingNavigationRailInset.current else 0.dp)
