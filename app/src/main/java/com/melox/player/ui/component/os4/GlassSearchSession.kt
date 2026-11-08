package com.melox.player.ui.component.os4

import androidx.compose.runtime.*
import com.melox.player.ui.theme.LocalOs4Style

internal class GlassSearchSession(
    val expanded: State<Boolean>,
    val query: State<String>,
    val label: State<String>,
    val onQueryChange: State<(String) -> Unit>,
    val onFocusedChange: State<(Boolean) -> Unit>,
    val onDismiss: State<() -> Unit>,
)

internal class GlassSearchPage {
    var session: GlassSearchSession? by mutableStateOf(null)
    var transitionActive by mutableStateOf(false)
    var progress: State<Float>? by mutableStateOf(null)
    val isExpanded: Boolean get() = session?.expanded?.value == true
}

internal val LocalGlassSearchPage = staticCompositionLocalOf<GlassSearchPage?> { null }

@Composable
internal fun registerGlassSearchSession(
    expanded: Boolean, query: String, label: String,
    onQueryChange: (String) -> Unit, onFocusedChange: (Boolean) -> Unit, onDismiss: () -> Unit,
): Boolean {
    val page = LocalGlassSearchPage.current
    if (!LocalOs4Style.current || page == null) return false
    val expandedState = rememberUpdatedState(expanded)
    val queryState = rememberUpdatedState(query)
    val labelState = rememberUpdatedState(label)
    val queryCallback = rememberUpdatedState(onQueryChange)
    val focusCallback = rememberUpdatedState(onFocusedChange)
    val dismissCallback = rememberUpdatedState(onDismiss)
    val session = remember(page) { GlassSearchSession(expandedState, queryState, labelState, queryCallback, focusCallback, dismissCallback) }
    DisposableEffect(page, session) {
        page.session = session
        onDispose { if (page.session === session) page.session = null }
    }
    return true
}
