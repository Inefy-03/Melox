// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.melox.player.ui.component.os4

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.icon.MiuixIcons

internal val glassSearchIcon: ImageVector
    get() {
        if (_searchRegular != null) return _searchRegular!!
        _searchRegular = ImageVector.Builder(
            name = "Search.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(695.9f, 335.2f),
                        PathNode.QuadTo(715.9f, 347.2f, 723.9f, 346.7f),
                        PathNode.QuadTo(731.9f, 346.2f, 745.9f, 332.2f),
                        PathNode.LineTo(996.9f, 81.2f),
                        PathNode.QuadTo(1007.9f, 70.2f, 1018.4f, 69.7f),
                        PathNode.QuadTo(1028.9f, 69.2f, 1040.9f, 81.2f),
                        PathNode.LineTo(1057.9f, 98.2f),
                        PathNode.QuadTo(1069.9f, 110.2f, 1069.9f, 119.7f),
                        PathNode.QuadTo(1069.9f, 129.2f, 1057.9f, 141.2f),
                        PathNode.LineTo(803.9f, 396.2f),
                        PathNode.QuadTo(791.9f, 408.2f, 792.4f, 415.7f),
                        PathNode.QuadTo(792.9f, 423.2f, 805.9f, 442.2f),
                        PathNode.QuadTo(839.9f, 491.2f, 858.4f, 549.7f),
                        PathNode.QuadTo(876.9f, 608.2f, 876.9f, 671.2f),
                        PathNode.QuadTo(876.9f, 780.2f, 822.9f, 872.7f),
                        PathNode.QuadTo(768.9f, 965.2f, 676.4f, 1019.2f),
                        PathNode.QuadTo(583.9f, 1073.2f, 474.9f, 1073.2f),
                        PathNode.QuadTo(365.9f, 1073.2f, 273.4f, 1019.2f),
                        PathNode.QuadTo(180.9f, 965.2f, 126.9f, 872.7f),
                        PathNode.QuadTo(72.9f, 780.2f, 72.9f, 671.2f),
                        PathNode.QuadTo(72.9f, 562.2f, 126.9f, 469.7f),
                        PathNode.QuadTo(180.9f, 377.2f, 273.4f, 323.2f),
                        PathNode.QuadTo(365.9f, 269.2f, 474.9f, 269.2f),
                        PathNode.QuadTo(534.9f, 269.2f, 591.4f, 286.2f),
                        PathNode.QuadTo(647.9f, 303.2f, 695.9f, 335.2f),
                        PathNode.Close,
                        PathNode.MoveTo(157.9f, 671.2f),
                        PathNode.QuadTo(157.9f, 757.2f, 200.4f, 830.2f),
                        PathNode.QuadTo(242.9f, 903.2f, 315.9f, 945.7f),
                        PathNode.QuadTo(388.9f, 988.2f, 474.9f, 988.2f),
                        PathNode.QuadTo(560.9f, 988.2f, 633.4f, 945.7f),
                        PathNode.QuadTo(705.9f, 903.2f, 748.4f, 830.2f),
                        PathNode.QuadTo(790.9f, 757.2f, 790.9f, 671.2f),
                        PathNode.QuadTo(790.9f, 585.2f, 748.4f, 512.2f),
                        PathNode.QuadTo(705.9f, 439.2f, 633.4f, 396.7f),
                        PathNode.QuadTo(560.9f, 354.2f, 474.9f, 354.2f),
                        PathNode.QuadTo(388.9f, 354.2f, 315.9f, 396.7f),
                        PathNode.QuadTo(242.9f, 439.2f, 200.4f, 512.2f),
                        PathNode.QuadTo(157.9f, 585.2f, 157.9f, 671.2f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _searchRegular!!
    }

private var _searchRegular: ImageVector? = null

internal val glassCloseIcon: ImageVector
    get() {
        if (_closeRegular != null) return _closeRegular!!
        _closeRegular = ImageVector.Builder(
            name = "Close.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(183.4f, 981.7f),
                        PathNode.LineTo(161.4f, 959.7f),
                        PathNode.QuadTo(151.4f, 949.7f, 151.4f, 939.2f),
                        PathNode.QuadTo(151.4f, 928.7f, 161.4f, 917.7f),
                        PathNode.LineTo(507.4f, 570.7f),
                        PathNode.LineTo(160.4f, 225.7f),
                        PathNode.QuadTo(150.4f, 215.7f, 150.4f, 205.2f),
                        PathNode.QuadTo(150.4f, 194.7f, 160.4f, 183.7f),
                        PathNode.LineTo(182.4f, 161.7f),
                        PathNode.QuadTo(192.4f, 151.7f, 203.4f, 151.7f),
                        PathNode.QuadTo(214.4f, 151.7f, 224.4f, 161.7f),
                        PathNode.LineTo(571.4f, 506.7f),
                        PathNode.LineTo(917.4f, 160.7f),
                        PathNode.QuadTo(938.4f, 139.7f, 959.4f, 160.7f),
                        PathNode.LineTo(981.4f, 182.7f),
                        PathNode.QuadTo(992.4f, 192.7f, 992.4f, 203.7f),
                        PathNode.QuadTo(992.4f, 214.7f, 981.4f, 224.7f),
                        PathNode.LineTo(635.4f, 570.7f),
                        PathNode.LineTo(980.4f, 917.7f),
                        PathNode.QuadTo(990.4f, 928.7f, 990.4f, 939.2f),
                        PathNode.QuadTo(990.4f, 949.7f, 980.4f, 959.7f),
                        PathNode.LineTo(958.4f, 981.7f),
                        PathNode.QuadTo(948.4f, 992.7f, 937.4f, 992.7f),
                        PathNode.QuadTo(926.4f, 992.7f, 916.4f, 981.7f),
                        PathNode.LineTo(571.4f, 634.7f),
                        PathNode.LineTo(225.4f, 981.7f),
                        PathNode.QuadTo(215.4f, 992.7f, 204.9f, 992.7f),
                        PathNode.QuadTo(194.4f, 992.7f, 183.4f, 981.7f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _closeRegular!!
    }

private var _closeRegular: ImageVector? = null
