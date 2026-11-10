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

internal val GlassIcons.Reset: ImageVector
    get() {
        if (_resetRegular != null) return _resetRegular!!
        _resetRegular = ImageVector.Builder(
            name = "Reset.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(32.3f, 732.4f),
                        PathNode.LineTo(308.3f, 456.4f),
                        PathNode.QuadTo(318.3f, 446.4f, 329.3f, 446.4f),
                        PathNode.QuadTo(340.3f, 446.4f, 350.3f, 456.4f),
                        PathNode.LineTo(368.3f, 474.4f),
                        PathNode.QuadTo(379.3f, 484.4f, 379.3f, 495.4f),
                        PathNode.QuadTo(379.3f, 506.4f, 369.3f, 516.4f),
                        PathNode.LineTo(163.3f, 722.4f),
                        PathNode.LineTo(751.3f, 722.4f),
                        PathNode.QuadTo(831.3f, 722.4f, 898.8f, 681.9f),
                        PathNode.QuadTo(966.3f, 641.4f, 1004.3f, 571.9f),
                        PathNode.QuadTo(1042.3f, 502.4f, 1039.3f, 422.4f),
                        PathNode.QuadTo(1036.3f, 345.4f, 996.8f, 281.9f),
                        PathNode.QuadTo(957.3f, 218.4f, 891.8f, 181.4f),
                        PathNode.QuadTo(826.3f, 144.4f, 749.3f, 144.4f),
                        PathNode.LineTo(252.3f, 144.4f),
                        PathNode.QuadTo(238.3f, 144.4f, 230.3f, 136.4f),
                        PathNode.QuadTo(222.3f, 128.4f, 222.3f, 114.4f),
                        PathNode.LineTo(222.3f, 88.4f),
                        PathNode.QuadTo(222.3f, 74.4f, 230.3f, 66.4f),
                        PathNode.QuadTo(238.3f, 58.4f, 252.3f, 58.4f),
                        PathNode.LineTo(749.3f, 58.4f),
                        PathNode.QuadTo(848.3f, 58.4f, 933.3f, 106.4f),
                        PathNode.QuadTo(1018.3f, 154.4f, 1069.3f, 236.9f),
                        PathNode.QuadTo(1120.3f, 319.4f, 1124.3f, 418.4f),
                        PathNode.QuadTo(1128.3f, 523.4f, 1078.8f, 612.9f),
                        PathNode.QuadTo(1029.3f, 702.4f, 942.3f, 755.4f),
                        PathNode.QuadTo(855.3f, 808.4f, 751.3f, 808.4f),
                        PathNode.LineTo(162.3f, 808.4f),
                        PathNode.LineTo(369.3f, 1014.4f),
                        PathNode.QuadTo(379.3f, 1024.4f, 379.3f, 1035.4f),
                        PathNode.QuadTo(379.3f, 1046.4f, 368.3f, 1056.4f),
                        PathNode.LineTo(350.3f, 1074.4f),
                        PathNode.QuadTo(340.3f, 1084.4f, 329.3f, 1084.4f),
                        PathNode.QuadTo(318.3f, 1084.4f, 308.3f, 1074.4f),
                        PathNode.LineTo(32.3f, 799.4f),
                        PathNode.QuadTo(18.3f, 785.4f, 18.3f, 765.9f),
                        PathNode.QuadTo(18.3f, 746.4f, 32.3f, 732.4f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _resetRegular!!
    }

private var _resetRegular: ImageVector? = null
