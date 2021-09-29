package com.miyagi.shashin.repository

import com.miyagi.shashin.model.Notification
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface NotificationRepository : CrudRepository<Notification?, Int?> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Int): MutableIterable<Notification?>?
    fun findAllByAlbumIdAndUserIdOrderByCreatedAtDesc(albumId: Int,userId: Int): MutableIterable<Notification?>?
    fun findAllByMetadataIdAndUserIdOrderByCreatedAtDesc(metadataId: String,userId: Int): MutableIterable<Notification?>?
    fun countAllByUserIdAndReadIsFalse(userId: Int): Int
    fun countAllByAlbumIdAndUserIdAndReadIsFalse(albumId: Int,userId: Int): Int
    fun countAllByMetadataIdAndUserIdAndReadIsFalse(metadataId: String,userId: Int): Int
}