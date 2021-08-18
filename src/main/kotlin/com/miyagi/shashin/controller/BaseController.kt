package com.miyagi.shashin.controller

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
import javax.servlet.http.HttpSession


@ControllerAdvice
class BaseController {

    private var logger: Logger = Logger.getLogger(BaseController::class.simpleName)

    @ModelAttribute
    fun addAttributes(model: Model) {
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
        } catch(e: Exception) {
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