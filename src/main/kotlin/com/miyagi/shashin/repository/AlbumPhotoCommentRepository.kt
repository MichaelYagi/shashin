package com.miyagi.shashin.repository

import com.miyagi.shashin.model.AlbumComment
import com.miyagi.shashin.model.AlbumPhotoComment
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AlbumPhotoCommentRepository : CrudRepository<AlbumPhotoComment?, Int?> {
    fun deleteByCommentId(commentId: Int): Long
    fun deleteByAlbumId(albumId: Int): Long
}