package com.miyagi.shashin.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.context.restart.RestartEndpoint
import org.springframework.stereotype.Service

@Service
class RestartService(
    private val restartEndpoint: RestartEndpoint? = null
) {
    fun restartApp() {
        restartEndpoint!!.restart()
    }
}