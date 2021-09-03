package com.miyagi.shashin.repository

import com.miyagi.shashin.model.RecognitionLabel
import org.springframework.data.repository.CrudRepository

interface RecognitionLabelRepository : CrudRepository<RecognitionLabel?, Int?> {
    fun findByName(name: String): RecognitionLabel?
}