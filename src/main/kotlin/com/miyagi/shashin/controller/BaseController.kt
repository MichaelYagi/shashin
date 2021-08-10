package com.miyagi.shashin.controller

import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute
import java.util.*
import kotlin.collections.HashMap


@ControllerAdvice
class BaseController {
    @ModelAttribute
    fun addAttributes(model: Model) {
        model["copyrightYear"] = Calendar.getInstance().get(Calendar.YEAR)
        model["titleDescriptor"] = ""
    }
}