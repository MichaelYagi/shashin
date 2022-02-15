package com.miyagi.shashin.repository

import com.miyagi.shashin.model.Favorite
import com.miyagi.shashin.model.FavoriteCount
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

@Transactional
@Repository
interface FavoriteRepository : CrudRepository<Favorite?, Int?> {
    fun findByMetadataIdAndUserId(metdataId: String?, userId: Int?): Favorite?
    @Query("SELECT f.* FROM favorite f INNER JOIN metadata m on m.id = f.metadata_id WHERE f.user_id = :userId ORDER BY m.year DESC, m.month DESC, m.day DESC, m.time DESC LIMIT :offset, :limit", nativeQuery = true)
    fun findAllByUserIdAndOffsetAndLimit(@Param("userId") userId: Int, @Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Favorite?>?
    fun findAllByUserId(userId: Int?): MutableIterable<Favorite?>?
    fun findAllByMetadataId(metadataId: String?): MutableIterable<Favorite?>?
    @Query("SELECT f.id,f.metadata_id as metadataId,f.user_id as userId,f.created_at as createdAt,f.modified_at as modifiedAt,(SELECT COUNT(*) FROM favorite f2 WHERE f.metadata_id = f2.metadata_id) AS count FROM favorite f WHERE f.metadata_id IN :metadataIds", nativeQuery = true)
    fun countByMetadataIdIn(metadataIds: List<String>): MutableIterable<FavoriteCount>
    fun countAllByMetadataId(metadataId: String): Int
    fun deleteByMetadataIdAndUserId(metadataId: String?, userId: Int?): Long
    fun deleteByUserId(userId: Int?): Long
    fun deleteByMetadataId(metadataId: String?): Long
}