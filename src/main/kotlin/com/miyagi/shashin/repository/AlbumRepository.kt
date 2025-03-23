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
    @Query("SELECT DISTINCT m.id FROM metadata m LEFT JOIN albumphoto ap ON ap.metadata_id = m.id LEFT JOIN album a ON ap.album_id = a.id WHERE a.id = :albumId AND m.lat IS NOT NULL AND m.lng IS NOT NULL ORDER BY name COLLATE NOCASE ASC", nativeQuery = true)
    fun findMetadataIdsByAlbumId(albumId: Int?): MutableIterable<String>?
    @Query("SELECT DISTINCT m.id FROM metadata m LEFT JOIN albumphoto ap ON ap.metadata_id = m.id LEFT JOIN album a ON ap.album_id = a.id LEFT JOIN useralbum ua ON a.id = ua.album_id WHERE a.id = :albumId AND ua.user_id = :userId AND m.lat IS NOT NULL AND m.lng IS NOT NULL ORDER BY name COLLATE NOCASE ASC", nativeQuery = true)
    fun findMetadataIdsByAlbumIdAndUserId(albumId: Int, @Param("userId") userId: Int): MutableIterable<String>?
    fun findAlbumById(albumId: Int?): Album?
    @Query("SELECT DISTINCT * FROM album ORDER BY name COLLATE NOCASE ASC", nativeQuery = true)
    fun findAllOrderByAlbumName(): MutableIterable<Album>?
    @Query("SELECT DISTINCT a.* FROM album a LEFT JOIN albumphoto ap ON ap.album_id = a.id LEFT JOIN metadata m ON ap.metadata_id = m.id WHERE m.lat IS NOT NULL AND m.lng IS NOT NULL ORDER BY m.taken_at DESC", nativeQuery = true)
    fun findAllWithLocationOrderByAlbumName(): MutableIterable<Album>?
    @Query("SELECT DISTINCT a.* FROM album a LEFT JOIN albumphoto ap ON ap.album_id = a.id LEFT JOIN metadata m ON ap.metadata_id = m.id LEFT JOIN useralbum ua ON a.id = ua.album_id WHERE ua.user_id = :userId AND m.lat IS NOT NULL AND m.lng IS NOT NULL ORDER BY m.taken_at DESC", nativeQuery = true)
    fun findAllWithLocationOrderByAlbumNameAndUserId(@Param("userId") userId: Int): MutableIterable<Album>?
    fun findAlbumByNameIgnoreCase(name: String): Album?
    @Query("SELECT DISTINCT u.id as userId, u.username, a.id as albumId FROM user u, album a INNER JOIN useralbum ua ON u.id = ua.user_id AND ua.album_id = a.id WHERE u.id = :userId ORDER BY RANDOM()", nativeQuery = true)
    fun findRandomAlbumsByUser(@Param("userId") userId: Int): MutableIterable<UserSharedAlbums>
    @Query("SELECT DISTINCT a.id FROM album a LEFT JOIN albumphoto ap ON a.id = ap.album_id LEFT JOIN metadata m ON ap.metadata_id = m.id WHERE m.id = :metadataId", nativeQuery = true)
    fun findAlbumIdsByMetadataId(@Param("metadataId") metadataId: String): MutableIterable<Int>?
}