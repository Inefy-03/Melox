package com.melox.player.ui.component.library

// Interaction pattern adapted from Replica0110/Lyrico (Apache-2.0).

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val AlphabetSections =
    listOf("0") + ('A'..'Z').map(Char::toString) + listOf("#")

private sealed interface AlphabetSideBarItem {
    data object ScrollTop : AlphabetSideBarItem
    data class Section(val value: String) : AlphabetSideBarItem
}

@Composable
fun AlphabetSideBar(
    sectionIndexMap: Map<String, Int>,
    itemCount: Int,
    scrollStateKey: Any,
    isAtTarget: (Int) -> Boolean,
    scrollToItem: suspend (Int) -> Unit,
    modifier: Modifier = Modifier,
    sections: List<String> = AlphabetSections,
    showScrollTop: Boolean = true,
    onTargetIndexChanged: (targetIndex: Int, restoreLargeTitle: Boolean) -> Unit,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val scope = rememberCoroutineScope()
    val items = remember(sections) {
        listOf<AlphabetSideBarItem>(AlphabetSideBarItem.ScrollTop) +
            sections.map(AlphabetSideBarItem::Section)
    }
    var scrollJob by remember { mutableStateOf<Job?>(null) }
    var selectedItem by remember { mutableStateOf<AlphabetSideBarItem?>(null) }
    var indicatorItem by remember { mutableStateOf<AlphabetSideBarItem?>(null) }
    var indicatorVisible by remember { mutableStateOf(false) }
    var lastSelectedIndex by remember { mutableIntStateOf(-1) }
    val currentItemCount by rememberUpdatedState(itemCount)
    val currentSectionIndexMap by rememberUpdatedState(sectionIndexMap)
    val currentSections by rememberUpdatedState(sections)
    val currentShowScrollTop by rememberUpdatedState(showScrollTop)
    val currentIsAtTarget by rememberUpdatedState(isAtTarget)
    val currentScrollToItem by rememberUpdatedState(scrollToItem)
    val currentOnTargetIndexChanged by rememberUpdatedState(onTargetIndexChanged)

    LaunchedEffect(sectionIndexMap, itemCount, scrollStateKey) {
        scrollJob?.cancel()
        scrollJob = null
        selectedItem = null
        indicatorVisible = false
        lastSelectedIndex = -1
    }

    fun targetIndex(item: AlphabetSideBarItem): Int = when (item) {
        AlphabetSideBarItem.ScrollTop -> 0
        is AlphabetSideBarItem.Section -> findAlphabetTargetIndex(
            section = item.value,
            sectionIndexMap = currentSectionIndexMap,
            sections = currentSections,
        )
    }

    fun updateSelection(
        index: Int,
        force: Boolean = false,
    ) {
        if (index !in items.indices || (!force && index == lastSelectedIndex)) return
        val item = items[index]
        if (item == AlphabetSideBarItem.ScrollTop && !currentShowScrollTop) return
        lastSelectedIndex = index
        selectedItem = item
        indicatorItem = item
        indicatorVisible = true

        val maxIndex = currentItemCount - 1
        if (maxIndex < 0) return
        val targetIndex = targetIndex(item).coerceIn(0, maxIndex)
        currentOnTargetIndexChanged(
            targetIndex,
            item == AlphabetSideBarItem.ScrollTop,
        )
        if (!currentIsAtTarget(targetIndex)) {
            scrollJob?.cancel()
            scrollJob = scope.launch {
                try {
                    val latestMaxIndex = currentItemCount - 1
                    if (latestMaxIndex >= 0) {
                        currentScrollToItem(targetIndex.coerceAtMost(latestMaxIndex))
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: IndexOutOfBoundsException) {
                    // A sort can replace the lazy layout between validation and scrolling.
                } catch (_: IllegalArgumentException) {
                    // Lazy layouts reject an index from a concurrently replaced item provider.
                }
            }
        }
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun clearSelection() {
        selectedItem = null
        indicatorVisible = false
        lastSelectedIndex = -1
    }

    BoxWithConstraints(modifier = modifier) {
        val itemCount = items.size
        if (itemCount == 0 || maxHeight <= 0.dp) return@BoxWithConstraints

        // Fill the complete space between the top bar and mini player. This keeps the
        // first and last hit targets anchored while their spacing adapts to the window.
        val cellSize = maxHeight / itemCount.toFloat()
        val barHeight = cellSize * itemCount
        val cellHeightPx = with(density) { cellSize.toPx() }

        fun itemIndexAt(y: Float): Int =
            (y / cellHeightPx).toInt().coerceIn(0, items.lastIndex)

        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .height(maxHeight),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlphabetIndicator(
                item = indicatorItem,
                visible = indicatorVisible,
            )
            Box(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .width(cellSize)
                    .height(barHeight)
                    .pointerInput(items, cellHeightPx, sectionIndexMap) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            val downItemIndex = itemIndexAt(down.position.y)
                            var dragged = false
                            var released = false
                            if (downItemIndex != 0) {
                                updateSelection(downItemIndex)
                            } else if (currentShowScrollTop) {
                                selectedItem = items[0]
                                indicatorItem = items[0]
                                indicatorVisible = true
                            }
                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                        ?: break
                                    if (!change.pressed) {
                                        released = true
                                        break
                                    }
                                    change.consume()
                                    if (
                                        !dragged &&
                                        (change.position - down.position).getDistance() >= touchSlop
                                    ) {
                                        dragged = true
                                    }
                                    if (dragged) {
                                        updateSelection(
                                            itemIndexAt(change.position.y).coerceAtLeast(1),
                                        )
                                    }
                                }
                            } finally {
                                if (
                                    released &&
                                    !dragged &&
                                    downItemIndex == 0 &&
                                    currentShowScrollTop
                                ) {
                                    updateSelection(0, force = true)
                                }
                                clearSelection()
                            }
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                items.forEach { item ->
                    AlphabetCell(
                        item = item,
                        selected = selectedItem == item,
                        size = cellSize,
                        showScrollTop = showScrollTop,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlphabetIndicator(
    item: AlphabetSideBarItem?,
    visible: Boolean,
) {
    AnimatedVisibility(
        visible = visible && item != null,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .squircleBackground(
                    color = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                    cornerRadius = 25.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            when (item) {
                AlphabetSideBarItem.ScrollTop -> Icon(
                    imageVector = MiuixIcons.ChevronBackward,
                    contentDescription = null,
                    modifier = Modifier
                        .size(30.dp)
                        .rotate(90f),
                    tint = MiuixTheme.colorScheme.primary,
                )

                is AlphabetSideBarItem.Section -> Text(
                    text = item.value,
                    style = MiuixTheme.textStyles.title1,
                    color = MiuixTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )

                null -> Unit
            }
        }
    }
}

@Composable
private fun AlphabetCell(
    item: AlphabetSideBarItem,
    selected: Boolean,
    size: Dp,
    showScrollTop: Boolean,
) {
    val fontSize = when {
        size < 8.dp -> 4.sp
        size < 12.dp -> 6.sp
        size < 16.dp -> 8.sp
        else -> 9.sp
    }
    val selectedColor = if (MiuixTheme.colorScheme.surface.luminance() > 0.5f) {
        Color.Black
    } else {
        Color.White
    }
    val cellColor = if (selected) {
        selectedColor
    } else {
        MiuixTheme.colorScheme.onSurfaceVariantActions
    }

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        when (item) {
            AlphabetSideBarItem.ScrollTop -> AnimatedVisibility(
                visible = showScrollTop,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Icon(
                    imageVector = MiuixIcons.ChevronBackward,
                    contentDescription = null,
                    modifier = Modifier
                        .size(size * 0.68f)
                        .rotate(90f),
                    tint = cellColor,
                )
            }

            is AlphabetSideBarItem.Section -> Text(
                text = item.value,
                style = MiuixTheme.textStyles.body2.copy(fontSize = fontSize),
                color = cellColor,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

internal fun findAlphabetTargetIndex(
    section: String,
    sectionIndexMap: Map<String, Int>,
    sections: List<String> = AlphabetSections,
): Int {
    if (sectionIndexMap.isEmpty()) return 0
    sectionIndexMap[section]?.let { return it }

    val requestedIndex = sections.indexOf(section)
    if (requestedIndex < 0) return 0

    sections
        .drop(requestedIndex + 1)
        .firstNotNullOfOrNull(sectionIndexMap::get)
        ?.let { return it }

    return sections
        .asReversed()
        .firstNotNullOfOrNull(sectionIndexMap::get)
        ?: 0
}
