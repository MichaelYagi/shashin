package com.miyagi.shashin.util

import ai.djl.modality.Classifications
import ai.djl.modality.cv.Image
import ai.djl.modality.cv.ImageFactory
import ai.djl.modality.cv.output.DetectedObjects
import ai.djl.repository.zoo.Criteria
import ai.djl.repository.zoo.ModelZoo
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.miyagi.shashin.model.*
import com.miyagi.shashin.repository.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.xml.bind.DatatypeConverter.parseBase64Binary


@Suppress("UNCHECKED_CAST")
@Component
class FileUtils(private val metadataRepository: MetadataRepository) {
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
            return arrayOf("jpeg","jpg","png","bmp","gif","webm","webp","heif","heic")
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
            if (allowableRawImageFiles().contains(extension.lowercase())) {
                return true
            }
            return false
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
                if (someFile.createNewFile()) {
                    logger.log(Level.INFO, type + " created: " + someFile.name)
                    return someFile
                } else if (overwriteThumbnails) {
                    logger.log(Level.INFO, type + " overwriting: " + someFile.name)
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

        fun pingURL(url: String, requestProperties: Map<String,String>? = null, timeoutInMS: Int = 0, requestMethod: String = "HEAD", jsonInputString: String = "", getResponse: Boolean = false): Boolean {
            var urlcopy = url
            urlcopy = urlcopy.replaceFirst(
                "^https".toRegex(),
                "http"
            ) // Otherwise an exception may be thrown on invalid SSL certificates.
            return try {
                val connection: HttpURLConnection = URL(urlcopy).openConnection() as HttpURLConnection
                connection.connectTimeout = timeoutInMS
                connection.readTimeout = timeoutInMS
                connection.requestMethod = requestMethod

                if (requestProperties != null) {
                    for (requestProperty in requestProperties) {
                        connection.setRequestProperty(requestProperty.key, requestProperty.value)
                    }
                }

                if (jsonInputString.isNotEmpty()) {
                    connection.setDoOutput(true)
                    val os: OutputStream = connection.outputStream
                    os.write(jsonInputString.toByteArray())
                    os.flush()
                    os.close()
                }

                if (getResponse) {
                    var response = ""
                    BufferedReader(
                        InputStreamReader(connection.getInputStream(), "utf-8")
                    ).use { br ->
                        val responseBuilder = StringBuilder()
                        var responseLine: String?
                        while (br.readLine().also { responseLine = it } != null) {
                            responseBuilder.append(responseLine!!.trim { it <= ' ' })
                        }
                        response = responseBuilder.toString()
                    }
                    logger.log(Level.INFO, "pingURL - response: $response")
                }

                val responseCode: Int = connection.responseCode

                responseCode in 200..399
            } catch (exception: IOException) {
                logger.log(Level.WARNING, "pingURL - exception for $urlcopy: ${exception.message}")
                false
            }
        }

        fun checkCompreFaceConnection(compreFaceServer: String?, compreFaceKey: String?): Boolean {
            val timingStart = Date()
            var available = false

            if (!compreFaceKey.isNullOrBlank() && !compreFaceServer.isNullOrBlank()) {
                val requestProperties: Map<String, String> = mapOf("x-api-key" to compreFaceKey)
                available = pingURL(compreFaceServer + "api/v1/recognition/faces?page=1&size=1&subject=1", requestProperties, 1000)
            }

            val timingEnd = Date()
            val diff: Long = timingEnd.time - timingStart.time

            val processingTime = SimpleDateFormat("mm:ss:SSS").format(Date(diff))
            logger.log(Level.INFO, "checkCompreFaceConnection - processing time: $processingTime")

            return available
        }

        fun checkNominatimConnection(nominatimUrl: String?): Boolean {
            var available = false
            if (!nominatimUrl.isNullOrBlank()) {
                var response: ResponseEntity<String>?
                try {
                    val webClient = WebClient.create(nominatimUrl)
                    response = webClient.get()
                        .retrieve()
                        .toEntity(String::class.java)
                        .block()

                    if (response != null) {
                        val jsonResult = response.body
                        val mapper = ObjectMapper()
                        val jsonObj = mapper.readTree(jsonResult)
                        val resultMap = mapper.convertValue(jsonObj, object : TypeReference<Map<String, Any>>() {})
                        if (resultMap.containsKey("status") && resultMap["status"] == 0) {
                            available = true
                        }
                    }
                } catch (e: Exception) {
                    available = false
                }
            }

            return available
        }

        fun recognizePerson(settings: Settings?, metadataObj: Metadata?): MutableList<String> {
            var personList = mutableListOf<String>()
            var response: String? = null

            try {
                var webClient: WebClient?
                if (settings != null && checkCompreFaceConnection(
                        settings.getCompreFaceServer(),
                        settings.getCompreFaceKey()
                    )
                ) {
                    webClient = WebClient.create(settings.getCompreFaceServer()!!)
                    var builder = MultipartBodyBuilder()
                    builder.part(
                        "file",
                        FileSystemResource(metadataObj?.getThumbnailPathSmall()!!)
                    )

                    response = webClient.post()
                        .uri("api/v1/recognition/recognize")
                        .header(
                            HttpHeaders.CONTENT_TYPE,
                            MediaType.MULTIPART_FORM_DATA.toString()
                        )
                        .header("x-api-key", settings.getCompreFaceKey())
                        .body(BodyInserters.fromMultipartData(builder.build()))
                        .retrieve()
                        .bodyToMono(String::class.java)
                        .block()

                    logger.log(
                        Level.INFO,
                        "Recognizing face for " + metadataObj.getPath() + ": " + response
                    )
                }


            } catch (e: Exception) {
                logger.log(
                    Level.WARNING,
                    "Error recognizing face for " + metadataObj?.getPath() + ": " + e.localizedMessage
                )
            }

            if (response != null) {
                val mapper = ObjectMapper()
                val jsonObj = mapper.readTree(response)
                val resultMap = mapper.convertValue(
                    jsonObj,
                    object :
                        TypeReference<Map<String, ArrayList<Map<String, Any>>>>() {})
                val resultList =
                    resultMap["result"] as ArrayList<Map<String, Any>>
                if (resultList.isNotEmpty() && resultList[0].containsKey("subjects")) {
                    for (singleResult in resultList) {
                        val subjects =
                            singleResult["subjects"] as ArrayList<Map<String, Any>>

                        for (subjectObj in subjects) {
                            var subject = ""
                            var similarity = 0.0

                            if (subjectObj.isNotEmpty()) {
                                subject = subjectObj["subject"].toString()
                                similarity =
                                    subjectObj["similarity"].toString()
                                        .toDouble()
                            }

                            if (similarity != 1.0 && (similarity <= 0.0 || similarity >= settings?.getRecognitionConfidenceThreshold()
                                    .toString().toDouble())
                            ) {
                                personList.add(subject)
                            }
                        }
                    }
                }
            }

            return personList
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

        fun subjectRecognizer(metadataRepository: MetadataRepository?, recognitionLabelRepository: RecognitionLabelRepository?, recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository?, settings: Settings, threadFile: File?, shouldStop: Boolean?) {
            // Scan records of photos that haven't been scanned in a separate thread
            val testImages = metadataRepository?.findNonMatched(settings.getMatchScanLimit()!!)
            val distinctLabelRecords = recognitionLabelPhotoRepository?.findGroupByRecognitionLabelId()

            if (testImages != null && distinctLabelRecords != null && distinctLabelRecords.count() > 0) {
                val mapper = ObjectMapper()
                val webClient = WebClient.create(settings.getCompreFaceServer()!!)

                for (testImage in testImages) {

                    if (shouldStop != null && shouldStop) {
                        break
                    }

                    val metadataObj = metadataRepository.findById(testImage.getId()).get()

                    // Facial recognition
                    val faceFsr = FileSystemResource(metadataObj.getThumbnailPathSmall()!!)
                    var builder = MultipartBodyBuilder()
                    builder.part(
                        "file",
                        faceFsr
                    )

                    var response: String? = null

                    try {
                        response = webClient.post()
                            .uri("api/v1/recognition/recognize")
                            .header(
                                HttpHeaders.CONTENT_TYPE,
                                MediaType.MULTIPART_FORM_DATA.toString()
                            )
                            .header("x-api-key", settings.getCompreFaceKey())
                            .body(BodyInserters.fromMultipartData(builder.build()))
                            .retrieve()
                            .bodyToMono(String::class.java)
                            .block()

                        logger.log(
                            Level.INFO,
                            "Recognizing face for " + metadataObj.getId() + " - " + metadataObj.getPath() + ": " + response
                        )
                    } catch (e: Exception) {
                        val recognitionLabelRecord =
                            recognitionLabelRepository?.findByNameIgnoreCase("object")
                        var recognitionLabelObj = RecognitionLabel()
                        if (recognitionLabelRecord == null) {
                            recognitionLabelObj.setName("object")
                            recognitionLabelObj.setCreatedAt(TextUtils.getCurrentTimestamp())
                            recognitionLabelObj.setModifiedAt(TextUtils.getCurrentTimestamp())
                            recognitionLabelRepository?.save(recognitionLabelObj)
                        } else {
                            recognitionLabelObj = recognitionLabelRecord
                        }

                        val recognitionLabelPhotoObj = RecognitionLabelPhoto()
                        recognitionLabelPhotoObj.setMetadataId(metadataObj.getId())
                        recognitionLabelPhotoObj.setRecognitionLabelId(recognitionLabelObj.getId())
                        recognitionLabelPhotoObj.setConfidence("-0.1")
                        recognitionLabelPhotoRepository.save(recognitionLabelPhotoObj)

                        logger.log(
                            Level.WARNING,
                            "Error recognizing face for " + metadataObj.getId() + " - " + metadataObj.getPath() + ": " + e.localizedMessage
                        )
                    }

                    if (response != null) {

                        var jsonObj = mapper.readTree(response)
                        val resultMap = mapper.convertValue(
                            jsonObj,
                            object :
                                TypeReference<Map<String, ArrayList<Map<String, Any>>>>() {})

                        var resultList: ArrayList<Map<String, Any>>? = null

                        if (resultMap.containsKey("result")) {
                            resultList =
                                resultMap["result"] as ArrayList<Map<String, Any>>
                        }

                        if (resultList != null) {
                            if (resultList.isNotEmpty() && resultList[0].containsKey("subjects")) {
                                for (singleResult in resultList) {
                                    val subjects =
                                        singleResult["subjects"] as ArrayList<Map<String, Any>>

                                    for (subjectObj in subjects) {
                                        var subject = ""
                                        var similarity = 0.0

                                        if (subjectObj.isNotEmpty()) {
                                            subject = subjectObj["subject"].toString()
                                            similarity =
                                                subjectObj["similarity"].toString().toDouble()
                                        }

                                        if (threadFile != null) {
                                            writeToThreadFileAndLogMessage(
                                                "Analyzing subject " + subject + " for " + metadataObj.getPath(),
                                                threadFile
                                            )
                                        }

                                        if (similarity != 1.0 && (similarity <= 0.0 || similarity >= settings.getRecognitionConfidenceThreshold()
                                                .toString().toDouble())
                                        ) {

                                            response = null

                                            try {
                                                builder = MultipartBodyBuilder()
                                                builder.part(
                                                    "file",
                                                    faceFsr
                                                )

                                                response = webClient.post()
                                                    .uri("api/v1/recognition/faces?subject=${subject}")
                                                    .header(
                                                        HttpHeaders.CONTENT_TYPE,
                                                        MediaType.MULTIPART_FORM_DATA.toString()
                                                    )
                                                    .header(
                                                        "x-api-key",
                                                        settings.getCompreFaceKey()
                                                    )
                                                    .body(BodyInserters.fromMultipartData(builder.build()))
                                                    .retrieve()
                                                    .bodyToMono(String::class.java)
                                                    .block()
                                            } catch (e: Exception) {
                                                logger.log(
                                                    Level.WARNING,
                                                    "Error uploading face for " + subject + " for " + metadataObj.getId() + " - " +" image " + metadataObj.getPath() + ": " + e.localizedMessage
                                                )
                                            }

                                            var compreFaceImageId: String? = null

                                            if (response != null) {
                                                jsonObj = mapper.readTree(response)

                                                if (jsonObj.has("image_id")) {
                                                    compreFaceImageId =
                                                        jsonObj["image_id"].toString()
                                                    compreFaceImageId =
                                                        compreFaceImageId.drop(1).dropLast(1)
                                                }
                                            }

                                            logger.log(
                                                Level.INFO,
                                                "Uploaded subject for " + metadataObj.getId() + " - " + metadataObj.getPath() + " for subject " + subject + ": " + response
                                            )

                                            val recognitionLabelObj =
                                                recognitionLabelRepository?.findByNameIgnoreCase(
                                                    subject
                                                )

                                            if (recognitionLabelObj != null) {
                                                val recognitionLabelPhoto =
                                                    recognitionLabelPhotoRepository.countByRecognitionLabelIdAndMetadataId(
                                                        recognitionLabelObj.getId(),
                                                        metadataObj.getId()
                                                    )

                                                if (recognitionLabelPhoto == 0) {
                                                    val recognitionLabelPhotoObj =
                                                        RecognitionLabelPhoto()
                                                    recognitionLabelPhotoObj.setMetadataId(
                                                        metadataObj.getId()
                                                    )
                                                    recognitionLabelPhotoObj.setRecognitionLabelId(
                                                        recognitionLabelObj.getId()
                                                    )
                                                    recognitionLabelPhotoObj.setConfidence(
                                                        similarity.toString()
                                                    )
                                                    if (compreFaceImageId != null) {
                                                        recognitionLabelPhotoObj.setCompreFaceImageId(
                                                            compreFaceImageId
                                                        )
                                                    }
                                                    recognitionLabelPhotoRepository.save(
                                                        recognitionLabelPhotoObj
                                                    )

                                                    metadataObj.setModifiedAt(TextUtils.getCurrentTimestamp())
                                                    metadataRepository.save(metadataObj)

                                                    if (threadFile != null) {
                                                        writeToThreadFileAndLogMessage(
                                                            "Processed subject " + subject + " for " + metadataObj.getPath() + " with similarity " + similarity.toString(),
                                                            threadFile
                                                        )
                                                    }
                                                }
                                            } else {
                                                logger.log(
                                                    Level.INFO,
                                                    "Did not process subject " + subject + " for " + metadataObj.getId() + " - " + metadataObj.getPath() + " with similarity " + similarity.toString()
                                                )
                                            }
                                        } else {
                                            logger.log(
                                                Level.INFO,
                                                "Did not upload subject " + subject + " for " + metadataObj.getId() + " - " + metadataObj.getPath() + " with similarity " + similarity.toString()
                                            )
                                        }
                                    }
                                }
                            }
                        }
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