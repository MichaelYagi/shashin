package com.miyagi.shashin.controller

import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.MetricsUtil
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.http.*
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
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
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import javax.activation.URLDataSource
import javax.servlet.http.HttpServletResponse


@Controller
class MediaServiceController {

    @Autowired
    private lateinit var metadataRepository: MetadataRepository

    @Value("\${app.sidecar.path}")
    private var relativeSidecarDir: String? = null

    private var logger: Logger = Logger.getLogger(MediaServiceController::class.simpleName)

    private var validFileNameRegex = "[^a-zA-Z0-9.-]".toRegex()

    @RequestMapping(value = ["/api/v1/video/{metadataId}"], method = [RequestMethod.GET], produces = ["video/mp4","video/3gpp","video/mpeg","video/ogg","video/quicktime","video/webm"])
    @ResponseBody
    @Throws(java.io.IOException::class)
    fun getVideo(response: HttpServletResponse?, @PathVariable metadataId: String): ResponseEntity<FileSystemResource> {
        val metadataObj = metadataRepository.findById(metadataId)

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
                video.setPreset(PresetEnum.SUPERFAST.presetName)
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

            return getVideoFactory(response, metadata, path)
        } else {
            return ResponseEntity<FileSystemResource>(null, null, HttpStatus.NOT_FOUND)
        }
    }

    @RequestMapping(value = ["/api/v1/video/{metadataId}/download"], method = [RequestMethod.GET], produces = ["video/mp4","video/3gpp","video/mpeg","video/ogg","video/quicktime","video/webm"])
    @ResponseBody
    @Throws(java.io.IOException::class)
    fun getVideoDownload(response: HttpServletResponse?, @PathVariable metadataId: String): ResponseEntity<FileSystemResource>? {
        val metadataObj = metadataRepository.findById(metadataId)

        if (metadataObj.isPresent && !metadataObj.get().getType().isNullOrBlank() && metadataObj.get().getType()?.contains("video")!!) {
            return getVideoFactory(response, metadataObj.get(), metadataObj.get().getPath()!!, true)
        } else {
            return ResponseEntity<FileSystemResource>(null, null, HttpStatus.NOT_FOUND)
        }
    }

    private fun getVideoFactory(response: HttpServletResponse?, metadataObj: Metadata, path: String, attachFile: Boolean = false): ResponseEntity<FileSystemResource> {
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
    fun getVideoPlayer(model: Model, @PathVariable metadataId: String): String {
        return setModel(metadataId,model,"player")
    }

    @RequestMapping(value = ["/image/{metadataId}/viewer"], method = [RequestMethod.GET])
    fun getImageViewer(model: Model, @PathVariable metadataId: String): String {
        return setModel(metadataId,model,"viewer")
    }

    private fun setModel(metadataId: String,model: Model,module: String): String {
        val metadataObj = metadataRepository.findById(metadataId)

        model["metadataObj"] = metadataObj.get()
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)

        return module
    }

    @RequestMapping(value = ["/api/v1/image/{metadataId}","/api/v1/image/{metadataId}.jpg"], method = [RequestMethod.GET], produces = ["image/apng","image/avif","image/gif","image/jpeg","image/png","image/svg+xml","image/svg+xml","image/webp"])
    @ResponseBody
    @Throws(java.io.IOException::class)
    fun getImage(response: HttpServletResponse?, @PathVariable metadataId: String): ResponseEntity<FileSystemResource> {
        return getImageFactory(response, metadataId)
    }

    @RequestMapping(value = ["/api/v1/image/{metadataId}/download"], method = [RequestMethod.GET], produces = ["image/apng","image/avif","image/gif","image/jpeg","image/png","image/svg+xml","image/svg+xml","image/webp"])
    @ResponseBody
    @Throws(java.io.IOException::class)
    fun getImageDownload(response: HttpServletResponse?, @PathVariable metadataId: String): ResponseEntity<FileSystemResource> {
        return getImageFactory(response, metadataId, true)
    }

    private fun getImageFactory(response: HttpServletResponse?, metadataId: String, attachFile: Boolean = false): ResponseEntity<FileSystemResource> {
        val metadataObj = metadataRepository.findById(metadataId)

        if (metadataObj.isPresent && !metadataObj.get().getType().isNullOrBlank() && metadataObj.get().getType()?.contains("image")!!) {
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
            return ResponseEntity<FileSystemResource>(null, null, HttpStatus.NOT_FOUND)
        }
    }
}