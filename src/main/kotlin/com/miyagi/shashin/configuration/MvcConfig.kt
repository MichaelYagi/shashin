package com.miyagi.shashin.configuration

import com.miyagi.shashin.repository.MediaDirectoryRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.FileSystemResource
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.PathResourceResolver
import java.nio.file.Paths

@Configuration
class MvcConfig : WebMvcConfigurer {

    @Value("\${app.sidecar.path}")
    private val relativeSidecarDir: String? = null

    @Value("\${app.api.version}")
    private val apiVersion: String? = null

    @Autowired
    private val mediaDirRepository: MediaDirectoryRepository? = null

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val rootPath = FileSystemResource("").file.absolutePath
        var thumbnailDir = "file:///$rootPath$relativeSidecarDir"+"thumbnails/"
        thumbnailDir = thumbnailDir.replace('\\', '/').lowercase()

        val mediaDirs = mediaDirRepository?.findAll()
        if (mediaDirs != null) {
            for (mediaDir in mediaDirs) {
                val _mediaDir = mediaDir?.getDirectory().toString().lowercase()
                val parentDir = Paths.get(_mediaDir)
                val parentDirString = parentDir.fileName.toString().lowercase()

                registry
                    .addResourceHandler("/api/$apiVersion/original/video/$parentDirString/**")
                    .addResourceLocations("file:///$_mediaDir/")
                    .setCachePeriod(3600)
                    .resourceChain(true)
                    .addResolver(PathResourceResolver())
            }
        }

        registry
            .addResourceHandler("/api/$apiVersion/thumbnails/**")
            .addResourceLocations(thumbnailDir)
            .setCachePeriod(3600)
            .resourceChain(true)
            .addResolver(PathResourceResolver())
    }
}