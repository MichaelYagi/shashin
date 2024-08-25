package com.miyagi.shashin.component

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.service.CustomUserDetailsService
import com.miyagi.shashin.util.ApiResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository
import org.springframework.ui.set
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