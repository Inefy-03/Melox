// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.melox.player.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

// Regular toolbar vectors from the pinned Miuix Glass demo; other app icons keep their own style.
internal object GlassTopBarIcons

internal val GlassTopBarIcons.ChevronBackward: ImageVector
    get() {
        if (_chevronBackwardRegular != null) return _chevronBackwardRegular!!
        _chevronBackwardRegular = ImageVector.Builder(
            name = "ChevronBackward.Glass.Regular",
            defaultWidth = 28.0f.dp,
            defaultHeight = 28.0f.dp,
            viewportWidth = 1333.3833f,
            viewportHeight = 1333.3833f,
            autoMirror = true,
        ).apply {
            group(scaleY = -1.0f, translationX = 9.5417f, translationY = 1238.1417f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(301.7f, 551.4f),
                        PathNode.LineTo(775.7f, 78.4f),
                        PathNode.QuadTo(785.7f, 68.4f, 796.2f, 68.9f),
                        PathNode.QuadTo(806.7f, 69.4f, 820.7f, 83.4f),
                        PathNode.LineTo(838.7f, 101.4f),
                        PathNode.QuadTo(847.7f, 110.4f, 848.7f, 120.4f),
                        PathNode.QuadTo(849.7f, 130.4f, 841.7f, 138.4f),
                        PathNode.LineTo(410.7f, 570.4f),
                        PathNode.LineTo(841.7f, 1001.4f),
                        PathNode.QuadTo(849.7f, 1009.4f, 849.2f, 1020.4f),
                        PathNode.QuadTo(848.7f, 1031.4f, 841.7f, 1039.4f),
                        PathNode.LineTo(812.7f, 1067.4f),
                        PathNode.QuadTo(805.7f, 1074.4f, 795.7f, 1073.9f),
                        PathNode.QuadTo(785.7f, 1073.4f, 777.7f, 1065.4f),
                        PathNode.LineTo(300.7f, 588.4f),
                        PathNode.QuadTo(293.7f, 581.4f, 293.7f, 570.4f),
                        PathNode.QuadTo(293.7f, 559.4f, 301.7f, 551.4f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _chevronBackwardRegular!!
    }

private var _chevronBackwardRegular: ImageVector? = null

internal val GlassTopBarIcons.Search: ImageVector
    get() {
        if (_searchRegular != null) return _searchRegular!!
        _searchRegular = ImageVector.Builder(
            name = "Search.Glass.Regular",
            defaultWidth = 28.0f.dp,
            defaultHeight = 28.0f.dp,
            viewportWidth = 1333.3833f,
            viewportHeight = 1333.3833f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 95.2417f, translationY = 1238.1417f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(695.9f, 335.2f),
                        PathNode.QuadTo(715.9f, 347.2f, 723.9f, 346.7f),
                        PathNode.QuadTo(731.9f, 346.2f, 745.9f, 332.2f),
                        PathNode.LineTo(996.9f, 81.2f),
                        PathNode.QuadTo(1007.9f, 70.2f, 1018.4f, 69.7f),
                        PathNode.QuadTo(1028.9f, 69.2f, 1040.9f, 81.2f),
                        PathNode.LineTo(1057.9f, 98.2f),
                        PathNode.QuadTo(1069.9f, 110.2f, 1069.9f, 119.7f),
                        PathNode.QuadTo(1069.9f, 129.2f, 1057.9f, 141.2f),
                        PathNode.LineTo(803.9f, 396.2f),
                        PathNode.QuadTo(791.9f, 408.2f, 792.4f, 415.7f),
                        PathNode.QuadTo(792.9f, 423.2f, 805.9f, 442.2f),
                        PathNode.QuadTo(839.9f, 491.2f, 858.4f, 549.7f),
                        PathNode.QuadTo(876.9f, 608.2f, 876.9f, 671.2f),
                        PathNode.QuadTo(876.9f, 780.2f, 822.9f, 872.7f),
                        PathNode.QuadTo(768.9f, 965.2f, 676.4f, 1019.2f),
                        PathNode.QuadTo(583.9f, 1073.2f, 474.9f, 1073.2f),
                        PathNode.QuadTo(365.9f, 1073.2f, 273.4f, 1019.2f),
                        PathNode.QuadTo(180.9f, 965.2f, 126.9f, 872.7f),
                        PathNode.QuadTo(72.9f, 780.2f, 72.9f, 671.2f),
                        PathNode.QuadTo(72.9f, 562.2f, 126.9f, 469.7f),
                        PathNode.QuadTo(180.9f, 377.2f, 273.4f, 323.2f),
                        PathNode.QuadTo(365.9f, 269.2f, 474.9f, 269.2f),
                        PathNode.QuadTo(534.9f, 269.2f, 591.4f, 286.2f),
                        PathNode.QuadTo(647.9f, 303.2f, 695.9f, 335.2f),
                        PathNode.Close,
                        PathNode.MoveTo(157.9f, 671.2f),
                        PathNode.QuadTo(157.9f, 757.2f, 200.4f, 830.2f),
                        PathNode.QuadTo(242.9f, 903.2f, 315.9f, 945.7f),
                        PathNode.QuadTo(388.9f, 988.2f, 474.9f, 988.2f),
                        PathNode.QuadTo(560.9f, 988.2f, 633.4f, 945.7f),
                        PathNode.QuadTo(705.9f, 903.2f, 748.4f, 830.2f),
                        PathNode.QuadTo(790.9f, 757.2f, 790.9f, 671.2f),
                        PathNode.QuadTo(790.9f, 585.2f, 748.4f, 512.2f),
                        PathNode.QuadTo(705.9f, 439.2f, 633.4f, 396.7f),
                        PathNode.QuadTo(560.9f, 354.2f, 474.9f, 354.2f),
                        PathNode.QuadTo(388.9f, 354.2f, 315.9f, 396.7f),
                        PathNode.QuadTo(242.9f, 439.2f, 200.4f, 512.2f),
                        PathNode.QuadTo(157.9f, 585.2f, 157.9f, 671.2f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _searchRegular!!
    }

private var _searchRegular: ImageVector? = null

internal val GlassTopBarIcons.SettingsOutline: ImageVector
    get() {
        if (_settingsRegular != null) return _settingsRegular!!
        _settingsRegular = ImageVector.Builder(
            name = "SettingsOutline.Glass.Regular",
            defaultWidth = 28.0f.dp,
            defaultHeight = 28.0f.dp,
            viewportWidth = 1333.3833f,
            viewportHeight = 1333.3833f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 95.2417f, translationY = 1238.1417f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(595.4f, 60.2f),
                        PathNode.QuadTo(606.4f, 62.2f, 620.4f, 69.2f),
                        PathNode.QuadTo(634.4f, 76.2f, 651.4f, 86.2f),
                        PathNode.LineTo(664.4f, 93.2f),
                        PathNode.LineTo(945.4f, 251.2f),
                        PathNode.QuadTo(975.4f, 268.2f, 988.9f, 276.7f),
                        PathNode.QuadTo(1002.4f, 285.2f, 1011.4f, 295.2f),
                        PathNode.QuadTo(1029.4f, 314.2f, 1036.4f, 338.2f),
                        PathNode.QuadTo(1040.4f, 350.2f, 1041.4f, 370.2f),
                        PathNode.QuadTo(1042.4f, 390.2f, 1042.4f, 417.2f),
                        PathNode.LineTo(1042.4f, 726.2f),
                        PathNode.QuadTo(1042.4f, 753.2f, 1041.4f, 772.7f),
                        PathNode.QuadTo(1040.4f, 792.2f, 1036.4f, 804.2f),
                        PathNode.QuadTo(1029.4f, 827.2f, 1011.4f, 848.2f),
                        PathNode.QuadTo(1002.4f, 858.2f, 985.9f, 868.2f),
                        PathNode.QuadTo(969.4f, 878.2f, 945.4f, 891.2f),
                        PathNode.LineTo(664.4f, 1050.2f),
                        PathNode.QuadTo(642.4f, 1063.2f, 624.9f, 1071.7f),
                        PathNode.QuadTo(607.4f, 1080.2f, 595.4f, 1082.2f),
                        PathNode.QuadTo(571.4f, 1088.2f, 547.4f, 1082.2f),
                        PathNode.QuadTo(535.4f, 1080.2f, 517.9f, 1071.7f),
                        PathNode.QuadTo(500.4f, 1063.2f, 478.4f, 1050.2f),
                        PathNode.LineTo(197.4f, 891.2f),
                        PathNode.QuadTo(190.4f, 887.2f, 182.4f, 883.2f),
                        PathNode.QuadTo(165.4f, 873.2f, 151.9f, 864.7f),
                        PathNode.QuadTo(138.4f, 856.2f, 131.4f, 848.2f),
                        PathNode.QuadTo(113.4f, 827.2f, 106.4f, 804.2f),
                        PathNode.QuadTo(102.4f, 792.2f, 101.4f, 772.7f),
                        PathNode.QuadTo(100.4f, 753.2f, 100.4f, 726.2f),
                        PathNode.LineTo(100.4f, 417.2f),
                        PathNode.QuadTo(100.4f, 390.2f, 101.4f, 370.2f),
                        PathNode.QuadTo(102.4f, 350.2f, 106.4f, 338.2f),
                        PathNode.QuadTo(113.4f, 314.2f, 131.4f, 295.2f),
                        PathNode.QuadTo(140.4f, 285.2f, 157.4f, 274.7f),
                        PathNode.QuadTo(174.4f, 264.2f, 197.4f, 251.2f),
                        PathNode.LineTo(478.4f, 93.2f),
                        PathNode.LineTo(491.4f, 86.2f),
                        PathNode.QuadTo(508.4f, 76.2f, 522.4f, 69.2f),
                        PathNode.QuadTo(536.4f, 62.2f, 547.4f, 60.2f),
                        PathNode.QuadTo(571.4f, 55.2f, 595.4f, 60.2f),
                        PathNode.Close,
                        PathNode.MoveTo(186.4f, 390.2f),
                        PathNode.LineTo(186.4f, 759.2f),
                        PathNode.QuadTo(186.4f, 773.2f, 192.9f, 783.7f),
                        PathNode.QuadTo(199.4f, 794.2f, 214.4f, 803.2f),
                        PathNode.LineTo(539.4f, 986.2f),
                        PathNode.QuadTo(558.4f, 997.2f, 569.9f, 997.7f),
                        PathNode.QuadTo(581.4f, 998.2f, 598.4f, 989.2f),
                        PathNode.LineTo(918.4f, 809.2f),
                        PathNode.QuadTo(943.4f, 795.2f, 950.4f, 783.2f),
                        PathNode.QuadTo(957.4f, 771.2f, 957.4f, 742.2f),
                        PathNode.LineTo(957.4f, 393.2f),
                        PathNode.QuadTo(957.4f, 374.2f, 952.4f, 362.7f),
                        PathNode.QuadTo(947.4f, 351.2f, 933.4f, 343.2f),
                        PathNode.LineTo(603.4f, 156.2f),
                        PathNode.QuadTo(584.4f, 145.2f, 571.9f, 145.2f),
                        PathNode.QuadTo(559.4f, 145.2f, 539.4f, 156.2f),
                        PathNode.LineTo(217.4f, 338.2f),
                        PathNode.QuadTo(199.4f, 348.2f, 192.9f, 358.7f),
                        PathNode.QuadTo(186.4f, 369.2f, 186.4f, 390.2f),
                        PathNode.Close,
                        PathNode.MoveTo(771.4f, 571.2f),
                        PathNode.QuadTo(771.4f, 626.2f, 744.4f, 671.7f),
                        PathNode.QuadTo(717.4f, 717.2f, 671.9f, 744.2f),
                        PathNode.QuadTo(626.4f, 771.2f, 571.4f, 771.2f),
                        PathNode.QuadTo(517.4f, 771.2f, 471.4f, 744.2f),
                        PathNode.QuadTo(425.4f, 717.2f, 398.4f, 671.7f),
                        PathNode.QuadTo(371.4f, 626.2f, 371.4f, 571.2f),
                        PathNode.QuadTo(371.4f, 517.2f, 398.4f, 471.2f),
                        PathNode.QuadTo(425.4f, 425.2f, 471.4f, 398.2f),
                        PathNode.QuadTo(517.4f, 371.2f, 571.4f, 371.2f),
                        PathNode.QuadTo(626.4f, 371.2f, 671.9f, 398.2f),
                        PathNode.QuadTo(717.4f, 425.2f, 744.4f, 471.2f),
                        PathNode.QuadTo(771.4f, 517.2f, 771.4f, 571.2f),
                        PathNode.Close,
                        PathNode.MoveTo(459.4f, 571.2f),
                        PathNode.QuadTo(459.4f, 602.2f, 474.4f, 627.7f),
                        PathNode.QuadTo(489.4f, 653.2f, 514.9f, 668.2f),
                        PathNode.QuadTo(540.4f, 683.2f, 571.4f, 683.2f),
                        PathNode.QuadTo(602.4f, 683.2f, 627.9f, 668.2f),
                        PathNode.QuadTo(653.4f, 653.2f, 668.4f, 627.7f),
                        PathNode.QuadTo(683.4f, 602.2f, 683.4f, 571.2f),
                        PathNode.QuadTo(683.4f, 540.2f, 668.4f, 514.7f),
                        PathNode.QuadTo(653.4f, 489.2f, 627.9f, 474.2f),
                        PathNode.QuadTo(602.4f, 459.2f, 571.4f, 459.2f),
                        PathNode.QuadTo(540.4f, 459.2f, 514.9f, 474.2f),
                        PathNode.QuadTo(489.4f, 489.2f, 474.4f, 514.7f),
                        PathNode.QuadTo(459.4f, 540.2f, 459.4f, 571.2f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _settingsRegular!!
    }

private var _settingsRegular: ImageVector? = null

internal val GlassTopBarIcons.More: ImageVector
    get() {
        if (_moreRegular != null) return _moreRegular!!
        _moreRegular = ImageVector.Builder(
            name = "More.Glass.Regular",
            defaultWidth = 28.0f.dp,
            defaultHeight = 28.0f.dp,
            viewportWidth = 1333.3833f,
            viewportHeight = 1333.3833f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 95.2417f, translationY = 1238.1417f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(571.4f, 1005.9f),
                        PathNode.QuadTo(542.4f, 1005.9f, 521.4f, 984.9f),
                        PathNode.QuadTo(500.4f, 963.9f, 500.4f, 934.9f),
                        PathNode.QuadTo(500.4f, 905.9f, 521.4f, 884.9f),
                        PathNode.QuadTo(542.4f, 863.9f, 571.4f, 863.9f),
                        PathNode.QuadTo(600.4f, 863.9f, 621.4f, 884.9f),
                        PathNode.QuadTo(642.4f, 905.9f, 642.4f, 934.9f),
                        PathNode.QuadTo(642.4f, 963.9f, 621.4f, 984.9f),
                        PathNode.QuadTo(600.4f, 1005.9f, 571.4f, 1005.9f),
                        PathNode.Close,
                        PathNode.MoveTo(571.4f, 277.9f),
                        PathNode.QuadTo(542.4f, 277.9f, 521.4f, 257.4f),
                        PathNode.QuadTo(500.4f, 236.9f, 500.4f, 206.9f),
                        PathNode.QuadTo(500.4f, 177.9f, 521.4f, 157.4f),
                        PathNode.QuadTo(542.4f, 136.9f, 571.4f, 136.9f),
                        PathNode.QuadTo(600.4f, 136.9f, 621.4f, 157.4f),
                        PathNode.QuadTo(642.4f, 177.9f, 642.4f, 206.9f),
                        PathNode.QuadTo(642.4f, 236.9f, 621.4f, 257.4f),
                        PathNode.QuadTo(600.4f, 277.9f, 571.4f, 277.9f),
                        PathNode.Close,
                        PathNode.MoveTo(571.4f, 641.9f),
                        PathNode.QuadTo(542.4f, 641.9f, 521.4f, 620.9f),
                        PathNode.QuadTo(500.4f, 599.9f, 500.4f, 570.9f),
                        PathNode.QuadTo(500.4f, 541.9f, 521.4f, 520.9f),
                        PathNode.QuadTo(542.4f, 499.9f, 571.4f, 499.9f),
                        PathNode.QuadTo(600.4f, 499.9f, 621.4f, 520.9f),
                        PathNode.QuadTo(642.4f, 541.9f, 642.4f, 570.9f),
                        PathNode.QuadTo(642.4f, 599.9f, 621.4f, 620.9f),
                        PathNode.QuadTo(600.4f, 641.9f, 571.4f, 641.9f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _moreRegular!!
    }

private var _moreRegular: ImageVector? = null

internal val GlassTopBarIcons.Add: ImageVector
    get() {
        if (_addRegular != null) return _addRegular!!
        _addRegular = ImageVector.Builder(
            name = "Add.Glass.Regular",
            defaultWidth = 28.0f.dp,
            defaultHeight = 28.0f.dp,
            viewportWidth = 1333.3833f,
            viewportHeight = 1333.3833f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 95.2417f, translationY = 1238.1417f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(616.9f, 111.4f),
                        PathNode.LineTo(616.9f, 1031.4f),
                        PathNode.QuadTo(616.9f, 1043.4f, 609.9f, 1050.9f),
                        PathNode.QuadTo(602.9f, 1058.4f, 587.9f, 1058.4f),
                        PathNode.LineTo(555.9f, 1058.4f),
                        PathNode.QuadTo(542.9f, 1058.4f, 535.4f, 1050.4f),
                        PathNode.QuadTo(527.9f, 1042.4f, 527.9f, 1031.4f),
                        PathNode.LineTo(527.9f, 111.4f),
                        PathNode.QuadTo(527.9f, 98.4f, 535.9f, 91.4f),
                        PathNode.QuadTo(543.9f, 84.4f, 556.9f, 84.4f),
                        PathNode.LineTo(588.9f, 84.4f),
                        PathNode.QuadTo(601.9f, 84.4f, 609.4f, 91.4f),
                        PathNode.QuadTo(616.9f, 98.4f, 616.9f, 111.4f),
                        PathNode.Close,
                        PathNode.MoveTo(1030.9f, 616.4f),
                        PathNode.LineTo(111.9f, 616.4f),
                        PathNode.QuadTo(99.9f, 616.4f, 92.4f, 609.4f),
                        PathNode.QuadTo(84.9f, 602.4f, 84.9f, 587.4f),
                        PathNode.LineTo(84.9f, 553.4f),
                        PathNode.QuadTo(84.9f, 540.4f, 92.9f, 532.9f),
                        PathNode.QuadTo(100.9f, 525.4f, 111.9f, 525.4f),
                        PathNode.LineTo(1030.9f, 525.4f),
                        PathNode.QuadTo(1043.9f, 525.4f, 1050.9f, 533.4f),
                        PathNode.QuadTo(1057.9f, 541.4f, 1057.9f, 554.4f),
                        PathNode.LineTo(1057.9f, 588.4f),
                        PathNode.QuadTo(1057.9f, 601.4f, 1050.9f, 608.9f),
                        PathNode.QuadTo(1043.9f, 616.4f, 1030.9f, 616.4f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _addRegular!!
    }

private var _addRegular: ImageVector? = null

internal val GlassTopBarIcons.SelectAll: ImageVector
    get() {
        if (_selectAllRegular != null) return _selectAllRegular!!
        _selectAllRegular = ImageVector.Builder(
            name = "SelectAll.Glass.Regular",
            defaultWidth = 28.0f.dp,
            defaultHeight = 28.0f.dp,
            viewportWidth = 1333.3833f,
            viewportHeight = 1333.3833f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 95.2417f, translationY = 1238.1417f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(347.0f, 1020.3f),
                        PathNode.LineTo(225.0f, 897.3f),
                        PathNode.QuadTo(220.0f, 892.3f, 215.5f, 891.8f),
                        PathNode.QuadTo(211.0f, 891.3f, 205.0f, 897.3f),
                        PathNode.LineTo(156.0f, 946.3f),
                        PathNode.QuadTo(148.0f, 954.3f, 137.5f, 955.8f),
                        PathNode.QuadTo(127.0f, 957.3f, 117.0f, 947.3f),
                        PathNode.LineTo(96.0f, 926.3f),
                        PathNode.QuadTo(85.0f, 915.3f, 86.0f, 905.3f),
                        PathNode.QuadTo(87.0f, 895.3f, 95.0f, 887.3f),
                        PathNode.LineTo(178.0f, 804.3f),
                        PathNode.QuadTo(197.0f, 785.3f, 215.5f, 785.8f),
                        PathNode.QuadTo(234.0f, 786.3f, 256.0f, 807.3f),
                        PathNode.LineTo(409.0f, 960.3f),
                        PathNode.QuadTo(419.0f, 970.3f, 419.5f, 982.3f),
                        PathNode.QuadTo(420.0f, 994.3f, 410.0f, 1003.3f),
                        PathNode.LineTo(390.0f, 1023.3f),
                        PathNode.QuadTo(379.0f, 1033.3f, 368.0f, 1031.8f),
                        PathNode.QuadTo(357.0f, 1030.3f, 347.0f, 1020.3f),
                        PathNode.Close,
                        PathNode.MoveTo(1057.0f, 540.3f),
                        PathNode.LineTo(1057.0f, 570.3f),
                        PathNode.QuadTo(1057.0f, 585.3f, 1048.5f, 591.8f),
                        PathNode.QuadTo(1040.0f, 598.3f, 1026.0f, 598.3f),
                        PathNode.LineTo(521.0f, 598.3f),
                        PathNode.QuadTo(508.0f, 598.3f, 499.0f, 591.3f),
                        PathNode.QuadTo(490.0f, 584.3f, 490.0f, 571.3f),
                        PathNode.LineTo(490.0f, 541.3f),
                        PathNode.QuadTo(490.0f, 526.3f, 498.5f, 519.3f),
                        PathNode.QuadTo(507.0f, 512.3f, 521.0f, 512.3f),
                        PathNode.LineTo(1026.0f, 512.3f),
                        PathNode.QuadTo(1040.0f, 512.3f, 1048.5f, 518.8f),
                        PathNode.QuadTo(1057.0f, 525.3f, 1057.0f, 540.3f),
                        PathNode.Close,
                        PathNode.MoveTo(1057.0f, 877.3f),
                        PathNode.LineTo(1057.0f, 907.3f),
                        PathNode.QuadTo(1057.0f, 922.3f, 1048.5f, 928.8f),
                        PathNode.QuadTo(1040.0f, 935.3f, 1026.0f, 935.3f),
                        PathNode.LineTo(521.0f, 935.3f),
                        PathNode.QuadTo(508.0f, 935.3f, 499.0f, 928.3f),
                        PathNode.QuadTo(490.0f, 921.3f, 490.0f, 908.3f),
                        PathNode.LineTo(490.0f, 878.3f),
                        PathNode.QuadTo(490.0f, 863.3f, 498.5f, 856.3f),
                        PathNode.QuadTo(507.0f, 849.3f, 521.0f, 849.3f),
                        PathNode.LineTo(1026.0f, 849.3f),
                        PathNode.QuadTo(1040.0f, 849.3f, 1048.5f, 855.8f),
                        PathNode.QuadTo(1057.0f, 862.3f, 1057.0f, 877.3f),
                        PathNode.Close,
                        PathNode.MoveTo(1057.0f, 202.3f),
                        PathNode.LineTo(1057.0f, 232.3f),
                        PathNode.QuadTo(1057.0f, 247.3f, 1048.5f, 253.8f),
                        PathNode.QuadTo(1040.0f, 260.3f, 1026.0f, 260.3f),
                        PathNode.LineTo(521.0f, 260.3f),
                        PathNode.QuadTo(508.0f, 260.3f, 499.0f, 253.3f),
                        PathNode.QuadTo(490.0f, 246.3f, 490.0f, 233.3f),
                        PathNode.LineTo(490.0f, 203.3f),
                        PathNode.QuadTo(490.0f, 188.3f, 498.5f, 181.3f),
                        PathNode.QuadTo(507.0f, 174.3f, 521.0f, 174.3f),
                        PathNode.LineTo(1026.0f, 174.3f),
                        PathNode.QuadTo(1040.0f, 174.3f, 1048.5f, 180.8f),
                        PathNode.QuadTo(1057.0f, 187.3f, 1057.0f, 202.3f),
                        PathNode.Close,
                        PathNode.MoveTo(347.0f, 684.3f),
                        PathNode.LineTo(225.0f, 561.3f),
                        PathNode.QuadTo(220.0f, 556.3f, 215.5f, 555.8f),
                        PathNode.QuadTo(211.0f, 555.3f, 205.0f, 561.3f),
                        PathNode.LineTo(156.0f, 610.3f),
                        PathNode.QuadTo(148.0f, 618.3f, 137.5f, 619.8f),
                        PathNode.QuadTo(127.0f, 621.3f, 117.0f, 611.3f),
                        PathNode.LineTo(96.0f, 590.3f),
                        PathNode.QuadTo(85.0f, 579.3f, 86.0f, 569.3f),
                        PathNode.QuadTo(87.0f, 559.3f, 95.0f, 551.3f),
                        PathNode.LineTo(178.0f, 468.3f),
                        PathNode.QuadTo(197.0f, 449.3f, 215.5f, 449.8f),
                        PathNode.QuadTo(234.0f, 450.3f, 256.0f, 471.3f),
                        PathNode.LineTo(409.0f, 624.3f),
                        PathNode.QuadTo(419.0f, 634.3f, 419.5f, 646.3f),
                        PathNode.QuadTo(420.0f, 658.3f, 410.0f, 667.3f),
                        PathNode.LineTo(390.0f, 687.3f),
                        PathNode.QuadTo(379.0f, 697.3f, 368.0f, 695.8f),
                        PathNode.QuadTo(357.0f, 694.3f, 347.0f, 684.3f),
                        PathNode.Close,
                        PathNode.MoveTo(347.0f, 345.3f),
                        PathNode.LineTo(225.0f, 222.3f),
                        PathNode.QuadTo(220.0f, 217.3f, 215.5f, 216.8f),
                        PathNode.QuadTo(211.0f, 216.3f, 205.0f, 222.3f),
                        PathNode.LineTo(156.0f, 271.3f),
                        PathNode.QuadTo(148.0f, 279.3f, 137.5f, 280.8f),
                        PathNode.QuadTo(127.0f, 282.3f, 117.0f, 272.3f),
                        PathNode.LineTo(96.0f, 251.3f),
                        PathNode.QuadTo(85.0f, 240.3f, 86.0f, 230.3f),
                        PathNode.QuadTo(87.0f, 220.3f, 95.0f, 212.3f),
                        PathNode.LineTo(178.0f, 129.3f),
                        PathNode.QuadTo(197.0f, 110.3f, 215.5f, 110.8f),
                        PathNode.QuadTo(234.0f, 111.3f, 256.0f, 132.3f),
                        PathNode.LineTo(409.0f, 285.3f),
                        PathNode.QuadTo(419.0f, 295.3f, 419.5f, 307.3f),
                        PathNode.QuadTo(420.0f, 319.3f, 410.0f, 328.3f),
                        PathNode.LineTo(390.0f, 348.3f),
                        PathNode.QuadTo(379.0f, 358.3f, 368.0f, 356.8f),
                        PathNode.QuadTo(357.0f, 355.3f, 347.0f, 345.3f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _selectAllRegular!!
    }

private var _selectAllRegular: ImageVector? = null

internal val GlassTopBarIcons.Close: ImageVector
    get() {
        if (_closeRegular != null) return _closeRegular!!
        _closeRegular = ImageVector.Builder(
            name = "Close.Glass.Regular",
            defaultWidth = 28.0f.dp,
            defaultHeight = 28.0f.dp,
            viewportWidth = 1333.3833f,
            viewportHeight = 1333.3833f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 95.2417f, translationY = 1238.1417f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(183.4f, 981.7f),
                        PathNode.LineTo(161.4f, 959.7f),
                        PathNode.QuadTo(151.4f, 949.7f, 151.4f, 939.2f),
                        PathNode.QuadTo(151.4f, 928.7f, 161.4f, 917.7f),
                        PathNode.LineTo(507.4f, 570.7f),
                        PathNode.LineTo(160.4f, 225.7f),
                        PathNode.QuadTo(150.4f, 215.7f, 150.4f, 205.2f),
                        PathNode.QuadTo(150.4f, 194.7f, 160.4f, 183.7f),
                        PathNode.LineTo(182.4f, 161.7f),
                        PathNode.QuadTo(192.4f, 151.7f, 203.4f, 151.7f),
                        PathNode.QuadTo(214.4f, 151.7f, 224.4f, 161.7f),
                        PathNode.LineTo(571.4f, 506.7f),
                        PathNode.LineTo(917.4f, 160.7f),
                        PathNode.QuadTo(938.4f, 139.7f, 959.4f, 160.7f),
                        PathNode.LineTo(981.4f, 182.7f),
                        PathNode.QuadTo(992.4f, 192.7f, 992.4f, 203.7f),
                        PathNode.QuadTo(992.4f, 214.7f, 981.4f, 224.7f),
                        PathNode.LineTo(635.4f, 570.7f),
                        PathNode.LineTo(980.4f, 917.7f),
                        PathNode.QuadTo(990.4f, 928.7f, 990.4f, 939.2f),
                        PathNode.QuadTo(990.4f, 949.7f, 980.4f, 959.7f),
                        PathNode.LineTo(958.4f, 981.7f),
                        PathNode.QuadTo(948.4f, 992.7f, 937.4f, 992.7f),
                        PathNode.QuadTo(926.4f, 992.7f, 916.4f, 981.7f),
                        PathNode.LineTo(571.4f, 634.7f),
                        PathNode.LineTo(225.4f, 981.7f),
                        PathNode.QuadTo(215.4f, 992.7f, 204.9f, 992.7f),
                        PathNode.QuadTo(194.4f, 992.7f, 183.4f, 981.7f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _closeRegular!!
    }

private var _closeRegular: ImageVector? = null

internal val GlassTopBarIcons.Ok: ImageVector
    get() {
        if (_okRegular != null) return _okRegular!!
        _okRegular = ImageVector.Builder(
            name = "Ok.Glass.Regular",
            defaultWidth = 28.0f.dp,
            defaultHeight = 28.0f.dp,
            viewportWidth = 1333.3833f,
            viewportHeight = 1333.3833f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 95.2417f, translationY = 1238.1417f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(1097.4f, 992.9f),
                        PathNode.LineTo(1069.4f, 1015.9f),
                        PathNode.QuadTo(1060.4f, 1022.9f, 1049.4f, 1021.4f),
                        PathNode.QuadTo(1038.4f, 1019.9f, 1031.4f, 1010.9f),
                        PathNode.LineTo(438.4f, 238.9f),
                        PathNode.QuadTo(436.4f, 235.9f, 433.4f, 235.9f),
                        PathNode.QuadTo(430.4f, 235.9f, 428.4f, 238.9f),
                        PathNode.LineTo(111.4f, 648.9f),
                        PathNode.QuadTo(104.4f, 656.9f, 93.4f, 658.4f),
                        PathNode.QuadTo(82.4f, 659.9f, 73.4f, 652.9f),
                        PathNode.LineTo(45.4f, 629.9f),
                        PathNode.QuadTo(36.4f, 622.9f, 34.9f, 611.9f),
                        PathNode.QuadTo(33.4f, 600.9f, 40.4f, 592.9f),
                        PathNode.LineTo(392.4f, 141.9f),
                        PathNode.QuadTo(402.4f, 128.9f, 416.9f, 123.9f),
                        PathNode.QuadTo(431.4f, 118.9f, 445.9f, 122.9f),
                        PathNode.QuadTo(460.4f, 126.9f, 469.4f, 138.9f),
                        PathNode.LineTo(1102.4f, 954.9f),
                        PathNode.QuadTo(1109.4f, 963.9f, 1107.9f, 974.9f),
                        PathNode.QuadTo(1106.4f, 985.9f, 1097.4f, 992.9f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _okRegular!!
    }

private var _okRegular: ImageVector? = null
