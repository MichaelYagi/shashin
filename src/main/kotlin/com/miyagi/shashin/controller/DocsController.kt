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
        val module = "docs/about"

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)

        return module
    }

    @GetMapping("/docs/technologies")
    fun getFeatures(model: Model, request: HttpServletRequest): String {
        val module = "docs/technologies"

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)

        return module
    }

    @GetMapping("/docs/api/v1")
    fun getVOneApi(model: Model, request: HttpServletRequest): String {
        val module = "docs/api"

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)

        return module
    }

    @GetMapping("/docs/api/v1/timeline")
    fun getTimelineApi(model: Model, request: HttpServletRequest): String {
        val module = "docs/timeline"

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)

        return module
    }

    @GetMapping("/docs/api/v1/user")
    fun getUserApi(model: Model, request: HttpServletRequest): String {
        val module = "docs/user"

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)

        return module
    }

    @GetMapping("/docs/api/v1/album")
    fun getAlbumApi(model: Model, request: HttpServletRequest): String {
        val module = "docs/album"

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)

        return module
    }
}