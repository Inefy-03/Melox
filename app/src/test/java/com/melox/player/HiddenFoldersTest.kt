package com.melox.player

import com.melox.player.data.library.MusicLibrarySnapshotCodec
import com.melox.player.data.library.buildFolderGroups
import com.melox.player.data.library.buildMusicLibraryStatistics
import com.melox.player.data.library.fullMusicFolderPath
import com.melox.player.data.library.hiddenFolderDisplayPaths
import com.melox.player.data.library.isMusicFolderHidden
import com.melox.player.data.library.normalizedHiddenFolderPaths
import com.melox.player.data.library.visibleMusicTracks
import com.melox.player.model.MusicTrack
import com.melox.player.ui.viewmodel.FolderPresentationState
import com.melox.player.ui.viewmodel.LibraryProjection
import com.melox.player.ui.viewmodel.visibleLibraryProjections
import com.melox.player.ui.viewmodel.withHiddenFolders
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HiddenFoldersTest {
    @Test
    fun fullAddressesResolvePrimaryRemovableAndDocumentVolumes() {
        val primary = track(1, "/Music/Album").copy(contentUri = "content://media/external_primary/audio/media/1")
        val removable = primary.copy(id = 2, contentUri = "content://media/ABCD-1234/audio/media/2")
        val document = primary.copy(contentUri = "content://com.android.externalstorage.documents/tree/ABCD-1234%3AMusic/document/ABCD-1234%3AMusic%2FAlbum%2F1.flac")
        assertEquals("/storage/emulated/0/Music/Album", fullMusicFolderPath(primary))
        assertEquals("/storage/ABCD-1234/Music/Album", fullMusicFolderPath(removable))
        assertEquals("/storage/ABCD-1234/Music/Album", fullMusicFolderPath(removable.copy(contentUri = "content://media/abcd-1234/audio/media/2")))
        assertEquals("/storage/ABCD-1234/Music/Album", fullMusicFolderPath(document))
        assertEquals("/storage/emulated/0", fullMusicFolderPath(primary.copy(folderPath = "/")))
        assertEquals("/storage/ABCD-1234/Music", fullMusicFolderPath(removable.copy(folderPath = "/storage/ABCD-1234/Music")))
    }

    @Test
    fun identicalRelativeFoldersStaySeparateAcrossVolumesAndHideOnlyTheirTarget() {
        val tracks = listOf(
            track(1, "/Music/Album").copy(contentUri = "content://media/external_primary/audio/media/1"),
            track(2, "/Music/Album").copy(contentUri = "content://media/ABCD-1234/audio/media/2"),
        )
        val folders = buildFolderGroups(tracks)
        assertEquals(2, folders.size)
        assertEquals(setOf("/storage/emulated/0/Music/Album", "/storage/ABCD-1234/Music/Album"), folders.map { it.path }.toSet())
        assertEquals(listOf(2L), visibleMusicTracks(tracks, listOf("/storage/emulated/0/Music")).map { it.id })
        assertEquals(emptyList<MusicTrack>(), visibleMusicTracks(tracks, listOf("/Music")))
        assertSame(tracks, visibleMusicTracks(tracks, emptyList()))
    }

    @Test
    fun legacyHiddenAddressesIncludeAllMatchingStorageRootsWithoutChangingRemovalKeys() {
        val tracks = listOf(
            track(1, "/Music/Album").copy(contentUri = "content://media/external_primary/audio/media/1"),
            track(2, "/Music/Album").copy(contentUri = "content://media/ABCD-1234/audio/media/2"),
        )
        val addresses = hiddenFolderDisplayPaths(tracks, listOf("/Music", "/storage/ABCD-1234/Other"))
        assertEquals("/storage/ABCD-1234/Music\n/storage/emulated/0/Music", addresses["/Music"])
        assertEquals("/storage/ABCD-1234/Other", addresses["/storage/ABCD-1234/Other"])
        assertEquals("/storage/emulated/0/Music", hiddenFolderDisplayPaths(emptyList(), listOf("/Music"))["/Music"])
    }

    @Test
    fun matchingIncludesDescendantsButNotSiblingPrefixes() {
        val paths = normalizedHiddenFolderPaths(listOf(" /Music/ ", "\\music\\"))
        assertEquals(listOf("/Music"), paths)
        assertTrue(isMusicFolderHidden("/MUSIC/Album", paths))
        assertTrue(isMusicFolderHidden("\\Music\\", paths))
        assertFalse(isMusicFolderHidden("/Music2", paths))
        assertFalse(isMusicFolderHidden(null, paths))
        assertTrue(isMusicFolderHidden("/Other", listOf("/")))
    }

    @Test
    fun largeFolderCanBeHiddenAndRestoredFromTheCompleteCache() {
        val allTracks = List(20_000) { track(it.toLong(), "/Music/Huge") } +
            track(20_000, "/Music2")
        val bytes = ByteArrayOutputStream().apply {
            MusicLibrarySnapshotCodec.write(this, allTracks)
        }.toByteArray()
        val restored = MusicLibrarySnapshotCodec.read(ByteArrayInputStream(bytes))
        assertEquals(listOf(20_000L), visibleMusicTracks(restored, listOf("/Music")).map { it.id })
        assertEquals(20_001, restored.size)
        assertSame(restored, visibleMusicTracks(restored, emptyList()))
        assertEquals(allTracks, restored)
    }

    @Test
    fun optimisticFolderRowsAndAlphabetIndicesChangeWithoutReadingSongs() {
        val folders = buildFolderGroups(listOf(track(1, "/Alpha"), track(2, "/Beta")))
            .map { folder ->
                folder.copy(tracks = object : AbstractList<MusicTrack>() {
                    override val size: Int get() = error("Immediate feedback must not inspect songs")
                    override fun get(index: Int): MusicTrack = error("Must not inspect songs")
                })
            }
        val presentation = FolderPresentationState(
            items = folders,
            sectionIndexMap = mapOf("A" to 0, "B" to 1),
        ).withHiddenFolders(listOf("/Alpha"))
        assertEquals(listOf("Beta"), presentation.items.map { it.name })
        assertEquals(mapOf("B" to 0), presentation.sectionIndexMap)
    }

    @Test
    fun scanUpdatesRespectHiddenPathsAndUnhideRebuildsEveryLibraryGroup() = runBlocking {
        val tracks = MutableStateFlow(listOf(track(1, "/Hidden"), track(2, "/Visible")))
        val paths = MutableStateFlow(listOf("/Hidden"))
        val results = Channel<LibraryProjection>(Channel.UNLIMITED)
        val collector = launch { visibleLibraryProjections(tracks, paths).collect(results::send) }
        suspend fun awaitIds(ids: List<Long>): LibraryProjection = withTimeout(5_000) {
            var result = results.receive()
            while (result.tracks.map { it.id } != ids) result = results.receive()
            result
        }
        try {
            val hidden = awaitIds(listOf(2))
            val statistics = buildMusicLibraryStatistics(hidden.tracks)
            assertEquals(1, statistics.totalTrackCount)
            assertEquals(100L, statistics.totalBytes)
            assertEquals(1, hidden.albums.size)
            assertEquals(1, hidden.artists.size)
            assertEquals(listOf("/Visible"), hidden.folders.map { it.path })
            tracks.value = tracks.value + track(3, "/Hidden/New") + track(4, "/Other")
            assertEquals(2, awaitIds(listOf(2, 4)).folders.size)
            paths.value = emptyList()
            val restored = awaitIds(listOf(1, 2, 3, 4))
            assertEquals(4, buildMusicLibraryStatistics(restored.tracks).totalTrackCount)
            assertEquals(4, restored.albums.size)
            assertEquals(4, restored.artists.size)
            assertEquals(4, restored.folders.size)
            paths.value = listOf("/Hidden")
            paths.value = emptyList()
            tracks.value = tracks.value + track(5, "/Hidden")
            assertEquals(5, awaitIds(listOf(1, 2, 3, 4, 5)).tracks.size)
        } finally {
            collector.cancelAndJoin()
        }
    }

    @Test
    fun slowProjectionRunsOffCallerThreadAndCannotPublishAfterHide() = runBlocking {
        val callerThread = Thread.currentThread()
        val started = CompletableDeferred<Unit>()
        val hiddenRequestObserved = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        val firstRead = AtomicBoolean(true)
        val tracks = object : AbstractList<MusicTrack>() {
            override val size = 1
            override fun get(index: Int): MusicTrack {
                assertTrue("Projection must run off the UI caller", Thread.currentThread() !== callerThread)
                if (firstRead.compareAndSet(true, false)) {
                    started.complete(Unit)
                    check(release.await(5, TimeUnit.SECONDS))
                }
                return track(1, "/Music")
            }
        }
        val paths = MutableStateFlow<List<String>>(emptyList())
        val emitted = mutableListOf<LibraryProjection>()
        val complete = CompletableDeferred<Unit>()
        val collector = launch {
            visibleLibraryProjections(
                MutableStateFlow(tracks),
                paths.onEach { if (it.isNotEmpty()) hiddenRequestObserved.complete(Unit) },
            ).collect {
                emitted += it
                complete.complete(Unit)
            }
        }
        try {
            withTimeout(5_000) { started.await() }
            paths.value = listOf("/Music")
            withTimeout(5_000) { hiddenRequestObserved.await() }
            repeat(4) { yield() }
            release.countDown()
            withTimeout(5_000) { complete.await() }
            assertTrue(emitted.isNotEmpty())
            assertTrue(emitted.all { it.tracks.isEmpty() })
        } finally {
            release.countDown()
            collector.cancelAndJoin()
        }
    }

    private fun track(id: Long, path: String) = MusicTrack(
        id = id,
        title = "Song $id",
        artist = "Artist $id",
        album = "Album $id",
        albumId = id + 1,
        durationMs = 60_000,
        dateAddedEpochSeconds = 0,
        dateModifiedEpochSeconds = 0,
        fileName = "$id.flac",
        fileSizeBytes = 100,
        contentUri = "content://music/$id",
        titleSectionKey = "S",
        titleSortKey = "song $id",
        folderPath = path,
    )
}
