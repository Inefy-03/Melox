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
import com.melox.player.model.TransientAudioFocusBehavior
import com.melox.player.ui.screen.playback.AudioPlaybackPreferences
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.melox.player.ui.component.os4.OverlayDropdownPreference
import com.melox.player.ui.component.AdaptiveTopAppBar
import com.melox.player.ui.component.BlurredBar
import com.melox.player.ui.component.PageCard
import com.melox.player.ui.component.PageScaffold
import com.melox.player.ui.component.miuixBarColor
import com.melox.player.ui.component.rememberBlurBackdrop
import com.melox.player.ui.component.os4.Icon
import com.melox.player.ui.component.os4.IconButton
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import com.melox.player.ui.component.os4.navigationBackIcon
import top.yukonga.miuix.kmp.preference.SwitchPreference
import com.melox.player.ui.component.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
internal fun AudioOutputScreen(
    settings: AppSettings,
    bottomContentPadding: Dp,
    listState: LazyListState,
    scrollBehavior: ScrollBehavior,
    onBack: () -> Unit,
    playbackSpeed: Float,
    audioSessionId: Int,
    highPrecisionOutput: Boolean,
    playbackPauseFade: Boolean,
    onAudioFocusEnabledChange: (Boolean) -> Unit,
    onTransientAudioFocusBehaviorChange: (TransientAudioFocusBehavior) -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onHighPrecisionOutputChange: (Boolean) -> Unit,
    onPlaybackPauseFadeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    var audioFocus by remember(settings.audioFocusEnabled) { mutableStateOf(settings.audioFocusEnabled) }
    var behavior by remember(settings.transientAudioFocusBehavior) {
        mutableStateOf(settings.transientAudioFocusBehavior)
    }
    val behaviors = TransientAudioFocusBehavior.entries
    val labels = listOf(
        stringResource(R.string.settings_audio_focus_keep_volume),
        stringResource(R.string.settings_audio_focus_duck),
        stringResource(R.string.settings_audio_focus_pause),
    )
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
                    title = stringResource(R.string.settings_audio_output_title),
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
                item(key = "audio_focus_title") {
                    SmallTitle(text = stringResource(R.string.settings_audio_output_title))
                }
                item(key = "audio_focus") {
                    PageCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        SwitchPreference(
                            title = stringResource(R.string.settings_audio_focus_title),
                            summary = stringResource(R.string.settings_audio_focus_summary),
                            checked = audioFocus,
                            onCheckedChange = {
                                audioFocus = it
                                onAudioFocusEnabledChange(it)
                            },
                        )
                        OverlayDropdownPreference(
                            title = stringResource(R.string.settings_audio_focus_transient_title),
                            items = labels,
                            selectedIndex = behaviors.indexOf(behavior),
                            enabled = audioFocus,
                            onSelectedIndexChange = { index ->
                                behaviors.getOrNull(index)?.let {
                                    behavior = it
                                    onTransientAudioFocusBehaviorChange(it)
                                }
                            },
                        )
                        SwitchPreference(
                            title = stringResource(R.string.playback_pause_fade),
                            checked = playbackPauseFade,
                            onCheckedChange = onPlaybackPauseFadeChange,
                        )
                    }
                }
                item(key = "audio_settings_title") {
                    SmallTitle(
                        text = stringResource(R.string.playback_audio_settings),
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                item(key = "audio_settings") {
                    PageCard(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    ) {
                        AudioPlaybackPreferences(
                            playbackSpeed = playbackSpeed,
                            audioSessionId = audioSessionId,
                            highPrecisionOutput = highPrecisionOutput,
                            speedBackgroundColor = MiuixTheme.colorScheme.surface,
                            onPlaybackSpeedChange = onPlaybackSpeedChange,
                            onHighPrecisionOutputChange = onHighPrecisionOutputChange,
                        )
                    }
                }
            }
        }
    }
}
