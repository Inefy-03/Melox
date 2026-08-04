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
import com.melox.player.data.library.MusicSortConfig
import com.melox.player.data.library.MusicSortField
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
fun MusicSortButton(
    config: MusicSortConfig,
    onConfigChange: (MusicSortConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPopup by remember { mutableStateOf(false) }
    val sortEntries = listOf(
        MusicSortField.TITLE to stringResource(R.string.music_sort_title),
        MusicSortField.DATE_ADDED to stringResource(R.string.music_sort_date_added),
        MusicSortField.FILE_NAME to stringResource(R.string.music_sort_file_name),
        MusicSortField.FILE_SIZE to stringResource(R.string.music_sort_file_size),
        MusicSortField.DURATION to stringResource(R.string.music_sort_duration),
    )
    val optionSize = sortEntries.size + 1

    Box(modifier = modifier) {
        OverlayListPopup(
            show = showPopup,
            popupPositionProvider = ListPopupDefaults.ContextMenuPositionProvider,
            alignment = PopupPositionProvider.Align.TopEnd,
            onDismissRequest = { showPopup = false },
        ) {
            ListPopupColumn {
                sortEntries.forEachIndexed { index, (field, label) ->
                    DropdownImpl(
                        text = label,
                        optionSize = optionSize,
                        isSelected = config.field == field,
                        index = index,
                        onSelectedIndexChange = {
                            onConfigChange(config.copy(field = field))
                            showPopup = false
                        },
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    thickness = 1.5.dp,
                )

                DropdownImpl(
                    text = stringResource(R.string.music_sort_descending),
                    optionSize = optionSize,
                    isSelected = config.descending,
                    index = sortEntries.size,
                    onSelectedIndexChange = {
                        onConfigChange(config.copy(descending = !config.descending))
                        showPopup = false
                    },
                )
            }
        }

        IconButton(
            onClick = { showPopup = true },
            holdDownState = showPopup,
        ) {
            Icon(
                imageVector = MiuixIcons.Sort,
                contentDescription = stringResource(R.string.music_sort_action),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
