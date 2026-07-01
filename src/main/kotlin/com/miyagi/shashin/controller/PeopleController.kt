package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.ShashinApplication
import com.miyagi.shashin.component.Message
import com.miyagi.shashin.component.ScanMessage
import com.miyagi.shashin.model.*
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.service.DuplicateImageDetection
import com.miyagi.shashin.service.ImageProcessing
import com.miyagi.shashin.util.*
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import org.apache.commons.text.StringEscapeUtils
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
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.client.MultipartBodyBuilder
import java.security.Principal

@Suppress("UNCHECKED_CAST")
@Controller
class PeopleController(
    private var metadataRepository: MetadataRepository? = null,
    private var albumRepository: AlbumRepository? = null,
    private var recognitionLabelRepository: RecognitionLabelRepository? = null,
    private var recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository? = null,
    private var favoriteRepository: FavoriteRepository,
    private val keywordRepository: KeywordRepository? = null,
    private val keywordPhotoRepository: KeywordPhotoRepository? = null,
    private val notificationRepository: NotificationRepository? = null,
    private var userRepository: UserRepository? = null,
    private var duplicatesRepository: DuplicatesRepository? = null,
    var messageSource: MessageSource? = null,
    @Value("\${app.role.super}")
    private var superRole: String,
    @Value("\${app.role.admin}")
    private var adminRole: String,
    @Value("\${app.sidecar.path}")
private val relativeSidecarDir: String? = null
): BaseController(
    recognitionLabelRepository = recognitionLabelRepository,
    albumRepository = albumRepository,
    keywordRepository = keywordRepository,
    metadataRepository = metadataRepository
) {
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

        var msg = messageSource?.getMessage("main.pages.matching.start", null, locale).toString()

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

            // Find face, object and duplicate image matches
            doPrediction(settings, superAdmins, locale)
        }

        resp["msg"] = messageSource?.getMessage("main.pages.matching.start", null, locale).toString()
        resp["status"] = ApiResponse.SUCCESS.status
        return mapper.writeValueAsString(resp)
    }

    fun doPrediction(settings: Settings, superAdmins: MutableIterable<User>?, locale: Locale) {
        Thread {
            val threadFile = FileUtils.createThreadFile(threadExtensionName)
            if (!NetworkUtils.checkArgusConnection(settings.getArgusServer(), settings.getArgusKey())) {
                if (superAdmins != null) {
                    val notificationObjList = mutableListOf<Notification>()
                    val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                    sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
                    for (admin in superAdmins) {
                        var language = admin.getLanguage()
                        if (language == null) { language = "en" }
                        val notificationObj = Notification()
                        notificationObj.setUserId(admin.getId())
                        notificationObj.setCreatedAt(getCurrentTimestamp())
                        notificationObj.setModifiedAt(getCurrentTimestamp())
                        notificationObj.setRead(false)
                        notificationObj.setMessage(messageSource?.getMessage("main.notification.compreface.notconnected", null, Locale(language)))
                        notificationObjList.add(notificationObj)
                    }
                    if (notificationObjList.isNotEmpty()) {
                        notificationRepository?.saveAll(notificationObjList)
                    }
                }
            }

            // Object and person recognition
            if (threadFile != null) {

                // Find duplicate images
                var duplicateCount = 0
                if (settings.getDuplicateDetection() == true) {
                    duplicateCount = DuplicateImageDetection.findAndStoreDuplicates(duplicatesRepository!!)
                }

                val recognitionCount = ImageProcessing.Companion.scanAll(
                    metadataRepository,
                    recognitionLabelRepository,
                    recognitionLabelPhotoRepository,
                    keywordRepository,
                    keywordPhotoRepository,
                    settings,
                    threadFile,
                    shouldStop,
                    messageSource,
                    locale
                )

                val adminSupers = userRepository?.findAllByAuthorityEquals(superRole)

                if (adminSupers != null && (recognitionCount > 0 || duplicateCount > 0)) {
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
                        var msg = ""
                        if (recognitionCount > 0) {
                            msg += messageSource?.getMessage("main.notification.people.matchcount", arrayOf(recognitionCount), locale) + "."
                        }
                        if (duplicateCount > 0) {
                            msg += messageSource?.getMessage("main.notification.duplicate.matchcount", arrayOf("<a href='/duplicates' target='_blank'>$duplicateCount</a>"), locale) +"- ${sdtf.format(Date())}."
                        }
                        if (msg.length > 0) {
                            msg += " - ${sdtf.format(Date())}."
                        }
                        notificationObj.setMessage(msg)
                        notificationObjList.add(notificationObj)
                    }
                    if (notificationObjList.isNotEmpty()) {
                        notificationRepository?.saveAll(notificationObjList)
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
        model["reviewItems"] = mutableListOf<MutableMap<String, Any>>()
        model["recognitionLabels"] = mutableListOf<RecognitionLabel>()
        model["allAlbumList"] = mutableListOf<Album>()
        model["labelPhotoMap"] = mutableMapOf<String, Any>()
        model["keywordMap"] = mutableMapOf<String, String>()
        val counts = HashMap<String,Int>()
        counts["person"] = 0
        counts["matches"] = 0
        counts["training"] = 0
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

        val faceRecogServicesAvailable = NetworkUtils.checkArgusConnection(
            settings.getArgusServer(),
            settings.getArgusKey()
        )
        model["faceRecogServicesAvailable"] = faceRecogServicesAvailable
        val argusIdentityId = recognitionLabel?.get()?.getArgusIdentityId()
        counts["training"] = getEnrolledGalleryItems(settings, argusIdentityId).size

        // Pull pending review items from Argus, filtered to this person's argusIdentityId
        val reviewItems = mutableListOf<MutableMap<String, Any>>()
        model["reviewItems"] = reviewItems
        model["argusServer"] = (settings.getArgusServer() ?: "").trimEnd('/')

        if (faceRecogServicesAvailable && argusIdentityId != null) {
            try {
                val argusServer = (settings.getArgusServer() ?: "").trimEnd('/')
                val webClient = WebClient.create(settings.getArgusServer()!!)
                var cursor: String? = null

                do {
                    val uri = if (cursor != null) "api/review?limit=100&cursor=$cursor" else "api/review?limit=100"
                    val reviewJson = webClient.get().uri(uri)
                        .header("X-API-Key", settings.getArgusKey())
                        .retrieve().bodyToMono(String::class.java).block() ?: break

                    val reviewObj = mapper.readTree(reviewJson)
                    val items = reviewObj["items"] ?: break
                    val hasMore = reviewObj["has_more"]?.asBoolean() ?: false
                    cursor = if (hasMore) reviewObj["next_cursor"]?.textValue() else null

                    for (item in items) {
                        val suggestedMatches = item["suggested_matches"]
                        if (suggestedMatches == null || suggestedMatches.size() == 0) continue
                        val topMatch = suggestedMatches[0]
                        if (topMatch["identity_id"].asInt() != argusIdentityId) continue

                        val detectionId = item["detection_id"].asInt().toString()
                        val cropUrl = item["crop_url"]?.textValue() ?: ""
                        val similarity = topMatch["similarity"]?.asDouble() ?: 0.0
                        val suggestedLabel = topMatch["label"]?.textValue() ?: ""

                        val rlp = recognitionLabelPhotoRepository?.findByArgusDetectionId(detectionId)
                        val metadataObj = if (rlp?.getMetadataId() != null)
                            metadataRepository?.findByMetadataId(rlp.getMetadataId()!!) else null

                        val itemMap = mutableMapOf<String, Any>()
                        itemMap["detectionId"] = detectionId
                        itemMap["cropUrl"] = argusServer + cropUrl
                        itemMap["similarity"] = similarity
                        itemMap["suggestedLabel"] = suggestedLabel
                        itemMap["hasPhoto"] = metadataObj != null
                        if (metadataObj != null) {
                            itemMap["metadataId"] = metadataObj.getId() ?: ""
                            itemMap["thumbnailUrl"] = "/api/v1/thumbnails/225/${metadataObj.getId()}"
                            itemMap["year"] = metadataObj.getYear() ?: ""
                            itemMap["month"] = metadataObj.getMonth() ?: ""
                            itemMap["day"] = metadataObj.getDay() ?: ""
                        }
                        reviewItems.add(itemMap)
                    }
                } while (cursor != null)

                if (reviewItems.isNotEmpty()) model["message"] = ""
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Error fetching Argus review queue for person $personId: ${e.localizedMessage}")
            }
        }

        counts["matches"] = reviewItems.size

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

    @RequestMapping(value = ["/person/argus/delete"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @Secured("ROLE_ADMIN", "ROLE_SUPER")
    @ResponseBody
    fun deleteArgusEmbeddings(model: Model, @RequestBody requestBody: JsonNode, request: HttpServletRequest, locale: Locale): String {
        val imageMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})

        resp["responseData"] = mutableMapOf<String, Any?>()
        resp["msg"] = ""
        resp["status"] = ApiResponse.FAIL.status

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null && imageMap.containsKey("imageIds")) {
            val imageIdsString = imageMap["imageIds"].toString()

            val settings = model.getAttribute("settings") as Settings
            val argusConnection = NetworkUtils.checkArgusConnection(
                settings.getArgusServer(),
                settings.getArgusKey()
            )

            if (imageIdsString.isNotBlank() && argusConnection) {
                val webClient = WebClient.create(settings.getArgusServer()!!)

                try {
                    val idArray: Array<String>? = mapper.readValue(imageIdsString, object : TypeReference<Array<String>>() {})

                    if (!idArray.isNullOrEmpty()) {
                        for (detectionId in idArray) {
                            webClient.delete()
                                .uri("api/detections/$detectionId")
                                .header("X-API-Key", settings.getArgusKey())
                                .retrieve()
                                .bodyToMono(String::class.java)
                                .block()

                            val recognitionLabelPhotoObj = recognitionLabelPhotoRepository?.findByArgusDetectionId(detectionId)
                            if (recognitionLabelPhotoObj != null) {
                                recognitionLabelPhotoObj.setArgusDetectionId("")
                                recognitionLabelPhotoRepository?.save(recognitionLabelPhotoObj)
                            }
                        }
                    }

                    resp["msg"] = ""
                    resp["status"] = ApiResponse.SUCCESS.status
                } catch (e: Exception) {
                    resp["msg"] = messageSource?.getMessage("main.compreface.error.msg", null, locale)

                    logger.log(
                        Level.WARNING,
                        "Error could not delete embeddings from Argus: ${e.localizedMessage}"
                    )

                    return mapper.writeValueAsString(resp)
                }
            }
        }

        return mapper.writeValueAsString(resp)
    }

    @GetMapping("/person/argus/{personId}")
    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    fun getArgusTrainingImages(model: Model, @PathVariable personId: Int, request: HttpServletRequest, locale: Locale): String {
        val module = "training"
        syncArgusConfirmedToShashin(personId, model.getAttribute("settings") as Settings)
        val response = buildArgusGallery(model, personId, null, model.getAttribute("queryLimit").toString().toInt(), locale)
        val counts = HashMap<String,Int>()
        counts["training"] = 0
        counts["person"] = 0
        counts["matches"] = 0
        response["counts"] = counts

        for ((k, v) in response) {
            if (v != null) model[k] = v
        }

        val settings = model.getAttribute("settings") as Settings
        val recognitionLabel = recognitionLabelRepository?.findById(personId)

        var subject: String? = null
        if (recognitionLabel != null && recognitionLabel.isPresent) {
            response["personInfo"] = recognitionLabel.get()
            subject = recognitionLabel.get().getName()
        }
        val faceRecogServicesAvailable = NetworkUtils.checkArgusConnection(
            settings.getArgusServer(),
            settings.getArgusKey()
        )
        model["faceRecogServicesAvailable"] = faceRecogServicesAvailable
        val argusIdentityId2 = recognitionLabel?.takeIf { it.isPresent }?.get()?.getArgusIdentityId()
        counts["training"] = getEnrolledGalleryItems(settings, argusIdentityId2).size

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

        // Matches badge reflects the Argus review queue (same source the Matches tab renders),
        // so the count never disagrees with the tab contents.
        counts["matches"] = if (faceRecogServicesAvailable) countArgusReviewMatches(settings, argusIdentityId2) else 0

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

    @RequestMapping(value = ["/person/argus/{personId}/gallery"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedArgusGallery(model: Model, request: HttpServletRequest, @PathVariable personId: Int, @RequestParam(required = false) cursor: String? = null, locale: Locale): String {
        var response = mutableMapOf<String, Any?>()

        if (model.getAttribute("currentUser") != "") {
            response = buildArgusGallery(model, personId, cursor, model.getAttribute("queryLimit").toString().toInt(), locale)
            response["msg"] = ""
            response["status"] = ApiResponse.SUCCESS.status

            return mapper.writeValueAsString(response)
        }

        response["message"] = ""
        response["resultList"] = mutableListOf<MutableMap<String, String>>()
        response["msg"] = messageSource?.getMessage("main.noresults", null, locale)
        response["status"] = ApiResponse.FAIL.status

        return mapper.writeValueAsString(response)
    }

    @RequestMapping(value = ["/person/argus/{personId}/resync"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun resyncArgusIdentity(model: Model, @PathVariable personId: Int, locale: Locale): String {
        val response = mutableMapOf<String, Any?>()
        val settings = model.getAttribute("settings") as Settings

        if (settings.getArgusServer().isNullOrBlank() || settings.getArgusKey().isNullOrBlank()) {
            response["status"] = ApiResponse.FAIL.status
            response["msg"] = "Argus is not configured."
            return mapper.writeValueAsString(response)
        }

        val label = recognitionLabelRepository?.findById(personId)
        if (label == null || !label.isPresent) {
            response["status"] = ApiResponse.FAIL.status
            response["msg"] = "Person not found."
            return mapper.writeValueAsString(response)
        }
        val person = label.get()
        val personName = person.getName() ?: run {
            response["status"] = ApiResponse.FAIL.status
            response["msg"] = "Person has no name."
            return mapper.writeValueAsString(response)
        }

        return try {
            val summaryJson = WebClient.create(settings.getArgusServer()!!)
                .get()
                .uri("api/identities/summary?type=face")
                .header("X-API-Key", settings.getArgusKey())
                .retrieve()
                .bodyToMono(String::class.java)
                .block()

            val identities = mapper.readTree(summaryJson ?: "{}")
            val match = identities["items"]?.firstOrNull { it["label"]?.asText()?.equals(personName, ignoreCase = true) == true }

            if (match == null) {
                response["status"] = ApiResponse.FAIL.status
                response["msg"] = "No Argus identity found matching \"$personName\"."
            } else {
                val argusId = match["id"].asInt()
                if (person.getArgusIdentityId() != argusId) {
                    person.setArgusIdentityId(argusId)
                    recognitionLabelRepository?.save(person)
                    response["status"] = ApiResponse.SUCCESS.status
                    response["msg"] = "Re-synced: Argus identity ID updated to $argusId."
                } else {
                    response["status"] = ApiResponse.SUCCESS.status
                    response["msg"] = "Already in sync (identity ID $argusId)."
                }
            }
            mapper.writeValueAsString(response)
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error resyncing Argus identity for person $personId: ${e.localizedMessage}")
            response["status"] = ApiResponse.FAIL.status
            response["msg"] = "Error contacting Argus: ${e.localizedMessage}"
            mapper.writeValueAsString(response)
        }
    }

    private fun buildArgusGallery(model: Model, personId: Int, cursor: String? = null, size: Int = model.getAttribute("queryLimit").toString().toInt(), locale: Locale): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        response["message"] = messageSource?.getMessage("main.nothing", null, locale)
        response["parameter"] = personId
        response["resultList"] = mutableListOf<MutableMap<String, Any>>()
        response["has_more"] = false

        val settings = model.getAttribute("settings") as Settings
        response["argusServer"] = (settings.getArgusServer() ?: "").trimEnd('/')

        val recognitionLabel = recognitionLabelRepository?.findById(personId)

        if (recognitionLabel != null && recognitionLabel.isPresent) {
            response["personInfo"] = recognitionLabel.get()
            val argusIdentityId = recognitionLabel.get().getArgusIdentityId()

            val galleryJson = getArgusGalleryForIdentity(settings, argusIdentityId, limit = size, enrolled = true, cursor = cursor)
            if (galleryJson != null) {
                val galleryObj = mapper.readTree(galleryJson)
                val items = if (galleryObj.has("items")) galleryObj["items"].toList() else emptyList()
                response["next_cursor"] = galleryObj["next_cursor"]?.takeUnless { it.isNull }?.textValue()
                response["has_more"] = galleryObj["has_more"]?.asBoolean() ?: false
                if (items.isNotEmpty()) {
                    val resultList = mutableListOf<MutableMap<String, Any>>()
                    for (item in items) {
                        val detectionId = item["detection_id"].asInt().toString()
                        val itemMap = mutableMapOf<String, Any>()
                        itemMap["id"] = detectionId
                        itemMap["crop_url"] = if (item.has("crop_url") && !item["crop_url"].isNull) item["crop_url"].textValue() else ""
                        itemMap["metadata_date"] = ""
                        val rlp = recognitionLabelPhotoRepository?.findByArgusDetectionId(detectionId)
                        if (rlp?.getMetadataId() != null) {
                            val metadataObj = metadataRepository?.findByMetadataId(rlp.getMetadataId()!!)
                            if (metadataObj != null) {
                                itemMap["metadata_date"] = "${metadataObj.getYear()}-${metadataObj.getMonth()}-${metadataObj.getDay()}"
                            }
                        }
                        resultList.add(itemMap)
                    }
                    response["resultList"] = resultList
                    response["message"] = ""
                }
            }
        }

        response["msg"] = messageSource?.getMessage("main.results", null, locale)
        response["status"] = ApiResponse.SUCCESS.status

        return response
    }

    private fun syncArgusConfirmedToShashin(personId: Int, settings: Settings) {
        val recognitionLabel = recognitionLabelRepository?.findById(personId)
        val argusIdentityId = recognitionLabel?.takeIf { it.isPresent }?.get()?.getArgusIdentityId() ?: return
        if (settings.getArgusServer().isNullOrBlank() || settings.getArgusKey().isNullOrBlank()) return

        try {
            // Only sync detections affirmatively assigned to this person (confirmed or reassigned to
            // this identity) — NOT pending matches, which must not be promoted to confirmed
            // (confidence 0.0); they belong in the Matches review queue, not the Person tab.
            val confirmedStatuses = setOf("confirmed", "reassigned")
            val items = getGalleryItems(settings, argusIdentityId)
                .filter { it.has("review_status") && it["review_status"].asText() in confirmedStatuses }

            for (item in items) {
                val detectionId = item["detection_id"].asInt().toString()
                val rlp = recognitionLabelPhotoRepository?.findByArgusDetectionId(detectionId) ?: continue
                if (rlp.getRecognitionLabelId() == null) {
                    rlp.setRecognitionLabelId(personId)
                    rlp.setConfidence("0.0")
                    try { recognitionLabelPhotoRepository?.save(rlp) } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error syncing Argus confirmed detections for person $personId: ${e.localizedMessage}")
        }
    }

    // One pass over the Argus review queue, returning identity_id -> pending review count, bucketed
    // by each item's top suggested match. This is the SAME source the Matches tab (getPredictions)
    // renders from, so badges stay consistent with the tab contents — a stale local low-match row no
    // longer shows a count with no photos. Fetching once and bucketing avoids an Argus call per person.
    private fun countArgusReviewMatchesByIdentity(settings: Settings): Map<Int, Int> {
        val counts = mutableMapOf<Int, Int>()
        if (settings.getArgusServer().isNullOrBlank() || settings.getArgusKey().isNullOrBlank()) return counts
        try {
            val webClient = WebClient.create(settings.getArgusServer()!!)
            var cursor: String? = null
            do {
                val uri = if (cursor != null) "api/review?limit=100&cursor=$cursor" else "api/review?limit=100"
                val reviewJson = webClient.get().uri(uri)
                    .header("X-API-Key", settings.getArgusKey())
                    .retrieve().bodyToMono(String::class.java).block() ?: break

                val reviewObj = mapper.readTree(reviewJson)
                val items = reviewObj["items"] ?: break
                val hasMore = reviewObj["has_more"]?.asBoolean() ?: false
                cursor = if (hasMore) reviewObj["next_cursor"]?.textValue() else null

                for (item in items) {
                    val suggestedMatches = item["suggested_matches"]
                    if (suggestedMatches == null || suggestedMatches.size() == 0) continue
                    val identityId = suggestedMatches[0]["identity_id"].asInt()
                    counts[identityId] = (counts[identityId] ?: 0) + 1
                }
            } while (cursor != null)
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error counting Argus review matches: ${e.localizedMessage}")
        }
        return counts
    }

    private fun countArgusReviewMatches(settings: Settings, argusIdentityId: Int?): Int {
        if (argusIdentityId == null) return 0
        return countArgusReviewMatchesByIdentity(settings)[argusIdentityId] ?: 0
    }

    // All items the Argus identity gallery returns. NOTE: the gallery includes EVERY detection
    // attributed to the identity (enrolled references, confirmed appearances, and pending matches
    // awaiting review) — each item carries "enrolled" (bool) and "review_status" so callers can
    // narrow to what they actually mean.
    private fun getGalleryItems(settings: Settings, argusIdentityId: Int?, enrolled: Boolean = false): List<com.fasterxml.jackson.databind.JsonNode> {
        val json = getArgusGalleryForIdentity(settings, argusIdentityId, enrolled = enrolled) ?: return emptyList()
        return try {
            val galleryJson = mapper.readTree(json)
            if (galleryJson.has("items")) galleryJson["items"].toList() else emptyList()
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error parsing Argus gallery for identity $argusIdentityId: ${e.localizedMessage}")
            emptyList()
        }
    }

    private fun getEnrolledGalleryItems(settings: Settings, argusIdentityId: Int?): List<JsonNode> =
        getGalleryItems(settings, argusIdentityId, enrolled = true)

    private fun getArgusGalleryForIdentity(settings: Settings, argusIdentityId: Int?, limit: Int = 9999, enrolled: Boolean = false, cursor: String? = null): String? {
        if (argusIdentityId == null || settings.getArgusServer().isNullOrBlank() || settings.getArgusKey().isNullOrBlank()) return null
        val uri = buildString {
            append("api/identities/$argusIdentityId/gallery?limit=$limit")
            if (enrolled) append("&enrolled=true")
            if (cursor != null) append("&cursor=$cursor")
        }
        return try {
            WebClient.create(settings.getArgusServer()!!)
                .get()
                .uri(uri)
                .header("X-API-Key", settings.getArgusKey())
                .retrieve()
                .bodyToMono(String::class.java)
                .block()
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error getting Argus gallery for identity $argusIdentityId: ${e.localizedMessage}")
            null
        }
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
                // Matches badges reflect the Argus review queue (same source the Matches tab renders).
                // Fetch the queue once and bucket by identity, and map each person to its argusIdentityId.
                val reviewCountsByIdentity = countArgusReviewMatchesByIdentity(settings)
                val argusIdByLabelId = recognitionLabelRepository?.findAll()
                    ?.filterNotNull()?.associate { it.getId() to it.getArgusIdentityId() } ?: emptyMap()

                for (person in peopleList) {
                    var coverUrl = ""
                    if (person.getCoverUrl() != null) {
                        val metadata = metadataRepository?.findByThumbnailCentered(person.getCoverUrl().toString())
                        if (metadata != null) {
                            coverUrl = "/api/v1/thumbnails/centered/"+metadata.getId()
                        }
                    }
                    coverUrls[person.getId() as Int] = coverUrl

                    val argusIdentityId = argusIdByLabelId[person.getId()]
                    val matchCount = if (argusIdentityId != null) (reviewCountsByIdentity[argusIdentityId] ?: 0) else 0
                    if (matchCount > 0) {
                        counts[person.getId()!!] = matchCount
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

        syncArgusConfirmedToShashin(personId, model.getAttribute("settings") as Settings)

        val response = buildPersonAlbum(model,module,personId,page, model.getAttribute("queryLimit").toString().toInt(), locale)
        for ((k, v) in response) {
            if (v != null) model[k] = v
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
    fun getPagedPersonMetadataList(model: Model, request: HttpServletRequest, @PathVariable personId: Int, @PathVariable page: Int, locale: Locale): String {
        val response = mutableMapOf<String, Any?>()
        response["message"] = ""
        response["metadataList"] = mutableListOf<Metadata>()
        response["msg"] = messageSource?.getMessage("main.noresults", null, locale)
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
        counts["training"] = 0
        response["counts"] = counts
        response["canEdit"] = model.getAttribute("authority") == adminRole || model.getAttribute("authority") == superRole
        response["faceRecogServicesAvailable"] = false

        response["msg"] = messageSource?.getMessage("main.noresults", null, locale)
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

            // Matches badge reflects the Argus review queue (same source the Matches tab renders),
            // so the count never disagrees with the tab contents.
            val argusIdentityId = if (recognitionLabel != null && recognitionLabel.isPresent) recognitionLabel.get().getArgusIdentityId() else null
            counts["matches"] = countArgusReviewMatches(settings, argusIdentityId)

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

//                metadataList = if (module == "person") {
//                    metadataRepository?.findMetadataByPersonByModified(
//                        settings.getRecognitionConfidenceThreshold()!!,
//                        personId,
//                        pageValue,
//                        size
//                    )
//                } else {
//                    metadataRepository?.findMetadataByPerson(
//                        settings.getRecognitionConfidenceThreshold()!!,
//                        personId,
//                        pageValue,
//                        size
//                    )
//                }

                metadataList = metadataRepository?.findMetadataByPerson(
                    settings.getRecognitionConfidenceThreshold()!!,
                    personId,
                    pageValue,
                    size
                )
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

                val faceRecogServicesAvailable = NetworkUtils.checkArgusConnection(
                    settings.getArgusServer(),
                    settings.getArgusKey()
                )
                response["faceRecogServicesAvailable"] = faceRecogServicesAvailable
                val argusId = recognitionLabel?.get()?.getArgusIdentityId()
                val galleryJson2 = getArgusGalleryForIdentity(settings, argusId)
                if (!galleryJson2.isNullOrBlank()) {
                    val gJson = mapper.readTree(galleryJson2)
                    if (gJson.has("items")) counts["training"] = gJson["items"].size()
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
                            val recognitionLabelObj = recognitionLabelPhoto.getRecognitionLabelId()?.let { recognitionLabelRepository?.findById(it) }
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

            response["msg"] = messageSource?.getMessage("main.results", null, locale)
            response["status"] = ApiResponse.SUCCESS.status
        }

        return response
    }

    @RequestMapping(value = ["/person/update"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @ResponseBody
    fun postPersonUpdate(model: Model, @RequestBody requestBody: JsonNode, locale: Locale): String {
        val personMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})

        if (personMap.containsKey("metadataId") &&
            personMap.containsKey("tagpeople") &&
            personMap.containsKey("isObject")
        ) {
            val metadataId = StringEscapeUtils.escapeHtml4(personMap["metadataId"].toString())
            val isObject = personMap["isObject"].toString().toBoolean()

            if (personMap.containsKey("currentPerson") && personMap["currentPerson"].toString() != "") {
                val recognitionLabel = personMap["currentPerson"].toString()
                val argusDetectionIdMap = mutableMapOf<String, Any?>()

                if (recognitionLabel.trim().isNotBlank()) {
                    val recognitionLabelRecord =
                        recognitionLabelRepository?.findByNameIgnoreCase(recognitionLabel.trim())
                    if (recognitionLabelRecord != null) {
                        val recognitionLabelPhoto =
                            recognitionLabelPhotoRepository?.findByRecognitionLabelIdAndMetadataId(
                                recognitionLabelRecord.getId(),
                                metadataId
                            )
                        if (recognitionLabelPhoto != null && !recognitionLabelPhoto.getArgusDetectionId()
                                .isNullOrEmpty()
                        ) {
                            argusDetectionIdMap["${recognitionLabel.replace("\\s".toRegex(), "")}-$metadataId"] =
                                recognitionLabelPhoto.getArgusDetectionId()!!
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
                                ImageProcessing.Companion.buildPersonUpload(
                                    model.getAttribute("settings") as Settings,
                                    recognitionLabelString.trim(),
                                    metadata?.get(),
                                    argusDetectionIdMap,
                                    recognitionLabelRepository
                                )
                            )
                            val jsonRespObj = mapper.readTree(uploadResp)

                            var detectionId: String? = null
                            if (jsonRespObj.has("responseData") && jsonRespObj["responseData"].has("detection_id")) {
                                detectionId = jsonRespObj["responseData"]["detection_id"].toString()
                            }

                            val recognitionLabelPhotoObj = RecognitionLabelPhoto()
                            recognitionLabelPhotoObj.setMetadataId(metadataId)
                            recognitionLabelPhotoObj.setRecognitionLabelId(recognitionLabelObj.getId())
                            recognitionLabelPhotoObj.setConfidence("0.0")
                            recognitionLabelPhotoObj.setArgusDetectionId(detectionId)
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

                resp["msg"] = messageSource?.getMessage("main.modal.saved", null, locale)
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

                resp["msg"] = messageSource?.getMessage("main.modal.saved", null, locale)
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

                resp["msg"] = messageSource?.getMessage("main.modal.saved", null, locale)
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

                        resp["msg"] = messageSource?.getMessage("main.modal.saved", null, locale)
                        resp["status"] = ApiResponse.SUCCESS.status
                        return mapper.writeValueAsString(resp)
                    }
                }
            }
        }

        resp["msg"] = messageSource?.getMessage("main.modal.saved.fail", null, locale)
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/person/argus/reconcile"], method = [RequestMethod.POST], produces = ["application/json"])
    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @ResponseBody
    fun reconcileArgus(model: Model, locale: Locale): String {
        val response = mutableMapOf<String, Any?>()
        val settings = model.getAttribute("settings") as Settings

        if (settings.getArgusServer().isNullOrBlank() || settings.getArgusKey().isNullOrBlank()) {
            response["status"] = ApiResponse.FAIL.status
            response["msg"] = "Argus is not configured."
            return mapper.writeValueAsString(response)
        }

        var identitiesProcessed = 0
        var recordsUpdated = 0

        try {
            val webClient = WebClient.create(settings.getArgusServer()!!)
            val argusServer = settings.getArgusServer()!!.trimEnd('/')

            val summaryJson = webClient.get()
                .uri("api/identities/summary?type=face")
                .header("X-API-Key", settings.getArgusKey())
                .retrieve().bodyToMono(String::class.java).block()

            val identities = mapper.readTree(summaryJson ?: "{}")

            for (identity in identities["items"] ?: emptyList()) {
                val argusIdentityId = identity["id"]?.asInt() ?: continue
                val argusName = identity["label"]?.asText() ?: continue

                var person = recognitionLabelRepository?.findByNameIgnoreCase(argusName)
                if (person == null) {
                    val newLabel = RecognitionLabel()
                    newLabel.setName(argusName)
                    newLabel.setArgusIdentityId(argusIdentityId)
                    person = recognitionLabelRepository?.save(newLabel)
                    recordsUpdated++
                }
                if (person == null) continue
                identitiesProcessed++

                // Sync identity ID if wrong
                if (person.getArgusIdentityId() != argusIdentityId) {
                    person.setArgusIdentityId(argusIdentityId)
                    recognitionLabelRepository?.save(person)
                }

                // Pull full gallery for this identity
                val galleryJson = webClient.get()
                    .uri("api/identities/$argusIdentityId/gallery?limit=9999")
                    .header("X-API-Key", settings.getArgusKey())
                    .retrieve().bodyToMono(String::class.java).block() ?: continue

                val galleryObj = mapper.readTree(galleryJson)
                val items = galleryObj["items"] ?: continue

                for (item in items) {
                    val detectionId = item["detection_id"]?.asInt()?.toString() ?: continue
                    val enrolled = item["enrolled"]?.asBoolean() ?: false

                    val record = recognitionLabelPhotoRepository?.findByArgusDetectionId(detectionId) ?: continue

                    var changed = false
                    if (record.getRecognitionLabelId() != person.getId()) {
                        record.setRecognitionLabelId(person.getId())
                        changed = true
                    }
                    // enrolled=true means explicitly labeled → Training Image (auto_tagged=false)
                    val expectedAutoTagged = !enrolled
                    if (record.getAutoTagged() != expectedAutoTagged) {
                        record.setAutoTagged(expectedAutoTagged)
                        changed = true
                    }
                    if (changed) {
                        try {
                            recognitionLabelPhotoRepository?.save(record)
                            recordsUpdated++
                        } catch (_: Exception) {}
                    }

                    // Set cover from first Shashin-matched photo if none exists
                    if (person.getCoverUrl() == null && record.getMetadataId() != null) {
                        val metadata = metadataRepository?.findByMetadataId(record.getMetadataId()!!)
                        if (metadata != null) {
                            person.setCoverUrl(metadata.getThumbnailUrlCentered())
                            recognitionLabelRepository?.save(person)
                        }
                    }
                }
            }

            response["status"] = ApiResponse.SUCCESS.status
            response["msg"] = "Reconciled $identitiesProcessed identities, updated $recordsUpdated records."
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Argus reconciliation error: ${e.localizedMessage}")
            response["status"] = ApiResponse.FAIL.status
            response["msg"] = "Error: ${e.localizedMessage}"
        }

        return mapper.writeValueAsString(response)
    }

    @RequestMapping(value = ["/person/matches/confirm"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @ResponseBody
    fun confirmMatch(model: Model, @RequestBody requestBody: JsonNode, locale: Locale): String {
        val bodyMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        resp["msg"] = ""
        resp["status"] = ApiResponse.FAIL.status

        val detectionId = bodyMap["detectionId"]?.toString()
        val action = bodyMap["action"]?.toString() ?: "confirm"
        val identityId = bodyMap["identityId"]?.toString()?.toIntOrNull()

        if (!detectionId.isNullOrBlank()) {
            val settings = model.getAttribute("settings") as Settings

            try {
                val webClient = WebClient.create(settings.getArgusServer()!!)
                val argusServer = (settings.getArgusServer() ?: "").trimEnd('/')

                when (action) {
                    "confirm" -> {
                        val personIdParam = bodyMap["personId"]?.toString()?.toIntOrNull()
                        val cropUrl = bodyMap["cropUrl"]?.toString()

                        // Enroll the face as a Training Image by re-posting the crop to api/detect/faces?label=Name.
                        // This gives it enrolled=true in Argus so it improves future recognition.
                        var newDetectionId: String? = null
                        if (personIdParam != null && !cropUrl.isNullOrBlank()) {
                            val person = recognitionLabelRepository?.findById(personIdParam)
                            val personName = person?.takeIf { it.isPresent }?.get()?.getName()
                            if (!personName.isNullOrBlank()) {
                                try {
                                    val cropBytes = WebClient.create().get()
                                        .uri(cropUrl)
                                        .header("X-API-Key", settings.getArgusKey())
                                        .retrieve().bodyToMono(ByteArray::class.java).block()

                                    if (cropBytes != null) {
                                        val resource = object : ByteArrayResource(cropBytes) {
                                            override fun getFilename() = "crop.jpg"
                                        }
                                        val builder = MultipartBodyBuilder()
                                        builder.part("file", resource)
                                        builder.part("label", personName)

                                        val enrollResp = webClient.post()
                                            .uri("api/detect/faces")
                                            .header("X-API-Key", settings.getArgusKey())
                                            .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA.toString())
                                            .body(BodyInserters.fromMultipartData(builder.build()))
                                            .retrieve().bodyToMono(String::class.java).block()

                                        val enrollJson = mapper.readTree(enrollResp ?: "{}")
                                        newDetectionId = enrollJson["faces"]?.get(0)?.get("detection_id")?.asInt()?.toString()
                                        val newIdentityId = enrollJson["faces"]?.get(0)?.get("identity_id")?.asInt()
                                        if (newIdentityId != null) {
                                            val p = person.get()
                                            if (p.getArgusIdentityId() != newIdentityId) {
                                                p.setArgusIdentityId(newIdentityId)
                                                recognitionLabelRepository?.save(p)
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    logger.log(Level.WARNING, "Enrollment on confirm failed for detection $detectionId: ${e.localizedMessage}")
                                }
                            }
                        }

                        // Confirm the original detection in Argus review queue to clean it up
                        webClient.post()
                            .uri("api/review/$detectionId/confirm")
                            .header("X-API-Key", settings.getArgusKey())
                            .retrieve().bodyToMono(String::class.java).block()

                        // Update the DB record: link to person, mark as Training Image (auto_tagged=false)
                        val record = recognitionLabelPhotoRepository?.findByArgusDetectionId(detectionId)
                        if (record != null && personIdParam != null) {
                            record.setRecognitionLabelId(personIdParam)
                            record.setAutoTagged(false)
                            record.setConfidence("0.0")
                            if (newDetectionId != null) record.setArgusDetectionId(newDetectionId)
                            try { recognitionLabelPhotoRepository?.save(record) } catch (_: Exception) {}
                        }
                    }
                    "reject" -> {
                        webClient.post()
                            .uri("api/review/$detectionId/reject")
                            .header("X-API-Key", settings.getArgusKey())
                            .retrieve().bodyToMono(String::class.java).block()

                        recognitionLabelPhotoRepository?.findByArgusDetectionId(detectionId)
                            ?.let { recognitionLabelPhotoRepository?.delete(it) }
                    }
                    "reassign" -> {
                        if (identityId != null) {
                            val body = mapper.writeValueAsString(mapOf("identity_id" to identityId))
                            webClient.post()
                                .uri("api/review/$detectionId/reassign")
                                .header("X-API-Key", settings.getArgusKey())
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                                .body(BodyInserters.fromValue(body))
                                .retrieve().bodyToMono(String::class.java).block()

                            val record = recognitionLabelPhotoRepository?.findByArgusDetectionId(detectionId)
                            if (record != null) {
                                val label = recognitionLabelRepository?.findByArgusIdentityId(identityId)
                                if (label != null) {
                                    record.setRecognitionLabelId(label.getId())
                                    record.setConfidence("0.0")
                                    try { recognitionLabelPhotoRepository?.save(record) } catch (_: Exception) {}
                                }
                            }
                        }
                    }
                }
                resp["msg"] = ""
                resp["status"] = ApiResponse.SUCCESS.status
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Error processing match action $action for detection $detectionId: ${e.localizedMessage}")
                resp["msg"] = e.localizedMessage
            }
        }

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
            val argusDetectionIdMap = mutableMapOf<String, Any?>()
            val settings = model.getAttribute("settings") as Settings

            personUpload(settings, personName, metadata?.get(), argusDetectionIdMap)

            resp["responseData"] = mutableMapOf<String, Any?>()
            resp["msg"] = ""
            resp["status"] = ApiResponse.SUCCESS.status

            return mapper.writeValueAsString(resp)
        }

        return mapper.writeValueAsString(resp)
    }

    fun personUpload(settings: Settings, personName: String?, metadata: Metadata?, argusDetectionIdMap: MutableMap<String, Any?>) {
        Thread {
            ImageProcessing.Companion.buildPersonUpload(
                settings,
                personName,
                metadata,
                argusDetectionIdMap,
                recognitionLabelRepository
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
        return mapper.writeValueAsString(ImageProcessing.Companion.buildPersonRecognition(model.getAttribute("settings") as Settings, metadata))
    }
}