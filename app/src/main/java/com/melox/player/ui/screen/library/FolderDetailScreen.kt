package com.melox.player.ui.screen.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.melox.player.ui.icon.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.melox.player.ui.theme.referenceMusicHighlightColors
import com.melox.player.R
import com.melox.player.data.library.ArtistGroup
import com.melox.player.data.library.FolderGroup
import com.melox.player.data.library.SongSearchFields
import com.melox.player.data.library.MusicSortConfig
import com.melox.player.data.library.MusicSortField
import com.melox.player.data.library.createMusicSortKeys
import com.melox.player.data.library.filterMusicTracks
import com.melox.player.data.library.sortMusicTracks
import com.melox.player.model.MusicTrack
import com.melox.player.model.ScanStatus
import com.melox.player.ui.component.library.LibrarySearchBar
import com.melox.player.ui.component.library.LibrarySearchButton
import com.melox.player.ui.component.TopAppBar
import com.melox.player.ui.component.TopBarBackdrop
import com.melox.player.ui.component.library.rememberSearchTopBarScrollBehavior
import com.melox.player.ui.component.library.MusicSortButton
import com.melox.player.ui.component.library.ShufflePlayButton
import com.melox.player.ui.component.library.SelectionActionsAnimatedContent
import com.melox.player.ui.component.library.SelectionNavigationIconAnimatedContent
import com.melox.player.ui.component.library.TrackSelectionActions
import com.melox.player.ui.component.library.selectedItemsInDisplayedOrder
import com.melox.player.ui.component.library.toggleAllTrackSelection
import com.melox.player.ui.component.miuixBarColor
import com.melox.player.ui.component.rememberBlurBackdrop
import com.melox.player.ui.component.Icon
import top.yukonga.miuix.kmp.basic.BreadcrumbBar
import top.yukonga.miuix.kmp.basic.BreadcrumbBarDefaults
import top.yukonga.miuix.kmp.basic.BreadcrumbItem
import com.melox.player.ui.component.IconButton
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import com.melox.player.ui.component.PageScaffold
import com.melox.player.ui.component.LocalGlassSearchResults
import com.melox.player.ui.component.library.LocalSongListFileNameVisible
import top.yukonga.miuix.kmp.blur.layerBackdrop
import com.melox.player.ui.component.navigationBackIcon

