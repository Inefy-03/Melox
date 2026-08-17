package com.melox.player.ui.screen.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.melox.player.R
import com.melox.player.model.DefaultHomePage
import com.melox.player.ui.locale.AppLanguage
import com.melox.player.ui.locale.currentAppLanguage
import com.melox.player.ui.locale.setAppLanguage
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun SettingsScreen(
    defaultHomePage: DefaultHomePage,
    trackCount: Int,
    onDefaultHomePageChange: (DefaultHomePage) -> Unit,
    onOpenThemeSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenScanSettings: () -> Unit,
    scrollBehavior: ScrollBehavior,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = contentPadding,
        overscrollEffect = null,
    ) {
        item(key = "personalization_title") {
            SmallTitle(text = stringResource(R.string.settings_section_personalization))
        }
        item(key = "personalization") {
            SettingsCard {
                LanguagePreference()
                DefaultHomePagePreference(
                    selectedPage = defaultHomePage,
                    onSelectedPageChange = onDefaultHomePageChange,
                )
                ArrowPreference(
                    title = stringResource(R.string.settings_theme_settings_title),
                    summary = stringResource(R.string.settings_theme_settings_summary),
                    onClick = onOpenThemeSettings,
                )
            }
        }
        item(key = "music_library_title") {
            SmallTitle(text = stringResource(R.string.settings_section_music_library))
        }
        item(key = "music_library") {
            SettingsCard {
                ArrowPreference(
                    title = stringResource(R.string.settings_scan_local_music_title),
                    summary = if (trackCount > 0) {
                        pluralStringResource(
                            R.plurals.settings_scan_music_song_count,
                            trackCount,
                            trackCount,
                        )
                    } else {
                        stringResource(R.string.settings_scan_music_empty_summary)
                    },
                    onClick = onOpenScanSettings,
                )
            }
        }
        item(key = "other_title") {
            SmallTitle(text = stringResource(R.string.settings_section_other))
        }
        item(key = "other") {
            SettingsCard {
                ArrowPreference(
                    title = stringResource(R.string.settings_about_title),
                    onClick = onOpenAbout,
                )
            }
        }
    }
}

@Composable
private fun DefaultHomePagePreference(
    selectedPage: DefaultHomePage,
    onSelectedPageChange: (DefaultHomePage) -> Unit,
) {
    val pages = listOf(
        DefaultHomePage.HOME to stringResource(R.string.navigation_home),
        DefaultHomePage.SONGS to stringResource(R.string.navigation_music),
        DefaultHomePage.LIBRARY to stringResource(R.string.navigation_library),
    )
    OverlayDropdownPreference(
        items = pages.map { it.second },
        selectedIndex = pages.indexOfFirst { it.first == selectedPage }.coerceAtLeast(0),
        title = stringResource(R.string.settings_default_home_title),
        onSelectedIndexChange = { index ->
            pages.getOrNull(index)?.first?.let(onSelectedPageChange)
        },
    )
}

@Composable
private fun LanguagePreference() {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val languages = listOf(
        AppLanguage.FOLLOW_SYSTEM to stringResource(R.string.language_follow_system),
        AppLanguage.SIMPLIFIED_CHINESE to stringResource(R.string.language_simplified_chinese),
        AppLanguage.ENGLISH to stringResource(R.string.language_english),
    )
    var selectedLanguage by remember {
        mutableStateOf(currentAppLanguage(context))
    }
    LaunchedEffect(context, configuration) {
        selectedLanguage = currentAppLanguage(context)
    }
    OverlayDropdownPreference(
        items = languages.map { it.second },
        selectedIndex = languages.indexOfFirst {
            it.first == selectedLanguage
        }.coerceAtLeast(0),
        title = stringResource(R.string.language_title),
        onSelectedIndexChange = { index ->
            languages.getOrNull(index)?.first?.let { language ->
                if (language != selectedLanguage) {
                    selectedLanguage = language
                    setAppLanguage(context, language)
                }
            }
        },
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        content = { content() },
    )
}
