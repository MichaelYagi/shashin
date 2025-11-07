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
    fun toolsController(
        metaRepository: MetadataRepository,
        buildProperties: BuildProperties? = null,
        healthEndpoint: HealthEndpoint? = null,
        @Value("\${app.endpoint.url.geocode}")
        geocodeUrl: String,
        @Value("\${app.circleci.key}")
        circleCiKey: String? = null,
        @Value("\${app.github.key}")
        githubKey: String? = null,
        @Value("#{systemProperties['com.miyagi.shashin.serverStartUnixMS'] ?: '0'}")
        shashinServerStartUnixMS: String,
        messageSource: MessageSource? = null
    ): ToolsController {
        return ToolsController(
            shashinServerStartUnixMS = shashinServerStartUnixMS,
            metaRepository = metaRepository,
            buildProperties = buildProperties,
            healthEndpoint = healthEndpoint,
            geocodeUrl = geocodeUrl,
            circleCiKey = circleCiKey,
            githubKey = githubKey,
            messageSource = messageSource
        )
    }
}