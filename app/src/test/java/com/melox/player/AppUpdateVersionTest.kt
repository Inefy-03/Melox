package com.melox.player

import com.melox.player.data.update.isNewerBuild
import com.melox.player.data.update.updateVersion
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateVersionTest {
    @Test
    fun buildTimestampWinsWhenBothVersionsHaveOne() {
        val installed = "2.0.0-beta4-2611200900"
        assertTrue(isNewerBuild(updateVersion("Melox_2.0.0-beta3-2611200901.apk"), "2.0.0-beta3", installed))
        assertFalse(isNewerBuild(updateVersion("Melox_2.0.0-beta5-2611200859.apk"), "2.0.0-beta5", installed))
        assertFalse(isNewerBuild(updateVersion(installed), "2.0.0-beta5", installed))
        assertNull(updateVersion("Melox_2.0.0-beta5-2611310900.apk"))
    }

    @Test
    fun tagFallbackComparesNumericSegmentsAndPrereleaseOrder() {
        assertTrue(isNewerBuild(null, "v1.2.10", "1.2.9-2611200900"))
        assertFalse(isNewerBuild(null, "v1.2.3.0", "1.2.3-2611200900"))
        assertTrue(isNewerBuild(null, "2.0.0-beta10", "2.0.0-beta9-2611200900"))
        assertTrue(isNewerBuild(null, "2.0.0", "2.0.0-rc1-2611200900"))
        assertFalse(isNewerBuild(null, "2.0.0-beta10", "2.0.0-2611200900"))
    }
}
