package com.miyagi.shashin.controller

import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.KeywordRepository
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping

@Controller
class MapController {

    @Autowired
    private val metadataRepository: MetadataRepository? = null

    @Autowired
    private val keywordRepository: KeywordRepository? = null

    @GetMapping("/map")
    fun getMap(model: Model): String {
        val module = "map"
        model["message"] = ""
        model["mapdata"] = mutableListOf<Metadata>()
        model["keywordMap"] = mutableMapOf<String, String>()
        model["showControls"] = false

        val currentUserObj = model.getAttribute("currentUser") as User?

        // If ROLE_ADMIN get lat lng for timeline
        if (currentUserObj != null) {
            if (currentUserObj.getAuthority() == model.getAttribute("adminRole")) {
                model["mapdata"] = metadataRepository!!.findTimelineAll()
                model["showControls"] = true
            } else {
                model["mapdata"] = metadataRepository!!.findByAlbumMetadataByUserId(currentUserObj.getId())
            }
            val keywordList = keywordRepository!!.findAllKeywordsGroupedByMetadataId()
            val keywordMap = mutableMapOf<String, String>()
            for (keywordGroup in keywordList) {
                keywordMap[keywordGroup.getMetadataId()!!] = keywordGroup.getKeywords()!!
            }
            model["keywordMap"] = keywordMap
        }

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

}