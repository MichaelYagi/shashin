package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.Favorite
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.repository.FavoriteRepository
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.repository.UserAlbumRepository
import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.ArrayList
import javax.transaction.Transactional

@Controller
class FavoritesController {
    @Value("\${app.sidecar.path}")
    private var relativeSidecarDir: String? = null

    @Value("\${app.api.version}")
    private var apiVersion: String? = null

    @Autowired
    private lateinit var favoriteRepository: FavoriteRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var metadataRepository: MetadataRepository

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()

    @GetMapping("/favorites")
    fun getFavorites(model: Model): String {
        val module = "favorites"
        model["data"] = "There are no photos."
        model["metadataList"] = ""

        val currentUserObj = userRepository.findByUsername(model.getAttribute("username").toString())
        val favoriteList = favoriteRepository.findAllByUserId(currentUserObj?.getId())
        if (favoriteList != null) {
            if (favoriteList.count() > 0) {
                val metadataList = ArrayList<Metadata>()
                model["data"] = ""
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

    @RequestMapping(value = ["/favorite/save"], method = [RequestMethod.POST])
    @ResponseBody
    fun postSaveFavorite(model: Model, @RequestBody requestBody: JsonNode): String {
        val favoritesMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (favoritesMap.containsKey("metadataId") && favoritesMap.containsKey("isFavorite")) {
            val metadataId = favoritesMap["metadataId"].toString()
            val isFavorite = favoritesMap["isFavorite"].toString().toBoolean()

            val currentUserObj = userRepository.findByUsername(model.getAttribute("username").toString())
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

    @RequestMapping(value = ["/favorite/delete"], method = [RequestMethod.POST])
    @ResponseBody
    @Transactional
    fun postDeleteFavorite(model: Model, @RequestBody requestBody: JsonNode): String {
        val favoritesMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (favoritesMap.containsKey("metadataId") && favoritesMap.containsKey("isFavorite")) {
            val metadataId = favoritesMap["metadataId"].toString()
            val isFavorite = favoritesMap["isFavorite"].toString().toBoolean()

            val currentUserObj = userRepository.findByUsername(model.getAttribute("username").toString())
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
}