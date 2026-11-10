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

internal val GlassIcons.Hide: ImageVector
    get() {
        if (_hideRegular != null) return _hideRegular!!
        _hideRegular = ImageVector.Builder(
            name = "Hide.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(978.4f, 144.9f),
                        PathNode.LineTo(996.4f, 162.9f),
                        PathNode.QuadTo(1007.4f, 173.9f, 1006.4f, 183.9f),
                        PathNode.QuadTo(1005.4f, 193.9f, 994.4f, 204.9f),
                        PathNode.LineTo(201.4f, 998.9f),
                        PathNode.QuadTo(191.4f, 1008.9f, 180.9f, 1009.4f),
                        PathNode.QuadTo(170.4f, 1009.9f, 158.4f, 997.9f),
                        PathNode.LineTo(143.4f, 982.9f),
                        PathNode.QuadTo(131.4f, 970.9f, 130.4f, 960.4f),
                        PathNode.QuadTo(129.4f, 949.9f, 142.4f, 936.9f),
                        PathNode.LineTo(936.4f, 142.9f),
                        PathNode.QuadTo(946.4f, 132.9f, 956.9f, 133.4f),
                        PathNode.QuadTo(967.4f, 133.9f, 978.4f, 144.9f),
                        PathNode.Close,
                        PathNode.MoveTo(775.4f, 192.9f),
                        PathNode.LineTo(708.4f, 260.9f),
                        PathNode.QuadTo(639.4f, 240.9f, 571.4f, 240.9f),
                        PathNode.QuadTo(473.4f, 240.9f, 377.9f, 280.9f),
                        PathNode.QuadTo(282.4f, 320.9f, 206.4f, 391.4f),
                        PathNode.QuadTo(130.4f, 461.9f, 89.4f, 551.9f),
                        PathNode.QuadTo(86.4f, 559.9f, 86.4f, 567.4f),
                        PathNode.QuadTo(86.4f, 574.9f, 89.4f, 581.9f),
                        PathNode.QuadTo(111.4f, 629.9f, 143.4f, 672.4f),
                        PathNode.QuadTo(175.4f, 714.9f, 216.4f, 751.9f),
                        PathNode.LineTo(156.4f, 812.9f),
                        PathNode.QuadTo(110.4f, 770.9f, 73.4f, 721.9f),
                        PathNode.QuadTo(36.4f, 672.9f, 12.4f, 617.9f),
                        PathNode.QuadTo(1.4f, 593.9f, 1.4f, 567.4f),
                        PathNode.QuadTo(1.4f, 540.9f, 12.4f, 516.9f),
                        PathNode.QuadTo(59.4f, 411.9f, 147.4f, 329.4f),
                        PathNode.QuadTo(235.4f, 246.9f, 346.4f, 200.9f),
                        PathNode.QuadTo(457.4f, 154.9f, 571.4f, 154.9f),
                        PathNode.QuadTo(671.4f, 154.9f, 775.4f, 192.9f),
                        PathNode.Close,
                        PathNode.MoveTo(621.4f, 347.9f),
                        PathNode.LineTo(538.4f, 431.9f),
                        PathNode.QuadTo(519.4f, 435.9f, 500.4f, 446.9f),
                        PathNode.QuadTo(481.4f, 456.9f, 467.4f, 472.9f),
                        PathNode.QuadTo(442.4f, 498.9f, 433.4f, 535.9f),
                        PathNode.LineTo(350.4f, 619.9f),
                        PathNode.QuadTo(344.4f, 594.9f, 344.4f, 568.9f),
                        PathNode.QuadTo(344.4f, 506.9f, 374.9f, 454.9f),
                        PathNode.QuadTo(405.4f, 402.9f, 457.4f, 372.4f),
                        PathNode.QuadTo(509.4f, 341.9f, 570.4f, 341.9f),
                        PathNode.QuadTo(597.4f, 341.9f, 621.4f, 347.9f),
                        PathNode.Close,
                        PathNode.MoveTo(373.4f, 943.9f),
                        PathNode.LineTo(442.4f, 875.9f),
                        PathNode.QuadTo(511.4f, 893.9f, 571.4f, 893.9f),
                        PathNode.QuadTo(670.4f, 893.9f, 765.9f, 854.4f),
                        PathNode.QuadTo(861.4f, 814.9f, 936.4f, 743.9f),
                        PathNode.QuadTo(1011.4f, 672.9f, 1052.4f, 582.9f),
                        PathNode.QuadTo(1055.4f, 575.9f, 1055.4f, 568.4f),
                        PathNode.QuadTo(1055.4f, 560.9f, 1052.4f, 552.9f),
                        PathNode.QuadTo(1031.4f, 507.9f, 1000.4f, 465.9f),
                        PathNode.QuadTo(969.4f, 423.9f, 930.4f, 387.9f),
                        PathNode.LineTo(990.4f, 326.9f),
                        PathNode.QuadTo(1034.4f, 367.9f, 1069.9f, 415.9f),
                        PathNode.QuadTo(1105.4f, 463.9f, 1130.4f, 517.9f),
                        PathNode.QuadTo(1141.4f, 541.9f, 1141.4f, 568.4f),
                        PathNode.QuadTo(1141.4f, 594.9f, 1130.4f, 618.9f),
                        PathNode.QuadTo(1083.4f, 722.9f, 995.9f, 804.9f),
                        PathNode.QuadTo(908.4f, 886.9f, 797.4f, 932.9f),
                        PathNode.QuadTo(686.4f, 978.9f, 571.4f, 978.9f),
                        PathNode.QuadTo(470.4f, 978.9f, 373.4f, 943.9f),
                        PathNode.Close,
                        PathNode.MoveTo(528.4f, 788.9f),
                        PathNode.LineTo(617.4f, 699.9f),
                        PathNode.QuadTo(631.4f, 695.9f, 644.4f, 687.9f),
                        PathNode.QuadTo(664.4f, 672.9f, 668.4f, 668.9f),
                        PathNode.QuadTo(681.4f, 656.9f, 690.9f, 642.4f),
                        PathNode.QuadTo(700.4f, 627.9f, 705.4f, 610.9f),
                        PathNode.LineTo(793.4f, 521.9f),
                        PathNode.QuadTo(798.4f, 546.9f, 798.4f, 566.9f),
                        PathNode.QuadTo(798.4f, 627.9f, 767.9f, 679.9f),
                        PathNode.QuadTo(737.4f, 731.9f, 685.4f, 762.4f),
                        PathNode.QuadTo(633.4f, 792.9f, 571.4f, 792.9f),
                        PathNode.QuadTo(547.4f, 792.9f, 528.4f, 788.9f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _hideRegular!!
    }

private var _hideRegular: ImageVector? = null
