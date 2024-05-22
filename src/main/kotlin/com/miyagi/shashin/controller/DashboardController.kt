package com.miyagi.shashin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.component.Message
import com.miyagi.shashin.component.StatMessage
import com.miyagi.shashin.model.Settings
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.ApiResponse
import com.miyagi.shashin.util.NetworkUtils
import com.miyagi.shashin.util.TextUtils
import com.sun.management.OperatingSystemMXBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.core.io.FileSystemResource
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.annotation.SubscribeMapping
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.socket.messaging.SessionConnectEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import org.springframework.web.socket.messaging.SessionSubscribeEvent
import java.lang.management.ManagementFactory
import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.Level
import java.util.logging.Logger
import javax.servlet.http.HttpSession


@Controller
class DashboardController {
    @Value("\${app.role.admin}")
    private var adminRole: String? = null

    @Value("\${app.role.user}")
    private var userRole: String? = null

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
    private lateinit var useragentRepository: UseragentRepository

    @Value("\${app.endpoint.url.geocode}")
    private var geocodeUrl: String? = null

    private var logger: Logger = Logger.getLogger(DashboardController::class.simpleName)

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()

    @MessageMapping("/statmessage")
    @SendTo("/topic/statmessages")
    @Throws(java.lang.Exception::class)
    fun sendScanMessage(message: StatMessage): Message? {
        //println("message:${message.getMessage()}")
        val metricsMap = mutableMapOf<String,Any>()

        val memoryMXBean = ManagementFactory.getMemoryMXBean()
        metricsMap["initialMemoryGB"] = memoryMXBean.heapMemoryUsage.init.toDouble() / 1073741824
        metricsMap["usedHeapMemoryGB"] = memoryMXBean.heapMemoryUsage.used.toDouble() / 1073741824
        metricsMap["maxHeapMemoryGB"] = memoryMXBean.heapMemoryUsage.max.toDouble() / 1073741824
        metricsMap["committedMemoryGB"] = memoryMXBean.heapMemoryUsage.committed.toDouble() / 1073741824
//        println("Used Heap Memory GB:"+metricsMap["usedHeapMemoryGB"])
//        println("Max Heap Memory GB:"+metricsMap["maxHeapMemoryGB"])

        val osMXBean: OperatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean

//        println("Process CPU load:"+(osMXBean.processCpuLoad * 100).toInt())
//        println("System CPU load:"+(osMXBean.cpuLoad * 100).toInt())
        metricsMap["processCpuLoadPercentDouble"] = osMXBean.processCpuLoad
        @Suppress("DEPRECATION")
        metricsMap["systemCpuLoadPercentDouble"] = osMXBean.systemCpuLoad
        val dtf = DateTimeFormatter.ofPattern("HH:mm:ss")
        val now = LocalDateTime.now()
        metricsMap["timestamp"] = now.format(dtf)

        val msg: String = mapper.writeValueAsString(metricsMap)
//        println(msg)

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
        val response = buildDashboardData(model)

        for ((k, v) in response) {
            model[k] = v!!
        }

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    private fun buildDashboardData(model: Model): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        // Site stats
        val photosWithPeopleTaggedCount = recognitionLabelPhotoRepository.countDistinctMetadataId()
        val favoritesCount = favoriteRepository.count()
        val commentsCount = commentRepository.count()
        val albumCount = albumRepository.count()
        val keywordCount = keywordRepository.count()
        val browserCount = useragentRepository.countDistinctAgentName()
        val osCount = useragentRepository.countDistinctOsName()
        response["photosWithPeopleTaggedCount"] = photosWithPeopleTaggedCount
        response["favoritesCount"] = favoritesCount
        response["commentsCount"] = commentsCount
        response["albumCount"] = albumCount
        response["keywordCount"] = keywordCount
        response["browserTotalCount"] = browserCount
        response["osTotalCount"] = osCount

        val runtimeMXBean = ManagementFactory.getRuntimeMXBean()
        val seconds: Long = runtimeMXBean.uptime / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        response["uptime"] = (if (days > 0) (days.toString() + " day"+(if (days.toInt() == 1) "" else "s")+ " ") else "") + (if ((hours % 24) < 10) "0" else "") + (hours % 24) + ":" + (if ((minutes % 60) < 10) "0" else "") + (minutes % 60) + ":" + (if ((seconds % 60) < 10) "0" else "") + (seconds % 60)

        val reachable: Boolean = NetworkUtils.checkNominatimConnection(geocodeUrl+"status.php?format=json")
        response["nominatimAvailable"] = reachable

        val settings = model.getAttribute("settings") as Settings?
        if (settings != null && settings.getCompreFaceKey() != "" && settings.getCompreFaceServer() != "") {
            val faceRecogServicesAvailable = NetworkUtils.checkCompreFaceConnection(
                settings.getCompreFaceServer(),
                settings.getCompreFaceKey()
            )
            response["compreFaceAvailable"] = faceRecogServicesAvailable
        }

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

        // Files stats
        val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
        val sidecarDir = rootPath + model.getAttribute("relativeSidecarDir")
        var sidecarSize = 0.toLong()
        try {
            if (Files.isSymbolicLink(Paths.get(sidecarDir))) {
                sidecarSize = Files.walk(Paths.get(sidecarDir), FileVisitOption.FOLLOW_LINKS).mapToLong { p -> p.toFile().length() }.sum()
            } else {
                sidecarSize = Files.walk(Paths.get(sidecarDir)).mapToLong { p -> p.toFile().length() }.sum()
            }
        } catch(e: Exception) {
            logger.log(Level.SEVERE, "Error calculating sidecar size:"+ e.message)
        }
        response["sidecarSizeMB"] = String.format("%.2f", (sidecarSize.toDouble()/(1024 * 1024).toDouble()))

        // User stats
        val allowedUserCount = userRepository.countAllByIsAuthorizedIsTrueAndAuthorityEquals(userRole!!)
        val notAllowedUserCount = userRepository.countAllByIsAuthorizedIsFalseAndAuthorityEquals(userRole!!)
        val allowedAdminCount = userRepository.countAllByIsAuthorizedIsTrueAndAuthorityEquals(adminRole!!)
        val notAllowedAdminCount = userRepository.countAllByIsAuthorizedIsFalseAndAuthorityEquals(adminRole!!)
        response["allowedUserCount"] = allowedUserCount
        response["notAllowedUserCount"] = notAllowedUserCount
        response["allowedAdminCount"] = allowedAdminCount
        response["notAllowedAdminCount"] = notAllowedAdminCount

        // Media stats
        val photoCount = metadataRepository.countAllByTypeContains("image")
        val videoCount = metadataRepository.countAllByTypeContains("video")
        val notLocatedCount = metadataRepository.countAllByLatIsNullAndLngIsNull()
        val hiddenCount = metadataRepository.countAllByHiddenIsTrue()
        response["photoCount"] = photoCount
        response["videoCount"] = videoCount
        response["notLocatedCount"] = notLocatedCount
        response["hiddenCount"] = hiddenCount

        // Camera stats
        val cameraCounts = metadataRepository.countByCameraType()
        val cameraCountList = ArrayList<HashMap<String, Any>>()
        for (cameraCount in cameraCounts) {
            val cameraCountMap = HashMap<String, Any>()
            var cameraName = cameraCount.getCamera().toString()
            if (cameraCount.getCamera() == null) {
                cameraName = "Unknown"
            }
            cameraCountMap["y"] = cameraName
            cameraCountMap["x"] = cameraCount.getCount().toString().toInt()
            cameraCountList.add(cameraCountMap)
        }
        response["cameraCountJson"] = mapper.writeValueAsString(cameraCountList)
        response["cameraTotalCount"] = cameraCountList.count()

        // Keyword stats
        val keywordCounts = keywordRepository.countByKeyword()
        val keywordCountList = ArrayList<HashMap<String, Any>>()
        for (kwCount in keywordCounts) {
            val keywordCountMap = HashMap<String, Any>()
            val keyword = kwCount.getKeyword().toString()
            keywordCountMap["y"] = keyword
            keywordCountMap["x"] = kwCount.getCount().toString().toInt()
            keywordCountList.add(keywordCountMap)
        }
        response["keywordCountJson"] = mapper.writeValueAsString(keywordCountList)
        response["keywordTotalCount"] = keywordCount

        response["message"] = ""
        response["msg"] = ""
        response["status"] = ApiResponse.SUCCESS.status

        return response
    }
}