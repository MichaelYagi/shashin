package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.*
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.MetadataProcessing
import com.miyagi.shashin.util.TextUtils
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import com.miyagi.shashin.util.TextUtils.Companion.timeOffsets
import net.iakovlev.timeshape.TimeZoneEngine
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import javax.transaction.Transactional
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
    private lateinit var albumPhotoRepository: AlbumPhotoRepository

    @Autowired
    private lateinit var albumPhotoCommentRepository: AlbumPhotoCommentRepository

    @Autowired
    private lateinit var favoriteRepository: FavoriteRepository

    @Autowired
    private lateinit var userAlbumRepository: UserAlbumRepository

    @Autowired
    private lateinit var commentRepository: CommentRepository

    @Autowired
    private lateinit var albumCommentRepository: AlbumCommentRepository

    @Autowired
    private lateinit var keywordRepository: KeywordRepository

    @Autowired
    private lateinit var keywordPhotoRepository: KeywordPhotoRepository

    @Autowired
    private var recognitionLabelRepository: RecognitionLabelRepository? = null

    @Autowired
    private var recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository? = null

    @Value("\${app.endpoint.url.geocode}")
    private var geocodeUrl: String? = null

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()

    @RequestMapping(value = ["/timeline"], method = [RequestMethod.GET])
    fun getTimelineByDate(model: Model): String {
        val module = "timeline"

        val initialMetadataObj = metadataRepository.findDistinctFirstByHiddenIsFalseOrderByYearDescMonthDescDayDesc()
        var date = "undated"
        if (initialMetadataObj != null && initialMetadataObj.getYear() != null && initialMetadataObj.getMonth() != null && initialMetadataObj.getDay() != null) {
            date = initialMetadataObj.getYear().toString() + "-" + initialMetadataObj.getMonth().toString() + "-" + initialMetadataObj.getDay().toString()
        }

        val dates = getMetadataDates("all")
        model["metadataDates"] = dates["metadataDates"]!!

        val response = buildTimelineDataByDate(model,"all",date,true)

        for ((k, v) in response) {
            model[k] = v!!
        }

        model["timeOffsets"] = timeOffsets()
        val keywordList = keywordRepository.findAll()
        var keywords = ""
        if (keywordList.count() > 0) {
            keywords = keywordList.map { it?.getKeyword() }.joinToString(",")
        }
        model["keywords"] = keywords
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RequestMapping(value = ["/timeline/{mediaType}"], method = [RequestMethod.GET])
    fun getTimelineMediaTypeByDate(model: Model,@PathVariable mediaType: String): String {
        val module = "timeline"

        val initialMetadataObj = if (mediaType != "all") {
            metadataRepository.findDistinctFirstByHiddenIsFalseByMediaTypeOrderByYearDescMonthDescDayDesc(mediaType)
        } else {
            metadataRepository.findDistinctFirstByHiddenIsFalseOrderByYearDescMonthDescDayDesc()
        }
        var date = "undated"
        if (initialMetadataObj != null && initialMetadataObj.getYear() != null && initialMetadataObj.getMonth() != null && initialMetadataObj.getDay() != null) {
            date = initialMetadataObj.getYear().toString() + "-" + initialMetadataObj.getMonth().toString() + "-" + initialMetadataObj.getDay().toString()
        }

        val dates = getMetadataDates(mediaType)
        model["metadataDates"] = dates["metadataDates"]!!

        val response = buildTimelineDataByDate(model,mediaType,date,true)

        for ((k, v) in response) {
            model[k] = v!!
        }

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RequestMapping(value = ["/timeline/mediatype/{mediaType}"], method = [RequestMethod.GET])
    fun getTimelineMediaType(model: Model,@PathVariable mediaType: String): String {
        val module = "timeline"
        val response = buildTimelineData(model,mediaType,0, true)
        for ((k, v) in response) {
            model[k] = v!!
        }

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RequestMapping(value = ["/timeline/mediatype/{mediaType}/{page}","/api/v1/timeline/mediatype/{mediaType}/page/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedTimeline(model: Model, @PathVariable page: Int,@PathVariable mediaType: String): String {
        return mapper.writeValueAsString(buildTimelineData(model,mediaType,page,false))
    }

    @RequestMapping(value = ["/api/v1/timeline","/api/v1/timeline/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getTimelineJson(model: Model, @PathVariable(required = false) page: Int?): String {
        var pageValue = 0
        if (page != null) {
            pageValue = page
        }
        return mapper.writeValueAsString(buildTimelineData(model,"all",pageValue,false))
    }

    private fun buildTimelineData(model: Model,mediaTypeFilter: String,page: Int,isInitialRequest: Boolean): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        var mediaType = "photo"
        if (mediaTypeFilter == "video") {
            mediaType = mediaTypeFilter
        }
        response["message"] = "There are no "+mediaType+"s. Please setup directories in Settings and scan."
        response["metadataList"] = mutableListOf<Metadata>()
        response["favorites"] = mutableMapOf<String, Any>()
        response["albumList"] = mutableListOf<Album>()
        response["recognitionLabels"] = mutableListOf<RecognitionLabel>()
        response["labelPhotoMap"] = mutableMapOf<String, Any>()
        response["mediaTypeFilter"] = mediaTypeFilter
        response["keywordMap"] = mutableMapOf<String, Any>()

        response["msg"] = "Could not get results"
        response["status"] = "fail"

        val favoritesMap = HashMap<String, Boolean>()
        if (model.getAttribute("currentUser") != "") {
            val currentUserObj = model.getAttribute("currentUser") as User?
            if (!isInitialRequest) {
                val favorites = favoriteRepository.findAllByUserId(currentUserObj?.getId())
                if (favorites != null) {
                    for (favorite in favorites) {
                        if (favorite != null) {
                            favoritesMap[favorite.getMetadataId().toString()] = true
                        }
                    }
                }
            }

            val recognitionLabels = recognitionLabelRepository?.findAllByNameNotContaining("object")
            if (recognitionLabels != null && recognitionLabels.count() > 0) {
                response["recognitionLabels"] = recognitionLabels
            }

            val queryLimit = model.getAttribute("queryLimit").toString().toInt()
            val pageValue = page*queryLimit

            val metadataList: MutableList<Metadata> = if (mediaTypeFilter == "all") {
                metadataRepository.findAllByOffsetAndLimit(
                    pageValue,
                    model.getAttribute("queryLimit").toString().toInt()
                ).toMutableList()
            } else {
                metadataRepository.findAllByTypeOffsetAndLimit(
                    mediaTypeFilter,
                    pageValue,
                    model.getAttribute("queryLimit").toString().toInt()
                ).toMutableList()
            }

//            val metadataList: MutableList<Metadata> = if (mediaTypeFilter == "all") {
//                metadataRepository.findTimelineAll().toMutableList()
//            } else {
//                metadataRepository.findTimelineAllByType(
//                    mediaTypeFilter
//                ).toMutableList()
//            }
            response["metadataList"] = metadataList

            if (metadataList.isNotEmpty()) {
                response["message"] = ""

                if (!isInitialRequest) {
                    response["favorites"] = favoritesMap

                    val labelPhotoMap = mutableMapOf<String, String>()
                    val keywordMap = mutableMapOf<String, String>()
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
                            labelPhotoMap[metadata.getId()] = labelString
                        }

                        val keywords = keywordRepository.findKeywordsByMetadataId(metadata.getId())
                        var keywordMetadataList = ""
                        for (keyword in keywords) {
                            keywordMetadataList += keyword.getKeyword()+","
                        }

                        if (keywordMetadataList.isNotEmpty()) {
                            keywordMetadataList = keywordMetadataList.dropLast(1)
                            keywordMap[metadata.getId()] = keywordMetadataList
                        }
                    }
                    response["labelPhotoMap"] = labelPhotoMap
                    response["keywordMap"] = keywordMap
                }

                val albumList = albumRepository.findAll()
                if (albumList.count() > 0) {
                    response["albumList"] = albumList
                }
            }

            response["metadataList"] = metadataList
            response["favorites"] = favoritesMap
            response["msg"] = "Results"
            response["status"] = "success"
        }

        return response
    }

    @RequestMapping(value = ["/timeline/mediatype/{mediaType}/date/{date}","/api/v1/timeline/mediatype/{mediaType}/date/{date}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getTimelineByDate(model: Model, @PathVariable date: String,@PathVariable mediaType: String): String {
        return mapper.writeValueAsString(buildTimelineDataByDate(model,mediaType,date,false))
    }

    @RequestMapping(value = ["/timeline/mediatype/{mediaType}/date/{date}/metadata"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getTimelineMetadataByDate(model: Model, @PathVariable date: String,@PathVariable mediaType: String): String {
        return mapper.writeValueAsString(buildTimelineDataByDate(model,mediaType,date,true))
    }

    @RequestMapping(value = ["/api/v1/keywords"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getAllKeywords(model: Model): String {
        val response = mutableMapOf<String, Any?>()
        response["keywords"] = keywordRepository.findAll()
        return mapper.writeValueAsString(response)
    }

    @RequestMapping(value = ["/timeline/dates/{mediaType}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getTimelineDates(model: Model, @PathVariable mediaType: String): String {
        return mapper.writeValueAsString(getMetadataDates(mediaType))
    }

    private fun getMetadataDates(mediaType: String): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        response["metadataDates"] = mutableListOf<MetadataDate>()
        val metadataDates = if (mediaType == "all") {
            metadataRepository.findAllYearMonthDay()
        } else {
            metadataRepository.findAllYearMonthDayByMediaType(mediaType)
        }
        if (metadataDates != null) {
            response["metadataDates"] = metadataDates
        }

        return response
    }

    private fun buildTimelineDataByDate(model: Model,mediaTypeFilter: String,date: String?,metadataOnly: Boolean): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        var mediaType = "photo"
        if (mediaTypeFilter == "video") {
            mediaType = mediaTypeFilter
        }
        response["message"] = "There are no "+mediaType+"s. Please setup directories in Settings and scan."
        response["metadataList"] = mutableListOf<Metadata>()
        response["favorites"] = mutableMapOf<String, Any>()
        response["albumList"] = mutableListOf<Album>()
        response["recognitionLabels"] = mutableListOf<RecognitionLabel>()
        response["labelPhotoMap"] = mutableMapOf<String, Any>()
        response["mediaTypeFilter"] = mediaTypeFilter
        response["albumMap"] = mutableMapOf<String, Any>()
        response["keywordMap"] = mutableMapOf<String, Any>()

        response["msg"] = "Could not get results"
        response["status"] = "fail"

        if (date != null && date.isNotBlank()) {
            var year: Int? = null
            var month: Int? = null
            var day: Int? = null
            if (date != "undated") {
                val dateArray = date.split("-")
                year = dateArray[0].toInt()
                month = dateArray[1].toInt()
                day = dateArray[2].toInt()
            }

            val favoritesMap = HashMap<String, HashMap<String, Any>>()
            if (model.getAttribute("currentUser") != "") {
                val currentUserObj = model.getAttribute("currentUser") as User?

                val metadataList: MutableList<Metadata>? = if (mediaTypeFilter == "all") {
                    metadataRepository.findAllByYearAndMonthAndDayAndHiddenEqualsOrderByYearDescMonthDescDayDescTimeDesc(
                        year, month, day, false
                    ).toMutableList()
                } else {
                    metadataRepository.findAllByTypeAndYearAndMonthAndDay(
                        mediaTypeFilter,
                        year, month, day
                    ).toMutableList()
                }

                if (metadataList != null && metadataList.isNotEmpty()) {
                    response["metadataList"] = metadataList
                    response["message"] = ""
                    response["favorites"] = favoritesMap

                    if (!metadataOnly) {
                        val recognitionLabels = recognitionLabelRepository?.findAllByNameNotContaining("object")
                        if (recognitionLabels != null && recognitionLabels.count() > 0) {
                            response["recognitionLabels"] = recognitionLabels
                        }

                        val labelPhotoMap = mutableMapOf<String, String>()
                        val albumMap = mutableMapOf<String, String>()
                        val keywordMap = mutableMapOf<String, String>()

                        for (metadata in metadataList) {
                            val favorites = favoriteRepository.findAllByMetadataId(metadata.getId())
                            if (favorites != null) {
                                for (favorite in favorites) {
                                    if (favorite != null) {
                                        favoritesMap[metadata.getId()] = hashMapOf(
                                            "favorite" to (favorite.getUserId() == currentUserObj?.getId()),
                                            "count" to favoriteRepository.countAllByMetadataId(metadata.getId())
                                        )

                                        if ((favorite.getUserId() == currentUserObj?.getId())) {
                                            break
                                        }
                                    }
                                }
                            }
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
                                labelPhotoMap[metadata.getId()] = labelString
                            }

                            val albumPhotos = albumPhotoRepository.findAlbumPhotoByMetadataId(metadata.getId())
                            if (albumPhotos != null) {
                                var albumMetadataList = ""
                                for (albumPhoto in albumPhotos) {
                                    val album = albumRepository.findById(albumPhoto!!.getAlbumId()!!)
                                    albumMetadataList += album.get().getName()+","
                                }
                                if (albumMetadataList.isNotEmpty()) {
                                    albumMetadataList = albumMetadataList.dropLast(1)
                                    albumMap[metadata.getId()] = albumMetadataList
                                }
                            }

                            val keywords = keywordRepository.findKeywordsByMetadataId(metadata.getId())
                            var keywordMetadataList = ""
                            for (keyword in keywords) {
                                keywordMetadataList += keyword.getKeyword()+","
                            }

                            if (keywordMetadataList.isNotEmpty()) {
                                keywordMetadataList = keywordMetadataList.dropLast(1)
                                keywordMap[metadata.getId()] = keywordMetadataList
                            }
                        }
                        response["labelPhotoMap"] = labelPhotoMap
                        response["albumMap"] = albumMap
                        response["keywordMap"] = keywordMap

                        val albumList = albumRepository.findAll()
                        if (albumList.count() > 0) {
                            response["albumList"] = albumList
                        }

                        response["favorites"] = favoritesMap
                    }
                }

                response["msg"] = "Results"
                response["status"] = "success"
            }
        }

        return response
    }

    @RequestMapping(value = ["/timeline/remove/{metadataId}"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun removeMetadata(model: Model, @RequestBody requestBody: JsonNode, @PathVariable metadataId: String): String? {
//        println(requestBody)
        val metadataMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})

        if (metadataMap.containsKey("id") &&
            metadataMap.containsKey("year") &&
            metadataMap.containsKey("month") &&
            metadataMap.containsKey("day") &&
            metadataMap.containsKey("time") &&
            metadataMap.containsKey("offset") &&
            metadataMap.containsKey("keywords") &&
            metadataMap.containsKey("latlng") &&
            metadataMap.containsKey("title") &&
//            metadataMap.containsKey("labelIds") &&
            metadataMap.containsKey("tagpeople") &&
            metadataMap.containsKey("hidden") &&
            metadataMap.containsKey("isObject") &&
            metadataMap["id"].toString() == metadataId
        ) {
            val metadataObj = metadataRepository.findById(metadataId)
            val recognitionLabelPhotos = recognitionLabelPhotoRepository?.findByMetadataId(metadataObj.get().getId())
            if (recognitionLabelPhotos != null) {
                for (recognitionLabelPhoto in recognitionLabelPhotos) {
                    recognitionLabelPhotoRepository?.delete(recognitionLabelPhoto)
                }
            }
            val isHidden = metadataMap["hidden"].toString().toBoolean()

            if (isHidden) {
                metadataObj.get().setHidden(true)
                removeMetadata(metadataId)
            }

            // Update DB
            metadataRepository.save(metadataObj.get())
            // Update MD file
            //val mediaProcessingUtils = MediaProcessing(model.getAttribute("apiVersion").toString(),model.getAttribute("geocodeUrl").toString())
            val originalImagePath = metadataObj.get().getPath()
            var rootDir: String? = null
            val rootMediaDirs = mediaDirRepository.findAll()
            for (rootmediaDir in rootMediaDirs) {
                if (originalImagePath != null && rootmediaDir != null) {
                    if (originalImagePath.contains(rootmediaDir.getDirectory().toString())) {
                        rootDir = rootmediaDir.getDirectory()
                        break
                    }
                }
            }

            if (rootDir != null) {
                MetadataProcessing.updateSidecarMetadata(metadataObj.get(), model.getAttribute("relativeSidecarDir").toString())
            }
            resp["msg"] = "Saved!"
            resp["status"] = "success"
            return mapper.writeValueAsString(resp)
        }
        resp["msg"] = "Could not save"
        resp["status"] = "fail"
        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/timeline/update/{metadataId}","/api/v1/update/metadata/{metadataId}"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    fun updateMetadata(model: Model, @RequestBody requestBody: JsonNode, @PathVariable metadataId: String): String? {
//        println(requestBody)
        val metadataMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})

        if (metadataMap.containsKey("id") &&
            metadataMap.containsKey("year") &&
            metadataMap.containsKey("month") &&
            metadataMap.containsKey("day") &&
            metadataMap.containsKey("time") &&
            metadataMap.containsKey("offset") &&
            metadataMap.containsKey("keywords") &&
            metadataMap.containsKey("latlng") &&
            metadataMap.containsKey("title") &&
            metadataMap.containsKey("albumnames") &&
            metadataMap.containsKey("tagpeople") &&
            metadataMap.containsKey("hidden") &&
            metadataMap.containsKey("isObject") &&
            metadataMap["id"].toString() == metadataId
        ) {
            val metadataObj = metadataRepository.findById(metadataId)

            // Process albums
            val albumPhotos = albumPhotoRepository.findAlbumPhotoByMetadataId(metadataId)
            val currentAlbumIdList = mutableListOf<Int>()
            if (albumPhotos != null) {
                for (albumPhoto in albumPhotos) {
                    if (!currentAlbumIdList.contains(albumPhoto!!.getAlbumId()!!)) {
                        currentAlbumIdList.add(albumPhoto.getAlbumId()!!)
                    }
                }
            }

            if (metadataMap["albumnames"].toString().trim() != "") {
                val albumsArray = metadataMap["albumnames"].toString().split(",")

                for (albumNameRaw in albumsArray) {
                    if (albumNameRaw.trim().isNotBlank()) {
                        val albumName = albumNameRaw.trim().replace(" +".toRegex(), " ")
                        var albumObj = albumRepository.findAlbumByNameIgnoreCase(albumName)
                        var albumId: Int

                        // Add new album
                        if (albumObj == null) {
                            albumObj = Album()
                            if (metadataObj.get().getThumbnailUrlCentered() != null) {
                                albumObj.setCoverUrl(metadataObj.get().getThumbnailUrlCentered())
                            }
                            albumObj.setName(albumName)
                            albumObj.setCreatedAt(getCurrentTimestamp())
                            albumObj.setModifiedAt(getCurrentTimestamp())
                            albumObj = albumRepository.save(albumObj)
                            albumId = albumObj.getId()
                        } else {
                            albumId = albumObj.getId()
                        }

                        if (albumId > 0) {
                            val currentUserObj = model.getAttribute("currentUser") as User?
                            if (currentUserObj != null) {
                                val userAlbumCount = userAlbumRepository.countByUserIdAndAlbumId(currentUserObj.getId(), albumId)
                                if (userAlbumCount == 0) {
                                    val userAlbumObj = UserAlbum()
                                    userAlbumObj.setAlbumId(albumId)
                                    userAlbumObj.setUserId(currentUserObj.getId())
                                    userAlbumObj.setCreatedAt(getCurrentTimestamp())
                                    userAlbumObj.setModifiedAt(getCurrentTimestamp())
                                    userAlbumRepository.save(userAlbumObj)
                                }
                            }

                            val albumPhotoCount = albumPhotoRepository.countByMetadataIdAndAlbumId(metadataId, albumId)!!
                            if (albumPhotoCount == 0) {
                                val albumPhotoObj = AlbumPhoto()
                                albumPhotoObj.setMetadataId(metadataId)
                                albumPhotoObj.setAlbumId(albumId)
                                albumPhotoObj.setCreatedAt(getCurrentTimestamp())
                                albumPhotoObj.setModifiedAt(getCurrentTimestamp())
                                albumPhotoRepository.save(albumPhotoObj)
                            }
                        }

                        if (currentAlbumIdList.contains(albumId)) {
                            // Collect to delete
                            val indexToRemove = currentAlbumIdList.indexOf(albumObj.getId())
                            currentAlbumIdList.removeAt(indexToRemove)
                        }
                    }
                }
            }

            if (currentAlbumIdList.isNotEmpty()) {
                for (albumId in currentAlbumIdList) {
                    albumPhotoRepository.deleteByMetadataIdAndAlbumId(metadataId, albumId)
                    val count = albumPhotoRepository.countByAlbumId(albumId)

                    if (count != null) {
                        if (count.toInt() > 0) {
                            val coverAlbumUrl = metadataObj.get().getThumbnailUrlCentered()
                            val album = albumRepository.findById(albumId)
                            if (album.get().getCoverUrl() == coverAlbumUrl) {
                                // Use the first photo in album
                                val albumPhoto = albumPhotoRepository.findFirstByOrderByIdAsc()
                                if (albumPhoto != null) {
                                    val albumMetadataObj = metadataRepository.findById(albumPhoto.getMetadataId().toString())
                                    album.get().setCoverUrl(albumMetadataObj.get().getThumbnailUrlCentered())
                                    albumRepository.save(album.get())
                                }
                            }

                        } else {
                            userAlbumRepository.deleteByAlbumId(albumId)
                            albumRepository.deleteById(albumId)
                            // Delete comments
                            val albumComments = albumCommentRepository.findAllByAlbumId(albumId)
                            if (albumComments != null) {
                                val commentIdList = ArrayList<Int>()
                                for (albumComment in albumComments) {
                                    if (albumComment != null && albumComment.getCommentId() !in commentIdList) {
                                        commentIdList.add(albumComment.getCommentId()!!)
                                    }
                                }

                                if (commentIdList.isNotEmpty()) {
                                    commentRepository.deleteAllById(commentIdList)
                                    albumCommentRepository.deleteByAlbumId(albumId)
                                    albumPhotoCommentRepository.deleteByAlbumId(albumId)
                                }
                            }
                        }
                    }
                }
            }

            // Process tagged people
            val recognitionLabelPhotos = recognitionLabelPhotoRepository?.findByMetadataId(metadataObj.get().getId())
            if (recognitionLabelPhotos != null) {
                for (recognitionLabelPhoto in recognitionLabelPhotos) {
                    recognitionLabelPhotoRepository?.delete(recognitionLabelPhoto)
                }
            }
            val isObject = metadataMap["isObject"].toString().toBoolean()

            if (metadataMap["tagpeople"].toString().trim() != "") {
                val recognitionLabelArray = metadataMap["tagpeople"].toString().split(",")
                if (recognitionLabelArray.count() > 0) {
                    recognitionLabelPhotoRepository?.deleteByMetadataId(metadataId)
                }
                for (recognitionLabel in recognitionLabelArray) {
                    if (recognitionLabel.trim().isNotBlank()) {
                        val recognitionLabelRecord =
                            recognitionLabelRepository?.findByNameIgnoreCase(recognitionLabel.trim())
                        var recognitionLabelObj = RecognitionLabel()
                        if (recognitionLabelRecord == null) {
                            recognitionLabelObj.setName(recognitionLabel.trim())
                            recognitionLabelObj.setCreatedAt(getCurrentTimestamp())
                            recognitionLabelObj.setModifiedAt(getCurrentTimestamp())
                            recognitionLabelRepository?.save(recognitionLabelObj)
                        } else {
                            recognitionLabelObj = recognitionLabelRecord
                        }
                        val recognitionLabelPhotoCount =
                            recognitionLabelPhotoRepository?.countByRecognitionLabelIdAndMetadataId(
                                recognitionLabelObj.getId(),
                                metadataObj.get().getId()
                            )
                        if (recognitionLabelPhotoCount == 0) {
                            val recognitionLabelPhotoObj = RecognitionLabelPhoto()
                            recognitionLabelPhotoObj.setMetadataId(metadataObj.get().getId())
                            recognitionLabelPhotoObj.setRecognitionLabelId(recognitionLabelObj.getId())
                            recognitionLabelPhotoObj.setConfidence("0.0")
                            recognitionLabelPhotoRepository?.save(recognitionLabelPhotoObj)
                        }
                    }
                }
            } else if (isObject) {
                recognitionLabelPhotoRepository?.deleteByMetadataId(metadataId)
                val recognitionLabelRecord = recognitionLabelRepository?.findByNameIgnoreCase("object")
                var recognitionLabelObj = RecognitionLabel()
                if (recognitionLabelRecord == null) {
                    recognitionLabelObj.setName("object")
                    recognitionLabelObj.setCreatedAt(getCurrentTimestamp())
                    recognitionLabelObj.setModifiedAt(getCurrentTimestamp())
                    recognitionLabelRepository?.save(recognitionLabelObj)
                } else {
                    recognitionLabelObj = recognitionLabelRecord
                }

                val recognitionLabelPhotoObj = RecognitionLabelPhoto()
                recognitionLabelPhotoObj.setMetadataId(metadataId)
                recognitionLabelPhotoObj.setRecognitionLabelId(recognitionLabelObj.getId())
                recognitionLabelPhotoObj.setConfidence("-0.1")
                recognitionLabelPhotoRepository?.save(recognitionLabelPhotoObj)
            } else if (metadataMap["tagpeople"].toString().isBlank()) {
                recognitionLabelPhotoRepository?.deleteByMetadataId(metadataId)
            }

            if (metadataMap["title"].toString().trim() == "") {
                metadataObj.get().setTitle(metadataObj.get().getFileName())
            } else {
                metadataObj.get().setTitle(metadataMap["title"].toString().trim())
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
            if (metadataMap["time"].toString() == "") {
                metadataObj.get().setTime(null)
            } else {
                metadataObj.get().setTime(metadataMap["time"].toString())
            }
            if (metadataMap["offset"].toString() == "") {
                metadataObj.get().setTimeZone(null)
            } else {
                metadataObj.get().setTimeZone(metadataMap["offset"].toString())
            }

            keywordPhotoRepository.deleteAllByMetadataId(metadataId)
            if (metadataMap["keywords"].toString().isNotBlank()) {
                var keywords = metadataMap["keywords"].toString().trim()
                if (keywords.last() == ',') {
                    keywords = keywords.dropLast(1)
                }
                val keywordList = keywords.split(",").map { it.trim() }

                for (keywordTerm in keywordList) {
                    val keyword = keywordTerm.trim().lowercase()

                    if (keyword.isNotEmpty()) {
                        val keywordCount = keywordRepository.countByKeywordIgnoreCase(keyword)
                        var keywordObj = Keyword()
                        if (keywordCount == 0) {
                            keywordObj.setKeyword(keywordTerm)
                            keywordObj.setCreatedAt(getCurrentTimestamp())
                            keywordObj.setModifiedAt(getCurrentTimestamp())
                            keywordRepository.save(keywordObj)
                        } else {
                            keywordObj = keywordRepository.findByKeywordIgnoreCase(keywordTerm)!!
                        }

                        val keywordPhotoObj = KeywordPhoto()
                        keywordPhotoObj.setKeywordId(keywordObj.getId())
                        keywordPhotoObj.setMetadataId(metadataId)
                        keywordPhotoObj.setCreatedAt(getCurrentTimestamp())
                        keywordPhotoObj.setModifiedAt(getCurrentTimestamp())
                        keywordPhotoRepository.save(keywordPhotoObj)
                    }
                }

                val keywordIdsToDelete = keywordRepository.findAllOrphanedKeywordIds()
                if (keywordIdsToDelete.count() > 0) {
                    keywordRepository.deleteAllById(keywordIdsToDelete)
                }
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

                    val buildPlace = TextUtils.getPlaceNameFromJson(TextUtils.getGeoData(geocodeUrl!!,latlngArr[0], latlngArr[1]))
                    if (buildPlace.isNotBlank()) {
                        metadataObj.get().setPlaceName(buildPlace)

                        val engine = TimeZoneEngine.initialize()
                        val maybeZoneId: Optional<ZoneId> = engine.query(latlngArr[0].toString().toDouble(), latlngArr[1].toString().toDouble())
                        val zone = ZoneId.of(maybeZoneId.get().id)
                        val dt = LocalDateTime.now()
                        val zdt: ZonedDateTime = dt.atZone(zone)
                        val offset = zdt.offset
                        metadataObj.get().setTimeZone(offset.toString())
                    }
                }
            }

            // Update DB
            metadataRepository.save(metadataObj.get())
            // Update MD file
            //val mediaProcessingUtils = MediaProcessing(model.getAttribute("apiVersion").toString(),model.getAttribute("geocodeUrl").toString())
            val originalImagePath = metadataObj.get().getPath()
            var rootDir: String? = null
            val rootMediaDirs = mediaDirRepository.findAll()
            for (rootmediaDir in rootMediaDirs) {
                if (originalImagePath != null && rootmediaDir != null) {
                    if (originalImagePath.contains(rootmediaDir.getDirectory().toString())) {
                        rootDir = rootmediaDir.getDirectory()
                        break
                    }
                }
            }

            if (rootDir != null) {
                MetadataProcessing.updateSidecarMetadata(metadataObj.get(), model.getAttribute("relativeSidecarDir").toString())
            }

            val keywordList = keywordRepository.findAll()
            var keywords = ""
            if (keywordList.count() > 0) {
                keywords = keywordList.map { it?.getKeyword() }.joinToString(",")
            }
            resp["keywords"] = keywords
            resp["msg"] = "Saved!"
            resp["status"] = "success"
            return mapper.writeValueAsString(resp)
        }
        resp["msg"] = "Could not save"
        resp["status"] = "fail"
        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/timeline/remove/batch"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun removeBatchMetadata(model: Model, @RequestBody requestBody: JsonNode): String? {
//        println(requestBody)
        val batchMetadataMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})

        var idArray: Array<String>? = null
        var isHidden = false

        for ((k, v) in batchMetadataMap) {
            if (v != "") {
                when (k) {
                    "batchhidden" -> {
                        if (v.toString() == "on") {
                            isHidden = true
                        }
                    }
                    "batchMetadataIds" -> {
                        idArray = mapper.readValue(v.toString(), Array<String>::class.java)
                    }
                }
            }
        }

        if (!idArray.isNullOrEmpty()) {
            val metadataList: ArrayList<Metadata> = ArrayList()

            for (id in idArray) {
                val metadataObj: Optional<Metadata?> = metadataRepository.findById(id)
                val metadata = metadataObj.get()

                if (isHidden) {
                    metadata.setHidden(true)
                    removeMetadata(id)
                }

                metadataList.add(metadata)
            }

            if (metadataList.isNotEmpty()) {
                // Update DB
                metadataRepository.saveAll(metadataList)

                // Update MD file
                //val mediaProcessingUtils = MediaProcessing(model.getAttribute("apiVersion").toString(),model.getAttribute("geocodeUrl").toString())
                for (metadata in metadataList) {
                    val originalImagePath = metadata.getPath()
                    var rootDir: String? = null
                    val rootMediaDirs = mediaDirRepository.findAll()
                    for (rootmediaDir in rootMediaDirs) {
                        if (originalImagePath != null && rootmediaDir != null) {
                            if (originalImagePath.contains(rootmediaDir.getDirectory().toString())) {
                                rootDir = rootmediaDir.getDirectory()
                                break
                            }
                        }
                    }
                    if (rootDir != null) {
                        MetadataProcessing.updateSidecarMetadata(metadata, model.getAttribute("relativeSidecarDir").toString())
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

    @RequestMapping(value = ["/timeline/update/batch","/api/v1/update/metadata/batch"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    fun updateBatchMetadata(model: Model, @RequestBody requestBody: JsonNode): String? {
        // println(requestBody)
        val batchMetadataMap = mapper.convertValue(requestBody, object : TypeReference<BatchMetadataInput>() {})

        val idArray: Array<String>? = batchMetadataMap.batchMetadataIds
        val dayTaken: Int? = batchMetadataMap.dayTakenBatchData
        val monthTaken: Int? = batchMetadataMap.monthTakenBatchData
        val yearTaken: Int? = batchMetadataMap.yearTakenBatchData
        var latlng: String? = batchMetadataMap.latlngBatchData
        val offset: String? = batchMetadataMap.offsetTakenBatchData
        var keywords: String? = batchMetadataMap.keywordsBatchData
        val recognitionLabelNames: String? = batchMetadataMap.tagBatchDataInput
        val albumNames: String? = batchMetadataMap.albumNameInput
        val isObject = batchMetadataMap.batchisobject == "on"
        val isHidden = batchMetadataMap.batchhidden == "on"

        if (!idArray.isNullOrEmpty()) {

            val albumIdList: ArrayList<Int> = ArrayList()

            // Process albums
            if (!isHidden && albumNames != null && albumNames.toString().trim() != "") {
                val albumNameList = albumNames.toString().split(",")

                val currentUserObj = model.getAttribute("currentUser") as User?

                for (albumNameRaw in albumNameList) {
                    if (albumNameRaw.trim().isNotBlank()) {
                        val albumName = albumNameRaw.trim().replace(" +".toRegex(), " ")
                        val albumObject = albumRepository.findAlbumByNameIgnoreCase(albumName)
                        var albumObj = Album()
                        var albumId: Int

                        if (albumObject != null) {
                            albumId = albumObject.getId()
                        } else {
                            val firstAvailableMetadataId = idArray[0]
                            val metadataObj = metadataRepository.findById(firstAvailableMetadataId)
                            if (metadataObj.get().getThumbnailUrlCentered() != null) {
                                albumObj.setCoverUrl(metadataObj.get().getThumbnailUrlCentered())
                            }
                            albumObj.setName(albumName)
                            albumObj.setCreatedAt(getCurrentTimestamp())
                            albumObj.setModifiedAt(getCurrentTimestamp())
                            albumObj = albumRepository.save(albumObj)
                            albumId = albumObj.getId()
                        }

                        if (albumId > 0) {
                            albumIdList.add(albumId)

                            if (currentUserObj != null) {
                                val userAlbumCount = userAlbumRepository.countByUserIdAndAlbumId(currentUserObj.getId(), albumId)
                                if (userAlbumCount == 0) {
                                    val userAlbumObj = UserAlbum()
                                    userAlbumObj.setAlbumId(albumId)
                                    userAlbumObj.setUserId(currentUserObj.getId())
                                    userAlbumObj.setCreatedAt(getCurrentTimestamp())
                                    userAlbumObj.setModifiedAt(getCurrentTimestamp())
                                    userAlbumRepository.save(userAlbumObj)
                                }
                            }
                        }
                    }
                }
            }

            val metadataList: ArrayList<Metadata> = ArrayList()
            for (id in idArray) {
                val metadataObj: Optional<Metadata?> = metadataRepository.findById(id)
                val metadata = metadataObj.get()

                if (isHidden) {
                    metadata.setHidden(true)
                    removeMetadata(id)
                } else {
                    // Add album photo
                    if (albumIdList.isNotEmpty()) {
                        albumPhotoRepository.deleteByMetadataId(metadata.getId())

                        for (albumId in albumIdList) {
                            val albumPhotoCount = albumPhotoRepository.countByMetadataIdAndAlbumId(metadata.getId(), albumId)!!
                            if (albumPhotoCount == 0) {
                                var albumPhotoObj: AlbumPhoto
                                albumPhotoObj = AlbumPhoto()
                                albumPhotoObj.setMetadataId(metadata.getId())
                                albumPhotoObj.setAlbumId(albumId)
                                albumPhotoObj.setCreatedAt(getCurrentTimestamp())
                                albumPhotoObj.setModifiedAt(getCurrentTimestamp())
                                albumPhotoRepository.save(albumPhotoObj)
                            }
                        }
                    }

                    // Process tagged people
//                    val recognitionLabelPhotos = recognitionLabelPhotoRepository?.findByMetadataId(metadata.getId())
//                    if (recognitionLabelPhotos != null) {
//                        for (recognitionLabelPhoto in recognitionLabelPhotos) {
//                            recognitionLabelPhotoRepository?.delete(recognitionLabelPhoto)
//                        }
//                    }

                    if (isObject) {
                        // Process tagged people
                        val recognitionLabelPhotos = recognitionLabelPhotoRepository?.findByMetadataId(metadata.getId())
                        if (recognitionLabelPhotos != null) {
                            for (recognitionLabelPhoto in recognitionLabelPhotos) {
                                recognitionLabelPhotoRepository?.delete(recognitionLabelPhoto)
                            }
                        }

                        recognitionLabelPhotoRepository?.deleteByMetadataId(metadata.getId())
                        val recognitionLabelRecord = recognitionLabelRepository?.findByNameIgnoreCase("object")
                        var recognitionLabelObj = RecognitionLabel()
                        if (recognitionLabelRecord == null) {
                            recognitionLabelObj.setName("object")
                            recognitionLabelObj.setCreatedAt(getCurrentTimestamp())
                            recognitionLabelObj.setModifiedAt(getCurrentTimestamp())
                            recognitionLabelRepository?.save(recognitionLabelObj)
                        } else {
                            recognitionLabelObj = recognitionLabelRecord
                        }

                        val recognitionLabelPhotoObj = RecognitionLabelPhoto()
                        recognitionLabelPhotoObj.setMetadataId(metadata.getId())
                        recognitionLabelPhotoObj.setRecognitionLabelId(recognitionLabelObj.getId())
                        recognitionLabelPhotoObj.setConfidence("-0.1")
                        recognitionLabelPhotoRepository?.save(recognitionLabelPhotoObj)
                    } else if (recognitionLabelNames != null && recognitionLabelNames.toString().trim() != "") {
                        // Process tagged people
                        val recognitionLabelPhotos = recognitionLabelPhotoRepository?.findByMetadataId(metadata.getId())
                        if (recognitionLabelPhotos != null) {
                            for (recognitionLabelPhoto in recognitionLabelPhotos) {
                                recognitionLabelPhotoRepository?.delete(recognitionLabelPhoto)
                            }
                        }

                        val recognitionLabelArray = recognitionLabelNames.toString().split(",")
                        if (recognitionLabelArray.count() > 0) {
                            recognitionLabelPhotoRepository?.deleteByMetadataId(metadata.getId())
                        }
                        for (recognitionLabel in recognitionLabelArray) {
                            if (recognitionLabel.trim().isNotBlank()) {
                                val recognitionLabelRecord =
                                    recognitionLabelRepository?.findByNameIgnoreCase(recognitionLabel.trim())
                                var recognitionLabelObj = RecognitionLabel()
                                if (recognitionLabelRecord == null) {
                                    recognitionLabelObj.setName(recognitionLabel.trim())
                                    recognitionLabelObj.setCreatedAt(getCurrentTimestamp())
                                    recognitionLabelObj.setModifiedAt(getCurrentTimestamp())
                                    recognitionLabelRepository?.save(recognitionLabelObj)
                                } else {
                                    recognitionLabelObj = recognitionLabelRecord
                                }
                                val recognitionLabelPhotoCount =
                                    recognitionLabelPhotoRepository?.countByRecognitionLabelIdAndMetadataId(
                                        recognitionLabelObj.getId(),
                                        metadata.getId()
                                    )
                                if (recognitionLabelPhotoCount == 0) {
                                    val recognitionLabelPhotoObj = RecognitionLabelPhoto()
                                    recognitionLabelPhotoObj.setRecognitionLabelId(recognitionLabelObj.getId())
                                    recognitionLabelPhotoObj.setMetadataId(metadata.getId())
                                    recognitionLabelPhotoObj.setConfidence("0.0")
                                    recognitionLabelPhotoRepository?.save(recognitionLabelPhotoObj)
                                }
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
                    if (offset == null || offset == "") {
                        metadata.setTimeZone(null)
                    } else {
                        metadata.setTimeZone(offset)
                    }
                    if (latlng != null) {
                        latlng = latlng.replace("\\s".toRegex(), "")
                        val latlngArr = latlng.split(",")
                        if (latlngArr.count() == 2) {
                            metadata.setLat(latlngArr[0])
                            metadata.setLng(latlngArr[1])
                            val buildPlace = TextUtils.getPlaceNameFromJson(TextUtils.getGeoData(geocodeUrl!!,latlngArr[0], latlngArr[1]))
                            if (buildPlace.isNotBlank()) {
                                metadata.setPlaceName(buildPlace)

                                val engine = TimeZoneEngine.initialize()
                                val maybeZoneId: Optional<ZoneId> = engine.query(latlngArr[0].toDouble(), latlngArr[1].toDouble())
                                val zone = ZoneId.of(maybeZoneId.get().id)
                                val dt = LocalDateTime.now()
                                val zdt: ZonedDateTime = dt.atZone(zone)
                                val offset = zdt.offset
                                metadata.setTimeZone(offset.toString())
                            }
                        }
                    }
                    if (keywords != null && keywords.isNotBlank()) {
                        keywords = keywords.toString().trim()
                        if (keywords.last() == ',') {
                            keywords = keywords.dropLast(1)
                        }
                        val keywordList = keywords.split(",").map { it.trim() }

                        keywordPhotoRepository.deleteAllByMetadataId(metadata.getId())

                        for (keywordTerm in keywordList) {
                            val keyword = keywordTerm.trim().lowercase()

                            if (keyword.isNotEmpty()) {
                                val keywordCount = keywordRepository.countByKeywordIgnoreCase(keyword)
                                var keywordObj = Keyword()
                                if (keywordCount == 0) {
                                    keywordObj.setKeyword(keywordTerm)
                                    keywordObj.setCreatedAt(getCurrentTimestamp())
                                    keywordObj.setModifiedAt(getCurrentTimestamp())
                                    keywordRepository.save(keywordObj)
                                } else {
                                    keywordObj = keywordRepository.findByKeywordIgnoreCase(keywordTerm)!!
                                }

                                val keywordPhotoObj = KeywordPhoto()
                                keywordPhotoObj.setKeywordId(keywordObj.getId())
                                keywordPhotoObj.setMetadataId(metadata.getId())
                                keywordPhotoObj.setCreatedAt(getCurrentTimestamp())
                                keywordPhotoObj.setModifiedAt(getCurrentTimestamp())
                                keywordPhotoRepository.save(keywordPhotoObj)
                            }
                        }
                    }
                }

                metadataList.add(metadata)
            }

            val keywordIdsToDelete = keywordRepository.findAllOrphanedKeywordIds()
            if (keywordIdsToDelete.count() > 0) {
                keywordRepository.deleteAllById(keywordIdsToDelete)
            }

            if (metadataList.isNotEmpty()) {
                // Update DB
                metadataRepository.saveAll(metadataList)

                // Update MD file
                //val mediaProcessingUtils = MediaProcessing(model.getAttribute("apiVersion").toString(),model.getAttribute("geocodeUrl").toString())
                for (metadata in metadataList) {
                    val originalImagePath = metadata.getPath()
                    var rootDir: String? = null
                    val rootMediaDirs = mediaDirRepository.findAll()
                    for (rootmediaDir in rootMediaDirs) {
                        if (originalImagePath != null && rootmediaDir != null) {
                            if (originalImagePath.contains(rootmediaDir.getDirectory().toString())) {
                                rootDir = rootmediaDir.getDirectory()
                                break
                            }
                        }
                    }

                    if (rootDir != null) {
                        MetadataProcessing.updateSidecarMetadata(metadata, model.getAttribute("relativeSidecarDir").toString())
                    }
                }

                val keywordList = keywordRepository.findAll()
                var keywordListString = ""
                if (keywordList.count() > 0) {
                    keywordListString = keywordList.map { it?.getKeyword() }.joinToString(",")
                }
                resp["keywords"] = keywordListString
                resp["msg"] = "Saved!"
                resp["status"] = "success"
                return mapper.writeValueAsString(resp)
            }
        }
        resp["msg"] = "Could not save"
        resp["status"] = "fail"
        return mapper.writeValueAsString(resp)
    }

    @Transactional
    fun removeMetadata(id: String) {
        recognitionLabelPhotoRepository?.deleteByMetadataId(id)
        albumPhotoRepository.deleteByMetadataId(id)
        favoriteRepository.deleteByMetadataId(id)
        albumPhotoCommentRepository.deleteByMetadataId(id)
        // Find albums
        val allAlbums = albumRepository.findAll()
        for (album in allAlbums) {
            val albumId = album?.getId()
            if (albumId != null && albumId > 0) {
                val albumPhotoCount = albumPhotoRepository.countByAlbumId(albumId)
                // Delete album
                if (albumPhotoCount == 0) {
                    albumRepository.deleteById(albumId)
                    albumCommentRepository.deleteByAlbumId(albumId)
                    userAlbumRepository.deleteByAlbumId(albumId)
                }
            }
        }

        // Find people
        val allPeople = recognitionLabelRepository?.findAll()
        if (allPeople != null) {
            for (person in allPeople) {
                val peronLabelId = person?.getId()
                if (peronLabelId != null && peronLabelId > 0) {
                    val recognitionLabelPhotoCount = recognitionLabelPhotoRepository?.countByRecognitionLabelId(peronLabelId)
                    if (recognitionLabelPhotoCount == 0) {
                        recognitionLabelRepository?.deleteById(peronLabelId)
                    }
                }
            }
        }

    }

    @RequestMapping(value = ["/timeline/sync/{metadataId}"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    fun postSyncData(model: Model, @RequestBody requestBody: JsonNode, @PathVariable metadataId: String): String? {
        val batchMetadataMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        resp["year"] = ""
        resp["month"] = ""
        resp["day"] = ""
        resp["time"] = ""

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
                val time = dateArray[1]
                metadataObj.setYear(year)
                metadataObj.setMonth(month)
                metadataObj.setDay(day)
                metadataObj.setTimeZone(time)
                metadataRepository.save(metadataObj)

                resp["year"] = year.toString()
                resp["month"] = month.toString()
                resp["day"] = day.toString()
                resp["time"] = time

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