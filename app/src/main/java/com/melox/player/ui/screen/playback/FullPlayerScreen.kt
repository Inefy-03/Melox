package com.melox.player.ui.screen.playback

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Rect
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
import com.melox.player.model.withTrackMetadata
import com.melox.player.ui.component.library.PlaybackArtworkFrame
import com.melox.player.ui.component.library.TrackActionsOverlay
import com.melox.player.ui.component.library.formatDuration
import com.melox.player.ui.component.library.rememberArtworkBitmap
import com.melox.player.ui.component.playback.ArtworkFlowBackground
import com.melox.player.ui.component.playback.BlurredArtworkBackground
import com.melox.player.ui.component.playback.PLAYER_FULL_ARTWORK_CORNER_RADIUS
import com.melox.player.ui.component.playback.PLAYER_FULL_ARTWORK_REQUEST_SIZE
import com.melox.player.ui.component.playback.PLAYER_TRACK_ARTWORK_CROSSFADE_DURATION_MILLIS
import com.melox.player.ui.component.playback.PLAYER_TRACK_ARTWORK_CROSSFADE_EASING
import com.melox.player.ui.component.playback.playerControlIconTransition
import com.melox.player.ui.component.playback.recordPlayerLayer
import com.melox.player.ui.component.playback.rememberPlayerSheetVerticalDragModifier
import com.melox.player.ui.component.playback.artworkInsetRect
import com.melox.player.ui.component.playback.fittedArtworkRect
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ConvertFile
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Playlist
import top.yukonga.miuix.kmp.icon.extended.Replace
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun FullPlayerScreen(
    playback: PlaybackUiState,
    currentTrack: MusicTrack?,
    lyrics: LyricsUiState,
    isDark: Boolean,
    playbackBackgroundStyle: PlaybackBackgroundStyle,
    lyricFontScale: Float,
    lyricFontWeight: Int,
    forceWordByWordLyrics: Boolean,
    lyricBlurEnabled: Boolean,
    centerLyrics: Boolean,
    hideControlsOnLyrics: Boolean,
    showLyricsTranslation: Boolean,
    onLyricFontScaleChange: (Float) -> Unit,
    onLyricFontWeightChange: (Int) -> Unit,
    onForceWordByWordLyricsChange: (Boolean) -> Unit,
    onLyricBlurEnabledChange: (Boolean) -> Unit,
    onCenterLyricsChange: (Boolean) -> Unit,
    onHideControlsOnLyricsChange: (Boolean) -> Unit,
    onShowLyricsTranslationChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onCyclePlaybackMode: () -> Unit,
    onOpenQueue: () -> Unit,
    onPlayNext: (MusicTrack) -> Unit,
    onAppendToQueue: (MusicTrack) -> Unit,
    onGoToAlbum: (MusicTrack) -> Unit,
    artistGroups: List<ArtistGroup>,
    onGoToArtist: (ArtistGroup) -> Unit,
    onExternalEditReturned: (Long) -> Unit,
    playerLayer: GraphicsLayer,
    interactionEnabled: Boolean,
    lyricsPagingEnabled: Boolean,
    drawInPlace: Boolean,
    sharedArtworkVisible: Boolean,
    initialArtworkPageSelected: Boolean,
    onPlayerDragStart: () -> Unit,
    onPlayerDrag: (Float) -> Unit,
    onPlayerDragEnd: (Float) -> Unit,
    onPlayerDragCancel: () -> Unit,
    onPlayerBoundsChanged: (Rect) -> Unit,
    onArtworkBoundsChanged: (Rect) -> Unit,
    onArtworkPageSelectedChanged: (Boolean) -> Unit,
    onStatusBarBackgroundDarkChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = playback.currentItem?.let { queueItem ->
        currentTrack?.let(queueItem::withTrackMetadata) ?: queueItem
    }
    val loadedArtworkBitmap = item?.let {
        rememberArtworkBitmap(
            contentUri = it.contentUri,
            dateModifiedEpochSeconds = it.dateModifiedEpochSeconds,
            fileSizeBytes = it.fileSizeBytes,
            size = PLAYER_FULL_ARTWORK_REQUEST_SIZE,
        )
    }
    val artworkBlend = rememberArtworkBlend(
        targetBitmap = loadedArtworkBitmap,
        animate = drawInPlace,
    )
    val emphasisControlColor = Color.White
    val controlColor = emphasisControlColor.copy(alpha = 0.8f)
    val artistControlColor = emphasisControlColor.copy(
        alpha = 0.6f,
    )
    val artworkCornerRadius = PLAYER_FULL_ARTWORK_CORNER_RADIUS
    var showTrackActions by remember { mutableStateOf(false) }
    var showLyricsSettings by remember { mutableStateOf(false) }
    var displayedLyricFontScale by remember { mutableFloatStateOf(lyricFontScale) }
    var displayedLyricFontWeight by remember { mutableIntStateOf(lyricFontWeight) }
    var displayedForceWordByWordLyrics by remember {
        mutableStateOf(forceWordByWordLyrics)
    }
    var displayedLyricBlurEnabled by remember { mutableStateOf(lyricBlurEnabled) }
    var displayedCenterLyrics by remember { mutableStateOf(centerLyrics) }
    var displayedHideControlsOnLyrics by remember {
        mutableStateOf(hideControlsOnLyrics)
    }
    var displayedShowLyricsTranslation by remember {
        mutableStateOf(showLyricsTranslation)
    }
    var lyricsFollowRequestKey by remember { mutableIntStateOf(0) }
    var lyricsSeekRequestKey by remember { mutableIntStateOf(0) }
    var lyricsSeekPositionMs by remember { mutableLongStateOf(playback.positionMs) }
    var lyricsPreviewPositionMs by remember { mutableStateOf<Long?>(null) }
    val layoutDirection = LocalLayoutDirection.current
    val playerSafeDrawingPadding = WindowInsets.safeDrawing.asPaddingValues()
    val playerForegroundPadding = PaddingValues(
        start = playerSafeDrawingPadding.calculateStartPadding(layoutDirection),
        top = playerSafeDrawingPadding.calculateTopPadding(),
        end = playerSafeDrawingPadding.calculateEndPadding(layoutDirection),
    )
    val playerBottomPadding = playerSafeDrawingPadding.calculateBottomPadding()
    val onTogglePlayPauseFromPlayer = {
        if (!playback.playWhenReady) lyricsFollowRequestKey += 1
        onTogglePlayPause()
    }
    val onSeekFromPlayer: (Long) -> Unit = { targetPositionMs ->
        lyricsSeekPositionMs = targetPositionMs
        lyricsSeekRequestKey += 1
        lyricsPreviewPositionMs = null
        onSeek(targetPositionMs)
    }
    val onPreviewSeekFromPlayer: (Long?) -> Unit = { targetPositionMs ->
        lyricsPreviewPositionMs = targetPositionMs
    }
    LaunchedEffect(item?.contentUri) {
        lyricsPreviewPositionMs = null
        lyricsSeekRequestKey = 0
        lyricsSeekPositionMs = playback.positionMs
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
    LaunchedEffect(forceWordByWordLyrics) {
        displayedForceWordByWordLyrics = forceWordByWordLyrics
    }
    LaunchedEffect(lyricBlurEnabled) {
        displayedLyricBlurEnabled = lyricBlurEnabled
    }
    LaunchedEffect(centerLyrics) {
        displayedCenterLyrics = centerLyrics
    }
    LaunchedEffect(hideControlsOnLyrics) {
        displayedHideControlsOnLyrics = hideControlsOnLyrics
    }
    LaunchedEffect(showLyricsTranslation) {
        displayedShowLyricsTranslation = showLyricsTranslation
    }
    val artworkPageSelected = pagerState.settledPage == 0
    SideEffect {
        onArtworkPageSelectedChanged(artworkPageSelected)
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
            .onGloballyPositioned { coordinates ->
                onPlayerBoundsChanged(coordinates.boundsInRoot())
            }
            .recordPlayerLayer(
                layer = playerLayer,
                drawInPlace = drawInPlace,
            ),
        containerColor = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(dismissGestureModifier),
        ) {
            when (playbackBackgroundStyle) {
                PlaybackBackgroundStyle.BLURRED_ARTWORK -> BlurredArtworkBackground(
                    contentUri = item?.contentUri.orEmpty(),
                    dateModifiedEpochSeconds = item?.dateModifiedEpochSeconds ?: 0L,
                    fileSizeBytes = item?.fileSizeBytes ?: 0L,
                    animate = drawInPlace && playback.isPlaying,
                    animateArtworkTransition = true,
                    onStatusBarBackgroundDarkChanged = onStatusBarBackgroundDarkChanged,
                    modifier = Modifier.fillMaxSize(),
                )

                PlaybackBackgroundStyle.FLOWING_COLORS -> ArtworkFlowBackground(
                    artwork = artworkBlend.currentBitmap,
                    isDark = isDark,
                    animate = drawInPlace && playback.isPlaying,
                    animateColorTransition = drawInPlace,
                    onStatusBarBackgroundDarkChanged = onStatusBarBackgroundDarkChanged,
                    modifier = Modifier.fillMaxSize(),
                )
            }
    Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(playerForegroundPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val lyricsCenterOffsetY = if (displayedHideControlsOnLyrics) {
                        (-(
                            playerForegroundPadding.calculateTopPadding().value +
                                PLAYER_HEADER_TOP_PADDING.value +
                                PLAYER_HEADER_CONTENT_HEIGHT.value +
                                PLAYER_HEADER_TO_CONTENT_SPACING.value
                            ) / 2f).dp
                    } else {
                        0.dp
                    }
                    PlayerHeader(
                        item = item,
                        titleColor = emphasisControlColor,
                        artistColor = artistControlColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 32.dp,
                                top = PLAYER_HEADER_TOP_PADDING,
                                end = 32.dp,
                            ),
                    )
                    Spacer(Modifier.height(PLAYER_HEADER_TO_CONTENT_SPACING))
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        val landscape = maxWidth >= 600.dp && maxWidth > maxHeight
                        if (landscape) {
                            val artworkSize = minOf(
                                maxHeight - playerBottomPadding - 24.dp,
                                (maxWidth - 32.dp) / 2f,
                                292.dp,
                            )
                            if (displayedHideControlsOnLyrics) {
                                PlayerContentPager(
                                    pagerState = pagerState,
                                    lyrics = lyrics,
                                    positionMs = playback.positionMs,
                                    previewPositionMs = lyricsPreviewPositionMs,
                                    isPlaying = playback.isPlaying,
                                    onSeek = onSeekFromPlayer,
                                    contentWidth = artworkSize,
                                    controlColor = controlColor,
                                    emphasisControlColor = emphasisControlColor,
                                    lyricsPagingEnabled = lyricsPagingEnabled,
                                    lyricFontScale = displayedLyricFontScale,
                                    lyricFontWeight = displayedLyricFontWeight,
                                    forceWordByWordLyrics = displayedForceWordByWordLyrics,
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
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(
                                                    top = 12.dp,
                                                    bottom = playerBottomPadding + 12.dp,
                                                ),
                                            horizontalArrangement = Arrangement.spacedBy(32.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxSize(),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                PlayerArtwork(
                                                    size = artworkSize,
                                                    playWhenReady = playback.playWhenReady,
                                                    artworkBlend = artworkBlend,
                                                    cornerRadius = artworkCornerRadius,
                                                    sharedArtworkVisible = sharedArtworkVisible,
                                                    onArtworkBoundsChanged = onArtworkBoundsChanged,
                                                )
                                            }
                                            PlayerDetails(
                                                modifier = Modifier.width(artworkSize),
                                                contentWidth = artworkSize,
                                                playback = playback,
                                                controlColor = controlColor,
                                                emphasisControlColor = emphasisControlColor,
                                                onTogglePlayPause = onTogglePlayPauseFromPlayer,
                                                onPrevious = onPrevious,
                                                onNext = onNext,
                                                onSeek = onSeekFromPlayer,
                                                onPreviewPositionChange = onPreviewSeekFromPlayer,
                                                onCyclePlaybackMode = onCyclePlaybackMode,
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
                                        }
                                    },
                                )
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(
                                            top = 12.dp,
                                            bottom = playerBottomPadding + 12.dp,
                                        ),
                                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    PlayerContentPager(
                                        pagerState = pagerState,
                                        lyrics = lyrics,
                                        positionMs = playback.positionMs,
                                        previewPositionMs = lyricsPreviewPositionMs,
                                        isPlaying = playback.isPlaying,
                                        onSeek = onSeekFromPlayer,
                                        contentWidth = artworkSize,
                                        controlColor = controlColor,
                                        emphasisControlColor = emphasisControlColor,
                                        lyricsPagingEnabled = lyricsPagingEnabled,
                                        lyricFontScale = displayedLyricFontScale,
                                        lyricFontWeight = displayedLyricFontWeight,
                                        forceWordByWordLyrics = displayedForceWordByWordLyrics,
                                        lyricBlurEnabled = displayedLyricBlurEnabled,
                                        centerLyrics = displayedCenterLyrics,
                                        lyricCenterOffsetY = lyricsCenterOffsetY,
                                        showLyricsTranslation = displayedShowLyricsTranslation,
                                        showBottomFade = true,
                                        resumeFollowRequestKey = lyricsFollowRequestKey,
                                        seekRequestKey = lyricsSeekRequestKey,
                                        seekPositionMs = lyricsSeekPositionMs,
                                        modifier = Modifier.weight(1f),
                                        artworkContent = {
                                            PlayerArtwork(
                                                size = artworkSize,
                                                playWhenReady = playback.playWhenReady,
                                                artworkBlend = artworkBlend,
                                                cornerRadius = artworkCornerRadius,
                                                sharedArtworkVisible = sharedArtworkVisible,
                                                onArtworkBoundsChanged = onArtworkBoundsChanged,
                                            )
                                        },
                                    )
                                    PlayerDetails(
                                        modifier = Modifier.width(artworkSize),
                                        contentWidth = artworkSize,
                                        playback = playback,
                                        controlColor = controlColor,
                                        emphasisControlColor = emphasisControlColor,
                                        onTogglePlayPause = onTogglePlayPauseFromPlayer,
                                        onPrevious = onPrevious,
                                        onNext = onNext,
                                        onSeek = onSeekFromPlayer,
                                        onPreviewPositionChange = onPreviewSeekFromPlayer,
                                        onCyclePlaybackMode = onCyclePlaybackMode,
                                        onOpenLyricsSettings = { showLyricsSettings = true },
                                        onOpenQueue = onOpenQueue,
                                        onOpenTrackActions = {
                                            if (currentTrack != null) showTrackActions = true
                                        },
                                    )
                                }
                            }
                        } else {
                            val artworkSize = minOf(
                                maxWidth - 56.dp,
                                (maxHeight - playerBottomPadding) * 0.58f,
                            ).coerceAtMost(344.dp)
                            if (displayedHideControlsOnLyrics) {
                                PlayerContentPager(
                                    pagerState = pagerState,
                                    lyrics = lyrics,
                                    positionMs = playback.positionMs,
                                    previewPositionMs = lyricsPreviewPositionMs,
                                    isPlaying = playback.isPlaying,
                                    onSeek = onSeekFromPlayer,
                                    contentWidth = artworkSize,
                                    controlColor = controlColor,
                                    emphasisControlColor = emphasisControlColor,
                                    lyricsPagingEnabled = lyricsPagingEnabled,
                                    lyricFontScale = displayedLyricFontScale,
                                    lyricFontWeight = displayedLyricFontWeight,
                                    forceWordByWordLyrics = displayedForceWordByWordLyrics,
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
                                            artworkContent = {
                                                PlayerArtwork(
                                                    size = artworkSize,
                                                    playWhenReady = playback.playWhenReady,
                                                    artworkBlend = artworkBlend,
                                                    cornerRadius = artworkCornerRadius,
                                                    sharedArtworkVisible = sharedArtworkVisible,
                                                    onArtworkBoundsChanged = onArtworkBoundsChanged,
                                                )
                                            },
                                            detailsContent = {
                                                PlayerDetails(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    contentWidth = artworkSize,
                                                    playback = playback,
                                                    controlColor = controlColor,
                                                    emphasisControlColor = emphasisControlColor,
                                                    onTogglePlayPause =
                                                        onTogglePlayPauseFromPlayer,
                                                    onPrevious = onPrevious,
                                                    onNext = onNext,
                                                    onSeek = onSeekFromPlayer,
                                                    onPreviewPositionChange = onPreviewSeekFromPlayer,
                                                    onCyclePlaybackMode = onCyclePlaybackMode,
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
                                    artworkContent = {
                                        PlayerContentPager(
                                            pagerState = pagerState,
                                            lyrics = lyrics,
                                            positionMs = playback.positionMs,
                                            previewPositionMs = lyricsPreviewPositionMs,
                                            isPlaying = playback.isPlaying,
                                            onSeek = onSeekFromPlayer,
                                            contentWidth = artworkSize,
                                            controlColor = controlColor,
                                            emphasisControlColor = emphasisControlColor,
                                            lyricsPagingEnabled = lyricsPagingEnabled,
                                            lyricFontScale = displayedLyricFontScale,
                                            lyricFontWeight = displayedLyricFontWeight,
                                            forceWordByWordLyrics =
                                                displayedForceWordByWordLyrics,
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
                                                    size = artworkSize,
                                                    playWhenReady = playback.playWhenReady,
                                                    artworkBlend = artworkBlend,
                                                    cornerRadius = artworkCornerRadius,
                                                    sharedArtworkVisible = sharedArtworkVisible,
                                                    onArtworkBoundsChanged =
                                                        onArtworkBoundsChanged,
                                                )
                                            },
                                        )
                                    },
                                    detailsContent = {
                                        PlayerDetails(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentWidth = artworkSize,
                                            playback = playback,
                                            controlColor = controlColor,
                                            emphasisControlColor = emphasisControlColor,
                                            onTogglePlayPause = onTogglePlayPauseFromPlayer,
                                            onPrevious = onPrevious,
                                            onNext = onNext,
                                            onSeek = onSeekFromPlayer,
                                            onPreviewPositionChange = onPreviewSeekFromPlayer,
                                            onCyclePlaybackMode = onCyclePlaybackMode,
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
            onGoToAlbum = onGoToAlbum,
            artistGroups = artistGroups,
            onGoToArtist = onGoToArtist,
            onExternalEditReturned = onExternalEditReturned,
        )
        LyricsSettingsSheet(
            show = showLyricsSettings,
            lyricFontScale = displayedLyricFontScale,
            lyricFontWeight = displayedLyricFontWeight,
            forceWordByWordLyrics = displayedForceWordByWordLyrics,
            lyricBlurEnabled = displayedLyricBlurEnabled,
            centerLyrics = displayedCenterLyrics,
            hideControlsOnLyrics = displayedHideControlsOnLyrics,
            showLyricsTranslation = displayedShowLyricsTranslation,
            onDismiss = { showLyricsSettings = false },
            onLyricFontScalePreview = { displayedLyricFontScale = it },
            onLyricFontScaleCommit = {
                onLyricFontScaleChange(displayedLyricFontScale)
            },
            onLyricFontWeightPreview = { displayedLyricFontWeight = it },
            onLyricFontWeightCommit = {
                onLyricFontWeightChange(displayedLyricFontWeight)
            },
            onForceWordByWordLyricsChange = {
                displayedForceWordByWordLyrics = it
                onForceWordByWordLyricsChange(it)
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
    }
}

private data class ArtworkBlend(
    val previousBitmap: Bitmap?,
    val currentBitmap: Bitmap?,
    val progress: Float,
    val hasPreviousFrame: Boolean,
)

@Composable
private fun rememberArtworkBlend(
    targetBitmap: Bitmap?,
    animate: Boolean,
): ArtworkBlend {
    var previousBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentBitmap by remember { mutableStateOf(targetBitmap) }
    var hasPreviousFrame by remember { mutableStateOf(false) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(targetBitmap, animate) {
        if (!animate) {
            previousBitmap = null
            currentBitmap = targetBitmap
            hasPreviousFrame = false
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        if (targetBitmap === currentBitmap && !hasPreviousFrame) {
            return@LaunchedEffect
        }
        previousBitmap = if (progress.value < 0.5f && hasPreviousFrame) {
            previousBitmap
        } else {
            currentBitmap
        }
        currentBitmap = targetBitmap
        hasPreviousFrame = true
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = PLAYER_TRACK_ARTWORK_CROSSFADE_DURATION_MILLIS,
                easing = PLAYER_TRACK_ARTWORK_CROSSFADE_EASING,
            ),
        )
        hasPreviousFrame = false
        previousBitmap = null
    }

    return ArtworkBlend(
        previousBitmap = previousBitmap,
        currentBitmap = currentBitmap,
        progress = progress.value,
        hasPreviousFrame = hasPreviousFrame,
    )
}

@Composable
private fun PortraitPlayerLayout(
    modifier: Modifier = Modifier,
    artworkContent: @Composable () -> Unit,
    detailsContent: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.padding(bottom = PLAYER_PANEL_BOTTOM_SPACING),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            artworkContent()
        }
        Spacer(Modifier.height(PLAYER_ARTWORK_PROGRESS_SPACING))
        detailsContent()
    }
}

@Composable
private fun PlayerContentPager(
    pagerState: PagerState,
    lyrics: LyricsUiState,
    positionMs: Long,
    previewPositionMs: Long?,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    contentWidth: Dp,
    controlColor: Color,
    emphasisControlColor: Color,
    lyricsPagingEnabled: Boolean,
    lyricFontScale: Float,
    lyricFontWeight: Int,
    forceWordByWordLyrics: Boolean,
    lyricBlurEnabled: Boolean,
    centerLyrics: Boolean,
    lyricCenterOffsetY: Dp,
    showLyricsTranslation: Boolean,
    showBottomFade: Boolean,
    resumeFollowRequestKey: Int,
    seekRequestKey: Int,
    seekPositionMs: Long,
    artworkContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    HorizontalPager(
        state = pagerState,
        userScrollEnabled = lyricsPagingEnabled,
        modifier = modifier.clipToBounds(),
        beyondViewportPageCount = 1,
        verticalAlignment = Alignment.CenterVertically,
        key = { it },
    ) { page ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (page == 0) {
                artworkContent()
            } else {
                SyncedLyrics(
                    lyrics = lyrics,
                    positionMs = positionMs,
                    previewPositionMs = previewPositionMs,
                    isPlaying = isPlaying,
                    onSeek = onSeek,
                    contentWidth = contentWidth,
                    controlColor = controlColor,
                    emphasisControlColor = emphasisControlColor,
                    lyricFontScale = lyricFontScale,
                    lyricFontWeight = lyricFontWeight,
                    forceWordByWordLyrics = forceWordByWordLyrics,
                    lyricBlurEnabled = lyricBlurEnabled,
                    centerLyrics = centerLyrics,
                    lyricCenterOffsetY = lyricCenterOffsetY,
                    showLyricsTranslation = showLyricsTranslation,
                    showBottomFade = showBottomFade,
                    resumeFollowRequestKey = resumeFollowRequestKey,
                    seekRequestKey = seekRequestKey,
                    seekPositionMs = seekPositionMs,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun SyncedLyrics(
    lyrics: LyricsUiState,
    positionMs: Long,
    previewPositionMs: Long?,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    contentWidth: Dp,
    controlColor: Color,
    emphasisControlColor: Color,
    lyricFontScale: Float,
    lyricFontWeight: Int,
    forceWordByWordLyrics: Boolean,
    lyricBlurEnabled: Boolean,
    centerLyrics: Boolean,
    lyricCenterOffsetY: Dp,
    showLyricsTranslation: Boolean,
    showBottomFade: Boolean,
    resumeFollowRequestKey: Int,
    seekRequestKey: Int,
    seekPositionMs: Long,
    modifier: Modifier = Modifier,
) {
    var displayedDocument by remember { mutableStateOf<LyricsDocument?>(null) }
    val lyricsAlpha = remember { Animatable(0f) }

    LaunchedEffect(lyrics) {
        when (lyrics) {
            LyricsUiState.Loading -> {
                if (displayedDocument != null) {
                    lyricsAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(180),
                    )
                }
            }

            LyricsUiState.Unavailable -> {
                if (displayedDocument != null) {
                    lyricsAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(180),
                    )
                    displayedDocument = null
                }
            }

            is LyricsUiState.Available -> {
                val incomingDocument = lyrics.document
                if (displayedDocument != incomingDocument) {
                    if (displayedDocument != null) {
                        lyricsAlpha.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(180),
                        )
                    }
                    displayedDocument = incomingDocument
                    lyricsAlpha.snapTo(0f)
                }
                lyricsAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(240),
                )
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (displayedDocument != null) {
            displayedDocument?.let { document ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = lyricsAlpha.value },
                ) {
                    LyricsView(
                        document = document,
                        positionMs = positionMs,
                        previewPositionMs = previewPositionMs,
                        isPlaying = isPlaying,
                        onSeek = onSeek,
                        contentWidth = contentWidth,
                        lyricFontScale = lyricFontScale,
                        lyricFontWeight = lyricFontWeight,
                        forceWordByWordLyrics = forceWordByWordLyrics,
                        lyricBlurEnabled = lyricBlurEnabled,
                        centerLyrics = centerLyrics,
                        centerOffsetY = lyricCenterOffsetY,
                        showLyricsTranslation = showLyricsTranslation,
                        showBottomFade = showBottomFade,
                        resumeFollowRequestKey = resumeFollowRequestKey,
                        seekRequestKey = seekRequestKey,
                        seekPositionMs = seekPositionMs,
                        emphasisColor = emphasisControlColor,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        } else if (lyrics is LyricsUiState.Unavailable) {
            Text(
                text = stringResource(R.string.lyrics_unavailable),
                modifier = Modifier.width(contentWidth),
                style = MiuixTheme.textStyles.title3.copy(
                    fontSize = (LYRIC_PRIMARY_FONT_SIZE_SP * lyricFontScale).sp,
                    lineHeight = (LYRIC_PRIMARY_LINE_HEIGHT_SP * lyricFontScale).sp,
                    fontWeight = FontWeight(lyricFontWeight.coerceIn(1, 1000)),
                    fontSynthesis = FontSynthesis.None,
                ),
                color = controlColor.copy(alpha = 0.72f),
                textAlign = if (centerLyrics) TextAlign.Center else TextAlign.Start,
            )
        }
    }
}

@Composable
private fun LyricsSettingsSheet(
    show: Boolean,
    lyricFontScale: Float,
    lyricFontWeight: Int,
    forceWordByWordLyrics: Boolean,
    lyricBlurEnabled: Boolean,
    centerLyrics: Boolean,
    hideControlsOnLyrics: Boolean,
    showLyricsTranslation: Boolean,
    onDismiss: () -> Unit,
    onLyricFontScalePreview: (Float) -> Unit,
    onLyricFontScaleCommit: () -> Unit,
    onLyricFontWeightPreview: (Int) -> Unit,
    onLyricFontWeightCommit: () -> Unit,
    onForceWordByWordLyricsChange: (Boolean) -> Unit,
    onLyricBlurEnabledChange: (Boolean) -> Unit,
    onCenterLyricsChange: (Boolean) -> Unit,
    onHideControlsOnLyricsChange: (Boolean) -> Unit,
    onShowLyricsTranslationChange: (Boolean) -> Unit,
) {
    val bottomPadding = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    OverlayBottomSheet(
        show = show,
        title = stringResource(R.string.lyrics_settings),
        enableWindowDim = true,
        onDismissRequest = onDismiss,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = bottomPadding + 12.dp),
            colors = CardDefaults.defaultColors(
                color = MiuixTheme.colorScheme.secondaryContainer,
            ),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                SwitchPreference(
                    title = stringResource(R.string.lyrics_translation),
                    checked = showLyricsTranslation,
                    onCheckedChange = onShowLyricsTranslationChange,
                )
                SliderPreference(
                    value = lyricFontScale,
                    onValueChange = onLyricFontScalePreview,
                    title = stringResource(R.string.lyrics_size),
                    valueText = stringResource(
                        R.string.lyrics_size_value,
                        (lyricFontScale * 100f).roundToInt(),
                    ),
                    valueRange = MIN_LYRIC_FONT_SCALE..MAX_LYRIC_FONT_SCALE,
                    onValueChangeFinished = onLyricFontScaleCommit,
                    showKeyPoints = true,
                    keyPoints = listOf(
                        MIN_LYRIC_FONT_SCALE,
                        DEFAULT_LYRIC_FONT_SCALE,
                        MAX_LYRIC_FONT_SCALE,
                    ),
                )
                SliderPreference(
                    value = lyricFontWeight.toFloat(),
                    onValueChange = { value ->
                        onLyricFontWeightPreview(value.roundToInt())
                    },
                    title = stringResource(R.string.lyrics_weight),
                    valueText = stringResource(
                        R.string.lyrics_weight_value,
                        lyricFontWeight,
                    ),
                    valueRange = MIN_LYRIC_FONT_WEIGHT.toFloat()..
                        MAX_LYRIC_FONT_WEIGHT.toFloat(),
                    steps = LYRICS_FONT_WEIGHT_STEP_COUNT,
                    onValueChangeFinished = onLyricFontWeightCommit,
                    showKeyPoints = true,
                )
                SwitchPreference(
                    title = stringResource(R.string.lyrics_center),
                    checked = centerLyrics,
                    onCheckedChange = onCenterLyricsChange,
                )
                SwitchPreference(
                    title = stringResource(R.string.lyrics_blur),
                    summary = stringResource(R.string.lyrics_blur_summary),
                    checked = lyricBlurEnabled,
                    onCheckedChange = onLyricBlurEnabledChange,
                )
                SwitchPreference(
                    title = stringResource(R.string.lyrics_hide_controls),
                    checked = hideControlsOnLyrics,
                    onCheckedChange = onHideControlsOnLyricsChange,
                )
                SwitchPreference(
                    title = stringResource(R.string.lyrics_force_word_by_word),
                    summary = stringResource(R.string.lyrics_force_word_by_word_summary),
                    checked = forceWordByWordLyrics,
                    onCheckedChange = onForceWordByWordLyricsChange,
                )
            }
        }
    }
}

@Composable
private fun PlayerArtwork(
    size: Dp,
    playWhenReady: Boolean,
    artworkBlend: ArtworkBlend,
    cornerRadius: Dp,
    sharedArtworkVisible: Boolean,
    onArtworkBoundsChanged: (Rect) -> Unit,
) {
    val artworkPadding by animateDpAsState(
        targetValue = if (playWhenReady) {
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
    val artworkContainerSize = size + PLAYER_ARTWORK_CONTAINER_EXPANSION
    val artworkContentSize =
        (artworkContainerSize - artworkPadding * 2f).coerceAtLeast(0.dp)
    val density = LocalDensity.current
    var artworkLayoutBounds by remember { mutableStateOf(Rect.Zero) }
    SideEffect {
        if (artworkLayoutBounds.width > 0f && artworkLayoutBounds.height > 0f) {
            val insetArtworkBounds = artworkInsetRect(
                bounds = artworkLayoutBounds,
                inset = with(density) { artworkPadding.toPx() },
            )
            val visibleArtworkBounds = artworkBlend.currentBitmap?.let { bitmap ->
                fittedArtworkRect(
                    bounds = insetArtworkBounds,
                    bitmapWidth = bitmap.width,
                    bitmapHeight = bitmap.height,
                )
            } ?: insetArtworkBounds
            onArtworkBoundsChanged(visibleArtworkBounds)
        }
    }
    Box(
        modifier = Modifier
            .size(artworkContainerSize)
            .onGloballyPositioned { coordinates ->
                artworkLayoutBounds = coordinates.boundsInRoot()
            }
            .graphicsLayer {
                alpha = if (sharedArtworkVisible) 1f else 0f
                clip = false
            },
        contentAlignment = Alignment.Center,
    ) {
        if (artworkBlend.hasPreviousFrame && artworkBlend.progress < 1f) {
            PlaybackArtworkFrame(
                bitmap = artworkBlend.previousBitmap,
                size = artworkContentSize,
                cornerRadius = cornerRadius,
                modifier = Modifier,
                contentScale = ContentScale.Fit,
                useSquircleClip = true,
                drawArtworkShadow = true,
                artworkAlpha = 1f - artworkBlend.progress,
            )
        }
        PlaybackArtworkFrame(
            bitmap = artworkBlend.currentBitmap,
            size = artworkContentSize,
            cornerRadius = cornerRadius,
            modifier = Modifier,
            contentScale = ContentScale.Fit,
            useSquircleClip = true,
            drawArtworkShadow = true,
            artworkAlpha = artworkBlend.progress,
        )
    }
}

private data class PlayerHeaderContent(
    val trackKey: String?,
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
    titleColor: Color,
    artistColor: Color,
    modifier: Modifier = Modifier,
) {
    val header = PlayerHeaderContent(
        trackKey = item?.contentUri,
        title = item?.title ?: stringResource(R.string.no_track_selected),
        artist = displayArtistName(item?.artist)
            ?: stringResource(R.string.music_unknown_artist),
    )
    AnimatedContent(
        targetState = header,
        modifier = modifier,
        transitionSpec = {
            fadeIn(tween(180)).togetherWith(fadeOut(tween(140)))
        },
        label = "playerHeaderTrack",
    ) { target ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PLAYER_HEADER_TITLE_SLOT_HEIGHT),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = target.title,
                    modifier = Modifier.fillMaxWidth(),
                    style = MiuixTheme.textStyles.title3.copy(
                        lineHeight = PLAYER_HEADER_TITLE_LINE_HEIGHT,
                    ),
                    color = titleColor,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PLAYER_HEADER_ARTIST_SLOT_HEIGHT),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = playerHeaderArtistText(target.artist),
                    modifier = Modifier.fillMaxWidth(),
                    style = MiuixTheme.textStyles.body2.copy(
                        fontSize = 14.sp,
                        lineHeight = PLAYER_HEADER_ARTIST_LINE_HEIGHT,
                    ),
                    color = artistColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PlayerDetails(
    contentWidth: Dp,
    playback: PlaybackUiState,
    controlColor: Color,
    emphasisControlColor: Color,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onPreviewPositionChange: (Long?) -> Unit,
    onCyclePlaybackMode: () -> Unit,
    onOpenLyricsSettings: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenTrackActions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .width(contentWidth)
                    .zIndex(1f),
            ) {
                PlayerProgress(
                    positionMs = playback.positionMs,
                    durationMs = playback.durationMs,
                    enabled = playback.currentItem != null,
                    indicatorColor = emphasisControlColor,
                    labelColor = controlColor,
                    onSeek = onSeek,
                    onPreviewPositionChange = onPreviewPositionChange,
                )
            }
            Row(
                modifier = Modifier.width(minOf(contentWidth, 320.dp)),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onPrevious,
                    minWidth = 64.dp,
                    minHeight = 64.dp,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_player_previous_track),
                        contentDescription = stringResource(R.string.previous_track),
                        modifier = Modifier.size(26.dp),
                        tint = emphasisControlColor,
                    )
                }
                AnimatedPlayPauseButton(
                    playWhenReady = playback.playWhenReady,
                    tint = emphasisControlColor,
                    onClick = onTogglePlayPause,
                )
                IconButton(
                    onClick = onNext,
                    minWidth = 64.dp,
                    minHeight = 64.dp,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_player_next_track),
                        contentDescription = stringResource(R.string.next_track),
                        modifier = Modifier.size(26.dp),
                        tint = emphasisControlColor,
                    )
                }
            }
            Spacer(Modifier.height(PLAYER_CONTROL_GROUP_SPACING))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlaybackModeButton(
                    mode = playback.playbackMode,
                    tint = controlColor,
                    onClick = onCyclePlaybackMode,
                )
                PlayerIconButton(
                    icon = MiuixIcons.ConvertFile,
                    description = stringResource(R.string.lyrics_settings_open),
                    tint = controlColor,
                    onClick = onOpenLyricsSettings,
                )
                PlayerIconButton(
                    icon = MiuixIcons.Playlist,
                    description = stringResource(R.string.open_queue),
                    tint = controlColor,
                    onClick = onOpenQueue,
                )
                PlayerIconButton(
                    icon = MiuixIcons.More,
                    description = stringResource(R.string.track_actions),
                    size = 24.dp,
                    tint = controlColor,
                    onClick = onOpenTrackActions,
                )
            }
        }
    }
}

