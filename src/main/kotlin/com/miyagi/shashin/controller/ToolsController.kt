package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.component.Message
import com.miyagi.shashin.component.ScaperMessage
import com.miyagi.shashin.model.SearchHistory
import com.miyagi.shashin.model.Settings
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.repository.SearchHistoryRepository
import com.miyagi.shashin.util.*
import com.sun.management.OperatingSystemMXBean
import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.geometry.Positions
import org.jsoup.Jsoup
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.HealthComponent
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.boot.info.BuildProperties
import org.springframework.context.event.EventListener
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.UrlResource
import org.springframework.http.*
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.annotation.SubscribeMapping
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import org.springframework.web.socket.messaging.SessionConnectEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import org.springframework.web.socket.messaging.SessionSubscribeEvent
import java.awt.image.BufferedImage
import java.awt.image.RenderedImage
import java.io.*
import java.lang.management.ManagementFactory
import java.math.RoundingMode
import java.net.URISyntaxException
import java.net.URL
import java.nio.file.Files
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession
import javax.xml.bind.DatatypeConverter
import kotlin.io.path.isDirectory


@Controller
class ToolsController {

    @Autowired
    private lateinit var metaRepository: MetadataRepository

    @Autowired
    private var buildProperties: BuildProperties? = null

    @Autowired
    private val searchHistoryRepository: SearchHistoryRepository? = null

    @Value("\${app.endpoint.url.geocode}")
    private lateinit var geocodeUrl: String

    @Value("\${app.circleci.key}")
    private lateinit var circleCiKey: String

    @Autowired
    private var healthEndpoint: HealthEndpoint? = null

    private var logger: Logger = Logger.getLogger(ToolsController::class.simpleName)

    private var currentIndex = 0
    private var totalIndex = 0

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, Any?>()

    @MessageMapping("/scrapermessage")
    @SendTo("/topic/scrapermessages")
    @Throws(java.lang.Exception::class)
    fun sendScanMessage(message: ScaperMessage): Message? {
        //println("message:${message.getMessage()}")
        val scraperMap = mutableMapOf<String,Int>()
        scraperMap["currentIndex"] = currentIndex
        scraperMap["totalIndex"] = totalIndex

        val msg: String = mapper.writeValueAsString(scraperMap)
//        println(msg)

        val messageObj = Message()
        messageObj.setContent(msg)

        return messageObj
    }

    @SubscribeMapping("/topic/scrapermessages")
    fun subscribe(
        session: HttpSession,
        @PathVariable pipelineId: String,
        @PathVariable topic: String
    ) {}

    @EventListener
    fun onApplicationEvent(event: SessionConnectEvent) {}

    @EventListener
    fun onApplicationEvent(event: SessionDisconnectEvent) {}

    @EventListener
    fun handleSubscribeEvent(event: SessionSubscribeEvent) {}

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
        model["activePage"] = "imagescraper"

