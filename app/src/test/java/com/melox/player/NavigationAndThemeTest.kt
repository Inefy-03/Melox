package com.melox.player

import android.Manifest
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import com.melox.player.model.AppSettings
import com.melox.player.model.BottomBarStyle
import com.melox.player.model.DynamicColorSource
import com.melox.player.model.LyricsFormatPriority
import com.melox.player.model.LyricsSourcePriority
import com.melox.player.model.NavigationTransitionStyle
import com.melox.player.model.PlaybackBackgroundStyle
import com.melox.player.model.ThemeMode
import com.melox.player.ui.component.glassIcon
import com.melox.player.ui.component.glassSidebarIcon
import com.melox.player.ui.component.library.shouldClearSearchFocusAfterImeDismissed
import com.melox.player.ui.component.liquid.floatingNavigationBarBottomPadding
import com.melox.player.ui.component.topBarGlassIcon
import com.melox.player.ui.floatingBottomBarAvailableWidth
import com.melox.player.ui.floatingBottomBarBottomPadding
import com.melox.player.ui.floatingMiniPlayerBottomPaddingWhenNavigationIsHidden
import com.melox.player.ui.icon.*
import com.melox.player.ui.isMiuixWideLayout
import com.melox.player.ui.navigation.navigationTransitionCornerRadius
import com.melox.player.ui.navigation.ordinaryBackHandlerEnabled
import com.melox.player.ui.navigation.predictiveBackHandlerEnabled
import com.melox.player.ui.requiredAudioPermission
import com.melox.player.ui.resolveBottomBarStyle
import com.melox.player.ui.rootPagerUserScrollEnabled
import com.melox.player.ui.shouldShowNavigation
import com.melox.player.ui.shouldTriggerTabSwitchHaptic
import com.melox.player.ui.shouldUseNavigationRail
import com.melox.player.ui.theme.SettingsHapticFeedback
import com.melox.player.ui.theme.resolveDynamicColorSeed
import com.melox.player.ui.theme.toColorSchemeMode
import com.melox.player.ui.usesNormalMiniPlayerChrome
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.ColorSchemeMode

