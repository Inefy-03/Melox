package com.melox.player.ui.screen.playback

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.melox.player.data.library.ArtistGroup
import com.melox.player.model.LyricAnimationMode
import com.melox.player.model.LyricsTranslationLanguage
import com.melox.player.model.MusicTrack
import com.melox.player.model.PlaybackBackgroundStyle
import com.melox.player.model.PlaybackQueueItem
import com.melox.player.model.PlaybackUiState
import com.melox.player.ui.MiniPlayerChrome
import com.melox.player.ui.component.animation.InteractiveHighlight
import com.melox.player.ui.component.library.PLACEHOLDER_ARTWORK_RES_ID
import com.melox.player.ui.component.playback.ArtworkTransitionState
import com.melox.player.ui.component.playback.DynamicFlowBackgroundState
import com.melox.player.ui.component.playback.MiniPlayer
import com.melox.player.ui.component.playback.PLAYER_FULL_ARTWORK_REQUEST_SIZE
import com.melox.player.ui.component.playback.prefetchPlaybackArtworkResource
import com.melox.player.ui.component.playback.prefetchPlaybackBackground
import com.melox.player.ui.viewmodel.MeloxViewModel

@Composable
internal fun FullPlayerHost(
    viewModel: MeloxViewModel,
    artworkState: ArtworkTransitionState<Bitmap>,
    onArtworkDisplaySizeChanged: (Int) -> Unit,
    circularArtwork: Boolean,
    squareArtwork: Boolean,
    tracks: List<MusicTrack>,
    artistGroups: List<ArtistGroup>,
    playbackBackgroundStyle: PlaybackBackgroundStyle,
    dynamicFlowBackgroundState: DynamicFlowBackgroundState,
    playbackBackgroundDark: Boolean,
    dynamicColorEnabled: Boolean,
    playbackContentColor: Color,
    lyricFontScale: Float,
    lyricFontWeight: Int,
    lyricAnimationMode: LyricAnimationMode,
    lyricBlurEnabled: Boolean,
    centerLyrics: Boolean,
    leftAlignPlayerTitle: Boolean,
    hideControlsOnLyrics: Boolean,
    immersiveMode: Boolean,
    showLyricsTranslation: Boolean,
    showLyricsRomanization: Boolean,
    lyricsTranslationLanguage: LyricsTranslationLanguage,
    showArtworkLyrics: Boolean,
    showMusicTagEditor: Boolean,
    showLyricoEditor: Boolean,
    showLunaBeatEditor: Boolean,
    onDismiss: () -> Unit,
    onOpenQueue: () -> Unit,
    onAddToPlaylist: (MusicTrack) -> Unit,
    onGoToAlbum: (MusicTrack) -> Unit,
    onGoToArtist: (ArtistGroup) -> Unit,
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
    onArtworkPageSelectedChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()
    val lyrics by viewModel.lyricsState.collectAsStateWithLifecycle()
    val sleepTimerState by viewModel.sleepTimerState.collectAsStateWithLifecycle()
    val sleepTimerSelectionSeconds by viewModel.sleepTimerSelectionSeconds.collectAsStateWithLifecycle()
    val autoExtendSleepTimer by viewModel.autoExtendSleepTimer.collectAsStateWithLifecycle()
    val playbackPauseFade by viewModel.playbackPauseFade.collectAsStateWithLifecycle()
    val currentTrack = remember(tracks, playback.currentItem?.trackId) {
        val trackId = playback.currentItem?.trackId
        tracks.firstOrNull { it.id == trackId }
    }
    FullPlayerScreen(
        playback = playback,
        artworkState = artworkState,
        onArtworkDisplaySizeChanged = onArtworkDisplaySizeChanged,
        circularArtwork = circularArtwork,
        squareArtwork = squareArtwork,
        currentTrack = currentTrack,
        lyrics = lyrics,
        playbackBackgroundStyle = playbackBackgroundStyle,
        dynamicFlowBackgroundState = dynamicFlowBackgroundState,
        playbackBackgroundDark = playbackBackgroundDark,
        dynamicColorEnabled = dynamicColorEnabled,
        playbackContentColor = playbackContentColor,
        lyricFontScale = lyricFontScale,
        lyricFontWeight = lyricFontWeight,
        lyricAnimationMode = lyricAnimationMode,
        lyricBlurEnabled = lyricBlurEnabled,
        centerLyrics = centerLyrics,
        leftAlignPlayerTitle = leftAlignPlayerTitle,
        hideControlsOnLyrics = hideControlsOnLyrics,
        immersiveMode = immersiveMode,
        onImmersiveModeChange = viewModel::setImmersiveMode,
        showLyricsTranslation = showLyricsTranslation,
        showLyricsRomanization = showLyricsRomanization,
        lyricsTranslationLanguage = lyricsTranslationLanguage,
        showArtworkLyrics = showArtworkLyrics,
        onLyricFontScaleChange = viewModel::setLyricFontScale,
        onLyricFontWeightChange = viewModel::setLyricFontWeight,
        onLyricAnimationModeChange = viewModel::setLyricAnimationMode,
        onLyricBlurEnabledChange = viewModel::setLyricBlurEnabled,
        onCenterLyricsChange = viewModel::setCenterLyrics,
        onLeftAlignPlayerTitleChange = viewModel::setLeftAlignPlayerTitle,
        onHideControlsOnLyricsChange = viewModel::setHideControlsOnLyrics,
        onShowLyricsTranslationChange = viewModel::setShowLyricsTranslation,
        onShowLyricsRomanizationChange = viewModel::setShowLyricsRomanization,
        onLyricsTranslationLanguageChange = viewModel::setLyricsTranslationLanguage,
        onDismiss = onDismiss,
        onTogglePlayPause = viewModel::togglePlayPause,
        onPrevious = viewModel::previous,
        onNext = viewModel::next,
        onSeek = viewModel::seekTo,
        onCyclePlaybackMode = viewModel::cyclePlaybackMode,
        onPlaybackSpeedChange = viewModel::setPlaybackSpeed,
        sleepTimerState = sleepTimerState,
        sleepTimerSeconds = sleepTimerSelectionSeconds,
        onSleepTimerSecondsChange = viewModel::setSleepTimerSelectionSeconds,
        autoExtendSleepTimer = autoExtendSleepTimer,
        onAutoExtendSleepTimerChange = viewModel::setAutoExtendSleepTimer,
        playbackPauseFade = playbackPauseFade,
        onPlaybackPauseFadeChange = viewModel::setPlaybackPauseFade,
        onStartSleepTimer = viewModel::startSleepTimer,
        onCancelSleepTimer = viewModel::cancelSleepTimer,
        onAcknowledgeSleepTimerInterruption = viewModel::acknowledgeSleepTimerInterruption,
        onOpenQueue = onOpenQueue,
        onPlayNext = viewModel::playNext,
        onAppendToQueue = viewModel::appendToQueue,
        onAddToPlaylist = onAddToPlaylist,
        onGoToAlbum = onGoToAlbum,
        artistGroups = artistGroups,
        onGoToArtist = onGoToArtist,
        onExternalEditReturned = viewModel::refreshTrackAfterExternalEdit,
        showMusicTagEditor = showMusicTagEditor,
        showLyricoEditor = showLyricoEditor,
        showLunaBeatEditor = showLunaBeatEditor,
        backgroundLayer = backgroundLayer,
        contentLayer = contentLayer,
        frameRecordingGeneration = frameRecordingGeneration,
        interactionEnabled = interactionEnabled,
        playerActive = playerActive,
        blockUnderlyingInput = blockUnderlyingInput,
        lyricsPagingEnabled = lyricsPagingEnabled,
        drawInPlace = drawInPlace,
        recordInPlace = recordInPlace,
        sharedArtworkVisible = sharedArtworkVisible,
        initialArtworkPageSelected = initialArtworkPageSelected,
        onPlayerDragStart = onPlayerDragStart,
        onPlayerDrag = onPlayerDrag,
        onPlayerDragEnd = onPlayerDragEnd,
        onPlayerDragCancel = onPlayerDragCancel,
        onBackgroundLayerRecorded = onBackgroundLayerRecorded,
        onContentLayerRecorded = onContentLayerRecorded,
        onPlayerBoundsChanged = onPlayerBoundsChanged,
        onArtworkBoundsChanged = onArtworkBoundsChanged,
        onArtworkPageSelectedChanged = onArtworkPageSelectedChanged,
        modifier = modifier,
    )
}

