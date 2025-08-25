package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.Favorite
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.Notification
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.ApiResponse
import com.miyagi.shashin.util.TextUtils
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import io.swagger.v3.oas.annotations.Operation
import org.apache.commons.text.StringEscapeUtils
import org.springdoc.core.annotations.RouterOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CacheEvict
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.*
import jakarta.transaction.Transactional
import org.springframework.context.MessageSource
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.math.ceil
import kotlin.math.floor

@Suppress("UNCHECKED_CAST")
@Controller
@Secured("ROLE_SUPER","ROLE_ADMIN","ROLE_USER")
class FavoritesController: BaseController() {

    @Autowired
    private lateinit var favoriteRepository: FavoriteRepository

    @Autowired
    private lateinit var metadataRepository: MetadataRepository

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private val keywordRepository: KeywordRepository? = null

    @Autowired
    var messageSource: MessageSource? = null

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, Any?>()

    @RequestMapping(value = ["/favorites", "/favorites/{mediaType}"], method = [RequestMethod.GET])
    fun getFavorites(model: Model,@PathVariable(required = false) mediaType: String?, locale: Locale): String {
        val module = "favorites"
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        model["pageParam"] = 0

        val response = buildFavorites(model,0,model.getAttribute("queryLimit").toString().toInt(),mediaType, locale)
        for ((k, v) in response) {
            model[k] = v!!
        }

        getAllAttributeData(model)

        return model.getAttribute("activePage").toString()
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getPagedFavorites",
            summary = "Get paged results used for your favorites.",
            description = "<strong>Get paged results used for your favorites.</strong>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/favorites/{page}\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>page</td><td>param</td><td>int</td><td>required</td><td>page number of results to return used for pagination. Page index starts from 0. The default query/page size is 20. Admins can set the query/page size in the <a href=\"/settings\">settings</a></td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"msg\": \"\",\n" +
                    "    \"status\": \"success\",\n" +
                    "    \"metadataList\": [\n" +
                    "        {\n" +
                    "           &lt;metadata&gt;\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"keywordMap\": {\n" +
                    "        {\n" +
                    "           \"&lt;metadata_id&gt;\": \"&lt;keyword_1,keyword_n&gt;\"\n" +
                    "        }\n" +
                    "    }\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>metadataList[].metadata</td><td>object</td><td>A <a href=\"#\" data-bs-toggle=\"modal\" data-bs-target=\"#propMetadataDocs\">Metadata</a> object</td></tr>" +
                    "<tr><td>keywordMap.&lt;metadata_id&gt;</td><td>string</td><td>A comma seperated list of keywords for the associated metadata ID</td></tr>" +
                    "</tbody></table>"
        )
    )
    @RequestMapping(value = ["/favorites/{page}","/favorites/mediatype/{mediaType}/page/{page}","/api/v1/favorites/{page}","/api/v1/favorites/mediatype/{mediaType}/page/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedFavorites(model: Model, @PathVariable page: Int, @RequestParam size: Optional<Int>,@PathVariable(required = false) mediaType: String?, locale: Locale): String {
        return mapper.writeValueAsString(buildFavorites(model,page,size.orElse(model.getAttribute("queryLimit").toString().toInt()),mediaType,locale))
    }

    @RequestMapping(value = ["/favorites/{page}/{mediaType}"], method = [RequestMethod.GET])
    fun getFavoritesPage(model: Model,@PathVariable(required = true) page: Int,@PathVariable(required = true) mediaType: String, locale: Locale): String {
        val module = "favorites"

        val response = buildFavorites(model,page,model.getAttribute("queryLimit").toString().toInt(),mediaType, locale)
        for ((k, v) in response) {
            model[k] = v!!
        }

        getAllAttributeData(model)

        model["currentPage"] = (page+1)
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    private fun buildFavorites(model: Model, page: Int = 0, size: Int = model.getAttribute("queryLimit").toString().toInt(), mediaTypeFilter: String?, locale: Locale): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        response["message"] = messageSource?.getMessage("main.nothing", null, locale)
        response["metadataList"] = mutableListOf<Metadata>()
        response["mediaTypeFilter"] = "all"
        response["keywordMap"] = mutableMapOf<String, String>()
        response["formattedDateMap"] = mutableMapOf<String, String>()
        response["page"] = page
        response["size"] = size
        response["totalPages"] = 0

        var mediaType = mediaTypeFilter

        if (mediaTypeFilter.isNullOrEmpty()) {
            mediaType = "all"
        }

        response["mediaTypeFilter"] = mediaType

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            val favoriteList = if (mediaType == "all") {
                response["totalPages"] = floor((favoriteRepository.countAllByUserId(currentUserObj.getId())!!.toDouble()) / size.toDouble()).toInt()

                favoriteRepository.findAllByUserIdAndOffsetAndLimit(currentUserObj.getId(), (page * size), size)
            } else if (mediaType == "nolatlng") {
                response["totalPages"] = floor((favoriteRepository.countAllByUserIdAndNoCoord(currentUserObj.getId())!!.toDouble()) / size.toDouble()).toInt()

                favoriteRepository.findAllByUserIdAndNoCoordAndOffsetAndLimit(
                    currentUserObj.getId(),
                    (page * size),
                    size
                )
            } else if (mediaType == "description") {
                response["totalPages"] = floor((favoriteRepository.countAllByUserIdAndDescription(currentUserObj.getId())!!.toDouble()) / size.toDouble()).toInt()

                favoriteRepository.findAllByUserIdAndDescriptionAndOffsetAndLimit(
                    currentUserObj.getId(),
                    (page * size),
                    size
                )
            } else {
                response["totalPages"] = floor((favoriteRepository.countAllByUserIdAndMediaType(currentUserObj.getId(), mediaType)!!.toDouble()) / size.toDouble()).toInt()

                favoriteRepository.findAllByUserIdAndMediaTypeAndOffsetAndLimit(
                    currentUserObj.getId(),
                    mediaType,
                    (page * size),
                    size
                )
            }

            if (favoriteList != null && favoriteList.count() > 0) {
                val metadataList = ArrayList<Metadata>()
                val formattedDateMap = HashMap<String, String>()
                model["message"] = ""
                for (favorite in favoriteList) {
                    if (favorite != null) {
                        val metadataObj = metadataRepository.findById(favorite.getMetadataId().toString())
                        metadataList.add(metadataObj.get())
                        val date = metadataObj.get().getYear().toString() + "-" + metadataObj.get().getMonth().toString() + "-" + metadataObj.get().getDay().toString()
                        formattedDateMap[metadataObj.get().getId().toString()] = TextUtils.formatToLongDate(date, model.getAttribute("locale").toString()).toString()
                    }
                }
                if (metadataList.count() > 0) {
                    response["metadataList"] = metadataList
                }

                val keywordMap = mutableMapOf<String, String>()
                val keywordList = keywordRepository!!.findAllKeywordsGroupedByMetadataId()
                for (keywordGroup in keywordList) {
                    keywordMap[keywordGroup.getMetadataId()!!] = keywordGroup.getKeywords()!!
                }

                response["keywordMap"] = keywordMap
                response["formattedDateMap"] = formattedDateMap
                response["message"] = ""
                response["msg"] = messageSource?.getMessage("main.results", null, locale)
                response["status"] = ApiResponse.SUCCESS.status
                return response
            }
        }

        return response
    }


    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/favorites/metadata/list/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getFavoritesMetadataList(model: Model,@PathVariable page: Int, locale: Locale): String? {
        val response = mutableMapOf<String, Any?>()
        response["msg"] = messageSource?.getMessage("main.noresults", null, locale)
        response["status"] = ApiResponse.FAIL.status
        response["metadataList"] = ArrayList<Metadata>()
        val size: Int = model.getAttribute("queryLimit") as Int

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            val favoriteList =
                favoriteRepository.findAllByUserIdAndOffsetAndLimit(currentUserObj.getId(), (page * size), size)
            if (favoriteList != null && favoriteList.count() > 0) {
                val metadataList = ArrayList<Metadata>()
                model["message"] = ""
                for (favorite in favoriteList) {
                    if (favorite != null) {
                        val metadataObj = metadataRepository.findById(favorite.getMetadataId().toString())
                        metadataList.add(metadataObj.get())
                    }
                }
                if (metadataList.count() > 0) {
                    response["metadataList"] = metadataList
                }
            }
        }

        return mapper.writeValueAsString(response)
    }

    @RequestMapping(value = ["/favorite/save"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @CacheEvict(value = ["allMetadata", "allMetadataByDate", "allMetadataByDateAndType", "allMetadataOnlyByDate", "allMetadataAndAttributesByDate", "singleMetadataRequest", "allAlbumMetadataWithCoordinates", "allMetadataWithCoordinates"], allEntries = true)
    @ResponseBody
    fun postSaveFavorite(model: Model, @RequestBody requestBody: JsonNode, locale: Locale): String {
        val favoritesMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (favoritesMap.containsKey("metadataId") && favoritesMap.containsKey("isFavorite")) {
            val metadataId = favoritesMap["metadataId"].toString()
            val isFavorite = favoritesMap["isFavorite"].toString().toBoolean()

            val currentUserObj = model.getAttribute("currentUser") as User?
            val favorite = Favorite()
            if (currentUserObj != null) {
                favorite.setUserId(currentUserObj.getId())
                favorite.setMetadataId(metadataId)
                favorite.setModifiedAt(getCurrentTimestamp())
                val favoriteObj = favoriteRepository.findByMetadataIdAndUserId(metadataId,currentUserObj.getId())
                if (favoriteObj != null) {
                    val favoriteId = favoriteObj.getId()
                    favorite.setId(favoriteId)
                    favorite.setModifiedAt(getCurrentTimestamp())

                    if (isFavorite) {
                        favoriteRepository.save(favorite)
                    } else {
                        favoriteRepository.deleteByMetadataIdAndUserId(metadataId,currentUserObj.getId())
                    }
                } else if (isFavorite) {
                    favorite.setCreatedAt(getCurrentTimestamp())
                    favoriteRepository.save(favorite)
                }

                // Notify admins
                val admins = userRepository.findAllAdmins()

                val metadata = metadataRepository.findById(metadataId)

                val notificationObjList = mutableListOf<Notification>()
                val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
                for (admin in admins) {
                    if (admin.getId() != currentUserObj.getId()) {
                        var language = admin.getLanguage()
                        if (language == null) {
                            language = "en"
                        }

                        var locale = Locale(language)
                        val notificationObj = Notification()
                        notificationObj.setImageUrl("/api/v1/thumbnails/centered/"+metadata.get().getId())
                        notificationObj.setCreatedAt(getCurrentTimestamp())
                        notificationObj.setModifiedAt(getCurrentTimestamp())
                        notificationObj.setRead(false)
                        notificationObj.setMessage(
                            messageSource?.getMessage("main.notification.favorites.likes", arrayOf(currentUserObj.getUsername(), "<a href='/api/v1/image/" + metadata.get()
                                .getId() + "' target='_blank'>" + metadata.get()
                                .getFileName() + "</a>"), locale) + "- " + sdtf.format(Date())
                        )
                        notificationObj.setType("favorite")
                        notificationObj.setIdentifier(favorite.getId().toString())
                        notificationObj.setUserId(admin.getId())
                        notificationObjList.add(notificationObj)
                    }
                }
                notificationRepository.saveAll(notificationObjList)

                metadata.get().setModifiedAt(getCurrentTimestamp())
                metadataRepository.save(metadata.get())

                resp["count"] = favoriteRepository.countAllByMetadataId(metadataId)
                resp["msg"] = messageSource?.getMessage("main.modal.saved", null, locale)
                resp["status"] = ApiResponse.SUCCESS.status
                return mapper.writeValueAsString(resp)
            }
        }

        resp["msg"] = messageSource?.getMessage("main.modal.saved.fail", null, locale)
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/favorite/delete"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Transactional
    @CacheEvict(value = ["allMetadata", "allMetadataByDate", "allMetadataByDateAndType", "allMetadataOnlyByDate", "allMetadataAndAttributesByDate", "singleMetadataRequest", "allAlbumMetadataWithCoordinates", "allMetadataWithCoordinates"], allEntries = true)
    fun postDeleteFavorite(model: Model, @RequestBody requestBody: JsonNode, locale: Locale): String {
        val favoritesMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (favoritesMap.containsKey("metadataId") && favoritesMap.containsKey("isFavorite")) {
            val metadataId = favoritesMap["metadataId"].toString()
            val isFavorite = favoritesMap["isFavorite"].toString().toBoolean()

            val currentUserObj = model.getAttribute("currentUser") as User?
            val favorite = Favorite()
            if (currentUserObj != null) {
                favorite.setUserId(currentUserObj.getId())
                favorite.setMetadataId(metadataId)
                favorite.setModifiedAt(getCurrentTimestamp())
                val favoriteObj = favoriteRepository.findByMetadataIdAndUserId(metadataId,currentUserObj.getId())
                if (favoriteObj != null) {
                    val favoriteId = favoriteObj.getId()
                    favorite.setId(favoriteId)

                    if (isFavorite) {
                        favoriteRepository.save(favorite)
                    } else {
                        favoriteRepository.deleteByMetadataIdAndUserId(metadataId,currentUserObj.getId())
                    }
                } else if (isFavorite) {
                    favorite.setCreatedAt(getCurrentTimestamp())
                    favoriteRepository.save(favorite)
                }

                resp["count"] = favoriteRepository.countAllByMetadataId(metadataId)
                resp["msg"] = messageSource?.getMessage("main.modal.saved", null, locale)
                resp["status"] = ApiResponse.SUCCESS.status
                return mapper.writeValueAsString(resp)
            }


        }

        resp["msg"] = messageSource?.getMessage("main.modal.saved.fail", null, locale)
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/favorites/delete"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Transactional
    @CacheEvict(value = ["allMetadata", "allMetadataByDate", "allMetadataByDateAndType", "allMetadataOnlyByDate", "allMetadataAndAttributesByDate", "singleMetadataRequest", "allAlbumMetadataWithCoordinates", "allMetadataWithCoordinates"], allEntries = true)
    fun postDeleteFavorites(model: Model, @RequestBody requestBody: JsonNode, locale: Locale): String {
        val favoritesMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (favoritesMap.containsKey("metadataIdList")) {
            val metadataIdList = favoritesMap["metadataIdList"] as MutableList<String>

            val currentUserObj = model.getAttribute("currentUser") as User?
            if (currentUserObj != null) {
                for (metadataId in metadataIdList) {
                    favoriteRepository.deleteByMetadataIdAndUserId(StringEscapeUtils.escapeHtml4(metadataId), currentUserObj.getId())
                }

                resp["msg"] = messageSource?.getMessage("main.removed", null, locale)
                resp["status"] = ApiResponse.SUCCESS.status
                return mapper.writeValueAsString(resp)
            }


        }

        resp["msg"] = messageSource?.getMessage("main.notremoved", null, locale)
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }
}