package com.melox.player

import androidx.compose.ui.unit.dp
import com.melox.player.ui.screen.playback.PLAYER_PORTRAIT_CONTENT_MAX_WIDTH
import com.melox.player.ui.screen.playback.PlayerLayout
import com.melox.player.ui.screen.playback.compactLandscapeArtworkSize
import com.melox.player.ui.screen.playback.compactLandscapeArtworkPaneWidth
import com.melox.player.ui.screen.playback.compactLandscapeHeaderTop
import com.melox.player.ui.screen.playback.compactLandscapeLyricsToggleStart
import com.melox.player.ui.screen.playback.compactLandscapePaneContentWidth
import com.melox.player.ui.screen.playback.compactLandscapeSidePadding
import com.melox.player.ui.screen.playback.compactLandscapePlayerSpacing
import com.melox.player.ui.screen.playback.landscapePlayerDesignContentWidth
import com.melox.player.ui.screen.playback.landscapePlayerLyricsContentWidth
import com.melox.player.ui.screen.playback.landscapePlayerLyricsPaneWidth
import com.melox.player.ui.screen.playback.landscapePlayerPaneSpacing
import com.melox.player.ui.screen.playback.landscapePlayerPlaybackPaneWidth
import com.melox.player.ui.screen.playback.landscapePlayerSpacing
import com.melox.player.ui.screen.playback.landscapePlayerArtworkSize
import com.melox.player.ui.screen.playback.landscapePlayerArtworkAlignmentSize
import com.melox.player.ui.screen.playback.playerContentWidth
import com.melox.player.ui.screen.playback.playerControlsBottomPadding
import com.melox.player.ui.screen.playback.playerArtworkContentSize
import com.melox.player.ui.screen.playback.playerArtworkAlignmentContentSize
import com.melox.player.ui.screen.playback.playerArtworkSizeForContent
import com.melox.player.ui.screen.playback.playerHeaderTopPadding
import com.melox.player.ui.screen.playback.playerLayout
import com.melox.player.ui.screen.playback.playerHidesSystemNavigation
import com.melox.player.ui.screen.playback.playerLyricsAreActive
import com.melox.player.ui.screen.playback.playerPrimaryControlLayout
import com.melox.player.ui.screen.playback.playerUnboundedContentWidth
import com.melox.player.ui.screen.playback.playerVerticalSpacing
import com.melox.player.ui.screen.playback.fitPlayerArtworkSize
import com.melox.player.ui.screen.playback.usesWidePlayerLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerResponsiveLayoutTest {
    @Test
    fun primaryControlsSpaceTheIconBoundsEvenly() {
        val layout = playerPrimaryControlLayout(320.dp)

        assertEquals(56.5.dp, layout.previousIconOffset)
        assertEquals(
            layout.previousIconOffset,
            layout.playPauseIconOffset - layout.previousIconOffset - 26.dp,
        )
        assertEquals(
            layout.previousIconOffset,
            layout.nextIconOffset - layout.playPauseIconOffset - 42.dp,
        )
        assertEquals(
            layout.previousIconOffset,
            320.dp - layout.nextIconOffset - 26.dp,
        )
    }

    @Test
    fun primaryControlsUseCurrentTouchTargetSpacingOnNarrowWidths() {
        val layout = playerPrimaryControlLayout(264.dp)

        assertEquals(4.5.dp, layout.playPauseTouchOffset - layout.previousTouchOffset - 64.dp)
        assertEquals(12.5.dp, layout.nextTouchOffset - layout.playPauseTouchOffset - 72.dp)
        assertTrue(layout.previousTouchOffset >= 0.dp)
        assertTrue(layout.nextTouchOffset + 64.dp <= 264.dp)
    }

    @Test
    fun landscapeWideFrameReservesLeadingSpacerInsideTheCenteredGroup() {
        assertEquals(976.dp, landscapePlayerDesignContentWidth(1000.dp))
        assertEquals(0.dp, landscapePlayerDesignContentWidth(16.dp))
    }

    @Test
    fun landscapeLyricsPaneUsesItsOwnWidthAndInset() {
        val width = 1000.dp
        val designWidth = landscapePlayerDesignContentWidth(width)
        val playbackPane = landscapePlayerPlaybackPaneWidth(width)
        val lyricsPane = landscapePlayerLyricsPaneWidth(width)
        val contentWidth = playerUnboundedContentWidth(playbackPane)

        assertEquals(976.dp, designWidth)
        assertEquals(428.dp, playbackPane)
        assertEquals(524.dp, lyricsPane)
        assertEquals(372.dp, contentWidth)
        assertEquals(96.dp, lyricsPane - playbackPane)
        assertEquals(460.dp, landscapePlayerLyricsContentWidth(lyricsPane))
        assertEquals(
            32.dp,
            (lyricsPane - landscapePlayerLyricsContentWidth(lyricsPane)) / 2f,
        )
        assertTrue(landscapePlayerPaneSpacing(width) < 32.dp)
        assertTrue(landscapePlayerPaneSpacing(width) > 16.dp)

        val widthAbovePaneLimit = 1248.dp
        val widePlaybackPane = landscapePlayerPlaybackPaneWidth(widthAbovePaneLimit)
        val wideLyricsPane = landscapePlayerLyricsPaneWidth(widthAbovePaneLimit)
        assertEquals(480.dp, widePlaybackPane)
        assertEquals(600.dp, wideLyricsPane)
        assertEquals(24.dp, landscapePlayerPaneSpacing(widthAbovePaneLimit))
        assertEquals(60.dp, (widthAbovePaneLimit - 24.dp - widePlaybackPane -
            landscapePlayerPaneSpacing(widthAbovePaneLimit) - wideLyricsPane) / 2f)
    }

    @Test
    fun widePaneSpacingCompressesBeforeTheFixedLeadingInsetLosesItsMargin() {
        assertEquals(24.dp, landscapePlayerPaneSpacing(1152.dp))
        assertEquals(18.272728f, landscapePlayerPaneSpacing(876.dp).value, 0.00001f)
        assertEquals(12.dp, landscapePlayerPaneSpacing(600.dp))
        assertEquals(12.dp, landscapePlayerPaneSpacing(400.dp))
    }

    @Test
    fun playerHeaderTopPaddingUsesLayoutSpecificInset() {
        assertEquals(40.dp, playerHeaderTopPadding(PlayerLayout.PORTRAIT, 24.dp))
        assertEquals(24.dp, playerHeaderTopPadding(PlayerLayout.COMPACT_LANDSCAPE, 24.dp))
        assertEquals(48.dp, playerHeaderTopPadding(PlayerLayout.WIDE_TWO_PANE, 24.dp))
    }

    @Test
    fun landscapeArtworkUsesParentWidthAndA64DpVerticalInset() {
        assertEquals(340.dp, landscapePlayerArtworkSize(400.dp, 500.dp))
        assertEquals(140.dp, landscapePlayerArtworkSize(400.dp, 300.dp))
    }

    @Test
    fun playerContentUsesTheInsetCoverWidthWithoutItsShadow() {
        assertEquals(394.dp, playerArtworkSizeForContent(400.dp))
        assertEquals(394.dp, playerArtworkContentSize(394.dp, 6.dp))
        assertEquals(346.dp, playerArtworkContentSize(394.dp, 30.dp))
        assertEquals(394.dp, playerArtworkAlignmentContentSize(394.dp))
        assertEquals(440.dp, landscapePlayerArtworkSize(500.dp, 600.dp))
        assertEquals(444.dp, landscapePlayerArtworkAlignmentSize(500.dp))
        assertEquals(
            168.dp,
            fitPlayerArtworkSize(landscapePlayerArtworkSize(500.dp, 600.dp), 200.dp),
        )
        assertEquals(
            444.dp,
            playerArtworkContentSize(landscapePlayerArtworkAlignmentSize(500.dp), 6.dp),
        )
        assertEquals(
            408.dp,
            playerArtworkContentSize(landscapePlayerArtworkAlignmentSize(500.dp), 24.dp),
        )
    }

    @Test
    fun playerLayoutUsesTheMiuixWideThresholdAndKeepsCompactLandscapeSeparate() {
        assertTrue(usesWidePlayerLayout(960.dp, 640.dp))
        assertTrue(usesWidePlayerLayout(840.dp, 1000.dp))
        assertTrue(usesWidePlayerLayout(800.dp, 800.dp))
        assertEquals(
            PlayerLayout.COMPACT_LANDSCAPE,
            playerLayout(599.dp, 400.dp),
        )
        assertEquals(
            PlayerLayout.COMPACT_LANDSCAPE,
            playerLayout(600.dp, 500.dp),
        )
        assertEquals(
            PlayerLayout.COMPACT_LANDSCAPE,
            playerLayout(800.dp, 400.dp),
        )
        assertEquals(
            PlayerLayout.WIDE_TWO_PANE,
            playerLayout(960.dp, 640.dp),
        )
        assertEquals(
            PlayerLayout.WIDE_TWO_PANE,
            playerLayout(840.dp, 1200.dp),
        )
    }

    @Test
    fun lyricsFrameClockRunsOnlyForTheVisibleOrIncomingLyricsPage() {
        assertFalse(playerLyricsAreActive(PlayerLayout.PORTRAIT, 0, 0))
        assertTrue(playerLyricsAreActive(PlayerLayout.PORTRAIT, 0, 1))
        assertTrue(playerLyricsAreActive(PlayerLayout.PORTRAIT, 1, 1))
        assertFalse(playerLyricsAreActive(PlayerLayout.COMPACT_LANDSCAPE, 0, 0))
        assertTrue(playerLyricsAreActive(PlayerLayout.COMPACT_LANDSCAPE, 1, 0))
        assertTrue(playerLyricsAreActive(PlayerLayout.WIDE_TWO_PANE, 0, 0))
    }

    @Test
    fun systemNavigationOnlyHidesForTheVisibleCompactLandscapePlayer() {
        assertTrue(playerHidesSystemNavigation(PlayerLayout.COMPACT_LANDSCAPE, true))
        assertFalse(playerHidesSystemNavigation(PlayerLayout.COMPACT_LANDSCAPE, false))
        assertFalse(playerHidesSystemNavigation(PlayerLayout.PORTRAIT, true))
        assertFalse(playerHidesSystemNavigation(PlayerLayout.WIDE_TWO_PANE, true))
    }

    @Test
    fun compactLandscapeKeepsFullArtworkAndUsesPortraitContentInset() {
        assertEquals(250.dp, compactLandscapeArtworkSize(
            paneWidth = 250.dp,
            availableHeight = 300.dp,
        ))
        assertEquals(194.dp, compactLandscapePaneContentWidth(250.dp))
        assertEquals(180.dp, compactLandscapeArtworkSize(
            paneWidth = 250.dp,
            availableHeight = 180.dp,
        ))
    }

    @Test
    fun compactLandscapeGivesTextMoreWidthAndAnchorsTheHeaderToControls() {
        val artworkPane = compactLandscapeArtworkPaneWidth(800.dp)
        assertEquals(360.dp, artworkPane)
        assertEquals(440.dp, 800.dp - artworkPane)
        assertEquals(320.dp, compactLandscapeArtworkSize(artworkPane, 320.dp))
        assertEquals(56.dp, compactLandscapeHeaderTop(400.dp, 52.dp, 200.dp))
        assertEquals(0.dp, compactLandscapeHeaderTop(200.dp, 52.dp, 200.dp))
        assertEquals(360.dp, compactLandscapeLyricsToggleStart(440.dp, 40.dp))
        assertEquals(260.dp, compactLandscapeLyricsToggleStart(320.dp, 40.dp))
    }

    @Test
    fun compactLandscapeBalancesOppositeHorizontalSafeInsets() {
        assertEquals(24.dp, compactLandscapeSidePadding(0.dp, 24.dp))
        assertEquals(0.dp, compactLandscapeSidePadding(24.dp, 0.dp))
        assertEquals(0.dp, compactLandscapeSidePadding(24.dp, 24.dp))
    }

    @Test
    fun widePortraitArtworkSharesContentWidthUnlessHeightIsInsufficient() {
        for (width in listOf(420, 500, 600, 800)) {
            val content = playerContentWidth(width.dp, PLAYER_PORTRAIT_CONTENT_MAX_WIDTH)
            assertEquals(content, fitPlayerArtworkSize(content, content + 32.dp))
            assertEquals(content - 1.dp, fitPlayerArtworkSize(content, content + 31.dp))
            assertEquals((width - 56).coerceAtMost(560).dp, content)
        }
        assertEquals(264.dp, playerContentWidth(320.dp, PLAYER_PORTRAIT_CONTENT_MAX_WIDTH))
        assertEquals(944.dp, playerUnboundedContentWidth(1000.dp))
    }

    @Test
    fun shortWindowsKeepReadableColumnsWhileArtworkShrinks() {
        val width = 400.dp
        val controls = playerContentWidth(width, PLAYER_PORTRAIT_CONTENT_MAX_WIDTH)
        val lyrics = playerContentWidth(width, 560.dp)
        val artwork = fitPlayerArtworkSize(controls, 200.dp)
        assertEquals(344.dp, controls)
        assertEquals(344.dp, lyrics)
        assertTrue(artwork < controls)
        assertEquals(560.dp, playerContentWidth(1000.dp, 560.dp))
        assertEquals(560.dp, fitPlayerArtworkSize(
            playerContentWidth(1000.dp, PLAYER_PORTRAIT_CONTENT_MAX_WIDTH), 1000.dp,
        ))
        assertEquals(0.dp, playerContentWidth(40.dp, 344.dp))
    }

    @Test
    fun playerControlGroupSpacingIsTwentyDpAcrossLayouts() {
        val portrait = playerVerticalSpacing(700.dp, 344.dp)
        assertEquals(12.dp, portrait.headerToArtwork)
        assertEquals(16.dp, portrait.artworkToProgress)
        assertEquals(32.dp, portrait.progressToPrimary)
        assertEquals(20.dp, portrait.controlGroup)
        assertEquals(32.dp, portrait.panelBottom)
        assertEquals(
            32.dp,
            playerVerticalSpacing(
                availableHeight = 700.dp,
                preferredArtworkSize = 344.dp,
                panelBottom = 32.dp,
            ).panelBottom,
        )

        val landscape = landscapePlayerSpacing()
        assertEquals(12.dp, landscape.headerToArtwork)
        assertEquals(12.dp, landscape.artworkToProgress)
        assertEquals(32.dp, landscape.progressToPrimary)
        assertEquals(20.dp, landscape.controlGroup)
        assertEquals(0.dp, landscape.panelBottom)

        assertEquals(20.dp, compactLandscapePlayerSpacing().controlGroup)
        assertEquals(344.dp, fitPlayerArtworkSize(344.dp, 376.dp))
        assertEquals(343.dp, fitPlayerArtworkSize(344.dp, 375.dp))
        assertEquals(0.dp, fitPlayerArtworkSize(344.dp, 32.dp))
    }

    @Test
    fun playerControlsBottomPaddingUsesTheSafeAreaWhenAvailable() {
        assertEquals(32.dp, playerControlsBottomPadding(0.dp))
        assertEquals(36.dp, playerControlsBottomPadding(24.dp))
    }

    @Test
    fun playerSpacingRemainsStableAcrossWindowHeights() {
        val expected = playerVerticalSpacing(0.dp, 344.dp)
        for (height in 1..1200) {
            val current = playerVerticalSpacing(height.dp, 344.dp)
            assertEquals(expected.headerToArtwork, current.headerToArtwork)
            assertEquals(expected.artworkToProgress, current.artworkToProgress)
            assertEquals(expected.progressToPrimary, current.progressToPrimary)
            assertEquals(expected.controlGroup, current.controlGroup)
            assertEquals(expected.panelBottom, current.panelBottom)
        }
    }
}
