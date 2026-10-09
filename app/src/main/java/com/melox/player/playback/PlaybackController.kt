package com.melox.player.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.melox.player.data.playback.MiniPlaybackSnapshotStore
import com.melox.player.model.MusicTrack
import com.melox.player.model.PlaybackSnapshot
import com.melox.player.model.PlaybackQueueItem
import com.melox.player.model.PlaybackMode
import com.melox.player.model.PlaybackUiState
import com.melox.player.model.PLAYBACK_SPEED_VALUES
import com.melox.player.model.SleepTimerState
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** UI-facing controller for the service-owned Media3 session. */
class PlaybackController(context: Context) {
    private val applicationContext = context.applicationContext
    private val mainExecutor = ContextCompat.getMainExecutor(applicationContext)
    private val released = AtomicBoolean(false)
    private val playbackModeChangeInFlight = AtomicBoolean(false)
    private val lastTrackSkipElapsedRealtimeMs = java.util.concurrent.atomic.AtomicLong(Long.MIN_VALUE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val initialSnapshot = MiniPlaybackSnapshotStore(applicationContext).load()
    private val mutableState = MutableStateFlow(
        initialSnapshot.toInitialPlaybackState(),
    )
    val state: StateFlow<PlaybackUiState> = mutableState.asStateFlow()
    private var pendingPlaybackModeChange: PendingPlaybackModeChange? = null
    private var playbackModeChangeTimeoutJob: Job? = null
    private var homeRecommendationPlaybackJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var extendedTimerItemIndex: Int? = null
    private var pendingPlaybackIntent: Boolean? = null
    private var playbackIntentTimeoutJob: Job? = null
    private val mutableSleepTimerState = MutableStateFlow(SleepTimerState())
    val sleepTimerState: StateFlow<SleepTimerState> = mutableSleepTimerState.asStateFlow()
    private val mutableAutoExtendSleepTimer = MutableStateFlow(false)
    val autoExtendSleepTimer: StateFlow<Boolean> = mutableAutoExtendSleepTimer.asStateFlow()
    private val mutablePlaybackPauseFade = MutableStateFlow(false)
    val playbackPauseFade: StateFlow<Boolean> = mutablePlaybackPauseFade.asStateFlow()
    private val mutableHighPrecisionOutput = MutableStateFlow(false)
    val highPrecisionOutput: StateFlow<Boolean> = mutableHighPrecisionOutput.asStateFlow()
    private var pendingPlaybackSpeed: Float? = null
    private var pendingSpeedTimeoutJob: Job? = null
    private var speedRequestSerial = 0L
    private var homeRecommendationPlaybackRequest = 0L
    private var queueClearPending = false

    private var pendingTrackSkip: PendingTrackSkip? = null
    private var trackChangeDirection = 1
    private var playbackIteration = 0L

    private val listener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady && mutableSleepTimerState.value.extending) {
                cancelSleepTimer(interrupted = true)
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (mutableSleepTimerState.value.extending) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
                ) {
                    closeAtTimerEnd(
                        extended = true,
                        advanceIfStillOnIndex = if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                            extendedTimerItemIndex
                        } else null,
                    )
                } else {
                    cancelSleepTimer(interrupted = true)
                }
            }
            // Preserve playback identity even when collectors conflate a rapid A/B/A transition.
            playbackIteration += 1L
            val previous = mutableState.value
            val requestedDirection = pendingTrackSkip
                ?.takeIf {
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK &&
                        it.mediaId == mediaItem?.mediaId
                }
                ?.direction
            pendingTrackSkip = null
            trackChangeDirection = resolveTrackChangeDirection(
                requestedDirection = requestedDirection,
                automatic = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
                previousIndex = previous.currentIndex,
                nextIndex = previous.queue.indexOfFirst { it.mediaId == mediaItem?.mediaId },
                queueSize = previous.queue.size,
            )
        }

        override fun onEvents(player: Player, events: Player.Events) {
            if (mutableSleepTimerState.value.extending && !player.playWhenReady) {
                cancelSleepTimer(interrupted = true)
            }
            publish(player)
            finishPlaybackModeChangeIfApplied(player)
        }

        override fun onPlayerError(error: PlaybackException) {
            publishError(error.localizedMessage)
            finishPlaybackModeChange()
        }
    }
    private val controllerFuture = MediaController.Builder(
        applicationContext,
        SessionToken(
            applicationContext,
            ComponentName(applicationContext, PlaybackService::class.java),
        ),
    ).setListener(object : MediaController.Listener {
        override fun onExtrasChanged(controller: MediaController, extras: Bundle) {
            publish(controller)
        }
    }).buildAsync()

    init {
        controllerFuture.addListener(
            {
                runCatching(controllerFuture::get)
                    .onSuccess { controller ->
                        if (!released.get()) {
                            controller.addListener(listener)
                            publish(controller)
                        }
                    }
                    .onFailure { error -> publishError(error.localizedMessage) }
            },
            mainExecutor,
        )
        scope.launch {
            while (isActive) {
                delay(if (mutableState.value.isPlaying) 500L else 1_500L)
                withController(::publish)
            }
        }
    }

    /** Replaces playback with the visible result set and starts at [startIndex]. */
    fun playQueue(tracks: List<MusicTrack>, startIndex: Int) {
        if (startIndex !in tracks.indices) return
        val sourceItems = tracks.mapIndexed { index, track ->
            track.toPlaybackQueueItem(
                context = applicationContext,
                sourceOrder = index.toDouble(),
                playbackMode = mutableState.value.playbackMode,
            )
        }
        withController { controller ->
            val playbackMode = controller.currentPlaybackMode()
            val reordered = reorderQueueForPlaybackMode(
                queue = sourceItems.map { it.copy(playbackMode = playbackMode) },
                currentIndex = startIndex,
                targetMode = playbackMode,
            )
            controller.setMediaItems(
                reordered.queue.map(PlaybackQueueItem::toMediaItem),
                reordered.currentIndex,
                C.TIME_UNSET,
            )
            controller.shuffleModeEnabled = false
            controller.repeatMode = playbackMode.toPlayerRepeatMode()
            controller.prepare()
            controller.play()
        }
    }

    fun playHomeRecommendation(
        selectedTrack: MusicTrack,
        loadedRecommendations: List<MusicTrack>,
        allTracks: List<MusicTrack>,
    ) {
        val request = ++homeRecommendationPlaybackRequest
        homeRecommendationPlaybackJob?.cancel()
        withController { controller ->
            val playbackMode = controller.currentPlaybackMode()
            homeRecommendationPlaybackJob = scope.launch(Dispatchers.Default) {
                val queueTracks = buildHomeRecommendationPlaybackQueue(
                    selectedTrackId = selectedTrack.id,
                    recommendations = loadedRecommendations,
                    allTracks = allTracks,
                    playbackMode = playbackMode,
                )
                if (queueTracks.isEmpty()) return@launch
                val sourceOrderByTrackId = allTracks
                    .mapIndexed { index, track -> track.id to index.toDouble() }
                    .toMap()
                val mediaItems = queueTracks.map { track ->
                    track.toPlaybackQueueItem(
                        context = applicationContext,
                        sourceOrder = sourceOrderByTrackId.getValue(track.id),
                        playbackMode = playbackMode,
                    ).toMediaItem()
                }
                withContext(Dispatchers.Main.immediate) {
                    if (released.get() || request != homeRecommendationPlaybackRequest) {
                        return@withContext
                    }
                    controller.setMediaItems(mediaItems, 0, C.TIME_UNSET)
                    controller.shuffleModeEnabled = false
                    controller.repeatMode = playbackMode.toPlayerRepeatMode()
                    controller.prepare()
                    controller.play()
                }
            }
        }
    }

    fun playExternal(uri: Uri) {
        withController { controller ->
            val playbackMode = controller.currentPlaybackMode()
            controller.setMediaItem(
                uri.toExternalPlaybackQueueItem(applicationContext)
                    .copy(playbackMode = playbackMode)
                    .toMediaItem(),
            )
            controller.shuffleModeEnabled = false
            controller.repeatMode = playbackMode.toPlayerRepeatMode()
            controller.prepare()
            controller.play()
        }
    }

    fun togglePlayPause() {
        val play = !mutableState.value.playWhenReady
        if (!play && mutableSleepTimerState.value.extending) cancelSleepTimer(interrupted = true)
        if (mutablePlaybackPauseFade.value) {
            pendingPlaybackIntent = play
            mutableState.value = mutableState.value.copy(playWhenReady = play, isPlaying = play)
            playbackIntentTimeoutJob?.cancel()
            playbackIntentTimeoutJob = scope.launch {
                delay(1_000L)
                pendingPlaybackIntent = null
                withController(::publish)
            }
        }
        withController { controller ->
            if (mutablePlaybackPauseFade.value) {
                controller.sendCustomCommand(
                    SessionCommand(ACTION_SET_PLAYBACK_INTENT, Bundle.EMPTY),
                    Bundle().apply { putBoolean(EXTRA_PLAYBACK_INTENT, play) },
                )
            } else if (play) {
                if (controller.playbackState == Player.STATE_IDLE) controller.prepare()
                controller.play()
            } else {
                controller.pause()
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        if (speed !in PLAYBACK_SPEED_VALUES) return
        if (mutableState.value.floatOutputActive && speed != 1f) return
        val request = ++speedRequestSerial
        pendingPlaybackSpeed = speed
        mutableState.value = mutableState.value.copy(playbackSpeed = speed)
        pendingSpeedTimeoutJob?.cancel()
        pendingSpeedTimeoutJob = scope.launch {
            delay(1_500L)
            if (request == speedRequestSerial) {
                pendingPlaybackSpeed = null
                withController(::publish)
            }
        }
        withController { controller ->
            val result = controller.sendCustomCommand(
                SessionCommand(ACTION_SET_PLAYBACK_SPEED, Bundle.EMPTY),
                Bundle().apply { putFloat(EXTRA_PLAYBACK_SPEED, speed) },
            )
            result.addListener({
                if (request == speedRequestSerial &&
                    runCatching { result.get().resultCode }.getOrNull() !=
                    androidx.media3.session.SessionResult.RESULT_SUCCESS
                ) {
                    pendingPlaybackSpeed = null
                    pendingSpeedTimeoutJob?.cancel()
                    publish(controller)
                }
            }, mainExecutor)
        }
    }

    fun setHighPrecisionOutput(enabled: Boolean) {
        mutableHighPrecisionOutput.value = enabled
        withController { controller ->
            controller.sendCustomCommand(
                SessionCommand(ACTION_SET_HIGH_PRECISION_OUTPUT, Bundle.EMPTY),
                Bundle().apply { putBoolean(EXTRA_HIGH_PRECISION_OUTPUT, enabled) },
            )
        }
    }

    fun setAutoExtendSleepTimer(enabled: Boolean) {
        mutableAutoExtendSleepTimer.value = enabled
    }

    fun setPlaybackPauseFade(enabled: Boolean) {
        if (mutablePlaybackPauseFade.value == enabled) return
        mutablePlaybackPauseFade.value = enabled
        sendPauseFadeOption()
    }

    private fun sendPauseFadeOption() = withController { controller ->
        controller.sendCustomCommand(
            SessionCommand(ACTION_SET_PAUSE_FADE, Bundle.EMPTY),
            Bundle().apply { putBoolean(EXTRA_PAUSE_FADE, mutablePlaybackPauseFade.value) },
        )
    }

    fun startSleepTimer(seconds: Int) {
        if (seconds !in 1..86_399) return
        sleepTimerJob?.cancel()
        extendedTimerItemIndex = null
        val endTimeMs = SystemClock.elapsedRealtime() + seconds * 1_000L
        mutableSleepTimerState.value = SleepTimerState(active = true, remainingSeconds = seconds,
            interruptionNotice = mutableSleepTimerState.value.interruptionNotice)
        sleepTimerJob = scope.launch {
            while (isActive) {
                val tick = sleepTimerTick(endTimeMs, SystemClock.elapsedRealtime()) ?: break
                mutableSleepTimerState.value = mutableSleepTimerState.value.copy(
                    remainingSeconds = tick.remainingSeconds,
                )
                delay(tick.delayUntilNextSecondMs)
            }
            if (!isActive) return@launch
            val controller = if (controllerFuture.isDone) {
                runCatching(controllerFuture::get).getOrNull()
            } else null
            val mediaRemainingMs = controller?.let { it.duration - it.currentPosition }
                ?.takeIf { it > 0L && it < 86_400_000L }
            if (!mutableAutoExtendSleepTimer.value || controller?.isPlaying != true ||
                mediaRemainingMs == null
            ) {
                closeAtTimerEnd()
                return@launch
            }
            extendedTimerItemIndex = controller.currentMediaItemIndex
            while (isActive) {
                val remainingMediaMs = controller.duration - controller.currentPosition
                if (remainingMediaMs <= 0L || controller.playbackState == Player.STATE_ENDED) {
                    closeAtTimerEnd(
                        extended = true,
                        advanceIfStillOnIndex = extendedTimerItemIndex,
                    )
                    return@launch
                }
                mutableSleepTimerState.value = mutableSleepTimerState.value.copy(
                    extending = true,
                    remainingSeconds = 0,
                    extensionSeconds = remainingExtensionSeconds(
                        controller.duration,
                        controller.currentPosition,
                        controller.playbackParameters.speed,
                    ),
                )
                delay(250L)
            }
        }
    }

    fun cancelSleepTimer() = cancelSleepTimer(interrupted = false)

    fun acknowledgeSleepTimerInterruption() {
        mutableSleepTimerState.value = mutableSleepTimerState.value.copy(interruptionNotice = 0)
    }

    private fun cancelSleepTimer(interrupted: Boolean) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        extendedTimerItemIndex = null
        val notice = mutableSleepTimerState.value.interruptionNotice + if (interrupted) 1 else 0
        mutableSleepTimerState.value = SleepTimerState(interruptionNotice = notice)
    }

    private fun closeAtTimerEnd(
        extended: Boolean = false,
        advanceIfStillOnIndex: Int? = null,
    ) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        extendedTimerItemIndex = null
        mutableSleepTimerState.value = SleepTimerState(
            interruptionNotice = mutableSleepTimerState.value.interruptionNotice,
        )
        withController { controller ->
            controller.sendCustomCommand(
                SessionCommand(ACTION_CLOSE_APPLICATION_COMMAND, Bundle.EMPTY),
                Bundle().apply {
                    putBoolean(EXTRA_CLOSE_AFTER_EXTENSION, extended)
                    advanceIfStillOnIndex?.let {
                        putInt(EXTRA_EXTENSION_EXPECTED_INDEX, it)
                    }
                },
            )
        }
    }

    fun seekTo(positionMs: Long) = withController { it.seekTo(positionMs.coerceAtLeast(0L)) }

    fun previous() = skipTrack(-1)

    fun next() = skipTrack(1)

    private fun skipTrack(direction: Int) {
        if (!acceptTrackSkip(SystemClock.elapsedRealtime())) return
        withController { controller ->
            val targetIndex = controller.adjacentMediaItemIndex(direction) ?: return@withController
            pendingTrackSkip = if (targetIndex == controller.currentMediaItemIndex) null else {
                PendingTrackSkip(
                    mediaId = controller.getMediaItemAt(targetIndex).mediaId,
                    direction = direction,
                )
            }
            controller.seekToDefaultPosition(targetIndex)
            controller.play()
        }
    }

    internal fun acceptTrackSkip(nowElapsedRealtimeMs: Long): Boolean {
        while (true) {
            val previous = lastTrackSkipElapsedRealtimeMs.get()
            if (!shouldAcceptTrackSkip(previous, nowElapsedRealtimeMs)) return false
            if (lastTrackSkipElapsedRealtimeMs.compareAndSet(previous, nowElapsedRealtimeMs)) {
                return true
            }
        }
    }

    fun cyclePlaybackMode() {
        if (!playbackModeChangeInFlight.compareAndSet(false, true)) return
        withController { controller ->
            if (controller.mediaItemCount == 0) {
                finishPlaybackModeChange()
                return@withController
            }
            val queue = controller.currentPlaybackQueue()
            val currentMode = controller.currentPlaybackMode()
            val targetMode = nextPlaybackMode(currentMode)
            val reordered = reorderQueueForPlaybackMode(
                queue = queue,
                currentIndex = controller.currentMediaItemIndex,
                targetMode = targetMode,
            )
            beginPlaybackModeChange(targetMode, reordered.queue)
            mutableState.value = mutableState.value.copy(playbackMode = targetMode)
            PlaybackModeMemory.set(targetMode)
            controller.shuffleModeEnabled = false
            controller.repeatMode = targetMode.toPlayerRepeatMode()
            controller.applyPlaybackQueue(
                targetQueue = reordered.queue,
                targetCurrentIndex = reordered.currentIndex,
            )
        }
    }

    fun playNext(track: MusicTrack) {
        withController { controller ->
            val queue = controller.currentPlaybackQueue()
            val playbackMode = controller.currentPlaybackMode()
            val insertionIndex = nextQueueInsertionIndex(
                currentIndex = controller.currentMediaItemIndex,
                itemCount = controller.mediaItemCount,
            )
            val item = track.toPlaybackQueueItem(
                context = applicationContext,
                sourceOrder = sourceOrderForPlayNext(
                    queue = queue,
                    currentIndex = controller.currentMediaItemIndex,
                    playbackMode = playbackMode,
                ),
                playbackMode = playbackMode,
            ).toMediaItem()
            controller.addMediaItem(insertionIndex, item)
            if (controller.mediaItemCount == 1) {
                controller.prepare()
            }
        }
    }

    fun append(track: MusicTrack) {
        withController { controller ->
            val queue = controller.currentPlaybackQueue()
            val playbackMode = controller.currentPlaybackMode()
            val item = track.toPlaybackQueueItem(
                context = applicationContext,
                sourceOrder = queue.maxOfOrNull(PlaybackQueueItem::sourceOrder)
                    ?.plus(1.0)
                    ?: 0.0,
                playbackMode = playbackMode,
            ).toMediaItem()
            controller.addMediaItem(item)
            if (controller.mediaItemCount == 1) controller.prepare()
        }
    }

    fun jumpTo(index: Int) = withController { controller ->
        if (isValidQueueIndex(index, controller.mediaItemCount)) {
            controller.seekToDefaultPosition(index)
            controller.play()
        }
    }

    fun move(fromIndex: Int, toIndex: Int) = withController { controller ->
        if (isValidQueueMove(fromIndex, toIndex, controller.mediaItemCount)) {
            controller.moveMediaItem(fromIndex, toIndex)
        }
    }

    fun remove(index: Int) = withController { controller ->
        if (isValidQueueIndex(index, controller.mediaItemCount)) {
            controller.removeMediaItem(index)
        }
    }

    fun clear() {
        queueClearPending = true
        mutableState.value = PlaybackUiState(playbackMode = mutableState.value.playbackMode)
        withController { controller ->
            controller.stop()
            controller.clearMediaItems()
        }
    }

    fun release() {
        if (released.compareAndSet(false, true)) {
            finishPlaybackModeChange()
            playbackIntentTimeoutJob?.cancel()
            runCatching(controllerFuture::get).getOrNull()?.removeListener(listener)
            scope.cancel()
            MediaController.releaseFuture(controllerFuture)
        }
    }

    private fun publish(player: Player) {
        val controller = player as? MediaController
        val floatOutputActive = controller?.sessionExtras
            ?.getBoolean(EXTRA_FLOAT_OUTPUT_ACTIVE, false) ?: false
        if (floatOutputActive) {
            pendingPlaybackSpeed = null
            pendingSpeedTimeoutJob?.cancel()
        } else if (pendingPlaybackSpeed == player.playbackParameters.speed) {
            pendingPlaybackSpeed = null
            pendingSpeedTimeoutJob?.cancel()
        }
        if (pendingPlaybackIntent == player.playWhenReady) {
            pendingPlaybackIntent = null
            playbackIntentTimeoutJob?.cancel()
            playbackIntentTimeoutJob = null
        }
        val rawQueue = player.currentPlaybackQueue()
        if (queueClearPending && rawQueue.isNotEmpty()) return
        queueClearPending = false
        val playbackMode = displayedPlaybackMode(
            playerMode = player.currentPlaybackMode(),
            pendingMode = pendingPlaybackModeChange?.mode,
        )
        val queue = rawQueue.map { item -> item.copy(playbackMode = playbackMode) }
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it >= 0L }
            ?: queue.getOrNull(player.currentMediaItemIndex)?.durationMs
            ?: 0L
        val positionMs = player.currentPosition.coerceAtLeast(0L)
        mutableState.value = PlaybackUiState(
            queue = queue,
            currentIndex = player.currentMediaItemIndex.takeIf { queue.isNotEmpty() } ?: -1,
            isPlaying = pendingPlaybackIntent ?: player.isPlaying,
            playWhenReady = pendingPlaybackIntent ?: player.playWhenReady,
            positionMs = positionMs,
            playbackIteration = playbackIteration,
            trackChangeDirection = trackChangeDirection,
            positionUpdateElapsedRealtimeMs = SystemClock.elapsedRealtime(),
            playbackSpeed = pendingPlaybackSpeed ?: player.playbackParameters.speed,
            floatOutputActive = floatOutputActive,
            durationMs = duration.coerceAtLeast(0L),
            bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L),
            playbackMode = playbackMode,
            errorMessage = player.playerError?.localizedMessage,
        )
    }

    private fun publishError(message: String?) {
        mutableState.value = mutableState.value.copy(
            errorMessage = message?.takeIf(String::isNotBlank),
        )
    }

    private fun beginPlaybackModeChange(
        mode: PlaybackMode,
        queue: List<PlaybackQueueItem>,
    ) {
        pendingPlaybackModeChange = PendingPlaybackModeChange(mode, queue)
        playbackModeChangeTimeoutJob?.cancel()
        playbackModeChangeTimeoutJob = scope.launch {
            delay(1_000)
            finishPlaybackModeChange()
        }
    }

    private fun finishPlaybackModeChangeIfApplied(player: Player) {
        val pending = pendingPlaybackModeChange ?: return
        if (
            player.currentPlaybackMode() == pending.mode &&
            player.hasPlaybackQueueOrder(pending.queue)
        ) {
            finishPlaybackModeChange()
        }
    }

    private fun finishPlaybackModeChange() {
        pendingPlaybackModeChange = null
        playbackModeChangeTimeoutJob?.cancel()
        playbackModeChangeTimeoutJob = null
        playbackModeChangeInFlight.set(false)
    }

    private fun withController(block: (MediaController) -> Unit) {
        if (released.get()) return
        if (controllerFuture.isDone) {
            runCatching(controllerFuture::get).getOrNull()?.let(block)
            return
        }
        controllerFuture.addListener(
            {
                if (!released.get()) {
                    runCatching(controllerFuture::get).getOrNull()?.let(block)
                }
            },
            mainExecutor,
        )
    }
}

