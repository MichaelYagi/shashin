package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.*
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.ApiResponse
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.MetadataProcessing
import com.miyagi.shashin.util.TextUtils
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import com.miyagi.shashin.util.TextUtils.Companion.returnForbiddenError
import io.swagger.v3.oas.annotations.Operation
import org.apache.commons.text.StringEscapeUtils
import org.springdoc.core.annotations.RouterOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import jakarta.servlet.http.HttpServletResponse
import jakarta.transaction.Transactional
import org.springframework.web.multipart.MultipartFile
import kotlin.collections.count
import kotlin.io.path.isDirectory


@Suppress("UNCHECKED_CAST")
@Controller
class AlbumsController: BaseController() {

    @Autowired
    private lateinit var albumRepository: AlbumRepository

    @Autowired
    private lateinit var albumPhotoRepository: AlbumPhotoRepository

    @Autowired
    private lateinit var userAlbumRepository: UserAlbumRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var metadataRepository: MetadataRepository

    @Autowired
    private lateinit var commentRepository: CommentRepository

    @Autowired
    private lateinit var favoriteRepository: FavoriteRepository

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var albumCommentRepository: AlbumCommentRepository

    @Autowired
    private lateinit var albumPhotoCommentRepository: AlbumPhotoCommentRepository

    @Autowired
    private val keywordRepository: KeywordRepository? = null

    @Autowired
    private lateinit var settingsController: SettingsController

    @Value("\${app.role.super}")
    private var superRole: String? = null

    @Value("\${app.role.admin}")
    private var adminRole: String? = null

