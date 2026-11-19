package com.melox.player.ui.component.library

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
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
import com.melox.player.ui.theme.LocalHapticFeedbackEnabled
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.CardDefaults
import com.melox.player.ui.component.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val AlphabetSideBarWidth = 20.dp

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
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    sections: List<String> = AlphabetSections,
    onTargetIndexChanged: (targetIndex: Int, restoreLargeTitle: Boolean) -> Unit,
) {
    val view = LocalView.current
    val hapticFeedbackEnabled by rememberUpdatedState(LocalHapticFeedbackEnabled.current)
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
    val currentIsAtTarget by rememberUpdatedState(isAtTarget)
    val scrollTopVisible by remember {
        derivedStateOf { currentItemCount > 0 && !currentIsAtTarget(0) }
    }
    val currentShowScrollTop by rememberUpdatedState(scrollTopVisible)
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
        scrollJob?.cancel()
        if (!currentIsAtTarget(targetIndex)) {
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
        if (hapticFeedbackEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    fun clearSelection() {
        selectedItem = null
        indicatorVisible = false
        lastSelectedIndex = -1
    }

    val preferredCellHeight = 15.dp
    BoxWithConstraints(modifier = modifier.padding(contentPadding).fillMaxHeight()) {
        if (maxHeight <= 0.dp || sections.isEmpty()) return@BoxWithConstraints

        val slotCount = (maxHeight / preferredCellHeight).toInt()
        val ranges = remember(items.size, slotCount) {
            listOf(0..0) + alphabetSectionRanges(sections.size, slotCount - 1)
        }
        val currentRanges by rememberUpdatedState(ranges)
        val cellSize = with(density) {
            val availablePx = constraints.maxHeight / ranges.size
            minOf(preferredCellHeight.roundToPx(), availablePx).toDp()
        }
        if (cellSize <= 0.dp) return@BoxWithConstraints
        val barHeight = cellSize * ranges.size
        val textScale = cellSize / preferredCellHeight

        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd),
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
                    .width(AlphabetSideBarWidth)
                    .height(barHeight)
                    .pointerInput(items, sectionIndexMap, scrollStateKey) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            val downItemIndex = alphabetItemIndexAt(down.position.y, size.height, currentRanges)
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
                                            alphabetItemIndexAt(change.position.y, size.height, currentRanges)
                                                .coerceAtLeast(1),
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
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                val selectedIndex = items.indexOf(selectedItem)
                ranges.forEach { range ->
                    AlphabetCell(
                        item = items[range.first].takeIf { range.first == range.last },
                        selected = selectedIndex in range,
                        size = cellSize,
                        textScale = textScale,
                        showScrollTop = scrollTopVisible,
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
                    color = CardDefaults.defaultColors().color.copy(alpha = 0.8f),
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

                is AlphabetSideBarItem.Section -> AlphabetText(
                    text = item.value,
                    style = MiuixTheme.textStyles.title1.copy(fontWeight = FontWeight.Bold),
                    color = MiuixTheme.colorScheme.primary,
                )

                null -> Unit
            }
        }
    }
}

@Composable
private fun AlphabetCell(
    item: AlphabetSideBarItem?,
    selected: Boolean,
    size: Dp,
    textScale: Float,
    showScrollTop: Boolean,
) {
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
        modifier = Modifier.width(AlphabetSideBarWidth).height(size),
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
                        .size(12.dp.coerceAtMost(size))
                        .rotate(90f),
                    tint = cellColor,
                )
            }

            is AlphabetSideBarItem.Section, null -> AlphabetText(
                text = item?.value ?: "·",
                style = MiuixTheme.textStyles.footnote2.let {
                    it.copy(
                        fontSize = it.fontSize * textScale,
                        lineHeight = if (it.lineHeight.isSpecified) it.lineHeight * textScale else it.lineHeight,
                    )
                },
                color = cellColor,
            )
        }
    }
}

@Composable
private fun AlphabetText(text: String, style: TextStyle, color: Color) {
    val density = LocalDensity.current.density
    val fixedTextDensity = remember(density) { Density(density, fontScale = 1f) }
    CompositionLocalProvider(LocalDensity provides fixedTextDensity) {
        Text(text = text, style = style, color = color, textAlign = TextAlign.Center, maxLines = 1)
    }
}

// Item 0 belongs to ScrollTop. The first and last sections flank the letter range.
internal fun alphabetSectionRanges(sectionCount: Int, slots: Int): List<IntRange> {
    if (slots >= sectionCount || sectionCount <= 5) {
        return (1..sectionCount).map { it..it }
    }

    val lastLetter = sectionCount - 1
    val visibleLetters = ((slots - 1) / 2).coerceAtLeast(2)
    return buildList {
        add(1..1)
        var previous = 1
        for (index in 0 until visibleLetters) {
            val letter = 2 + (index * (lastLetter - 2f) / (visibleLetters - 1)).roundToInt()
            if (letter > previous + 1) add((previous + 1) until letter)
            add(letter..letter)
            previous = letter
        }
        add(sectionCount..sectionCount)
    }
}

internal fun alphabetItemIndexAt(y: Float, height: Int, ranges: List<IntRange>): Int {
    if (height <= 0) return 0
    val position = (y / height * ranges.size).coerceIn(0f, ranges.size.toFloat())
    val slot = position.toInt().coerceAtMost(ranges.lastIndex)
    val range = ranges[slot]
    return (range.first + ((position - slot) * (range.last - range.first + 1)).toInt())
        .coerceAtMost(range.last)
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
