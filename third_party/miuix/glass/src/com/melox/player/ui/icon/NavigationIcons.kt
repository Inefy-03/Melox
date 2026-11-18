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

// Regular navigation vectors from Miuix 3a980f7c; retain the official 28 dp canvas.
// Music and Contacts preserve Melox's solid paths without the upstream interior cutouts.

internal val GlassIcons.NavigationMusic: ImageVector
    get() {
        if (_navigationMusicRegular != null) return _navigationMusicRegular!!
        _navigationMusicRegular = ImageVector.Builder(
            name = "Music.Glass.Regular",
            defaultWidth = 28.0f.dp,
            defaultHeight = 28.0f.dp,
            viewportWidth = 1333.3833f,
            viewportHeight = 1333.3833f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 95.2417f, translationY = 1238.1417f) {
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
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _navigationMusicRegular!!
    }

private var _navigationMusicRegular: ImageVector? = null

internal val GlassIcons.NavigationAlbum: ImageVector
    get() {
        if (_navigationAlbumRegular != null) return _navigationAlbumRegular!!
        _navigationAlbumRegular = ImageVector.Builder(
            name = "Album.Glass.Regular",
            defaultWidth = 28.0f.dp,
            defaultHeight = 28.0f.dp,
            viewportWidth = 1333.3833f,
            viewportHeight = 1333.3833f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 95.2417f, translationY = 1238.1417f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(920.4f, 90.9f),
                        PathNode.QuadTo(969.4f, 115.9f, 993.4f, 162.9f),
                        PathNode.QuadTo(1005.4f, 187.9f, 1008.4f, 222.4f),
                        PathNode.QuadTo(1011.4f, 256.9f, 1011.4f, 338.9f),
                        PathNode.LineTo(1011.4f, 804.9f),
                        PathNode.QuadTo(1011.4f, 886.9f, 1008.4f, 921.4f),
                        PathNode.QuadTo(1005.4f, 955.9f, 993.4f, 979.9f),
                        PathNode.QuadTo(981.4f, 1002.9f, 962.4f, 1021.9f),
                        PathNode.QuadTo(943.4f, 1040.9f, 920.4f, 1052.9f),
                        PathNode.QuadTo(895.4f, 1064.9f, 860.9f, 1067.9f),
                        PathNode.QuadTo(826.4f, 1070.9f, 745.4f, 1070.9f),
                        PathNode.LineTo(397.4f, 1070.9f),
                        PathNode.QuadTo(316.4f, 1070.9f, 281.9f, 1067.9f),
                        PathNode.QuadTo(247.4f, 1064.9f, 222.4f, 1052.9f),
                        PathNode.QuadTo(199.4f, 1040.9f, 180.4f, 1021.9f),
                        PathNode.QuadTo(161.4f, 1002.9f, 149.4f, 979.9f),
                        PathNode.QuadTo(137.4f, 955.9f, 134.4f, 921.4f),
                        PathNode.QuadTo(131.4f, 886.9f, 131.4f, 804.9f),
                        PathNode.LineTo(131.4f, 338.9f),
                        PathNode.QuadTo(131.4f, 256.9f, 134.4f, 222.4f),
                        PathNode.QuadTo(137.4f, 187.9f, 149.4f, 162.9f),
                        PathNode.QuadTo(173.4f, 115.9f, 222.4f, 90.9f),
                        PathNode.QuadTo(247.4f, 77.9f, 281.9f, 74.9f),
                        PathNode.QuadTo(316.4f, 71.9f, 397.4f, 71.9f),
                        PathNode.LineTo(745.4f, 71.9f),
                        PathNode.QuadTo(826.4f, 71.9f, 860.9f, 74.9f),
                        PathNode.QuadTo(895.4f, 77.9f, 920.4f, 90.9f),
                        PathNode.Close,
                        PathNode.MoveTo(268.4f, 324.9f),
                        PathNode.QuadTo(245.4f, 336.9f, 235.4f, 358.9f),
                        PathNode.QuadTo(229.4f, 369.9f, 227.9f, 385.9f),
                        PathNode.QuadTo(226.4f, 401.9f, 226.4f, 438.9f),
                        PathNode.LineTo(226.4f, 853.9f),
                        PathNode.QuadTo(226.4f, 891.9f, 227.9f, 907.4f),
                        PathNode.QuadTo(229.4f, 922.9f, 235.4f, 933.9f),
                        PathNode.QuadTo(245.4f, 955.9f, 268.4f, 967.9f),
                        PathNode.QuadTo(279.4f, 973.9f, 295.4f, 974.9f),
                        PathNode.QuadTo(311.4f, 975.9f, 348.4f, 975.9f),
                        PathNode.LineTo(794.4f, 975.9f),
                        PathNode.QuadTo(832.4f, 975.9f, 847.9f, 974.9f),
                        PathNode.QuadTo(863.4f, 973.9f, 874.4f, 967.9f),
                        PathNode.QuadTo(897.4f, 955.9f, 907.4f, 933.9f),
                        PathNode.QuadTo(913.4f, 922.9f, 914.9f, 907.4f),
                        PathNode.QuadTo(916.4f, 891.9f, 916.4f, 853.9f),
                        PathNode.LineTo(916.4f, 438.9f),
                        PathNode.QuadTo(916.4f, 401.9f, 914.9f, 385.9f),
                        PathNode.QuadTo(913.4f, 369.9f, 907.4f, 358.9f),
                        PathNode.QuadTo(897.4f, 336.9f, 874.4f, 324.9f),
                        PathNode.QuadTo(863.4f, 318.9f, 847.9f, 317.9f),
                        PathNode.QuadTo(832.4f, 316.9f, 794.4f, 316.9f),
                        PathNode.LineTo(348.4f, 316.9f),
                        PathNode.QuadTo(311.4f, 316.9f, 295.4f, 317.9f),
                        PathNode.QuadTo(279.4f, 318.9f, 268.4f, 324.9f),
                        PathNode.Close,
                        PathNode.MoveTo(520.4f, 375.9f),
                        PathNode.QuadTo(526.4f, 378.9f, 530.4f, 393.4f),
                        PathNode.QuadTo(534.4f, 407.9f, 542.4f, 443.9f),
                        PathNode.LineTo(554.4f, 494.9f),
                        PathNode.QuadTo(561.4f, 522.9f, 569.4f, 531.4f),
                        PathNode.QuadTo(577.4f, 539.9f, 601.4f, 544.9f),
                        PathNode.LineTo(656.4f, 556.9f),
                        PathNode.QuadTo(684.4f, 562.9f, 703.4f, 568.4f),
                        PathNode.QuadTo(722.4f, 573.9f, 725.4f, 578.9f),
                        PathNode.QuadTo(731.4f, 589.9f, 725.4f, 600.9f),
                        PathNode.QuadTo(722.4f, 606.9f, 707.4f, 611.4f),
                        PathNode.QuadTo(692.4f, 615.9f, 656.4f, 622.9f),
                        PathNode.LineTo(605.4f, 634.9f),
                        PathNode.QuadTo(577.4f, 639.9f, 569.4f, 647.9f),
                        PathNode.QuadTo(561.4f, 655.9f, 555.4f, 682.9f),
                        PathNode.LineTo(542.4f, 736.9f),
                        PathNode.LineTo(539.4f, 750.9f),
                        PathNode.QuadTo(534.4f, 772.9f, 529.9f, 787.4f),
                        PathNode.QuadTo(525.4f, 801.9f, 520.4f, 804.9f),
                        PathNode.QuadTo(516.4f, 807.9f, 510.4f, 807.9f),
                        PathNode.QuadTo(504.4f, 807.9f, 499.4f, 804.9f),
                        PathNode.QuadTo(493.4f, 801.9f, 489.4f, 786.9f),
                        PathNode.QuadTo(485.4f, 771.9f, 477.4f, 736.9f),
                        PathNode.LineTo(465.4f, 684.9f),
                        PathNode.QuadTo(460.4f, 664.9f, 455.9f, 656.4f),
                        PathNode.QuadTo(451.4f, 647.9f, 442.9f, 643.4f),
                        PathNode.QuadTo(434.4f, 638.9f, 414.4f, 634.9f),
                        PathNode.LineTo(363.4f, 622.9f),
                        PathNode.QuadTo(327.4f, 614.9f, 312.9f, 610.9f),
                        PathNode.QuadTo(298.4f, 606.9f, 295.4f, 600.9f),
                        PathNode.QuadTo(289.4f, 589.9f, 295.4f, 578.9f),
                        PathNode.QuadTo(298.4f, 574.9f, 313.9f, 569.9f),
                        PathNode.QuadTo(329.4f, 564.9f, 352.4f, 559.9f),
                        PathNode.LineTo(363.4f, 556.9f),
                        PathNode.LineTo(414.4f, 545.9f),
                        PathNode.QuadTo(443.4f, 539.9f, 451.4f, 531.9f),
                        PathNode.QuadTo(459.4f, 523.9f, 465.4f, 494.9f),
                        PathNode.LineTo(477.4f, 443.9f),
                        PathNode.QuadTo(485.4f, 407.9f, 489.4f, 393.4f),
                        PathNode.QuadTo(493.4f, 378.9f, 499.4f, 375.9f),
                        PathNode.QuadTo(504.4f, 372.9f, 510.4f, 372.9f),
                        PathNode.QuadTo(516.4f, 372.9f, 520.4f, 375.9f),
                        PathNode.Close,
                        PathNode.MoveTo(729.4f, 667.9f),
                        PathNode.QuadTo(732.4f, 668.9f, 735.4f, 677.4f),
                        PathNode.QuadTo(738.4f, 685.9f, 741.4f, 698.9f),
                        PathNode.LineTo(752.4f, 736.9f),
                        PathNode.QuadTo(755.4f, 749.9f, 759.9f, 753.9f),
                        PathNode.QuadTo(764.4f, 757.9f, 777.4f, 761.9f),
                        PathNode.LineTo(815.4f, 772.9f),
                        PathNode.QuadTo(827.4f, 775.9f, 835.9f, 778.9f),
                        PathNode.QuadTo(844.4f, 781.9f, 846.4f, 784.9f),
                        PathNode.QuadTo(849.4f, 789.9f, 846.4f, 795.9f),
                        PathNode.QuadTo(843.4f, 798.9f, 837.9f, 800.9f),
                        PathNode.QuadTo(832.4f, 802.9f, 823.4f, 805.9f),
                        PathNode.QuadTo(819.4f, 806.9f, 815.4f, 807.9f),
                        PathNode.LineTo(777.4f, 817.9f),
                        PathNode.QuadTo(765.4f, 820.9f, 760.9f, 825.4f),
                        PathNode.QuadTo(756.4f, 829.9f, 753.4f, 840.9f),
                        PathNode.LineTo(741.4f, 881.9f),
                        PathNode.QuadTo(734.4f, 909.9f, 729.4f, 911.9f),
                        PathNode.QuadTo(727.4f, 913.9f, 723.9f, 913.9f),
                        PathNode.QuadTo(720.4f, 913.9f, 718.4f, 911.9f),
                        PathNode.QuadTo(714.4f, 909.9f, 706.4f, 881.9f),
                        PathNode.LineTo(696.4f, 843.9f),
                        PathNode.QuadTo(693.4f, 831.9f, 688.4f, 826.9f),
                        PathNode.QuadTo(683.4f, 821.9f, 670.4f, 817.9f),
                        PathNode.LineTo(632.4f, 807.9f),
                        PathNode.QuadTo(628.4f, 806.9f, 624.4f, 805.9f),
                        PathNode.QuadTo(615.4f, 802.9f, 609.9f, 800.9f),
                        PathNode.QuadTo(604.4f, 798.9f, 601.4f, 795.9f),
                        PathNode.QuadTo(598.4f, 789.9f, 601.4f, 784.9f),
                        PathNode.QuadTo(603.4f, 781.9f, 609.9f, 779.9f),
                        PathNode.QuadTo(616.4f, 777.9f, 625.4f, 774.9f),
                        PathNode.LineTo(632.4f, 772.9f),
                        PathNode.LineTo(670.4f, 761.9f),
                        PathNode.QuadTo(683.4f, 757.9f, 688.4f, 753.4f),
                        PathNode.QuadTo(693.4f, 748.9f, 696.4f, 736.9f),
                        PathNode.LineTo(706.4f, 698.9f),
                        PathNode.QuadTo(709.4f, 685.9f, 712.4f, 677.4f),
                        PathNode.QuadTo(715.4f, 668.9f, 718.4f, 667.9f),
                        PathNode.QuadTo(720.4f, 665.9f, 723.9f, 665.9f),
                        PathNode.QuadTo(727.4f, 665.9f, 729.4f, 667.9f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _navigationAlbumRegular!!
    }

private var _navigationAlbumRegular: ImageVector? = null

internal val GlassIcons.NavigationContacts: ImageVector
    get() {
        if (_navigationContactsRegular != null) return _navigationContactsRegular!!
        _navigationContactsRegular = ImageVector.Builder(
            name = "Contacts.Glass.Regular",
            defaultWidth = 28.0f.dp,
            defaultHeight = 28.0f.dp,
            viewportWidth = 1333.3833f,
            viewportHeight = 1333.3833f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 95.2417f, translationY = 1238.1417f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(1022.9f, 87.4f),
                        PathNode.QuadTo(1046.9f, 100.4f, 1056.9f, 123.4f),
                        PathNode.QuadTo(1062.9f, 135.4f, 1064.4f, 151.9f),
                        PathNode.QuadTo(1065.9f, 168.4f, 1065.9f, 208.4f),
                        PathNode.QuadTo(1065.9f, 259.4f, 1064.4f, 281.9f),
                        PathNode.QuadTo(1062.9f, 304.4f, 1054.9f, 325.4f),
                        PathNode.QuadTo(1047.9f, 345.4f, 1031.9f, 367.9f),
                        PathNode.QuadTo(1015.9f, 390.4f, 998.9f, 403.4f),
                        PathNode.QuadTo(981.9f, 417.4f, 964.9f, 424.9f),
                        PathNode.QuadTo(947.9f, 432.4f, 911.9f, 444.4f),
                        PathNode.QuadTo(745.9f, 500.4f, 571.9f, 500.4f),
                        PathNode.QuadTo(396.9f, 500.4f, 231.9f, 444.4f),
                        PathNode.QuadTo(194.9f, 432.4f, 177.9f, 424.9f),
                        PathNode.QuadTo(160.9f, 417.4f, 143.9f, 403.4f),
                        PathNode.QuadTo(126.9f, 389.4f, 110.9f, 367.4f),
                        PathNode.QuadTo(94.9f, 345.4f, 87.9f, 325.4f),
                        PathNode.QuadTo(79.9f, 304.4f, 78.4f, 281.9f),
                        PathNode.QuadTo(76.9f, 259.4f, 76.9f, 208.4f),
                        PathNode.QuadTo(76.9f, 168.4f, 78.4f, 151.9f),
                        PathNode.QuadTo(79.9f, 135.4f, 85.9f, 123.4f),
                        PathNode.QuadTo(95.9f, 100.4f, 119.9f, 87.4f),
                        PathNode.QuadTo(131.9f, 81.4f, 148.9f, 79.9f),
                        PathNode.QuadTo(165.9f, 78.4f, 204.9f, 78.4f),
                        PathNode.LineTo(937.9f, 78.4f),
                        PathNode.QuadTo(976.9f, 78.4f, 993.9f, 79.9f),
                        PathNode.QuadTo(1010.9f, 81.4f, 1022.9f, 87.4f),
                        PathNode.Close,
                        PathNode.MoveTo(819.9f, 815.4f),
                        PathNode.QuadTo(819.9f, 882.4f, 786.4f, 939.9f),
                        PathNode.QuadTo(752.9f, 997.4f, 695.9f, 1030.9f),
                        PathNode.QuadTo(638.9f, 1064.4f, 571.9f, 1064.4f),
                        PathNode.QuadTo(503.9f, 1064.4f, 446.9f, 1030.9f),
                        PathNode.QuadTo(389.9f, 997.4f, 356.4f, 939.9f),
                        PathNode.QuadTo(322.9f, 882.4f, 322.9f, 815.4f),
                        PathNode.QuadTo(322.9f, 748.4f, 356.4f, 691.4f),
                        PathNode.QuadTo(389.9f, 634.4f, 446.9f, 600.9f),
                        PathNode.QuadTo(503.9f, 567.4f, 571.9f, 567.4f),
                        PathNode.QuadTo(638.9f, 567.4f, 695.9f, 600.9f),
                        PathNode.QuadTo(752.9f, 634.4f, 786.4f, 691.4f),
                        PathNode.QuadTo(819.9f, 748.4f, 819.9f, 815.4f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _navigationContactsRegular!!
    }

private var _navigationContactsRegular: ImageVector? = null

internal val GlassIcons.NavigationFolderFill: ImageVector
    get() {
        if (_navigationFolderFillRegular != null) return _navigationFolderFillRegular!!
        _navigationFolderFillRegular = ImageVector.Builder(
            name = "FolderFill.Glass.Regular",
            defaultWidth = 28.0f.dp,
            defaultHeight = 28.0f.dp,
            viewportWidth = 1333.3833f,
            viewportHeight = 1333.3833f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 95.2417f, translationY = 1238.1417f) {
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
        return _navigationFolderFillRegular!!
    }

private var _navigationFolderFillRegular: ImageVector? = null