internal data class SleepTimerTick(
    val remainingSeconds: Int,
    val delayUntilNextSecondMs: Long,
)

internal fun sleepTimerTick(deadlineMs: Long, nowMs: Long): SleepTimerTick? {
    val remainingMs = deadlineMs - nowMs
    if (remainingMs <= 0L) return null
    val remainingSeconds = ((remainingMs + 999L) / 1_000L).toInt()
    return SleepTimerTick(
        remainingSeconds = remainingSeconds,
        delayUntilNextSecondMs = remainingMs - (remainingSeconds - 1) * 1_000L,
    )
}

internal fun remainingExtensionSeconds(
    durationMs: Long,
    positionMs: Long,
    playbackSpeed: Float,
): Int {
    if (durationMs <= positionMs || !playbackSpeed.isFinite() || playbackSpeed <= 0f) return 0
    val remainingRealMs = (durationMs - positionMs) / playbackSpeed.toDouble()
    return kotlin.math.ceil(remainingRealMs / 1_000.0).toInt().coerceAtLeast(0)
}

internal fun shouldAcceptTrackSkip(
    previousElapsedRealtimeMs: Long,
    nowElapsedRealtimeMs: Long,
    intervalMillis: Long = 300L,
): Boolean = previousElapsedRealtimeMs == Long.MIN_VALUE ||
    nowElapsedRealtimeMs - previousElapsedRealtimeMs >= intervalMillis

