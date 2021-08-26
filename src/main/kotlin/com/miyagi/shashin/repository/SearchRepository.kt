package com.miyagi.shashin.repository

import com.miyagi.shashin.model.Metadata
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface SearchRepository : CrudRepository<Metadata?, String?> {
    @Query("SELECT * FROM metadata WHERE keywords LIKE %:searchTerm% OR file_name LIKE %:searchTerm% OR place_name LIKE %:searchTerm%", nativeQuery = true)
    fun findMetadataBySearchTerm(@Param("searchTerm") searchTerm: String): MutableIterable<Metadata>
    @Query("SELECT DISTINCT m.* FROM metadata m INNER JOIN albumphoto a ON m.id = a.metadata_id INNER JOIN useralbum ua ON ua.album_id = a.album_id WHERE (m.keywords LIKE %:searchTerm% OR m.file_name LIKE %:searchTerm% m.place_name LIKE %:searchTerm%) AND ua.user_id = :userId", nativeQuery = true)
    fun findMetadataBySearchTermAndUserId(@Param("searchTerm") searchTerm: String, @Param("userId") userId: Int): MutableIterable<Metadata>
}