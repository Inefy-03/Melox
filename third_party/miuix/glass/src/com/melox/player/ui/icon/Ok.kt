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

internal val GlassIcons.Ok: ImageVector
    get() {
        if (_okRegular != null) return _okRegular!!
        _okRegular = ImageVector.Builder(
            name = "Ok.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(1097.4f, 992.9f),
                        PathNode.LineTo(1069.4f, 1015.9f),
                        PathNode.QuadTo(1060.4f, 1022.9f, 1049.4f, 1021.4f),
                        PathNode.QuadTo(1038.4f, 1019.9f, 1031.4f, 1010.9f),
                        PathNode.LineTo(438.4f, 238.9f),
                        PathNode.QuadTo(436.4f, 235.9f, 433.4f, 235.9f),
                        PathNode.QuadTo(430.4f, 235.9f, 428.4f, 238.9f),
                        PathNode.LineTo(111.4f, 648.9f),
                        PathNode.QuadTo(104.4f, 656.9f, 93.4f, 658.4f),
                        PathNode.QuadTo(82.4f, 659.9f, 73.4f, 652.9f),
                        PathNode.LineTo(45.4f, 629.9f),
                        PathNode.QuadTo(36.4f, 622.9f, 34.9f, 611.9f),
                        PathNode.QuadTo(33.4f, 600.9f, 40.4f, 592.9f),
                        PathNode.LineTo(392.4f, 141.9f),
                        PathNode.QuadTo(402.4f, 128.9f, 416.9f, 123.9f),
                        PathNode.QuadTo(431.4f, 118.9f, 445.9f, 122.9f),
                        PathNode.QuadTo(460.4f, 126.9f, 469.4f, 138.9f),
                        PathNode.LineTo(1102.4f, 954.9f),
                        PathNode.QuadTo(1109.4f, 963.9f, 1107.9f, 974.9f),
                        PathNode.QuadTo(1106.4f, 985.9f, 1097.4f, 992.9f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _okRegular!!
    }

private var _okRegular: ImageVector? = null
