package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.miyagi.shashin.model.*
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.service.ImageProcessing
import com.miyagi.shashin.service.MetadataProcessing
import com.miyagi.shashin.util.*
import com.miyagi.shashin.util.TextUtils.Companion.getCommonDateFormat
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import com.miyagi.shashin.util.TextUtils.Companion.sortPlaceNames
import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletRequest
import org.apache.commons.text.StringEscapeUtils
import org.springdoc.core.annotations.RouterOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.InputStreamResource
import org.springframework.http.*
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO
import jakarta.servlet.http.HttpServletResponse
import jakarta.transaction.Transactional
import net.iakovlev.timeshape.TimeZoneEngine
import org.springframework.context.MessageSource
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.String
import kotlin.collections.ArrayList
import kotlin.collections.set
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString

@Suppress("UNCHECKED_CAST")
@Controller
class TimelineController: BaseController() {

    @Autowired
    private lateinit var metadataRepository: MetadataRepository

    @Autowired
    private lateinit var albumRepository: AlbumRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

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
    private lateinit var searchRepository: SearchRepository

    @Autowired
    private var recognitionLabelRepository: RecognitionLabelRepository? = null

    @Autowired
    private var recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository? = null

    @Autowired
    var messageSource: MessageSource? = null

    @Value("\${app.endpoint.url.geocode}")
    private var geocodeUrl: String? = null

    @Value("\${app.role.admin}")
    private var adminRole: String? = null

    @Value("\${app.role.super}")
    private var superRole: String? = null

    @Value("\${app.api.version}")
    private var apiVersion: String? = null

    @Value("\${app.sidecar.path}")
    private var relativeSidecarDir: String? = null

    private var logger: Logger = Logger.getLogger(TimelineController::class.simpleName)

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, Any?>()

    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/timeline", "/timeline/{mediaType}"], method = [RequestMethod.GET])
    fun getTimelineMediaTypeByDate(model: Model,@PathVariable(required = false) mediaType: String?,locale: Locale): String {
        return buildTimelineModel(model,mediaType,locale)
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/metadata/range/{anchorId}/{selectId}/{view}/{mediaType}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getMetadataIdsBetweenRange(model: Model,@PathVariable(required = true) anchorId: String?, @PathVariable(required = true) selectId: String?, @PathVariable(required = true) mediaType: String?, @PathVariable(required = true) view: String?, @RequestParam albumId: Optional<Int>, @RequestParam personId: Optional<Int>, @RequestParam folderName: Optional<String>, @RequestParam searchTerm: Optional<String>, locale: Locale): String {
        val retMetadataIdArray = mutableListOf<String>()
        val retMetadataFilenameArray = mutableListOf<String>()
        val retMetadataThumbnailArray = mutableListOf<String>()
        val retMetadataDatesArray = mutableListOf<String>()
        val response = mutableMapOf<String, Any?>()
        val settings = model.getAttribute("settings") as Settings
        var currentUserObj: User? = null
        if (model.getAttribute("currentUser") != "") {
            currentUserObj = model.getAttribute("currentUser") as User?
        }

        response["msg"] = messageSource?.getMessage("main.noresults", null, locale)
        response["status"] = ApiResponse.FAIL.status
        response["metadataIdArray"] = mutableListOf<String>()
        response["metadataFilenameArray"] = mutableListOf<String>()
        response["metadataThumbnailArray"] = mutableListOf<String>()
        response["metadataDatesArray"] = mutableListOf<String>()
        response["direction"] = "down"

        if (anchorId !== null && anchorId !== "" && selectId !== null && selectId !== "" && anchorId !== selectId) {
            val albumIdCopy = albumId.orElse(0)
            val personIdCopy = personId.orElse(0)
            val folderNameCopy = folderName.orElse("")
            val searchTermCopy = searchTerm.orElse("")
            val anchorMetadata = metadataRepository.findByMetadataId(anchorId)
            val selectMetadata = metadataRepository.findByMetadataId(selectId)

            val anchorMetadataDateString: String = when (view) {
                "accessed" -> anchorMetadata?.getLastAccessedAt().orEmpty()
                "modified" -> anchorMetadata?.getModifiedAt().orEmpty()
                "recent" -> anchorMetadata?.getAddedAt().orEmpty()
                "archived" -> anchorMetadata?.getModifiedAt().orEmpty()
                else -> {
                    // Taken, albums, person, matches or timeline view
                    anchorMetadata?.getTakenAt().orEmpty()
                }
            }

            val selectMetadataDateString: String = when (view) {
                "accessed" -> selectMetadata?.getLastAccessedAt().orEmpty()
                "modified" -> selectMetadata?.getModifiedAt().orEmpty()
                "recent" -> selectMetadata?.getAddedAt().orEmpty()
                "archived" -> selectMetadata?.getModifiedAt().orEmpty()
                else -> {
                    // Taken, albums, person, matches or timeline view
                    selectMetadata?.getTakenAt().orEmpty()
                }
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val anchorMetadataDateObj = sdf.parse(anchorMetadataDateString)
            val selectMetadataDateObj = sdf.parse(selectMetadataDateString)

            var startDate = selectMetadataDateString
            var endDate = anchorMetadataDateString
            var direction = "down"
            if (anchorMetadataDateObj < selectMetadataDateObj) {
                direction = "up"
                startDate = anchorMetadataDateString
                endDate = selectMetadataDateString
            }

            // If timeline view
            val metadatas: MutableList<Metadata>? =
                if (view === "taken" || view === "timeline") {
                    if (mediaType == "all") {
                        metadataRepository.findMetadataIdBetweenTakenAt(startDate, endDate)
                    } else if (mediaType == "nolatlng") {
                        metadataRepository.findMetadataIdBetweenTakenAtNoCoord(startDate, endDate)
                    } else if (mediaType == "description") {
                        metadataRepository.findMetadataIdBetweenTakenAtByDescription(startDate, endDate)
                    } else {
                        metadataRepository.findMetadataIdBetweenTakenAtWithMediaType(
                            startDate,
                            endDate,
                            mediaType.toString()
                        )
                    }
                } else if (view == "accessed") {
                    if (mediaType == "all") {
                        metadataRepository.findMetadataIdBetweenAccessedAt(startDate, endDate)
                    } else if (mediaType == "nolatlng") {
                        metadataRepository.findMetadataIdBetweenAccessAtNoCoord(startDate, endDate)
                    } else if (mediaType == "description") {
                        metadataRepository.findMetadataIdBetweenAccessAtByDescription(startDate, endDate)
                    } else {
                        metadataRepository.findMetadataIdBetweenAccessedAtWithType(startDate, endDate, mediaType.toString())
                    }
                } else if (view == "modified") {
                    if (mediaType == "all") {
                        metadataRepository.findMetadataIdBetweenModifiedAt(startDate, endDate)
                    } else if (mediaType == "nolatlng") {
                        metadataRepository.findMetadataIdBetweenModifiedAtNoCoord(startDate, endDate)
                    } else if (mediaType == "description") {
                        metadataRepository.findMetadataIdBetweenModifiedAtByDescription(startDate, endDate)
                    } else {
                        metadataRepository.findMetadataIdBetweenModifiedAtWithType(startDate, endDate, mediaType.toString())
                    }
                } else if (view == "recent") {
                    if (mediaType == "all") {
                        metadataRepository.findMetadataIdBetweenAddedAt(startDate, endDate)
                    } else if (mediaType == "nolatlng") {
                        metadataRepository.findMetadataIdBetweenAddedAtNoCoord(startDate, endDate)
                    } else if (mediaType == "description") {
                        metadataRepository.findMetadataIdBetweenAddedAtByDescription(startDate, endDate)
                    } else {
                        metadataRepository.findMetadataIdBetweenAddedAtWithType(startDate, endDate, mediaType.toString())
                    }
                } else if (view == "taken") {
                    if (mediaType == "all") {
                        metadataRepository.findMetadataIdBetweenTakenAt(startDate, endDate)
                    } else if (mediaType == "nolatlng") {
                        metadataRepository.findMetadataIdBetweenTakenAtNoCoord(startDate, endDate)
                    } else if (mediaType == "description") {
                        metadataRepository.findMetadataIdBetweenTakenAtByDescription(startDate, endDate)
                    } else {
                        metadataRepository.findMetadataIdBetweenTakenAtWithType(
                            startDate,
                            endDate,
                            mediaType.toString()
                        )
                    }
                } else if (view == "archived") {
                    metadataRepository.findAllByHiddenByDate(startDate, endDate)
                } else if (view == "folder" && folderNameCopy.isNotEmpty()) {
                    metadataRepository.findAllByFolderByDates(startDate, endDate, folderNameCopy)
                } else if (view == "search" && searchTermCopy.isNotEmpty()) {
                    searchRepository.findMetadataBySearchTermByDate(startDate, endDate, searchTermCopy)
                } else if (view == "favorites" && personIdCopy > 0) {
                    if (mediaType == "all") {
                        favoriteRepository.findAllByUserIdAndDate(startDate, endDate, personIdCopy)
                    } else if (mediaType == "nolatlng") {
                        favoriteRepository.findAllByUserIdAndDateNoCoord(startDate, endDate, personIdCopy)
                    } else if (mediaType == "description") {
                        favoriteRepository.findAllByUserIdAndDateByDescription(startDate, endDate, personIdCopy)
                    } else {
                        favoriteRepository.findAllByUserIdAndDateByMediaType(startDate, endDate, personIdCopy, mediaType.toString())
                    }
                } else if (view == "matches" && personIdCopy > 0) {
                    metadataRepository.findLowMatchesByPersonAndDate(startDate, endDate, personIdCopy, settings.getRecognitionConfidenceThreshold()!!)
                } else if (view == "person" && personIdCopy > 0) {
                    if (currentUserObj!!.getAuthority() == model.getAttribute("userRole")) {
                        metadataRepository.findAlbumPhotoByPersonAndDate(
                            startDate,
                            endDate,
                            settings.getRecognitionConfidenceThreshold()!!,
                            personIdCopy,
                            currentUserObj.getId()
                        )
                    } else {
                        metadataRepository.findMetadataByPersonAndDate(
                            startDate, endDate,
                            settings.getRecognitionConfidenceThreshold()!!,
                            personIdCopy
                        )
                    }
                } else if (albumIdCopy > 0) {
                    if (mediaType == "all") {
                        albumRepository.findMetadataIdBetweenAlbum(albumIdCopy, startDate, endDate)
                    } else if (mediaType == "nolatlng") {
                        albumRepository.findMetadataIdBetweenAlbumNoCoord(albumIdCopy, startDate, endDate)
                    } else if (mediaType == "description") {
                        albumRepository.findMetadataIdBetweenAlbumByDesciption(albumIdCopy, startDate, endDate)
                    } else if (mediaType == "comments") {
                        albumRepository.findMetadataIdBetweenAlbumByComments(albumIdCopy, startDate, endDate)
                    } else {
                        albumRepository.findMetadataIdBetweenAlbumWithType(albumIdCopy, startDate, endDate, mediaType.toString())
                    }
                } else {
                    if (mediaType == "all") {
                        metadataRepository.findMetadataIdBetweenTakenAt(startDate, endDate)
                    } else {
                        metadataRepository.findMetadataIdBetweenTakenAtWithType(startDate, endDate, mediaType.toString())
                    }
                }

            if (metadatas != null && metadatas.isNotEmpty()) {
                var startCaptured = false

                for (metadata in metadatas) {
                    if (direction == "down" && metadata.getId() == anchorId) {
                        startCaptured = true
                    } else if (direction == "up" && metadata.getId() == selectId) {
                        startCaptured = true
                    }

                    if (startCaptured) {
                        retMetadataIdArray.add(metadata.getId())
                        retMetadataFilenameArray.add(metadata.getFileName()!!)
                        retMetadataThumbnailArray.add("/api/v1/thumbnails/centered/"+metadata.getId())

                        if (albumIdCopy > 0 || view == "timeline" || view == "taken") {
                            retMetadataDatesArray.add(metadata.getTakenAt()!!)
                        } else if (view == "accessed") {
                            retMetadataDatesArray.add(metadata.getLastAccessedAt()!!)
                        } else if (view == "modified") {
                            retMetadataDatesArray.add(metadata.getModifiedAt()!!)
                        } else if (view == "recent") {
                            retMetadataDatesArray.add(metadata.getAddedAt()!!)
                        } else {
                            retMetadataDatesArray.add(metadata.getTakenAt()!!)
                        }

                        if (direction == "down" && metadata.getId() == selectId) {
                            break
                        } else if (direction == "up" && metadata.getId() == anchorId) {
                            break
                        }
                    }
                }

                response["msg"] = messageSource?.getMessage("main.success", null, locale)
                response["status"] = ApiResponse.SUCCESS.status
                response["direction"] = direction
                response["metadataIdArray"] = retMetadataIdArray
                response["metadataFilenameArray"] = retMetadataFilenameArray
                response["metadataThumbnailArray"] = retMetadataThumbnailArray
                response["metadataDatesArray"] = retMetadataDatesArray
            }
        }

        return mapper.writeValueAsString(response)
    }

    private fun buildTimelineModel(model: Model,mediaTypeFilter: String?, locale: Locale): String {
        val module = "timeline"

//        val validMediaTypes = arrayOf("all","video")

        var mediaType = mediaTypeFilter

        if (mediaTypeFilter.isNullOrEmpty()) {
            mediaType = "all"
        }

        val initialMetadataObj = if (mediaType == "all") {
            metadataRepository.findDistinctFirstByHiddenIsFalseOrderByYearDescMonthDescDayDescTimeDesc()
        } else if (mediaType == "nolatlng") {
            metadataRepository.findDistinctFirstByHiddenIsFalseByNoCoordOrderByYearDescMonthDescDayDesc()
        } else if (mediaType == "description") {
            metadataRepository.findDistinctFirstByHiddenIsFalseByDescriptionOrderByYearDescMonthDescDayDesc()
        } else {
            metadataRepository.findDistinctFirstByHiddenIsFalseByMediaTypeOrderByYearDescMonthDescDayDesc(mediaType!!)
        }
        var date = "undated"
        if (initialMetadataObj?.getYear() != null && initialMetadataObj.getMonth() != null && initialMetadataObj.getDay() != null) {
            date = initialMetadataObj.getYear().toString() + "-" + initialMetadataObj.getMonth().toString() + "-" + initialMetadataObj.getDay().toString()
        }

        model["initialLongDate"] = TextUtils.formatToLongDate(date, model.getAttribute("locale").toString()).toString()
        val dates = getMetadataDates(mediaType, locale)
        model["metadataDates"] = dates["metadataDates"]!!

        val timelineData = buildTimelineDataByDate(model,mediaType,date,false,locale)

        for ((k, v) in timelineData) {
            model[k] = v!!
        }

        val countByYearAndMonthMap = mutableMapOf<String, Int>()

        val countByYearAndMonthList = metadataRepository.countByYearAndMonth()

        if (countByYearAndMonthList.count() > 0) {
            for (yearMonthCount in countByYearAndMonthList) {
                countByYearAndMonthMap[yearMonthCount.getYear().toString() + "-" + yearMonthCount.getMonth().toString()] = yearMonthCount.getCount()!!
            }
        }
        model["metadataYearMonthCount"] = countByYearAndMonthMap

//        if (!validMediaTypes.contains(mediaType)) {
//            model["message"] = "Oops! $mediaType is not a valid media type!"
//        }

        getAllAttributeData(model)

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)

        return module
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getTimelineMediaType",
            summary = "Get results for timeline content with associated favorites mapping.",
            description = "<strong>Get results for timeline content with associated favorites mapping.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/timeline/mediatype/{mediaType}\" \\\n" +
                    "-H \"Content-Type: application/json\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>Content-Type</td><td>header</td><td>string</td><td>required</td><td>application/json</td></tr>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>mediaType</td><td>param</td><td>string</td><td>required</td><td>One of \"all\", \"video\" or \"image\"</td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"msg\": \"\",\n" +
                    "    \"message\": \"\",\n" +
                    "    \"status\": \"success\",\n" +
                    "    \"mediaTypeFilter\": \"&lt;media_type&gt;\",\n" +
                    "    \"metadataList\": [\n" +
                    "        {\n" +
                    "           &lt;metadata&gt;\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"favorites\": {\n" +
                    "        \"&lt;metadata_id&gt;\": {\n" +
                    "            \"count\": &lt;count&gt;,\n" +
                    "            \"favorite\": &lt;is_favorite&gt;\n" +
                    "        }\n" +
                    "    }\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>mediaTypeFilter</td><td>string</td><td>One of \"all\", \"video\" or \"image\"</td></tr>" +
                    "<tr><td>metadataList[].metadata</td><td>object</td><td>A <a href=\"#\" data-bs-toggle=\"modal\" data-bs-target=\"#propMetadataDocs\">Metadata</a> object</td></tr>" +
                    "<tr><td>favourites.&lt;metadata_id&gt;.count</td><td>int</td><td>The number of people who saved this media as a favorite</td></tr>" +
                    "<tr><td>favourites.&lt;metadata_id&gt;.favorite</td><td>boolean</td><td>True if saved as a favorite</td></tr>" +
                    "</tbody></table>"
        )
    )
    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/timeline/mediatype/{mediaType}"], method = [RequestMethod.GET])
    fun getTimelineMediaType(model: Model,@PathVariable mediaType: String, locale: Locale): String {
        val module = "timeline"
        val response = buildTimelineData(model,mediaType,0,model.getAttribute("queryLimit").toString().toInt(),locale)
        for ((k, v) in response) {
            model[k] = v!!
        }

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/timeline/mediatype/{mediaType}/{page}","/api/v1/timeline/mediatype/{mediaType}/page/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedTimeline(model: Model, @PathVariable page: Int,@PathVariable mediaType: String,locale: Locale): String {
        return mapper.writeValueAsString(buildTimelineData(model,mediaType,page,model.getAttribute("queryLimit").toString().toInt(),locale))
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/timeline/yearmonthcounts/{mediaType}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getMetadataYearMonthCounts(model: Model, @PathVariable mediaType: String, locale: Locale): String {
        val response = mutableMapOf<String, Any?>()

        response["msg"] = messageSource?.getMessage("main.results", null, locale)
        response["status"] = ApiResponse.SUCCESS.status

        val metadataDateHash = mutableMapOf<String, Int>()
        response["metadataDatesHash"] = metadataDateHash

        response["metadataDates"] = mutableListOf<MetadataDate>()
        val metadataDates = if (mediaType == "all") {
            metadataRepository.findAllYearMonthDay()
        } else if (mediaType == "nolatlng") {
            metadataRepository.findAllYearMonthDayByNoCoord()
        } else if (mediaType == "description") {
            metadataRepository.findAllYearMonthDayByDescription()
        } else {
            metadataRepository.findAllYearMonthDayByMediaType(mediaType)
        }
        if (metadataDates != null) {
            response["metadataDates"] = metadataDates

            val dates = metadataDates.toMutableList()
            for ((index, metadataDate) in dates.withIndex()) {
                metadataDateHash[metadataDate.getYear().toString() + "-" + metadataDate.getMonth()
                    .toString() + "-" + metadataDate.getDay().toString()] = index
            }
            response["metadataDatesHash"] = metadataDateHash
        }

        val countByYearAndMonthMap = mutableMapOf<String, Int>()

        val countByYearAndMonthList = metadataRepository.countByYearAndMonth()

        if (countByYearAndMonthList.count() > 0) {
            for (yearMonthCount in countByYearAndMonthList) {
                countByYearAndMonthMap[yearMonthCount.getYear().toString() + "-" + yearMonthCount.getMonth().toString()] = yearMonthCount.getCount()!!
            }
        }
        response["metadataYearMonthCount"] = countByYearAndMonthMap

        return mapper.writeValueAsString(response)
    }


    @RouterOperation(
        operation =
        Operation(
            operationId = "getTimelineJson",
            summary = "Get paged or all results for timeline content with associated favorites mapping.",
            description = "<strong>Get paged or all results for timeline content with associated favorites mapping.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/timeline/{page}\" \\\n" +
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
                    "    \"message\": \"\",\n" +
                    "    \"status\": \"success\",\n" +
                    "    \"mediaTypeFilter\": \"&lt;media_type&gt;\",\n" +
                    "    \"metadataList\": [\n" +
                    "        {\n" +
                    "           &lt;metadata&gt;\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"favorites\": {\n" +
                    "        \"&lt;metadata_id&gt;\": {\n" +
                    "            \"count\": &lt;count&gt;,\n" +
                    "            \"favorite\": &lt;is_favorite&gt;\n" +
                    "        }\n" +
                    "    }\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>mediaTypeFilter</td><td>string</td><td>One of \"all\", \"video\" or \"image\"</td></tr>" +
                    "<tr><td>metadataList[].metadata</td><td>object</td><td>A <a href=\"#\" data-bs-toggle=\"modal\" data-bs-target=\"#propMetadataDocs\">Metadata</a> object</td></tr>" +
                    "<tr><td>favourites.&lt;metadata_id&gt;.count</td><td>int</td><td>The number of people who saved this media as a favorite</td></tr>" +
                    "<tr><td>favourites.&lt;metadata_id&gt;.favorite</td><td>boolean</td><td>True if saved as a favorite</td></tr>" +
                    "</tbody></table>"
        )
    )
    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/api/v1/timeline","/api/v1/timeline/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getTimelineJson(model: Model, @PathVariable(required = false) page: Int?, locale: Locale): String {
        var pageValue = 0
        if (page != null) {
            pageValue = page
        }
        return mapper.writeValueAsString(buildTimelineData(model,"all",pageValue, model.getAttribute("queryLimit").toString().toInt(), locale))
    }

    private fun buildTimelineData(model: Model, mediaTypeFilter: String, page: Int = 0, size: Int = model.getAttribute("queryLimit").toString().toInt(), locale: Locale = Locale("en")): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        var mediaType = "photo"
        if (mediaTypeFilter == "video") {
            mediaType = mediaTypeFilter
        }
        response["message"] = messageSource?.getMessage("main.noresults", null, locale)
        response["metadataList"] = mutableListOf<Metadata>()
        response["favorites"] = mutableMapOf<String, Any>()
        response["mediaTypeFilter"] = mediaTypeFilter
        response["page"] = page
        response["size"] = size

        response["msg"] = messageSource?.getMessage("main.noresults", null, locale)
        response["status"] = ApiResponse.FAIL.status

        val favoritesMap = HashMap<String, HashMap<String, Any>>()
        if (model.getAttribute("currentUser") != "") {
            val currentUserObj = model.getAttribute("currentUser") as User?
            val pageValue = page*size

            var metadataList: MutableList<Metadata?>? = null

            if (metadataRepository.count() > 0) {
                metadataList = if (mediaTypeFilter == "all") {
                    metadataRepository.findAllByOffsetAndLimit(
                        pageValue,
                        size
                    ).toMutableList()
                } else if (mediaTypeFilter == "nolatlng") {
                    metadataRepository.findAllMissingCoordOffsetAndLimit(
                        pageValue,
                        size
                    ).toMutableList()
                } else if (mediaTypeFilter == "description") {
                    metadataRepository.findAllDescriptionOffsetAndLimit(
                        pageValue,
                        size
                    ).toMutableList()
                } else {
                    metadataRepository.findAllByTypeOffsetAndLimit(
                        mediaTypeFilter,
                        pageValue,
                        size
                    ).toMutableList()
                }
            }

            if (!metadataList.isNullOrEmpty()) {
                response["message"] = ""
                val favoriteCounts = favoriteRepository.countByMetadataIdIn(metadataList.map { it!!.getId() }.toList())

                if (favoriteCounts.count() > 0) {
                    for (favoriteCount in favoriteCounts) {
                        favoritesMap[favoriteCount.getMetadataId()!!] = hashMapOf(
                            "favorite" to (favoriteCount.getUserId() == currentUserObj?.getId()),
                            "count" to favoriteCount.getCount() as Any
                        )

                        if (favoriteCount.getUserId() == currentUserObj?.getId()) {
                            break
                        }
                    }
                }

                response["favorites"] = favoritesMap
            }

            response["metadataList"] = metadataList
            response["favorites"] = favoritesMap
            response["msg"] = messageSource?.getMessage("main.results", null, locale)
            response["status"] = ApiResponse.SUCCESS.status
        }

        return response
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/timeline/mediatype/{mediaType}/date/{date}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    @Cacheable(value = ["allMetadataAndAttributesByDate"], key = "{#date, #mediaType}")
    fun getTimelineByDate(model: Model, @PathVariable date: String,@PathVariable mediaType: String,locale: Locale): ResponseEntity<String> {
        val json = mapper.writeValueAsString(buildTimelineDataByDate(model,mediaType,date,false,locale))
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
            .body(json)
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getTimelineByDateApi",
            summary = "Get paged or all results for timeline content with associated favorites mapping.",
            description = "<strong>Get paged or all results for timeline content with associated favorites mapping.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/timeline/mediatype/{mediaType}/date/{date}\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>mediaType</td><td>param</td><td>string</td><td>required</td><td>One of \"all\", \"video\" or \"image\"</td></tr>" +
                    "<tr><td>date</td><td>param</td><td>string</td><td>required</td><td>A valid timeline date in the format <yyyy-mm-dd> eg. 2023-6-27</td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"msg\": \"\",\n" +
                    "    \"message\": \"\",\n" +
                    "    \"status\": \"success\",\n" +
                    "    \"mediaTypeFilter\": \"&lt;media_type&gt;\",\n" +
                    "    \"metadataList\": [\n" +
                    "        {\n" +
                    "           &lt;metadata&gt;\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"favorites\": {\n" +
                    "        \"&lt;metadata_id&gt;\": {\n" +
                    "            \"count\": &lt;count&gt;,\n" +
                    "            \"favorite\": &lt;is_favorite&gt;\n" +
                    "        }\n" +
                    "    }\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>mediaTypeFilter</td><td>string</td><td>One of \"all\", \"video\" or \"image\"</td></tr>" +
                    "<tr><td>metadataList[].metadata</td><td>object</td><td>A <a href=\"#\" data-bs-toggle=\"modal\" data-bs-target=\"#propMetadataDocs\">Metadata</a> object</td></tr>" +
                    "<tr><td>favourites.&lt;metadata_id&gt;.count</td><td>int</td><td>The number of people who saved this media as a favorite</td></tr>" +
                    "<tr><td>favourites.&lt;metadata_id&gt;.favorite</td><td>boolean</td><td>True if saved as a favorite</td></tr>" +
                    "</tbody></table>"
        )
    )
    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/api/v1/timeline/mediatype/{mediaType}/date/{date}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    @Cacheable(value = ["allMetadataAndAttributesByDate"], key = "{#date, #mediaType}")
    fun getTimelineByDateApi(model: Model, @PathVariable date: String,@PathVariable mediaType: String,locale: Locale): String {
        return mapper.writeValueAsString(buildTimelineDataByDate(model,mediaType,date,false,locale))
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/timeline/mediatype/{mediaType}/date/{date}/metadata"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    @Cacheable(value = ["allMetadataOnlyByDate"], key = "{#date, #mediaType}")
    fun getTimelineMetadataByDate(model: Model, @PathVariable date: String,@PathVariable mediaType: String,locale: Locale): ResponseEntity<String> {
        val jsonMap = buildTimelineDataByDate(model,mediaType,date,true,locale)
        val json = mapper.writeValueAsString(jsonMap)
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
            .body(json)
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getTimelineMetadataByDateApi",
            summary = "Get minimal metadata information by date.",
            description = "<strong>Get minimal metadata information by date.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/timeline/mediatype/{mediaType}/date/{date}/metadata\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>mediaType</td><td>param</td><td>string</td><td>required</td><td>One of \"all\", \"video\" or \"image\"</td></tr>" +
                    "<tr><td>date</td><td>param</td><td>string</td><td>required</td><td>A valid timeline date in the format &lt;yyyy-mm-dd&gt; eg. 2023-6-27</td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"msg\": \"\",\n" +
                    "    \"message\": \"\",\n" +
                    "    \"status\": \"success\",\n" +
                    "    \"mediaTypeFilter\": \"&lt;media_type&gt;\",\n" +
                    "    \"metadataList\": [\n" +
                    "        {\n" +
                    "           \"id\": \"&lt;metadata_id&gt;\",\n" +
                    "           \"thumbnailUrlCentered\": \"&lt;centered_thumbnail_url&gt;\",\n" +
                    "           \"thumbnailSmallHeight\": &lt;thumbnail_height&gt;,\n" +
                    "           \"thumbnailUrlSmall\": \"&lt;thumbnail_url&gt;\",\n" +
                    "           \"thumbnailSmallWidth\": &lt;thumbnail_width&gt;,\n" +
                    "           \"type\": \"&lt;data_type&gt;\",\n" +
                    "           \"fileName\": \"&lt;name_of_file&gt;\",\n" +
                    "           \"year\": &lt;year&gt;,\n" +
                    "           \"month\": &lt;month&gt;,\n" +
                    "           \"day\": &lt;day&gt;\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>mediaTypeFilter</td><td>string</td><td>One of \"all\", \"video\" or \"image\"</td></tr>" +
                    "<tr><td>metadataList[].id</td><td>string</td><td>The metadata ID</td></tr>" +
                    "<tr><td>metadataList[].thumbnailUrlCentered</td><td>string</td><td>The centered thumbnail image URL</td></tr>" +
                    "<tr><td>metadataList[].thumbnailSmallHeight</td><td>int</td><td>The thumbnail height</td></tr>" +
                    "<tr><td>metadataList[].thumbnailUrlSmall</td><td>string</td><td>The thumbnail image URL</td></tr>" +
                    "<tr><td>metadataList[].thumbnailSmallWidth</td><td>int</td><td>The thumbnail width</td></tr>" +
                    "<tr><td>metadataList[].type</td><td>string</td><td>The file type</td></tr>" +
                    "<tr><td>metadataList[].fileName</td><td>string</td><td>The file name</td></tr>" +
                    "<tr><td>metadataList[].year</td><td>int</td><td>The year the media was captured</td></tr>" +
                    "<tr><td>metadataList[].month</td><td>int</td><td>The month the media was captured</td></tr>" +
                    "<tr><td>metadataList[].day</td><td>int</td><td>The day the media was captured</td></tr>" +
                    "</tbody></table>"
        )
    )
    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/api/v1/timeline/mediatype/{mediaType}/date/{date}/metadata"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    @Cacheable(value = ["allMetadataOnlyByDate"], key = "{#date, #mediaType}")
    fun getTimelineMetadataByDateApi(model: Model, @PathVariable date: String,@PathVariable mediaType: String,locale: Locale): String {
        val jsonMap = buildTimelineDataByDate(model,mediaType,date,true,locale)
        return mapper.writeValueAsString(jsonMap)
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getAllKeywords",
            summary = "Get a list of all keywords.",
            description = "<strong>Get a list of all keywords.</strong>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/keywords\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"keywords\": [\n" +
                    "        {\n" +
                    "            \"id\": &lt;keyword_id&gt;,\n" +
                    "            \"keyword\": \"&lt;keyword&gt;\"\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>keywords[].id</td><td>int</td><td>The keyword ID</td></tr>" +
                    "<tr><td>keywords[].keyword</td><td>string</td><td>The keyword</td></tr>" +
                    "</tbody></table>"
        )
    )
    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/api/v1/keywords"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getAllKeywords(model: Model): String {
        val response = mutableMapOf<String, Any?>()
        response["keywords"] = keywordRepository.findAllDistinctOrderByKeyword()

        return mapper.writeValueAsString(response)
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getTimelineDates",
            summary = "Get a list of all timeline dates by media type.",
            description = "<strong>Get a list of all timeline dates by media type.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/timeline/dates/{mediaType}\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>mediaType</td><td>param</td><td>string</td><td>required</td><td>One of \"all\", \"video\" or \"image\"</td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"metadataDates\": [\n" +
                    "        {\n" +
                    "            \"year\": &lt;year&gt;,\n" +
                    "            \"month\": \"&lt;month&gt;\",\n" +
                    "            \"day\": \"&lt;dat&gt;\"\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>metadataDates[].year</td><td>int</td><td>The 4 digit year</td></tr>" +
                    "<tr><td>metadataDates[].month</td><td>int</td><td>The month (1-12)</td></tr>" +
                    "<tr><td>metadataDates[].day</td><td>int</td><td>The day (1-31)</td></tr>" +
                    "</tbody></table>"
        )
    )
    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/timeline/dates/{mediaType}","/api/v1/timeline/dates/{mediaType}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getTimelineDates(model: Model, @PathVariable mediaType: String, locale: Locale): String {
        return mapper.writeValueAsString(getMetadataDates(mediaType, locale))
    }

