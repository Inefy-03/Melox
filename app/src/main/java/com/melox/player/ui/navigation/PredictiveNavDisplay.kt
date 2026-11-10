package com.melox.player.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.melox.player.model.NavigationTransitionStyle
import com.melox.player.ui.isMiuixWideLayout
import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavCornerClipMode
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.NavEntryBuilder
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.nav.transition.NavSettleSpec
import top.yukonga.miuix.kmp.nav.transition.NavMotion
import top.yukonga.miuix.kmp.nav.transition.navGraphicsTransition
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val LocalWindowBackDispatcherOwner =
    compositionLocalOf<NavigationEventDispatcherOwner?> { null }

internal val LocalOrdinaryBackDispatcherOwner =
    compositionLocalOf<NavigationEventDispatcherOwner?> { null }

/** Retains the complete host, including its adaptive chrome and dispatcher ownership. */
@Composable
internal fun rememberNavigationHostContent(content: @Composable () -> Unit): @Composable () -> Unit {
    val latestContent = androidx.compose.runtime.rememberUpdatedState(content)
    return remember {
        androidx.compose.runtime.movableContentOf { latestContent.value() }
    }
}

internal val ImmediateNavigationTransition = navGraphicsTransition(
    motion = NavMotion(programmatic = NavSettleSpec.Tween(0, LinearEasing)),
    scrim = { 0f },
) {}

internal fun secondaryPageTransition(transitionStyle: NavigationTransitionStyle) =
    if (transitionStyle == NavigationTransitionStyle.AOSP) AospNavigationTransition else NavTransitions.MiuixDefault

internal fun secondaryPageEnterMotion(transitionStyle: NavigationTransitionStyle): NavSettleSpec.Tween =
    secondaryPageTransition(transitionStyle).motion.programmatic as NavSettleSpec.Tween

/**
 * Keeps one Miuix navigation host while predictive back is toggled at runtime.
 */
@Composable
fun PredictiveNavDisplay(
    backStack: NavBackStack,
    predictiveBackEnabled: Boolean,
    transitionStyle: NavigationTransitionStyle,
    isDark: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backEnabled: Boolean = true,
    withinPageBackEnabled: Boolean = false,
    content: NavEntryBuilder.() -> Unit,
) {
    val hasPreviousEntries = backStack.size > 1
    val dispatcherOwner = LocalNavigationEventDispatcherOwner.current
    val cornerRadius = rememberNavSystemCornerRadius()
    val useAospTransition = transitionStyle == NavigationTransitionStyle.AOSP
    val windowSize = LocalWindowInfo.current.containerSize
    val transitionCornerRadius = with(LocalDensity.current) {
        navigationTransitionCornerRadius(
            windowWidth = windowSize.width.toDp(),
            windowHeight = windowSize.height.toDp(),
            transitionStyle = transitionStyle,
            systemCornerRadius = cornerRadius,
        )
    }
    val transition = secondaryPageTransition(transitionStyle)
    val effects = NavDisplayEffects(
        cornerClipRadius = transitionCornerRadius,
        cornerClipMode = if (useAospTransition) {
            NavCornerClipMode.All
        } else {
            NavCornerClipMode.Leading
        },
        dimAmount = if (useAospTransition) {
            if (isDark) 0.8f else 0.2f
        } else {
            0.5f
        },
        backdropColor = MiuixTheme.colorScheme.surface,
    )
    val disabledDispatcherOwner = remember {
        object : NavigationEventDispatcherOwner {
            override val navigationEventDispatcher =
                NavigationEventDispatcher().also { it.isEnabled = false }
        }
    }
    val activeDispatcherOwner = dispatcherOwner.takeIf {
        backEnabled && predictiveBackEnabled && !withinPageBackEnabled
    }
        ?: disabledDispatcherOwner
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = backEnabled && predictiveBackEnabled && withinPageBackEnabled,
        onBackCompleted = onBack,
    )
    BackHandler(
        enabled = ordinaryBackHandlerEnabled(
            predictiveBackEnabled = predictiveBackEnabled,
            hasPreviousEntries = hasPreviousEntries,
            backEnabled = backEnabled,
        ),
        onBack = onBack,
    )
    CompositionLocalProvider(
        LocalNavigationEventDispatcherOwner provides activeDispatcherOwner,
        LocalWindowBackDispatcherOwner provides dispatcherOwner.takeIf { backEnabled },
        LocalOrdinaryBackDispatcherOwner provides dispatcherOwner.takeIf {
            backEnabled && !predictiveBackEnabled
        },
    ) {
        NavDisplay(
            backStack = backStack,
            modifier = modifier,
            onBack = onBack,
            transition = transition,
            effects = effects,
            content = content,
        )
    }
}

internal fun navigationTransitionCornerRadius(
    windowWidth: Dp,
    windowHeight: Dp,
    transitionStyle: NavigationTransitionStyle,
    systemCornerRadius: Dp,
): Dp = when {
    transitionStyle == NavigationTransitionStyle.MIUIX &&
        isMiuixWideLayout(windowWidth, windowHeight) -> 0.dp
    transitionStyle == NavigationTransitionStyle.AOSP && systemCornerRadius <= 0.dp -> 32.dp
    else -> systemCornerRadius
}

internal fun predictiveBackHandlerEnabled(
    predictiveBackEnabled: Boolean,
    hasPreviousEntries: Boolean,
    backEnabled: Boolean = true,
): Boolean = backEnabled && predictiveBackEnabled && hasPreviousEntries

internal fun ordinaryBackHandlerEnabled(
    predictiveBackEnabled: Boolean,
    hasPreviousEntries: Boolean,
    backEnabled: Boolean = true,
): Boolean = backEnabled && !predictiveBackEnabled && hasPreviousEntries
