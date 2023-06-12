package com.miyagi.shashin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.KeywordRepository
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.util.ApiResponse
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import java.util.concurrent.TimeUnit

@Controller
class MapController {

    @Autowired
    private val metadataRepository: MetadataRepository? = null

    val mapper = ObjectMapper()

    @GetMapping("/map")
    fun getMap(model: Model): String {
        val module = "map"
        model["message"] = ""
        model["showControls"] = false

        val currentUserObj = model.getAttribute("currentUser") as User?

        // If ROLE_ADMIN get lat lng for timeline
        if (currentUserObj != null) {
            if (currentUserObj.getAuthority() == model.getAttribute("adminRole")) {
                model["showControls"] = true
            }
        }

        model["msg"] = ""
        model["status"] = ApiResponse.SUCCESS.status
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @Secured("ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["api/v1/mapdata"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getMapDataAdmin(model: Model): ResponseEntity<String> {
        val response = mutableMapOf<String, Any?>()
        val currentUserObj = model.getAttribute("currentUser") as User?
        response["mapdata"] = mutableListOf<Metadata>()
        response["msg"] = "Not logged in"
        response["status"] = ApiResponse.SUCCESS.status

        // If ROLE_ADMIN get lat lng for timeline
        if (currentUserObj != null) {
            if (currentUserObj.getAuthority() == model.getAttribute("adminRole")) {
                response["mapdata"] = metadataRepository!!.findTimelineAllWithCoordinates()
            } else {
                response["mapdata"] = metadataRepository!!.findByAlbumMetadataByUserIdWithCoordinates(currentUserObj.getId())
            }

            response["msg"] = ""
            response["status"] = ApiResponse.SUCCESS.status
        }

        val json = mapper.writeValueAsString(response)
        return ResponseEntity
            .ok()
//            .eTag(UUID.nameUUIDFromBytes(json.toByteArray()).toString())
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
            .body(json)
    }
}