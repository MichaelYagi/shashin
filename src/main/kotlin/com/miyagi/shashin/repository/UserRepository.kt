package com.miyagi.shashin.repository

import com.miyagi.shashin.model.User
import com.miyagi.shashin.model.UserSharedAlbums
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : CrudRepository<User?, Int?> {
    fun findAll(sort: Sort?): MutableIterable<User?>?
    fun findByUsername(username: String?): User?
    @Query("SELECT u.id as userId, u.username, a.id as albumId, CASE WHEN ua.user_id IS NULL THEN FALSE ELSE TRUE END AS isShared FROM user u, album a LEFT JOIN useralbum ua ON u.id = ua.user_id AND ua.album_id = a.id WHERE u.id != :userId", nativeQuery = true)
    fun findUserBySharedAlbum(@Param("userId") userId: Int): MutableIterable<UserSharedAlbums>
    fun findAllByAuthorityEquals(authority: String): MutableIterable<User>
    fun countAllByIsAllowedIsFalseAndAuthorityEquals(authority: String): Int
    fun countAllByIsAllowedIsTrueAndAuthorityEquals(authority: String): Int
    fun countAllByLoggedInIsTrue(): Int
}