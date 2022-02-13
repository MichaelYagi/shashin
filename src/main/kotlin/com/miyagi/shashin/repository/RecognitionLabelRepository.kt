package com.miyagi.shashin.repository

import com.miyagi.shashin.model.RecognitionLabel
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface RecognitionLabelRepository : CrudRepository<RecognitionLabel?, Int?> {
    fun findByNameIgnoreCase(name: String): RecognitionLabel?
    @Query("SELECT DISTINCT * FROM recognitionlabel WHERE name != :name ORDER BY name COLLATE NOCASE ASC", nativeQuery = true)
    fun findAllByNameNotContaining(@Param("name") name: String): MutableIterable<RecognitionLabel>?
}