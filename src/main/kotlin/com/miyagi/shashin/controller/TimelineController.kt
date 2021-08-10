package com.miyagi.shashin.controller

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping

@Controller
class TimelineController {
    @GetMapping("/timeline")
    fun test(model: Model): String {
        model["data"] = "This is some data"
        return "timeline"
    }
}