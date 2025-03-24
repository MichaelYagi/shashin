package com.miyagi.shashin.component

import ai.djl.ModelException
import ai.djl.inference.Predictor
import ai.djl.modality.cv.Image
import ai.djl.modality.cv.ImageFactory
import ai.djl.modality.cv.output.DetectedObjects
import ai.djl.modality.cv.transform.Normalize
import ai.djl.modality.cv.transform.Resize
import ai.djl.modality.cv.transform.ToTensor
import ai.djl.ndarray.NDList
import ai.djl.repository.zoo.Criteria
import ai.djl.translate.Pipeline
import ai.djl.translate.TranslateException
import ai.djl.translate.Translator
import ai.djl.translate.TranslatorContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.*
import com.miyagi.shashin.repository.RecognitionLabelPhotoRepository
import com.miyagi.shashin.repository.RecognitionLabelRepository
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.MetricsUtil
import com.miyagi.shashin.util.TextUtils
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import net.coobird.thumbnailator.Thumbnails
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO
import kotlin.math.sqrt


class DjlFaceRecognizer {
    private var logger: Logger = Logger.getLogger(DjlFaceRecognizer::class.simpleName)

    private var testImages: MutableIterable<Metadata>? = null
    private var trainingData: MutableIterable<TrainingData>? = null
    private var recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository? = null
    private var recognitionLabelRepository: RecognitionLabelRepository? = null
    private lateinit var cascadeFileList: MutableList<String>
    private lateinit var sidecarDir: String
    private lateinit var settings: Settings
    private lateinit var threadFile: File
    private var shouldStop: AtomicBoolean = AtomicBoolean(false)

    constructor()

    constructor(testImages: MutableIterable<Metadata>, trainingData: MutableIterable<TrainingData>, recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository?, recognitionLabelRepository: RecognitionLabelRepository?, settings: Settings, relativeSidecarDir: String, threadFile: File, shouldStop: AtomicBoolean = AtomicBoolean(false)) : this() {
        this.trainingData = trainingData
        this.testImages = testImages
        this.recognitionLabelPhotoRepository = recognitionLabelPhotoRepository
        this.recognitionLabelRepository = recognitionLabelRepository
        this.cascadeFileList = mutableListOf()
        val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
        this.sidecarDir = rootPath + relativeSidecarDir
        this.settings = settings
        this.threadFile = threadFile
        this.shouldStop = shouldStop
    }

    fun getSubImage(image: BufferedImage, jsonNode: JsonNode, index: Int): BufferedImage {
//        val cornerMin = jsonNode.get(index).get("boundingBox").get("corners")[0]
//        val xMin = cornerMin.get("x").toString().toDouble()
//        val yMin = cornerMin.get("y").toString().toDouble()
//        val cornerMax = jsonNode.get(index).get("boundingBox").get("corners")[2]
//        val xMax = cornerMax.get("x").toString().toDouble()
//        val yMax = cornerMax.get("y").toString().toDouble()

        val rectangle = jsonNode.get(index).get("boundingBox").get("rect")
        val xMin = rectangle.get(0).toString().toDouble()
        val yMin = rectangle.get(1).toString().toDouble()
        val xMax = rectangle.get(2).toString().toDouble()
        val yMax = rectangle.get(3).toString().toDouble()

        val y1 = yMin * image.height
        val x1 = xMin * image.width
        val y2 = yMax * image.height
        val x2 = xMax * image.width

        return image.getSubimage(x1.toInt(), y1.toInt(), (x2-x1).toInt(), (y2-y1).toInt())
    }

