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

internal val GlassIcons.Edit: ImageVector
    get() {
        if (_editRegular != null) return _editRegular!!
        _editRegular = ImageVector.Builder(
            name = "Edit.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(896.1f, 110.9f),
                        PathNode.QuadTo(951.1f, 138.9f, 979.1f, 193.9f),
                        PathNode.QuadTo(993.1f, 221.9f, 996.6f, 261.4f),
                        PathNode.QuadTo(1000.1f, 300.9f, 1000.1f, 393.9f),
                        PathNode.LineTo(1000.1f, 728.9f),
                        PathNode.QuadTo(1000.1f, 742.9f, 992.6f, 749.4f),
                        PathNode.QuadTo(985.1f, 755.9f, 975.6f, 755.4f),
                        PathNode.QuadTo(966.1f, 754.9f, 960.1f, 748.9f),
                        PathNode.LineTo(933.1f, 720.9f),
                        PathNode.QuadTo(922.1f, 709.9f, 918.1f, 696.9f),
                        PathNode.QuadTo(914.1f, 683.9f, 914.1f, 664.9f),
                        PathNode.LineTo(914.1f, 350.9f),
                        PathNode.QuadTo(914.1f, 296.9f, 912.1f, 274.4f),
                        PathNode.QuadTo(910.1f, 251.9f, 902.1f, 235.9f),
                        PathNode.QuadTo(887.1f, 203.9f, 855.1f, 187.9f),
                        PathNode.QuadTo(838.1f, 179.9f, 815.6f, 177.9f),
                        PathNode.QuadTo(793.1f, 175.9f, 740.1f, 175.9f),
                        PathNode.LineTo(354.1f, 175.9f),
                        PathNode.QuadTo(301.1f, 175.9f, 278.1f, 177.9f),
                        PathNode.QuadTo(255.1f, 179.9f, 239.1f, 187.9f),
                        PathNode.QuadTo(209.1f, 202.9f, 191.1f, 235.9f),
                        PathNode.QuadTo(183.1f, 251.9f, 181.1f, 274.4f),
                        PathNode.QuadTo(179.1f, 296.9f, 179.1f, 350.9f),
                        PathNode.LineTo(179.1f, 752.9f),
                        PathNode.QuadTo(179.1f, 806.9f, 181.1f, 829.4f),
                        PathNode.QuadTo(183.1f, 851.9f, 191.1f, 868.9f),
                        PathNode.QuadTo(209.1f, 901.9f, 239.1f, 915.9f),
                        PathNode.QuadTo(255.1f, 923.9f, 278.1f, 925.9f),
                        PathNode.QuadTo(301.1f, 927.9f, 354.1f, 927.9f),
                        PathNode.LineTo(681.1f, 927.9f),
                        PathNode.QuadTo(720.1f, 927.9f, 736.1f, 942.9f),
                        PathNode.LineTo(766.1f, 970.9f),
                        PathNode.QuadTo(776.1f, 980.9f, 776.1f, 990.9f),
                        PathNode.QuadTo(776.1f, 1000.9f, 768.1f, 1007.4f),
                        PathNode.QuadTo(760.1f, 1013.9f, 749.1f, 1013.9f),
                        PathNode.LineTo(398.1f, 1013.9f),
                        PathNode.QuadTo(305.1f, 1013.9f, 265.1f, 1010.4f),
                        PathNode.QuadTo(225.1f, 1006.9f, 197.1f, 992.9f),
                        PathNode.QuadTo(143.1f, 964.9f, 114.1f, 909.9f),
                        PathNode.QuadTo(100.1f, 881.9f, 97.1f, 842.4f),
                        PathNode.QuadTo(94.1f, 802.9f, 94.1f, 709.9f),
                        PathNode.LineTo(94.1f, 393.9f),
                        PathNode.QuadTo(94.1f, 300.9f, 97.1f, 261.4f),
                        PathNode.QuadTo(100.1f, 221.9f, 114.1f, 193.9f),
                        PathNode.QuadTo(143.1f, 138.9f, 197.1f, 110.9f),
                        PathNode.QuadTo(225.1f, 96.9f, 265.1f, 93.4f),
                        PathNode.QuadTo(305.1f, 89.9f, 398.1f, 89.9f),
                        PathNode.LineTo(696.1f, 89.9f),
                        PathNode.QuadTo(789.1f, 89.9f, 828.6f, 93.4f),
                        PathNode.QuadTo(868.1f, 96.9f, 896.1f, 110.9f),
                        PathNode.Close,
                        PathNode.MoveTo(569.1f, 485.9f),
                        PathNode.LineTo(1037.1f, 954.9f),
                        PathNode.QuadTo(1050.1f, 967.9f, 1048.6f, 987.9f),
                        PathNode.QuadTo(1047.1f, 1007.9f, 1037.1f, 1017.9f),
                        PathNode.LineTo(1014.1f, 1040.9f),
                        PathNode.QuadTo(1003.1f, 1051.9f, 982.6f, 1052.9f),
                        PathNode.QuadTo(962.1f, 1053.9f, 947.1f, 1038.9f),
                        PathNode.LineTo(482.1f, 572.9f),
                        PathNode.QuadTo(448.1f, 539.9f, 423.1f, 496.9f),
                        PathNode.LineTo(369.1f, 395.9f),
                        PathNode.QuadTo(366.1f, 389.9f, 369.1f, 383.4f),
                        PathNode.QuadTo(372.1f, 376.9f, 378.6f, 373.9f),
                        PathNode.QuadTo(385.1f, 370.9f, 391.1f, 373.9f),
                        PathNode.LineTo(492.1f, 427.9f),
                        PathNode.QuadTo(533.1f, 450.9f, 569.1f, 485.9f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _editRegular!!
    }

private var _editRegular: ImageVector? = null
