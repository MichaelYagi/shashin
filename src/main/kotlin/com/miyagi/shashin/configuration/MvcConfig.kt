package com.miyagi.shashin.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.io.FileSystemResource
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor
import org.springframework.web.servlet.i18n.SessionLocaleResolver
import org.springframework.web.servlet.resource.PathResourceResolver
import org.springframework.web.util.UrlPathHelper
import java.util.Locale

@Configuration
@PropertySource(value = ["classpath:messages_ja.properties"], encoding = "UTF-8")
class MvcConfig : WebMvcConfigurer {

    @Value("\${app.sidecar.path}")
    private val relativeSidecarDir: String? = null

    @Value("\${app.api.version}")
    private val apiVersion: String? = null

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val rootPath = FileSystemResource("").file.absolutePath

//        var thumbnailDir = "file:///$rootPath$relativeSidecarDir"+"thumbnails/"
//        thumbnailDir = thumbnailDir.replace('\\', '/').lowercase()

        var profileDir = "file:///$rootPath$relativeSidecarDir"+"profile/"
        profileDir = profileDir.replace('\\', '/').lowercase()

        registry
//            .addResourceHandler("/api/$apiVersion/thumbnails/**", "/api/$apiVersion/profile/**")
//            .addResourceLocations(thumbnailDir, profileDir)
            .addResourceHandler("/api/$apiVersion/profile/**")
            .addResourceLocations(profileDir)
            .setCachePeriod(3600)
            .resourceChain(true)
            .addResolver(PathResourceResolver())
    }

    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        val urlPathHelper = UrlPathHelper()
        urlPathHelper.isUrlDecode = true
        configurer.setUrlPathHelper(urlPathHelper)
    }

    @Bean
    fun localeResolver(): LocaleResolver {
        val slr = SessionLocaleResolver()
        slr.setDefaultLocale(Locale.ENGLISH) // Set your default locale
        return slr
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        val localeChangeInterceptor = LocaleChangeInterceptor().apply {
            paramName = "lang" // Parameter name for locale change
        }
        registry.addInterceptor(localeChangeInterceptor)
    }
}