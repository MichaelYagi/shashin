package com.miyagi.shashin.controller

import com.miyagi.shashin.model.PersistentLoginsDetails
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.repository.PersistentLoginsRepository
import com.miyagi.shashin.util.FileUtils
import com.sun.management.OperatingSystemMXBean
import jdk.jfr.Description
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.context.ApplicationContext
import org.springframework.core.io.FileSystemResource
import org.springframework.http.*
import org.springframework.messaging.handler.HandlerMethod
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.support.WebApplicationContextUtils
import org.springframework.web.reactive.result.method.RequestMappingInfo
import org.springframework.web.server.adapter.WebHttpHandlerBuilder.applicationContext
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.lang.management.ManagementFactory
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Controller
class TestController {

    @Autowired
    private lateinit var persistentLoginsRepository: PersistentLoginsRepository

    @Secured("ROLE_ADMIN")
    @GetMapping("/test")
    fun test(model: Model, request: HttpServletRequest, response: HttpServletResponse): String {
        model["somevalue"] = "This is a test"

        val persistentLoginsDetails = persistentLoginsRepository.findAllPersistentLoginsDetails()
        model["persistentLoginsDetails"] = persistentLoginsDetails as Any


        return "test"
    }

    @Secured("ROLE_ADMIN")
    @RequestMapping(value = ["/testvideo"], method = [RequestMethod.GET], produces = ["video/mp4","video/3gpp","video/mpeg","video/ogg","video/quicktime","video/webm"])
    @ResponseBody
    fun getTestVideo(response: HttpServletResponse?): FileSystemResource? {
        val path = "c:/Users/micha/Downloads/testVideo/PXL_20210725_213342002.mp4";
        return FileSystemResource(path)
    }

    @Secured("ROLE_ADMIN")
    @RequestMapping(value = ["/testimage"], method = [RequestMethod.GET], produces = ["image/apng","image/avif","image/gif","image/jpeg","image/png","image/svg+xml","image/svg+xml","image/webp"])
    @ResponseBody
    fun getTestImage(response: HttpServletResponse?): FileSystemResource? {
        val path = "c:/Users/micha/Downloads/testData/anotherDir/DSCF1061.JPG";
        return FileSystemResource(path)
    }

    @Secured("ROLE_ADMIN")
    @RequestMapping(value = ["/testaudio"], method = [RequestMethod.GET], produces = ["audio/3gpp","audio/aac","audio/flac","audio/mpeg","audio/mp3","audio/mp4","audio/ogg","audio/wav","audio/webm"])
    @ResponseBody
    fun getTestAudio(response: HttpServletResponse?): FileSystemResource? {
        val path = "c:/some/audio.mp3";
        return FileSystemResource(path)
    }
}