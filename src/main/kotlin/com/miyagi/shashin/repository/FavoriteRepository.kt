package com.miyagi.shashin.repository

import com.miyagi.shashin.model.Favorite
import com.miyagi.shashin.model.Metadata
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface FavoriteRepository : CrudRepository<Favorite?, Int?> {
    fun findByMetadataIdAndUserId(metdataId: String?, userId: Int?): Favorite?
    @Query("SELECT f.* FROM favorite f INNER JOIN metadata m on m.id = f.metadata_id WHERE f.user_id = :userId ORDER BY m.year DESC, m.month DESC, m.day DESC, m.time DESC LIMIT :offset, :limit ", nativeQuery = true)
    fun findAllByUserIdAndOffsetAndLimit(@Param("userId") userId: Int, @Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Favorite?>?
    fun findAllByUserId(userId: Int?): MutableIterable<Favorite?>?
    fun findAllByMetadataIdAndUserId(metadataId: String?, userId: Int?): MutableIterable<Favorite?>?
    @Cacheable("favoritesByMetadata")
    fun findAllByMetadataId(metadataId: String?): MutableIterable<Favorite?>?
    @Cacheable("favoriteCountsByMetadata")
    fun countAllByMetadataId(metadataId: String): Int
    fun deleteByMetadataIdAndUserId(metadataId: String?, userId: Int?): Long
    fun deleteByUserId(userId: Int?): Long
    fun deleteByMetadataId(metadataId: String?): Long
}