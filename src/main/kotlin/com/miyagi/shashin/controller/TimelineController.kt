package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.Favorite
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.ImageProcessingUtils
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


@Controller
class TimelineController {

    @Autowired
    private val metadataRepository: MetadataRepository? = null

    @Value("\${app.sidecar.path}")
    private lateinit var relativeSidecarDir: String

    @Value("\${app.api.version}")
    private var apiVersion: String? = null

    @Autowired
    private val mediaDirRepository: MediaDirectoryRepository? = null

    @Autowired
    private val albumRepository: AlbumRepository? = null

    @Autowired
    private lateinit var favoriteRepository: FavoriteRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()

    @GetMapping("/timeline")
    fun getTimeline(model: Model): String {
        val module = "timeline"
        val currentUserObj = userRepository.findByUsername(model.getAttribute("username").toString())
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

        val sort = Sort.by(
            Sort.Order.desc("year"),
            Sort.Order.desc("month"),
            Sort.Order.desc("day")
        )

        model["metadataList"] = ""
        model["favorites"] = ""
        val metadataList = metadataRepository?.findAll(sort)
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

    @RequestMapping(value = ["/timeline/update/{metadataId}"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    fun updateMetadata(@RequestBody requestBody: JsonNode, @PathVariable metadataId: String): String? {
//        println(metadataFormUpdateData.toString())
        println(requestBody)
        val metadataMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})

        if (metadataMap.containsKey("id") &&
            metadataMap.containsKey("year") &&
            metadataMap.containsKey("month") &&
            metadataMap.containsKey("day") &&
            metadataMap.containsKey("latlng")
        ) {
            val metadataObj = metadataRepository?.findById(metadataMap["id"].toString())
            if (metadataObj != null) {
                metadataObj.get().setYear(metadataMap["year"].toString().toInt())
                metadataObj.get().setMonth(metadataMap["month"].toString().toInt())
                metadataObj.get().setDay(metadataMap["day"].toString().toInt())
                var latlng = metadataMap["latlng"].toString()
                latlng = latlng.replace("\\s".toRegex(), "")
                val latlngArr = latlng.split(",")
                if (latlngArr.count() == 2) {
                    metadataObj.get().setLat(latlngArr[0])
                    metadataObj.get().setLng(latlngArr[1])
                }

                // Update DB
                metadataRepository?.save(metadataObj.get())
                // Update MD file
                val imageProcessingUtils = ImageProcessingUtils(apiVersion)
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
                    imageProcessingUtils.saveMetadata(metadataObj.get(), relativeSidecarDir, rootDir)
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
    fun updateBatchMetadata(@RequestBody requestBody: JsonNode): String? {
        val batchMetadataMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})

        var idArray: Array<String>? = null
        var dayTaken: Int? = null
        var monthTaken: Int? = null
        var yearTaken: Int? = null
        var latlng: String? = null

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
                    
                    metadataList.add(metadata)
                }
            }

            if (metadataList.isNotEmpty()) {
                // Update DB
                metadataRepository?.saveAll(metadataList)

                // Update MD file
                val imageProcessingUtils = ImageProcessingUtils(apiVersion)
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
                        imageProcessingUtils.saveMetadata(metadata, relativeSidecarDir, rootDir)
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