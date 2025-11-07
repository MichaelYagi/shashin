package com.miyagi.shashin.component

import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.scheduling.annotation.Async
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.logout.LogoutHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class CustomLogoutHandler(var userRepository: UserRepository? = null) : LogoutHandler {

    @Async
    override fun logout(request: HttpServletRequest, response: HttpServletResponse, authentication: Authentication?) {
        var authentication = authentication
        if (authentication == null) {
            authentication = SecurityContextHolder.getContext().authentication
        }

        if (authentication != null && !authentication.name.isNullOrBlank()) {
            synchronized (userRepository!!) {
                val user = userRepository?.findByUsername(authentication.name)

                if (user != null) {
                    user.setModifiedAt(getCurrentTimestamp())
                    userRepository?.save(user)
                }
            }
        }
    }
}
