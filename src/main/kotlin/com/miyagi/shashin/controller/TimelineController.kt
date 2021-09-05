package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.RecognitionLabel
import com.miyagi.shashin.model.RecognitionLabelPhoto
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.MediaProcessingUtils
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
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

    @Autowired
    private var recognitionLabelRepository: RecognitionLabelRepository? = null

    @Autowired
    private var recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository? = null

    @Value("\${app.endpoint.url.geocode}")
    private var geocodeUrl: String? = null

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()

    @RequestMapping(value = ["/timeline"], method = [RequestMethod.GET])
    fun getTimeline(model: Model): String {
        val module = "timeline"
        val response = buildTimelineData(model,0)
        model["data"] = response["data"]!!
        model["metadataList"] = response["metadataList"]!!
        model["favorites"] = response["favorites"]!!
        model["albumList"] = response["albumList"]!!
        model["recognitionLabels"] = response["recognitionLabels"]!!
        model["labelPhotoMap"] = response["labelPhotoMap"]!!

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RequestMapping(value = ["/timeline/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedTimeline(model: Model, @PathVariable page: Int): String {
        return mapper.writeValueAsString(buildTimelineData(model,page))
    }

    @RequestMapping(value = ["/api/v1/timeline","/api/v1/timeline/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getTimelineJson(model: Model, @PathVariable(required = false) page: Int?): String {
        var pageValue = 0
        if (page != null) {
            pageValue = page
        }
        return mapper.writeValueAsString(buildTimelineData(model,pageValue))
    }

    private fun buildTimelineData(model: Model,page: Int): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        response["data"] = "There are no photos. Please setup directories in Settings and scan ."
        response["metadataList"] = ""
        response["favorites"] = ""
        response["albumList"] = ""
        response["recognitionLabels"] = ""
        response["labelPhotoMap"] = ""

        response["msg"] = "Could not get results"
        response["status"] = "fail"

        if (model.getAttribute("currentUser") != "") {
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

            val recognitionLabels = recognitionLabelRepository?.findAll()
            if (recognitionLabels != null && recognitionLabels.count() > 0) {
                response["recognitionLabels"] = recognitionLabels
            }

            val queryLimit = model.getAttribute("queryLimit").toString().toInt()
            val pageValue = page*queryLimit
            val metadataList =
                metadataRepository.findAllByOffsetAndLimit(pageValue, model.getAttribute("queryLimit").toString().toInt()).toList()
            response["metadataList"] = metadataList
            response["favorites"] = favoritesMap
            if (metadataList.count() > 0) {
                response["data"] = ""

                val labelPhotoMap = mutableMapOf<String, String>()
                for (metadata in metadataList) {
                    val recognitionLabelPhotos = recognitionLabelPhotoRepository?.findByMetadataId(metadata.getId())
                    var labelString = ""
                    if (recognitionLabelPhotos != null) {
                        for (recognitionLabelPhoto in recognitionLabelPhotos) {
                            val recognitionLabelObj = recognitionLabelRepository?.findById(recognitionLabelPhoto.getRecognitionLabelId()!!)
                            if (recognitionLabelObj != null) {
                                labelString += recognitionLabelObj.get().getName() + ","
                            }
                        }
                    }
                    if (labelString.isNotBlank()) {
                        labelString = labelString.dropLast(1)
                    }
                    labelPhotoMap[metadata.getId()] = labelString
                }
                response["labelPhotoMap"] = labelPhotoMap
            }

            val albumList = albumRepository.findAll()
            if (albumList.count() > 0) {
                response["albumList"] = albumList
            }
            response["metadataList"] = metadataList
            response["favorites"] = favoritesMap
            response["msg"] = "Results"
            response["status"] = "success"
        }

        return response
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
            metadataMap.containsKey("latlng") &&
//            metadataMap.containsKey("labelIds") &&
            metadataMap.containsKey("tagpeople")
        ) {
            val metadataObj = metadataRepository.findById(metadataMap["id"].toString())
            val recognitionLabelPhotos = recognitionLabelPhotoRepository?.findByMetadataId(metadataObj.get().getId())
            if (recognitionLabelPhotos != null) {
                for (recognitionLabelPhoto in recognitionLabelPhotos) {
                    recognitionLabelPhotoRepository?.delete(recognitionLabelPhoto)
                }
            }

            if (metadataMap["tagpeople"].toString() != "") {
                val recognitionLabelArray = metadataMap["tagpeople"].toString().split(",")
                for (recognitionLabel in recognitionLabelArray) {
                    val recognitionLabelRecord = recognitionLabelRepository?.findByNameIgnoreCase(recognitionLabel.trim())
                    var recognitionLabelObj = RecognitionLabel()
                    if (recognitionLabelRecord == null) {
                        recognitionLabelObj.setName(recognitionLabel.trim())
                        val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        val now = LocalDateTime.now()
                        recognitionLabelObj.setCreatedAt(dtf.format(now))
                        recognitionLabelObj.setModifiedAt(dtf.format(now))
                        recognitionLabelRepository?.save(recognitionLabelObj)
                    } else {
                        recognitionLabelObj = recognitionLabelRecord
                    }
                    val recognitionLabelPhotoCount = recognitionLabelPhotoRepository?.countByRecognitionLabelIdAndMetadataId(recognitionLabelObj.getId(),metadataObj.get().getId())
                    if (recognitionLabelPhotoCount == 0) {
                        val recognitionLabelPhotoObj = RecognitionLabelPhoto()
                        recognitionLabelPhotoObj.setMetadataId(metadataObj.get().getId())
                        recognitionLabelPhotoObj.setRecognitionLabelId(recognitionLabelObj.getId())
                        recognitionLabelPhotoObj.setConfidence("0.0")
                        recognitionLabelPhotoRepository?.save(recognitionLabelPhotoObj)
                    }
                }
            }
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
                    val buildPlace = TextUtils.getPlaceNameFromCoordinates(geocodeUrl!!,latlngArr[0], latlngArr[1])
                    if (buildPlace.isNotBlank()) {
                        metadataObj.get().setPlaceName(buildPlace)
                    }
                }
            }

            // Update DB
            metadataRepository.save(metadataObj.get())
            // Update MD file
            val mediaProcessingUtils = MediaProcessingUtils(model.getAttribute("apiVersion").toString(),model.getAttribute("geocodeUrl").toString())
            val originalImagePath = metadataObj.get().getPath()
            var rootDir: String? = null
            val rootMediaDirs = mediaDirRepository.findAll()
            for (rootmediaDir in rootMediaDirs) {
                if (originalImagePath != null && rootmediaDir != null) {
                    if (originalImagePath.replace('\\', '/').contains(rootmediaDir.getDirectory().toString())) {
                        rootDir = rootmediaDir.getDirectory()
                        break
                    }
                }
            }

            if (rootDir != null) {
                mediaProcessingUtils.saveMetadata(metadataObj.get(), model.getAttribute("relativeSidecarDir").toString(), rootDir)
            }
            resp["msg"] = "Saved!"
            resp["status"] = "success"
            return mapper.writeValueAsString(resp)
        }
        resp["msg"] = "Could not save"
        resp["status"] = "fail"
        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/timeline/update/batch"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    fun updateBatchMetadata(model: Model, @RequestBody requestBody: JsonNode): String? {
