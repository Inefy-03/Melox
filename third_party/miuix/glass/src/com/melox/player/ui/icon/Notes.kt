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

internal val GlassIcons.Notes: ImageVector
    get() {
        if (_notesRegular != null) return _notesRegular!!
        _notesRegular = ImageVector.Builder(
            name = "Notes.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(893.9f, 84.9f),
                        PathNode.QuadTo(947.9f, 112.9f, 972.9f, 163.9f),
                        PathNode.QuadTo(987.9f, 192.9f, 991.4f, 233.9f),
                        PathNode.QuadTo(994.9f, 274.9f, 994.9f, 370.9f),
                        PathNode.LineTo(994.9f, 925.9f),
                        PathNode.QuadTo(994.9f, 973.9f, 993.4f, 994.4f),
                        PathNode.QuadTo(991.9f, 1014.9f, 984.9f, 1028.9f),
                        PathNode.QuadTo(971.9f, 1054.9f, 944.9f, 1068.9f),
                        PathNode.QuadTo(930.9f, 1076.9f, 910.4f, 1078.4f),
                        PathNode.QuadTo(889.9f, 1079.9f, 841.9f, 1079.9f),
                        PathNode.LineTo(454.9f, 1079.9f),
                        PathNode.QuadTo(359.9f, 1079.9f, 318.9f, 1076.4f),
                        PathNode.QuadTo(277.9f, 1072.9f, 247.9f, 1057.9f),
                        PathNode.QuadTo(194.9f, 1028.9f, 169.9f, 978.9f),
                        PathNode.QuadTo(154.9f, 949.9f, 151.4f, 908.9f),
                        PathNode.QuadTo(147.9f, 867.9f, 147.9f, 771.9f),
                        PathNode.LineTo(147.9f, 216.9f),
                        PathNode.QuadTo(147.9f, 168.9f, 149.4f, 148.4f),
                        PathNode.QuadTo(150.9f, 127.9f, 157.9f, 113.9f),
                        PathNode.QuadTo(170.9f, 87.9f, 197.9f, 73.9f),
                        PathNode.QuadTo(211.9f, 65.9f, 232.4f, 64.4f),
                        PathNode.QuadTo(252.9f, 62.9f, 300.9f, 62.9f),
                        PathNode.LineTo(686.9f, 62.9f),
                        PathNode.QuadTo(782.9f, 62.9f, 823.9f, 66.4f),
                        PathNode.QuadTo(864.9f, 69.9f, 893.9f, 84.9f),
                        PathNode.Close,
                        PathNode.MoveTo(232.9f, 189.9f),
                        PathNode.LineTo(232.9f, 816.9f),
                        PathNode.QuadTo(232.9f, 871.9f, 234.9f, 895.9f),
                        PathNode.QuadTo(236.9f, 919.9f, 244.9f, 935.9f),
                        PathNode.QuadTo(260.9f, 965.9f, 290.9f, 981.9f),
                        PathNode.QuadTo(307.9f, 989.9f, 331.4f, 991.9f),
                        PathNode.QuadTo(354.9f, 993.9f, 410.9f, 993.9f),
                        PathNode.LineTo(869.9f, 993.9f),
                        PathNode.QuadTo(894.9f, 993.9f, 902.4f, 985.9f),
                        PathNode.QuadTo(909.9f, 977.9f, 909.9f, 951.9f),
                        PathNode.LineTo(909.9f, 324.9f),
                        PathNode.QuadTo(909.9f, 270.9f, 907.9f, 246.9f),
                        PathNode.QuadTo(905.9f, 222.9f, 896.9f, 206.9f),
                        PathNode.QuadTo(882.9f, 177.9f, 850.9f, 160.9f),
                        PathNode.QuadTo(834.9f, 152.9f, 811.4f, 150.9f),
                        PathNode.QuadTo(787.9f, 148.9f, 731.9f, 148.9f),
                        PathNode.LineTo(263.9f, 148.9f),
                        PathNode.QuadTo(246.9f, 148.9f, 239.9f, 157.9f),
                        PathNode.QuadTo(232.9f, 166.9f, 232.9f, 189.9f),
                        PathNode.Close,
                        PathNode.MoveTo(638.9f, 343.9f),
                        PathNode.LineTo(638.9f, 368.9f),
                        PathNode.QuadTo(638.9f, 384.9f, 630.4f, 391.4f),
                        PathNode.QuadTo(621.9f, 397.9f, 604.9f, 397.9f),
                        PathNode.LineTo(385.9f, 397.9f),
                        PathNode.QuadTo(367.9f, 397.9f, 359.9f, 391.4f),
                        PathNode.QuadTo(351.9f, 384.9f, 351.9f, 366.9f),
                        PathNode.LineTo(351.9f, 343.9f),
                        PathNode.QuadTo(351.9f, 326.9f, 359.9f, 319.9f),
                        PathNode.QuadTo(367.9f, 312.9f, 385.9f, 312.9f),
                        PathNode.LineTo(604.9f, 312.9f),
                        PathNode.QuadTo(622.9f, 312.9f, 630.9f, 319.9f),
                        PathNode.QuadTo(638.9f, 326.9f, 638.9f, 343.9f),
                        PathNode.Close,
                        PathNode.MoveTo(790.9f, 775.9f),
                        PathNode.LineTo(790.9f, 800.9f),
                        PathNode.QuadTo(790.9f, 816.9f, 782.9f, 823.4f),
                        PathNode.QuadTo(774.9f, 829.9f, 757.9f, 829.9f),
                        PathNode.LineTo(383.9f, 829.9f),
                        PathNode.QuadTo(367.9f, 829.9f, 359.9f, 822.9f),
                        PathNode.QuadTo(351.9f, 815.9f, 351.9f, 800.9f),
                        PathNode.LineTo(351.9f, 775.9f),
                        PathNode.QuadTo(351.9f, 758.9f, 360.4f, 751.9f),
                        PathNode.QuadTo(368.9f, 744.9f, 383.9f, 744.9f),
                        PathNode.LineTo(757.9f, 744.9f),
                        PathNode.QuadTo(774.9f, 744.9f, 782.9f, 751.9f),
                        PathNode.QuadTo(790.9f, 758.9f, 790.9f, 775.9f),
                        PathNode.Close,
                        PathNode.MoveTo(790.9f, 558.9f),
                        PathNode.LineTo(790.9f, 583.9f),
                        PathNode.QuadTo(790.9f, 599.9f, 782.9f, 606.4f),
                        PathNode.QuadTo(774.9f, 612.9f, 757.9f, 612.9f),
                        PathNode.LineTo(383.9f, 612.9f),
                        PathNode.QuadTo(367.9f, 612.9f, 359.9f, 605.9f),
                        PathNode.QuadTo(351.9f, 598.9f, 351.9f, 583.9f),
                        PathNode.LineTo(351.9f, 558.9f),
                        PathNode.QuadTo(351.9f, 541.9f, 360.4f, 534.9f),
                        PathNode.QuadTo(368.9f, 527.9f, 383.9f, 527.9f),
                        PathNode.LineTo(757.9f, 527.9f),
                        PathNode.QuadTo(774.9f, 527.9f, 782.9f, 534.9f),
                        PathNode.QuadTo(790.9f, 541.9f, 790.9f, 558.9f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _notesRegular!!
    }

private var _notesRegular: ImageVector? = null
