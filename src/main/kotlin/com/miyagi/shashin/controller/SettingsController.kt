package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.miyagi.shashin.component.Message
import com.miyagi.shashin.component.ScanMessage
import com.miyagi.shashin.model.*
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.service.RestartService
import com.miyagi.shashin.util.*
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.CacheEvict
import org.springframework.context.event.EventListener
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.InputStreamResource
import org.springframework.data.domain.Sort
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.annotation.SubscribeMapping
import org.springframework.security.access.annotation.Secured
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import org.springframework.web.socket.messaging.SessionConnectEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import org.springframework.web.socket.messaging.SessionSubscribeEvent
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession
import javax.transaction.Transactional
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString
import kotlinx.coroutines.*

@Suppress("UNCHECKED_CAST")
@Controller
class SettingsController {

    @Value("\${app.api.version}")
    private var apiVersion: String? = null

    @Value("\${app.endpoint.url.geocode}")
    private var geocodeUrl: String? = null

    @Value("\${app.sidecar.path}")
    private var relativeSidecarDir: String? = null

    @Value("\${app.build.properties.name}")
    private val appName: String? = null

    @Autowired
    private val metadataRepository: MetadataRepository? = null

    @Autowired
    private val mediaDirRepository: MediaDirectoryRepository? = null

    @Autowired
    private val userRepository: UserRepository? = null

    @Autowired
    private val userAlbumRepository: UserAlbumRepository? = null

    @Autowired
    private val favoriteRepository: FavoriteRepository? = null

    @Autowired
    private val commentRepository: CommentRepository? = null

    @Autowired
    private val albumPhotoCommentRepository: AlbumPhotoCommentRepository? = null

    @Autowired
    private val albumCommentRepository: AlbumCommentRepository? = null

    @Autowired
    private val albumRepository: AlbumRepository? = null

    @Autowired
    private val albumPhotoRepository: AlbumPhotoRepository? = null

    @Autowired
    private val notificationRepository: NotificationRepository? = null

    @Autowired
    private val recognitionLabelRepository: RecognitionLabelRepository? = null

    @Autowired
    private val recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository? = null

    @Autowired
    private val keywordRepository: KeywordRepository? = null

    @Autowired
    private val keywordPhotoRepository: KeywordPhotoRepository? = null

    @Autowired
    private val settingsRepository: SettingsRepository? = null

    @Autowired
    private val restartService: RestartService? = null

    @Value("\${app.role.admin}")
    private var adminRole: String? = null

    @Value("\${spring.datasource.url}")
    private var dataSourceUrl: String? = null

    private var bcrypt = BCryptPasswordEncoder()

    private var shouldStop = AtomicBoolean(false)

    private var logger: Logger = Logger.getLogger(SettingsController::class.simpleName)

    private var alreadyScannedFilepaths = mutableListOf<String>()

    private var scanCount: Int = 0

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()

    @MessageMapping("/scanmessage")
    @SendTo("/topic/messages")
    @Throws(java.lang.Exception::class)
    fun sendScanMessage(message: ScanMessage): Message? {
        //println("message:${message.getMessage()}")
        var msg = "Start Scan"

        val mediaDirs = mediaDirRepository?.findByExclude(false)
        if (mediaDirs != null && mediaDirs.count() > 0) {
            if (!FileUtils.checkThreadFileAlive("shashinscan")) {
                msg = "Scan Complete"
                if (shouldStop.get()) {
                    msg = "Scan Cancelled"
                }
            } else {
                val threadFileContent = FileUtils.readThreadFile("shashinscan")
                if (threadFileContent != null) {
                    msg = "Scan in progress: "
                    if (shouldStop.get()) {
                        msg = "Scan cancellation in progress: "
                    }
                    msg += threadFileContent.replace("\\", "/")
                }
            }
        } else {
            msg = "No directories configured"
        }

        val messageObj = Message()
        messageObj.setContent(msg)

        return messageObj
    }

