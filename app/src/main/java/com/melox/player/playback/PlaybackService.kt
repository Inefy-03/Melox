package com.melox.player.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.melox.player.MainActivity
import com.melox.player.data.playback.PlaybackSnapshotStore
import com.melox.player.data.playback.MiniPlaybackSnapshotStore
import com.melox.player.model.PlaybackMode
import com.melox.player.model.PlaybackQueueItem
import com.melox.player.model.PlaybackSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

/** Owns background audio playback and exposes it to Android system media controls. */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var snapshotStore: PlaybackSnapshotStore
    private lateinit var miniSnapshotStore: MiniPlaybackSnapshotStore
    private var snapshotDebounceJob: Job? = null
    private var snapshotRestorePending = false
    private var snapshotRestoreSeed: PlaybackSnapshot? = null
    private val snapshotWriteMutex = Mutex()
    private val snapshotWriteSequence = AtomicLong()
    private val lastPersistedSnapshotSequence = AtomicLong()

    override fun onCreate() {
        super.onCreate()
        snapshotStore = PlaybackSnapshotStore(this)
        miniSnapshotStore = MiniPlaybackSnapshotStore(this)
        val startupSnapshot = miniSnapshotStore.load()
        snapshotRestoreSeed = startupSnapshot
        PlaybackModeMemory.set(startupSnapshot?.playbackMode ?: PlaybackMode.ORDER)
        val renderersFactory = DefaultRenderersFactory(this)
            .setEnableAudioFloatOutput(true)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        val player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.setWakeMode(C.WAKE_MODE_LOCAL)
        startupSnapshot?.let { snapshot ->
            player.setMediaItems(
                snapshot.queue.map(PlaybackQueueItem::toMediaItem),
                snapshot.currentIndex,
                snapshot.positionMs,
            )
            player.shuffleModeEnabled = false
            player.repeatMode = snapshot.playbackMode.toPlayerRepeatMode()
        }
        player.addListener(
            object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    scheduleSnapshotWrite(
                        player = player,
                        immediate = events.contains(Player.EVENT_TIMELINE_CHANGED) ||
                            events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                            events.contains(Player.EVENT_REPEAT_MODE_CHANGED),
                    )
                }
            }
        )
        restoreSnapshot(player, startupSnapshot)
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()
        serviceScope.launch {
            while (isActive) {
                delay(SNAPSHOT_INTERVAL_MS)
                if (player.isPlaying) scheduleSnapshotWrite(player)
            }
        }
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo,
    ): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        mediaSession?.player?.let { player ->
            if (player.shouldPersistCurrentSnapshot()) {
                persistSnapshotBlocking(player)
            }
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.run {
            if (player.shouldPersistCurrentSnapshot()) {
                persistSnapshotBlocking(player)
            }
            player.release()
            release()
        }
        serviceScope.cancel()
        mediaSession = null
        super.onDestroy()
    }

    private fun restoreSnapshot(
        player: Player,
        startupSnapshot: PlaybackSnapshot?,
    ) {
        snapshotRestorePending = true
        serviceScope.launch {
            // A controller command issued while the disk read was running wins over restoration.
            val snapshot = withContext(Dispatchers.IO) { snapshotStore.loadUnvalidated() }
            if (!player.isAwaitingSnapshotRestore(startupSnapshot)) {
                completeSnapshotRestore(player)
                return@launch
            }
            if (snapshot == null) {
                PlaybackModeMemory.set(PlaybackMode.ORDER)
                withContext(Dispatchers.IO) { miniSnapshotStore.clear() }
                player.clearMediaItems()
                completeSnapshotRestore(player)
                return@launch
            }
            PlaybackModeMemory.set(snapshot.playbackMode)
            val previewCurrentItem = player.currentPlaybackQueue()
                .getOrNull(player.currentMediaItemIndex)
            val restoredCurrentIndex = previewCurrentItem?.let { currentItem ->
                snapshot.queue.indexOfFirst { item -> item.isSameQueueSlot(currentItem) }
            }?.takeIf { it >= 0 } ?: snapshot.currentIndex
            val shouldPlay = player.playWhenReady
            val restorePositionMs = if (previewCurrentItem != null) {
                player.currentPosition
            } else {
                snapshot.positionMs
            }.coerceAtLeast(0L)
            val activeSnapshot = snapshot.copy(
                currentIndex = restoredCurrentIndex,
                positionMs = restorePositionMs,
            )
            player.setMediaItems(
                activeSnapshot.queue.map { it.toMediaItem() },
                activeSnapshot.currentIndex,
                restorePositionMs,
            )
            player.shuffleModeEnabled = false
            player.repeatMode = snapshot.playbackMode.toPlayerRepeatMode()
            player.prepare()
            if (shouldPlay) player.play() else player.pause()
            withContext(Dispatchers.IO) {
                miniSnapshotStore.save(activeSnapshot)
            }

            val validatedSnapshot = withContext(Dispatchers.IO) {
                snapshotStore.validate(activeSnapshot)
            }
            val reconciledSnapshot = reconcileValidatedPlaybackSnapshot(
                restoredSnapshot = activeSnapshot,
                validatedSnapshot = validatedSnapshot,
                currentQueue = player.currentPlaybackQueue(),
                currentIndex = player.currentMediaItemIndex,
                positionMs = player.currentPosition,
                playbackMode = player.currentPlaybackMode(),
            )
            if (reconciledSnapshot == null) {
                completeSnapshotRestore(player)
                return@launch
            }
            if (!hasSameQueueSlots(reconciledSnapshot.queue, activeSnapshot.queue)) {
                val keepPlaying = player.playWhenReady
                if (reconciledSnapshot.queue.isEmpty()) {
                    player.clearMediaItems()
                } else {
                    player.setMediaItems(
                        reconciledSnapshot.queue.map { it.toMediaItem() },
                        reconciledSnapshot.currentIndex,
                        reconciledSnapshot.positionMs,
                    )
                    player.shuffleModeEnabled = false
                    player.repeatMode = reconciledSnapshot.playbackMode.toPlayerRepeatMode()
                    player.prepare()
                    if (keepPlaying) player.play() else player.pause()
                }
            }
            completeSnapshotRestore(player)
        }
    }

    private fun completeSnapshotRestore(player: Player) {
        snapshotRestorePending = false
        snapshotRestoreSeed = null
        scheduleSnapshotWrite(player, immediate = true)
    }

    private fun scheduleSnapshotWrite(
        player: Player,
        immediate: Boolean = false,
    ) {
        if (snapshotRestorePending) return
        val snapshot = player.toSnapshot()
        val sequence = snapshotWriteSequence.incrementAndGet()
        if (immediate) {
            snapshotDebounceJob?.cancel()
            snapshotDebounceJob = null
            serviceScope.launch { persistSnapshot(snapshot, sequence) }
        } else {
            snapshotDebounceJob?.cancel()
            snapshotDebounceJob = serviceScope.launch {
                delay(SNAPSHOT_DEBOUNCE_MS)
                persistSnapshot(snapshot, sequence)
            }
        }
    }

    private suspend fun persistSnapshot(
        snapshot: PlaybackSnapshot,
        sequence: Long,
    ) = withContext(Dispatchers.IO) {
        snapshotWriteMutex.withLock {
            if (sequence <= lastPersistedSnapshotSequence.get()) return@withLock
            runCatching { snapshotStore.save(snapshot) }
            runCatching { miniSnapshotStore.save(snapshot) }
            lastPersistedSnapshotSequence.set(sequence)
        }
    }

    private fun persistSnapshotBlocking(player: Player) {
        val snapshot = player.toSnapshot()
        val sequence = snapshotWriteSequence.incrementAndGet()
        snapshotDebounceJob?.cancel()
        snapshotDebounceJob = null
        runBlocking(Dispatchers.IO) {
            persistSnapshot(snapshot, sequence)
        }
    }

    private fun Player.toSnapshot(): PlaybackSnapshot {
        val rawQueue = currentPlaybackQueue()
        val playbackMode = currentPlaybackMode()
        val queue = rawQueue.map { item -> item.copy(playbackMode = playbackMode) }
        return PlaybackSnapshot(
            queue = queue,
            currentIndex = currentMediaItemIndex.takeIf { queue.isNotEmpty() } ?: -1,
            positionMs = currentPosition.coerceAtLeast(0L),
            playbackMode = playbackMode,
        )
    }

    private fun Player.isAwaitingSnapshotRestore(
        startupSnapshot: PlaybackSnapshot?,
    ): Boolean = if (startupSnapshot == null) {
        mediaItemCount == 0
    } else {
        hasSameQueueSlots(currentPlaybackQueue(), startupSnapshot.queue)
    }

    private fun Player.shouldPersistCurrentSnapshot(): Boolean =
        !snapshotRestorePending || !isAwaitingSnapshotRestore(snapshotRestoreSeed)

    private companion object {
        const val SNAPSHOT_DEBOUNCE_MS = 350L
        const val SNAPSHOT_INTERVAL_MS = 5_000L
    }
}