@Composable
fun FolderDetailScreen(
    folder: FolderGroup,
    currentTrackId: Long?,
    bottomContentPadding: Dp,
    onBack: () -> Unit,
    selectionExitRequest: Int,
    onSelectionModeChange: (Boolean) -> Unit,
    onTrackClick: (List<MusicTrack>, Int) -> Unit,
    onShufflePlay: (List<MusicTrack>) -> Unit,
    onPlayNext: (MusicTrack) -> Unit,
    onAppendToQueue: (MusicTrack) -> Unit,
    onAddToPlaylist: (MusicTrack) -> Unit,
    onAddTracksToPlaylist: (List<MusicTrack>, () -> Unit) -> Unit,
    onGoToAlbum: (MusicTrack) -> Unit,
    artistGroups: List<ArtistGroup>,
    onGoToArtist: (ArtistGroup) -> Unit,
    onExternalEditReturned: (Long) -> Unit,
    showMusicTagEditor: Boolean,
    showLyricoEditor: Boolean,
    showLunaBeatEditor: Boolean,
    sortConfig: MusicSortConfig,
    onSortConfigChange: (MusicSortConfig) -> Unit,
    currentTrackContentUri: String? = null,
    miniPlayerBounds: Rect = Rect.Zero,
    playerContentBounds: Rect = Rect.Zero,
    childFolders: List<FolderGroup> = emptyList(),
    childSongCounts: Map<String, Int> = emptyMap(),
    breadcrumbItems: List<BreadcrumbItem> = emptyList(),
    titleOverride: String? = null,
    onDirectoryClick: (String) -> Unit = {},
    onBreadcrumbClick: (Int) -> Unit = {},
    onBlockFolder: (String) -> Unit = {},
    scanStatus: ScanStatus = ScanStatus.Success(folder.tracks.size),
) {
    var query by rememberSaveable(folder.key) { mutableStateOf("") }
    var searchVisible by rememberSaveable(folder.key) { mutableStateOf(false) }
    var searchFocused by remember(folder.key) { mutableStateOf(false) }
    val canSearch = folder.tracks.isNotEmpty()
    LaunchedEffect(canSearch) {
        if (!canSearch) {
            searchVisible = false
            searchFocused = false
            query = ""
        }
    }
    val searchFields = SongSearchFields(
        showFileName = LocalSongListFileNameVisible.current,
        unknownTitle = stringResource(R.string.music_unknown_title),
    )
    var selectedTrackUris by remember(folder.key) {
        mutableStateOf<Set<String>>(emptySet())
    }
    var selectionMode by remember(folder.key) { mutableStateOf(false) }
    val onDirectorySearchBack: () -> Unit = {
        if (query.isNotEmpty()) {
            query = ""
        } else {
            searchVisible = false
            searchFocused = false
        }
    }
    val handleBack: () -> Unit = {
        if (breadcrumbItems.isNotEmpty() && searchVisible) onDirectorySearchBack() else onBack()
    }
    val scrollBehavior = rememberSearchTopBarScrollBehavior(searchVisible)
    val directoryStateHolder = rememberSaveableStateHolder()
    val breadcrumbListState = remember(folder.key) { LazyListState() }
    val showSongActions = breadcrumbItems.isEmpty() || folder.tracks.isNotEmpty()
    var backdropCaptureCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val backdrop = rememberBlurBackdrop(captureCoordinates = { backdropCaptureCoordinates })
    val layoutDirection = LocalLayoutDirection.current
    val sortedTracks = remember(folder.tracks, sortConfig) { sortMusicTracks(folder.tracks, sortConfig) }
    val displayedTracks = remember(sortedTracks, query, searchFields, canSearch) {
        if (canSearch) filterMusicTracks(sortedTracks, query, searchFields) else sortedTracks
    }
    val sectionIndexMap = remember(sortedTracks, sortConfig.field) {
        buildMap {
            sortedTracks.forEachIndexed { index, track ->
                val key = when (sortConfig.field) {
                    MusicSortField.ARTIST -> createMusicSortKeys(track.artist).section
                    MusicSortField.FILE_NAME -> createMusicSortKeys(track.fileName).section
                    else -> track.titleSectionKey
                }
                putIfAbsent(key, index)
            }
        }
    }
    var pendingBlockFolder by remember(folder.key) { mutableStateOf<FolderGroup?>(null) }
    val displayedKeys = displayedTracks.map(MusicTrack::contentUri)
    BackHandler(enabled = selectionMode) {
        selectedTrackUris = emptySet()
        selectionMode = false
    }
    LaunchedEffect(folder.key, selectionMode) {
        onSelectionModeChange(selectionMode)
    }
    LaunchedEffect(selectionExitRequest) {
        if (selectionMode) {
            selectedTrackUris = emptySet()
            selectionMode = false
        }
    }

    PageScaffold(
        hasAlphabetIndex = true,
            topBar = {
            TopBarBackdrop(backdrop = backdrop) {
                TopAppBar(
                    title = if (selectionMode) {
                        if (selectedTrackUris.isEmpty()) {
                            stringResource(R.string.selection_choose_songs)
                        } else {
                            stringResource(
                                R.string.selection_selected_count,
                                selectedTrackUris.size,
                            )
                        }
                    } else {
                        titleOverride ?: folder.name ?: stringResource(R.string.folder_unknown)
                    },
                    color = backdrop.miuixBarColor(),
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        SelectionNavigationIconAnimatedContent(
                            selectionMode = selectionMode,
                            onCloseSelection = {
                                selectedTrackUris = emptySet()
                                selectionMode = false
                            },
                            defaultNavigationIcon = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(onClick = handleBack) {
                                        Icon(
                                            imageVector = navigationBackIcon(),
                                            contentDescription = stringResource(R.string.back),
                                        )
                                    }
                                    if (showSongActions) {
                                        ShufflePlayButton(
                                            enabled = displayedTracks.isNotEmpty(),
                                            onClick = { onShufflePlay(displayedTracks) },
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
                                    allSelected = displayedKeys.isNotEmpty() &&
                                        selectedTrackUris.containsAll(displayedKeys),
                                    actionEnabled = selectedTrackUris.isNotEmpty(),
                                    onToggleAll = {
                                        selectedTrackUris = toggleAllTrackSelection(
                                            selectedTrackUris,
                                            displayedKeys,
                                        )
                                    },
                                    onAction = {
                                        val tracks = selectedItemsInDisplayedOrder(
                                            displayedTracks,
                                            selectedTrackUris,
                                            MusicTrack::contentUri,
                                        )
                                        if (tracks.isNotEmpty()) {
                                            onAddTracksToPlaylist(tracks) {
                                                selectedTrackUris = emptySet()
                                                selectionMode = false
                                            }
                                        }
                                    },
                                )
                            },
                            defaultActions = {
                                FolderDetailActions(
                                    scrollBehavior = scrollBehavior,
                                    showSongSort = showSongActions,
                                    showSearch = canSearch,
                                    searchVisible = searchVisible,
                                    sortConfig = sortConfig,
                                    onSearchVisibleChange = { visible ->
                                        searchVisible = visible
                                        searchFocused = visible
                                        if (!visible) query = ""
                                    },
                                    onSortConfigChange = onSortConfigChange,
                                )
                            },
                        )
                    },
                    bottomContent = {
                        Column {
                            if (canSearch) {
                                LibrarySearchBar(
                                    visible = searchVisible,
                                    focused = searchFocused,
                                    query = query,
                                    label = stringResource(R.string.search_hint),
                                    onBackRequest = onDirectorySearchBack.takeIf { breadcrumbItems.isNotEmpty() },
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
                            }
                            if (breadcrumbItems.isNotEmpty()) {
                                // BreadcrumbBar 0.9.4 reads body2 directly; limit the override to it.
                                MiuixTheme(textStyles = MiuixTheme.textStyles.copy(
                                    body2 = MiuixTheme.textStyles.footnote2,
                                )) {
                                    val colors = MiuixTheme.colorScheme
                                    val isDark = colors.surface.luminance() < 0.5f
                                    val highlightColors = if (MiuixTheme.isDynamicColor) {
                                        colors.primary to colors.primary.copy(alpha = if (isDark) 0.2f else 0.15f)
                                    } else {
                                        referenceMusicHighlightColors(isDark)
                                    }
                                    BreadcrumbBar(
                                        items = breadcrumbItems,
                                        onItemClick = onBreadcrumbClick,
                                        enabled = !selectionMode,
                                        colors = BreadcrumbBarDefaults.breadcrumbBarColors(
                                            color = if (isDark) Color(0xFF7B7B7B) else Color(0xFF8E94AA),
                                            backgroundColor = if (isDark) Color(0xFF262626) else Color(0xFFF3F4F8),
                                            separatorColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFD9D9D9),
                                            highlightColor = highlightColors.first,
                                            highlightBackgroundColor = highlightColors.second,
                                        ),
                                        insideMargin = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    )
                                }
                            }
                        }
                    },
                )
            }
        },
        ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(backdrop?.let { Modifier.onGloballyPositioned { coordinates -> backdropCaptureCoordinates = coordinates }.layerBackdrop(it) } ?: Modifier),
        ) {
            val showingResults = LocalGlassSearchResults.current
            val displayedChildFolders = if (showingResults) emptyList() else childFolders
            val listContent: @Composable (LazyListState) -> Unit = { listState ->
                MusicListScreen(
                    showLocateAction = true,
                    currentTrackContentUri = currentTrackContentUri,
                    miniPlayerBounds = miniPlayerBounds,
                    playerContentBounds = playerContentBounds,
                    onTrackClick = onTrackClick,
                    displayedTracks = if (showingResults) displayedTracks else sortedTracks,
                    sectionIndexMap = if (showingResults) emptyMap() else sectionIndexMap,
                    scanStatus = scanStatus,
                    currentTrackId = currentTrackId,
                    query = if (showingResults) query else "",
                    sortConfig = sortConfig,
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
                    scrollBehavior = scrollBehavior,
                    listState = listState,
                    topContentSpacing = if (breadcrumbItems.isNotEmpty()) 4.dp else 12.dp,
                    contentPadding = PaddingValues(
                        start = padding.calculateStartPadding(layoutDirection),
                        top = padding.calculateTopPadding(),
                        end = padding.calculateEndPadding(layoutDirection),
                        bottom = maxOf(
                            padding.calculateBottomPadding(),
                            bottomContentPadding,
                        ),
                    ),
                    selectionMode = selectionMode,
                    selectedTrackUris = selectedTrackUris,
                    onSelectionChange = { selectedTrackUris = it },
                    onSelectionModeChange = { selectionMode = it },
                    emptyContent = if (breadcrumbItems.isNotEmpty()) {
                        { emptyModifier ->
                            MusicLibraryEmptyState(
                                scanStatus = scanStatus,
                                query = query,
                                emptyMessageRes = R.string.folder_empty,
                                noSearchResultsRes = R.string.folder_no_search_results,
                                icon = GlassIcons.FolderFill,
                                modifier = emptyModifier,
                            )
                        }
                    } else null,
                    leadingItemCount = displayedChildFolders.size,
                    leadingContent = {
                        items(displayedChildFolders, key = { "directory:${it.key}" }) { child ->
                            FolderListItem(
                                folder = child,
                                songCount = childSongCounts[child.key] ?: child.tracks.size,
                                onClick = {
                                    selectedTrackUris = emptySet()
                                    selectionMode = false
                                    onDirectoryClick(child.key)
                                },
                                onLongClick = { pendingBlockFolder = child },
                            )
                        }
                    },
                )
            }
            // The search overlay is a separate composition and may overlap the directory's disposal.
            if (showingResults) {
                listContent(rememberLazyListState())
            } else {
                directoryStateHolder.SaveableStateProvider(folder.key) {
                    listContent(if (breadcrumbItems.isNotEmpty()) breadcrumbListState else rememberLazyListState())
                }
            }
        }
    }
    FolderHideDialog(
        folder = pendingBlockFolder,
        onDismiss = { pendingBlockFolder = null },
        onBlockFolder = onBlockFolder,
    )
}

@Composable
private fun RowScope.FolderDetailActions(
    scrollBehavior: ScrollBehavior,
    showSongSort: Boolean,
    showSearch: Boolean,
    searchVisible: Boolean,
    sortConfig: MusicSortConfig,
    onSearchVisibleChange: (Boolean) -> Unit,
    onSortConfigChange: (MusicSortConfig) -> Unit,
) {
    if (showSearch) {
        LibrarySearchButton(
            visible = searchVisible,
            scrollBehavior = scrollBehavior,
            onClick = { onSearchVisibleChange(!searchVisible) },
        )
    }
    if (showSongSort) {
        MusicSortButton(
            config = sortConfig,
            onConfigChange = onSortConfigChange,
        )
    }
}
