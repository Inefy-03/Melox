package com.melox.player.ui.screen.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melox.player.R
import com.melox.player.ui.component.AdaptiveTopAppBar
import com.melox.player.ui.component.BlurredBar
import com.melox.player.ui.component.miuixBarColor
import com.melox.player.ui.component.rememberBlurBackdrop
import com.melox.player.ui.screen.library.MusicLibraryEmptyMessage
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import com.melox.player.ui.component.PageScaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun BlockedFoldersScreen(
    paths: List<String>,
    displayPaths: Map<String, String>,
    bottomContentPadding: Dp,
    onBack: () -> Unit,
    onUnblock: (String) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val layoutDirection = LocalLayoutDirection.current
    var pendingPath by remember { mutableStateOf<String?>(null) }
    val sortedPaths = remember(paths) {
        paths.sortedWith(compareBy(String::lowercase).thenBy { it })
    }
    PageScaffold(
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                blurEnabled = backdrop != null,
                scrollBehavior = scrollBehavior,
            ) {
                AdaptiveTopAppBar(
                    title = stringResource(R.string.scan_blocked_folder_title),
                    color = backdrop.miuixBarColor(),
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    start = padding.calculateStartPadding(layoutDirection),
                    top = padding.calculateTopPadding() + 12.dp,
                    end = padding.calculateEndPadding(layoutDirection),
                    bottom = maxOf(padding.calculateBottomPadding(), bottomContentPadding) + 16.dp,
                ),
                overscrollEffect = null,
            ) {
                if (sortedPaths.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            MusicLibraryEmptyMessage(
                                icon = MiuixIcons.Folder,
                                text = stringResource(R.string.scan_blocked_folder_empty),
                            )
                        }
                    }
                } else {
                    items(sortedPaths, key = { it }) { path ->
                        BlockedFolderRow(path = path, displayPath = displayPaths[path] ?: path, onRemove = { pendingPath = path })
                    }
                }
            }
        }
    }
    OverlayDialog(
        show = pendingPath != null,
        title = stringResource(R.string.scan_remove_blocked_folder_title),
        summary = stringResource(
            R.string.scan_remove_blocked_folder_message,
            displayPaths[pendingPath] ?: pendingPath.orEmpty(),
        ),
        enableWindowDim = true,
        onDismissRequest = { pendingPath = null },
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(
                text = stringResource(R.string.clear_queue_confirm_cancel),
                onClick = { pendingPath = null },
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(20.dp))
            TextButton(
                text = stringResource(R.string.clear_queue_confirm_confirm),
                onClick = {
                    pendingPath?.let(onUnblock)
                    pendingPath = null
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}

@Composable
private fun BlockedFolderRow(path: String, displayPath: String, onRemove: () -> Unit) {
    val normalized = path.trimEnd('/').ifEmpty { "/" }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 30.dp, end = 28.dp, top = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_file),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = normalized.substringAfterLast('/').ifEmpty { normalized },
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = displayPath,
                style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                painter = painterResource(R.drawable.ic_remove_circle),
                contentDescription = stringResource(R.string.scan_remove_blocked_folder_title),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
