package com.miyagi.shashin.repository

import com.miyagi.shashin.model.AlbumPhotoCount
import com.miyagi.shashin.model.Keyword
import com.miyagi.shashin.model.Metadata
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface KeywordRepository : CrudRepository<Keyword?, Int?> {
    fun countByKeywordIgnoreCase(keyword: String?): Int
    fun findByKeywordIgnoreCase(keyword: String?): Keyword?
    @Query("SELECT k.* FROM keyword k INNER JOIN keywordphoto kp on kp.keyword_id = k.id WHERE kp.metadata_id = :metadataId", nativeQuery = true)
    fun findKeywordsByMetadataId(metadataId: String): MutableIterable<Keyword>
}