@Composable
internal fun MiniPlayerHost(
    viewModel: MeloxViewModel,
    artworkState: ArtworkTransitionState<Bitmap>,
    circularArtwork: Boolean,
    squareArtwork: Boolean,
    showMiniPlayerLyrics: Boolean,
    lyricAnimationMode: LyricAnimationMode,
    chrome: MiniPlayerChrome,
    onChromeChanged: (MiniPlayerChrome) -> Unit,
    interactiveHighlight: InteractiveHighlight,
    onOpen: (Float, Float) -> Unit,
    onOpenQueue: () -> Unit,
    onPlayerDragStart: (Float, Float) -> Unit,
    onPlayerDrag: (Float, Float) -> Unit,
    onPlayerDragEnd: (Float, Float) -> Unit,
    onPlayerDragCancel: () -> Unit,
    playerLayer: GraphicsLayer,
    playerContentLayer: GraphicsLayer,
    frameRecordingGeneration: Int,
    recordForTransition: Boolean,
    drawInPlace: Boolean,
    surfaceVisible: Boolean,
    sharedArtworkVisible: Boolean,
    playerSheetProgress: () -> Float,
    capturedPressScale: Float?,
    capturedSwipeTranslationX: Float?,
    onLayerRecorded: (generation: Int, size: IntSize) -> Unit,
    onPlayerBoundsChanged: (Rect) -> Unit,
    onPlayerContentBoundsChanged: (Rect) -> Unit,
    onPlayerControlsBoundsChanged: (Rect) -> Unit,
    onArtworkBoundsChanged: (Rect) -> Unit,
) {
    val playback by viewModel.compactPlaybackState.collectAsStateWithLifecycle()
    SideEffect {
        onChromeChanged(chrome)
    }
    MiniPlayer(
        playback = playback,
        artworkState = artworkState,
        circularArtwork = circularArtwork,
        squareArtwork = squareArtwork,
        chrome = chrome,
        interactiveHighlight = interactiveHighlight,
        onOpen = { scale, offsetX ->
            if (playback.currentItem != null) onOpen(scale, offsetX)
        },
        onTogglePlayPause = viewModel::togglePlayPause,
        onPrevious = viewModel::previous,
        onNext = viewModel::next,
        onOpenQueue = onOpenQueue,
        onPlayerDragStart = onPlayerDragStart,
        onPlayerDrag = onPlayerDrag,
        onPlayerDragEnd = onPlayerDragEnd,
        onPlayerDragCancel = onPlayerDragCancel,
        playerLayer = playerLayer,
        playerContentLayer = playerContentLayer,
        frameRecordingGeneration = frameRecordingGeneration,
        recordForTransition = recordForTransition,
        drawInPlace = drawInPlace,
        surfaceVisible = surfaceVisible,
        sharedArtworkVisible = sharedArtworkVisible,
        playerSheetProgress = playerSheetProgress,
        capturedPressScale = capturedPressScale,
        capturedSwipeTranslationX = capturedSwipeTranslationX,
        onLayerRecorded = onLayerRecorded,
        onPlayerBoundsChanged = onPlayerBoundsChanged,
        onPlayerContentBoundsChanged = onPlayerContentBoundsChanged,
        onPlayerControlsBoundsChanged = onPlayerControlsBoundsChanged,
        onArtworkBoundsChanged = onArtworkBoundsChanged,
        subtitleContent = { mediaId, artist, style, color, textStartPadding ->
            MiniPlayerSubtitleHost(
                viewModel = viewModel,
                mediaId = mediaId,
                artist = artist,
                style = style,
                color = color,
                textStartPadding = textStartPadding,
                enabled = showMiniPlayerLyrics,
                active = drawInPlace && surfaceVisible && !recordForTransition,
                animationMode = lyricAnimationMode,
            )
        },
    )
}