private data class PendingTrackSkip(val mediaId: String, val direction: Int)

internal fun PlaybackSnapshot?.toInitialPlaybackState(): PlaybackUiState {
    val snapshot = this ?: return PlaybackUiState()
    val item = snapshot.queue.getOrNull(snapshot.currentIndex) ?: return PlaybackUiState()
    return PlaybackUiState(
        queue = snapshot.queue,
        currentIndex = snapshot.currentIndex,
        isPlaying = false,
        playWhenReady = false,
        positionMs = snapshot.positionMs.coerceAtLeast(0L),
        durationMs = item.durationMs.coerceAtLeast(0L),
        bufferedPositionMs = 0L,
        playbackMode = snapshot.playbackMode,
    )
}

internal fun nextQueueInsertionIndex(currentIndex: Int, itemCount: Int): Int =
    (currentIndex + 1).coerceIn(0, itemCount.coerceAtLeast(0))

internal fun isValidQueueIndex(index: Int, itemCount: Int): Boolean =
    index in 0 until itemCount.coerceAtLeast(0)

internal fun isValidQueueMove(fromIndex: Int, toIndex: Int, itemCount: Int): Boolean =
    fromIndex != toIndex &&
        isValidQueueIndex(fromIndex, itemCount) &&
        isValidQueueIndex(toIndex, itemCount)

