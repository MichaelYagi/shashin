package com.miyagi.shashin.util

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.miyagi.shashin.model.Folder
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.Settings
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Component
import org.springframework.ui.set
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.ArrayList
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
         * Pings a HTTP URL. This effectively sends a HEAD request and returns `true` if the response code is in
         * the 200-399 range.
         * @param url The HTTP URL to be pinged.
         * @param timeout The timeout in millis for both the connection timeout and the response read timeout. Note that
         * the total timeout is effectively two times the given timeout.
         * @return `true` if the given HTTP URL has returned response code 200-399 on a HEAD request within the
         * given timeout, otherwise `false`.
         */
        fun pingURL(url: String, timeout: Int): Boolean {
            var urlcopy = url
            urlcopy = urlcopy.replaceFirst(
                "^https".toRegex(),
                "http"
            ) // Otherwise an exception may be thrown on invalid SSL certificates.
            return try {
                val connection: HttpURLConnection = URL(urlcopy).openConnection() as HttpURLConnection
                connection.connectTimeout = timeout
                connection.readTimeout = timeout
                connection.requestMethod = "HEAD"
                val responseCode: Int = connection.responseCode
                responseCode in 200..399
            } catch (exception: IOException) {
                false
            }
        }

        fun checkCompreFaceConnection(compreFaceServer: String?, compreFaceKey: String?): Boolean {
            var available = false
            if (!compreFaceKey.isNullOrBlank() && !compreFaceServer.isNullOrBlank()) {
                var compreFaceResponse: ResponseEntity<String>?
                try {
                    val webClient = WebClient.create(compreFaceServer)
                    compreFaceResponse = webClient.get()
                        .uri("api/v1/recognition/subjects/")
                        .header("x-api-key", compreFaceKey)
                        .retrieve()
                        .toEntity(String::class.java)
                        .block()
                    available =
                        compreFaceResponse != null && compreFaceResponse.statusCode.toString().lowercase() == "200 ok"
                } catch (e: Exception) {
                    available = false
                }
            }

            return available
        }

        fun buildPersonUpload(settings: Settings, personName: String?, metadata: Metadata?, compreFaceImageIdMap: MutableMap<String, Any?>): MutableMap<String, Any?> {
            val mapper = ObjectMapper()
            val uploadresponse = mutableMapOf<String, Any?>()
            uploadresponse["responseData"] = mutableMapOf<String, Any?>()
            uploadresponse["similarity"] = 0.0

            uploadresponse["msg"] = ""
            uploadresponse["status"] = ApiResponse.FAIL.status

            if (checkCompreFaceConnection(settings.getCompreFaceServer(), settings.getCompreFaceKey())) {
                var response: String?

                if (!personName.isNullOrBlank() && !metadata?.getId().isNullOrBlank()) {

                    val webClient = WebClient.create(settings.getCompreFaceServer()!!)

                    val recognizedObj = mapper.writeValueAsString(buildPersonRecognition(settings, metadata))

                    val jsonRespObj = mapper.readTree(recognizedObj)
                    var subjectObj: JsonNode
                    var subject = ""
                    var similarity = 0.0
                    if (jsonRespObj.has("recognizeData") && jsonRespObj["recognizeData"].has(0)) {
                        subjectObj = jsonRespObj["recognizeData"].get(0)
                        if (subjectObj.has("subject")) {
                            subject = subjectObj["subject"].textValue();
                            similarity = subjectObj["similarity"].asDouble()
                        }
                    }
                    uploadresponse["similarity"] = similarity

                    // This means the person has been relabeled
                    if (subject != "" && personName != subject) {
                        similarity = 0.0

                        val compreFaceImageId =
                            compreFaceImageIdMap[subject.filterNot { it.isWhitespace() } + "-" + metadata?.getId()]

                        try {
                            if (compreFaceImageId != null && compreFaceImageId.toString().isNotEmpty()) {
                                webClient.delete()
                                    .uri("api/v1/recognition/faces/$compreFaceImageId")
                                    .header("x-api-key", settings.getCompreFaceKey())
                                    .retrieve()
                                    .bodyToMono(String::class.java)
                                    .block()
                            }
                        } catch (e: Exception) {
                            logger.log(
                                Level.WARNING,
                                "Error deleting CompreFace ID ${compreFaceImageId} for ${metadata?.getId()}: " + e.localizedMessage
                            )
                            val errorResponse =
                                e.localizedMessage.replace("<EOL>", "").replace("400 : ", "").replace("\\s".toRegex(), "")
                        }
                    }

                    // Uploaded faces
                    if (similarity != 1.0 && (similarity <= 0.0 || similarity >= settings.getRecognitionConfidenceThreshold().toString().toDouble())) {
                        try {
                            if (metadata != null) {
                                val builder = MultipartBodyBuilder()
                                builder.part("file", FileSystemResource(metadata.getThumbnailPathSmall()!!))

                                response = webClient.post()
                                    .uri("api/v1/recognition/faces?subject=${personName}")
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA.toString())
                                    .header("x-api-key", settings.getCompreFaceKey())
                                    .body(BodyInserters.fromMultipartData(builder.build()))
                                    .retrieve()
                                    .bodyToMono(String::class.java)
                                    .block()

                                val jsonObj = mapper.readTree(response)

                                logger.log(
                                    Level.INFO,
                                    "Face $personName for ${metadata.getId()} uploaded: " + response
                                )

                                uploadresponse["responseData"] = jsonObj

                                uploadresponse["msg"] = ""
                                uploadresponse["status"] = ApiResponse.SUCCESS.status
                            } else {
                                response = "Metadata not found."
                                uploadresponse["responseData"] = response
                            }
                        } catch (e: Exception) {
                            logger.log(
                                Level.WARNING,
                                "Error uploading face $personName for ${metadata?.getId()}: " + e.localizedMessage
                            )
                            val errorResponse =
                                e.localizedMessage.replace("<EOL>", "").replace("400 : ", "").replace("\\s".toRegex(), "")
                            uploadresponse["responseData"] = errorResponse
                        }
                    } else {
                        logger.log(
                            Level.WARNING,
                            "Error - Similarity: $similarity, Threshold: ${settings.getRecognitionConfidenceThreshold().toString().toDouble()}"
                        )
                        uploadresponse["msg"] = "Error - Similarity: $similarity, Threshold: ${settings.getRecognitionConfidenceThreshold().toString().toDouble()}"
                        uploadresponse["status"] = ApiResponse.FAIL.status
                    }
                } else {
                    logger.log(
                        Level.WARNING,
                        "Person name or metadata ID blank"
                    )
                    uploadresponse["msg"] = "Person name or metadata ID blank"
                    uploadresponse["status"] = ApiResponse.FAIL.status
                }
            }

            return uploadresponse
        }

        fun buildPersonRecognition(settings: Settings, metadata: Metadata?): MutableMap<String, Any?> {
            val mapper = ObjectMapper()
            val recogresponse = mutableMapOf<String, Any?>()

            recogresponse["recognizeData"] = mutableMapOf<String, Any?>()
            recogresponse["msg"] = ""
            recogresponse["status"] = ApiResponse.FAIL.status

            if (checkCompreFaceConnection(settings.getCompreFaceServer(), settings.getCompreFaceKey())) {

                var response: String?

                if (metadata !== null) {
                    // Recognizing faces

                    try {
                        val webClient = WebClient.create(settings.getCompreFaceServer()!!)

                        val builder = MultipartBodyBuilder()
                        builder.part("file", FileSystemResource(metadata.getThumbnailPathSmall()!!))

                        response = webClient.post()
                            .uri("api/v1/recognition/recognize")
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA.toString())
                            .header("x-api-key", settings.getCompreFaceKey())
                            .body(BodyInserters.fromMultipartData(builder.build()))
                            .retrieve()
                            .bodyToMono(String::class.java)
                            .block()

                        val jsonObj = mapper.readTree(response)
                        val resultMap = mapper.convertValue(jsonObj, object : TypeReference<Map<String, ArrayList<Map<String, Any>>>>() {})
                        val resultList = resultMap["result"] as ArrayList<Map<String, Any>>
                        var subjects: ArrayList<Map<String, Any>>? = null
                        if (resultList.isNotEmpty() && resultList[0].containsKey("subjects")) {
                            subjects = resultList[0]["subjects"] as ArrayList<Map<String, Any>>
                        }
                        recogresponse["recognizeData"] = subjects
                        recogresponse["msg"] = ""
                        recogresponse["status"] = ApiResponse.SUCCESS.status

                    } catch (e: Exception) {
                        val errorResponse =
                            e.localizedMessage.replace("<EOL>", "").replace("400 : ", "").replace("\\s".toRegex(), "")
                        recogresponse["recognizeData"] = errorResponse
                    }
                } else {
                    recogresponse["msg"] = "Metadata ID blank"
                    recogresponse["status"] = ApiResponse.FAIL.status
                }
            }

            return recogresponse
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