@Composable
private fun MiniPlayerSubtitleHost(
    viewModel: MeloxViewModel,
    mediaId: String?,
    artist: String,
    style: TextStyle,
    color: Color,
    textStartPadding: Dp,
    enabled: Boolean,
    active: Boolean,
    animationMode: LyricAnimationMode,
) {
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()
    val lyrics by viewModel.lyricsState.collectAsStateWithLifecycle()
    val document = if (enabled) {
        compactLyricsDocument(
            lyrics, mediaId.takeIf { it == playback.currentItem?.mediaId },
        )
    } else {
        null
    }
    key(mediaId, playback.currentItem?.mediaId) {
        MiniPlayerLyrics(
            document = document,
            playback = playback,
            artist = artist,
            style = style,
            color = color,
            active = active,
            animationMode = animationMode,
            modifier = Modifier.fillMaxWidth(),
            textStartPadding = textStartPadding,
        )
    }
}

internal fun playbackArtworkPrefetchItems(playback: PlaybackUiState): List<PlaybackQueueItem> {
    val queue = playback.queue
    val currentIndex = playback.currentIndex
    if (currentIndex !in queue.indices) return emptyList()
    return listOf(
        currentIndex,
        (currentIndex + 1) % queue.size,
        (currentIndex - 1 + queue.size) % queue.size,
    ).distinct().map(queue::get)
}

