package com.melox.player.ui.screen.playback

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melox.player.ui.component.library.PLAYBACK_ARTWORK_SHADOW_BLUR_RADIUS
import com.melox.player.ui.isMiuixWideLayout

internal data class PlayerVerticalSpacing(
    val artworkToProgress: Dp = PLAYER_ARTWORK_PROGRESS_SPACING,
    val progressToPrimary: Dp = PLAYER_PROGRESS_TO_PRIMARY_SPACING,
    val controlGroup: Dp = PLAYER_CONTROL_GROUP_SPACING,
    val headerToArtwork: Dp = PLAYER_HEADER_TO_CONTENT_SPACING,
    val panelBottom: Dp = PLAYER_CONTROL_ROW_BOTTOM_NO_SAFE_AREA_SPACING,
)

internal data class PlayerPrimaryControlLayout(
    val previousIconOffset: Dp,
    val playPauseIconOffset: Dp,
    val nextIconOffset: Dp,
    val previousTouchOffset: Dp,
    val playPauseTouchOffset: Dp,
    val nextTouchOffset: Dp,
)

internal fun playerPrimaryControlLayout(containerWidth: Dp): PlayerPrimaryControlLayout {
    val equalVisualGap = (
        containerWidth - PLAYER_SIDE_CONTROL_ICON_SIZE * 2f - PLAYER_PLAY_PAUSE_ICON_SIZE
    ).coerceAtLeast(0.dp) / 4f
    val minimumVisualGapForTouch = PLAYER_PRIMARY_CONTROL_MIN_TOUCH_GAP +
        (PLAYER_SIDE_CONTROL_TOUCH_SIZE - PLAYER_SIDE_CONTROL_ICON_SIZE) / 2f +
        (PLAYER_PLAY_PAUSE_TOUCH_SIZE - PLAYER_PLAY_PAUSE_ICON_SIZE) / 2f
    val previousTouchOffset: Dp
    val playPauseTouchOffset: Dp
    val nextTouchOffset: Dp
    if (equalVisualGap >= minimumVisualGapForTouch) {
        previousTouchOffset = equalVisualGap -
            (PLAYER_SIDE_CONTROL_TOUCH_SIZE - PLAYER_SIDE_CONTROL_ICON_SIZE) / 2f
        playPauseTouchOffset = equalVisualGap * 2f + PLAYER_SIDE_CONTROL_ICON_SIZE -
            (PLAYER_PLAY_PAUSE_TOUCH_SIZE - PLAYER_PLAY_PAUSE_ICON_SIZE) / 2f
        nextTouchOffset = equalVisualGap * 3f +
            PLAYER_SIDE_CONTROL_ICON_SIZE + PLAYER_PLAY_PAUSE_ICON_SIZE -
            (PLAYER_SIDE_CONTROL_TOUCH_SIZE - PLAYER_SIDE_CONTROL_ICON_SIZE) / 2f
    } else {
        val touchGroupWidth = PLAYER_SIDE_CONTROL_TOUCH_SIZE * 2f +
            PLAYER_PLAY_PAUSE_TOUCH_SIZE + PLAYER_PRIMARY_CONTROL_MIN_TOUCH_GAP * 2f
        previousTouchOffset = ((containerWidth - touchGroupWidth) / 2f).coerceAtLeast(0.dp)
        playPauseTouchOffset = previousTouchOffset +
            PLAYER_SIDE_CONTROL_TOUCH_SIZE + PLAYER_PRIMARY_CONTROL_MIN_TOUCH_GAP
        nextTouchOffset = playPauseTouchOffset +
            PLAYER_PLAY_PAUSE_TOUCH_SIZE + PLAYER_PRIMARY_CONTROL_MIN_TOUCH_GAP
    }
    return PlayerPrimaryControlLayout(
        previousIconOffset = previousTouchOffset +
            (PLAYER_SIDE_CONTROL_TOUCH_SIZE - PLAYER_SIDE_CONTROL_ICON_SIZE) / 2f,
        playPauseIconOffset = playPauseTouchOffset +
            (PLAYER_PLAY_PAUSE_TOUCH_SIZE - PLAYER_PLAY_PAUSE_ICON_SIZE) / 2f,
        nextIconOffset = nextTouchOffset +
            (PLAYER_SIDE_CONTROL_TOUCH_SIZE - PLAYER_SIDE_CONTROL_ICON_SIZE) / 2f,
        previousTouchOffset = previousTouchOffset,
        playPauseTouchOffset = playPauseTouchOffset,
        nextTouchOffset = nextTouchOffset,
    )
}

