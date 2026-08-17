package com.melox.player.ui.component.library

import android.content.ClipData
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
import com.melox.player.model.MusicTrack
import java.io.File

internal const val MusicTagEditorPackage = "com.xjcheng.musictageditor"
internal const val MusicTagEditorActivity =
    "com.xjcheng.musictageditor.SongDetailActivity"
internal const val LyricoPackage = "com.lonx.lyrico"
internal const val LyricoEditTagAction = "com.lonx.lyrico.action.EDIT_TAG"

private const val ExternalEditorUriGrantFlags =
    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
private const val FileProviderSuffix = ".fileprovider"

internal enum class ExternalEditorKind {
    MusicTagEditor,
    Lyrico,
}

private data class ExternalEditorUris(
    val editUri: Uri,
    val mediaStoreUri: Uri?,
)

internal fun launchExternalEditor(
    context: Context,
    track: MusicTrack,
    kind: ExternalEditorKind,
): Boolean {
    val uris = track.externalEditorUris(context) ?: return false
    val candidates = when (kind) {
        ExternalEditorKind.MusicTagEditor -> track.musicTagEditorIntents(context, uris)
        ExternalEditorKind.Lyrico -> listOf(track.lyricoEditorIntent(context, uris))
    }
    val availableCandidates = candidates.filter(context::canOpenExternalEditor)
    return availableCandidates.any { intent ->
        runCatching {
            val launchIntent = Intent(intent).apply {
                addFlags(tagEditorActivityFlags())
            }
            val targetPackage = launchIntent.component?.packageName ?: launchIntent.`package`
            targetPackage?.let { packageName ->
                context.grantExternalEditorUriPermission(packageName, launchIntent.data)
                context.grantExternalEditorUriPermission(
                    packageName,
                    IntentCompat.getParcelableExtra(
                        launchIntent,
                        Intent.EXTRA_STREAM,
                        Uri::class.java,
                    ),
                )
            }
            context.startActivity(launchIntent)
            true
        }.getOrDefault(false)
    }
}

internal fun Context.hasExternalEditor(kind: ExternalEditorKind): Boolean = when (kind) {
    ExternalEditorKind.MusicTagEditor -> runCatching {
        packageManager.getPackageInfo(MusicTagEditorPackage, 0)
    }.isSuccess
    ExternalEditorKind.Lyrico -> runCatching {
        packageManager.getPackageInfo(LyricoPackage, 0)
    }.isSuccess
}

private fun MusicTrack.externalEditorUris(context: Context): ExternalEditorUris? {
    val mediaStoreUri = id.takeIf { it > 0L }?.let { trackId ->
        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackId)
    }
    val fileUri = displayFileLocation()
        ?.let(::File)
        ?.takeIf { it.isFile && it.canRead() }
        ?.let { file ->
            runCatching {
                FileProvider.getUriForFile(
                    context,
                    context.packageName + FileProviderSuffix,
                    file,
                )
            }.getOrNull()
        }
    val editUri = fileUri ?: mediaStoreUri ?: return null
    return ExternalEditorUris(editUri = editUri, mediaStoreUri = mediaStoreUri)
}

private fun MusicTrack.musicTagEditorIntents(
    context: Context,
    uris: ExternalEditorUris,
): List<Intent> {
    val component = ComponentName(MusicTagEditorPackage, MusicTagEditorActivity)
    val label = title ?: fileName ?: "audio"
    fun baseIntent(action: String): Intent = Intent(action).apply {
        addCategory(Intent.CATEGORY_DEFAULT)
        putExternalEditorTrackExtras(this@musicTagEditorIntents, uris.editUri, uris.mediaStoreUri)
        clipData = ClipData.newUri(context.contentResolver, label, uris.editUri)
        addFlags(ExternalEditorUriGrantFlags)
    }
    val editByPath = Intent(Intent.ACTION_EDIT).apply {
        setComponent(component)
        putMusicTagEditorPathExtras(this@musicTagEditorIntents, uris.mediaStoreUri)
        putExternalEditorTrackExtras(this@musicTagEditorIntents, uris.editUri, uris.mediaStoreUri)
        clipData = ClipData.newUri(context.contentResolver, label, uris.editUri)
        addFlags(ExternalEditorUriGrantFlags)
    }
    val explicitView = baseIntent(Intent.ACTION_VIEW).apply {
        setComponent(component)
        setDataAndType(uris.editUri, externalEditorMimeType())
    }
    val packageView = baseIntent(Intent.ACTION_VIEW).apply {
        setPackage(MusicTagEditorPackage)
        setDataAndType(uris.editUri, externalEditorMimeType())
    }
    val packageSend = baseIntent(Intent.ACTION_SEND).apply {
        setPackage(MusicTagEditorPackage)
        type = externalEditorMimeType()
        putExtra(Intent.EXTRA_STREAM, uris.editUri)
    }
    return listOf(editByPath, explicitView, packageView, packageSend)
}

