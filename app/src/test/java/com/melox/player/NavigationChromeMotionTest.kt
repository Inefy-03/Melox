package com.melox.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.melox.player.data.library.AlbumGroup
import com.melox.player.data.library.ArtistGroup
import com.melox.player.ui.AppNavDestination
import com.melox.player.ui.AppRoute
import com.melox.player.ui.missingLibraryDetailCount
import com.melox.player.ui.rememberLibraryDetailForExit
import com.melox.player.model.NavigationTransitionStyle
import com.melox.player.ui.navigation.AospNavigationTransition
import com.melox.player.ui.navigation.secondaryPageEnterMotion
import com.melox.player.ui.navigation.ImmediateNavigationTransition
import com.melox.player.ui.navigationBarAnimationSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import top.yukonga.miuix.kmp.nav.transition.NavSettleSpec
import top.yukonga.miuix.kmp.nav.transition.NavTransitions

class NavigationChromeMotionTest {
    @Test
    fun upstreamBottomNavigationDragKeepsTheTrailAndSqueezesAtBothEnds() {
        val forward = top.yukonga.miuix.kmp.upstream.glass.navigationDragTarget(100f, 80f, 0f, 320f, 5f, false)
        assertEquals(80f, forward.left, 0f)
        assertEquals(180f, forward.right, 0f)
        assertEquals(top.yukonga.miuix.kmp.upstream.glass.GlassMotion.navDragFollow<Float>(), forward.leftSpring)
        val reverse = top.yukonga.miuix.kmp.upstream.glass.navigationDragTarget(100f, 80f, 0f, 320f, -5f, false)
        assertEquals(100f, reverse.left, 0f)
        assertEquals(200f, reverse.right, 0f)
        val capped = top.yukonga.miuix.kmp.upstream.glass.navigationDragTarget(100f, 80f, 0f, 320f, 100f, false)
        assertEquals(40f, capped.left, 0f)
        for ((position, delta) in listOf(-10f to -20f, 300f to 20f)) {
            val target = top.yukonga.miuix.kmp.upstream.glass.navigationDragTarget(position, 80f, 0f, 320f, delta, false)
            val bounds = top.yukonga.miuix.kmp.upstream.glass.navigationIndicatorBounds(target.left, target.right, 320f, 3f)
            assertEquals(77f, bounds.y - bounds.x, 0f)
            assertTrue(bounds.x >= 3f && bounds.y <= 317f)
        }
    }

    @Test
    fun landscapeNavigationKeepsItsOriginalDragTargets() {
        for (position in -100..400 step 10) {
            for (delta in listOf(-100f, -5f, 0f, 5f, 100f)) {
                for (changed in listOf(false, true)) {
                    val target = top.yukonga.miuix.kmp.glass.navigationDragTarget(position.toFloat(), 80f, 320f, delta, changed)
                    val start = position.toFloat().coerceIn(0f, 240f)
                    val trail = if (changed) 0f else (kotlin.math.abs(delta) * 4f).coerceAtMost(60f)
                    assertEquals(if (delta > 0f && start + 80f < 320f) start - trail else start, target.left, 0f)
                    assertEquals(if (delta < 0f && start > 0f) start + 80f + trail else start + 80f, target.right, 0f)
                }
            }
        }
    }

    @Test
    fun glassIndicatorStartsAtTheSuppliedDefaultDestinationThenAnimates() = kotlinx.coroutines.runBlocking {
        for (initialIndex in 0..3) {
            val motion = top.yukonga.miuix.kmp.glass.GlassNavigationIndicatorMotion()
            val targets = mutableListOf<Pair<Float, Float>>()
            val animate: (Float, Float) -> Unit = { left, right -> targets += left to right }
            org.junit.Assert.assertFalse(motion.useAnimatedBounds())
            val left = 3f + initialIndex * 80f
            val right = left + 90f
            motion.syncTarget(left, right, animate)
            assertEquals(left, motion.left.value, 0f)
            assertEquals(right, motion.right.value, 0f)
            assertTrue(motion.useAnimatedBounds())
            assertTrue(targets.isEmpty())
            motion.syncTarget(left + 10f, right + 10f, animate)
            assertEquals(left, motion.left.value, 0f)
            assertEquals(right, motion.right.value, 0f)
            assertEquals(listOf(left + 10f to right + 10f), targets)
        }
    }

