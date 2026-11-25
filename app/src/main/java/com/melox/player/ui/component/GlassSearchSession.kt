package com.melox.player.ui.component

import androidx.compose.runtime.*

internal class GlassSearchSession(
    val expanded: State<Boolean>,
    val pageActive: State<Boolean>,
    val query: State<String>,
    val focused: State<Boolean>,
    val label: State<String>,
    val onQueryChange: State<(String) -> Unit>,
    val onFocusedChange: State<(Boolean) -> Unit>,
    val onDismiss: State<() -> Unit>,
)

internal class GlassSearchPage {
    var session: GlassSearchSession? by mutableStateOf(null)
    var transitionActive by mutableStateOf(false)
    val isExpanded: Boolean get() = session?.expanded?.value == true
}

internal val LocalGlassSearchPage = staticCompositionLocalOf<GlassSearchPage?> { null }
internal val LocalGlassSearchResults = staticCompositionLocalOf { false }

@Composable
internal fun registerGlassSearchSession(
    expanded: Boolean, pageActive: Boolean, query: String, label: String, focused: Boolean,
    onQueryChange: (String) -> Unit, onFocusedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
): Boolean {
    val page = LocalGlassSearchPage.current
    if (page == null) return false
    val expandedState = rememberUpdatedState(expanded)
    val pageActiveState = rememberUpdatedState(pageActive)
    val queryState = rememberUpdatedState(query)
    val focusedState = rememberUpdatedState(focused)
    val labelState = rememberUpdatedState(label)
    val queryCallback = rememberUpdatedState(onQueryChange)
    val focusCallback = rememberUpdatedState(onFocusedChange)
    val dismissCallback = rememberUpdatedState(onDismiss)
    val session = remember(page) { GlassSearchSession(expandedState, pageActiveState, queryState, focusedState, labelState, queryCallback, focusCallback, dismissCallback) }
    DisposableEffect(page, session) {
        page.session = session
        onDispose { if (page.session === session) page.session = null }
    }
    return true
}
