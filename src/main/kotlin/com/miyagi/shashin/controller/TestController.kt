package com.miyagi.shashin.controller

import com.miyagi.shashin.repository.UserRepository
import com.sun.management.OperatingSystemMXBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.io.IOException
import java.lang.management.ManagementFactory
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.servlet.http.HttpServletResponse


@Controller
class TestController {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Value("\${app.endpoint.url.geocode}")
    private lateinit var geocodeUrl: String

    @Secured("ROLE_ADMIN")
    @GetMapping("/test")
    fun test(model: Model): String {
        model["somevalue"] = "This is a test"
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
        val count = userRepository.countAllByLoggedInIsTrue()
        val timingTwo = Date()
        if (count >= 0) {
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
        val reachable: Boolean = pingURL(geocodeUrl, 200)
        if (reachable) {
            model["geocoderServicesAvailable"] = "OK"
        } else {
            model["geocoderServicesAvailable"] = "FAIL"
            status = "FAIL"
        }

        model["status"] = status

        return "health"
    }

    private fun roundOffDecimal(number: Double): Any {
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.CEILING
        return df.format(number).toDouble()
    }

    /**
     * Pings a HTTP URL. This effectively sends a HEAD request and returns `true` if the response code is in
     * the 200-399 range.
     * @param url The HTTP URL to be pinged.
     * @param timeout The timeout in millis for both the connection timeout and the response read timeout. Note that
     * the total timeout is effectively two times the given timeout.
     * @return `true` if the given HTTP URL has returned response code 200-399 on a HEAD request within the
     * given timeout, otherwise `false`.
     */
    private fun pingURL(url: String, timeout: Int): Boolean {
        var url = url
        url = url.replaceFirst(
            "^https".toRegex(),
            "http"
        ) // Otherwise an exception may be thrown on invalid SSL certificates.
        return try {
            val connection: HttpURLConnection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = timeout
            connection.readTimeout = timeout
            connection.requestMethod = "HEAD"
            val responseCode: Int = connection.responseCode
            responseCode in 200..399
        } catch (exception: IOException) {
            false
        }
    }
}