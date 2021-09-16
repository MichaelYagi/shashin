package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.component.FaceRecognizer
import com.miyagi.shashin.component.Message
import com.miyagi.shashin.component.ScanMessage
import com.miyagi.shashin.model.*
import com.miyagi.shashin.repository.AlbumRepository
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.repository.RecognitionLabelPhotoRepository
import com.miyagi.shashin.repository.RecognitionLabelRepository
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.event.EventListener
import org.springframework.data.repository.query.Param
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.annotation.SubscribeMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import org.springframework.web.socket.messaging.SessionConnectEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import org.springframework.web.socket.messaging.SessionSubscribeEvent
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.ArrayList
import java.util.HashMap
import javax.servlet.http.HttpSession

@Controller
class PeopleController {

    @Autowired
    private var metadataRepository: MetadataRepository? = null

    @Autowired
    private var albumRepository: AlbumRepository? = null

    @Autowired
    private var recognitionLabelRepository: RecognitionLabelRepository? = null

    @Autowired
    private var recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository? = null

    private val threadExtensionName: String = "facescan_shashinscan"

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()

    @MessageMapping("/matchmessage")
    @SendTo("/topic/matchmessages")
    @Throws(java.lang.Exception::class)
    fun sendMatcnMessage(message: ScanMessage): Message? {
        var msg = "Start Matching"

        if (!FileUtils.checkThreadFileAlive(threadExtensionName)) {
            msg = "Matching Complete"
        }

        val threadFileContent = FileUtils.readThreadFile(threadExtensionName)
        if (threadFileContent != null) {
            msg = "Matching in progress: " + threadFileContent.replace("\\", "/")
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

    @RequestMapping(value = ["/person/matches/deletethread"], method = [RequestMethod.GET], produces = ["application/json"])
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    fun deleteThread(model: Model): String {
        FileUtils.deleteThreadFiles("facescan_shashinscan")
        resp["msg"] = "Thread file manually deleted"
        resp["status"] = "success"
        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/person/matches/start"], method = [RequestMethod.GET], produces = ["application/json"])
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    fun startPredictions(model: Model): String {
        val settings = model.getAttribute("settings") as Settings

        // Scan records of photos that haven't been scanned in a separate thread
        val testImages = metadataRepository?.findNonMatched(settings.getMatchScanLimit()!!)
        val trainingData = metadataRepository?.findTrainingData(settings.getRecognitionConfidenceThreshold()!!, settings.getTrainingDataLimit()!!)
        val distinctLabelRecords = this.recognitionLabelPhotoRepository?.findGroupByRecognitionLabelId()

        // Start matching in a separate thread
        if (testImages != null && trainingData != null && distinctLabelRecords != null && distinctLabelRecords.count() > 1) {
            val faceRecognizer = FaceRecognizer(testImages, trainingData, recognitionLabelPhotoRepository, recognitionLabelRepository)
            faceRecognizer.runRecognizer()
        } else {
            resp["msg"] = "Training data not detected. At least 2 people must be tagged."
            resp["status"] = "fail"
            return mapper.writeValueAsString(resp)
        }

        resp["msg"] = "Start Matching"
        resp["status"] = "success"
        return mapper.writeValueAsString(resp)
    }

    @GetMapping("/person/matches/{personId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun getPredictions(model: Model, @PathVariable personId: Int): String {
        val module = "matches"
        model["message"] = ""
        model["lowMatchResults"] = ""
        model["recognitionLabels"] = ""
        model["labelPhotoMap"] = ""
        model["parameter"] = personId

        val recognitionLabels = recognitionLabelRepository?.findAllByNameNotContaining("object")
        if (recognitionLabels != null && recognitionLabels.count() > 0) {
            model["recognitionLabels"] = recognitionLabels
        }

        val recognitionLabel = recognitionLabelRepository?.findById(personId)
        if (recognitionLabel != null) {
            model["personInfo"] = recognitionLabel.get()
        }

        val settings = model.getAttribute("settings") as Settings
        // Get records of photos that haven't been confirmed - Threshold not 9.0 and greater than threshold configured
        val lowMatchResults = metadataRepository?.findLowMatchesByPerson(personId,settings.getRecognitionConfidenceThreshold()!!)
        if (lowMatchResults != null && lowMatchResults.count() > 0) {
            model["lowMatchResults"] = lowMatchResults

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
                }
                labelPhotoMap[metadata.getId()] = labelString
            }
            model["labelPhotoMap"] = labelPhotoMap
        }

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @GetMapping("/people")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    fun getPeople(model: Model): String {
        val module = "people"
        model["message"] = "There are no people tagged."
        model["peopleList"] = ""

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            var peopleList: MutableIterable<MetadataPeople>? = null
            val settings = model.getAttribute("settings") as Settings

            if (currentUserObj.getAuthority() == model.getAttribute("userRole")) {
                peopleList = metadataRepository?.findAlbumPhotoByPeople(settings.getRecognitionConfidenceThreshold()!!)
            } else if (currentUserObj.getAuthority() == model.getAttribute("adminRole")) {
                peopleList = metadataRepository?.findMetadataByPeople(settings.getRecognitionConfidenceThreshold()!!)
            }
            if (peopleList != null && peopleList.count() > 0) {
                model["peopleList"] = peopleList
                model["message"] = ""
            }
        }

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RequestMapping(value = ["/person/{personId}"], method = [RequestMethod.GET])
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    fun getPagedTimeline(model: Model, @PathVariable personId: Int): String {
        val module = "person"
        val page = 0
        val response = buildPersonAlbum(model,personId,page)
        model["message"] = response["message"]!!
        model["metadataList"] = response["metadataList"]!!
        model["recognitionLabels"] = response["recognitionLabels"]!!
        model["labelPhotoMap"] = response["labelPhotoMap"]!!
        model["personInfo"] = response["personInfo"]!!
        model["parameter"] = response["parameter"]!!

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    private fun buildPersonAlbum(model: Model,personId: Int,page: Int): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        response["message"] = "There are no photos.."
        response["metadataList"] = ""
        response["labelPhotoMap"] = ""
        response["personInfo"] = ""
        response["recognitionLabels"] = ""
        response["parameter"] = personId

        response["msg"] = "Could not get results"
        response["status"] = "fail"

        if (model.getAttribute("currentUser") != "") {
            val currentUserObj = model.getAttribute("currentUser") as User?
//            val queryLimit = model.getAttribute("queryLimit").toString().toInt()
//            val pageValue = page*queryLimit

            val recognitionLabel = recognitionLabelRepository?.findById(personId)
            if (recognitionLabel != null) {
                response["personInfo"] = recognitionLabel.get()
            }

            var metadataList: MutableIterable<Metadata>? = null
            val settings = model.getAttribute("settings") as Settings
            if (currentUserObj!!.getAuthority() == model.getAttribute("userRole")) {
                metadataList = metadataRepository?.findAlbumPhotoByPerson(settings.getRecognitionConfidenceThreshold()!!,personId,currentUserObj.getId(),page,2000)
            } else if (currentUserObj.getAuthority() == model.getAttribute("adminRole")) {
                val recognitionLabels = recognitionLabelRepository?.findAllByNameNotContaining("object")
                if (recognitionLabels != null && recognitionLabels.count() > 0) {
                    response["recognitionLabels"] = recognitionLabels
                }
                metadataList = metadataRepository?.findMetadataByPerson(settings.getRecognitionConfidenceThreshold()!!,personId,page,2000)
            }

            response["metadataList"] = metadataList
            if (metadataList != null && metadataList.count() > 0) {
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
                    }
                    nameTaggedMap["labels"] = labelString
                    nameTaggedMap["isTagged"] = isAutoTagged
                    labelPhotoMap[metadata.getId()] = nameTaggedMap
                }
                response["labelPhotoMap"] = labelPhotoMap
            }

            response["metadataList"] = metadataList
            response["msg"] = "Results"
            response["status"] = "success"
        }

        return response
    }