@Composable
private fun AnimatedPlayPauseButton(
    playWhenReady: Boolean,
    tint: Color,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        minWidth = 80.dp,
        minHeight = 80.dp,
    ) {
        AnimatedContent(
            targetState = playWhenReady,
            transitionSpec = { playerControlIconTransition() },
            label = "playPauseIcon",
        ) { playing ->
            Icon(
                painter = painterResource(
                    if (playing) R.drawable.ic_player_pause else R.drawable.ic_player_play,
                ),
                contentDescription = stringResource(if (playing) R.string.pause else R.string.play),
                modifier = Modifier.size(42.dp),
                tint = tint,
            )
        }
    }
}

@Composable
private fun PlaybackModeButton(
    mode: PlaybackMode,
    tint: Color,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        minWidth = 36.dp,
        minHeight = 36.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (mode) {
                PlaybackMode.ORDER -> Icon(
                    imageVector = MiuixIcons.Replace,
                    contentDescription = stringResource(R.string.playback_mode_order),
                    modifier = Modifier
                        .size(26.dp)
                        .graphicsLayer { scaleX = -1f },
                    tint = tint,
                )
                PlaybackMode.REPEAT_ONE -> Icon(
                    imageVector = MiuixIcons.Replace,
                    contentDescription = stringResource(R.string.playback_mode_repeat_one),
                    modifier = Modifier
                        .size(26.dp)
                        .graphicsLayer { scaleX = -1f },
                    tint = tint,
                )
                PlaybackMode.RANDOM -> Icon(
                    painter = painterResource(R.drawable.ic_player_shuffle),
                    contentDescription = stringResource(R.string.playback_mode_random),
                    modifier = Modifier.size(22.dp),
                    tint = tint,
                )
            }
            if (mode == PlaybackMode.REPEAT_ONE) {
                Text(
                    text = stringResource(R.string.playback_mode_one_badge),
                    style = MiuixTheme.textStyles.footnote2,
                    color = tint,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun PlayerProgress(
    positionMs: Long,
    durationMs: Long,
    enabled: Boolean,
    indicatorColor: Color,
    labelColor: Color,
    onSeek: (Long) -> Unit,
    onPreviewPositionChange: (Long?) -> Unit,
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    val interactionEnabled = enabled && durationMs > 0L
    var seekPositionMs by remember { mutableFloatStateOf(positionMs.toFloat()) }
    var isSeeking by remember { mutableStateOf(false) }
    val currentOnSeek by rememberUpdatedState(onSeek)
    val currentOnPreviewPositionChange by rememberUpdatedState(onPreviewPositionChange)
    LaunchedEffect(positionMs, durationMs, isSeeking) {
        if (!isSeeking) {
            seekPositionMs = positionMs.coerceIn(0L, safeDuration).toFloat()
        }
    }
    val displayedPosition = if (isSeeking) {
        seekPositionMs
    } else {
        positionMs.coerceIn(0L, safeDuration).toFloat()
    }
    val progress = (displayedPosition / safeDuration.toFloat()).coerceIn(0f, 1f)
    val indicatorHeight by animateDpAsState(
        targetValue = if (isSeeking) 12.dp else PLAYER_PROGRESS_IDLE_HEIGHT,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 550f),
        label = "playerProgressPressedHeight",
    )
    val indicatorWidthExpansion by animateDpAsState(
        targetValue = if (isSeeking) 8.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 550f),
        label = "playerProgressPressedWidth",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(PLAYER_PROGRESS_LAYOUT_HEIGHT),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(PLAYER_PROGRESS_TOUCH_HEIGHT)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                    if (interactionEnabled) {
                        setProgress { targetProgress ->
                            seekPositionMs =
                                targetProgress.coerceIn(0f, 1f) * safeDuration.toFloat()
                            currentOnSeek(seekPositionMs.roundToLong())
                            true
                        }
                    }
                }
                .pointerInput(interactionEnabled, safeDuration) {
                    if (!interactionEnabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        fun updatePosition(horizontalPosition: Float): Long {
                            val fraction = if (size.width == 0) {
                                0f
                            } else {
                                (horizontalPosition / size.width).coerceIn(0f, 1f)
                            }
                            seekPositionMs = fraction * safeDuration.toFloat()
                            return seekPositionMs.roundToLong()
                        }
                        isSeeking = true
                        updatePosition(down.position.x)
                        down.consume()
                        var completed = false
                        var isDragging = false
                        try {
                            completed = drag(down.id) { change ->
                                val targetPositionMs = updatePosition(change.position.x)
                                if (
                                    !isDragging &&
                                        progressGestureIsDrag(
                                            horizontalDistancePx =
                                                change.position.x - down.position.x,
                                            touchSlopPx = viewConfiguration.touchSlop,
                                        )
                                ) {
                                    isDragging = true
                                }
                                if (isDragging) {
                                    currentOnPreviewPositionChange(targetPositionMs)
                                }
                                change.consume()
                            }
                            if (completed) currentOnSeek(seekPositionMs.roundToLong())
                        } finally {
                            if (!completed || !isDragging) {
                                currentOnPreviewPositionChange(null)
                            }
                            isSeeking = false
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            LinearProgressIndicator(
                progress = progress,
                colors = ProgressIndicatorDefaults.progressIndicatorColors(
                    foregroundColor = if (enabled) {
                        indicatorColor
                    } else {
                        indicatorColor.copy(alpha = indicatorColor.alpha * 0.55f)
                    },
                    disabledForegroundColor =
                        indicatorColor.copy(alpha = indicatorColor.alpha * 0.55f),
                    backgroundColor =
                        indicatorColor.copy(alpha = indicatorColor.alpha * 0.28f),
                ),
                height = indicatorHeight,
                modifier = Modifier
                    .width(maxWidth + indicatorWidthExpansion),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = PLAYER_PROGRESS_LABEL_OFFSET),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDuration(displayedPosition.roundToLong()),
                style = MiuixTheme.textStyles.footnote1,
                color = labelColor,
            )
            Text(
                text = formatDuration(durationMs),
                style = MiuixTheme.textStyles.footnote1,
                color = labelColor,
            )
        }
    }
}

@Composable
private fun PlayerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    size: Dp = 26.dp,
    tint: Color = MiuixTheme.colorScheme.onSurface,
) {
    IconButton(
        onClick = onClick,
        minWidth = 36.dp,
        minHeight = 36.dp,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(size),
            tint = tint,
        )
    }
}

