package com.miyagi.shashin.controller

import com.miyagi.shashin.ShashinApplication
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.util.FileUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.FileSystemResource
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import ws.schild.jave.Encoder
import ws.schild.jave.MultimediaObject
import ws.schild.jave.encode.AudioAttributes
import ws.schild.jave.encode.EncodingAttributes
import ws.schild.jave.encode.VideoAttributes
import ws.schild.jave.encode.enums.X264_PROFILE
import ws.schild.jave.info.VideoSize
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import javax.servlet.http.HttpServletResponse


@Controller
class MediaServiceController {

    @Autowired
    private lateinit var metadataRepository: MetadataRepository

    @Throws(IOException::class)
    private fun copyInputStreamToFile(inputStream: InputStream, file: File) {

        // append = false
        FileOutputStream(file, false).use { outputStream ->
            var read: Int
            val bytes = ByteArray(DEFAULT_BUFFER_SIZE)
            while (inputStream.read(bytes).also { read = it } != -1) {
                outputStream.write(bytes, 0, read)
            }
        }
    }

    @RequestMapping(value = ["/api/v1/video/{metadataId}"], method = [RequestMethod.GET], produces = ["video/mp4","video/3gpp","video/mpeg","video/ogg","video/quicktime","video/webm"])
    @ResponseBody
    fun getVideo(response: HttpServletResponse?, @PathVariable metadataId: String): FileSystemResource? {
        val metadataObj = metadataRepository.findById(metadataId)
        var path = metadataObj.get().getPath()!!

        if (metadataObj.get().getCompressionType()!!.lowercase() != "h.264") {
            /* Step 1. Declaring source file and Target file */
            val source = File(path)

            val tempFilePath = System.getProperty("java.io.tmpdir")+"/temp.mp4"
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
            video.setX264Profile(X264_PROFILE.BASELINE)
            // More the frames more quality and size, but keep it low based on devices like mobile
            video.setBitRate(350000)
            video.setFrameRate(20)
//            video.setSize(VideoSize(400, 300))

            /* Step 4. Set Encoding Attributes*/
            val attrs = EncodingAttributes()
            attrs.setOutputFormat("mp4")
            attrs.setAudioAttributes(audio)
            attrs.setVideoAttributes(video)

            /* Step 5. Do the Encoding*/
            try {
                val encoder = Encoder()
                encoder.encode(MultimediaObject(source), target, attrs)
                path = target.path
            } catch (e: Exception) {
                /*Handle here the video failure*/
                e.printStackTrace()
            }
        }
        return FileSystemResource(path)
    }

    @RequestMapping(value = ["/api/v1/image/{metadataId}"], method = [RequestMethod.GET], produces = ["image/apng","image/avif","image/gif","image/jpeg","image/png","image/svg+xml","image/svg+xml","image/webp"])
    @ResponseBody
    fun getImage(response: HttpServletResponse?, @PathVariable metadataId: String): FileSystemResource? {
        val metadataObj = metadataRepository.findById(metadataId)
        val path = metadataObj.get().getPath()!!
        return FileSystemResource(path)
    }

    @RequestMapping(value = ["/api/v1/audio/{metadataId}"], method = [RequestMethod.GET], produces = ["audio/3gpp","audio/aac","audio/flac","audio/mpeg","audio/mp3","audio/mp4","audio/ogg","audio/wav","audio/webm"])
    @ResponseBody
    fun getAudio(response: HttpServletResponse?, @PathVariable metadataId: String): FileSystemResource? {
        val metadataObj = metadataRepository.findById(metadataId)
        val path = metadataObj.get().getPath()!!
        return FileSystemResource(path)
    }
}