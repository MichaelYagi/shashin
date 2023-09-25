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
    fun findAlbumById(albumId: Int?): Album?
    @Query("SELECT DISTINCT * FROM album ORDER BY name COLLATE NOCASE ASC", nativeQuery = true)
    fun findAllOrderByAlbumName(): MutableIterable<Album>
    fun findAlbumByNameIgnoreCase(name: String): Album?
    @Query("SELECT DISTINCT u.id as userId, u.username, a.id as albumId, CASE WHEN ua.user_id IS NULL THEN FALSE ELSE TRUE END AS isShared FROM user u, album a LEFT JOIN useralbum ua ON u.id = ua.user_id AND ua.album_id = a.id WHERE u.id = :userId ORDER BY RANDOM()", nativeQuery = true)
    fun findRandomAlbumsByUser(@Param("userId") userId: Int): MutableIterable<UserSharedAlbums>
}