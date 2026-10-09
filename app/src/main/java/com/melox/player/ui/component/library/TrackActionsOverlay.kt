package com.melox.player.ui.component.library

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.melox.player.R
import com.melox.player.ui.component.bottomSheetCardColor
import com.melox.player.ui.component.bottomSheetGlassModifier
import com.melox.player.ui.component.bottomSheetMaterialColor
import com.melox.player.data.library.ArtistGroup
import com.melox.player.data.library.artistGroupKey
import com.melox.player.data.library.displayArtistName
import com.melox.player.data.library.folderDisplayPath
import com.melox.player.data.library.splitArtistNames
import com.melox.player.model.MusicTrack
import java.util.Locale
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.ContactsCircle
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Playlist
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

private val TrackActionIconSize = 22.dp
private val TrackActionAddToQueueIconSize = 20.dp
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
) {
    val context = LocalContext.current
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
    var artworkPreview by remember { mutableStateOf<ArtworkPreviewRequest?>(null) }
    var artworkPreviewPresented by remember { mutableStateOf(false) }
    var songInfoTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var retainedSongInfoTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var artistListTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var retainedArtistListTrack by remember { mutableStateOf<MusicTrack?>(null) }
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
    val openExternalEditor: (MusicTrack, ExternalEditorKind, String) -> Unit =
        { selectedTrack, editorKind, notFoundMessage ->
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
            songInfoTrack = null
            retainedSongInfoTrack = null
            artistListTrack = null
            retainedArtistListTrack = null
        }
    }
    LaunchedEffect(songInfoTrack?.id) {
        if (songInfoTrack != null) {
            retainedSongInfoTrack = songInfoTrack
        }
    }
    LaunchedEffect(artistListTrack?.id) {
        if (artistListTrack != null) {
            retainedArtistListTrack = artistListTrack
        }
    }
    OverlayBottomSheet(
        show = track != null,
        allowDismiss = artworkPreview == null,
        modifier = bottomSheetGlassModifier(),
        backgroundColor = bottomSheetMaterialColor(),
        enableWindowDim = true,
        onDismissRequest = onDismiss,
        onDismissFinished = {
            retainedTrack = null
        },
    ) {
        (track ?: retainedTrack)?.let { selectedTrack ->
            SheetScrollableContent(
                bottomPadding = navigationBarBottomPadding + 12.dp,
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.defaultColors(
                        color = bottomSheetCardColor(),
                    ),
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
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = bottomSheetCardColor(),
                    ),
                ) {
                    TrackAction(
                        icon = MiuixIcons.Add,
                        text = stringResource(R.string.music_play_next),
                        onClick = {
                            onPlayNext(selectedTrack)
                            onDismiss()
                        },
                    )
                    TrackAction(
                        icon = painterResource(R.drawable.ic_add_list),
                        text = stringResource(R.string.music_add_to_queue),
                        iconSize = TrackActionAddToQueueIconSize,
                        iconStartPadding = 1.dp,
                        iconEndPadding = 2.dp,
                        onClick = {
                            onAppendToQueue(selectedTrack)
                            onDismiss()
                        },
                    )
                    TrackAction(
                        icon = MiuixIcons.AddCircle,
                        text = stringResource(R.string.playlist_add_to),
                        onClick = {
                            onAddToPlaylist(selectedTrack)
                            onDismiss()
                        },
                    )
                    onGoToAlbum?.let { goToAlbum ->
                        selectedTrack.album?.let { albumName ->
                            TrackAction(
                                icon = MiuixIcons.Album,
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
                                icon = MiuixIcons.ContactsCircle,
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
                                        artistListTrack = selectedTrack
                                        onDismiss()
                                    }
                                },
                            )
                        }
                    }
                    visibleExternalEditors(
                        showMusicTagEditor,
                        showLyricoEditor,
                        showLunaBeatEditor,
                    ).forEach { editor ->
                        val titleRes = when (editor) {
                            ExternalEditorKind.MusicTagEditor -> R.string.music_edit_with_music_tag_editor
                            ExternalEditorKind.Lyrico -> R.string.music_edit_with_lyrico
                            ExternalEditorKind.LunaBeat -> R.string.music_edit_with_luna_beat
                        }
                        val notFoundMessage = when (editor) {
                            ExternalEditorKind.MusicTagEditor -> musicTagEditorNotFoundMessage
                            ExternalEditorKind.Lyrico -> lyricoNotFoundMessage
                            ExternalEditorKind.LunaBeat -> lunaBeatNotFoundMessage
                        }
                        TrackAction(
                            icon = MiuixIcons.Edit,
                            text = stringResource(titleRes),
                            onClick = {
                                openExternalEditor(selectedTrack, editor, notFoundMessage)
                            },
                        )
                    }
                    TrackAction(
                        icon = {
                            Icon(
                                imageVector = MiuixIcons.Share,
                                contentDescription = null,
                                modifier = Modifier.size(TrackActionIconSize).graphicsLayer {
                                    scaleX = 24f / 22f
                                    scaleY = scaleX
                                },
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
                        icon = MiuixIcons.Info,
                        text = stringResource(R.string.music_song_info),
                        onClick = {
                            songInfoTrack = selectedTrack
                            onDismiss()
                        },
                    )
                }
            }
        }
    }

    OverlayBottomSheet(
        show = songInfoTrack != null,
        modifier = bottomSheetGlassModifier(),
        backgroundColor = bottomSheetMaterialColor(),
        title = stringResource(R.string.music_song_info),
        startAction = {
            BottomSheetCloseButton(onClick = { songInfoTrack = null })
        },
        enableWindowDim = true,
        onDismissRequest = { songInfoTrack = null },
        onDismissFinished = { retainedSongInfoTrack = null },
    ) {
        (songInfoTrack ?: retainedSongInfoTrack)?.let { infoTrack ->
            SheetScrollableContent(
                bottomPadding = navigationBarBottomPadding + 12.dp,
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.defaultColors(
                        color = bottomSheetCardColor(),
                    ),
                ) {
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
                    colors = CardDefaults.defaultColors(
                        color = bottomSheetCardColor(),
                    ),
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

    OverlayBottomSheet(
        show = artistListTrack != null,
        modifier = bottomSheetGlassModifier(),
        backgroundColor = bottomSheetMaterialColor(),
        title = stringResource(R.string.participating_artists),
        startAction = {
            BottomSheetCloseButton(onClick = { artistListTrack = null })
        },
        enableWindowDim = true,
        onDismissRequest = { artistListTrack = null },
        onDismissFinished = { retainedArtistListTrack = null },
    ) {
        (artistListTrack ?: retainedArtistListTrack)?.let { artistTrack ->
            SheetScrollableContent(
                bottomPadding = navigationBarBottomPadding + 12.dp,
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = bottomSheetCardColor(),
                    ),
                ) {
                    participatingArtistGroups(
                        track = artistTrack,
                        artistGroups = artistGroups,
                    ).forEach { artist ->
                        ArtistListItem(
                            artist = artist,
                            artworkTextSpacing = 12.dp,
                            insideMargin = PaddingValues(12.dp),
                            showNavigationIcon = false,
                            onClick = {
                                artistListTrack = null
                                onGoToArtist?.invoke(artist)
                            },
                        )
                    }
                }
            }
        }
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
private fun SheetScrollableContent(
    bottomPadding: Dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .overScrollVertical(
                nestedScrollToParent = false,
                isEnabled = { scrollState.maxValue > 0 },
            )
            .verticalScroll(scrollState, overscrollEffect = null)
            .padding(bottom = bottomPadding),
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
    onClick: () -> Unit,
) {
    TrackAction(
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = iconStartPadding, end = iconEndPadding)
                    .size(iconSize),
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        },
        text = text,
        truncateText = truncateText,
        onClick = onClick,
    )
}

@Composable
private fun TrackAction(
    icon: Painter,
    text: String,
    iconSize: Dp = TrackActionIconSize,
    iconStartPadding: Dp = 0.dp,
    iconEndPadding: Dp = 0.dp,
    truncateText: Boolean = false,
    onClick: () -> Unit,
) {
    TrackAction(
        icon = {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = iconStartPadding, end = iconEndPadding)
                    .size(iconSize),
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        },
        text = text,
        truncateText = truncateText,
        onClick = onClick,
    )
}

@Composable
private fun TrackAction(
    icon: @Composable () -> Unit,
    text: String,
    truncateText: Boolean = false,
    onClick: () -> Unit,
) {
    if (!truncateText) {
        BasicComponent(
            title = text,
            startAction = icon,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    BasicComponent(
        startAction = icon,
        onClick = onClick,
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
    IconButton(onClick = onClick) {
        Icon(
            imageVector = MiuixIcons.Close,
            contentDescription = stringResource(R.string.close),
            tint = MiuixTheme.colorScheme.onBackground,
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
