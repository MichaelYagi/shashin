package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.component.FaceRecognizer
import com.miyagi.shashin.component.Message
import com.miyagi.shashin.component.ScanMessage
import com.miyagi.shashin.model.*
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.ApiResponse
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.TextUtils
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import org.apache.commons.text.StringEscapeUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.annotation.SubscribeMapping
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.socket.messaging.SessionConnectEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import org.springframework.web.socket.messaging.SessionSubscribeEvent
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import java.util.logging.Logger
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession
import javax.transaction.Transactional

@Controller
class PeopleController {

    @Autowired
    private var metadataRepository: MetadataRepository? = null

    @Autowired
    private var notificationRepository: NotificationRepository? = null

    @Autowired
    private var userRepository: UserRepository? = null

    @Autowired
    private var recognitionLabelRepository: RecognitionLabelRepository? = null

    @Autowired
    private var recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository? = null

    @Autowired
    private val keywordRepository: KeywordRepository? = null

    @Value("\${app.role.admin}")
    private lateinit var adminRole: String

    private var shouldStop = AtomicBoolean(false)

    private val threadExtensionName: String = "facescan_shashinscan"

    private var logger: Logger = Logger.getLogger(SettingsController::class.simpleName)

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, Any?>()

    @MessageMapping("/matchmessage")
    @SendTo("/topic/matchmessages")
    @Throws(java.lang.Exception::class)
    fun sendMatcnMessage(message: ScanMessage): Message? {
        var msg = "Start Matching"

        if (shouldStop.get()) {
            msg = "Matching Cancelled"
        } else if (!FileUtils.checkThreadFileAlive(threadExtensionName)) {
            msg = "Matching Complete"
        } else {
            val threadFileContent = FileUtils.readThreadFile(threadExtensionName)
            if (threadFileContent != null) {
                msg = "Matching in progress: " + threadFileContent.replace("\\", "/")
            }
        }

        val messageObj = Message()
        messageObj.setContent(msg)

        return messageObj
    }

    @SubscribeMapping("/topic/matchmessages")
    fun subscribe(
        session: HttpSession,
        @PathVariable pipelineId: String,
        @PathVariable topic: String
    ) {
//        println("subscribe")
//        println(session.id)
//        messagingTemplate?.convertAndSend("/app/scanmessage", "testingzzz");

    }

    @EventListener
    fun onApplicationEvent(event: SessionConnectEvent) {
//        println("SessionConnectEvent")
//        println(event.source)

//        messagingTemplate?.convertAndSend("/topic/messages", "testingzzz");
    }

    @EventListener
    fun onApplicationEvent(event: SessionDisconnectEvent) {
//        println("SessionDisconnectEvent")
//        println(event.sessionId)
    }

    @EventListener
    fun handleSubscribeEvent(event: SessionSubscribeEvent) {
//        println("SessionSubscribeEvent")
//        println(event.message)
    }

