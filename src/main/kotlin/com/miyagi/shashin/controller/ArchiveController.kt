package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.repository.KeywordRepository
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.util.ApiResponse
import com.miyagi.shashin.util.TextUtils
import org.apache.commons.text.StringEscapeUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CacheEvict
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import jakarta.transaction.Transactional
import org.springframework.context.MessageSource
import java.util.*

@Controller
@Secured("ROLE_SUPER","ROLE_ADMIN")
class ArchiveController(
    private var metadataRepository: MetadataRepository,
    private val keywordRepository: KeywordRepository? = null,
    var messageSource: MessageSource? = null
) {
    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()

    @GetMapping("/archived")
    fun getFavorites(model: Model, locale: Locale): String {
        val module = "archived"
        model["message"] = messageSource?.getMessage("main.nothing", null, locale)
        model["foldersCount"] = metadataRepository.countByFolder()
        model["metadataList"] = mutableListOf<Metadata>()
        model["keywordMap"] = mutableMapOf<String, String>()

        if (metadataRepository.count() > 0) {
            val trashList = metadataRepository.findAllByHiddenAndOffsetAndLimit(0, model.getAttribute("queryLimit").toString().toInt())

            if (trashList.count() > 0) {
                model["metadataList"] = trashList
                val keywordList = keywordRepository!!.findAllKeywordsGroupedByMetadataId()
                val keywordMap = mutableMapOf<String, String>()
                for (keywordGroup in keywordList) {
                    keywordMap[keywordGroup.getMetadataId()!!] = keywordGroup.getKeywords()!!
                }
                model["keywordMap"] = keywordMap
                model["message"] = ""
            }
        }

        model["msg"] = ""
        model["status"] = ApiResponse.SUCCESS.status
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/archived/metadata/list/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getTrashMetadataList(model: Model,@PathVariable page: Int, locale: Locale): String? {
        val response = mutableMapOf<String, Any?>()
        response["msg"] = messageSource?.getMessage("main.noresults", null, locale)
        response["status"] = ApiResponse.FAIL.status
        response["metadataList"] = ArrayList<Metadata>()

        if (metadataRepository.count() > 0) {
            val size: Int = model.getAttribute("queryLimit") as Int
            val trashList = metadataRepository.findAllByHiddenAndOffsetAndLimit(page*size, size).toMutableList()

            if (trashList.count() > 0) {
                model["metadataList"] = trashList
            }
        }

        return mapper.writeValueAsString(response)
    }

    @RequestMapping(value = ["/archived/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedFavorites(model: Model, @PathVariable page: Int, locale: Locale): String {
        val response = mutableMapOf<String, Any?>()
        response["metadataList"] = mutableListOf<Metadata>()
        response["keywordMap"] = mutableMapOf<String, String>()

        if (page > 0) {
            if (metadataRepository.count() > 0) {
                val trashList = metadataRepository.findAllByHiddenAndOffsetAndLimit((page*model.getAttribute("queryLimit").toString().toInt()), model.getAttribute("queryLimit").toString().toInt())

                if (trashList.count() > 0) {
                    model["metadataList"] = trashList
                    response["metadataList"] = trashList
                    val keywordList = keywordRepository!!.findAllKeywordsGroupedByMetadataId()
                    val keywordMap = mutableMapOf<String, String>()
                    for (keywordGroup in keywordList) {
                        keywordMap[keywordGroup.getMetadataId()!!] = keywordGroup.getKeywords()!!
                    }
                    response["keywordMap"] = keywordMap
                    response["msg"] = ""
                    response["status"] = ApiResponse.SUCCESS.status
                    return mapper.writeValueAsString(response)
                }
            }
        }

        response["msg"] = messageSource?.getMessage("main.noresults", null, locale)
        response["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(response)
    }

    @RequestMapping(value = ["/unarchive"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Transactional
    @CacheEvict(value = ["allMetadata", "allMetadataByDate", "allMetadataByDateAndType", "allMetadataOnlyByDate", "allMetadataAndAttributesByDate", "singleMetadataRequest", "allAlbumMetadataWithCoordinates", "allMetadataWithCoordinates"], allEntries = true)
    fun postUnhideMetadata(model: Model, @RequestBody requestBody: JsonNode, locale: Locale): String {
        val trashMp = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (trashMp.containsKey("metadataIdList")) {
            val metadataIdList = trashMp["metadataIdList"] as MutableList<*>

            if (metadataIdList.count() > 0) {
                for (metadataId in metadataIdList) {
                    val metadataObj = metadataRepository.findById(StringEscapeUtils.escapeHtml4(metadataId as String))
                    if (metadataObj.isPresent) {
                        metadataObj.get().setHidden(false)
                        metadataObj.get().setModifiedAt(TextUtils.getCurrentTimestamp())
                        metadataRepository.save(metadataObj.get())
                    }
                }

                resp["msg"] = messageSource?.getMessage("main.toast.app.media.restored", null, locale)
                resp["status"] = ApiResponse.SUCCESS.status
                return mapper.writeValueAsString(resp)
            }
        }

        resp["msg"] = messageSource?.getMessage("main.toast.app.media.restored.fail", null, locale)
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }
}