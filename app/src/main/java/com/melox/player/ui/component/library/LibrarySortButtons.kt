package com.melox.player.ui.component.library

import com.melox.player.ui.component.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.melox.player.R
import com.melox.player.data.library.AlbumGridStyle
import com.melox.player.data.library.AlbumSortConfig
import com.melox.player.data.library.AlbumSortField
import com.melox.player.data.library.ArtistSortConfig
import com.melox.player.data.library.ArtistSortField
import com.melox.player.data.library.FolderSortConfig
import com.melox.player.data.library.FolderSortField
import com.melox.player.ui.component.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Sort

@Composable
fun AlbumSortButton(
    config: AlbumSortConfig,
    onConfigChange: (AlbumSortConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridStyles = listOf(
        AlbumGridStyle.THREE to stringResource(R.string.album_grid_three),
        AlbumGridStyle.TWO_SMALL to stringResource(R.string.album_grid_two_small),
    )
    val fields = listOf(
        AlbumSortField.ALBUM to stringResource(R.string.album_sort_album),
        AlbumSortField.ALBUM_ARTIST to stringResource(R.string.album_sort_album_artist),
        AlbumSortField.SONG_COUNT to stringResource(R.string.album_sort_song_count),
        AlbumSortField.YEAR to stringResource(R.string.album_sort_year),
    )
    if (LocalOs4Style.current) {
        OverlayIconDropdownMenu(
            modifier = modifier,
            entries = listOf(
                DropdownEntry(gridStyles.map { (gridStyle, label) ->
                    DropdownItem(text = label, selected = config.gridStyle == gridStyle,
                        onClick = { onConfigChange(config.copy(gridStyle = gridStyle)) })
                },
            ))),
            DropdownEntry(fields.map { (field, label) ->
                DropdownItem(text = label, selected = config.field == field,
                    onClick = { onConfigChange(config.copy(field = field)) })
            }),
            DropdownEntry(listOf(DropdownItem(
                text = stringResource(R.string.music_sort_descending), selected = config.descending,
                onClick = { onConfigChange(config.copy(descending = !config.descending)) },
            ))),
        ),
        collapseOnSelection = true,
    ) {
        Icon(MiuixIcons.Sort, stringResource(R.string.album_sort_action), Modifier.size(24.dp))
    }
}

@Composable
fun ArtistSortButton(
    config: ArtistSortConfig,
    onConfigChange: (ArtistSortConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fields = listOf(
        ArtistSortField.NAME to stringResource(R.string.artist_sort_name),
        ArtistSortField.SONG_COUNT to stringResource(R.string.artist_sort_song_count),
        ArtistSortField.ALBUM_COUNT to stringResource(R.string.artist_sort_album_count),
    )
    OverlayIconDropdownMenu(
        modifier = modifier,
        entries = listOf(
            DropdownEntry(fields.map { (field, label) ->
                DropdownItem(text = label, selected = config.field == field,
                    onClick = { onConfigChange(config.copy(field = field)) })
            }),
            DropdownEntry(listOf(DropdownItem(
                text = stringResource(R.string.music_sort_descending), selected = config.descending,
                onClick = { onConfigChange(config.copy(descending = !config.descending)) },
            ))),
        ),
        collapseOnSelection = true,
    ) {
        Icon(MiuixIcons.Sort, stringResource(R.string.artist_sort_action), Modifier.size(24.dp))
    }
}

@Composable
fun FolderSortButton(
    config: FolderSortConfig,
    onConfigChange: (FolderSortConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fields = listOf(
        FolderSortField.NAME to stringResource(R.string.folder_sort_name),
        FolderSortField.SONG_COUNT to stringResource(R.string.folder_sort_song_count),
    )
    OverlayIconDropdownMenu(
        modifier = modifier,
        entries = listOf(
            DropdownEntry(fields.map { (field, label) ->
                DropdownItem(text = label, selected = config.field == field,
                    onClick = { onConfigChange(config.copy(field = field)) })
            }),
            DropdownEntry(listOf(DropdownItem(
                text = stringResource(R.string.music_sort_descending), selected = config.descending,
                onClick = { onConfigChange(config.copy(descending = !config.descending)) },
            ))),
        ),
        collapseOnSelection = true,
    ) {
        Icon(MiuixIcons.Sort, stringResource(R.string.folder_sort_action), Modifier.size(24.dp))
    }
}
