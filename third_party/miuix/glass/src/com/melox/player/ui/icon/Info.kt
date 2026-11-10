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

internal val GlassIcons.Info: ImageVector
    get() {
        if (_infoRegular != null) return _infoRegular!!
        _infoRegular = ImageVector.Builder(
            name = "Info.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(1081.9f, 570.9f),
                        PathNode.QuadTo(1081.9f, 709.9f, 1013.4f, 826.9f),
                        PathNode.QuadTo(944.9f, 943.9f, 827.9f, 1012.9f),
                        PathNode.QuadTo(710.9f, 1081.9f, 571.9f, 1081.9f),
                        PathNode.QuadTo(432.9f, 1081.9f, 315.9f, 1012.9f),
                        PathNode.QuadTo(198.9f, 943.9f, 129.9f, 826.9f),
                        PathNode.QuadTo(60.9f, 709.9f, 60.9f, 570.9f),
                        PathNode.QuadTo(60.9f, 431.9f, 129.9f, 314.9f),
                        PathNode.QuadTo(198.9f, 197.9f, 315.9f, 129.4f),
                        PathNode.QuadTo(432.9f, 60.9f, 571.9f, 60.9f),
                        PathNode.QuadTo(710.9f, 60.9f, 827.9f, 129.4f),
                        PathNode.QuadTo(944.9f, 197.9f, 1013.4f, 314.9f),
                        PathNode.QuadTo(1081.9f, 431.9f, 1081.9f, 570.9f),
                        PathNode.Close,
                        PathNode.MoveTo(146.9f, 570.9f),
                        PathNode.QuadTo(146.9f, 686.9f, 203.9f, 784.4f),
                        PathNode.QuadTo(260.9f, 881.9f, 358.4f, 938.9f),
                        PathNode.QuadTo(455.9f, 995.9f, 571.9f, 995.9f),
                        PathNode.QuadTo(686.9f, 995.9f, 784.4f, 938.9f),
                        PathNode.QuadTo(881.9f, 881.9f, 939.4f, 784.4f),
                        PathNode.QuadTo(996.9f, 686.9f, 996.9f, 570.9f),
                        PathNode.QuadTo(996.9f, 454.9f, 939.4f, 357.4f),
                        PathNode.QuadTo(881.9f, 259.9f, 784.4f, 202.9f),
                        PathNode.QuadTo(686.9f, 145.9f, 571.9f, 145.9f),
                        PathNode.QuadTo(455.9f, 145.9f, 358.4f, 202.9f),
                        PathNode.QuadTo(260.9f, 259.9f, 203.9f, 357.4f),
                        PathNode.QuadTo(146.9f, 454.9f, 146.9f, 570.9f),
                        PathNode.Close,
                        PathNode.MoveTo(625.9f, 806.9f),
                        PathNode.QuadTo(625.9f, 829.9f, 610.4f, 845.4f),
                        PathNode.QuadTo(594.9f, 860.9f, 571.9f, 860.9f),
                        PathNode.QuadTo(548.9f, 860.9f, 532.9f, 844.9f),
                        PathNode.QuadTo(516.9f, 828.9f, 516.9f, 806.9f),
                        PathNode.QuadTo(516.9f, 784.9f, 533.4f, 768.4f),
                        PathNode.QuadTo(549.9f, 751.9f, 571.9f, 751.9f),
                        PathNode.QuadTo(593.9f, 751.9f, 609.9f, 767.9f),
                        PathNode.QuadTo(625.9f, 783.9f, 625.9f, 806.9f),
                        PathNode.Close,
                        PathNode.MoveTo(613.9f, 308.9f),
                        PathNode.LineTo(613.9f, 667.9f),
                        PathNode.QuadTo(613.9f, 679.9f, 606.9f, 687.4f),
                        PathNode.QuadTo(599.9f, 694.9f, 584.9f, 694.9f),
                        PathNode.LineTo(556.9f, 694.9f),
                        PathNode.QuadTo(543.9f, 694.9f, 536.4f, 686.9f),
                        PathNode.QuadTo(528.9f, 678.9f, 528.9f, 667.9f),
                        PathNode.LineTo(528.9f, 308.9f),
                        PathNode.QuadTo(528.9f, 295.9f, 536.9f, 288.9f),
                        PathNode.QuadTo(544.9f, 281.9f, 557.9f, 281.9f),
                        PathNode.LineTo(585.9f, 281.9f),
                        PathNode.QuadTo(598.9f, 281.9f, 606.4f, 288.9f),
                        PathNode.QuadTo(613.9f, 295.9f, 613.9f, 308.9f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _infoRegular!!
    }

private var _infoRegular: ImageVector? = null
