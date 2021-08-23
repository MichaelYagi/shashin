package com.miyagi.shashin.repository

import com.miyagi.shashin.model.AlbumComments
import com.miyagi.shashin.model.AlbumPhotoComments
import com.miyagi.shashin.model.Comment
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface CommentRepository : CrudRepository<Comment?, Int?> {
    @Query("SELECT c.id as commentId, c.comment, a.album_id as albumId,u.id as userId, u.username, c.created_at as createdAt FROM comment c LEFT JOIN user u ON u.id = c.user_id LEFT JOIN albumcomment a on c.id = a.comment_id WHERE a.album_id = :albumId ORDER BY c.created_at DESC", nativeQuery = true)
    fun findCommentsByAlbumId(@Param("albumId") albumId: Int): MutableIterable<AlbumComments>
    @Query("SELECT c.id as commentId, c.comment, a.album_id as albumId, a.metadata_id as metadataId, u.id as userId, u.username, c.created_at as createdAt FROM comment c, metadata m LEFT JOIN user u ON u.id = c.user_id LEFT JOIN albumphotocomment a on c.id = a.comment_id WHERE a.metadata_id = m.id AND a.album_id = :albumId AND a.metadata_id = :metadataId ORDER BY c.created_at DESC", nativeQuery = true)
    fun findCommentsByAlbumIdAndMetadataId(@Param("albumId") albumId: Int, @Param("metadataId") metadataId: String): MutableIterable<AlbumPhotoComments>
    fun deleteByUserId(userId: Int?): Long
}