package com.miyagi.shashin.unit

import com.miyagi.shashin.ToolsControllerTestConfig
import com.miyagi.shashin.e2e.BaseSeleniumTests
import com.miyagi.shashin.service.VideoProcessing
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

@ActiveProfiles("test")
@Import(ToolsControllerTestConfig::class)
class VideoImageTests {

    var logger: Logger = Logger.getLogger(BaseSeleniumTests::class.simpleName)

    @Test
    fun processScreenshotTest() {
        val classLoader = javaClass.classLoader
        val testVideoUrl: String = (classLoader.getResource("subdir")!!.path)+"/dice.mp4"
        val testVideoFile = File(testVideoUrl)

        val videoProcessing = VideoProcessing(testVideoFile)
        val screenshotImage = videoProcessing.getVideoScreenshot()
        var ssImageExists = false
        if (screenshotImage != null && screenshotImage.height > 0 && screenshotImage.width > 0) {
            ssImageExists = true
        }

        Assertions.assertTrue(ssImageExists)
        logger.log(Level.INFO, "processScreenshotTest passed")
    }

    @Test
    fun processGifTest() {
        val classLoader = javaClass.classLoader
        val testVideoUrl: String = (classLoader.getResource("subdir")!!.path)+"/dice.mp4"
        val testVideoFile = File(testVideoUrl)

        val videoProcessing = VideoProcessing(testVideoFile)
        val processedGifFile = videoProcessing.getVideoGifFile(5.0)
        var gifExists = false
        if (processedGifFile != null && processedGifFile.exists() && processedGifFile.length() > 0) {
            gifExists = true
        }

        Assertions.assertTrue(gifExists)
        logger.log(Level.INFO, "processGifTest passed")
    }
}
