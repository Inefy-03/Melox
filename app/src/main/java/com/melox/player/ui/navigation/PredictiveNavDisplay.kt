package com.melox.player.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.rememberNavigationEventState
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.compose.NavigationBackHandler

/**
 * Hosts a single-pane stack with Miuix Navigation3 transitions.
 *
 * One scene host is retained while the setting changes so a visible entry is
 * never registered with two saveable-state providers in the same frame.
 */
@Composable
fun <T : Any> PredictiveNavDisplay(
    backStack: List<T>,
    predictiveBackEnabled: Boolean,
    onBack: () -> Unit,
    entryProvider: (T) -> NavEntry<T>,
    modifier: Modifier = Modifier,
) {
    val entries = rememberDecoratedNavEntries(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
        ),
        entryProvider = entryProvider,
    )

    val sceneState = rememberSceneState(
        entries = entries,
        sceneStrategies = listOf(SinglePaneSceneStrategy()),
        sceneDecoratorStrategies = emptyList(),
        sharedTransitionScope = null,
        onBack = onBack,
    )
    val navigationEventState = rememberNavigationEventState(sceneState)
    val hasPreviousEntries = sceneState.currentScene.previousEntries.isNotEmpty()
    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = predictiveBackHandlerEnabled(
            predictiveBackEnabled = predictiveBackEnabled,
            hasPreviousEntries = hasPreviousEntries,
        ),
        onBackCompleted = {
            repeat(
                sceneState.entries.size -
                    sceneState.currentScene.previousEntries.size,
            ) {
                onBack()
            }
        },
    )
    BackHandler(
        enabled = !predictiveBackEnabled && hasPreviousEntries,
        onBack = onBack,
    )
    NavDisplay(
        sceneState = sceneState,
        navigationEventState = navigationEventState,
        modifier = modifier,
    )
}

internal fun predictiveBackHandlerEnabled(
    predictiveBackEnabled: Boolean,
    hasPreviousEntries: Boolean,
): Boolean = predictiveBackEnabled && hasPreviousEntries
