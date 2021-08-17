package com.miyagi.shashin.component

import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.DefaultRedirectStrategy
import org.springframework.security.web.RedirectStrategy
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.stereotype.Component
import java.io.IOException
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class AuthFailureHandler : SimpleUrlAuthenticationFailureHandler() {
    private val redirectStrategy = DefaultRedirectStrategy()

    @Throws(IOException::class, ServletException::class)
    override fun onAuthenticationFailure(
        request: HttpServletRequest?,
        response: HttpServletResponse?,
        exception: AuthenticationException?
    ) {
        redirectStrategy.sendRedirect(request, response, "/users/login?error=401")
    }
}