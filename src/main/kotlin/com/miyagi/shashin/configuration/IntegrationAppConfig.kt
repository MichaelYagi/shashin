package com.miyagi.shashin.configuration

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

@Configuration
class IntegrationAppConfig {
    private var logger: Logger = Logger.getLogger(IntegrationAppConfig::class.simpleName)

    @Bean
    @ConditionalOnMissingBean(BuildProperties::class)
    fun buildProperties(): BuildProperties = BuildProperties(Properties()).also {
        logger.log(Level.WARNING, "BuildProperties bean did not auto-load, creating mock BuildProperties")
        return BuildProperties(Properties())
    }
}