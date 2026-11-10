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

internal val GlassIcons.Delete: ImageVector
    get() {
        if (_deleteRegular != null) return _deleteRegular!!
        _deleteRegular = ImageVector.Builder(
            name = "Delete.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(832.9f, 52.9f),
                        PathNode.QuadTo(873.9f, 71.9f, 894.9f, 109.9f),
                        PathNode.QuadTo(905.9f, 129.9f, 910.4f, 158.4f),
                        PathNode.QuadTo(914.9f, 186.9f, 918.9f, 249.9f),
                        PathNode.LineTo(961.9f, 843.9f),
                        PathNode.LineTo(1032.9f, 843.9f),
                        PathNode.QuadTo(1042.9f, 843.9f, 1050.9f, 851.9f),
                        PathNode.QuadTo(1058.9f, 859.9f, 1058.9f, 871.9f),
                        PathNode.LineTo(1058.9f, 900.9f),
                        PathNode.QuadTo(1058.9f, 912.9f, 1050.9f, 920.9f),
                        PathNode.QuadTo(1042.9f, 928.9f, 1032.9f, 928.9f),
                        PathNode.LineTo(853.9f, 928.9f),
                        PathNode.QuadTo(829.9f, 928.9f, 819.9f, 934.9f),
                        PathNode.QuadTo(809.9f, 940.9f, 800.9f, 960.9f),
                        PathNode.LineTo(778.9f, 1007.9f),
                        PathNode.QuadTo(775.9f, 1014.9f, 771.9f, 1021.9f),
                        PathNode.QuadTo(763.9f, 1039.9f, 756.9f, 1052.4f),
                        PathNode.QuadTo(749.9f, 1064.9f, 741.9f, 1072.9f),
                        PathNode.QuadTo(723.9f, 1090.9f, 702.9f, 1097.9f),
                        PathNode.QuadTo(690.9f, 1101.9f, 672.4f, 1102.9f),
                        PathNode.QuadTo(653.9f, 1103.9f, 627.9f, 1103.9f),
                        PathNode.LineTo(514.9f, 1103.9f),
                        PathNode.QuadTo(489.9f, 1103.9f, 471.4f, 1102.9f),
                        PathNode.QuadTo(452.9f, 1101.9f, 440.9f, 1097.9f),
                        PathNode.QuadTo(418.9f, 1090.9f, 400.9f, 1072.9f),
                        PathNode.QuadTo(392.9f, 1063.9f, 383.9f, 1047.4f),
                        PathNode.QuadTo(374.9f, 1030.9f, 363.9f, 1007.9f),
                        PathNode.LineTo(338.9f, 955.9f),
                        PathNode.QuadTo(330.9f, 937.9f, 322.4f, 933.4f),
                        PathNode.QuadTo(313.9f, 928.9f, 287.9f, 928.9f),
                        PathNode.LineTo(111.9f, 928.9f),
                        PathNode.QuadTo(101.9f, 928.9f, 92.9f, 921.4f),
                        PathNode.QuadTo(83.9f, 913.9f, 83.9f, 897.9f),
                        PathNode.LineTo(83.9f, 873.9f),
                        PathNode.QuadTo(83.9f, 858.9f, 92.9f, 851.4f),
                        PathNode.QuadTo(101.9f, 843.9f, 111.9f, 843.9f),
                        PathNode.LineTo(171.9f, 843.9f),
                        PathNode.LineTo(212.9f, 250.9f),
                        PathNode.QuadTo(216.9f, 186.9f, 221.4f, 158.4f),
                        PathNode.QuadTo(225.9f, 129.9f, 236.9f, 109.9f),
                        PathNode.QuadTo(260.9f, 70.9f, 298.9f, 52.9f),
                        PathNode.QuadTo(318.9f, 42.9f, 346.9f, 40.9f),
                        PathNode.QuadTo(374.9f, 38.9f, 440.9f, 38.9f),
                        PathNode.LineTo(691.9f, 38.9f),
                        PathNode.QuadTo(757.9f, 38.9f, 785.4f, 40.9f),
                        PathNode.QuadTo(812.9f, 42.9f, 832.9f, 52.9f),
                        PathNode.Close,
                        PathNode.MoveTo(342.9f, 132.9f),
                        PathNode.QuadTo(322.9f, 141.9f, 311.9f, 161.9f),
                        PathNode.QuadTo(306.9f, 170.9f, 304.4f, 188.9f),
                        PathNode.QuadTo(301.9f, 206.9f, 299.9f, 231.9f),
                        PathNode.LineTo(256.9f, 843.9f),
                        PathNode.LineTo(876.9f, 843.9f),
                        PathNode.LineTo(833.9f, 231.9f),
                        PathNode.LineTo(832.9f, 221.9f),
                        PathNode.QuadTo(830.9f, 196.9f, 828.9f, 183.9f),
                        PathNode.QuadTo(826.9f, 170.9f, 821.9f, 161.9f),
                        PathNode.QuadTo(810.9f, 141.9f, 790.9f, 132.9f),
                        PathNode.QuadTo(780.9f, 127.9f, 766.9f, 126.9f),
                        PathNode.QuadTo(752.9f, 125.9f, 719.9f, 125.9f),
                        PathNode.LineTo(413.9f, 125.9f),
                        PathNode.QuadTo(380.9f, 125.9f, 366.9f, 126.9f),
                        PathNode.QuadTo(352.9f, 127.9f, 342.9f, 132.9f),
                        PathNode.Close,
                        PathNode.MoveTo(517.9f, 289.9f),
                        PathNode.LineTo(501.9f, 705.9f),
                        PathNode.QuadTo(500.9f, 715.9f, 493.4f, 723.4f),
                        PathNode.QuadTo(485.9f, 730.9f, 474.9f, 730.9f),
                        PathNode.LineTo(442.9f, 730.9f),
                        PathNode.QuadTo(431.9f, 730.9f, 423.9f, 722.9f),
                        PathNode.QuadTo(415.9f, 714.9f, 415.9f, 703.9f),
                        PathNode.LineTo(432.9f, 287.9f),
                        PathNode.QuadTo(433.9f, 276.9f, 441.4f, 269.4f),
                        PathNode.QuadTo(448.9f, 261.9f, 459.9f, 261.9f),
                        PathNode.LineTo(491.9f, 261.9f),
                        PathNode.QuadTo(502.9f, 261.9f, 510.9f, 270.4f),
                        PathNode.QuadTo(518.9f, 278.9f, 517.9f, 289.9f),
                        PathNode.Close,
                        PathNode.MoveTo(700.9f, 287.9f),
                        PathNode.LineTo(717.9f, 703.9f),
                        PathNode.QuadTo(717.9f, 714.9f, 709.9f, 722.9f),
                        PathNode.QuadTo(701.9f, 730.9f, 690.9f, 730.9f),
                        PathNode.LineTo(658.9f, 730.9f),
                        PathNode.QuadTo(647.9f, 730.9f, 640.4f, 723.4f),
                        PathNode.QuadTo(632.9f, 715.9f, 631.9f, 705.9f),
                        PathNode.LineTo(615.9f, 289.9f),
                        PathNode.QuadTo(614.9f, 278.9f, 622.9f, 270.4f),
                        PathNode.QuadTo(630.9f, 261.9f, 641.9f, 261.9f),
                        PathNode.LineTo(673.9f, 261.9f),
                        PathNode.QuadTo(684.9f, 261.9f, 692.4f, 269.4f),
                        PathNode.QuadTo(699.9f, 276.9f, 700.9f, 287.9f),
                        PathNode.Close,
                        PathNode.MoveTo(425.9f, 937.9f),
                        PathNode.LineTo(447.9f, 982.9f),
                        PathNode.QuadTo(457.9f, 1004.9f, 470.4f, 1011.4f),
                        PathNode.QuadTo(482.9f, 1017.9f, 508.9f, 1017.9f),
                        PathNode.LineTo(633.9f, 1017.9f),
                        PathNode.QuadTo(660.9f, 1017.9f, 672.4f, 1011.4f),
                        PathNode.QuadTo(683.9f, 1004.9f, 693.9f, 984.9f),
                        PathNode.LineTo(717.9f, 937.9f),
                        PathNode.QuadTo(719.9f, 934.9f, 718.4f, 931.9f),
                        PathNode.QuadTo(716.9f, 928.9f, 712.9f, 928.9f),
                        PathNode.LineTo(430.9f, 928.9f),
                        PathNode.QuadTo(420.9f, 928.9f, 425.9f, 937.9f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _deleteRegular!!
    }

private var _deleteRegular: ImageVector? = null
