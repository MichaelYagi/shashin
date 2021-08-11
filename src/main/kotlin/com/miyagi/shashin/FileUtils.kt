package com.miyagi.shashin

import java.io.File
import java.io.IOException
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

class FileUtils {
    companion object {
        private var logger: Logger = Logger.getLogger(FileUtils::class.simpleName)

        fun allowableImageFiles(): Array<String> {
            return arrayOf("jpeg","jpg","tiff","png","bmp","gif","webm","ico","nef","cr2","orf","arw","rw2","rwl","srw","mp4","wav","avi")
        }

        fun isRaw(extension: String): Boolean {
            val rawFormats = arrayOf("nef","cr2","orf","arw","rw2","rwl","srw")
            if (rawFormats.contains(extension.lowercase())) {
                return true
            }
            return false
        }

        fun createFile(filePath: String, fileName: String, type: String): File? {
            try {
                // Create directory if dne
                val someFileDir = File(filePath)
                if (!someFileDir.exists()) {
                    someFileDir.mkdirs()
                }
                // Create file
                val someFile = File(fileName)
                if (someFile.createNewFile()) {
                    logger.log(Level.INFO, type + " created: " + someFile.name)
                    return someFile
                } else {
                    logger.log(Level.INFO, type + " already exists: " + someFile.name)
                    return someFile
                }
            } catch (e: IOException) {
                logger.log(Level.SEVERE, type + " creation error: " + e.message)
                return null
            }
        }
    }
}