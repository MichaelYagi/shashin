package com.miyagi.shashin.util

import com.drew.imaging.ImageMetadataReader
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.miyagi.shashin.model.Metadata
import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.geometry.Positions
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO

class ImageProcessing(private var apiVersion: String?, private var file: File, private var sidecarDir: String, private var metadataObj: Metadata?) {

    private var logger: Logger = Logger.getLogger(ImageProcessing::class.simpleName)

    fun createThumbnails(): Metadata? {
        var _metadataObj = metadataObj

        // Check rotation
        var rotation = 0
        try {
            val fileMetadata = ImageMetadataReader.readMetadata(file)
            var jpegImageWidth = false
            var jpegImageHeight = false
            for (directory in fileMetadata.directories) {
                for (tag in directory.tags) {
                    when (tag.tagName) {
                        "Orientation" -> {
                            if (tag.description.contains("Rotate") && ((!jpegImageHeight && !jpegImageWidth) || directory.name == "Exif IFD0")) {
                                val digit = tag.description.filter { it.isDigit() }

                                if (TextUtils.isInteger(digit)) {
                                    rotation = digit.toInt()
                                }
                            }
                        }
                        "Exif Image Height", "Height", "Image Height" -> {
                            if (jpegImageHeight) {
                                continue
                            }

                            if (directory.name == "JPEG" && tag.tagName == "Image Height") {
                                jpegImageHeight = true
                            }
                        }
                        "Exif Image Width", "Width", "Image Width" -> {
                            if (jpegImageWidth) {
                                continue
                            }

                            if (directory.name == "JPEG" && tag.tagName == "Image Width") {
                                jpegImageWidth = true
                            }
                        }
                    }
                }
            }
//        println("rotation:$rotation")
        } catch (e: Exception) {
            logger.log(
                Level.WARNING,
                "Could not get rotation when reading metadata for " + file.name + ": " + e.message
            )
        }

        val thumbnailDirectory = sidecarDir.dropLast(1) + "/thumbnails"

        // Map path to sidecar file
        val fileRootDir: String = FileUtils.getRootDir(file)
        val supportedImageFormats = FileUtils.allowableImageFiles()
        val supportedVideoFormats = FileUtils.allowableVideoFiles()
        var extension = "jpg"

        var img: BufferedImage? = null
        if (FileUtils.isRaw(file.extension.lowercase())) {
            try {
                img = ImageIO.read(file)
                if (rotation > 0) {
                    img = rotateImage(img, rotation.toDouble())
                }
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Could not read file: " + file.path)
            }
        } else if (supportedImageFormats.contains(file.extension.lowercase())) {
            extension = file.extension.lowercase()
            try {
                img = ImageIO.read(file)
                if (rotation > 0) {
                    img = rotateImage(img, rotation.toDouble())
                }
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Could not read file: " + file.path)
            }
        } else if (supportedVideoFormats.contains(file.extension.lowercase())) {
            // Grab screen shot
            img = grabScreenshot(file)
            if (img != null && _metadataObj?.getOriginalImageWidth() == null && _metadataObj?.getOriginalImageHeight() == null) {
                _metadataObj?.setOriginalImageWidth(img.width)
                _metadataObj?.setOriginalImageHeight(img.height)
            }
//            _metadataObj?.setVideoUrl("/api/$apiVersion/original/video$fileRootDir/" + file.name)
        }

        // Create thumbnails
        if (img != null) {
            _metadataObj?.setFolder(fileRootDir)

            // Raw file to image conversion
            var thumbnailFileStr: String
            var tnFile: File?
            if (FileUtils.isRaw(file.extension.lowercase())) {
                thumbnailFileStr = thumbnailDirectory + fileRootDir + "/" + file.name + "_original." + extension
                tnFile = FileUtils.createFile(thumbnailDirectory + fileRootDir, thumbnailFileStr, "Thumbnail")
                if (tnFile != null) {
                    ImageIO.write(img, extension, tnFile)
                }
                _metadataObj?.setThumbnailUrlOriginal("/api/$apiVersion/thumbnails$fileRootDir/" + file.name + "_original." + extension)
            }

            // Gallery thumbnails
            thumbnailFileStr = thumbnailDirectory + fileRootDir + "/" + file.name + "_209." + extension
            tnFile = FileUtils.createFile(thumbnailDirectory + fileRootDir, thumbnailFileStr, "Thumbnail")

            var scaled209: BufferedImage = if (file.extension.lowercase() == "gif") {
                Thumbnails.of(img)
                    .height(209)
                    .imageType(BufferedImage.TYPE_INT_ARGB)
                    .outputQuality(1.0)
                    .asBufferedImage()
            } else {
                Thumbnails.of(img)
                    .height(209)
                    .outputQuality(1.0)
                    .asBufferedImage()
            }

            if (scaled209.width > scaled209.height * 2) {
                scaled209 = if (file.extension.lowercase() == "gif") {
                    Thumbnails.of(img)
                        .height(209)
                        .imageType(BufferedImage.TYPE_INT_ARGB)
                        .outputQuality(1.0)
                        .sourceRegion(Positions.CENTER, 209, 209)
                        .asBufferedImage()
                } else {
                    Thumbnails.of(scaled209)
                        .height(209)
                        .outputQuality(1.0)
                        .sourceRegion(Positions.CENTER, 209, 209)
                        .asBufferedImage()
                }
            }
            if (tnFile != null) {
                ImageIO.write(scaled209, extension, tnFile)
            }
            _metadataObj?.setThumbnailSmallHeight(scaled209.height)
            _metadataObj?.setThumbnailSmallWidth(scaled209.width)
            _metadataObj?.setThumbnailPathSmall(thumbnailFileStr)
            _metadataObj?.setThumbnailUrlSmall("/api/$apiVersion/thumbnails$fileRootDir/" + file.name + "_209." + extension)

            // Square image thumbnail
            thumbnailFileStr = thumbnailDirectory + fileRootDir + "/" + file.name + "_centered." + extension
            tnFile = FileUtils.createFile(thumbnailDirectory + fileRootDir, thumbnailFileStr, "Thumbnail")
            if (tnFile != null) {
                val square: BufferedImage
                if (img.height > img.width) {
//                    scaled = scaleImageByWidth(img, 209)
                    val temp = Thumbnails.of(img)
                        .width(209)
                        .outputQuality(1.0)
                        .asBufferedImage()
                    square = Thumbnails.of(temp)
                        .width(209)
                        .outputQuality(1.0)
                        .sourceRegion(Positions.CENTER, 209, 209)
                        .asBufferedImage()
                } else {
//                    scaled = scaleImageByHeight(img, 209)
                    val temp = Thumbnails.of(img)
                        .height(209)
                        .outputQuality(1.0)
                        .asBufferedImage()
                    square = Thumbnails.of(temp)
                        .height(209)
                        .outputQuality(1.0)
                        .sourceRegion(Positions.CENTER, 209, 209)
                        .asBufferedImage()
                }

//                val square: BufferedImage = getSquareThumbnail(scaled)

                ImageIO.write(square, extension, tnFile)
            }
            _metadataObj?.setThumbnailPathCentered(thumbnailFileStr)
            _metadataObj?.setThumbnailUrlCentered("/api/$apiVersion/thumbnails$fileRootDir/" + file.name + "_centered." + extension)

            // Map marker thumbnail
            thumbnailFileStr = thumbnailDirectory + fileRootDir + "/" + file.name + "_mapmarker." + extension
            tnFile = FileUtils.createFile(thumbnailDirectory + fileRootDir, thumbnailFileStr, "Thumbnail")
            if (tnFile != null) {
                val mapMarker: BufferedImage
                if (img.height > img.width) {
                    val temp = Thumbnails.of(img)
                        .width(45)
                        .outputQuality(1.0)
                        .asBufferedImage()
                    mapMarker = Thumbnails.of(temp)
                        .width(45)
                        .outputQuality(1.0)
                        .sourceRegion(Positions.CENTER, 45, 45)
                        .asBufferedImage()
                } else {
                    val temp = Thumbnails.of(img)
                        .height(45)
                        .outputQuality(1.0)
                        .asBufferedImage()
                    mapMarker = Thumbnails.of(temp)
                        .height(45)
                        .outputQuality(1.0)
                        .sourceRegion(Positions.CENTER, 45, 45)
                        .asBufferedImage()
                }

//                    val scaled: BufferedImage
//                    if (img.height > img.width) {
//                        scaled = scaleImageByWidth(img, 45)
//                    } else {
//                        scaled = scaleImageByHeight(img, 45)
//                    }
//                    val mapMarker: BufferedImage = getSquareThumbnail(scaled)

                ImageIO.write(mapMarker, extension, tnFile)
            }
            _metadataObj?.setMapMarkerPath(thumbnailFileStr)
            _metadataObj?.setMapMarkerUrl("/api/$apiVersion/thumbnails$fileRootDir/" + file.name + "_mapmarker." + extension)
        } else {
            logger.log(Level.WARNING, "File not supported: " + file.name)
            _metadataObj = null
        }

        // Save serialized metadata obj
        val metadataDirectory = sidecarDir.dropLast(1) + "/metadata/"
        val metadataFileStr = metadataDirectory + fileRootDir + "/" + file.name + ".yaml"
        val mdFile = FileUtils.createFile(metadataDirectory + fileRootDir, metadataFileStr, "YAML metadata")
        if (mdFile != null) {
            val yamlFactory: YAMLFactory = YAMLFactory.builder()
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .disable(YAMLGenerator.Feature.SPLIT_LINES)
                .build()
            val om = ObjectMapper(yamlFactory)
            om.writeValue(mdFile, _metadataObj)
        }

        return _metadataObj
    }

