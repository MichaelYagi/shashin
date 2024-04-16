package com.miyagi.shashin.component

import org.springframework.stereotype.Component

@Component
class ScaperMessage {
    private var message: String? = null

    fun ScaperMessage() {}

    fun ScaperMessage(message: String?) {
        this.message = message
    }

    fun getMessage(): String? {
        return message
    }

    fun setMessage(message: String?) {
        this.message = message
    }
}