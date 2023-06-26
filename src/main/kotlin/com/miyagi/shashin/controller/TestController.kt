package com.miyagi.shashin.controller

import com.miyagi.shashin.repository.MetadataRepository
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
    private lateinit var metaRepository: MetadataRepository

    @Autowired
    private var buildProperties: BuildProperties? = null

    @Value("\${app.endpoint.url.geocode}")
    private lateinit var geocodeUrl: String

    @Secured("ROLE_ADMIN")
    @GetMapping("/test")
    fun test(model: Model, request: HttpServletRequest, response: HttpServletResponse): String {
        model["somevalue"] = "This is a test"




        val applicationContext =
            WebApplicationContextUtils.getRequiredWebApplicationContext(request.session.servletContext)
        val requestMappingHandlerMapping = applicationContext
            .getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping::class.java)
        val map = requestMappingHandlerMapping.handlerMethods
        model["apiEndpointsMap"] = mutableMapOf<String, String>()
        model["nonApiEndpointsMap"] = mutableMapOf<String, String>()
        val apiMap = mutableMapOf<String, String>()
        val nonApiMap = mutableMapOf<String, String>()

        map.forEach { (key, value) ->
            if (key.toString().contains("/api/v1/", ignoreCase = true) && !key.toString().contains("/docs/", ignoreCase = true)) {
                apiMap[key.toString()] = key.toString()
            } else {
                nonApiMap[key.toString()] = value.toString()
            }
        }

        model["apiEndpointsMap"] = apiMap
        model["nonApiEndpointsMap"] = nonApiMap




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

    @GetMapping("/health")
    fun getHealth(model: Model): String {

        var status = "OK"

        val timingOne = Date()
        val allMetadata = metaRepository.findAll()
        val timingTwo = Date()
        if (allMetadata.count() >= 0) {
            model["dbConnect"] = "OK"
        } else {
            model["dbConnect"] = "FAIL"
            status = "FAIL"
        }

        val diff: Long = timingTwo.time - timingOne.time

        if (diff >= 0) {
            model["dbTiming"] = SimpleDateFormat("mm:ss:SSS").format(Date(diff))
        } else {
            model["dbTiming"] = "FAIL"
            status = "FAIL"
        }

        val memoryMXBean = ManagementFactory.getMemoryMXBean()
        model["initialMemoryGB"] = roundOffDecimal(memoryMXBean.heapMemoryUsage.init.toDouble() / 1073741824)
        model["usedHeapMemoryGB"] = roundOffDecimal(memoryMXBean.heapMemoryUsage.used.toDouble() / 1073741824)
        model["maxHeapMemoryGB"] = roundOffDecimal(memoryMXBean.heapMemoryUsage.max.toDouble() / 1073741824)
        model["committedMemoryGB"] = roundOffDecimal(memoryMXBean.heapMemoryUsage.committed.toDouble() / 1073741824)
//        println("Used Heap Memory GB:"+metricsMap["usedHeapMemoryGB"])
//        println("Max Heap Memory GB:"+metricsMap["maxHeapMemoryGB"])

        val osMXBean: OperatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean

//        println("Process CPU load:"+(osMXBean.processCpuLoad * 100).toInt())
//        println("System CPU load:"+(osMXBean.cpuLoad * 100).toInt())
        model["processCpuLoadPercentDouble"] = (osMXBean.processCpuLoad * 100).toInt()
        @Suppress("DEPRECATION")
        model["systemCpuLoadPercentDouble"] = (osMXBean.systemCpuLoad * 100).toInt()
        model["os"] = System.getProperty("os.name") + " v" + System.getProperty("os.version") + " " + System.getProperty("os.arch")
        val reachable: Boolean = FileUtils.pingURL(geocodeUrl, 200)
        if (reachable) {
            model["geocoderServicesAvailable"] = "OK"
        } else {
            model["geocoderServicesAvailable"] = "FAIL"
            status = "FAIL"
        }

        val faceRecogServicesAvailable = model.getAttribute("faceRecogServicesAvailable").toString().toBoolean()
        if (faceRecogServicesAvailable) {
            model["faceRecogAvailable"] = "OK"
        } else {
            model["faceRecogAvailable"] = "FAIL"
        }

        model["buildVersion"] = if (buildProperties != null) buildProperties?.version.toString() else "Missing"

        model["status"] = status

        return "health"
    }

    private fun roundOffDecimal(number: Double): Any {
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.CEILING
        return df.format(number).toDouble()
    }
}