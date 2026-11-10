package com.melox.player.ui.screen.playback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melox.player.R
import com.melox.player.model.PLAYBACK_SPEED_VALUES
import com.melox.player.model.SleepTimerState
import com.melox.player.ui.component.playback.AudioEffectPreferences
import com.melox.player.ui.component.bottomSheetCardColor
import com.melox.player.ui.component.bottomSheetGlassModifier
import com.melox.player.ui.component.bottomSheetMaterialColor
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.NumberPickerDefaults
import top.yukonga.miuix.kmp.basic.TabRowDefaults
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import com.melox.player.ui.component.os4.OverlayBottomSheet
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.squircle.LocalSquircleEnabled
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.melox.player.ui.component.overScrollVertical
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal fun formatTimerDuration(seconds: Int): String {
    val value = seconds.coerceAtLeast(0)
    return "${(value / 3600).toString().padStart(2, '0')}:" +
        "${((value / 60) % 60).toString().padStart(2, '0')}:" +
        (value % 60).toString().padStart(2, '0')
}

internal fun formatTimerExtension(seconds: Int): String {
    return "+ ${formatTimerDuration(seconds)}"
}

@Composable
private fun RowScope.TimerDigit(
    value: Int,
    modifier: Modifier = Modifier,
    showPlus: Boolean = false,
) {
    val digitStyle = MiuixTheme.textStyles.title1.copy(fontWeight = FontWeight.SemiBold)
    Box(
        modifier = modifier.weight(1f).height(NumberPickerDefaults.ItemHeight),
        contentAlignment = Alignment.Center,
    ) {
        if (showPlus) {
            Row(
                modifier = Modifier.wrapContentWidth(unbounded = true),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "+", style = digitStyle)
                Spacer(Modifier.width(4.dp))
                Text(text = value.toString().padStart(2, '0'), style = digitStyle)
                // Balance the leading glyph without shifting the hour digits.
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "+",
                    style = digitStyle,
                    color = Color.Transparent,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
        } else {
            Text(text = value.toString().padStart(2, '0'), style = digitStyle)
        }
    }
}

@Composable
private fun TimerSeparator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.width(12.dp).height(NumberPickerDefaults.ItemHeight),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = ":", fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TimerTimeRow(
    seconds: Int,
    modifier: Modifier = Modifier,
    showPlus: Boolean = false,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimerDigit(seconds / 3600, showPlus = showPlus)
        TimerSeparator()
        TimerDigit(seconds / 60 % 60)
        TimerSeparator()
        TimerDigit(seconds % 60)
    }
}

