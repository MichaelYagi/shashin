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
import org.springframework.http.ResponseEntity
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.annotation.SubscribeMapping
import org.springframework.cache.annotation.CacheEvict
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
import org.springframework.messaging.simp.SimpMessagingTemplate
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
    private val relativeSidecarDir: String? = null,
    private var argusReconcile: com.miyagi.shashin.component.ArgusReconcile? = null,
    private var ollamaVisionService: com.miyagi.shashin.component.OllamaVisionService? = null,
    private var messagingTemplate: SimpMessagingTemplate? = null
): BaseController(
    recognitionLabelRepository = recognitionLabelRepository,
    albumRepository = albumRepository,
    keywordRepository = keywordRepository,
    metadataRepository = metadataRepository
) {
    private var shouldStop = AtomicBoolean(false)

    private val threadExtensionName: String = "facescan_shashinscan"

    private val userLocaleCache = java.util.concurrent.ConcurrentHashMap<String, Locale>()

    private var logger: Logger = Logger.getLogger(SettingsController::class.simpleName)

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, Any?>()

    @MessageMapping("/matchmessage")
    fun sendMatcnMessage(message: ScanMessage, principal: Principal) {
        val username = principal.name
        val locale = userLocaleCache.getOrPut(username) {
            val user = userRepository?.findByUsername(username)
            val lang = if (user?.getLanguage().isNullOrEmpty()) "en" else user!!.getLanguage()!!
            Locale(lang)
        }

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

        try {
            messagingTemplate?.convertAndSend("/topic/matchmessages", messageObj)
        } catch (_: Exception) {
            // session closed before delivery — safe to ignore
        }
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

                val (recognitionCount, scannedIds) = ImageProcessing.Companion.scanAll(
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
                if (scannedIds.isNotEmpty()) {
                    metadataRepository?.bulkUpdateLastAccessedAt(getCurrentTimestamp(), scannedIds)
                    ollamaVisionService?.processItems(scannedIds, settings)
                }

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

            argusReconcile?.run(settings)
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
                personCount = metadataRepository?.countByPhotoAlbumByPerson(personId,currentUserObj.getId())!!
            } else if (currentUserObj.getAuthority() == model.getAttribute("adminRole") || currentUserObj.getAuthority() == model.getAttribute("superRole")) {
                personCount = metadataRepository?.countByMetadataByPerson(personId)!!
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
        counts["training"] = getArgusIdentityCounts(settings, argusIdentityId).detectionCount

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

                        val sourceImageIdStr = item["source_image_id"]?.takeUnless { it.isNull }?.asInt()?.toString()
                        val rlp = recognitionLabelPhotoRepository?.findFirstByArgusDetectionId(detectionId)
                            ?: if (sourceImageIdStr != null)
                                recognitionLabelPhotoRepository?.findFirstByArgusSourceImageIdAndMetadataIdIsNotNull(sourceImageIdStr)
                               else null
                        val metadataObj = if (rlp?.getMetadataId() != null)
                            metadataRepository?.findByMetadataId(rlp.getMetadataId()!!) else null
                        val sourceImageUrl = item["source_image_url"]?.takeUnless { it.isNull }?.textValue()

                        val itemMap = mutableMapOf<String, Any>()
                        itemMap["detectionId"] = detectionId
                        itemMap["cropUrl"] = argusServer + cropUrl
                        itemMap["similarity"] = similarity
                        itemMap["suggestedLabel"] = suggestedLabel
                        if (metadataObj != null) {
                            itemMap["hasPhoto"] = true
                            itemMap["metadataId"] = metadataObj.getId() ?: ""
                            itemMap["thumbnailUrl"] = "/api/v1/thumbnails/225/${metadataObj.getId()}"
                            itemMap["year"] = metadataObj.getYear() ?: ""
                            itemMap["month"] = metadataObj.getMonth() ?: ""
                            itemMap["day"] = metadataObj.getDay() ?: ""
                        } else if (sourceImageUrl != null) {
                            itemMap["hasPhoto"] = true
                            itemMap["metadataId"] = ""
                            itemMap["thumbnailUrl"] = argusServer + sourceImageUrl
                        } else {
                            itemMap["hasPhoto"] = false
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

                            val recognitionLabelPhotoObj = recognitionLabelPhotoRepository?.findFirstByArgusDetectionId(detectionId)
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
        val argusCounts = getArgusIdentityCounts(settings, argusIdentityId2)
        counts["training"] = argusCounts.detectionCount

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            var personCount = 0
            if (currentUserObj.getAuthority() == model.getAttribute("userRole")) {
                personCount = metadataRepository?.countByPhotoAlbumByPerson(personId,currentUserObj.getId())!!
            } else if (currentUserObj.getAuthority() == model.getAttribute("adminRole") || currentUserObj.getAuthority() == model.getAttribute("superRole")) {
                personCount = metadataRepository?.countByMetadataByPerson(personId)!!
            }
            if (personCount > 0) {
                counts["person"] = personCount
            } else {
                counts["person"] = 0
            }
        }

        counts["matches"] = argusCounts.pendingReviewCount

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

        return try {
            val webClient = WebClient.create(settings.getArgusServer()!!)
            val apiKey = settings.getArgusKey()!!
            var argusIdentity: com.fasterxml.jackson.databind.JsonNode? = null

            // 1. Try direct fetch by stored argusIdentityId
            val existingArgusId = person.getArgusIdentityId()
            if (existingArgusId != null) {
                try {
                    val identityJson = webClient.get()
                        .uri("api/identities/$existingArgusId")
                        .header("X-API-Key", apiKey)
                        .retrieve()
                        .bodyToMono(String::class.java)
                        .block()
                    val node = mapper.readTree(identityJson ?: "{}")
                    if (node?.has("id") == true) argusIdentity = node
                } catch (_: Exception) {}
            }

            // 2. Fall back to external_ref lookup
            if (argusIdentity == null) {
                val extRefJson = webClient.get()
                    .uri("api/identities?external_ref=$personId&type=face")
                    .header("X-API-Key", apiKey)
                    .retrieve()
                    .bodyToMono(String::class.java)
                    .block()
                argusIdentity = mapper.readTree(extRefJson ?: "{}")["items"]?.firstOrNull()
            }

            // 3. Fall back to name search — use GET /api/identities?q= so Argus filters server-side
            //    and there is no page-size cap on the result set to worry about
            if (argusIdentity == null) {
                val personName = person.getName()
                if (!personName.isNullOrBlank()) {
                    val encoded = java.net.URLEncoder.encode(personName, "UTF-8")
                    val searchJson = webClient.get()
                        .uri("api/identities?type=face&q=$encoded")
                        .header("X-API-Key", apiKey)
                        .retrieve()
                        .bodyToMono(String::class.java)
                        .block()
                    argusIdentity = mapper.readTree(searchJson ?: "{}")["items"]
                        ?.firstOrNull { it["label"]?.asText()?.equals(personName, ignoreCase = true) == true }
                }
            }

            if (argusIdentity == null) {
                response["status"] = ApiResponse.FAIL.status
                response["msg"] = "No matching Argus identity found for \"${person.getName()}\"."
            } else {
                val argusId = argusIdentity["id"].asInt()
                val argusLabel = argusIdentity["label"]?.takeUnless { it.isNull }?.asText() ?: ""
                val argusExternalRef = argusIdentity["external_ref"]?.takeUnless { it.isNull }?.asText()
                val messages = mutableListOf<String>()
                var changed = false

                if (person.getArgusIdentityId() != argusId) {
                    person.setArgusIdentityId(argusId)
                    changed = true
                    messages.add("Argus identity ID updated to $argusId")
                }

                // Sync name Argus → Shashin if different
                if (argusLabel.isNotBlank() && !argusLabel.equals(person.getName(), ignoreCase = true)) {
                    person.setName(argusLabel)
                    changed = true
                    messages.add("Name updated to \"$argusLabel\" from Argus")
                }

                if (changed) recognitionLabelRepository?.save(person)

                // Write Shashin person ID into Argus external_ref if not already set
                if (argusExternalRef != personId.toString()) {
                    val extRefBody = mapper.writeValueAsString(mapOf("external_ref" to personId.toString()))
                    webClient.put()
                        .uri("api/identities/$argusId/external_ref")
                        .header("X-API-Key", apiKey)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                        .body(BodyInserters.fromValue(extRefBody))
                        .retrieve().bodyToMono(String::class.java).block()
                    messages.add("Linked Shashin person $personId in Argus")
                }

                response["status"] = ApiResponse.SUCCESS.status
                response["msg"] = if (messages.isEmpty()) "Already in sync (identity ID $argusId)." else messages.joinToString("; ") + "."
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

            val galleryJson = getArgusGalleryForIdentity(settings, argusIdentityId, limit = size, cursor = cursor)
            if (galleryJson != null) {
                val galleryObj = mapper.readTree(galleryJson)
                val items = if (galleryObj.has("items")) galleryObj["items"].toList() else emptyList()
                response["next_cursor"] = galleryObj["next_cursor"]?.takeUnless { it.isNull }?.textValue()
                response["has_more"] = galleryObj["has_more"]?.asBoolean() ?: false
                if (items.isNotEmpty()) {
                    val detectionIds = items.map { it["detection_id"].asInt().toString() }
                    val rlpByDetectionId = recognitionLabelPhotoRepository
                        ?.findAllByArgusDetectionIdIn(detectionIds)
                        ?.filterNotNull()
                        ?.associateBy { it.getArgusDetectionId() ?: "" }
                        ?: emptyMap()
                    val metadataIds = rlpByDetectionId.values.mapNotNull { it.getMetadataId() }.distinct()
                    val metadataById = if (metadataIds.isNotEmpty())
                        metadataRepository?.findAllByMetadataIds(metadataIds.toTypedArray())
                            ?.associateBy { it.getId() ?: "" } ?: emptyMap()
                    else emptyMap()

                    val resultList = mutableListOf<MutableMap<String, Any>>()
                    for (item in items) {
                        val detectionId = item["detection_id"].asInt().toString()
                        val itemMap = mutableMapOf<String, Any>()
                        itemMap["id"] = detectionId
                        itemMap["crop_url"] = if (item.has("crop_url") && !item["crop_url"].isNull) item["crop_url"].textValue() else ""
                        itemMap["metadata_date"] = ""
                        itemMap["metadata_id"] = ""
                        val rlp = rlpByDetectionId[detectionId]
                        if (rlp?.getMetadataId() != null) {
                            val metadataObj = metadataById[rlp.getMetadataId()!!]
                            if (metadataObj != null) {
                                itemMap["metadata_date"] = "${metadataObj.getYear()}-${metadataObj.getMonth()}-${metadataObj.getDay()}"
                                itemMap["metadata_id"] = metadataObj.getId() ?: ""
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
                val rlp = recognitionLabelPhotoRepository?.findFirstByArgusDetectionId(detectionId) ?: continue
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

    private data class ArgusIdentityCounts(val detectionCount: Int, val pendingReviewCount: Int)

    private fun getArgusIdentityCounts(settings: Settings, argusIdentityId: Int?): ArgusIdentityCounts {
        if (argusIdentityId == null || settings.getArgusServer().isNullOrBlank() || settings.getArgusKey().isNullOrBlank())
            return ArgusIdentityCounts(0, 0)
        return try {
            val json = WebClient.create(settings.getArgusServer()!!)
                .get()
                .uri("api/identities/$argusIdentityId")
                .header("X-API-Key", settings.getArgusKey())
                .retrieve().bodyToMono(String::class.java).block() ?: return ArgusIdentityCounts(0, 0)
            val node = mapper.readTree(json)
            ArgusIdentityCounts(
                detectionCount = node["detection_count"]?.asInt() ?: 0,
                pendingReviewCount = node["pending_review_count"]?.asInt() ?: 0
            )
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error getting Argus identity counts for $argusIdentityId: ${e.localizedMessage}")
            ArgusIdentityCounts(0, 0)
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
            WebClient.builder()
                .baseUrl(settings.getArgusServer()!!)
                .codecs { it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024) }
                .build()
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

    @GetMapping("/people/{personId}/argus-crop")
    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @ResponseBody
    fun getArgusPersonCrop(model: Model, @PathVariable personId: Int): ResponseEntity<ByteArray> {
        val settings = model.getAttribute("settings") as Settings
        if (settings.getArgusServer().isNullOrBlank() || settings.getArgusKey().isNullOrBlank()) {
            return ResponseEntity.notFound().build()
        }
        val path = recognitionLabelRepository?.findById(personId)
            ?.takeIf { it.isPresent }?.get()?.getArgusThumbnailPath()
        if (path.isNullOrBlank()) return ResponseEntity.notFound().build()

        return try {
            val exchangeStrategies = org.springframework.web.reactive.function.client.ExchangeStrategies.builder()
                .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }
                .build()
            val bytes = WebClient.builder()
                .baseUrl(settings.getArgusServer()!!)
                .exchangeStrategies(exchangeStrategies)
                .build()
                .get()
                .uri(path)
                .header("X-API-Key", settings.getArgusKey())
                .retrieve()
                .bodyToMono(ByteArray::class.java)
                .block() ?: return ResponseEntity.notFound().build()

            ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                .body(bytes)
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error proxying Argus crop for person $personId: ${e.localizedMessage}")
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/person/argus/crop")
    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @ResponseBody
    fun getArgusTrainingCrop(model: Model, @RequestParam path: String): ResponseEntity<ByteArray> {
        val settings = model.getAttribute("settings") as Settings
        if (settings.getArgusServer().isNullOrBlank() || settings.getArgusKey().isNullOrBlank()) {
            return ResponseEntity.notFound().build()
        }
        return try {
            val exchangeStrategies = org.springframework.web.reactive.function.client.ExchangeStrategies.builder()
                .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }
                .build()
            val bytes = WebClient.builder()
                .baseUrl(settings.getArgusServer()!!)
                .exchangeStrategies(exchangeStrategies)
                .build()
                .get()
                .uri(path)
                .header("X-API-Key", settings.getArgusKey())
                .retrieve()
                .bodyToMono(ByteArray::class.java)
                .block() ?: return ResponseEntity.notFound().build()

            ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                .body(bytes)
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error proxying Argus crop: ${e.localizedMessage}")
            ResponseEntity.notFound().build()
        }
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
        model["argusCoverPersonIds"] = emptySet<Int>()

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            var peopleList: List<MetadataPeople>? = null
            val settings = model.getAttribute("settings") as Settings

            if (currentUserObj.getAuthority() == model.getAttribute("userRole")) {
                peopleList = metadataRepository?.findAlbumPhotoByPeople(
                    currentUserObj.getId(),
                    TextUtils.getObjectName()
                )?.toList()
            } else if (currentUserObj.getAuthority() == model.getAttribute("adminRole") || currentUserObj.getAuthority() == model.getAttribute("superRole")) {
                peopleList = metadataRepository?.findAllPeopleWithTagCount(
                    TextUtils.getObjectName()
                )?.toList()
            }

            if (!peopleList.isNullOrEmpty()) {
                // Matches badges reflect the Argus review queue (same source the Matches tab renders).
                // Fetch the queue once and bucket by identity, and map each person to its argusIdentityId.
                val reviewCountsByIdentity = countArgusReviewMatchesByIdentity(settings)
                val allLabels = recognitionLabelRepository?.findAll()?.filterNotNull() ?: emptyList()
                val argusIdByLabelId = allLabels.associate { it.getId() to it.getArgusIdentityId() }
                val argusCoverPersonIds = allLabels.filter { it.getArgusThumbnailPath() != null }.map { it.getId() }.toSet()

                // Batch-load cover URL → metadata ID in one query instead of one per person
                val rawCoverUrls = peopleList.mapNotNull { it.getCoverUrl()?.toString() }.toSet()
                val coverUrlToId: Map<String, String> = if (rawCoverUrls.isNotEmpty()) {
                    metadataRepository?.findAllByThumbnailCenteredIn(rawCoverUrls)
                        ?.associate { it.getThumbnailUrlCentered()!! to it.getId() } ?: emptyMap()
                } else emptyMap()

                for (person in peopleList) {
                    var coverUrl = ""
                    val rawCover = person.getCoverUrl()?.toString()
                    if (rawCover != null) {
                        val id = coverUrlToId[rawCover]
                        if (id != null) coverUrl = "/api/v1/thumbnails/centered/$id"
                    }
                    if (coverUrl.isEmpty() && person.getThumbnailUrlCentered() != null) {
                        coverUrl = person.getThumbnailUrlCentered()!!
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
                model["argusCoverPersonIds"] = argusCoverPersonIds
            }

            if (!peopleList.isNullOrEmpty()) {
                model["peopleList"] = peopleList
                model["message"] = ""
            }
        }

        val settings = model.getAttribute("settings") as? Settings
        model["faceRecogServicesAvailable"] = NetworkUtils.checkArgusConnection(
            settings?.getArgusServer(), settings?.getArgusKey()
        )
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

    @RequestMapping(value = ["/person/{personId}/rename"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["text/plain"])
    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @ResponseBody
    fun renamePerson(model: Model, @PathVariable personId: Int, @RequestBody requestBody: JsonNode): org.springframework.http.ResponseEntity<String> {
        val newName = requestBody.get("name")?.asText()?.trim()
        if (newName.isNullOrBlank()) {
            return org.springframework.http.ResponseEntity.badRequest().body("Name cannot be empty")
        }
        val labelOpt = recognitionLabelRepository?.findById(personId)
        if (labelOpt == null || !labelOpt.isPresent) {
            return org.springframework.http.ResponseEntity.notFound().build()
        }
        val conflicting = recognitionLabelRepository?.findByNameIgnoreCase(newName)
        if (conflicting != null && conflicting.getId() != personId) {
            return org.springframework.http.ResponseEntity.badRequest().body("A person with that name already exists")
        }
        val label = labelOpt.get()
        label.setName(newName)
        recognitionLabelRepository?.save(label)

        val settings = model.getAttribute("settings") as Settings
        val argusIdentityId = label.getArgusIdentityId()
        if (argusIdentityId != null && !settings.getArgusServer().isNullOrBlank() && !settings.getArgusKey().isNullOrBlank()) {
            try {
                val webClient = WebClient.create(settings.getArgusServer()!!)
                val body = mapper.writeValueAsString(mapOf("label" to newName))
                webClient.put()
                    .uri("api/identities/$argusIdentityId")
                    .header("X-API-Key", settings.getArgusKey()!!)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(body)
                    .retrieve().bodyToMono(String::class.java).block()
            } catch (_: Exception) {}
        }

        return org.springframework.http.ResponseEntity.ok("OK")
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

            val argusIdentityId = if (recognitionLabel != null && recognitionLabel.isPresent) recognitionLabel.get().getArgusIdentityId() else null

            var metadataList: MutableIterable<Metadata>? = null
            if (currentUserObj!!.getAuthority() == model.getAttribute("userRole")) {
                metadataList = metadataRepository?.findAlbumPhotoByPerson(
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

                metadataList = metadataRepository?.findMetadataByPerson(
                    personId,
                    pageValue,
                    size
                )
            }

            val albumList = albumRepository?.findAllOrderByAlbumName()
            if (albumList != null && albumList.count() > 0) {
                response["allAlbumList"] = albumList
            }

            val faceRecogServicesAvailable = NetworkUtils.checkArgusConnection(
                settings.getArgusServer(),
                settings.getArgusKey()
            )
            response["faceRecogServicesAvailable"] = faceRecogServicesAvailable
            val argusId = recognitionLabel?.get()?.getArgusIdentityId()
            val argusCounts = getArgusIdentityCounts(settings, argusId)
            counts["training"] = argusCounts.detectionCount
            counts["matches"] = argusCounts.pendingReviewCount

            if (metadataList != null && metadataList.count() > 0) {
                var personCount = 0
                if (currentUserObj.getAuthority() == model.getAttribute("userRole")) {
                    personCount = metadataRepository?.countByPhotoAlbumByPerson(personId,currentUserObj.getId())!!
                } else if (currentUserObj.getAuthority() == model.getAttribute("adminRole") || currentUserObj.getAuthority() == model.getAttribute("superRole")) {
                    personCount = metadataRepository?.countByMetadataByPerson(personId)!!
                }
                if (personCount > 0) {
                    counts["person"] = personCount
                } else {
                    counts["person"] = 0
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
                    val nameTaggedMap = mutableMapOf<String,Any>()
                    if (recognitionLabelPhotos != null) {
                        for (recognitionLabelPhoto in recognitionLabelPhotos) {
                            val recognitionLabelObj = recognitionLabelPhoto.getRecognitionLabelId()?.let { recognitionLabelRepository?.findById(it) }
                            if (recognitionLabelObj != null && recognitionLabelObj.get().getName() != TextUtils.getObjectName()) {
                                labelString += recognitionLabelObj.get().getName() + ","
                            }
                        }
                    }

                    if (labelString.isNotBlank()) {
                        labelString = labelString.dropLast(1)
                        nameTaggedMap["labels"] = labelString
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
                    val settingsForDelete = model.getAttribute("settings") as? Settings
                    if (!settingsForDelete?.getArgusServer().isNullOrBlank() &&
                        !settingsForDelete?.getArgusKey().isNullOrBlank()) {
                        val deleteClient = WebClient.create(settingsForDelete!!.getArgusServer()!!)
                        recognitionLabelPhotoRepository?.findByMetadataId(metadataId)?.forEach { existing ->
                            val argDetId = existing?.getArgusDetectionId()
                            if (!argDetId.isNullOrBlank()) {
                                try {
                                    deleteClient.delete()
                                        .uri("api/detections/$argDetId")
                                        .header("X-API-Key", settingsForDelete.getArgusKey())
                                        .retrieve().bodyToMono(String::class.java).block()
                                } catch (_: Exception) {}
                            }
                        }
                    }
                    recognitionLabelPhotoRepository?.deleteByMetadataId(metadataId)
                }

                val settings = model.getAttribute("settings") as Settings
                val argusConfigured = !settings.getArgusServer().isNullOrBlank() && !settings.getArgusKey().isNullOrBlank()

                for (recognitionLabelString in recognitionLabelArray) {
                    if (recognitionLabelString.trim().isNotBlank() && recognitionLabelString.trim() != "null") {
                        var recognitionLabelPhotoCount: Int
                        var recognitionLabelObj = RecognitionLabel()

                        val recognitionLabelRecord =
                            recognitionLabelRepository?.findByNameIgnoreCase(recognitionLabelString.trim())

                        val wasNewlyCreated = recognitionLabelRecord == null

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
                                    settings,
                                    recognitionLabelString.trim(),
                                    metadata?.get(),
                                    argusDetectionIdMap,
                                    recognitionLabelRepository
                                )
                            )
                            val jsonRespObj = mapper.readTree(uploadResp)

                            var detectionId: String? = null
                            var sourceImageId: String? = null
                            if (jsonRespObj.has("responseData")) {
                                val rd = jsonRespObj["responseData"]
                                if (rd.has("detection_id")) detectionId = rd["detection_id"].toString()
                                if (rd.has("source_image_id") && !rd["source_image_id"].isNull)
                                    sourceImageId = rd["source_image_id"].asInt().toString()
                            }

                            if (argusConfigured && detectionId == null) {
                                if (wasNewlyCreated) recognitionLabelRepository?.delete(recognitionLabelObj)
                                resp["msg"] = "No face detected in this photo. Try a clearer image."
                                resp["status"] = ApiResponse.FAIL.status
                                return mapper.writeValueAsString(resp)
                            }

                            val recognitionLabelPhotoObj = RecognitionLabelPhoto()
                            recognitionLabelPhotoObj.setMetadataId(metadataId)
                            recognitionLabelPhotoObj.setRecognitionLabelId(recognitionLabelObj.getId())
                            recognitionLabelPhotoObj.setConfidence("0.0")
                            recognitionLabelPhotoObj.setArgusDetectionId(detectionId)
                            if (sourceImageId != null) recognitionLabelPhotoObj.setArgusSourceImageId(sourceImageId)
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

    // Detect all faces in a photo via Argus (no label) and return detection IDs + bboxes.
    // Uses existing recognitionlabelphoto records when available; otherwise calls Argus and stores results.
    @GetMapping("/person/faces/{metadataId}")
    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @ResponseBody
    fun getPhotoFaces(model: Model, @PathVariable metadataId: String): String {
        val response = mutableMapOf<String, Any?>()
        val settings = model.getAttribute("settings") as Settings

        if (settings.getArgusServer().isNullOrBlank() || settings.getArgusKey().isNullOrBlank()) {
            response["faces"] = emptyList<Any>()
            return mapper.writeValueAsString(response)
        }

        val argusServer = settings.getArgusServer()!!.trimEnd('/')

        // Use existing detection records when available
        val existingRlps = recognitionLabelPhotoRepository?.findByMetadataId(metadataId)
            ?.filterNotNull()?.filter { !it.getArgusDetectionId().isNullOrBlank() } ?: emptyList()

        if (existingRlps.isNotEmpty()) {
            val faces = existingRlps.map { rlp ->
                val person = rlp.getRecognitionLabelId()?.let { recognitionLabelRepository?.findById(it)?.orElse(null) }
                mapOf(
                    "detectionId" to rlp.getArgusDetectionId(),
                    "personId" to person?.getId(),
                    "personName" to person?.getName(),
                    "autoTagged" to (rlp.getAutoTagged() ?: true)
                )
            }
            response["faces"] = faces
            return mapper.writeValueAsString(response)
        }

        // No cached detections — ask Argus to detect without labeling
        val metadata = metadataRepository?.findByMetadataId(metadataId) ?: run {
            response["faces"] = emptyList<Any>()
            return mapper.writeValueAsString(response)
        }
        val imagePath = com.miyagi.shashin.service.ImageProcessing.Companion.argusImagePath(metadata)
        if (imagePath == null) {
            response["faces"] = emptyList<Any>()
            return mapper.writeValueAsString(response)
        }

        return try {
            val webClient = WebClient.create("$argusServer/")
            val builder = org.springframework.http.client.MultipartBodyBuilder()
            builder.part("file", org.springframework.core.io.FileSystemResource(imagePath))
            builder.part("replace", "true")

            val detectResp = webClient.post().uri("api/detect/faces")
                .header("X-API-Key", settings.getArgusKey())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA.toString())
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve().bodyToMono(String::class.java).block()

            val jsonObj = mapper.readTree(detectResp ?: "{}")
            val faces = mutableListOf<Map<String, Any?>>()

            if (jsonObj.has("faces")) {
                for (face in jsonObj["faces"]) {
                    val detectionId = face["detection_id"]?.asInt()?.toString() ?: continue
                    val bbox = if (face.has("bbox")) mapper.convertValue(face["bbox"], Map::class.java) else null
                    val cropUrl = face["crop_url"]?.takeUnless { it.isNull }?.asText()
                        ?.let { "$argusServer$it" }

                    var personId: Int? = null
                    var personName: String? = null
                    if (face.has("identity_id") && !face["identity_id"].isNull) {
                        val identityId = face["identity_id"].asInt()
                        val person = recognitionLabelRepository?.findByArgusIdentityId(identityId)
                        personId = person?.getId()
                        personName = person?.getName()
                    }

                    // Store detection in Shashin if not already recorded
                    if (recognitionLabelPhotoRepository?.findFirstByArgusDetectionId(detectionId) == null) {
                        val rlp = RecognitionLabelPhoto()
                        rlp.setMetadataId(metadataId)
                        rlp.setArgusDetectionId(detectionId)
                        rlp.setAutoTagged(true)
                        rlp.setConfidence("0.0")
                        if (personId != null) rlp.setRecognitionLabelId(personId)
                        jsonObj["source_image_id"]?.takeUnless { it.isNull }?.asInt()?.toString()
                            ?.let { rlp.setArgusSourceImageId(it) }
                        try { recognitionLabelPhotoRepository?.save(rlp) } catch (_: Exception) {}
                    }

                    faces.add(mapOf(
                        "detectionId" to detectionId,
                        "personId" to personId,
                        "personName" to personName,
                        "bbox" to bbox,
                        "cropUrl" to cropUrl,
                        "autoTagged" to true
                    ))
                }
            }
            response["faces"] = faces
            mapper.writeValueAsString(response)
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error detecting faces for $metadataId: ${e.localizedMessage}")
            response["faces"] = emptyList<Any>()
            mapper.writeValueAsString(response)
        }
    }

    // Label a specific Argus detection as a person, then update Shashin's recognitionlabelphoto.
    @PostMapping("/person/faces/label")
    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @ResponseBody
    fun labelFace(model: Model, @RequestBody body: JsonNode, locale: Locale): String {
        val response = mutableMapOf<String, Any?>()
        val settings = model.getAttribute("settings") as Settings
        val detectionId = body["detectionId"]?.asText()?.takeIf { it.isNotBlank() }
            ?: return mapper.writeValueAsString(response.also { it["status"] = ApiResponse.FAIL.status })
        val metadataId = body["metadataId"]?.asText()?.takeIf { it.isNotBlank() }
            ?: return mapper.writeValueAsString(response.also { it["status"] = ApiResponse.FAIL.status })
        val personId = body["personId"]?.takeUnless { it.isNull }?.asInt()
        val personName = body["personName"]?.takeUnless { it.isNull }?.asText()?.trim()

        var person = personId?.let { recognitionLabelRepository?.findById(it)?.orElse(null) }
        if (person == null && !personName.isNullOrBlank()) {
            person = recognitionLabelRepository?.findByNameIgnoreCase(personName) ?: run {
                val newP = RecognitionLabel()
                newP.setName(personName)
                newP.setCreatedAt(getCurrentTimestamp())
                newP.setModifiedAt(getCurrentTimestamp())
                recognitionLabelRepository?.save(newP)
            }
        }
        if (person == null) {
            response["status"] = ApiResponse.FAIL.status
            response["msg"] = "Person not found"
            return mapper.writeValueAsString(response)
        }

        if (!settings.getArgusServer().isNullOrBlank() && !settings.getArgusKey().isNullOrBlank()) {
            try {
                val webClient = WebClient.create(settings.getArgusServer()!!)
                val labelBody = mapper.writeValueAsString(mapOf("label" to person.getName(), "enroll" to true))
                val labelResp = webClient.put()
                    .uri("api/detections/$detectionId/label")
                    .header("X-API-Key", settings.getArgusKey())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(labelBody)
                    .retrieve().bodyToMono(String::class.java).block()
                val labelJson = mapper.readTree(labelResp ?: "{}")
                val returnedIdentityId = labelJson["identity_id"]?.takeUnless { it.isNull }?.asInt()
                if (returnedIdentityId != null && person.getArgusIdentityId() != returnedIdentityId) {
                    person.setArgusIdentityId(returnedIdentityId)
                    recognitionLabelRepository?.save(person)
                    try {
                        val extRefBody = mapper.writeValueAsString(mapOf("external_ref" to person.getId().toString()))
                        webClient.put()
                            .uri("api/identities/$returnedIdentityId/external_ref")
                            .header("X-API-Key", settings.getArgusKey())
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .bodyValue(extRefBody)
                            .retrieve().bodyToMono(String::class.java).block()
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Error labeling detection $detectionId: ${e.localizedMessage}")
            }
        }

        val rlp = recognitionLabelPhotoRepository?.findFirstByArgusDetectionId(detectionId) ?: RecognitionLabelPhoto().also {
            it.setMetadataId(metadataId)
            it.setArgusDetectionId(detectionId)
        }
        rlp.setRecognitionLabelId(person.getId())
        rlp.setConfidence("0.0")
        rlp.setAutoTagged(false)
        try { recognitionLabelPhotoRepository?.save(rlp) } catch (_: Exception) {}

        response["status"] = ApiResponse.SUCCESS.status
        response["msg"] = "Labeled as ${person.getName()}"
        return mapper.writeValueAsString(response)
    }

    @RequestMapping(value = ["/person/argus/reconcile"], method = [RequestMethod.POST], produces = ["application/json"])
    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @ResponseBody
    @CacheEvict(value = ["peopleTagCounts"], allEntries = true)
    fun reconcileArgus(model: Model, locale: Locale): String {
        val response = mutableMapOf<String, Any?>()
        val settings = model.getAttribute("settings") as Settings
        return try {
            argusReconcile?.run(settings)
            response["status"] = ApiResponse.SUCCESS.status
            response["msg"] = "Reconcile complete."
            mapper.writeValueAsString(response)
        } catch (e: Exception) {
            response["status"] = ApiResponse.FAIL.status
            response["msg"] = "Error: ${e.localizedMessage}"
            mapper.writeValueAsString(response)
        }
    }

    @RequestMapping(value = ["/person/search"], method = [RequestMethod.GET], produces = ["application/json"])
    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @ResponseBody
    fun searchPeople(@RequestParam q: String): String {
        val results = recognitionLabelRepository?.searchByName(q) ?: emptyList()
        val list = results.filterNotNull().map { mapOf("id" to it.getId(), "name" to it.getName()) }
        return mapper.writeValueAsString(list)
    }

    @RequestMapping(value = ["/person/matches/reassign"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @ResponseBody
    fun reassignMatch(model: Model, @RequestBody requestBody: JsonNode, locale: Locale): String {
        val response = mutableMapOf<String, Any?>()
        response["status"] = ApiResponse.FAIL.status
        response["msg"] = ""
        val bodyMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        val detectionId = bodyMap["detectionId"]?.toString() ?: return mapper.writeValueAsString(response)
        val cropUrl = bodyMap["cropUrl"]?.toString() ?: return mapper.writeValueAsString(response)
        val targetName = bodyMap["targetName"]?.toString()?.trim() ?: return mapper.writeValueAsString(response)
        val settings = model.getAttribute("settings") as Settings
        if (settings.getArgusServer().isNullOrBlank()) return mapper.writeValueAsString(response)

        var targetPerson = recognitionLabelRepository?.findByNameIgnoreCase(targetName)
        if (targetPerson == null) {
            val newLabel = RecognitionLabel()
            newLabel.setName(targetName)
            targetPerson = recognitionLabelRepository?.save(newLabel)
        }
        if (targetPerson == null) return mapper.writeValueAsString(response)

        val webClient = WebClient.create(settings.getArgusServer()!!)
        val argusIdentityId = targetPerson.getArgusIdentityId()
        try {
            if (argusIdentityId != null) {
                val reassignBody = mapper.writeValueAsString(mapOf("identity_id" to argusIdentityId))
                webClient.post().uri("api/review/$detectionId/reassign")
                    .header("X-API-Key", settings.getArgusKey())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                    .body(BodyInserters.fromValue(reassignBody))
                    .retrieve().bodyToMono(String::class.java).block()
            } else {
                val labelBody = mapper.writeValueAsString(mapOf("label" to targetName))
                val labelResp = webClient.put()
                    .uri("api/detections/$detectionId/label")
                    .header("X-API-Key", settings.getArgusKey())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                    .body(BodyInserters.fromValue(labelBody))
                    .retrieve().bodyToMono(String::class.java).block()
                val labelJson = mapper.readTree(labelResp ?: "{}")
                val returnedIdentityId = labelJson["identity_id"]?.asInt()
                if (returnedIdentityId != null) {
                    targetPerson.setArgusIdentityId(returnedIdentityId)
                    recognitionLabelRepository?.save(targetPerson)
                    try {
                        val extRefBody = mapper.writeValueAsString(mapOf("external_ref" to targetPerson.getId().toString()))
                        webClient.put()
                            .uri("api/identities/$returnedIdentityId/external_ref")
                            .header("X-API-Key", settings.getArgusKey())
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                            .body(BodyInserters.fromValue(extRefBody))
                            .retrieve().bodyToMono(String::class.java).block()
                    } catch (_: Exception) {}
                }
                webClient.post().uri("api/review/$detectionId/confirm")
                    .header("X-API-Key", settings.getArgusKey())
                    .retrieve().bodyToMono(String::class.java).block()
            }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Reassign match failed: ${e.localizedMessage}")
        }
        val record = recognitionLabelPhotoRepository?.findFirstByArgusDetectionId(detectionId)
        if (record != null) {
            record.setRecognitionLabelId(targetPerson.getId())
            record.setAutoTagged(false)
            record.setConfidence("0.0")
            try { recognitionLabelPhotoRepository?.save(record) } catch (_: Exception) {}
        }
        response["status"] = ApiResponse.SUCCESS.status
        response["msg"] = "Reassigned to ${targetPerson.getName()}"
        return mapper.writeValueAsString(response)
    }

    @RequestMapping(value = ["/person/training/reassign"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @ResponseBody
    fun reassignTraining(model: Model, @RequestBody requestBody: JsonNode, locale: Locale): String {
        val response = mutableMapOf<String, Any?>()
        response["status"] = ApiResponse.FAIL.status
        response["msg"] = ""
        val bodyMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        val detectionId = bodyMap["detectionId"]?.toString() ?: return mapper.writeValueAsString(response)
        val cropUrl = bodyMap["cropUrl"]?.toString() ?: return mapper.writeValueAsString(response)
        val targetName = bodyMap["targetName"]?.toString()?.trim() ?: return mapper.writeValueAsString(response)
        val settings = model.getAttribute("settings") as Settings
        if (settings.getArgusServer().isNullOrBlank()) return mapper.writeValueAsString(response)

        var targetPerson = recognitionLabelRepository?.findByNameIgnoreCase(targetName)
        if (targetPerson == null) {
            val newLabel = RecognitionLabel()
            newLabel.setName(targetName)
            targetPerson = recognitionLabelRepository?.save(newLabel)
        }
        if (targetPerson == null) return mapper.writeValueAsString(response)

        val webClient = WebClient.create(settings.getArgusServer()!!)
        var newDetectionId: String? = null
        try {
            val cropBytes = WebClient.create().get().uri(cropUrl)
                .header("X-API-Key", settings.getArgusKey())
                .retrieve().bodyToMono(ByteArray::class.java).block()
            if (cropBytes != null) {
                val resource = object : ByteArrayResource(cropBytes) { override fun getFilename() = "crop.jpg" }
                val builder = MultipartBodyBuilder()
                builder.part("file", resource)
                builder.part("label", targetName)
                builder.part("replace", "true")
                val enrollResp = webClient.post().uri("api/detect/faces")
                    .header("X-API-Key", settings.getArgusKey())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA.toString())
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve().bodyToMono(String::class.java).block()
                val enrollJson = mapper.readTree(enrollResp ?: "{}")
                newDetectionId = enrollJson["faces"]?.get(0)?.get("detection_id")?.asInt()?.toString()
                val newIdentityId = enrollJson["faces"]?.get(0)?.get("identity_id")?.asInt()
                if (newIdentityId != null && targetPerson.getArgusIdentityId() != newIdentityId) {
                    targetPerson.setArgusIdentityId(newIdentityId)
                    recognitionLabelRepository?.save(targetPerson)
                }
                if (newIdentityId != null) {
                    try {
                        val extRefBody = mapper.writeValueAsString(mapOf("external_ref" to targetPerson.getId().toString()))
                        webClient.put()
                            .uri("api/identities/$newIdentityId/external_ref")
                            .header("X-API-Key", settings.getArgusKey())
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                            .body(BodyInserters.fromValue(extRefBody))
                            .retrieve().bodyToMono(String::class.java).block()
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Reassign training enroll failed: ${e.localizedMessage}")
        }
        try {
            webClient.delete().uri("api/detections/$detectionId")
                .header("X-API-Key", settings.getArgusKey())
                .retrieve().bodyToMono(String::class.java).block()
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Reassign training delete old detection failed: ${e.localizedMessage}")
        }
        val record = recognitionLabelPhotoRepository?.findFirstByArgusDetectionId(detectionId)
        if (record != null) {
            record.setRecognitionLabelId(targetPerson.getId())
            record.setAutoTagged(false)
            record.setConfidence("0.0")
            if (newDetectionId != null) record.setArgusDetectionId(newDetectionId)
            try { recognitionLabelPhotoRepository?.save(record) } catch (_: Exception) {}
        }
        response["status"] = ApiResponse.SUCCESS.status
        response["msg"] = "Reassigned to ${targetPerson.getName()}"
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

                        if (personIdParam != null) {
                            val person = recognitionLabelRepository?.findById(personIdParam)
                            val personName = person?.takeIf { it.isPresent }?.get()?.getName()
                            if (!personName.isNullOrBlank()) {
                                val labelBody = mapper.writeValueAsString(mapOf("label" to personName))
                                val labelResp = webClient.put()
                                    .uri("api/detections/$detectionId/label")
                                    .header("X-API-Key", settings.getArgusKey())
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                                    .body(BodyInserters.fromValue(labelBody))
                                    .retrieve().bodyToMono(String::class.java).block()

                                val labelJson = mapper.readTree(labelResp ?: "{}")
                                val returnedIdentityId = labelJson["identity_id"]?.asInt()
                                if (returnedIdentityId != null) {
                                    val p = person.get()
                                    if (p.getArgusIdentityId() != returnedIdentityId) {
                                        p.setArgusIdentityId(returnedIdentityId)
                                        recognitionLabelRepository?.save(p)
                                    }
                                    try {
                                        val extRefBody = mapper.writeValueAsString(mapOf("external_ref" to personIdParam.toString()))
                                        webClient.put()
                                            .uri("api/identities/$returnedIdentityId/external_ref")
                                            .header("X-API-Key", settings.getArgusKey())
                                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                                            .body(BodyInserters.fromValue(extRefBody))
                                            .retrieve().bodyToMono(String::class.java).block()
                                    } catch (_: Exception) {}
                                }

                                try {
                                    webClient.post().uri("api/review/$detectionId/confirm")
                                        .header("X-API-Key", settings.getArgusKey())
                                        .retrieve().bodyToMono(String::class.java).block()
                                } catch (_: Exception) {}
                            }
                        }

                        val record = recognitionLabelPhotoRepository?.findFirstByArgusDetectionId(detectionId)
                        if (record != null && personIdParam != null) {
                            record.setRecognitionLabelId(personIdParam)
                            record.setAutoTagged(false)
                            record.setConfidence("0.0")
                            try { recognitionLabelPhotoRepository?.save(record) } catch (_: Exception) {}
                            record.getMetadataId()?.let { mid ->
                                metadataRepository?.findById(mid)?.orElse(null)?.let { m ->
                                    m.setModifiedAt(TextUtils.getCurrentTimestamp())
                                    metadataRepository?.save(m)
                                }
                            }
                        }
                    }
                    "reject" -> {
                        webClient.post()
                            .uri("api/review/$detectionId/reject")
                            .header("X-API-Key", settings.getArgusKey())
                            .retrieve().bodyToMono(String::class.java).block()

                        recognitionLabelPhotoRepository?.findFirstByArgusDetectionId(detectionId)
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

                            val record = recognitionLabelPhotoRepository?.findFirstByArgusDetectionId(detectionId)
                            if (record != null) {
                                val label = recognitionLabelRepository?.findByArgusIdentityId(identityId)
                                if (label != null) {
                                    record.setRecognitionLabelId(label.getId())
                                    record.setConfidence("0.0")
                                    try { recognitionLabelPhotoRepository?.save(record) } catch (_: Exception) {}
                                }
                                record.getMetadataId()?.let { mid ->
                                    metadataRepository?.findById(mid)?.orElse(null)?.let { m ->
                                        m.setModifiedAt(TextUtils.getCurrentTimestamp())
                                        metadataRepository?.save(m)
                                    }
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