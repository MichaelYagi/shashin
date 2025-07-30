package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.ShashinApplication
import com.miyagi.shashin.component.Message
import com.miyagi.shashin.component.ScanMessage
import com.miyagi.shashin.model.*
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.*
import com.miyagi.shashin.util.ImageProcessing.Companion.buildObjectRecognitionCriteria
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import org.apache.commons.text.StringEscapeUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.annotation.SubscribeMapping
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.socket.messaging.SessionConnectEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import org.springframework.web.socket.messaging.SessionSubscribeEvent
import java.io.*
import java.net.URL
import java.net.URLConnection
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import java.util.logging.Logger
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import org.springframework.context.MessageSource
import java.security.Principal

@Suppress("UNCHECKED_CAST")
@Controller
class PeopleController: BaseController() {

    @Autowired
    private var metadataRepository: MetadataRepository? = null

    @Autowired
    private var albumRepository: AlbumRepository? = null

    @Autowired
    private var recognitionLabelRepository: RecognitionLabelRepository? = null

    @Autowired
    private var recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository? = null

    @Autowired
    private lateinit var favoriteRepository: FavoriteRepository

    @Autowired
    private val keywordRepository: KeywordRepository? = null

    @Autowired
    private val keywordPhotoRepository: KeywordPhotoRepository? = null

    @Autowired
    private val notificationRepository: NotificationRepository? = null

    @Autowired
    private var userRepository: UserRepository? = null

    @Autowired
    var messageSource: MessageSource? = null

    @Value("\${app.role.super}")
    private lateinit var superRole: String

    @Value("\${app.role.admin}")
    private lateinit var adminRole: String

    @Value("\${app.sidecar.path}")
    private val relativeSidecarDir: String? = null

    private var shouldStop = AtomicBoolean(false)

    private val threadExtensionName: String = "facescan_shashinscan"

    private var logger: Logger = Logger.getLogger(SettingsController::class.simpleName)

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, Any?>()

