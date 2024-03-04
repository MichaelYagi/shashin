package com.miyagi.shashin.util

import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.geometry.Positions
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.stream.FileImageOutputStream
import javax.imageio.stream.ImageOutputStream
class VideoProcessing(private val videoFile: File) {

    private val frameGrabber: FFmpegFrameGrabber = FFmpegFrameGrabber(videoFile.path)

    private var logger: Logger = Logger.getLogger(VideoProcessing::class.simpleName)

    fun getVideoScreenshot(): BufferedImage? {
        var bi: BufferedImage? = null
        try {
            frameGrabber.start()

            val aa = Java2DFrameConverter()

            val f = frameGrabber.grabImage()
            bi = aa.convert(f)

            val rotationStr = frameGrabber.getVideoMetadata("rotate")
            if (!rotationStr.isNullOrBlank() && bi != null) {
                val rotation = rotationStr.toDouble()
                if (rotation > 0) {
                    bi = ImageProcessing.rotateImage(bi, rotation)
                }
            }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Could not capture screenshot for video ${videoFile.name}: ${e.message}")
        }

        frameGrabber.stop()

        return bi
    }

    fun getVideoGifFile(): File? {
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
            val totalFrameCount = frameGrabber.lengthInFrames
            // Skip every x frames
            val skipFrame = 3
            val limit = 25*skipFrame

            for (frameCount in 0 until totalFrameCount) {
                if (frameCount > limit) {
                    break
                }

                val imageGrabber = frameGrabber.grabImage()

                if (frameCount % skipFrame == 0) {
                    bi = frameConverter.convert(imageGrabber)

                    if (bi != null) {
                        val rotationStr = frameGrabber.getVideoMetadata("rotate")
                        if (!rotationStr.isNullOrBlank()) {
                            val rotation = rotationStr.toDouble()
                            if (rotation > 0) {
                                bi = ImageProcessing.rotateImage(bi, rotation)
                            }
                        }

                        val thumbnails = Thumbnails.of(bi)
                            .outputQuality(1.0)

                        if (bi.width > bi.height * 2) {
                            thumbnails
                                .crop(Positions.CENTER)
                                .size(FileUtils.thumbnailHeight(), FileUtils.thumbnailHeight())
                        } else {
                            thumbnails
                                .height(FileUtils.thumbnailHeight())
                        }

                        writer.writeToSequence(thumbnails.asBufferedImage())
                    }

                    logger.log(Level.INFO, "Current frame ${frameGrabber.frameNumber} of $totalFrameCount with iteration $frameCount and limit $limit")
                }
            }

            writer.close()
            output.close()
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Could not capture screenshot for video ${videoFile.name}: ${e.message}")
            return null
        }

        metricsUtil.end()

        frameGrabber.stop()

        return File(tempGifFilePath)
    }
}