package com.melox.player.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.melox.player.data.playback.MiniPlaybackSnapshotStore
import com.melox.player.model.MusicTrack
import com.melox.player.model.PlaybackSnapshot
import com.melox.player.model.PlaybackQueueItem
import com.melox.player.model.PlaybackMode
import com.melox.player.model.PlaybackUiState
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
import kotlin.random.Random

/** UI-facing controller for the service-owned Media3 session. */
class PlaybackController(context: Context) {
    private val applicationContext = context.applicationContext
    private val mainExecutor = ContextCompat.getMainExecutor(applicationContext)
    private val released = AtomicBoolean(false)
    private val playbackModeChangeInFlight = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val initialSnapshot = MiniPlaybackSnapshotStore(applicationContext).load()
    private val mutableState = MutableStateFlow(
        initialSnapshot.toInitialPlaybackState(),
    )
    val state: StateFlow<PlaybackUiState> = mutableState.asStateFlow()
    private var pendingPlaybackModeChange: PendingPlaybackModeChange? = null
    private var playbackModeChangeTimeoutJob: Job? = null

    private val listener = object : Player.Listener {
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
    ).buildAsync()

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
        withController { controller ->
            val playbackMode = controller.currentPlaybackMode()
            val queueTracks = buildHomeRecommendationPlaybackQueue(
                selectedTrackId = selectedTrack.id,
                recommendations = loadedRecommendations,
                allTracks = allTracks,
                playbackMode = playbackMode,
            )
            if (queueTracks.isEmpty()) return@withController
            val sourceOrderByTrackId = allTracks
                .mapIndexed { index, track -> track.id to index.toDouble() }
                .toMap()
            val queue = queueTracks.map { track ->
                track.toPlaybackQueueItem(
                    context = applicationContext,
                    sourceOrder = sourceOrderByTrackId.getValue(track.id),
                    playbackMode = playbackMode,
                )
            }
            controller.setMediaItems(
                queue.map(PlaybackQueueItem::toMediaItem),
                0,
                C.TIME_UNSET,
            )
            controller.shuffleModeEnabled = false
            controller.repeatMode = playbackMode.toPlayerRepeatMode()
            controller.prepare()
            controller.play()
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

    fun togglePlayPause() = withController { controller ->
        if (controller.playWhenReady) controller.pause() else {
            if (controller.playbackState == Player.STATE_IDLE) controller.prepare()
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) = withController { it.seekTo(positionMs.coerceAtLeast(0L)) }

    fun previous() = withController { controller ->
        controller.seekToAdjacentMediaItem(offset = -1)
    }

    fun next() = withController { controller ->
        controller.seekToAdjacentMediaItem(offset = 1)
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

    fun remove(index: Int) = withController { controller ->
        if (isValidQueueIndex(index, controller.mediaItemCount)) {
            controller.removeMediaItem(index)
        }
    }

    fun clear() = withController { controller ->
        controller.stop()
        controller.clearMediaItems()
    }

    fun release() {
        if (released.compareAndSet(false, true)) {
            finishPlaybackModeChange()
            runCatching(controllerFuture::get).getOrNull()?.removeListener(listener)
            scope.cancel()
            MediaController.releaseFuture(controllerFuture)
        }
    }

    private fun publish(player: Player) {
        val rawQueue = player.currentPlaybackQueue()
        val playbackMode = player.currentPlaybackMode()
        val queue = rawQueue.map { item -> item.copy(playbackMode = playbackMode) }
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it >= 0L }
            ?: queue.getOrNull(player.currentMediaItemIndex)?.durationMs
            ?: 0L
        mutableState.value = PlaybackUiState(
            queue = queue,
            currentIndex = player.currentMediaItemIndex.takeIf { queue.isNotEmpty() } ?: -1,
            isPlaying = player.isPlaying,
            playWhenReady = player.playWhenReady,
            positionMs = player.currentPosition.coerceAtLeast(0L),
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

internal fun PlaybackSnapshot?.toInitialPlaybackState(): PlaybackUiState {
    val snapshot = this ?: return PlaybackUiState()
    val item = snapshot.queue.getOrNull(snapshot.currentIndex) ?: return PlaybackUiState()
    return PlaybackUiState(
        queue = listOf(item),
        currentIndex = 0,
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

private fun Player.seekToAdjacentMediaItem(offset: Int) {
    if (mediaItemCount <= 0) return
    val currentIndex = currentMediaItemIndex
        .takeIf { isValidQueueIndex(it, mediaItemCount) }
        ?: 0
    val targetIndex = (currentIndex + offset)
        .floorMod(mediaItemCount)
    seekToDefaultPosition(targetIndex)
}

private fun Player.applyPlaybackQueue(
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
