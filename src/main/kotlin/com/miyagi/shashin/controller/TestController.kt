package com.miyagi.shashin.controller

import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.ImageProcessing
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.imageio.ImageIO
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Controller
class TestController {

    @Autowired
    private lateinit var persistentLoginsRepository: PersistentLoginsRepository

    @Autowired
    private lateinit var metadataRepository: MetadataRepository

    @Autowired
    private lateinit var recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository

    @Autowired
    private lateinit var recognitionLabelRepository: RecognitionLabelRepository

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private val jdbcTemplate: JdbcTemplate? = null

    @Value("\${app.role.super}")
    private var superRole: String? = null

    @Value("\${app.role.admin}")
    private var adminRole: String? = null

    private var shouldStop = AtomicBoolean(false)

    private val threadExtensionName: String = "facescan_shashinscan"

    @Value("\${app.sidecar.path}")
    private val relativeSidecarDir: String? = null

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @GetMapping("/test")
    fun test(model: Model, request: HttpServletRequest, response: HttpServletResponse): String {
        model["somevalue"] = "This is a test"

        // Retroactively create gif
//        var metadataList = metadataRepository.findAllByMediaType("video")
//
//        if (metadataList != null) {
//            var numProcessed = 0
//            val metadataCount = metadataList.count()
//            for ((index, metadata) in metadataList.withIndex()) {
//                println("iteration ${index+1} out of $metadataCount")
//                if (metadata.getThumbnailPathSmall() !== null) {
//                    val jpgVersion = metadata.getThumbnailPathSmall()
//                    val gifVersion = jpgVersion?.replace("_225.jpg", "_225.gif")
//                    println("processing $gifVersion")
//
//                    val gifFile = File(gifVersion!!)
//                    if (!gifFile.exists()) {
//                        println("gif doesn't exist")
//
//                        ImageProcessing.createVideoGif(metadata.getId(), metadataRepository)
//                        numProcessed++
//                        println("processed $gifVersion")
//                    } else {
//                        println("already exists $gifVersion")
//                    }
//
//                    println("-------------")
//                }
//            }
//            println("Number processed: $numProcessed")
//        }

        // Retroactively create 112 images
        val allMetadataList = metadataRepository.findAll()

        if (allMetadataList != null) {
            val metadataCount = allMetadataList.count()
            for ((index, metadata) in allMetadataList.withIndex()) {
                println("iteration ${index+1} out of $metadataCount")
                if (metadata != null) {
                    if (metadata.getThumbnailPathExtraSmall() == null || !File(metadata.getThumbnailPathExtraSmall()!!).exists()) {
                        val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
                        val sidecarDir = rootPath + relativeSidecarDir
                        val imageProcessing = ImageProcessing("v1", File(metadata.getPath()!!), sidecarDir, metadata)
                        val metadataObj = imageProcessing.createThumbnails()
                        if (metadataObj != null) {
                            metadataRepository.save(metadataObj)
                        }
                    }
                }
            }
        }

        return "test"
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/testvideo"], method = [RequestMethod.GET], produces = ["video/mp4","video/3gpp","video/mpeg","video/ogg","video/quicktime","video/webm"])
    @ResponseBody
    fun getTestVideo(response: HttpServletResponse?): FileSystemResource? {
        val path = "c:/Users/micha/Downloads/testVideo/PXL_20210725_213342002.mp4";
        return FileSystemResource(path)
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/testimage"], method = [RequestMethod.GET], produces = ["image/apng","image/avif","image/gif","image/jpeg","image/png","image/svg+xml","image/svg+xml","image/webp"])
    @ResponseBody
    fun getTestImage(response: HttpServletResponse?): FileSystemResource? {
        val path = "c:/Users/micha/Downloads/testData/anotherDir/DSCF1061.JPG";
        return FileSystemResource(path)
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/testaudio"], method = [RequestMethod.GET], produces = ["audio/3gpp","audio/aac","audio/flac","audio/mpeg","audio/mp3","audio/mp4","audio/ogg","audio/wav","audio/webm"])
    @ResponseBody
    fun getTestAudio(response: HttpServletResponse?): FileSystemResource? {
        val path = "c:/some/audio.mp3";
        return FileSystemResource(path)
    }
}