internal fun Player.currentPlaybackQueue(): List<PlaybackQueueItem> =
    List(mediaItemCount) { index ->
        getMediaItemAt(index).toPlaybackQueueItem()
    }

internal fun Player.currentPlaybackMode(): PlaybackMode {
    if (repeatMode == Player.REPEAT_MODE_ONE) return PlaybackMode.REPEAT_ONE
    return PlaybackModeMemory.get()
}

internal fun PlaybackMode.toPlayerRepeatMode(): Int =
    if (this == PlaybackMode.REPEAT_ONE) {
        Player.REPEAT_MODE_ONE
    } else {
        Player.REPEAT_MODE_ALL
    }

internal fun displayedPlaybackMode(
    playerMode: PlaybackMode,
    pendingMode: PlaybackMode?,
): PlaybackMode = pendingMode ?: playerMode

internal fun sourceOrderForPlayNext(
    queue: List<PlaybackQueueItem>,
    currentIndex: Int,
    playbackMode: PlaybackMode,
): Double {
    if (queue.isEmpty()) return 0.0
    if (playbackMode == PlaybackMode.RANDOM) {
        return queue.maxOf(PlaybackQueueItem::sourceOrder) + 1.0
    }
    val currentItem = queue.getOrNull(currentIndex)
        ?: return queue.minOf(PlaybackQueueItem::sourceOrder) - 1.0
    val nextItem = queue.getOrNull(currentIndex + 1)
    return if (nextItem != null && nextItem.sourceOrder > currentItem.sourceOrder) {
        currentItem.sourceOrder + (nextItem.sourceOrder - currentItem.sourceOrder) / 2.0
    } else {
        queue.maxOf(PlaybackQueueItem::sourceOrder) + 1.0
    }
}

