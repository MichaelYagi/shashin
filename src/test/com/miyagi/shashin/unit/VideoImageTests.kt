package com.miyagi.shashin.unit

import ai.djl.modality.cv.ImageFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.service.DjlFaceRecognizer
import com.miyagi.shashin.e2e.BaseSeleniumTests
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.service.ImageProcessing
import com.miyagi.shashin.service.ImageProcessing.Companion.buildObjectRecognitionCriteria
import com.miyagi.shashin.service.VideoProcessing
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO

@ActiveProfiles("test")
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

    @Test
    fun processObjectRecognitionTest() {
        val classLoader = javaClass.classLoader
        val testImageUrl: String = (classLoader.getResource("subdir")!!.path)+"/tablecup.jpg"
        val testImageFile = File(testImageUrl)

        // Create thumbnails
        val metadata = Metadata()
        metadata.setId("00000000-00000000-00000000-00000012")
        metadata.setPath(testImageFile.path)
        val criteria = buildObjectRecognitionCriteria()
        val threshold = 0.70

        val objectMap = ImageProcessing.objectRecognizer(
            metadata,
            criteria!!,
            threshold
        )

        Assertions.assertTrue(objectMap.isNotEmpty())
        this.logger.log(Level.INFO, "Objects with confidence map and threshold $threshold: ${objectMap.map { "${it.key}: ${it.value}" }.joinToString(", ")}")

        val objectMap2 = ImageProcessing.objectRecognizer(
            metadata,
            criteria
        )
        Assertions.assertTrue(objectMap2.isNotEmpty())
        Assertions.assertTrue(objectMap2.size != objectMap.size)
        this.logger.log(Level.INFO, "processObjectRecognitionTest - Objects with confidence map: ${objectMap2.map { "${it.key}: ${it.value}" }.joinToString(", ")}")
    }

    @Test
    fun processDetectPredictTest() {
        val classLoader = javaClass.classLoader
        val testImageUrl: String = (classLoader.getResource("subdir")!!.path)+"/people.jpg"
        val testImageFile = File(testImageUrl)

        val imageBi = ImageIO.read(testImageFile)
        val img = ImageFactory.getInstance().fromImage(imageBi)

        val djlFaceRecognizer = DjlFaceRecognizer()
        val detect = djlFaceRecognizer.detect(img)
        Assertions.assertTrue(detect?.numberOfObjects!! > 0)

        val predict = djlFaceRecognizer.predict(img)
        Assertions.assertTrue(predict != null && predict.isNotEmpty())
        logger.log(Level.INFO, "processDetectPredictTest passed")
    }

    @Test
    fun processFaceRecognitionTest() {
        val mapper = ObjectMapper()

        val classLoader = javaClass.classLoader
        val testImageUrl: String = (classLoader.getResource("subdir")!!.path)+"/people.jpg"
        val testImageFile = File(testImageUrl)

        val imageBi = ImageIO.read(testImageFile)
        val img = ImageFactory.getInstance().fromImage(imageBi)

        val detectedTrainingImages = DjlFaceRecognizer().detect(img)
        val numOfObjects = detectedTrainingImages?.numberOfObjects
        Assertions.assertTrue(numOfObjects!! > 0)
        val trainingImageJsonNode = mapper.readTree(detectedTrainingImages.toJson())
        val metadata = Metadata()
        metadata.setId("asdf")
        metadata.setPath(testImageFile.path)

        val limit = 3

        // Test similarity
        val similarityArrays: MutableList<MutableList<Any>> = DjlFaceRecognizer().getSimilarities(
            limit,
            limit,
            imageBi,
            imageBi,
            trainingImageJsonNode,
            trainingImageJsonNode)

        Assertions.assertTrue(similarityArrays.isNotEmpty())

        var index = 0
        for (similarityArray in similarityArrays) {
            val similarity = similarityArray[0].toString().toFloat()

            if (index%(limit+1) == 0) {
                Assertions.assertTrue(similarity == 1.0F)
            } else {
                Assertions.assertTrue(similarity > 0)
            }

            this.logger.log(Level.INFO, "Iteration $index: s - $similarity")
            index++
        }
        logger.log(Level.INFO, "processFaceRecognitionTest passed")
    }
}