package com.miyagi.shashin

import java.util.*

class FileUtils {
    companion object {
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
    }
}