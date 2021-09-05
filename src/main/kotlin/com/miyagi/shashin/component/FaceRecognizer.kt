package com.miyagi.shashin.component

import com.miyagi.shashin.ShashinApplication
import com.miyagi.shashin.controller.SettingsController
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.TrainingData
import com.miyagi.shashin.util.FileUtils
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier
import org.springframework.stereotype.Component
import java.io.File
import java.net.URL
import java.util.logging.Level
import java.util.logging.Logger

@Component
class FaceRecognizer() {
    private var testImages: MutableIterable<Metadata>? = null
    private var trainingData: MutableIterable<TrainingData>? = null
    private var cascadeDir: String = "lib/haarcascades"
    private lateinit var cascadeDirList: MutableList<String>
    private var logger: Logger = Logger.getLogger(FaceRecognizer::class.simpleName)
    private val threadExtensionName: String = "facescan_shashinscan"

    internal constructor(testImages: MutableIterable<Metadata>, trainingData: MutableIterable<TrainingData>) : this() {
        this.trainingData = trainingData
        this.testImages = testImages
        this.cascadeDirList = mutableListOf()
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
                    this.cascadeDirList.add(child.absolutePath)
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

                    if (this.cascadeDirList.isNotEmpty()) {
                        for (cascadeDir in this.cascadeDirList) {
                            getPrediction()
                        }
                    }
                }
            }.start()
        }
    }

    private fun getPrediction() {
        val faceDetector = CascadeClassifier()

    }
}