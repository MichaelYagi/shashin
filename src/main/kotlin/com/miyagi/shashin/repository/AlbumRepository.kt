package com.miyagi.shashin.repository

import com.miyagi.shashin.model.*
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AlbumRepository : CrudRepository<Album?, Int?> {

    @Query("SELECT DISTINCT m.* FROM albumphoto ap, metadata m WHERE ap.album_id = :albumId AND ap.metadata_id = m.id AND m.type LIKE %:type% AND m.year = :year AND m.month = :month AND m.day = :day AND m.hidden = 0 ORDER BY m.year DESC, m.month DESC, m.day DESC, m.time DESC", nativeQuery = true)
    fun findAlbumMetadataByDateAndFilter(@Param("albumId") albumId: Int, @Param("type") type: String, @Param("year") year: Int, @Param("month") month: Int, @Param("day") day: Int): MutableIterable<Metadata>?

    @Query("SELECT DISTINCT m.* FROM albumphoto ap, metadata m WHERE ap.album_id = :albumId AND ap.metadata_id = m.id AND m.year = :year AND m.month = :month AND m.day = :day AND m.hidden = 0 ORDER BY m.year DESC, m.month DESC, m.day DESC, m.time DESC", nativeQuery = true)
    fun findAlbumMetadataByDate(@Param("albumId") albumId: Int, @Param("year") year: Int, @Param("month") month: Int, @Param("day") day: Int): MutableIterable<Metadata>?

    @Query("SELECT DISTINCT m.* FROM albumphoto ap, metadata m WHERE ap.album_id = :albumId AND ap.metadata_id = m.id AND m.year = :year AND m.month = :month AND m.day = :day AND m.hidden = 0 AND m.type LIKE %:type% ORDER BY m.year DESC, m.month DESC, m.day DESC, m.time DESC", nativeQuery = true)
    fun findAlbumMetadataByDateAndMediaType(@Param("albumId") albumId: Int, @Param("year") year: Int, @Param("month") month: Int, @Param("day") day: Int, @Param("type") type: String): MutableIterable<Metadata>?

    @Query("SELECT COUNT(*) FROM album", nativeQuery = true)
    fun countAllAlbums(): Int
    @Query("SELECT a.id as albumId, COUNT(ap.metadata_id) as photoCount FROM album a LEFT JOIN albumphoto ap ON a.id = ap.album_id GROUP BY a.id", nativeQuery = true)
    fun countNumberOfPhotosInAlbums(): MutableIterable<AlbumPhotoCount?>?
    @Query("SELECT DISTINCT m.id FROM metadata m LEFT JOIN albumphoto ap ON ap.metadata_id = m.id LEFT JOIN album a ON ap.album_id = a.id WHERE a.id = :albumId AND m.lat IS NOT NULL AND m.lng IS NOT NULL AND m.hidden = 0 ORDER BY name COLLATE NOCASE ASC", nativeQuery = true)
    fun findMetadataIdsByAlbumId(albumId: Int?): MutableIterable<String>?
    @Query("SELECT DISTINCT m.id FROM metadata m LEFT JOIN albumphoto ap ON ap.metadata_id = m.id LEFT JOIN album a ON ap.album_id = a.id LEFT JOIN useralbum ua ON a.id = ua.album_id WHERE a.id = :albumId AND ua.user_id = :userId AND m.lat IS NOT NULL AND m.lng IS NOT NULL AND m.hidden = 0 ORDER BY name COLLATE NOCASE ASC", nativeQuery = true)
    fun findMetadataIdsByAlbumIdAndUserId(albumId: Int, @Param("userId") userId: Int): MutableIterable<String>?
    fun findAlbumById(albumId: Int?): Album?
    @Query("SELECT DISTINCT * FROM album ORDER BY name COLLATE NOCASE ASC", nativeQuery = true)
    fun findAllOrderByAlbumName(): MutableIterable<Album>?
    @Query("SELECT DISTINCT a.* FROM album a LEFT JOIN albumphoto ap ON ap.album_id = a.id LEFT JOIN metadata m ON ap.metadata_id = m.id WHERE m.lat IS NOT NULL AND m.lng IS NOT NULL AND m.hidden = 0 ORDER BY a.name COLLATE NOCASE ASC", nativeQuery = true)
    fun findAllWithLocationOrderByAlbumName(): MutableIterable<Album>?
    @Query("SELECT DISTINCT a.* FROM album a LEFT JOIN albumphoto ap ON ap.album_id = a.id LEFT JOIN metadata m ON ap.metadata_id = m.id LEFT JOIN useralbum ua ON a.id = ua.album_id WHERE ua.user_id = :userId AND m.lat IS NOT NULL AND m.lng IS NOT NULL AND m.hidden = 0 ORDER BY a.name COLLATE NOCASE ASC", nativeQuery = true)
    fun findAllWithLocationOrderByAlbumNameAndUserId(@Param("userId") userId: Int): MutableIterable<Album>?
    fun findAlbumByNameIgnoreCase(name: String): Album?
    @Query("SELECT DISTINCT u.id as userId, u.username, a.id as albumId FROM user u, album a INNER JOIN useralbum ua ON u.id = ua.user_id AND ua.album_id = a.id WHERE u.id = :userId ORDER BY RANDOM()", nativeQuery = true)
    fun findRandomAlbumsByUser(@Param("userId") userId: Int): MutableIterable<UserSharedAlbums>
    @Query("SELECT DISTINCT a.id FROM album a LEFT JOIN albumphoto ap ON a.id = ap.album_id LEFT JOIN metadata m ON ap.metadata_id = m.id WHERE m.id = :metadataId AND m.hidden = 0", nativeQuery = true)
    fun findAlbumIdsByMetadataId(@Param("metadataId") metadataId: String): MutableIterable<Int>?
}