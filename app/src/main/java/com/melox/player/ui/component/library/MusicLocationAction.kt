package com.melox.player.ui.component.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.melox.player.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import com.melox.player.ui.component.os4.Icon
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.basic.TopAppBarState
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MusicLocationAction(
    visible: Boolean,
    currentIndex: Int,
    listState: LazyListState,
    scrollBehavior: ScrollBehavior,
    miniPlayerBounds: Rect,
    playerContentBounds: Rect,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var locateJob by remember { mutableStateOf<Job?>(null) }
    val locateActionSize = ButtonDefaults.MinHeight
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
            .offset {
                with(density) {
                    musicLocationOffset(
                        miniPlayerBounds,
                        playerContentBounds,
                        locateActionSize.toPx(),
                        12.dp.toPx(),
                        (contentPadding.calculateEndPadding(layoutDirection) +
                            TopAppBarDefaults.ActionIconPadding).toPx(),
                    )
                }
            }
            .then(if (visible) Modifier else Modifier.clearAndSetSemantics {}),
        label = "musicLocationVisibility",
    ) {
        FloatingActionButton(
            onClick = {
                if (visible && currentIndex >= 0) {
                    locateJob?.cancel()
                    locateJob = scope.launch {
                        val paddingBeforeCollapse = listState.layoutInfo.beforeContentPadding
                        val collapsedPadding = paddingBeforeCollapse +
                            collapseMusicLocationTopBar(scrollBehavior.state).roundToInt()
                        // Programmatic list scrolling does not dispatch nested scroll to the bar.
                        snapshotFlow { listState.layoutInfo.beforeContentPadding }
                            .first { it <= collapsedPadding + 1 }
                        listState.animateScrollToItem(
                            currentIndex,
                            scrollOffset = with(density) { 12.dp.roundToPx() },
                        )
                    }
                }
            },
            containerColor = CardDefaults.defaultColors().color.copy(alpha = 0.8f),
            shadowElevation = 0.dp,
            minWidth = locateActionSize,
            minHeight = locateActionSize,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_locate_current_track),
                contentDescription = stringResource(R.string.locate_current_track),
                modifier = Modifier.size(24.dp),
                tint = MiuixTheme.colorScheme.onSurface,
            )
        }
    }
}

internal fun collapseMusicLocationTopBar(state: TopAppBarState): Float {
    if (state.heightOffsetLimit == -Float.MAX_VALUE) return 0f
    val heightChange = state.heightOffsetLimit - state.heightOffset
    state.heightOffset = state.heightOffsetLimit
    state.contentOffset = state.heightOffsetLimit
    return heightChange
}

internal fun musicLocationOffset(
    miniPlayerBounds: Rect,
    playerContentBounds: Rect,
    actionSizePx: Float,
    gapPx: Float,
    trailingPaddingPx: Float,
): IntOffset = IntOffset(
    (playerContentBounds.width - trailingPaddingPx - actionSizePx)
        .roundToInt().coerceAtLeast(0),
    (miniPlayerBounds.top - playerContentBounds.top - gapPx - actionSizePx)
        .roundToInt().coerceAtLeast(0),
)