    fun startPredict(): Int {
        var recognitionCount = 0

        val metricsUtil = MetricsUtil()
        metricsUtil.start("Face comparisons")

        if (this.trainingData != null && this.trainingData!!.count() > 0 && this.testImages != null && this.testImages!!.count() > 0) {
            val mapper = ObjectMapper()

            val trainingDataCount = this.trainingData!!.count()
            val testImagesCount = this.testImages!!.count()

            // Loop through training data
            var trainingDataCurrentCount = 0
            for (trainingImageObj in this.trainingData!!) {
                if (this.shouldStop.get()) {
                    break
                }

                logger.log(
                    Level.INFO,
                    "Processing training image ${trainingDataCurrentCount+1}/$trainingDataCount - ${trainingImageObj.getRecognitionLabelName()!!} using training image ${trainingImageObj.getPath()}"
                )

                var trainingImageTn = Thumbnails.of(ImageIO.read(File(trainingImageObj.getPath()!!)))
                if (trainingImageObj.getType()?.contains("video") == true) {
                    trainingImageTn = if (trainingImageObj.getThumbnailUrlOriginal() !== null) {
                        val path = this.sidecarDir + (trainingImageObj.getThumbnailUrlOriginal()!!.replace("/api/v1/",""))
                        Thumbnails.of(ImageIO.read(File(path)))
                    } else {
                        Thumbnails.of(ImageIO.read(File(trainingImageObj.getThumbnailPathSmall()!!)))
                    }
                }

                val scaledTrainingImage = trainingImageTn
                    .outputQuality(0.5)
                    .imageType(BufferedImage.TYPE_BYTE_GRAY)
                    .height(1000)
                    .asBufferedImage()

                val trainingImage = ImageFactory.getInstance().fromImage(scaledTrainingImage)
                val detectedTrainingImages = detect(trainingImage)
                if (detectedTrainingImages == null) {
                    logger.log(
                        Level.WARNING,
                        "Something went wrong. Faces could not be recognized in ${trainingImageObj.getPath()} training image"
                    )
                    break
                }
                val numOfTrainingObject = detectedTrainingImages?.numberOfObjects
                val trainingImageJsonNode = mapper.readTree(detectedTrainingImages?.toJson())

                FileUtils.writeToThreadFileAndLogMessage("$numOfTrainingObject faces detected in ${trainingImageObj.getPath()} training image",this.threadFile)
                logger.log(
                    Level.INFO,
                    "$numOfTrainingObject faces detected in ${trainingImageObj.getPath()} training image"
                )

                // Loop through test images
//                this.testImages!!.forEachIndexed { testImagesCurrentCount, testImageObj ->
                var testImagesCurrentCount = 0
                for (testImageObj in this.testImages!!) {
                    if (this.shouldStop.get()) {
                        break
                    }
                    logger.log(
                        Level.INFO,
                        "Processing test image ${testImagesCurrentCount+1}/$testImagesCount - ${testImageObj.getPath()}"
                    )

                    var scaledTestImageTn = Thumbnails.of(ImageIO.read(File(testImageObj.getPath()!!)))
                    if (testImageObj.getType()?.contains("video") == true) {
                        if (testImageObj.getThumbnailUrlOriginal() !== null) {
                            val path = this.sidecarDir + (testImageObj.getThumbnailUrlOriginal()!!.replace("/api/v1/",""))
                            scaledTestImageTn = Thumbnails.of(ImageIO.read(File(path)))
                        } else {
                            scaledTestImageTn =
                                Thumbnails.of(ImageIO.read(File(testImageObj.getThumbnailPathSmall()!!)))
                        }
                    }

                    val scaledTestImage = scaledTestImageTn
                        .outputQuality(0.5)
                        .imageType(BufferedImage.TYPE_BYTE_GRAY)
                        .height(1000)
                        .asBufferedImage()

                    val testImage = ImageFactory.getInstance().fromImage(scaledTestImage)
                    val detectedTestImages = detect(testImage)
                    val numOfTestObject = detectedTestImages?.numberOfObjects
                    val testImageJsonNode = mapper.readTree(detectedTestImages?.toJson())

                    FileUtils.writeToThreadFileAndLogMessage("$numOfTestObject faces detected in ${testImageObj.getPath()} test image",this.threadFile)
                    logger.log(
                        Level.INFO,
                        "$numOfTestObject faces detected in ${testImageObj.getPath()} test image"
                    )

                    if (numOfTestObject == 0) {
                        recognitionLabelPhotoRepository?.deleteByMetadataId(testImageObj.getId())
                        val recognitionLabelRecord = recognitionLabelRepository?.findByNameIgnoreCase(TextUtils.getObjectName())
                        var recognitionLabelObj = RecognitionLabel()
                        if (recognitionLabelRecord == null) {
                            recognitionLabelObj.setName(TextUtils.getObjectName())
                            recognitionLabelObj.setCreatedAt(getCurrentTimestamp())
                            recognitionLabelObj.setModifiedAt(getCurrentTimestamp())
                            recognitionLabelRepository?.save(recognitionLabelObj)
                        } else {
                            recognitionLabelObj = recognitionLabelRecord
                        }

                        val recognitionLabelPhotoObj = RecognitionLabelPhoto()
                        recognitionLabelPhotoObj.setMetadataId(testImageObj.getId())
                        recognitionLabelPhotoObj.setRecognitionLabelId(recognitionLabelObj.getId())
                        recognitionLabelPhotoObj.setConfidence("-0.1")
                        recognitionLabelPhotoRepository?.save(recognitionLabelPhotoObj)
                    }

                    val similarityArrays: MutableList<MutableList<Any>> = getSimilarities(
                        numOfTrainingObject!!,
                        numOfTestObject!!,
                        scaledTrainingImage,
                        scaledTestImage,
                        trainingImageJsonNode,
                        testImageJsonNode,
                        trainingImageObj.getRecognitionLabelId()!!,
                        testImageObj.getId(),
                        trainingImageObj,
                        testImageObj,
                        shouldStop)

                    for (similarityArray in similarityArrays) {
                        val similarity = similarityArray[0] as Double
                        val trainingImageLabelId = similarityArray[1] as Int
                        val testImageLabelId = similarityArray[2] as String

                        // Save record if greater than threshold
                        if (similarity >= this.settings.getRecognitionConfidenceThreshold()!!.toDouble()) {
                            recognitionCount++

                            val recordCount =
                                this.recognitionLabelPhotoRepository?.countByRecognitionLabelIdAndMetadataId(
                                    trainingImageLabelId,
                                    testImageLabelId
                                )
                            if (recordCount == 0) {
                                val recognitionLabelPhoto = RecognitionLabelPhoto()
                                recognitionLabelPhoto.setMetadataId(testImageLabelId)
                                recognitionLabelPhoto.setRecognitionLabelId(trainingImageLabelId)
                                recognitionLabelPhoto.setConfidence(similarity.toString())
                                recognitionLabelPhoto.setAutoTagged(true)
                                this.recognitionLabelPhotoRepository?.save(recognitionLabelPhoto)
                            } else {
                                val recognitionLabelPhoto =
                                    this.recognitionLabelPhotoRepository?.findByRecognitionLabelIdAndMetadataId(
                                        trainingImageLabelId,
                                        testImageLabelId
                                    )
                                if (recognitionLabelPhoto != null) {
                                    recognitionLabelPhoto.setConfidence(similarity.toString())
                                    recognitionLabelPhoto.setAutoTagged(true)
                                    this.recognitionLabelPhotoRepository?.save(
                                        recognitionLabelPhoto
                                    )
                                }
                            }
                        }
                    }

                    testImagesCurrentCount++
                }
                trainingDataCurrentCount++
            }
        }

        metricsUtil.end()

        return recognitionCount
    }

