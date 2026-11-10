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

internal val GlassIcons.Add: ImageVector
    get() {
        if (_addRegular != null) return _addRegular!!
        _addRegular = ImageVector.Builder(
            name = "Add.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(616.9f, 111.4f),
                        PathNode.LineTo(616.9f, 1031.4f),
                        PathNode.QuadTo(616.9f, 1043.4f, 609.9f, 1050.9f),
                        PathNode.QuadTo(602.9f, 1058.4f, 587.9f, 1058.4f),
                        PathNode.LineTo(555.9f, 1058.4f),
                        PathNode.QuadTo(542.9f, 1058.4f, 535.4f, 1050.4f),
                        PathNode.QuadTo(527.9f, 1042.4f, 527.9f, 1031.4f),
                        PathNode.LineTo(527.9f, 111.4f),
                        PathNode.QuadTo(527.9f, 98.4f, 535.9f, 91.4f),
                        PathNode.QuadTo(543.9f, 84.4f, 556.9f, 84.4f),
                        PathNode.LineTo(588.9f, 84.4f),
                        PathNode.QuadTo(601.9f, 84.4f, 609.4f, 91.4f),
                        PathNode.QuadTo(616.9f, 98.4f, 616.9f, 111.4f),
                        PathNode.Close,
                        PathNode.MoveTo(1030.9f, 616.4f),
                        PathNode.LineTo(111.9f, 616.4f),
                        PathNode.QuadTo(99.9f, 616.4f, 92.4f, 609.4f),
                        PathNode.QuadTo(84.9f, 602.4f, 84.9f, 587.4f),
                        PathNode.LineTo(84.9f, 553.4f),
                        PathNode.QuadTo(84.9f, 540.4f, 92.9f, 532.9f),
                        PathNode.QuadTo(100.9f, 525.4f, 111.9f, 525.4f),
                        PathNode.LineTo(1030.9f, 525.4f),
                        PathNode.QuadTo(1043.9f, 525.4f, 1050.9f, 533.4f),
                        PathNode.QuadTo(1057.9f, 541.4f, 1057.9f, 554.4f),
                        PathNode.LineTo(1057.9f, 588.4f),
                        PathNode.QuadTo(1057.9f, 601.4f, 1050.9f, 608.9f),
                        PathNode.QuadTo(1043.9f, 616.4f, 1030.9f, 616.4f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _addRegular!!
    }

private var _addRegular: ImageVector? = null
