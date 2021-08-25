package com.miyagi.shashin.configuration

import com.miyagi.shashin.controller.BaseController
import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.util.FileUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.Level
import java.util.logging.Logger
import javax.servlet.http.HttpSessionEvent

import javax.servlet.http.HttpSessionListener

@Configuration
internal class SessionListenerConfig : HttpSessionListener {

    private var logger: Logger = Logger.getLogger(BaseController::class.simpleName)

    @Autowired
    var userRepository: UserRepository? = null

    override fun sessionCreated(event: HttpSessionEvent) {
        //println("session created")
    }

    override fun sessionDestroyed(event: HttpSessionEvent) {
        //println("session destroyed")
        try {
            val securityContext: SecurityContext =
                event.session.getAttribute("SPRING_SECURITY_CONTEXT") as SecurityContext
            val user = userRepository?.findByUsername(securityContext.authentication.name)
            val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val now = LocalDateTime.now()
            if (user != null) {
                user.setLoggedIn(false)
                user.setModifiedAt(dtf.format(now))
                userRepository?.save(user)
            }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "sessionDestroyed Listener error: " + e.message)
        }
    }
}