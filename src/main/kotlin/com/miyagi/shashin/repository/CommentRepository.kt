package com.miyagi.shashin.repository

import com.miyagi.shashin.model.AlbumComments
import com.miyagi.shashin.model.Comment
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface CommentRepository : CrudRepository<Comment?, Int?> {
    @Query("SELECT c.id as commentId, c.comment, a.album_id as albumId,u.id as userId, u.username FROM comment c LEFT JOIN user u ON u.id = c.user_id LEFT JOIN albumcomment a on c.id = a.comment_id WHERE a.album_id = :albumId ORDER BY c.created_at DESC", nativeQuery = true)
    fun findCommentsByAlbumId(@Param("albumId") albumId: Int): MutableIterable<AlbumComments>
}