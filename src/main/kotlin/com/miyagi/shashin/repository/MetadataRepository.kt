package com.miyagi.shashin.repository

import com.miyagi.shashin.model.AlbumPhotoComments
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.MetadataPeople
import com.miyagi.shashin.model.TrainingData
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MetadataRepository : PagingAndSortingRepository<Metadata?, String?> {
   @Query("SELECT DISTINCT * FROM metadata m LEFT JOIN albumphoto ap ON m.id = ap.metadata_id LEFT JOIN useralbum ua ON ap.album_id = ua.album_id LEFT JOIN album a ON a.id = ua.album_id WHERE ua.user_id = :userId", nativeQuery = true)
   fun findByAlbumMetadataByUserId(@Param("userId") userId: Int): MutableIterable<Metadata>
   @Query("SELECT * FROM metadata ORDER BY year DESC, month DESC, day DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findAllByOffsetAndLimit(@Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Metadata>
   @Query("SELECT rl.*, COUNT(*) AS tagCount, m.thumbnail_url_centered AS thumbnailUrlCentered FROM metadata m INNER JOIN recognitionlabelphoto rlp ON m.id = rlp.metadata_id INNER JOIN recognitionlabel rl ON rl.id = rlp.recognition_label_id GROUP BY rl.id", nativeQuery = true)
   fun findMetadataByPeople(): MutableIterable<MetadataPeople>
   @Query("SELECT DISTINCT m.* FROM metadata m LEFT JOIN recognitionlabelphoto rlp on m.id = rlp.metadata_id LEFT JOIN recognitionlabel rl on rlp.recognition_label_id = rl.id WHERE rl.id = :recognitionLabelId AND rlp.confidence >= 0.0 AND rlp.confidence <= :recognitionConfidenceThreshold LIMIT :offset, :limit", nativeQuery = true)
   fun findMetadataByPerson(@Param("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String, @Param("recognitionLabelId") recognitionLabelId: Int, @Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Metadata>
   @Query("SELECT rl.*, COUNT(*) AS tagCount, m.thumbnail_url_centered AS thumbnailUrlCentered FROM metadata m INNER JOIN recognitionlabelphoto rlp ON m.id = rlp.metadata_id INNER JOIN recognitionlabel rl ON rl.id = rlp.recognition_label_id INNER JOIN albumphoto ap ON ap.metadata_id = m.id GROUP BY rl.id", nativeQuery = true)
   fun findAlbumPhotoByPeople(): MutableIterable<MetadataPeople>
   @Query("SELECT DISTINCT m.* FROM metadata m LEFT JOIN albumphoto ap on m.id = ap.metadata_id LEFT JOIN useralbum ua on ap.album_id = ua.album_id LEFT JOIN recognitionlabelphoto rlp on m.id = rlp.metadata_id LEFT JOIN recognitionlabel rl on rlp.recognition_label_id = rl.id WHERE rl.id = :recognitionLabelId AND rlp.confidence >= 0.0 AND rlp.confidence <= :recognitionConfidenceThreshold AND ua.user_id = :userId LIMIT :offset, :limit", nativeQuery = true)
   fun findAlbumPhotoByPerson(@Param("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String, @Param("recognitionLabelId") recognitionLabelId: Int,@Param("userId") userId: Int, @Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Metadata>
   @Query("SELECT DISTINCT m.* FROM metadata m LEFT JOIN recognitionlabelphoto rlp ON m.id = rlp.metadata_id WHERE confidence > :recognitionConfidenceThreshold AND confidence < 100.0 AND rlp.recognition_label_id = :recognitionLabelId LIMIT 0, :matchScanLimit", nativeQuery = true)
   fun findLowMatchesByPerson(@Param("recognitionLabelId") recognitionLabelId: Int,@Param("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String,@Param("matchScanLimit") matchScanLimit: Int): MutableIterable<Metadata>
   @Query("SELECT DISTINCT m.* FROM metadata m WHERE m.id NOT IN (SELECT metadata_id FROM recognitionlabelphoto) LIMIT 0, :matchScanLimit",nativeQuery = true)
   fun findNonMatched(@Param("matchScanLimit") matchScanLimit: Int): MutableIterable<Metadata>
   @Query("SELECT DISTINCT m.id as metadataId,m.type,m.path,m.thumbnail_path_small as thumbnailPathSmall,rlp.recognition_label_id as recognitionLabelId,rl.name as recognitionLabelName FROM metadata m LEFT JOIN recognitionlabelphoto rlp ON rlp.metadata_id = m.id LEFT JOIN recognitionlabel rl ON rlp.recognition_label_id = rl.id WHERE rlp.id IN (SELECT rlp2.id FROM recognitionlabelphoto rlp2 WHERE rlp.recognition_label_id = rlp2.recognition_label_id LIMIT :trainingDataLimit) AND rlp.confidence >= 0.0 AND rlp.confidence <= :recognitionConfidenceThreshold ORDER BY rlp.recognition_label_id",nativeQuery = true)
   fun findTrainingData(@Param("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String, @Param("trainingDataLimit") trainingDataLimit: Int): MutableIterable<TrainingData>
}