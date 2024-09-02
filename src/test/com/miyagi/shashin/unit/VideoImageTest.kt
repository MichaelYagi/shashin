package com.miyagi.shashin.unit

import ai.djl.modality.Classifications
import ai.djl.modality.cv.ImageFactory
import ai.djl.repository.zoo.ModelZoo
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.component.DjlFaceRecognizer
import com.miyagi.shashin.e2e.BaseSeleniumTests
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.util.ImageProcessing
import com.miyagi.shashin.util.ImageProcessing.Companion.buildObjectRecognitionCriteria
import com.miyagi.shashin.util.VideoProcessing
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import java.io.File
import java.net.URL
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO

@ActiveProfiles("test")
class VideoImageTest {

    var logger: Logger = Logger.getLogger(BaseSeleniumTests::class.simpleName)

    @Test
    fun processScreenshotTest() {
        val classLoader = javaClass.classLoader
        val testVideoUrl: URL = classLoader.getResource("dice.mp4")!!
        val testVideoFile = File(testVideoUrl.file)

        val videoProcessing = VideoProcessing(testVideoFile)
        val screenshotImage = videoProcessing.getVideoScreenshot()
        var ssImageExists = false
        if (screenshotImage != null && screenshotImage.height > 0 && screenshotImage.width > 0) {
            ssImageExists = true
        }

        Assertions.assertTrue(ssImageExists)
    }

    @Test
    fun processGifTest() {
        val classLoader = javaClass.classLoader
        val testVideoUrl: URL = classLoader.getResource("dice.mp4")!!
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
        val metadata = Metadata()
        metadata.setId("00000000-00000000-00000000-00000012")
        metadata.setPath(testImageUrl.path)

        val criteria = buildObjectRecognitionCriteria()
        val keywordArray = ImageProcessing.objectRecognizer(
            metadata,
            criteria!!,
            0.45
        )

        this.logger.log(Level.INFO, "keywords: ${keywordArray.joinToString(", ")}")

        Assertions.assertTrue(keywordArray.isNotEmpty())
    }

    @Test
    fun processDetectPredictTest() {
        val classLoader = javaClass.classLoader
        val testImageUrl: URL = classLoader.getResource("people.jpg")!!
        val testImageFile = File(testImageUrl.file)

        val imageBi = ImageIO.read(testImageFile)
        val img = ImageFactory.getInstance().fromImage(imageBi)

        val djlFaceRecognizer = DjlFaceRecognizer()
        val detect = djlFaceRecognizer.detect(img)
        Assertions.assertTrue(detect?.numberOfObjects!! > 0)

        val predict = djlFaceRecognizer.predict(img)
        Assertions.assertTrue(predict != null && predict.isNotEmpty())
    }

    @Test
    fun processObjectRecognizerTest() {
        val classLoader = javaClass.classLoader
        val testImageUrl: URL = classLoader.getResource("people.jpg")!!
        val testImageFile = File(testImageUrl.file)

        val imageBi = ImageIO.read(testImageFile)
        val img = ImageFactory.getInstance().fromImage(imageBi)

        val criteria = buildObjectRecognitionCriteria()
        Assertions.assertNotNull(criteria)

        ModelZoo.loadModel(criteria).use { objmodel ->
            objmodel.newPredictor().use { predictor ->
                val detection = predictor.predict(img)
                val numOfObjects = detection.numberOfObjects

                Assertions.assertTrue(numOfObjects > 0)

                if (numOfObjects > 0) {
                    for (i in 0 until numOfObjects) {

                        val objProbability =
                            detection.item<Classifications.Classification?>(i).probability
                        val objSubject =
                            detection.item<Classifications.Classification?>(i).className

                        Assertions.assertTrue(objSubject.isNotEmpty())
                        Assertions.assertTrue(objProbability > 0)

                        this.logger.log(Level.INFO, "Iteration ${i+1}: p - $objProbability; s - $objSubject")
                    }
                }
            }
        }
    }

    @Test
    fun processFaceRecognitionTest() {
        val mapper = ObjectMapper()

        val classLoader = javaClass.classLoader
        val testImageUrl: URL = classLoader.getResource("people.jpg")!!
        val testImageFile = File(testImageUrl.file)

        val imageBi = ImageIO.read(testImageFile)
        val img = ImageFactory.getInstance().fromImage(imageBi)

        val detectedTrainingImages = DjlFaceRecognizer().detect(img)
        val numOfObjects = detectedTrainingImages?.numberOfObjects
        Assertions.assertTrue(numOfObjects!! > 0)
        val trainingImageJsonNode = mapper.readTree(detectedTrainingImages?.toJson())

        // Test against same image against different faces detected
        for (i in 0 until 2) {

            // Get sub images
            val trainingSubImageBi = DjlFaceRecognizer().getSubImage(imageBi, trainingImageJsonNode, i)
            val trainingSubImage = ImageFactory.getInstance().fromImage(trainingSubImageBi)

            try {
                val trainingFeature = DjlFaceRecognizer().predict(trainingSubImage)

                for (j in 0 until 2) {

                    val testSubImageBi = DjlFaceRecognizer().getSubImage(imageBi, trainingImageJsonNode, j)
                    val testSubImage = ImageFactory.getInstance().fromImage(testSubImageBi)

                    try {
                        val testFeature = DjlFaceRecognizer().predict(testSubImage)

                        // Compare images
                        val similarity =
                            DjlFaceRecognizer().calculateSimilar(trainingFeature, testFeature)

                        this.logger.log(Level.INFO, "Iteration $i-$j: s - $similarity")

                        if (i == j) {
                            Assertions.assertTrue(similarity == 1.0F)
                        } else {
                            Assertions.assertTrue(similarity > 0)
                        }

                    } catch (_: Exception) {
                        Assertions.assertTrue(false)
                    }
                }
            } catch (_: Exception) {
                Assertions.assertTrue(false)
            }
        }
    }
}