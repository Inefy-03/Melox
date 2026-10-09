package com.melox.player

import android.Manifest
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.melox.player.data.library.AlbumGridStyle
import com.melox.player.data.library.AlbumSortConfig
import com.melox.player.data.library.AlbumSortField
import com.melox.player.data.library.ArtistSortConfig
import com.melox.player.data.library.ArtistSortField
import com.melox.player.data.library.FolderSortConfig
import com.melox.player.data.library.FolderSortField
import com.melox.player.data.library.LocalAudioProperties
import com.melox.player.data.library.MusicLibrarySnapshotCodec
import com.melox.player.data.library.MusicSortConfig
import com.melox.player.data.library.MusicSortField
import com.melox.player.data.library.buildAlbumGroups
import com.melox.player.data.library.buildAlbumDiscSections
import com.melox.player.data.library.buildArtistGroups
import com.melox.player.data.library.buildFolderGroups
import com.melox.player.data.library.createMusicSortKeys
import com.melox.player.data.library.displayArtistName
import com.melox.player.data.library.filterAlbums
import com.melox.player.data.library.filterArtists
import com.melox.player.data.library.filterFolders
import com.melox.player.data.library.filterMusicTracks
import com.melox.player.data.library.folderDisplayPath
import com.melox.player.data.library.hasReusableAudioProperties
import com.melox.player.data.library.japaneseKanaToRomaji
import com.melox.player.data.library.normalizeAudioProperties
import com.melox.player.data.library.parseAudioTagProperties
import com.melox.player.data.library.normalizeMusicFolderPath
import com.melox.player.data.library.sortAlbums
import com.melox.player.data.library.sortArtists
import com.melox.player.data.library.sortFolders
import com.melox.player.data.library.sortMusicTracks
import com.melox.player.data.library.splitArtistNames
import com.melox.player.data.playback.PlaybackSnapshotCodec
import com.melox.player.data.playback.retainReadableItems
import com.melox.player.data.playback.toStartupPlaybackPreview
import com.melox.player.data.repository.migrateLegacyLyricFontScale
import com.melox.player.data.repository.normalizeLyricFontWeight
import com.melox.player.data.repository.resolveAlbumGridStyleOrdinal
import com.melox.player.data.repository.LyricsRequest
import com.melox.player.model.AppSettings
import com.melox.player.model.AudioQuality
import com.melox.player.model.BottomBarStyle
import com.melox.player.model.DynamicColorSource
import com.melox.player.model.MusicTrack
import com.melox.player.model.NavigationTransitionStyle
import com.melox.player.model.LyricLine
import com.melox.player.model.LyricsDocument
import com.melox.player.model.LyricsFormat
import com.melox.player.model.LyricsSidecarFormatPriority
import com.melox.player.model.LyricsSource
import com.melox.player.model.LyricsSourcePriority
import com.melox.player.model.LyricsUiState
import com.melox.player.model.PlaybackMode
import com.melox.player.model.PlaybackBackgroundStyle
import com.melox.player.model.PlaybackQueueItem
import com.melox.player.model.PlaybackSnapshot
import com.melox.player.model.PlaybackUiState
import com.melox.player.model.ScanStatus
import com.melox.player.model.ThemeMode
import com.melox.player.model.resolveAudioQuality
import com.melox.player.model.withTrackMetadata
import com.melox.player.playback.isValidQueueIndex
import com.melox.player.playback.buildHomeRecommendationPlaybackQueue
import com.melox.player.playback.displayedPlaybackMode
import com.melox.player.playback.hasSameQueueSlots
import com.melox.player.playback.isValidQueueMove
import com.melox.player.playback.nextPlaybackMode
import com.melox.player.playback.nextQueueInsertionIndex
import com.melox.player.playback.playbackQueueReplacement
import com.melox.player.playback.reorderQueueForPlaybackMode
import com.melox.player.playback.reconcileValidatedPlaybackSnapshot
import com.melox.player.playback.sourceOrderForPlayNext
import com.melox.player.playback.toInitialPlaybackState
import com.melox.player.ui.component.library.findAlphabetTargetIndex
import com.melox.player.ui.component.library.fitArtworkDimensions
import com.melox.player.ui.component.library.formatDuration
import com.melox.player.ui.component.library.fullPlayerArtworkTargetSizePx
import com.melox.player.ui.screen.playback.artworkCrossfadeDurationMillis
import com.melox.player.ui.component.library.playbackArtworkShadowBounds
import com.melox.player.ui.component.library.artworkCacheFileStem
import com.melox.player.ui.component.library.createArtworkCacheKey
import com.melox.player.ui.component.library.AlphabetSections
import com.melox.player.ui.component.library.audioFormatLabel
import com.melox.player.ui.component.library.displayFileLocation
import com.melox.player.ui.component.library.participatingArtistGroups
import com.melox.player.ui.component.library.playbackArtworkCornerRadius
import com.melox.player.ui.component.library.responsiveGridColumnCount
import com.melox.player.ui.component.library.snapshotArtworkDiskCacheEntries
import com.melox.player.ui.component.playback.hasDifferentMetadataSwipeTarget
import com.melox.player.ui.component.playback.hasExpectedMiniMetadataSwipeTarget
import com.melox.player.ui.viewmodel.shouldPublishLyricsResolution
import com.melox.player.ui.viewmodel.shouldShowLyricsLoading
import com.melox.player.ui.viewmodel.resolveLyricsStates
import com.melox.player.ui.component.playback.KenBurnsFrame
import com.melox.player.ui.component.playback.DYNAMIC_FLOW_ARTWORK_SATURATION
import com.melox.player.ui.component.playback.DYNAMIC_FLOW_BACKGROUND_DARKEN_AMOUNT
import com.melox.player.ui.component.playback.advanceDynamicFlowClockMillis
import com.melox.player.ui.component.playback.createRenderScriptBlurBoxSizes
import com.melox.player.ui.component.playback.dynamicFlowDownsampleFactor
import com.melox.player.ui.component.playback.interpolateKenBurnsFrame
import com.melox.player.ui.component.playback.miniMetadataSwipeThresholdDirection
import com.melox.player.ui.component.playback.shouldTriggerMiniMetadataSwipeThresholdHaptic
import com.melox.player.ui.component.liquid.floatingNavigationBarBottomPadding
import com.melox.player.ui.shouldClearSearchFocusAfterImeDismissed
import com.melox.player.ui.floatingBottomBarBottomPadding
import com.melox.player.ui.floatingMiniPlayerBottomPaddingWhenNavigationIsHidden
import com.melox.player.ui.component.playback.PLAYER_LAYER_HANDOFF_END_PROGRESS
import com.melox.player.ui.component.playback.PLAYER_MINI_CONTENT_FADE_END_PROGRESS
import com.melox.player.ui.component.playback.PLAYER_CONTENT_APPEAR_START_PROGRESS
import com.melox.player.ui.component.playback.PLAYER_CONTENT_APPEAR_END_PROGRESS
import com.melox.player.ui.component.playback.playerSheetBarAlpha
import com.melox.player.ui.component.playback.playerSheetMiniContentAlpha
import com.melox.player.ui.component.playback.playerSheetBackgroundAlpha
import com.melox.player.ui.component.playback.playerSheetDragProgress
import com.melox.player.ui.component.playback.playerSheetDragTarget
import com.melox.player.ui.component.playback.playerSheetVerticalTravel
import com.melox.player.ui.component.playback.playerSheetGlassVisible
import com.melox.player.ui.component.playback.playerSheetMiniPlayerAcceptsInput
import com.melox.player.ui.component.playback.playerSheetResidentHostTranslationY
import com.melox.player.ui.component.playback.scaledDynamicFlowTimeMs
import com.melox.player.ui.component.playback.playerSheetPageAlpha
import com.melox.player.ui.component.playback.playerSheetUsesFullPlayerStatusBar
import com.melox.player.ui.component.playback.playerWindowUsesPhysicalScreenCorners
import com.melox.player.ui.component.playback.fittedArtworkRect
import com.melox.player.ui.component.playback.artworkInsetRect
import com.melox.player.ui.component.playback.sharedArtworkRect
import com.melox.player.ui.component.playback.sharedArtworkTargetIsOnscreen
import com.melox.player.ui.component.playback.sharedContainerContentOffset
import com.melox.player.ui.component.playback.sharedContainerCornerRadius
import com.melox.player.ui.component.playback.sharedContainerCornerRadii
import com.melox.player.ui.component.playback.sharedContainerRect
import com.melox.player.ui.component.playback.sharedContainerRenderRect
import com.melox.player.ui.component.playback.sharedMiniPlayerContentOffset
import com.melox.player.ui.component.playback.sharedMiniPlayerControlsTranslationX
import com.melox.player.ui.component.playback.SharedContainerCornerRadii
import com.melox.player.ui.component.playlist.PlaylistArtworkLayout
import com.melox.player.ui.component.playlist.playlistArtworkLayout
import com.melox.player.ui.floatingBottomBarAvailableWidth
import com.melox.player.ui.component.playback.PlayerSheetTransitionState
import com.melox.player.ui.navigation.predictiveBackHandlerEnabled
import com.melox.player.ui.navigation.ordinaryBackHandlerEnabled
import com.melox.player.ui.navigation.navigationTransitionCornerRadius
import com.melox.player.ui.isMiuixWideLayout
import com.melox.player.ui.requiredAudioPermission
import com.melox.player.ui.resolveBottomBarStyle
import com.melox.player.ui.rootPagerUserScrollEnabled
import com.melox.player.ui.shouldUseNavigationRail
import com.melox.player.ui.shouldShowNavigation
import com.melox.player.ui.usesNormalMiniPlayerChrome
import com.melox.player.ui.screen.home.homePlaylistGridColumnCount
import com.melox.player.ui.screen.home.homeInitialRecommendationCount
import com.melox.player.ui.screen.home.selectHomeRecommendations
import com.melox.player.ui.screen.library.albumGridColumnCount
import com.melox.player.ui.screen.library.MusicLibraryPlaceholder
import com.melox.player.ui.screen.library.albumDetailHeaderCoverSize
import com.melox.player.ui.screen.library.resolveMusicPlaybackSelection
import com.melox.player.ui.screen.library.toMusicLibraryPlaceholder
import com.melox.player.ui.screen.playback.LYRIC_INACTIVE_TEXT_ALPHA
import com.melox.player.ui.screen.playback.LYRIC_CENTERING_BASE_STIFFNESS
import com.melox.player.ui.screen.playback.LYRIC_CENTERING_MAX_STIFFNESS
import com.melox.player.ui.screen.playback.LYRIC_PRIMARY_FONT_SIZE_SP
import com.melox.player.ui.screen.playback.LYRIC_PRIMARY_LINE_HEIGHT_SP
import com.melox.player.ui.screen.playback.LYRIC_TRANSLATION_FONT_SIZE_SP
import com.melox.player.ui.screen.playback.LYRIC_TRANSLATION_LINE_HEIGHT_SP
import com.melox.player.ui.screen.playback.LYRICS_MANUAL_FOLLOW_RESUME_DELAY_MS
import com.melox.player.ui.screen.playback.characterMotion
import com.melox.player.ui.screen.playback.characterProgress
import com.melox.player.ui.screen.playback.shouldUseWordAnimation
import com.melox.player.ui.screen.playback.wordMotion
import com.melox.player.ui.screen.playback.lyricBlurRadiusTarget
import com.melox.player.ui.screen.playback.lyricBlurShouldDisableForBrowsing
import com.melox.player.ui.screen.playback.lyricCenterScrollDelta
import com.melox.player.ui.screen.playback.lyricDisplayedPositionMs
import com.melox.player.ui.screen.playback.lyricLineRenderPositionMs
import com.melox.player.ui.screen.playback.lyricLineLayerAlpha
import com.melox.player.ui.screen.playback.lyricOutgoingSeekCanClear
import com.melox.player.ui.screen.playback.lyricEdgeFadeHeights
import com.melox.player.ui.screen.playback.lyricIntervalProgress
import com.melox.player.ui.screen.playback.lyricLineVerticalPaddingDp
import com.melox.player.ui.screen.playback.lyricOffscreenTranslationDistance
import com.melox.player.ui.screen.playback.lyricPlaybackPositionMs
import com.melox.player.ui.screen.playback.lyricProgrammaticTranslationStart
import com.melox.player.ui.screen.playback.lyricCenteringSpringStiffness
import com.melox.player.ui.screen.playback.lyricScrollIsManual
import com.melox.player.ui.screen.playback.lyricSeekUsesAnimatedCentering
import com.melox.player.ui.screen.playback.lyricSeekPositionIsApplied
import com.melox.player.ui.screen.playback.lyricSeekRequestIsAcknowledged
import com.melox.player.ui.screen.playback.stabilizedLyricPlaybackPositionMs
import com.melox.player.ui.screen.playback.lyricTargetScrollOffset
import com.melox.player.ui.screen.playback.lyricTranslationAlpha
import com.melox.player.ui.screen.playback.lyricWordProgressActiveAlpha
import com.melox.player.ui.screen.playback.lyricVerticalDragExceedsTouchSlop
import com.melox.player.ui.screen.playback.progressGestureIsDrag
import com.melox.player.ui.screen.playback.playerHeaderArtistText
import com.melox.player.ui.screen.playback.fitPlayerArtworkSize
import com.melox.player.ui.screen.playback.hiddenLyricsCenterOffsetY
import com.melox.player.ui.screen.playback.queueListHeight
import com.melox.player.ui.screen.playback.queueLocationAnchorHeight
import com.melox.player.ui.screen.playback.moveQueueListItem
import com.melox.player.ui.screen.playback.queueMoveTargetAfterCurrent
import com.melox.player.ui.screen.playback.queueOffscreenExitAlpha
import com.melox.player.ui.screen.playback.queueOffscreenExitTranslation
import com.melox.player.ui.screen.playback.queuePlayNextPeerTranslation
import com.melox.player.ui.screen.playback.queueShowsPlayNextAction
import com.melox.player.ui.screen.playback.sliderValueAtPosition
import com.melox.player.ui.theme.toColorSchemeMode
import com.melox.player.ui.theme.resolveDynamicColorSeed
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.CRC32
import java.util.zip.CheckedOutputStream
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import kotlin.random.Random

