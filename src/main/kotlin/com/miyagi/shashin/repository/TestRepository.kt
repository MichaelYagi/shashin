package com.miyagi.shashin.repository

import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.RecognitionLabel
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import jakarta.transaction.Transactional

@Transactional
@Repository
interface TestRepository : CrudRepository<Metadata?, String?> {
    @Query("SELECT id FROM metadata WHERE lat IS NOT NULL AND place_name IS NULL", nativeQuery = true)
    fun findLocationsWithNullPlace(): MutableList<String>?

    @Query("SELECT * FROM metadata WHERE place_name LIKE :placeName%", nativeQuery = true)
    fun findByPlaceName(@Param("placeName") term: String): MutableList<Metadata>?

    @Query("SELECT * FROM metadata WHERE focal_length IS NOT NULL", nativeQuery = true)
    fun findAllFocalLengths(): MutableList<Metadata>?

    @Query("SELECT * FROM metadata WHERE type LIKE \"%image%\" AND type NOT LIKE \"%gif%\"", nativeQuery = true)
    fun findImagePaths(): MutableList<Metadata>?

    @Query("SELECT COUNT(*) FROM metadata WHERE type LIKE \"%image%\" AND type NOT LIKE \"%gif%\"", nativeQuery = true)
    fun countImagePaths(): Int
}