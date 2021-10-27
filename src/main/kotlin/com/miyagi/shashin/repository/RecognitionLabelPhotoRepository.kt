package com.miyagi.shashin.repository

import com.miyagi.shashin.model.MetadataPeople
import com.miyagi.shashin.model.RecognitionLabelId
import com.miyagi.shashin.model.RecognitionLabelPhoto
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

@Transactional
interface RecognitionLabelPhotoRepository : CrudRepository<RecognitionLabelPhoto?, Int?> {
    fun countByRecognitionLabelIdAndMetadataId(recognitionLabelId: Int,metadataId: String): Int

    fun countByRecognitionLabelId(recognitionLabelId: Int): Int

    @Query("SELECT recognition_label_id as recognitionLabelId FROM recognitionlabelphoto GROUP BY recognition_label_id", nativeQuery = true)
    fun findGroupByRecognitionLabelId(): MutableIterable<RecognitionLabelId>

    @Query("SELECT COUNT(DISTINCT metadata_id) FROM recognitionlabelphoto", nativeQuery = true)
    fun countDistinctMetadataId(): Int

    fun deleteByMetadataId(metadataId: String): Int

    fun findByMetadataId(metadataId: String): MutableIterable<RecognitionLabelPhoto>

    fun findByRecognitionLabelIdAndMetadataId(recognitionLabelId: Int,metadataId: String): RecognitionLabelPhoto
}