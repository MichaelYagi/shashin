package com.miyagi.shashin.repository

import com.miyagi.shashin.model.AlbumPhotoComments
import com.miyagi.shashin.model.Metadata
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface MetadataRepository : CrudRepository<Metadata?, String?> {
   fun findAll(sort: Sort?): MutableIterable<Metadata?>?

   @Query("SELECT DISTINCT * FROM metadata m LEFT JOIN albumphoto ap ON m.id = ap.metadata_id LEFT JOIN useralbum ua ON ap.album_id = ua.album_id LEFT JOIN album a ON a.id = ua.album_id WHERE ua.user_id = :userId", nativeQuery = true)
   fun findByAlbumMetadataByUserId(@Param("userId") userId: Int): MutableIterable<Metadata>
}