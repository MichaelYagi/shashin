package com.miyagi.shashin.repository

import com.miyagi.shashin.model.Notification
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.Query

@Transactional
@Repository
interface NotificationRepository : CrudRepository<Notification?, Int?> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Int): MutableIterable<Notification?>?
    @Query("SELECT * FROM notification WHERE type=:type AND identifier=:identifier AND user_id = :userId ORDER BY created_at DESC", nativeQuery = true)
    fun findAllByTypeAndId(type: String, identifier: String, userId: Int): MutableIterable<Notification?>?
    fun findAllByUserIdAndReadIsFalse(userId: Int): MutableIterable<Notification?>?
}