package com.miyagi.shashin.repository

import com.miyagi.shashin.model.UserAlbum
import org.springframework.data.repository.CrudRepository

interface UserAlbumRepository : CrudRepository<UserAlbum?, Int?> {
    fun countByUserIdAndAlbumId(userId: Int?, albumId: Int?): Int?
    fun findAllByUserId(userId: Int?): MutableIterable<UserAlbum?>?
    fun findByUserIdAndAlbumId(userId: Int?, albumId: Int?): UserAlbum?
    fun findAllByOrderByUserIdAsc(): MutableIterable<UserAlbum?>?
    fun deleteByAlbumId(albumId: Int?): Long?
    fun deleteByUserIdAndAlbumId(userId: Int?, albumId: Int?): Long?
    fun deleteByUserId(userId: Int?): Long?
}