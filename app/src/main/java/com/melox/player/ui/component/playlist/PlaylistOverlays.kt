package com.melox.player.ui.component.playlist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.melox.player.R
import com.melox.player.model.LocalPlaylist
import com.melox.player.model.MusicTrack
import com.melox.player.ui.component.library.PlaybackArtwork
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.glass.GlassCard as Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.glass.GlassTextField
import com.melox.player.ui.icon.Add
import com.melox.player.ui.icon.Close
import com.melox.player.ui.icon.GlassIcons
import com.melox.player.ui.component.OverlayBottomSheet
import com.melox.player.ui.component.BottomSheetHeaderButton
import com.melox.player.ui.component.OverlayDialog
import com.melox.player.ui.component.OverlayDialogActions
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.melox.player.ui.component.overScrollVertical

@Composable
fun PlaylistPickerOverlay(
    tracks: List<MusicTrack>?,
    playlists: List<LocalPlaylist>,
    onDismiss: () -> Unit,
    onCreateRequestDismiss: () -> Unit,
    onCreatePlaylist: (List<MusicTrack>) -> Unit,
    onPlaylistSelected: (String, List<MusicTrack>) -> Unit,
) {
    var retainedTracks by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    var retainedPlaylists by remember { mutableStateOf<List<LocalPlaylist>>(emptyList()) }
    var pendingCreateTracks by remember { mutableStateOf<List<MusicTrack>?>(null) }
    LaunchedEffect(tracks) {
        if (tracks != null) retainedTracks = tracks
    }
    LaunchedEffect(playlists) {
        retainedPlaylists = playlists
    }
    val displayedTracks = tracks ?: retainedTracks
    val displayedPlaylists = if (tracks != null) playlists else retainedPlaylists
    val bottomPadding = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding() + 16.dp

    OverlayBottomSheet(
        show = tracks != null,
        title = stringResource(R.string.playlist_picker_title),
        startAction = {
            BottomSheetHeaderButton(onClick = onDismiss) {
                Icon(
                    imageVector = GlassIcons.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = MiuixTheme.colorScheme.onSurface,
                )
            }
        },
        endAction = {
            BottomSheetHeaderButton(
                onClick = {
                    pendingCreateTracks = displayedTracks
                    onCreateRequestDismiss()
                },
            ) {
                Icon(
                    imageVector = GlassIcons.Add,
                    contentDescription = stringResource(R.string.playlist_create),
                    tint = MiuixTheme.colorScheme.onSurface,
                )
            }
        },
        enableWindowDim = true,
        onDismissRequest = onDismiss,
        onDismissFinished = {
            val tracksForCreation = pendingCreateTracks
            pendingCreateTracks = null
            retainedTracks = emptyList()
            retainedPlaylists = emptyList()
            tracksForCreation?.let(onCreatePlaylist)
        },
    ) { headerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState(), overscrollEffect = null)
                .overScrollVertical()
                .padding(headerPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (displayedPlaylists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp + bottomPadding)
                        .padding(bottom = bottomPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.playlist_empty),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = bottomPadding),
                ) {
                    displayedPlaylists.forEach { playlist ->
                        val cover = playlist.entries.firstOrNull()?.trackSnapshot
                        BasicComponent(
                            startAction = {
                                if (cover == null) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .squircleBackground(
                                                color = MiuixTheme.colorScheme.surfaceContainerHighest,
                                                cornerRadius = 6.dp,
                                            ),
                                    )
                                } else {
                                    PlaybackArtwork(
                                        contentUri = cover.contentUri,
                                        dateModifiedEpochSeconds = cover.dateModifiedEpochSeconds,
                                        fileSizeBytes = cover.fileSizeBytes,
                                        size = 44.dp,
                                        cornerRadius = 6.dp,
                                    )
                                }
                            },
                            onClick = {
                                onPlaylistSelected(playlist.id, displayedTracks)
                            },
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = playlist.name,
                                    style = MiuixTheme.textStyles.headline2,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.playlist_song_count,
                                        playlist.entries.size,
                                        playlist.entries.size,
                                    ),
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    maxLines = 1,
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
fun PlaylistNameDialog(
    show: Boolean,
    title: String,
    initialName: String = "",
    requestFocusOnShow: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val name = rememberTextFieldState(initialName)
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(show, initialName) {
        if (show) name.setTextAndPlaceCursorAtEnd(initialName)
    }
    OverlayDialog(
        show = show,
        title = title,
        enableWindowDim = true,
        onDismissRequest = onDismiss,
    ) {
        LaunchedEffect(show, requestFocusOnShow) {
            if (show && requestFocusOnShow) focusRequester.requestFocus()
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassTextField(
                state = name,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                label = stringResource(R.string.playlist_name_hint),
                useLabelAsPlaceholder = true,
                lineLimits = TextFieldLineLimits.SingleLine,
            )
            OverlayDialogActions(
                dismissText = stringResource(R.string.clear_queue_confirm_cancel),
                confirmText = stringResource(R.string.clear_queue_confirm_confirm),
                onDismiss = onDismiss,
                onConfirm = { onConfirm(name.text.toString().trim()) },
                confirmEnabled = name.text.isNotBlank(),
            )
        }
    }
}
