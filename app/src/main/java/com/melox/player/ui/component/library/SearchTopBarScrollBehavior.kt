package com.melox.player.ui.component.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior

@Composable
internal fun rememberSearchTopBarScrollBehavior(searchVisible: Boolean): ScrollBehavior {
    val currentSearchVisible by rememberUpdatedState(searchVisible)
    val behavior = MiuixScrollBehavior()
    return remember(behavior) {
        SearchTopBarScrollBehavior(behavior) { currentSearchVisible }
    }
}

internal class SearchTopBarScrollBehavior(
    private val behavior: ScrollBehavior,
    private val searchVisible: () -> Boolean,
) : ScrollBehavior by behavior {
    override val isPinned: Boolean
        get() = searchVisible() || behavior.isPinned

    override val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
            if (searchVisible()) Offset.Zero else behavior.nestedScrollConnection.onPreScroll(available, source)

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
            if (searchVisible()) Offset.Zero else behavior.nestedScrollConnection.onPostScroll(consumed, available, source)

        override suspend fun onPreFling(available: Velocity): Velocity =
            if (searchVisible()) Velocity.Zero else behavior.nestedScrollConnection.onPreFling(available)

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
            if (searchVisible()) Velocity.Zero else behavior.nestedScrollConnection.onPostFling(consumed, available)
    }
}
