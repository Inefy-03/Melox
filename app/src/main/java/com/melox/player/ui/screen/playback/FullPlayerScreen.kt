package com.melox.player.ui.screen.playback

import android.graphics.Bitmap
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Rect
import com.melox.player.model.LyricAnimationMode
import com.melox.player.R
import com.melox.player.data.library.ArtistGroup
import com.melox.player.data.library.displayArtistName
import com.melox.player.model.MusicTrack
import com.melox.player.model.LyricsUiState
import com.melox.player.model.LyricsDocument
import com.melox.player.model.PlaybackMode
import com.melox.player.model.PlaybackBackgroundStyle
import com.melox.player.model.PlaybackQueueItem
import com.melox.player.model.PlaybackUiState
import com.melox.player.model.SleepTimerState
import com.melox.player.model.withTrackMetadata
import com.melox.player.ui.usesMiuixSmallTopAppBar
import com.melox.player.ui.screen.settings.LyricsInterfacePreferences
import com.melox.player.ui.isMiuixWideLayout
import com.melox.player.ui.component.library.PlaybackArtworkFrame
import com.melox.player.ui.component.library.PLAYBACK_ARTWORK_SHADOW_BLUR_RADIUS
import com.melox.player.ui.component.library.TrackActionsOverlay
import com.melox.player.ui.component.bottomSheetCardColor
import com.melox.player.ui.component.bottomSheetGlassModifier
import com.melox.player.ui.component.bottomSheetMaterialColor
import com.melox.player.ui.component.library.formatDuration
import com.melox.player.ui.component.library.fullPlayerArtworkTargetSizePx
import com.melox.player.ui.component.library.normalizeArtworkTargetSize
import com.melox.player.ui.component.library.rememberFullPlayerArtworkBitmapPixels
import com.melox.player.ui.component.playback.BlurredArtworkBackground
import com.melox.player.ui.component.playback.DynamicFlowBackground
import com.melox.player.ui.component.playback.DynamicFlowBackgroundState
import com.melox.player.ui.component.playback.PLAYER_FULL_ARTWORK_CORNER_RADIUS
import com.melox.player.ui.component.playback.PLAYER_FULL_ARTWORK_REQUEST_SIZE
import com.melox.player.ui.component.playback.PLAYER_ARTWORK_RESOLUTION_CROSSFADE_DURATION_MILLIS
import com.melox.player.ui.component.playback.PLAYER_TRACK_ARTWORK_CROSSFADE_DURATION_MILLIS
import com.melox.player.ui.component.playback.PLAYER_TRACK_ARTWORK_CROSSFADE_EASING
import com.melox.player.ui.component.playback.rememberPlaybackArtworkResource
import com.melox.player.ui.component.playback.playerControlIconTransition
import com.melox.player.ui.component.playback.recordPlayerLayer
import com.melox.player.ui.component.playback.WeightedCrossfadeFrame
import com.melox.player.ui.component.playback.weightedCrossfadeFrames
import com.melox.player.ui.component.playback.rememberPlayerSheetVerticalDragModifier
import com.melox.player.ui.component.playback.artworkInsetRect
import com.melox.player.ui.component.playback.fittedArtworkRect
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Playlist
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
internal fun FullPlayerScreen(
    playback: PlaybackUiState,
    currentTrack: MusicTrack?,
    lyrics: LyricsUiState,
    playbackBackgroundStyle: PlaybackBackgroundStyle,
    dynamicFlowBackgroundState: DynamicFlowBackgroundState,
    lyricFontScale: Float,
    lyricFontWeight: Int,
    lyricAnimationMode: LyricAnimationMode,
    lyricBlurEnabled: Boolean,
    centerLyrics: Boolean,
    leftAlignPlayerTitle: Boolean,
    hideControlsOnLyrics: Boolean,
    showLyricsTranslation: Boolean,
    onLyricFontScaleChange: (Float) -> Unit,
    onLyricFontWeightChange: (Int) -> Unit,
    onLyricAnimationModeChange: (LyricAnimationMode) -> Unit,
    onLyricBlurEnabledChange: (Boolean) -> Unit,
    onCenterLyricsChange: (Boolean) -> Unit,
    onLeftAlignPlayerTitleChange: (Boolean) -> Unit,
    onHideControlsOnLyricsChange: (Boolean) -> Unit,
    onShowLyricsTranslationChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onCyclePlaybackMode: () -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    highPrecisionOutput: Boolean,
    onHighPrecisionOutputChange: (Boolean) -> Unit,
    sleepTimerState: SleepTimerState,
    sleepTimerSeconds: Int,
    onSleepTimerSecondsChange: (Int) -> Unit,
    autoExtendSleepTimer: Boolean,
    onAutoExtendSleepTimerChange: (Boolean) -> Unit,
    playbackPauseFade: Boolean,
    onPlaybackPauseFadeChange: (Boolean) -> Unit,
    onStartSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onAcknowledgeSleepTimerInterruption: () -> Unit,
    onOpenQueue: () -> Unit,
    onPlayNext: (MusicTrack) -> Unit,
    onAppendToQueue: (MusicTrack) -> Unit,
    onAddToPlaylist: (MusicTrack) -> Unit,
    onGoToAlbum: (MusicTrack) -> Unit,
    artistGroups: List<ArtistGroup>,
    onGoToArtist: (ArtistGroup) -> Unit,
    onExternalEditReturned: (Long) -> Unit,
    showMusicTagEditor: Boolean,
    showLyricoEditor: Boolean,
    showLunaBeatEditor: Boolean,
    backgroundLayer: GraphicsLayer,
    contentLayer: GraphicsLayer,
    frameRecordingGeneration: Int,
    interactionEnabled: Boolean,
    blockUnderlyingInput: Boolean,
    lyricsPagingEnabled: Boolean,
    drawInPlace: Boolean,
    sharedArtworkVisible: Boolean,
    initialArtworkPageSelected: Boolean,
    onPlayerDragStart: () -> Unit,
    onPlayerDrag: (Float) -> Unit,
    onPlayerDragEnd: (Float) -> Unit,
    onPlayerDragCancel: () -> Unit,
    onBackgroundLayerRecorded: (generation: Int, size: IntSize) -> Unit,
    onContentLayerRecorded: (generation: Int, size: IntSize) -> Unit,
    onPlayerBoundsChanged: (Rect) -> Unit,
    onArtworkBoundsChanged: (Rect) -> Unit,
    onArtworkPageSelectedChanged: (Boolean) -> Unit,
    onStatusBarBackgroundDarkChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = playback.currentItem?.let { queueItem ->
        currentTrack?.let(queueItem::withTrackMetadata) ?: queueItem
    }
    val density = LocalDensity.current
    val backgroundArtworkTargetSizePx = normalizeArtworkTargetSize(
        with(density) { PLAYER_FULL_ARTWORK_REQUEST_SIZE.roundToPx() },
    )
    var artworkDisplaySizePx by remember(density) {
        mutableIntStateOf(backgroundArtworkTargetSizePx)
    }
    val resolvedFullPlayerArtworkTargetSizePx =
        fullPlayerArtworkTargetSizePx(artworkDisplaySizePx)
    val artworkResource = rememberPlaybackArtworkResource(
        contentUri = item?.contentUri.orEmpty(),
        dateModifiedEpochSeconds = item?.dateModifiedEpochSeconds ?: 0L,
        fileSizeBytes = item?.fileSizeBytes ?: 0L,
        requestSize = PLAYER_FULL_ARTWORK_REQUEST_SIZE,
        prioritizeBlurredBackground =
            playbackBackgroundStyle == PlaybackBackgroundStyle.BLURRED_ARTWORK,
    )
    val fullPlayerArtwork = rememberFullPlayerArtworkBitmapPixels(
        contentUri = item?.contentUri.orEmpty(),
        dateModifiedEpochSeconds = item?.dateModifiedEpochSeconds ?: 0L,
        fileSizeBytes = item?.fileSizeBytes ?: 0L,
        targetSizePx = resolvedFullPlayerArtworkTargetSizePx,
        enabled = item != null && artworkDisplaySizePx > 0,
    )
    val artworkBlend = rememberArtworkBlend(
        targetBitmap = fullPlayerArtwork ?: artworkResource.artwork,
        animate = true,
        contentKey = item?.contentUri,
        sameContentDurationMillis = PLAYER_ARTWORK_RESOLUTION_CROSSFADE_DURATION_MILLIS,
    )
    val onArtworkDisplaySizeChanged: (Int) -> Unit = { sizePx ->
        artworkDisplaySizePx = sizePx
    }
    val emphasisControlColor = Color.White
    val controlColor = emphasisControlColor.copy(alpha = 0.6f)
    val artistControlColor = emphasisControlColor.copy(
        alpha = 0.6f,
    )
    val artworkCornerRadius = PLAYER_FULL_ARTWORK_CORNER_RADIUS
    var showTrackActions by remember { mutableStateOf(false) }
    var showLyricsSettings by remember { mutableStateOf(false) }
    var showPlaybackOptions by remember { mutableStateOf(false) }
    var showTimerInterruption by remember { mutableStateOf(false) }
    LaunchedEffect(sleepTimerState.interruptionNotice) {
        showTimerInterruption = sleepTimerState.interruptionNotice > 0
    }
    var displayedLyricFontScale by remember { mutableFloatStateOf(lyricFontScale) }
    var displayedLyricFontWeight by remember { mutableIntStateOf(lyricFontWeight) }
    var displayedLyricAnimationMode by remember {
        mutableStateOf(lyricAnimationMode)
    }
    var displayedLyricBlurEnabled by remember { mutableStateOf(lyricBlurEnabled) }
    var displayedCenterLyrics by remember { mutableStateOf(centerLyrics) }
    var displayedLeftAlignPlayerTitle by remember {
        mutableStateOf(leftAlignPlayerTitle)
    }
    var displayedHideControlsOnLyrics by remember {
        mutableStateOf(hideControlsOnLyrics)
    }
    var displayedShowLyricsTranslation by remember {
        mutableStateOf(showLyricsTranslation)
    }
    var lyricsFollowRequestKey by remember { mutableIntStateOf(0) }
    var lyricsSeekRequestKey by remember { mutableIntStateOf(0) }
    var lyricsSeekPositionMs by remember { mutableLongStateOf(playback.positionMs) }
    var lyricsSeekPositionUpdateAnchorMs by remember { mutableLongStateOf(0L) }
    var lyricsPreviewPositionMs by remember { mutableStateOf<Long?>(null) }
    val artworkPadding by animateDpAsState(
        targetValue = if (playback.playWhenReady) {
            PLAYER_ARTWORK_PLAYING_PADDING
        } else {
            PLAYER_ARTWORK_PAUSED_PADDING
        },
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 200f,
        ),
        label = "playerArtworkPadding",
    )
    val playerHeaderTitleSlotHeight = with(density) {
        PLAYER_HEADER_TITLE_LINE_HEIGHT.toDp()
    }
    val playerHeaderArtistSlotHeight = with(density) {
        PLAYER_HEADER_ARTIST_LINE_HEIGHT.toDp()
    }
    val playerHeaderContentHeight = playerHeaderTitleSlotHeight +
        1.dp +
        playerHeaderArtistSlotHeight
    val layoutDirection = LocalLayoutDirection.current
    val playerSafeDrawingPadding = WindowInsets.systemBars
        .union(WindowInsets.displayCutout)
        .asPaddingValues()
    val playerSafeStart = playerSafeDrawingPadding.calculateStartPadding(layoutDirection)
    val playerSafeTop = playerSafeDrawingPadding.calculateTopPadding()
    val playerSafeEnd = playerSafeDrawingPadding.calculateEndPadding(layoutDirection)
    val playerSafeBottom = playerSafeDrawingPadding.calculateBottomPadding()
    val playerControlsBottomPadding = playerControlsBottomPadding(playerSafeBottom)
    val onTogglePlayPauseFromPlayer = {
        if (!playback.playWhenReady) lyricsFollowRequestKey += 1
        onTogglePlayPause()
    }
    val onSeekFromPlayer: (Long) -> Unit = { targetPositionMs ->
        lyricsSeekPositionMs = targetPositionMs
        lyricsSeekPositionUpdateAnchorMs = playback.positionUpdateElapsedRealtimeMs
        lyricsSeekRequestKey += 1
        lyricsPreviewPositionMs = null
        onSeek(targetPositionMs)
    }
    val onPreviewSeekFromPlayer: (Long?) -> Unit = { targetPositionMs ->
        lyricsPreviewPositionMs = targetPositionMs
    }
    LaunchedEffect(item?.contentUri, playback.playbackIteration) {
        lyricsPreviewPositionMs = null
        lyricsSeekRequestKey = 0
        lyricsSeekPositionMs = playback.positionMs
        lyricsSeekPositionUpdateAnchorMs = 0L
    }
    LaunchedEffect(
        lyricsSeekRequestKey,
        lyricsSeekPositionMs,
        lyricsSeekPositionUpdateAnchorMs,
        playback.positionMs,
        playback.positionUpdateElapsedRealtimeMs,
    ) {
        if (
            lyricSeekRequestIsAcknowledged(
                requestKey = lyricsSeekRequestKey,
                positionUpdateAnchorElapsedRealtimeMs = lyricsSeekPositionUpdateAnchorMs,
                positionUpdateElapsedRealtimeMs = playback.positionUpdateElapsedRealtimeMs,
                currentPositionMs = playback.positionMs,
                seekPositionMs = lyricsSeekPositionMs,
            )
        ) {
            lyricsSeekRequestKey = 0
        }
    }
    val pagerState = rememberPagerState(
        initialPage = if (initialArtworkPageSelected) 0 else 1,
        pageCount = { 2 },
    )
    LaunchedEffect(lyricFontScale) {
        displayedLyricFontScale = lyricFontScale
    }
    LaunchedEffect(lyricFontWeight) {
        displayedLyricFontWeight = lyricFontWeight
    }
    LaunchedEffect(lyricAnimationMode) {
        displayedLyricAnimationMode = lyricAnimationMode
    }
    LaunchedEffect(lyricBlurEnabled) {
        displayedLyricBlurEnabled = lyricBlurEnabled
    }
    LaunchedEffect(centerLyrics) {
        displayedCenterLyrics = centerLyrics
    }
    LaunchedEffect(leftAlignPlayerTitle) {
        displayedLeftAlignPlayerTitle = leftAlignPlayerTitle
    }
    LaunchedEffect(hideControlsOnLyrics) {
        displayedHideControlsOnLyrics = hideControlsOnLyrics
    }
    LaunchedEffect(showLyricsTranslation) {
        displayedShowLyricsTranslation = showLyricsTranslation
    }
    val dismissGestureModifier = rememberPlayerSheetVerticalDragModifier(
        enabled = interactionEnabled,
        hasItem = item != null,
        onDragStart = onPlayerDragStart,
        onDrag = onPlayerDrag,
        onDragEnd = onPlayerDragEnd,
        onDragCancel = onPlayerDragCancel,
    )
    BackHandler(
        enabled = interactionEnabled,
        onBack = onDismiss,
    )
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(interactionEnabled, blockUnderlyingInput) {
                if (interactionEnabled || !blockUnderlyingInput) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                            .changes.forEach { it.consume() }
                    }
                }
            }
            .onGloballyPositioned { coordinates ->
                onPlayerBoundsChanged(
                    coordinates.findRootCoordinates().localBoundingBoxOf(
                        sourceCoordinates = coordinates,
                        clipBounds = false,
                    ),
                )
            },
        containerColor = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(dismissGestureModifier),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .recordPlayerLayer(
                        layer = backgroundLayer,
                        drawInPlace = drawInPlace,
                        recordingGeneration = frameRecordingGeneration,
                        onRecorded = onBackgroundLayerRecorded,
                    ),
            ) {
                when (playbackBackgroundStyle) {
                    PlaybackBackgroundStyle.BLURRED_ARTWORK -> BlurredArtworkBackground(
                        resource = artworkResource,
                        animate = drawInPlace && playback.isPlaying,
                        animateArtworkTransition = true,
                        onStatusBarBackgroundDarkChanged = onStatusBarBackgroundDarkChanged,
                        modifier = Modifier.fillMaxSize(),
                    )

                    PlaybackBackgroundStyle.DYNAMIC_FLOW -> DynamicFlowBackground(
                        state = dynamicFlowBackgroundState,
                        artwork = artworkResource.artwork,
                        artworkLoading = artworkResource.isLoading,
                        backgroundColor = artworkResource.backgroundColor,
                        animate = drawInPlace && playback.isPlaying,
                        onStatusBarBackgroundDarkChanged = onStatusBarBackgroundDarkChanged,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .recordPlayerLayer(
                        layer = contentLayer,
                        drawInPlace = drawInPlace,
                        recordingGeneration = frameRecordingGeneration,
                        onRecorded = onContentLayerRecorded,
                    ),
            ) {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val safeContentWidth = (maxWidth - playerSafeStart - playerSafeEnd)
                        .coerceAtLeast(0.dp)
                    val safeContentHeight = (maxHeight - playerSafeTop - playerSafeBottom)
                        .coerceAtLeast(0.dp)
                    val playerLayout = playerLayout(
                        windowWidth = safeContentWidth,
                        windowHeight = safeContentHeight,
                    )
                    val headerTopPadding = playerHeaderTopPadding(playerLayout, playerSafeTop)
                    val lyricsActive = playerLyricsAreActive(
                        layout = playerLayout,
                        currentPage = pagerState.currentPage,
                        targetPage = pagerState.targetPage,
                    )
                    SideEffect {
                        onArtworkPageSelectedChanged(
                            playerLayout != PlayerLayout.PORTRAIT || pagerState.settledPage == 0,
                        )
                    }
                    val portraitArtworkSize = playerArtworkSizeForContent(
                        playerContentWidth(
                            safeContentWidth,
                            PLAYER_PORTRAIT_CONTENT_MAX_WIDTH,
                        ),
                    )
                    val portraitArtworkContentWidth = playerArtworkAlignmentContentSize(
                        portraitArtworkSize,
                    )
                    val headerSpacing = playerVerticalSpacing(
                        availableHeight = maxHeight - playerHeaderContentHeight -
                            headerTopPadding,
                        preferredArtworkSize = portraitArtworkSize,
                    ).headerToArtwork
                    val pageHeight = maxHeight
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = playerSafeStart,
                                end = playerSafeEnd,
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                    val lyricsCenterOffsetY = if (displayedHideControlsOnLyrics) {
                        hiddenLyricsCenterOffsetY(
                            pageHeight = pageHeight,
                            headerTopPadding = headerTopPadding,
                            headerContentHeight = playerHeaderContentHeight,
                            headerSpacing = headerSpacing,
                        )
                    } else {
                        0.dp
                    }
                    if (playerLayout == PlayerLayout.PORTRAIT) {
                        PlayerHeader(
                            item = item,
                            trackChangeDirection = playback.trackChangeDirection,
                            titleColor = emphasisControlColor,
                            artistColor = artistControlColor,
                            leftAligned = displayedLeftAlignPlayerTitle,
                            titleSlotHeight = playerHeaderTitleSlotHeight,
                            artistSlotHeight = playerHeaderArtistSlotHeight,
                            modifier = Modifier
                                .width(portraitArtworkContentWidth)
                                .padding(top = headerTopPadding)
                                .then(
                                    rememberPlayerHeaderGestureModifier(
                                        enabled = displayedHideControlsOnLyrics &&
                                            lyricsPagingEnabled && item != null &&
                                            pagerState.settledPage == 1 &&
                                            !pagerState.isScrollInProgress,
                                        playWhenReady = playback.playWhenReady,
                                        onTogglePlayPause = onTogglePlayPause,
                                        onPrevious = onPrevious,
                                        onNext = onNext,
                                        onOpenLyricsSettings = { showLyricsSettings = true },
                                    ),
                                ),
                        )
                        Spacer(Modifier.height(headerSpacing))
                    }
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (playerLayout == PlayerLayout.WIDE_TWO_PANE) {
                            val playbackPaneWidth = landscapePlayerPlaybackPaneWidth(maxWidth)
                            val lyricsPaneWidth = landscapePlayerLyricsPaneWidth(maxWidth)
                            val paneSpacing = landscapePlayerPaneSpacing(maxWidth)
                            val artworkSize = landscapePlayerArtworkSize(
                                availableWidth = playbackPaneWidth,
                                availableHeight = maxHeight,
                            )
                            val artworkContentWidth = playerUnboundedContentWidth(playbackPaneWidth)
                            WidePlayerContent(
                                topPadding = headerTopPadding,
                                bottomPadding = playerControlsBottomPadding,
                                playbackPaneWidth = playbackPaneWidth,
                                lyricsPaneWidth = lyricsPaneWidth,
                                paneSpacing = paneSpacing,
                                playbackContent = {
                                    val spacing = landscapePlayerSpacing()
                                    PlayerHeader(
                                        item = item,
                                        trackChangeDirection = playback.trackChangeDirection,
                                        titleColor = emphasisControlColor,
                                        artistColor = artistControlColor,
                                        leftAligned = displayedLeftAlignPlayerTitle,
                                        titleSlotHeight = playerHeaderTitleSlotHeight,
                                        artistSlotHeight = playerHeaderArtistSlotHeight,
                                        modifier = Modifier.width(artworkContentWidth),
                                    )
                                    Spacer(Modifier.height(LANDSCAPE_PLAYER_TITLE_TO_ARTWORK_SPACING))
                                    BoxWithConstraints(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        PlayerArtwork(
                                            size = fitPlayerArtworkSize(
                                                preferredSize = artworkSize,
                                                availableHeight = maxHeight,
                                            ),
                                            artworkPadding = artworkPadding,
                                            artworkBlend = artworkBlend,
                                            cornerRadius = artworkCornerRadius,
                                            sharedArtworkVisible = sharedArtworkVisible,
                                            onArtworkBoundsChanged = onArtworkBoundsChanged,
                                            onArtworkDisplaySizeChanged =
                                                onArtworkDisplaySizeChanged,
                                        )
                                    }
                                    Spacer(Modifier.height(LANDSCAPE_PLAYER_ARTWORK_TO_PROGRESS_SPACING))
                                    PlayerDetails(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight(),
                                        contentWidth = artworkContentWidth,
                                        spacing = spacing,
                                        playback = playback,
                                        emphasisControlColor = emphasisControlColor,
                                        onTogglePlayPause = onTogglePlayPauseFromPlayer,
                                        onPrevious = onPrevious,
                                        onNext = onNext,
                                        onSeek = onSeekFromPlayer,
                                        onPreviewPositionChange = onPreviewSeekFromPlayer,
                                        onCyclePlaybackMode = onCyclePlaybackMode,
                                        onOpenPlaybackOptions = { showPlaybackOptions = true },
                                        onOpenLyricsSettings = { showLyricsSettings = true },
                                        onOpenQueue = onOpenQueue,
                                        onOpenTrackActions = {
                                            if (currentTrack != null) showTrackActions = true
                                        },
                                        primaryControlsWidth = artworkContentWidth,
                                        secondaryControlsWidth = playbackPaneWidth,
                                    )
                                    Spacer(Modifier.height(spacing.panelBottom))
                                },
                                lyricsContent = {
                                    SyncedLyrics(
                                        lyrics = lyrics,
                                        positionMs = playback.positionMs,
                                        positionUpdateElapsedRealtimeMs =
                                            playback.positionUpdateElapsedRealtimeMs,
                                        playbackIteration = playback.playbackIteration,
                                        playbackSpeed = playback.playbackSpeed,
                                        previewPositionMs = lyricsPreviewPositionMs,
                                        isPlaying = playback.isPlaying,
                                        active = lyricsActive,
                                        onSeek = onSeekFromPlayer,
                                        contentWidth = landscapePlayerLyricsContentWidth(lyricsPaneWidth),
                                        controlColor = controlColor,
                                        emphasisControlColor = emphasisControlColor,
                                        lyricFontScale = displayedLyricFontScale,
                                        lyricFontWeight = displayedLyricFontWeight,
                                        lyricAnimationMode = displayedLyricAnimationMode,
                                        lyricBlurEnabled = displayedLyricBlurEnabled,
                                        centerLyrics = displayedCenterLyrics,
                                        lyricCenterOffsetY = 0.dp,
                                        showLyricsTranslation = displayedShowLyricsTranslation,
                                        showBottomFade = true,
                                        resumeFollowRequestKey = lyricsFollowRequestKey,
                                        seekRequestKey = lyricsSeekRequestKey,
                                        seekPositionMs = lyricsSeekPositionMs,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                },
                            )
                        } else if (playerLayout == PlayerLayout.COMPACT_LANDSCAPE) {
                            CompactLandscapePlayerLayout(
                                pagerState = pagerState,
                                lyricsPagingEnabled = lyricsPagingEnabled,
                                artworkContent = { artworkSize ->
                                    PlayerArtwork(
                                        size = artworkSize,
                                        artworkPadding = artworkPadding,
                                        artworkBlend = artworkBlend,
                                        cornerRadius = artworkCornerRadius,
                                        sharedArtworkVisible = sharedArtworkVisible,
                                        onArtworkBoundsChanged = onArtworkBoundsChanged,
                                        onArtworkDisplaySizeChanged =
                                            onArtworkDisplaySizeChanged,
                                    )
                                },
                                lyricsContent = { contentWidth, contentHeight ->
                                    Box(
                                        modifier = Modifier
                                            .width(contentWidth)
                                            .height(contentHeight),
                                    ) {
                                        SyncedLyrics(
                                            lyrics = lyrics,
                                            positionMs = playback.positionMs,
                                            positionUpdateElapsedRealtimeMs =
                                                playback.positionUpdateElapsedRealtimeMs,
                                            playbackIteration = playback.playbackIteration,
                                            playbackSpeed = playback.playbackSpeed,
                                            previewPositionMs = lyricsPreviewPositionMs,
                                            isPlaying = playback.isPlaying,
                                            active = lyricsActive,
                                            onSeek = onSeekFromPlayer,
                                            contentWidth = contentWidth,
                                            controlColor = controlColor,
                                            emphasisControlColor = emphasisControlColor,
                                            lyricFontScale = displayedLyricFontScale,
                                            lyricFontWeight = displayedLyricFontWeight,
                                            lyricAnimationMode = displayedLyricAnimationMode,
                                            lyricBlurEnabled = displayedLyricBlurEnabled,
                                            centerLyrics = displayedCenterLyrics,
                                            lyricCenterOffsetY = 0.dp,
                                            showLyricsTranslation = displayedShowLyricsTranslation,
                                            showBottomFade = true,
                                            resumeFollowRequestKey = lyricsFollowRequestKey,
                                            seekRequestKey = lyricsSeekRequestKey,
                                            seekPositionMs = lyricsSeekPositionMs,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                },
                                controlsContent = { paneWidth, contentWidth ->
                                    Column(
                                        modifier = Modifier
                                            .width(paneWidth)
                                            .wrapContentHeight(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        PlayerHeader(
                                            item = item,
                                            trackChangeDirection = playback.trackChangeDirection,
                                            titleColor = emphasisControlColor,
                                            artistColor = artistControlColor,
                                            leftAligned = displayedLeftAlignPlayerTitle,
                                            titleSlotHeight = playerHeaderTitleSlotHeight,
                                            artistSlotHeight = playerHeaderArtistSlotHeight,
                                            modifier = Modifier.width(contentWidth),
                                        )
                                        Spacer(Modifier.height(COMPACT_LANDSCAPE_HEADER_TO_CONTROLS_SPACING))
                                        PlayerDetails(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentWidth = contentWidth,
                                            spacing = compactLandscapePlayerSpacing(),
                                            playback = playback,
                                            emphasisControlColor = emphasisControlColor,
                                            onTogglePlayPause = onTogglePlayPauseFromPlayer,
                                            onPrevious = onPrevious,
                                            onNext = onNext,
                                            onSeek = onSeekFromPlayer,
                                            onPreviewPositionChange = onPreviewSeekFromPlayer,
                                            onCyclePlaybackMode = onCyclePlaybackMode,
                                            onOpenPlaybackOptions = { showPlaybackOptions = true },
                                            onOpenLyricsSettings = { showLyricsSettings = true },
                                            onOpenQueue = onOpenQueue,
                                            onOpenTrackActions = {
                                                if (currentTrack != null) showTrackActions = true
                                            },
                                            primaryControlsWidth = minOf(contentWidth, 320.dp),
                                            secondaryControlsWidth = paneWidth,
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(
                                        start = compactLandscapeSidePadding(
                                            ownSafeInset = playerSafeStart,
                                            oppositeSafeInset = playerSafeEnd,
                                        ),
                                        top = playerSafeTop,
                                        end = compactLandscapeSidePadding(
                                            ownSafeInset = playerSafeEnd,
                                            oppositeSafeInset = playerSafeStart,
                                        ),
                                        bottom = playerSafeBottom,
                                    ),
                            )
                        } else {
                            val contentWidth = playerContentWidth(
                                maxWidth, PLAYER_PORTRAIT_CONTENT_MAX_WIDTH,
                            )
                            val artworkSize = playerArtworkSizeForContent(contentWidth)
                            val artworkContentWidth = playerArtworkAlignmentContentSize(artworkSize)
                            val spacing = playerVerticalSpacing(
                                availableHeight = maxHeight,
                                preferredArtworkSize = artworkSize,
                                panelBottom = playerControlsBottomPadding,
                            )
                            if (displayedHideControlsOnLyrics) {
                                PlayerContentPager(
                                    pagerState = pagerState,
                                    lyrics = lyrics,
                                    positionMs = playback.positionMs,
                                    positionUpdateElapsedRealtimeMs =
                                        playback.positionUpdateElapsedRealtimeMs,
                                    playbackIteration = playback.playbackIteration,
                                    playbackSpeed = playback.playbackSpeed,
                                    previewPositionMs = lyricsPreviewPositionMs,
                                    isPlaying = playback.isPlaying,
                                    lyricsActive = lyricsActive,
                                    onSeek = onSeekFromPlayer,
                                    contentWidth = artworkContentWidth,
                                    controlColor = controlColor,
                                    emphasisControlColor = emphasisControlColor,
                                    lyricsPagingEnabled = lyricsPagingEnabled,
                                    lyricFontScale = displayedLyricFontScale,
                                    lyricFontWeight = displayedLyricFontWeight,
                                    lyricAnimationMode = displayedLyricAnimationMode,
                                    lyricBlurEnabled = displayedLyricBlurEnabled,
                                    centerLyrics = displayedCenterLyrics,
                                    lyricCenterOffsetY = lyricsCenterOffsetY,
                                    showLyricsTranslation = displayedShowLyricsTranslation,
                                    showBottomFade = false,
                                    resumeFollowRequestKey = lyricsFollowRequestKey,
                                    seekRequestKey = lyricsSeekRequestKey,
                                    seekPositionMs = lyricsSeekPositionMs,
                                    modifier = Modifier.fillMaxSize(),
                                    artworkContent = {
                                        PortraitPlayerLayout(
                                            spacing = spacing,
                                            artworkContent = { artworkHeight ->
                                                PlayerArtwork(
                                                    size = fitPlayerArtworkSize(
                                                        artworkSize,
                                                        artworkHeight,
                                                    ),
                                                    artworkPadding = artworkPadding,
                                                    artworkBlend = artworkBlend,
                                                    cornerRadius = artworkCornerRadius,
                                                    sharedArtworkVisible = sharedArtworkVisible,
                                                    onArtworkBoundsChanged = onArtworkBoundsChanged,
                                                    onArtworkDisplaySizeChanged =
                                                        onArtworkDisplaySizeChanged,
                                                )
                                            },
                                            detailsContent = {
                                                PlayerDetails(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    contentWidth = artworkContentWidth,
                                                    secondaryControlsWidth = maxWidth,
                                                    spacing = spacing,
                                                    playback = playback,
                                                    emphasisControlColor = emphasisControlColor,
                                                    onTogglePlayPause =
                                                        onTogglePlayPauseFromPlayer,
                                                    onPrevious = onPrevious,
                                                    onNext = onNext,
                                                    onSeek = onSeekFromPlayer,
                                                    onPreviewPositionChange = onPreviewSeekFromPlayer,
                                                    onCyclePlaybackMode = onCyclePlaybackMode,
                                                    onOpenPlaybackOptions = { showPlaybackOptions = true },
                                                    onOpenLyricsSettings = {
                                                        showLyricsSettings = true
                                                    },
                                                    onOpenQueue = onOpenQueue,
                                                    onOpenTrackActions = {
                                                        if (currentTrack != null) {
                                                            showTrackActions = true
                                                        }
                                                    },
                                                )
                                            },
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    },
                                )
                            } else {
                                PortraitPlayerLayout(
                                    spacing = spacing,
                                    artworkContent = { artworkHeight ->
                                        PlayerContentPager(
                                            pagerState = pagerState,
                                            lyrics = lyrics,
                                            positionMs = playback.positionMs,
                                            positionUpdateElapsedRealtimeMs =
                                                playback.positionUpdateElapsedRealtimeMs,
                                            playbackIteration = playback.playbackIteration,
                                            playbackSpeed = playback.playbackSpeed,
                                            previewPositionMs = lyricsPreviewPositionMs,
                                            isPlaying = playback.isPlaying,
                                            lyricsActive = lyricsActive,
                                            onSeek = onSeekFromPlayer,
                                            contentWidth = artworkContentWidth,
                                            controlColor = controlColor,
                                            emphasisControlColor = emphasisControlColor,
                                            lyricsPagingEnabled = lyricsPagingEnabled,
                                            lyricFontScale = displayedLyricFontScale,
                                            lyricFontWeight = displayedLyricFontWeight,
                                            lyricAnimationMode =
                                                displayedLyricAnimationMode,
                                            lyricBlurEnabled = displayedLyricBlurEnabled,
                                            centerLyrics = displayedCenterLyrics,
                                            lyricCenterOffsetY = lyricsCenterOffsetY,
                                            showLyricsTranslation =
                                                displayedShowLyricsTranslation,
                                            showBottomFade = true,
                                            resumeFollowRequestKey = lyricsFollowRequestKey,
                                            seekRequestKey = lyricsSeekRequestKey,
                                            seekPositionMs = lyricsSeekPositionMs,
                                            modifier = Modifier.fillMaxSize(),
                                            artworkContent = {
                                                PlayerArtwork(
                                                    size = fitPlayerArtworkSize(
                                                        artworkSize,
                                                        artworkHeight,
                                                    ),
                                                    artworkPadding = artworkPadding,
                                                    artworkBlend = artworkBlend,
                                                    cornerRadius = artworkCornerRadius,
                                                    sharedArtworkVisible = sharedArtworkVisible,
                                                    onArtworkBoundsChanged =
                                                        onArtworkBoundsChanged,
                                                    onArtworkDisplaySizeChanged =
                                                        onArtworkDisplaySizeChanged,
                                                )
                                            },
                                        )
                                    },
                                    detailsContent = {
                                        PlayerDetails(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentWidth = artworkContentWidth,
                                            secondaryControlsWidth = maxWidth,
                                            spacing = spacing,
                                            playback = playback,
                                            emphasisControlColor = emphasisControlColor,
                                            onTogglePlayPause = onTogglePlayPauseFromPlayer,
                                            onPrevious = onPrevious,
                                            onNext = onNext,
                                            onSeek = onSeekFromPlayer,
                                            onPreviewPositionChange = onPreviewSeekFromPlayer,
                                            onCyclePlaybackMode = onCyclePlaybackMode,
                                            onOpenPlaybackOptions = { showPlaybackOptions = true },
                                            onOpenLyricsSettings = {
                                                showLyricsSettings = true
                                            },
                                            onOpenQueue = onOpenQueue,
                                            onOpenTrackActions = {
                                                if (currentTrack != null) {
                                                    showTrackActions = true
                                                }
                                            },
                                        )
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                    }
                }
                TrackActionsOverlay(
                    track = currentTrack.takeIf { showTrackActions },
                    onDismiss = { showTrackActions = false },
                    onPlayNext = onPlayNext,
                    onAppendToQueue = onAppendToQueue,
                    onAddToPlaylist = onAddToPlaylist,
                    onGoToAlbum = onGoToAlbum,
                    artistGroups = artistGroups,
                    onGoToArtist = onGoToArtist,
                    onExternalEditReturned = onExternalEditReturned,
                    showMusicTagEditor = showMusicTagEditor,
                    showLyricoEditor = showLyricoEditor,
                    showLunaBeatEditor = showLunaBeatEditor,
                )
                PlayerSettingsSheet(
                    show = showLyricsSettings,
                    leftAlignPlayerTitle = displayedLeftAlignPlayerTitle,
                    lyricFontScale = displayedLyricFontScale,
                    lyricFontWeight = displayedLyricFontWeight,
                    lyricAnimationMode = displayedLyricAnimationMode,
                    lyricBlurEnabled = displayedLyricBlurEnabled,
                    centerLyrics = displayedCenterLyrics,
                    hideControlsOnLyrics = displayedHideControlsOnLyrics,
                    showLyricsTranslation = displayedShowLyricsTranslation,
                    onDismiss = { showLyricsSettings = false },
                    onLeftAlignPlayerTitleChange = {
                        displayedLeftAlignPlayerTitle = it
                        onLeftAlignPlayerTitleChange(it)
                    },
                    onLyricFontScalePreview = { displayedLyricFontScale = it },
                    onLyricFontScaleCommit = {
                        onLyricFontScaleChange(displayedLyricFontScale)
                    },
                    onLyricFontWeightPreview = { displayedLyricFontWeight = it },
                    onLyricFontWeightCommit = {
                        onLyricFontWeightChange(displayedLyricFontWeight)
                    },
                    onLyricAnimationModeChange = {
                        displayedLyricAnimationMode = it
                        onLyricAnimationModeChange(it)
                    },
                    onLyricBlurEnabledChange = {
                        displayedLyricBlurEnabled = it
                        onLyricBlurEnabledChange(it)
                    },
                    onCenterLyricsChange = {
                        displayedCenterLyrics = it
                        onCenterLyricsChange(it)
                    },
                    onHideControlsOnLyricsChange = {
                        displayedHideControlsOnLyrics = it
                        onHideControlsOnLyricsChange(it)
                    },
                    onShowLyricsTranslationChange = {
                        displayedShowLyricsTranslation = it
                        onShowLyricsTranslationChange(it)
                    },
                )
                PlaybackOptionsDialog(
                    show = showPlaybackOptions,
                    playbackSpeed = playback.playbackSpeed,
                    floatOutputActive = playback.floatOutputActive,
                    highPrecisionOutput = highPrecisionOutput,
                    onHighPrecisionOutputChange = onHighPrecisionOutputChange,
                    timerSeconds = sleepTimerSeconds,
                    timerState = sleepTimerState,
                    autoExtendSleepTimer = autoExtendSleepTimer,
                    onAutoExtendSleepTimerChange = onAutoExtendSleepTimerChange,
                    playbackPauseFade = playbackPauseFade,
                    onPlaybackPauseFadeChange = onPlaybackPauseFadeChange,
                    onDismiss = { showPlaybackOptions = false },
                    onPlaybackSpeedChange = onPlaybackSpeedChange,
                    onTimerSecondsChange = onSleepTimerSecondsChange,
                    onStartTimer = onStartSleepTimer,
                    onCancelTimer = onCancelSleepTimer,
                )
                OverlayDialog(
                    show = showTimerInterruption,
                    title = stringResource(R.string.playback_timer_extension_title),
                    summary = stringResource(R.string.playback_timer_extension_interrupted),
                    onDismissRequest = {
                        showTimerInterruption = false
                        onAcknowledgeSleepTimerInterruption()
                    },
                ) {
                    TextButton(
                        text = stringResource(R.string.playback_timer_confirm),
                        onClick = {
                            showTimerInterruption = false
                            onAcknowledgeSleepTimerInterruption()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
    }
}

}

@Composable
private fun PlayerSettingsSheet(
    show: Boolean,
    leftAlignPlayerTitle: Boolean,
    lyricFontScale: Float,
    lyricFontWeight: Int,
    lyricAnimationMode: LyricAnimationMode,
    lyricBlurEnabled: Boolean,
    centerLyrics: Boolean,
    hideControlsOnLyrics: Boolean,
    showLyricsTranslation: Boolean,
    onDismiss: () -> Unit,
    onLeftAlignPlayerTitleChange: (Boolean) -> Unit,
    onLyricFontScalePreview: (Float) -> Unit,
    onLyricFontScaleCommit: () -> Unit,
    onLyricFontWeightPreview: (Int) -> Unit,
    onLyricFontWeightCommit: () -> Unit,
    onLyricAnimationModeChange: (LyricAnimationMode) -> Unit,
    onLyricBlurEnabledChange: (Boolean) -> Unit,
    onCenterLyricsChange: (Boolean) -> Unit,
    onHideControlsOnLyricsChange: (Boolean) -> Unit,
    onShowLyricsTranslationChange: (Boolean) -> Unit,
) {
    val wideLayout = usesMiuixSmallTopAppBar()
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val density = LocalDensity.current
    var firstCardHeightPx by remember { mutableIntStateOf(0) }
    val contentMaxHeight = if (isPortrait && firstCardHeightPx > 0) {
        with(density) { firstCardHeightPx.toDp() } + 32.dp
    } else {
        Dp.Infinity
    }
    val bottomPadding = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    OverlayBottomSheet(
        show = show,
        modifier = bottomSheetGlassModifier(),
        backgroundColor = bottomSheetMaterialColor(),
        title = stringResource(R.string.player_settings),
        enableWindowDim = true,
        onDismissRequest = onDismiss,
    ) {
        val scrollState = rememberScrollState()
        LyricsInterfacePreferences(
            leftAlignPlayerTitle = leftAlignPlayerTitle,
            centerLyrics = centerLyrics,
            showLyricsTranslation = showLyricsTranslation,
            lyricFontScale = lyricFontScale,
            lyricFontWeight = lyricFontWeight,
            lyricBlurEnabled = lyricBlurEnabled,
            lyricAnimationMode = lyricAnimationMode,
            hideControlsOnLyrics = hideControlsOnLyrics,
            onLeftAlignPlayerTitleChange = onLeftAlignPlayerTitleChange,
            onCenterLyricsChange = onCenterLyricsChange,
            onShowLyricsTranslationChange = onShowLyricsTranslationChange,
            onLyricFontScalePreview = onLyricFontScalePreview,
            onLyricFontScaleCommit = onLyricFontScaleCommit,
            onLyricFontWeightPreview = onLyricFontWeightPreview,
            onLyricFontWeightCommit = onLyricFontWeightCommit,
            onLyricBlurEnabledChange = onLyricBlurEnabledChange,
            onLyricAnimationModeChange = onLyricAnimationModeChange,
            onHideControlsOnLyricsChange = onHideControlsOnLyricsChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = contentMaxHeight)
                .overScrollVertical(
                    nestedScrollToParent = false,
                    isEnabled = { scrollState.maxValue > 0 },
                )
                .verticalScroll(scrollState, overscrollEffect = null)
                .padding(bottom = bottomPadding + 12.dp),
            cardColor = bottomSheetCardColor(),
            usePageMaterial = false,
            showHideControls = !wideLayout,
            firstCardModifier = Modifier.onSizeChanged { firstCardHeightPx = it.height },
        )
    }
}

@Composable
internal fun TappableSliderPreference(
    value: Float,
    onValueChange: (Float) -> Unit,
    title: String,
    valueText: String,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    showKeyPoints: Boolean = false,
    keyPoints: List<Float>? = null,
    magnetThreshold: Float = 0.02f,
    enabled: Boolean = true,
    summary: String? = null,
) {
    val layoutDirection = LocalLayoutDirection.current
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)
    val currentEnabled by rememberUpdatedState(enabled)

    BasicComponent(
        title = title,
        summary = summary,
        enabled = enabled,
        endActions = {
            Row(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .align(Alignment.CenterVertically)
                    .weight(1f, fill = false),
            ) {
                Text(
                    text = valueText,
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                    color = if (enabled) MiuixTheme.colorScheme.onSurfaceVariantActions
                    else MiuixTheme.colorScheme.disabledOnSecondaryVariant,
                )
            }
        },
        bottomAction = {
            Slider(
                value = value,
                enabled = enabled,
                onValueChange = { if (currentEnabled) currentOnValueChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        if (!enabled) {
                            disabled()
                            setProgress { false }
                        }
                    }
                    .pointerInput(enabled, valueRange, steps, keyPoints, magnetThreshold, layoutDirection) {
                        if (!enabled) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var pointerPosition = down.position
                            var maxDistance = 0f
                            while (true) {
                                val change = awaitPointerEvent().changes
                                    .firstOrNull { it.id == down.id }
                                    ?: break
                                pointerPosition = change.position
                                maxDistance = maxOf(
                                    maxDistance,
                                    (pointerPosition - down.position).getDistance(),
                                )
                                if (!change.pressed) {
                                    if (maxDistance < viewConfiguration.touchSlop) {
                                        val tappedValue = sliderValueAtPosition(
                                            positionX = pointerPosition.x,
                                            width = size.width,
                                            height = size.height,
                                            valueRange = valueRange,
                                            steps = steps,
                                            keyPoints = keyPoints,
                                            magnetThreshold = magnetThreshold,
                                            reverseDirection = layoutDirection == LayoutDirection.Rtl,
                                        )
                                        currentOnValueChange(tappedValue)
                                        currentOnValueChangeFinished?.invoke()
                                    }
                                    break
                                }
                            }
                        }
                    },
                valueRange = valueRange,
                steps = steps,
                onValueChangeFinished = currentOnValueChangeFinished?.let { callback ->
                    { if (currentEnabled) callback() }
                },
                showKeyPoints = showKeyPoints,
                keyPoints = keyPoints,
                magnetThreshold = magnetThreshold,
            )
        },
    )
}

internal fun sliderValueAtPosition(
    positionX: Float,
    width: Int,
    height: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    keyPoints: List<Float>?,
    magnetThreshold: Float,
    reverseDirection: Boolean,
): Float {
    val thumbRadius = height / 2f
    val availableWidth = (width - 2f * thumbRadius).coerceAtLeast(0f)
    val visualFraction = if (availableWidth == 0f) {
        0f
    } else {
        ((positionX - thumbRadius) / availableWidth).coerceIn(0f, 1f)
    }
    val fraction = if (reverseDirection) 1f - visualFraction else visualFraction
    val rangeLength = valueRange.endInclusive - valueRange.start
    if (steps > 0) {
        val intervalCount = steps + 1
        val stepIndex = (fraction * intervalCount).roundToInt().coerceIn(0, intervalCount)
        return valueRange.start + rangeLength * stepIndex / intervalCount
    }
    val baseValue = valueRange.start + rangeLength * fraction
    val nearestKeyPoint = keyPoints
        ?.minByOrNull { point ->
            abs((point - valueRange.start) / rangeLength - fraction)
        }
        ?: return baseValue
    val keyPointFraction = (nearestKeyPoint - valueRange.start) / rangeLength
    return if (abs(keyPointFraction - fraction) < magnetThreshold) {
        nearestKeyPoint
    } else {
        baseValue
    }
}

private data class PlayerHeaderContent(
    val trackKey: String?,
    val direction: Int,
    val title: String,
    val artist: String,
)

internal fun playerHeaderArtistText(artist: String): AnnotatedString = buildAnnotatedString {
    artist.split(" / ").forEachIndexed { index, name ->
        if (index > 0) {
            append(' ')
            withStyle(SpanStyle(fontWeight = FontWeight.Thin)) {
                append('/')
            }
            append(' ')
        }
        append(name)
    }
}

@Composable
private fun PlayerHeader(
    item: PlaybackQueueItem?,
    trackChangeDirection: Int,
    titleColor: Color,
    artistColor: Color,
    leftAligned: Boolean,
    titleSlotHeight: Dp,
    artistSlotHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val header = PlayerHeaderContent(
        trackKey = item?.contentUri,
        direction = trackChangeDirection,
        title = item?.title ?: stringResource(R.string.no_track_selected),
        artist = displayArtistName(item?.artist)
            ?: stringResource(R.string.music_unknown_artist),
    )
    val headerTransition = updateTransition(header, label = "playerHeaderTrack")
    val alignmentProgress = animateFloatAsState(
        targetValue = if (leftAligned) 0f else 1f,
        animationSpec = playerTextAlignmentSpec(),
        label = "playerHeaderAlignment",
    )
    val titleFadeWidthPx = with(LocalDensity.current) {
        PLAYER_HEADER_TITLE_EDGE_FADE_WIDTH.roundToPx()
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (leftAligned) Alignment.Start else Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = titleSlotHeight),
            contentAlignment = if (leftAligned) Alignment.CenterStart else Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .expandLeftForMarquee(PLAYER_HEADER_TITLE_EDGE_FADE_WIDTH)
                    .fillMaxWidth()
                    .playerHeaderEdgeMask(),
            ) {
                headerTransition.AnimatedContent(
                    modifier = Modifier.fillMaxWidth(),
                    contentKey = { it.trackKey to it.direction },
                    transitionSpec = {
                        slideInHorizontally(tween(PLAYER_TRACK_ARTWORK_CROSSFADE_DURATION_MILLIS)) { it * targetState.direction }
                            .togetherWith(
                                slideOutHorizontally(tween(PLAYER_TRACK_ARTWORK_CROSSFADE_DURATION_MILLIS)) { -it * targetState.direction },
                            ).using(null)
                    },
                ) { target ->
                    var titleLayout by remember(target.trackKey) {
                        mutableStateOf<TextLayoutResult?>(null)
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .animatedPlayerTextAlignment(
                                layout = titleLayout,
                                progress = alignmentProgress,
                                viewportInsetPx = titleFadeWidthPx.toFloat(),
                            )
                            .basicMarquee(
                                iterations = if (headerTransition.isRunning) 0 else 1,
                                spacing = MarqueeSpacing.fractionOfContainer(
                                    PLAYER_HEADER_MARQUEE_SPACING_FRACTION,
                                ),
                            ),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            text = target.title,
                            modifier = Modifier.fillMaxWidth()
                                .padding(start = PLAYER_HEADER_TITLE_EDGE_FADE_WIDTH),
                            style = MiuixTheme.textStyles.title3.copy(
                                lineHeight = PLAYER_HEADER_TITLE_LINE_HEIGHT,
                            ),
                            color = titleColor,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            onTextLayout = { titleLayout = it },
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = artistSlotHeight),
            contentAlignment = if (leftAligned) Alignment.CenterStart else Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .expandLeftForMarquee(PLAYER_HEADER_TITLE_EDGE_FADE_WIDTH)
                    .fillMaxWidth()
                    .playerHeaderEdgeMask(),
            ) {
                headerTransition.AnimatedContent(
                    modifier = Modifier.fillMaxWidth(),
                    contentKey = { it.trackKey to it.direction },
                    transitionSpec = {
                        slideInHorizontally(tween(PLAYER_TRACK_ARTWORK_CROSSFADE_DURATION_MILLIS)) { it * targetState.direction }
                            .togetherWith(
                                slideOutHorizontally(tween(PLAYER_TRACK_ARTWORK_CROSSFADE_DURATION_MILLIS)) { -it * targetState.direction },
                            ).using(null)
                    },
                    contentAlignment = if (leftAligned) Alignment.CenterStart else Alignment.Center,
                ) { target ->
                    var artistLayout by remember(target.trackKey) {
                        mutableStateOf<TextLayoutResult?>(null)
                    }
                    Text(
                        text = playerHeaderArtistText(target.artist),
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = PLAYER_HEADER_TITLE_EDGE_FADE_WIDTH)
                            .animatedPlayerTextAlignment(artistLayout, alignmentProgress),
                        style = MiuixTheme.textStyles.body2.copy(
                            fontSize = 14.sp,
                            lineHeight = PLAYER_HEADER_ARTIST_LINE_HEIGHT,
                        ),
                        color = artistColor,
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { artistLayout = it },
                    )
                }
            }
        }
    }
}

private fun Modifier.playerHeaderEdgeMask(): Modifier = this
    .graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
    }
    .drawWithContent {
        drawContent()
        val fadeWidth = PLAYER_HEADER_TITLE_EDGE_FADE_WIDTH.toPx()
        val edgeFraction = (
            fadeWidth / size.width.coerceAtLeast(1f)
        ).coerceIn(0f, 0.18f)
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent,
                edgeFraction to Color.Black,
                1f - edgeFraction to Color.Black,
                1f to Color.Transparent,
            ),
            blendMode = BlendMode.DstIn,
        )
    }

private fun Modifier.expandLeftForMarquee(extra: Dp): Modifier = layout { measurable, constraints ->
    val extraPx = extra.roundToPx()
    val expandedConstraints = constraints.copy(
        minWidth = constraints.minWidth + extraPx,
        maxWidth = constraints.maxWidth + extraPx,
    )
    val placeable = measurable.measure(expandedConstraints)
    layout(constraints.maxWidth, placeable.height) {
        placeable.placeRelative(-extraPx, 0)
    }
}

internal fun progressGestureIsDrag(
    horizontalDistancePx: Float,
    touchSlopPx: Float,
): Boolean = abs(horizontalDistancePx) >= touchSlopPx
