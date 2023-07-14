package com.miyagi.shashin.controller

import com.miyagi.shashin.component.ApiError
import com.miyagi.shashin.model.Settings
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.SettingsRepository
import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.util.ApiResponse
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.TextUtils
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.util.StringUtils.capitalize
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpClientErrorException.Unauthorized
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.persistence.EntityNotFoundException
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletResponse
import javax.transaction.Transactional


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
    private var buildProperties: BuildProperties? = null

    @Value("\${app.sidecar.path}")
    private lateinit var relativeSidecarDir: String

    @Value("\${app.api.version}")
    private lateinit var apiVersion: String

    @Value("\${app.role.admin}")
    private lateinit var adminRole: String

    @Value("\${app.role.user}")
    private lateinit var userRole: String

    @Value("\${app.endpoint.url.compreface}")
    private lateinit var comprefaceServer: String

    @Value("\${app.endpoint.url.geocode}")
    private lateinit var geocodeUrl: String

    @Value("\${app.config.default.querylimit}")
    private var queryLimitProperty: Int = 20

    @Value("\${app.config.default.matchscanlimit}")
    private var matchScanLimitProperty: Int = 50

    @Value("\${app.config.default.trainingdatalimit}")
    private var trainingDataLimitProperty: Int = 100

    @Value("\${app.config.default.notificationlimit}")
    private var notificationLimitProperty: Int = 20

    @Value("\${app.config.default.searchhistorylimit}")
    private var searchHistoryLimitProperty: Int = 15

    @Value("\${app.config.default.recognitionConfidenceThreshold}")
    private lateinit var recognitionConfidenceThresholdProperty: String

    @Value("\${server.port}")
    private lateinit var portProperty: String

//    @ExceptionHandler(Exception::class)
//    fun globeExceptionHandler(ex: java.lang.Exception, request: WebRequest): ResponseEntity<*>? {
//        return buildResponseEntity(ApiError(HttpStatus.INTERNAL_SERVER_ERROR, ex.localizedMessage, ex))
//    }

    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<Any?> {
        val error = "Malformed JSON request"
        return buildResponseEntity(ApiError(HttpStatus.BAD_REQUEST, error, ex))
    }

    @ExceptionHandler(EntityNotFoundException::class)
    protected fun handleEntityNotFound(
        ex: EntityNotFoundException
    ): ResponseEntity<Any?>? {
        val apiError = ApiError(HttpStatus.NOT_FOUND)
        apiError.setMsg(ex.message)
        return buildResponseEntity(apiError)
    }

    private fun buildResponseEntity(apiError: ApiError): ResponseEntity<Any?> {
        return ResponseEntity<Any?>(apiError, apiError.getStatus()!!)
    }

    override fun handleHttpMediaTypeNotSupported(
        ex: HttpMediaTypeNotSupportedException,
        headers: HttpHeaders,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<Any?> {
        return buildResponseEntity(ApiError(HttpStatus.METHOD_NOT_ALLOWED, ex.localizedMessage, ex))
    }

    override fun handleHttpRequestMethodNotSupported(
        ex: HttpRequestMethodNotSupportedException,
        headers: HttpHeaders,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<Any?> {
        return buildResponseEntity(ApiError(HttpStatus.METHOD_NOT_ALLOWED, ex.localizedMessage, ex))
    }

    @ModelAttribute
    @Transactional
    fun addAttributes(model: Model, response: HttpServletResponse, authentication: Authentication?) {
        model["userRole"] = userRole
        model["adminRole"] = adminRole
        model["settings"] = Settings()
        model["activeProfile"] = ""
        if (environment != null && environment.activeProfiles.isNotEmpty()) {
            val profile = environment.activeProfiles[0]
            if (profile != "prod") {
                model["activeProfile"] = capitalize(profile)
            }
        }

        var queryLimit = queryLimitProperty
        var searchHistoryLimit = searchHistoryLimitProperty

        val settings = settingsRepository?.findFirstByOrderByIdAsc()
        if (settings != null) {
            queryLimit = settings.getQueryLimit()!!
            searchHistoryLimit = settings.getSearchHistoryLimit()!!
            model["settings"] = settings
        } else {
            val settingsObj = Settings()
            settingsObj.setQueryLimit(queryLimitProperty)
            settingsObj.setMatchScanLimit(matchScanLimitProperty)
            settingsObj.setTrainingDataLimit(trainingDataLimitProperty)
            settingsObj.setNotificationLimit(notificationLimitProperty)
            settingsObj.setSearchHistoryLimit(searchHistoryLimitProperty)
            settingsObj.setCompreFaceServer(comprefaceServer)
            settingsObj.setPort(portProperty)
            settingsObj.setScanAutomatically(false)
            settingsObj.setRecognitionConfidenceThreshold(recognitionConfidenceThresholdProperty)
            settingsObj.setCreatedAt(getCurrentTimestamp())
            settingsObj.setModifiedAt(getCurrentTimestamp())

            model["settings"] = settingsObj
        }
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
        model["randomString"] = TextUtils.generateUUID(getCurrentTimestamp())
        val requestAttributes = RequestContextHolder.currentRequestAttributes()
        val attributes = requestAttributes as ServletRequestAttributes
        val request = attributes.request
        var currentUser: User?

        if (!request.getHeader("X-API-KEY").isNullOrBlank()) {
            currentUser = userRepository.findByApikey(request.getHeader("X-API-KEY"))
            if (currentUser != null) {
                model["currentUser"] = currentUser
                model["authority"] = currentUser.getAuthority()!!
                model["username"] = currentUser.getUsername()!!
                model["apikey"] = currentUser.getApikey()!!
            } else {
                val logger: Logger = Logger.getLogger(AttributeController::class.simpleName)
                logger.log(Level.INFO, "{\"message\":\"Invalid API Key\"}")
            }
        } else if (authentication != null) {
            val authorities = authentication.authorities as Collection<GrantedAuthority>
            model["username"] = authentication.name

            //println("Session: "+securityContext.authentication.name)

            for (authority in authorities) {
                model["authority"] = authority.authority
            }
            currentUser = userRepository.findByUsername(authentication.name)

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
                if (currentUser.getDarkMode() == null) {
                    currentUser.setDarkMode(false)
                }
                if (!currentUser.getApikey().isNullOrBlank()) {
                    model["apikey"] = currentUser.getApikey()!!
                }
                model["currentUser"] = currentUser
            }
//            } else {
//                val logger: Logger = Logger.getLogger(AttributeController::class.simpleName)
//                model["currentUser"] = User()
//                val cookie = Cookie("remember-me", null) // Not necessary, but saves bandwidth.
//                cookie.path = "/"
//                cookie.isHttpOnly = true
//                cookie.maxAge = 0
//                response.addCookie(cookie)
//                logger.log(Level.INFO, "Not logged in. Authentication is null")
//            }
        }
        model["baseUrl"] = String.format("%s://%s:%d/",request.scheme,  request.serverName, request.serverPort);

        model["faceRecogServicesAvailable"] = FileUtils.checkCompreFaceConnection(settings?.getCompreFaceServer(), settings?.getCompreFaceKey())

        model["operatingSystemInfo"] = ""
        if (model.getAttribute("authority") ==  adminRole) {
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