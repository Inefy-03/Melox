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

internal val GlassIcons.HomeFill: ImageVector
    get() {
        if (_homeScreenSurfaceRegular != null) return _homeScreenSurfaceRegular!!
        _homeScreenSurfaceRegular = buildHyperOSIcon(
            name = "HomeFill.Glass.Regular",
            pathData = listOf(
                PathNode.MoveTo(1051f, 563f),
                PathNode.LineTo(751f, 815f),
                PathNode.QuadTo(708f, 851f, 665f, 851f),
                PathNode.QuadTo(622f, 851f, 579f, 815f),
                PathNode.LineTo(279f, 563f),
                PathNode.QuadTo(240f, 530f, 222.5f, 492.5f),
                PathNode.QuadTo(205f, 455f, 205f, 404f),
                PathNode.VerticalTo(29f),
                PathNode.QuadTo(205f, -34f, 238.5f, -67.5f),
                PathNode.QuadTo(272f, -101f, 335f, -101f),
                PathNode.HorizontalTo(473f),
                PathNode.QuadTo(499f, -101f, 513.5f, -87f),
                PathNode.QuadTo(528f, -73f, 528f, -46f),
                PathNode.VerticalTo(156f),
                PathNode.QuadTo(528f, 207f, 555f, 233.5f),
                PathNode.QuadTo(582f, 260f, 632f, 260f),
                PathNode.HorizontalTo(698f),
                PathNode.QuadTo(748f, 260f, 775f, 233.5f),
                PathNode.QuadTo(802f, 207f, 802f, 156f),
                PathNode.VerticalTo(-46f),
                PathNode.QuadTo(802f, -73f, 816.5f, -87f),
                PathNode.QuadTo(831f, -101f, 857f, -101f),
                PathNode.HorizontalTo(995f),
                PathNode.QuadTo(1058f, -101f, 1091.5f, -68f),
                PathNode.QuadTo(1125f, -35f, 1125f, 29f),
                PathNode.VerticalTo(404f),
                PathNode.QuadTo(1125f, 455f, 1107.5f, 492.5f),
                PathNode.QuadTo(1090f, 530f, 1051f, 563f),
                PathNode.Close,
            ),
            scale = 1f,
            translationX = 1.6667f,
            translationY = 1041.6667f,
            autoMirror = false,
        )
        return _homeScreenSurfaceRegular!!
    }

private var _homeScreenSurfaceRegular: ImageVector? = null
