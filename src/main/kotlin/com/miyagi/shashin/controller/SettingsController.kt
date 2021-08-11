package com.miyagi.shashin.controller

import com.drew.imaging.ImageMetadataReader
import com.miyagi.shashin.FileUtils
import com.miyagi.shashin.ImageProcessingUtils
import com.miyagi.shashin.TextUtils
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.io.File
import java.io.IOException





@Controller
class SettingsController {
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

            // TODO: Make these configurable
            val dir = "c:/Users/micha/Downloads/testData/"
            val sidecarDir = "c:/Users/micha/Downloads/testData/sidecar/"

            if (!checkThreadFileAlive(dir)) {
                // Clean up any existing thread files
                deleteThreadFiles(dir)

                // Iterate through directory in another thread
                Thread {
                    //Create file with thread name and write file name iterated
                    try {
                        val threadFile = File(dir + "/" + Thread.currentThread().name + ".shashinscan")
                        if (threadFile.createNewFile()) {
                            println("File created: " + threadFile.name)
                        } else {
                            println("File already exists.")
                        }
                        getFile(dir, threadFile, sidecarDir)

                        // Delete thread file
                        if (threadFile.delete()) {
                            println("Deleted the file: " + threadFile.name);
                        } else {
                            println("Failed to delete the file.");
                        }
                    } catch (e: IOException) {
                        println("An error occurred.")
                        e.printStackTrace()
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

    private fun checkThreadFileAlive(dirPath: String): Boolean {
        val f = File(dirPath)
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

    private fun deleteThreadFiles(dirPath: String) {
        val f = File(dirPath)
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

    private fun getFile(dirPath: String, threadFile: File, sidecarDir: String) {
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
                            // TODO: Log and display in web app
                            println(file.name)

                            // TODO: Check if mapped sidecar file exists, if it does, skip creating them
                            if (false) {

                            } else {
                                ImageProcessingUtils.createSidecarData(file, sidecarDir)
                                ImageProcessingUtils.createThumbnails(file, sidecarDir)
                            }
                        }
                        // TODO: Remove output
                        println("---------------------")
                    }
                }

                if (file.isDirectory) {
                    getFile(file.absolutePath, threadFile, sidecarDir)
                }
            }
        }
    }
}