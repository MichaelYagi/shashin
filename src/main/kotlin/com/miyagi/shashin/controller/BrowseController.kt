package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.*
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.ApiResponse
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.TextUtils
import io.swagger.v3.oas.annotations.Operation
import org.springdoc.core.annotations.RouterOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*
import jakarta.servlet.http.HttpServletRequest
import org.apache.commons.text.StringEscapeUtils
import org.hibernate.query.Page
import org.springframework.http.MediaType
import org.springframework.web.multipart.MultipartFile
import kotlin.collections.HashMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.collections.mutableListOf
import kotlin.math.ceil

@Controller
class BrowseController: BaseController() {

    @Autowired
    private lateinit var metadataRepository: MetadataRepository

    @Autowired
    private lateinit var albumRepository: AlbumRepository

    @Autowired
    private lateinit var albumPhotoRepository: AlbumPhotoRepository

    @Autowired
    private lateinit var favoriteRepository: FavoriteRepository

    @Autowired
    private lateinit var keywordRepository: KeywordRepository

    @Autowired
    private var recognitionLabelRepository: RecognitionLabelRepository? = null

    @Autowired
    private var recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository? = null

    @Autowired
    private var folderDataRepository: FolderDataRepository? = null

    @Autowired
    private lateinit var settingsController: SettingsController

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()

    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/metadata/media/upload/batch","/api/v1/metadata/media/upload/batch"], method = [RequestMethod.POST], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE], produces = ["application/json"])
    @ResponseBody
    fun postUploadToTimeline(model: Model, @RequestParam("files[]") media: List<MultipartFile>): String {
        resp["msg"] = "Could not save"
        resp["status"] = ApiResponse.FAIL.status

        val hasMediaUploadDirectory = model.getAttribute("hasMediaUploadDirectory") as Boolean?

        val settings = model.getAttribute("settings") as Settings?

        val currentUserObj = model.getAttribute("currentUser") as User?

        if (currentUserObj != null && !media.isEmpty() && hasMediaUploadDirectory != null && hasMediaUploadDirectory && !settings?.getUploadMediaDirectory().isNullOrBlank()) {
            val fileUploadedMap = FileUtils.copyMultipartFiles(media, settings)
            val uploadedFiles = fileUploadedMap["uploadedFiles"] as MutableList<String>
            val notUploadedFiles = fileUploadedMap["notUploadedFiles"] as MutableList<String>

            if (!uploadedFiles.isEmpty()) {
                settingsController.scanMediaDirectories(false, 0, currentUserObj.getId())
            }

            if (!notUploadedFiles.isEmpty() && !uploadedFiles.isEmpty()) {
                resp["msg"] = "Files uploaded: <br>${uploadedFiles.joinToString("<br>")}.<br><br>Some items not uploaded. Check file formats: <br>${notUploadedFiles.joinToString("<br>")}"
                resp["status"] = ApiResponse.FAIL.status
            } else if (!notUploadedFiles.isEmpty() && uploadedFiles.isEmpty()) {
                resp["msg"] = "Items not uploaded. Check file formats: <br>${notUploadedFiles.joinToString("<br>")}"
                resp["status"] = ApiResponse.FAIL.status
            } else if (!uploadedFiles.isEmpty()) {
                resp["msg"] = "Files uploaded. Processing files"
                resp["status"] = ApiResponse.SUCCESS.status
            } else {
                resp["msg"] = "Files uploaded. Processing files"
                resp["status"] = ApiResponse.SUCCESS.status
            }
        }

        return mapper.writeValueAsString(resp)
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/recent","/recent/{mediaType}"], method = [RequestMethod.GET])
    fun getRecentlyAdded(model: Model,@PathVariable(required = false) mediaType: String?): String {
        val module = "recent"
        buildInitialPage(module,model,mediaType)

        model["pageParam"] = 0
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/recent/{page}/{mediaType}"], method = [RequestMethod.GET])
    fun getRecentlyAddedPage(model: Model,@PathVariable(required = true) page: Int,@PathVariable(required = true) mediaType: String): String {
        val module = "recent"

        val response = buildBrowseRecord(module,model,page,model.getAttribute("queryLimit").toString().toInt(),mediaType)
        for ((k, v) in response) {
            model[k] = v!!
        }

        model["currentPage"] = (page+1)
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getPagedModified",
            summary = "Get paged results for recently added content.",
            description = "<strong>Get paged results for recently added content.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/recent/{page}\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>folder</td><td>param</td><td>string</td><td>required</td><td>URL encoded name of the folder path</td></tr>" +
                    "<tr><td>page</td><td>param</td><td>int</td><td>required</td><td>page number of results to return used for pagination. Page index starts from 0. The default query/page size is 20. Admins can set the query/page size in the <a href=\"/settings\">settings</a></td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"msg\": \"\",\n" +
                    "    \"message\": \"\",\n" +
                    "    \"status\": \"success\",\n" +
                    "    \"mediaTypeFilter\": \"&lt;media_type&gt;\",\n" +
                    "    \"folder\": \"&lt;folder_name&gt;\",\n" +
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
                    "    },\n" +
                    "    \"albumsList\": [\n" +
                    "        {\n" +
                    "            \"coverUrl\": \"&lt;relative_cover_url&gt;\",\n" +
                    "            \"albumVideoCount\": &lt;album_video_count&gt;,\n" +
                    "            \"albumPhotoCount\": &lt;album_photo_count&gt;,\n" +
                    "            \"id\": &lt;album_id&gt;,\n" +
                    "            \"shareUrl\": \"&lt;relative_share_url&gt;\",\n" +
                    "            \"name\": \"&lt;album_name&gt;\"\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"recognitionLabels\": [\n" +
                    "        {\n" +
                    "            \"id\": &lt;album_id&gt;,\n" +
                    "            \"name\": \"&lt;subject_name&gt;\"\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"labelPhotoMap\": {\n" +
                    "        \"&lt;metadata_id&gt;\": \"&lt;subject_name&gt;\"\n" +
                    "    },\n" +
                    "    \"albumMap\": {\n" +
                    "        \"&lt;metadata_id&gt;\": \"&lt;album_name&gt;\"\n" +
                    "    },\n" +
                    "    \"keywordMap\": {\n" +
                    "        \"&lt;metadata_id&gt;\": \"&lt;keywords&gt;\"\n" +
                    "    }\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>mediaTypeFilter</td><td>string</td><td>One of \"all\", \"video\" or \"image\"</td></tr>" +
                    "<tr><td>folder</td><td>string</td><td>full path and name of the folder</td></tr>" +
                    "<tr><td>metadataList[].metadata</td><td>object</td><td>A <a href=\"#\" data-bs-toggle=\"modal\" data-bs-target=\"#propMetadataDocs\">Metadata</a> object</td></tr>" +
                    "<tr><td>favourites.&lt;metadata_id&gt;.count</td><td>int</td><td>The number of people who saved this media as a favorite</td></tr>" +
                    "<tr><td>favourites.&lt;metadata_id&gt;.favorite</td><td>boolean</td><td>True if saved as a favorite</td></tr>" +
                    "<tr><td>albumList[].coverUrl</td><td>string</td><td>Relative URL for the album cover image</td></tr>" +
                    "<tr><td>albumList[].albumVideoCount</td><td>string</td><td>The number of videos in this album</td></tr>" +
                    "<tr><td>albumList[].albumPhotoCount</td><td>string</td><td>The number of photos in this album</td></tr>" +
                    "<tr><td>albumList[].id</td><td>int</td><td>The album ID</td></tr>" +
                    "<tr><td>albumList[].shareUrl</td><td>string</td><td>Part of the share URL endpoint for public sharing</td></tr>" +
                    "<tr><td>albumList[].name</td><td>string</td><td>The album name</td></tr>" +
                    "<tr><td>recognitionLabels[].id</td><td>int</td><td>Album ID the subject is associated with</td></tr>" +
                    "<tr><td>recognitionLabels[].name</td><td>string</td><td>Subject name</td></tr>" +
                    "<tr><td>labelPhotoMap.&lt;metadata_id&gt;.&lt;subject_name&gt;</td><td>string</td><td>names associated with media</td></tr>" +
                    "<tr><td>albumMap.&lt;metadata_id&gt;.&lt;album_name&gt;</td><td>string</td><td>album name associated with media</td></tr>" +
                    "<tr><td>keywordMap.&lt;metadata_id&gt;.&lt;keywords&gt;</td><td>string</td><td>keywords associated with media</td></tr>" +
                    "</tbody></table>"
        )
    )
    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/recent/mediatype/{mediaType}/page/{page}","/api/v1/recent/{page}","/api/v1/recent/mediatype/{mediaType}/page/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedRecent(model: Model, request: HttpServletRequest, @PathVariable page: Int,@PathVariable(required = false) mediaType: String?): String {
        return mapper.writeValueAsString(buildBrowseRecord("recent",model,page, model.getAttribute("queryLimit").toString().toInt(), mediaType))
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/taken","/taken/{mediaType}"], method = [RequestMethod.GET])
    fun getTaken(model: Model,@PathVariable(required = false) mediaType: String?): String {
        val module = "taken"

        buildInitialPage(module,model,mediaType)

        model["pageParam"] = 0
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/taken/{page}","/taken/mediatype/{mediaType}/page/{page}","/api/v1/taken/{page}", "/api/v1/taken/mediatype/{mediaType}/page/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedTaken(model: Model, request: HttpServletRequest, @PathVariable page: Int, @PathVariable(required = false) mediaType: String?): String {
        return mapper.writeValueAsString(buildBrowseRecord("taken",model,page,model.getAttribute("queryLimit").toString().toInt(),mediaType))
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/taken/{page}/{mediaType}"], method = [RequestMethod.GET])
    fun getTakenAddedPage(model: Model,@PathVariable(required = true) page: Int,@PathVariable(required = true) mediaType: String): String {
        val module = "taken"

        val response = buildBrowseRecord(module,model,page,model.getAttribute("queryLimit").toString().toInt(),mediaType)
        for ((k, v) in response) {
            model[k] = v!!
        }

        model["currentPage"] = (page+1)
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/modified","/modified/{mediaType}"], method = [RequestMethod.GET])
    fun getModified(model: Model,@PathVariable(required = false) mediaType: String?): String {
        val module = "modified"

        buildInitialPage(module,model,mediaType)

        model["pageParam"] = 0
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/modified/{page}/{mediaType}"], method = [RequestMethod.GET])
    fun getModifiedAddedPage(model: Model,@PathVariable(required = true) page: Int,@PathVariable(required = true) mediaType: String): String {
        val module = "modified"

        val response = buildBrowseRecord(module,model,page,model.getAttribute("queryLimit").toString().toInt(),mediaType)
        for ((k, v) in response) {
            model[k] = v!!
        }

        model["currentPage"] = (page+1)
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getPagedModified",
            summary = "Get paged results for recently modified content.",
            description = "<strong>Get paged results for recently modified content.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/modified/{page}\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>folder</td><td>param</td><td>string</td><td>required</td><td>URL encoded name of the folder path</td></tr>" +
                    "<tr><td>page</td><td>param</td><td>int</td><td>required</td><td>page number of results to return used for pagination. Page index starts from 0. The default query/page size is 20. Admins can set the query/page size in the <a href=\"/settings\">settings</a></td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"msg\": \"\",\n" +
                    "    \"message\": \"\",\n" +
                    "    \"status\": \"success\",\n" +
                    "    \"mediaTypeFilter\": \"&lt;media_type&gt;\",\n" +
                    "    \"folder\": \"&lt;folder_name&gt;\",\n" +
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
                    "    },\n" +
                    "    \"albumsList\": [\n" +
                    "        {\n" +
                    "            \"coverUrl\": \"&lt;relative_cover_url&gt;\",\n" +
                    "            \"albumVideoCount\": &lt;album_video_count&gt;,\n" +
                    "            \"albumPhotoCount\": &lt;album_photo_count&gt;,\n" +
                    "            \"id\": &lt;album_id&gt;,\n" +
                    "            \"shareUrl\": \"&lt;relative_share_url&gt;\",\n" +
                    "            \"name\": \"&lt;album_name&gt;\"\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"recognitionLabels\": [\n" +
                    "        {\n" +
                    "            \"id\": &lt;album_id&gt;,\n" +
                    "            \"name\": \"&lt;subject_name&gt;\"\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"labelPhotoMap\": {\n" +
                    "        \"&lt;metadata_id&gt;\": \"&lt;subject_name&gt;\"\n" +
                    "    },\n" +
                    "    \"albumMap\": {\n" +
                    "        \"&lt;metadata_id&gt;\": \"&lt;album_name&gt;\"\n" +
                    "    },\n" +
                    "    \"keywordMap\": {\n" +
                    "        \"&lt;metadata_id&gt;\": \"&lt;keywords&gt;\"\n" +
                    "    }\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>mediaTypeFilter</td><td>string</td><td>One of \"all\", \"video\" or \"image\"</td></tr>" +
                    "<tr><td>folder</td><td>string</td><td>full path and name of the folder</td></tr>" +
                    "<tr><td>metadataList[].metadata</td><td>object</td><td>A <a href=\"#\" data-bs-toggle=\"modal\" data-bs-target=\"#propMetadataDocs\">Metadata</a> object</td></tr>" +
                    "<tr><td>favourites.&lt;metadata_id&gt;.count</td><td>int</td><td>The number of people who saved this media as a favorite</td></tr>" +
                    "<tr><td>favourites.&lt;metadata_id&gt;.favorite</td><td>boolean</td><td>True if saved as a favorite</td></tr>" +
                    "<tr><td>albumList[].coverUrl</td><td>string</td><td>Relative URL for the album cover image</td></tr>" +
                    "<tr><td>albumList[].albumVideoCount</td><td>string</td><td>The number of videos in this album</td></tr>" +
                    "<tr><td>albumList[].albumPhotoCount</td><td>string</td><td>The number of photos in this album</td></tr>" +
                    "<tr><td>albumList[].id</td><td>int</td><td>The album ID</td></tr>" +
                    "<tr><td>albumList[].shareUrl</td><td>string</td><td>Part of the share URL endpoint for public sharing</td></tr>" +
                    "<tr><td>albumList[].name</td><td>string</td><td>The album name</td></tr>" +
                    "<tr><td>recognitionLabels[].id</td><td>int</td><td>Album ID the subject is associated with</td></tr>" +
                    "<tr><td>recognitionLabels[].name</td><td>string</td><td>Subject name</td></tr>" +
                    "<tr><td>labelPhotoMap.&lt;metadata_id&gt;.&lt;subject_name&gt;</td><td>string</td><td>names associated with media</td></tr>" +
                    "<tr><td>albumMap.&lt;metadata_id&gt;.&lt;album_name&gt;</td><td>string</td><td>album name associated with media</td></tr>" +
                    "<tr><td>keywordMap.&lt;metadata_id&gt;.&lt;keywords&gt;</td><td>string</td><td>keywords associated with media</td></tr>" +
                    "</tbody></table>"
        )
    )
    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/modified/{page}","/modified/mediatype/{mediaType}/page/{page}","/api/v1/modified/{page}","/api/v1/modified/mediatype/{mediaType}/page/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedModified(model: Model, request: HttpServletRequest, @PathVariable page: Int,@PathVariable(required = false) mediaType: String?): String {
        return mapper.writeValueAsString(buildBrowseRecord("modified",model,page,model.getAttribute("queryLimit").toString().toInt(),mediaType))
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/accessed","/accessed/{mediaType}"], method = [RequestMethod.GET])
    fun getAccessed(model: Model,@PathVariable(required = false) mediaType: String?): String {
        val module = "accessed"

        buildInitialPage(module,model,mediaType)

        model["pageParam"] = 0
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/accessed/{page}/{mediaType}"], method = [RequestMethod.GET])
    fun getAccessedAddedPage(model: Model,@PathVariable(required = true) page: Int,@PathVariable(required = true) mediaType: String): String {
        val module = "accessed"

        val response = buildBrowseRecord(module,model,page,model.getAttribute("queryLimit").toString().toInt(),mediaType)
        for ((k, v) in response) {
            model[k] = v!!
        }

        model["currentPage"] = (page+1)
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/accessed/{page}","/accessed/mediatype/{mediaType}/page/{page}","/api/v1/accessed/{page}","/api/v1/accessed/mediatype/{mediaType}/page/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedAccessed(model: Model, request: HttpServletRequest, @PathVariable page: Int,@PathVariable(required = false) mediaType: String?): String {
        return mapper.writeValueAsString(buildBrowseRecord("accessed",model,page,model.getAttribute("queryLimit").toString().toInt(),mediaType))
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getPagedModified",
            summary = "Get paged results for recently modified content.",
            description = "<strong>Get paged results for recently modified content.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/modified?page={page}&size={size}\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>folder</td><td>param</td><td>string</td><td>required</td><td>URL encoded name of the folder path</td></tr>" +
                    "<tr><td>page</td><td>param</td><td>int</td><td>optional</td><td>page number of results to return used for pagination. Default is 0.</td></tr>" +
                    "<tr><td>size</td><td>param</td><td>int</td><td>optional</td><td>The default query/page size is 20. Admins can set the default query/page size in the <a href=\"/settings\">settings</a></td></tr>" +                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"msg\": \"\",\n" +
                    "    \"message\": \"\",\n" +
                    "    \"status\": \"success\",\n" +
                    "    \"mediaTypeFilter\": \"&lt;media_type&gt;\",\n" +
                    "    \"folder\": \"&lt;folder_name&gt;\",\n" +
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
                    "    },\n" +
                    "    \"albumsList\": [\n" +
                    "        {\n" +
                    "            \"coverUrl\": \"&lt;relative_cover_url&gt;\",\n" +
                    "            \"albumVideoCount\": &lt;album_video_count&gt;,\n" +
                    "            \"albumPhotoCount\": &lt;album_photo_count&gt;,\n" +
                    "            \"id\": &lt;album_id&gt;,\n" +
                    "            \"shareUrl\": \"&lt;relative_share_url&gt;\",\n" +
                    "            \"name\": \"&lt;album_name&gt;\"\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"recognitionLabels\": [\n" +
                    "        {\n" +
                    "            \"id\": &lt;album_id&gt;,\n" +
                    "            \"name\": \"&lt;subject_name&gt;\"\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"labelPhotoMap\": {\n" +
                    "        \"&lt;metadata_id&gt;\": \"&lt;subject_name&gt;\"\n" +
                    "    },\n" +
                    "    \"albumMap\": {\n" +
                    "        \"&lt;metadata_id&gt;\": \"&lt;album_name&gt;\"\n" +
                    "    },\n" +
                    "    \"keywordMap\": {\n" +
                    "        \"&lt;metadata_id&gt;\": \"&lt;keywords&gt;\"\n" +
                    "    }\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>mediaTypeFilter</td><td>string</td><td>One of \"all\", \"video\" or \"image\"</td></tr>" +
                    "<tr><td>folder</td><td>string</td><td>full path and name of the folder</td></tr>" +
                    "<tr><td>metadataList[].metadata</td><td>object</td><td>A <a href=\"#\" data-bs-toggle=\"modal\" data-bs-target=\"#propMetadataDocs\">Metadata</a> object</td></tr>" +
                    "<tr><td>favourites.&lt;metadata_id&gt;.count</td><td>int</td><td>The number of people who saved this media as a favorite</td></tr>" +
                    "<tr><td>favourites.&lt;metadata_id&gt;.favorite</td><td>boolean</td><td>True if saved as a favorite</td></tr>" +
                    "<tr><td>albumList[].coverUrl</td><td>string</td><td>Relative URL for the album cover image</td></tr>" +
                    "<tr><td>albumList[].albumVideoCount</td><td>string</td><td>The number of videos in this album</td></tr>" +
                    "<tr><td>albumList[].albumPhotoCount</td><td>string</td><td>The number of photos in this album</td></tr>" +
                    "<tr><td>albumList[].id</td><td>int</td><td>The album ID</td></tr>" +
                    "<tr><td>albumList[].shareUrl</td><td>string</td><td>Part of the share URL endpoint for public sharing</td></tr>" +
                    "<tr><td>albumList[].name</td><td>string</td><td>The album name</td></tr>" +
                    "<tr><td>recognitionLabels[].id</td><td>int</td><td>Album ID the subject is associated with</td></tr>" +
                    "<tr><td>recognitionLabels[].name</td><td>string</td><td>Subject name</td></tr>" +
                    "<tr><td>labelPhotoMap.&lt;metadata_id&gt;.&lt;subject_name&gt;</td><td>string</td><td>names associated with media</td></tr>" +
                    "<tr><td>albumMap.&lt;metadata_id&gt;.&lt;album_name&gt;</td><td>string</td><td>album name associated with media</td></tr>" +
                    "<tr><td>keywordMap.&lt;metadata_id&gt;.&lt;keywords&gt;</td><td>string</td><td>keywords associated with media</td></tr>" +
                    "</tbody></table>"
        )
    )
    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/api/v1/modified","/api/v1/modified/{mediaType}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedSizeModified(model: Model, request: HttpServletRequest, @RequestParam page: Optional<Int>, @RequestParam size: Optional<Int>, @PathVariable(required = false) mediaType: String?): String {
        return mapper.writeValueAsString(buildBrowseRecord("modified", model ,page.orElse(0), size.orElse(model.getAttribute("queryLimit").toString().toInt()), mediaType))
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/metadata/list/{module}/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getMetadataList(model: Model,@PathVariable module: String,@PathVariable page: Int, @RequestParam folder: Optional<String>): String {
        val response = mutableMapOf<String, Any?>()
        var metadataList: MutableIterable<Metadata>? = null
        response["metadataList"] = mutableListOf<Metadata>()
        response["msg"] = "Results"
        response["status"] = ApiResponse.SUCCESS.status
        val size: Int = model.getAttribute("queryLimit") as Int
        val pageValue = page*size

        if (module == "recent") {
            metadataList = metadataRepository.findRecentByOffsetAndLimit(
                pageValue,
                size
            ).toMutableList()
        } else if (module == "modified") {
            metadataList = metadataRepository.findModifiedByOffsetAndLimit(
                pageValue,
                size
            ).toMutableList()
        } else if (module == "folder") {
            val decodedValue = URLDecoder.decode(folder.orElse(""), StandardCharsets.UTF_8.toString())
            if (decodedValue == "") {
                metadataList = metadataRepository.findAllByFolderOffsetAndLimit(
                    decodedValue,
                    pageValue,
                    size
                ).toMutableList()
            }
        }

        if (metadataList != null) {
            response["metadataList"] = metadataList
        }

        return mapper.writeValueAsString(response)
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/browse/album/list"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getBrowseAlbumList(model: Model): String? {
        val response = mutableMapOf<String, Any?>()
        response["msg"] = "No Results"
        response["status"] = ApiResponse.FAIL.status
        response["albumList"] = mutableListOf<Album>()

        try {
            val albumList = albumRepository.findAllOrderByAlbumName()
            if (albumList != null && albumList.count() > 0) {
                response["albumList"] = albumList
                response["msg"] = "Results"
                response["status"] = ApiResponse.SUCCESS.status
            }
        } catch(_: Exception) {}

        return mapper.writeValueAsString(response)
    }

    private fun buildBrowseRecord(module: String, model: Model, page: Int = 0, size: Int = model.getAttribute("queryLimit").toString().toInt(), mediaTypeFilter: String?): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()
        response["message"] = "Nothing to see here."
        response["metadataList"] = mutableListOf<Metadata>()
        response["favorites"] = mutableMapOf<String, Any>()
        response["albumList"] = mutableListOf<Album>()
        response["recognitionLabels"] = mutableListOf<RecognitionLabel>()
        response["labelPhotoMap"] = mutableMapOf<String, Any>()
        response["mediaTypeFilter"] = "all"
        response["albumMap"] = mutableMapOf<String, Any>()
        response["keywordMap"] = mutableMapOf<String, Any>()
        response["placenameMap"] = mutableMapOf<String, MutableList<String>?>()
        response["page"] = page
        response["size"] = size
        response["totalPages"] = 0

        response["msg"] = "Could not get results"
        response["status"] = ApiResponse.FAIL.status

        var mediaType = mediaTypeFilter

        if (mediaTypeFilter.isNullOrEmpty()) {
            mediaType = "all"
        }

        response["mediaTypeFilter"] = mediaType

        if (model.getAttribute("currentUser") != "") {
            val currentUserObj = model.getAttribute("currentUser") as User?
            val pageValue = page*size

            val favoritesMap = HashMap<String, HashMap<String, Any>>()

            var metadataList = mutableListOf<Metadata>()
            if (mediaType == "all") {
                response["totalPages"] = ceil((metadataRepository.countAllByHiddenIsFalse().toDouble()) / size.toDouble()).toInt()

                when (module) {
                    "recent" -> {
                        metadataList = metadataRepository.findRecentByOffsetAndLimit(
                            pageValue,
                            size
                        ).toMutableList()
                    }
                    "modified" -> {
                        metadataList = metadataRepository.findModifiedByOffsetAndLimit(
                            pageValue,
                            size
                        ).toMutableList()
                    }
                    "taken" -> {
                        metadataList = metadataRepository.findTakenByOffsetAndLimit(
                            pageValue,
                            size
                        ).toMutableList()
                    }
                    "accessed" -> {
                        metadataList = metadataRepository.findLastAccessedByOffsetAndLimit(
                            pageValue,
                            size
                        ).toMutableList()
                    }
                }
            } else if (mediaType == "nolatlng") {
                response["totalPages"] = ceil((metadataRepository.countByNoCoordAndOffsetAndLimit().toDouble()) / size.toDouble()).toInt()

                when (module) {
                    "recent" -> {
                        metadataList = metadataRepository.findRecentByNoCoordAndOffsetAndLimit(
                            pageValue,
                            size
                        ).toMutableList()
                    }
                    "modified" -> {
                        metadataList = metadataRepository.findModifiedByNoCoordAndOffsetAndLimit(
                            pageValue,
                            size
                        ).toMutableList()
                    }
                    "taken" -> {
                        metadataList = metadataRepository.findTakenByNoCoordAndOffsetAndLimit(
                            pageValue,
                            size
                        ).toMutableList()
                    }
                    "accessed" -> {
                        metadataList = metadataRepository.findLastAccessedByNoCoordAndOffsetAndLimit(
                            pageValue,
                            size
                        ).toMutableList()
                    }
                }
            } else {
                response["totalPages"] = ceil((metadataRepository.countByMediaTypeAndOffsetAndLimit(mediaType).toDouble()) / size.toDouble()).toInt()

                when (module) {
                    "recent" -> {
                        metadataList = metadataRepository.findRecentByMediaTypeAndOffsetAndLimit(
                            pageValue,
                            mediaType,
                            size
                        ).toMutableList()
                    }
                    "modified" -> {
                        metadataList = metadataRepository.findModifiedByMediaTypeAndOffsetAndLimit(
                            pageValue,
                            mediaType,
                            size
                        ).toMutableList()
                    }
                    "taken" -> {
                        metadataList = metadataRepository.findTakenByMediaTypeAndOffsetAndLimit(
                            pageValue,
                            mediaType,
                            size
                        ).toMutableList()
                    }
                    "accessed" -> {
                        metadataList = metadataRepository.findLastAccessedByMediaTypeAndOffsetAndLimit(
                            pageValue,
                            mediaType,
                            size
                        ).toMutableList()
                    }
                }
            }

            if (metadataList.isNotEmpty()) {
                response["metadataList"] = metadataList
                response["message"] = ""
                response["favorites"] = favoritesMap

                val recognitionLabels =
                    recognitionLabelRepository?.findAllByNameNotContaining(TextUtils.getObjectName())
                if (recognitionLabels != null && recognitionLabels.count() > 0) {
                    response["recognitionLabels"] = recognitionLabels
                }

                val labelPhotoMap = mutableMapOf<String, String>()
                val albumMap = mutableMapOf<String, String>()
                val keywordMap = mutableMapOf<String, String>()
                val placenameMap = mutableMapOf<String, MutableList<String>?>()

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
                    if (recognitionLabelPhotos != null && recognitionLabelPhotos.count() > 0) {
                        for (recognitionLabelPhoto in recognitionLabelPhotos) {
                            if (recognitionLabelPhoto.getRecognitionLabelId() != null) {
                                val recognitionLabelObj =
                                    recognitionLabelRepository?.findById(recognitionLabelPhoto.getRecognitionLabelId()!!)
                                if (recognitionLabelObj != null) {
                                    labelString += recognitionLabelObj.get().getName() + ","
                                }
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

                    val placenameKey = metadata.getYear().toString()+"-"+metadata.getMonth().toString()+"-"+metadata.getDay().toString()
                    if (!placenameMap.containsKey(placenameKey)) {
                        placenameMap[placenameKey] = TextUtils.getPlaceNamesForDateHeader(metadata.getYear()!!, metadata.getMonth()!!, metadata.getDay()!!, metadataRepository)
                    }
                }
                response["placenameMap"] = placenameMap
                response["labelPhotoMap"] = labelPhotoMap
                response["albumMap"] = albumMap
                response["keywordMap"] = keywordMap

                val albumList = albumRepository.findAllOrderByAlbumName()
                if (albumList != null && albumList.count() > 0) {
                    response["albumList"] = albumList
                }

                response["favorites"] = favoritesMap
            }

            response["msg"] = "Results"
            response["status"] = ApiResponse.SUCCESS.status
        }

        return response
    }

    private fun buildInitialFoldersPage(model: Model): Model {
        val page = 0
        val response = buildPagedFolders(model,page)

        for ((k, v) in response) {
            model[k] = v!!
        }

        getAllAttributeData(model)

        return model
    }

    private fun buildInitialPage(module: String, model: Model, mediaTypeFilter: String?): Model {
        val response = buildBrowseRecord(module,model,0,model.getAttribute("queryLimit").toString().toInt(),mediaTypeFilter)

        for ((k, v) in response) {
            model[k] = v!!
        }

        getAllAttributeData(model)

        model["foldersCount"] = metadataRepository.countByFolder()

        return model
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @GetMapping("/folders")
    fun getFolders(model: Model): String {
        val module = "folders"
        buildInitialFoldersPage(model)

        model["pageParam"] = 0
        model["foldersCount"] = metadataRepository.countByFolder()
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    private fun buildPagedFolders(model: Model, page: Int = 0, size: Int = model.getAttribute("queryLimit").toString().toInt()): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        response["status"] = ApiResponse.FAIL.status

        val module = "folders"
        response["msg"] = "Nothing to see here."
        response["message"] = "Nothing to see here."
        response["foldersList"] = mutableListOf<Folder>()
        response["page"] = page
        response["size"] = size
        response["totalPages"] = 0

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            val pageValue = page*size

            val folderObj = metadataRepository.findFoldersOffsetAndLimit(pageValue, size)

            if (folderObj != null && folderObj.count() > 0) {
                val folderCount = metadataRepository.countTotalFolders()
                response["totalPages"] = ceil((folderCount.toDouble()) / size.toDouble()).toInt()
                response["foldersList"] = folderObj
                response["status"] = ApiResponse.SUCCESS.status
                response["message"] = ""
            }
        }

        response["msg"] = ""
        response["activePage"] = module
        response["activeSidebar"] = module
        response["titleDescriptor"] = TextUtils.capitalized(module)

        return response
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getPagedFolders",
            summary = "Get paged list of all folders.",
            description = "<strong>Get paged list of all folders.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/folders/{page}\" \\\n" +
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
                    "    \"foldersList\": [\n" +
                    "        {\n" +
                    "            \"folder\": \"&lt;folder_name&gt;\",\n" +
                    "            \"thumbnailUrlCentered\": &lt;cover_relative_url&gt;,\n" +
                    "            \"count\": &lt;media_count&gt;\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>foldersList[].folder</td><td>string</td><td>The folder name</td></tr>" +
                    "<tr><td>foldersList[].thumbnailUrlCentered</td><td>string</td><td>The relative cover image URL of the folder</td></tr>" +
                    "<tr><td>foldersList[].count</td><td>int</td><td>The number of media in the folder</td></tr>" +
                    "</tbody></table>"
        )
    )
    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/folders/page/{page}","/api/v1/folders/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedFolders(model: Model, request: HttpServletRequest, @PathVariable page: Int): String {
        return mapper.writeValueAsString(buildPagedFolders(model,page))
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/folders/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    fun getPaginationAnonymousShareAlbum(model: Model, @PathVariable page: Int): String? {
        val response = buildPagedFolders(model, page)

        for ((k, v) in response) {
            model[k] = v!!
        }

        val module = "folders"

        model["currentPage"] = (page+1)
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getPagedSizeFolders",
            summary = "Get paged list of all folders with page/size query params.",
            description = "<strong>Get list of all folders.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/folders?page={page}&size={size}\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>page</td><td>param</td><td>int</td><td>optional</td><td>page number of results to return used for pagination. Default is 0.</td></tr>" +
                    "<tr><td>size</td><td>param</td><td>int</td><td>optional</td><td>The default query/page size is 20. Admins can set the default query/page size in the <a href=\"/settings\">settings</a></td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"msg\": \"\",\n" +
                    "    \"status\": \"success\",\n" +
                    "    \"foldersList\": [\n" +
                    "        {\n" +
                    "            \"folder\": \"&lt;folder_name&gt;\",\n" +
                    "            \"thumbnailUrlCentered\": &lt;cover_relative_url&gt;,\n" +
                    "            \"count\": &lt;media_count&gt;\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>foldersList[].folder</td><td>string</td><td>The folder name</td></tr>" +
                    "<tr><td>foldersList[].thumbnailUrlCentered</td><td>string</td><td>The relative cover image URL of the folder</td></tr>" +
                    "<tr><td>foldersList[].count</td><td>int</td><td>The number of media in the folder</td></tr>" +
                    "</tbody></table>"
        )
    )
    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/api/v1/folders"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedSizeFolders(model: Model, request: HttpServletRequest, @RequestParam page: Optional<Int>, @RequestParam size: Optional<Int>): String {
        return mapper.writeValueAsString(buildPagedFolders(model, page.orElse(0), size.orElse(model.getAttribute("queryLimit").toString().toInt())))
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/folder/{folder}"], method = [RequestMethod.GET])
    fun getFolder(model: Model, @PathVariable folder: String): String {
        val module = "folder"
        val page = 0
        val decodedValue = URLDecoder.decode(folder, StandardCharsets.UTF_8.toString())
        val response = buildFolder(model,decodedValue,page)

        for ((k, v) in response) {
            model[k] = v!!
        }

        getAllAttributeData(model)

        model["pageParam"] = 0
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = decodedValue

        return module
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getPagedFolder",
            summary = "Get paged results for folder content.",
            description = "<strong>Get paged results for folder content.</strong> Pages start from 0. The page size can be configured through the web interface (default 20).<br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/folder/{page}/{folder}\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>folder</td><td>param</td><td>string</td><td>required</td><td>URL encoded name of the folder path</td></tr>" +
                    "<tr><td>page</td><td>param</td><td>int</td><td>required</td><td>page number of results to return used for pagination. Page index starts from 0. The default query/page size is 20. Admins can set the query/page size in the <a href=\"/settings\">settings</a></td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"msg\": \"\",\n" +
                    "    \"message\": \"\",\n" +
                    "    \"status\": \"success\",\n" +
                    "    \"mediaTypeFilter\": \"&lt;media_type&gt;\",\n" +
                    "    \"folder\": \"&lt;folder_name&gt;\",\n" +
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
                    "    },\n" +
                    "    \"albumsList\": [\n" +
                    "        {\n" +
                    "            \"coverUrl\": \"&lt;relative_cover_url&gt;\",\n" +
                    "            \"albumVideoCount\": &lt;album_video_count&gt;,\n" +
                    "            \"albumPhotoCount\": &lt;album_photo_count&gt;,\n" +
                    "            \"id\": &lt;album_id&gt;,\n" +
                    "            \"shareUrl\": \"&lt;relative_share_url&gt;\",\n" +
                    "            \"name\": \"&lt;album_name&gt;\"\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"recognitionLabels\": [\n" +
                    "        {\n" +
                    "            \"id\": &lt;album_id&gt;,\n" +
                    "            \"name\": \"&lt;subject_name&gt;\"\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"labelPhotoMap\": {\n" +
                    "        \"&lt;metadata_id&gt;\": \"&lt;subject_name&gt;\"\n" +
                    "    },\n" +
                    "    \"albumMap\": {\n" +
                    "        \"&lt;metadata_id&gt;\": \"&lt;album_name&gt;\"\n" +
                    "    },\n" +
                    "    \"keywordMap\": {\n" +
                    "        \"&lt;metadata_id&gt;\": \"&lt;keywords&gt;\"\n" +
                    "    }\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>mediaTypeFilter</td><td>string</td><td>One of \"all\", \"video\" or \"image\"</td></tr>" +
                    "<tr><td>folder</td><td>string</td><td>full path and name of the folder</td></tr>" +
                    "<tr><td>metadataList[].metadata</td><td>object</td><td>A <a href=\"#\" data-bs-toggle=\"modal\" data-bs-target=\"#propMetadataDocs\">Metadata</a> object</td></tr>" +
                    "<tr><td>favourites.&lt;metadata_id&gt;.count</td><td>int</td><td>The number of people who saved this media as a favorite</td></tr>" +
                    "<tr><td>favourites.&lt;metadata_id&gt;.favorite</td><td>boolean</td><td>True if saved as a favorite</td></tr>" +
                    "<tr><td>albumList[].coverUrl</td><td>string</td><td>Relative URL for the album cover image</td></tr>" +
                    "<tr><td>albumList[].albumVideoCount</td><td>string</td><td>The number of videos in this album</td></tr>" +
                    "<tr><td>albumList[].albumPhotoCount</td><td>string</td><td>The number of photos in this album</td></tr>" +
                    "<tr><td>albumList[].id</td><td>int</td><td>The album ID</td></tr>" +
                    "<tr><td>albumList[].shareUrl</td><td>string</td><td>Part of the share URL endpoint for public sharing</td></tr>" +
                    "<tr><td>albumList[].name</td><td>string</td><td>The album name</td></tr>" +
                    "<tr><td>recognitionLabels[].id</td><td>int</td><td>Album ID the subject is associated with</td></tr>" +
                    "<tr><td>recognitionLabels[].name</td><td>string</td><td>Subject name</td></tr>" +
                    "<tr><td>labelPhotoMap.&lt;metadata_id&gt;.&lt;subject_name&gt;</td><td>string</td><td>names associated with media</td></tr>" +
                    "<tr><td>albumMap.&lt;metadata_id&gt;.&lt;album_name&gt;</td><td>string</td><td>album name associated with media</td></tr>" +
                    "<tr><td>keywordMap.&lt;metadata_id&gt;.&lt;keywords&gt;</td><td>string</td><td>keywords associated with media</td></tr>" +
                    "</tbody></table>"
        )
    )
    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/folder/page/{page}/{folder}","/api/v1/folder/{page}/{folder}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedFolder(model: Model, request: HttpServletRequest, @PathVariable page: Int, @PathVariable folder: String): String {
        return mapper.writeValueAsString(buildFolder(model,URLDecoder.decode(folder, StandardCharsets.UTF_8.toString()),page))
    }

    @RequestMapping(value = ["/folder/{folder}/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    fun getPaginationFolder(model: Model, @PathVariable folder: String,@PathVariable page: Int): String? {
        val response = buildFolder(model,URLDecoder.decode(folder, StandardCharsets.UTF_8.toString()),page)

        for ((k, v) in response) {
            model[k] = v!!
        }

        val module = "folder"

        model["currentPage"] = (page+1)
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getPagedFolder",
            summary = "Get paged results for folder content.",
            description = "<strong>Get paged results for folder content.</strong> Pages start from 0. The page size can be configured through the web interface (default 20).<br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/folder/{folder}?page={page}&size={size}\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>folder</td><td>param</td><td>string</td><td>required</td><td>URL encoded name of the folder path</td></tr>" +
                    "<tr><td>page</td><td>param</td><td>int</td><td>optional</td><td>page number of results to return used for pagination. Default is 0.</td></tr>" +
                    "<tr><td>size</td><td>param</td><td>int</td><td>optional</td><td>The default query/page size is 20. Admins can set the default query/page size in the <a href=\"/settings\">settings</a></td></tr>" +                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"msg\": \"\",\n" +
                    "    \"message\": \"\",\n" +
                    "    \"status\": \"success\",\n" +
                    "    \"mediaTypeFilter\": \"&lt;media_type&gt;\",\n" +
                    "    \"folder\": \"&lt;folder_name&gt;\",\n" +
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
                    "    },\n" +
                    "    \"albumsList\": [\n" +
                    "        {\n" +
                    "            \"coverUrl\": \"&lt;relative_cover_url&gt;\",\n" +
                    "            \"albumVideoCount\": &lt;album_video_count&gt;,\n" +
                    "            \"albumPhotoCount\": &lt;album_photo_count&gt;,\n" +
                    "            \"id\": &lt;album_id&gt;,\n" +
                    "            \"shareUrl\": \"&lt;relative_share_url&gt;\",\n" +
                    "            \"name\": \"&lt;album_name&gt;\"\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"recognitionLabels\": [\n" +
                    "        {\n" +
                    "            \"id\": &lt;album_id&gt;,\n" +
                    "            \"name\": \"&lt;subject_name&gt;\"\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"labelPhotoMap\": {\n" +
                    "        \"&lt;metadata_id&gt;\": \"&lt;subject_name&gt;\"\n" +
                    "    },\n" +
                    "    \"albumMap\": {\n" +
                    "        \"&lt;metadata_id&gt;\": \"&lt;album_name&gt;\"\n" +
                    "    },\n" +
                    "    \"keywordMap\": {\n" +
                    "        \"&lt;metadata_id&gt;\": \"&lt;keywords&gt;\"\n" +
                    "    }\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>mediaTypeFilter</td><td>string</td><td>One of \"all\", \"video\" or \"image\"</td></tr>" +
                    "<tr><td>folder</td><td>string</td><td>full path and name of the folder</td></tr>" +
                    "<tr><td>metadataList[].metadata</td><td>object</td><td>A <a href=\"#\" data-bs-toggle=\"modal\" data-bs-target=\"#propMetadataDocs\">Metadata</a> object</td></tr>" +
                    "<tr><td>favourites.&lt;metadata_id&gt;.count</td><td>int</td><td>The number of people who saved this media as a favorite</td></tr>" +
                    "<tr><td>favourites.&lt;metadata_id&gt;.favorite</td><td>boolean</td><td>True if saved as a favorite</td></tr>" +
                    "<tr><td>albumList[].coverUrl</td><td>string</td><td>Relative URL for the album cover image</td></tr>" +
                    "<tr><td>albumList[].albumVideoCount</td><td>string</td><td>The number of videos in this album</td></tr>" +
                    "<tr><td>albumList[].albumPhotoCount</td><td>string</td><td>The number of photos in this album</td></tr>" +
                    "<tr><td>albumList[].id</td><td>int</td><td>The album ID</td></tr>" +
                    "<tr><td>albumList[].shareUrl</td><td>string</td><td>Part of the share URL endpoint for public sharing</td></tr>" +
                    "<tr><td>albumList[].name</td><td>string</td><td>The album name</td></tr>" +
                    "<tr><td>recognitionLabels[].id</td><td>int</td><td>Album ID the subject is associated with</td></tr>" +
                    "<tr><td>recognitionLabels[].name</td><td>string</td><td>Subject name</td></tr>" +
                    "<tr><td>labelPhotoMap.&lt;metadata_id&gt;.&lt;subject_name&gt;</td><td>string</td><td>names associated with media</td></tr>" +
                    "<tr><td>albumMap.&lt;metadata_id&gt;.&lt;album_name&gt;</td><td>string</td><td>album name associated with media</td></tr>" +
                    "<tr><td>keywordMap.&lt;metadata_id&gt;.&lt;keywords&gt;</td><td>string</td><td>keywords associated with media</td></tr>" +
                    "</tbody></table>"
        )
    )
    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/api/v1/folder/{folder}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedFolder(model: Model, request: HttpServletRequest, @PathVariable folder: String, @RequestParam page: Optional<Int>, @RequestParam size: Optional<Int>): String {
        return mapper.writeValueAsString(buildFolder(model,URLDecoder.decode(folder, StandardCharsets.UTF_8.toString()), page.orElse(0), size.orElse(model.getAttribute("queryLimit").toString().toInt())))
    }

    private fun buildFolder(model: Model, folder: String, page: Int = 0, size: Int = model.getAttribute("queryLimit").toString().toInt()): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()
        response["message"] = "Nothing to see here."
        response["metadataList"] = mutableListOf<Metadata>()
        response["favorites"] = mutableMapOf<String, Any>()
        response["albumList"] = mutableListOf<Album>()
        response["recognitionLabels"] = mutableListOf<RecognitionLabel>()
        response["labelPhotoMap"] = mutableMapOf<String, Any>()
        response["mediaTypeFilter"] = "all"
        response["albumMap"] = mutableMapOf<String, Any>()
        response["keywordMap"] = mutableMapOf<String, Any>()
        response["folder"] = folder
        response["page"] = page
        response["size"] = size
        response["totalPages"] = 0

        response["msg"] = "Could not get results"
        response["status"] = ApiResponse.FAIL.status

        if (model.getAttribute("currentUser") != "") {
            val currentUserObj = model.getAttribute("currentUser") as User?
            val pageValue = page*size

            val favoritesMap = HashMap<String, HashMap<String, Any>>()


            val folderTotalCount = metadataRepository.countFolder(folder)
            response["totalPages"] = ceil((folderTotalCount.toDouble()) / size.toDouble()).toInt()
            val metadataList = metadataRepository.findAllByFolderOffsetAndLimit(
                folder,
                pageValue,
                size
            ).toMutableList()

            if (metadataList.isNotEmpty()) {
                response["metadataList"] = metadataList
                response["message"] = ""
                response["favorites"] = favoritesMap

                val recognitionLabels =
                    recognitionLabelRepository?.findAllByNameNotContaining(TextUtils.getObjectName())
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
                    }
                    keywordMap[metadata.getId()] = keywordMetadataList
                }
                response["labelPhotoMap"] = labelPhotoMap
                response["albumMap"] = albumMap
                response["keywordMap"] = keywordMap

                val albumList = albumRepository.findAllOrderByAlbumName()
                if (albumList != null && albumList.count() > 0) {
                    response["albumList"] = albumList
                }

                response["favorites"] = favoritesMap
            }

            response["msg"] = "Results"
            response["status"] = ApiResponse.SUCCESS.status
        }

        return response
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/folder/update"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    fun postFolderUpdate(model: Model, @RequestBody requestBody: JsonNode): String {

        val response = mutableMapOf<String, Any?>()

        val folderMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})

        response["responseData"] = mutableMapOf<String, Any?>()
        response["msg"] = ""
        response["status"] = ApiResponse.FAIL.status

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null && folderMap.containsKey("setCoverFolder") && folderMap["setCoverFolder"].toString().toBoolean() && folderMap.containsKey("metadataId") && folderMap.containsKey("folder")) {
            val metadataId = folderMap["metadataId"].toString()
            val folder = folderMap["folder"].toString()

            if (folderDataRepository!!.countByFolder(folder) == 1) {
                val folderData = folderDataRepository!!.findByFolder(folder)
                folderData.setMid(metadataId)
                folderDataRepository!!.save(folderData)

                response["msg"] = ""
                response["status"] = ApiResponse.SUCCESS.status
            } else {
                response["msg"] = "Single entry not found."
            }
        }

        return mapper.writeValueAsString(response)
    }
}