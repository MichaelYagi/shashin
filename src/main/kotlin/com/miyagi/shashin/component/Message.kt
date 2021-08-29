package com.miyagi.shashin.component

import org.springframework.stereotype.Component

@Component
class Message {
    private var content: String? = null

    fun Message() {}

    fun Message(content: String?) {
        this.content = content
    }

    fun setContent(content: String?) {
        this.content = content
    }

    fun getContent(): String? {
        return content
    }
}