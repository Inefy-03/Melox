package com.melox.player

import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.melox.player.model.AppSettings
import com.melox.player.model.MusicTrack
import com.melox.player.model.ScanStatus
import com.melox.player.ui.screen.home.HomeRecommendations
import com.melox.player.ui.screen.home.HomeScreen
import com.melox.player.ui.theme.MeloxTheme
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior

@RunWith(AndroidJUnit4::class)
class HomeStartupLayoutTest {
    @Test
    fun playlistHeaderStaysAtTheSameOffsetFromScanningThroughPreparedCards() {
        val stage = mutableStateOf(0)
        lateinit var listState: LazyListState
        val tracks = listOf(track())
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContentView(ComposeView(activity).apply {
                    setContent {
                        MeloxTheme(AppSettings(blurEnabled = false)) {
                            listState = rememberLazyListState()
                            Box(Modifier.size(width = 360.dp, height = 500.dp)) {
                                HomeScreen(
                                    tracks = if (stage.value == 0) emptyList() else tracks,
                                    recommendations = if (stage.value < 2) null else HomeRecommendations(
                                        tracks = tracks,
                                        artworkTrackIds = emptySet(),
                                    ),
                                    playlists = emptyList(),
                                    playlistsLoaded = false,
                                    scanStatus = if (stage.value == 0) ScanStatus.Scanning else ScanStatus.Success(1),
                                    blurEnabled = false,
                                    onOpenPlaylists = {},
                                    onOpenScanSettings = {},
                                    onCreatePlaylist = {},
                                    onPlaylistClick = {},
                                    onRecommendationClick = { _, _ -> },
                                    onRecommendationGestureActiveChanged = {},
                                    scrollBehavior = MiuixScrollBehavior(),
                                    listState = listState,
                                    landscape = false,
                                )
                            }
                        }
                    }
                })
            }
            val scanningOffset = playlistOffset(scenario, "home_recommendation_empty") { listState }
            scenario.onActivity { stage.value = 1 }
            assertEquals(scanningOffset, playlistOffset(scenario, "home_recommendation_loading") { listState })
            scenario.onActivity { stage.value = 2 }
            assertEquals(scanningOffset, playlistOffset(scenario, "home_recommendations") { listState })
        }
    }

    private fun playlistOffset(
        scenario: ActivityScenario<MainActivity>,
        recommendationKey: String,
        state: () -> LazyListState,
    ): Int {
        val deadline = SystemClock.uptimeMillis() + 5_000
        do {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            var offset: Int? = null
            scenario.onActivity {
                val items = state().layoutInfo.visibleItemsInfo
                if (items.any { it.key == recommendationKey }) {
                    offset = items.find { it.key == "home_playlists_title" }?.offset
                }
            }
            offset?.let { return it }
            SystemClock.sleep(16)
        } while (SystemClock.uptimeMillis() < deadline)
        error("Home did not measure $recommendationKey and its following playlist header")
    }

    private fun track() = MusicTrack(
        id = 1,
        title = "Track",
        artist = "Artist",
        album = null,
        durationMs = 60_000,
        dateAddedEpochSeconds = 1,
        dateModifiedEpochSeconds = 1,
        fileName = "track.mp3",
        fileSizeBytes = 100,
        contentUri = "content://music/startup-layout-test",
        titleSectionKey = "T",
        titleSortKey = "track",
    )
}
