package com.miyagi.shashin.component

import com.miyagi.shashin.ShashinApplication
import com.miyagi.shashin.controller.SettingsController
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.TrainingData
import com.miyagi.shashin.util.FileUtils
import org.bytedeco.javacpp.DoublePointer
import org.bytedeco.javacpp.IntPointer
import org.bytedeco.opencv.global.opencv_core
import org.bytedeco.opencv.global.opencv_imgcodecs
import org.bytedeco.opencv.global.opencv_imgproc
import org.bytedeco.opencv.opencv_core.*
import org.bytedeco.opencv.opencv_face.FisherFaceRecognizer
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier
import org.springframework.stereotype.Component
import java.io.File
import java.net.URL
import java.nio.IntBuffer
import java.util.logging.Level
import java.util.logging.Logger

@Component
class FaceRecognizer() {
    private var testImages: MutableIterable<Metadata>? = null
    private var trainingData: MutableIterable<TrainingData>? = null
    private var cascadeDir: String = "lib/cascades"
    private lateinit var cascadeFileList: MutableList<String>
    private var logger: Logger = Logger.getLogger(FaceRecognizer::class.simpleName)
    private val threadExtensionName: String = "facescan_shashinscan"
    private val imageSize: Int = 300

    internal constructor(testImages: MutableIterable<Metadata>, trainingData: MutableIterable<TrainingData>) : this() {
        this.trainingData = trainingData
        this.testImages = testImages
        this.cascadeFileList = mutableListOf()
        loadCascadeData()
    }

    private fun loadCascadeData() {
        val classLoader: ClassLoader = ShashinApplication::class.java.classLoader
        val cascadeDirPath: URL? = classLoader.getResource(this.cascadeDir)
        val dir = File(cascadeDirPath?.file!!)

        val directoryListing: Array<File>? = dir.listFiles()
        if (directoryListing != null) {
            for (child in directoryListing) {
                if (child.isFile && child.extension == "xml") {
                    logger.log(Level.INFO, "Cascade file loaded: " + child.name)
                    this.cascadeFileList.add(child.absolutePath)
                }
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
                    getPrediction()
                }
            }.start()
        }
    }

    private fun getPrediction() {
        val matchMap = mutableMapOf<String, Any?>()
        var totalCount = 0

        if (this.testImages != null && this.testImages!!.count() > 0) {
            logger.log(Level.INFO, "Starting face matching.")

            for (testImage in this.testImages!!) {
                matchMap["label"] = ""
                matchMap["confidence"] = 0.0

                if (this.cascadeFileList.isNotEmpty()) {
                    for (cascadeFile in this.cascadeFileList) {
                        // Load cascade file
                        var faceDetector = CascadeClassifier()
                        faceDetector.load(cascadeFile)

                        val testimage: Mat = opencv_imgcodecs.imread(testImage.getPath(), opencv_imgcodecs.IMREAD_GRAYSCALE)
                        val testimageFaceDetections = RectVector()
                        faceDetector.detectMultiScale(testimage, testimageFaceDetections)

                        logger.log(Level.INFO, "Detected "+testimageFaceDetections.size()+" faces for "+testImage.getPath())

                        if (testimageFaceDetections.size() > 0) {

                            val faceMap = mutableMapOf<Mat,Int>()
                            if (this.trainingData != null && this.trainingData!!.count() > 0) {
                                for (trainingImage in this.trainingData!!) {
                                    val label = trainingImage.getRecognitionLabelId()!!
                                    logger.log(Level.INFO, "Training Image processed. Label: "+label+" - image: "+trainingImage.getPath())

                                    val image: Mat =
                                        opencv_imgcodecs.imread(trainingImage.getPath(), opencv_imgcodecs.IMREAD_GRAYSCALE)

                                    // Detect faces on training image and loop through each one
                                    val faceDetections = RectVector()
                                    faceDetector.detectMultiScale(image, faceDetections)
                                    var rectCrop: Rect? = null
                                    for (i in 0 until faceDetections.size()) {
                                        // Crop and resize
                                        var rect: Rect = faceDetections.get(i)
                                        opencv_imgproc.rectangle(
                                            image,
                                            Point(rect.x(), rect.y()),
                                            Point(rect.x() + rect.width(), rect.y() + rect.height()),
                                            Scalar(0.0, 255.0)
                                        )
                                        rectCrop = Rect(rect.x(), rect.y(), rect.width(), rect.height())
                                        val imageRoi = Mat(image, rectCrop)

                                        val resizeimage = Mat()
                                        // All images need to be the same size to compare
                                        val sz = Size(imageSize, imageSize)
                                        opencv_imgproc.resize(imageRoi, resizeimage, sz)

                                        // Test output for training data
                                        totalCount++
                                        val testOutput = "C:/Users/micha/Downloads/outputfolder/trainingset/testImage-$totalCount-${testImage.getFileName()}.jpg"
                                        opencv_imgcodecs.imwrite(testOutput, resizeimage)
                                        println("\n"+testOutput)

                                        // Save in map
                                        faceMap[resizeimage] = label
                                        resizeimage.release()
                                    }
                                    image.release()
                                }

                                // Loop through training image map and label
                                val images = MatVector(faceMap.size.toLong())
                                val labels = Mat(faceMap.size, 1, opencv_core.CV_32SC1)
                                val labelsBuf: IntBuffer = labels.createBuffer()
                                var counter = 0
                                for ((mat, label) in faceMap) {
                                    images.put(counter.toLong(), mat)
                                    labelsBuf.put(counter, label)
                                    counter++
                                }
                                val faceRecognizer: org.bytedeco.opencv.opencv_face.FaceRecognizer = FisherFaceRecognizer.create()
                                faceRecognizer.train(images, labels)
                                labels.release()
                                val label = IntPointer(1)
                                val confidence = DoublePointer(1)

                                faceDetector = CascadeClassifier()
                                faceDetector.load(cascadeFile)
                                faceDetector.detectMultiScale(testimage, testimageFaceDetections)

                                var rectCrop: Rect? = null
                                for (i in 0 until testimageFaceDetections.size()) {
                                    var rect: Rect = testimageFaceDetections.get(i)
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
                                    val testOutput = "C:/Users/micha/Downloads/outputfolder/trainingset/testImage-$totalCount-${testImage.getFileName()}.jpg"
                                    opencv_imgcodecs.imwrite(testOutput, resizeimage)
                                    println("\n"+testOutput)

                                    imageRoi.release()
                                    resizeimage.release()
                                    faceRecognizer.predict(resizeimage, label, confidence)
                                    val predictedLabel = label[0]

                                    logger.log(Level.INFO, "Predicted label: " + predictedLabel + " Distance :"+ confidence[0]/1000)
                                    if ((confidence[0]/1000) > matchMap["confidence"].toString().toFloat()) {
                                        matchMap["confidence"] = (confidence[0]/1000)
                                        matchMap["label"] = predictedLabel
                                    }
                                }
                            }
                        } else {
                            matchMap["label"] = ""
                            matchMap["confidence"] = -1.0
                        }

                        testimage.release()
                    }
                }

                logger.log(Level.INFO, "Final outcome - Label: "+matchMap["label"]+" - Confidence: "+matchMap["confidence"]+". For " +testImage.getPath())
            }
        }

        logger.log(Level.INFO, "Completed face matching.")
    }
}