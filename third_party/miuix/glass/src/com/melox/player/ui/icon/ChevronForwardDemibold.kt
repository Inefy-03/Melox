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

internal val GlassIcons.ChevronForwardDemibold: ImageVector
    get() {
        if (_chevronForwardDemibold != null) return _chevronForwardDemibold!!
        _chevronForwardDemibold = ImageVector.Builder(
            name = "ChevronForward.Glass.Demibold",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = true,
        ).apply {
            group(scaleY = -1.0f, translationX = 85.7f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(320.5f, 1077.6f),
                        PathNode.LineTo(291.5f, 1049.2f),
                        PathNode.QuadTo(280.4f, 1037.0f, 279.9f, 1020.3f),
                        PathNode.QuadTo(279.4f, 1003.6f, 291.5f, 991.9f),
                        PathNode.LineTo(712.9f, 570.5f),
                        PathNode.LineTo(291.5f, 148.2f),
                        PathNode.QuadTo(279.4f, 136.0f, 280.4f, 120.5f),
                        PathNode.QuadTo(281.4f, 105.0f, 294.5f, 91.9f),
                        PathNode.LineTo(312.5f, 73.9f),
                        PathNode.QuadTo(330.7f, 55.7f, 346.7f, 55.2f),
                        PathNode.QuadTo(362.7f, 54.7f, 376.8f, 68.9f),
                        PathNode.LineTo(850.8f, 541.9f),
                        PathNode.QuadTo(863.0f, 553.6f, 863.0f, 570.3f),
                        PathNode.QuadTo(863.0f, 587.0f, 851.8f, 598.2f),
                        PathNode.LineTo(374.8f, 1075.2f),
                        PathNode.QuadTo(363.2f, 1086.9f, 347.6f, 1087.6f),
                        PathNode.QuadTo(332.1f, 1088.3f, 320.5f, 1077.6f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _chevronForwardDemibold!!
    }

private var _chevronForwardDemibold: ImageVector? = null