        return "imagescraper"
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/tools/imagescraper"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun postScrapeImages(model: Model, @RequestBody requestBody: JsonNode): String? {
        val imageScraperMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})

        val response = mutableMapOf<String, Any?>()
        response["pageUrl"] = ""
        response["numOfImages"] = 0
        response["toastMessage"] = ""
        response["status"] = ""
        response["msg"] = ""
        response["imgList"] = mutableListOf<MutableMap<String,Any>>()
        response["srcList"] = mutableListOf<String>()
        response["activePage"] = "imagescraper"

        if (imageScraperMap.containsKey("pageUrl")) {
            val pageUrl: String = java.lang.String.valueOf(imageScraperMap["pageUrl"])

            val currentUserObj = model.getAttribute("currentUser") as User?
            if (currentUserObj != null && NetworkUtils.pingURL(java.lang.String.valueOf(pageUrl))) {
                response["pageUrl"] = pageUrl

                val searchImageScraperTermCount = searchHistoryRepository?.countByUserIdAndTermIgnoreCase(currentUserObj.getId(), pageUrl, SearchHistoryTypes.UrlHistorySearch.type)
                val searchHistory: SearchHistory?
                if (searchImageScraperTermCount == 0) {
                    searchHistory = SearchHistory()
                    searchHistory.setTerm(pageUrl)
                    searchHistory.setSearchType(SearchHistoryTypes.UrlHistorySearch.type)
                    searchHistory.setUserId(currentUserObj.getId())
                    searchHistory.setCreatedAt(TextUtils.getCurrentTimestamp())
                    searchHistory.setModifiedAt(TextUtils.getCurrentTimestamp())
                } else {
                    searchHistory =
                        searchHistoryRepository?.findDistinctByUserIdAndTerm(currentUserObj.getId(), pageUrl, SearchHistoryTypes.UrlHistorySearch.type)
                    searchHistory?.setModifiedAt(TextUtils.getCurrentTimestamp())
                }

                if (searchHistory != null) {
                    searchHistoryRepository?.save(searchHistory)
                }

                val searchImageScraperHistoryCount = searchHistoryRepository?.countByUserId(currentUserObj.getId(), SearchHistoryTypes.UrlHistorySearch.type)
                val searchHistoryLimit = model.getAttribute("searchHistoryLimit").toString().toInt()

                if (searchImageScraperHistoryCount != null && searchImageScraperHistoryCount >= searchHistoryLimit) {
                    val searchHistoryRefresh = searchHistoryRepository?.findTopNByUserIdOrderByIdDesc(currentUserObj.getId(), 1, SearchHistoryTypes.UrlHistorySearch.type)
                    if (searchHistoryRefresh != null && searchHistoryRefresh.count() > 0) {
                        searchHistoryRepository?.deleteByIdAndSearchType(searchHistoryRefresh.last().getId(), SearchHistoryTypes.UrlHistorySearch.type)
                    }
                }

                val pageUrlObj = URL(pageUrl)
                // eg. https://store.line.me/stickershop/author/939305/en
                val imgList = mutableListOf<MutableMap<String, Any>>()
                val srcList = mutableListOf<String>()
                try {
                    val doc = Jsoup.connect(pageUrl).get()
                    val imgTags = doc.getElementsByTag("img")
                    val totalImages = imgTags.count()
                    totalIndex = imgTags.count()

                    for ((index, imgTag) in imgTags.withIndex()) {
                        currentIndex = index
                        if (imgTag.hasAttr("src") && imgTag.attr("src").isNotEmpty()) {
                            val imgObj = mutableMapOf<String, Any>()
                            var srcUrl = imgTag.attr("src").toString()
                            var image: BufferedImage? = null
                            var urlWithoutParameters: String? = null
                            try {
                                if (srcUrl.startsWith("data:image")) {
                                    urlWithoutParameters = srcUrl
                                    val base64Image: String = srcUrl.split(",")[1]
                                    val imageBytes = DatatypeConverter.parseBase64Binary(base64Image)
                                    image = ImageIO.read(ByteArrayInputStream(imageBytes))
                                } else {
                                    if (!srcUrl.startsWith("http")) {
                                        val path: String =
                                            pageUrlObj.file.substring(0, pageUrlObj.file.lastIndexOf('/'))
                                        val base: String = (pageUrlObj.protocol + "://" + pageUrlObj.host) + path
                                        srcUrl = "$base/$srcUrl"
                                    }
                                    urlWithoutParameters = getUrlWithoutParameters(srcUrl)
                                    val url = URL(urlWithoutParameters)
                                    image = ImageIO.read(url)
                                }
                            } catch (e: Exception) {
                                logger.log(
                                    Level.WARNING,
                                    e.message
                                )
                            }

                            if (image != null && urlWithoutParameters != null && image.height > 1 && image.width > 1 && !srcList.contains(urlWithoutParameters)) {
                                var width = 209
                                var height = 209

                                if (image.width < 209) {
                                    width = image.width
                                }

                                if (image.height < 209) {
                                    height = image.height
                                }

                                val thumbnail = Thumbnails.of(image)
                                    .outputQuality(1.0)
                                    .imageType(BufferedImage.TYPE_INT_ARGB)
                                    .outputFormat("png")
//                            .height(FileUtils.thumbnailHeight())
                                    .crop(Positions.CENTER)
                                    .size(width, height)
                                    .asBufferedImage()
                                val base64String = imgToBase64String(thumbnail, "png")
                                imgObj["imgRealHeight"] = image.height
                                imgObj["imgRealWidth"] = image.width
                                imgObj["imgThumbBase64"] = base64String
                                imgObj["imgThumbHeight"] = thumbnail.height
                                imgObj["imgThumbWidth"] = thumbnail.width
                                imgObj["imgRealSrc"] = urlWithoutParameters
                                imgObj["imgTitle"] = if (imgTag.hasAttr("title")) imgTag.attr("title") else ""
                                imgObj["imgAlt"] = if (imgTag.hasAttr("alt")) imgTag.attr("alt") else ""

                                imgList.add(imgObj)
                                srcList.add(urlWithoutParameters)
                                logger.log(
                                    Level.INFO,
                                    "${index + 1}/$totalImages - Processed image at $urlWithoutParameters"
                                )
                            } else {
                                logger.log(
                                    Level.WARNING,
                                    "${index + 1}/$totalImages - Could not process image at $urlWithoutParameters"
                                )
                            }
                        }
                    }

                    response["srcList"] = srcList
                    response["imgList"] = imgList.sortedBy { it["imgRealWidth"].toString().toInt() }
                    response["numOfImages"] = srcList.size
                    val plural = if (srcList.size != 1) "s" else ""
                    response["toastMessage"] = "Page processed with ${srcList.size} result$plural"
                    response["status"] = ApiResponse.SUCCESS.status
                } catch (e: Exception) {
                    logger.log(
                        Level.SEVERE,
                        "Error getting URL: " + e.message
                    )
                    response["toastMessage"] = "Error getting URL: " + e.message
                }
            } else {
                response["toastMessage"] = "Invalid URL"
                response["status"] = ApiResponse.FAIL.status
            }
        } else {
            response["toastMessage"] = "Something went wrong"
            response["status"] = ApiResponse.FAIL.status
        }

        return mapper.writeValueAsString(response)
    }

    @Throws(URISyntaxException::class)
    private fun getUrlWithoutParameters(url: String): String {
        return if (url.contains("?")) {
            url.substring(0, url.lastIndexOf("?"))
        } else {
            url
        }
//        val uri = URI(url)
//        return URI(
//            uri.scheme,
//            uri.getAuthority(),
//            uri.getPath(),
//            null,  // Ignore the query part of the input url
//            uri.getFragment()
//        ).toString()
    }

    @RequestMapping(value = ["/tools/imagescraper/history"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun getImageScraperSearchHistory(model: Model, request: HttpServletRequest, @RequestParam size: Optional<Int>): String {
        val searchHistoryLimit = size.orElse(model.getAttribute("searchHistoryLimit").toString().toInt())
        val response = mutableMapOf<String, Any?>()
        response["urlHistoryList"] = mutableListOf<SearchHistory>()
        response["msg"] = "Not authorized"
        response["status"] = ApiResponse.FAIL.status

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            response["msg"] = "Success!"
            response["status"] = ApiResponse.SUCCESS.status

            val urlHistoryList =
                searchHistoryRepository?.findTopNByUserIdOrderByModifiedAtDesc(currentUserObj.getId(), searchHistoryLimit, SearchHistoryTypes.UrlHistorySearch.type)
            if (urlHistoryList != null) {
                response["urlHistoryList"] = urlHistoryList
            }
        }
        return mapper.writeValueAsString(response)
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/tools/download/image"], method = [RequestMethod.POST])
    fun downloadScrapeImage(model: Model, request: HttpServletRequest, response: HttpServletResponse, @RequestParam("imageUrl") imageUrl: String): ResponseEntity<UrlResource> {
        if (imageUrl != "") {
            val resource: UrlResource?
            if (imageUrl.startsWith("data:image")) {
                val base64Image: String = imageUrl.split(",")[1]
                val imageBytes = DatatypeConverter.parseBase64Binary(base64Image)
                val image = ImageIO.read(ByteArrayInputStream(imageBytes))
                val path = System.getProperty("java.io.tmpdir") + "/image.png"
                val tempFile = File(path)
                ImageIO.write(image, "png", tempFile)
                resource = UrlResource("file:$path")
            } else {
                resource = UrlResource(imageUrl)
            }
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
            try {
                val doc = Jsoup.connect(pageUrl).get()
                val imgTags = doc.getElementsByTag("img")

                for (imgTag in imgTags) {
                    if (imgTag.hasAttr("src") && imgTag.attr("src").isNotEmpty()) {
                        val srcUrl = imgTag.attr("src").toString()
                        val urlWithoutParameters = getUrlWithoutParameters(srcUrl)
                        imageUrls.add(urlWithoutParameters)
                    }
                }

                val tempExportBaseDir = Files.createTempDirectory("images")
                val srcList = mutableListOf<String>()

                for ((index, imageUrl) in imageUrls.withIndex()) {
                    var currentImageUrl = imageUrl
                    var image: BufferedImage?
                    var urlWithoutParameters: String?

                    if (currentImageUrl.startsWith("data:image")) {
                        urlWithoutParameters = currentImageUrl
                        val base64Image: String = currentImageUrl.split(",")[1]
                        val imageBytes = DatatypeConverter.parseBase64Binary(base64Image)
                        image = ImageIO.read(ByteArrayInputStream(imageBytes))
                    } else {
                        if (!currentImageUrl.startsWith("http")) {
                            val pageUrlObj = URL(pageUrl)
                            val path: String = pageUrlObj.file.substring(0, pageUrlObj.file.lastIndexOf('/'))
                            val base: String = (pageUrlObj.protocol + "://" + pageUrlObj.host) + path
                            currentImageUrl = "$base/$currentImageUrl"
                        }
                        urlWithoutParameters = getUrlWithoutParameters(currentImageUrl)
                        try {
                            val url = URL(urlWithoutParameters)
                            image = ImageIO.read(url)
                        } catch (e: Exception) {
                            logger.log(
                                Level.WARNING,
                                "${index + 1}/${imageUrls.size} - Not a valid url. $urlWithoutParameters"
                            )
                            continue
                        }
                    }

                    if (image != null && image.height > 1 && image.width > 1 && !srcList.contains(urlWithoutParameters)) {
                        srcList.add(urlWithoutParameters)

                        val tempFileTo =
                            File("$tempExportBaseDir/${index+1}.png")
                        ImageIO.write(image, "png", tempFileTo)
                        logger.log(
                            Level.INFO,
                            "${index + 1}/${imageUrls.size} - Processed image at $urlWithoutParameters"
                        )
                    }
                }

                if (tempExportBaseDir.isDirectory() && tempExportBaseDir.toList().isNotEmpty()) {
                    val tempDir = tempExportBaseDir.toFile()
                    val outputZipFile = FileUtils.zipFolder(tempDir, "ShashinScrapedImages")
                    FileUtils.deleteDirectory(tempDir)

                    if (outputZipFile != null) {
                        outputZipFile.deleteOnExit()

                        val resource = InputStreamResource(FileInputStream(outputZipFile))
                        val contentLength = outputZipFile.length()

                        val headers = HttpHeaders()
                        headers.add(
                            HttpHeaders.SET_COOKIE, ResponseCookie.from(
                                "ShashinImageScraper",
                                outputZipFile.name.replace("\\s".toRegex(), "_").lowercase(Locale.getDefault())
                            ).path("/").build().toString()
                        )
                        headers.add(HttpHeaders.SET_COOKIE,
                            ResponseCookie.from("ShashinImageScraperSize", contentLength.toString()).path("/").build()
                                .toString()
                        )
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
            } catch (e: Exception) {
                logger.log(
                    Level.SEVERE,
                    "Error getting URL: " + e.message
                )
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

        for ((k, v) in buildHealthData(model)) {
            model[k] = v!!
        }

        model["localServerTime"] = TextUtils.getCurrentTimestampTZ()

        return "health"
    }

    @RequestMapping(value = ["/api/v1/status"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun getStatusApi(model: Model): String {
        val healthData = buildHealthData(model)
        return "{\"status\":\""+healthData["status"]+"\"}"
    }

    @RequestMapping(value = ["/api/v1/health"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun getHealthApi(model: Model): String {
        return mapper.writeValueAsString(buildHealthData(model))
    }

    private fun buildHealthData(model: Model): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        val requestTimingStart = Date()

        var status = "OK"

        response["status"] = status

        response["utcTimestampMS"] = System.currentTimeMillis()

        val health: HealthComponent? = healthEndpoint!!.health()
        if (health == null || health.status.code != "UP") {
            status = "FAIL"
        }

        response["uptime"] = TextUtils.getServerUptime()

        val nominatimTimingStart = Date()
        val reachable: Boolean = NetworkUtils.checkNominatimConnection(geocodeUrl+"status.php?format=json")
        if (reachable) {
            response["nominatimAvailable"] = "OK"
        } else {
            response["nominatimAvailable"] = "FAIL"
            status = "FAIL"
        }
        val nominatimTimingEnd = Date()
        val nominatimTimingDiff: Long = nominatimTimingEnd.time - nominatimTimingStart.time
//        response["nominatimTiming"] = SimpleDateFormat("mm:ss.SSS").format(Date(nominatimTimingDiff))
        logger.log(Level.INFO, "HealthEP - Nominatim connection time: ${SimpleDateFormat("mm:ss.SSS").format(Date(nominatimTimingDiff))}")

        // If enabled - status fail if not available
        val settings = model.getAttribute("settings") as Settings?
        if (settings?.getCompreFaceKey() != null &&
            settings.getCompreFaceKey() != "" &&
            settings.getCompreFaceServer() != null &&
            settings.getCompreFaceServer() != "")
        {
            val compreFaceTimingStart = Date()
            val faceRecogServicesAvailable = NetworkUtils.checkCompreFaceConnection(
                settings.getCompreFaceServer(),
                settings.getCompreFaceKey()
            )
            if (faceRecogServicesAvailable) {
                response["compreFaceAvailable"] = "OK"
            } else {
                response["compreFaceAvailable"] = "FAIL"
                status = "FAIL"
            }
            val compreFaceTimingEnd = Date()
            val compreFaceTimingDiff: Long = compreFaceTimingEnd.time - compreFaceTimingStart.time
//            response["compreFaceTiming"] = SimpleDateFormat("mm:ss.SSS").format(Date(compreFaceTimingDiff))
            logger.log(Level.INFO, "HealthEP - CompreFace connection time: ${SimpleDateFormat("mm:ss.SSS").format(Date(compreFaceTimingDiff))}")
        }

        val dbTimingStart = Date()
        val metadataResult = metaRepository.findAllByOffsetAndLimit(0,500)
        val dbTimingEnd = Date()
        var sqlLiteQueryCount = 0

        try {
            response["sqlLiteAvailable"] = "OK"
//            response["sqlLiteQueryCount"] = metadataResult.count()
            sqlLiteQueryCount = metadataResult.count()
        } catch (e: Exception) {
            response["sqlLiteAvailable"] = "FAIL"
//            response["sqlLiteQueryCount"] = sqlLiteQueryCount
            status = "FAIL"
            logger.log(Level.WARNING, "HealthEP - Error querying SQLLite: ${e.message}")
        }

        val circleciTimingStart = Date()
        val passing: Boolean = NetworkUtils.checkCircleCiStatus(circleCiKey)
        if (passing) {
            response["circleCIBuild"] = "OK"
        } else {
            response["circleCIBuild"] = "FAIL"
            // Don't include as part of status, credits might run out resulting
            // status = "FAIL"
        }
        val circleciTimingEnd = Date()
        val circleciTimingDiff: Long = circleciTimingEnd.time - circleciTimingStart.time
//        response["circleCIBuildTiming"] = SimpleDateFormat("mm:ss.SSS").format(Date(circleciTimingDiff))
        logger.log(Level.INFO, "HealthEP - CircleCI connection time: ${SimpleDateFormat("mm:ss.SSS").format(Date(circleciTimingDiff))}")

        val dbTimingDiff: Long = dbTimingEnd.time - dbTimingStart.time
//        response["sqlLiteQueryTiming"] = SimpleDateFormat("mm:ss.SSS").format(Date(dbTimingDiff))
        logger.log(Level.INFO, "HealthEP - SQLite query time for $sqlLiteQueryCount records: ${SimpleDateFormat("mm:ss.SSS").format(Date(dbTimingDiff))}")

        response["buildVersion"] = if (buildProperties != null) buildProperties?.version.toString() else "Missing"

        val systemMap = mutableMapOf<String, Any?>()
        val memoryMXBean = ManagementFactory.getMemoryMXBean()
        systemMap["initialMemoryGB"] = roundOffDecimal(memoryMXBean.heapMemoryUsage.init.toDouble() / 1073741824)
        systemMap["usedHeapMemoryGB"] = roundOffDecimal(memoryMXBean.heapMemoryUsage.used.toDouble() / 1073741824)
        systemMap["maxHeapMemoryGB"] = roundOffDecimal(memoryMXBean.heapMemoryUsage.max.toDouble() / 1073741824)
        systemMap["committedMemoryGB"] = roundOffDecimal(memoryMXBean.heapMemoryUsage.committed.toDouble() / 1073741824)

        val osMXBean: OperatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean

        val cores = Runtime.getRuntime().availableProcessors()
        if (cores < 1) {
            status = "FAIL"
        }
        systemMap["availableCores"] = cores

//        println("Process CPU load:"+(osMXBean.processCpuLoad * 100).toInt())
//        println("System CPU load:"+(osMXBean.cpuLoad * 100).toInt())
        systemMap["processCpuLoadPercent"] = (osMXBean.processCpuLoad * 100).toInt()
        @Suppress("DEPRECATION")
        systemMap["systemCpuLoadPercent"] = (osMXBean.systemCpuLoad * 100).toInt()
        systemMap["os"] = System.getProperty("os.name") + " v" + System.getProperty("os.version") + " " + System.getProperty("os.arch")
        response["system"] = systemMap

        val requestTimingEnd = Date()

        val requestTimingDiff: Long = requestTimingEnd.time - requestTimingStart.time

//        response["requestTiming"] = SimpleDateFormat("mm:ss:SSS").format(Date(requestTimingDiff))
        logger.log(Level.INFO, "HealthEP - Total request time: ${SimpleDateFormat("mm:ss:SSS").format(Date(requestTimingDiff))}")

        response["status"] = status

        return response
    }

    private fun roundOffDecimal(number: Double): Any {
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.CEILING
        return df.format(number).toDouble()
    }
}