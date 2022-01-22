package com.miyagi.shashin.repository

import com.miyagi.shashin.model.KeywordPhoto
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

@Transactional
@Repository
interface KeywordPhotoRepository : CrudRepository<KeywordPhoto?, Int?> {
    fun countByKeywordIdAndMetadataId(keywordId: Int, metadataId: String): Int
    fun deleteAllByMetadataId(metadataId: String): Long?
}