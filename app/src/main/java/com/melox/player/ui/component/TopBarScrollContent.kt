package com.melox.player.ui.component

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.utils.overScrollVertical as miuixOverScrollVertical
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import androidx.compose.foundation.lazy.LazyColumn as FoundationLazyColumn
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid as FoundationLazyVerticalGrid

internal class TopBarScrollContent {
    var bounds by mutableStateOf(Rect.Zero)
    val lists = mutableStateMapOf<Any, ScrollContentPosition>()
    private val visibleList: ScrollContentPosition?
        get() = lists.values.maxByOrNull {
            val overlap = it.bounds.intersect(bounds)
            overlap.width.coerceAtLeast(0f) * overlap.height.coerceAtLeast(0f)
        }

    val distance: Float? get() = visibleList?.distance?.value?.invoke()
    val canScrollBackward: Boolean? get() = visibleList?.canScrollBackward?.value?.invoke()
}

internal class ScrollContentPosition(
    val distance: State<() -> Float>,
    val canScrollBackward: State<() -> Boolean>,
) {
    var bounds by mutableStateOf(Rect.Zero)
}

internal val LocalTopBarScrollContent = staticCompositionLocalOf<TopBarScrollContent?> { null }

@Composable
private fun Modifier.topBarScrollContent(canScrollBackward: () -> Boolean, distance: () -> Float): Modifier {
    val owner = LocalTopBarScrollContent.current ?: return this
    val latest = rememberUpdatedState(distance)
    val latestCanScrollBackward = rememberUpdatedState(canScrollBackward)
    val position = remember(owner) { ScrollContentPosition(latest, latestCanScrollBackward) }
    DisposableEffect(owner, position) {
        owner.lists[position] = position
        onDispose { owner.lists.remove(position) }
    }
    return onGloballyPositioned { position.bounds = it.boundsInRoot() }
}

@Composable
internal fun topBarContentScrolled(behavior: ScrollBehavior?): Boolean {
    val content = LocalTopBarScrollContent.current
    return content?.canScrollBackward ?: behavior?.state?.let {
        it.contentOffset < -1f || it.overscrollOffset < -1f
    } ?: true
}

@Composable
internal fun LazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    overscrollEffect: OverscrollEffect? = androidx.compose.foundation.rememberOverscrollEffect(),
    content: LazyListScope.() -> Unit,
) = FoundationLazyColumn(
    modifier.topBarScrollContent(canScrollBackward = { state.canScrollBackward }) { if (!state.canScrollBackward) 0f else if (state.firstVisibleItemIndex > 0) Float.POSITIVE_INFINITY else state.firstVisibleItemScrollOffset.toFloat() },
    state, contentPadding, reverseLayout, verticalArrangement, horizontalAlignment,
    flingBehavior, userScrollEnabled, overscrollEffect, content = content,
)

@Composable
internal fun LazyVerticalGrid(
    columns: GridCells,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    overscrollEffect: OverscrollEffect? = androidx.compose.foundation.rememberOverscrollEffect(),
    content: LazyGridScope.() -> Unit,
) = FoundationLazyVerticalGrid(
    columns, modifier.topBarScrollContent(canScrollBackward = { state.canScrollBackward }) { if (!state.canScrollBackward) 0f else if (state.firstVisibleItemIndex > 0) Float.POSITIVE_INFINITY else state.firstVisibleItemScrollOffset.toFloat() },
    state, contentPadding, reverseLayout, verticalArrangement, horizontalArrangement,
    flingBehavior, userScrollEnabled, overscrollEffect, content = content,
)

@Composable
internal fun Modifier.overScrollVertical(
    scrollBehavior: ScrollBehavior? = null,
    isEnabled: () -> Boolean = { true },
): Modifier {
    val state = scrollBehavior?.state
    val onOffsetChanged: ((Float) -> Unit)? = remember(state) {
        state?.let { { offset -> it.overscrollOffset = offset } }
    }
    return miuixOverScrollVertical(onOffsetChanged = onOffsetChanged, isEnabled = isEnabled)
}
