package com.miyagi.shashin.component

import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.stereotype.Component
import java.io.IOException
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class AjaxAwareAuthenticationEntryPoint(loginFormUrl: String?) : LoginUrlAuthenticationEntryPoint(loginFormUrl) {
    @Throws(IOException::class, ServletException::class)
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException?
    ) {
        val ajaxHeader = request.getHeader("X-Requested-With")
        if ("XMLHttpRequest" == ajaxHeader) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Ajax Request Denied (Session Expired)")
        } else {
            super.commence(request, response, authException)
        }
    }
}