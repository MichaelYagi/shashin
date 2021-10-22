package com.miyagi.shashin.component

import com.miyagi.shashin.ShashinApplication
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.Notification
import com.miyagi.shashin.model.RecognitionLabelPhoto
import com.miyagi.shashin.model.TrainingData
import com.miyagi.shashin.repository.NotificationRepository
import com.miyagi.shashin.repository.RecognitionLabelPhotoRepository
import com.miyagi.shashin.repository.RecognitionLabelRepository
import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.TextUtils
import org.bytedeco.javacpp.DoublePointer
import org.bytedeco.javacpp.IntPointer
import org.bytedeco.opencv.global.opencv_core
import org.bytedeco.opencv.global.opencv_imgcodecs
import org.bytedeco.opencv.global.opencv_imgproc
import org.bytedeco.opencv.opencv_core.*
import org.bytedeco.opencv.opencv_face.FisherFaceRecognizer
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.tensorflow.Graph
import org.tensorflow.Session
import org.tensorflow.Tensor
import org.tensorflow.Tensors
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.*
import java.nio.IntBuffer
import java.util.ArrayList
import java.util.Arrays
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.sqrt


@Component
class FaceRecognizer() {
    private var testImages: MutableIterable<Metadata>? = null
    private var trainingData: MutableIterable<TrainingData>? = null
    private var recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository? = null
    private var notificationRepository: NotificationRepository? = null
    private var recognitionLabelRepository: RecognitionLabelRepository? = null
    private var userRepository: UserRepository? = null
    private var adminRole: String? = null
    private var cascadeDir: String = "lib/cascades"
    private var modelDir: String = "lib/models"
    private lateinit var cascadeFileList: MutableList<String>
    private var logger: Logger = Logger.getLogger(FaceRecognizer::class.simpleName)
    private val threadExtensionName: String = "facescan_shashinscan"
    private val imageSize: Int = 170
    private val graph: Graph = Graph()
    private val fullFaceFeaturesList = ArrayList<FullFaceFeatures>()
    private var distanceThreshold: Double = 0.6

    internal constructor(testImages: MutableIterable<Metadata>, trainingData: MutableIterable<TrainingData>, recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository?, recognitionLabelRepository: RecognitionLabelRepository?, notificationRepository: NotificationRepository?, userRepository: UserRepository?, adminRole: String?, distanceThreshold: Double) : this() {
        this.trainingData = trainingData
        this.testImages = testImages
        this.recognitionLabelPhotoRepository = recognitionLabelPhotoRepository
        this.recognitionLabelRepository = recognitionLabelRepository
        this.notificationRepository = notificationRepository
        this.userRepository = userRepository
        this.adminRole = adminRole
        this.cascadeFileList = mutableListOf()
        this.graph.importGraphDef(loadGraphDef())
        this.distanceThreshold = distanceThreshold
        loadCascadeData()
    }

    private fun loadCascadeData() {
        val classLoader: ClassLoader = ShashinApplication::class.java.classLoader
        val fileListing: MutableList<File> = mutableListOf()

        var cascadeFileStream = classLoader.getResourceAsStream(this.cascadeDir+"/haarcascade_frontalface_alt.xml")
        var tempFilePath = System.getProperty("java.io.tmpdir")+"/haarcascade_frontalface_alt.xml"
        var tempFile = File(tempFilePath)
        copyInputStreamToFile(cascadeFileStream!!,tempFile)
        fileListing.add(tempFile)

        cascadeFileStream = classLoader.getResourceAsStream(this.cascadeDir+"/haarcascade_frontalface_alt2.xml")
        tempFilePath = System.getProperty("java.io.tmpdir")+"/haarcascade_frontalface_default.xml"
        tempFile = File(tempFilePath)
        copyInputStreamToFile(cascadeFileStream!!,tempFile)
        fileListing.add(tempFile)

        for (child in fileListing) {
            logger.log(Level.INFO, "Cascade file loaded: " + child.name)
            this.cascadeFileList.add(child.absolutePath)
        }
    }

    @Throws(IOException::class)
    private fun copyInputStreamToFile(inputStream: InputStream, file: File) {

        // append = false
        FileOutputStream(file, false).use { outputStream ->
            var read: Int
            val bytes = ByteArray(DEFAULT_BUFFER_SIZE)
            while (inputStream.read(bytes).also { read = it } != -1) {
                outputStream.write(bytes, 0, read)
            }
        }
    }

