package com.miyagi.shashin.component

import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.service.AuthenticationService
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.GenericFilterBean
import java.io.IOException
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class AuthenticationFilter(userRepository: UserRepository?) : GenericFilterBean() {
    private var userRepository: UserRepository? = null

    init {
        this.userRepository = userRepository
    }
    @Throws(IOException::class, ServletException::class)
    override fun doFilter(request: ServletRequest?, response: ServletResponse, filterChain: FilterChain) {
        try {
            val authentication: Authentication =  AuthenticationService.getAuthentication(this.userRepository, request as HttpServletRequest)
            SecurityContextHolder.getContext().authentication = authentication
        } catch (exp: Exception) {
            val httpResponse = response as HttpServletResponse
            httpResponse.status = HttpServletResponse.SC_UNAUTHORIZED
            httpResponse.contentType = MediaType.APPLICATION_JSON_VALUE
            val writer = httpResponse.writer
            writer.print(exp.message)
            writer.flush()
            writer.close()
            return
        }
        filterChain.doFilter(request, response)
    }
}