    private fun grabScreenshot(file: File): BufferedImage? {
        try {
            val frameGrabber = FFmpegFrameGrabber(file.path)
//            frameGrabber.format = file.extension.lowercase()
            frameGrabber.start()

            val aa = Java2DFrameConverter()

            var f = frameGrabber.grabKeyFrame()
            var bi = aa.convert(f)

            val limit = 1000
            var count = 0
            while (bi != null) {
                if (limit > count) {
                    break
                }
                f = frameGrabber.grabKeyFrame()
                bi = aa.convert(f)
                count++
            }

            val rotationStr = frameGrabber.getVideoMetadata("rotate")
            if (!rotationStr.isNullOrBlank() && bi != null) {
                val rotation = rotationStr.toDouble()
                if (rotation > 0) {
                    bi = rotateImage(bi, rotation)
                }
            }

            frameGrabber.stop()

            return bi
        } catch (e: IOException) {
            logger.log(Level.WARNING, "Could not convert video " + file.name + ": " + e.message)
            return null
        }
    }

    private fun rotateImage(buffImage: BufferedImage, angle: kotlin.Double): BufferedImage {
        val radian = Math.toRadians(angle)
        val sin = Math.abs(Math.sin(radian))
        val cos = Math.abs(Math.cos(radian))
        val width = buffImage.width
        val height = buffImage.height
        val nWidth = Math.floor(width.toDouble() * cos + height.toDouble() * sin).toInt()
        val nHeight = Math.floor(height.toDouble() * cos + width.toDouble() * sin).toInt()
        val rotatedImage = BufferedImage(
            nWidth, nHeight, BufferedImage.TYPE_INT_RGB
        )
        val graphics = rotatedImage.createGraphics()
        graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC
        )
        graphics.translate((nWidth - width) / 2, (nHeight - height) / 2)
        // rotation around the center point
        graphics.rotate(radian, (width / 2).toDouble(), (height / 2).toDouble())
        graphics.drawImage(buffImage, 0, 0, null)
        graphics.dispose()
        return rotatedImage
    }

    private fun scaleImageByRatio(source: BufferedImage, ratio: kotlin.Double): BufferedImage {
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
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val xScale = w.toDouble() / source.width
        val yScale = h.toDouble() / source.height
        val at = AffineTransform.getScaleInstance(xScale, yScale)
        g2d.drawRenderedImage(source, at)
        g2d.dispose()
        return bi
    }

    private fun getSquareThumbnail(source: BufferedImage): BufferedImage {
        // Get a square thumbnail
        val side = source.width.coerceAtMost(source.height)
        val x = (source.width - side) / 2
        val y = (source.height - side) / 2
        return source.getSubimage(x, y, side, side)
    }

    private fun getCompatibleImage(w: Int, h: Int): BufferedImage {
        return BufferedImage(
            w, h,
            BufferedImage.TYPE_INT_RGB
        )
    }
}