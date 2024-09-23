package com.miyagi.shashin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.Album
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.Notification
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.AlbumRepository
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.repository.NotificationRepository
import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.util.ApiResponse
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.MetricsUtil
import com.miyagi.shashin.util.TextUtils
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.http.*
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import ws.schild.jave.Encoder
import ws.schild.jave.MultimediaObject
import ws.schild.jave.encode.AudioAttributes
import ws.schild.jave.encode.EncodingAttributes
import ws.schild.jave.encode.VideoAttributes
import ws.schild.jave.encode.enums.PresetEnum
import ws.schild.jave.info.VideoSize
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import jakarta.activation.URLDataSource
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import kotlin.text.split


@Controller
class MediaServiceController {

    @Autowired
    private lateinit var metadataRepository: MetadataRepository

    @Autowired
    private lateinit var albumRepository: AlbumRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Value("\${app.sidecar.path}")
    private var relativeSidecarDir: String? = null

    private var logger: Logger = Logger.getLogger(MediaServiceController::class.simpleName)

    private var validFileNameRegex = "[^a-zA-Z0-9.-]".toRegex()

    @RequestMapping(value = ["/api/v1/video/{metadataId}"], method = [RequestMethod.GET], produces = ["video/mp4","video/3gpp","video/mpeg","video/ogg","video/quicktime","video/webm"])
    @ResponseBody
    @Throws(java.io.IOException::class)
    fun getVideo(response: HttpServletResponse?, request: HttpServletRequest?, @PathVariable metadataId: String): ResponseEntity<FileSystemResource> {
        return processVideo(metadataId, request, response)
    }

