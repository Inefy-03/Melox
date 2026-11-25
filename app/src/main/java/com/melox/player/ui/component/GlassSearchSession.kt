package com.melox.player.ui.component

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.*

internal class GlassSearchSession(
    val expanded: State<Boolean>,
    val pageActive: State<Boolean>,
    val queryState: State<TextFieldState>,
    val focused: State<Boolean>,
    val label: State<String>,
    val onFocusedChange: State<(Boolean) -> Unit>,
    val onDismiss: State<() -> Unit>,
)

internal class GlassSearchPage {
    var session: GlassSearchSession? by mutableStateOf(null)
    var transitionActive by mutableStateOf(false)
    val isExpanded: Boolean get() = session?.expanded?.value == true
}

internal val LocalGlassSearchPage = staticCompositionLocalOf<GlassSearchPage?> { null }
internal val LocalGlassSearchQuery = compositionLocalOf<String?> { null }

@Composable
internal fun registerGlassSearchSession(
    expanded: Boolean, pageActive: Boolean, queryState: TextFieldState, label: String, focused: Boolean,
    onFocusedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
): Boolean {
    val page = LocalGlassSearchPage.current
    if (page == null) return false
    val expandedState = rememberUpdatedState(expanded)
    val pageActiveState = rememberUpdatedState(pageActive)
    val editor = rememberUpdatedState(queryState)
    val focusedState = rememberUpdatedState(focused)
    val labelState = rememberUpdatedState(label)
    val focusCallback = rememberUpdatedState(onFocusedChange)
    val dismissCallback = rememberUpdatedState(onDismiss)
    val session = remember(page) { GlassSearchSession(expandedState, pageActiveState, editor, focusedState, labelState, focusCallback, dismissCallback) }
    DisposableEffect(page, session) {
        page.session = session
        onDispose { if (page.session === session) page.session = null }
    }
    return true
}
