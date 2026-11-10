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

internal val GlassIcons.Create: ImageVector
    get() {
        if (_createRegular != null) return _createRegular!!
        _createRegular = ImageVector.Builder(
            name = "Create.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(943.9f, 938.2f),
                        PathNode.QuadTo(926.9f, 943.2f, 898.9f, 948.2f),
                        PathNode.QuadTo(877.9f, 951.2f, 849.4f, 950.7f),
                        PathNode.QuadTo(820.9f, 950.2f, 797.9f, 944.2f),
                        PathNode.QuadTo(770.9f, 937.2f, 738.9f, 925.2f),
                        PathNode.QuadTo(718.9f, 970.2f, 694.9f, 1001.2f),
                        PathNode.QuadTo(671.9f, 1032.2f, 631.9f, 1068.2f),
                        PathNode.QuadTo(615.9f, 1084.2f, 597.9f, 1097.2f),
                        PathNode.QuadTo(585.9f, 1106.2f, 571.4f, 1105.7f),
                        PathNode.QuadTo(556.9f, 1105.2f, 542.9f, 1096.2f),
                        PathNode.QuadTo(533.9f, 1089.2f, 510.9f, 1068.2f),
                        PathNode.QuadTo(469.9f, 1032.2f, 447.9f, 1001.2f),
                        PathNode.QuadTo(423.9f, 970.2f, 403.9f, 925.2f),
                        PathNode.QuadTo(371.9f, 937.2f, 344.9f, 944.2f),
                        PathNode.QuadTo(323.9f, 949.2f, 296.9f, 950.2f),
                        PathNode.QuadTo(269.9f, 951.2f, 243.9f, 948.2f),
                        PathNode.QuadTo(220.9f, 945.2f, 198.9f, 938.2f),
                        PathNode.QuadTo(184.9f, 934.2f, 177.4f, 923.7f),
                        PathNode.QuadTo(169.9f, 913.2f, 169.9f, 900.2f),
                        PathNode.LineTo(169.9f, 666.2f),
                        PathNode.QuadTo(169.9f, 557.2f, 223.9f, 464.7f),
                        PathNode.QuadTo(277.9f, 372.2f, 370.4f, 318.2f),
                        PathNode.QuadTo(462.9f, 264.2f, 571.9f, 264.2f),
                        PathNode.QuadTo(680.9f, 264.2f, 773.4f, 318.2f),
                        PathNode.QuadTo(865.9f, 372.2f, 919.9f, 464.7f),
                        PathNode.QuadTo(973.9f, 557.2f, 973.9f, 666.2f),
                        PathNode.LineTo(973.9f, 897.2f),
                        PathNode.QuadTo(973.9f, 911.2f, 965.9f, 922.7f),
                        PathNode.QuadTo(957.9f, 934.2f, 943.9f, 938.2f),
                        PathNode.Close,
                        PathNode.MoveTo(457.9f, 837.2f),
                        PathNode.QuadTo(464.9f, 844.2f, 467.9f, 851.2f),
                        PathNode.LineTo(470.9f, 860.2f),
                        PathNode.QuadTo(477.9f, 878.2f, 489.4f, 904.2f),
                        PathNode.QuadTo(500.9f, 930.2f, 515.9f, 949.2f),
                        PathNode.QuadTo(521.9f, 957.2f, 556.9f, 994.2f),
                        PathNode.LineTo(570.9f, 1008.2f),
                        PathNode.QuadTo(605.9f, 975.2f, 626.9f, 949.2f),
                        PathNode.QuadTo(651.9f, 916.2f, 671.9f, 859.2f),
                        PathNode.LineTo(672.9f, 855.2f),
                        PathNode.QuadTo(677.9f, 842.2f, 684.9f, 837.2f),
                        PathNode.QuadTo(698.9f, 826.2f, 712.9f, 826.2f),
                        PathNode.QuadTo(724.9f, 826.2f, 734.9f, 832.2f),
                        PathNode.QuadTo(746.9f, 838.2f, 766.4f, 845.7f),
                        PathNode.QuadTo(785.9f, 853.2f, 816.9f, 861.2f),
                        PathNode.QuadTo(844.9f, 868.2f, 887.9f, 864.2f),
                        PathNode.LineTo(887.9f, 666.2f),
                        PathNode.QuadTo(887.9f, 580.2f, 845.4f, 507.7f),
                        PathNode.QuadTo(802.9f, 435.2f, 730.4f, 392.7f),
                        PathNode.QuadTo(657.9f, 350.2f, 571.9f, 350.2f),
                        PathNode.QuadTo(485.9f, 350.2f, 412.9f, 392.7f),
                        PathNode.QuadTo(339.9f, 435.2f, 297.4f, 507.7f),
                        PathNode.QuadTo(254.9f, 580.2f, 254.9f, 666.2f),
                        PathNode.LineTo(254.9f, 864.2f),
                        PathNode.QuadTo(274.9f, 866.2f, 292.9f, 865.7f),
                        PathNode.QuadTo(310.9f, 865.2f, 325.9f, 861.2f),
                        PathNode.QuadTo(374.9f, 848.2f, 407.9f, 832.2f),
                        PathNode.QuadTo(418.9f, 826.2f, 429.9f, 826.2f),
                        PathNode.QuadTo(446.9f, 826.2f, 457.9f, 837.2f),
                        PathNode.Close,
                        PathNode.MoveTo(463.9f, 86.2f),
                        PathNode.LineTo(463.9f, 112.2f),
                        PathNode.QuadTo(463.9f, 126.2f, 459.4f, 133.2f),
                        PathNode.QuadTo(454.9f, 140.2f, 442.9f, 144.2f),
                        PathNode.QuadTo(376.9f, 165.2f, 325.9f, 199.2f),
                        PathNode.QuadTo(274.9f, 233.2f, 230.9f, 283.2f),
                        PathNode.QuadTo(217.9f, 297.2f, 206.9f, 299.7f),
                        PathNode.QuadTo(195.9f, 302.2f, 182.9f, 290.2f),
                        PathNode.LineTo(161.9f, 272.2f),
                        PathNode.QuadTo(152.9f, 264.2f, 152.9f, 254.2f),
                        PathNode.QuadTo(152.9f, 244.2f, 158.9f, 236.2f),
                        PathNode.QuadTo(210.9f, 172.2f, 277.9f, 128.2f),
                        PathNode.QuadTo(344.9f, 84.2f, 426.9f, 60.2f),
                        PathNode.QuadTo(441.9f, 55.2f, 452.9f, 63.2f),
                        PathNode.QuadTo(463.9f, 71.2f, 463.9f, 86.2f),
                        PathNode.Close,
                        PathNode.MoveTo(614.9f, 67.2f),
                        PathNode.LineTo(614.9f, 316.2f),
                        PathNode.LineTo(528.9f, 316.2f),
                        PathNode.LineTo(528.9f, 67.2f),
                        PathNode.QuadTo(528.9f, 53.2f, 536.4f, 45.2f),
                        PathNode.QuadTo(543.9f, 37.2f, 560.9f, 37.2f),
                        PathNode.LineTo(582.9f, 37.2f),
                        PathNode.QuadTo(600.9f, 37.2f, 607.9f, 45.2f),
                        PathNode.QuadTo(614.9f, 53.2f, 614.9f, 67.2f),
                        PathNode.Close,
                        PathNode.MoveTo(982.9f, 233.2f),
                        PathNode.QuadTo(990.9f, 242.2f, 989.9f, 253.7f),
                        PathNode.QuadTo(988.9f, 265.2f, 980.9f, 271.2f),
                        PathNode.LineTo(956.9f, 292.2f),
                        PathNode.QuadTo(945.9f, 301.2f, 935.9f, 299.2f),
                        PathNode.QuadTo(925.9f, 297.2f, 918.9f, 289.2f),
                        PathNode.QuadTo(870.9f, 235.2f, 819.9f, 201.2f),
                        PathNode.QuadTo(768.9f, 167.2f, 707.9f, 147.2f),
                        PathNode.QuadTo(689.9f, 141.2f, 684.9f, 134.7f),
                        PathNode.QuadTo(679.9f, 128.2f, 679.9f, 109.2f),
                        PathNode.LineTo(679.9f, 87.2f),
                        PathNode.QuadTo(679.9f, 71.2f, 689.9f, 63.7f),
                        PathNode.QuadTo(699.9f, 56.2f, 711.9f, 59.2f),
                        PathNode.QuadTo(795.9f, 83.2f, 861.4f, 125.7f),
                        PathNode.QuadTo(926.9f, 168.2f, 982.9f, 233.2f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _createRegular!!
    }

private var _createRegular: ImageVector? = null