    fun runRecognizer() {
        if (!FileUtils.checkThreadFileAlive(threadExtensionName)) {

            // Clean up any existing thread files
            FileUtils.deleteThreadFiles(threadExtensionName)

            Thread {
                val tempDir = System.getProperty("java.io.tmpdir")
                val threadFile = FileUtils.createFile(tempDir, tempDir + "/" + Thread.currentThread().name + ".facescan_shashinscan", "Thread")
                if (threadFile != null) {
                    getPrediction(threadFile)
                }
            }.start()
        }
    }

    private fun getPrediction(threadFile: File) {
        val matchMap = mutableMapOf<Int, Any?>()
        var totalCount = 0

        // Load training data
//        val faceMap = mutableMapOf<Mat,Int>()
//        val trainingDataMap = mutableMapOf<String,MutableMap<Mat,Int>>()

        // Process each training image through different cascades and load into map
        if (this.cascadeFileList.isNotEmpty()) {
            for (cascadeFile in this.cascadeFileList) {
                if (this.trainingData != null && this.trainingData!!.count() > 0) {
                    for (trainingImage in this.trainingData!!) {
                        var path = trainingImage.getPath()
                        if (trainingImage.getType()?.contains("video") == true) {
                            path = trainingImage.getThumbnailPathSmall()
                        }
                        val label = trainingImage.getRecognitionLabelId()!!
                        writeToThreadFileAndLogMessage("Training Image processed. Label: $label - image: $path using $cascadeFile",threadFile)
                        val image: Mat =
                            opencv_imgcodecs.imread(path, opencv_imgcodecs.IMREAD_COLOR) //opencv_imgcodecs.IMREAD_GRAYSCALE

                        // Detect faces on training image and loop through each one
                        val faceDetectorTrainingData = CascadeClassifier()
                        faceDetectorTrainingData.load(cascadeFile)
                        val faceDetections = RectVector()
                        faceDetectorTrainingData.detectMultiScale(image, faceDetections)
                        faceDetectorTrainingData.close()

                        // Crop image into square
                        var rectCrop: Rect?
                        for (i in 0 until faceDetections.size()) {
                            // Crop and resize
                            val rect: Rect = faceDetections.get(i)
                            opencv_imgproc.rectangle(
                                image,
                                Point(rect.x(), rect.y()),
                                Point(rect.x() + rect.width(), rect.y() + rect.height()),
                                Scalar(0.0, 255.0)
                            )
                            rectCrop = Rect(rect.x(), rect.y(), rect.width(), rect.height())
                            val imageRoi = Mat(image, rectCrop)

                            val resizetrainingimage = Mat()
                            // All images need to be the same size to compare
                            val sz = Size(imageSize, imageSize)
                            opencv_imgproc.resize(imageRoi, resizetrainingimage, sz)

                            // Test output for training data
                            totalCount++

                            // Test save
//                            val testOutput = "C:/Users/micha/Downloads/outputfolder/trainingset/testImage-$totalCount-${trainingImage.getMetadataId()}.jpg"
//                            opencv_imgcodecs.imwrite(testOutput, resizetrainingimage)

                            val tempFilePath = System.getProperty("java.io.tmpdir")+"/temp.jpg"
                            opencv_imgcodecs.imwrite(tempFilePath, resizetrainingimage)
                            val tempImg = ImageIO.read(File(tempFilePath))
                            val faceFeatures = passImageThroughNeuralNetwork(tempImg, 1, graph)
                            val fullFaceFeatures = FullFaceFeatures()
                            fullFaceFeatures.setIdentifier(label)
                            fullFaceFeatures.setFaceFeatures(1,faceFeatures)
//                        val tempFile = createFile("C:/Users/micha/Downloads/outputfolder/trainingset/","C:/Users/micha/Downloads/outputfolder/trainingset/testImage-$label-$i-${image.name}-$counter.jpg", "Thumbnail")
//                        ImageIO.write(tempImg, "jpg", tempFile)
                            fullFaceFeaturesList.add(fullFaceFeatures)

                            // Save in map
//                            faceMap[resizetrainingimage] = label
//                            trainingDataMap[cascadeFile] = faceMap

                        }
                        image.release()
                    }
                }
            }
        }

        // Process test images through each cascade and run against training data
        if (this.testImages != null && this.testImages!!.count() > 0) {
            writeToThreadFileAndLogMessage("Starting face matching.",threadFile)
            var found = false

            for (testImage in this.testImages!!) {
                matchMap[0] = 0.0
                var path = testImage.getPath()
                if (testImage.getType()?.contains("video") == true) {
                    path = testImage.getThumbnailPathSmall()
                }
                writeToThreadFileAndLogMessage("matching against "+path!!,threadFile)

                if (this.cascadeFileList.isNotEmpty()) {
                    for (cascadeFile in this.cascadeFileList) {
                        val testimage: Mat = opencv_imgcodecs.imread(path, opencv_imgcodecs.IMREAD_COLOR)

                        // Load cascade file
                        val faceDetector = CascadeClassifier()
                        faceDetector.load(cascadeFile)

                        val testimageFaceDetections = RectVector()
                        faceDetector.detectMultiScale(testimage, testimageFaceDetections)

                        logger.log(Level.INFO, "Detected "+testimageFaceDetections.size()+" faces for "+testImage.getPath()+" using "+cascadeFile)
                        faceDetector.close()

                        if (testimageFaceDetections.size() > 0) {

                            if (this.trainingData != null && this.trainingData!!.count() > 0) {
                                // Loop through training image map and label
//                                val images = MatVector(faceMap.size.toLong())
//                                val labels = Mat(faceMap.size, 1, opencv_core.CV_32SC1)
//                                val labelsBuf: IntBuffer = labels.createBuffer()
//                                var counter = 0
//                                for ((mat, label) in trainingDataMap[cascadeFile].orEmpty()) {
//                                    images.put(counter.toLong(), mat)
//                                    labelsBuf.put(counter, label)
//                                    counter++
//                                }
//                                val faceRecognizer: org.bytedeco.opencv.opencv_face.FaceRecognizer = FisherFaceRecognizer.create()
//                                faceRecognizer.train(images, labels)
//                                labels.release()
//                                images.clear()
//
//                                val label = IntPointer(1)
//                                val confidence = DoublePointer(1)

                                val faceDetectorTestImage = CascadeClassifier()
                                faceDetectorTestImage.load(cascadeFile)
                                faceDetectorTestImage.detectMultiScale(testimage, testimageFaceDetections)
                                faceDetectorTestImage.close()

                                val predictions = arrayOfNulls<Prediction>(testimageFaceDetections.size().toInt())

                                // Loop through each detected face in image and crop image into square
                                var rectCrop: Rect?
                                for (i in 0 until testimageFaceDetections.size()) {
                                    val rect: Rect = testimageFaceDetections.get(i)
                                    opencv_imgproc.rectangle(
                                        testimage,
                                        Point(rect.x(), rect.y()),
                                        Point(rect.x() + rect.width(), rect.y() + rect.height()),
                                        Scalar(0.0, 255.0)
                                    )
                                    rectCrop = Rect(rect.x(), rect.y(), rect.width(), rect.height())
                                    val imageRoi = Mat(testimage, rectCrop)

                                    val resizeimage = Mat()
                                    val sz = Size(imageSize, imageSize)
                                    opencv_imgproc.resize(imageRoi, resizeimage, sz)

                                    // Test output for test data
                                    totalCount++

                                    imageRoi.release()

                                    // Execute prediction
//                                    faceRecognizer.predict(resizeimage, label, confidence)
//                                    val predictedLabel = label[0]

                                    // Test save
//                                    val testOutput = "C:/Users/micha/Downloads/outputfolder/testimage/testImage-$predictedLabel-${confidence[0]/1000}-${testImage.getFileName()}.jpg"
//                                    opencv_imgcodecs.imwrite(testOutput, resizeimage)

                                    // Discriminate as much as possible and pick least confident match
//                                    logger.log(Level.INFO, "Predicted label: "+predictedLabel+" Distance :"+confidence[0]/1000+" for "+testImage.getFileName()+" using "+cascadeFile)
//                                    if (!matchMap.containsKey(predictedLabel) || (matchMap.containsKey(predictedLabel) && ((confidence[0]/1000) > matchMap[predictedLabel].toString().toFloat()))) {
//                                        matchMap[predictedLabel] = (confidence[0]/1000)
//                                    }

                                    val tempFilePath = System.getProperty("java.io.tmpdir")+"/temp.jpg"
                                    opencv_imgcodecs.imwrite(tempFilePath, resizeimage)
                                    val tempImg = ImageIO.read(File(tempFilePath))
                                    val faceFeatures = passImageThroughNeuralNetwork(tempImg, 1, graph)
                                    predictions[i.toInt()] = predictBestMatchFromPool(faceFeatures!!,
                                        fullFaceFeaturesList.toTypedArray()
                                    )

                                    var candidate: Prediction? = null
                                    try {
                                        candidate = predictions.maxByOrNull { it!!.getPercentage() }
                                    } catch (e: Exception) {}
                                    if (candidate != null && candidate.isIdentified) {
                                        logger.log(Level.INFO, "Predicted label: " + candidate.identifier + " Distance :"+ candidate.distance+" Percentage :"+candidate.percentage+" for "+testImage.getFileName()+" using "+cascadeFile)
                                        matchMap[candidate.identifier] = candidate.distance
                                    }
                                }
                            }
                        } else {
                            matchMap[0] = -1.0
                            val objectLabel = this.recognitionLabelRepository?.findByNameIgnoreCase("object")
                            if (objectLabel != null) {
                                val recognitionLabelPhoto = RecognitionLabelPhoto()
                                recognitionLabelPhoto.setMetadataId(testImage.getId())
                                recognitionLabelPhoto.setRecognitionLabelId(objectLabel.getId())
                                recognitionLabelPhoto.setConfidence("-0.1")
                                recognitionLabelPhoto.setAutoTagged(true)
                                this.recognitionLabelPhotoRepository?.save(recognitionLabelPhoto)
                            }
                        }

                        testimage.release()

//                        if (++totalCount % 200 == 0) {
//                            System.gc();
//                        }
                    }
                }



                for ((labelId, confidence) in matchMap) {
                    if (labelId != 0) {
                        found = true
                        val confidenceVal: String = "%.1f".format(confidence)
                        val message =
                            "Final outcome - Label: $labelId - Confidence: $confidence for $path"
                        writeToThreadFileAndLogMessage(message, threadFile)

                        // Save record
                        val recordCount = this.recognitionLabelPhotoRepository?.countByRecognitionLabelIdAndMetadataId(labelId,testImage.getId())
                        if (recordCount == 0) {
                            val recognitionLabelPhoto = RecognitionLabelPhoto()
                            recognitionLabelPhoto.setMetadataId(testImage.getId())
                            recognitionLabelPhoto.setRecognitionLabelId(labelId)
                            recognitionLabelPhoto.setConfidence(confidenceVal)
                            recognitionLabelPhoto.setAutoTagged(true)
                            this.recognitionLabelPhotoRepository?.save(recognitionLabelPhoto)
                        } else {
                            val recognitionLabelPhoto = this.recognitionLabelPhotoRepository?.findByRecognitionLabelIdAndMetadataId(labelId,testImage.getId())
                            if (recognitionLabelPhoto != null) {
                                recognitionLabelPhoto.setConfidence(confidenceVal)
                                recognitionLabelPhoto.setAutoTagged(true)
                                this.recognitionLabelPhotoRepository?.save(recognitionLabelPhoto)
                            }
                        }
                    }
                }
                matchMap.clear()
            }

            val admins = userRepository?.findAllByAuthorityEquals(adminRole!!)
            if (found && admins != null) {
                val notificationObjList = mutableListOf<Notification>()
                for (admin in admins) {
                    val notificationObj = Notification()
                    notificationObj.setUserId(admin.getId())
                    notificationObj.setCreatedAt(TextUtils.getModifiedCreateTimestamp())
                    notificationObj.setModifiedAt(TextUtils.getModifiedCreateTimestamp())
                    notificationObj.setRead(false)
                    notificationObj.setMessage("Matches to people found!")
                    notificationObjList.add(notificationObj)
                }
                if (notificationObjList.isNotEmpty()) {
                    notificationRepository?.saveAll(notificationObjList)
                }
            }
        }

        writeToThreadFileAndLogMessage("Matching Complete",threadFile)
    }

