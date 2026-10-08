package com.melox.player.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.melox.player.R
import com.melox.player.ui.screen.playback.DEFAULT_LYRIC_FONT_SCALE
import com.melox.player.ui.screen.playback.LYRICS_FONT_WEIGHT_STEP_COUNT
import com.melox.player.ui.screen.playback.LYRIC_PRIMARY_FONT_SIZE_SP
import com.melox.player.ui.screen.playback.MAX_LYRIC_FONT_SCALE
import com.melox.player.ui.screen.playback.MAX_LYRIC_FONT_WEIGHT
import com.melox.player.ui.screen.playback.MIN_LYRIC_FONT_SCALE
import com.melox.player.ui.screen.playback.MIN_LYRIC_FONT_WEIGHT
import com.melox.player.ui.screen.playback.TappableSliderPreference
import kotlin.math.roundToInt
import com.melox.player.ui.component.PageCard as Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun LyricsInterfacePreferences(
    leftAlignPlayerTitle: Boolean,
    centerLyrics: Boolean,
    showLyricsTranslation: Boolean,
    lyricFontScale: Float,
    lyricFontWeight: Int,
    lyricBlurEnabled: Boolean,
    forceWordByWordLyrics: Boolean,
    hideControlsOnLyrics: Boolean,
    onLeftAlignPlayerTitleChange: (Boolean) -> Unit,
    onCenterLyricsChange: (Boolean) -> Unit,
    onShowLyricsTranslationChange: (Boolean) -> Unit,
    onLyricFontScalePreview: (Float) -> Unit,
    onLyricFontScaleCommit: () -> Unit,
    onLyricFontWeightPreview: (Int) -> Unit,
    onLyricFontWeightCommit: () -> Unit,
    onLyricBlurEnabledChange: (Boolean) -> Unit,
    onForceWordByWordLyricsChange: (Boolean) -> Unit,
    onHideControlsOnLyricsChange: (Boolean) -> Unit,
    hideStatusBar: Boolean = false,
    onHideStatusBarChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
    cardColor: Color = MiuixTheme.colorScheme.surfaceContainer,
    showHideControls: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.defaultColors(color = cardColor),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SwitchPreference(
                    title = stringResource(R.string.player_title_left_aligned),
                    checked = leftAlignPlayerTitle,
                    onCheckedChange = onLeftAlignPlayerTitleChange,
                )
                SwitchPreference(
                    title = stringResource(R.string.lyrics_center),
                    checked = centerLyrics,
                    onCheckedChange = onCenterLyricsChange,
                )
                SwitchPreference(
                    title = stringResource(R.string.lyrics_translation),
                    checked = showLyricsTranslation,
                    onCheckedChange = onShowLyricsTranslationChange,
                )
                TappableSliderPreference(
                    value = lyricFontScale,
                    onValueChange = onLyricFontScalePreview,
                    title = stringResource(R.string.lyrics_size),
                    valueText = stringResource(
                        R.string.lyrics_size_value,
                        (LYRIC_PRIMARY_FONT_SIZE_SP * lyricFontScale).roundToInt(),
                    ),
                    valueRange = MIN_LYRIC_FONT_SCALE..MAX_LYRIC_FONT_SCALE,
                    onValueChangeFinished = onLyricFontScaleCommit,
                    showKeyPoints = true,
                    keyPoints = listOf(DEFAULT_LYRIC_FONT_SCALE),
                )
                TappableSliderPreference(
                    value = lyricFontWeight.toFloat(),
                    onValueChange = { onLyricFontWeightPreview(it.roundToInt()) },
                    title = stringResource(R.string.lyrics_weight),
                    valueText = stringResource(R.string.lyrics_weight_value, lyricFontWeight),
                    valueRange = MIN_LYRIC_FONT_WEIGHT.toFloat()..
                        MAX_LYRIC_FONT_WEIGHT.toFloat(),
                    steps = LYRICS_FONT_WEIGHT_STEP_COUNT,
                    onValueChangeFinished = onLyricFontWeightCommit,
                    showKeyPoints = true,
                )
                SwitchPreference(
                    title = stringResource(R.string.lyrics_blur),
                    summary = stringResource(R.string.lyrics_blur_summary),
                    checked = lyricBlurEnabled,
                    onCheckedChange = onLyricBlurEnabledChange,
                )
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.defaultColors(color = cardColor),
        ) {
            SwitchPreference(
                title = stringResource(R.string.lyrics_force_word_by_word),
                summary = stringResource(R.string.lyrics_force_word_by_word_summary),
                checked = forceWordByWordLyrics,
                onCheckedChange = onForceWordByWordLyricsChange,
            )
        }
        if (showHideControls) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.defaultColors(color = cardColor),
            ) {
                SwitchPreference(
                    title = stringResource(R.string.lyrics_hide_controls),
                    summary = stringResource(R.string.lyrics_hide_controls_summary),
                    checked = hideControlsOnLyrics,
                    onCheckedChange = onHideControlsOnLyricsChange,
                )
            }
        }
        if (onHideStatusBarChange != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.defaultColors(color = cardColor),
            ) {
                SwitchPreference(
                    title = stringResource(R.string.settings_hide_status_bar_title),
                    summary = stringResource(R.string.settings_hide_status_bar_summary),
                    checked = hideStatusBar,
                    onCheckedChange = onHideStatusBarChange,
                )
            }
        }
    }
}
