package com.melox.player

import androidx.compose.animation.core.spring
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import com.melox.player.ui.component.library.SearchTopBarScrollBehavior
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBarState

class SearchTopBarScrollBehaviorTest {
    @Test
    fun visibleSearchBlocksScrollAndFlingWithoutConsumingListMotion() = runBlocking {
        val original = RecordingScrollBehavior()
        val behavior = SearchTopBarScrollBehavior(original) { true }
        val connection = behavior.nestedScrollConnection

        assertTrue(behavior.isPinned)
        assertEquals(Offset.Zero, connection.onPreScroll(Offset(0f, 20f), NestedScrollSource.UserInput))
        assertEquals(Offset.Zero, connection.onPostScroll(Offset.Zero, Offset(0f, 20f), NestedScrollSource.UserInput))
        assertEquals(Velocity.Zero, connection.onPreFling(Velocity(0f, 100f)))
        assertEquals(Velocity.Zero, connection.onPostFling(Velocity.Zero, Velocity(0f, 100f)))
        assertEquals(0, original.events)
        assertEquals(-100f, original.state.heightOffset, 0f)
    }

    @Test
    fun closingSearchRestoresOriginalBehaviorUsingTheSameConnectionAndState() = runBlocking {
        var visible = true
        val original = RecordingScrollBehavior()
        val behavior = SearchTopBarScrollBehavior(original) { visible }
        val connection = behavior.nestedScrollConnection
        visible = false

        assertFalse(behavior.isPinned)
        assertSame(original.state, behavior.state)
        assertSame(original.snapAnimationSpec, behavior.snapAnimationSpec)
        assertEquals(Offset(0f, 20f), connection.onPreScroll(Offset(0f, 20f), NestedScrollSource.UserInput))
        assertEquals(Offset(0f, 20f), connection.onPostScroll(Offset.Zero, Offset(0f, 20f), NestedScrollSource.UserInput))
        assertEquals(Velocity(0f, 100f), connection.onPreFling(Velocity(0f, 100f)))
        assertEquals(Velocity(0f, 100f), connection.onPostFling(Velocity.Zero, Velocity(0f, 100f)))
        assertEquals(4, original.events)
        assertEquals(0f, original.state.heightOffset, 0f)

        visible = true
        connection.onPostScroll(Offset.Zero, Offset(0f, 20f), NestedScrollSource.UserInput)
        assertEquals(4, original.events)
    }

    @Test
    fun lockingDuringOpeningKeepsTheOngoingCollapseStateAndAnimationSpec() {
        val original = RecordingScrollBehavior()
        original.state.heightOffset = -30f
        val behavior = SearchTopBarScrollBehavior(original) { true }

        behavior.nestedScrollConnection.onPostScroll(Offset.Zero, Offset(0f, 20f), NestedScrollSource.UserInput)
        assertEquals(-30f, behavior.state.heightOffset, 0f)
        behavior.state.heightOffset = -100f
        assertEquals(1f, original.state.collapsedFraction, 0f)
        assertSame(original.snapAnimationSpec, behavior.snapAnimationSpec)
    }

    private class RecordingScrollBehavior : ScrollBehavior {
        override val state = TopAppBarState(-100f, -100f, -100f)
        override val isPinned = false
        override val snapAnimationSpec = spring<Float>(stiffness = 2500f)
        override val flingAnimationSpec = null
        var events = 0

        private fun expand() {
            events++
            state.heightOffset = 0f
        }

        override val nestedScrollConnection = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                expand()
                return available
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                expand()
                return available
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                expand()
                return available
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                expand()
                return available
            }
        }
    }
}