    private fun writeToThreadFileAndLogMessage(message: String, threadFile: File) {
        try {
            val writer = BufferedWriter(FileWriter(threadFile))
            writer.write(message)
            writer.close()
        } catch(e: Exception) {
            logger.log(Level.WARNING, "Could not write to thread file: " + threadFile.name)
        }
        logger.log(Level.INFO, message)
    }

    final fun loadGraphDef(): ByteArray {
        val classLoader: ClassLoader = ShashinApplication::class.java.classLoader
        val modelFileStream = classLoader.getResourceAsStream(this.modelDir+"/model_face_recognition.pb")
        val tempFilePath = System.getProperty("java.io.tmpdir")+"/model_face_recognition.pb"
        val tempFile = File(tempFilePath)
        copyInputStreamToFile(modelFileStream!!,tempFile)
        return tempFile.readBytes()
    }

    private fun passImageThroughNeuralNetwork(image: BufferedImage, faceType: Int, graph: Graph): FaceFeatures? {
        var features: FaceFeatures
        Session(graph).use { session ->
            val feedImage: Tensor<Float> = Tensors.create(imageToMultiDimensionalArray(image))
            val response = session.runner()
                .feed("input", feedImage)
                .feed("phase_train", Tensor.create(false))
                .fetch("embeddings")
                .run()[0]
//            .expect(Float::class.java)
            val shape = response.shape()


//        println(shape.joinToString(", "))
            //first dimension should return 1 as for image with normal size
            //second dimension should give 128 characteristics of face
            check(!(shape[0].toInt() != 1 || shape[1].toInt() != 128)) { "illegal output values: 1 = " + shape[0] + " 2 = " + shape[1] }
            val featuresHolder =
                Array(1) { FloatArray(128) }
            response.copyTo(featuresHolder)
            features = FaceFeatures(featuresHolder[0], faceType)
            response.close()
        }
        return features
    }

