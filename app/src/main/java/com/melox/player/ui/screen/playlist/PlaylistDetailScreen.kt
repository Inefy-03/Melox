package com.melox.player.ui.screen.playlist

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import com.melox.player.ui.component.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.melox.player.ui.component.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.melox.player.R
import com.melox.player.data.library.ArtistGroup
import com.melox.player.data.library.SearchMatch
import com.melox.player.data.library.createMusicSortKeys
import com.melox.player.data.library.rankSearchResults
import com.melox.player.data.library.SongSearchFields
import com.melox.player.data.playlist.PlaylistSortConfig
import com.melox.player.data.playlist.PlaylistSortField
import com.melox.player.data.playlist.canonicalPlaylistEntryOrder
import com.melox.player.data.playlist.resolvePlaylistPlaybackSelection
import com.melox.player.data.playlist.resolvePlaylistTracks
import com.melox.player.data.playlist.sortPlaylistTracks
import com.melox.player.model.LocalPlaylist
import com.melox.player.model.MusicTrack
import com.melox.player.model.ResolvedPlaylistTrack
import com.melox.player.ui.component.library.LibrarySearchBar
import com.melox.player.ui.component.library.LibrarySearchButton
import com.melox.player.ui.component.TopAppBar
import com.melox.player.ui.component.TopBarBackdrop
import com.melox.player.ui.component.LocalGlassSearchQuery
import com.melox.player.ui.component.LocalPageAlphabetIndexPadding
import com.melox.player.ui.component.library.rememberSearchTopBarScrollBehavior
import com.melox.player.ui.component.library.AlphabetSections
import com.melox.player.ui.component.library.AlphabetSideBar
import com.melox.player.ui.component.library.ShufflePlayButton
import com.melox.player.ui.component.library.MusicLocationAction
import com.melox.player.ui.component.library.MusicTrackRow
import com.melox.player.ui.component.library.PreserveSortScrollPosition
import com.melox.player.ui.component.library.ResetSearchResultScroll
import com.melox.player.ui.component.library.LocalSongListFileNameVisible
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
import com.melox.player.ui.component.rememberGlassBlurBackdrop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.upstream.blur.layerBackdrop as glassLayerBackdrop
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import com.melox.player.ui.component.Icon
import com.melox.player.ui.component.IconButton
import com.melox.player.ui.component.PageScaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import com.melox.player.ui.component.navigationBackIcon
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Music
import com.melox.player.ui.component.OverlayDialog
import com.melox.player.ui.component.OverlayDialogActions
import com.melox.player.ui.component.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole

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
    showSaltAudioEditor: Boolean,
    showSaltAudioAnalyzer: Boolean,
    onRename: (String) -> Unit,
    onSetCustomCover: suspend (Uri) -> Boolean,
    onDelete: () -> Unit,
    onRemoveEntries: (Set<String>) -> Unit,
    onMoveEntry: (List<String>) -> Boolean,
    sortConfig: PlaylistSortConfig,
    onSortConfigChange: (PlaylistSortConfig) -> Unit,
    currentTrackContentUri: String? = null,
    miniPlayerBounds: Rect = Rect.Zero,
    playerContentBounds: Rect = Rect.Zero,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val message = if (onSetCustomCover(uri)) {
                    R.string.playlist_custom_cover_success
                } else {
                    R.string.playlist_custom_cover_failed
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val searchDraft = remember(playlist.id) { TextFieldState() }
    var searchVisible by rememberSaveable(playlist.id) { mutableStateOf(false) }
    var searchFocused by remember(playlist.id) { mutableStateOf(false) }
    val scrollBehavior = rememberSearchTopBarScrollBehavior(searchVisible)
    var backdropCaptureCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val backdrop = rememberBlurBackdrop(captureCoordinates = { backdropCaptureCoordinates })
    var locationCaptureCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val locationBackdrop = rememberGlassBlurBackdrop(captureCoordinates = { locationCaptureCoordinates })
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val resolvedTracks = remember(playlist, libraryTracks, readableContentUris) {
        resolvePlaylistTracks(playlist, libraryTracks, readableContentUris)
    }
    val unavailableDescription = stringResource(R.string.playlist_unavailable)
    val searchFields = SongSearchFields(
        showFileName = LocalSongListFileNameVisible.current,
        unknownTitle = stringResource(R.string.music_unknown_title),
    )
    val persistedDisplayedTracks = remember(resolvedTracks, sortConfig) {
        sortPlaylistTracks(resolvedTracks, sortConfig)
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
        selectionMode && sortConfig.field == PlaylistSortField.CUSTOM && !searchVisible
    val currentOnMoveEntry by rememberUpdatedState(onMoveEntry)
    val currentCanonicalEntryIds by rememberUpdatedState(
        playlist.entries.map { entry -> entry.id },
    )
    val currentPersistedDisplayedTracks by rememberUpdatedState(persistedDisplayedTracks)
    val hapticFeedback = LocalHapticFeedback.current
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
    val canShuffle = displayedTracks.any { it.available }
    val currentIndex = displayedTracks.indexOfFirst { item ->
        item.available && item.track.contentUri == currentTrackContentUri
    }
    val showAlphabetIndex = displayedTracks.isNotEmpty() &&
        (sortConfig.field == PlaylistSortField.TITLE ||
            sortConfig.field == PlaylistSortField.FILE_NAME)
    val locateActionVisible = currentIndex >= 0 && !selectionMode &&
        miniPlayerBounds.width > 0f && playerContentBounds.width > 0f
    val locateBottomPadding = if (locateActionVisible) {
        with(density) {
            (playerContentBounds.bottom - miniPlayerBounds.top).coerceAtLeast(0f).toDp()
        } + 12.dp + ButtonDefaults.MinHeight
    } else {
        0.dp
    }
    val displayedEntryIds = displayedTracks.map { item -> item.entry.id }
    PreserveSortScrollPosition(sortConfig, "", displayedTracks.size, listState) {
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
        hasAlphabetIndex = true,
        topBar = {
            TopBarBackdrop(backdrop = backdrop) {
                TopAppBar(
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
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(onClick = onBack) {
                                        Icon(
                                            imageVector = navigationBackIcon(),
                                            contentDescription = stringResource(R.string.back),
                                        )
                                    }
                                    if (canShuffle) {
                                        ShufflePlayButton(
                                            enabled = !isDragging,
                                            onClick = {
                                                onShufflePlay(
                                                    displayedTracks.filter { it.available }.map { it.track },
                                                )
                                            },
                                        )
                                    }
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
                                    },
                                )
                                PlaylistSortButton(
                                    config = sortConfig,
                                    onConfigChange = onSortConfigChange,
                                )
                                PlaylistActionsButton(
                                    onRename = { showRename = true },
                                    onCustomCover = {
                                        coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                    onDelete = { showDelete = true },
                                )
                            },
                        )
                    },
                    bottomContent = {
                        LibrarySearchBar(
                            visible = searchVisible,
                            focused = searchFocused,
                            queryState = searchDraft,
                            label = stringResource(R.string.search_hint),
                            onFocusedChange = { searchFocused = it },
                            onVisibleChange = { visible ->
                                searchVisible = visible
                                if (!visible) {
                                    searchFocused = false
                                }
                            },
                        )
                    },
                )
            }
        },
    ) { padding ->
        val searchQuery = LocalGlassSearchQuery.current
        val selectionAllowed = searchQuery == null
        val selectingTracks = selectionAllowed && selectionMode
        val playbackTracks = if (searchQuery == null) displayedTracks else {
            remember(displayedTracks, searchQuery, searchFields) {
                val keyword = searchQuery.trim()
                if (keyword.isEmpty()) emptyList() else displayedTracks.filter { item ->
                    searchFields.match(item.track, keyword) != SearchMatch.NONE
                }
            }
        }
        val resultTracks = if (searchQuery == null) displayedTracks else {
            remember(playbackTracks, searchQuery, searchFields) {
                playbackTracks.rankSearchResults(searchQuery) { item, keyword -> searchFields.match(item.track, keyword) }
            }
        }
        val contentListState = if (searchQuery == null) listState else {
            val searchListState = rememberLazyListState()
            ResetSearchResultScroll(searchQuery, searchListState)
            searchListState
        }
        val indexBottomPadding = maxOf(
            padding.calculateBottomPadding(),
            bottomContentPadding,
        )
        if (selectionAllowed) {
            SideEffect {
                reorderScrollTopPadding = padding.calculateTopPadding()
                reorderScrollBottomPadding = indexBottomPadding
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(backdrop?.takeIf { selectionAllowed }?.let { Modifier.onGloballyPositioned { coordinates -> backdropCaptureCoordinates = coordinates }.layerBackdrop(it) } ?: Modifier),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .then(locationBackdrop?.takeIf { selectionAllowed }?.let {
                        Modifier.onGloballyPositioned { coordinates -> locationCaptureCoordinates = coordinates }.glassLayerBackdrop(it)
                    } ?: Modifier)
                    .scrollEndHaptic()
                    .overScrollVertical(scrollBehavior = scrollBehavior)
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                state = contentListState,
                userScrollEnabled = !isDragging,
                contentPadding = PaddingValues(
                    start = padding.calculateStartPadding(layoutDirection),
                    top = padding.calculateTopPadding() + 12.dp,
                    end = padding.calculateEndPadding(layoutDirection),
                    bottom = maxOf(
                        padding.calculateBottomPadding(),
                        bottomContentPadding,
                        locateBottomPadding,
                    ) + 12.dp,
                ),
                overscrollEffect = null,
            ) {
                if (resultTracks.isEmpty() && selectionAllowed) {
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
                        items = resultTracks,
                        key = { _, item -> item.entry.id },
                    ) { _, item ->
                        val entryId = item.entry.id
                        val rowContent: @Composable (Modifier) -> Unit = { rowModifier ->
                            MusicTrackRow(
                                track = item.track,
                                isCurrent = item.available && item.track.id == currentTrackId,
                                onClick = {
                                    if (!isDragging) {
                                        if (selectingTracks) {
                                            selectedEntryIds = toggleTrackSelection(
                                                selectedEntryIds,
                                                entryId,
                                            )
                                        } else {
                                            resolvePlaylistPlaybackSelection(
                                                displayedTracks = playbackTracks,
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
                                descriptionOverride = unavailableDescription.takeUnless { item.available },
                                selectionMode = selectingTracks,
                                selected = entryId in selectedEntryIds,
                                onLongClick = if (selectionAllowed) {
                                    {
                                        selectedTrack = null
                                        selectionMode = true
                                        selectedEntryIds += entryId
                                    }
                                } else null,
                                modifier = rowModifier.fillMaxWidth(),
                            )
                        }
                        if (selectionAllowed && reorderEnabled) {
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
                                            hapticFeedback.performHapticFeedback(
                                                HapticFeedbackType.LongPress,
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

            if (selectionAllowed && showAlphabetIndex) {
                AlphabetSideBar(
                    contentPadding = LocalPageAlphabetIndexPadding.current,
                    sectionIndexMap = sectionIndexMap,
                    itemCount = displayedTracks.size,
                    scrollStateKey = contentListState,
                    isAtTarget = { targetIndex ->
                        contentListState.firstVisibleItemIndex == targetIndex &&
                            contentListState.firstVisibleItemScrollOffset == 0
                    },
                    scrollToItem = contentListState::scrollToItem,
                    sections = sections,
                    canScrollBackward = { contentListState.canScrollBackward },
                    scrollBehavior = scrollBehavior,
                    modifier = Modifier
                        .align(Alignment.TopEnd),
                )
            }
            MusicLocationAction(
                visible = selectionAllowed && locateActionVisible,
                currentIndex = currentIndex,
                listState = contentListState,
                scrollBehavior = scrollBehavior,
                miniPlayerBounds = miniPlayerBounds,
                contentPadding = padding,
                backdrop = locationBackdrop,
                alphabetIndexVisible = showAlphabetIndex,
            )
        }
    }

    PlaylistNameDialog(
        show = showRename,
        title = stringResource(R.string.playlist_rename_title),
        initialName = playlist.name,
        requestFocusOnShow = true,
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
        showSaltAudioEditor = showSaltAudioEditor,
        showSaltAudioAnalyzer = showSaltAudioAnalyzer,
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
        OverlayDialogActions(
            dismissText = stringResource(R.string.playlist_remove_selected_cancel),
            confirmText = stringResource(R.string.playlist_remove_selected_confirm),
            onDismiss = { showRemoveSelectedConfirm = false },
            onConfirm = {
                val entryIds = selectedEntryIds
                showRemoveSelectedConfirm = false
                if (entryIds.isNotEmpty()) {
                    onRemoveEntries(entryIds)
                    selectedEntryIds = emptySet()
                    selectionMode = false
                }
            },
            confirmRole = GlassDialogButtonRole.Destructive,
        )
    }
    OverlayDialog(
        show = showDelete,
        title = stringResource(R.string.playlist_delete_title),
        summary = stringResource(R.string.playlist_delete_message, playlist.name),
        enableWindowDim = true,
        onDismissRequest = { showDelete = false },
    ) {
        OverlayDialogActions(
            dismissText = stringResource(R.string.clear_queue_confirm_cancel),
            confirmText = stringResource(R.string.clear_queue_confirm_confirm),
            onDismiss = { showDelete = false },
            onConfirm = {
                showDelete = false
                onDelete()
            },
            confirmRole = GlassDialogButtonRole.Destructive,
        )
    }
}

@Composable
private fun RowScope.PlaylistActionsButton(
    onRename: () -> Unit,
    onCustomCover: () -> Unit,
    onDelete: () -> Unit,
) {
    OverlayIconDropdownMenu(
        entry = DropdownEntry(listOf(
            DropdownItem(text = stringResource(R.string.playlist_rename), onClick = onRename),
            DropdownItem(text = stringResource(R.string.playlist_custom_cover), onClick = onCustomCover),
            DropdownItem(text = stringResource(R.string.playlist_delete), onClick = onDelete),
        )),
    ) {
        Icon(MiuixIcons.More, stringResource(R.string.playlist_more_actions), Modifier.size(24.dp))
    }
}