class UiLogicTest {
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
    fun dynamicFlowUsesConfiguredVisualValuesAndAdaptiveDownsampling() {
        assertEquals(2.5f, DYNAMIC_FLOW_ARTWORK_SATURATION, 0f)
        assertEquals(0.25f, DYNAMIC_FLOW_BACKGROUND_DARKEN_AMOUNT, 0f)
        assertEquals(16f, dynamicFlowDownsampleFactor(419), 0f)
        assertEquals(20f, dynamicFlowDownsampleFactor(420), 0f)
        assertEquals(10_000L, scaledDynamicFlowTimeMs(10_000L, 10))
    }

    @Test
    fun dynamicFlowClockStartsAndResumesWithoutJumpingToAbsoluteFrameTime() {
        val absoluteFrameNanos = 987_654_321_000_000L
        val initial = advanceDynamicFlowClockMillis(
            elapsedMillis = 0L,
            previousFrameNanos = null,
            frameNanos = absoluteFrameNanos,
        )
        val advanced = advanceDynamicFlowClockMillis(
            elapsedMillis = initial,
            previousFrameNanos = absoluteFrameNanos,
            frameNanos = absoluteFrameNanos + 42_000_000L,
        )
        val resumed = advanceDynamicFlowClockMillis(
            elapsedMillis = advanced,
            previousFrameNanos = null,
            frameNanos = absoluteFrameNanos + 5_000_000_000L,
        )

        assertEquals(0L, initial)
        assertEquals(42L, advanced)
        assertEquals(42L, resumed)
    }

    @Test
    fun rootPagerOnlyYieldsAnActiveRecommendationGestureOnInteriorPages() {
        assertTrue(
            rootPagerUserScrollEnabled(
                selectedPage = 0,
                homeRecommendationPage = 0,
                homeRecommendationPageCount = 4,
                homeRecommendationGestureActive = true,
            ),
        )
        assertFalse(
            rootPagerUserScrollEnabled(
                selectedPage = 0,
                homeRecommendationPage = 1,
                homeRecommendationPageCount = 4,
                homeRecommendationGestureActive = true,
            ),
        )
        assertTrue(
            rootPagerUserScrollEnabled(
                selectedPage = 0,
                homeRecommendationPage = 3,
                homeRecommendationPageCount = 4,
                homeRecommendationGestureActive = true,
            ),
        )
        assertTrue(
            rootPagerUserScrollEnabled(
                selectedPage = 0,
                homeRecommendationPage = 1,
                homeRecommendationPageCount = 4,
                homeRecommendationGestureActive = false,
            ),
        )
        assertTrue(
            rootPagerUserScrollEnabled(
                selectedPage = 1,
                homeRecommendationPage = 1,
                homeRecommendationPageCount = 4,
                homeRecommendationGestureActive = true,
            ),
        )
    }

    @Test
    fun startupAppearanceDefaultsCanRenderNavigationImmediately() {
        val settings = AppSettings()

        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertFalse(settings.dynamicColorEnabled)
        assertEquals(DynamicColorSource.PLAYBACK_ARTWORK, settings.dynamicColorSource)
        assertEquals(
            PlaybackBackgroundStyle.BLURRED_ARTWORK,
            settings.playbackBackgroundStyle,
        )
        assertEquals(400, settings.lyricFontWeight)
        assertFalse(settings.centerLyrics)
        assertFalse(settings.leftAlignPlayerTitle)
        assertTrue(settings.showLyricsTranslation)
        assertEquals(LyricsSourcePriority.EMBEDDED, settings.lyricsSourcePriority)
        assertEquals(
            LyricsSidecarFormatPriority.LRC,
            settings.lyricsSidecarFormatPriority,
        )
        assertEquals(true, settings.blurEnabled)
        assertFalse(settings.progressiveTopBarBlurEnabled)
        assertFalse(settings.hideBottomBar)
        assertEquals(true, settings.predictiveBackEnabled)
        assertEquals(NavigationTransitionStyle.MIUIX, settings.navigationTransitionStyle)
        assertFalse(settings.refreshLibraryOnStart)
        assertFalse(settings.skipShortAudio)
        assertEquals(BottomBarStyle.NORMAL, settings.bottomBarStyle)
    }

    @Test
    fun playbackBlurUsesThreeOddRenderScriptCalibratedBoxes() {
        val boxSizes = createRenderScriptBlurBoxSizes(radius = 25)

        assertEquals(3, boxSizes.size)
        assertTrue(boxSizes.all { size -> size > 0 && size % 2 == 1 })
        assertArrayEquals(intArrayOf(21, 21, 21), boxSizes)
    }

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
            sidecarFormatPriority = LyricsSidecarFormatPriority.TTML,
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
                    sidecarFormatPriority = LyricsSidecarFormatPriority.LRC,
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
    fun fullPlayerArtworkResolutionFollowsDisplayNeedAndMemoryBudget() {
        val largeHeap = 3L * 1024L * 1024L * 1024L

        assertEquals(1800, fullPlayerArtworkTargetSizePx(1800, largeHeap))
        assertEquals(3999, fullPlayerArtworkTargetSizePx(3999, largeHeap))
        assertEquals(4000, fullPlayerArtworkTargetSizePx(4000, largeHeap))
        assertEquals(4001, fullPlayerArtworkTargetSizePx(4001, largeHeap))
        assertEquals(8000, fullPlayerArtworkTargetSizePx(9000, largeHeap))
        assertEquals(
            4096,
            fullPlayerArtworkTargetSizePx(
                displayedSizePx = 8000,
                maxMemoryBytes = 512L * 1024L * 1024L,
            ),
        )
    }