@Composable
internal fun SleepTimerSheet(
    show: Boolean,
    timerSeconds: Int,
    timerState: SleepTimerState,
    autoExtendSleepTimer: Boolean,
    onDismiss: () -> Unit,
    onTimerSecondsChange: (Int) -> Unit,
    onStartTimer: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onAutoExtendSleepTimerChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val hourPicker = remember(show) { TimerNumberPickerState(timerSeconds / 3600, 0..23) }
    val minutePicker = remember(show) { TimerNumberPickerState(timerSeconds / 60 % 60, 0..59) }
    val secondPicker = remember(show) { TimerNumberPickerState(timerSeconds % 60, 0..59) }
    val startScope = rememberCoroutineScope()
    var startJob by remember { mutableStateOf<Job?>(null) }
    DisposableEffect(show) {
        onDispose {
            startJob?.cancel()
            hourPicker.unlock()
            minutePicker.unlock()
            secondPicker.unlock()
        }
    }
    val pickerDuration = {
        hourPicker.value * 3600 + minutePicker.value * 60 + secondPicker.value
    }
    var lastActiveTimerSeconds by remember(show) {
        mutableIntStateOf(timerState.remainingSeconds)
    }
    var lastActiveTimerWasExtended by remember(show) {
        mutableStateOf(timerState.extending)
    }
    SideEffect {
        if (timerState.active) {
            lastActiveTimerSeconds = if (timerState.extending) {
                timerState.extensionSeconds
            } else {
                timerState.remainingSeconds
            }
            lastActiveTimerWasExtended = timerState.extending
        }
    }
    val selectedTimerSeconds = pickerDuration()
    val timerCollapseProgress by animateFloatAsState(
        targetValue = if (timerState.active) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "timerCollapse",
    )
    val expandedTimerHeight = NumberPickerDefaults.ItemHeight * 3 + 24.dp
    val collapsedTimerHeight = 64.dp
    val timerCardHeight = expandedTimerHeight +
        (collapsedTimerHeight - expandedTimerHeight) * timerCollapseProgress
    val pickerAlpha = (1f - timerCollapseProgress / 0.2f).coerceIn(0f, 1f)
    val overlayAlpha = 1f - pickerAlpha
    val selectedTimeFraction = ((1f - timerCollapseProgress) / 0.75f).coerceIn(0f, 1f)
    OverlayBottomSheet(
        show = show,
        modifier = modifier.then(bottomSheetGlassModifier()),
        backgroundColor = bottomSheetMaterialColor(),
        title = stringResource(R.string.playback_sleep_timer),
        enableWindowDim = true,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .overScrollVertical(
                    nestedScrollToParent = false,
                    isEnabled = { scrollState.maxValue > 0 },
                )
                .verticalScroll(scrollState, overscrollEffect = null)
                .padding(bottom = bottomPadding + 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(timerCardHeight)
                    .squircleClip(cornerRadius = CardDefaults.CornerRadius),
                colors = CardDefaults.defaultColors(color = bottomSheetCardColor()),
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .requiredHeight(expandedTimerHeight)
                            .padding(12.dp)
                            .graphicsLayer { alpha = pickerAlpha },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TimerNumberPicker(
                            state = hourPicker,
                            onValueChange = { onTimerSecondsChange(pickerDuration()) },
                            modifier = Modifier.weight(1f),
                            enabled = !timerState.active,
                        )
                        TimerSeparator()
                        TimerNumberPicker(
                            state = minutePicker,
                            onValueChange = { onTimerSecondsChange(pickerDuration()) },
                            modifier = Modifier.weight(1f),
                            enabled = !timerState.active,
                        )
                        TimerSeparator()
                        TimerNumberPicker(
                            state = secondPicker,
                            onValueChange = { onTimerSecondsChange(pickerDuration()) },
                            modifier = Modifier.weight(1f),
                            enabled = !timerState.active,
                        )
                    }
                    val expandedTimeWidth = maxWidth - 24.dp
                    val compactTimeWidth = minOf(168.dp, expandedTimeWidth)
                    val timeRowCompactness =
                        ((timerCollapseProgress - 0.2f) / 0.8f).coerceIn(0f, 1f)
                    val timeWidth = expandedTimeWidth +
                        (compactTimeWidth - expandedTimeWidth) * timeRowCompactness
                    val timeRowModifier = Modifier
                        .align(Alignment.Center)
                        .width(timeWidth)
                    TimerTimeRow(
                        seconds = if (timerState.active) {
                            if (timerState.extending) timerState.extensionSeconds
                            else timerState.remainingSeconds
                        } else {
                            lastActiveTimerSeconds
                        },
                        showPlus = if (timerState.active) timerState.extending
                        else lastActiveTimerWasExtended,
                        modifier = timeRowModifier.graphicsLayer {
                            alpha = overlayAlpha *
                                if (timerState.active) 1f else 1f - selectedTimeFraction
                        },
                    )
                    if (!timerState.active) {
                        TimerTimeRow(
                            seconds = selectedTimerSeconds,
                            modifier = timeRowModifier.graphicsLayer {
                                alpha = overlayAlpha * selectedTimeFraction
                            },
                        )
                    }
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.defaultColors(color = bottomSheetCardColor()),
            ) {
                SwitchPreference(
                    title = stringResource(R.string.playback_timer_auto_extend),
                    checked = autoExtendSleepTimer,
                    onCheckedChange = onAutoExtendSleepTimerChange,
                )
            }
            Button(
                onClick = {
                    if (timerState.active) {
                        startJob?.cancel()
                        hourPicker.unlock()
                        minutePicker.unlock()
                        secondPicker.unlock()
                        onCancelTimer()
                    } else {
                        if (hourPicker.inputLocked) return@Button
                        val duration = hourPicker.captureForStart() * 3600 +
                            minutePicker.captureForStart() * 60 + secondPicker.captureForStart()
                        if (duration > 0) {
                            onTimerSecondsChange(duration)
                            startJob = startScope.launch {
                                var started = false
                                try {
                                    coroutineScope {
                                        val hours = async { hourPicker.alignForStart() }
                                        val minutes = async { minutePicker.alignForStart() }
                                        val seconds = async { secondPicker.alignForStart() }
                                        hours.await()
                                        minutes.await()
                                        seconds.await()
                                    }
                                    onStartTimer(duration)
                                    started = true
                                } finally {
                                    if (!started) {
                                        hourPicker.unlock()
                                        minutePicker.unlock()
                                        secondPicker.unlock()
                                    }
                                }
                            }
                        } else {
                            hourPicker.unlock()
                            minutePicker.unlock()
                            secondPicker.unlock()
                        }
                    }
                },
                enabled = timerState.active || selectedTimerSeconds > 0,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(
                    text = stringResource(
                        if (timerState.active) R.string.playback_sleep_timer_stop
                        else R.string.playback_sleep_timer_start,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
internal fun AudioSettingsSheet(
    show: Boolean,
    playbackSpeed: Float,
    audioSessionId: Int,
    playbackPauseFade: Boolean,
    onDismiss: () -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onPlaybackPauseFadeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    OverlayBottomSheet(
        show = show,
        modifier = modifier.then(bottomSheetGlassModifier()),
        backgroundColor = bottomSheetMaterialColor(),
        title = stringResource(R.string.playback_audio_settings),
        enableWindowDim = true,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .overScrollVertical(
                    nestedScrollToParent = false,
                    isEnabled = { scrollState.maxValue > 0 },
                )
                .verticalScroll(scrollState, overscrollEffect = null)
                .padding(bottom = bottomPadding + 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlaybackSpeedSelector(
                playbackSpeed = playbackSpeed,
                backgroundColor = bottomSheetCardColor(),
                onPlaybackSpeedChange = onPlaybackSpeedChange,
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.defaultColors(color = bottomSheetCardColor()),
            ) {
                AudioEffectPreferences(audioSessionId = audioSessionId)
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.defaultColors(color = bottomSheetCardColor()),
            ) {
                SwitchPreference(
                    title = stringResource(R.string.playback_pause_fade),
                    checked = playbackPauseFade,
                    onCheckedChange = onPlaybackPauseFadeChange,
                )
            }
        }
    }
}

@Composable
internal fun PlaybackSpeedSelector(
    playbackSpeed: Float,
    backgroundColor: Color,
    onPlaybackSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val speedLabels = stringArrayResource(R.array.playback_speed_options).toList()
    val speedDescription = stringResource(R.string.playback_speed_title)
    val speedBackgroundColor = backgroundColor.let { color ->
        // Miuix 0.9.4's squircle-background shader returns an unpremultiplied color.
        if (isRuntimeShaderSupported() && LocalSquircleEnabled.current) {
            color.copy(
                red = color.red * color.alpha,
                green = color.green * color.alpha,
                blue = color.blue * color.alpha,
            )
        } else {
            color
        }
    }
    Box(
        modifier = modifier.fillMaxWidth().semantics { contentDescription = speedDescription },
    ) {
        TabRowWithContour(
            tabs = speedLabels,
            selectedTabIndex = PLAYBACK_SPEED_VALUES.indexOf(playbackSpeed)
                .takeIf { it >= 0 } ?: PLAYBACK_SPEED_VALUES.indexOf(1f),
            onTabSelected = { index -> onPlaybackSpeedChange(PLAYBACK_SPEED_VALUES[index]) },
            colors = TabRowDefaults.tabRowColors(backgroundColor = speedBackgroundColor),
        )
    }
}

@Composable
internal fun AudioPlaybackPreferences(
    playbackSpeed: Float,
    audioSessionId: Int,
    highPrecisionOutput: Boolean,
    speedBackgroundColor: Color,
    onPlaybackSpeedChange: (Float) -> Unit,
    onHighPrecisionOutputChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        PlaybackSpeedSelector(
            playbackSpeed = playbackSpeed,
            backgroundColor = speedBackgroundColor,
            onPlaybackSpeedChange = onPlaybackSpeedChange,
            modifier = Modifier.padding(12.dp),
        )
        AudioEffectPreferences(audioSessionId = audioSessionId)
        SwitchPreference(
            title = stringResource(R.string.playback_high_precision_output),
            checked = highPrecisionOutput,
            onCheckedChange = onHighPrecisionOutputChange,
        )
    }
}
