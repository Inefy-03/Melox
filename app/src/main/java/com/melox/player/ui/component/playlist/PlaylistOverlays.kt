package com.melox.player.ui.component.playlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.melox.player.ui.component.bottomSheetCardColor
import com.melox.player.ui.component.bottomSheetGlassModifier
import com.melox.player.ui.component.bottomSheetMaterialColor
import com.melox.player.model.LocalPlaylist
import com.melox.player.model.MusicTrack
import com.melox.player.ui.component.library.PlaybackArtwork
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import com.melox.player.ui.component.os4.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Close
import com.melox.player.ui.component.os4.OverlayBottomSheet
import com.melox.player.ui.component.os4.OverlayDialog
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

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
        .calculateBottomPadding() + 12.dp

    OverlayBottomSheet(
        show = tracks != null,
        modifier = bottomSheetGlassModifier(),
        backgroundColor = bottomSheetMaterialColor(),
        title = stringResource(R.string.playlist_picker_title),
        startAction = {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = MiuixIcons.Close,
                    contentDescription = stringResource(R.string.close),
                )
            }
        },
        endAction = {
            IconButton(
                onClick = {
                    pendingCreateTracks = displayedTracks
                    onCreateRequestDismiss()
                },
            ) {
                Icon(
                    imageVector = MiuixIcons.Add,
                    contentDescription = stringResource(R.string.playlist_create),
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
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState(), overscrollEffect = null)
                .overScrollVertical(),
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
                    colors = CardDefaults.defaultColors(
                        color = bottomSheetCardColor(),
                    ),
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
    var name by remember { mutableStateOf(initialName) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(show, initialName) {
        if (show) name = initialName
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
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            TextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                label = stringResource(R.string.playlist_name_hint),
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    text = stringResource(R.string.clear_queue_confirm_cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(20.dp))
                TextButton(
                    text = stringResource(R.string.clear_queue_confirm_confirm),
                    onClick = { onConfirm(name.trim()) },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}