private fun Player.adjacentMediaItemIndex(offset: Int): Int? {
    if (mediaItemCount <= 0) return null
    val currentIndex = currentMediaItemIndex
        .takeIf { isValidQueueIndex(it, mediaItemCount) }
        ?: 0
    return (currentIndex + offset).floorMod(mediaItemCount)
}

internal fun Player.applyPlaybackQueue(
    targetQueue: List<PlaybackQueueItem>,
    targetCurrentIndex: Int,
) {
    val currentIndex = currentMediaItemIndex
    if (
        targetQueue.size != mediaItemCount ||
        currentIndex !in 0 until mediaItemCount ||
        targetCurrentIndex !in targetQueue.indices ||
        hasPlaybackQueueOrder(targetQueue)
    ) {
        return
    }
    val currentItem = getMediaItemAt(currentIndex).toPlaybackQueueItem()
    if (!currentItem.isSameQueueSlot(targetQueue[targetCurrentIndex])) return

    val replacement = playbackQueueReplacement(
        targetQueue = targetQueue,
        currentIndex = currentIndex,
        targetCurrentIndex = targetCurrentIndex,
    )
    val beforeCurrent = replacement.beforeCurrent.map(PlaybackQueueItem::toMediaItem)
    val afterCurrent = replacement.afterCurrent.map(PlaybackQueueItem::toMediaItem)
    if (replacement.replaceAfterCurrentFirst) {
        replaceMediaItems(currentIndex + 1, mediaItemCount, afterCurrent)
        replaceMediaItems(0, currentIndex, beforeCurrent)
    } else {
        replaceMediaItems(0, currentIndex, beforeCurrent)
        replaceMediaItems(targetCurrentIndex + 1, mediaItemCount, afterCurrent)
    }
}

