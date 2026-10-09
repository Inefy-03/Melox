package com.melox.player.ui.screen.playback

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.melox.player.R
import com.melox.player.ui.component.bottomSheetGlassModifier
import com.melox.player.ui.component.bottomSheetMaterialColor
import com.melox.player.data.library.displayArtistName
import com.melox.player.model.PlaybackQueueItem
import com.melox.player.model.PlaybackUiState
import com.melox.player.ui.component.library.PlaybackArtwork
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val QueueArtworkSize = 44.dp
private val QueueArtworkCornerRadius = 6.dp
private val QueueRowHeight = QueueArtworkSize + 24.dp
private val QueuePositionRowHeight = 16.dp
private val QueueTitlePositionSpacing = 6.dp
private val QueueSheetHeaderHeight = 82.dp + QueueTitlePositionSpacing

@Composable
private fun QueueSheetPosition(position: String) {
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            Text(
                text = position,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(QueuePositionRowHeight),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
                lineHeight = QueuePositionRowHeight.value.sp,
            )
        },
    ) { measurables, constraints ->
        val positionText = measurables.single().measure(Constraints(maxWidth = constraints.maxWidth))
        layout(constraints.maxWidth, QueueTitlePositionSpacing.roundToPx()) {
            positionText.placeRelative(0, -positionText.height)
        }
    }
}

private fun PlaybackQueueItem.queueSlotKey(): String = buildString {
    append(mediaId.length)
    append(':')
    append(mediaId)
    append(contentUri.length)
    append(':')
    append(contentUri)
    append(':')
    append(sourceOrder.toBits())
}

internal fun queueListHeight(
    itemCount: Int,
    rowHeight: Dp,
    maxHeight: Dp,
): Dp {
    val boundedMaxHeight = maxHeight.coerceAtLeast(0.dp)
    if (itemCount <= 0 || rowHeight <= 0.dp || boundedMaxHeight == 0.dp) return 0.dp

    return if (itemCount.toFloat() >= boundedMaxHeight.value / rowHeight.value) {
        boundedMaxHeight
    } else {
        rowHeight * itemCount
    }
}

internal fun <T> moveQueueListItem(
    items: List<T>,
    fromIndex: Int,
    toIndex: Int,
): List<T> {
    if (fromIndex !in items.indices || toIndex !in items.indices || fromIndex == toIndex) {
        return items
    }
    return items.toMutableList().apply {
        add(toIndex, removeAt(fromIndex))
    }
}

internal fun queueMoveTargetAfterCurrent(
    itemIndex: Int,
    currentIndex: Int,
    itemCount: Int,
): Int? {
    if (
        itemIndex == currentIndex ||
        itemIndex !in 0 until itemCount ||
        currentIndex !in 0 until itemCount
    ) {
        return null
    }
    return if (itemIndex < currentIndex) currentIndex else currentIndex + 1
}

internal fun queueShowsPlayNextAction(
    itemIndex: Int,
    currentIndex: Int,
    itemCount: Int,
): Boolean =
    itemIndex in 0 until itemCount &&
        currentIndex in 0 until itemCount &&
        itemIndex != currentIndex &&
        itemIndex != currentIndex + 1

internal fun queueOffscreenExitTranslation(
    itemIndex: Int,
    currentIndex: Int,
    itemOffsetPx: Int,
    itemSizePx: Int,
    viewportStartPx: Int,
    viewportEndPx: Int,
): Float = if (itemIndex > currentIndex) {
    (viewportStartPx - itemOffsetPx - itemSizePx).toFloat()
} else {
    (viewportEndPx - itemOffsetPx).toFloat()
}

internal fun queueOffscreenExitAlpha(
    itemIndex: Int,
    currentIndex: Int,
    itemOffsetPx: Int,
    itemSizePx: Int,
    translationY: Float,
    viewportStartPx: Int,
    viewportEndPx: Int,
): Float {
    if (itemSizePx <= 0) return 0f
    val translatedTop = itemOffsetPx + translationY
    return if (itemIndex > currentIndex) {
        ((translatedTop + itemSizePx - viewportStartPx) / itemSizePx).coerceIn(0f, 1f)
    } else {
        ((viewportEndPx - translatedTop) / itemSizePx).coerceIn(0f, 1f)
    }
}

internal fun queuePlayNextPeerTranslation(
    itemIndex: Int,
    movingIndex: Int,
    currentIndex: Int,
    rowHeightPx: Float,
    progress: Float,
): Float = when {
    movingIndex > currentIndex && itemIndex in (currentIndex + 1) until movingIndex ->
        rowHeightPx * progress
    movingIndex < currentIndex && itemIndex in (movingIndex + 1)..currentIndex ->
        -rowHeightPx * progress
    else -> 0f
}

