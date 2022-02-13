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
   @Query("SELECT DISTINCT * FROM metadata m LEFT JOIN albumphoto ap ON m.id = ap.metadata_id LEFT JOIN useralbum ua ON ap.album_id = ua.album_id LEFT JOIN album a ON a.id = ua.album_id WHERE m.hidden = false AND ua.user_id = :userId", nativeQuery = true)
   fun findByAlbumMetadataByUserId(@Param("userId") userId: Int): MutableIterable<Metadata>

   @Query("SELECT * FROM metadata WHERE hidden = false ORDER BY year DESC, month DESC, day DESC, time DESC", nativeQuery = true)
   fun findTimelineAll(): MutableIterable<Metadata>

   @Query("SELECT * FROM metadata WHERE hidden = false ORDER BY year DESC, month DESC, day DESC, time DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findAllByOffsetAndLimit(@Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Metadata>

   fun findDistinctFirstByHiddenIsFalseOrderByYearDescMonthDescDayDesc(): Metadata?

   @Query("SELECT * FROM metadata WHERE hidden = false AND type LIKE %:type% ORDER BY year DESC, month DESC, day DESC, time DESC LIMIT 1", nativeQuery = true)
   fun findDistinctFirstByHiddenIsFalseByMediaTypeOrderByYearDescMonthDescDayDesc(@Param("type") type: String): Metadata?

   @Query("SELECT DISTINCT year,month,day FROM metadata WHERE hidden = false ORDER BY year DESC, month DESC, day DESC", nativeQuery = true)
   fun findAllYearMonthDay(): MutableIterable<MetadataDate>?

   @Query("SELECT DISTINCT year,month,day FROM metadata WHERE hidden = false AND type LIKE %:type% ORDER BY year DESC, month DESC, day DESC, time DESC", nativeQuery = true)
   fun findAllYearMonthDayByMediaType(@Param("type") type: String): MutableIterable<MetadataDate>?

   fun countMetadataById(metadataId: String): Int

   fun countAllByTypeContains(type: String): Int

   fun countAllByHiddenIsTrue(): Int

   fun countAllByLatIsNullAndLngIsNull(): Int

   @Query("SELECT camera, COUNT(*) AS count FROM metadata GROUP BY camera ORDER BY count DESC", nativeQuery = true)
   fun countByCameraType(): MutableIterable<CameraTypeCount>

   @Query("SELECT camera FROM metadata WHERE camera IS NOT NULL GROUP BY camera ORDER BY camera COLLATE NOCASE ASC", nativeQuery = true)
   fun findByCameraTypeAlphabetical(): MutableIterable<String>

   @Query("SELECT * FROM metadata WHERE hidden = true ORDER BY year DESC, month DESC, day DESC, time DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findAllByHiddenAndOffsetAndLimit(@Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Metadata>

   @Query("SELECT * FROM metadata WHERE type LIKE %:type% AND hidden = false AND DATE(added_at ) = (SELECT DATE(added_at) FROM metadata ORDER BY added_at DESC LIMIT 1) ORDER BY added_at DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findRecentByTypeOffsetAndLimit(@Param("type") type: String,@Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Metadata>

   @Query("SELECT * FROM metadata WHERE hidden = false AND DATE(added_at ) = (SELECT DATE(added_at) FROM metadata ORDER BY added_at DESC LIMIT 1) ORDER BY added_at DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findMostRecentByOffsetAndLimit(@Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Metadata>

   @Query("SELECT * FROM metadata WHERE hidden = false ORDER BY added_at DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findRecentByOffsetAndLimit(@Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Metadata>

   fun findAllByYearAndMonthAndDayAndHiddenEqualsOrderByYearDescMonthDescDayDescTimeDesc(year: Int?, month: Int?, day: Int?, hidden: Boolean?): MutableIterable<Metadata>

   @Query("SELECT * FROM metadata WHERE type LIKE %:type% AND hidden = false ORDER BY year DESC, month DESC, day DESC, time DESC", nativeQuery = true)
   fun findTimelineAllByType(@Param("type") type: String): MutableIterable<Metadata>

   @Query("SELECT * FROM metadata WHERE type LIKE %:type% AND hidden = false ORDER BY year DESC, month DESC, day DESC, time DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findAllByTypeOffsetAndLimit(@Param("type") type: String,@Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Metadata>

   @Query("SELECT * FROM metadata WHERE type LIKE %:type% AND year = :year AND month = :month AND day = :day AND hidden = false ORDER BY year DESC, month DESC, day DESC, time DESC", nativeQuery = true)
   fun findAllByTypeAndYearAndMonthAndDay(@Param("type") type: String,@Param("year") year: Int?,@Param("month") month: Int?,@Param("day") day: Int?): MutableIterable<Metadata>

   @Query("SELECT rl.*, COUNT(*) AS tagCount, m.thumbnail_url_centered AS thumbnailUrlCentered FROM metadata m INNER JOIN recognitionlabelphoto rlp ON m.id = rlp.metadata_id INNER JOIN recognitionlabel rl ON rl.id = rlp.recognition_label_id WHERE rl.name != 'object' AND m.hidden = false AND rlp.confidence >= 0.0 AND rlp.confidence <= :recognitionConfidenceThreshold GROUP BY rl.id", nativeQuery = true)
   fun findMetadataByPeople(@Param("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String): MutableIterable<MetadataPeople>

   @Query("SELECT DISTINCT m.* FROM metadata m LEFT JOIN recognitionlabelphoto rlp on m.id = rlp.metadata_id LEFT JOIN recognitionlabel rl on rlp.recognition_label_id = rl.id WHERE m.hidden = false AND rl.id = :recognitionLabelId AND rlp.confidence >= 0.0 AND rlp.confidence <= :recognitionConfidenceThreshold ORDER BY m.year DESC, m.month DESC, m.day DESC, m.time DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findMetadataByPerson(@Param("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String, @Param("recognitionLabelId") recognitionLabelId: Int, @Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Metadata>

   @Query("SELECT rl.*, COUNT(distinct m.id || rl.id) AS tagCount, m.thumbnail_url_centered AS thumbnailUrlCentered FROM metadata m LEFT JOIN albumphoto ap on m.id = ap.metadata_id LEFT JOIN useralbum ua on ap.album_id = ua.album_id LEFT JOIN recognitionlabelphoto rlp on m.id = rlp.metadata_id LEFT JOIN recognitionlabel rl on rlp.recognition_label_id = rl.id WHERE m.hidden = false AND rl.name != 'object' AND rlp.confidence >= 0.0 AND rlp.confidence <= :recognitionConfidenceThreshold AND ua.user_id = :userId GROUP BY rl.id", nativeQuery = true)
   fun findAlbumPhotoByPeople(@Param("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String,@Param("userId") userId: Int): MutableIterable<MetadataPeople>

   @Query("SELECT DISTINCT m.* FROM metadata m LEFT JOIN albumphoto ap on m.id = ap.metadata_id LEFT JOIN useralbum ua on ap.album_id = ua.album_id LEFT JOIN recognitionlabelphoto rlp on m.id = rlp.metadata_id LEFT JOIN recognitionlabel rl on rlp.recognition_label_id = rl.id WHERE m.hidden = false AND rl.id = :recognitionLabelId AND rlp.confidence >= 0.0 AND rlp.confidence <= :recognitionConfidenceThreshold AND ua.user_id = :userId ORDER BY m.year DESC, m.month DESC, m.day DESC, m.time DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findAlbumPhotoByPerson(@Param("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String, @Param("recognitionLabelId") recognitionLabelId: Int,@Param("userId") userId: Int, @Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Metadata>

   @Query("SELECT DISTINCT m.* FROM metadata m INNER JOIN recognitionlabelphoto rlp ON m.id = rlp.metadata_id WHERE m.hidden = false AND confidence > :recognitionConfidenceThreshold AND confidence < 99.0 AND rlp.recognition_label_id = :recognitionLabelId", nativeQuery = true) //  LIMIT 0, :matchScanLimit - ,@Param("matchScanLimit") matchScanLimit: Int
   fun findLowMatchesByPerson(@Param("recognitionLabelId") recognitionLabelId: Int,@Param("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String): MutableIterable<Metadata>

   @Query("SELECT DISTINCT m.* FROM metadata m WHERE m.id NOT IN (SELECT metadata_id FROM recognitionlabelphoto) AND m.hidden = false ORDER BY RANDOM() LIMIT 0, :matchScanLimit",nativeQuery = true)
   fun findNonMatched(@Param("matchScanLimit") matchScanLimit: Int): MutableIterable<Metadata>

   @Query("SELECT DISTINCT m.id as metadataId,m.type,m.path,m.thumbnail_path_small as thumbnailPathSmall,rlp.recognition_label_id as recognitionLabelId,rl.name as recognitionLabelName FROM metadata m LEFT JOIN recognitionlabelphoto rlp ON rlp.metadata_id = m.id LEFT JOIN recognitionlabel rl ON rlp.recognition_label_id = rl.id WHERE m.hidden = false AND rlp.id IN (SELECT rlp2.id FROM recognitionlabelphoto rlp2 WHERE rlp.recognition_label_id = rlp2.recognition_label_id LIMIT :trainingDataLimit) AND rlp.confidence >= 0.0 AND rlp.confidence <= :recognitionConfidenceThreshold ORDER BY rlp.recognition_label_id, RANDOM()",nativeQuery = true)
   fun findTrainingData(@Param("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String, @Param("trainingDataLimit") trainingDataLimit: Int): MutableIterable<TrainingData>

   @Query("SELECT m.folder, m.thumbnail_url_centered as thumbnailUrlCentered, (SELECT COUNT(*) FROM metadata m1 WHERE m1.folder = m.folder) as count FROM metadata m GROUP BY m.folder ORDER BY m.folder DESC", nativeQuery = true)
   fun findFolders(): MutableIterable<Folder?>?

   @Query("SELECT * FROM metadata WHERE folder = :folder AND hidden = false ORDER BY year DESC, month DESC, day DESC, time DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findAllByFolderOffsetAndLimit(@Param("folder") folder: String,@Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Metadata>

}