@Composable
internal fun PlaybackArtworkPrefetchEffect(
    viewModel: MeloxViewModel,
    playbackBackgroundStyle: PlaybackBackgroundStyle,
) {
    val playback = viewModel.compactPlaybackState.collectAsStateWithLifecycle().value
    val applicationContext = LocalContext.current.applicationContext
    val artworkPrefetchSizePx = with(LocalDensity.current) {
        PLAYER_FULL_ARTWORK_REQUEST_SIZE.roundToPx()
    }
    val placeholderArtworkResId = PLACEHOLDER_ARTWORK_RES_ID
    LaunchedEffect(
        playback.currentIndex,
        playback.queue,
        artworkPrefetchSizePx,
        placeholderArtworkResId,
        playbackBackgroundStyle,
    ) {
        playbackArtworkPrefetchItems(playback).forEachIndexed { index, item ->
            if (index == 0 &&
                playbackBackgroundStyle == PlaybackBackgroundStyle.BLURRED_ARTWORK
            ) {
                prefetchPlaybackBackground(
                    context = applicationContext,
                    contentUri = item.contentUri,
                    dateModifiedEpochSeconds = item.dateModifiedEpochSeconds,
                    fileSizeBytes = item.fileSizeBytes,
                    placeholderArtworkResId = placeholderArtworkResId,
                )
            }
            prefetchPlaybackArtworkResource(
                context = applicationContext,
                contentUri = item.contentUri,
                dateModifiedEpochSeconds = item.dateModifiedEpochSeconds,
                fileSizeBytes = item.fileSizeBytes,
                targetSizePx = artworkPrefetchSizePx,
                placeholderArtworkResId = placeholderArtworkResId,
                includeBlurredArtwork =
                    playbackBackgroundStyle == PlaybackBackgroundStyle.BLURRED_ARTWORK,
                foreground = index == 0,
            )
        }
    }
}

@Composable
internal fun QueueSheetHost(
    viewModel: MeloxViewModel,
    show: Boolean,
    onDismiss: () -> Unit,
) {
    val playback by viewModel.compactPlaybackState.collectAsStateWithLifecycle()
    QueueSheet(
        show = show,
        playback = playback,
        onDismiss = onDismiss,
        onJumpTo = viewModel::jumpToQueueItem,
        onMove = viewModel::moveQueueItem,
        onRemove = viewModel::removeQueueItem,
        onClear = viewModel::clearQueue,
    )
}
