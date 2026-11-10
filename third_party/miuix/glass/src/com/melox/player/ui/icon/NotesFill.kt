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

internal val GlassIcons.NotesFill: ImageVector
    get() {
        if (_notesFillRegular != null) return _notesFillRegular!!
        _notesFillRegular = ImageVector.Builder(
            name = "NotesFill.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(911.4f, 78.9f),
                        PathNode.QuadTo(938.4f, 91.9f, 959.9f, 113.4f),
                        PathNode.QuadTo(981.4f, 134.9f, 994.4f, 161.9f),
                        PathNode.QuadTo(1008.4f, 188.9f, 1011.9f, 228.9f),
                        PathNode.QuadTo(1015.4f, 268.9f, 1015.4f, 361.9f),
                        PathNode.LineTo(1015.4f, 932.9f),
                        PathNode.QuadTo(1015.4f, 979.9f, 1013.9f, 999.4f),
                        PathNode.QuadTo(1012.4f, 1018.9f, 1005.4f, 1032.9f),
                        PathNode.QuadTo(992.4f, 1058.9f, 963.4f, 1074.9f),
                        PathNode.QuadTo(949.4f, 1081.9f, 929.9f, 1083.4f),
                        PathNode.QuadTo(910.4f, 1084.9f, 863.4f, 1084.9f),
                        PathNode.LineTo(431.4f, 1084.9f),
                        PathNode.QuadTo(338.4f, 1084.9f, 298.4f, 1081.4f),
                        PathNode.QuadTo(258.4f, 1077.9f, 231.4f, 1063.9f),
                        PathNode.QuadTo(204.4f, 1050.9f, 183.4f, 1029.4f),
                        PathNode.QuadTo(162.4f, 1007.9f, 148.4f, 980.9f),
                        PathNode.QuadTo(134.4f, 952.9f, 130.9f, 913.4f),
                        PathNode.QuadTo(127.4f, 873.9f, 127.4f, 780.9f),
                        PathNode.LineTo(127.4f, 209.9f),
                        PathNode.QuadTo(127.4f, 162.9f, 128.9f, 143.4f),
                        PathNode.QuadTo(130.4f, 123.9f, 137.4f, 109.9f),
                        PathNode.QuadTo(144.4f, 95.9f, 155.4f, 85.4f),
                        PathNode.QuadTo(166.4f, 74.9f, 179.4f, 67.9f),
                        PathNode.QuadTo(193.4f, 60.9f, 213.4f, 59.4f),
                        PathNode.QuadTo(233.4f, 57.9f, 279.4f, 57.9f),
                        PathNode.LineTo(711.4f, 57.9f),
                        PathNode.QuadTo(804.4f, 57.9f, 844.4f, 61.4f),
                        PathNode.QuadTo(884.4f, 64.9f, 911.4f, 78.9f),
                        PathNode.Close,
                        PathNode.MoveTo(333.4f, 566.9f),
                        PathNode.LineTo(333.4f, 576.9f),
                        PathNode.QuadTo(333.4f, 591.9f, 340.4f, 598.4f),
                        PathNode.QuadTo(347.4f, 604.9f, 360.4f, 604.9f),
                        PathNode.LineTo(783.4f, 604.9f),
                        PathNode.QuadTo(796.4f, 604.9f, 802.9f, 598.4f),
                        PathNode.QuadTo(809.4f, 591.9f, 809.4f, 576.9f),
                        PathNode.LineTo(809.4f, 566.9f),
                        PathNode.QuadTo(809.4f, 551.9f, 803.4f, 544.9f),
                        PathNode.QuadTo(797.4f, 537.9f, 782.4f, 537.9f),
                        PathNode.LineTo(360.4f, 537.9f),
                        PathNode.QuadTo(346.4f, 537.9f, 339.9f, 544.9f),
                        PathNode.QuadTo(333.4f, 551.9f, 333.4f, 566.9f),
                        PathNode.Close,
                        PathNode.MoveTo(333.4f, 329.9f),
                        PathNode.LineTo(333.4f, 339.9f),
                        PathNode.QuadTo(333.4f, 354.9f, 340.4f, 361.4f),
                        PathNode.QuadTo(347.4f, 367.9f, 360.4f, 367.9f),
                        PathNode.LineTo(597.4f, 367.9f),
                        PathNode.QuadTo(610.4f, 367.9f, 616.9f, 361.4f),
                        PathNode.QuadTo(623.4f, 354.9f, 623.4f, 339.9f),
                        PathNode.LineTo(623.4f, 329.9f),
                        PathNode.QuadTo(623.4f, 314.9f, 617.4f, 307.9f),
                        PathNode.QuadTo(611.4f, 300.9f, 596.4f, 300.9f),
                        PathNode.LineTo(360.4f, 300.9f),
                        PathNode.QuadTo(346.4f, 300.9f, 339.9f, 307.9f),
                        PathNode.QuadTo(333.4f, 314.9f, 333.4f, 329.9f),
                        PathNode.Close,
                        PathNode.MoveTo(333.4f, 802.9f),
                        PathNode.LineTo(333.4f, 812.9f),
                        PathNode.QuadTo(333.4f, 827.9f, 340.4f, 834.4f),
                        PathNode.QuadTo(347.4f, 840.9f, 360.4f, 840.9f),
                        PathNode.LineTo(783.4f, 840.9f),
                        PathNode.QuadTo(796.4f, 840.9f, 802.9f, 834.4f),
                        PathNode.QuadTo(809.4f, 827.9f, 809.4f, 812.9f),
                        PathNode.LineTo(809.4f, 802.9f),
                        PathNode.QuadTo(809.4f, 787.9f, 803.4f, 780.9f),
                        PathNode.QuadTo(797.4f, 773.9f, 782.4f, 773.9f),
                        PathNode.LineTo(360.4f, 773.9f),
                        PathNode.QuadTo(346.4f, 773.9f, 339.9f, 780.9f),
                        PathNode.QuadTo(333.4f, 787.9f, 333.4f, 802.9f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _notesFillRegular!!
    }

private var _notesFillRegular: ImageVector? = null