    @RequestMapping(value = ["/person/update"], method = [RequestMethod.POST], produces = ["application/json"])
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    fun postPersonUpdate(model: Model, @RequestBody requestBody: JsonNode): String {
        val personMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (personMap.containsKey("metadataId") &&
            personMap.containsKey("tagpeople") &&
            personMap.containsKey("isObject")
        ) {
            val metadataId = personMap.get("metadataId").toString()
            val isObject = personMap.get("isObject").toString().toBoolean()

            if (personMap["tagpeople"].toString() != "") {
                val recognitionLabelArray = personMap["tagpeople"].toString().split(",")
                if (recognitionLabelArray.count() > 0) {
                    recognitionLabelPhotoRepository?.deleteByMetadataId(metadataId)
                }
                for (recognitionLabel in recognitionLabelArray) {
                    val recognitionLabelRecord = recognitionLabelRepository?.findByNameIgnoreCase(recognitionLabel.trim())
                    var recognitionLabelObj = RecognitionLabel()
                    if (recognitionLabelRecord == null) {
                        recognitionLabelObj.setName(recognitionLabel.trim())
                        val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        val now = LocalDateTime.now()
                        recognitionLabelObj.setCreatedAt(dtf.format(now))
                        recognitionLabelObj.setModifiedAt(dtf.format(now))
                        recognitionLabelRepository?.save(recognitionLabelObj)
                    } else {
                        recognitionLabelObj = recognitionLabelRecord
                    }
                    val recognitionLabelPhotoCount = recognitionLabelPhotoRepository?.countByRecognitionLabelIdAndMetadataId(recognitionLabelObj.getId(),metadataId)
                    if (recognitionLabelPhotoCount == 0) {
                        val recognitionLabelPhotoObj = RecognitionLabelPhoto()
                        recognitionLabelPhotoObj.setMetadataId(metadataId)
                        recognitionLabelPhotoObj.setRecognitionLabelId(recognitionLabelObj.getId())
                        recognitionLabelPhotoObj.setConfidence("0.0")
                        recognitionLabelPhotoRepository?.save(recognitionLabelPhotoObj)
                    }
                }

                resp["msg"] = "Saved"
                resp["status"] = "success"
                return mapper.writeValueAsString(resp)
            } else if (isObject) {
                recognitionLabelPhotoRepository?.deleteByMetadataId(metadataId)
                val recognitionLabelRecord = recognitionLabelRepository?.findByNameIgnoreCase("object")
                var recognitionLabelObj = RecognitionLabel()
                if (recognitionLabelRecord == null) {
                    recognitionLabelObj.setName("object")
                    val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    val now = LocalDateTime.now()
                    recognitionLabelObj.setCreatedAt(dtf.format(now))
                    recognitionLabelObj.setModifiedAt(dtf.format(now))
                    recognitionLabelRepository?.save(recognitionLabelObj)
                } else {
                    recognitionLabelObj = recognitionLabelRecord
                }

                val recognitionLabelPhotoObj = RecognitionLabelPhoto()
                recognitionLabelPhotoObj.setMetadataId(metadataId)
                recognitionLabelPhotoObj.setRecognitionLabelId(recognitionLabelObj.getId())
                recognitionLabelPhotoObj.setConfidence("-0.1")
                recognitionLabelPhotoRepository?.save(recognitionLabelPhotoObj)

                resp["msg"] = "Saved"
                resp["status"] = "success"
                return mapper.writeValueAsString(resp)
            } else if (personMap["tagpeople"].toString().isBlank()) {
                recognitionLabelPhotoRepository?.deleteByMetadataId(metadataId)

                resp["msg"] = "Saved"
                resp["status"] = "success"
                return mapper.writeValueAsString(resp)
            }
        }

        resp["msg"] = "Could not save"
        resp["status"] = "fail"
        return mapper.writeValueAsString(resp)
    }

}