internal enum class PlayerLayout {
    PORTRAIT,
    COMPACT_LANDSCAPE,
    WIDE_TWO_PANE,
}

internal fun playerLayout(windowWidth: Dp, windowHeight: Dp): PlayerLayout = when {
    windowWidth <= 0.dp || windowHeight <= 0.dp -> PlayerLayout.PORTRAIT
    isMiuixWideLayout(windowWidth, windowHeight) && (
        windowWidth < windowHeight || windowHeight >= PLAYER_WIDE_LANDSCAPE_MIN_HEIGHT
    ) -> PlayerLayout.WIDE_TWO_PANE
    windowWidth < windowHeight -> PlayerLayout.PORTRAIT
    else -> PlayerLayout.COMPACT_LANDSCAPE
}

internal fun playerLyricsAreActive(
    layout: PlayerLayout,
    currentPage: Int,
    targetPage: Int,
): Boolean = layout == PlayerLayout.WIDE_TWO_PANE || currentPage == 1 || targetPage == 1

internal fun usesWidePlayerLayout(windowWidth: Dp, windowHeight: Dp): Boolean =
    playerLayout(windowWidth, windowHeight) == PlayerLayout.WIDE_TWO_PANE

internal fun playerHidesSystemNavigation(layout: PlayerLayout, fullPlayerVisible: Boolean): Boolean =
    fullPlayerVisible && layout == PlayerLayout.COMPACT_LANDSCAPE

internal const val COMPACT_LANDSCAPE_ARTWORK_WEIGHT = 0.45f
internal val COMPACT_LANDSCAPE_LYRICS_EXTENSION = 12.dp

internal fun compactLandscapeLyricsToggleStart(paneWidth: Dp, buttonWidth: Dp): Dp {
    val gap = ((paneWidth - buttonWidth * 5f) / 6f).coerceAtLeast(0.dp)
    return (paneWidth - gap - buttonWidth).coerceAtLeast(0.dp)
}

internal fun compactLandscapeArtworkPaneWidth(availableWidth: Dp): Dp =
    availableWidth.coerceAtLeast(0.dp) * COMPACT_LANDSCAPE_ARTWORK_WEIGHT

internal fun compactLandscapeHeaderTop(
    availableHeight: Dp,
    headerHeight: Dp,
    controlsHeight: Dp,
): Dp = ((availableHeight - headerHeight -
    COMPACT_LANDSCAPE_HEADER_TO_CONTROLS_SPACING - controlsHeight) / 2f).coerceAtLeast(0.dp)

internal fun compactLandscapeArtworkSize(
    paneWidth: Dp,
    availableHeight: Dp,
): Dp = minOf(paneWidth, availableHeight).coerceAtLeast(0.dp)

internal fun compactLandscapePaneContentWidth(paneWidth: Dp): Dp =
    playerContentWidth(paneWidth, PLAYER_PORTRAIT_CONTENT_MAX_WIDTH)

internal fun compactLandscapeSidePadding(
    ownSafeInset: Dp,
    oppositeSafeInset: Dp,
): Dp = (oppositeSafeInset - ownSafeInset).coerceAtLeast(0.dp)

