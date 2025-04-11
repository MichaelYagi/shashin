package com.miyagi.shashin.controller

import com.miyagi.shashin.component.ApiError
import com.miyagi.shashin.model.Settings
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.repository.SettingsRepository
import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.util.ApiResponse
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.TextUtils
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import jakarta.persistence.EntityNotFoundException
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.transaction.Transactional
import org.springframework.beans.TypeMismatchException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.http.*
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import org.springframework.web.servlet.resource.NoResourceFoundException
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger


@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
class AttributeController: ResponseEntityExceptionHandler() {

    @Autowired
    private val environment: Environment? = null

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private var settingsRepository: SettingsRepository? = null

    @Autowired
    private var metadataRepository: MetadataRepository? = null

    @Autowired
    private var buildProperties: BuildProperties? = null

    @Value("\${app.sidecar.path}")
    private lateinit var relativeSidecarDir: String

    @Value("\${app.api.version}")
    private lateinit var apiVersion: String

    @Value("\${app.role.super}")
    private lateinit var superRole: String

    @Value("\${app.role.admin}")
    private lateinit var adminRole: String

    @Value("\${app.role.user}")
    private lateinit var userRole: String

    @Value("\${app.endpoint.url.geocode}")
    private lateinit var geocodeUrl: String

    @Value("\${app.config.default.querylimit}")
    private var queryLimitProperty: Int = 30

    @Value("\${app.config.default.matchscanlimit}")
    private var matchScanLimitProperty: Int = 50

    @Value("\${app.config.default.trainingdatalimit}")
    private var trainingDataLimitProperty: Int = 100

    @Value("\${app.config.default.notificationlimit}")
    private var notificationLimitProperty: Int = 20

    @Value("\${app.config.default.searchhistorylimit}")
    private var searchHistoryLimitProperty: Int = 10

    @Value("\${app.config.default.recognitionConfidenceThreshold}")
    private lateinit var recognitionConfidenceThresholdProperty: String

    @Value("\${app.config.default.objectRecognitionConfidenceThreshold}")
    private lateinit var objectRecognitionConfidenceThresholdProperty: String

    @Value("\${server.port}")
    private lateinit var portProperty: String

    @Value("\${app.config.default.scheduledTime}")
    private lateinit var scheduledTime: String

    @Value("\${app.rememberme.key}")
    private var rememberMeKey: String? = null

    override fun handleHttpRequestMethodNotSupported(
        ex: HttpRequestMethodNotSupportedException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val acceptHeader = request.getHeader("accept")?.lowercase()
        if (acceptHeader != null && acceptHeader.contains("text/html")) {
            headers.contentType = MediaType.TEXT_HTML
        }
        return buildResponseEntity(ApiError(HttpStatus.METHOD_NOT_ALLOWED, ex.localizedMessage, ex), headers)
    }

    override fun handleNoHandlerFoundException(
        ex: NoHandlerFoundException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val acceptHeader = request.getHeader("accept")?.lowercase()
        if (acceptHeader != null && acceptHeader.contains("text/html")) {
            headers.contentType = MediaType.TEXT_HTML
        }
        return buildResponseEntity(ApiError(HttpStatus.NOT_FOUND, ex.localizedMessage, ex), headers)
    }

    override fun handleNoResourceFoundException(
        ex: NoResourceFoundException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val acceptHeader = request.getHeader("accept")?.lowercase()
        if (acceptHeader != null && acceptHeader.contains("text/html")) {
            // Causes an UnsupportedOperationException for some reason
            headers.contentType = MediaType.TEXT_HTML
        }
        return buildResponseEntity(ApiError(HttpStatus.NOT_FOUND, ex.localizedMessage, ex), headers)
    }

    override fun handleTypeMismatch(
        ex: TypeMismatchException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val acceptHeader = request.getHeader("accept")?.lowercase()
        if (acceptHeader != null && acceptHeader.contains("text/html")) {
            headers.contentType = MediaType.TEXT_HTML
        }
        return buildResponseEntity(ApiError(HttpStatus.BAD_REQUEST, ex.localizedMessage, ex), headers)
    }

    override fun handleHttpMediaTypeNotSupported(
        ex: HttpMediaTypeNotSupportedException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val acceptHeader = request.getHeader("accept")?.lowercase()
        if (acceptHeader != null && acceptHeader.contains("text/html")) {
            headers.contentType = MediaType.TEXT_HTML
        }
        return buildResponseEntity(ApiError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.localizedMessage, ex), headers)
    }

    private fun buildResponseEntity(apiError: ApiError, headers: HttpHeaders): ResponseEntity<Any> {
        return ResponseEntity<Any>(apiError, headers, apiError.getStatus()!!)
    }

