package com.miyagi.shashin.repository

import com.miyagi.shashin.model.*
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface KeywordRepository : CrudRepository<Keyword?, Int?> {
    @Query("SELECT k.keyword, COUNT(kp.keyword_id) as count FROM keyword k INNER JOIN keywordphoto kp on k.id = kp.keyword_id GROUP BY kp.keyword_id", nativeQuery = true)
    fun countByKeyword(): MutableIterable<KeywordCount>
    fun countByKeywordIgnoreCase(keyword: String?): Int
    fun findByKeywordIgnoreCase(keyword: String?): Keyword?
    @Query("SELECT k.* FROM keyword k INNER JOIN keywordphoto kp on kp.keyword_id = k.id WHERE kp.metadata_id = :metadataId", nativeQuery = true)
    fun findKeywordsByMetadataId(metadataId: String): MutableIterable<Keyword>
    @Query("SELECT kp.metadata_id AS metadataId, GROUP_CONCAT(k.keyword) AS keywords FROM keyword k INNER JOIN keywordphoto kp on k.id = kp.keyword_id GROUP BY kp.metadata_id", nativeQuery = true)
    fun findAllKeywordsGroupedByMetadataId(): MutableIterable<KeywordsMetadata>
    @Query("SELECT id FROM keyword WHERE id NOT IN (SELECT DISTINCT keyword_id FROM keywordphoto)", nativeQuery = true)
    fun findAllOrphanedKeywordIds(): MutableIterable<Int>
}