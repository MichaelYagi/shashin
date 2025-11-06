package com.miyagi.shashin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.SearchHistory
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.AlbumRepository
import com.miyagi.shashin.repository.FavoriteRepository
import com.miyagi.shashin.repository.KeywordRepository
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.repository.RecognitionLabelRepository
import com.miyagi.shashin.repository.SearchHistoryRepository
import com.miyagi.shashin.repository.SearchRepository
import com.miyagi.shashin.util.ApiResponse
import com.miyagi.shashin.util.SearchHistoryTypes
import com.miyagi.shashin.util.TextUtils
import io.swagger.v3.oas.annotations.Operation
import org.springdoc.core.annotations.RouterOperation
import org.springframework.http.MediaType
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.util.*
import jakarta.servlet.http.HttpServletRequest
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.math.ceil


@Controller
@Secured("ROLE_SUPER","ROLE_ADMIN","ROLE_USER")
class SearchController(
    private val searchRepository: SearchRepository,
    private var metadataRepository: MetadataRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val keywordRepository: KeywordRepository,
    private val favoriteRepository: FavoriteRepository,
    recognitionLabelRepository: RecognitionLabelRepository,
    albumRepository: AlbumRepository
): BaseController(
    recognitionLabelRepository = recognitionLabelRepository,
    albumRepository = albumRepository,
    keywordRepository = keywordRepository,
    metadataRepository = metadataRepository
) {
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

        model["pageParam"] = 0
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

    @RequestMapping(value = ["/search/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedSearch(model: Model, request: HttpServletRequest, @PathVariable page: Int): String {
        val hasSearchTerm = request.parameterMap.containsKey("term")
        var term = ""
        if (hasSearchTerm) {
            term = request.getParameter("term").toString()
        }
        return mapper.writeValueAsString(buildSearchData(model,term,page))
    }

    @GetMapping("/search/{page}/term/{term}")
    fun getPaginationSearch(model: Model, request: HttpServletRequest, @PathVariable page: Int, @PathVariable term: String): String {
        val module = "search"

        val response = buildSearchData(model,term,page)
        for ((k, v) in response) {
            model[k] = v!!
        }

        model["currentPage"] = (page+1)
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    private fun buildSearchData(model: Model,term: String?, page: Int): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()
        response["term"] = ""
        response["metadataSearchList"] = mutableListOf<Metadata>()
        response["keywordMap"] = mutableMapOf<String, String>()
        response["favorites"] = mutableMapOf<String, String>()
        response["page"] = page
        val size = model.getAttribute("queryLimit").toString().toInt()
        response["size"] = size
        response["totalPages"] = 0
        val currentUserObj = model.getAttribute("currentUser") as User?

        if (!term.isNullOrBlank()) {
            response["term"] = term

            var updatedTerm = term

            // If a date is detected, reformat to yyyy-mm-dd
            val possibleDate = TextUtils.convertDateToYMD(term)
            if (possibleDate != null) {
                updatedTerm = possibleDate
            }

            val favoritesMap = HashMap<String, HashMap<String, Any>>()

            val queryLimit = model.getAttribute("queryLimit").toString().toInt()
            val pageValue = page*queryLimit
            var metadataList: MutableIterable<Metadata>? = null

            if (model.getAttribute("authority").toString() == model.getAttribute("adminRole") || model.getAttribute(
                    "authority"
                ).toString() == model.getAttribute("superRole")
            ) {
                metadataList = if (updatedTerm.lowercase() == "shashinedit" || updatedTerm.lowercase() == "shashinedited") {
                    searchRepository?.findMetadataEditedPhotos(pageValue, queryLimit)
                } else if (updatedTerm.lowercase() == "nolatlng") {
                    metadataRepository.findAllMissingCoordOffsetAndLimit(
                        pageValue,
                        queryLimit
                    )
                } else if (updatedTerm.lowercase() == "latlng") {
                    metadataRepository.findAllWithCoordOffsetAndLimit(
                        pageValue,
                        queryLimit
                    )
                } else if (updatedTerm.lowercase() == "description") {
                    metadataRepository.findAllDescriptionOffsetAndLimit(
                        pageValue,
                        queryLimit
                    )
                } else {
                    searchRepository?.findMetadataBySearchTerm(updatedTerm, pageValue, queryLimit)
                }
                response["metadataSearchList"] = metadataList as MutableIterable<Metadata>
                response["totalPages"] = ceil((searchRepository?.countAllByHiddenIsFalse(updatedTerm)!!.toDouble()) / size.toDouble()).toInt()
            } else if (model.getAttribute("authority").toString() == model.getAttribute("userRole")) {
                if (currentUserObj != null) {
                    if (updatedTerm.lowercase() == "shashinedit" || updatedTerm.lowercase() == "shashinedited") {
                        metadataList = searchRepository?.findMetadataEditedPhotosAndUserId(currentUserObj.getId(), pageValue, queryLimit)
                    } else if (updatedTerm.lowercase() == "nolatlng") {
                        metadataRepository.findAllMissingCoordAndUserIdOffsetAndLimit(
                            currentUserObj.getId(),
                            pageValue,
                            queryLimit
                        )
                    } else if (updatedTerm.lowercase() == "latlng") {
                        metadataRepository.findAllWithCoordAndUserIdOffsetAndLimit(
                            currentUserObj.getId(),
                            pageValue,
                            queryLimit
                        )
                    } else if (updatedTerm.lowercase() == "description") {
                        metadataRepository.findAllDescriptionAndUserIdOffsetAndLimit(
                            currentUserObj.getId(),
                            pageValue,
                            queryLimit
                        )
                    } else {
                        metadataList = searchRepository?.findMetadataBySearchTermAndUserId(
                            updatedTerm,
                            currentUserObj.getId(),
                            pageValue,
                            queryLimit
                        )
                    }

                    response["metadataSearchList"] = metadataList as MutableIterable<Metadata>
                    response["totalPages"] = ceil((searchRepository?.countAllByHiddenIsFalseAndUserId(updatedTerm, currentUserObj.getId())!!.toDouble()) / size.toDouble()).toInt()
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

    @RequestMapping(value = ["/search/metadata/list/{page}/{term}"], method = [RequestMethod.GET], produces = ["application/json"])
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
                    var updatedTerm = term

                    // If a date is detected, reformat to yyyy-mm-dd
                    val possibleDate = TextUtils.convertDateToYMD(term)
                    if (possibleDate != null) {
                        updatedTerm = possibleDate
                    }

                    val metadataList = searchRepository?.findMetadataBySearchTermAndUserId(
                        updatedTerm,
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
    @Transactional
    fun postSearch(model: Model, redirectAttributes: RedirectAttributes, request: HttpServletRequest): String {
        model["term"] = ""
        if (request.getParameter("appSearchInput") != null) {
            var term: String = java.lang.String.valueOf(request.getParameter("appSearchInput"))
            val originalTerm = term

            val currentUserObj = model.getAttribute("currentUser") as User?
            if (currentUserObj != null) {
                val searchTermCount = searchHistoryRepository?.countByUserIdAndTermIgnoreCase(
                    currentUserObj.getId(),
                    term.lowercase(),
                    SearchHistoryTypes.AppHistorySearch.type
                )!!

                if (term.isNotBlank()) {

                    // If a date is detected, reformat to yyyy-mm-dd
                    val possibleDate = TextUtils.convertDateToYMD(term)
                    if (possibleDate != null) {
                        term = possibleDate
                    }

                    val searchHistory: SearchHistory?
                    if (searchTermCount == 0) {
                        searchHistory = SearchHistory()
                        searchHistory.setTerm(term)
                        searchHistory.setSearchType(SearchHistoryTypes.AppHistorySearch.type)
                        searchHistory.setUserId(currentUserObj.getId())
                        searchHistory.setCreatedAt(TextUtils.getCurrentTimestamp())
                        searchHistory.setModifiedAt(TextUtils.getCurrentTimestamp())
                    } else {
                        searchHistory =
                            searchHistoryRepository.findDistinctByUserIdAndTerm(
                                currentUserObj.getId(),
                                term,
                                SearchHistoryTypes.AppHistorySearch.type
                            )
                        searchHistory?.setModifiedAt(TextUtils.getCurrentTimestamp())
                    }

                    if (searchHistory != null) {

                        searchHistoryRepository.save(searchHistory)
                    }

                    val searchHistoryCount = searchHistoryRepository.countByUserId(
                        currentUserObj.getId(),
                        SearchHistoryTypes.AppHistorySearch.type
                    )
                    val searchHistoryLimit = model.getAttribute("searchHistoryLimit").toString().toInt()
                    if (searchHistoryCount > searchHistoryLimit) {
                        val searchHistoryRefresh = searchHistoryRepository.findTopNByUserIdOrderByIdAsc(
                            currentUserObj.getId(),
                            1,
                            SearchHistoryTypes.AppHistorySearch.type
                        )

                        if (searchHistoryRefresh != null && searchHistoryRefresh.count() > 0) {
                            searchHistoryRepository.deleteByIdAndSearchType(
                                searchHistoryRefresh.last().getId(),
                                SearchHistoryTypes.AppHistorySearch.type
                            )
                        }
                    }

                    redirectAttributes.addAttribute("term", originalTerm)
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
        return "redirect:/$module"
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getSearchHistory",
            summary = "Get your search history results.",
            description = "<strong>Get your search history results.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/search/history?size={size}\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
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
    @RequestMapping(value = ["/api/v1/search/history","/search/history"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getSearchHistory(model: Model, request: HttpServletRequest, @RequestParam size: Optional<Int>): String {
        val searchHistoryLimit = size.orElse(model.getAttribute("searchHistoryLimit").toString().toInt())
        val response = mutableMapOf<String, Any?>()
        response["searchHistoryList"] = mutableListOf<SearchHistory>()
        response["msg"] = "Not authorized"
        response["status"] = ApiResponse.FAIL.status
        response["size"] = searchHistoryLimit

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            response["msg"] = "Success!"
            response["status"] = ApiResponse.SUCCESS.status

            val searchHistoryList =
                searchHistoryRepository?.findTopNByUserIdOrderByModifiedAtDesc(currentUserObj.getId(), searchHistoryLimit, SearchHistoryTypes.AppHistorySearch.type)
            if (searchHistoryList != null) {
                response["searchHistoryList"] = searchHistoryList
            }
        }
        return mapper.writeValueAsString(response)
    }
}