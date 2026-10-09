package com.melox.player

import com.melox.player.data.playlist.PlaylistSortConfig
import com.melox.player.data.playlist.PlaylistSortField
import com.melox.player.data.repository.decodePlaylistSortConfigs
import com.melox.player.data.repository.encodePlaylistSortConfigs
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistSortPreferencesTest {
    @Test
    fun storageRoundTripRetainsIndependentPlaylistFieldsAndDirections() {
        val configs = mapOf(
            "first" to PlaylistSortConfig(PlaylistSortField.TITLE, true),
            "second" to PlaylistSortConfig(PlaylistSortField.FILE_NAME, false),
            "third" to PlaylistSortConfig(PlaylistSortField.CUSTOM, true),
        )
        assertEquals(configs, decodePlaylistSortConfigs(encodePlaylistSortConfigs(configs)))
        val changed = configs + ("first" to PlaylistSortConfig(PlaylistSortField.DURATION))
        val restored = decodePlaylistSortConfigs(encodePlaylistSortConfigs(changed))
        assertEquals(configs["second"], restored["second"])
        assertEquals(configs["third"], restored["third"])
        assertEquals(PlaylistSortField.DURATION, restored["first"]?.field)
    }

    @Test
    fun malformedAndUnknownValuesLeaveDefaultSortAvailable() {
        val restored = decodePlaylistSortConfigs(
            setOf("TITLE|true|valid", "FUTURE|false|unknown", "TITLE|maybe|broken", "CUSTOM|false|", "bad"),
        )
        assertEquals(mapOf("valid" to PlaylistSortConfig(PlaylistSortField.TITLE, true)), restored)
        assertEquals(PlaylistSortConfig(), restored["unknown"] ?: PlaylistSortConfig())
        assertEquals(emptyMap<String, PlaylistSortConfig>(), decodePlaylistSortConfigs(emptySet()))
    }
}
