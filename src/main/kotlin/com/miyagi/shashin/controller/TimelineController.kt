package com.miyagi.shashin.controller

import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping

@Controller
class TimelineController {

    @Autowired
    private val metadataRepository: MetadataRepository? = null

    @GetMapping("/timeline")
    fun getTimeline(model: Model): String {
        val module = "timeline"

        model["metadataList"] = ""
        val metadataList = metadataRepository?.findAll(Sort.by(Sort.Direction.DESC, "takenAt"))
        if (metadataList != null) {
            model["metadataList"] = metadataList
        }

        model["data"] = ""
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }
}