    @SubscribeMapping("/topic/messages")
    fun subscribe(
        session: HttpSession,
        @PathVariable pipelineId: String,
        @PathVariable topic: String
    ) {
        //println("subscribe")
        //println(session.id)
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

    @Secured("ROLE_ADMIN")
    @RequestMapping(value = ["/settings"], method = [RequestMethod.GET])
    fun getSettings(model: Model, @ModelAttribute("settings") settings: Settings): String {
        val mediaDirectories = mediaDirRepository?.findByExclude(false)
        val mediaExcludeDirectories = mediaDirRepository?.findByExclude(true)

        val module = "settings"
        model["message"] = ""
        model["mediaDirList"] = ""
        model["mediaExcludeDirList"] = ""
        model["settings"] = ""
        model["alertClass"] = ""

        var dirDneString = ""
        if (model.getAttribute("authority").toString() == model.getAttribute("adminRole") && mediaDirectories != null && mediaExcludeDirectories != null) {
            model["mediaDirList"] = mediaDirectories.joinToString { "${it?.getDirectory()}" }
            model["mediaExcludeDirList"] = mediaExcludeDirectories.joinToString { "${it?.getDirectory()}" }

            for (mediaDir in mediaDirectories) {
                if (mediaDir != null) {
                    val path: Path = Paths.get(mediaDir.getDirectory()!!)
                    if (!Files.exists(path)) {
                        dirDneString += "${mediaDir.getDirectory()},"
                    }
                }
            }

            for (mediaDir in mediaExcludeDirectories) {
                if (mediaDir != null) {
                    val path: Path = Paths.get(mediaDir.getDirectory()!!)
                    if (!Files.exists(path)) {
                        dirDneString += "${mediaDir.getDirectory()},"
                    }
                }
            }

            if (dirDneString.isNotBlank()) {
                dirDneString = "Cannot find "+dirDneString.dropLast(1)
                model["alertClass"] = "alert-warning"
            }

            model.addAttribute("settings", settings)
        }

        val faceRecogServicesAvailable = model.getAttribute("faceRecogServicesAvailable").toString().toBoolean()
        model["faceRecogAvailableStatusIcon"] = "bi-x-circle"
        model["faceRecogAvailableStatusColor"] = "red"
        model["faceRecogAvailableStatusText"] = "Could not connect to CompreFace server"
        if (faceRecogServicesAvailable) {
            model["faceRecogAvailableStatusIcon"] = "bi-check-circle"
            model["faceRecogAvailableStatusColor"] = "green"
            model["faceRecogAvailableStatusText"] = "Connected to CompreFace server"
        }

        model["msg"] = ""
        model["status"] = ApiResponse.SUCCESS.status
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        model["message"] = dirDneString

        return module
    }

    @Secured("ROLE_ADMIN")
    @CacheEvict(value = ["firstSettingQuery"], allEntries = true)
    @RequestMapping(value = ["/settings"], method = [RequestMethod.POST])
    fun postSettings(
        model: Model, redirectAttributes: RedirectAttributes,
        request: HttpServletRequest,
        @RequestHeader headers: HttpHeaders,
        @RequestParam("mediaDirList") mediaDirList: String,
        @RequestParam("mediaExcludeDirList") mediaExcludeDirList: String,
        @RequestParam("compreFaceServer") compreFaceServer: String,
        @RequestParam("compreFaceKey") compreFaceKey: String,
        @RequestParam("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String,
        @RequestParam("queryLimit") queryLimit: Int,
        @RequestParam("matchScanLimit") matchScanLimit: Int,
        @RequestParam("trainingDataLimit") trainingDataLimit: Int,
        @RequestParam("notificationLimit") notificationLimit: Int,
        @RequestParam("searchHistoryLimit") searchHistoryLimit: Int,
        @RequestParam("changePort") port: String,
        @RequestParam("scanAutomatically") scanAutomatically: String?,
    ): String {
        var resetServer = false
        var mediaDirs: List<String>? = null
        val mediaDirArrayList: ArrayList<MediaDirectory> = ArrayList()
        if (mediaDirList.isNotBlank()) {
            mediaDirs = mediaDirList.trim().split(",").map { it.trim() }
        }
        var mediaExcludeDirs: List<String>? = null
        val mediaExcludeDirArrayList: ArrayList<MediaDirectory> = ArrayList()
        if (mediaExcludeDirList.isNotBlank()) {
            mediaExcludeDirs = mediaExcludeDirList.trim().split(",").map { it.trim() }
        }

        model["alertClass"] = "alert-success"
        var statusMessage = ""

        var dirDneString = ""
        if (!mediaDirs.isNullOrEmpty()) {

            val allMediaDirs = mediaDirRepository?.findByExclude(false)
            val allMediaDirList: List<String>? = allMediaDirs?.map { it?.getDirectory()!! }
            if (scanAutomatically == "on" &&
                (mediaDirs.isNotEmpty() && (!mediaDirs.containsAll(allMediaDirList!!) || !allMediaDirList.containsAll(mediaDirs)))
            ) {
                resetServer = true
            }

            mediaDirRepository?.deleteByExclude(false)
            for (mediaDir in mediaDirs) {
                if (mediaDir.trim().isNotBlank()) {
                    var mediaDirObj = mediaDirRepository?.findByExcludeIsAndDirectory(false, mediaDir)
                    if (mediaDirObj == null) {
                        mediaDirObj = MediaDirectory()
                        mediaDirObj.setDirectory(mediaDir)
                    }
                    mediaDirObj.setExclude(false)
                    mediaDirObj.setCreatedAt(getCurrentTimestamp())
                    mediaDirObj.setModifiedAt(getCurrentTimestamp())
                    mediaDirArrayList.add(mediaDirObj)

                    val path: Path = Paths.get(mediaDir)
                    if (!Files.exists(path)) {
                        dirDneString += "$mediaDir,"
                    }
                }
            }
            mediaDirRepository?.saveAll(mediaDirArrayList)
        } else {
            mediaDirRepository?.deleteByExclude(false)
        }

        if (!mediaExcludeDirs.isNullOrEmpty()) {
            val allMediaExcludeDirs = mediaDirRepository?.findByExclude(true)
            val allMediaExcludeDirList: List<String>? = allMediaExcludeDirs?.map { it?.getDirectory()!! }
            if (scanAutomatically == "on" &&
                (mediaExcludeDirs.isNotEmpty() && (!mediaExcludeDirs.containsAll(allMediaExcludeDirList!!) || !allMediaExcludeDirList.containsAll(mediaExcludeDirs)))
            ) {
                resetServer = true
            }

            mediaDirRepository?.deleteByExclude(true)
            for (mediaDir in mediaExcludeDirs) {
                if (mediaDir.trim().isNotBlank()) {
                    var mediaDirObj = mediaDirRepository?.findByExcludeIsAndDirectory(true, mediaDir)
                    if (mediaDirObj == null) {
                        mediaDirObj = MediaDirectory()
                        mediaDirObj.setDirectory(mediaDir)
                    }
                    mediaDirObj.setExclude(true)
                    mediaDirObj.setCreatedAt(getCurrentTimestamp())
                    mediaDirObj.setModifiedAt(getCurrentTimestamp())
                    mediaExcludeDirArrayList.add(mediaDirObj)

                    val path: Path = Paths.get(mediaDir)
                    if (!Files.exists(path)) {
                        dirDneString += "$mediaDir,"
                    }
                }
            }
            mediaDirRepository?.saveAll(mediaExcludeDirArrayList)
        } else {
            mediaDirRepository?.deleteByExclude(true)
        }

        if (dirDneString.isNotBlank()) {
            statusMessage = "Cannot find "+dirDneString.dropLast(1)
            model["alertClass"] = "alert-warning"
        }

        val settings = settingsRepository?.findFirstByOrderByIdAsc()

        if (compreFaceServer.isNotEmpty()) {
            settings?.setCompreFaceServer(compreFaceServer)
        }
        if (compreFaceKey.isNotEmpty()) {
            settings?.setCompreFaceKey(compreFaceKey)
        }
        if (recognitionConfidenceThreshold.isNotEmpty()) {
            settings?.setRecognitionConfidenceThreshold(recognitionConfidenceThreshold)
        }
        if (queryLimit > 0) {
            settings?.setQueryLimit(queryLimit)
        }
        if (matchScanLimit > 0) {
            settings?.setMatchScanLimit(matchScanLimit)
        }
        if (notificationLimit > 0) {
            settings?.setNotificationLimit(notificationLimit)
        }
        if (trainingDataLimit > 0) {
            settings?.setTrainingDataLimit(trainingDataLimit)
        }
        if (searchHistoryLimit > 0) {
            settings?.setSearchHistoryLimit(searchHistoryLimit)
        }
        if (settings != null && port.isNotEmpty() && port != settings.getPort()) {
            settings.setPort(port)
            resetServer = true
        }
        if (scanAutomatically == "on") {
            settings?.setScanAutomatically(true)
        } else {
            settings?.setScanAutomatically(false)
        }

        if (settings != null) {
            settingsRepository?.save(settings)
            model["settings"] = settings

            var faceRecogServicesAvailable = false
            var compreFaceResponse: ResponseEntity<String>?
            try {
                val webClient = WebClient.create(settings.getCompreFaceServer()!!)
                compreFaceResponse = webClient.get()
                    .uri("api/v1/recognition/subjects/")
                    .header("x-api-key", settings.getCompreFaceKey())
                    .retrieve()
                    .toEntity(String::class.java)
                    .block()
                faceRecogServicesAvailable = compreFaceResponse != null && compreFaceResponse.statusCode.toString().lowercase() == "200 ok"
            } catch (e: Exception) {
                logger.log(Level.INFO, "Error CompreFace connection: " + e.localizedMessage)
            }

            model["faceRecogAvailableStatusIcon"] = "bi-x-circle"
            model["faceRecogAvailableStatusColor"] = "red"
            model["faceRecogAvailableStatusText"] = "Could not connect to CompreFace server"
            if (faceRecogServicesAvailable) {
                model["faceRecogAvailableStatusIcon"] = "bi-check-circle"
                model["faceRecogAvailableStatusColor"] = "green"
                model["faceRecogAvailableStatusText"] = "Connected to CompreFace server"
            }
        }

        if (statusMessage.isBlank() && model.getAttribute("alertClass") == "alert-success") {
            statusMessage = "Settings saved"
        }

        if (resetServer) {
            val baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
            val baseUrlArray = baseUrl.split(":")
            val oldPort = baseUrlArray[baseUrlArray.count()-1]
            val newBaseUrl = baseUrl.replace(":$oldPort",":$port")

            restartService?.restartApp()

            return "redirect:$newBaseUrl/settings"
        }

        val module = "settings"
        model["msg"] = ""
        model["status"] = ApiResponse.SUCCESS.status
        model["message"] = ""
        model["mediaDirList"] = mediaDirList.trim()
        model["mediaExcludeDirList"] = mediaExcludeDirList.trim()
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        model["statusMessage"] = statusMessage

        return module
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/settings/users")
    fun getUsers(model: Model): String {
        val module = "users"
        model["users"] = mutableListOf<User>()

        val sort = Sort.by(
            Sort.Order.asc("username")
        )
        val users = userRepository?.findAll(sort)
        if (users != null) {
            model["users"] = users
        }

        model["msg"] = ""
        model["status"] = ApiResponse.SUCCESS.status
        model["message"] = ""
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @Secured("ROLE_ADMIN")
    @RequestMapping(value = ["/settings/content/delete"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @CacheEvict(value = ["allMetadataByDate", "allMetadataByDateAndType", "allMetadataOnlyByDate", "allMetadataAndAttributesByDate", "singleMetadataRequest", "allAlbumMetadataWithCoordinates", "allMetadataWithCoordinates"], allEntries = true)
    @ResponseBody
    @Transactional
    fun deleteContent(model: Model, @RequestBody requestBody: JsonNode): String? {
        val userDeleteMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (userDeleteMap.containsKey("deleteContent")) {
            val deleteContent = userDeleteMap["deleteContent"].toString().toBoolean()

            if (deleteContent) {
                albumCommentRepository?.deleteAll()
                albumPhotoCommentRepository?.deleteAll()
                albumPhotoRepository?.deleteAll()
                albumRepository?.deleteAll()
                commentRepository?.deleteAll()
                favoriteRepository?.deleteAll()
                metadataRepository?.deleteAll()
                notificationRepository?.deleteAll()
                recognitionLabelPhotoRepository?.deleteAll()
                recognitionLabelRepository?.deleteAll()
                userAlbumRepository?.deleteAll()
                keywordRepository?.deleteAll()
                keywordPhotoRepository?.deleteAll()

                // Cleanup CompreFace subjects
                val settings = model.getAttribute("settings") as Settings
                val faceRecogServicesAvailable = model.getAttribute("faceRecogServicesAvailable").toString().toBoolean()

                if (faceRecogServicesAvailable) {
                    val webClient = WebClient.create(settings.getCompreFaceServer()!!)
                    webClient.delete()
                        .uri("api/v1/recognition/subjects")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                        .header("x-api-key", settings.getCompreFaceKey())
                        .retrieve()
                        .bodyToMono(String::class.java)
                        .block()
                }

                // Clean up thread files
                FileUtils.deleteThreadFiles("shashinscan")
                FileUtils.deleteThreadFiles("facescan_shashinscan")

                val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
                val sidecarDir = rootPath + model.getAttribute("relativeSidecarDir")
                val sidecarDirFile = File(sidecarDir)
                if (sidecarDirFile.exists()) {

                    // Delete it
                    val dirDeleteSuccess = sidecarDirFile.deleteRecursively()

                    if (dirDeleteSuccess) {
                        resp["msg"] = "Success!"
                    } else {
                        resp["msg"] = "Success, but could not delete sidecar files."
                    }
                    resp["status"] = ApiResponse.SUCCESS.status
                    return mapper.writeValueAsString(resp)
                }

                resp["msg"] = "Success!"
                resp["status"] = ApiResponse.SUCCESS.status
                return mapper.writeValueAsString(resp)
            }

            resp["msg"] = "Something went wrong."
            resp["status"] = ApiResponse.FAIL.status
            return mapper.writeValueAsString(resp)
        }

        resp["msg"] = "Something went wrong."
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }

    @Secured("ROLE_ADMIN")
    @RequestMapping(value = ["/settings/user/delete/{userId}"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun deleteUser(model: Model, @RequestBody requestBody: JsonNode, @PathVariable userId: Int): String? {
        val userDeleteMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (userDeleteMap.containsKey("userId") && userDeleteMap.containsKey("delete")) {
            val userIdRequest = userDeleteMap["userId"].toString().toInt()
            val deleteFlag = userDeleteMap["delete"].toString().toBoolean()

            if (deleteFlag && userId == userIdRequest) {
                userRepository?.deleteById(userId)
                userAlbumRepository?.deleteByUserId(userId)
                favoriteRepository?.deleteByUserId(userId)
                commentRepository?.deleteByUserId(userId)
            }

            resp["msg"] = "Success!"
            resp["status"] = ApiResponse.SUCCESS.status
            return mapper.writeValueAsString(resp)
        }

        resp["msg"] = "Could not save"
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }

    @Secured("ROLE_ADMIN")
    @RequestMapping(value = ["/settings/user/changepassword/{userId}"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun resetPasswordUser(model: Model, @RequestBody requestBody: JsonNode, @PathVariable userId: Int): String? {
        val userMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (userMap.containsKey("userId") && userMap.containsKey("password")) {
            val userIdRequest = userMap["userId"].toString().toInt()
            val password = userMap["password"].toString()

            if (password.isNotBlank() && userId == userIdRequest) {
                val userObjOpt = userRepository?.findById(userIdRequest)
                if (userObjOpt != null) {
                    val userObj = userObjOpt.get()
                    userObj.setModifiedAt(getCurrentTimestamp())
                    userObj.setPassword(bcrypt.encode(password))
                    userRepository?.save(userObj)
                    resp["msg"] = "Success!"
                    resp["status"] = ApiResponse.SUCCESS.status
                    return mapper.writeValueAsString(resp)
                }
            }
        }

        resp["msg"] = "Could not save"
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }

    @Secured("ROLE_ADMIN")
    @RequestMapping(value = ["/settings/user/role/{userId}"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun changeUserRole(model: Model, @RequestBody requestBody: JsonNode, @PathVariable userId: Int): String? {
        val userRoleChangeMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (userRoleChangeMap.containsKey("userId") && userRoleChangeMap.containsKey("changeTo")) {
            val userIdRequest = userRoleChangeMap["userId"].toString().toInt()
            val changeRoleTo = userRoleChangeMap["changeTo"].toString()

            if (changeRoleTo == model.getAttribute("userRole")) {
                favoriteRepository?.deleteByUserId(userIdRequest)
            }
            if (userId == userIdRequest) {
                val userObj = userRepository?.findById(userId)?.get()
                if (userObj != null) {
                    userObj.setModifiedAt(getCurrentTimestamp())
                    userObj.setAuthority(changeRoleTo)
                    userRepository?.save(userObj)
                }
            }

            resp["msg"] = "Success!"
            resp["status"] = ApiResponse.SUCCESS.status
            return mapper.writeValueAsString(resp)
        }

        resp["msg"] = "Could not save"
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }

    @Secured("ROLE_ADMIN")
    @RequestMapping(value = ["/settings/user/permission/{userId}"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun changeUserLoginPermission(model: Model, @RequestBody requestBody: JsonNode, @PathVariable userId: Int): String? {
        val userRoleChangeMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (userRoleChangeMap.containsKey("userId") && userRoleChangeMap.containsKey("changeTo")) {
            val userIdRequest = userRoleChangeMap["userId"].toString().toInt()
            val changePermissionTo = userRoleChangeMap["changeTo"].toString().toBoolean()

            if (userId == userIdRequest) {
                val userObj = userRepository?.findById(userId)?.get()
                if (userObj != null) {
                    userObj.setModifiedAt(getCurrentTimestamp())
                    userObj.setIsAuthorized(changePermissionTo)
                    userRepository?.save(userObj)
                }
            }

            resp["msg"] = "Success!"
            resp["status"] = ApiResponse.SUCCESS.status
            return mapper.writeValueAsString(resp)
        }

        resp["msg"] = "Could not save"
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/settings/logs")
    fun getLogs(model: Model, request: HttpServletRequest): String {
        var lineLimit = 1000
        if (request.getParameter("lines") != null && isNumber(request.getParameter("lines"))) {
            lineLimit = request.getParameter("lines").toString().toInt()
        }
        val module = "logs"
        model["msg"] = ""
        model["status"] = ApiResponse.SUCCESS.status
        model["message"] = "No log file present"
        model["logList"] = mutableListOf<String>()
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)

        val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
        val logFilePath = "$rootPath/logs/spring-boot-logger.log"
        val f = File(logFilePath)
        if (f.exists() && !f.isDirectory) {
            model["message"] = ""
            //val content = Files.readString(Paths.get(logFilePath), StandardCharsets.UTF_8)
            val content = readFileAsLinesUsingUseLines(logFilePath)
            model["logList"] = content.takeLast(lineLimit)
        }
        return module
    }

    private fun isNumber(s: String): Boolean {
        return try {
            s.toInt()
            true
        } catch (ex: NumberFormatException) {
            false
        }
    }

    private fun readFileAsLinesUsingUseLines(fileName: String): List<String> {
        val inputStream = File(fileName).inputStream()
        val lineList = mutableListOf<String>()

        inputStream.bufferedReader().forEachLine {
            lineList.add(it.trimEnd())
        }

        return lineList
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/settings/logs/download")
    fun getDownloadLogsLogs(): ResponseEntity<InputStreamResource>? {
        val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
        val logFilePath = "$rootPath/logs/spring-boot-logger.log"
        val f = File(logFilePath)
        if (f.exists() && !f.isDirectory) {
            val headers = HttpHeaders()
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename="+appName+"_"+java.time.Clock.systemUTC().instant()+".log")
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate")
            headers.add("Pragma", "no-cache")
            headers.add("Expires", "0")

            val resource = InputStreamResource(FileInputStream(f))

            return ResponseEntity.ok()
                .headers(headers)
                .contentLength(f.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource)
        }
        return null
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/settings/match")
    fun getMatchScan(model: Model): String {
        val module = "match"
        model["msg"] = ""
        model["status"] = ApiResponse.SUCCESS.status
        model["message"] = "Click scan to start finding people"
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/settings/scan")
    fun getScan(model: Model): String {
        val module = "scan"
        model["msg"] = ""
        model["status"] = ApiResponse.SUCCESS.status
        model["message"] = "Click scan to scan photo directories"
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @Secured("ROLE_ADMIN")
    @CacheEvict(value = ["allMetadataByDate", "allMetadataByDateAndType", "allMetadataOnlyByDate", "allMetadataAndAttributesByDate", "singleMetadataRequest", "allAlbumMetadataWithCoordinates", "allMetadataWithCoordinates"], allEntries = true)
    @RequestMapping(value = ["/settings/scan"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun postScan(
        model: Model,
        @RequestParam submit: String,
        @RequestParam deleteThread: Boolean,
        @RequestParam cancelScan: Boolean,
        @RequestParam reindexFiles: Boolean
    ): String {
        resp["msg"] = "Nothing to see here"

        alreadyScannedFilepaths.clear()

        if (deleteThread) {
            deleteThreadScan()
            resp["msg"] = "Thread file manually deleted"
        }

        if (cancelScan) {
            shouldStop.set(true)
//            deleteThreadScan()
//            resp["msg"] = "Scan Cancelled"
//            logger.log(Level.INFO, "Scan Cancelled, throwing exception")
//            // Kill it with fire since it's a recursive process
//            throw Exception("Cancelling media scan and killing it with fire!")
        }

        if (submit == "Scan") {
            resp["msg"] = scanMediaDirectories(reindexFiles)
        }

        return mapper.writeValueAsString(resp)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/settings/snapshot")
    fun getSnapshot(model: Model): String {
        val module = "snapshot"
        model["msg"] = ""
        model["status"] = ApiResponse.SUCCESS.status
        model["message"] = "Export or import metadata zip file and backup database. People and keywords will not be exported."
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/settings/snapshot/export")
    fun postExportSnapshot(model: Model, @RequestParam snapshot: String, response: HttpServletResponse): ResponseEntity<InputStreamResource>? {
        val user = model.getAttribute("currentUser") as User?
        val userId = user?.getId()

        if (snapshot == "export" && userId != null && userId > 0) {
            val tempExportBaseDir = Files.createTempDirectory("shashin")

            // Rebuild metadata
            val metadataList = metadataRepository?.findAll()
            if (metadataList != null && metadataList.count() > 0) {
                val tempMetadataExportDir = Files.createDirectories(Paths.get(tempExportBaseDir.pathString+"/metadata"))
                for (metadata in metadataList) {
                    if (metadata != null) {
                        val tempFile = File(tempMetadataExportDir.toAbsolutePath().toString() + "/" + metadata.getId() + ".yaml")
                        if (tempFile.createNewFile()) {
                            val yamlFactory: YAMLFactory = YAMLFactory.builder()
                                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                                .disable(YAMLGenerator.Feature.SPLIT_LINES)
                                .build()
                            val om = ObjectMapper(yamlFactory)
                            om.writeValue(tempFile, metadata)
                        } else {
                            logger.log(
                                Level.INFO,
                                "Exporting metadata. File already exists: " + tempFile.absolutePath
                            )
                        }
                    }
                }
            }

            // Rebuild Favorites
            val favoriteList = favoriteRepository?.findAll()
            if (favoriteList != null && favoriteList.count() > 0) {
                val tempMetadataExportDir = Files.createDirectories(Paths.get(tempExportBaseDir.pathString+"/favorite"))
                for (favorite in favoriteList) {
                    if (favorite != null) {
                        favorite.setUserId(userId)
                        val tempFile = File(tempMetadataExportDir.toAbsolutePath().toString() + "/" + favorite.getMetadataId() + "_" + userId + ".yaml")
                        if (tempFile.createNewFile()) {
                            val yamlFactory: YAMLFactory = YAMLFactory.builder()
                                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                                .disable(YAMLGenerator.Feature.SPLIT_LINES)
                                .build()
                            val om = ObjectMapper(yamlFactory)
                            om.writeValue(tempFile, favorite)
                        } else {
                            logger.log(
                                Level.INFO,
                                "Exporting metadata. File already exists: " + tempFile.absolutePath
                            )
                        }
                    }
                }
            }

            // Rebuild albums
            val albumList = albumRepository?.findAll()
            if (albumList != null && albumList.count() > 0) {
                val tempMetadataExportDir = Files.createDirectories(Paths.get(tempExportBaseDir.pathString+"/album"))
                for (album in albumList) {
                    if (album != null) {
                        val tempFile = File(tempMetadataExportDir.toAbsolutePath().toString() + "/" + album.getId() + ".yaml")
                        if (tempFile.createNewFile()) {
                            val yamlFactory: YAMLFactory = YAMLFactory.builder()
                                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                                .disable(YAMLGenerator.Feature.SPLIT_LINES)
                                .build()
                            val om = ObjectMapper(yamlFactory)
                            om.writeValue(tempFile, album)
                        } else {
                            logger.log(
                                Level.INFO,
                                "Exporting album. File already exists: " + tempFile.absolutePath
                            )
                        }
                    }
                }
            }

            // Rebuild albumphoto
            val albumPhotoList = albumPhotoRepository?.findAll()
            if (albumPhotoList != null && albumPhotoList.count() > 0) {
                val tempMetadataExportDir = Files.createDirectories(Paths.get(tempExportBaseDir.pathString+"/albumphoto"))
                for (albumPhoto in albumPhotoList) {
                    if (albumPhoto != null) {
                        val tempFile = File(tempMetadataExportDir.toAbsolutePath().toString() + "/" + albumPhoto.getMetadataId() + "_" + albumPhoto.getAlbumId() + ".yaml")
                        if (tempFile.createNewFile()) {
                            val yamlFactory: YAMLFactory = YAMLFactory.builder()
                                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                                .disable(YAMLGenerator.Feature.SPLIT_LINES)
                                .build()
                            val om = ObjectMapper(yamlFactory)
                            om.writeValue(tempFile, albumPhoto)
                        } else {
                            logger.log(
                                Level.INFO,
                                "Exporting albumPhoto. File already exists: " + tempFile.absolutePath
                            )
                        }
                    }
                }
            }

            // Backup the database
            val dbBackupName = DatabaseUtil.backup(dataSourceUrl)
            if (dbBackupName == "") {
                logger.log(
                    Level.WARNING,
                    "Could not backup database"
                )
            }

            if (tempExportBaseDir.isDirectory() && tempExportBaseDir.toList().isNotEmpty()) {
                val tempDir = tempExportBaseDir.toFile()
                val outputZipFile = FileUtils.zipFolder(tempDir,appName?.lowercase()+"_metadata_export")
                FileUtils.deleteDirectory(tempDir)

                if (outputZipFile != null) {
                    outputZipFile.deleteOnExit()

                    val resource = InputStreamResource(FileInputStream(outputZipFile))
                    val contentLength = outputZipFile.length()

                    val headers = HttpHeaders()
                    headers.add(HttpHeaders.SET_COOKIE, "ShashinSnapshotName=" + outputZipFile.name)
                    headers.add(HttpHeaders.SET_COOKIE, "ShashinSnapshotSize=" + contentLength)
                    headers.add(HttpHeaders.SET_COOKIE, "ShashinDbBackupName=" + dbBackupName)
                    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + outputZipFile.name)
                    headers.add("Cache-Control", "no-cache, no-store, must-revalidate")
                    headers.add("Pragma", "no-cache")
                    headers.add("Expires", "0")

                    return ResponseEntity.ok()
                        .headers(headers)
                        .contentLength(contentLength)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource)
                }
            }
        }

        return null
    }

    @Secured("ROLE_ADMIN")
    @RequestMapping(value = ["/settings/directorytree"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun getDirectoryTree(model: Model, @RequestBody requestBody: JsonNode): String? {
        val metadataMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        val dirs = mutableMapOf<String, Any>()
        dirs["status"] = ""
        dirs["msg"] = ""
        dirs["dirs"] = mutableMapOf<String, MutableList<String>>()
        val dirMap = mutableMapOf<String, MutableList<String>>()
        val os = System.getProperty("os.name")
        dirs["os"] = os

        if (metadataMap.containsKey("path")) {
            val path = metadataMap["path"].toString()

            if (path.isNotEmpty()) {
                val filePath = File(path)

                val directories = filePath.listFiles { file -> file.isDirectory && !file.isHidden }

                if (directories != null) {
                    val dirNames = mutableListOf<String>()
                    for (i in directories.indices) {
                        dirNames.add(directories[i].name)
                    }

                    dirMap[path] = dirNames
                }
            } else {
                val directories = File.listRoots()
                val dirNames = mutableListOf<String>()
                for (i in directories.indices) {
                    dirNames.add(directories[i].toString())
                }

                dirMap["roots"] = dirNames
            }

            dirs["dirs"] = dirMap
        }

        return mapper.writeValueAsString(dirs)
    }

    @Secured("ROLE_ADMIN")
    @CacheEvict(value = ["allMetadataByDate", "allMetadataByDateAndType", "allMetadataOnlyByDate", "allMetadataAndAttributesByDate", "singleMetadataRequest", "allAlbumMetadataWithCoordinates", "allMetadataWithCoordinates"], allEntries = true)
    @PostMapping("/settings/snapshot")
    @Transactional
    fun postImportSnapshot(model: Model, @RequestParam snapshot: String, @RequestParam snapshotFile: MultipartFile): String {
        val module = "snapshot"

        model["message"] = "Invalid file"
        val user = model.getAttribute("currentUser") as User?
        val userId = user?.getId()

        if (snapshot == "import" && !snapshotFile.isEmpty && userId != null && userId > 0) {

            model["message"] = "Completed import."

            val tempFile = File.createTempFile("upload", null)
            snapshotFile.transferTo(tempFile)
            val zipFile = ZipFile(tempFile)

            val mapper = ObjectMapper(YAMLFactory())
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            val entries: Enumeration<out ZipEntry> = zipFile.entries()

            val albumPhotoList = mutableListOf<AlbumPhoto>()
            val albumList = mutableListOf<Album>()
            val favoriteList = mutableListOf<Favorite>()

            // Get all entries in zip file
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val stream: InputStream = zipFile.getInputStream(entry)
                val inputAsString = stream.bufferedReader().use { it.readText() }
                val tempEntry = entry.name.replace("\\", "/")

                logger.log(
                    Level.INFO,
                    "Importing Entry: " + entry.name + "."
                )

                if (tempEntry.startsWith("metadata/") || entry.name.startsWith("metadata\\")) {
                    val importedMetadata = mapper.readValue(inputAsString, Metadata::class.java)
                    if (importedMetadata != null) {
                        val foundMetadataRecord = metadataRepository?.findById(importedMetadata.getId())

                        if (foundMetadataRecord != null && foundMetadataRecord.isPresent && !foundMetadataRecord.isEmpty) {
                            val foundMetadata = foundMetadataRecord.get()
                            var message = "not imported"
                            val saved = saveImportedMetadata(importedMetadata, foundMetadata)
                            if (saved) {
                                message = "imported"
                            }

                            logger.log(
                                Level.INFO,
                                "Metadata: " + importedMetadata.getId() + " pointing to " + importedMetadata.getPath() + " " + message + "."
                            )
                        }
                    }
                }

                if (tempEntry.startsWith("albumphoto/") || entry.name.startsWith("albumphoto\\")) {
                    val importedAlbumPhoto = mapper.readValue(inputAsString, AlbumPhoto::class.java)
                    albumPhotoList.add(importedAlbumPhoto)
                }

                if (tempEntry.startsWith("album/") || entry.name.startsWith("album\\")) {
                    val importedAlbum = mapper.readValue(inputAsString, Album::class.java)
                    albumList.add(importedAlbum)
                }

                if (tempEntry.startsWith("favorite/") || entry.name.startsWith("favorite\\")) {
                    val importedFavorite = mapper.readValue(inputAsString, Favorite::class.java)
                    if (importedFavorite != null && importedFavorite.getUserId() == userId) {
                        val importedFavoriteRecord = favoriteRepository?.findByMetadataIdAndUserId(importedFavorite.getMetadataId(), userId)

                        if (importedFavoriteRecord == null) {
                            val favoriteObj = Favorite()
                            favoriteObj.setUserId(importedFavorite.getUserId())
                            favoriteObj.setMetadataId(importedFavorite.getMetadataId())
                            favoriteObj.setCreatedAt(getCurrentTimestamp())
                            favoriteObj.setModifiedAt(getCurrentTimestamp())
                            favoriteList.add(favoriteObj)

                            logger.log(
                                Level.INFO,
                                "Favorite: " + importedFavorite.getMetadataId() + " imported."
                            )
                        } else {
                            logger.log(
                                Level.INFO,
                                "Favorite: " + importedFavorite.getMetadataId() + " not imported."
                            )
                        }
                    }
                }
            }

            saveImportedFavorites(favoriteList)

            val albumPhotoListInsert = mutableListOf<AlbumPhoto>()

            if (albumList.isNotEmpty() && albumPhotoList.isNotEmpty()) {
                for (albumPhoto in albumPhotoList) {
                    for (importedAlbum in albumList) {
                        if (albumPhoto.getAlbumId() == importedAlbum.getId()) {
                            var hasAlbumCover = true
                            val foundAlbumRecord = albumRepository?.findAlbumByNameIgnoreCase(importedAlbum.getName()!!)
                            var albumId: Int?

                            if (foundAlbumRecord == null) {
                                // Insert record
                                val albumObj = Album()
                                albumObj.setName(importedAlbum.getName())
                                albumObj.setCreatedAt(getCurrentTimestamp())
                                albumObj.setModifiedAt(getCurrentTimestamp())
                                albumRepository?.save(albumObj)
                                albumId = albumObj.getId()
                                hasAlbumCover = false

                                logger.log(
                                    Level.INFO,
                                    "Album: " + importedAlbum.getName()!! + " created."
                                )
                            } else {
                                albumId = foundAlbumRecord.getId()

                                logger.log(
                                    Level.INFO,
                                    "Album: " + importedAlbum.getName()!! + " already exists."
                                )
                            }

                            val userAlbumCount = userAlbumRepository?.countByUserIdAndAlbumId(userId, albumId)
                            if (userAlbumCount == 0) {
                                val userAlbumObj = UserAlbum()
                                userAlbumObj.setAlbumId(albumId)
                                userAlbumObj.setUserId(userId)
                                userAlbumObj.setModifiedAt(getCurrentTimestamp())
                                userAlbumObj.setCreatedAt(getCurrentTimestamp())
                                userAlbumRepository?.save(userAlbumObj)
                            }

                            val metadataObj = metadataRepository?.findById(albumPhoto.getMetadataId()!!)
                            if (metadataObj != null && metadataObj.isPresent) {
                                albumPhotoRepository?.deleteByMetadataId(metadataObj.get().getId())
                                hasAlbumCover = false
                            }

                            if (!hasAlbumCover) {
                                val albumObj = albumRepository?.findById(albumId)
                                if (albumObj != null && metadataObj != null && metadataObj.isPresent && !metadataObj.get()
                                        .getHidden()!!
                                ) {
                                    albumObj.get().setCoverUrl(metadataObj.get().getThumbnailUrlCentered())
                                    albumRepository?.save(albumObj.get())
                                }
                            }

                            if (metadataObj != null && metadataObj.isPresent && !metadataObj.get()
                                    .getHidden()!!
                            ) {
                                val albumPhotoCount = albumPhotoRepository?.countByMetadataIdAndAlbumId(
                                    albumPhoto.getMetadataId()!!,
                                    albumId
                                )
                                if (albumPhotoCount == 0) {
                                    val albumPhotoObj = AlbumPhoto()
                                    albumPhotoObj.setAlbumId(albumId)
                                    albumPhotoObj.setMetadataId(albumPhoto.getMetadataId())
                                    albumPhotoObj.setCreatedAt(getCurrentTimestamp())
                                    albumPhotoObj.setModifiedAt(getCurrentTimestamp())
//                                    albumPhotoRepository?.save(albumPhotoObj)
                                    albumPhotoListInsert.add(albumPhotoObj)
                                }
                            }
                        }
                    }
                }
            }

            if (albumPhotoListInsert.size > 0) {
                albumPhotoRepository?.saveAll(albumPhotoListInsert)
            }

            val albumPhotoCounts = albumRepository?.countNumberOfPhotosInAlbums()
            if (albumPhotoCounts != null) {
                for (albumPhotoCount in albumPhotoCounts) {
                    if (albumPhotoCount != null && albumPhotoCount.getPhotoCount() == 0) {
                        // Delete the album
                        albumRepository?.deleteById(albumPhotoCount.getAlbumId()!!)
                        userAlbumRepository?.deleteByAlbumId(albumPhotoCount.getAlbumId()!!)
                    }
                }
            }

            tempFile.delete();
        }

        model["msg"] = ""
        model["status"] = ApiResponse.SUCCESS.status
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @Transactional
    fun saveImportedFavorites(favoriteList: MutableList<Favorite>) {
        if (favoriteList.isNotEmpty()) {
            favoriteRepository?.saveAll(favoriteList)
        }
    }

    @Transactional
    fun saveImportedMetadata(importedMetadata: Metadata?, foundMetadata: Metadata?): Boolean {
        if (importedMetadata != null && foundMetadata != null &&
            (importedMetadata.getTitle() != foundMetadata.getTitle() ||
            importedMetadata.getCamera() != foundMetadata.getCamera() ||
            importedMetadata.getDescription() != foundMetadata.getDescription() ||
            importedMetadata.getYear() != foundMetadata.getYear() ||
            importedMetadata.getMonth() != foundMetadata.getMonth() ||
            importedMetadata.getDay() != foundMetadata.getDay() ||
            importedMetadata.getTime() != foundMetadata.getTime() ||
            importedMetadata.getTimeZone() != foundMetadata.getTimeZone() ||
            importedMetadata.getLat() != foundMetadata.getLat() ||
            importedMetadata.getLng() != foundMetadata.getLng() ||
            (importedMetadata.getHidden() != foundMetadata.getHidden() && importedMetadata.getHidden() != null && foundMetadata.getHidden() != null))
        ) {
            val metadataRepo = metadataRepository?.findById(importedMetadata.getId())
            if (metadataRepo != null && !metadataRepo.isEmpty) {
                val metadata = metadataRepo.get()
                metadata.setTitle(importedMetadata.getTitle())
                metadata.setCamera(importedMetadata.getCamera())
                metadata.setDescription(importedMetadata.getDescription())
                metadata.setYear(importedMetadata.getYear())
                metadata.setMonth(importedMetadata.getMonth())
                metadata.setDay(importedMetadata.getDay())
                metadata.setTime(importedMetadata.getTime())
                metadata.setTimeZone(importedMetadata.getTimeZone())
                metadata.setCamera(importedMetadata.getCamera())
                metadata.setLat(importedMetadata.getLat())
                metadata.setLng(importedMetadata.getLng())
                metadata.setModifiedAt(getCurrentTimestamp())
                if (importedMetadata.getHidden() != null && foundMetadata.getHidden() != null) {
                    metadata.setHidden(importedMetadata.getHidden())
                }
                metadataRepository?.save(metadata)
                return true
            } else {
                return false
            }
        }

        return false
    }

    @CacheEvict(value = ["allMetadataByDate", "allMetadataByDateAndType", "allMetadataOnlyByDate", "allMetadataAndAttributesByDate", "singleMetadataRequest", "allAlbumMetadataWithCoordinates", "allMetadataWithCoordinates"], allEntries = true)
    fun scanMediaDirectories(reindexFiles: Boolean): String {
        scanCount = 0
        val admins = userRepository?.findAllByAuthorityEquals(adminRole!!)
        val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
        val sidecarDir = rootPath + relativeSidecarDir
        var threadFileContent = FileUtils.readThreadFile("shashinscan")
//        var msg: String

        if ((shouldStop.get() && (!FileUtils.checkThreadFileAlive("shashinscan") || (threadFileContent != null && threadFileContent == "Scan Cancelled") || (threadFileContent != null && threadFileContent == "Scan Complete"))) || (!shouldStop.get() && !FileUtils.checkThreadFileAlive("shashinscan"))) {
            shouldStop.set(false)
            if (threadFileContent != null) {
                deleteThreadScan()
            }
        } else {
            return "Scan cancellation in progress, please wait"
        }

        val mediaDirs = mediaDirRepository?.findByExclude(false)
        val mediaExcludeDirs = mediaDirRepository?.findByExclude(true)

        if (mediaDirs != null && mediaDirs.count() > 0) {
            var mediaDirNotFound = false
            for (mediaDir in mediaDirs) {
                val dir = Paths.get(mediaDir?.getDirectory()!!)
                if (!Files.exists(dir)) {
                    mediaDirNotFound = true
                    break
                }
            }

            if (!mediaDirNotFound) {

                if (!FileUtils.checkThreadFileAlive("shashinscan")) {
                    // Clean up any existing thread files
                    deleteThreadScan()

                    // Iterate through directory in another thread
                    Thread {
                        //Create file with thread name and write file name iterated
                        val tempDir = System.getProperty("java.io.tmpdir")
                        val threadFile = FileUtils.createFile(
                            tempDir,
                            tempDir + "/" + Thread.currentThread().name + ".shashinscan",
                            "Thread"
                        )
                        if (threadFile != null) {
                            // Check for deleted original files
                            val metadataList = metadataRepository?.findAll()

                            if (metadataList != null) {
                                var deleteCount = 0
                                val settings = settingsRepository?.findFirstByOrderByIdAsc()

                                for (metadata in metadataList) {
                                    if (shouldStop.get()) {
                                        writeToThreadFile("Scan Cancelled", threadFile)
                                        break
                                    }
                                    if (metadata != null) {
                                        if (!metadata.getPath().isNullOrBlank()) {
                                            writeToThreadFile(
                                                "Checking for changes for file: " + metadata.getPath(),
                                                threadFile
                                            )

                                            // Check if metadata path still exists, if not, delete media
                                            var basePathExists = false
                                            var excludeBasePathExists = false

                                            for (mediaDir in mediaDirs) {
                                                basePathExists = metadata.getPath()!!
                                                    .startsWith(mediaDir!!.getDirectory().toString())

                                                if (basePathExists) {
                                                    break
                                                }
                                            }

                                            if (mediaExcludeDirs != null) {
                                                for (mediaExcludeDir in mediaExcludeDirs) {
                                                    excludeBasePathExists = metadata.getPath()!!
                                                        .startsWith(mediaExcludeDir?.getDirectory().toString())

                                                    if (excludeBasePathExists) {
                                                        break
                                                    }
                                                }
                                            }

                                            val checkFile = File(metadata.getPath()!!)
                                            if (!checkFile.exists() || !basePathExists || excludeBasePathExists) {
                                                deleteCount++

                                                // Delete side car and metadata files
                                                if (!metadata.getThumbnailPathCentered().isNullOrBlank()) {
                                                    val fileObj = File(metadata.getThumbnailPathCentered()!!)
                                                    if (fileObj.delete()) {
                                                        logger.log(
                                                            Level.INFO,
                                                            "Deleted thumbnail centered file: " + fileObj.name
                                                        )
                                                    } else {
                                                        logger.log(
                                                            Level.WARNING,
                                                            "Failed to delete thumbnail centered file: " + fileObj.name
                                                        )
                                                    }
                                                }
                                                if (!metadata.getThumbnailPathSmall().isNullOrBlank()) {
                                                    val fileObj = File(metadata.getThumbnailPathSmall()!!)
                                                    if (fileObj.delete()) {
                                                        logger.log(
                                                            Level.INFO,
                                                            "Deleted thumbnail small file: " + fileObj.name
                                                        )
                                                    } else {
                                                        logger.log(
                                                            Level.WARNING,
                                                            "Failed to delete thumbnail small file: " + fileObj.name
                                                        )
                                                    }
                                                }
                                                if (!metadata.getMapMarkerPath().isNullOrBlank()) {
                                                    val fileObj = File(metadata.getMapMarkerPath()!!)
                                                    if (fileObj.delete()) {
                                                        logger.log(
                                                            Level.INFO,
                                                            "Deleted map marker file: " + fileObj.name
                                                        )
                                                    } else {
                                                        logger.log(
                                                            Level.WARNING,
                                                            "Failed to delete map marker file: " + fileObj.name
                                                        )
                                                    }
                                                }

                                                val metadataDir = sidecarDir + "metadata/"
                                                val thumbnailDir = sidecarDir.replace('\\', '/') + "thumbnails"
                                                var relativePath: String =
                                                    metadata.getThumbnailPathCentered()!!.replace('\\', '/').lowercase()
                                                        .replace(thumbnailDir.lowercase(), "")
                                                relativePath = relativePath.replace("_centered.jpg", "")

                                                val metadataExifFile = "$metadataDir$relativePath.exif.yaml"
                                                val fileObj = File(metadataExifFile)
                                                if (fileObj.delete()) {
                                                    logger.log(Level.INFO, "Deleted EXIF file: " + fileObj.name)
                                                } else {
                                                    logger.log(
                                                        Level.WARNING,
                                                        "Failed to delete EXIF file: " + fileObj.name
                                                    )
                                                }

                                                // Delete comments
                                                logger.log(
                                                    Level.FINE,
                                                    "File " + metadata.getPath() + " no longer exists. Deleting metadata: " + metadata.getId()
                                                )
                                                val albumPhotoCommentList =
                                                    albumPhotoCommentRepository?.findByMetadataId(metadata.getId())
                                                if (albumPhotoCommentList != null) {
                                                    for (albumPhotoComment in albumPhotoCommentList) {
                                                        if (albumPhotoComment != null) {
                                                            val commentCount =
                                                                commentRepository?.countById(albumPhotoComment.getId())
                                                            if (commentCount != null && commentCount > 0) {
                                                                commentRepository?.deleteById(albumPhotoComment.getId())
                                                            }
                                                        }
                                                    }
                                                }
                                                albumPhotoCommentRepository?.deleteByMetadataId(metadata.getId())
                                                logger.log(
                                                    Level.INFO,
                                                    "Removed comment records for: " + metadata.getId()
                                                )

                                                // Delete from favorites
                                                favoriteRepository?.deleteByMetadataId(metadata.getId())
                                                logger.log(
                                                    Level.INFO,
                                                    "Removed favorite records for: " + metadata.getId()
                                                )

                                                // Delete from keywords
                                                keywordPhotoRepository?.deleteAllByMetadataId(metadata.getId())
                                                val keywords = keywordRepository?.findAll()
                                                if (keywords != null) {
                                                    for (keywordObj in keywords) {
                                                        val keywordCount =
                                                            keywordPhotoRepository?.countByKeywordId(keywordObj!!.getId())
                                                        if (keywordCount != null && keywordCount == 0) {
                                                            keywordRepository?.deleteById(keywordObj!!.getId())
                                                        }
                                                    }
                                                }
                                                logger.log(
                                                    Level.INFO,
                                                    "Removed keywords records for: " + metadata.getId()
                                                )

                                                // Delete from album
                                                albumPhotoRepository?.deleteByMetadataId(metadata.getId())
                                                val albumPhotoCounts = albumRepository?.countNumberOfPhotosInAlbums()
                                                if (albumPhotoCounts != null) {
                                                    for (albumPhotoCount in albumPhotoCounts) {
                                                        if (albumPhotoCount != null) {
                                                            if (albumPhotoCount.getPhotoCount() == 0) {
                                                                // Delete the album
                                                                albumRepository?.deleteById(albumPhotoCount.getAlbumId()!!)
                                                                userAlbumRepository?.deleteByAlbumId(albumPhotoCount.getAlbumId()!!)
                                                            } else {
                                                                val firstAlbumPhoto =
                                                                    albumPhotoRepository?.findFirstByAlbumId(
                                                                        albumPhotoCount.getAlbumId()!!
                                                                    )
                                                                val metadataObj =
                                                                    metadataRepository?.findById(firstAlbumPhoto?.getMetadataId()!!)
                                                                val albumObj =
                                                                    albumRepository?.findById(albumPhotoCount.getAlbumId()!!)
                                                                if (metadataObj != null && metadataObj.isPresent && albumObj != null && albumObj.isPresent) {
                                                                    albumObj.get().setCoverUrl(
                                                                        metadataObj.get().getThumbnailUrlCentered()
                                                                    )
                                                                    albumRepository?.save(albumObj.get())
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                logger.log(Level.INFO, "Removed album records for: " + metadata.getId())

                                                // Delete tagged people
                                                val recognitionLabelPhotos =
                                                    recognitionLabelPhotoRepository?.findByMetadataId(metadata.getId())
                                                if (settings != null) {
                                                    if (FileUtils.checkCompreFaceConnection(
                                                            settings.getCompreFaceServer(),
                                                            settings.getCompreFaceKey()
                                                        )
                                                    ) {
                                                        val webClient =
                                                            WebClient.create(settings.getCompreFaceServer()!!)
                                                        if (recognitionLabelPhotos != null) {
                                                            for (recognitionLabelPhoto in recognitionLabelPhotos) {
                                                                if (recognitionLabelPhoto.getCompreFaceImageId() != null && recognitionLabelPhoto.getCompreFaceImageId()!!
                                                                        .isNotBlank()
                                                                ) {
                                                                    try {
                                                                        webClient.delete()
                                                                            .uri("api/v1/recognition/faces/${recognitionLabelPhoto.getCompreFaceImageId()}")
                                                                            .header(
                                                                                "x-api-key",
                                                                                settings.getCompreFaceKey()
                                                                            )
                                                                            .retrieve()
                                                                            .bodyToMono(String::class.java)
                                                                            .block()
                                                                    } catch (e: Exception) {
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                recognitionLabelPhotoRepository?.deleteByMetadataId(metadata.getId())

                                                // Delete metadata
                                                metadataRepository?.deleteById(metadata.getId())
                                                logger.log(
                                                    Level.INFO,
                                                    "Removed metadata records for: " + metadata.getId()
                                                )
                                            } else if (!reindexFiles) {
                                                alreadyScannedFilepaths.add(checkFile.path)
                                            }
                                        }
                                    }
                                }

                                if (deleteCount > 0) {
                                    // Set notification for scanCount and date and link to /recent
                                    val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                                    val msg =
                                        "Deleted $deleteCount images/videos at " + sdtf.format(Date()) + "."
                                    if (admins != null) {
                                        val notificationObjList = mutableListOf<Notification>()
                                        for (admin in admins) {
                                            val notificationObj = Notification()
                                            notificationObj.setUserId(admin.getId())
                                            notificationObj.setCreatedAt(getCurrentTimestamp())
                                            notificationObj.setModifiedAt(getCurrentTimestamp())
                                            notificationObj.setRead(false)
                                            notificationObj.setMessage(msg)
                                            notificationObjList.add(notificationObj)
                                        }
                                        if (notificationObjList.isNotEmpty()) {
                                            notificationRepository?.saveAll(notificationObjList)
                                        }
                                    }
                                }
                            }

                            // Scan for new files
                            if (!shouldStop.get()) {
                                val settings = settingsRepository?.findFirstByOrderByIdAsc()
                                var webClient: WebClient? = null
                                if (settings != null && FileUtils.checkCompreFaceConnection(
                                        settings.getCompreFaceServer(),
                                        settings.getCompreFaceKey()
                                    )
                                ) {
                                    webClient = WebClient.create(settings.getCompreFaceServer()!!)
                                }

                                val recognitionLabelPhotoLabels =
                                    recognitionLabelPhotoRepository?.findGroupByRecognitionLabelId()

                                for (mediaDir in mediaDirs) {
                                    if (mediaDir != null) {
                                        getFile(
                                            mediaDir.getDirectory().toString(),
                                            threadFile,
                                            sidecarDir,
                                            mediaDir.getDirectory().toString(),
                                            mediaExcludeDirs,
                                            settings,
                                            webClient,
                                            recognitionLabelPhotoLabels
                                        )
                                    }
                                }

                                FileUtils.deleteEmptyDirectoriesOfFolder(File(sidecarDir))
                            }

                            if (shouldStop.get()) {
                                logger.log(Level.INFO, "Scan Cancelled")
                            } else {
                                logger.log(Level.INFO, "Scan Complete")
                            }

                            // Delete thread file
                            if (threadFile.delete()) {
                                if (scanCount > 0) {
                                    // Set notification for scanCount and date and link to /recent
                                    val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                                    val msg =
                                        "Scan complete for <a href='/recent' target='_blank'>$scanCount images/videos</a> at " + sdtf.format(
                                            Date()
                                        ) + "."
                                    if (admins != null) {
                                        val notificationObjList = mutableListOf<Notification>()
                                        for (admin in admins) {
                                            val notificationObj = Notification()
                                            notificationObj.setUserId(admin.getId())
                                            notificationObj.setCreatedAt(getCurrentTimestamp())
                                            notificationObj.setModifiedAt(getCurrentTimestamp())
                                            notificationObj.setRead(false)
                                            notificationObj.setMessage(msg)
                                            notificationObjList.add(notificationObj)
                                        }
                                        if (notificationObjList.isNotEmpty()) {
                                            notificationRepository?.saveAll(notificationObjList)
                                        }
                                    }
                                }

                                logger.log(Level.FINE, "Thread file deleted: " + threadFile.name)
                            } else {
                                logger.log(Level.SEVERE, "Could not delete thread file: " + threadFile.name)
                            }
                        }
                    }.start()

                    return "Start Scan"
                }

                threadFileContent = FileUtils.readThreadFile("shashinscan")
                var lmsg = "Scan in progress"
                if (shouldStop.get()) {
                    lmsg = "Scan cancellation in progress"
                }

                return if (threadFileContent != null) {
                    lmsg + ": " + threadFileContent.replace("\\", "/")
                } else {
                    lmsg
                }
            } else {
                logger.log(
                    Level.INFO,
                    "Directory not found"
                )
                return "Directory not found"
            }
        } else {
            logger.log(
                Level.INFO,
                "No directories configured"
            )
            return "No directories configured"
        }
//        msg = "Start Scan"
//
//        return msg
    }

    private fun deleteThreadScan() {
        FileUtils.deleteThreadFiles("shashinscan")
        logger.log(Level.INFO, "shashinscan thread file deleted")
    }

    private fun writeToThreadFile(threadText: String, threadFile: File) {
        try {
            val writer = BufferedWriter(FileWriter(threadFile))
            writer.write(threadText)
            writer.close()
        } catch(e: Exception) {
            logger.log(Level.WARNING, "Could not write to thread file: " + threadFile.name)
        }
    }

    private fun getFile(dirPath: String, threadFile: File, sidecarDir: String, rootDir: String, mediaExcludeDirs: MutableIterable<MediaDirectory?>?, settings: Settings?, webClient: WebClient?, recognitionLabelPhotoLabels:  MutableIterable<RecognitionLabelId>?) {
        val f = File(dirPath)
        val files = f.listFiles()

        if (files != null) {
            for (i in files.indices) {

                val file: File = files[i]
                var threadText = file.path + " ALREADY SCANNED"

                if (shouldStop.get()) {
                    threadText = "Scan Cancelled"
                    writeToThreadFile(threadText, threadFile)
                    break
                }

                var exclude = false
                if (mediaExcludeDirs != null) {
                    for (mediaExcludeDir in mediaExcludeDirs) {
                        // Don't scan if path starts with exclude path
                        if (file.path.startsWith(mediaExcludeDir?.getDirectory().toString())) {
                            exclude = true
                            break
                        }
                    }
                }

                if (file.isFile && !alreadyScannedFilepaths.contains(file.path)) {
                    if (!exclude && FileUtils.allowableMediaFiles().contains(file.extension.lowercase())) {

                        //val mediaProcessingUtils = MediaProcessing(apiVersion,geocodeUrl)
                        var metadataObj: Metadata? = Metadata()

                        if (!shouldStop.get() && FileUtils.allowableMediaFiles().contains(file.extension.lowercase())) {
                            val metadataProcessing = MetadataProcessing(apiVersion!!, file, sidecarDir, metadataObj!!, geocodeUrl!!)
                            metadataObj = metadataProcessing.populateMetadata()
                            if (metadataObj.getId().isNotEmpty()) {
                                // Check for entry
                                val metadataCount = metadataRepository?.countMetadataById(metadataObj.getId())

                                if (metadataCount == 0) {
                                    val imageProcessing = ImageProcessing(apiVersion, file, sidecarDir, metadataObj)
                                    metadataObj = imageProcessing.createThumbnails()
                                    if (metadataObj?.getThumbnailSmallWidth() != null && metadataObj.getThumbnailSmallHeight() != null && metadataObj.getThumbnailUrlSmall() != null) {
                                        metadataObj.setHidden(false)

                                        try {
                                            metadataRepository?.save(metadataObj)

                                            try {
                                                if (settings != null && webClient != null && FileUtils.checkCompreFaceConnection(settings.getCompreFaceServer(), settings.getCompreFaceKey())) {
                                                    // Have at least 3 people tagged
                                                    val compreFaceTagAllow = 3
                                                    if (recognitionLabelPhotoLabels!!.count() >= compreFaceTagAllow) {
                                                        var builder = MultipartBodyBuilder()
                                                        builder.part(
                                                            "file",
                                                            FileSystemResource(metadataObj.getThumbnailPathSmall()!!)
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
                                                                "Recognizing face for " + metadataObj.getPath() + ": " + response
                                                            )
                                                        } catch(e: Exception) {
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
                                                            recognitionLabelPhotoObj.setMetadataId(metadataObj.getId())
                                                            recognitionLabelPhotoObj.setRecognitionLabelId(recognitionLabelObj.getId())
                                                            recognitionLabelPhotoObj.setConfidence("-0.1")
                                                            recognitionLabelPhotoRepository?.save(recognitionLabelPhotoObj)

                                                            logger.log(
                                                                Level.WARNING,
                                                                "Error recognizing face for " + metadataObj.getPath() + ": " + e.localizedMessage
                                                            )
                                                        }

                                                        if (response != null) {
                                                            var jsonObj = mapper.readTree(response)
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

                                                                        if (similarity != 1.0 && (similarity <= 0.0 || similarity >= settings.getRecognitionConfidenceThreshold()
                                                                                .toString().toDouble())
                                                                        ) {

                                                                            builder = MultipartBodyBuilder()
                                                                            builder.part(
                                                                                "file",
                                                                                FileSystemResource(metadataObj.getThumbnailPathSmall()!!)
                                                                            )

                                                                            response = null

                                                                            try {
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
                                                                                    .body(
                                                                                        BodyInserters.fromMultipartData(
                                                                                            builder.build()
                                                                                        )
                                                                                    )
                                                                                    .retrieve()
                                                                                    .bodyToMono(String::class.java)
                                                                                    .block()
                                                                            } catch (e: Exception) {
                                                                                logger.log(
                                                                                    Level.WARNING,
                                                                                    "Error uploading face for " + subject + ": " + e.localizedMessage
                                                                                )
                                                                            }

                                                                            var compreFaceImageId: String? = null
                                                                            if (response != null) {
                                                                                jsonObj = mapper.readTree(response)
                                                                                if (jsonObj.has("image_id")) {
                                                                                    compreFaceImageId =
                                                                                        jsonObj["image_id"].toString()
                                                                                    compreFaceImageId =
                                                                                        compreFaceImageId.drop(1)
                                                                                            .dropLast(1)
                                                                                }
                                                                            }

                                                                            logger.log(
                                                                                Level.INFO,
                                                                                "Uploaded face for " + metadataObj.getPath() + " for subject " + subject + ": " + response
                                                                            )

                                                                            val recognitionLabelObj =
                                                                                recognitionLabelRepository?.findByNameIgnoreCase(
                                                                                    subject
                                                                                )

                                                                            val recognitionLabelPhoto = recognitionLabelPhotoRepository?.countByRecognitionLabelIdAndMetadataId(recognitionLabelObj!!.getId(), metadataObj.getId())

                                                                            if (recognitionLabelObj != null && recognitionLabelPhoto == 0) {
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
                                                                                recognitionLabelPhotoRepository?.save(
                                                                                    recognitionLabelPhotoObj
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        logger.log(
                                                            Level.INFO,
                                                            "You need to manually tag at least $compreFaceTagAllow different people to use face recognition service"
                                                        )
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                logger.log(
                                                    Level.INFO,
                                                    "Issue connecting to face recognizer: " + metadataObj.getPath() + ": " + e.localizedMessage
                                                )
                                            }

                                            threadText = file.path + " indexed"
                                            scanCount++
                                        } catch (e: Exception) {
                                            logger.log(
                                                Level.SEVERE,
                                                "Error saving metadata: " + metadataObj.getPath() + ": " + e.localizedMessage
                                            )
                                            threadText = "Error saving metadata: " + metadataObj.getPath() + "."
                                        }
                                    } else {
                                        logger.log(
                                            Level.WARNING,
                                            "Could not process thumbnails for "+file.path+"."
                                        )
                                        threadText = "Could not process thumbnails for "+file.path+"."
                                    }
                                } else {
                                    threadText = file.path + " ENTRY EXISTS"
                                    logger.log(Level.INFO, "Entry exists: " + file.name)
                                }
                            }
                        } else {
                            threadText = file.path + " NOT SUPPORTED"
                            logger.log(Level.WARNING, "File not supported: " + threadFile.name)
                        }

                        writeToThreadFile(threadText, threadFile)
                    }
                } else {
                    writeToThreadFile(threadText, threadFile)
                    logger.log(Level.INFO, "Entry exists: " + file.name)
                }

                if (file.isDirectory) {
                    getFile(file.absolutePath, threadFile, sidecarDir, rootDir, mediaExcludeDirs, settings, webClient, recognitionLabelPhotoLabels)
                }
            }
        }
    }
}