    private fun imageToMultiDimensionalArray(bi: BufferedImage?): Array<Array<Array<FloatArray>>>? {
        requireNotNull(bi) { "image for neural network is null" }
        val height = bi.height
        val width = bi.width
        val depth = 3
        val image = Array(1) {
            Array(width) {
                Array(height) {
                    FloatArray(depth)
                }
            }
        }
        for (i in 0 until width) {
            for (j in 0 until height) {
                val rgb = bi.getRGB(i, j)
                val color = Color(rgb)
                image[0][i][j][0] = color.red.toFloat()
                image[0][i][j][1] = color.green.toFloat()
                image[0][i][j][2] = color.blue.toFloat()
            }
        }
        return image
    }

    private fun euclidDistance(first: FloatArray, second: FloatArray): Float {
        require(first.size == second.size) { "should be same size" }
        var sum = 0f
        for (i in first.indices) {
            sum += abs(first[i] - second[i])
        }
        return sqrt(sum.toDouble()).toFloat()
    }

    private fun matchTwoFeatureArrays(
        first: FaceFeatures, second: FaceFeatures,
        identifier: Int
    ): Prediction {
        val distance: Float = euclidDistance(first.features, second.features)
//        println("distance with $identifier = $distance")
        val percentageThreshold = 75.0f //70.0f
        val percentage = 100f.coerceAtMost(100 * this.distanceThreshold.toFloat() / distance)
//        println("percentage = $percentage")
//        println("isIdentified = ${percentage >= percentageThreshold}")
//        println("=================================")
        return Prediction(percentage, percentage >= percentageThreshold, identifier,distance)
    }

    private fun predictBestMatchFromPool(
        userToFind: FaceFeatures,
        collectedFeatures: Array<FullFaceFeatures>
    ): Prediction? {
        //find best prediction using euclid distance
        val predictions = arrayOfNulls<Prediction>(collectedFeatures.size)
        val inputFaceType = userToFind.getFaceType()
        for (i in collectedFeatures.indices) {
            predictions[i] = matchTwoFeatureArrays(
                userToFind,
                collectedFeatures[i].getFaceFeatures(inputFaceType)!!,
                collectedFeatures[i].getIdentifier()
            )
        }
        return Arrays.stream(predictions).max { first, second ->
            first!!.percentage.compareTo(second!!.percentage)
        }.orElse(null)
    }
}