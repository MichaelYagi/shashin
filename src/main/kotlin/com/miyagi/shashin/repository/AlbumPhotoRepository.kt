package com.miyagi.shashin.repository

import com.miyagi.shashin.model.AlbumPhoto
import com.miyagi.shashin.model.Favorite
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AlbumPhotoRepository : CrudRepository<AlbumPhoto?, Int?> {
    fun countByMetadataIdAndAlbumId(metdataId: String?, albumId: Int?): Int?
    fun countByAlbumId(albumId: Int?): Int?
    @Query("SELECT * FROM albumphoto WHERE album_id = :albumId LIMIT :offset, :limit", nativeQuery = true)
    fun findAllByAlbumIdAndOffsetAndLimit(@Param("albumId") albumId: Int, @Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<AlbumPhoto?>?
    fun findAllByAlbumId(albumId: Int?): MutableIterable<AlbumPhoto?>?
    fun findFirstByOrderByIdAsc(): AlbumPhoto?
    fun deleteByMetadataIdAndAlbumId(metadataId: String?, albumId: Int?): Long?
    fun deleteByAlbumId(albumId: Int?): Long?
    fun deleteByMetadataId(metadataId: String?): Long?
}