    @Test
    fun glassIndicatorKeepsItsFirstDragAndReleaseOnAnimatedBounds() = kotlinx.coroutines.runBlocking {
        val motion = top.yukonga.miuix.kmp.glass.GlassNavigationIndicatorMotion()
        motion.syncTarget(3f, 83f) { _, _ -> error("Startup must snap") }
        assertTrue(motion.useAnimatedBounds())
        motion.left.snapTo(18f)
        motion.right.snapTo(98f)
        val targets = mutableListOf<Pair<Float, Float>>()
        motion.syncTarget(3f, 83f) { left, right -> targets += left to right }
        assertEquals(18f, motion.left.value, 0f)
        assertEquals(98f, motion.right.value, 0f)
        assertEquals(listOf(3f to 83f), targets)
        motion.syncTarget(83f, 163f) { left, right -> targets += left to right }
        assertEquals(18f, motion.left.value, 0f)
        assertTrue(motion.useAnimatedBounds())
    }

    @Test
    fun draggedGlassIndicatorWidthIsContinuousAcrossVariableAndEqualSlots() {
        val widths = listOf(70f, 96f, 58f)
        fun width(x: Float) = top.yukonga.miuix.kmp.glass.navigationDragWidth(x, widths)
        assertEquals(70f, width(-100f), 0f)
        assertEquals(70f, width(35f), 0f)
        assertEquals(96f, width(118f), 0f)
        assertEquals(58f, width(195f), 0f)
        assertEquals(58f, width(300f), 0f)
        for (boundary in listOf(70f, 166f)) {
            assertEquals(width(boundary - 0.001f), width(boundary + 0.001f), 0.002f)
        }
        for (position in -10..400) {
            assertEquals(80f, top.yukonga.miuix.kmp.glass.navigationDragWidth(position.toFloat(), listOf(80f, 80f, 80f)), 0f)
        }
    }

    @Test
    fun sidebarSelectionFollowsTheCurrentPageAndItsNearestListedAncestor() {
        val routes = listOf(
            com.melox.player.ui.AppRoute.PLAYLISTS,
            com.melox.player.ui.AppRoute.ALBUMS,
            com.melox.player.ui.AppRoute.ARTISTS,
            com.melox.player.ui.AppRoute.FOLDERS,
        )
        fun selected(vararg path: com.melox.player.ui.AppRoute) = com.melox.player.ui.selectedSidebarShortcut(
            path.mapIndexed { index, route -> com.melox.player.ui.AppNavDestination(route, depth = index) },
            routes,
        )
        org.junit.Assert.assertNull(selected(com.melox.player.ui.AppRoute.ROOT))
        routes.forEachIndexed { index, route ->
            assertEquals(index, selected(com.melox.player.ui.AppRoute.ROOT, route))
        }
        assertEquals(listOf(0, 2, 2, 2), routes.map { com.melox.player.ui.sidebarShortcutPrimaryPage(it) })
        org.junit.Assert.assertNull(com.melox.player.ui.sidebarShortcutPrimaryPage(null))
        assertEquals(1, selected(com.melox.player.ui.AppRoute.ROOT, com.melox.player.ui.AppRoute.ALBUMS, com.melox.player.ui.AppRoute.ALBUM_DETAIL))
        assertEquals(2, selected(com.melox.player.ui.AppRoute.ROOT, com.melox.player.ui.AppRoute.ARTISTS, com.melox.player.ui.AppRoute.ARTIST_DETAIL, com.melox.player.ui.AppRoute.ALBUM_DETAIL))
        org.junit.Assert.assertNull(selected(com.melox.player.ui.AppRoute.ROOT, com.melox.player.ui.AppRoute.ALBUM_DETAIL))
    }

    @Test
    fun navigationHostKeepsItsRememberedStateAcrossLayoutPositionsAndRouteCaptures() = kotlinx.coroutines.runBlocking {
        val secondary = androidx.compose.runtime.mutableStateOf(false)
        var capturedRoute = false
        var token: Any? = null
        var created = 0
        withArtworkComposition(content = {
            val route = secondary.value
            val retained = com.melox.player.ui.navigation.rememberNavigationHostContent {
                val remembered = androidx.compose.runtime.remember { created++; Any() }
                androidx.compose.runtime.SideEffect { token = remembered; capturedRoute = route }
            }
            if (route) retained() else retained()
        }) {
            frame(0)
            val initialToken = token
            secondary.value = true
            frame(16)
            org.junit.Assert.assertSame(initialToken, token)
            assertTrue(capturedRoute)
            secondary.value = false
            frame(32)
            org.junit.Assert.assertSame(initialToken, token)
            org.junit.Assert.assertFalse(capturedRoute)
            assertEquals(1, created)
        }
    }

