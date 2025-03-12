package com.miyagi.shashin.repository

import com.miyagi.shashin.model.SearchHistory
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface SearchHistoryRepository : CrudRepository<SearchHistory?, Int?> {
    @Query("SELECT * FROM searchhistory WHERE user_id = :userId AND search_type = :type ORDER BY modified_at DESC LIMIT :limit", nativeQuery = true)
    fun findTopNByUserIdOrderByModifiedAtDesc(@Param("userId") userId: Int, @Param("limit") limit: Int, type: Int): MutableIterable<SearchHistory>?
    @Query("SELECT * FROM searchhistory WHERE user_id = :userId AND search_type = :type ORDER BY id ASC LIMIT :limit", nativeQuery = true)
    fun findTopNByUserIdOrderByIdAsc(@Param("userId") userId: Int, @Param("limit") limit: Int, type: Int): MutableIterable<SearchHistory>?
    @Query("SELECT COUNT(*) FROM searchhistory WHERE user_id = :userId AND LOWER(term) = LOWER(:term) AND search_type = :type", nativeQuery = true)
    fun countByUserIdAndTermIgnoreCase(userId: Int, term: String, type: Int): Int
    @Query("SELECT COUNT(*) FROM searchhistory WHERE user_id = :userId AND search_type = :type", nativeQuery = true)
    fun countByUserId(userId: Int, type: Int): Int
    @Query("SELECT * FROM searchhistory WHERE user_id = :userId AND LOWER(term) = LOWER(:term) AND search_type = :type GROUP BY user_id, LOWER(term)", nativeQuery = true)
    fun findDistinctByUserIdAndTerm(@Param("userId") userId: Int, term: String, type: Int): SearchHistory?
    fun deleteByIdAndSearchType(id: Int, type: Int): Long
}