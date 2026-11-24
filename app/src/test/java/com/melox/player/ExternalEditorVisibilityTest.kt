package com.melox.player

import com.melox.player.model.AppSettings
import com.melox.player.ui.component.library.ExternalAudioAction
import com.melox.player.ui.component.library.visibleExternalAudioActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalEditorVisibilityTest {
    @Test
    fun defaultSettingsEnableAllActions() {
        val settings = AppSettings()
        assertTrue(settings.showMusicTagEditor)
        assertTrue(settings.showLyricoEditor)
        assertTrue(settings.showLunaBeatEditor)
        assertTrue(settings.showSaltAudioEditor)
        assertTrue(settings.showSaltAudioAnalyzer)
        assertEquals(
            ExternalAudioAction.entries,
            visibleExternalAudioActions(
                settings.showMusicTagEditor,
                settings.showLyricoEditor,
                settings.showLunaBeatEditor,
                settings.showSaltAudioEditor,
                settings.showSaltAudioAnalyzer,
            ),
        )
    }

    @Test
    fun hidingMusicTagEditorPreservesLyrico() {
        assertEquals(
            listOf(ExternalAudioAction.Lyrico),
            visibleExternalAudioActions(
                showMusicTagEditor = false,
                showLyricoEditor = true,
                showLunaBeatEditor = false,
                showSaltAudioEditor = false,
                showSaltAudioAnalyzer = false,
            ),
        )
    }

    @Test
    fun hidingLyricoPreservesMusicTagEditor() {
        assertEquals(
            listOf(ExternalAudioAction.MusicTagEditor),
            visibleExternalAudioActions(
                showMusicTagEditor = true,
                showLyricoEditor = false,
                showLunaBeatEditor = false,
                showSaltAudioEditor = false,
                showSaltAudioAnalyzer = false,
            ),
        )
    }

    @Test
    fun hidingAllEditorsRemovesAllEntries() {
        assertTrue(visibleExternalAudioActions(false, false, false, false, false).isEmpty())
    }

    @Test
    fun enablingEditorsAgainRestoresTheirOrder() {
        val settings = AppSettings(
            showMusicTagEditor = false,
            showLyricoEditor = false,
            showLunaBeatEditor = false,
            showSaltAudioEditor = false,
            showSaltAudioAnalyzer = false,
        ).copy(showMusicTagEditor = true, showLyricoEditor = true)
        assertEquals(
            listOf(ExternalAudioAction.MusicTagEditor, ExternalAudioAction.Lyrico),
            visibleExternalAudioActions(
                settings.showMusicTagEditor,
                settings.showLyricoEditor,
                settings.showLunaBeatEditor,
                settings.showSaltAudioEditor,
                settings.showSaltAudioAnalyzer,
            ),
        )
    }

    @Test
    fun allSwitchCombinationsKeepOnlyEnabledActionsInOrder() {
        val editors = listOf(
            ExternalAudioAction.MusicTagEditor,
            ExternalAudioAction.Lyrico,
            ExternalAudioAction.LunaBeat,
            ExternalAudioAction.SaltAudioEditor,
            ExternalAudioAction.SaltAudioAnalyzer,
        )
        for (mask in 0..31) {
            val enabled = List(5) { index -> mask and (1 shl index) != 0 }
            assertEquals(
                "Switch combination $mask",
                editors.filterIndexed { index, _ -> enabled[index] },
                visibleExternalAudioActions(enabled[0], enabled[1], enabled[2], enabled[3], enabled[4]),
            )
        }
    }
}
