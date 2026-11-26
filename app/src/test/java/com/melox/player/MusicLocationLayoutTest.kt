package com.melox.player

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import com.melox.player.ui.component.library.collapseMusicLocationTopBar
import com.melox.player.ui.component.library.syncAlphabetJumpTopBar
import com.melox.player.ui.component.library.musicLocationOffset
import org.junit.Assert.assertEquals
import org.junit.Test
import top.yukonga.miuix.kmp.basic.TopAppBarState

class MusicLocationLayoutTest {
    @Test
    fun alphabetJumpKeepsTheTopBarInSyncWithTheMeasuredDestination() {
        val state = TopAppBarState(-100f, -20f, -20f)

        syncAlphabetJumpTopBar(state, isPinned = false, canScrollBackward = true)
        assertEquals(-100f, state.heightOffset, 0f)
        assertEquals(-100f, state.contentOffset, 0f)

        syncAlphabetJumpTopBar(state, isPinned = false, canScrollBackward = false)
        assertEquals(0f, state.heightOffset, 0f)
        assertEquals(0f, state.contentOffset, 0f)

        syncAlphabetJumpTopBar(state, isPinned = true, canScrollBackward = true)
        assertEquals(0f, state.heightOffset, 0f)
        assertEquals(0f, state.contentOffset, 0f)
    }

    @Test
    fun compactActionSharesSortIconAlignmentAcrossMiniPlayerStyles() {
        val viewport = Rect(80f, 20f, 480f, 820f)
        val miniPlayer = Rect(100f, 700f, 460f, 748f)
        assertEquals(IntOffset(344, 628), musicLocationOffset(miniPlayer.top, viewport, 40f, 12f, 16f))
        val normalMiniPlayer = Rect(80f, 720f, 480f, 774f)
        assertEquals(IntOffset(344, 648), musicLocationOffset(normalMiniPlayer.top, viewport, 40f, 12f, 16f))
    }

    @Test
    fun windowOriginChangesDoNotChangeLocalActionPlacement() {
        val original = musicLocationOffset(700f, Rect(0f, 0f, 400f, 800f), 40f, 12f, 16f)
        val translated = musicLocationOffset(730f, Rect(100f, 30f, 500f, 830f), 40f, 12f, 16f)
        assertEquals(original, translated)
    }

    @Test
    fun pageWidthAndTrailingInsetAnchorActionAcrossSidebarAndPlayerSizes() {
        val fullPage = Rect(0f, 20f, 1000f, 620f)
        val insetPage = Rect(262f, 20f, 1000f, 620f)
        val smallPlayer = Rect(300f, 540f, 900f, 590f)
        val widePlayer = Rect(20f, 520f, 980f, 590f)
        assertEquals(IntOffset(920, 468), musicLocationOffset(smallPlayer.top, fullPage, 40f, 12f, 40f))
        assertEquals(IntOffset(658, 468), musicLocationOffset(smallPlayer.top, insetPage, 40f, 12f, 40f))
        assertEquals(IntOffset(920, 448), musicLocationOffset(widePlayer.top, fullPage, 40f, 12f, 40f))
    }

    @Test
    fun actionRemainsInsideThePageWhenThePlayerMovesBelowItsViewport() {
        val page = Rect(262f, 20f, 1000f, 620f)
        val player = Rect(300f, 800f, 900f, 850f)
        assertEquals(IntOffset(658, 560), musicLocationOffset(player.top, page, 40f, 12f, 40f))
    }

    @Test
    fun locationCollapsesExpandedAndPartiallyCollapsedBarsBeforeListPlacement() {
        for (offset in listOf(0f, -30f, -100f)) {
            val state = TopAppBarState(-100f, offset, offset)
            assertEquals(-100f - offset, collapseMusicLocationTopBar(state), 0f)
            assertEquals(-100f, state.heightOffset, 0f)
            assertEquals(-100f, state.contentOffset, 0f)
            assertEquals(1f, state.collapsedFraction, 0f)
        }
    }

    @Test
    fun pinnedAndUnmeasuredBarsCannotIntroduceInvalidPadding() {
        val pinned = TopAppBarState(0f, 0f, 0f)
        assertEquals(0f, collapseMusicLocationTopBar(pinned), 0f)
        val unmeasured = TopAppBarState(-Float.MAX_VALUE, 0f, 0f)
        assertEquals(0f, collapseMusicLocationTopBar(unmeasured), 0f)
        assertEquals(0f, unmeasured.heightOffset, 0f)
    }
}
