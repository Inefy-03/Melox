package com.melox.player.ui.screen.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.melox.player.R
import com.melox.player.data.library.ArtistGroup
import com.melox.player.data.library.FolderGroup
import com.melox.player.data.library.MusicSortConfig
import com.melox.player.data.library.MusicSortField
import com.melox.player.data.library.createMusicSortKeys
import com.melox.player.data.library.filterMusicTracks
import com.melox.player.data.library.sortMusicTracks
import com.melox.player.model.MusicTrack
import com.melox.player.model.ScanStatus
import com.melox.player.ui.LibrarySearchBar
import com.melox.player.ui.LibrarySearchButton
import com.melox.player.ui.component.AdaptiveTopAppBar
import com.melox.player.ui.component.BlurredBar
import com.melox.player.ui.component.library.fixedAlphabetIndexTopPadding
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
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import com.melox.player.ui.component.PageScaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

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
) {
    var query by rememberSaveable(folder.key) { mutableStateOf("") }
    var searchVisible by rememberSaveable(folder.key) { mutableStateOf(false) }
    var searchFocused by remember(folder.key) { mutableStateOf(false) }
    var selectedTrackUris by remember(folder.key) {
        mutableStateOf<Set<String>>(emptySet())
    }
    var selectionMode by remember(folder.key) { mutableStateOf(false) }
    var sortFieldOrdinal by rememberSaveable(folder.key) {
        mutableIntStateOf(MusicSortField.TITLE.ordinal)
    }
    var sortDescending by rememberSaveable(folder.key) { mutableStateOf(false) }
    val sortConfig = MusicSortConfig(
        field = MusicSortField.entries.getOrElse(sortFieldOrdinal) {
            MusicSortField.TITLE
        },
        descending = sortDescending,
    )
    val scrollBehavior = rememberSearchTopBarScrollBehavior(searchVisible)
    val listState = rememberLazyListState()
    val backdrop = rememberBlurBackdrop()
    val layoutDirection = LocalLayoutDirection.current
    val displayedTracks = remember(folder.tracks, query, sortConfig) {
        sortMusicTracks(filterMusicTracks(folder.tracks, query), sortConfig)
    }
    val sectionIndexMap = remember(displayedTracks, sortConfig.field) {
        buildMap {
            displayedTracks.forEachIndexed { index, track ->
                val key = when (sortConfig.field) {
                    MusicSortField.ARTIST -> createMusicSortKeys(track.artist).section
                    MusicSortField.FILE_NAME -> createMusicSortKeys(track.fileName).section
                    else -> track.titleSectionKey
                }
                putIfAbsent(key, index)
            }
        }
    }
    val displayedKeys = displayedTracks.map(MusicTrack::contentUri)
    BackHandler(enabled = selectionMode) {
        selectedTrackUris = emptySet()
        selectionMode = false
    }
    LaunchedEffect(selectionMode) {
        onSelectionModeChange(selectionMode)
    }
    LaunchedEffect(selectionExitRequest) {
        if (selectionMode) {
            selectedTrackUris = emptySet()
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
                        if (selectedTrackUris.isEmpty()) {
                            stringResource(R.string.selection_choose_songs)
                        } else {
                            stringResource(
                                R.string.selection_selected_count,
                                selectedTrackUris.size,
                            )
                        }
                    } else {
                        folder.name ?: stringResource(R.string.folder_unknown)
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
                                Row {
                                    IconButton(onClick = onBack) {
                                        Icon(
                                            imageVector = MiuixIcons.Back,
                                            contentDescription = stringResource(R.string.back),
                                        )
                                    }
                                    ShufflePlayButton(
                                        enabled = displayedTracks.isNotEmpty(),
                                        onClick = { onShufflePlay(displayedTracks) },
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
                                searchVisible = searchVisible,
                                sortConfig = sortConfig,
                                onSearchVisibleChange = { visible ->
                                    searchVisible = visible
                                    searchFocused = visible
                                    if (!visible) query = ""
                                },
                                onSortConfigChange = { config ->
                                    sortFieldOrdinal = config.field.ordinal
                                    sortDescending = config.descending
                                },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
        ) {
            MusicListScreen(
                onTrackClick = onTrackClick,
                displayedTracks = displayedTracks,
                sectionIndexMap = sectionIndexMap,
                scanStatus = ScanStatus.Success(folder.tracks.size),
                currentTrackId = currentTrackId,
                query = query,
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
                scrollBehavior = scrollBehavior,
                indexTopPadding = indexTopPadding,
                listState = listState,
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
            )
        }
    }
}

@Composable
private fun RowScope.FolderDetailActions(
    scrollBehavior: ScrollBehavior,
    searchVisible: Boolean,
    sortConfig: MusicSortConfig,
    onSearchVisibleChange: (Boolean) -> Unit,
    onSortConfigChange: (MusicSortConfig) -> Unit,
) {
    LibrarySearchButton(
        visible = searchVisible,
        scrollBehavior = scrollBehavior,
        onClick = { onSearchVisibleChange(!searchVisible) },
    )
    MusicSortButton(
        config = sortConfig,
        onConfigChange = onSortConfigChange,
    )
}
