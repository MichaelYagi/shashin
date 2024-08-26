package com.miyagi.shashin.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.miyagi.shashin.ShashinApplication
import com.miyagi.shashin.model.*
import com.miyagi.shashin.repository.*
import org.springframework.core.io.FileSystemResource
import org.springframework.stereotype.Component
import java.io.*
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.xml.bind.DatatypeConverter.parseBase64Binary


@Component
class FileUtils(private val metadataRepository: MetadataRepository) {
    companion object {
        private var logger: Logger = Logger.getLogger(FileUtils::class.simpleName)

        fun thumbnailHeight(): Int {
            return 225
        }

        fun allowableMediaFiles(): Array<String> {
            return allowableImageFiles() + allowableVideoFiles() + allowableRawImageFiles()
        }

        fun allowableImageFiles(): Array<String> {
            return arrayOf("jpeg","jpg","png","bmp","gif","webm","webp","heif","heic","tiff")
        }

        fun allowableAudioFiles(): Array<String> {
            return arrayOf("3gpp","aac","flac","mpeg","mp3","mp4","ogg","wav","webm")
        }

        fun allowableVideoFiles(): Array<String> {
            return arrayOf("mp4","wav","avi","mov","hevc")
        }

        fun allowableRawImageFiles(): Array<String> {
            return arrayOf("nef","cr2","orf","arw","rw2","rwl","srw")
        }

        fun isRaw(extension: String): Boolean {
            return allowableRawImageFiles().contains(extension.lowercase())
        }

        fun fileCount(dir: String?): Long {
            if (dir.isNullOrBlank()) {
                logger.log(Level.WARNING, "Error counting files: Paths is null or blank")
                return 0
            }

            val slashesDir = dir.toString().replace('\\', '/')
            val directory = File(slashesDir)
            var count = 0L

            if (directory.exists()) {
                val files = directory.listFiles()

                try {
                    if (files != null) {
                        for (f in files) {
                            if (f.isDirectory) {
                                count += fileCount(f.absolutePath)
                            } else {
                                count++
                            }
                        }
                    }

                    return count
                } catch (e: IOException) {
                    logger.log(Level.SEVERE, "Error counting files for $slashesDir: " + e.message)
                    return 0
                }
            } else {
                logger.log(Level.WARNING, "Error counting files: $slashesDir does not exist")
                return 0
            }
        }

        fun createFile(filePath: String, fileName: String, type: String, overwriteThumbnails: Boolean = false): File? {
            try {
                // Create directory if dne
                val someFileDir = File(filePath)
                if (!someFileDir.exists()) {
                    someFileDir.mkdirs()
                }
                // Create file
                val someFile = File(fileName)
                if (overwriteThumbnails) {
                    logger.log(Level.INFO, type + " overwriting: " + someFile.name)
                    return someFile
                } else if (someFile.createNewFile()) {
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

        fun createThreadFile(threadName: String?): File? {
            if (!threadName.isNullOrBlank()) {
                //Create file with thread name and write file name iterated
                val tempDir = System.getProperty("java.io.tmpdir")
                val threadFile = createFile(
                    tempDir,
                    tempDir + "/" + Thread.currentThread().name + "." + threadName,
                    "Thread"
                )
                if (threadFile != null && threadFile.exists()) {
                    return threadFile
                }
                logger.log(Level.INFO, "$threadName could not be created")
            } else {
                logger.log(Level.INFO, "threadName not specified")
            }

            return null
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
            try {
                val yamlReader = ObjectMapper(YAMLFactory())
                val obj = yamlReader.readValue(yaml, Any::class.java)
                val jsonWriter = ObjectMapper()
                val json = jsonWriter.writeValueAsString(obj)

                if (isJSONValid(json)) {
                    return json
                }
            } catch (e: IOException) {
                logger.log(Level.WARNING, "Yaml to JSON conversion failed: " + e.message + ". String passed was: " + yaml)
            }
            return ""
        }

        fun getExifFile(folder: String, fileName: String, relativeSidecarDir: String): File? {
            // metadata/<folder>/<fileName>.exif.yaml
            val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
            val sidecarDir = rootPath + relativeSidecarDir
            val exifFilePath = sidecarDir.dropLast(1) + "/metadata" + folder + "/" + fileName + ".exif.yaml"
            val exifFile = File(exifFilePath)

            if (exifFile.exists()) {
                return exifFile
            }

            return null
        }

        fun convertExifToJsonNode(folder: String, fileName: String, relativeSidecarDir: String): JsonNode? {
            // metadata/<folder>/<fileName>.exif.yaml
            val json: String
            var jsonNode: JsonNode? = null
            val mapper = ObjectMapper()

            val exifFile = getExifFile(folder, fileName, relativeSidecarDir)

            if (exifFile != null && exifFile.exists()) {
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
            val zipFile = File(toZipFolder.parent, java.lang.String.format("%s.zip", fileName.replace("\\s".toRegex(), "_").lowercase() + "_" + dtf.format(now)))
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

        fun parseBase64(url: String): ByteArray? {
            val base64ImageArray = url.split(",")
            if (base64ImageArray.size > 1) {
                val base64Image = base64ImageArray[1];
                if (base64Image.isNotBlank()) {
                    return parseBase64Binary(base64Image)
                }
            }

            return null
        }

        fun writeToThreadFileAndLogMessage(message: String, threadFile: File) {
            try {
                val writer = BufferedWriter(FileWriter(threadFile))
                writer.write(message)
                writer.close()
            } catch(e: Exception) {
                logger.log(Level.WARNING, "Could not write to thread file: " + threadFile.name)
            }
        }

        private fun isJSONValid(jsonInString: String): Boolean {
            if (jsonInString.first() != '{' && jsonInString.first() != '[') {
                return false
            }

            try {
                Gson().fromJson(jsonInString, Any::class.java)
                return true
            } catch (ex: JsonSyntaxException) {
                return false
            }
        }

        fun loadFaceRecogFiles(settings: Settings?) {
            val classLoader: ClassLoader = ShashinApplication::class.java.classLoader
            val vggfaceFileExists = classLoader.getResource("lib/vggface2.pt") != null
            val retinafaceFileExists = classLoader.getResource("lib/retinaface.pt") != null

            if (settings != null && (!vggfaceFileExists || !retinafaceFileExists) && !NetworkUtils.checkCompreFaceConnection(
                    settings.getCompreFaceServer(),
                    settings.getCompreFaceKey()
                )
            ) {
                Thread {
                    // Download into resource lib folder
                    // https://github.com/jmformenti/face-recognition-java/raw/master/core/src/main/resources/models/pytorch/vggface2/vggface2.pt
                    // https://resources.djl.ai/test-models/pytorch/retinaface.zip
                    val baseDir = File(ClassLoader.getSystemResource("schema.sql").path).parent.replace("\\", "/")
                    Files.createDirectories(Paths.get("$baseDir/lib"))
                    val vggFile = File("$baseDir/lib/vggface2.pt")
                    if (vggFile.createNewFile()) {
                        org.apache.commons.io.FileUtils.copyURLToFile(
                            URL("https://github.com/jmformenti/face-recognition-java/raw/master/core/src/main/resources/models/pytorch/vggface2/vggface2.pt"),
                            File("$baseDir/lib/vggface2.pt")
                        )
                        if (!File("$baseDir/lib/vggface2.pt").exists()) {
                            logger.log(Level.WARNING, "vggface2 does not exist. vggface2 could not be created")
                        }
                    } else {
                        logger.log(Level.WARNING, "vggface2 could not be created")
                    }

                    val retinaFile = File("$baseDir/lib/retinaface.zip")
                    if (retinaFile.createNewFile()) {
                        org.apache.commons.io.FileUtils.copyURLToFile(
                            URL("https://resources.djl.ai/test-models/pytorch/retinaface.zip"),
                            File("$baseDir/lib/retinaface.zip")
                        )
                        if (!File("$baseDir/lib/retinaface.zip").exists()) {
                            logger.log(Level.WARNING, "retinaface.zip does not exist")
                        } else {
                            // Unzip it
                            FileUtils.unzip(
                                File("$baseDir/lib/retinaface.zip"),
                                File("$baseDir/lib/")
                            )
                            if (!File("$baseDir/lib/retinaface.pt").exists()) {
                                logger.log(
                                    Level.WARNING,
                                    "Could not unzip, retinaface.pt does not exist. retinaface could not be created"
                                )
                            } else {
                                logger.log(Level.INFO, "unzip successful for retinaface.pt")
                            }
                        }
                    } else {
                        logger.log(Level.WARNING, "retinaface could not be created")
                    }
                }.start()
            } else {
                logger.log(Level.INFO, "face recognition setup")
            }
        }

        @Throws(ZipException::class, IOException::class)
        fun unzip(file: File?, targetDir: File) {
            targetDir.mkdirs()
            val zipFile = ZipFile(file)

            if (file != null && file.exists()) {
                try {
                    val entries: Enumeration<out ZipEntry> = zipFile.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val targetFile = File(targetDir, entry.name)
                        if (entry.isDirectory) {
                            targetFile.mkdirs()
                        } else {
                            val input: InputStream = zipFile.getInputStream(entry)
                            try {
                                val output: OutputStream = FileOutputStream(targetFile)
                                try {
                                    copy(input, output)
                                } finally {
                                    output.close()
                                }
                            } finally {
                                input.close()
                            }
                        }
                    }
                } finally {
                    zipFile.close()
                }
            } else {
                logger.log(Level.WARNING, "Zip file does not exist: " + file?.absolutePath)
            }

        }

        @Throws(IOException::class)
        private fun copy(input: InputStream, output: OutputStream) {
            val buffer = ByteArray(4096)
            var size: Int
            while ((input.read(buffer).also { size = it }) != -1) output.write(buffer, 0, size)
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