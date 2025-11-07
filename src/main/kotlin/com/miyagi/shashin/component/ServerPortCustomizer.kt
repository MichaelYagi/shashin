package com.miyagi.shashin.component

import com.miyagi.shashin.repository.SettingsRepository
import org.springframework.boot.web.server.ConfigurableWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.stereotype.Component


@Component
class ServerPortCustomizer(private var settingsRepository: SettingsRepository? = null) : WebServerFactoryCustomizer<ConfigurableWebServerFactory> {

    override fun customize(factory: ConfigurableWebServerFactory) {
        val settings = settingsRepository?.findFirstByOrderByIdAsc()
        settings?.getPort()?.let { factory.setPort(it.toInt()) }
    }
}