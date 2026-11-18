package com.melox.player

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.melox.player.model.PlaybackUiState
import com.melox.player.ui.component.library.playbackArtworkCornerRadius
import com.melox.player.ui.component.library.playbackArtworkShadowBounds
import com.melox.player.ui.component.playback.PLAYER_CONTENT_APPEAR_END_PROGRESS
import com.melox.player.ui.component.playback.PLAYER_CONTENT_APPEAR_START_PROGRESS
import com.melox.player.ui.component.playback.PLAYER_LAYER_HANDOFF_END_PROGRESS
import com.melox.player.ui.component.playback.PLAYER_MINI_CONTENT_FADE_END_PROGRESS
import com.melox.player.ui.component.playback.PlayerSheetDragAxis
import com.melox.player.ui.component.playback.PlayerSheetTransitionState
import com.melox.player.ui.component.playback.SharedContainerCornerRadii
import com.melox.player.ui.component.playback.artworkInsetRect
import com.melox.player.ui.component.playback.fittedArtworkRect
import com.melox.player.ui.component.playback.hasDifferentMetadataSwipeTarget
import com.melox.player.ui.component.playback.hasExpectedMiniMetadataSwipeTarget
import com.melox.player.ui.component.playback.miniMetadataSwipeThresholdDirection
import com.melox.player.ui.component.playback.playerSheetBackgroundAlpha
import com.melox.player.ui.component.playback.playerSheetBarAlpha
import com.melox.player.ui.component.playback.playerSheetDragAxis
import com.melox.player.ui.component.playback.playerSheetDragProgress
import com.melox.player.ui.component.playback.playerSheetDragTarget
import com.melox.player.ui.component.playback.playerSheetGlassVisible
import com.melox.player.ui.component.playback.playerSheetMiniContentAlpha
import com.melox.player.ui.component.playback.playerSheetMiniPlayerAcceptsInput
import com.melox.player.ui.component.playback.playerSheetPageAlpha
import com.melox.player.ui.component.playback.playerSheetPlaceholderArtworkAlpha
import com.melox.player.ui.component.playback.playerSheetResidentHostTranslationY
import com.melox.player.ui.component.playback.playerSheetUsesFullPlayerStatusBar
import com.melox.player.ui.component.playback.playerSheetVerticalTravel
import com.melox.player.ui.component.playback.playerWindowUsesPhysicalScreenCorners
import com.melox.player.ui.component.playback.sharedArtworkRect
import com.melox.player.ui.component.playback.sharedArtworkTargetIsOnscreen
import com.melox.player.ui.component.playback.sharedContainerContentOffset
import com.melox.player.ui.component.playback.sharedContainerCornerRadii
import com.melox.player.ui.component.playback.sharedContainerCornerRadius
import com.melox.player.ui.component.playback.sharedContainerRect
import com.melox.player.ui.component.playback.sharedContainerRenderRect
import com.melox.player.ui.component.playback.sharedMiniPlayerContentOffset
import com.melox.player.ui.component.playback.sharedMiniPlayerControlsTranslationX
import com.melox.player.ui.component.playback.shouldTriggerMiniMetadataSwipeThresholdHaptic
import com.melox.player.ui.screen.playback.fitPlayerArtworkSize
import com.melox.player.ui.screen.playback.playerHeaderArtistText
import com.melox.player.ui.screen.playback.sliderValueAtPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerSheetLogicTest {
    @Test
    fun sliderTapUsesTrackBoundsStepsKeyPointsAndLayoutDirection() {
        assertEquals(
            1f,
            sliderValueAtPosition(
                positionX = 82.5f,
                width = 300,
                height = 30,
                valueRange = 0.6666667f..2f,
                steps = 0,
                keyPoints = listOf(1f),
                magnetThreshold = 0.02f,
                reverseDirection = false,
            ),
            0.0001f,
        )
        assertEquals(
            1.3333333f,
            sliderValueAtPosition(
                positionX = 150f,
                width = 300,
                height = 30,
                valueRange = 0.6666667f..2f,
                steps = 0,
                keyPoints = listOf(1f),
                magnetThreshold = 0.02f,
                reverseDirection = false,
            ),
            0f,
        )
        assertEquals(
            700f,
            sliderValueAtPosition(
                positionX = 215f,
                width = 300,
                height = 30,
                valueRange = 100f..900f,
                steps = 7,
                keyPoints = null,
                magnetThreshold = 0.02f,
                reverseDirection = false,
            ),
            0f,
        )
        (0..8).forEach { index ->
            assertEquals(
                100f + index * 100f,
                sliderValueAtPosition(
                    positionX = 15f + index * 270f / 8f,
                    width = 300,
                    height = 30,
                    valueRange = 100f..900f,
                    steps = 7,
                    keyPoints = null,
                    magnetThreshold = 0.02f,
                    reverseDirection = false,
                ),
                0f,
            )
        }
        assertEquals(
            0.75f,
            sliderValueAtPosition(
                positionX = 82.5f,
                width = 300,
                height = 30,
                valueRange = 0f..1f,
                steps = 0,
                keyPoints = null,
                magnetThreshold = 0.02f,
                reverseDirection = true,
            ),
            0.0001f,
        )
    }

    @Test
    fun playerHeaderArtistsUseThinSlashSeparators() {
        val text = playerHeaderArtistText("Ada / Ben / Cyd")

        assertEquals("Ada / Ben / Cyd", text.text)
        assertEquals(
            listOf("/", "/"),
            text.spanStyles
                .filter { it.item.fontWeight == FontWeight.Thin }
                .map { text.text.substring(it.start, it.end) },
        )
    }

    @Test
    fun sharedPlayerContainerReachesBothMeasuredEndpoints() {
        val miniPlayer = Rect(16f, 700f, 384f, 768f)
        val fullPlayer = Rect(0f, 0f, 400f, 800f)

        assertEquals(miniPlayer, sharedContainerRect(miniPlayer, fullPlayer, 0f))
        assertEquals(fullPlayer, sharedContainerRect(miniPlayer, fullPlayer, 1f))

        val midpoint = sharedContainerRect(miniPlayer, fullPlayer, 0.5f)
        assertEquals(384f, midpoint.width, 0.0001f)
        assertEquals(434f, midpoint.height, 0.0001f)
        assertEquals(200f, midpoint.center.x, 0.0001f)
        assertEquals(567f, midpoint.center.y, 0.0001f)
        assertEquals(8f, midpoint.left, 0.0001f)
        assertEquals(350f, midpoint.top, 0.0001f)
        assertEquals(392f, midpoint.right, 0.0001f)
        assertEquals(784f, midpoint.bottom, 0.0001f)
    }

    @Test
    fun sharedPlayerContainerInterpolatesEveryEdgeInLandscape() {
        val miniPlayer = Rect(420f, 700f, 780f, 764f)
        val fullPlayer = Rect(0f, 0f, 1200f, 800f)

        val midpoint = sharedContainerRect(miniPlayer, fullPlayer, 0.5f)

        assertEquals(780f, midpoint.width, 0.0001f)
        assertEquals(600f, midpoint.center.x, 0.0001f)
        assertEquals(210f, midpoint.left, 0.0001f)
        assertEquals(350f, midpoint.top, 0.0001f)
        assertEquals(990f, midpoint.right, 0.0001f)
        assertEquals(782f, midpoint.bottom, 0.0001f)
    }

    @Test
    fun sharedMiniPlayerControlsStayBoundToTheAnimatedRightInset() {
        val source = Rect(16f, 700f, 384f, 768f)
        val controls = Rect(294f, 714f, 374f, 754f)
        val animated = Rect(0f, 500f, 800f, 768f)

        val translation = sharedMiniPlayerControlsTranslationX(
            sourcePlayerBounds = source,
            animatedPlayerBounds = animated,
            controlsBounds = controls,
        )

        assertEquals(10f, source.right - controls.right, 0.0001f)
        assertEquals(10f, animated.right - (controls.left + translation + controls.width), 0.0001f)
        assertEquals(416f, translation, 0.0001f)
    }

    @Test
    fun sharedMiniPlayerContentKeepsItsScreenXAndTopInset() {
        val source = Rect(16f, 700f, 384f, 768f)
        val content = Rect(22f, 706f, 374f, 762f)

        assertEquals(
            Offset(6f, 6f),
            sharedMiniPlayerContentOffset(
                sourcePlayerBounds = source,
                animatedPlayerBounds = source,
                contentBounds = content,
            ),
        )
        assertEquals(
            Offset(22f, 6f),
            sharedMiniPlayerContentOffset(
                sourcePlayerBounds = source,
                animatedPlayerBounds = Rect(0f, 0f, 800f, 768f),
                contentBounds = content,
            ),
        )
    }

    @Test
    fun settledPlayerContainerOverscansWithoutMovingPageContent() {
        val miniPlayer = Rect(16f, 700f, 384f, 768f)
        val fullPlayer = Rect(0f, 0f, 400f, 800f)
        val rendered = sharedContainerRenderRect(
            source = miniPlayer,
            target = fullPlayer,
            progress = 1f,
            endpointOverscanPx = 3f,
        )

        assertEquals(Rect(-3f, -3f, 403f, 803f), rendered)
        assertEquals(
            Offset(3f, 3f),
            sharedContainerContentOffset(
                renderBounds = rendered,
                contentBounds = fullPlayer,
            ),
        )
        assertEquals(
            sharedContainerRect(miniPlayer, fullPlayer, 0.99f),
            sharedContainerRenderRect(
                source = miniPlayer,
                target = fullPlayer,
                progress = 0.99f,
                endpointOverscanPx = 3f,
            ),
        )
    }

    @Test
    fun settledPlayerContainerUsesExactTargetBoundsWithoutProductionOverscan() {
        val miniPlayer = Rect(16f, 700f, 384f, 768f)
        val fullPlayer = Rect(0f, 0f, 400f, 800f)
        val rendered = sharedContainerRenderRect(
            source = miniPlayer,
            target = fullPlayer,
            progress = 1f,
        )

        assertEquals(fullPlayer, rendered)
        assertEquals(
            Offset.Zero,
            sharedContainerContentOffset(
                renderBounds = rendered,
                contentBounds = fullPlayer,
            ),
        )
    }

    @Test
    fun sharedPlayerContainerTargetsTheAvailableScreenCornerRadius() {
        assertEquals(18f, sharedContainerCornerRadius(18f, 46f, 0f), 0f)
        assertEquals(0f, sharedContainerCornerRadius(18f, 46f, 1f), 0f)
        assertEquals(0f, sharedContainerCornerRadius(18f, 0f, 1f), 0f)
    }

    @Test
    fun sharedPlayerContainerInterpolatesEverySmoothCornerIndependently() {
        assertEquals(
            SharedContainerCornerRadii(
                topStart = 15f,
                topEnd = 21f,
                bottomEnd = 27f,
                bottomStart = 33f,
            ),
            sharedContainerCornerRadii(
                source = SharedContainerCornerRadii(12f, 18f, 24f, 30f),
                target = SharedContainerCornerRadii(18f, 24f, 30f, 36f),
                progress = 0.5f,
            ),
        )
        assertEquals(
            SharedContainerCornerRadii(0f, 0f, 0f, 0f),
            sharedContainerCornerRadii(
                source = SharedContainerCornerRadii(12f, 18f, 24f, 30f),
                target = SharedContainerCornerRadii(18f, 24f, 30f, 36f),
                progress = 1f,
            ),
        )
    }

    @Test
    fun playerWindowUsesPhysicalCornersOnlyWhenItFillsTheMainScreen() {
        assertTrue(
            playerWindowUsesPhysicalScreenCorners(
                currentWidth = 1080,
                currentHeight = 2400,
                maximumWidth = 1080,
                maximumHeight = 2400,
                isInMultiWindowMode = false,
                isInPictureInPictureMode = false,
            ),
        )
        assertFalse(
            playerWindowUsesPhysicalScreenCorners(
                currentWidth = 760,
                currentHeight = 1200,
                maximumWidth = 1080,
                maximumHeight = 2400,
                isInMultiWindowMode = false,
                isInPictureInPictureMode = false,
            ),
        )
        assertFalse(
            playerWindowUsesPhysicalScreenCorners(
                currentWidth = 1080,
                currentHeight = 2400,
                maximumWidth = 1080,
                maximumHeight = 2400,
                isInMultiWindowMode = true,
                isInPictureInPictureMode = false,
            ),
        )
        assertFalse(
            playerWindowUsesPhysicalScreenCorners(
                currentWidth = 1080,
                currentHeight = 2400,
                maximumWidth = 1080,
                maximumHeight = 2400,
                isInMultiWindowMode = false,
                isInPictureInPictureMode = true,
            ),
        )
    }

    @Test
    fun sharedArtworkGrowsRightAndUpWithoutChangingAspectRatio() {
        val thumbnail = Rect(16f, 708f, 64f, 756f)
        val albumArt = Rect(28f, 120f, 372f, 464f)
        val samples = listOf(0f, 0.25f, 0.5f, 0.75f, 1f).map { progress ->
            sharedArtworkRect(thumbnail, albumArt, progress)
        }

        assertEquals(thumbnail, samples.first())
        assertEquals(albumArt, samples.last())
        samples.zipWithNext().forEach { (before, after) ->
            assertTrue(after.center.x > before.center.x)
            assertTrue(after.center.y < before.center.y)
            assertTrue(after.width > before.width)
            assertEquals(before.width / before.height, after.width / after.height, 0.0001f)
        }
    }

    @Test
    fun landscapeArtworkFollowsContainerEdgesWithTheExistingSizeClock() {
        val source = Rect(24f, 380f, 72f, 428f)
        for (target in listOf(Rect(60f, 40f, 300f, 280f), Rect(140f, 90f, 500f, 450f))) {
            for (progress in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
                val bounds = sharedArtworkRect(source, target, progress, trackContainerEdges = true)
                val original = sharedArtworkRect(source, target, progress)
                assertEquals(source.left + (target.left - source.left) * progress, bounds.left, 0.001f)
                assertEquals(source.top + (target.top - source.top) * progress, bounds.top, 0.001f)
                assertEquals(original.width, bounds.width, 0.001f)
                assertEquals(original.height, bounds.height, 0.001f)
            }
            assertEquals(source, sharedArtworkRect(source, target, -1f, trackContainerEdges = true))
            assertEquals(target, sharedArtworkRect(source, target, 2f, trackContainerEdges = true))
        }
    }

    @Test
    fun sharedArtworkPathUsesTheOriginalEasedCenterTrajectory() {
        val thumbnail = Rect(16f, 708f, 64f, 756f)
        val albumArt = Rect(28f, 120f, 372f, 464f)
        val midpoint = sharedArtworkRect(thumbnail, albumArt, 0.5f)

        assertEquals(82f, midpoint.left, 0.0001f)
        assertEquals(513f, midpoint.top, 0.0001f)
        assertEquals(278f, midpoint.right, 0.0001f)
        assertEquals(709f, midpoint.bottom, 0.0001f)
    }

    @Test
    fun sharedArtworkTargetUsesTheMeasuredPagerPositionAsABinaryDecision() {
        val viewport = Rect(0f, 0f, 360f, 800f)
        val visible = Rect(40f, 120f, 320f, 400f)
        val partiallyLeftButStillOnArtworkPage = Rect(-120f, 120f, 160f, 400f)
        val offscreenLeft = Rect(-360f, 120f, -80f, 400f)

        assertTrue(sharedArtworkTargetIsOnscreen(visible, viewport))
        assertTrue(
            sharedArtworkTargetIsOnscreen(partiallyLeftButStillOnArtworkPage, viewport),
        )
        assertFalse(sharedArtworkTargetIsOnscreen(offscreenLeft, viewport))
    }

    @Test
    fun lyricsPageKeepsSharedArtworkDisabledAcrossCloseAndReopen() {
        val state = PlayerSheetTransitionState()

        state.open()
        state.updateFullPlayerArtworkPageSelected(false)
        state.beginFullPlayerDrag()

        assertTrue(state.targetOpen)
        assertFalse(state.sharedArtworkEnabled)

        state.close()
        state.open()

        assertTrue(state.targetOpen)
        assertFalse(state.fullPlayerArtworkPageSelected)
        assertFalse(state.sharedArtworkEnabled)
    }

    @Test
    fun sharedArtworkTargetMatchesVisiblePlaybackInsets() {
        val expandedContainerBounds = Rect(22f, 114f, 378f, 470f)

        val playingBounds = artworkInsetRect(expandedContainerBounds, 8f)
        assertEquals(30f, playingBounds.left, 0.0001f)
        assertEquals(122f, playingBounds.top, 0.0001f)
        assertEquals(370f, playingBounds.right, 0.0001f)
        assertEquals(462f, playingBounds.bottom, 0.0001f)

        val pausedBounds = artworkInsetRect(expandedContainerBounds, 32f)
        assertEquals(54f, pausedBounds.left, 0.0001f)
        assertEquals(146f, pausedBounds.top, 0.0001f)
        assertEquals(346f, pausedBounds.right, 0.0001f)
        assertEquals(438f, pausedBounds.bottom, 0.0001f)
        assertEquals(expandedContainerBounds.center.x, pausedBounds.center.x, 0.0001f)
        assertEquals(expandedContainerBounds.center.y, pausedBounds.center.y, 0.0001f)
        assertEquals(
            expandedContainerBounds.width / expandedContainerBounds.height,
            pausedBounds.width / pausedBounds.height,
            0.0001f,
        )
    }

    @Test
    fun playbackArtworkShadowUsesCenteredRequestedSize() {
        val shadowBounds = playbackArtworkShadowBounds(
            width = 300f,
            height = 200f,
        )

        assertEquals(3f, shadowBounds.left, 0.0001f)
        assertEquals(2f, shadowBounds.top, 0.0001f)
        assertEquals(297f, shadowBounds.right, 0.0001f)
        assertEquals(198f, shadowBounds.bottom, 0.0001f)
    }

    @Test
    fun portraitArtworkFitsAvailableHeightIncludingItsShadow() {
        assertEquals(268.dp, fitPlayerArtworkSize(344.dp, 300.dp))
        assertEquals(344.dp, fitPlayerArtworkSize(344.dp, 700.dp))
        assertEquals(244.dp, fitPlayerArtworkSize(244.dp, 800.dp))
        assertEquals(0.dp, fitPlayerArtworkSize(244.dp, 20.dp))
    }

    @Test
    fun fittedArtworkRectCentersRectangularArtworkInsideTheFrame() {
        val frame = Rect(100f, 200f, 300f, 400f)

        assertEquals(
            Rect(100f, 250f, 300f, 350f),
            fittedArtworkRect(frame, bitmapWidth = 2, bitmapHeight = 1),
        )
        assertEquals(
            Rect(150f, 200f, 250f, 400f),
            fittedArtworkRect(frame, bitmapWidth = 1, bitmapHeight = 2),
        )

        val miniImage = fittedArtworkRect(Rect(0f, 700f, 48f, 748f), 2, 1)
        val fullImage = fittedArtworkRect(Rect(100f, 100f, 400f, 400f), 2, 1)
        assertEquals(miniImage, sharedArtworkRect(miniImage, fullImage, 0f))
        val expandedImage = sharedArtworkRect(miniImage, fullImage, 1f)
        assertEquals(fullImage, expandedImage)
        assertEquals(1f, expandedImage.width / fullImage.width, 0f)
        assertEquals(0f, expandedImage.left - fullImage.left, 0f)
        assertEquals(0f, expandedImage.top - fullImage.top, 0f)
    }

    @Test
    fun rectangularMiniPlayerArtworkUsesASlightlySmallerCornerRadius() {
        val cornerRadius = 8.dp

        assertEquals(
            cornerRadius,
            playbackArtworkCornerRadius(cornerRadius, 512, 512, 1.dp),
        )
        assertEquals(
            7.dp,
            playbackArtworkCornerRadius(cornerRadius, 1024, 512, 1.dp),
        )
        assertEquals(
            7.dp,
            playbackArtworkCornerRadius(cornerRadius, 512, 1024, 1.dp),
        )
    }

    @Test
    fun sharedPlayerLayersKeepThePlaybackBarTransitionUnchanged() {
        assertEquals(0.2f, PLAYER_LAYER_HANDOFF_END_PROGRESS, 0f)
        assertEquals(1f, playerSheetBarAlpha(0f), 0f)
        assertEquals(0f, playerSheetBackgroundAlpha(0f), 0f)
        assertTrue(playerSheetBarAlpha(0.2f) in 0f..1f)
        assertTrue(playerSheetBackgroundAlpha(0.2f) in 0f..1f)
        assertEquals(0f, playerSheetBarAlpha(PLAYER_LAYER_HANDOFF_END_PROGRESS), 0f)
        assertEquals(1f, playerSheetBackgroundAlpha(PLAYER_LAYER_HANDOFF_END_PROGRESS), 0f)
        assertEquals(0f, playerSheetBarAlpha(1f), 0f)
        assertEquals(1f, playerSheetBackgroundAlpha(1f), 0f)
    }

    @Test
    fun placeholderArtworkStartsAtThirtyPercentAndReachesNinetyAtHandoff() {
        assertEquals(0.3f, playerSheetPlaceholderArtworkAlpha(0f), 0f)
        assertTrue(playerSheetPlaceholderArtworkAlpha(0.1f) in 0.3f..0.9f)
        assertEquals(0.9f, playerSheetPlaceholderArtworkAlpha(PLAYER_LAYER_HANDOFF_END_PROGRESS), 0f)
        assertEquals(0.9f, playerSheetPlaceholderArtworkAlpha(1f), 0f)
    }

    @Test
    fun miniPlayerContentFadesOutBeforeTheBarSurfaceHandoffCompletes() {
        assertEquals(1f, playerSheetMiniContentAlpha(0f), 0f)
        assertTrue(playerSheetMiniContentAlpha(0.1f) in 0f..1f)
        assertEquals(0f, playerSheetMiniContentAlpha(PLAYER_MINI_CONTENT_FADE_END_PROGRESS), 0f)
        assertEquals(0f, playerSheetMiniContentAlpha(1f), 0f)
    }

    @Test
    fun fullPlayerContentAppearsAfterBackgroundHandoff() {
        assertEquals(0f, playerSheetPageAlpha(PLAYER_CONTENT_APPEAR_START_PROGRESS), 0f)
        assertEquals(1f, playerSheetPageAlpha(PLAYER_CONTENT_APPEAR_END_PROGRESS), 0f)
        assertTrue(playerSheetPageAlpha(0.55f) in 0f..1f)
    }

    @Test
    fun playerSheetGlassStopsAtContentHandoff() {
        assertTrue(playerSheetGlassVisible(0f))
        assertTrue(playerSheetGlassVisible(PLAYER_LAYER_HANDOFF_END_PROGRESS - 0.001f))
        assertFalse(playerSheetGlassVisible(PLAYER_LAYER_HANDOFF_END_PROGRESS))
        assertFalse(playerSheetGlassVisible(1f))
    }

    @Test
    fun miniPlayerAcceptsInputAsSoonAsTheSharedBarReturns() {
        assertTrue(playerSheetMiniPlayerAcceptsInput(false, false, false, 0.1f))
        assertTrue(playerSheetMiniPlayerAcceptsInput(false, false, false, 0.02f))
        assertTrue(playerSheetMiniPlayerAcceptsInput(false, false, false, 0f))
        assertFalse(
            playerSheetMiniPlayerAcceptsInput(
                false,
                false,
                false,
                PLAYER_LAYER_HANDOFF_END_PROGRESS + 0.001f,
            ),
        )
        assertFalse(playerSheetMiniPlayerAcceptsInput(true, false, false, 0f))
        assertFalse(playerSheetMiniPlayerAcceptsInput(false, true, false, 0.5f))
        assertTrue(playerSheetMiniPlayerAcceptsInput(false, true, true, 0f))
    }

    @Test
    fun activeDragKeepsItsStartingInputHost() {
        assertTrue(
            playerSheetMiniPlayerAcceptsInput(
                targetOpen = true,
                isDragging = true,
                dragStartedFromMiniPlayer = true,
                progress = 0.6f,
            ),
        )
        assertFalse(
            playerSheetMiniPlayerAcceptsInput(
                targetOpen = false,
                isDragging = true,
                dragStartedFromMiniPlayer = false,
                progress = 0.2f,
            ),
        )
    }

    @Test
    fun residentFullPlayerMovesOffscreenWhileTheMiniPlayerOwnsInput() {
        assertEquals(800f, playerSheetResidentHostTranslationY(true, 800), 0f)
        assertEquals(0f, playerSheetResidentHostTranslationY(false, 800), 0f)
    }

    @Test
    fun fullPlayerWaitsForVisibleReadyContentBeforeAcceptingInput() {
        val state = PlayerSheetTransitionState()

        state.open()

        assertTrue(state.fullPlayerHostMounted)
        assertFalse(state.fullPlayerDrawsInPlace)
        assertFalse(state.fullPlayerAcceptsInput)

        val containerBounds = Rect(0f, 0f, 360f, 800f)
        val artworkBounds = Rect(24f, 120f, 336f, 432f)
        state.updateMiniPlayerBounds(Rect(6f, 720f, 354f, 788f))
        state.updateFullPlayerBounds(containerBounds)
        state.updateMiniArtworkBounds(Rect(16f, 730f, 64f, 778f))
        state.updateFullArtworkBounds(artworkBounds)

        assertTrue(state.fullPlayerHostMounted)
        assertTrue(state.fullPlayerAcceptsInput)
    }

    @Test
    fun collapsedTransitionTailKeepsTheFullPlayerHostResidentAfterFirstOpen() {
        val state = PlayerSheetTransitionState(initialProgress = 0.1f)
        state.updateMiniPlayerBounds(Rect(6f, 720f, 354f, 788f))
        state.updateFullPlayerBounds(Rect(0f, 0f, 360f, 800f))
        state.updateMiniArtworkBounds(Rect(16f, 730f, 64f, 778f))
        state.updateFullArtworkBounds(Rect(24f, 120f, 336f, 432f))

        assertTrue(state.isMounted)
        assertTrue(state.miniPlayerAcceptsInput)
        assertTrue(state.fullPlayerHostMounted)
        assertFalse(state.fullPlayerAcceptsInput)
    }

    @Test
    fun fullyExpandedPlayerKeepsItsInputHost() {
        val state = PlayerSheetTransitionState(initialProgress = 1f)
        state.updateMiniPlayerBounds(Rect(6f, 720f, 354f, 788f))
        state.updateFullPlayerBounds(Rect(0f, 0f, 360f, 800f))
        state.updateMiniArtworkBounds(Rect(16f, 730f, 64f, 778f))
        state.updateFullArtworkBounds(Rect(24f, 120f, 336f, 432f))

        assertTrue(state.fullPlayerHostMounted)
        assertTrue(state.fullPlayerAcceptsInput)
    }

    @Test
    fun miniPlayerDragKeepsTheFullPlayerRecordingHostMounted() {
        val state = PlayerSheetTransitionState()
        state.updateMiniPlayerBounds(Rect(6f, 720f, 354f, 788f))
        state.updateFullPlayerBounds(Rect(0f, 0f, 360f, 800f))
        state.updateMiniArtworkBounds(Rect(16f, 730f, 64f, 778f))
        state.updateFullArtworkBounds(Rect(24f, 120f, 336f, 432f))

        state.beginMiniPlayerDrag()
        state.dragBy(-160f)

        assertTrue(state.miniPlayerAcceptsInput)
        assertTrue(state.fullPlayerHostMounted)
        assertFalse(state.fullPlayerAcceptsInput)
    }

    @Test
    fun interruptedDragCanBeTakenOverImmediately() {
        val state = PlayerSheetTransitionState()
        state.updateFullPlayerBounds(Rect(0f, 0f, 360f, 800f))

        state.beginMiniPlayerDrag()
        state.dragBy(-160f)
        state.endDrag(velocityY = -900f)

        assertFalse(state.isDragging)
        state.beginFullPlayerDrag()
        assertTrue(state.isDragging)
        assertEquals(0.2f, state.progress, 0.001f)
    }

    @Test
    fun fullPlayerStatusBarIconsSwitchAtNinetySixPercentInBothDirections() {
        for (progress in listOf(0f, 0.2f, 0.959f, 0.96f, 0.961f, 1f, 0.96f, 0.959f, 0f)) {
            assertEquals(progress >= 0.96f, playerSheetUsesFullPlayerStatusBar(progress))
        }
        assertFalse(playerSheetUsesFullPlayerStatusBar(0.959999f))
        assertTrue(playerSheetUsesFullPlayerStatusBar(0.96f))
    }

    @Test
    fun sharedPlayerDragMapsDirectlyToOneClampedProgress() {
        assertEquals(
            0.25f,
            playerSheetDragProgress(
                startProgress = 0f,
                dragDistanceY = -200f,
                travelDistance = 800f,
            ),
            0f,
        )
        assertEquals(
            0.75f,
            playerSheetDragProgress(
                startProgress = 1f,
                dragDistanceY = 200f,
                travelDistance = 800f,
            ),
            0f,
        )
        assertEquals(0f, playerSheetDragProgress(0f, 200f, 800f), 0f)
        assertEquals(1f, playerSheetDragProgress(1f, -200f, 800f), 0f)
    }

    @Test
    fun sharedPlayerDragUsesMeasuredVerticalContainerTravel() {
        assertEquals(
            720f,
            playerSheetVerticalTravel(
                source = Rect(6f, 720f, 354f, 788f),
                target = Rect(0f, 0f, 360f, 800f),
            ),
            0f,
        )
        assertEquals(
            120f,
            playerSheetVerticalTravel(
                source = Rect(20f, 80f, 380f, 180f),
                target = Rect(0f, 0f, 400f, 300f),
            ),
            0f,
        )
        assertEquals(
            700f,
            playerSheetVerticalTravel(
                source = Rect(420f, 700f, 780f, 764f),
                target = Rect(0f, 0f, 1200f, 800f),
            ),
            0f,
        )
        assertEquals(
            1f,
            playerSheetVerticalTravel(
                source = Rect(0f, 0f, 300f, 64f),
                target = Rect(0f, 0f, 300f, 64f),
            ),
            0f,
        )
    }

    @Test
    fun sharedPlayerDragRequiresVerticalIntentBeyondTouchSlop() {
        assertEquals(PlayerSheetDragAxis.PENDING, playerSheetDragAxis(Offset.Zero, 10f))
        assertEquals(PlayerSheetDragAxis.PENDING, playerSheetDragAxis(Offset(6f, 8f), 10f))
        for (x in listOf(-1f, 1f)) for (y in listOf(-1f, 1f)) {
            assertEquals(PlayerSheetDragAxis.HORIZONTAL, playerSheetDragAxis(Offset(x * 100f, y * 50f), 10f))
            assertEquals(PlayerSheetDragAxis.VERTICAL, playerSheetDragAxis(Offset(x * 100f, y * 60f), 10f))
            assertEquals(PlayerSheetDragAxis.PENDING, playerSheetDragAxis(Offset(x * 11f, y * 8f), 10f))
            assertEquals(PlayerSheetDragAxis.VERTICAL, playerSheetDragAxis(Offset(0f, y * 11f), 10f))
        }
    }

    @Test
    fun sharedPlayerDragReleaseUsesDensityScaledVelocityAndPositionThresholds() {
        for (density in listOf(1f, 2f, 3f)) {
            fun target(velocity: Float, progress: Float, open: Boolean, travelDp: Float = 112f) =
                playerSheetDragTarget(velocity * density, progress, travelDp * density, open, density)
            assertTrue(target(124f, 0.75f, true))
            assertFalse(target(125f, 0.75f, true))
            assertFalse(target(-124f, 0.25f, false))
            assertTrue(target(-125f, 0.25f, false))
            assertTrue(target(0f, 0.5001f, true))
            assertFalse(target(0f, 0.5f, true))
            assertFalse(target(0f, 0.4999f, false))
            assertTrue(target(0f, 0.5f, false))
            // Tiny reverse motion does not override a completed slow drag.
            assertFalse(target(-1f, 0.25f, true))
            assertTrue(target(1f, 0.75f, false))
            assertTrue(target(-125f, 0.25f, true))
            assertFalse(target(125f, 0.75f, false))
            // Endpoint clamps retain slow releases but allow intentional reverse flings.
            assertTrue(target(124f, 1f, true))
            assertFalse(target(-124f, 0f, false))
            assertFalse(target(125f, 1f, true))
            assertTrue(target(-125f, 0f, false))
            assertFalse(target(0f, 0f, true, travelDp = 20f))
            assertTrue(target(0f, 1f, false, travelDp = 20f))
        }
    }

    @Test
    fun playerSheetStateReturnsShortDragsAndCancelsToItsOriginalAnchor() {
        fun state(open: Boolean) = PlayerSheetTransitionState(if (open) 1f else 0f).apply {
            gestureDensity = 2f
            updateMiniPlayerBounds(Rect(0f, 720f, 360f, 800f))
            updateFullPlayerBounds(Rect(0f, 0f, 360f, 800f))
            if (open) beginFullPlayerDrag() else beginMiniPlayerDrag()
        }
        val shortClose = state(true)
        shortClose.dragBy(40f)
        shortClose.endDrag(1f)
        assertTrue(shortClose.targetOpen)
        val shortOpen = state(false)
        shortOpen.dragBy(-40f)
        shortOpen.endDrag(-1f)
        assertFalse(shortOpen.targetOpen)
        val slowClose = state(true)
        slowClose.dragBy(200f)
        slowClose.dragBy(-1f)
        slowClose.endDrag(-1f)
        assertFalse(slowClose.targetOpen)
        for (initial in listOf(0.4f, 0.6f)) {
            val interrupted = PlayerSheetTransitionState(initial).apply {
                updateMiniPlayerBounds(Rect(0f, 720f, 360f, 800f))
                updateFullPlayerBounds(Rect(0f, 0f, 360f, 800f))
                beginFullPlayerDrag()
            }
            interrupted.dragBy(if (initial > 0.5f) 20f else -20f)
            interrupted.endDrag(0f)
            assertEquals(initial > 0.5f, interrupted.targetOpen)
        }
        val cancelled = state(true)
        cancelled.dragBy(200f)
        cancelled.cancelDrag()
        assertTrue(cancelled.targetOpen)
        assertFalse(cancelled.isDragging)
    }

    @Test
    fun metadataSwipeRequiresADifferentQueueTargetForHaptic() {
        val single = PlaybackUiState(
            queue = listOf(playbackQueueItem("one")),
            currentIndex = 0,
        )
        val multiple = PlaybackUiState(
            queue = listOf(playbackQueueItem("one"), playbackQueueItem("two")),
            currentIndex = 0,
        )

        assertFalse(single.hasDifferentMetadataSwipeTarget(-1f))
        assertFalse(single.hasDifferentMetadataSwipeTarget(1f))
        assertEquals(true, multiple.hasDifferentMetadataSwipeTarget(-1f))
        assertEquals(true, multiple.hasDifferentMetadataSwipeTarget(1f))
    }

    @Test
    fun emptyMiniPlayerStateIsNotTreatedAsASwipeTarget() {
        assertFalse(hasExpectedMiniMetadataSwipeTarget(null, null))
        assertFalse(hasExpectedMiniMetadataSwipeTarget("current", null))
        assertTrue(hasExpectedMiniMetadataSwipeTarget("current", "current"))
    }

    @Test
    fun metadataSwipeHapticTracksEachThresholdDirection() {
        assertEquals(
            -1,
            miniMetadataSwipeThresholdDirection(
                offsetPx = -100f,
                commits = true,
                hasDifferentTarget = true,
            ),
        )
        assertEquals(
            0,
            miniMetadataSwipeThresholdDirection(
                offsetPx = 0f,
                commits = false,
                hasDifferentTarget = true,
            ),
        )
        assertEquals(
            1,
            miniMetadataSwipeThresholdDirection(
                offsetPx = 100f,
                commits = true,
                hasDifferentTarget = true,
            ),
        )
        assertEquals(
            0,
            miniMetadataSwipeThresholdDirection(
                offsetPx = 100f,
                commits = true,
                hasDifferentTarget = false,
            ),
        )

        assertTrue(shouldTriggerMiniMetadataSwipeThresholdHaptic(0, -1))
        assertFalse(shouldTriggerMiniMetadataSwipeThresholdHaptic(-1, -1))
        assertFalse(shouldTriggerMiniMetadataSwipeThresholdHaptic(-1, 0))
        assertTrue(shouldTriggerMiniMetadataSwipeThresholdHaptic(0, 1))
        assertTrue(shouldTriggerMiniMetadataSwipeThresholdHaptic(-1, 1))
        assertTrue(shouldTriggerMiniMetadataSwipeThresholdHaptic(1, -1))
    }
}
