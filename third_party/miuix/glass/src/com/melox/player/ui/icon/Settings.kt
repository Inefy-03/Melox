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

internal val GlassIcons.Settings: ImageVector
    get() {
        if (_settingsRegular != null) return _settingsRegular!!
        _settingsRegular = ImageVector.Builder(
            name = "Settings.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(595.4f, 60.2f),
                        PathNode.QuadTo(606.4f, 62.2f, 620.4f, 69.2f),
                        PathNode.QuadTo(634.4f, 76.2f, 651.4f, 86.2f),
                        PathNode.LineTo(664.4f, 93.2f),
                        PathNode.LineTo(945.4f, 251.2f),
                        PathNode.QuadTo(975.4f, 268.2f, 988.9f, 276.7f),
                        PathNode.QuadTo(1002.4f, 285.2f, 1011.4f, 295.2f),
                        PathNode.QuadTo(1029.4f, 314.2f, 1036.4f, 338.2f),
                        PathNode.QuadTo(1040.4f, 350.2f, 1041.4f, 370.2f),
                        PathNode.QuadTo(1042.4f, 390.2f, 1042.4f, 417.2f),
                        PathNode.LineTo(1042.4f, 726.2f),
                        PathNode.QuadTo(1042.4f, 753.2f, 1041.4f, 772.7f),
                        PathNode.QuadTo(1040.4f, 792.2f, 1036.4f, 804.2f),
                        PathNode.QuadTo(1029.4f, 827.2f, 1011.4f, 848.2f),
                        PathNode.QuadTo(1002.4f, 858.2f, 985.9f, 868.2f),
                        PathNode.QuadTo(969.4f, 878.2f, 945.4f, 891.2f),
                        PathNode.LineTo(664.4f, 1050.2f),
                        PathNode.QuadTo(642.4f, 1063.2f, 624.9f, 1071.7f),
                        PathNode.QuadTo(607.4f, 1080.2f, 595.4f, 1082.2f),
                        PathNode.QuadTo(571.4f, 1088.2f, 547.4f, 1082.2f),
                        PathNode.QuadTo(535.4f, 1080.2f, 517.9f, 1071.7f),
                        PathNode.QuadTo(500.4f, 1063.2f, 478.4f, 1050.2f),
                        PathNode.LineTo(197.4f, 891.2f),
                        PathNode.QuadTo(190.4f, 887.2f, 182.4f, 883.2f),
                        PathNode.QuadTo(165.4f, 873.2f, 151.9f, 864.7f),
                        PathNode.QuadTo(138.4f, 856.2f, 131.4f, 848.2f),
                        PathNode.QuadTo(113.4f, 827.2f, 106.4f, 804.2f),
                        PathNode.QuadTo(102.4f, 792.2f, 101.4f, 772.7f),
                        PathNode.QuadTo(100.4f, 753.2f, 100.4f, 726.2f),
                        PathNode.LineTo(100.4f, 417.2f),
                        PathNode.QuadTo(100.4f, 390.2f, 101.4f, 370.2f),
                        PathNode.QuadTo(102.4f, 350.2f, 106.4f, 338.2f),
                        PathNode.QuadTo(113.4f, 314.2f, 131.4f, 295.2f),
                        PathNode.QuadTo(140.4f, 285.2f, 157.4f, 274.7f),
                        PathNode.QuadTo(174.4f, 264.2f, 197.4f, 251.2f),
                        PathNode.LineTo(478.4f, 93.2f),
                        PathNode.LineTo(491.4f, 86.2f),
                        PathNode.QuadTo(508.4f, 76.2f, 522.4f, 69.2f),
                        PathNode.QuadTo(536.4f, 62.2f, 547.4f, 60.2f),
                        PathNode.QuadTo(571.4f, 55.2f, 595.4f, 60.2f),
                        PathNode.Close,
                        PathNode.MoveTo(186.4f, 390.2f),
                        PathNode.LineTo(186.4f, 759.2f),
                        PathNode.QuadTo(186.4f, 773.2f, 192.9f, 783.7f),
                        PathNode.QuadTo(199.4f, 794.2f, 214.4f, 803.2f),
                        PathNode.LineTo(539.4f, 986.2f),
                        PathNode.QuadTo(558.4f, 997.2f, 569.9f, 997.7f),
                        PathNode.QuadTo(581.4f, 998.2f, 598.4f, 989.2f),
                        PathNode.LineTo(918.4f, 809.2f),
                        PathNode.QuadTo(943.4f, 795.2f, 950.4f, 783.2f),
                        PathNode.QuadTo(957.4f, 771.2f, 957.4f, 742.2f),
                        PathNode.LineTo(957.4f, 393.2f),
                        PathNode.QuadTo(957.4f, 374.2f, 952.4f, 362.7f),
                        PathNode.QuadTo(947.4f, 351.2f, 933.4f, 343.2f),
                        PathNode.LineTo(603.4f, 156.2f),
                        PathNode.QuadTo(584.4f, 145.2f, 571.9f, 145.2f),
                        PathNode.QuadTo(559.4f, 145.2f, 539.4f, 156.2f),
                        PathNode.LineTo(217.4f, 338.2f),
                        PathNode.QuadTo(199.4f, 348.2f, 192.9f, 358.7f),
                        PathNode.QuadTo(186.4f, 369.2f, 186.4f, 390.2f),
                        PathNode.Close,
                        PathNode.MoveTo(771.4f, 571.2f),
                        PathNode.QuadTo(771.4f, 626.2f, 744.4f, 671.7f),
                        PathNode.QuadTo(717.4f, 717.2f, 671.9f, 744.2f),
                        PathNode.QuadTo(626.4f, 771.2f, 571.4f, 771.2f),
                        PathNode.QuadTo(517.4f, 771.2f, 471.4f, 744.2f),
                        PathNode.QuadTo(425.4f, 717.2f, 398.4f, 671.7f),
                        PathNode.QuadTo(371.4f, 626.2f, 371.4f, 571.2f),
                        PathNode.QuadTo(371.4f, 517.2f, 398.4f, 471.2f),
                        PathNode.QuadTo(425.4f, 425.2f, 471.4f, 398.2f),
                        PathNode.QuadTo(517.4f, 371.2f, 571.4f, 371.2f),
                        PathNode.QuadTo(626.4f, 371.2f, 671.9f, 398.2f),
                        PathNode.QuadTo(717.4f, 425.2f, 744.4f, 471.2f),
                        PathNode.QuadTo(771.4f, 517.2f, 771.4f, 571.2f),
                        PathNode.Close,
                        PathNode.MoveTo(459.4f, 571.2f),
                        PathNode.QuadTo(459.4f, 602.2f, 474.4f, 627.7f),
                        PathNode.QuadTo(489.4f, 653.2f, 514.9f, 668.2f),
                        PathNode.QuadTo(540.4f, 683.2f, 571.4f, 683.2f),
                        PathNode.QuadTo(602.4f, 683.2f, 627.9f, 668.2f),
                        PathNode.QuadTo(653.4f, 653.2f, 668.4f, 627.7f),
                        PathNode.QuadTo(683.4f, 602.2f, 683.4f, 571.2f),
                        PathNode.QuadTo(683.4f, 540.2f, 668.4f, 514.7f),
                        PathNode.QuadTo(653.4f, 489.2f, 627.9f, 474.2f),
                        PathNode.QuadTo(602.4f, 459.2f, 571.4f, 459.2f),
                        PathNode.QuadTo(540.4f, 459.2f, 514.9f, 474.2f),
                        PathNode.QuadTo(489.4f, 489.2f, 474.4f, 514.7f),
                        PathNode.QuadTo(459.4f, 540.2f, 459.4f, 571.2f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _settingsRegular!!
    }

private var _settingsRegular: ImageVector? = null
