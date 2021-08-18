package com.miyagi.shashin.repository

import com.miyagi.shashin.model.Album
import com.miyagi.shashin.model.AlbumPhoto
import org.springframework.data.repository.CrudRepository

interface AlbumRepository : CrudRepository<Album?, Int?> {
    fun findByName(name: String?): Album?
}