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
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import org.springframework.context.MessageSource
import org.springframework.http.MediaType
import org.springframework.web.multipart.MultipartFile
import kotlin.collections.HashMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.collections.mutableListOf
import kotlin.math.ceil

@Controller
class BrowseController(
    private var metadataRepository: MetadataRepository,
    private var albumRepository: AlbumRepository,
    private var albumPhotoRepository: AlbumPhotoRepository,
    private var favoriteRepository: FavoriteRepository,
    private var keywordRepository: KeywordRepository,
    private var recognitionLabelRepository: RecognitionLabelRepository? = null,
    private var recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository? = null,
    private var folderDataRepository: FolderDataRepository? = null,
    private var duplicatesRepository: DuplicatesRepository? = null,
    private var settingsController: SettingsController,
    var messageSource: MessageSource? = null
): BaseController(
    recognitionLabelRepository = recognitionLabelRepository,
    albumRepository = albumRepository,
    keywordRepository = keywordRepository,
    metadataRepository = metadataRepository
) {
    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()

    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/metadata/media/upload/batch","/api/v1/metadata/media/upload/batch"], method = [RequestMethod.POST], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE], produces = ["application/json"])
    @ResponseBody
    fun postUploadToTimeline(model: Model, session: HttpSession, @RequestParam("files[]") media: List<MultipartFile>, locale: Locale): String {
        resp["msg"] = messageSource?.getMessage("main.modal.saved.fail", null, locale)
        resp["status"] = ApiResponse.FAIL.status

        val hasMediaUploadDirectory = model.getAttribute("hasMediaUploadDirectory") as Boolean?

        val settings = model.getAttribute("settings") as Settings?

        val currentUserObj = model.getAttribute("currentUser") as User?

        if (currentUserObj != null && !media.isEmpty() && hasMediaUploadDirectory != null && hasMediaUploadDirectory && !settings?.getUploadMediaDirectory().isNullOrBlank()) {
            val fileUploadedMap = FileUtils.copyMultipartFiles(media, settings)
            val uploadedFiles = fileUploadedMap["uploadedFiles"] as MutableList<String>
            val notUploadedFiles = fileUploadedMap["notUploadedFiles"] as MutableList<String>

            if (!uploadedFiles.isEmpty()) {
                settingsController.scanMediaDirectories(false, 0, currentUserObj.getId(), locale)
            }

            if (!notUploadedFiles.isEmpty() && !uploadedFiles.isEmpty()) {
                resp["msg"] = messageSource?.getMessage("main.pages.browse.success.save.format", arrayOf(uploadedFiles.joinToString("<br>"),notUploadedFiles.joinToString("<br>")), locale)
                resp["status"] = ApiResponse.FAIL.status
            } else if (!notUploadedFiles.isEmpty() && uploadedFiles.isEmpty()) {
                resp["msg"] = messageSource?.getMessage("main.pages.browse.fail.save.format", arrayOf(notUploadedFiles.joinToString("<br>")), locale)
                resp["status"] = ApiResponse.FAIL.status
            } else if (!uploadedFiles.isEmpty()) {
                resp["msg"] = messageSource?.getMessage("main.pages.browse.success.save", null, locale)
                resp["status"] = ApiResponse.SUCCESS.status
            } else {
                resp["msg"] = messageSource?.getMessage("main.pages.browse.success.save", null, locale)
                resp["status"] = ApiResponse.SUCCESS.status
            }
        }

        return mapper.writeValueAsString(resp)
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/recent","/recent/{mediaType}"], method = [RequestMethod.GET])
    fun getRecentlyAdded(model: Model,@PathVariable(required = false) mediaType: String?,locale: Locale): String {
        val module = "recent"
        buildInitialPage(module,model,mediaType,locale)

        model["pageParam"] = 0
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/recent/{page}/{mediaType}"], method = [RequestMethod.GET])
    fun getRecentlyAddedPage(model: Model,@PathVariable(required = true) page: Int,@PathVariable(required = true) mediaType: String, locale: Locale): String {
        val module = "recent"

        val response = buildBrowseRecord(module,model,page,model.getAttribute("queryLimit").toString().toInt(),mediaType, locale)
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
    @RequestMapping(value = ["/duplicates"], method = [RequestMethod.GET])
    fun getDuplicates(model: Model,locale: Locale): String {
        val module = "duplicates"
        buildInitialPage(module,model,null,locale)

        model["pageParam"] = 0
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/duplicates/page/{page}","/api/v1/duplicates/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getDuplicatesJson(model: Model,@PathVariable(required = true) page: Int, locale: Locale): String {
        return mapper.writeValueAsString(buildBrowseRecord("duplicates",model,page,model.getAttribute("queryLimit").toString().toInt(),null, locale))
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/duplicates/{page}"], method = [RequestMethod.GET])
    fun getDuplicatesPage(model: Model,@PathVariable(required = true) page: Int, locale: Locale): String {
        val module = "duplicates"

        val response = buildBrowseRecord(module,model,page,model.getAttribute("queryLimit").toString().toInt(),null, locale)
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
    fun getPagedRecent(model: Model, request: HttpServletRequest, @PathVariable page: Int,@PathVariable(required = false) mediaType: String?, locale: Locale): String {
        return mapper.writeValueAsString(buildBrowseRecord("recent",model,page, model.getAttribute("queryLimit").toString().toInt(), mediaType, locale))
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/taken","/taken/{mediaType}"], method = [RequestMethod.GET])
    fun getTaken(model: Model,@PathVariable(required = false) mediaType: String?,locale: Locale): String {
        val module = "taken"

        buildInitialPage(module,model,mediaType,locale)

        model["pageParam"] = 0
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/taken/{page}","/taken/mediatype/{mediaType}/page/{page}","/api/v1/taken/{page}", "/api/v1/taken/mediatype/{mediaType}/page/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedTaken(model: Model, request: HttpServletRequest, @PathVariable page: Int, @PathVariable(required = false) mediaType: String?, locale: Locale): String {
        return mapper.writeValueAsString(buildBrowseRecord("taken",model,page,model.getAttribute("queryLimit").toString().toInt(),mediaType, locale))
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/taken/{page}/{mediaType}"], method = [RequestMethod.GET])
    fun getTakenAddedPage(model: Model,@PathVariable(required = true) page: Int,@PathVariable(required = true) mediaType: String, locale: Locale): String {
        val module = "taken"

        val response = buildBrowseRecord(module,model,page,model.getAttribute("queryLimit").toString().toInt(),mediaType, locale)
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
    fun getModified(model: Model,@PathVariable(required = false) mediaType: String?,locale: Locale): String {
        val module = "modified"

        buildInitialPage(module,model,mediaType,locale)

        model["pageParam"] = 0
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/modified/{page}/{mediaType}"], method = [RequestMethod.GET])
    fun getModifiedAddedPage(model: Model,@PathVariable(required = true) page: Int,@PathVariable(required = true) mediaType: String, locale: Locale): String {
        val module = "modified"

        val response = buildBrowseRecord(module,model,page,model.getAttribute("queryLimit").toString().toInt(),mediaType,locale)
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
    fun getPagedModified(model: Model, request: HttpServletRequest, @PathVariable page: Int,@PathVariable(required = false) mediaType: String?, locale: Locale): String {
        return mapper.writeValueAsString(buildBrowseRecord("modified",model,page,model.getAttribute("queryLimit").toString().toInt(),mediaType, locale))
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/accessed","/accessed/{mediaType}"], method = [RequestMethod.GET])
    fun getAccessed(model: Model,@PathVariable(required = false) mediaType: String?,locale: Locale): String {
        val module = "accessed"

        buildInitialPage(module,model,mediaType,locale)

        model["pageParam"] = 0
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/accessed/{page}/{mediaType}"], method = [RequestMethod.GET])
    fun getAccessedAddedPage(model: Model,@PathVariable(required = true) page: Int,@PathVariable(required = true) mediaType: String, locale: Locale): String {
        val module = "accessed"

        val response = buildBrowseRecord(module,model,page,model.getAttribute("queryLimit").toString().toInt(),mediaType, locale)
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
    fun getPagedAccessed(model: Model, request: HttpServletRequest, @PathVariable page: Int,@PathVariable(required = false) mediaType: String?, locale: Locale): String {
        return mapper.writeValueAsString(buildBrowseRecord("accessed",model,page,model.getAttribute("queryLimit").toString().toInt(),mediaType, locale))
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
    fun getPagedSizeModified(model: Model, request: HttpServletRequest, @RequestParam page: Optional<Int>, @RequestParam size: Optional<Int>, @PathVariable(required = false) mediaType: String?, locale: Locale): String {
        return mapper.writeValueAsString(buildBrowseRecord("modified", model ,page.orElse(0), size.orElse(model.getAttribute("queryLimit").toString().toInt()), mediaType, locale))
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/metadata/list/{module}/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getMetadataList(model: Model,@PathVariable module: String,@PathVariable page: Int, @RequestParam folder: Optional<String>, locale: Locale): String {
        val response = mutableMapOf<String, Any?>()
        var metadataList: MutableIterable<Metadata>? = null
        response["metadataList"] = mutableListOf<Metadata>()
        response["msg"] = messageSource?.getMessage("main.results", null, locale)
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
    fun getBrowseAlbumList(model: Model, locale: Locale): String? {
        val response = mutableMapOf<String, Any?>()
        response["msg"] = messageSource?.getMessage("main.noresults", null, locale)
        response["status"] = ApiResponse.FAIL.status
        response["albumList"] = mutableListOf<Album>()

        try {
            val albumList = albumRepository.findAllOrderByAlbumName()
            if (albumList != null && albumList.count() > 0) {
                response["albumList"] = albumList
                response["msg"] = messageSource?.getMessage("main.results", null, locale)
                response["status"] = ApiResponse.SUCCESS.status
            }
        } catch(_: Exception) {}

        return mapper.writeValueAsString(response)
    }

    private fun buildBrowseRecord(module: String, model: Model, page: Int = 0, size: Int = model.getAttribute("queryLimit").toString().toInt(), mediaTypeFilter: String?, locale: Locale): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()
        var message = "<a href='/articles/quickstart' target='_blank'>"+messageSource?.getMessage("main.nothing", null, locale)+"</a>"
        if (module == "duplicates") {
            message = messageSource?.getMessage("main.nothing", null, locale).toString()
        }
        response["message"] = message
        response["metadataList"] = mutableListOf<Metadata>()
        response["favorites"] = mutableMapOf<String, Any>()
        response["albumList"] = mutableListOf<Album>()
        response["recognitionLabels"] = mutableListOf<RecognitionLabel>()
        response["labelPhotoMap"] = mutableMapOf<String, Any>()
        response["mediaTypeFilter"] = "all"
        response["albumMap"] = mutableMapOf<String, Any>()
        response["keywordMap"] = mutableMapOf<String, Any>()
        response["placenameMap"] = mutableMapOf<String, MutableList<String>?>()
        response["formattedDateMap"] = mutableMapOf<String, String>()
        response["page"] = page
        response["size"] = size
        response["totalPages"] = 0

        response["msg"] = messageSource?.getMessage("main.noresults", null, locale)
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
                    "duplicates" -> {
                        response["totalPages"] = ceil((duplicatesRepository?.countAllMetadataIds()!!.toDouble()) / size.toDouble()).toInt()

                        metadataList = duplicatesRepository?.findAllMetadataIds(
                            pageValue,
                            size
                        )!!.toMutableList()
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
            } else if (mediaType == "description") {
                response["totalPages"] = ceil((metadataRepository.countByDescriptionAndOffsetAndLimit().toDouble()) / size.toDouble()).toInt()

                when (module) {
                    "recent" -> {
                        metadataList = metadataRepository.findRecentByDescriptionAndOffsetAndLimit(
                            pageValue,
                            size
                        ).toMutableList()
                    }
                    "modified" -> {
                        metadataList = metadataRepository.findModifiedByDescriptionAndOffsetAndLimit(
                            pageValue,
                            size
                        ).toMutableList()
                    }
                    "taken" -> {
                        metadataList = metadataRepository.findTakenByDescriptionAndOffsetAndLimit(
                            pageValue,
                            size
                        ).toMutableList()
                    }
                    "accessed" -> {
                        metadataList = metadataRepository.findLastAccessedByDescriptionAndOffsetAndLimit(
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
                val formattedDateMap = HashMap<String, String>()

                for (metadata in metadataList) {
                    var date = metadata.getTakenAt().toString()
                    when (module) {
                        "recent" -> {
                            date = metadata.getAddedAt().toString()
                        }
                        "modified" -> {
                            date = metadata.getModifiedAt().toString()
                        }
                        "accessed" -> {
                            date = metadata.getLastAccessedAt().toString()
                        }
                    }

                    formattedDateMap[metadata.getId().toString()] = TextUtils.formatToLongDate(date, model.getAttribute("locale").toString()).toString()

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
                response["formattedDateMap"] = formattedDateMap

                val albumList = albumRepository.findAllOrderByAlbumName()
                if (albumList != null && albumList.count() > 0) {
                    response["albumList"] = albumList
                }

                response["favorites"] = favoritesMap
            }

            response["msg"] = messageSource?.getMessage("main.results", null, locale)
            response["status"] = ApiResponse.SUCCESS.status
        }

        return response
    }

    private fun buildInitialFoldersPage(model: Model, locale: Locale): Model {
        val page = 0
        val response = buildPagedFolders(model,page,model.getAttribute("queryLimit").toString().toInt(), locale)

        for ((k, v) in response) {
            model[k] = v!!
        }

        getAllAttributeData(model)

        return model
    }

    private fun buildInitialPage(module: String, model: Model, mediaTypeFilter: String?, locale: Locale): Model {
        val response = buildBrowseRecord(module,model,0,model.getAttribute("queryLimit").toString().toInt(),mediaTypeFilter, locale)

        for ((k, v) in response) {
            model[k] = v!!
        }

        getAllAttributeData(model)

        model["foldersCount"] = metadataRepository.countByFolder()

        return model
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/browse/mediatype/{mediaTypeFilter}/date/{date}/{view}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getBrowseMetadataListFromDate(@PathVariable mediaTypeFilter: String, @PathVariable date: String, @PathVariable view: String, locale: Locale): String? {
        val response = mutableMapOf<String, Any?>()
        response["msg"] = messageSource?.getMessage("main.noresults", null, locale)
        response["status"] = ApiResponse.FAIL.status
        response["metadataList"] = mutableListOf<Metadata>()
        val dateArray = date.split("-")

        if (dateArray.size == 3) {
            val year = dateArray[0].toInt()
            val month = dateArray[1].toInt()
            val day = dateArray[2].toInt()

            val dbDate = year.toString() + "-" + (if (month > 9) month.toString() else "0$month") + "-" + (if (day > 9) day.toString() else "0$day")
            val startDate = "$dbDate 00:00:00"
            val endDate = "$dbDate 23:59:59"

            response["msg"] = ""
            response["status"] = ApiResponse.SUCCESS.status
            var metadataList = mutableListOf<Metadata>()

            // accessed, modified, added, taken in timeline controller
            if (mediaTypeFilter == "all") {
                when (view) {
                    "accessed" -> {
                        metadataList = metadataRepository.findLastAccessedByDate(
                            startDate, endDate
                        ).toMutableList()
                    }
                    "modified" -> {
                        metadataList = metadataRepository.findModifiedByDate(
                            startDate, endDate
                        ).toMutableList()
                    }
                    "recent" -> {
                        metadataList = metadataRepository.findRecentByDate(
                            startDate, endDate
                        ).toMutableList()
                    }
                    "taken" -> {
                        metadataList = metadataRepository.findTakenByDate(
                            startDate, endDate
                        ).toMutableList()
                    }
                }
            } else if (mediaTypeFilter == "nolatlng") {
                when (view) {
                    "accessed" -> {
                        metadataList = metadataRepository.findLastAccessedByNoCoordAndDate(
                            startDate, endDate
                        ).toMutableList()
                    }
                    "modified" -> {
                        metadataList = metadataRepository.findModifiedByNoCoordAndDate(
                            startDate, endDate
                        ).toMutableList()
                    }
                    "recent" -> {
                        metadataList = metadataRepository.findRecentByNoCoordAndDate(
                            startDate, endDate
                        ).toMutableList()
                    }
                    "taken" -> {
                        metadataList = metadataRepository.findTakenByNoCoordAndDate(
                            startDate, endDate
                        ).toMutableList()
                    }
                }
            } else if (mediaTypeFilter == "description") {
                when (view) {
                    "accessed" -> {
                        metadataList = metadataRepository.findLastAccessedByDescriptionAndDate(
                            startDate, endDate
                        ).toMutableList()
                    }
                    "modified" -> {
                        metadataList = metadataRepository.findModifiedByDescriptionAndDate(
                            startDate, endDate
                        ).toMutableList()
                    }
                    "recent" -> {
                        metadataList = metadataRepository.findRecentByDescriptionAndDate(
                            startDate, endDate
                        ).toMutableList()
                    }
                    "taken" -> {
                        metadataList = metadataRepository.findTakenByDescriptionAndDate(
                            startDate, endDate
                        ).toMutableList()
                    }
                }
            } else {
                when (view) {
                    "accessed" -> {
                        metadataList = metadataRepository.findLastAccessedByMediaTypeAndDate(
                            mediaTypeFilter, startDate, endDate
                        ).toMutableList()
                    }
                    "modified" -> {
                        metadataList = metadataRepository.findModifiedByMediaTypeAndDate(
                            mediaTypeFilter, startDate, endDate
                        ).toMutableList()
                    }
                    "recent" -> {
                        metadataList = metadataRepository.findRecentByMediaTypeAndDate(
                            mediaTypeFilter, startDate, endDate
                        ).toMutableList()
                    }
                    "taken" -> {
                        metadataList = metadataRepository.findTakenByMediaTypeAndDate(
                            mediaTypeFilter, startDate, endDate
                        ).toMutableList()
                    }
                }
            }

            response["metadataList"] = metadataList
        }

        return mapper.writeValueAsString(response)
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/browse/range/{metadataId}/{view}/{mediaType}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getMetadataIdsBetweenRange(model: Model,@PathVariable(required = true) metadataId: String?, @PathVariable(required = true) view: String?, @PathVariable(required = true) mediaType: String?, locale: Locale): String {
        val retMetadataIdArray = mutableListOf<MutableList<String>>()
        val response = mutableMapOf<String, Any?>()

        response["msg"] = messageSource?.getMessage("main.noresults", null, locale)
        response["status"] = ApiResponse.FAIL.status
        response["metadataIdArray"] = mutableListOf<MutableList<String>>()

        if (metadataId !== null && metadataId !== "") {
            val metadata = metadataRepository.findByMetadataId(metadataId)

            val metadataDate = if (view == "accessed") {
                metadata?.getLastAccessedAt().toString()
            } else if (view == "modified") {
                metadata?.getModifiedAt().toString()
            } else if (view == "recent") {
                metadata?.getAddedAt().toString()
            } else {
                metadata?.getTakenAt().toString()
            }

            val ymdArray = metadataDate.split(" ")
            val ymd = ymdArray[0]

            var startDate = "$ymd 00:00:00"
            var endDate = "$ymd 23:59:59"

            val metadatas = if (view == "accessed") {
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
                    metadataRepository.findMetadataIdBetweenTakenAtWithType(startDate, endDate, mediaType.toString())
                }
            } else {
                if (mediaType == "all") {
                    metadataRepository.findMetadataIdBetweenTakenAt(startDate, endDate)
                } else {
                    metadataRepository.findMetadataIdBetweenTakenAtWithType(startDate, endDate, mediaType.toString())
                }
            }

            if (metadatas != null && metadatas.isNotEmpty()) {
                for (metadata in metadatas) {
                    retMetadataIdArray.add(mutableListOf(metadata.getId(),metadata.getFileName()!!, "/api/v1/thumbnails/centered/"+metadata.getId()))
                }

                response["msg"] = messageSource?.getMessage("main.success", null, locale)
                response["status"] = ApiResponse.SUCCESS.status
                response["metadataIdArray"] = retMetadataIdArray
            }
        }

        return mapper.writeValueAsString(response)
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @GetMapping("/folders")
    fun getFolders(model: Model, locale: Locale): String {
        val module = "folders"
        buildInitialFoldersPage(model, locale)

        model["pageParam"] = 0
        model["foldersCount"] = metadataRepository.countByFolder()
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    private fun buildPagedFolders(model: Model, page: Int = 0, size: Int = model.getAttribute("queryLimit").toString().toInt(), locale: Locale): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        response["status"] = ApiResponse.FAIL.status

        val module = "folders"
        response["msg"] = messageSource?.getMessage("main.nothing", null, locale)
        response["message"] = "<a href='/articles/quickstart' target='_blank'>"+messageSource?.getMessage("main.nothing", null, locale)+"</a>"
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
    fun getPagedFolders(model: Model, request: HttpServletRequest, @PathVariable page: Int, locale: Locale): String {
        return mapper.writeValueAsString(buildPagedFolders(model,page,model.getAttribute("queryLimit").toString().toInt(), locale))
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/folders/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    fun getPaginationAnonymousShareAlbum(model: Model, @PathVariable page: Int, locale: Locale): String? {
        val response = buildPagedFolders(model, page, model.getAttribute("queryLimit").toString().toInt(), locale)

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
    fun getPagedSizeFolders(model: Model, request: HttpServletRequest, @RequestParam page: Optional<Int>, @RequestParam size: Optional<Int>, locale: Locale): String {
        return mapper.writeValueAsString(buildPagedFolders(model, page.orElse(0), size.orElse(model.getAttribute("queryLimit").toString().toInt()), locale))
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/folder/{folder}"], method = [RequestMethod.GET])
    fun getFolder(model: Model, @PathVariable folder: String, locale: Locale): String {
        val module = "folder"
        val page = 0
        val decodedValue = URLDecoder.decode(folder, StandardCharsets.UTF_8.toString())
        val response = buildFolder(model,decodedValue,page,model.getAttribute("queryLimit").toString().toInt(), locale)

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
    fun getPagedFolder(model: Model, request: HttpServletRequest, @PathVariable page: Int, @PathVariable folder: String,locale: Locale): String {
        return mapper.writeValueAsString(buildFolder(model,URLDecoder.decode(folder, StandardCharsets.UTF_8.toString()),page,model.getAttribute("queryLimit").toString().toInt(),locale))
    }

    @RequestMapping(value = ["/folder/{folder}/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    fun getPaginationFolder(model: Model, @PathVariable folder: String,@PathVariable page: Int,locale: Locale): String? {
        val response = buildFolder(model,URLDecoder.decode(folder, StandardCharsets.UTF_8.toString()),page,model.getAttribute("queryLimit").toString().toInt(),locale)

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
    fun getPagedFolder(model: Model, request: HttpServletRequest, @PathVariable folder: String, @RequestParam page: Optional<Int>, @RequestParam size: Optional<Int>, locale: Locale): String {
        return mapper.writeValueAsString(buildFolder(model,URLDecoder.decode(folder, StandardCharsets.UTF_8.toString()), page.orElse(0), size.orElse(model.getAttribute("queryLimit").toString().toInt()), locale))
    }

    private fun buildFolder(model: Model, folder: String, page: Int = 0, size: Int = model.getAttribute("queryLimit").toString().toInt(), locale: Locale): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()
        response["message"] = "<a href='/articles/quickstart' target='_blank'>"+messageSource?.getMessage("main.nothing", null, locale)+"</a>"
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

        response["msg"] = messageSource?.getMessage("main.noresults", null, locale)
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

            response["msg"] = messageSource?.getMessage("main.results", null, locale)
            response["status"] = ApiResponse.SUCCESS.status
        }

        return response
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/folder/update"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    fun postFolderUpdate(model: Model, @RequestBody requestBody: JsonNode, locale: Locale): String {

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
                response["msg"] = messageSource?.getMessage("main.noresults", null, locale)
            }
        }

        return mapper.writeValueAsString(response)
    }
}