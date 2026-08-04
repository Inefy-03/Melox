package com.melox.player.model

import androidx.media3.common.Player

enum class PlaybackMode {
    ORDER,
    REPEAT_ONE,
    RANDOM,
}

/** Metadata required to render and persist one item in the playback queue. */
data class PlaybackQueueItem(
    val mediaId: String,
    val trackId: Long?,
    val contentUri: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long,
    val dateModifiedEpochSeconds: Long,
    val fileSizeBytes: Long,
    val sourceOrder: Double = 0.0,
    val playbackMode: PlaybackMode = PlaybackMode.ORDER,
)

/** Immutable projection of the service-owned Media3 player. */
data class PlaybackUiState(
    val queue: List<PlaybackQueueItem> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val playWhenReady: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val playbackMode: PlaybackMode = PlaybackMode.ORDER,
    val errorMessage: String? = null,
) {
    val currentItem: PlaybackQueueItem?
        get() = queue.getOrNull(currentIndex)

    val shuffleEnabled: Boolean
        get() = playbackMode == PlaybackMode.RANDOM

    val repeatMode: Int
        get() = if (playbackMode == PlaybackMode.REPEAT_ONE) {
            Player.REPEAT_MODE_ONE
        } else {
            Player.REPEAT_MODE_ALL
        }
}

/** Versioned data persisted by the playback service. */
data class PlaybackSnapshot(
    val queue: List<PlaybackQueueItem>,
    val currentIndex: Int,
    val positionMs: Long,
    val playbackMode: PlaybackMode,
)
