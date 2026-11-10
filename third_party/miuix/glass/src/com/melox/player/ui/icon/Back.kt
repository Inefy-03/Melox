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

internal val GlassIcons.Back: ImageVector
    get() {
        if (_backRegular != null) return _backRegular!!
        _backRegular = ImageVector.Builder(
            name = "Back.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = true,
        ).apply {
            group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(161.7f, 528.2f),
                        PathNode.LineTo(1093.7f, 528.2f),
                        PathNode.QuadTo(1109.7f, 528.2f, 1117.7f, 536.2f),
                        PathNode.QuadTo(1125.7f, 544.2f, 1125.7f, 559.2f),
                        PathNode.LineTo(1125.7f, 586.2f),
                        PathNode.QuadTo(1125.7f, 599.2f, 1117.2f, 606.7f),
                        PathNode.QuadTo(1108.7f, 614.2f, 1093.7f, 614.2f),
                        PathNode.LineTo(161.7f, 614.2f),
                        PathNode.LineTo(445.7f, 898.2f),
                        PathNode.QuadTo(455.7f, 908.2f, 455.7f, 918.2f),
                        PathNode.QuadTo(455.7f, 928.2f, 443.7f, 940.2f),
                        PathNode.LineTo(426.7f, 957.2f),
                        PathNode.QuadTo(414.7f, 969.2f, 404.7f, 969.2f),
                        PathNode.QuadTo(394.7f, 969.2f, 382.7f, 957.2f),
                        PathNode.LineTo(35.7f, 609.2f),
                        PathNode.QuadTo(17.7f, 591.2f, 17.2f, 571.7f),
                        PathNode.QuadTo(16.7f, 552.2f, 36.7f, 532.2f),
                        PathNode.LineTo(382.7f, 186.2f),
                        PathNode.QuadTo(394.7f, 174.2f, 404.2f, 173.7f),
                        PathNode.QuadTo(413.7f, 173.2f, 426.7f, 186.2f),
                        PathNode.LineTo(445.7f, 205.2f),
                        PathNode.QuadTo(456.7f, 216.2f, 456.7f, 224.7f),
                        PathNode.QuadTo(456.7f, 233.2f, 444.7f, 245.2f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _backRegular!!
    }

private var _backRegular: ImageVector? = null
