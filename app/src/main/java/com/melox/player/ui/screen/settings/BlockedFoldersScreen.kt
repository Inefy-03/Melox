package com.melox.player.ui.screen.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.melox.player.ui.component.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.melox.player.ui.component.TopAppBar
import com.melox.player.ui.component.TopBarBackdrop
import com.melox.player.ui.component.miuixBarColor
import com.melox.player.ui.component.rememberBlurBackdrop
import com.melox.player.ui.screen.library.MusicLibraryEmptyMessage
import com.melox.player.ui.component.Icon
import com.melox.player.ui.component.IconButton
import com.melox.player.ui.icon.GlassIcons
import com.melox.player.ui.icon.Remove
import com.melox.player.ui.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.basic.IconButtonDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import com.melox.player.ui.component.PageScaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import com.melox.player.ui.component.navigationBackIcon
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.blur.layerBackdrop
import com.melox.player.ui.component.OverlayDialog
import com.melox.player.ui.component.OverlayDialogActions
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.melox.player.ui.component.overScrollVertical
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
    var backdropCaptureCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val backdrop = rememberBlurBackdrop(captureCoordinates = { backdropCaptureCoordinates })
    val layoutDirection = LocalLayoutDirection.current
    var pendingPath by remember { mutableStateOf<String?>(null) }
    val sortedPaths = remember(paths) {
        paths.sortedWith(compareBy(String::lowercase).thenBy { it })
    }
    PageScaffold(
        topBar = {
            TopBarBackdrop(backdrop = backdrop) {
                TopAppBar(
                    title = stringResource(R.string.scan_blocked_folder_title),
                    color = backdrop.miuixBarColor(),
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = navigationBackIcon(),
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
                .then(backdrop?.let { Modifier.onGloballyPositioned { coordinates -> backdropCaptureCoordinates = coordinates }.layerBackdrop(it) } ?: Modifier),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .scrollEndHaptic()
                    .overScrollVertical(scrollBehavior = scrollBehavior)
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
        OverlayDialogActions(
            dismissText = stringResource(R.string.clear_queue_confirm_cancel),
            confirmText = stringResource(R.string.clear_queue_confirm_confirm),
            onDismiss = { pendingPath = null },
            onConfirm = {
                pendingPath?.let(onUnblock)
                pendingPath = null
            },
        )
    }
}

@Composable
private fun BlockedFolderRow(path: String, displayPath: String, onRemove: () -> Unit) {
    val normalized = path.trimEnd('/').ifEmpty { "/" }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 30.dp,
                end = GlassTopAppBarDefaults.HorizontalPadding +
                    (GlassTopAppBarDefaults.ButtonSize - IconButtonDefaults.MinWidth) / 2,
                top = 16.dp,
                bottom = 16.dp,
            ),
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
        IconButton(onClick = onRemove, glassEnabled = false) {
            Icon(
                imageVector = GlassIcons.Remove,
                contentDescription = stringResource(R.string.scan_remove_blocked_folder_title),
            )
        }
    }
}
