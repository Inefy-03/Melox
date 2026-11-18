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

internal val GlassIcons.RecentTasks: ImageVector
    get() {
        if (_recentTasksSurfaceRegular != null) return _recentTasksSurfaceRegular!!
        _recentTasksSurfaceRegular = buildHyperOSIcon(
            name = "RecentTasks.Glass.Regular",
            pathData = listOf(
                PathNode.MoveTo(267f, 852f),
                PathNode.QuadTo(221f, 827f, 196f, 781f),
                PathNode.QuadTo(184f, 757f, 181f, 723f),
                PathNode.QuadTo(178f, 689f, 178f, 609f),
                PathNode.VerticalTo(141f),
                PathNode.QuadTo(178f, 61f, 181f, 27f),
                PathNode.QuadTo(184f, -7f, 196f, -31f),
                PathNode.QuadTo(221f, -77f, 267f, -102f),
                PathNode.QuadTo(291f, -114f, 325f, -117f),
                PathNode.QuadTo(359f, -120f, 439f, -120f),
                PathNode.HorizontalTo(560f),
                PathNode.QuadTo(640f, -120f, 674f, -117f),
                PathNode.QuadTo(708f, -114f, 732f, -102f),
                PathNode.QuadTo(778f, -77f, 803f, -31f),
                PathNode.QuadTo(815f, -7f, 818f, 27f),
                PathNode.QuadTo(821f, 61f, 821f, 141f),
                PathNode.VerticalTo(609f),
                PathNode.QuadTo(821f, 689f, 818f, 723f),
                PathNode.QuadTo(815f, 757f, 803f, 781f),
                PathNode.QuadTo(778f, 827f, 732f, 852f),
                PathNode.QuadTo(708f, 864f, 674f, 867f),
                PathNode.QuadTo(640f, 870f, 560f, 870f),
                PathNode.HorizontalTo(439f),
                PathNode.QuadTo(359f, 870f, 325f, 867f),
                PathNode.QuadTo(291f, 864f, 267f, 852f),
                PathNode.Close,
                PathNode.MoveTo(885f, 721f),
                PathNode.QuadTo(887f, 692f, 887f, 612f),
                PathNode.VerticalTo(138f),
                PathNode.QuadTo(887f, 58f, 885f, 29f),
                PathNode.QuadTo(883f, 7f, 879f, -12f),
                PathNode.HorizontalTo(950f),
                PathNode.QuadTo(1012f, -12f, 1038f, -10f),
                PathNode.QuadTo(1064f, -8f, 1083f, 2f),
                PathNode.QuadTo(1120f, 21f, 1138f, 57f),
                PathNode.QuadTo(1148f, 76f, 1150f, 102f),
                PathNode.QuadTo(1152f, 128f, 1152f, 190f),
                PathNode.VerticalTo(560f),
                PathNode.QuadTo(1152f, 622f, 1150f, 648f),
                PathNode.QuadTo(1148f, 674f, 1138f, 693f),
                PathNode.QuadTo(1120f, 729f, 1083f, 748f),
                PathNode.QuadTo(1064f, 758f, 1038f, 760f),
                PathNode.QuadTo(1012f, 762f, 950f, 762f),
                PathNode.HorizontalTo(879f),
                PathNode.QuadTo(881f, 752f, 882.5f, 742f),
                PathNode.QuadTo(884f, 732f, 885f, 721f),
                PathNode.Close,
            ),
            scale = 1f,
            translationX = 1.6667f,
            translationY = 1041.6667f,
            autoMirror = false,
        )
        return _recentTasksSurfaceRegular!!
    }

private var _recentTasksSurfaceRegular: ImageVector? = null