private fun Player.hasPlaybackQueueOrder(targetQueue: List<PlaybackQueueItem>): Boolean =
    targetQueue.size == mediaItemCount && targetQueue.indices.all { index ->
        getMediaItemAt(index).toPlaybackQueueItem().isSameQueueSlot(targetQueue[index])
    }

private fun PlaybackQueueItem.isSameQueueSlot(other: PlaybackQueueItem): Boolean =
    mediaId == other.mediaId &&
        contentUri == other.contentUri &&
        sourceOrder == other.sourceOrder

private fun Int.floorMod(modulus: Int): Int =
    ((this % modulus) + modulus) % modulus

internal data class ReorderedPlaybackQueue(
    val queue: List<PlaybackQueueItem>,
    val currentIndex: Int,
)

internal data class PlaybackQueueReplacement(
    val beforeCurrent: List<PlaybackQueueItem>,
    val afterCurrent: List<PlaybackQueueItem>,
    val replaceAfterCurrentFirst: Boolean,
)

private data class PendingPlaybackModeChange(
    val mode: PlaybackMode,
    val queue: List<PlaybackQueueItem>,
)

internal fun nextPlaybackMode(mode: PlaybackMode): PlaybackMode = when (mode) {
    PlaybackMode.ORDER -> PlaybackMode.REPEAT_ONE
    PlaybackMode.REPEAT_ONE -> PlaybackMode.RANDOM
    PlaybackMode.RANDOM -> PlaybackMode.ORDER
}

