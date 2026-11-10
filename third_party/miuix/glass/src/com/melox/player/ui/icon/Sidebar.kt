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

internal val GlassIcons.Sidebar: ImageVector
    get() {
        if (_sidebarRegular != null) return _sidebarRegular!!
        _sidebarRegular = ImageVector.Builder(
            name = "Sidebar.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(977.4f, 141.9f),
                        PathNode.QuadTo(1032.4f, 169.9f, 1060.4f, 224.9f),
                        PathNode.QuadTo(1074.4f, 252.9f, 1077.9f, 292.4f),
                        PathNode.QuadTo(1081.4f, 331.9f, 1081.4f, 424.9f),
                        PathNode.LineTo(1081.4f, 717.9f),
                        PathNode.QuadTo(1081.4f, 810.9f, 1077.9f, 850.4f),
                        PathNode.QuadTo(1074.4f, 889.9f, 1060.4f, 917.9f),
                        PathNode.QuadTo(1032.4f, 972.9f, 977.4f, 1000.9f),
                        PathNode.QuadTo(949.4f, 1014.9f, 909.9f, 1018.4f),
                        PathNode.QuadTo(870.4f, 1021.9f, 777.4f, 1021.9f),
                        PathNode.LineTo(365.4f, 1021.9f),
                        PathNode.QuadTo(272.4f, 1021.9f, 232.4f, 1018.4f),
                        PathNode.QuadTo(192.4f, 1014.9f, 164.4f, 1000.9f),
                        PathNode.QuadTo(110.4f, 972.9f, 81.4f, 917.9f),
                        PathNode.QuadTo(67.4f, 889.9f, 64.4f, 850.4f),
                        PathNode.QuadTo(61.4f, 810.9f, 61.4f, 717.9f),
                        PathNode.LineTo(61.4f, 424.9f),
                        PathNode.QuadTo(61.4f, 331.9f, 64.4f, 292.4f),
                        PathNode.QuadTo(67.4f, 252.9f, 81.4f, 224.9f),
                        PathNode.QuadTo(110.4f, 169.9f, 164.4f, 141.9f),
                        PathNode.QuadTo(192.4f, 127.9f, 232.4f, 124.4f),
                        PathNode.QuadTo(272.4f, 120.9f, 365.4f, 120.9f),
                        PathNode.LineTo(777.4f, 120.9f),
                        PathNode.QuadTo(870.4f, 120.9f, 909.9f, 124.4f),
                        PathNode.QuadTo(949.4f, 127.9f, 977.4f, 141.9f),
                        PathNode.Close,
                        PathNode.MoveTo(206.4f, 217.9f),
                        PathNode.QuadTo(175.4f, 234.9f, 158.4f, 265.9f),
                        PathNode.QuadTo(150.4f, 281.9f, 148.4f, 304.9f),
                        PathNode.QuadTo(146.4f, 327.9f, 146.4f, 380.9f),
                        PathNode.LineTo(146.4f, 760.9f),
                        PathNode.QuadTo(146.4f, 814.9f, 148.4f, 837.4f),
                        PathNode.QuadTo(150.4f, 859.9f, 158.4f, 875.9f),
                        PathNode.QuadTo(176.4f, 908.9f, 206.4f, 923.9f),
                        PathNode.QuadTo(222.4f, 931.9f, 245.4f, 933.9f),
                        PathNode.QuadTo(268.4f, 935.9f, 321.4f, 935.9f),
                        PathNode.LineTo(478.4f, 935.9f),
                        PathNode.LineTo(478.4f, 205.9f),
                        PathNode.LineTo(321.4f, 205.9f),
                        PathNode.QuadTo(268.4f, 205.9f, 245.4f, 207.9f),
                        PathNode.QuadTo(222.4f, 209.9f, 206.4f, 217.9f),
                        PathNode.Close,
                        PathNode.MoveTo(397.4f, 801.9f),
                        PathNode.LineTo(397.4f, 828.9f),
                        PathNode.QuadTo(397.4f, 842.9f, 389.9f, 850.4f),
                        PathNode.QuadTo(382.4f, 857.9f, 368.4f, 857.9f),
                        PathNode.LineTo(261.4f, 857.9f),
                        PathNode.QuadTo(249.4f, 857.9f, 240.4f, 851.4f),
                        PathNode.QuadTo(231.4f, 844.9f, 231.4f, 830.9f),
                        PathNode.LineTo(231.4f, 799.9f),
                        PathNode.QuadTo(231.4f, 785.9f, 240.4f, 779.4f),
                        PathNode.QuadTo(249.4f, 772.9f, 261.4f, 772.9f),
                        PathNode.LineTo(368.4f, 772.9f),
                        PathNode.QuadTo(382.4f, 772.9f, 389.9f, 780.9f),
                        PathNode.QuadTo(397.4f, 788.9f, 397.4f, 801.9f),
                        PathNode.Close,
                        PathNode.MoveTo(563.4f, 935.9f),
                        PathNode.LineTo(820.4f, 935.9f),
                        PathNode.QuadTo(874.4f, 935.9f, 896.9f, 933.9f),
                        PathNode.QuadTo(919.4f, 931.9f, 936.4f, 923.9f),
                        PathNode.QuadTo(968.4f, 907.9f, 983.4f, 875.9f),
                        PathNode.QuadTo(991.4f, 859.9f, 993.4f, 837.4f),
                        PathNode.QuadTo(995.4f, 814.9f, 995.4f, 760.9f),
                        PathNode.LineTo(995.4f, 380.9f),
                        PathNode.QuadTo(995.4f, 327.9f, 993.4f, 304.9f),
                        PathNode.QuadTo(991.4f, 281.9f, 983.4f, 265.9f),
                        PathNode.QuadTo(969.4f, 235.9f, 936.4f, 217.9f),
                        PathNode.QuadTo(919.4f, 209.9f, 896.9f, 207.9f),
                        PathNode.QuadTo(874.4f, 205.9f, 820.4f, 205.9f),
                        PathNode.LineTo(563.4f, 205.9f),
                        PathNode.Close,
                        PathNode.MoveTo(397.4f, 650.9f),
                        PathNode.LineTo(397.4f, 677.9f),
                        PathNode.QuadTo(397.4f, 691.9f, 389.9f, 699.4f),
                        PathNode.QuadTo(382.4f, 706.9f, 368.4f, 706.9f),
                        PathNode.LineTo(261.4f, 706.9f),
                        PathNode.QuadTo(249.4f, 706.9f, 240.4f, 700.4f),
                        PathNode.QuadTo(231.4f, 693.9f, 231.4f, 679.9f),
                        PathNode.LineTo(231.4f, 648.9f),
                        PathNode.QuadTo(231.4f, 634.9f, 240.4f, 628.4f),
                        PathNode.QuadTo(249.4f, 621.9f, 261.4f, 621.9f),
                        PathNode.LineTo(368.4f, 621.9f),
                        PathNode.QuadTo(382.4f, 621.9f, 389.9f, 629.9f),
                        PathNode.QuadTo(397.4f, 637.9f, 397.4f, 650.9f),
                        PathNode.Close,
                        PathNode.MoveTo(397.4f, 500.9f),
                        PathNode.LineTo(397.4f, 527.9f),
                        PathNode.QuadTo(397.4f, 541.9f, 389.9f, 549.4f),
                        PathNode.QuadTo(382.4f, 556.9f, 368.4f, 556.9f),
                        PathNode.LineTo(261.4f, 556.9f),
                        PathNode.QuadTo(249.4f, 556.9f, 240.4f, 550.4f),
                        PathNode.QuadTo(231.4f, 543.9f, 231.4f, 529.9f),
                        PathNode.LineTo(231.4f, 498.9f),
                        PathNode.QuadTo(231.4f, 484.9f, 240.4f, 478.4f),
                        PathNode.QuadTo(249.4f, 471.9f, 261.4f, 471.9f),
                        PathNode.LineTo(368.4f, 471.9f),
                        PathNode.QuadTo(382.4f, 471.9f, 389.9f, 479.9f),
                        PathNode.QuadTo(397.4f, 487.9f, 397.4f, 500.9f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _sidebarRegular!!
    }

private var _sidebarRegular: ImageVector? = null
