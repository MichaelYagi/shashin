package com.miyagi.shashin.repository

import com.miyagi.shashin.model.SearchHistory
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface SearchHistoryRepository : CrudRepository<SearchHistory?, Int?> {
    @Query("SELECT * FROM searchhistory WHERE user_id = :userId AND search_type = 1 ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    fun findTopNByUserIdOrderByCreatedAtDesc(@Param("userId") userId: Int, @Param("limit") limit: Int): MutableIterable<SearchHistory>?
    @Query("SELECT * FROM searchhistory WHERE user_id = :userId AND search_type = 1 ORDER BY id DESC LIMIT :limit", nativeQuery = true)
    fun findTopNByUserIdOrderByIdDesc(@Param("userId") userId: Int, @Param("limit") limit: Int): MutableIterable<SearchHistory>?
    @Query("SELECT COUNT(*) FROM searchhistory WHERE user_id = :userId AND LOWER(term) = LOWER(:term) AND search_type = 1", nativeQuery = true)
    fun countByUserIdAndTermIgnoreCase(userId: Int, term: String): Int
    @Query("SELECT COUNT(*) FROM searchhistory WHERE user_id = :userId AND search_type = 1", nativeQuery = true)
    fun countByUserId(userId: Int): Int
    @Query("SELECT * FROM searchhistory WHERE user_id = :userId AND LOWER(term) = LOWER(:term) AND search_type = 1 GROUP BY user_id, LOWER(term)", nativeQuery = true)
    fun findDistinctByUserIdAndTerm(@Param("userId") userId: Int, term: String): SearchHistory?
}