internal fun reorderQueueForPlaybackMode(
    queue: List<PlaybackQueueItem>,
    currentIndex: Int,
    targetMode: PlaybackMode,
    random: Random = Random.Default,
): ReorderedPlaybackQueue {
    if (queue.isEmpty()) return ReorderedPlaybackQueue(emptyList(), -1)
    val currentItem = queue.getOrNull(currentIndex) ?: queue.first()
    val reordered = when (targetMode) {
        PlaybackMode.ORDER -> queue.sortedBy(PlaybackQueueItem::sourceOrder)
        PlaybackMode.REPEAT_ONE -> queue.sortedBy(PlaybackQueueItem::sourceOrder)
        PlaybackMode.RANDOM -> buildList(queue.size) {
            add(currentItem)
            addAll(queue.filterNot { it.isSameQueueSlot(currentItem) }.shuffled(random))
        }
    }.map { it.copy(playbackMode = targetMode) }
    return ReorderedPlaybackQueue(
        queue = reordered,
        currentIndex = reordered.indexOfFirst { it.isSameQueueSlot(currentItem) }
            .coerceAtLeast(0),
    )
}

internal fun playbackQueueReplacement(
    targetQueue: List<PlaybackQueueItem>,
    currentIndex: Int,
    targetCurrentIndex: Int,
): PlaybackQueueReplacement {
    require(currentIndex in targetQueue.indices)
    require(targetCurrentIndex in targetQueue.indices)
    return PlaybackQueueReplacement(
        beforeCurrent = targetQueue.subList(0, targetCurrentIndex),
        afterCurrent = targetQueue.subList(targetCurrentIndex + 1, targetQueue.size),
        replaceAfterCurrentFirst = targetCurrentIndex >= currentIndex,
    )
}

