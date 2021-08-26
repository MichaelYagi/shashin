package com.miyagi.shashin.repository

import com.miyagi.shashin.model.Album
import com.miyagi.shashin.model.AlbumPhoto
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AlbumRepository : CrudRepository<Album?, Int?> {
    fun findByName(name: String?): Album?
}