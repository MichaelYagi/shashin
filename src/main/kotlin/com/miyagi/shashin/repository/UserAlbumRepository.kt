package com.miyagi.shashin.repository

import com.miyagi.shashin.model.UserAlbum
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

@Transactional
@Repository
interface UserAlbumRepository : CrudRepository<UserAlbum?, Int?> {
    fun countByUserIdAndAlbumId(userId: Int?, albumId: Int?): Int?
    @Query("SELECT * FROM useralbum WHERE user_id = :userId LIMIT :offset, :limit", nativeQuery = true)
    fun findAllByUserIdAndOffsetAndLimit(@Param("userId") userId: Int, @Param("offset") offset: Int, @Param("limit") limit: Int): MutableIterable<UserAlbum?>?
    fun findDistinctByUserIdAndAlbumId(userId: Int?, albumId: Int?): UserAlbum?
    fun findAllByOrderByUserIdAsc(): MutableIterable<UserAlbum?>?
    fun deleteByAlbumId(albumId: Int?): Long?
    fun deleteByUserIdAndAlbumId(userId: Int?, albumId: Int?): Long?
    fun deleteByUserId(userId: Int?): Long?
}