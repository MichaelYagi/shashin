package com.miyagi.shashin.controller

import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.TextUtils
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import javax.servlet.http.HttpServletRequest

@Controller
class DocsController {
    @RequestMapping(value = ["/docs","/docs/about"], method = [RequestMethod.GET])
    fun getAbout(model: Model, request: HttpServletRequest): String {
        val module = "about"

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)

        return module
    }

    @GetMapping("/docs/technologies")
    fun getFeatures(model: Model, request: HttpServletRequest): String {
        val module = "technologies"

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)

        return module
    }
}