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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.melox.player.R
import com.melox.player.data.library.ArtistGroup
import top.yukonga.miuix.kmp.basic.BasicComponent
import androidx.compose.ui.platform.LocalLayoutDirection
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
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
    val layoutDirection = LocalLayoutDirection.current
    BasicComponent(
        modifier = modifier.fillMaxWidth(),
        startAction = {
            Row {
                ArtistArtwork(artist)
                Spacer(modifier = Modifier.width((artworkTextSpacing - 8.dp).coerceAtLeast(0.dp)))
            }
        },
        endActions = if (showNavigationIcon) {
            {
                Image(
                    imageVector = MiuixIcons.Basic.ArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(width = 10.dp, height = 16.dp).graphicsLayer {
                        scaleX = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
                    },
                    colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurfaceVariantActions),
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
    artist: ArtistGroup,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val context = LocalContext.current.applicationContext
    val targetSizePx = with(LocalDensity.current) { ArtistDetailArtworkSize.roundToPx() }
    val bitmap = rememberArtworkLoad(artist to targetSizePx) {
        val portrait = if (artist.name != null) {
            artist.tracks.firstNotNullOfOrNull { track ->
                loadArtworkBitmap(
                    context = context,
                    contentUri = track.contentUri,
                    dateModifiedEpochSeconds = track.dateModifiedEpochSeconds,
                    fileSizeBytes = track.fileSizeBytes,
                    targetSizePx = targetSizePx,
                    artistImage = true,
                )
            }
        } else null
        portrait ?: artist.coverTrack?.let { track ->
            loadArtworkBitmap(
                context = context,
                contentUri = track.contentUri,
                dateModifiedEpochSeconds = track.dateModifiedEpochSeconds,
                fileSizeBytes = track.fileSizeBytes,
                targetSizePx = targetSizePx,
            )
        }
    }?.value
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
        )
    }
}

internal val ArtistDetailArtworkSize = 72.dp