    @Test
    fun fullPlayerArtworkKeepsTrackTransitionAndShortensResolutionUpgrade() {
        assertEquals(
            500,
            artworkCrossfadeDurationMillis("track-a", "track-b", 500, 100),
        )
        assertEquals(
            100,
            artworkCrossfadeDurationMillis("track-a", "track-a", 500, 100),
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

    @Test
    fun kenBurnsFrameInterpolationPreservesEndpoints() {
        val start = KenBurnsFrame(
            scale = 1.08f,
            horizontalBias = -1f,
            verticalBias = 0.5f,
        )
        val end = KenBurnsFrame(
            scale = 1.2f,
            horizontalBias = 1f,
            verticalBias = -0.5f,
        )

        assertEquals(start, interpolateKenBurnsFrame(start, end, 0f))
        assertEquals(end, interpolateKenBurnsFrame(start, end, 1f))
        val midpoint = interpolateKenBurnsFrame(start, end, 0.5f)
        assertEquals(1.14f, midpoint.scale, 0.000001f)
        assertEquals(0f, midpoint.horizontalBias, 0.000001f)
        assertEquals(0f, midpoint.verticalBias, 0.000001f)
    }

    @Test
    fun predictiveBackHandlerRequiresBothSettingAndBackEntry() {
        assertEquals(true, predictiveBackHandlerEnabled(true, true))
        assertFalse(predictiveBackHandlerEnabled(true, false))
        assertFalse(predictiveBackHandlerEnabled(false, true))
        assertEquals(true, ordinaryBackHandlerEnabled(false, true))
        assertFalse(ordinaryBackHandlerEnabled(true, true))
        assertFalse(ordinaryBackHandlerEnabled(false, false))
    }

    @Test
    fun mountedPlayerDisablesBothNavigationBackPaths() {
        for (predictiveEnabled in listOf(false, true)) {
            for (hasPreviousEntries in listOf(false, true)) {
                assertFalse(
                    predictiveBackHandlerEnabled(predictiveEnabled, hasPreviousEntries, false),
                )
                assertFalse(
                    ordinaryBackHandlerEnabled(predictiveEnabled, hasPreviousEntries, false),
                )
                assertEquals(
                    hasPreviousEntries,
                    predictiveBackHandlerEnabled(predictiveEnabled, hasPreviousEntries, true) ||
                        ordinaryBackHandlerEnabled(predictiveEnabled, hasPreviousEntries, true),
                )
            }
        }
    }

    @Test
    fun themeSettingsMapToExpectedMiuixModes() {
        val expected = mapOf(
            AppSettings(ThemeMode.SYSTEM, false) to ColorSchemeMode.System,
            AppSettings(ThemeMode.LIGHT, false) to ColorSchemeMode.Light,
            AppSettings(ThemeMode.DARK, false) to ColorSchemeMode.Dark,
            AppSettings(ThemeMode.SYSTEM, true) to ColorSchemeMode.MonetSystem,
            AppSettings(ThemeMode.LIGHT, true) to ColorSchemeMode.MonetLight,
            AppSettings(ThemeMode.DARK, true) to ColorSchemeMode.MonetDark,
        )

        expected.forEach { (settings, mode) ->
            assertEquals(mode, settings.toColorSchemeMode())
        }
    }

    @Test
    fun unsupportedDynamicColorFallsBackToStaticTheme() {
        assertEquals(
            ColorSchemeMode.System,
            AppSettings(dynamicColorEnabled = true)
                .toColorSchemeMode(dynamicColorSupported = false),
        )
        assertEquals(
            ColorSchemeMode.Dark,
            AppSettings(
                themeMode = ThemeMode.DARK,
                dynamicColorEnabled = true,
            ).toColorSchemeMode(dynamicColorSupported = false),
        )
    }

    @Test
    fun dynamicColorSourceSelectsPlatformOrArtworkSeed() {
        val artworkColor = Color(0xFFB3261E)

        assertEquals(
            null,
            resolveDynamicColorSeed(
                dynamicColorEnabled = true,
                source = DynamicColorSource.DESKTOP,
                playbackArtworkColor = artworkColor,
            ),
        )
        assertEquals(
            artworkColor,
            resolveDynamicColorSeed(
                dynamicColorEnabled = true,
                source = DynamicColorSource.PLAYBACK_ARTWORK,
                playbackArtworkColor = artworkColor,
            ),
        )
        assertEquals(
            null,
            resolveDynamicColorSeed(
                dynamicColorEnabled = true,
                source = DynamicColorSource.PLAYBACK_ARTWORK,
                playbackArtworkColor = null,
            ),
        )
        assertEquals(
            null,
            resolveDynamicColorSeed(
                dynamicColorEnabled = false,
                source = DynamicColorSource.PLAYBACK_ARTWORK,
                playbackArtworkColor = artworkColor,
            ),
        )
    }

    @Test
    fun audioPermissionChangesAtAndroid13() {
        assertEquals(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            requiredAudioPermission(sdkInt = 32),
        )
        assertEquals(
            Manifest.permission.READ_MEDIA_AUDIO,
            requiredAudioPermission(sdkInt = 33),
        )
    }

    @Test
    fun unsupportedLiquidGlassFallsBackToFloating() {
        assertEquals(
            BottomBarStyle.FLOATING,
            resolveBottomBarStyle(BottomBarStyle.LIQUID_GLASS, liquidGlassSupported = false),
        )
        assertEquals(
            BottomBarStyle.LIQUID_GLASS,
            resolveBottomBarStyle(BottomBarStyle.LIQUID_GLASS, liquidGlassSupported = true),
        )
        assertEquals(
            BottomBarStyle.NORMAL,
            resolveBottomBarStyle(BottomBarStyle.NORMAL, liquidGlassSupported = false),
        )
    }

    @Test
    fun floatingStyleOverridesTheWideScreenNavigationRail() {
        assertFalse(
            shouldUseNavigationRail(
                windowWidth = 800.dp,
                windowHeight = 1200.dp,
                effectiveStyle = BottomBarStyle.FLOATING,
            ),
        )
        assertFalse(
            usesNormalMiniPlayerChrome(
                renderedBottomBarStyle = BottomBarStyle.FLOATING,
                liquidGlassSupported = true,
            ),
        )
        assertFalse(
            shouldUseNavigationRail(
                windowWidth = 840.dp,
                windowHeight = 1200.dp,
                effectiveStyle = BottomBarStyle.FLOATING,
            ),
        )
        assertFalse(
            usesNormalMiniPlayerChrome(
                renderedBottomBarStyle = BottomBarStyle.FLOATING,
                liquidGlassSupported = true,
            ),
        )
        assertFalse(
            usesNormalMiniPlayerChrome(
                renderedBottomBarStyle = BottomBarStyle.FLOATING,
                liquidGlassSupported = true,
            ),
        )
        assertTrue(
            shouldShowNavigation(
                currentRouteIsRoot = true,
                hideBottomBar = false,
                landscape = false,
                requestedBottomBarStyle = BottomBarStyle.FLOATING,
            ),
        )
        assertTrue(
            shouldShowNavigation(
                currentRouteIsRoot = true,
                hideBottomBar = false,
                landscape = true,
                requestedBottomBarStyle = BottomBarStyle.FLOATING,
            ),
        )
        assertFalse(
            shouldShowNavigation(
                currentRouteIsRoot = true,
                hideBottomBar = false,
                landscape = true,
                requestedBottomBarStyle = BottomBarStyle.FLOATING,
                renderedBottomBarStyle = BottomBarStyle.NORMAL,
            ),
        )
    }

    @Test
    fun adaptiveLayoutMatchesTheOfficialMiuixBreakpoints() {
        assertFalse(isMiuixWideLayout(windowWidth = 599.dp, windowHeight = 400.dp))
        assertTrue(isMiuixWideLayout(windowWidth = 600.dp, windowHeight = 500.dp))
        assertFalse(isMiuixWideLayout(windowWidth = 600.dp, windowHeight = 720.dp))
        assertTrue(isMiuixWideLayout(windowWidth = 840.dp, windowHeight = 1200.dp))
    }

    @Test
    fun miuixLandscapeSecondaryTransitionHasNoCornerClip() {
        assertEquals(
            0.dp,
            navigationTransitionCornerRadius(
                windowWidth = 1200.dp,
                windowHeight = 800.dp,
                transitionStyle = NavigationTransitionStyle.MIUIX,
                systemCornerRadius = 24.dp,
            ),
        )
        assertEquals(
            24.dp,
            navigationTransitionCornerRadius(
                windowWidth = 800.dp,
                windowHeight = 1200.dp,
                transitionStyle = NavigationTransitionStyle.MIUIX,
                systemCornerRadius = 24.dp,
            ),
        )
    }

    @Test
    fun aospCornerFallbackRemainsUnchanged() {
        assertEquals(
            32.dp,
            navigationTransitionCornerRadius(
                windowWidth = 1200.dp,
                windowHeight = 800.dp,
                transitionStyle = NavigationTransitionStyle.AOSP,
                systemCornerRadius = 0.dp,
            ),
        )
    }

    @Test
    fun floatingStyleUsesTheFloatingPathOnAllScreenOrientations() {
        assertFalse(
            shouldUseNavigationRail(
                windowWidth = 1200.dp,
                windowHeight = 800.dp,
                effectiveStyle = BottomBarStyle.FLOATING,
            ),
        )
        assertTrue(
            shouldUseNavigationRail(
                windowWidth = 1200.dp,
                windowHeight = 800.dp,
                effectiveStyle = BottomBarStyle.NORMAL,
            ),
        )
        assertFalse(
            usesNormalMiniPlayerChrome(
                renderedBottomBarStyle = BottomBarStyle.FLOATING,
                liquidGlassSupported = true,
            ),
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
    fun landscapeFloatingBarUsesTheFullHorizontalContentWidth() {
        assertEquals(
            1152.dp,
            floatingBottomBarAvailableWidth(
                windowWidth = 1200.dp,
                windowHeight = 800.dp,
                contentMaxWidth = 1152.dp,
                portraitReferenceWidth = 800.dp,
            ),
        )
        assertEquals(
            752.dp,
            floatingBottomBarAvailableWidth(
                windowWidth = 800.dp,
                windowHeight = 1200.dp,
                contentMaxWidth = 752.dp,
                portraitReferenceWidth = 800.dp,
            ),
        )
        assertEquals(
            352.dp,
            floatingBottomBarAvailableWidth(
                windowWidth = 599.dp,
                windowHeight = 400.dp,
                contentMaxWidth = 551.dp,
                portraitReferenceWidth = 400.dp,
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
    fun fullPlayerStatusBarIconsFollowTheSharedLayerHandoff() {
        assertFalse(playerSheetUsesFullPlayerStatusBar(0f))
        assertFalse(playerSheetUsesFullPlayerStatusBar(PLAYER_LAYER_HANDOFF_END_PROGRESS))
        assertTrue(playerSheetUsesFullPlayerStatusBar(PLAYER_LAYER_HANDOFF_END_PROGRESS + 0.001f))
        assertTrue(playerSheetUsesFullPlayerStatusBar(1f))
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
    fun sharedPlayerDragReleaseFollowsVerticalDirection() {
        assertTrue(playerSheetDragTarget(-1f, 1f, originOpen = false))
        assertFalse(playerSheetDragTarget(1f, -1f, originOpen = true))
        assertTrue(playerSheetDragTarget(0f, -1f, originOpen = false))
        assertFalse(playerSheetDragTarget(0f, 1f, originOpen = true))
        assertTrue(playerSheetDragTarget(0f, 0f, originOpen = true))
        assertFalse(playerSheetDragTarget(0f, 0f, originOpen = false))
    }

    @Test
    fun durationFormattingSupportsHoursAndInvalidValues() {
        assertEquals("0:00", formatDuration(-1L))
        assertEquals("3:05", formatDuration(185_000L))
        assertEquals("1:02:03", formatDuration(3_723_000L))
    }

    @Test
    fun titleSortKeysCoverDigitsAsciiPinyinAndFallback() {
        assertEquals("0", createMusicSortKeys("1989").section)
        assertEquals("A", createMusicSortKeys("afterglow").section)
        assertEquals("Z", createMusicSortKeys("周杰伦").section)
        assertEquals("#", createMusicSortKeys("♪ intro").section)
        assertEquals("#", createMusicSortKeys(null).section)
    }

    @Test
    fun japaneseKanaUsesRomajiForTitleSortingAndSections() {
        assertEquals("sakura", japaneseKanaToRomaji("さくら"))
        assertEquals("katakana", japaneseKanaToRomaji("カタカナ"))
        assertEquals("ccha", japaneseKanaToRomaji("っちゃ"))
        assertEquals("S", createMusicSortKeys("さくら").section)
        assertEquals("1_SAKURA", createMusicSortKeys("さくら").value)
        assertEquals("T", createMusicSortKeys("とうきょう").section)
    }

    @Test
    fun missingAlphabetSectionsResolveInDisplayOrder() {
        val sectionIndexMap = mapOf(
            "0" to 0,
            "B" to 3,
            "M" to 8,
            "#" to 12,
        )

        assertEquals(3, findAlphabetTargetIndex("A", sectionIndexMap))
        assertEquals(8, findAlphabetTargetIndex("C", sectionIndexMap))
        assertEquals(12, findAlphabetTargetIndex("Z", sectionIndexMap))
        assertEquals(12, findAlphabetTargetIndex("#", sectionIndexMap))
        assertEquals(0, findAlphabetTargetIndex("?", sectionIndexMap))
    }

    @Test
    fun missingAlphabetSectionsResolveInDescendingDisplayOrder() {
        val sectionIndexMap = mapOf(
            "#" to 0,
            "M" to 3,
            "B" to 8,
            "0" to 12,
        )
        val descendingSections = AlphabetSections.asReversed()

        assertEquals(3, findAlphabetTargetIndex("Z", sectionIndexMap, descendingSections))
        assertEquals(8, findAlphabetTargetIndex("C", sectionIndexMap, descendingSections))
        assertEquals(12, findAlphabetTargetIndex("A", sectionIndexMap, descendingSections))
    }

    @Test
    fun musicSortingCoversEveryFieldAndDescendingOrder() {
        val tracks = listOf(
            musicTrack(
                id = 1L,
                title = "Bravo",
                artist = "Zed",
                dateAddedEpochSeconds = 30L,
                fileName = "c.mp3",
                fileSizeBytes = 300L,
                durationMs = 200L,
            ),
            musicTrack(
                id = 2L,
                title = "Alpha",
                artist = "Alpha",
                dateAddedEpochSeconds = 10L,
                fileName = "b.mp3",
                fileSizeBytes = 100L,
                durationMs = 300L,
            ),
            musicTrack(
                id = 3L,
                title = "Charlie",
                artist = "Mia",
                dateAddedEpochSeconds = 20L,
                fileName = "a.mp3",
                fileSizeBytes = 200L,
                durationMs = 100L,
            ),
        )

        assertEquals(listOf(2L, 1L, 3L), tracks.sortedIds(MusicSortField.TITLE))
        assertEquals(listOf(2L, 3L, 1L), tracks.sortedIds(MusicSortField.ARTIST))
        assertEquals(listOf(2L, 3L, 1L), tracks.sortedIds(MusicSortField.DATE_ADDED))
        assertEquals(listOf(3L, 2L, 1L), tracks.sortedIds(MusicSortField.FILE_NAME))
        assertEquals(listOf(2L, 3L, 1L), tracks.sortedIds(MusicSortField.FILE_SIZE))
        assertEquals(listOf(3L, 1L, 2L), tracks.sortedIds(MusicSortField.DURATION))
        assertEquals(
            listOf(3L, 1L, 2L),
            tracks.sortedIds(MusicSortField.TITLE, descending = true),
        )
        assertEquals("A", createMusicSortKeys(tracks[2].fileName).section)
    }

    @Test
    fun singleSongSearchFallsBackToTheSortedSongsPageQueue() {
        val queue = listOf(
            musicTrack(1L, "Alpha", 1L, "alpha.mp3", 1L, 1L),
            musicTrack(2L, "Bravo", 2L, "bravo.mp3", 2L, 2L),
            musicTrack(3L, "Charlie", 3L, "charlie.mp3", 3L, 3L),
        )
        val displayed = listOf(queue[1])

        val selection = resolveMusicPlaybackSelection(
            displayedTracks = displayed,
            queueTracks = queue,
            query = "brav",
            selectedIndex = 0,
        )

        assertEquals(queue, selection?.first)
        assertEquals(1, selection?.second)
    }

    @Test
    fun multipleSongSearchUsesTheCompleteSongsPageQueue() {
        val displayed = listOf(
            musicTrack(1L, "Alpha", 1L, "alpha.mp3", 1L, 1L),
            musicTrack(2L, "Alpine", 2L, "alpine.mp3", 2L, 2L),
        )
        val fullQueue = displayed + musicTrack(3L, "Bravo", 3L, "bravo.mp3", 3L, 3L)

        val selection = resolveMusicPlaybackSelection(
            displayedTracks = displayed,
            queueTracks = fullQueue,
            query = "al",
            selectedIndex = 1,
        )

        assertEquals(fullQueue, selection?.first)
        assertEquals(1, selection?.second)
    }

    @Test
    fun refreshedLibraryMetadataUpdatesPlaybackUiWithoutChangingQueueIdentity() {
        val queueItem = playbackQueueItem("track-7").copy(
            trackId = 7L,
            title = "Old title",
            artist = "Old artist",
            album = "Old album",
            durationMs = 1_000L,
            dateModifiedEpochSeconds = 10L,
            fileSizeBytes = 20L,
        )
        val refreshedTrack = musicTrack(
            id = 7L,
            title = "New title",
            dateAddedEpochSeconds = 1L,
            fileName = "new.flac",
            fileSizeBytes = 40L,
            durationMs = 2_000L,
            dateModifiedEpochSeconds = 30L,
        ).copy(
            artist = "New artist",
            album = "New album",
        )

        val updated = PlaybackUiState(
            queue = listOf(queueItem),
            currentIndex = 0,
            positionMs = 500L,
            isPlaying = true,
        ).withTrackMetadata(listOf(refreshedTrack))

        assertEquals(queueItem.mediaId, updated.currentItem?.mediaId)
        assertEquals(queueItem.contentUri, updated.currentItem?.contentUri)
        assertEquals("New title", updated.currentItem?.title)
        assertEquals("New artist", updated.currentItem?.artist)
        assertEquals("New album", updated.currentItem?.album)
        assertEquals(2_000L, updated.currentItem?.durationMs)
        assertEquals(30L, updated.currentItem?.dateModifiedEpochSeconds)
        assertEquals(40L, updated.currentItem?.fileSizeBytes)
        assertEquals(500L, updated.positionMs)
        assertTrue(updated.isPlaying)
    }

    @Test
    fun albumAndArtistGroupsExposeRequestedSortCounts() {
        val tracks = listOf(
            musicTrack(1L, "One", 1L, "one.mp3", 1L, 1L).copy(
                artist = "Artist B",
                album = "Album B",
                albumArtist = "Artist B",
                year = 2024,
            ),
            musicTrack(2L, "Two", 2L, "two.mp3", 2L, 2L).copy(
                artist = "Artist A",
                album = "Album A",
                albumArtist = "Artist A",
                year = 2020,
            ),
            musicTrack(3L, "Three", 3L, "three.mp3", 3L, 3L).copy(
                artist = "Artist A",
                album = "Album A",
                albumArtist = "Artist A",
                year = 2020,
            ),
        )

        val albums = buildAlbumGroups(tracks)
        assertEquals(
            listOf("Album A", "Album B"),
            sortAlbums(
                albums,
                AlbumSortConfig(field = AlbumSortField.SONG_COUNT, descending = true),
            ).map { it.name },
        )
        assertEquals(
            listOf("Album A", "Album B"),
            sortAlbums(
                albums,
                AlbumSortConfig(field = AlbumSortField.YEAR),
            ).map { it.name },
        )

        val artists = buildArtistGroups(tracks)
        assertEquals(
            listOf("Artist A", "Artist B"),
            sortArtists(
                artists,
                ArtistSortConfig(field = ArtistSortField.SONG_COUNT, descending = true),
            ).map { it.name },
        )
        assertEquals(2, artists.first { it.name == "Artist A" }.tracks.size)
        assertEquals(1, artists.first { it.name == "Artist A" }.albumCount)
    }

    @Test
    fun albumDiscSectionsSortMissingMetadataBeforeNumberedTracks() {
        val tracks = listOf(
            musicTrack(7L, "Disc One Track Two", 7L, "7.mp3", 7L, 7_000L)
                .copy(discNumber = 1, trackNumber = 2),
            musicTrack(1L, "Unnumbered B", 1L, "1.mp3", 1L, 1_000L),
            musicTrack(9L, "Disc Two Track Three", 9L, "9.mp3", 9L, 9_000L)
                .copy(discNumber = 2, trackNumber = 3),
            musicTrack(5L, "Disc One Missing B", 5L, "5.mp3", 5L, 5_000L)
                .copy(discNumber = 1),
            musicTrack(3L, "Numbered Two", 3L, "3.mp3", 3L, 3_000L)
                .copy(trackNumber = 2),
            musicTrack(8L, "Disc One Track One", 8L, "8.mp3", 8L, 8_000L)
                .copy(discNumber = 1, trackNumber = 1),
            musicTrack(2L, "Unnumbered A", 2L, "2.mp3", 2L, 2_000L),
            musicTrack(6L, "Disc One Missing A", 6L, "6.mp3", 6L, 6_000L)
                .copy(discNumber = 1),
            musicTrack(4L, "Numbered One", 4L, "4.mp3", 4L, 4_000L)
                .copy(trackNumber = 1),
        )

        val sections = buildAlbumDiscSections(tracks)

        assertEquals(listOf(null, 1, 2), sections.map { it.discNumber })
        assertEquals(listOf(2L, 1L, 4L, 3L), sections[0].tracks.map(MusicTrack::id))
        assertEquals(listOf(6L, 5L, 8L, 7L), sections[1].tracks.map(MusicTrack::id))
        assertEquals(listOf(9L), sections[2].tracks.map(MusicTrack::id))
        assertEquals(listOf(10_000L, 26_000L, 9_000L), sections.map { it.totalDurationMs })
    }

    @Test
    fun artistGroupsSplitDelimitedArtistNames() {
        val tracks = listOf(
            musicTrack(1L, "One", 1L, "one.mp3", 1L, 1L).copy(
                artist = "Artist A，Artist B, Artist C、Artist D/Artist E & Artist G",
                album = "Album One",
                albumArtist = "Artist A",
            ),
            musicTrack(2L, "Two", 2L, "two.mp3", 2L, 2L).copy(
                artist = "Artist B / Artist F",
                album = "Album Two",
                albumArtist = "Artist B",
            ),
        )

        val artistsByName = buildArtistGroups(tracks).associateBy { it.name }

        assertEquals(
            setOf(
                "Artist A",
                "Artist B",
                "Artist C",
                "Artist D",
                "Artist E",
                "Artist F",
                "Artist G",
            ),
            artistsByName.keys,
        )
        assertEquals(listOf(1L, 2L), artistsByName.getValue("Artist B").tracks.map { it.id })
        assertEquals(2, artistsByName.getValue("Artist B").albumCount)
        assertEquals(
            "Artist A / Artist B / Artist C / Artist D / Artist E / Artist G",
            displayArtistName(
                "Artist A，Artist B, Artist C、Artist D/Artist E & Artist G",
            ),
        )
        assertEquals(
            listOf("Artist A", "Artist B"),
            splitArtistNames("Artist A & Artist B & artist a"),
        )
    }

    @Test
    fun participatingArtistsResolveRealGroupsInTrackOrder() {
        val tracks = listOf(
            musicTrack(1L, "Duet", 1L, "duet.flac", 1L, 1L).copy(
                artist = "Artist B & Artist A",
                album = "Album",
            ),
            musicTrack(2L, "Solo", 2L, "solo.flac", 2L, 2L).copy(
                artist = "Artist A & Artist C",
                album = "Album",
            ),
            musicTrack(3L, "Guest", 3L, "guest.flac", 3L, 3L).copy(
                artist = "Artist C / Artist D",
                album = "Album",
            ),
        )
        val groups = buildArtistGroups(tracks)

        assertEquals(
            listOf("Artist B", "Artist A"),
            participatingArtistGroups(tracks.first(), groups).map { it.name },
        )
        assertEquals(
            listOf("Artist B", "Artist A", "Artist C", "Artist D"),
            participatingArtistGroups(tracks, groups).map { it.name },
        )
    }

    @Test
    fun songInfoFormatsAudioTypeAndPrimaryStorageLocation() {
        val track = musicTrack(
            id = 3L,
            title = "Song",
            dateAddedEpochSeconds = 1L,
            fileName = "song.flac",
            fileSizeBytes = 1L,
            durationMs = 1L,
        ).copy(
            folderPath = "/storage/self/primary/Music/Album",
            mimeType = "audio/flac",
        )

        assertEquals("FLAC", track.audioFormatLabel())
        assertEquals(
            "/storage/emulated/0/Music/Album/song.flac",
            track.displayFileLocation(),
        )
        assertEquals(
            "MPEG",
            track.copy(fileName = "song", mimeType = "audio/mpeg").audioFormatLabel(),
        )
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

    @Test
    fun searchFocusClearsWhenTheVisibleKeyboardIsDismissed() {
        assertTrue(
            shouldClearSearchFocusAfterImeDismissed(
                searchFocused = true,
                imeVisible = false,
                imeWasVisible = true,
            ),
        )
        assertFalse(
            shouldClearSearchFocusAfterImeDismissed(
                searchFocused = true,
                imeVisible = true,
                imeWasVisible = false,
            ),
        )
        assertFalse(
            shouldClearSearchFocusAfterImeDismissed(
                searchFocused = false,
                imeVisible = false,
                imeWasVisible = true,
            ),
        )
    }

    @Test
    fun albumGridStylesKeepLegacySelectionsAndExpectedColumnCounts() {
        assertEquals(AlbumGridStyle.TWO_SMALL, AlbumSortConfig().gridStyle)
        assertEquals(
            listOf(2, 3),
            AlbumGridStyle.entries.map(AlbumGridStyle::columns),
        )
        assertEquals(
            AlbumGridStyle.TWO_SMALL.ordinal,
            resolveAlbumGridStyleOrdinal(storedStyleOrdinal = null, legacyColumns = 2),
        )
        assertEquals(
            AlbumGridStyle.THREE.ordinal,
            resolveAlbumGridStyleOrdinal(storedStyleOrdinal = null, legacyColumns = 3),
        )
        assertEquals(
            AlbumGridStyle.TWO_SMALL.ordinal,
            resolveAlbumGridStyleOrdinal(
                storedStyleOrdinal = AlbumGridStyle.TWO_SMALL.ordinal,
                legacyColumns = 3,
            ),
        )
        assertEquals(
            AlbumGridStyle.TWO_SMALL.ordinal,
            resolveAlbumGridStyleOrdinal(
                storedStyleOrdinal = 1,
                legacyColumns = 2,
            ),
        )
        assertEquals(
            AlbumGridStyle.THREE.ordinal,
            resolveAlbumGridStyleOrdinal(
                storedStyleOrdinal = 2,
                legacyColumns = 3,
            ),
        )
        assertEquals(2, albumGridColumnCount(AlbumGridStyle.TWO_SMALL, false, false, 400.dp))
        assertEquals(3, albumGridColumnCount(AlbumGridStyle.TWO_SMALL, true, false, 520.dp))
        assertEquals(4, albumGridColumnCount(AlbumGridStyle.TWO_SMALL, true, true, 680.dp))
        assertEquals(5, albumGridColumnCount(AlbumGridStyle.TWO_SMALL, true, false, 840.dp))
        assertEquals(3, albumGridColumnCount(AlbumGridStyle.THREE, false, true, 400.dp))
        assertEquals(3, albumGridColumnCount(AlbumGridStyle.THREE, true, false, 400.dp))
        assertEquals(4, albumGridColumnCount(AlbumGridStyle.THREE, true, false, 560.dp))
        assertEquals(5, albumGridColumnCount(AlbumGridStyle.THREE, true, true, 720.dp))
        assertEquals(6, albumGridColumnCount(AlbumGridStyle.THREE, true, false, 840.dp))
        assertEquals(2, homePlaylistGridColumnCount(landscape = false, availableWidth = 400.dp))
        assertEquals(2, homePlaylistGridColumnCount(landscape = true, availableWidth = 400.dp))
        assertEquals(3, homePlaylistGridColumnCount(landscape = true, availableWidth = 600.dp))
        assertEquals(5, homePlaylistGridColumnCount(landscape = true, availableWidth = 840.dp))
        assertEquals(3, responsiveGridColumnCount(600.dp, 32.dp, 140.dp, 2, 6))
        assertEquals(4, homeInitialRecommendationCount(400.dp))
        assertEquals(8, homeInitialRecommendationCount(1280.dp))
        assertEquals(9, homeInitialRecommendationCount(1413.dp))
    }

    @Test
    fun playlistGridColumnsStayBetweenTheTwoAlbumGridStyles() {
        (320..1200 step 4).forEach { width ->
            val availableWidth = width.dp
            val smallAlbumColumns = albumGridColumnCount(
                gridStyle = AlbumGridStyle.TWO_SMALL,
                landscape = true,
                navigationRailExpanded = false,
                availableWidth = availableWidth,
            )
            val playlistColumns = homePlaylistGridColumnCount(
                landscape = true,
                availableWidth = availableWidth,
            )
            val largeAlbumColumns = albumGridColumnCount(
                gridStyle = AlbumGridStyle.THREE,
                landscape = true,
                navigationRailExpanded = false,
                availableWidth = availableWidth,
            )

            assertTrue(smallAlbumColumns <= playlistColumns)
            assertTrue(playlistColumns <= largeAlbumColumns)
        }
    }

    @Test
    fun playlistArtworkUsesTheExpectedLayoutForEachEntryCount() {
        assertEquals(PlaylistArtworkLayout.EMPTY, playlistArtworkLayout(0))
        assertEquals(PlaylistArtworkLayout.SINGLE, playlistArtworkLayout(1))
        assertEquals(PlaylistArtworkLayout.DOUBLE, playlistArtworkLayout(2))
        assertEquals(PlaylistArtworkLayout.COLLAGE, playlistArtworkLayout(3))
        assertEquals(PlaylistArtworkLayout.COLLAGE, playlistArtworkLayout(12))
    }

    @Test
    fun albumDetailHeaderCoverUsesFixedSizeInEveryOrientation() {
        assertEquals(96.dp, albumDetailHeaderCoverSize())
    }

    @Test
    fun landscapeFloatingBottomBarUsesNavigationInsetOrTwelveDpFallback() {
        assertEquals(24.dp, floatingBottomBarBottomPadding(24.dp))
        assertEquals(12.dp, floatingBottomBarBottomPadding(0.dp))
    }

    @Test
    fun portraitMiniPlayerStopsAtTheFloatingNavigationBarPosition() {
        assertEquals(
            24.dp,
            floatingMiniPlayerBottomPaddingWhenNavigationIsHidden(
                navigationBarBottomInset = 0.dp,
                isPortrait = true,
            ),
        )
        assertEquals(32.dp, floatingNavigationBarBottomPadding(0.dp))
        assertEquals(32.dp, floatingNavigationBarBottomPadding(24.dp))
        assertEquals(
            0.dp,
            floatingMiniPlayerBottomPaddingWhenNavigationIsHidden(
                navigationBarBottomInset = 0.dp,
                isPortrait = false,
            ),
        )
        assertEquals(
            24.dp,
            floatingMiniPlayerBottomPaddingWhenNavigationIsHidden(
                navigationBarBottomInset = 24.dp,
                isPortrait = true,
            ),
        )
    }

    @Test
    fun homeRecommendationsStopWhenArtworkTracksAreExhausted() = runBlocking {
        val tracks = (1L..8L).map { id ->
            musicTrack(id, "Track $id", id, "$id.mp3", id, id)
        }
        val artworkTrackIds = setOf(2L, 4L, 7L)

        val recommendations = selectHomeRecommendations(
            tracks = tracks,
            seed = 42,
            count = 5,
        ) { track ->
            track.id in artworkTrackIds
        }
        val repeatedSelection = selectHomeRecommendations(
            tracks = tracks,
            seed = 42,
            count = 5,
        ) { track ->
            track.id in artworkTrackIds
        }

        assertEquals(artworkTrackIds, recommendations.map(MusicTrack::id).toSet())
        assertEquals(artworkTrackIds.size, recommendations.size)
        assertEquals(recommendations.map(MusicTrack::id), repeatedSelection.map(MusicTrack::id))
    }

    @Test
    fun homeRecommendationPriorityPassPublishesTwoAndReusesTheirArtwork() = runBlocking {
        val tracks = (1L..8L).map { id ->
            musicTrack(id, "Track $id", id, "$id.mp3", id, id)
        }
        val priorityProbeIds = mutableListOf<Long>()
        val priorityRecommendations = selectHomeRecommendations(
            tracks = tracks,
            seed = 42,
            count = 2,
            probeBatchSize = 2,
        ) { track ->
            priorityProbeIds += track.id
            true
        }
        val expansionProbeIds = mutableListOf<Long>()
        val expandedRecommendations = selectHomeRecommendations(
            tracks = tracks,
            seed = 42,
            count = 5,
            knownArtworkTrackIds = priorityRecommendations.map(MusicTrack::id).toSet(),
        ) { track ->
            expansionProbeIds += track.id
            true
        }

        assertEquals(2, priorityProbeIds.size)
        assertEquals(priorityRecommendations, expandedRecommendations.take(2))
        assertTrue(expansionProbeIds.none { it in priorityRecommendations.map(MusicTrack::id) })
    }

    @Test
    fun homeRecommendationQueueStartsWithSelectedTrackAndKeepsLoadedPrefixOrder() {
        val tracks = (1L..5L).map { id ->
            musicTrack(id, "Track $id", id, "$id.mp3", id, id)
        }

        val queue = buildHomeRecommendationPlaybackQueue(
            selectedTrackId = 2L,
            recommendations = listOf(tracks[2], tracks[0], tracks[1], tracks[2]),
            allTracks = tracks,
            playbackMode = PlaybackMode.ORDER,
        )

        assertEquals(listOf(2L, 3L, 1L, 4L, 5L), queue.map(MusicTrack::id))
    }

    @Test
    fun homeRecommendationQueueKeepsItsPrefixUntilPlaybackModeChanges() {
        val tracks = (1L..6L).map { id ->
            musicTrack(id, "Track $id", id, "$id.mp3", id, id)
        }
        val randomQueue = buildHomeRecommendationPlaybackQueue(
            selectedTrackId = 2L,
            recommendations = listOf(tracks[2], tracks[0], tracks[1]),
            allTracks = tracks,
            playbackMode = PlaybackMode.RANDOM,
            random = Random(17),
        )

        assertEquals(listOf(2L, 3L, 1L), randomQueue.take(3).map(MusicTrack::id))
        assertEquals(
            setOf(4L, 5L, 6L),
            randomQueue.drop(3).map(MusicTrack::id).toSet(),
        )

        val sourceOrderByTrackId = tracks.mapIndexed { index, track -> track.id to index.toDouble() }.toMap()
        val normalized = reorderQueueForPlaybackMode(
            queue = randomQueue.map { track ->
                playbackQueueItem(track.id.toString()).copy(
                    sourceOrder = sourceOrderByTrackId.getValue(track.id),
                )
            },
            currentIndex = 0,
            targetMode = PlaybackMode.REPEAT_ONE,
        )

        assertEquals(listOf("1", "2", "3", "4", "5", "6"), normalized.queue.map(PlaybackQueueItem::mediaId))
        assertEquals(1, normalized.currentIndex)
        assertEquals(
            setOf(PlaybackMode.REPEAT_ONE),
            normalized.queue.map(PlaybackQueueItem::playbackMode).toSet(),
        )
    }

    @Test
    fun albumGroupsUseNormalizedAlbumAndAlbumArtistInsteadOfMediaStoreId() {
        val tracks = listOf(
            musicTrack(1L, "Solo", 1L, "solo.mp3", 1L, 1L).copy(
                artist = "Artist A",
                album = "Compilation",
                albumArtist = "Various Artists",
                albumId = 100L,
                year = 2024,
            ),
            musicTrack(2L, "Duet", 2L, "duet.mp3", 2L, 2L).copy(
                artist = "Artist B",
                album = "  Compilation  ",
                albumArtist = "Various   Artists",
                albumId = 200L,
            ),
            musicTrack(3L, "Other", 3L, "other.mp3", 3L, 3L).copy(
                artist = "Artist C",
                album = "Compilation",
                albumArtist = "Artist C",
                albumId = 100L,
            ),
            musicTrack(4L, "Fourth", 4L, "fourth.mp3", 4L, 4L).copy(
                artist = "Artist D",
                album = "No Album Artist",
                albumId = 300L,
            ),
            musicTrack(5L, "Fifth", 5L, "fifth.mp3", 5L, 5L).copy(
                artist = "Artist E",
                album = "No Album Artist",
                albumId = 400L,
            ),
        )

        val albums = buildAlbumGroups(tracks)

        assertEquals(3, albums.size)
        assertEquals(
            listOf(1L, 2L),
            albums.first { it.albumArtist == "Various Artists" }.tracks.map(MusicTrack::id),
        )
        assertEquals(2024, albums.first { it.albumArtist == "Various Artists" }.year)
        assertEquals(
            listOf(4L, 5L),
            albums.first { it.name == "No Album Artist" }.tracks.map(MusicTrack::id),
        )
        assertNull(albums.first { it.name == "No Album Artist" }.albumArtist)
        assertNull(albums.first { it.name == "No Album Artist" }.year)
    }

    @Test
    fun folderPathsNormalizeAndHideThePrimaryStorageRoot() {
        assertEquals(
            "/Music/Rock",
            normalizeMusicFolderPath("Music/Rock/", includesFileName = false),
        )
        assertEquals(
            "/storage/emulated/0/Music",
            normalizeMusicFolderPath(
                "/storage/emulated/0/Music/song.flac",
                includesFileName = true,
            ),
        )
        assertEquals(
            "/Music",
            folderDisplayPath("/storage/emulated/0/Music/"),
        )
        assertEquals(
            "/Podcasts/Music",
            folderDisplayPath("/storage/self/primary/Podcasts/Music"),
        )
        assertNull(normalizeMusicFolderPath(null, includesFileName = false))
    }

    @Test
    fun folderGroupsKeepEqualNamesAtDifferentPathsAndSortDeterministically() {
        val tracks = listOf(
            musicTrack(1L, "One", 1L, "one.mp3", 1L, 1L)
                .copy(folderPath = "/storage/emulated/0/Music"),
            musicTrack(2L, "Two", 2L, "two.mp3", 2L, 2L)
                .copy(folderPath = "/storage/emulated/0/Music"),
            musicTrack(3L, "Three", 3L, "three.mp3", 3L, 3L)
                .copy(folderPath = "/storage/emulated/0/Podcasts/Music"),
            musicTrack(4L, "Four", 4L, "four.mp3", 4L, 4L)
                .copy(folderPath = "/storage/emulated/0/Download"),
        )

        val folders = buildFolderGroups(tracks)
        assertEquals(3, folders.size)
        assertEquals(2, folders.count { it.name == "Music" })
        assertEquals(
            listOf(1L, 2L),
            folders.single { it.displayPath == "/Music" }.tracks.map(MusicTrack::id),
        )
        assertTrue(filterFolders(folders, "podcasts").isEmpty())
        assertEquals(
            "/Music",
            sortFolders(
                folders,
                FolderSortConfig(
                    field = FolderSortField.SONG_COUNT,
                    descending = true,
                ),
            ).first().displayPath,
        )
        assertEquals(
            listOf("/Download", "/Music", "/Podcasts/Music"),
            sortFolders(
                folders,
                FolderSortConfig(field = FolderSortField.NAME),
            ).map { it.displayPath },
        )
    }

    @Test
    fun musicSearchMatchesVisibleMetadataAndPreservesSourceOrder() {
        val tracks = listOf(
            musicTrack(1L, "Blue Hour", 1L, "blue.mp3", 1L, 1L)
                .copy(artist = "TXT", album = "Minisode"),
            musicTrack(2L, "夜曲", 2L, "nocturne.flac", 2L, 2L)
                .copy(artist = "周杰伦", album = "十一月的萧邦"),
        )

        assertEquals(listOf(1L), filterMusicTracks(tracks, "txt").map(MusicTrack::id))
        assertEquals(listOf(2L), filterMusicTracks(tracks, "萧邦").map(MusicTrack::id))
        assertEquals(listOf(2L), filterMusicTracks(tracks, "FLAC").map(MusicTrack::id))
        assertEquals(listOf(1L, 2L), filterMusicTracks(tracks, "  ").map(MusicTrack::id))
    }

    @Test
    fun libraryGroupSearchMatchesOnlyPageItemTitles() {
        val tracks = listOf(
            musicTrack(1L, "Blue Hour", 1L, "blue.mp3", 1L, 1L)
                .copy(
                    artist = "TXT",
                    album = "Minisode",
                    albumArtist = "Big Hit",
                    folderPath = "/storage/emulated/0/Collections/Pop",
                ),
            musicTrack(2L, "Night Drive", 2L, "night.flac", 2L, 2L)
                .copy(
                    artist = "Moon",
                    album = "After Dark",
                    albumArtist = "Night Label",
                    folderPath = "/storage/emulated/0/Archive/Jazz",
                ),
        )
        val albums = buildAlbumGroups(tracks)
        val artists = buildArtistGroups(tracks)
        val folders = buildFolderGroups(tracks)

        assertEquals(
            listOf("Minisode"),
            filterAlbums(albums, "mini").map { it.name },
        )
        assertEquals(
            listOf("Moon"),
            filterArtists(artists, "moon").map { it.name },
        )
        assertEquals(
            listOf("Pop"),
            filterFolders(folders, "pop").map { it.name },
        )
        assertTrue(filterAlbums(albums, "blue").isEmpty())
        assertTrue(filterAlbums(albums, "big hit").isEmpty())
        assertTrue(filterArtists(artists, "after dark").isEmpty())
        assertTrue(filterArtists(artists, "night.flac").isEmpty())
        assertTrue(filterFolders(folders, "collections").isEmpty())
        assertTrue(filterFolders(folders, "blue hour").isEmpty())
    }

    @Test
    fun unresolvedMusicLibraryNeverMapsToEmptyContent() {
        assertEquals(
            MusicLibraryPlaceholder.Empty,
            ScanStatus.Idle.toMusicLibraryPlaceholder(),
        )
        assertEquals(
            MusicLibraryPlaceholder.Loading,
            ScanStatus.Scanning.toMusicLibraryPlaceholder(),
        )
        assertEquals(
            MusicLibraryPlaceholder.Empty,
            ScanStatus.PermissionRequired.toMusicLibraryPlaceholder(),
        )
        assertEquals(
            MusicLibraryPlaceholder.Empty,
            ScanStatus.Success(0).toMusicLibraryPlaceholder(),
        )
        assertEquals(
            MusicLibraryPlaceholder.Error,
            ScanStatus.Error("failed").toMusicLibraryPlaceholder(),
        )
    }

    @Test
    fun musicLibrarySnapshotRoundTripsTrackMetadata() {
        val tracks = listOf(
            musicTrack(
                id = 7L,
                title = "周杰伦",
                dateAddedEpochSeconds = 123L,
                fileName = "track.flac",
                fileSizeBytes = 456L,
                durationMs = 789L,
                dateModifiedEpochSeconds = 321L,
            ).copy(
                artist = "Artist",
                album = "Album",
                albumId = 42L,
                folderPath = "/storage/emulated/0/Music",
                mimeType = "audio/flac",
                bitrateBitsPerSecond = 1_800_000,
                sampleRateHz = 96_000,
                channelCount = 2,
                trackNumber = 3,
                discNumber = 2,
                bitDepth = 24,
                audioPropertiesScanned = true,
            ),
        )
        val encoded = ByteArrayOutputStream().also { output ->
            MusicLibrarySnapshotCodec.write(output, tracks)
        }.toByteArray()

        assertEquals(
            tracks,
            MusicLibrarySnapshotCodec.read(ByteArrayInputStream(encoded)),
        )
    }

    @Test
    fun audioQualityUsesFormatAndAvailableTechnicalMetadata() {
        val baseTrack = musicTrack(
            id = 9L,
            title = "Quality",
            dateAddedEpochSeconds = 1L,
            fileName = "quality.mp3",
            fileSizeBytes = 0L,
            durationMs = 180_000L,
        )

        assertEquals(
            AudioQuality.RAW,
            baseTrack.copy(
                fileName = "quality.flac",
                mimeType = "audio/flac",
                sampleRateHz = 192_000,
                bitDepth = 32,
            ).resolveAudioQuality(),
        )
        assertEquals(
            AudioQuality.HI_RES,
            baseTrack.copy(
                fileName = "quality.flac",
                mimeType = "audio/flac",
                sampleRateHz = 192_000,
                bitDepth = 24,
            ).resolveAudioQuality(),
        )
        assertEquals(
            AudioQuality.HI_RES,
            baseTrack.copy(
                fileName = "quality.flac",
                mimeType = "audio/flac",
                sampleRateHz = 44_100,
                bitDepth = 24,
            ).resolveAudioQuality(),
        )
        assertEquals(
            AudioQuality.SQ,
            baseTrack.copy(
                fileName = "quality.flac",
                mimeType = "audio/flac",
                sampleRateHz = 192_000,
                bitDepth = 16,
            ).resolveAudioQuality(),
        )
        assertEquals(
            AudioQuality.SQ,
            baseTrack.copy(
                fileName = "quality.flac",
                mimeType = "audio/flac",
                sampleRateHz = 44_099,
                bitDepth = 32,
            ).resolveAudioQuality(),
        )
        assertEquals(
            AudioQuality.HQ,
            baseTrack.copy(bitrateBitsPerSecond = 320_000).resolveAudioQuality(),
        )
        assertNull(
            baseTrack.copy(bitrateBitsPerSecond = 319_999).resolveAudioQuality(),
        )
        assertNull(baseTrack.resolveAudioQuality())
    }

    @Test
    fun audioQualityDoesNotGuessFromFileSize() {
        val track = musicTrack(
            id = 10L,
            title = "Estimated",
            dateAddedEpochSeconds = 1L,
            fileName = "estimated.m4a",
            fileSizeBytes = 7_200_000L,
            durationMs = 180_000L,
        )

        assertNull(track.resolveAudioQuality())
    }

    @Test
    fun tagLibAudioPropertiesNormalizeToStoredUnits() {
        assertEquals(
            LocalAudioProperties(
                durationMs = 180_000L,
                bitrateBitsPerSecond = 1_411_000,
                sampleRateHz = 96_000,
                channelCount = 2,
            ),
            normalizeAudioProperties(
                durationMs = 180_000,
                bitrateKbps = 1_411,
                sampleRateHz = 96_000,
                channelCount = 2,
            ),
        )
        assertEquals(
            LocalAudioProperties(
                durationMs = null,
                bitrateBitsPerSecond = null,
                sampleRateHz = null,
                channelCount = null,
            ),
            normalizeAudioProperties(
                durationMs = 0,
                bitrateKbps = -1,
                sampleRateHz = 0,
                channelCount = 0,
            ),
        )
    }

    @Test
    fun tagLibPropertyMapResolvesLibraryFields() {
        assertEquals(
            com.melox.player.data.library.LocalAudioTags(
                title = "Title",
                artist = "Artist A/Artist B",
                album = "Album",
                albumArtist = "Album Artist",
                year = 2024,
                trackNumber = 3,
                discNumber = 2,
            ),
            parseAudioTagProperties(
                mapOf(
                    "TITLE" to arrayOf(" Title "),
                    "ARTIST" to arrayOf("Artist A", "Artist B"),
                    "ALBUM" to arrayOf("Album"),
                    "ALBUM ARTIST" to arrayOf("Album Artist"),
                    "DATE" to arrayOf("2024-08-04"),
                    "TRACKNUMBER" to arrayOf("3/12"),
                    "DISCNUMBER" to arrayOf("2/2"),
                ),
            ),
        )
    }

    @Test
    fun tagLibPropertyMapResolvesWavAndId3Aliases() {
        assertEquals(
            com.melox.player.data.library.LocalAudioTags(
                title = "WAV title",
                artist = "WAV artist",
                album = "WAV album",
                albumArtist = "Album artist",
                year = 2025,
                trackNumber = 4,
                discNumber = 1,
            ),
            parseAudioTagProperties(
                mapOf(
                    "INAM" to arrayOf("WAV title"),
                    "IART" to arrayOf("WAV artist"),
                    "IPRD" to arrayOf("WAV album"),
                    "TPE2" to arrayOf("Album artist"),
                    "DATE" to arrayOf("2025"),
                    "ITRK" to arrayOf("4/10"),
                    "DISKNUMBER" to arrayOf("1/1"),
                ),
            ),
        )
        assertEquals(
            "ID3 title",
            parseAudioTagProperties(mapOf("TIT2" to arrayOf("ID3 title"))).title,
        )
    }

    @Test
    fun audioPropertiesReuseRequiresAnUnchangedCompletedSourceRead() {
        val track = musicTrack(
            id = 11L,
            title = "Cached",
            dateAddedEpochSeconds = 1L,
            fileName = "cached.flac",
            fileSizeBytes = 4_000L,
            durationMs = 5_000L,
            dateModifiedEpochSeconds = 3L,
        ).copy(audioPropertiesScanned = true)

        assertEquals(
            true,
            track.hasReusableAudioProperties(
                id = 11L,
                contentUri = "content://music/11",
                dateModifiedEpochSeconds = 3L,
                fileSizeBytes = 4_000L,
            ),
        )
        assertEquals(
            false,
            track.copy(audioPropertiesScanned = false).hasReusableAudioProperties(
                id = 11L,
                contentUri = "content://music/11",
                dateModifiedEpochSeconds = 3L,
                fileSizeBytes = 4_000L,
            ),
        )
        assertEquals(
            false,
            track.hasReusableAudioProperties(
                id = 11L,
                contentUri = "content://music/11",
                dateModifiedEpochSeconds = 4L,
                fileSizeBytes = 4_000L,
            ),
        )
    }

    @Test
    fun musicLibrarySnapshotReadsVersionOneWithSafeArtworkFallback() {
        val original = musicTrack(
            id = 8L,
            title = "Legacy",
            dateAddedEpochSeconds = 12L,
            fileName = "legacy.mp3",
            fileSizeBytes = 34L,
            durationMs = 56L,
        ).copy(
            artist = "Artist",
            album = "Album",
        )

        assertEquals(
            listOf(original),
            MusicLibrarySnapshotCodec.read(
                ByteArrayInputStream(encodeVersionOneSnapshot(original)),
            ),
        )
    }

    @Test
    fun musicLibrarySnapshotVersionSixForcesCompleteMetadataRefresh() {
        val original = musicTrack(
            id = 12L,
            title = "Legacy properties",
            dateAddedEpochSeconds = 13L,
            fileName = "legacy.flac",
            fileSizeBytes = 14L,
            durationMs = 15L,
            dateModifiedEpochSeconds = 16L,
        ).copy(
            artist = "Artist",
            album = "Album",
            albumArtist = "Album Artist",
            year = 2020,
            mimeType = "audio/flac",
            bitrateBitsPerSecond = 1_411_000,
            sampleRateHz = 96_000,
            channelCount = 2,
            audioPropertiesScanned = true,
        )

        val restored = MusicLibrarySnapshotCodec.read(
            ByteArrayInputStream(encodeVersionSixSnapshot(original)),
        ).single()

        assertFalse(restored.audioPropertiesScanned)
        assertNull(restored.trackNumber)
        assertNull(restored.discNumber)
        assertNull(restored.bitDepth)
    }

    @Test
    fun artworkCacheIdentityChangesWithSourceRevisionAndSize() {
        val base = createArtworkCacheKey(
            contentUri = "content://music/1",
            dateModifiedEpochSeconds = 2L,
            fileSizeBytes = 3L,
            targetSizePx = 48,
        )

        assertEquals(64, artworkCacheFileStem(base).length)
        assertEquals(artworkCacheFileStem(base), artworkCacheFileStem(base))
        assertNotEquals(
            base,
            createArtworkCacheKey("content://music/1", 4L, 3L, 48),
        )
        assertNotEquals(
            base,
            createArtworkCacheKey("content://music/1", 2L, 5L, 48),
        )
        assertNotEquals(
            base,
            createArtworkCacheKey("content://music/1", 2L, 3L, 96),
        )
    }

    @Test
    fun artworkThumbnailDimensionsPreserveAspectRatioWithoutUpscaling() {
        assertEquals(256 to 256, fitArtworkDimensions(512, 512, 256))
        assertEquals(256 to 128, fitArtworkDimensions(1024, 512, 256))
        assertEquals(128 to 256, fitArtworkDimensions(512, 1024, 256))
        assertEquals(120 to 80, fitArtworkDimensions(120, 80, 256))
    }

    @Test
    fun artworkDiskCacheSnapshotsMutableFileMetadataBeforeSorting() {
        val newer = CountingMetadataFile("newer.png", lastModifiedValue = 20L)
        val older = CountingMetadataFile("older.png", lastModifiedValue = 10L)

        val entries = snapshotArtworkDiskCacheEntries(arrayOf(newer, older))

        assertEquals(listOf("older.png", "newer.png"), entries.map { it.file.name })
        assertEquals(1, newer.lastModifiedCalls)
        assertEquals(1, older.lastModifiedCalls)
        assertEquals(1, newer.lengthCalls)
        assertEquals(1, older.lengthCalls)
    }

    @Test
    fun musicLibrarySnapshotRejectsTruncatedAndCorruptData() {
        val encoded = ByteArrayOutputStream().also { output ->
            MusicLibrarySnapshotCodec.write(
                output,
                listOf(
                    musicTrack(
                        id = 1L,
                        title = "Track",
                        dateAddedEpochSeconds = 2L,
                        fileName = "track.mp3",
                        fileSizeBytes = 3L,
                        durationMs = 4L,
                    ),
                ),
            )
        }.toByteArray()

        assertThrows(IOException::class.java) {
            MusicLibrarySnapshotCodec.read(
                ByteArrayInputStream(encoded.copyOf(encoded.size - 1)),
            )
        }

        val corrupted = encoded.copyOf().also { bytes ->
            bytes[bytes.lastIndex - Long.SIZE_BYTES] =
                (bytes[bytes.lastIndex - Long.SIZE_BYTES].toInt() xor 1).toByte()
        }
        assertThrows(IOException::class.java) {
            MusicLibrarySnapshotCodec.read(ByteArrayInputStream(corrupted))
        }
    }

    @Test
    fun playbackSnapshotRoundTripsQueueAndTransportState() {
        val firstItem = PlaybackQueueItem(
            mediaId = "7",
            trackId = 7L,
            contentUri = "content://media/audio/7",
            title = "Track",
            artist = "Artist",
            album = "Album",
            durationMs = 123_000L,
            dateModifiedEpochSeconds = 456L,
            fileSizeBytes = 789L,
            sourceOrder = 2.0,
            playbackMode = PlaybackMode.RANDOM,
        )
        val secondItem = firstItem.copy(
            mediaId = "8",
            trackId = 8L,
            contentUri = "content://media/audio/8",
            title = "Track 2",
            sourceOrder = 3.0,
        )
        val snapshot = PlaybackSnapshot(
            queue = listOf(firstItem, secondItem),
            currentIndex = 1,
            positionMs = 12_000L,
            playbackMode = PlaybackMode.RANDOM,
        )
        val encoded = ByteArrayOutputStream().also { output ->
            PlaybackSnapshotCodec.write(output, snapshot)
        }.toByteArray()

        assertEquals(
            snapshot,
            PlaybackSnapshotCodec.read(ByteArrayInputStream(encoded)),
        )
        assertThrows(IOException::class.java) {
            PlaybackSnapshotCodec.read(
                ByteArrayInputStream(encoded.copyOf(encoded.size - 3)),
            )
        }

        val invalidPlaybackMode = encoded.copyOf().also { bytes ->
            val modeOffset = bytes.size - Long.SIZE_BYTES - Int.SIZE_BYTES
            bytes[modeOffset] = 0
            bytes[modeOffset + 1] = 0
            bytes[modeOffset + 2] = 0
            bytes[modeOffset + 3] = 99
        }
        assertThrows(IOException::class.java) {
            PlaybackSnapshotCodec.read(ByteArrayInputStream(invalidPlaybackMode))
        }
    }

    @Test
    fun playbackRestorePrunesUnreadableItemsWithoutLosingCurrentPosition() {
        val queue = listOf(
            playbackQueueItem("missing-before"),
            playbackQueueItem("current"),
            playbackQueueItem("after"),
        )
        val snapshot = PlaybackSnapshot(
            queue = queue,
            currentIndex = 1,
            positionMs = 12_345L,
            playbackMode = PlaybackMode.ORDER,
        )

        val retainedCurrent = snapshot.retainReadableItems { it.mediaId != "missing-before" }
        assertEquals(listOf("current", "after"), retainedCurrent?.queue?.map { it.mediaId })
        assertEquals(0, retainedCurrent?.currentIndex)
        assertEquals(12_345L, retainedCurrent?.positionMs)

        val missingCurrent = snapshot.retainReadableItems { it.mediaId == "after" }
        assertEquals(0, missingCurrent?.currentIndex)
        assertEquals(0L, missingCurrent?.positionMs)

        assertNull(snapshot.retainReadableItems { false })
    }

    @Test
    fun playbackSnapshotReadsVersionOneShuffleAsRandomQueue() {
        val item = playbackQueueItem("legacy")
        val restored = PlaybackSnapshotCodec.read(
            ByteArrayInputStream(
                encodeVersionOnePlaybackSnapshot(
                    item = item,
                    shuffleEnabled = true,
                    repeatMode = 2,
                ),
            ),
        )

        assertEquals(PlaybackMode.RANDOM, restored.playbackMode)
        assertEquals(PlaybackMode.RANDOM, restored.queue.single().playbackMode)
        assertEquals(0.0, restored.queue.single().sourceOrder, 0.0)
    }

    @Test
    fun playbackModesCycleAndRandomizeQueueOnce() {
        assertEquals(PlaybackMode.REPEAT_ONE, nextPlaybackMode(PlaybackMode.ORDER))
        assertEquals(PlaybackMode.RANDOM, nextPlaybackMode(PlaybackMode.REPEAT_ONE))
        assertEquals(PlaybackMode.ORDER, nextPlaybackMode(PlaybackMode.RANDOM))

        val sourceQueue = listOf("a", "b", "c", "d").mapIndexed { index, mediaId ->
            playbackQueueItem(mediaId).copy(sourceOrder = index.toDouble())
        }
        val randomized = reorderQueueForPlaybackMode(
            queue = sourceQueue,
            currentIndex = 1,
            targetMode = PlaybackMode.RANDOM,
            random = Random(17),
        )
        assertEquals("b", randomized.queue.first().mediaId)
        assertEquals(0, randomized.currentIndex)
        assertEquals(sourceQueue.map { it.mediaId }.toSet(), randomized.queue.map { it.mediaId }.toSet())
        assertEquals(
            setOf(PlaybackMode.RANDOM),
            randomized.queue.map { it.playbackMode }.toSet(),
        )

        val restored = reorderQueueForPlaybackMode(
            queue = randomized.queue,
            currentIndex = randomized.currentIndex,
            targetMode = PlaybackMode.ORDER,
        )
        assertEquals(listOf("a", "b", "c", "d"), restored.queue.map { it.mediaId })
        assertEquals(1, restored.currentIndex)
    }

    @Test
    fun pendingPlaybackModeKeepsTheNewIconUntilThePlayerConfirmsIt() {
        assertEquals(
            PlaybackMode.RANDOM,
            displayedPlaybackMode(
                playerMode = PlaybackMode.REPEAT_ONE,
                pendingMode = PlaybackMode.RANDOM,
            ),
        )
        assertEquals(
            PlaybackMode.ORDER,
            displayedPlaybackMode(
                playerMode = PlaybackMode.ORDER,
                pendingMode = null,
            ),
        )
    }

    @Test
    fun randomToOrderReplacementKeepsCurrentSlotOutsideTwoBulkSpans() {
        val sourceQueue = List(100_000) { index ->
            playbackQueueItem(index.toString()).copy(sourceOrder = index.toDouble())
        }
        val randomized = reorderQueueForPlaybackMode(
            queue = sourceQueue,
            currentIndex = 75_000,
            targetMode = PlaybackMode.RANDOM,
            random = Random(17),
        )
        val restored = reorderQueueForPlaybackMode(
            queue = randomized.queue,
            currentIndex = randomized.currentIndex,
            targetMode = PlaybackMode.ORDER,
        )
        val replacement = playbackQueueReplacement(
            targetQueue = restored.queue,
            currentIndex = randomized.currentIndex,
            targetCurrentIndex = restored.currentIndex,
        )

        assertEquals(75_000, replacement.beforeCurrent.size)
        assertEquals(24_999, replacement.afterCurrent.size)
        assertTrue(replacement.replaceAfterCurrentFirst)
        assertEquals("74999", replacement.beforeCurrent.last().mediaId)
        assertEquals("75001", replacement.afterCurrent.first().mediaId)
        assertFalse(
            (replacement.beforeCurrent + replacement.afterCurrent)
                .any { item ->
                    item.mediaId == restored.queue[restored.currentIndex].mediaId &&
                        item.contentUri == restored.queue[restored.currentIndex].contentUri &&
                        item.sourceOrder == restored.queue[restored.currentIndex].sourceOrder
                },
        )
    }

    @Test
    fun queueReplacementExcludesOnlyTheCurrentRepeatedSlot() {
        val targetQueue = listOf(
            playbackQueueItem("repeat").copy(sourceOrder = 0.0),
            playbackQueueItem("repeat").copy(sourceOrder = 1.0),
            playbackQueueItem("other").copy(sourceOrder = 2.0),
        )

        val replacement = playbackQueueReplacement(
            targetQueue = targetQueue,
            currentIndex = 2,
            targetCurrentIndex = 1,
        )

        assertEquals(listOf(0.0), replacement.beforeCurrent.map(PlaybackQueueItem::sourceOrder))
        assertEquals(listOf(2.0), replacement.afterCurrent.map(PlaybackQueueItem::sourceOrder))
        assertFalse(replacement.replaceAfterCurrentFirst)
    }

    @Test
    fun randomModeKeepsCurrentItemAndModeMetadata() {
        val sourceQueue = listOf("a", "b").mapIndexed { index, mediaId ->
            playbackQueueItem(mediaId).copy(sourceOrder = index.toDouble())
        }

        val randomized = reorderQueueForPlaybackMode(
            queue = sourceQueue,
            currentIndex = 0,
            targetMode = PlaybackMode.RANDOM,
            random = Random(1),
        )

        assertEquals("a", randomized.queue.first().mediaId)
        assertEquals(setOf(PlaybackMode.RANDOM), randomized.queue.map { it.playbackMode }.toSet())
    }

    @Test
    fun randomModeRetainsRepeatedQueueSlots() {
        val sourceQueue = listOf(
            playbackQueueItem("repeat").copy(sourceOrder = 0.0),
            playbackQueueItem("repeat").copy(sourceOrder = 1.0),
            playbackQueueItem("other").copy(sourceOrder = 2.0),
        )

        val randomized = reorderQueueForPlaybackMode(
            queue = sourceQueue,
            currentIndex = 1,
            targetMode = PlaybackMode.RANDOM,
            random = Random(1),
        )

        assertEquals(sourceQueue.size, randomized.queue.size)
        assertEquals(1.0, randomized.queue.first().sourceOrder, 0.0)
        assertEquals(0, randomized.currentIndex)
        assertEquals(
            sourceQueue.map(PlaybackQueueItem::sourceOrder).sorted(),
            randomized.queue.map(PlaybackQueueItem::sourceOrder).sorted(),
        )
    }

    @Test
    fun miniPlaybackSnapshotSeedsLastCurrentItemBeforeServiceConnection() {
        val queue = List(24) { index -> playbackQueueItem(index.toString()) }
        val item = queue[12].copy(
            durationMs = 123_000L,
            playbackMode = PlaybackMode.RANDOM,
        )
        val snapshotQueue = queue.toMutableList().also { items -> items[12] = item }

        val preview = PlaybackSnapshot(
            queue = snapshotQueue,
            currentIndex = 12,
            positionMs = 4_000L,
            playbackMode = PlaybackMode.RANDOM,
        ).toStartupPlaybackPreview(maxItemCount = 10)

        requireNotNull(preview)
        val state = preview.toInitialPlaybackState()

        assertEquals(item, state.currentItem)
        assertEquals(10, state.queue.size)
        assertEquals(0, state.currentIndex)
        assertEquals(4_000L, state.positionMs)
        assertEquals(PlaybackMode.RANDOM, state.playbackMode)
        assertFalse(state.isPlaying)
    }

    @Test
    fun startupPlaybackPreviewUsesTheLastFullWindowNearQueueEnd() {
        val queue = List(24) { index -> playbackQueueItem(index.toString()) }

        val preview = PlaybackSnapshot(
            queue = queue,
            currentIndex = 22,
            positionMs = 0L,
            playbackMode = PlaybackMode.ORDER,
        ).toStartupPlaybackPreview(maxItemCount = 10)

        requireNotNull(preview)
        assertEquals(queue.subList(14, 24), preview.queue)
        assertEquals(8, preview.currentIndex)
    }

    @Test
    fun stagedPlaybackValidationPreservesTheActiveQueueSlotAndPosition() {
        val first = playbackQueueItem("duplicate").copy(sourceOrder = 0.0)
        val second = playbackQueueItem("duplicate").copy(sourceOrder = 1.0)
        val third = playbackQueueItem("third").copy(sourceOrder = 2.0)
        val restored = PlaybackSnapshot(
            queue = listOf(first, second, third),
            currentIndex = 0,
            positionMs = 1_000L,
            playbackMode = PlaybackMode.ORDER,
        )
        val validated = restored.copy(queue = listOf(second, third))

        val reconciled = reconcileValidatedPlaybackSnapshot(
            restoredSnapshot = restored,
            validatedSnapshot = validated,
            currentQueue = restored.queue,
            currentIndex = 1,
            positionMs = 8_000L,
            playbackMode = PlaybackMode.REPEAT_ONE,
        )

        requireNotNull(reconciled)
        assertEquals(
            listOf(second, third).map { item ->
                item.copy(playbackMode = PlaybackMode.REPEAT_ONE)
            },
            reconciled.queue,
        )
        assertEquals(0, reconciled.currentIndex)
        assertEquals(8_000L, reconciled.positionMs)
        assertEquals(PlaybackMode.REPEAT_ONE, reconciled.playbackMode)
    }

    @Test
    fun stagedPlaybackValidationDoesNotReplaceAUserMutatedQueue() {
        val restored = PlaybackSnapshot(
            queue = listOf(playbackQueueItem("one"), playbackQueueItem("two")),
            currentIndex = 0,
            positionMs = 0L,
            playbackMode = PlaybackMode.ORDER,
        )

        assertNull(
            reconcileValidatedPlaybackSnapshot(
                restoredSnapshot = restored,
                validatedSnapshot = restored,
                currentQueue = restored.queue.dropLast(1),
                currentIndex = 0,
                positionMs = 0L,
                playbackMode = PlaybackMode.ORDER,
            ),
        )
        assertTrue(hasSameQueueSlots(restored.queue, restored.queue))
        assertFalse(hasSameQueueSlots(restored.queue, restored.queue.reversed()))
    }

    @Test
    fun bottomBarBooleansResolveToExpectedNavigationStyle() {
        assertEquals(BottomBarStyle.NORMAL, AppSettings().bottomBarStyle)
        assertEquals(
            BottomBarStyle.FLOATING,
            AppSettings(floatingBottomBar = true).bottomBarStyle,
        )
        assertEquals(
            BottomBarStyle.LIQUID_GLASS,
            AppSettings(floatingBottomBar = true, liquidGlass = true).bottomBarStyle,
        )
    }

    @Test
    fun floatingBottomBarToggleRetainsLiquidGlassPreference() {
        val settings = AppSettings(floatingBottomBar = false, liquidGlass = true)

        assertEquals(BottomBarStyle.NORMAL, settings.bottomBarStyle)
        assertEquals(
            BottomBarStyle.LIQUID_GLASS,
            settings.copy(floatingBottomBar = true).bottomBarStyle,
        )
    }

    @Test
    fun queueOperationIndicesAreDeterministicAndBoundsChecked() {
        assertEquals(0, nextQueueInsertionIndex(currentIndex = 0, itemCount = 0))
        assertEquals(1, nextQueueInsertionIndex(currentIndex = 0, itemCount = 3))
        assertEquals(3, nextQueueInsertionIndex(currentIndex = 2, itemCount = 3))
        assertEquals(0, nextQueueInsertionIndex(currentIndex = -1, itemCount = 3))

        assertEquals(true, isValidQueueIndex(index = 0, itemCount = 1))
        assertEquals(true, isValidQueueIndex(index = 2, itemCount = 3))
        assertFalse(isValidQueueIndex(index = -1, itemCount = 3))
        assertFalse(isValidQueueIndex(index = 3, itemCount = 3))
        assertFalse(isValidQueueIndex(index = 0, itemCount = 0))

        assertEquals(true, isValidQueueMove(fromIndex = 0, toIndex = 2, itemCount = 3))
        assertFalse(isValidQueueMove(fromIndex = 1, toIndex = 1, itemCount = 3))
        assertFalse(isValidQueueMove(fromIndex = -1, toIndex = 1, itemCount = 3))
        assertFalse(isValidQueueMove(fromIndex = 1, toIndex = 3, itemCount = 3))
    }

    @Test
    fun queueDraftMoveAndPlayNextTargetPreserveSlotIdentity() {
        val queue = listOf("a", "b", "current", "d", "e")

        assertEquals(
            listOf("b", "current", "a", "d", "e"),
            moveQueueListItem(queue, fromIndex = 0, toIndex = 2),
        )
        assertEquals(
            2,
            queueMoveTargetAfterCurrent(itemIndex = 0, currentIndex = 2, itemCount = 5),
        )
        assertEquals(
            3,
            queueMoveTargetAfterCurrent(itemIndex = 4, currentIndex = 2, itemCount = 5),
        )
        assertNull(
            queueMoveTargetAfterCurrent(itemIndex = 2, currentIndex = 2, itemCount = 5),
        )
        assertEquals("current", moveQueueListItem(queue, 2, 4)[4])
    }

    @Test
    fun queuePlayNextActionSkipsCurrentAndImmediateNextItems() {
        assertFalse(queueShowsPlayNextAction(itemIndex = 2, currentIndex = 2, itemCount = 5))
        assertFalse(queueShowsPlayNextAction(itemIndex = 3, currentIndex = 2, itemCount = 5))
        assertTrue(queueShowsPlayNextAction(itemIndex = 1, currentIndex = 2, itemCount = 5))
        assertTrue(queueShowsPlayNextAction(itemIndex = 4, currentIndex = 2, itemCount = 5))
        assertFalse(queueShowsPlayNextAction(itemIndex = 0, currentIndex = -1, itemCount = 5))
    }

    @Test
    fun queueOffscreenPlayNextAnimationUsesTheRequestedViewportEdge() {
        assertEquals(
            -308f,
            queueOffscreenExitTranslation(
                itemIndex = 4,
                currentIndex = 1,
                itemOffsetPx = 240,
                itemSizePx = 68,
                viewportStartPx = 0,
                viewportEndPx = 600,
            ),
            0f,
        )
        assertEquals(
            480f,
            queueOffscreenExitTranslation(
                itemIndex = 1,
                currentIndex = 4,
                itemOffsetPx = 120,
                itemSizePx = 68,
                viewportStartPx = 0,
                viewportEndPx = 600,
            ),
            0f,
        )
    }

    @Test
    fun queueOffscreenPlayNextFadesOnlyAcrossTheViewportBoundary() {
        assertEquals(
            1f,
            queueOffscreenExitAlpha(
                itemIndex = 4,
                currentIndex = 1,
                itemOffsetPx = 240,
                itemSizePx = 68,
                translationY = -240f,
                viewportStartPx = 0,
                viewportEndPx = 600,
            ),
            0f,
        )
        assertEquals(
            0.5f,
            queueOffscreenExitAlpha(
                itemIndex = 4,
                currentIndex = 1,
                itemOffsetPx = 240,
                itemSizePx = 68,
                translationY = -274f,
                viewportStartPx = 0,
                viewportEndPx = 600,
            ),
            0f,
        )
        assertEquals(
            0f,
            queueOffscreenExitAlpha(
                itemIndex = 1,
                currentIndex = 4,
                itemOffsetPx = 120,
                itemSizePx = 68,
                translationY = 480f,
                viewportStartPx = 0,
                viewportEndPx = 600,
            ),
            0f,
        )
    }

    @Test
    fun queueOffscreenPlayNextMovesCrossedRowsOutOfTheGap() {
        assertEquals(
            34f,
            queuePlayNextPeerTranslation(
                itemIndex = 3,
                movingIndex = 5,
                currentIndex = 1,
                rowHeightPx = 68f,
                progress = 0.5f,
            ),
            0f,
        )
        assertEquals(
            -68f,
            queuePlayNextPeerTranslation(
                itemIndex = 2,
                movingIndex = 1,
                currentIndex = 4,
                rowHeightPx = 68f,
                progress = 1f,
            ),
            0f,
        )
        assertEquals(
            0f,
            queuePlayNextPeerTranslation(
                itemIndex = 5,
                movingIndex = 5,
                currentIndex = 1,
                rowHeightPx = 68f,
                progress = 1f,
            ),
            0f,
        )
    }

    @Test
    fun queueLocationAddsOnlyTheSpaceNeededToPlaceCurrentFirst() {
        assertEquals(
            0.dp,
            queueLocationAnchorHeight(
                itemCount = 10,
                currentIndex = 1,
                rowHeight = 68.dp,
                viewportHeight = 600.dp,
                bottomPadding = 24.dp,
            ),
        )
        assertEquals(
            508.dp,
            queueLocationAnchorHeight(
                itemCount = 10,
                currentIndex = 9,
                rowHeight = 68.dp,
                viewportHeight = 600.dp,
                bottomPadding = 24.dp,
            ),
        )
        assertEquals(
            0.dp,
            queueLocationAnchorHeight(
                itemCount = 0,
                currentIndex = -1,
                rowHeight = 68.dp,
                viewportHeight = 600.dp,
                bottomPadding = 24.dp,
            ),
        )
    }

    @Test
    fun queueListHeightCapsLargeRestoredQueuesBeforeMultiplication() {
        val rowHeight = 68.dp
        val maxHeight = 600.dp

        assertEquals(0.dp, queueListHeight(itemCount = 0, rowHeight, maxHeight))
        assertEquals(204.dp, queueListHeight(itemCount = 3, rowHeight, maxHeight))
        assertEquals(maxHeight, queueListHeight(itemCount = 100_000, rowHeight, maxHeight))
        assertEquals(maxHeight, queueListHeight(itemCount = Int.MAX_VALUE, rowHeight, maxHeight))
    }

    @Test
    fun playNextPreservesOrderQueuePositionOutsideRandomMode() {
        val queue = listOf(
            playbackQueueItem("a").copy(sourceOrder = 0.0),
            playbackQueueItem("b").copy(sourceOrder = 1.0),
            playbackQueueItem("c").copy(sourceOrder = 2.0),
        )

        assertEquals(
            0.5,
            sourceOrderForPlayNext(
                queue,
                currentIndex = 0,
                playbackMode = PlaybackMode.ORDER,
            ),
            0.0,
        )
        assertEquals(
            3.0,
            sourceOrderForPlayNext(
                queue,
                currentIndex = 1,
                playbackMode = PlaybackMode.RANDOM,
            ),
            0.0,
        )
    }
}

private class CountingMetadataFile(
    path: String,
    private val lastModifiedValue: Long,
) : File(path) {
    var lastModifiedCalls: Int = 0
        private set
    var lengthCalls: Int = 0
        private set

    override fun isFile(): Boolean = true

    override fun lastModified(): Long {
        lastModifiedCalls += 1
        return lastModifiedValue
    }

    override fun length(): Long {
        lengthCalls += 1
        return 1L
    }
}

private fun encodeVersionOneSnapshot(track: MusicTrack): ByteArray {
    val encoded = ByteArrayOutputStream()
    val checksum = CRC32()
    val output = DataOutputStream(
        CheckedOutputStream(BufferedOutputStream(encoded), checksum),
    )
    output.writeInt(0x5549584D)
    output.writeInt(1)
    output.writeInt(1)
    output.writeLong(track.id)
    output.writeNullableString(track.title)
    output.writeNullableString(track.artist)
    output.writeNullableString(track.album)
    output.writeLong(track.durationMs)
    output.writeLong(track.dateAddedEpochSeconds)
    output.writeNullableString(track.fileName)
    output.writeLong(track.fileSizeBytes)
    output.writeSizedString(track.contentUri)
    output.writeSizedString(track.titleSectionKey)
    output.writeSizedString(track.titleSortKey)
    output.writeLong(checksum.value)
    output.flush()
    return encoded.toByteArray()
}

private fun encodeVersionSixSnapshot(track: MusicTrack): ByteArray {
    val encoded = ByteArrayOutputStream()
    val checksum = CRC32()
    val output = DataOutputStream(
        CheckedOutputStream(BufferedOutputStream(encoded), checksum),
    )
    output.writeInt(0x5549584D)
    output.writeInt(6)
    output.writeInt(1)
    output.writeLong(track.id)
    output.writeNullableString(track.title)
    output.writeNullableString(track.artist)
    output.writeNullableString(track.album)
    output.writeNullableString(track.albumArtist)
    output.writeInt(track.year ?: 0)
    output.writeLong(track.durationMs)
    output.writeLong(track.dateAddedEpochSeconds)
    output.writeLong(track.dateModifiedEpochSeconds)
    output.writeNullableString(track.fileName)
    output.writeNullableString(track.folderPath)
    output.writeLong(track.albumId ?: 0L)
    output.writeLong(track.fileSizeBytes)
    output.writeSizedString(track.contentUri)
    output.writeSizedString(track.titleSectionKey)
    output.writeSizedString(track.titleSortKey)
    output.writeNullableString(track.mimeType)
    output.writeInt(track.bitrateBitsPerSecond ?: 0)
    output.writeInt(track.sampleRateHz ?: 0)
    output.writeInt(track.channelCount ?: 0)
    output.writeBoolean(track.audioPropertiesScanned)
    output.writeLong(checksum.value)
    output.flush()
    return encoded.toByteArray()
}

private fun encodeVersionOnePlaybackSnapshot(
    item: PlaybackQueueItem,
    shuffleEnabled: Boolean,
    repeatMode: Int,
): ByteArray {
    val encoded = ByteArrayOutputStream()
    val checksum = CRC32()
    val output = DataOutputStream(
        CheckedOutputStream(BufferedOutputStream(encoded), checksum),
    )
    output.writeInt(0x4D454C50)
    output.writeInt(1)
    output.writeInt(1)
    output.writeSizedString(item.mediaId)
    output.writeNullableLong(item.trackId)
    output.writeSizedString(item.contentUri)
    output.writeSizedString(item.title)
    output.writeNullableString(item.artist)
    output.writeNullableString(item.album)
    output.writeLong(item.durationMs)
    output.writeLong(item.dateModifiedEpochSeconds)
    output.writeLong(item.fileSizeBytes)
    output.writeInt(0)
    output.writeLong(12_000L)
    output.writeBoolean(shuffleEnabled)
    output.writeInt(repeatMode)
    output.writeLong(checksum.value)
    output.flush()
    return encoded.toByteArray()
}

private fun DataOutputStream.writeNullableLong(value: Long?) {
    writeBoolean(value != null)
    if (value != null) writeLong(value)
}

private fun DataOutputStream.writeNullableString(value: String?) {
    writeBoolean(value != null)
    if (value != null) writeSizedString(value)
}

private fun DataOutputStream.writeSizedString(value: String) {
    val bytes = value.toByteArray(Charsets.UTF_8)
    writeInt(bytes.size)
    write(bytes)
}

private fun musicTrack(
    id: Long,
    title: String,
    dateAddedEpochSeconds: Long,
    fileName: String,
    fileSizeBytes: Long,
    durationMs: Long,
    dateModifiedEpochSeconds: Long = 0L,
    artist: String? = null,
): MusicTrack {
    val sortKeys = createMusicSortKeys(title)
    return MusicTrack(
        id = id,
        title = title,
        artist = artist,
        album = null,
        durationMs = durationMs,
        dateAddedEpochSeconds = dateAddedEpochSeconds,
        dateModifiedEpochSeconds = dateModifiedEpochSeconds,
        fileName = fileName,
        fileSizeBytes = fileSizeBytes,
        contentUri = "content://music/$id",
        titleSectionKey = sortKeys.section,
        titleSortKey = sortKeys.value,
    )
}

private fun playbackQueueItem(mediaId: String): PlaybackQueueItem = PlaybackQueueItem(
    mediaId = mediaId,
    trackId = null,
    contentUri = "content://music/$mediaId",
    title = mediaId,
    artist = null,
    album = null,
    durationMs = 1L,
    dateModifiedEpochSeconds = 1L,
    fileSizeBytes = 1L,
)

private fun List<MusicTrack>.sortedIds(
    field: MusicSortField,
    descending: Boolean = false,
): List<Long> = sortMusicTracks(
    tracks = this,
    config = MusicSortConfig(field = field, descending = descending),
).map(MusicTrack::id)
