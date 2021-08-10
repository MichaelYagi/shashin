package com.miyagi.shashin.controller

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping

@Controller
class TestController {
    @GetMapping("/test")
    fun test(model: Model): String {
        model["somevalue"] = "This is a test"
        return "test"
    }
}