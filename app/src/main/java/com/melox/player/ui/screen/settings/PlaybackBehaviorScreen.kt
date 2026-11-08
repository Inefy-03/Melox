package com.melox.player.ui.screen.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import com.melox.player.ui.component.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.melox.player.R
import com.melox.player.model.AppSettings
import com.melox.player.ui.component.AdaptiveTopAppBar
import com.melox.player.ui.component.BlurredBar
import com.melox.player.ui.component.PageCard
import com.melox.player.ui.component.PageScaffold
import com.melox.player.ui.component.miuixBarColor
import com.melox.player.ui.component.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Icon
import com.melox.player.ui.component.os4.IconButton
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import com.melox.player.ui.component.os4.navigationBackIcon
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
internal fun PlaybackBehaviorScreen(
    settings: AppSettings,
    bottomContentPadding: Dp,
    listState: LazyListState,
    scrollBehavior: ScrollBehavior,
    onBack: () -> Unit,
    onAutoPlayOnStartChange: (Boolean) -> Unit,
    onTerminateOnTaskRemovedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    var autoPlay by remember(settings.autoPlayOnStart) { mutableStateOf(settings.autoPlayOnStart) }
    var terminate by remember(settings.terminateOnTaskRemoved) {
        mutableStateOf(settings.terminateOnTaskRemoved)
    }
    var captureCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val backdrop = rememberBlurBackdrop(captureCoordinates = { captureCoordinates })
    PageScaffold(
        modifier = modifier,
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                blurEnabled = backdrop != null,
                scrollBehavior = scrollBehavior,
            ) {
                AdaptiveTopAppBar(
                    title = stringResource(R.string.settings_playback_behavior_title),
                    color = backdrop.miuixBarColor(),
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = navigationBackIcon(),
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().then(
                backdrop?.let {
                    Modifier.onGloballyPositioned { captureCoordinates = it }.layerBackdrop(it)
                } ?: Modifier,
            ),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                state = listState,
                contentPadding = PaddingValues(
                    start = padding.calculateStartPadding(layoutDirection),
                    top = padding.calculateTopPadding() + 12.dp,
                    end = padding.calculateEndPadding(layoutDirection),
                    bottom = maxOf(padding.calculateBottomPadding(), bottomContentPadding) + 16.dp,
                ),
                overscrollEffect = null,
            ) {
                item(key = "auto_play") {
                    PageCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        SwitchPreference(
                            title = stringResource(R.string.settings_auto_play_title),
                            summary = stringResource(R.string.settings_auto_play_summary),
                            checked = autoPlay,
                            onCheckedChange = {
                                autoPlay = it
                                onAutoPlayOnStartChange(it)
                            },
                        )
                    }
                }
                item(key = "terminate_on_task_removed") {
                    PageCard(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(top = 12.dp),
                    ) {
                        SwitchPreference(
                            title = stringResource(R.string.settings_terminate_on_task_removed_title),
                            summary = stringResource(R.string.settings_terminate_on_task_removed_summary),
                            checked = terminate,
                            onCheckedChange = {
                                terminate = it
                                onTerminateOnTaskRemovedChange(it)
                            },
                        )
                    }
                }
            }
        }
    }
}
