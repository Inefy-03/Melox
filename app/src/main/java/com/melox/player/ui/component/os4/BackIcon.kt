// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.melox.player.ui.component.os4

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.icon.MiuixIcons

import androidx.compose.runtime.Composable
import com.melox.player.ui.theme.LocalOs4Style
import top.yukonga.miuix.kmp.icon.extended.Back

@Composable
internal fun navigationBackIcon(): ImageVector =
    if (LocalOs4Style.current) glassChevronBackward else MiuixIcons.Back

// Regular ChevronBackward from the pinned upstream miuix-glass-icons source.
private val glassChevronBackward: ImageVector
    get() {
        if (_chevronBackwardRegular != null) return _chevronBackwardRegular!!
        _chevronBackwardRegular = ImageVector.Builder(
            name = "ChevronBackward.Glass.Regular",
            defaultWidth = 24.0f.dp,
            defaultHeight = 24.0f.dp,
            viewportWidth = 1142.9f,
            viewportHeight = 1142.9f,
            autoMirror = true,
        ).apply {
            group(scaleY = -1.0f, translationX = -85.7f, translationY = 1142.9f) {
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
