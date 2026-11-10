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

internal val GlassIcons.Folder: ImageVector
    get() {
        if (_folderRegular != null) return _folderRegular!!
        _folderRegular = ImageVector.Builder(
            name = "Folder.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(979.9f, 145.4f),
                        PathNode.QuadTo(1033.9f, 173.4f, 1062.9f, 228.4f),
                        PathNode.QuadTo(1076.9f, 256.4f, 1079.9f, 295.9f),
                        PathNode.QuadTo(1082.9f, 335.4f, 1082.9f, 428.4f),
                        PathNode.LineTo(1082.9f, 613.4f),
                        PathNode.QuadTo(1082.9f, 707.4f, 1079.9f, 746.9f),
                        PathNode.QuadTo(1076.9f, 786.4f, 1062.9f, 814.4f),
                        PathNode.QuadTo(1033.9f, 868.4f, 979.9f, 897.4f),
                        PathNode.QuadTo(951.9f, 911.4f, 912.4f, 914.4f),
                        PathNode.QuadTo(872.9f, 917.4f, 779.9f, 917.4f),
                        PathNode.LineTo(583.9f, 917.4f),
                        PathNode.QuadTo(562.9f, 917.4f, 553.9f, 921.4f),
                        PathNode.QuadTo(544.9f, 925.4f, 528.9f, 941.4f),
                        PathNode.LineTo(505.9f, 965.4f),
                        PathNode.QuadTo(480.9f, 993.4f, 467.9f, 1001.4f),
                        PathNode.QuadTo(453.9f, 1010.4f, 436.9f, 1015.4f),
                        PathNode.QuadTo(424.9f, 1018.4f, 384.9f, 1018.4f),
                        PathNode.LineTo(363.9f, 1018.4f),
                        PathNode.QuadTo(270.9f, 1018.4f, 230.9f, 1014.9f),
                        PathNode.QuadTo(190.9f, 1011.4f, 162.9f, 997.4f),
                        PathNode.QuadTo(136.9f, 984.4f, 115.4f, 962.9f),
                        PathNode.QuadTo(93.9f, 941.4f, 79.9f, 914.4f),
                        PathNode.QuadTo(65.9f, 887.4f, 62.9f, 847.4f),
                        PathNode.QuadTo(59.9f, 807.4f, 59.9f, 714.4f),
                        PathNode.LineTo(59.9f, 428.4f),
                        PathNode.QuadTo(59.9f, 335.4f, 62.9f, 295.9f),
                        PathNode.QuadTo(65.9f, 256.4f, 79.9f, 228.4f),
                        PathNode.QuadTo(108.9f, 173.4f, 162.9f, 145.4f),
                        PathNode.QuadTo(190.9f, 131.4f, 230.9f, 127.9f),
                        PathNode.QuadTo(270.9f, 124.4f, 363.9f, 124.4f),
                        PathNode.LineTo(779.9f, 124.4f),
                        PathNode.QuadTo(872.9f, 124.4f, 912.4f, 127.9f),
                        PathNode.QuadTo(951.9f, 131.4f, 979.9f, 145.4f),
                        PathNode.Close,
                        PathNode.MoveTo(204.9f, 222.4f),
                        PathNode.QuadTo(174.9f, 236.4f, 156.9f, 269.4f),
                        PathNode.QuadTo(148.9f, 286.4f, 146.9f, 308.9f),
                        PathNode.QuadTo(144.9f, 331.4f, 144.9f, 384.4f),
                        PathNode.LineTo(144.9f, 634.4f),
                        PathNode.LineTo(997.9f, 634.4f),
                        PathNode.LineTo(997.9f, 384.4f),
                        PathNode.QuadTo(997.9f, 331.4f, 995.9f, 308.9f),
                        PathNode.QuadTo(993.9f, 286.4f, 985.9f, 269.4f),
                        PathNode.QuadTo(969.9f, 237.4f, 937.9f, 222.4f),
                        PathNode.QuadTo(921.9f, 214.4f, 899.4f, 212.4f),
                        PathNode.QuadTo(876.9f, 210.4f, 822.9f, 210.4f),
                        PathNode.LineTo(319.9f, 210.4f),
                        PathNode.QuadTo(266.9f, 210.4f, 243.9f, 212.4f),
                        PathNode.QuadTo(220.9f, 214.4f, 204.9f, 222.4f),
                        PathNode.Close,
                        PathNode.MoveTo(156.9f, 873.4f),
                        PathNode.QuadTo(165.9f, 889.4f, 177.9f, 901.4f),
                        PathNode.QuadTo(189.9f, 913.4f, 204.9f, 920.4f),
                        PathNode.QuadTo(220.9f, 928.4f, 243.9f, 930.4f),
                        PathNode.QuadTo(266.9f, 932.4f, 319.9f, 932.4f),
                        PathNode.LineTo(381.9f, 932.4f),
                        PathNode.QuadTo(397.9f, 932.4f, 408.4f, 928.4f),
                        PathNode.QuadTo(418.9f, 924.4f, 427.9f, 914.4f),
                        PathNode.LineTo(455.9f, 883.4f),
                        PathNode.LineTo(460.9f, 877.4f),
                        PathNode.QuadTo(470.9f, 867.4f, 478.4f, 859.9f),
                        PathNode.QuadTo(485.9f, 852.4f, 492.9f, 848.4f),
                        PathNode.QuadTo(504.9f, 840.4f, 523.9f, 835.4f),
                        PathNode.QuadTo(535.9f, 832.4f, 575.9f, 832.4f),
                        PathNode.LineTo(822.9f, 832.4f),
                        PathNode.QuadTo(876.9f, 832.4f, 899.4f, 830.4f),
                        PathNode.QuadTo(921.9f, 828.4f, 937.9f, 820.4f),
                        PathNode.QuadTo(970.9f, 802.4f, 985.9f, 772.4f),
                        PathNode.QuadTo(990.9f, 762.4f, 993.4f, 749.9f),
                        PathNode.QuadTo(995.9f, 737.4f, 996.9f, 719.4f),
                        PathNode.LineTo(995.9f, 719.4f),
                        PathNode.LineTo(144.9f, 719.4f),
                        PathNode.LineTo(144.9f, 758.4f),
                        PathNode.QuadTo(144.9f, 811.4f, 146.9f, 833.9f),
                        PathNode.QuadTo(148.9f, 856.4f, 156.9f, 873.4f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _folderRegular!!
    }

private var _folderRegular: ImageVector? = null
