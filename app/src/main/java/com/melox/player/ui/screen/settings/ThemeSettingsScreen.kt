package com.melox.player.ui.screen.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.melox.player.R
import com.melox.player.model.AppSettings
import com.melox.player.model.ThemeMode
import com.melox.player.ui.component.MiuixBlurredBar
import com.melox.player.ui.component.miuixBarColor
import com.melox.player.ui.component.rememberMiuixBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun ThemeSettingsScreen(
    settings: AppSettings,
    bottomContentPadding: Dp,
    liquidGlassSupported: Boolean,
    onBack: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onBlurChange: (Boolean) -> Unit,
    onFloatingBottomBarChange: (Boolean) -> Unit,
    onLiquidGlassChange: (Boolean) -> Unit,
    onPredictiveBackChange: (Boolean) -> Unit,
) {
    val dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val blurSupported = liquidGlassSupported
    var blurChecked by remember(settings.blurEnabled) {
        mutableStateOf(settings.blurEnabled)
    }
    var floatingBottomBarChecked by remember(settings.floatingBottomBar) {
        mutableStateOf(settings.floatingBottomBar)
    }
    var liquidGlassChecked by remember(settings.liquidGlass) {
        mutableStateOf(settings.liquidGlass)
    }
    var dynamicColorChecked by remember(settings.dynamicColorEnabled) {
        mutableStateOf(settings.dynamicColorEnabled)
    }
    var predictiveBackChecked by remember(settings.predictiveBackEnabled) {
        mutableStateOf(settings.predictiveBackEnabled)
    }
    var selectedThemeMode by remember(settings.themeMode) {
        mutableStateOf(settings.themeMode)
    }
    val themeModes = listOf(
        ThemeMode.SYSTEM to stringResource(R.string.theme_mode_system),
        ThemeMode.LIGHT to stringResource(R.string.theme_mode_light),
        ThemeMode.DARK to stringResource(R.string.theme_mode_dark),
    )
    val topBarBackdrop = rememberMiuixBlurBackdrop(
        enabled = blurChecked && blurSupported,
    )
    Scaffold(
        topBar = {
            MiuixBlurredBar(backdrop = topBarBackdrop) {
                SmallTopAppBar(
                    title = stringResource(R.string.settings_theme_settings_title),
                    color = topBarBackdrop.miuixBarColor(),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    topBarBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier,
                ),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .scrollEndHaptic()
                    .overScrollVertical(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = maxOf(
                        padding.calculateBottomPadding(),
                        bottomContentPadding,
                    ) + 16.dp,
                ),
                overscrollEffect = null,
            ) {
                item {
                    SmallTitle(text = stringResource(R.string.settings_section_appearance))
                }
                item {
                    ThemeCard {
                        OverlayDropdownPreference(
                            items = themeModes.map { it.second },
                            selectedIndex = themeModes.indexOfFirst {
                                it.first == selectedThemeMode
                            }.coerceAtLeast(0),
                            title = stringResource(R.string.settings_theme_mode_title),
                            onSelectedIndexChange = { index ->
                                themeModes.getOrNull(index)?.first?.let { themeMode ->
                                    if (themeMode != selectedThemeMode) {
                                        selectedThemeMode = themeMode
                                        onThemeModeChange(themeMode)
                                    }
                                }
                            },
                        )
                        SwitchPreference(
                            checked = blurChecked && blurSupported,
                            onCheckedChange = { checked ->
                                blurChecked = checked
                                onBlurChange(checked)
                            },
                            title = stringResource(R.string.settings_blur_title),
                            summary = stringResource(
                                if (blurSupported) {
                                    R.string.settings_blur_summary
                                } else {
                                    R.string.settings_blur_unsupported
                                },
                            ),
                            enabled = blurSupported,
                        )
                        SwitchPreference(
                            checked = floatingBottomBarChecked,
                            onCheckedChange = { checked ->
                                floatingBottomBarChecked = checked
                                if (!checked) liquidGlassChecked = false
                                onFloatingBottomBarChange(checked)
                            },
                            title = stringResource(R.string.settings_floating_bottom_bar_title),
                            summary = stringResource(R.string.settings_floating_bottom_bar_summary),
                        )
                        AnimatedVisibility(
                            visible = floatingBottomBarChecked,
                            enter = fadeIn(animationSpec = tween(200)) +
                                expandVertically(animationSpec = tween(250)),
                            exit = fadeOut(animationSpec = tween(150)) +
                                shrinkVertically(animationSpec = tween(200)),
                            label = "liquidGlassPreferenceVisibility",
                        ) {
                            SwitchPreference(
                                checked = liquidGlassChecked && liquidGlassSupported,
                                onCheckedChange = { checked ->
                                    liquidGlassChecked = checked
                                    onLiquidGlassChange(checked)
                                },
                                title = stringResource(R.string.settings_liquid_glass_title),
                                summary = stringResource(
                                    if (liquidGlassSupported) {
                                        R.string.settings_liquid_glass_summary
                                    } else {
                                        R.string.settings_liquid_glass_unsupported
                                    },
                                ),
                                enabled = liquidGlassSupported,
                            )
                        }
                        SwitchPreference(
                            checked = dynamicColorChecked && dynamicColorSupported,
                            onCheckedChange = { checked ->
                                dynamicColorChecked = checked
                                onDynamicColorChange(checked)
                            },
                            title = stringResource(R.string.settings_dynamic_color_title),
                            summary = stringResource(
                                if (dynamicColorSupported) {
                                    R.string.settings_dynamic_color_summary
                                } else {
                                    R.string.settings_dynamic_color_unsupported
                                },
                            ),
                            enabled = dynamicColorSupported,
                        )
                    }
                }
                item {
                    SmallTitle(text = stringResource(R.string.settings_section_navigation))
                }
                item {
                    ThemeCard {
                        SwitchPreference(
                            checked = predictiveBackChecked,
                            onCheckedChange = { checked ->
                                predictiveBackChecked = checked
                                onPredictiveBackChange(checked)
                            },
                            title = stringResource(R.string.settings_predictive_back_title),
                            summary = stringResource(R.string.settings_predictive_back_summary),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        content = { content() },
    )
}
