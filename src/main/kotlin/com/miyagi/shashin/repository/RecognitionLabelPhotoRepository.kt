package com.miyagi.shashin.repository

import com.miyagi.shashin.model.AlbumPhoto
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.RecognitionLabelId
import com.miyagi.shashin.model.RecognitionLabelPhoto
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Transactional
@Repository
interface RecognitionLabelPhotoRepository : CrudRepository<RecognitionLabelPhoto?, Int?> {
    fun countByRecognitionLabelIdAndMetadataId(recognitionLabelId: Int,metadataId: String): Int
    fun countByMetadataId(metadataId: String?): Int?
    fun countByRecognitionLabelId(recognitionLabelId: Int): Int

    fun findFirstByRecognitionLabelId(@Param("recognitionLabelId") recognitionLabelId: Int): RecognitionLabelPhoto?

    @Query("SELECT recognition_label_id as recognitionLabelId FROM recognitionlabelphoto GROUP BY recognition_label_id", nativeQuery = true)
    fun findGroupByRecognitionLabelId(): MutableIterable<RecognitionLabelId>

    @Query("SELECT COUNT(DISTINCT metadata_id) FROM recognitionlabelphoto", nativeQuery = true)
    fun countDistinctMetadataId(): Int

    fun deleteByMetadataId(metadataId: String): Int

    fun deleteByRecognitionLabelIdAndMetadataId(recognitionLabelId: Int,metadataId: String): Int

    fun findByMetadataId(metadataId: String): MutableIterable<RecognitionLabelPhoto>

    fun findByCompreFaceImageId(compreFaceImageId: String): RecognitionLabelPhoto?

    fun findByRecognitionLabelIdAndMetadataId(recognitionLabelId: Int,metadataId: String): RecognitionLabelPhoto?

}