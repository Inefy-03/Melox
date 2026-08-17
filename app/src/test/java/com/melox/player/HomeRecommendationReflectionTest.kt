package com.melox.player

import com.melox.player.ui.component.home.HOME_RECOMMENDATION_REFLECTION_SIZE_PX
import com.melox.player.ui.component.home.createHomeRecommendationReflectionCacheKey
import com.melox.player.ui.component.home.createHomeRecommendationReflectionMesh
import com.melox.player.ui.screen.home.homeRecommendationCardReady
import com.melox.player.ui.screen.home.homeRecommendationCropPlacement
import com.melox.player.ui.screen.home.homeRecommendationReflectionPivotY
import com.melox.player.ui.screen.home.homeRecommendationUsesReflection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRecommendationReflectionTest {
    @Test
    fun reflectionCacheKeyTracksArtworkIdentityAndFileVersion() {
        val baseKey = createHomeRecommendationReflectionCacheKey(
            contentUri = "content://media/audio/7",
            dateModifiedEpochSeconds = 10L,
            fileSizeBytes = 20L,
        )

        assertTrue(baseKey.contains("home-recommendation-reflection-v1"))
        assertTrue(baseKey.contains("|$HOME_RECOMMENDATION_REFLECTION_SIZE_PX|"))
        assertNotEquals(
            baseKey,
            createHomeRecommendationReflectionCacheKey(
                contentUri = "content://media/audio/7",
                dateModifiedEpochSeconds = 11L,
                fileSizeBytes = 20L,
            ),
        )
        assertNotEquals(
            baseKey,
            createHomeRecommendationReflectionCacheKey(
                contentUri = "content://media/audio/8",
                dateModifiedEpochSeconds = 10L,
                fileSizeBytes = 20L,
            ),
        )
    }

    @Test
    fun reflectionMeshUsesThirtySixScaledVertices() {
        val mesh = createHomeRecommendationReflectionMesh(width = 100, height = 200)

        assertEquals(72, mesh.size)
        assertEquals(-23.51f, mesh[0], 0.001f)
        assertEquals(-19.34f, mesh[1], 0.001f)
        assertEquals(118.68f, mesh[70], 0.001f)
        assertEquals(205.66f, mesh[71], 0.001f)
        assertTrue(mesh.indices.filter { it % 2 == 0 }.any { mesh[it] < 0f })
        assertTrue(mesh.indices.filter { it % 2 == 0 }.any { mesh[it] > 100f })
        assertTrue(mesh.indices.filter { it % 2 == 1 }.any { mesh[it] < 0f })
        assertTrue(mesh.indices.filter { it % 2 == 1 }.any { mesh[it] > 200f })
    }

    @Test
    fun reflectionUsesArtworkCropAndMirrorPlacement() {
        val clearPlacement = homeRecommendationCropPlacement(
            sourceWidth = 512,
            sourceHeight = 512,
            destinationWidth = 240f,
            destinationHeight = 240f,
        )
        val reflectionPlacement = homeRecommendationCropPlacement(
            sourceWidth = 96,
            sourceHeight = 96,
            destinationWidth = 240f,
            destinationHeight = 190f,
            verticalOffset = 60f,
        )
        val pivotY = homeRecommendationReflectionPivotY(
            cardHeight = 310f,
            clearArtworkHeight = 240f,
        )

        assertEquals(0f, clearPlacement.left, 0f)
        assertEquals(0f, clearPlacement.top, 0f)
        assertEquals(240f, clearPlacement.width, 0f)
        assertEquals(240f, clearPlacement.height, 0f)
        assertEquals(0f, reflectionPlacement.left, 0f)
        assertEquals(35f, reflectionPlacement.top, 0.001f)
        assertEquals(240f, reflectionPlacement.width, 0.001f)
        assertEquals(240f, reflectionPlacement.height, 0.001f)
        assertEquals(215f, pivotY, 0f)
        assertEquals(
            155f,
            2f * pivotY - (reflectionPlacement.top + reflectionPlacement.height),
            0.001f,
        )
    }

    @Test
    fun reflectionRequiresTheSettingAndBothBitmaps() {
        assertTrue(homeRecommendationUsesReflection(true, true, true))
        assertFalse(homeRecommendationUsesReflection(false, true, true))
        assertFalse(homeRecommendationUsesReflection(true, false, true))
        assertFalse(homeRecommendationUsesReflection(true, true, false))
    }

    @Test
    fun cardWaitsForEveryBitmapRequiredByTheSelectedPresentation() {
        assertTrue(homeRecommendationCardReady(false, true, false))
        assertFalse(homeRecommendationCardReady(false, false, false))
        assertTrue(homeRecommendationCardReady(true, true, true))
        assertFalse(homeRecommendationCardReady(true, true, false))
        assertFalse(homeRecommendationCardReady(true, false, true))
    }
}
