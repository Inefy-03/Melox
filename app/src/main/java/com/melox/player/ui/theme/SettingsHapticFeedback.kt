package com.melox.player.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

internal val LocalHapticFeedbackEnabled = compositionLocalOf { true }

internal class SettingsHapticFeedback(
    private val delegate: HapticFeedback,
    private val isEnabled: () -> Boolean,
) : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        if (isEnabled()) {
            delegate.performHapticFeedback(hapticFeedbackType)
        }
    }
}
