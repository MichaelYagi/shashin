package com.miyagi.shashin.repository

import com.miyagi.shashin.model.MediaDirectory
import com.miyagi.shashin.model.User
import org.springframework.data.repository.CrudRepository

interface MediaDirectoryRepository : CrudRepository<MediaDirectory?, Int?> {
    fun findByDirectory(directory: String?): MediaDirectory?
}