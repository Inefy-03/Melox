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

internal val GlassIcons.Music: ImageVector
    get() {
        if (_musicRegular != null) return _musicRegular!!
        _musicRegular = ImageVector.Builder(
            name = "Music.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(405.4f, 284.4f),
                        PathNode.LineTo(480.4f, 817.4f),
                        PathNode.QuadTo(484.4f, 850.4f, 489.4f, 874.4f),
                        PathNode.QuadTo(493.4f, 891.4f, 501.4f, 901.4f),
                        PathNode.QuadTo(509.4f, 911.4f, 523.4f, 918.4f),
                        PathNode.QuadTo(532.4f, 923.4f, 554.4f, 923.9f),
                        PathNode.QuadTo(576.4f, 924.4f, 594.4f, 924.4f),
                        PathNode.LineTo(603.4f, 924.4f),
                        PathNode.LineTo(871.4f, 924.4f),
                        PathNode.LineTo(884.4f, 924.4f),
                        PathNode.QuadTo(903.4f, 924.4f, 923.9f, 923.9f),
                        PathNode.QuadTo(944.4f, 923.4f, 956.4f, 918.4f),
                        PathNode.QuadTo(980.4f, 908.4f, 982.4f, 871.4f),
                        PathNode.QuadTo(983.4f, 853.4f, 980.4f, 809.4f),
                        PathNode.LineTo(960.4f, 457.4f),
                        PathNode.QuadTo(959.4f, 451.4f, 954.9f, 448.9f),
                        PathNode.QuadTo(950.4f, 446.4f, 943.4f, 450.4f),
                        PathNode.QuadTo(924.4f, 459.4f, 906.9f, 462.9f),
                        PathNode.QuadTo(889.4f, 466.4f, 871.4f, 466.4f),
                        PathNode.QuadTo(826.4f, 466.4f, 788.4f, 443.9f),
                        PathNode.QuadTo(750.4f, 421.4f, 727.9f, 383.4f),
                        PathNode.QuadTo(705.4f, 345.4f, 705.4f, 300.4f),
                        PathNode.QuadTo(705.4f, 255.4f, 727.9f, 217.4f),
                        PathNode.QuadTo(750.4f, 179.4f, 788.4f, 156.9f),
                        PathNode.QuadTo(826.4f, 134.4f, 871.4f, 134.4f),
                        PathNode.QuadTo(913.4f, 134.4f, 950.4f, 154.4f),
                        PathNode.QuadTo(987.4f, 174.4f, 1010.9f, 208.9f),
                        PathNode.QuadTo(1034.4f, 243.4f, 1036.4f, 285.4f),
                        PathNode.LineTo(1065.4f, 805.4f),
                        PathNode.QuadTo(1068.4f, 846.4f, 1068.4f, 873.4f),
                        PathNode.QuadTo(1067.4f, 890.4f, 1064.9f, 903.9f),
                        PathNode.QuadTo(1062.4f, 917.4f, 1056.4f, 930.4f),
                        PathNode.QuadTo(1047.4f, 950.4f, 1031.9f, 966.4f),
                        PathNode.QuadTo(1016.4f, 982.4f, 995.4f, 993.4f),
                        PathNode.QuadTo(982.4f, 1001.4f, 968.9f, 1004.9f),
                        PathNode.QuadTo(955.4f, 1008.4f, 940.4f, 1008.4f),
                        PathNode.LineTo(541.4f, 1008.4f),
                        PathNode.QuadTo(526.4f, 1008.4f, 513.9f, 1005.4f),
                        PathNode.QuadTo(501.4f, 1002.4f, 487.4f, 996.4f),
                        PathNode.QuadTo(468.4f, 987.4f, 452.4f, 973.4f),
                        PathNode.QuadTo(436.4f, 959.4f, 424.4f, 941.4f),
                        PathNode.QuadTo(416.4f, 928.4f, 411.4f, 916.4f),
                        PathNode.QuadTo(406.4f, 904.4f, 405.4f, 892.4f),
                        PathNode.LineTo(342.4f, 449.4f),
                        PathNode.QuadTo(341.4f, 443.4f, 337.4f, 441.4f),
                        PathNode.QuadTo(333.4f, 439.4f, 327.4f, 442.4f),
                        PathNode.QuadTo(304.4f, 455.4f, 283.4f, 460.9f),
                        PathNode.QuadTo(262.4f, 466.4f, 240.4f, 466.4f),
                        PathNode.QuadTo(195.4f, 466.4f, 157.4f, 443.9f),
                        PathNode.QuadTo(119.4f, 421.4f, 96.9f, 383.4f),
                        PathNode.QuadTo(74.4f, 345.4f, 74.4f, 300.4f),
                        PathNode.QuadTo(74.4f, 255.4f, 96.9f, 217.4f),
                        PathNode.QuadTo(119.4f, 179.4f, 157.4f, 156.9f),
                        PathNode.QuadTo(195.4f, 134.4f, 240.4f, 134.4f),
                        PathNode.QuadTo(282.4f, 134.4f, 317.9f, 154.4f),
                        PathNode.QuadTo(353.4f, 174.4f, 376.4f, 208.4f),
                        PathNode.QuadTo(399.4f, 242.4f, 405.4f, 284.4f),
                        PathNode.Close,
                        PathNode.MoveTo(159.4f, 300.4f),
                        PathNode.QuadTo(159.4f, 333.4f, 183.4f, 357.4f),
                        PathNode.QuadTo(207.4f, 381.4f, 240.4f, 381.4f),
                        PathNode.QuadTo(273.4f, 381.4f, 297.4f, 357.4f),
                        PathNode.QuadTo(321.4f, 333.4f, 321.4f, 300.4f),
                        PathNode.QuadTo(321.4f, 281.4f, 311.4f, 262.9f),
                        PathNode.QuadTo(301.4f, 244.4f, 282.9f, 231.9f),
                        PathNode.QuadTo(264.4f, 219.4f, 240.4f, 219.4f),
                        PathNode.QuadTo(207.4f, 219.4f, 183.4f, 243.4f),
                        PathNode.QuadTo(159.4f, 267.4f, 159.4f, 300.4f),
                        PathNode.Close,
                        PathNode.MoveTo(790.4f, 300.4f),
                        PathNode.QuadTo(790.4f, 333.4f, 813.9f, 357.4f),
                        PathNode.QuadTo(837.4f, 381.4f, 871.4f, 381.4f),
                        PathNode.QuadTo(904.4f, 381.4f, 927.9f, 357.4f),
                        PathNode.QuadTo(951.4f, 333.4f, 951.4f, 300.4f),
                        PathNode.QuadTo(951.4f, 267.4f, 927.9f, 243.9f),
                        PathNode.QuadTo(904.4f, 220.4f, 871.4f, 220.4f),
                        PathNode.QuadTo(837.4f, 220.4f, 813.9f, 243.9f),
                        PathNode.QuadTo(790.4f, 267.4f, 790.4f, 300.4f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _musicRegular!!
    }

private var _musicRegular: ImageVector? = null
