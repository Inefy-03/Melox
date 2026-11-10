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

internal val GlassIcons.Sort: ImageVector
    get() {
        if (_sortRegular != null) return _sortRegular!!
        _sortRegular = ImageVector.Builder(
            name = "Sort.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(282.2f, 975.7f),
                        PathNode.LineTo(252.2f, 975.7f),
                        PathNode.QuadTo(237.2f, 975.7f, 230.7f, 966.7f),
                        PathNode.QuadTo(224.2f, 957.7f, 224.2f, 944.7f),
                        PathNode.LineTo(224.2f, 311.7f),
                        PathNode.LineTo(156.2f, 378.7f),
                        PathNode.QuadTo(147.2f, 387.7f, 138.2f, 387.2f),
                        PathNode.QuadTo(129.2f, 386.7f, 122.2f, 379.7f),
                        PathNode.LineTo(96.2f, 353.7f),
                        PathNode.QuadTo(87.2f, 344.7f, 87.7f, 335.7f),
                        PathNode.QuadTo(88.2f, 326.7f, 95.2f, 319.7f),
                        PathNode.LineTo(234.2f, 180.7f),
                        PathNode.QuadTo(248.2f, 166.7f, 267.2f, 167.2f),
                        PathNode.QuadTo(286.2f, 167.7f, 301.2f, 182.7f),
                        PathNode.LineTo(437.2f, 318.7f),
                        PathNode.QuadTo(446.2f, 327.7f, 446.2f, 336.7f),
                        PathNode.QuadTo(446.2f, 345.7f, 436.2f, 354.7f),
                        PathNode.LineTo(412.2f, 378.7f),
                        PathNode.QuadTo(404.2f, 386.7f, 395.2f, 387.2f),
                        PathNode.QuadTo(386.2f, 387.7f, 378.2f, 379.7f),
                        PathNode.LineTo(309.2f, 311.7f),
                        PathNode.LineTo(309.2f, 944.7f),
                        PathNode.QuadTo(309.2f, 957.7f, 302.7f, 966.7f),
                        PathNode.QuadTo(296.2f, 975.7f, 282.2f, 975.7f),
                        PathNode.Close,
                        PathNode.MoveTo(1055.2f, 223.7f),
                        PathNode.LineTo(1055.2f, 253.7f),
                        PathNode.QuadTo(1055.2f, 268.7f, 1046.7f, 275.2f),
                        PathNode.QuadTo(1038.2f, 281.7f, 1024.2f, 281.7f),
                        PathNode.LineTo(519.2f, 281.7f),
                        PathNode.QuadTo(506.2f, 281.7f, 497.2f, 274.7f),
                        PathNode.QuadTo(488.2f, 267.7f, 488.2f, 254.7f),
                        PathNode.LineTo(488.2f, 224.7f),
                        PathNode.QuadTo(488.2f, 209.7f, 496.7f, 202.7f),
                        PathNode.QuadTo(505.2f, 195.7f, 519.2f, 195.7f),
                        PathNode.LineTo(1024.2f, 195.7f),
                        PathNode.QuadTo(1038.2f, 195.7f, 1046.7f, 202.2f),
                        PathNode.QuadTo(1055.2f, 208.7f, 1055.2f, 223.7f),
                        PathNode.Close,
                        PathNode.MoveTo(1055.2f, 561.7f),
                        PathNode.LineTo(1055.2f, 591.7f),
                        PathNode.QuadTo(1055.2f, 606.7f, 1046.7f, 613.2f),
                        PathNode.QuadTo(1038.2f, 619.7f, 1024.2f, 619.7f),
                        PathNode.LineTo(519.2f, 619.7f),
                        PathNode.QuadTo(506.2f, 619.7f, 497.2f, 612.7f),
                        PathNode.QuadTo(488.2f, 605.7f, 488.2f, 592.7f),
                        PathNode.LineTo(488.2f, 562.7f),
                        PathNode.QuadTo(488.2f, 547.7f, 496.7f, 540.7f),
                        PathNode.QuadTo(505.2f, 533.7f, 519.2f, 533.7f),
                        PathNode.LineTo(1024.2f, 533.7f),
                        PathNode.QuadTo(1038.2f, 533.7f, 1046.7f, 540.2f),
                        PathNode.QuadTo(1055.2f, 546.7f, 1055.2f, 561.7f),
                        PathNode.Close,
                        PathNode.MoveTo(1055.2f, 898.7f),
                        PathNode.LineTo(1055.2f, 928.7f),
                        PathNode.QuadTo(1055.2f, 943.7f, 1046.7f, 950.2f),
                        PathNode.QuadTo(1038.2f, 956.7f, 1024.2f, 956.7f),
                        PathNode.LineTo(519.2f, 956.7f),
                        PathNode.QuadTo(506.2f, 956.7f, 497.2f, 949.7f),
                        PathNode.QuadTo(488.2f, 942.7f, 488.2f, 929.7f),
                        PathNode.LineTo(488.2f, 899.7f),
                        PathNode.QuadTo(488.2f, 884.7f, 496.7f, 877.7f),
                        PathNode.QuadTo(505.2f, 870.7f, 519.2f, 870.7f),
                        PathNode.LineTo(1024.2f, 870.7f),
                        PathNode.QuadTo(1038.2f, 870.7f, 1046.7f, 877.2f),
                        PathNode.QuadTo(1055.2f, 883.7f, 1055.2f, 898.7f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _sortRegular!!
    }

private var _sortRegular: ImageVector? = null
