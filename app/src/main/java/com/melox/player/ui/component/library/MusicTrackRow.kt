package com.melox.player.ui.component.library


import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.sp
import com.melox.player.R
import com.melox.player.data.library.displayArtistName
import com.melox.player.model.AudioQuality
import com.melox.player.model.MusicTrack
import com.melox.player.model.resolveAudioQuality
import java.util.Locale
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.CheckboxDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme

enum class MusicTrackDescriptionMode {
    ArtistAndAlbum,
    Album,
    Artist,
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicTrackRow(
    track: MusicTrack,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onMoreClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    descriptionMode: MusicTrackDescriptionMode = MusicTrackDescriptionMode.ArtistAndAlbum,
    artworkOverlayText: String? = null,
    enabled: Boolean = true,
    descriptionOverride: String? = null,
    moreActionEnabled: Boolean = true,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    val title = track.title ?: stringResource(R.string.music_unknown_title)
    val moreActionLabel = stringResource(R.string.music_more_actions, title)
    val rowActionLabel = stringResource(R.string.music_play_track_action, title)
    val rowGestureModifier = if (selectionMode) {
        Modifier.clickable(
            enabled = true,
            onClickLabel = rowActionLabel,
            onClick = onClick,
        )
    } else {
        Modifier.combinedClickable(
            enabled = enabled || onLongClick != null,
            onClickLabel = rowActionLabel,
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(rowGestureModifier)
            .padding(
                start = 16.dp,
                top = 8.dp,
                end = TopAppBarDefaults.ActionIconPadding,
                bottom = 8.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MusicTrackLeadingContent(
            track = track,
            isCurrent = isCurrent,
            descriptionMode = descriptionMode,
            artworkOverlayText = artworkOverlayText,
            enabled = enabled,
            descriptionOverride = descriptionOverride,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatDuration(track.durationMs),
                modifier = Modifier.width(IconButtonDefaults.MinWidth),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (selectionMode) {
                Box(
                    modifier = Modifier.size(IconButtonDefaults.MinWidth),
                    contentAlignment = Alignment.Center,
                ) {
                    Checkbox(
                        state = if (selected) {
                            ToggleableState.On
                        } else {
                            ToggleableState.Off
                        },
                        onClick = onClick,
                        colors = CheckboxDefaults.checkboxColors(
                            disabledCheckedForegroundColor = MiuixTheme.colorScheme.onPrimary,
                            disabledUncheckedForegroundColor = MiuixTheme.colorScheme.secondary,
                            disabledCheckedBackgroundColor = MiuixTheme.colorScheme.primary,
                            disabledUncheckedBackgroundColor = MiuixTheme.colorScheme.secondary,
                        ),
                        enabled = true,
                        modifier = Modifier.scale(20f / 26f),
                    )
                }
            } else if (onMoreClick != null) {
                Box(
                    modifier = Modifier
                        .size(IconButtonDefaults.MinWidth)
                        .clickable(
                            enabled = moreActionEnabled,
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
                        tint = if (moreActionEnabled) {
                            MiuixTheme.colorScheme.onSurfaceVariantActions
                        } else {
                            MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.45f)
                        },
                    )
                }
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
    artworkContent: (@Composable () -> Unit)? = null,
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
            artworkContent = artworkContent,
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
    artworkOverlayText: String? = null,
    enabled: Boolean = true,
    descriptionOverride: String? = null,
    artworkContent: (@Composable () -> Unit)? = null,
) {
    val title = track.title ?: stringResource(R.string.music_unknown_title)
    val artist = displayArtistName(track.artist) ?: stringResource(R.string.music_unknown_artist)
    val description = descriptionOverride ?: when (descriptionMode) {
        MusicTrackDescriptionMode.ArtistAndAlbum -> track.album?.let { album ->
            stringResource(R.string.music_artist_album, artist, album)
        } ?: artist
        MusicTrackDescriptionMode.Album ->
            track.album ?: stringResource(R.string.album_unknown)
        MusicTrackDescriptionMode.Artist -> artist
    }
    val quality = track.resolveAudioQuality().takeIf { descriptionOverride == null }
    val contentAlpha = if (enabled) 1f else 0.45f
    val qualityLabel = quality?.let {
        stringResource(
            when (it) {
                AudioQuality.RAW -> R.string.music_quality_raw
                AudioQuality.HI_RES -> R.string.music_quality_hi_res
                AudioQuality.SQ -> R.string.music_quality_sq
                AudioQuality.HQ -> R.string.music_quality_hq
            },
        )
    }
    val qualityColors = quality?.let {
        val darkSurface = MiuixTheme.colorScheme.surface.luminance() < 0.5f
        when (it) {
            AudioQuality.RAW -> if (darkSurface) {
                Color(0xFFFF858D) to Color(0xFF43191D)
            } else {
                Color(0xFFE24D4D) to Color(0xFFFFD6DA)
            }
            AudioQuality.HI_RES -> if (darkSurface) {
                Color(0xFFE8C34A) to Color(0xFF3A3010)
            } else {
                Color(0xFFB88600) to Color(0xFFFFE1A8)
            }
            AudioQuality.SQ -> if (darkSurface) {
                Color(0xFFC294F4) to Color(0xFF30203F)
            } else {
                Color(0xFF8A5ED3) to Color(0xFFE3D6FB)
            }
            AudioQuality.HQ -> if (darkSurface) {
                Color(0xFF79B8FF) to Color(0xFF102A43)
            } else {
                Color(0xFF3A7DDE) to Color(0xFFD2E4FF)
            }
        }
    }

    if (artworkContent != null) {
        artworkContent()
    } else if (artworkOverlayText == null) {
        TrackArtwork(
            contentUri = track.contentUri,
            dateModifiedEpochSeconds = track.dateModifiedEpochSeconds,
            fileSizeBytes = track.fileSizeBytes,
            modifier = Modifier.alpha(contentAlpha),
            size = artworkSize,
            cornerRadius = artworkCornerRadius,
        )
    } else {
        Box(
            modifier = Modifier.size(artworkSize),
            contentAlignment = Alignment.Center,
        ) {
            TrackArtwork(
                contentUri = track.contentUri,
                dateModifiedEpochSeconds = track.dateModifiedEpochSeconds,
                fileSizeBytes = track.fileSizeBytes,
                modifier = Modifier
                    .alpha(contentAlpha)
                    .squircleClip(artworkCornerRadius)
                    .blur(6.dp)
                    .alpha(0.1f),
                size = artworkSize,
                cornerRadius = artworkCornerRadius,
            )
            Text(
                text = artworkOverlayText,
                style = MiuixTheme.textStyles.headline1,
                color = MiuixTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .alpha(contentAlpha),
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
                    textColor = requireNotNull(qualityColors).first,
                    containerColor = qualityColors.second,
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
    val descriptionTextHeight = with(LocalDensity.current) {
        MiuixTheme.textStyles.footnote1.fontSize.toDp()
    }
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = descriptionTextHeight)
            .clip(RoundedCornerShape(3.dp))
            .background(containerColor)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = textColor,
            style = MiuixTheme.textStyles.footnote2,
            fontSize = 9.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
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