internal fun progressGestureIsDrag(
    horizontalDistancePx: Float,
    touchSlopPx: Float,
): Boolean = abs(horizontalDistancePx) >= touchSlopPx

// 歌词设置区域：最小歌词字号缩放（70%）。
private const val MIN_LYRIC_FONT_SCALE = 0.7f
// 歌词设置区域：默认歌词字号缩放（100%）。
private const val DEFAULT_LYRIC_FONT_SCALE = 1f
// 歌词设置区域：最大歌词字号缩放（130%）。
private const val MAX_LYRIC_FONT_SCALE = 1.3f
// 歌词设置区域：最小歌词字重。
private const val MIN_LYRIC_FONT_WEIGHT = 100
// 歌词设置区域：最大歌词字重。
private const val MAX_LYRIC_FONT_WEIGHT = 900
// 歌词设置区域：100 至 900 字重的可选档位数。
private const val LYRICS_FONT_WEIGHT_STEP_COUNT = 7

// 播放页封面区域：播放状态下封面四周内边距。
private val PLAYER_ARTWORK_PLAYING_PADDING = 6.dp
// 播放页封面区域：暂停状态下封面四周内边距。
private val PLAYER_ARTWORK_PAUSED_PADDING = 30.dp
// 播放页封面区域：外层容器相对封面的宽高总扩展量。
private val PLAYER_ARTWORK_CONTAINER_EXPANSION = 12.dp

