package com.miyagi.shashin.repository

import com.miyagi.shashin.model.*
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.ListCrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MetadataRepository : ListCrudRepository<Metadata?, String?>, PagingAndSortingRepository<Metadata, String?> {
   @Query("SELECT * FROM metadata WHERE id = :metadataId", nativeQuery = true)
   fun findByMetadataId(@Param("metadataId") metadataId: String): Metadata?

   @Query("SELECT * FROM metadata WHERE hidden = 0 AND type LIKE %:type%", nativeQuery = true)
   fun findAllByMediaType(@Param("type") type: String): MutableIterable<Metadata>?

   @Cacheable(value = ["allAlbumMetadataWithCoordinates"], key = "{#userId}")
   @Query("SELECT DISTINCT m.id, m.type, m.lat, m.lng, m.year, m.month, m.day, m.thumbnail_url_small as thumbnailUrlSmall, m.thumbnail_url_original as thumbnailUrlOriginal, m.video_url as videoUrl, m.original_image_width as originalImageWidth, m.original_image_height as originalImageHeight, m.map_marker_url as mapMarkerUrl, m.place_name as placeName FROM metadata m LEFT JOIN albumphoto ap ON m.id = ap.metadata_id LEFT JOIN useralbum ua ON ap.album_id = ua.album_id LEFT JOIN album a ON a.id = ua.album_id WHERE m.hidden = 0 AND ua.user_id = :userId AND m.lat IS NOT NULL AND m.lat != \"\" AND m.lng IS NOT NULL AND m.lng != \"\"", nativeQuery = true)
   fun findByAlbumMetadataByUserIdForMap(@Param("userId") userId: Int): MutableIterable<MapData>

   @Query("SELECT * FROM metadata WHERE CAST(strftime('%s', (year || '-' || (case when month < 10 then '0' || month else month end) || '-' || (case when day < 10 then '0' || day else day end) || ' ' || time)) as integer) BETWEEN CAST(strftime('%s', :startDate) as INTEGER) AND CAST(strftime('%s', :endDate) as INTEGER) ORDER BY CAST(strftime('%s', (year || '-' || (case when month < 10 then '0' || month else month end) || '-' || (case when day < 10 then '0' || day else day end) || ' ' || time)) as integer) DESC\n", nativeQuery = true)
   fun findMetadataIdBetweenTakenAt(@Param("startDate") startDate: String, @Param("endDate") endDate: String): MutableList<Metadata>?

   @Cacheable(value = ["allAlbumMetadataWithCoordinates"], key = "{#userId}")
   @Query("SELECT DISTINCT m.id, m.type, m.lat, m.lng, m.year, m.month, m.day, m.thumbnail_url_small as thumbnailUrlSmall, m.thumbnail_url_original as thumbnailUrlOriginal, m.video_url as videoUrl, m.original_image_width as originalImageWidth, m.original_image_height as originalImageHeight, m.map_marker_url as mapMarkerUrl, m.place_name as placeName FROM metadata m LEFT JOIN albumphoto ap ON m.id = ap.metadata_id LEFT JOIN useralbum ua ON ap.album_id = ua.album_id LEFT JOIN album a ON a.id = ua.album_id WHERE m.hidden = 0 AND ua.user_id = :userId AND m.lat IS NOT NULL AND m.lat != \"\" AND m.lng IS NOT NULL AND m.lng != \"\" LIMIT :offset, :limit", nativeQuery = true)
   fun findByAlbumMetadataByUserIdForMapWithLimit(@Param("userId") userId: Int, @Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<MapData>

   @Cacheable(value = ["allAlbumMetadataWithCoordinates"], key = "{#userId}")
   @Query("SELECT DISTINCT m.id, m.type, m.lat, m.lng, m.year, m.month, m.day, m.thumbnail_url_small as thumbnailUrlSmall, m.thumbnail_url_original as thumbnailUrlOriginal, m.video_url as videoUrl, m.original_image_width as originalImageWidth, m.original_image_height as originalImageHeight, m.map_marker_url as mapMarkerUrl, m.place_name as placeName FROM metadata m LEFT JOIN albumphoto ap ON m.id = ap.metadata_id LEFT JOIN useralbum ua ON ap.album_id = ua.album_id LEFT JOIN album a ON a.id = ua.album_id WHERE m.taken_at >= :startDate AND m.taken_at <= :endDate AND m.hidden = 0 AND ua.user_id = :userId AND m.lat IS NOT NULL AND m.lat != \"\" AND m.lng IS NOT NULL AND m.lng != \"\" LIMIT :offset, :limit", nativeQuery = true)
   fun findByAlbumMetadataByUserIdDatesForMapWithLimit(@Param("userId") userId: Int, @Param("offset") offset: Int, @Param("limit") limit: Int, @Param("startDate") startDate: String, @Param("endDate") endDate: String): MutableIterable<MapData>

   @Cacheable(value = ["allMetadataWithCoordinates"])
   @Query("SELECT metadata.id, metadata.type, metadata.lat, metadata.lng, metadata.year, metadata.month, metadata.day, metadata.thumbnail_url_small as thumbnailUrlSmall, metadata.thumbnail_url_original as thumbnailUrlOriginal, metadata.video_url as videoUrl, metadata.original_image_width as originalImageWidth, metadata.original_image_height as originalImageHeight, metadata.map_marker_url as mapMarkerUrl, metadata.place_name as placeName FROM metadata WHERE hidden = 0 AND lat IS NOT NULL AND lat != \"\" AND lng IS NOT NULL AND lng != \"\" ORDER BY year DESC, month DESC, day DESC, time DESC", nativeQuery = true)
   fun findTimelineAllForMap(): MutableIterable<MapData>

   @Cacheable(value = ["allMetadataWithCoordinates"])
   @Query("SELECT metadata.id, metadata.type, metadata.lat, metadata.lng, metadata.year, metadata.month, metadata.day, metadata.thumbnail_url_small as thumbnailUrlSmall, metadata.thumbnail_url_original as thumbnailUrlOriginal, metadata.video_url as videoUrl, metadata.original_image_width as originalImageWidth, metadata.original_image_height as originalImageHeight, metadata.map_marker_url as mapMarkerUrl, metadata.place_name as placeName FROM metadata WHERE hidden = 0 AND lat IS NOT NULL AND lat != \"\" AND lng IS NOT NULL AND lng != \"\" ORDER BY year DESC, month DESC, day DESC, time DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findTimelineForMap(@Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<MapData>

   @Cacheable(value = ["allMetadataWithCoordinates"])
   @Query("SELECT metadata.id, metadata.type, metadata.lat, metadata.lng, metadata.year, metadata.month, metadata.day, metadata.thumbnail_url_small as thumbnailUrlSmall, metadata.thumbnail_url_original as thumbnailUrlOriginal, metadata.video_url as videoUrl, metadata.original_image_width as originalImageWidth, metadata.original_image_height as originalImageHeight, metadata.map_marker_url as mapMarkerUrl, metadata.place_name as placeName FROM metadata WHERE metadata.taken_at >= :startDate AND metadata.taken_at <= :endDate AND hidden = 0 AND lat IS NOT NULL AND lat != \"\" AND lng IS NOT NULL AND lng != \"\" ORDER BY year DESC, month DESC, day DESC, time DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findTimelineDatesForMap(@Param("offset") offset: Int, @Param("limit") limit: Int, @Param("startDate") startDate: String, @Param("endDate") endDate: String): MutableIterable<MapData>

   @Query("SELECT * FROM metadata WHERE thumbnail_url_centered = :thumbnailUrlCentered", nativeQuery = true)
   fun findByThumbnailCentered(@Param("thumbnailUrlCentered") thumbnailUrlCentered: String): Metadata?

   @Query("SELECT * FROM metadata WHERE hidden = 0 ORDER BY year DESC, month DESC, day DESC, time DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findAllByOffsetAndLimit(@Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Metadata>

   fun findDistinctFirstByHiddenIsFalseOrderByYearDescMonthDescDayDescTimeDesc(): Metadata?

   @Query("SELECT * FROM metadata WHERE hidden = 0 AND type LIKE %:type% ORDER BY year DESC, month DESC, day DESC, time DESC LIMIT 1", nativeQuery = true)
   fun findDistinctFirstByHiddenIsFalseByMediaTypeOrderByYearDescMonthDescDayDesc(@Param("type") type: String): Metadata?

   @Query("SELECT DISTINCT year,month,day FROM metadata WHERE hidden = 0 ORDER BY year DESC, month DESC, day DESC", nativeQuery = true)
   fun findAllYearMonthDay(): MutableIterable<MetadataDate>?

   @Query("SELECT DISTINCT year,month,day FROM metadata WHERE hidden = 0 AND type LIKE %:type% ORDER BY year DESC, month DESC, day DESC, time DESC", nativeQuery = true)
   fun findAllYearMonthDayByMediaType(@Param("type") type: String): MutableIterable<MetadataDate>?

   fun countMetadataById(metadataId: String): Int

   fun countAllByTypeContains(type: String): Int

   fun countAllByHiddenIsTrue(): Int

   fun countAllByLatIsNullAndLngIsNull(): Int

   @Query("SELECT camera, COUNT(*) AS count FROM metadata GROUP BY camera ORDER BY count DESC", nativeQuery = true)
   fun countByCameraType(): MutableIterable<CameraTypeCount>

   @Query("SELECT camera FROM metadata WHERE camera IS NOT NULL GROUP BY camera ORDER BY camera COLLATE NOCASE ASC", nativeQuery = true)
   fun findByCameraTypeAlphabetical(): MutableIterable<String>

   @Query("SELECT lens FROM metadata WHERE lens IS NOT NULL GROUP BY lens ORDER BY lens COLLATE NOCASE ASC", nativeQuery = true)
   fun findByLensTypeAlphabetical(): MutableIterable<String>

   @Query("SELECT * FROM metadata WHERE hidden = 1 ORDER BY modified_at DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findAllByHiddenAndOffsetAndLimit(@Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Metadata>

   @Query("SELECT * FROM metadata WHERE hidden = 0 ORDER BY modified_at DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findModifiedByOffsetAndLimit(@Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Metadata>

   @Query("SELECT * FROM metadata WHERE hidden = 0 ORDER BY last_accessed_at DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findLastAccessedByOffsetAndLimit(@Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Metadata>

   @Query("SELECT * FROM metadata WHERE hidden = 0 ORDER BY added_at DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findRecentByOffsetAndLimit(@Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Metadata>

   @Query("SELECT * FROM metadata WHERE hidden = 0 ORDER BY year DESC, month DESC, day DESC, time DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findTakenByOffsetAndLimit(@Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Metadata>

   @Query("SELECT * FROM metadata WHERE hidden = 0 AND type LIKE %:type% ORDER BY modified_at DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findModifiedByMediaTypeAndOffsetAndLimit(@Param("offset") offset: Int, @Param("type") type: String?, @Param("limit") limit: Int): MutableIterable<Metadata>

   @Query("SELECT * FROM metadata WHERE hidden = 0 AND type LIKE %:type% ORDER BY added_at DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findRecentByMediaTypeAndOffsetAndLimit(@Param("offset") offset: Int, @Param("type") type: String?, @Param("limit") limit: Int): MutableIterable<Metadata>

   @Query("SELECT * FROM metadata WHERE hidden = 0 AND type LIKE %:type% ORDER BY year DESC, month DESC, day DESC, time DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findTakenByMediaTypeAndOffsetAndLimit(@Param("offset") offset: Int, @Param("type") type: String?, @Param("limit") limit: Int): MutableIterable<Metadata>

   @Query("SELECT * FROM metadata WHERE hidden = 0 AND type LIKE %:type% ORDER BY last_accessed_at DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findLastAccessedByMediaTypeAndOffsetAndLimit(@Param("offset") offset: Int, @Param("type") type: String?, @Param("limit") limit: Int): MutableIterable<Metadata>

   fun findAllByYearAndMonthAndDayAndHiddenEqualsOrderByYearDescMonthDescDayDescTimeDesc(year: Int?, month: Int?, day: Int?, hidden: Boolean?): MutableIterable<Metadata>

   @Query("SELECT * FROM metadata WHERE year = :year AND month = :month AND day = :day AND hidden = 0 AND place_name IS NOT NULL ORDER BY year DESC, month DESC, day DESC, time DESC", nativeQuery = true)
   fun findTimelinePlaceByDate(year: Int?, month: Int?, day: Int?): MutableIterable<Metadata>

   @Query("SELECT * FROM metadata WHERE year = :year AND month = :month AND day = :day AND type LIKE %:type% AND hidden = 0 AND place_name IS NOT NULL ORDER BY year DESC, month DESC, day DESC, time DESC", nativeQuery = true)
   fun findTimelinePlaceByDateAndType(year: Int?, month: Int?, day: Int?, @Param("type") type: String?): MutableIterable<Metadata>

   @Query("SELECT * FROM metadata WHERE type LIKE %:type% AND hidden = 0 ORDER BY year DESC, month DESC, day DESC, time DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findAllByTypeOffsetAndLimit(@Param("type") type: String?,@Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Metadata>

   @Query("SELECT * FROM metadata WHERE type LIKE %:type% AND year = :year AND month = :month AND day = :day AND hidden = 0 ORDER BY year DESC, month DESC, day DESC, time DESC", nativeQuery = true)
   fun findAllByTypeAndYearAndMonthAndDay(@Param("type") type: String?,@Param("year") year: Int?,@Param("month") month: Int?,@Param("day") day: Int?): MutableIterable<Metadata>

   @Query("SELECT rl.id,rl.name,rl.cover_url as coverUrl, COUNT(*) AS tagCount, m.thumbnail_url_centered AS thumbnailUrlCentered FROM metadata m INNER JOIN recognitionlabelphoto rlp ON m.id = rlp.metadata_id INNER JOIN recognitionlabel rl ON rl.id = rlp.recognition_label_id WHERE rl.name != :objectName AND m.hidden = 0 AND rlp.confidence >= 0.0 AND rlp.confidence <= :recognitionConfidenceThreshold GROUP BY rl.id", nativeQuery = true)
   fun findMetadataByPeople(@Param("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String, @Param("objectName") objectName: String): MutableIterable<MetadataPeople>

   @Query("SELECT DISTINCT m.* FROM metadata m LEFT JOIN recognitionlabelphoto rlp on m.id = rlp.metadata_id LEFT JOIN recognitionlabel rl on rlp.recognition_label_id = rl.id WHERE m.hidden = 0 AND rl.id = :recognitionLabelId AND rlp.confidence >= 0.0 AND rlp.confidence <= :recognitionConfidenceThreshold ORDER BY m.year DESC, m.month DESC, m.day DESC, m.time DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findMetadataByPerson(@Param("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String, @Param("recognitionLabelId") recognitionLabelId: Int, @Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Metadata>
   @Query("SELECT DISTINCT m.* FROM metadata m LEFT JOIN recognitionlabelphoto rlp on m.id = rlp.metadata_id LEFT JOIN recognitionlabel rl on rlp.recognition_label_id = rl.id WHERE m.hidden = 0 AND rl.id = :recognitionLabelId AND rlp.confidence >= 0.0 AND rlp.confidence <= :recognitionConfidenceThreshold ORDER BY m.modified_at DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findMetadataByPersonByModified(@Param("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String, @Param("recognitionLabelId") recognitionLabelId: Int, @Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Metadata>

   @Query("SELECT rl.id,rl.name,rl.cover_url as coverUrl, COUNT(distinct m.id || rl.id) AS tagCount, m.thumbnail_url_centered AS thumbnailUrlCentered FROM metadata m LEFT JOIN albumphoto ap on m.id = ap.metadata_id LEFT JOIN useralbum ua on ap.album_id = ua.album_id LEFT JOIN recognitionlabelphoto rlp on m.id = rlp.metadata_id LEFT JOIN recognitionlabel rl on rlp.recognition_label_id = rl.id WHERE m.hidden = 0 AND rl.name != :objectName AND rlp.confidence >= 0.0 AND rlp.confidence <= :recognitionConfidenceThreshold AND ua.user_id = :userId GROUP BY rl.id", nativeQuery = true)
   fun findAlbumPhotoByPeople(@Param("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String,@Param("userId") userId: Int, @Param("objectName") objectName: String): MutableIterable<MetadataPeople>

   @Query("SELECT DISTINCT m.* FROM metadata m LEFT JOIN albumphoto ap on m.id = ap.metadata_id LEFT JOIN useralbum ua on ap.album_id = ua.album_id LEFT JOIN recognitionlabelphoto rlp on m.id = rlp.metadata_id LEFT JOIN recognitionlabel rl on rlp.recognition_label_id = rl.id WHERE m.hidden = 0 AND rl.id = :recognitionLabelId AND rlp.confidence >= 0.0 AND rlp.confidence <= :recognitionConfidenceThreshold AND ua.user_id = :userId ORDER BY m.year DESC, m.month DESC, m.day DESC, m.time DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findAlbumPhotoByPerson(@Param("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String, @Param("recognitionLabelId") recognitionLabelId: Int,@Param("userId") userId: Int, @Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Metadata>

   @Query("SELECT DISTINCT m.* FROM metadata m INNER JOIN recognitionlabelphoto rlp ON m.id = rlp.metadata_id WHERE m.hidden = 0 AND confidence > :recognitionConfidenceThreshold AND confidence < 1.0 AND confidence != 0.0 AND rlp.recognition_label_id = :recognitionLabelId", nativeQuery = true) //  LIMIT 0, :matchScanLimit - ,@Param("matchScanLimit") matchScanLimit: Int
   fun findLowMatchesByPerson(@Param("recognitionLabelId") recognitionLabelId: Int,@Param("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String): MutableIterable<Metadata>
   @Query("SELECT COUNT(*) FROM metadata m INNER JOIN recognitionlabelphoto rlp ON m.id = rlp.metadata_id WHERE m.hidden = 0 AND confidence > :recognitionConfidenceThreshold AND confidence < 1.0 AND confidence != 0.0", nativeQuery = true)
   fun findAllLowMatches(@Param("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String): Int
   @Query("SELECT DISTINCT m.* FROM metadata m LEFT JOIN keywordphoto kp ON kp.metadata_id = m.id WHERE kp.metadata_id IS NULL AND m.hidden = 0 ORDER BY RANDOM() LIMIT 0, :matchScanLimit",nativeQuery = true)
   fun findWithoutKeywords(@Param("matchScanLimit") matchScanLimit: Int): MutableIterable<Metadata>
   @Query("SELECT DISTINCT m.* FROM metadata m LEFT JOIN recognitionlabelphoto rlp ON rlp.metadata_id = m.id WHERE rlp.metadata_id IS NULL AND m.hidden = 0 ORDER BY RANDOM() LIMIT 0, :matchScanLimit",nativeQuery = true)
   fun findNonMatched(@Param("matchScanLimit") matchScanLimit: Int): MutableIterable<Metadata>

   @Query("SELECT COUNT(DISTINCT folder) FROM metadata WHERE hidden = 0", nativeQuery = true)
   fun countByFolder(): Int

   @Query("SELECT m.folder, (SELECT mid FROM folderdata WHERE folder = m.folder) as metadataId, (SELECT COUNT(*) FROM metadata m1 WHERE m1.folder = m.folder AND m1.hidden = 0) as count FROM metadata m WHERE m.hidden = 0 GROUP BY m.folder ORDER BY m.folder ASC LIMIT :offset, :limit", nativeQuery = true)
   fun findFoldersOffsetAndLimit(@Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Folder?>?

   @Query("SELECT * FROM metadata WHERE folder = :folder AND hidden = 0 ORDER BY year DESC, month DESC, day DESC, time DESC LIMIT :offset, :limit", nativeQuery = true)
   fun findAllByFolderOffsetAndLimit(@Param("folder") folder: String,@Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Metadata>

   @Query("SELECT * FROM metadata WHERE id IN :metadataIds", nativeQuery = true)
   fun findAllByMetadataIds(metadataIds: Array<String>): MutableIterable<Metadata>

   @Query("SELECT year, month, COUNT(*) as count FROM metadata group by year, month order by year DESC, month DESC", nativeQuery = true)
   fun countByYearAndMonth(): MutableIterable<MetadataYearMonthCount>

   @Query("SELECT COUNT(DISTINCT m.id) AS count FROM metadata m LEFT JOIN albumphoto ap on m.id = ap.metadata_id LEFT JOIN useralbum ua on ap.album_id = ua.album_id LEFT JOIN recognitionlabelphoto rlp on m.id = rlp.metadata_id LEFT JOIN recognitionlabel rl on rlp.recognition_label_id = rl.id WHERE m.hidden = 0 AND rl.id = :recognitionLabelId AND rlp.confidence >= 0.0 AND rlp.confidence <= :recognitionConfidenceThreshold AND ua.user_id = :userId", nativeQuery = true)
   fun countByPhotoAlbumByPerson(@Param("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String, @Param("recognitionLabelId") recognitionLabelId: Int,@Param("userId") userId: Int): Int

   @Query("SELECT COUNT(DISTINCT m.id) FROM metadata m LEFT JOIN recognitionlabelphoto rlp on m.id = rlp.metadata_id LEFT JOIN recognitionlabel rl on rlp.recognition_label_id = rl.id WHERE m.hidden = 0 AND rl.id = :recognitionLabelId AND rlp.confidence >= 0.0 AND rlp.confidence <= :recognitionConfidenceThreshold", nativeQuery = true)
   fun countByMetadataByPerson(@Param("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String, @Param("recognitionLabelId") recognitionLabelId: Int): Int

   @Query("SELECT COUNT(DISTINCT m.id) FROM metadata m INNER JOIN recognitionlabelphoto rlp ON m.id = rlp.metadata_id WHERE m.hidden = 0 AND confidence > :recognitionConfidenceThreshold AND confidence < 1.0 AND confidence != 0.0 AND rlp.recognition_label_id = :recognitionLabelId", nativeQuery = true) //  LIMIT 0, :matchScanLimit - ,@Param("matchScanLimit") matchScanLimit: Int
   fun countLowMatchesByPerson(@Param("recognitionLabelId") recognitionLabelId: Int,@Param("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String): Int

   @Query("SELECT DISTINCT m.id as metadataId,m.type,m.path,m.thumbnail_path_small as thumbnailPathSmall,m.thumbnail_url_original as thumbnailUrlOriginal,rlp.recognition_label_id as recognitionLabelId,rl.name as recognitionLabelName FROM metadata m LEFT JOIN recognitionlabelphoto rlp ON rlp.metadata_id = m.id LEFT JOIN recognitionlabel rl ON rlp.recognition_label_id = rl.id WHERE m.hidden = 0 AND rlp.id IN (SELECT rlp2.id FROM recognitionlabelphoto rlp2 WHERE rlp.recognition_label_id = rlp2.recognition_label_id LIMIT :trainingDataLimit) AND rlp.confidence >= 0.0 AND rlp.confidence <= :recognitionConfidenceThreshold ORDER BY rlp.recognition_label_id, RANDOM()",nativeQuery = true)
   fun findTrainingData(@Param("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String, @Param("trainingDataLimit") trainingDataLimit: Int): MutableIterable<TrainingData>

   @Query("SELECT * FROM metadata WHERE type LIKE %:type% AND hidden = 0 AND original_image_width < original_image_height ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
   fun findRandomMetadataMediaPortrait(@Param("type") type: String): Metadata?

   @Query("SELECT * FROM metadata WHERE type LIKE %:type% AND hidden = 0 AND original_image_width > original_image_height ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
   fun findRandomMetadataMediaLandscape(@Param("type") type: String): Metadata?

   @Query("SELECT * FROM metadata WHERE type LIKE %:type% AND hidden = 0 ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
   fun findRandomMetadataMedia(@Param("type") type: String): Metadata?

   @Query("SELECT * FROM metadata WHERE file_name LIKE %:filename% AND hidden = 0 ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
   fun findRandomMetadataMediaAndFilter(@Param("filename") filename: String): Metadata?

   @Query("SELECT * FROM metadata WHERE file_name LIKE %:filename% AND hidden = 0 AND original_image_width > original_image_height ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
   fun findRandomMetadataMediaAndFilterLandscape(@Param("filename") filename: String): Metadata?

   @Query("SELECT * FROM metadata WHERE file_name LIKE %:filename% AND hidden = 0 AND original_image_width < original_image_height ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
   fun findRandomMetadataMediaAndFilterPortrait(@Param("filename") filename: String): Metadata?

   @Query("SELECT m.* FROM user u, album a INNER JOIN useralbum ua ON u.id = ua.user_id AND ua.album_id = a.id INNER JOIN albumphoto ap ON a.id = ap.album_id INNER JOIN metadata m ON ap.metadata_id = m.id WHERE u.id = :userId AND m.type LIKE %:type% AND m.hidden = 0 AND m.original_image_width < m.original_image_height ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
   fun findRandomAlbumMediaByUserPortrait(@Param("userId") userId: Int, @Param("type") type: String): Metadata?

   @Query("SELECT m.* FROM user u, album a INNER JOIN useralbum ua ON u.id = ua.user_id AND ua.album_id = a.id INNER JOIN albumphoto ap ON a.id = ap.album_id INNER JOIN metadata m ON ap.metadata_id = m.id WHERE u.id = :userId AND m.type LIKE %:type% AND m.hidden = 0 AND m.original_image_width > m.original_image_height ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
   fun findRandomAlbumMediaByUserLandscape(@Param("userId") userId: Int, @Param("type") type: String): Metadata?

   @Query("SELECT m.* FROM user u, album a INNER JOIN useralbum ua ON u.id = ua.user_id AND ua.album_id = a.id INNER JOIN albumphoto ap ON a.id = ap.album_id INNER JOIN metadata m ON ap.metadata_id = m.id WHERE u.id = :userId AND m.type LIKE %:type% AND m.hidden = 0 ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
   fun findRandomAlbumMediaByUser(@Param("userId") userId: Int, @Param("type") type: String): Metadata?

   @Query("SELECT m.* FROM user u, album a INNER JOIN useralbum ua ON u.id = ua.user_id AND ua.album_id = a.id INNER JOIN albumphoto ap ON a.id = ap.album_id INNER JOIN metadata m ON ap.metadata_id = m.id WHERE u.id = :userId AND m.file_name LIKE %:filename% AND m.hidden = 0 ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
   fun findRandomAlbumMediaAndFilterByUser(@Param("userId") userId: Int, @Param("filename") filename: String): Metadata?

   @Query("SELECT m.* FROM user u, album a INNER JOIN useralbum ua ON u.id = ua.user_id AND ua.album_id = a.id INNER JOIN albumphoto ap ON a.id = ap.album_id INNER JOIN metadata m ON ap.metadata_id = m.id WHERE u.id = :userId AND m.file_name LIKE %:filename% AND m.hidden = 0 AND m.original_image_width > m.original_image_height ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
   fun findRandomAlbumMediaAndFilterByUserLandscape(@Param("userId") userId: Int, @Param("filename") filename: String): Metadata?

   @Query("SELECT m.* FROM user u, album a INNER JOIN useralbum ua ON u.id = ua.user_id AND ua.album_id = a.id INNER JOIN albumphoto ap ON a.id = ap.album_id INNER JOIN metadata m ON ap.metadata_id = m.id WHERE u.id = :userId AND m.file_name LIKE %:filename% AND m.hidden = 0 AND m.original_image_width < m.original_image_height ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
   fun findRandomAlbumMediaAndFilterByUserPortrait(@Param("userId") userId: Int, @Param("filename") filename: String): Metadata?

   @Query("SELECT COUNT(*) FROM user u, album a INNER JOIN useralbum ua ON u.id = ua.user_id AND ua.album_id = a.id INNER JOIN albumphoto ap ON a.id = ap.album_id INNER JOIN metadata m ON ap.metadata_id = m.id WHERE u.id = :userId AND m.type LIKE %:type%", nativeQuery = true)
   fun countAlbumByMedia(@Param("userId") userId: Int, @Param("type") type: String): Int
}