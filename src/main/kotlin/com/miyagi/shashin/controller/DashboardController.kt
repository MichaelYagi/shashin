package com.miyagi.shashin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.component.CpuMetrics
import com.miyagi.shashin.component.Message
import com.miyagi.shashin.component.StatMessage
import com.miyagi.shashin.model.Settings
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.service.FileStats
import com.miyagi.shashin.util.ApiResponse
import com.miyagi.shashin.util.MetricsUtil
import com.miyagi.shashin.util.NetworkUtils
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.annotation.SubscribeMapping
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.socket.messaging.SessionConnectEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import org.springframework.web.socket.messaging.SessionSubscribeEvent
import java.lang.management.ManagementFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.Level
import java.util.logging.Logger
import jakarta.servlet.http.HttpSession
import org.springframework.web.bind.annotation.*
import java.io.File
import java.lang.management.MemoryMXBean
import kotlin.collections.set


@Controller
class DashboardController {
    @Value("\${app.role.super}")
    private var superRole: String? = null

    @Value("\${app.role.admin}")
    private var adminRole: String? = null

    @Value("\${app.role.user}")
    private var userRole: String? = null

    @Value("#{systemProperties['com.miyagi.shashin.serverStartUnixMS']}")
    private var shashinServerStartUnixMS: String? = null

    @Autowired
    private lateinit var metadataRepository: MetadataRepository

    @Autowired
    private lateinit var albumRepository: AlbumRepository

    @Autowired
    private lateinit var favoriteRepository: FavoriteRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var commentRepository: CommentRepository

    @Autowired
    private lateinit var recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository

    @Autowired
    private lateinit var keywordRepository: KeywordRepository

    @Autowired
    private lateinit var settingsRepository: SettingsRepository

    @Autowired
    private lateinit var useragentRepository: UseragentRepository

    @Value("\${app.endpoint.url.geocode}")
    private var geocodeUrl: String? = null

    //    val osMXBean: OperatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
//    val osMXBean: OperatingSystemMXBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)
    val osMXBean = CpuMetrics()
    val memoryMXBean: MemoryMXBean = ManagementFactory.getMemoryMXBean()
    var invalidProcessCpuLoadCounter = 0
    var invalidSystemCpuLoadCounter = 0

    private var logger: Logger = Logger.getLogger(DashboardController::class.simpleName)

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()

    @MessageMapping("/statmessage")
    @SendTo("/topic/statmessages")
    @Throws(java.lang.Exception::class)
    fun sendScanMessage(message: StatMessage): Message? {
        //println("message:${message.getMessage()}")
        val metricsMap = mutableMapOf<String,Any>()

        metricsMap["initialMemoryGB"] = memoryMXBean.heapMemoryUsage.init.toDouble() / 1073741824
        metricsMap["usedHeapMemoryGB"] = memoryMXBean.heapMemoryUsage.used.toDouble() / 1073741824
        metricsMap["maxHeapMemoryGB"] = memoryMXBean.heapMemoryUsage.max.toDouble() / 1073741824
        metricsMap["committedMemoryGB"] = memoryMXBean.heapMemoryUsage.committed.toDouble() / 1073741824
//        println("Used Heap Memory GB:"+metricsMap["usedHeapMemoryGB"])
//        println("Max Heap Memory GB:"+metricsMap["maxHeapMemoryGB"])

        var processCpuLoad = osMXBean.getProcessCpuLoad()
        var systemCpuLoad = osMXBean.getSystemCpuLoad()

        if (processCpuLoad == null || processCpuLoad < 0 || processCpuLoad.isNaN()) {
            invalidProcessCpuLoadCounter++
            logger.log(Level.INFO, "Raw process CPU load: $processCpuLoad")
            processCpuLoad = 0.0
        } else if (processCpuLoad > 1) {
            invalidProcessCpuLoadCounter++
            logger.log(Level.INFO, "Raw process CPU load greater than 1.0: $processCpuLoad")
            processCpuLoad = 0.0
        }

        if (systemCpuLoad == null || systemCpuLoad < 0 || systemCpuLoad.isNaN()) {
            invalidSystemCpuLoadCounter++
            logger.log(Level.INFO, "Raw system CPU load: $systemCpuLoad")
            systemCpuLoad = 0.0
        } else if (systemCpuLoad > 1) {
            invalidSystemCpuLoadCounter++
            logger.log(Level.INFO, "Raw system CPU load greater than 1.0: $systemCpuLoad")
            systemCpuLoad = 0.0
        }

//        logger.log(Level.INFO, "Processed process CPU load:$processCpuLoad")
//        logger.log(Level.INFO, "Processed system CPU load:$systemCpuLoad")


        metricsMap["processCpuLoadPercentDouble"] = processCpuLoad
        metricsMap["systemCpuLoadPercentDouble"] = systemCpuLoad
        metricsMap["invalidSystemCpuLoadCounter"] = invalidSystemCpuLoadCounter
        metricsMap["invalidProcessCpuLoadCounter"] = invalidProcessCpuLoadCounter

        if (invalidSystemCpuLoadCounter > 5 || invalidProcessCpuLoadCounter > 5) {
            invalidSystemCpuLoadCounter = 0
            invalidProcessCpuLoadCounter = 0
        }

        val dtf = DateTimeFormatter.ofPattern("HH:mm:ss")
        val now = LocalDateTime.now()
        metricsMap["timestamp"] = now.format(dtf)

        val msg: String = mapper.writeValueAsString(metricsMap)

        val messageObj = Message()
        messageObj.setContent(msg)

        return messageObj
    }

