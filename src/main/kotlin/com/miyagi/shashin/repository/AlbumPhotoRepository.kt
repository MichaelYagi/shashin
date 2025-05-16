package com.miyagi.shashin.repository

import com.miyagi.shashin.model.AlbumPhoto
import com.miyagi.shashin.model.Favorite
import com.miyagi.shashin.model.Metadata
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import jakarta.transaction.Transactional

@Repository
@Transactional
interface AlbumPhotoRepository : CrudRepository<AlbumPhoto?, Int?> {
    fun countByMetadataIdAndAlbumId(metadataId: String?, albumId: Int?): Int?
    fun countByMetadataId(metadataId: String?): Int?
    fun countByAlbumId(albumId: Int?): Int?
    @Query("SELECT COUNT(DISTINCT ap.metadata_id) FROM albumphoto ap, metadata m WHERE ap.album_id = :albumId AND ap.metadata_id = m.id AND m.type LIKE %:type% AND m.hidden = 0", nativeQuery = true)
    fun countAlbumIdAndMediaType(@Param("albumId") albumId: Int, @Param("type") type: String): Int?
    @Query("SELECT COUNT(DISTINCT ap.metadata_id) FROM albumphoto ap, metadata m WHERE ap.album_id = :albumId AND ap.metadata_id = m.id AND ((m.lat IS NULL OR m.lat == \"\") OR (m.lng IS NULL OR m.lng == \"\")) AND m.hidden = 0", nativeQuery = true)
    fun countAlbumIdAndNoCoord(albumId: Int?): Int?
    @Query("SELECT COUNT(DISTINCT ap.metadata_id) FROM albumphoto ap, metadata m WHERE ap.album_id = :albumId AND ap.metadata_id = m.id AND m.description IS NOT NULL AND m.description != \"\" AND m.hidden = 0", nativeQuery = true)
    fun countAlbumIdAndDescription(albumId: Int?): Int?
    @Query("SELECT COUNT(DISTINCT ap.metadata_id) FROM albumphoto ap, metadata m WHERE ap.album_id = :albumId AND ap.metadata_id = m.id AND m.type NOT LIKE '%video%' AND m.hidden = 0", nativeQuery = true)
    fun countPhotosByAlbumId(albumId: Int?): Int?
    @Query("SELECT COUNT(DISTINCT ap.metadata_id) FROM albumphoto ap, metadata m WHERE ap.album_id = :albumId AND ap.metadata_id = m.id AND m.type LIKE '%video%' AND m.hidden = 0", nativeQuery = true)
    fun countVideosByAlbumId(albumId: Int?): Int?
    @Query("SELECT DISTINCT ap.* FROM albumphoto ap, metadata m WHERE ap.album_id = :albumId AND ap.metadata_id = m.id AND m.hidden = 0 ORDER BY m.year DESC, m.month DESC, m.day DESC, m.time DESC", nativeQuery = true)
    fun findAllByAlbumId(@Param("albumId") albumId: Int): MutableIterable<AlbumPhoto?>?
    @Query("SELECT DISTINCT m.* FROM albumphoto ap, metadata m WHERE ap.album_id IN :albumIds AND ap.metadata_id = m.id AND m.type LIKE '%image%' AND m.hidden = 0 ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    fun findRandomImagesByAlbumIdsAndLimit(@Param("albumIds") albumIds: MutableList<Int>, @Param("limit") limit: Int): MutableIterable<Metadata>?
    @Query("SELECT DISTINCT ap.* FROM albumphoto ap, metadata m WHERE ap.album_id = :albumId AND ap.metadata_id = m.id AND m.type LIKE '%image%' AND m.hidden = 0 ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    fun findRandomImagesByAlbumIdAndLimit(@Param("albumId") albumId: Int, @Param("limit") limit: Int): MutableIterable<AlbumPhoto?>?
    @Query("SELECT DISTINCT ap.* FROM albumphoto ap, metadata m WHERE ap.album_id = :albumId AND ap.metadata_id = m.id AND m.hidden = 0 ORDER BY m.year DESC, m.month DESC, m.day DESC, m.time DESC LIMIT :offset, :limit", nativeQuery = true)
    fun findAllByAlbumIdAndOffsetAndLimit(@Param("albumId") albumId: Int, @Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<AlbumPhoto?>?
    @Query("SELECT COUNT(DISTINCT ap.metadata_id) FROM albumphoto ap, metadata m WHERE ap.album_id = :albumId AND ap.metadata_id = m.id AND m.hidden = 0", nativeQuery = true)
    fun countAlbumId(@Param("albumId") albumId: Int): Int?
    @Query("SELECT DISTINCT ap.* FROM albumphoto ap, metadata m WHERE ap.album_id = :albumId AND ap.metadata_id = m.id AND m.type LIKE %:type% AND m.hidden = 0 ORDER BY m.year DESC, m.month DESC, m.day DESC, m.time DESC LIMIT :offset, :limit", nativeQuery = true)
    fun findAllByAlbumIdAndMediaTypeAndOffsetAndLimit(@Param("albumId") albumId: Int, @Param("type") type: String, @Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<AlbumPhoto?>?
    @Query("SELECT DISTINCT ap.* FROM albumphoto ap, metadata m WHERE ap.album_id = :albumId AND ap.metadata_id = m.id AND ((m.lat IS NULL OR m.lat == \"\") OR (m.lng IS NULL OR m.lng == \"\")) AND m.hidden = 0 ORDER BY m.year DESC, m.month DESC, m.day DESC, m.time DESC LIMIT :offset, :limit", nativeQuery = true)
    fun findAllByAlbumIdAndNoCoordAndOffsetAndLimit(@Param("albumId") albumId: Int, @Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<AlbumPhoto?>?
    @Query("SELECT DISTINCT ap.* FROM albumphoto ap, metadata m WHERE ap.album_id = :albumId AND ap.metadata_id = m.id AND m.description IS NOT NULL AND m.description != \"\" AND m.hidden = 0 ORDER BY m.year DESC, m.month DESC, m.day DESC, m.time DESC LIMIT :offset, :limit", nativeQuery = true)
    fun findAllByAlbumIdAndDescriptionAndOffsetAndLimit(@Param("albumId") albumId: Int, @Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<AlbumPhoto?>?
    @Query("SELECT DISTINCT ap.* FROM albumphoto ap, metadata m, albumphotocomment apc WHERE ap.album_id = :albumId AND ap.metadata_id = m.id AND ap.album_id = apc.album_id AND apc.metadata_id = ap.metadata_id AND m.hidden = 0 ORDER BY m.year DESC, m.month DESC, m.day DESC, m.time DESC LIMIT :offset, :limit", nativeQuery = true)
    fun findAllByAlbumIdAndCommentsOnlyAndOffsetAndLimit(@Param("albumId") albumId: Int, @Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<AlbumPhoto?>?
    @Query("SELECT COUNT(DISTINCT ap.metadata_id) FROM albumphoto ap, metadata m, albumphotocomment apc WHERE ap.album_id = :albumId AND ap.metadata_id = m.id AND ap.album_id = apc.album_id AND apc.metadata_id = ap.metadata_id AND m.hidden = 0", nativeQuery = true)
    fun countByAlbumIdAndCommentsOnly(@Param("albumId") albumId: Int): Int?
    fun findFirstByAlbumId(@Param("albumId") albumId: Int): AlbumPhoto?
    fun findAlbumPhotoByMetadataId(metadataId: String?): MutableIterable<AlbumPhoto?>?
    @Query("SELECT DISTINCT ap.* FROM albumphoto ap INNER JOIN useralbum ua ON ua.album_id = ap.album_id INNER JOIN album a ON ap.album_id = a.id WHERE ap.metadata_id = :metadataId AND ua.user_id = :userId", nativeQuery = true)
    fun findAlbumPhotoByUserIdAndMetadataId(@Param("userId") userId: Int, metadataId: String?): MutableIterable<AlbumPhoto?>?
    fun deleteByMetadataIdAndAlbumId(metadataId: String?, albumId: Int?): Long?
    fun deleteByAlbumId(albumId: Int?): Long?
    fun deleteByMetadataId(metadataId: String?): Long?
    fun findAllByMetadataId(metadataId: String?): MutableIterable<AlbumPhoto?>?
}