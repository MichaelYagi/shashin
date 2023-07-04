package com.miyagi.shashin.component

import com.miyagi.shashin.util.TextUtils
import org.springframework.http.HttpStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal class ApiError private constructor() {
    private var status: HttpStatus? = null

    private var timestamp: String? = null
    private var msg: String? = null
    private var debugMessage: String? = null

    constructor(status: HttpStatus?) : this() {
        this.status = status
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern(TextUtils.getCommonDateFormat())
        this.timestamp = now.format(formatter)
    }

    constructor(status: HttpStatus?, ex: Throwable) : this() {
        this.status = status
        msg = "Unexpected error"
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern(TextUtils.getCommonDateFormat())
        this.timestamp = now.format(formatter)
        debugMessage = ex.localizedMessage
    }

    constructor(status: HttpStatus?, message: String?, ex: Throwable) : this() {
        this.status = status
        this.msg = message
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern(TextUtils.getCommonDateFormat())
        this.timestamp = now.format(formatter)
        debugMessage = ex.localizedMessage
    }

    fun setStatus(status: HttpStatus?) {
        this.status = status
    }
    fun getStatus(): HttpStatus? {
        return this.status
    }

    fun setMsg(message: String?) {
        this.msg = message
    }

    fun getMsg(): String? {
        return this.msg
    }

    fun setTimestamp(timestamp: String?) {
        this.timestamp = timestamp
    }

    fun getTimestamp(): String? {
        return this.timestamp
    }
}