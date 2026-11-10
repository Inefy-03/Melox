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

internal val GlassIcons.RecordingTape: ImageVector
    get() {
        if (_recordingTapeRegular != null) return _recordingTapeRegular!!
        _recordingTapeRegular = ImageVector.Builder(
            name = "RecordingTape.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(979.9f, 169.4f),
                        PathNode.QuadTo(1004.9f, 182.4f, 1024.9f, 202.9f),
                        PathNode.QuadTo(1044.9f, 223.4f, 1057.9f, 248.4f),
                        PathNode.QuadTo(1072.9f, 277.4f, 1076.4f, 318.4f),
                        PathNode.QuadTo(1079.9f, 359.4f, 1079.9f, 455.4f),
                        PathNode.LineTo(1079.9f, 688.4f),
                        PathNode.QuadTo(1079.9f, 784.4f, 1076.4f, 824.9f),
                        PathNode.QuadTo(1072.9f, 865.4f, 1057.9f, 895.4f),
                        PathNode.QuadTo(1044.9f, 920.4f, 1024.9f, 940.4f),
                        PathNode.QuadTo(1004.9f, 960.4f, 979.9f, 973.4f),
                        PathNode.QuadTo(949.9f, 988.4f, 909.4f, 991.9f),
                        PathNode.QuadTo(868.9f, 995.4f, 772.9f, 995.4f),
                        PathNode.LineTo(370.9f, 995.4f),
                        PathNode.QuadTo(274.9f, 995.4f, 233.9f, 991.9f),
                        PathNode.QuadTo(192.9f, 988.4f, 163.9f, 973.4f),
                        PathNode.QuadTo(138.9f, 960.4f, 118.4f, 940.4f),
                        PathNode.QuadTo(97.9f, 920.4f, 84.9f, 895.4f),
                        PathNode.QuadTo(69.9f, 865.4f, 66.4f, 824.9f),
                        PathNode.QuadTo(62.9f, 784.4f, 62.9f, 688.4f),
                        PathNode.LineTo(62.9f, 455.4f),
                        PathNode.QuadTo(62.9f, 359.4f, 66.4f, 318.4f),
                        PathNode.QuadTo(69.9f, 277.4f, 84.9f, 248.4f),
                        PathNode.QuadTo(97.9f, 223.4f, 118.4f, 202.9f),
                        PathNode.QuadTo(138.9f, 182.4f, 163.9f, 169.4f),
                        PathNode.QuadTo(192.9f, 154.4f, 233.9f, 150.9f),
                        PathNode.QuadTo(274.9f, 147.4f, 370.9f, 147.4f),
                        PathNode.LineTo(772.9f, 147.4f),
                        PathNode.QuadTo(868.9f, 147.4f, 909.4f, 150.9f),
                        PathNode.QuadTo(949.9f, 154.4f, 979.9f, 169.4f),
                        PathNode.Close,
                        PathNode.MoveTo(206.9f, 245.4f),
                        PathNode.QuadTo(175.9f, 260.4f, 160.9f, 291.4f),
                        PathNode.QuadTo(151.9f, 307.4f, 149.9f, 330.9f),
                        PathNode.QuadTo(147.9f, 354.4f, 147.9f, 410.4f),
                        PathNode.LineTo(147.9f, 733.4f),
                        PathNode.QuadTo(147.9f, 788.4f, 149.9f, 812.4f),
                        PathNode.QuadTo(151.9f, 836.4f, 160.9f, 852.4f),
                        PathNode.QuadTo(174.9f, 881.4f, 206.9f, 898.4f),
                        PathNode.QuadTo(222.9f, 906.4f, 246.4f, 908.4f),
                        PathNode.QuadTo(269.9f, 910.4f, 324.9f, 910.4f),
                        PathNode.LineTo(816.9f, 910.4f),
                        PathNode.QuadTo(872.9f, 910.4f, 896.4f, 908.4f),
                        PathNode.QuadTo(919.9f, 906.4f, 935.9f, 898.4f),
                        PathNode.QuadTo(965.9f, 882.4f, 982.9f, 852.4f),
                        PathNode.QuadTo(990.9f, 836.4f, 992.9f, 812.4f),
                        PathNode.QuadTo(994.9f, 788.4f, 994.9f, 733.4f),
                        PathNode.LineTo(994.9f, 410.4f),
                        PathNode.QuadTo(994.9f, 354.4f, 992.9f, 330.9f),
                        PathNode.QuadTo(990.9f, 307.4f, 982.9f, 291.4f),
                        PathNode.QuadTo(964.9f, 259.4f, 935.9f, 245.4f),
                        PathNode.QuadTo(919.9f, 236.4f, 896.4f, 234.4f),
                        PathNode.QuadTo(872.9f, 232.4f, 816.9f, 232.4f),
                        PathNode.LineTo(324.9f, 232.4f),
                        PathNode.QuadTo(269.9f, 232.4f, 246.4f, 234.4f),
                        PathNode.QuadTo(222.9f, 236.4f, 206.9f, 245.4f),
                        PathNode.Close,
                        PathNode.MoveTo(527.9f, 571.4f),
                        PathNode.QuadTo(527.9f, 599.4f, 518.9f, 623.4f),
                        PathNode.QuadTo(516.9f, 628.4f, 520.9f, 628.4f),
                        PathNode.LineTo(622.9f, 628.4f),
                        PathNode.QuadTo(623.9f, 628.4f, 624.9f, 627.4f),
                        PathNode.QuadTo(625.9f, 626.4f, 624.9f, 625.4f),
                        PathNode.QuadTo(613.9f, 599.4f, 613.9f, 571.4f),
                        PathNode.QuadTo(613.9f, 533.4f, 632.9f, 500.4f),
                        PathNode.QuadTo(651.9f, 467.4f, 684.9f, 448.4f),
                        PathNode.QuadTo(717.9f, 429.4f, 756.9f, 429.4f),
                        PathNode.QuadTo(795.9f, 429.4f, 828.9f, 448.4f),
                        PathNode.QuadTo(861.9f, 467.4f, 880.9f, 500.4f),
                        PathNode.QuadTo(899.9f, 533.4f, 899.9f, 571.4f),
                        PathNode.QuadTo(899.9f, 619.4f, 872.9f, 655.4f),
                        PathNode.QuadTo(845.9f, 691.4f, 805.9f, 705.4f),
                        PathNode.QuadTo(785.9f, 711.4f, 758.4f, 712.9f),
                        PathNode.QuadTo(730.9f, 714.4f, 680.9f, 714.4f),
                        PathNode.LineTo(461.9f, 714.4f),
                        PathNode.QuadTo(411.9f, 714.4f, 384.4f, 712.9f),
                        PathNode.QuadTo(356.9f, 711.4f, 337.9f, 705.4f),
                        PathNode.QuadTo(295.9f, 691.4f, 269.4f, 654.4f),
                        PathNode.QuadTo(242.9f, 617.4f, 242.9f, 571.4f),
                        PathNode.QuadTo(242.9f, 533.4f, 261.9f, 500.4f),
                        PathNode.QuadTo(280.9f, 467.4f, 313.9f, 448.4f),
                        PathNode.QuadTo(346.9f, 429.4f, 384.9f, 429.4f),
                        PathNode.QuadTo(423.9f, 429.4f, 456.9f, 448.4f),
                        PathNode.QuadTo(489.9f, 467.4f, 508.9f, 500.4f),
                        PathNode.QuadTo(527.9f, 533.4f, 527.9f, 571.4f),
                        PathNode.Close,
                        PathNode.MoveTo(327.9f, 571.4f),
                        PathNode.QuadTo(327.9f, 595.4f, 344.9f, 611.9f),
                        PathNode.QuadTo(361.9f, 628.4f, 384.9f, 628.4f),
                        PathNode.QuadTo(408.9f, 628.4f, 425.4f, 611.9f),
                        PathNode.QuadTo(441.9f, 595.4f, 441.9f, 571.4f),
                        PathNode.QuadTo(441.9f, 547.4f, 425.4f, 530.9f),
                        PathNode.QuadTo(408.9f, 514.4f, 384.9f, 514.4f),
                        PathNode.QuadTo(361.9f, 514.4f, 344.9f, 530.9f),
                        PathNode.QuadTo(327.9f, 547.4f, 327.9f, 571.4f),
                        PathNode.Close,
                        PathNode.MoveTo(699.9f, 571.4f),
                        PathNode.QuadTo(699.9f, 595.4f, 716.9f, 611.9f),
                        PathNode.QuadTo(733.9f, 628.4f, 756.9f, 628.4f),
                        PathNode.QuadTo(780.9f, 628.4f, 797.9f, 611.9f),
                        PathNode.QuadTo(814.9f, 595.4f, 814.9f, 571.4f),
                        PathNode.QuadTo(814.9f, 547.4f, 797.9f, 530.9f),
                        PathNode.QuadTo(780.9f, 514.4f, 756.9f, 514.4f),
                        PathNode.QuadTo(733.9f, 514.4f, 716.9f, 530.9f),
                        PathNode.QuadTo(699.9f, 547.4f, 699.9f, 571.4f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _recordingTapeRegular!!
    }

private var _recordingTapeRegular: ImageVector? = null
