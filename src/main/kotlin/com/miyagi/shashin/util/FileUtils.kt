package com.miyagi.shashin.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.miyagi.shashin.ShashinApplication
import com.miyagi.shashin.model.*
import com.miyagi.shashin.repository.*
import org.apache.commons.io.FilenameUtils
import org.apache.tika.Tika
import org.apache.tika.detect.Detector
import org.apache.tika.mime.MediaType
import org.apache.tika.parser.AutoDetectParser
import org.springframework.core.io.FileSystemResource
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.*
import java.net.URL
import java.net.URLConnection
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.xml.bind.DatatypeConverter.parseBase64Binary
import kotlin.io.path.Path


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
            return arrayOf("jpeg","jpg","png","bmp","gif","webm","webp","heif","heic","tiff") //,"avif")
        }

        fun allowableAudioFiles(): Array<String> {
            return arrayOf("3gpp","aac","flac","mpeg","mp3","mp4","ogg","wav","webm")
        }

        fun allowableVideoFiles(): Array<String> {
            return arrayOf("mp4","wav","avi","mov","hevc")
        }

        fun allowableRawImageFiles(): Array<String> {
            return arrayOf("nef","cr2","orf","arw","rw2","rwl","srw","dng","tiff") //,"raf"
        }

        fun isRaw(extension: String): Boolean {
            return allowableRawImageFiles().contains(extension.lowercase())
        }

        fun sidecarDiskUsage(sidecarDir: String): Map<String, Double> {
            var rawSidecarUsabe: Double = 0.0
            var rawSidecarTotal: Double = 0.0
            val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')

            try {
                var dir = Paths.get(rootPath)
                dir = dir.toRealPath()
                if (File(sidecarDir).exists()) {
                    dir = Paths.get(sidecarDir)
                }
                val fs = Files.getFileStore(dir)
                rawSidecarUsabe = fs.usableSpace.toDouble()
                rawSidecarTotal = fs.totalSpace.toDouble()
            } catch (exception: Exception) {
                logger.log(Level.WARNING, "Error reading sidecar directory:" + exception.message)
            }

            return mapOf("rawSidecarUsabe" to rawSidecarUsabe,"rawSidecarTotal" to rawSidecarTotal)
        }

        fun sidecarDiskUsed(sidecarDir: String): Long {
            var sidecarSize = 0.toLong()

            try {
                var directory = File(sidecarDir)
                if (Files.isSymbolicLink(Paths.get(sidecarDir))) {
                    val path = Path(sidecarDir)
                    val realPath = path.toRealPath()
                    directory = realPath.toFile()
                }
                val files = directory.walk().filter { it.isFile }.toList()
                files.map { file ->
                    sidecarSize += file.length()
                }
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Error calculating sidecar size:" + e.message)
            }

            return sidecarSize
        }

        fun probeFileExtension(file: File): String {
            var mediaExtension = file.extension.lowercase()
            if (mediaExtension == "") {
                val mediaMimeType = Files.probeContentType(Path(file.path))

                if (mediaMimeType != null) {
                    val mediaMimeTypeArray = mediaMimeType.split("/")
                    if (mediaMimeTypeArray.size > 1) {
                        mediaExtension = mediaMimeTypeArray[1].lowercase()
                    }
                } else {
                    try {
                        val inputStream: InputStream = BufferedInputStream(FileInputStream(file))
                        val mimeType: String? = URLConnection.guessContentTypeFromStream(inputStream)
                        inputStream.close()
                        if (mimeType != null) {
                            val mediaMimeTypeArray = mimeType.split("/")
                            if (mediaMimeTypeArray.size > 1) {
                                mediaExtension = mediaMimeTypeArray[1].lowercase()
                            }
                        }
                    } catch (_: Exception) {}

                    if (mediaExtension == "") {
                        val tika = Tika()
                        try {
                            val mimeType = tika.detect(file)
                            if (mimeType != null) {
                                val mediaMimeTypeArray = mimeType.split("/")
                                if (mediaMimeTypeArray.size > 1) {
                                    mediaExtension = mediaMimeTypeArray[1].lowercase()
                                }
                            }
                        } catch (e: Exception) {
                            logger.log(Level.WARNING, "Could not detect file extension: ${e.localizedMessage}")
                        }
                    }
                }
            }

            return mediaExtension
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

        fun createFile(fileName: String, overwriteThumbnails: Boolean = false): File? {
            val someFile = File(fileName)

            try {
                // Create directory if dne
                val someFileDir = File(someFile.parent)
                if (!someFileDir.exists()) {
                    someFileDir.mkdirs()
                }
                // Create file
                if (overwriteThumbnails) {
                    logger.log(Level.INFO, "Overwriting file: $fileName")
                    return someFile
                } else if (someFile.createNewFile()) {
                    logger.log(Level.INFO, "Created file: $fileName")
                    return someFile
                } else {
                    logger.log(Level.INFO, "File already exists: $fileName")
                    //return someFile
                    return null
                }
            } catch (e: IOException) {
                logger.log(Level.SEVERE, "Error creating $fileName: " + e.message)
                return null
            }
        }

        fun createThreadFile(threadName: String?): File? {
            if (!threadName.isNullOrBlank()) {
                //Create file with thread name and write file name iterated
                val tempDir = System.getProperty("java.io.tmpdir")
                val threadFile = createFile(
                    tempDir + "/" + Thread.currentThread().name + "." + threadName
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

        private fun threadIsAlive(threadName: String): Boolean {
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
            for (fileEntry in folder.listFiles()!!) {
                if (fileEntry.isDirectory) {
                    deleteEmptyDirectoriesOfFolder(fileEntry)
                    if (fileEntry.listFiles() != null && fileEntry.listFiles()!!.isEmpty()) {
                        fileEntry.delete()
                    }
                }
            }
        }

        fun parseBase64(url: String): ByteArray? {
            val base64ImageArray = url.split(",")
            if (base64ImageArray.size > 1) {
                val base64Image = base64ImageArray[1]
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

        fun copyMultipartFiles(media: List<MultipartFile>, settings: Settings): MutableMap<String, MutableList<String>> {
            var uploadDirectory = settings.getUploadMediaDirectory()?.replace('\\', '/')
            if (uploadDirectory?.last() != '/') {
                uploadDirectory = "$uploadDirectory/shashin/"
            }

            val ret = mutableMapOf<String, MutableList<String>>()
            val uploadedFiles = mutableListOf<String>()
            val notUploadedFiles = mutableListOf<String>()

            val simpleTimestamp = TextUtils.getUnformattedCurrentTimestamp()
            if (!File(uploadDirectory + simpleTimestamp).exists()) {
                Files.createDirectories(Paths.get(uploadDirectory + simpleTimestamp))
            }

            for (file in media) {
                val extension = FilenameUtils.getExtension(file.originalFilename).lowercase()

                if (extension != "" && !allowableMediaFiles().contains(extension)) {
                    notUploadedFiles.add(file.originalFilename.toString())
                } else {
                    val copyToFile = File(uploadDirectory + simpleTimestamp + "/" + file.originalFilename)
                    if (!copyToFile.exists()) {
                        try {
                            copyToFile.createNewFile()
                            val outputStream = FileOutputStream(copyToFile)
                            outputStream.write(file.bytes)
                            outputStream.close()

                            val probeExtension = probeFileExtension(copyToFile)

                            if (allowableMediaFiles().contains(probeExtension)) {
                                uploadedFiles.add(file.originalFilename.toString())
                                logger.log(
                                    Level.INFO,
                                    file.originalFilename + " uploaded"
                                )
                            } else {
                                Files.deleteIfExists(Paths.get(copyToFile.absolutePath))
                                notUploadedFiles.add(file.originalFilename.toString())
                                logger.log(
                                    Level.WARNING,
                                    file.originalFilename + ": Extension not detected or media type not accepted"
                                )
                            }
                        } catch (e: Exception) {
                            logger.log(
                                Level.WARNING,
                                file.originalFilename + ": " + e.localizedMessage
                            )
                            notUploadedFiles.add(file.originalFilename.toString())
                        }
                    } else {
                        logger.log(
                            Level.WARNING,
                            file.originalFilename + " already exists"
                        )
                        notUploadedFiles.add(file.originalFilename.toString())
                    }
                }
            }

            ret["uploadedFiles"] = uploadedFiles
            ret["notUploadedFiles"] = notUploadedFiles

            return ret
        }

        @Throws(ZipException::class, IOException::class)
        fun unzip(file: File?, targetDir: File) {
            if (file != null && file.exists()) {
                targetDir.mkdirs()
                val zipFile = ZipFile(file)

                zipFile.use { zipF ->
                    val entries: Enumeration<out ZipEntry> = zipF.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val targetFile = File(targetDir, entry.name)
                        if (entry.isDirectory) {
                            targetFile.mkdirs()
                        } else {
                            val input: InputStream = zipF.getInputStream(entry)
                            input.use { inp ->
                                val output: OutputStream = FileOutputStream(targetFile)
                                output.use { outp ->
                                    copy(inp, outp)
                                }
                            }
                        }
                    }
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
            val metricsUtil = MetricsUtil()
            val buffer = 2048
            val fileList = folder.listFiles()
            var origin: BufferedInputStream?

            if (fileList != null) {
                for (file in fileList) {
                    if (file.isDirectory) {
                        zipSubFolder(out, file, basePathLength)
                    } else {
                        if (!file.path.endsWith(".exif.yaml")) {
                            val fileSizeInBytes = file.length()

                            // Optionally, convert to other units
                            val fileSizeInKB = fileSizeInBytes / 1024
//                            val fileSizeInMB = fileSizeInKB / 1024

                            logger.log(
                                Level.INFO,
                                "${file.path} $fileSizeInKB KB"
                            )

                            val contentType = Files.probeContentType(file.toPath())
                            if (contentType != null && contentType.contains("video")) {
                                out.setLevel(Deflater.NO_COMPRESSION) //Deflater.BEST_SPEED
                            } else if (fileSizeInKB > 10000) { //10mb
                                out.setLevel(Deflater.NO_COMPRESSION) //Deflater.BEST_SPEED
                            } else {
                                out.setLevel(Deflater.DEFAULT_COMPRESSION)
                            }
                            val data = ByteArray(buffer)
                            val unmodifiedFilePath = file.path
                            val relativePath = unmodifiedFilePath.substring(basePathLength + 1)
                            val fi = FileInputStream(unmodifiedFilePath)
                            origin = BufferedInputStream(fi, buffer)
                            metricsUtil.start("Zip Entry: " + file.name)
                            val entry = ZipEntry(relativePath)
                            entry.time = file.lastModified() // to keep modification time after unzipping
                            out.putNextEntry(entry)
                            var count: Int
                            while (origin.read(data, 0, buffer).also { count = it } != -1) {
                                out.write(data, 0, count)
                            }
                            metricsUtil.end()
                            origin.close()
                            out.closeEntry()
                        }
                    }
                }
            }
        }
    }
}