package com.melox.player.ui.screen.playlist

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import com.melox.player.ui.component.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.melox.player.R
import com.melox.player.model.LocalPlaylist
import com.melox.player.ui.component.AdaptiveTopAppBar
import com.melox.player.ui.component.PageScaffold
import com.melox.player.ui.component.BlurredBar
import com.melox.player.ui.component.miuixBarColor
import com.melox.player.ui.component.playlist.PlaylistGridItem
import com.melox.player.ui.component.rememberBlurBackdrop
import com.melox.player.ui.screen.home.homePlaylistGridColumnCount
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import com.melox.player.ui.component.os4.Icon
import com.melox.player.ui.component.os4.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import com.melox.player.ui.component.os4.navigationBackIcon
import top.yukonga.miuix.kmp.icon.extended.RecordingTape
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.melox.player.ui.component.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun PlaylistLibraryScreen(
    playlists: List<LocalPlaylist>,
    loaded: Boolean,
    landscape: Boolean,
    bottomContentPadding: Dp,
    onBack: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onPlaylistClick: (LocalPlaylist) -> Unit,
    onMovePlaylists: (List<String>) -> Boolean,
) {
    val scrollBehavior = MiuixScrollBehavior()
    var backdropCaptureCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val backdrop = rememberBlurBackdrop(captureCoordinates = { backdropCaptureCoordinates })
    val layoutDirection = LocalLayoutDirection.current
    val gridState = rememberLazyGridState()
    var draftPlaylists by remember { mutableStateOf(playlists) }
    var draggedPlaylistId by remember { mutableStateOf<String?>(null) }
    var baselineIds by remember { mutableStateOf<List<String>?>(null) }
    val currentPlaylists by rememberUpdatedState(playlists)
    val currentOnMovePlaylists by rememberUpdatedState(onMovePlaylists)
    val view = LocalView.current
    val hapticFeedback = LocalHapticFeedback.current
    val moveEarlierLabel = stringResource(R.string.playlist_move_earlier)
    val moveLaterLabel = stringResource(R.string.playlist_move_later)

    LaunchedEffect(playlists, draggedPlaylistId) {
        if (draggedPlaylistId == null) draftPlaylists = playlists
    }

    fun finishDrag(playlistId: String) {
        if (draggedPlaylistId != playlistId) return
        val orderedIds = draftPlaylists.map(LocalPlaylist::id)
        val sourceUnchanged = baselineIds == currentPlaylists.map(LocalPlaylist::id)
        val changed = orderedIds != baselineIds
        val accepted = sourceUnchanged && (!changed || currentOnMovePlaylists(orderedIds))
        if (accepted && changed) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
        } else {
            draftPlaylists = currentPlaylists
        }
        draggedPlaylistId = null
        baselineIds = null
    }

    PageScaffold(
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                blurEnabled = backdrop != null,
                scrollBehavior = scrollBehavior,
            ) {
                AdaptiveTopAppBar(
                    title = stringResource(R.string.home_playlists_title),
                    color = backdrop.miuixBarColor(),
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = { if (draggedPlaylistId == null) onBack() }) {
                            Icon(
                                imageVector = navigationBackIcon(),
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { if (draggedPlaylistId == null) onCreatePlaylist() }) {
                            Icon(
                                imageVector = MiuixIcons.Add,
                                contentDescription = stringResource(R.string.playlist_create),
                            )
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
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val gridContentPadding = PaddingValues(
                    start = padding.calculateStartPadding(layoutDirection) + 16.dp,
                    top = padding.calculateTopPadding() + 12.dp,
                    end = padding.calculateEndPadding(layoutDirection) + 16.dp,
                    bottom = maxOf(
                        padding.calculateBottomPadding(),
                        bottomContentPadding,
                    ) + 12.dp,
                )
                val reorderState = rememberReorderableLazyGridState(
                    lazyGridState = gridState,
                    scrollThresholdPadding = gridContentPadding,
                ) { from, to ->
                    val fromIndex = draftPlaylists.indexOfFirst { it.id == from.key }
                    val toIndex = draftPlaylists.indexOfFirst { it.id == to.key }
                    if (fromIndex in draftPlaylists.indices && toIndex in draftPlaylists.indices) {
                        draftPlaylists = draftPlaylists.toMutableList().apply {
                            add(toIndex, removeAt(fromIndex))
                        }
                    }
                }
                BackHandler(enabled = reorderState.isAnyItemDragging) {}
                LazyVerticalGrid(
                    state = gridState,
                    userScrollEnabled = !reorderState.isAnyItemDragging,
                    columns = GridCells.Fixed(
                        homePlaylistGridColumnCount(
                            landscape = landscape,
                            availableWidth = maxWidth,
                        ),
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .scrollEndHaptic()
                        .overScrollVertical()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = gridContentPadding,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                    overscrollEffect = null,
                ) {
                    if (!loaded) {
                        item(
                            key = "playlist_library_loading",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                InfiniteProgressIndicator(color = MiuixTheme.colorScheme.onSurface)
                            }
                        }
                    } else if (playlists.isEmpty()) {
                        item(
                            key = "empty_playlist_library",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(
                                        (
                                            maxHeight -
                                                gridContentPadding.calculateTopPadding() -
                                                gridContentPadding.calculateBottomPadding()
                                        ).coerceAtLeast(1.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                PlaylistEmptyMessage(
                                    icon = MiuixIcons.RecordingTape,
                                    text = stringResource(R.string.playlist_empty),
                                )
                            }
                        }
                    } else {
                        items(
                            items = draftPlaylists,
                            key = LocalPlaylist::id,
                        ) { playlist ->
                            ReorderableItem(state = reorderState, key = playlist.id) { _ ->
                                fun moveBy(offset: Int): Boolean {
                                    if (draggedPlaylistId != null) return false
                                    val source = currentPlaylists
                                    val index = source.indexOfFirst { it.id == playlist.id }
                                    val target = index + offset
                                    if (index !in source.indices || target !in source.indices) return false
                                    val ordered = source.toMutableList().apply {
                                        add(target, removeAt(index))
                                    }
                                    return currentOnMovePlaylists(ordered.map(LocalPlaylist::id))
                                }
                                PlaylistGridItem(
                                    playlist = playlist,
                                    showEmptyArtworkIcon = false,
                                    onClick = {
                                        if (draggedPlaylistId == null) onPlaylistClick(playlist)
                                    },
                                    modifier = Modifier
                                        .longPressDraggableHandle(
                                            onDragStarted = {
                                                view.performHapticFeedback(
                                                    HapticFeedbackConstants.LONG_PRESS,
                                                )
                                                draggedPlaylistId = playlist.id
                                                baselineIds = currentPlaylists.map(LocalPlaylist::id)
                                            },
                                            onDragStopped = { finishDrag(playlist.id) },
                                        )
                                        .semantics {
                                            customActions = buildList {
                                                val index = draftPlaylists.indexOfFirst { it.id == playlist.id }
                                                if (index > 0) {
                                                    add(CustomAccessibilityAction(moveEarlierLabel) { moveBy(-1) })
                                                }
                                                if (index < draftPlaylists.lastIndex) {
                                                    add(CustomAccessibilityAction(moveLaterLabel) { moveBy(1) })
                                                }
                                            }
                                        },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PlaylistEmptyMessage(
    icon: ImageVector,
    text: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.disabledOnSecondaryVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = text,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}
