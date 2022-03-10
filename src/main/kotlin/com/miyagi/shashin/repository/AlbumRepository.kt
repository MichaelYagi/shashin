package com.miyagi.shashin.repository

import com.miyagi.shashin.model.*
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AlbumRepository : CrudRepository<Album?, Int?> {
    @Query("SELECT a.id as albumId, COUNT(ap.metadata_id) as photoCount FROM album a LEFT JOIN albumphoto ap ON a.id = ap.album_id GROUP BY a.id", nativeQuery = true)
    fun countNumberOfPhotosInAlbums(): MutableIterable<AlbumPhotoCount?>?
    @Query("SELECT DISTINCT * FROM album ORDER BY name COLLATE NOCASE ASC", nativeQuery = true)
    fun findAllOrderByAlbumName(): MutableIterable<Album>
    fun findAlbumByNameIgnoreCase(name: String): Album?
}