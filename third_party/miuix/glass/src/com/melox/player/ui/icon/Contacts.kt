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

internal val GlassIcons.Contacts: ImageVector
    get() {
        if (_contactsRegular != null) return _contactsRegular!!
        _contactsRegular = ImageVector.Builder(
            name = "Contacts.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
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
                        PathNode.MoveTo(161.9f, 194.4f),
                        PathNode.LineTo(161.9f, 218.4f),
                        PathNode.QuadTo(161.9f, 250.4f, 162.9f, 264.9f),
                        PathNode.QuadTo(163.9f, 279.4f, 168.9f, 292.4f),
                        PathNode.QuadTo(173.9f, 303.4f, 181.4f, 314.9f),
                        PathNode.QuadTo(188.9f, 326.4f, 197.9f, 333.4f),
                        PathNode.QuadTo(208.9f, 342.4f, 220.9f, 348.4f),
                        PathNode.QuadTo(232.9f, 354.4f, 255.9f, 361.4f),
                        PathNode.QuadTo(400.9f, 415.4f, 561.9f, 415.4f),
                        PathNode.QuadTo(648.9f, 415.4f, 731.9f, 399.4f),
                        PathNode.QuadTo(814.9f, 383.4f, 890.9f, 353.4f),
                        PathNode.LineTo(900.9f, 349.4f),
                        PathNode.QuadTo(932.9f, 336.4f, 945.9f, 324.4f),
                        PathNode.QuadTo(965.9f, 306.4f, 973.9f, 282.4f),
                        PathNode.QuadTo(978.9f, 270.4f, 979.9f, 256.4f),
                        PathNode.QuadTo(980.9f, 242.4f, 980.9f, 211.4f),
                        PathNode.LineTo(980.9f, 194.4f),
                        PathNode.QuadTo(980.9f, 174.4f, 975.4f, 169.4f),
                        PathNode.QuadTo(969.9f, 164.4f, 949.9f, 164.4f),
                        PathNode.LineTo(192.9f, 164.4f),
                        PathNode.QuadTo(172.9f, 164.4f, 167.4f, 169.4f),
                        PathNode.QuadTo(161.9f, 174.4f, 161.9f, 194.4f),
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
                        PathNode.MoveTo(407.9f, 815.4f),
                        PathNode.QuadTo(407.9f, 859.4f, 429.9f, 896.9f),
                        PathNode.QuadTo(451.9f, 934.4f, 489.9f, 956.4f),
                        PathNode.QuadTo(527.9f, 978.4f, 571.9f, 978.4f),
                        PathNode.QuadTo(615.9f, 978.4f, 653.4f, 956.4f),
                        PathNode.QuadTo(690.9f, 934.4f, 712.9f, 896.9f),
                        PathNode.QuadTo(734.9f, 859.4f, 734.9f, 815.4f),
                        PathNode.QuadTo(734.9f, 771.4f, 712.9f, 733.9f),
                        PathNode.QuadTo(690.9f, 696.4f, 653.4f, 674.4f),
                        PathNode.QuadTo(615.9f, 652.4f, 571.9f, 652.4f),
                        PathNode.QuadTo(527.9f, 652.4f, 489.9f, 674.4f),
                        PathNode.QuadTo(451.9f, 696.4f, 429.9f, 733.9f),
                        PathNode.QuadTo(407.9f, 771.4f, 407.9f, 815.4f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _contactsRegular!!
    }

private var _contactsRegular: ImageVector? = null
