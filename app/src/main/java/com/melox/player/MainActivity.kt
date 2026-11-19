package com.melox.player

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.core.content.IntentCompat
import com.melox.player.ui.LocalAdaptiveWindowSize
import com.melox.player.ui.MeloxApp
import com.melox.player.ui.viewmodel.MeloxViewModel

/** Hosts the single Compose hierarchy for the player. */
class MainActivity : AppCompatActivity() {
    private val viewModel: MeloxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appViewModel = viewModel
        setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val size = IntSize(constraints.maxWidth, constraints.maxHeight)
                CompositionLocalProvider(LocalAdaptiveWindowSize provides size) {
                    MeloxApp(viewModel = appViewModel)
                }
            }
        }
        appViewModel.applyStartupPlayback(allowAutoPlay = intent.externalAudioUri() == null)
        if (savedInstanceState == null) {
            playAudioFrom(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        playAudioFrom(intent)
    }

    private fun playAudioFrom(intent: Intent) {
        intent.externalAudioUri()?.let { uri ->
            if (
                intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0 &&
                intent.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0
            ) {
                runCatching {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
            }
            viewModel.playExternalAudio(uri)
        }
    }
}

private fun Intent.externalAudioUri(): Uri? = when (action) {
    Intent.ACTION_VIEW -> data
    Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(
        this,
        Intent.EXTRA_STREAM,
        Uri::class.java,
    ) ?: clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri

    else -> null
}
