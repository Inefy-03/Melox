package com.melox.player.ui.screen.playback

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.melox.player.R
import kotlin.math.roundToInt

// The track exports geometry after measuring the controls once.
private val CompactLandscapeViewportHeight = HorizontalAlignmentLine { first, second ->
    maxOf(first, second)
}
private val CompactLandscapeHeaderTop = HorizontalAlignmentLine(::minOf)

@Composable
internal fun CompactLandscapePlayerLayout(
    pagerState: PagerState,
    lyricsPagingEnabled: Boolean,
    immersiveMode: Boolean,
    chromeVerticalInset: Dp,
    artworkContent: @Composable (Dp) -> Unit,
    headerContent: @Composable (contentWidth: Dp) -> Unit,
    lyricsToggleContent: @Composable (showLyrics: Boolean, enabled: Boolean, onToggle: () -> Unit) -> Unit,
    lyricsContent: @Composable (paneWidth: Dp, contentWidth: Dp, contentHeight: Dp, active: Boolean) -> Unit,
    controlsContent: @Composable (paneWidth: Dp, contentWidth: Dp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showLyrics = pagerState.currentPage == 1
    val toggleLyrics = {
        pagerState.requestScrollToPage(if (pagerState.currentPage == 0) 1 else 0)
    }
    val switchLabel = stringResource(
        if (showLyrics) R.string.player_show_controls else R.string.player_show_lyrics,
    )
    val transition = updateTransition(showLyrics, label = "compactLandscapePage")
    val lyricsProgress = transition.animateFloat(
        transitionSpec = { tween(300, easing = FastOutSlowInEasing) },
        label = "compactLandscapeLyricsProgress",
    ) { if (it) 1f else 0f }
    val lyricsActive = transition.currentState || transition.targetState
    BoxWithConstraints(modifier = modifier) {
        val artworkPaneWidth = compactLandscapeArtworkPaneWidth(maxWidth)
        val controlsPaneWidth = maxWidth - artworkPaneWidth
        val chromeHeight = (maxHeight - chromeVerticalInset * 2).coerceAtLeast(0.dp)
        val artworkSize = compactLandscapeArtworkSize(artworkPaneWidth, chromeHeight)
        val contentWidth = compactLandscapePaneContentWidth(controlsPaneWidth)
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.weight(COMPACT_LANDSCAPE_ARTWORK_WEIGHT).fillMaxHeight()
                    .wrapContentHeight(unbounded = true),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = if (immersiveMode) Modifier.clickable(
                        enabled = lyricsPagingEnabled,
                        interactionSource = null,
                        indication = null,
                        role = Role.Button,
                        onClickLabel = switchLabel,
                        onClick = toggleLyrics,
                    ) else Modifier,
                ) {
                    artworkContent(artworkSize)
                }
            }
            SubcomposeLayout(Modifier.weight(1f - COMPACT_LANDSCAPE_ARTWORK_WEIGHT)
                .fillMaxHeight()) { constraints ->
                val paneWidth = constraints.maxWidth
                val paneHeight = constraints.maxHeight
                val chromeInset = chromeVerticalInset.roundToPx()
                val chromeBottom = paneHeight - chromeInset
                val naturalConstraints = Constraints(maxWidth = paneWidth)
                val extensionPx = COMPACT_LANDSCAPE_LYRICS_EXTENSION.roundToPx()
                val toggle = subcompose("lyricsToggle") {
                    Box(Modifier.playerChromeVisibility(!immersiveMode)) {
                        lyricsToggleContent(showLyrics, lyricsPagingEnabled && !immersiveMode, toggleLyrics)
                    }
                }.single().measure(naturalConstraints)
                val toggleStart = compactLandscapeLyricsToggleStart(
                    paneWidth.toDp(), toggle.width.toDp(),
                ).roundToPx()
                val contentStart = ((paneWidth - contentWidth.roundToPx()) / 2).coerceAtLeast(0)
                val headerWidth = (toggleStart - contentStart - 8.dp.roundToPx()).coerceAtLeast(0)
                val header = subcompose("header") {
                    Box(contentAlignment = Alignment.Center) { headerContent(headerWidth.toDp()) }
                }.single().measure(Constraints(maxWidth = headerWidth))
                val headerHeight = maxOf(header.height, toggle.height)
                val page = subcompose("page") {
                    Layout(
                        modifier = Modifier.clipToBounds()
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithCache {
                                val (topFadeDp, bottomFadeDp) = lyricEdgeFadeHeights(true)
                                val topFade = (topFadeDp.dp.toPx() / size.height.coerceAtLeast(1f))
                                    .coerceAtMost(0.5f)
                                val bottomFade = (bottomFadeDp.dp.toPx() / size.height.coerceAtLeast(1f))
                                    .coerceAtMost(0.5f)
                                onDrawWithContent {
                                    drawContent()
                                    val progress = lyricsProgress.value
                                    // Fade at both viewport edges while the page moves beneath the header.
                                    val strength = 4f * progress * (1f - progress)
                                    if (strength > 0f) {
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                0f to Color.Black.copy(alpha = 1f - strength),
                                                topFade to Color.Black,
                                                1f - bottomFade to Color.Black,
                                                1f to Color.Black.copy(alpha = 1f - strength),
                                            ),
                                            blendMode = BlendMode.DstIn,
                                        )
                                    }
                                }
                            },
                        content = {
                            SubcomposeLayout { trackConstraints ->
                                val controls = subcompose("controls") {
                                    Box(Modifier.compactLandscapePaneInput(!showLyrics)) {
                                        controlsContent(controlsPaneWidth, contentWidth)
                                    }
                                }.single().measure(Constraints(maxWidth = paneWidth))
                                val headerTop = chromeInset + compactLandscapeHeaderTop(
                                    chromeHeight, headerHeight.toDp(), controls.height.toDp(),
                                ).roundToPx()
                                val headerBottom = headerTop + headerHeight
                                val viewportHeight = if (immersiveMode) paneHeight else {
                                    (chromeBottom - headerBottom).coerceAtLeast(0) + extensionPx * 2
                                }
                                val lyrics = subcompose("lyrics") {
                                    Box(
                                        modifier = Modifier.compactLandscapePaneInput(showLyrics),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        lyricsContent(controlsPaneWidth, contentWidth,
                                            viewportHeight.toDp(), lyricsActive)
                                    }
                                }.single().measure(Constraints.fixed(paneWidth, viewportHeight))
                                // At progress zero, the viewport origin and track offset cancel out this gap.
                                val controlsTop = viewportHeight +
                                    COMPACT_LANDSCAPE_HEADER_TO_CONTROLS_SPACING.roundToPx() +
                                    if (immersiveMode) headerBottom else 0
                                layout(
                                    trackConstraints.maxWidth,
                                    controlsTop + controls.height,
                                    mapOf(
                                        CompactLandscapeViewportHeight to viewportHeight,
                                        CompactLandscapeHeaderTop to headerTop,
                                    ),
                                ) {
                                    lyrics.placeRelative(0, 0)
                                    controls.placeRelative(0, controlsTop)
                                }
                            }
                        },
                    ) { measurables, viewportConstraints ->
                        val track = measurables.single().measure(Constraints(maxWidth = paneWidth))
                        val viewportHeight = track[CompactLandscapeViewportHeight]
                        val visibleHeight = if (immersiveMode) viewportHeight else {
                            (viewportHeight - extensionPx * (1f - lyricsProgress.value))
                                .roundToInt().coerceAtLeast(0)
                        }
                        layout(viewportConstraints.maxWidth, visibleHeight,
                            mapOf(CompactLandscapeHeaderTop to track[CompactLandscapeHeaderTop])) {
                            // Lyrics and controls share one physical track and one translation.
                            track.placeRelativeWithLayer(0, -viewportHeight) {
                                translationY = lyricsProgress.value * viewportHeight
                            }
                        }
                    }
                }.single().measure(naturalConstraints)
                val headerTop = page[CompactLandscapeHeaderTop]
                layout(paneWidth, paneHeight) {
                    header.placeRelativeWithLayer(contentStart, headerTop + (headerHeight - header.height) / 2) {
                        translationY = -extensionPx * lyricsProgress.value
                    }
                    toggle.placeRelativeWithLayer(toggleStart, headerTop + (headerHeight - toggle.height) / 2) {
                        translationY = -extensionPx * lyricsProgress.value
                    }
                    if (immersiveMode) {
                        page.placeRelative(0, 0)
                    } else {
                        page.placeRelativeWithLayer(0, headerTop + headerHeight) {
                            translationY = -extensionPx * lyricsProgress.value
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.compactLandscapePaneInput(enabled: Boolean): Modifier = if (enabled) this else {
    clearAndSetSemantics {}.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
            }
        }
    }
}
