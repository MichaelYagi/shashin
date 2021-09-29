package com.miyagi.shashin.repository

import com.miyagi.shashin.model.Notification
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface NotificationRepository : CrudRepository<Notification?, Int?> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Int): MutableIterable<Notification?>?
    fun countAllByUserIdAndReadIsFalse(userId: Int): Int
}