package com.miyagi.shashin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.SearchHistory
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.FavoriteRepository
import com.miyagi.shashin.repository.KeywordRepository
import com.miyagi.shashin.repository.SearchHistoryRepository
import com.miyagi.shashin.repository.SearchRepository
import com.miyagi.shashin.util.ApiResponse
import com.miyagi.shashin.util.TextUtils
import io.swagger.v3.oas.annotations.Operation
import org.apache.commons.text.StringEscapeUtils
import org.springdoc.core.annotations.RouterOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.util.*
import javax.servlet.http.HttpServletRequest


@Controller
@Secured("ROLE_SUPER","ROLE_ADMIN","ROLE_USER")
class SearchController: BaseController() {

    @Autowired
    private val searchRepository: SearchRepository? = null

    @Autowired
    private val searchHistoryRepository: SearchHistoryRepository? = null

    @Autowired
    private val keywordRepository: KeywordRepository? = null

    @Autowired
    private val favoriteRepository: FavoriteRepository? = null

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()

    @GetMapping("/search")
    fun getSearch(model: Model, request: HttpServletRequest): String {
        val module = "search"
        val hasSearchTerm = request.parameterMap.containsKey("term")
        var term = ""
        if (hasSearchTerm) {
            term = request.getParameter("term").toString()
        }
        val response = buildSearchData(model,term,0)

        getAllAttributeData(model)

        model["term"] = response["term"]!!
        model["favorites"] = response["favorites"]!!
        model["metadataSearchList"] = response["metadataSearchList"]!!
        model["status"] = response["status"]!!

        model["msg"] = ""
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = term

        return module
    }

