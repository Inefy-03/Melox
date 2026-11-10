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

internal val GlassIcons.More: ImageVector
    get() {
        if (_moreRegular != null) return _moreRegular!!
        _moreRegular = ImageVector.Builder(
            name = "More.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(571.4f, 1005.9f),
                        PathNode.QuadTo(542.4f, 1005.9f, 521.4f, 984.9f),
                        PathNode.QuadTo(500.4f, 963.9f, 500.4f, 934.9f),
                        PathNode.QuadTo(500.4f, 905.9f, 521.4f, 884.9f),
                        PathNode.QuadTo(542.4f, 863.9f, 571.4f, 863.9f),
                        PathNode.QuadTo(600.4f, 863.9f, 621.4f, 884.9f),
                        PathNode.QuadTo(642.4f, 905.9f, 642.4f, 934.9f),
                        PathNode.QuadTo(642.4f, 963.9f, 621.4f, 984.9f),
                        PathNode.QuadTo(600.4f, 1005.9f, 571.4f, 1005.9f),
                        PathNode.Close,
                        PathNode.MoveTo(571.4f, 277.9f),
                        PathNode.QuadTo(542.4f, 277.9f, 521.4f, 257.4f),
                        PathNode.QuadTo(500.4f, 236.9f, 500.4f, 206.9f),
                        PathNode.QuadTo(500.4f, 177.9f, 521.4f, 157.4f),
                        PathNode.QuadTo(542.4f, 136.9f, 571.4f, 136.9f),
                        PathNode.QuadTo(600.4f, 136.9f, 621.4f, 157.4f),
                        PathNode.QuadTo(642.4f, 177.9f, 642.4f, 206.9f),
                        PathNode.QuadTo(642.4f, 236.9f, 621.4f, 257.4f),
                        PathNode.QuadTo(600.4f, 277.9f, 571.4f, 277.9f),
                        PathNode.Close,
                        PathNode.MoveTo(571.4f, 641.9f),
                        PathNode.QuadTo(542.4f, 641.9f, 521.4f, 620.9f),
                        PathNode.QuadTo(500.4f, 599.9f, 500.4f, 570.9f),
                        PathNode.QuadTo(500.4f, 541.9f, 521.4f, 520.9f),
                        PathNode.QuadTo(542.4f, 499.9f, 571.4f, 499.9f),
                        PathNode.QuadTo(600.4f, 499.9f, 621.4f, 520.9f),
                        PathNode.QuadTo(642.4f, 541.9f, 642.4f, 570.9f),
                        PathNode.QuadTo(642.4f, 599.9f, 621.4f, 620.9f),
                        PathNode.QuadTo(600.4f, 641.9f, 571.4f, 641.9f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _moreRegular!!
    }

private var _moreRegular: ImageVector? = null
