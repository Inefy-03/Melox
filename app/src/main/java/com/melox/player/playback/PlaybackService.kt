 package com.melox.player.playback

import android.app.PendingIntent
import android.content.Intent
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.AudioAttributes as PlatformAudioAttributes
import android.os.Handler
import android.os.Bundle
import android.os.Process
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.melox.player.MainActivity
import com.melox.player.MeloxApplication
import com.melox.player.R
import com.melox.player.data.playback.PlaybackSnapshotStore
import com.melox.player.data.playback.MiniPlaybackSnapshotStore
import com.melox.player.data.repository.SettingsRepository
import com.melox.player.model.MusicTrack
import com.melox.player.model.PlaybackMode
import com.melox.player.model.PlaybackQueueItem
import com.melox.player.model.AppSettings
import com.melox.player.model.PlaybackSnapshot
import com.melox.player.model.SleepTimerState
import com.melox.player.model.PLAYBACK_SPEED_VALUES
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

/** Owns background audio playback and exposes it to Android system media controls. */
internal const val ACTION_CLOSE_APPLICATION_COMMAND =
    "com.melox.player.action.CLOSE_APPLICATION_COMMAND"
internal const val ACTION_REMOVE_QUEUE_CONTENT_URIS = "com.melox.player.action.REMOVE_QUEUE_CONTENT_URIS"
internal const val EXTRA_REMOVED_CONTENT_URIS = "removed_content_uris"
internal const val ACTION_SET_PAUSE_FADE = "com.melox.player.action.SET_PAUSE_FADE"
internal const val ACTION_SET_PLAYBACK_INTENT = "com.melox.player.action.SET_PLAYBACK_INTENT"
internal const val ACTION_APPLY_STARTUP_PLAYBACK = "com.melox.player.action.APPLY_STARTUP_PLAYBACK"
internal const val EXTRA_ALLOW_STARTUP_AUTO_PLAY = "allow_startup_auto_play"
internal const val ACTION_SET_PLAYBACK_SPEED = "com.melox.player.action.SET_PLAYBACK_SPEED"
internal const val ACTION_SET_HIGH_PRECISION_OUTPUT = "com.melox.player.action.SET_HIGH_PRECISION_OUTPUT"
internal const val ACTION_START_SLEEP_TIMER = "com.melox.player.action.START_SLEEP_TIMER"
internal const val ACTION_CANCEL_SLEEP_TIMER = "com.melox.player.action.CANCEL_SLEEP_TIMER"
internal const val ACTION_SET_TIMER_EXTENSION = "com.melox.player.action.SET_TIMER_EXTENSION"
internal const val ACTION_ACKNOWLEDGE_TIMER_INTERRUPTION = "com.melox.player.action.ACKNOWLEDGE_TIMER_INTERRUPTION"
internal const val EXTRA_SLEEP_TIMER_STATE = "sleep_timer_state"
internal const val EXTRA_TIMER_SECONDS = "timer_seconds"
internal const val EXTRA_TIMER_EXTENSION = "timer_extension"
internal const val EXTRA_TIMER_INTERRUPTED = "timer_interrupted"
internal const val EXTRA_PAUSE_FADE = "pause_fade"
internal const val EXTRA_PLAYBACK_INTENT = "playback_intent"
internal const val EXTRA_PLAYBACK_SPEED = "playback_speed"
internal const val EXTRA_HIGH_PRECISION_OUTPUT = "high_precision_output"
internal const val EXTRA_FLOAT_OUTPUT_ACTIVE = "float_output_active"
internal const val EXTRA_AUDIO_FOCUS_PAUSE_REVISION = "audio_focus_pause_revision"
internal const val EXTRA_SNAPSHOT_RESTORE_PENDING = "snapshot_restore_pending"
internal const val EXTRA_CLOSE_AFTER_EXTENSION = "close_after_extension"
internal const val EXTRA_EXTENSION_EXPECTED_INDEX = "extension_expected_index"

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var snapshotStore: PlaybackSnapshotStore
    private lateinit var miniSnapshotStore: MiniPlaybackSnapshotStore
    private lateinit var settingsRepository: SettingsRepository
    private var snapshotDebounceJob: Job? = null
    private var snapshotRestorePending = false
    private var snapshotRestoreCancelledByQueueCommand = false
    private var snapshotRestoreModeChanged = false
    private var snapshotRestoreSeed: PlaybackSnapshot? = null
    private val snapshotWriteMutex = Mutex()
    private val snapshotWriteSequence = AtomicLong()
    private val lastSnapshotWriteSequence = AtomicLong()
    private var lastSnapshotWriteSucceeded = false
    private val queueProjection = PlaybackQueueProjection()
    private var hiddenQueueContentUris: Set<String> = emptySet()
    private val deferredRemovedContentUris = mutableSetOf<String>()
    private var artworkLoadJob: Job? = null
    private var requestedArtworkKey: PlaybackArtworkKey? = null
    private var isClosing = false
    private var startupPlaybackApplied = false
    private var autoPlayOnStart = false
    private var showNotificationPlaybackModeButton = true
    private var showNotificationCloseButton = true
    private val cyclePlaybackModeCommand = SessionCommand(ACTION_CYCLE_PLAYBACK_MODE, Bundle.EMPTY)
    private val closeApplicationCommand = SessionCommand(ACTION_CLOSE_APPLICATION_COMMAND, Bundle.EMPTY)
    private val pauseFadeCommand = SessionCommand(ACTION_SET_PAUSE_FADE, Bundle.EMPTY)
    private val playbackIntentCommand = SessionCommand(ACTION_SET_PLAYBACK_INTENT, Bundle.EMPTY)
    private val playbackSpeedCommand = SessionCommand(ACTION_SET_PLAYBACK_SPEED, Bundle.EMPTY)
    private val highPrecisionOutputCommand = SessionCommand(ACTION_SET_HIGH_PRECISION_OUTPUT, Bundle.EMPTY)
    private var playbackVolumeFade: PlaybackVolumeFade? = null
    private var activePlayer: ExoPlayer? = null
    private var platformFocusRequest: AudioFocusRequest? = null
    private var audioFocusPauseRevision = 0L
    private val audioManager by lazy { getSystemService(AudioManager::class.java) }
    private val playbackAudioFocus by lazy {
        PlaybackAudioFocus(
            requestFocus = { onChange ->
                val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(PlatformAudioAttributes.Builder()
                        .setUsage(PlatformAudioAttributes.USAGE_MEDIA)
                        .setContentType(PlatformAudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAcceptsDelayedFocusGain(true)
                    .setWillPauseWhenDucked(true)
                    .setOnAudioFocusChangeListener({ onChange(it) }, Handler(mainLooper))
                    .build()
                platformFocusRequest = request
                audioManager.requestAudioFocus(request)
            },
            abandonFocus = {
                platformFocusRequest?.let(audioManager::abandonAudioFocusRequest)
                platformFocusRequest = null
            },
            isPlaybackRequested = { activePlayer?.playWhenReady == true },
            pause = {
                audioFocusPauseRevision++
                playbackVolumeFade?.pauseImmediately() ?: activePlayer?.pause()
                updateSessionExtras()
            },
            resume = {
                activePlayer?.let { player ->
                    if (!snapshotRestorePending && player.playbackState == Player.STATE_IDLE) player.prepare()
                    player.play()
                }
            },
            setVolumeMultiplier = { playbackVolumeFade?.setFocusVolumeMultiplier(it) },
        )
    }
    private var highPrecisionOutput = false
    private var floatOutputActive = false
    private var audioEffectSessionId = C.AUDIO_SESSION_ID_UNSET
    private var outputGeneration = 0
    private var pauseFadeEnabled = false
    private var deferredPlaybackSpeed: Float? = null
    private var deferredHighPrecisionOutput: Boolean? = null
    private val sleepTimer = PlaybackSleepTimer(
        scope = serviceScope,
        player = { activePlayer },
        publish = { updateSessionExtras() },
        close = { extended, expectedIndex ->
            if (extended) closeAfterExtendedTimer(expectedIndex ?: -1) else closeApplication()
        },
    )
    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (player !== activePlayer) return
            if (events.contains(Player.EVENT_AUDIO_SESSION_ID)) {
                updateAudioEffectSession(player.audioSessionId)
            }
            if (player.playbackState == Player.STATE_ENDED ||
                (!snapshotRestorePending && player.mediaItemCount == 0)
            ) playbackAudioFocus.onUserPause()
            sleepTimer.onPlayerEvents(player)
            if (events.contains(Player.EVENT_REPEAT_MODE_CHANGED)) {
                refreshMediaButtonPreferences(player.currentPlaybackMode())
            }
            if (events.contains(Player.EVENT_TIMELINE_CHANGED) ||
                events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)
            ) {
                if (player.mediaItemCount == 0) setFloatOutputActive(false)
                scheduleArtworkUpdate(player)
            }
            scheduleSnapshotWrite(
                player = player,
                immediate = events.contains(Player.EVENT_TIMELINE_CHANGED) ||
                    events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                    events.contains(Player.EVENT_REPEAT_MODE_CHANGED),
            )
        }
    }
    private val mediaSessionCallback = object : MediaSession.Callback {
        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommand: Int,
        ): Int {
            if (playerCommand == Player.COMMAND_PLAY_PAUSE || playerCommand == Player.COMMAND_STOP) {
                playbackAudioFocus.onUserPause()
            }
            return SessionResult.RESULT_SUCCESS
        }

        override fun onPlayerInteractionFinished(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommands: Player.Commands,
        ) {
            if (!isClosing && session.player === activePlayer) {
                if (playerCommands.contains(Player.COMMAND_PLAY_PAUSE) && session.player.playWhenReady) {
                    playbackAudioFocus.onPlaybackIntentChanged(true)
                }
                if (snapshotRestorePending && (
                        playerCommands.contains(Player.COMMAND_SET_REPEAT_MODE) ||
                            playerCommands.contains(Player.COMMAND_SET_SHUFFLE_MODE)
                    )
                ) {
                    snapshotRestoreModeChanged = true
                }
                if (snapshotRestorePending && playerCommands.contains(Player.COMMAND_CHANGE_MEDIA_ITEMS)) {
                    snapshotRestoreCancelledByQueueCommand = true
                }
                session.player.resumeAfterSystemTrackSkip(playerCommands)
            }
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): com.google.common.util.concurrent.ListenableFuture<List<MediaItem>> =
            resolveVisibleMediaItems(mediaItems)

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): com.google.common.util.concurrent.ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val result = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
            val job = serviceScope.launch {
                try {
                    val visible = visibleMediaItems(mediaItems)
                    val retainedUris = visible.mapTo(HashSet()) { it.toPlaybackQueueItem().contentUri }
                    val sourceIndices = mediaItems.indices.filter {
                        mediaItems[it].toPlaybackQueueItem().contentUri in retainedUris
                    }
                    val selection = visibleQueueStart(sourceIndices, startIndex, startPositionMs)
                    result.set(MediaSession.MediaItemsWithStartPosition(visible, selection.first, selection.second))
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (exception: Exception) {
                    result.setException(exception)
                }
            }
            job.invokeOnCompletion { if (!result.isDone) result.cancel(false) }
            return result
        }

        override fun onConnectAsync(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ) = Futures.immediateFuture(
            MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
                .setAvailableSessionCommands(
                    (if (controller.isTrusted) {
                        MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                    } else {
                        MediaSession.ConnectionResult.DEFAULT_UNTRUSTED_SESSION_COMMANDS
                    }).buildUpon().apply {
                        if (controller.isTrusted) {
                            add(cyclePlaybackModeCommand)
                            add(closeApplicationCommand)
                            add(SessionCommand(ACTION_REMOVE_QUEUE_CONTENT_URIS, Bundle.EMPTY))
                            add(pauseFadeCommand)
                            add(playbackIntentCommand)
                            add(SessionCommand(ACTION_APPLY_STARTUP_PLAYBACK, Bundle.EMPTY))
                            add(playbackSpeedCommand)
                            add(highPrecisionOutputCommand)
                            add(SessionCommand(ACTION_START_SLEEP_TIMER, Bundle.EMPTY))
                            add(SessionCommand(ACTION_CANCEL_SLEEP_TIMER, Bundle.EMPTY))
                            add(SessionCommand(ACTION_SET_TIMER_EXTENSION, Bundle.EMPTY))
                            add(SessionCommand(ACTION_ACKNOWLEDGE_TIMER_INTERRUPTION, Bundle.EMPTY))
                        }
                    }.build(),
                )
                .build(),
        )

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ) = when (customCommand.customAction) {
            ACTION_APPLY_STARTUP_PLAYBACK -> {
                if (!startupPlaybackApplied && !isClosing) {
                    startupPlaybackApplied = true
                    val player = session.player
                    if (autoPlayOnStart && args.getBoolean(EXTRA_ALLOW_STARTUP_AUTO_PLAY) &&
                        (snapshotRestorePending || player.mediaItemCount > 0)
                    ) {
                        // Restoration carries this intent forward; later pause/queue commands win.
                        if (!snapshotRestorePending && player.playbackState == Player.STATE_IDLE) {
                            player.prepare()
                        }
                        player.play()
                    }
                }
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            ACTION_CYCLE_PLAYBACK_MODE -> {
                cyclePlaybackMode(session.player)
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            ACTION_CLOSE_APPLICATION_COMMAND -> {
                if (args.getBoolean(EXTRA_CLOSE_AFTER_EXTENSION)) {
                    closeAfterExtendedTimer(args.getInt(EXTRA_EXTENSION_EXPECTED_INDEX, -1))
                } else {
                    closeApplication()
                }
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            ACTION_REMOVE_QUEUE_CONTENT_URIS -> {
                val uris = args.getStringArrayList(EXTRA_REMOVED_CONTENT_URIS).orEmpty().toSet()
                if (snapshotRestorePending) deferredRemovedContentUris += uris
                else session.player.removePlaybackContentUris(uris)
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            ACTION_SET_PAUSE_FADE -> {
                pauseFadeEnabled = args.getBoolean(EXTRA_PAUSE_FADE)
                playbackVolumeFade?.setEnabled(pauseFadeEnabled)
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            ACTION_SET_PLAYBACK_INTENT -> {
                if (!args.getBoolean(EXTRA_PLAYBACK_INTENT)) playbackAudioFocus.onUserPause()
                playbackVolumeFade?.requestPlaybackIntent(args.getBoolean(EXTRA_PLAYBACK_INTENT))
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            ACTION_SET_PLAYBACK_SPEED -> {
                val speed = args.getFloat(EXTRA_PLAYBACK_SPEED, 1f)
                if (speed in PLAYBACK_SPEED_VALUES) {
                    if (snapshotRestorePending) deferredPlaybackSpeed = speed
                    else activePlayer?.setPlaybackSpeed(speed)
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                } else {
                    Futures.immediateFuture(SessionResult(SessionError.ERROR_BAD_VALUE))
                }
            }

            ACTION_SET_HIGH_PRECISION_OUTPUT -> {
                val enabled = args.getBoolean(EXTRA_HIGH_PRECISION_OUTPUT, false)
                if (snapshotRestorePending) deferredHighPrecisionOutput = enabled
                else setHighPrecisionOutput(enabled)
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            ACTION_START_SLEEP_TIMER -> {
                val seconds = args.getInt(EXTRA_TIMER_SECONDS)
                if (seconds in 1..86_399) {
                    sleepTimer.autoExtend = args.getBoolean(EXTRA_TIMER_EXTENSION)
                    sleepTimer.start(seconds)
                    timerCommandResult()
                } else Futures.immediateFuture(SessionResult(SessionError.ERROR_BAD_VALUE))
            }

            ACTION_CANCEL_SLEEP_TIMER -> {
                sleepTimer.cancel(interrupted = args.getBoolean(EXTRA_TIMER_INTERRUPTED))
                timerCommandResult()
            }

            ACTION_SET_TIMER_EXTENSION -> {
                sleepTimer.autoExtend = args.getBoolean(EXTRA_TIMER_EXTENSION)
                timerCommandResult()
            }

            ACTION_ACKNOWLEDGE_TIMER_INTERRUPTION -> {
                sleepTimer.acknowledgeInterruption()
                timerCommandResult()
            }

            else -> Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
        }
    }

    override fun onCreate() {
        super.onCreate()
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .build()
            .apply { setSmallIcon(R.drawable.ic_notification_melox) }
        setMediaNotificationProvider(notificationProvider)
        snapshotStore = PlaybackSnapshotStore(this)
        miniSnapshotStore = MiniPlaybackSnapshotStore(this)
        settingsRepository = SettingsRepository(this)
        val startupSettings = runBlocking(Dispatchers.IO) { settingsRepository.loadSettings() }
        highPrecisionOutput = startupSettings.highPrecisionOutput
        pauseFadeEnabled = startupSettings.playbackPauseFade
        autoPlayOnStart = startupSettings.autoPlayOnStart
        showNotificationPlaybackModeButton = startupSettings.showNotificationPlaybackModeButton
        showNotificationCloseButton = startupSettings.showNotificationCloseButton
        sleepTimer.autoExtend = startupSettings.autoExtendSleepTimer
        val startupSnapshot = miniSnapshotStore.load()
            ?.withoutContentUris(startupSettings.hiddenTrackUris)
            ?.takeIf { it.queue.isNotEmpty() }
        hiddenQueueContentUris = startupSettings.hiddenTrackUris
        snapshotRestoreSeed = startupSnapshot
        snapshotRestorePending = true
        PlaybackModeMemory.set(startupSnapshot?.playbackMode ?: PlaybackMode.ORDER)
        val player = createPlayer(highPrecisionOutput)
        activePlayer = player
        player.setPlaybackSpeed(startupSettings.playbackSpeed)
        playbackVolumeFade = PlaybackVolumeFade(player, serviceScope).apply {
            setEnabled(pauseFadeEnabled)
        }
        playbackAudioFocus.updateSettings(
            startupSettings.audioFocusEnabled, startupSettings.transientAudioFocusBehavior,
        )
        serviceScope.launch {
            settingsRepository.settings
                .map { it.audioFocusEnabled to it.transientAudioFocusBehavior }
                .distinctUntilChanged()
                .collect { (enabled, behavior) -> playbackAudioFocus.updateSettings(enabled, behavior) }
        }
        startupSnapshot?.let { snapshot ->
            player.setMediaItems(
                snapshot.queue.map(PlaybackQueueItem::toMediaItem),
                snapshot.currentIndex,
                snapshot.positionMs,
            )
            player.shuffleModeEnabled = false
            player.repeatMode = snapshot.playbackMode.toPlayerRepeatMode()
        }
        restoreSnapshot(player, startupSnapshot)
        serviceScope.launch {
            settingsRepository.settings
                .playbackExclusionUpdates()
                .collectLatest { uris ->
                    hiddenQueueContentUris = uris
                    if (!snapshotRestorePending) {
                        activePlayer?.removePlaybackContentUris(hiddenQueueContentUris)
                    }
                }
        }
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
            .setSessionExtras(sessionExtras())
            .setCallback(mediaSessionCallback)
            .setMediaButtonPreferences(mediaButtonPreferences(player.currentPlaybackMode()))
            .build()
        serviceScope.launch {
            settingsRepository.settings
                .map { it.showNotificationPlaybackModeButton to it.showNotificationCloseButton }
                .distinctUntilChanged()
                .collect { (showModeButton, showCloseButton) ->
                    showNotificationPlaybackModeButton = showModeButton
                    showNotificationCloseButton = showCloseButton
                    refreshMediaButtonPreferences(player.currentPlaybackMode())
                }
        }
        scheduleArtworkUpdate(player)
        (application as MeloxApplication).fairMemoryManager.savePlayback = ::saveMemoryCheckpoint
        serviceScope.launch {
            while (isActive) {
                delay(SNAPSHOT_INTERVAL_MS)
                activePlayer?.takeIf(Player::isPlaying)?.let(::scheduleSnapshotWrite)
            }
        }
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo,
    ): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val terminate = runBlocking(Dispatchers.IO) {
            settingsRepository.loadSettings().terminateOnTaskRemoved
        }
        mediaSession?.player?.let { player ->
            if (player.shouldPersistCurrentSnapshot()) {
                persistSnapshotBlocking(player)
            }
        }
        if (terminate) closeApplication() else super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        updateAudioEffectSession(C.AUDIO_SESSION_ID_UNSET)
        playbackAudioFocus.release()
        playbackVolumeFade?.release()
        playbackVolumeFade = null
        mediaSession?.run {
            if (player.shouldPersistCurrentSnapshot()) {
                persistSnapshotBlocking(player)
            }
            player.release()
            release()
        }
        activePlayer = null
        serviceScope.cancel()
        mediaSession = null
        (application as MeloxApplication).fairMemoryManager.savePlayback = null
        super.onDestroy()
    }

    private fun resolveVisibleMediaItems(
        mediaItems: List<MediaItem>,
    ): com.google.common.util.concurrent.ListenableFuture<List<MediaItem>> {
        val result = SettableFuture.create<List<MediaItem>>()
        val job = serviceScope.launch {
            try {
                result.set(visibleMediaItems(mediaItems))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                result.setException(exception)
            }
        }
        job.invokeOnCompletion { if (!result.isDone) result.cancel(false) }
        return result
    }

    private suspend fun visibleMediaItems(mediaItems: List<MediaItem>): List<MediaItem> {
        if (mediaItems.any { it.localConfiguration == null }) throw UnsupportedOperationException()
        val settings = settingsRepository.loadSettings()
        if (settings.hiddenTrackUris.isEmpty()) return mediaItems
        return mediaItems.filterNot { it.toPlaybackQueueItem().contentUri in settings.hiddenTrackUris }
    }

    private fun createPlayer(highPrecision: Boolean): ExoPlayer {
        val generation = ++outputGeneration
        val renderersFactory = PrecisionRenderersFactory(this, highPrecision) { active ->
            serviceScope.launch {
                if (generation == outputGeneration) setFloatOutputActive(active)
            }
        }
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        return ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this, PlaybackExtractorsFactory()))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                false,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                setWakeMode(C.WAKE_MODE_LOCAL)
                addListener(playerListener)
                val owner = this
                addListener(object : Player.Listener {
                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                        if (owner === activePlayer) {
                            if (!playWhenReady) sleepTimer.onPlayerEvents(owner)
                            playbackVolumeFade?.onPlayWhenReadyChanged(playWhenReady, reason)
                            playbackAudioFocus.onPlaybackIntentChanged(playWhenReady)
                        }
                    }

                    override fun onMediaItemTransition(
                        mediaItem: androidx.media3.common.MediaItem?,
                        reason: Int,
                    ) {
                        if (owner === activePlayer) sleepTimer.onMediaItemTransition(reason)
                    }
                })
            }
    }

    private fun setHighPrecisionOutput(enabled: Boolean) {
        val oldPlayer = activePlayer ?: return
        if (enabled == highPrecisionOutput) return
        val session = mediaSession ?: return
        val nextPlayer = createPlayer(enabled)
        val items = (0 until oldPlayer.mediaItemCount).map(oldPlayer::getMediaItemAt)
        val currentIndex = oldPlayer.currentMediaItemIndex
        if (currentIndex in items.indices) {
            nextPlayer.setMediaItems(items, currentIndex, oldPlayer.currentPosition.coerceAtLeast(0L))
        } else if (items.isNotEmpty()) {
            nextPlayer.setMediaItems(items)
        }
        nextPlayer.shuffleModeEnabled = oldPlayer.shuffleModeEnabled
        nextPlayer.repeatMode = oldPlayer.repeatMode
        nextPlayer.setPlaybackSpeed(oldPlayer.playbackParameters.speed)
        val oldFade = playbackVolumeFade
        val keepPlaying = oldPlayer.playWhenReady && oldFade?.hasPendingPause != true
        val shouldPrepare = oldPlayer.playbackState != Player.STATE_IDLE && items.isNotEmpty()
        nextPlayer.playWhenReady = keepPlaying
        updateAudioEffectSession(C.AUDIO_SESSION_ID_UNSET)
        session.setPlayer(nextPlayer)
        activePlayer = nextPlayer
        highPrecisionOutput = enabled
        setFloatOutputActive(false)
        oldPlayer.pause()
        oldFade?.release()
        playbackVolumeFade = PlaybackVolumeFade(nextPlayer, serviceScope).apply {
            setEnabled(pauseFadeEnabled)
            setFocusVolumeMultiplier(playbackAudioFocus.volumeMultiplier)
        }
        oldPlayer.removeListener(playerListener)
        oldPlayer.release()
        artworkLoadJob?.cancel()
        artworkLoadJob = null
        requestedArtworkKey = null
        if (shouldPrepare) {
            nextPlayer.prepare()
        }
        scheduleArtworkUpdate(nextPlayer)
        scheduleSnapshotWrite(nextPlayer, immediate = true)
    }

    private fun updateAudioEffectSession(sessionId: Int) {
        if (audioEffectSessionId == sessionId) return
        fun notifyEffect(action: String, id: Int) {
            if (id <= 0) return
            try {
                sendBroadcast(Intent(action)
                    .putExtra(AudioEffect.EXTRA_AUDIO_SESSION, id)
                    .putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                    .putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC))
            } catch (_: SecurityException) {
                // Some vendor effect receivers are restricted to system applications.
            }
        }
        notifyEffect(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION, audioEffectSessionId)
        audioEffectSessionId = sessionId
        notifyEffect(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION, sessionId)
    }

    private fun setFloatOutputActive(active: Boolean) {
        if (floatOutputActive == active) return
        floatOutputActive = active
        updateSessionExtras()
    }

    private fun sessionExtras(): Bundle = Bundle().apply {
        putBoolean(EXTRA_FLOAT_OUTPUT_ACTIVE, floatOutputActive)
        putLong(EXTRA_AUDIO_FOCUS_PAUSE_REVISION, audioFocusPauseRevision)
        putBoolean(EXTRA_SNAPSHOT_RESTORE_PENDING, snapshotRestorePending)
        putBundle(EXTRA_SLEEP_TIMER_STATE, sleepTimer.state.toBundle())
    }

    private fun timerCommandResult() = Futures.immediateFuture(
        SessionResult(SessionResult.RESULT_SUCCESS, Bundle().apply {
            putBundle(EXTRA_SLEEP_TIMER_STATE, sleepTimer.state.toBundle())
        }),
    )

    private fun updateSessionExtras() {
        mediaSession?.setSessionExtras(sessionExtras())
    }

    private fun restoreSnapshot(
        player: Player,
        startupSnapshot: PlaybackSnapshot?,
    ) {
        snapshotRestorePending = true
        serviceScope.launch {
            // A controller command issued while the disk read was running wins over restoration.
            val savedSnapshot = withContext(Dispatchers.IO) { snapshotStore.loadUnvalidated() }
            val settings = settingsRepository.loadSettings()
            val snapshot = savedSnapshot?.withoutContentUris(
                settings.hiddenTrackUris + hiddenQueueContentUris + deferredRemovedContentUris,
            )
            if (isClosing) return@launch
            if (snapshotRestoreCancelledByQueueCommand || !player.isAwaitingSnapshotRestore(startupSnapshot)) {
                completeSnapshotRestore(player)
                return@launch
            }
            val currentMode = player.currentPlaybackMode()
            val requestedMode = currentMode.takeIf {
                snapshotRestoreModeChanged || it != (startupSnapshot?.playbackMode ?: PlaybackMode.ORDER)
            }
            if (snapshot == null || snapshot.queue.isEmpty()) {
                PlaybackModeMemory.set(requestedMode ?: snapshot?.playbackMode ?: PlaybackMode.ORDER)
                withContext(Dispatchers.IO) { miniSnapshotStore.clear() }
                player.pause()
                player.clearMediaItems()
                completeSnapshotRestore(player)
                return@launch
            }
            val previewCurrentItem = player.currentPlaybackQueue()
                .getOrNull(player.currentMediaItemIndex)
            val previewIndex = previewCurrentItem?.let { currentItem ->
                snapshot.queue.indexOfFirst { item -> item.isSameQueueSlot(currentItem) }
            }?.takeIf { it >= 0 }
            val restoredCurrentIndex = previewIndex ?: snapshot.currentIndex
            val shouldPlay = player.playWhenReady
            val restorePositionMs = if (previewIndex != null) {
                player.currentPosition
            } else {
                snapshot.positionMs
            }.coerceAtLeast(0L)
            val activeSnapshot = snapshot.copy(
                currentIndex = restoredCurrentIndex,
                positionMs = restorePositionMs,
            ).withRestoredPlaybackMode(requestedMode)
            PlaybackModeMemory.set(activeSnapshot.playbackMode)
            player.setMediaItems(
                activeSnapshot.queue.map { it.toMediaItem() },
                activeSnapshot.currentIndex,
                restorePositionMs,
            )
            player.shuffleModeEnabled = false
            player.repeatMode = activeSnapshot.playbackMode.toPlayerRepeatMode()
            player.prepare()
            if (shouldPlay) player.play() else player.pause()
            withContext(Dispatchers.IO) {
                miniSnapshotStore.save(activeSnapshot)
            }
            if (isClosing) return@launch

            val validatedSnapshot = withContext(Dispatchers.IO) {
                snapshotStore.validate(activeSnapshot)
            }
            if (isClosing) return@launch
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
        snapshotRestoreModeChanged = false
        snapshotRestoreSeed = null
        player.removePlaybackContentUris(hiddenQueueContentUris + deferredRemovedContentUris)
        deferredRemovedContentUris.clear()
        scheduleSnapshotWrite(player, immediate = true)
        deferredHighPrecisionOutput?.let { enabled ->
            deferredHighPrecisionOutput = null
            setHighPrecisionOutput(enabled)
        }
        deferredPlaybackSpeed?.let { speed ->
            deferredPlaybackSpeed = null
            activePlayer?.setPlaybackSpeed(speed)
        }
        updateSessionExtras()
    }

    private fun scheduleSnapshotWrite(
        player: Player,
        immediate: Boolean = false,
    ) {
        if (isClosing || snapshotRestorePending) return
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

    private fun scheduleArtworkUpdate(player: Player) {
        if (isClosing) return
        val currentIndex = player.currentMediaItemIndex
        val currentItem = currentIndex
            .takeIf { it in 0 until player.mediaItemCount }
            ?.let(player::getMediaItemAt)
        if (currentItem == null) {
            artworkLoadJob?.cancel()
            artworkLoadJob = null
            requestedArtworkKey = null
            return
        }
        val queueItem = currentItem.toPlaybackQueueItem()
        val artworkKey = queueItem.toPlaybackArtworkKey()
        if (requestedArtworkKey != artworkKey) {
            artworkLoadJob?.cancel()
            artworkLoadJob = null
            requestedArtworkKey = null
        }
        if (player.hasArtwork() || currentItem.mediaMetadata.hasArtwork()) return
        if (requestedArtworkKey == artworkKey) return

        requestedArtworkKey = artworkKey
        artworkLoadJob = serviceScope.launch {
            val artworkData = withContext(Dispatchers.IO) {
                loadPlaybackArtworkData(queueItem.contentUri)
            } ?: return@launch
            val activeIndex = player.currentMediaItemIndex
            val activeItem = activeIndex
                .takeIf { it in 0 until player.mediaItemCount }
                ?.let(player::getMediaItemAt)
                ?: return@launch
            if (
                activeItem.toPlaybackQueueItem().toPlaybackArtworkKey() != artworkKey ||
                player.hasArtwork() ||
                activeItem.mediaMetadata.hasArtwork()
            ) {
                return@launch
            }
            player.replaceMediaItem(activeIndex, activeItem.withArtworkData(artworkData))
        }
    }

    private fun Player.hasArtwork(): Boolean = mediaMetadata.hasArtwork()

    private fun androidx.media3.common.MediaMetadata.hasArtwork(): Boolean =
        artworkData != null || artworkUri != null

    private suspend fun persistSnapshot(
        snapshot: PlaybackSnapshot,
        sequence: Long,
    ) = withContext(Dispatchers.IO) {
        snapshotWriteMutex.withLock {
            if (sequence <= lastSnapshotWriteSequence.get()) {
                return@withLock lastSnapshotWriteSucceeded
            }
            val fullSaved = runCatching { snapshotStore.save(snapshot) }.isSuccess
            val miniSaved = runCatching { miniSnapshotStore.save(snapshot) }.isSuccess
            (fullSaved && miniSaved).also { saved ->
                // Even a partial write must not let an older queued snapshot replace it.
                lastSnapshotWriteSequence.set(sequence)
                lastSnapshotWriteSucceeded = saved
            }
        }
    }

    private suspend fun saveMemoryCheckpoint(): Boolean = withContext(Dispatchers.Main.immediate) {
        // Never replace the full disk queue with the bounded startup preview, or
        // race the restore path's mini-snapshot write. The receiver owns the timeout.
        while (snapshotRestorePending && !isClosing && mediaSession != null) delay(25L)
        val player = mediaSession?.player ?: return@withContext false
        if (isClosing) return@withContext false
        val snapshot = player.toSnapshot()
        val sequence = snapshotWriteSequence.incrementAndGet()
        snapshotDebounceJob?.cancel()
        snapshotDebounceJob = null
        persistSnapshot(snapshot, sequence)
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
        val playbackMode = currentPlaybackMode()
        val queue = queueProjection.queue(this, playbackMode)
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

    private fun cyclePlaybackMode(player: Player) {
        if (isClosing) return
        if (snapshotRestorePending) snapshotRestoreModeChanged = true
        val targetMode = nextPlaybackMode(player.currentPlaybackMode())
        val reordered = reorderQueueForPlaybackMode(
            queue = player.currentPlaybackQueue(),
            currentIndex = player.currentMediaItemIndex,
            targetMode = targetMode,
        )
        player.applyPlaybackMode(targetMode, reordered)
        refreshMediaButtonPreferences(targetMode)
    }

    private fun closeApplication() {
        if (isClosing) return
        isClosing = true
        Process.killProcess(Process.myPid())
    }

    private fun closeAfterExtendedTimer(expectedIndex: Int) {
        if (isClosing) return
        isClosing = true
        mediaSession?.player?.let { player ->
            playbackVolumeFade?.setEnabled(false)
            player.pause()
            val currentIndex = player.currentMediaItemIndex
            if (currentIndex in 0 until player.mediaItemCount) {
                val targetIndex = nextItemIndexAfterExtendedTimer(
                    currentIndex = currentIndex,
                    expectedIndex = expectedIndex,
                    itemCount = player.mediaItemCount,
                )
                player.seekToDefaultPosition(targetIndex)
            }
            if (player.shouldPersistCurrentSnapshot()) persistSnapshotBlocking(player)
        }
        Process.killProcess(Process.myPid())
    }

    private fun refreshMediaButtonPreferences(mode: PlaybackMode) {
        mediaSession?.setMediaButtonPreferences(mediaButtonPreferences(mode))
    }

    private fun mediaButtonPreferences(mode: PlaybackMode): List<CommandButton> = buildList {
        if (showNotificationPlaybackModeButton) {
            add(
                CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                    .setSessionCommand(cyclePlaybackModeCommand)
                    .setCustomIconResId(mode.notificationIconResId())
                    .setDisplayName(getString(mode.notificationLabelResId()))
                    .setSlots(CommandButton.SLOT_BACK_SECONDARY, CommandButton.SLOT_OVERFLOW)
                    .build(),
            )
        }
        if (showNotificationCloseButton) {
            add(
                CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                    .setSessionCommand(closeApplicationCommand)
                    .setCustomIconResId(R.drawable.ic_notification_close)
                    .setDisplayName(getString(R.string.notification_close))
                    .setSlots(CommandButton.SLOT_FORWARD_SECONDARY, CommandButton.SLOT_OVERFLOW)
                    .build(),
            )
        }
    }

    private companion object {
        const val ACTION_CYCLE_PLAYBACK_MODE =
            "com.melox.player.action.CYCLE_PLAYBACK_MODE"
        const val SNAPSHOT_DEBOUNCE_MS = 350L
        const val SNAPSHOT_INTERVAL_MS = 5_000L
    }
}

internal fun nextItemIndexAfterExtendedTimer(
    currentIndex: Int,
    expectedIndex: Int,
    itemCount: Int,
): Int = if (itemCount > 0 && currentIndex == expectedIndex) {
    (currentIndex + 1) % itemCount
} else {
    currentIndex
}

private fun PlaybackMode.notificationIconResId(): Int = when (this) {
    PlaybackMode.ORDER -> R.drawable.ic_notification_list_loop
    PlaybackMode.REPEAT_ONE -> R.drawable.ic_notification_repeat_one
    PlaybackMode.RANDOM -> R.drawable.ic_notification_shuffle
}

private fun PlaybackMode.notificationLabelResId(): Int = when (this) {
    PlaybackMode.ORDER -> R.string.notification_playback_mode_list_loop
    PlaybackMode.REPEAT_ONE -> R.string.notification_playback_mode_repeat_one
    PlaybackMode.RANDOM -> R.string.notification_playback_mode_shuffle
}

internal fun PlaybackSnapshot.withRestoredPlaybackMode(requestedMode: PlaybackMode?): PlaybackSnapshot {
    if (requestedMode == null || requestedMode == playbackMode) return this
    val reordered = reorderQueueForPlaybackMode(queue, currentIndex, requestedMode)
    return copy(
        queue = reordered.queue,
        currentIndex = reordered.currentIndex,
        playbackMode = requestedMode,
    )
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

internal fun SleepTimerState.toBundle(): Bundle = Bundle().apply {
    putBoolean("active", active)
    putInt("remaining_seconds", remainingSeconds)
    putInt("extension_seconds", extensionSeconds)
    putBoolean("extending", extending)
    putInt("interruption_notice", interruptionNotice)
}

internal fun Bundle.toSleepTimerState(): SleepTimerState = SleepTimerState(
    active = getBoolean("active"),
    remainingSeconds = getInt("remaining_seconds"),
    extensionSeconds = getInt("extension_seconds"),
    extending = getBoolean("extending"),
    interruptionNotice = getInt("interruption_notice"),
)

internal fun Flow<AppSettings>.playbackExclusionUpdates(): Flow<Set<String>> =
    map { it.hiddenTrackUris }.distinctUntilChanged()

internal fun PlaybackSnapshot.withoutContentUris(contentUris: Set<String>): PlaybackSnapshot {
    val retainedIndices = queue.indices.filter { queue[it].contentUri !in contentUris }
    if (retainedIndices.size == queue.size) return this
    if (retainedIndices.isEmpty()) return copy(queue = emptyList(), currentIndex = -1, positionMs = 0L)
    val nextSourceIndex = retainedIndices.firstOrNull { it >= currentIndex } ?: retainedIndices.first()
    return copy(
        queue = retainedIndices.map(queue::get),
        currentIndex = retainedIndices.indexOf(nextSourceIndex),
        positionMs = positionMs.takeIf { nextSourceIndex == currentIndex } ?: 0L,
    )
}

internal fun visibleQueueStart(
    retainedIndices: List<Int>,
    startIndex: Int,
    startPositionMs: Long,
): Pair<Int, Long> {
    if (retainedIndices.isEmpty() || startIndex == C.INDEX_UNSET) return C.INDEX_UNSET to C.TIME_UNSET
    val sourceIndex = retainedIndices.firstOrNull { it >= startIndex } ?: retainedIndices.first()
    return retainedIndices.indexOf(sourceIndex) to
        (startPositionMs.takeIf { sourceIndex == startIndex } ?: C.TIME_UNSET)
}