internal fun playerHeaderTopPadding(layout: PlayerLayout, safeTop: Dp): Dp =
    safeTop.coerceAtLeast(0.dp) + when (layout) {
        PlayerLayout.PORTRAIT -> PLAYER_HEADER_TOP_PADDING
        PlayerLayout.COMPACT_LANDSCAPE -> 0.dp
        PlayerLayout.WIDE_TWO_PANE -> PLAYER_LANDSCAPE_VERTICAL_PADDING
    }

internal fun hiddenLyricsCenterOffsetY(
    pageHeight: Dp,
    headerTopPadding: Dp,
    headerContentHeight: Dp,
    headerSpacing: Dp,
): Dp {
    val headerHeight = headerTopPadding + headerContentHeight + headerSpacing
    return pageHeight * 0.4f - (pageHeight + headerHeight).div(2f)
}

internal fun landscapePlayerDesignContentWidth(availableWidth: Dp): Dp =
    (availableWidth - PLAYER_WIDE_FIXED_START_INSET).coerceAtLeast(0.dp)

internal fun landscapePlayerPlaybackPaneWidth(availableWidth: Dp): Dp = (
    landscapePlayerDesignContentWidth(availableWidth) -
        PLAYER_WIDE_LYRICS_PANE_WIDTH_EXPANSION - PLAYER_WIDE_PANE_SPACING_MAX
).coerceAtLeast(0.dp)
    .div(2f)
    .coerceAtMost(PLAYER_WIDE_PLAYBACK_PANE_MAX_WIDTH)

internal fun landscapePlayerLyricsPaneWidth(availableWidth: Dp): Dp {
    val playbackWidth = landscapePlayerPlaybackPaneWidth(availableWidth)
    val designWidth = landscapePlayerDesignContentWidth(availableWidth)
    // Preserve the narrower-screen split; lyrics can keep growing after playback reaches its cap.
    return maxOf(
        playbackWidth + PLAYER_WIDE_LYRICS_PANE_WIDTH_EXPANSION,
        designWidth - playbackWidth - PLAYER_WIDE_PANE_SPACING_MAX,
    ).coerceAtMost(PLAYER_WIDE_LYRICS_PANE_MAX_WIDTH).coerceAtMost(designWidth)
}

internal fun landscapePlayerPaneSpacing(availableWidth: Dp): Dp {
    val compressionFraction = (
        (PLAYER_WIDE_PREFERRED_GROUP_WIDTH - availableWidth).value /
            (PLAYER_WIDE_PREFERRED_GROUP_WIDTH -
                PLAYER_WIDE_PANE_SPACING_COMPRESSION_END_WIDTH).value
        ).coerceIn(0f, 1f)
    return PLAYER_WIDE_PANE_SPACING_MAX -
        (PLAYER_WIDE_PANE_SPACING_MAX - PLAYER_WIDE_PANE_SPACING_MIN) * compressionFraction
}

internal fun landscapePlayerLyricsContentWidth(lyricsPaneWidth: Dp): Dp =
    (lyricsPaneWidth - PLAYER_WIDE_LYRICS_CONTENT_SIDE_INSET * 2f)
        .coerceAtLeast(0.dp)

internal fun landscapePlayerArtworkSize(availableWidth: Dp, availableHeight: Dp): Dp =
    minOf(
        playerUnboundedContentWidth(availableWidth),
        availableHeight - LANDSCAPE_PLAYER_ARTWORK_VERTICAL_INSET * 2 -
            PLAYER_ARTWORK_VERTICAL_FOOTPRINT_EXPANSION,
    ).coerceAtLeast(0.dp)

internal fun landscapePlayerArtworkAlignmentSize(availableWidth: Dp): Dp =
    playerUnboundedContentWidth(availableWidth)

internal fun playerArtworkSizeForContent(availableSize: Dp): Dp =
    (availableSize - PLAYER_ARTWORK_HORIZONTAL_REDUCTION).coerceAtLeast(0.dp)

internal fun playerArtworkContentSize(size: Dp, padding: Dp): Dp =
    (size + PLAYER_ARTWORK_CONTAINER_EXPANSION - padding * 2f).coerceAtLeast(0.dp)

