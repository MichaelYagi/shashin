package com.miyagi.shashin.util

import com.miyagi.shashin.model.Folder
import org.springframework.stereotype.Component
import java.io.*
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Component
class FileUtils {
    companion object {
        private var logger: Logger = Logger.getLogger(FileUtils::class.simpleName)

        fun thumbnailHeight(): Int {
            return 225
        }

        fun allowableMediaFiles(): Array<String> {
            return allowableImageFiles() + allowableVideoFiles() + allowableRawImageFiles()
        }

        fun allowableImageFiles(): Array<String> {
            return arrayOf("jpeg","jpg","png","bmp","gif","webm","webp")
        }

        fun allowableAudioFiles(): Array<String> {
            return arrayOf("3gpp","aac","flac","mpeg","mp3","mp4","ogg","wav","webm")
        }

        fun allowableVideoFiles(): Array<String> {
            return arrayOf("mp4","wav","avi","mov")
        }

        fun allowableRawImageFiles(): Array<String> {
            return arrayOf("nef","cr2","orf","arw","rw2","rwl","srw")
        }

        fun isRaw(extension: String): Boolean {
            if (allowableRawImageFiles().contains(extension.lowercase())) {
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
                    //return someFile
                    return null
                }
            } catch (e: IOException) {
                logger.log(Level.SEVERE, type + " creation error: " + e.message)
                return null
            }
        }

        fun threadIsAlive(threadName: String): Boolean {
            for (t in Thread.getAllStackTraces().keys) {
                if (t.name == threadName) {
                    return t.isAlive
                }
            }
            return false
        }

        fun checkThreadFileAlive(extension: String): Boolean {
            val tempDir = System.getProperty("java.io.tmpdir")
            val f = File(tempDir)
            val files = f.listFiles()
            if (files != null) {
                for (i in files.indices) {
                    val file: File = files[i]

                    if (file.isFile &&
                        file.extension.lowercase() == extension &&
                        threadIsAlive(file.nameWithoutExtension)
                    ) {
                        return true
                    }
                }
            }

            return false
        }

        fun deleteThreadFiles(extension: String) {
            val tempDir = System.getProperty("java.io.tmpdir")
            val f = File(tempDir)
            val files = f.listFiles()
            if (files != null) {
                for (i in files.indices) {
                    val file: File = files[i]

                    if (file.isFile &&
                        file.extension.lowercase() == extension
                    ) {
                        logger.log(Level.INFO, "Thread file deleted: " + file.name)
                        file.delete()
                    }
                }
            }
        }

        fun readThreadFile(extension: String): String? {
            val tempDir = System.getProperty("java.io.tmpdir")
            val f = File(tempDir)
            val files = f.listFiles()
            if (files != null) {
                for (i in files.indices) {
                    val file: File = files[i]

                    if (file.isFile &&
                        file.extension.lowercase() == extension &&
                        threadIsAlive(file.nameWithoutExtension)
                    ) {
                        return Files.readString(file.toPath())
                    }
                }
            }

            return null
        }

        fun getRootDir(file: File): String {
            var fileRootDir: String = file.parent.replace('\\', '/').replace(":", "")
                .lowercase()  //.replace(rootDirFilePath.replace('\\', '/').lowercase(), "")
            fileRootDir = fileRootDir.replace('\\', '/')

            if (fileRootDir.last() == '/') {
                fileRootDir = fileRootDir.dropLast(1)
            }

            if (fileRootDir.take(2) == "//") {
                fileRootDir = fileRootDir.drop(1)
            }

            if (fileRootDir.first() != '/' && fileRootDir.first() != '\\') {
                fileRootDir = "/$fileRootDir"
            }

            return fileRootDir
        }

        fun deleteDirectory(directoryToBeDeleted: File): Boolean {
            val allContents = directoryToBeDeleted.listFiles()
            if (allContents != null) {
                for (file in allContents) {
                    deleteDirectory(file)
                }
            }
            return directoryToBeDeleted.delete()
        }

        /**
         * Zips a Folder to "[Folder].zip"
         * @param toZipFolder Folder to be zipped
         * @return the resulting ZipFile
         */
        fun zipFolder(toZipFolder: File, fileName: String): File? {
            val dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            val now = LocalDateTime.now()
            val zipFile = File(toZipFolder.parent, java.lang.String.format("%s.zip", fileName + "_" + dtf.format(now)))
            return try {
                val out = ZipOutputStream(FileOutputStream(zipFile))
                zipSubFolder(out, toZipFolder, toZipFolder.path.length)
                out.close()
                zipFile
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
                null
            }
        }

        /**
         * Main zip Function
         * @param out Target ZipStream
         * @param folder Folder to be zipped
         * @param basePathLength Length of original Folder Path (for recursion)
         */
        @Throws(IOException::class)
        private fun zipSubFolder(out: ZipOutputStream, folder: File, basePathLength: Int) {
            val buffer = 2048
            val fileList = folder.listFiles()
            var origin: BufferedInputStream?

            if (fileList != null) {
                for (file in fileList) {
                    if (file.isDirectory) {
                        zipSubFolder(out, file, basePathLength)
                    } else {
                        if (!file.path.endsWith(".exif.yaml")) {
                            val data = ByteArray(buffer)
                            val unmodifiedFilePath = file.path
                            val relativePath = unmodifiedFilePath.substring(basePathLength + 1)
                            val fi = FileInputStream(unmodifiedFilePath)
                            origin = BufferedInputStream(fi, buffer)
                            val entry = ZipEntry(relativePath)
                            entry.time = file.lastModified() // to keep modification time after unzipping
                            out.putNextEntry(entry)
                            var count: Int
                            while (origin.read(data, 0, buffer).also { count = it } != -1) {
                                out.write(data, 0, count)
                            }
                            origin.close()
                            out.closeEntry()
                        }
                    }
                }
            }
        }
    }
}