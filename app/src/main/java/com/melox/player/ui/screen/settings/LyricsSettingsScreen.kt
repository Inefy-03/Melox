package com.melox.player.ui.screen.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import com.melox.player.ui.component.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.melox.player.model.LyricsTranslationLanguage
import com.melox.player.model.LyricAnimationMode
import com.melox.player.R
import com.melox.player.model.AppSettings
import com.melox.player.model.LyricsFormatPriority
import com.melox.player.model.LyricsSourcePriority
import com.melox.player.ui.component.AdaptiveTopAppBar
import com.melox.player.ui.component.BlurredBar
import com.melox.player.ui.component.miuixBarColor
import com.melox.player.ui.component.rememberBlurBackdrop
import com.melox.player.ui.component.PageCard as Card
import top.yukonga.miuix.kmp.basic.DropdownItem
import com.melox.player.ui.component.os4.Icon
import com.melox.player.ui.component.os4.IconButton
import com.melox.player.ui.component.PageScaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import com.melox.player.ui.component.os4.navigationBackIcon
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlaySpinnerPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import com.melox.player.ui.component.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
internal fun LyricsSettingsScreen(
    settings: AppSettings,
    bottomContentPadding: Dp,
    listState: LazyListState,
    scrollBehavior: ScrollBehavior,
    onBack: () -> Unit,
    onLyricsSourcePriorityChange: (LyricsSourcePriority) -> Unit,
    onLyricsFormatPriorityChange: (LyricsFormatPriority) -> Unit,
    onOpenLyricsInterface: () -> Unit,
    onShowMiniPlayerLyricsChange: (Boolean) -> Unit,
    onShowArtworkLyricsChange: (Boolean) -> Unit,
    onShowMusicTagEditorChange: (Boolean) -> Unit,
    onShowLyricoEditorChange: (Boolean) -> Unit,
    onShowLunaBeatEditorChange: (Boolean) -> Unit,
) {
    var sourcePriority by remember(settings.lyricsSourcePriority) {
        mutableStateOf(settings.lyricsSourcePriority)
    }
    var formatPriority by remember(settings.lyricsFormatPriority) {
        mutableStateOf(settings.lyricsFormatPriority)
    }
    var showMiniPlayerLyrics by remember(settings.showMiniPlayerLyrics) {
        mutableStateOf(settings.showMiniPlayerLyrics)
    }
    var showArtworkLyrics by remember(settings.showArtworkLyrics) {
        mutableStateOf(settings.showArtworkLyrics)
    }
    var showMusicTagEditor by remember(settings.showMusicTagEditor) {
        mutableStateOf(settings.showMusicTagEditor)
    }
    var showLyricoEditor by remember(settings.showLyricoEditor) {
        mutableStateOf(settings.showLyricoEditor)
    }
    var showLunaBeatEditor by remember(settings.showLunaBeatEditor) {
        mutableStateOf(settings.showLunaBeatEditor)
    }
    LyricsSettingsPage(
        titleRes = R.string.settings_lyrics_title,
        bottomContentPadding = bottomContentPadding,
        listState = listState,
        scrollBehavior = scrollBehavior,
        onBack = onBack,
    ) {
        item(key = "lyrics_priorities_title") {
            SmallTitle(text = stringResource(R.string.lyrics_priority_section))
        }
        item(key = "lyrics_priorities") {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            ) {
                OverlaySpinnerPreference(
                    items = listOf(
                        DropdownItem(text = stringResource(R.string.lyrics_source_embedded)),
                        DropdownItem(text = stringResource(R.string.lyrics_source_external)),
                    ),
                    selectedIndex = when (sourcePriority) {
                        LyricsSourcePriority.EMBEDDED -> 0
                        LyricsSourcePriority.SIDECAR -> 1
                    },
                    title = stringResource(R.string.lyrics_source_priority),
                    onSelectedIndexChange = { index ->
                        val priority = if (index == 0) {
                            LyricsSourcePriority.EMBEDDED
                        } else {
                            LyricsSourcePriority.SIDECAR
                        }
                        if (priority != sourcePriority) {
                            sourcePriority = priority
                            onLyricsSourcePriorityChange(priority)
                        }
                    },
                )
                OverlaySpinnerPreference(
                    items = listOf(
                        DropdownItem(text = stringResource(R.string.lyrics_format_lrc)),
                        DropdownItem(text = stringResource(R.string.lyrics_format_ttml)),
                    ),
                    selectedIndex = when (formatPriority) {
                        LyricsFormatPriority.LRC -> 0
                        LyricsFormatPriority.TTML -> 1
                    },
                    title = stringResource(R.string.lyrics_format_priority),
                    onSelectedIndexChange = { index ->
                        val priority = if (index == 0) {
                            LyricsFormatPriority.LRC
                        } else {
                            LyricsFormatPriority.TTML
                        }
                        if (priority != formatPriority) {
                            formatPriority = priority
                            onLyricsFormatPriorityChange(priority)
                        }
                    },
                )
            }
        }
        item(key = "lyrics_display_title") {
            SmallTitle(
                text = stringResource(R.string.lyrics_display_section),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        item(key = "lyrics_interface") {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            ) {
                ArrowPreference(
                    title = stringResource(R.string.settings_lyrics_interface_title),
                    onClick = onOpenLyricsInterface,
                )
                SwitchPreference(
                    title = stringResource(R.string.lyrics_mini_player),
                    checked = showMiniPlayerLyrics,
                    onCheckedChange = {
                        showMiniPlayerLyrics = it
                        onShowMiniPlayerLyricsChange(it)
                    },
                )
                SwitchPreference(
                    title = stringResource(R.string.lyrics_artwork_page),
                    summary = stringResource(R.string.lyrics_artwork_page_summary),
                    checked = showArtworkLyrics,
                    onCheckedChange = {
                        showArtworkLyrics = it
                        onShowArtworkLyricsChange(it)
                    },
                )
            }
        }
        item(key = "lyrics_editor_menu_title") {
            SmallTitle(
                text = stringResource(R.string.lyrics_editor_menu),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        item(key = "lyrics_editor_menu") {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                SwitchPreference(
                    title = stringResource(R.string.lyrics_editor_music_tag),
                    checked = showMusicTagEditor,
                    onCheckedChange = {
                        showMusicTagEditor = it
                        onShowMusicTagEditorChange(it)
                    },
                )
                SwitchPreference(
                    title = stringResource(R.string.lyrics_editor_lyrico),
                    checked = showLyricoEditor,
                    onCheckedChange = {
                        showLyricoEditor = it
                        onShowLyricoEditorChange(it)
                    },
                )
                SwitchPreference(
                    title = stringResource(R.string.lyrics_editor_luna_beat),
                    checked = showLunaBeatEditor,
                    onCheckedChange = {
                        showLunaBeatEditor = it
                        onShowLunaBeatEditorChange(it)
                    },
                )
            }
        }
    }
}

@Composable
internal fun LyricsInterfaceSettingsScreen(
    settings: AppSettings,
    centerLyricsEnabled: Boolean,
    bottomContentPadding: Dp,
    listState: LazyListState,
    scrollBehavior: ScrollBehavior,
    onBack: () -> Unit,
    onLeftAlignPlayerTitleChange: (Boolean) -> Unit,
    onCenterLyricsChange: (Boolean) -> Unit,
    onShowLyricsTranslationChange: (Boolean) -> Unit,
    onShowLyricsRomanizationChange: (Boolean) -> Unit,
    onLyricsTranslationLanguageChange: (LyricsTranslationLanguage) -> Unit,
    onLyricFontScaleChange: (Float) -> Unit,
    onLyricFontWeightChange: (Int) -> Unit,
    onLyricBlurEnabledChange: (Boolean) -> Unit,
    onLyricAnimationModeChange: (LyricAnimationMode) -> Unit,
    onHideControlsOnLyricsChange: (Boolean) -> Unit,
) {
    var leftAlignTitle by remember(settings.leftAlignPlayerTitle) {
        mutableStateOf(settings.leftAlignPlayerTitle)
    }
    var centerLyrics by remember(settings.centerLyrics) { mutableStateOf(settings.centerLyrics) }
    var translation by remember(settings.showLyricsTranslation) {
        mutableStateOf(settings.showLyricsTranslation)
    }
    var translationLanguage by remember(settings.lyricsTranslationLanguage) {
        mutableStateOf(settings.lyricsTranslationLanguage)
    }
    var romanization by remember(settings.showLyricsRomanization) {
        mutableStateOf(settings.showLyricsRomanization)
    }
    var fontScale by remember(settings.lyricFontScale) { mutableFloatStateOf(settings.lyricFontScale) }
    var fontWeight by remember(settings.lyricFontWeight) { mutableIntStateOf(settings.lyricFontWeight) }
    var blur by remember(settings.lyricBlurEnabled) { mutableStateOf(settings.lyricBlurEnabled) }
    var animationMode by remember(settings.lyricAnimationMode) {
        mutableStateOf(settings.lyricAnimationMode)
    }
    var hideControls by remember(settings.hideControlsOnLyrics) {
        mutableStateOf(settings.hideControlsOnLyrics)
    }
    LyricsSettingsPage(
        titleRes = R.string.settings_lyrics_interface_title,
        bottomContentPadding = bottomContentPadding,
        listState = listState,
        scrollBehavior = scrollBehavior,
        onBack = onBack,
    ) {
        item(key = "lyrics_interface_title") {
            SmallTitle(text = stringResource(R.string.lyrics_settings_section))
        }
        item(key = "lyrics_interface_preferences") {
            LyricsInterfacePreferences(
                leftAlignPlayerTitle = leftAlignTitle,
                centerLyrics = centerLyrics,
                centerLyricsEnabled = centerLyricsEnabled,
                lyricFontScale = fontScale,
                lyricFontWeight = fontWeight,
                lyricBlurEnabled = blur,
                lyricAnimationMode = animationMode,
                hideControlsOnLyrics = hideControls,
                onLeftAlignPlayerTitleChange = {
                    leftAlignTitle = it
                    onLeftAlignPlayerTitleChange(it)
                },
                onCenterLyricsChange = {
                    centerLyrics = it
                    onCenterLyricsChange(it)
                },
                onLyricFontScalePreview = { fontScale = it },
                onLyricFontScaleCommit = { onLyricFontScaleChange(fontScale) },
                onLyricFontWeightPreview = { fontWeight = it },
                onLyricFontWeightCommit = { onLyricFontWeightChange(fontWeight) },
                onLyricBlurEnabledChange = {
                    blur = it
                    onLyricBlurEnabledChange(it)
                },
                onLyricAnimationModeChange = {
                    animationMode = it
                    onLyricAnimationModeChange(it)
                },
                onHideControlsOnLyricsChange = {
                    hideControls = it
                    onHideControlsOnLyricsChange(it)
                },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        item(key = "lyrics_translation_title") {
            SmallTitle(text = stringResource(R.string.lyrics_translation), modifier = Modifier.padding(top = 12.dp))
        }
        item(key = "lyrics_translation_preferences") {
            LyricsTranslationPreferences(
                showLyricsTranslation = translation,
                showLyricsRomanization = romanization,
                lyricsTranslationLanguage = translationLanguage,
                onShowLyricsTranslationChange = {
                    translation = it
                    onShowLyricsTranslationChange(it)
                },
                onLyricsTranslationLanguageChange = {
                    translationLanguage = it
                    onLyricsTranslationLanguageChange(it)
                },
                onShowLyricsRomanizationChange = {
                    romanization = it
                    onShowLyricsRomanizationChange(it)
                },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

@Composable
private fun LyricsSettingsPage(
    @StringRes titleRes: Int,
    bottomContentPadding: Dp,
    listState: LazyListState,
    scrollBehavior: ScrollBehavior,
    onBack: () -> Unit,
    content: LazyListScope.() -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
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
                    title = stringResource(titleRes),
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
            modifier = Modifier.fillMaxSize()
                .then(topBarBackdrop?.let { Modifier.onGloballyPositioned { coordinates -> topBarBackdropCaptureCoordinates = coordinates }.layerBackdrop(it) } ?: Modifier),
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
                content = content,
            )
        }
    }
}