    private fun getMetadataDates(mediaType: String, locale: Locale): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        response["metadataDates"] = mutableListOf<MetadataDate>()
        response["msg"] = messageSource?.getMessage("main.noresults", null, locale)
        response["status"] = ApiResponse.FAIL.status

        val metadataDates = if (mediaType == "all") {
            metadataRepository.findAllYearMonthDay()
        } else if (mediaType == "nolatlng") {
            metadataRepository.findAllYearMonthDayByNoCoord()
        } else if (mediaType == "description") {
            metadataRepository.findAllYearMonthDayByDescription()
        } else {
            metadataRepository.findAllYearMonthDayByMediaType(mediaType)
        }
        if (metadataDates != null) {
            response["msg"] = messageSource?.getMessage("main.success", null, locale)
            response["status"] = ApiResponse.SUCCESS.status
            response["metadataDates"] = metadataDates
        }

        return response
    }

    private fun buildTimelineDataByDate(model: Model,mediaTypeFilter: String,date: String?,metadataOnly: Boolean,locale: Locale): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        var mediaType = "photo"
        if (mediaTypeFilter == "video") {
            mediaType = mediaTypeFilter
        }
        response["message"] = messageSource?.getMessage("main.noresults", null, locale)
        response["metadataList"] = mutableListOf<Metadata>()
        response["favorites"] = mutableMapOf<String, Any>()
        response["mediaTypeFilter"] = mediaTypeFilter
        response["placeNameHeaders"] = mutableListOf<String>()

        response["msg"] = messageSource?.getMessage("main.noresults", null, locale)
        response["status"] = ApiResponse.FAIL.status

        if (!date.isNullOrBlank()) {
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

                val metadataList: MutableList<Metadata>

                if (metadataOnly) {
                    metadataList = if (mediaTypeFilter == "all") {
                        metadataRepository.findAllByYearAndMonthAndDayAndHiddenEqualsOrderByYearDescMonthDescDayDescTimeDesc(
                            year, month, day, false
                        ).toMutableList()
                    } else if (mediaTypeFilter == "nolatlng") {
                        metadataRepository.findAllByNoCoordAndYearAndMonthAndDay(
                            year, month, day
                        ).toMutableList()
                    } else if (mediaTypeFilter == "description") {
                        metadataRepository.findAllByDescriptionAndYearAndMonthAndDay(
                            year, month, day
                        ).toMutableList()
                    } else {
                        metadataRepository.findAllByTypeAndYearAndMonthAndDay(
                            mediaTypeFilter,
                            year, month, day
                        ).toMutableList()
                    }

                    if (metadataList.isNotEmpty()) {
                        response["metadataList"] = metadataList
                        response["message"] = ""
                        response["favorites"] = favoritesMap
                    }
                } else {
                    metadataList = if (mediaTypeFilter == "all") {
                        metadataRepository.findAllByYearAndMonthAndDayAndHiddenEqualsOrderByYearDescMonthDescDayDescTimeDesc(
                            year, month, day, false
                        ).toMutableList()
                    } else if (mediaTypeFilter == "nolatlng") {
                        metadataRepository.findAllByNoCoordAndYearAndMonthAndDay(
                            year, month, day
                        ).toMutableList()
                    } else if (mediaTypeFilter == "description") {
                        metadataRepository.findAllByDescriptionAndYearAndMonthAndDay(
                            year, month, day
                        ).toMutableList()
                    } else {
                        metadataRepository.findAllByTypeAndYearAndMonthAndDay(
                            mediaTypeFilter,
                            year, month, day
                        ).toMutableList()
                    }

                    if (metadataList.isNotEmpty()) {
                        response["metadataList"] = metadataList
                        response["message"] = ""
                        response["favorites"] = favoritesMap

                        val favoriteCounts = favoriteRepository.countByMetadataIdIn(metadataList.map { it.getId() }.toList())

                        if (favoriteCounts.count() > 0) {
                            for (favoriteCount in favoriteCounts) {
                                favoritesMap[favoriteCount.getMetadataId()!!] = hashMapOf(
                                    "favorite" to (favoriteCount.getUserId() == currentUserObj?.getId()),
                                    "count" to favoriteCount.getCount() as Any
                                )

                                if (favoriteCount.getUserId() == currentUserObj?.getId()) {
                                    break
                                }
                            }
                        }

                        response["favorites"] = favoritesMap
                    }
                }

                response["placeNameHeaders"] = sortPlaceNames(metadataList)
                response["msg"] = messageSource?.getMessage("main.results", null, locale)
                response["status"] = ApiResponse.SUCCESS.status
            }
        }

        return response
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/timeline/all/dates","/api/v1/timeline/all/dates"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getAllTimeline(model: Model,locale: Locale): String {
        val response = mutableMapOf<String, Any?>()
        val favoritesMap = HashMap<String, HashMap<String, Any>>()
        val currentUserObj = model.getAttribute("currentUser") as User?
        response["allMetadata"] = mutableListOf<Metadata>()
        response["msg"] = messageSource?.getMessage("main.not.loggedin", null, locale)
        response["status"] = ApiResponse.FAIL.status
        response["favorites"] = favoritesMap
        response["placeNameHeaders"] = mutableListOf<String>()

        if (currentUserObj != null) {
            val metadataList = if (currentUserObj.getAuthority() == model.getAttribute("adminRole") || currentUserObj.getAuthority() == model.getAttribute("superRole")) {
                metadataRepository.findAllTimeline()
            } else {
                metadataRepository.findByAlbumMetadataByUserId(currentUserObj.getId())
            }

            response["allMetadata"] = metadataList

            response["placeNameHeaders"] = sortPlaceNames(metadataList)

            val favoriteCounts = favoriteRepository.countByMetadataIdIn(metadataList.map { it.getId() }.toList())

            if (favoriteCounts.count() > 0) {
                for (favoriteCount in favoriteCounts) {
                    favoritesMap[favoriteCount.getMetadataId()!!] = hashMapOf(
                        "favorite" to (favoriteCount.getUserId() == currentUserObj.getId()),
                        "count" to favoriteCount.getCount() as Any
                    )
                }
            }

            response["favorites"] = favoritesMap

            response["msg"] = ""
            response["status"] = ApiResponse.SUCCESS.status
        }

        return mapper.writeValueAsString(response)
    }

    fun deleteThumbnails(metadata: Metadata) {
        val possibleExtension = metadata.getFileName()?.substringAfterLast('.', "")?.lowercase()

        // Re-process thumbnails
        var thumbnailFile = metadata.getThumbnailPathCentered()
        if (!thumbnailFile.isNullOrBlank()) {
            val centeredTnFile = File(metadata.getThumbnailPathCentered()!!)
            if (centeredTnFile.exists()) {
                centeredTnFile.delete()
                logger.log(Level.INFO, "Centered thumbnail deleted: " + metadata.getThumbnailPathCentered())
            }
            val altCenteredTnFile = File(metadata.getThumbnailPathCentered()!!.substringBeforeLast(".") + "." + possibleExtension)
            if (altCenteredTnFile.exists()) {
                altCenteredTnFile.delete()
                logger.log(Level.INFO, "Alt centered thumbnail deleted: " + metadata.getThumbnailPathCentered())
            }
        } else {
            logger.log(Level.WARNING, "Centered path blank: " + metadata.getThumbnailPathCentered())
        }

        thumbnailFile = metadata.getMapMarkerPath()
        if (!thumbnailFile.isNullOrBlank()) {
            val mapTnFile = File(metadata.getMapMarkerPath()!!)
            if (mapTnFile.exists()) {
                mapTnFile.delete()
                logger.log(Level.INFO, "Map thumbnail deleted: " + metadata.getMapMarkerPath())
            }
            val altMapTnFile = File(metadata.getMapMarkerPath()!!.substringBeforeLast(".") + "." + possibleExtension)
            if (altMapTnFile.exists()) {
                altMapTnFile.delete()
                logger.log(Level.INFO, "Alt thumbnail deleted: " + metadata.getMapMarkerPath())
            }
        } else {
            logger.log(Level.WARNING, "Map path blank: " + metadata.getMapMarkerPath())
        }

        thumbnailFile = metadata.getThumbnailPathSmall()
        if (!thumbnailFile.isNullOrBlank()) {
            val smallTnFile = File(metadata.getThumbnailPathSmall()!!)
            if (smallTnFile.exists()) {
                smallTnFile.delete()
                logger.log(Level.INFO, "Small thumbnail deleted: " + metadata.getThumbnailPathSmall())
            }
            val altSmallTnFile = File(metadata.getThumbnailPathSmall()!!.substringBeforeLast(".") + "." + possibleExtension)
            if (altSmallTnFile.exists()) {
                altSmallTnFile.delete()
                logger.log(Level.INFO, "Alt small thumbnail deleted: " + metadata.getThumbnailPathSmall())
            }
        } else {
            logger.log(Level.WARNING, "Small thumbnail path blank: " + metadata.getThumbnailPathSmall())
        }

        val originalTnFile =
            File(metadata.getThumbnailPathCentered()?.replace("_centered.", "_original.")!!)
        if (originalTnFile.exists()) {
            originalTnFile.delete()
            logger.log(Level.INFO, "Original thumbnail deleted: " + metadata.getThumbnailPathCentered()?.replace("_centered.", "_original.")!!)
        } else {
            logger.log(Level.WARNING, "Original thumbnail not deleted: " + metadata.getThumbnailPathCentered()?.replace("_centered.", "_original.")!!)
        }
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "rescanMetadata",
            summary = "Rescan thumbnails and metadata.",
            description = "<strong>Rescan thumbnails and metadata.</strong><br>" +
                    "<pre><code>" +
                    "curl -X POST \"http://127.0.0.1:6624/api/v1/rescan/metadata\" \\\n" +
                    "-H \"Content-Type: application/json\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>Content-Type</td><td>header</td><td>string</td><td>required</td><td>application/json</td></tr>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>metadataIdList</td><td>body param</td><td>array</td><td>required</td><td>A list of metadata IDs to rescan</td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"metadataMap\": {\n" +
                    "           &lt;metadata_id&gt;: &lt;metadata&gt;\n" +
                    "    }\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>metadataMap.&lt;metadata_id&gt;</td><td>string</td><td>Metadata ID</td></tr>" +
                    "<tr><td>metadataMap.&lt;metadata_id&gt;.&lt;metadata&gt;</td><td>object</td><td>A <a href=\"#\" data-bs-toggle=\"modal\" data-bs-target=\"#propMetadataDocs\">Metadata</a> object</td></tr>" +
                    "</tbody></table>"
        )
    )
    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/rescan/metadata", "/api/v1/rescan/metadata"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @CacheEvict(value = ["allMetadata", "allMetadataByDate", "allMetadataByDateAndType", "allMetadataOnlyByDate", "allMetadataAndAttributesByDate", "singleMetadataRequest", "allAlbumMetadataWithCoordinates", "allMetadataWithCoordinates"], allEntries = true)
    fun rescanMetadata(@RequestBody requestBody: JsonNode, locale: Locale): String? {
//        println(requestBody)
        val metadataMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        val retMap = mutableMapOf<String,Metadata>()

        if (metadataMap.containsKey("metadataIdList")) {
            val metadataIdArray = metadataMap["metadataIdList"] as ArrayList<String>?
            if (!metadataIdArray.isNullOrEmpty()) {
                var errorDetected = false

                for (metadataId in metadataIdArray) {
                    val metadataObj = metadataRepository.findById(metadataId)
                    if (metadataObj.isPresent) {
                        val metadata = metadataObj.get()
                        val stringMetadata = Gson().toJson(metadata, Metadata::class.java)
                        var metadataCopy = Gson().fromJson(stringMetadata, Metadata::class.java)

                        val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
                        val sidecarDir = rootPath + relativeSidecarDir

                        val exifFile = FileUtils.getExifFile(metadataCopy.getFolder()!!, metadataCopy.getFileName()!!, relativeSidecarDir!!)

                        if (exifFile != null && exifFile.exists()) {
                            if (exifFile.delete()) {
                                deleteThumbnails(metadataCopy)

                                val metadataPath = metadataCopy.getPath()!!

                                metadataCopy = Metadata()

                                // Re-process metadata
                                val metadataProcessing = MetadataProcessing(
                                    apiVersion!!,
                                    File(metadataPath),
                                    sidecarDir,
                                    metadataCopy,
                                    geocodeUrl!!
                                )
                                metadataCopy = metadataProcessing.populateMetadata()

                                if (!metadataCopy.getLat().isNullOrBlank() && !metadataCopy.getLng().isNullOrBlank()) {
                                    val lat = metadataCopy.getLat().toString()
                                    val lng = metadataCopy.getLng().toString()
                                    val geoDataJson = TextUtils.getGeoData(geocodeUrl!!, lat, lng)

                                    val buildPlace = TextUtils.getPlaceNameFromJson(geoDataJson)
                                    if (buildPlace.isNotBlank()) {
                                        metadataCopy.setPlaceName(buildPlace)

                                        val engine = TimeZoneEngine.initialize()
                                        val maybeZoneId: Optional<ZoneId> =
                                            engine.query(
                                                lat.toString().toDouble(),
                                                lng.toString().toDouble()
                                            )
                                        val zone = ZoneId.of(maybeZoneId.get().id)
                                        val dt = LocalDateTime.now()
                                        val zdt: ZonedDateTime = dt.atZone(zone)
                                        val offset = zdt.offset
                                        metadataCopy.setTimeZone(offset.toString())
                                        logger.log(
                                            Level.INFO,
                                            "Place set for " + metadataCopy.getFileName()
                                        )
                                    }
                                }

                                val imageProcessing =
                                    ImageProcessing(apiVersion, File(metadataPath), sidecarDir, metadataCopy)
                                metadataCopy = imageProcessing.createThumbnails()!!

                                metadataCopy.setModifiedAt(getCurrentTimestamp())

                                if (metadataCopy.getId().isNotEmpty() && metadataCopy.getThumbnailSmallWidth() != null && metadataCopy.getThumbnailSmallHeight() != null && metadataCopy.getThumbnailUrlSmall() != null) {
                                    metadataCopy.setHidden(false)
                                    metadataRepository.save(metadataCopy)

                                    // Something was updated that changed the UUID, delete the old record
                                    if (metadataCopy.getId() != metadataId) {
                                        val metadataToDelete = metadataRepository.findByMetadataId(metadataId)

                                        if (metadataToDelete != null) {
                                            // Transfer comments
                                            val albumPhotoComments = mutableListOf(AlbumPhotoComment())
                                            val albumPhotoCommentList =
                                                albumPhotoCommentRepository.findByMetadataId(metadataId)
                                            if (albumPhotoCommentList != null) {
                                                for (albumPhotoComment in albumPhotoCommentList) {
                                                    if (albumPhotoComment != null) {
                                                        albumPhotoComment.setMetadataId(metadataCopy.getId())
                                                        albumPhotoComments.add(albumPhotoComment)
                                                    }
                                                }
                                            }
                                            if (albumPhotoComments.size > 0) {
                                                albumPhotoCommentRepository.saveAll(albumPhotoComments)
                                            }
                                            logger.log(
                                                Level.INFO,
                                                "Updated comment records for: " + metadataCopy.getId()
                                            )

                                            // Transfer favorites
                                            val favoritesList = mutableListOf(Favorite())
                                            val favorites = favoriteRepository.findAllByMetadataId(metadataId)
                                            if (favorites != null) {
                                                for (favorite in favorites) {
                                                    if (favorite != null) {
                                                        favorite.setMetadataId(metadataCopy.getId())
                                                        favoritesList.add(favorite)
                                                    }
                                                }
                                            }
                                            if (favoritesList.size > 0) {
                                                favoriteRepository.saveAll(favoritesList)
                                            }
                                            logger.log(
                                                Level.INFO,
                                                "Updated favorite records for: " + metadataCopy.getId()
                                            )

                                            // Transfer from keywords
                                            val keywordsList = mutableListOf(KeywordPhoto())
                                            val keywords = keywordPhotoRepository.findAllByMetadataId(metadataId)
                                            if (keywords != null) {
                                                for (keywordObj in keywords) {
                                                    if (keywordObj != null) {
                                                        keywordObj.setMetadataId(metadataCopy.getId())
                                                        keywordsList.add(keywordObj)
                                                    }
                                                }
                                            }
                                            if (keywordsList.size > 0) {
                                                keywordPhotoRepository.saveAll(keywordsList)
                                            }
                                            logger.log(
                                                Level.INFO,
                                                "Updated keywords records for: " + metadataCopy.getId()
                                            )

                                            // Transfer from album
                                            val albumsList = mutableListOf(AlbumPhoto())
                                            val albums = albumPhotoRepository.findAllByMetadataId(metadataId)
                                            if (albums != null) {
                                                for (album in albums) {
                                                    if (album != null) {
                                                        album.setMetadataId(metadataCopy.getId())
                                                        albumsList.add(album)
                                                    }
                                                }
                                            }
                                            if (albumsList.size > 0) {
                                                albumPhotoRepository.saveAll(albumsList)
                                            }
                                            logger.log(Level.INFO, "Updated album records for: " + metadataCopy.getId())

                                            // Delete tagged people
                                            val recognitionLabelPhotosList = mutableListOf(RecognitionLabelPhoto())
                                            val recognitionLabelPhotos =
                                                recognitionLabelPhotoRepository?.findByMetadataId(metadataId)
                                            if (recognitionLabelPhotos != null) {
                                                for (recognitionLabelPhoto in recognitionLabelPhotos) {
                                                    recognitionLabelPhotosList.add(recognitionLabelPhoto)
                                                }
                                            }
                                            if (recognitionLabelPhotosList.size > 0) {
                                                recognitionLabelPhotoRepository?.deleteAll(recognitionLabelPhotosList)
                                            }

                                            logger.log(Level.INFO, "Deleted tagged people records for: " + metadataCopy.getId())

                                            metadataRepository.delete(metadataToDelete)
                                            logger.log(
                                                Level.WARNING,
                                                "UUID changed to "+metadataCopy.getId()+", deleting old record "+metadataId+" for " + metadata.getPath()
                                            )
                                        }
                                    }

                                    retMap[metadataCopy.getId()] = metadataCopy
                                } else {
                                    logger.log(
                                        Level.WARNING,
                                        "Could not rescan data for " + metadata.getPath()
                                    )
                                    errorDetected = true
                                }
                            }
                        }
                    }
                }

                createVideoGif(metadataIdArray, metadataRepository, true)

                return if (errorDetected) {
                    resp["msg"] = messageSource?.getMessage("main.modal.saved.fail", null, locale)
                    resp["status"] = ApiResponse.WARN.status
                    resp["metadataMap"] = retMap
                    mapper.writeValueAsString(resp)
                } else {
                    resp["msg"] = messageSource?.getMessage("main.modal.saved", null, locale)
                    resp["status"] = ApiResponse.SUCCESS.status
                    resp["metadataMap"] = retMap
                    mapper.writeValueAsString(resp)
                }
            }
        }

        resp["metadataMap"] = retMap
        resp["msg"] = messageSource?.getMessage("main.modal.saved.fail", null, locale)
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }

    fun createVideoGif(metadataIdArray: ArrayList<String>?, metadataRepository: MetadataRepository, overwrite: Boolean) {
        if (metadataIdArray != null) {
            Thread {
                for (metadataId in metadataIdArray) {
                    ImageProcessing.Companion.createVideoGif(metadataId, metadataRepository, overwrite)
                }
            }.start()
        }
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/metadata/remove/{metadataId}"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Transactional
    @CacheEvict(value = ["allMetadata", "allMetadataByDate", "allMetadataByDateAndType", "allMetadataOnlyByDate", "allMetadataAndAttributesByDate", "singleMetadataRequest", "allAlbumMetadataWithCoordinates", "allMetadataWithCoordinates"], allEntries = true)
    fun removeMetadata(model: Model, @RequestBody requestBody: JsonNode, @PathVariable metadataId: String, locale: Locale): String? {
//        println(requestBody)
        val metadataMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})

        if (metadataMap.containsKey("id") &&
            metadataMap.containsKey("hidden") &&
            metadataMap["id"].toString() == metadataId
        ) {
            val metadataObj = metadataRepository.findById(metadataId)

            if (metadataObj.isPresent) {
                val isHidden = metadataMap["hidden"].toString().toBoolean()

                if (isHidden) {
                    metadataObj.get().setHidden(true)
                    metadataObj.get().setModifiedAt(getCurrentTimestamp())
                    removeMetadata(metadataId, locale)
                }

                // Update record
                metadataRepository.save(metadataObj.get())

                resp["msg"] = messageSource?.getMessage("main.modal.saved", null, locale)
                resp["status"] = ApiResponse.SUCCESS.status
                return mapper.writeValueAsString(resp)
            }
        }
        resp["msg"] = messageSource?.getMessage("main.modal.saved.fail", null, locale)
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/metadata/attributes"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun getMetadataAttributeData(model: Model,locale: Locale): String? {
        val attrResponse = getAllAttributeData(model)
        for ((k, v) in attrResponse) {
            resp[k] = v
        }

        resp["msg"] = messageSource?.getMessage("main.success", null, locale)
        resp["status"] = ApiResponse.SUCCESS.status
        return mapper.writeValueAsString(resp)
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "updateMetadata",
            summary = "Update metadata.",
            description = "<strong>Update metadata.</strong><br>" +
                    "<pre><code>" +
                    "curl -X PUT \"http://127.0.0.1:6624/api/v1/update/metadata/{metadataId}\" \\\n" +
                    "-H \"Content-Type: application/json\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\" \\\n" +
                    "-d '{\n" +
                    "     \"id\": \"&lt;metadata_id&gt;\",\n" +
                    "     \"title\": \"&lt;title&gt;\",\n" +
                    "     \"description\": \"&lt;description&gt;\",\n" +
                    "     \"latlng\": \"&lt;lat&gt;,&lt;lng&gt;\",\n" +
                    "     \"offset\": \"&lt;taken_offset&gt;\",\n" +
                    "     \"albumnames\": \"&lt;album_name_1,album_name_n&gt;\",\n" +
                    "     \"keywords\": \"&lt;keyword_1&gt;,&lt;keyword_n&gt;\",\n" +
                    "     \"tagpeople\": \"&lt;subject_name_1&gt;,&lt;subject_name_n&gt;\",\n" +
                    "     \"camera\": \"&lt;camera_name&gt;\",\n" +
                    "     \"lens\": \"&lt;lens_name&gt;\",\n" +
                    "     \"time\": \"&lt;time_taken&gt;\",\n" +
                    "     \"year\": \"&lt;year_taken&gt;\",\n" +
                    "     \"month\": \"&lt;month_taken&gt;\",\n" +
                    "     \"day\": \"&lt;day_taken&gt;\",\n" +
                    "     \"hidden\": &lt;hidden_flag&gt;,\n" +
                    "     \"isObject\": &lt;is_object_flag&gt;\n" +
                    "    }'" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>Content-Type</td><td>header</td><td>string</td><td>required</td><td>application/json</td></tr>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>id</td><td>body param</td><td>string</td><td>required</td><td>Metadata id associated with media to edit</td></tr>" +
                    "<tr><td>title</td><td>body param</td><td>string</td><td>required</td><td>The title of media</td></tr>" +
                    "<tr><td>description</td><td>body param</td><td>string</td><td>required</td><td>The description of media</td></tr>" +
                    "<tr><td>latlng</td><td>body param</td><td>string</td><td>required</td><td>Tag media with a latitude and longitude set separated by a comma</td></tr>" +
                    "<tr><td>offset</td><td>body param</td><td>string</td><td>required</td><td>Tag media with the offset time media was taken</td></tr>" +
                    "<tr><td>albumnames</td><td>body param</td><td>string</td><td>required</td><td>Tag media with a comma separated list of album names</td></tr>" +
                    "<tr><td>keywords</td><td>body param</td><td>string</td><td>required</td><td>Tag media with a comma separated list of keywords</td></tr>" +
                    "<tr><td>tagpeople</td><td>body param</td><td>string</td><td>required</td><td>Tag media with a comma separated list of subjects</td></tr>" +
                    "<tr><td>camera</td><td>body param</td><td>string</td><td>required</td><td>Tag media with the name of the camera used</td></tr>" +
                    "<tr><td>lens</td><td>body param</td><td>string</td><td>required</td><td>Tag media with the name of the lens used</td></tr>" +
                    "<tr><td>time</td><td>body param</td><td>string</td><td>required</td><td>Tag media with the time media was taken in format HH:MM:SS</td></tr>" +
                    "<tr><td>year</td><td>body param</td><td>string</td><td>required</td><td>Tag media with the year media was taken</td></tr>" +
                    "<tr><td>month</td><td>body param</td><td>string</td><td>required</td><td>Tag media with the month media was taken</td></tr>" +
                    "<tr><td>day</td><td>body param</td><td>string</td><td>required</td><td>Tag media with the day media was taken</td></tr>" +
                    "<tr><td>hidden</td><td>body param</td><td>boolean</td><td>required</td><td>Flag to hide/unhide media</td></tr>" +
                    "<tr><td>isObject</td><td>body param</td><td>boolean</td><td>required</td><td>Flag to tag media as an object</td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"msg\": \"\",\n" +
                    "    \"message\": \"\",\n" +
                    "    \"recognitionLabels\": [\n" +
                    "        {\n" +
                    "            \"id\": &ltsubject_id&gt;,\n" +
                    "            \"name\": \"&ltsubject_name&gt;\"\n" +
                    "        },\n" +
                    "    ],\n" +
                    "    \"allAlbumList\": [\n" +
                    "        {\n" +
                    "            \"id\": &lt;album_id&gt;,\n" +
                    "            \"name\": \"&lt;name_of_album&gt;\",\n" +
                    "            \"coverUrl\": \"&lt;relative_url&gt;\",\n" +
                    "            \"shareUrl\": \"&lt;public_url_key&gt;\"\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"timeOffsets\": [\n" +
                    "        \"-12:00\",\n" +
                    "        \"-11:00\",\n" +
                    "        \"-10:00\",\n" +
                    "        \"-09:30\",\n" +
                    "        \"-09:00\",\n" +
                    "        \"-08:00\",\n" +
                    "        \"-07:00\",\n" +
                    "        \"-06:00\",\n" +
                    "        \"-05:00\",\n" +
                    "        \"-04:30\",\n" +
                    "        \"-04:00\",\n" +
                    "        \"-03:30\",\n" +
                    "        \"-03:00\",\n" +
                    "        \"-02:00\",\n" +
                    "        \"-01:00\",\n" +
                    "        \"±00:00\",\n" +
                    "        \"+01:00\",\n" +
                    "        \"+02:00\",\n" +
                    "        \"+03:00\",\n" +
                    "        \"+03:30\",\n" +
                    "        \"+04:00\",\n" +
                    "        \"+04:30\",\n" +
                    "        \"+05:00\",\n" +
                    "        \"+05:30\",\n" +
                    "        \"+05:45\",\n" +
                    "        \"+06:00\",\n" +
                    "        \"+06:30\",\n" +
                    "        \"+07:00\",\n" +
                    "        \"+08:00\",\n" +
                    "        \"+08:45\",\n" +
                    "        \"+09:00\",\n" +
                    "        \"+09:30\",\n" +
                    "        \"+10:00\",\n" +
                    "        \"+11:00\",\n" +
                    "        \"+11:30\",\n" +
                    "        \"+12:00\",\n" +
                    "        \"+13:00\",\n" +
                    "        \"+14:00\"\n" +
                    "    ],\n" +
                    "    \"keywords\": \"&lt;keyword_1,keyword_n&gt;\",\n" +
                    "    \"cameras\": \"&lt;camera_1,camera_n&gt;\",\n" +
                    "    \"lenses\": \"&lt;lens_1,lens_n&gt;\"" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>recognitionLabels[].id</td><td>int</td><td>Album ID the subject is associated with</td></tr>" +
                    "<tr><td>recognitionLabels[].name</td><td>string</td><td>Subject name</td></tr>" +
                    "<tr><td>allAlbumList[].id</td><td>int</td><td>The album ID</td></tr>" +
                    "<tr><td>allAlbumList[].name</td><td>string</td><td>The album name</td></tr>" +
                    "<tr><td>allAlbumList[].coverUrl</td><td>string</td><td>Relative URL for the album cover image</td></tr>" +
                    "<tr><td>allAlbumList[].shareUrl</td><td>string</td><td>Part of the share URL endpoint for public sharing</td></tr>" +
                    "<tr><td>timeOffsets</td><td>array</td><td>An array of valid time offsets</td></tr>" +
                    "<tr><td>keywords</td><td>string</td><td>A comma separated string of all keywords</td></tr>" +
                    "<tr><td>cameras</td><td>string</td><td>A comma separated string of all cameras</td></tr>" +
                    "<tr><td>lenses</td><td>string</td><td>A comma separated string of all lenses</td></tr>" +
                    "</tbody></table>"
        )
    )
    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/metadata/update/{metadataId}","/api/v1/update/metadata/{metadataId}"], method = [RequestMethod.PUT], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @CacheEvict(value = ["allMetadata", "allMetadataByDate", "allMetadataByDateAndType", "allMetadataOnlyByDate", "allMetadataAndAttributesByDate", "singleMetadataRequest", "allAlbumMetadataWithCoordinates", "allMetadataWithCoordinates"], allEntries = true)
    fun updateMetadata(model: Model, @RequestBody requestBody: JsonNode, @PathVariable metadataId: String, response: HttpServletResponse, locale: Locale): String? {
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
            metadataMap.containsKey("description") &&
            metadataMap.containsKey("albumnames") &&
            metadataMap.containsKey("tagpeople") &&
            metadataMap.containsKey("hidden") &&
            metadataMap.containsKey("isObject") &&
            metadataMap.containsKey("camera") &&
            metadataMap.containsKey("lens") &&
            metadataMap.containsKey("duration") &&
            metadataMap["id"].toString() == metadataId
        ) {
            resp["msg"] = messageSource?.getMessage("main.modal.saved", null, locale)
            resp["status"] = ApiResponse.SUCCESS.status
            resp["keywordsIdentified"] = ""

            val metadataObj = metadataRepository.findById(metadataId)
            resp["shortPlaceName"] = ""

            if (TextUtils.metadataInputValidation(
                    metadataMap["day"].toString().toInt(),
                    metadataMap["month"].toString().toInt(),
                    metadataMap["year"].toString().toInt(),
                    metadataMap["time"].toString(),
                    metadataMap["offset"].toString(),
                    metadataMap["duration"].toString()) &&
                metadataObj.isPresent)
            {
                val metricsUtil = MetricsUtil()

                metricsUtil.start("Metadata Update - Process albums")
                val currentUserObj = model.getAttribute("currentUser") as User?
                val albumIdAddedList = mutableListOf<Int>()

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
                    val albumPhotoList: ArrayList<AlbumPhoto> = ArrayList()

                    for (albumNameRaw in albumsArray) {

                        val albumId = processAlbum(albumNameRaw, currentUserObj, metadataObj.get())

                        if (albumId > 0) {
                            val albumPhotoCount =
                                albumPhotoRepository.countByMetadataIdAndAlbumId(metadataId, albumId)!!
                            if (albumPhotoCount == 0) {
                                val albumPhotoObj = AlbumPhoto()
                                albumPhotoObj.setMetadataId(metadataId)
                                albumPhotoObj.setAlbumId(albumId)
                                albumPhotoObj.setCreatedAt(getCurrentTimestamp())
                                albumPhotoObj.setModifiedAt(getCurrentTimestamp())
                                albumPhotoList.add(albumPhotoObj)
                                if (!albumIdAddedList.contains(albumId)) {
                                    albumIdAddedList.add(albumId)
                                }
                            }

                            if (currentAlbumIdList.contains(albumId)) {
                                // Collect to delete
                                val indexToRemove = currentAlbumIdList.indexOf(albumId)
                                currentAlbumIdList.removeAt(indexToRemove)
                            }
                        }
                    }

                    if (albumPhotoList.isNotEmpty()) {
                        albumPhotoRepository.saveAll(albumPhotoList)
                    }
                }

                if (currentAlbumIdList.isNotEmpty()) {
                    var albumListString = ""
                    for (albumId in currentAlbumIdList) {
                        val count = MetadataProcessing.Companion.deleteAlbumPhoto(metadataRepository, albumRepository, albumPhotoRepository, metadataId, albumId)

                        if (count != null && count.toInt() == 0) {
                            MetadataProcessing.Companion.deleteAlbum(
                                albumRepository,
                                albumPhotoRepository,
                                userAlbumRepository,
                                commentRepository,
                                albumPhotoCommentRepository,
                                albumCommentRepository,
                                albumId
                            )

                            val albumObj = albumRepository.findAlbumById(albumId)
                            if (albumObj != null) {
                                albumListString += albumObj.getName() + ","
                            }
                        }
                    }

                    val admins = userRepository.findAllAdmins()

                    if (admins.count() > 0 && albumListString.length > 0) {
                        albumListString = albumListString.dropLast(1)
                        val notificationObjList = mutableListOf<Notification>()
                        val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                        sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
                        for (admin in admins) {
                            var language = admin.getLanguage()
                            if (language == null) {
                                language = "en"
                            }

                            var locale = Locale(language)
                            val notificationObj = Notification()
                            notificationObj.setUserId(admin.getId())
                            notificationObj.setCreatedAt(getCurrentTimestamp())
                            notificationObj.setModifiedAt(getCurrentTimestamp())
                            notificationObj.setRead(false)
                            notificationObj.setMessage(messageSource?.getMessage("main.notification.albums.delete", arrayOf(albumListString), locale) +"- ${sdtf.format(Date())}")
                            notificationObjList.add(notificationObj)
                        }
                        if (notificationObjList.isNotEmpty()) {
                            notificationRepository.saveAll(notificationObjList)
                        }
                    }
                }
                metricsUtil.end()

                metricsUtil.start("Metadata Update - Process people")
                // Process tagged people
                val taggedPeople = metadataMap["tagpeople"].toString()
                val isObject = metadataMap["isObject"].toString().toBoolean()

                processPeople(
                    model.getAttribute("settings") as Settings,
                    metadataObj.get(),
                    taggedPeople,
                    isObject,
                    false
                )

                cleanupOrphanedSubjects()
                metricsUtil.end()

                metricsUtil.start("Metadata Update - attributes")
                if (metadataMap["title"].toString().trim() == "") {
                    metadataObj.get().setTitle(metadataObj.get().getFileName())
                } else if (metadataObj.get().getTitle() != metadataMap["title"].toString().trim()) {
                    metadataObj.get().setTitle(metadataMap["title"].toString().trim())
                }
                metricsUtil.end()
                metricsUtil.start("Metadata Update - description")
                if (metadataMap["description"].toString().trim() == "") {
                    metadataObj.get().setDescription(null)
                } else if (metadataObj.get().getDescription() != metadataMap["description"].toString().trim()) {
                    metadataObj.get()
                        .setDescription(metadataMap["description"].toString().trim())
                }
                metricsUtil.end()
                metricsUtil.start("Metadata Update - camera")
                if (metadataMap["camera"].toString().trim() != "") {
                    val camera = metadataMap["camera"].toString().trim()
                    metadataObj.get().setCamera(camera)

//                    val cameraTypes = metadataRepository.findByCameraTypeAlphabetical()
//                    for (cameraType in cameraTypes) {
//                        if (camera.trim().lowercase() == cameraType.trim().lowercase()) {
//                            camera = cameraType
//                            break
//                        }
//                    }
//
//                    if (metadataObj.get().getCamera() != camera) {
//                        metadataObj.get().setCamera(camera)
//                    }
                } else {
                    metadataObj.get().setCamera(null)
                }
                metricsUtil.end()
                metricsUtil.start("Metadata Update - lens")
                if (metadataMap["lens"].toString().trim() != "") {
                    val lens = metadataMap["lens"].toString().trim()
                    metadataObj.get().setLens(lens)

//                    val lensTypes = metadataRepository.findByLensTypeAlphabetical()
//                    for (lensType in lensTypes) {
//                        if (lens.trim().lowercase() == lensType.trim().lowercase()) {
//                            lens = lensType
//                            break
//                        }
//                    }
//
//                    if (metadataObj.get().getLens() != lens) {
//                        metadataObj.get().setLens(lens)
//                    }
                } else {
                    metadataObj.get().setLens(null)
                }
                metricsUtil.end()
                metricsUtil.start("Metadata Update - duration")
                if (metadataObj.get().getType()?.contains("video")!! &&
                    metadataMap["duration"].toString().trim() != "" && metadataMap["duration"].toString().trim() != "0:00" &&
                    metadataMap["duration"].toString().trim() != "00:00" && metadataMap["duration"].toString().trim() != "0:00:00" &&
                    metadataMap["duration"].toString().trim() != "00:00:00")
                {
                    metadataObj.get().setDuration(metadataMap["duration"].toString().trim())
                } else {
                    metadataObj.get().setDuration(null)
                }
                metricsUtil.end()
                metricsUtil.start("Metadata Update - year")
                if (metadataMap["year"].toString() == "") {
                    metadataObj.get().setYear(null)
                } else if (metadataObj.get().getYear() != metadataMap["year"].toString().toInt()) {
                    metadataObj.get().setYear(StringEscapeUtils.escapeHtml4(metadataMap["year"].toString()).toInt())
                }
                metricsUtil.end()
                metricsUtil.start("Metadata Update - month")
                if (metadataMap["month"].toString() == "") {
                    metadataObj.get().setMonth(null)
                } else if (metadataObj.get().getMonth() != metadataMap["month"].toString().toInt()) {
                    metadataObj.get().setMonth(StringEscapeUtils.escapeHtml4(metadataMap["month"].toString()).toInt())
                }
                metricsUtil.end()
                metricsUtil.start("Metadata Update - day")
                if (metadataMap["day"].toString() == "") {
                    metadataObj.get().setDay(null)
                } else if (metadataObj.get().getDay() != metadataMap["day"].toString().toInt()) {
                    metadataObj.get().setDay(StringEscapeUtils.escapeHtml4(metadataMap["day"].toString()).toInt())
                }
                // Original taken date can be looked at in EXIF data
                if (metadataMap["year"].toString() != "" && metadataMap["month"].toString() != "" && metadataMap["day"].toString() != "") {
                    // Set taken date
                    val yearTaken = metadataMap["year"].toString().toInt()
                    val monthTaken = metadataMap["month"].toString().toInt()
                    val dayTaken = metadataMap["day"].toString().toInt()

                    val takenAt = metadataObj.get().getTakenAt()
                    if (takenAt != null) {
                        val takenArr = takenAt.split(" ")
                        if (takenArr.count() == 2) {
                            val month = if (monthTaken > 9) monthTaken else "0$monthTaken"
                            val day = if (dayTaken > 9) dayTaken else "0$dayTaken"
                            val time = takenArr[1]
                            val takenVal = "$yearTaken-$month-$day $time"
                            metadataObj.get().setTakenAt(takenVal)
                        }
                    }
                }

                metricsUtil.end()
                metricsUtil.start("Metadata Update - time")
                if (metadataMap["time"].toString() == "") {
                    metadataObj.get().setTime(null)
                } else if (metadataObj.get().getTime() != metadataMap["time"].toString()) {
                    metadataObj.get().setTime(StringEscapeUtils.escapeHtml4(metadataMap["time"].toString()))
                }
                metricsUtil.end()
                metricsUtil.start("Metadata Update - offset")
                if (metadataMap["offset"].toString() == "") {
                    metadataObj.get().setTimeZone(null)
                } else if (metadataObj.get().getTimeZone() != metadataMap["offset"].toString()) {
                    metadataObj.get().setTimeZone(StringEscapeUtils.escapeHtml4(metadataMap["offset"].toString()))
                }
                metricsUtil.end()

                metricsUtil.start("Metadata Update - Process keywords")
                keywordPhotoRepository.deleteAllByMetadataId(metadataId)
                if (metadataMap["keywords"].toString().isNotBlank()) {
                    var keywords = metadataMap["keywords"].toString().trim()
                    if (keywords.last() == ',') {
                        keywords = keywords.dropLast(1)
                    }
                    val keywordList = keywords.split(",").map { it.trim() }
                    processKeywords(keywordList, metadataId)
                }

                val keywordIdsToDelete = keywordRepository.findAllOrphanedKeywordIds()
                if (keywordIdsToDelete.count() > 0) {
                    keywordRepository.deleteAllById(keywordIdsToDelete)
                }
                metricsUtil.end()

                var setAndSave = false

                metricsUtil.start("Metadata Update - Process location")

                if (metadataMap["latlng"].toString() == "") {
                    metadataObj.get().setLat(null)
                    metadataObj.get().setLng(null)
                    metadataObj.get().setPlaceName(null)
                } else {
                    val latlng = metadataMap["latlng"].toString()
                    val latlngArray = latlng.split(",")

                    if (latlngArray.size == 2) {
                        val newlat = latlngArray[0].trim()
                        val newlng = latlngArray[1].trim()

                        if (metadataObj.get().getLat() != newlat || metadataObj.get().getLng() != newlng) {
                            setAndSave = true

                            processCoordinates(metadataMap, metadataObj, model.getAttribute("locale").toString())
                        }
                    }
                }
                metricsUtil.end()

                // Update record
                if (!setAndSave) {
                    metricsUtil.start("Metadata Update - Update record")
                    metadataObj.get().setModifiedAt(getCurrentTimestamp())
                    metadataRepository.save(metadataObj.get())
                    metricsUtil.end()
                }

                notifyAlbumUpdate(albumIdAddedList,currentUserObj, locale)

                metricsUtil.start("Metadata Update - Getting attributes")
                val attrResponse = getAllAttributeData(model)
                for ((k, v) in attrResponse) {
                    resp[k] = v
                }
                metricsUtil.end()

                return mapper.writeValueAsString(resp)
            } else {
                return TextUtils.returnForbiddenError(response)
            }
        }
        logger.log(Level.WARNING, "Updating metadata failed. Could not save.")
        resp["msg"] = messageSource?.getMessage("main.modal.saved.fail", null, locale)
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }

    fun processCoordinates(metadataMap: Map<String, Any>, metadataObj: Optional<Metadata?>, locale: String? = null) {
        if (metadataObj.isPresent && metadataMap.containsKey("latlng")) {
            Thread {
                val coordinateMap = TextUtils.processCoordinates(geocodeUrl!!, metadataMap["latlng"].toString(), locale)
                if (coordinateMap["lat"] != null && coordinateMap["lng"] != null) {
                    metadataObj.get().setLat(coordinateMap["lat"])
                    metadataObj.get().setLng(coordinateMap["lng"])
                }

                if (coordinateMap["place"] != null) {
                    metadataObj.get().setPlaceName(coordinateMap["place"])
                    resp["shortPlaceName"] = TextUtils.formatPlaceNameForHeader(coordinateMap["place"])
                }
                if (coordinateMap["timezone"] != null) {
                    metadataObj.get().setTimeZone(coordinateMap["timezone"])
                }

                metadataObj.get().setModifiedAt(getCurrentTimestamp())
                metadataRepository.save(metadataObj.get())
            }.start()
        }
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/metadata/update/batch/coordinates","/api/v1/update/metadata/batch/coordinates"], method = [RequestMethod.PUT], consumes = ["application/json"], produces = ["application/json"])
    @CacheEvict(value = ["allMetadata", "allMetadataByDate", "allMetadataByDateAndType", "allMetadataOnlyByDate", "allMetadataAndAttributesByDate", "singleMetadataRequest", "allAlbumMetadataWithCoordinates", "allMetadataWithCoordinates"], allEntries = true)
    @ResponseBody
    fun updateBatchLocationMetadata(model: Model, @RequestBody requestBody: JsonNode, response: HttpServletResponse, locale: Locale): String? {
        resp["msg"] = messageSource?.getMessage("main.fail", null, locale)
        resp["status"] = ApiResponse.FAIL.status

        val metadataMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})

        if (metadataMap.containsKey("ids") &&
            metadataMap.containsKey("latlng")
        ) {
            val coordArray = metadataMap["latlng"].toString().split(",")
            val idArray: Array<String>? = mapper.readValue(metadataMap["ids"].toString(), object : TypeReference<Array<String>>() {})

            if (idArray != null && coordArray.size == 2 && idArray.isNotEmpty()) {
                setCoordinatesCR(idArray, coordArray, model.getAttribute("locale").toString())

                resp["msg"] = messageSource?.getMessage("main.modal.saved", null, locale)
                resp["status"] = ApiResponse.SUCCESS.status
            }

            resp["msg"] = messageSource?.getMessage("main.modal.saved", null, locale)
            resp["status"] = ApiResponse.SUCCESS.status
        }

        return mapper.writeValueAsString(resp)
    }

    fun setCoordinatesCR(idArray: Array<String>?, coordArray: List<String>, locale: String? = null) {
        Thread {
            val metadataList = mutableListOf<Metadata>()
            if (!idArray.isNullOrEmpty() && coordArray.size == 2) {
                for (metadataId in idArray) {
                    val metadata = setCoordinates(metadataId, coordArray[0], coordArray[1], locale)
                    metadataList.add(metadata)
                }
            }
            if (metadataList.size > 0) {
                metadataRepository.saveAll(metadataList)
            }
        }.start()
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/metadata/update/coordinates/{metadataId}","/api/v1/update/metadata/coordinates/{metadataId}"], method = [RequestMethod.PUT], consumes = ["application/json"], produces = ["application/json"])
    @CacheEvict(value = ["allMetadata", "allMetadataByDate", "allMetadataByDateAndType", "allMetadataOnlyByDate", "allMetadataAndAttributesByDate", "singleMetadataRequest", "allAlbumMetadataWithCoordinates", "allMetadataWithCoordinates"], allEntries = true)
    @ResponseBody
    fun updateLocationMetadata(model: Model, @RequestBody requestBody: JsonNode, @PathVariable metadataId: String, response: HttpServletResponse, locale: Locale): String? {
//        println(requestBody)
        var metadata = Metadata()

        resp["msg"] = messageSource?.getMessage("main.fail", null, locale)
        resp["status"] = ApiResponse.FAIL.status
        resp["metadata"] = metadata
        resp["shortPlaceName"] = ""

        val metadataMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})

        if (metadataMap.containsKey("id") &&
            metadataMap.containsKey("latlng") &&
            metadataMap["id"].toString() == metadataId
        ) {
            val coordArray = metadataMap["latlng"].toString().split(",")
            if (coordArray.size == 2) {
                metadata = setCoordinates(metadataMap["id"].toString(), coordArray[0], coordArray[1], model.getAttribute("locale").toString())
                resp["shortPlaceName"] = TextUtils.formatPlaceNameForHeader(metadata.getPlaceName())
                metadataRepository.save(metadata)

                resp["metadata"] = metadata
                resp["msg"] = messageSource?.getMessage("main.modal.saved", null, locale)
                resp["status"] = ApiResponse.SUCCESS.status
            }
        }

        return mapper.writeValueAsString(resp)
    }

    private fun setCoordinates(metadataId: String, lat: String, lng: String, locale: String? = null): Metadata {

        var coordinateMap = mapOf<String, String?>()
        if (lat != "" && lng != "" && metadataId != "") {

            val metadataObj = metadataRepository.findById(metadataId)

            if (metadataObj.isPresent) {
                coordinateMap = TextUtils.processCoordinates(geocodeUrl!!, "${lat.trim()},${lng.trim()}", locale)
                if (coordinateMap["lat"] != null && coordinateMap["lng"] != null) {
                    metadataObj.get().setLat(coordinateMap["lat"])
                    metadataObj.get().setLng(coordinateMap["lng"])
                }

                if (coordinateMap["place"] != null) {
                    metadataObj.get().setPlaceName(coordinateMap["place"])
                }
                if (coordinateMap["timezone"] != null) {
                    metadataObj.get().setTimeZone(coordinateMap["timezone"])
                }

                metadataObj.get().setModifiedAt(getCurrentTimestamp())

                return metadataObj.get()
            }

            return metadataObj.get()
        }

        return Metadata()
    }

    private fun notifyAlbumUpdate(albumIdAddedList: MutableList<Int>, currentUserObj: User?, locale: Locale) {
        var adminAlbumsMessage = ""
        val filteredUserAlbumsMap = mutableMapOf<Int,MutableList<String>>()
        val admins = userRepository.findAllAdmins()
        var imageUrl: String? = null

        for (albumId in albumIdAddedList) {
            if (currentUserObj != null) {
                val album = albumRepository.findAlbumById(albumId)

                adminAlbumsMessage += "<a href='album/$albumId' target='_blank'>${album?.getName()}</a>,"

                val userList = userRepository.findDistinctUserByAlbumId(albumId)

                if (userList != null) {
                    for (user in userList) {
                        if (filteredUserAlbumsMap[user!!.getId()] == null) {
                            filteredUserAlbumsMap[user.getId()] = mutableListOf()
                        }
                        filteredUserAlbumsMap[user.getId()]?.add("<a href='album/$albumId' target='_blank'>${album?.getName()}</a>")
                    }
                }

                imageUrl = album?.getCoverUrl()
            }
        }

        adminAlbumsMessage = adminAlbumsMessage.dropLast(1)

        var coverUrl = ""
        if (imageUrl != null) {
            val metadata = metadataRepository.findByThumbnailCentered(imageUrl)
            if (metadata != null) {
                coverUrl = "/api/v1/thumbnails/centered/"+metadata.getId()
            }
        }

        val notificationObjList = mutableListOf<Notification>()
        if (adminAlbumsMessage.isNotEmpty()) {
            val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
            sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())

            for (admin in admins) {
                var language = admin.getLanguage()
                if (language == null) {
                    language = "en"
                }

                var locale = Locale(language)
                val notificationObj = Notification()
                notificationObj.setUserId(admin.getId())
                notificationObj.setCreatedAt(getCurrentTimestamp())
                notificationObj.setModifiedAt(getCurrentTimestamp())
                notificationObj.setImageUrl(coverUrl)
                notificationObj.setRead(false)
                notificationObj.setMessage(messageSource?.getMessage("main.notification.timeline.photos", null, locale)+"$adminAlbumsMessage - ${sdtf.format(Date())}.")
                notificationObjList.add(notificationObj)
            }
        }

        val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
        sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())

        filteredUserAlbumsMap.forEach { entry ->
            val userId = entry.key
            val albumLinks = entry.value

            var message = ""
            for (albumLink in albumLinks) {
                message += "$albumLink,"
            }
            message = message.dropLast(1)

            var user = userRepository.findById(userId)
            var language = user.get().getLanguage()
            if (language == null) {
                language = "en"
            }

            var locale = Locale(language)
            val notificationObj = Notification()
            notificationObj.setUserId(userId)
            notificationObj.setCreatedAt(getCurrentTimestamp())
            notificationObj.setModifiedAt(getCurrentTimestamp())
            notificationObj.setImageUrl(coverUrl)
            notificationObj.setRead(false)
            notificationObj.setMessage(messageSource?.getMessage("main.notification.timeline.photos", null, locale)+"$message - ${sdtf.format(Date())}.")
            notificationObjList.add(notificationObj)
        }

        if (notificationObjList.isNotEmpty()) {
            notificationRepository.saveAll(notificationObjList)
        }
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/metadata/remove/batch"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Transactional
    @CacheEvict(value = ["allMetadata", "allMetadataByDate", "allMetadataByDateAndType", "allMetadataOnlyByDate", "allMetadataAndAttributesByDate", "singleMetadataRequest", "allAlbumMetadataWithCoordinates", "allMetadataWithCoordinates"], allEntries = true)
    fun removeBatchMetadata(model: Model, @RequestBody requestBody: JsonNode, locale: Locale): String? {
//        println(requestBody)
        val batchMetadataMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})

        val settings = model.getAttribute("settings") as Settings

        val headers = HttpHeaders()
        headers.contentType = MediaType.MULTIPART_FORM_DATA
        headers.add("x-api-key", settings.getCompreFaceKey())

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

            var index = 0
            for (id in idArray) {
                index++
                val metadataObj: Optional<Metadata?> = metadataRepository.findById(id)
                val metadata = metadataObj.get()

                if (isHidden) {
                    val dtf = DateTimeFormatter.ofPattern(getCommonDateFormat())
                    val now = LocalDateTime.now()
                    val adjustedNow = now.plusSeconds(index.toLong())

                    metadata.setModifiedAt(dtf.format(adjustedNow))
                    metadata.setHidden(true)
                    removeMetadata(id, locale)
                }

                metadataList.add(metadata)
            }

            if (metadataList.isNotEmpty()) {
                // Update record
                metadataRepository.saveAll(metadataList)

                resp["msg"] = messageSource?.getMessage("main.modal.saved", null, locale)
                resp["status"] = ApiResponse.SUCCESS.status
                return mapper.writeValueAsString(resp)
            }
        }
        logger.log(Level.WARNING, "Removing batch metadata failed. Could not save.")
        resp["msg"] = messageSource?.getMessage("main.modal.saved.fail", null, locale)
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "updateBatchMetadata",
            summary = "Update a batch of metadata.",
            description = "<strong>Update a batch of metadata.</strong><br>" +
                    "<pre><code>" +
                    "curl -X PUT \"http://127.0.0.1:6624/api/v1/update/metadata/batch\" \\\n" +
                    "-H \"Content-Type: application/json\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\" \\\n" +
                    "-d '{\n" +
                    "     \"batchMetadataIds\": [\"&lt;metadata_id_1&gt;\",\"&lt;metadata_id_n&gt;\"],\n" +
                    "     \"batchhidden\": &lt;batch_hidden_flag&gt;,\n" +
                    "     \"batchisobject\": &lt;batch_is_object_flag&gt;,\n" +
                    "     \"cameraBatchData\": \"&lt;batch_camera_name&gt;\",\n" +
                    "     \"lensBatchData\": \"&lt;batch_lens_name&gt;\",\n" +
                    "     \"yearTakenBatchData\": \"&lt;batch_year_taken&gt;\",\n" +
                    "     \"monthTakenBatchData\": \"&lt;batch_month_taken&gt;\",\n" +
                    "     \"dayTakenBatchData\": \"&lt;batch_day_taken&gt;\",\n" +
                    "     \"offsetTakenBatchData\": \"&lt;batch_taken_offset&gt;\",\n" +
                    "     \"albumNameInput\": \"&lt;album_name_1&gt;,&lt;album_name_n&gt;\",\n" +
                    "     \"tagBatchDataInput\": \"&lt;subject_name_1&gt;,&lt;subject_name_n&gt;\",\n" +
                    "     \"keywordsBatchData\": \"&lt;keyword_1&gt;,&lt;keyword_n&gt;\",\n" +
                    "     \"latlngBatchData\": \"&lt;lat&gt;,&lt;lng&gt;\"\n" +
                    "    }'" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>Content-Type</td><td>header</td><td>string</td><td>required</td><td>application/json</td></tr>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>batchMetadataIds</td><td>body param</td><td>array</td><td>required</td><td>A list of media to batch edit</td></tr>" +
                    "<tr><td>batchhidden</td><td>body param</td><td>boolean</td><td>required</td><td>Flag to batch hide/unhide media. If left blank or null, this value will not be updated.</td></tr>" +
                    "<tr><td>batchisobject</td><td>body param</td><td>boolean</td><td>required</td><td>Flag to batch tag media as an object. If left blank or null, this value will not be updated.</td></tr>" +
                    "<tr><td>cameraBatchData</td><td>body param</td><td>string</td><td>required</td><td>Batch tag media with the name of the camera used. If left blank or null, this value will not be updated.</td></tr>" +
                    "<tr><td>lensBatchData</td><td>body param</td><td>string</td><td>required</td><td>Batch tag media with the name of the lens used. If left blank or null, this value will not be updated.</td></tr>" +
                    "<tr><td>yearTakenBatchData</td><td>body param</td><td>string</td><td>required</td><td>Batch tag media with the year media was taken. If left blank or null, this value will not be updated.</td></tr>" +
                    "<tr><td>monthTakenBatchData</td><td>body param</td><td>string</td><td>required</td><td>Batch tag media with the month media was taken. If left blank or null, this value will not be updated.</td></tr>" +
                    "<tr><td>dayTakenBatchData</td><td>body param</td><td>string</td><td>required</td><td>Batch tag media with the day media was taken. If left blank or null, this value will not be updated.</td></tr>" +
                    "<tr><td>offsetTakenBatchData</td><td>body param</td><td>string</td><td>required</td><td>Batch tag media with the offset time media was taken. If left blank or null, this value will not be updated.</td></tr>" +
                    "<tr><td>albumNameInput</td><td>body param</td><td>string</td><td>required</td><td>Batch tag media with a comma separated list of album names. If left blank or null, this value will not be updated.</td></tr>" +
                    "<tr><td>tagBatchDataInput</td><td>body param</td><td>string</td><td>required</td><td>Batch tag media with a comma separated list of subjects. If left blank or null, this value will not be updated.</td></tr>" +
                    "<tr><td>keywordsBatchData</td><td>body param</td><td>string</td><td>required</td><td>Batch tag media with a comma separated list of keywords. If left blank or null, this value will not be updated.</td></tr>" +
                    "<tr><td>latlngBatchData</td><td>body param</td><td>string</td><td>required</td><td>Batch tag media with a latitude and longitude set separated by a comma. If left blank or null, this value will not be updated.</td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"msg\": \"\",\n" +
                    "    \"message\": \"\",\n" +
                    "    \"recognitionLabels\": [\n" +
                    "        {\n" +
                    "            \"id\": &ltsubject_id&gt;,\n" +
                    "            \"name\": \"&ltsubject_name&gt;\"\n" +
                    "        },\n" +
                    "    ],\n" +
                    "    \"allAlbumList\": [\n" +
                    "        {\n" +
                    "            \"id\": &lt;album_id&gt;,\n" +
                    "            \"name\": \"&lt;name_of_album&gt;\",\n" +
                    "            \"coverUrl\": \"&lt;relative_url&gt;\",\n" +
                    "            \"shareUrl\": \"&lt;public_url_key&gt;\"\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"timeOffsets\": [\n" +
                    "        \"-12:00\",\n" +
                    "        \"-11:00\",\n" +
                    "        \"-10:00\",\n" +
                    "        \"-09:30\",\n" +
                    "        \"-09:00\",\n" +
                    "        \"-08:00\",\n" +
                    "        \"-07:00\",\n" +
                    "        \"-06:00\",\n" +
                    "        \"-05:00\",\n" +
                    "        \"-04:30\",\n" +
                    "        \"-04:00\",\n" +
                    "        \"-03:30\",\n" +
                    "        \"-03:00\",\n" +
                    "        \"-02:00\",\n" +
                    "        \"-01:00\",\n" +
                    "        \"±00:00\",\n" +
                    "        \"+01:00\",\n" +
                    "        \"+02:00\",\n" +
                    "        \"+03:00\",\n" +
                    "        \"+03:30\",\n" +
                    "        \"+04:00\",\n" +
                    "        \"+04:30\",\n" +
                    "        \"+05:00\",\n" +
                    "        \"+05:30\",\n" +
                    "        \"+05:45\",\n" +
                    "        \"+06:00\",\n" +
                    "        \"+06:30\",\n" +
                    "        \"+07:00\",\n" +
                    "        \"+08:00\",\n" +
                    "        \"+08:45\",\n" +
                    "        \"+09:00\",\n" +
                    "        \"+09:30\",\n" +
                    "        \"+10:00\",\n" +
                    "        \"+11:00\",\n" +
                    "        \"+11:30\",\n" +
                    "        \"+12:00\",\n" +
                    "        \"+13:00\",\n" +
                    "        \"+14:00\"\n" +
                    "    ],\n" +
                    "    \"keywords\": \"&lt;keyword_1,keyword_n&gt;\",\n" +
                    "    \"cameras\": \"&lt;camera_1,camera_n&gt;\",\n" +
                    "    \"lenses\": \"&lt;lens_1,lens_n&gt;\"" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>recognitionLabels[].id</td><td>int</td><td>Album ID the subject is associated with</td></tr>" +
                    "<tr><td>recognitionLabels[].name</td><td>string</td><td>Subject name</td></tr>" +
                    "<tr><td>allAlbumList[].id</td><td>int</td><td>The album ID</td></tr>" +
                    "<tr><td>allAlbumList[].name</td><td>string</td><td>The album name</td></tr>" +
                    "<tr><td>allAlbumList[].coverUrl</td><td>string</td><td>Relative URL for the album cover image</td></tr>" +
                    "<tr><td>allAlbumList[].shareUrl</td><td>string</td><td>Part of the share URL endpoint for public sharing</td></tr>" +
                    "<tr><td>timeOffsets</td><td>array</td><td>An array of valid time offsets</td></tr>" +
                    "<tr><td>keywords</td><td>string</td><td>A comma separated string of all keywords</td></tr>" +
                    "<tr><td>cameras</td><td>string</td><td>A comma separated string of all cameras</td></tr>" +
                    "<tr><td>lenses</td><td>string</td><td>A comma separated string of all lenses</td></tr>" +
                    "</tbody></table>"
        )
    )
    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/metadata/update/batch","/api/v1/update/metadata/batch"], method = [RequestMethod.PUT], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @CacheEvict(value = ["allMetadata", "allMetadataByDate", "allMetadataByDateAndType", "allMetadataOnlyByDate", "allMetadataAndAttributesByDate", "singleMetadataRequest", "allAlbumMetadataWithCoordinates", "allMetadataWithCoordinates"], allEntries = true)
    fun updateBatchMetadata(model: Model, @RequestBody requestBody: JsonNode, locale: Locale): String? {
//         println(requestBody)
        val batchMetadataMap = mapper.convertValue(requestBody, object : TypeReference<BatchMetadataInput>() {})

        val idArray: Array<String>? = batchMetadataMap.batchMetadataIds
        val dayTaken: Int? = batchMetadataMap.dayTakenBatchData
        val monthTaken: Int? = batchMetadataMap.monthTakenBatchData
        val yearTaken: Int? = batchMetadataMap.yearTakenBatchData
        val latlng: String? = StringEscapeUtils.escapeHtml4(batchMetadataMap.latlngBatchData)
        val offset: String? = StringEscapeUtils.escapeHtml4(batchMetadataMap.offsetTakenBatchData)
        val camera: String? = batchMetadataMap.cameraBatchData
        val lens: String? = batchMetadataMap.lensBatchData
        var keywords: String? = batchMetadataMap.keywordsBatchData
        val recognitionLabelNames: String? = batchMetadataMap.tagBatchDataInput
        val albumNames: String? = batchMetadataMap.albumNameInput
//        println(albumNames)
        val addToAlbums = batchMetadataMap.addtoexistingalbums == "on"
        val addToPeople = batchMetadataMap.addtoexistingpeople == "on"
        val addToKeywords = batchMetadataMap.addtoexistingkeywords == "on"

        val isObject = batchMetadataMap.batchisobject == "on"
        val isHidden = batchMetadataMap.batchhidden == "on"

        if (TextUtils.metadataInputValidation(
                dayTaken,
                monthTaken,
                yearTaken,
                null,
                offset,
                null) &&
            !idArray.isNullOrEmpty())
        {

            val metricsUtil = MetricsUtil()
            metricsUtil.start("Batch Metadata Update")

            val currentUserObj = model.getAttribute("currentUser") as User?
            val settings = model.getAttribute("settings") as Settings

            val headers = HttpHeaders()
            headers.add("x-api-key", settings.getCompreFaceKey())

            val firstAvailableMetadataId = StringEscapeUtils.escapeHtml4(idArray[0])
            val metadataCoverAlbumObj = metadataRepository.findById(firstAvailableMetadataId)
            val albumIdList: ArrayList<Int> = ArrayList()

            // Process albums
            if (!isHidden && albumNames != null && albumNames.toString().trim() != "") {
                val albumNameList = albumNames.toString().split(",")

                for (albumNameRaw in albumNameList) {
                    val albumId = processAlbum(albumNameRaw, currentUserObj, metadataCoverAlbumObj.get())
                    if (albumId > 0) {
                        albumIdList.add(albumId)
                    }
                }
            }

//            if (camera != null && camera.trim().isNotBlank()) {
//                val cameraTypes = metadataRepository.findByCameraTypeAlphabetical()
//                for (cameraType in cameraTypes) {
//                    if (camera!!.trim().lowercase() == cameraType.trim().lowercase()) {
//                        camera = cameraType.trim()
//                        break
//                    }
//                }
//            }
//
//            if (lens != null && lens.trim().isNotBlank()) {
//                val lensTypes = metadataRepository.findByLensTypeAlphabetical()
//                for (lensType in lensTypes) {
//                    if (lens!!.trim().lowercase() == lensType.trim().lowercase()) {
//                        lens = lensType.trim()
//                        break
//                    }
//                }
//            }

            val metadataList: ArrayList<Metadata> = ArrayList()
            val albumPhotoList: ArrayList<AlbumPhoto> = ArrayList()

            // Process keyword and lat/lng data
            if (latlng != null && latlng != "") {
                setLocation(latlng, idArray)
            }

            var keywordList = mutableListOf<String>()
            if (!keywords.isNullOrBlank()) {
                keywords = keywords.toString().trim()
                if (keywords.last() == ',') {
                    keywords = keywords.dropLast(1)
                }
                keywordList = keywords.split(",").map { it.trim() } as MutableList<String>
            }

            val albumIdAddedList = mutableListOf<Int>()

            for (idVal in idArray) {
                val id = StringEscapeUtils.escapeHtml4(idVal)
                val metadataObj: Optional<Metadata?> = metadataRepository.findById(id)
                if (metadataObj.isPresent) {
                    val metadata = metadataObj.get()

                    if (isHidden) {
                        metadata.setHidden(true)
                        removeMetadata(id, locale)
                    } else {
                        // Add album photo
                        if (albumIdList.isNotEmpty()) {
                            if (addToAlbums == false) {
                                albumPhotoRepository.deleteByMetadataId(metadata.getId())
                            }

                            for (albumId in albumIdList) {
                                val albumPhotoCount =
                                    albumPhotoRepository.countByMetadataIdAndAlbumId(metadata.getId(), albumId)!!
                                if (albumPhotoCount == 0) {
                                    val albumPhotoObj = AlbumPhoto()
                                    albumPhotoObj.setMetadataId(metadata.getId())
                                    albumPhotoObj.setAlbumId(albumId)
                                    albumPhotoObj.setCreatedAt(getCurrentTimestamp())
                                    albumPhotoObj.setModifiedAt(getCurrentTimestamp())
                                    albumPhotoList.add(albumPhotoObj)
                                    if (!albumIdAddedList.contains(albumId)) {
                                        albumIdAddedList.add(albumId)
                                    }
                                }
                            }
                        }

                        processPeople(
                            model.getAttribute("settings") as Settings,
                            metadata,
                            recognitionLabelNames.toString(),
                            isObject,
                            addToPeople
                        )

                        if (dayTaken != null) {
                            metadata.setDay(dayTaken)
                        }
                        if (monthTaken != null) {
                            metadata.setMonth(monthTaken)
                        }
                        if (yearTaken != null) {
                            metadata.setYear(yearTaken)
                        }
                        // Original taken date can be looked at in EXIF data
                        if (yearTaken != null && monthTaken != null && dayTaken != null) {
                            // Set taken date
                            val takenAt = metadata.getTakenAt()
                            if (takenAt != null) {
                                val takenArr = takenAt.split(" ")
                                if (takenArr.count() == 2) {
                                    val month = if (monthTaken > 9) monthTaken else "0$monthTaken"
                                    val day = if (dayTaken > 9) dayTaken else "0$dayTaken"
                                    val time = takenArr[1]
                                    val takenVal = "$yearTaken-$month-$day $time"
                                    metadata.setTakenAt(takenVal)
                                }
                            }
                        }
                        if (camera != null && camera.trim().isNotBlank()) {
                            metadata.setCamera(camera)
                        }
                        if (lens != null && lens.trim().isNotBlank()) {
                            metadata.setLens(lens)
                        }
                        if (offset != null && offset.trim().isNotBlank()) {
                            metadata.setTimeZone(offset)
                        }

                        if (keywordList.isNotEmpty()) {
                            if (addToKeywords == false) {
                                keywordPhotoRepository.deleteAllByMetadataId(metadata.getId())
                            }
                            processKeywords(keywordList, metadata.getId())
                        }
                    }

                    metadata.setModifiedAt(getCurrentTimestamp())
                    metadataList.add(metadata)
                } else {
                    logger.log(Level.WARNING, "Updating metadata $id failed for batch operation. Verify that $id is valid.")
                }
            }

            val keywordIdsToDelete = keywordRepository.findAllOrphanedKeywordIds()

            if (keywordIdsToDelete.count() > 0) {
                keywordRepository.deleteAllById(keywordIdsToDelete)
            }

            if (albumPhotoList.isNotEmpty()) {
                albumPhotoRepository.saveAll(albumPhotoList)
            }

            notifyAlbumUpdate(albumIdAddedList,currentUserObj, locale)

            if (metadataList.isNotEmpty()) {
                // Update record
                metadataRepository.saveAll(metadataList)

                val attrResponse = getAllAttributeData(model)
                for ((k, v) in attrResponse) {
                    resp[k] = v
                }

                metricsUtil.end()

                resp["msg"] = messageSource?.getMessage("main.modal.saved", null, locale)
                resp["status"] = ApiResponse.SUCCESS.status

                return mapper.writeValueAsString(resp)
            }

            metricsUtil.end()
        }
        logger.log(Level.WARNING, "Updating batch metadata failed. Could not save.")
        resp["msg"] = messageSource?.getMessage("main.modal.saved.fail", null, locale)
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }

    fun setLocation(latlng: String, idArray: Array<String>?) {
        Thread {
            val coordinateMap = TextUtils.processCoordinates(geocodeUrl!!, latlng)
            val lat = coordinateMap["lat"]
            val lng = coordinateMap["lng"]
            val timezone = coordinateMap["timezone"]
            val place = coordinateMap["place"]

            if (latlng.isNotEmpty() && (lat == null || lng == null)) {
                logger.log(Level.WARNING, "Could not save location due to invalid latlng.")
            } else {
                val metadataLocationList = mutableListOf<Metadata>()
                if (idArray != null) {
                    for (idVal in idArray) {
                        val id = StringEscapeUtils.escapeHtml4(idVal)
                        val metadataObj: Optional<Metadata?> = metadataRepository.findById(id)
                        if (metadataObj.isPresent) {
                            val metadata = metadataObj.get()

                            if (metadata.getLat() != lat || metadata.getLng() != lng) {
                                metadata.setLat(lat)
                                metadata.setLng(lng)

                                if (timezone != null) {
                                    metadata.setTimeZone(timezone)
                                }

                                if (place != null) {
                                    metadata.setPlaceName(place)
                                }

                                metadataLocationList.add(metadata)
                            }
                        }
                    }
                }

                if (metadataLocationList.size > 0) {
                    metadataRepository.saveAll(metadataLocationList)
                }
            }
        }.start()
    }

    @Transactional
    fun removeMetadata(metadataId: String, locale: Locale) {
        recognitionLabelPhotoRepository?.deleteByMetadataId(metadataId)
        albumPhotoRepository.deleteByMetadataId(metadataId)
        favoriteRepository.deleteByMetadataId(metadataId)
        albumPhotoCommentRepository.deleteByMetadataId(metadataId)

        // Find albums
        cleanupOrphanedAlbums(locale)

        // Find album cover
        cleanupAlbumCover(metadataId)

        // Find people
        cleanupOrphanedSubjects()

        // Find person cover
        cleanupPersonCover(metadataId)
    }

    fun cleanupAlbumCover(metadataId: String) {
        val metadataObj = metadataRepository.findByMetadataId(metadataId)

        if (metadataObj != null) {
            val thumbnailUrlCentered = metadataObj.getThumbnailUrlCentered()

            val allAlbums = albumRepository.findAll()
            for (album in allAlbums) {
                val albumCoverUrl = album?.getCoverUrl()

                if (album != null && thumbnailUrlCentered == albumCoverUrl) {
                    val albumPhoto = albumPhotoRepository.findFirstByAlbumId(album.getId())

                    // Find the next album photo and set as album cover
                    if (albumPhoto != null && metadataRepository.count() > 0) {

                        val albumPhotoMetadata = metadataRepository.findByMetadataId(albumPhoto.getMetadataId()!!)

                        if (albumPhotoMetadata != null) {
                            album.setCoverUrl(albumPhotoMetadata.getThumbnailUrlCentered())
                            logger.log(
                                Level.INFO,
                                "Set the album cover when cleaning album cover"
                            )
                        }
                    }
                }
            }
        }
    }

    fun cleanupOrphanedAlbums(locale: Locale) {
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

                    val admins = userRepository.findAllAdmins()

                    if (admins.count() > 0) {
                        val notificationObjList = mutableListOf<Notification>()
                        val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                        sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
                        for (admin in admins) {
                            var language = admin.getLanguage()
                            if (language == null) {
                                language = "en"
                            }

                            var locale = Locale(language)
                            val notificationObj = Notification()
                            notificationObj.setUserId(admin.getId())
                            notificationObj.setCreatedAt(getCurrentTimestamp())
                            notificationObj.setModifiedAt(getCurrentTimestamp())
                            notificationObj.setRead(false)
                            notificationObj.setMessage(messageSource?.getMessage("main.notification.album.delete", arrayOf(album.getName()), locale)+"- ${sdtf.format(Date())}")
                            notificationObjList.add(notificationObj)
                        }
                        if (notificationObjList.isNotEmpty()) {
                            notificationRepository.saveAll(notificationObjList)
                        }
                    }
                }
            }
        }
    }

    fun cleanupPersonCover(metadataId: String) {
        if (metadataRepository.count() > 0) {
            val metadataObj = metadataRepository.findByMetadataId(metadataId)

            if (metadataObj != null) {
                val thumbnailUrlCentered = metadataObj.getThumbnailUrlCentered()

                val allPeople = recognitionLabelRepository?.findAll()
                if (allPeople != null) {
                    for (person in allPeople) {
                        val personCoverUrl = person?.getCoverUrl()

                        if (person != null && thumbnailUrlCentered == personCoverUrl) {
                            val personPhoto = recognitionLabelPhotoRepository?.findFirstByRecognitionLabelId(person.getId())

                            // Find the next person album photo and set as person cover
                            if (personPhoto != null && metadataRepository.count() > 0) {
                                val personPhotoMetadata = metadataRepository.findByMetadataId(personPhoto.getMetadataId()!!)

                                if (personPhotoMetadata != null) {
                                    person.setCoverUrl(personPhotoMetadata.getThumbnailUrlCentered())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun cleanupOrphanedSubjects() {
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

    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/timeline/sync/{metadataId}"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun postSyncData(model: Model, @RequestBody requestBody: JsonNode, @PathVariable metadataId: String, locale: Locale): String? {
        val batchMetadataMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        resp["year"] = ""
        resp["month"] = ""
        resp["day"] = ""
        resp["time"] = ""

        if (batchMetadataMap.containsKey("id") && batchMetadataMap["id"] == metadataId) {
            val metadataOptional = metadataRepository.findById(metadataId)
            val metadataObj = metadataOptional.get()

            if (metadataObj.getTakenAt() != null) {
                val datePattern = TextUtils.getCommonDateFormat()
                val dateArray = metadataObj.getTakenAt()!!.format(datePattern).split(" ")
                val takenDateArray = dateArray[0].split("-")
                val year = takenDateArray[0].toInt()
                val month = takenDateArray[1].toInt()
                val day = takenDateArray[2].toInt()
                val time = dateArray[1]
                metadataObj.setYear(year)
                metadataObj.setMonth(month)
                metadataObj.setDay(day)
                metadataObj.setTimeZone(time)
                metadataObj.setModifiedAt(getCurrentTimestamp())
                metadataRepository.save(metadataObj)

                resp["year"] = year.toString()
                resp["month"] = month.toString()
                resp["day"] = day.toString()
                resp["time"] = time

                resp["msg"] = messageSource?.getMessage("main.modal.saved", null, locale)
                resp["status"] = ApiResponse.SUCCESS.status
                return mapper.writeValueAsString(resp)
            }
        }


        resp["msg"] = messageSource?.getMessage("main.modal.saved.fail", null, locale)
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getMetadata",
            summary = "Get metadata information and associated keyword and favorites data.",
            description = "<strong>Get metadata information and associated keyword and favorites data.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/metadata/{id}\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"msg\": \"\",\n" +
                    "    \"status\": \"success\",\n" +
                    "    \"favorites\": {\n" +
                    "        \"&lt;metadata_id&gt;\": {\n" +
                    "            \"favorite\": &lt;is_favorite&gt;,\n" +
                    "            \"count\": &lt;favorites_count&gt;\n" +
                    "        }\n" +
                    "    },\n" +
                    "    \"keywordList\": [\"&lt;keyword_1&gt;\", \"&lt;keyword_n&gt;\"]\n" +
                    "    \"metadata\": {\n" +
                    "        &lt;metadata&gt;\n" +
                    "    }\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>favorites.&lt;metadata_id&gt;.favorite</td><td>boolean</td><td>Indicates that this media is flagged as a favorite</td></tr>" +
                    "<tr><td>favorites.&lt;metadata_id&gt;.count</td><td>int</td><td>Indicates the number of favorites for this media</td></tr>" +
                    "<tr><td>keywordList</td><td>array</td><td>A list of keywords tagged for this media</td></tr>" +
                    "<tr><td>metadata</td><td>object</td><td>A <a href=\"#\" data-bs-toggle=\"modal\" data-bs-target=\"#propMetadataDocs\">Metadata</a> object</td></tr>" +
                    "</tbody></table>"
        )
    )
    @RequestMapping(value = ["/api/v1/metadata/{id}", "/metadata/{id}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    @Cacheable(value = ["singleMetadataRequest"], key = "{#id}")
    @Secured("ROLE_SUPER","ROLE_ADMIN","ROLE_USER")
    fun getMetadata(model: Model, request: HttpServletRequest, @PathVariable(required = true) id: String): ResponseEntity<String> {
        val response = mutableMapOf<String, Any?>()
        val keywordArray = mutableListOf<String>()
        val keywords = keywordRepository.findKeywordsByMetadataId(id)
        for (keyword in keywords) {
            keywordArray.add(keyword.getKeyword()!!)
        }
        response["keywordList"] = keywordArray

        response["albumMap"] = mutableMapOf<Int, String>()

        response["lastAccessedByDetails"] = null

        response["uploadedByDetails"] = null

        response["lastAccessedAt"] = null

        response["shortPlaceName"] = null

        var scheme = request.getHeader("X-Forwarded-Proto")
        if (scheme == null) {
            scheme = request.scheme // Fallback if not behind a proxy
        }
        var baseUrlBuilder = ServletUriComponentsBuilder.fromRequestUri(request).replacePath(null)
        if (scheme == "https") {
            baseUrlBuilder = baseUrlBuilder.scheme("https")
        }
        response["baseUrl"] = baseUrlBuilder.build().toUriString()

        val emptyJson = "{}"
        val mapper = ObjectMapper()
        response["metadata"] = mapper.readTree(emptyJson)

        val metadataRecord = metadataRepository.findById(id)

        val currentUserObj = model.getAttribute("currentUser") as User?

        if (metadataRecord.isPresent) {

            response["albumMap"] = getAlbumMapForUser(currentUserObj, id)

            val metadataObj = metadataRecord.get()
            var metadataObjCopy: Metadata? = null
            metadataObjCopy = metadataObj
            // Hide info from users
            if (currentUserObj?.getAuthority()!! == "ROLE_USER") {
                metadataObjCopy.setLastAccessedAt(null)
                metadataObjCopy.setLastAccessedBy(null)
                metadataObjCopy.setUploadedBy(null)
                metadataObjCopy.setFreeFormString(null)
            } else if (currentUserObj.getAuthority()!! == "ROLE_ADMIN" || currentUserObj.getAuthority()!! == "ROLE_SUPER") {
                val accessInfo = metadataRecord.get().getFreeFormString()
                val freeFormObj = TextUtils.parseMetadataFreeformString(accessInfo)
                if (freeFormObj != null) {
                    val accessClientIP = freeFormObj.getClientIP()
                    val accessBrowser = freeFormObj.getBrowser()
                    val accessRequestResourceType = freeFormObj.getRequestResourceType()
                    val accessOS = freeFormObj.getOperatingSystem()
                    val accessPage = freeFormObj.getViewPage()
                    val accessInfoString = " $accessPage - $accessOS $accessBrowser $accessRequestResourceType"

                    val uploadedByUserId = metadataRecord.get().getUploadedBy()
                    if (uploadedByUserId != null && uploadedByUserId > 0) {
                        val userUploaded = userRepository.findById(uploadedByUserId)
                        response["uploadedByDetails"] = userUploaded.get().getUsername()
                    }

                    if (metadataObj.getLastAccessedBy() != null && metadataObj.getLastAccessedBy()!! > 0) {
                        val userObj = userRepository.findById(metadataRecord.get().getLastAccessedBy())

                        if (userObj != null) {
                            response["lastAccessedByDetails"] =
                                userObj.getUsername() + " " + accessClientIP + accessInfoString
                        } else {
                            response["lastAccessedByDetails"] = accessClientIP + accessInfoString
                        }
                    } else {
                        response["lastAccessedByDetails"] = accessClientIP + accessInfoString
                    }
                }
            }
            response["metadata"] = metadataObjCopy

            if (metadataObjCopy.getPlaceName() != null) {
                response["shortPlaceName"] = TextUtils.formatPlaceNameForHeader(metadataObjCopy.getPlaceName())
            }
        }

        val favoritesMap = HashMap<String, HashMap<String, Any>>()
        val idList = mutableListOf<String>()
        idList.add(id)

        val favoriteCounts = favoriteRepository.countByMetadataIdIn(idList)

        if (favoriteCounts.count() > 0) {
            for (favoriteCount in favoriteCounts) {
                favoritesMap[favoriteCount.getMetadataId()!!] = hashMapOf(
                    "favorite" to (favoriteCount.getUserId() == currentUserObj?.getId()),
                    "count" to favoriteCount.getCount() as Any
                )

                if (favoriteCount.getUserId() == currentUserObj?.getId()) {
                    break
                }
            }
        }
        response["favorites"] = favoritesMap

        response["msg"] = ""
        response["status"] = ApiResponse.SUCCESS.status

        val json = mapper.writeValueAsString(response)
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
            .body(json)
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getTimelineMetadata",
            summary = "Get media info for a timeline view.",
            description = "<strong>Get media info for a timeline view.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/complete/metadata/{id}\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>id</td><td>param</td><td>string</td><td>required</td><td>A valid metadata ID</td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"msg\": \"\",\n" +
                    "    \"status\": \"success\",\n" +
                    "    \"allRecognitionLabels\": [\n" +
                    "        {\n" +
                    "            \"id\": &lt;subject_id&gt;,\n" +
                    "            \"name\": &lt;subject_name&gt;,\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"albumList\": [\"&lt;album_name_1&gt;\",\"&lt;album_name_n&gt;\"]\n" +
                    "    \"taggedPeopleList\": [\"&lt;subject_1&gt;\",\"&lt;subject_n&gt;\"]\n" +
                    "    \"keywordList\": [\"&lt;keyword_1&gt;\",\"&lt;keyword_n&gt;\"]\n" +
                    "    \"allAlbumList\": [\n" +
                    "        {\n" +
                    "            \"id\": &lt;album_id&gt;,\n" +
                    "            \"name\": \"&lt;name_of_album&gt;\",\n" +
                    "            \"coverUrl\": \"&lt;relative_url&gt;\",\n" +
                    "            \"shareUrl\": \"&lt;public_url_key&gt;\"\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"metadata\": {\n" +
                    "           &lt;metadata&gt;\n" +
                    "    },\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>allRecognitionLabels[].id</td><td>int</td><td>The subject ID</td></tr>" +
                    "<tr><td>allRecognitionLabels[].name</td><td>int</td><td>The subject name</td></tr>" +
                    "<tr><td>albumList</td><td>array</td><td>A list of album names associated with this media</td></tr>" +
                    "<tr><td>taggedPeopleList</td><td>array</td><td>A list of subject names associated with this media</td></tr>" +
                    "<tr><td>keywordList</td><td>array</td><td>A list of keywords associated with this media</td></tr>" +
                    "<tr><td>allAlbumList[].id</td><td>int</td><td>The album ID</td></tr>" +
                    "<tr><td>allAlbumList[].name</td><td>string</td><td>The album name</td></tr>" +
                    "<tr><td>allAlbumList[].coverUrl</td><td>string</td><td>Relative URL for the album cover image</td></tr>" +
                    "<tr><td>allAlbumList[].shareUrl</td><td>string</td><td>Part of the share URL endpoint for public sharing</td></tr>" +
                    "<tr><td>metadata</td><td>object</td><td>A <a href=\"#\" data-bs-toggle=\"modal\" data-bs-target=\"#propMetadataDocs\">Metadata</a> object</td></tr>" +
                    "</tbody></table>"
        )
    )
    @RequestMapping(value = ["/api/v1/complete/metadata/{id}","/complete/metadata/{id}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    @Secured("ROLE_SUPER","ROLE_ADMIN","ROLE_USER")
    fun getTimelineMetadata(model: Model, request: HttpServletRequest, @PathVariable(required = true) id: String): String {
        val response = mutableMapOf<String, Any?>()

        response["allAlbumList"] = mutableListOf<Album>()
        response["allRecognitionLabels"] = mutableListOf<RecognitionLabel>()
        val labelArray = mutableListOf<String>()
        response["taggedPeopleList"] = labelArray
        response["albumMap"] = mutableMapOf<Int, String>()
        response["lastAccessedAt"] = null
        response["baseUrl"] = null
        response["shortPlaceName"] = null

        val emptyJson = "{}"
        val mapper = ObjectMapper()
        response["metadata"] = mapper.readTree(emptyJson)

        val currentUserObj = model.getAttribute("currentUser") as User?

        val metadataRecord = metadataRepository.findById(id)
        if (metadataRecord.isPresent) {
            var scheme = request.getHeader("X-Forwarded-Proto")
            if (scheme == null) {
                scheme = request.scheme // Fallback if not behind a proxy
            }
            var baseUrlBuilder = ServletUriComponentsBuilder.fromRequestUri(request).replacePath(null)
            if (scheme == "https") {
                baseUrlBuilder = baseUrlBuilder.scheme("https")
            }
            response["baseUrl"] = baseUrlBuilder.build().toUriString()

            val metadataObj = metadataRecord.get()
            var metadataObjCopy: Metadata? = null
            metadataObjCopy = metadataObj
            // Hide info from users
            if (currentUserObj?.getAuthority()!! == "ROLE_USER") {
                metadataObjCopy.setLastAccessedAt(null)
                metadataObjCopy.setLastAccessedBy(null)
                metadataObjCopy.setUploadedBy(null)
                metadataObjCopy.setFreeFormString(null)
            }
            response["metadata"] = metadataObjCopy

            if (metadataObjCopy.getPlaceName() != null && metadataObjCopy.getPlaceName() != "") {
                response["shortPlaceName"] = TextUtils.formatPlaceNameForHeader(metadataObjCopy.getPlaceName())
            }

            val recognitionLabelPhotos = recognitionLabelPhotoRepository?.findByMetadataId(id)
            if (recognitionLabelPhotos != null) {
                for (recognitionLabelPhoto in recognitionLabelPhotos) {
                    val recognitionLabelObj = recognitionLabelRepository?.findById(recognitionLabelPhoto.getRecognitionLabelId()!!)
                    if (recognitionLabelObj != null) {
                        labelArray.add(recognitionLabelObj.get().getName()!!)
                    }
                }
            }
            response["taggedPeopleList"] = labelArray
            response["albumMap"] = getAlbumMapForUser(currentUserObj, id)

            val keywordArray = mutableListOf<String>()
            val keywords = keywordRepository.findKeywordsByMetadataId(id)
            for (keyword in keywords) {
                keywordArray.add(keyword.getKeyword()!!)
            }
            response["keywordList"] = keywordArray

            val allRecognitionLabels = recognitionLabelRepository?.findAllByNameNotContaining(TextUtils.getObjectName())
            if (allRecognitionLabels != null && allRecognitionLabels.count() > 0) {
                response["allRecognitionLabels"] = allRecognitionLabels
            }

            val allAlbumList = albumRepository.findAllOrderByAlbumName()
            if (allAlbumList != null && allAlbumList.count() > 0) {
                response["allAlbumList"] = allAlbumList
            }
        }

        response["msg"] = ""
        response["status"] = ApiResponse.SUCCESS.status

        return mapper.writeValueAsString(response)
    }

    private fun getAlbumMapForUser(currentUserObj: User?, metadataId: String?): MutableMap<Int, String> {
        val albumMap = mutableMapOf<Int, String>()

        if (currentUserObj != null && metadataId != null) {
            val albumPhotos = if (currentUserObj.getAuthority() == "ROLE_ADMIN" || currentUserObj.getAuthority() == "ROLE_SUPER") {
                albumPhotoRepository.findAlbumPhotoByMetadataId(metadataId)
            } else {
                albumPhotoRepository.findAlbumPhotoByUserIdAndMetadataId(currentUserObj.getId(), metadataId)
            }

            if (albumPhotos != null) {
                for (albumPhoto in albumPhotos) {
                    val album = albumRepository.findById(albumPhoto!!.getAlbumId()!!)
                    albumMap[album.get().getId()] = album.get().getName()!!
                }
            }
        }

        return albumMap
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getExifData",
            summary = "Get EXIF data.",
            description = "<strong>Get EXIF data extracted using <a href=\"https://github.com/drewnoakes/metadata-extractor\">Metadata Extractor</a>.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/exif/metadata/{id}\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>id</td><td>param</td><td>string</td><td>required</td><td>The metadata ID</td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "Different outputs depending on media. See <a href=\"https://github.com/drewnoakes/metadata-extractor\">Metadata Extractor</a> for more details."
        )
    )
    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/api/v1/exif/metadata/{id}","/exif/metadata/{id}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getExifData(model: Model, @PathVariable(required = true) id: String, response: HttpServletResponse, locale: Locale): String {
        val responseMap = mutableMapOf<String, Any?>()

        val metadataRecord = metadataRepository.findById(id)
        if (metadataRecord.isPresent) {
            val metadata = metadataRecord.get()

            val jsonNode = FileUtils.convertExifToJsonNode(metadata.getFolder()!!, metadata.getFileName()!!, relativeSidecarDir!!)

            responseMap["msg"] = messageSource?.getMessage("main.fail", null, locale)
            responseMap["status"] = ApiResponse.FAIL.status

            if (jsonNode != null) {
                responseMap["exif"] = jsonNode
                responseMap["msg"] = ""
                responseMap["status"] = ApiResponse.SUCCESS.status
            }
        } else {
            return TextUtils.returnForbiddenError(response)
        }

        return mapper.writeValueAsString(responseMap)
    }

    @RequestMapping(value = ["/metadata/download/batch"],
        method = [RequestMethod.POST],
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE]
    )
    @Secured("ROLE_SUPER","ROLE_ADMIN","ROLE_USER")
    @ResponseBody
    fun downloadBatchMetadata(model: Model, @RequestParam paramMap: Map<String, String>): ResponseEntity<InputStreamResource>? {

        if (paramMap.containsKey("batchMetadataIds")) {
            val idArray: Array<String>? = mapper.readValue(paramMap["batchMetadataIds"], object : TypeReference<Array<String>>() {})
            if (!idArray.isNullOrEmpty()) {
                val metadatas = metadataRepository.findAllByMetadataIds(idArray).toMutableList()

                val tempDownloadDir = Files.createTempDirectory("shashin_download")

                for (metadata in metadatas) {
                    val tempFile = File(tempDownloadDir.pathString + "/" + metadata.getId() + "." + metadata.getExpectedExtension())
                    if (tempFile.createNewFile()) {
                        Files.copy(Path(metadata.getPath()!!), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }
                }

                if (tempDownloadDir.isDirectory() && tempDownloadDir.toList().isNotEmpty()) {
                    val tempDir = tempDownloadDir.toFile()
                    val outputZipFile = FileUtils.zipFolder(tempDir, "shashin_download")

                    if (outputZipFile != null) {
                        FileUtils.deleteDirectory(tempDir)

                        val headers = HttpHeaders()
                        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + outputZipFile.name)
                        headers.add("Cache-Control", "no-cache, no-store, must-revalidate")
                        headers.add("Pragma", "no-cache")
                        headers.add("Expires", "0")
                        headers.add("Set-Cookie", "fileDownload=true; path=/")

                        val resource = InputStreamResource(FileInputStream(outputZipFile))
                        val contentLength = outputZipFile.length()

                        return ResponseEntity.ok()
                            .headers(headers)
                            .contentLength(contentLength)
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .body(resource)
                    }
                }
            }
        }

        return null
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/metadata/edit/thumbs"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @CacheEvict(value = ["allMetadata", "allMetadataByDate", "allMetadataByDateAndType", "allMetadataOnlyByDate", "allMetadataAndAttributesByDate", "singleMetadataRequest", "allAlbumMetadataWithCoordinates", "allMetadataWithCoordinates"], allEntries = true)
    fun editThumbs(model: Model, @RequestBody requestBody: JsonNode, @RequestParam restore: Optional<Boolean>, locale: Locale): String? {
        val metadataMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})

        if (metadataMap.containsKey("metadataId") &&
            metadataMap.containsKey("rotation") &&
            metadataMap.containsKey("flipX") &&
            metadataMap.containsKey("flipY") &&
            metadataMap.containsKey("brightness") &&
            metadataMap.containsKey("contrast")
        ) {
            val metadataId = metadataMap["metadataId"] as String
            val rotation = metadataMap["rotation"] as Int
            val flipX = metadataMap["flipX"] as Boolean
            val flipY = metadataMap["flipY"] as Boolean
            val brightness = metadataMap["brightness"].toString().toDouble()
            val contrast = metadataMap["contrast"].toString().toDouble()
            val restoreImages = restore.orElse(false)

            val metadataObj = metadataRepository.findById(metadataId)

            if (metadataObj.isPresent) {
                var metadata: Metadata = metadataObj.get()
                val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
                val sidecarDir = rootPath + relativeSidecarDir

                var edited = false

                var path = metadataObj.get().getPath()!!
                if (!restoreImages && !metadataObj.get().getThumbnailUrlOriginal()!!.contains(metadataId)) {
                    path = sidecarDir + (metadataObj.get().getThumbnailUrlOriginal()!!.replace("/api/v1/",""))
                }

                var imageFile = File(path)
                val bufferedImage = ImageIO.read(imageFile)
                var editedImage = bufferedImage

                if (!restoreImages) {
                    if (flipX) {
                        editedImage = ImageProcessing.flipHorizontally(editedImage)
                        edited = true
                    }

                    if (flipY) {
                        editedImage = ImageProcessing.flipVertically(editedImage)
                        edited = true
                    }

                    if (metadata.getBrightness() == null || (metadata.getBrightness() != null && brightness != metadata.getBrightness().toString().toDouble())) {
                        metadata.setBrightness(brightness.toString())
                        editedImage = ImageProcessing.adjustBrightness(editedImage, brightness)
                        edited = true
                    }

                    if (metadata.getContrast() == null || (metadata.getContrast() != null && contrast != metadata.getContrast().toString().toDouble())) {
                        metadata.setContrast(contrast.toString())
                        editedImage = ImageProcessing.adjustContrast(editedImage, contrast)
                        edited = true
                    }

                    editedImage = ImageProcessing.rotateImage(editedImage, rotation.toDouble())
                    if (editedImage.height != metadata.getOriginalImageHeight() && editedImage.width != metadata.getOriginalImageWidth()) {
                        val setWidth = metadata.getOriginalImageHeight()
                        val setHeight = metadata.getOriginalImageWidth()
                        metadata.setOriginalImageWidth(setWidth)
                        metadata.setOriginalImageHeight(setHeight)

                        val setSmallWidth = metadata.getThumbnailSmallHeight()
                        val setSmallHeight = metadata.getThumbnailSmallWidth()
                        metadata.setThumbnailSmallWidth(setSmallWidth)
                        metadata.setThumbnailSmallHeight(setSmallHeight)

                        edited = true
                    } else if (rotation % 360 != 0) {
                        edited = true
                    }
                } else {
                    edited = true
                }

                if (edited) {
                    deleteThumbnails(metadata)
                    val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
                    val sidecarDir = rootPath + relativeSidecarDir
                    val imageProcessing = ImageProcessing(apiVersion, File(metadata.getPath()!!), sidecarDir, metadata)
                    metadata = imageProcessing.setThumbnails(
                        editedImage,
                        metadata,
                        !restoreImages,
                        metadata.getExpectedExtension().toString(),
                        true
                    )
                    if (restoreImages) {
                        metadata.setThumbnailUrlOriginal("/api/$apiVersion/image/${metadata.getId()}")
                        metadata.setBrightness("1.0")
                        metadata.setContrast("1.0")
                    }
                    metadata.setModifiedAt(getCurrentTimestamp())
                    metadataRepository.save(metadata)
                }

                resp["metadata"] = metadata
                resp["msg"] = messageSource?.getMessage("main.modal.saved", null, locale)
                resp["status"] = ApiResponse.SUCCESS.status
                return mapper.writeValueAsString(resp)
            }
        }

        resp["metadata"] = null
        resp["msg"] = messageSource?.getMessage("main.modal.saved.fail", null, locale)
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/metadata/update/thumbs"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @CacheEvict(value = ["allMetadata", "allMetadataByDate", "allMetadataByDateAndType", "allMetadataOnlyByDate", "allMetadataAndAttributesByDate", "singleMetadataRequest", "allAlbumMetadataWithCoordinates", "allMetadataWithCoordinates"], allEntries = true)
    fun updateThumbs(model: Model, @RequestBody requestBody: JsonNode, locale: Locale): String? {
        val metadataMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})

        if (metadataMap.containsKey("metadataId") &&
            metadataMap.containsKey("base64Data") &&
            metadataMap.containsKey("isImage")
        ) {
            val metadataId = metadataMap["metadataId"] as String
            val base64Data = metadataMap["base64Data"] as String
            val isImage = metadataMap["isImage"] as Boolean

            val metadataObj = metadataRepository.findById(metadataId)

            if (metadataObj.isPresent) {
                var metadata = metadataObj.get()
                val imageBytes = FileUtils.parseBase64(base64Data)
                if (imageBytes != null) {
                    val img = ImageIO.read(ByteArrayInputStream(imageBytes))

                    val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
                    val sidecarDir = rootPath + relativeSidecarDir
                    val imageProcessing = ImageProcessing(apiVersion, File(metadata.getPath()!!), sidecarDir, metadata)

                    var extension = "jpg"
                    if (isImage) {
                        extension = metadata.getExpectedExtension().toString()
                    }

                    metadata = imageProcessing.setThumbnails(img, metadata, true, extension, true)
                    metadata.setModifiedAt(getCurrentTimestamp())

                    metadataRepository.save(metadata)
                }

                resp["posterUrl"] = metadata.getThumbnailUrlOriginal()
                resp["msg"] = messageSource?.getMessage("main.modal.saved", null, locale)
                resp["status"] = ApiResponse.SUCCESS.status
                return mapper.writeValueAsString(resp)
            }
        }

        resp["posterUrl"] = ""
        resp["msg"] = messageSource?.getMessage("main.modal.saved.fail", null, locale)
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }

    private fun processAlbum(albumNameRaw: String, currentUserObj: User?, metadataObj: Metadata?): Int {
//        val albumIdList: ArrayList<Int> = ArrayList()
        var albumId = 0

        if (albumNameRaw.trim().isNotBlank() && currentUserObj != null) {
            val albumName = albumNameRaw.trim().replace(" +".toRegex()," ")
            val albumObject = albumRepository.findAlbumByNameIgnoreCase(albumName)
            var albumObj = Album()

            if (albumObject != null) {
                albumId = albumObject.getId()
            } else {
                if (metadataObj?.getThumbnailUrlCentered() != null) {
                    albumObj.setCoverUrl(metadataObj.getThumbnailUrlCentered())
                    logger.log(
                        Level.INFO,
                        "Set the album cover when processing album"
                    )
                }
                albumObj.setName(albumName)
                albumObj.setCreatedAt(getCurrentTimestamp())
                albumObj.setModifiedAt(getCurrentTimestamp())
                albumObj = albumRepository.save(albumObj)
                albumId = albumObj.getId()
            }

            if (albumId > 0) {
//                albumIdList.add(albumId)

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

        return albumId
    }

    private fun processKeywords(keywordList: List<String>, metadataId: String) {
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

                val keywordPhotoCount = keywordPhotoRepository.countByKeywordIdAndMetadataId(keywordObj.getId(), metadataId)
                if (keywordPhotoCount == 0) {
                    val keywordPhotoObj = KeywordPhoto()
                    keywordPhotoObj.setKeywordId(keywordObj.getId())
                    keywordPhotoObj.setMetadataId(metadataId)
                    keywordPhotoObj.setCreatedAt(getCurrentTimestamp())
                    keywordPhotoObj.setModifiedAt(getCurrentTimestamp())
                    keywordPhotoRepository.save(keywordPhotoObj)
                }
            }
        }
    }

    private fun processPeople(settings: Settings, metadataObj: Metadata?, taggedPeople: String?, isObject: Boolean, addPerson: Boolean) {
        if (metadataObj != null) {
            val metadataId = metadataObj.getId()

            if (addPerson == false) {
                recognitionLabelPhotoRepository?.deleteByMetadataId(metadataId)
            }

            if (isObject) {
                val recognitionLabelRecord = recognitionLabelRepository?.findByNameIgnoreCase(TextUtils.getObjectName())
                var recognitionLabelObj = RecognitionLabel()
                if (recognitionLabelRecord == null) {
                    recognitionLabelObj.setName(TextUtils.getObjectName())
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
            } else if (taggedPeople != null && taggedPeople.trim() != "") {
                val recognitionLabelArray = taggedPeople.split(",")

                val compreFaceImageIdMap = mutableMapOf<String, Any?>()
//                    val recognitionLabelList = mutableListOf<RecognitionLabel>()
                val recognitionLabelPhotoList = mutableListOf<RecognitionLabelPhoto>()

                for (recognitionLabel in recognitionLabelArray) {
                    if (recognitionLabel.trim().isNotBlank() && recognitionLabel.trim() != "null") {
                        val recognitionLabelRecord =
                            recognitionLabelRepository?.findByNameIgnoreCase(recognitionLabel.trim())
                        var recognitionLabelObj = RecognitionLabel()
                        if (recognitionLabelRecord == null) {
                            recognitionLabelObj.setName(recognitionLabel.trim())
                            recognitionLabelObj.setCreatedAt(getCurrentTimestamp())
                            recognitionLabelObj.setModifiedAt(getCurrentTimestamp())
                            recognitionLabelObj.setCoverUrl(metadataObj.getThumbnailUrlCentered())
                            recognitionLabelRepository?.save(recognitionLabelObj)
                            //recognitionLabelList.add(recognitionLabelObj)
                        } else {
                            recognitionLabelObj = recognitionLabelRecord
                        }
                        val recognitionLabelPhotoCount =
                            recognitionLabelPhotoRepository?.countByRecognitionLabelIdAndMetadataId(
                                recognitionLabelObj.getId(),
                                metadataId
                            )

                        if (recognitionLabelPhotoCount == 0 || addPerson) {
                            // Delete person before adding to existing list of people
                            if (recognitionLabelPhotoCount != null && addPerson && recognitionLabelPhotoCount > 0) {
                                recognitionLabelPhotoRepository?.deleteByRecognitionLabelIdAndMetadataId(
                                    recognitionLabelObj.getId(),
                                    metadataId
                                )
                            }
                            val recognitionLabelPhotoObj = RecognitionLabelPhoto()
                            recognitionLabelPhotoObj.setMetadataId(metadataObj.getId())
                            recognitionLabelPhotoObj.setRecognitionLabelId(recognitionLabelObj.getId())
                            // 0.0 confirmed recognition by user
                            recognitionLabelPhotoObj.setConfidence("0.0")
                            //recognitionLabelPhotoObj.setCompreFaceImageId(compreFaceImageId)
                            recognitionLabelPhotoList.add(recognitionLabelPhotoObj)

                            buildPersonUpload(
                                settings,
                                recognitionLabel,
                                metadataObj,
                                compreFaceImageIdMap
                            )
                        }
                    }
                }

                if (recognitionLabelPhotoList.isNotEmpty()) {
                    recognitionLabelPhotoRepository?.saveAll(recognitionLabelPhotoList)
                }

            }
        }
    }

    fun buildPersonUpload(settings: Settings, recognitionLabel: String, metadataObj: Metadata?, compreFaceImageIdMap: MutableMap<String, Any?>) {
        Thread {
            ImageProcessing.Companion.buildPersonUpload(
                settings,
                recognitionLabel,
                metadataObj,
                compreFaceImageIdMap
            )
        }.start()
    }
}