    @MessageMapping("/matchmessage")
    @SendTo("/topic/matchmessages")
    @Throws(java.lang.Exception::class)
    fun sendMatcnMessage(message: ScanMessage, principal: Principal): Message? {
        val username = principal.name
        val user = userRepository?.findByUsername(username)
        var lang = "en"
        if (user != null && user.getLanguage() != null && user.getLanguage() != "") {
            lang = user.getLanguage().toString()
        }
        val locale = Locale(lang)

        var msg = "Start Matching"

        if (shouldStop.get()) {
            msg = messageSource?.getMessage("main.pages.matching.cancelled", null, locale).toString()
        } else if (!FileUtils.checkThreadFileAlive(threadExtensionName)) {
            msg = messageSource?.getMessage("main.pages.matching.complete", null, locale).toString()
        } else {
            val threadFileContent = FileUtils.readThreadFile(threadExtensionName)
            if (threadFileContent != null) {
                msg = messageSource?.getMessage("main.pages.matching.scanning.mip", null, locale).toString() + ": " + threadFileContent.replace("\\", "/")
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

    @RequestMapping(value = ["/matches/start"], method = [RequestMethod.POST], produces = ["application/json"])
    @Secured("ROLE_SUPER")
    @ResponseBody
    fun startPredictions(model: Model,@RequestParam stopScan: Boolean, request: HttpServletRequest, locale: Locale): String {
        val settings = model.getAttribute("settings") as Settings

        if (stopScan) {
            shouldStop.set(true)
        }

        if (!FileUtils.checkThreadFileAlive(threadExtensionName)) {
            // Clean up any existing thread files
            FileUtils.deleteThreadFiles(threadExtensionName)

            val superAdmins = userRepository?.findAllByAuthorityEquals(superRole)

            doPrediction(settings, superAdmins, locale)
        }

        resp["msg"] = "Start Matching"
        resp["status"] = ApiResponse.SUCCESS.status
        return mapper.writeValueAsString(resp)
    }

    fun doPrediction(settings: Settings, superAdmins: MutableIterable<User>?, locale: Locale) {
        Thread {
            val threadFile = FileUtils.createThreadFile(threadExtensionName)
            val classLoader: ClassLoader = ShashinApplication::class.java.classLoader
            val vggfaceFileExists = classLoader.getResource("lib/vggface2.pt") != null
            val retinafaceFileExists = classLoader.getResource("lib/retinaface.pt") != null

            if ((!vggfaceFileExists || !retinafaceFileExists) && !NetworkUtils.checkCompreFaceConnection(
                    settings.getCompreFaceServer(),
                    settings.getCompreFaceKey()
                )) {
                if (superAdmins != null) {
                    val notificationObjList = mutableListOf<Notification>()
                    val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                    sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
                    for (admin in superAdmins) {
                        var language = admin.getLanguage()
                        if (language == null) {
                            language = "en"
                        }

                        var locale = Locale(language)
                        val notificationObj = Notification()
                        notificationObj.setUserId(admin.getId())
                        notificationObj.setCreatedAt(getCurrentTimestamp())
                        notificationObj.setModifiedAt(getCurrentTimestamp())
                        notificationObj.setRead(false)
                        notificationObj.setMessage(messageSource?.getMessage("main.notification.people.missing", null, locale))
                        notificationObjList.add(notificationObj)
                    }
                    if (notificationObjList.isNotEmpty()) {
                        notificationRepository?.saveAll(notificationObjList)
                    }
                }
            }

            // Object and person recognition
            if (threadFile != null) {
                var recognitionCount = 0
                if (settings.getFacialDetection() == true) {
                    recognitionCount = ImageProcessing.subjectRecognizer(
                        metadataRepository,
                        recognitionLabelRepository,
                        recognitionLabelPhotoRepository,
                        relativeSidecarDir!!,
                        settings,
                        threadFile,
                        shouldStop,
                        messageSource,
                        locale
                    )
                }

                val adminSupers = userRepository?.findAllByAuthorityEquals(superRole)

                if (adminSupers != null && recognitionCount > 0) {
                    val notificationObjList = mutableListOf<Notification>()
                    val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                    sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
                    for (adminSuper in adminSupers) {
                        var language = adminSuper.getLanguage()
                        if (language == null) {
                            language = "en"
                        }

                        var locale = Locale(language)
                        val notificationObj = Notification()
                        notificationObj.setUserId(adminSuper.getId())
                        notificationObj.setCreatedAt(getCurrentTimestamp())
                        notificationObj.setModifiedAt(getCurrentTimestamp())
                        notificationObj.setRead(false)
                        notificationObj.setMessage(messageSource?.getMessage("main.notification.people.matchcount", arrayOf(recognitionCount), locale) +"- ${sdtf.format(Date())}.")
                        notificationObjList.add(notificationObj)
                    }
                    if (notificationObjList.isNotEmpty()) {
                        notificationRepository?.saveAll(notificationObjList)
                    }
                }

                if (settings.getObjectDetection() == true) {
                    val withoutKeywords = metadataRepository?.findWithoutKeywords(settings.getMatchScanLimit()!!)

                    if (withoutKeywords != null) {
                        val criteria = buildObjectRecognitionCriteria()
                        val threshold = settings.getObjectRecognitionConfidenceThreshold()

                        if (criteria != null) {
                            for (withoutKeyword in withoutKeywords) {
                                if (shouldStop.get()) {
                                    break
                                }

                                val metadataWithoutKeywordsObj =
                                    metadataRepository?.findById(withoutKeyword.getId())?.get()

                                val keywordMap = ImageProcessing.objectRecognizer(
                                    metadataWithoutKeywordsObj!!,
                                    criteria,
                                    threshold.toString().toDouble(),
                                    threadFile,
                                    shouldStop.get(),
                                    messageSource,
                                    locale
                                )

                                ImageProcessing.processObjects(keywordMap.keys.toTypedArray().toList(), metadataWithoutKeywordsObj, keywordRepository!!, keywordPhotoRepository!!, metadataRepository!!)
                            }
                        }
                    }
                }
            }

            shouldStop.set(false)
            FileUtils.deleteThreadFiles(threadExtensionName)
            FileUtils.writeToThreadFileAndLogMessage(messageSource?.getMessage("main.pages.matching.complete", null, locale).toString(), threadFile!!)
        }.start()
    }

    @GetMapping("/person/matches/{personId}")
    @Secured("ROLE_ADMIN","ROLE_SUPER")
    fun getPredictions(model: Model, @PathVariable personId: Int, request: HttpServletRequest, locale: Locale): String {
        val module = "matches"
        model["message"] = messageSource?.getMessage("main.nothing", null, locale)
        model["lowMatchResults"] = mutableListOf<Metadata>()
        model["recognitionLabels"] = mutableListOf<RecognitionLabel>()
        model["allAlbumList"] = mutableListOf<Album>()
        model["labelPhotoMap"] = mutableMapOf<String, Any>()
        model["keywordMap"] = mutableMapOf<String, String>()
        val counts = HashMap<String,Int>()
        counts["person"] = 0
        counts["matches"] = 0
        counts["compreface"] = 0
        model["counts"] = counts
        model["parameter"] = personId

        val recognitionLabels = recognitionLabelRepository?.findAllByNameNotContaining(TextUtils.getObjectName())
        if (recognitionLabels != null && recognitionLabels.count() > 0) {
            model["recognitionLabels"] = recognitionLabels
        }

        val recognitionLabel = recognitionLabelRepository?.findById(personId)
        if (recognitionLabel != null && recognitionLabel.isPresent) {
            model["personInfo"] = recognitionLabel.get()
        }

        val albumList = albumRepository?.findAllOrderByAlbumName()
        if (albumList != null && albumList.count() > 0) {
            model["allAlbumList"] = albumList
        }

        val settings = model.getAttribute("settings") as Settings

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            var personCount = 0
            if (currentUserObj.getAuthority() == model.getAttribute("userRole")) {
                personCount = metadataRepository?.countByPhotoAlbumByPerson(settings.getRecognitionConfidenceThreshold()!!,personId,currentUserObj.getId())!!
            } else if (currentUserObj.getAuthority() == model.getAttribute("adminRole") || currentUserObj.getAuthority() == model.getAttribute("superRole")) {
                personCount = metadataRepository?.countByMetadataByPerson(settings.getRecognitionConfidenceThreshold()!!,personId)!!
            }
            if (personCount > 0) {
                counts["person"] = personCount
            } else {
                counts["person"] = 0
            }
        }

        val faceRecogServicesAvailable = NetworkUtils.checkCompreFaceConnection(
            settings.getCompreFaceServer(),
            settings.getCompreFaceKey()
        )
        model["faceRecogServicesAvailable"] = faceRecogServicesAvailable
        val subject = recognitionLabel?.get()?.getName()
        val subjectCompreFaceJsonStr = getCompreFaceJsonForSubject(model, faceRecogServicesAvailable, subject, 0, 9999)
        if (!subjectCompreFaceJsonStr.isNullOrBlank()) {
            val jsonObj = mapper.readTree(subjectCompreFaceJsonStr)
            val resultMap = mapper.convertValue(jsonObj, object : TypeReference<Map<String, Any>>() {})
            val resultList: ArrayList<MutableMap<String, String>>?

            if (resultMap.containsKey("faces")) {
                resultList = resultMap["faces"] as ArrayList<MutableMap<String, String>>
                counts["compreface"] = resultList.size
            }
        }

        // Get records of photos that haven't been confirmed - Threshold not 9.0 and greater than threshold configured
        val lowMatchResults = metadataRepository?.findLowMatchesByPerson(personId, settings.getRecognitionConfidenceThreshold()!!)

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
                        if (recognitionLabelObj != null && recognitionLabelObj.get().getName() != TextUtils.getObjectName()) {
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

        getAllAttributeData(model)

        model["msg"] = ""
        model["status"] = ApiResponse.SUCCESS.status
        model["counts"] = counts
        model["activePage"] = module
        model["activeSidebar"] = module
        var title = TextUtils.capitalized(module)
        if (recognitionLabel != null && recognitionLabel.isPresent && recognitionLabel.get().getName() != "") {
            title = TextUtils.capitalized(module) + " - " + recognitionLabel.get().getName()
        }
        model["titleDescriptor"] = title
        return module
    }

    @RequestMapping(value = ["/person/compreface/delete"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @Secured("ROLE_ADMIN", "ROLE_SUPER")
    @ResponseBody
    fun deleteCompreFaceGetImages(model: Model, @RequestBody requestBody: JsonNode, request: HttpServletRequest): String {
        val imageMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})

        resp["responseData"] = mutableMapOf<String, Any?>()
        resp["msg"] = ""
        resp["status"] = ApiResponse.FAIL.status

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null && imageMap.containsKey("imageIds")) {
            val imageIdsString = imageMap["imageIds"].toString()

            val settings = model.getAttribute("settings") as Settings
            val compreFaceConnection = NetworkUtils.checkCompreFaceConnection(
                settings.getCompreFaceServer(),
                settings.getCompreFaceKey()
            )

            if (imageIdsString.isNotBlank() && compreFaceConnection) {
                val webClient = WebClient.create(settings.getCompreFaceServer()!!)

                try {
                    val response = webClient.post()
                        .uri("api/v1/recognition/faces/delete")
                        .header("x-api-key", settings.getCompreFaceKey())
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                        .body(BodyInserters.fromValue(imageIdsString))
                        .retrieve()
                        .bodyToMono(String::class.java)
                        .block()

                    resp["msg"] = response
                    resp["status"] = ApiResponse.SUCCESS.status

                    val idArray: Array<String>? = mapper.readValue(imageIdsString, object : TypeReference<Array<String>>() {})

                    if (!idArray.isNullOrEmpty()) {
                        for (imageId in idArray) {
                            val recognitionLabelPhotoObj = recognitionLabelPhotoRepository?.findByCompreFaceImageId(imageId)
                            if (recognitionLabelPhotoObj != null) {
                                recognitionLabelPhotoObj.setCompreFaceImageId("")
                                recognitionLabelPhotoRepository?.save(recognitionLabelPhotoObj)
                            }
                        }
                    }
                } catch (e: Exception) {
                    resp["msg"] = "Error could not delete faces from CompreFace"

                    logger.log(
                        Level.WARNING,
                        "Error could not delete faces from CompreFace: ${e.localizedMessage}"
                    )

                    return mapper.writeValueAsString(resp)
                }

                resp["responseData"]
            }
        }

        return mapper.writeValueAsString(resp)
    }

    @GetMapping("/person/compreface/{personId}")
    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    fun getCompreFaceGetImages(model: Model, @PathVariable personId: Int, request: HttpServletRequest, locale: Locale): String {
        val module = "compreface"
        val page = 0
        val response = buildCompreFace(model,personId,page, model.getAttribute("queryLimit").toString().toInt(), locale)
        val counts = HashMap<String,Int>()
        counts["compreface"] = 0
        counts["person"] = 0
        counts["matches"] = 0
        response["counts"] = counts

        for ((k, v) in response) {
            model[k] = v!!
        }

        val settings = model.getAttribute("settings") as Settings
        val recognitionLabel = recognitionLabelRepository?.findById(personId)

        var subject: String? = null
        if (recognitionLabel != null && recognitionLabel.isPresent) {
            response["personInfo"] = recognitionLabel.get()
            subject = recognitionLabel.get().getName()
        }
        val faceRecogServicesAvailable = NetworkUtils.checkCompreFaceConnection(
            settings.getCompreFaceServer(),
            settings.getCompreFaceKey()
        )
        model["faceRecogServicesAvailable"] = faceRecogServicesAvailable
        val allSubjectCompreFaceJsonStr = getCompreFaceJsonForSubject(model, faceRecogServicesAvailable, subject, 0, 9999)
        if (!allSubjectCompreFaceJsonStr.isNullOrBlank()) {
            val jsonObj = mapper.readTree(allSubjectCompreFaceJsonStr)
            val resultMap = mapper.convertValue(jsonObj, object : TypeReference<Map<String, Any>>() {})
            val resultList: ArrayList<MutableMap<String, String>>?

            if (resultMap.containsKey("faces")) {
                resultList = resultMap["faces"] as ArrayList<MutableMap<String, String>>
                counts["compreface"] = resultList.size
            }
        }

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            var personCount = 0
            if (currentUserObj.getAuthority() == model.getAttribute("userRole")) {
                personCount = metadataRepository?.countByPhotoAlbumByPerson(settings.getRecognitionConfidenceThreshold()!!,personId,currentUserObj.getId())!!
            } else if (currentUserObj.getAuthority() == model.getAttribute("adminRole") || currentUserObj.getAuthority() == model.getAttribute("superRole")) {
                personCount = metadataRepository?.countByMetadataByPerson(settings.getRecognitionConfidenceThreshold()!!,personId)!!
            }
            if (personCount > 0) {
                counts["person"] = personCount
            } else {
                counts["person"] = 0
            }
        }

        // Get records of photos that haven't been confirmed - Threshold not 9.0 and greater than threshold configured
        val lowMatchCount = metadataRepository?.countLowMatchesByPerson(personId,settings.getRecognitionConfidenceThreshold()!!)
        if (lowMatchCount != null && lowMatchCount > 0) {
            counts["matches"] = lowMatchCount
        } else {
            counts["matches"] = 0
        }

        response["counts"] = counts

        model["msg"] = ""
        model["status"] = ApiResponse.SUCCESS.status
        model["activePage"] = module
        model["activeSidebar"] = module
        var title = TextUtils.capitalized(module)
        if (recognitionLabel != null && recognitionLabel.isPresent && recognitionLabel.get().getName() != "") {
            title = TextUtils.capitalized(module) + " - " + recognitionLabel.get().getName()
        }
        model["titleDescriptor"] = title
        return module
    }

    @RequestMapping(value = ["/person/compreface/{personId}/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedComprefaceList(model: Model, request: HttpServletRequest, @PathVariable personId: Int, @PathVariable page: Int, locale: Locale): String {
        var response = mutableMapOf<String, Any?>()

        if (model.getAttribute("currentUser") != "") {
            response = buildCompreFace(model,personId,page, model.getAttribute("queryLimit").toString().toInt(), locale)
            response["msg"] = ""
            response["status"] = ApiResponse.SUCCESS.status

            return mapper.writeValueAsString(response)
        }

        response["message"] = ""
        response["resultList"] = mutableListOf<MutableMap<String, String>>()
        response["msg"] = "Could not get results"
        response["status"] = ApiResponse.FAIL.status

        return mapper.writeValueAsString(response)
    }

    private fun buildCompreFace(model: Model, personId: Int, page: Int = 0, size: Int = model.getAttribute("queryLimit").toString().toInt(), locale: Locale): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        response["message"] = messageSource?.getMessage("main.nothing", null, locale)
        response["parameter"] = personId
        response["resultList"] = mutableListOf<MutableMap<String, String>>()

        val settings = model.getAttribute("settings") as Settings
        response["compreFaceServer"] = settings.getCompreFaceServer()!!
        response["compreFaceApiKey"] = settings.getCompreFaceKey()!!

        // Get the recognition label
        val recognitionLabel = recognitionLabelRepository?.findById(personId)

        if (recognitionLabel != null && recognitionLabel.isPresent) {
            response["personInfo"] = recognitionLabel.get()
            val subject = recognitionLabel.get().getName()

            val faceRecogServicesAvailable = NetworkUtils.checkCompreFaceConnection(
                settings.getCompreFaceServer(),
                settings.getCompreFaceKey()
            )
            val subjectCompreFaceJsonStr = getCompreFaceJsonForSubject(model, faceRecogServicesAvailable, subject, page, size)
            if (!subjectCompreFaceJsonStr.isNullOrBlank()) {
                val jsonObj = mapper.readTree(subjectCompreFaceJsonStr)
                val resultMap = mapper.convertValue(jsonObj, object : TypeReference<Map<String, Any>>() {})
                val resultList: ArrayList<MutableMap<String, String>>?

                if (resultMap.containsKey("faces")) {
                    resultList = resultMap["faces"] as ArrayList<MutableMap<String, String>>

                    for (facesResult in resultList) {
                        val compreFaceImageId: String? = facesResult["image_id"]
                        val recognitionLabelPhotoObj = recognitionLabelPhotoRepository?.findByCompreFaceImageId(compreFaceImageId!!)
                        facesResult["metadata_date"] = ""
                        facesResult["image_base64"] = ""
                        val compreFaceImageUrl = "${settings.getCompreFaceServer()}api/v1/static/${settings.getCompreFaceKey()!!}/images/${compreFaceImageId}"
                        val base64String = getByteArrayFromImageURL(compreFaceImageUrl)
                        if (!base64String.isNullOrBlank()) {
                            facesResult["image_base64"] = base64String
                        }
                        if (recognitionLabelPhotoObj != null) {
                            if (metadataRepository != null && metadataRepository!!.count() > 0) {
                                val metadataObj = metadataRepository?.findByMetadataId(
                                    recognitionLabelPhotoObj.getMetadataId().toString()
                                )

                                if (metadataObj != null) {
                                    val metadataDate =
                                        "${metadataObj.getYear()}-${metadataObj.getMonth()}-${metadataObj.getDay()}"
                                    facesResult["metadata_date"] = metadataDate
                                }
                            }
                        }
                    }

                    response["resultList"] = resultList
                    response["message"] = ""
                }
            }
        }

        response["msg"] = "Results"
        response["status"] = ApiResponse.SUCCESS.status

        return response
    }

    private fun getCompreFaceJsonForSubject(model: Model, faceRecogServicesAvailable: Boolean, subject: String?, page: Int, queryLimit: Int): String? {
        var subjectCompreFaceJsonStr: String? = null
        val settings = model.getAttribute("settings") as Settings

        if (faceRecogServicesAvailable) {
            val webClient = WebClient.create(settings.getCompreFaceServer()!!)
            try {
                subjectCompreFaceJsonStr = webClient.get()
                    .uri("api/v1/recognition/faces?subject=${subject}&page=${page}&size=${queryLimit}")
                    .header(
                        "x-api-key",
                        settings.getCompreFaceKey()
                    )
                    .retrieve()
                    .bodyToMono(String::class.java)
                    .block()
            } catch (e: Exception) {
                subjectCompreFaceJsonStr = "{\"error\" : \"Error getting CompreFace results for $subject\"}"

                logger.log(
                    Level.WARNING,
                    "Error getting CompreFace results for ${subject}: ${e.localizedMessage}"
                )
            }
        }

        return subjectCompreFaceJsonStr
    }

    private fun getByteArrayFromImageURL(url: String): String? {
        try {
            val imageUrl = URL(url)
            val ucon: URLConnection = imageUrl.openConnection()
            val `is`: InputStream = ucon.getInputStream()
            val baos = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var read: Int
            while (`is`.read(buffer, 0, buffer.size).also { read = it } != -1) {
                baos.write(buffer, 0, read)
            }
            baos.flush()
            return Base64.getEncoder().encodeToString(baos.toByteArray())
        } catch (e: java.lang.Exception) {
            logger.log(
                Level.WARNING,
                "Could not get byte array from image URL ${url}: ${e.localizedMessage}"
            )
        }
        return null
    }

    @GetMapping("/people")
    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    fun getPeople(model: Model, locale: Locale): String {
        val module = "people"
        model["message"] = messageSource?.getMessage("main.nothing", null, locale)
        model["peopleList"] = mutableListOf<MetadataPeople>()
        val counts = HashMap<Int,Int>()
        model["counts"] = counts
        val coverUrls = HashMap<Int, String>()
        model["coverUrls"] = coverUrls

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            var peopleList: MutableIterable<MetadataPeople>? = null
            val settings = model.getAttribute("settings") as Settings

            if (currentUserObj.getAuthority() == model.getAttribute("userRole")) {
                peopleList = metadataRepository?.findAlbumPhotoByPeople(
//                    settings.getRecognitionConfidenceThreshold()!!,
                    "1.0",
                    currentUserObj.getId(),
                    TextUtils.getObjectName()
                )
            } else if (currentUserObj.getAuthority() == model.getAttribute("adminRole") || currentUserObj.getAuthority() == model.getAttribute("superRole")) {
                peopleList = metadataRepository?.findMetadataByPeople(
//                    settings.getRecognitionConfidenceThreshold()!!,
                    "1.0",
                    TextUtils.getObjectName()
                )
            }

            if (peopleList != null && peopleList.count() > 0) {
                for (person in peopleList) {
                    var coverUrl = ""
                    if (person.getCoverUrl() != null) {
                        val metadata = metadataRepository?.findByThumbnailCentered(person.getCoverUrl().toString())
                        if (metadata != null) {
                            coverUrl = "/api/v1/thumbnails/centered/"+metadata.getId()
                        }
                    }
                    coverUrls[person.getId() as Int] = coverUrl

                    val lowMatchResults = metadataRepository?.findLowMatchesByPerson(
                        person.getId()!!,
                        settings.getRecognitionConfidenceThreshold()!!
                    )

                    if (lowMatchResults != null && lowMatchResults.count() > 0) {
                        counts[person.getId()!!] = lowMatchResults.count()
                    }
                }
                model["coverUrls"] = coverUrls
                model["counts"] = counts
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
    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    fun getPerson(model: Model, @PathVariable personId: Int,request: HttpServletRequest, locale: Locale): String {
        val module = "person"
        val page = 0
        val response = buildPersonAlbum(model,module,personId,page, model.getAttribute("queryLimit").toString().toInt(), locale)
        for ((k, v) in response) {
            model[k] = v!!
        }

        getAllAttributeData(model)

        // val person = mapper.convertValue(response["personInfo"], object : TypeReference<Map<String, Any>>() {})

        model["msg"] = ""
        model["status"] = ApiResponse.SUCCESS.status
        model["activePage"] = module
        model["activeSidebar"] = module
        var title = TextUtils.capitalized(module)
        val personInfo = response["personInfo"] as RecognitionLabel
        if (!personInfo.getName().isNullOrBlank()) {
            title = TextUtils.capitalized(module) + " - " + personInfo.getName()
        }
        model["titleDescriptor"] = title
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
            } else if (currentUserObj.getAuthority() == model.getAttribute("adminRole") || currentUserObj.getAuthority() == model.getAttribute(
                    "superRole"
                )
            ) {
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
    fun getPagedPerson(model: Model, request: HttpServletRequest, @PathVariable personId: Int, @PathVariable page: Int, locale: Locale): String {
        return mapper.writeValueAsString(buildPersonAlbum(model,"person",personId,page, model.getAttribute("queryLimit").toString().toInt(), locale))
    }

    private fun buildPersonAlbum(model: Model,module: String,personId: Int,page: Int = 0, size: Int = model.getAttribute("queryLimit").toString().toInt(), locale: Locale): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        response["message"] = messageSource?.getMessage("main.nothing", null, locale)
        response["metadataList"] = mutableListOf<Metadata>()
        response["labelPhotoMap"] = mutableMapOf<String, Any>()
        response["personInfo"] = RecognitionLabel()
        response["recognitionLabels"] = mutableListOf<RecognitionLabel>()
        response["allAlbumList"] = mutableListOf<Album>()
        response["parameter"] = personId
        response["keywordMap"] = mutableMapOf<String, Any>()
        val counts = HashMap<String,Int>()
        response["favorites"] = HashMap<String, HashMap<String, Any>>()
        counts["person"] = 0
        counts["matches"] = 0
        counts["compreface"] = 0
        response["counts"] = counts
        response["canEdit"] = model.getAttribute("authority") == adminRole || model.getAttribute("authority") == superRole
        response["faceRecogServicesAvailable"] = false

        response["msg"] = "Could not get results"
        response["status"] = ApiResponse.FAIL.status

        if (model.getAttribute("currentUser") != "") {
            val favoritesMap = HashMap<String, HashMap<String, Any>>()
            val currentUserObj = model.getAttribute("currentUser") as User?
            val settings = model.getAttribute("settings") as Settings
            val pageValue = page*size

            response["currentUser"] = currentUserObj

            val recognitionLabel = recognitionLabelRepository?.findById(personId)
            if (recognitionLabel != null && recognitionLabel.isPresent) {
                response["personInfo"] = recognitionLabel.get()
            }

            // Get records of photos that haven't been confirmed - Threshold not 9.0 and greater than threshold configured
            val lowMatchCount = metadataRepository?.countLowMatchesByPerson(personId,settings.getRecognitionConfidenceThreshold()!!)
            if (lowMatchCount != null && lowMatchCount > 0) {
                counts["matches"] = lowMatchCount
            } else {
                counts["matches"] = 0
            }

            var metadataList: MutableIterable<Metadata>? = null
            if (currentUserObj!!.getAuthority() == model.getAttribute("userRole")) {
                metadataList = metadataRepository?.findAlbumPhotoByPerson(
                    settings.getRecognitionConfidenceThreshold()!!,
                    personId,
                    currentUserObj.getId(),
                    pageValue,
                    size
                )
            } else if (currentUserObj.getAuthority() == model.getAttribute("adminRole") || currentUserObj.getAuthority() == model.getAttribute("superRole")) {
                val recognitionLabels =
                    recognitionLabelRepository?.findAllByNameNotContaining(TextUtils.getObjectName())

                if (recognitionLabels != null && recognitionLabels.count() > 0) {
                    response["recognitionLabels"] = recognitionLabels
                }

                metadataList = if (module == "person") {
                    metadataRepository?.findMetadataByPersonByModified(
                        settings.getRecognitionConfidenceThreshold()!!,
                        personId,
                        pageValue,
                        size
                    )
                } else {
                    metadataRepository?.findMetadataByPerson(
                        settings.getRecognitionConfidenceThreshold()!!,
                        personId,
                        pageValue,
                        size
                    )
                }
            }

            val albumList = albumRepository?.findAllOrderByAlbumName()
            if (albumList != null && albumList.count() > 0) {
                response["allAlbumList"] = albumList
            }

            if (metadataList != null && metadataList.count() > 0) {
                var personCount = 0
                if (currentUserObj.getAuthority() == model.getAttribute("userRole")) {
                    personCount = metadataRepository?.countByPhotoAlbumByPerson(settings.getRecognitionConfidenceThreshold()!!,personId,currentUserObj.getId())!!
                } else if (currentUserObj.getAuthority() == model.getAttribute("adminRole") || currentUserObj.getAuthority() == model.getAttribute("superRole")) {
                    personCount = metadataRepository?.countByMetadataByPerson(settings.getRecognitionConfidenceThreshold()!!,personId)!!
                }
                if (personCount > 0) {
                    counts["person"] = personCount
                } else {
                    counts["person"] = 0
                }

                val faceRecogServicesAvailable = NetworkUtils.checkCompreFaceConnection(
                    settings.getCompreFaceServer(),
                    settings.getCompreFaceKey()
                )
                response["faceRecogServicesAvailable"] = faceRecogServicesAvailable
                val subject = recognitionLabel?.get()?.getName()
                val subjectCompreFaceJsonStr = getCompreFaceJsonForSubject(model, faceRecogServicesAvailable, subject, 0, 9999)
                if (!subjectCompreFaceJsonStr.isNullOrBlank()) {
                    val jsonObj = mapper.readTree(subjectCompreFaceJsonStr)
                    val resultMap = mapper.convertValue(jsonObj, object : TypeReference<Map<String, Any>>() {})
                    val resultList: ArrayList<MutableMap<String, String>>?

                    if (resultMap.containsKey("faces")) {
                        resultList = resultMap["faces"] as ArrayList<MutableMap<String, String>>
                        counts["compreface"] = resultList.size
                    }
                }

                response["message"] = ""

                val labelPhotoMap = mutableMapOf<String, MutableMap<String,Any>>()
                for (metadata in metadataList) {
                    val favorites = favoriteRepository.findAllByMetadataId(metadata.getId())
                    if (favorites != null) {
                        for (favorite in favorites) {
                            if (favorite != null) {
                                favoritesMap[metadata.getId()] = hashMapOf(
                                    "favorite" to (favorite.getUserId() == currentUserObj.getId()),
                                    "count" to favoriteRepository.countAllByMetadataId(metadata.getId())
                                )

                                if ((favorite.getUserId() == currentUserObj.getId())) {
                                    break
                                }
                            }
                        }
                    }

                    val recognitionLabelPhotos = recognitionLabelPhotoRepository?.findByMetadataId(metadata.getId())

                    var labelString = ""
                    var isAutoTagged = false
                    val nameTaggedMap = mutableMapOf<String,Any>()
                    if (recognitionLabelPhotos != null) {
                        for (recognitionLabelPhoto in recognitionLabelPhotos) {
                            val recognitionLabelObj = recognitionLabelRepository?.findById(recognitionLabelPhoto.getRecognitionLabelId()!!)
                            if (recognitionLabelObj != null && recognitionLabelObj.get().getName() != TextUtils.getObjectName()) {
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
                response["favorites"] = favoritesMap
            }

            response["counts"] = counts

            response["msg"] = "Results"
            response["status"] = ApiResponse.SUCCESS.status
        }

        return response
    }

    @RequestMapping(value = ["/person/update"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @ResponseBody
    fun postPersonUpdate(model: Model, @RequestBody requestBody: JsonNode): String {
        val personMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})

        if (personMap.containsKey("metadataId") &&
            personMap.containsKey("tagpeople") &&
            personMap.containsKey("isObject")
        ) {
            val metadataId = StringEscapeUtils.escapeHtml4(personMap["metadataId"].toString())
            val isObject = personMap["isObject"].toString().toBoolean()

            if (personMap.containsKey("currentPerson") && personMap["currentPerson"].toString() != "") {
                val recognitionLabel = personMap["currentPerson"].toString()
                val compreFaceImageIdMap = mutableMapOf<String, Any?>()

                if (recognitionLabel.trim().isNotBlank()) {
                    val recognitionLabelRecord =
                        recognitionLabelRepository?.findByNameIgnoreCase(recognitionLabel.trim())
                    if (recognitionLabelRecord != null) {
                        val recognitionLabelPhoto =
                            recognitionLabelPhotoRepository?.findByRecognitionLabelIdAndMetadataId(
                                recognitionLabelRecord.getId(),
                                metadataId
                            )
                        if (recognitionLabelPhoto != null && !recognitionLabelPhoto.getCompreFaceImageId()
                                .isNullOrEmpty()
                        ) {
                            compreFaceImageIdMap["${recognitionLabel.replace("\\s".toRegex(), "")}-$metadataId"] =
                                recognitionLabelPhoto.getCompreFaceImageId()!!
                        }
                    }
                }

                val metadata = metadataRepository?.findById(metadataId)

                val recognitionLabelArray = personMap["tagpeople"].toString().split(",")

                if (recognitionLabelArray.isNotEmpty()) {
                    recognitionLabelPhotoRepository?.deleteByMetadataId(metadataId)
                }

                for (recognitionLabelString in recognitionLabelArray) {
                    if (recognitionLabelString.trim().isNotBlank() && recognitionLabelString.trim() != "null") {
                        var recognitionLabelPhotoCount: Int
                        var recognitionLabelObj = RecognitionLabel()

                        val recognitionLabelRecord =
                            recognitionLabelRepository?.findByNameIgnoreCase(recognitionLabelString.trim())

                        if (recognitionLabelRecord == null) {
                            recognitionLabelObj.setName(recognitionLabelString.trim())
                            recognitionLabelObj.setCreatedAt(getCurrentTimestamp())
                            recognitionLabelObj.setModifiedAt(getCurrentTimestamp())
                            recognitionLabelObj.setCoverUrl(metadata?.get()?.getThumbnailUrlCentered())
                            recognitionLabelRepository?.save(recognitionLabelObj)
                        } else {
                            recognitionLabelObj = recognitionLabelRecord
                        }

                        recognitionLabelPhotoCount =
                        recognitionLabelPhotoRepository?.countByRecognitionLabelIdAndMetadataId(
                            recognitionLabelObj.getId(),
                            metadataId
                        )!!

                        if (recognitionLabelPhotoCount == 0) {
                            val uploadResp = mapper.writeValueAsString(
                                ImageProcessing.buildPersonUpload(
                                    model.getAttribute("settings") as Settings,
                                    recognitionLabelString.trim(),
                                    metadata?.get(),
                                    compreFaceImageIdMap
                                )
                            )
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
                val recognitionLabels =
                    recognitionLabelRepository?.findAllByNameNotContaining(TextUtils.getObjectName())
                if (recognitionLabels != null && recognitionLabels.count() > 0) {
                    resp["recognitionLabels"] = recognitionLabels
                }

                resp["msg"] = "Saved"
                resp["status"] = ApiResponse.SUCCESS.status
                return mapper.writeValueAsString(resp)
            } else if (isObject) {
                recognitionLabelPhotoRepository?.deleteByMetadataId(metadataId)

                var recognitionLabelObj = RecognitionLabel()

                val recognitionLabelRecord =
                    recognitionLabelRepository?.findByNameIgnoreCase(TextUtils.getObjectName())
                if (recognitionLabelRecord == null) {
                    recognitionLabelObj.setName(TextUtils.getObjectName())
                    recognitionLabelObj.setCreatedAt(getCurrentTimestamp())
                    recognitionLabelObj.setModifiedAt(getCurrentTimestamp())
                    recognitionLabelRepository?.save(recognitionLabelObj)
                } else {
                    recognitionLabelObj = recognitionLabelRecord
                }

                resp["recognitionLabels"] = mutableListOf<RecognitionLabel>()

                val recognitionLabelPhotoObj = RecognitionLabelPhoto()
                recognitionLabelPhotoObj.setMetadataId(metadataId)
                recognitionLabelPhotoObj.setRecognitionLabelId(recognitionLabelObj.getId())
                recognitionLabelPhotoObj.setConfidence("-0.1")
                recognitionLabelPhotoRepository?.save(recognitionLabelPhotoObj)

                val recognitionLabels =
                    recognitionLabelRepository?.findAllByNameNotContaining(TextUtils.getObjectName())
                if (recognitionLabels != null && recognitionLabels.count() > 0) {
                    resp["recognitionLabels"] = recognitionLabels
                }

                resp["msg"] = "Saved"
                resp["status"] = ApiResponse.SUCCESS.status
                return mapper.writeValueAsString(resp)
            } else if (personMap["tagpeople"].toString().isBlank()) {
                resp["recognitionLabels"] = mutableListOf<RecognitionLabel>()
                recognitionLabelPhotoRepository?.deleteByMetadataId(metadataId)

                val recognitionLabels =
                    recognitionLabelRepository?.findAllByNameNotContaining(TextUtils.getObjectName())
                if (recognitionLabels != null && recognitionLabels.count() > 0) {
                    resp["recognitionLabels"] = recognitionLabels
                }

                resp["msg"] = "Saved"
                resp["status"] = ApiResponse.SUCCESS.status
                return mapper.writeValueAsString(resp)
            }
        } else if (personMap.containsKey("setCoverPerson") &&
            personMap.containsKey("metadataId") &&
            personMap.containsKey("personId")
        ) {
            val metadataId = StringEscapeUtils.escapeHtml4(personMap["metadataId"].toString())
            val personId = StringEscapeUtils.escapeHtml4(personMap["personId"].toString()).toInt()
            val setCoverPerson = personMap["setCoverPerson"].toString().toBoolean()

            if (setCoverPerson) {
                if (metadataRepository != null && metadataRepository!!.count() > 0) {

                    val personObj = recognitionLabelRepository?.findById(personId)
                    val metadataObj = metadataRepository?.findByMetadataId(metadataId)
                    val coverAlbumUrl = metadataObj?.getThumbnailUrlCentered()

                    if (personObj != null && personObj.isPresent && metadataObj != null) {
                        personObj.get().setCoverUrl(coverAlbumUrl)
                        personObj.get().setModifiedAt(getCurrentTimestamp())
                        recognitionLabelRepository?.save(personObj.get())

                        resp["msg"] = "Saved"
                        resp["status"] = ApiResponse.SUCCESS.status
                        return mapper.writeValueAsString(resp)
                    }
                }
            }
        }

        resp["msg"] = "Could not save"
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/person/recognition/faces"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @Secured("ROLE_SUPER", "ROLE_ADMIN")
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
            val settings = model.getAttribute("settings") as Settings

            personUpload(settings, personName, metadata?.get(), compreFaceImageIdMap)

            resp["responseData"] = mutableMapOf<String, Any?>()
            resp["msg"] = ""
            resp["status"] = ApiResponse.SUCCESS.status

            return mapper.writeValueAsString(resp)
        }

        return mapper.writeValueAsString(resp)
    }

    fun personUpload(settings: Settings, personName: String?, metadata: Metadata?, compreFaceImageIdMap: MutableMap<String, Any?>) {
        Thread {
            ImageProcessing.buildPersonUpload(
                settings,
                personName,
                metadata,
                compreFaceImageIdMap
            )
        }.start()
    }

    @RequestMapping(value = ["/person/recognition/recognize/{metadataId}"], method = [RequestMethod.GET], produces = ["application/json"])
    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @ResponseBody
    fun postPersonRecognize(model: Model, @PathVariable metadataId: String): String {
        resp["responseData"] = mutableMapOf<String, Any?>()
        resp["msg"] = ""
        resp["status"] = ApiResponse.FAIL.status

        var metadata: Metadata? = null

        if (metadataRepository != null && metadataRepository!!.count() > 0) {
            val metadataOpt = metadataRepository?.findByMetadataId(metadataId)

            if (metadataOpt != null) {
                metadata = metadataOpt
            }
        }
        return mapper.writeValueAsString(ImageProcessing.buildPersonRecognition(model.getAttribute("settings") as Settings, metadata))
    }
}