package com.miyagi.shashin.repository

import com.miyagi.shashin.model.AlbumComment
import com.miyagi.shashin.model.AlbumPhotoComment
import org.springframework.data.repository.CrudRepository

interface AlbumPhotoCommentRepository : CrudRepository<AlbumPhotoComment?, Int?> {
    fun deleteByCommentId(commentId: Int): Long
    fun deleteByAlbumId(albumId: Int): Long
}