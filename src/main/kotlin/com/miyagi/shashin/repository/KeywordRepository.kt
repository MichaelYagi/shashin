package com.miyagi.shashin.repository

import com.miyagi.shashin.model.*
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface KeywordRepository : CrudRepository<Keyword?, Int?> {
    @Cacheable(value = ["dashboardKeywordStats"], key = "'keywords'")
    @Query("SELECT k.keyword, COUNT(kp.keyword_id) as count FROM keyword k INNER JOIN keywordphoto kp on k.id = kp.keyword_id WHERE k.keyword != 'unidentified objects' GROUP BY kp.keyword_id ORDER BY count DESC", nativeQuery = true)
    fun countByKeyword(): List<KeywordCount>
    fun countByKeywordIgnoreCase(keyword: String?): Int
    fun findByKeywordIgnoreCase(keyword: String?): Keyword?
    @Query("SELECT k.* FROM keyword k INNER JOIN keywordphoto kp on kp.keyword_id = k.id WHERE kp.metadata_id = :metadataId", nativeQuery = true)
    fun findKeywordsByMetadataId(metadataId: String): MutableIterable<Keyword>
    @Query("SELECT kp.metadata_id AS metadataId, GROUP_CONCAT(k.keyword) AS keywords FROM keyword k INNER JOIN keywordphoto kp on k.id = kp.keyword_id WHERE k.keyword != 'unidentified objects' AND kp.metadata_id IN :metadataIds GROUP BY kp.metadata_id", nativeQuery = true)
    fun findAllKeywordsGroupedByMetadataIds(metadataIds: MutableList<String>): MutableIterable<KeywordsMetadata>
    @Query("SELECT kp.metadata_id AS metadataId, GROUP_CONCAT(k.keyword) AS keywords FROM keyword k INNER JOIN keywordphoto kp on k.id = kp.keyword_id WHERE k.keyword != 'unidentified objects' GROUP BY kp.metadata_id", nativeQuery = true)
    fun findAllKeywordsGroupedByMetadataId(): MutableIterable<KeywordsMetadata>
    @Query("SELECT DISTINCT k.id FROM keyword k LEFT JOIN keywordphoto kp ON kp.keyword_id = k.id WHERE kp.keyword_id IS NULL", nativeQuery = true)
    fun findAllOrphanedKeywordIds(): MutableIterable<Int>
    @Query("SELECT DISTINCT * FROM keyword ORDER BY keyword COLLATE NOCASE ASC", nativeQuery = true)
    fun findAllDistinctOrderByKeyword(): MutableIterable<Keyword>
}