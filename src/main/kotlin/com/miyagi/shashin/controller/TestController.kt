package com.miyagi.shashin.controller

import com.miyagi.shashin.model.Settings
import com.miyagi.shashin.repository.MetadataRepository
import com.sun.management.OperatingSystemMXBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate
import java.io.File
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
    private lateinit var metaRepository: MetadataRepository

    @Autowired
    private var buildProperties: BuildProperties? = null

    @Value("\${app.endpoint.url.geocode}")
    private lateinit var geocodeUrl: String

    @Secured("ROLE_ADMIN")
    @GetMapping("/test")
    fun test(model: Model): String {
        model["somevalue"] = "This is a test"



        // Add API key to header
        var settings = model.getAttribute("settings") as Settings
        val headers = HttpHeaders()
        headers.contentType = MediaType.MULTIPART_FORM_DATA
        headers.add("x-api-key", settings.getCompreFaceKey())
        var body: MultiValueMap<String, Any>? = null
        var metadata: Optional<com.miyagi.shashin.model.Metadata?>? = null
        var thumbFile: FileSystemResource? = null
        var serverUrl = ""
        var requestEntity: HttpEntity<MultiValueMap<String, Any>>
        var restTemplate: RestTemplate
        var response: String?


        // Uploaded faces
//        serverUrl = "${faceRecogUrl}api/v1/recognition/faces?subject=noah&det_prob_threshold=0.45"
//
//        body = LinkedMultiValueMap()
//        metadata = metaRepository.findById("7944b8f0-2dd2-3226-8c67-9d7350735617")
//        thumbFile = FileSystemResource(metadata.get().getThumbnailPathSmall()!!)
//        body.add("file", thumbFile)
//
//        requestEntity = HttpEntity<MultiValueMap<String, Any>>(body, headers)
//        restTemplate = RestTemplate()
//        response = restTemplate.postForObject(serverUrl, requestEntity, String::class.java)
//        println(response)



        // Recognize face
        serverUrl = "${settings.getCompreFaceServer()}api/v1/recognition/recognize"

try {
    // Noah
    body = LinkedMultiValueMap()
    metadata = metaRepository.findById("1aec30fc-871e-3e5c-956b-1442b50c6849")
    thumbFile = FileSystemResource(metadata.get().getThumbnailPathSmall()!!)
    body.add("file", thumbFile)

    requestEntity = HttpEntity<MultiValueMap<String, Any>>(body, headers)
    restTemplate = RestTemplate()
    response = restTemplate.postForObject(serverUrl, requestEntity, String::class.java)
    println(response)

    /*
        // Ryuko
        body = LinkedMultiValueMap()
        metadata = metaRepository.findById("77823c1f-9d8a-332a-94cc-a04a052f8971")
        thumbFile = FileSystemResource(metadata.get().getThumbnailPathSmall()!!)
        body.add("file", thumbFile)

        requestEntity = HttpEntity<MultiValueMap<String, Any>>(body, headers)
        restTemplate = RestTemplate()
        response = restTemplate.postForObject(serverUrl, requestEntity, String::class.java)
        println(response)

        // Mike
        body = LinkedMultiValueMap()
        metadata = metaRepository.findById("752e9360-617c-3643-baff-5b6964f7f284")
        thumbFile = FileSystemResource(metadata.get().getThumbnailPathSmall()!!)
        body.add("file", thumbFile)

        requestEntity = HttpEntity<MultiValueMap<String, Any>>(body, headers)
        restTemplate = RestTemplate()
        response = restTemplate.postForObject(serverUrl, requestEntity, String::class.java)
        println(response)*/

        // Nobody
        body = LinkedMultiValueMap()
        metadata = metaRepository.findById("4ba78ee5-e3f9-36f4-89e9-e3a93fef6c3e")
        thumbFile = FileSystemResource(metadata.get().getThumbnailPathSmall()!!)
        body.add("file", thumbFile)

        requestEntity = HttpEntity<MultiValueMap<String, Any>>(body, headers)
        restTemplate = RestTemplate()
        try {
            response = restTemplate.postForObject(serverUrl, requestEntity, String::class.java)
            println(response)
        } catch (e: Exception) {
            println("error:" + e.localizedMessage.replace("<EOL>", "").replace("400 : ", "").replace("\\s".toRegex(), ""))
        }
    } catch (e: Exception) {
        println("error:" + e.localizedMessage)
    }

/*        // Mike
        body = LinkedMultiValueMap()
        metadata = metaRepository.findById("c691b1f5-591e-395e-88f8-955093f5bbaa")
        thumbFile = FileSystemResource(metadata.get().getThumbnailPathSmall()!!)
        body.add("file", thumbFile)

        requestEntity = HttpEntity<MultiValueMap<String, Any>>(body, headers)
        restTemplate = RestTemplate()
        response = restTemplate.postForObject(serverUrl, requestEntity, String::class.java)
        println(response)*/


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
        val reachable: Boolean = pingURL(geocodeUrl, 200)
        if (reachable) {
            model["geocoderServicesAvailable"] = "OK"
        } else {
            model["geocoderServicesAvailable"] = "FAIL"
            status = "FAIL"
        }

        val settings = model.getAttribute("settings") as Settings
        val faceRecogReachable: Boolean = pingURL(settings.getCompreFaceServer()!!, 200)
        if (faceRecogReachable) {
            model["faceRecogServicesAvailable"] = "OK"
        } else {
            model["faceRecogServicesAvailable"] = "FAIL"
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