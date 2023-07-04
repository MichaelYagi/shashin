package com.miyagi.shashin.component

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.util.TextUtils
import org.springframework.http.HttpStatus
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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