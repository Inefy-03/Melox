package com.melox.player.ui.screen.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.melox.player.R
import com.melox.player.model.AppSettings
import com.melox.player.model.CoverShape
import com.melox.player.model.DynamicColorSource
import com.melox.player.model.DynamicFlowStyle
import com.melox.player.model.NavigationTransitionStyle
import com.melox.player.model.PlaybackBackgroundStyle
import com.melox.player.model.ThemeStyle
import com.melox.player.model.ThemeMode
import com.melox.player.ui.component.AdaptiveTopAppBar
import com.melox.player.ui.component.BlurredBar
import com.melox.player.ui.component.miuixBarColor
import com.melox.player.ui.component.rememberBlurBackdrop
import com.melox.player.ui.component.PageScaffold
import com.melox.player.ui.component.PageCard as Card
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import com.melox.player.ui.component.os4.IconButton
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import com.melox.player.ui.component.os4.navigationBackIcon
import com.melox.player.ui.component.os4.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun ThemeSettingsScreen(
    settings: AppSettings,
    bottomContentPadding: Dp,
    listState: LazyListState,
    scrollBehavior: ScrollBehavior,
    onBack: () -> Unit,
    onThemeStyleChange: (ThemeStyle) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onImmersiveModeChange: (Boolean) -> Unit,
    onHideStatusBarChange: (Boolean) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onDynamicColorSourceChange: (DynamicColorSource) -> Unit,
    onPlaybackBackgroundStyleChange: (PlaybackBackgroundStyle) -> Unit,
    onDynamicFlowStyleChange: (DynamicFlowStyle) -> Unit,
    onCoverShapeChange: (CoverShape) -> Unit,
    onArtworkFlipAnimationChange: (Boolean) -> Unit,
    onShowSongListArtworkChange: (Boolean) -> Unit,
    onShowSongListFileNameChange: (Boolean) -> Unit,
    onShowSongListDurationChange: (Boolean) -> Unit,
    onPredictiveBackChange: (Boolean) -> Unit,
    onNavigationTransitionStyleChange: (NavigationTransitionStyle) -> Unit,
    onOpenMainBackground: () -> Unit,
    onOpenInterfaceStyle: () -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    var immersiveModeChecked by remember(settings.immersiveMode) {
        mutableStateOf(settings.immersiveMode)
    }
    var hideStatusBarChecked by remember(settings.hideStatusBar) {
        mutableStateOf(settings.hideStatusBar)
    }
    var dynamicColorChecked by remember(settings.dynamicColorEnabled) {
        mutableStateOf(settings.dynamicColorEnabled)
    }
    var dynamicColorSource by remember(settings.dynamicColorSource) {
        mutableStateOf(settings.dynamicColorSource)
    }
    var playbackBackgroundStyle by remember(settings.playbackBackgroundStyle) {
        mutableStateOf(settings.playbackBackgroundStyle)
    }
    var dynamicFlowStyle by remember(settings.dynamicFlowStyle) {
        mutableStateOf(settings.dynamicFlowStyle)
    }
    var songListArtworkChecked by remember(settings.showSongListArtwork) {
        mutableStateOf(settings.showSongListArtwork)
    }
    var coverShape by remember(settings.coverShape) {
        mutableStateOf(settings.coverShape)
    }
    var artworkFlipAnimationChecked by remember(settings.artworkFlipAnimation) {
        mutableStateOf(settings.artworkFlipAnimation)
    }
    var songListFileNameChecked by remember(settings.showSongListFileName) {
        mutableStateOf(settings.showSongListFileName)
    }
    var songListDurationChecked by remember(settings.showSongListDuration) {
        mutableStateOf(settings.showSongListDuration)
    }
    var predictiveBackChecked by remember(settings.predictiveBackEnabled) {
        mutableStateOf(settings.predictiveBackEnabled)
    }
    var navigationTransitionStyle by remember(settings.navigationTransitionStyle) {
        mutableStateOf(settings.navigationTransitionStyle)
    }
    var selectedThemeMode by remember(settings.themeMode) {
        mutableStateOf(settings.themeMode)
    }
    val systemDark = isSystemInDarkTheme()
    val playbackBackgroundStyles = listOf(
        PlaybackBackgroundStyle.BLURRED_ARTWORK to
            stringResource(R.string.settings_playback_background_blurred_artwork),
        PlaybackBackgroundStyle.DYNAMIC_FLOW to
            stringResource(R.string.settings_playback_background_dynamic_flow),
    )
    val dynamicFlowStyles = listOf(
        DynamicFlowStyle.SYSTEM to stringResource(R.string.theme_mode_system),
        DynamicFlowStyle.LIGHT to stringResource(R.string.settings_dynamic_flow_light),
        DynamicFlowStyle.DARK to stringResource(R.string.settings_dynamic_flow_dark),
    )
    val coverShapes = listOf(
        CoverShape.DEFAULT to stringResource(R.string.settings_cover_shape_default),
        CoverShape.ROUNDED_RECT to stringResource(R.string.settings_cover_shape_rounded),
        CoverShape.CIRCLE to stringResource(R.string.settings_cover_shape_circle),
    )
    var topBarBackdropCaptureCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val topBarBackdrop = rememberBlurBackdrop(captureCoordinates = { topBarBackdropCaptureCoordinates })
    PageScaffold(
            topBar = {
            BlurredBar(
                backdrop = topBarBackdrop,
                blurEnabled = topBarBackdrop != null,
                scrollBehavior = scrollBehavior,
            ) {
                AdaptiveTopAppBar(
                    title = stringResource(R.string.settings_theme_settings_title),
                    color = topBarBackdrop.miuixBarColor(),
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
            modifier = Modifier
                .fillMaxSize()
                .then(
                    topBarBackdrop?.let { Modifier.onGloballyPositioned { coordinates -> topBarBackdropCaptureCoordinates = coordinates }.layerBackdrop(it) } ?: Modifier,
                ),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                state = listState,
                contentPadding = PaddingValues(
                    start = padding.calculateStartPadding(layoutDirection),
                    top = padding.calculateTopPadding(),
                    end = padding.calculateEndPadding(layoutDirection),
                    bottom = maxOf(
                        padding.calculateBottomPadding(),
                        bottomContentPadding,
                    ) + 16.dp,
                ),
                overscrollEffect = null,
            ) {
                item {
                    ThemeModeSelector(
                        themeMode = selectedThemeMode,
                        systemDark = systemDark,
                        onThemeModeChange = { mode ->
                            if (mode != selectedThemeMode) {
                                selectedThemeMode = mode
                                onThemeModeChange(mode)
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    )
                }
                item {
                    ThemeCard {
                        SwitchPreference(
                            title = stringResource(R.string.theme_mode_system),
                            checked = selectedThemeMode == ThemeMode.SYSTEM,
                            onCheckedChange = { checked ->
                                val mode = themeModeFollowingSystem(checked, systemDark)
                                selectedThemeMode = mode
                                onThemeModeChange(mode)
                            },
                        )
                    }
                }
                item {
                    ThemeCard {
                        OverlayDropdownPreference(
                            title = stringResource(R.string.settings_theme_style_title),
                            summary = if (Build.VERSION.SDK_INT < 33) stringResource(R.string.settings_glass_styles_unsupported) else null,
                            entry = DropdownEntry(items = listOf(
                                DropdownItem(
                                    text = stringResource(R.string.settings_style_os3),
                                    selected = settings.themeStyle == ThemeStyle.OS3,
                                    onClick = { onThemeStyleChange(ThemeStyle.OS3) },
                                ),
                                DropdownItem(
                                    text = stringResource(R.string.settings_style_os4),
                                    selected = settings.themeStyle == ThemeStyle.OS4,
                                    enabled = Build.VERSION.SDK_INT >= 33,
                                    onClick = { onThemeStyleChange(ThemeStyle.OS4) },
                                ),
                            )),
                        )
                    }
                }
                item {
                    SmallTitle(text = stringResource(R.string.settings_section_appearance))
                }
                item {
                    ThemeCard {
                        ArrowPreference(
                            title = stringResource(R.string.settings_custom_background_title),
                            onClick = onOpenMainBackground,
                        )
                        ArrowPreference(
                            title = stringResource(R.string.settings_interface_style_title),
                            onClick = onOpenInterfaceStyle,
                        )
                        SwitchPreference(
                            title = stringResource(R.string.settings_immersive_mode_title),
                            summary = stringResource(R.string.settings_immersive_mode_summary),
                            checked = immersiveModeChecked,
                            onCheckedChange = { checked ->
                                immersiveModeChecked = checked
                                onImmersiveModeChange(checked)
                            },
                        )
                        SwitchPreference(
                            title = stringResource(R.string.settings_hide_status_bar_title),
                            checked = hideStatusBarChecked,
                            onCheckedChange = { checked ->
                                hideStatusBarChecked = checked
                                onHideStatusBarChange(checked)
                            },
                        )
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
                        AnimatedVisibility(
                            visible = dynamicColorChecked && dynamicColorSupported,
                            enter = fadeIn(animationSpec = tween(200)) +
                                expandVertically(animationSpec = tween(250)),
                            exit = fadeOut(animationSpec = tween(150)) +
                                shrinkVertically(animationSpec = tween(200)),
                            label = "dynamicColorSourceVisibility",
                        ) {
                            val sources = listOf(
                                DynamicColorSource.PLAYBACK_ARTWORK to
                                    stringResource(R.string.settings_dynamic_color_source_artwork),
                                DynamicColorSource.DESKTOP to
                                    stringResource(R.string.settings_dynamic_color_source_desktop),
                            )
                            OverlayDropdownPreference(
                                items = sources.map { it.second },
                                selectedIndex = sources.indexOfFirst {
                                    it.first == dynamicColorSource
                                }.coerceAtLeast(0),
                                title = stringResource(R.string.settings_dynamic_color_source_title),
                                onSelectedIndexChange = { index ->
                                    sources.getOrNull(index)?.first?.let { source ->
                                        dynamicColorSource = source
                                        onDynamicColorSourceChange(source)
                                    }
                                },
                            )
                        }
                    }
                }
                item {
                    SmallTitle(text = stringResource(R.string.settings_section_playback_interface))
                }
                item {
                    ThemeCard {
                        OverlayDropdownPreference(
                            items = playbackBackgroundStyles.map { it.second },
                            selectedIndex = playbackBackgroundStyles.indexOfFirst {
                                it.first == playbackBackgroundStyle
                            }.coerceAtLeast(0),
                            title = stringResource(R.string.settings_playback_background_title),
                            onSelectedIndexChange = { index ->
                                playbackBackgroundStyles.getOrNull(index)?.first?.let { style ->
                                    if (style != playbackBackgroundStyle) {
                                        playbackBackgroundStyle = style
                                        onPlaybackBackgroundStyleChange(style)
                                    }
                                }
                            },
                        )
                        AnimatedVisibility(
                            visible = playbackBackgroundStyle == PlaybackBackgroundStyle.DYNAMIC_FLOW,
                            enter = fadeIn(animationSpec = tween(200)) +
                                expandVertically(animationSpec = tween(250)),
                            exit = fadeOut(animationSpec = tween(150)) +
                                shrinkVertically(animationSpec = tween(200)),
                            label = "dynamicFlowStyleVisibility",
                        ) {
                            OverlayDropdownPreference(
                                items = dynamicFlowStyles.map { it.second },
                                selectedIndex = dynamicFlowStyles.indexOfFirst {
                                    it.first == dynamicFlowStyle
                                }.coerceAtLeast(0),
                                title = stringResource(R.string.settings_dynamic_flow_style_title),
                                onSelectedIndexChange = { index ->
                                    dynamicFlowStyles.getOrNull(index)?.first?.let { style ->
                                        if (style != dynamicFlowStyle) {
                                            dynamicFlowStyle = style
                                            onDynamicFlowStyleChange(style)
                                        }
                                    }
                                },
                            )
                        }
                        OverlayDropdownPreference(
                            items = coverShapes.map { it.second },
                            selectedIndex = coverShapes.indexOfFirst {
                                it.first == coverShape
                            }.coerceAtLeast(0),
                            title = stringResource(R.string.settings_cover_shape_title),
                            onSelectedIndexChange = { index ->
                                coverShapes.getOrNull(index)?.first?.let { shape ->
                                    if (shape != coverShape) {
                                        coverShape = shape
                                        onCoverShapeChange(shape)
                                    }
                                }
                            },
                        )
                        SwitchPreference(
                            title = stringResource(R.string.settings_artwork_flip_animation_title),
                            checked = artworkFlipAnimationChecked,
                            onCheckedChange = { checked ->
                                artworkFlipAnimationChecked = checked
                                onArtworkFlipAnimationChange(checked)
                            },
                        )
                    }
                }
                item {
                    SmallTitle(text = stringResource(R.string.settings_section_song_list))
                }
                item {
                    ThemeCard {
                        SwitchPreference(
                            title = stringResource(R.string.settings_song_list_artwork_title),
                            checked = songListArtworkChecked,
                            onCheckedChange = { checked ->
                                songListArtworkChecked = checked
                                onShowSongListArtworkChange(checked)
                            },
                        )
                        SwitchPreference(
                            title = stringResource(R.string.settings_song_list_file_name_title),
                            checked = songListFileNameChecked,
                            onCheckedChange = { checked ->
                                songListFileNameChecked = checked
                                onShowSongListFileNameChange(checked)
                            },
                        )
                        SwitchPreference(
                            title = stringResource(R.string.settings_song_list_duration_title),
                            checked = songListDurationChecked,
                            onCheckedChange = { checked ->
                                songListDurationChecked = checked
                                onShowSongListDurationChange(checked)
                            },
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
                        )
                        AnimatedVisibility(
                            visible = predictiveBackChecked,
                            enter = fadeIn(animationSpec = tween(200)) +
                                expandVertically(animationSpec = tween(250)),
                            exit = fadeOut(animationSpec = tween(150)) +
                                shrinkVertically(animationSpec = tween(200)),
                            label = "navigationTransitionStyleVisibility",
                        ) {
                            val transitionStyles = listOf(
                                NavigationTransitionStyle.MIUIX to stringResource(
                                    R.string.settings_navigation_transition_style_miuix,
                                ),
                                NavigationTransitionStyle.AOSP to stringResource(
                                    R.string.settings_navigation_transition_style_aosp,
                                ),
                            )
                            OverlayDropdownPreference(
                                title = stringResource(
                                    R.string.settings_navigation_transition_style_title,
                                ),
                                items = transitionStyles.map { it.second },
                                selectedIndex = transitionStyles.indexOfFirst {
                                    it.first == navigationTransitionStyle
                                }.coerceAtLeast(0),
                                onSelectedIndexChange = { index ->
                                    transitionStyles.getOrNull(index)?.first?.let { style ->
                                        navigationTransitionStyle = style
                                        onNavigationTransitionStyleChange(style)
                                    }
                                },
                            )
                        }
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
