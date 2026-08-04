package com.melox.player.ui.component.library

// Information hierarchy adapted from Replica0110/Lyrico (Apache-2.0).

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import com.melox.player.R
import com.melox.player.data.library.displayArtistName
import com.melox.player.model.AudioQuality
import com.melox.player.model.MusicTrack
import com.melox.player.model.resolveAudioQuality
import java.util.Locale
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.theme.MiuixTheme

enum class MusicTrackDescriptionMode {
    ArtistAndAlbum,
    Album,
    Artist,
}

@Composable
fun MusicTrackRow(
    track: MusicTrack,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    descriptionMode: MusicTrackDescriptionMode = MusicTrackDescriptionMode.ArtistAndAlbum,
) {
    val title = track.title ?: stringResource(R.string.music_unknown_title)
    val moreActionLabel = stringResource(R.string.music_more_actions, title)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(R.string.music_play_track_action, title),
                onClick = onClick,
            )
            .padding(
                start = 16.dp,
                top = 8.dp,
                end = 16.dp,
                bottom = 8.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MusicTrackLeadingContent(
            track = track,
            isCurrent = isCurrent,
            descriptionMode = descriptionMode,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatDuration(track.durationMs),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                maxLines = 1,
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClickLabel = moreActionLabel,
                        onClick = onMoreClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MiuixIcons.More,
                    contentDescription = moreActionLabel,
                    modifier = Modifier.size(20.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                )
            }
        }
    }
}

@Composable
internal fun MusicTrackSummary(
    track: MusicTrack,
    modifier: Modifier = Modifier,
    isCurrent: Boolean = false,
    descriptionMode: MusicTrackDescriptionMode = MusicTrackDescriptionMode.ArtistAndAlbum,
    artworkSize: Dp = 48.dp,
    artworkCornerRadius: Dp = 6.dp,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MusicTrackLeadingContent(
            track = track,
            isCurrent = isCurrent,
            descriptionMode = descriptionMode,
            artworkSize = artworkSize,
            artworkCornerRadius = artworkCornerRadius,
        )
    }
}

@Composable
private fun RowScope.MusicTrackLeadingContent(
    track: MusicTrack,
    isCurrent: Boolean,
    descriptionMode: MusicTrackDescriptionMode,
    artworkSize: Dp = 48.dp,
    artworkCornerRadius: Dp = 6.dp,
) {
    val title = track.title ?: stringResource(R.string.music_unknown_title)
    val artist = displayArtistName(track.artist) ?: stringResource(R.string.music_unknown_artist)
    val description = when (descriptionMode) {
        MusicTrackDescriptionMode.ArtistAndAlbum -> track.album?.let { album ->
            stringResource(R.string.music_artist_album, artist, album)
        } ?: artist
        MusicTrackDescriptionMode.Album ->
            track.album ?: stringResource(R.string.album_unknown)
        MusicTrackDescriptionMode.Artist -> artist
    }
    val quality = track.resolveAudioQuality()
    val qualityLabel = quality?.let {
        stringResource(
            when (it) {
                AudioQuality.HR -> R.string.music_quality_hr
                AudioQuality.SQ -> R.string.music_quality_sq
                AudioQuality.HQ -> R.string.music_quality_hq
            },
        )
    }
    val qualityColor = quality?.let {
        val darkSurface = MiuixTheme.colorScheme.surface.luminance() < 0.5f
        when (it) {
            AudioQuality.HR -> Color(0xFFFFD54F)
            AudioQuality.SQ -> if (darkSurface) Color(0xFFB69CFF) else Color(0xFF7650D8)
            AudioQuality.HQ -> if (darkSurface) Color(0xFF74B4FF) else Color(0xFF246FD1)
        }
    }

    TrackArtwork(
        contentUri = track.contentUri,
        dateModifiedEpochSeconds = track.dateModifiedEpochSeconds,
        fileSizeBytes = track.fileSizeBytes,
        size = artworkSize,
        cornerRadius = artworkCornerRadius,
    )

    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.headline2,
            fontWeight = FontWeight.Medium,
            color = if (isCurrent) {
                MiuixTheme.colorScheme.primary
            } else {
                MiuixTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            qualityLabel?.let {
                ProjectBadge(
                    text = it,
                    textColor = requireNotNull(qualityColor),
                    containerColor = qualityColor.copy(alpha = 0.16f),
                )
            }
            Text(
                text = description,
                modifier = Modifier.weight(1f),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun ProjectBadge(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MiuixTheme.colorScheme.primary,
    containerColor: Color = MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 16.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(containerColor)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
        )
    }
}

internal fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}

internal fun formatFileSize(fileSizeBytes: Long): String {
    val safeBytes = fileSizeBytes.coerceAtLeast(0L)
    if (safeBytes < 1_024L) return "$safeBytes B"
    val kibibytes = safeBytes / 1_024.0
    if (kibibytes < 1_024.0) return String.format(Locale.ROOT, "%.1f KB", kibibytes)
    val mebibytes = kibibytes / 1_024.0
    if (mebibytes < 1_024.0) return String.format(Locale.ROOT, "%.1f MB", mebibytes)
    return String.format(Locale.ROOT, "%.1f GB", mebibytes / 1_024.0)
}
