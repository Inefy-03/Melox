package com.melox.player.ui.component.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.melox.player.R
import com.melox.player.ui.component.Icon
import com.melox.player.ui.component.IconButton
import com.melox.player.ui.component.LocalGlassSearchPage
import com.melox.player.ui.component.publishBottomSearchAction
import com.melox.player.ui.component.registerGlassSearchSession
import com.melox.player.ui.navigation.LocalOrdinaryBackDispatcherOwner
import com.melox.player.ui.navigation.LocalWindowBackDispatcherOwner
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Search

@Composable
internal fun LibrarySearchButton(
    visible: Boolean,
    scrollBehavior: ScrollBehavior,
    contentDescription: String = stringResource(R.string.music_search_hint),
    onClick: () -> Unit,
    bottomActionActive: Boolean = true,
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val effectiveOnSearchClick: () -> Unit = { currentOnClick() }
    if (publishBottomSearchAction(bottomActionActive, visible || LocalGlassSearchPage.current?.transitionActive == true, visible, contentDescription, true, effectiveOnSearchClick)) return
    IconButton(
        onClick = effectiveOnSearchClick,
        enabled = true,
        holdDownState = visible,
    ) {
        Icon(
            imageVector = MiuixIcons.Search,
            contentDescription = contentDescription,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LibrarySearchBar(
    visible: Boolean,
    focused: Boolean,
    queryState: TextFieldState,
    label: String,
    onFocusedChange: (Boolean) -> Unit,
    onVisibleChange: (Boolean) -> Unit,
    backEnabled: Boolean = true,
    onBackRequest: (() -> Unit)? = null,
) {
    if (LocalGlassSearchPage.current == null) return
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val ordinaryBackOwner = LocalOrdinaryBackDispatcherOwner.current
    val searchBackOwner = LocalWindowBackDispatcherOwner.current.takeIf { onBackRequest != null }
        ?: ordinaryBackOwner
        ?: checkNotNull(LocalNavigationEventDispatcherOwner.current)
    val ordinaryBackEnabled = onBackRequest == null && visible && backEnabled &&
        ordinaryBackOwner != null &&
        lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    // Activity 1.13 otherwise registers BackHandler in the disabled entry dispatcher.
    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides searchBackOwner) {
        BackHandler(enabled = ordinaryBackEnabled) {
            onVisibleChange(false)
        }
    }
    registerGlassSearchSession(
        visible, backEnabled, queryState, label, focused, onFocusedChange,
    ) { onVisibleChange(false) }
}

internal fun shouldClearSearchFocusAfterImeDismissed(
    searchFocused: Boolean,
    imeVisible: Boolean,
    imeWasVisible: Boolean,
): Boolean = searchFocused && imeWasVisible && !imeVisible
