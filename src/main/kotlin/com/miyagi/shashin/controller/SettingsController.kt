package com.miyagi.shashin.controller

import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.ImageProcessingUtils
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger


@Controller
class SettingsController {

    @Value("\${app.sidecar.path}")
    private var relativeSidecarDir: String? = null
    @Value("\${app.api.version}")
    private var apiVersion: String? = null
    @Autowired
    private val metadataRepository: MetadataRepository? = null
    private var logger: Logger = Logger.getLogger(ImageProcessingUtils::class.simpleName)

    // TODO: Make these configurable
    val photoDir = "c:/Users/micha/Downloads/testData/"
    val rootPhotoDir = "c:/Users/micha/Downloads/testData/"


    @GetMapping("/settings")
    fun getIndex(model: Model): String {
        val module = "settings"
        model["data"] = "This is the settings page"
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @GetMapping("/settings/scan")
    fun getScan(model: Model): String {
        val module = "scan"
        model["data"] = "Scan photos"
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RequestMapping(value = ["/settings/scan"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    fun postScan(@RequestParam submit: String): String {
        if (submit == "Scan") {
            if (!checkThreadFileAlive()) {
                // Clean up any existing thread files
                deleteThreadFiles()

                val rootPath = FileSystemResource("").file.absolutePath
                val sidecarDir = rootPath + relativeSidecarDir

                // Iterate through directory in another thread
                Thread {
                    //Create file with thread name and write file name iterated
                    val tempDir = System.getProperty("java.io.tmpdir")
                    val threadFile = FileUtils.createFile(tempDir, tempDir + "/" + Thread.currentThread().name + ".shashinscan", "Thread")
                    if (threadFile != null) {
                        getFile(photoDir, threadFile, sidecarDir, rootPhotoDir)
                        // Delete thread file
                        if (threadFile.delete()) {
                            logger.log(Level.FINE, "Thread file deleted: " + threadFile.name)
                        } else {
                            logger.log(Level.SEVERE, "Could not delete thread file: " + threadFile.name)
                        }
                    }
                }.start()
                return "{\"msg\":\"Scan start\"}"
            }
            return "{\"msg\":\"Scan in progress\"}"
        }
        return "{\"msg\":\"Error\"}"
    }

    private fun threadIsAlive(threadName: String): Boolean {
        for (t in Thread.getAllStackTraces().keys) {
            if (t.name == threadName) {
                return t.isAlive
            }
        }
        return false
    }

    private fun checkThreadFileAlive(): Boolean {
        val tempDir = System.getProperty("java.io.tmpdir")
        val f = File(tempDir)
        val files = f.listFiles()
        if (files != null) {
            for (i in files.indices) {
                val file: File = files[i]

                if (file.isFile &&
                    file.extension.lowercase() == "shashinscan" &&
                    threadIsAlive(file.nameWithoutExtension)
                ) {
                    return true
                }
            }
        }

        return false
    }

    private fun deleteThreadFiles() {
        val tempDir = System.getProperty("java.io.tmpdir")
        val f = File(tempDir)
        val files = f.listFiles()
        if (files != null) {
            for (i in files.indices) {
                val file: File = files[i]

                if (file.isFile &&
                    file.extension.lowercase() == "shashinscan"
                ) {
                    file.delete()
                }
            }
        }
    }

    private fun getFile(dirPath: String, threadFile: File, sidecarDir: String, rootDir: String) {
        val f = File(dirPath)
        val files = f.listFiles()
        if (files != null) {
            for (i in files.indices) {

                // TODO: Remove this test
                //Thread.sleep(4000);

                val file: File = files[i]

                if (file.isFile) {
                    val supportedFormats = FileUtils.allowableImageFiles()
                    if (supportedFormats.contains(file.extension.lowercase())) {
                        // TODO: If RAW then convert to jpeg
                        if (FileUtils.isRaw(file.extension.lowercase())) {

                        } else {
                            // TODO: Check if mapped sidecar file exists, if it does, skip creating them
                            if (false) {

                            } else {
                                val imageProcessingUtils = ImageProcessingUtils(apiVersion)
                                val thumbnailFile = imageProcessingUtils.createThumbnails(file, sidecarDir, rootDir)
                                val metadataObj = imageProcessingUtils.createMetadata(file, sidecarDir, rootDir, thumbnailFile)
                                if (metadataObj != null) {
                                    metadataRepository?.save(metadataObj)
                                }
                            }
                        }
                    }
                }

                if (file.isDirectory) {
                    getFile(file.absolutePath, threadFile, sidecarDir, rootDir)
                }
            }
        }
    }
}