    private var logger: Logger = Logger.getLogger(AlbumsController::class.simpleName)

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, Any?>()

    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @GetMapping("/albums")
    fun getAlbums(model: Model): String {
        val response = buildAlbums(model, 0)
        for ((k, v) in response) {
            model[k] = v!!
        }
        return model.getAttribute("activePage").toString()
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getAlbumsApi",
            summary = "Get paged list for all albums.",
            description = "<strong>Get paged list for all albums.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/albums/{page}\" \\\n" +
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
                    "    \"baseUrl\": \"&lt;base_url&gt;\",\n" +
                    "    \"showControls\": &lt;show_controls&gt;,\n" +
                    "    \"userCount\": &lt;user_count&gt;,\n" +
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
                    "    \"userAlbums\": [\n" +
                    "        {\n" +
                    "            \"id\": \"&lt;user_albums_id&gt;,\"\n" +
                    "            \"userId\": &lt;user_id&gt;,\n" +
                    "            \"albumId\": &lt;album_id&gt;\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"albumsCommentsMap\": \"&lt;albumId&gt;\": [\n" +
                    "        {\n" +
                    "            \"albumId\": \"&lt;album_id&gt;\",\n" +
                    "            \"commentId\": \"&lt;comment_id&gt;\",\n" +
                    "            \"comment\": &lt;comment&gt;,\n" +
                    "            \"userId\": &lt;user_id&gt;,\n" +
                    "            \"username\": &lt;username&gt;\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"sharedAlbums\": [\n" +
                    "        {\n" +
                    "            \"albumId\": &lt;album_id&gt;,\n" +
                    "            \"userId\": &lt;user_id&gt;,\n" +
                    "            \"username\": &lt;username&gt;,\n" +
                    "            \"isShared\": \"&lt;is_shared&gt;\"\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>baseUrl</td><td>string</td><td>Current base URL</td></tr>" +
                    "<tr><td>showControls</td><td>boolean</td><td>Set to true if an ADMIN role, and have icons shown to edit the album.</td></tr>" +
                    "<tr><td>userCount</td><td>int</td><td>Number of users</td></tr>" +
                    "<tr><td>albumList[].coverUrl</td><td>string</td><td>Relative URL for the album cover image</td></tr>" +
                    "<tr><td>albumList[].albumVideoCount</td><td>string</td><td>The number of videos in this album</td></tr>" +
                    "<tr><td>albumList[].albumPhotoCount</td><td>string</td><td>The number of photos in this album</td></tr>" +
                    "<tr><td>albumList[].id</td><td>int</td><td>The album ID</td></tr>" +
                    "<tr><td>albumList[].shareUrl</td><td>string</td><td>Part of the share URL endpoint for public sharing</td></tr>" +
                    "<tr><td>albumList[].name</td><td>string</td><td>The album name</td></tr>" +
                    "<tr><td>userAlbums[].id</td><td>int</td><td>The user albums ID</td></tr>" +
                    "<tr><td>userAlbums[].userId</td><td>int</td><td>The user ID to share the album</td></tr>" +
                    "<tr><td>userAlbums[].albumId</td><td>int</td><td>The album ID to share the album</td></tr>" +
                    "<tr><td>albumsCommentsMap.&lt;albumId&gt;[].albumId</td><td>int</td><td>The album ID for the comment</td></tr>" +
                    "<tr><td>albumsCommentsMap.&lt;albumId&gt;[].commentId</td><td>int</td><td>The comment ID for the comment</td></tr>" +
                    "<tr><td>albumsCommentsMap.&lt;albumId&gt;[].comment</td><td>string</td><td>The comment for this album</td></tr>" +
                    "<tr><td>albumsCommentsMap.&lt;albumId&gt;[].userId</td><td>int</td><td>The user ID for the comment</td></tr>" +
                    "<tr><td>albumsCommentsMap.&lt;albumId&gt;[].username</td><td>string</td><td>The username for the comment</td></tr>" +
                    "<tr><td>sharedAlbums[].albumId</td><td>int</td><td>The album ID used for who to allow sharing with</td></tr>" +
                    "<tr><td>sharedAlbums[].userId</td><td>int</td><td>The user ID used for who to allow sharing with</td></tr>" +
                    "<tr><td>sharedAlbums[].username</td><td>string</td><td>The username used for who to allow sharing with</td></tr>" +
                    "<tr><td>sharedAlbums[].isShared</td><td>boolean</td><td>Flag of whether this album is shared with this user or not</td></tr>" +
                    "</tbody></table>"
        )
    )
    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/api/v1/albums/{page}","/albums/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getAlbumsApi(model: Model, @PathVariable page: Int): String {
        return mapper.writeValueAsString(buildAlbums(model, page))
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getAlbumsApi",
            summary = "Get paged list for all albums.",
            description = "<strong>Get paged list for all albums.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/albums?page={page}&size={size}\" \\\n" +
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
                    "    \"baseUrl\": \"&lt;base_url&gt;\",\n" +
                    "    \"showControls\": &lt;show_controls&gt;,\n" +
                    "    \"userCount\": &lt;user_count&gt;,\n" +
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
                    "    \"userAlbums\": [\n" +
                    "        {\n" +
                    "            \"id\": \"&lt;user_albums_id&gt;,\"\n" +
                    "            \"userId\": &lt;user_id&gt;,\n" +
                    "            \"albumId\": &lt;album_id&gt;\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"albumsCommentsMap\": \"&lt;albumId&gt;\": [\n" +
                    "        {\n" +
                    "            \"albumId\": \"&lt;album_id&gt;\",\n" +
                    "            \"commentId\": \"&lt;comment_id&gt;\",\n" +
                    "            \"comment\": &lt;comment&gt;,\n" +
                    "            \"userId\": &lt;user_id&gt;,\n" +
                    "            \"username\": &lt;username&gt;\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"sharedAlbums\": [\n" +
                    "        {\n" +
                    "            \"albumId\": &lt;album_id&gt;,\n" +
                    "            \"userId\": &lt;user_id&gt;,\n" +
                    "            \"username\": &lt;username&gt;,\n" +
                    "            \"isShared\": \"&lt;is_shared&gt;\"\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>baseUrl</td><td>string</td><td>Current base URL</td></tr>" +
                    "<tr><td>showControls</td><td>boolean</td><td>Set to true if an ADMIN role, and have icons shown to edit the album.</td></tr>" +
                    "<tr><td>userCount</td><td>int</td><td>Number of users</td></tr>" +
                    "<tr><td>albumList[].coverUrl</td><td>string</td><td>Relative URL for the album cover image</td></tr>" +
                    "<tr><td>albumList[].albumVideoCount</td><td>string</td><td>The number of videos in this album</td></tr>" +
                    "<tr><td>albumList[].albumPhotoCount</td><td>string</td><td>The number of photos in this album</td></tr>" +
                    "<tr><td>albumList[].id</td><td>int</td><td>The album ID</td></tr>" +
                    "<tr><td>albumList[].shareUrl</td><td>string</td><td>Part of the share URL endpoint for public sharing</td></tr>" +
                    "<tr><td>albumList[].name</td><td>string</td><td>The album name</td></tr>" +
                    "<tr><td>userAlbums[].id</td><td>int</td><td>The user albums ID</td></tr>" +
                    "<tr><td>userAlbums[].userId</td><td>int</td><td>The user ID to share the album</td></tr>" +
                    "<tr><td>userAlbums[].albumId</td><td>int</td><td>The album ID to share the album</td></tr>" +
                    "<tr><td>albumsCommentsMap.&lt;albumId&gt;[].albumId</td><td>int</td><td>The album ID for the comment</td></tr>" +
                    "<tr><td>albumsCommentsMap.&lt;albumId&gt;[].commentId</td><td>int</td><td>The comment ID for the comment</td></tr>" +
                    "<tr><td>albumsCommentsMap.&lt;albumId&gt;[].comment</td><td>string</td><td>The comment for this album</td></tr>" +
                    "<tr><td>albumsCommentsMap.&lt;albumId&gt;[].userId</td><td>int</td><td>The user ID for the comment</td></tr>" +
                    "<tr><td>albumsCommentsMap.&lt;albumId&gt;[].username</td><td>string</td><td>The username for the comment</td></tr>" +
                    "<tr><td>sharedAlbums[].albumId</td><td>int</td><td>The album ID used for who to allow sharing with</td></tr>" +
                    "<tr><td>sharedAlbums[].userId</td><td>int</td><td>The user ID used for who to allow sharing with</td></tr>" +
                    "<tr><td>sharedAlbums[].username</td><td>string</td><td>The username used for who to allow sharing with</td></tr>" +
                    "<tr><td>sharedAlbums[].isShared</td><td>boolean</td><td>Flag of whether this album is shared with this user or not</td></tr>" +
                    "</tbody></table>"
        )
    )
    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/api/v1/albums"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getAlbumsApi(model: Model, @RequestParam page: Optional<Int>, @RequestParam size: Optional<Int>): String {
        return mapper.writeValueAsString(buildAlbums(model, page.orElse(0), size.orElse(model.getAttribute("queryLimit").toString().toInt())))
    }

    private fun buildAlbums(model: Model, page: Int = 0, size: Int = model.getAttribute("queryLimit").toString().toInt()): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        val module = "albums"
        response["message"] = "Nothing to see here."
        response["albumsList"] =  mutableListOf<Album>()
        response["userAlbums"] = mutableListOf<UserAlbum>()
        response["userCount"] = 0
        response["totalImageCount"] = 0
        response["sharedAlbums"] = ArrayList<HashMap<String, Any>>()
        response["sharedAlbumsMap"] = HashMap<Int, Int>()
        response["albumsCommentsMap"] = mutableMapOf<Int, ArrayList<HashMap<String, Any>>>()
        response["page"] = page
        response["size"] = size

        var showControls = false
        var totalImageCount = 0

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            var userAlbums: MutableList<UserAlbum?>? = null
            if (currentUserObj.getAuthority() != null) {
                if (currentUserObj.getAuthority()!! == "ROLE_ADMIN" || currentUserObj.getAuthority()!! == "ROLE_SUPER") {
                    showControls = true
                    try {
                        userAlbums =
                            userAlbumRepository.findAllOffsetAndLimit((page * size), size) as MutableList<UserAlbum?>?
                    } catch (e: Exception) {
                        logger.log(Level.WARNING, "userAlbumRepository.findAllOffsetAndLimit error: ${e.message}")
                    }
                } else {
                    try {
                        userAlbums =
                            userAlbumRepository.findAllByUserIdAndOffsetAndLimit(
                                currentUserObj.getId(),
                                (page * size),
                                size
                            ) as MutableList<UserAlbum?>?
                    } catch (e: Exception) {
                        logger.log(Level.WARNING, "userAlbumRepository.findAllByUserIdAndOffsetAndLimit error: ${e.message}")
                    }
                }
            }

            if (userAlbums != null && userAlbums.count() > 0) {
                val albumsCommentsMap = HashMap<Int, ArrayList<HashMap<String, Any>>>()

                val albums = ArrayList<HashMap<String, Any>>()
                var albumVideoCount: Int?
                var albumPhotoCount: Int?

                for (userAlbum in userAlbums) {
                    val albumCommentsList = ArrayList<HashMap<String, Any>>()
                    if (userAlbum?.getAlbumId() != null) {
                        val albumMap = HashMap<String, Any>()
                        val albumObj = albumRepository.findById(userAlbum.getAlbumId()!!)
                        albumPhotoCount = albumPhotoRepository.countPhotosByAlbumId(userAlbum.getAlbumId()!!)

                        if (albumPhotoCount == null) {
                            albumPhotoCount = 0
                        }
                        totalImageCount += albumPhotoCount
                        albumVideoCount = albumPhotoRepository.countVideosByAlbumId(userAlbum.getAlbumId()!!)
                        if (albumVideoCount == null) {
                            albumVideoCount = 0
                        }

                        albumMap["id"] = albumObj.get().getId()
                        albumMap["name"] = if (albumObj.get().getName() == null) "" else albumObj.get().getName()!!
                        var coverUrl = ""
                        if (albumObj.get().getCoverUrl() != null) {
                            val metadata = metadataRepository.findByThumbnailCentered(albumObj.get().getCoverUrl().toString())
                            if (metadata != null) {
                                coverUrl = "/api/v1/thumbnails/centered/"+metadata.getId()
                            }
                        }
                        albumMap["coverUrl"] = coverUrl
                        albumMap["shareUrl"] = if (albumObj.get().getShareUrl() == null) "" else albumObj.get().getShareUrl()!!
                        albumMap["albumPhotoCount"] = albumPhotoCount
                        albumMap["albumVideoCount"] = albumVideoCount
                        albums.add(albumMap)

                        // Get comments for this album
                        val albumComments = commentRepository.findCommentsByAlbumId(albumObj.get().getId())
                        for (albumComment in albumComments) {
                            val albumCommentMap = HashMap<String, Any>()
                            albumCommentMap["comment"] = albumComment.getComment().toString()
                            albumCommentMap["commentId"] = albumComment.getCommentId().toString().toInt()
                            albumCommentMap["albumId"] = albumComment.getAlbumId().toString().toInt()
                            albumCommentMap["userId"] = albumComment.getUserId().toString().toInt()
                            albumCommentMap["userProfile"] =
                                if (albumComment.getUserProfile() == null) "" else albumComment.getUserProfile()
                                    .toString()
                            albumCommentMap["username"] = albumComment.getUsername().toString()
                            albumCommentMap["createdAt"] =
                                TextUtils.formatToLongDateWithTime(albumComment.getCreatedAt().toString())
                            albumCommentsList.add(albumCommentMap)
                        }
                        if (albumCommentsList.isNotEmpty()) {
                            albumsCommentsMap[albumObj.get().getId()] = albumCommentsList
                        }
                    }
                }

                if (albums.isNotEmpty()) {
                    response["albumsList"] = albums
                    response["albumsCommentsMap"] = albumsCommentsMap

                    if (currentUserObj.getAuthority()!! == "ROLE_ADMIN" || currentUserObj.getAuthority()!! == "ROLE_SUPER") {
                        val userCount = userRepository.count()

                        if (userCount > 1) {
                            response["userAlbums"] = userAlbumRepository.findAllByOrderByUserIdAsc()!!
                            response["userCount"] = userCount
                            val sharedAlbumsList = ArrayList<HashMap<String, Any>>()
                            val sharedAlbumsAlbumMap = HashMap<Int, Int>()

                            val sharedAlbums = userRepository.findUserBySharedAlbum(currentUserObj.getId())
                            for (sharedAlbum in sharedAlbums) {
                                val sharedAlbumsMap = HashMap<String, Any>()
                                sharedAlbumsMap["userId"] = sharedAlbum.getUserId().toString().toInt()
                                sharedAlbumsMap["albumId"] = sharedAlbum.getAlbumId().toString().toInt()
                                sharedAlbumsMap["username"] = sharedAlbum.getUsername().toString()
                                sharedAlbumsMap["isShared"] = sharedAlbum.getIsShared().toString().toInt()
                                if (!sharedAlbumsAlbumMap.containsKey(
                                        sharedAlbum.getAlbumId().toString().toInt()
                                    ) && sharedAlbum.getIsShared().toString().toInt() == 1
                                ) {
                                    sharedAlbumsAlbumMap[sharedAlbum.getAlbumId().toString().toInt()] =
                                        sharedAlbum.getIsShared().toString().toInt()
                                }

                                sharedAlbumsList.add(sharedAlbumsMap)
                            }
                            response["sharedAlbums"] = sharedAlbumsList
                            response["sharedAlbumsMap"] = sharedAlbumsAlbumMap
                        }
                    }
                    response["message"] = ""
                }
            }
        }

        response["totalImageCount"] = totalImageCount
        response["showControls"] = showControls
        response["msg"] = "Success!"
        response["status"] = ApiResponse.SUCCESS.status
        response["activePage"] = module
        response["activeSidebar"] = module
        response["titleDescriptor"] = TextUtils.capitalized(module)
        response["baseUrl"] = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()

        return response
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getSharedAlbumsApi",
            summary = "Get list of shared albums.",
            description = "<strong>Get list of shared albums.</strong>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/sharedalbums\" \\\n" +
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
                    "    \"userCount\": &lt;user_count&gt;,\n" +
                    "    \"userAlbums\": [\n" +
                    "        {\n" +
                    "            \"id\": \"&lt;user_albums_id&gt;,\"\n" +
                    "            \"userId\": &lt;user_id&gt;,\n" +
                    "            \"albumId\": &lt;album_id&gt;\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"sharedAlbums\": [\n" +
                    "        {\n" +
                    "            \"albumId\": &lt;album_id&gt;,\n" +
                    "            \"userId\": &lt;user_id&gt;,\n" +
                    "            \"username\": &lt;username&gt;,\n" +
                    "            \"isShared\": \"&lt;is_shared&gt;\"\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>userCount</td><td>int</td><td>Number of users</td></tr>" +
                    "<tr><td>userAlbums[].id</td><td>int</td><td>The user albums ID</td></tr>" +
                    "<tr><td>userAlbums[].userId</td><td>int</td><td>The user ID to share the album</td></tr>" +
                    "<tr><td>userAlbums[].albumId</td><td>int</td><td>The album ID to share the album</td></tr>" +
                    "<tr><td>sharedAlbums[].albumId</td><td>int</td><td>The album ID used for who to allow sharing with</td></tr>" +
                    "<tr><td>sharedAlbums[].userId</td><td>int</td><td>The user ID used for who to allow sharing with</td></tr>" +
                    "<tr><td>sharedAlbums[].username</td><td>string</td><td>The username used for who to allow sharing with</td></tr>" +
                    "<tr><td>sharedAlbums[].isShared</td><td>boolean</td><td>Flag of whether this album is shared with this user or not</td></tr>" +
                    "</tbody></table>"
        )
    )
    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/api/v1/sharedalbums","/sharedalbums"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getSharedAlbumsApi(model: Model): String {
        return mapper.writeValueAsString(buildSharedAlbumsList(model))
    }
    private fun buildSharedAlbumsList(model: Model): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()
        response["status"] = ""
        response["msg"] = "No results"

        val sharedAlbumsList = ArrayList<HashMap<String, Any>>()
        val currentUserObj = model.getAttribute("currentUser") as User?

        if (currentUserObj != null) {
            val userCount = userRepository.count()
            if (userCount > 1) {
                response["userAlbums"] = userAlbumRepository.findAllByOrderByUserIdAsc()!!
                response["userCount"] = userCount

                val sharedAlbums = userRepository.findUserBySharedAlbum(currentUserObj.getId())
                response["status"] = "Success"
                response["msg"] = "Results"

                for (sharedAlbum in sharedAlbums) {
                    val sharedAlbumsMap = HashMap<String, Any>()
                    sharedAlbumsMap["userId"] = sharedAlbum.getUserId().toString().toInt()
                    sharedAlbumsMap["albumId"] = sharedAlbum.getAlbumId().toString().toInt()
                    sharedAlbumsMap["username"] = sharedAlbum.getUsername().toString()
                    sharedAlbumsMap["isShared"] = sharedAlbum.getIsShared().toString().toInt()
                    sharedAlbumsList.add(sharedAlbumsMap)
                }
            }
        }
        response["sharedAlbums"] = sharedAlbumsList

        return response
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getAlbumCommentsApi",
            summary = "Get comments for an album.",
            description = "<strong>Get comments for an album.</strong>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/albumcomments/{albumId}\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>albumId</td><td>param</td><td>int</td><td>required</td><td>Comments associated with this album Id</td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"msg\": \"\",\n" +
                    "    \"albumCommentsList\": [" +
                    "        {\n" +
                    "            \"comment\": \"&lt;comment&gt;\",\n" +
                    "            \"commentId\": &lt;comment_id&gt;,\n" +
                    "            \"userId\": &lt;user_id&gt;,\n" +
                    "            \"albumId\": &lt;album_id&gt;,\n" +
                    "            \"username\": \"&lt;username&gt;\"\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"status\": \"success\"\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>albumCommentsList[].comment</td><td>string</td><td>The comment</td></tr>" +
                    "<tr><td>albumCommentsList[].commentId</td><td>int</td><td>The comment ID</td></tr>" +
                    "<tr><td>albumCommentsList[].albumId</td><td>int</td><td>The album ID for the album the user commented on</td></tr>" +
                    "<tr><td>albumCommentsList[].userId</td><td>int</td><td>The user ID of the person who commented</td></tr>" +
                    "<tr><td>albumCommentsList[].username</td><td>string</td><td>The username of the person who commented</td></tr>" +
                    "</tbody></table>"
        )
    )
    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/api/v1/albumcomments/{albumId}","/albumcomments/{albumId}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getAlbumCommentsApi(@PathVariable albumId: Int): String {
        return mapper.writeValueAsString(buildAlbumComments(albumId))
    }
    private fun buildAlbumComments(albumId: Int): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()
        response["status"] = ApiResponse.FAIL.status
        response["msg"] = "No results"
        val albumCommentsList = ArrayList<HashMap<String, Any>>()

        // Get comments for this album
        val albumComments = commentRepository.findCommentsByAlbumId(albumId)
        for (albumComment in albumComments) {
            val albumCommentMap = HashMap<String, Any>()
            albumCommentMap["comment"] = albumComment.getComment().toString()
            albumCommentMap["commentId"] = albumComment.getCommentId().toString().toInt()
            albumCommentMap["albumId"] = albumComment.getAlbumId().toString().toInt()
            albumCommentMap["userId"] = albumComment.getUserId().toString().toInt()
            albumCommentMap["userProfile"] = if (albumComment.getUserProfile()==null) "" else albumComment.getUserProfile().toString()
            albumCommentMap["username"] = albumComment.getUsername().toString()
            albumCommentMap["createdAt"] = TextUtils.formatToLongDateWithTime(albumComment.getCreatedAt().toString())
            albumCommentsList.add(albumCommentMap)
        }

        response["status"] = ApiResponse.SUCCESS.status
        response["msg"] = ""

        response["albumCommentsList"] = albumCommentsList

        return response
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "deleteAlbumPhotos",
            summary = "Delete album.",
            description = "<strong>Delete album.</strong>" +
                    "<pre><code>" +
                    "curl -X DELETE \"http://127.0.0.1:6624/api/v1/all/album/delete\" \\\n" +
                    "-H \"Content-Type: application/json\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\" \\\n" +
                    "-d '{\"albumId\": &lt;album_id&gt;}'" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>Content-Type</td><td>header</td><td>string</td><td>required</td><td>application/json</td></tr>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>albumId</td><td>body param</td><td>int</td><td>required</td><td>Save a comment for this album ID and media</td></tr>" +
                    "</tbody></table><br>"
        )
    )
    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/album/delete", "/api/v1/all/album/delete"], method = [RequestMethod.DELETE], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun deleteAlbumPhotos(model: Model, @RequestBody requestBody: JsonNode, response: HttpServletResponse): String? {
        val albumDeleteMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})

        if (albumDeleteMap.containsKey("albumId")) {
            val albumIdRequest = albumDeleteMap["albumId"].toString().toInt()
            val albumObj = albumRepository.findAlbumById(albumIdRequest)

//            val currentUserObj = model.getAttribute("currentUser") as User?
//            val userAlbumCount = userAlbumRepository.countByUserIdAndAlbumId(currentUserObj?.getId(), albumIdRequest)
            val userAlbumCount = userAlbumRepository.countByAlbumId(albumIdRequest)

            if (userAlbumCount != null && userAlbumCount > 0) {
                userAlbumRepository.deleteByAlbumId(albumIdRequest)
                albumPhotoRepository.deleteByAlbumId(albumIdRequest)
                albumRepository.deleteById(albumIdRequest)

                val admins = userRepository.findAllAdmins()

                if (albumObj != null && admins.count() > 0) {
                    val notificationObjList = mutableListOf<Notification>()
                    val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                    sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
                    for (admin in admins) {
                        val notificationObj = Notification()
                        notificationObj.setUserId(admin.getId())
                        notificationObj.setCreatedAt(getCurrentTimestamp())
                        notificationObj.setModifiedAt(getCurrentTimestamp())
                        notificationObj.setRead(false)
                        notificationObj.setMessage("Album ${albumObj.getName()} deleted at ${sdtf.format(Date())}")
                        notificationObjList.add(notificationObj)
                    }
                    if (notificationObjList.isNotEmpty()) {
                        notificationRepository.saveAll(notificationObjList)
                    }
                }

                // Delete comments
                val albumComments = albumCommentRepository.findAllByAlbumId(albumIdRequest)
                if (albumComments != null) {
                    val commentIdList = ArrayList<Int>()
                    for (albumComment in albumComments) {
                        if (albumComment != null && albumComment.getCommentId() !in commentIdList) {
                            commentIdList.add(albumComment.getCommentId()!!)
                        }
                    }

                    if (commentIdList.count() > 0) {
                        commentRepository.deleteAllById(commentIdList)
                        albumCommentRepository.deleteByAlbumId(albumIdRequest)
                        albumPhotoCommentRepository.deleteByAlbumId(albumIdRequest)
                    }

                    resp["msg"] = "Success!"
                    resp["status"] = ApiResponse.SUCCESS.status
                    return mapper.writeValueAsString(resp)
                }
            } else {
                return returnForbiddenError(response)
            }
        }

        resp["msg"] = "Could not save"
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/album/media/upload/batch/{albumId}","/api/v1/album/media/upload/batch/{albumId}"], method = [RequestMethod.POST], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE], produces = ["application/json"])
    @ResponseBody
    fun postUploadToAlbum(model: Model, @PathVariable albumId: Int, @RequestParam("files[]") media: List<MultipartFile>): String {
        resp["msg"] = "Could not save"
        resp["status"] = ApiResponse.FAIL.status

        val hasMediaUploadDirectory = model.getAttribute("hasMediaUploadDirectory") as Boolean?

        val settings = model.getAttribute("settings") as Settings?

        val currentUserObj = model.getAttribute("currentUser") as User?

        if (currentUserObj != null && !media.isEmpty() && albumId > 0 && hasMediaUploadDirectory != null && hasMediaUploadDirectory && !settings?.getUploadMediaDirectory().isNullOrBlank()) {
            val fileUploadedMap = FileUtils.copyMultipartFiles(media, settings)
            val uploadedFiles = fileUploadedMap["uploadedFiles"] as MutableList<String>
            val notUploadedFiles = fileUploadedMap["notUploadedFiles"] as MutableList<String>

            if (!uploadedFiles.isEmpty()) {
                settingsController.scanMediaDirectories(false, albumId, currentUserObj.getId())
            }

            if (!notUploadedFiles.isEmpty() && !uploadedFiles.isEmpty()) {
                resp["msg"] = "Some items not saved to album - ${notUploadedFiles.joinToString(", ")}. Check file formats."
            } else if (!notUploadedFiles.isEmpty() && uploadedFiles.isEmpty()) {
                resp["msg"] = "Items not saved to album. Check file formats."
            } else if (!uploadedFiles.isEmpty()) {
                resp["msg"] = "Saved to album. Processing files."
            } else {
                resp["msg"] = "Saved to album. Processing files."
            }
            resp["status"] = ApiResponse.SUCCESS.status
        }

        return mapper.writeValueAsString(resp)
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/album/media/delete/batch"], method = [RequestMethod.DELETE], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun deleteAlbumPhotos(@RequestBody requestBody: JsonNode): String? {
        val batchMetadataMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (batchMetadataMap.containsKey("metadataIdList") && batchMetadataMap.containsKey("albumId")) {
            val idArray = batchMetadataMap["metadataIdList"] as ArrayList<String>
            val albumId = batchMetadataMap["albumId"].toString().toInt()
            val albumObj = albumRepository.findAlbumById(albumId)

            for (metadataId in idArray) {
                MetadataProcessing.deleteAlbumPhoto(metadataRepository, albumRepository, albumPhotoRepository, metadataId, albumId)
            }

            val count = MetadataProcessing.deleteAlbum(
                albumRepository,
                albumPhotoRepository,
                userAlbumRepository,
                commentRepository,
                albumPhotoCommentRepository,
                albumCommentRepository,
                albumId
            )

            if (count!! > 0) {
                val admins = userRepository.findAllAdmins()

                if (albumObj != null && admins.count() > 0) {
                    val notificationObjList = mutableListOf<Notification>()
                    val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                    sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
                    for (admin in admins) {
                        val notificationObj = Notification()
                        notificationObj.setUserId(admin.getId())
                        notificationObj.setCreatedAt(getCurrentTimestamp())
                        notificationObj.setModifiedAt(getCurrentTimestamp())
                        notificationObj.setRead(false)
                        notificationObj.setMessage("Album ${albumObj.getName()} deleted at ${sdtf.format(Date())}")
                        notificationObjList.add(notificationObj)
                    }
                    if (notificationObjList.isNotEmpty()) {
                        notificationRepository.saveAll(notificationObjList)
                    }
                }
            }

            if (count.toInt() == 0) {
                resp["msg"] = "/albums"
                resp["status"] = "redirect"
                return mapper.writeValueAsString(resp)
            }

            resp["msg"] = "Saved!"
            resp["status"] = ApiResponse.SUCCESS.status
            return mapper.writeValueAsString(resp)
        }

        resp["msg"] = "Could not save"
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/album/update"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun updateAlbum(@RequestBody requestBody: JsonNode): String? {
        val albumOptionsMapper = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (albumOptionsMapper.containsKey("setCoverAlbum") &&
            albumOptionsMapper.containsKey("metadataId") &&
            albumOptionsMapper.containsKey("albumId")
        ) {
            val albumId = albumOptionsMapper["albumId"].toString().toInt()
            val metadataId = StringEscapeUtils.escapeHtml4(albumOptionsMapper["metadataId"].toString())
            val setCoverAlbum = albumOptionsMapper["setCoverAlbum"].toString().toBoolean()

            if (setCoverAlbum) {
                val metadataObj = metadataRepository.findById(metadataId)
                val coverAlbumUrl = metadataObj.get().getThumbnailUrlCentered()
                val album = albumRepository.findById(albumId)
                album.get().setCoverUrl(coverAlbumUrl)
                album.get().setModifiedAt(getCurrentTimestamp())
                albumRepository.save(album.get())

                logger.log(
                    Level.INFO,
                    "Set the album cover in /album/update"
                )
            }

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
            operationId = "postAnonymousShareAlbum",
            summary = "Save and generate a sharable public URL.",
            description = "<strong>Save and generate a sharable public URL.</strong> This can be a user generated share link eg. http://127.0.0.1:6624/share/abcd/album/1" +
                    "<pre><code>" +
                    "curl -X POST \"http://127.0.0.1:6624/api/v1/share/album/save\" \\\n" +
                    "-H \"Content-Type: application/json\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\" \\\n" +
                    "-d '{\"albumId\": &lt;album_id&gt;, \"relativeShareUrl\": \"&lt;relative_share_url&gt;\"}'" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>Content-Type</td><td>header</td><td>string</td><td>required</td><td>application/json</td></tr>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>albumId</td><td>body param</td><td>int</td><td>required</td><td>Album Id for the album to share</td></tr>" +
                    "<tr><td>relativeShareUrl</td><td>body param</td><td>string</td><td>required</td><td>The relative share URL you'd like to save</td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"msg\": \"\",\n" +
                    "    \"relativeShareUrl\": \"&lt;relative_share_url&gt;\",\n" +
                    "    \"status\": \"success\"\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>relativeShareUrl</td><td>string</td><td>The saved relative URL that can be used to share the album</td></tr>" +
                    "</tbody></table>"
        )
    )
    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/share/album/save","/api/v1/share/album/save"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    //@Transactional
    fun postAnonymousShareAlbum(@RequestBody requestBody: JsonNode, response: HttpServletResponse): String? {
        val albumShareInfo = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (albumShareInfo.containsKey("albumId") && albumShareInfo.containsKey("relativeShareUrl")) {
            val albumIdRequest = albumShareInfo["albumId"].toString().toInt()
            var relativeShareUrl: String? =
                StringEscapeUtils.escapeHtml4(albumShareInfo["relativeShareUrl"].toString().trim())

            if (albumIdRequest > 0) {
                val admins = userRepository.findAllAdmins()
                val notificationObjList = mutableListOf<Notification>()
                val albumObj = albumRepository.findById(albumIdRequest)
                if (albumObj.isPresent && albumObj.get().getId() == albumIdRequest) {
                    resp["msg"] = "Share link generated"
                    if (relativeShareUrl != null && relativeShareUrl.isEmpty()) {
                        relativeShareUrl = null
                        resp["msg"] = "Share link cleared"
                    }
                    albumObj.get().setShareUrl(relativeShareUrl)
                    albumRepository.save(albumObj.get())

                    var action = "<a href='/share/$relativeShareUrl/album/$albumIdRequest' target='_blank'>generated</a>"
                    if (relativeShareUrl == null) {
                        action = "cleared"
                    }

                    var coverUrl = ""
                    if (albumObj.get().getCoverUrl() != null) {
                        val metadata = metadataRepository.findByThumbnailCentered(albumObj.get().getCoverUrl().toString())
                        if (metadata != null) {
                            coverUrl = "/api/v1/thumbnails/centered/"+metadata.getId()
                        }
                    }
                    for (admin in admins) {
                        val notificationObj = Notification()
                        notificationObj.setImageUrl(coverUrl)
                        notificationObj.setUserId(admin.getId())
                        notificationObj.setCreatedAt(getCurrentTimestamp())
                        notificationObj.setModifiedAt(getCurrentTimestamp())
                        notificationObj.setRead(false)
                        notificationObj.setMessage("Share URL was $action for album '<a href='/album/$albumIdRequest' target='_blank'>${albumObj.get().getName()}</a>'")
                        notificationObjList.add(notificationObj)
                    }

                    if (notificationObjList.isNotEmpty()) {
                        notificationRepository.saveAll(notificationObjList)
                    }

                    resp["relativeShareUrl"] = relativeShareUrl
                    resp["status"] = ApiResponse.SUCCESS.status
                    return mapper.writeValueAsString(resp)
                } else {
                    return returnForbiddenError(response)
                }
            }
        }

        resp["relativeShareLink"] = ""
        resp["msg"] = "Could not generate link"
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/share/{shareLink}/album/{albumId}"], method = [RequestMethod.GET])
    fun getAnonymousShareAlbum(model: Model, res: HttpServletResponse, @PathVariable shareLink: String, @PathVariable albumId: Int): String? {
        val module = "share"

        val queryLimit = model.getAttribute("queryLimit").toString().toInt()
        val response = buildShareData(albumId,shareLink, queryLimit, 0)

        if (response["status"] === ApiResponse.SUCCESS.status) {
            val userIp = model.getAttribute("clientIP").toString()
            val admins = userRepository.findAllAdmins()

            val album = response["album"] as Album?

            if (!TextUtils.isLocalIp(userIp)) {
                val notificationObjList = mutableListOf<Notification>()
                val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())

                var coverUrl = ""
                if (album != null && album.getCoverUrl() != null) {
                    val metadata = metadataRepository.findByThumbnailCentered(album.getCoverUrl().toString())
                    if (metadata != null) {
                        coverUrl = "/api/v1/thumbnails/centered/"+metadata.getId()
                    }
                }

                for (admin in admins) {
                    val notificationObj = Notification()
                    notificationObj.setImageUrl(coverUrl)
                    notificationObj.setUserId(admin.getId())
                    notificationObj.setCreatedAt(getCurrentTimestamp())
                    notificationObj.setModifiedAt(getCurrentTimestamp())
                    notificationObj.setRead(false)
                    var message =
                        "IP <a href='https://ipgeolocation.io/ip-location/$userIp' target='_blank'>$userIp</a> viewed shared album '<a href='/share/$shareLink/album/$albumId' target='_blank'>${album?.getName()}</a>' at ${
                            sdtf.format(Date())
                        }"
                    if (album == null || album.getId() == 0) {
                        message =
                            "IP <a href='https://ipgeolocation.io/ip-location/$userIp' target='_blank'>$userIp</a> tried to access non existent shared album at shareLink <strong>$shareLink</strong> and albumId <strong>$albumId</strong> at ${
                                sdtf.format(Date())
                            }"
                    }
                    notificationObj.setMessage(message)
                    notificationObjList.add(notificationObj)
                }

                if (notificationObjList.isNotEmpty()) {
                    notificationRepository.saveAll(notificationObjList)
                }
            }

            model["pageParam"] = 0
            model["album"] = response["album"]!!
            model["albumMetadataList"] = response["albumMetadataList"]!!
            model["albumMetadataSize"] = response["albumMetadataSize"]!!
            model["totalPages"] = response["totalPages"]!!
            model["shareLink"] = response["shareLink"]!!
            model["message"] = response["message"]!!
            model["msg"] = response["msg"]!!
            model["status"] = response["status"]!!
            val currentUserObj = model.getAttribute("currentUser") as User?
            model["darkMode"] = false
            if (currentUserObj != null) {
                model["darkMode"] = currentUserObj.getDarkMode()!!
            }

            model["activePage"] = module
            model["activeSidebar"] = module
            if (album?.getName() != null) {
                model["titleDescriptor"] = album.getName() as String
            } else {
                model["titleDescriptor"] = TextUtils.capitalized(module)
            }
        } else {
            for ((k, v) in response) {
                model[k] = v!!
            }
            model["message"] = "Resource not found."
        }

        return module
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getPagedAnonymousShareAlbum",
            summary = "Get paged results for shared album content.",
            description = "<strong>Get paged results for shared album content.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/share/{shareLink}/album/{albumId}/page/{page}\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>shareLink</td><td>param</td><td>string</td><td>required</td><td>The relative share URL you'd like to save</td></tr>" +
                    "<tr><td>albumId</td><td>param</td><td>int</td><td>required</td><td>The album ID</td></tr>" +
                    "<tr><td>page</td><td>param</td><td>int</td><td>required</td><td>page number of results to return used for pagination. Page index starts from 0. The default query/page size is 20. Admins can set the query/page size in the <a href=\"/settings\">settings</a></td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"msg\": \"\",\n" +
                    "    \"message\": \"\",\n" +
                    "    \"shareLink\": \"&lt;relative_share_url&gt;\",\n" +
                    "    \"album\": {\n" +
                    "        \"id\": &lt;album_id&gt;,\n" +
                    "        \"name\": \"&lt;name_of_album&gt;\",\n" +
                    "        \"coverUrl\": \"&lt;relative_url&gt;\",\n" +
                    "        \"shareUrl\": \"&lt;public_url_key&gt;\"\n" +
                    "    },\n" +
                    "    \"albumMetadataList\": [\n" +
                    "        {\n" +
                    "           &lt;metadata&gt;\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>shareLink</td><td>string</td><td>The saved relative URL that can be used to share the album</td></tr>" +
                    "<tr><td>album.id</td><td>int</td><td>The album ID</td></tr>" +
                    "<tr><td>album.name</td><td>string</td><td>The album name</td></tr>" +
                    "<tr><td>album.coverUrl</td><td>string</td><td>Relative URL for the album cover image</td></tr>" +
                    "<tr><td>album.shareUrl</td><td>string</td><td>Part of the share URL endpoint for public sharing</td></tr>" +
                    "<tr><td>albumMetadataList[].metadata</td><td>object</td><td>A <a href=\"#\" data-bs-toggle=\"modal\" data-bs-target=\"#propMetadataDocs\">Metadata</a> object</td></tr>" +
                    "</tbody></table>"
        )
    )
    @RequestMapping(value = ["/share/{shareLink}/album/{albumId}/page/{page}", "/api/v1/share/{shareLink}/album/{albumId}/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedAnonymousShareAlbum(model: Model, @PathVariable shareLink: String, @PathVariable albumId: Int, @PathVariable page: Int): String? {
        val queryLimit = model.getAttribute("queryLimit").toString().toInt()
        val response = buildShareData(albumId,StringEscapeUtils.escapeHtml4(shareLink), queryLimit, page)
        return mapper.writeValueAsString(response)
    }

    @RequestMapping(value = ["/share/{shareLink}/album/{albumId}/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    fun getPaginationAnonymousShareAlbum(model: Model, @PathVariable shareLink: String, @PathVariable albumId: Int, @PathVariable page: Int): String? {
        val queryLimit = model.getAttribute("queryLimit").toString().toInt()
        val response = buildShareData(albumId,StringEscapeUtils.escapeHtml4(shareLink), queryLimit, page)

        for ((k, v) in response) {
            model[k] = v!!
        }

        val module = "share"

        model["currentPage"] = (page+1)
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getPagedSizeAnonymousShareAlbum",
            summary = "Get results for shared album content.",
            description = "<strong>Get results for shared album content.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/share/{shareLink}/album/{albumId}?page={page}&size={size}\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>shareLink</td><td>param</td><td>string</td><td>required</td><td>The relative share URL you'd like to save</td></tr>" +
                    "<tr><td>albumId</td><td>param</td><td>int</td><td>required</td><td>The album ID</td></tr>" +
                    "<tr><td>page</td><td>param</td><td>int</td><td>optional</td><td>page number of results to return used for pagination. Default is 0.</td></tr>" +
                    "<tr><td>size</td><td>param</td><td>int</td><td>optional</td><td>The default query/page size is 20. Admins can set the default query/page size in the <a href=\"/settings\">settings</a></td></tr>" +                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"msg\": \"\",\n" +
                    "    \"message\": \"\",\n" +
                    "    \"shareLink\": \"&lt;relative_share_url&gt;\",\n" +
                    "    \"album\": {\n" +
                    "        \"id\": &lt;album_id&gt;,\n" +
                    "        \"name\": \"&lt;name_of_album&gt;\",\n" +
                    "        \"coverUrl\": \"&lt;relative_url&gt;\",\n" +
                    "        \"shareUrl\": \"&lt;public_url_key&gt;\"\n" +
                    "    },\n" +
                    "    \"albumMetadataList\": [\n" +
                    "        {\n" +
                    "           &lt;metadata&gt;\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>shareLink</td><td>string</td><td>The saved relative URL that can be used to share the album</td></tr>" +
                    "<tr><td>album.id</td><td>int</td><td>The album ID</td></tr>" +
                    "<tr><td>album.name</td><td>string</td><td>The album name</td></tr>" +
                    "<tr><td>album.coverUrl</td><td>string</td><td>Relative URL for the album cover image</td></tr>" +
                    "<tr><td>album.shareUrl</td><td>string</td><td>Part of the share URL endpoint for public sharing</td></tr>" +
                    "<tr><td>albumMetadataList[].metadata</td><td>object</td><td>A <a href=\"#\" data-bs-toggle=\"modal\" data-bs-target=\"#propMetadataDocs\">Metadata</a> object</td></tr>" +
                    "</tbody></table>"
        )
    )
    @RequestMapping(value = ["/api/v1/share/{shareLink}/album/{albumId}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedSizeAnonymousShareAlbum(model: Model, @PathVariable shareLink: String, @PathVariable albumId: Int, @RequestParam page: Optional<Int>, @RequestParam size: Optional<Int>): String? {
        val response = buildShareData(albumId,StringEscapeUtils.escapeHtml4(shareLink), size.orElse(model.getAttribute("queryLimit").toString().toInt()), page.orElse(0))
        return mapper.writeValueAsString(response)
    }

    private fun buildShareData(albumId: Int,shareLink: String, size: Int, page: Int): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()
        response["message"] = "Nothing to see here."
        val tempAlbum = Album()
        tempAlbum.setId(0)
        response["album"] = tempAlbum
        response["totalPages"] = 0
        response["albumMetadataList"] = mutableListOf<Metadata>()
        response["albumMetadataSize"] = 0
        response["shareLink"] = ""
        response["page"] = page
        response["size"] = size
        response["msg"] = "No results"
        response["status"] = ApiResponse.FAIL.status

        val photoObj = albumRepository.findById(albumId)
        if (photoObj.isPresent && photoObj.get().getShareUrl() == shareLink) {
            val resultPage = page*size
            val albumTotalCount = albumPhotoRepository.countAlbumId(albumId)
            val albumPhotos = albumPhotoRepository.findAllByAlbumIdAndOffsetAndLimit(albumId, resultPage, size)
            val albumMetadataList = ArrayList<Metadata>()
            if (albumPhotos != null) {
                for (albumPhoto in albumPhotos) {
                    if (albumPhoto != null) {
                        val metadata = metadataRepository.findById(albumPhoto.getMetadataId()!!)
                        albumMetadataList.add(metadata.get())
                    }
                }

                if (albumMetadataList.isNotEmpty()) {
                    val album = albumRepository.findById(albumId)
                    response["totalPages"] = albumTotalCount?.div(size)
                    response["message"] = ""
                    response["album"] = album.get()
                    response["albumMetadataList"] = albumMetadataList
                    response["albumMetadataSize"] = albumMetadataList.size
                    response["shareLink"] = shareLink
                    response["msg"] = "Results"
                    response["status"] = ApiResponse.SUCCESS.status
                }
            }
        }

        return response
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/album/metadata/list/{albumId}/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getAlbumMetadataList(model: Model, @PathVariable albumId: Int,@PathVariable page: Int): String? {
        val response = mutableMapOf<String, Any?>()
        response["msg"] = "No Results"
        response["status"] = ApiResponse.FAIL.status
        response["albumMetadataList"] = ArrayList<Metadata>()
        val size: Int = model.getAttribute("queryLimit") as Int
        val resultPage = page * size

        val albumPhotos = albumPhotoRepository.findAllByAlbumIdAndOffsetAndLimit(albumId, resultPage, size)
        val albumMetadataList = ArrayList<Metadata>()
        if (albumPhotos != null) {
            for (albumPhoto in albumPhotos) {
                if (albumPhoto != null) {
                    val metadata = metadataRepository.findById(albumPhoto.getMetadataId()!!)
                    albumMetadataList.add(metadata.get())
                }
            }

            if (albumMetadataList.isNotEmpty()) {
                response["albumMetadataList"] = albumMetadataList
                response["msg"] = "Results"
                response["status"] = ApiResponse.SUCCESS.status
            }
        }

        return mapper.writeValueAsString(response)
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/album/share/{albumId}"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun shareAlbum(@RequestBody requestBody: JsonNode, @PathVariable albumId: Int): String? {
        val shareAlbum = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (shareAlbum.containsKey("albumId") && shareAlbum.containsKey("userShareMap")) {
            val userMapObj = mapper.readTree(shareAlbum["userShareMap"].toString())
            val userMap = mapper.convertValue(userMapObj, object : TypeReference<Map<String, Boolean>>() {})
            val shareAlbumId = shareAlbum["albumId"].toString().toInt()
            val userAlbumList = mutableListOf<UserAlbum>()
            val notificationObjList = mutableListOf<Notification>()
            val deleteUserAlbumList = mutableListOf<UserAlbum>()
            val albumObj = albumRepository.findAlbumById(albumId)
            val admins = userRepository.findAllAdmins()
            val userList = mutableListOf<String>()

            var coverUrl = ""
            if (albumObj != null && albumObj.getCoverUrl() != null) {
                val metadata = metadataRepository.findByThumbnailCentered(albumObj.getCoverUrl().toString())
                if (metadata != null) {
                    coverUrl = "/api/v1/thumbnails/centered/"+metadata.getId()
                }
            }

            for ((userId, share) in userMap) {
                if (share) {
                    val countUserAlbum = userAlbumRepository.countByUserIdAndAlbumId(userId.toInt(), albumId)
                    if (countUserAlbum == 0) {
                        val userObj = userRepository.findById(userId.toInt())

                        val userAlbumObj = UserAlbum()
                        userAlbumObj.setUserId(userId.toInt())
                        userAlbumObj.setAlbumId(shareAlbumId)
                        userAlbumObj.setCreatedAt(getCurrentTimestamp())
                        userAlbumObj.setModifiedAt(getCurrentTimestamp())
                        userAlbumList.add(userAlbumObj)

                        val notificationObj = Notification()
                        notificationObj.setImageUrl(coverUrl)
                        notificationObj.setUserId(userId.toInt())
                        notificationObj.setCreatedAt(getCurrentTimestamp())
                        notificationObj.setModifiedAt(getCurrentTimestamp())
                        notificationObj.setRead(false)
                        notificationObj.setMessage("Album '<a href='/album/$shareAlbumId' target='_blank'>${albumObj?.getName()}</a>' was shared with you.")
                        notificationObjList.add(notificationObj)

                        userList.add(userObj.get().getUsername()!!)
                    }
                } else {
                    val userAlbumObj = userAlbumRepository.findDistinctByUserIdAndAlbumId(userId.toInt(),shareAlbumId)
                    if (userAlbumObj != null) {
                        deleteUserAlbumList.add(userAlbumObj)
                    }
                }
            }

            if (userList.size > 0) {
                val userListString = userList.joinToString(",")
                var coverUrl = ""
                if (albumObj != null && albumObj.getCoverUrl() != null) {
                    val metadata = metadataRepository.findByThumbnailCentered(albumObj.getCoverUrl().toString())
                    if (metadata != null) {
                        coverUrl = "/api/v1/thumbnails/centered/"+metadata.getId()
                    }
                }

                for (admin in admins) {
                    val notificationObj = Notification()
                    notificationObj.setImageUrl(coverUrl)
                    notificationObj.setUserId(admin.getId())
                    notificationObj.setCreatedAt(getCurrentTimestamp())
                    notificationObj.setModifiedAt(getCurrentTimestamp())
                    notificationObj.setRead(false)
                    notificationObj.setMessage("Album '<a href='/album/$shareAlbumId' target='_blank'>${albumObj?.getName()}</a>' was shared with users $userListString")
                    notificationObjList.add(notificationObj)
                }
            }

            if (notificationObjList.count() > 0) {
                notificationRepository.saveAll(notificationObjList)
            }
            if (userAlbumList.count() > 0) {
                userAlbumRepository.saveAll(userAlbumList)
            }
            if (deleteUserAlbumList.count() > 0) {
                userAlbumRepository.deleteAll(deleteUserAlbumList)
            }

            resp["msg"] = "Shared!"
            resp["status"] = ApiResponse.SUCCESS.status
            return mapper.writeValueAsString(resp)
        }

        resp["msg"] = "Could not save"
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/album/{albumId}","/album/{albumId}/{mediaType}"], method = [RequestMethod.GET])
    fun getAlbum(model: Model, @PathVariable albumId: Int,@PathVariable(required = false) mediaType: String?): String {
        val response = buildAlbum(model,albumId,0,model.getAttribute("queryLimit").toString().toInt(),mediaType)
        for ((k, v) in response) {
            model[k] = v!!
        }
        model["currentUser"] = model.getAttribute("currentUser") as User

        getAllAttributeData(model)

        model["sharedAlbumUsers"] = userRepository.findAllUserBySharedAlbum(albumId)
        model["pageParam"] = 0

        return model.getAttribute("activePage").toString()
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/album/{albumId}/{page}/{mediaType}"], method = [RequestMethod.GET])
    fun getAlbumsPaged(model: Model,@PathVariable(required = true) albumId: Int,@PathVariable(required = true) page: Int,@PathVariable(required = true) mediaType: String): String {
        val response = buildAlbum(model,albumId,page,model.getAttribute("queryLimit").toString().toInt(),mediaType)

        for ((k, v) in response) {
            model[k] = v!!
        }

        val module = response["activePage"].toString()

        model["currentPage"] = (page+1)
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getPagedAlbum",
            summary = "Get paged results for album content.",
            description = "<strong>Get paged results for album content.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/album/{albumId}/page/{page}\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>albumId</td><td>param</td><td>int</td><td>required</td><td>The album ID</td></tr>" +
                    "<tr><td>page</td><td>param</td><td>int</td><td>required</td><td>page number of results to return used for pagination. Page index starts from 0. The default query/page size is 20. Admins can set the query/page size in the <a href=\"/settings\">settings</a></td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"msg\": \"\",\n" +
                    "    \"message\": \"\",\n" +
                    "    \"status\": \"success\",\n" +
                    "    \"canEdit\": &lt;can_edit&gt;,\n" +
                    "    \"albumId\": \"&lt;album_id&gt;\",\n" +
                    "    \"album\": {\n" +
                    "        \"id\": &lt;album_id&gt;,\n" +
                    "        \"name\": \"&lt;name_of_album&gt;\",\n" +
                    "        \"coverUrl\": \"&lt;relative_url&gt;\",\n" +
                    "        \"shareUrl\": \"&lt;public_url_key&gt;\"\n" +
                    "    },\n" +
                    "    \"albumMetadataList\": [\n" +
                    "        {\n" +
                    "           &lt;metadata&gt;\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>canEdit</td><td>boolean</td><td>Authorized to edit the album or not</td></tr>" +
                    "<tr><td>albumId</td><td>int</td><td>The album ID</td></tr>" +
                    "<tr><td>album.id</td><td>int</td><td>The album ID</td></tr>" +
                    "<tr><td>album.name</td><td>string</td><td>The album name</td></tr>" +
                    "<tr><td>album.coverUrl</td><td>string</td><td>Relative URL for the album cover image</td></tr>" +
                    "<tr><td>album.shareUrl</td><td>string</td><td>Part of the share URL endpoint for public sharing</td></tr>" +
                    "<tr><td>albumMetadataList[].metadata</td><td>object</td><td>A <a href=\"#\" data-bs-toggle=\"modal\" data-bs-target=\"#propMetadataDocs\">Metadata</a> object</td></tr>" +
                    "</tbody></table>"
        )
    )
    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/album/{albumId}/page/{page}","/api/v1/album/{albumId}/page/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedAlbum(model: Model, @PathVariable albumId: Int, @PathVariable page: Int): String {
        return mapper.writeValueAsString(buildAlbum(model,albumId,page,model.getAttribute("queryLimit").toString().toInt(),"all"))
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/album/{albumId}/mediatype/{mediaType}/page/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedAlbumWithMediaType(model: Model, @PathVariable albumId: Int, @PathVariable page: Int,@PathVariable mediaType: String): String {
        return mapper.writeValueAsString(buildAlbum(model,albumId,page,model.getAttribute("queryLimit").toString().toInt(),mediaType))
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getPagedSizeAlbum",
            summary = "Get paged results for album content by specifying the page and size.",
            description = "<strong>Get paged results for album content by specifying the page and size.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/album/{albumId}?page={page}&size={size}\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>albumId</td><td>param</td><td>int</td><td>required</td><td>The album ID</td></tr>" +
                    "<tr><td>page</td><td>param</td><td>int</td><td>optional</td><td>page number of results to return used for pagination. Default is 0.</td></tr>" +
                    "<tr><td>size</td><td>param</td><td>int</td><td>optional</td><td>The default query/page size is 20. Admins can set the default query/page size in the <a href=\"/settings\">settings</a></td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"msg\": \"\",\n" +
                    "    \"message\": \"\",\n" +
                    "    \"status\": \"success\",\n" +
                    "    \"canEdit\": &lt;can_edit&gt;,\n" +
                    "    \"albumId\": \"&lt;album_id&gt;\",\n" +
                    "    \"album\": {\n" +
                    "        \"id\": &lt;album_id&gt;,\n" +
                    "        \"name\": \"&lt;name_of_album&gt;\",\n" +
                    "        \"coverUrl\": \"&lt;relative_url&gt;\",\n" +
                    "        \"shareUrl\": \"&lt;public_url_key&gt;\"\n" +
                    "    },\n" +
                    "    \"albumMetadataList\": [\n" +
                    "        {\n" +
                    "           &lt;metadata&gt;\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>canEdit</td><td>boolean</td><td>Authorized to edit the album or not</td></tr>" +
                    "<tr><td>albumId</td><td>int</td><td>The album ID</td></tr>" +
                    "<tr><td>album.id</td><td>int</td><td>The album ID</td></tr>" +
                    "<tr><td>album.name</td><td>string</td><td>The album name</td></tr>" +
                    "<tr><td>album.coverUrl</td><td>string</td><td>Relative URL for the album cover image</td></tr>" +
                    "<tr><td>album.shareUrl</td><td>string</td><td>Part of the share URL endpoint for public sharing</td></tr>" +
                    "<tr><td>albumMetadataList[].metadata</td><td>object</td><td>A <a href=\"#\" data-bs-toggle=\"modal\" data-bs-target=\"#propMetadataDocs\">Metadata</a> object</td></tr>" +
                    "</tbody></table>"
        )
    )
    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/api/v1/album/{albumId}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedSizeAlbum(model: Model, @PathVariable albumId: Int, @RequestParam page: Optional<Int>, @RequestParam size: Optional<Int>): String {
        return mapper.writeValueAsString(buildAlbum(model, albumId, page.orElse(0), size.orElse(model.getAttribute("queryLimit").toString().toInt()), "all"))
    }

    private fun buildAlbum(model: Model, albumId: Int, page: Int = 0, size: Int = model.getAttribute("queryLimit").toString().toInt(), mediaTypeFilter: String?): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        val module = "album"
        response["message"] = "Nothing to see here."
        response["activePage"] = module
        response["activeSidebar"] = module
        response["titleDescriptor"] = TextUtils.capitalized(module)
        response["page"] = page
        response["size"] = size

        response["album"] = Album()
        response["albumId"] = 0
        response["albumMetadataList"] = mutableListOf<Metadata>()
        response["albumPhotoCommentsMap"] = mutableMapOf<String, ArrayList<HashMap<String, Any>>>()
        response["userMap"] = mutableMapOf<String, Any>()
        response["favorites"] = mutableMapOf<String, String>()
        response["msg"] = "No results"
        response["status"] = "noop"
        response["keywordMap"] = mutableMapOf<String, String>()
        response["status"] = "noop"
        response["canEdit"] = (model.getAttribute("authority") == adminRole || model.getAttribute("authority") == superRole)
        response["totalPages"] = 0

        var mediaType = mediaTypeFilter

        if (mediaTypeFilter.isNullOrEmpty()) {
            mediaType = "all"
        }

        response["mediaTypeFilter"] = mediaType

        val favoritesMap = HashMap<String, HashMap<String, Any>>()
        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null && albumId > 0) {
            var userAlbums: UserAlbum? = null
            if (currentUserObj.getAuthority()!! == "ROLE_USER") {
                userAlbums = userAlbumRepository.findDistinctByUserIdAndAlbumId(currentUserObj.getId(), albumId)
            }

            if ((currentUserObj.getAuthority()!! == "ROLE_ADMIN" || currentUserObj.getAuthority()!! == "ROLE_SUPER") || (userAlbums != null && currentUserObj.getAuthority()!! == "ROLE_USER")) {
                // Get album photos
                val albumPhotos: MutableIterable<AlbumPhoto?>? = if (mediaType == "all") {
                    response["totalPages"] = albumPhotoRepository.countByAlbumId(albumId)?.div(size)

                    albumPhotoRepository.findAllByAlbumIdAndOffsetAndLimit(
                        albumId,
                        page * size,
                        size
                    )
                } else if (mediaType == "nolatlng") {
                    response["totalPages"] = albumPhotoRepository.countAlbumIdAndNoCoord(albumId)?.div(size)

                    albumPhotoRepository.findAllByAlbumIdAndNoCoordAndOffsetAndLimit(
                        albumId,
                        page * size,
                        size
                    )
                } else {
                    response["totalPages"] = albumPhotoRepository.countAlbumIdAndMediaType(albumId,mediaType)?.div(size)

                    albumPhotoRepository.findAllByAlbumIdAndMediaTypeAndOffsetAndLimit(
                        albumId,
                        mediaType,
                        page * size,
                        size
                    )
                }
                val albumMetadataList = ArrayList<Metadata>()
                if (albumPhotos != null) {
                    val albumPhotosCommentsMap = HashMap<String, ArrayList<HashMap<String, Any>>>()

                    for (albumPhoto in albumPhotos) {
                        val albumPhotoCommentsList = ArrayList<HashMap<String, Any>>()
                        if (albumPhoto != null) {
                            val metadata = metadataRepository.findById(albumPhoto.getMetadataId()!!)
                            albumMetadataList.add(metadata.get())

                            val favorites = favoriteRepository.findAllByMetadataId(albumPhoto.getMetadataId())
                            if (favorites != null) {
                                for (favorite in favorites) {
                                    if (favorite != null) {
                                        favoritesMap[albumPhoto.getMetadataId().toString()] = hashMapOf(
                                            "favorite" to (favorite.getUserId() == currentUserObj.getId()),
                                            "count" to favoriteRepository.countAllByMetadataId(albumPhoto.getMetadataId().toString())
                                        )

                                        if (favorite.getUserId() == currentUserObj.getId()) {
                                            break
                                        }
                                    }
                                }
                            }

                            // Get comments for this photo
                            val albumPhotoComments = commentRepository.findCommentsByAlbumIdAndMetadataId(albumId,albumPhoto.getMetadataId()!!)
                            for (albumPhotoComment in albumPhotoComments) {
                                val albumPhotoCommentMap = HashMap<String, Any>()
                                albumPhotoCommentMap["comment"] = albumPhotoComment.getComment().toString()
                                albumPhotoCommentMap["commentId"] = albumPhotoComment.getCommentId().toString().toInt()
                                albumPhotoCommentMap["metadataId"] = albumPhotoComment.getMetadataId().toString()
                                albumPhotoCommentMap["albumId"] = albumPhotoComment.getAlbumId().toString().toInt()
                                albumPhotoCommentMap["userId"] = albumPhotoComment.getUserId().toString().toInt()
                                albumPhotoCommentMap["userProfile"] = if (albumPhotoComment.getUserProfile()==null) "" else albumPhotoComment.getUserProfile().toString()
                                albumPhotoCommentMap["username"] = albumPhotoComment.getUsername().toString()
                                albumPhotoCommentMap["createdAt"] = TextUtils.formatToLongDateWithTime(albumPhotoComment.getCreatedAt().toString())
                                albumPhotoCommentsList.add(albumPhotoCommentMap)
                            }
                            if (albumPhotoCommentsList.isNotEmpty()) {
                                albumPhotosCommentsMap[metadata.get().getId()] = albumPhotoCommentsList
                            }
                        }
                    }
                    if (albumMetadataList.count() > 0) {
                        val album = albumRepository.findById(albumId)
                        response["titleDescriptor"] = album.get().getName()
                        response["favorites"] = favoritesMap
                        response["albumPhotoCommentsMap"] = albumPhotosCommentsMap
                        response["album"] = album.get()
                        var coverUrl = ""
                        if (album.get().getCoverUrl() != null) {
                            val metadata = metadataRepository.findByThumbnailCentered(album.get().getCoverUrl().toString())
                            if (metadata != null) {
                                coverUrl = "/api/v1/thumbnails/centered/"+metadata.getId()
                            }
                        }
                        response["coverUrl"] = coverUrl
                        response["albumId"] = album.get().getId()
                        val keywordList = keywordRepository!!.findAllKeywordsGroupedByMetadataId()
                        val keywordMap = mutableMapOf<String, String>()
                        for (keywordGroup in keywordList) {
                            keywordMap[keywordGroup.getMetadataId()!!] = keywordGroup.getKeywords()!!
                        }
                        response["keywordMap"] = keywordMap
                        val userMap = HashMap<String, Any>()
                        userMap["id"] = currentUserObj.getId()
                        userMap["username"] = if (currentUserObj.getUsername() == null) "" else currentUserObj.getUsername()!!
                        userMap["userProfile"] = if (currentUserObj.getProfile() == null) "" else currentUserObj.getProfile()!!
                        var showControls = false
                        if (currentUserObj.getAuthority() != null && (currentUserObj.getAuthority()!! == "ROLE_ADMIN" || currentUserObj.getAuthority()!! == "ROLE_SUPER")) {
                            showControls = true
                        }
                        userMap["showControls"] = showControls
                        response["userMap"] = userMap
                        response["albumMetadataList"] = albumMetadataList
                        response["msg"] = "Results retrieved"
                        response["status"] = ApiResponse.SUCCESS.status
                        response["message"] = ""
                    }
                }
            }
        }

        return response
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @PostMapping("/album/download/{albumId}")
    fun postAlbumDownload(model: Model, @RequestParam download: Int, @PathVariable albumId: Int, response: HttpServletResponse): ResponseEntity<InputStreamResource>? {
        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null && albumId > 0 && download == albumId) {
            var userAlbums: UserAlbum? = null
            if (currentUserObj.getAuthority()!! == "ROLE_USER") {
                userAlbums = userAlbumRepository.findDistinctByUserIdAndAlbumId(currentUserObj.getId(), albumId)
            }

            if ((currentUserObj.getAuthority()!! == "ROLE_ADMIN" || currentUserObj.getAuthority()!! == "ROLE_SUPER") || (userAlbums != null && currentUserObj.getAuthority()!! == "ROLE_USER")) {
                // Get album photos
                val albumPhotos = albumPhotoRepository.findAllByAlbumId(albumId)

                if (albumPhotos != null) {
                    val albumObj = albumRepository.findAlbumById(albumId)
                    val tempExportBaseDir = Files.createTempDirectory(albumId.toString())

                    for (albumPhoto in albumPhotos) {
                        if (albumPhoto != null) {
                            val metadata = metadataRepository.findById(albumPhoto.getMetadataId()!!)
                            if (metadata.isPresent && !metadata.get().getType()?.contains("video", ignoreCase = true)!!) {
                                val tempFile = File(metadata.get().getPath()!!)
                                if (tempFile.exists()) {
                                    val tempFileTo =
                                        File(tempExportBaseDir.toString() + "/" + metadata.get().getFileName())
                                    Files.copy(
                                        tempFile.toPath(),
                                        tempFileTo.toPath(),
                                        StandardCopyOption.REPLACE_EXISTING
                                    )
                                } else {
                                    logger.log(
                                        Level.INFO,
                                        "Exporting album photo. File does not exist: " + tempFile.absolutePath
                                    )
                                }
                            } else {
                                logger.log(
                                    Level.INFO,
                                    "Ignoring album video: " + metadata.get().getPath()
                                )
                            }
                        }
                    }

                    if (tempExportBaseDir.isDirectory() && tempExportBaseDir.toList().isNotEmpty()) {
                        val tempDir = tempExportBaseDir.toFile()
                        val outputZipFile = FileUtils.zipFolder(tempDir, albumObj?.getName()!!)
                        FileUtils.deleteDirectory(tempDir)

                        if (outputZipFile != null) {
                            outputZipFile.deleteOnExit()

                            val resource = InputStreamResource(FileInputStream(outputZipFile))
                            val contentLength = outputZipFile.length()

                            val headers = HttpHeaders()
                            headers.add(HttpHeaders.SET_COOKIE, ResponseCookie.from("ShashinAlbumName",
                                outputZipFile.name.replace("\\s".toRegex(), "_").lowercase(Locale.getDefault())
                            ).path("/").build().toString())
                            headers.add(HttpHeaders.SET_COOKIE, ResponseCookie.from("ShashinAlbumSize",contentLength.toString()).path("/").build().toString())
                            headers.add(
                                HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=" + outputZipFile.name
                            )
                            headers.add("Cache-Control", "no-cache, no-store, must-revalidate")
                            headers.add("Pragma", "no-cache")
                            headers.add("Expires", "0")

                            return ResponseEntity.ok()
                                .headers(headers)
                                .contentLength(contentLength)
                                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                .body(resource)
                        }
                    }
                }
            }
        }

        return null
    }

    @PostMapping("/download/share/{shareLink}/album/{albumId}")
    fun postShareAlbumDownload(model: Model, @RequestParam download: Optional<Int>, @RequestParam downloadArray: Optional<String>, @PathVariable shareLink: String, @PathVariable albumId: Int, response: HttpServletResponse): ResponseEntity<InputStreamResource>? {
        if (albumId > 0 && (download.isPresent && download.get() == albumId) || (downloadArray.isPresent && downloadArray.get() != "")) {

            // Get album photos
            val albumPhotos = albumPhotoRepository.findAllByAlbumId(albumId)
            val albumObj = albumRepository.findById(albumId)

            if (albumPhotos != null && albumObj.isPresent && albumObj.get().getShareUrl() == shareLink) {
                val tempExportBaseDir = Files.createTempDirectory(albumId.toString())

                if (download.isPresent && download.get() == albumId) {
                    for (albumPhoto in albumPhotos) {
                        if (albumPhoto != null) {
                            val metadata = metadataRepository.findById(albumPhoto.getMetadataId()!!)
                            if (!metadata.get().getType()?.contains("video", ignoreCase = true)!!) {
                                val tempFile = File(metadata.get().getPath()!!)
                                if (tempFile.exists()) {
                                    val tempFileTo =
                                        File(tempExportBaseDir.toString() + "/" + metadata.get().getFileName())
                                    Files.copy(
                                        tempFile.toPath(),
                                        tempFileTo.toPath(),
                                        StandardCopyOption.REPLACE_EXISTING
                                    )
                                } else {
                                    logger.log(
                                        Level.INFO,
                                        "Exporting album photo. File does not exist: " + tempFile.absolutePath
                                    )
                                }
                            } else {
                                logger.log(
                                    Level.INFO,
                                    "Ignoring album video: " + metadata.get().getPath()
                                )
                            }
                        }
                    }
                } else if (downloadArray.isPresent && downloadArray.get() != "") {
                    val metadataIdArray: Array<String>? = mapper.readValue(downloadArray.get(), object : TypeReference<Array<String>>() {})

                    if (metadataIdArray != null) {
                        for (metadataId in metadataIdArray) {
                            val metadata = metadataRepository.findById(metadataId)
                            val tempFile = File(metadata.get().getPath()!!)
                            if (tempFile.exists()) {
                                val tempFileTo =
                                    File(tempExportBaseDir.toString() + "/" + metadata.get().getFileName())
                                Files.copy(
                                    tempFile.toPath(),
                                    tempFileTo.toPath(),
                                    StandardCopyOption.REPLACE_EXISTING
                                )
                            } else {
                                logger.log(
                                    Level.INFO,
                                    "Exporting album photo. File does not exist: " + tempFile.absolutePath
                                )
                            }
                        }
                    }
                }

                if (tempExportBaseDir.isDirectory() && tempExportBaseDir.toList().isNotEmpty()) {
                    val tempDir = tempExportBaseDir.toFile()
                    val outputZipFile = FileUtils.zipFolder(tempDir, albumObj.get().getName()!!)
                    FileUtils.deleteDirectory(tempDir)

                    if (outputZipFile != null) {
                        outputZipFile.deleteOnExit()

                        val resource = InputStreamResource(FileInputStream(outputZipFile))
                        val contentLength = outputZipFile.length()

                        val headers = HttpHeaders()
                        headers.add(HttpHeaders.SET_COOKIE, ResponseCookie.from("ShashinShareAlbumName",
                            outputZipFile.name.replace("\\s".toRegex(), "_").lowercase(Locale.getDefault())
                        ).path("/").build().toString())
                        headers.add(HttpHeaders.SET_COOKIE, ResponseCookie.from("ShashinShareAlbumSize",contentLength.toString()).path("/").build().toString())
                        headers.add(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=" + outputZipFile.name
                        )
                        headers.add("Cache-Control", "no-cache, no-store, must-revalidate")
                        headers.add("Pragma", "no-cache")
                        headers.add("Expires", "0")

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
    @RequestMapping(value = ["/album/updatename/{albumId}"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun updateAlbumName(@RequestBody requestBody: JsonNode, @PathVariable albumId: Int): String? {
        val albumPayload = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (albumPayload.containsKey("albumId") && albumPayload.containsKey("albumName")) {
            val postAlbumId = albumPayload["albumId"].toString().toInt()
            val albumName = albumPayload["albumName"].toString()

            if (postAlbumId == albumId && albumName.isNotEmpty()) {
                val foundAlbumRecord = albumRepository.findAlbumByNameIgnoreCase(albumName)

                return if (foundAlbumRecord == null || foundAlbumRecord.getId() == albumId) {
                    val albumObj = albumRepository.findById(albumId).get()
                    albumObj.setName(albumName)
                    albumRepository.save(albumObj)

                    resp["msg"] = "Saved"
                    resp["status"] = ApiResponse.SUCCESS.status
                    mapper.writeValueAsString(resp)
                } else {
                    resp["msg"] = "Album name \"$albumName\" already exists"
                    resp["status"] = ApiResponse.WARN.status
                    mapper.writeValueAsString(resp)
                }

            }
        }

        resp["msg"] = "Could not save"
        resp["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(resp)
    }
}