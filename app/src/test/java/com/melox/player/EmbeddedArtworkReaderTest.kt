package com.melox.player

import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import com.kyant.taglib.Picture
import com.melox.player.data.library.coverPicturePriority
import com.melox.player.data.library.readFlacPictureMetadata
import com.melox.player.data.library.readId3Pictures
import com.melox.player.data.library.selectEmbeddedCover
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNotNull
import org.junit.Test

@UnstableApi
class EmbeddedArtworkReaderTest {
    @Test
    fun flacPictureBlocksPreferFrontCoverRegardlessOfOrder() {
        val artist = byteArrayOf(8, 1)
        val front = byteArrayOf(3, 1, 2)
        val back = byteArrayOf(4, 1)
        for (blocks in listOf(listOf(8 to artist, 4 to back, 3 to front), listOf(3 to front, 8 to artist))) {
            val file = flac(blocks)
            val cover = readFlacPictureMetadata(file.inputStream())?.cover
            assertArrayEquals(front, cover?.readFrom(file.inputStream()))
        }
    }

    @Test
    fun artistOnlyPicturesDoNotBecomeSongCovers() {
        val artist = byteArrayOf(8, 1)
        val front = byteArrayOf(3, 1, 2)
        val portrait = Picture(artist, "Artist", "Artist", "image/jpeg")
        val frontCover = Picture(front, "", "Front Cover", "image/jpeg")
        assertNull(selectEmbeddedCover(arrayOf(portrait)) { it })
        assertArrayEquals(front, selectEmbeddedCover(arrayOf(portrait, frontCover)) { it })

        val metadata = readFlacPictureMetadata(flac(listOf(8 to artist)).inputStream())
        assertNotNull(metadata)
        assertNull(metadata?.cover)
        assertTrue(metadata?.hasPictureBlocks == true)

        val withCover = flac(listOf(3 to front, 8 to artist))
        val artistPicture = readFlacPictureMetadata(withCover.inputStream(), artistImage = true)?.cover
        assertArrayEquals(artist, artistPicture?.readFrom(withCover.inputStream()))
        assertArrayEquals(artist, selectEmbeddedCover(arrayOf(frontCover, portrait), artistImage = true) { it })
        assertNull(selectEmbeddedCover(arrayOf(frontCover), artistImage = true) { it })
    }

    @Test
    fun flacUnspecifiedPictureIsUsableAndRanksBelowFrontCover() {
        val unspecified = byteArrayOf(1, 2)
        val front = byteArrayOf(3, 4)
        for ((pictures, expected) in listOf(
            listOf(-1 to unspecified, 8 to byteArrayOf(8)) to unspecified,
            listOf(-1 to unspecified, 3 to front) to front,
        )) {
            val file = flac(pictures)
            val cover = readFlacPictureMetadata(file.inputStream())?.cover
            assertArrayEquals(expected, cover?.readFrom(file.inputStream()))
        }
    }

    @Test
    fun undecodableFrontCoverFallsBackToTheNextAlbumImage() {
        val bad = byteArrayOf(0)
        val icon = byteArrayOf(2)
        val back = byteArrayOf(4)
        val pictures = arrayOf(
            Picture(icon, "", "Other File Icon", "image/png"),
            Picture(bad, "", "Front Cover", "image/jpeg"),
            Picture(back, "", "Back Cover", "image/jpeg"),
        )
        assertArrayEquals(back, selectEmbeddedCover(pictures) { it.takeUnless { it === bad } })
        val file = flac(listOf(8 to byteArrayOf(8), 2 to icon))
        assertArrayEquals(icon, readFlacPictureMetadata(file.inputStream())?.cover?.readFrom(file.inputStream()))
    }

    @Test
    fun hanserFlacResolvesToItsSquareFrontCover() =
        assertSuppliedFlacCover("MELOX_FLAC_ARTWORK_REGRESSION_FILE", size = 1550)

    @Test
    fun matryoshkaFlacResolvesToItsSquareFrontCover() =
        assertSuppliedFlacCover("MELOX_MATRYOSHKA_ARTWORK_REGRESSION_FILE", size = 1500)

    @Test
    fun nineM88FlacResolvesToItsLargeFrontCover() =
        assertSuppliedFlacCover("MELOX_9M88_ARTWORK_REGRESSION_FILE", size = 3000)

    @Test
    fun cutleryFlacResolvesToItsUnspecifiedCover() =
        assertSuppliedFlacCover("MELOX_CUTLERY_ARTWORK_REGRESSION_FILE", size = 1400)

    @Test
    fun iNandesuMp3ResolvesToItsUnspecifiedId3Cover() {
        val path = System.getenv("MELOX_I_NANDESU_ARTWORK_REGRESSION_FILE")
        assumeNotNull(path)
        ParsableByteArray.setShouldEnforceLimitOnLegacyMethods(false)
        try {
            val picture = File(checkNotNull(path)).inputStream().use(::readId3Pictures).single()
            assertEquals(0xff, picture.pictureType)
            assertNotNull(coverPicturePriority(picture.pictureType))
            val image = javax.imageio.ImageIO.read(picture.pictureData.inputStream())
            assertEquals(1280, image.width)
            assertEquals(720, image.height)
        } finally {
            ParsableByteArray.setShouldEnforceLimitOnLegacyMethods(null)
        }
    }

    private fun assertSuppliedFlacCover(environmentVariable: String, size: Int) {
        val path = System.getenv(environmentVariable)
        assumeNotNull(path)
        val file = File(checkNotNull(path))
        val cover = file.inputStream().use(::readFlacPictureMetadata)?.cover
        val image = javax.imageio.ImageIO.read(file.inputStream().use(checkNotNull(cover)::readFrom).inputStream())
        assertEquals(size, image.width)
        assertEquals(size, image.height)
    }

    private fun flac(pictures: List<Pair<Int, ByteArray>>): ByteArray = ByteArrayOutputStream().also { file ->
        val output = DataOutputStream(file)
        output.writeInt(0x664C6143)
        pictures.forEachIndexed { index, (type, data) ->
            val mime = "image/jpeg".toByteArray()
            val blockLength = 32 + mime.size + data.size
            val header = (if (index == pictures.lastIndex) Int.MIN_VALUE else 0) or (6 shl 24) or blockLength
            output.writeInt(header)
            output.writeInt(type)
            output.writeInt(mime.size)
            output.write(mime)
            output.writeInt(0)
            repeat(4) { output.writeInt(0) }
            output.writeInt(data.size)
            output.write(data)
        }
    }.toByteArray()
}
