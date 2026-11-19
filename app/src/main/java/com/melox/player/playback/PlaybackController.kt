package com.melox.player.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
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
import java.util.concurrent.atomic.AtomicLong
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
    private val lastTrackSkipTapElapsedRealtimeMs = AtomicLong(Long.MIN_VALUE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val initialSnapshot = MiniPlaybackSnapshotStore(applicationContext).load()
    private val mutableState = MutableStateFlow(
        initialSnapshot.toInitialPlaybackState(),
    )
    val state: StateFlow<PlaybackUiState> = mutableState.asStateFlow()
    private var pendingPlaybackModeChange: PendingPlaybackModeChange? = null
    private var playbackModeChangeTimeoutJob: Job? = null
    private var homeRecommendationPlaybackJob: Job? = null
    private var pendingPlaybackIntent: Boolean? = null
    private var audioFocusPauseRevision = 0L
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
    private val queueProjection = PlaybackQueueProjection()
    private var timerCommandSerial = 0L
    private var timerCommandPending = false

    private val trackDirection = TrackChangeDirectionState()
    private var playbackIteration = 0L
    private var positionDiscontinuityRevision = 0L

    private val listener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (isForcedPlaybackPause(playWhenReady, reason)) {
                pendingPlaybackIntent = null
                playbackIntentTimeoutJob?.cancel()
                playbackIntentTimeoutJob = null
            }
            // Button intent must not wait for the next bundled event or position sample.
            mutableState.value = mutableState.value.copy(
                playWhenReady = pendingPlaybackIntent ?: playWhenReady,
            )
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            positionDiscontinuityRevision += 1L
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // Preserve playback identity even when collectors conflate a rapid A/B/A transition.
            playbackIteration += 1L
            val player = if (controllerFuture.isDone) runCatching(controllerFuture::get).getOrNull() else null
            val previous = mutableState.value
            val nextIndex = when {
                player == null -> previous.queue.indexOfFirst { it.mediaId == mediaItem?.mediaId }
                player.currentMediaItem?.mediaId == mediaItem?.mediaId -> player.currentMediaItemIndex
                else -> (0 until player.mediaItemCount).firstOrNull {
                    player.getMediaItemAt(it).mediaId == mediaItem?.mediaId
                } ?: -1
            }
            trackDirection.onTransition(
                mediaId = mediaItem?.mediaId,
                seek = reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK,
                automatic = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
                nextIndex = nextIndex,
                queueSize = player?.mediaItemCount ?: previous.queue.size,
            )
        }

        override fun onEvents(player: Player, events: Player.Events) {
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

    fun applyStartupPlayback(allowAutoPlay: Boolean) = withController { controller ->
        controller.sendCustomCommand(
            SessionCommand(ACTION_APPLY_STARTUP_PLAYBACK, Bundle.EMPTY),
            Bundle().apply { putBoolean(EXTRA_ALLOW_STARTUP_AUTO_PLAY, allowAutoPlay) },
        )
    }

    /** Replaces playback with the visible result set and starts at [startIndex]. */
    fun playQueue(
        tracks: List<MusicTrack>,
        startIndex: Int,
        requestedMode: PlaybackMode? = null,
    ) {
        if (startIndex !in tracks.indices) return
        val sourceItems = tracks.mapIndexed { index, track ->
            track.toPlaybackQueueItem(
                context = applicationContext,
                sourceOrder = index.toDouble(),
                playbackMode = mutableState.value.playbackMode,
            )
        }
        withController { controller ->
            val playbackMode = requestedMode ?: controller.currentPlaybackMode()
            if (requestedMode != null) {
                finishPlaybackModeChange()
                PlaybackModeMemory.set(playbackMode)
                mutableState.value = mutableState.value.copy(playbackMode = playbackMode)
            }
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
        sendSleepTimerCommand(ACTION_SET_TIMER_EXTENSION, Bundle().apply {
            putBoolean(EXTRA_TIMER_EXTENSION, enabled)
        })
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
        mutableSleepTimerState.value = SleepTimerState(
            active = true,
            remainingSeconds = seconds,
            interruptionNotice = mutableSleepTimerState.value.interruptionNotice,
        )
        sendSleepTimerCommand(ACTION_START_SLEEP_TIMER, Bundle().apply {
            putInt(EXTRA_TIMER_SECONDS, seconds)
            putBoolean(EXTRA_TIMER_EXTENSION, mutableAutoExtendSleepTimer.value)
        })
    }

    fun cancelSleepTimer(interrupted: Boolean = false) {
        mutableSleepTimerState.value = SleepTimerState(
            interruptionNotice = mutableSleepTimerState.value.interruptionNotice + if (interrupted) 1 else 0,
        )
        sendSleepTimerCommand(ACTION_CANCEL_SLEEP_TIMER, Bundle().apply {
            putBoolean(EXTRA_TIMER_INTERRUPTED, interrupted)
        })
    }

    fun acknowledgeSleepTimerInterruption() {
        mutableSleepTimerState.value = mutableSleepTimerState.value.copy(interruptionNotice = 0)
        sendSleepTimerCommand(ACTION_ACKNOWLEDGE_TIMER_INTERRUPTION)
    }

    private fun sendSleepTimerCommand(action: String, args: Bundle = Bundle.EMPTY) {
        val serial = ++timerCommandSerial
        timerCommandPending = true
        withController { controller ->
            val future = controller.sendCustomCommand(SessionCommand(action, Bundle.EMPTY), args)
            future.addListener({
                if (serial != timerCommandSerial || released.get()) return@addListener
                timerCommandPending = false
                val result = runCatching { future.get() }.getOrNull()
                val timer = result?.extras?.getBundle(EXTRA_SLEEP_TIMER_STATE)
                    ?: controller.sessionExtras.getBundle(EXTRA_SLEEP_TIMER_STATE)
                timer?.let { mutableSleepTimerState.value = it.toSleepTimerState() }
            }, mainExecutor)
        }
    }

    fun seekTo(positionMs: Long) = withController { it.seekTo(positionMs.coerceAtLeast(0L)) }

    fun previous() = skipTrack(-1)

    fun next() = skipTrack(1)

    private fun skipTrack(direction: Int): Boolean {
        if (!acceptTrackSkip(SystemClock.elapsedRealtime())) return false
        var dispatched = false
        withController { controller ->
            val targetIndex = controller.adjacentMediaItemIndex(direction) ?: return@withController
            trackDirection.observeCurrentItem(controller.currentMediaItem?.mediaId, controller.currentMediaItemIndex)
            if (targetIndex != controller.currentMediaItemIndex) {
                // Set the accepted intent before MediaController can publish its optimistic target.
                trackDirection.request(controller.getMediaItemAt(targetIndex).mediaId, direction)
            }
            controller.seekToDefaultPosition(targetIndex)
            controller.play()
            dispatched = true
        }
        return dispatched
    }

    internal fun acceptTrackSkip(nowElapsedRealtimeMs: Long): Boolean =
        acceptTrackSkip(lastTrackSkipTapElapsedRealtimeMs, nowElapsedRealtimeMs)

    fun cyclePlaybackMode() {
        if (!playbackModeChangeInFlight.compareAndSet(false, true)) return
        withController { controller ->
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
            controller.applyPlaybackMode(targetMode, reordered)
            if (queue.isEmpty()) finishPlaybackModeChange()
        }
    }

    fun playNext(track: MusicTrack, onAdded: () -> Unit) {
        withController { controller ->
            if (!controller.isCommandAvailable(Player.COMMAND_CHANGE_MEDIA_ITEMS)) return@withController
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
            onAdded()
        }
    }

    fun append(track: MusicTrack, onAdded: () -> Unit) {
        withController { controller ->
            if (!controller.isCommandAvailable(Player.COMMAND_CHANGE_MEDIA_ITEMS)) return@withController
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
            onAdded()
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

    fun removeContentUris(contentUris: Set<String>) {
        if (contentUris.isEmpty()) return
        withController { controller ->
            controller.sendCustomCommand(
                SessionCommand(ACTION_REMOVE_QUEUE_CONTENT_URIS, Bundle.EMPTY),
                Bundle().apply { putStringArrayList(EXTRA_REMOVED_CONTENT_URIS, ArrayList(contentUris)) },
            )
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

    @OptIn(UnstableApi::class)
    private fun publish(player: Player) {
        val controller = player as? MediaController
        if (!timerCommandPending) {
            controller?.sessionExtras?.getBundle(EXTRA_SLEEP_TIMER_STATE)?.let { timer ->
                mutableSleepTimerState.value = timer.toSleepTimerState()
            }
        }
        val floatOutputActive = controller?.sessionExtras
            ?.getBoolean(EXTRA_FLOAT_OUTPUT_ACTIVE, false) ?: false
        if (pendingPlaybackSpeed == player.playbackParameters.speed) {
            pendingPlaybackSpeed = null
            pendingSpeedTimeoutJob?.cancel()
        }
        val focusPauseRevision = controller?.sessionExtras?.getLong(EXTRA_AUDIO_FOCUS_PAUSE_REVISION) ?: 0L
        if (focusPauseRevision != audioFocusPauseRevision || pendingPlaybackIntent == player.playWhenReady) {
            audioFocusPauseRevision = focusPauseRevision
            pendingPlaybackIntent = null
            playbackIntentTimeoutJob?.cancel()
            playbackIntentTimeoutJob = null
        }
        val playbackMode = displayedPlaybackMode(
            playerMode = player.currentPlaybackMode(),
            pendingMode = pendingPlaybackModeChange?.mode,
        )
        val queue = queueProjection.queue(player, playbackMode)
        if (queueClearPending && queue.isNotEmpty()) return
        queueClearPending = false
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it >= 0L }
            ?: queue.getOrNull(player.currentMediaItemIndex)?.durationMs
            ?: 0L
        val positionMs = player.currentPosition.coerceAtLeast(0L)
        trackDirection.observeCurrentItem(player.currentMediaItem?.mediaId, player.currentMediaItemIndex)
        val liveState = PlaybackUiState(
            queue = queue,
            currentIndex = player.currentMediaItemIndex.takeIf { queue.isNotEmpty() } ?: -1,
            isPlaying = pendingPlaybackIntent ?: player.isPlaying,
            playWhenReady = pendingPlaybackIntent ?: player.playWhenReady,
            positionMs = positionMs,
            playbackIteration = playbackIteration,
            positionDiscontinuityRevision = positionDiscontinuityRevision,
            trackChangeDirection = trackDirection.direction,
            positionUpdateElapsedRealtimeMs = SystemClock.elapsedRealtime(),
            playbackSpeed = pendingPlaybackSpeed ?: player.playbackParameters.speed,
            floatOutputActive = floatOutputActive,
            audioSessionId = player.audioSessionId,
            durationMs = duration.coerceAtLeast(0L),
            bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L),
            playbackMode = playbackMode,
            errorMessage = player.playerError?.localizedMessage,
        )
        mutableState.value = playbackStateDuringSnapshotRestore(
            previous = mutableState.value,
            live = liveState,
            snapshotRestorePending = controller?.sessionExtras
                ?.getBoolean(EXTRA_SNAPSHOT_RESTORE_PENDING, false) == true,
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

internal fun acceptTrackSkip(
    lastTapElapsedRealtimeMs: AtomicLong,
    nowElapsedRealtimeMs: Long,
): Boolean {
    val previous = lastTapElapsedRealtimeMs.getAndSet(nowElapsedRealtimeMs)
    return previous == Long.MIN_VALUE || nowElapsedRealtimeMs - previous > 300L
}

/** Keep direction attached to a media transition, not to repeated metadata/timeline callbacks. */
internal class TrackChangeDirectionState {
    var direction: Int = 1
        private set
    private var mediaId: String? = null
    private var index = -1
    private var initialized = false
    private var pending: Pair<String, Int>? = null

    fun observeCurrentItem(mediaId: String?, index: Int) {
        if (!initialized || this.mediaId == mediaId) {
            this.mediaId = mediaId
            this.index = index
            initialized = true
        }
    }

    fun request(mediaId: String, direction: Int) {
        pending = mediaId to direction
        this.direction = direction
    }

    fun onTransition(mediaId: String?, seek: Boolean, automatic: Boolean, nextIndex: Int, queueSize: Int) {
        val requested = pending?.takeIf { seek && it.first == mediaId }?.second
        if (requested != null || automatic || !initialized || this.mediaId != mediaId) {
            direction = resolveTrackChangeDirection(requested, automatic, index, nextIndex, queueSize)
        }
        // A duplicate or unrelated callback must not consume a later accepted skip.
        if (requested != null || automatic || mediaId == null) pending = null
        this.mediaId = mediaId
        index = nextIndex
        initialized = true
    }
}

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

internal fun playbackStateDuringSnapshotRestore(
    previous: PlaybackUiState,
    live: PlaybackUiState,
    snapshotRestorePending: Boolean,
): PlaybackUiState = if (
    snapshotRestorePending && live.currentItem == null && previous.currentItem != null
) {
    previous
} else {
    live
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

internal fun Player.applyPlaybackMode(mode: PlaybackMode, reordered: ReorderedPlaybackQueue) {
    PlaybackModeMemory.set(mode)
    shuffleModeEnabled = false
    repeatMode = mode.toPlayerRepeatMode()
    applyPlaybackQueue(reordered.queue, reordered.currentIndex)
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

/** Remove native queue slots without preparing or seeking a surviving current item. */
internal fun Player.removePlaybackContentUris(contentUris: Set<String>) {
    if (contentUris.isEmpty()) return
    val indices = (0 until mediaItemCount).filter { index ->
        getMediaItemAt(index).toPlaybackQueueItem().contentUri in contentUris
    }
    if (indices.isEmpty()) return
    if (indices.size == mediaItemCount) {
        stop()
        clearMediaItems()
    } else {
        indices.asReversed().forEach(::removeMediaItem)
    }
}
