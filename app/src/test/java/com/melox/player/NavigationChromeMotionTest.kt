package com.melox.player

import com.melox.player.ui.keepSidebarPageOnRootSelection
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.melox.player.ui.Os4NavigationSidebarMotion
import com.melox.player.ui.Os4NavigationReboundMotion
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
    fun sidebarOwnershipSwitchKeepsTheSamePageWhenReturningToItsUpperDestination() {
        assertTrue(keepSidebarPageOnRootSelection(shortcutSelected = true, currentPage = 2, targetPage = 2))
        assertTrue(keepSidebarPageOnRootSelection(shortcutSelected = true, currentPage = 0, targetPage = 0))
        org.junit.Assert.assertFalse(keepSidebarPageOnRootSelection(shortcutSelected = true, currentPage = 2, targetPage = 0))
        org.junit.Assert.assertFalse(keepSidebarPageOnRootSelection(shortcutSelected = false, currentPage = 2, targetPage = 2))
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
        val bottom = TargetBasedAnimation(Os4NavigationReboundMotion, Dp.VectorConverter, 40.dp, 740.dp)
        val bottomPositions = (0..bottom.durationNanos step 8_000_000L).map { bottom.getValueFromNanos(it).value }
        assertTrue("Only the expanding sidebar bottom should overshoot", bottomPositions.max() > 740f)
        assertEquals(740f, bottom.getValueFromNanos(bottom.durationNanos).value, 0.001f)
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
