package com.melox.player

import com.melox.player.model.AppSettings
import com.melox.player.ui.component.library.ExternalEditorKind
import com.melox.player.ui.component.library.visibleExternalEditors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalEditorVisibilityTest {
    @Test
    fun defaultSettingsHideAllEditors() {
        val settings = AppSettings()
        assertFalse(settings.showMusicTagEditor)
        assertFalse(settings.showLyricoEditor)
        assertFalse(settings.showLunaBeatEditor)
        assertTrue(
            visibleExternalEditors(
                settings.showMusicTagEditor,
                settings.showLyricoEditor,
                settings.showLunaBeatEditor,
            ).isEmpty(),
        )
    }

    @Test
    fun hidingMusicTagEditorPreservesLyrico() {
        assertEquals(
            listOf(ExternalEditorKind.Lyrico),
            visibleExternalEditors(
                showMusicTagEditor = false,
                showLyricoEditor = true,
                showLunaBeatEditor = false,
            ),
        )
    }

    @Test
    fun hidingLyricoPreservesMusicTagEditor() {
        assertEquals(
            listOf(ExternalEditorKind.MusicTagEditor),
            visibleExternalEditors(
                showMusicTagEditor = true,
                showLyricoEditor = false,
                showLunaBeatEditor = false,
            ),
        )
    }

    @Test
    fun hidingAllEditorsRemovesAllEntries() {
        assertTrue(visibleExternalEditors(false, false, false).isEmpty())
    }

    @Test
    fun enablingEditorsAgainRestoresTheirOrder() {
        val settings = AppSettings(showMusicTagEditor = false, showLyricoEditor = false)
            .copy(showMusicTagEditor = true, showLyricoEditor = true)
        assertEquals(
            listOf(ExternalEditorKind.MusicTagEditor, ExternalEditorKind.Lyrico),
            visibleExternalEditors(
                settings.showMusicTagEditor,
                settings.showLyricoEditor,
                settings.showLunaBeatEditor,
            ),
        )
    }

    @Test
    fun allSwitchCombinationsKeepOnlyEnabledEditorsInOrder() {
        val editors = listOf(
            ExternalEditorKind.MusicTagEditor,
            ExternalEditorKind.Lyrico,
            ExternalEditorKind.LunaBeat,
        )
        for (mask in 0..7) {
            val enabled = List(3) { index -> mask and (1 shl index) != 0 }
            assertEquals(
                "Switch combination $mask",
                editors.filterIndexed { index, _ -> enabled[index] },
                visibleExternalEditors(enabled[0], enabled[1], enabled[2]),
            )
        }
    }
}
