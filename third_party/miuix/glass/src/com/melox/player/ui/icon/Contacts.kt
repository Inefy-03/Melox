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
        return _contactsRegular!!
    }

private var _contactsRegular: ImageVector? = null