internal fun playerArtworkAlignmentContentSize(size: Dp): Dp =
    playerArtworkContentSize(size, PLAYER_ARTWORK_PLAYING_PADDING)

internal val PLAYER_PORTRAIT_CONTENT_MAX_WIDTH = 560.dp

internal fun playerContentWidth(availableWidth: Dp, maximumWidth: Dp): Dp =
    (availableWidth - 56.dp).coerceIn(0.dp, maximumWidth)

internal fun playerUnboundedContentWidth(availableWidth: Dp): Dp =
    (availableWidth - 56.dp).coerceAtLeast(0.dp)

internal fun playerVerticalSpacing(
    availableHeight: Dp,
    preferredArtworkSize: Dp,
    panelBottom: Dp = PLAYER_CONTROL_ROW_BOTTOM_NO_SAFE_AREA_SPACING,
): PlayerVerticalSpacing = PlayerVerticalSpacing(
    artworkToProgress = PLAYER_ARTWORK_PROGRESS_SPACING,
    progressToPrimary = PLAYER_PROGRESS_TO_PRIMARY_SPACING,
    controlGroup = PLAYER_CONTROL_GROUP_SPACING,
    headerToArtwork = PLAYER_HEADER_TO_CONTENT_SPACING,
    panelBottom = panelBottom,
)

internal fun playerControlsBottomPadding(safeBottom: Dp): Dp {
    val bottomSafeArea = safeBottom.coerceAtLeast(0.dp)
    return if (bottomSafeArea > 0.dp) {
        bottomSafeArea + PLAYER_CONTROL_ROW_BOTTOM_SAFE_AREA_SPACING
    } else {
        PLAYER_CONTROL_ROW_BOTTOM_NO_SAFE_AREA_SPACING
    }
}

// Landscape uses its dedicated cover/content relationship and 8 dp artwork gaps.
internal fun landscapePlayerSpacing(): PlayerVerticalSpacing = PlayerVerticalSpacing(
    artworkToProgress = LANDSCAPE_PLAYER_ARTWORK_TO_PROGRESS_SPACING,
    progressToPrimary = PLAYER_PROGRESS_TO_PRIMARY_SPACING,
    controlGroup = PLAYER_CONTROL_GROUP_SPACING,
    headerToArtwork = LANDSCAPE_PLAYER_TITLE_TO_ARTWORK_SPACING,
    panelBottom = PLAYER_WIDE_PANEL_BOTTOM_SPACING,
)

internal fun compactLandscapePlayerSpacing(): PlayerVerticalSpacing = PlayerVerticalSpacing(
    artworkToProgress = PLAYER_ARTWORK_PROGRESS_SPACING,
    progressToPrimary = PLAYER_PROGRESS_TO_PRIMARY_SPACING,
    controlGroup = PLAYER_CONTROL_GROUP_SPACING,
    headerToArtwork = PLAYER_HEADER_TO_CONTENT_SPACING,
    panelBottom = 0.dp,
)

internal fun fitPlayerArtworkSize(preferredSize: Dp, availableHeight: Dp): Dp =
    minOf(
        preferredSize,
        availableHeight - PLAYER_ARTWORK_VERTICAL_FOOTPRINT_EXPANSION,
    ).coerceAtLeast(0.dp)

// 歌词设置区域：最小歌词字号缩放（16 sp / 24 sp）。
internal const val MIN_LYRIC_FONT_SCALE = 0.6666667f
// 歌词设置区域：默认歌词字号缩放（100%）。
internal const val DEFAULT_LYRIC_FONT_SCALE = 1f
// 歌词设置区域：最大歌词字号缩放（48 sp / 24 sp）。
internal const val MAX_LYRIC_FONT_SCALE = 2f
// 歌词设置区域：最小歌词字重。
internal const val MIN_LYRIC_FONT_WEIGHT = 100
// 歌词设置区域：最大歌词字重。
internal const val MAX_LYRIC_FONT_WEIGHT = 900
// 歌词设置区域：100 至 900 字重的可选档位数。
internal const val LYRICS_FONT_WEIGHT_STEP_COUNT = 7
// 播放页标题跑马灯：重复内容之间占标题可用宽度的间距比例。
internal const val PLAYER_HEADER_MARQUEE_SPACING_FRACTION = 0.15f
// 播放页标题跑马灯：标题两端的渐隐宽度。
internal val PLAYER_HEADER_TITLE_EDGE_FADE_WIDTH = 12.dp

