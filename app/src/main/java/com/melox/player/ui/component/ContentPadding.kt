package com.melox.player.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

internal fun PaddingValues.adjustPadding(
    start: (Dp) -> Dp = { it },
    top: (Dp) -> Dp = { it },
    end: (Dp) -> Dp = { it },
    bottom: (Dp) -> Dp = { it },
): PaddingValues = object : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp {
        val left = this@adjustPadding.calculateLeftPadding(layoutDirection)
        return if (layoutDirection == LayoutDirection.Ltr) start(left) else end(left)
    }

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp {
        val right = this@adjustPadding.calculateRightPadding(layoutDirection)
        return if (layoutDirection == LayoutDirection.Ltr) end(right) else start(right)
    }

    override fun calculateTopPadding(): Dp = top(this@adjustPadding.calculateTopPadding())

    override fun calculateBottomPadding(): Dp = bottom(this@adjustPadding.calculateBottomPadding())
}