// 播放页标题区：安全区以下的顶部间距。
private val PLAYER_HEADER_TOP_PADDING = 16.dp
// 播放页标题区：标题区到封面内容区的间距。
private val PLAYER_HEADER_TO_CONTENT_SPACING = 12.dp
// 播放页标题区：歌名文本行高。
private val PLAYER_HEADER_TITLE_LINE_HEIGHT = 32.sp
// 播放页标题区：歌手名文本行高。
private val PLAYER_HEADER_ARTIST_LINE_HEIGHT = 20.sp
// 播放页标题区：歌名固定单行槽位高度。
private val PLAYER_HEADER_TITLE_SLOT_HEIGHT = 32.dp
// 播放页标题区：歌手名固定单行槽位高度。
private val PLAYER_HEADER_ARTIST_SLOT_HEIGHT = 20.dp
// 播放页标题区：歌名与歌手名槽位之间保留 1 dp 间隔。
private val PLAYER_HEADER_CONTENT_HEIGHT =
    PLAYER_HEADER_TITLE_SLOT_HEIGHT + 1.dp + PLAYER_HEADER_ARTIST_SLOT_HEIGHT

// 播放页控制区：封面底部到进度条的间距。
private val PLAYER_ARTWORK_PROGRESS_SPACING = 16.dp
// 播放页控制区：进度条实际下边缘到主控制行的间距。
private val PLAYER_PROGRESS_TO_PRIMARY_SPACING = 32.dp
// 播放页控制区：主控制行到下方功能按钮行的间距。
private val PLAYER_CONTROL_GROUP_SPACING = 16.dp
// 播放页控制区：底部功能按钮行到面板底边的间距。
private val PLAYER_PANEL_BOTTOM_SPACING = 32.dp
// 播放页进度条区域：未触摸时实际指示条高度。
private val PLAYER_PROGRESS_IDLE_HEIGHT = 6.dp
// 播放页进度条区域：拖动进度条的触摸目标高度。
private val PLAYER_PROGRESS_TOUCH_HEIGHT = 26.dp
// 播放页进度条区域：时间文本与实际指示条下边缘的间距。
private val PLAYER_PROGRESS_TIME_SPACING = 8.dp
// 播放页进度条区域：实际指示条下边缘相对触摸目标顶部的偏移。
private val PLAYER_PROGRESS_IDLE_BOTTOM =
    (PLAYER_PROGRESS_TOUCH_HEIGHT + PLAYER_PROGRESS_IDLE_HEIGHT) / 2f
// 播放页进度条区域：时间文本相对触摸目标顶部的偏移。
private val PLAYER_PROGRESS_LABEL_OFFSET =
    PLAYER_PROGRESS_IDLE_BOTTOM + PLAYER_PROGRESS_TIME_SPACING
// 播放页进度条区域：进度条容器总高度，保证主控制行距实际条下边缘 32 dp。
private val PLAYER_PROGRESS_LAYOUT_HEIGHT =
    PLAYER_PROGRESS_IDLE_BOTTOM + PLAYER_PROGRESS_TO_PRIMARY_SPACING
