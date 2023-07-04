package com.miyagi.shashin.component

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.therapi.runtimejavadoc.repack.com.eclipsesource.json.JsonObject
import com.miyagi.shashin.repository.NotificationRepository
import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.util.TextUtils
import jdk.jfr.ContentType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.web.DefaultRedirectStrategy
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Component
class ApiAccessDeniedHandler : AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest?,
        response: HttpServletResponse?,
        accessDeniedException: org.springframework.security.access.AccessDeniedException?
    ) {
        val jsonResponseMap = mutableMapOf<String, Any>()
        jsonResponseMap["msg"] = accessDeniedException?.localizedMessage!!
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern(TextUtils.getCommonDateFormat())
        jsonResponseMap["timestamp"] = now.format(formatter);
        jsonResponseMap["status"] = HttpStatus.FORBIDDEN
        val mapper = ObjectMapper()
        val jsonResponse = mapper.writeValueAsString(jsonResponseMap)
        
        response?.contentType = "application/json"
        response?.status = HttpStatus.FORBIDDEN.value()
        response?.writer?.write(jsonResponse)
    }
}