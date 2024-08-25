package com.miyagi.shashin.component

import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.service.AuthenticationService
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.GenericFilterBean
import java.io.IOException
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

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