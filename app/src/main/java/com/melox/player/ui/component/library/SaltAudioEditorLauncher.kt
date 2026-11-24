package com.melox.player.ui.component.library

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.melox.player.R
import com.melox.player.model.MusicTrack

private const val SavedAsCopyExtra = "com.xuncorp.saltaudioanalyzer.extra.SAVED_AS_COPY"

internal fun launchSaltAudioAnalysis(context: Context, track: MusicTrack): Boolean {
    val intent = track.saltAudioIntent(context, Intent.ACTION_VIEW) ?: return false
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

@Composable
internal fun rememberSaltAudioEditorLauncher(
    onLaunched: () -> Unit,
    onOriginalSaved: (Long) -> Unit,
): (MusicTrack) -> Unit {
    val context = LocalContext.current
    var pendingTrackId by rememberSaveable { mutableStateOf<Long?>(null) }
    var awaitingWriteAccess by rememberSaveable { mutableStateOf<Intent?>(null) }
    val showError: (Int) -> Unit = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    val showUnavailable = {
        showError(
            if (context.hasExternalEditor(ExternalAudioAction.SaltAudioEditor)) {
                R.string.salt_audio_analyzer_update_required
            } else {
                R.string.salt_audio_analyzer_not_found
            },
        )
    }
    val editor = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val trackId = pendingTrackId
        pendingTrackId = null
        val saved = result.data
        if (trackId != null && result.resultCode == Activity.RESULT_OK &&
            saved?.data != null && !saved.getBooleanExtra(SavedAsCopyExtra, false)
        ) {
            onOriginalSaved(trackId)
        }
    }
    val launchEditor: (Intent) -> Unit = { intent ->
        try {
            try {
                editor.launch(intent)
            } catch (error: SecurityException) {
                if (intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION == 0) throw error
                editor.launch(Intent(intent).apply { removeFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION) })
            }
            onLaunched()
        } catch (_: ActivityNotFoundException) {
            pendingTrackId = null
            showUnavailable()
        } catch (_: SecurityException) {
            pendingTrackId = null
            showError(R.string.music_external_editor_unavailable)
        }
    }
    val writeAccess = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val intent = awaitingWriteAccess
        awaitingWriteAccess = null
        if (intent != null) {
            if (result.resultCode == Activity.RESULT_OK) {
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            launchEditor(intent)
        }
    }
    return launch@{ track ->
        if (pendingTrackId != null) return@launch
        val intent = track.saltAudioIntent(context, Intent.ACTION_EDIT)
        val audioUri = intent?.data
        if (intent == null || audioUri == null) {
            showError(R.string.music_external_editor_unavailable)
            return@launch
        }
        if (!context.canOpenExternalEditor(intent)) {
            showUnavailable()
            return@launch
        }

        pendingTrackId = track.id
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            audioUri.authority == MediaStore.AUTHORITY &&
            intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION == 0
        ) {
            // MediaStore requires the user's consent before we can forward write access.
            val request = runCatching {
                MediaStore.createWriteRequest(context.contentResolver, listOf(audioUri))
            }.getOrNull()
            if (request != null) {
                awaitingWriteAccess = intent
                try {
                    writeAccess.launch(IntentSenderRequest.Builder(request.intentSender).build())
                    return@launch
                } catch (_: SecurityException) {
                    awaitingWriteAccess = null
                }
            }
        }
        launchEditor(intent)
    }
}
