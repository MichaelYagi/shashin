package com.miyagi.shashin.repository

import com.miyagi.shashin.model.*
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

   @Query("SELECT * FROM metadata ORDER BY year DESC, month DESC, day DESC, time DESC", nativeQuery = true)
   fun findTimelineAll(): MutableIterable<Metadata>

   @Query("SELECT * FROM metadata ORDER BY year DESC, month DESC, day DESC, time DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findAllByOffsetAndLimit(@Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Metadata>

   fun findAllByYearAndMonthAndDayOrderByYearDescMonthDescDayDescTimeDesc(year: Int?, month: Int?, day: Int?): MutableIterable<Metadata>

   @Query("SELECT year,month,day FROM metadata GROUP BY year, month, day ORDER BY year DESC, month DESC, day DESC, time DESC", nativeQuery = true)
   fun findTimelineData(): MutableIterable<TimelineData>

   @Query("SELECT * FROM metadata WHERE type LIKE %:type% ORDER BY year DESC, month DESC, day DESC, time DESC", nativeQuery = true)
   fun findTimelineAllByType(@Param("type") type: String): MutableIterable<Metadata>

   @Query("SELECT * FROM metadata WHERE type LIKE %:type% ORDER BY year DESC, month DESC, day DESC, time DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findAllByTypeOffsetAndLimit(@Param("type") type: String,@Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Metadata>

   @Query("SELECT * FROM metadata WHERE type LIKE %:type% AND year = :year AND month = :month AND day = :day ORDER BY year DESC, month DESC, day DESC, time DESC", nativeQuery = true)
   fun findAllByTypeAndYearAndMonthAndDay(@Param("type") type: String,@Param("year") year: Int?,@Param("month") month: Int?,@Param("day") day: Int?): MutableIterable<Metadata>

   @Query("SELECT rl.*, COUNT(*) AS tagCount, m.thumbnail_url_centered AS thumbnailUrlCentered FROM metadata m INNER JOIN recognitionlabelphoto rlp ON m.id = rlp.metadata_id INNER JOIN recognitionlabel rl ON rl.id = rlp.recognition_label_id WHERE rl.name != 'object'  AND rlp.confidence >= 0.0 AND rlp.confidence <= :recognitionConfidenceThreshold GROUP BY rl.id", nativeQuery = true)
   fun findMetadataByPeople(@Param("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String): MutableIterable<MetadataPeople>

   @Query("SELECT DISTINCT m.* FROM metadata m LEFT JOIN recognitionlabelphoto rlp on m.id = rlp.metadata_id LEFT JOIN recognitionlabel rl on rlp.recognition_label_id = rl.id WHERE rl.id = :recognitionLabelId AND rlp.confidence >= 0.0 AND rlp.confidence <= :recognitionConfidenceThreshold LIMIT :offset, :limit", nativeQuery = true)
   fun findMetadataByPerson(@Param("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String, @Param("recognitionLabelId") recognitionLabelId: Int, @Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Metadata>

   @Query("SELECT rl.*, COUNT(distinct m.id || rl.id) AS tagCount, m.thumbnail_url_centered AS thumbnailUrlCentered FROM metadata m LEFT JOIN albumphoto ap on m.id = ap.metadata_id LEFT JOIN useralbum ua on ap.album_id = ua.album_id LEFT JOIN recognitionlabelphoto rlp on m.id = rlp.metadata_id LEFT JOIN recognitionlabel rl on rlp.recognition_label_id = rl.id WHERE rl.name != 'object' AND rlp.confidence >= 0.0 AND rlp.confidence <= :recognitionConfidenceThreshold AND ua.user_id = :userId GROUP BY rl.id", nativeQuery = true)
   fun findAlbumPhotoByPeople(@Param("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String,@Param("userId") userId: Int): MutableIterable<MetadataPeople>

   @Query("SELECT DISTINCT m.* FROM metadata m LEFT JOIN albumphoto ap on m.id = ap.metadata_id LEFT JOIN useralbum ua on ap.album_id = ua.album_id LEFT JOIN recognitionlabelphoto rlp on m.id = rlp.metadata_id LEFT JOIN recognitionlabel rl on rlp.recognition_label_id = rl.id WHERE rl.id = :recognitionLabelId AND rlp.confidence >= 0.0 AND rlp.confidence <= :recognitionConfidenceThreshold AND ua.user_id = :userId LIMIT :offset, :limit", nativeQuery = true)
   fun findAlbumPhotoByPerson(@Param("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String, @Param("recognitionLabelId") recognitionLabelId: Int,@Param("userId") userId: Int, @Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Metadata>

   @Query("SELECT DISTINCT m.* FROM metadata m INNER JOIN recognitionlabelphoto rlp ON m.id = rlp.metadata_id WHERE confidence > :recognitionConfidenceThreshold AND confidence < 99.0 AND rlp.recognition_label_id = :recognitionLabelId", nativeQuery = true) //  LIMIT 0, :matchScanLimit - ,@Param("matchScanLimit") matchScanLimit: Int
   fun findLowMatchesByPerson(@Param("recognitionLabelId") recognitionLabelId: Int,@Param("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String): MutableIterable<Metadata>

   @Query("SELECT DISTINCT m.* FROM metadata m WHERE m.id NOT IN (SELECT metadata_id FROM recognitionlabelphoto) LIMIT 0, :matchScanLimit",nativeQuery = true)
   fun findNonMatched(@Param("matchScanLimit") matchScanLimit: Int): MutableIterable<Metadata>

   @Query("SELECT DISTINCT m.id as metadataId,m.type,m.path,m.thumbnail_path_small as thumbnailPathSmall,rlp.recognition_label_id as recognitionLabelId,rl.name as recognitionLabelName FROM metadata m LEFT JOIN recognitionlabelphoto rlp ON rlp.metadata_id = m.id LEFT JOIN recognitionlabel rl ON rlp.recognition_label_id = rl.id WHERE rlp.id IN (SELECT rlp2.id FROM recognitionlabelphoto rlp2 WHERE rlp.recognition_label_id = rlp2.recognition_label_id LIMIT :trainingDataLimit) AND rlp.confidence >= 0.0 AND rlp.confidence <= :recognitionConfidenceThreshold ORDER BY rlp.recognition_label_id",nativeQuery = true)
   fun findTrainingData(@Param("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String, @Param("trainingDataLimit") trainingDataLimit: Int): MutableIterable<TrainingData>
}