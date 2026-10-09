package com.melox.player

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import com.melox.player.ui.screen.library.collapseMusicLocationTopBar
import com.melox.player.ui.screen.library.musicLocationOffset
import org.junit.Assert.assertEquals
import org.junit.Test
import top.yukonga.miuix.kmp.basic.TopAppBarState

class MusicLocationLayoutTest {
    @Test
    fun compactActionSharesSortIconAlignmentAcrossMiniPlayerStyles() {
        val viewport = Rect(80f, 20f, 480f, 820f)
        val miniPlayer = Rect(100f, 700f, 460f, 748f)
        assertEquals(IntOffset(344, 628), musicLocationOffset(miniPlayer, viewport, 40f, 12f, 16f))
        val normalMiniPlayer = Rect(80f, 720f, 480f, 774f)
        assertEquals(IntOffset(344, 648), musicLocationOffset(normalMiniPlayer, viewport, 40f, 12f, 16f))
    }

    @Test
    fun windowOriginChangesDoNotChangeLocalActionPlacement() {
        val original = musicLocationOffset(Rect(20f, 700f, 380f, 748f), Rect(0f, 0f, 400f, 800f), 40f, 12f, 16f)
        val translated = musicLocationOffset(Rect(120f, 730f, 480f, 778f), Rect(100f, 30f, 500f, 830f), 40f, 12f, 16f)
        assertEquals(original, translated)
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
