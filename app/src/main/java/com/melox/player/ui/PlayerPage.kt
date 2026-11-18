package com.melox.player.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import com.melox.player.ui.component.LocalGlassSearchPage
import com.melox.player.ui.component.PageScaffold
import com.melox.player.ui.component.SmallTopAppBar
import com.melox.player.ui.component.TopAppBar
import com.melox.player.ui.component.TopBarBackdrop
import com.melox.player.ui.component.library.fixedAlphabetIndexTopPadding
import com.melox.player.ui.component.miuixBarColor
import com.melox.player.ui.component.refreshFixedWallpaperSample
import com.melox.player.ui.component.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.blur.layerBackdrop

@Composable
internal fun PlayerPage(
    title: String,
    outerPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
    useSmallTopAppBar: Boolean,
    isRootPage: Boolean = true,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
    content: @Composable (PaddingValues, ScrollBehavior, indexTopPadding: Dp) -> Unit,
) {
    val landscapeNavigation = LocalLandscapeNavigation.current.takeIf { isRootPage }
    val replaceTitle = landscapeNavigation?.labels?.contains(title) == true
    val layoutDirection = LocalLayoutDirection.current
    var topBarCaptureCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val topBarBackdrop = rememberBlurBackdrop(captureCoordinates = { topBarCaptureCoordinates })

    PageScaffold(
        topBar = {
            TopBarBackdrop(backdrop = topBarBackdrop) {
                val topBarColor = topBarBackdrop.miuixBarColor()
                if (!isRootPage) {
                    TopAppBar(
                        title = title,
                        scrollBehavior = scrollBehavior,
                        color = topBarColor,
                        navigationIcon = navigationIcon,
                        actions = actions,
                        bottomContent = bottomContent,
                    )
                } else if (useSmallTopAppBar) {
                    SmallTopAppBar(
                        title = title,
                        inlineTitleEnabled = !replaceTitle,
                        modifier = Modifier,
                        color = topBarColor,
                        navigationIcon = navigationIcon,
                        actions = actions,
                        scrollBehavior = scrollBehavior,
                        bottomContent = bottomContent,
                    )
                } else {
                    TopAppBar(
                        title = title,
                        modifier = Modifier,
                        color = topBarColor,
                        navigationIcon = navigationIcon,
                        actions = actions,
                        scrollBehavior = scrollBehavior,
                        bottomContent = bottomContent,
                    )
                }
            }
        },
    ) { innerPadding ->
        val indexTopPadding = fixedAlphabetIndexTopPadding()
        // The page bar owns top/system insets; the outer scaffold owns bottom-bar clearance.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { topBarCaptureCoordinates = it }
                .refreshFixedWallpaperSample()
                .then(topBarBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
        ) {
            content(
                PaddingValues(
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    top = innerPadding.calculateTopPadding(),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                    bottom = if (LocalGlassSearchPage.current?.isExpanded == true) {
                        innerPadding.calculateBottomPadding()
                    } else {
                        outerPadding.calculateBottomPadding()
                    },
                ),
                scrollBehavior,
                indexTopPadding,
            )
        }
    }
}
