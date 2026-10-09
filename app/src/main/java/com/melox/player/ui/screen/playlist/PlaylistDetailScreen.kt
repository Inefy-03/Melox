package com.melox.player.ui.screen.playlist

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.melox.player.R
import com.melox.player.data.library.ArtistGroup
import com.melox.player.data.library.createMusicSortKeys
import com.melox.player.data.library.filterMusicTracks
import com.melox.player.data.playlist.PlaylistSortConfig
import com.melox.player.data.playlist.PlaylistSortField
import com.melox.player.data.playlist.canonicalPlaylistEntryOrder
import com.melox.player.data.playlist.resolvePlaylistPlaybackSelection
import com.melox.player.data.playlist.resolvePlaylistTracks
import com.melox.player.data.playlist.sortPlaylistTracks
import com.melox.player.model.LocalPlaylist
import com.melox.player.model.MusicTrack
import com.melox.player.model.ResolvedPlaylistTrack
import com.melox.player.ui.LibrarySearchBar
import com.melox.player.ui.LibrarySearchButton
import com.melox.player.ui.component.AdaptiveTopAppBar
import com.melox.player.ui.component.BlurredBar
import com.melox.player.ui.component.library.LocalAlphabetIndexBottomPadding
import com.melox.player.ui.component.library.fixedAlphabetIndexTopPadding
import com.melox.player.ui.component.library.rememberSearchTopBarScrollBehavior
import com.melox.player.ui.component.library.AlphabetSections
import com.melox.player.ui.component.library.AlphabetSideBar
import com.melox.player.ui.component.library.ShufflePlayButton
import com.melox.player.ui.component.library.MusicTrackRow
import com.melox.player.ui.component.library.PreserveSortScrollPosition
import com.melox.player.ui.component.library.SelectionActionsAnimatedContent
import com.melox.player.ui.component.library.SelectionNavigationIconAnimatedContent
import com.melox.player.ui.component.library.TrackActionsOverlay
import com.melox.player.ui.component.library.TrackSelectionActions
import com.melox.player.ui.component.library.toggleAllTrackSelection
import com.melox.player.ui.component.library.toggleTrackSelection
import com.melox.player.ui.component.miuixBarColor
import com.melox.player.ui.component.playlist.PlaylistNameDialog
import com.melox.player.ui.component.playlist.PlaylistSortButton
import com.melox.player.ui.component.rememberBlurBackdrop
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import com.melox.player.ui.component.PageScaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun PlaylistDetailScreen(
    playlist: LocalPlaylist,
    libraryTracks: List<MusicTrack>,
    readableContentUris: Set<String>,
    currentTrackId: Long?,
    artistGroups: List<ArtistGroup>,
    bottomContentPadding: Dp,
    onBack: () -> Unit,
    selectionExitRequest: Int,
    onSelectionModeChange: (Boolean) -> Unit,
    onTrackClick: (List<MusicTrack>, Int) -> Unit,
    onShufflePlay: (List<MusicTrack>) -> Unit,
    onPlayNext: (MusicTrack) -> Unit,
    onAppendToQueue: (MusicTrack) -> Unit,
    onAddToPlaylist: (MusicTrack) -> Unit,
    onGoToAlbum: (MusicTrack) -> Unit,
    onGoToArtist: (ArtistGroup) -> Unit,
    onExternalEditReturned: (Long) -> Unit,
    showMusicTagEditor: Boolean,
    showLyricoEditor: Boolean,
    showLunaBeatEditor: Boolean,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onRemoveEntries: (Set<String>) -> Unit,
    onMoveEntry: (List<String>) -> Boolean,
    sortConfig: PlaylistSortConfig,
    onSortConfigChange: (PlaylistSortConfig) -> Unit,
) {
    var query by rememberSaveable(playlist.id) { mutableStateOf("") }
    var searchVisible by rememberSaveable(playlist.id) { mutableStateOf(false) }
    var searchFocused by remember(playlist.id) { mutableStateOf(false) }
    val scrollBehavior = rememberSearchTopBarScrollBehavior(searchVisible)
    val backdrop = rememberBlurBackdrop()
    val listState = rememberLazyListState()
    val layoutDirection = LocalLayoutDirection.current
    val resolvedTracks = remember(playlist, libraryTracks, readableContentUris) {
        resolvePlaylistTracks(playlist, libraryTracks, readableContentUris)
    }
    val filteredTracks = remember(resolvedTracks, query) {
        val matchingContentUris = filterMusicTracks(
            resolvedTracks.map(ResolvedPlaylistTrack::track),
            query,
        ).map(MusicTrack::contentUri).toSet()
        resolvedTracks.filter { item -> item.track.contentUri in matchingContentUris }
    }
    val persistedDisplayedTracks = remember(filteredTracks, sortConfig) {
        sortPlaylistTracks(filteredTracks, sortConfig)
    }
    var selectedEntryIds by remember(playlist.id) {
        mutableStateOf<Set<String>>(emptySet())
    }
    var selectionMode by remember(playlist.id) { mutableStateOf(false) }
    var selectedTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var showRename by rememberSaveable(playlist.id) { mutableStateOf(false) }
    var showDelete by rememberSaveable(playlist.id) { mutableStateOf(false) }
    var showRemoveSelectedConfirm by rememberSaveable(playlist.id) {
        mutableStateOf(false)
    }
    var customDraftTracks by remember(playlist.id, sortConfig) {
        mutableStateOf(persistedDisplayedTracks)
    }
    var draggedEntryId by remember(playlist.id) { mutableStateOf<String?>(null) }
    var dragBaselineEntryIds by remember(playlist.id) {
        mutableStateOf<List<String>?>(null)
    }
    var reorderScrollTopPadding by remember { mutableStateOf(0.dp) }
    var reorderScrollBottomPadding by remember { mutableStateOf(0.dp) }
    val reorderEnabled =
        selectionMode && sortConfig.field == PlaylistSortField.CUSTOM && query.isBlank()
    val currentOnMoveEntry by rememberUpdatedState(onMoveEntry)
    val currentCanonicalEntryIds by rememberUpdatedState(
        playlist.entries.map { entry -> entry.id },
    )
    val currentPersistedDisplayedTracks by rememberUpdatedState(persistedDisplayedTracks)
    val hapticFeedback = LocalHapticFeedback.current
    val view = LocalView.current
    val playlistReorderState = rememberReorderableLazyListState(
        lazyListState = listState,
        scrollThresholdPadding = PaddingValues(
            top = reorderScrollTopPadding,
            bottom = reorderScrollBottomPadding,
        ),
    ) { from, to ->
        val fromIndex = customDraftTracks.indexOfFirst { item -> item.entry.id == from.key }
        val toIndex = customDraftTracks.indexOfFirst { item -> item.entry.id == to.key }
        if (fromIndex in customDraftTracks.indices && toIndex in customDraftTracks.indices) {
            customDraftTracks = customDraftTracks.toMutableList().apply {
                add(toIndex, removeAt(fromIndex))
            }
        }
    }
    val isDragging = reorderEnabled && playlistReorderState.isAnyItemDragging
    val displayedTracks = if (sortConfig.field == PlaylistSortField.CUSTOM) {
        customDraftTracks
    } else {
        persistedDisplayedTracks
    }
    val displayedEntryIds = displayedTracks.map { item -> item.entry.id }
    PreserveSortScrollPosition(sortConfig, query, displayedTracks.size, listState) {
        listState.requestScrollToItem(
            listState.firstVisibleItemIndex,
            listState.firstVisibleItemScrollOffset,
        )
    }
    val sectionIndexMap = remember(displayedTracks, sortConfig.field) {
        if (
            sortConfig.field == PlaylistSortField.TITLE ||
            sortConfig.field == PlaylistSortField.FILE_NAME
        ) {
            buildMap {
                displayedTracks.forEachIndexed { index, item ->
                    val section = when (sortConfig.field) {
                        PlaylistSortField.FILE_NAME -> {
                            createMusicSortKeys(item.track.fileName).section
                        }

                        else -> item.track.titleSectionKey
                    }
                    putIfAbsent(section, index)
                }
            }
        } else {
            emptyMap()
        }
    }
    val sections = remember(sortConfig.descending) {
        if (sortConfig.descending) AlphabetSections.asReversed() else AlphabetSections
    }

    fun finishDraggedEntry(activeEntryId: String) {
        if (draggedEntryId != activeEntryId) return
        val baselineEntryIds = dragBaselineEntryIds
        val orderedEntryIds = canonicalPlaylistEntryOrder(
            displayedEntryIds = customDraftTracks.map { item -> item.entry.id },
            descending = sortConfig.descending,
        )
        val sourceUnchanged = baselineEntryIds != null &&
            baselineEntryIds == currentCanonicalEntryIds
        val positionChanged = baselineEntryIds != null && orderedEntryIds != baselineEntryIds
        val accepted = when {
            !sourceUnchanged -> false
            orderedEntryIds == baselineEntryIds -> true
            else -> currentOnMoveEntry(orderedEntryIds)
        }
        if (accepted && positionChanged) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
        }
        if (!accepted || orderedEntryIds == baselineEntryIds) {
            customDraftTracks = currentPersistedDisplayedTracks
        }
        draggedEntryId = null
        dragBaselineEntryIds = null
    }

    LaunchedEffect(persistedDisplayedTracks, sortConfig.field) {
        if (!isDragging && sortConfig.field == PlaylistSortField.CUSTOM) {
            customDraftTracks = persistedDisplayedTracks
        }
    }
    LaunchedEffect(displayedEntryIds) {
        val retainedSelection = selectedEntryIds intersect displayedEntryIds.toSet()
        if (retainedSelection != selectedEntryIds) {
            selectedEntryIds = retainedSelection
        }
    }
    LaunchedEffect(selectionMode) {
        onSelectionModeChange(selectionMode)
    }
    LaunchedEffect(selectionExitRequest) {
        if (selectionMode && !isDragging) {
            selectedEntryIds = emptySet()
            selectionMode = false
        }
    }
    BackHandler(enabled = selectionMode) {
        if (!isDragging) {
            selectedEntryIds = emptySet()
            selectionMode = false
        }
    }

    PageScaffold(
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                blurEnabled = backdrop != null,
                scrollBehavior = scrollBehavior,
            ) {
                AdaptiveTopAppBar(
                    title = if (selectionMode) {
                        if (selectedEntryIds.isEmpty()) {
                            stringResource(R.string.selection_choose_songs)
                        } else {
                            stringResource(
                                R.string.selection_selected_count,
                                selectedEntryIds.size,
                            )
                        }
                    } else {
                        playlist.name
                    },
                    color = backdrop.miuixBarColor(),
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        SelectionNavigationIconAnimatedContent(
                            selectionMode = selectionMode,
                            onCloseSelection = {
                                if (!isDragging) {
                                    selectedEntryIds = emptySet()
                                    selectionMode = false
                                }
                            },
                            defaultNavigationIcon = {
                                Row {
                                    IconButton(onClick = onBack) {
                                        Icon(
                                            imageVector = MiuixIcons.Back,
                                            contentDescription = stringResource(R.string.back),
                                        )
                                    }
                                    ShufflePlayButton(
                                        enabled = displayedTracks.any { item -> item.available } && !isDragging,
                                        onClick = {
                                            onShufflePlay(
                                                displayedTracks.filter { item -> item.available }
                                                    .map { item -> item.track },
                                            )
                                        },
                                    )
                                }
                            },
                        )
                    },
                    actions = {
                        SelectionActionsAnimatedContent(
                            selectionMode = selectionMode,
                            selectionActions = {
                                TrackSelectionActions(
                                    allSelected = displayedEntryIds.isNotEmpty() &&
                                        selectedEntryIds.containsAll(displayedEntryIds),
                                    actionEnabled = selectedEntryIds.isNotEmpty(),
                                    enabled = !isDragging,
                                    removeAction = true,
                                    onToggleAll = {
                                        if (!isDragging) {
                                            selectedEntryIds = toggleAllTrackSelection(
                                                selectedEntryIds,
                                                displayedEntryIds,
                                            )
                                        }
                                    },
                                    onAction = {
                                        if (selectedEntryIds.isNotEmpty()) {
                                            showRemoveSelectedConfirm = true
                                        }
                                    },
                                )
                            },
                            defaultActions = {
                                LibrarySearchButton(
                                    visible = searchVisible,
                                    scrollBehavior = scrollBehavior,
                                    onClick = {
                                        searchVisible = !searchVisible
                                        searchFocused = searchVisible
                                        if (!searchVisible) query = ""
                                    },
                                )
                                PlaylistSortButton(
                                    config = sortConfig,
                                    onConfigChange = onSortConfigChange,
                                )
                                PlaylistActionsButton(
                                    onRename = { showRename = true },
                                    onDelete = { showDelete = true },
                                )
                            },
                        )
                    },
                    bottomContent = {
                        LibrarySearchBar(
                            visible = searchVisible,
                            focused = searchFocused,
                            query = query,
                            label = stringResource(R.string.search_hint),
                            onQueryChange = { query = it },
                            onFocusedChange = { searchFocused = it },
                            onVisibleChange = { visible ->
                                searchVisible = visible
                                if (!visible) {
                                    searchFocused = false
                                    query = ""
                                }
                            },
                        )
                    },
                )
            }
        },
    ) { padding ->
        val indexTopPadding = fixedAlphabetIndexTopPadding()
        val indexBottomPadding = maxOf(
            padding.calculateBottomPadding(),
            bottomContentPadding,
        )
        SideEffect {
            reorderScrollTopPadding = padding.calculateTopPadding()
            reorderScrollBottomPadding = indexBottomPadding
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                state = listState,
                userScrollEnabled = !isDragging,
                contentPadding = PaddingValues(
                    start = padding.calculateStartPadding(layoutDirection),
                    top = padding.calculateTopPadding() + 12.dp,
                    end = padding.calculateEndPadding(layoutDirection),
                    bottom = maxOf(
                        padding.calculateBottomPadding(),
                        bottomContentPadding,
                    ) + 12.dp,
                ),
                overscrollEffect = null,
            ) {
                if (displayedTracks.isEmpty()) {
                    item(key = "empty_playlist_detail") {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            PlaylistEmptyMessage(
                                icon = MiuixIcons.Music,
                                text = stringResource(R.string.playlist_detail_empty),
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = displayedTracks,
                        key = { _, item -> item.entry.id },
                    ) { _, item ->
                        val entryId = item.entry.id
                        val rowContent: @Composable (Modifier) -> Unit = { rowModifier ->
                            MusicTrackRow(
                                track = item.track,
                                isCurrent = item.available && item.track.id == currentTrackId,
                                onClick = {
                                    if (!isDragging) {
                                        if (selectionMode) {
                                            selectedEntryIds = toggleTrackSelection(
                                                selectedEntryIds,
                                                entryId,
                                            )
                                        } else {
                                            resolvePlaylistPlaybackSelection(
                                                displayedTracks = displayedTracks,
                                                selectedEntryId = entryId,
                                            )?.let { (tracks, startIndex) ->
                                                onTrackClick(tracks, startIndex)
                                            }
                                        }
                                    }
                                },
                                onMoreClick = { selectedTrack = item.track },
                                enabled = item.available,
                                moreActionEnabled = item.available,
                                descriptionOverride = if (item.available) {
                                    null
                                } else {
                                    stringResource(R.string.playlist_unavailable)
                                },
                                selectionMode = selectionMode,
                                selected = entryId in selectedEntryIds,
                                onLongClick = {
                                    selectedTrack = null
                                    selectionMode = true
                                    selectedEntryIds += entryId
                                },
                                modifier = rowModifier.fillMaxWidth(),
                            )
                        }
                        if (reorderEnabled) {
                            ReorderableItem(
                                state = playlistReorderState,
                                key = entryId,
                                animateItemModifier = Modifier.animateItem(
                                    fadeInSpec = null,
                                    placementSpec = spring(),
                                    fadeOutSpec = null,
                                ),
                            ) { _ ->
                                rowContent(
                                    Modifier.longPressDraggableHandle(
                                        onDragStarted = {
                                            view.performHapticFeedback(
                                                HapticFeedbackConstants.LONG_PRESS,
                                            )
                                            draggedEntryId = entryId
                                            dragBaselineEntryIds = currentCanonicalEntryIds
                                        },
                                        onDragStopped = { finishDraggedEntry(entryId) },
                                    ),
                                )
                            }
                        } else {
                            rowContent(Modifier)
                        }
                    }
                }
            }

            if (
                displayedTracks.isNotEmpty() &&
                (sortConfig.field == PlaylistSortField.TITLE ||
                    sortConfig.field == PlaylistSortField.FILE_NAME) &&
                query.isBlank()
            ) {
                AlphabetSideBar(
                    sectionIndexMap = sectionIndexMap,
                    itemCount = displayedTracks.size,
                    scrollStateKey = listState,
                    isAtTarget = { targetIndex ->
                        listState.firstVisibleItemIndex == targetIndex &&
                            listState.firstVisibleItemScrollOffset == 0
                    },
                    scrollToItem = listState::scrollToItem,
                    sections = sections,
                    onTargetIndexChanged = { _, restoreLargeTitle ->
                        val topBarState = scrollBehavior.state
                        if (restoreLargeTitle && !scrollBehavior.isPinned) {
                            topBarState.heightOffset = 0f
                            topBarState.contentOffset = 0f
                        } else if (topBarState.heightOffsetLimit != -Float.MAX_VALUE) {
                            topBarState.heightOffset = topBarState.heightOffsetLimit
                            topBarState.contentOffset = topBarState.heightOffsetLimit
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(
                            top = indexTopPadding + 4.dp,
                            end = padding.calculateEndPadding(layoutDirection),
                            bottom = LocalAlphabetIndexBottomPadding.current,
                        )
                        .fillMaxHeight(),
                )
            }
        }
    }

    PlaylistNameDialog(
        show = showRename,
        title = stringResource(R.string.playlist_rename_title),
        initialName = playlist.name,
        onDismiss = { showRename = false },
        onConfirm = { name ->
            onRename(name)
            showRename = false
        },
    )
    TrackActionsOverlay(
        track = selectedTrack,
        onDismiss = { selectedTrack = null },
        onPlayNext = onPlayNext,
        onAppendToQueue = onAppendToQueue,
        onAddToPlaylist = onAddToPlaylist,
        onGoToAlbum = onGoToAlbum,
        artistGroups = artistGroups,
        onGoToArtist = onGoToArtist,
        onExternalEditReturned = onExternalEditReturned,
        showMusicTagEditor = showMusicTagEditor,
        showLyricoEditor = showLyricoEditor,
        showLunaBeatEditor = showLunaBeatEditor,
    )
    OverlayDialog(
        show = showRemoveSelectedConfirm,
        title = stringResource(R.string.playlist_remove_selected_title),
        summary = stringResource(
            R.string.playlist_remove_selected_message,
            selectedEntryIds.size,
        ),
        enableWindowDim = true,
        onDismissRequest = { showRemoveSelectedConfirm = false },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(
                text = stringResource(R.string.playlist_remove_selected_cancel),
                onClick = { showRemoveSelectedConfirm = false },
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(20.dp))
            TextButton(
                text = stringResource(R.string.playlist_remove_selected_confirm),
                onClick = {
                    val entryIds = selectedEntryIds
                    showRemoveSelectedConfirm = false
                    if (entryIds.isNotEmpty()) {
                        onRemoveEntries(entryIds)
                        selectedEntryIds = emptySet()
                        selectionMode = false
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
    OverlayDialog(
        show = showDelete,
        title = stringResource(R.string.playlist_delete_title),
        summary = stringResource(R.string.playlist_delete_message, playlist.name),
        enableWindowDim = true,
        onDismissRequest = { showDelete = false },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(
                text = stringResource(R.string.clear_queue_confirm_cancel),
                onClick = { showDelete = false },
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(20.dp))
            TextButton(
                text = stringResource(R.string.clear_queue_confirm_confirm),
                onClick = {
                    showDelete = false
                    onDelete()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}

@Composable
private fun RowScope.PlaylistActionsButton(
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var showPopup by remember { mutableStateOf(false) }
    Box {
        OverlayListPopup(
            show = showPopup,
            popupPositionProvider = ListPopupDefaults.ContextMenuPositionProvider,
            alignment = PopupPositionProvider.Align.TopEnd,
            onDismissRequest = { showPopup = false },
        ) {
            ListPopupColumn {
                DropdownImpl(
                    text = stringResource(R.string.playlist_rename),
                    optionSize = 2,
                    isSelected = false,
                    index = 0,
                    onSelectedIndexChange = {
                        showPopup = false
                        onRename()
                    },
                )
                DropdownImpl(
                    text = stringResource(R.string.playlist_delete),
                    optionSize = 2,
                    isSelected = false,
                    index = 1,
                    onSelectedIndexChange = {
                        showPopup = false
                        onDelete()
                    },
                )
            }
        }
        IconButton(
            onClick = { showPopup = true },
            holdDownState = showPopup,
        ) {
            Icon(
                imageVector = MiuixIcons.More,
                contentDescription = stringResource(R.string.playlist_more_actions),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
