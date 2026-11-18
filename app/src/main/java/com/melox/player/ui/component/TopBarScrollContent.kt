package com.melox.player.ui.component

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.composed
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
    val overscrolls = mutableStateMapOf<Any, ScrollContentPosition>()
    val overscrollDistance: Float
        get() = overscrolls.values.maxByOrNull {
            val overlap = it.bounds.intersect(bounds)
            overlap.width.coerceAtLeast(0f) * overlap.height.coerceAtLeast(0f)
        }?.distance?.value?.invoke() ?: 0f
    val distance: Float?
        get() = lists.values.maxByOrNull {
            val overlap = it.bounds.intersect(bounds)
            overlap.width.coerceAtLeast(0f) * overlap.height.coerceAtLeast(0f)
        }?.distance?.value?.invoke()

    fun visualContentOffset(fallback: Float): Float =
        (distance?.let { -it } ?: fallback) + overscrollDistance
}

internal class ScrollContentPosition(val distance: State<() -> Float>) {
    var bounds by mutableStateOf(Rect.Zero)
}

internal val LocalTopBarScrollContent = staticCompositionLocalOf<TopBarScrollContent?> { null }

@Composable
private fun Modifier.topBarScrollContent(distance: () -> Float): Modifier {
    val owner = LocalTopBarScrollContent.current ?: return this
    val latest = rememberUpdatedState(distance)
    val position = remember(owner) { ScrollContentPosition(latest) }
    DisposableEffect(owner, position) {
        owner.lists[position] = position
        onDispose { owner.lists.remove(position) }
    }
    return onGloballyPositioned { position.bounds = it.boundsInRoot() }
}

@Composable
internal fun topBarContentScrolled(behavior: ScrollBehavior?): Boolean {
    val content = LocalTopBarScrollContent.current
    return content?.distance?.let { it > 0f } ?: behavior?.state?.let { it.contentOffset < 0f } ?: true
}

@Composable
internal fun rememberTopBarVisualContentOffset(behavior: ScrollBehavior?): (() -> Float)? {
    val content = LocalTopBarScrollContent.current ?: return null
    if (behavior == null) return null
    // Programmatic jumps and filtering update lazy layout without nested-scroll callbacks.
    return remember(content, behavior) {
        { content.visualContentOffset(behavior.state.contentOffset) }
    }
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
    modifier.topBarScrollContent { if (!state.canScrollBackward) 0f else if (state.firstVisibleItemIndex > 0) Float.POSITIVE_INFINITY else state.firstVisibleItemScrollOffset.toFloat() },
    state, contentPadding, reverseLayout, verticalArrangement, horizontalAlignment,
    flingBehavior, userScrollEnabled, overscrollEffect, content,
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
    columns, modifier.topBarScrollContent { if (!state.canScrollBackward) 0f else if (state.firstVisibleItemIndex > 0) Float.POSITIVE_INFINITY else state.firstVisibleItemScrollOffset.toFloat() },
    state, contentPadding, reverseLayout, verticalArrangement, horizontalArrangement,
    flingBehavior, userScrollEnabled, overscrollEffect, content,
)

// Observe the native effect's translated content, including its spring settle frames.
internal fun Modifier.overScrollVertical(
    isEnabled: () -> Boolean = { true },
): Modifier = composed {
    val owner = LocalTopBarScrollContent.current
    if (owner == null) return@composed this.miuixOverScrollVertical(isEnabled = isEnabled)
    var displacement by remember { mutableFloatStateOf(0f) }
    val distance = rememberUpdatedState<() -> Float> { displacement }
    val position = remember(owner) { ScrollContentPosition(distance) }
    DisposableEffect(owner, position) {
        owner.overscrolls[position] = position
        onDispose { owner.overscrolls.remove(position) }
    }
    val updateDisplacement = remember { { offset: Float -> displacement = offset } }
    this
        .onGloballyPositioned { position.bounds = it.boundsInRoot() }
        .miuixOverScrollVertical(onOffsetChanged = updateDisplacement, isEnabled = isEnabled)
}
