package com.miyagi.shashin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.*
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.ApiResponse
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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*
import javax.servlet.http.HttpServletRequest
import kotlin.collections.HashMap

@Controller
@Secured("ROLE_ADMIN")
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

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()

    @RequestMapping(value = ["/recent"], method = [RequestMethod.GET])
    fun getRecentlyAdded(model: Model): String {
        val module = "recent"

        buildInitialPage(module,model)

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getPagedModified",
            description = "<strong>Get paged results for recently added content.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/recent/{page}\" \\\n" +
                    "-H \"Content-Type: application/json\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>Content-Type</td><td>header</td><td>string</td><td>required</td><td>application/json</td></tr>" +
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
    @RequestMapping(value = ["/recent/{page}","/api/v1/recent/{page}"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun getPagedRecent(model: Model, request: HttpServletRequest, @PathVariable page: Int): String {
        return mapper.writeValueAsString(buildBrowseRecord("recent",model,page))
    }

    @RequestMapping(value = ["/modified"], method = [RequestMethod.GET])
    fun getModified(model: Model): String {
        val module = "modified"

        buildInitialPage(module,model)

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getPagedModified",
            description = "<strong>Get paged results for recently modified content.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/modified/{page}\" \\\n" +
                    "-H \"Content-Type: application/json\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>Content-Type</td><td>header</td><td>string</td><td>required</td><td>application/json</td></tr>" +
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
    @RequestMapping(value = ["/modified/{page}","/api/v1/modified/{page}"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun getPagedModified(model: Model, request: HttpServletRequest, @PathVariable page: Int): String {
        return mapper.writeValueAsString(buildBrowseRecord("modified",model,page))
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getPagedModified",
            description = "<strong>Get paged results for recently modified content.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/modified?page={page}&size={size}\" \\\n" +
                    "-H \"Content-Type: application/json\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>Content-Type</td><td>header</td><td>string</td><td>required</td><td>application/json</td></tr>" +
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
    @RequestMapping(value = ["/modified/{page}","/api/v1/modified"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun getPagedSizeModified(model: Model, request: HttpServletRequest, @RequestParam page: Optional<Int>, @RequestParam size: Optional<Int>): String {
        return mapper.writeValueAsString(buildBrowseRecord("modified", model ,page.orElse(0), size.orElse(model.getAttribute("queryLimit").toString().toInt())))
    }

    private fun buildBrowseRecord(module: String, model: Model, page: Int = 0, size: Int = model.getAttribute("queryLimit").toString().toInt()): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()
        response["message"] = "There are no photos. Please setup directories to scan in Settings and index media in Media Indexing."
        response["metadataList"] = mutableListOf<Metadata>()
        response["favorites"] = mutableMapOf<String, Any>()
        response["albumList"] = mutableListOf<Album>()
        response["recognitionLabels"] = mutableListOf<RecognitionLabel>()
        response["labelPhotoMap"] = mutableMapOf<String, Any>()
        response["mediaTypeFilter"] = "all"
        response["albumMap"] = mutableMapOf<String, Any>()
        response["keywordMap"] = mutableMapOf<String, Any>()

        response["msg"] = "Could not get results"
        response["status"] = ApiResponse.FAIL.status

        if (model.getAttribute("currentUser") != "") {
            val currentUserObj = model.getAttribute("currentUser") as User?
            val pageValue = page*size

            val favoritesMap = HashMap<String, HashMap<String, Any>>()

            var metadataList = mutableListOf<Metadata>()
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
            }

            if (metadataList.isNotEmpty()) {
                response["metadataList"] = metadataList
                response["message"] = ""
                response["favorites"] = favoritesMap

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

                val albumList = albumRepository.findAllOrderByAlbumName()
                if (albumList.count() > 0) {
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

        for ((k, v) in response) {
            model[k] = v!!
        }

        getAllAttribueData(model)

        return model
    }

    private fun buildInitialPage(module: String, model: Model): Model {
        val page = 0
        val response = buildBrowseRecord(module,model,page)
        for ((k, v) in response) {
            model[k] = v!!
        }

        for ((k, v) in response) {
            model[k] = v!!
        }

        getAllAttribueData(model)

        return model
    }

    @GetMapping("/folders")
    fun getFolders(model: Model): String {
        val module = "folders"
        buildInitialFoldersPage(model)

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    private fun buildPagedFolders(model: Model, page: Int = 0, size: Int = model.getAttribute("queryLimit").toString().toInt()): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        response["status"] = ApiResponse.FAIL.status

        val module = "folders"
        response["msg"] = "There are no folders."
        response["message"] = "There are no folders."
        response["foldersList"] = mutableListOf<Folder>()

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            val pageValue = page*size
            val folderObj = metadataRepository.findFoldersOffsetAndLimit(pageValue, size)

            if (folderObj != null && folderObj.count() > 0) {
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
            description = "<strong>Get paged list of all folders.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/folders/{page}\" \\\n" +
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
    @RequestMapping(value = ["/folders/{page}","/api/v1/folders/{page}"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun getPagedFolders(model: Model, request: HttpServletRequest, @PathVariable page: Int): String {
        return mapper.writeValueAsString(buildPagedFolders(model,page))
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getPagedSizeFolders",
            description = "<strong>Get list of all folders.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/folders?page={page}&size={size}\" \\\n" +
                    "-H \"Content-Type: application/json\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>Content-Type</td><td>header</td><td>string</td><td>required</td><td>application/json</td></tr>" +
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
    @RequestMapping(value = ["/api/v1/folders"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun getPagedSizeFolders(model: Model, request: HttpServletRequest, @RequestParam page: Optional<Int>, @RequestParam size: Optional<Int>): String {
        return mapper.writeValueAsString(buildPagedFolders(model, page.orElse(0), size.orElse(model.getAttribute("queryLimit").toString().toInt())))
    }

    @RequestMapping(value = ["/folder/{folder}"], method = [RequestMethod.GET])
    fun getRecentlyAdded(model: Model, @PathVariable folder: String): String {
        val module = "folder"
        val page = 0
        val decodedValue = URLDecoder.decode(folder, StandardCharsets.UTF_8.toString())
        val response = buildFolder(model,decodedValue,page)

        for ((k, v) in response) {
            model[k] = v!!
        }

        getAllAttribueData(model)

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module) + " - " +  decodedValue

        return module
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getPagedFolder",
            description = "<strong>Get paged results for folder content.</strong> Pages start from 0. The page size can be configured through the web interface (default 20).<br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/folder/{page}/{folder}\" \\\n" +
                    "-H \"Content-Type: application/json\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>Content-Type</td><td>header</td><td>string</td><td>required</td><td>application/json</td></tr>" +
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
    @RequestMapping(value = ["/folder/{page}/{folder}","/api/v1/folder/{page}/{folder}"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun getPagedFolder(model: Model, request: HttpServletRequest, @PathVariable page: Int, @PathVariable folder: String): String {
        return mapper.writeValueAsString(buildFolder(model,URLDecoder.decode(folder, StandardCharsets.UTF_8.toString()),page))
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getPagedFolder",
            description = "<strong>Get paged results for folder content.</strong> Pages start from 0. The page size can be configured through the web interface (default 20).<br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/folder/{folder}?page={page}&size={size}\" \\\n" +
                    "-H \"Content-Type: application/json\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>Content-Type</td><td>header</td><td>string</td><td>required</td><td>application/json</td></tr>" +
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
    @RequestMapping(value = ["/api/v1/folder/{folder}"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun getPagedFolder(model: Model, request: HttpServletRequest, @PathVariable folder: String, @RequestParam page: Optional<Int>, @RequestParam size: Optional<Int>): String {
        return mapper.writeValueAsString(buildFolder(model,URLDecoder.decode(folder, StandardCharsets.UTF_8.toString()), page.orElse(0), size.orElse(model.getAttribute("queryLimit").toString().toInt())))
    }

    private fun buildFolder(model: Model, folder: String, page: Int = 0, size: Int = model.getAttribute("queryLimit").toString().toInt()): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()
        response["message"] = "There are no photos. Please setup directories to scan in Settings and index media in Media Indexing."
        response["metadataList"] = mutableListOf<Metadata>()
        response["favorites"] = mutableMapOf<String, Any>()
        response["albumList"] = mutableListOf<Album>()
        response["recognitionLabels"] = mutableListOf<RecognitionLabel>()
        response["labelPhotoMap"] = mutableMapOf<String, Any>()
        response["mediaTypeFilter"] = "all"
        response["albumMap"] = mutableMapOf<String, Any>()
        response["keywordMap"] = mutableMapOf<String, Any>()
        response["folder"] = folder

        response["msg"] = "Could not get results"
        response["status"] = ApiResponse.FAIL.status

        if (model.getAttribute("currentUser") != "") {
            val currentUserObj = model.getAttribute("currentUser") as User?
            val pageValue = page*size

            val favoritesMap = HashMap<String, HashMap<String, Any>>()

            val metadataList: MutableList<Metadata> = metadataRepository.findAllByFolderOffsetAndLimit(
                folder,
                pageValue,
                size
            ).toMutableList()

            if (metadataList.isNotEmpty()) {
                response["metadataList"] = metadataList
                response["message"] = ""
                response["favorites"] = favoritesMap

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
                    }
                    keywordMap[metadata.getId()] = keywordMetadataList
                }
                response["labelPhotoMap"] = labelPhotoMap
                response["albumMap"] = albumMap
                response["keywordMap"] = keywordMap

                val albumList = albumRepository.findAllOrderByAlbumName()
                if (albumList.count() > 0) {
                    response["albumList"] = albumList
                }

                response["favorites"] = favoritesMap
            }

            response["msg"] = "Results"
            response["status"] = ApiResponse.SUCCESS.status
        }

        return response
    }
}