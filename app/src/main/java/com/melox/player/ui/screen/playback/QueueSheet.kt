package com.melox.player.ui.screen.playback

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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.melox.player.R
import com.melox.player.data.library.displayArtistName
import com.melox.player.model.PlaybackUiState
import com.melox.player.ui.component.library.PlaybackArtwork
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val QueueArtworkSize = 44.dp
private val QueueArtworkCornerRadius = 6.dp
private val QueueRowHeight = QueueArtworkSize + 24.dp
private val QueueSheetHeaderHeight = 82.dp

internal fun queueCardHeight(
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

@Composable
fun QueueSheet(
    show: Boolean,
    playback: PlaybackUiState,
    onDismiss: () -> Unit,
    onJumpTo: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClear: () -> Unit,
) {
    var showClearConfirm by remember { mutableStateOf(false) }
    val queueListState = rememberLazyListState()
    LaunchedEffect(show) {
        if (!show) showClearConfirm = false
    }
    LaunchedEffect(show, playback.currentIndex, playback.queue.size) {
        if (show && playback.currentIndex in playback.queue.indices) {
            queueListState.scrollToItem(playback.currentIndex)
        }
    }
    val sheetBackground = BottomSheetDefaults.backgroundColor()
    val queueBottomPadding = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding() + 12.dp
    val queueSheetTopInset = maxOf(
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
        WindowInsets.captionBar.asPaddingValues().calculateTopPadding(),
        WindowInsets.displayCutout.asPaddingValues().calculateTopPadding(),
    )
    val queueMaxCardHeight = (
        LocalWindowInfo.current.containerDpSize.height -
            queueSheetTopInset -
            QueueSheetHeaderHeight -
            queueBottomPadding
        ).coerceAtLeast(0.dp)

    OverlayBottomSheet(
        show = show,
        title = stringResource(R.string.playback_queue),
        startAction = {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = MiuixIcons.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = MiuixTheme.colorScheme.onBackground,
                )
            }
        },
        endAction = {
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
        },
        backgroundColor = sheetBackground,
        enableWindowDim = true,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = queueBottomPadding),
        ) {
            if (playback.queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.queue_empty),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            queueCardHeight(
                                itemCount = playback.queue.size,
                                rowHeight = QueueRowHeight,
                                maxHeight = queueMaxCardHeight,
                            ),
                        ),
                    colors = CardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = queueListState,
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        itemsIndexed(
                            items = playback.queue,
                            key = { index, item -> "${item.mediaId}:$index" },
                        ) { index, item ->
                            val isCurrent = index == playback.currentIndex
                            BasicComponent(
                                modifier = Modifier.fillMaxWidth(),
                                holdDownState = isCurrent,
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
                                    IconButton(onClick = { onRemove(index) }) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_remove_circle),
                                            contentDescription =
                                                stringResource(R.string.remove_from_queue),
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                },
                                insideMargin = PaddingValues(
                                    start = 12.dp,
                                    top = 12.dp,
                                    end = 12.dp,
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
