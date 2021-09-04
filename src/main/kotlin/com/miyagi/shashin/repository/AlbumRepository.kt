package com.miyagi.shashin.repository

import com.miyagi.shashin.model.*
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AlbumRepository : CrudRepository<Album?, Int?> {
    fun findByName(name: String?): Album?
    @Query("SELECT a.id as albumId, COUNT(*) as photoCount FROM album a INNER JOIN albumphoto ap ON a.id = ap.album_id GROUP BY a.id", nativeQuery = true)
    fun countNumberOfPhotosInAlbums(): MutableIterable<AlbumPhotoCount?>?
}