package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.*
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.ApiResponse
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import com.miyagi.shashin.util.TextUtils.Companion.returnForbiddenError
import io.swagger.v3.oas.annotations.Operation
import org.apache.commons.text.StringEscapeUtils
import org.springdoc.core.annotations.RouterOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.*
import javax.servlet.http.HttpServletResponse
import javax.transaction.Transactional

@Controller
class CommentsController {

    @Autowired
    private lateinit var commentRepository: CommentRepository

    @Autowired
    private lateinit var albumRepository: AlbumRepository

    @Autowired
    private lateinit var userAlbumRepository: UserAlbumRepository

    @Autowired
    private lateinit var albumCommentRepository: AlbumCommentRepository

    @Autowired
    private lateinit var albumPhotoCommentRepository: AlbumPhotoCommentRepository

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var metadataRepository: MetadataRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Value("\${app.role.admin}")
    private var adminRole: String? = null

    @Value("\${app.role.user}")
    private var userRole: String? = null

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, Any?>()

    @RouterOperation(
        operation =
        Operation(
            operationId = "postSaveComment",
            description = "<strong>Save a comment for an album.</strong>" +
                    "<pre><code>" +
                    "curl -X POST \"http://127.0.0.1:6624/api/v1/comment/album/save\" \\\n" +
                    "-H \"Content-Type: application/json\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "-d '{\"albumId\": &lt;album_id&gt;, \"comment\": \"&lt;comment&gt;\"}'" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>Content-Type</td><td>header</td><td>string</td><td>required</td><td>application/json</td></tr>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>albumId</td><td>body param</td><td>int</td><td>required</td><td>Save a comment for this album ID</td></tr>" +
                    "<tr><td>comment</td><td>body param</td><td>string</td><td>required</td><td>The comment</td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"commentCount\": &lt;comment_count&gt;,\n" +
                    "    \"commentId\": &lt;comment_id&gt;\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>commentCount</td><td>int</td><td>The number of comments for this album</td></tr>" +
                    "<tr><td>commentId</td><td>int</td><td>The comment ID</td></tr>" +
                    "</tbody></table>"
        )
    )
    @RequestMapping(value = ["/comment/album/save","/api/v1/comment/album/save"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun postSaveComment(model: Model, @RequestBody requestBody: JsonNode): String {
        val commentMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (commentMap.containsKey("albumId") && commentMap.containsKey("comment")) {
            val albumId = commentMap["albumId"].toString().toInt()
            val commentText = StringEscapeUtils.escapeHtml4(commentMap["comment"].toString())

            val currentUserObj = model.getAttribute("currentUser") as User?

            if (currentUserObj != null) {
                // Insert into comments
                val comment = Comment()
                comment.setUserId(currentUserObj.getId())
                comment.setComment(commentText)
                comment.setCreatedAt(getCurrentTimestamp())
                comment.setModifiedAt(getCurrentTimestamp())
                val savedCommentObj = commentRepository.save(comment)

                // Insert into album comment
                val albumComment = AlbumComment()
                albumComment.setCommentId(savedCommentObj.getId())
                albumComment.setAlbumId(albumId)
                albumComment.setCreatedAt(getCurrentTimestamp())
                albumComment.setModifiedAt(getCurrentTimestamp())
                albumCommentRepository.save(albumComment)

                // Notify if admin or other users in album
                val albumObj = albumRepository.findById(albumId)
                val users = userRepository.findAll()
                val notificationObjList = mutableListOf<Notification>()
                val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
                for (user in users) {
                    val notificationObj = Notification()
                    var createEntry = false
                    if (user != null && user.getId() != currentUserObj.getId()) {
//                        if (user.getAuthority() == adminRole) {
//                            createEntry = true
//                        } else {
                            val album = userAlbumRepository.findDistinctByUserIdAndAlbumId(user.getId(),albumId)
                            if (album != null) {
                                createEntry = true
                            }
//                        }

                        if (createEntry) {
                            notificationObj.setUserId(user.getId())
                            notificationObj.setCommentId(albumComment.getId())
                            notificationObj.setAlbumId(albumId)
                            notificationObj.setCreatedAt(getCurrentTimestamp())
                            notificationObj.setModifiedAt(getCurrentTimestamp())
                            notificationObj.setRead(false)
                            notificationObj.setMessage(currentUserObj.getUsername()+" commented on album <a href='/albums' target='_blank'>"+albumObj.get().getName()+"</a> \""+commentText+"\" on "+sdtf.format(Date()))
                            notificationObjList.add(notificationObj)
                        }
                    }
                }
                notificationRepository.saveAll(notificationObjList)

                val comments = commentRepository.findCommentsByAlbumId(albumId)
                resp["commentCount"] = comments.count()
                resp["msg"] = "Comment saved!"
                resp["status"] = ApiResponse.SUCCESS.status
                resp["commentId"] = savedCommentObj.getId().toString()
                return mapper.writeValueAsString(resp)
            }
        }

        resp["commentCount"] = 0
        resp["msg"] = "Could not save to comment"
        resp["status"] = ApiResponse.FAIL.status
        resp["commentId"] = 0
        return mapper.writeValueAsString(resp)
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "postSaveAlbumPhotoComment",
            description = "<strong>Save a comment for an album photo or video.</strong>" +
                    "<pre><code>" +
                    "curl -X POST \"http://127.0.0.1:6624/api/v1/comment/albumphoto/save\" \\\n" +
                    "-H \"Content-Type: application/json\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "-d '{\"albumId\": &lt;album_id&gt;, \"comment\": \"&lt;comment&gt;\", \"metadataId\": \"&lt;metadata_id&gt;\"}'" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>Content-Type</td><td>header</td><td>string</td><td>required</td><td>application/json</td></tr>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>albumId</td><td>body param</td><td>int</td><td>required</td><td>Save a comment for this album ID and media</td></tr>" +
                    "<tr><td>comment</td><td>body param</td><td>string</td><td>required</td><td>The comment</td></tr>" +
                    "<tr><td>metadataId</td><td>body param</td><td>string</td><td>required</td><td>The metadata ID for media</td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"commentId\": &lt;comment_id&gt;\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>commentId</td><td>int</td><td>The comment ID</td></tr>" +
                    "</tbody></table>"
        )
    )
    @RequestMapping(value = ["/comment/albumphoto/save","/api/v1/comment/albumphoto/save"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun postSaveAlbumPhotoComment(model: Model, @RequestBody requestBody: JsonNode): String {
        val commentMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (commentMap.containsKey("albumId") && commentMap.containsKey("comment") && commentMap.containsKey("metadataId")) {
            val albumId = commentMap["albumId"].toString().toInt()
            val metadataId = StringEscapeUtils.escapeHtml4(commentMap["metadataId"].toString())
            val commentText = StringEscapeUtils.escapeHtml4(commentMap["comment"].toString())

            val currentUserObj = model.getAttribute("currentUser") as User?
            if (currentUserObj != null) {
                // Insert into comments
                val comment = Comment()
                comment.setUserId(currentUserObj.getId())
                comment.setComment(commentText)
                comment.setCreatedAt(getCurrentTimestamp())
                comment.setModifiedAt(getCurrentTimestamp())
                val savedCommentObj = commentRepository.save(comment)

                // Insert into album photo comment
                val albumPhotoComment = AlbumPhotoComment()
                albumPhotoComment.setCommentId(savedCommentObj.getId())
                albumPhotoComment.setMetadataId(metadataId)
                albumPhotoComment.setAlbumId(albumId)
                albumPhotoComment.setCreatedAt(getCurrentTimestamp())
                albumPhotoComment.setModifiedAt(getCurrentTimestamp())
                albumPhotoCommentRepository.save(albumPhotoComment)

                // Notify if admin or other users in album
                val metadataObj = metadataRepository.findById(metadataId)
                val albumObj = albumRepository.findById(albumId)
                val users = userRepository.findAll()
                val notificationObjList = mutableListOf<Notification>()
                val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
                    for (user in users) {
                    val notificationObj = Notification()
                    var createEntry = false
                    if (user != null && user.getId() != currentUserObj.getId()) {
//                        if (user.getAuthority() == adminRole) {
//                            createEntry = true
//                        } else {
                            val album = userAlbumRepository.findDistinctByUserIdAndAlbumId(user.getId(),albumId)
                            if (album != null) {
                                createEntry = true
                            }
//                        }

                        if (createEntry) {
                            notificationObj.setUserId(user.getId())
                            notificationObj.setCommentId(savedCommentObj.getId())
                            notificationObj.setMetadataId(metadataId)
                            notificationObj.setAlbumId(albumId)
                            notificationObj.setRead(false)
                            notificationObj.setCreatedAt(getCurrentTimestamp())
                            notificationObj.setModifiedAt(getCurrentTimestamp())
                            notificationObj.setMessage(currentUserObj.getUsername()+" commented on album "+albumObj.get().getName()+" for photo <a href='/album/"+albumObj.get().getId()+"' target='_blank'>"+metadataObj.get().getFileName()+"</a> \""+commentText+"\" on "+sdtf.format(Date()))
                            notificationObjList.add(notificationObj)
                        }
                    }
                }
                notificationRepository.saveAll(notificationObjList)

                resp["msg"] = "Comment saved!"
                resp["status"] = ApiResponse.SUCCESS.status
                resp["commentId"] = savedCommentObj.getId().toString()
                return mapper.writeValueAsString(resp)
            }
        }

        resp["msg"] = "Could not save to comment"
        resp["status"] = ApiResponse.FAIL.status
        resp["commentId"] = 0
        return mapper.writeValueAsString(resp)
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "postUpdateComment",
            description = "<strong>Update a comment for an album, album photo or video.</strong>" +
                    "<pre><code>" +
                    "curl -X PUT \"http://127.0.0.1:6624/api/v1/comment/update\" \\\n" +
                    "-H \"Content-Type: application/json\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "-d '{\"commentId\": &lt;comment_id&gt;, \"comment\": \"&lt;updated_comment&gt;\"}'" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>Content-Type</td><td>header</td><td>string</td><td>required</td><td>application/json</td></tr>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>commentId</td><td>body param</td><td>int</td><td>required</td><td>The comment ID for the comment to update.</td></tr>" +
                    "<tr><td>comment</td><td>body param</td><td>string</td><td>required</td><td>The updated comment</td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"commentId\": &lt;comment_id&gt;\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>commentId</td><td>int</td><td>The comment ID that was updated</td></tr>" +
                    "</tbody></table>"
        )
    )
    @RequestMapping(value = ["/comment/update","/api/v1/comment/update"], method = [RequestMethod.PUT], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun postUpdateComment(model: Model, @RequestBody requestBody: JsonNode, response: HttpServletResponse): String {
        val commentMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (commentMap.containsKey("commentId") && commentMap.containsKey("comment")) {
            val commentId = commentMap["commentId"].toString().toInt()
            val commentText = StringEscapeUtils.escapeHtml4(commentMap["comment"].toString())

            val currentUserObj = model.getAttribute("currentUser") as User?
            if (currentUserObj != null) {

                // Update comment
                val commentObj = commentRepository.findById(commentId)
                if (commentObj.isPresent && currentUserObj.getId() == commentObj.get().getUserId()) {
                    commentObj.get().setComment(commentText)
                    commentObj.get().setModifiedAt(getCurrentTimestamp())
                    commentRepository.save(commentObj.get())

                    resp["msg"] = "Comment saved!"
                    resp["status"] = ApiResponse.SUCCESS.status
                    resp["commentId"] = commentObj.get().getId().toString()
                    return mapper.writeValueAsString(resp)
                } else {
                    return returnForbiddenError(response)
                }
            }
        }

        resp["msg"] = "Could not update comment"
        resp["status"] = ApiResponse.FAIL.status
        resp["commentId"] = 0
        return mapper.writeValueAsString(resp)
    }


    @RouterOperation(
        operation =
        Operation(
            operationId = "postDeleteAlbumPhotoComment",
            description = "<strong>Delete a comment for an album, album photo or video.</strong>" +
                    "<pre><code>" +
                    "curl -X DELETE \"http://127.0.0.1:6624/api/v1/comment/albumphoto/delete\" \\\n" +
                    "-H \"Content-Type: application/json\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "-d '{\"commentId\": &lt;comment_id&gt;}'" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>Content-Type</td><td>header</td><td>string</td><td>required</td><td>application/json</td></tr>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>commentId</td><td>body param</td><td>int</td><td>required</td><td>The comment ID to delete.</td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"commentId\": &lt;comment_id&gt;\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>commentId</td><td>int</td><td>The comment ID that was deleted</td></tr>" +
                    "</tbody></table>"
        )
    )
    @RequestMapping(value = ["/comment/albumphoto/delete","/api/v1/comment/albumphoto/delete"], method = [RequestMethod.DELETE], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun postDeleteAlbumPhotoComment(model: Model, @RequestBody requestBody: JsonNode): String {
        val commentMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        println(commentMap)
        if (commentMap.containsKey("commentId")) {
            val commentId = commentMap["commentId"].toString().toInt()

            val currentUserObj = model.getAttribute("currentUser") as User?
            if (currentUserObj != null) {
                // Delete comment
                val commentObj = commentRepository.findById(commentId)
                if (currentUserObj.getId() == commentObj.get().getUserId()) {
                    albumPhotoCommentRepository.deleteByCommentId(commentId)
                    commentRepository.deleteById(commentId)

                    resp["msg"] = "Comment deleted"
                    resp["status"] = ApiResponse.SUCCESS.status
                    resp["commentId"] = commentId.toString()
                    return mapper.writeValueAsString(resp)
                }
            }
        }

        resp["msg"] = "Could not delete comment"
        resp["status"] = ApiResponse.FAIL.status
        resp["commentId"] = 0
        return mapper.writeValueAsString(resp)
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "postDeleteComment",
            description = "<strong>Delete a comment for an album.</strong>" +
                    "<pre><code>" +
                    "curl -X DELETE \"http://127.0.0.1:6624/api/v1/comment/album/delete\" \\\n" +
                    "-H \"Content-Type: application/json\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "-d '{\"commentId\": &lt;comment_id&gt;, \"albumId\": &lt;album_id&gt;}'" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>Content-Type</td><td>header</td><td>string</td><td>required</td><td>application/json</td></tr>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "<tr><td>commentId</td><td>body param</td><td>int</td><td>required</td><td>The comment ID to delete.</td></tr>" +
                    "<tr><td>albumId</td><td>body param</td><td>int</td><td>required</td><td>The album ID associated with the comment.</td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"commentId\": &lt;comment_id&gt;,\n" +
                    "    \"commentCount\": &lt;comment_count&gt;\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>commentId</td><td>int</td><td>The comment ID that was deleted</td></tr>" +
                    "<tr><td>commentCount</td><td>int</td><td>The comment count after deletion</td></tr>" +
                    "</tbody></table>"
        )
    )
    @RequestMapping(value = ["/comment/album/delete", "/api/v1/comment/album/delete"], method = [RequestMethod.DELETE], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun postDeleteComment(model: Model, @RequestBody requestBody: JsonNode): String {
        val commentMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (commentMap.containsKey("commentId") && commentMap.containsKey("albumId")) {
            val commentId = commentMap["commentId"].toString().toInt()
            val albumId = commentMap["albumId"].toString().toInt()

            val currentUserObj = model.getAttribute("currentUser") as User?
            if (currentUserObj != null) {
                // Delete comment
                val commentObj = commentRepository.findById(commentId)
                if (currentUserObj.getId() == commentObj.get().getUserId()) {
                    albumCommentRepository.deleteByCommentId(commentId)
                    commentRepository.deleteById(commentId)

                    val comments = commentRepository.findCommentsByAlbumId(albumId)
                    resp["commentCount"] = comments.count()
                    resp["msg"] = "Comment deleted"
                    resp["status"] = ApiResponse.SUCCESS.status
                    resp["commentId"] = commentId.toString()
                    return mapper.writeValueAsString(resp)
                }
            }
        }

        resp["commentCount"] = 0
        resp["msg"] = "Could not delete comment"
        resp["status"] = ApiResponse.FAIL.status
        resp["commentId"] = 0
        return mapper.writeValueAsString(resp)
    }
}