package com.miyagi.shashin.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.util.TextUtils
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
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

//        println(apiKey)

        if (apiKey == null || userObj == null || (userObj.getApikey() != null && apiKey != userObj.getApikey()) || (userObj.getApikey() != null && !userObj.getIsAuthorized()!!)) {

            val jsonResponseMap = mutableMapOf<String, Any>()
            jsonResponseMap["msg"] = "Invalid API Key"
            val now = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern(TextUtils.getCommonDateFormat())
            jsonResponseMap["timestamp"] = now.format(formatter);
            jsonResponseMap["status"] = HttpStatus.UNAUTHORIZED
            val mapper = ObjectMapper()
            val jsonResponse = mapper.writeValueAsString(jsonResponseMap)
            throw BadCredentialsException(jsonResponse)
        }

        // See how UserController.loginUser handles login
        return ApiKeyAuthentication(apiKey, AuthorityUtils.commaSeparatedStringToAuthorityList(userObj.getAuthority()))
    }
}