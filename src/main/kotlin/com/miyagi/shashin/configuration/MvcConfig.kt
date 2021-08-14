package com.miyagi.shashin.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.FileSystemResource
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.PathResourceResolver

@Configuration
class MvcConfig : WebMvcConfigurer {

    @Value("\${app.sidecar.path}")
    private val relativeSidecarDir: String? = null
    @Value("\${app.api.version}")
    private val apiVersion: String? = null
    @Value("\${app.mediaDir}")
    private val mediaDir: String? = null

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val rootPath = FileSystemResource("").file.absolutePath
        val thumbnailDir = "file:///$rootPath$relativeSidecarDir/thumbnails/"

        registry
            .addResourceHandler("/api/$apiVersion/thumbnails/**","/api/$apiVersion/original/video/**")
            .addResourceLocations(thumbnailDir, "file:///$mediaDir")
            .setCachePeriod(3600)
            .resourceChain(true)
            .addResolver(PathResourceResolver())
    }
}