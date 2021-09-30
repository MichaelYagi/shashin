package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.Favorite
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.Notification
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.FavoriteRepository
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.repository.NotificationRepository
import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.ArrayList
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

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()

    @GetMapping("/favorites")
    fun getFavorites(model: Model): String {
        val module = "favorites"
        model["message"] = "There are no favorites."
        model["metadataList"] = ""

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            val favoriteList = favoriteRepository.findAllByUserIdAndOffsetAndLimit(currentUserObj.getId(),0, model.getAttribute("queryLimit").toString().toInt())
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
                    model["metadataList"] = metadataList
                }
            }
        }

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RequestMapping(value = ["/favorites/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedFavorites(model: Model, @PathVariable page: Int): String {
        val response = mutableMapOf<String, Any?>()
        response["metadataList"] = ""

        if (page > 0) {
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
                    response["msg"] = "Results"
                    response["status"] = "success"
                    return mapper.writeValueAsString(response)
                }
            }
        }

        resp["msg"] = "Could not get results"
        resp["status"] = "fail"
        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/favorite/save"], method = [RequestMethod.POST])
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
                val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val now = LocalDateTime.now()
                favorite.setModifiedAt(dtf.format(now))
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
                    favorite.setCreatedAt(dtf.format(now))
                    favoriteRepository.save(favorite)
                }

                // Notify admins
                val admins = userRepository.findAllByAuthorityEquals(adminRole!!)
                val metadata = metadataRepository.findById(metadataId)
                val notificationObjList = mutableListOf<Notification>()
                for (admin in admins) {
                    val notificationObj = Notification()
                    notificationObj.setCreatedAt(dtf.format(now))
                    notificationObj.setModifiedAt(dtf.format(now))
                    notificationObj.setRead(false)
                    notificationObj.setMessage(currentUserObj.getUsername()+" likes <a href='/api/v1/image/"+metadata.get().getId()+"' target='_blank'>"+metadata.get().getFileName()+"</a> on "+dtf.format(now))
                    notificationObj.setFavoriteId(favorite.getId())
                    notificationObj.setUserId(admin.getId())
                    notificationObjList.add(notificationObj)
                }
                notificationRepository.saveAll(notificationObjList)

                resp["msg"] = "Saved!"
                resp["status"] = "success"
                return mapper.writeValueAsString(resp)
            }


        }

        resp["msg"] = "Could not save to favorites"
        resp["status"] = "fail"
        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/favorite/delete"], method = [RequestMethod.POST])
    @ResponseBody
    @Transactional
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
                val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val now = LocalDateTime.now()
                favorite.setModifiedAt(dtf.format(now))
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
                    favorite.setCreatedAt(dtf.format(now))
                    favoriteRepository.save(favorite)
                }

                resp["msg"] = "Saved!"
                resp["status"] = "success"
                return mapper.writeValueAsString(resp)
            }


        }

        resp["msg"] = "Could not save to favorites"
        resp["status"] = "fail"
        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/favorites/delete"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun postDeleteFavorites(model: Model, @RequestBody requestBody: JsonNode): String {
        val favoritesMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (favoritesMap.containsKey("metadataIdList")) {
            val metadataIdList = favoritesMap["metadataIdList"] as MutableList<String>

            val currentUserObj = model.getAttribute("currentUser") as User?
            if (currentUserObj != null) {
                for (metadataId in metadataIdList) {
                    favoriteRepository.deleteByMetadataIdAndUserId(metadataId, currentUserObj.getId())
                }

                resp["msg"] = "Removed from favorites"
                resp["status"] = "success"
                return mapper.writeValueAsString(resp)
            }


        }

        resp["msg"] = "Could not remove from favorites"
        resp["status"] = "fail"
        return mapper.writeValueAsString(resp)
    }
}