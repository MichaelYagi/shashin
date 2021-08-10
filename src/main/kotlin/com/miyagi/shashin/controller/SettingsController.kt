package com.miyagi.shashin.controller

import com.miyagi.shashin.TextUtils
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*


@Controller
class SettingsController {
    @GetMapping("/settings")
    fun getIndex(model: Model): String {
        val module = "settings"
        model["data"] = "This is the settings page"
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @GetMapping("/settings/scan")
    fun getScan(model: Model): String {
        val module = "scan"
        model["data"] = "This is scan setting"
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RequestMapping(value = ["/settings/scan"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    fun postScan(@RequestParam submit: String): String {
        if (submit == "Scan") {
            return "{\"msg\":\"Scan start\"}"
        }
        return "{\"msg\":\"Error\"}"
    }
//    fun postScan(model: Model): String {
//        val module = "scan"
//        model["data"] = "This is scan POST setting"
//        model["activePage"] = module
//        model["titleDescriptor"] = TextUtils.capitalized(module)
//        return module
//    }
}