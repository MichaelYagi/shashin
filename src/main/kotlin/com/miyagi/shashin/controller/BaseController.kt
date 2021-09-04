package com.miyagi.shashin.controller

import com.miyagi.shashin.model.Settings
import com.miyagi.shashin.repository.SettingsRepository
import com.miyagi.shashin.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletResponse

@ControllerAdvice
class BaseController {

    private var logger: Logger = Logger.getLogger(BaseController::class.simpleName)

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private var settingsRepository: SettingsRepository? = null

    @Value("\${app.sidecar.path}")
    private lateinit var relativeSidecarDir: String

    @Value("\${app.api.version}")
    private lateinit var apiVersion: String

    @Value("\${app.role.admin}")
    private lateinit var adminRole: String

    @Value("\${app.role.user}")
    private lateinit var userRole: String

    @Value("\${app.endpoint.url.geocode}")
    private lateinit var geocodeUrl: String

    @ModelAttribute
    fun addAttributes(model: Model, response: HttpServletResponse) {
        model["userRole"] = userRole
        model["adminRole"] = adminRole
        model["settings"] = ""

        var queryLimit = 20
        val settingsCount = settingsRepository?.count()
        if (settingsCount != null && settingsCount > 0) {
            val settings = settingsRepository?.findFirstByOrderByIdAsc()
            if (settings != null) {
                queryLimit = settings.getQueryLimit()!!
            }
            if (settings != null) {
                model["settings"] = settings
            }
        } else {
            val settingsObj = Settings()
            settingsObj.setQueryLimit(20)
            settingsObj.setMatchScanLimit(10)
            settingsObj.setRecognitionConfidenceThreshold("0.6")
            val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val now = LocalDateTime.now()
            settingsObj.setCreatedAt(dtf.format(now))
            settingsObj.setModifiedAt(dtf.format(now))
            settingsRepository?.save(settingsObj)

            model["settings"] = settingsObj
        }
        model["queryLimit"] = queryLimit
        model["apiVersion"] = apiVersion
        model["relativeSidecarDir"] = relativeSidecarDir
        model["geocodeUrl"] = geocodeUrl
        model["parameter"] = ""

        model["currentUser"] = ""

        model["authority"] = ""
        model["username"] = ""
        val requestAttributes = RequestContextHolder.currentRequestAttributes()
        val attributes = requestAttributes as ServletRequestAttributes
        val request = attributes.request
        val session = request.getSession(true)
        try {
            val securityContext: SecurityContext = session.getAttribute("SPRING_SECURITY_CONTEXT") as SecurityContext
            val authorities = securityContext.authentication.authorities as Collection<GrantedAuthority>
            model["username"] = securityContext.authentication.name
            for (authority in authorities) {
                model["authority"] = authority.authority
            }
            val currentUser = userRepository.findByUsername(securityContext.authentication.name)
            if (currentUser != null) {
                model["currentUser"] = currentUser
            } else {
                SecurityContextHolder.clearContext();
                session?.invalidate()
                val cookie = Cookie("remember-me", null) // Not necessary, but saves bandwidth.
                cookie.path = "/"
                cookie.isHttpOnly = true
                cookie.maxAge = 0
                response.addCookie(cookie)
            }
        } catch(e: Exception) {
            model["currentUser"] = ""
            val cookie = Cookie("remember-me", null) // Not necessary, but saves bandwidth.
            cookie.path = "/"
            cookie.isHttpOnly = true
            cookie.maxAge = 0
            response.addCookie(cookie)
            logger.log(Level.WARNING, "Error getting authority: " + e.message)
        }
        model["baseUrl"] = String.format("%s://%s:%d/",request.scheme,  request.serverName, request.serverPort);

        model["operatingSystemInfo"] = getOperatingSystemInfo()
        model["copyrightYear"] = Calendar.getInstance().get(Calendar.YEAR)
        model["titleDescriptor"] = ""
        model["data"] = ""
        model["activePage"] = ""
        model["activeSidebar"] = ""
        model["titleDescriptor"] = ""
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