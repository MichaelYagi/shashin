package com.miyagi.shashin.repository

import com.miyagi.shashin.model.AlbumPhotoCount
import com.miyagi.shashin.model.RecognitionLabel
import org.springframework.data.repository.CrudRepository

interface RecognitionLabelRepository : CrudRepository<RecognitionLabel?, Int?> {
    fun findByNameIgnoreCase(name: String): RecognitionLabel?
}