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

internal val GlassIcons.Playlist: ImageVector
    get() {
        if (_playlistRegular != null) return _playlistRegular!!
        _playlistRegular = ImageVector.Builder(
            name = "Playlist.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(378.4f, 880.7f),
                        PathNode.LineTo(169.4f, 1000.7f),
                        PathNode.QuadTo(158.4f, 1006.7f, 146.9f, 1004.7f),
                        PathNode.QuadTo(135.4f, 1002.7f, 127.9f, 993.7f),
                        PathNode.QuadTo(120.4f, 984.7f, 120.4f, 971.7f),
                        PathNode.LineTo(120.4f, 730.7f),
                        PathNode.QuadTo(120.4f, 718.7f, 127.9f, 709.7f),
                        PathNode.QuadTo(135.4f, 700.7f, 146.9f, 698.7f),
                        PathNode.QuadTo(158.4f, 696.7f, 169.4f, 702.7f),
                        PathNode.LineTo(378.4f, 822.7f),
                        PathNode.QuadTo(389.4f, 828.7f, 392.9f, 840.2f),
                        PathNode.QuadTo(396.4f, 851.7f, 392.9f, 863.2f),
                        PathNode.QuadTo(389.4f, 874.7f, 378.4f, 880.7f),
                        PathNode.Close,
                        PathNode.MoveTo(1022.4f, 165.7f),
                        PathNode.LineTo(1022.4f, 195.7f),
                        PathNode.QuadTo(1022.4f, 210.7f, 1013.9f, 217.2f),
                        PathNode.QuadTo(1005.4f, 223.7f, 991.4f, 223.7f),
                        PathNode.LineTo(151.4f, 223.7f),
                        PathNode.QuadTo(138.4f, 223.7f, 129.4f, 216.7f),
                        PathNode.QuadTo(120.4f, 209.7f, 120.4f, 196.7f),
                        PathNode.LineTo(120.4f, 166.7f),
                        PathNode.QuadTo(120.4f, 151.7f, 128.9f, 144.7f),
                        PathNode.QuadTo(137.4f, 137.7f, 151.4f, 137.7f),
                        PathNode.LineTo(991.4f, 137.7f),
                        PathNode.QuadTo(1005.4f, 137.7f, 1013.9f, 144.2f),
                        PathNode.QuadTo(1022.4f, 150.7f, 1022.4f, 165.7f),
                        PathNode.Close,
                        PathNode.MoveTo(1022.4f, 505.7f),
                        PathNode.LineTo(1022.4f, 535.7f),
                        PathNode.QuadTo(1022.4f, 550.7f, 1013.9f, 557.2f),
                        PathNode.QuadTo(1005.4f, 563.7f, 991.4f, 563.7f),
                        PathNode.LineTo(151.4f, 563.7f),
                        PathNode.QuadTo(138.4f, 563.7f, 129.4f, 556.7f),
                        PathNode.QuadTo(120.4f, 549.7f, 120.4f, 536.7f),
                        PathNode.LineTo(120.4f, 506.7f),
                        PathNode.QuadTo(120.4f, 491.7f, 128.9f, 484.7f),
                        PathNode.QuadTo(137.4f, 477.7f, 151.4f, 477.7f),
                        PathNode.LineTo(991.4f, 477.7f),
                        PathNode.QuadTo(1005.4f, 477.7f, 1013.9f, 484.2f),
                        PathNode.QuadTo(1022.4f, 490.7f, 1022.4f, 505.7f),
                        PathNode.Close,
                        PathNode.MoveTo(1022.4f, 842.7f),
                        PathNode.LineTo(1022.4f, 872.7f),
                        PathNode.QuadTo(1022.4f, 887.7f, 1013.9f, 894.2f),
                        PathNode.QuadTo(1005.4f, 900.7f, 991.4f, 900.7f),
                        PathNode.LineTo(539.4f, 900.7f),
                        PathNode.QuadTo(526.4f, 900.7f, 517.4f, 893.7f),
                        PathNode.QuadTo(508.4f, 886.7f, 508.4f, 873.7f),
                        PathNode.LineTo(508.4f, 843.7f),
                        PathNode.QuadTo(508.4f, 828.7f, 516.9f, 821.7f),
                        PathNode.QuadTo(525.4f, 814.7f, 539.4f, 814.7f),
                        PathNode.LineTo(991.4f, 814.7f),
                        PathNode.QuadTo(1005.4f, 814.7f, 1013.9f, 821.2f),
                        PathNode.QuadTo(1022.4f, 827.7f, 1022.4f, 842.7f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _playlistRegular!!
    }

private var _playlistRegular: ImageVector? = null
