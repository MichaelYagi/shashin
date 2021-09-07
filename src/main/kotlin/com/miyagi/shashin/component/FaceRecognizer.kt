package com.miyagi.shashin.component

import com.miyagi.shashin.ShashinApplication
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.RecognitionLabelPhoto
import com.miyagi.shashin.model.TrainingData
import com.miyagi.shashin.repository.RecognitionLabelPhotoRepository
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
import java.io.*
import java.net.URL
import java.nio.IntBuffer
import java.nio.file.Files
import java.util.logging.Level
import java.util.logging.Logger


@Component
class FaceRecognizer() {
    private var testImages: MutableIterable<Metadata>? = null
    private var trainingData: MutableIterable<TrainingData>? = null
    private var recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository? = null
    private var cascadeDir: String = "lib/cascades"
    private lateinit var cascadeFileList: MutableList<String>
    private var logger: Logger = Logger.getLogger(FaceRecognizer::class.simpleName)
    private val threadExtensionName: String = "facescan_shashinscan"
    private val imageSize: Int = 300

    internal constructor(testImages: MutableIterable<Metadata>, trainingData: MutableIterable<TrainingData>, recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository?) : this() {
        this.trainingData = trainingData
        this.testImages = testImages
        this.recognitionLabelPhotoRepository = recognitionLabelPhotoRepository
        this.cascadeFileList = mutableListOf()
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

        cascadeFileStream = classLoader.getResourceAsStream(this.cascadeDir+"/haarcascade_frontalface_default.xml")
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
        val faceMap = mutableMapOf<Mat,Int>()
        val trainingDataMap = mutableMapOf<String,MutableMap<Mat,Int>>()

        // Process each training image through different cascades and load into map
        if (this.cascadeFileList.isNotEmpty()) {
            for (cascadeFile in this.cascadeFileList) {
                if (this.trainingData != null && this.trainingData!!.count() > 0) {
                    for (trainingImage in this.trainingData!!) {
                        val label = trainingImage.getRecognitionLabelId()!!
                        writeToThreadFileAndLogMessage("Training Image processed. Label: "+label+" - image: "+trainingImage.getPath()+" using "+cascadeFile,threadFile)

                        val image: Mat =
                            opencv_imgcodecs.imread(trainingImage.getPath(), opencv_imgcodecs.IMREAD_GRAYSCALE)

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

                            // Save in map
                            faceMap[resizetrainingimage] = label
                            trainingDataMap[cascadeFile] = faceMap
                        }
                        image.release()
                    }
                }
            }
        }

        // Process test images through each cascade and run against training data
        if (this.testImages != null && this.testImages!!.count() > 0) {
            writeToThreadFileAndLogMessage("Starting face matching.",threadFile)

            for (testImage in this.testImages!!) {
                matchMap[0] = 0.0
                writeToThreadFileAndLogMessage("matching against "+testImage.getPath()!!,threadFile)

                if (this.cascadeFileList.isNotEmpty()) {
                    for (cascadeFile in this.cascadeFileList) {
                        // Load cascade file
                        val faceDetector = CascadeClassifier()
                        faceDetector.load(cascadeFile)

                        val testimage: Mat = opencv_imgcodecs.imread(testImage.getPath(), opencv_imgcodecs.IMREAD_GRAYSCALE)
                        val testimageFaceDetections = RectVector()
                        faceDetector.detectMultiScale(testimage, testimageFaceDetections)

                        logger.log(Level.INFO, "Detected "+testimageFaceDetections.size()+" faces for "+testImage.getPath()+" using "+cascadeFile)
                        faceDetector.close()

                        if (testimageFaceDetections.size() > 0) {

                            if (this.trainingData != null && this.trainingData!!.count() > 0) {
                                // Loop through training image map and label
                                val images = MatVector(faceMap.size.toLong())
                                val labels = Mat(faceMap.size, 1, opencv_core.CV_32SC1)
                                val labelsBuf: IntBuffer = labels.createBuffer()
                                var counter = 0
                                for ((mat, label) in trainingDataMap[cascadeFile].orEmpty()) {
                                    images.put(counter.toLong(), mat)
                                    labelsBuf.put(counter, label)
                                    counter++
                                }
                                val faceRecognizer: org.bytedeco.opencv.opencv_face.FaceRecognizer = FisherFaceRecognizer.create()
                                faceRecognizer.train(images, labels)
                                labels.release()
                                images.clear()

                                val label = IntPointer(1)
                                val confidence = DoublePointer(1)

                                val faceDetectorTestImage = CascadeClassifier()
                                faceDetectorTestImage.load(cascadeFile)
                                faceDetectorTestImage.detectMultiScale(testimage, testimageFaceDetections)
                                faceDetectorTestImage.close()

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
//                                    val testOutput = "C:/Users/micha/Downloads/outputfolder/trainingset/testImage-$totalCount-${testImage.getFileName()}.jpg"
//                                    opencv_imgcodecs.imwrite(testOutput, resizeimage)
//                                    println("\n"+testOutput)

                                    imageRoi.release()

                                    // Execute prediction
                                    faceRecognizer.predict(resizeimage, label, confidence)
                                    val predictedLabel = label[0]

                                    // Discriminate as much as possible and pick least confident match
                                    logger.log(Level.INFO, "Predicted label: "+predictedLabel+" Distance :"+confidence[0]/1000+" for "+testImage.getFileName()+" using "+cascadeFile)
                                    if (!matchMap.containsKey(predictedLabel) || (matchMap.containsKey(predictedLabel) && ((confidence[0]/1000) > matchMap[predictedLabel].toString().toFloat()))) {
                                        matchMap[predictedLabel] = (confidence[0]/1000)
                                    }
                                }
                            }
                        } else {
                            matchMap[0] = -1.0
                        }

                        testimage.release()

//                        if (++totalCount % 200 == 0) {
//                            System.gc();
//                        }
                    }
                }

                for ((labelId, confidence) in matchMap) {
                    if (labelId != 0) {
                        val confidenceVal: String = "%.1f".format(confidence)
                        val message =
                            "Final outcome - Label: " + labelId + " - Confidence: " + confidence + " for " + testImage.getPath()
                        writeToThreadFileAndLogMessage(message, threadFile)

                        // Save record
                        val recordCount = this.recognitionLabelPhotoRepository?.countByRecognitionLabelIdAndMetadataId(labelId,testImage.getId())
                        if (recordCount == 0) {
                            val recognitionLabelPhoto = RecognitionLabelPhoto()
                            recognitionLabelPhoto.setMetadataId(testImage.getId())
                            recognitionLabelPhoto.setRecognitionLabelId(labelId)
                            recognitionLabelPhoto.setConfidence(confidenceVal)
                            this.recognitionLabelPhotoRepository?.save(recognitionLabelPhoto)
                        } else {
                            val recognitionLabelPhoto = this.recognitionLabelPhotoRepository?.findByRecognitionLabelIdAndMetadataId(labelId,testImage.getId())
                            if (recognitionLabelPhoto != null) {
                                recognitionLabelPhoto.setConfidence(confidenceVal)
                                this.recognitionLabelPhotoRepository?.save(recognitionLabelPhoto)
                            }
                        }
                    }
                }
                matchMap.clear()
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
}