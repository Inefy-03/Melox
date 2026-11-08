package com.melox.player.ui.screen.playback

import android.graphics.Bitmap
import android.widget.Toast
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
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
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
import androidx.compose.ui.platform.LocalContext
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
import com.melox.player.model.LyricsTranslationLanguage
import com.melox.player.model.LyricAnimationMode
import com.melox.player.R
import com.melox.player.data.library.ArtistGroup
import com.melox.player.data.library.displayArtistName
import com.melox.player.model.MusicTrack
import com.melox.player.model.LyricsUiState
import com.melox.player.model.isDuetForMedia
import com.melox.player.model.LyricsDocument
import com.melox.player.model.PlaybackMode
import com.melox.player.model.PlaybackBackgroundStyle
import com.melox.player.model.PlaybackQueueItem
import com.melox.player.model.PlaybackUiState
import com.melox.player.model.SleepTimerState
import com.melox.player.model.withTrackMetadata
import com.melox.player.ui.usesMiuixSmallTopAppBar
import com.melox.player.ui.screen.settings.LyricsInterfacePreferences
import com.melox.player.ui.screen.settings.LyricsTranslationPreferences
import com.melox.player.ui.isMiuixWideLayout
import com.melox.player.ui.component.library.PlaybackArtworkFrame
import com.melox.player.ui.component.library.PLAYBACK_ARTWORK_SHADOW_BLUR_RADIUS
import com.melox.player.ui.component.library.TrackActionsOverlay
import com.melox.player.ui.component.bottomSheetCardColor
import com.melox.player.ui.component.bottomSheetGlassModifier
import com.melox.player.ui.component.bottomSheetMaterialColor
import com.melox.player.ui.component.library.formatDuration
import com.melox.player.ui.component.playback.ArtworkTransitionState
import com.melox.player.ui.component.playback.BlurredArtworkBackground
import com.melox.player.ui.component.playback.DynamicFlowBackground
import com.melox.player.ui.component.playback.DynamicFlowBackgroundState
import com.melox.player.ui.component.playback.PLAYER_FULL_ARTWORK_CORNER_RADIUS
import com.melox.player.ui.component.playback.PLAYER_FULL_ARTWORK_REQUEST_SIZE
import com.melox.player.ui.component.playback.PLAYER_TRACK_ARTWORK_CROSSFADE_DURATION_MILLIS
import com.melox.player.ui.component.playback.PLAYER_TRACK_ARTWORK_CROSSFADE_EASING
import com.melox.player.ui.component.playback.rememberPlaybackArtworkResource
import com.melox.player.ui.component.playback.playerControlIconTransition
import com.melox.player.ui.component.playback.recordPlayerLayer
import com.melox.player.ui.component.playback.WeightedCrossfadeFrame
import com.melox.player.ui.component.playback.weightedCrossfadeFrames
import com.melox.player.ui.component.playback.rememberPlayerSheetVerticalDragModifier
import com.melox.player.ui.component.liquid.rememberCombinedBackdrop
import com.melox.player.ui.component.playback.artworkInsetRect
import com.melox.player.ui.component.playback.fittedArtworkRect
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import com.melox.player.ui.component.os4.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.NotesFill
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Playlist
import top.yukonga.miuix.kmp.icon.extended.Tune
import com.melox.player.ui.component.os4.OverlayBottomSheet
import com.melox.player.ui.component.os4.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun FullPlayerScreen(
    playback: PlaybackUiState,
    artworkState: ArtworkTransitionState<Bitmap>,
    currentTrack: MusicTrack?,
    lyrics: LyricsUiState,
    playbackBackgroundStyle: PlaybackBackgroundStyle,
    dynamicFlowBackgroundState: DynamicFlowBackgroundState,
    playbackBackgroundDark: Boolean,
    playbackContentColor: Color,
    dynamicColorEnabled: Boolean,
    lyricFontScale: Float,
    lyricFontWeight: Int,
    lyricAnimationMode: LyricAnimationMode,
    lyricBlurEnabled: Boolean,
    centerLyrics: Boolean,
    leftAlignPlayerTitle: Boolean,
    hideControlsOnLyrics: Boolean,
    immersiveMode: Boolean,
    onImmersiveModeChange: (Boolean) -> Unit,
    showLyricsTranslation: Boolean,
    showLyricsRomanization: Boolean,
    lyricsTranslationLanguage: LyricsTranslationLanguage,
    showArtworkLyrics: Boolean,
    onLyricFontScaleChange: (Float) -> Unit,
    onLyricFontWeightChange: (Int) -> Unit,
    onLyricAnimationModeChange: (LyricAnimationMode) -> Unit,
    onLyricBlurEnabledChange: (Boolean) -> Unit,
    onCenterLyricsChange: (Boolean) -> Unit,
    onLeftAlignPlayerTitleChange: (Boolean) -> Unit,
    onHideControlsOnLyricsChange: (Boolean) -> Unit,
    onShowLyricsTranslationChange: (Boolean) -> Unit,
    onShowLyricsRomanizationChange: (Boolean) -> Unit,
    onLyricsTranslationLanguageChange: (LyricsTranslationLanguage) -> Unit,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Boolean,
    onNext: () -> Boolean,
    onSeek: (Long) -> Unit,
    onCyclePlaybackMode: () -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
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
    playerActive: Boolean,
    blockUnderlyingInput: Boolean,
    lyricsPagingEnabled: Boolean,
    drawInPlace: Boolean,
    recordInPlace: Boolean,
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
    onArtworkDisplaySizeChanged: (Int) -> Unit,
    onArtworkPageSelectedChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    circularArtwork: Boolean = false,
    squareArtwork: Boolean = false,
) {
    val item = playback.currentItem?.let { queueItem ->
        currentTrack?.let(queueItem::withTrackMetadata) ?: queueItem
    }
    val context = LocalContext.current
    val density = LocalDensity.current
    val artworkResource = rememberPlaybackArtworkResource(
        contentUri = item?.contentUri.orEmpty(),
        dateModifiedEpochSeconds = item?.dateModifiedEpochSeconds ?: 0L,
        fileSizeBytes = item?.fileSizeBytes ?: 0L,
        requestSize = PLAYER_FULL_ARTWORK_REQUEST_SIZE,
        prioritizeBlurredBackground =
            playbackBackgroundStyle == PlaybackBackgroundStyle.BLURRED_ARTWORK,
    )
    val artworkBlend = artworkState.toArtworkBlend()
    val emphasisControlColor = playbackContentColor
    val primaryControlAlpha = if (
        playbackBackgroundStyle == PlaybackBackgroundStyle.DYNAMIC_FLOW &&
        !playbackBackgroundDark && !dynamicColorEnabled
    ) 0.9f else null
    val activeLyricAlpha = if (primaryControlAlpha != null) 0.8f else 1f
    val controlColor = emphasisControlColor.copy(alpha = 0.6f)
    val artistControlColor = emphasisControlColor.copy(
        alpha = 0.6f,
    )
    val artworkCornerRadius = PLAYER_FULL_ARTWORK_CORNER_RADIUS
    var showTrackActions by remember { mutableStateOf(false) }
    var showLyricsSettings by remember { mutableStateOf(false) }
    var showAudioSettings by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showTranslationSettings by remember { mutableStateOf(false) }
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
    val centerLyricsEnabled = !lyrics.isDuetForMedia(playback.currentItem?.mediaId)
    var displayedLeftAlignPlayerTitle by remember {
        mutableStateOf(leftAlignPlayerTitle)
    }
    var displayedHideControlsOnLyrics by remember {
        mutableStateOf(hideControlsOnLyrics)
    }
    val hideLyricsControls = displayedHideControlsOnLyrics
    val onToggleImmersiveMode = {
        val enabled = !immersiveMode
        onImmersiveModeChange(enabled)
        if (enabled) {
            Toast.makeText(
                context,
                R.string.settings_immersive_mode_title,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
    var displayedShowLyricsTranslation by remember {
        mutableStateOf(showLyricsTranslation)
    }
    val toggleLyricsTranslation = {
        displayedShowLyricsTranslation = !displayedShowLyricsTranslation
        onShowLyricsTranslationChange(displayedShowLyricsTranslation)
    }
    var displayedLyricsTranslationLanguage by remember {
        mutableStateOf(lyricsTranslationLanguage)
    }
    var displayedShowLyricsRomanization by remember {
        mutableStateOf(showLyricsRomanization)
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
    val playerSafeDrawingPadding = WindowInsets.systemBarsIgnoringVisibility
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
        initialPage = if (initialArtworkPageSelected || item == null) 0 else 1,
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
    LaunchedEffect(immersiveMode) {
        lyricsPreviewPositionMs = null
    }
    LaunchedEffect(showLyricsTranslation) {
        displayedShowLyricsTranslation = showLyricsTranslation
    }
    LaunchedEffect(lyricsTranslationLanguage) {
        displayedLyricsTranslationLanguage = lyricsTranslationLanguage
    }
    LaunchedEffect(showLyricsRomanization) {
        displayedShowLyricsRomanization = showLyricsRomanization
    }
    val dismissGestureModifier = rememberPlayerSheetVerticalDragModifier(
        enabled = interactionEnabled,
        hasItem = true,
        onDragStart = onPlayerDragStart,
        onDrag = onPlayerDrag,
        onDragEnd = onPlayerDragEnd,
        onDragCancel = onPlayerDragCancel,
    )
    BackHandler(
        enabled = interactionEnabled,
        onBack = onDismiss,
    )
    val lyricActionsBackground = if (isRuntimeShaderSupported()) {
        rememberLayerBackdrop()
    } else null
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
                        recordInPlace = recordInPlace,
                        backdrop = lyricActionsBackground,
                        onRecorded = onBackgroundLayerRecorded,
                    ),
            ) {
                if (item == null) {
                    val emptyBackground = if (playbackBackgroundStyle == PlaybackBackgroundStyle.BLURRED_ARTWORK) {
                        if (playbackBackgroundDark) MiuixTheme.colorScheme.surface
                        else MiuixTheme.colorScheme.background
                    } else {
                        remember(playbackBackgroundDark) {
                            if (playbackBackgroundDark) darkColorScheme().surface
                            else lightColorScheme().background
                        }
                    }
                    Box(
                        Modifier.fillMaxSize().background(emptyBackground),
                    )
                } else when (playbackBackgroundStyle) {
                    PlaybackBackgroundStyle.BLURRED_ARTWORK -> BlurredArtworkBackground(
                        resource = artworkResource,
                        animate = drawInPlace && playback.isPlaying,
                        animateArtworkTransition = true,
                        modifier = Modifier.fillMaxSize(),
                    )

                    PlaybackBackgroundStyle.DYNAMIC_FLOW -> DynamicFlowBackground(
                        state = dynamicFlowBackgroundState,
                        dark = playbackBackgroundDark,
                        artwork = artworkResource.artwork,
                        artworkLoading = artworkResource.isLoading,
                        backgroundColor = artworkResource.backgroundColor,
                        animate = drawInPlace && playback.isPlaying,
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
                        recordInPlace = recordInPlace,
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
                    val lyricsActive = playerActive && playerLyricsAreActive(
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
                    val portraitHeaderHeight = headerTopPadding + playerHeaderContentHeight + headerSpacing
                    val portraitChromeAlpha = playerChromeAlpha(!immersiveMode)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = playerSafeStart,
                                end = playerSafeEnd,
                            ),
                    ) {
                    val lyricsCenterOffsetY = if (hideLyricsControls) {
                        hiddenLyricsCenterOffsetY(
                            pageHeight = pageHeight,
                            headerTopPadding = 0.dp,
                            headerContentHeight = 0.dp,
                            headerSpacing = 0.dp,
                        )
                    } else {
                        0.dp
                    }
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize(),
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
                                        modifier = Modifier
                                            .width(artworkContentWidth)
                                            .playerChromeVisibility(!immersiveMode),
                                    )
                                    Spacer(Modifier.height(LANDSCAPE_PLAYER_TITLE_TO_ARTWORK_SPACING))
                                    BoxWithConstraints(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        PlayerArtwork(
                                            circularArtwork = circularArtwork, squareArtwork = squareArtwork,
                                            showArtwork = item != null,
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
                                        primaryControlAlpha = primaryControlAlpha,
                                        immersiveMode = immersiveMode,
                                        onToggleImmersiveMode = onToggleImmersiveMode,
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
                                        onOpenAudioSettings = { showAudioSettings = true },
                                        onOpenSleepTimer = { showSleepTimer = true },
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
                                    val lyricBackdrop = if (lyricActionsBackground != null) {
                                        rememberLayerBackdrop()
                                    } else null
                                    val actionBackdrop = if (lyricActionsBackground != null && lyricBackdrop != null) {
                                        rememberCombinedBackdrop(lyricActionsBackground, lyricBackdrop)
                                    } else null
                                    Box(Modifier.fillMaxSize()) {
                                        SyncedLyrics(
                                            activeLyricAlpha = activeLyricAlpha,
                                            lyrics = lyrics,
                                            currentMediaId = playback.currentItem?.mediaId,
                                            positionMs = playback.positionMs,
                                            positionUpdateElapsedRealtimeMs =
                                                playback.positionUpdateElapsedRealtimeMs,
                                            playbackIteration = playback.playbackIteration,
                                            playbackSpeed = playback.playbackSpeed,
                                            previewPositionMs = lyricsPreviewPositionMs,
                                            isPlaying = playback.isPlaying,
                                            active = lyricsActive,
                                            playerActive = playerActive,
                                            positionDiscontinuityRevision = playback.positionDiscontinuityRevision,
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
                                            showLyricsRomanization = displayedShowLyricsRomanization,
                                            lyricsTranslationLanguage = displayedLyricsTranslationLanguage,
                                            showBottomFade = true,
                                            resumeFollowRequestKey = lyricsFollowRequestKey,
                                            seekRequestKey = lyricsSeekRequestKey,
                                            seekPositionMs = lyricsSeekPositionMs,
                                            modifier = Modifier.fillMaxSize().then(
                                                if (lyricBackdrop != null) Modifier.layerBackdrop(lyricBackdrop)
                                                else Modifier,
                                            ),
                                        )
                                        PlayerLyricActionsRow(
                                            showLyricsTranslation = displayedShowLyricsTranslation,
                                            emphasisControlColor = emphasisControlColor,
                                            onOpenLyricsSettings = { showLyricsSettings = true },
                                            onToggleLyricsTranslation = toggleLyricsTranslation,
                                            onOpenTranslationSettings = { showTranslationSettings = true },
                                            enabled = !immersiveMode,
                                            blurBackdrop = actionBackdrop,
                                            modifier = Modifier.align(Alignment.BottomCenter)
                                                .fillMaxWidth().playerChromeVisibility(!immersiveMode),
                                        )
                                    }
                                },
                            )
                        } else if (playerLayout == PlayerLayout.COMPACT_LANDSCAPE) {
                            CompactLandscapePlayerLayout(
                                pagerState = pagerState,
                                lyricsPagingEnabled = lyricsPagingEnabled && item != null,
                                immersiveMode = immersiveMode,
                                artworkContent = { artworkSize ->
                                    PlayerArtwork(
                                        circularArtwork = circularArtwork, squareArtwork = squareArtwork,
                                        showArtwork = item != null,
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
                                headerContent = { contentWidth ->
                                    PlayerHeader(
                                        item = item,
                                        trackChangeDirection = playback.trackChangeDirection,
                                        titleColor = emphasisControlColor,
                                        artistColor = artistControlColor,
                                        leftAligned = displayedLeftAlignPlayerTitle,
                                        titleSlotHeight = playerHeaderTitleSlotHeight,
                                        artistSlotHeight = playerHeaderArtistSlotHeight,
                                        modifier = Modifier
                                            .width(contentWidth)
                                            .playerChromeVisibility(!immersiveMode),
                                    )
                                },
                                lyricsToggleContent = { showLyrics, enabled, onToggle ->
                                    PlayerIconButton(
                                        icon = if (showLyrics) MiuixIcons.NotesFill else MiuixIcons.Notes,
                                        description = stringResource(
                                            if (showLyrics) R.string.player_show_controls
                                            else R.string.player_show_lyrics,
                                        ),
                                        onClick = onToggle,
                                        size = 24.dp,
                                        tint = emphasisControlColor.copy(alpha = 0.6f),
                                        enabled = enabled,
                                    )
                                },
                                lyricsContent = { paneWidth, contentWidth, contentHeight, lyricsVisible ->
                                    val lyricBackdrop = if (lyricActionsBackground != null) {
                                        rememberLayerBackdrop()
                                    } else null
                                    val actionBackdrop = if (lyricActionsBackground != null && lyricBackdrop != null) {
                                        rememberCombinedBackdrop(lyricActionsBackground, lyricBackdrop)
                                    } else null
                                    Box(
                                        modifier = Modifier
                                            .width(paneWidth)
                                            .height(contentHeight),
                                    ) {
                                        SyncedLyrics(
                                            activeLyricAlpha = activeLyricAlpha,
                                            lyrics = lyrics,
                                            currentMediaId = playback.currentItem?.mediaId,
                                            positionMs = playback.positionMs,
                                            positionUpdateElapsedRealtimeMs =
                                                playback.positionUpdateElapsedRealtimeMs,
                                            playbackIteration = playback.playbackIteration,
                                            playbackSpeed = playback.playbackSpeed,
                                            previewPositionMs = lyricsPreviewPositionMs,
                                            isPlaying = playback.isPlaying,
                                            active = playerActive && lyricsVisible,
                                            playerActive = playerActive,
                                            positionDiscontinuityRevision = playback.positionDiscontinuityRevision,
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
                                            showLyricsRomanization = displayedShowLyricsRomanization,
                                            lyricsTranslationLanguage = displayedLyricsTranslationLanguage,
                                            showBottomFade = true,
                                            resumeFollowRequestKey = lyricsFollowRequestKey,
                                            seekRequestKey = lyricsSeekRequestKey,
                                            seekPositionMs = lyricsSeekPositionMs,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .then(
                                                    if (lyricBackdrop != null) Modifier.layerBackdrop(lyricBackdrop)
                                                    else Modifier,
                                                ),
                                        )
                                        PlayerLyricActionsRow(
                                            showLyricsTranslation = displayedShowLyricsTranslation,
                                            emphasisControlColor = emphasisControlColor,
                                            onOpenLyricsSettings = { showLyricsSettings = true },
                                            onToggleLyricsTranslation = toggleLyricsTranslation,
                                            onOpenTranslationSettings = { showTranslationSettings = true },
                                            enabled = !immersiveMode,
                                            blurBackdrop = actionBackdrop,
                                            modifier = Modifier.align(Alignment.BottomCenter)
                                                .fillMaxWidth().playerChromeVisibility(!immersiveMode),
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
                                        PlayerDetails(
                                            primaryControlAlpha = primaryControlAlpha,
                                            immersiveMode = immersiveMode,
                                            onToggleImmersiveMode = onToggleImmersiveMode,
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
                                            onOpenAudioSettings = { showAudioSettings = true },
                                            onOpenSleepTimer = { showSleepTimer = true },
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
                                        // Mirror the top safe space independently of navigation bars.
                                        bottom = playerSafeTop,
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
                            if (hideLyricsControls) {
                                PlayerContentPager(
                                    activeLyricAlpha = activeLyricAlpha,
                                    pagerState = pagerState,
                                    lyrics = lyrics,
                                    currentMediaId = playback.currentItem?.mediaId,
                                    positionMs = playback.positionMs,
                                    positionUpdateElapsedRealtimeMs =
                                        playback.positionUpdateElapsedRealtimeMs,
                                    playbackIteration = playback.playbackIteration,
                                    playbackSpeed = playback.playbackSpeed,
                                    previewPositionMs = lyricsPreviewPositionMs,
                                    isPlaying = playback.isPlaying,
                                    lyricsActive = lyricsActive,
                                    playerActive = playerActive,
                                    positionDiscontinuityRevision = playback.positionDiscontinuityRevision,
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
                                    showLyricsRomanization = displayedShowLyricsRomanization,
                                    lyricsTranslationLanguage = displayedLyricsTranslationLanguage,
                                    showBottomFade = false,
                                    resumeFollowRequestKey = lyricsFollowRequestKey,
                                    seekRequestKey = lyricsSeekRequestKey,
                                    seekPositionMs = lyricsSeekPositionMs,
                                    modifier = Modifier.fillMaxSize(),
                                    lyricsBackground = lyricActionsBackground,
                                    lyricsViewportTopInset = portraitHeaderHeight,
                                    immersiveMode = immersiveMode,
                                    lyricsBottomContent = { blurBackdrop ->
                                        PlayerLyricActionsRow(
                                            showLyricsTranslation = displayedShowLyricsTranslation,
                                            emphasisControlColor = emphasisControlColor,
                                            onOpenLyricsSettings = { showLyricsSettings = true },
                                            onToggleLyricsTranslation = toggleLyricsTranslation,
                                            onOpenTranslationSettings = { showTranslationSettings = true },
                                            enabled = !immersiveMode,
                                            blurBackdrop = blurBackdrop,
                                            modifier = Modifier.fillMaxWidth()
                                                .then(if (!immersiveMode) dismissGestureModifier else Modifier)
                                                .padding(bottom = spacing.panelBottom)
                                                .playerChromeVisibility(!immersiveMode),
                                        )
                                    },
                                    artworkContent = { overscrollModifier ->
                                        PortraitPlayerLayout(
                                            spacing = spacing,
                                            artworkTopPadding = portraitHeaderHeight,
                                            artworkModifier = overscrollModifier,
                                            artworkContent = { _ ->
                                                PortraitArtworkWithLyrics(
                                                    activeLyricAlpha = activeLyricAlpha,
                                                    lyrics = lyrics,
                                                    playback = playback,
                                                    enabled = showArtworkLyrics,
                                                    lyricsVisible = !immersiveMode,
                                                    active = playerActive &&
                                                        (pagerState.currentPage == 0 || pagerState.targetPage == 0),
                                                    showTranslation = displayedShowLyricsTranslation,
                                                    showRomanization = displayedShowLyricsRomanization,
                                                    translationLanguage = displayedLyricsTranslationLanguage,
                                                    centerLyrics = displayedCenterLyrics,
                                                    animationMode = displayedLyricAnimationMode,
                                                    lyricFontWeight = displayedLyricFontWeight,
                                                    preferredArtworkSize = artworkSize,
                                                    progressGap = spacing.artworkToProgress +
                                                        PLAYER_PROGRESS_TOUCH_HEIGHT / 2f,
                                                    contentWidth = artworkContentWidth,
                                                    color = emphasisControlColor,
                                                    previewPositionMs = lyricsPreviewPositionMs
                                                        ?: lyricsSeekPositionMs.takeIf { lyricsSeekRequestKey != 0 },
                                                    modifier = Modifier.fillMaxSize(),
                                                    artworkContent = { captionArtworkHeight ->
                                                        PlayerArtwork(
                                                            circularArtwork = circularArtwork, squareArtwork = squareArtwork,
                                                            showArtwork = item != null,
                                                            size = fitPlayerArtworkSize(
                                                                artworkSize,
                                                                captionArtworkHeight,
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
                                                )
                                            },
                                            detailsContent = {
                                                PlayerDetails(
                                                    primaryControlAlpha = primaryControlAlpha,
                                                    immersiveMode = immersiveMode,
                                                    onToggleImmersiveMode = onToggleImmersiveMode,
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
                                                    onOpenAudioSettings = { showAudioSettings = true },
                                                    onOpenSleepTimer = { showSleepTimer = true },
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
                                val progressTrackInset =
                                    (PLAYER_PROGRESS_TOUCH_HEIGHT - PLAYER_PROGRESS_IDLE_HEIGHT) / 2f
                                val lyricBackdrop = if (lyricActionsBackground != null) {
                                    rememberLayerBackdrop()
                                } else null
                                val controlsBackdrop = if (lyricActionsBackground != null && lyricBackdrop != null) {
                                    rememberCombinedBackdrop(lyricActionsBackground, lyricBackdrop)
                                } else null
                                PortraitPlayerLayout(
                                    spacing = spacing.copy(artworkToProgress = 0.dp),
                                    artworkBottomOverlap = progressTrackInset,
                                    artworkTopPadding = portraitHeaderHeight,
                                    overlayDetails = true,
                                    detailsModifier = if (!immersiveMode) dismissGestureModifier else Modifier,
                                    artworkContent = { artworkHeight ->
                                        PlayerContentPager(
                                            activeLyricAlpha = activeLyricAlpha,
                                            pagerState = pagerState,
                                            lyrics = lyrics,
                                            currentMediaId = playback.currentItem?.mediaId,
                                            positionMs = playback.positionMs,
                                            positionUpdateElapsedRealtimeMs =
                                                playback.positionUpdateElapsedRealtimeMs,
                                            playbackIteration = playback.playbackIteration,
                                            playbackSpeed = playback.playbackSpeed,
                                            previewPositionMs = lyricsPreviewPositionMs,
                                            isPlaying = playback.isPlaying,
                                            lyricsActive = lyricsActive,
                                            playerActive = playerActive,
                                            positionDiscontinuityRevision = playback.positionDiscontinuityRevision,
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
                                            lyricCenterOffsetY =
                                                portraitHeaderHeight + (artworkHeight - maxHeight) / 2f,
                                            showLyricsTranslation =
                                                displayedShowLyricsTranslation,
                                            showLyricsRomanization = displayedShowLyricsRomanization,
                                            lyricsTranslationLanguage = displayedLyricsTranslationLanguage,
                                            showBottomFade = true,
                                            resumeFollowRequestKey = lyricsFollowRequestKey,
                                            seekRequestKey = lyricsSeekRequestKey,
                                            seekPositionMs = lyricsSeekPositionMs,
                                            modifier = Modifier.fillMaxSize(),
                                            lyricsBackground = lyricActionsBackground,
                                            lyricsBackdrop = lyricBackdrop,
                                            lyricsViewportTopInset = portraitHeaderHeight,
                                            lyricsViewportBottomInset =
                                                (maxHeight - portraitHeaderHeight - artworkHeight).coerceAtLeast(0.dp),
                                            immersiveMode = immersiveMode,
                                            artworkAlignment = Alignment.TopCenter,
                                            lyricsBottomContent = { blurBackdrop ->
                                                PlayerLyricActionsRow(
                                                    showLyricsTranslation = displayedShowLyricsTranslation,
                                                    emphasisControlColor = emphasisControlColor,
                                                    onOpenLyricsSettings = { showLyricsSettings = true },
                                                    onToggleLyricsTranslation = toggleLyricsTranslation,
                                                    onOpenTranslationSettings = { showTranslationSettings = true },
                                                    enabled = !immersiveMode,
                                                    blurBackdrop = blurBackdrop,
                                                    modifier = Modifier.fillMaxWidth()
                                                        .then(if (!immersiveMode) dismissGestureModifier else Modifier)
                                                        .padding(bottom = PLAYER_PROGRESS_TIME_SPACING + 12.dp)
                                                        .playerChromeVisibility(!immersiveMode),
                                                )
                                            },
                                            artworkContent = { overscrollModifier ->
                                                PortraitArtworkWithLyrics(
                                                    activeLyricAlpha = activeLyricAlpha,
                                                    lyrics = lyrics,
                                                    playback = playback,
                                                    enabled = showArtworkLyrics,
                                                    lyricsVisible = !immersiveMode,
                                                    active = playerActive &&
                                                        (pagerState.currentPage == 0 || pagerState.targetPage == 0),
                                                    showTranslation = displayedShowLyricsTranslation,
                                                    showRomanization = displayedShowLyricsRomanization,
                                                    translationLanguage = displayedLyricsTranslationLanguage,
                                                    centerLyrics = displayedCenterLyrics,
                                                    animationMode = displayedLyricAnimationMode,
                                                    lyricFontWeight = displayedLyricFontWeight,
                                                    preferredArtworkSize = artworkSize,
                                                    progressGap = spacing.artworkToProgress +
                                                        PLAYER_PROGRESS_TOUCH_HEIGHT / 2f,
                                                    contentWidth = artworkContentWidth,
                                                    color = emphasisControlColor,
                                                    previewPositionMs = lyricsPreviewPositionMs
                                                        ?: lyricsSeekPositionMs.takeIf { lyricsSeekRequestKey != 0 },
                                                    modifier = Modifier
                                                        .padding(top = portraitHeaderHeight)
                                                        .fillMaxWidth().height(artworkHeight)
                                                        .padding(bottom = spacing.artworkToProgress + progressTrackInset)
                                                        .then(overscrollModifier),
                                                    artworkContent = { captionArtworkHeight ->
                                                        PlayerArtwork(
                                                            circularArtwork = circularArtwork, squareArtwork = squareArtwork,
                                                            showArtwork = item != null,
                                                            size = fitPlayerArtworkSize(
                                                                artworkSize,
                                                                captionArtworkHeight,
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
                                        )
                                    },
                                    detailsContent = {
                                        PlayerDetails(
                                            primaryControlAlpha = primaryControlAlpha,
                                            primaryControlsBackdrop = controlsBackdrop,
                                            primaryControlsBackdropAlpha = {
                                                if (immersiveMode || portraitChromeAlpha.value < 1f) {
                                                    (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                                                        .coerceIn(0f, 1f)
                                                } else 0f
                                            },
                                            immersiveMode = immersiveMode,
                                            onToggleImmersiveMode = onToggleImmersiveMode,
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
                                            onOpenAudioSettings = { showAudioSettings = true },
                                            onOpenSleepTimer = { showSleepTimer = true },
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
                    if (playerLayout == PlayerLayout.PORTRAIT) {
                        Column(
                            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
                                .then(if (!immersiveMode) dismissGestureModifier else Modifier)
                                .playerChromeVisibility(!immersiveMode)
                                .padding(top = headerTopPadding),
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
                                modifier = Modifier
                                    .width(portraitArtworkContentWidth)
                                    .then(
                                        rememberPlayerHeaderGestureModifier(
                                            enabled = !immersiveMode && hideLyricsControls &&
                                                lyricsPagingEnabled && item != null &&
                                                pagerState.settledPage == 1 &&
                                                !pagerState.isScrollInProgress,
                                            playWhenReady = playback.playWhenReady,
                                            onTogglePlayPause = onTogglePlayPause,
                                            onPrevious = { onPrevious() },
                                            onNext = { onNext() },
                                        ),
                                    ),
                            )
                            Spacer(Modifier.height(headerSpacing))
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
                    centerLyricsEnabled = centerLyricsEnabled,
                    hideControlsOnLyrics = displayedHideControlsOnLyrics,
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
                )
                LyricsTranslationSheet(
                    show = showTranslationSettings,
                    onDismiss = { showTranslationSettings = false },
                    showLyricsTranslation = displayedShowLyricsTranslation,
                    showLyricsRomanization = displayedShowLyricsRomanization,
                    lyricsTranslationLanguage = displayedLyricsTranslationLanguage,
                    onShowLyricsTranslationChange = {
                        displayedShowLyricsTranslation = it
                        onShowLyricsTranslationChange(it)
                    },
                    onLyricsTranslationLanguageChange = {
                        displayedLyricsTranslationLanguage = it
                        onLyricsTranslationLanguageChange(it)
                    },
                    onShowLyricsRomanizationChange = {
                        displayedShowLyricsRomanization = it
                        onShowLyricsRomanizationChange(it)
                    },
                )
                SleepTimerSheet(
                    show = showSleepTimer,
                    timerSeconds = sleepTimerSeconds,
                    timerState = sleepTimerState,
                    autoExtendSleepTimer = autoExtendSleepTimer,
                    onAutoExtendSleepTimerChange = onAutoExtendSleepTimerChange,
                    onDismiss = { showSleepTimer = false },
                    onTimerSecondsChange = onSleepTimerSecondsChange,
                    onStartTimer = onStartSleepTimer,
                    onCancelTimer = onCancelSleepTimer,
                )
                AudioSettingsSheet(
                    show = showAudioSettings,
                    playbackSpeed = playback.playbackSpeed,
                    audioSessionId = playback.audioSessionId,
                    playbackPauseFade = playbackPauseFade,
                    onPlaybackPauseFadeChange = onPlaybackPauseFadeChange,
                    onDismiss = { showAudioSettings = false },
                    onPlaybackSpeedChange = onPlaybackSpeedChange,
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
    centerLyricsEnabled: Boolean,
    hideControlsOnLyrics: Boolean,
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
) {
    val wideLayout = usesMiuixSmallTopAppBar()
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
            centerLyricsEnabled = centerLyricsEnabled,
            lyricFontScale = lyricFontScale,
            lyricFontWeight = lyricFontWeight,
            lyricBlurEnabled = lyricBlurEnabled,
            lyricAnimationMode = lyricAnimationMode,
            hideControlsOnLyrics = hideControlsOnLyrics,
            onLeftAlignPlayerTitleChange = onLeftAlignPlayerTitleChange,
            onCenterLyricsChange = onCenterLyricsChange,
            onLyricFontScalePreview = onLyricFontScalePreview,
            onLyricFontScaleCommit = onLyricFontScaleCommit,
            onLyricFontWeightPreview = onLyricFontWeightPreview,
            onLyricFontWeightCommit = onLyricFontWeightCommit,
            onLyricBlurEnabledChange = onLyricBlurEnabledChange,
            onLyricAnimationModeChange = onLyricAnimationModeChange,
            onHideControlsOnLyricsChange = onHideControlsOnLyricsChange,
            modifier = Modifier
                .fillMaxWidth()
                .overScrollVertical(
                    nestedScrollToParent = false,
                    isEnabled = { scrollState.maxValue > 0 },
                )
                .verticalScroll(scrollState, overscrollEffect = null)
                .padding(bottom = bottomPadding + 12.dp),
            cardColor = bottomSheetCardColor(),
            usePageMaterial = false,
            showHideControls = !wideLayout,
        )
    }
}

@Composable
private fun LyricsTranslationSheet(
    show: Boolean,
    showLyricsTranslation: Boolean,
    showLyricsRomanization: Boolean,
    lyricsTranslationLanguage: LyricsTranslationLanguage,
    onDismiss: () -> Unit,
    onShowLyricsTranslationChange: (Boolean) -> Unit,
    onShowLyricsRomanizationChange: (Boolean) -> Unit,
    onLyricsTranslationLanguageChange: (LyricsTranslationLanguage) -> Unit,
) {
    val scrollState = rememberScrollState()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    OverlayBottomSheet(
        show = show,
        modifier = bottomSheetGlassModifier(),
        backgroundColor = bottomSheetMaterialColor(),
        title = stringResource(R.string.lyrics_translation),
        enableWindowDim = true,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .overScrollVertical(
                    nestedScrollToParent = false,
                    isEnabled = { scrollState.maxValue > 0 },
                )
                .verticalScroll(scrollState, overscrollEffect = null)
                .padding(bottom = bottomPadding + 12.dp),
        ) {
            LyricsTranslationPreferences(
                showLyricsTranslation = showLyricsTranslation,
                showLyricsRomanization = showLyricsRomanization,
                lyricsTranslationLanguage = lyricsTranslationLanguage,
                onShowLyricsTranslationChange = onShowLyricsTranslationChange,
                onShowLyricsRomanizationChange = onShowLyricsRomanizationChange,
                onLyricsTranslationLanguageChange = onLyricsTranslationLanguageChange,
                cardColor = bottomSheetCardColor(),
                usePageMaterial = false,
            )
        }
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
    val direction = remember(item?.contentUri) { trackChangeDirection }
    val header = PlayerHeaderContent(
        trackKey = item?.contentUri,
        direction = direction,
        title = item?.title ?: stringResource(R.string.mini_player_title),
        artist = displayArtistName(item?.artist)
            ?: stringResource(if (item == null) R.string.mini_player_empty else R.string.music_unknown_artist),
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
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .expandLeftForMarquee(PLAYER_HEADER_TITLE_EDGE_FADE_WIDTH)
                .fillMaxWidth()
                .playerHeaderEdgeMask(),
        ) {
            headerTransition.AnimatedContent(
                modifier = Modifier.fillMaxWidth(),
                contentKey = { it.trackKey },
                transitionSpec = {
                    val slideDirection = targetState.direction
                    slideInHorizontally(tween(PLAYER_TRACK_ARTWORK_CROSSFADE_DURATION_MILLIS)) { it * slideDirection }
                        .togetherWith(
                            slideOutHorizontally(tween(PLAYER_TRACK_ARTWORK_CROSSFADE_DURATION_MILLIS)) { -it * slideDirection },
                        ).using(null)
                },
                contentAlignment = if (leftAligned) Alignment.CenterStart else Alignment.Center,
            ) { target ->
                var titleLayout by remember(target.trackKey) {
                    mutableStateOf<TextLayoutResult?>(null)
                }
                var artistLayout by remember(target.trackKey) {
                    mutableStateOf<TextLayoutResult?>(null)
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (leftAligned) Alignment.Start else Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(min = titleSlotHeight),
                        contentAlignment = if (leftAligned) Alignment.CenterStart else Alignment.Center,
                    ) {
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
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(min = artistSlotHeight),
                        contentAlignment = if (leftAligned) Alignment.CenterStart else Alignment.Center,
                    ) {
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

internal fun Modifier.expandLeftForMarquee(extra: Dp): Modifier = layout { measurable, constraints ->
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
