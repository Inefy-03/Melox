package com.melox.player.ui.component.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import com.melox.player.ui.component.LocalGlassSearchQuery

private class RenderedSort(var value: Any)
private class RenderedSearchQuery(var value: String? = null)

@Composable
internal fun ResetSearchResultScroll(query: String, scrollState: Any) {
    val previous = remember(scrollState) { RenderedSearchQuery() }
    SideEffect {
        if (previous.value != query) {
            // Use the new result set's first position in the next measurement, before key anchoring.
            when (scrollState) {
                is LazyListState -> scrollState.requestScrollToItem(0)
                is LazyGridState -> scrollState.requestScrollToItem(0)
            }
            previous.value = query
        }
    }
}

internal class SearchScrollPosition {
    private var searching = false
    private var savedPosition: Pair<Int, Int>? = null

    fun update(query: String, index: Int, offset: Int): Pair<Int, Int>? {
        val nextSearching = query.isNotBlank()
        val restore = savedPosition.takeIf { searching && !nextSearching }
        if (!searching && nextSearching) savedPosition = index to offset
        if (!nextSearching) savedPosition = null
        searching = nextSearching
        return restore
    }
}

/** Preserve sort coordinates and restore the pre-search viewport when filtering ends. */
@Composable
internal fun PreserveSortScrollPosition(
    sortKey: Any,
    query: String,
    itemCount: Int,
    scrollState: Any,
    layoutKey: Any = Unit,
    preservePosition: () -> Unit,
) {
    if (LocalGlassSearchQuery.current != null) {
        ResetSearchResultScroll(query, scrollState)
        return
    }
    val searchPosition = remember(scrollState) { SearchScrollPosition() }
    // Search, source-size and geometry changes establish a new baseline.
    val previous = remember(scrollState, query, itemCount, layoutKey) {
        RenderedSort(sortKey)
    }
    SideEffect {
        val restoredPosition = when (scrollState) {
            is LazyListState -> searchPosition.update(
                query, scrollState.firstVisibleItemIndex, scrollState.firstVisibleItemScrollOffset,
            )
            is LazyGridState -> searchPosition.update(
                query, scrollState.firstVisibleItemIndex, scrollState.firstVisibleItemScrollOffset,
            )
            else -> null
        }
        if (restoredPosition != null) {
            val (index, offset) = restoredPosition
            when (scrollState) {
                is LazyListState -> scrollState.requestScrollToItem(index, offset)
                is LazyGridState -> scrollState.requestScrollToItem(index, offset)
            }
        }
        if (previous.value != sortKey) {
            preservePosition()
            previous.value = sortKey
        }
    }
}
