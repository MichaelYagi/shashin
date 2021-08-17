package com.miyagi.shashin.controller

import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute
import java.util.*


@ControllerAdvice
class BaseController {

    @ModelAttribute
    fun addAttributes(model: Model) {
        model["authority"] = ""
        val authorities = SecurityContextHolder.getContext().authentication.authorities as Collection<GrantedAuthority>

        for (authority in authorities) {
            model["authority"] = authority.authority
        }
        model["copyrightYear"] = Calendar.getInstance().get(Calendar.YEAR)
        model["titleDescriptor"] = ""
    }
}