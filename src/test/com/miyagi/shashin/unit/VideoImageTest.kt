package com.miyagi.shashin.unit

import ai.djl.modality.cv.ImageFactory
import com.miyagi.shashin.component.DjlFaceRecognizer
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.Settings
import com.miyagi.shashin.util.ImageProcessing
import com.miyagi.shashin.util.ImageProcessing.Companion.buildObjectRecognitionCriteria
import com.miyagi.shashin.util.VideoProcessing
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import java.io.File
import java.net.URL
import javax.imageio.ImageIO

@ActiveProfiles("test")
class VideoImageTest {

    @Test
    fun processGifTest() {
        val classLoader = javaClass.classLoader
        val testVideoUrl: URL = classLoader.getResource("earth.mp4")!!
        val testVideoFile = File(testVideoUrl.file)

        val videoProcessing = VideoProcessing(testVideoFile)
        val processedGifFile = videoProcessing.getVideoGifFile(10.0)
        var gifExists = false
        if (processedGifFile != null && processedGifFile.exists() && processedGifFile.length() > 0) {
            gifExists = true
        }

        Assertions.assertTrue(gifExists)
    }

    @Test
    fun processKeywordsTest() {
        val classLoader = javaClass.classLoader
        val testImageUrl: URL = classLoader.getResource("tablecup.jpg")!!

        // Create thumbnails
        var metadata = Metadata()
        metadata.setId("00000000-00000000-00000000-00000012")
        metadata.setPath(testImageUrl.path)

        var criteria = buildObjectRecognitionCriteria()
        val settings = Settings()
        settings.setObjectRecognitionConfidenceThreshold("0.45")
        val keywordArray = ImageProcessing.objectRecognizer(
            metadata,
            criteria!!,
            settings,
            null,
            null
        )

        Assertions.assertTrue(keywordArray.isNotEmpty())
    }

    @Test
    fun processObjectsTest() {
        val classLoader = javaClass.classLoader
        val testImageUrl: URL = classLoader.getResource("people.jpg")!!
        val testVideoFile = File(testImageUrl.file)

        val imageBi = ImageIO.read(testVideoFile)
        val img = ImageFactory.getInstance().fromImage(imageBi)

        val djlFaceRecognizer = DjlFaceRecognizer()
        val detect = djlFaceRecognizer.detect(img)
        Assertions.assertTrue(detect.numberOfObjects > 0)

        val predict = djlFaceRecognizer.predict(img)
        Assertions.assertTrue(predict.isNotEmpty())
    }
}