//        println(requestBody)
        val batchMetadataMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})

        var idArray: Array<String>? = null
        var dayTaken: Int? = null
        var monthTaken: Int? = null
        var yearTaken: Int? = null
        var latlng: String? = null
        var keywords: String? = null
        var recognitionLabelNames: String? = null

        for ((k, v) in batchMetadataMap) {
            if (v != "") {
                when (k) {
                    "tagBatchDataInput" -> {
                        recognitionLabelNames = v.toString()
                    }
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
                val metadataObj: Optional<Metadata?> = metadataRepository.findById(id)
                val metadata = metadataObj.get()
                val recognitionLabelPhotos = recognitionLabelPhotoRepository?.findByMetadataId(metadata.getId())
                if (recognitionLabelPhotos != null) {
                    for (recognitionLabelPhoto in recognitionLabelPhotos) {
                        recognitionLabelPhotoRepository?.delete(recognitionLabelPhoto)
                    }
                }

                if (recognitionLabelNames.toString() != "") {
                    val recognitionLabelArray = recognitionLabelNames.toString().split(",")
                    for (recognitionLabel in recognitionLabelArray) {
                        val recognitionLabelRecord = recognitionLabelRepository?.findByNameIgnoreCase(recognitionLabel.trim())
                        var recognitionLabelObj = RecognitionLabel()
                        if (recognitionLabelRecord == null) {
                            recognitionLabelObj.setName(recognitionLabel.trim())
                            val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            val now = LocalDateTime.now()
                            recognitionLabelObj.setCreatedAt(dtf.format(now))
                            recognitionLabelObj.setModifiedAt(dtf.format(now))
                            recognitionLabelRepository?.save(recognitionLabelObj)
                        } else {
                            recognitionLabelObj = recognitionLabelRecord
                        }
                        val recognitionLabelPhotoCount = recognitionLabelPhotoRepository?.countByRecognitionLabelIdAndMetadataId(recognitionLabelObj.getId(),metadata.getId())
                        if (recognitionLabelPhotoCount == 0) {
                            val recognitionLabelPhotoObj = RecognitionLabelPhoto()
                            recognitionLabelPhotoObj.setRecognitionLabelId(recognitionLabelObj.getId())
                            recognitionLabelPhotoObj.setMetadataId(metadata.getId())
                            recognitionLabelPhotoObj.setConfidence("0.0")
                            recognitionLabelPhotoRepository?.save(recognitionLabelPhotoObj)
                        }
                    }
                }

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
                        val buildPlace = TextUtils.getPlaceNameFromCoordinates(geocodeUrl!!,latlngArr[0], latlngArr[1])
                        if (buildPlace.isNotBlank()) {
                            metadataObj.get().setPlaceName(buildPlace)
                        }
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

            if (metadataList.isNotEmpty()) {
                // Update DB
                metadataRepository.saveAll(metadataList)

                // Update MD file
                val mediaProcessingUtils = MediaProcessingUtils(model.getAttribute("apiVersion").toString(),model.getAttribute("geocodeUrl").toString())
                for (metadata in metadataList) {
                    val originalImagePath = metadata.getPath()
                    var rootDir: String? = null
                    val rootMediaDirs = mediaDirRepository.findAll()
                    for (rootmediaDir in rootMediaDirs) {
                        if (originalImagePath != null && rootmediaDir != null) {
                            if (originalImagePath.replace('\\', '/').contains(rootmediaDir.getDirectory().toString())) {
                                rootDir = rootmediaDir.getDirectory()
                                break
                            }
                        }
                    }
                    if (rootDir != null) {
                        mediaProcessingUtils.saveMetadata(metadata, model.getAttribute("relativeSidecarDir").toString(), rootDir)
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

    @RequestMapping(value = ["/timeline/sync/{metadataId}"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    fun postSyncData(model: Model, @RequestBody requestBody: JsonNode, @PathVariable metadataId: String): String? {
        val batchMetadataMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        resp["year"] = ""
        resp["month"] = ""
        resp["day"] = ""

        if (batchMetadataMap.containsKey("id") && batchMetadataMap["id"] == metadataId) {
            val metadataOptional = metadataRepository.findById(metadataId)
            val metadataObj = metadataOptional.get()

            if (metadataObj.getTakenAt() != null) {
                val datePattern = "yyyy-MM-dd HH:mm:ss"
                val dateArray = metadataObj.getTakenAt()!!.format(datePattern).toString().split(" ")
                val takenDateArray = dateArray[0].split("-")
                val year = takenDateArray[0].toInt()
                val month = takenDateArray[1].toInt()
                val day = takenDateArray[2].toInt()
                metadataObj.setYear(year)
                metadataObj.setMonth(month)
                metadataObj.setDay(day)
                metadataRepository.save(metadataObj)

                resp["year"] = year.toString()
                resp["month"] = month.toString()
                resp["day"] = day.toString()

                resp["msg"] = "Saved"
                resp["status"] = "success"
                return mapper.writeValueAsString(resp)
            }
        }


        resp["msg"] = "Could not save"
        resp["status"] = "fail"
        return mapper.writeValueAsString(resp)
    }
}