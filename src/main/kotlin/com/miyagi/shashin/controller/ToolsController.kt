package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.Settings
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.repository.PersistentLoginsRepository
import com.miyagi.shashin.util.ApiResponse
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.NetworkUtils
import com.sun.management.OperatingSystemMXBean
import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.geometry.Positions
import org.jsoup.Jsoup
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.UrlResource
import org.springframework.http.*
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*
import java.awt.image.BufferedImage
import java.awt.image.RenderedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.lang.management.ManagementFactory
import java.math.RoundingMode
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import javax.activation.URLDataSource
import javax.imageio.ImageIO
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.io.path.isDirectory


@Controller
class ToolsController {

    @Autowired
    private lateinit var metaRepository: MetadataRepository

    @Autowired
    private var buildProperties: BuildProperties? = null

    @Value("\${app.endpoint.url.geocode}")
    private lateinit var geocodeUrl: String

    @Autowired
    private lateinit var persistentLoginsRepository: PersistentLoginsRepository

    private var logger: Logger = Logger.getLogger(ToolsController::class.simpleName)


    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, Any?>()

//    @RequestMapping(value = ["tools/minifyassets"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
//    @ResponseBody
//    fun getMinifyAssets(model: Model): String {
//        val response = mutableMapOf<String, Any?>()
//
//        var input = "static/js/site/app.js"
//        var ouput = "static/js/site/app.min.js"
//        var resource = ClassPathResource("static/js/site/app.js")
//        var fis = FileInputStream(resource.file)
//        var fileContents = getFileContent(fis,"UTF-8")
//        var compiled = jscompile(fileContents)
//
//        println(compiled)
//
//        response["msg"] = ApiResponse.SUCCESS.status
//        response["message"] = "Success"
//
//        return mapper.writeValueAsString(response)
//    }

//    private fun jscompile(code: String?): String? {
//        val compiler = Compiler()
//        val options = CompilerOptions()
//        // Advanced mode is used here, but additional options could be set, too.
//        CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(
//            options
//        )
//
//        // To get the complete set of externs, the logic in
//        // CompilerRunner.getDefaultExterns() should be used here.
//        val extern = SourceFile.fromCode(
//            "externs.js",
//            "function alert(x) {}"
//        )
//
//        // The dummy input name "input.js" is used here so that any warnings or
//        // errors will cite line numbers in terms of input.js.
//        val input = SourceFile.fromCode("input.js", code)
//
//        // compile() returns a Result, but it is not needed here.
//        compiler.compile(extern, input, options)
//
//        // The compiler is responsible for generating the compiled code; it is not
//        // accessible via the Result.
//        return compiler.toSource()
//    }
//
//    @Throws(IOException::class)
//    private fun getFileContent(
//        fis: FileInputStream,
//        encoding: String
//    ): String {
//        BufferedReader(InputStreamReader(fis, encoding)).use { br ->
//            val sb = StringBuilder()
//            var line: String?
//            while (br.readLine().also { line = it } != null) {
//                sb.append(line)
//                sb.append('\n')
//            }
//            return sb.toString()
//        }
//    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/tools/remembermetokens"], method = [RequestMethod.GET])
    fun getPersistentTokens(model: Model, request: HttpServletRequest, response: HttpServletResponse): String? {
        var currentRememberMeToken = ""
        for (cookie in request.cookies!!) {
            if (cookie.name.contains("remember-me")) {
                currentRememberMeToken = cookie.value

                break
            }
        }

//        val persistentLoginsDetails = persistentLoginsRepository.findAllPersistentLoginsDetails()
//        model["persistentLoginsDetails"] = persistentLoginsDetails as Any
        model["currentTimeMS"] = System.currentTimeMillis()
        model["currentRememberMeToken"] = currentRememberMeToken

        return "tokens"
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/tools/imagescraper"], method = [RequestMethod.GET])
    fun getScrapeImages(model: Model, request: HttpServletRequest, response: HttpServletResponse): String? {
        model["pageUrl"] = ""
        model["numOfImages"] = 0
        model["toastMessage"] = ""
        model["status"] = ""
        model["imgList"] = mutableListOf<MutableMap<String,Any>>()
        model["srcList"] = mutableListOf<String>()

        return "imagescraper"
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/tools/imagescraper"], method = [RequestMethod.POST])
    fun postScrapeImages(model: Model, request: HttpServletRequest, response: HttpServletResponse, @RequestBody formData: MultiValueMap<String, String>): String? {
        model["pageUrl"] = ""
        model["numOfImages"] = 0
        model["toastMessage"] = ""
        model["status"] = ""
        model["imgList"] = mutableListOf<MutableMap<String,Any>>()
        model["srcList"] = mutableListOf<String>()

        if (formData.containsKey("pageUrl")) {
            val pageUrl: String = java.lang.String.valueOf(formData.getFirst("pageUrl"))
            model["pageUrl"] = pageUrl

            if (NetworkUtils.pingURL(java.lang.String.valueOf(formData.getFirst("pageUrl")))) {
                // eg. https://store.line.me/stickershop/author/939305/en
                val imgList = mutableListOf<MutableMap<String, String>>()
                val srcList = mutableListOf<String>()
                val doc = Jsoup.connect(pageUrl).get()
                val imgTags = doc.getElementsByTag("img")

                for (imgTag in imgTags) {
                    if (imgTag.hasAttr("src") && imgTag.attr("src").isNotEmpty()) {
                        val imgObj = mutableMapOf<String, String>()
                        val url = URL(imgTag.attr("src"))
                        val image = ImageIO.read(url)
                        val thumbnail = Thumbnails.of(image)
                            .outputQuality(1.0)
                            .imageType(BufferedImage.TYPE_INT_ARGB)
                            .outputFormat("png")
//                            .height(FileUtils.thumbnailHeight())
                            .crop(Positions.CENTER)
                            .size(209, 209)
                            .asBufferedImage()
                        val base64String = imgToBase64String(thumbnail, "png")
                        imgObj["imgThumbBase64"] = base64String
                        imgObj["imgThumbHeight"] = thumbnail.height.toString()
                        imgObj["imgThumbWidth"] = thumbnail.width.toString()
                        imgObj["imgRealSrc"] = imgTag.attr("src")
                        imgObj["imgTitle"] = if (imgTag.hasAttr("title")) imgTag.attr("title") else ""
                        imgObj["imgAlt"] = if (imgTag.hasAttr("alt")) imgTag.attr("alt") else ""
                        imgList.add(imgObj)
                        srcList.add(imgTag.attr("src"))
                    }
                }

                model["srcList"] = srcList
                model["imgList"] = imgList
                model["numOfImages"] = imgTags.size
                model["toastMessage"] = "Page processed"
                model["status"] = ApiResponse.SUCCESS.status
            } else {
                model["toastMessage"] = "Invalid URL"
                model["status"] = ApiResponse.FAIL.status
            }
        } else {
            model["toastMessage"] = "Something went wrong"
            model["status"] = ApiResponse.FAIL.status
        }

        return "imagescraper"
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/tools/download/image"], method = [RequestMethod.POST])
    fun downloadScrapeImage(model: Model, request: HttpServletRequest, response: HttpServletResponse, @RequestParam("imageUrl") imageUrl: String): ResponseEntity<UrlResource> {
        if (imageUrl != "") {
            val resource = UrlResource(imageUrl)
            val headers = HttpHeaders()

            try {
                headers.contentLength = resource.contentLength()

                // Sanitize filename
                val filename = resource.filename
                response.setHeader("Content-Disposition", "attachment; filename=$filename")
                headers.setCacheControl(CacheControl.maxAge(24, TimeUnit.HOURS))
                return ResponseEntity<UrlResource>(resource, headers, HttpStatus.OK)
            } catch (e: Exception) {
                logger.log(
                    Level.SEVERE,
                    "Error setting image ResponseEntity: " + e.message
                )
                return ResponseEntity<UrlResource>(null, null, HttpStatus.NOT_FOUND)
            }
        } else {
            return ResponseEntity<UrlResource>(null, null, HttpStatus.NOT_FOUND)
        }
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/tools/download/images"], method = [RequestMethod.POST])
    fun downloadScrapeImages(model: Model, request: HttpServletRequest, response: HttpServletResponse, @RequestParam("pageUrl") pageUrl: String): ResponseEntity<InputStreamResource>? {
        if (pageUrl != "" && NetworkUtils.pingURL(java.lang.String.valueOf(pageUrl))) {
            // eg. https://store.line.me/stickershop/author/939305/en
            val imageUrls = mutableListOf<String>()
            val doc = Jsoup.connect(pageUrl).get()
            val imgTags = doc.getElementsByTag("img")

            for (imgTag in imgTags) {
                if (imgTag.hasAttr("src") && imgTag.attr("src").isNotEmpty()) {
                    imageUrls.add(imgTag.attr("src"))
                }
            }

            val tempExportBaseDir = Files.createTempDirectory("images")

            for ((index, imageUrl) in imageUrls.withIndex()) {
                val image = ImageIO.read(URL(imageUrl))
                val tempFileTo =
                    File("$tempExportBaseDir/$index.png")
                ImageIO.write(image, "png", tempFileTo)
            }

            if (tempExportBaseDir.isDirectory() && tempExportBaseDir.toList().isNotEmpty()) {
                val tempDir = tempExportBaseDir.toFile()
                val outputZipFile = FileUtils.zipFolder(tempDir, "ImageScraperDownload")
                FileUtils.deleteDirectory(tempDir)

                if (outputZipFile != null) {
                    outputZipFile.deleteOnExit()

                    val resource = InputStreamResource(FileInputStream(outputZipFile))
                    val contentLength = outputZipFile.length()

                    val headers = HttpHeaders()
                    headers.add(HttpHeaders.SET_COOKIE, ResponseCookie.from("ShashinImageScraper",
                        outputZipFile.name.replace("\\s".toRegex(), "_").lowercase(Locale.getDefault())
                    ).path("/").build().toString())
                    headers.add(HttpHeaders.SET_COOKIE, ResponseCookie.from("ShashinImageScraperSize",contentLength.toString()).path("/").build().toString())
                    headers.add(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + outputZipFile.name
                    )
                    headers.add("Cache-Control", "no-cache, no-store, must-revalidate")
                    headers.add("Pragma", "no-cache")
                    headers.add("Expires", "0")

                    return ResponseEntity.ok()
                        .headers(headers)
                        .contentLength(contentLength)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource)
                }
            }
        }

        return ResponseEntity<InputStreamResource>(null, null, HttpStatus.NOT_FOUND)
    }

    private fun imgToBase64String(img: RenderedImage?, formatName: String? = "jpg"): String {
        try {
            val out = ByteArrayOutputStream()
            ImageIO.write(img, formatName, out)
            val bytes = out.toByteArray()

            val base64bytes = String(Base64.getEncoder().encode(bytes))
            if (base64bytes.isNotBlank()) {
                return "data:image/png;base64,$base64bytes"
            } else {
                logger.log(Level.WARNING, "No result converting image to Base64")
                return ""
            }
        } catch (ioe: IOException) {
            logger.log(Level.WARNING, "Error converting image to Base64. Message: ${ioe.message}")
            return ""
        }
    }

    @GetMapping("/health")
    fun getHealth(model: Model): String {

        var status = "OK"

        model["dbCount"] = 0
        val timingOne = Date()
        val allMetadata = metaRepository.findAll()
        val timingTwo = Date()
        if (allMetadata.count() >= 0) {
            model["dbConnect"] = "OK"
            model["dbCount"] = allMetadata.count()
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

        val cores = Runtime.getRuntime().availableProcessors()
        if (cores < 1) {
            status = "FAIL"
        }
        model["availableCores"] = cores

//        println("Process CPU load:"+(osMXBean.processCpuLoad * 100).toInt())
//        println("System CPU load:"+(osMXBean.cpuLoad * 100).toInt())
        model["processCpuLoadPercentDouble"] = (osMXBean.processCpuLoad * 100).toInt()
        @Suppress("DEPRECATION")
        model["systemCpuLoadPercentDouble"] = (osMXBean.systemCpuLoad * 100).toInt()
        model["os"] = System.getProperty("os.name") + " v" + System.getProperty("os.version") + " " + System.getProperty("os.arch")
        val reachable: Boolean = NetworkUtils.checkNominatimConnection(geocodeUrl+"status.php?format=json")
        if (reachable) {
            model["geocoderServicesAvailable"] = "OK"
        } else {
            model["geocoderServicesAvailable"] = "FAIL"
            status = "FAIL"
        }

        // If enabled - status fail if not available
        val settings = model.getAttribute("settings") as Settings?
        if (settings != null && settings.getCompreFaceKey() != "" && settings.getCompreFaceServer() != "") {
            val faceRecogServicesAvailable = NetworkUtils.checkCompreFaceConnection(
                settings.getCompreFaceServer(),
                settings.getCompreFaceKey()
            )
            if (faceRecogServicesAvailable) {
                model["faceRecogAvailable"] = "OK"
            } else {
                model["faceRecogAvailable"] = "FAIL"
                status = "FAIL"
            }
        } else {
            model["faceRecogAvailable"] = "N/A"
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