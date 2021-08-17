package com.miyagi.shashin.component

import com.miyagi.shashin.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.web.DefaultRedirectStrategy
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class AuthSuccessHandler : SimpleUrlAuthenticationSuccessHandler() {
    private val redirectStrategy = DefaultRedirectStrategy()

    @Value("\${app.role.admin}")
    private var adminRole: String? = null

    @Value("\${app.role.user}")
    private var userRole: String? = null

    var userRepository: UserRepository? = null

    @Throws(IOException::class)
    override fun handle(request: HttpServletRequest?, response: HttpServletResponse?, authentication: Authentication?) {

        if (authentication != null) {
            val user = userRepository?.findByUsername(authentication.name)
            val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val now = LocalDateTime.now()
            if (user != null) {
                user.setModifiedAt(dtf.format(now))
                user.setLoggedIn(true)
                userRepository?.save(user)
            }
            for (authority in authentication.authorities) {
                if (authority.authority == adminRole) {
                    redirectStrategy.sendRedirect(request, response, "/timeline")
                } else if (authority.authority == userRole) {
                    redirectStrategy.sendRedirect(request, response, "/albums")
                }
                break
            }
        }
    }
}