package com.miyagi.shashin.repository

import com.miyagi.shashin.model.Favorite
import com.miyagi.shashin.model.FavoriteCount
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import jakarta.transaction.Transactional

@Transactional
@Repository
interface FavoriteRepository : CrudRepository<Favorite?, Int?> {
    fun findByMetadataIdAndUserId(metadataId: String?, userId: Int?): Favorite?
    @Query("SELECT COUNT(f.metadata_id) FROM favorite f INNER JOIN metadata m on m.id = f.metadata_id WHERE f.user_id = :userId AND m.hidden = 0", nativeQuery = true)
    fun countAllByUserId(@Param("userId") userId: Int): Int?
    @Query("SELECT f.* FROM favorite f INNER JOIN metadata m on m.id = f.metadata_id WHERE f.user_id = :userId AND m.hidden = 0 ORDER BY m.year DESC, m.month DESC, m.day DESC, m.time DESC LIMIT :offset, :limit", nativeQuery = true)
    fun findAllByUserIdAndOffsetAndLimit(@Param("userId") userId: Int, @Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Favorite?>?
    @Query("SELECT f.* FROM favorite f INNER JOIN metadata m on m.id = f.metadata_id WHERE f.user_id = :userId AND m.type LIKE %:type% AND m.hidden = 0 ORDER BY m.year DESC, m.month DESC, m.day DESC, m.time DESC LIMIT :offset, :limit", nativeQuery = true)
    fun findAllByUserIdAndMediaTypeAndOffsetAndLimit(@Param("userId") userId: Int, @Param("type") type: String, @Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Favorite?>?
    @Query("SELECT COUNT(f.metadata_id) FROM favorite f INNER JOIN metadata m on m.id = f.metadata_id WHERE f.user_id = :userId AND m.type LIKE %:type% AND m.hidden = 0", nativeQuery = true)
    fun countAllByUserIdAndMediaType(@Param("userId") userId: Int, @Param("type") type: String): Int?
    @Query("SELECT f.* FROM favorite f INNER JOIN metadata m on m.id = f.metadata_id WHERE f.user_id = :userId AND ((m.lat IS NULL OR m.lat == \"\") OR (m.lng IS NULL OR m.lng == \"\")) AND m.hidden = 0 ORDER BY m.year DESC, m.month DESC, m.day DESC, m.time DESC LIMIT :offset, :limit", nativeQuery = true)
    fun findAllByUserIdAndNoCoordAndOffsetAndLimit(@Param("userId") userId: Int, @Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Favorite?>?
    @Query("SELECT f.* FROM favorite f INNER JOIN metadata m on m.id = f.metadata_id WHERE f.user_id = :userId AND m.description IS NOT NULL AND m.description != \"\" AND m.hidden = 0 ORDER BY m.year DESC, m.month DESC, m.day DESC, m.time DESC LIMIT :offset, :limit", nativeQuery = true)
    fun findAllByUserIdAndDescriptionAndOffsetAndLimit(@Param("userId") userId: Int, @Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Favorite?>?
    @Query("SELECT COUNT(f.metadata_id) FROM favorite f INNER JOIN metadata m on m.id = f.metadata_id WHERE f.user_id = :userId AND ((m.lat IS NULL OR m.lat == \"\") OR (m.lng IS NULL OR m.lng == \"\")) AND m.hidden = 0", nativeQuery = true)
    fun countAllByUserIdAndNoCoord(@Param("userId") userId: Int): Int?
    @Query("SELECT COUNT(f.metadata_id) FROM favorite f INNER JOIN metadata m on m.id = f.metadata_id WHERE f.user_id = :userId AND m.description IS NOT NULL AND m.description != \"\" AND m.hidden = 0", nativeQuery = true)
    fun countAllByUserIdAndDescription(@Param("userId") userId: Int): Int?
    fun findAllByMetadataId(metadataId: String?): MutableIterable<Favorite?>?
    @Query("SELECT f.id,f.metadata_id as metadataId,f.user_id as userId,f.created_at as createdAt,f.modified_at as modifiedAt,(SELECT COUNT(*) FROM favorite f2 WHERE f.metadata_id = f2.metadata_id) AS count FROM favorite f WHERE f.metadata_id IN :metadataIds", nativeQuery = true)
    fun countByMetadataIdIn(metadataIds: List<String>): MutableIterable<FavoriteCount>
    fun countAllByMetadataId(metadataId: String): Int
    fun deleteByMetadataIdAndUserId(metadataId: String?, userId: Int?): Long
    fun deleteByUserId(userId: Int?): Long
    fun deleteByMetadataId(metadataId: String?): Long
}