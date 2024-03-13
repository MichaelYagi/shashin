package com.miyagi.shashin.component

import org.springframework.stereotype.Component

@Component
class NotificationMessage {
    private var message: String? = null

    fun NotificationMessage() {}

    fun NotificationMessage(message: String?) {
        this.message = message
    }

    fun getMessage(): String? {
        return message
    }

    fun setMessage(message: String?) {
        this.message = message
    }
}