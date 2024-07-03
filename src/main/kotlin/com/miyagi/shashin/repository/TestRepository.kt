package com.miyagi.shashin.repository

import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.RecognitionLabel
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

@Transactional
@Repository
interface TestRepository : CrudRepository<Metadata?, String?> {
    @Query("SELECT id FROM metadata WHERE lat IS NOT NULL AND place_name IS NULL", nativeQuery = true)
    fun findLocationsWithNullPlace(): MutableList<String>?
}