// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.melox.player.ui.icon

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode

internal val GlassIcons.Remove: ImageVector
    get() {
        if (_removeRegular != null) return _removeRegular!!
        _removeRegular = buildHyperOSIcon(
            name = "Remove.Glass.Regular",
            pathData = listOf(
                PathNode.MoveTo(902f, 418f),
                PathNode.HorizontalTo(428f),
                PathNode.QuadTo(398f, 418f, 398f, 388f),
                PathNode.VerticalTo(362f),
                PathNode.QuadTo(398f, 332f, 428f, 332f),
                PathNode.HorizontalTo(902f),
                PathNode.QuadTo(916f, 332f, 924f, 340f),
                PathNode.QuadTo(932f, 348f, 932f, 362f),
                PathNode.VerticalTo(388f),
                PathNode.QuadTo(932f, 418f, 902f, 418f),
                PathNode.Close,
                PathNode.MoveTo(154f, 375f),
                PathNode.QuadTo(154f, 236f, 223f, 119f),
                PathNode.QuadTo(292f, 2f, 409f, -67f),
                PathNode.QuadTo(526f, -136f, 665f, -136f),
                PathNode.QuadTo(804f, -136f, 921f, -67f),
                PathNode.QuadTo(1038f, 2f, 1107f, 119f),
                PathNode.QuadTo(1176f, 236f, 1176f, 375f),
                PathNode.QuadTo(1176f, 514f, 1107f, 631f),
                PathNode.QuadTo(1038f, 748f, 921f, 817f),
                PathNode.QuadTo(804f, 886f, 665f, 886f),
                PathNode.QuadTo(526f, 886f, 409f, 817f),
                PathNode.QuadTo(292f, 748f, 223f, 631f),
                PathNode.QuadTo(154f, 514f, 154f, 375f),
                PathNode.Close,
                PathNode.MoveTo(1090f, 375f),
                PathNode.QuadTo(1090f, 259f, 1033f, 161.5f),
                PathNode.QuadTo(976f, 64f, 878.5f, 7f),
                PathNode.QuadTo(781f, -50f, 665f, -50f),
                PathNode.QuadTo(549f, -50f, 451.5f, 7f),
                PathNode.QuadTo(354f, 64f, 297f, 161.5f),
                PathNode.QuadTo(240f, 259f, 240f, 375f),
                PathNode.QuadTo(240f, 491f, 297f, 588.5f),
                PathNode.QuadTo(354f, 686f, 451.5f, 743f),
                PathNode.QuadTo(549f, 800f, 665f, 800f),
                PathNode.QuadTo(781f, 800f, 878.5f, 743f),
                PathNode.QuadTo(976f, 686f, 1033f, 588.5f),
                PathNode.QuadTo(1090f, 491f, 1090f, 375f),
                PathNode.Close,
            ),
            scale = 1f,
            translationX = 1.6667f,
            translationY = 1041.6667f,
            autoMirror = false,
        )
        return _removeRegular!!
    }

private var _removeRegular: ImageVector? = null
