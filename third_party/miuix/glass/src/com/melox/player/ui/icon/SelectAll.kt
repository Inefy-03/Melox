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

internal val GlassIcons.SelectAll: ImageVector
    get() {
        if (_selectAllRegular != null) return _selectAllRegular!!
        _selectAllRegular = ImageVector.Builder(
            name = "SelectAll.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(347.0f, 1020.3f),
                        PathNode.LineTo(225.0f, 897.3f),
                        PathNode.QuadTo(220.0f, 892.3f, 215.5f, 891.8f),
                        PathNode.QuadTo(211.0f, 891.3f, 205.0f, 897.3f),
                        PathNode.LineTo(156.0f, 946.3f),
                        PathNode.QuadTo(148.0f, 954.3f, 137.5f, 955.8f),
                        PathNode.QuadTo(127.0f, 957.3f, 117.0f, 947.3f),
                        PathNode.LineTo(96.0f, 926.3f),
                        PathNode.QuadTo(85.0f, 915.3f, 86.0f, 905.3f),
                        PathNode.QuadTo(87.0f, 895.3f, 95.0f, 887.3f),
                        PathNode.LineTo(178.0f, 804.3f),
                        PathNode.QuadTo(197.0f, 785.3f, 215.5f, 785.8f),
                        PathNode.QuadTo(234.0f, 786.3f, 256.0f, 807.3f),
                        PathNode.LineTo(409.0f, 960.3f),
                        PathNode.QuadTo(419.0f, 970.3f, 419.5f, 982.3f),
                        PathNode.QuadTo(420.0f, 994.3f, 410.0f, 1003.3f),
                        PathNode.LineTo(390.0f, 1023.3f),
                        PathNode.QuadTo(379.0f, 1033.3f, 368.0f, 1031.8f),
                        PathNode.QuadTo(357.0f, 1030.3f, 347.0f, 1020.3f),
                        PathNode.Close,
                        PathNode.MoveTo(1057.0f, 540.3f),
                        PathNode.LineTo(1057.0f, 570.3f),
                        PathNode.QuadTo(1057.0f, 585.3f, 1048.5f, 591.8f),
                        PathNode.QuadTo(1040.0f, 598.3f, 1026.0f, 598.3f),
                        PathNode.LineTo(521.0f, 598.3f),
                        PathNode.QuadTo(508.0f, 598.3f, 499.0f, 591.3f),
                        PathNode.QuadTo(490.0f, 584.3f, 490.0f, 571.3f),
                        PathNode.LineTo(490.0f, 541.3f),
                        PathNode.QuadTo(490.0f, 526.3f, 498.5f, 519.3f),
                        PathNode.QuadTo(507.0f, 512.3f, 521.0f, 512.3f),
                        PathNode.LineTo(1026.0f, 512.3f),
                        PathNode.QuadTo(1040.0f, 512.3f, 1048.5f, 518.8f),
                        PathNode.QuadTo(1057.0f, 525.3f, 1057.0f, 540.3f),
                        PathNode.Close,
                        PathNode.MoveTo(1057.0f, 877.3f),
                        PathNode.LineTo(1057.0f, 907.3f),
                        PathNode.QuadTo(1057.0f, 922.3f, 1048.5f, 928.8f),
                        PathNode.QuadTo(1040.0f, 935.3f, 1026.0f, 935.3f),
                        PathNode.LineTo(521.0f, 935.3f),
                        PathNode.QuadTo(508.0f, 935.3f, 499.0f, 928.3f),
                        PathNode.QuadTo(490.0f, 921.3f, 490.0f, 908.3f),
                        PathNode.LineTo(490.0f, 878.3f),
                        PathNode.QuadTo(490.0f, 863.3f, 498.5f, 856.3f),
                        PathNode.QuadTo(507.0f, 849.3f, 521.0f, 849.3f),
                        PathNode.LineTo(1026.0f, 849.3f),
                        PathNode.QuadTo(1040.0f, 849.3f, 1048.5f, 855.8f),
                        PathNode.QuadTo(1057.0f, 862.3f, 1057.0f, 877.3f),
                        PathNode.Close,
                        PathNode.MoveTo(1057.0f, 202.3f),
                        PathNode.LineTo(1057.0f, 232.3f),
                        PathNode.QuadTo(1057.0f, 247.3f, 1048.5f, 253.8f),
                        PathNode.QuadTo(1040.0f, 260.3f, 1026.0f, 260.3f),
                        PathNode.LineTo(521.0f, 260.3f),
                        PathNode.QuadTo(508.0f, 260.3f, 499.0f, 253.3f),
                        PathNode.QuadTo(490.0f, 246.3f, 490.0f, 233.3f),
                        PathNode.LineTo(490.0f, 203.3f),
                        PathNode.QuadTo(490.0f, 188.3f, 498.5f, 181.3f),
                        PathNode.QuadTo(507.0f, 174.3f, 521.0f, 174.3f),
                        PathNode.LineTo(1026.0f, 174.3f),
                        PathNode.QuadTo(1040.0f, 174.3f, 1048.5f, 180.8f),
                        PathNode.QuadTo(1057.0f, 187.3f, 1057.0f, 202.3f),
                        PathNode.Close,
                        PathNode.MoveTo(347.0f, 684.3f),
                        PathNode.LineTo(225.0f, 561.3f),
                        PathNode.QuadTo(220.0f, 556.3f, 215.5f, 555.8f),
                        PathNode.QuadTo(211.0f, 555.3f, 205.0f, 561.3f),
                        PathNode.LineTo(156.0f, 610.3f),
                        PathNode.QuadTo(148.0f, 618.3f, 137.5f, 619.8f),
                        PathNode.QuadTo(127.0f, 621.3f, 117.0f, 611.3f),
                        PathNode.LineTo(96.0f, 590.3f),
                        PathNode.QuadTo(85.0f, 579.3f, 86.0f, 569.3f),
                        PathNode.QuadTo(87.0f, 559.3f, 95.0f, 551.3f),
                        PathNode.LineTo(178.0f, 468.3f),
                        PathNode.QuadTo(197.0f, 449.3f, 215.5f, 449.8f),
                        PathNode.QuadTo(234.0f, 450.3f, 256.0f, 471.3f),
                        PathNode.LineTo(409.0f, 624.3f),
                        PathNode.QuadTo(419.0f, 634.3f, 419.5f, 646.3f),
                        PathNode.QuadTo(420.0f, 658.3f, 410.0f, 667.3f),
                        PathNode.LineTo(390.0f, 687.3f),
                        PathNode.QuadTo(379.0f, 697.3f, 368.0f, 695.8f),
                        PathNode.QuadTo(357.0f, 694.3f, 347.0f, 684.3f),
                        PathNode.Close,
                        PathNode.MoveTo(347.0f, 345.3f),
                        PathNode.LineTo(225.0f, 222.3f),
                        PathNode.QuadTo(220.0f, 217.3f, 215.5f, 216.8f),
                        PathNode.QuadTo(211.0f, 216.3f, 205.0f, 222.3f),
                        PathNode.LineTo(156.0f, 271.3f),
                        PathNode.QuadTo(148.0f, 279.3f, 137.5f, 280.8f),
                        PathNode.QuadTo(127.0f, 282.3f, 117.0f, 272.3f),
                        PathNode.LineTo(96.0f, 251.3f),
                        PathNode.QuadTo(85.0f, 240.3f, 86.0f, 230.3f),
                        PathNode.QuadTo(87.0f, 220.3f, 95.0f, 212.3f),
                        PathNode.LineTo(178.0f, 129.3f),
                        PathNode.QuadTo(197.0f, 110.3f, 215.5f, 110.8f),
                        PathNode.QuadTo(234.0f, 111.3f, 256.0f, 132.3f),
                        PathNode.LineTo(409.0f, 285.3f),
                        PathNode.QuadTo(419.0f, 295.3f, 419.5f, 307.3f),
                        PathNode.QuadTo(420.0f, 319.3f, 410.0f, 328.3f),
                        PathNode.LineTo(390.0f, 348.3f),
                        PathNode.QuadTo(379.0f, 358.3f, 368.0f, 356.8f),
                        PathNode.QuadTo(357.0f, 355.3f, 347.0f, 345.3f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _selectAllRegular!!
    }

private var _selectAllRegular: ImageVector? = null
