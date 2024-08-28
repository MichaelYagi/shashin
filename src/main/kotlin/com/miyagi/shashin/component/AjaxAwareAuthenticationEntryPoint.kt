package com.miyagi.shashin.component

import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import java.io.IOException
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class AjaxAwareAuthenticationEntryPoint(loginFormUrl: String?) : LoginUrlAuthenticationEntryPoint(loginFormUrl) {

    @Throws(IOException::class, ServletException::class)
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException?
    ) {

        var requestUri = request.requestURI.toString()

        if (request.queryString != null) {
            requestUri = requestUri + "?" + request.queryString
        }

        request.session.setAttribute("ShashinReferer", requestUri)

        super.commence(request, response, authException)
    }
}