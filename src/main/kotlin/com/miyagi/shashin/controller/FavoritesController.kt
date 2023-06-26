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
import org.apache.commons.text.StringEscapeUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.CacheEvict
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.*
import javax.transaction.Transactional

@Suppress("UNCHECKED_CAST")
@Controller
@Secured("ROLE_ADMIN","ROLE_USER")
class FavoritesController {
    @Value("\${app.role.admin}")
    private var adminRole: String? = null

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

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, Any?>()

    @GetMapping("/favorites")
    fun getFavorites(model: Model): String {
        val response = buildFavorites(model,0)
        for ((k, v) in response) {
            model[k] = v!!
        }
        return model.getAttribute("activePage").toString()
    }

    @RequestMapping(value = ["/favorites/{page}","api/v1/favorites/{page}"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun getPagedFavorites(model: Model, @PathVariable page: Int): String {
        return mapper.writeValueAsString(buildFavorites(model,page))
    }

    private fun buildFavorites(model: Model, page: Int): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        val module = "favorites"
        response["activePage"] = module
        response["activeSidebar"] = module
        response["titleDescriptor"] = TextUtils.capitalized(module)
        response["message"] = "There are no favorites."
        response["metadataList"] = mutableListOf<Metadata>()
        response["keywordMap"] = mutableMapOf<String, String>()

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            val favoriteList = favoriteRepository.findAllByUserIdAndOffsetAndLimit(currentUserObj.getId(),(page*model.getAttribute("queryLimit").toString().toInt()), model.getAttribute("queryLimit").toString().toInt())
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
                val keywordList = keywordRepository!!.findAllKeywordsGroupedByMetadataId()
                val keywordMap = mutableMapOf<String, String>()
                for (keywordGroup in keywordList) {
                    keywordMap[keywordGroup.getMetadataId()!!] = keywordGroup.getKeywords()!!
                }
                response["keywordMap"] = keywordMap
                response["message"] = ""
                response["msg"] = "Results"
                response["status"] = ApiResponse.SUCCESS.status
                return response
            }
        }

        return response
    }

    @RequestMapping(value = ["/favorite/save"], method = [RequestMethod.POST], produces = ["application/json"])
    @CacheEvict(value = ["allMetadataByDate", "allMetadataByDateAndType", "allMetadataOnlyByDate", "allMetadataAndAttributesByDate", "singleMetadataRequest", "allAlbumMetadataWithCoordinates", "allMetadataWithCoordinates"], allEntries = true)
    @ResponseBody
    fun postSaveFavorite(model: Model, @RequestBody requestBody: JsonNode): String {
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

                // Notify admins
                val admins = userRepository.findAllByAuthorityEquals(adminRole!!)
                val metadata = metadataRepository.findById(metadataId)
                val notificationObjList = mutableListOf<Notification>()
                val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
                for (admin in admins) {
                    if (admin.getId() != currentUserObj.getId()) {
                        val notificationObj = Notification()
                        notificationObj.setCreatedAt(getCurrentTimestamp())
                        notificationObj.setModifiedAt(getCurrentTimestamp())
                        notificationObj.setRead(false)
                        notificationObj.setMessage(
                            currentUserObj.getUsername() + " likes <a href='/api/v1/image/" + metadata.get()
                                .getId() + "' target='_blank'>" + metadata.get()
                                .getFileName() + "</a> on " + sdtf.format(Date())
                        )
                        notificationObj.setFavoriteId(favorite.getId())
                        notificationObj.setUserId(admin.getId())
                        notificationObjList.add(notificationObj)
                    }
                }
                notificationRepository.saveAll(notificationObjList)

                resp["count"] = favoriteRepository.countAllByMetadataId(metadataId)
                resp["msg"] = "Saved!"
                resp["status"] = ApiResponse.SUCCESS.status
                return mapper.writeValueAsString(resp)
            }
        }

        resp["msg"] = "Could not save to favorites"
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/favorite/delete"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    @Transactional
    @CacheEvict(value = ["allMetadataByDate", "allMetadataByDateAndType", "allMetadataOnlyByDate", "allMetadataAndAttributesByDate", "singleMetadataRequest", "allAlbumMetadataWithCoordinates", "allMetadataWithCoordinates"], allEntries = true)
    fun postDeleteFavorite(model: Model, @RequestBody requestBody: JsonNode): String {
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
                resp["msg"] = "Saved!"
                resp["status"] = ApiResponse.SUCCESS.status
                return mapper.writeValueAsString(resp)
            }


        }

        resp["msg"] = "Could not save to favorites"
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/favorites/delete"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    @Transactional
    @CacheEvict(value = ["allMetadataByDate", "allMetadataByDateAndType", "allMetadataOnlyByDate", "allMetadataAndAttributesByDate", "singleMetadataRequest", "allAlbumMetadataWithCoordinates", "allMetadataWithCoordinates"], allEntries = true)
    fun postDeleteFavorites(model: Model, @RequestBody requestBody: JsonNode): String {
        val favoritesMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (favoritesMap.containsKey("metadataIdList")) {
            val metadataIdList = favoritesMap["metadataIdList"] as MutableList<String>

            val currentUserObj = model.getAttribute("currentUser") as User?
            if (currentUserObj != null) {
                for (metadataId in metadataIdList) {
                    favoriteRepository.deleteByMetadataIdAndUserId(StringEscapeUtils.escapeHtml4(metadataId), currentUserObj.getId())
                }

                resp["msg"] = "Removed from favorites"
                resp["status"] = ApiResponse.SUCCESS.status
                return mapper.writeValueAsString(resp)
            }


        }

        resp["msg"] = "Could not remove from favorites"
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }
}