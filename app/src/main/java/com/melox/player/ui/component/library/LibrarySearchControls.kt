package com.melox.player.ui.component.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.melox.player.R
import com.melox.player.ui.LocalLandscapeNavigation
import com.melox.player.ui.component.Icon
import com.melox.player.ui.component.IconButton
import com.melox.player.ui.component.LocalGlassSearchPage
import com.melox.player.ui.component.publishBottomSearchAction
import com.melox.player.ui.component.registerGlassSearchSession
import com.melox.player.ui.navigation.LocalOrdinaryBackDispatcherOwner
import com.melox.player.ui.navigation.LocalWindowBackDispatcherOwner
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.glass.popupShadowOutsets
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
    if (LocalLandscapeNavigation.current != null) {
        val searchHapticFeedback = LocalHapticFeedback.current
        val searchPage = LocalGlassSearchPage.current
        val materialProgress = top.yukonga.miuix.kmp.glass.LocalGlassTopAppBarContext.current?.materialProgress
        SideEffect { searchPage?.materialProgress = materialProgress }
        val density = LocalDensity.current
        val shadowOutsets = remember(density) {
            with(density) { popupShadowOutsets(top.yukonga.miuix.kmp.glass.GlassShadows.Regular) }
        }
        Box(Modifier.onGloballyPositioned { searchPage?.buttonBounds = it.boundsInRoot() }.graphicsLayer {
            alpha = if (searchPage?.overlayPresentation?.value == true) 0f else 1f
            outsets = shadowOutsets
        }) {
            top.yukonga.miuix.kmp.glass.GlassIconButton(
                onClick = {
                    searchHapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    effectiveOnSearchClick()
                },
                enabled = !visible,
                size = top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults.ButtonSize,
                modifier = (searchPage?.let { Modifier.then(top.yukonga.miuix.kmp.glass.GlassPopupAnchorElement(it.buttonAnchor)) } ?: Modifier)
                    .then(if (visible) Modifier.clearAndSetSemantics { } else Modifier),
            ) {
                Icon(imageVector = MiuixIcons.Search, contentDescription = contentDescription)
            }
        }
        return
    }
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
    query: String,
    label: String,
    onQueryChange: (String) -> Unit,
    onFocusedChange: (Boolean) -> Unit,
    onVisibleChange: (Boolean) -> Unit,
    backEnabled: Boolean = true,
    onBackRequest: (() -> Unit)? = null,
    resultsQuery: String = query,
) {
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
        visible && backEnabled, query, label, onQueryChange, onFocusedChange, resultsQuery,
    ) { onVisibleChange(false) }
}

internal fun shouldClearSearchFocusAfterImeDismissed(
    searchFocused: Boolean,
    imeVisible: Boolean,
    imeWasVisible: Boolean,
): Boolean = searchFocused && imeWasVisible && !imeVisible
