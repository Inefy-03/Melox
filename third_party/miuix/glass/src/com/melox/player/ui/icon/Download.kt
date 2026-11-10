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

internal val GlassIcons.Download: ImageVector
    get() {
        if (_downloadRegular != null) return _downloadRegular!!
        _downloadRegular = ImageVector.Builder(
            name = "Download.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(917.4f, 61.4f),
                        PathNode.QuadTo(944.4f, 74.4f, 965.9f, 95.9f),
                        PathNode.QuadTo(987.4f, 117.4f, 1000.4f, 144.4f),
                        PathNode.QuadTo(1014.4f, 172.4f, 1017.9f, 211.9f),
                        PathNode.QuadTo(1021.4f, 251.4f, 1021.4f, 344.4f),
                        PathNode.LineTo(1021.4f, 611.4f),
                        PathNode.QuadTo(1021.4f, 686.4f, 1019.4f, 718.9f),
                        PathNode.QuadTo(1017.4f, 751.4f, 1007.4f, 774.4f),
                        PathNode.QuadTo(993.4f, 810.4f, 965.9f, 837.9f),
                        PathNode.QuadTo(938.4f, 865.4f, 902.4f, 879.4f),
                        PathNode.QuadTo(880.4f, 888.4f, 849.9f, 890.9f),
                        PathNode.QuadTo(819.4f, 893.4f, 752.4f, 893.4f),
                        PathNode.LineTo(729.4f, 893.4f),
                        PathNode.QuadTo(716.4f, 893.4f, 707.9f, 886.9f),
                        PathNode.QuadTo(699.4f, 880.4f, 699.4f, 864.4f),
                        PathNode.LineTo(699.4f, 840.4f),
                        PathNode.QuadTo(699.4f, 821.4f, 708.4f, 814.4f),
                        PathNode.QuadTo(717.4f, 807.4f, 733.4f, 807.4f),
                        PathNode.LineTo(761.4f, 807.4f),
                        PathNode.QuadTo(814.4f, 807.4f, 836.9f, 805.9f),
                        PathNode.QuadTo(859.4f, 804.4f, 876.4f, 796.4f),
                        PathNode.QuadTo(908.4f, 779.4f, 923.4f, 748.4f),
                        PathNode.QuadTo(931.4f, 731.4f, 933.4f, 708.9f),
                        PathNode.QuadTo(935.4f, 686.4f, 935.4f, 633.4f),
                        PathNode.LineTo(935.4f, 301.4f),
                        PathNode.QuadTo(935.4f, 248.4f, 933.4f, 225.4f),
                        PathNode.QuadTo(931.4f, 202.4f, 923.4f, 186.4f),
                        PathNode.QuadTo(908.4f, 154.4f, 876.4f, 138.4f),
                        PathNode.QuadTo(859.4f, 130.4f, 836.9f, 128.4f),
                        PathNode.QuadTo(814.4f, 126.4f, 761.4f, 126.4f),
                        PathNode.LineTo(381.4f, 126.4f),
                        PathNode.QuadTo(328.4f, 126.4f, 305.4f, 128.4f),
                        PathNode.QuadTo(282.4f, 130.4f, 266.4f, 138.4f),
                        PathNode.QuadTo(233.4f, 153.4f, 218.4f, 186.4f),
                        PathNode.QuadTo(210.4f, 202.4f, 208.4f, 225.4f),
                        PathNode.QuadTo(206.4f, 248.4f, 206.4f, 301.4f),
                        PathNode.LineTo(206.4f, 633.4f),
                        PathNode.QuadTo(206.4f, 686.4f, 208.4f, 708.9f),
                        PathNode.QuadTo(210.4f, 731.4f, 218.4f, 748.4f),
                        PathNode.QuadTo(233.4f, 779.4f, 266.4f, 796.4f),
                        PathNode.QuadTo(282.4f, 804.4f, 305.4f, 805.9f),
                        PathNode.QuadTo(328.4f, 807.4f, 381.4f, 807.4f),
                        PathNode.LineTo(410.4f, 807.4f),
                        PathNode.QuadTo(426.4f, 807.4f, 434.4f, 814.9f),
                        PathNode.QuadTo(442.4f, 822.4f, 442.4f, 841.4f),
                        PathNode.LineTo(442.4f, 864.4f),
                        PathNode.QuadTo(442.4f, 893.4f, 413.4f, 893.4f),
                        PathNode.LineTo(388.4f, 893.4f),
                        PathNode.QuadTo(323.4f, 893.4f, 292.9f, 890.9f),
                        PathNode.QuadTo(262.4f, 888.4f, 240.4f, 880.4f),
                        PathNode.QuadTo(204.4f, 865.4f, 176.9f, 837.9f),
                        PathNode.QuadTo(149.4f, 810.4f, 134.4f, 774.4f),
                        PathNode.QuadTo(125.4f, 751.4f, 123.4f, 718.9f),
                        PathNode.QuadTo(121.4f, 686.4f, 121.4f, 611.4f),
                        PathNode.LineTo(121.4f, 344.4f),
                        PathNode.QuadTo(121.4f, 251.4f, 124.4f, 211.9f),
                        PathNode.QuadTo(127.4f, 172.4f, 141.4f, 144.4f),
                        PathNode.QuadTo(155.4f, 117.4f, 176.9f, 95.9f),
                        PathNode.QuadTo(198.4f, 74.4f, 224.4f, 61.4f),
                        PathNode.QuadTo(252.4f, 47.4f, 292.4f, 43.9f),
                        PathNode.QuadTo(332.4f, 40.4f, 425.4f, 40.4f),
                        PathNode.LineTo(717.4f, 40.4f),
                        PathNode.QuadTo(810.4f, 40.4f, 849.9f, 43.9f),
                        PathNode.QuadTo(889.4f, 47.4f, 917.4f, 61.4f),
                        PathNode.Close,
                        PathNode.MoveTo(610.4f, 325.4f),
                        PathNode.LineTo(811.4f, 525.4f),
                        PathNode.QuadTo(819.4f, 533.4f, 820.4f, 544.9f),
                        PathNode.QuadTo(821.4f, 556.4f, 811.4f, 566.4f),
                        PathNode.LineTo(790.4f, 586.4f),
                        PathNode.QuadTo(779.4f, 596.4f, 768.9f, 594.9f),
                        PathNode.QuadTo(758.4f, 593.4f, 750.4f, 585.4f),
                        PathNode.LineTo(614.4f, 449.4f),
                        PathNode.LineTo(614.4f, 1075.4f),
                        PathNode.QuadTo(614.4f, 1086.4f, 606.4f, 1094.4f),
                        PathNode.QuadTo(598.4f, 1102.4f, 587.4f, 1102.4f),
                        PathNode.LineTo(554.4f, 1102.4f),
                        PathNode.QuadTo(543.4f, 1102.4f, 535.9f, 1094.4f),
                        PathNode.QuadTo(528.4f, 1086.4f, 528.4f, 1075.4f),
                        PathNode.LineTo(528.4f, 449.4f),
                        PathNode.LineTo(389.4f, 586.4f),
                        PathNode.QuadTo(381.4f, 594.4f, 371.4f, 594.9f),
                        PathNode.QuadTo(361.4f, 595.4f, 352.4f, 586.4f),
                        PathNode.LineTo(330.4f, 564.4f),
                        PathNode.QuadTo(321.4f, 555.4f, 321.9f, 543.9f),
                        PathNode.QuadTo(322.4f, 532.4f, 336.4f, 518.4f),
                        PathNode.LineTo(531.4f, 325.4f),
                        PathNode.QuadTo(552.4f, 304.4f, 571.4f, 304.9f),
                        PathNode.QuadTo(590.4f, 305.4f, 610.4f, 325.4f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _downloadRegular!!
    }

private var _downloadRegular: ImageVector? = null
