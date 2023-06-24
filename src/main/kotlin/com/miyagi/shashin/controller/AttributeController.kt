package com.miyagi.shashin.controller

import com.miyagi.shashin.model.Settings
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.SettingsRepository
import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.util.ApiResponse
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.util.StringUtils.capitalize
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.reactive.function.client.WebClient
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletResponse
import javax.transaction.Transactional


@ControllerAdvice
class AttributeController {

    private var logger: Logger = Logger.getLogger(AttributeController::class.simpleName)

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

    @ModelAttribute
    @Transactional
    fun addAttributes(model: Model, response: HttpServletResponse) {
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
        val requestAttributes = RequestContextHolder.currentRequestAttributes()
        val attributes = requestAttributes as ServletRequestAttributes
        val request = attributes.request
        var currentUser: User?

        //println("X-API-KEY: "+request.getHeader("X-API-KEY"))

        if (!request.getHeader("X-API-KEY").isNullOrBlank()) {
            currentUser = userRepository.findByApikey(request.getHeader("X-API-KEY"))
            if (currentUser != null) {
                model["currentUser"] = currentUser
                model["authority"] = currentUser.getAuthority()!!
                model["username"] = currentUser.getUsername()!!
                model["apikey"] = currentUser.getApikey()!!
            } else {
                logger.log(Level.INFO, "{\"message\":\"Invalid API Key\"}")
            }
        } else {
            val session = request.getSession(true)
            try {
                val securityContext: SecurityContext =
                    session.getAttribute("SPRING_SECURITY_CONTEXT") as SecurityContext
                val authorities = securityContext.authentication.authorities as Collection<GrantedAuthority>
                model["username"] = securityContext.authentication.name

                //println("Session: "+securityContext.authentication.name)

                for (authority in authorities) {
                    model["authority"] = authority.authority
                }
                currentUser = userRepository.findByUsername(securityContext.authentication.name)
                if (currentUser != null) {
                    if (currentUser.getAuthority() == adminRole && (currentUser.getIsAllowed() == false || currentUser.getIsAllowed() == null)) {
                        currentUser.setIsAllowed(true)
                    }
                    if (currentUser.getDarkMode() == null) {
                        currentUser.setDarkMode(false)
                    }
                    if (!currentUser.getApikey().isNullOrBlank()) {
                        model["apikey"] = currentUser.getApikey()!!
                    }
                }

                if (currentUser == null || currentUser.getIsAllowed() == false) {
                    SecurityContextHolder.clearContext()
                    session?.invalidate()
                    val cookie = Cookie("remember-me", null) // Not necessary, but saves bandwidth.
                    cookie.path = "/"
                    cookie.isHttpOnly = true
                    cookie.maxAge = 0
                    response.addCookie(cookie)
                } else {
                    model["currentUser"] = currentUser
                }
            } catch (e: Exception) {
                model["currentUser"] = User()
                val cookie = Cookie("remember-me", null) // Not necessary, but saves bandwidth.
                cookie.path = "/"
                cookie.isHttpOnly = true
                cookie.maxAge = 0
                response.addCookie(cookie)
                logger.log(Level.INFO, "Not logged in. " + e.localizedMessage)
            }
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