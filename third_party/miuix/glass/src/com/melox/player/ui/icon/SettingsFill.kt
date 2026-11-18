// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.melox.player.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

internal val GlassIcons.SettingsFill: ImageVector
    get() {
        if (_appSettingsSurfaceRegular != null) return _appSettingsSurfaceRegular!!
        _appSettingsSurfaceRegular = buildHyperOSIcon(
            name = "SettingsFill.Glass.Regular",
            pathData = listOf(
                PathNode.MoveTo(520f, 375f),
                PathNode.QuadTo(520f, 336f, 539.5f, 302.5f),
                PathNode.QuadTo(559f, 269f, 592.5f, 249.5f),
                PathNode.QuadTo(626f, 230f, 665f, 230f),
                PathNode.QuadTo(704f, 230f, 737.5f, 249.5f),
                PathNode.QuadTo(771f, 269f, 790.5f, 302.5f),
                PathNode.QuadTo(810f, 336f, 810f, 375f),
                PathNode.QuadTo(810f, 414f, 790.5f, 447.5f),
                PathNode.QuadTo(771f, 481f, 737.5f, 500.5f),
                PathNode.QuadTo(704f, 520f, 665f, 520f),
                PathNode.QuadTo(626f, 520f, 592.5f, 500.5f),
                PathNode.QuadTo(559f, 481f, 539.5f, 447.5f),
                PathNode.QuadTo(520f, 414f, 520f, 375f),
                PathNode.Close,
                PathNode.MoveTo(204f, 546f),
                PathNode.VerticalTo(204f),
                PathNode.QuadTo(204f, 157f, 222.5f, 125.5f),
                PathNode.QuadTo(241f, 94f, 281f, 72f),
                PathNode.LineTo(588f, -101f),
                PathNode.QuadTo(628f, -124f, 665f, -124f),
                PathNode.QuadTo(702f, -124f, 742f, -101f),
                PathNode.LineTo(1049f, 72f),
                PathNode.QuadTo(1089f, 94f, 1107.5f, 125.5f),
                PathNode.QuadTo(1126f, 157f, 1126f, 204f),
                PathNode.VerticalTo(546f),
                PathNode.QuadTo(1126f, 593f, 1107.5f, 624.5f),
                PathNode.QuadTo(1089f, 656f, 1049f, 678f),
                PathNode.LineTo(742f, 851f),
                PathNode.QuadTo(702f, 874f, 665f, 874f),
                PathNode.QuadTo(628f, 874f, 588f, 851f),
                PathNode.LineTo(281f, 678f),
                PathNode.QuadTo(241f, 656f, 222.5f, 624.5f),
                PathNode.QuadTo(204f, 593f, 204f, 546f),
                PathNode.Close,
                PathNode.MoveTo(914f, 375f),
                PathNode.QuadTo(914f, 307f, 880.5f, 250f),
                PathNode.QuadTo(847f, 193f, 790f, 159.5f),
                PathNode.QuadTo(733f, 126f, 665f, 126f),
                PathNode.QuadTo(597f, 126f, 540f, 159.5f),
                PathNode.QuadTo(483f, 193f, 449.5f, 250f),
                PathNode.QuadTo(416f, 307f, 416f, 375f),
                PathNode.QuadTo(416f, 443f, 449.5f, 500f),
                PathNode.QuadTo(483f, 557f, 540f, 590.5f),
                PathNode.QuadTo(597f, 624f, 665f, 624f),
                PathNode.QuadTo(733f, 624f, 790f, 590.5f),
                PathNode.QuadTo(847f, 557f, 880.5f, 500f),
                PathNode.QuadTo(914f, 443f, 914f, 375f),
                PathNode.Close,
            ),
            scale = 1f,
            translationX = 1.6667f,
            translationY = 1041.6667f,
            autoMirror = false,
        )
        return _appSettingsSurfaceRegular!!
    }

private var _appSettingsSurfaceRegular: ImageVector? = null
