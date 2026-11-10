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

internal val GlassIcons.ChevronForward: ImageVector
    get() {
        if (_chevronForwardRegular != null) return _chevronForwardRegular!!
        _chevronForwardRegular = ImageVector.Builder(
            name = "ChevronForward.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = true,
        ).apply {
            group(scaleY = -1.0f, translationX = 85.7f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(330.2f, 1067.4f),
                        PathNode.LineTo(301.2f, 1039.4f),
                        PathNode.QuadTo(294.2f, 1031.4f, 293.7f, 1020.4f),
                        PathNode.QuadTo(293.2f, 1009.4f, 301.2f, 1001.4f),
                        PathNode.LineTo(732.2f, 570.4f),
                        PathNode.LineTo(301.2f, 138.4f),
                        PathNode.QuadTo(293.2f, 130.4f, 294.2f, 120.4f),
                        PathNode.QuadTo(295.2f, 110.4f, 304.2f, 101.4f),
                        PathNode.LineTo(322.2f, 83.4f),
                        PathNode.QuadTo(336.2f, 69.4f, 346.7f, 68.9f),
                        PathNode.QuadTo(357.2f, 68.4f, 367.2f, 78.4f),
                        PathNode.LineTo(841.2f, 551.4f),
                        PathNode.QuadTo(849.2f, 559.4f, 849.2f, 570.4f),
                        PathNode.QuadTo(849.2f, 581.4f, 842.2f, 588.4f),
                        PathNode.LineTo(365.2f, 1065.4f),
                        PathNode.QuadTo(357.2f, 1073.4f, 347.2f, 1073.9f),
                        PathNode.QuadTo(337.2f, 1074.4f, 330.2f, 1067.4f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _chevronForwardRegular!!
    }

private var _chevronForwardRegular: ImageVector? = null