    @RequestMapping(value = ["/search/{page}"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun getPagedSearch(model: Model, request: HttpServletRequest, @PathVariable page: Int): String {
        val hasSearchTerm = request.parameterMap.containsKey("term")
        var term = ""
        if (hasSearchTerm) {
            term = request.getParameter("term").toString()
        }
        return mapper.writeValueAsString(buildSearchData(model,term,page))
    }

    private fun buildSearchData(model: Model,term: String?, page: Int): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()
        response["term"] = ""
        response["metadataSearchList"] = mutableListOf<Metadata>()
        response["keywordMap"] = mutableMapOf<String, String>()
        response["favorites"] = mutableMapOf<String, String>()
        val currentUserObj = model.getAttribute("currentUser") as User?

        if (!term.isNullOrBlank()) {
            response["term"] = term

            val favoritesMap = HashMap<String, HashMap<String, Any>>()

            val queryLimit = model.getAttribute("queryLimit").toString().toInt()
            val pageValue = page*queryLimit
            var metadataList: MutableIterable<Metadata>? = null
            if (model.getAttribute("authority").toString() == model.getAttribute("adminRole") || model.getAttribute("authority").toString() == model.getAttribute("superRole")) {
                metadataList = searchRepository?.findMetadataBySearchTerm(term,pageValue,queryLimit)
                response["metadataSearchList"] = metadataList as MutableIterable<Metadata>
            } else if (model.getAttribute("authority").toString() == model.getAttribute("userRole")) {
                if (currentUserObj != null) {
                    metadataList = searchRepository?.findMetadataBySearchTermAndUserId(term,currentUserObj.getId(),pageValue,queryLimit)
                    response["metadataSearchList"] = metadataList as MutableIterable<Metadata>
                }
            }

            if (metadataList != null) {
                for (metadata in metadataList) {
                    val favorites = favoriteRepository!!.findAllByMetadataId(metadata.getId())
                    if (favorites != null) {
                        for (favorite in favorites) {
                            if (favorite != null) {
                                val favCount = favoriteRepository.countAllByMetadataId(metadata.getId())

                                favoritesMap[metadata.getId()] = hashMapOf(
                                    "favorite" to (favorite.getUserId() == currentUserObj?.getId()),
                                    "count" to favCount
                                )

                                if (favorite.getUserId() == currentUserObj?.getId()) {
                                    break
                                }
                            }
                        }
                    }
                }
            }

            val keywordList = keywordRepository!!.findAllKeywordsGroupedByMetadataId()
            val keywordMap = mutableMapOf<String, String>()
            for (keywordGroup in keywordList) {
                keywordMap[keywordGroup.getMetadataId()!!] = keywordGroup.getKeywords()!!
            }
            response["keywordMap"] = keywordMap
            response["favorites"] = favoritesMap
        }

        response["status"] = ApiResponse.SUCCESS.status
        response["msg"] = ""

        return response
    }

    @RequestMapping(value = ["/search/metadata/list/{page}/{term}"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun getSearchMetadataList(model: Model,@PathVariable page: Int,@PathVariable term: String?): String? {
        val response = mutableMapOf<String, Any?>()
        response["msg"] = "No Results"
        response["status"] = ApiResponse.FAIL.status
        response["metadataSearchList"] = ArrayList<Metadata>()

        if (!term.isNullOrBlank()) {
            val queryLimit = model.getAttribute("queryLimit").toString().toInt()
            val pageValue = page * queryLimit
            if (model.getAttribute("authority").toString() == model.getAttribute("adminRole") || model.getAttribute("authority").toString() == model.getAttribute("superRole")) {
                val metadataList = searchRepository?.findMetadataBySearchTerm(term, pageValue, queryLimit)
                response["metadataSearchList"] = metadataList as MutableIterable<Metadata>
                response["msg"] = "Results"
                response["status"] = ApiResponse.SUCCESS.status
            } else if (model.getAttribute("authority").toString() == model.getAttribute("userRole")) {
                val currentUserObj = model.getAttribute("currentUser") as User?
                if (currentUserObj != null) {
                    val metadataList = searchRepository?.findMetadataBySearchTermAndUserId(
                        term,
                        currentUserObj.getId(),
                        pageValue,
                        queryLimit
                    )
                    response["metadataSearchList"] = metadataList as MutableIterable<Metadata>
                    response["msg"] = "Results"
                    response["status"] = ApiResponse.SUCCESS.status
                }
            }
        } else {
            response["msg"] = "No Results"
            response["status"] = ApiResponse.SUCCESS.status
        }

        return mapper.writeValueAsString(response)
    }

    @RequestMapping(value = ["/search"], method = [RequestMethod.POST], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun postSearch(model: Model, redirectAttributes: RedirectAttributes, @RequestBody formData: MultiValueMap<String, String>): String {
        model["term"] = ""
        if (formData.containsKey("appSearchInput")) {
            val term: String = java.lang.String.valueOf(formData.getFirst("appSearchInput"))

            val currentUserObj = model.getAttribute("currentUser") as User?
            if (currentUserObj != null) {
                val searchTermCount = searchHistoryRepository?.countByUserIdAndTermIgnoreCase(currentUserObj.getId(), term.lowercase())
                if (term.isNotBlank()) {
                    val searchHistoryCount = searchHistoryRepository?.countByUserId(currentUserObj.getId())
                    val searchHistory: SearchHistory?
                    if (searchTermCount == 0) {
                        searchHistory = SearchHistory()
                        searchHistory.setTerm(term)
                        searchHistory.setUserId(currentUserObj.getId())
                        searchHistory.setCreatedAt(TextUtils.getCurrentTimestamp())
                        searchHistory.setModifiedAt(TextUtils.getCurrentTimestamp())
                    } else {
                        searchHistory =
                            searchHistoryRepository?.findDistinctByUserIdAndTerm(currentUserObj.getId(), term)
                        searchHistory?.setModifiedAt(TextUtils.getCurrentTimestamp())
                    }

                    if (searchHistory != null) {
                        searchHistoryRepository?.save(searchHistory)
                    }

                    val searchHistoryLimit = model.getAttribute("searchHistoryLimit").toString().toInt()
                    if (searchHistoryCount != null && searchHistoryCount > searchHistoryLimit) {
                        val searchHistoryRefresh = searchHistoryRepository?.findTopNByUserIdOrderByIdDesc(currentUserObj.getId(), 1)
                        if (searchHistoryRefresh != null && searchHistoryRefresh.count() > 0) {
                            searchHistoryRepository?.deleteById(searchHistoryRefresh.last().getId())
                        }
                    }

                    redirectAttributes.addAttribute("term", term)
                }
            }
        }

        val module = "search"
        model["msg"] = ""
        model["status"] = ApiResponse.SUCCESS.status
        model["message"] = ""
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return "redirect:/"+module
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getSearchHistory",
            description = "<strong>Get your search history results.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/search/history?size={size}\" \\\n" +
                    "-H \"Content-Type: application/json\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>Content-Type</td><td>header</td><td>string</td><td>required</td><td>application/json</td></tr>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>size</td><td>param</td><td>int</td><td>optional</td><td>The default query/page size is 20. Admins can set the default query/page size in the <a href=\"/settings\">settings</a></td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"msg\": \"\",\n" +
                    "    \"message\": \"\",\n" +
                    "    \"status\": \"success\",\n" +
                    "    \"searchHistoryList\": [\n" +
                    "        {\n" +
                    "            \"id\": &lt;search_history_id&gt;,\n" +
                    "            \"term\": \"&lt;search_term&gt;\",\n" +
                    "            \"userId\": &lt;user_id&gt;\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>searchHistoryList[].id</td><td>int</td><td>The search history term ID</td></tr>" +
                    "<tr><td>searchHistoryList[].term</td><td>string</td><td>The search history term</td></tr>" +
                    "<tr><td>searchHistoryList[].userId</td><td>int</td><td>The user that searched this search history term</td></tr>" +
                    "</tbody></table>"
        )
    )
    @RequestMapping(value = ["/api/v1/search/history","/search/history"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun getSearchHistory(model: Model, request: HttpServletRequest, @RequestParam size: Optional<Int>): String {
        val searchHistoryLimit = size.orElse(model.getAttribute("searchHistoryLimit").toString().toInt())
        val response = mutableMapOf<String, Any?>()
        response["searchHistoryList"] = mutableListOf<SearchHistory>()
        response["msg"] = "Not authorized"
        response["status"] = ApiResponse.FAIL.status

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            response["msg"] = "Success!"
            response["status"] = ApiResponse.SUCCESS.status

            val searchHistoryList =
                searchHistoryRepository?.findTopNByUserIdOrderByCreatedAtDesc(currentUserObj.getId(), searchHistoryLimit)
            if (searchHistoryList != null) {
                response["searchHistoryList"] = searchHistoryList
            }
        }
        return mapper.writeValueAsString(response)
    }
}