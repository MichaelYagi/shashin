package com.miyagi.shashin.controller

import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.TextUtils
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import javax.servlet.http.HttpServletRequest

@Controller
class IndexController {
    @GetMapping("/features")
    fun getSearch(model: Model, request: HttpServletRequest): String {
        val module = "features"

        model["supportedImageTypes"] = FileUtils.allowableImageFiles().joinToString(", ", transform = { it.uppercase() })
        model["supportedRawTypes"] = FileUtils.allowableRawImageFiles().joinToString(", ", transform = { it.uppercase() })
        model["supportedVideoTypes"] = FileUtils.allowableVideoFiles().joinToString(", ", transform = { it.uppercase() })

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)

        return module
    }
}