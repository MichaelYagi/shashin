package com.miyagi.shashin.repository

import com.miyagi.shashin.model.AlbumPhoto
import com.miyagi.shashin.model.Favorite
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

@Repository
@Transactional
interface AlbumPhotoRepository : CrudRepository<AlbumPhoto?, Int?> {
    fun countByMetadataIdAndAlbumId(metadataId: String?, albumId: Int?): Int?
    fun countByAlbumId(albumId: Int?): Int?
    @Query("SELECT COUNT(DISTINCT ap.metadata_id) FROM albumphoto ap, metadata m WHERE ap.album_id = :albumId  AND ap.metadata_id = m.id AND m.type NOT LIKE '%video%'", nativeQuery = true)
    fun countPhotosByAlbumId(albumId: Int?): Int?
    @Query("SELECT COUNT(DISTINCT ap.metadata_id) FROM albumphoto ap, metadata m WHERE ap.album_id = :albumId  AND ap.metadata_id = m.id AND m.type LIKE '%video%'", nativeQuery = true)
    fun countVideosByAlbumId(albumId: Int?): Int?
    @Query("SELECT DISTINCT ap.* FROM albumphoto ap, metadata m WHERE ap.album_id = :albumId  AND ap.metadata_id = m.id ORDER BY m.year DESC, m.month DESC, m.day DESC, m.time DESC", nativeQuery = true)
    fun findAllByAlbumId(@Param("albumId") albumId: Int): MutableIterable<AlbumPhoto?>?
    @Query("SELECT DISTINCT ap.* FROM albumphoto ap, metadata m WHERE ap.album_id = :albumId  AND ap.metadata_id = m.id ORDER BY m.year DESC, m.month DESC, m.day DESC, m.time DESC LIMIT :offset, :limit", nativeQuery = true)
    fun findAllByAlbumIdAndOffsetAndLimit(@Param("albumId") albumId: Int, @Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<AlbumPhoto?>?
    fun findFirstByAlbumId(@Param("albumId") albumId: Int): AlbumPhoto?
    fun findFirstByOrderByIdAsc(): AlbumPhoto?
    fun findAlbumPhotoByMetadataId(metadataId: String?): MutableIterable<AlbumPhoto?>?
    fun deleteByMetadataIdAndAlbumId(metadataId: String?, albumId: Int?): Long?
    fun deleteByAlbumId(albumId: Int?): Long?
    fun deleteByMetadataId(metadataId: String?): Long?
}