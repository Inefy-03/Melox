package com.melox.player.ui.component.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.overlay.OverlayListPopup

@Composable
fun AlbumSortButton(
    config: AlbumSortConfig,
    onConfigChange: (AlbumSortConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridStyles = listOf(
        AlbumGridStyle.TWO_SMALL to stringResource(R.string.album_grid_two_small),
        AlbumGridStyle.THREE to stringResource(R.string.album_grid_three),
    )
    val fields = listOf(
        AlbumSortField.ALBUM to stringResource(R.string.album_sort_album),
        AlbumSortField.ALBUM_ARTIST to stringResource(R.string.album_sort_album_artist),
        AlbumSortField.SONG_COUNT to stringResource(R.string.album_sort_song_count),
        AlbumSortField.YEAR to stringResource(R.string.album_sort_year),
    )
    LibrarySortPopup(
        modifier = modifier,
        contentDescription = stringResource(R.string.album_sort_action),
        popupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
    ) { dismiss ->
        gridStyles.forEachIndexed { index, (gridStyle, label) ->
            DropdownImpl(
                text = label,
                optionSize = gridStyles.size + fields.size + 1,
                isSelected = config.gridStyle == gridStyle,
                index = index,
                onSelectedIndexChange = {
                    onConfigChange(config.copy(gridStyle = gridStyle))
                    dismiss()
                },
            )
        }
        PopupDivider()
        fields.forEachIndexed { index, (field, label) ->
            DropdownImpl(
                text = label,
                optionSize = gridStyles.size + fields.size + 1,
                isSelected = config.field == field,
                index = gridStyles.size + index,
                onSelectedIndexChange = {
                    onConfigChange(config.copy(field = field))
                    dismiss()
                },
            )
        }
        PopupDivider()
        DropdownImpl(
            text = stringResource(R.string.music_sort_descending),
            optionSize = gridStyles.size + fields.size + 1,
            isSelected = config.descending,
            index = gridStyles.size + fields.size,
            onSelectedIndexChange = {
                onConfigChange(config.copy(descending = !config.descending))
                dismiss()
            },
        )
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
    LibrarySortPopup(
        modifier = modifier,
        contentDescription = stringResource(R.string.artist_sort_action),
    ) { dismiss ->
        fields.forEachIndexed { index, (field, label) ->
            DropdownImpl(
                text = label,
                optionSize = fields.size + 1,
                isSelected = config.field == field,
                index = index,
                onSelectedIndexChange = {
                    onConfigChange(config.copy(field = field))
                    dismiss()
                },
            )
        }
        PopupDivider()
        DropdownImpl(
            text = stringResource(R.string.music_sort_descending),
            optionSize = fields.size + 1,
            isSelected = config.descending,
            index = fields.size,
            onSelectedIndexChange = {
                onConfigChange(config.copy(descending = !config.descending))
                dismiss()
            },
        )
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
    LibrarySortPopup(
        modifier = modifier,
        contentDescription = stringResource(R.string.folder_sort_action),
    ) { dismiss ->
        fields.forEachIndexed { index, (field, label) ->
            DropdownImpl(
                text = label,
                optionSize = fields.size + 1,
                isSelected = config.field == field,
                index = index,
                onSelectedIndexChange = {
                    onConfigChange(config.copy(field = field))
                    dismiss()
                },
            )
        }
        PopupDivider()
        DropdownImpl(
            text = stringResource(R.string.music_sort_descending),
            optionSize = fields.size + 1,
            isSelected = config.descending,
            index = fields.size,
            onSelectedIndexChange = {
                onConfigChange(config.copy(descending = !config.descending))
                dismiss()
            },
        )
    }
}

@Composable
private fun LibrarySortPopup(
    contentDescription: String,
    modifier: Modifier = Modifier,
    popupPositionProvider: PopupPositionProvider = ListPopupDefaults.ContextMenuPositionProvider,
    content: @Composable (dismiss: () -> Unit) -> Unit,
) {
    var showPopup by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OverlayListPopup(
            show = showPopup,
            popupPositionProvider = popupPositionProvider,
            alignment = PopupPositionProvider.Align.TopEnd,
            onDismissRequest = { showPopup = false },
        ) {
            ListPopupColumn {
                content { showPopup = false }
            }
        }
        IconButton(
            onClick = { showPopup = true },
            holdDownState = showPopup,
        ) {
            Icon(
                imageVector = MiuixIcons.Sort,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun PopupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        thickness = 1.5.dp,
    )
}
