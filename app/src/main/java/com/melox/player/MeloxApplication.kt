package com.melox.player

import android.app.Application
import com.melox.player.data.playback.ListeningStatisticsStore
import com.melox.player.memory.FairMemoryManager
import com.melox.player.ui.component.library.trimArtworkMemoryCache
import com.melox.player.ui.component.playback.trimBlurredArtworkMemoryCache

class MeloxApplication : Application() {
    internal val listeningStatistics by lazy { ListeningStatisticsStore(this) }

    internal lateinit var fairMemoryManager: FairMemoryManager
        private set

    override fun onCreate() {
        super.onCreate()
        fairMemoryManager = FairMemoryManager(::trimImageCaches)
        fairMemoryManager.register(this)
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        trimImageCaches()
    }

    @Suppress("DEPRECATION")
    override fun onLowMemory() {
        super.onLowMemory()
        trimImageCaches()
    }

    private fun trimImageCaches() {
        trimArtworkMemoryCache()
        trimBlurredArtworkMemoryCache()
    }
}
