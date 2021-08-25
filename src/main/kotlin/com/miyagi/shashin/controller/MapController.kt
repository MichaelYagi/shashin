package com.miyagi.shashin.controller

import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping

@Controller
class MapController {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private val metadataRepository: MetadataRepository? = null

    @GetMapping("/map")
    fun getMap(model: Model): String {
        val module = "map"
        model["data"] = ""
        model["mapdata"] = ""

        val currentUserObj = model.getAttribute("currentUser") as User?

        // If ADMIN get lat lng for timeline
        if (currentUserObj != null) {
            if (currentUserObj.getAuthority() == model.getAttribute("adminRole")) {
                model["mapdata"] = metadataRepository!!.findAll()
            } else {
                model["mapdata"] = metadataRepository!!.findByAlbumMetadataByUserId(currentUserObj.getId())
            }
        }

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

}