    fun getSimilarities(numOfTrainingObject: Int,
                        numOfTestObject: Int,
                        trainingImage: BufferedImage,
                        testImage: BufferedImage,
                        trainingImageJsonNode: JsonNode,
                        testImageJsonNode: JsonNode,
                        recognitionLabelId: Int = 0,
                        testImageId: String = "test",
                        trainingImageObj: TrainingData? = null,
                        testImageObj: Metadata? = null,
                        shouldStop: AtomicBoolean? = null): MutableList<MutableList<Any>>
    {
        val similarityArray = mutableListOf<MutableList<Any>>()

        for (i in 0 until numOfTrainingObject) {
            if (shouldStop != null && shouldStop.get()) {
                break
            }

            // Get sub images
            val trainingSubImageBi = getSubImage(trainingImage, trainingImageJsonNode, i)
            val trainingSubImage = ImageFactory.getInstance().fromImage(trainingSubImageBi)

            if (trainingImageObj != null) {
                FileUtils.writeToThreadFileAndLogMessage("Predicting faces detected in ${trainingImageObj.getPath()} training image",this.threadFile)
                logger.log(
                    Level.INFO,
                    "Predicting faces detected in ${trainingImageObj.getPath()} training image"
                )
            } else {
                logger.log(
                    Level.INFO,
                    "Predicting faces detected in training image"
                )
            }

            try {
                val trainingFeature = predict(trainingSubImage)

                for (j in 0 until numOfTestObject) {
                    if (shouldStop != null && shouldStop.get()) {
                        break
                    }

                    val testSubImageBi = getSubImage(testImage, testImageJsonNode, j)
                    val testSubImage = ImageFactory.getInstance().fromImage(testSubImageBi)

                    if (testImageObj != null) {
                        FileUtils.writeToThreadFileAndLogMessage(
                            "Predicting faces detected in ${testImageObj.getPath()} test image",
                            this.threadFile
                        )
                    }
                    logger.log(
                        Level.INFO,
                        "Predicting faces detected in test image"
                    )

                    try {
                        val testFeature = predict(testSubImage)

                        // Compare images
                        val similarity =
                            calculateSimilar(trainingFeature, testFeature).toString()

                        var message =
                            "Similarity $similarity for ${i + 1}/$numOfTrainingObject test image face ${j + 1}/$numOfTestObject"
                        if (trainingImageObj != null && testImageObj != null) {
                            message =
                                "Similarity $similarity for person ${trainingImageObj.getRecognitionLabelId()} ${trainingImageObj.getRecognitionLabelName()} between training image ${trainingImageObj.getPath()} face ${i + 1}/$numOfTrainingObject and test image ${testImageObj.getPath()} face ${j + 1}/$numOfTestObject"
                            FileUtils.writeToThreadFileAndLogMessage(message, this.threadFile)
                        }
                        logger.log(
                            Level.INFO,
                            message
                        )

                        val similarityData = mutableListOf<Any>()
                        similarityData.add(similarity.toDouble())
                        similarityData.add(recognitionLabelId.toInt())
                        similarityData.add(testImageId.toString())
                        similarityArray.add(similarityData)

                    } catch (e: Exception) {
                        if (testImageObj != null) {
                            logger.log(
                                Level.WARNING,
                                "Could not predict for test image ${testImageObj.getPath()}. ${e.message}"
                            )
                        } else {
                            logger.log(
                                Level.WARNING,
                                "Could not predict for test image. ${e.message}"
                            )
                        }
                        continue
                    }
                }
            } catch (e: Exception) {
                if (trainingImageObj != null) {
                    logger.log(
                        Level.WARNING,
                        "Could not predict for training image ${trainingImageObj.getPath()}. ${e.message}"
                    )
                } else {
                    logger.log(
                        Level.WARNING,
                        "Could not predict for training image. ${e.message}"
                    )
                }
                continue
            }
        }

        return similarityArray
    }

