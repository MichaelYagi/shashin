package com.miyagi.shashin.repository

import com.miyagi.shashin.model.Favorite
import com.miyagi.shashin.model.Metadata
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface FavoriteRepository : CrudRepository<Favorite?, Int?> {
    fun findByMetadataIdAndUserId(metdataId: String?, userId: Int?): Favorite?
    @Query("SELECT * FROM favorite WHERE user_id = :userId LIMIT :offset, :limit", nativeQuery = true)
    fun findAllByUserIdAndOffsetAndLimit(@Param("userId") userId: Int, @Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Favorite?>?
    fun findAllByUserId(userId: Int?): MutableIterable<Favorite?>?
    fun deleteByMetadataIdAndUserId(metdataId: String?, userId: Int?): Long
    fun deleteByUserId(userId: Int?): Long
}