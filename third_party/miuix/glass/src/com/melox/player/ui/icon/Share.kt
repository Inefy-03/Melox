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

internal val GlassIcons.Share: ImageVector
    get() {
        if (_shareRegular != null) return _shareRegular!!
        _shareRegular = ImageVector.Builder(
            name = "Share.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(917.4f, 5.2f),
                        PathNode.QuadTo(944.4f, 19.2f, 965.9f, 40.7f),
                        PathNode.QuadTo(987.4f, 62.2f, 1000.4f, 88.2f),
                        PathNode.QuadTo(1014.4f, 116.2f, 1017.9f, 155.7f),
                        PathNode.QuadTo(1021.4f, 195.2f, 1021.4f, 289.2f),
                        PathNode.LineTo(1021.4f, 508.2f),
                        PathNode.QuadTo(1021.4f, 583.2f, 1019.4f, 615.7f),
                        PathNode.QuadTo(1017.4f, 648.2f, 1007.4f, 671.2f),
                        PathNode.QuadTo(993.4f, 707.2f, 965.9f, 734.7f),
                        PathNode.QuadTo(938.4f, 762.2f, 902.4f, 776.2f),
                        PathNode.QuadTo(880.4f, 785.2f, 849.4f, 787.7f),
                        PathNode.QuadTo(818.4f, 790.2f, 751.4f, 790.2f),
                        PathNode.LineTo(728.4f, 790.2f),
                        PathNode.QuadTo(716.4f, 790.2f, 707.9f, 782.7f),
                        PathNode.QuadTo(699.4f, 775.2f, 699.4f, 760.2f),
                        PathNode.LineTo(699.4f, 737.2f),
                        PathNode.QuadTo(699.4f, 718.2f, 707.9f, 711.2f),
                        PathNode.QuadTo(716.4f, 704.2f, 731.4f, 704.2f),
                        PathNode.LineTo(761.4f, 704.2f),
                        PathNode.QuadTo(814.4f, 704.2f, 836.9f, 702.2f),
                        PathNode.QuadTo(859.4f, 700.2f, 876.4f, 692.2f),
                        PathNode.QuadTo(907.4f, 676.2f, 923.4f, 645.2f),
                        PathNode.QuadTo(931.4f, 628.2f, 933.4f, 605.7f),
                        PathNode.QuadTo(935.4f, 583.2f, 935.4f, 529.2f),
                        PathNode.LineTo(935.4f, 245.2f),
                        PathNode.QuadTo(935.4f, 191.2f, 933.4f, 168.7f),
                        PathNode.QuadTo(931.4f, 146.2f, 923.4f, 130.2f),
                        PathNode.QuadTo(909.4f, 100.2f, 876.4f, 82.2f),
                        PathNode.QuadTo(859.4f, 74.2f, 836.9f, 72.2f),
                        PathNode.QuadTo(814.4f, 70.2f, 761.4f, 70.2f),
                        PathNode.LineTo(381.4f, 70.2f),
                        PathNode.QuadTo(328.4f, 70.2f, 305.4f, 72.2f),
                        PathNode.QuadTo(282.4f, 74.2f, 266.4f, 82.2f),
                        PathNode.QuadTo(233.4f, 100.2f, 218.4f, 130.2f),
                        PathNode.QuadTo(210.4f, 146.2f, 208.4f, 168.7f),
                        PathNode.QuadTo(206.4f, 191.2f, 206.4f, 245.2f),
                        PathNode.LineTo(206.4f, 529.2f),
                        PathNode.QuadTo(206.4f, 583.2f, 208.4f, 605.7f),
                        PathNode.QuadTo(210.4f, 628.2f, 218.4f, 645.2f),
                        PathNode.QuadTo(234.4f, 677.2f, 266.4f, 692.2f),
                        PathNode.QuadTo(282.4f, 700.2f, 305.4f, 702.2f),
                        PathNode.QuadTo(328.4f, 704.2f, 381.4f, 704.2f),
                        PathNode.LineTo(410.4f, 704.2f),
                        PathNode.QuadTo(427.4f, 704.2f, 434.9f, 711.7f),
                        PathNode.QuadTo(442.4f, 719.2f, 442.4f, 737.2f),
                        PathNode.LineTo(442.4f, 760.2f),
                        PathNode.QuadTo(442.4f, 775.2f, 434.4f, 782.7f),
                        PathNode.QuadTo(426.4f, 790.2f, 412.4f, 790.2f),
                        PathNode.QuadTo(330.4f, 790.2f, 296.9f, 788.2f),
                        PathNode.QuadTo(263.4f, 786.2f, 240.4f, 776.2f),
                        PathNode.QuadTo(204.4f, 762.2f, 176.9f, 734.7f),
                        PathNode.QuadTo(149.4f, 707.2f, 134.4f, 670.2f),
                        PathNode.QuadTo(125.4f, 647.2f, 123.4f, 615.2f),
                        PathNode.QuadTo(121.4f, 583.2f, 121.4f, 508.2f),
                        PathNode.LineTo(121.4f, 289.2f),
                        PathNode.QuadTo(121.4f, 195.2f, 124.4f, 155.7f),
                        PathNode.QuadTo(127.4f, 116.2f, 141.4f, 88.2f),
                        PathNode.QuadTo(155.4f, 62.2f, 176.9f, 40.7f),
                        PathNode.QuadTo(198.4f, 19.2f, 224.4f, 5.2f),
                        PathNode.QuadTo(252.4f, -8.8f, 292.4f, -11.8f),
                        PathNode.QuadTo(332.4f, -14.8f, 425.4f, -14.8f),
                        PathNode.LineTo(717.4f, -14.8f),
                        PathNode.QuadTo(810.4f, -14.8f, 849.9f, -11.8f),
                        PathNode.QuadTo(889.4f, -8.8f, 917.4f, 5.2f),
                        PathNode.Close,
                        PathNode.MoveTo(613.4f, 365.2f),
                        PathNode.LineTo(613.4f, 1010.2f),
                        PathNode.LineTo(746.4f, 877.2f),
                        PathNode.QuadTo(758.4f, 865.2f, 769.9f, 864.7f),
                        PathNode.QuadTo(781.4f, 864.2f, 795.4f, 878.2f),
                        PathNode.LineTo(806.4f, 889.2f),
                        PathNode.QuadTo(820.4f, 903.2f, 820.4f, 913.7f),
                        PathNode.QuadTo(820.4f, 924.2f, 807.4f, 937.2f),
                        PathNode.LineTo(601.4f, 1143.2f),
                        PathNode.QuadTo(587.4f, 1157.2f, 571.4f, 1157.7f),
                        PathNode.QuadTo(555.4f, 1158.2f, 542.4f, 1145.2f),
                        PathNode.LineTo(329.4f, 932.2f),
                        PathNode.QuadTo(322.4f, 926.2f, 321.4f, 915.7f),
                        PathNode.QuadTo(320.4f, 905.2f, 330.4f, 895.2f),
                        PathNode.LineTo(351.4f, 874.2f),
                        PathNode.QuadTo(362.4f, 863.2f, 372.9f, 864.2f),
                        PathNode.QuadTo(383.4f, 865.2f, 395.4f, 877.2f),
                        PathNode.LineTo(528.4f, 1010.2f),
                        PathNode.LineTo(528.4f, 365.2f),
                        PathNode.QuadTo(528.4f, 350.2f, 535.9f, 343.2f),
                        PathNode.QuadTo(543.4f, 336.2f, 558.4f, 336.2f),
                        PathNode.LineTo(585.4f, 336.2f),
                        PathNode.QuadTo(598.4f, 336.2f, 605.9f, 343.7f),
                        PathNode.QuadTo(613.4f, 351.2f, 613.4f, 365.2f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _shareRegular!!
    }

private var _shareRegular: ImageVector? = null
