package com.melox.player.ui.component.library

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
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
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.ContactsCircle
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

private val TrackActionIconSize = 22.dp
private val TrackActionAddToQueueIconSize = 20.dp
private val TrackActionSummaryArtworkSize = 56.dp
private val TrackActionSummaryArtworkCornerRadius = 8.dp
internal const val MusicTagEditorPackage = "com.xjcheng.musictageditor"
internal const val LyricoPackage = "com.lonx.lyrico"
internal const val LyricoEditTagAction = "com.lonx.lyrico.action.EDIT_TAG"

@Composable
fun TrackActionsOverlay(
    track: MusicTrack?,
    onDismiss: () -> Unit,
    onPlayNext: (MusicTrack) -> Unit,
    onAppendToQueue: (MusicTrack) -> Unit,
    onGoToAlbum: ((MusicTrack) -> Unit)? = null,
    artistGroups: List<ArtistGroup> = emptyList(),
    onGoToArtist: ((ArtistGroup) -> Unit)? = null,
    onExternalEditReturned: (Long) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnExternalEditReturned by rememberUpdatedState(onExternalEditReturned)
    val externalEditorUnavailableMessage =
        stringResource(R.string.music_external_editor_unavailable)
    var retainedTrack by remember { mutableStateOf(track) }
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
    val externalEditorLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        finishExternalEdit()
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
    val launchExternalEditor: (MusicTrack, String, String) -> Unit =
        { selectedTrack, packageName, action ->
            pendingExternalEditTrackId = selectedTrack.id
            runCatching {
                externalEditorLauncher.launch(
                    selectedTrack.externalEditorIntent(
                        packageName = packageName,
                        action = action,
                    ),
                )
            }.onSuccess {
                onDismiss()
            }.onFailure {
                pendingExternalEditTrackId = null
                Toast.makeText(
                    context,
                    externalEditorUnavailableMessage,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    val navigationBarBottomPadding = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    LaunchedEffect(track?.id) {
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
                        color = MiuixTheme.colorScheme.secondaryContainer,
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
                    )
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.secondaryContainer,
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
                    TrackAction(
                        icon = MiuixIcons.Edit,
                        text = stringResource(R.string.music_edit_with_music_tag_editor),
                        onClick = {
                            launchExternalEditor(
                                selectedTrack,
                                MusicTagEditorPackage,
                                Intent.ACTION_VIEW,
                            )
                        },
                    )
                    TrackAction(
                        icon = MiuixIcons.Edit,
                        text = stringResource(R.string.music_edit_with_lyrico),
                        onClick = {
                            launchExternalEditor(
                                selectedTrack,
                                LyricoPackage,
                                LyricoEditTagAction,
                            )
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
                        color = MiuixTheme.colorScheme.secondaryContainer,
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
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.secondaryContainer,
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
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.secondaryContainer,
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
}

@Composable
private fun SheetScrollableContent(
    bottomPadding: Dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState(), overscrollEffect = null)
            .overScrollVertical()
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

internal fun MusicTrack.externalEditorIntent(
    packageName: String,
    action: String,
): Intent {
    val uri = contentUri.toUri()
    return Intent(action).apply {
        setDataAndType(
            uri,
            mimeType?.takeIf { it.startsWith("audio/", ignoreCase = true) } ?: "audio/*",
        )
        setPackage(packageName)
        clipData = ClipData.newRawUri("audio", uri)
        addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
    }
}

internal fun participatingArtistGroups(
    track: MusicTrack,
    artistGroups: List<ArtistGroup>,
): List<ArtistGroup> = splitArtistNames(track.artist).mapNotNull { artistName ->
    artistGroups.firstOrNull { it.key == artistGroupKey(artistName) }
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
