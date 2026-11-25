package com.melox.player.ui.component.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.melox.player.R
import com.melox.player.data.library.ArtistGroup
import com.melox.player.data.library.artistGroupKey
import com.melox.player.data.library.displayArtistName
import com.melox.player.data.library.folderDisplayPath
import com.melox.player.data.library.splitArtistNames
import com.melox.player.model.MusicTrack
import java.util.Locale
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.glass.GlassCard as Card
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import com.melox.player.ui.component.Icon
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import com.melox.player.ui.icon.*
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Reset
import com.melox.player.ui.component.OverlayBottomSheet
import com.melox.player.ui.component.BottomSheetHeaderButton
import com.melox.player.ui.component.OverlayDialog
import com.melox.player.ui.component.OverlayDialogActions
import com.melox.player.ui.component.OverlayDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.melox.player.ui.component.overScrollVertical
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole

private val TrackActionIconSize = 24.dp
private val TrackActionSummaryArtworkSize = 56.dp
private val TrackActionSummaryArtworkCornerRadius = 8.dp
@Composable
fun TrackActionsOverlay(
    track: MusicTrack?,
    onDismiss: () -> Unit,
    onPlayNext: (MusicTrack) -> Unit,
    onAppendToQueue: (MusicTrack) -> Unit,
    onAddToPlaylist: (MusicTrack) -> Unit,
    onGoToAlbum: ((MusicTrack) -> Unit)? = null,
    artistGroups: List<ArtistGroup> = emptyList(),
    onGoToArtist: ((ArtistGroup) -> Unit)? = null,
    onExternalEditReturned: (Long) -> Unit,
    showMusicTagEditor: Boolean,
    showLyricoEditor: Boolean,
    showLunaBeatEditor: Boolean,
    showSaltAudioEditor: Boolean,
    showSaltAudioAnalyzer: Boolean,
) {
    val context = LocalContext.current
    val mutations = LocalTrackMutationActions.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnExternalEditReturned by rememberUpdatedState(onExternalEditReturned)
    val externalEditorUnavailableMessage =
        stringResource(R.string.music_external_editor_unavailable)
    val musicTagEditorNotFoundMessage =
        stringResource(R.string.music_tag_editor_not_found)
    val lyricoNotFoundMessage = stringResource(R.string.lyrico_not_found)
    val lunaBeatNotFoundMessage = stringResource(R.string.luna_beat_not_found)
    val shareTitle = stringResource(R.string.music_share)
    var retainedTrack by remember { mutableStateOf(track) }
    var mutationTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var deleteRequested by remember { mutableStateOf(false) }
    var showMutationConfirm by remember { mutableStateOf(false) }
    var artworkPreview by remember { mutableStateOf<ArtworkPreviewRequest?>(null) }
    var artworkPreviewPresented by remember { mutableStateOf(false) }
    var songInfoTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var retainedSongInfoTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var selectedArtists by remember { mutableStateOf(emptyList<ArtistGroup>()) }
    val audioActions = visibleExternalAudioActions(
        showMusicTagEditor,
        showLyricoEditor,
        showLunaBeatEditor,
        showSaltAudioEditor,
        showSaltAudioAnalyzer,
    )
    val openSaltAudioEditor = rememberSaltAudioEditorLauncher(
        onLaunched = onDismiss,
        onOriginalSaved = onExternalEditReturned,
    )
    var pendingExternalEditTrackId by rememberSaveable { mutableStateOf<Long?>(null) }
    val finishExternalEdit = {
        pendingExternalEditTrackId?.let { trackId ->
            pendingExternalEditTrackId = null
            latestOnExternalEditReturned(trackId)
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                finishExternalEdit()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    val openExternalEditor: (MusicTrack, ExternalAudioAction) -> Unit =
        open@{ selectedTrack, editorKind ->
            val notFoundMessage = when (editorKind) {
                ExternalAudioAction.MusicTagEditor -> musicTagEditorNotFoundMessage
                ExternalAudioAction.Lyrico -> lyricoNotFoundMessage
                ExternalAudioAction.LunaBeat -> lunaBeatNotFoundMessage
                ExternalAudioAction.SaltAudioEditor -> {
                    openSaltAudioEditor(selectedTrack)
                    return@open
                }
                ExternalAudioAction.SaltAudioAnalyzer -> {
                    if (launchSaltAudioAnalysis(context, selectedTrack)) {
                        onDismiss()
                    } else {
                        val message = if (context.hasExternalEditor(editorKind)) {
                            R.string.salt_audio_analysis_unavailable
                        } else {
                            R.string.salt_audio_analyzer_not_found
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                    return@open
                }
            }
            val launched = runCatching {
                launchExternalEditor(
                    context = context,
                    track = selectedTrack,
                    kind = editorKind,
                )
            }.getOrDefault(false)
            if (launched) {
                pendingExternalEditTrackId = selectedTrack.id
                onDismiss()
            } else {
                Toast.makeText(
                    context,
                    if (context.hasExternalEditor(editorKind)) {
                        externalEditorUnavailableMessage
                    } else {
                        notFoundMessage
                    },
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    val navigationBarBottomPadding = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    LaunchedEffect(track?.id) {
        artworkPreview = null
        artworkPreviewPresented = false
        if (track != null) {
            retainedTrack = track
            mutationTrack = null
            showMutationConfirm = false
            songInfoTrack = null
            retainedSongInfoTrack = null
            selectedArtists = emptyList()
        }
    }
    LaunchedEffect(songInfoTrack?.id) {
        if (songInfoTrack != null) {
            retainedSongInfoTrack = songInfoTrack
        }
    }
    OverlayBottomSheet(
        show = track != null,
        allowDismiss = artworkPreview == null,
        enableWindowDim = true,
        startAction = {
            (track ?: retainedTrack)?.let { selectedTrack ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-6).dp),
                ) {
                    MusicTrackSummary(
                        track = selectedTrack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 12.dp,
                                top = 12.dp,
                                end = 16.dp,
                                bottom = 12.dp,
                            ),
                        artworkSize = TrackActionSummaryArtworkSize,
                        artworkCornerRadius = TrackActionSummaryArtworkCornerRadius,
                        artworkContent = {
                            PreviewableTrackArtwork(
                                track = selectedTrack,
                                size = TrackActionSummaryArtworkSize,
                                cornerRadius = TrackActionSummaryArtworkCornerRadius,
                                hidden = artworkPreviewPresented,
                                onPreview = { artworkPreview = it },
                            )
                        },
                    )
                }
            }
        },
        onDismissRequest = onDismiss,
        onDismissFinished = {
            retainedTrack = null
            if (mutationTrack != null) showMutationConfirm = true
        },
    ) { headerPadding ->
        (track ?: retainedTrack)?.let { selectedTrack ->
            SheetScrollableContent(
                headerPadding = headerPadding,
                bottomPadding = navigationBarBottomPadding + 16.dp,
                headerTopReduction = 12.dp,
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                ) {
                    TrackAction(
                        icon = GlassIcons.Add,
                        text = stringResource(R.string.music_play_next),
                        enabled = selectedTrack.contentUri !in mutations?.hiddenContentUris.orEmpty(),
                        onClick = {
                            onPlayNext(selectedTrack)
                            onDismiss()
                        },
                    )
                    TrackAction(
                        icon = GlassIcons.AddCircle,
                        text = stringResource(R.string.music_add_to_queue),
                        enabled = selectedTrack.contentUri !in mutations?.hiddenContentUris.orEmpty(),
                        onClick = {
                            onAppendToQueue(selectedTrack)
                            onDismiss()
                        },
                    )
                    TrackAction(
                        icon = GlassIcons.CopyEditPoint,
                        text = stringResource(R.string.playlist_add_to),
                        onClick = {
                            onAddToPlaylist(selectedTrack)
                            onDismiss()
                        },
                    )
                    onGoToAlbum?.let { goToAlbum ->
                        selectedTrack.album?.let { albumName ->
                            TrackAction(
                                icon = GlassIcons.Item,
                                text = stringResource(
                                    R.string.music_album_label,
                                    albumName,
                                ),
                                truncateText = true,
                                onClick = {
                                    goToAlbum(selectedTrack)
                                    onDismiss()
                                },
                            )
                        }
                    }
                    onGoToArtist?.let { goToArtist ->
                        val selectedArtistGroups = participatingArtistGroups(
                            track = selectedTrack,
                            artistGroups = artistGroups,
                        )
                        displayArtistName(selectedTrack.artist)?.takeIf {
                            selectedArtistGroups.isNotEmpty()
                        }?.let { artistName ->
                            TrackAction(
                                icon = GlassIcons.Contacts,
                                text = stringResource(
                                    R.string.music_artist_label,
                                    artistName,
                                ),
                                truncateText = true,
                                onClick = {
                                    if (selectedArtistGroups.size == 1) {
                                        goToArtist(selectedArtistGroups.single())
                                        onDismiss()
                                    } else {
                                        selectedArtists = selectedArtistGroups
                                        onDismiss()
                                    }
                                },
                            )
                        }
                    }
                    val directAction = audioActions.singleOrNull()
                    if (directAction != null) {
                        val titleRes = when (directAction) {
                            ExternalAudioAction.MusicTagEditor -> R.string.music_edit_with_music_tag_editor
                            ExternalAudioAction.Lyrico -> R.string.music_edit_with_lyrico
                            ExternalAudioAction.LunaBeat -> R.string.music_edit_with_luna_beat
                            ExternalAudioAction.SaltAudioEditor -> R.string.lyrics_editor_salt_audio_editor
                            ExternalAudioAction.SaltAudioAnalyzer -> R.string.lyrics_editor_salt_audio_analyzer
                        }
                        TrackAction(
                            icon = {
                                MiuixIcon(
                                    imageVector = ImageVector.vectorResource(directAction.iconRes),
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                )
                            },
                            text = stringResource(titleRes),
                            onClick = { openExternalEditor(selectedTrack, directAction) },
                        )
                    } else if (audioActions.isNotEmpty()) {
                        OverlayDropdownMenu(
                            title = stringResource(R.string.music_edit_metadata),
                            itemIcons = audioActions.map { ImageVector.vectorResource(it.iconRes) },
                            modifier = Modifier.fillMaxWidth(),
                            startAction = {
                                Icon(
                                    imageVector = GlassIcons.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(TrackActionIconSize),
                                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                )
                            },
                            entry = DropdownEntry(
                                items = audioActions.map { editor ->
                                    DropdownItem(
                                        text = stringResource(
                                            when (editor) {
                                                ExternalAudioAction.MusicTagEditor -> R.string.lyrics_editor_music_tag
                                                ExternalAudioAction.Lyrico -> R.string.lyrics_editor_lyrico
                                                ExternalAudioAction.LunaBeat -> R.string.lyrics_editor_luna_beat
                                                ExternalAudioAction.SaltAudioEditor -> R.string.lyrics_editor_salt_audio_editor
                                                ExternalAudioAction.SaltAudioAnalyzer -> R.string.lyrics_editor_salt_audio_analyzer
                                            },
                                        ),
                                        onClick = { openExternalEditor(selectedTrack, editor) },
                                    )
                                },
                            ),
                        )
                    }
                    TrackAction(
                        icon = {
                            Icon(
                                imageVector = GlassIcons.Share,
                                contentDescription = null,
                                modifier = Modifier.size(TrackActionIconSize),
                                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            )
                        },
                        text = shareTitle,
                        onClick = {
                            if (shareTrackFile(context, selectedTrack, shareTitle)) {
                                onDismiss()
                            } else {
                                Toast.makeText(context, R.string.music_share_failed, Toast.LENGTH_SHORT).show()
                            }
                        },
                    )
                    TrackAction(
                        icon = GlassIcons.Info,
                        text = stringResource(R.string.music_song_info),
                        onClick = {
                            songInfoTrack = selectedTrack
                            onDismiss()
                        },
                    )
                    TrackAction(
                        icon = if (selectedTrack.contentUri in mutations?.hiddenContentUris.orEmpty()) {
                            MiuixIcons.Reset
                        } else {
                            GlassIcons.Hide
                        },
                        text = stringResource(
                            if (selectedTrack.contentUri in mutations?.hiddenContentUris.orEmpty()) {
                                R.string.music_restore_song
                            } else {
                                R.string.music_hide_song
                            },
                        ),
                        enabled = mutations != null,
                        onClick = {
                            if (selectedTrack.contentUri in mutations?.hiddenContentUris.orEmpty()) {
                                mutations?.onRestore?.invoke(setOf(selectedTrack.contentUri))
                            } else {
                                mutationTrack = selectedTrack
                                deleteRequested = false
                            }
                            onDismiss()
                        },
                    )
                    TrackAction(
                        icon = GlassIcons.Delete,
                        text = stringResource(R.string.music_delete_song),
                        enabled = mutations != null,
                        onClick = {
                            mutationTrack = selectedTrack
                            deleteRequested = true
                            onDismiss()
                        },
                    )
                }
            }
        }
    }

    OverlayDialog(
        show = showMutationConfirm,
        title = stringResource(
            if (deleteRequested) R.string.music_delete_song else R.string.music_hide_song,
        ),
        summary = stringResource(
            if (deleteRequested) R.string.music_delete_confirm else R.string.music_hide_confirm,
            mutationTrack?.title ?: stringResource(R.string.music_unknown_title),
        ),
        onDismissRequest = { showMutationConfirm = false },
        onDismissFinished = { mutationTrack = null },
    ) {
        OverlayDialogActions(
            dismissText = stringResource(R.string.clear_queue_confirm_cancel),
            confirmText = stringResource(R.string.clear_queue_confirm_confirm),
            onDismiss = { showMutationConfirm = false },
            onConfirm = {
                mutationTrack?.let { selected ->
                    if (deleteRequested) mutations?.onDelete?.invoke(selected)
                    else mutations?.onHide?.invoke(selected)
                }
                showMutationConfirm = false
            },
            confirmRole = if (deleteRequested) GlassDialogButtonRole.Destructive else GlassDialogButtonRole.Primary,
        )
    }

    OverlayBottomSheet(
        show = songInfoTrack != null,
        title = stringResource(R.string.music_song_info),
        startAction = {
            BottomSheetCloseButton(onClick = { songInfoTrack = null })
        },
        enableWindowDim = true,
        onDismissRequest = { songInfoTrack = null },
        onDismissFinished = { retainedSongInfoTrack = null },
    ) { headerPadding ->
        (songInfoTrack ?: retainedSongInfoTrack)?.let { infoTrack ->
            SheetScrollableContent(
                headerPadding = headerPadding,
                bottomPadding = navigationBarBottomPadding + 16.dp,
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    SongInfoRow(stringResource(R.string.song_info_title), infoTrack.title)
                    SongInfoRow(stringResource(R.string.song_info_artist), infoTrack.artist)
                    SongInfoRow(stringResource(R.string.song_info_album), infoTrack.album)
                    SongInfoRow(
                        stringResource(R.string.song_info_album_artist),
                        infoTrack.albumArtist,
                    )
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    SongInfoRow(
                        stringResource(R.string.song_info_duration),
                        formatDuration(infoTrack.durationMs),
                    )
                    SongInfoRow(
                        stringResource(R.string.song_info_format),
                        infoTrack.audioFormatLabel(),
                    )
                    SongInfoRow(
                        stringResource(R.string.song_info_file_size),
                        formatFileSize(infoTrack.fileSizeBytes),
                    )
                    SongInfoRow(
                        stringResource(R.string.song_info_bitrate),
                        infoTrack.bitrateBitsPerSecond?.takeIf { it > 0 }?.let { bitrate ->
                            stringResource(R.string.song_info_bitrate_value, bitrate / 1_000f)
                        },
                    )
                    SongInfoRow(
                        stringResource(R.string.song_info_sample_rate),
                        infoTrack.sampleRateHz?.takeIf { it > 0 }?.let { sampleRate ->
                            stringResource(R.string.song_info_sample_rate_value, sampleRate / 1_000f)
                        },
                    )
                    SongInfoRow(
                        stringResource(R.string.song_info_bit_depth),
                        infoTrack.bitDepth?.takeIf { it > 0 }?.let { bitDepth ->
                            stringResource(R.string.song_info_bit_depth_value, bitDepth)
                        },
                    )
                    SongInfoRow(
                        stringResource(R.string.song_info_file_location),
                        infoTrack.displayFileLocation(),
                    )
                }
            }
        }
    }

    if (onGoToArtist != null) {
        TrackArtistsSheet(
            artists = selectedArtists,
            onDismiss = { selectedArtists = emptyList() },
            onGoToArtist = onGoToArtist,
        )
    }
    artworkPreview?.let { request ->
        ArtworkPreviewOverlay(
            request = request,
            onPresented = { artworkPreviewPresented = it },
            onClosed = { artworkPreview = null },
        )
    }
}

@Composable
internal fun TrackArtistsSheet(
    artists: List<ArtistGroup>,
    onDismiss: () -> Unit,
    onGoToArtist: (ArtistGroup) -> Unit,
) {
    var retainedArtists by remember { mutableStateOf(artists) }
    LaunchedEffect(artists) {
        if (artists.isNotEmpty()) retainedArtists = artists
    }
    val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    OverlayBottomSheet(
        show = artists.isNotEmpty(),
        title = stringResource(R.string.participating_artists),
        startAction = { BottomSheetCloseButton(onClick = onDismiss) },
        enableWindowDim = true,
        onDismissRequest = onDismiss,
        onDismissFinished = { if (artists.isEmpty()) retainedArtists = emptyList() },
    ) { headerPadding ->
        SheetScrollableContent(
            headerPadding = headerPadding,
            bottomPadding = navigationBottom + 16.dp,
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                artists.ifEmpty { retainedArtists }.forEach { artist ->
                    ArtistListItem(
                        artist = artist,
                        artworkTextSpacing = 12.dp,
                        insideMargin = PaddingValues(12.dp),
                        showNavigationIcon = false,
                        onClick = {
                            onDismiss()
                            onGoToArtist(artist)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetScrollableContent(
    headerPadding: PaddingValues,
    bottomPadding: Dp,
    headerTopReduction: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    val layoutDirection = LocalLayoutDirection.current
    val adjustedPadding = PaddingValues(
        start = headerPadding.calculateStartPadding(layoutDirection),
        top = (headerPadding.calculateTopPadding() - headerTopReduction).coerceAtLeast(0.dp),
        end = headerPadding.calculateEndPadding(layoutDirection),
        bottom = headerPadding.calculateBottomPadding(),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .overScrollVertical(
                isEnabled = { scrollState.maxValue > 0 },
            )
            .verticalScroll(scrollState, overscrollEffect = null)
            .padding(adjustedPadding)
            .padding(bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun TrackAction(
    icon: ImageVector,
    text: String,
    iconSize: Dp = TrackActionIconSize,
    iconStartPadding: Dp = 0.dp,
    iconEndPadding: Dp = 0.dp,
    truncateText: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)?,
) {
    TrackAction(
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = iconStartPadding, end = iconEndPadding)
                    .size(iconSize),
                tint = if (enabled) {
                    MiuixTheme.colorScheme.onSurfaceVariantActions
                } else {
                    MiuixTheme.colorScheme.disabledOnSecondaryVariant
                },
            )
        },
        text = text,
        truncateText = truncateText,
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
private fun TrackAction(
    icon: @Composable () -> Unit,
    text: String,
    truncateText: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)?,
) {
    if (!truncateText) {
        BasicComponent(
            title = text,
            startAction = icon,
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    BasicComponent(
        startAction = icon,
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            fontSize = MiuixTheme.textStyles.headline1.fontSize,
            fontWeight = FontWeight.Medium,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BottomSheetCloseButton(onClick: () -> Unit) {
    BottomSheetHeaderButton(onClick = onClick) {
        MiuixIcon(
            imageVector = GlassIcons.Close,
            contentDescription = stringResource(R.string.close),
            tint = MiuixTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SongInfoRow(label: String, value: String?) {
    val context = LocalContext.current
    val displayValue = value?.takeIf(String::isNotBlank)
        ?: stringResource(R.string.not_available)
    BasicComponent(
        title = label,
        onClick = {
            context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(
                ClipData.newPlainText(label, displayValue),
            )
        },
        endActions = {
            Text(
                text = displayValue,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

internal fun participatingArtistGroups(
    track: MusicTrack,
    artistGroups: List<ArtistGroup>,
): List<ArtistGroup> = participatingArtistGroups(listOf(track), artistGroups)

internal fun participatingArtistGroups(
    tracks: List<MusicTrack>,
    artistGroups: List<ArtistGroup>,
): List<ArtistGroup> {
    val artistGroupsByKey = artistGroups.associateBy(ArtistGroup::key)
    return tracks
        .asSequence()
        .flatMap { track -> splitArtistNames(track.artist).asSequence() }
        .map(::artistGroupKey)
        .distinct()
        .mapNotNull { artistGroupsByKey[it] }
        .toList()
}

internal fun MusicTrack.audioFormatLabel(): String? {
    val extension = fileName
        ?.substringAfterLast('.', missingDelimiterValue = "")
        ?.trim()
        ?.takeIf(String::isNotEmpty)
    return extension?.uppercase(Locale.ROOT) ?: mimeType
        ?.substringAfter('/', missingDelimiterValue = "")
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?.uppercase(Locale.ROOT)
}

internal fun MusicTrack.displayFileLocation(): String? {
    if (folderPath.isNullOrBlank() && fileName.isNullOrBlank()) return null
    val displayFolder = folderDisplayPath(folderPath)
    val baseFolder = when {
        displayFolder == "/" -> "/storage/emulated/0"
        displayFolder.startsWith("/storage/") -> displayFolder
        else -> "/storage/emulated/0${displayFolder.trimEnd('/')}"
    }
    val name = fileName?.trim()?.takeIf(String::isNotEmpty)
    return name?.let { "$baseFolder/$it" } ?: baseFolder
}
