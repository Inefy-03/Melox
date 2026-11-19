package com.melox.player.ui.component.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.toSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
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
import top.yukonga.miuix.kmp.upstream.glass.GlassIconButton
import top.yukonga.miuix.kmp.upstream.glass.GlassNavigationBarDefaults
import top.yukonga.miuix.kmp.upstream.blur.Backdrop
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.melox.player.ui.component.Icon
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
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    backdrop: Backdrop? = null,
    alphabetIndexVisible: Boolean = false,
) {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var locateJob by remember { mutableStateOf<Job?>(null) }
    val locateActionSize = ButtonDefaults.MinHeight
    val trailingPadding = maxOf(
        TopAppBarDefaults.ActionIconPadding,
        if (alphabetIndexVisible) AlphabetSideBarWidth else 0.dp,
    )
    var actionAreaBounds by remember { mutableStateOf(Rect.Zero) }
    // The page reports its origin after layout, even when the player is already measured.
    val actionVisible = visible && !actionAreaBounds.isEmpty && !miniPlayerBounds.isEmpty
    Box(modifier.fillMaxSize().zIndex(1f).onGloballyPositioned { actionAreaBounds = Rect(it.positionInRoot(), it.size.toSize()) }) {
        AnimatedVisibility(
            visible = actionVisible,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .offset {
                    with(density) {
                        musicLocationOffset(
                            miniPlayerBounds,
                            actionAreaBounds,
                            locateActionSize.toPx(),
                            12.dp.toPx(),
                            (contentPadding.calculateEndPadding(layoutDirection) + trailingPadding).toPx(),
                        )
                    }
                }
                .then(if (actionVisible) Modifier else Modifier.clearAndSetSemantics {}.pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                    }
                }),
            label = "musicLocationVisibility",
        ) {
            GlassIconButton(
                onClick = {
                    if (actionVisible && currentIndex >= 0) {
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
                modifier = Modifier.semantics { role = Role.Button },
                backdrop = backdrop,
                inheritTopBarMaterial = false,
                material = GlassNavigationBarDefaults.material(),
                stroke = GlassNavigationBarDefaults.stroke(),
                fill = MiuixTheme.colorScheme.surfaceContainer,
                size = locateActionSize,
                shadow = null,
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
    pageBounds: Rect,
    actionSizePx: Float,
    gapPx: Float,
    trailingPaddingPx: Float,
): IntOffset = IntOffset(
    (pageBounds.width - trailingPaddingPx - actionSizePx)
        .roundToInt().coerceAtLeast(0),
    (miniPlayerBounds.top - pageBounds.top - gapPx - actionSizePx)
        .roundToInt().coerceIn(0, (pageBounds.height - actionSizePx).roundToInt().coerceAtLeast(0)),
)

