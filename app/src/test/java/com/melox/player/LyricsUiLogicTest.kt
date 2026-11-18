package com.melox.player

import androidx.compose.ui.unit.dp
import com.melox.player.data.repository.LyricsRequest
import com.melox.player.data.repository.migrateLegacyLyricFontScale
import com.melox.player.data.repository.normalizeLyricFontWeight
import com.melox.player.model.LyricLine
import com.melox.player.model.LyricsDocument
import com.melox.player.model.LyricsFormat
import com.melox.player.model.LyricsFormatPriority
import com.melox.player.model.LyricsSource
import com.melox.player.model.LyricsSourcePriority
import com.melox.player.model.LyricsUiState
import com.melox.player.ui.screen.playback.LYRICS_MANUAL_FOLLOW_RESUME_DELAY_MS
import com.melox.player.ui.screen.playback.LYRIC_CENTERING_BASE_STIFFNESS
import com.melox.player.ui.screen.playback.LYRIC_CENTERING_MAX_STIFFNESS
import com.melox.player.ui.screen.playback.LYRIC_INACTIVE_TEXT_ALPHA
import com.melox.player.ui.screen.playback.LYRIC_PRIMARY_FONT_SIZE_SP
import com.melox.player.ui.screen.playback.LYRIC_PRIMARY_LINE_HEIGHT_SP
import com.melox.player.ui.screen.playback.LYRIC_TRANSLATION_FONT_SIZE_SP
import com.melox.player.ui.screen.playback.LYRIC_TRANSLATION_LINE_HEIGHT_SP
import com.melox.player.ui.screen.playback.characterMotion
import com.melox.player.ui.screen.playback.characterProgress
import com.melox.player.ui.screen.playback.hiddenLyricsCenterOffsetY
import com.melox.player.ui.screen.playback.lyricBlurRadiusTarget
import com.melox.player.ui.screen.playback.lyricBlurShouldDisableForBrowsing
import com.melox.player.ui.screen.playback.lyricCenterScrollDelta
import com.melox.player.ui.screen.playback.lyricCenteringSpringStiffness
import com.melox.player.ui.screen.playback.lyricDisplayedPositionMs
import com.melox.player.ui.screen.playback.lyricEdgeFadeHeights
import com.melox.player.ui.screen.playback.lyricIntervalProgress
import com.melox.player.ui.screen.playback.lyricLineLayerAlpha
import com.melox.player.ui.screen.playback.lyricLineRenderPositionMs
import com.melox.player.ui.screen.playback.lyricLineVerticalPaddingDp
import com.melox.player.ui.screen.playback.lyricOffscreenTranslationDistance
import com.melox.player.ui.screen.playback.lyricOutgoingSeekCanClear
import com.melox.player.ui.screen.playback.lyricPlaybackPositionMs
import com.melox.player.ui.screen.playback.lyricProgrammaticTranslationStart
import com.melox.player.ui.screen.playback.lyricScrollIsManual
import com.melox.player.ui.screen.playback.lyricSeekPositionIsApplied
import com.melox.player.ui.screen.playback.lyricSeekRequestIsAcknowledged
import com.melox.player.ui.screen.playback.lyricSeekUsesAnimatedCentering
import com.melox.player.ui.screen.playback.lyricTargetScrollOffset
import com.melox.player.ui.screen.playback.lyricTranslationAlpha
import com.melox.player.ui.screen.playback.lyricVerticalDragExceedsTouchSlop
import com.melox.player.ui.screen.playback.lyricWordProgressActiveAlpha
import com.melox.player.ui.screen.playback.progressGestureIsDrag
import com.melox.player.ui.screen.playback.shouldUseWordAnimation
import com.melox.player.ui.screen.playback.stabilizedLyricPlaybackPositionMs
import com.melox.player.ui.screen.playback.wordMotion
import com.melox.player.ui.viewmodel.resolveLyricsStates
import com.melox.player.ui.viewmodel.shouldPublishLyricsResolution
import com.melox.player.ui.viewmodel.shouldShowLyricsLoading
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsUiLogicTest {
    @Test
    fun visibleLyricsControlsUseMatchingTopAndBottomEdgeFades() {
        assertEquals(100f to 100f, lyricEdgeFadeHeights(showBottomFade = true))
    }

    @Test
    fun hiddenLyricsControlsRemoveOnlyTheBottomEdgeFade() {
        assertEquals(100f to 0f, lyricEdgeFadeHeights(showBottomFade = false))
    }

    @Test
    fun lyricTranslationKeepsItsInactiveColorWhenLineFocusChanges() {
        assertEquals(1f, lyricTranslationAlpha(0.4f), 0f)
        assertEquals(0.4f, lyricTranslationAlpha(1f), 0f)
        assertEquals(0.4f, lyricTranslationAlpha(0.8f) * 0.8f, 0.0001f)
    }

    @Test
    fun wordProgressUsesOneAlphaChannelWithoutCompoundingInactiveColor() {
        assertEquals(1f, lyricLineLayerAlpha(0.4f, usesWordProgress = true), 0f)
        assertEquals(0.4f, lyricWordProgressActiveAlpha(0.4f, usesWordProgress = true), 0f)
        assertEquals(1f, lyricLineLayerAlpha(1f, usesWordProgress = true), 0f)
        assertEquals(1f, lyricWordProgressActiveAlpha(1f, usesWordProgress = true), 0f)

        assertEquals(0.4f, lyricLineLayerAlpha(0.4f, usesWordProgress = false), 0f)
        assertEquals(1f, lyricWordProgressActiveAlpha(0.4f, usesWordProgress = false), 0f)
    }

    @Test
    fun hiddenLyricsFocusesAtFortyPercentOfThePageHeight() {
        assertEquals(
            (-132).dp,
            hiddenLyricsCenterOffsetY(
                pageHeight = 800.dp,
                headerTopPadding = 40.dp,
                headerContentHeight = 52.dp,
                headerSpacing = 12.dp,
            ),
        )
    }

    @Test
    fun activeLyricScrollTargetUsesTheViewportCenter() {
        assertEquals(
            0f,
            lyricCenterScrollDelta(
                itemOffset = 380,
                itemSize = 40,
                viewportStartOffset = 0,
                viewportEndOffset = 800,
            ),
            0f,
        )
        assertEquals(
            120f,
            lyricCenterScrollDelta(
                itemOffset = 500,
                itemSize = 40,
                viewportStartOffset = 0,
                viewportEndOffset = 800,
            ),
            0f,
        )
        assertEquals(
            50f,
            lyricCenterScrollDelta(
                itemOffset = 380,
                itemSize = 40,
                viewportStartOffset = 0,
                viewportEndOffset = 800,
                centerOffsetPx = -50f,
            ),
            0f,
        )
        assertEquals(
            80f,
            lyricCenterScrollDelta(
                itemOffset = 60,
                itemSize = 40,
                viewportStartOffset = -500,
                viewportEndOffset = 500,
            ),
            0f,
        )
        assertEquals(
            120,
            lyricTargetScrollOffset(
                itemSize = 40,
                viewportStartOffset = -500,
                viewportEndOffset = 300,
            ),
        )
    }

    @Test
    fun manualLyricBrowsingKeepsBlurDisabledUntilFollowResumes() {
        assertEquals(5_000L, LYRICS_MANUAL_FOLLOW_RESUME_DELAY_MS)
        assertEquals(
            0f,
            lyricBlurRadiusTarget(
                lyricBlurEnabled = true,
                distanceFromFocus = 3,
                isUserBrowsingLyrics = true,
            ),
            0f,
        )
        assertEquals(
            9f,
            lyricBlurRadiusTarget(
                lyricBlurEnabled = true,
                distanceFromFocus = 3,
                isUserBrowsingLyrics = false,
            ),
            0f,
        )
    }

    @Test
    fun lyricIntervalProgressPreservesTimingEndpoints() {
        assertEquals(0f, lyricIntervalProgress(900L, 1_000L, 2_000L), 0f)
        assertEquals(0.5f, lyricIntervalProgress(1_500L, 1_000L, 2_000L), 0f)
        assertEquals(1f, lyricIntervalProgress(2_100L, 1_000L, 2_000L), 0f)
        assertEquals(0f, lyricIntervalProgress(999L, 1_000L, 1_000L), 0f)
        assertEquals(1f, lyricIntervalProgress(1_000L, 1_000L, 1_000L), 0f)
    }

    @Test
    fun lyricsUseTenDpSpacingWithTranslationAndTwelveDpWithoutIt() {
        assertEquals(
            10f,
            lyricLineVerticalPaddingDp(
                hasTimedWords = true,
                hasTranslation = true,
                showLyricsTranslation = true,
            ),
            0f,
        )
        assertEquals(
            10f,
            lyricLineVerticalPaddingDp(
                hasTimedWords = false,
                hasTranslation = true,
                showLyricsTranslation = true,
            ),
            0f,
        )
        assertEquals(
            12f,
            lyricLineVerticalPaddingDp(
                hasTimedWords = true,
                hasTranslation = false,
                showLyricsTranslation = true,
            ),
            0f,
        )
        assertEquals(
            12f,
            lyricLineVerticalPaddingDp(
                hasTimedWords = true,
                hasTranslation = true,
                showLyricsTranslation = false,
            ),
            0f,
        )
        assertEquals(
            12f,
            lyricLineVerticalPaddingDp(
                hasTimedWords = false,
                hasTranslation = false,
                showLyricsTranslation = true,
            ),
            0f,
        )
        assertEquals(
            12f,
            lyricLineVerticalPaddingDp(
                hasTimedWords = false,
                hasTranslation = true,
                showLyricsTranslation = false,
            ),
            0f,
        )
    }

    @Test
    fun unrevealedWordByWordTextMatchesInactiveLyricStrength() {
        assertEquals(0.4f, LYRIC_INACTIVE_TEXT_ALPHA, 0f)
    }

    @Test
    fun lyricSeekTargetStaysActiveUntilTheSmoothClockReachesIt() {
        assertFalse(
            lyricSeekPositionIsApplied(
                currentPositionMs = 10_000L,
                seekPositionMs = 50_000L,
            ),
        )
        assertTrue(
            lyricSeekPositionIsApplied(
                currentPositionMs = 49_800L,
                seekPositionMs = 50_000L,
            ),
        )
    }

    @Test
    fun lyricSeekTargetWaitsForAFreshPlaybackSample() {
        assertFalse(
            lyricSeekRequestIsAcknowledged(
                requestKey = 1,
                positionUpdateAnchorElapsedRealtimeMs = 2_000L,
                positionUpdateElapsedRealtimeMs = 1_900L,
                currentPositionMs = 50_000L,
                seekPositionMs = 50_000L,
            ),
        )
        assertTrue(
            lyricSeekRequestIsAcknowledged(
                requestKey = 1,
                positionUpdateAnchorElapsedRealtimeMs = 2_000L,
                positionUpdateElapsedRealtimeMs = 2_100L,
                currentPositionMs = 49_900L,
                seekPositionMs = 50_000L,
            ),
        )
    }

    @Test
    fun lyricClockStaysMonotonicAcrossOrdinaryBackwardSamples() {
        assertEquals(
            10_116.666667,
            stabilizedLyricPlaybackPositionMs(
                previousPositionMs = 10_100.0,
                sampledPositionMs = 10_000L,
                frameAdvanceNanos = 16_666_667L,
                playbackSpeed = 1f,
            ),
            0.000001,
        )
        assertEquals(
            10_116.666667,
            stabilizedLyricPlaybackPositionMs(
                previousPositionMs = 10_100.0,
                sampledPositionMs = 5_000L,
                frameAdvanceNanos = 16_666_667L,
                playbackSpeed = 1f,
            ),
            0.000001,
        )
        assertEquals(
            10_200.0,
            stabilizedLyricPlaybackPositionMs(
                previousPositionMs = 10_100.0,
                sampledPositionMs = 10_200L,
                frameAdvanceNanos = 16_666_667L,
                playbackSpeed = 1f,
            ),
            0.0,
        )
    }

    @Test
    fun lyricClockPreservesSubMillisecondFrameTimeAcrossRefreshRates() {
        var sixtyHertzPositionMs = 10_000.0
        repeat(60) {
            sixtyHertzPositionMs = stabilizedLyricPlaybackPositionMs(
                previousPositionMs = sixtyHertzPositionMs,
                sampledPositionMs = 10_000L,
                frameAdvanceNanos = 16_666_667L,
                playbackSpeed = 1f,
            )
        }
        assertEquals(11_000.00002, sixtyHertzPositionMs, 0.000001)

        var oneTwentyHertzPositionMs = 10_000.0
        repeat(120) {
            oneTwentyHertzPositionMs = stabilizedLyricPlaybackPositionMs(
                previousPositionMs = oneTwentyHertzPositionMs,
                sampledPositionMs = 10_000L,
                frameAdvanceNanos = 8_333_333L,
                playbackSpeed = 1f,
            )
        }
        assertEquals(10_999.99996, oneTwentyHertzPositionMs, 0.000001)
    }

    @Test
    fun lyricPlaybackStateEndsBeforeTheNextLineBecomesActive() {
        val document = LyricsDocument(
            lines = listOf(
                LyricLine(
                    agent = "main",
                    startTimeMs = 1_000L,
                    endTimeMs = 2_000L,
                    text = "First",
                    words = emptyList(),
                    translation = null,
                ),
                LyricLine(
                    agent = "main",
                    startTimeMs = 4_000L,
                    endTimeMs = 5_000L,
                    text = "Second",
                    words = emptyList(),
                    translation = null,
                ),
            ),
            format = LyricsFormat.TTML,
            source = LyricsSource.SIDECAR,
        )

        assertEquals(0, document.visualLineIndex(3_500L))
        assertEquals(1, document.focusLineIndex(3_500L))
        assertEquals(-1, document.currentLineIndex(3_500L))
        assertEquals(0, document.currentLineIndex(1_999L))
        assertEquals(-1, document.currentLineIndex(2_000L))
        assertEquals(1, document.currentLineIndex(4_000L))
    }

    @Test
    fun lyricPriorityChangePublishesOnlyWhenTheResolvedSourceChanges() {
        val embeddedFirst = LyricsRequest(
            mediaId = "1",
            contentUri = "content://media/external_primary/audio/media/1",
            fileName = "Song.flac",
            folderPath = "/Music/Album",
            durationMs = 10_000L,
            sourcePriority = LyricsSourcePriority.EMBEDDED,
            formatPriority = LyricsFormatPriority.TTML,
        )
        val sidecarFirst = embeddedFirst.copy(sourcePriority = LyricsSourcePriority.SIDECAR)
        val embeddedLyrics = LyricsDocument(
            lines = emptyList(),
            format = LyricsFormat.LRC,
            source = LyricsSource.EMBEDDED,
        )
        val ttmlSidecarLyrics = LyricsDocument(
            lines = emptyList(),
            format = LyricsFormat.TTML,
            source = LyricsSource.SIDECAR,
        )
        val lrcSidecarLyrics = ttmlSidecarLyrics.copy(format = LyricsFormat.LRC)

        assertFalse(shouldShowLyricsLoading(embeddedFirst, sidecarFirst))
        assertFalse(
            shouldPublishLyricsResolution(
                previousRequest = embeddedFirst,
                request = sidecarFirst,
                previousDocument = embeddedLyrics,
                document = embeddedLyrics,
            ),
        )
        assertTrue(
            shouldPublishLyricsResolution(
                previousRequest = embeddedFirst,
                request = sidecarFirst,
                previousDocument = embeddedLyrics,
                document = ttmlSidecarLyrics,
            ),
        )
        assertTrue(
            shouldPublishLyricsResolution(
                previousRequest = sidecarFirst,
                request = sidecarFirst.copy(
                    formatPriority = LyricsFormatPriority.LRC,
                ),
                previousDocument = ttmlSidecarLyrics,
                document = lrcSidecarLyrics,
            ),
        )
    }

    @Test
    fun lyricResolutionFlowCanPublishFromCollectLatestWithoutAFlowInvariantCrash() = runBlocking {
        val states = flowOf<LyricsRequest?>(null)
            .resolveLyricsStates { error("No lyrics should be loaded without a request") }
            .toList()

        assertEquals(listOf(LyricsUiState.Unavailable), states)
    }

    @Test
    fun lyricSeekKeepsTheOutgoingLineAtItsCapturedProgress() {
        assertEquals(
            12_345L,
            lyricLineRenderPositionMs(
                lineIndex = 2,
                displayedPositionMs = 40_000L,
                outgoingSeekLineIndex = 2,
                outgoingSeekPositionMs = 12_345L,
            ),
        )
        assertEquals(
            40_000L,
            lyricLineRenderPositionMs(
                lineIndex = 5,
                displayedPositionMs = 40_000L,
                outgoingSeekLineIndex = 2,
                outgoingSeekPositionMs = 12_345L,
            ),
        )
    }

    @Test
    fun lyricSeekClearsTheOutgoingLineOnlyAfterItsFadeSettles() {
        assertFalse(
            lyricOutgoingSeekCanClear(
                lineIndex = 2,
                outgoingSeekLineIndex = 2,
                currentLineIndex = 5,
                settledAlpha = 0.5f,
            ),
        )
        assertTrue(
            lyricOutgoingSeekCanClear(
                lineIndex = 2,
                outgoingSeekLineIndex = 2,
                currentLineIndex = 5,
                settledAlpha = 0.4f,
            ),
        )
    }

    @Test
    fun lyricClockUsesTheMonotonicSampleAnchorWithoutDroppingLongFrames() {
        assertEquals(
            20_000L,
            lyricPlaybackPositionMs(
                positionMs = 10_000L,
                positionUpdateElapsedRealtimeMs = 1_000L,
                nowElapsedRealtimeMs = 11_000L,
                isPlaying = true,
                playbackSpeed = 1f,
            ),
        )
        assertEquals(
            11_000L,
            lyricPlaybackPositionMs(
                positionMs = 10_000L,
                positionUpdateElapsedRealtimeMs = 1_000L,
                nowElapsedRealtimeMs = 1_500L,
                isPlaying = true,
                playbackSpeed = 2f,
            ),
        )
        assertEquals(
            10_000L,
            lyricPlaybackPositionMs(
                positionMs = 10_000L,
                positionUpdateElapsedRealtimeMs = 1_000L,
                nowElapsedRealtimeMs = 11_000L,
                isPlaying = false,
                playbackSpeed = 1f,
            ),
        )
    }

    @Test
    fun lyricPlaybackAndTapUseCenteringButPreviewSnapsToTheTarget() {
        assertTrue(lyricSeekUsesAnimatedCentering(true, centerOffsetUnchanged = true))
        assertFalse(lyricSeekUsesAnimatedCentering(true, centerOffsetUnchanged = false))
        assertFalse(lyricSeekUsesAnimatedCentering(false, centerOffsetUnchanged = true))
        assertFalse(lyricSeekUsesAnimatedCentering(true, centerOffsetUnchanged = true, active = false))
        assertFalse(
            lyricSeekUsesAnimatedCentering(
                hasPositionedInitialFocus = true,
                centerOffsetUnchanged = true,
                isPreviewing = true,
            ),
        )
    }

    @Test
    fun progressPreviewDirectlyOwnsTheDisplayedLyricPosition() {
        assertEquals(
            42_000L,
            lyricDisplayedPositionMs(
                previewPositionMs = 42_000L,
                seekRequestPending = true,
                seekPositionMs = 30_000L,
                smoothPositionMs = 10_000L,
            ),
        )
        assertEquals(
            30_000L,
            lyricDisplayedPositionMs(
                previewPositionMs = null,
                seekRequestPending = true,
                seekPositionMs = 30_000L,
                smoothPositionMs = 10_000L,
            ),
        )
        assertEquals(
            10_000L,
            lyricDisplayedPositionMs(
                previewPositionMs = null,
                seekRequestPending = false,
                seekPositionMs = 30_000L,
                smoothPositionMs = 10_000L,
            ),
        )
    }

    @Test
    fun progressTapDoesNotStartALyricPreviewSession() {
        assertFalse(progressGestureIsDrag(horizontalDistancePx = 7f, touchSlopPx = 8f))
        assertTrue(progressGestureIsDrag(horizontalDistancePx = 8f, touchSlopPx = 8f))
        assertTrue(progressGestureIsDrag(horizontalDistancePx = -12f, touchSlopPx = 8f))
    }

    @Test
    fun onlyUserOwnedListScrollingStartsManualLyricBrowsing() {
        assertTrue(
            lyricScrollIsManual(
                listIsScrolling = true,
                scrollInCode = false,
            ),
        )
        assertFalse(
            lyricScrollIsManual(
                listIsScrolling = true,
                scrollInCode = true,
            ),
        )
        assertFalse(
            lyricScrollIsManual(
                listIsScrolling = false,
                scrollInCode = false,
            ),
        )
    }

    @Test
    fun onlyDominantVerticalLyricMovementDisablesBlur() {
        assertTrue(
            lyricVerticalDragExceedsTouchSlop(
                horizontalDeltaPx = 0f,
                verticalDeltaPx = 9f,
                touchSlopPx = 8f,
            ),
        )
        assertFalse(
            lyricVerticalDragExceedsTouchSlop(
                horizontalDeltaPx = 7f,
                verticalDeltaPx = 7f,
                touchSlopPx = 8f,
            ),
        )
        assertFalse(
            lyricVerticalDragExceedsTouchSlop(
                horizontalDeltaPx = 12f,
                verticalDeltaPx = 5f,
                touchSlopPx = 8f,
            ),
        )
        assertFalse(
            lyricVerticalDragExceedsTouchSlop(
                horizontalDeltaPx = 0f,
                verticalDeltaPx = 8f,
                touchSlopPx = 8f,
            ),
        )
    }

    @Test
    fun visibleLyricTargetKeepsVisualContinuityAfterTheListSnaps() {
        assertEquals(
            72f,
            lyricProgrammaticTranslationStart(
                currentTranslationY = 12f,
                measuredScrollDelta = 60f,
                targetRenderIndex = 8,
                previousRenderIndex = 6,
                offscreenTravelPx = 96f,
            ),
            0f,
        )
        assertEquals(
            -20f,
            lyricProgrammaticTranslationStart(
                currentTranslationY = 10f,
                measuredScrollDelta = -30f,
                targetRenderIndex = 4,
                previousRenderIndex = 6,
                offscreenTravelPx = 96f,
            ),
            0f,
        )
    }

    @Test
    fun offscreenAndRepeatedLyricTargetsRetargetTheVisualAnimation() {
        assertEquals(
            96f,
            lyricProgrammaticTranslationStart(
                currentTranslationY = 0f,
                measuredScrollDelta = null,
                targetRenderIndex = 8,
                previousRenderIndex = 2,
                offscreenTravelPx = 96f,
            ),
            0f,
        )
        assertEquals(
            -72f,
            lyricProgrammaticTranslationStart(
                currentTranslationY = 24f,
                measuredScrollDelta = null,
                targetRenderIndex = 1,
                previousRenderIndex = 8,
                offscreenTravelPx = 96f,
            ),
            0f,
        )
        assertEquals(
            24f,
            lyricProgrammaticTranslationStart(
                currentTranslationY = 24f,
                measuredScrollDelta = null,
                targetRenderIndex = 8,
                previousRenderIndex = 8,
                offscreenTravelPx = 96f,
            ),
            0f,
        )
    }

    @Test
    fun offscreenLyricTargetBeginsOutsideTheViewportBeforeCentering() {
        assertEquals(
            420f,
            lyricOffscreenTranslationDistance(
                viewportStartOffset = 0,
                viewportEndOffset = 800,
                itemSize = 40,
            ),
            0f,
        )
        assertEquals(
            370f,
            lyricOffscreenTranslationDistance(
                viewportStartOffset = -100,
                viewportEndOffset = 600,
                itemSize = 40,
            ),
            0f,
        )
    }

    @Test
    fun blurIsDisabledOnlyAfterARealUserDragStartsBrowsing() {
        assertFalse(
            lyricBlurShouldDisableForBrowsing(
                isUserBrowsingLyrics = false,
                isManualScrolling = false,
            ),
        )
        assertFalse(
            lyricBlurShouldDisableForBrowsing(
                isUserBrowsingLyrics = false,
                isManualScrolling = true,
            ),
        )
        assertTrue(
            lyricBlurShouldDisableForBrowsing(
                isUserBrowsingLyrics = true,
                isManualScrolling = false,
            ),
        )
        assertTrue(
            lyricBlurShouldDisableForBrowsing(
                isUserBrowsingLyrics = true,
                isManualScrolling = true,
            ),
        )
    }

    @Test
    fun denseLyricIntervalsOnlyIncreaseCenteringSpeed() {
        assertEquals(
            LYRIC_CENTERING_BASE_STIFFNESS,
            lyricCenteringSpringStiffness(null),
            0f,
        )
        assertEquals(
            LYRIC_CENTERING_BASE_STIFFNESS,
            lyricCenteringSpringStiffness(1_000L),
            0f,
        )
        assertEquals(
            LYRIC_CENTERING_BASE_STIFFNESS,
            lyricCenteringSpringStiffness(2_000L),
            0f,
        )
        assertTrue(lyricCenteringSpringStiffness(540L) > LYRIC_CENTERING_BASE_STIFFNESS)
        assertTrue(lyricCenteringSpringStiffness(210L) > lyricCenteringSpringStiffness(540L))
        assertEquals(
            LYRIC_CENTERING_MAX_STIFFNESS,
            lyricCenteringSpringStiffness(210L),
            0f,
        )
    }

    @Test
    fun legacyLyricFontScaleMapsOldEightyPercentToNewHundredPercent() {
        assertEquals(1f, migrateLegacyLyricFontScale(0.8f), 0f)
        assertEquals(1.25f, migrateLegacyLyricFontScale(1f), 0f)
        assertEquals(0.6666667f, migrateLegacyLyricFontScale(0.4f), 0.0000001f)
        assertEquals(1.5f, migrateLegacyLyricFontScale(1.2f), 0f)
    }

    @Test
    fun lyricFontWeightUsesHundredPointStepsWithinSupportedRange() {
        assertEquals(100, normalizeLyricFontWeight(50))
        assertEquals(100, normalizeLyricFontWeight(149))
        assertEquals(200, normalizeLyricFontWeight(150))
        assertEquals(400, normalizeLyricFontWeight(400))
        assertEquals(900, normalizeLyricFontWeight(950))
        (1..9).forEach { step ->
            val weight = step * 100
            assertEquals(weight, normalizeLyricFontWeight(weight))
        }
    }

    @Test
    fun lyricFontScaleKeepsPercentageRangeForRequestedSpSizes() {
        assertEquals(24f, LYRIC_PRIMARY_FONT_SIZE_SP, 0f)
        assertEquals(28f, LYRIC_PRIMARY_LINE_HEIGHT_SP, 0f)
        assertEquals(16f, LYRIC_TRANSLATION_FONT_SIZE_SP, 0f)
        assertEquals(22f, LYRIC_TRANSLATION_LINE_HEIGHT_SP, 0f)
        assertEquals(16f, LYRIC_PRIMARY_FONT_SIZE_SP * 0.6666667f, 0.0001f)
        assertEquals(24f, LYRIC_PRIMARY_FONT_SIZE_SP, 0.0001f)
        assertEquals(48f, LYRIC_PRIMARY_FONT_SIZE_SP * 2f, 0.0001f)
        assertEquals(
            2f / 3f,
            LYRIC_TRANSLATION_FONT_SIZE_SP / LYRIC_PRIMARY_FONT_SIZE_SP,
            0.0001f,
        )
    }

    @Test
    fun wordMotionProvidesScaleOffsetAndGlow() {
        val start = wordMotion(progress = 0f, durationMs = 2_000L)
        val middle = wordMotion(progress = 0.5f, durationMs = 2_000L)
        val end = wordMotion(progress = 1f, durationMs = 2_000L)

        assertTrue(start.scale >= 1f)
        assertTrue(middle.scale > 1f)
        assertEquals(0f, start.offsetYPx, 0f)
        assertTrue(middle.offsetYPx < start.offsetYPx)
        assertTrue(middle.glowRadius > 0f)
        assertTrue(middle.glowAlpha > 0f)
        assertEquals(start.glowRadius, middle.glowRadius, 0f)
        assertEquals(middle.glowRadius, end.glowRadius, 0f)
        assertEquals(0f, end.glowAlpha, 0.000001f)
    }

    @Test
    fun charactersStartAcrossTheFirstThirtyTwoPercentOfASyllable() {
        assertEquals(
            100f / 680f,
            characterProgress(
                positionMs = 1_100L,
                wordStartTimeMs = 1_000L,
                wordEndTimeMs = 2_000L,
                characterIndex = 0,
                characterCount = 3,
            ),
            0.000001f,
        )
        assertEquals(
            0f,
            characterProgress(
                positionMs = 1_100L,
                wordStartTimeMs = 1_000L,
                wordEndTimeMs = 2_000L,
                characterIndex = 1,
                characterCount = 3,
            ),
            0f,
        )
        assertEquals(
            0f,
            characterProgress(
                positionMs = 1_100L,
                wordStartTimeMs = 1_000L,
                wordEndTimeMs = 2_000L,
                characterIndex = 2,
                characterCount = 3,
            ),
            0f,
        )
        val finalMotion = characterMotion(
            positionMs = 2_000L,
            wordStartTimeMs = 1_000L,
            wordEndTimeMs = 2_000L,
            characterIndex = 2,
            characterCount = 3,
        )
        assertEquals(0f, finalMotion.glowAlpha, 0.000001f)
    }

    @Test
    fun wordAnimationEligibilityIncludesCjkButExcludesFastWords() {
        assertTrue(
            shouldUseWordAnimation(
                content = "中文",
                durationMs = 2_000L,
            ),
        )
        assertFalse(
            shouldUseWordAnimation(
                content = "hello",
                durationMs = 900L,
            ),
        )
        assertTrue(
            shouldUseWordAnimation(
                content = "word",
                durationMs = 2_000L,
            ),
        )
    }
}