    @Test
    fun routeReconciliationKeepsRetainedEntriesAndHonorsAnAlreadyCommittedPop() {
        val root = com.melox.player.ui.AppNavDestination(com.melox.player.ui.AppRoute.ROOT, depth = 0)
        val parent = com.melox.player.ui.AppNavDestination(com.melox.player.ui.AppRoute.ALBUMS, depth = 1)
        val detail = com.melox.player.ui.AppNavDestination(com.melox.player.ui.AppRoute.ALBUM_DETAIL, "album", 2)
        val stack = top.yukonga.miuix.kmp.nav.core.navBackStackOf(root, parent)
        com.melox.player.ui.synchronizeAppNavBackStack(stack, listOf(root.copy(), parent.copy(), detail))
        org.junit.Assert.assertSame(root, stack[0])
        org.junit.Assert.assertSame(parent, stack[1])
        assertEquals(detail, stack.last())

        // The completion callback pops before Miuix releases the gesture's driver ownership.
        stack.removeAt(stack.lastIndex)
        com.melox.player.ui.synchronizeAppNavBackStack(stack, listOf(root.copy(), parent.copy()))
        assertEquals(listOf(root, parent), stack.toList())
        org.junit.Assert.assertSame(parent, stack.last())
        com.melox.player.ui.synchronizeAppNavBackStack(stack, listOf(root.copy()))
        org.junit.Assert.assertSame(root, stack.single())
    }

    @Test
    fun deletingTheFinalTrackSkipsOnlyMissingDetailsAtTheTopOfTheStack() {
        val root = AppNavDestination(AppRoute.ROOT, depth = 0)
        val library = AppNavDestination(AppRoute.ARTISTS, depth = 1)
        val artist = AppNavDestination(AppRoute.ARTIST_DETAIL, "artist", depth = 2)
        val album = AppNavDestination(AppRoute.ALBUM_DETAIL, "album", depth = 3)
        val destinations = listOf(root, library, artist, album)
        val albums = listOf(AlbumGroup("album", null, null, null, emptyList()))
        val artists = listOf(ArtistGroup("artist", null, emptyList(), albumCount = 1))

        assertEquals(0, destinations.missingLibraryDetailCount(albums, artists))
        assertEquals(1, destinations.missingLibraryDetailCount(emptyList(), artists))
        assertEquals(2, destinations.missingLibraryDetailCount(emptyList(), emptyList()))
        assertEquals(1, destinations.dropLast(1).missingLibraryDetailCount(albums, emptyList()))
        assertEquals(0, destinations.missingLibraryDetailCount(albums, emptyList()))
    }

    @Test
    fun aMissingDetailKeepsItsLastContentUntilItsEntryChanges() = kotlinx.coroutines.runBlocking {
        val contentKey = mutableStateOf("album")
        val detail = mutableStateOf<String?>("initial")
        var rendered: String? = null
        withArtworkComposition(content = {
            val content = rememberLibraryDetailForExit(contentKey.value, detail.value)
            SideEffect { rendered = content }
        }) {
            frame(0)
            assertEquals("initial", rendered)
            detail.value = "updated"
            frame(16)
            detail.value = null
            frame(32)
            assertEquals("updated", rendered)
            contentKey.value = "another-album"
            frame(48)
            assertEquals(null, rendered)
        }
    }

    @Test
    fun inlineTitleAppearsOnlyWhenTheNormalLandscapeTitleIsHidden() {
        fun inline(alpha: Float, progress: Float?, enabled: Boolean = true, title: String = "Albums") =
            com.melox.player.ui.component.shouldShowInlineTopBarTitle(title, alpha, progress, enabled)
        assertTrue(!inline(1f, 1f))
        assertTrue(!inline(1f, 0.5f))
        assertTrue(inline(1f, 0f))
        assertTrue(inline(0f, 1f))
        assertTrue(!inline(1f, null))
        assertTrue(!inline(1f, 0f, enabled = false))
        assertTrue(!inline(1f, 0f, title = ""))
    }

