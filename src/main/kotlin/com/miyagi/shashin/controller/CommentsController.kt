package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.*
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import org.apache.commons.lang3.StringEscapeUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
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

    @RequestMapping(value = ["/comment/album/save"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
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
                            val album = userAlbumRepository.findByUserIdAndAlbumId(user.getId(),albumId)
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
                resp["status"] = "success"
                resp["commentId"] = savedCommentObj.getId().toString()
                return mapper.writeValueAsString(resp)
            }
        }

        resp["commentCount"] = 0
        resp["msg"] = "Could not save to comment"
        resp["status"] = "fail"
        resp["commentId"] = 0
        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/comment/albumphoto/save"], method = [RequestMethod.POST], produces = ["application/json"])
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
                            val album = userAlbumRepository.findByUserIdAndAlbumId(user.getId(),albumId)
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
                resp["status"] = "success"
                resp["commentId"] = savedCommentObj.getId().toString()
                return mapper.writeValueAsString(resp)
            }
        }

        resp["msg"] = "Could not save to comment"
        resp["status"] = "fail"
        resp["commentId"] = 0
        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/comment/update"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    fun postUpdateComment(model: Model, @RequestBody requestBody: JsonNode): String {
        val commentMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (commentMap.containsKey("commentId") && commentMap.containsKey("comment")) {
            val commentId = commentMap["commentId"].toString().toInt()
            val commentText = StringEscapeUtils.escapeHtml4(commentMap["comment"].toString())

            val currentUserObj = model.getAttribute("currentUser") as User?
            if (currentUserObj != null) {

                // Update comment
                val commentObj = commentRepository.findById(commentId)
                if (currentUserObj.getId() == commentObj.get().getUserId()) {
                    commentObj.get().setComment(commentText)
                    commentObj.get().setModifiedAt(getCurrentTimestamp())
                    commentRepository.save(commentObj.get())

                    resp["msg"] = "Comment saved!"
                    resp["status"] = "success"
                    resp["commentId"] = commentObj.get().getId().toString()
                    return mapper.writeValueAsString(resp)
                }
            }
        }

        resp["msg"] = "Could not update comment"
        resp["status"] = "fail"
        resp["commentId"] = 0
        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/comment/albumphoto/delete"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun postDeleteAlbumPhotoComment(model: Model, @RequestBody requestBody: JsonNode): String {
        val commentMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (commentMap.containsKey("commentId") && commentMap.containsKey("metadataId")) {
            val commentId = commentMap["commentId"].toString().toInt()

            val currentUserObj = model.getAttribute("currentUser") as User?
            if (currentUserObj != null) {
                // Delete comment
                val commentObj = commentRepository.findById(commentId)
                if (currentUserObj.getId() == commentObj.get().getUserId()) {
                    albumPhotoCommentRepository.deleteByCommentId(commentId)
                    commentRepository.deleteById(commentId)

                    resp["msg"] = "Comment deleted"
                    resp["status"] = "success"
                    resp["commentId"] = commentId.toString()
                    return mapper.writeValueAsString(resp)
                }
            }
        }

        resp["msg"] = "Could not delete comment"
        resp["status"] = "fail"
        resp["commentId"] = 0
        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/comment/album/delete"], method = [RequestMethod.POST], produces = ["application/json"])
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
                    resp["status"] = "success"
                    resp["commentId"] = commentId.toString()
                    return mapper.writeValueAsString(resp)
                }
            }
        }

        resp["commentCount"] = 0
        resp["msg"] = "Could not delete comment"
        resp["status"] = "fail"
        resp["commentId"] = 0
        return mapper.writeValueAsString(resp)
    }
}