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

internal val GlassIcons.Tune: ImageVector
    get() {
        if (_tuneRegular != null) return _tuneRegular!!
        _tuneRegular = ImageVector.Builder(
            name = "Tune.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(580.4f, 281.4f),
                        PathNode.QuadTo(580.4f, 330.4f, 556.4f, 371.9f),
                        PathNode.QuadTo(532.4f, 413.4f, 490.9f, 437.4f),
                        PathNode.QuadTo(449.4f, 461.4f, 400.4f, 461.4f),
                        PathNode.QuadTo(351.4f, 461.4f, 310.4f, 437.4f),
                        PathNode.QuadTo(269.4f, 413.4f, 244.9f, 371.9f),
                        PathNode.QuadTo(220.4f, 330.4f, 220.4f, 281.4f),
                        PathNode.QuadTo(220.4f, 232.4f, 244.9f, 191.4f),
                        PathNode.QuadTo(269.4f, 150.4f, 310.4f, 125.9f),
                        PathNode.QuadTo(351.4f, 101.4f, 400.4f, 101.4f),
                        PathNode.QuadTo(449.4f, 101.4f, 490.9f, 125.9f),
                        PathNode.QuadTo(532.4f, 150.4f, 556.4f, 191.4f),
                        PathNode.QuadTo(580.4f, 232.4f, 580.4f, 281.4f),
                        PathNode.Close,
                        PathNode.MoveTo(306.4f, 281.4f),
                        PathNode.QuadTo(306.4f, 307.4f, 318.9f, 328.9f),
                        PathNode.QuadTo(331.4f, 350.4f, 352.9f, 363.4f),
                        PathNode.QuadTo(374.4f, 376.4f, 400.4f, 376.4f),
                        PathNode.QuadTo(426.4f, 376.4f, 448.4f, 363.4f),
                        PathNode.QuadTo(470.4f, 350.4f, 482.9f, 328.9f),
                        PathNode.QuadTo(495.4f, 307.4f, 495.4f, 281.4f),
                        PathNode.QuadTo(495.4f, 255.4f, 482.9f, 233.9f),
                        PathNode.QuadTo(470.4f, 212.4f, 448.4f, 199.4f),
                        PathNode.QuadTo(426.4f, 186.4f, 400.4f, 186.4f),
                        PathNode.QuadTo(374.4f, 186.4f, 352.9f, 199.4f),
                        PathNode.QuadTo(331.4f, 212.4f, 318.9f, 233.9f),
                        PathNode.QuadTo(306.4f, 255.4f, 306.4f, 281.4f),
                        PathNode.Close,
                        PathNode.MoveTo(271.4f, 324.4f),
                        PathNode.LineTo(106.4f, 324.4f),
                        PathNode.QuadTo(95.4f, 324.4f, 86.9f, 317.4f),
                        PathNode.QuadTo(78.4f, 310.4f, 78.4f, 293.4f),
                        PathNode.LineTo(78.4f, 270.4f),
                        PathNode.QuadTo(78.4f, 253.4f, 86.4f, 246.4f),
                        PathNode.QuadTo(94.4f, 239.4f, 106.4f, 239.4f),
                        PathNode.LineTo(271.4f, 239.4f),
                        PathNode.Close,
                        PathNode.MoveTo(1064.4f, 270.4f),
                        PathNode.LineTo(1064.4f, 293.4f),
                        PathNode.QuadTo(1064.4f, 310.4f, 1055.9f, 317.4f),
                        PathNode.QuadTo(1047.4f, 324.4f, 1036.4f, 324.4f),
                        PathNode.LineTo(538.4f, 324.4f),
                        PathNode.LineTo(538.4f, 238.4f),
                        PathNode.LineTo(1036.4f, 239.4f),
                        PathNode.QuadTo(1048.4f, 239.4f, 1056.4f, 246.4f),
                        PathNode.QuadTo(1064.4f, 253.4f, 1064.4f, 270.4f),
                        PathNode.Close,
                        PathNode.MoveTo(1064.4f, 850.4f),
                        PathNode.LineTo(1064.4f, 873.4f),
                        PathNode.QuadTo(1064.4f, 890.4f, 1055.9f, 897.4f),
                        PathNode.QuadTo(1047.4f, 904.4f, 1036.4f, 904.4f),
                        PathNode.LineTo(871.4f, 904.4f),
                        PathNode.LineTo(871.4f, 819.4f),
                        PathNode.LineTo(1036.4f, 819.4f),
                        PathNode.QuadTo(1048.4f, 819.4f, 1056.4f, 826.4f),
                        PathNode.QuadTo(1064.4f, 833.4f, 1064.4f, 850.4f),
                        PathNode.Close,
                        PathNode.MoveTo(604.4f, 904.4f),
                        PathNode.LineTo(106.4f, 904.4f),
                        PathNode.QuadTo(95.4f, 904.4f, 86.9f, 897.4f),
                        PathNode.QuadTo(78.4f, 890.4f, 78.4f, 873.4f),
                        PathNode.LineTo(78.4f, 850.4f),
                        PathNode.QuadTo(78.4f, 833.4f, 86.4f, 826.4f),
                        PathNode.QuadTo(94.4f, 819.4f, 106.4f, 819.4f),
                        PathNode.LineTo(604.4f, 819.4f),
                        PathNode.Close,
                        PathNode.MoveTo(921.4f, 861.4f),
                        PathNode.QuadTo(921.4f, 910.4f, 897.4f, 951.9f),
                        PathNode.QuadTo(873.4f, 993.4f, 831.9f, 1017.4f),
                        PathNode.QuadTo(790.4f, 1041.4f, 741.4f, 1041.4f),
                        PathNode.QuadTo(692.4f, 1041.4f, 651.4f, 1017.4f),
                        PathNode.QuadTo(610.4f, 993.4f, 585.9f, 951.9f),
                        PathNode.QuadTo(561.4f, 910.4f, 561.4f, 861.4f),
                        PathNode.QuadTo(561.4f, 812.4f, 585.9f, 771.4f),
                        PathNode.QuadTo(610.4f, 730.4f, 651.4f, 705.9f),
                        PathNode.QuadTo(692.4f, 681.4f, 741.4f, 681.4f),
                        PathNode.QuadTo(790.4f, 681.4f, 831.9f, 705.9f),
                        PathNode.QuadTo(873.4f, 730.4f, 897.4f, 771.4f),
                        PathNode.QuadTo(921.4f, 812.4f, 921.4f, 861.4f),
                        PathNode.Close,
                        PathNode.MoveTo(647.4f, 861.4f),
                        PathNode.QuadTo(647.4f, 887.4f, 659.9f, 908.9f),
                        PathNode.QuadTo(672.4f, 930.4f, 693.9f, 943.4f),
                        PathNode.QuadTo(715.4f, 956.4f, 741.4f, 956.4f),
                        PathNode.QuadTo(767.4f, 956.4f, 789.4f, 943.4f),
                        PathNode.QuadTo(811.4f, 930.4f, 823.9f, 908.9f),
                        PathNode.QuadTo(836.4f, 887.4f, 836.4f, 861.4f),
                        PathNode.QuadTo(836.4f, 835.4f, 823.9f, 813.9f),
                        PathNode.QuadTo(811.4f, 792.4f, 789.4f, 779.4f),
                        PathNode.QuadTo(767.4f, 766.4f, 741.4f, 766.4f),
                        PathNode.QuadTo(715.4f, 766.4f, 693.9f, 779.4f),
                        PathNode.QuadTo(672.4f, 792.4f, 659.9f, 813.9f),
                        PathNode.QuadTo(647.4f, 835.4f, 647.4f, 861.4f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _tuneRegular!!
    }

private var _tuneRegular: ImageVector? = null
