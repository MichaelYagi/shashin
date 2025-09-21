package com.miyagi.shashin.service

import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.GifSequenceWriter
import com.miyagi.shashin.util.MetricsUtil
import com.miyagi.shashin.util.TextUtils
import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.geometry.Positions
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.stream.FileImageOutputStream
import javax.imageio.stream.ImageOutputStream
import kotlin.text.filter

class VideoProcessing(private val videoFile: File) {

    init {
        // No log output
        try {
            avutil.av_log_set_level(avutil.AV_LOG_QUIET)
        } catch (_: IOException) {}
    }

    private val frameGrabber: FFmpegFrameGrabber = FFmpegFrameGrabber(videoFile.path)

    private var logger: Logger = Logger.getLogger(VideoProcessing::class.simpleName)

    fun getVideoRotation(): Double {
        var rotation: Double = 0.0

        var rotationStr: String? = frameGrabber.getVideoMetadata("rotate")
        if (rotationStr.isNullOrBlank()) {
            rotationStr = frameGrabber.displayRotation.toString()
        }

        var rotationDouble = rotationStr.toDouble()
        var rotationInt = rotationDouble.toInt()

        val rotationFiltered = rotationInt.toString().filter { it.isDigit() }

        if (TextUtils.Companion.isInteger(rotationFiltered)) {
            rotation = rotationFiltered.toDouble()
        }

        return rotation
    }

    fun getVideoScreenshot(): BufferedImage? {
        var bi: BufferedImage? = null

        try {
            frameGrabber.start()

            val aa = Java2DFrameConverter()

            val f = frameGrabber.grabImage()
            bi = aa.convert(f)

            val rotation = getVideoRotation()

            if (rotation != 0.0 && bi != null) {
                bi = ImageProcessing.Companion.rotateImage(bi, rotation)
            }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Could not capture screenshot for video ${videoFile.name}: ${e.message}")
        }

        frameGrabber.stop()

        return bi
    }

    fun getVideoGifFile(frameLimit: Double = 0.0): File? {
        val metricsUtil = MetricsUtil()
        metricsUtil.start("converting video to gif")

        var bi: BufferedImage?

        val tempGifFilePath = System.getProperty("java.io.tmpdir") + "/temp.gif"
        if (Files.exists(Paths.get(tempGifFilePath))) {
            val tempFile = File(tempGifFilePath)
            tempFile.delete()
        }

        try {
            frameGrabber.start()

            val frameConverter = Java2DFrameConverter()
            val output: ImageOutputStream = FileImageOutputStream(File(tempGifFilePath))
            val writer = GifSequenceWriter(output, BufferedImage.TYPE_INT_ARGB, 0, true)
            val framerate = frameGrabber.frameRate
            val totalFrameCount = frameGrabber.lengthInFrames
            // Skip every x frames
            val skipFrame = framerate/12

            var limit = framerate*skipFrame
            if (frameLimit != 0.0) {
                limit = frameLimit
            }

            for (frameCount in 0 until totalFrameCount) {
                if (frameCount > limit) {
                    break
                }

                val imageGrabber = frameGrabber.grabImage()

                if ((frameCount % skipFrame).toInt() == 0) {
                    bi = frameConverter.convert(imageGrabber)

                    if (bi != null) {
                        val rotation = getVideoRotation()
                        if (rotation != 0.0) {
                            bi = ImageProcessing.Companion.rotateImage(bi, rotation)
                        }

                        val thumbnails = Thumbnails.of(bi)
                            .outputQuality(1.0)

                        if (bi.width > bi.height * 2) {
                            thumbnails
                                .crop(Positions.CENTER)
                                .size(FileUtils.Companion.thumbnailHeight(), FileUtils.Companion.thumbnailHeight())
                        } else {
                            thumbnails
                                .height(FileUtils.Companion.thumbnailHeight())
                        }

                        writer.writeToSequence(thumbnails.asBufferedImage())
                    }

                    logger.log(Level.INFO, "Current frame ${frameGrabber.frameNumber} of $totalFrameCount with iteration $frameCount and limit $limit")
                }
            }

            writer.close()
            output.close()
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Could not capture gif for video ${videoFile.name}: ${e.message}")
            return null
        }

        metricsUtil.end()

        frameGrabber.stop()

        return File(tempGifFilePath)
    }
}