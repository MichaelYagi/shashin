package com.miyagi.shashin.service

import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.UserRepository
import org.springframework.aop.framework.AopProxyUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.stereotype.Service
import javax.servlet.http.HttpServletRequest


@Service
object AuthenticationService {
    private const val AUTH_TOKEN_HEADER_NAME = "X-API-KEY"

    fun getAuthentication(userRepository: UserRepository?, request: HttpServletRequest): Authentication {
        // Get API from header
        val apiKey = request.getHeader(AUTH_TOKEN_HEADER_NAME)
        var userObj: User? = null
        if (apiKey != null) {
            userObj = userRepository?.findByApikey(apiKey.trim())
        }

        println(apiKey)

        if (apiKey == null || userObj == null || (userObj.getApikey() != null && apiKey != userObj.getApikey()) || (userObj.getApikey() != null && !userObj.getIsAllowed()!!)) {
            throw BadCredentialsException("{\"message\":\"Invalid API Key\"}")
        }

        // See how UserController.loginUser handles login
        return ApiKeyAuthentication(apiKey, AuthorityUtils.commaSeparatedStringToAuthorityList(userObj.getAuthority()))
    }
}