    private fun calculateSimilar(feature1: FloatArray?, feature2: FloatArray?): Float {
        if (feature1 != null && feature2 != null) {
            var ret = 0.0f
            var mod1 = 0.0f
            var mod2 = 0.0f
            val length = feature1.size
            for (i in 0 until length) {
                ret += feature1[i] * feature2[i]
                mod1 += feature1[i] * feature1[i]
                mod2 += feature2[i] * feature2[i]
            }
            return ((ret / sqrt(mod1.toDouble()) / sqrt(mod2.toDouble()) + 1) / 2.0f).toFloat()
        } else {
            return 0.0f
        }
    }

    @Throws(IOException::class, ModelException::class, TranslateException::class)
    fun predict(img: Image): FloatArray? {
        img.wrappedImage

        var criteria: Criteria<Image, FloatArray>? = null

        val tempFilePath = System.getProperty("java.io.tmpdir") + "/vggface2.pt"
        val tempFile = File(tempFilePath)
        if (!tempFile.exists()) {
            val vggStream = ClassPathResource("lib/vggface2.pt").inputStream
            tempFile.deleteOnExit()
            org.apache.commons.io.FileUtils.copyInputStreamToFile(vggStream, tempFile)
        }
        val vggPath = tempFile.path

        try {
            criteria =
                Criteria.builder()
                    .setTypes(Image::class.java, FloatArray::class.java)
                    .optTranslator(FaceFeatureTranslator())
//                .optProgress(ProgressBar())
//                .optModelUrls(
//                    classLoader.getResource("lib/face_feature.zip").path
//                )
//                .optModelName("face_feature") // specify model file prefix
                    .optModelPath(
                        Paths.get(vggPath)
//                        classLoader.getResource("lib/vggface2.pt")!!.path
//                        ClassPathResource("lib/vggface2.pt").path
//                    "https://github.com/jmformenti/face-recognition-java/raw/master/core/src/main/resources/models/pytorch/vggface2/vggface2.pt"
                    )
//                .optModelName("vggface2") // specify model file prefix
                    .optEngine("PyTorch") // Use PyTorch engine
                    .build()
        } catch (e: Exception) {
            logger.log(
                Level.WARNING,
                "Could not build criteria for prediction. ${e.message}"
            )
        }

        if (criteria == null) {
            return null
        } else {
            try {
                criteria.loadModel().use { model ->
                    val predictor: Predictor<Image, FloatArray> = model.newPredictor()
                    return predictor.predict(img)
                }
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Could not predict for prediction. ${e.message}")
                return null
            }
        }
    }

