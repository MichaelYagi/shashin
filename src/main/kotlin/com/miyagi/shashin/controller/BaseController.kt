package com.miyagi.shashin.controller

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
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

@ControllerAdvice
class BaseController {

    private var logger: Logger = Logger.getLogger(BaseController::class.simpleName)

    @Autowired
    private lateinit var userRepository: UserRepository

    @Value("\${app.sidecar.path}")
    private lateinit var relativeSidecarDir: String

    @Value("\${app.api.version}")
    private lateinit var apiVersion: String

    @Value("\${app.query.limit}")
    private var queryLimit: Int? = null

    @Value("\${app.role.admin}")
    private lateinit var adminRole: String

    @Value("\${app.role.user}")
    private lateinit var userRole: String

    @Value("\${app.endpoint.url.geocode}")
    private lateinit var geocodeUrl: String

    @ModelAttribute
    fun addAttributes(model: Model) {
        model["userRole"] = userRole
        model["adminRole"] = adminRole
        model["queryLimit"] = queryLimit!!.toInt()
        model["apiVersion"] = apiVersion
        model["relativeSidecarDir"] = relativeSidecarDir
        model["geocodeUrl"] = geocodeUrl

        model["currentUser"] = ""

        model["authority"] = ""
        model["username"] = ""
        val requestAttributes = RequestContextHolder
            .currentRequestAttributes()
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
            }
        } catch(e: Exception) {
            model["currentUser"] = ""
            logger.log(Level.WARNING, "Error getting authority: " + e.message)
        }
        model["copyrightYear"] = Calendar.getInstance().get(Calendar.YEAR)
        model["titleDescriptor"] = ""
        model["data"] = ""
        model["activePage"] = ""
        model["activeSidebar"] = ""
        model["titleDescriptor"] = ""
    }
}