class NavigationAndThemeTest {
    @Test
    fun topBarTitleVisibilityTracksCollapseLimitChanges() {
        val state = top.yukonga.miuix.kmp.basic.TopAppBarState(-1000f, -100f, 0f)
        val visible = androidx.compose.runtime.derivedStateOf { state.collapsedFraction * 3f >= 1f }
        assertFalse(visible.value)
        androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
            state.heightOffsetLimit = -100f
        }
        assertTrue(visible.value)
    }

    @Test
    fun glassSearchResultsAreInteractiveOnlyForAnExpandedNonblankQuery() {
        assertFalse(com.melox.player.ui.component.showGlassSearchResults(false, "song"))
        assertFalse(com.melox.player.ui.component.showGlassSearchResults(true, ""))
        assertFalse(com.melox.player.ui.component.showGlassSearchResults(true, "  "))
        assertTrue(com.melox.player.ui.component.showGlassSearchResults(true, "song"))
        assertFalse(com.melox.player.ui.component.showGlassSearchResults(true, "song", ""))
        assertFalse(com.melox.player.ui.component.showGlassSearchResults(true, "song", "old"))
        assertFalse(com.melox.player.ui.component.showGlassSearchResults(true, "song", "son"))
        assertTrue(com.melox.player.ui.component.showGlassSearchResults(true, "song", "song"))
        assertFalse(com.melox.player.ui.component.showGlassSearchResults(false, "song", "song"))
    }

    @Test
    fun navigationStyleMigratesLegacyChoicesAndRetainsExplicitOverrides() {
        assertEquals(BottomBarStyle.GLASS, com.melox.player.model.resolveStoredNavigationStyle(null, null, null))
        assertEquals(BottomBarStyle.LIQUID_GLASS, com.melox.player.model.resolveStoredNavigationStyle(null, "FLOATING", null))
        assertEquals(BottomBarStyle.NORMAL, com.melox.player.model.resolveStoredNavigationStyle(null, "LIQUID_GLASS", false))
        assertEquals(BottomBarStyle.NORMAL, com.melox.player.model.resolveStoredNavigationStyle(null, "NORMAL", null))
        assertEquals(BottomBarStyle.NORMAL, com.melox.player.model.resolveStoredNavigationStyle(null, null, false))
        for (style in listOf(BottomBarStyle.NORMAL, BottomBarStyle.LIQUID_GLASS, BottomBarStyle.GLASS)) {
            assertEquals(style, com.melox.player.model.resolveStoredNavigationStyle(style.name, "NORMAL", false))
        }
        assertEquals(BottomBarStyle.LIQUID_GLASS, com.melox.player.model.resolveStoredNavigationStyle("unknown", null, true))
        assertEquals(BottomBarStyle.GLASS, resolveBottomBarStyle(BottomBarStyle.GLASS, liquidGlassSupported = false))
        assertEquals(BottomBarStyle.NORMAL, com.melox.player.model.resolveStoredNavigationStyle("OS3_FLOATING", null, true))
        assertFalse(AppSettings(selectedBottomBarStyle = BottomBarStyle.GLASS, smallPlayerBar = true).effectiveSmallPlayerBar)
        val compactPreference = AppSettings(selectedBottomBarStyle = BottomBarStyle.GLASS, smallPlayerBar = true)
        assertTrue(compactPreference.smallPlayerBar)
        assertTrue(compactPreference.copy(selectedBottomBarStyle = BottomBarStyle.LIQUID_GLASS).effectiveSmallPlayerBar)
        assertTrue(compactPreference.copy(selectedBottomBarStyle = BottomBarStyle.NORMAL).effectiveSmallPlayerBar)
        assertTrue(AppSettings(selectedBottomBarStyle = BottomBarStyle.LIQUID_GLASS, smallPlayerBar = true).effectiveSmallPlayerBar)

    }

    @Test
    fun glassIconMappingReplacesEveryOriginalSymbolUsedByTheApp() {
        val originalIcons = listOf(
            MiuixIcons.Add, MiuixIcons.AddCircle, MiuixIcons.Album, MiuixIcons.Back,
            MiuixIcons.ChevronBackward, MiuixIcons.Close, MiuixIcons.Contacts,
            MiuixIcons.ContactsCircle, MiuixIcons.Create, MiuixIcons.Delete,
            MiuixIcons.Download, MiuixIcons.Edit, MiuixIcons.Folder, MiuixIcons.FolderFill,
            MiuixIcons.Hide, MiuixIcons.Home, MiuixIcons.Info, MiuixIcons.More,
            MiuixIcons.Music, MiuixIcons.Notes, MiuixIcons.NotesFill, MiuixIcons.Playlist,
            MiuixIcons.RecordingTape, MiuixIcons.Reset, MiuixIcons.Search,
            MiuixIcons.SelectAll, MiuixIcons.Settings, MiuixIcons.Share,
            MiuixIcons.Sort, MiuixIcons.Translate, MiuixIcons.Tune,
        )
        originalIcons.forEach { original ->
            val glass = glassIcon(original)
            assertTrue(original.name, ".Glass." in glass.name)
            assertEquals(glass.name, glass, glassIcon(glass))
        }
        assertEquals("HomeFill.Glass.Regular", glassIcon(MiuixIcons.Home).name)
        assertEquals("AiScan.Glass.Regular", glassIcon(MiuixIcons.Album).name)
        assertEquals("Tune.Glass.Regular", glassIcon(MiuixIcons.Sort).name)
        assertEquals("Tune.Glass.Regular", topBarGlassIcon(MiuixIcons.Sort).name)
        assertEquals("Tune.Glass.Regular", topBarGlassIcon(MiuixIcons.Tune).name)
        assertEquals(28.dp, topBarGlassIcon(MiuixIcons.Sort).defaultWidth)
        assertEquals("SettingsOutline.Glass.Regular", topBarGlassIcon(MiuixIcons.Settings).name)
        assertEquals(28.dp, topBarGlassIcon(MiuixIcons.Settings).defaultWidth)
        assertEquals("RecentTasks.Glass.Regular", glassSidebarIcon(MiuixIcons.RecordingTape).name)
        assertEquals("Item.Glass.Regular", glassSidebarIcon(MiuixIcons.Album).name)
        assertEquals("Contacts.Glass.Regular", glassSidebarIcon(MiuixIcons.Contacts).name)
        assertEquals("FolderFill.Glass.Regular", glassSidebarIcon(MiuixIcons.Folder).name)
        listOf(
            GlassIcons.HomeFill, GlassIcons.NavigationMusic, GlassIcons.NavigationAlbum, GlassIcons.SettingsFill,
            glassSidebarIcon(MiuixIcons.RecordingTape), glassSidebarIcon(MiuixIcons.Album),
            glassSidebarIcon(MiuixIcons.Contacts), glassSidebarIcon(MiuixIcons.Folder),
        ).forEach { icon ->
            assertEquals(icon.name, 28.dp, icon.defaultWidth)
            assertEquals(icon.name, 28.dp, icon.defaultHeight)
        }
    }

    @Test
    fun previewHistoryRecordsInertialImpactsAndKeepsBothEdgesAfterRestoration() {
        val handoff = com.melox.player.ui.component.HorizontalEdgeHandoff()
        assertFalse(handoff.shouldHandleDrag(12f, canHandoff = true))
        assertFalse(handoff.shouldBounceInertia(0f, canHandoff = true))
        assertEquals(0, handoff.reachedEdges)
        assertTrue(handoff.shouldBounceInertia(320f, canHandoff = true))
        assertEquals(0, handoff.reachedEdges)
        handoff.recordInertialImpact(320f)
        assertFalse(handoff.shouldHandleDrag(12f, canHandoff = true))
        assertFalse(handoff.shouldBounceInertia(320f, canHandoff = true))
        assertTrue(handoff.shouldBounceInertia(-320f, canHandoff = true))
        handoff.recordInertialImpact(-320f)

        val restored = com.melox.player.ui.component.HorizontalEdgeHandoff(handoff.reachedEdges)
        assertFalse(restored.shouldHandleDrag(12f, canHandoff = true))
        assertFalse(restored.shouldHandleDrag(-12f, canHandoff = true))
        repeat(3) {
            assertTrue(restored.shouldHandleDrag(12f, canHandoff = false))
            assertTrue(restored.shouldHandleDrag(-12f, canHandoff = false))
            assertTrue(restored.shouldBounceInertia(320f, canHandoff = false))
            assertTrue(restored.shouldBounceInertia(-320f, canHandoff = false))
        }
    }

    @Test
    fun inertialFlingFeedsBoundaryVelocityToReboundAndOnlySnapsAfterNaturalRest() = runBlocking {
        val handoff = com.melox.player.ui.component.HorizontalEdgeHandoff()
        var decayRemaining = 320f
        var boundaryRemainder = -3f
        var reboundVisible = true
        val reboundVelocities = mutableListOf<Float>()
        val snapVelocities = mutableListOf<Float>()
        val decay = object : androidx.compose.foundation.gestures.FlingBehavior {
            override suspend fun androidx.compose.foundation.gestures.ScrollScope.performFling(initialVelocity: Float): Float {
                assertEquals(10f + boundaryRemainder, scrollBy(10f), 0f)
                return decayRemaining
            }
        }
        val snap = object : androidx.compose.foundation.gestures.TargetedFlingBehavior {
            override suspend fun androidx.compose.foundation.gestures.ScrollScope.performFling(
                initialVelocity: Float,
                onRemainingDistanceUpdated: (Float) -> Unit,
            ): Float {
                snapVelocities += initialVelocity
                onRemainingDistanceUpdated(0f)
                return 0f
            }
        }
        val effect = object : androidx.compose.foundation.OverscrollEffect {
            override val isInProgress: Boolean
                get() = reboundVisible && reboundVelocities.lastOrNull()?.let { it != 0f } == true
            override val node = object : androidx.compose.ui.Modifier.Node() { }
            override fun applyToScroll(
                delta: Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource,
                performScroll: (Offset) -> Offset,
            ): Offset = performScroll(delta)

            override suspend fun applyToFling(
                velocity: androidx.compose.ui.unit.Velocity,
                performFling: suspend (androidx.compose.ui.unit.Velocity) -> androidx.compose.ui.unit.Velocity,
            ) {
                reboundVelocities += (velocity - performFling(velocity)).x
            }
        }
        val scrollState = androidx.compose.foundation.gestures.ScrollableState { pixels ->
            pixels + boundaryRemainder
        }
        val scope = object : androidx.compose.foundation.gestures.ScrollScope {
            override fun scrollBy(pixels: Float): Float = error("Fling must stay inside its owning scroll state")
        }
        val behavior = com.melox.player.ui.component.HorizontalEdgeFlingBehavior(
            scrollState, decay, snap, effect, handoff, physicalDirection = -1f, canHandoff = true,
        )
        assertEquals(0f, with(behavior) { scope.performFling(1000f) }, 0f)
        assertEquals(listOf(-320f), reboundVelocities)
        assertTrue(snapVelocities.isEmpty())
        assertEquals(320f, with(behavior) { scope.performFling(1000f) }, 0f)
        assertEquals(listOf(-320f, 0f), reboundVelocities)
        decayRemaining = 0.8f
        boundaryRemainder = 0f
        assertEquals(0f, with(behavior) { scope.performFling(1000f) }, 0f)
        assertEquals(listOf(0f), snapVelocities)

        val freshHistory = com.melox.player.ui.component.HorizontalEdgeHandoff()
        val naturalStop = com.melox.player.ui.component.HorizontalEdgeFlingBehavior(
            scrollState, decay, snap, effect, freshHistory, physicalDirection = -1f, canHandoff = true,
        )
        assertEquals(0f, with(naturalStop) { scope.performFling(1000f) }, 0f)
        assertEquals(0, freshHistory.reachedEdges)
        assertEquals(listOf(0f, 0f), snapVelocities)
        assertFalse(freshHistory.shouldHandleDrag(12f, canHandoff = true))
        assertFalse(freshHistory.shouldHandleDrag(-12f, canHandoff = true))

        handoff.parentDragActive = true
        val reboundsBeforeHandoff = reboundVelocities.size
        assertEquals(1000f, with(behavior) { scope.performFling(1000f) }, 0f)
        assertEquals(reboundsBeforeHandoff, reboundVelocities.size)
        handoff.parentDragActive = false

        reboundVisible = false
        boundaryRemainder = -3f
        val clock = androidx.compose.runtime.BroadcastFrameClock()
        val invisibleImpact = async(clock) {
            with(naturalStop) { scope.performFling(1000f) }
        }
        repeat(5) { frame ->
            kotlinx.coroutines.yield()
            clock.sendFrame(frame * 16_000_000L)
            kotlinx.coroutines.yield()
        }
        kotlinx.coroutines.withTimeout(5000) { invisibleImpact.await() }
        assertEquals(0, freshHistory.reachedEdges)

    }

    @Test
    fun topBarUsesVisibleListScrollabilityAfterPagerSwitch() {
        val page = com.melox.player.ui.component.TopBarScrollContent()
        page.bounds = androidx.compose.ui.geometry.Rect(0f, 0f, 600f, 800f)
        val scrolled = com.melox.player.ui.component.ScrollContentPosition(
            androidx.compose.runtime.mutableStateOf({ 180f }),
            androidx.compose.runtime.mutableStateOf({ true }),
        )
        val atTop = com.melox.player.ui.component.ScrollContentPosition(
            androidx.compose.runtime.mutableStateOf({ 0f }),
            androidx.compose.runtime.mutableStateOf({ false }),
        )
        scrolled.bounds = page.bounds
        atTop.bounds = androidx.compose.ui.geometry.Rect(600f, 0f, 1200f, 800f)
        page.lists[scrolled] = scrolled
        page.lists[atTop] = atTop
        assertEquals(180f, page.distance)
        assertEquals(true, page.canScrollBackward)
        scrolled.bounds = androidx.compose.ui.geometry.Rect(-600f, 0f, 0f, 800f)
        atTop.bounds = page.bounds
        assertEquals(0f, page.distance)
        assertEquals(false, page.canScrollBackward)
    }

    @Test
    fun clearingSearchRestoresTheOriginalViewportAfterQueryRefinement() {
        val position = com.melox.player.ui.component.library.SearchScrollPosition()
        assertEquals(null, position.update("", 42, 17))
        assertEquals(null, position.update("a", 42, 17))
        assertEquals(null, position.update("ab", 3, 9))
        assertEquals(42 to 17, position.update("", 0, 0))
        assertEquals(null, position.update("", 42, 17))
        assertEquals(null, position.update("new", 8, 24))
        assertEquals(8 to 24, position.update("  ", 0, 0))
    }

    @Test
    fun rootPagerYieldsAllActiveRecommendationGestures() {
        assertFalse(rootPagerUserScrollEnabled(selectedPage = 0, homeRecommendationGestureActive = true))
        assertTrue(rootPagerUserScrollEnabled(selectedPage = 0, homeRecommendationGestureActive = false))
        assertTrue(rootPagerUserScrollEnabled(selectedPage = 1, homeRecommendationGestureActive = true))
        assertTrue(rootPagerUserScrollEnabled(selectedPage = 1, homeRecommendationGestureActive = false))
    }

    @Test
    fun hapticFeedbackDefaultsOnAndGatesEveryFeedbackTypeAtDispatchTime() {
        assertTrue(AppSettings().hapticFeedbackEnabled)
        var enabled = true
        val received = mutableListOf<HapticFeedbackType>()
        val delegate = object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                received += hapticFeedbackType
            }
        }
        val feedback = SettingsHapticFeedback(delegate) { enabled }
        val effects = listOf(
            HapticFeedbackType.ContextClick,
            HapticFeedbackType.LongPress,
            HapticFeedbackType.SegmentTick,
            HapticFeedbackType.TextHandleMove,
            HapticFeedbackType.GestureEnd,
            HapticFeedbackType.GestureThresholdActivate,
            HapticFeedbackType.ToggleOn,
            HapticFeedbackType.Confirm,
        )
        effects.forEach(feedback::performHapticFeedback)
        assertEquals(effects, received)
        enabled = false
        effects.forEach(feedback::performHapticFeedback)
        assertEquals(effects, received)
        enabled = true
        effects.forEach(feedback::performHapticFeedback)
        assertEquals(effects + effects, received)
    }

    @Test
    fun tabSwitchHapticOnlyFiresWhenEnabledAndTheTabChanges() {
        assertTrue(
            shouldTriggerTabSwitchHaptic(
                enabled = true,
                selectedPage = 0,
                targetPage = 1,
            ),
        )
        assertFalse(
            shouldTriggerTabSwitchHaptic(
                enabled = true,
                selectedPage = 1,
                targetPage = 1,
            ),
        )
        assertFalse(
            shouldTriggerTabSwitchHaptic(
                enabled = false,
                selectedPage = 0,
                targetPage = 1,
            ),
        )
    }

    @Test
    fun startupAppearanceDefaultsCanRenderNavigationImmediately() {
        val settings = AppSettings()

        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals(BottomBarStyle.GLASS, settings.bottomBarStyle)
        assertFalse(settings.dynamicColorEnabled)
        assertTrue(settings.showSongListArtwork)
        assertTrue(settings.showSongListDuration)
        assertFalse(settings.circularArtwork)
        assertFalse(settings.showSongListFileName)
        assertEquals(DynamicColorSource.PLAYBACK_ARTWORK, settings.dynamicColorSource)
        assertEquals(
            PlaybackBackgroundStyle.BLURRED_ARTWORK,
            settings.playbackBackgroundStyle,
        )
        assertEquals(400, settings.lyricFontWeight)
        assertFalse(settings.centerLyrics)
        assertFalse(settings.leftAlignPlayerTitle)
        assertTrue(settings.showLyricsTranslation)
        assertTrue(settings.showLyricsRomanization)
        assertEquals(com.melox.player.model.LyricsTranslationLanguage.AUTO, settings.lyricsTranslationLanguage)
        assertTrue(settings.showMiniPlayerLyrics)
        assertFalse(settings.showArtworkLyrics)
        assertEquals(LyricsSourcePriority.EMBEDDED, settings.lyricsSourcePriority)
        assertEquals(
            LyricsFormatPriority.LRC,
            settings.lyricsFormatPriority,
        )
        assertEquals(true, settings.blurEnabled)
        assertEquals(true, settings.predictiveBackEnabled)
        assertEquals(NavigationTransitionStyle.MIUIX, settings.navigationTransitionStyle)
        assertTrue(settings.songListPullToRefresh)
        assertFalse(settings.refreshLibraryOnStart)
        assertFalse(settings.skipShortAudio)
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
            BottomBarStyle.NORMAL,
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
                landscape = false,
                requestedBottomBarStyle = BottomBarStyle.FLOATING,
            ),
        )
        assertTrue(
            shouldShowNavigation(
                currentRouteIsRoot = true,
                landscape = true,
                requestedBottomBarStyle = BottomBarStyle.FLOATING,
            ),
        )
        assertFalse(
            shouldShowNavigation(
                currentRouteIsRoot = true,
                landscape = true,
                requestedBottomBarStyle = BottomBarStyle.FLOATING,
                renderedBottomBarStyle = BottomBarStyle.NORMAL,
            ),
        )
    }

    @Test
    fun glassNavigationRemainsAvailableOnSecondaryPages() {
        for (landscape in listOf(false, true)) {
            assertTrue(shouldShowNavigation(
                currentRouteIsRoot = false, landscape = landscape,
                requestedBottomBarStyle = BottomBarStyle.GLASS,
            ))
        }
        for (style in listOf(BottomBarStyle.NORMAL, BottomBarStyle.LIQUID_GLASS)) {
            assertFalse(shouldShowNavigation(
                currentRouteIsRoot = false, landscape = false,
                requestedBottomBarStyle = style,
            ))
            assertTrue(shouldShowNavigation(
                currentRouteIsRoot = true, landscape = false,
                requestedBottomBarStyle = style,
            ))
        }
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
            768.dp,
            floatingBottomBarAvailableWidth(
                windowWidth = 800.dp,
                windowHeight = 1200.dp,
                contentMaxWidth = 788.dp,
                portraitReferenceWidth = 800.dp,
            ),
        )
        assertEquals(
            551.dp,
            floatingBottomBarAvailableWidth(
                windowWidth = 599.dp,
                windowHeight = 400.dp,
                contentMaxWidth = 551.dp,
                portraitReferenceWidth = 400.dp,
            ),
        )
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
    fun landscapeFloatingBottomBarUsesOfficialClearanceOrSixteenDpFallback() {
        assertEquals(32.dp, floatingBottomBarBottomPadding(24.dp))
        assertEquals(12.dp, floatingBottomBarBottomPadding(4.dp))
        assertEquals(16.dp, floatingBottomBarBottomPadding(0.dp))
    }

    @Test
    fun portraitMiniPlayerStopsAtTheFloatingNavigationBarPosition() {
        assertEquals(
            16.dp,
            floatingMiniPlayerBottomPaddingWhenNavigationIsHidden(
                navigationBarBottomInset = 0.dp,
                isPortrait = true,
            ),
        )
        assertEquals(24.dp, floatingNavigationBarBottomPadding(0.dp))
        assertEquals(32.dp, floatingNavigationBarBottomPadding(24.dp))
        assertEquals(12.dp, floatingNavigationBarBottomPadding(4.dp))
        assertEquals(
            8.dp,
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
    fun playerOnlyFloatingSurfaceMatchesOfficialClearanceInBothOrientations() {
        listOf(true, false).forEach { isPortrait ->
            listOf(4.dp, 24.dp).forEach { inset ->
                val externalPadding = floatingMiniPlayerBottomPaddingWhenNavigationIsHidden(
                    navigationBarBottomInset = inset,
                    isPortrait = isPortrait,
                )
                assertEquals(inset + 8.dp, externalPadding + 8.dp)
            }
        }
    }

    @Test
    fun glassThemeRetainsAllThreeIndependentNavigationStyles() {
        assertEquals(BottomBarStyle.GLASS, AppSettings().bottomBarStyle)
        for (style in listOf(BottomBarStyle.NORMAL, BottomBarStyle.LIQUID_GLASS, BottomBarStyle.GLASS)) {
            assertEquals(style, AppSettings(selectedBottomBarStyle = style).bottomBarStyle)
        }
        assertEquals(
            BottomBarStyle.GLASS,
            com.melox.player.model.resolveStoredNavigationStyle("OS4", null, null),
        )
        assertEquals(
            BottomBarStyle.NORMAL,
            com.melox.player.model.resolveStoredNavigationStyle("OS3_FLOATING", null, null),
        )
        assertEquals(
            BottomBarStyle.NORMAL,
            resolveBottomBarStyle(BottomBarStyle.LIQUID_GLASS, liquidGlassSupported = false),
        )
    }
}