    @RequestMapping(value = ["/person/matches/start"], method = [RequestMethod.POST], produces = ["application/json"])
    @Secured("ROLE_ADMIN")
    @ResponseBody
    fun startPredictions(model: Model,@RequestParam cancelScan: Boolean): String {
        val settings = model.getAttribute("settings") as Settings

        if (cancelScan) {
            shouldStop.set(true)
        }

        // Scan records of photos that haven't been scanned in a separate thread
        val testImages = metadataRepository?.findNonMatched(settings.getMatchScanLimit()!!)
//        val trainingData = metadataRepository?.findTrainingData(settings.getRecognitionConfidenceThreshold()!!, settings.getTrainingDataLimit()!!)
        val distinctLabelRecords = this.recognitionLabelPhotoRepository?.findGroupByRecognitionLabelId()

        // Start matching in a separate thread
        if (testImages != null && distinctLabelRecords != null && distinctLabelRecords.count() > 0) {
//            val faceRecognizer = FaceRecognizer(
//                testImages,
//                trainingData,
//                recognitionLabelPhotoRepository,
//                recognitionLabelRepository,
//                notificationRepository,
//                userRepository,
//                adminRole,
//                settings.getRecognitionConfidenceThreshold()!!.toDouble()
//            )
//            faceRecognizer.runRecognizer(shouldStop.get())


            val tempDir = System.getProperty("java.io.tmpdir")

            if (!FileUtils.checkThreadFileAlive(threadExtensionName)) {
                // Clean up any existing thread files
                FileUtils.deleteThreadFiles(threadExtensionName)

                Thread {
                    val threadFile = FileUtils.createFile(
                        tempDir,
                        tempDir + "/" + Thread.currentThread().name + "." + threadExtensionName,
                        "Thread"
                    )

                    if (threadFile != null) {
                        for (distinctLabelRecord in distinctLabelRecords) {
                            if (FileUtils.readThreadFile(threadExtensionName) == null || !FileUtils.checkThreadFileAlive(threadExtensionName)) {
                                break
                            }

                            val name =
                                recognitionLabelRepository?.findById(distinctLabelRecord.getRecognitionLabelId()!!)

                            val compreFaceImageIdMap = mutableMapOf<String, Any?>()

                            if (name != null && name.get().getName() != "null") {
                                for (testImage in testImages) {
                                    if (FileUtils.readThreadFile(threadExtensionName) == null || !FileUtils.checkThreadFileAlive(threadExtensionName)) {
                                        break
                                    }

                                    val metadata = metadataRepository?.findById(testImage.getId())

                                    writeToThreadFileAndLogMessage("Matching " + metadata?.get()?.getPath(),threadFile)

                                    val personUploaded = mapper.writeValueAsString(
                                        buildPersonUpload(
                                            settings,
                                            name.get().getName(),
                                            metadata?.get(),
                                            compreFaceImageIdMap
                                        )
                                    )
                                    val jsonRespObj = mapper.readTree(personUploaded)

                                    if (jsonRespObj.has("similarity") && jsonRespObj.has("responseData") && jsonRespObj["responseData"].has(
                                            "subject"
                                        )
                                    ) {
                                        val subject = jsonRespObj["responseData"].get("subject").textValue()
                                        val similarity = jsonRespObj["similarity"].asDouble()

                                        logger.log(
                                            Level.INFO,
                                            "Results for $subject: $similarity"
                                        )

                                        var compreFaceImageId: String? = null
                                        if (jsonRespObj.has("responseDataUpload") && jsonRespObj["responseDataUpload"].has(
                                                "image_id"
                                            )
                                        ) {
                                            compreFaceImageId = jsonRespObj["responseDataUpload"]["image_id"].toString()
                                            compreFaceImageId = compreFaceImageId.drop(1).dropLast(1)
                                        } else if (jsonRespObj.has("responseData") && jsonRespObj["responseData"].has("image_id")) {
                                            compreFaceImageId = jsonRespObj["responseData"]["image_id"].toString()
                                            compreFaceImageId = compreFaceImageId.drop(1).dropLast(1)
                                        }

                                        if (similarity >= settings.getRecognitionConfidenceThreshold()!!.toDouble()) {
                                            val countDistinctLabelIdAndMetadataId =
                                                recognitionLabelPhotoRepository?.countDistinctLabelIdAndMetadataId(
                                                    distinctLabelRecord.getRecognitionLabelId()!!,
                                                    testImage.getId()
                                                )
                                            if (countDistinctLabelIdAndMetadataId == 0) {
                                                // Create new record
                                                val recognitionLabelPhoto = RecognitionLabelPhoto()
                                                recognitionLabelPhoto.setRecognitionLabelId(distinctLabelRecord.getRecognitionLabelId()!!)
                                                recognitionLabelPhoto.setMetadataId(testImage.getId())
                                                recognitionLabelPhoto.setAutoTagged(true)
                                                recognitionLabelPhoto.setConfidence(similarity.toString())
                                                recognitionLabelPhoto.setCompreFaceImageId(compreFaceImageId)
                                                recognitionLabelPhotoRepository?.save(recognitionLabelPhoto)
                                            } else {
                                                // Update
                                                val recognitionLabelPhoto =
                                                    recognitionLabelPhotoRepository?.findById(distinctLabelRecord.getRecognitionLabelId()!!)
                                                if (recognitionLabelPhoto != null) {
                                                    recognitionLabelPhoto.get().setAutoTagged(true)
                                                    recognitionLabelPhoto.get().setConfidence(similarity.toString())
                                                    recognitionLabelPhoto.get().setCompreFaceImageId(compreFaceImageId)
                                                    recognitionLabelPhotoRepository?.save(recognitionLabelPhoto.get())
                                                }
                                            }
                                        }
                                    } else {
                                        logger.log(
                                            Level.WARNING,
                                            "Could not get results from recognizer for " + name.get().getName()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    FileUtils.deleteThreadFiles(threadExtensionName)
                    writeToThreadFileAndLogMessage("Matching Complete",threadFile!!)
                }.start()
            }

            if (shouldStop.get()) {
                FileUtils.deleteThreadFiles(threadExtensionName)
            }

            shouldStop.set(false)
        } else {
            resp["msg"] = "Training data not detected."
            resp["status"] = ApiResponse.FAIL.status
            return mapper.writeValueAsString(resp)
        }

        resp["msg"] = "Start Matching"
        resp["status"] = ApiResponse.SUCCESS.status
        return mapper.writeValueAsString(resp)
    }

    private fun writeToThreadFileAndLogMessage(message: String, threadFile: File) {
        try {
            val writer = BufferedWriter(FileWriter(threadFile))
            writer.write(message)
            writer.close()
        } catch(e: Exception) { }
    }

    @GetMapping("/person/matches/{personId}")
    @Secured("ROLE_ADMIN")
    fun getPredictions(model: Model, @PathVariable personId: Int): String {
        val module = "matches"
        model["message"] = "There are no photos."
        model["lowMatchResults"] = mutableListOf<Metadata>()
        model["recognitionLabels"] = mutableListOf<RecognitionLabel>()
        model["labelPhotoMap"] = mutableMapOf<String, Any>()
        model["keywordMap"] = mutableMapOf<String, String>()
        val counts = HashMap<String,Int>()
        counts["person"] = 0
        counts["matches"] = 0
        model["counts"] = counts
        model["parameter"] = personId

        val recognitionLabels = recognitionLabelRepository?.findAllByNameNotContaining("object")
        if (recognitionLabels != null && recognitionLabels.count() > 0) {
            model["recognitionLabels"] = recognitionLabels
        }

        val recognitionLabel = recognitionLabelRepository?.findById(personId)
        if (recognitionLabel != null && recognitionLabel.isPresent) {
            model["personInfo"] = recognitionLabel.get()
        }

        val settings = model.getAttribute("settings") as Settings

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            var metadataList: MutableIterable<Metadata>? = null
            if (currentUserObj.getAuthority() == model.getAttribute("userRole")) {
                metadataList = metadataRepository?.findAlbumPhotoByPerson(settings.getRecognitionConfidenceThreshold()!!,personId,currentUserObj.getId(),0,2000)
            } else if (currentUserObj.getAuthority() == model.getAttribute("adminRole")) {
                metadataList = metadataRepository?.findMetadataByPerson(settings.getRecognitionConfidenceThreshold()!!,personId,0,2000)
            }
            if (metadataList != null && metadataList.count() > 0) {
                counts["person"] = metadataList.count()
            }
        }

        // Get records of photos that haven't been confirmed - Threshold not 9.0 and greater than threshold configured
        val lowMatchResults = metadataRepository?.findLowMatchesByPerson(personId,settings.getRecognitionConfidenceThreshold()!!)
        if (lowMatchResults != null && lowMatchResults.count() > 0) {
            counts["matches"] = lowMatchResults.count()
            model["lowMatchResults"] = lowMatchResults
            model["message"] = ""

            val labelPhotoMap = mutableMapOf<String, String>()
            for (metadata in lowMatchResults) {
                val recognitionLabelPhotos = recognitionLabelPhotoRepository?.findByMetadataId(metadata.getId())
                var labelString = ""
                if (recognitionLabelPhotos != null) {
                    for (recognitionLabelPhoto in recognitionLabelPhotos) {
                        val recognitionLabelObj = recognitionLabelRepository?.findById(recognitionLabelPhoto.getRecognitionLabelId()!!)
                        if (recognitionLabelObj != null && recognitionLabelObj.get().getName() != "object") {
                            labelString += recognitionLabelObj.get().getName() + ","
                        }
                    }
                }
                if (labelString.isNotBlank()) {
                    labelString = labelString.dropLast(1)
                    labelPhotoMap[metadata.getId()] = labelString
                }
            }
            model["labelPhotoMap"] = labelPhotoMap

            val keywordList = keywordRepository!!.findAllKeywordsGroupedByMetadataId()
            val keywordMap = mutableMapOf<String, String>()
            for (keywordGroup in keywordList) {
                keywordMap[keywordGroup.getMetadataId()!!] = keywordGroup.getKeywords()!!
            }
            model["keywordMap"] = keywordMap
        }

        model["msg"] = ""
        model["status"] = ApiResponse.SUCCESS.status
        model["counts"] = counts
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @GetMapping("/people")
    @Secured("ROLE_ADMIN", "ROLE_USER")
    fun getPeople(model: Model): String {
        val module = "people"
        model["message"] = "There are no people tagged."
        model["peopleList"] = mutableListOf<MetadataPeople>()
        val counts = HashMap<Int,Int>()
        model["counts"] = counts

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            var peopleList: MutableIterable<MetadataPeople>? = null
            val settings = model.getAttribute("settings") as Settings

            if (currentUserObj.getAuthority() == model.getAttribute("userRole")) {
                peopleList = metadataRepository?.findAlbumPhotoByPeople(settings.getRecognitionConfidenceThreshold()!!,currentUserObj.getId())
            } else if (currentUserObj.getAuthority() == model.getAttribute("adminRole")) {
                peopleList = metadataRepository?.findMetadataByPeople(settings.getRecognitionConfidenceThreshold()!!)

                if (peopleList != null && peopleList.count() > 0) {
                    for (person in peopleList) {
                        val lowMatchResults = metadataRepository?.findLowMatchesByPerson(person.getId()!!,settings.getRecognitionConfidenceThreshold()!!)
                        if (lowMatchResults != null && lowMatchResults.count() > 0) {
                            counts[person.getId()!!] = lowMatchResults.count()
                        }
                    }
                    model["counts"] = counts
                }
            }
            if (peopleList != null && peopleList.count() > 0) {
                model["peopleList"] = peopleList
                model["message"] = ""
            }
        }

        model["msg"] = ""
        model["status"] = ApiResponse.SUCCESS.status
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RequestMapping(value = ["/person/{personId}"], method = [RequestMethod.GET])
    @Secured("ROLE_ADMIN", "ROLE_USER")
    fun getPerson(model: Model, @PathVariable personId: Int): String {
        val module = "person"
        val page = 0
        val response = buildPersonAlbum(model,personId,page)
        for ((k, v) in response) {
            model[k] = v!!
        }

        // val person = mapper.convertValue(response["personInfo"], object : TypeReference<Map<String, Any>>() {})

        model["msg"] = ""
        model["status"] = ApiResponse.SUCCESS.status
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RequestMapping(value = ["/person/metadata/{personId}/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedPersonMetadataList(model: Model, request: HttpServletRequest, @PathVariable personId: Int, @PathVariable page: Int): String {
        val response = mutableMapOf<String, Any?>()
        response["message"] = ""
        response["metadataList"] = mutableListOf<Metadata>()
        response["msg"] = "Could not get results"
        response["status"] = ApiResponse.FAIL.status

        if (model.getAttribute("currentUser") != "") {
            val currentUserObj = model.getAttribute("currentUser") as User?
            val settings = model.getAttribute("settings") as Settings
            val queryLimit = model.getAttribute("queryLimit").toString().toInt()
            val pageValue = page*queryLimit

            var metadataList: MutableIterable<Metadata>? = mutableListOf(Metadata())
            if (currentUserObj!!.getAuthority() == model.getAttribute("userRole")) {
                metadataList = metadataRepository?.findAlbumPhotoByPerson(
                    settings.getRecognitionConfidenceThreshold()!!,
                    personId,
                    currentUserObj.getId(),
                    pageValue,
                    queryLimit
                )
                response["msg"] = ""
                response["status"] = ApiResponse.SUCCESS.status
            } else if (currentUserObj.getAuthority() == model.getAttribute("adminRole")) {
                metadataList = metadataRepository?.findMetadataByPerson(
                    settings.getRecognitionConfidenceThreshold()!!,
                    personId,
                    pageValue,
                    queryLimit
                )
                response["msg"] = ""
                response["status"] = ApiResponse.SUCCESS.status
            }

            response["metadataList"] = metadataList
        }

        return mapper.writeValueAsString(response)
    }

    @RequestMapping(value = ["/person/{personId}/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedPerson(model: Model, request: HttpServletRequest, @PathVariable personId: Int, @PathVariable page: Int): String {
        return mapper.writeValueAsString(buildPersonAlbum(model,personId,page))
    }

    private fun buildPersonAlbum(model: Model,personId: Int,page: Int): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        response["message"] = "There are no photos."
        response["metadataList"] = mutableListOf<Metadata>()
        response["labelPhotoMap"] = mutableMapOf<String, Any>()
        response["personInfo"] = RecognitionLabel()
        response["recognitionLabels"] = mutableListOf<RecognitionLabel>()
        response["parameter"] = personId
        response["keywordMap"] = mutableMapOf<String, Any>()
        val counts = HashMap<String,Int>()
        counts["person"] = 0
        counts["matches"] = 0
        response["counts"] = counts
        response["canEdit"] = model.getAttribute("authority") == adminRole

        response["msg"] = "Could not get results"
        response["status"] = ApiResponse.FAIL.status

        if (model.getAttribute("currentUser") != "") {
            val currentUserObj = model.getAttribute("currentUser") as User?
            val settings = model.getAttribute("settings") as Settings
            val queryLimit = model.getAttribute("queryLimit").toString().toInt()
            val pageValue = page*queryLimit

            response["currentUser"] = currentUserObj

            val recognitionLabel = recognitionLabelRepository?.findById(personId)
            if (recognitionLabel != null && recognitionLabel.isPresent) {
                response["personInfo"] = recognitionLabel.get()
            }

            // Get records of photos that haven't been confirmed - Threshold not 9.0 and greater than threshold configured
            val lowMatchResults = metadataRepository?.findLowMatchesByPerson(personId,settings.getRecognitionConfidenceThreshold()!!)
            if (lowMatchResults != null && lowMatchResults.count() > 0) {
                counts["matches"] = lowMatchResults.count()
            }

            var metadataList: MutableIterable<Metadata>? = null
            var completeMetadataList: MutableIterable<Metadata>? = null
            if (currentUserObj!!.getAuthority() == model.getAttribute("userRole")) {
                metadataList = metadataRepository?.findAlbumPhotoByPerson(settings.getRecognitionConfidenceThreshold()!!,personId,currentUserObj.getId(),pageValue,queryLimit)
                completeMetadataList = metadataRepository?.findAlbumPhotoByPerson(settings.getRecognitionConfidenceThreshold()!!,personId,currentUserObj.getId(),0,9999)

            } else if (currentUserObj.getAuthority() == model.getAttribute("adminRole")) {
                val recognitionLabels = recognitionLabelRepository?.findAllByNameNotContaining("object")
                if (recognitionLabels != null && recognitionLabels.count() > 0) {
                    response["recognitionLabels"] = recognitionLabels
                }
                metadataList = metadataRepository?.findMetadataByPerson(settings.getRecognitionConfidenceThreshold()!!,personId,pageValue,queryLimit)
                completeMetadataList = metadataRepository?.findMetadataByPerson(settings.getRecognitionConfidenceThreshold()!!,personId,0,9999)
            }

            if (metadataList != null && metadataList.count() > 0) {
                if (completeMetadataList != null) {
                    counts["person"] = completeMetadataList.count()
                }
                response["message"] = ""

                val labelPhotoMap = mutableMapOf<String, MutableMap<String,Any>>()
                for (metadata in metadataList) {
                    val recognitionLabelPhotos = recognitionLabelPhotoRepository?.findByMetadataId(metadata.getId())
                    var labelString = ""
                    var isAutoTagged = false
                    val nameTaggedMap = mutableMapOf<String,Any>()
                    if (recognitionLabelPhotos != null) {
                        for (recognitionLabelPhoto in recognitionLabelPhotos) {
                            val recognitionLabelObj = recognitionLabelRepository?.findById(recognitionLabelPhoto.getRecognitionLabelId()!!)
                            if (recognitionLabelObj != null && recognitionLabelObj.get().getName() != "object") {
                                labelString += recognitionLabelObj.get().getName() + ","
                            }
                            if (!isAutoTagged) {
                                isAutoTagged = recognitionLabelPhoto.getAutoTagged() == true
                            }
                        }
                    }
                    if (labelString.isNotBlank()) {
                        labelString = labelString.dropLast(1)
                        nameTaggedMap["labels"] = labelString
                        nameTaggedMap["isTagged"] = isAutoTagged
                        labelPhotoMap[metadata.getId()] = nameTaggedMap
                    }
                }
                response["labelPhotoMap"] = labelPhotoMap
                response["metadataList"] = metadataList
                val keywordList = keywordRepository!!.findAllKeywordsGroupedByMetadataId()
                val keywordMap = mutableMapOf<String, String>()
                for (keywordGroup in keywordList) {
                    keywordMap[keywordGroup.getMetadataId()!!] = keywordGroup.getKeywords()!!
                }
                response["keywordMap"] = keywordMap
            }

            response["counts"] = counts

            response["msg"] = "Results"
            response["status"] = ApiResponse.SUCCESS.status
        }

        return response
    }

    @RequestMapping(value = ["/person/update"], method = [RequestMethod.POST], produces = ["application/json"])
    @Secured("ROLE_ADMIN")
    @ResponseBody
    fun postPersonUpdate(model: Model, @RequestBody requestBody: JsonNode): String {
        val personMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (personMap.containsKey("metadataId") &&
            personMap.containsKey("tagpeople") &&
            personMap.containsKey("isObject")
        ) {
            val metadataId = StringEscapeUtils.escapeHtml4(personMap["metadataId"].toString())
            val isObject = personMap["isObject"].toString().toBoolean()
            val recognitionLabelArray = personMap["tagpeople"].toString().split(",")

            if (personMap.containsKey("currentPerson") && personMap["currentPerson"].toString() != "") {
                val recognitionLabel = personMap["currentPerson"].toString()
                val compreFaceImageIdMap = mutableMapOf<String, Any?>()

                if (recognitionLabel.trim().isNotBlank()) {
                    val recognitionLabelRecord =
                        recognitionLabelRepository?.findByNameIgnoreCase(recognitionLabel.trim())
                    if (recognitionLabelRecord != null) {
                        val recognitionLabelPhoto = recognitionLabelPhotoRepository?.findByRecognitionLabelIdAndMetadataId(recognitionLabelRecord.getId(), metadataId)
                        if (recognitionLabelPhoto != null && recognitionLabelPhoto.getCompreFaceImageId()!!.isNotEmpty()) {
                            compreFaceImageIdMap["${recognitionLabel.replace("\\s".toRegex(), "")}-$metadataId"] = recognitionLabelPhoto.getCompreFaceImageId()!!
                        }
                    }
                }

                if (recognitionLabelArray.count() > 0) {
                    recognitionLabelPhotoRepository?.deleteByMetadataId(metadataId)
                }

                for (recognitionLabelString in recognitionLabelArray) {
                    if (recognitionLabelString.trim().isNotBlank()) {
                        val recognitionLabelRecord =
                            recognitionLabelRepository?.findByNameIgnoreCase(recognitionLabelString.trim())
                        var recognitionLabelObj = RecognitionLabel()
                        if (recognitionLabelRecord == null) {
                            recognitionLabelObj.setName(recognitionLabelString.trim())
                            recognitionLabelObj.setCreatedAt(getCurrentTimestamp())
                            recognitionLabelObj.setModifiedAt(getCurrentTimestamp())
                            recognitionLabelRepository?.save(recognitionLabelObj)
                        } else {
                            recognitionLabelObj = recognitionLabelRecord
                        }
                        val recognitionLabelPhotoCount =
                            recognitionLabelPhotoRepository?.countByRecognitionLabelIdAndMetadataId(
                                recognitionLabelObj.getId(),
                                metadataId
                            )
                        if (recognitionLabelPhotoCount == 0) {
                            val metadata = metadataRepository?.findById(metadataId)
                            val uploadResp = mapper.writeValueAsString(buildPersonUpload(model.getAttribute("settings") as Settings, recognitionLabelString.trim(), metadata?.get(), compreFaceImageIdMap))
                            val jsonRespObj = mapper.readTree(uploadResp)

                            var compreFaceImageId: String? = null
                            if (jsonRespObj.has("responseDataUpload") && jsonRespObj["responseDataUpload"].has("image_id")) {
                                compreFaceImageId = jsonRespObj["responseDataUpload"]["image_id"].toString()
                                compreFaceImageId = compreFaceImageId.drop(1).dropLast(1)
                            } else if (jsonRespObj.has("responseData") && jsonRespObj["responseData"].has("image_id")) {
                                compreFaceImageId = jsonRespObj["responseData"]["image_id"].toString()
                                compreFaceImageId = compreFaceImageId.drop(1).dropLast(1)
                            }

                            val recognitionLabelPhotoObj = RecognitionLabelPhoto()
                            recognitionLabelPhotoObj.setMetadataId(metadataId)
                            recognitionLabelPhotoObj.setRecognitionLabelId(recognitionLabelObj.getId())
                            recognitionLabelPhotoObj.setConfidence("0.0")
                            recognitionLabelPhotoObj.setCompreFaceImageId(compreFaceImageId)
                            recognitionLabelPhotoRepository?.save(recognitionLabelPhotoObj)
                        }
                    }
                }

                resp["recognitionLabels"] = mutableListOf<RecognitionLabel>()
                val recognitionLabels = recognitionLabelRepository?.findAllByNameNotContaining("object")
                if (recognitionLabels != null && recognitionLabels.count() > 0) {
                    resp["recognitionLabels"] = recognitionLabels
                }

                resp["msg"] = "Saved"
                resp["status"] = ApiResponse.SUCCESS.status
                return mapper.writeValueAsString(resp)
            } else if (isObject) {
                recognitionLabelPhotoRepository?.deleteByMetadataId(metadataId)
                val recognitionLabelRecord = recognitionLabelRepository?.findByNameIgnoreCase("object")
                var recognitionLabelObj = RecognitionLabel()
                if (recognitionLabelRecord == null) {
                    recognitionLabelObj.setName("object")
                    recognitionLabelObj.setCreatedAt(getCurrentTimestamp())
                    recognitionLabelObj.setModifiedAt(getCurrentTimestamp())
                    recognitionLabelRepository?.save(recognitionLabelObj)
                } else {
                    recognitionLabelObj = recognitionLabelRecord
                }

                val recognitionLabelPhotoObj = RecognitionLabelPhoto()
                recognitionLabelPhotoObj.setMetadataId(metadataId)
                recognitionLabelPhotoObj.setRecognitionLabelId(recognitionLabelObj.getId())
                recognitionLabelPhotoObj.setConfidence("-0.1")
                recognitionLabelPhotoRepository?.save(recognitionLabelPhotoObj)

                resp["recognitionLabels"] = mutableListOf<RecognitionLabel>()
                val recognitionLabels = recognitionLabelRepository?.findAllByNameNotContaining("object")
                if (recognitionLabels != null && recognitionLabels.count() > 0) {
                    resp["recognitionLabels"] = recognitionLabels
                }

                resp["msg"] = "Saved"
                resp["status"] = ApiResponse.SUCCESS.status
                return mapper.writeValueAsString(resp)
            } else if (personMap["tagpeople"].toString().isBlank()) {
                recognitionLabelPhotoRepository?.deleteByMetadataId(metadataId)

                resp["recognitionLabels"] = mutableListOf<RecognitionLabel>()
                val recognitionLabels = recognitionLabelRepository?.findAllByNameNotContaining("object")
                if (recognitionLabels != null && recognitionLabels.count() > 0) {
                    resp["recognitionLabels"] = recognitionLabels
                }

                resp["msg"] = "Saved"
                resp["status"] = ApiResponse.SUCCESS.status
                return mapper.writeValueAsString(resp)
            }
        }

        resp["msg"] = "Could not save"
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/person/recognition/faces"], method = [RequestMethod.POST], produces = ["application/json"])
    @Secured("ROLE_ADMIN")
    @ResponseBody
    fun postPersonUpload(model: Model, @RequestBody requestBody: JsonNode): String {
        val personMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})

        resp["responseData"] = mutableMapOf<String, Any?>()
        resp["msg"] = ""
        resp["status"] = ApiResponse.FAIL.status

        if (personMap.containsKey("personName") &&
            personMap.containsKey("metadataId")
        ) {
            val personName = personMap["personName"].toString()
            val metadataId = personMap["metadataId"].toString()
            val metadata = metadataRepository?.findById(metadataId)
            val compreFaceImageIdMap = mutableMapOf<String, Any?>()
            return mapper.writeValueAsString(buildPersonUpload(model.getAttribute("settings") as Settings, personName, metadata?.get(), compreFaceImageIdMap))
        }

        return mapper.writeValueAsString(resp)
    }

    private fun processPeople(settings: Settings, metadataObj: Metadata?, taggedPeople: String?, isObject: Boolean) {
        if (metadataObj != null) {
            val metadataId = metadataObj.getId()

            val recognitionLabelPhotos = recognitionLabelPhotoRepository?.findByMetadataId(metadataId)
            if (recognitionLabelPhotos != null) {
                for (recognitionLabelPhoto in recognitionLabelPhotos) {
                    recognitionLabelPhotoRepository?.delete(recognitionLabelPhoto)
                }
            }

            if (taggedPeople != null && taggedPeople.trim() != "") {
                val recognitionLabelArray = StringEscapeUtils.escapeHtml4(taggedPeople).split(",")
                if (recognitionLabelArray.count() > 0) {
                    recognitionLabelPhotoRepository?.deleteByMetadataId(metadataId)
                }

                val compreFaceImageIdMap = mutableMapOf<String, Any?>()

                for (recognitionLabel in recognitionLabelArray) {
                    if (recognitionLabel.trim().isNotBlank()) {
                        val uploadResp = mapper.writeValueAsString(buildPersonUpload(settings, recognitionLabel, metadataObj, compreFaceImageIdMap))
                        val jsonRespObj = mapper.readTree(uploadResp)

                        var compreFaceImageId: String? = null
                        if (jsonRespObj.has("responseDataUpload") && jsonRespObj["responseDataUpload"].has("image_id")) {
                            compreFaceImageId = jsonRespObj["responseDataUpload"]["image_id"].toString()
                            compreFaceImageId = compreFaceImageId.drop(1).dropLast(1)
                        } else if (jsonRespObj.has("responseData") && jsonRespObj["responseData"].has("image_id")) {
                            compreFaceImageId = jsonRespObj["responseData"]["image_id"].toString()
                            compreFaceImageId = compreFaceImageId.drop(1).dropLast(1)
                        }

                        val recognitionLabelRecord =
                            recognitionLabelRepository?.findByNameIgnoreCase(recognitionLabel.trim())
                        var recognitionLabelObj = RecognitionLabel()
                        if (recognitionLabelRecord == null) {
                            recognitionLabelObj.setName(recognitionLabel.trim())
                            recognitionLabelObj.setCreatedAt(getCurrentTimestamp())
                            recognitionLabelObj.setModifiedAt(getCurrentTimestamp())
                            recognitionLabelRepository?.save(recognitionLabelObj)
                        } else {
                            recognitionLabelObj = recognitionLabelRecord
                        }
                        val recognitionLabelPhotoCount =
                            recognitionLabelPhotoRepository?.countByRecognitionLabelIdAndMetadataId(
                                recognitionLabelObj.getId(),
                                metadataId
                            )
                        if (recognitionLabelPhotoCount == 0) {
                            val recognitionLabelPhotoObj = RecognitionLabelPhoto()
                            recognitionLabelPhotoObj.setMetadataId(metadataObj.getId())
                            recognitionLabelPhotoObj.setRecognitionLabelId(recognitionLabelObj.getId())
                            recognitionLabelPhotoObj.setConfidence("0.0")
                            recognitionLabelPhotoObj.setCompreFaceImageId(compreFaceImageId)
                            recognitionLabelPhotoRepository?.save(recognitionLabelPhotoObj)
                        }
                    }
                }
            } else if (isObject) {
                recognitionLabelPhotoRepository?.deleteByMetadataId(metadataId)
                val recognitionLabelRecord = recognitionLabelRepository?.findByNameIgnoreCase("object")
                var recognitionLabelObj = RecognitionLabel()
                if (recognitionLabelRecord == null) {
                    recognitionLabelObj.setName("object")
                    recognitionLabelObj.setCreatedAt(getCurrentTimestamp())
                    recognitionLabelObj.setModifiedAt(getCurrentTimestamp())
                    recognitionLabelRepository?.save(recognitionLabelObj)
                } else {
                    recognitionLabelObj = recognitionLabelRecord
                }

                val recognitionLabelPhotoObj = RecognitionLabelPhoto()
                recognitionLabelPhotoObj.setMetadataId(metadataId)
                recognitionLabelPhotoObj.setRecognitionLabelId(recognitionLabelObj.getId())
                recognitionLabelPhotoObj.setConfidence("-0.1")
                recognitionLabelPhotoRepository?.save(recognitionLabelPhotoObj)
            }
        }
    }

    private fun buildPersonUpload(settings: Settings, personName: String?, metadata: Metadata?, compreFaceImageIdMap: MutableMap<String, Any?>): MutableMap<String, Any?> {
        val uploadresponse = mutableMapOf<String, Any?>()
        uploadresponse["responseData"] = mutableMapOf<String, Any?>()
        uploadresponse["similarity"] = 0.0

        uploadresponse["msg"] = ""
        uploadresponse["status"] = ApiResponse.FAIL.status

        if (settings.getCompreFaceKey() != null && settings.getCompreFaceKey()!!.isNotBlank() &&
            settings.getCompreFaceServer() != null && settings.getCompreFaceServer()!!.isNotBlank()) {
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

                    val compreFaceImageId = compreFaceImageIdMap[subject.filterNot {it.isWhitespace()} + "-" + metadata?.getId()]
                    if (compreFaceImageId != null && compreFaceImageId.toString().isNotEmpty()) {
                        webClient.delete()
                            .uri("api/v1/recognition/faces/$compreFaceImageId")
                            .header("x-api-key", settings.getCompreFaceKey())
                            .retrieve()
                            .bodyToMono(String::class.java)
                            .block()
                    }
                }

                // Uploaded faces
                if (similarity != 1.0 && (similarity == 0.0 || similarity >= settings.getRecognitionConfidenceThreshold().toString().toDouble())) {
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
                            uploadresponse["responseData"] = jsonObj

                            uploadresponse["msg"] = ""
                            uploadresponse["status"] = ApiResponse.SUCCESS.status
                        } else {
                            response = "Metadata not found."
                            uploadresponse["responseData"] = response
                        }
                    } catch (e: Exception) {
                        val errorResponse =
                            e.localizedMessage.replace("<EOL>", "").replace("400 : ", "").replace("\\s".toRegex(), "")
                        uploadresponse["responseData"] = errorResponse
                    }
                } else {
                    uploadresponse["msg"] = "Duplicate image"
                    uploadresponse["status"] = ApiResponse.FAIL.status
                }
            } else {
                uploadresponse["msg"] = "Person name or metadata ID blank"
                uploadresponse["status"] = ApiResponse.FAIL.status
            }
        }

        return uploadresponse
    }

    private fun buildPersonRecognition(settings: Settings, metadata: Metadata?): MutableMap<String, Any?> {
        val recogresponse = mutableMapOf<String, Any?>()

        recogresponse["recognizeData"] = mutableMapOf<String, Any?>()
        recogresponse["msg"] = ""
        recogresponse["status"] = ApiResponse.FAIL.status

        if (settings.getCompreFaceKey() != null && settings.getCompreFaceKey()!!.isNotBlank() &&
            settings.getCompreFaceServer() != null && settings.getCompreFaceServer()!!.isNotBlank()) {
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
                    val subjects = resultList[0]["subjects"] as ArrayList<Map<String, Any>>

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

    @RequestMapping(value = ["/person/recognition/recognize/{metadataId}"], method = [RequestMethod.GET], produces = ["application/json"])
    @Secured("ROLE_ADMIN")
    @ResponseBody
    fun postPersonRecognize(model: Model, @PathVariable metadataId: String): String {
        resp["responseData"] = mutableMapOf<String, Any?>()
        resp["msg"] = ""
        resp["status"] = ApiResponse.FAIL.status

        val metadata = metadataRepository?.findByMetadataId(metadataId)
        return mapper.writeValueAsString(buildPersonRecognition(model.getAttribute("settings") as Settings, metadata))
    }
}