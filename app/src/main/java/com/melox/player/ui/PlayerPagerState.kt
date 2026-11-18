// Adapted from compose-miuix-ui's example application.
// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.melox.player.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

// Shared by tab navigation and gesture settling, matching upstream PagerNavigationSpringSpec.
internal val PlayerPagerNavigationSpringSpec = spring<Float>(
    stiffness = 322.2f,
    dampingRatio = 32.31f / (2f * kotlin.math.sqrt(322.2f)),
    visibilityThreshold = 0.5f,
)

/**
 * Keeps tab selection responsive while the underlying Pager performs the Miuix-style scroll.
 *
 * The selected tab changes at tap time, while [syncPage] reconciles selections made by a swipe.
 */
@Stable
internal class PlayerPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope,
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    var isNavigating by mutableStateOf(false)
        private set

    private var navigationJob: Job? = null

    fun snapToPage(targetIndex: Int) {
        if (targetIndex !in 0 until pagerState.pageCount) return
        navigationJob?.cancel()
        navigationJob = null
        selectedPage = targetIndex
        isNavigating = false
        pagerState.requestScrollToPage(targetIndex)
    }

    fun animateToPage(targetIndex: Int) {
        if (targetIndex == selectedPage || targetIndex !in 0 until pagerState.pageCount) return

        // A newer tab tap supersedes an unfinished animation and becomes the sole source of truth.
        navigationJob?.cancel()
        selectedPage = targetIndex
        isNavigating = true

        navigationJob = coroutineScope.launch {
            val currentJob = coroutineContext.job
            try {
                with(pagerState) {
                    scroll(MutatePriority.UserInput) {
                        updateTargetPage(targetIndex)
                        val pageSize = pagerState.layoutInfo.pageSize + pagerState.layoutInfo.pageSpacing
                        val distanceInPages =
                            targetIndex - pagerState.currentPage - pagerState.currentPageOffsetFraction
                        val scrollPixels = distanceInPages * pageSize

                        var previousValue = 0f
                        animate(
                            initialValue = 0f,
                            targetValue = scrollPixels,
                            animationSpec = PlayerPagerNavigationSpringSpec,
                        ) { currentValue, _ ->
                            previousValue += scrollBy(currentValue - previousValue)
                        }
                        updateCurrentPage(targetIndex)
                    }
                }
            } finally {
                if (navigationJob == currentJob) {
                    isNavigating = false
                    if (pagerState.currentPage != targetIndex) {
                        selectedPage = pagerState.currentPage
                    }
                }
            }
        }
    }

    fun syncPage(currentPage: Int) {
        // Do not overwrite the eager tab selection until a programmatic scroll has settled.
        if (!isNavigating && selectedPage != currentPage) {
            selectedPage = currentPage
        }
    }
}

@Composable
internal fun rememberPlayerPagerState(
    pagerState: PagerState,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): PlayerPagerState = remember(pagerState, coroutineScope) {
    PlayerPagerState(pagerState, coroutineScope)
}