internal fun reconcileValidatedPlaybackSnapshot(
    restoredSnapshot: PlaybackSnapshot,
    validatedSnapshot: PlaybackSnapshot?,
    currentQueue: List<PlaybackQueueItem>,
    currentIndex: Int,
    positionMs: Long,
    playbackMode: PlaybackMode,
): PlaybackSnapshot? {
    if (!hasSameQueueSlots(currentQueue, restoredSnapshot.queue)) return null
    val validatedQueue = validatedSnapshot?.queue.orEmpty().map { item ->
        item.copy(playbackMode = playbackMode)
    }
    if (validatedQueue.isEmpty()) {
        return PlaybackSnapshot(
            queue = emptyList(),
            currentIndex = -1,
            positionMs = 0L,
            playbackMode = playbackMode,
        )
    }
    val currentItem = currentQueue.getOrNull(currentIndex)
    val retainedCurrentIndex = currentItem?.let { item ->
        validatedQueue.indexOfFirst { candidate -> candidate.isSameQueueSlot(item) }
    } ?: -1
    return PlaybackSnapshot(
        queue = validatedQueue,
        currentIndex = retainedCurrentIndex.takeIf { it >= 0 } ?: 0,
        positionMs = if (retainedCurrentIndex >= 0) positionMs.coerceAtLeast(0L) else 0L,
        playbackMode = playbackMode,
    )
}

internal fun hasSameQueueSlots(
    first: List<PlaybackQueueItem>,
    second: List<PlaybackQueueItem>,
): Boolean = first.size == second.size && first.indices.all { index ->
    first[index].isSameQueueSlot(second[index])
}

private fun PlaybackQueueItem.isSameQueueSlot(other: PlaybackQueueItem): Boolean =
    mediaId == other.mediaId &&
        contentUri == other.contentUri &&
        sourceOrder == other.sourceOrder
