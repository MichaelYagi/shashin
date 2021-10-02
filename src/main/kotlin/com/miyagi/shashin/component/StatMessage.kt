package com.miyagi.shashin.component

import org.springframework.stereotype.Component

@Component
class StatMessage {
    private var message: String? = null

    fun StatMessage() {}

    fun StatMessage(message: String?) {
        this.message = message
    }

    fun getMessage(): String? {
        return message
    }

    fun setMessage(message: String?) {
        this.message = message
    }
}