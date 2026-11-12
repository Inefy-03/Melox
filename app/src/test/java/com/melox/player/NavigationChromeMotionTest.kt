package com.melox.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.melox.player.ui.Os4NavigationSidebarMotion
import com.melox.player.ui.Os4NavigationReboundMotion
import com.melox.player.ui.os4NavigationItemMotion
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
    fun glassIndicatorKeepsEveryStartupMeasurementAtItsDefaultDestination() = kotlinx.coroutines.runBlocking {
        for (initialIndex in 0..2) {
            val motion = top.yukonga.miuix.kmp.glass.GlassNavigationIndicatorMotion(initialIndex)
            val animatedTargets = mutableListOf<Pair<Float, Float>>()
            val animate: (Float, Float) -> Unit = { left, right -> animatedTargets += left to right }
            org.junit.Assert.assertFalse(motion.useAnimatedBounds(initialIndex))
            for (width in listOf(20f, 48f, 360f, 284f)) {
                val slotWidth = (width - 16f) / 4f
                val left = 3f + initialIndex * slotWidth
                val right = 13f + (initialIndex + 1) * slotWidth
                motion.syncTarget(initialIndex, left, right, animate)
                assertEquals(left, motion.left.value, 0f)
                assertEquals(right, motion.right.value, 0f)
                org.junit.Assert.assertFalse(motion.useAnimatedBounds(initialIndex))
                assertTrue(animatedTargets.isEmpty())
            }
            val nextIndex = initialIndex + 1
            assertTrue(motion.useAnimatedBounds(nextIndex))
            motion.syncTarget(nextIndex, 200f, 280f, animate)
            motion.syncTarget(nextIndex, 180f, 260f, animate)
            motion.syncTarget(initialIndex, 3f, 83f, animate)
            assertEquals(listOf(200f to 280f, 180f to 260f, 3f to 83f), animatedTargets)
            assertTrue(motion.useAnimatedBounds(initialIndex))
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
        assertEquals(listOf(0, 2, 2, 2), routes.map { com.melox.player.ui.sidebarShortcutPrimaryPage(it) })
        org.junit.Assert.assertNull(com.melox.player.ui.sidebarShortcutPrimaryPage(null))
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
    fun inlineTitleAppearsOnlyWhenTheNormalLandscapeTitleIsHidden() {
        fun inline(alpha: Float, progress: Float?, enabled: Boolean = true, title: String = "Albums") =
            com.melox.player.ui.component.os4.shouldShowInlineTopBarTitle(title, alpha, progress, enabled)
        assertTrue(!inline(1f, 1f))
        assertTrue(!inline(1f, 0.5f))
        assertTrue(inline(1f, 0f))
        assertTrue(inline(0f, 1f))
        assertTrue(!inline(1f, null))
        assertTrue(!inline(1f, 0f, enabled = false))
        assertTrue(!inline(1f, 0f, title = ""))
    }

    @Test
    fun landscapeCapsuleReboundsOnlyWhenCollapsingSidebar() {
        assertEquals(200f, Os4NavigationReboundMotion.stiffness, 0f)
        assertEquals(0.8f, Os4NavigationReboundMotion.dampingRatio, 0f)
        val motion = TargetBasedAnimation(Os4NavigationReboundMotion, Dp.VectorConverter, 12.dp, 400.dp)
        val positions = (0..motion.durationNanos step 8_000_000L).map { motion.getValueFromNanos(it).value }
        val peak = positions.max()
        assertTrue("Horizontal settle should overshoot to the right", peak > 400.2f)
        assertTrue("The reference rebound should stay subtle", peak < 407f)
        val peakMillis = positions.indices.maxBy { positions[it] } * 8
        assertTrue("Reference reaches its peak about 400 ms after travel begins", peakMillis in 350..450)
        assertTrue("Return should continue beyond the size morph", motion.durationNanos > 600_000_000L)
        assertEquals(400f, motion.getValueFromNanos(motion.durationNanos).value, 0.001f)
        val expansion = TargetBasedAnimation(Os4NavigationSidebarMotion, Dp.VectorConverter, 400.dp, 12.dp)
        val expansionPositions = (0..300 step 4).map { expansion.getValueFromNanos(it * 1_000_000L).value }
        assertTrue(expansionPositions.all { it in 12f..400f })
        assertTrue(expansionPositions.zipWithNext().all { (before, after) -> after <= before })
        assertEquals(12f, expansionPositions.last(), 0.001f)
        assertEquals(0.696f, com.melox.player.ui.os4SidebarHeightMotion(true, 696.dp).visibilityThreshold!!.value, 0.0001f)
        val bottom = TargetBasedAnimation(com.melox.player.ui.os4SidebarHeightMotion(true, 696.dp), Dp.VectorConverter, 44.dp, 740.dp)
        val bottomPositions = (0..bottom.durationNanos step 8_000_000L).map { bottom.getValueFromNanos(it).value }
        assertTrue("Only the expanding sidebar bottom should overshoot", bottomPositions.max() > 740f)
        assertEquals(740f, bottom.getValueFromNanos(bottom.durationNanos).value, 0.001f)
    }

    @Test
    fun measuredNavigationSlotsKeepHitTestingAndSpringEdgesAligned() {
        val widths = listOf(70f, 96f, 58f)
        val edges = listOf(0f, 70f, 166f, 224f)
    @Test
    fun landscapeCascadeKeepsBothDirectionsOrderedAndRetargetable() {
        val entering = List(7) { index ->
            TargetBasedAnimation(os4NavigationItemMotion(index, expanded = true), Float.VectorConverter, 0f, 1f)
        }
        val leaving = List(7) { index ->
            TargetBasedAnimation(os4NavigationItemMotion(index, expanded = false), Float.VectorConverter, 1f, 0f)
        }
        assertTrue(entering.zipWithNext().all { (first, next) -> first.durationNanos < next.durationNanos })
        for (timeMillis in 16..280 step 8) {
            val time = timeMillis * 1_000_000L
            val positions = entering.map { it.getValueFromNanos(time) }
            assertTrue(positions.zipWithNext().all { (first, next) -> first > next })
            val reversePositions = leaving.map { it.getValueFromNanos(time) }
            assertTrue(reversePositions.zipWithNext().all { (first, next) -> first < next })
        }
        entering.indices.forEach { index ->
            assertTrue(leaving[index].durationNanos > entering[index].durationNanos)
            val interrupted = TargetBasedAnimation(os4NavigationItemMotion(index, expanded = false), Float.VectorConverter, entering[index].getValueFromNanos(100_000_000L), 0f)
            assertEquals(entering[index].getValueFromNanos(100_000_000L), interrupted.getValueFromNanos(0L), 0f)
        }
        entering.forEach { motion -> assertEquals(1f, motion.getValueFromNanos(motion.durationNanos), 0f) }
        leaving.forEach { motion -> assertEquals(0f, motion.getValueFromNanos(motion.durationNanos), 0f) }
    }

    @Test
    fun navigationIconsAndShortcutLabelsHaveIndependentFadePhases() {
        fun animation(spec: androidx.compose.animation.core.TweenSpec<Float>, entering: Boolean) =
            TargetBasedAnimation(spec, Float.VectorConverter, if (entering) 0f else 1f, if (entering) 1f else 0f)
        val iconIn = animation(com.melox.player.ui.Os4NavigationIconEnterMotion, true)
        val labelIn = animation(com.melox.player.ui.Os4NavigationLabelEnterMotion, true)
        val iconOut = animation(com.melox.player.ui.Os4NavigationIconExitMotion, false)
        val labelOut = animation(com.melox.player.ui.Os4NavigationLabelExitMotion, false)
        assertTrue(iconIn.getValueFromNanos(64_000_000L) > 0f)
        assertEquals(0f, labelIn.getValueFromNanos(64_000_000L), 0f)
        assertTrue(iconOut.getValueFromNanos(64_000_000L) < 0.5f)
        assertEquals(1f, labelOut.getValueFromNanos(64_000_000L), 0f)
        assertEquals(0f, iconOut.getValueFromNanos(120_000_000L), 0f)
        assertTrue(labelOut.getValueFromNanos(120_000_000L) > 0f)
        assertEquals(0f, labelOut.getValueFromNanos(labelOut.durationNanos), 0f)
    }

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
        assertTrue("The rebound must fit inside the reference bottom clearance", bottomPositions.max() < 748f)
        val bottomPeakMillis = bottomPositions.indices.maxBy { bottomPositions[it] } * 8
        assertTrue(bottomPeakMillis in 350..420)
        val heightExit = TargetBasedAnimation(com.melox.player.ui.os4SidebarHeightMotion(false, 696.dp), Dp.VectorConverter, 740.dp, 44.dp)
        assertTrue(heightExit.getValueFromNanos(100_000_000L).value < 320f)
        assertEquals(44f, heightExit.getValueFromNanos(heightExit.durationNanos).value, 0.001f)
        val motion = ImmediateNavigationTransition.motion.programmatic as NavSettleSpec.Tween
        assertEquals(0, motion.durationMillis)
        assertEquals(top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection.None, ImmediateNavigationTransition.dismissDirection)
    }

    @Test
    fun portraitCategoryEntriesUseTheOfficialMiuixTransition() {
        assertEquals(NavTransitions.MiuixDefault, com.melox.player.ui.navigation.secondaryPageTransition(NavigationTransitionStyle.MIUIX))
        assertTrue(com.melox.player.ui.shouldUseImmediateOs4Navigation(1200.dp, 840.dp, com.melox.player.model.BottomBarStyle.OS4))
        assertTrue(!com.melox.player.ui.shouldUseImmediateOs4Navigation(840.dp, 1200.dp, com.melox.player.model.BottomBarStyle.OS4))
        assertTrue(!com.melox.player.ui.shouldUseImmediateOs4Navigation(1200.dp, 840.dp, com.melox.player.model.BottomBarStyle.NORMAL))
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
