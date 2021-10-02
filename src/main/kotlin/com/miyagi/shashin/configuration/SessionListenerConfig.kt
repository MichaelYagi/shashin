package com.miyagi.shashin.configuration

import com.miyagi.shashin.controller.BaseController
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.util.FileUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.Level
import java.util.logging.Logger
import javax.servlet.http.HttpSessionEvent

import javax.servlet.http.HttpSessionListener

@Configuration
internal class SessionListenerConfig : HttpSessionListener {

    @Autowired
    private val sessionRegistry: SessionRegistry? = null

    private val loggedInUsernameList = mutableListOf<String>()

    private var logger: Logger = Logger.getLogger(SessionListenerConfig::class.simpleName)

    @Autowired
    var userRepository: UserRepository? = null

    override fun sessionCreated(event: HttpSessionEvent) {
        //println("session created")
        checkPrincipals(sessionRegistry)
    }

    override fun sessionDestroyed(event: HttpSessionEvent) {
        //println("session destroyed")
        checkPrincipals(sessionRegistry)
    }

    private fun checkPrincipals(sessionRegistry: SessionRegistry?) {
        if (sessionRegistry != null) {
            for (loggedInPrinciple in sessionRegistry.allPrincipals) {
                val principle = loggedInPrinciple as UserDetails
                val username = principle.username
                loggedInUsernameList.add(username)
            }
        }
        val users = userRepository?.findAllByIsAllowedTrue()
        if (users != null) {
            val userList = mutableListOf<User>()
            for (user in users) {
                if (loggedInUsernameList.contains(user.getUsername())) {
                    user.setLoggedIn(true)
                } else {
                    user.setLoggedIn(false)
                }
                userList.add(user)
            }
            if (userList.isNotEmpty()) {
                userRepository?.saveAll(userList)
            }
        }
    }
}