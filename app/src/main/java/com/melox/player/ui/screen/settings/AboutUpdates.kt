package com.melox.player.ui.screen.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.melox.player.BuildConfig
import com.melox.player.R
import com.melox.player.data.update.AppRelease
import com.melox.player.data.update.AppUpdates
import com.melox.player.data.update.InstallResult
import com.melox.player.data.update.InvalidUpdateApk
import com.melox.player.data.update.UpdateAssetUnavailable
import com.melox.player.data.update.installUpdate
import com.melox.player.data.update.isNewerThan
import com.melox.player.ui.component.OverlayDialog
import com.melox.player.ui.component.OverlayDialogActions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File
import java.time.LocalDate

@Composable
internal fun AboutUpdates(
    isCurrentPage: Boolean,
    isNavigationSettled: () -> Boolean,
    content: @Composable (available: Boolean, checking: Boolean, onCheck: () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val updates = remember(context.applicationContext) { AppUpdates(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val navigationSettled by rememberUpdatedState(isNavigationSettled)
    var available by remember(updates, isCurrentPage) {
        mutableStateOf(updates.cachedUpdateAvailable(LocalDate.now()))
    }
    var checking by remember(updates, isCurrentPage) { mutableStateOf(false) }
    var dialogOpen by remember { mutableStateOf(false) }
    var dialogRelease by remember { mutableStateOf<AppRelease?>(null) }
    var downloaded by remember { mutableStateOf<Pair<String, File>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<Float?>(null) }

    fun toast(message: Int) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    suspend fun check(manual: Boolean, date: LocalDate = LocalDate.now()) {
        if (checking) return
        checking = true
        try {
            val release = updates.check()?.takeIf { it.isNewerThan(BuildConfig.VERSION_NAME) }
            available = release != null
            updates.cacheAvailability(date, available)
            if (manual) {
                if (release == null) {
                    toast(R.string.about_update_none)
                } else {
                    dialogRelease = release
                    dialogOpen = true
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            available = false
            updates.cacheAvailability(date, available = false)
            if (manual) toast(R.string.about_update_request_failed)
        } finally {
            checking = false
        }
    }

    LaunchedEffect(updates, isCurrentPage) {
        if (!isCurrentPage) return@LaunchedEffect
        snapshotFlow { navigationSettled() }.first { it }
        val date = LocalDate.now()
        if (updates.claimAutomaticCheck(date)) check(manual = false, date = date)
    }
    content(available, isCurrentPage && checking) {
        if (isCurrentPage) scope.launch { check(manual = true) }
    }

    dialogRelease?.let { release ->
        val ready = downloaded?.takeIf { it.first == release.tag && it.second.isFile }?.second
        OverlayDialog(
            show = dialogOpen,
            title = stringResource(R.string.about_update_title, release.tag),
            onDismissRequest = { if (!busy) dialogOpen = false },
            onDismissFinished = { if (!dialogOpen) dialogRelease = null },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (release.notes.isNotBlank()) {
                    Text(
                        text = release.notes,
                        modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
                        style = MiuixTheme.textStyles.body2,
                    )
                }
                val label = when {
                    busy && ready != null -> stringResource(R.string.about_update_installing)
                    busy -> progress?.let { "${(it * 100).toInt()}%" }
                        ?: stringResource(R.string.about_update_downloading)
                    ready != null -> stringResource(R.string.about_update_install)
                    else -> stringResource(R.string.about_update_download)
                }
                OverlayDialogActions(
                    dismissText = stringResource(R.string.about_update_later),
                    confirmText = label,
                    onDismiss = { dialogOpen = false },
                    onConfirm = {
                        busy = true
                        progress = null
                        scope.launch {
                            try {
                                if (ready == null) {
                                    val file = updates.download(release) { fraction ->
                                        withContext(Dispatchers.Main.immediate) { progress = fraction }
                                    }
                                    downloaded?.second?.delete()
                                    downloaded = release.tag to file
                                } else {
                                    when (installUpdate(context, ready)) {
                                        InstallResult.PermissionRequired -> toast(R.string.about_update_install_permission)
                                        InstallResult.Installed, InstallResult.InstallerOpened -> dialogOpen = false
                                    }
                                }
                            } catch (error: CancellationException) {
                                throw error
                            } catch (_: UpdateAssetUnavailable) {
                                toast(R.string.about_update_apk_pending)
                            } catch (_: InvalidUpdateApk) {
                                toast(R.string.about_update_invalid_apk)
                            } catch (_: Exception) {
                                toast(if (ready == null) R.string.about_update_download_failed else R.string.about_update_install_failed)
                            } finally {
                                busy = false
                            }
                        }
                    },
                    dismissEnabled = !busy,
                    confirmEnabled = !busy,
                )
            }
        }
    }
}
