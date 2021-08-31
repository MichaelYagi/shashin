package com.miyagi.shashin.controller

import com.miyagi.shashin.repository.MetadataRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import javax.servlet.http.HttpServletResponse

@Controller
class MediaServiceController {

    @Autowired
    private lateinit var metadataRepository: MetadataRepository

    @RequestMapping(value = ["/api/v1/video/{metadataId}"], method = [RequestMethod.GET], produces = ["video/mp4","video/3gpp","video/mpeg","video/ogg","video/quicktime","video/webm"])
    @ResponseBody
    fun getVideo(response: HttpServletResponse?, @PathVariable metadataId: String): FileSystemResource? {
        val metadataObj = metadataRepository.findById(metadataId)
        val path = metadataObj.get().getPath()!!
        return FileSystemResource(path)
    }

    @RequestMapping(value = ["/api/v1/image/{metadataId}"], method = [RequestMethod.GET], produces = ["image/apng","image/avif","image/gif","image/jpeg","image/png","image/svg+xml","image/svg+xml","image/webp"])
    @ResponseBody
    fun getImage(response: HttpServletResponse?, @PathVariable metadataId: String): FileSystemResource? {
        val metadataObj = metadataRepository.findById(metadataId)
        val path = metadataObj.get().getPath()!!
        return FileSystemResource(path)
    }

    @RequestMapping(value = ["/api/v1/audio/{metadataId}"], method = [RequestMethod.GET], produces = ["audio/3gpp","audio/aac","audio/flac","audio/mpeg","audio/mp3","audio/mp4","audio/ogg","audio/wav","audio/webm"])
    @ResponseBody
    fun getAudio(response: HttpServletResponse?, @PathVariable metadataId: String): FileSystemResource? {
        val metadataObj = metadataRepository.findById(metadataId)
        val path = metadataObj.get().getPath()!!
        return FileSystemResource(path)
    }
}