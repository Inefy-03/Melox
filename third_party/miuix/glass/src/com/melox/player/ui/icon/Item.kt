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

internal val GlassIcons.Item: ImageVector
    get() {
        if (_itemSurfaceRegular != null) return _itemSurfaceRegular!!
        _itemSurfaceRegular = buildHyperOSIcon(
            name = "Item.Glass.Regular",
            pathData = listOf(
                PathNode.MoveTo(293f, 564f),
                PathNode.QuadTo(318f, 566f, 381f, 566f),
                PathNode.HorizontalTo(408f),
                PathNode.HorizontalTo(921f),
                PathNode.HorizontalTo(949f),
                PathNode.QuadTo(1012f, 566f, 1037f, 564f),
                PathNode.QuadTo(1059f, 562f, 1078f, 558f),
                PathNode.LineTo(1077f, 568f),
                PathNode.QuadTo(1076f, 575f, 1073.5f, 582.5f),
                PathNode.QuadTo(1071f, 590f, 1068f, 595f),
                PathNode.QuadTo(1061f, 610f, 1048.5f, 622.5f),
                PathNode.QuadTo(1036f, 635f, 1020f, 643f),
                PathNode.QuadTo(1004f, 651f, 980.5f, 653f),
                PathNode.QuadTo(957f, 655f, 903f, 655f),
                PathNode.HorizontalTo(427f),
                PathNode.QuadTo(373f, 655f, 350f, 653f),
                PathNode.QuadTo(327f, 651f, 310f, 643f),
                PathNode.QuadTo(278f, 626f, 261f, 595f),
                PathNode.QuadTo(253f, 578f, 252f, 558f),
                PathNode.QuadTo(271f, 562f, 293f, 564f),
                PathNode.Close,
                PathNode.MoveTo(427f, 721f),
                PathNode.HorizontalTo(903f),
                PathNode.QuadTo(950f, 721f, 969f, 720f),
                PathNode.QuadTo(967f, 748f, 961f, 761f),
                PathNode.QuadTo(949f, 784f, 924f, 797f),
                PathNode.QuadTo(912f, 803f, 893.5f, 805f),
                PathNode.QuadTo(875f, 807f, 837f, 807f),
                PathNode.HorizontalTo(493f),
                PathNode.QuadTo(453f, 807f, 435.5f, 805.5f),
                PathNode.QuadTo(418f, 804f, 406f, 797f),
                PathNode.QuadTo(381f, 784f, 369f, 761f),
                PathNode.QuadTo(363f, 748f, 361f, 720f),
                PathNode.QuadTo(380f, 721f, 427f, 721f),
                PathNode.Close,
                PathNode.MoveTo(1097f, -100f),
                PathNode.QuadTo(1120f, -88f, 1138.5f, -69.5f),
                PathNode.QuadTo(1157f, -51f, 1169f, -27f),
                PathNode.QuadTo(1182f, -2f, 1185f, 32.5f),
                PathNode.QuadTo(1188f, 67f, 1188f, 148f),
                PathNode.VerticalTo(234f),
                PathNode.QuadTo(1188f, 316f, 1185f, 350.5f),
                PathNode.QuadTo(1182f, 385f, 1169f, 409f),
                PathNode.QuadTo(1144f, 458f, 1101f, 480f),
                PathNode.QuadTo(1074f, 494f, 1031.5f, 497f),
                PathNode.QuadTo(989f, 500f, 922f, 500f),
                PathNode.HorizontalTo(408f),
                PathNode.QuadTo(341f, 500f, 298.5f, 497f),
                PathNode.QuadTo(256f, 494f, 229f, 480f),
                PathNode.QuadTo(186f, 458f, 161f, 409f),
                PathNode.QuadTo(148f, 385f, 145f, 350.5f),
                PathNode.QuadTo(142f, 316f, 142f, 234f),
                PathNode.VerticalTo(148f),
                PathNode.QuadTo(142f, 67f, 145f, 32.5f),
                PathNode.QuadTo(148f, -2f, 161f, -27f),
                PathNode.QuadTo(173f, -51f, 191.5f, -69.5f),
                PathNode.QuadTo(210f, -88f, 233f, -100f),
                PathNode.QuadTo(258f, -112f, 292.5f, -115f),
                PathNode.QuadTo(327f, -118f, 408f, -118f),
                PathNode.HorizontalTo(922f),
                PathNode.QuadTo(1003f, -118f, 1037.5f, -115f),
                PathNode.QuadTo(1072f, -112f, 1097f, -100f),
                PathNode.Close,
            ),
            scale = 1f,
            translationX = 1.6667f,
            translationY = 1011.1667f,
            autoMirror = false,
        )
        return _itemSurfaceRegular!!
    }

private var _itemSurfaceRegular: ImageVector? = null
