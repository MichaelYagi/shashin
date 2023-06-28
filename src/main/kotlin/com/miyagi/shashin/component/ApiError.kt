package com.miyagi.shashin.component

import com.fasterxml.jackson.annotation.JsonFormat
import org.springframework.http.HttpStatus
import java.time.LocalDateTime


internal class ApiError private constructor() {
    private var status: HttpStatus? = null

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private val timestamp: LocalDateTime
    private var msg: String? = null
    private var debugMessage: String? = null
    private val subErrors: List<ApiSubError>? = null

    init {
        timestamp = LocalDateTime.now()
    }

    constructor(status: HttpStatus?) : this() {
        this.status = status
    }

    constructor(status: HttpStatus?, ex: Throwable) : this() {
        this.status = status
        msg = "Unexpected error"
        debugMessage = ex.localizedMessage
    }

    constructor(status: HttpStatus?, message: String?, ex: Throwable) : this() {
        this.status = status
        this.msg = message
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
}