package com.miyagi.shashin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.component.Message
import com.miyagi.shashin.component.StatMessage
import com.miyagi.shashin.repository.*
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
import java.lang.Double.parseDouble
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.text.DecimalFormat
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
    private lateinit var mediaDirRepository: MediaDirectoryRepository

    @Autowired
    private lateinit var albumRepository: AlbumRepository

    @Autowired
    private lateinit var albumPhotoRepository: AlbumPhotoRepository

    @Autowired
    private lateinit var albumPhotoCommentRepository: AlbumPhotoCommentRepository

    @Autowired
    private lateinit var favoriteRepository: FavoriteRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var commentRepository: CommentRepository

    @Autowired
    private lateinit var recognitionLabelRepository: RecognitionLabelRepository

    @Autowired
    private lateinit var recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository

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
        metricsMap["systemCpuLoadPercentDouble"] = osMXBean.cpuLoad
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
    @Secured("ROLE_ADMIN")
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
        response["photosWithPeopleTaggedCount"] = photosWithPeopleTaggedCount
        response["favoritesCount"] = favoritesCount
        response["commentsCount"] = commentsCount
        response["albumCount"] = albumCount

        // Files stats
        val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
        val sidecarDir = rootPath + model.getAttribute("relativeSidecarDir")
        var sidecarSize = 0.toLong()
        try {
            sidecarSize = Files.walk(Paths.get(sidecarDir)).mapToLong { p -> p.toFile().length() }.sum()
        } catch(e: Exception) {
            logger.log(Level.SEVERE, "Error calculating sidecar size:"+ e.message)
        }
        response["sidecarSizeMB"] = sidecarSize/(1024 * 1024)

        // User stats
        val allowedUserCount = userRepository.countAllByIsAllowedIsTrueAndAuthorityEquals(userRole!!)
        val notAllowedUserCount = userRepository.countAllByIsAllowedIsFalseAndAuthorityEquals(userRole!!)
        val allowedAdminCount = userRepository.countAllByIsAllowedIsTrueAndAuthorityEquals(adminRole!!)
        val notAllowedAdminCount = userRepository.countAllByIsAllowedIsFalseAndAuthorityEquals(adminRole!!)
        val loggedInCount = userRepository.countAllByLoggedInIsTrue()
        response["allowedUserCount"] = allowedUserCount
        response["notAllowedUserCount"] = notAllowedUserCount
        response["allowedAdminCount"] = allowedAdminCount
        response["notAllowedAdminCount"] = notAllowedAdminCount
        response["loggedInCount"] = loggedInCount

        // Media stats
        val photoCount = metadataRepository.countAllByTypeContains("image")
        val videoCount = metadataRepository.countAllByTypeContains("video")
        val notLocatedCount = metadataRepository.countAllByLatIsNullAndLngIsNull()
        val hiddenCount = metadataRepository.countAllByHiddenIsTrue()
        response["photoCount"] = photoCount
        response["videoCount"] = videoCount
        response["notLocatedCount"] = notLocatedCount
        response["hiddenCount"] = hiddenCount
        val cameraCounts = metadataRepository.countByCameraType()
        val cameraCountList = ArrayList<HashMap<String, Any>>()
        for (cameraCount in cameraCounts) {
            val cameraCountMap = HashMap<String, Any>()
            var cameraName = cameraCount.getCamera().toString()
            if (cameraCount.getCamera() == null) {
                cameraName = "Unknown"
            }
            cameraCountMap["x"] = cameraName
            cameraCountMap["y"] = cameraCount.getCount().toString().toInt()
            cameraCountList.add(cameraCountMap)
        }
        response["cameraCountJson"] = mapper.writeValueAsString(cameraCountList)
        response["cameraTotalCount"] = cameraCountList.count()

        response["message"] = ""

        return response
    }
}