// 宽屏播放页：固定 24 dp 作为双栏组的首项，整个组在页面中居中。
internal val PLAYER_WIDE_FIXED_START_INSET = 24.dp
// 宽屏播放页：左侧播放列的最大宽度。
internal val PLAYER_WIDE_PLAYBACK_PANE_MAX_WIDTH = 480.dp
// Independent upper bound for the lyrics pane.
internal val PLAYER_WIDE_LYRICS_PANE_MAX_WIDTH = 600.dp
// Baseline lyrics-width expansion before the playback pane reaches its cap.
internal val PLAYER_WIDE_LYRICS_PANE_WIDTH_EXPANSION = 96.dp
// 宽屏播放页：播放栏与歌词栏之间的可调间距范围。
internal val PLAYER_WIDE_PANE_SPACING_MAX = 24.dp
internal val PLAYER_WIDE_PANE_SPACING_MIN = 12.dp
// 宽屏播放页：双栏组触及左边缘后，间距压缩到最小值的终止宽度。
internal val PLAYER_WIDE_PANE_SPACING_COMPRESSION_END_WIDTH = 600.dp
internal val PLAYER_WIDE_PREFERRED_GROUP_WIDTH =
    PLAYER_WIDE_FIXED_START_INSET +
        PLAYER_WIDE_PLAYBACK_PANE_MAX_WIDTH +
        PLAYER_WIDE_LYRICS_PANE_MAX_WIDTH +
        PLAYER_WIDE_PANE_SPACING_MAX
// 宽屏播放页：歌词栏内容两侧的固定内缩。
internal val PLAYER_WIDE_LYRICS_CONTENT_SIDE_INSET = 32.dp
// 宽屏播放页：双栏区域的上下内边距。
internal val PLAYER_LANDSCAPE_VERTICAL_PADDING = 24.dp
// 播放页：有系统安全区时，功能控制行与该安全区之间的固定间距。
internal val PLAYER_CONTROL_ROW_BOTTOM_SAFE_AREA_SPACING = 12.dp
// 播放页：无系统安全区时，功能控制行到页面底边的固定间距。
internal val PLAYER_CONTROL_ROW_BOTTOM_NO_SAFE_AREA_SPACING = 32.dp
// 紧凑横屏播放页：标题区域下边缘到主控制区上边缘的间距。
internal val COMPACT_LANDSCAPE_HEADER_TO_CONTROLS_SPACING = 36.dp

// 横屏窗口只有在可用高度达到阈值时才启用双栏播放页。
internal val PLAYER_WIDE_LANDSCAPE_MIN_HEIGHT = 600.dp

// 横屏封面：上下预留的固定内缩量。
internal val LANDSCAPE_PLAYER_ARTWORK_VERTICAL_INSET = 64.dp
// 横屏封面：标题槽位下边缘到封面上边缘的间距。
internal val LANDSCAPE_PLAYER_TITLE_TO_ARTWORK_SPACING = 12.dp
// 横屏封面：封面下边缘到进度条上边缘的间距。
internal val LANDSCAPE_PLAYER_ARTWORK_TO_PROGRESS_SPACING = 12.dp
// 宽屏播放页：左侧播放列功能行下边缘到该列底部的间距。
internal val PLAYER_WIDE_PANEL_BOTTOM_SPACING = 0.dp
// 播放页封面：内容尺寸相对测量可用尺寸的水平缩减量。
internal val PLAYER_ARTWORK_HORIZONTAL_REDUCTION = 6.dp

