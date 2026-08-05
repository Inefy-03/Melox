package com.melox.player.ui.screen.playback

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Rect
import com.melox.player.R
import com.melox.player.data.library.ArtistGroup
import com.melox.player.model.MusicTrack
import com.melox.player.model.LyricsDocument
import com.melox.player.model.LyricsUiState
import com.melox.player.model.PlaybackMode
import com.melox.player.model.PlaybackQueueItem
import com.melox.player.model.PlaybackUiState
import com.melox.player.ui.component.library.PlaybackArtworkFrame
import com.melox.player.ui.component.library.TrackActionsOverlay
import com.melox.player.ui.component.library.formatDuration
import com.melox.player.ui.component.library.rememberArtworkBitmap
import com.melox.player.ui.component.playback.ArtworkFlowBackground
import com.melox.player.ui.component.playback.PLAYER_FULL_ARTWORK_REQUEST_SIZE
import com.melox.player.ui.component.playback.PLAYER_TRACK_ARTWORK_CROSSFADE_DURATION_MILLIS
import com.melox.player.ui.component.playback.PLAYER_TRACK_ARTWORK_CROSSFADE_EASING
import com.melox.player.ui.component.playback.playerControlIconTransition
import com.melox.player.ui.component.playback.recordPlayerLayer
import com.melox.player.ui.component.playback.fittedArtworkRect
import com.melox.player.ui.component.playback.scaledRectAroundCenter
import kotlin.math.roundToLong
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Playlist
import top.yukonga.miuix.kmp.icon.extended.Replace
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun FullPlayerScreen(
    playback: PlaybackUiState,
    currentTrack: MusicTrack?,
    lyrics: LyricsUiState,
    isDark: Boolean,
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
    playerLayer: GraphicsLayer,
    interactionEnabled: Boolean,
    drawInPlace: Boolean,
    sharedArtworkVisible: Boolean,
    onPlayerDragStart: () -> Unit,
    onPlayerDrag: (Float) -> Unit,
    onPlayerDragEnd: (Float) -> Unit,
    onPlayerDragCancel: () -> Unit,
    onPlayerBoundsChanged: (Rect) -> Unit,
    onArtworkBoundsChanged: (Rect) -> Unit,
    onStatusBarBackgroundDarkChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = playback.currentItem
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
    val artistControlColor = controlColor
    val artworkCornerRadius = 12.dp
    val currentOnPlayerDragStart by rememberUpdatedState(onPlayerDragStart)
    val currentOnPlayerDrag by rememberUpdatedState(onPlayerDrag)
    val currentOnPlayerDragEnd by rememberUpdatedState(onPlayerDragEnd)
    val currentOnPlayerDragCancel by rememberUpdatedState(onPlayerDragCancel)
    var showTrackActions by remember { mutableStateOf(false) }
    val dismissGestureModifier = if (interactionEnabled) {
        Modifier.pointerInput(Unit) {
            val velocityTracker = VelocityTracker()
            detectVerticalDragGestures(
                onDragStart = {
                    velocityTracker.resetTracking()
                    currentOnPlayerDragStart()
                },
                onVerticalDrag = { change, dragAmount ->
                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                    currentOnPlayerDrag(dragAmount)
                    change.consume()
                },
                onDragEnd = {
                    currentOnPlayerDragEnd(velocityTracker.calculateVelocity().y)
                },
                onDragCancel = {
                    currentOnPlayerDragCancel()
                },
            )
        }
    } else {
        Modifier
    }
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
            ArtworkFlowBackground(
                artwork = artworkBlend.currentBitmap,
                isDark = isDark,
                animate = drawInPlace && playback.isPlaying,
                animateColorTransition = drawInPlace,
                onStatusBarBackgroundDarkChanged = onStatusBarBackgroundDarkChanged,
                modifier = Modifier.fillMaxSize(),
            )
            Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PlayerHeader(
                        item = item,
                        errorMessage = playback.errorMessage,
                        titleColor = emphasisControlColor,
                        artistColor = artistControlColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp, top = 16.dp, end = 32.dp, bottom = 10.dp)
                            .height(64.dp),
                    )
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        val landscape = maxWidth >= 600.dp && maxWidth > maxHeight
                        if (landscape) {
                            val artworkSize = minOf(
                                maxHeight - 24.dp,
                                (maxWidth - 32.dp) / 2f,
                                292.dp,
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(32.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PlayerMiddlePager(
                                    lyrics = lyrics,
                                    positionMs = playback.positionMs,
                                    onSeek = onSeek,
                                    size = artworkSize,
                                    controlColor = controlColor,
                                    emphasisControlColor = emphasisControlColor,
                                    playWhenReady = playback.playWhenReady,
                                    artworkBlend = artworkBlend,
                                    artworkCornerRadius = artworkCornerRadius,
                                    sharedArtworkVisible = sharedArtworkVisible,
                                    onArtworkBoundsChanged = onArtworkBoundsChanged,
                                    modifier = Modifier.weight(1f),
                                )
                                PlayerDetails(
                                    modifier = Modifier.width(artworkSize),
                                    playback = playback,
                                    controlColor = controlColor,
                                    emphasisControlColor = emphasisControlColor,
                                    onTogglePlayPause = onTogglePlayPause,
                                    onPrevious = onPrevious,
                                    onNext = onNext,
                                    onSeek = onSeek,
                                    onCyclePlaybackMode = onCyclePlaybackMode,
                                    onOpenQueue = onOpenQueue,
                                    onOpenTrackActions = {
                                        if (currentTrack != null) showTrackActions = true
                                    },
                                )
                            }
                        } else {
                            val artworkSize = minOf(
                                maxWidth - 56.dp,
                                maxHeight * 0.58f,
                            ).coerceAtMost(344.dp)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                PlayerMiddlePager(
                                    lyrics = lyrics,
                                    positionMs = playback.positionMs,
                                    onSeek = onSeek,
                                    size = artworkSize,
                                    controlColor = controlColor,
                                    emphasisControlColor = emphasisControlColor,
                                    playWhenReady = playback.playWhenReady,
                                    artworkBlend = artworkBlend,
                                    artworkCornerRadius = artworkCornerRadius,
                                    sharedArtworkVisible = sharedArtworkVisible,
                                    onArtworkBoundsChanged = onArtworkBoundsChanged,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                )
                                Spacer(Modifier.height(24.dp))
                                PlayerDetails(
                                    modifier = Modifier.width(artworkSize),
                                    playback = playback,
                                    controlColor = controlColor,
                                    emphasisControlColor = emphasisControlColor,
                                    onTogglePlayPause = onTogglePlayPause,
                                    onPrevious = onPrevious,
                                    onNext = onNext,
                                    onSeek = onSeek,
                                    onCyclePlaybackMode = onCyclePlaybackMode,
                                    onOpenQueue = onOpenQueue,
                                    onOpenTrackActions = {
                                        if (currentTrack != null) showTrackActions = true
                                    },
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
private fun PlayerMiddlePager(
    lyrics: LyricsUiState,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    size: Dp,
    controlColor: Color,
    emphasisControlColor: Color,
    playWhenReady: Boolean,
    artworkBlend: ArtworkBlend,
    artworkCornerRadius: Dp,
    sharedArtworkVisible: Boolean,
    onArtworkBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    HorizontalPager(
        state = pagerState,
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
                PlayerArtwork(
                    size = size,
                    playWhenReady = playWhenReady,
                    artworkBlend = artworkBlend,
                    cornerRadius = artworkCornerRadius,
                    sharedArtworkVisible = sharedArtworkVisible,
                    onArtworkBoundsChanged = onArtworkBoundsChanged,
                )
            } else {
                SyncedLyrics(
                    lyrics = lyrics,
                    positionMs = positionMs,
                    onSeek = onSeek,
                    contentWidth = size,
                    controlColor = controlColor,
                    emphasisControlColor = emphasisControlColor,
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
    onSeek: (Long) -> Unit,
    contentWidth: Dp,
    controlColor: Color,
    emphasisControlColor: Color,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.12f to Color.Black,
                        0.88f to Color.Black,
                        1f to Color.Transparent,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        when (lyrics) {
            LyricsUiState.Loading -> Text(
                text = stringResource(R.string.lyrics_loading),
                style = MiuixTheme.textStyles.body1,
                color = controlColor.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
            )

            LyricsUiState.Unavailable -> Text(
                text = stringResource(R.string.lyrics_unavailable),
                style = MiuixTheme.textStyles.body1,
                color = controlColor.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
            )

            is LyricsUiState.Available -> {
                val document = lyrics.document
                val currentLine = document.currentLineIndex(positionMs)
                val listState = rememberLazyListState()
                val horizontalPadding =
                    ((maxWidth - contentWidth) / 2f).coerceAtLeast(12.dp)
                LaunchedEffect(document, currentLine) {
                    if (currentLine >= 0) {
                        listState.animateScrollToItem(currentLine)
                    } else {
                        listState.scrollToItem(0)
                    }
                }
                val centeringPadding = (maxHeight / 2f - 28.dp).coerceAtLeast(24.dp)
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = horizontalPadding,
                        top = centeringPadding,
                        end = horizontalPadding,
                        bottom = centeringPadding,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(
                        items = document.lines,
                        key = { index, line -> "${line.startTimeMs}:$index" },
                    ) { index, line ->
                        val isCurrent = index == currentLine
                        val lyricsText = remember(line.text) {
                            line.text
                                .lineSequence()
                                .map(String::trim)
                                .filter(String::isNotEmpty)
                                .toList()
                        }
                        val mainLyricText = lyricsText.firstOrNull().orEmpty()
                        val translationText = lyricsText
                            .drop(1)
                            .joinToString("\n")
                            .ifBlank { null }
                        val lyricRowShape = RoundedCornerShape(12.dp)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(lyricRowShape)
                                .clickable { onSeek(line.startTimeMs) }
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 8.dp,
                                ),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            LyricLine(
                                line = line,
                                lineIndex = index,
                                isCurrent = isCurrent,
                                positionMs = positionMs,
                                document = document,
                                controlColor = controlColor,
                                emphasisControlColor = emphasisControlColor,
                            )
                            translationText?.let { translation ->
                                Text(
                                    text = translation,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = MiuixTheme.textStyles.body1.copy(
                                        fontSize = if (isCurrent) 16.sp else 13.2.sp,
                                    ),
                                    color = if (isCurrent) {
                                        emphasisControlColor.copy(alpha = 0.8f)
                                    } else {
                                        controlColor.copy(alpha = 0.48f)
                                    },
                                    fontWeight = FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricLine(
    line: com.melox.player.model.TimedLyricLine,
    lineIndex: Int,
    isCurrent: Boolean,
    positionMs: Long,
    document: LyricsDocument,
    controlColor: Color,
    emphasisControlColor: Color,
) {
    val words = line.words
    if (words == null) {
        // No word-level timing: render plain line text
        val lyricText = line.text.lines().firstOrNull(String::isNotBlank).orEmpty()
        val lineProgress = if (isCurrent) document.lineProgress(positionMs) else 0f
        Text(
            text = lyricText,
            modifier = Modifier.fillMaxWidth(),
            style = if (isCurrent) {
                MiuixTheme.textStyles.title3.copy(fontSize = 21.sp)
            } else {
                MiuixTheme.textStyles.body1.copy(fontSize = 17.sp)
            },
            color = if (isCurrent) {
                val progressAlpha = 1.0f - (lineProgress * 0.28f)
                emphasisControlColor.copy(alpha = 0.4f + (progressAlpha - 0.4f).coerceIn(0f, 1f))
            } else {
                controlColor.copy(alpha = 0.6f)
            },
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
        )
    } else {
        // Word-level timing: render each word with individual progress
        val baseStyle = if (isCurrent) {
            MiuixTheme.textStyles.title3.copy(fontSize = 21.sp, fontWeight = FontWeight.Bold)
        } else {
            MiuixTheme.textStyles.body1.copy(fontSize = 17.sp, fontWeight = FontWeight.Normal)
        }
        val currentWordIndex = document.currentWordIndex(lineIndex, positionMs)
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            LyricsWordFlow(
                words = words,
                currentWordIndex = currentWordIndex,
                positionMs = positionMs,
                isCurrentLine = isCurrent,
                baseStyle = baseStyle,
                controlColor = controlColor,
                emphasisControlColor = emphasisControlColor,
            )
        }
    }
}

@Composable
private fun LyricsWordFlow(
    words: List<com.melox.player.model.TimedWord>,
    currentWordIndex: Int,
    positionMs: Long,
    isCurrentLine: Boolean,
    baseStyle: androidx.compose.ui.text.TextStyle,
    controlColor: Color,
    emphasisControlColor: Color,
) {
    val dimmedColor = if (isCurrentLine) {
        controlColor.copy(alpha = 0.48f)
    } else {
        controlColor.copy(alpha = 0.38f)
    }
    val sungColor = emphasisControlColor.copy(alpha = 0.92f)
    val activeColor = emphasisControlColor

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        words.forEachIndexed { index, word ->
            val isWordSung = index < currentWordIndex
            val isActiveWord = index == currentWordIndex
            val isUpcoming = index > currentWordIndex

            val wordProgress = if (isActiveWord && isCurrentLine) {
                val duration = word.endTimeMs - word.startTimeMs
                if (duration > 0) {
                    ((positionMs - word.startTimeMs).toFloat() / duration)
                        .coerceIn(0f, 1f)
                } else 1f
            } else if (isWordSung) {
                1f
            } else {
                0f
            }

            if (isCurrentLine && (isActiveWord || isWordSung)) {
                Box {
                    // Full dimmed background
                    Text(
                        text = word.text,
                        style = baseStyle,
                        color = dimmedColor,
                    )
                    // Sung portion, clipped horizontally by progress
                    Text(
                        text = word.text,
                        style = baseStyle,
                        color = if (isWordSung) sungColor else activeColor,
                        modifier = Modifier
                            .graphicsLayer {
                                // Clip from left: scaleX from 0 to 1 reveals left-to-right
                                val clipX = wordProgress.coerceIn(0.001f, 1f)
                                scaleX = clipX
                                transformOrigin = TransformOrigin(0f, 0.5f)
                            },
                    )
                }
            } else {
                Text(
                    text = word.text,
                    style = baseStyle,
                    color = dimmedColor,
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
    // 播放/暂停封面缩放动画，复刻 FlamingoSank 播放恢复时的放大回弹：
    // 先快速放大冲过 1.0 到 1.02，再以渐缓的减速回到 1.0（速度渐缓）。
    // 暂停时用 350ms EaseOutQuart 缩小到 0.9。
    // 缩放比例保持不变（1f <-> 0.9f 以及回弹峰值 1.02）。
    val pauseTween: AnimationSpec<Float> = remember {
        tween(
            durationMillis = 350,
            easing = EaseOutQuart,
        )
    }
    val jumpOutTween: AnimationSpec<Float> = remember {
        tween(
            durationMillis = 180,
            easing = LinearOutSlowInEasing,
        )
    }
    val settleTween: AnimationSpec<Float> = remember {
        tween(
            durationMillis = 160,
            easing = LinearOutSlowInEasing,
        )
    }
    val playbackScale = remember {
        Animatable(if (playWhenReady) 1f else 0.9f)
    }
    var previousPlayWhenReady by remember { mutableStateOf(playWhenReady) }
    LaunchedEffect(playWhenReady) {
        val resumed = playWhenReady && !previousPlayWhenReady
        previousPlayWhenReady = playWhenReady
        when {
            resumed -> {
                playbackScale.animateTo(
                    targetValue = 1.02f,
                    animationSpec = jumpOutTween,
                )
                playbackScale.animateTo(
                    targetValue = 1f,
                    animationSpec = settleTween,
                )
            }

            !playWhenReady -> playbackScale.animateTo(
                targetValue = 0.9f,
                animationSpec = pauseTween,
            )
        }
    }
    val artworkScale = playbackScale.value
    var artworkLayoutBounds by remember { mutableStateOf(Rect.Zero) }
    var artworkContentBoundsInFrame by remember(artworkBlend.currentBitmap) {
        mutableStateOf(Rect.Zero)
    }
    SideEffect {
        if (artworkLayoutBounds.width > 0f && artworkLayoutBounds.height > 0f) {
            val unscaledArtworkBounds = when {
                artworkContentBoundsInFrame.width > 0f &&
                    artworkContentBoundsInFrame.height > 0f -> Rect(
                    left = artworkLayoutBounds.left + artworkContentBoundsInFrame.left,
                    top = artworkLayoutBounds.top + artworkContentBoundsInFrame.top,
                    right = artworkLayoutBounds.left + artworkContentBoundsInFrame.right,
                    bottom = artworkLayoutBounds.top + artworkContentBoundsInFrame.bottom,
                )

                artworkBlend.currentBitmap != null -> fittedArtworkRect(
                    bounds = artworkLayoutBounds,
                    bitmapWidth = artworkBlend.currentBitmap.width,
                    bitmapHeight = artworkBlend.currentBitmap.height,
                )

                else -> artworkLayoutBounds
            }
            onArtworkBoundsChanged(
                scaledRectAroundCenter(
                    bounds = unscaledArtworkBounds,
                    scale = artworkScale,
                ),
            )
        }
    }
    val shadowAlpha = 1f
    Box(
        modifier = Modifier
            .size(size)
            .onGloballyPositioned { coordinates ->
                artworkLayoutBounds = coordinates.boundsInRoot()
            }
            .graphicsLayer {
                alpha = if (sharedArtworkVisible) 1f else 0f
                scaleX = artworkScale
                scaleY = artworkScale
                clip = false
            },
    ) {
        if (artworkBlend.hasPreviousFrame && artworkBlend.progress < 1f) {
            PlaybackArtworkFrame(
                bitmap = artworkBlend.previousBitmap,
                size = size,
                cornerRadius = cornerRadius,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = 1f - artworkBlend.progress
                },
                contentScale = ContentScale.Fit,
                shadowElevation = 6.dp * shadowAlpha * (1f - artworkBlend.progress),
                ambientShadowColor = Color.Black.copy(
                    alpha = 0.16f * shadowAlpha * (1f - artworkBlend.progress),
                ),
                spotShadowColor = Color.Black.copy(
                    alpha = 0.22f * shadowAlpha * (1f - artworkBlend.progress),
                ),
            )
        }
        PlaybackArtworkFrame(
            bitmap = artworkBlend.currentBitmap,
            size = size,
            cornerRadius = cornerRadius,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = artworkBlend.progress
                },
            contentScale = ContentScale.Fit,
            shadowElevation = 6.dp * shadowAlpha * artworkBlend.progress,
            ambientShadowColor = Color.Black.copy(alpha = 0.16f * shadowAlpha * artworkBlend.progress),
            spotShadowColor = Color.Black.copy(alpha = 0.22f * shadowAlpha * artworkBlend.progress),
            onContentBoundsChanged = { bounds ->
                artworkContentBoundsInFrame = bounds
            },
        )
    }
}

@Composable
private fun PlayerHeader(
    item: PlaybackQueueItem?,
    errorMessage: String?,
    titleColor: Color,
    artistColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = item?.title ?: stringResource(R.string.no_track_selected),
            modifier = Modifier.fillMaxWidth(),
            style = MiuixTheme.textStyles.title3,
            color = titleColor,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item?.artist ?: stringResource(R.string.music_unknown_artist),
            modifier = Modifier.fillMaxWidth(),
            style = MiuixTheme.textStyles.body2,
            color = artistColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (errorMessage != null) {
            Text(
                text = stringResource(R.string.playback_error),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PlayerDetails(
    playback: PlaybackUiState,
    controlColor: Color,
    emphasisControlColor: Color,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onCyclePlaybackMode: () -> Unit,
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
            PlayerProgress(
                positionMs = playback.positionMs,
                durationMs = playback.durationMs,
                enabled = playback.currentItem != null,
                indicatorColor = emphasisControlColor,
                labelColor = controlColor,
                onSeek = onSeek,
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth(),
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
                        modifier = Modifier.size(28.dp),
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
                        modifier = Modifier.size(28.dp),
                        tint = emphasisControlColor,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlaybackModeButton(
                    mode = playback.playbackMode,
                    tint = controlColor,
                    onClick = onCyclePlaybackMode,
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
                modifier = Modifier.size(48.dp),
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
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    val interactionEnabled = enabled && durationMs > 0L
    var seekPositionMs by remember { mutableFloatStateOf(positionMs.toFloat()) }
    var isSeeking by remember { mutableStateOf(false) }
    val currentOnSeek by rememberUpdatedState(onSeek)
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
        targetValue = if (isSeeking) 12.dp else 6.dp,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 550f),
        label = "playerProgressPressedHeight",
    )
    val indicatorWidthExpansion by animateDpAsState(
        targetValue = if (isSeeking) 8.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 550f),
        label = "playerProgressPressedWidth",
    )
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
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
                    fun updatePosition(horizontalPosition: Float) {
                        val fraction = if (size.width == 0) {
                            0f
                        } else {
                            (horizontalPosition / size.width).coerceIn(0f, 1f)
                        }
                        seekPositionMs = fraction * safeDuration.toFloat()
                    }
                    isSeeking = true
                    updatePosition(down.position.x)
                    down.consume()
                    try {
                        val completed = drag(down.id) { change ->
                            updatePosition(change.position.x)
                            change.consume()
                        }
                        if (completed) currentOnSeek(seekPositionMs.roundToLong())
                    } finally {
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
        modifier = Modifier.fillMaxWidth(),
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
