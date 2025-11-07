package com.miyagi.shashin

import com.miyagi.shashin.controller.ToolsController
import com.miyagi.shashin.repository.MetadataRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.boot.info.BuildProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean

@TestConfiguration
class ToolsControllerTestConfig {

    @Bean
    fun toolsController() { //: ToolsController {

//        private var metaRepository: MetadataRepository,
//        private var buildProperties: BuildProperties? = null,
//        private var healthEndpoint: HealthEndpoint? = null,
//        @Value("\${app.endpoint.url.geocode}")
//        private var geocodeUrl: String,
//        @Value("\${app.circleci.key}")
//        private var circleCiKey: String? = null,
//        @Value("\${app.github.key}")
//        private var githubKey: String? = null,
//        var messageSource: MessageSource? = null

//        return ToolsController(
//            shashinServerStartUnixMS = System.currentTimeMillis().toString(),
//            metaRepository = TODO(),
//            buildProperties = TODO(),
//            healthEndpoint = TODO(),
//            geocodeUrl = TODO(),
//            circleCiKey = TODO(),
//            githubKey = TODO(),
//            messageSource = TODO()
//        )
    }
}