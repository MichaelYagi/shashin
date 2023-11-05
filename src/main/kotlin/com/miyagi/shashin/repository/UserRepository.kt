package com.miyagi.shashin.repository

import com.miyagi.shashin.model.User
import com.miyagi.shashin.model.UserSharedAlbums
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : CrudRepository<User?, Int?> {
    fun findAll(sort: Sort?): MutableIterable<User?>?
    fun findByUsername(username: String?): User?
    fun findById(userId: Int?): User?
    fun findByApikey(apikey: String?): User?
    @Query("SELECT DISTINCT u.id as userId, u.username, a.id as albumId, CASE WHEN ua.user_id IS NULL THEN FALSE ELSE TRUE END AS isShared FROM user u, album a LEFT JOIN useralbum ua ON u.id = ua.user_id AND ua.album_id = a.id WHERE u.id != :userId AND u.is_authorized = 1 AND u.authority = 'ROLE_USER'", nativeQuery = true)
    fun findUserBySharedAlbum(@Param("userId") userId: Int): MutableIterable<UserSharedAlbums>
    @Query("SELECT DISTINCT u.* FROM user u LEFT JOIN useralbum ua ON u.id = ua.user_id WHERE (ua.album_id = :albumId OR u.authority = 'ROLE_ADMIN') AND is_authorized = 1", nativeQuery = true)
    fun findAllUserBySharedAlbum(@Param("albumId") albumId: Int): MutableIterable<User>
    fun findAllByAuthorityEquals(authority: String): MutableIterable<User>
    fun countAllByIsAuthorizedIsFalseAndAuthorityEquals(authority: String): Int
    fun countAllByIsAuthorizedIsTrueAndAuthorityEquals(authority: String): Int
}