    private fun processVideo(metadataId: String?, request: HttpServletRequest?, response: HttpServletResponse?): ResponseEntity<FileSystemResource> {
        var metadataObj = metadataRepository.findById(metadataId!!)

        if (metadataObj.isPresent && !metadataObj.get().getType().isNullOrBlank() && metadataObj.get().getType()?.contains("video")!!) {
            var path = metadataObj.get().getPath()!!
            val metadata = metadataObj.get()

            var mp4MajorBrand = ""

            // metadata/<folder>/<fileName>.exif.yaml
            val jsonNode = FileUtils.convertExifToJsonNode(metadata.getFolder()!!, metadata.getFileName()!!, relativeSidecarDir!!)

            if (jsonNode != null && jsonNode.has("MP4-MajorBrand")) {
                mp4MajorBrand = jsonNode.get("MP4-MajorBrand").textValue()
            }

            if (metadata.getType() != null &&
                (metadata.getType()!!.lowercase().contains("mp4") || metadata.getType()!!.lowercase().contains("quicktime")) &&
                (
                        ((metadata.getCompressionType() == null || metadata.getCompressionType()!!.lowercase() == "unknown") &&
                                metadata.getExpectedExtension() != null &&
                                metadata.getExpectedExtension()!!.lowercase() == "mov" &&
                                File(metadata.getPath()!!).extension.lowercase() == "mov") ||
                                (metadata.getCompressionType() != null && metadata.getCompressionType()!!.lowercase() != "h.264") &&
                                (mp4MajorBrand.lowercase().contains("mpeg"))
                        )
            ) {
                logger.log(Level.INFO, "Converting video " + metadata.getPath() + " to mp4.")
                val metricsUtil = MetricsUtil()

                metricsUtil.start("Converting video to mp4")

                /* Step 1. Declaring source file and Target file */
                val source = File(path)

                val tempFilePath = System.getProperty("java.io.tmpdir") + "/temp.mp4"
                if (Files.exists(Paths.get(tempFilePath))) {
                    val tempFile = File(tempFilePath)
                    tempFile.delete()
                }
                val target = File(tempFilePath)

                /* Step 2. Set Audio Attributes for conversion*/
                val audio = AudioAttributes()
                audio.setCodec("aac")
                audio.setBitRate(64000)
                audio.setChannels(2)
                audio.setSamplingRate(44100)

                /* Step 3. Set Video Attributes for conversion*/
                val video = VideoAttributes()
                video.setCodec("h264")
                // video.setX264Profile(X264_PROFILE.BASELINE)
                // More the frames and higher bitrate means more quality and size,
                // keep it low based on devices like mobile
                // Here 160 kbps video is 160000
                val kbps = 700
                video.setBitRate(kbps*1000)
                video.setFrameRate(25)
                val width = if (metadata.getOriginalImageWidth() == null) FileUtils.thumbnailHeight() else metadata.getOriginalImageWidth()!!
                val height = if (metadata.getOriginalImageHeight() == null) FileUtils.thumbnailHeight() else metadata.getOriginalImageHeight()!!
                video.setSize(VideoSize(width, height))
                video.setFaststart(true)
                video.setQuality(4)

                // If video is 3 minutes or more, set to ultrafast
                var preset = PresetEnum.SUPERFAST.presetName

                val duration = metadata.getDuration()
                if (duration == "0:00" || duration == null) {
                    preset = PresetEnum.ULTRAFAST.presetName
                    video.setPreset(preset)
                } else {
                    val timeArray = duration.split(":")
                    if (timeArray.size > 1) {
                        if (timeArray.size > 2) {
                            val hour = timeArray[0].toInt()
                            if (hour > 0) {
                                preset = PresetEnum.ULTRAFAST.presetName
                                video.setPreset(preset)
                            }
                        } else {
                            val minute = timeArray[0].toInt()
                            if (minute > 2) {
                                preset = PresetEnum.ULTRAFAST.presetName
                                video.setPreset(preset)
                            }
                        }
                    }
                }
                logger.log(Level.INFO, "Using $preset preset to convert video based on duration $duration.")

                video.setPreset(preset)

                /* Step 4. Set Encoding Attributes*/
                val attrs = EncodingAttributes()
                attrs.setOutputFormat("mp4")
                attrs.setAudioAttributes(audio)
                attrs.setVideoAttributes(video)


                metricsUtil.end()
                metricsUtil.start("Converting video to mp4 - encoding")

                /* Step 5. Do the Encoding*/
                try {
                    val encoder = Encoder()
                    encoder.encode(MultimediaObject(source), target, attrs)
                    path = target.path
                } catch (e: Exception) {
                    /*Handle here the video failure*/
                    logger.log(
                        Level.SEVERE,
                        "Could not convert video " + metadata.getPath() + " to mp4: " + e.message
                    )
                }

                metricsUtil.end()
            }

            val userIp = TextUtils.getClientIp(request)
            if (userIp !== null && !TextUtils.isLocalIp(userIp)) {
                val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
                logger.log(Level.INFO, "IP $userIp played video ${metadataObj.get().getTitle()}' at ${sdtf.format(Date())}")
            }

            return getVideoFactory(request, response, metadata, path)
        } else {
            Thread {
                val admins = userRepository.findAllAdmins()
                val userIp = TextUtils.getClientIp(request)
                if (userIp !== null && !TextUtils.isLocalIp(userIp)) {
                    val notificationObjList = mutableListOf<Notification>()
                    val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                    sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
                    for (admin in admins) {
                        val notificationObj = Notification()
                        notificationObj.setUserId(admin.getId())
                        notificationObj.setCreatedAt(getCurrentTimestamp())
                        notificationObj.setModifiedAt(getCurrentTimestamp())
                        notificationObj.setRead(false)
                        val message =
                            "IP <a href='https://ipgeolocation.io/ip-location/$userIp' target='_blank'>$userIp</a> tried to play invalid video at ${
                                sdtf.format(Date())
                            }"
                        notificationObj.setMessage(message)
                        notificationObjList.add(notificationObj)
                    }

                    if (notificationObjList.isNotEmpty()) {
                        notificationRepository.saveAll(notificationObjList)
                    }
                }
            }.start()

            return ResponseEntity<FileSystemResource>(null, null, HttpStatus.NOT_FOUND)
        }
    }

    @RequestMapping(value = ["/api/v1/video/{metadataId}/download"], method = [RequestMethod.GET], produces = ["video/mp4","video/3gpp","video/mpeg","video/ogg","video/quicktime","video/webm"])
    @ResponseBody
    @Throws(java.io.IOException::class)
    fun getVideoDownload(model: Model, response: HttpServletResponse?, request: HttpServletRequest?, @PathVariable metadataId: String): ResponseEntity<FileSystemResource>? {
        val metadataObj = metadataRepository.findById(metadataId)

        if (metadataObj.isPresent && !metadataObj.get().getType().isNullOrBlank() && metadataObj.get().getType()?.contains("video")!!) {
            val userIp = TextUtils.getClientIp(request)
            if (userIp !== null && !TextUtils.isLocalIp(userIp)) {
                val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
                logger.log(Level.INFO, "IP $userIp downloaded video ${metadataObj.get().getTitle()}' at ${sdtf.format(Date())}")
            }

            return getVideoFactory(request, response, metadataObj.get(), metadataObj.get().getPath()!!, true)
        } else {
            Thread {
                val admins = userRepository.findAllAdmins()
                val userIp = TextUtils.getClientIp(request)
                if (userIp !== null && !TextUtils.isLocalIp(userIp)) {
                    val notificationObjList = mutableListOf<Notification>()
                    val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                    sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
                    for (admin in admins) {
                        val notificationObj = Notification()
                        notificationObj.setUserId(admin.getId())
                        notificationObj.setCreatedAt(getCurrentTimestamp())
                        notificationObj.setModifiedAt(getCurrentTimestamp())
                        notificationObj.setRead(false)
                        val message =
                            "IP <a href='https://ipgeolocation.io/ip-location/$userIp' target='_blank'>$userIp</a> tried to download video with metadata ID $metadataId at ${
                                sdtf.format(Date())
                            }"
                        notificationObj.setMessage(message)
                        notificationObjList.add(notificationObj)
                    }

                    if (notificationObjList.isNotEmpty()) {
                        notificationRepository.saveAll(notificationObjList)
                    }
                }
            }.start()

            return ResponseEntity<FileSystemResource>(null, null, HttpStatus.NOT_FOUND)
        }
    }

    private fun getVideoFactory(request: HttpServletRequest?, response: HttpServletResponse?, metadataObj: Metadata, path: String, attachFile: Boolean = false): ResponseEntity<FileSystemResource> {
        metadataObj.setLastAccessedAt(getCurrentTimestamp())

        val currentUserObj = request?.session?.getAttribute("CurrentUser") as User?
        if (currentUserObj != null && currentUserObj.getId() > 0) {
            metadataObj.setLastAccessedBy(currentUserObj.getId())
        }

        metadataRepository.save(metadataObj)

        var resource = FileSystemResource(path)
        val headers = HttpHeaders()
        try {
            headers.contentLength = resource.contentLength()
            if (metadataObj.getType() != null && "/" in metadataObj.getType()!!) {
                val typeList = metadataObj.getType()!!.split("/")
                if (typeList.count() == 2) {
                    headers.contentType = MediaType(typeList[0], typeList[1])
                }
            }

            if (attachFile) {
                val filename = resource.filename.replace(validFileNameRegex, "_")
                response?.setHeader("Content-Disposition", "attachment; filename=$filename")
            }

            headers.setCacheControl(CacheControl.maxAge(24, TimeUnit.HOURS))
            return ResponseEntity<FileSystemResource>(resource, headers, HttpStatus.OK)
        } catch (e: Exception) {
            logger.log(
                Level.SEVERE,
                "Error setting video ResponseEntity for "+path+": " + e.message
            )

            val source = URLDataSource(this.javaClass.getResource("/static/images/fnf.png"))
            resource = FileSystemResource(source.url.path)
            headers.contentLength = resource.contentLength()
            if (metadataObj.getType() != null && "/" in metadataObj.getType()!!) {
                val typeList = metadataObj.getType()!!.split("/")
                if (typeList.count() == 2) {
                    headers.contentType = MediaType(typeList[0], typeList[1])
                }
            }
            if (attachFile) {
                // Sanitize filename
                val filename = resource.filename.replace(validFileNameRegex, "_")
                response?.setHeader("Content-Disposition", "attachment; filename=$filename")
            }
            headers.setCacheControl(CacheControl.maxAge(24, TimeUnit.HOURS))
            return ResponseEntity<FileSystemResource>(resource, headers, HttpStatus.OK)
        }
    }

    @RequestMapping(value = ["/video/{metadataId}/player"], method = [RequestMethod.GET])
    fun getVideoPlayer(model: Model, request: HttpServletRequest?, @PathVariable metadataId: String): String {
        return setModel(metadataId, model, request, "player")
    }

    @RequestMapping(value = ["/image/{metadataId}/viewer"], method = [RequestMethod.GET])
    fun getImageViewer(model: Model, request: HttpServletRequest?, @PathVariable metadataId: String): String {
        return setModel(metadataId, model, request, "viewer")
    }

    private fun setModel(metadataId: String, model: Model, request: HttpServletRequest?, module: String): String {
        model["metadataObj"] = Metadata()
        val metadataObj = metadataRepository.findById(metadataId)
        val metadata = metadataObj.get()

        // Updated viewed date
        if (metadataObj.isPresent) {
            model["metadataObj"] = metadata
            metadata.setLastAccessedAt(getCurrentTimestamp())

            val currentUserObj = request?.session?.getAttribute("CurrentUser") as User?
            if (currentUserObj != null && currentUserObj.getId() > 0) {
                metadata.setLastAccessedBy(currentUserObj.getId())
            }

            metadataRepository.save(metadata)
        }
        
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)

        return module
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/api/v1/media/metadata/{metadataId}", "/media/metadata/{metadataId}"], method = [(RequestMethod.GET)], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Throws(java.io.IOException::class)
    fun getMediaMetadata(model: Model, request: HttpServletRequest, @PathVariable metadataId: String): String {
        val mapper = ObjectMapper()
        val resp = mutableMapOf<String, Any?>()
        val currentUser = model.getAttribute("currentUser") as User?
        resp["metadata"] = mutableMapOf<String, Any?>()
        resp["status"] = ApiResponse.FAIL.status
        resp["albumIds"] = mutableListOf<Int>()
        resp["shortPlaceName"] = ""
        resp["msg"] = ""

        var baseUrlBuilder = ServletUriComponentsBuilder.fromRequestUri(request).replacePath(null)
        if (request.scheme == "https") {
            baseUrlBuilder = baseUrlBuilder.scheme("https")
        }
        val baseUrl = baseUrlBuilder.build().toUriString()
        resp["baseUrl"] = baseUrl

        if (currentUser != null) {
            val metadata = metadataRepository.findByMetadataId(metadataId)

            if (metadata != null) {
                resp["albumIds"] = albumRepository.findAlbumIdsByMetadataId(metadata.getId())
                resp["metadata"] = metadata
                resp["shortPlaceName"] = TextUtils.formatPlaceNameForHeader(metadata.getPlaceName())
                resp["status"] = ApiResponse.SUCCESS.status
                resp["msg"] = ""
                logger.log(Level.INFO, "Image metadata ID ${metadata.getId()}")
            }
        }

        return mapper.writeValueAsString(resp)
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/api/v1/random/image/{type}", "/random/image/{type}"], method = [(RequestMethod.GET)], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Throws(java.io.IOException::class)
    fun getRandomImageByType(model: Model, request: HttpServletRequest, @PathVariable type: String): String {
        val mapper = ObjectMapper()
        val resp = mutableMapOf<String, Any?>()
        val currentUser = model.getAttribute("currentUser") as User?
        resp["metadata"] = mutableMapOf<String, Any?>()
        resp["status"] = ApiResponse.FAIL.status
        resp["albumIds"] = mutableListOf<Int>()
        resp["msg"] = ""

        var baseUrlBuilder = ServletUriComponentsBuilder.fromRequestUri(request).replacePath(null)
        if (request.scheme == "https") {
            baseUrlBuilder = baseUrlBuilder.scheme("https")
        }
        val baseUrl = baseUrlBuilder.build().toUriString()
        resp["baseUrl"] = baseUrl

        if (currentUser != null) {
            val randomMetadata = (if (currentUser.getAuthority()!! == "ROLE_ADMIN" || currentUser.getAuthority()!! == "ROLE_SUPER") {
                metadataRepository.findRandomMetadataMedia(type)
            } else {
                metadataRepository.findRandomAlbumMediaByUser(currentUser.getId(), type)
            })

            if (randomMetadata != null) {
                resp["albumIds"] = albumRepository.findAlbumIdsByMetadataId(randomMetadata.getId())
                resp["metadata"] = randomMetadata
                resp["shortPlaceName"] = TextUtils.formatPlaceNameForHeader(randomMetadata.getPlaceName())
                resp["status"] = ApiResponse.SUCCESS.status
                resp["msg"] = ""
                logger.log(Level.INFO, "Random image metadata ID ${randomMetadata.getId()}")
            }
        }

        return mapper.writeValueAsString(resp)
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/api/v1/random/image", "/random/image"], method = [(RequestMethod.GET)], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Throws(java.io.IOException::class)
    fun getRandomImage(model: Model, request: HttpServletRequest): String {
        val mapper = ObjectMapper()
        val resp = mutableMapOf<String, Any?>()
        val currentUser = model.getAttribute("currentUser") as User?
        resp["metadata"] = mutableMapOf<String, Any?>()
        resp["status"] = ApiResponse.FAIL.status
        resp["albumIds"] = mutableListOf<Int>()
        resp["msg"] = ""

        var baseUrlBuilder = ServletUriComponentsBuilder.fromRequestUri(request).replacePath(null)
        if (request.scheme == "https") {
            baseUrlBuilder = baseUrlBuilder.scheme("https")
        }
        val baseUrl = baseUrlBuilder.build().toUriString()
        resp["baseUrl"] = baseUrl

        if (currentUser != null) {
            val randomMetadata = (if (currentUser.getAuthority()!! == "ROLE_ADMIN" || currentUser.getAuthority()!! == "ROLE_SUPER") {
                metadataRepository.findRandomMetadataMedia("image")
            } else {
                metadataRepository.findRandomAlbumMediaByUser(currentUser.getId(), "image")
            })

            if (randomMetadata != null) {
                resp["albumIds"] = albumRepository.findAlbumIdsByMetadataId(randomMetadata.getId())
                resp["metadata"] = randomMetadata
                resp["shortPlaceName"] = TextUtils.formatPlaceNameForHeader(randomMetadata.getPlaceName())
                resp["status"] = ApiResponse.SUCCESS.status
                resp["msg"] = ""
                logger.log(Level.INFO, "Random image metadata ID ${randomMetadata.getId()}")
            }
        }

        return mapper.writeValueAsString(resp)
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/api/v1/random/video"], method = [(RequestMethod.GET)], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Throws(java.io.IOException::class)
    fun getRandomVideo(model: Model, request: HttpServletRequest): String {
        val mapper = ObjectMapper()
        val resp = mutableMapOf<String, Any?>()
        val currentUser = model.getAttribute("currentUser") as User?
        resp["metadata"] = mutableMapOf<String, Any?>()
        resp["status"] = ApiResponse.FAIL.status
        resp["msg"] = ""

        var baseUrlBuilder = ServletUriComponentsBuilder.fromRequestUri(request).replacePath(null)
        if (request.scheme == "https") {
            baseUrlBuilder = baseUrlBuilder.scheme("https")
        }
        val baseUrl = baseUrlBuilder.build().toUriString()
        resp["baseUrl"] = baseUrl

        if (currentUser != null) {
            val randomMetadata = (if (currentUser.getAuthority()!! == "ROLE_ADMIN" || currentUser.getAuthority()!! == "ROLE_SUPER") {
                metadataRepository.findRandomMetadataMedia("video")
            } else {
                metadataRepository.findRandomAlbumMediaByUser(currentUser.getId(), "video")
            })

            if (randomMetadata != null) {
                resp["metadata"] = randomMetadata
                resp["shortPlaceName"] = TextUtils.formatPlaceNameForHeader(randomMetadata.getPlaceName())
                resp["status"] = ApiResponse.SUCCESS.status
                resp["msg"] = ""
                logger.log(Level.INFO, "Random image metadata ID ${randomMetadata.getId()}")
            }
        }

        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/api/v1/image/{metadataId}","/api/v1/image/{metadataId}.jpg"], method = [RequestMethod.GET], produces = ["image/apng","image/avif","image/gif","image/jpeg","image/png","image/svg+xml","image/svg+xml","image/webp"])
    @ResponseBody
    @Throws(java.io.IOException::class)
    fun getImage(response: HttpServletResponse?, request: HttpServletRequest?, @PathVariable metadataId: String): ResponseEntity<FileSystemResource> {
        return getImageFactory(request, response, metadataId)
    }

    @RequestMapping(value = ["/api/v1/image/{metadataId}/download"], method = [RequestMethod.GET], produces = ["image/apng","image/avif","image/gif","image/jpeg","image/png","image/svg+xml","image/svg+xml","image/webp"])
    @ResponseBody
    @Throws(java.io.IOException::class)
    fun getImageDownload(response: HttpServletResponse?,request: HttpServletRequest?, @PathVariable metadataId: String): ResponseEntity<FileSystemResource> {
        return getImageFactory(request, response, metadataId, true)
    }

    private fun getImageFactory(request: HttpServletRequest?, response: HttpServletResponse?, metadataId: String?, attachFile: Boolean = false): ResponseEntity<FileSystemResource> {
        val metadataObj = metadataRepository.findById(metadataId!!)

        if (metadataObj.isPresent && !metadataObj.get().getType().isNullOrBlank() && metadataObj.get().getType()?.contains("image")!!) {
            // Updated viewed date
            val metadata = metadataObj.get()
            metadata.setLastAccessedAt(getCurrentTimestamp())

            val currentUserObj = request?.session?.getAttribute("CurrentUser") as User?
            if (currentUserObj != null && currentUserObj.getId() > 0) {
                metadata.setLastAccessedBy(currentUserObj.getId())
            }
            metadataRepository.save(metadata)

            val path = metadataObj.get().getPath()!!
            var resource = FileSystemResource(path)
            val headers = HttpHeaders()
            try {
                headers.contentLength = resource.contentLength()
                if (metadataObj.get().getType() != null && "/" in metadataObj.get().getType()!!) {
                    val typeList = metadataObj.get().getType()!!.split("/")
                    if (typeList.count() == 2) {
                        headers.contentType = MediaType(typeList[0], typeList[1])
                    }
                }
                if (attachFile) {
                    // Sanitize filename
                    val filename = resource.filename.replace(validFileNameRegex, "_")
                    response?.setHeader("Content-Disposition", "attachment; filename=$filename")
                }
                headers.setCacheControl(CacheControl.maxAge(24, TimeUnit.HOURS))
                return ResponseEntity<FileSystemResource>(resource, headers, HttpStatus.OK)
            } catch (e: Exception) {
                logger.log(
                    Level.SEVERE,
                    "Error setting image ResponseEntity for "+path+": " + e.message
                )

                val source = URLDataSource(this.javaClass.getResource("/static/images/fnf.png"))
                resource = FileSystemResource(source.url.path)
                headers.contentLength = resource.contentLength()
                if (metadataObj.get().getType() != null && "/" in metadataObj.get().getType()!!) {
                    val typeList = metadataObj.get().getType()!!.split("/")
                    if (typeList.count() == 2) {
                        headers.contentType = MediaType(typeList[0], typeList[1])
                    }
                }
                if (attachFile) {
                    val filename = resource.filename.replace(validFileNameRegex, "_")
                    response?.setHeader("Content-Disposition", "attachment; filename=$filename")
                }
                headers.setCacheControl(CacheControl.maxAge(24, TimeUnit.HOURS))
                return ResponseEntity<FileSystemResource>(resource, headers, HttpStatus.OK)
            }
        } else {
            Thread {
                val admins = userRepository.findAllAdmins()
                val userIp = TextUtils.getClientIp(request)
                if (userIp !== null && !TextUtils.isLocalIp(userIp)) {
                    val notificationObjList = mutableListOf<Notification>()
                    val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                    sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
                    for (admin in admins) {
                        val notificationObj = Notification()
                        notificationObj.setUserId(admin.getId())
                        notificationObj.setCreatedAt(getCurrentTimestamp())
                        notificationObj.setModifiedAt(getCurrentTimestamp())
                        notificationObj.setRead(false)
                        val message =
                            "IP <a href='https://ipgeolocation.io/ip-location/$userIp' target='_blank'>$userIp</a> tried to view invalid image with metadata ID $metadataId at ${
                                sdtf.format(Date())
                            }"
                        notificationObj.setMessage(message)
                        notificationObjList.add(notificationObj)
                    }

                    if (notificationObjList.isNotEmpty()) {
                        notificationRepository.saveAll(notificationObjList)
                    }
                }
            }.start()

            return ResponseEntity<FileSystemResource>(null, null, HttpStatus.NOT_FOUND)
        }
    }
}