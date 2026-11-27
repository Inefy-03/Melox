package com.melox.player.ui.screen.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.melox.player.R
import com.melox.player.model.ThemeMode
import com.melox.player.model.effectiveThemeMode
import com.melox.player.ui.usesMiuixSmallTopAppBar
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ThemeModeSelector(
    themeMode: ThemeMode,
    systemDark: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val effectiveMode = effectiveThemeMode(themeMode, systemDark)
    val wide = usesMiuixSmallTopAppBar()
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
                ThemeModePreview(
                    dark = mode == ThemeMode.DARK,
                    wide = wide,
                    isSelected = effectiveMode == mode,
                    onClick = { onThemeModeChange(mode) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ThemeModePreview(
    dark: Boolean,
    wide: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.semantics(mergeDescendants = true) {
            role = Role.RadioButton
            selected = isSelected
        },
        cornerRadius = 26.dp,
        insideMargin = PaddingValues(vertical = 6.dp),
        colors = CardDefaults.defaultColors(color = Color.Transparent),
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .squircleBorder(
                    width = 3.dp,
                    color = if (isSelected) MiuixTheme.colorScheme.primary else Color.Transparent,
                    cornerRadius = 26.dp,
                )
                .padding(6.dp),
        ) {
            ThemeModeIllustration(dark = dark, wide = wide)
        }
        Text(
            text = stringResource(if (dark) R.string.theme_mode_dark else R.string.theme_mode_light),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            color = if (isSelected) MiuixTheme.colorScheme.primary
                else MiuixTheme.colorScheme.onSurfaceSecondary,
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ThemeModeIllustration(
    dark: Boolean,
    wide: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.fillMaxWidth().aspectRatio(if (wide) 2f else 1f).squircleClip(20.dp),
    ) {
        drawRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF406EFF), Color(0xFF55B9FA))),
        )
        val illustrationWidth = size.width * if (wide) 0.7f else 1f
        val start = (size.width - illustrationWidth) / 2f
        val unitX = illustrationWidth / 680f
        val unitY = size.height / 680f
        val cornerUnit = minOf(unitX, unitY)
        val panelColor = if (dark) Color.Black.copy(alpha = 0.2f)
            else Color.White.copy(alpha = 0.2f)
        val lineColor = if (dark) Color.White.copy(alpha = 0.35f)
            else Color(0xFF285ABC).copy(alpha = 0.6f)
        for (top in listOf(148f, 356f)) {
            drawRoundRect(
                color = panelColor,
                topLeft = Offset(start + 48f * unitX, top * unitY),
                size = Size(584f * unitX, 176f * unitY),
                cornerRadius = CornerRadius(44f * cornerUnit),
            )
            for ((offset, width) in listOf(52f to 460f, 100f to 312f)) {
                drawRoundRect(
                    color = lineColor,
                    topLeft = Offset(start + 112f * unitX, (top + offset) * unitY),
                    size = Size(width * unitX, 24f * unitY),
                    cornerRadius = CornerRadius(12f * cornerUnit),
                )
            }
        }
    }
}

internal fun themeModeFollowingSystem(followSystem: Boolean, systemDark: Boolean): ThemeMode =
    if (followSystem) ThemeMode.SYSTEM else effectiveThemeMode(ThemeMode.SYSTEM, systemDark)
