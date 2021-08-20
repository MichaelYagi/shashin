package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.AlbumComment
import com.miyagi.shashin.model.Comment
import com.miyagi.shashin.model.Favorite
import com.miyagi.shashin.repository.AlbumCommentRepository
import com.miyagi.shashin.repository.CommentRepository
import com.miyagi.shashin.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.transaction.Transactional

@Controller
class CommentsController {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var commentRepository: CommentRepository

    @Autowired
    private lateinit var albumCommentRepository: AlbumCommentRepository

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()

    @RequestMapping(value = ["/comment/album/save"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    fun postSaveFavorite(model: Model, @RequestBody requestBody: JsonNode): String {
        val commentMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (commentMap.containsKey("albumId") && commentMap.containsKey("comment")) {
            val albumId = commentMap["albumId"].toString().toInt()
            val commentText = commentMap["comment"].toString()

            val currentUserObj = userRepository.findByUsername(model.getAttribute("username").toString())

            if (currentUserObj != null) {
                val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val now = LocalDateTime.now()

                // Insert into comments
                val comment = Comment()
                comment.setUserId(currentUserObj.getId())
                comment.setComment(commentText)
                comment.setCreatedAt(dtf.format(now))
                comment.setModifiedAt(dtf.format(now))
                val savedCommentObj = commentRepository.save(comment)

                // Insert into album comment
                val albumComment = AlbumComment()
                albumComment.setCommentId(savedCommentObj.getId())
                albumComment.setAlbumId(albumId)
                albumComment.setCreatedAt(dtf.format(now))
                albumComment.setModifiedAt(dtf.format(now))
                albumCommentRepository.save(albumComment)

                resp["msg"] = "Comment saved!"
                resp["status"] = "success"
                resp["commentId"] = savedCommentObj.getId().toString()
                return mapper.writeValueAsString(resp)
            }
        }

        resp["msg"] = "Could not save to comment"
        resp["status"] = "fail"
        resp["commentId"] = ""
        return mapper.writeValueAsString(resp)
    }
}