internal fun buildHomeRecommendationPlaybackQueue(
    selectedTrackId: Long,
    recommendations: List<MusicTrack>,
    allTracks: List<MusicTrack>,
    playbackMode: PlaybackMode,
    random: Random = Random.Default,
): List<MusicTrack> {
    val uniqueTracks = allTracks.distinctBy(MusicTrack::id)
    val selectedTrack = uniqueTracks.firstOrNull { track -> track.id == selectedTrackId }
        ?: return emptyList()
    val availableTrackIds = uniqueTracks.mapTo(hashSetOf(), MusicTrack::id)
    val prefixIds = linkedSetOf(selectedTrackId)
    val remainingRecommendations = recommendations.filter { track ->
        track.id != selectedTrackId &&
            track.id in availableTrackIds &&
            prefixIds.add(track.id)
    }
    val remainingTracks = uniqueTracks.filterNot { track -> track.id in prefixIds }
    val modeOrderedTracks = when (playbackMode) {
        PlaybackMode.RANDOM -> remainingTracks.shuffled(random)
        PlaybackMode.ORDER,
        PlaybackMode.REPEAT_ONE -> remainingTracks
    }
    return listOf(selectedTrack) + remainingRecommendations + modeOrderedTracks
}

internal fun resolveTrackChangeDirection(
    requestedDirection: Int?,
    automatic: Boolean,
    previousIndex: Int,
    nextIndex: Int,
    queueSize: Int,
): Int = when {
    requestedDirection != null -> requestedDirection
    automatic -> 1
    previousIndex < 0 || nextIndex < 0 || previousIndex == nextIndex -> 1
    queueSize > 2 && previousIndex == 0 && nextIndex == queueSize - 1 -> -1
    previousIndex == queueSize - 1 && nextIndex == 0 -> 1
    nextIndex < previousIndex -> -1
    else -> 1
}
