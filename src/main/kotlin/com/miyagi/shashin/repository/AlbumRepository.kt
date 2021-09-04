package com.miyagi.shashin.repository

import com.miyagi.shashin.model.*
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AlbumRepository : CrudRepository<Album?, Int?> {
    fun findByName(name: String?): Album?
    @Query("SELECT a.id as albumId, COUNT(*) as photoCount FROM album a INNER JOIN albumphoto ap ON a.id = ap.album_id GROUP BY a.id", nativeQuery = true)
    fun countNumberOfPhotosInAlbums(): MutableIterable<AlbumPhotoCount?>?
    @Query("SELECT rl.*, COUNT(*) AS tag_count, m.thumbnail_url_centered FROM metadata m INNER JOIN recognitionlabelphoto rlp ON m.id = rlp.metadata_id INNER JOIN recognitionlabel rl ON rl.id = rlp.recognition_label_id INNER JOIN albumphoto ap ON ap.metadata_id = m.id GROUP BY rl.id", nativeQuery = true)
    fun findAlbumPhotoByPeople(): MutableIterable<MetadataPeople>
    @Query("SELECT DISTINCT m.* FROM metadata m LEFT JOIN albumphoto ap on m.id = ap.metadata_id LEFT JOIN useralbum ua on ap.album_id = ua.album_id LEFT JOIN recognitionlabelphoto rlp on m.id = rlp.metadata_id LEFT JOIN recognitionlabel rl on rlp.recognition_label_id = rl.id WHERE rl.id = :recognitionLabelId AND ua.user_id = :userId LIMIT :offset, :limit", nativeQuery = true)
    fun findAlbumPhotoByPerson(@Param("recognitionLabelId") recognitionLabelId: Int,@Param("userId") userId: Int, @Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<Metadata>
}