package com.melox.player.ui.screen.settings

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.melox.player.R
import com.melox.player.model.AppSettings
import com.melox.player.model.ScanStatus
import com.melox.player.ui.component.MiuixBlurredBar
import com.melox.player.ui.component.miuixBarColor
import com.melox.player.ui.component.rememberMiuixBlurBackdrop
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

private data class CustomFolderPresentation(
    val uri: String,
    val name: String,
    val path: String,
)

@Composable
fun ScanMusicScreen(
    settings: AppSettings,
    scanStatus: ScanStatus,
    bottomContentPadding: Dp,
    onBack: () -> Unit,
    onRefreshOnStartChange: (Boolean) -> Unit,
    onSkipShortAudioChange: (Boolean) -> Unit,
    onAddCustomFolder: (Uri) -> Unit,
    onRemoveCustomFolder: (String) -> Unit,
    onStartScan: () -> Unit,
) {
    val context = LocalContext.current
    val isScanning = scanStatus is ScanStatus.Scanning
    val scrollBehavior = MiuixScrollBehavior()
    val topBarBackdrop = rememberMiuixBlurBackdrop(enabled = settings.blurEnabled)
    var refreshOnStartChecked by remember(settings.refreshLibraryOnStart) {
        mutableStateOf(settings.refreshLibraryOnStart)
    }
    var skipShortAudioChecked by remember(settings.skipShortAudio) {
        mutableStateOf(settings.skipShortAudio)
    }
    var displayedCustomFolderUris by remember(settings.customFolderUris) {
        mutableStateOf(settings.customFolderUris)
    }
    var pendingRemoval by remember { mutableStateOf<CustomFolderPresentation?>(null) }
    val customFolders = remember(displayedCustomFolderUris) {
        displayedCustomFolderUris.map(::customFolderPresentation)
    }
    val customFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        val uriString = uri.toString()
        if (uriString !in displayedCustomFolderUris) {
            displayedCustomFolderUris = (displayedCustomFolderUris + uriString).sorted()
        }
        onAddCustomFolder(uri)
    }

    Scaffold(
        topBar = {
            MiuixBlurredBar(backdrop = topBarBackdrop) {
                TopAppBar(
                    title = stringResource(R.string.scan_music_page_title),
                    color = topBarBackdrop.miuixBarColor(),
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
                .then(topBarBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = maxOf(
                        padding.calculateBottomPadding(),
                        bottomContentPadding,
                    ) + 16.dp,
                ),
                overscrollEffect = null,
            ) {
                item(key = "scan_title") {
                    SmallTitle(text = stringResource(R.string.scan_music_card_title))
                }
                item(key = "scan_options") {
                    ScanCard {
                        SwitchPreference(
                            checked = refreshOnStartChecked,
                            onCheckedChange = { checked ->
                                refreshOnStartChecked = checked
                                onRefreshOnStartChange(checked)
                            },
                            title = stringResource(R.string.scan_refresh_on_start),
                        )
                        SwitchPreference(
                            checked = skipShortAudioChecked,
                            onCheckedChange = { checked ->
                                skipShortAudioChecked = checked
                                onSkipShortAudioChange(checked)
                            },
                            title = stringResource(R.string.scan_skip_short_audio),
                        )
                    }
                }
                item(key = "custom_folder_title") {
                    SmallTitle(text = stringResource(R.string.scan_custom_folder_title))
                }
                item(key = "custom_folders") {
                    ScanCard(bottomPadding = 0.dp) {
                        ArrowPreference(
                            title = stringResource(R.string.scan_add_custom_folder),
                            onClick = {
                                customFolderLauncher.launch(
                                    Uri.parse(
                                        "content://com.android.externalstorage.documents/root/primary",
                                    ),
                                )
                            },
                        )
                        customFolders.forEach { folder ->
                            CustomFolderRow(
                                folder = folder,
                                onLongClick = { pendingRemoval = folder },
                            )
                        }
                    }
                }
                item(key = "start_scan") {
                    Button(
                        onClick = onStartScan,
                        enabled = !isScanning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(top = 16.dp),
                        colors = ButtonDefaults.buttonColorsPrimary(),
                    ) {
                        Text(
                            text = stringResource(R.string.scan_start),
                        )
                    }
                }
            }
        }
    }

    OverlayDialog(
        show = pendingRemoval != null,
        title = stringResource(R.string.scan_remove_folder_title),
        summary = stringResource(R.string.scan_remove_folder_message),
        enableWindowDim = true,
        onDismissRequest = { pendingRemoval = null },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(
                text = stringResource(R.string.clear_queue_confirm_cancel),
                onClick = { pendingRemoval = null },
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(20.dp))
            TextButton(
                text = stringResource(R.string.clear_queue_confirm_confirm),
                onClick = {
                    pendingRemoval?.uri?.let { uriString ->
                        displayedCustomFolderUris = displayedCustomFolderUris - uriString
                        runCatching {
                            context.contentResolver.releasePersistableUriPermission(
                                Uri.parse(uriString),
                                Intent.FLAG_GRANT_READ_URI_PERMISSION,
                            )
                        }
                        onRemoveCustomFolder(uriString)
                    }
                    pendingRemoval = null
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CustomFolderRow(
    folder: CustomFolderPresentation,
    onLongClick: () -> Unit,
) {
    BasicComponent(
        modifier = Modifier
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick,
            ),
        startAction = {
            Box(modifier = Modifier.padding(end = 8.dp)) {
                Image(
                    painter = painterResource(R.drawable.ic_file),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
    ) {
        Text(
            text = folder.name,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = folder.path,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ScanCard(
    bottomPadding: Dp = 12.dp,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = bottomPadding),
        content = { content() },
    )
}

private fun customFolderPresentation(uriString: String): CustomFolderPresentation {
    val path = customFolderDisplayPath(uriString)
    return CustomFolderPresentation(
        uri = uriString,
        name = path.trimEnd('/').substringAfterLast('/').ifEmpty { path },
        path = path,
    )
}

internal fun customFolderDisplayPath(uriString: String): String {
    val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return uriString
    val documentId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
        ?: return uriString
    val separator = documentId.indexOf(':')
    if (separator <= 0) return uriString
    val volume = documentId.substring(0, separator)
    val path = Uri.decode(documentId.substring(separator + 1)).trim('/')
    return when {
        volume.equals("primary", ignoreCase = true) && path.isEmpty() ->
            "/storage/emulated/0/"
        volume.equals("primary", ignoreCase = true) ->
            "/storage/emulated/0/$path"
        else -> "/storage/$volume/${path}".trimEnd('/')
    }
}
