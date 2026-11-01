package com.miyagi.shashin.unit

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.service.ImageProcessing
import com.miyagi.shashin.service.KirokuMetadata
import com.miyagi.shashin.service.MetadataProcessing
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/** Metadata extraction through Kiroku, checked against files whose metadata is known (see test resources/metadata/README.md). */
class MetadataProcessingTests {
    @TempDir
    lateinit var tempDir: Path

    private fun resource(name: String): File = File(javaClass.classLoader.getResource("metadata/$name")!!.toURI())

    private fun sidecarDir(): String = tempDir.toFile().absolutePath.replace('\\', '/') + "/"

    private fun populate(file: File): Metadata = MetadataProcessing("v1", file, sidecarDir(), Metadata(), "").populateMetadata()

    @Test
    fun photoWithFullExif() {
        val m = populate(resource("jpeg_full_exif.jpg"))

        Assertions.assertEquals("image/jpeg", m.getType())
        Assertions.assertEquals("jpg", m.getExpectedExtension())
        Assertions.assertEquals("Kiroku Test Optics KT-1000 Mark II", m.getCamera())
        Assertions.assertEquals("Kiroku KT 35mm F1.8", m.getLens())
        Assertions.assertEquals(400, m.getIso())
        Assertions.assertEquals("1/250", m.getExposure())
        Assertions.assertEquals(2.8, m.getFstopNumber())
        Assertions.assertEquals(35.0, m.getFocalLength())
        Assertions.assertEquals(48.858222, m.getLat()!!.toDouble(), 1e-6)
        Assertions.assertEquals(2.2945, m.getLng()!!.toDouble(), 1e-6)
        Assertions.assertEquals("2023-06-17 14:32:10", m.getTakenAt())
        Assertions.assertEquals(2023, m.getYear())
        Assertions.assertEquals(6, m.getMonth())
        Assertions.assertEquals(17, m.getDay())
        Assertions.assertEquals("14:32:10", m.getTime())
        Assertions.assertEquals("Baseline", m.getCompressionType())
        Assertions.assertEquals(480, m.getOriginalImageWidth())
        Assertions.assertEquals(640, m.getOriginalImageHeight())
        Assertions.assertNull(m.getDuration())
    }

    @Test
    fun iphoneVideoUsesTheSameFieldsAsPhotos() {
        val m = populate(resource("video_iphone_style.mov"))

        Assertions.assertEquals("video/quicktime", m.getType())
        Assertions.assertEquals("mov", m.getExpectedExtension())
        Assertions.assertEquals("Apple iPhone 15 Pro", m.getCamera())
        // Local time at the place of capture (+09:00), like a photo's EXIF time
        Assertions.assertEquals("2024-05-03 10:15:30", m.getTakenAt())
        Assertions.assertEquals(35.6895, m.getLat()!!.toDouble(), 1e-4)
        Assertions.assertEquals(139.6917, m.getLng()!!.toDouble(), 1e-4)
        Assertions.assertEquals("0:01", m.getDuration())
        // Stored 320x240 with a 90 degree display matrix
        Assertions.assertEquals(240, m.getOriginalImageWidth())
        Assertions.assertEquals(320, m.getOriginalImageHeight())
        Assertions.assertTrue(m.getCompressionType()!!.isNotBlank())
        Assertions.assertTrue(m.getVideoUrl()!!.startsWith("/api/v1/video/"))
    }

    @Test
    fun exifSidecarIsWritten() {
        val file = resource("jpeg_full_exif.jpg")
        populate(file)

        val sidecar = tempDir.toFile().walk().first { it.name == file.name + ".exif.yaml" }
        val tree = ObjectMapper(YAMLFactory()).readTree(sidecar)
        Assertions.assertEquals("Kiroku Test Optics", tree["EXIF:IFD0"]["Make"].textValue())
        Assertions.assertTrue(tree.has("EXIF:GPS"))
    }

    @Test
    fun mirroredOrientationIsApplied() {
        val file = resource("orientation7.jpg")
        val result = KirokuMetadata.read(file)
        Assertions.assertEquals(90, KirokuMetadata.rotation(result))
        Assertions.assertTrue(KirokuMetadata.mirrored(result))

        val m = ImageProcessing("v1", file, sidecarDir(), populate(file)).createThumbnails()!!
        // 400x200 stored, so portrait once rotated
        Assertions.assertEquals(200, m.getOriginalImageWidth())
        Assertions.assertEquals(400, m.getOriginalImageHeight())
        Assertions.assertTrue(m.getThumbnailSmallWidth()!! < m.getThumbnailSmallHeight()!!)
    }

    @Test
    fun unreadableFileKeepsFileTimes() {
        val file = tempDir.resolve("not-media.jpg").toFile()
        file.writeText("not an image")
        val m = populate(file)

        Assertions.assertNull(m.getCamera())
        Assertions.assertNull(m.getIso())
        Assertions.assertNotNull(m.getTakenAt())
    }

    @Test
    fun mpegMajorBrands() {
        Assertions.assertTrue(KirokuMetadata.isMpegMajorBrand("mp21"))
        Assertions.assertTrue(KirokuMetadata.isMpegMajorBrand("MSNV"))
        Assertions.assertFalse(KirokuMetadata.isMpegMajorBrand("isom"))
        Assertions.assertFalse(KirokuMetadata.isMpegMajorBrand("qt"))
        Assertions.assertFalse(KirokuMetadata.isMpegMajorBrand(null))
    }
}
