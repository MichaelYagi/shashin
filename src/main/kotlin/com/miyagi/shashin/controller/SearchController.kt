package com.miyagi.shashin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.SearchHistory
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.SearchHistoryRepository
import com.miyagi.shashin.repository.SearchRepository
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import javax.servlet.http.HttpServletRequest


@Controller
@Secured("ROLE_ADMIN","ROLE_USER")
class SearchController {

    @Autowired
    private val searchRepository: SearchRepository? = null

    @Autowired
    private val searchHistoryRepository: SearchHistoryRepository? = null

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()

    @GetMapping("/search")
    fun getSearch(model: Model, request: HttpServletRequest): String {
        val module = "search"
        val hasSearchTerm = request.parameterMap.containsKey("searchTerm")
        var searchTerm = ""
        if (hasSearchTerm) {
            searchTerm = request.getParameter("searchTerm").toString()
        }
        val response = buildSearchData(model,searchTerm,0)

        model["searchTerm"] = response["searchTerm"]!!
        model["metadataSearchList"] = response["metadataSearchList"]!!
        model["status"] = response["status"]!!

        model["message"] = ""
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)

        return module
    }

    @RequestMapping(value = ["/search/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedSearch(model: Model, request: HttpServletRequest, @PathVariable page: Int): String {
        val searchTerm = request.getParameter("searchTerm").toString()
        return mapper.writeValueAsString(buildSearchData(model,searchTerm,page))
    }

    private fun buildSearchData(model: Model,searchTerm: String?, page: Int): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()
        response["searchTerm"] = ""
        response["metadataSearchList"] = ""

        if (!searchTerm.isNullOrBlank()) {
            response["searchTerm"] = searchTerm
            val queryLimit = model.getAttribute("queryLimit").toString().toInt()
            val pageValue = page*queryLimit
            if (model.getAttribute("authority").toString() == model.getAttribute("adminRole")) {
                val metadataList = searchRepository?.findMetadataBySearchTerm(searchTerm,pageValue,queryLimit)
                response["metadataSearchList"] = metadataList as MutableIterable<Metadata>
            } else if (model.getAttribute("authority").toString() == model.getAttribute("userRole")) {
                val currentUserObj = model.getAttribute("currentUser") as User?
                if (currentUserObj != null) {
                    val metadataList = searchRepository?.findMetadataBySearchTermAndUserId(searchTerm,currentUserObj.getId(),pageValue,queryLimit)
                    response["metadataSearchList"] = metadataList as MutableIterable<Metadata>
                }
            }
        }

        response["status"] = "success"

        return response
    }

    @RequestMapping(value = ["/search"], method = [RequestMethod.POST], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun postSearch(model: Model, redirectAttributes: RedirectAttributes, @RequestBody formData: MultiValueMap<String, String>): String {
        model["searchTerm"] = ""
        if (formData.containsKey("appSearchInput")) {
            val searchTerm: String = java.lang.String.valueOf(formData.getFirst("appSearchInput"))

            val currentUserObj = model.getAttribute("currentUser") as User?
            if (currentUserObj != null) {
                val searchTermCount = searchHistoryRepository?.countByUserIdAndTermIgnoreCase(currentUserObj.getId(), searchTerm.lowercase())
                if (searchTerm.isNotBlank()) {
                    val searchHistoryCount = searchHistoryRepository?.countByUserId(currentUserObj.getId())
                    if (searchTermCount == 0) {
                        val searchHistory = SearchHistory()
                        searchHistory.setTerm(searchTerm)
                        searchHistory.setUserId(currentUserObj.getId())
                        searchHistory.setCreatedAt(TextUtils.getCurrentTimestamp())
                        searchHistory.setModifiedAt(TextUtils.getCurrentTimestamp())
                        searchHistoryRepository?.save(searchHistory)
                    }

                    val searchHistoryLimit = model.getAttribute("searchHistoryLimit").toString().toInt()
                    if (searchHistoryCount != null && searchHistoryCount > searchHistoryLimit) {
                        val searchHistory = searchHistoryRepository?.findTopNByUserIdOrderByIdDesc(currentUserObj.getId(), 1)
                        if (searchHistory != null && searchHistory.count() > 0) {
                            searchHistoryRepository?.deleteById(searchHistory.last().getId())
                        }
                    }
                }
            }

            redirectAttributes.addAttribute("searchTerm", searchTerm)
        }

        val module = "search"
        model["message"] = ""
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return "redirect:/"+module
    }

    @RequestMapping(value = ["/api/v1/search/history"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getSearchHistory(model: Model, request: HttpServletRequest): String {
        val response = mutableMapOf<String, Any?>()
        response["searchHistoryList"] = ""
        response["msg"] = "Not authorized"
        response["status"] = "fail"

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            response["msg"] = "Success!"
            response["status"] = "success"

            val searchHistoryLimit = model.getAttribute("searchHistoryLimit").toString().toInt()
            val searchHistoryList =
                searchHistoryRepository?.findTopNByUserIdOrderByCreatedAtDesc(currentUserObj.getId(), searchHistoryLimit)
            if (searchHistoryList != null) {
                response["searchHistoryList"] = searchHistoryList
            }
        }
        return mapper.writeValueAsString(response)
    }
}