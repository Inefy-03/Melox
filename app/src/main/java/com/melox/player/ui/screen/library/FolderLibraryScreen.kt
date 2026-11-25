package com.melox.player.ui.screen.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.melox.player.ui.component.LocalPageAlphabetIndexPadding
import com.melox.player.ui.component.library.PreserveSortScrollPosition
import com.melox.player.ui.component.library.offsetSectionIndices

import com.melox.player.ui.icon.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import top.yukonga.miuix.kmp.utils.SinkFeedback
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.melox.player.ui.component.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melox.player.R
import com.melox.player.data.library.FolderGroup
import com.melox.player.data.library.FolderSortConfig
import com.melox.player.data.library.FolderSortField
import com.melox.player.model.ScanStatus
import com.melox.player.ui.component.library.AlphabetSections
import com.melox.player.ui.component.library.AlphabetSideBar
import com.melox.player.ui.component.Icon
import com.melox.player.ui.component.PageCard
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import com.melox.player.ui.component.OverlayDialog
import com.melox.player.ui.component.OverlayDialogActions
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.extended.FolderFill
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.melox.player.ui.component.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun FolderLibraryScreen(
    displayedFolders: List<FolderGroup>,
    sectionIndexMap: Map<String, Int>,
    scanStatus: ScanStatus,
    sortConfig: FolderSortConfig,
    onFolderClick: (FolderGroup) -> Unit,
    onOpenInternalStorage: () -> Unit,
    onBlockFolder: (String) -> Unit = {},
    scrollBehavior: ScrollBehavior,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    showIndex: Boolean = true,
) {
    var pendingBlockFolder by remember { mutableStateOf<FolderGroup?>(null) }
    val layoutDirection = LocalLayoutDirection.current
    val listSectionIndexMap = remember(sectionIndexMap) { offsetSectionIndices(sectionIndexMap, 1) }
    PreserveSortScrollPosition(
        sortKey = sortConfig,
        query = "",
        itemCount = maxOf(displayedFolders.size, 1) + 1,
        scrollState = listState,
    ) {
        listState.requestScrollToItem(
            listState.firstVisibleItemIndex,
            listState.firstVisibleItemScrollOffset,
        )
    }
    val sections = remember(sortConfig.descending) {
        if (sortConfig.descending) AlphabetSections.asReversed() else AlphabetSections
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .scrollEndHaptic()
                .overScrollVertical(scrollBehavior = scrollBehavior)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            state = listState,
            contentPadding = PaddingValues(
                start = contentPadding.calculateStartPadding(layoutDirection),
                top = contentPadding.calculateTopPadding() + 12.dp,
                end = contentPadding.calculateEndPadding(layoutDirection),
                bottom = contentPadding.calculateBottomPadding() + 12.dp,
            ),
            overscrollEffect = null,
        ) {
            item(key = "internal_storage") {
                PageCard(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 20.dp).padding(bottom = 12.dp),
                ) {
                    ArrowPreference(
                        title = stringResource(R.string.folder_internal_storage),
                        onClick = onOpenInternalStorage,
                        startAction = {
                            Icon(
                                imageVector = MiuixIcons.FolderFill,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                    )
                }
            }
            if (displayedFolders.isEmpty()) {
                item(key = "empty_folder") {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        MusicLibraryEmptyState(
                            scanStatus = scanStatus,
                            query = "",
                            emptyMessageRes = R.string.folder_empty,
                            noSearchResultsRes = R.string.folder_no_search_results,
                            icon = GlassIcons.FolderFill,
                        )
                    }
                }
            } else {
                items(
                    items = displayedFolders,
                    key = FolderGroup::key,
                ) { folder ->
                    FolderListItem(
                        folder = folder,
                        onClick = { onFolderClick(folder) },
                        onLongClick = { pendingBlockFolder = folder },
                    )
                }
            }
        }

        if (
            showIndex &&
            displayedFolders.isNotEmpty() &&
            sortConfig.field == FolderSortField.NAME
        ) {
            AlphabetSideBar(
                contentPadding = LocalPageAlphabetIndexPadding.current,
                sectionIndexMap = listSectionIndexMap,
                itemCount = displayedFolders.size + 1,
                scrollStateKey = listState,
                isAtTarget = { targetIndex ->
                    listState.firstVisibleItemIndex == targetIndex &&
                        listState.firstVisibleItemScrollOffset == 0
                },
                scrollToItem = listState::scrollToItem,
                sections = sections,
                canScrollBackward = { listState.canScrollBackward },
                scrollBehavior = scrollBehavior,
                modifier = Modifier
                    .align(Alignment.TopEnd),
            )
        }
    }

    FolderHideDialog(
        folder = pendingBlockFolder,
        onDismiss = { pendingBlockFolder = null },
        onBlockFolder = onBlockFolder,
    )
}

@Composable
internal fun FolderHideDialog(
    folder: FolderGroup?,
    onDismiss: () -> Unit,
    onBlockFolder: (String) -> Unit,
) {
    OverlayDialog(
        show = folder != null,
        title = stringResource(R.string.folder_block_title),
        summary = stringResource(
            R.string.folder_block_message,
            folder?.path.orEmpty(),
        ),
        enableWindowDim = true,
        onDismissRequest = onDismiss,
    ) {
        OverlayDialogActions(
            dismissText = stringResource(R.string.clear_queue_confirm_cancel),
            confirmText = stringResource(R.string.clear_queue_confirm_confirm),
            onDismiss = onDismiss,
            onConfirm = {
                val path = folder?.path
                onDismiss()
                path?.let(onBlockFolder)
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FolderListItem(
    folder: FolderGroup,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    songCount: Int = folder.tracks.size,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val songCountText = pluralStringResource(
        R.plurals.folder_song_count,
        songCount,
        songCount,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = interactionSource,
                indication = SinkFeedback(),
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(start = 30.dp, end = 28.dp, top = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_file),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = folder.name ?: stringResource(R.string.folder_unknown),
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = songCountText,
                style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val layoutDirection = LocalLayoutDirection.current
        Image(
            imageVector = MiuixIcons.Basic.ArrowRight,
            contentDescription = null,
            modifier = Modifier.size(width = 10.dp, height = 16.dp).graphicsLayer {
                scaleX = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
            },
            colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurfaceVariantActions),
        )
    }
}
