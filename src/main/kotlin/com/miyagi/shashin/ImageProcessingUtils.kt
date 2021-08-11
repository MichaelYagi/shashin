package com.miyagi.shashin

import com.drew.imaging.ImageMetadataReader
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO

class ImageProcessingUtils {

    companion object {
        private var logger: Logger = Logger.getLogger(ImageProcessingUtils::class.simpleName)

        fun createSidecarData(file: File, sidecarDir: String, rootDir: String) {
            val metadata = ImageMetadataReader.readMetadata(file)
            for (directory in metadata.directories) {
                for (tag in directory.tags) {
                    // TODO: Get this info into exif file
                    //println(tag)
                }
            }
            // TODO: Save data to yaml/exif/sidecar files in configured directory
        }

        fun createThumbnails(file: File, sidecarDir: String, rootDir: String) {
            // Scale to different sizes and save
            val img: BufferedImage = ImageIO.read(file)
            val scaled: BufferedImage = scaleImageByHeight(img, 224)

            // Map path to sidecar file
            val tempFile = File(rootDir)
            var fileRootDir: String = (file.parent).lowercase().replace((tempFile.canonicalPath).lowercase(), "")
            fileRootDir = fileRootDir.replace('\\', '/')
            val tnDir = sidecarDir.dropLast(1) + "/thumbnails" + fileRootDir
            val tnFileStr = tnDir + "/" + file.name + ".jpg"

            try {
                // Create directory if dne
                val thumbnailFileDir = File(tnDir)
                if (!thumbnailFileDir.exists()) {
                    thumbnailFileDir.mkdirs()
                }
                // Create file
                val thumbnailFile = File(tnFileStr)
                if (thumbnailFile.createNewFile()) {
                    logger.log(Level.INFO, "Thumbnail created: " + thumbnailFile.name)
                } else {
                    logger.log(Level.INFO, "Thumbnail already exists: " + thumbnailFile.name)
                }
                ImageIO.write(scaled, "jpg", thumbnailFile)
            } catch (e: IOException) {
                logger.log(Level.SEVERE, "Thumbnail creation error: " + e.message)
            }
        }

        private fun scaleImageByRatio(source: BufferedImage, ratio: Double): BufferedImage {
            val w = (source.width * ratio).toInt()
            val h = (source.height * ratio).toInt()
            return scaleImage(source, w, h)
        }

        private fun scaleImageByWidth(source: BufferedImage, w: Int): BufferedImage {
            val wScale: Float = (w.toFloat() / source.width.toFloat())
            val h = source.height.toFloat() * wScale //3705*(50/6000)
            return scaleImage(source, w, h.toInt())
        }

        private fun scaleImageByHeight(source: BufferedImage, h: Int): BufferedImage {
            val hScale: Float = (h.toFloat() / source.height.toFloat())
            val w = source.width.toFloat() * hScale
            return scaleImage(source, w.toInt(), h)
        }

        private fun scaleImage(source: BufferedImage, w: Int, h: Int): BufferedImage {
            val bi = getCompatibleImage(w, h)
            val g2d = bi.createGraphics()
            val xScale = w.toDouble() / source.width
            val yScale = h.toDouble() / source.height
            val at = AffineTransform.getScaleInstance(xScale, yScale)
            g2d.drawRenderedImage(source, at)
            g2d.dispose()
            return bi
        }

        private fun getCompatibleImage(w: Int, h: Int): BufferedImage {
            return BufferedImage(
                w, h,
                BufferedImage.TYPE_INT_RGB
            )
        }
    }
}