package com.miyagi.shashin.component

import com.miyagi.shashin.repository.SettingsRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.server.ConfigurableWebServerFactory

import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.stereotype.Component


@Component
class ServerPortCustomizer : WebServerFactoryCustomizer<ConfigurableWebServerFactory> {
    @Autowired
    private var settingsRepository: SettingsRepository? = null

    override fun customize(factory: ConfigurableWebServerFactory) {
        val settings = settingsRepository?.findFirstByOrderByIdAsc()
        if (settings != null) {
            settings.getPort()?.let { factory.setPort(it.toInt()) }
        }
    }
}