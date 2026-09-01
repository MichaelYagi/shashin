package com.miyagi.shashin.repository

import com.miyagi.shashin.model.User
import com.miyagi.shashin.model.UserRoleCount
import com.miyagi.shashin.model.UserSharedAlbums
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : CrudRepository<User?, Int?> {
    fun findAll(sort: Sort?): MutableIterable<User?>?
    @Query("SELECT * FROM user ORDER BY id DESC LIMIT :offset, :limit", nativeQuery = true)
    fun findAllByOffsetAndLimit(@Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<User?>?
    @Cacheable(value = ["users"], key = "#username")
    fun findByUsername(username: String?): User?
    fun findById(userId: Int?): User?
    fun findByApikey(apikey: String?): User?
    @CacheEvict(value = ["users"], allEntries = true)
    override fun <S : User?> save(entity: S & Any): S & Any
    @Query("SELECT DISTINCT u.* FROM useralbum ua INNER JOIN user u ON u.id = ua.user_id INNER JOIN album a ON a.id = ua.album_id WHERE u.authority != 'ROLE_SUPER' AND u.authority != 'ROLE_ADMIN' AND ua.album_id = :albumId", nativeQuery = true)
    fun findDistinctUserByAlbumId(@Param("albumId") albumId: Int): MutableIterable<User?>?
    @Query("SELECT DISTINCT u.id as userId, u.username, a.id as albumId, CASE WHEN ua.user_id IS NULL THEN FALSE ELSE TRUE END AS isShared FROM user u, album a LEFT JOIN useralbum ua ON u.id = ua.user_id AND ua.album_id = a.id WHERE u.id != :userId AND u.is_authorized = 1 AND u.authority = 'ROLE_USER'", nativeQuery = true)
    fun findUserBySharedAlbum(@Param("userId") userId: Int): MutableIterable<UserSharedAlbums>
    @Query("SELECT DISTINCT u.* FROM user u LEFT JOIN useralbum ua ON u.id = ua.user_id WHERE (ua.album_id = :albumId OR u.authority = 'ROLE_ADMIN' OR u.authority = 'ROLE_SUPER') AND is_authorized = 1", nativeQuery = true)
    fun findAllUserBySharedAlbum(@Param("albumId") albumId: Int): MutableIterable<User>
    @Query("SELECT * FROM user WHERE (authority = 'ROLE_ADMIN' OR authority = 'ROLE_SUPER') AND is_authorized = 1", nativeQuery = true)
    fun findAllAdmins(): MutableIterable<User>
    fun findAllByAuthorityEquals(authority: String): MutableIterable<User>
    @Query("SELECT * FROM user WHERE is_authorized = :authorized", nativeQuery = true)
    fun findAllByAuthorized(authorized: Boolean): MutableIterable<User>
    fun countAllByIsAuthorizedIsFalseAndAuthorityEquals(authority: String): Int
    fun countAllByIsAuthorizedIsTrueAndAuthorityEquals(authority: String): Int
    @Query("SELECT authority, SUM(CASE WHEN is_authorized = 1 THEN 1 ELSE 0 END) AS allowedCount, SUM(CASE WHEN is_authorized = 0 THEN 1 ELSE 0 END) AS notAllowedCount FROM user WHERE authority IN (:roles) GROUP BY authority", nativeQuery = true)
    fun getUserRoleCounts(@Param("roles") roles: List<String>): List<UserRoleCount>
}