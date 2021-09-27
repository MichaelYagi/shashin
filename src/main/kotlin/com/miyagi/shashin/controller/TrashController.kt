package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.Favorite
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.ArrayList
import javax.transaction.Transactional

@Controller
@Secured("ROLE_ADMIN")
class TrashController {

    @Autowired
    private lateinit var metadataRepository: MetadataRepository

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()

    @GetMapping("/trash")
    fun getFavorites(model: Model): String {
        val module = "trash"
        model["message"] = "There are nothing trashed."
        model["metadataList"] = ""

        val trashList = metadataRepository.findAllByHiddenAndOffsetAndLimit(0, model.getAttribute("queryLimit").toString().toInt())
        if (trashList.count() > 0) {
            model["metadataList"] = trashList
            model["message"] = ""
        }

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RequestMapping(value = ["/trash/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedFavorites(model: Model, @PathVariable page: Int): String {
        val response = mutableMapOf<String, Any?>()
        response["metadataList"] = ""

        if (page > 0) {
            val trashList = metadataRepository.findAllByHiddenAndOffsetAndLimit((page*model.getAttribute("queryLimit").toString().toInt()), model.getAttribute("queryLimit").toString().toInt())
            if (trashList.count() > 0) {
                response["metadataList"] = trashList
                response["msg"] = ""
                response["status"] = "success"
                return mapper.writeValueAsString(response)
            }
        }

        response["msg"] = "Could not get results"
        response["status"] = "fail"
        return mapper.writeValueAsString(response)
    }

    @RequestMapping(value = ["/trash/unhide"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun postUnhideMetadata(model: Model, @RequestBody requestBody: JsonNode): String {
        val trashMp = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (trashMp.containsKey("metadataIdList")) {
            val metadataIdList = trashMp["metadataIdList"] as MutableList<String>

            if (metadataIdList.count() > 0) {
                for (metadataId in metadataIdList) {
                    val metadataObj = metadataRepository.findById(metadataId)
                    metadataObj.get().setHidden(false)
                    metadataRepository.save(metadataObj.get())
                }

                resp["msg"] = "Untrashed photos"
                resp["status"] = "success"
                return mapper.writeValueAsString(resp)
            }
        }

        resp["msg"] = "Could not untrash"
        resp["status"] = "fail"
        return mapper.writeValueAsString(resp)
    }
}