package com.miyagi.shashin.repository

import com.miyagi.shashin.model.RecognitionLabelPhoto
import org.springframework.data.repository.CrudRepository

interface RecognitionLabelPhotoRepository : CrudRepository<RecognitionLabelPhoto?, Int?> {
    fun countByRecognitionLabelIdAndAndMetadataId(recognitionLabelId: Int,metadataId: String): Int
    fun deleteByMetadataId(metadataId: String): Int
    fun findByMetadataId(metadataId: String): MutableIterable<RecognitionLabelPhoto>
}