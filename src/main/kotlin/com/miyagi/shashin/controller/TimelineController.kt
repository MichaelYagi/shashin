package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.*
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.ApiResponse
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.TextUtils
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import io.swagger.v3.oas.annotations.Operation
import net.iakovlev.timeshape.TimeZoneEngine
import org.apache.commons.text.StringEscapeUtils
import org.springdoc.core.annotations.RouterOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.InputStreamResource
import org.springframework.http.*
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import javax.transaction.Transactional
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString


@Controller
@Secured("ROLE_ADMIN")
class TimelineController: BaseController() {

    @Autowired
    private lateinit var metadataRepository: MetadataRepository

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

    @Value("\${app.sidecar.path}")
    private var relativeSidecarDir: String? = null

    private var logger: Logger = Logger.getLogger(TimelineController::class.simpleName)

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, Any?>()

    @RequestMapping(value = ["/timeline", "/timeline/{mediaType}"], method = [RequestMethod.GET])
    fun getTimelineMediaTypeByDate(model: Model,@PathVariable(required = false) mediaType: String?): String {
        return buildTimelineModel(model,mediaType)
    }

    private fun buildTimelineModel(model: Model,mediaTypeFilter: String?): String {
        val module = "timeline"

        val validMediaTypes = arrayOf("all","video")

        var mediaType = mediaTypeFilter

        if (mediaTypeFilter.isNullOrEmpty()) {
            mediaType = "all"
        }

        val initialMetadataObj = if (mediaType != "all") {
            metadataRepository.findDistinctFirstByHiddenIsFalseByMediaTypeOrderByYearDescMonthDescDayDesc(mediaType!!)
        } else {
            metadataRepository.findDistinctFirstByHiddenIsFalseOrderByYearDescMonthDescDayDesc()
        }
        var date = "undated"
        if (initialMetadataObj != null && initialMetadataObj.getYear() != null && initialMetadataObj.getMonth() != null && initialMetadataObj.getDay() != null) {
            date = initialMetadataObj.getYear().toString() + "-" + initialMetadataObj.getMonth().toString() + "-" + initialMetadataObj.getDay().toString()
        }

        val dates = getMetadataDates(mediaType)
        model["metadataDates"] = dates["metadataDates"]!!

        val timelineData = buildTimelineDataByDate(model,mediaType,date,false)

        for ((k, v) in timelineData) {
            model[k] = v!!
        }

        val countByYearAndMonthList = metadataRepository.countByYearAndMonth()
        val countByYearAndMonthMap = mutableMapOf<String, Int>()
        if (countByYearAndMonthList.count() > 0) {
            for (yearMonthCount in countByYearAndMonthList) {
                countByYearAndMonthMap[yearMonthCount.getYear().toString() + "-" + yearMonthCount.getMonth().toString()] = yearMonthCount.getCount()!!
            }
        }
        model["metadataYearMonthCount"] = countByYearAndMonthMap

        if (!validMediaTypes.contains(mediaType)) {
            model["message"] = "Oops! $mediaType is not a valid media type!"
        }

        getAllAttribueData(model)

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)