//    @ExceptionHandler(Exception::class)
//    protected fun handleException(
//        headers: HttpHeaders,
//        request: WebRequest,
//        ex: Exception
//    ): ResponseEntity<Any>? {
//        val acceptHeader = request.getHeader("accept")?.lowercase()
//        if (acceptHeader != null && acceptHeader.contains("text/html")) {
//            headers.contentType = MediaType.TEXT_HTML
//        }
//        val apiError = ApiError(HttpStatus.NOT_FOUND)
//        apiError.setMsg(ex.message)
//        return buildResponseEntity(apiError, headers)
//    }

    @ExceptionHandler(EntityNotFoundException::class)
    protected fun handleEntityNotFound(
        headers: HttpHeaders,
        request: WebRequest,
        ex: EntityNotFoundException
    ): ResponseEntity<Any>? {
        val acceptHeader = request.getHeader("accept")?.lowercase()
        if (acceptHeader != null && acceptHeader.contains("text/html")) {
            headers.contentType = MediaType.TEXT_HTML
        }
        val apiError = ApiError(HttpStatus.NOT_FOUND)
        apiError.setMsg(ex.message)
        return buildResponseEntity(apiError, headers)
    }

    @ModelAttribute
    @Transactional
    fun addAttributes(model: Model, request: HttpServletRequest, response: HttpServletResponse, authentication: Authentication?) {
//        val totalTimingStart = Date()
//        var timingStart = Date()
        val logger: Logger = Logger.getLogger(AttributeController::class.simpleName)

        val browserDetails = request.getHeader("User-Agent")
        var browser = ""
        var os = ""
        var isMobile = false
        if (browserDetails != null) {
            val user = browserDetails.lowercase(Locale.getDefault())

            //===============Platform===========================
            os = if (user.lowercase().contains("windows")) {
                "Windows";
            } else if(user.lowercase().contains("mac")) {
                "Mac";
            } else if(user.lowercase().contains("x11")) {
                "Unix";
            } else if(user.lowercase().contains("android")) {
                "Android";
            } else if(user.lowercase().contains("iphone")) {
                "IPhone";
            } else {
                "UnKnown, More-Info: " + user.lowercase();
            }

//            logger.log(Level.INFO, "User Agent: $browserDetails")
            //===============Browser===========================
            if (user.contains("msie")) {
                browser = "IE"
            } else if (user.contains("safari") && user.contains("version")) {
                browser = "Safari"
            } else if (user.contains("opr") || user.contains("opera")) {
                browser = "Opera"
            } else if (user.contains("chrome")) {
                browser = "Chrome"
            } else if (user.indexOf("mozilla/7.0") > -1 || user.indexOf("netscape6") != -1 || user.indexOf("mozilla/4.7") != -1 || user.indexOf(
                    "mozilla/4.78"
                ) != -1 || user.indexOf("mozilla/4.08") != -1 || user.indexOf("mozilla/3") != -1
            ) {
                browser = "Netscape"
            } else if (user.contains("firefox")) {
                browser = "Firefox"
            } else if (user.contains("rv")) {
                browser = "IE"
            } else {
                browser = "Unknown browser: $browserDetails"
            }

            isMobile = TextUtils.isMobile(browserDetails)
        }
//        logger.log(Level.INFO,"Browser Name: $browser")
        model["isMobile"] = isMobile
        var thumbnailType = "225"
        var thumbnailHeight = "225"
        if (isMobile) {
            thumbnailType = "centered"
            thumbnailHeight = "120"
        }
        model["thumbnailType"] = thumbnailType
        model["thumbnailHeight"] = thumbnailHeight

        model["agentName"] = browser

        model["agentOS"] = os

        model["clientIP"] = TextUtils.getClientIp(request)

//        var timingEnd = Date()
//        var diff: Long = timingEnd.time - timingStart.time

//        var processingTime = SimpleDateFormat("mm:ss:SSS").format(Date(diff))
//        logger.log(Level.INFO, "AttributeController - browser processing time: $processingTime")

        model["userRole"] = userRole
        model["adminRole"] = adminRole
        model["superRole"] = superRole
        model["settings"] = Settings()
        model["activeProfile"] = "prod"
        model["faceRecogServicesAvailable"] = false
        val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
        sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
        model["timezone"] = sdtf.timeZone.toZoneId().toString()

        if (environment != null && environment.activeProfiles.isNotEmpty()) {
            model["activeProfile"] = environment.activeProfiles[0]
        }

//        timingStart = Date()

        var queryLimit = queryLimitProperty
        var searchHistoryLimit = searchHistoryLimitProperty

        model["acceptedMediaTypes"] = FileUtils.allowableMediaFiles()
        model["acceptedImageTypes"] = FileUtils.allowableImageFiles()

        model["hasMediaUploadDirectory"] = false

        val settings = settingsRepository?.findFirstByOrderByIdAsc()
        if (settings != null) {
            queryLimit = settings.getQueryLimit()!!
            searchHistoryLimit = settings.getSearchHistoryLimit()!!
            if (settings.getUploadMediaDirectory() != null && settings.getUploadMediaDirectory() != "") {
                model["hasMediaUploadDirectory"] = true
            }
            model["settings"] = settings
        } else {
            val settingsObj = Settings()
            settingsObj.setQueryLimit(queryLimitProperty)
            settingsObj.setMatchScanLimit(matchScanLimitProperty)
            settingsObj.setTrainingDataLimit(trainingDataLimitProperty)
            settingsObj.setNotificationLimit(notificationLimitProperty)
            settingsObj.setSearchHistoryLimit(searchHistoryLimitProperty)
//            settingsObj.setCompreFaceServer(comprefaceServer)
            settingsObj.setPort(portProperty)
            settingsObj.setScanAutomatically(false)
            settingsObj.setObjectDetection(false)
            settingsObj.setFacialDetection(false)
            settingsObj.setScheduledMatching(false)
            settingsObj.setScheduledTime(scheduledTime)
            model["objectRecogEnabled"] = false
            model["facialRecogEnabled"] = false
            settingsObj.setRecognitionConfidenceThreshold(recognitionConfidenceThresholdProperty)
            settingsObj.setObjectRecognitionConfidenceThreshold(objectRecognitionConfidenceThresholdProperty)
            settingsObj.setCreatedAt(getCurrentTimestamp())
            settingsObj.setModifiedAt(getCurrentTimestamp())
            model["settings"] = settingsObj
        }

        // Check for face recog files
//        FileUtils.loadFaceRecogFiles(settings)

        model["searchHistoryLimit"] = searchHistoryLimit
        model["queryLimit"] = queryLimit
        model["apiVersion"] = apiVersion
        model["relativeSidecarDir"] = relativeSidecarDir
        model["geocodeUrl"] = geocodeUrl
        model["buildProperties"] = ""
        if (buildProperties != null) {
            model["buildProperties"] = buildProperties!!
        }
        model["parameter"] = ""
        model["currentUser"] = User()
        model["authority"] = ""
        model["username"] = ""
        model["apikey"] = ""
        model["randomString"] = TextUtils.generateUUID(
            getCurrentTimestamp(),
            null,
            null,
            null,
            null,
            null,
            "random string generated from AttributeController"
        )

//        timingEnd = Date()
//        diff = timingEnd.time - timingStart.time

//        processingTime = SimpleDateFormat("mm:ss:SSS").format(Date(diff))
//        logger.log(Level.INFO, "AttributeController - setting processing time: $processingTime")

//        timingStart = Date()

        model["hasAlbums"] = false
        val currentUser: User?
        var currentUserId = 0
        val cookieUser: User? = TextUtils.checkValidRememberMeToken(request.getHeader("Cookie"), rememberMeKey.toString(), userRepository)

        model["userCount"] = userRepository.count()

        model["requestResourceType"] = "web"
        if (!request.getHeader("X-API-KEY").isNullOrBlank()) {
            model["requestResourceType"] = "api"
            currentUser = userRepository.findByApikey(request.getHeader("X-API-KEY"))
            if (currentUser != null) {
                model["currentUser"] = currentUser
                model["authority"] = currentUser.getAuthority()!!
                model["username"] = currentUser.getUsername()!!
                model["apikey"] = currentUser.getApikey()!!

                if (currentUser.getDarkMode() == null) {
                    currentUser.setDarkMode(false)
                }
                if (currentUser.getAutoplayVideo() == null) {
                    currentUser.setAutoplayVideo(false)
                }
                if (currentUser.getShowPlacename() == null) {
                    currentUser.setShowPlacename(false)
                }
                if (currentUser.getNotificationAlerts() == null) {
                    currentUser.setNotificationAlerts(true)
                }
                if (currentUser.getSlideshowInterval() == null) {
                    currentUser.setSlideshowInterval(10)
                }

                val mediaCount = (if (currentUser.getAuthority()!! == "ROLE_ADMIN" || currentUser.getAuthority()!! == "ROLE_SUPER") {
                    metadataRepository?.countAllByTypeContains("image")
                } else {
                    metadataRepository?.countAlbumByMedia(currentUser.getId(), "image")
                })

                if (mediaCount != null) {
                    model["hasAlbums"] = mediaCount > 0
                }
            } else {
                logger.log(Level.INFO, "{\"message\":\"Invalid API Key\"}")
            }
        } else if (authentication != null) {
            val authorities = authentication.authorities as Collection<GrantedAuthority>
            model["username"] = authentication.name

//            println("Session: "+authentication.name)

            for (authority in authorities) {
                model["authority"] = authority.authority
            }

            if (request.session.getAttribute("CurrentUser") == null) {
//                logger.log(Level.INFO, "AttributeController - CurrentUser db check")

                currentUser = userRepository.findByUsername(authentication.name)
                request.session.setAttribute("CurrentUser",currentUser)
            } else {
//                logger.log(Level.INFO, "AttributeController - CurrentUser attribute check")

                currentUser = request.session.getAttribute("CurrentUser") as User
            }

            if (currentUser == null || currentUser.getIsAuthorized() == false) {
                SecurityContextHolder.clearContext()
                val session = request.getSession(true)
                session?.invalidate()
                val cookie = Cookie("remember-me", null) // Not necessary, but saves bandwidth.
                cookie.path = "/"
                cookie.isHttpOnly = true
                cookie.maxAge = 0
                response.addCookie(cookie)
            } else {
                // Cleanup persistent tokens on non api requests
                //DatabaseUtil.cleanupPersistence(persistentLoginsExpiryRepository, persistentLoginsRepository)

                if (currentUser.getDarkMode() == null) {
                    currentUser.setDarkMode(false)
                }
                if (currentUser.getAutoplayVideo() == null) {
                    currentUser.setAutoplayVideo(false)
                }
                if (currentUser.getShowPlacename() == null) {
                    currentUser.setShowPlacename(false)
                }
                if (currentUser.getNotificationAlerts() == null) {
                    currentUser.setNotificationAlerts(true)
                }
                if (currentUser.getSlideshowInterval() == null) {
                    currentUser.setSlideshowInterval(10)
                }
                if (!currentUser.getApikey().isNullOrBlank()) {
                    model["apikey"] = currentUser.getApikey()!!
                }
                currentUserId = currentUser.getId()
                model["currentUser"] = currentUser
            }

            val mediaCount = (if (currentUser?.getAuthority()!! == "ROLE_ADMIN" || currentUser.getAuthority()!! == "ROLE_SUPER") {
                metadataRepository?.countAllByTypeContains("image")
            } else {
                metadataRepository?.countAlbumByMedia(currentUser.getId(), "image")
            })

            if (mediaCount != null) {
                model["hasAlbums"] = mediaCount > 0
            }
        } else if (cookieUser != null) {
            currentUser = cookieUser

            model["username"] = cookieUser.getUsername()
            model["authority"] = cookieUser.getAuthority()
            model["apikey"] = cookieUser.getApikey()!!
            model["currentUser"] = cookieUser
        }

//        timingEnd = Date()
//        diff = timingEnd.time - timingStart.time

//        processingTime = SimpleDateFormat("mm:ss:SSS").format(Date(diff))
//        logger.log(Level.INFO, "AttributeController - current user processing time: $processingTime")

//        timingStart = Date()

        var baseUrlBuilder = ServletUriComponentsBuilder.fromRequestUri(request).replacePath(null)
        if (request.scheme == "https") {
            baseUrlBuilder = baseUrlBuilder.scheme("https")
        }
        val baseUrl = baseUrlBuilder.build().toUriString()
        model["baseUrl"] = "$baseUrl/"

        model["operatingSystemInfo"] = ""
        if (model.getAttribute("authority") == adminRole || model.getAttribute("authority") == superRole) {
            model["operatingSystemInfo"] = getOperatingSystemInfo()
        }
        model["copyrightYear"] = Calendar.getInstance().get(Calendar.YEAR)
        model["titleDescriptor"] = ""
        model["message"] = ""
        model["activePage"] = ""
        model["activeSidebar"] = ""
        model["titleDescriptor"] = ""
        model["msg"] = "Response status not set. Defaulting to status fail."
        model["status"] = ApiResponse.FAIL.status


//        timingEnd = Date()
//        diff = timingEnd.time - timingStart.time

//        processingTime = SimpleDateFormat("mm:ss:SSS").format(Date(diff))
//        logger.log(Level.INFO, "AttributeController - base builder processing time: $processingTime")

//        val totalTimingEnd = Date()
//        diff = totalTimingEnd.time - totalTimingStart.time

//        processingTime = SimpleDateFormat("mm:ss:SSS").format(Date(diff))
//        logger.log(Level.INFO, "AttributeController - total processing time: $processingTime")
    }

    private fun getOperatingSystemInfo(): String {
        // The key for getting operating system name
        val name = "os.name"
        // The key for getting operating system version
        val version = "os.version"
        // The key for getting operating system architecture
        val architecture = "os.arch"

        return System.getProperty(name)+" v"+System.getProperty(version)+" "+System.getProperty(architecture)
    }
}