    @SubscribeMapping("/topic/statmessages")
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

    @RequestMapping(value = ["/dashboard"], method = [RequestMethod.GET])
    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    fun getDashboard(model: Model): String {
        val module = "dashboard"
//        val response = buildDashboardData(model)
//
//        for ((k, v) in response) {
//            model[k] = v!!
//        }

        model["shashinServerStartUnixMS"] = shashinServerStartUnixMS
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/dashboard/data","/api/v1/dashboard/data"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getDashboardApi(model: Model): String {
        val settings = model.getAttribute("settings") as Settings?
        return mapper.writeValueAsString(buildDashboardData(model,settings))
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/stats/data","/api/v1/stats/data"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getStatsApi(model: Model): String {
        val settings = model.getAttribute("settings") as Settings?
        return mapper.writeValueAsString(buildDashboardData(model,settings,true))
    }

    private fun buildDashboardData(model: Model, settings: Settings?, simplified: Boolean = false): MutableMap<String, Any?> {
        var response = mutableMapOf<String, Any?>()

        val showLimit = 40

        val metricsUtil = MetricsUtil()
        metricsUtil.start("file stats")
        // Files stats

        val fileStats = FileStats()
        response = fileStats.getFileStats(model, settings)

        response["uptimeText"] = TextUtils.getServerUptimeFormatted()
        response["uptimeMS"] = TextUtils.getServerUptimeMS()

        metricsUtil.end()

        val settings = model.getAttribute("settings") as Settings?

        if (!simplified) {
            metricsUtil.start("nominatim check")
            val reachable: Boolean = NetworkUtils.checkNominatimConnection(geocodeUrl + "status.php?format=json")
            response["nominatimAvailable"] = reachable
            metricsUtil.end()

            response["compreFaceAvailable"] = null
            if (settings?.getFacialDetection() == true &&
                settings.getCompreFaceKey() != null &&
                settings.getCompreFaceKey() != "" && settings.getCompreFaceServer() != null &&
                settings.getCompreFaceServer() != ""
            ) {
                metricsUtil.start("compreFace check")
                val faceRecogServicesAvailable = NetworkUtils.checkCompreFaceConnection(
                    settings.getCompreFaceServer(),
                    settings.getCompreFaceKey()
                )
                response["compreFaceAvailable"] = faceRecogServicesAvailable
                metricsUtil.end()
            }

            metricsUtil.start("site stats")
            // Site stats
//        val photosWithPeopleTaggedCount = recognitionLabelPhotoRepository.countDistinctMetadataId()
            val peopleList = metadataRepository.findMetadataByPeople(
                settings?.getRecognitionConfidenceThreshold()!!,
                TextUtils.getObjectName()
            )
            val favoritesCount = favoriteRepository.count()
            val commentsCount = commentRepository.count()
            val albumCount = albumRepository.count()

//        response["photosWithPeopleTaggedCount"] = photosWithPeopleTaggedCount
            response["photosWithPeopleTaggedCount"] = peopleList.count()
            response["favoritesCount"] = favoritesCount
            response["commentsCount"] = commentsCount
            response["albumCount"] = albumCount
            metricsUtil.end()

            metricsUtil.start("UA stats")
            // Browser stats
            val browserCounts = useragentRepository.countByAgentName()
            val browserCountList = ArrayList<HashMap<String, Any>>()

            for (agentNameCount in browserCounts) {
                val agentNameCountMap = HashMap<String, Any>()
                var agentName = agentNameCount.getAgentName().toString()
                if (agentNameCount.getAgentName() == null) {
                    agentName = "Unknown"
                }
                agentNameCountMap["y"] = agentName
                agentNameCountMap["x"] = agentNameCount.getCount().toString().toInt()
                browserCountList.add(agentNameCountMap)
            }
            response["agentNameCountJson"] = mapper.writeValueAsString(browserCountList)

            // OS name stats
            val osNameCounts = useragentRepository.countByOsName()
            val osNameCountList = ArrayList<HashMap<String, Any>>()

            for (osNameCount in osNameCounts) {
                val osNameCountMap = HashMap<String, Any>()
                var osName = osNameCount.getOsName().toString()
                if (osNameCount.getOsName() == null) {
                    osName = "Unknown"
                }
                osNameCountMap["y"] = osName
                osNameCountMap["x"] = osNameCount.getCount().toString().toInt()
                osNameCountList.add(osNameCountMap)
            }
            response["osNameCountJson"] = mapper.writeValueAsString(osNameCountList)

            // Camera stats
            val cameraCounts = metadataRepository.countByCameraType()
            val cameraCountList = ArrayList<HashMap<String, Any>>()
            var cameraTotals = 0
            if (cameraCounts.count() > 0) {
                val maxCameraCount = cameraCounts.toList()[0].getCount()
                for (cameraCount in cameraCounts) {
                    cameraTotals++
                    if (cameraCount.getCamera() != null && cameraCount.getCount() != null && (cameraTotals <= 10 || cameraCount.getCount()!! >= showLimit) /*cameraTotals <= 15*/ /*cameraCount.getCount()!! > maxCameraCount!! * 0.05*/) {
                        val cameraCountMap = HashMap<String, Any>()
                        var cameraName = cameraCount.getCamera().toString()
                        cameraCountMap["y"] = cameraName
                        cameraCountMap["x"] = cameraCount.getCount().toString().toInt()
                        cameraCountList.add(cameraCountMap)
                    }
                }
            }
            response["cameraCountJson"] = mapper.writeValueAsString(cameraCountList)
            response["cameraTotalCount"] = cameraTotals

            // Placename stats
            val placenameCounts = metadataRepository.countByLocation()
            val placenameCountList = ArrayList<HashMap<String, Any>>()
            var placenameTotals = 0
            if (placenameCounts.count() > 0) {
                val maxPlacenameCount = placenameCounts.toList()[0].getCount()
                for (placenameCount in placenameCounts) {
                    placenameTotals++
                    if (placenameCount.getCity() != null && placenameCount.getCity()!!.isNotEmpty() &&
                        placenameCount.getProvince() != null && placenameCount.getProvince()!!.isNotEmpty() &&
                        placenameCount.getCountry() != null && placenameCount.getCountry()!!.isNotEmpty()
                    ) {
                        val placeName =
                            placenameCount.getCity() + ", " + placenameCount.getProvince() + ", " + placenameCount.getCountry()
                        if (placenameCount.getCount() != null && (placenameTotals <= 10 || placenameCount.getCount()!! >= showLimit) /*placenameTotals <= 20*/ /*placenameCount.getCount()!! > maxPlacenameCount!! * 0.05*/) {
                            val placenameCountMap = HashMap<String, Any>()
                            placenameCountMap["y"] = placeName
                            placenameCountMap["x"] = placenameCount.getCount().toString().toInt()
                            placenameCountList.add(placenameCountMap)
                        }
                    }
                }
            }
            response["placenameCountJson"] = mapper.writeValueAsString(placenameCountList)
            response["placenameTotalCount"] = placenameTotals

            val browserCount = useragentRepository.countDistinctAgentName()
            val osCount = useragentRepository.countDistinctOsName()
            response["browserTotalCount"] = browserCount
            response["osTotalCount"] = osCount
            metricsUtil.end()

            // Keyword stats
            metricsUtil.start("keyword stats")
            val keywordCounts = keywordRepository.countByKeyword()
            val keywordCountList = ArrayList<HashMap<String, Any>>()
            var keywordCount = 0
            if (keywordCounts.count() > 0) {
                val maxKwCount = keywordCounts.toList()[0].getCount()
                for (kwCount in keywordCounts) {
                    keywordCount++
                    if (kwCount.getCount() != null && (keywordCount <= 10 || kwCount.getCount()!! >= showLimit) /*keywordCount <= 15*/ /*kwCount.getCount()!! > maxKwCount!! * showLimit*/) {
                        val keywordCountMap = HashMap<String, Any>()
                        val keyword = kwCount.getKeyword().toString()
                        keywordCountMap["y"] = keyword
                        keywordCountMap["x"] = kwCount.getCount().toString().toInt()
                        keywordCountList.add(keywordCountMap)
                    }
                }
            }
            response["keywordCountJson"] = mapper.writeValueAsString(keywordCountList)
            response["keywordTotalCount"] = keywordCount
            metricsUtil.end()

            // User stats
            metricsUtil.start("user stats")
            val allowedUserCount = userRepository.countAllByIsAuthorizedIsTrueAndAuthorityEquals(userRole!!)
            val notAllowedUserCount = userRepository.countAllByIsAuthorizedIsFalseAndAuthorityEquals(userRole!!)
            val allowedAdminCount = userRepository.countAllByIsAuthorizedIsTrueAndAuthorityEquals(adminRole!!)
            val notAllowedAdminCount = userRepository.countAllByIsAuthorizedIsFalseAndAuthorityEquals(adminRole!!)
            val allowedSuperCount = userRepository.countAllByIsAuthorizedIsTrueAndAuthorityEquals(superRole!!)
            val notAllowedSuperCount = userRepository.countAllByIsAuthorizedIsFalseAndAuthorityEquals(superRole!!)
            response["allowedUserCount"] = allowedUserCount
            response["notAllowedUserCount"] = notAllowedUserCount
            response["allowedAdminCount"] = allowedAdminCount
            response["notAllowedAdminCount"] = notAllowedAdminCount
            response["allowedSuperCount"] = allowedSuperCount
            response["notAllowedSuperCount"] = notAllowedSuperCount
            metricsUtil.end()

            metricsUtil.start("image status check")
            response["mediaAvailable"] = true
            if (metadataRepository.count() > 0) {
                val metadataResult = metadataRepository.findRandomMetadata()
                if (metadataResult != null && !File(metadataResult.getPath().toString()).exists()) {
                    response["mediaAvailable"] = false
                }
            }
            metricsUtil.end()
        }

        metricsUtil.start("media stats")
        // Media stats
        val photoCount = metadataRepository.countAllByTypeContains("image")
        val videoCount = metadataRepository.countAllByTypeContains("video")
        val notLocatedCount = metadataRepository.countAllByLatIsNullAndLngIsNull()
        val hiddenCount = metadataRepository.countAllByHiddenIsTrue()
        response["photoCount"] = photoCount
        response["videoCount"] = videoCount
        response["notLocatedCount"] = notLocatedCount
        response["hiddenCount"] = hiddenCount
        metricsUtil.end()

        response["endpointSlowestProcessingTimeMS"] = metricsUtil.getMaxTime()
        response["endpointSlowestProcessingTimeModule"] = metricsUtil.getMaxTimeModule().toString()
        response["endpointFastestProcessingTimeMS"] = metricsUtil.getMinTime()
        response["endpointFastestProcessingTimeModule"] = metricsUtil.getMinTimeModule().toString()
        response["endpointAverageProcessingTimeMS"] = String.format("%.2f", metricsUtil.getAverageTime()).toDouble()
        response["endpointAllTimings"] = metricsUtil.getAllTimings()
        response["endpointProcessingTimeMS"] = metricsUtil.getTotalElapsedTime()
        response["endpointProcessingTimeText"] = metricsUtil.getTotalElapsedTime().toString() + " ms"

        response["message"] = ""
        response["msg"] = ""
        response["status"] = ApiResponse.SUCCESS.status

        return response
    }
}