        return module
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getTimelineMediaType",
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
    @RequestMapping(value = ["/timeline/mediatype/{mediaType}", "/api/v1/timeline/mediatype/{mediaType}"], method = [RequestMethod.GET])
    fun getTimelineMediaType(model: Model,@PathVariable mediaType: String): String {
        val module = "timeline"
        val response = buildTimelineData(model,mediaType,0)
        for ((k, v) in response) {
            model[k] = v!!
        }

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RequestMapping(value = ["/timeline/mediatype/{mediaType}/{page}","/api/v1/timeline/mediatype/{mediaType}/page/{page}"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun getPagedTimeline(model: Model, @PathVariable page: Int,@PathVariable mediaType: String): String {
        return mapper.writeValueAsString(buildTimelineData(model,mediaType,page))
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getTimelineJson",
            description = "<strong>Get paged or all results for timeline content with associated favorites mapping.</strong> Pages start from 0. The page size can be configured through the web interface (default 20).<br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/timeline/{page}\" \\\n" +
                    "-H \"Content-Type: application/json\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>Content-Type</td><td>header</td><td>string</td><td>required</td><td>application/json</td></tr>" +
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
    @RequestMapping(value = ["/api/v1/timeline","/api/v1/timeline/{page}"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun getTimelineJson(model: Model, @PathVariable(required = false) page: Int?): String {
        var pageValue = 0
        if (page != null) {
            pageValue = page
        }
        return mapper.writeValueAsString(buildTimelineData(model,"all",pageValue))
    }

    private fun buildTimelineData(model: Model,mediaTypeFilter: String,page: Int): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        var mediaType = "photo"
        if (mediaTypeFilter == "video") {
            mediaType = mediaTypeFilter
        }
        response["message"] = "There are no "+mediaType+"s. Please setup directories to scan in Settings and index media in Media Indexing."
        response["metadataList"] = mutableListOf<Metadata>()
        response["favorites"] = mutableMapOf<String, Any>()
        response["mediaTypeFilter"] = mediaTypeFilter

        response["msg"] = "Could not get results"
        response["status"] = ApiResponse.FAIL.status

        val favoritesMap = HashMap<String, HashMap<String, Any>>()
        if (model.getAttribute("currentUser") != "") {
            val currentUserObj = model.getAttribute("currentUser") as User?
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

            if (metadataList.isNotEmpty()) {
                response["message"] = ""
                val favoriteCounts = favoriteRepository.countByMetadataIdIn(metadataList.map { it.getId() }.toList())
                if (favoriteCounts.count() > 0) {
                    for (favoriteCount in favoriteCounts) {
                        favoritesMap[favoriteCount.getMetadataId()!!] = hashMapOf(
                            "favorite" to (favoriteCount.getUserId() == currentUserObj?.getId()),
                            "count" to favoriteCount.getCount() as Any
                        )
                    }
                }

                response["favorites"] = favoritesMap
            }

            response["metadataList"] = metadataList
            response["favorites"] = favoritesMap
            response["msg"] = "Results"
            response["status"] = ApiResponse.SUCCESS.status
        }

        return response
    }

    @RequestMapping(value = ["/timeline/mediatype/{mediaType}/date/{date}"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Cacheable(value = ["allMetadataAndAttributesByDate"], key = "{#date, #mediaType}")
    fun getTimelineByDate(model: Model, @PathVariable date: String,@PathVariable mediaType: String): ResponseEntity<String> {
        val json = mapper.writeValueAsString(buildTimelineDataByDate(model,mediaType,date,false))
        return ResponseEntity
            .ok()
//            .eTag(UUID.nameUUIDFromBytes(json.toByteArray()).toString())
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
            .body(json)
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getTimelineByDateApi",
            description = "<strong>Get paged or all results for timeline content with associated favorites mapping.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/timeline/mediatype/{mediaType}/date/{date}\" \\\n" +
                    "-H \"Content-Type: application/json\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>Content-Type</td><td>header</td><td>string</td><td>required</td><td>application/json</td></tr>" +
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
    @RequestMapping(value = ["/api/v1/timeline/mediatype/{mediaType}/date/{date}"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Cacheable(value = ["allMetadataAndAttributesByDate"], key = "{#date, #mediaType}")
    fun getTimelineByDateApi(model: Model, @PathVariable date: String,@PathVariable mediaType: String): String {
        return mapper.writeValueAsString(buildTimelineDataByDate(model,mediaType,date,false))
    }

    @RequestMapping(value = ["/timeline/mediatype/{mediaType}/date/{date}/metadata"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Cacheable(value = ["allMetadataOnlyByDate"], key = "{#date, #mediaType}")
    fun getTimelineMetadataByDate(model: Model, @PathVariable date: String,@PathVariable mediaType: String): ResponseEntity<String> {
        val jsonMap = buildTimelineDataByDate(model,mediaType,date,true)
        val json = mapper.writeValueAsString(jsonMap)
        return ResponseEntity
            .ok()
//            .eTag(UUID.nameUUIDFromBytes(json.toByteArray()).toString())
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
            .body(json)
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getTimelineMetadataByDateApi",
            description = "<strong>Get minimal metadata information by date.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/timeline/mediatype/{mediaType}/date/{date}/metadata\" \\\n" +
                    "-H \"Content-Type: application/json\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>Content-Type</td><td>header</td><td>string</td><td>required</td><td>application/json</td></tr>" +
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
    @RequestMapping(value = ["/api/v1/timeline/mediatype/{mediaType}/date/{date}/metadata"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Cacheable(value = ["allMetadataOnlyByDate"], key = "{#date, #mediaType}")
    fun getTimelineMetadataByDateApi(model: Model, @PathVariable date: String,@PathVariable mediaType: String): String {
        val jsonMap = buildTimelineDataByDate(model,mediaType,date,true)
        return mapper.writeValueAsString(jsonMap)
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getAllKeywords",
            description = "<strong>Get a list of all keywords.</strong>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/keywords\" \\\n" +
                    "-H \"Content-Type: application/json\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>Content-Type</td><td>header</td><td>string</td><td>required</td><td>application/json</td></tr>" +
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
    @RequestMapping(value = ["/api/v1/keywords"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
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
            description = "<strong>Get a list of all timeline dates by media type.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/timeline/dates/{mediaType}\" \\\n" +
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
    @RequestMapping(value = ["/timeline/dates/{mediaType}","/api/v1/timeline/dates/{mediaType}"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun getTimelineDates(model: Model, @PathVariable mediaType: String): String {
        return mapper.writeValueAsString(getMetadataDates(mediaType))
    }

    private fun getMetadataDates(mediaType: String): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        response["metadataDates"] = mutableListOf<MetadataDate>()
        response["msg"] = "Could not get results"
        response["status"] = ApiResponse.FAIL.status

        val metadataDates = if (mediaType == "all") {
            metadataRepository.findAllYearMonthDay()
        } else {
            metadataRepository.findAllYearMonthDayByMediaType(mediaType)
        }
        if (metadataDates != null) {
            response["msg"] = "Success"
            response["status"] = ApiResponse.SUCCESS.status
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
        response["message"] = "There are no "+mediaType+"s. Please setup directories to scan in Settings and index media in Media Indexing."
        response["metadataList"] = mutableListOf<Metadata>()
        response["favorites"] = mutableMapOf<String, Any>()
        response["mediaTypeFilter"] = mediaTypeFilter

        response["msg"] = "Could not get results"
        response["status"] = ApiResponse.FAIL.status

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

                if (metadataOnly) {
                    val metadataList: MutableList<MetadataFocused> = if (mediaTypeFilter == "all") {
                        metadataRepository.findTimelineDateFocused(
                            year, month, day
                        ).toMutableList()
                    } else {
                        metadataRepository.findAllByTypeAndYearAndMonthAndDayFocused(
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
                    val metadataList: MutableList<Metadata> = if (mediaTypeFilter == "all") {
                        metadataRepository.findAllByYearAndMonthAndDayAndHiddenEqualsOrderByYearDescMonthDescDayDescTimeDesc(
                            year, month, day, false
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
                            }
                        }

                        response["favorites"] = favoritesMap
                    }
                }

                response["msg"] = "Results"
                response["status"] = ApiResponse.SUCCESS.status
            }
        }

        return response
    }

    @RequestMapping(value = ["/timeline/remove/{metadataId}"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Transactional
    @CacheEvict(value = ["allMetadataByDate", "allMetadataByDateAndType", "allMetadataOnlyByDate", "allMetadataAndAttributesByDate", "singleMetadataRequest", "allAlbumMetadataWithCoordinates", "allMetadataWithCoordinates"], allEntries = true)
    fun removeMetadata(model: Model, @RequestBody requestBody: JsonNode, @PathVariable metadataId: String): String? {
//        println(requestBody)
        val metadataMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})

        if (metadataMap.containsKey("id") &&
            metadataMap.containsKey("hidden") &&
            metadataMap["id"].toString() == metadataId
        ) {
            val metadataObj = metadataRepository.findById(metadataId)
            val isHidden = metadataMap["hidden"].toString().toBoolean()

            if (isHidden) {
                metadataObj.get().setHidden(true)
                metadataObj.get().setModifiedAt(getCurrentTimestamp())
                removeMetadata(metadataId)
            }

            // Update record
            metadataRepository.save(metadataObj.get())

            resp["msg"] = "Saved!"
            resp["status"] = ApiResponse.SUCCESS.status
            return mapper.writeValueAsString(resp)
        }
        resp["msg"] = "Could not save"
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "updateMetadata",
            description = "<strong>Update metadata.</strong><br>" +
                    "<pre><code>" +
                    "curl -X POST \"http://127.0.0.1:6624/api/v1/update/metadata/{metadataId}\" \\\n" +
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
    @RequestMapping(value = ["/timeline/update/{metadataId}","/api/v1/update/metadata/{metadataId}"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @CacheEvict(value = ["allMetadataByDate", "allMetadataByDateAndType", "allMetadataOnlyByDate", "allMetadataAndAttributesByDate", "singleMetadataRequest", "allAlbumMetadataWithCoordinates", "allMetadataWithCoordinates"], allEntries = true)
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
            metadataMap.containsKey("description") &&
            metadataMap.containsKey("albumnames") &&
            metadataMap.containsKey("tagpeople") &&
            metadataMap.containsKey("hidden") &&
            metadataMap.containsKey("isObject") &&
            metadataMap.containsKey("camera") &&
            metadataMap.containsKey("lens") &&
            metadataMap["id"].toString() == metadataId
        ) {
            resp["msg"] = "Saved!"
            resp["status"] = ApiResponse.SUCCESS.status

            val metadataObj = metadataRepository.findById(metadataId)
            val currentUserObj = model.getAttribute("currentUser") as User?

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

                    val albumId = processAlbum(albumNameRaw, currentUserObj, metadataObj.get())

                    if (albumId > 0) {
                        val albumPhotoCount = albumPhotoRepository.countByMetadataIdAndAlbumId(metadataId, albumId)!!
                        if (albumPhotoCount == 0) {
                            val albumPhotoObj = AlbumPhoto()
                            albumPhotoObj.setMetadataId(metadataId)
                            albumPhotoObj.setAlbumId(albumId)
                            albumPhotoObj.setCreatedAt(getCurrentTimestamp())
                            albumPhotoObj.setModifiedAt(getCurrentTimestamp())
                            albumPhotoRepository.save(albumPhotoObj)
                        }

                        if (currentAlbumIdList.contains(albumId)) {
                            // Collect to delete
                            val indexToRemove = currentAlbumIdList.indexOf(albumId)
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
            if (metadataMap["tagpeople"].toString().isBlank()) {
                recognitionLabelPhotoRepository?.deleteByMetadataId(metadataId)
            } else {
                processPeople(model.getAttribute("settings") as Settings, metadataObj.get(), metadataMap["tagpeople"].toString(), metadataMap["isObject"].toString().toBoolean())
            }

            if (metadataMap["title"].toString().trim() == "") {
                metadataObj.get().setTitle(metadataObj.get().getFileName())
            } else if (metadataObj.get().getTitle() != metadataMap["title"].toString().trim()) {
                metadataObj.get().setTitle(StringEscapeUtils.escapeHtml4(metadataMap["title"].toString()).trim())
            }
            if (metadataMap["description"].toString().trim() == "") {
                metadataObj.get().setDescription(null)
            } else if (metadataObj.get().getDescription() != metadataMap["description"].toString().trim()) {
                metadataObj.get().setDescription(StringEscapeUtils.escapeHtml4(metadataMap["description"].toString()).trim())
            }
            if (metadataMap["camera"].toString().trim() != "") {
                var camera = StringEscapeUtils.escapeHtml4(metadataMap["camera"].toString()).trim()
                val cameraTypes = metadataRepository.findByCameraTypeAlphabetical()
                for (cameraType in cameraTypes) {
                    if (camera.trim().lowercase() == cameraType.trim().lowercase()) {
                        camera = cameraType
                        break
                    }
                }

                if (metadataObj.get().getCamera() != camera) {
                    metadataObj.get().setCamera(camera)
                }
            } else {
                metadataObj.get().setCamera(null)
            }
            if (metadataMap["lens"].toString().trim() != "") {
                var lens = StringEscapeUtils.escapeHtml4(metadataMap["lens"].toString()).trim()
                val lensTypes = metadataRepository.findByLensTypeAlphabetical()
                for (lensType in lensTypes) {
                    if (lens.trim().lowercase() == lensType.trim().lowercase()) {
                        lens = lensType
                        break
                    }
                }

                if (metadataObj.get().getLens() != lens) {
                    metadataObj.get().setLens(lens)
                }
            } else {
                metadataObj.get().setLens(null)
            }
            if (metadataMap["year"].toString() == "") {
                metadataObj.get().setYear(null)
            } else if (metadataObj.get().getYear() != metadataMap["year"].toString().toInt()) {
                metadataObj.get().setYear(StringEscapeUtils.escapeHtml4(metadataMap["year"].toString()).toInt())
            }
            if (metadataMap["month"].toString() == "") {
                metadataObj.get().setMonth(null)
            } else if (metadataObj.get().getMonth() != metadataMap["month"].toString().toInt()) {
                metadataObj.get().setMonth(StringEscapeUtils.escapeHtml4(metadataMap["month"].toString()).toInt())
            }
            if (metadataMap["day"].toString() == "") {
                metadataObj.get().setDay(null)
            } else if (metadataObj.get().getDay() != metadataMap["day"].toString().toInt()) {
                metadataObj.get().setDay(StringEscapeUtils.escapeHtml4(metadataMap["day"].toString()).toInt())
            }
            if (metadataMap["time"].toString() == "") {
                metadataObj.get().setTime(null)
            } else if (metadataObj.get().getTime() != metadataMap["time"].toString()) {
                metadataObj.get().setTime(StringEscapeUtils.escapeHtml4(metadataMap["time"].toString()))
            }
            if (metadataMap["offset"].toString() == "") {
                metadataObj.get().setTimeZone(null)
            } else if (metadataObj.get().getTimeZone() != metadataMap["offset"].toString()) {
                metadataObj.get().setTimeZone(StringEscapeUtils.escapeHtml4(metadataMap["offset"].toString()))
            }

            keywordPhotoRepository.deleteAllByMetadataId(metadataId)
            if (metadataMap["keywords"].toString().isNotBlank()) {
                var keywords = StringEscapeUtils.escapeHtml4(metadataMap["keywords"].toString()).trim()
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

            if (metadataMap["latlng"].toString() == "") {
                metadataObj.get().setLat(null)
                metadataObj.get().setLng(null)
            } else {
                val coordinateMap = processCoordinates(metadataMap["latlng"].toString())
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
            }

            // Update record
            metadataObj.get().setModifiedAt(getCurrentTimestamp())
            metadataRepository.save(metadataObj.get())

            val attrResponse = getAllAttribueData(model)
            for ((k, v) in attrResponse) {
                resp[k] = v
            }

            return mapper.writeValueAsString(resp)
        }
        logger.log(Level.WARNING, "Updating metadata failed. Could not save.")
        resp["msg"] = "Could not save"
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/timeline/remove/batch"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Transactional
    @CacheEvict(value = ["allMetadataByDate", "allMetadataByDateAndType", "allMetadataOnlyByDate", "allMetadataAndAttributesByDate", "singleMetadataRequest", "allAlbumMetadataWithCoordinates", "allMetadataWithCoordinates"], allEntries = true)
    fun removeBatchMetadata(model: Model, @RequestBody requestBody: JsonNode): String? {
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

            for (id in idArray) {
                val metadataObj: Optional<Metadata?> = metadataRepository.findById(id)
                val metadata = metadataObj.get()

                if (isHidden) {
                    metadata.setModifiedAt(getCurrentTimestamp())
                    metadata.setHidden(true)
                    removeMetadata(id)
                }

                metadataList.add(metadata)
            }

            if (metadataList.isNotEmpty()) {
                // Update record
                metadataRepository.saveAll(metadataList)

                resp["msg"] = "Saved!"
                resp["status"] = ApiResponse.SUCCESS.status
                return mapper.writeValueAsString(resp)
            }
        }
        logger.log(Level.WARNING, "Removing batch metadata failed. Could not save.")
        resp["msg"] = "Could not save"
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "updateBatchMetadata",
            description = "<strong>Update a batch of metadata.</strong><br>" +
                    "<pre><code>" +
                    "curl -X POST \"http://127.0.0.1:6624/api/v1/update/metadata/batch\" \\\n" +
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
                    "<tr><td>batchhidden</td><td>body param</td><td>boolean</td><td>required</td><td>Flag to batch hide/unhide media</td></tr>" +
                    "<tr><td>batchisobject</td><td>body param</td><td>boolean</td><td>required</td><td>Flag to batch tag media as an object</td></tr>" +
                    "<tr><td>cameraBatchData</td><td>body param</td><td>string</td><td>required</td><td>Batch tag media with the name of the camera used</td></tr>" +
                    "<tr><td>lensBatchData</td><td>body param</td><td>string</td><td>required</td><td>Batch tag media with the name of the lens used</td></tr>" +
                    "<tr><td>yearTakenBatchData</td><td>body param</td><td>string</td><td>required</td><td>Batch tag media with the year media was taken</td></tr>" +
                    "<tr><td>monthTakenBatchData</td><td>body param</td><td>string</td><td>required</td><td>Batch tag media with the month media was taken</td></tr>" +
                    "<tr><td>dayTakenBatchData</td><td>body param</td><td>string</td><td>required</td><td>Batch tag media with the day media was taken</td></tr>" +
                    "<tr><td>offsetTakenBatchData</td><td>body param</td><td>string</td><td>required</td><td>Batch tag media with the offset time media was taken</td></tr>" +
                    "<tr><td>albumNameInput</td><td>body param</td><td>string</td><td>required</td><td>Batch tag media with a comma separated list of album names</td></tr>" +
                    "<tr><td>tagBatchDataInput</td><td>body param</td><td>string</td><td>required</td><td>Batch tag media with a comma separated list of subjects</td></tr>" +
                    "<tr><td>keywordsBatchData</td><td>body param</td><td>string</td><td>required</td><td>Batch tag media with a comma separated list of keywords</td></tr>" +
                    "<tr><td>latlngBatchData</td><td>body param</td><td>string</td><td>required</td><td>Batch tag media with a latitude and longitude set separated by a comma</td></tr>" +
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
    @RequestMapping(value = ["/timeline/update/batch","/api/v1/update/metadata/batch"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @CacheEvict(value = ["allMetadataByDate", "allMetadataByDateAndType", "allMetadataOnlyByDate", "allMetadataAndAttributesByDate", "singleMetadataRequest", "allAlbumMetadataWithCoordinates", "allMetadataWithCoordinates"], allEntries = true)
    fun updateBatchMetadata(model: Model, @RequestBody requestBody: JsonNode): String? {
//         println(requestBody)
        val batchMetadataMap = mapper.convertValue(requestBody, object : TypeReference<BatchMetadataInput>() {})

        val idArray: Array<String>? = batchMetadataMap.batchMetadataIds
        val dayTaken: Int? = batchMetadataMap.dayTakenBatchData
        val monthTaken: Int? = batchMetadataMap.monthTakenBatchData
        val yearTaken: Int? = batchMetadataMap.yearTakenBatchData
        val latlng: String? = StringEscapeUtils.escapeHtml4(batchMetadataMap.latlngBatchData)
        val offset: String? = StringEscapeUtils.escapeHtml4(batchMetadataMap.offsetTakenBatchData)
        var camera: String? = StringEscapeUtils.escapeHtml4(batchMetadataMap.cameraBatchData)
        var lens: String? = StringEscapeUtils.escapeHtml4(batchMetadataMap.lensBatchData)
        var keywords: String? = StringEscapeUtils.escapeHtml4(batchMetadataMap.keywordsBatchData)
        val recognitionLabelNames: String? = StringEscapeUtils.escapeHtml4(batchMetadataMap.tagBatchDataInput)
        val albumNames: String? = StringEscapeUtils.escapeHtml4(batchMetadataMap.albumNameInput)
//        println(albumNames)
        val isObject = batchMetadataMap.batchisobject == "on"
        val isHidden = batchMetadataMap.batchhidden == "on"

        if (!idArray.isNullOrEmpty()) {
            resp["msg"] = "Saved!"
            resp["status"] = ApiResponse.SUCCESS.status

            val settings = model.getAttribute("settings") as Settings

            val headers = HttpHeaders()
            headers.add("x-api-key", settings.getCompreFaceKey())

            val firstAvailableMetadataId = StringEscapeUtils.escapeHtml4(idArray[0])
            val metadataCoverAlbumObj = metadataRepository.findById(firstAvailableMetadataId)
            var albumIdList: ArrayList<Int> = ArrayList()

            // Process albums
            if (!isHidden && albumNames != null && albumNames.toString().trim() != "") {
                val albumNameList = albumNames.toString().split(",")

                val currentUserObj = model.getAttribute("currentUser") as User?

                for (albumNameRaw in albumNameList) {
                    val albumId = processAlbum(albumNameRaw, currentUserObj, metadataCoverAlbumObj.get())
                    if (albumId > 0) {
                        albumIdList.add(albumId)
                    }
                }
            }

            if (camera != null && camera.trim().isNotBlank()) {
                val cameraTypes = metadataRepository.findByCameraTypeAlphabetical()
                for (cameraType in cameraTypes) {
                    if (camera!!.trim().lowercase() == cameraType.trim().lowercase()) {
                        camera = cameraType.trim()
                        break
                    }
                }
            }

            if (lens != null && lens.trim().isNotBlank()) {
                val lensTypes = metadataRepository.findByLensTypeAlphabetical()
                for (lensType in lensTypes) {
                    if (lens!!.trim().lowercase() == lensType.trim().lowercase()) {
                        lens = lensType.trim()
                        break
                    }
                }
            }

            val metadataList: ArrayList<Metadata> = ArrayList()

            // Process keyword and lat/lng data
            val coordinateMap = processCoordinates(latlng)
            val lat = coordinateMap["lat"]
            val lng = coordinateMap["lng"]
            val place = coordinateMap["place"]
            val timezone = coordinateMap["timezone"]

            var keywordList = mutableListOf<String>()
            if (keywords != null && keywords.isNotBlank()) {
                keywords = keywords.toString().trim()
                if (keywords.last() == ',') {
                    keywords = keywords.dropLast(1)
                }
                keywordList = keywords.split(",").map { it.trim() } as MutableList<String>
            }

            for (idVal in idArray) {
                val id = StringEscapeUtils.escapeHtml4(idVal)
                val metadataObj: Optional<Metadata?> = metadataRepository.findById(id)
                val metadata = metadataObj.get()
                metadata.setModifiedAt(getCurrentTimestamp())

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

                    processPeople(model.getAttribute("settings") as Settings, metadata, recognitionLabelNames.toString(), isObject)

                    if (dayTaken != null) {
                        metadata.setDay(dayTaken)
                    }
                    if (monthTaken != null) {
                        metadata.setMonth(monthTaken)
                    }
                    if (yearTaken != null) {
                        metadata.setYear(yearTaken)
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

                    if (lat != null && lng != null) {
                        metadata.setLat(lat)
                        metadata.setLng(lng)

                        if (place != null) {
                            metadata.setPlaceName(place)
                        }

                        if (timezone != null) {
                            metadata.setTimeZone(timezone)
                        }
                    }

                    if (keywordList.isNotEmpty()) {
                        keywordPhotoRepository.deleteAllByMetadataId(metadata.getId())
                        processKeywords(keywordList, metadata.getId())
                    }
                }

                metadata.setModifiedAt(getCurrentTimestamp())
                metadataList.add(metadata)
            }

            val keywordIdsToDelete = keywordRepository.findAllOrphanedKeywordIds()
            if (keywordIdsToDelete.count() > 0) {
                keywordRepository.deleteAllById(keywordIdsToDelete)
            }

            if (metadataList.isNotEmpty()) {
                // Update record
                metadataRepository.saveAll(metadataList)

                val attrResponse = getAllAttribueData(model)
                for ((k, v) in attrResponse) {
                    resp[k] = v
                }

                return mapper.writeValueAsString(resp)
            }
        }
        logger.log(Level.WARNING, "Updating batch metadata failed. Could not save.")
        resp["msg"] = "Could not save"
        resp["status"] = ApiResponse.FAIL.status
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

    @RequestMapping(value = ["/timeline/sync/{metadataId}"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
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
                metadataObj.setModifiedAt(getCurrentTimestamp())
                metadataRepository.save(metadataObj)

                resp["year"] = year.toString()
                resp["month"] = month.toString()
                resp["day"] = day.toString()
                resp["time"] = time

                resp["msg"] = "Saved"
                resp["status"] = ApiResponse.SUCCESS.status
                return mapper.writeValueAsString(resp)
            }
        }


        resp["msg"] = "Could not save"
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getMetadata",
            description = "<strong>Get metadata information and associated keyword and favorites data.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/metadata/{id}\" \\\n" +
                    "-H \"Content-Type: application/json\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>Content-Type</td><td>header</td><td>string</td><td>required</td><td>application/json</td></tr>" +
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
    @RequestMapping(value = ["/api/v1/metadata/{id}"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Cacheable(value = ["singleMetadataRequest"], key = "{#id}")
    @Secured("ROLE_ADMIN","ROLE_USER")
    fun getMetadata(model: Model, @PathVariable(required = true) id: String): ResponseEntity<String> {
        val response = mutableMapOf<String, Any?>()
        val keywordArray = mutableListOf<String>()
        val keywords = keywordRepository.findKeywordsByMetadataId(id)
        for (keyword in keywords) {
            keywordArray.add(keyword.getKeyword()!!)
        }
        response["keywordList"] = keywordArray

        val emptyJson = "{}"
        val mapper = ObjectMapper()
        response["metadata"] = mapper.readTree(emptyJson)

        val metadataRecord = metadataRepository.findById(id)
        if (metadataRecord.isPresent) {
            response["metadata"] = metadataRecord.get()
        }

        val favoritesMap = HashMap<String, HashMap<String, Any>>()
        val idList = mutableListOf<String>()
        idList.add(id)
        val currentUserObj = model.getAttribute("currentUser") as User?
        val favoriteCounts = favoriteRepository.countByMetadataIdIn(idList)
        if (favoriteCounts.count() > 0) {
            for (favoriteCount in favoriteCounts) {
                favoritesMap[favoriteCount.getMetadataId()!!] = hashMapOf(
                    "favorite" to (favoriteCount.getUserId() == currentUserObj?.getId()),
                    "count" to favoriteCount.getCount() as Any
                )
            }
        }
        response["favorites"] = favoritesMap

        response["msg"] = ""
        response["status"] = ApiResponse.SUCCESS.status

        val json = mapper.writeValueAsString(response)
        return ResponseEntity
            .ok()
//            .eTag(UUID.nameUUIDFromBytes(json.toByteArray()).toString())
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
            .body(json)
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getTimelineMetadata",
            description = "<strong>Get media info for a timeline view.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/timeline/metadata/{id}\" \\\n" +
                    "-H \"Content-Type: application/json\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>Content-Type</td><td>header</td><td>string</td><td>required</td><td>application/json</td></tr>" +
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
    @RequestMapping(value = ["/api/v1/timeline/metadata/{id}"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun getTimelineMetadata(model: Model, @PathVariable(required = true) id: String): String {
        val response = mutableMapOf<String, Any?>()

        response["allAlbumList"] = mutableListOf<Album>()
        response["allRecognitionLabels"] = mutableListOf<RecognitionLabel>()
        val albumArray = mutableListOf<String>()
        response["albumList"] = albumArray
        val labelArray = mutableListOf<String>()
        response["taggedPeopleList"] = labelArray

        val emptyJson = "{}"
        val mapper = ObjectMapper()
        response["metadata"] = mapper.readTree(emptyJson)

        val metadataRecord = metadataRepository.findById(id)
        if (metadataRecord.isPresent) {
            response["metadata"] = metadataRecord.get()

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

            val albumPhotos = albumPhotoRepository.findAlbumPhotoByMetadataId(id)
            if (albumPhotos != null) {
                for (albumPhoto in albumPhotos) {
                    val album = albumRepository.findById(albumPhoto!!.getAlbumId()!!)
                    albumArray.add(album.get().getName()!!)
                }
            }
            response["albumList"] = albumArray

            val keywordArray = mutableListOf<String>()
            val keywords = keywordRepository.findKeywordsByMetadataId(id)
            for (keyword in keywords) {
                keywordArray.add(keyword.getKeyword()!!)
            }
            response["keywordList"] = keywordArray

            val allRecognitionLabels = recognitionLabelRepository?.findAllByNameNotContaining("object")
            if (allRecognitionLabels != null && allRecognitionLabels.count() > 0) {
                response["allRecognitionLabels"] = allRecognitionLabels
            }

            val allAlbumList = albumRepository.findAllOrderByAlbumName()
            if (allAlbumList.count() > 0) {
                response["allAlbumList"] = allAlbumList
            }
        }

        response["msg"] = ""
        response["status"] = ApiResponse.SUCCESS.status

        return mapper.writeValueAsString(response)
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getExifData",
            description = "<strong>Get EXIF data extracted using <a href=\"https://github.com/drewnoakes/metadata-extractor\">Metadata Extractor</a>.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/exif/metadata/{id}\" \\\n" +
                    "-H \"Content-Type: application/json\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>Content-Type</td><td>header</td><td>string</td><td>required</td><td>application/json</td></tr>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>id</td><td>param</td><td>string</td><td>required</td><td>The metadata ID</td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "Different outputs depending on media. See <a href=\"https://github.com/drewnoakes/metadata-extractor\">Metadata Extractor</a> for more details."
        )
    )
    @RequestMapping(value = ["/api/v1/exif/metadata/{id}"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun getExifData(model: Model, @PathVariable(required = true) id: String): String {
        val response = mutableMapOf<String, Any?>()

        val metadataRecord = metadataRepository.findById(id)
        if (metadataRecord.isPresent) {
            val metadata = metadataRecord.get()

            val jsonNode = FileUtils.convertExifToJsonNode(metadata.getFolder()!!, metadata.getFileName()!!, relativeSidecarDir!!)

            response["msg"] = "Could not get EXIF file"
            response["status"] = ApiResponse.FAIL.status

            if (jsonNode != null) {
                response["exif"] = jsonNode
                response["msg"] = ""
                response["status"] = ApiResponse.SUCCESS.status
            }
        } else {
            response["msg"] = "Could not get record"
            response["status"] = ApiResponse.FAIL.status
        }

        return mapper.writeValueAsString(response)
    }

    @RequestMapping(value = ["/timeline/download/batch"],
        method = [RequestMethod.POST],
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE]
    )
    @ResponseBody
    fun downloadBatchMetadata(model: Model, @RequestParam paramMap: Map<String, String>): ResponseEntity<InputStreamResource>? {

        if (paramMap.containsKey("batchMetadataIds")) {
            val idArray: Array<String>? = mapper.readValue(paramMap["batchMetadataIds"], object : TypeReference<Array<String>>() {})
            if (idArray != null && idArray.isNotEmpty()) {
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

    private fun processAlbum(albumNameRaw: String, currentUserObj: User?, metadataObj: Metadata?): Int {
//        val albumIdList: ArrayList<Int> = ArrayList()
        var albumId = 0

        if (albumNameRaw.trim().isNotBlank() && currentUserObj != null) {
            val albumName = StringEscapeUtils.escapeHtml4(albumNameRaw).trim().replace(" +".toRegex()," ")
            val albumObject = albumRepository.findAlbumByNameIgnoreCase(albumName)
            var albumObj = Album()

            if (albumObject != null) {
                albumId = albumObject.getId()
            } else {
                if (metadataObj != null && metadataObj.getThumbnailUrlCentered() != null) {
                    albumObj.setCoverUrl(metadataObj.getThumbnailUrlCentered())
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

    private fun processCoordinates(latlngStr: String?): Map<String, String?> {
        val coordinateMap = mutableMapOf<String, String?>()
        coordinateMap["lat"] = null
        coordinateMap["lng"] = null
        coordinateMap["place"] = null
        coordinateMap["timezone"] = null

        var lat: String? = null
        var lng: String? = null
        var place: String? = null
        var timezone: String? = null

        if (latlngStr != null && latlngStr.trim().isNotBlank()) {
            val latlng = latlngStr.replace("\\s".toRegex(), "")
            val latlngArr = latlng.split(",")
            val latlngRegex = "^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?)\\s*,\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)$".toRegex()
            if (latlngRegex.matches(latlng) && latlngArr.count() == 2) {
                lat = latlngArr[0]
                lng = latlngArr[1]

                val buildPlace = TextUtils.getPlaceNameFromJson(TextUtils.getGeoData(geocodeUrl!!,lat, lng))
                if (buildPlace.isNotBlank()) {
                    place = buildPlace

                    val engine = TimeZoneEngine.initialize()
                    val maybeZoneId: Optional<ZoneId> = engine.query(lat.toDouble(), lng.toDouble())
                    val zone = ZoneId.of(maybeZoneId.get().id)
                    val dt = LocalDateTime.now()
                    val zdt: ZonedDateTime = dt.atZone(zone)
                    val zoneOffset = zdt.offset
                    timezone = zoneOffset.toString()
                }
            }
        }

        if (lat != null && lng != null) {
            coordinateMap["lat"] = lat
            coordinateMap["lng"] = lng
        }
        if (place != null) {
            coordinateMap["place"] = place
        }
        if (timezone != null) {
            coordinateMap["timezone"] = timezone
        }

        return coordinateMap
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

                val keywordPhotoObj = KeywordPhoto()
                keywordPhotoObj.setKeywordId(keywordObj.getId())
                keywordPhotoObj.setMetadataId(metadataId)
                keywordPhotoObj.setCreatedAt(getCurrentTimestamp())
                keywordPhotoObj.setModifiedAt(getCurrentTimestamp())
                keywordPhotoRepository.save(keywordPhotoObj)
            }
        }
    }

    private fun processPeople(settings: Settings, metadataObj: Metadata?, taggedPeople: String?, isObject: Boolean) {
        if (metadataObj != null) {
            val metadataId = metadataObj.getId()

            val recognitionLabelPhotos = recognitionLabelPhotoRepository?.findByMetadataId(metadataId)
            if (recognitionLabelPhotos != null) {
                for (recognitionLabelPhoto in recognitionLabelPhotos) {
                    recognitionLabelPhotoRepository?.delete(recognitionLabelPhoto)
                }
            }

            if (taggedPeople != null && taggedPeople.trim() != "") {
                val recognitionLabelArray = StringEscapeUtils.escapeHtml4(taggedPeople).split(",")
                if (recognitionLabelArray.count() > 0) {
                    recognitionLabelPhotoRepository?.deleteByMetadataId(metadataId)
                }

                val compreFaceImageIdMap = mutableMapOf<String, Any?>()

                for (recognitionLabel in recognitionLabelArray) {
                    if (recognitionLabel.trim().isNotBlank()) {
                        val uploadResp = mapper.writeValueAsString(FileUtils.buildPersonUpload(settings, recognitionLabel, metadataObj, compreFaceImageIdMap))
                        val jsonRespObj = mapper.readTree(uploadResp)

                        var compreFaceImageId: String? = null
                        if (jsonRespObj.has("responseDataUpload") && jsonRespObj["responseDataUpload"].has("image_id")) {
                            compreFaceImageId = jsonRespObj["responseDataUpload"]["image_id"].toString()
                            compreFaceImageId = compreFaceImageId.drop(1).dropLast(1)
                        } else if (jsonRespObj.has("responseData") && jsonRespObj["responseData"].has("image_id")) {
                            compreFaceImageId = jsonRespObj["responseData"]["image_id"].toString()
                            compreFaceImageId = compreFaceImageId.drop(1).dropLast(1)
                        }

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
                                metadataId
                            )
                        if (recognitionLabelPhotoCount == 0) {
                            val recognitionLabelPhotoObj = RecognitionLabelPhoto()
                            recognitionLabelPhotoObj.setMetadataId(metadataObj.getId())
                            recognitionLabelPhotoObj.setRecognitionLabelId(recognitionLabelObj.getId())
                            recognitionLabelPhotoObj.setConfidence("0.0")
                            recognitionLabelPhotoObj.setCompreFaceImageId(compreFaceImageId)
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
            }
        }
    }
}