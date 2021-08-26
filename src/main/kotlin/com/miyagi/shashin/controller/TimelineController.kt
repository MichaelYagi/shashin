package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.ImageProcessingUtils
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.annotation.security.RolesAllowed
import kotlin.collections.HashMap

@Controller
@Secured("ROLE_ADMIN")
class TimelineController {

    @Autowired
    private lateinit var metadataRepository: MetadataRepository

    @Autowired
    private lateinit var mediaDirRepository: MediaDirectoryRepository

    @Autowired
    private lateinit var albumRepository: AlbumRepository

    @Autowired
    private lateinit var favoriteRepository: FavoriteRepository

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()

    @RequestMapping(value = ["/timeline"], method = [RequestMethod.GET])
    fun getTimeline(model: Model): String {
        val module = "timeline"
        val currentUserObj = model.getAttribute("currentUser") as User?
        val favorites = favoriteRepository.findAllByUserId(currentUserObj?.getId())
        val favoritesMap = HashMap<String, Boolean>()
        if (favorites != null) {
            for (favorite in favorites) {
                if (favorite != null) {
                    favoritesMap[favorite.getMetadataId().toString()] = true
                }
            }
        }

        model["data"] = "There are no photos. Please setup directories in Settings and scan ."

        model["metadataList"] = ""
        model["favorites"] = ""
        val metadataList = metadataRepository?.findAllByOffsetAndLimit(0, model.getAttribute("queryLimit").toString().toInt())?.toList()
        if (metadataList != null) {
            model["metadataList"] = metadataList
            model["favorites"] = favoritesMap
            if (metadataList.count() > 0) {
                model["data"] = ""
            }
        }

        model["albumList"] = ""
        val albumList = albumRepository?.findAll()
        if (albumList != null && albumList.count() > 0) {
            model["albumList"] = albumList
        }

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RequestMapping(value = ["/timeline/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedTimeline(model: Model, @PathVariable page: Int): String {
        val response = mutableMapOf<String, Any?>()

        if (page > 0) {
            val currentUserObj = model.getAttribute("currentUser") as User?
            val favorites = favoriteRepository.findAllByUserId(currentUserObj?.getId())
            val favoritesMap = HashMap<String, Boolean>()
            if (favorites != null) {
                for (favorite in favorites) {
                    if (favorite != null) {
                        favoritesMap[favorite.getMetadataId().toString()] = true
                    }
                }
            }

            val metadataList = metadataRepository?.findAllByOffsetAndLimit((page*model.getAttribute("queryLimit").toString().toInt()), model.getAttribute("queryLimit").toString().toInt())?.toList()
            if (metadataList != null) {
                response["metadataList"] = ""
                response["favorites"] = ""
                response["albumList"] = ""
                response["msg"] = "Results"
                response["status"] = "success"
                if (metadataList.isNotEmpty()) {
                    val albumList = albumRepository?.findAll()
                    if (albumList != null && albumList.count() > 0) {
                        response["albumList"] = albumList
                    }
                    response["metadataList"] = metadataList
                    response["favorites"] = favoritesMap
                    response["msg"] = "Results"
                    response["status"] = "success"
                }
                return mapper.writeValueAsString(response)
            }
        }

        response["msg"] = "Could not get results"
        response["status"] = "fail"
        return mapper.writeValueAsString(response)
    }


    @RequestMapping(value = ["/timeline/update/{metadataId}"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    fun updateMetadata(model: Model, @RequestBody requestBody: JsonNode, @PathVariable metadataId: String): String? {
//        println(requestBody)
        val metadataMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})

        if (metadataMap.containsKey("id") &&
            metadataMap.containsKey("year") &&
            metadataMap.containsKey("month") &&
            metadataMap.containsKey("day") &&
            metadataMap.containsKey("keywords") &&
            metadataMap.containsKey("latlng")
        ) {
            val metadataObj = metadataRepository?.findById(metadataMap["id"].toString())
            if (metadataObj != null) {
                if (metadataMap["year"].toString() == "") {
                    metadataObj.get().setYear(null)
                } else {
                    metadataObj.get().setYear(metadataMap["year"].toString().toInt())
                }
                if (metadataMap["month"].toString() == "") {
                    metadataObj.get().setMonth(null)
                } else {
                    metadataObj.get().setMonth(metadataMap["month"].toString().toInt())
                }
                if (metadataMap["day"].toString() == "") {
                    metadataObj.get().setDay(null)
                } else {
                    metadataObj.get().setDay(metadataMap["day"].toString().toInt())
                }
                if (metadataMap["keywords"].toString() == "") {
                    metadataObj.get().setKeywords(null)
                } else {
                    val keywordArray = metadataMap["keywords"].toString().split(",")
                    var keywords = keywordArray.joinToString { it.trim() }.trim()
                    if (keywords.last() == ',') {
                        keywords = keywords.dropLast(1)
                    }
                    metadataObj.get().setKeywords(keywords)
                }
                if (metadataMap["latlng"].toString() == "") {
                    metadataObj.get().setLat(null)
                    metadataObj.get().setLng(null)
                } else {
                    var latlng = metadataMap["latlng"].toString()
                    latlng = latlng.replace("\\s".toRegex(), "")
                    val latlngArr = latlng.split(",")
                    if (latlngArr.count() == 2) {
                        metadataObj.get().setLat(latlngArr[0])
                        metadataObj.get().setLng(latlngArr[1])
                    }
                }

                // Update DB
                metadataRepository?.save(metadataObj.get())
                // Update MD file
                val imageProcessingUtils = ImageProcessingUtils(model.getAttribute("apiVersion").toString())
                val originalImagePath = metadataObj.get().getPath()
                var rootDir: String? = null
                val rootMediaDirs = mediaDirRepository?.findAll()
                if (rootMediaDirs != null) {
                    for (rootmediaDir in rootMediaDirs) {
                        if (originalImagePath != null && rootmediaDir != null) {
                            if (originalImagePath.replace('\\', '/').contains(rootmediaDir.getDirectory().toString())) {
                                rootDir = rootmediaDir.getDirectory()
                                break
                            }
                        }
                    }
                }

                if (rootDir != null) {
                    imageProcessingUtils.saveMetadata(metadataObj.get(), model.getAttribute("relativeSidecarDir").toString(), rootDir)
                }
                resp["msg"] = "Saved!"
                resp["status"] = "success"
                return mapper.writeValueAsString(resp)
            }
        }
        resp["msg"] = "Could not save"
        resp["status"] = "fail"
        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/timeline/update/batch"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    fun updateBatchMetadata(model: Model, @RequestBody requestBody: JsonNode): String? {
        val batchMetadataMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})

        var idArray: Array<String>? = null
        var dayTaken: Int? = null
        var monthTaken: Int? = null
        var yearTaken: Int? = null
        var latlng: String? = null
        var keywords: String? = null

        for ((k, v) in batchMetadataMap) {
            if (v != "") {

                when (k) {
                    "batchMetadataIds" -> {
                        idArray = mapper.readValue(v.toString(), Array<String>::class.java)
                    }
                    "dayTakenBatchData" -> {
                        dayTaken = v.toString().toInt()
                    }
                    "monthTakenBatchData" -> {
                        monthTaken = v.toString().toInt()
                    }
                    "yearTakenBatchData" -> {
                        yearTaken = v.toString().toInt()
                    }
                    "latlngBatchData" -> {
                        latlng = v.toString()
                    }
                    "keywordsBatchData"  -> {
                        keywords = v.toString()
                    }
                }
            }
        }

        if (!idArray.isNullOrEmpty()) {
            val metadataList: ArrayList<Metadata> = ArrayList()

            for (id in idArray) {
                val metadataObj: Optional<Metadata?>? = metadataRepository?.findById(id)

                if (metadataObj != null) {
                    val metadata = metadataObj.get()

                    if (dayTaken != null) {
                        metadata.setDay(dayTaken)
                    }
                    if (monthTaken != null) {
                        metadata.setMonth(monthTaken)
                    }
                    if (yearTaken != null) {
                        metadata.setYear(yearTaken)
                    }
                    if (latlng != null) {
                        latlng = latlng.replace("\\s".toRegex(), "")
                        val latlngArr = latlng.split(",")
                        if (latlngArr.count() == 2) {
                            metadata.setLat(latlngArr[0])
                            metadata.setLng(latlngArr[1])
                        }
                    }
                    if (keywords != null) {
                        val keywordArray = keywords.toString().split(",")
                        keywords = keywordArray.joinToString { it.trim() }.trim()
                        if (keywords.last() == ',') {
                            keywords = keywords.dropLast(1)
                        }
                        metadata.setKeywords(keywords)

                    }

                    metadataList.add(metadata)
                }
            }

            if (metadataList.isNotEmpty()) {
                // Update DB
                metadataRepository?.saveAll(metadataList)

                // Update MD file
                val imageProcessingUtils = ImageProcessingUtils(model.getAttribute("apiVersion").toString())
                for (metadata in metadataList) {
                    val originalImagePath = metadata.getPath()
                    var rootDir: String? = null
                    val rootMediaDirs = mediaDirRepository?.findAll()
                    if (rootMediaDirs != null) {
                        for (rootmediaDir in rootMediaDirs) {
                            if (originalImagePath != null && rootmediaDir != null) {
                                if (originalImagePath.replace('\\', '/').contains(rootmediaDir.getDirectory().toString())) {
                                    rootDir = rootmediaDir.getDirectory()
                                    break
                                }
                            }
                        }
                    }
                    if (rootDir != null) {
                        imageProcessingUtils.saveMetadata(metadata, model.getAttribute("relativeSidecarDir").toString(), rootDir)
                    }
                }
                resp["msg"] = "Saved!"
                resp["status"] = "success"
                return mapper.writeValueAsString(resp)
            }
        }
        resp["msg"] = "Could not save"
        resp["status"] = "fail"
        return mapper.writeValueAsString(resp)
    }
}