private fun MusicTrack.lyricoEditorIntent(
    context: Context,
    uris: ExternalEditorUris,
): Intent = Intent(LyricoEditTagAction).apply {
    setPackage(LyricoPackage)
    addCategory(Intent.CATEGORY_DEFAULT)
    setDataAndType(uris.editUri, externalEditorMimeType())
    putExtra(Intent.EXTRA_STREAM, uris.editUri)
    putExternalEditorTrackExtras(this@lyricoEditorIntent, uris.editUri, uris.mediaStoreUri)
    clipData = ClipData.newUri(
        context.contentResolver,
        title ?: fileName ?: "audio",
        uris.editUri,
    )
    addFlags(ExternalEditorUriGrantFlags)
}

private fun Intent.putMusicTagEditorPathExtras(
    track: MusicTrack,
    mediaStoreUri: Uri?,
) {
    putExtra("display_name", track.fileName ?: track.title.orEmpty())
    track.displayFileLocation()?.let { filePath ->
        putExtra("filepath", filePath)
        putExtra("path", filePath)
        putExtra("filePath", filePath)
    }
    mediaStoreUri?.let {
        putExtra("uri", it.toString())
        putExtra("mediaStoreUri", it.toString())
    }
}

private fun Intent.putExternalEditorTrackExtras(
    track: MusicTrack,
    editUri: Uri,
    mediaStoreUri: Uri?,
) {
    val displayTitle = listOfNotNull(track.title, track.artist)
        .joinToString(" - ")
        .ifEmpty { track.fileName.orEmpty() }
    putExtra(Intent.EXTRA_TITLE, displayTitle)
    putExtra("title", track.title.orEmpty())
    putExtra("artist", track.artist.orEmpty())
    putExtra("album", track.album.orEmpty())
    track.displayFileLocation()?.let { filePath ->
        putExtra("path", filePath)
        putExtra("filePath", filePath)
    }
    putExtra("id", track.id)
    putExtra("songId", track.id)
    putExtra("mediaId", track.id)
    putExtra("uri", editUri.toString())
    putExtra("contentUrl", editUri.toString())
    putExtra("contentUri", editUri.toString())
    putExtra("contenturl", editUri.toString())
    putExtra("content_uri", editUri.toString())
    mediaStoreUri?.let { putExtra("mediaStoreUri", it.toString()) }
}

private fun MusicTrack.externalEditorMimeType(): String =
    mimeType?.takeIf { it.startsWith("audio/", ignoreCase = true) } ?: "audio/*"

@Suppress("DEPRECATION")
private fun Context.canOpenExternalEditor(intent: Intent): Boolean {
    intent.component?.let { component ->
        if (runCatching { packageManager.getActivityInfo(component, 0) }.isSuccess) {
            return true
        }
    }
    return packageManager.queryIntentActivities(
        intent,
        PackageManager.MATCH_DEFAULT_ONLY,
    ).isNotEmpty()
}

private fun Context.grantExternalEditorUriPermission(packageName: String, uri: Uri?) {
    if (uri == null) return
    runCatching {
        grantUriPermission(packageName, uri, ExternalEditorUriGrantFlags)
    }
}

private fun tagEditorActivityFlags(): Int =
    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
