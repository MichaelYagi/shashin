package com.miyagi.shashin.repository

import com.miyagi.shashin.model.User
import com.miyagi.shashin.model.UserAlbum
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import jakarta.transaction.Transactional

@Transactional
@Repository
interface UserAlbumRepository : CrudRepository<UserAlbum?, Int?> {
    fun countUserAlbumsByUserId(userId: Int): Int?
    fun countByUserIdAndAlbumId(userId: Int?, albumId: Int?): Int?
    fun countByAlbumId(albumId: Int?): Int?
    @Query("SELECT DISTINCT(ua.album_id) FROM useralbum ua INNER JOIN album a ON a.id = ua.album_id WHERE ua.user_id = :userId", nativeQuery = true)
    fun findAlbumIdsByUserId(@Param("userId") userId: Int): MutableList<Int>?
    @Query("SELECT ua.* FROM useralbum ua INNER JOIN album a ON a.id = ua.album_id WHERE ua.user_id = :userId ORDER BY name COLLATE NOCASE ASC LIMIT :offset, :limit", nativeQuery = true)
    fun findAllByUserIdAndOffsetAndLimit(@Param("userId") userId: Int, @Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<UserAlbum?>?
    @Query("SELECT ua.* FROM useralbum ua INNER JOIN album a ON a.id = ua.album_id GROUP BY ua.album_id ORDER BY name COLLATE NOCASE ASC LIMIT :offset, :limit", nativeQuery = true)
    fun findAllOffsetAndLimit(@Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<UserAlbum?>?
    fun findFirstByUserIdAndAlbumId(userId: Int?, albumId: Int?): UserAlbum?
    fun findAllByOrderByUserIdAsc(): MutableIterable<UserAlbum?>?
    fun findAllByAlbumId(albumId: Int?): MutableIterable<UserAlbum?>?
    fun deleteByAlbumId(albumId: Int?): Long?
    fun deleteByUserId(userId: Int?): Long?
}