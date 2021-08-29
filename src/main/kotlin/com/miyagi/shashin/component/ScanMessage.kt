package com.miyagi.shashin.component

import org.springframework.stereotype.Component

@Component
class ScanMessage {
    private var message: String? = null

    fun ScanMessage() {}

    fun ScanMessage(message: String?) {
        this.message = message
    }

    fun getMessage(): String? {
        return message
    }

    fun setMessage(message: String?) {
        this.message = message
    }
}