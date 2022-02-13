package com.miyagi.shashin.repository

import com.miyagi.shashin.model.AlbumComment
import com.miyagi.shashin.model.AlbumPhoto
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

@Transactional
@Repository
interface AlbumCommentRepository : CrudRepository<AlbumComment?, Int?> {
    fun deleteByCommentId(commentId: Int): Long
    fun deleteByAlbumId(albumId: Int): Long
    fun findAllByAlbumId(albumId: Int): MutableIterable<AlbumComment?>?
}