package com.melox.player.ui.component.os4

import androidx.compose.runtime.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.melox.player.ui.theme.LocalOs4Style

internal class GlassSearchSession(
    val expanded: State<Boolean>,
    val query: State<String>,
    val label: State<String>,
    val onQueryChange: State<(String) -> Unit>,
    val onFocusedChange: State<(Boolean) -> Unit>,
    val onDismiss: State<() -> Unit>,
)

internal fun showGlassSearchResults(expanded: Boolean, query: String): Boolean =
    expanded && query.isNotBlank()

internal class GlassSearchPage {
    var session: GlassSearchSession? by mutableStateOf(null)
    var materialProgress: State<Float>? by mutableStateOf(null)
    val buttonAnchor = top.yukonga.miuix.kmp.glass.GlassPopupAnchor()
    var buttonBounds: Rect? = null
    var fieldBounds: Rect? = null
    var overlayPresentation: State<Boolean>? by mutableStateOf(null)
    var transitionActive by mutableStateOf(false)
    val isExpanded: Boolean get() = session?.expanded?.value == true
}

internal class WideGlassSearchHost {
    val pages = mutableStateListOf<GlassSearchPage>()
    val expandedPage: GlassSearchPage? get() = pages.lastOrNull { it.isExpanded }
}

internal val LocalWideGlassSearchHost = staticCompositionLocalOf<WideGlassSearchHost?> { null }

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

/** Observe outside taps without taking clicks or scroll gestures away from page content. */
@Composable
internal fun Modifier.dismissWideSearchOnOutsideTap(
    currentPage: () -> GlassSearchPage?,
    coordinates: () -> LayoutCoordinates?,
): Modifier {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    return pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val page = currentPage()
            val session = page?.session
            val rootPosition = coordinates()?.takeIf { it.isAttached }?.localToRoot(down.position)
            val outside = page?.isExpanded == true && rootPosition != null &&
                page.fieldBounds?.contains(rootPosition) == false
            var moved = false
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.size > 1 || event.changes.any {
                        (it.position - down.position).getDistance() > viewConfiguration.touchSlop
                    }) moved = true
            } while (event.changes.any { it.pressed })
            if (outside && !moved && page.session === session && page.isExpanded) {
                keyboard?.hide()
                focusManager.clearFocus(force = true)
                session?.onFocusedChange?.value?.invoke(false)
                if (session?.query?.value?.isEmpty() == true) session.onDismiss.value()
            }
        }
    }
}
