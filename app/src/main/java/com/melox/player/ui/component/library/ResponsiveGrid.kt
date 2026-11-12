package com.melox.player.ui.component.library

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal fun responsiveGridColumnCount(
    availableWidth: Dp,
    horizontalPadding: Dp,
    minimumCellWidth: Dp,
    minimumColumns: Int,
    maximumColumns: Int,
): Int {
    val contentWidth = (availableWidth - horizontalPadding).coerceAtLeast(0.dp)
    val measuredColumns = ((contentWidth + GridSpacing) / (minimumCellWidth + GridSpacing))
        .toInt()
    return measuredColumns.coerceIn(minimumColumns, maximumColumns)
}

private val GridSpacing = 12.dp

internal fun isCompactLandscapeGrid(windowWidth: Dp, windowHeight: Dp): Boolean =
    windowWidth > windowHeight && windowHeight > 0.dp && windowHeight < 600.dp