    @Throws(IOException::class, ModelException::class, TranslateException::class)
    fun detect(img: Image): DetectedObjects? {
        val confThresh = 0.85
        val nmsThresh = 0.45
        val variance = doubleArrayOf(0.1, 0.2)
        val topK = 5000
        val scales = arrayOf(intArrayOf(16, 32), intArrayOf(64, 128), intArrayOf(256, 512))
        val steps = intArrayOf(8, 16, 32)
        val translator =
            FaceDetectionTranslator(confThresh, nmsThresh, variance, topK, scales, steps)

        var criteria: Criteria<Image, DetectedObjects>? = null

        val tempFilePath = System.getProperty("java.io.tmpdir") + "/retinaface.pt"
        val tempFile = File(tempFilePath)
        if (!tempFile.exists()) {
            // Use inputStream to get file
            val retinaStream = ClassPathResource("lib/retinaface.pt").inputStream
            tempFile.deleteOnExit()
            org.apache.commons.io.FileUtils.copyInputStreamToFile(retinaStream, tempFile)
        }
        val retinaPath = tempFile.path

        try {
            criteria =
                Criteria.builder()
                    .setTypes(Image::class.java, DetectedObjects::class.java)
                    .optModelPath(
                        Paths.get(retinaPath)
//                        classLoader.getResource("lib/retinaface.pt")!!.path
//                        ClassPathResource("lib/retinaface.pt").path
                    ) // Load model from local file, e.g:
//                .optModelUrls("https://resources.djl.ai/test-models/pytorch/retinaface.zip")
//                .optModelName("retinaface") // specify model file prefix
                    .optTranslator(translator)
//                .optProgress(ProgressBar())
                    .optEngine("PyTorch") // Use PyTorch engine
                    .build()
        } catch (e: Exception) {
            logger.log(
                Level.WARNING,
                "Could not build criteria for detection. ${e.message}"
            )
        }

        if (criteria == null) {
            return null
        } else {
            try {
                criteria.loadModel().use { model ->
                    model.newPredictor().use { predictor ->
                        val detection = predictor.predict(img)
                        return detection
                    }
                }
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Could not predict for detection. ${e.message}")
                return null
            }
        }
    }

