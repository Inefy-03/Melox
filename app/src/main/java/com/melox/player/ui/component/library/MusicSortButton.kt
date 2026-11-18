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
import com.melox.player.data.library.MusicSortConfig
import com.melox.player.data.library.MusicSortField
import com.melox.player.ui.component.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Sort

@Composable
fun MusicSortButton(
    config: MusicSortConfig,
    onConfigChange: (MusicSortConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sortEntries = listOf(
        MusicSortField.TITLE to stringResource(R.string.music_sort_title),
        MusicSortField.ARTIST to stringResource(R.string.music_sort_artist),
        MusicSortField.FILE_NAME to stringResource(R.string.music_sort_file_name),
        MusicSortField.FILE_SIZE to stringResource(R.string.music_sort_file_size),
        MusicSortField.DATE_ADDED to stringResource(R.string.music_sort_date_added),
        MusicSortField.DURATION to stringResource(R.string.music_sort_duration),
    )
    OverlayIconDropdownMenu(
        modifier = modifier,
        entries = listOf(
            DropdownEntry(sortEntries.map { (field, label) ->
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
        Icon(MiuixIcons.Sort, stringResource(R.string.music_sort_action), Modifier.size(24.dp))
    }
}
