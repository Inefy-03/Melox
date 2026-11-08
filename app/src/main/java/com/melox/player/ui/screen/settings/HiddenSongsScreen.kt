package com.melox.player.ui.screen.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.melox.player.ui.component.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.melox.player.R
import com.melox.player.data.library.ArtistGroup
import com.melox.player.model.AppSettings
import com.melox.player.model.MusicTrack
import com.melox.player.ui.component.AdaptiveTopAppBar
import com.melox.player.ui.component.BlurredBar
import com.melox.player.ui.component.PageScaffold
import com.melox.player.ui.component.library.MusicTrackRow
import com.melox.player.ui.component.library.SelectionActionsAnimatedContent
import com.melox.player.ui.component.library.SelectionNavigationIconAnimatedContent
import com.melox.player.ui.component.library.TrackActionsOverlay
import com.melox.player.ui.component.library.toggleTrackSelection
import com.melox.player.ui.component.miuixBarColor
import com.melox.player.ui.component.rememberBlurBackdrop
import com.melox.player.ui.screen.library.MusicLibraryEmptyMessage
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import com.melox.player.ui.component.os4.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import com.melox.player.ui.component.os4.navigationBackIcon
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.icon.extended.Reset
import top.yukonga.miuix.kmp.icon.extended.SelectAll
import com.melox.player.ui.component.os4.OverlayDialog
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
internal fun HiddenSongsScreen(
    tracks: List<MusicTrack>,
    settings: AppSettings,
    bottomContentPadding: Dp,
    selectionExitRequest: Int,
    onSelectionModeChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onRestore: (Set<String>) -> Unit,
    onRestoreAll: () -> Unit,
    onPlayNext: (MusicTrack) -> Unit,
    onAppendToQueue: (MusicTrack) -> Unit,
    onAddToPlaylist: (MusicTrack) -> Unit,
    onGoToAlbum: (MusicTrack) -> Unit,
    artistGroups: List<ArtistGroup>,
    onGoToArtist: (ArtistGroup) -> Unit,
    onExternalEditReturned: (Long) -> Unit,
) {
    var backdropCaptureCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val backdrop = rememberBlurBackdrop(captureCoordinates = { backdropCaptureCoordinates })
    val scrollBehavior = MiuixScrollBehavior()
    val layoutDirection = LocalLayoutDirection.current
    val trackUris = remember(tracks) { tracks.map(MusicTrack::contentUri).toSet() }
    var selectedUris by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var restoreAll by remember { mutableStateOf(false) }
    val closeSelection = {
        selectionMode = false
        selectedUris = emptySet()
    }
    BackHandler(enabled = selectionMode, onBack = closeSelection)
    LaunchedEffect(selectionMode) { onSelectionModeChange(selectionMode) }
    LaunchedEffect(selectionExitRequest) { closeSelection() }
    LaunchedEffect(trackUris) {
        selectedUris = selectedUris intersect trackUris
        if (tracks.isEmpty()) closeSelection()
    }
    PageScaffold(
        topBar = {
            BlurredBar(backdrop, backdrop != null, scrollBehavior) {
                AdaptiveTopAppBar(
                    title = if (selectionMode) {
                        if (selectedUris.isEmpty()) stringResource(R.string.selection_choose_songs)
                        else stringResource(R.string.selection_selected_count, selectedUris.size)
                    } else {
                        stringResource(R.string.scan_hidden_songs_title)
                    },
                    color = backdrop.miuixBarColor(),
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        SelectionNavigationIconAnimatedContent(
                            selectionMode = selectionMode,
                            onCloseSelection = closeSelection,
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(navigationBackIcon(), stringResource(R.string.back))
                            }
                        }
                    },
                    actions = {
                        SelectionActionsAnimatedContent(
                            selectionMode = selectionMode,
                            selectionActions = {
                                val allSelected = trackUris.isNotEmpty() && selectedUris == trackUris
                                IconButton(
                                    onClick = { selectedUris = if (allSelected) emptySet() else trackUris },
                                    enabled = trackUris.isNotEmpty(),
                                ) {
                                    Icon(
                                        MiuixIcons.SelectAll,
                                        stringResource(
                                            if (allSelected) R.string.selection_deselect_all
                                            else R.string.selection_select_all,
                                        ),
                                        modifier = Modifier.size(24.dp),
                                        tint = when {
                                            trackUris.isEmpty() -> MiuixTheme.colorScheme.disabledOnSurface
                                            allSelected -> MiuixTheme.colorScheme.primary
                                            else -> LocalContentColor.current
                                        },
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        onRestore(selectedUris intersect trackUris)
                                        closeSelection()
                                    },
                                    enabled = selectedUris.isNotEmpty(),
                                ) {
                                    Icon(
                                        MiuixIcons.Reset,
                                        stringResource(R.string.music_restore_song),
                                        tint = if (selectedUris.isNotEmpty()) LocalContentColor.current
                                        else MiuixTheme.colorScheme.disabledOnSurface,
                                    )
                                }
                            },
                            defaultActions = {
                                IconButton(
                                    onClick = { restoreAll = true },
                                    enabled = tracks.isNotEmpty(),
                                ) {
                                    Icon(
                                        MiuixIcons.Reset,
                                        stringResource(R.string.hidden_songs_restore_all),
                                        tint = if (tracks.isNotEmpty()) LocalContentColor.current
                                        else MiuixTheme.colorScheme.disabledOnSurface,
                                    )
                                }
                            },
                        )
                    },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize()
                .then(backdrop?.let { Modifier.onGloballyPositioned { coordinates -> backdropCaptureCoordinates = coordinates }.layerBackdrop(it) } ?: Modifier),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
                    .scrollEndHaptic().overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    start = padding.calculateStartPadding(layoutDirection),
                    top = padding.calculateTopPadding() + 12.dp,
                    end = padding.calculateEndPadding(layoutDirection),
                    bottom = maxOf(padding.calculateBottomPadding(), bottomContentPadding) + 16.dp,
                ),
                overscrollEffect = null,
            ) {
                if (tracks.isEmpty()) {
                    item {
                        Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            MusicLibraryEmptyMessage(
                                MiuixIcons.Music,
                                stringResource(R.string.hidden_songs_empty),
                            )
                        }
                    }
                } else {
                    items(tracks, key = MusicTrack::contentUri) { track ->
                        MusicTrackRow(
                            track = track,
                            isCurrent = false,
                            onClick = {
                                if (selectionMode) {
                                    selectedUris = toggleTrackSelection(selectedUris, track.contentUri)
                                } else {
                                    selectedTrack = track
                                }
                            },
                            onMoreClick = { selectedTrack = track },
                            selectionMode = selectionMode,
                            selected = track.contentUri in selectedUris,
                            onLongClick = {
                                selectedTrack = null
                                selectionMode = true
                                selectedUris = selectedUris + track.contentUri
                            },
                        )
                    }
                }
            }
        }
    }
    OverlayDialog(
        show = restoreAll,
        title = stringResource(R.string.hidden_songs_restore_all),
        summary = pluralStringResource(
            R.plurals.hidden_songs_restore_all_confirm,
            tracks.size,
            tracks.size,
        ),
        onDismissRequest = { restoreAll = false },
    ) {
        Row(Modifier.fillMaxWidth()) {
            TextButton(
                text = stringResource(R.string.clear_queue_confirm_cancel),
                onClick = { restoreAll = false },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(20.dp))
            TextButton(
                text = stringResource(R.string.clear_queue_confirm_confirm),
                onClick = {
                    onRestoreAll()
                    restoreAll = false
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
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
        showMusicTagEditor = settings.showMusicTagEditor,
        showLyricoEditor = settings.showLyricoEditor,
        showLunaBeatEditor = settings.showLunaBeatEditor,
    )
}
