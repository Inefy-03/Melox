package com.melox.player.ui.screen.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
import com.melox.player.ui.component.MiuixBlurredBar
import com.melox.player.ui.component.library.MusicSortButton
import com.melox.player.ui.component.miuixBarColor
import com.melox.player.ui.component.rememberMiuixBlurBackdrop
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import kotlinx.coroutines.launch

@Composable
fun FolderDetailScreen(
    folder: FolderGroup,
    currentTrackId: Long?,
    blurEnabled: Boolean,
    bottomContentPadding: Dp,
    onBack: () -> Unit,
    onTrackClick: (List<MusicTrack>, Int) -> Unit,
    onPlayNext: (MusicTrack) -> Unit,
    onAppendToQueue: (MusicTrack) -> Unit,
    onGoToAlbum: (MusicTrack) -> Unit,
    artistGroups: List<ArtistGroup>,
    onGoToArtist: (ArtistGroup) -> Unit,
) {
    var query by rememberSaveable(folder.key) { mutableStateOf("") }
    var searchVisible by rememberSaveable(folder.key) { mutableStateOf(false) }
    var searchFocused by remember(folder.key) { mutableStateOf(false) }
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
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    var bottomContentHeightPx by remember { mutableIntStateOf(0) }
    var fixedExpandedBarPadding by remember(folder.key, density) {
        mutableStateOf<Dp?>(null)
    }
    val displayedTracks = remember(folder.tracks, query, sortConfig) {
        sortMusicTracks(filterMusicTracks(folder.tracks, query), sortConfig)
    }
    val sectionIndexMap = remember(displayedTracks, sortConfig.field) {
        buildMap {
            displayedTracks.forEachIndexed { index, track ->
                val key = when (sortConfig.field) {
                    MusicSortField.FILE_NAME -> createMusicSortKeys(track.fileName).section
                    else -> track.titleSectionKey
                }
                putIfAbsent(key, index)
            }
        }
    }

    Scaffold(
        topBar = {
            MiuixBlurredBar(backdrop) {
                TopAppBar(
                    title = folder.name ?: stringResource(R.string.folder_unknown),
                    color = backdrop.miuixBarColor(),
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                    actions = {
                        FolderDetailActions(
                            searchVisible = searchVisible,
                            sortConfig = sortConfig,
                            onSearchVisibleChange = { visible ->
                                searchVisible = visible
                                searchFocused = visible
                                if (!visible) query = ""
                            },
                            onSortConfigChange = { config ->
                                val changed = config != sortConfig
                                sortFieldOrdinal = config.field.ordinal
                                sortDescending = config.descending
                                if (changed) {
                                    scope.launch {
                                        listState.scrollToItem(0)
                                        scrollBehavior.state.heightOffset = 0f
                                        scrollBehavior.state.contentOffset = 0f
                                    }
                                }
                            },
                        )
                    },
                    bottomContent = {
                        Box(
                            modifier = Modifier.onSizeChanged {
                                bottomContentHeightPx = it.height
                            },
                        ) {
                            LibrarySearchBar(
                                visible = searchVisible,
                                focused = searchFocused,
                                query = query,
                                label = stringResource(R.string.music_search_hint),
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
                    },
                )
            }
        },
    ) { padding ->
        val bottomContentHeight = with(density) { bottomContentHeightPx.toDp() }
        val currentBarPadding =
            (padding.calculateTopPadding() - bottomContentHeight).coerceAtLeast(0.dp)
        val heightOffset = with(density) {
            scrollBehavior.state.heightOffset.toDp()
        }
        val measuredExpandedBarPadding =
            (currentBarPadding - heightOffset).coerceAtLeast(currentBarPadding)
        SideEffect {
            if (
                fixedExpandedBarPadding == null &&
                scrollBehavior.state.heightOffsetLimit != -Float.MAX_VALUE
            ) {
                fixedExpandedBarPadding = measuredExpandedBarPadding
            }
        }
        val indexTopPadding =
            (fixedExpandedBarPadding ?: measuredExpandedBarPadding) + bottomContentHeight
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
                onGoToAlbum = onGoToAlbum,
                artistGroups = artistGroups,
                onGoToArtist = onGoToArtist,
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
            )
        }
    }
}

@Composable
private fun RowScope.FolderDetailActions(
    searchVisible: Boolean,
    sortConfig: MusicSortConfig,
    onSearchVisibleChange: (Boolean) -> Unit,
    onSortConfigChange: (MusicSortConfig) -> Unit,
) {
    LibrarySearchButton(
        visible = searchVisible,
        onClick = { onSearchVisibleChange(!searchVisible) },
    )
    MusicSortButton(
        config = sortConfig,
        onConfigChange = onSortConfigChange,
    )
}