    @Test
    fun measuredNavigationSlotsKeepHitTestingAndSpringEdgesAligned() {
        val widths = listOf(70f, 96f, 58f)
        val edges = listOf(0f, 70f, 166f, 224f)
        edges.forEachIndexed { slot, position ->
            assertEquals(position, top.yukonga.miuix.kmp.glass.navigationSlotPosition(slot.toFloat(), widths), 0.001f)
            assertEquals(slot.toFloat(), top.yukonga.miuix.kmp.glass.navigationSlotAt(position, widths), 0.001f)
        }
        assertEquals(118f, top.yukonga.miuix.kmp.glass.navigationSlotPosition(1.5f, widths), 0.001f)
        assertEquals(1.5f, top.yukonga.miuix.kmp.glass.navigationSlotAt(118f, widths), 0.001f)
        // Full indicator widths become adjacent top slots, retaining both former 5 px wings.
        val fullWidths = widths.map { it + 10f }
        val firstRight = top.yukonga.miuix.kmp.glass.navigationSlotPosition(1f, fullWidths)
        val secondRight = top.yukonga.miuix.kmp.glass.navigationSlotPosition(2f, fullWidths)
        assertEquals(80f, firstRight, 0.001f)
        assertEquals(106f, secondRight - firstRight, 0.001f)
        val bounds = top.yukonga.miuix.kmp.glass.navigationIndicatorBounds(240f, 340f, 300f, 43f, 3f)
        assertEquals(240f, bounds.x, 0.001f)
        assertEquals(297f, bounds.y, 0.001f)
        val symmetric = top.yukonga.miuix.kmp.glass.navigationIndicatorBounds(240f, 340f, 300f, 3f)
        assertEquals(297f, symmetric.y, 0.001f)
    }

    @Test
    fun sidebarDestinationsSwitchImmediately() {
        val motion = ImmediateNavigationTransition.motion.programmatic as NavSettleSpec.Tween
        assertEquals(0, motion.durationMillis)
        assertEquals(top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection.None, ImmediateNavigationTransition.dismissDirection)
    }

    @Test
    fun portraitCategoryEntriesUseTheOfficialMiuixTransition() {
        assertEquals(NavTransitions.MiuixDefault, com.melox.player.ui.navigation.secondaryPageTransition(NavigationTransitionStyle.MIUIX))
        assertTrue(com.melox.player.ui.shouldUseImmediateSidebarNavigation(1200.dp, 840.dp, com.melox.player.model.BottomBarStyle.GLASS))
        assertTrue(!com.melox.player.ui.shouldUseImmediateSidebarNavigation(840.dp, 1200.dp, com.melox.player.model.BottomBarStyle.GLASS))
        assertTrue(!com.melox.player.ui.shouldUseImmediateSidebarNavigation(1200.dp, 840.dp, com.melox.player.model.BottomBarStyle.NORMAL))
    }

    @Test
    fun miuixChromeTracksOfficialPageTravelThroughoutEntry() {
        assertPageTravel(NavigationTransitionStyle.MIUIX, NavTransitions.MiuixDefault.motion.programmatic)
    }

    @Test
    fun aospChromeTracksPageTravelIncludingTheTwoSegmentCurve() {
        assertPageTravel(NavigationTransitionStyle.AOSP, AospNavigationTransition.motion.programmatic)
    }

    @Test
    fun rootSettingHideAndReturnKeepTheirExistingMotion() {
        val hide = navigationBarAnimationSpec<Float>(showNavigation = false, hideMotion = null)
        val show = navigationBarAnimationSpec<Float>(
            showNavigation = true,
            hideMotion = secondaryPageEnterMotion(NavigationTransitionStyle.AOSP),
        )
        assertEquals(240, hide.durationMillis)
        assertEquals(280, show.durationMillis)
        assertEquals(FastOutSlowInEasing, hide.easing)
        assertEquals(FastOutSlowInEasing, show.easing)
    }

    private fun assertPageTravel(style: NavigationTransitionStyle, pageMotion: NavSettleSpec) {
        val motion = pageMotion as NavSettleSpec.Tween
        val spec = navigationBarAnimationSpec<Float>(
            showNavigation = false,
            hideMotion = secondaryPageEnterMotion(style),
        )
        val navigationSpace = TargetBasedAnimation(spec, Float.VectorConverter, 98f, 0f)
        val playerGap = TargetBasedAnimation(spec, Float.VectorConverter, 0f, 18f)
        val pageTravel = TargetBasedAnimation(
            androidx.compose.animation.core.tween<Float>(motion.durationMillis, easing = motion.easing),
            Float.VectorConverter,
            0f,
            1f,
        )
        assertEquals(motion.durationMillis * 1_000_000L, navigationSpace.durationNanos)
        assertTrue(spec.easing !== FastOutSlowInEasing)
        for (millis in (0..motion.durationMillis step 16) + motion.durationMillis) {
            val nanos = millis * 1_000_000L
            val fraction = pageTravel.getValueFromNanos(nanos)
            val chromeTravel = 1f - navigationSpace.getValueFromNanos(nanos) / 98f
            val gapTravel = playerGap.getValueFromNanos(nanos) / 18f
            assertEquals("Navigation at $millis ms", fraction, chromeTravel, 0.00001f)
            assertEquals("Player at $millis ms", fraction, gapTravel, 0.00001f)
        }
    }
}
