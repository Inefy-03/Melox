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

internal val GlassIcons.Wallpaper: ImageVector
    get() {
        if (_wallpaperSurfaceRegular != null) return _wallpaperSurfaceRegular!!
        _wallpaperSurfaceRegular = buildHyperOSIcon(
            name = "Wallpaper.Glass.Regular",
            pathData = listOf(
                PathNode.MoveTo(220f, 638f),
                PathNode.VerticalTo(348f),
                PathNode.QuadTo(220f, 227f, 280f, 125.5f),
                PathNode.QuadTo(340f, 24f, 442f, -36f),
                PathNode.QuadTo(544f, -96f, 665f, -96f),
                PathNode.QuadTo(713f, -96f, 762f, -85f),
                PathNode.QuadTo(802f, 15f, 802f, 126f),
                PathNode.QuadTo(802f, 272f, 734.5f, 397.5f),
                PathNode.QuadTo(667f, 523f, 550f, 604f),
                PathNode.QuadTo(433f, 685f, 290f, 702f),
                PathNode.QuadTo(262f, 705f, 241f, 686f),
                PathNode.QuadTo(220f, 667f, 220f, 638f),
                PathNode.Close,
                PathNode.MoveTo(713f, 547f),
                PathNode.QuadTo(788f, 460f, 829.5f, 351f),
                PathNode.QuadTo(871f, 242f, 871f, 123f),
                PathNode.QuadTo(871f, 31f, 844f, -61f),
                PathNode.QuadTo(922f, -27f, 982.5f, 34f),
                PathNode.QuadTo(1043f, 95f, 1076.5f, 175.5f),
                PathNode.QuadTo(1110f, 256f, 1110f, 345f),
                PathNode.VerticalTo(636f),
                PathNode.QuadTo(1110f, 664f, 1089f, 683f),
                PathNode.QuadTo(1068f, 702f, 1040f, 699f),
                PathNode.QuadTo(947f, 688f, 863.5f, 648.5f),
                PathNode.QuadTo(780f, 609f, 713f, 547f),
                PathNode.Close,
                PathNode.MoveTo(559f, 778f),
                PathNode.LineTo(533f, 753f),
                PathNode.QuadTo(521f, 742f, 523f, 726f),
                PathNode.QuadTo(525f, 710f, 539f, 702f),
                PathNode.QuadTo(608f, 663f, 664f, 610f),
                PathNode.QuadTo(725f, 666f, 792f, 704f),
                PathNode.QuadTo(806f, 712f, 808.5f, 728f),
                PathNode.QuadTo(811f, 744f, 799f, 755f),
                PathNode.LineTo(791f, 763f),
                PathNode.QuadTo(734f, 816f, 704f, 835f),
                PathNode.QuadTo(687f, 846f, 666.5f, 846f),
                PathNode.QuadTo(646f, 846f, 629f, 835f),
                PathNode.QuadTo(599f, 816f, 559f, 778f),
                PathNode.Close,
            ),
            scale = 1f,
            translationX = 1.6667f,
            translationY = 1041.6667f,
            autoMirror = false,
        )
        return _wallpaperSurfaceRegular!!
    }

private var _wallpaperSurfaceRegular: ImageVector? = null
