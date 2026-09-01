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
import io.micrometer.core.instrument.MeterRegistry
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
class DashboardController(
    private var metadataRepository: MetadataRepository,
    private var albumRepository: AlbumRepository,
    private var favoriteRepository: FavoriteRepository,
    private var userRepository: UserRepository,
    private var commentRepository: CommentRepository,
    private var keywordRepository: KeywordRepository,
    private var useragentRepository: UseragentRepository,
    private var fileStats: FileStats,
    meterRegistry: MeterRegistry,
    @Value("\${app.endpoint.url.geocode}")
    private var geocodeUrl: String? = null,
    @Value("\${app.role.super}")
    private var superRole: String? = null,
    @Value("\${app.role.admin}")
    private var adminRole: String? = null,
    @Value("\${app.role.user}")
    private var userRole: String? = null,
    @Value("#{systemProperties['com.miyagi.shashin.serverStartUnixMS']}")
    private var shashinServerStartUnixMS: String? = null
) {
    val osMXBean = CpuMetrics(meterRegistry)
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

            response["argusAvailable"] = null
            if (!settings?.getArgusServer().isNullOrBlank() &&
                !settings?.getArgusKey().isNullOrBlank()
            ) {
                metricsUtil.start("argus check")
                val faceRecogServicesAvailable = NetworkUtils.checkArgusConnection(
                    settings.getArgusServer(),
                    settings.getArgusKey()
                )
                response["argusAvailable"] = faceRecogServicesAvailable
                metricsUtil.end()
            }

            metricsUtil.start("site stats")
            // Site stats
            val favoritesCount = favoriteRepository.count()
            val commentsCount = commentRepository.count()
            val albumCount = albumRepository.count()
            val photosWithPeopleTaggedCount = metadataRepository.countDistinctPeopleTagged(TextUtils.getObjectName())

            response["photosWithPeopleTaggedCount"] = photosWithPeopleTaggedCount
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
                agentNameCountMap["y"] = agentNameCount.getAgentName() ?: "Unknown"
                agentNameCountMap["x"] = agentNameCount.getCount() ?: 0
                browserCountList.add(agentNameCountMap)
            }
            response["agentNameCountJson"] = mapper.writeValueAsString(browserCountList)
            response["browserTotalCount"] = browserCountList.size

            // OS name stats
            val osNameCounts = useragentRepository.countByOsName()
            val osNameCountList = ArrayList<HashMap<String, Any>>()
            for (osNameCount in osNameCounts) {
                val osNameCountMap = HashMap<String, Any>()
                osNameCountMap["y"] = osNameCount.getOsName() ?: "Unknown"
                osNameCountMap["x"] = osNameCount.getCount() ?: 0
                osNameCountList.add(osNameCountMap)
            }
            response["osNameCountJson"] = mapper.writeValueAsString(osNameCountList)
            response["osTotalCount"] = osNameCountList.size

            // Camera stats
            val cameraCounts = metadataRepository.countByCameraType()
            val cameraCountList = ArrayList<HashMap<String, Any>>()
            var cameraTotals = 0
            for (cameraCount in cameraCounts) {
                cameraTotals++
                if (cameraCount.getCamera() != null && cameraCount.getCount() != null && (cameraTotals <= 10 || cameraCount.getCount()!! >= showLimit)) {
                    val cameraCountMap = HashMap<String, Any>()
                    cameraCountMap["y"] = cameraCount.getCamera().toString()
                    cameraCountMap["x"] = cameraCount.getCount()!!
                    cameraCountList.add(cameraCountMap)
                }
            }
            response["cameraCountJson"] = mapper.writeValueAsString(cameraCountList)
            response["cameraTotalCount"] = cameraTotals

            // Placename stats
            val placenameCounts = metadataRepository.countByLocation()
            val placenameCountList = ArrayList<HashMap<String, Any>>()
            var placenameTotals = 0
            for (placenameCount in placenameCounts) {
                placenameTotals++
                if (!placenameCount.getCity().isNullOrEmpty() &&
                    !placenameCount.getProvince().isNullOrEmpty() &&
                    !placenameCount.getCountry().isNullOrEmpty()
                ) {
                    val placeName = "${placenameCount.getCity()}, ${placenameCount.getProvince()}, ${placenameCount.getCountry()}"
                    if (placenameCount.getCount() != null && (placenameTotals <= 10 || placenameCount.getCount()!! >= showLimit)) {
                        val placenameCountMap = HashMap<String, Any>()
                        placenameCountMap["y"] = placeName
                        placenameCountMap["x"] = placenameCount.getCount()!!
                        placenameCountList.add(placenameCountMap)
                    }
                }
            }
            response["placenameCountJson"] = mapper.writeValueAsString(placenameCountList)
            response["placenameTotalCount"] = placenameTotals
            metricsUtil.end()

            // Keyword stats
            metricsUtil.start("keyword stats")
            val keywordCounts = keywordRepository.countByKeyword()
            val keywordCountList = ArrayList<HashMap<String, Any>>()
            var keywordCount = 0
            for (kwCount in keywordCounts) {
                keywordCount++
                if (kwCount.getCount() != null && (keywordCount <= 10 || kwCount.getCount()!! >= showLimit)) {
                    val keywordCountMap = HashMap<String, Any>()
                    keywordCountMap["y"] = kwCount.getKeyword().toString()
                    keywordCountMap["x"] = kwCount.getCount()!!
                    keywordCountList.add(keywordCountMap)
                }
            }
            response["keywordCountJson"] = mapper.writeValueAsString(keywordCountList)
            response["keywordTotalCount"] = keywordCount
            metricsUtil.end()

            // User stats
            metricsUtil.start("user stats")
            val roleCounts = userRepository.getUserRoleCounts(listOf(userRole!!, adminRole!!, superRole!!))
            val roleMap = roleCounts.associateBy { it.getAuthority() }
            response["allowedUserCount"] = roleMap[userRole]?.getAllowedCount() ?: 0
            response["notAllowedUserCount"] = roleMap[userRole]?.getNotAllowedCount() ?: 0
            response["allowedAdminCount"] = roleMap[adminRole]?.getAllowedCount() ?: 0
            response["notAllowedAdminCount"] = roleMap[adminRole]?.getNotAllowedCount() ?: 0
            response["allowedSuperCount"] = roleMap[superRole]?.getAllowedCount() ?: 0
            response["notAllowedSuperCount"] = roleMap[superRole]?.getNotAllowedCount() ?: 0
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
        // Media stats — single query instead of four separate table scans
        val mediaCounts = metadataRepository.getMediaCounts()
        response["photoCount"] = mediaCounts.getPhotoCount() ?: 0
        response["videoCount"] = mediaCounts.getVideoCount() ?: 0
        response["notLocatedCount"] = mediaCounts.getNotLocatedCount() ?: 0
        response["hiddenCount"] = mediaCounts.getHiddenCount() ?: 0
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