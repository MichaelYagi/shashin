package com.miyagi.shashin.repository

import com.miyagi.shashin.model.SearchHistory
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface SearchHistoryRepository : CrudRepository<SearchHistory?, Int?> {
    @Query("SELECT * FROM searchhistory WHERE user_id = :userId ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    fun findTopNByUserIdOrderByCreatedAtDesc(@Param("userId") userId: Int, @Param("limit") limit: Int): MutableIterable<SearchHistory>?
    @Query("SELECT * FROM searchhistory WHERE user_id = :userId ORDER BY id ASC LIMIT :limit", nativeQuery = true)
    fun findTopNByUserIdOrderByIdDesc(@Param("userId") userId: Int, @Param("limit") limit: Int): MutableIterable<SearchHistory>?
    fun countByUserIdAndTermIgnoreCase(userId: Int, term: String): Int
    fun countByUserId(userId: Int): Int
}