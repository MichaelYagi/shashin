package com.miyagi.shashin.component

import com.miyagi.shashin.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.security.core.session.SessionDestroyedEvent
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


@Component
class SessionEndedListener: ApplicationListener<SessionDestroyedEvent> {

    @Autowired
    var userRepository: UserRepository? = null

    override fun onApplicationEvent(event: SessionDestroyedEvent) {
        for (securityContext in event.securityContexts) {
            val authentication = securityContext.authentication
            val user = userRepository?.findByUsername(authentication.name)
            val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val now = LocalDateTime.now()
            if (user != null) {
                user.setModifiedAt(dtf.format(now))
                user.setLoggedIn(false)
                userRepository?.save(user)
            }
        }
    }
}