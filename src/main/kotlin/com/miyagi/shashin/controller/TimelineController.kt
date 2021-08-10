package com.miyagi.shashin.controller

import com.miyagi.shashin.TextUtils
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping

@Controller
class TimelineController {
    @GetMapping("/timeline")
    fun getTimeline(model: Model): String {
        val module = "timeline"
        model["data"] = "This is some timeline data"
        model["activePage"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }
}