internal fun queueLocationAnchorHeight(
    itemCount: Int,
    currentIndex: Int,
    rowHeight: Dp,
    viewportHeight: Dp,
    bottomPadding: Dp,
): Dp {
    if (currentIndex !in 0 until itemCount) return 0.dp
    val rowsAfterCurrent = itemCount - currentIndex - 1
    return (
        viewportHeight -
            rowHeight -
            rowHeight * rowsAfterCurrent -
            bottomPadding
    ).coerceAtLeast(0.dp)
}

@Composable
fun QueueSheet(
    show: Boolean,
    playback: PlaybackUiState,
    onDismiss: () -> Unit,
    onJumpTo: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClear: () -> Unit,
) {
    var showClearConfirm by remember { mutableStateOf(false) }
    val queueListState = rememberLazyListState()
    var draftQueue by remember { mutableStateOf(playback.queue) }
    var dragStartIndex by remember { mutableIntStateOf(-1) }
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var playNextExitKey by remember { mutableStateOf<String?>(null) }
    var playNextExitTranslationY by remember { mutableStateOf(0f) }
    var playNextExitItemOffsetPx by remember { mutableIntStateOf(0) }
    var playNextExitItemSizePx by remember { mutableIntStateOf(0) }
    var playNextExitViewportStartPx by remember { mutableIntStateOf(0) }
    var playNextExitViewportEndPx by remember { mutableIntStateOf(0) }
    val playNextExitProgress = remember { Animatable(0f) }
    val queueCoroutineScope = rememberCoroutineScope()
    val currentOnMove by rememberUpdatedState(onMove)
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val view = LocalView.current
    val queueReorderState = rememberReorderableLazyListState(queueListState) { from, to ->
        val fromIndex = draftQueue.indexOfFirst { item -> item.queueSlotKey() == from.key }
        val toIndex = draftQueue.indexOfFirst { item -> item.queueSlotKey() == to.key }
        if (fromIndex in draftQueue.indices && toIndex in draftQueue.indices) {
            draftQueue = moveQueueListItem(draftQueue, fromIndex, toIndex)
        }
    }

    fun finishDraggedItem(activeKey: String) {
        if (draggedKey != activeKey) return
        val fromIndex = dragStartIndex
        val toIndex = draftQueue.indexOfFirst { it.queueSlotKey() == activeKey }
        if (fromIndex in draftQueue.indices && toIndex in draftQueue.indices && fromIndex != toIndex) {
            currentOnMove(fromIndex, toIndex)
            hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
        }
        draggedKey = null
        dragStartIndex = -1
    }

    LaunchedEffect(show) {
        if (!show) {
            showClearConfirm = false
            draftQueue = playback.queue
            playNextExitKey = null
            playNextExitProgress.snapTo(0f)
            draggedKey = null
            dragStartIndex = -1
        } else {
            draftQueue = playback.queue
            withFrameNanos { }
            if (playback.currentIndex in playback.queue.indices) {
                queueListState.scrollToItem(playback.currentIndex)
            }
        }
    }
    LaunchedEffect(show, playback.queue) {
        if (show && !queueReorderState.isAnyItemDragging && playNextExitKey == null) {
            draftQueue = playback.queue
        }
    }
    val queueListBottomPadding = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding() + 12.dp
    val queueSheetTopInset = maxOf(
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
        WindowInsets.captionBar.asPaddingValues().calculateTopPadding(),
        WindowInsets.displayCutout.asPaddingValues().calculateTopPadding(),
    )
    val queueMaxListHeight = (
        LocalWindowInfo.current.containerDpSize.height -
            queueSheetTopInset -
            QueueSheetHeaderHeight
    ).coerceAtLeast(0.dp)
    val queueRowsMaxHeight = (
        queueMaxListHeight - queueListBottomPadding
    ).coerceAtLeast(0.dp)
    val currentKey = playback.currentItem?.queueSlotKey()
    val displayedCurrentIndex = draftQueue.indexOfFirst { item ->
        item.queueSlotKey() == currentKey
    }
    val queuePosition =
        "${(displayedCurrentIndex + 1).coerceAtLeast(0)} / ${draftQueue.size}"

    fun moveAfterCurrent(index: Int, slotKey: String) {
        if (playNextExitKey != null) return
        val targetIndex = queueMoveTargetAfterCurrent(
            itemIndex = index,
            currentIndex = displayedCurrentIndex,
            itemCount = draftQueue.size,
        ) ?: return
        val layoutInfo = queueListState.layoutInfo
        val currentVisible = layoutInfo.visibleItemsInfo.any { itemInfo ->
            itemInfo.key == currentKey
        }
        val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { visibleItem ->
            visibleItem.key == slotKey
        }

        if (currentVisible || itemInfo == null) {
            draftQueue = moveQueueListItem(draftQueue, index, targetIndex)
            currentOnMove(index, targetIndex)
            return
        }

        val exitTranslationY = queueOffscreenExitTranslation(
            itemIndex = index,
            currentIndex = displayedCurrentIndex,
            itemOffsetPx = itemInfo.offset,
            itemSizePx = itemInfo.size,
            viewportStartPx = layoutInfo.viewportStartOffset,
            viewportEndPx = layoutInfo.viewportEndOffset,
        )
        queueCoroutineScope.launch {
            playNextExitKey = slotKey
            playNextExitTranslationY = exitTranslationY
            playNextExitItemOffsetPx = itemInfo.offset
            playNextExitItemSizePx = itemInfo.size
            playNextExitViewportStartPx = layoutInfo.viewportStartOffset
            playNextExitViewportEndPx = layoutInfo.viewportEndOffset
            playNextExitProgress.snapTo(0f)
            playNextExitProgress.animateTo(
                targetValue = 1f,
                animationSpec = folmeSpring(damping = 0.9f, response = 0.38f),
            )
            draftQueue = moveQueueListItem(draftQueue, index, targetIndex)
            currentOnMove(index, targetIndex)
            playNextExitProgress.snapTo(0f)
            withFrameNanos { }
            playNextExitKey = null
        }
    }

    OverlayBottomSheet(
        show = show,
        modifier = bottomSheetGlassModifier(),
        title = stringResource(R.string.playback_queue),
        insideMargin = DpSize(0.dp, 0.dp),
        startAction = {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.padding(start = 16.dp),
            ) {
                Icon(
                    imageVector = MiuixIcons.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = MiuixTheme.colorScheme.onBackground,
                )
            }
        },
        endAction = {
            Row(modifier = Modifier.padding(end = 20.dp)) {
                IconButton(
                    onClick = {
                        if (displayedCurrentIndex in draftQueue.indices) {
                            queueCoroutineScope.launch {
                                queueListState.animateScrollToItem(displayedCurrentIndex)
                            }
                        }
                    },
                    enabled = displayedCurrentIndex in draftQueue.indices,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_locate_current_track),
                        contentDescription = stringResource(R.string.locate_current_track),
                        modifier = Modifier.size(24.dp),
                    )
                }
                IconButton(
                    onClick = { showClearConfirm = true },
                    enabled = playback.queue.isNotEmpty(),
                ) {
                    Icon(
                        imageVector = MiuixIcons.Delete,
                        contentDescription = stringResource(R.string.clear_queue),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        },
        backgroundColor = bottomSheetMaterialColor(),
        enableWindowDim = true,
        onDismissRequest = onDismiss,
    ) {
        QueueSheetPosition(queuePosition)
        if (draftQueue.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp + queueListBottomPadding)
                    .padding(bottom = queueListBottomPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.queue_empty),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        } else {
            val queueListViewportHeight = (
                queueListHeight(
                    itemCount = draftQueue.size,
                    rowHeight = QueueRowHeight,
                    maxHeight = queueRowsMaxHeight,
                ) + queueListBottomPadding
            ).coerceAtMost(queueMaxListHeight)
            val locationAnchorHeight = queueLocationAnchorHeight(
                itemCount = draftQueue.size,
                currentIndex = displayedCurrentIndex,
                rowHeight = QueueRowHeight,
                viewportHeight = queueListViewportHeight,
                bottomPadding = queueListBottomPadding,
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(queueListViewportHeight),
                state = queueListState,
                userScrollEnabled = !queueReorderState.isAnyItemDragging,
                contentPadding = PaddingValues(bottom = queueListBottomPadding),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                itemsIndexed(
                    items = draftQueue,
                    key = { _, item -> item.queueSlotKey() },
                ) { index, item ->
                    val slotKey = item.queueSlotKey()
                    val isCurrent = slotKey == currentKey
                    val isPlayNextExiting = slotKey == playNextExitKey
                    val peerTranslationY = playNextExitKey?.let { movingKey ->
                        val movingIndex = draftQueue.indexOfFirst { queueItem ->
                            queueItem.queueSlotKey() == movingKey
                        }
                        queuePlayNextPeerTranslation(
                            itemIndex = index,
                            movingIndex = movingIndex,
                            currentIndex = displayedCurrentIndex,
                            rowHeightPx = with(density) { QueueRowHeight.toPx() },
                            progress = playNextExitProgress.value,
                        )
                    } ?: 0f
                    ReorderableItem(
                        state = queueReorderState,
                        key = slotKey,
                        animateItemModifier = if (playNextExitKey == null) {
                            Modifier.animateItem(
                                fadeInSpec = null,
                                placementSpec = spring(),
                                fadeOutSpec = null,
                            )
                        } else {
                            Modifier
                        },
                    ) { isDragging ->
                        BasicComponent(
                            modifier = Modifier
                                .fillMaxWidth()
                                .longPressDraggableHandle(
                                    enabled = playNextExitKey == null,
                                    onDragStarted = {
                                        view.performHapticFeedback(
                                            HapticFeedbackConstants.LONG_PRESS,
                                        )
                                        draggedKey = slotKey
                                        dragStartIndex = draftQueue.indexOfFirst { queueItem ->
                                            queueItem.queueSlotKey() == slotKey
                                        }
                                    },
                                    onDragStopped = { finishDraggedItem(slotKey) },
                                )
                                .then(
                                    when {
                                        isPlayNextExiting -> Modifier
                                            .zIndex(1f)
                                            .graphicsLayer {
                                                val progress = playNextExitProgress.value
                                                translationY = playNextExitTranslationY * progress
                                                alpha = queueOffscreenExitAlpha(
                                                    itemIndex = index,
                                                    currentIndex = displayedCurrentIndex,
                                                    itemOffsetPx = playNextExitItemOffsetPx,
                                                    itemSizePx = playNextExitItemSizePx,
                                                    translationY = translationY,
                                                    viewportStartPx = playNextExitViewportStartPx,
                                                    viewportEndPx = playNextExitViewportEndPx,
                                                )
                                            }
                                        peerTranslationY != 0f -> Modifier.graphicsLayer {
                                            translationY = peerTranslationY
                                        }
                                        else -> Modifier
                                    }
                                ),
                            holdDownState = isCurrent || isDragging,
                            startAction = {
                                PlaybackArtwork(
                                    contentUri = item.contentUri,
                                    dateModifiedEpochSeconds = item.dateModifiedEpochSeconds,
                                    fileSizeBytes = item.fileSizeBytes,
                                    size = QueueArtworkSize,
                                    cornerRadius = QueueArtworkCornerRadius,
                                    contentScale = ContentScale.Fit,
                                )
                            },
                            endActions = {
                                if (
                                    queueShowsPlayNextAction(
                                        itemIndex = index,
                                        currentIndex = displayedCurrentIndex,
                                        itemCount = draftQueue.size,
                                    )
                                ) {
                                    IconButton(
                                        onClick = { moveAfterCurrent(index, slotKey) },
                                        enabled = playNextExitKey == null,
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                if (index < displayedCurrentIndex) {
                                                    R.drawable.ic_queue_play_next_end
                                                } else {
                                                    R.drawable.ic_queue_play_next_start
                                                },
                                            ),
                                            contentDescription =
                                                stringResource(R.string.move_after_current),
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onRemove(index) },
                                    modifier = Modifier.padding(end = 3.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_remove_circle),
                                        contentDescription =
                                            stringResource(R.string.remove_from_queue),
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            },
                            insideMargin = PaddingValues(
                                start = 24.dp,
                                top = 12.dp,
                                end = 16.dp,
                                bottom = 12.dp,
                            ),
                            onClick = {
                                onJumpTo(index)
                                onDismiss()
                            },
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = item.title,
                                    style = MiuixTheme.textStyles.headline2,
                                    fontWeight = FontWeight.Medium,
                                    color = MiuixTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = displayArtistName(item.artist)
                                        ?: stringResource(R.string.music_unknown_artist),
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                if (locationAnchorHeight > 0.dp) {
                    item(key = "queue-location-anchor") {
                        Spacer(modifier = Modifier.height(locationAnchorHeight))
                    }
                }
            }
        }
    }

    OverlayDialog(
        show = show && showClearConfirm,
        title = stringResource(R.string.clear_queue_confirm_title),
        summary = stringResource(R.string.clear_queue_confirm_message),
        enableWindowDim = true,
        onDismissRequest = { showClearConfirm = false },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(
                text = stringResource(R.string.clear_queue_confirm_cancel),
                onClick = { showClearConfirm = false },
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(20.dp))
            TextButton(
                text = stringResource(R.string.clear_queue_confirm_confirm),
                onClick = {
                    showClearConfirm = false
                    onClear()
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}
