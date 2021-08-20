package com.miyagi.shashin.repository

import com.miyagi.shashin.model.AlbumComment
import com.miyagi.shashin.model.AlbumPhoto
import org.springframework.data.repository.CrudRepository

interface AlbumCommentRepository : CrudRepository<AlbumComment?, Int?> {
    fun deleteByCommentId(commentId: Int): Long
    fun deleteByAlbumId(albumId: Int): Long
    fun findAllByAlbumId(albumId: Int): MutableIterable<AlbumComment?>?
}