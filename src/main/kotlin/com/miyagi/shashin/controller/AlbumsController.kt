package com.miyagi.shashin.controller

import com.miyagi.shashin.util.TextUtils
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping

@Controller
class AlbumsController {

    @GetMapping("/albums")
    fun getSettings(model: Model): String {
        val module = "albums"
        model["data"] = "There are no albums."
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }
}