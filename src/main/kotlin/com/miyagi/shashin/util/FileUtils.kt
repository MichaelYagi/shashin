package com.miyagi.shashin.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.miyagi.shashin.model.Folder
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
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

        @Value("\${app.sidecar.path}")
        private var relativeSidecarDir: String? = null

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

        fun convertYamlToJson(yaml: String?): String {
            val yamlReader = ObjectMapper(YAMLFactory())
            val obj = yamlReader.readValue(yaml, Any::class.java)
            val jsonWriter = ObjectMapper()
            return jsonWriter.writeValueAsString(obj)
        }

        fun convertExifToJsonNode(folder: String, fileName: String, relativeSidecarDir: String): JsonNode? {
            // metadata/<folder>/<fileName>.exif.yaml
            val json: String
            var jsonNode: JsonNode? = null
            val mapper = ObjectMapper()

            val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
            val sidecarDir = rootPath + relativeSidecarDir
            val exifFilePath = sidecarDir.dropLast(1) + "/metadata" + folder + "/" + fileName + ".exif.yaml"
            val exifFile = File(exifFilePath)

            if (exifFile.exists()) {
                val content = Files.readString(exifFile.toPath())
                json = convertYamlToJson(content)

                if (json.isNotEmpty()) {
                    jsonNode = mapper.readTree(json)
                }
            }

            return jsonNode
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

        fun deleteEmptyDirectoriesOfFolder(folder: File) {
            for (fileEntry in folder.listFiles()) {
                if (fileEntry.isDirectory) {
                    deleteEmptyDirectoriesOfFolder(fileEntry)
                    if (fileEntry.listFiles() != null && fileEntry.listFiles().isEmpty()) {
                        fileEntry.delete()
                    }
                }
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