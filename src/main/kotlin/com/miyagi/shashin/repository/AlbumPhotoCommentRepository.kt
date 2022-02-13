package com.miyagi.shashin.repository

import com.miyagi.shashin.model.AlbumPhotoComment
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

@Transactional
@Repository
interface AlbumPhotoCommentRepository : CrudRepository<AlbumPhotoComment?, Int?> {
    fun deleteByCommentId(commentId: Int): Long
    fun deleteByAlbumId(albumId: Int): Long
    fun findByMetadataId(metadataId: String): MutableIterable<AlbumPhotoComment?>?
    fun deleteByMetadataId(metadataId: String): Long
}