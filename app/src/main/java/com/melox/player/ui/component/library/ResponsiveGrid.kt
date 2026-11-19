package com.melox.player.ui.component.library

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal fun PaddingValues.gridViewportWidth(maxWidth: Dp, direction: LayoutDirection): Dp =
    maxWidth - calculateStartPadding(direction) - calculateEndPadding(direction)

internal val PlaylistGridHorizontalPadding = 16.dp

internal fun playlistGridColumnCount(landscape: Boolean, availableWidth: Dp): Int =
    responsiveGridColumnCount(
        availableWidth = availableWidth,
        horizontalPadding = PlaylistGridHorizontalPadding * 2,
        minimumCellWidth = 160.dp,
        minimumColumns = 2,
        maximumColumns = if (landscape) 6 else 3,
    )

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
