package com.melox.player.ui.component.library

import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.melox.player.MainActivity
import com.melox.player.model.MusicTrack
import java.io.File

internal fun shareTrackFile(context: Context, track: MusicTrack, chooserTitle: String): Boolean =
    runCatching {
        val source = track.contentUri.toUri()
        val uri = when (source.scheme) {
            "content" -> source
            "file" -> FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(requireNotNull(source.path)),
            )
            else -> return false
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = track.mimeType?.takeIf { it.startsWith("audio/", ignoreCase = true) }
                ?: "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(track.fileName ?: track.title ?: "audio", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            putExtra(
                Intent.EXTRA_EXCLUDE_COMPONENTS,
                arrayOf(ComponentName(context, MainActivity::class.java)),
            )
        }
        context.startActivity(chooser)
        true
    }.getOrDefault(false)
