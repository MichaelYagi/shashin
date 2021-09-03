package com.miyagi.shashin.repository

import com.miyagi.shashin.model.Metadata
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface SearchRepository : CrudRepository<Metadata?, String?> {
    @Query("SELECT m.* FROM metadata m LEFT JOIN recognitionlabelphoto rlp ON m.id = rlp.metadata_id LEFT JOIN recognitionlabel rl ON rl.id = rlp.recognition_label_id WHERE m.keywords LIKE %:searchTerm% OR m.file_name LIKE %:searchTerm% OR m.place_name LIKE %:searchTerm% OR rl.name LIKE %:searchTerm%", nativeQuery = true)
    fun findMetadataBySearchTerm(@Param("searchTerm") searchTerm: String): MutableIterable<Metadata>
    @Query("SELECT DISTINCT m.* FROM metadata m LEFT JOIN recognitionlabelphoto rlp ON m.id = rlp.metadata_id LEFT JOIN recognitionlabel rl ON rl.id = rlp.recognition_label_id LEFT JOIN albumphoto a ON m.id = a.metadata_id LEFT JOIN useralbum ua ON ua.album_id = a.album_id WHERE (m.keywords LIKE %:searchTerm% OR m.file_name LIKE %:searchTerm% OR m.place_name LIKE %:searchTerm% OR rl.name LIKE %:searchTerm%) AND ua.user_id = :userId", nativeQuery = true)
    fun findMetadataBySearchTermAndUserId(@Param("searchTerm") searchTerm: String, @Param("userId") userId: Int): MutableIterable<Metadata>
}