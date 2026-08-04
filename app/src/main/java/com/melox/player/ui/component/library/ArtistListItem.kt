package com.melox.player.ui.component.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.melox.player.R
import com.melox.player.data.library.ArtistGroup
import com.melox.player.model.MusicTrack
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ArtistListItem(
    artist: ArtistGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artworkTextSpacing: Dp = 8.dp,
    insideMargin: PaddingValues = PaddingValues(
        start = 16.dp,
        top = 8.dp,
        end = 28.dp,
        bottom = 8.dp,
    ),
    showNavigationIcon: Boolean = true,
) {
    BasicComponent(
        modifier = modifier.fillMaxWidth(),
        startAction = {
            Row {
                ArtistArtwork(artist.coverTrack)
                Spacer(modifier = Modifier.width((artworkTextSpacing - 8.dp).coerceAtLeast(0.dp)))
            }
        },
        endActions = if (showNavigationIcon) {
            {
                Icon(
                    imageVector = MiuixIcons.Basic.ArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(width = 10.dp, height = 16.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                )
            }
        } else {
            null
        },
        insideMargin = insideMargin,
        onClick = onClick,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = artist.name ?: stringResource(R.string.artist_unknown),
                style = MiuixTheme.textStyles.headline2,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.artist_counts,
                    pluralStringResource(
                        R.plurals.artist_album_count,
                        artist.albumCount,
                        artist.albumCount,
                    ),
                    pluralStringResource(
                        R.plurals.artist_song_count,
                        artist.tracks.size,
                        artist.tracks.size,
                    ),
                ),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun ArtistArtwork(
    track: MusicTrack?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val bitmap = rememberArtworkBitmap(
        contentUri = track?.contentUri.orEmpty(),
        dateModifiedEpochSeconds = track?.dateModifiedEpochSeconds ?: 0L,
        fileSizeBytes = track?.fileSizeBytes ?: 0L,
        size = size,
    )
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .background(MiuixTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MiuixIcons.Music,
                contentDescription = null,
                modifier = Modifier.size(size / 2f),
                tint = MiuixTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}