// 播放页封面区域：播放状态下封面四周内边距。
internal val PLAYER_ARTWORK_PLAYING_PADDING = 6.dp
// 播放页封面区域：暂停状态下封面四周内边距。
internal val PLAYER_ARTWORK_PAUSED_PADDING = 24.dp
// 播放页封面区域：外层容器相对封面的宽高总扩展量。
internal val PLAYER_ARTWORK_CONTAINER_EXPANSION = 12.dp
// 播放页封面：垂直占用范围的总扩展量，取容器扩展量与阴影模糊直径中的较大值。
internal val PLAYER_ARTWORK_VERTICAL_FOOTPRINT_EXPANSION =
    maxOf(
        PLAYER_ARTWORK_CONTAINER_EXPANSION,
        PLAYBACK_ARTWORK_SHADOW_BLUR_RADIUS * 2f,
    )

// 播放页标题区：安全区以下的顶部间距。
internal val PLAYER_HEADER_TOP_PADDING = 16.dp
// 播放页标题区：标题槽位到内容区的间距。
internal val PLAYER_HEADER_TO_CONTENT_SPACING = 12.dp
// 播放页标题区：歌名文本行高。
internal val PLAYER_HEADER_TITLE_LINE_HEIGHT = 32.sp
// 播放页标题区：歌手名文本行高。
internal val PLAYER_HEADER_ARTIST_LINE_HEIGHT = 20.sp
// 播放页控制区：封面内容下边缘到进度条上边缘的间距。
internal val PLAYER_ARTWORK_PROGRESS_SPACING = 16.dp
// 播放页主控制：前后曲目图标和播放图标的可见布局尺寸。
internal val PLAYER_SIDE_CONTROL_ICON_SIZE = 26.dp
internal val PLAYER_PLAY_PAUSE_ICON_SIZE = 42.dp
// 播放页主控制：前后曲目和播放按钮的最小按压区域。
internal val PLAYER_SIDE_CONTROL_TOUCH_SIZE = 56.dp
internal val PLAYER_PLAY_PAUSE_TOUCH_SIZE = 72.dp
// 播放页主控制：相邻按压区域之间的最小水平间距。
internal val PLAYER_PRIMARY_CONTROL_MIN_TOUCH_GAP = 12.dp
// 播放页控制区：进度条实际指示条下边缘到主控制行上边缘的间距。
internal val PLAYER_PROGRESS_TO_PRIMARY_SPACING = 32.dp
// 播放页控制区：主控制行下边缘到功能行上边缘的间距。
internal val PLAYER_CONTROL_GROUP_SPACING = 20.dp
// 播放页进度条区域：未触摸时实际指示条高度。
internal val PLAYER_PROGRESS_IDLE_HEIGHT = 6.dp
// 播放页进度条区域：拖动进度条的触摸目标高度。
internal val PLAYER_PROGRESS_TOUCH_HEIGHT = 24.dp
// 播放页进度条区域：时间文本与实际指示条下边缘的间距。
internal val PLAYER_PROGRESS_TIME_SPACING = 8.dp
// 播放页进度条区域：实际指示条下边缘相对触摸目标顶部的偏移。
internal val PLAYER_PROGRESS_IDLE_BOTTOM =
    (PLAYER_PROGRESS_TOUCH_HEIGHT + PLAYER_PROGRESS_IDLE_HEIGHT) / 2f
// 播放页进度条区域：时间文本相对触摸目标顶部的偏移。
internal val PLAYER_PROGRESS_LABEL_OFFSET =
    PLAYER_PROGRESS_IDLE_BOTTOM + PLAYER_PROGRESS_TIME_SPACING
// 播放页进度条区域：进度条容器总高度，保证主控制行距实际条下边缘 32 dp。
internal fun playerProgressLayoutHeight(primaryControlSpacing: Dp): Dp =
    PLAYER_PROGRESS_IDLE_BOTTOM + primaryControlSpacing
