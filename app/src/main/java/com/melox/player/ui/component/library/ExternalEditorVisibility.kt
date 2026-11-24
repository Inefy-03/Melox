package com.melox.player.ui.component.library

internal fun visibleExternalAudioActions(
    showMusicTagEditor: Boolean,
    showLyricoEditor: Boolean,
    showLunaBeatEditor: Boolean,
    showSaltAudioEditor: Boolean,
    showSaltAudioAnalyzer: Boolean,
): List<ExternalAudioAction> = buildList {
    if (showMusicTagEditor) add(ExternalAudioAction.MusicTagEditor)
    if (showLyricoEditor) add(ExternalAudioAction.Lyrico)
    if (showLunaBeatEditor) add(ExternalAudioAction.LunaBeat)
    if (showSaltAudioEditor) add(ExternalAudioAction.SaltAudioEditor)
    if (showSaltAudioAnalyzer) add(ExternalAudioAction.SaltAudioAnalyzer)
}
