package com.miyagi.shashin.controller

import com.drew.imaging.ImageMetadataReader
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.Notification
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.AlbumRepository
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.repository.NotificationRepository
import com.miyagi.shashin.repository.SlideshowAlbumRepository
import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.util.*
import com.miyagi.shashin.util.TextUtils.Companion.getCacheControl
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
import net.coobird.thumbnailator.Thumbnails
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.awt.image.BufferedImage
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.text.split

@CrossOrigin(origins = ["*"], originPatterns = [], allowedHeaders = ["*"], methods = [RequestMethod.GET], maxAge = 3600)
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

    @Autowired
    private lateinit var slideshowAlbumRepository: SlideshowAlbumRepository

    @Value("\${app.sidecar.path}")
    private var relativeSidecarDir: String? = null

    private var logger: Logger = Logger.getLogger(MediaServiceController::class.simpleName)

    private var validFileNameRegex = "[^a-zA-Z0-9.-]".toRegex()

    @RequestMapping(value = ["/api/v1/thumbnails/{type}/{metadataId}"], method = [RequestMethod.GET])
    fun getThumbnail(model: Model, request: HttpServletRequest, @PathVariable type: String, @PathVariable metadataId: String): ResponseEntity<FileSystemResource> {
        val metadataObj = metadataRepository.findById(metadataId)

        if (metadataObj.isPresent && (type == "original" || type == "225" || type == "112" || type == "centered" || type == "map" || type == "gif")) {
            // Updated viewed date
            val metadata = metadataObj.get()

            val path = if (type == "225") {
                metadata.getThumbnailPathSmall().toString()
            } else if (type == "gif") {
                val gif = metadata.getThumbnailPathSmall().toString().replace("_225.jpg", "_225.gif")
                if (File(gif).exists()) {
                    metadata.getThumbnailPathSmall().toString().replace("_225.jpg", "_225.gif")
                } else {
                    metadata.getThumbnailPathSmall().toString()
                }
            } else if (type == "112") {
                metadata.getThumbnailPathExtraSmall().toString()
            } else if (type == "centered") {
                metadata.getThumbnailPathCentered().toString()
            } else if (type == "original") {
                val small = metadata.getThumbnailPathSmall().toString()
                val smallFile = File(small)

                val original = small.replace("_225.jpg", "_original.jpg")
                val gifOriginal = small.replace("_225.gif", "_original.gif")
                if (smallFile.extension == "jpg" && File(original).exists()) {
                    metadata.getThumbnailPathSmall().toString().replace("_225.jpg", "_original.jpg")
                } else if (smallFile.extension == "gif" && (File(gifOriginal).exists())) {
                    metadata.getThumbnailPathSmall().toString().replace("_225.gif", "_original.gif")
                } else {
                    metadata.getPath().toString()
                }
            } else {
                metadata.getMapMarkerPath().toString()
            }

            val headers = HttpHeaders()

            if (File(path).exists()) {
                var resource = FileSystemResource(path)

                try {
                    headers.contentLength = resource.contentLength()

                    val metadataType = if (type == "225" || type == "112" || type == "centered" || type == "map") {
                        "image/jpg"
                    } else if (type == "gif") {
                        "image/gif"
                    } else {
                        metadataObj.get().getType().toString()
                    }

                    if ("/" in metadataType) {
                        val metadataTypeList = metadataType.split("/")
                        if (metadataTypeList.count() == 2) {
                            headers.contentType = MediaType(metadataTypeList[0], metadataTypeList[1])
                        }
                    }
                    headers.setCacheControl(CacheControl.maxAge(24, TimeUnit.HOURS))
                    return ResponseEntity<FileSystemResource>(resource, headers, HttpStatus.OK)
                } catch (e: Exception) {
                    logger.log(
                        Level.SEVERE,
                        "Error setting image ResponseEntity for " + path + ": " + e.message
                    )

                    val source = URLDataSource(this.javaClass.getResource("/static/images/fnf.png"))
                    resource = FileSystemResource(source.url.path)
                    headers.contentLength = resource.contentLength()
                    headers.contentType = MediaType("image", "png")
                    headers.setCacheControl(CacheControl.maxAge(24, TimeUnit.HOURS))
                    return ResponseEntity<FileSystemResource>(resource, headers, HttpStatus.OK)
                }
            } else {
                logger.log(
                    Level.SEVERE,
                    "Error setting image ResponseEntity for $path"
                )

                val source = URLDataSource(this.javaClass.getResource("/static/images/fnf.png"))
                val resource = FileSystemResource(source.url.path)
                headers.contentLength = resource.contentLength()
                headers.contentType = MediaType("image", "png")
                headers.setCacheControl(CacheControl.maxAge(24, TimeUnit.HOURS))
                return ResponseEntity<FileSystemResource>(resource, headers, HttpStatus.OK)
            }
        } else {
            Thread {
                val admins = userRepository.findAllAdmins()
                val userIp = model.getAttribute("clientIP").toString()
                if (!TextUtils.isLocalIp(userIp)) {
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

    @RequestMapping(value = ["/api/v1/video/{metadataId}"], method = [RequestMethod.GET], produces = ["video/mp4","video/3gpp","video/mpeg","video/ogg","video/quicktime","video/webm"])
    @ResponseBody
    @Throws(IOException::class)
    fun getVideo(model: Model, response: HttpServletResponse, request: HttpServletRequest, @PathVariable metadataId: String): ResponseEntity<FileSystemResource> {
        return processVideo(model, metadataId, request, response)
    }

    private fun processVideo(model: Model, metadataId: String?, request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<FileSystemResource> {
        val metadataObj = metadataRepository.findById(metadataId!!)

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

            val userIp = model.getAttribute("clientIP").toString()
            if (!TextUtils.isLocalIp(userIp)) {
                val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
                logger.log(Level.INFO, "IP $userIp played video ${metadataObj.get().getTitle()}' at ${sdtf.format(Date())}")
            }

            return getVideoFactory(model, request, response, metadata, path)
        } else {
            Thread {
                val admins = userRepository.findAllAdmins()
                val userIp = model.getAttribute("clientIP").toString()
                if (!TextUtils.isLocalIp(userIp)) {
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
    @Throws(IOException::class)
    fun getVideoDownload(model: Model, response: HttpServletResponse, request: HttpServletRequest, @PathVariable metadataId: String): ResponseEntity<FileSystemResource>? {
        val metadataObj = metadataRepository.findById(metadataId)

        if (metadataObj.isPresent && !metadataObj.get().getType().isNullOrBlank() && metadataObj.get().getType()?.contains("video")!!) {
            val userIp = model.getAttribute("clientIP").toString()
            if (!TextUtils.isLocalIp(userIp)) {
                val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
                logger.log(Level.INFO, "IP $userIp downloaded video ${metadataObj.get().getTitle()}' at ${sdtf.format(Date())}")
            }

            return getVideoFactory(model, request, response, metadataObj.get(), metadataObj.get().getPath()!!, true)
        } else {
            Thread {
                val admins = userRepository.findAllAdmins()
                val userIp = model.getAttribute("clientIP").toString()
                if (!TextUtils.isLocalIp(userIp)) {
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

    private fun getVideoFactory(model: Model, request: HttpServletRequest, response: HttpServletResponse, metadataObj: Metadata, path: String, attachFile: Boolean = false): ResponseEntity<FileSystemResource> {
        metadataObj.setLastAccessedAt(getCurrentTimestamp())

        val currentUserObj = request.session?.getAttribute("CurrentUser") as User?
        if (currentUserObj != null && currentUserObj.getId() > 0) {
            metadataObj.setLastAccessedBy(currentUserObj.getId())
        } else {
            metadataObj.setLastAccessedBy(0)
        }
        metadataObj.setFreeFormString(TextUtils.getMetadataFreeformString(model))
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
                response.setHeader("Content-Disposition", "attachment; filename=$filename")
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
    fun getVideoPlayer(model: Model, request: HttpServletRequest, @PathVariable metadataId: String): String {
        return setModel(metadataId, model, request, "player")
    }

    @RequestMapping(value = ["/image/{metadataId}/viewer"], method = [RequestMethod.GET])
    fun getImageViewer(model: Model, request: HttpServletRequest, @PathVariable metadataId: String): String {
        return setModel(metadataId, model, request, "viewer")
    }

    private fun setModel(metadataId: String, model: Model, request: HttpServletRequest, module: String): String {
        model["metadataObj"] = Metadata()
        val metadataObj = metadataRepository.findById(metadataId)
        val metadata = metadataObj.get()

        // Updated viewed date
        if (metadataObj.isPresent) {
            model["metadataObj"] = metadata
            metadata.setLastAccessedAt(getCurrentTimestamp())

            val currentUserObj = request.session?.getAttribute("CurrentUser") as User?
            if (currentUserObj != null && currentUserObj.getId() > 0) {
                metadata.setLastAccessedBy(currentUserObj.getId())
            } else {
                metadata.setLastAccessedBy(0)
            }
            metadata.setFreeFormString(TextUtils.getMetadataFreeformString(model))
            metadataRepository.save(metadata)
        }

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)

        return module
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/api/v1/media/metadata/{metadataId}", "/media/metadata/{metadataId}"], method = [(RequestMethod.GET)], produces = ["application/json"])
    @ResponseBody
    @Throws(IOException::class)
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
                Thread {
                    metadata.setLastAccessedAt(getCurrentTimestamp())
                    metadata.setLastAccessedBy(currentUser.getId())
                    metadata.setFreeFormString(TextUtils.getMetadataFreeformString(model))
                    metadataRepository.save(metadata)
                }.start()

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
    @RequestMapping(value = ["/api/v1/random/metadata/filename/{filename}", "/random/metadata/filename/{filename}"], method = [(RequestMethod.GET)], produces = ["application/json"])
    @ResponseBody
    @Throws(IOException::class)
    fun getRandomImageByFilename(model: Model, request: HttpServletRequest, @PathVariable filename: String): String {
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
                metadataRepository.findRandomMetadataMediaAndFilter(filename)
            } else {
                metadataRepository.findRandomAlbumMediaAndFilterByUser(currentUser.getId(), filename)
            })

            if (randomMetadata != null) {
                Thread {
                    randomMetadata.setLastAccessedAt(getCurrentTimestamp())
                    randomMetadata.setLastAccessedBy(currentUser.getId())
                    randomMetadata.setFreeFormString(TextUtils.getMetadataFreeformString(model))
                    metadataRepository.save(randomMetadata)
                }.start()

                resp["albumIds"] = albumRepository.findAlbumIdsByMetadataId(randomMetadata.getId())
                resp["metadata"] = randomMetadata
                resp["shortPlaceName"] = TextUtils.formatPlaceNameForHeader(randomMetadata.getPlaceName())
                resp["status"] = ApiResponse.SUCCESS.status
                resp["msg"] = ""
                logger.log(Level.INFO, "Random media metadata ID ${randomMetadata.getId()}")
            }
        }

        return mapper.writeValueAsString(resp)
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/api/v1/random/metadata/type/{type}", "/random/metadata/type/{type}"], method = [(RequestMethod.GET)], produces = ["application/json"])
    @ResponseBody
    @Throws(IOException::class)
    fun getRandomImageByType(model: Model, request: HttpServletRequest, @PathVariable type: String, @RequestParam slideAlbumsOnly: Optional<Boolean>): String {
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
            var slideshowAlbums: Array<String> = arrayOf("all")
            if ((slideAlbumsOnly.isPresent && slideAlbumsOnly.get())) {
                val slideshowObj = slideshowAlbumRepository.findFirstByUserId(currentUser.getId())
                println("testzzz")
                println(slideshowObj.toString())
                if (slideshowObj != null) {
                    slideshowAlbums = slideshowObj.getAlbums()!!.split(",").toTypedArray()
                    //slideshowAlbums = slideshowAlbums.filter { it != "all" }.toTypedArray()
                }
            }

            val randomMetadata =
                (if (slideshowAlbums.contains("all") && (currentUser.getAuthority()!! == "ROLE_ADMIN" || currentUser.getAuthority()!! == "ROLE_SUPER")) {
                    metadataRepository.findRandomMetadataMedia(type)
                } else if (slideshowAlbums.contains("all") && currentUser.getAuthority()!! == "ROLE_USER") {
                    metadataRepository.findRandomAlbumMediaByUser(currentUser.getId(), type)
                } else if (slideshowAlbums.isNotEmpty()) {
                    println("testzzz1")
                    val randomIndex = (0..slideshowAlbums.lastIndex).random()
                    val albumId = slideshowAlbums[randomIndex]
                    println(albumId)
                    metadataRepository.findRandomAlbumMediaByUserAndAlbum(currentUser.getId(), albumId.toInt(), type)
                } else {
                    null
                })

            if (randomMetadata != null) {
                Thread {
                    randomMetadata.setLastAccessedAt(getCurrentTimestamp())
                    randomMetadata.setLastAccessedBy(currentUser.getId())
                    randomMetadata.setFreeFormString(TextUtils.getMetadataFreeformString(model))
                    metadataRepository.save(randomMetadata)
                }.start()

                resp["albumIds"] = albumRepository.findAlbumIdsByMetadataId(randomMetadata.getId())
                resp["metadata"] = randomMetadata
                resp["shortPlaceName"] = TextUtils.formatPlaceNameForHeader(randomMetadata.getPlaceName())
                resp["status"] = ApiResponse.SUCCESS.status
                resp["msg"] = ""
                logger.log(Level.INFO, "Random media metadata ID ${randomMetadata.getId()}")
            } else {
                logger.log(Level.INFO, "Album photos not detected")
            }
        }

        return mapper.writeValueAsString(resp)
    }

    private fun getRandomImageBy(type: String, model: Model, request: HttpServletRequest, filter: String, height: Optional<Int>, width: Optional<Int>, orientationImage: Optional<Int>, albumsOnly: Optional<Boolean>, ttl: Optional<String>): ResponseEntity<FileSystemResource> {
        val currentUser = model.getAttribute("currentUser") as User?

        if (currentUser != null) {
            val orientation = orientationImage.orElse(2)

            val randomMetadata: Metadata? =
                if (orientation == 0) { // landscape
                    if (type == "filename") {
                        (if (!(albumsOnly.isPresent && albumsOnly.get()) && (currentUser.getAuthority()!! == "ROLE_ADMIN" || currentUser.getAuthority()!! == "ROLE_SUPER")) {
                            metadataRepository.findRandomMetadataMediaAndFilterLandscape(filter)
                        } else if (!albumsOnly.isPresent || (albumsOnly.isPresent && albumsOnly.get())) {
                            metadataRepository.findRandomAlbumMediaAndFilterByUserLandscape(currentUser.getId(), filter)
                        } else {
                            null
                        })
                    } else {
                        (if (!(albumsOnly.isPresent && albumsOnly.get()) && (currentUser.getAuthority()!! == "ROLE_ADMIN" || currentUser.getAuthority()!! == "ROLE_SUPER")) {
                            metadataRepository.findRandomMetadataMediaLandscape(filter)
                        } else if (!albumsOnly.isPresent || (albumsOnly.isPresent && albumsOnly.get())) {
                            metadataRepository.findRandomAlbumMediaByUserLandscape(currentUser.getId(), filter)
                        } else {
                            null
                        })
                    }
                } else if (orientation == 1) { // portrait
                    if (type == "filename") {
                        (if (!(albumsOnly.isPresent && albumsOnly.get()) && (currentUser.getAuthority()!! == "ROLE_ADMIN" || currentUser.getAuthority()!! == "ROLE_SUPER")) {
                            metadataRepository.findRandomMetadataMediaAndFilterPortrait(filter)
                        } else if (!albumsOnly.isPresent || (albumsOnly.isPresent && albumsOnly.get())) {
                            metadataRepository.findRandomAlbumMediaAndFilterByUserPortrait(currentUser.getId(), filter)
                        } else {
                            null
                        })
                    } else {
                        (if (!(albumsOnly.isPresent && albumsOnly.get()) && (currentUser.getAuthority()!! == "ROLE_ADMIN" || currentUser.getAuthority()!! == "ROLE_SUPER")) {
                            metadataRepository.findRandomMetadataMediaPortrait(filter)
                        } else if (!albumsOnly.isPresent || (albumsOnly.isPresent && albumsOnly.get())) {
                            metadataRepository.findRandomAlbumMediaByUserPortrait(currentUser.getId(), filter)
                        } else {
                            null
                        })
                    }
                } else {
                    if (type == "filename") {
                        (if (!(albumsOnly.isPresent && albumsOnly.get()) && (currentUser.getAuthority()!! == "ROLE_ADMIN" || currentUser.getAuthority()!! == "ROLE_SUPER")) {
                            metadataRepository.findRandomMetadataMediaAndFilter(filter)
                        } else if (!albumsOnly.isPresent || (albumsOnly.isPresent && albumsOnly.get())) {
                            metadataRepository.findRandomAlbumMediaAndFilterByUser(currentUser.getId(), filter)
                        } else {
                            null
                        })
                    } else {
                        (if (!(albumsOnly.isPresent && albumsOnly.get()) && (currentUser.getAuthority()!! == "ROLE_ADMIN" || currentUser.getAuthority()!! == "ROLE_SUPER")) {
                            metadataRepository.findRandomMetadataMedia(filter)
                        } else if (!albumsOnly.isPresent || (albumsOnly.isPresent && albumsOnly.get())) {
                            metadataRepository.findRandomAlbumMediaByUser(currentUser.getId(), filter)
                        } else {
                            null
                        })
                    }
                }

            if (randomMetadata != null) {
                Thread {
                    randomMetadata.setLastAccessedAt(getCurrentTimestamp())
                    randomMetadata.setLastAccessedBy(currentUser.getId())
                    randomMetadata.setFreeFormString(TextUtils.getMetadataFreeformString(model))
                    metadataRepository.save(randomMetadata)
                }.start()

                val imageHeight = height.orElse(randomMetadata.getOriginalImageHeight())
                val imageWidth = width.orElse(randomMetadata.getOriginalImageWidth())

                var resource: FileSystemResource?
                val path: String?

                if (imageHeight == randomMetadata.getOriginalImageHeight() && imageWidth == randomMetadata.getOriginalImageWidth()) {
                    resource = FileSystemResource(File(randomMetadata.getPath()!!))
                    path = randomMetadata.getPath()!!
                } else {
                    val outputExtension = "jpg"

                    val file = File(randomMetadata.getPath()!!)

                    var rotation = 0
                    val fileMetadata: com.drew.metadata.Metadata
                    try {
                        fileMetadata = ImageMetadataReader.readMetadata(file)

                        for (directory in fileMetadata.directories) {
                            for (tag in directory.tags) {
                                when (tag.tagName) {
                                    "Orientation", "Rotation" -> {
                                        if (tag.description.contains("Rotate")) {
                                            val digit = tag.description.filter { it.isDigit() }
                                            if (TextUtils.isInteger(digit)) {
                                                rotation = digit.toInt()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        logger.log(
                            Level.SEVERE,
                            "Error reading Metadata for " + file.name + ": " + e.message
                        )
                    }

                    val img = ImageIO.read(file)

//                    if (rotation != 0) {
//                        img = ImageProcessing.rotateImage(img, rotation.toDouble())
//                        logger.log(
//                            Level.INFO,
//                            "Rotation applied: $rotation"
//                        )
//                    }

                    val thumbnails = Thumbnails.of(img).outputQuality(1.0)

                    val mediaExtension = FileUtils.probeFileExtension(file)

                    if (mediaExtension == "gif") {
                        thumbnails
                            .imageType(BufferedImage.TYPE_INT_ARGB)
                    }

                    val tempFile = File(System.getProperty("java.io.tmpdir") + "/temp." + outputExtension)

                    if (imageHeight <= randomMetadata.getOriginalImageHeight()!!) {
                        thumbnails.height(imageHeight)
                    } else {
                        thumbnails.height(randomMetadata.getOriginalImageHeight()!!)
                    }

                    if (imageWidth <= randomMetadata.getOriginalImageWidth()!!) {
                        thumbnails.width(imageWidth)
                    } else {
                        thumbnails.width(randomMetadata.getOriginalImageWidth()!!)
                    }

                    thumbnails.toFile(tempFile)

                    try {
                        ImageIO.write(ImageIO.read(tempFile), outputExtension, tempFile)
                    } catch (_: IOException) {
                        logger.log(Level.WARNING, "Could not read file: " + tempFile.path)
                    }

                    resource = FileSystemResource(tempFile)
                    path = tempFile.path
                }

                val headers = HttpHeaders()
                try {
                    headers.contentLength = resource.contentLength()
                    if (randomMetadata.getType() != null && "/" in randomMetadata.getType()!!) {
                        val typeList = randomMetadata.getType()!!.split("/")
                        if (typeList.count() == 2) {
                            headers.contentType = MediaType(typeList[0], typeList[1])
                        }
                    }

                    val cacheTime = ttl.orElse("none")
                    headers.setCacheControl(getCacheControl(cacheTime))

                    return ResponseEntity<FileSystemResource>(resource, headers, HttpStatus.OK)
                } catch (e: Exception) {
                    logger.log(
                        Level.SEVERE,
                        "Error setting image ResponseEntity for "+path+": " + e.message
                    )

                    val source = URLDataSource(this.javaClass.getResource("/static/images/fnf.png"))
                    resource = FileSystemResource(source.url.path)
                    headers.contentLength = resource.contentLength()
                    headers.contentType = MediaType("image", "png")
                    headers.setCacheControl(CacheControl.noCache())
                    return ResponseEntity<FileSystemResource>(resource, headers, HttpStatus.OK)
                }
            }
        }

        logger.log(
            Level.SEVERE,
            "Error setting image ResponseEntity for random image"
        )

        val source = URLDataSource(this.javaClass.getResource("/static/images/fnf.png"))
        val resource = FileSystemResource(source.url.path)
        val headers = HttpHeaders()
        headers.contentLength = resource.contentLength()
        headers.contentType = MediaType("image", "png")

        headers.setCacheControl(CacheControl.noCache())
        return ResponseEntity<FileSystemResource>(resource, headers, HttpStatus.OK)
    }

    @RequestMapping(value = ["/api/v1/random/image/filename/{filename}", "/random/image/filename/{filename}"], method = [(RequestMethod.GET)], produces = ["image/apng","image/avif","image/gif","image/jpeg","image/png","image/svg+xml","image/svg+xml","image/webp"])
    @ResponseBody
    @Throws(IOException::class)
    fun getRandomImageByDimensionAndFilename(model: Model, request: HttpServletRequest, @PathVariable filename: String, @RequestParam height: Optional<Int>, @RequestParam width: Optional<Int>, @RequestParam orientation: Optional<Int>, @RequestParam albumsOnly: Optional<Boolean>, @RequestParam ttl: Optional<String>): ResponseEntity<FileSystemResource> {
        return getRandomImageBy("filename", model, request, filename, height, width, orientation, albumsOnly, ttl)
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/api/v1/random/image/type/{type}", "/random/image/type/{type}"], method = [(RequestMethod.GET)], produces = ["image/apng","image/avif","image/gif","image/jpeg","image/png","image/svg+xml","image/svg+xml","image/webp"])
    @ResponseBody
    @Throws(IOException::class)
    fun getRandomImageByDimensionAndType(model: Model, request: HttpServletRequest, @PathVariable type: String, @RequestParam height: Optional<Int>, @RequestParam width: Optional<Int>, @RequestParam orientation: Optional<Int>, @RequestParam albumsOnly: Optional<Boolean>, @RequestParam ttl: Optional<String>): ResponseEntity<FileSystemResource> {
        return getRandomImageBy("type", model, request, type, height, width, orientation, albumsOnly, ttl)
    }

    @RequestMapping(value = ["/api/v1/image/{metadataId}","/api/v1/image/{metadataId}.jpg"], method = [RequestMethod.GET], produces = ["image/apng","image/avif","image/gif","image/jpeg","image/png","image/svg+xml","image/svg+xml","image/webp"])
    @ResponseBody
    @Throws(IOException::class)
    fun getImage(model: Model, response: HttpServletResponse, request: HttpServletRequest, @PathVariable metadataId: String): ResponseEntity<FileSystemResource> {
        return getImageFactory(model, request, response, metadataId)
    }

    @RequestMapping(value = ["/api/v1/image/{metadataId}/download"], method = [RequestMethod.GET], produces = ["image/apng","image/avif","image/gif","image/jpeg","image/png","image/svg+xml","image/svg+xml","image/webp"])
    @ResponseBody
    @Throws(IOException::class)
    fun getImageDownload(model: Model, response: HttpServletResponse,request: HttpServletRequest, @PathVariable metadataId: String): ResponseEntity<FileSystemResource> {
        return getImageFactory(model, request, response, metadataId, true)
    }

    private fun getImageFactory(model: Model,request: HttpServletRequest, response: HttpServletResponse, metadataId: String?, attachFile: Boolean = false): ResponseEntity<FileSystemResource> {
        val metadataObj = metadataRepository.findById(metadataId!!)

        if (metadataObj.isPresent && !metadataObj.get().getType().isNullOrBlank() && metadataObj.get().getType()?.contains("image")!!) {
            // Updated viewed date
            val metadata = metadataObj.get()
            metadata.setLastAccessedAt(getCurrentTimestamp())

            val currentUserObj = request.session?.getAttribute("CurrentUser") as User?
            if (currentUserObj != null && currentUserObj.getId() > 0) {
                metadata.setLastAccessedBy(currentUserObj.getId())
            } else {
                metadata.setLastAccessedBy(0)
            }
            metadata.setFreeFormString(TextUtils.getMetadataFreeformString(model))
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
                val userIp = model.getAttribute("clientIP").toString()
                if (!TextUtils.isLocalIp(userIp)) {
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