package com.miyagi.shashin

import com.drew.imaging.ImageMetadataReader
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import javax.imageio.ImageIO


class ImageProcessingUtils {
    companion object {
        fun createSidecarData(file: File, sidecarDir: String) {
            val metadata = ImageMetadataReader.readMetadata(file)
            for (directory in metadata.directories) {
                for (tag in directory.tags) {
                    // TODO: Get this info into exif file
                    //println(tag)
                }
            }
            // TODO: Save data to yaml/exif/sidecar files in configured directory
        }

        fun createThumbnails(file: File, sidecarDir: String) {
            // Scale to different sizes and save
            val img: BufferedImage = ImageIO.read(file)
            val scaled: BufferedImage = scaleImageByHeight(img, 224)
            val path: Path = Paths.get(file.canonicalPath)
            val directory: String = path.parent.toString()
            val strPath = directory.replace('\\', '/')
            val uri = URI(strPath)
            val tnDir = sidecarDir.dropLast(1) + uri.path
            val tnFileStr = tnDir + "/" + file.name

            try {
                val thumbnailFileDir = File(tnDir)
                if (!thumbnailFileDir.exists()) {
                    thumbnailFileDir.mkdirs()
                }
                val thumbnailFile = File(tnFileStr)
                if (thumbnailFile.createNewFile()) {
                    println("Thumbnail created: " + thumbnailFile.name)
                } else {
                    println("File already exists.")
                }
                ImageIO.write(scaled, "jpg", thumbnailFile)
            } catch (e: IOException) {
                println("An error occurred.")
                e.printStackTrace()
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