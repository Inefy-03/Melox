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

internal val GlassIcons.FolderFill: ImageVector
    get() {
        if (_folderFillRegular != null) return _folderFillRegular!!
        _folderFillRegular = ImageVector.Builder(
            name = "FolderFill.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(978.9f, 159.4f),
                        PathNode.QuadTo(1001.9f, 170.4f, 1020.9f, 189.4f),
                        PathNode.QuadTo(1039.9f, 208.4f, 1051.9f, 231.4f),
                        PathNode.QuadTo(1063.9f, 256.4f, 1066.9f, 290.9f),
                        PathNode.QuadTo(1069.9f, 325.4f, 1069.9f, 407.4f),
                        PathNode.LineTo(1069.9f, 667.4f),
                        PathNode.LineTo(72.9f, 667.4f),
                        PathNode.LineTo(72.9f, 407.4f),
                        PathNode.QuadTo(72.9f, 325.4f, 75.9f, 290.9f),
                        PathNode.QuadTo(78.9f, 256.4f, 90.9f, 231.4f),
                        PathNode.QuadTo(102.9f, 208.4f, 121.9f, 189.4f),
                        PathNode.QuadTo(140.9f, 170.4f, 163.9f, 159.4f),
                        PathNode.QuadTo(188.9f, 146.4f, 223.4f, 143.4f),
                        PathNode.QuadTo(257.9f, 140.4f, 339.9f, 140.4f),
                        PathNode.LineTo(803.9f, 140.4f),
                        PathNode.QuadTo(884.9f, 140.4f, 919.4f, 143.4f),
                        PathNode.QuadTo(953.9f, 146.4f, 978.9f, 159.4f),
                        PathNode.Close,
                        PathNode.MoveTo(1051.9f, 823.4f),
                        PathNode.QuadTo(1027.9f, 870.4f, 978.9f, 895.4f),
                        PathNode.QuadTo(953.9f, 907.4f, 919.4f, 910.4f),
                        PathNode.QuadTo(884.9f, 913.4f, 803.9f, 913.4f),
                        PathNode.LineTo(610.9f, 913.4f),
                        PathNode.QuadTo(583.9f, 913.4f, 574.9f, 915.4f),
                        PathNode.QuadTo(555.9f, 918.4f, 537.9f, 931.4f),
                        PathNode.QuadTo(530.9f, 936.4f, 511.9f, 955.4f),
                        PathNode.LineTo(501.9f, 965.4f),
                        PathNode.QuadTo(490.9f, 976.4f, 481.9f, 982.4f),
                        PathNode.QuadTo(459.9f, 997.4f, 438.9f, 1000.4f),
                        PathNode.QuadTo(428.9f, 1002.4f, 398.9f, 1002.4f),
                        PathNode.LineTo(339.9f, 1002.4f),
                        PathNode.QuadTo(257.9f, 1002.4f, 223.4f, 999.4f),
                        PathNode.QuadTo(188.9f, 996.4f, 163.9f, 984.4f),
                        PathNode.QuadTo(114.9f, 959.4f, 90.9f, 911.4f),
                        PathNode.QuadTo(79.9f, 889.4f, 76.4f, 857.4f),
                        PathNode.QuadTo(72.9f, 825.4f, 72.9f, 761.4f),
                        PathNode.LineTo(72.9f, 734.4f),
                        PathNode.LineTo(1068.9f, 734.4f),
                        PathNode.QuadTo(1066.9f, 765.4f, 1063.4f, 785.9f),
                        PathNode.QuadTo(1059.9f, 806.4f, 1051.9f, 823.4f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _folderFillRegular!!
    }

private var _folderFillRegular: ImageVector? = null
