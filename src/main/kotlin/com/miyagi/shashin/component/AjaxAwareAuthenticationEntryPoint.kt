package com.miyagi.shashin.component

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.util.ApiResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.ui.set
import java.io.IOException
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class AjaxAwareAuthenticationEntryPoint(loginFormUrl: String?, private var apiVersion: String?) : LoginUrlAuthenticationEntryPoint(loginFormUrl) {

    @Throws(IOException::class, ServletException::class)
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException?
    ) {
        var uriPath = request.requestURI.toString()

        if (uriPath.contains("api/$apiVersion/")) {
            try {
                val securityContext: SecurityContext =
                    request.session.getAttribute("SPRING_SECURITY_CONTEXT") as SecurityContext
                val authorities = securityContext.authentication.authorities as Collection<GrantedAuthority>

                var currauthority = ""

                for (authority in authorities) {
                    currauthority = authority.authority
                }

                if (currauthority == "ROLE_ADMIN") {
                    uriPath = "/timeline"
                } else if (currauthority == "ROLE_USER") {
                    uriPath = "/albums"
                }

            } catch (e: Exception) {
                uriPath = loginFormUrl
            }
        }
        request.session.setAttribute("ShashinReferer",uriPath)

        val ajaxHeader = request.getHeader("X-Requested-With")
        if ("XMLHttpRequest" == ajaxHeader || request.requestURI.startsWith("/api/$apiVersion/")) {
            response.contentType = "application/json"
            val payload: MutableMap<String, Any> = HashMap()
            payload["msg"] = "Unauthorized"
            payload["statusCode"] = HttpServletResponse.SC_UNAUTHORIZED
            payload["status"] = ApiResponse.FAIL.status
            val json = ObjectMapper().writeValueAsString(payload)
            response.writer.append(json)
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            //response.sendError(HttpServletResponse.SC_FORBIDDEN, "Ajax Request Denied (Session Expired)")
        } else {
            super.commence(request, response, authException)
        }
    }
}