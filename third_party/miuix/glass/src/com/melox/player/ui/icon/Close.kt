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

internal val GlassIcons.Close: ImageVector
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