    fun test() {
        // Noah
//        val imageBi1 = ImageIO.read(File("C:/Users/Michael/Documents/shashin/sidecar_dev/thumbnails/z/2024/mar/PXL_20240303_191551227.jpg_225.jpg"))
//        val imageBi1 = ImageIO.read(File("Z:/2024/mar/PXL_20240303_191551227.jpg"))
//        val image1 = Thumbnails.of(imageBi1)
//            .outputQuality(0.5)
//            .imageType(BufferedImage.TYPE_BYTE_GRAY)
//            .height(1000)
//            .asBufferedImage()

//        val img1 = ImageFactory.getInstance().fromImage(image1)
//        val img1 = ImageFactory.getInstance().fromFile(imageFile1)

        // Noah
//        val imageBi2 = ImageIO.read(File("C:/Users/Michael/Documents/shashin/sidecar_dev/thumbnails/c/users/michael/downloads/testpics/PXL_20230519_205237907.jpg_225.jpg"))
//        val imageBi2 = ImageIO.read(File("C:/users/michael/downloads/testpics/PXL_20230519_205237907.jpg"))
//        val image2 = Thumbnails.of(imageBi2)
//            .outputQuality(0.5)
//            .imageType(BufferedImage.TYPE_BYTE_GRAY)
//            .height(1000)
//            .asBufferedImage()
//        val img2 = ImageFactory.getInstance().fromImage(image2)

        // Ryuko
//        val imageBi3 = ImageIO.read(File("C:/Users/Michael/Documents/shashin/sidecar_dev/thumbnails/c/users/michael/downloads/testpics/japan 2018/IMG_20181220_191102.jpg_225.jpg"))
        val imageBi3 = ImageIO.read(File("C:/users/michael/downloads/testpics/japan 2018/IMG_20181220_191102.jpg"))
        val image3 = Thumbnails.of(imageBi3)
            .outputQuality(0.5)
            .imageType(BufferedImage.TYPE_BYTE_GRAY)
            .height(1000)
            .asBufferedImage()
        val img3 = ImageFactory.getInstance().fromImage(image3)

        // Mike
//        val imageBi4 = ImageIO.read(File("C:/Users/Michael/Documents/shashin/sidecar_dev/thumbnails/c/users/michael/downloads/testpics/PXL_20231203_001440894.MP.jpg_225.jpg"))
//        val imageBi4 = ImageIO.read(File("C:/users/michael/downloads/testpics/PXL_20231203_001440894.MP.jpg"))
//        val image4 = Thumbnails.of(imageBi4)
//            .outputQuality(0.5)
//            .imageType(BufferedImage.TYPE_BYTE_GRAY)
//            .height(1000)
//            .asBufferedImage()
//        val img4 = ImageFactory.getInstance().fromImage(image4)

        // Mike, Ryuko, Noah, Dai, Miki, Hitoshi
//        val imageBi5 = ImageIO.read(File("C:/Users/Michael/Documents/shashin/sidecar_dev/thumbnails/c/users/michael/downloads/testpics/japan 2018/IMG_20181230_113407.jpg_225.jpg"))
//        val imageBi5 = ImageIO.read(File("C:/users/michael/downloads/testpics/japan 2018/IMG_20181230_113407.jpg"))
//        val image5 = Thumbnails.of(imageBi5)
//            .outputQuality(0.5)
//            .imageType(BufferedImage.TYPE_BYTE_GRAY)
//            .height(1000)
//            .asBufferedImage()
//        val img5 = ImageFactory.getInstance().fromImage(image5)

        // Noah, Ryuko
//        val imageBi6 = ImageIO.read(File("C:/Users/Michael/Documents/shashin/sidecar_dev/thumbnails/c/users/michael/downloads/testpics/DSC06100.JPG_225.jpg"))
        val imageBi6 = ImageIO.read(File("C:/Users/Michael/Downloads/testpics/japan 2018/IMG_20181219_113155.jpg"))
        val image6 = Thumbnails.of(imageBi6)
            .outputQuality(0.5)
            .imageType(BufferedImage.TYPE_BYTE_GRAY)
            .height(1000)
            .asBufferedImage()
        val img6 = ImageFactory.getInstance().fromImage(image6)

//        val feature1: FloatArray = predict(img1)
//        val feature2: FloatArray = predict(img2)
//        val feature3: FloatArray = predict(img3)
//        val feature4: FloatArray = predict(img4)
//        val feature5: FloatArray = predict(img5)
//        val feature6: FloatArray = predict(img6)


        val mapper = ObjectMapper()

        val detect3 = detect(img3)
        val numOfObjects3 = detect3?.numberOfObjects
        val imageWidth3 = image3.width
        val imageHeight3 = image3.height
        val jsonNode3 = mapper.readTree(detect3?.toJson())
        println(detect3?.toJson())

        val detect6 = detect(img6)
        val numOfObjects6 = detect6?.numberOfObjects
        val imageWidth6 = image6.width
        val imageHeight6 = image6.height
        val jsonNode6 = mapper.readTree(detect6?.toJson())
        println(detect6?.toJson())

        // 1st photo - find faces
        println("i: $numOfObjects6")
        println("j: $numOfObjects3")
        for (i in 0 until numOfObjects6!!) {
            var cornerMin = jsonNode6.get(i).get("boundingBox").get("corners")[0]
            var xMin = cornerMin.get("x").toString().toDouble()
            var yMin = cornerMin.get("y").toString().toDouble()
            var cornerMax = jsonNode6.get(i).get("boundingBox").get("corners")[2]
            var xMax = cornerMax.get("x").toString().toDouble()
            var yMax = cornerMax.get("y").toString().toDouble()

            var y1 = yMin * imageHeight6
            var x1 = xMin * imageWidth6
            var y2 = yMax * imageHeight6
            var x2 = xMax * imageWidth6

            // Temp image file with face in subimage
            val bi6 = image6.getSubimage(x1.toInt(), y1.toInt(), (x2-x1).toInt(), (y2-y1).toInt())
            var tempFilePath = "C:\\Users\\Michael\\Downloads\\outputfolder\\temp-i-$i.jpg"
            ImageIO.write(bi6, "jpg", File(tempFilePath))
            val tempImage6 = ImageFactory.getInstance().fromImage(bi6)

            // Compare with other profiles
            println("testImage predict")
            val testImage = ImageFactory.getInstance().fromImage(image6)
            predict(testImage)
            println("tempImage6 predict")
            val tempFeature6 = predict(tempImage6)

            // 2nd photo - find faces
            for (j in 0 until numOfObjects3!!) {
                cornerMin = jsonNode3.get(j).get("boundingBox").get("corners")[0]
                xMin = cornerMin.get("x").toString().toDouble()
                yMin = cornerMin.get("y").toString().toDouble()
                cornerMax = jsonNode3.get(j).get("boundingBox").get("corners")[2]
                xMax = cornerMax.get("x").toString().toDouble()
                yMax = cornerMax.get("y").toString().toDouble()

                y1 = yMin * imageHeight3
                x1 = xMin * imageWidth3
                y2 = yMax * imageHeight3
                x2 = xMax * imageWidth3

                // Temp image file with face in subimage
                val bi3 = image3.getSubimage(x1.toInt(), y1.toInt(), (x2-x1).toInt(), (y2-y1).toInt())
                tempFilePath = "C:\\Users\\Michael\\Downloads\\outputfolder\\temp-j-$j.jpg"
                ImageIO.write(bi3, "jpg", File(tempFilePath))
                val tempImage3 = ImageFactory.getInstance().fromImage(bi3)

                // Compare with other profiles
                val tempFeature3 = predict(tempImage3)

                println("testing iteration $i-$j: "+calculateSimilar(tempFeature3!!, tempFeature6!!).toString())
            }

        }
    }

    class FaceFeatureTranslator :
        Translator<Image?, FloatArray?> {

        override fun processInput(ctx: TranslatorContext, input: Image?): NDList {
            val array = input?.toNDArray(ctx.ndManager, Image.Flag.GRAYSCALE)
            val pipeline = Pipeline()
            pipeline
                .add(Resize(100))
                .add(ToTensor())
                .add(
                    Normalize(
                        floatArrayOf(127.5f / 255.0f, 127.5f / 255.0f, 127.5f / 255.0f),
                        floatArrayOf(
                            128.0f / 255.0f, 128.0f / 255.0f, 128.0f / 255.0f
                        )
                    )
                )

            return pipeline.transform(NDList(array))
        }

        /** {@inheritDoc}  */
        override fun processOutput(ctx: TranslatorContext, list: NDList): FloatArray {
            val result = NDList()
            val numOutputs = list.singletonOrThrow().shape[0]
            for (i in 0 until numOutputs) {
                result.add(list.singletonOrThrow()[i])
            }
            val embeddings = result.map { it.toFloatArray() }.toTypedArray()
            return FloatArray(embeddings.size) { embeddings[it][0] }
        }
    }
}