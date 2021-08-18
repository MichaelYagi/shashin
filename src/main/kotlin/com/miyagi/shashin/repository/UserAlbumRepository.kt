package com.miyagi.shashin.repository

import com.miyagi.shashin.model.UserAlbum
import org.springframework.data.repository.CrudRepository

interface UserAlbumRepository : CrudRepository<UserAlbum?, Int?> {
    fun countByUserIdAndAlbumId(userId: Int?, albumId: Int?): Int?
    fun findAllByUserId(userId: Int?): MutableIterable<UserAlbum?>?
}