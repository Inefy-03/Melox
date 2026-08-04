package com.melox.player

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import com.melox.player.ui.MeloxApp
import com.melox.player.ui.viewmodel.MeloxViewModel

/** Hosts the single Compose hierarchy for the player. */
class MainActivity : AppCompatActivity() {
    private val viewModel: MeloxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appViewModel = viewModel
        setContent {
            MeloxApp(viewModel = appViewModel)
        }
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
