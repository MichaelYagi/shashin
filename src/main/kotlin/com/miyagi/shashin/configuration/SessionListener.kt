package com.miyagi.shashin.configuration

import com.miyagi.shashin.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.ui.set
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.servlet.http.HttpSessionEvent

import javax.servlet.http.HttpSessionListener

@Configuration
internal class SessionListener : HttpSessionListener {

    @Autowired
    var userRepository: UserRepository? = null

    override fun sessionCreated(event: HttpSessionEvent) {
        //println("session created")
    }

    override fun sessionDestroyed(event: HttpSessionEvent) {
        //println("session destroyed")
        val securityContext: SecurityContext = event.session.getAttribute("SPRING_SECURITY_CONTEXT") as SecurityContext
        val user = userRepository?.findByUsername(securityContext.authentication.name)
        val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val now = LocalDateTime.now()
        if (user != null) {
            user.setLoggedIn(false)
            user.setModifiedAt(dtf.format(now))
            userRepository?.save(user)
        }
    }
}