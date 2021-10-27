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
    @Query("SELECT DISTINCT ap.* FROM albumphoto ap, metadata m WHERE ap.album_id = :albumId  AND ap.metadata_id = m.id ORDER BY m.year DESC, m.month DESC, m.day DESC, m.time DESC LIMIT :offset, :limit", nativeQuery = true)
    fun findAllByAlbumIdAndOffsetAndLimit(@Param("albumId") albumId: Int, @Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<AlbumPhoto?>?
    fun findFirstByOrderByIdAsc(): AlbumPhoto?
    fun findAlbumPhotoByMetadataId(metadataId: String?): MutableIterable<AlbumPhoto?>?
    fun deleteByMetadataIdAndAlbumId(metadataId: String?, albumId: Int?): Long?
    fun deleteByAlbumId(albumId: Int?): Long?
    fun deleteByMetadataId(metadataId: String?): Long?
}