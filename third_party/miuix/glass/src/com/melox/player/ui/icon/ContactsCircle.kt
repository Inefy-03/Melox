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

internal val GlassIcons.ContactsCircle: ImageVector
    get() {
        if (_contactsCircleRegular != null) return _contactsCircleRegular!!
        _contactsCircleRegular = ImageVector.Builder(
            name = "ContactsCircle.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = false,
        ).apply {
            group(scaleY = -1.0f, translationX = 0.0f, translationY = 1142.9f) {
                addPath(
                    pathData = listOf(
                        PathNode.MoveTo(1070.4f, 570.9f),
                        PathNode.QuadTo(1070.4f, 706.9f, 1003.4f, 821.4f),
                        PathNode.QuadTo(936.4f, 935.9f, 821.4f, 1002.9f),
                        PathNode.QuadTo(706.4f, 1069.9f, 571.4f, 1069.9f),
                        PathNode.QuadTo(436.4f, 1069.9f, 321.4f, 1002.9f),
                        PathNode.QuadTo(206.4f, 935.9f, 139.4f, 821.4f),
                        PathNode.QuadTo(72.4f, 706.9f, 72.4f, 570.9f),
                        PathNode.QuadTo(72.4f, 435.9f, 139.4f, 321.4f),
                        PathNode.QuadTo(206.4f, 206.9f, 321.4f, 139.9f),
                        PathNode.QuadTo(436.4f, 72.9f, 571.4f, 72.9f),
                        PathNode.QuadTo(706.4f, 72.9f, 821.4f, 139.9f),
                        PathNode.QuadTo(936.4f, 206.9f, 1003.4f, 321.4f),
                        PathNode.QuadTo(1070.4f, 435.9f, 1070.4f, 570.9f),
                        PathNode.Close,
                        PathNode.MoveTo(844.4f, 166.9f),
                        PathNode.QuadTo(875.4f, 183.9f, 900.4f, 208.9f),
                        PathNode.QuadTo(925.4f, 233.9f, 941.4f, 264.9f),
                        PathNode.QuadTo(942.4f, 266.9f, 941.9f, 269.9f),
                        PathNode.QuadTo(941.4f, 272.9f, 940.4f, 274.9f),
                        PathNode.QuadTo(865.4f, 383.9f, 770.4f, 444.4f),
                        PathNode.QuadTo(675.4f, 504.9f, 571.4f, 504.9f),
                        PathNode.QuadTo(467.4f, 504.9f, 372.4f, 444.4f),
                        PathNode.QuadTo(277.4f, 383.9f, 202.4f, 274.9f),
                        PathNode.QuadTo(200.4f, 270.9f, 200.4f, 268.9f),
                        PathNode.QuadTo(201.4f, 266.9f, 201.4f, 264.9f),
                        PathNode.QuadTo(234.4f, 200.9f, 298.4f, 166.9f),
                        PathNode.QuadTo(331.4f, 149.9f, 377.9f, 146.4f),
                        PathNode.QuadTo(424.4f, 142.9f, 533.4f, 142.9f),
                        PathNode.LineTo(609.4f, 142.9f),
                        PathNode.QuadTo(718.4f, 142.9f, 764.9f, 146.4f),
                        PathNode.QuadTo(811.4f, 149.9f, 844.4f, 166.9f),
                        PathNode.Close,
                        PathNode.MoveTo(167.4f, 570.9f),
                        PathNode.QuadTo(167.4f, 680.9f, 221.9f, 773.4f),
                        PathNode.QuadTo(276.4f, 865.9f, 368.9f, 920.4f),
                        PathNode.QuadTo(461.4f, 974.9f, 571.4f, 974.9f),
                        PathNode.QuadTo(681.4f, 974.9f, 773.9f, 920.4f),
                        PathNode.QuadTo(866.4f, 865.9f, 920.9f, 773.4f),
                        PathNode.QuadTo(975.4f, 680.9f, 975.4f, 570.9f),
                        PathNode.QuadTo(975.4f, 460.9f, 920.9f, 368.4f),
                        PathNode.QuadTo(866.4f, 275.9f, 773.9f, 221.9f),
                        PathNode.QuadTo(681.4f, 167.9f, 571.4f, 167.9f),
                        PathNode.QuadTo(461.4f, 167.9f, 368.9f, 221.9f),
                        PathNode.QuadTo(276.4f, 275.9f, 221.9f, 368.4f),
                        PathNode.QuadTo(167.4f, 460.9f, 167.4f, 570.9f),
                        PathNode.Close,
                        PathNode.MoveTo(728.4f, 711.9f),
                        PathNode.QuadTo(728.4f, 754.9f, 707.4f, 790.9f),
                        PathNode.QuadTo(686.4f, 826.9f, 650.4f, 847.9f),
                        PathNode.QuadTo(614.4f, 868.9f, 571.4f, 868.9f),
                        PathNode.QuadTo(528.4f, 868.9f, 492.4f, 847.9f),
                        PathNode.QuadTo(456.4f, 826.9f, 435.4f, 790.9f),
                        PathNode.QuadTo(414.4f, 754.9f, 414.4f, 711.9f),
                        PathNode.QuadTo(414.4f, 669.9f, 435.4f, 633.4f),
                        PathNode.QuadTo(456.4f, 596.9f, 492.4f, 575.9f),
                        PathNode.QuadTo(528.4f, 554.9f, 571.4f, 554.9f),
                        PathNode.QuadTo(614.4f, 554.9f, 650.4f, 575.9f),
                        PathNode.QuadTo(686.4f, 596.9f, 707.4f, 633.4f),
                        PathNode.QuadTo(728.4f, 669.9f, 728.4f, 711.9f),
                        PathNode.Close,
                    ),
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    pathFillType = PathFillType.NonZero,
                )
            }
        }.build()
        return _contactsCircleRegular!!
    }

private var _contactsCircleRegular: ImageVector? = null
