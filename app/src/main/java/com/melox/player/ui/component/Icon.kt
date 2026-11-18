package com.melox.player.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import com.melox.player.ui.icon.*
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon

internal fun glassIcon(icon: ImageVector): ImageVector {
    if (".Glass." in icon.name) return icon
    return when (icon.name.substringBefore('.')) {
        "Add" -> GlassIcons.Add
        "AddCircle" -> GlassIcons.AddCircle
        "Album" -> GlassIcons.AiScan
        "ArrowRight" -> GlassIcons.ChevronForward
        "Back" -> GlassIcons.Back
        "ChevronBackward" -> GlassIcons.ChevronBackward
        "ChevronForward" -> GlassIcons.ChevronForward
        "Check" -> GlassIcons.Ok
        "Close" -> GlassIcons.Close
        "Contacts" -> GlassIcons.Contacts
        "ContactsCircle" -> GlassIcons.ContactsCircle
        "Create" -> GlassIcons.Create
        "Delete" -> GlassIcons.Delete
        "Download" -> GlassIcons.Download
        "Edit" -> GlassIcons.Edit
        "Folder" -> GlassIcons.Folder
        "FolderFill" -> GlassIcons.FolderFill
        "Hide" -> GlassIcons.Hide
        "Home" -> GlassIcons.HomeFill
        "Info" -> GlassIcons.Info
        "More" -> GlassIcons.More
        "Music" -> GlassIcons.Music
        "Notes" -> GlassIcons.Notes
        "NotesFill" -> GlassIcons.NotesFill
        "Playlist" -> GlassIcons.Playlist
        "RecordingTape" -> GlassIcons.RecordingTape
        "Reset" -> GlassIcons.Reset
        "Search" -> GlassIcons.Search
        "SearchCleanup" -> GlassIcons.Close
        "SelectAll" -> GlassIcons.SelectAll
        "Settings" -> GlassTopBarIcons.SettingsOutline
        "Share" -> GlassIcons.Share
        "Sidebar" -> GlassIcons.Sidebar
        "Sort" -> GlassIcons.Tune
        "Translate" -> GlassIcons.Translate
        "Tune" -> GlassIcons.Tune
        else -> icon
    }
}

internal fun glassSidebarIcon(icon: ImageVector): ImageVector = when (icon.name.substringBefore('.')) {
    "RecordingTape" -> GlassIcons.RecentTasks
    "Album" -> GlassIcons.Item
    "Contacts" -> GlassIcons.NavigationContacts
    "Folder" -> GlassIcons.NavigationFolderFill
    else -> glassIcon(icon)
}

internal fun topBarGlassIcon(icon: ImageVector): ImageVector = when (icon.name.substringBefore('.')) {
    "Back", "ChevronBackward" -> GlassTopBarIcons.ChevronBackward
    "Search" -> GlassTopBarIcons.Search
    "Sort", "Tune" -> GlassIcons.Tune
    "Settings", "SettingsOutline" -> GlassTopBarIcons.SettingsOutline
    "More" -> GlassTopBarIcons.More
    "Add" -> GlassTopBarIcons.Add
    "SelectAll" -> GlassTopBarIcons.SelectAll
    "Close", "SearchCleanup" -> GlassTopBarIcons.Close
    "Check", "Ok" -> GlassTopBarIcons.Ok
    else -> glassIcon(icon)
}

@Composable
internal fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) = MiuixIcon(
    when {
        LocalTopBarButtonHaptics.current -> topBarGlassIcon(imageVector)
        else -> glassIcon(imageVector)
    },
    contentDescription,
    if (LocalTopBarButtonHaptics.current) Modifier.size(28.dp).then(modifier) else modifier,
    tint,
)

@Composable
internal fun Icon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) = MiuixIcon(painter, contentDescription, modifier, tint)

@Composable
internal fun Icon(
    bitmap: ImageBitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) = MiuixIcon(bitmap, contentDescription, modifier, tint)
